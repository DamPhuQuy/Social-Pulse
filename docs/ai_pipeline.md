# AI Pipeline Architecture

Tài liệu này mô tả chi tiết kiến trúc, quy trình xử lý dữ liệu, huấn luyện, đánh giá và triển khai của **AI Pipeline** thuộc hệ thống **Social Pulse**. Hệ thống sử dụng mô hình xếp hạng LightGBM offline để chấm điểm mức độ liên quan của các bài đăng (candidates) đối với người dùng (viewers).

---

## 1. Problem Definition
*   **Mục tiêu (Objective):** Cá nhân hóa nguồn cấp dữ liệu (feed) bằng cách chấm điểm và sắp xếp các bài đăng theo độ tương tác dự kiến của người dùng cụ thể.
*   **Bài toán học máy:** Thuộc lớp bài toán **Learning-to-Rank (LTR)** dạng **Pointwise**, được mô hình hóa dưới dạng một bài toán hồi quy (Regression) nhằm dự báo mức độ tương tác (popularity) của một bài viết cụ thể với một người xem (viewer).
*   **Đầu vào (Input):** 
    *   Thông tin bài viết (Post metadata)
    *   Thông tin tác giả (Author metadata)
    *   Lịch sử tương tác giữa người xem và tác giả (Viewer-Author interaction history)
*   **Đầu ra (Output):** Điểm số liên quan dự kiến (score liên tục) của từng bài đăng cho người dùng tương ứng. Nền tảng backend sẽ sắp xếp các bài viết theo thứ tự điểm giảm dần.
*   **Yêu cầu phi chức năng:** Thời gian phản hồi cực nhanh (< 50ms cho danh sách các bài đăng) và hỗ trợ cơ chế xếp hạng dự phòng (Fallback) khi dịch vụ AI gặp sự cố.

### Candidate Generation vs. Ranking (Quy trình Truy xuất so với Xếp hạng)
Trong hệ thống gợi ý môi trường Production thực tế, quá trình phân phối tin tức được phân lớp thành nhiều giai đoạn chính nhằm đáp ứng bài toán quy mô lớn:
1.  **Candidate Generation (Truy xuất ứng viên):** Backend hệ thống (ví dụ Spring Boot) thực hiện lọc nhanh từ cơ sở dữ liệu hàng chục nghìn bài viết xuống khoảng vài trăm bài viết ứng viên (candidates) có tiềm năng phù hợp nhất bằng các heuristics đơn giản hoặc truy vấn cơ sở dữ liệu nhanh (SQL, Redis).
2.  **Filtering (Lọc):** Loại bỏ các bài viết bị chặn, bài viết trùng lặp, bài viết chứa nội dung nhạy cảm hoặc vi phạm chính sách của người xem hiện tại.
3.  **Ranking (Xếp hạng - Dịch vụ AI):** Dịch vụ AI (FastAPI với mô hình LightGBM) chỉ chịu trách nhiệm cho giai đoạn này. Dịch vụ nhận danh sách vài trăm ứng viên đã qua bộ lọc từ backend, chấm điểm độ liên quan cá nhân hóa cao dựa trên các đặc trưng động, và trả về điểm số tương tác dự kiến.
4.  **Re-ranking (Tái xếp hạng):** Backend nhận điểm số từ AI, áp dụng các luật kinh doanh (business rules), đa dạng hóa nội dung (diversity), chèn quảng cáo hoặc tin ghim trước khi trả về luồng bảng tin cuối cùng cho người dùng.

Hệ thống AI Pipeline hiện tại của Social Pulse chỉ đảm nhận giai đoạn **Ranking**, giúp giảm thiểu tối đa tài nguyên tính toán bằng cách ủy thác khâu lọc thô và truy xuất ứng viên ban đầu cho tầng backend thượng nguồn (upstream backend).

---

## 2. Dataset Architecture
Dữ liệu huấn luyện được mô hình hóa từ hai thực thể chính từ kho lưu trữ Pushshift Reddit:
1.  **Submissions (Bài viết):** Chứa thông tin gốc của bài viết tại thời điểm thu thập (ví dụ: `RS_2019-04.zst`).
2.  **Comments (Tương tác):** Chứa lịch sử bình luận của người dùng trên các bài viết (ví dụ: `RC_2019-04.zst`).

### Cấu trúc dòng dữ liệu huấn luyện (Row Composition):
*   **Positive Rows (Dòng tích cực):** Đại diện cho một viewer đã thực sự có tương tác (bình luận) vào bài đăng của author trước đó. Nhãn (label) là giá trị liên tục được biến đổi từ độ tương tác thực tế của bài đăng.
*   **Negative Rows (Dòng tiêu cực):** Đại diện cho các viewer được lấy mẫu ngẫu nhiên (sampled) chưa từng tương tác với author của bài đăng đó trong khoảng thời gian xác định. Dòng này nhận nhãn bằng `0.0`.
*   **Fallback Rows:** Dành cho các bài viết không có dữ liệu tương tác lịch sử nào từ người xem, sử dụng các đặc trưng tác giả/bài viết làm cơ sở và nhãn gốc.

