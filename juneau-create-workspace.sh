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

. juneau-env.sh

export WORKSPACE=target/workspace

rm -Rf $WORKSPACE
mkdir -p $WORKSPACE
unzip -o juneau-microservice/juneau-my-jetty-microservice/target/my-jetty-microservice-$X_VERSION-bin.zip -d $WORKSPACE/my-jetty-microservice
unzip -o juneau-microservice/juneau-my-springboot-microservice/target/my-springboot-microservice-$X_VERSION-bin.zip -d $WORKSPACE/my-springboot-microservice
unzip -o juneau-examples/juneau-examples-core/target/juneau-examples-core-$X_VERSION-bin.zip -d $WORKSPACE/juneau-examples-core
unzip -o juneau-examples/juneau-examples-rest-jetty/target/juneau-examples-rest-jetty-$X_VERSION-bin.zip -d $WORKSPACE/juneau-examples-rest-jetty
unzip -o juneau-examples/juneau-examples-rest-springboot/target/juneau-examples-rest-springboot-$X_VERSION-bin.zip -d $WORKSPACE/juneau-examples-rest-springboot

echo '*******************************************************************************'
echo '***** SUCCESS *****************************************************************'
echo '*******************************************************************************'
