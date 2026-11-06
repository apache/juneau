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
 * <h2 class='section'>See Also:</h2><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauMicroserviceJetty">juneau-microservice-jetty</a>
 * </ul>
 */
public class BasicJettyServerFactory implements JettyServerFactory {

	private static final String VALIDATING_PROPERTY = "org.eclipse.jetty.xml.XmlParser.Validating";

	/**
	 * Jetty turns on DTD/schema validation of <c>jetty.xml</c> whenever the JDK's built-in Xerces parser is in use, which
	 * requires fetching the DTD over the network.  Newer JDKs (28+) block that fetch, so the configuration fails to load.
	 * Validation adds nothing for our shipped files, so default it off, unless the user has set the property explicitly.
	 * Jetty reads this property when its (pooled) parser is created, so it must be set before the first
	 * {@link XmlConfiguration} is constructed.
	 */
	private static void disableXmlValidationByDefault() {
		if (System.getProperty(VALIDATING_PROPERTY) == null)
			System.setProperty(VALIDATING_PROPERTY, "false");
	}

	@Override
	public Server create(String jettyXml) throws Exception {
		Objects.requireNonNull(jettyXml,
			"jetty.xml file location was not specified in the configuration file (Jetty/config) or manifest file (Jetty-Config) or found on the file system or classpath.");
		var f = createTempFile("jetty.xml");
		try (var r = new StringReader(jettyXml); Writer w = new FileWriter(f)) {
			pipe(r, w);
			w.flush();
		}
		disableXmlValidationByDefault();
		var xmlConfiguration = new XmlConfiguration(new PathResourceFactory().newResource(f.toPath()));
		return (Server)xmlConfiguration.configure();
	}
}