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

projects=( 
"juneau-core/juneau-config"
"juneau-core/juneau-dto"
"juneau-core/juneau-marshall"
"juneau-core/juneau-marshall-rdf"
"juneau-core/juneau-svl"
"juneau-doc"
"juneau-examples/juneau-examples-core"
"juneau-examples/juneau-examples-rest"
"juneau-examples/juneau-examples-rest-jetty"
"juneau-examples/juneau-examples-rest-springboot"
"juneau-microservice/juneau-microservice-core"
"juneau-microservice/juneau-microservice-jetty"
"juneau-microservice/juneau-my-jetty-microservice"
"juneau-microservice/juneau-my-springboot-microservice"
"juneau-rest/juneau-rest-client"
"juneau-rest/juneau-rest-mock"
"juneau-rest/juneau-rest-server"
"juneau-rest/juneau-rest-server-jaxrs"
"juneau-rest/juneau-rest-server-rdf"
"juneau-rest/juneau-rest-server-springboot"
"juneau-sc/juneau-sc-client"
"juneau-sc/juneau-sc-server"
)

for i in "${projects[@]}"
do
	echo Preferences applied to $i
	cp eclipse-preferences/source-prefs/org.eclipse.jdt.core.prefs $i/.settings
	cp eclipse-preferences/source-prefs/org.eclipse.jdt.ui.prefs $i/.settings
done

projects=( 
"juneau-core/juneau-core-utest"
"juneau-examples/juneau-examples-rest-jetty-ftest"
"juneau-microservice/juneau-microservice-ftest"
"juneau-rest/juneau-rest-client-utest"
"juneau-rest/juneau-rest-mock-utest"
"juneau-rest/juneau-rest-server-utest"
)

for i in "${projects[@]}"
do
	echo Preferences applied to $i
	cp eclipse-preferences/test-prefs/org.eclipse.jdt.core.prefs $i/.settings
	cp eclipse-preferences/test-prefs/org.eclipse.jdt.ui.prefs $i/.settings
done

