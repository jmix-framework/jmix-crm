FROM eclipse-temurin:21.0.3_9-jdk AS builder
WORKDIR /application
COPY . .

RUN --mount=type=cache,target=/root/.gradle chmod +x gradlew && ./gradlew bootJar -i -Pvaadin.productionMode

FROM eclipse-temurin:21.0.3_9-jdk

RUN mkdir -p /heapdumps
ENV JAVA_TOOL_OPTIONS="-Xmx2g -Xms1g -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/heapdumps/heapdump.hprof"
ENV CRM_ACTIVE_PROFILE="online"

COPY --from=builder /application/build/libs/crm.jar crm.jar
VOLUME /application

RUN apt-get update \
 && apt-get install -y --no-install-recommends \
    curl \
    libfreetype6 \
    fonts-dejavu \
    fontconfig \
 && rm -rf /var/lib/apt/lists/*

ENTRYPOINT ["java", "-jar", "/crm.jar"]
