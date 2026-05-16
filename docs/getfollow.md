# Quy trình viết chức năng Get Followers/Following List

## Mục tiêu
Xây dựng API để lấy danh sách followers (người theo dõi) và following (đang theo dõi) của một user.

## Kiến trúc Clean Architecture
Dự án sử dụng kiến trúc phân lớp:
- **Domain Layer**: Model, Repository interface
- **Application Layer**: DTO, UseCase, Service
- **Infrastructure Layer**: Repository implementation, Entity, Mapper
- **Adapter Layer**: Controller (Web), Repository Adapter

## Thứ tự viết code

### 1. Domain Layer (Không cần thay đổi)
- ✅ Model `Follow` đã tồn tại
- ✅ Repository interface `FollowRepository` đã tồn tại

### 2. Application Layer - DTO Response

**File 1**: `backend/src/main/java/com/socialpulse/app/follow/application/dto/response/FollowerResponse.java`
- Tạo DTO để trả về thông tin người theo dõi
- Bao gồm: userId, username, fullName, avatarUrl, isFollowing (người xem có follow lại không)

**File 2**: `backend/src/main/java/com/socialpulse/app/follow/application/dto/response/FollowingResponse.java`
- Tạo DTO để trả về thông tin người đang theo dõi
- Bao gồm: userId, username, fullName, avatarUrl, isFollowingBack (người đó có follow lại không)

### 3. Infrastructure Layer - Repository

**File 3**: Cập nhật `backend/src/main/java/com/socialpulse/app/follow/domain/repository/FollowRepository.java`
- Thêm method: `List<Follow> findFollowersByUserId(Long userId, int offset, int limit)`
- Thêm method: `List<Follow> findFollowingByUserId(Long userId, int offset, int limit)`
- Thêm method: `long countFollowersByUserId(Long userId)`
- Thêm method: `long countFollowingByUserId(Long userId)`

**File 4**: Cập nhật `backend/src/main/java/com/socialpulse/app/follow/infrastructure/persistence/repository/JpaFollowRepository.java`
- Implement các query methods với pagination

**File 5**: Cập nhật `backend/src/main/java/com/socialpulse/app/follow/adapter/persistence/FollowRepositoryAdapter.java`
- Implement các method từ FollowRepository interface

### 4. Application Layer - Service

**File 6**: `backend/src/main/java/com/socialpulse/app/follow/application/service/GetFollowersService.java`
- Business logic để lấy danh sách followers
- Kiểm tra xem current user có follow lại những người này không

**File 7**: `backend/src/main/java/com/socialpulse/app/follow/application/service/GetFollowingService.java`
- Business logic để lấy danh sách following
- Kiểm tra xem những người này có follow lại current user không

### 5. Application Layer - UseCase

**File 8**: `backend/src/main/java/com/socialpulse/app/follow/application/usecase/GetFollowersUseCase.java`
- UseCase interface cho get followers

**File 9**: `backend/src/main/java/com/socialpulse/app/follow/application/usecase/GetFollowingUseCase.java`
- UseCase interface cho get following

### 6. Adapter Layer - Controller

**File 10**: Cập nhật `backend/src/main/java/com/socialpulse/app/follow/adapter/web/FollowController.java`
- Thêm endpoint: `GET /api/v1/follows/{userId}/followers`
- Thêm endpoint: `GET /api/v1/follows/{userId}/following`
- Hỗ trợ pagination với query params: page, size

## API Endpoints

### Get Followers
```
GET /api/v1/follows/{userId}/followers?page=0&size=20
```
Response:
```json
{
  "data": {
    "followers": [
      {
        "userId": 123,
        "username": "john_doe",
        "fullName": "John Doe",
        "avatarUrl": "https://...",
        "isFollowing": true
      }
    ],
    "totalCount": 150,
    "page": 0,
    "size": 20,
    "hasNext": true
  }
}
```

### Get Following
```
GET /api/v1/follows/{userId}/following?page=0&size=20
```
Response:
```json
{
  "data": {
    "following": [
      {
        "userId": 456,
        "username": "jane_smith",
        "fullName": "Jane Smith",
        "avatarUrl": "https://...",
        "isFollowingBack": false
      }
    ],
    "totalCount": 89,
    "page": 0,
    "size": 20,
    "hasNext": true
  }
}
```

## Lưu ý kỹ thuật

1. **Pagination**: Sử dụng offset-based pagination (page, size)
2. **Performance**: Cần optimize query để tránh N+1 problem khi check isFollowing/isFollowingBack
3. **Privacy**: Có thể cần kiểm tra quyền xem followers/following (nếu user set private)
4. **Caching**: Có thể cache kết quả cho các user có nhiều followers

## Thời gian ước tính
- Viết code: 2-3 giờ
- Testing: 1 giờ
- Tổng: 3-4 giờ
