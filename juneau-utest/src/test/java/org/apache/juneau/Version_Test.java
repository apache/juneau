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

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import static org.apache.juneau.Version.*;

import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Version_Test {

	@Test
	public void a01_basic() {
		assertObject(of(null)).isNull();
		assertObject(of("")).asString().is("0");

		Version x = of("1.2.3");
		assertInteger(x.getMajor().orElse(null)).is(1);
		assertInteger(x.getMinor().orElse(null)).is(2);
		assertInteger(x.getMaintenance().orElse(null)).is(3);
		assertInteger(x.getPart(0).orElse(null)).is(1);
		assertInteger(x.getPart(1).orElse(null)).is(2);
		assertInteger(x.getPart(2).orElse(null)).is(3);
		assertInteger(x.getPart(-1).orElse(null)).isNull();
		assertInteger(x.getPart(3).orElse(null)).isNull();

		x = of("1..x");
		assertString(x).is("1.0.2147483647");
	}

	@Test
	public void a02_isAtLeast() {
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

	@Test
	public void a03_isAtMost() {
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

	@Test
	public void a04_isEqualsTo() {
		Version x = of("1.2.3");

		assertTrue(x.equals(of("1.2.3")));
		assertTrue(x.equals(of("1.2")));
		assertTrue(x.equals(of("1.2.3.4")));
		assertFalse(x.equals(of("1.2.4")));
	}

	@Test
	public void a05_compareTo() {
		List<Version> l = alist(
			of("1.2.3"),
			of("1.2"),
			of(""),
			of("1.2.3.4"),
			of("2.0"),
			of("2")
		);
		assertList(l).asSorted().asString().is("[0, 1.2, 1.2.3, 1.2.3.4, 2, 2.0]");
		Collections.reverse(l);
		assertList(l).asSorted().asString().is("[0, 1.2, 1.2.3, 1.2.3.4, 2, 2.0]");
	}
}
