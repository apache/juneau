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
package org.apache.juneau.commons.conversion;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class CachingConverter_Test extends TestBase {

	private static final BasicConverter C = BasicConverter.INSTANCE;

	//====================================================================================================
	// a - canConvert
	//====================================================================================================

	@Test void a01_canConvertSameType() {
		assertTrue(C.canConvert(String.class, String.class));
	}

	@Test void a02_canConvertKnownConversion() {
		assertTrue(C.canConvert(String.class, Integer.class));
	}

	@Test void a03_canConvertNoConversionAvailable() {
		assertFalse(C.canConvert(StringBuilder.class, java.net.URI.class));
	}

	//====================================================================================================
	// b - to(Object, Type, Type...)
	//====================================================================================================

	@Test void b01_toTypeNullInput() {
		assertNull(C.to(null, (Type) List.class));
	}

	// Helper to get a ParameterizedType for List<String> via reflection.
	@SuppressWarnings("unused")
	private List<String> listOfStringField;
	private static final Type LIST_OF_STRING;
	static {
		try {
			LIST_OF_STRING = CachingConverter_Test.class.getDeclaredField("listOfStringField").getGenericType();
		} catch (NoSuchFieldException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	@Test void b02_toParameterizedType() {
		var result = C.to(List.of("a", "b"), LIST_OF_STRING);
		assertNotNull(result);
		assertInstanceOf(List.class, result);
	}

	@Test void b03_toTypeWithPlainClass() {
		var result = C.to(42, (Type) String.class);
		assertEquals("42", result);
	}

	@Test void b04_toTypeNoConversionAvailable() {
		assertThrows(InvalidConversionException.class, () -> C.to(new StringBuilder("x"), (Type) java.net.URI.class));
	}

	@Test void b05_toTypeWithPlainClassArg() {
		// args contains a plain Class (false branch of line 157 ternary)
		var result = C.to(List.of("1", "2"), (Type) List.class, (Type) Integer.class);
		assertNotNull(result);
		assertInstanceOf(List.class, result);
		assertEquals(List.of(1, 2), result);
	}

	// Helper field used to obtain a ParameterizedType for use as an arg.
	@SuppressWarnings("unused")
	private List<String> listArgField;
	private static final Type LIST_ARG_TYPE;
	static {
		try {
			LIST_ARG_TYPE = CachingConverter_Test.class.getDeclaredField("listArgField").getGenericType();
		} catch (NoSuchFieldException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	@Test void b06_toTypeWithParameterizedTypeArg() {
		// args contains a ParameterizedType (true branch of line 157 ternary); raw type List is extracted
		var result = C.to(List.of(List.of("a", "b")), (Type) List.class, LIST_ARG_TYPE);
		assertNotNull(result);
		assertInstanceOf(List.class, result);
	}

	//====================================================================================================
	// c - to(Object, Object, ConverterSession, Class)
	//====================================================================================================

	@Test void c01_toMemberOfNullInput() {
		assertNull(C.to(null, "ignored", (ConverterSession)null, String.class));
	}

	@Test void c02_toMemberOfSameType() {
		var s = "hello";
		assertSame(s, C.to(s, null, (ConverterSession)null, String.class));
	}

	@Test void c03_toMemberOfConversion() {
		assertEquals("42", C.to(42, null, (ConverterSession)null, String.class));
	}

	@Test void c04_toMemberOfNoConversion() {
		assertThrows(InvalidConversionException.class, () -> C.to(new StringBuilder("x"), null, (ConverterSession)null, java.net.URI.class));
	}

	//====================================================================================================
	// d - to(Object, Object, ConverterSession, Type, Type...)
	//====================================================================================================

	// Sentinel used as the memberOf argument to avoid null-vs-Type overload ambiguity.
	private static final Object D_MEMBER_OF = new Object();

	@Test void d01_toMemberOfTypeNullInput() {
		assertNull(C.to(null, D_MEMBER_OF, (ConverterSession)null, (Type) String.class));
	}

	@Test void d02_toMemberOfTypePlainClass() {
		// mainType is a plain Class (false branch of ParameterizedType ternary)
		assertEquals("42", C.to(42, D_MEMBER_OF, (ConverterSession)null, (Type) String.class));
	}

	@SuppressWarnings("unused")
	private List<String> d03_listField;
	private static final Type D03_LIST_TYPE;
	static {
		try {
			D03_LIST_TYPE = CachingConverter_Test.class.getDeclaredField("d03_listField").getGenericType();
		} catch (NoSuchFieldException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	@Test void d03_toMemberOfTypeWithParameterizedType() {
		// mainType is a ParameterizedType (true branch of ternary); raw type List is extracted
		var result = C.to(List.of("a", "b"), D_MEMBER_OF, (ConverterSession)null, D03_LIST_TYPE);
		assertNotNull(result);
		assertInstanceOf(List.class, result);
	}

	@Test void d04_toMemberOfTypeWithPlainClassArg() {
		// args contains a plain Class (false branch of arg ternary)
		var result = C.to(List.of("1", "2"), D_MEMBER_OF, (ConverterSession)null, (Type) List.class, (Type) Integer.class);
		assertEquals(List.of(1, 2), result);
	}

	@Test void d05_toMemberOfTypeWithParameterizedTypeArg() {
		// args contains a ParameterizedType (true branch of arg ternary); raw type List is extracted
		var result = C.to(List.of(List.of("a", "b")), D_MEMBER_OF, (ConverterSession)null, (Type) List.class, LIST_ARG_TYPE);
		assertNotNull(result);
		assertInstanceOf(List.class, result);
	}

	@Test void d06_toMemberOfTypeNoConversion() {
		assertThrows(InvalidConversionException.class, () -> C.to(new StringBuilder("x"), D_MEMBER_OF, (ConverterSession)null, (Type) java.net.URI.class));
	}
}
