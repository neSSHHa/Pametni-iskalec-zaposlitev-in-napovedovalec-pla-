FROM maven:3.9.9-eclipse-temurin-21

WORKDIR /app

COPY pom.xml .
COPY src ./src
COPY data ./data

CMD ["mvn", "compile", "exec:java", "-Dexec.mainClass=ImportSloveniaData"]