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
package org.apache.juneau.common.utils;

import static org.apache.juneau.common.utils.ThrowableUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class ThrowableUtils_Test extends TestBase {

	//====================================================================================================
	// findCause(Throwable, Class)
	//====================================================================================================
	@Test
	void a01_findCause() {
		IOException rootCause = new IOException("root");
		RuntimeException middleCause = new RuntimeException("middle", rootCause);
		Exception topException = new Exception("top", middleCause);

		// Find IOException
		assertTrue(findCause(topException, IOException.class).isPresent());
		assertEquals("root", findCause(topException, IOException.class).get().getMessage());

		// Find RuntimeException
		assertTrue(findCause(topException, RuntimeException.class).isPresent());
		assertEquals("middle", findCause(topException, RuntimeException.class).get().getMessage());

		// Find Exception (returns itself)
		assertTrue(findCause(topException, Exception.class).isPresent());
		assertEquals("top", findCause(topException, Exception.class).get().getMessage());

		// Not found
		assertFalse(findCause(topException, IllegalArgumentException.class).isPresent());

		// Null exception
		assertFalse(findCause(null, IOException.class).isPresent());
	}

	@Test
	void a02_findCause_noCause() {
		Exception ex = new Exception("test");
		
		// Find itself
		assertTrue(findCause(ex, Exception.class).isPresent());
		assertEquals("test", findCause(ex, Exception.class).get().getMessage());

		// Not found
		assertFalse(findCause(ex, IOException.class).isPresent());
	}

	@Test
	void a03_findCause_longChain() {
		Throwable cause = new IllegalStateException("root");
		for (int i = 0; i < 10; i++) {
			cause = new RuntimeException("level" + i, cause);
		}

		// Should find the root cause
		assertTrue(findCause(cause, IllegalStateException.class).isPresent());
		assertEquals("root", findCause(cause, IllegalStateException.class).get().getMessage());
	}
}

