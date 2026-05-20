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
package org.apache.juneau;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.json5.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link MarshallingSession}.
 *
 * <p>
 * Confirms that the session-level parse helpers tolerate null input now that
 * {@link Json5Map#ofString(CharSequence)} returns an empty instance instead of {@code null}.
 */
class MarshallingSession_Test extends TestBase {

	@Test void a01_parseToMapNullReturnsEmptySessionAttachedMap() {
		var session = MarshallingContext.DEFAULT.getSession();
		var m = session.parseToMap(null);
		assertNotNull(m);
		assertTrue(m.isEmpty());
		assertTrue(m instanceof Json5Map);
	}

	@Test void a02_parseToMapEmptyParsesToEmptyMap() {
		var session = MarshallingContext.DEFAULT.getSession();
		var m = session.parseToMap("{}");
		assertNotNull(m);
		assertTrue(m.isEmpty());
		assertTrue(m instanceof Json5Map);
	}

	@Test void a03_parseToMapPopulatedInputParsesNormally() {
		var session = MarshallingContext.DEFAULT.getSession();
		var m = session.parseToMap("{a:1,b:'two'}");
		assertEquals(2, m.size());
		assertEquals(1, ((Json5Map)m).getInt("a"));
		assertEquals("two", ((Json5Map)m).getString("b"));
	}
}
