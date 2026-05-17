# PlantUML Sequence Diagrams - Social Pulse API

This directory contains comprehensive PlantUML sequence diagrams for all API endpoints in the Social Pulse backend.

## Overview

Each diagram illustrates the runtime interaction between objects/instances/classes following Clean Architecture / DDD principles:
- Client → Security Filter → Controller → Application Service/Use Case → Repository → Database
- Includes validation, authorization, error handling, and response flows

## Diagram Files

### Authentication & Authorization
- **auth-endpoints.puml**
  - POST /api/auth/register - User Registration
  - POST /api/auth/login - User Login
  - POST /api/auth/refresh - Refresh Access Token
  - POST /api/auth/logout - User Logout

### User Management
- **user-endpoints.puml**
  - GET /api/users/{userId} - Get User Profile
  - PUT /api/users/me - Update Current User Profile
  - GET /api/users/me - Get Current User Profile
  - DELETE /api/users/me - Delete Current User Account
  - GET /api/users/search - Search Users

### Post Management
- **post-endpoints.puml**
  - POST /api/posts - Create Post
  - GET /api/posts/{postId} - Get Post Details
  - PUT /api/posts/{postId} - Update Post
  - DELETE /api/posts/{postId} - Delete Post
  - GET /api/posts - Get Posts Feed
  - GET /api/posts/user/{userId} - Get User Posts

### Like Management
- **like-endpoints.puml**
  - POST /api/posts/{postId}/like - Like Post
  - DELETE /api/posts/{postId}/like - Unlike Post
  - POST /api/comments/{commentId}/like - Like Comment
  - DELETE /api/comments/{commentId}/like - Unlike Comment
  - GET /api/posts/{postId}/likes - Get Post Likes

### Comment Management
- **comment-endpoints.puml**
  - POST /api/posts/{postId}/comments - Create Comment
  - GET /api/posts/{postId}/comments - Get Post Comments
  - GET /api/comments/{commentId}/replies - Get Comment Replies
  - PUT /api/comments/{commentId} - Update Comment
  - DELETE /api/comments/{commentId} - Delete Comment

### Follow Management
- **follow-endpoints.puml**
  - POST /api/users/{userId}/follow - Follow User
  - DELETE /api/users/{userId}/follow - Unfollow User
  - GET /api/users/{userId}/followers - Get User Followers
  - GET /api/users/{userId}/following - Get User Following

### Bookmark Management
- **bookmark-endpoints.puml**
  - POST /api/posts/{postId}/bookmark - Bookmark Post
  - DELETE /api/posts/{postId}/bookmark - Remove Bookmark
  - GET /api/bookmarks - Get User Bookmarks

### Report Management
- **report-endpoints.puml**
  - POST /api/reports - Create Report
  - GET /api/reports - Get Reports (Admin)
  - PUT /api/reports/{reportId}/status - Update Report Status (Admin)
  - GET /api/reports/my - Get My Reports

### Search & Trending
- **search-trending-endpoints.puml**
  - GET /api/search - Global Search
  - GET /api/trending/posts - Get Trending Posts
  - GET /api/trending/hashtags - Get Trending Hashtags
  - GET /api/posts/hashtag/{hashtag} - Get Posts by Hashtag

### Admin Operations
- **admin-endpoints.puml**
  - POST /api/admin/users/{userId}/suspend - Suspend User (Admin)
  - POST /api/admin/users/{userId}/unsuspend - Unsuspend User (Admin)
  - GET /api/admin/users - Get All Users (Admin)
  - GET /api/admin/statistics - Get Platform Statistics (Admin)
  - DELETE /api/admin/posts/{postId} - Delete Post (Admin)

### Notification Management
- **notification-endpoints.puml**
  - GET /api/notifications - Get User Notifications
  - PUT /api/notifications/{notificationId}/read - Mark Notification as Read
  - PUT /api/notifications/read-all - Mark All Notifications as Read
  - DELETE /api/notifications/{notificationId} - Delete Notification
  - GET /api/notifications/unread-count - Get Unread Notification Count

### Block Management
- **block-endpoints.puml**
  - POST /api/users/{userId}/block - Block User
  - DELETE /api/users/{userId}/block - Unblock User
  - GET /api/users/blocked - Get Blocked Users

### Settings & Password
- **settings-password-endpoints.puml**
  - PUT /api/users/me/settings - Update User Settings
  - GET /api/users/me/settings - Get User Settings
  - PUT /api/users/me/password - Change Password
  - POST /api/auth/forgot-password - Request Password Reset
  - POST /api/auth/reset-password - Reset Password

## Architecture Patterns

### Participants
- **Client**: External actor initiating requests
- **AuthFilter**: JWT validation and authentication
- **RoleAuthorizationFilter**: Role-based access control (ADMIN, MODERATOR)
- **Controller**: REST endpoint handlers
- **Service/UseCase**: Application business logic
- **Repository**: Data access layer
- **Database**: Persistent storage
- **Mapper**: Entity to DTO conversion
- **Additional Services**: PasswordEncoder, JwtTokenProvider, FileStorageService, EmailService, CacheService

### Common Patterns

#### Success Flow
```
Client → AuthFilter → Controller → Service → Repository → Database
Database → Repository → Service → Controller → Client (200/201)
```

#### Error Handling
```
alt/else blocks for:
- 401 Unauthorized (invalid/missing JWT)
- 403 Forbidden (insufficient permissions)
- 404 Not Found (resource not found)
- 409 Conflict (duplicate/already exists)
- 400 Bad Request (validation errors)
```

#### Authorization Checks
- JWT validation in AuthFilter
- Role verification in RoleAuthorizationFilter
- Ownership checks in Service layer
- Visibility/access permission checks

#### Transaction Management
- Begin transaction for multi-step operations
- Commit on success
- Implicit rollback on exception

## Rendering Diagrams

### Online Tools
- [PlantUML Online Editor](http://www.plantuml.com/plantuml/uml/)
- [PlantText](https://www.planttext.com/)

### Local Rendering
```bash
# Install PlantUML
brew install plantuml  # macOS
apt-get install plantuml  # Ubuntu

# Render single diagram
plantuml auth-endpoints.puml

# Render all diagrams
plantuml *.puml

# Generate PNG
plantuml -tpng auth-endpoints.puml

# Generate SVG
plantuml -tsvg auth-endpoints.puml
```

### IDE Integration
- **IntelliJ IDEA**: PlantUML Integration plugin
- **VS Code**: PlantUML extension
- **Eclipse**: PlantUML plugin

## Notes

- All diagrams follow the same structure and notation
- Assumptions are noted when Swagger documentation lacks internal implementation details
- Database queries are simplified for readability
- Transaction boundaries are explicitly shown
- Cache usage is illustrated where applicable
- Async operations and event publishing are noted when relevant

## Generated

These diagrams were generated from the Swagger/OpenAPI specification on 2026-05-16.
