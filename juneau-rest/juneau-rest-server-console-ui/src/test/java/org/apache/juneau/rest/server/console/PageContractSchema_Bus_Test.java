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
import org.apache.juneau.marshall.collections.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * The bus keys of schema 1.1 (spec §5.7, §11.2): top-level {@code topics} / {@code bridges}, and
 * {@code publishes} / {@code subscribes} on every card branch.  Rule checks (R-10, R-11, E-40..E-54) belong to
 * {@link BusWiringValidator}, not the schema; this class pins only shape.
 */
class PageContractSchema_Bus_Test extends TestBase {

	private static String fixture(String name) throws IOException {
		try (var in = PageContractSchema_Bus_Test.class.getResourceAsStream("/contracts/" + name)) {
			assertNotNull(in, () -> "missing fixture " + name);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	@Test void a01_validBusWiringPasses() throws Exception {
		var errors = PageContractSchema.get().validate(fixture("valid-bus-wiring.json"));
		assertTrue(errors.isEmpty(), () -> "valid-bus-wiring.json: " + errors);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
		"invalid-bus-topic-decl-retain.json|$.topics[0]: missing required 'retain'",
		"invalid-bus-topic-decl-family.json|$.topics[0].topic: does not match",
		"invalid-bus-topic-decl-publisher.json|$.topics[0].publisher: must be one of",
		"invalid-bus-bridge-transport.json|$.bridges[0].transport: must be one of",
		"invalid-bus-bridge-extra.json|$.bridges[0].url: not allowed",
		"invalid-bus-bridge-maxattempts.json|$.bridges[0].maxAttempts: expected integer",
		"invalid-bus-bridge-missing.json|$.bridges[0]: missing required 'session'",
		"invalid-bus-sub-as.json|$.cards[0].subscribes[0].as: does not match",
		"invalid-bus-sub-wildcard.json|$.cards[0].subscribes[0].topic: does not match",
		"invalid-bus-sub-whenempty.json|$.cards[0].subscribes[0].whenEmpty: must be one of",
		"invalid-bus-sub-map.json|$.cards[0].subscribes[0].map.x: does not match",
		"invalid-bus-pub-framework.json|$.cards[0].publishes[0].topic: does not match",
		"invalid-bus-pub-retain.json|$.cards[0].publishes[0]: missing required 'retain'",
		"invalid-bus-sub-as-console-output.json|$.cards[0].subscribes[0].as: does not match",
		"invalid-bus-pub-topic-run-view.json|$.cards[0].publishes[0].topic: does not match",
	})
	void a02_invalidBusShapesFail(String name, String expected) throws Exception {
		var errors = PageContractSchema.get().validate(fixture(name));
		assertTrue(errors.stream().anyMatch(e -> e.contains(expected)), () -> name + " expected '" + expected + "' in " + errors);
	}

	@Test void a03_defsPresentAndClosed() throws Exception {
		var defs = JsonMap.ofString(PageContractSchema.get().schemaJson()).getMap("$defs");
		for (var name : new String[] {"bridgeDecl", "topic", "topicDecl", "publication", "subscription", "ribbonPublish"})
			assertTrue(defs.containsKey(name), name);
		for (var name : new String[] {"bridgeDecl", "topicDecl", "publication", "subscription", "ribbonPublish"})
			assertEquals(false, defs.getMap(name).get("additionalProperties"), name + " is closed");
		for (var branch : new String[] {"card", "htmlCard", "datatablesCard", "consoleOutputCard", "runViewCard", "terminalCard"}) {
			var props = defs.getMap(branch).getMap("properties");
			assertTrue(props.containsKey("publishes") && props.containsKey("subscribes"), branch + " admits the wiring keys");
		}
	}
}
