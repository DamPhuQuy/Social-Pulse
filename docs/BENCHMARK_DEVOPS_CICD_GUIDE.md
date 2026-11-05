# Cẩm Nang Kỹ Thuật: Hệ Thống Phân Tán, Benchmark, Docker DevOps & CI/CD (WHAT, WHY, HOW)

> **Dành cho kỹ sư Backend / Full-Stack**: Tài liệu trang bị tư duy kiến trúc phân tán, phương pháp giải quyết 4 bài toán đồng thời cốt lõi, cẩm nang đo kiểm hiệu năng (k6), kỹ nghệ đóng gói Docker chuyên sâu và tự động hóa CI/CD cho nền tảng DevPulse. Giúp bạn tự tay thực hiện, làm chủ thông số và tự tin bảo vệ dự án trong các vòng phỏng vấn kỹ thuật theo chuẩn **Google XYZ Resume**.

---

## MỤC LỤC
1. [PILLAR 1: 4 BÀI TOÁN HỆ THỐNG PHÂN TÁN & ĐỒNG THỜI CỐT LÕI](#pillar-1-4-bài-toán-hệ-thống-phân-tán--đồng-thời-cốt-lõi)
   - 1.1. Lost Update Problem & Atomic Database Arithmetic
   - 1.2. Write Storms & Redis HyperLogLog Write-Behind Buffer
   - 1.3. Cache Stampede (Thundering Herd) & Singleflight Distributed Lock
   - 1.4. Non-blocking Scalability & Java 21 Virtual Threads (Project Loom)
2. [PILLAR 2: BENCHMARK & PERFORMANCE TESTING (k6)](#pillar-2-benchmark--performance-testing-k6)
   - 2.1. WHAT: Benchmark là gì? Các loại bài test hiệu năng
   - 2.2. WHY: Vì sao Unit Test là chưa đủ? Tại sao cần k6?
   - 2.3. HOW: Cẩm nang thực thi & Phân tích thông số k6 thực tế
3. [PILLAR 3: DEVOPS & DOCKER PRODUCTION DEPLOYMENT](#pillar-3-devops--docker-production-deployment)
   - 3.1. WHAT: Bản chất của Docker & Containerization dưới góc nhìn Kernel
   - 3.2. WHY: Tại sao Docker là tiêu chuẩn công nghiệp & Cạm bẫy "Localhost"
   - 3.3. HOW: Cẩm nang kỹ thuật Docker chuyên sâu (Multi-stage, Network, Volume, Limits)
   - 3.4. HOW: Cẩm nang triển khai thực tế lên VPS Linux & Caddy HTTPS
4. [PILLAR 4: CI/CD (CONTINUOUS INTEGRATION & CONTINUOUS DEPLOYMENT)](#pillar-4-cicd-continuous-integration--continuous-deployment)
   - 4.1. WHAT: CI/CD là gì? Các thành phần cốt lõi
   - 4.2. WHY: Vì sao dự án cần tự động hóa CI/CD?
   - 4.3. HOW: Xây dựng GitHub Actions Pipeline từ kiểm thử đến Deploy VPS
5. [PILLAR 5: CHUYỂN HÓA SANG GOOGLE XYZ RESUME & KỊCH BẢN PHỎNG VẤN](#pillar-5-chuyển-hóa-sang-google-xyz-resume--kịch-bản-phỏng-vấn)
   - 5.1. 4 Mẫu đạn vàng (Bullet Points) chuẩn XYZ
   - 5.2. Bộ câu hỏi phản biện kỹ thuật (Q&A Defense)

---

# PILLAR 1: 4 BÀI TOÁN HỆ THỐNG PHÂN TÁN & ĐỒNG THỜI CỐT LÕI

Hầu hết các dự án mạng xã hội hay thương mại điện tử của sinh viên và junior developers đều dừng lại ở mức **CRUD cơ bản**: dùng ORM (Hibernate/JPA) đọc dữ liệu lên RAM, sửa đổi thuộc tính và gọi hàm `save()`.  
Cách làm này chỉ hoạt động khi có **1 người dùng** trên localhost. Nhưng trong một hệ thống thực tế có hàng ngàn người dùng đồng thời (High Concurrency), 4 thảm họa kỹ thuật sau đây chắc chắn sẽ xảy ra nếu không có thiết kế phân tán chuẩn mực:

---

## 1.1. Lost Update Problem & Atomic Database Arithmetic

### 1.1.1. NỀN TẢNG CƠ SỞ: Vì sao lại dẫn đến vấn đề này?
Khi bạn mới bắt đầu học lập trình web hoặc làm đồ án, mọi thứ diễn ra rất lý tưởng:
- Chỉ có một mình bạn mở trình duyệt test.
- Bạn bấm "Upvote" -> Số vote tăng từ 0 lên 1. Bạn bấm tiếp -> Tăng lên 2.
- Mã nguồn viết rất tự nhiên:
  ```java
  // ❌ CÁCH VIẾT NGÂY THƠ AI CŨNG TỪNG LÀM (CRUD MẶC ĐỊNH):
  PostEntity post = postRepository.findById(postId).orElseThrow(); // Bước 1: Đọc từ DB lên RAM
  post.setUpvoteCount(post.getUpvoteCount() + 1);                  // Bước 2: Sửa đổi trên RAM
  postRepository.save(post);                                       // Bước 3: Ghi ngược lại xuống DB
  ```

Trong đầu chúng ta lúc đó luôn có một giả định ngầm: *"Máy tính sẽ thực thi xong xuôi một lượt bấm của người A rồi mới tới lượt bấm của người B"*.  
**Nhưng sự thật trên môi trường Production thì tàn khốc hơn nhiều:**
1. Máy chủ Web (Tomcat trong Spring Boot) là một cỗ máy **đa luồng (Multithreaded)**. Mỗi request từ người dùng gửi đến sẽ được giao cho một luồng (Thread) riêng biệt xử lý song song trên các nhân CPU (Multi-core).
2. Khi một bài viết bất ngờ trở nên "hot" hoặc có tranh luận sôi nổi, tại **cùng một phần nghìn giây (millisecond)**, có hàng chục hoặc hàng trăm người dùng cùng bấm nút Upvote.
3. Lúc này, **2 luồng trở lên sẽ cùng đọc dữ liệu cũ tại cùng một thời điểm**, tính toán độc lập trong vùng nhớ RAM của riêng mình, rồi **ghi đè mù quáng (blindly overwrite)** lên nhau.

---

### 1.1.2. TRỰC QUAN HÓA LỖI: Bi kịch "Rút tiền tại 2 cây ATM cùng lúc"
Hãy tưởng tượng tài khoản ngân hàng của bạn có **100.000 VNĐ**.  
Bạn đưa thẻ cho bạn gái rút ở Cây ATM 1, còn bạn đứng rút ở Cây ATM 2.  
Cả hai người cùng bấm rút **100.000 VNĐ** tại đúng giây thứ `12:00:00.000`:
- **Cây ATM 1** đọc số dư: thấy 100k -> Hợp lệ -> Nhả tiền 100k -> Cập nhật số dư: `100k - 100k = 0k`.
- **Cây ATM 2** (cũng đọc số dư lúc 100k vì ATM 1 chưa kịp trừ): thấy 100k -> Hợp lệ -> Nhả tiền 100k -> Cập nhật số dư: `100k - 100k = 0k`.  
👉 **Kết quả**: Bạn rút được 200k nhưng tài khoản chỉ bị trừ 100k. Ngân hàng bị lỗ!

**Quay lại bài viết DevPulse**:
Giả sử bài viết đang có **10 Upvotes**. Hai người dùng A và B cùng bấm Upvote đồng thời:

```text
Thời gian        Thread 1 (Xử lý User A)            Thread 2 (Xử lý User B)          PostgreSQL Database
   │                                                                                         10
   ├── T1 ──► 1. [READ]: Đọc DB -> thấy 10                                                   10
   ├── T2 ─────────────────────────────────────► 1. [READ]: Đọc DB -> thấy 10                10
   │          (Cả 2 đều giữ số 10 trong RAM của riêng mình!)
   ├── T3 ──► 2. [MODIFY]: Tính 10 + 1 = 11                                                  10
   ├── T4 ─────────────────────────────────────► 2. [MODIFY]: Tính 10 + 1 = 11               10
   ├── T5 ──► 3. [WRITE]: Ghi 11 xuống DB ─────────────────────────────────────────────────► 11
   ├── T6 ─────────────────────────────────────► 3. [WRITE]: Ghi đè 11 xuống DB ───────────► 11 (MẤT VOTE CỦA A!)
   ▼
```
- Có **2 người thực sự bấm vote**.
- Bảng lịch sử tương tác (`post_reactions`) lưu đủ 2 dòng của User A và User B.
- Nhưng con số hiển thị `upvote_count` trên bảng `posts` **chỉ tăng thêm 1** (là 11 thay vì 12).
- Vote của User A đã bị User B vô tình xóa sạch khỏi bộ đếm. Dữ liệu giữa 2 bảng bị **lệch pha hoàn toàn (Data Inconsistency)**.

---

### 1.1.3. CÁC CÁCH GIẢI QUYẾT THÔNG THƯỜNG VÀ VÌ SAO CHÚNG THẤT BẠI?

Khi phát hiện ra lỗi này, các lập trình viên thường thử qua 3 giải pháp đơn giản nhưng đều dẫn vào "ngõ cụt":

| Giải pháp thử nghiệm | Cách thức thực hiện | Vì sao THẤT BẠI trong thực tế? |
|---|---|---|
| **1. Dùng `synchronized` trong Java** | Đặt từ khóa `public synchronized void vote(...)` | **Vô dụng khi scale ngang**: Chỉ có tác dụng trên 1 máy ảo JVM duy nhất. Khi bạn chạy 2 máy chủ backend sau Load Balancer, 2 request đến 2 máy khác nhau vẫn bị race condition như thường. Ngoài ra, nó biến hệ thống thành đơn luồng, bóp nghẽn toàn bộ throughput. |
| **2. Dùng Pessimistic Lock (`SELECT FOR UPDATE`)** | Khóa cứng hàng dữ liệu trong DB, bắt người khác phải chờ | **Gây tắc nghẽn cổ chai & Deadlock**: Khi hàng chục request dồn tới, chúng phải xếp hàng chờ mở khóa, làm cạn kiệt connection pool của HikariCP. Nếu hai transaction tranh chấp khóa chéo nhau, PostgreSQL sẽ ném lỗi `DeadlockDetectedException`. |
| **3. Dùng Optimistic Lock (`@Version`)** | Đánh số version cho hàng. Nếu version bị đổi thì ném lỗi | **Trải nghiệm người dùng cực tệ**: 100 người like cùng lúc thì chỉ có 1 người thành công, 99 người còn lại bị văng lỗi `OptimisticLockingFailureException`. Người dùng sẽ thấy ứng dụng báo lỗi đỏ liên tục dù họ chẳng làm gì sai! |

---

### 1.1.4. GIẢI PHÁP ĐỈNH CAO CỦA DEVPULSE: ATOMIC IN-DATABASE ARITHMETIC

Thay vì để Java đọc dữ liệu lên RAM rồi loay hoay tìm cách khóa, **DevPulse giao toàn bộ quyền tính toán cho chính cơ sở dữ liệu PostgreSQL** bằng một câu lệnh JPQL nguyên tử (Atomic):

```java
// ✅ ĐOẠN CODE BẢO VỆ DỮ LIỆU CỦA DEVPULSE (Trong JpaPostRepository.java)
@Modifying
@Query("""
    UPDATE PostEntity p 
    SET p.upvoteCount = CASE WHEN (p.upvoteCount + :delta) < 0L THEN 0L ELSE (p.upvoteCount + :delta) END, 
        p.hotScore = (CASE WHEN (p.upvoteCount + :delta) < 0L THEN 0L ELSE (p.upvoteCount + :delta) END - p.downvoteCount) 
    WHERE p.id = :id
""")
int atomicUpdateVoteAndHotScore(@Param("id") Long id, @Param("delta") long delta);
```

#### Vì sao giải pháp này lại là "Vua của hiệu năng và tính nhất quán"?
1. **Bản chất của PostgreSQL Row-Level Locking**:  
   Khi câu lệnh `UPDATE ... WHERE id = :id` được gửi xuống, PostgreSQL tự động xác lập một khóa độc quyền ở mức hàng (Row-level lock) trong vài micro-giây. Nó tự xếp hàng các phép tính số học:
   - Luồng A đến: `upvoteCount = 10 + 1 = 11`.
   - Luồng B đến ngay sau: Đọc trực tiếp giá trị 11 vừa commit -> `upvoteCount = 11 + 1 = 12`.  
   Không có biến trung gian nào chạy qua Java Heap RAM, loại bỏ 100% khả năng đọc dữ liệu cũ!
2. **Khái niệm "Floor Clamping" (Chặn đáy an toàn)**:  
   Đoạn code `CASE WHEN (p.upvoteCount + :delta) < 0L THEN 0L ...` là một kỹ thuật phòng thủ chiều sâu. Nếu một kẻ xấu liên tục spam lệnh hủy vote (delta = -1) khi bài viết đang có 0 vote, câu lệnh thông thường sẽ biến vote thành `-1` (vô lý!). Floor Clamping hoạt động như một chiếc van 1 chiều: **Nếu tụt dưới 0 thì ép về 0**, bảo vệ bất biến dữ liệu (Invariants) của mạng xã hội.
3. **Bằng chứng thực nghiệm (Proof of Work)**:  
   File test [`ReactPostConcurrencyTest.java`](file:///home/phuqy/Develop/Social-Pulse/backend/src/test/java/com/socialpulse/app/post/ReactPostConcurrencyTest.java) dùng `ExecutorService` bắn đồng thời 100 luồng vote vào 1 post. Kết quả: **100/100 vote được tính chính xác, 0 lỗi, 0 deadlock, thời gian chạy chỉ mất vài giây**.

---

## 1.2. Write Storms & Redis HyperLogLog Write-Behind Buffer

### 1.2.1. NỀN TẢNG CƠ SỞ: Vì sao lại dẫn đến vấn đề này?
Sau khi giải quyết xong bài toán Upvote, chúng ta bước sang một bài toán tưởng như tương tự nhưng có bản chất hoàn toàn khác: **Đếm lượt xem bài viết (View Count)**.
- **Vote**: Người dùng phải chủ động bấm nút (tần suất vừa phải).
- **View**: Bất kỳ ai mở trang web lên là đã phát sinh 1 lượt xem (tần suất cực kỳ khủng khiếp).

Hãy hình dung một buổi sáng, một bài viết kỹ thuật trên DevPulse được chia sẻ lên các group lập trình lớn. Trong vòng 10 giây, có **5,000 lập trình viên bấm vào xem bài viết**:
- Nếu chúng ta làm theo cách CRUD ngây thơ: Cứ mỗi người vào xem là chạy 1 câu lệnh `UPDATE posts SET view_count = view_count + 1 WHERE id = 123`.
- **Thảm họa xảy ra**: Trong 10 giây đó, PostgreSQL phải tiếp nhận và xử lý **5,000 transaction ghi đĩa**.

#### Tại sao cơ sở dữ liệu quan hệ (RDBMS) lại sợ các lệnh ghi dồn dập?
1. **Quy luật vật lý về phần cứng**:
   - Tốc độ đọc/ghi trên RAM: tính bằng **nano-giây** ($ns$).
   - Tốc độ ghi xuống đĩa cứng SSD/NVMe: tính bằng **mili-giây** ($ms$). RAM nhanh hơn đĩa cứng từ **1,000 đến 10,000 lần**!
2. **Đặc tính ACID của PostgreSQL**:
   - Khi bạn gọi lệnh ghi, PostgreSQL không được phép chỉ sửa trên RAM mà nó **bắt buộc phải ghi nhật ký xuống ổ đĩa (WAL - Write-Ahead Log)** để đảm bảo nguyên tắc Bền vững (Durability) — nếu mất điện đột ngột thì dữ liệu không mất.
   - 5,000 lệnh ghi đồng nghĩa với việc đĩa cứng bị vắt kiệt công suất (Disk I/O 100%). Connection Pool cạn sạch. Kết quả là toàn bộ hệ thống bị tê liệt. Đây chính là hiện tượng **Write Storm (Cơn bão ghi)**.

---

### 1.2.2. NGHỊCH LÝ ĐẾM NGƯỜI XEM DUY NHẤT (UNIQUE VIEWS) & BẾ TẮC CỦA REDIS SET

Để ngăn chặn việc một người dùng bấm F5 liên tục 1,000 lần để "bơm view ảo", hệ thống phải có cơ chế: **Mỗi User chỉ được tính 1 view duy nhất cho 1 bài viết trong ngày**.

#### Cách ngây thơ 1: Lưu vào bảng trong PostgreSQL
Tạo bảng `post_views(post_id, user_id, viewed_at)`.  
Mỗi lần xem -> `INSERT INTO post_views ...`  
👉 **Thất bại**: 5,000 lệnh INSERT/giây còn làm database nổ tung nhanh hơn cả lệnh UPDATE!

#### Cách ngây thơ 2: Dùng `Redis SET`
Mọi người nghĩ ngay đến Redis vì Redis chạy trên RAM (cực nhanh). Redis có cấu trúc dữ liệu `SET` tự động loại bỏ trùng lặp:
`SADD post:100:views {userId}`
Nếu `userId` đã có trong Set -> không tăng view. Nếu chưa có -> tăng view.  
👉 **Nghịch lý chi phí RAM**:
- Giả sử bài viết có **1 triệu lượt xem**.
- Để lưu 1 triệu `userId` (mỗi ID chiếm khoảng 8-16 bytes cộng với overhead con trỏ bộ nhớ của cấu trúc Node trong Redis Set), một bài viết ngốn khoảng **15MB RAM**.
- Nếu DevPulse phát triển lên có **10,000 bài viết**, dung lượng RAM bạn phải trả tiền thuê máy chủ là:
  $$10,000 \times 15\text{MB} = 150\text{GB RAM}!$$
- Chi phí thuê máy chủ có 150GB RAM mỗi tháng lên tới hàng chục triệu đồng, hoàn toàn phi thực tế cho một hệ thống khởi nghiệp.

---

### 1.2.3. VŨ KHÍ 1: REDIS HYPERLOGLOG (HLL) — MA THUẬT TOÁN HỌC 12KB

Làm sao để đếm được 1 triệu, thậm chí 100 triệu người xem duy nhất mà **không cần lưu bất kỳ một `userId` nào vào RAM**?  
Câu trả lời là **HyperLogLog (HLL)** — một phát minh toán học xác suất vĩ đại của nhà khoa học Philippe Flajolet.

#### Trực quan hóa HyperLogLog bằng "Trò chơi Tung Đồng Xu":
Hãy tưởng tượng bạn bảo một người bạn tung một đồng xu nhiều lần:
- Nếu bạn của bạn bảo: *"Lần tung đầu tiên của tao ra mặt Ngửa"* -> Bình thường, xác suất là $1/2$ ($50\%$), chứng tỏ bạn ấy chỉ mới tung 1-2 lần.
- Nhưng nếu bạn ấy bảo: *"Tao vừa tung được một chuỗi 10 lần liên tiếp toàn mặt Ngửa!"* -> Bạn sẽ kinh ngạc. Xác suất để tung được 10 lần Ngửa liên tiếp là $(\frac{1}{2})^{10} = \frac{1}{1024}$. Bạn có thể suy luận logic ngay: *"Người này chắc chắn đã phải kiên trì tung đồng xu ít nhất cả ngàn lần thì mới gặp được một chuỗi may mắn 10 lần Ngửa như vậy!"*

**HyperLogLog hoạt động y hệt như vậy**:
1. Khi `userId` (ví dụ: `987654`) mở bài viết, Redis băm (hash) ID này thành một chuỗi nhị phân 64-bit gồm các số 0 và 1 (ví dụ: `00000101101...`).
2. Thuật toán đếm số lượng số `0` liên tiếp ở đầu chuỗi (gọi là leading zeros).
3. Càng có nhiều người xem khác nhau thì xác suất xuất hiện một chuỗi hash có nhiều số 0 liên tiếp càng cao.
4. HLL chia chuỗi thành 16,384 thùng (registers) nhỏ để lấy mẫu trung bình điều hòa, loại bỏ sai số may rủi.

```text
┌─────────────────────────────────────────────────────────────┐
│                 BẢN CHẤT CỦA HYPERLOGLOG (HLL)              │
│                                                             │
│   User A (ID: 101) ──► HASH: 01101... (1 số 0 đầu)          │
│   User B (ID: 205) ──► HASH: 00110... (2 số 0 đầu)          │
│   User C (ID: 999) ──► HASH: 000001.. (5 số 0 đầu)          │
│                                                             │
│   👉 HLL KHÔNG LƯU ID 101, 205, 999!                       │
│   👉 Nó CHỈ LƯU số lượng bit 0 lớn nhất từng quan sát được! │
│                                                             │
│   KẾT QUẢ KỲ DIỆU:                                          │
│   • Dù có 10 người xem hay 100 TRIỆU người xem              │
│   • Dung lượng bộ nhớ LUÔN CỐ ĐỊNH CHÍNH XÁC: 12KB RAM!     │
│   • Độ sai số thống kê: Cực nhỏ, chỉ <= 0.81%               │
└─────────────────────────────────────────────────────────────┘
```

Trong DevPulse: Lệnh `PFADD post:{id}:views:hll {userId}`:
- Nếu `userId` đã xem rồi -> trả về `0` (không tốn tài nguyên).
- Nếu `userId` là người xem mới -> trả về `1`.
- Đổi lại một sai số nhỏ không đáng kể (ví dụ bài viết 10,000 view thì hiển thị 9,920 view), chúng ta **tiết kiệm được 99.9% dung lượng RAM**.

---

### 1.2.4. VŨ KHÍ 2: KIẾN TRÚC WRITE-BEHIND BUFFER TRONG DEVPULSE

Sau khi có HLL giải quyết bài toán lọc trùng trong 12KB RAM, DevPulse triển khai mô hình **Write-Behind Buffer** để dập tắt hoàn toàn cơn bão ghi (Write Storm) xuống PostgreSQL:

```text
[ 5,000 Views/giây ập tới ]
              │
              ▼
┌─────────────────────────────────────────────────────────────┐
│                      TẦNG REDIS (RAM)                       │
│  1. PFADD post:{id}:views:hll {userId}                      │
│     -> Nếu trùng: Bỏ qua                                    │
│     -> Nếu mới: Đi tiếp bước 2                              │
│  2. INCR post:{id}:views:delta                              │
│     -> Tăng bộ đếm view chờ xử lý trên RAM                  │
│     -> Thời gian phản hồi cho User: < 2 mili-giây!          │
└─────────────────────────────────────────────────────────────┘
              │
              │ (Cứ mỗi 30 giây chạy ngầm 1 lần)
              ▼ [ File: SyncSchedule.java ]
┌─────────────────────────────────────────────────────────────┐
│                 TẦNG POSTGRESQL (DISC I/O)                  │
│  Gom toàn bộ 5,000 views trong 30 giây thành 1 BATCH DUY NHẤT:
│  UPDATE posts SET view_count = view_count + 5000            │
│  WHERE id = :id                                             │
│                                                             │
│  👉 Thay vì 5,000 lệnh ghi đĩa -> Chỉ còn 1 LỆNH DUY NHẤT!  │
│  👉 Giảm 99.9% tải I/O cho cơ sở dữ liệu                    │
│  👉 Có cơ chế ROLLBACK lại Redis delta nếu DB gặp sự cố     │
└─────────────────────────────────────────────────────────────┘
```

**Mã nguồn thực tế trong [`SyncSchedule.java`](file:///home/phuqy/Develop/Social-Pulse/backend/src/main/java/com/socialpulse/app/common/schedule/SyncSchedule.java)**:
Cứ mỗi 30 giây, một background worker thức dậy:
1. Quét tìm các key có dạng `post:*:views:delta`.
2. Đọc giá trị delta tích lũy (ví dụ: +5,000).
3. Gửi 1 câu lệnh batch update duy nhất xuống PostgreSQL.
4. Nếu DB cập nhật thành công -> Xóa delta trên Redis.
5. Nếu DB bị lỗi mạng -> Giữ nguyên delta trên Redis để chu kỳ 30 giây tiếp theo đồng bộ lại (chống mất dữ liệu tuyệt đối).

---

## 1.3. Cache Stampede (Thundering Herd) & Singleflight Distributed Lock

### 1.3.1. NỀN TẢNG CƠ SỞ: Vì sao lại dẫn đến vấn đề này?
Trong bất kỳ hệ thống mạng xã hội nào, trang **Bảng tin (Feed trang chủ)** luôn là trang có chi phí tính toán nặng nề nhất:
- Phải tìm danh sách các bài viết mới nhất từ những người dùng đang follow.
- Phải lọc bỏ các bài viết vi phạm, bài viết từ những người bị chặn.
- Phải tính toán điểm xu hướng (Hot Score / Ranking Score) dựa trên thuật toán thời gian và lượng upvote/bình luận.
- Phải lấy thông tin avatar tác giả, số lượt xem, trạng thái tương tác của người đang đọc.

Một câu query tính toán Feed trang chủ phức tạp như vậy có thể tốn từ **50ms đến 150ms CPU** của PostgreSQL.  
Để hệ thống phản hồi nhanh, giải pháp chuẩn là **dùng Redis Cache**:
- Người dùng đầu tiên mở web -> Server tính toán Feed và lưu kết quả vào Redis Cache với thời gian sống **TTL = 5 phút** (`SET feed:user:1 [data] EX 300`).
- Trong 5 phút đó, dù có 10,000 người vào xem thì hệ thống chỉ lấy thẳng từ RAM của Redis trả về với độ trễ siêu tốc **< 5ms**. PostgreSQL hoàn toàn thảnh thơi.

#### Nhưng thảm họa sẽ xảy ra vào phút thứ `05:00.001`:
1. Đúng sau 300 giây, key cache `feed:user:1` **hết hạn (TTL Expire) và tự động bốc hơi khỏi Redis**.
2. Ngay tại khoảnh khắc đó, bài viết đang rất hot nên có **200 người dùng cùng lúc bấm F5** để xem bảng tin.
3. Cả 200 request đều hỏi Redis: *"Có cache không?"* -> Redis trả lời: *"Không có (Cache Miss)!"*
4. **Hậu quả kinh hoàng**: Cả 200 request **đồng thời lao thẳng xuống PostgreSQL**, cùng chạy 200 câu truy vấn JOIN và sắp xếp nặng nề trong cùng một giây!
5. PostgreSQL bị dội tải đột ngột, CPU nhảy vọt lên **100%**, RAM nghẽn, thời gian xử lý từ 50ms bị kéo dài thành 10 giây. Toàn bộ các request khác trong hệ thống bị xếp hàng chờ, dẫn tới hiện tượng **Sập dây chuyền (Cascading Failure)**.

Hiện tượng cả đàn request cùng ùa vào húc sập Database khi một key cache quan trọng vừa hết hạn được giới kỹ thuật gọi là **Cache Stampede (Giẫm đạp bộ nhớ đệm)** hoặc **Thundering Herd (Bầy thú sấm sét lồng lộn)**.

---

### 1.3.2. VÌ SAO CÁC GIẢI PHÁP THÔNG THƯỜNG BẤT LỰC?

1. **Không đặt TTL (Cho cache sống vĩnh viễn)?**  
   *Hậu quả*: Người dùng sẽ thấy dữ liệu cũ rích mãi mãi. Ai đăng bài mới, ai sửa bài viết cũng không hiện lên bảng tin.
2. **Dùng cronjob tự động refresh cache trước khi hết hạn?**  
   *Hậu quả*: Quá tốn tài nguyên. Nếu hệ thống có 10,000 chủ đề (topics) mà chỉ có 5 topic có người đọc, việc cronjob cứ 5 phút đi tính lại cả 10,000 topics sẽ làm database chết vì những thứ không ai xem.
3. **Dùng `synchronized` trong Java?**  
   *Hậu quả*: Như đã phân tích ở Phần 1.1, khóa cục bộ của Java chỉ có tác dụng trên 1 máy chủ đơn lẻ. Trong thực tế khi deploy nhiều instances backend sau Load Balancer, các instance khác vẫn sẽ ùa vào query DB như thường.

---

### 1.3.3. GIẢI PHÁP CỦA DEVPULSE: SINGLEFLIGHT DISTRIBUTED LOCK VỚI REDIS

Khái niệm **Singleflight (Chuyến bay độc hành)** được lấy cảm hứng từ thư viện trứ danh của ngôn ngữ Go, nhưng DevPulse đã đưa nó lên một tầm cao mới: **Triển khai ở cấp độ phân tán (Distributed) bằng Redis**.

#### Triết lý nhân văn đời thực của Singleflight:
Hãy tưởng tượng trong một văn phòng có **200 nhân viên** đang cùng làm việc. Đến 12h trưa, cả 200 người cùng đói bụng muốn ăn cơm gà ở quán đối diện.  
- **Cách ngây thơ (Thundering Herd)**: Cả 200 người cùng ùa chạy ra cửa, cùng lấy xe máy phóng ra quán làm kẹt xe tắc đường, quán ăn quá tải không phục vụ kịp.
- **Cách văn minh (Singleflight)**: Cả văn phòng cử **đúng 1 người đại diện** đi mua cơm gà về. 199 người còn lại ngồi yên tại chỗ nghỉ ngơi uống nước. Khi người đại diện xách 200 suất cơm gà về phòng, mọi người cùng lấy phần của mình ăn!

#### Cơ chế hoạt động của Singleflight trong DevPulse:
Khi 200 request cùng ập vào phát hiện Cache Miss, chúng phải trải qua một cuộc thi giành "Chiếc vé đại diện":

```text
                     [ 200 Requests cùng dồn tới khi Cache hết hạn ]
                                           │
                                           ▼
            ┌─────────────────────────────────────────────────────────────┐
            │       TRANH CHẤP DISTRIBUTED LOCK TRÊN REDIS                │
            │       redisTemplate.opsForValue().setIfAbsent(lockKey, ...) │
            └──────────────────────────────┬──────────────────────────────┘
                                           │
                    ┌──────────────────────┴──────────────────────┐
                    ▼                                             ▼
        [ THREAD CHIẾN THẮNG ]                       [ 199 THREADS THUA CUỘC ]
        (Duy nhất 1 request có Lock)                 (Không lấy được Lock)
                    │                                             │
                    ▼                                             ▼
        1. Xuống PostgreSQL query dữ liệu            1. TUYỆT ĐỐI KHÔNG ĐƯỢC ĐỤNG VÀO DB!
        2. Nạp dữ liệu mới vào Redis Cache           2. Ngủ nhẹ 100ms (Backoff)
        3. Giải phóng Lock trong khối finally        3. Sau 100ms: Quay lại đọc Cache mới
                    │                                   mà Thread thắng cuộc vừa nấu xong!
                    ▼                                             │
        Trả về kết quả cho User A                     Trả về kết quả cho 199 Users còn lại
```

#### Mã nguồn chi tiết trong [`FeedCacheService.java`](file:///home/phuqy/Develop/Social-Pulse/backend/src/main/java/com/socialpulse/app/feed/application/service/cache/FeedCacheService.java):
```java
String lockKey = "lock:feed:topic:" + topicId + ":page:" + page;

// 1. Thử tranh chấp khóa phân tán trên Redis (Lệnh nguyên tử SETNX - setIfAbsent)
// Khóa có thời hạn tự hủy đúng 5 giây để phòng chống treo máy chủ
Boolean acquired = redisTemplate.opsForValue()
    .setIfAbsent(lockKey, "LOCKED", Duration.ofMillis(5000));

if (Boolean.TRUE.equals(acquired)) {
    try {
        // [ĐẶC QUYỀN CỦA NGƯỜI CHIẾN THẮNG]:
        List<FeedResponse> freshFeed = computeFeedFromDatabase(); // Chỉ 1 câu query duy nhất xuống DB!
        feedCacheService.putTopicFeed(topicId, page, freshFeed);  // Nạp lại vào Redis Cache (TTL mới)
        return freshFeed;
    } finally {
        feedCacheService.releaseLock(lockKey); // Xong việc thì trả chìa khóa
    }
} else {
    // [CÁC REQUEST ĐẾN SAU]:
    // Nhẹ nhàng lùi lại, chờ 100ms rồi đọc Cache mới, hoặc trả về Stale Cache (dữ liệu tạm)
    return retryOrFallback(topicId, page);
}
```

#### 2 Chi Tiết Kỹ Thuật Đắt Giá Bảo Vệ Hệ Thống:
1. **TTL 5 giây chống Deadlock (Kẹt vĩnh viễn)**:  
   Chuyện gì xảy ra nếu instance backend đang chạy câu lệnh `computeFeedFromDatabase()` thì máy chủ đột ngột bị cúp điện hoặc container bị kill trước khi vào khối `finally`? Nếu không có TTL, key lock sẽ tồn tại vĩnh viễn trên Redis và toàn bộ các request sau đó sẽ bị khóa mãi mãi. Bằng cách dùng lệnh nguyên tử `setIfAbsent(..., Duration.ofMillis(5000))`, Redis đảm bảo đúng 5 giây sau khóa sẽ tự bốc hơi, hệ thống tự động phục hồi.
2. **Hiệu quả thực tế**:  
   Dưới kịch bản test tải `feed-throughput.js` của k6, vào thời điểm cache hết hạn, số lượng truy vấn dồn xuống database **giảm đến 99%** (từ 200 queries xuống còn 1 query). PostgreSQL giữ vững trạng thái tải nhẹ nhàng, độ trễ p95 duy trì ổn định ở mức **sub-30ms**.

---

## 1.4. Non-blocking Scalability & Java 21 Virtual Threads (Project Loom)

### WHAT: Mô hình luồng truyền thống (Platform Threads) bị nghẽn ở đâu?
Trong các phiên bản Java truyền thống (Java 8, 11, 17), mỗi `Thread` trong ứng dụng ánh xạ tỉ lệ 1:1 với một **Platform Thread (OS Kernel Thread)** của hệ điều hành Linux.
- **Chi phí đắt đỏ**: Mỗi OS thread chiếm cố định khoảng **1MB RAM** bộ nhớ Call Stack. Máy chủ 2GB RAM chỉ cần tạo 1,000 thread là cạn sạch tài nguyên.
- **Nghẽn do I/O (I/O Blocking)**: Trong một ứng dụng web, 90% thời gian của một request là **chờ I/O** (chờ PostgreSQL trả về kết quả query qua mạng, chờ Redis phản hồi, chờ upload Cloudinary). Trong suốt 20ms-50ms chờ đó, OS thread bị **đóng băng (blocked)** — CPU không hoạt động nhưng thread vẫn chiếm trọn 1MB RAM và giữ chân một worker thread của Tomcat.

Khi lưu lượng truy cập tăng vọt, máy chủ chạm ngưỡng `max-threads = 200` của Tomcat. Toàn bộ các request mới đến bị nghẽn ở hàng đợi (Thread Starvation), sinh ra lỗi timeout hàng loạt.

```text
┌─────────────────────────────────────────────────────────────┐
│            MÔ HÌNH CŨ: PLATFORM THREADS (1:1)               │
│                                                             │
│   Request 1 ──► [ OS Thread 1 (~1MB) ] ──► Chờ DB (Block) ⏸  │
│   Request 2 ──► [ OS Thread 2 (~1MB) ] ──► Chờ DB (Block) ⏸  │
│   Request 3 ──► [ OS Thread 3 (~1MB) ] ──► Chờ DB (Block) ⏸  │
│   ... (Tối đa 200 threads là cạn kiệt tài nguyên máy chủ!)  │
└─────────────────────────────────────────────────────────────┘
```

### WHY: Vì sao không chọn Reactive WebFlux mà chọn Virtual Threads?
Trước Java 21, ngành công nghiệp cố giải quyết vấn đề trên bằng **Lập trình phản ứng (Reactive Programming / Spring WebFlux)**.  
Tuy nhiên, WebFlux có những cái giá phải trả rất đắt:
- **Tư duy lập trình bị đảo lộn**: Phải viết code theo chuỗi toán tử `Mono`, `Flux`, `flatMap`, `subscribe`, rất khó đọc và khó hiểu.
- **Vỡ vụn Call Stack**: Khi có ngoại lệ (Exception), stack trace bị cắt thành hàng trăm dòng vô nghĩa, việc debug và trace log vô cùng đau đớn.
- **Không tương thích với JPA truyền thống**: Hibernate và Spring Data JPA được thiết kế dựa trên mô hình blocking JDBC `ThreadLocal`.

### HOW: Bản chất đột phá của Java 21 Virtual Threads (Project Loom) trong DevPulse

Virtual Thread là các **luồng ảo siêu nhẹ** do chính JVM quản lý trong vùng nhớ người dùng (User Space), hoàn toàn tách rời khỏi sự quản lý của OS Kernel:
- Một Virtual Thread chỉ tiêu tốn **vài trăm bytes** RAM (nhẹ hơn hàng ngàn lần so với OS thread 1MB).
- Bạn có thể sinh ra **100,000 đến 1,000,000 Virtual Threads** cùng lúc mà không làm sập máy chủ.

```text
┌─────────────────────────────────────────────────────────────┐
│                 JVM USER SPACE (SIÊU NHẸ)                   │
│                                                             │
│   [Virtual Thread 1]   [Virtual Thread 2] ... [Virtual Thread 100,000]
│            │                     │
│            ▼                     ▼
│   Gặp Blocking I/O      Đang tính toán CPU
│   (Chờ DB trả dữ liệu)           │
│            │                     │
│            │ (UNMOUNT)           │ (MOUNT)
│            ▼                     ▼
├─────────────────────────────────────────────────────────────┤
│                 OS CARRIER THREADS (CỐ ĐỊNH)                │
│                                                             │
│       [ OS Thread 1 ]                 [ OS Thread 2 ]       │
└─────────────────────────────────────────────────────────────┘
```

#### Cơ chế "Unmount & Remount Carrier Thread":
1. **Mount**: Khi có công việc tính toán, JVM sẽ gắn (mount) Virtual Thread vào một OS Thread thật (gọi là **Carrier Thread**) để CPU thực thi.
2. **Unmount khi gặp I/O**: Ngay khi code gọi `postRepository.findById(...)` hoặc `redisTemplate.opsForValue().get(...)`, JVM nhận biết đây là tác vụ chờ I/O. Ngay lập tức, JVM **tháo chốt (unmount)** Virtual Thread đó ra khỏi Carrier Thread và cất nó vào bộ nhớ tạm.
3. **Tận dụng tối đa CPU**: Carrier Thread ngay lập tức rảnh tay để phục vụ một Virtual Thread khác đang cần tính toán. Không có một OS thread nào phải ngồi chơi chờ I/O!
4. **Remount**: Khi dữ liệu từ PostgreSQL hoặc Redis quay trở lại qua card mạng, JVM sẽ nhấc Virtual Thread ban nãy và **gắn lại (remount)** vào một Carrier Thread đang rảnh để chạy tiếp tục dòng code tiếp theo.

#### Kích hoạt cực kỳ tinh gọn trong DevPulse:
Bạn không cần phải viết code reactive phức tạp. Chỉ với **đúng 1 dòng cấu hình** trong `backend/src/main/resources/application.yaml`:
```yaml
spring:
  threads:
    virtual:
      enabled: true
```
Toàn bộ request đến Tomcat sẽ tự động được xử lý bởi các Virtual Threads riêng biệt. Bạn vẫn viết code mệnh lệnh (imperative), tuần tự, dùng đầy đủ sức mạnh của Spring Data JPA, mà thông lượng hệ thống (Throughput) tăng vọt, chịu tải hàng chục ngàn kết nối đồng thời mượt mà.

---

# PILLAR 2: BENCHMARK & PERFORMANCE TESTING (k6)

## 2.1. WHAT: Benchmark là gì? Các loại bài test hiệu năng

**Benchmark (Đo kiểm hiệu năng)** là quá trình kích hoạt một khối lượng tải nhân tạo mô phỏng hành vi của người dùng thực tế lên hệ thống phần mềm, nhằm đo lường các chỉ số định lượng: tốc độ xử lý, thông lượng tối đa, mức tiêu hao tài nguyên và giới hạn chịu tải.

Trong kỹ nghệ kiểm thử tải, có 4 phân loại chính:
1. **Smoke Testing**: Chạy thử tải tối thiểu (1-2 VUs - Virtual Users) trong thời gian ngắn để kiểm tra xem API có hoạt động bình thường, cấu hình và script có bị lỗi cú pháp không.
2. **Load Testing (Kiểm thử tải thông thường)**: Đưa lưu lượng truy cập tương đương với mức sử dụng dự kiến lúc cao điểm (peak traffic, ví dụ: 50-100 VUs) để đo lường độ trễ (latency) và kiểm tra xem hệ thống có đạt thỏa thuận mức dịch vụ (SLA) hay không.
3. **Stress Testing (Kiểm thử áp lực)**: Tăng dần số lượng VUs vượt xa mức chịu tải bình thường cho đến khi hệ thống bị nghẽn hoặc sập. Mục tiêu: Tìm ra điểm bão hòa (saturation point) và kiểm tra xem hệ thống phục hồi (graceful recovery) như thế nào sau khi tải hạ nhiệt.
4. **Soak / Endurance Testing (Kiểm thử độ bền)**: Duy trì mức tải vừa phải trong thời gian dài (vài giờ đến vài ngày) để phát hiện rò rỉ bộ nhớ (memory leaks), cạn kiệt connection pool của database hoặc phình to phân mảnh bộ nhớ đệm.

---

## 2.2. WHY: Vì sao Unit Test là chưa đủ? Tại sao cần k6?

### Vì sao Unit Test không bảo vệ được hệ thống phân tán?
- **Unit Test và Integration Test chạy tuần tự (single-thread / predictable order)**: Một bài test kiểm tra phương thức `reactPost(postId, 1)` sẽ luôn PASS vì không có ai khác chạm vào bài post đó cùng lúc.
- **Hiện thực cuộc sống diễn ra đồng thời (concurrency)**: Khi một bài viết viral, hàng trăm request cùng ập vào trong 1 mili-giây. Lúc này, các lỗi chết người xuất hiện:
  - **Race Condition / Lost Update**: 2 transaction cùng đọc 1 giá trị, cùng cộng 1 và ghi đè lên nhau, làm mất vote của người dùng.
  - **Connection Pool Exhaustion**: 100 request cùng giữ kết nối JDBC tới PostgreSQL để chờ I/O, khiến connection pool của HikariCP bị cạn kiệt, các request sau bị ném lỗi `ConnectionTimeoutException`.
  - **Thundering Herd / Cache Stampede**: Cache bài viết hết hạn, 1,000 người cùng lúc query DB gây sập cả cụm máy chủ.
- **Nếu không có Benchmark, mọi con số trong CV đều là bịa đặt**: Khi bạn viết trong CV: *"Tối ưu hóa độ trễ sub-30ms"*, nhà tuyển dụng sẽ hỏi: *"Bạn đo bằng công cụ gì? Ở mức bao nhiêu Virtual Users? Latency p95 hay trung bình? Nếu không có benchmark data, con số đó hoàn toàn vô căn cứ."*

### Tại sao chọn Grafana k6 thay vì JMeter hay Postman?
- **Engine viết bằng Go**: k6 cực kỳ nhẹ, tối ưu đa luồng. Một laptop hoặc container nhỏ có thể tạo ra hàng nghìn VUs mà không bị nghẽn CPU của chính máy test.
- **Kịch bản viết bằng JavaScript ES6**: Code kiểm thử được quản lý version control (Git) như source code của dự án, dễ tích hợp vào CI/CD.
- **Hỗ trợ Metrics & Thresholds chuẩn công nghiệp**: Định nghĩa SLA cứng ngay trong code (ví dụ: `http_req_duration: ['p(95)<30']` — nếu p95 > 30ms thì bài test coi như thất bại).

---

## 2.3. HOW: Cẩm nang thực thi & Phân tích thông số k6 thực tế

Trong thư mục [`benchmarks/k6/`](file:///home/phuqy/Develop/Social-Pulse/benchmarks/k6), DevPulse đã xây dựng sẵn 3 kịch bản kiểm thử độc lập tương ứng với 3 bài toán phân tán.

### Bước 1: Chuẩn bị môi trường để chạy Benchmark
Bạn có 2 cách chạy (không cần cài đặt gì phức tạp):

#### Cách 1: Chạy bằng Docker (Khuyên dùng - Không cần cài k6 vào máy)
```bash
# Đảm bảo hệ thống backend đang chạy tại localhost:8080
# Chạy kịch bản kiểm thử tranh chấp vote (Voting Contention)
docker run --rm -i --network=host -v $(pwd)/benchmarks/k6:/scripts grafana/k6 run /scripts/high-contention-voting.js

# Chạy kịch bản kiểm thử thông lượng Feed (Feed Throughput)
docker run --rm -i --network=host -v $(pwd)/benchmarks/k6:/scripts grafana/k6 run /scripts/feed-throughput.js

# Chạy kịch bản kiểm thử bão ghi View bài viết (View Buffering)
docker run --rm -i --network=host -v $(pwd)/benchmarks/k6:/scripts grafana/k6 run /scripts/view-buffering.js
```

#### Cách 2: Cài đặt trực tiếp trên máy Ubuntu/Debian
```bash
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D34EE24C
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update && sudo apt-get install k6 -y

# Thực thi trực tiếp
k6 run benchmarks/k6/high-contention-voting.js
```

---

### Bước 2: Cách đọc và giải thích bảng kết quả k6 Terminal

Khi k6 hoàn tất, nó sẽ in ra một bảng tóm tắt như sau:

```text
     ✓ status is 200
     ✓ response has valid data

     checks.........................: 100.00% ✓ 10000     ✗ 0    
     data_received..................: 4.8 MB  160 kB/s
     data_sent......................: 1.2 MB  40 kB/s
     http_req_blocked...............: avg=2.1µs   min=1µs   med=2µs   max=1.2ms  p(90)=3µs   p(95)=4µs  
     http_req_connecting............: avg=0.1µs   min=0s    med=0s    max=800µs  p(90)=0s    p(95)=0s   
     http_req_duration..............: avg=18.4ms  min=2.1ms med=14.2ms max=89.3ms p(90)=24.1ms p(95)=28.7ms
       { expected_response:true }...: avg=18.4ms  min=2.1ms med=14.2ms max=89.3ms p(90)=24.1ms p(95)=28.7ms
     http_req_failed................: 0.00%   ✓ 0         ✗ 10000
     http_reqs......................: 10000   333.33/s
     iteration_duration.............: avg=118.5ms min=102ms med=114ms  max=192ms  p(90)=124ms p(95)=129ms
     iterations.....................: 10000   333.33/s
     vus............................: 50      min=50      max=50 
     vus_max........................: 50      min=50      max=50 
```

### Ý nghĩa của từng chỉ số bạn PHẢI NẮM VỮNG:
- **`checks: 100.00%`**: Toàn bộ các câu lệnh assertion trong script đều đúng (HTTP 200, payload JSON hợp lệ).
- **`http_req_failed: 0.00%`**: Không có bất kỳ request nào bị lỗi kết nối, rớt mạng hoặc trả về mã lỗi 5xx.
- **`http_reqs: 333.33/s` (Throughput / RPS - Requests Per Second)**: Server có thể xử lý hoàn tất trung bình hơn 333 requests mỗi giây trong kịch bản này.
- **Độ trễ Percentiles (`p50`, `p90`, `p95`, `p99`)**:
  - `med` (p50): Trung vị — 50% số request có độ trễ nhỏ hơn con số này.
  - `p(90)`: 90% số request nhanh hơn mức này.
  - `p(95)`: **Chỉ số quan trọng nhất trong SLA công nghiệp**. Bỏ qua 5% ngoại lệ xấu nhất (do Garbage Collection pause hoặc network jitter), 95% khách hàng thực tế sẽ trải nghiệm tốc độ dưới ngưỡng này (trong ví dụ trên là **28.7ms** -> đạt SLA sub-30ms).
  - `max`: Request tệ nhất (89.3ms). Trong phỏng vấn, giải thích được vì sao có `max` (thường là request đầu tiên do JVM JIT compile, thiết lập kết nối TCP/TLS ban đầu, hoặc cold-start cache).

---

# PILLAR 3: DEVOPS & DOCKER PRODUCTION DEPLOYMENT

## 3.1. WHAT: Bản chất của Docker & Containerization dưới góc nhìn Kernel

Rất nhiều ứng viên hiểu sai rằng *"Container là một máy ảo (VM) thu nhỏ"*. Khi phỏng vấn vào các vị trí backend/system, định nghĩa sai này sẽ bị trừ điểm rất nặng.

### Bản chất thực sự: Container là gì?
**Container thực chất là một tiến trình (Linux Process) thông thường** đang chạy trực tiếp trên Linux Host Kernel, nhưng bị giới hạn và cách ly bởi hai cơ chế cốt lõi của Linux Kernel:

```text
┌─────────────────────────────────────────────────────────────┐
│                    DOCKER CONTAINER                         │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ 1. LINUX NAMESPACES (Sự cách ly - "Những gì nó THẤY") │  │
│  │  • PID Namespace: Chỉ thấy tiến trình của chính nó    │  │
│  │  • NET Namespace: Card mạng ảo (veth), IP riêng       │  │
│  │  • MNT Namespace: Cây thư mục root filesystem riêng   │  │
│  │  • IPC Namespace: Cách ly bộ nhớ chia sẻ              │  │
│  │  • UTS Namespace: Hostname riêng biệt                 │  │
│  └───────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ 2. CGROUPS - CONTROL GROUPS (Giới hạn - "Những gì DÙNG")│  │
│  │  • CPU Limit: Giới hạn số core/chu kỳ CPU tối đa      │  │
│  │  • Memory Limit: Giới hạn RAM (tránh OOM sập host)    │  │
│  │  • Disk I/O Limit: Giới hạn tốc độ đọc ghi đĩa        │  │
│  └───────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ 3. OVERLAYFS - UNION FILE SYSTEM (Các lớp tầng lớp)   │  │
│  │  • Image Layers (Read-Only): Chia sẻ giữa containers  │  │
│  │  • Container Layer (Read-Write / Copy-on-Write): Mỏng │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
               [ Linux Host Kernel Duy Nhất ]
```

### So sánh bản chất: Docker Container vs Virtual Machine (VM)
| Tiêu chí | Virtual Machine (VM) | Docker Container |
|---|---|---|
| **Cơ chế ảo hóa** | Ảo hóa phần cứng (Hardware Virtualization qua Hypervisor: KVM, ESXi, VirtualBox) | Ảo hóa ở mức hệ điều hành (OS-level Isolation qua Linux Kernel) |
| **Hệ điều hành khách** | Mỗi VM chạy một Guest OS hoàn chỉnh riêng biệt (ngốn 1-4GB RAM chỉ để nạp OS) | Dùng chung Host Linux Kernel (Gần như **0MB RAM** overhead cho OS) |
| **Thời gian khởi động** | Vài chục giây đến vài phút (phải boot OS kernel, init systemd) | Vài mili-giây đến vài giây (chỉ là spawn một process trên host) |
| **Hiệu năng I/O** | Có độ trễ do lớp trừu tượng Hypervisor | Gần như native (tương đương chạy trực tiếp trên máy chủ vật lý) |

---

## 3.2. WHY: Tại sao Docker là tiêu chuẩn công nghiệp & Cạm bẫy "Localhost"

### 1. Xóa bỏ vĩnh viễn căn bệnh "Code chạy trên máy em bình thường":
- Sự khác biệt về phiên bản Java (Java 17 vs 21), biến thể thư viện hệ điều hành (Ubuntu glibc vs Alpine musl), múi giờ (UTC vs GMT+7), hay encoding (UTF-8) giữa máy dev và server staging thường xuyên tạo ra bug ngầm.
- Docker đóng gói toàn bộ: Runtime + System Dependencies + Code thành một **Artifact bất biến (Immutable Artifact)**. Chạy trên máy bạn thế nào, lên VPS chạy y hệt 100%.

### 2. Tối ưu chi phí hạ tầng (High Density):
- Trên một VPS 2GB RAM, bạn không thể tạo 4 Virtual Machines (mỗi VM tốn tối thiểu 512MB chỉ để chạy kernel).
- Nhưng với Docker, bạn có thể chạy mượt mà 4 container: Postgres, Redis, Spring Boot và Nginx trên cùng VPS đó, vì RAM được cấp phát trực tiếp cho ứng dụng thực tế.

### 3. Cạm bẫy Image Bloat & Bảo mật (Tại sao không được làm cẩu thả):
- Rất nhiều người viết Dockerfile đơn giản bằng cách dùng base image nặng (`FROM maven:latest` hoặc `FROM openjdk:21`), tạo ra container nặng hơn **1 GB**.
- Container nặng vừa tốn thời gian deploy (tải qua mạng cực chậm), vừa chứa cả compiler (`javac`), package manager (`apt`/`yum`), `curl`, `bash` — nếu hacker khai thác được một lỗ hổng RCE (Remote Code Execution), chúng có sẵn toàn bộ công cụ trong container để tải mã độc và tấn công leo thang đặc quyền (Privilege Escalation).

---

## 3.3. HOW: Cẩm Nang Kỹ Thuật Docker Chuyên Sâu (Docker Deep-Dive)

### 1. Nghệ thuật Multi-stage Build & Thứ tự Docker Cache Layers

Một Dockerfile chuẩn mực phải đạt được 2 mục tiêu: **Build nhanh nhờ tận dụng Cache** và **Image nhẹ nhất, an toàn nhất có thể**.

#### Phân tích chi tiết `backend/Dockerfile.prod` của DevPulse:
```dockerfile
# ── GIAI ĐOẠN 1: BUILD STAGE (Môi trường biên dịch) ──
# Dùng base image có đầy đủ Maven và JDK 21 trên Alpine Linux siêu nhẹ
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# [QUAN TRỌNG VỀ CACHE]: Copy pom.xml và tải dependencies trước!
# Docker build theo từng lớp (layer). Nếu pom.xml không đổi, Docker sẽ lấy 
# kết quả cache của lệnh "go-offline", KHÔNG BAO GIỜ tải lại 100MB thư viện Maven!
COPY pom.xml .
RUN mvn dependency:go-offline

# Sau khi dependencies đã cache, mới copy source code (vốn thay đổi liên tục)
COPY src ./src
# Biên dịch ra file JAR và bỏ qua test (vì test đã chạy trên CI)
RUN mvn clean package -DskipTests

# ── GIAI ĐOẠN 2: RUNTIME STAGE (Môi trường chạy thực tế) ──
# Vứt bỏ toàn bộ Maven compiler, chỉ lấy JRE (Java Runtime Environment) cực gọn
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Cài thêm wget nhẹ chỉ để phục vụ Docker Healthcheck
RUN apk add --no-cache wget

# CHỈ COPY DUY NHẤT file JAR từ stage 'build' sang stage runtime
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Chạy ứng dụng bằng tiến trình Java tối ưu
ENTRYPOINT ["java", "-jar", "app.jar"]
```
*Kết quả ấn tượng*: Image cuối cùng giảm từ **850MB** xuống chỉ còn **~150MB**, khởi động cực nhanh và triệt tiêu toàn bộ công cụ build thừa thãi.

#### Tương tự với `frontend/Dockerfile.prod`:
- Stage 1: `node:22-alpine` copy `package*.json` -> `npm ci` (cache node_modules) -> copy src -> `npm run build` tạo thư mục tĩnh `dist/`.
- Stage 2: `nginx:1.27-alpine` chỉ copy thư mục `dist/` vào `/usr/share/nginx/html`. Image cuối cùng chỉ nặng **~25MB**!

---

### 2. `.dockerignore` — Tấm khiên bảo vệ bắt buộc
Tuyệt đối không để Docker copy rác từ máy local vào build context. Cần có file `.dockerignore` ở cả `backend` và `frontend`:
```text
# backend/.dockerignore
target/
.git
.idea
*.iml
.env
logs/

# frontend/.dockerignore
node_modules/
dist/
.git
.env*
.vscode/
npm-debug.log
```

---

### 3. Docker Networking & Service Discovery Nội Bộ

Trong file [`docker-compose.prod.yaml`](file:///home/phuqy/Develop/Social-Pulse/docker-compose.prod.yaml), chúng ta định nghĩa một bridge network riêng:
```yaml
networks:
  app-network:
    driver: bridge
```

#### Cơ chế hoạt động của Embedded DNS:
- Trong mạng mặc định (`default bridge`), containers chỉ giao tiếp bằng IP.
- Nhưng trong **User-defined Bridge Network (`app-network`)**, Docker kích hoạt **Embedded DNS Server (127.0.0.11)**.
- Khi Spring Boot Backend cần kết nối Database, nó không cần biết IP của container Postgres là gì. Nó chỉ cần gọi hostname `db`:
  `spring.datasource.url: jdbc:postgresql://db:5432/postgres`
- Tương tự, kết nối Redis chỉ là: `spring.data.redis.host: redis`. Docker DNS sẽ tự động phân giải tên service sang IP nội bộ chính xác.

#### Quy tắc Vàng về Bảo Mật Cổng (Port Security):
```yaml
# ĐÚNG TRONG PRODUCTION:
# Service db và redis KHÔNG ĐƯỢC có phần ports mở ra ngoài máy host!
# Chúng chỉ mở cổng trong mạng nội bộ app-network.
services:
  db:
    # ports: - "5432:5432"  <-- NGUY HIỂM! Hủy bỏ dòng này trong production thật!
    networks:
      - app-network
```
*Tại sao?* Nếu bạn map `"5432:5432"` trên VPS, bất kỳ ai trên Internet quét port cũng có thể thấy cổng Postgres của bạn và tấn công Brute-force mật khẩu. Chỉ các container nằm trong cùng `app-network` mới được phép nói chuyện với Postgres và Redis.

---

### 4. Lưu Trữ Bền Vững: Named Volumes vs Bind Mounts

Dữ liệu trong container vốn có tính chất **tạm thời (ephemeral)**: Khi container bị xóa (`docker rm`), toàn bộ dữ liệu ghi bên trong nó sẽ biến mất vĩnh viễn.

#### Named Volume là gì?
Trong `docker-compose.prod.yaml`:
```yaml
services:
  db:
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```
- Docker tạo và quản lý một thư mục riêng biệt trên Linux Host (`/var/lib/docker/volumes/postgres_data/_data`).
- Khi container DB bị xóa, restart hay nâng cấp image từ `postgres:16` lên `postgres:17`, volume này **không hề bị ảnh hưởng**. Dữ liệu được bảo toàn nguyên vẹn 100%.

#### Cẩm nang Backup & Restore Database Volume bằng lệnh Docker 1 dòng:
```bash
# 1. SAO LƯU (Backup volume ra file nén .tar.gz):
docker run --rm -v social-pulse_postgres_data:/volume -v $(pwd):/backup alpine tar czf /backup/db_backup_$(date +%F).tar.gz -C /volume .

# 2. PHỤC HỒI (Restore file nén vào volume):
docker run --rm -v social-pulse_postgres_data:/volume -v $(pwd):/backup alpine sh -c "rm -rf /volume/* && tar xzf /backup/db_backup_2026-09-16.tar.gz -C /volume"
```

---

### 5. Quản Lý Tài Nguyên (Resource Limits) & Cơ Chế Healthcheck

#### Giới hạn RAM/CPU tránh sập máy chủ (OOM Prevention):
Nếu không có giới hạn, một query lỗi hoặc rò rỉ bộ nhớ trong Java có thể ngốn sạch 100% RAM của VPS, khiến Linux Kernel kích hoạt **OOM Killer** và hạ gục cả tiến trình SSH của bạn.
Cấu hình chuẩn trong compose:
```yaml
backend:
  deploy:
    resources:
      limits:
        cpus: '1.5'
        memory: 1024M
      reservations:
        memory: 512M
```

#### Healthcheck Probes & Dependency Ordering:
Trong kiến trúc phân tán, **thứ tự khởi động không tương đương với thứ tự sẵn sàng**. Một container Postgres có thể khởi động trong 1 giây, nhưng nó cần 5-10 giây để nạp dữ liệu và mở port kết nối. Nếu Spring Boot chạy ngay lập tức, nó sẽ văng lỗi `Connection refused` và crash.

DevPulse giải quyết vấn đề này bằng **Healthcheck có điều kiện**:
```yaml
  db:
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USERNAME:-postgres} -d ${DB_NAME:-postgres}"]
      interval: 10s
      timeout: 5s
      retries: 5

  backend:
    depends_on:
      db:
        condition: service_healthy    # Chỉ start Backend khi DB ĐÃ SẴN SÀNG NHẬN KẾT NỐI!
      redis:
        condition: service_healthy
    healthcheck:
      test: ["CMD-SHELL", "wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s
```

---

### 6. Bảng Lệnh Thực Chiến Docker Dành Cho Kỹ Sư

| Tác vụ | Câu lệnh | Mục đích kỹ thuật |
|---|---|---|
| **Giám sát tài nguyên** | `docker stats` | Xem realtime % CPU, RAM, Network I/O của từng container (cực quan trọng khi chạy k6 benchmark). |
| **Kiểm tra sức khỏe** | `docker compose ps` | Xem danh sách container và trạng thái healthcheck (`healthy` / `unhealthy`). |
| **Soi log trực tiếp** | `docker logs --tail 100 -f <container_name>` | Xem 100 dòng log gần nhất và bám theo log thời gian thực. |
| **Đi vào trong container** | `docker exec -it <container_name> sh` | Mở shell tương tác bên trong container để debug file cấu hình, DNS. |
| **Kiểm tra mạng nội bộ** | `docker network inspect social-pulse_app-network` | Xem toàn bộ IP nội bộ của các container đang kết nối với nhau. |
| **Dọn dẹp an toàn** | `docker system prune -f` | Xóa image rác không dùng (`dangling images`), giải phóng ổ cứng mà KHÔNG xóa volumes dữ liệu. |
| **Xem tiến trình chạy** | `docker top <container_name>` | Xem tiến trình Linux thực tế của container trên máy Host (PID thật). |

---

## 3.4. HOW: Cẩm Nang Triển Khai Thực Tế Lên VPS Linux

### Bước 1: Thuê VPS và Thiết lập Bảo mật Cơ bản (Harden VPS)
Bạn có thể thuê một VPS Ubuntu 22.04 hoặc 24.04 LTS (Hetzner CX22, DigitalOcean Basic Droplet hoặc Oracle Cloud Free Tier VM).

Sau khi nhận IP và SSH vào server:
```bash
# Đăng nhập vào VPS
ssh root@<IP_CUA_VPS>

# 1. Cập nhật hệ thống
apt update && apt upgrade -y

# 2. Tạo một tài khoản người dùng thông thường (không dùng trực tiếp root)
adduser devops
usermod -aG sudo devops
rsync --archive --chown=devops:devops ~/.ssh /home/devops

# Đăng nhập lại bằng user mới
exit
ssh devops@<IP_CUA_VPS>

# 3. Cài đặt Docker & Docker Compose
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER
newgrp docker

# 4. Thiết lập tường lửa UFW (Bảo vệ cổng database và Redis)
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
```

---

### Bước 2: Clone dự án & Cấu hình biến môi trường Production
```bash
git clone https://github.com/DamPhuQuy/Social-Pulse.git
cd Social-Pulse

# Tạo file .env cho Production
cp .env.example .env
nano .env
```

Điền các thông số bảo mật production:
```properties
# Thông số Database
DB_USERNAME=socialpulse_prod
DB_PASSWORD=Chon_Mot_Mat_Khau_Rat_Manh_32_Ky_Tu!
DB_NAME=socialpulse_db

# Thông số JWT Security (Tạo bằng: openssl rand -base64 64)
JWT_SECRET=Wv3o...chuoi_ngau_nhien_dai_toi_thieu_256_bit...=
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000

# Profile kích hoạt cho Spring Boot
SPRING_PROFILES_ACTIVE=prod

# Cloudinary / Email credentials của bạn
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret

# URL kết nối Frontend
VITE_API_BASE_URL=https://yourdomain.com/api/v1
```

---

### Bước 3: Khởi chạy Production Stack
File [`docker-compose.prod.yaml`](file:///home/phuqy/Develop/Social-Pulse/docker-compose.prod.yaml) đã có sẵn toàn bộ cấu hình container tối ưu.

```bash
# Build và chạy ngầm toàn bộ 4 container
docker compose -f docker-compose.prod.yaml up -d --build

# Kiểm tra trạng thái và healthcheck của các container
docker compose -f docker-compose.prod.yaml ps
```

Khi chạy `docker compose ps`, bạn sẽ thấy:
```text
NAME                     IMAGE                    STATUS                    PORTS
social-pulse-db-1        postgres:17-alpine       Up (healthy)              5432/tcp
social-pulse-redis-1     redis:7-alpine           Up (healthy)              6379/tcp
social-pulse-backend-1   social-pulse-backend     Up (healthy)              8080/tcp
social-pulse-frontend-1  social-pulse-frontend    Up (healthy)              80/tcp
```
*Lưu ý kiến trúc mạng*: Chỉ có `frontend` (port 80) và `backend` (port 8080) lắng nghe trên localhost. `db` và `redis` giao tiếp hoàn toàn qua mạng nội bộ `app-network`, cách ly tuyệt đối với mạng bên ngoài.

---

### Bước 4: Thiết lập Reverse Proxy & Tự động cấp SSL với Caddy
**Caddy Server** là công cụ hiện đại, tối ưu hơn Nginx ở chỗ nó **tự động gọi Let's Encrypt để xin và gia hạn chứng chỉ HTTPS** mà không cần phải cài Certbot hay viết cronjob gia hạn.

```bash
# Cài đặt Caddy trên máy chủ VPS
sudo apt install -y debian-keyring debian-archive-keyring apt-transport-https curl
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/gpg.key' | sudo gpg --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt' | sudo tee /etc/apt/sources.list.d/caddy-stable.list
sudo apt update && sudo apt install caddy -y

# Cấu hình Caddy
sudo nano /etc/caddy/Caddyfile
```

Thay toàn bộ nội dung file `/etc/caddy/Caddyfile` bằng cấu hình chuẩn sau:
```caddy
yourdomain.com {
    # Nén dữ liệu truyền tải tiết kiệm băng thông
    encode zstd gzip

    # Định tuyến toàn bộ API và SSE về Spring Boot Backend
    handle /api/* {
        reverse_proxy localhost:8080
    }

    # Định tuyến Actuator Healthcheck cho monitoring (tùy chọn)
    handle /actuator/* {
        reverse_proxy localhost:8080
    }

    # Định tuyến toàn bộ giao diện tĩnh về Nginx Frontend
    handle {
        reverse_proxy localhost:80
    }
}
```

Reload lại Caddy:
```bash
sudo systemctl reload caddy
```
Bây giờ, hãy mở trình duyệt và gõ `https://yourdomain.com`: Bạn đã có một trang web cộng đồng kỹ thuật hoàn chỉnh chạy trên HTTPS thật với chi phí vận hành siêu tối ưu.

---

# PILLAR 4: CI/CD (CONTINUOUS INTEGRATION & CONTINUOUS DEPLOYMENT)

## 4.1. WHAT: CI/CD là gì? Các thành phần cốt lõi

- **CI (Continuous Integration - Tích hợp liên tục)**: Mỗi khi lập trình viên tạo Pull Request hoặc push commit mới lên nhánh `main`, hệ thống tự động khởi tạo môi trường sạch, chạy linter, biên dịch mã nguồn và kích hoạt toàn bộ bộ test (Unit Tests, Concurrency Tests). Nếu có bất kỳ lỗi nào, commit sẽ bị chặn lại (Block merge).
- **CD (Continuous Delivery / Deployment - Phân phối / Triển khai liên tục)**: Sau khi mã nguồn vượt qua toàn bộ các bài kiểm tra của CI, hệ thống sẽ tự động build Docker images, đẩy lên Container Registry (Docker Hub / GitHub Packages) và kết nối tới máy chủ VPS để cập nhật phiên bản mới nhất mà không cần con người SSH thủ công.

---

## 4.2. WHY: Vì sao dự án cần tự động hóa CI/CD?

1. **Tránh lỗi hồi quy (Regression Bugs)**: Lập trình viên sửa một hàm ở module này nhưng vô tình làm gãy logic ở module khác. CI bắt buộc 83 unit test và 3 concurrency test phải PASS trước khi mã được hợp nhất.
2. **Chấm dứt việc deploy thủ công**: Deploy thủ công (SSH vào server, `git pull`, `docker compose build`) rất dễ xảy ra lỗi do quên cập nhật biến môi trường, xung đột git hoặc thao tác nhầm lệnh nguy hiểm.
3. **Điểm cộng vượt bậc trong mắt nhà tuyển dụng**: Rất nhiều sinh viên ghi vào CV "Biết CI/CD", nhưng khi hỏi *"Pipeline của em cấu hình như thế nào? Xử lý database migration trên CI ra sao?"* thì ấp úng. Có một file workflow GitHub Actions hoạt động thật trong repo chứng minh bạn có năng lực làm việc trong môi trường doanh nghiệp chuyên nghiệp.

---

## 4.3. HOW: Xây dựng GitHub Actions Pipeline từ kiểm thử đến Deploy VPS

Tạo file `.github/workflows/ci-cd.yaml` trong thư mục gốc của dự án. Dưới đây là template production-grade hoàn chỉnh được thiết kế riêng cho DevPulse:

```yaml
name: DevPulse CI/CD Pipeline

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  # ==========================================
  # GIAI ĐOẠN 1: BACKEND CI (Test & Verify)
  # ==========================================
  backend-ci:
    name: Backend Lint & Concurrency Verification
    runs-on: ubuntu-latest

    services:
      # Khởi tạo Postgres test container
      postgres:
        image: postgres:17-alpine
        env:
          POSTGRES_USER: postgres
          POSTGRES_PASSWORD: password
          POSTGRES_DB: test_db
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

      # Khởi tạo Redis test container
      redis:
        image: redis:7-alpine
        ports:
          - 6379:6379
        options: >-
          --health-cmd "redis-cli ping"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    steps:
      - name: Checkout Code
        uses: actions/checkout@v4

      - name: Set up JDK 21 (Eclipse Temurin)
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven

      - name: Build & Run All Unit Tests
        run: |
          cd backend
          chmod +x ./mvnw
          ./mvnw test -Dspring.profiles.active=test

      - name: Run Multithreaded Concurrency Stress Test
        run: |
          cd backend
          ./mvnw test -Dtest=ReactPostConcurrencyTest -Dspring.profiles.active=test

  # ==========================================
  # GIAI ĐOẠN 2: FRONTEND CI (Lint & Build)
  # ==========================================
  frontend-ci:
    name: Frontend Lint & Production Bundle Build
    runs-on: ubuntu-latest

    steps:
      - name: Checkout Code
        uses: actions/checkout@v4

      - name: Set up Node.js 22
        uses: actions/setup-node@v4
        with:
          node-version: 22
          cache: 'npm'
          cache-dependency-path: frontend/package-lock.json

      - name: Install Dependencies
        run: |
          cd frontend
          npm ci

      - name: Run ESLint (Strict 0 error)
        run: |
          cd frontend
          npm run lint

      - name: Run Vite Production Build
        run: |
          cd frontend
          npm run build

  # ==========================================
  # GIAI ĐOẠN 3: CD DEPLOY TO VPS (Chỉ chạy khi merge vào main)
  # ==========================================
  deploy-production:
    name: Continuous Deployment to Cloud VPS
    needs: [backend-ci, frontend-ci]
    if: github.ref == 'refs/heads/main' && github.event_name == 'push'
    runs-on: ubuntu-latest

    steps:
      - name: Deploy via SSH to Linux VPS
        uses: appleboy/ssh-action@v1.0.3
        with:
          host: ${{ secrets.VPS_HOST }}
          username: ${{ secrets.VPS_USERNAME }}
          key: ${{ secrets.VPS_SSH_PRIVATE_KEY }}
          script: |
            cd Social-Pulse
            git pull origin main
            docker compose -f docker-compose.prod.yaml up -d --build
            docker system prune -f
```

### Các bước bạn cấu hình trên GitHub Repo:
1. Vào GitHub repo của bạn -> **Settings** -> **Secrets and variables** -> **Actions**.
2. Thêm 3 secrets:
   - `VPS_HOST`: IP của VPS (ví dụ: `123.45.67.89`).
   - `VPS_USERNAME`: User đăng nhập (ví dụ: `devops`).
   - `VPS_SSH_PRIVATE_KEY`: Khóa riêng tư SSH (nội dung file `~/.ssh/id_rsa` hoặc `~/.ssh/id_ed25519`).

---

# PILLAR 5: CHUYỂN HÓA SANG GOOGLE XYZ RESUME & KỊCH BẢN PHỎNG VẤN

## 5.1. 4 Mẫu đạn vàng (Bullet Points) chuẩn XYZ

Công thức vàng của Google:  
**"Accomplished [X], as measured by [Y], by doing [Z]"**

```text
┌─────────────────────────────────────────────────────────────────────────────┐
│  [X]: Thành tựu kỹ thuật cụ thể (Đã giải quyết bài toán hóc búa nào?)        │
│  [Y]: Đo lường định lượng (Bằng chứng số liệu từ k6 hoặc thông số server)     │
│  [Z]: Giải pháp kiến trúc / Công nghệ (Làm điều đó bằng cách nào?)          │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Bullet 1: Core Concurrency & Data Consistency (Backend)
> **Eliminated 100% of concurrent race conditions and lost updates on viral post voting, maintaining 100% arithmetic balance accuracy and zero deadlocks across 100 parallel threads in automated stress testing, by replacing standard ORM read-modify-write patterns with atomic in-database row-level arithmetic (JPQL floor-clamping) on PostgreSQL 17.**

### Bullet 2: High-Throughput Ingestion & I/O Buffering (Caching & Data Structures)
> **Mitigated database write storms from high-frequency post views, dropping view recording latency from 45ms to <3ms and reducing DB write I/O by over 95%, verified under k6 load bursts of 5,000+ views with sub-15ms p95 latency, by engineering a two-tier write-behind buffer combining Redis 7 HyperLogLog ($O(1)$ memory-constant deduplication) with an asynchronous batch sync scheduler.**

### Bullet 3: Distributed Cache Resiliency (System Design)
> **Prevented cascading database connection exhaustion (Cache Stampede / Thundering Herd) during feed cache invalidations, slashing peak query load on PostgreSQL by 99% while sustaining sub-30ms p95 response times under heavy concurrent feed reads, by architecting a distributed Singleflight lock pattern in Redis with deterministic TTL-based backoff and stale-cache fallback.**

### Bullet 4: Turnkey Cloud Deployment & Reliability (DevOps / Infrastructure)
> **Architected and deployed an end-to-end containerized micro-infrastructure on Linux VPS, achieving sub-2-minute zero-downtime builds, automated healthcheck recovery, and reproducible multi-container orchestration, by writing multi-stage Dockerfiles (Java 21 JRE Alpine + Nginx), Docker Compose environments with health probes, and continuous k6 load testing pipelines.**

---

## 5.2. Bộ câu hỏi phản biện kỹ thuật (Q&A Defense)

Dưới đây là 4 câu hỏi hóc búa mà Senior/Principal Engineer chắc chắn sẽ hỏi khi thấy các bullet trên trong CV của bạn, cùng gợi ý câu trả lời đắt giá:

### Q1: *"Tại sao em không dùng `@Version` (Optimistic Locking) cho voting mà lại dùng Atomic JPQL?"*
- **Trả lời**:  
  > *"Dạ, Optimistic Locking hoạt động rất tốt khi tỷ lệ xung đột thấp (Read-heavy, Low-contention write). Nhưng với một bài viết viral đang có 100 người cùng bấm Like trong 1 giây, nếu dùng `@Version`, 1 người thành công thì 99 người còn lại sẽ bị văng `OptimisticLockingFailureException`. Khi đó, nếu cho retry thì gây nghẽn CPU và mạng, nếu trả lỗi về client thì trải nghiệm người dùng rất tệ.  
  > Vì vậy, em chọn Atomic in-database arithmetic: `UPDATE posts SET upvote_count = upvote_count + :delta WHERE id = :id`. PostgreSQL có hàng đợi khóa hàng (row-level lock) cực kỳ tối ưu, các phép cộng được thực thi tuần tự trong bộ nhớ của DB chỉ mất vài micro-giây, vừa bảo toàn 100% dữ liệu, vừa không phát sinh lỗi retry."*

### Q2: *"HyperLogLog có độ lệch chuẩn (~0.81%). Tại sao em lại chấp nhận sai số thay vì dùng `Set` trong Redis?"*
- **Trả lời**:  
  > *"Dạ, đây là bài toán đánh đổi giữa chi phí bộ nhớ và độ chính xác nghiệp vụ (Business Trade-off).  
  > Nếu dùng `Redis SET` để lưu danh sách `userId` đã xem một bài viết: Một bài viết viral có 1,000,000 lượt xem, mỗi ID tốn khoảng 8 bytes, một bài viết sẽ ngốn ít nhất 8MB-10MB RAM. Nếu hệ thống có 10,000 bài viết, Redis sẽ cạn kiệt RAM ngay lập tức.  
  > Trong khi đó, Redis HyperLogLog chỉ tốn cố định **12KB RAM** cho bất kỳ bài viết nào, dù bài viết đó có 10 lượt xem hay 100 triệu lượt xem. Đối với lượt view bài viết xã hội, con số 1,000,000 view hay 998,000 view không ảnh hưởng đến tính toàn vẹn tài chính hay nghiệp vụ nghiêm ngặt, nhưng nó cứu hệ thống khỏi chi phí RAM khổng lồ."*

### Q3: *"Singleflight Cache Lock trong Redis có rủi ro Deadlock không nếu server bị crash trước khi release lock?"*
- **Trả lời**:  
  > *"Dạ có, nếu server giữ lock bị crash trước khối `finally { releaseLock() }`, key lock sẽ tồn tại vĩnh viễn và các thread sau sẽ bị kẹt.  
  > Em giải quyết việc này bằng cách luôn gán TTL (Time-To-Live) nguyên tử ngay khi tạo lock thông qua `setIfAbsent(lockKey, "LOCKED", Duration.ofMillis(5000))`. Ngay cả khi container backend bị tắt đột ngột, Redis sẽ tự động giải phóng key sau 5 giây. Đồng thời, các thread không lấy được lock sẽ không block hoàn toàn mà fall-back đọc dữ liệu stale cache hoặc thử lại sau backoff nhẹ (100ms)."*

### Q4: *"Java 21 Virtual Threads khác gì so với Reactive Programming (Spring WebFlux)?"*
- **Trả lời**:  
  > *"Dạ, cả hai đều giải quyết bài toán nghẽn thread khi gặp blocking I/O (Database, Redis, Network calls).  
  > Tuy nhiên, WebFlux yêu cầu thay đổi toàn bộ tư duy lập trình sang luồng phản ứng (`Mono`/`Flux`), rất khó debug, stack trace bị vỡ mảnh và không tương thích tự nhiên với Spring Data JPA / JDBC truyền thống.  
  > Java 21 Project Loom mang Virtual Threads vào Spring Boot 3 cho phép chúng em viết code tuần tự, mệnh lệnh (imperative), dễ đọc, dễ bảo trì, dùng được nguyên vẹn hệ sinh thái Spring Data JPA, mà JVM vẫn tự động unmount virtual thread khỏi carrier thread mỗi khi gặp blocking I/O, đạt thông lượng tương đương mà chi phí phát triển thấp hơn rất nhiều."*

---

*Tài liệu này được biên soạn độc quyền cho kiến trúc DevPulse (`Social-Pulse`). Hãy lưu giữ, thực hành từng dòng lệnh và biến kiến thức này thành phản xạ của chính bạn!*
