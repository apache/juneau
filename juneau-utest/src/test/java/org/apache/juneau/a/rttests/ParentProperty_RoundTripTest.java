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

