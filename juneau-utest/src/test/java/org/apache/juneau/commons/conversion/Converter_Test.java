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

class Converter_Test extends TestBase {

	//====================================================================================================
	// a - default canConvert
	//====================================================================================================

	private static final Converter STUB = new Converter() {
		@Override public <T> T to(Object o, Class<T> type) { return type.cast(o); }
		@Override public <T> T to(Object o, Type mainType, Type... args) { return null; }
	};

	@Test void a01_defaultCanConvertAlwaysTrue() {
		assertTrue(STUB.canConvert(String.class, String.class));
		assertTrue(STUB.canConvert(String.class, Integer.class));
	}

	//====================================================================================================
	// b - default memberOf methods
	//====================================================================================================

	// Minimal converter that uses BasicConverter internally but does NOT override the memberOf overloads,
	// so the default methods on the Converter interface are exercised directly.
	private static final Converter B_STUB = new Converter() {
		@Override public <T> T to(Object o, Class<T> type) { return BasicConverter.INSTANCE.to(o, type); }
		@Override public <T> T to(Object o, Type mainType, Type... args) { return BasicConverter.INSTANCE.to(o, mainType, args); }
	};

	@Test void b01_defaultToSessionClass() {
		// default to(o, memberOf, session, Class) ignores memberOf and session and delegates to to(o, Class)
		assertEquals(Integer.valueOf(42), B_STUB.to("42", new Object(), (ConverterSession)null, Integer.class));
	}

	@Test void b02_defaultToSessionType() {
		// default to(o, memberOf, session, Type, Type...) ignores memberOf and session and delegates to to(o, Type, Type...)
		var result = B_STUB.to(List.of("1", "2"), new Object(), (ConverterSession)null, (Type) List.class, new Type[]{Integer.class});
		assertEquals(List.of(1, 2), result);
	}
}
