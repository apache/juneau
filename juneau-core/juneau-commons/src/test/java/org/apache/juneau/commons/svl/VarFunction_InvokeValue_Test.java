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
package org.apache.juneau.commons.svl;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

/** Tests for {@link VarFunction#invokeValue(VarResolverSession, List)}. */
class VarFunction_InvokeValue_Test extends TestBase {

	/** Direct implementation that only supplies the string-based invoke. */
	private abstract static class Base implements VarFunction {
		@Override public String name() { return "t"; }
		@Override public int minArity() { return 0; }
		@Override public int maxArity() { return 0; }
	}

	@Test void a01_defaultDelegatesToInvoke() throws Exception {
		VarFunction f = new Base() {
			@Override public String invoke(VarResolverSession s, List<Object> a) { return "x"; }
		};
		assertEquals("x", f.invokeValue(null, List.of()));
	}

	@Test void a02_overrideReturnsTypedValue() throws Exception {
		VarFunction f = new Base() {
			@Override public String invoke(VarResolverSession s, List<Object> a) { return "5"; }
			@Override public Object invokeValue(VarResolverSession s, List<Object> a) { return 5L; }
		};
		assertEquals(5L, f.invokeValue(null, List.of()));
	}

	@Test void a03_nullPreserved() throws Exception {
		VarFunction f = new Base() {
			@Override public String invoke(VarResolverSession s, List<Object> a) { return null; }
		};
		assertNull(f.invokeValue(null, List.of()));
	}
}
