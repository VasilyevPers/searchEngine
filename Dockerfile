FROM openjdk:20-jdk
COPY /target/SearchEngine-1.0-SNAPSHOT.jar SearchEngine.jar
COPY ./application.yaml application.yaml
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "SearchEngine.jar"]