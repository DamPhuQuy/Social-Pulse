# Frontend React Architecture (Sơ đồ Kiến trúc Frontend React)

Sơ đồ này mô tả cấu trúc của ứng dụng Frontend React (Vite + TS + TailwindCSS), thể hiện các thành phần giao diện (UI Components), bộ quản lý trạng thái (Context & State), các Custom Hooks và cổng giao tiếp dịch vụ (Services / API Client).

```mermaid
flowchart TD
  %% Style definitions
  classDef ui fill:#f0fdfa,stroke:#0d9488,stroke-width:2px,color:#0f766e;
  classDef context fill:#fef2f2,stroke:#ef4444,stroke-width:2px,color:#991b1b;
  classDef hook fill:#fef8e0,stroke:#d97706,stroke-width:2px,color:#92400e;
  classDef api fill:#f5f3ff,stroke:#8b5cf6,stroke-width:2px,color:#5b21b6;
  classDef ext fill:#f8fafc,stroke:#475569,stroke-width:2px,color:#334155;

  subgraph UI_Layer ["UI Layer (Components & Pages)"]
    AppLayout["AppLayout<br>(Navbar, Sidebar, Main Area)"]:::ui
    
    subgraph Pages ["Pages"]
      Home["HomePage<br>(Personalized Feed)"]:::ui
      Detail["PostDetailPage<br>(Comments & Stats)"]:::ui
      Discovery["DiscoveryPage<br>(Trending Hashtags)"]:::ui
    end

    subgraph Component_Widgets ["UI Widgets"]
      FeedList["FeedList<br>(Scroll Container)"]:::ui
      PostCard["PostCard<br>(Author, Text, Actions)"]:::ui
      PostMedia["PostMedia<br>(Image grid layout)"]:::ui
      TopicTabs["TopicTabs<br>(Category filter)"]:::ui
    end
  end

  subgraph State_Context_Layer ["State & Global Context Layer"]
    AuthCtx["AuthContext<br>(accessToken, User Info)"]:::context
    
    subgraph RealTime_Engine ["Real-Time Event Engine"]
      SSECtx["RealTimeProvider<br>(SSE Connection Manager)"]:::context
      Reader["readSseStream<br>(Custom chunk parser)"]:::context
    end
    
    subgraph Local_Component_State ["HomePage Local State"]
      FeedState["feed: FeedItem[ ]"]:::context
      PageFlags["page: number<br>hasMore: boolean"]:::context
      LoadingFlag["feedLoading: boolean"]:::context
    end
  end

  subgraph Custom_Hooks ["Custom Hooks"]
    useAuth["useAuth( )<br>(Consume AuthContext)"]:::hook
    useObs["useIntersectionObserver( )<br>(Infinite scroll trigger)"]:::hook
  end

  subgraph API_Services_Layer ["API Client & Services Layer"]
    AxiosClient["apiClient<br>(Axios with JWT Interceptor)"]:::api
    
    subgraph Services ["API Services"]
      PostSvc["postService.ts<br>(getFeed, reactPost, deletePost)"]:::api
      AuthSvc["authService.ts<br>(login, register)"]:::api
    end
  end

  subgraph Backend_API ["Backend Server"]
    SpringAPI["Spring Boot API<br>(/api/v1)"]:::ext
  end

  %% Relationships and Flows
  %% 1. Component Hierarchy
  AppLayout --> Home
  AppLayout --> Detail
  AppLayout --> Discovery
  Home --> TopicTabs
  Home --> FeedList
  FeedList --> PostCard
  PostCard --> PostMedia

  %% 2. Hook and Context Consumption
  Home -->|Reads auth state| useAuth
  Home -->|Infinite scroll detection| useObs
  Home -->|Subscribes to Event| SSECtx
  useAuth --> AuthCtx

  %% 3. API Fetching Flow
  Home -->|Trigger loadFeed() / loadMore()| PostSvc
  PostSvc --> AxiosClient
  AxiosClient -->|Authorization: Bearer JWT| SpringAPI
  PostSvc -->|Returns FeedItem[ ]| FeedState

  %% 4. Real-time Event Flow (SSE)
  SSECtx -->|Establish connection /realtime/connect| SpringAPI
  SpringAPI -.->|Event stream stream-text| SSECtx
  SSECtx -->|Pump stream buffer| Reader
  Reader -->|Parse event: 'feed_refresh'| Reader
  Reader -->|Dispatch Window Event 'realtime:feed_refresh'| Home
  Home -.->|Trigger loadFeed(0)| PostSvc
```

## 1. Các Khối Chức Năng Chính

*   **UI Layer (Giao diện người dùng):** 
    *   `HomePage` đóng vai trò là container chính hiển thị bảng tin. Nó chứa `TopicTabs` để lọc bài viết theo chủ đề và `FeedList` để quản lý cuộn dữ liệu.
    *   `PostCard` hiển thị chi tiết từng bài đăng, thông tin tác giả, tích hợp `PostMedia` (chuyển đổi chuỗi URL ngăn cách bởi dấu phẩy thành grid ảnh).
*   **State & Global Context Layer (Quản lý trạng thái):**
    *   `AuthContext`: Lưu giữ token đăng nhập của người dùng để tự động đính kèm vào tất cả các yêu cầu HTTP.
    *   `RealTimeProvider` (SSE Connection Manager): Khi người dùng đăng nhập thành công, context này sẽ mở kết nối HTTP thô qua hàm `fetch` đến endpoint `/realtime/connect` với cờ `Accept: text/event-stream`. Lớp `readSseStream` thực hiện giải mã và đọc luồng stream liên tục theo từng dòng.
*   **API & Services Layer (Cổng kết nối):**
    *   `apiClient`: Sử dụng thư viện Axios có gắn Request Interceptor để đính kèm tiêu đề `Authorization: Bearer <JWT>` tự động.
    *   `postService.ts`: Khai báo các API lấy feed (`getFeed`), tương tác bày tỏ cảm xúc (`reactPost`), và xóa bài viết (`deletePost`).

## 2. Các Luồng Nghiệp Vụ Đặc Thù

1.  **Infinite Scroll (Cuộn vô hạn):**
    *   Thành phần `HomePage` định nghĩa một div làm mốc cuối trang (footer).
    *   Custom hook `useIntersectionObserver` lắng nghe sự kiện khi người dùng cuộn màn hình chạm đến div mốc này.
    *   Khi phát hiện sự kiện chạm đáy, hook sẽ gọi hàm `loadMoreFeed()`. Hàm này thực hiện gọi `getFeed(nextPage, 20)`, lọc trùng lặp ID bài viết, và nối dữ liệu mới vào mảng `feed` hiện tại trong State.
2.  **Server-Sent Events (SSE) Real-time Refresh:**
    *   Khi backend phát đi sự kiện `feed_refresh` (ví dụ do một người dùng khác vừa cập nhật bài đăng), hàm `readSseStream` của `RealTimeProvider` sẽ giải mã chuỗi JSON sự kiện.
    *   Một sự kiện tùy biến trình duyệt (`CustomEvent`) mang tên `realtime:feed_refresh` được phát ra phạm vi toàn cục (`window.dispatchEvent`).
    *   `HomePage` có gắn event listener lắng nghe sự kiện `realtime:feed_refresh` này. Khi nhận được, nó lập tức kích hoạt làm mới bảng tin (`loadFeed(0)`) để nạp bài đăng mới nhất mà không gây phiền phức cho trải nghiệm đọc hiện tại.
