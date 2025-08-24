// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.http;

import static org.junit.Assert.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class MediaType_Test extends SimpleTestBase {

	@Test void a01_basic() {
		assertEquals(new MediaType("text/foo"), new MediaType("text/foo"));

		Set<MediaType> x = new TreeSet<>();
		x.add(MediaType.of("text/foo"));
		x.add(MediaType.of("text/bar"));
		assertJson(x, "['text/bar','text/foo']");

		MediaType x2 = new MediaType((String)null);  // Interpreted as "/*"
		assertStringEmpty(x2.getType());
		assertEquals("*", x2.getSubType());
		assertJson(x2.getSubTypes(), "['*']");
		assertTrue(x2.isMetaSubtype());

		var x3 = MediaType.of("text/foo+bar");
		assertTrue(x3.hasSubType("bar"));
		assertFalse(x3.hasSubType("baz"));
		assertFalse(x3.hasSubType(null));
	}

	@Test void a02_match() {
		var x1 = MediaType.of("text/json");
		assertEquals(100000, x1.match(x1,true));

		assertEquals(10210, x1.match(MediaType.of("text/json+foo"),true));
		assertEquals(0, x1.match(MediaType.of("text/json+foo"),false));
	}

	@Test void a03_getParameter() {
		MediaType x1 = MediaType.of("text/json;x=1;q=1;y=2");
		assertEquals("1", x1.getParameter("x"));
		assertNull(x1.getParameter("q"));
		assertNull(x1.getParameter("y"));
		assertNull(x1.getParameter(null));
	}

	@Test void a04_equals() {
		var x1 = new MediaType("text/foo");
		var x2 = new MediaType("text/foo");
		var x3 = new MediaType("text/bar");
		assertEquals(x1, x2);
		assertNotEquals(x1, x3);
		assertNotEquals(x1, null);
	}

	@Test void a05_hashCode() {
		var x1 = new MediaType("text/foo");
		var x2 = new MediaType("text/foo");
		assertEquals(x2.hashCode(), x1.hashCode());
	}
}