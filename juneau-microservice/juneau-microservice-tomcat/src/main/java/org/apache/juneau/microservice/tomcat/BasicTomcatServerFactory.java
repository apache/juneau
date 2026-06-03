/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.microservice.tomcat;

import java.io.*;
import java.util.*;

import org.apache.catalina.startup.*;

/**
 * Basic implementation of a Tomcat server factory.
 *
 * <p>
 * Builds a programmatic {@link Tomcat} instance with the supplied Catalina base directory and a single root
 * context mounted at <c>""</c> with the base directory as its doc-base.  Auto-deployment is disabled (servlets
 * are mounted explicitly by {@link TomcatServerComponent}).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauMicroserviceTomcatBasics">juneau-microservice-tomcat Basics</a>
 * </ul>
 */
public class BasicTomcatServerFactory implements TomcatServerFactory {

	/**
	 * Constructor.
	 */
	public BasicTomcatServerFactory() { /* intentionally empty */ }

	@Override /* Overridden from TomcatServerFactory */
	public Tomcat create(File baseDir) throws Exception {
		Objects.requireNonNull(baseDir, "Catalina base directory was not specified.");
		var tomcat = new Tomcat();
		tomcat.setBaseDir(baseDir.getAbsolutePath());
		var context = tomcat.addContext("", baseDir.getAbsolutePath());
		context.setSessionTimeout(30);
		tomcat.getHost().setAutoDeploy(false);
		tomcat.getHost().setDeployOnStartup(false);
		return tomcat;
	}
}
