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
package org.apache.juneau.http.classic.header;

import static org.junit.jupiter.api.Assertions.*;

import java.util.function.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Validates {@link Via}.
 */
class Via_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Static creators
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_of_wireString() {
		var h = Via.of("1.0 fred, 1.1 nowhere.com");
		assertEquals("Via", h.getName());
		assertEquals("1.0 fred, 1.1 nowhere.com", h.getValue());
	}

	@Test void a02_of_wireString_null_returnsNull() {
		assertNull(Via.of((String)null));
	}

	@Test void a03_of_varargs() {
		var h = Via.of(new String[]{"1.0 fred", "1.1 nowhere.com"});
		assertEquals("1.0 fred, 1.1 nowhere.com", h.getValue());
	}

	// Fixed: Via now declares its own of(String,String) overload -- see Allow_Test.a03b.
	@Test void a03b_of_twoStringLiterals_resolvesToOwnTypeSafeFactory() {
		Object h = Via.of("1.0 fred", "1.1 nowhere.com");
		assertInstanceOf(Via.class, h);
		var via = (Via)h;
		assertEquals("Via", via.getName());
		assertEquals("1.0 fred, 1.1 nowhere.com", via.getValue());
	}

	@Test void a04_of_varargs_null_returnsNull() {
		assertNull(Via.of((String[])null));
	}

	@Test void a05_of_supplier_delaysEvaluation() {
		var calls = new int[1];
		var h = Via.of((Supplier<String[]>) () -> { calls[0]++; return new String[]{"1.0 fred"}; });
		assertEquals(0, calls[0]);
		assertEquals("1.0 fred", h.getValue());
		assertEquals(1, calls[0]);
	}

	@Test void a06_of_supplier_null_returnsNull() {
		assertNull(Via.of((Supplier<String[]>)null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Constructors
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_ctor_wireString() {
		assertEquals("1.0 fred", new Via("1.0 fred").getValue());
	}

	@Test void b02_ctor_varargs() {
		assertEquals("1.0 fred, 1.1 nowhere.com", new Via("1.0 fred", "1.1 nowhere.com").getValue());
	}
}
