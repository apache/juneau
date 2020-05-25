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

import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.junit.*;

/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
@FixMethodOrder(NAME_ASCENDING)
public class RoundTripOptionalObjectsTest extends RoundTripTest {

	public RoundTripOptionalObjectsTest(String label, SerializerBuilder s, ParserBuilder p, int flags) throws Exception {
		super(label, s, p, flags);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Standalone Optional objects
	//------------------------------------------------------------------------------------------------------------------

	// Empty Optional
	@Test
	public void a01_emptyOptional() throws Exception {
		Optional<String> o = Optional.empty();
		o = roundTrip(o);
		assertFalse(o.isPresent());
	}

	// Optional containing String.
	@Test
	public void a02_optionalContainingString() throws Exception {
		Optional<String> o = Optional.of("foobar");
		o = roundTrip(o);
		assertEquals("foobar", o.get());
		o = Optional.of("");
		o = roundTrip(o);
		assertEquals("", o.get());
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

	@Test
	public void b01a_stringField() throws Exception {
		B01 x = new B01();
		x.f1 = Optional.of("foo");
		x = roundTrip(x);
		assertEquals("foo", x.f1.get());
	}

	@Test
	public void b01b_stringField_emptyValue() throws Exception {
		B01 x = new B01();
		x.f1 = Optional.empty();
		x = roundTrip(x);
		assertFalse(x.f1.isPresent());
	}

	@Test
	public void b01c_stringField_nullField() throws Exception {
		B01 x = new B01();
		x.f1 = null;
		x = roundTrip(x);
		if (isValidationOnly())
			return;
		assertFalse(x.f1.isPresent());
	}

	//-----------------------------------------------------
	// Optional<Integer>
	//-----------------------------------------------------

	public static class B02 {
		public Optional<Integer> f1;
	}

	@Test
	public void b02a_integerField() throws Exception {
		B02 x = new B02();
		x.f1 = Optional.of(123);
		x = roundTrip(x);
		assertEquals(123, x.f1.get().intValue());
	}

	@Test
	public void b02b_integerField_emptyValue() throws Exception {
		B02 x = new B02();
		x.f1 = Optional.empty();
		x = roundTrip(x);
		assertFalse(x.f1.isPresent());
	}

	@Test
	public void b02c_integerField_nullField() throws Exception {
		B02 x = new B02();
		x.f1 = null;
		x = roundTrip(x);
		if (isValidationOnly())
			return;
		assertFalse(x.f1.isPresent());
	}

	//-----------------------------------------------------
	// Optional<List<Integer>>
	//-----------------------------------------------------

	public static class B03 {
		public Optional<List<Integer>> f1;
	}

	@Test
	public void b03a_integerListField() throws Exception {
		B03 x = new B03();
		x.f1 = Optional.of(AList.of(123));
		x = roundTrip(x);
		assertEquals(123, x.f1.get().get(0).intValue());
	}

	@Test
	public void b03b_integerListField_listWithNull() throws Exception {
		B03 x = new B03();
		x.f1 = Optional.of(AList.of((Integer)null));
		x = roundTrip(x);
		assertTrue(x.f1.isPresent());
		assertEquals(1, x.f1.get().size());
		assertNull(x.f1.get().get(0));
	}

	@Test
	public void b03c_integerListField_emptyList() throws Exception {
		B03 x = new B03();
		x.f1 = Optional.of(AList.of());
		x = roundTrip(x);
		assertTrue(x.f1.isPresent());
		assertEquals(0, x.f1.get().size());
	}

	@Test
	public void b03d_integerListField_emptyValue() throws Exception {
		B03 x = new B03();
		x.f1 = Optional.empty();
		x = roundTrip(x);
		assertFalse(x.f1.isPresent());
	}

	@Test
	public void b03e_integerListField_nullField() throws Exception {
		B03 x = new B03();
		x.f1 = null;
		x = roundTrip(x);
		if (isValidationOnly())
			return;
		assertFalse(x.f1.isPresent());
	}

	//-----------------------------------------------------
	// Optional<Optional<Integer>>
	//-----------------------------------------------------

	public static class B04 {
		public Optional<Optional<Integer>> f1;
	}

	@Test
	public void b04a_optionalOptionalInteger() throws Exception {
		B04 x = new B04();
		x.f1 = Optional.of(Optional.of(123));
		x = roundTrip(x);
		assertEquals(123, x.f1.get().get().intValue());
	}

	@Test
	public void b04b_optionalOptionalInteger_emptyInnerValue() throws Exception {
		B04 x = new B04();
		x.f1 = Optional.of(Optional.empty());
		x = roundTrip(x);
		assertTrue(x.f1.isPresent());
		assertFalse(x.f1.get().isPresent());
	}

	@Test
	public void b04c_optionalOptionalInteger_emptyOuterValue() throws Exception {
		B04 x = new B04();
		x.f1 = Optional.empty();
		x = roundTrip(x);
		if (isValidationOnly())
			return;
		assertTrue(x.f1.isPresent());
		assertFalse(x.f1.get().isPresent());
	}

	@Test
	public void b04d_optionalOptionalInteger_nullField() throws Exception {
		B04 x = new B04();
		x.f1 = null;
		x = roundTrip(x);
		if (isValidationOnly())
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
			B05B b = new B05B();
			b.f2 = 123;
			return b;
		}
	}

	@Test
	public void b05a_optionalOptionalBean() throws Exception {
		B05 x = new B05();
		x.f1 = Optional.of(Optional.of(B05B.create()));
		x = roundTrip(x);
		assertEquals(123, x.f1.get().get().f2);
	}

	@Test
	public void b05b_optionalOptionalBean_emptyInnerValue() throws Exception {
		B05 x = new B05();
		x.f1 = Optional.of(Optional.empty());
		x = roundTrip(x);
		assertTrue(x.f1.isPresent());
		assertFalse(x.f1.get().isPresent());
	}

	@Test
	public void b05c_optionalOptionalBean_emptyOuterValue() throws Exception {
		B05 x = new B05();
		x.f1 = Optional.empty();
		x = roundTrip(x);
		if (isValidationOnly())
			return;
		assertTrue(x.f1.isPresent());
		assertFalse(x.f1.get().isPresent());
	}

	@Test
	public void b05d_optionalOptionalBean_nullField() throws Exception {
		B05 x = new B05();
		x.f1 = null;
		x = roundTrip(x);
		if (isValidationOnly())
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

	@Test
	public void b06a_listOfOptionalIntegers() throws Exception {
		B06 x = new B06();
		x.f1 = AList.of(Optional.of(123));
		x = roundTrip(x);
		assertEquals(123, x.f1.get(0).get().intValue());
	}

	@Test
	public void b06b_listOfOptionalIntegers_listWithEmpty() throws Exception {
		B06 x = new B06();
		x.f1 = AList.of(Optional.empty());
		x = roundTrip(x);
		assertEquals(1, x.f1.size());
		assertFalse(x.f1.get(0).isPresent());
	}

	@Test
	public void b06c_listOfOptionalIntegers_listWithNull() throws Exception {
		B06 x = new B06();
		x.f1 = AList.of((Optional<Integer>)null);
		x = roundTrip(x);
		if (isValidationOnly())
			return;
		assertEquals(1, x.f1.size());
		assertFalse(x.f1.get(0).isPresent());
	}

	@Test
	public void b06d_listOfOptionalIntegers_nullField() throws Exception {
		B06 x = new B06();
		x.f1 = null;
		x = roundTrip(x);
		assertNull(x.f1);
	}

	//-----------------------------------------------------
	// Optional<Integer>[]
	//-----------------------------------------------------

	public static class B07 {
		public Optional<Integer>[] f1;
		public List<Integer>[] f2;
	}

	@Test
	@SuppressWarnings("unchecked")
	public void b07a_arrayOfOptionalIntegers() throws Exception {
		B07 x = new B07();
		x.f1 = new Optional[]{Optional.of(123)};
		x.f2 = new List[]{AList.of(123)};
		x = roundTrip(x);
		assertEquals(123, x.f1[0].get().intValue());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void b07b_arrayOfOptionalIntegers_listWithEmpty() throws Exception {
		B07 x = new B07();
		x.f1 = new Optional[]{Optional.empty()};
		x = roundTrip(x);
		assertEquals(1, x.f1.length);
		assertFalse(x.f1[0].isPresent());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void b07c_arrayOfOptionalIntegers_listWithNull() throws Exception {
		B07 x = new B07();
		x.f1 = new Optional[]{null};
		x = roundTrip(x);
		if (isValidationOnly())
			return;
		assertEquals(1, x.f1.length);
		assertFalse(x.f1[0].isPresent());
	}

	@Test
	public void b07d_arrayOfOptionalIntegers_nullField() throws Exception {
		B07 x = new B07();
		x.f1 = null;
		x = roundTrip(x);
		assertNull(x.f1);
	}
}
