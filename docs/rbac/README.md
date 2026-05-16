# RBAC trong Social Pulse

## 1. Muc tieu

Tai lieu nay mo ta co che RBAC (Role-Based Access Control) dang duoc su dung trong du an `Social-Pulse`, bao gom:

- mo hinh du lieu `users -> roles -> permissions`
- cach Spring Security xac thuc va uy quyen request
- cach JWT mang thong tin quyen
- vi tri ma quyen duoc kiem tra trong code
- thu vien va framework dang ho tro
- quy trinh them quyen moi cho module moi

Du an hien tai dung RBAC theo huong:

- `role` la lop nhom quyen
- `permission` la don vi kiem soat truy cap thuc te
- controller chu yeu kiem tra bang `permission string`
- service tiep tuc kiem tra ownership/business rule neu can

Noi cach khac, role khong phai "nguon su that" khi authorize request. Permission moi la nguon su that.

## 2. Thu vien va thanh phan ho tro

Du an hien tai khong dung mot thu vien RBAC rieng biet nhu Casbin, Keycloak Authorization Services hay OPA. RBAC duoc ghep tu cac thanh phan co san cua Spring ecosystem:

- `spring-boot-starter-security`
  - xay dung `SecurityFilterChain`
  - `AuthenticationManager`
  - `UserDetailsService`
  - `@PreAuthorize`
- `spring-boot-starter-data-jpa`
  - luu `User`, `Role`, `Permission` trong database
- `Flyway`
  - tao bang va seed role/permission
  - cap nhat quyen qua cac migration versioned
- `JJWT` (`io.jsonwebtoken`)
  - tao va verify access token / refresh token
- `BCryptPasswordEncoder`
  - hash password cho luong authenticate

### Cac file chinh

- `backend/src/main/java/com/socialpulse/app/security/config/SecurityConfig.java`
- `backend/src/main/java/com/socialpulse/app/security/jwt/JwtAuthenticationFilter.java`
- `backend/src/main/java/com/socialpulse/app/security/user/CustomUserDetails.java`
- `backend/src/main/java/com/socialpulse/app/security/user/CustomUserDetailsService.java`
- `backend/src/main/java/com/socialpulse/app/auth/application/service/jwt/JwtService.java`
- `backend/src/main/resources/db/migration/V10__create_roles_and_permissions.sql`
- `backend/src/main/resources/db/migration/V15__add_profile_crud_and_permission_normalization.sql`
- `backend/src/main/resources/db/migration/V21__remove_deprecated_any_permissions.sql`

## 3. Mo hinh du lieu RBAC

### 3.1 Bang database

He thong dung 5 bang chinh:

- `users`
- `roles`
- `permissions`
- `user_roles`
- `role_permissions`

Quan he:

- mot `user` co the co nhieu `role`
- mot `role` co the co nhieu `permission`
- mot `permission` co the duoc gan cho nhieu `role`

So do logic:

```text
users
  |
  | many-to-many
  v
user_roles
  ^
  |
roles
  |
  | many-to-many
  v
role_permissions
  ^
  |
permissions
```

### 3.2 Entity map voi database

- `UserEntity`
  - `@ManyToMany(fetch = FetchType.EAGER)` voi `RoleEntity`
  - join table: `user_roles`
- `RoleEntity`
  - `@ManyToMany(fetch = FetchType.EAGER)` voi `PermissionEntity`
  - join table: `role_permissions`
- `PermissionEntity`
  - mappedBy tu `RoleEntity`

Fetch dang la `EAGER` cho ca `user -> roles` va `role -> permissions`. Cach nay giup authentication/request authorization don gian hon vi quyen duoc load day du ngay khi tai user, doi lai co the tang chi phi query neu so role/permission lon.

### 3.3 Domain model

Tang domain co cac model:

- `com.socialpulse.app.user.domain.model.User`
- `com.socialpulse.app.user.domain.model.Role`
- `com.socialpulse.app.user.domain.model.Permission`

`Role` co helper `hasPermission(String permission)`, nhung trong runtime authorization hien tai, Spring Security chu yeu lam viec voi tap `GrantedAuthority` da duoc flatten tu toan bo permission.

## 4. Cau truc role va permission hien tai

### 4.1 Roles

Role duoc seed tu migration `V10__create_roles_and_permissions.sql`:

- `GUEST`
- `USER`
- `ADMIN`

Trong thuc te hien tai:

