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

import org.apache.juneau.commons.TestBase;
import org.junit.jupiter.api.*;

/**
 * Tests the strict-mode flag and limits on {@link VarResolver.Builder}.
 */
class VarResolver_StrictMode_Test extends TestBase {

	@Test void a01_defaults() {
		assertFalse(VarResolver.DEFAULT.strict);
		assertEquals(1000, VarResolver.DEFAULT.maxCollectionSize);
		assertEquals(32, VarResolver.DEFAULT.maxExprDepth);
	}

	@Test void a02_copyPreserves() {
		var r = VarResolver.create().strict(true).maxCollectionSize(5).maxExprDepth(7).build().copy().build();
		assertTrue(r.strict);
		assertEquals(5, r.maxCollectionSize);
		assertEquals(7, r.maxExprDepth);
	}

	@Test void a03_rejectsNonPositive() {
		assertThrows(IllegalArgumentException.class, () -> VarResolver.create().maxCollectionSize(0));
		assertThrows(IllegalArgumentException.class, () -> VarResolver.create().maxExprDepth(-1));
	}
}
