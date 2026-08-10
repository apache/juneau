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
package org.apache.juneau.swaps;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Verifies that resolving a {@link Class} value from parsed input does not trigger the named class's static
 * initializer as a side effect.
 */
class ClassSwap_NoInitialize_Test extends TestBase {

	/** Records whether its static initializer has run.  Referencing {@code .class} does not initialize it. */
	static class SideEffect {
		static volatile boolean initialized = false;
		static { initialized = true; }
	}

	@Test void a01_unswapDoesNotInitializeResolvedClass() throws Exception {
		SideEffect.initialized = false;
		var swap = new ClassSwap();

		var resolved = swap.unswap(null, SideEffect.class.getName(), null);

		assertEquals(SideEffect.class, resolved);
		assertFalse(SideEffect.initialized, "Resolving the class name must not run its static initializer");
	}

	@Test void a02_unswapResolvesCommonClass() throws Exception {
		var swap = new ClassSwap();
		assertEquals(String.class, swap.unswap(null, "java.lang.String", null));
	}
}
