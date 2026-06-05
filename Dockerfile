FROM eclipse-temurin:17-jdk-alpine

# 1. Non-root 유저 및 그룹 생성
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

VOLUME /tmp

# 2. 와일드카드 충돌 방지를 위해 정확한 Boot Jar 이름 명시
ARG JAR_FILE=build/libs/calbak-0.0.1-SNAPSHOT.jar

# 3. 파일 복사 시 소유권을 appuser로 지정
COPY --chown=appuser:appgroup ${JAR_FILE} app.jar

# 4. 이후 명령어는 root가 아닌 appuser 권한으로 실행
USER appuser

ENTRYPOINT ["java", "-jar", "/app.jar"]