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
package org.apache.juneau.terminal.region;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;

import javax.xml.parsers.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.w3c.dom.*;

/**
 * Guards the module split: the terminal module must stay free of any server-bus dependency, which lives in
 * juneau-terminal-bus instead.
 */
class TerminalModulePom_Test extends TestBase {

	@Test void a01_noBusDependency() throws Exception {
		var pom = new File(System.getProperty("basedir", "."), "pom.xml");
		var doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pom);
		var artifacts = new ArrayList<String>();
		var deps = doc.getElementsByTagName("dependency");
		for (var i = 0; i < deps.getLength(); i++) {
			var kids = deps.item(i).getChildNodes();
			for (var j = 0; j < kids.getLength(); j++) {
				var k = kids.item(j);
				if (k instanceof Element && "artifactId".equals(k.getNodeName()))
					artifacts.add(k.getTextContent().trim());
			}
		}
		assertFalse(artifacts.isEmpty());
		assertFalse(artifacts.contains("juneau-console-bus"), "terminal module must not depend on the bus: " + artifacts);
		assertFalse(artifacts.contains("juneau-console-bus-websocket"), "terminal module must not depend on the bus websocket: " + artifacts);
	}

	// Behavioral check: the test classpath holds the module's full resolved classpath, so this also catches transitive bus dependencies.
	// Assumes no test-scoped dependency (juneau-bct, juneau-rest-mock) brings in a bus module; verified via dependency:tree.
	@Test void a02_busClassesNotOnClasspath() {
		assertThrows(ClassNotFoundException.class, () -> Class.forName("org.apache.juneau.console.bus.ServerBus"), "juneau-console-bus must not be on the terminal classpath, even transitively");
		assertThrows(ClassNotFoundException.class, () -> Class.forName("org.apache.juneau.console.bus.websocket.BusWebSockets"), "juneau-console-bus-websocket must not be on the terminal classpath, even transitively");
	}
}
