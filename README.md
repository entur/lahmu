# Bikeservice

This is a work in progress

## Running

### Command line
 - `./gradlew clean build`
 - `java -jar build/libs/bikeservice-0.0.1.jar`

### Docker
- `docker build --tag bikeservice:0.0.1 .`
- `docker run --publish 8000:8080 --name bs bikeservice:0.0.1`

If the docker image already exists, you can remove it by: `docker rm --force bs`
