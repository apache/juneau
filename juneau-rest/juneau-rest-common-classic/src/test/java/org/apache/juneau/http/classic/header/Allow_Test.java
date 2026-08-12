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
 * Validates {@link Allow}.
 */
class Allow_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Static creators
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_of_wireString() {
		var h = Allow.of("GET, HEAD");
		assertEquals("Allow", h.getName());
		assertEquals("GET, HEAD", h.getValue());
	}

	@Test void a02_of_wireString_null_returnsNull() {
		assertNull(Allow.of((String)null));
	}

	@Test void a03_of_varargs() {
		// NOTE: must pass an actual String[]-typed expression here, not two String literals -- see a03b below.
		var h = Allow.of(new String[]{"GET", "HEAD"});
		assertEquals("GET, HEAD", h.getValue());
	}

	// Fixed: Allow now declares its own fixed-arity of(String,String) overload, so it's selected
	// outright (same declaring class beats an inherited candidate) instead of falling through to the inherited
	// BasicCsvHeader.of(String,String) factory. `Allow.of("GET", "HEAD")` now correctly returns an Allow header
	// with both values as CSV tokens, not a mistyped BasicCsvHeader (name="GET", value="HEAD"). Same fix applied
	// to the other BasicCsvHeader subclasses with of(String...) factories (ContentLanguage, Upgrade, Via).
	@Test void a03b_of_twoStringLiterals_resolvesToOwnTypeSafeFactory() {
		Object h = Allow.of("GET", "HEAD");
		assertInstanceOf(Allow.class, h);
		var allow = (Allow)h;
		assertEquals("Allow", allow.getName());
		assertEquals("GET, HEAD", allow.getValue());
	}

	@Test void a04_of_varargs_null_returnsNull() {
		assertNull(Allow.of((String[])null));
	}

	@Test void a05_of_supplier_delaysEvaluation() {
		var calls = new int[1];
		var h = Allow.of((Supplier<String[]>) () -> { calls[0]++; return new String[]{"GET","HEAD"}; });
		assertEquals(0, calls[0]);
		assertEquals("GET, HEAD", h.getValue());
		assertEquals(1, calls[0]);
	}

	@Test void a06_of_supplier_null_returnsNull() {
		assertNull(Allow.of((Supplier<String[]>)null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Constructors
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_ctor_wireString() {
		assertEquals("GET, HEAD", new Allow("GET, HEAD").getValue());
	}

	@Test void b02_ctor_varargs() {
		assertEquals("GET, HEAD", new Allow("GET", "HEAD").getValue());
	}
}
