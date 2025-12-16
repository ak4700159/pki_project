# ===== Base Image =====
FROM eclipse-temurin:17-jdk-jammy

# ===== Working Directory =====
WORKDIR /app

# ===== Copy client source =====
COPY ./ /app/server

# ===== Compile Java sources =====
WORKDIR /app/server
RUN mkdir ./key/repository
RUN mkdir -p out && \
    javac -d out $(find . -name "*.java")

# ===== Build JAR =====
RUN jar -cvfm server.jar manifest.txt -C out .

# ===== Load env variables at runtime =====
# (Docker에서는 source 대신 ENV 또는 --env-file 사용)
ENV REPOSITORY_DIRECTORY=/app/server/key/repository
ENV PRIVATE_KEY_PATH=/app/server/key/private_key.pem
ENV PUBLIC_KEY_PATH=/app/server/key/public_key.pem

# ===== Default Command =====
# 인자는 docker run 시 덮어쓸 수 있음
CMD ["java", "-jar", "server.jar", "2000"]
