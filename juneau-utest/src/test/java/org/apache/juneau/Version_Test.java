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

import static org.apache.juneau.Version.*;
import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

import org.junit.jupiter.api.*;

class Version_Test extends SimpleTestBase {

	@Test void a01_basic() {
		assertNull(of(null));
		assertObject(of("")).asString().is("0");

		Version x = of("1.2.3");
		assertEquals(1, x.getMajor().orElse(null));
		assertEquals(2, x.getMinor().orElse(null));
		assertEquals(3, x.getMaintenance().orElse(null));
		assertEquals(1, x.getPart(0).orElse(null));
		assertEquals(2, x.getPart(1).orElse(null));
		assertEquals(3, x.getPart(2).orElse(null));
		assertInteger(x.getPart(-1).orElse(null)).isNull();
		assertInteger(x.getPart(3).orElse(null)).isNull();

		x = of("1..x");
		assertString("1.0.2147483647", x);
	}

	@Test void a02_isAtLeast() {
		Version x = of("1.2.3");

		assertTrue(x.isAtLeast(of("1.2.2")));
		assertTrue(x.isAtLeast(of("1.2.3")));
		assertFalse(x.isAtLeast(of("1.2.4")));
		assertTrue(x.isAtLeast(of("1.2.2"), true));
		assertFalse(x.isAtLeast(of("1.2.3"), true));
		assertFalse(x.isAtLeast(of("1.2.4"), true));
		assertTrue(x.isAtLeast(of("1.2")));
		assertFalse(x.isAtLeast(of("1.3")));
		assertTrue(x.isAtLeast(of("1.1.3.1")));
		assertFalse(x.isAtLeast(of("1.2.3.1")));
		assertTrue(x.isAtLeast(of("1.2.3.0")));
		assertFalse(x.isAtLeast(of("1.3.0.1")));
	}

	@Test void a03_isAtMost() {
		Version x = of("1.2.3");

		assertFalse(x.isAtMost(of("1.2.2")));
		assertTrue(x.isAtMost(of("1.2.3")));
		assertTrue(x.isAtMost(of("1.2.4")));
		assertFalse(x.isAtMost(of("1.2.2"), true));
		assertFalse(x.isAtMost(of("1.2.3"), true));
		assertTrue(x.isAtMost(of("1.2.4"), true));
		assertTrue(x.isAtMost(of("1.2")));
		assertTrue(x.isAtMost(of("1.3")));
		assertFalse(x.isAtMost(of("1.1.3.1")));
		assertTrue(x.isAtMost(of("1.2.3.1")));
		assertTrue(x.isAtMost(of("1.2.3.0")));
		assertTrue(x.isAtMost(of("1.3.0.1")));
	}

	@Test void a04_isEqualsTo() {
		Version x = of("1.2.3");

		assertTrue(x.equals(of("1.2.3")));
		assertTrue(x.equals(of("1.2")));
		assertTrue(x.equals(of("1.2.3.4")));
		assertFalse(x.equals(of("1.2.4")));
	}

	@Test void a05_compareTo() {
		List<Version> l = alist(
			of("1.2.3"),
			of("1.2"),
			of(""),
			of("1.2.3.4"),
			of("2.0"),
			of("2")
		);
		Collections.sort(l);
		assertList(l, "0,1.2,1.2.3,1.2.3.4,2,2.0");
		Collections.reverse(l);
		assertList(l, "2.0,2,1.2.3.4,1.2.3,1.2,0");
	}
}