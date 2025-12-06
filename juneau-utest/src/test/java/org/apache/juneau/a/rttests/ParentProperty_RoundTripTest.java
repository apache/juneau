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

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.annotation.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
class ParentProperty_RoundTripTest extends RoundTripTest_Base {

	//====================================================================================================
	// @ParentProperty method.
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a01_parentProperty(RoundTrip_Tester t) throws Exception {
		var x = new ParentPropertyMethodContainer().init();
		x = t.roundTrip(x);
		if (t.isValidationOnly())
			return;
		assertEquals(x.f1, x.bean.parent.f1);
	}

	public static class ParentPropertyMethodContainer {
		public int f1;
		public ParentPropertyMethodBean bean;

		ParentPropertyMethodContainer init() {
			f1 = 1;
			bean = new ParentPropertyMethodBean().init();
			return this;
		}

	}
	public static class ParentPropertyMethodBean {
		ParentPropertyMethodContainer parent;
		public int f2;

		@ParentProperty
		protected void setParent(ParentPropertyMethodContainer v) {
			parent = v;
		}

		ParentPropertyMethodBean init() {
			f2 = 2;
			return this;
		}
	}

	//====================================================================================================
	// @ParentProperty field.
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a02_parentPropertyField(RoundTrip_Tester t) throws Exception {
		var x = new ParentPropertyFieldContainer().init();
		x = t.roundTrip(x);
		if (t.isValidationOnly())
			return;
		assertEquals(x.f1, x.bean.parent.f1);
	}

	public static class ParentPropertyFieldContainer {
		public int f1;
		public ParentPropertyFieldBean bean;

		ParentPropertyFieldContainer init() {
			f1 = 1;
			bean = new ParentPropertyFieldBean().init();
			return this;
		}
	}
	public static class ParentPropertyFieldBean {
		@ParentProperty
		public ParentPropertyFieldContainer parent;
		public int f2;

		ParentPropertyFieldBean init() {
			f2 = 2;
			return this;
		}
	}

	//====================================================================================================
	// @ParentProperty read-only (getter only, no setter).
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a03_readOnlyParentProperty(RoundTrip_Tester t) throws Exception {
		var x = new ReadOnlyParentPropertyContainer().init();
		// Initially, parent should be null (read-only property, can't be set)
		assertNull(x.bean.getParent(), "Read-only parent property should initially be null");
		x = t.roundTrip(x);
		if (t.isValidationOnly())
			return;
		// After round-trip, parent should still be null because parser won't set read-only properties
		assertNull(x.bean.getParent(), "Read-only parent property should not be set by parser");
		// Verify the object structure is still correct
		assertEquals(1, x.f1);
		assertEquals(2, x.bean.f2);
	}

	public static class ReadOnlyParentPropertyContainer {
		public int f1;
		public ReadOnlyParentPropertyBean bean;

		ReadOnlyParentPropertyContainer init() {
			f1 = 1;
			bean = new ReadOnlyParentPropertyBean().init();
			return this;
		}
	}
	public static class ReadOnlyParentPropertyBean {
		private ReadOnlyParentPropertyContainer parent;
		public int f2;

		@ParentProperty
		public ReadOnlyParentPropertyContainer getParent() {
			return parent;
		}

		ReadOnlyParentPropertyBean init() {
			f2 = 2;
			return this;
		}
	}
}

