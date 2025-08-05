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
package org.apache.juneau;

import static org.junit.Assert.*;
import org.junit.jupiter.api.*;

class PropertyNamerTest extends SimpleTestBase {

	//====================================================================================================
	// test dashed-lower-case
	//====================================================================================================
	@Test
	public void testDLC() {
		PropertyNamer n = new PropertyNamerDLC();

		assertEquals("abc", n.getPropertyName("ABC"));
		assertEquals("abc", n.getPropertyName("abc"));
		assertEquals("foo-bar-baz", n.getPropertyName("FooBarBaz"));
		assertEquals("foo-bar-baz", n.getPropertyName("FooBarBAZ"));
		assertEquals("foo-bar-baz", n.getPropertyName("fooBarBAZ"));
		assertEquals("", n.getPropertyName(""));
		assertNull(n.getPropertyName(null));
		assertEquals("a", n.getPropertyName("A"));
		assertEquals("a", n.getPropertyName("A"));
	}

	//====================================================================================================
	// test underscore-lower-case
	//====================================================================================================
	@Test
	public void testULC() {
		PropertyNamer n = new PropertyNamerULC();

		assertEquals("abc", n.getPropertyName("ABC"));
		assertEquals("abc", n.getPropertyName("abc"));
		assertEquals("foo_bar_baz", n.getPropertyName("FooBarBaz"));
		assertEquals("foo_bar_baz", n.getPropertyName("FooBarBAZ"));
		assertEquals("foo_bar_baz", n.getPropertyName("fooBarBAZ"));
		assertEquals("", n.getPropertyName(""));
		assertNull(n.getPropertyName(null));
		assertEquals("a", n.getPropertyName("A"));
		assertEquals("a", n.getPropertyName("A"));
	}
}