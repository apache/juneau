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
		assertObject(x).json().is("['text/bar','text/foo']");

		MediaType x2 = new MediaType((String)null);  // Interpreted as "/*"
		assertString(x2.getType()).isEmpty();
		assertString(x2.getSubType()).is("*");
		assertObject(x2.getSubTypes()).json().is("['*']");
		assertTrue(x2.isMeta());

		MediaType x3 = MediaType.of("text/foo+bar");
		assertTrue(x3.hasSubType("bar"));
		assertFalse(x3.hasSubType("baz"));
		assertFalse(x3.hasSubType(null));
	}
}
