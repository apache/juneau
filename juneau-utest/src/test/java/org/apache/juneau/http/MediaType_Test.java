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

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class MediaType_Test {

	@Test
	public void a01_basic() {
		assertEquals(new MediaType("text/foo"), new MediaType("text/foo"));
		assertNotEquals(new MediaType("text/foo"), "text/foo");

		Set<MediaType> x = new TreeSet<>();
		x.add(MediaType.of("text/foo"));
		x.add(MediaType.of("text/bar"));
		assertObject(x).asJson().is("['text/bar','text/foo']");

		MediaType x2 = new MediaType((String)null);  // Interpreted as "/*"
		assertString(x2.getType()).isEmpty();
		assertString(x2.getSubType()).is("*");
		assertObject(x2.getSubTypes()).asJson().is("['*']");
		assertTrue(x2.isMetaSubtype());

		MediaType x3 = MediaType.of("text/foo+bar");
		assertTrue(x3.hasSubType("bar"));
		assertFalse(x3.hasSubType("baz"));
		assertFalse(x3.hasSubType(null));
	}

	@Test
	public void a02_match() {
		MediaType x1 = MediaType.of("text/json");
		assertInteger(x1.match(x1,true)).is(100000);

		assertInteger(x1.match(MediaType.of("text/json+foo"),true)).is(10210);
		assertInteger(x1.match(MediaType.of("text/json+foo"),false)).is(0);
	}

	@Test
	public void a03_getParameter() {
		MediaType x1 = MediaType.of("text/json;x=1;q=1;y=2");
		assertString(x1.getParameter("x")).is("1");
		assertString(x1.getParameter("q")).isNull();
		assertString(x1.getParameter("y")).isNull();
		assertString(x1.getParameter(null)).isNull();
	}

	@Test
	public void a04_equals() {
		MediaType x1 = new MediaType("text/foo"), x2 = new MediaType("text/foo"), x3 = new MediaType("text/bar");
		assertBoolean(x1.equals(x2)).isTrue();
		assertBoolean(x1.equals(x3)).isFalse();
		assertBoolean(x1.equals(null)).isFalse();
	}

	@Test
	public void a05_hashCode() {
		MediaType x1 = new MediaType("text/foo"), x2 = new MediaType("text/foo");
		assertInteger(x1.hashCode()).is(x2.hashCode());
	}
}
