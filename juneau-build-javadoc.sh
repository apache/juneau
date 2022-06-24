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

function fail_with_message {
	X_DATE=$(date +'%H:%M:%S') 
	echo ' '
	echo "-------------------------------------------------------------------------------"
	echo "[$X_DATE] $1"
	echo '-------------------------------------------------------------------------------'
	fail; 
}

cd juneau-doc

mvn install
test -f target/juneau-doc-${JUNEAU_VERSION}-SNAPSHOT.jar || fail_with_message "target/juneau-doc-${JUNEAU_VERSION}-SNAPSHOT.jar not found."
test -f ../juneau-all/target/juneau-all-${JUNEAU_VERSION}-SNAPSHOT.jar || fail_with_message "../juneau-all/target/juneau-all-${JUNEAU_VERSION}-SNAPSHOT.jar.not found"

export cp=target/juneau-doc-${JUNEAU_VERSION}-SNAPSHOT.jar:../juneau-all/target/juneau-all-${JUNEAU_VERSION}-SNAPSHOT.jar
java -DjuneauVersion=$JUNEAU_VERSION -cp $cp org.apache.juneau.doc.internal.DocGenerator 
cd .. 

mvn javadoc:aggregate
tput bel

cd juneau-doc
java -cp $cp org.apache.juneau.doc.internal.DocLinkTester
cd .. 

rm -rf ../juneau-website/content/site/apidocs-$JUNEAU_VERSION
mkdir ../juneau-website/content/site/apidocs-$JUNEAU_VERSION
cp -r ./target/site/apidocs/* ../juneau-website/content/site/apidocs-$JUNEAU_VERSION
find ../juneau-website/content/site/apidocs-$JUNEAU_VERSION -type f -name '*.html' -exec sed -i '' s/-SNAPSHOT// {} +

echo '*******************************************************************************'
echo '***** SUCCESS *****************************************************************'
echo '*******************************************************************************'
