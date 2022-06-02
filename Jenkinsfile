// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************

timestamps {

	node ('ubuntu') { 
	
		stage ('Juneau-Java-1.8 - Checkout') {
			checkout([$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: '', url: 'https://github.com/apache/juneau']]]) 
		}
		
		stage ('Juneau-Java-1.8 - Build') {
	
			withEnv(["JAVA_HOME=${ tool 'JDK 1.8 (latest)' }", "PATH=$PATH:${env.JAVA_HOME}/bin"]) { 
	
				withMaven(jdk: 'JDK 1.8 (latest)', maven: 'Maven 3.2.5') { 
					sh "echo hello"
					sh "mvn clean install deploy"
				}
				
				junit '**/target/surefire-reports/*.xml' 
				jacoco()
				publishCoverage sourceFileResolver: sourceFiles('NEVER_STORE')
				publishHTML([allowMissing: false, alwaysLinkToLastBuild: false, keepAll: false, reportDir: '', reportFiles: 'index.html', reportName: 'HTML Report', reportTitles: ''])
			}
		}
		
		stage ('Juneau-Java-1.8 - Post build actions') {
			step([$class: 'Mailer', notifyEveryUnstableBuild: true, recipients: 'dev@juneau.apache.org', sendToIndividuals: true])
		}
	}
}