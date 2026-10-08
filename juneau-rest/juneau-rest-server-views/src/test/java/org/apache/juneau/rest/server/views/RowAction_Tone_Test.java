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
package org.apache.juneau.rest.server.views;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

/** Unit tests for {@link RowAction}'s {@code tone}, {@code confirmLabel} and {@code confirmRenderer} fields. */
class RowAction_Tone_Test extends TestBase {

	@Test void a01_unsetFieldsAreNull() {
		var a = RowAction.create("x");
		assertNull(a.tone);
		assertNull(a.confirmLabel);
		assertNull(a.confirmRenderer);
	}

	@Test void a02_dangerWireToken() {
		assertEquals("danger", RowAction.Tone.DANGER.wire());
		assertEquals("danger", RowAction.create("x").tone(RowAction.Tone.DANGER).tone);
	}

	@Test void a03_defaultWireToken() {
		assertEquals("default", RowAction.Tone.DEFAULT.wire());
		assertEquals("default", RowAction.create("x").tone(RowAction.Tone.DEFAULT).tone);
	}

	@Test void a04_confirmLabelSetVerbatim() {
		assertEquals("Delete forever", RowAction.create("x").confirmLabel("Delete forever").confirmLabel);
	}

	@Test void a05_confirmRendererSetVerbatim() {
		assertEquals("styled", RowAction.create("x").confirmRenderer("styled").confirmRenderer);
	}

	@Test void a06_fluentChainReturnsSameInstance() {
		var a = RowAction.create("x");
		assertSame(a, a.tone(RowAction.Tone.DANGER).confirmLabel("Go").confirmRenderer("r"));
	}

	@Test void a07_serializesOnlyWhenSet() {
		var plain = Json5.of(RowAction.create("x"));
		assertFalse(plain.contains("tone"), plain);
		assertFalse(plain.contains("confirmLabel"), plain);
		var json = Json5.of(RowAction.create("x").tone(RowAction.Tone.DANGER).confirmLabel("Go").confirmRenderer("r"));
		assertTrue(json.contains("tone:'danger'"), json);
		assertTrue(json.contains("confirmLabel:'Go'"), json);
		assertTrue(json.contains("confirmRenderer:'r'"), json);
	}
}
