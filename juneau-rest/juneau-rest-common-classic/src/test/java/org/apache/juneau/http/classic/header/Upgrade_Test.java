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
 * Validates {@link Upgrade}.
 */
class Upgrade_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Static creators
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_of_wireString() {
		var h = Upgrade.of("HTTP/2.0, SHTTP/1.3");
		assertEquals("Upgrade", h.getName());
		assertEquals("HTTP/2.0, SHTTP/1.3", h.getValue());
	}

	@Test void a02_of_wireString_null_returnsNull() {
		assertNull(Upgrade.of((String)null));
	}

	@Test void a03_of_varargs() {
		var h = Upgrade.of(new String[]{"HTTP/2.0", "SHTTP/1.3"});
		assertEquals("HTTP/2.0, SHTTP/1.3", h.getValue());
	}

	// Fixed: Upgrade now declares its own of(String,String) overload -- see Allow_Test.a03b.
	@Test void a03b_of_twoStringLiterals_resolvesToOwnTypeSafeFactory() {
		Object h = Upgrade.of("HTTP/2.0", "SHTTP/1.3");
		assertInstanceOf(Upgrade.class, h);
		var upgrade = (Upgrade)h;
		assertEquals("Upgrade", upgrade.getName());
		assertEquals("HTTP/2.0, SHTTP/1.3", upgrade.getValue());
	}

	@Test void a04_of_varargs_null_returnsNull() {
		assertNull(Upgrade.of((String[])null));
	}

	@Test void a05_of_supplier_delaysEvaluation() {
		var calls = new int[1];
		var h = Upgrade.of((Supplier<String[]>) () -> { calls[0]++; return new String[]{"HTTP/2.0"}; });
		assertEquals(0, calls[0]);
		assertEquals("HTTP/2.0", h.getValue());
		assertEquals(1, calls[0]);
	}

	@Test void a06_of_supplier_null_returnsNull() {
		assertNull(Upgrade.of((Supplier<String[]>)null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Constructors
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_ctor_wireString() {
		assertEquals("HTTP/2.0", new Upgrade("HTTP/2.0").getValue());
	}

	@Test void b02_ctor_varargs() {
		assertEquals("HTTP/2.0, SHTTP/1.3", new Upgrade("HTTP/2.0", "SHTTP/1.3").getValue());
	}
}
