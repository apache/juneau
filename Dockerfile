# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# WAR-on-Tomcat image for the Tomcat starter.
# Build the WAR first:  ./mvnw -q clean package   (produces target/my-app.war)
# Build the image:      docker build -t my-app .
# Run:                  docker run --rm -p 8080:8080 my-app   (open http://localhost:8080/helloWorld)
FROM tomcat:10.1-jdk17-temurin
RUN rm -rf /usr/local/tomcat/webapps/ROOT
COPY target/my-app.war /usr/local/tomcat/webapps/ROOT.war
COPY my-app.yaml /usr/local/tomcat/bin/my-app.yaml
EXPOSE 8080
