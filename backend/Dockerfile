# Giai đoạn 1: Build ứng dụng
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

# Copy file cấu hình Maven trước để cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy code và build file JAR
COPY src ./src
RUN mvn clean package -DskipTests

# Giai đoạn 2: Chạy ứng dụng (Runtime)
FROM eclipse-temurin:25-jre
WORKDIR /app

# Copy file JAR đã build từ giai đoạn trước sang
COPY --from=build /app/target/*.jar app.jar

# Khai báo cổng (mặc định Spring Boot là 8080)
EXPOSE 8080

# Chạy ứng dụng với các tham số tối ưu bộ nhớ
ENTRYPOINT ["java", "-jar", "app.jar"]


# Explanation

# AS build: Đặt tên cho giai đoạn đầu tiên. Mọi thứ cồng kềnh (Maven, mã nguồn chưa biên dịch) sẽ bị bỏ lại sau khi giai đoạn này kết thúc.

# eclipse-temurin:17-jre-alpine:

# JRE: Chỉ chứa môi trường thực thi, không chứa bộ công cụ phát triển (JDK), giúp giảm dung lượng.

# Alpine: Một bản phân phối Linux siêu nhẹ (chỉ vài MB).

# dependency:go-offline: Lệnh này giúp tải toàn bộ thư viện về trước. Lần sau nếu bạn chỉ sửa code mà không thêm thư viện mới, Docker sẽ bỏ qua bước này (rất nhanh).