- request khong authenticated se khong di theo role `GUEST`; thay vao do bi chan boi `SecurityFilterChain`, tru cac endpoint `permitAll`
- user moi dang ky duoc gan mac dinh role `USER`
- `ADMIN` duoc dung de nhom cac permission mang tinh "manage/moderate"

### 4.2 Permission naming convention

He thong da chuan hoa theo format:

```text
<domain>:<action>
```

Vi du:

- `post:read`
- `post:create`
- `post:update`
- `post:delete`
- `post:manage`
- `comment:react`
- `feed:read`
- `notification:update`

Migration `V21__remove_deprecated_any_permissions.sql` da xoa cac permission cu kieu:

- `post:delete:any`
- `comment:delete:any`

Nghia la he thong hien tai uu tien mot trong hai kieu sau:

- quyen "own" o controller + ownership check o service
- quyen "manage" cho admin/moderator

Khong dung them hau to `:any` nua.

### 4.3 Danh sach permission hien co

Theo cac migration hien tai, du an dang co cac permission sau:

- `user:read`
- `user:create`
- `user:update`
- `user:delete`
- `user:manage`
- `user:moderate`
- `post:read`
- `post:create`
- `post:update`
- `post:delete`
- `post:react`
- `post:manage`
- `comment:read`
- `comment:create`
- `comment:update`
- `comment:delete`
- `comment:react`
- `comment:manage`
- `follow:create`
- `follow:delete`
- `follow:read`
- `feed:read`
- `report:create`
- `report:manage`
- `discovery:read`
- `bookmark:create`
- `bookmark:delete`
- `bookmark:read`
- `notification:read`
- `notification:update`

Luu y:

- Khong phai tat ca permission deu duoc frontend dung truc tiep.
- Nguon su that la database seed qua Flyway, khong phai JWT claim.

## 5. Luong authenticate va authorize

### 5.1 Tong quan

Mot request protected di qua cac buoc:

```text
Client
  -> Authorization: Bearer <access-token>
  -> JwtAuthenticationFilter
  -> JwtService verify token
  -> CustomUserDetailsService load user tu DB
  -> CustomUserDetails flatten permissions thanh GrantedAuthority
  -> Spring Security dat Authentication vao SecurityContext
  -> @PreAuthorize kiem tra permission
  -> Service xu ly business rule + ownership rule
```

### 5.2 Dang nhap

Dang nhap duoc xu ly trong `AuthenticationService`:

1. user gui email/password
2. `AuthenticationManager.authenticate(...)` duoc goi
3. Spring Security dung `CustomUserDetailsService` de load user theo email
4. `AppPasswordEncoder` (BCrypt cost 12) so sanh password
5. neu hop le, he thong tao access token va refresh token

Phan nay khong tu viet mot `AuthenticationProvider` rieng. Du an dang tan dung co che mac dinh cua Spring Security voi:

- `UserDetailsService`
- bean `PasswordEncoder`
- `AuthenticationManager` lay tu `AuthenticationConfiguration`

### 5.3 Tao JWT

`JwtService` tao access token voi cac claim chinh:

- `sub`: email
- `userId`
- `roles`
- `permissions`
- `type=access`
- `iat`
- `exp`
- `issuer=social-pulse-api`

Refresh token gon hon:

- `sub`
- `type=refresh`

Luu y quan trong:

- JWT co chua `roles` va `permissions` de frontend co the tham khao
- nhung backend khong authorize request dua tren claim `permissions` trong token mot cach "blind trust"
- moi request van load lai user tu database thong qua `CustomUserDetailsService`
- vi vay, quyen thuc te duoc xac dinh boi DB tai thoi diem request, khong chi boi token payload

Day la mot lua chon an toan hon so voi cach chi giai ma token roi tin toan bo claims ben trong.

### 5.4 JWT filter

`JwtAuthenticationFilter` la `OncePerRequestFilter`:

- doc header `Authorization`
- chi nhan token theo format `Bearer <token>`
- extract email tu token
- neu `SecurityContext` chua co authentication, thi load user tu DB
- verify token hop le
- set `UsernamePasswordAuthenticationToken` vao `SecurityContextHolder`

Neu token invalid/expired:

- filter khong throw custom response tai day
- request tiep tuc di tiep voi trang thai chua authenticate
- Spring Security se xu ly thanh `401` neu endpoint bat buoc auth

### 5.5 Security filter chain

`SecurityConfig` dang cau hinh:

- `csrf.disable()`
- `SessionCreationPolicy.STATELESS`
- `formLogin.disable()`
- `httpBasic.disable()`
- them `JwtAuthenticationFilter` truoc `UsernamePasswordAuthenticationFilter`

