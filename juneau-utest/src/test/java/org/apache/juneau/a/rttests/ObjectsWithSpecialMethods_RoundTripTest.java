/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.a.rttests;

import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
class ObjectsWithSpecialMethods_RoundTripTest extends RoundTripTest_Base {

	//====================================================================================================
	// @NameProperty method.
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a01_nameProperty(RoundTrip_Tester t) throws Exception {
		var x = new A().init();
		x = t.roundTrip(x);
		assertBean(x, "a2{f2},m{k1{f2}}", "{2},{{2}}");
		if (t.isValidationOnly())
			return;
		assertBean(x, "a2{name}", "{a2}");
		assertBean(x, "m{k1{name}}", "{{k1}}");
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

	@ParameterizedTest
	@MethodSource("testers")
	void a02_parentProperty(RoundTrip_Tester t) throws Exception {
		var x = new B().init();
		x = t.roundTrip(x);
		if (t.isValidationOnly())
			return;
		assertEquals(x.f1, x.b2.parent.f1);
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
		protected void setParent(B v) {
			parent = v;
		}

		B2 init() {
			f2 = 2;
			return this;
		}
	}
}