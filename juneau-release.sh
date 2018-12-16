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

. juneau-release-env.sh

function fail { 
	echo ' '
	echo '*******************************************************************************'
	echo '***** FAILED ******************************************************************'
	echo '*******************************************************************************'
	echo ' '
	exit 1; 
}

function success { 
	echo ' '
	echo '*******************************************************************************'
	echo '***** SUCCESS *****************************************************************'
	echo '*******************************************************************************'
	echo ' '
	exit 1; 
}

function yprompt {
	echo -n "$1 (Y/n): "
	read prompt
	if [ "$prompt" != "Y" ] && [ "$prompt" != "" ] 
	then 
		fail;
	fi

} 

cd ~/.m2
mv repository repository-old
rm -rf repository-old & 
rm -rf $X_STAGING
mkdir -p $X_STAGING
mkdir $X_STAGING/git
cd $X_STAGING/git
git clone https://gitbox.apache.org/repos/asf/juneau.git
git clone https://gitbox.apache.org/repos/asf/juneau-website.git
cd juneau
git config user.name $X_USERNAME
git config user.email $X_EMAIL

java -version
yprompt "Are you using at least Java 8?";

mvn -version
yprompt "Are you using at least Maven 3?"

cd $X_STAGING/git/juneau
mvn clean verify

mvn javadoc:aggregate
yprompt "Is the javadoc generation clean?"

yprompt "Can juneau/juneau-microservice/juneau-my-jetty-microservice/target/my-jetty-microservice-$X_VERSION.bin.zip be deployed into an Eclipse workspace?"
yprompt "Can juneau/juneau-microservice/juneau-my-springboot-microservice/target/my-springboot-microservice-$X_VERSION.bin.zip be deployed into an Eclipse workspace?"
yprompt "Can juneau/juneau-examples/juneau-examples-rest-jetty/target/juneau-examples-rest-jetty-$X_VERSION.bin.zip be deployed into an Eclipse workspace?"
yprompt "Can juneau/juneau-examples/juneau-examples-rest-springboot/target/juneau-examples-rest-springboot-$X_VERSION.bin.zip be deployed into an Eclipse workspace?"

cd $X_STAGING/git/juneau
mvn deploy

mvn release:prepare -DautoVersionSubmodules=true -DreleaseVersion=$X_VERSION -Dtag=$X_RELEASE -DdevelopmentVersion=$X_NEXT_VERSION

yprompt "Did the release:prepare command succeed?"

git diff $X_RELEASE

mvn release:perform

open "https://repository.apache.org/#stagingRepositories"

echo "On Apache's Nexus instance, locate the staging repository for the code you just released.  It should be called something like orgapachejuneau-1000." 
echo "Check the Updated time stamp and click to verify its Content."
echo "Important - When all artifacts to be deployed are in the staging repository, tick the box next to it and click Close."
echo "DO NOT CLICK RELEASE YET - the release candidate must pass [VOTE] emails on dev@juneau before we release."
echo "Once closing has finished (check with Refresh), browse to the URL of the staging repository which should be something like https://repository.apache.org/content/repositories/orgapachejuneau-1000."
echo " "
echo "Enter the staging repository name: orgapachejuneau-"

read prompt
export X_REPO=$prompt;

yprompt "X_REPO = $X_REPO.  Is this correct?"

cd $X_STAGING
rm -rf dist 
svn co https://dist.apache.org/repos/dist/dev/juneau dist
svn rm dist/source/*
svn rm dist/binaries/*
mkdir dist/source/$X_RELEASE
mkdir dist/binaries/$X_RELEASE 
cd $X_STAGING/dist/source/$X_RELEASE
wget -e robots=off --recursive --no-parent --no-directories -A "*-source-release*" https://repository.apache.org/content/repositories/$X_REPO/org/apache/juneau/
mv juneau-${X_VERSION}-source-release.zip apache-juneau-${X_VERSION}-src.zip
mv juneau-${X_VERSION}-source-release.zip.asc apache-juneau-${X_VERSION}-src.zip.asc
mv juneau-${X_VERSION}-source-release.zip.md5 apache-juneau-${X_VERSION}-src.zip.md5
gpg --print-md SHA512 apache-juneau-${X_VERSION}-src.zip > apache-juneau-${X_VERSION}-src.zip.sha512
rm *.sha1
cd $X_STAGING/dist/binaries/$X_RELEASE
wget -e robots=off --recursive --no-parent --no-directories -A "juneau-distrib*-bin.zip*" https://repository.apache.org/content/repositories/$X_REPO/org/apache/juneau/
mv juneau-distrib-${X_VERSION}-bin.zip apache-juneau-${X_VERSION}-bin.zip
mv juneau-distrib-${X_VERSION}-bin.zip.asc apache-juneau-${X_VERSION}-bin.zip.asc
mv juneau-distrib-${X_VERSION}-bin.zip.md5 apache-juneau-${X_VERSION}-bin.zip.md5
gpg --print-md SHA512 apache-juneau-${X_VERSION}-bin.zip > apache-juneau-${X_VERSION}-bin.zip.sha512
rm *.sha1
cd $X_STAGING/dist
svn add source/$X_RELEASE
svn add binaries/$X_RELEASE
svn commit -m "$X_RELEASE"

open "https://dist.apache.org/repos/dist/dev/juneau"
yprompt "Are the files available at https://dist.apache.org/repos/dist/dev/juneau?"

echo "Voting can be started."

success;