Endpoint duoc `permitAll`:

- `/api/v1/auth/**`
- `/v3/api-docs/**`
- `/swagger-ui/**`
- `/swagger-ui.html`
- `/scalar/**`
- tat ca `OPTIONS`

Moi endpoint con lai:

- phai duoc authenticate
- sau do moi toi lop `@PreAuthorize`

## 6. Cau truc authority trong runtime

### 6.1 `CustomUserDetails`

`CustomUserDetails` nhan `User` domain object va flatten toan bo permission cua tat ca role thanh:

- `Collection<GrantedAuthority>`
- moi permission tro thanh `new SimpleGrantedAuthority(permission.getName())`

Vi du user co role `USER`, va role nay chua:

- `post:create`
- `post:read`
- `comment:create`

thi `getAuthorities()` se tra ve cac authority string dung nhu vay.

### 6.2 Role trong runtime

Trong runtime hien tai:

- role duoc dua vao JWT claim `roles`
- role duoc tra ve trong `/api/v1/auth/me`
- nhung `@PreAuthorize` dang khong check `hasRole(...)`

Nghia la:

- role dung de quan ly/seed nhom quyen
- permission dung de authorize endpoint

Day la mot thiet ke hop ly vi:

- tranh coupling controller voi ten role
- de thay doi mapping role-permission ma khong phai sua annotation trong controller
- de mo rong role moi trong tuong lai

## 7. Kiem tra quyen o dau

He thong dang co 2 tang kiem tra:

### 7.1 Tang 1: endpoint-level authorization

Su dung `@PreAuthorize` tren controller methods.

Vi du:

- `@PreAuthorize("hasAuthority('post:create')")`
- `@PreAuthorize("hasAuthority('post:read')")`
- `@PreAuthorize("hasAnyAuthority('post:update', 'post:manage')")`
- `@PreAuthorize("hasAuthority('feed:read')")`
- `@PreAuthorize("hasAuthority('notification:update')")`

Tang nay tra loi cau hoi:

- user co loai quyen nay hay khong

### 7.2 Tang 2: ownership/business authorization

Mot so action khong the chi check permission o controller, vi can biet user co phai chu so huu resource hay khong.

Vi du:

- `EditPostService`
  - cho phep neu la author
  - hoac co `post:manage`
- `DeletePostService`
  - cho phep neu la author
  - hoac co `post:manage`
- `DeleteCommentService`
  - cho phep neu la owner
  - hoac co `comment:manage`
- `ViewPostService`
  - du co `post:read`, van khong duoc xem post `PRIVATE` cua nguoi khac

Tang nay tra loi cau hoi:

- user co duoc thao tac tren object cu the nay hay khong

Day la diem rat quan trong: RBAC trong du an khong chi nam o annotation. No la su ket hop giua:

- coarse-grained permission check
- object-level business check

## 8. Role assignment hien tai

User moi duoc gan role mac dinh trong `CreateUserService` thong qua `UserRoleService.assignDefaultRole(user)`.

Co che hien tai:

- dang ky user moi
- `User.applyDefaultState()`
- `UserRoleService` load role `USER` tu DB
- gan role `USER` vao user
- save user

He thong cung co method `assignRoles(User user, Set<String> roleNames)` de gan role theo ten, nhung chua thay mot admin API day du cho role management trong scope hien tai.

## 9. Vi du thuc te trong du an

### 9.1 Xem feed

- endpoint: `GET /api/v1/feed`
- controller check: `feed:read`
- business layer: lay personalized feed cho user hien tai

### 9.2 Sua bai viet

- endpoint check: `post:update` hoac `post:manage`
- service check tiep:
  - neu la chinh chu bai viet -> cho phep
  - neu khong phai author nhung co `post:manage` -> cho phep
  - con lai -> tu choi

### 9.3 Danh dau notification da doc

- endpoint check: `notification:update`
- service check tiep:
  - notification phai thuoc `recipient_id` cua current user
  - neu khong -> `NOTIFICATION_NOT_FOUND`

Day cho thay du an dang ket hop:

- permission-level access
- resource ownership

## 10. Phuong an thuc hien duoc chon va ly do

Du an dang chon phuong an RBAC "database-backed permissions + Spring Security method security".

### 10.1 Tai sao khong chi dung role

Neu controller check truc tiep role, vi du `hasRole('ADMIN')`, he thong se bi cung:

- them role moi phai sua nhieu annotation
- kho tai su dung quyen giua cac role
- frontend/backend kho dong bo theo capability

Dung permission string giup:

