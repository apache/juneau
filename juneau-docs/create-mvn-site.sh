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

# Source the environment setup
. ../juneau-env.sh

function fail_with_message {
	X_DATE=$(date +'%H:%M:%S') 
	echo ' '
	echo "-------------------------------------------------------------------------------"
	echo "[$X_DATE] $1"
	echo '-------------------------------------------------------------------------------'
	exit 1; 
}

echo "Creating Maven site with javadocs for local testing..."

# Ensure we're in the main project directory (/juneau)
JUNEAU_ROOT=$(cd .. && pwd)
cd "$JUNEAU_ROOT"
echo "Working from: $(pwd)"

# Get the actual project version from the POM
PROJECT_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
echo "Detected project version: ${PROJECT_VERSION}"

echo "Generating maven site for root project..."
rm -rf "$JUNEAU_ROOT/target/site/*"
mvn clean compile site  | tee create-mvn-site.log

# Check if the Maven site was generated successfully
if [ ! -d "target/site" ]; then
    fail_with_message "Maven site generation failed - target/site directory not found."
fi

#echo "Generating sites for individual modules..."
## Generate sites for individual modules that are referenced in the navigation
#MODULES="juneau-core juneau-rest juneau-microservice juneau-sc juneau-examples juneau-utest juneau-bean juneau-all juneau-distrib"
#
#for module in $MODULES; do
#    if [ -d "$module" ]; then
#        echo "Generating site for $module..."
#        (cd "$module" && mvn site -q)
#        
#        # Copy the module site to the main site directory
#        if [ -d "$module/target/site" ]; then
#            echo "Copying $module site to main site directory..."
#            mkdir -p "target/site/$module"
#            cp -r "$module/target/site"/* "target/site/$module/"
#        fi
#    fi
#done

echo "Found Maven site in: target/site"

# Remove any existing content in the local testing directory
echo "Setting up local testing directory for Docusaurus..."
rm -rf "$JUNEAU_ROOT/juneau-docs/static/site"
mkdir -p "$JUNEAU_ROOT/juneau-docs/static/site"

# Copy the entire Maven site to the Docusaurus static directory
echo "Copying entire Maven site to Docusaurus static directory..."
cp -v -r target/site/* "$JUNEAU_ROOT/juneau-docs/static/site/"

echo '*******************************************************************************'
echo '***** SUCCESS *****************************************************************'
echo '*******************************************************************************'
echo "Maven site has been generated and copied successfully!"
echo "Complete Maven site is now available in: $JUNEAU_ROOT/juneau-docs/static/site/"
echo "This includes javadocs, project reports, and all other site content."
echo "You can now access it at: http://localhost:3000/site/"
echo "Ready for broken link testing in your Docusaurus documentation!"
