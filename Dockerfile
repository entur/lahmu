FROM adoptopenjdk/openjdk11:alpine
RUN apk update && apk upgrade &&
RUN addgroup appuser && adduser --disabled-password appuser --ingroup appuser
USER appuser

ADD build/libs/lahmu*.jar lahmu.jar

ENTRYPOINT ["java", "-jar", "lahmu.jar"]