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
package org.apache.juneau.rest.server.view.freemarker.console;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.server.console.*;
import org.junit.jupiter.api.*;

/**
 * Build-time bus wiring checks for a terminal card that subscribes to {@code term.nudge:<id>} with the {@code nudge} role.
 */
class TerminalNudgeWiring_Test extends TestBase {

	private static final String TOPIC = "term.nudge:t1";

	private static String contract(String subscribeAs, String declaredTopic, String downstream) {
		var topics = declaredTopic == null ? "" : "\"topics\":[{\"topic\":\"" + declaredTopic + "\",\"retain\":false,\"publisher\":\"server\"}],";
		var bridges = downstream == null ? "" : "\"bridges\":[{\"id\":\"live\",\"transport\":\"sse\",\"session\":\"/bus/session\",\"downstream\":[\"" + downstream + "\"]}],";
		return "{\"version\":\"1\",\"title\":\"T\",\"nav\":[],\"activeNav\":[]," + topics + bridges
			+ "\"cards\":[{\"id\":\"t1\",\"type\":\"terminal\",\"subscribes\":[{\"topic\":\"" + TOPIC + "\",\"as\":\"" + subscribeAs + "\"}]}]}";
	}

	private static List<String> problems(String contract) throws Exception {
		return BusWiringValidator.validate(JsonMap.ofString(contract), CardTypeRegistry.standard()).stream().map(Object::toString).toList();
	}

	private static List<String> codes(String contract) throws Exception {
		return BusWiringValidator.validate(JsonMap.ofString(contract), CardTypeRegistry.standard()).stream().map(p -> p.toString().split(" ")[0]).toList();
	}

	@Test void a01_fullWiringPasses() throws Exception {
		assertEquals(List.of(), problems(contract("nudge", TOPIC, TOPIC)));
	}

	@Test void a02_missingDeclaration() throws Exception {
		// E-41: nothing publishes the subscribed topic; E-51: the bridge carries an undeclared topic.
		assertEquals(List.of("E-41", "E-51"), codes(contract("nudge", null, TOPIC)));
	}

	@Test void a03_missingBridge() throws Exception {
		assertEquals(List.of("E-52"), codes(contract("nudge", TOPIC, null)));
	}

	@Test void a04_unknownRole() throws Exception {
		assertEquals(List.of("E-44"), codes(contract("nudges", TOPIC, TOPIC)));
	}

	@Test void a05_wildcardBridgeRejected() throws Exception {
		assertTrue(codes(contract("nudge", TOPIC, "term.nudge:*")).contains("E-51"));
	}

	@Test void a06_wildcardDeclarationRejected() throws Exception {
		assertTrue(codes(contract("nudge", "term.nudge:*", TOPIC)).contains("E-52"));
	}
}
