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
package org.apache.juneau.http;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.http.header.*;
import org.junit.jupiter.api.*;

class EntityTag_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Method tests
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_basic() {

		var x1 = new EntityTag("\"foo\"");
		assertString("\"foo\"", x1);
		assertEquals("foo", x1.getEntityValue());
		assertFalse(x1.isWeak());
		assertFalse(x1.isAny());

		var x2 = new EntityTag("W/\"foo\"");
		assertString("W/\"foo\"", x2);
		assertEquals("foo", x2.getEntityValue());
		assertTrue(x2.isWeak());
		assertFalse(x2.isAny());

		var x3 = new EntityTag("*");
		assertString("*", x3);
		assertEquals("*", x3.getEntityValue());
		assertFalse(x3.isWeak());
		assertTrue(x3.isAny());

		var x5 = new EntityTag("\"\"");
		assertString("\"\"", x5);
		assertEquals("", x5.getEntityValue());
		assertFalse(x5.isWeak());
		assertFalse(x5.isAny());

		var x6 = EntityTag.of("\"foo\"");
		assertString("\"foo\"", x6);
		assertEquals("foo", x6.getEntityValue());
		assertFalse(x6.isWeak());
		assertFalse(x6.isAny());

		var x7 = EntityTag.of((Supplier<?>)()->"\"foo\"");
		assertString("\"foo\"", x7);
		assertEquals("foo", x7.getEntityValue());
		assertFalse(x7.isWeak());
		assertFalse(x7.isAny());

		assertNull(EntityTag.of(null));
		assertNull(EntityTag.of((Supplier<?>)()->null));

		assertThrowsWithMessage(IllegalArgumentException.class, "Invalid value for entity-tag: [foo]", ()->new EntityTag("foo"));
		assertThrowsWithMessage(IllegalArgumentException.class, "Invalid value for entity-tag: [\"]", ()->new EntityTag("\""));
		assertThrowsWithMessage(IllegalArgumentException.class, "Invalid value for entity-tag: []", ()->new EntityTag(""));
		assertThrowsWithMessage(IllegalArgumentException.class, "Argument 'value' cannot be null.", ()->new EntityTag(null));
		assertThrowsWithMessage(IllegalArgumentException.class, "Invalid value for entity-tag: [\"a]", ()->new EntityTag("\"a"));
		assertThrowsWithMessage(IllegalArgumentException.class, "Invalid value for entity-tag: [a\"]", ()->new EntityTag("a\""));
		assertThrowsWithMessage(IllegalArgumentException.class, "Invalid value for entity-tag: [W/\"]", ()->new EntityTag("W/\""));
	}
}