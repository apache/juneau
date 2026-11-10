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

class HandlerRef_Test extends TestBase {
	@Test void named_ok() { assertEquals("orders.refresh", HandlerRef.named("orders.refresh").name()); }
	@Test void named_badGrammar() { assertThrows(IllegalArgumentException.class, () -> HandlerRef.named("refresh")); }
	@Test void named_upperFirst() { assertThrows(IllegalArgumentException.class, () -> HandlerRef.named("Orders.refresh")); }
	@Test void named_juneauNs_rejected() { assertThrows(IllegalArgumentException.class, () -> HandlerRef.named("juneau.nope")); }
	@Test void builtin_prefixes() { assertEquals("juneau.refresh", HandlerRef.builtin("refresh").name()); }
	@Test void builtin_unknown() { assertThrows(IllegalArgumentException.class, () -> HandlerRef.builtin("nope")); }
	@Test void inline_isInline() { assertTrue(HandlerRef.inline("target.refresh();").isInline()); }
	@Test void inline_blank() { assertThrows(IllegalArgumentException.class, () -> HandlerRef.inline(" ")); }
	@Test void named_juneauBuiltinAccepted() { assertEquals("juneau.countLabel", HandlerRef.named("juneau.countLabel").name()); }
	@Test void toJson_named_and_inline() {
		assertEquals("a.b", HandlerRef.named("a.b").toJson());
		assertEquals(Map.of("inline", "x"), HandlerRef.inline("x").toJson());
	}
}
