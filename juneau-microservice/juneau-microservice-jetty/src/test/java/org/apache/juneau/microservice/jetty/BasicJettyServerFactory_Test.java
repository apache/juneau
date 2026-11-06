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
package org.apache.juneau.microservice.jetty;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.*;

/**
 * Verifies that the shipped <c>jetty.xml</c> can be loaded by {@link BasicJettyServerFactory} on every supported JDK,
 * without the caller having to set any Jetty XML parser system properties.
 */
@ResourceLock(Resources.SYSTEM_PROPERTIES)
class BasicJettyServerFactory_Test {

	private static String shippedJettyXml() throws IOException {
		try (var is = BasicJettyServerFactory.class.getResourceAsStream("/jetty.xml")) {
			assertNotNull(is, "Shipped jetty.xml not found on classpath.");
			return new String(is.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	@Test void a01_createFromShippedJettyXml() throws Exception {
		// Variables such as ${S:availablePort:10000} are resolved by the microservice before the factory is called.
		var xml = shippedJettyXml().replaceAll("\\$\\{[^}]*\\}", "0");
		var prop = "org.eclipse.jetty.xml.XmlParser.Validating";
		var old = System.getProperty(prop);
		System.clearProperty(prop);
		try {
			var server = new BasicJettyServerFactory().create(xml);
			assertNotNull(server);
			assertTrue(server.getConnectors().length > 0);
		} finally {
			if (old != null)
				System.setProperty(prop, old);
		}
	}
}
