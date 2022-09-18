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

. ~/.profile

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
	exit 0; 
}

function message {
	X_DATE=$(date +'%H:%M:%S') 
	echo ' '
	echo "-------------------------------------------------------------------------------"
	echo "[$X_DATE] $1"
	echo '-------------------------------------------------------------------------------'
}

function fail_with_message {
	X_DATE=$(date +'%H:%M:%S') 
	echo ' '
	echo "-------------------------------------------------------------------------------"
	echo "[$X_DATE] $1"
	echo '-------------------------------------------------------------------------------'
	fail; 
}

function yprompt {
	echo ' '
	echo -n "$1 (Y/n): "
	read prompt
	if [ "$prompt" != "Y" ] && [ "$prompt" != "y" ] && [ "$prompt" != "" ] 
	then 
		fail;
	fi
}

function st {
	SECONDS=0
}

function et {
	echo "Execution time: ${SECONDS}s" 
}

command -v wget || fail_with_message "wget not found"
command -v gpg || fail_with_message "gpg not found"
command -v /usr/local/bin/gpg || fail_with_message "gpg not found in /usr/local/bin"
command -v svn || fail_with_message "svn not found"
export GPG_TTY=$(tty)

message "Checking Java version"
java -version
yprompt "Are you using at least Java 8?";

message "Checking Maven version"
mvn -version
yprompt "Are you using at least Maven 3?"

cd ~/.m2

st
if [ "$X_CLEANM2" != "N" ] && [ "$X_CLEANM2" != "n" ] 
then
	message "Cleaning Maven repository"
	mv repository repository-old
	rm -rf repository-old & 
fi
et

message "Making git folder"
st
rm -rf $X_STAGING
mkdir -p $X_STAGING
mkdir $X_STAGING/git
cd $X_STAGING/git
et

message "Cloning juneau.git"
st
git clone https://gitbox.apache.org/repos/asf/juneau.git
et

#message "Cloning juneau-website.git"
#st
#git clone https://gitbox.apache.org/repos/asf/juneau-website.git
#et

cd juneau
git config user.name $X_USERNAME
git config user.email $X_EMAIL

message "Running clean verify"
st
cd $X_STAGING/git/juneau
mvn clean verify
et

message "Running javadoc:aggregate"
st
mvn javadoc:aggregate
yprompt "Is the javadoc generation clean?"
et

message "Creating test workspace"
WORKSPACE=target/workspace
XV=${X_VERSION}-SNAPSHOT
rm -Rf $WORKSPACE
mkdir -p $WORKSPACE

ZIP_SRC=juneau-microservice/juneau-my-jetty-microservice/target/my-jetty-microservice-$XV-bin.zip
ZIP_TGT=$WORKSPACE/my-jetty-microservice
echo Unzipping $ZIP_SRC to $ZIP_TGT
unzip -o $ZIP_SRC -d $ZIP_TGT

ZIP_SRC=juneau-microservice/juneau-my-springboot-microservice/target/my-springboot-microservice-$XV-bin.zip
ZIP_TGT=$WORKSPACE/my-springboot-microservice
echo Unzipping $ZIP_SRC to $ZIP_TGT
unzip -o $ZIP_SRC -d $ZIP_TGT

ZIP_SRC=juneau-examples/juneau-examples-core/target/juneau-examples-core-$XV-bin.zip
ZIP_TGT=$WORKSPACE/juneau-examples-core
echo Unzipping $ZIP_SRC to $ZIP_TGT
unzip -o $ZIP_SRC -d $ZIP_TGT

ZIP_SRC=juneau-examples/juneau-examples-rest-jetty/target/juneau-examples-rest-jetty-$XV-bin.zip
ZIP_TGT=$WORKSPACE/juneau-examples-rest-jetty
echo Unzipping $ZIP_SRC to $ZIP_TGT
unzip -o $ZIP_SRC -d $ZIP_TGT

ZIP_SRC=juneau-examples/juneau-examples-rest-springboot/target/juneau-examples-rest-springboot-$XV-bin.zip
ZIP_TGT=$WORKSPACE/juneau-examples-rest-springboot
echo Unzipping $ZIP_SRC to $ZIP_TGT
unzip -o $ZIP_SRC -d $ZIP_TGT

yprompt "Can all workspace projecs in $X_STAGING/git/juneau/target/workspace be cleanly imported as Maven projects into Eclipse?"

message "Running deploy"
st
cd $X_STAGING/git/juneau
mvn deploy
et

message "Running release:prepare"
mvn release:prepare -DautoVersionSubmodules=true -DreleaseVersion=$X_VERSION -Dtag=$X_RELEASE -DdevelopmentVersion=$X_NEXT_VERSION
yprompt "Did the release:prepare command succeed?"

message "Running git diff"
git diff $X_RELEASE

message "Running release:perform"
st
mvn release:perform
open "https://repository.apache.org/#stagingRepositories"
et

echo "On Apache's Nexus instance, locate the staging repository for the code you just released.  It should be called something like orgapachejuneau-1000." 
echo "Check the Updated time stamp and click to verify its Content."
echo "IMPORTANT - When all artifacts to be deployed are in the staging repository, tick the box next to it and click Close."
echo "DO NOT CLICK RELEASE YET - the release candidate must pass [VOTE] emails on dev@juneau before we release."
echo "Once closing has finished (check with Refresh), browse to the URL of the staging repository which should be something like https://repository.apache.org/content/repositories/orgapachejuneau-1000."
echo " "
echo "Enter the staging repository name AFTER CLOSING IT!!!: orgapachejuneau-"

read prompt
export X_REPO="orgapachejuneau-$prompt";

yprompt "X_REPO = $X_REPO.  Is this correct?"

message "Creating binary artifacts"
st
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
gpg --print-md SHA512 apache-juneau-${X_VERSION}-src.zip > apache-juneau-${X_VERSION}-src.zip.sha512
rm *.sha1
cd $X_STAGING/dist/binaries/$X_RELEASE
wget -e robots=off --recursive --no-parent --no-directories -A "juneau-distrib*-bin.zip*" https://repository.apache.org/content/repositories/$X_REPO/org/apache/juneau/
mv juneau-distrib-${X_VERSION}-bin.zip apache-juneau-${X_VERSION}-bin.zip
mv juneau-distrib-${X_VERSION}-bin.zip.asc apache-juneau-${X_VERSION}-bin.zip.asc
gpg --print-md SHA512 apache-juneau-${X_VERSION}-bin.zip > apache-juneau-${X_VERSION}-bin.zip.sha512
rm *.sha1
cd $X_STAGING/dist
svn add source/$X_RELEASE
svn add binaries/$X_RELEASE
svn commit -m "$X_RELEASE"
et

open "https://dist.apache.org/repos/dist/dev/juneau"
yprompt "Are the files available at https://dist.apache.org/repos/dist/dev/juneau?"

echo "Voting can be started."

success;