### Negative Sampling Strategy (Chiến lược Lấy mẫu Tiêu cực)
Để huấn luyện một mô hình xếp hạng cá nhân hóa dạng Pointwise, việc thu thập dữ liệu chỉ dựa trên các tương tác thực tế (Positive) là chưa đủ vì mô hình sẽ không học được ranh giới quyết định. Do đó, hệ thống tích hợp một quy trình lấy mẫu âm nghiêm ngặt trong [feature_engineering.py](file:///home/phuquydam/Documents/Social-Pulse/ai_pipeline/training/feature_engineering.py):
*   **Temporal-Safe Negative Sampling (Lấy mẫu âm an toàn theo thời gian):** Đối với mỗi tương tác tích cực (viewer tương tác với post A của tác giả X vào thời điểm $t$), hệ thống tìm kiếm các bài viết tiêu cực (âm) từ các tác giả khác mà người dùng *không* tương tác trong cùng một cửa sổ thời gian gần kề.
*   **Hard Negative Sampling (Lấy mẫu âm khó):** Thay vì chọn ngẫu nhiên bài viết bất kỳ trên toàn hệ thống (dễ làm mô hình lười biếng), hệ thống lấy mẫu bài đăng âm của các tác giả khác tạo ra trong khoảng thời gian $\pm 72$ giờ (`_NEGATIVE_LOOKBACK_HOURS`) xung quanh thời điểm tương tác tích cực. Đây là những bài viết cạnh tranh trực tiếp sự chú ý của người dùng tại thời điểm đó.
*   **Author Exposure Bias & Popularity-balanced Negatives:** Quá trình lấy mẫu âm bỏ qua bài đăng của chính tác giả mà người dùng đã tương tác để tránh làm nhiễu tín hiệu cá nhân hóa, đồng thời đảm bảo bài đăng âm được chọn ngẫu nhiên từ kho lưu trữ các bài viết đang hoạt động để phản ánh phân phối bài viết thực tế.
*   **Tác động của tham số `negative_samples_per_positive = 2`:**
    *   *Class Imbalance (Mất cân bằng lớp):* Tạo ra tỷ lệ nhãn dương:âm là $1:2$. Tỷ lệ này đủ để mô hình học cách phân biệt sự ưu tiên mà không bị lệch dự đoán quá mức về phía lớp âm.
    *   *Ranking Bias (Thiên vị xếp hạng):* Giúp kiểm soát và giảm bớt thiên vị đối với các bài viết có lượng tương tác quá khủng bằng cách buộc mô hình phải so sánh giữa bài viết dương tính và các ứng viên âm tính diễn ra đồng thời.
    *   *Training Stability (Độ ổn định huấn luyện):* Tăng số lượng mẫu huấn luyện lên gấp 3 lần, giúp hàm loss hồi quy hội tụ ổn định và giảm thiểu phương sai sai số của dự đoán.

---

## 3. Data Collection Pipeline
Quy trình thu thập dữ liệu được thực hiện thông qua module [scanner.py](file:///home/phuquydam/Documents/Social-Pulse/ai_pipeline/training/scanner.py) với lớp `PushshiftDatasetScanner`.

### Quy trình quét Submissions (`scan_submissions`):
1.  **Streaming:** Đọc tuần tự luồng file nén `.zst` để tiết kiệm bộ nhớ thông qua `JsonLineReader`.
2.  **Bộ lọc chất lượng và tiền xử lý thô (`_preprocess_submission`):**
    *   **Trường bắt buộc:** Loại bỏ các bản ghi thiếu `id`, `author`, hoặc `created_utc`.
    *   **Bộ lọc Bot:** Loại bỏ các tác giả trong danh sách nghi ngờ (`_LIKELY_BOT_AUTHORS` như `automoderator`, `tweetposter`, v.v.) hoặc có tên kết thúc bằng `bot`/`_bot`/`-bot`.
    *   **Bộ lọc NSFW:** Loại bỏ bài viết người lớn nếu `exclude_nsfw=True`.
    *   **Độ dài nội dung:** Độ dài tiêu đề + nội dung phải nằm trong khoảng từ `min-content-length` (mặc định 20) đến `max-content-length` (mặc định 20,000).
    *   **Bộ lọc Spam & Tín hiệu thấp (`_low_signal_reason`):** Bài viết phải chứa tối thiểu 3 từ phân biệt (`min_distinct_token_count`), 12 ký tự chữ cái (`min_alpha_char_count`), và tối đa 8 liên kết URL (`max_url_count`).
    *   **Loại bỏ trùng lặp (Deduplication):** Sử dụng mã băm SHA-1 của tiêu đề và nội dung để loại bỏ bài viết trùng lặp nội dung khi `dedupe_posts=True`.
3.  **Lấy mẫu hồ chứa (Reservoir Sampling):** Sử dụng thuật toán lấy mẫu để duy trì một tập hợp ngẫu nhiên kích thước `sample_size` mà không cần tải toàn bộ dữ liệu vào RAM.

### Quy trình quét Tương tác/Comments (`scan_interactions`):
*   Quét luồng comments và lọc bỏ các bình luận của bot hoặc tài khoản bị xóa.
*   Chỉ giữ lại các bình luận trên bài đăng của những author đã có trong bản đồ bài viết (`post_author_map`).
*   Loại bỏ tự tương tác (self-comment) nơi commenter trùng với author.
*   Lưu trữ ánh xạ thời gian tương tác: `interactions[commenter][post_author] -> list[created_utc]`.

---

## 4. Data Preprocessing Pipeline
Tiền xử lý dữ liệu học máy được thực hiện bởi lớp `PushshiftFeatureEngineering` trong [feature_engineering.py](file:///home/phuquydam/Documents/Social-Pulse/ai_pipeline/training/feature_engineering.py). Quy trình được học từ tập huấn luyện và áp dụng nhất quán tại thời điểm dự đoán (Inference).

1.  **Xử lý giá trị khuyết thiếu (Imputation & Defaults):**
    *   Thay thế các trường số bị thiếu bằng giá trị mặc định `0.0`.
    *   Đặc trưng `hours_since_last_interaction` khi không có tương tác trước đó sẽ được gán giá trị mặc định là `999.0` (quy định tại `RankingFeatureSchema.DEFAULT_LAST_INTERACTION_HOURS`).
    *   Các cờ nhị phân mặc định nhận `0.0`.
2.  **Giới hạn biên (Capping):**
    *   Để hạn chế nhiễu từ các điểm dữ liệu dị biệt (outliers), các đặc trưng phân phối đuôi dài được cắt cụt tại phân vị thứ 99 (`DEFAULT_CAP_PERCENTILE = 99.0`) của tập huấn luyện.
    *   Các đặc trưng áp dụng capping bao gồm: `content_length`, `post_age_hours`, `author_seniority`, `author_post_count`, `author_engagement_rate`, `hours_since_last_interaction`.
3.  **Biến đổi Log (Log Transformation):**
    *   Áp dụng biến đổi `log1p` (tức là $log(1 + x)$) để thu hẹp khoảng phân phối của các đặc trưng đếm tích lũy: `interaction_count_7d` và `interaction_count_30d`.
4.  **Đóng gói tham số:** Các giá trị cap và thiết lập biến đổi log được lưu trữ trong `model.json` để phục vụ trực tiếp cho quá trình vector hóa thời điểm chạy thực tế (runtime vectorization).

---

## 5. Feature Engineering Pipeline
Hệ thống sử dụng **Feature Schema v2** gồm 11 đặc trưng cốt lõi (leakage-safe). Thứ tự đặc trưng được định nghĩa cố định tại [schema.py](file:///home/phuquydam/Documents/Social-Pulse/ai_pipeline/shared/schema.py):

| Chỉ số | Tên Đặc Trưng | Nguồn Gốc | Ý Nghĩa Chi Tiết |
| :--- | :--- | :--- | :--- |
| **0** | `content_length` | Bài đăng | Tổng độ dài tiêu đề và nội dung bài viết. |
| **1** | `has_multimedia` | Bài đăng | Cờ nhị phân (1.0/0.0) biểu thị bài đăng có video, ảnh, link đa phương tiện. |
| **2** | `is_share_post` | Bài đăng | Cờ nhị phân (1.0/0.0) biểu thị bài viết là crosspost. |
| **3** | `post_age_hours` | Bài đăng | Số giờ kể từ khi bài đăng được tạo đến thời điểm truy vấn (hoặc thời điểm tham chiếu). |
| **4** | `author_seniority` | Tác giả | Tuổi thọ tài khoản của tác giả tính bằng năm tại thời điểm đăng bài. |
| **5** | `author_post_count` | Tác giả | Số lượng bài đăng lịch sử của tác giả trước bài viết hiện tại. |
| **6** | `author_engagement_rate`| Tác giả | Điểm tương tác trung bình lịch sử của tác giả trước bài viết hiện tại. |
| **7** | `interaction_count_7d` | Tương tác | Số lần người xem bình luận bài viết của tác giả này trong 7 ngày trước đó. |
| **8** | `interaction_count_30d`| Tương tác | Số lần người xem bình luận bài viết của tác giả này trong 30 ngày trước đó. |
| **9** | `hours_since_last_interaction`| Tương tác| Số giờ trôi qua kể từ lần tương tác gần nhất giữa người xem và tác giả. |
| **10**| `affinity_score` | Tương tác | Tỷ lệ tương tác với tác giả này trên tổng số tương tác của người xem trong 30 ngày. |

---

## 6. Feature Selection Strategy
Chiến lược lựa chọn đặc trưng của Social Pulse được định hướng nhằm kết hợp cân bằng giữa:
1.  **Đặc trưng chất lượng nội dung tĩnh:** `content_length`, `has_multimedia`, `is_share_post`.
2.  **Đặc trưng uy tín/lịch sử của tác giả:** `author_seniority`, `author_post_count`, `author_engagement_rate`.
3.  **Đặc trưng quan hệ cá nhân hóa (Viewer-Author):** `interaction_count_7d`, `interaction_count_30d`, `hours_since_last_interaction`, `affinity_score`.

Hệ thống loại bỏ hoàn toàn các đặc trưng văn bản thô (NLP nhúng sâu) tại v2 để đảm bảo tốc độ tính toán cực nhanh ở backend và tránh quá tải bộ nhớ. Thứ tự truyền đặc trưng vào mô hình được kiểm soát chặt chẽ bằng danh sách `RankingFeatureSchema.FEATURE_ORDER` để ngăn ngừa lỗi lệch thứ tự dữ liệu giữa lúc train và lúc inference.

---

## 7. Data Leakage Prevention
Phòng chống rò rỉ dữ liệu (Data Leakage) là ưu tiên hàng đầu trong thiết kế AI Pipeline của Social Pulse:
*   **Loại bỏ ảnh chụp tương tác cuối kỳ (Target-Time Snapshots):** Các thông số tương tác của chính bài viết tại thời điểm thu thập (như tổng vote score, comment count thực tế cuối cùng, vote ratio) **hoàn toàn không** được làm đặc trưng. Chúng chỉ được dùng làm nhãn (label).
*   **Tính toán lịch sử tác giả theo dòng thời gian (Chronological Author State):** Khi duyệt qua các bài đăng theo trật tự thời gian để tính toán `author_post_count` và `author_engagement_rate`, trạng thái của tác giả được chụp lại **trước** khi cộng dồn chỉ số của chính bài viết hiện tại. Điều này ngăn mô hình "nhìn thấy trước" hiệu năng của bài viết đang cần dự đoán.
*   **Lọc mốc thời gian tương tác (Time-bound Interactions):** Khi tính các đặc trưng tương tác của viewer (ví dụ: `interaction_count_7d`), hệ thống chỉ quét các mốc thời gian bình luận xảy ra **trước** thời điểm tạo bài đăng cần chấm điểm (`created_utc`).
*   **Phân chia tập dữ liệu theo nhóm Post ID (Grouped Chronological Split):** 
    *   Dữ liệu được gom nhóm theo `post_id`. Toàn bộ các dòng dữ liệu (tích cực/tiêu cực) liên quan đến cùng một `post_id` phải cùng nằm chung trong một tập (hoặc Train, hoặc Validation, hoặc Test).
    *   Việc phân chia được thực hiện theo dòng thời gian (chronological order): huấn luyện trên dữ liệu cũ, đánh giá trên dữ liệu mới hơn để mô phỏng chính xác hành vi triển khai thực tế.
*   **Kiểm tra tính toàn vẹn phân tách (`split_integrity`):** Pipeline tự động chạy kiểm tra chéo tập hợp `post_id` giữa các tập split. Nếu phát hiện bất kỳ sự trùng lặp nào, cảnh báo rò rỉ sẽ được kích hoạt để hủy kết quả chạy.

---

## 8. Model Architecture Selection
*   **Thuật toán:** Gradient Boosted Decision Trees (GBDT) được hiện thực hóa thông qua thư viện **LightGBM** (`lightgbm.Booster`).
*   **Lý do lựa chọn:**
    *   Khả năng xử lý tối ưu các dạng dữ liệu bảng hỗn hợp (mixed tabular data) và dữ liệu có nhiều giá trị khuyết thiếu.
    *   Tốc độ huấn luyện nhanh và cơ chế phân bin dữ liệu (histogram-based) giúp tiết kiệm bộ nhớ đáng kể.
    *   Hỗ trợ xuất mô hình ra định dạng file text (`model.txt`), giúp quá trình tải mô hình ở môi trường production (FastAPI/C++) siêu nhẹ và độc lập với môi trường Python phức tạp.

---

## 9. Loss Function Selection
*   **Hàm mục tiêu huấn luyện (Objective):** `"regression"` (L2 Loss / Mean Squared Error) dùng để dự đoán điểm tương tác liên tục.
*   **Hàm lỗi đánh giá (Eval Metric):** `["rmse", "mae"]`.
*   **Công thức xây dựng nhãn mục tiêu (Popularity Proxy):**
    $$popularity = \max(score, 0) + \max(num\_comments, 0) + \max(num\_crossposts, 0)$$
    $$label = \ln(1 + popularity)$$
*   **Lý do chọn Regression thay vì Classification:** Do độ tương tác của Reddit phân phối lệch rất mạnh, việc dự đoán giá trị liên tục được làm mịn qua hàm $log(1+x)$ giúp mô hình phân biệt tốt hơn giữa các bài viết có độ thảo luận cực cao (viral) và các bài viết trung bình, tạo ra khoảng cách điểm số mịn màng hơn khi thực hiện sắp xếp (ranking).

### Label Construction Strategy (Chiến lược Thiết lập Nhãn)
Thiết lập nhãn mục tiêu (Label Engineering) trong một hệ thống xếp hạng bài viết mạng xã hội đối mặt với nhiều thách thức từ tính chất dữ liệu thực tế:
1.  **Reddit Vote Fuzzing (Làm nhiễu số vote):** Reddit tự động làm nhiễu nhẹ số điểm vote thực tế trên giao diện để chống spam bot. Việc cộng gộp `score + num_comments + num_crossposts` làm mờ đi tác động của cơ chế vote fuzzing này, giúp nhãn ổn định hơn.
2.  **Noisy & Delayed Engagement Accumulation (Tích lũy tương tác chậm):** Các chỉ số tương tác (like, share, comment) cần thời gian để tích lũy. Các bài viết mới đăng thường bị đánh giá thấp do độ trễ này. Bằng cách sử dụng kho dữ liệu Pushshift được chụp snapshot tại thời điểm cố định trong quá khứ (`retrieved_on` muộn hơn thời điểm tạo), nhãn đại diện cho trạng thái tương tác đã hội tụ ổn định.
3.  **Popularity Long-tail Distribution (Phân phối đuôi dài):** Độ phổ biến bài viết tuân theo luật lũy thừa (Power Law), nơi 1% bài viết chiếm 99% lượng tương tác. Nếu dùng nhãn thô trực tiếp, gradient của mô hình sẽ bị chi phối hoàn toàn bởi một số rất ít bài viết siêu viral. Biến đổi $label = \ln(1 + popularity)$ giúp nén dải giá trị cực lớn này về phân phối chuẩn hơn, giúp mô hình hội tụ tốt hơn.
4.  **Tại sao chọn Popularity Proxy thay vì Click-Through Rate (CTR) thực tế?**
    *   *CTR Lack of Support:* Trong tập dữ liệu Reddit lịch sử, chúng ta không có nhật ký hiển thị (impression logs) thực tế của từng người dùng mà chỉ có kết quả tương tác cuối cùng (bình luận). Việc tính CTR thực tế ($Clicks / Impressions$) là không khả thi.
    *   *Implicit Feedback:* Số lượng bình luận, crosspost và điểm số hoạt động như một chỉ báo ngầm (implicit feedback) phản ánh mức độ hấp dẫn tổng thể của bài đăng.

---

## 10. Optimization & Training Strategy
Chiến lược tối ưu hóa mô hình được thiết lập trong [trainer.py](file:///home/phuquydam/Documents/Social-Pulse/ai_pipeline/training/trainer.py):
*   **Tham số cấu trúc cây:**
    *   `max_depth`: Giới hạn độ sâu tối đa của cây (mặc định 8) để chống quá khớp.
    *   `num_leaves`: Số lá tối đa của cây, được tính tự động bằng $2^{max\_depth} - 1$ (mặc định 255).
    *   `min_child_samples` (`min_samples_leaf`): Số lượng mẫu tối thiểu trong một lá (mặc định 32).
*   **Tham số kiểm soát và chính quy hóa (Regularization):**
    *   `reg_lambda` (L2 regularization): Mặc định 1.5.
    *   `reg_alpha` (L1 regularization): Mặc định 0.05.
    *   `subsample`: Tỷ lệ lấy mẫu dữ liệu cho mỗi cây (mặc định 0.85).
    *   `colsample_bytree`: Tỷ lệ lấy mẫu đặc trưng cho mỗi cây (mặc định 0.80).
*   **Cơ chế dừng sớm (Early Stopping):** 
    *   Huấn luyện với tối đa `n_estimators` (mặc định 1200) vòng lặp.
    *   Sử dụng tập Validation để giám sát RMSE. Nếu lỗi trên tập Validation không giảm sau liên tiếp `early_stopping_rounds` (mặc định 50) vòng lặp, quá trình huấn luyện sẽ dừng lại và lấy Booster tại vòng lặp tốt nhất (`best_iteration`).
*   **Tối ưu phần cứng:** Huấn luyện mặc định yêu cầu GPU CUDA (`--device cuda`). Nếu lỗi phần cứng hoặc thiếu driver, hệ thống tự động rơi về chế độ CPU (`allow_cpu_fallback=True`). Trong các kịch bản chạy chính thức trên GPU chuyên dụng, fallback bị vô hiệu hóa để phát hiện lỗi sớm.

### Gradient Boosting Update Mechanism (Cơ chế Cập nhật Gradient Boosting)
LightGBM hoạt động dựa trên nguyên lý huấn luyện tăng cường cây quyết định theo cơ chế hồi quy gradient (Gradient Boosting Decision Trees - GBDT). Tiến trình toán học diễn ra như sau:

#### 1. Additive Boosting & Residual Fitting
Mô hình cuối cùng $F_M(x)$ được xây dựng dưới dạng cộng dồn (additive) của $M$ cây quyết định độc lập (weak learners):
$$F_m(x) = F_{m-1}(x) + \eta h_m(x)$$

Trong đó:
*   $F_m(x)$ là mô hình tổng hợp tại phân thân thứ $m$.
*   $\eta$ là tốc độ học (`learning_rate`, mặc định 0.05) đóng vai trò thu hẹp trọng số (shrinkage) để tránh quá khớp.
*   $h_m(x)$ là cây quyết định thứ $m$ được huấn luyện để khớp với phần dư (residuals) hoặc gradient của mô hình phía trước $F_{m-1}(x)$.

#### 2. Objective Function & Second-Order Optimization
Mục tiêu là tối thiểu hóa hàm mất mát tổng thể kết hợp thành phần chuẩn hóa (Regularization) để kiểm soát độ phức tạp của cây:
$$\text{Obj} = \sum_{i=1}^N l(y_i, \hat{y}_i) + \sum_{k=1}^m \Omega(f_k)$$

LightGBM tối ưu hóa hàm này bằng cách xấp xỉ Taylor bậc hai (Second-order Taylor expansion) xung quanh điểm dự báo hiện tại $\hat{y}_i^{(m-1)}$:
$$\text{Obj}^{(m)} \approx \sum_{i=1}^N \left[ l(y_i, \hat{y}_i^{(m-1)}) + g_i f_m(x_i) + \frac{1}{2} h_i f_m^2(x_i) \right] + \Omega(f_m)$$

Trong đó, Gradient thứ nhất $g_i$ và Gradient thứ hai (Hessian) $h_i$ trên từng mẫu dữ liệu $i$ được định nghĩa:
$$g_i = \frac{\partial l(y_i, \hat{y}_i^{(m-1)})}{\partial \hat{y}_i^{(m-1)}}$$
$$h_i = \frac{\partial^2 l(y_i, \hat{y}_i^{(m-1)})}{\partial (\hat{y}_i^{(m-1)})^2}$$

Với lỗi bình phương L2 (MSE regression) sử dụng trong hệ thống:
$$l(y_i, \hat{y}_i) = \frac{1}{2} (y_i - \hat{y}_i)^2 \implies g_i = \hat{y}_i - y_i, \quad h_i = 1$$

#### 3. Leaf-wise Tree Growth & Histogram Split Optimization
*   **Leaf-wise Growth (Phát triển theo lá):** Khác với XGBoost phát triển cây theo chiều ngang (level-wise), LightGBM tìm lá có độ giảm tổn thất lớn nhất để chia nhánh, giúp tạo ra các cây sâu hơn nhưng giảm sai số tốt hơn trên dữ liệu lớn.
*   **Histogram-based Split (Tối ưu hóa chia nhánh theo lược đồ):** Thay vì duyệt qua tất cả các giá trị số liên tục của các đặc trưng (rất chậm), LightGBM gom các giá trị liên tục vào các hộp rời rạc (`max_bin = 256`). Khi tìm điểm chia nhánh (split point), mô hình chỉ duyệt qua 256 bin này dựa trên tổng tích lũy gradient ($G$) và hessian ($H$) của từng bin, tăng tốc độ huấn luyện lên gấp nhiều lần.

---

## 11. Hyperparameter Tuning
Các siêu tham số được cấu hình thông qua tham số dòng lệnh được định nghĩa tại [arguments.py](file:///home/phuquydam/Documents/Social-Pulse/ai_pipeline/training/arguments.py) bao gồm:
*   Tốc độ học (`learning_rate`, mặc định 0.05)
*   Số bin tối đa để chia ngưỡng đặc trưng (`max_bin`, mặc định 256)
*   Tỷ lệ chia tập dữ liệu (`validation_ratio=0.2`, `test_ratio=0.1`)
*   Số lượng mẫu tiêu cực cho mỗi mẫu tích cực (`negative_samples_per_positive`, mặc định 2)
*   Số lượng người xem tích cực tối đa cho mỗi post (`max_positive_viewers_per_post`, mặc định 20)

Chiến lược dò tìm siêu tham số hiện tại dựa trên việc chạy các script mẫu được cấu hình sẵn như `train-pilot.ps1` (chạy thử nghiệm nhanh với dữ liệu nhỏ để kiểm tra logic) và `train-full-gpu.ps1` (chạy huấn luyện quy mô đầy đủ).

---

## 12. Validation Strategy
Hệ thống sử dụng chiến lược **Phân chia theo thời gian gom cụm (Time-Ordered Grouped Split)**:
1.  **Gom cụm:** Gom toàn bộ dòng dữ liệu theo thuộc tính phân tách `post_id`.
2.  **Sắp xếp:** Sắp xếp các cụm này theo thuộc tính thời gian tạo bài viết tăng dần (`created_utc`).
3.  **Phân chia:**
    *   Tập huấn luyện (Train): Chiếm phần dữ liệu cũ nhất ($1 - val\_ratio - test\_ratio$).
    *   Tập kiểm định (Validation): Chiếm phần dữ liệu ở giữa ($val\_ratio$). Dùng làm tập giám sát để thực hiện Early Stopping.
    *   Tập kiểm thử (Test): Chiếm phần dữ liệu mới nhất ở cuối ($test\_ratio$). Giữ vai trò đánh giá độc lập cuối cùng.
*   **Ý nghĩa:** Tránh rò rỉ thông tin tương lai về quá khứ và đảm bảo tính độc lập hoàn toàn giữa các cụm bài viết.

---

## 13. Evaluation Metrics
Hệ thống đo lường hiệu năng mô hình qua bộ chỉ số đa chiều lưu tại `metrics.json`:
*   **RMSE & MAE:** Đo lường sai lệch tuyệt đối của điểm dự báo so với nhãn log-popularity thực tế trên các tập Train/Val/Test.
*   **R2 Score (Coefficient of Determination):** Đo lường mức độ cải thiện phương sai giải thích được của mô hình so với dự báo hằng số bằng giá trị trung bình nhãn huấn luyện (`mean_label_baseline`).
*   **NDCG@10 (Normalized Discounted Cumulative Gain):** Chỉ số xếp hạng cốt lõi. Tính toán mức độ tối ưu của thứ tự sắp xếp bài viết trong danh sách của từng viewer riêng biệt.
*   **Pairwise Accuracy:** Chỉ số độ chính xác cặp so sánh. Đếm tỷ lệ số cặp bài viết $(A, B)$ của cùng một viewer mà mô hình dự đoán đúng xu hướng tương tác thực tế (tức là bài viết thực tế có tương tác cao hơn sẽ nhận điểm dự đoán cao hơn).

---

## 14. Model Update Strategy
Quá trình cập nhật mô hình được thực hiện định kỳ bằng cách quét dữ liệu mới và chạy lại luồng huấn luyện offline. Mô hình mới chỉ được phép đẩy lên môi trường Production nếu vượt qua **Bảng kiểm định an toàn (Train Checklist)**:
1.  **Không có rò rỉ dữ liệu:** `split_integrity.actual_post_id_overlap` giữa Train-Validation, Train-Test và Validation-Test phải bằng `0`.
2.  **Độ chính xác cặp:** `pairwise_accuracy` trên tập Test phải lớn hơn hoặc bằng `0.55` (chứng tỏ mô hình tốt hơn ngẫu nhiên đáng kể).
3.  **Độ uplift NDCG:** NDCG của mô hình phải cải thiện tối thiểu `0.02` so với baseline trung bình (`mean_label_baseline`).
4.  **Kiểm tra Overfitting:** Sai lệch RMSE tập Train không được thấp hơn 75% so với RMSE tập Validation.
5.  **Kiểm tra tính trung thực:** Chỉ số NDCG không được phép gần như hoàn hảo ($\ge 0.995$), vì điều này thường báo hiệu có đặc trưng bị rò rỉ nhãn (leaked feature).
6.  **Kiểm tra độ tập trung đặc trưng:** Không có bất kỳ đặc trưng đơn lẻ nào chiếm hơn 70% tổng mức đóng góp độ lợi (gain importance share).

---

## 15. Inference Pipeline
Kiến trúc phục vụ dự đoán thời gian thực (Inference Runtime) được cài đặt như một dịch vụ độc lập sử dụng **FastAPI** tại [server.py](file:///home/phuquydam/Documents/Social-Pulse/ai_pipeline/server.py), được điều phối chính bởi [ranking_service.py](file:///home/phuquydam/Documents/Social-Pulse/ai_pipeline/inference/ranking_service.py) và [vectorizer.py](file:///home/phuquydam/Documents/Social-Pulse/ai_pipeline/inference/vectorizer.py).

### Quy trình xử lý yêu cầu (Runtime Flow):
1.  **Nhận Yêu cầu:** Khách hàng (Spring Boot backend) gửi yêu cầu POST đến `/api/ranking/predict` kèm theo danh sách các bài đăng và các đặc trưng thô tương ứng (`post_features`, `author_features`, `interaction_features`).
2.  **Kiểm tra phiên bản hợp đồng:** Xác thực thuộc tính `feature_schema_version` của request phải khớp với cấu hình của dịch vụ (mặc định `v2`).
3.  **Vector hóa đặc trưng (`FeatureVectorizer`):**
    *   Áp dụng các giá trị cap (cắt cụt phân vị 99) được học từ lúc huấn luyện.
    *   Áp dụng log biến đổi cho các đặc trưng đếm tương tác 7 ngày/30 ngày.
    *   Xếp các đặc trưng vào mảng numpy đúng theo thứ tự cột mô hình yêu cầu.
4.  **Dự báo:** Gọi `booster.predict(matrix)` sử dụng đối tượng LightGBM đã tải sẵn vào RAM lúc khởi động dịch vụ từ file sidecar `model.txt`.
5.  **Trả kết quả:** Trả về danh sách JSON chứa `post_id`, predicted `score` và phiên bản schema tương ứng.
6.  **Cơ chế dự phòng (Inference Fallback):** Nếu FastAPI gặp sự cố mạng hoặc phản hồi quá lâu, backend sẽ chuyển hướng sử dụng `FallbackRankingService` để sắp xếp danh sách bằng các chỉ số tương tác tĩnh trong database mà không làm gián đoạn trải nghiệm người dùng.

### Online/Offline Feature Consistency (Tính Nhất quán Đặc trưng Online/Offline)
Một trong những lỗi nghiêm trọng nhất trong hệ thống ML là lệch phân phối giữa huấn luyện và chạy thực tế (Train-Serve Skew). Social Pulse ngăn ngừa lỗi này bằng cơ chế đồng bộ hóa đặc trưng nghiêm ngặt:
1.  **Parity of Feature Logic (Nhất quán logic biến đổi):** Cả quá trình sinh dữ liệu offline trong [feature_engineering.py](file:///home/phuquydam/Documents/Social-Pulse/ai_pipeline/training/feature_engineering.py) và vector hóa online trong [vectorizer.py](file:///home/phuquydam/Documents/Social-Pulse/ai_pipeline/inference/vectorizer.py) đều sử dụng chung một giao ước định danh và mặc định từ `RankingFeatureSchema`.
2.  **Configuration Parity (Nhất quán tham số tiền xử lý):** Tránh việc tính toán lại giá trị capping (P99) hay giá trị trung bình trên traffic thực tế. Thay vào đó, toàn bộ giá trị capped tối đa (`cap_values`) và danh sách các trường cần lấy log (`log_transform_features`) thu được từ tập train offline được ghi vào `model.json` và được load cứng vào dịch vụ FastAPI khi khởi động.
3.  **Feature Freshness (Độ tươi đặc trưng):** Các đặc trưng tác giả (`author_post_count`, `average_popularity`) và đặc trưng tương tác (`interaction_count_7d`) được backend cập nhật định kỳ vào cơ sở dữ liệu. Khi có request xếp hạng, backend trích xuất các đặc trưng này tại thời điểm hiện tại và truyền trực tiếp qua API, đảm bảo mô hình nhận được dữ liệu phản ánh đúng trạng thái thực tế nhất của người dùng và tác giả.

---

## 16. Monitoring & Drift Detection
Để đảm bảo mô hình hoạt động ổn định trên môi trường Production, hệ thống áp dụng các cơ chế giám sát sau:
*   **Liveness & Readiness Probe:** Endpoint `/health` của FastAPI trả về trạng thái hoạt động, thông tin mô hình đã tải (`model_loaded`), vị trí file mô hình và tính sẵn sàng của schema.
*   **Cảnh báo lệch phân phối dữ liệu (Distribution Shift Warnings):** Lúc huấn luyện, hệ thống tính toán độ lệch trung bình nhãn giữa Train/Val/Test. Nếu vượt ngưỡng quy định, cảnh báo `validation/test label distribution is shifted` sẽ được ghi nhận vào file metadata để cảnh báo kỹ sư AI cần điều chỉnh bộ lọc dữ liệu đầu vào.
*   **Log cảnh báo runtime:** Các trường hợp request gửi sai schema hoặc không tương thích đặc trưng sẽ tự động bị từ chối và ghi log cảnh báo chi tiết ở cấp độ hệ thống.

---

## 17. Experiment Tracking
Mọi phiên huấn luyện mô hình đều tự động kết xuất đầy đủ thông tin nhật ký nhằm phục vụ việc tái lập và theo dõi thí nghiệm:
*   **model.json:** Tệp kê khai chính chứa cấu hình siêu tham số, thông tin phần cứng chạy huấn luyện, biểu đồ tiến trình mất mát qua từng vòng lặp (loss history), tầm quan trọng đặc trưng (feature importance) và các cảnh báo đánh giá.
*   **metrics.json:** Tệp tóm tắt ngắn gọn các điểm số RMSE, MAE, R2, NDCG trên cả 3 tập split phục vụ cho các công cụ so sánh tự động.
*   **Biểu đồ phân tích (Plots):** Xuất tự động ra thư mục `ai_pipeline/model/plots/` gồm:
    *   `label_distribution.png`: Phân phối tần suất của nhãn tương tác.
    *   `split_label_distribution.png`: Biểu đồ so sánh phân phối nhãn giữa 3 tập split nhằm phát hiện trôi lệch nhãn (label shift).
    *   `training_curves.png`: Đường cong học tập (loss curves) của RMSE/MAE trên tập Train và Valid qua các vòng lặp.
    *   `feature_importance.png`: Xếp hạng mức đóng góp độ lợi (gain) của 11 đặc trưng.

---

## 18. Model Versioning
Phiên bản mô hình được kiểm soát chặt chẽ thông qua cấu trúc dữ liệu `RankingModelArtifact` định nghĩa tại [model.py](file:///home/phuquydam/Documents/Social-Pulse/ai_pipeline/shared/model.py):
*   **Mã định danh phiên bản (`artifact_version`):** Mặc định là `"1"`.
*   **Phiên bản cấu trúc đặc trưng (`feature_schema_version`):** Hiện tại là `"v2"`.
*   **Liên kết tệp bổ trợ (`model_file`):** Chỉ định tên tệp chứa cấu trúc cây nhị phân LightGBM thực tế (mặc định là `"model.txt"` nằm cùng thư mục với `model.json`).
*   Khởi chạy dịch vụ FastAPI sẽ kiểm tra chéo các thông số này trong metadata trước khi cho phép xếp hạng. Điều này đảm bảo Spring Boot backend và FastAPI luôn hoạt động trên cùng một phiên bản giao ước dữ liệu.

---

## 19. Deployment Architecture
Dịch vụ AI được tích hợp đóng gói để vận hành đồng bộ trong cụm ứng dụng Social Pulse:
*   **Đóng gói Container (Docker):** Sử dụng `Dockerfile` tối ưu hóa dựa trên công cụ quản lý thư viện Python siêu nhanh `uv`. Quá trình build đồng bộ các thư viện từ tệp `pyproject.toml` và `uv.lock`.
*   **Docker Compose:** Dịch vụ AI chạy dưới tên container `ai-pipeline` trên cổng `8000`, liên kết mạng nội bộ với container backend chính.
*   **Tham số cấu hình môi trường:**
    *   `AI_PIPELINE_ENABLED`: Bật/tắt gọi dịch vụ AI từ phía backend (mặc định `true`).
    *   `AI_PIPELINE_BASE_URL`: URL gọi API AI (ví dụ: `http://ai-pipeline:8000`).
    *   `AI_PIPELINE_FEATURE_SCHEMA_VERSION`: Giao ước schema (mặc định `v2`).
    *   `AI_PIPELINE_MODEL_LOCATION`: Đường dẫn trỏ tới tệp cấu hình mô hình bên trong container (mặc định `/app/model/model.json`).

## 20. System Design Trade-offs

Trong quá trình thiết kế hệ thống xếp hạng AI cho Social Pulse, nhiều quyết định kiến trúc đã được đưa ra dựa trên sự đánh đổi giữa độ chính xác của mô hình (model accuracy), độ trễ phản hồi (inference latency), chi phí vận hành (serving cost) và độ phức tạp triển khai (deployment complexity).

### 1. Why LightGBM instead of Deep Learning? (Tại sao chọn LightGBM thay vì Deep Learning?)
*   **Lower Inference Latency (Độ trễ dự đoán thấp hơn):** LightGBM chạy trên CPU với các cấu trúc cây IF-ELSE biên dịch trực tiếp, giúp thời gian phản hồi ở mức dưới mili-giây (sub-millisecond) cho hàng trăm ứng viên. Mô hình Deep Learning (ví dụ: MLP, DLRM) đòi hỏi tính toán ma trận lớn, có thể đẩy độ trễ vượt ngưỡng 50ms nếu không có phần cứng tăng tốc chuyên dụng.
*   **Lower Serving Cost (Chi phí vận hành thấp hơn):** LightGBM cực kỳ nhẹ về tài nguyên. Nó có thể chạy trực tiếp trên các nhân CPU giá rẻ của máy chủ ứng dụng mà không cần duy trì các cụm GPU đắt đỏ cho khâu phục vụ dự đoán.
*   **Tabular-Data Dominance (Ưu thế trên dữ liệu bảng):** Các đặc trưng của Social Pulse chủ yếu là dữ liệu dạng bảng (metadata bài viết, thống kê tác giả, tần suất tương tác lịch sử). Các mô hình cây quyết định (GBDT) như LightGBM vẫn giữ vị thế thống trị và vượt trội hơn Deep Learning về cả độ chính xác lẫn tính ổn định trên các định dạng dữ liệu này.
*   **Easier Deployment (Triển khai dễ dàng):** LightGBM hỗ trợ xuất mô hình ra tệp văn bản thuần `model.txt`. File này cực kỳ dễ phân phối, lưu trữ và load động ở FastAPI mà không cần cài đặt các framework nặng như TensorFlow hay PyTorch.
*   **Better Cold-Start Robustness (Khả năng chịu lỗi khởi đầu lạnh tốt hơn):** Khác với Deep Learning dễ bị sai lệch nghiêm trọng khi thiếu dữ liệu, mô hình cây có thể xử lý các giá trị khuyết thiếu (NaN) của người dùng mới bằng các nhánh mặc định đã được tính toán tối ưu từ trước.

### 2. Why Pointwise instead of LambdaMART? (Tại sao chọn Pointwise thay vì LambdaMART?)
*   **Simpler Debugging (Dễ dàng gỡ lỗi):** Phương pháp Pointwise mô hình hóa bài toán dưới dạng hồi quy (Regression). Điểm số đầu ra của mô hình có ý nghĩa vật lý trực quan (log-popularity ước tính), giúp các kỹ sư dễ dàng kiểm tra, gỡ lỗi và thiết lập các ngưỡng chặn (thresholds) khi cần thiết.
*   **More Stable Training (Huấn luyện ổn định hơn):** Pointwise Regression tối ưu hóa trực tiếp hàm lỗi L2 (MSE), một hàm lồi có bề mặt gradient cực kỳ mịn và ổn định. Ngược lại, LambdaMART (Listwise) tối ưu hóa hàm loss dựa trên NDCG (vốn không khả vi trực tiếp), dễ dẫn đến mất ổn định trong quá trình hội tụ trên các tập dữ liệu nhỏ hoặc mất cân bằng.
*   **Smaller Dataset Scale (Quy mô dữ liệu nhỏ):** Ở quy mô ban đầu của dự án, số lượng ứng viên và tương tác lịch sử chưa đạt mức hàng triệu. Huấn luyện Pointwise Regression giúp tận dụng hiệu quả từng dòng dữ liệu lấy mẫu âm/dương mà không đòi hỏi số lượng nhóm (queries/groups) khổng lồ như các thuật toán so sánh cặp hoặc danh sách.
*   **Easier Regression Monitoring (Dễ dàng giám sát hồi quy):** Các chỉ số đánh giá của hồi quy như RMSE, MAE, R2 rất trực quan, giúp dễ dàng phát hiện sự trôi lệch phân phối dữ liệu (Data Drift) so với các chỉ số xếp hạng tương đối của Listwise.

### 3. Why No Transformer Embeddings? (Tại sao không dùng Transformer Embeddings?)
*   **Memory Overhead (Quá tải bộ nhớ):** Việc tích hợp các mô hình ngôn ngữ lớn (LLMs/Transformers) để sinh vector nhúng (embeddings) cho tiêu đề/nội dung bài viết đòi hỏi dung lượng RAM/VRAM khổng lồ ở cả tầng offline lẫn online.
*   **Inference Latency (Độ trễ thời gian thực):** Chạy một mô hình Transformer (như BERT) để encode văn bản của hàng trăm bài viết ứng viên tại thời điểm người dùng load feed sẽ tạo ra nút thắt cổ chai về hiệu năng (thường mất từ 100ms - 500ms), vi phạm nghiêm trọng yêu cầu phi chức năng (< 50ms).
*   **Online Serving Complexity (Độ phức tạp vận hành):** Nếu không dùng Transformer online, ta sẽ phải lưu trữ sẵn (pre-compute) vector nhúng của tất cả bài viết vào cơ sở dữ liệu vector (Vector Database) và truy xuất thời gian thực. Điều này làm phức tạp hóa đáng kể kiến trúc hệ thống (cần duy trì Milvus/Qdrant, xử lý đồng bộ hóa vector).
*   **GPU Dependency (Sự phụ thuộc vào GPU):** Sử dụng Transformer bắt buộc hệ thống phải có GPU ở môi trường phục vụ dự đoán (Serving), làm mất đi tính linh hoạt của việc triển khai đa đám mây (multi-cloud) và làm tăng chi phí hạ tầng lên gấp nhiều lần.

---

## 21. Future Improvements
Các định hướng nâng cấp hệ thống AI Pipeline trong tương lai bao gồm:
1.  **Xây dựng Online Feature Store:** Tích hợp Redis hoặc một giải pháp Feature Store chuyên dụng nhằm lưu trữ và truy vấn nhanh các chỉ số tương tác thời gian thực của tác giả/người dùng thay vì tính toán ad-hoc tại database của backend.
2.  **Chuyển đổi sang Listwise Learning-to-Rank:** Chuyển đổi hàm mục tiêu huấn luyện của LightGBM từ hồi quy điểm (Pointwise Regression) sang tối ưu hóa thứ tự trực tiếp (**LambdaMART/LambdaRank**) để cải thiện trực tiếp chỉ số NDCG@10.
3.  **Tự động hóa phát hiện trôi lệch dữ liệu thời gian thực (Real-time Drift Detection):** Phát triển cơ chế định kỳ thu thập phân phối đặc trưng thực tế từ dữ liệu yêu cầu (request payloads) để so sánh với phân phối lúc huấn luyện trong `model.json`, từ đó tự động đưa ra cảnh báo cần huấn luyện lại mô hình (retraining trigger).
4.  **Nạp lại mô hình động (Dynamic Hot-reloading):** Cho phép dịch vụ FastAPI tự động phát hiện tệp mô hình mới được cập nhật trên đĩa và tải lại mô hình vào bộ nhớ RAM mà không cần phải khởi động lại container dịch vụ.
