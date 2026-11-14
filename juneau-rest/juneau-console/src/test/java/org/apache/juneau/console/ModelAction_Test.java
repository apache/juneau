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
package org.apache.juneau.console;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class ModelAction_Test extends TestBase {
	@Test void toMap_shape_and_order() {
		var m = ModelAction.of("go").icon("i").label("L").tooltip("T").tone("info").handler("app.go").payload(Map.of("a", 1)).toMap();
		assertEquals(List.of("type", "id", "icon", "label", "tooltip", "tone", "handler", "payload"), new ArrayList<>(m.keySet()));
		assertEquals("handler", m.get("type"));
		assertEquals("app.go", m.get("handler"));
	}
	@Test void toMap_minimal() {
		assertEquals(List.of("type", "id", "handler"), new ArrayList<>(ModelAction.of("go").handler("app.go").toMap().keySet()));
	}
	@Test void toMap_requiresHandler() {
		var action = ModelAction.of("go");
		assertThrows(IllegalStateException.class, action::toMap);
	}
	@Test void id_grammar() {
		assertThrows(IllegalArgumentException.class, () -> ModelAction.of("1 bad"));
		assertThrows(IllegalArgumentException.class, () -> ModelAction.of(""));
		var tooLong = "x".repeat(129);
		assertThrows(IllegalArgumentException.class, () -> ModelAction.of(tooLong));
		assertEquals("a.b_c-1", ModelAction.of("a.b_c-1").id());
	}
	@Test void handler_badName() {
		var action = ModelAction.of("go");
		assertThrows(IllegalArgumentException.class, () -> action.handler("Bad"));
	}
	@Test void handler_inline_json() {
		assertEquals(Map.of("inline", "x"), ModelAction.of("go").handler(HandlerRef.inline("x")).toMap().get("handler"));
	}
}
