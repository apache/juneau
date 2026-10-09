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
package org.apache.juneau.rest.server.console;

import static java.nio.charset.StandardCharsets.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * The shell attaches the contract's server bridges (spec §11.5): {@code juneau-console.js} calls
 * {@code JuneauViews.bus.wiring.attachBridges(contract)} exactly once, after Task 5's wiring validation, so the
 * page's error painter is already registered.  The behavior of {@code attachBridges} is pinned by
 * {@code ViewsJs_BusBridgeWs_Test}; the end-to-end bridge is {@code ConsoleBusBridge_BrowserTest} (Task 18).
 */
class ConsoleShell_AttachBridges_Test extends TestBase {

	private static String shell() throws IOException {
		try (var in = ConsoleShell_AttachBridges_Test.class.getResourceAsStream("/org/apache/juneau/console/juneau-console.js")) {
			assertNotNull(in, "juneau-console.js");
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	@Test void a01_shellAttachesContractBridgesOnceAfterWiringValidation() throws Exception {
		var js = shell();
		var validate = js.indexOf(".wiring.validate(");
		var attach = js.indexOf(".wiring.attachBridges(");
		assertTrue(validate >= 0, "Task 5's wiring.validate call is missing");
		assertTrue(attach > validate, "attachBridges must be called after wiring.validate");
		assertEquals(attach, js.lastIndexOf(".wiring.attachBridges("), "attachBridges is called exactly once");
	}
}
