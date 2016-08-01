# Dockerfile for creating a Juneau microservice container

FROM ubuntu

# Change this to whatever port you wish to use.
EXPOSE 10000

# Install Java
apt-get update
apt-get install default-jre

# Copy our jar
copy build/microservice.jar /

ENTRYPOINT []
CMD /bin/sh -c "java -jar microservice.jar 10000"
