# AI Pipeline Architecture (Sơ đồ Kiến trúc AI Pipeline)

Sơ đồ này mô tả chi tiết toàn bộ vòng đời của hệ thống Machine Learning (AI Pipeline), bao gồm hai giai đoạn độc lập: **Offline Training** (Huấn luyện ngoại tuyến từ tập dữ liệu lớn) và **Online Inference** (Dự đoán trực tuyến thời gian thực).

```mermaid
flowchart TD
  %% Style definitions
  classDef training fill:#fef3c7,stroke:#d97706,stroke-width:2px,color:#92400e;
  classDef inference fill:#f3e8ff,stroke:#9333ea,stroke-width:2px,color:#6b21a8;
  classDef files fill:#fee2e2,stroke:#dc2626,stroke-width:2px,color:#991b1b;
  classDef ext fill:#f1f5f9,stroke:#475569,stroke-width:2px,color:#334155;

  subgraph Phase_1 ["Phase 1: Offline Training (Huấn luyện Ngoại tuyến)"]
    Reddit[("Pushshift Reddit Dataset<br>(RS_2019-04.zst / RC_2019-04.zst)")]:::ext
    Scanner["scanner.py<br>(PushshiftDatasetScanner)"]:::training
    
    subgraph Quality_Filters ["Quality & Spam Filtering"]
      BotF["Bot Detection Filter"]:::training
      NsfwF["NSFW Filter"]:::training
      SpamF["Low-Signal Spam Filter"]:::training
      Reservoir["Reservoir Sampling"]:::training
    end
    
    FeatEng["feature_engineering.py<br>(PushshiftFeatureEngineering)"]:::training
    
    subgraph Sampling_Features ["Negative Sampling & Schema v2"]
      NegSampling["Temporal-Safe Negative Sampling<br>(±72h Lookback, Ratio 1:2)"]:::training
      FeatureSchema["11-Feature Matrix Builder"]:::training
    end
    
    subgraph Normalization ["Normalization & Preprocessing"]
      Cap["Capping Outliers (p99)"]:::training
      Log1p["Log1p Scaling<br>(7d & 30d interactions)"]:::training
    end
    
    Trainer["trainer.py<br>(LightGBM Trainer)"]:::training
    Eval["Model Evaluator<br>(NDCG@10, RMSE, Pairwise Acc)"]:::training
  end

  subgraph Artifacts ["Model Artifacts (Serialized Outputs)"]
    ModelTxt["model.txt<br>(LightGBM Booster Structure)"]:::files
    ModelJson["model.json<br>(Capping, default values metadata)"]:::files
  end

  subgraph Phase_2 ["Phase 2: Online Inference (Dự đoán Trực tuyến)"]
    SpringBoot["Spring Boot Backend<br>(Candidate Generation & Context)"]:::ext
    FastAPI["FastAPI Inference Server<br>(server.py - /predict)"]:::inference
    Vectorizer["inference/vectorizer.py<br>(Online Preprocessor)"]:::inference
    RankSvc["inference/ranking_service.py<br>(LightGBM Ranker)"]:::inference
  end

  %% Offline Flow
  Reddit -->|1. ZST File Stream| Scanner
  Scanner -->|2. Apply| Quality_Filters
  Quality_Filters -->|3. Clean Submissions & Comments| FeatEng
  FeatEng -->|4. Generate Dataset| Sampling_Features
  Sampling_Features -->|5. Apply Transformation| Normalization
  Normalization -->|6. Feature Matrix & Labels| Trainer
  Trainer -->|7. Evaluate| Eval
  Trainer -->|8. Serialize Model| ModelTxt
  FeatEng -->|9. Export Preprocessing Settings| ModelJson

  %% Online Flow
  SpringBoot -->|10. Request Predict<br>(Viewer Context + Candidates)| FastAPI
  FastAPI -->|11. Extract & Vectorize| Vectorizer
  ModelJson -.->|12. Load preprocessing parameters| Vectorizer
  Vectorizer -->|13. Matrix (N x 11)| RankSvc
  ModelTxt -.->|14. Load serialized model| RankSvc
  RankSvc -->|15. Run Inference| RankSvc
  RankSvc -->|16. Scores Array| FastAPI
  FastAPI -->|17. Ranked JSON Response| SpringBoot

  style Phase_1 fill:none,stroke:#d97706,stroke-width:1px,stroke-dasharray: 5 5
  style Phase_2 fill:none,stroke:#9333ea,stroke-width:1px,stroke-dasharray: 5 5
  style Artifacts fill:none,stroke:#dc2626,stroke-width:1px,stroke-dasharray: 5 5
  style Quality_Filters fill:none,stroke:#d97706,stroke-width:1px
  style Sampling_Features fill:none,stroke:#d97706,stroke-width:1px
  style Normalization fill:none,stroke:#d97706,stroke-width:1px
```

