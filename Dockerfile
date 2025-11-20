# Imagen base segura y version
FROM eclipse-temurin:17-jdk-alpine

#argumentos - direcion de archivo jar
#al compilar se crea ejecutable .jar (mediante java es ejecuta)

#variable de entorno = target/{artifactId - version que quieras}
ARG JAR_FILE=target/eta-0.0.1-SNAPSHOT.jar

#copia de jarfile 
COPY ${JAR_FILE} etaApp.jar

#al levantarlo, se expone en el 8080 en docker
EXPOSE 8080

#usamos java, .jar
ENTRYPOINT ["java","-jar","etaApp.jar"]
