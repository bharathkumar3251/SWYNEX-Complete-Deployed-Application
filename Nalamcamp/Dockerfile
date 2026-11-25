FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
       tesseract-ocr tesseract-ocr-eng tesseract-ocr-tam \
    && ln -s "$(dirname "$(dpkg -L tesseract-ocr-eng | grep '/eng.traineddata$')")" /opt/tessdata \
    && rm -rf /var/lib/apt/lists/*

ENV TESSERACT_PATH=/usr/bin/tesseract
ENV TESSDATA_PREFIX=/opt/tessdata

COPY --from=build /app/target/*.jar /app/nalamcamp.jar
CMD ["java", "-jar", "/app/nalamcamp.jar"]