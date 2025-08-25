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

import static org.apache.juneau.StringRanges.*;
import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class StringRanges_Test extends SimpleTestBase {

	@Test void a01_match() {
		List<String> x = alist("foo","bar","baz");

		assertEquals(-1, of((String)null).match(x));

		assertEquals(1, of("foo;q=0.5,bar").match(x));
		assertEquals(1, of("foo;q=0.5,bar").match(x));
		assertEquals(0, of("foo,*").match(x));
		assertEquals(1, of("*,bar").match(x));
		assertEquals(0, of("foo;q=0.6,bar;q=0.5").match(x));
		assertEquals(0, of("foo;q=0.6,bar;q=0.5,qux").match(x));
		assertEquals(-1, of("qux").match(x));
		assertEquals(-1, of("qux,q2x;q=0").match(x));
		assertEquals(-1, of("foo;q=0").match(x));
	}

	@Test void a02_getRange() {
		assertNotNull(of("foo").getRange(0));
		assertNull(of((String)null).getRange(0));
		assertNull(of("").getRange(0));
		assertNull(of((String)null).getRange(-1));
		assertNull(of((String)null).getRange(1));
	}

	@Test void a03_getRanges() {
		assertJson(of("foo").toList(), "['foo']");
		assertJson(of((String)null).toList(), "[]");
	}

	@Test void a04_toString() {
		assertString("*", new StringRange("*"));
		assertString("qux, bar;q=0.9, foo;q=0.6", of("foo;q=0.6,bar;q=0.9,qux"));
	}
}