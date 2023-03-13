FROM eclipse-temurin:17.0.6_10-jdk-alpine

RUN apk update && apk upgrade && apk add --no-cache \
  tini

RUN addgroup appuser && adduser --disabled-password appuser --ingroup appuser
USER appuser

ADD build/libs/lahmu*.jar lahmu.jar

ENTRYPOINT ["/sbin/tini", "--", "java", "-jar", "lahmu.jar"]