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
package org.apache.juneau.commons.function;

import static org.apache.juneau.commons.function.Suppliers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

@TestMethodOrder(MethodOrderer.MethodName.class)
class Suppliers_Test extends TestBase {

	//====================================================================================================
	// memoize
	//====================================================================================================
	@Test
	void a001_memoize_callsSupplierOnce() {
		var counter = new AtomicInteger();
		var s = memoize(() -> { counter.incrementAndGet(); return "value"; });
		assertEquals("value", s.get());
		assertEquals("value", s.get());
		assertEquals(1, counter.get());
	}

	@Test
	void a002_memoize_nullResult() {
		var counter = new AtomicInteger();
		var s = memoize(() -> { counter.incrementAndGet(); return null; });
		assertNull(s.get());
		assertNull(s.get());
		assertEquals(1, counter.get());
	}

	@Test
	void a003_memoize_nullSupplierThrows() {
		assertThrows(Exception.class, () -> memoize(null));
	}

	//====================================================================================================
	// memoizer
	//====================================================================================================
	@Test
	void a010_memoizer_callsSupplierOnce() {
		var counter = new AtomicInteger();
		var m = memoizer(() -> { counter.incrementAndGet(); return "value"; });
		assertEquals("value", m.get());
		assertEquals("value", m.get());
		assertEquals(1, counter.get());
	}

	@Test
	void a011_memoizer_reset() {
		var counter = new AtomicInteger();
		var m = memoizer(() -> { counter.incrementAndGet(); return "v" + counter.get(); });
		assertEquals("v1", m.get());
		m.reset();
		assertEquals("v2", m.get());
		assertEquals(2, counter.get());
	}

	@Test
	void a012_memoizer_nullSupplierThrows() {
		assertThrows(Exception.class, () -> memoizer(null));
	}
}
