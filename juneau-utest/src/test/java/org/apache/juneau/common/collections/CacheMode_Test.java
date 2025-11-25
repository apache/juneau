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
package org.apache.juneau.common.collections;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class CacheMode_Test extends TestBase {

	//====================================================================================================
	// parse(String)
	//====================================================================================================

	@Test
	void a01_parse_none_lowercase() {
		assertEquals(CacheMode.NONE, CacheMode.parse("none"));
	}

	@Test
	void a02_parse_none_uppercase() {
		assertEquals(CacheMode.NONE, CacheMode.parse("NONE"));
	}

	@Test
	void a03_parse_none_mixedCase() {
		assertEquals(CacheMode.NONE, CacheMode.parse("None"));
	}

	@Test
	void a04_parse_weak_lowercase() {
		assertEquals(CacheMode.WEAK, CacheMode.parse("weak"));
	}

	@Test
	void a05_parse_weak_uppercase() {
		assertEquals(CacheMode.WEAK, CacheMode.parse("WEAK"));
	}

	@Test
	void a06_parse_weak_mixedCase() {
		assertEquals(CacheMode.WEAK, CacheMode.parse("Weak"));
	}

	@Test
	void a07_parse_full_lowercase() {
		assertEquals(CacheMode.FULL, CacheMode.parse("full"));
	}

	@Test
	void a08_parse_full_uppercase() {
		assertEquals(CacheMode.FULL, CacheMode.parse("FULL"));
	}

	@Test
	void a09_parse_full_mixedCase() {
		assertEquals(CacheMode.FULL, CacheMode.parse("Full"));
	}

	@Test
	void a10_parse_null_returnsFull() {
		assertEquals(CacheMode.FULL, CacheMode.parse(null));
	}

	@Test
	void a11_parse_invalid_returnsFull() {
		assertEquals(CacheMode.FULL, CacheMode.parse("invalid"));
		assertEquals(CacheMode.FULL, CacheMode.parse(""));
		assertEquals(CacheMode.FULL, CacheMode.parse("NON"));
		assertEquals(CacheMode.FULL, CacheMode.parse("WEA"));
	}
}

