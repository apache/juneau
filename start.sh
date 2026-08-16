#!/usr/bin/env bash
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
# Start the Apache Juneau Release Manager locally.
#
#   ./start.sh
#
# Builds and runs the Spring Boot app via the spring-boot-maven-plugin.
# Once started it serves at:
#
#   http://127.0.0.1:8790/
#
# The Juneau REST resources are mounted under /rest/*, e.g.:
#
#   http://127.0.0.1:8790/rest/home
#   http://127.0.0.1:8790/rest/releases
#   http://127.0.0.1:8790/rest/credentials
#
# Bind address/port come from src/main/resources/application.properties.
#
set -euo pipefail

cd "$(dirname "$0")"

# Personal JDK layout: ~/jdk/default -> current JDK 17. Honor an existing
# JAVA_HOME if the caller already set one.
export JAVA_HOME="${JAVA_HOME:-$HOME/jdk/default}"
export PATH="$JAVA_HOME/bin:$PATH"

exec mvn spring-boot:run
