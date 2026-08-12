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
package org.apache.juneau.http.header;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Validates {@link Allow}.
 */
class Allow_Test extends TestBase {

	@Test void a01_of_wireString() {
		var h = Allow.of("GET, HEAD");
		assertEquals("Allow", h.getName());
		assertEquals("GET, HEAD", h.getValue());
	}

	@Test void a02_of_varargs() {
		var h = Allow.of(new String[]{"GET", "HEAD"});
		assertEquals("GET, HEAD", h.getValue());
	}

	// Fixed: Allow declares its own of(String,String) overload so two literal String arguments
	// resolve to this class's own factory instead of the inherited fixed-arity HttpCsvHeader.of(String,String)
	// (which Java overload resolution would otherwise prefer over Allow's own varargs of(String...)).
	@Test void a03_of_twoStringLiterals_resolvesToOwnTypeSafeFactory() {
		Object h = Allow.of("GET", "HEAD");
		assertInstanceOf(Allow.class, h);
		var allow = (Allow)h;
		assertEquals("Allow", allow.getName());
		assertEquals("GET, HEAD", allow.getValue());
	}
}
