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

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class StringRange_Test extends SimpleTestBase {

	@Test void a01_basic() {
		StringRange x1 = of("foo;a=1;q=0.5;b=2");
		assertString("0.5", x1.getQValue());
		assertJson(x1.getExtensions(), "[{name:'a',value:'1'},{name:'b',value:'2'}]");

		StringRange x2 = of("foo;q=1.0");
		assertString("1.0", x2.getQValue());
		assertJson(x2.getExtensions(), "[]");

		StringRange x3 = of("foo;a=1");
		assertString("1.0", x3.getQValue());
		assertJson(x3.getExtensions(), "[{name:'a',value:'1'}]");

		StringRange x4 = of("foo;a=1");
		assertEquals(x4, x3);
		assertNotEquals(x3, x2);
		assertNotEquals("foo", s(x3));
		assertEquals(x4.hashCode(), x3.hashCode());

		assertString("*", of((String)null));

		assertString("0", of("foo;q=0").match("foo"));
	}

	@Test void a02_match() {
		assertEquals(100, of("foo").match("foo"));
		assertEquals(0, of("foo").match("bar"));
		assertEquals(0, of("foo").match(null));
		assertEquals(50, of("*").match("foo"));
		assertEquals(50, of(null).match("foo"));
	}

	@Test void a03_getName() {
		assertEquals("foo", of("foo;a=1;q=0.5;b=2").getName());
		assertEquals("*", of(null).getName());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Utility methods
	//------------------------------------------------------------------------------------------------------------------

	private StringRange of(String val) {
		return new StringRange(val);
	}
}