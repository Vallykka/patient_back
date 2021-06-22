FROM clojure:openjdk-16-lein-alpine as builder

WORKDIR /usr/src/patient_back/
COPY project.clj /usr/src/patient_back/
COPY resources/ /usr/src/patient_back/resources
COPY src/ /usr/src/patient_back/src

RUN lein uberjar

FROM openjdk:16-jdk-alpine3.12

COPY --from=builder /usr/src/patient_back/target/patient_back-0.1.0-SNAPSHOT-standalone.jar /patient_back-0.1.0-SNAPSHOT-standalone.jar
CMD ["java", "-jar", "/patient_back-0.1.0-SNAPSHOT-standalone.jar"]