- bieu dien duoc "kha nang" thay vi "chuc danh"
- de map nhieu role cung chia se mot quyen
- de mo rong admin/moderator/custom role sau nay

### 10.2 Tai sao van giu role

Role van can thiet vi:

- DB can mot lop grouping de cap quyen hang loat
- du thao tac gan role cho user hon la gan tung permission le
- phu hop voi cai ten RBAC

Noi cach khac:

- role = nhom quyen
- permission = quyen thuc thi

### 10.3 Tai sao van load user tu DB moi request

Co the authorize dua tren claim `permissions` trong JWT de nhanh hon. Tuy nhien du an hien tai khong di huong do.

Loi ich cua cach dang dung:

- revoke/quy doi role-permission co hieu luc ngay voi request moi
- tranh tinh trang token cu van giu quyen da bi thu hoi
- backend khong phu thuoc hoan toan vao claims do client mang len

Diem danh doi:

- moi request can them mot lan load user
- can toi uu neu sau nay traffic lon hon

## 11. Cach them quyen moi

Khi them mot module moi, quy trinh nen la:

1. Them permission vao migration Flyway moi.
2. Gan permission do cho role phu hop trong `role_permissions`.
3. Neu can, seed permission cho `USER`, `ADMIN` hoac role moi.
4. Bao ve endpoint bang `@PreAuthorize("hasAuthority('module:action')")`.
5. Neu action tac dong tren resource cu the, them ownership/business check trong service.
6. Neu frontend can hien thi capability, co the doc tu JWT claim `permissions` hoac can nhac mo rong `/auth/me`.

### 11.1 Mau migration

```sql
INSERT INTO permissions (name, description) VALUES
    ('message:create', 'Create direct messages')
ON CONFLICT (name) DO UPDATE
SET description = EXCLUDED.description;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name = 'message:create'
WHERE r.name IN ('USER', 'ADMIN')
ON CONFLICT DO NOTHING;
```

### 11.2 Mau controller

```java
@PostMapping
@PreAuthorize("hasAuthority('message:create')")
public ResponseEntity<ApiResponse<Void>> createMessage(...) {
    ...
}
```

### 11.3 Mau service-level check

```java
boolean isOwner = resource.getUserId().equals(currentUser.getId());
boolean hasManagePermission = currentUser.getAuthorities().stream()
        .anyMatch(auth -> auth.getAuthority().equals("message:manage"));

if (!isOwner && !hasManagePermission) {
    throw new AppException(...);
}
```

## 12. Uu diem va han che hien tai

### 12.1 Uu diem

- de hieu va phu hop voi Spring Boot
- seed bang Flyway nen de version control
- role va permission tach bach
- controller da thong nhat theo permission string
- co tang ownership check de tranh over-authorize
- JWT chua roles/permissions de frontend de tieu thu

### 12.2 Han che

- role hierarchy chua co
- chua co admin UI/API day du de quan ly role/permission
- `@PreAuthorize` chu yeu dat o controller, chua co policy abstraction rieng
- nhieu ownership check van viet tay trong service
- `FetchType.EAGER` co the thanh diem nghen neu role/permission tang nhieu
- `/api/v1/auth/me` moi tra `roles`, chua tra `permissions`
- chua co co che audit log rieng cho thay doi role/quyen

## 13. Khuyen nghi cho giai doan tiep theo

Neu du an tiep tuc mo rong, nen uu tien:

1. Tao tai lieu chuan cho permission naming theo domain.
2. Bo sung admin APIs cho:
   - xem user roles
   - cap/thu hoi role
   - xem permission matrix
3. Can nhac bo sung `permissions` vao `/api/v1/auth/me` neu frontend can render UI theo capability.
4. Tach helper dung chung cho ownership check de giam lap code.
5. Neu traffic cao, can nhac cache short-lived cho user authorities hoac doi chien luoc load.
6. Neu nghiep vu phuc tap hon, can nhac chuyen mot phan sang policy-based authorization.

## 14. Tom tat

RBAC cua Social Pulse hien tai la:

- luu role va permission trong database
- seed va version bang Flyway
- authenticate bang Spring Security + BCrypt + JWT
- authorize request bang permission string qua `@PreAuthorize`
- dung role de nhom quyen
- dung service-level ownership check cho object-level access

Day la mot thiet ke thuc dung, de mo rong, phu hop voi quy mo hien tai cua du an. No chua phai he thong IAM phuc tap, nhung da du de bao ve API theo huong nhat quan, de hieu va co kha nang mo rong trong cac phase sau.
