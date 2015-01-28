FROM wouterd/java8
MAINTAINER Nicolas Modrzyk

ADD target/app-control-*-standalone.jar app.jar
ADD config.clj.sample config.clj.sample

EXPOSE 3000

ENTRYPOINT ["java","-jar","-Djava.awt.headless=true", "app.jar","config.clj.sample"]