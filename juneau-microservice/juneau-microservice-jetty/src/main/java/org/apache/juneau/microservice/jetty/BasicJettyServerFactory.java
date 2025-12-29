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
package org.apache.juneau.microservice.jetty;

import static org.apache.juneau.commons.utils.FileUtils.*;
import static org.apache.juneau.commons.utils.IoUtils.*;

import java.io.*;
import java.util.*;

import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.resource.*;
import org.eclipse.jetty.xml.*;

/**
 * Basic implementation of a Jetty server factory.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauMicroserviceJettyBasics">juneau-microservice-jetty Basics</a>
 * </ul>
 */
public class BasicJettyServerFactory implements JettyServerFactory {

	@Override
	public Server create(String jettyXml) throws Exception {
		Objects.requireNonNull(jettyXml,
			"jetty.xml file location was not specified in the configuration file (Jetty/config) or manifest file (Jetty-Config) or found on the file system or classpath.");
		var f = createTempFile("jetty.xml");
		try (var r = new StringReader(jettyXml); Writer w = new FileWriter(f)) {
			pipe(r, w);
			w.flush();
		}
		var xmlConfiguration = new XmlConfiguration(new PathResourceFactory().newResource(f.toPath()));
		return (Server)xmlConfiguration.configure();
	}
}