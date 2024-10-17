#!/bin/bash
# ***************************************************************************************************************************
# * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
# * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
# * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
# * with the License.  You may obtain a copy of the License at                                                              *
# *                                                                                                                         *
# *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
# *                                                                                                                         *
# * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
# * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
# * specific language governing permissions and limitations under the License.                                              *
# ***************************************************************************************************************************

# DO NOT CHECK IN CHANGES TO THIS FILE!

export X_VERSION=8.1.4
export X_NEXT_VERSION=8.1.5-SNAPSHOT
export X_RELEASE=juneau-8.1.4-RC1
export X_STAGING=~/tmp/dist-release-juneau
export X_USERNAME=jamesbognar
export X_EMAIL=jamesbognar@apache.org
export X_CLEANM2=N
export X_GIT_BRANCH=juneau-8.1.4-branch
export X_JAVA_HOME=/Users/james.bognar/blt/tools/Darwin/jdk/openjdk_11.0.21.0.102_11.69.52_x64

echo ' '
echo --- Settings ------------------------------------------------------------------
echo X_VERSION: $X_VERSION
echo X_NEXT_VERSION: $X_NEXT_VERSION
echo X_RELEASE = $X_RELEASE
echo X_STAGING = $X_STAGING
echo X_USERNAME = $X_USERNAME
echo X_EMAIL = $X_EMAIL
echo X_CLEANM2 = $X_CLEANM2
echo X_GIT_BRANCH = $X_GIT_BRANCH
echo -------------------------------------------------------------------------------
