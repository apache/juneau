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
package org.apache.juneau.a.rttests;

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.junit.*;

/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
@FixMethodOrder(NAME_ASCENDING)
public class RoundTripObjectsWithSpecialMethodsTest extends RoundTripTest {

	public RoundTripObjectsWithSpecialMethodsTest(String label, SerializerBuilder s, ParserBuilder p, int flags) throws Exception {
		super(label, s, p, flags);
	}

	//====================================================================================================
	// @NameProperty method.
	//====================================================================================================
	@Test
	public void testNameProperty() throws Exception {
		A t = new A().init();
		t = roundTrip(t);
		assertObject(t).json().is("{a2:{f2:2},m:{k1:{f2:2}}}");
		if (isValidationOnly())
			return;
		assertEquals("a2", t.a2.name);
		assertEquals("k1", t.m.get("k1").name);
	}

	public static class A {
		public A2 a2;
		public Map<String,A2> m;

		A init() {
			a2 = new A2().init();
			m = new LinkedHashMap<>();
			m.put("k1", new A2().init());
			return this;
		}

	}
	public static class A2 {
		String name;
		public int f2;

		@NameProperty
		protected void setName(String name) {
			this.name = name;
		}

		A2 init() {
			f2 = 2;
			return this;
		}
	}

	//====================================================================================================
	// @ParentProperty method.
	//====================================================================================================
	@Test
	public void testParentProperty() throws Exception {
		B t = new B().init();
		t = roundTrip(t);
		if (isValidationOnly())
			return;
		assertEquals(t.f1, t.b2.parent.f1);
	}

	public static class B {
		public int f1;
		public B2 b2;

		B init() {
			f1 = 1;
			b2 = new B2().init();
			return this;
		}

	}
	public static class B2 {
		B parent;
		public int f2;

		@ParentProperty
		protected void setParent(B parent) {
			this.parent = parent;
		}

		B2 init() {
			f2 = 2;
			return this;
		}
	}
}