## 1. Offline Training Phase (Huấn luyện Ngoại tuyến)

1.  **Dữ liệu thô (Pushshift Reddit Dataset):** Sử dụng các tệp nén Zstandard chứa bài viết (`RS_2019-04.zst`) và bình luận (`RC_2019-04.zst`).
2.  **ETL & Lọc sạch (`scanner.py`):** Lớp `PushshiftDatasetScanner` đọc stream tệp JSON lines tuần tự, lọc bỏ các bot tự động, bài đăng NSFW, nội dung quá ngắn/quá dài hoặc spam (chứa quá nhiều link/ít từ). Thuật toán Reservoir Sampling được sử dụng để lấy mẫu ngẫu nhiên không thiên vị mà không làm tràn RAM.
3.  **Lấy mẫu âm khó & Tạo đặc trưng (`feature_engineering.py`):**
    *   **Implicit Negative Sampling:** Tạo ra các dòng dữ liệu không tương tác (nhãn `0.0`) từ các tác giả khác trong khung thời gian $\pm 72$ giờ so với bài đăng tương tác thực tế (nhãn tương tác được biến đổi liên tục). Tỷ lệ Dương : Âm là $1:2$ giúp mô hình học cách phân biệt tốt nhất.
    *   **Preprocessing:** Tính toán các giá trị giới hạn biên (Capping) tại phân vị 99 (p99) của tập huấn luyện để loại bỏ ngoại lai (outliers), và áp dụng biến đổi `log1p` ($log(1 + x)$) cho các đặc trưng đếm cộng dồn để thu hẹp khoảng phân phối.
4.  **Huấn luyện & Serialization (`trainer.py`):** Huấn luyện mô hình LightGBM hồi quy Pointwise. Xuất ra tệp cấu trúc cây quyết định `model.txt` và lưu toàn bộ tham số tiền xử lý (capping, mặc định) vào `model.json`.

## 2. Online Inference Phase (Dự đoán Trực tuyến)

1.  **FastAPI Endpoint (`/api/ranking/predict`):** Nhận danh sách các ứng viên bài viết và ID người xem từ Spring Boot Backend.
2.  **Vectorizer thời gian chạy (`vectorizer.py`):** Tải các tham số cấu hình từ `model.json`, thực hiện trích xuất và biến đổi các đặc trưng thô thành vector 11 chiều theo đúng cấu trúc Schema v2:
    *   `content_length`: Độ dài văn bản bài viết.
    *   `has_multimedia`: Bài đăng có đính kèm đa phương tiện.
    *   `is_share_post`: Có phải bài viết chia sẻ chéo.
    *   `post_age_hours`: Thời gian tồn tại của bài viết.
    *   `author_seniority`: Tuổi thọ tài khoản tác giả (năm).
    *   `author_post_count`: Số bài viết lịch sử của tác giả.
    *   `author_engagement_rate`: Điểm tương tác trung bình của tác giả.
    *   `interaction_count_7d` & `interaction_count_30d`: Số lần người xem bình luận bài tác giả này trong 7 ngày / 30 ngày (áp dụng Log1p).
    *   `hours_since_last_interaction`: Thời gian kể từ tương tác cuối (mặc định 999.0 nếu chưa từng tương tác).
    *   `affinity_score`: Điểm thân mật giữa người xem và tác giả.
3.  **LightGBM Predictor (`ranking_service.py`):** Sử dụng mô hình `model.txt` để dự đoán nhanh điểm số liên quan cho $N$ ứng viên và trả kết quả về cho Spring Boot.
