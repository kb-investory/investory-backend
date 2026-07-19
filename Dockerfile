FROM tomcat:9.0.120-jdk17-temurin

# Remove Tomcat's default sample/manager apps so only our WAR is served.
RUN rm -rf /usr/local/tomcat/webapps/*

COPY build/libs/*.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080
