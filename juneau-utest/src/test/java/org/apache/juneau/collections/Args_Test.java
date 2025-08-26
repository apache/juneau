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
package org.apache.juneau.collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.apache.juneau.TestUtils.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class Args_Test extends SimpleTestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// test - Basic tests
	//-----------------------------------------------------------------------------------------------------------------
	@Test void basic() {
		var a = new Args(a());

		// Empty args
		assertNull(a.getArg(0));
		assertNull(a.getArg(1));
		assertNull(a.getArg(-1));
		assertNull(a.getArg("foo"));
		assertEquals(0, a.getArgs("foo").size());
		assertFalse(a.containsKey("foo"));

		a = new Args(new String[]{"foo"});
		assertEquals("foo", a.getArg(0));
		assertNull(a.getArg(1));
		assertNull(a.getArg(-1));
		assertNull(a.getArg("foo"));
		assertEquals(0, a.getArgs("foo").size());
		assertFalse(a.containsKey("foo"));

		a = new Args(new String[]{"foo", "bar bar"});
		assertEquals("foo", a.getArg(0));
		assertEquals("bar bar", a.getArg(1));
		assertNull(a.getArg(-1));
		assertNull(a.getArg("foo"));
		assertEquals(0, a.getArgs("foo").size());
		assertFalse(a.containsKey("foo"));

		a = new Args(new String[]{"foo", "bar bar", "-foo"});
		assertEquals("foo", a.getArg(0));
		assertEquals("bar bar", a.getArg(1));
		assertNull(a.getArg(-1));
		assertNull(a.getArg("foo"));
		assertEquals(0, a.getArgs("foo").size());
		assertTrue(a.containsKey("foo"));

		a = new Args(new String[]{"foo", "bar bar", "-foo", "bar bar"});
		assertEquals("foo", a.getArg(0));
		assertEquals("bar bar", a.getArg(1));
		assertNull(a.getArg(-1));
		assertEquals("bar bar", a.getArg("foo"));
		assertEquals(1, a.getArgs("foo").size());
		assertEquals("bar bar", a.getArgs("foo").get(0));
		assertTrue(a.containsKey("foo"));
	}
}
