FROM node:24-alpine AS frontend-build

WORKDIR /workspace/frontend

COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

COPY frontend ./
RUN npm run build

FROM eclipse-temurin:25-jdk AS backend-build

WORKDIR /workspace

COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle gradle
COPY src src
COPY --from=frontend-build /workspace/frontend/dist /workspace/src/main/resources/public

RUN chmod +x gradlew
RUN GRADLE_USER_HOME=/tmp/gradle-home ./gradlew clean installDist --no-daemon

FROM eclipse-temurin:25-jre

WORKDIR /opt/app

COPY --from=backend-build /workspace/build/install/parcial2 /opt/app

ENV APP_PORT=7070
ENV DB_PORT=9092
ENV DB_PATH=/var/lib/academic-events/academic-events
ENV DB_ALLOW_REMOTE_CONNECTIONS=false

EXPOSE 7070 9092

VOLUME ["/var/lib/academic-events"]

CMD ["./bin/parcial2"]
