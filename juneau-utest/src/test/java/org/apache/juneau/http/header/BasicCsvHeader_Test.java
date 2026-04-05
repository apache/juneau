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

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.http.HttpHeaders.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.junit.jupiter.api.*;

class BasicCsvHeader_Test extends TestBase {

	private static final String HEADER = "Foo";
	private static final String VALUE = "foo, bar";
	private static final String[] PARSED = { "foo", "bar" };

	@Rest
	public static class A {
		@RestOp
		public StringReader get(@Header(name=HEADER) @Schema(cf="multi") String[] h) {
			return reader(h == null ? "null" : join(h, '|'));
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Method tests
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_basic() throws Exception {
		var c = client().build();

		// Normal usage.
		c.get().header(csvHeader(HEADER,VALUE)).run().assertContent(VALUE);
		c.get().header(csvHeader(HEADER,VALUE)).run().assertContent(VALUE);
		c.get().header(csvHeader(HEADER,PARSED)).run().assertContent(VALUE);
		c.get().header(csvHeader(HEADER,()->PARSED)).run().assertContent(VALUE);

		// Invalid usage.
		c.get().header(csvHeader(HEADER,(Supplier<String[]>)null)).run().assertContent().isEmpty();
		c.get().header(csvHeader(HEADER,()->null)).run().assertContent().isEmpty();
		assertThrowsWithMessage(IllegalArgumentException.class, "Name cannot be empty on header.", ()->csvHeader("", VALUE));
		assertThrowsWithMessage(IllegalArgumentException.class, "Name cannot be empty on header.", ()->csvHeader(null, VALUE));
		assertThrowsWithMessage(IllegalArgumentException.class, "Name cannot be empty on header.", ()->csvHeader("", PARSED));
		assertThrowsWithMessage(IllegalArgumentException.class, "Name cannot be empty on header.", ()->csvHeader(null, PARSED));
		assertThrowsWithMessage(IllegalArgumentException.class, "Name cannot be empty on header.", ()->csvHeader("", ()->PARSED));
		assertThrowsWithMessage(IllegalArgumentException.class, "Name cannot be empty on header.", ()->csvHeader(null, ()->PARSED));
	}

	@Test void a02_contains() {
		var x = new BasicCsvHeader("Foo", (String)null,"bar","baz");
		assertTrue(x.contains(null));
		assertTrue(x.containsIgnoreCase(null));
		assertTrue(x.contains("bar"));
		assertTrue(x.containsIgnoreCase("bar"));
		assertFalse(x.contains("qux"));
		assertFalse(x.containsIgnoreCase("qux"));
		assertFalse(x.contains("BAR"));
		assertTrue(x.containsIgnoreCase("BAR"));

		var x2 = csvHeader("Foo",()->null);
		assertFalse(x2.contains((String)null));
		assertFalse(x2.containsIgnoreCase(null));
		assertFalse(x2.contains("bar"));
		assertFalse(x2.containsIgnoreCase("bar"));
	}

	@Test void a03_assertList() {
		csvHeader("Foo", "bar").assertList().isContains("bar").assertList().isNotContains("baz");
		new BasicCsvHeader("Foo", (String)null).assertList().isNull();
	}

	@Test void a04_factoryNullReturns() {
		assertNull(BasicCsvHeader.of("Foo", (String)null));
		assertNull(BasicCsvHeader.of("Foo", (String[])null));
		assertNull(BasicCsvHeader.of("Foo", (Supplier<String[]>)null));
	}

	@Test void a05_additionalMethods() {
		var h = new BasicCsvHeader("Foo", "bar", "baz");
		assertTrue(h.asArray().isPresent());
		assertEquals(2, h.asArray().get().length);
		assertTrue(h.asList().isPresent());
		assertEquals(2, h.asList().get().size());
		assertEquals(2, h.orElse((String[])null).length);
		assertEquals(2, h.toArray().length);
		assertEquals(2, h.toList().size());

		var h2 = csvHeader("Foo", ()->null);
		assertFalse(h2.asArray().isPresent());
		assertFalse(h2.asList().isPresent());
		assertEquals(1, h2.orElse(new String[]{"other"}).length);
		assertNull(h2.toArray());
		assertNull(h2.toList());

		var h3 = csvHeader("Foo", ()->new String[]{"foo", "bar"});
		assertTrue(h3.contains("foo"));
		assertTrue(h3.containsIgnoreCase("FOO"));
		assertFalse(h3.contains("baz"));
		assertFalse(h3.containsIgnoreCase("BAZ"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static RestClient.Builder client() {
		return MockRestClient.create(A.class);
	}
}