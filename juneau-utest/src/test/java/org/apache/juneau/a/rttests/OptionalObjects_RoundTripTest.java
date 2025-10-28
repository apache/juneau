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

import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.Utils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
class OptionalObjects_RoundTripTest extends RoundTripTest_Base {

	//------------------------------------------------------------------------------------------------------------------
	// Standalone Optional objects
	//------------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	void a01_emptyOptional(RoundTrip_Tester t) throws Exception {
		var x = opte();
		x = t.roundTrip(x);
		assertFalse(x.isPresent());
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a02_optionalContainingString(RoundTrip_Tester t) throws Exception {
		var x = opt("foobar");
		x = t.roundTrip(x);
		assertEquals("foobar", x.get());
		x = opt("");
		x = t.roundTrip(x);
		assertEquals("", x.get());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Bean properties of Optional types.
	//------------------------------------------------------------------------------------------------------------------

	//-----------------------------------------------------
	// Optional<String>
	//-----------------------------------------------------

	public static class B01 {
		public Optional<String> f1;
	}

	@ParameterizedTest
	@MethodSource("testers")
	void b01a_stringField(RoundTrip_Tester t) throws Exception {
		var x = new B01();
		x.f1 = opt("foo");
		x = t.roundTrip(x);
		assertEquals("foo", x.f1.get());
	}

	@ParameterizedTest
	@MethodSource("testers")
	void b01b_stringField_emptyValue(RoundTrip_Tester t) throws Exception {
		var x = new B01();
		x.f1 = opte();
		x = t.roundTrip(x);
		assertFalse(x.f1.isPresent());
	}

	@ParameterizedTest
	@MethodSource("testers")
	void b01c_stringField_nullField(RoundTrip_Tester t) throws Exception {
		var x = new B01();
		x.f1 = null;
		x = t.roundTrip(x);
		if (t.isValidationOnly())
			return;
		assertFalse(x.f1.isPresent());
	}

	//-----------------------------------------------------
	// Optional<Integer>
	//-----------------------------------------------------

	public static class B02 {
		public Optional<Integer> f1;
	}

	@ParameterizedTest
	@MethodSource("testers")
	void b02a_integerField(RoundTrip_Tester t) throws Exception {
		var x = new B02();
		x.f1 = opt(123);
		x = t.roundTrip(x);
		assertEquals(123, x.f1.get().intValue());
	}

	@ParameterizedTest
	@MethodSource("testers")
	void b02b_integerField_emptyValue(RoundTrip_Tester t) throws Exception {
		var x = new B02();
		x.f1 = opte();
		x = t.roundTrip(x);
		assertFalse(x.f1.isPresent());
	}

	@ParameterizedTest
	@MethodSource("testers")
	void b02c_integerField_nullField(RoundTrip_Tester t) throws Exception {
		var x = new B02();
		x.f1 = null;
		x = t.roundTrip(x);
		if (t.isValidationOnly())
			return;
		assertFalse(x.f1.isPresent());
	}

	//-----------------------------------------------------
	// Optional<List<Integer>>
	//-----------------------------------------------------

	public static class B03 {
		public Optional<List<Integer>> f1;
	}

	@ParameterizedTest
	@MethodSource("testers")
	void b03a_integerListField(RoundTrip_Tester t) throws Exception {
		var x = new B03();
		x.f1 = opt(alist(123));
		x = t.roundTrip(x);
		assertEquals(123, x.f1.get().get(0).intValue());
	}

	@ParameterizedTest
	@MethodSource("testers")
	void b03b_integerListField_listWithNull(RoundTrip_Tester t) throws Exception {
		var x = new B03();
		x.f1 = opt(alist((Integer)null));
		x = t.roundTrip(x);
		assertTrue(x.f1.isPresent());
		assertEquals(1, x.f1.get().size());
		assertNull(x.f1.get().get(0));
	}

	@ParameterizedTest
	@MethodSource("testers")
	void b03c_integerListField_emptyList(RoundTrip_Tester t) throws Exception {
		var x = new B03();
		x.f1 = opt(alist());
		x = t.roundTrip(x);
		assertTrue(x.f1.isPresent());
		assertEquals(0, x.f1.get().size());
	}

	@ParameterizedTest
	@MethodSource("testers")
	void b03d_integerListField_emptyValue(RoundTrip_Tester t) throws Exception {
		var x = new B03();
		x.f1 = opte();
		x = t.roundTrip(x);
		assertFalse(x.f1.isPresent());
	}

	@ParameterizedTest
	@MethodSource("testers")
	void b03e_integerListField_nullField(RoundTrip_Tester t) throws Exception {
		var x = new B03();
		x.f1 = null;
		x = t.roundTrip(x);
		if (t.isValidationOnly())
			return;
		assertFalse(x.f1.isPresent());
	}

	//-----------------------------------------------------
	// Optional<Optional<Integer>>
	//-----------------------------------------------------

	public static class B04 {
		public Optional<Optional<Integer>> f1;
	}

	@ParameterizedTest
	@MethodSource("testers")
	void b04a_optionalOptionalInteger(RoundTrip_Tester t) throws Exception {
		var x = new B04();
		x.f1 = opt(opt(123));
		x = t.roundTrip(x);
		assertEquals(123, x.f1.get().get().intValue());
	}

	@ParameterizedTest
	@MethodSource("testers")
	void b04b_optionalOptionalInteger_emptyInnerValue(RoundTrip_Tester t) throws Exception {
		var x = new B04();
		x.f1 = opt(opte());
		x = t.roundTrip(x);
		assertTrue(x.f1.isPresent());
		assertFalse(x.f1.get().isPresent());
	}

	@ParameterizedTest
	@MethodSource("testers")
	void b04c_optionalOptionalInteger_emptyOuterValue(RoundTrip_Tester t) throws Exception {
		var x = new B04();
		x.f1 = opte();
		x = t.roundTrip(x);
		if (t.isValidationOnly())
			return;
		assertTrue(x.f1.isPresent());
		assertFalse(x.f1.get().isPresent());
	}

	@ParameterizedTest
	@MethodSource("testers")
	void b04d_optionalOptionalInteger_nullField(RoundTrip_Tester t) throws Exception {
		var x = new B04();
		x.f1 = null;
		x = t.roundTrip(x);
		if (t.isValidationOnly())
			return;
		assertTrue(x.f1.isPresent());
		assertFalse(x.f1.get().isPresent());
	}

	//-----------------------------------------------------
	// Optional<Optional<Bean>>
	//-----------------------------------------------------

	public static class B05 {
		public Optional<Optional<B05B>> f1;
	}

	public static class B05B {
		public int f2;
		public static B05B create() {
			var b = new B05B();
			b.f2 = 123;
			return b;
		}
	}

	@ParameterizedTest
	@MethodSource("testers")
	void b05a_optionalOptionalBean(RoundTrip_Tester t) throws Exception {
		var x = new B05();
		x.f1 = opt(opt(B05B.create()));
		x = t.roundTrip(x);
		assertBean(x, "f1{f2}", "{123}");
	}

	@ParameterizedTest
	@MethodSource("testers")
	void b05b_optionalOptionalBean_emptyInnerValue(RoundTrip_Tester t) throws Exception {
		var x = new B05();
		x.f1 = opt(opte());
		x = t.roundTrip(x);
		assertTrue(x.f1.isPresent());
		assertFalse(x.f1.get().isPresent());
	}

	@ParameterizedTest
	@MethodSource("testers")
	void b05c_optionalOptionalBean_emptyOuterValue(RoundTrip_Tester t) throws Exception {
		var x = new B05();
		x.f1 = opte();
		x = t.roundTrip(x);
		if (t.isValidationOnly())
			return;
		assertTrue(x.f1.isPresent());
		assertFalse(x.f1.get().isPresent());
	}

	@ParameterizedTest
	@MethodSource("testers")
	void b05d_optionalOptionalBean_nullField(RoundTrip_Tester t) throws Exception {
		var x = new B05();
		x.f1 = null;
		x = t.roundTrip(x);
		if (t.isValidationOnly())
			return;
		assertTrue(x.f1.isPresent());
		assertFalse(x.f1.get().isPresent());
	}

	//-----------------------------------------------------
	// List<Optional<Integer>>
	//-----------------------------------------------------

	public static class B06 {
		public List<Optional<Integer>> f1;
	}

	@ParameterizedTest
	@MethodSource("testers")
	void b06a_listOfOptionalIntegers(RoundTrip_Tester t) throws Exception {
		var x = new B06();
		x.f1 = alist(opt(123));
		x = t.roundTrip(x);
		assertEquals(123, x.f1.get(0).get().intValue());
	}

	@ParameterizedTest
	@MethodSource("testers")
	void b06b_listOfOptionalIntegers_listWithEmpty(RoundTrip_Tester t) throws Exception {
		var x = new B06();
		x.f1 = alist(opte());
		x = t.roundTrip(x);
		assertEquals(1, x.f1.size());
		assertFalse(x.f1.get(0).isPresent());
	}

	@ParameterizedTest
	@MethodSource("testers")
	void b06c_listOfOptionalIntegers_listWithNull(RoundTrip_Tester t) throws Exception {
		var x = new B06();
		x.f1 = alist((Optional<Integer>)null);
		x = t.roundTrip(x);
		if (t.isValidationOnly())
			return;
		assertEquals(1, x.f1.size());
		assertFalse(x.f1.get(0).isPresent());
	}

	@ParameterizedTest
	@MethodSource("testers")
	void b06d_listOfOptionalIntegers_nullField(RoundTrip_Tester t) throws Exception {
		var x = new B06();
		x.f1 = null;
		x = t.roundTrip(x);
		assertNull(x.f1);
	}

	//-----------------------------------------------------
	// Optional<Integer>[]
	//-----------------------------------------------------

	public static class B07 {
		public Optional<Integer>[] f1;
		public List<Integer>[] f2;
	}

	@ParameterizedTest
	@MethodSource("testers")
	void b07a_arrayOfOptionalIntegers(RoundTrip_Tester t) throws Exception {
		var x = new B07();
		x.f1 = a(opt(123));
		x.f2 = a(alist(123));
		x = t.roundTrip(x);
		assertEquals(123, x.f1[0].get().intValue());
	}

	@ParameterizedTest
	@MethodSource("testers")
	void b07b_arrayOfOptionalIntegers_listWithEmpty(RoundTrip_Tester t) throws Exception {
		var x = new B07();
		x.f1 = a(opte());
		x = t.roundTrip(x);
		assertEquals(1, x.f1.length);
		assertFalse(x.f1[0].isPresent());
	}

	@ParameterizedTest
	@MethodSource("testers")
	void b07c_arrayOfOptionalIntegers_listWithNull(RoundTrip_Tester t) throws Exception {
		var x = new B07();
		x.f1 = a(n(Optional.class));
		x = t.roundTrip(x);
		if (t.isValidationOnly())
			return;
		assertEquals(1, x.f1.length);
		assertFalse(x.f1[0].isPresent());
	}

	@ParameterizedTest
	@MethodSource("testers")
	void b07d_arrayOfOptionalIntegers_nullField(RoundTrip_Tester t) throws Exception {
		var x = new B07();
		x.f1 = null;
		x = t.roundTrip(x);
		assertNull(x.f1);
	}
}