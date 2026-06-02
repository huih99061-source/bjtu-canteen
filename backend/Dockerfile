# 第一阶段：编译打包
FROM docker.m.daocloud.io/library/maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn clean package -DskipTests -q

# 第二阶段：运行（只含JRE，镜像更小）
FROM docker.m.daocloud.io/library/eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/target/canteen-sim-1.0.0.jar app.jar
EXPOSE 8888
ENTRYPOINT ["java", "-jar", "app.jar"]
