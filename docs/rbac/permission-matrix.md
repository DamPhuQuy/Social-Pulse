# Permission Matrix

## 1. Pham vi

Tai lieu nay liet ke `permission matrix` hien tai cua du an `Social-Pulse`, duoc tong hop tu cac migration:

- `V10__create_roles_and_permissions.sql`
- `V15__add_profile_crud_and_permission_normalization.sql`
- `V16__add_follow_read_permission.sql`
- `V17__add_comment_react_permission.sql`
- `V18__add_report_manage_permission.sql`
- `V19__add_discovery_and_bookmarks.sql`
- `V20__add_notifications.sql`
- `V21__remove_deprecated_any_permissions.sql`

Matrix nay phan anh quyen duoc seed vao database, khong phai quyen suy doan theo ten role.

## 2. Quy uoc doc bang

- `Y`: role co permission
- `N`: role khong co permission

Luu y:

- `GUEST` dang ton tai trong DB, nhung request anonymous hien tai khong chay theo role nay. Phan lon endpoint anonymous duoc `permitAll` truc tiep trong `SecurityConfig`.
- `ADMIN` khong duoc check bang `hasRole('ADMIN')` trong controller. Admin co kha nang thao tac vi role nay duoc gan nhieu permission hon.
- Hai permission cu `post:delete:any` va `comment:delete:any` da bi xoa khoi he thong o `V21`.

## 3. Tong quan theo role

### 3.1 GUEST

`GUEST` hien chi co:

- `post:read`

### 3.2 USER

`USER` la role mac dinh cua tai khoan moi dang ky va co cac nhom quyen:

- doc/tao/sua/xoa bai viet cua minh
- tao/sua/xoa comment cua minh
- react post, react comment
- doc/sua/xoa profile cua minh va tao profile neu can
- follow/unfollow/doc follow graph
- doc feed
- tao report
- discovery/search
- bookmark
- notification read/update

### 3.3 ADMIN

`ADMIN` ke thua phan lon quyen cua `USER`, dong thoi co them cac quyen quan tri:

- `post:manage`
- `comment:manage`
- `user:manage`
- `user:moderate`
- `report:manage`

## 4. Full Matrix

| Permission | GUEST | USER | ADMIN |
|---|---|---|---|
| `bookmark:create` | N | Y | Y |
| `bookmark:delete` | N | Y | Y |
| `bookmark:read` | N | Y | Y |
| `comment:create` | N | Y | Y |
| `comment:delete` | N | Y | Y |
| `comment:manage` | N | N | Y |
| `comment:react` | N | Y | Y |
| `comment:read` | N | Y | Y |
| `comment:update` | N | Y | Y |
| `discovery:read` | N | Y | Y |
| `feed:read` | N | Y | Y |
| `follow:create` | N | Y | Y |
| `follow:delete` | N | Y | Y |
| `follow:read` | N | Y | Y |
| `notification:read` | N | Y | Y |
| `notification:update` | N | Y | Y |
| `post:create` | N | Y | Y |
| `post:delete` | N | Y | Y |
| `post:manage` | N | N | Y |
| `post:react` | N | Y | Y |
| `post:read` | Y | Y | Y |
| `post:update` | N | Y | Y |
| `report:create` | N | Y | Y |
| `report:manage` | N | N | Y |
| `user:create` | N | Y | Y |
| `user:delete` | N | Y | Y |
| `user:manage` | N | N | Y |
| `user:moderate` | N | N | Y |
| `user:read` | N | Y | Y |
| `user:update` | N | Y | Y |

## 5. Matrix theo domain

### 5.1 User

| Permission | GUEST | USER | ADMIN |
|---|---|---|---|
| `user:read` | N | Y | Y |
| `user:create` | N | Y | Y |
| `user:update` | N | Y | Y |
| `user:delete` | N | Y | Y |
| `user:manage` | N | N | Y |
| `user:moderate` | N | N | Y |

### 5.2 Post

| Permission | GUEST | USER | ADMIN |
|---|---|---|---|
| `post:read` | Y | Y | Y |
| `post:create` | N | Y | Y |
| `post:update` | N | Y | Y |
| `post:delete` | N | Y | Y |
| `post:react` | N | Y | Y |
| `post:manage` | N | N | Y |

### 5.3 Comment

| Permission | GUEST | USER | ADMIN |
|---|---|---|---|
| `comment:read` | N | Y | Y |
| `comment:create` | N | Y | Y |
| `comment:update` | N | Y | Y |
| `comment:delete` | N | Y | Y |
| `comment:react` | N | Y | Y |
| `comment:manage` | N | N | Y |

### 5.4 Follow và Feed

| Permission | GUEST | USER | ADMIN |
|---|---|---|---|
| `follow:create` | N | Y | Y |
| `follow:delete` | N | Y | Y |
| `follow:read` | N | Y | Y |
| `feed:read` | N | Y | Y |

### 5.5 Report và Moderation

| Permission | GUEST | USER | ADMIN |
|---|---|---|---|
| `report:create` | N | Y | Y |
| `report:manage` | N | N | Y |

### 5.6 Discovery và Bookmark

| Permission | GUEST | USER | ADMIN |
|---|---|---|---|
| `discovery:read` | N | Y | Y |
| `bookmark:create` | N | Y | Y |
| `bookmark:delete` | N | Y | Y |
| `bookmark:read` | N | Y | Y |

### 5.7 Notification

| Permission | GUEST | USER | ADMIN |
|---|---|---|---|
| `notification:read` | N | Y | Y |
| `notification:update` | N | Y | Y |

## 6. Cach hieu matrix trong runtime

Matrix nay khong co nghia la chi can co permission la se thao tac duoc moi object.

Vi du:

- `post:update`
  - cho phep user vao endpoint sua bai viet
  - nhung service van co the kiem tra user co phai chu bai viet khong
- `comment:delete`
  - cho phep vao endpoint xoa comment
  - nhung service van co the chi cho owner xoa
- `post:manage`
  - la permission dac biet cho admin de vuot qua ownership check trong mot so service

Noi cach khac:

- permission matrix = coarse-grained access
- ownership/business rule = fine-grained access

## 7. Khuyen nghi su dung

Khi them module moi, nen cap nhat matrix theo nguyen tac:

1. Them permission moi theo format `<domain>:<action>`.
2. Gan permission cho role trong migration.
3. Neu la quyen "vuot ownership", uu tien dung hau to `:manage`.
4. Khong them lai format cu kieu `:any`.
5. Sau khi merge migration, cap nhat file nay de team frontend/backend co nguon tham chieu chung.

## 8. Tom tat nhanh

- `GUEST`: chi co `post:read`
- `USER`: co cac quyen tac nghiep thong thuong cua nguoi dung
- `ADMIN`: co them cac quyen `manage/moderate`
- Runtime authorization van phu thuoc them vao ownership check trong service
