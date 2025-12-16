# ===== Base Image =====
FROM eclipse-temurin:17-jdk-jammy

# ===== Working Directory =====
WORKDIR /app

# ===== Copy client source =====
COPY ./ /app/client

# ===== Compile Java sources =====
WORKDIR /app/client
RUN mkdir -p out && \
    javac -d out $(find . -name "*.java")

# ===== Build JAR =====
RUN jar -cvfm client.jar manifest.txt -C out .

# ===== Load env variables at runtime =====
# (Docker에서는 source 대신 ENV 또는 --env-file 사용)
ENV SERVER_PUBLIC_KEY_PATH=/app/client/key/server_public_key.pem
ENV PRIVATE_KEY_PATH=/app/client/key/private_key.pem
ENV PUBLIC_KEY_PATH=/app/client/key/public_key.pem

# ===== Default Command =====
# 인자는 docker run 시 덮어쓸 수 있음
CMD ["java", "-jar", "client.jar", "localhost", "2000", "lee", "kim"]
