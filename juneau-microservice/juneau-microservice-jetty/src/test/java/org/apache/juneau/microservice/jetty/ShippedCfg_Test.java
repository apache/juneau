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

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;

import org.apache.juneau.config.*;
import org.apache.juneau.microservice.console.*;
import org.junit.jupiter.api.*;

/**
 * Verifies the shipped <c>.cfg</c> files still reference console commands and Jetty config files that exist.
 */
class ShippedCfg_Test {

	// Surefire runs with the module directory as the working directory.
	private static final Path ROOT = Path.of("").toAbsolutePath().getParent().getParent();

	private static final String[] CFGS = {
		"juneau-microservice/juneau-microservice-jetty/src/main/resources/juneau.cfg",
		"juneau-petstore/juneau-petstore-jetty/juneau-petstore-jetty.cfg",
		"juneau-sc/juneau-sc-server/juneau-server-config.cfg"
	};

	private static Config load(String cfg) throws Exception {
		var p = ROOT.resolve(cfg);
		assertTrue(Files.isRegularFile(p), "Missing shipped cfg: " + p);
		return Config.create().memStore().build().load(Files.readString(p), true);
	}

	@Test void a01_consoleCommandsResolve() throws Exception {
		for (var cfg : CFGS) {
			var commands = load(cfg).get("Console/commands").asStringArray().orElse(new String[0]);
			assertTrue(commands.length > 0, cfg + ": no console commands listed");
			for (var s : commands) {
				var c = Class.forName(s.trim());
				assertTrue(ConsoleCommand.class.isAssignableFrom(c), cfg + ": " + s + " is not a ConsoleCommand");
			}
		}
	}

	@Test void a02_jettyConfigFileResolves() throws Exception {
		// The Jetty config path is looked up on the classpath or working directory of the module that ships the cfg.
		var cfgs = new String[][] {
			{"juneau-petstore/juneau-petstore-jetty/juneau-petstore-jetty.cfg", "juneau-petstore/juneau-petstore-jetty/src/main/resources"},
			{"juneau-sc/juneau-sc-server/juneau-server-config.cfg", "juneau-sc/juneau-sc-server/src/main/resources"}
		};
		for (var e : cfgs) {
			var jetty = load(e[0]).get("Jetty/config").asString().orElse(null);
			assertNotNull(jetty, e[0] + ": no Jetty/config");
			assertTrue(Files.isRegularFile(ROOT.resolve(e[1]).resolve(jetty)), e[0] + ": Jetty/config not found: " + jetty);
		}
	}
}
