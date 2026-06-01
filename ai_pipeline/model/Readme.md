# 📊 Báo cáo Huấn luyện & Đánh giá Mô hình AI (LightGBM)

Bản tổng hợp kết quả chi tiết từ quá trình huấn luyện và đánh giá mô hình xếp hạng tin tức (Feed Ranking) dựa trên dữ liệu thực tế thu thập được.

## 1. Thống kê Dữ liệu Huấn luyện
Mô hình được huấn luyện trên tập dữ liệu Reddit Pushshift lớn:
* **Số lượng bài viết đã quét**: 18.310.157 bài viết (chấp nhận 9.864.729 bài viết sau khi lọc nội dung rác, tài khoản ảo, nội dung nhạy cảm...).
* **Số lượng bình luận đã quét**: 138.473.643 bình luận (trích xuất được 1.493.397 tương tác hợp lệ từ 683.000 người dùng).
* **Tổng số mẫu dữ liệu huấn luyện**: 1.893.969 dòng (bao gồm 631.323 mẫu tích cực và 1.262.646 mẫu tiêu cực từ chiến lược chọn mẫu tiêu cực theo thời gian xem).
* **Phân bổ tập dữ liệu (Huấn luyện / Xác thực / Kiểm thử)**:
  * **Tập huấn luyện**: 1.335.684 dòng (70%)
  * **Tập xác thực**: 381.312 dòng (20%)
  * **Tập kiểm thử**: 176.973 dòng (10%)
* **Tỷ lệ nhãn không (Tỷ lệ không tương tác)**: 66,67% (phản ánh thực tế dữ liệu tương tác thưa thớt trên mạng xã hội).

## 2. Chỉ số Đánh giá Hiệu năng Mô hình
So sánh mô hình xếp hạng LightGBM với Mô hình cơ sở (Baseline - dự đoán bằng giá trị trung bình):

| Chỉ số | Tập huấn luyện | Tập xác thực | Tập kiểm thử | Mô hình cơ sở (Kiểm thử) | Ý nghĩa chỉ số |
| :--- | :---: | :---: | :---: | :---: | :--- |
| **RMSE** | 1,4545 | 1,4264 | **1,3899** | 2,1226 | Sai số bình phương trung bình cực tiểu (Càng thấp càng tốt) |
| **MAE** | 0,8892 | 0,8475 | **0,8253** | 1,7939 | Sai số tuyệt đối trung bình (Càng thấp càng tốt) |
| **NDCG@10** | 0,9394 | 0,9505 | **0,9574** | 0,7054 | Chỉ số chất lượng xếp hạng Top 10 (Càng gần 1 càng tốt) |
| **Hệ số xác định (R²)** | 0,5467 | 0,5627 | **0,5712** | -0,0001 | Tỷ lệ phương sai nhãn được giải thích (Càng cao càng tốt) |
| **Độ chính xác so sánh cặp** | 89,26% | 90,95% | **92,27%** | - | Tỷ lệ cặp bài viết được xếp thứ tự chính xác |

> [!TIP]
> Mô hình LightGBM mang lại hiệu năng cải thiện vượt trội so với Mô hình cơ sở trên tập kiểm thử (chỉ số NDCG@10 tăng mạnh từ **0,7054 lên 0,9574**, hệ số R² tăng từ **~0 lên 0,5712**).
> Độ chính xác so sánh cặp đạt **92,27%** đảm bảo khả năng hiển thị các nội dung phù hợp nhất lên đầu trang tin của người dùng.

## 3. Mức độ Đóng góp của các Đặc trưng (Feature Importance)
Đóng góp của các đặc trưng vào quyết định xếp hạng của mô hình (tính theo tỷ lệ độ lợi thông tin):

1. **`post_age_hours`** (Thời gian tồn tại của bài viết): **69,52%** 🥇
2. **`author_engagement_rate`** (Tỷ lệ tương tác của tác giả): **9,34%** 🥈
3. **`is_share_post`** (Có phải bài đăng chia sẻ lại): **6,91%** 🥉
4. **`author_post_count`** (Tổng số bài đăng của tác giả): **6,69%**
5. **`content_length`** (Độ dài nội dung bài đăng): **2,70%**
6. **`has_multimedia`** (Bài đăng chứa hình ảnh/video/đa phương tiện): **2,55%**
7. **`author_seniority`** (Thâm niên hoạt động của tác giả): **2,25%**
8. Các đặc trưng tương tác lịch sử trực tiếp giữa người xem và tác giả đóng góp dưới **0,1%** (bao gồm điểm tương đồng, số lượng tương tác trong 30 ngày, số giờ từ lần tương tác cuối, và số tương tác trong 7 ngày).

## 4. Siêu tham số & Môi trường Huấn luyện
* **Cấu hình thuật toán**:
  * Số lượng cây quyết định (`n_estimators`): 1200 (Vòng lặp tối ưu nhất đạt được tại **106**)
  * Tốc độ học (`learning_rate`): 0,05
  * Độ sâu tối đa của cây (`max_depth`): 8
  * Số mẫu tối thiểu tại một lá (`min_samples_leaf`): 32
  * Tỷ lệ lấy mẫu hàng (`subsample`): 0,85
  * Tỷ lệ lấy mẫu cột (`colsample_bytree`): 0,80
* **Thời gian huấn luyện**: 2003 giây (khoảng 33,4 phút).
* **Môi trường phần cứng**:
  * **Hệ điều hành**: Windows 11 (build 10.0.26100)
  * **Bộ vi xử lý (CPU)**: Intel Core (20 nhân logic)
  * **Bộ nhớ trong (RAM)**: 31,7 GB khả dụng
  * **Card đồ họa (GPU)**: NVIDIA GeForce RTX 5070 (12 GB VRAM, Driver 596.36) hỗ trợ tăng tốc CUDA.

## 5. Biểu đồ Phân tích & Đánh giá
Các biểu đồ phân tích được tạo và lưu trữ trong thư mục [plots/](file:///d:/Projects/social-pulse/ai_pipeline/model/plots):
* **Biểu đồ phân phối nhãn tổng thể**: [label_distribution.png](file:///d:/Projects/social-pulse/ai_pipeline/model/plots/label_distribution.png)
* **Biểu đồ phân phối nhãn theo các tập dữ liệu**: [split_label_distribution.png](file:///d:/Projects/social-pulse/ai_pipeline/model/plots/split_label_distribution.png)
* **Đường cong học tập (Loss Curves)**: [training_curves.png](file:///d:/Projects/social-pulse/ai_pipeline/model/plots/training_curves.png)
* **Mức độ quan trọng của đặc trưng**: [feature_importance.png](file:///d:/Projects/social-pulse/ai_pipeline/model/plots/feature_importance.png)
