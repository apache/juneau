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
package org.apache.juneau.microservice;

import static org.apache.juneau.rest.annotation.HookEvent.*;

import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.svl.vars.*;

/**
 * Superclass for all REST resource groups.
 * 
 * <p>
 * In additional to the functionality of the {@link RestServletGroupDefault} group,
 * augments the {@link RestContext#getVarResolver()} method with the following additional variable types:
 * <ul class='spaced-list'>
 * 	<li>
 * 		<jk>$ARG{...}</jk> - Command line arguments.
 * 		<br>Resolves values from {@link Microservice#getArgs()}.
 * 		
 * 		<h6>Example:</h6>
 * 		<p class='bcode'>
 * 	String firstArg = request.getVarResolver().resolve(<js>"$ARG{0}"</js>);  <jc>// First argument.</jc>
 * 	String namedArg = request.getVarResolver().resolve(<js>"$ARG{myarg}"</js>);  <jc>// Named argument (e.g. "myarg=foo"). </jc>
 * 		</p>
 * 	<li>
 * 		<jk>$MF{...}</jk> - Manifest file entries.
 * 		
 * 		<h6>Example:</h6>
 * 		<p class='bcode'>
 * 	String mainClass = request.getVarResolver().resolve(<js>"$MF{Main-Class}"</js>);  <jc>// Main class. </jc>
 * 		</p>
 * </ul>
 */
@SuppressWarnings("serial")
@RestResource(
	htmldoc=@HtmlDoc(
		links={
			"up: request:/..",
			"options: servlet:/?method=OPTIONS"
		},
		stylesheet="$C{REST/stylesheet,servlet:/styles/devops.css}"
	),
	config="$S{juneau.configFile}"
)
public abstract class ResourceGroup extends RestServletGroupDefault {

	/**
	 * Initializes the registry URL and rest clent.
	 * 
	 * @param config The resource config.
	 * @throws Exception
	 */
	@RestHook(INIT) 
	public void addConfigVars(RestConfig config) throws Exception {
		Microservice m = Microservice.getInstance();
		if (m != null) {
			config
			.addVars(ArgsVar.class, ManifestFileVar.class)
			.addVarContextObject(ArgsVar.SESSION_args, m.getArgs())
			.addVarContextObject(ManifestFileVar.SESSION_manifest, m.getManifest());
		}
	}
}
