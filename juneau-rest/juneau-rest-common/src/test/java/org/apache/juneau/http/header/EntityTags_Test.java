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
package org.apache.juneau.http.header;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class EntityTags_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Method tests
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_of_array() {
		var t1 = new EntityTag("\"foo\"");
		var t2 = new EntityTag("\"bar\"");

		var x1 = EntityTags.of(t1, t2);
		assertString("\"foo\", \"bar\"", x1);
		assertList(x1.toList(), "\"foo\"", "\"bar\"");
		assertEquals(2, x1.toArray().length);

		assertNull(EntityTags.of((EntityTag[])null));
	}

	@Test void a02_of_string() {
		var x1 = EntityTags.of("\"foo\", \"bar\"");
		assertString("\"foo\", \"bar\"", x1);
		assertList(x1.toList(), "\"foo\"", "\"bar\"");

		assertSame(EntityTags.EMPTY, EntityTags.of((String)null));
		assertSame(EntityTags.EMPTY, EntityTags.of(""));

		// Second call for the same non-empty value should hit the cache.
		var x2 = EntityTags.of("\"foo\", \"bar\"");
		assertString("\"foo\", \"bar\"", x2);
	}

	@Test void a03_constructors() {
		var x1 = new EntityTags((EntityTag[])null);
		assertNull(x1.toArray());
		assertNull(x1.toList());

		var x2 = new EntityTags((String)null);
		assertString(null, x2);
		assertEquals(0, x2.toArray().length);
	}

	@Test void b01_equals() {
		var t1 = new EntityTag("\"foo\"");
		var t2 = new EntityTag("\"bar\"");

		var a = EntityTags.of(t1, t2);
		assertEquals(a, a);
		assertEquals(a, EntityTags.of(t1, t2));
		assertEquals(a.hashCode(), EntityTags.of(t1, t2).hashCode());
		assertNotEquals(a, (Object)"not-entity-tags");
		assertNotEquals(a, null);
		assertNotEquals(a, EntityTags.of(t1));
		assertNotEquals(a, EntityTags.of(t2, t1));
	}
}
