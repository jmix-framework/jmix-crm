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

RUN apt update
RUN apt install -y curl
RUN apt install -y libfreetype6
RUN apt install -y fonts-dejavu
RUN apt install -y fontconfig

ENTRYPOINT ["java", "-jar", "/crm.jar"]
