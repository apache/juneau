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

import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

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

	/** A <c>host</c> setting with no value (empty or self-closing); left as-is it would listen on every interface. */
	private static final Pattern BLANK_HOST = Pattern.compile("<Set\\s+name=\"host\"\\s*(?:>\\s*</Set>|/>)");

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
		disableXmlValidationByDefault();
		// Files.createTempFile creates the file readable and writable by its owner only (where POSIX permissions are supported).
		var f = Files.createTempFile("jetty", ".xml");
		try {
			Files.writeString(f, BLANK_HOST.matcher(jettyXml).replaceAll("<Set name=\"host\">127.0.0.1</Set>"));
			var xmlConfiguration = new XmlConfiguration(new PathResourceFactory().newResource(f));
			return (Server)xmlConfiguration.configure();
		} finally {
			Files.deleteIfExists(f);
		}
	}
}