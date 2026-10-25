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
package org.apache.juneau.commons.settings;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

/**
 * Contract tests for the memoized, unmodifiable {@link RelaxedPropertySource#candidates(String)}.
 */
class RelaxedPropertySource_Memo_Test extends TestBase {

	@Test void a01_unmodifiable() {
		var c = RelaxedPropertySource.candidates("memo.a01.key");
		assertEquals(List.of("memo.a01.key", "MEMO_A01_KEY"), c);
		assertThrows(UnsupportedOperationException.class, () -> c.add("x"));
	}

	@Test void a02_sameReferenceOnRepeatedHits() {
		var first = RelaxedPropertySource.candidates("memo.a02.key");
		assertSame(first, RelaxedPropertySource.candidates("memo.a02.key"));
	}

	@Test void a03_nullName_throwsNpe() {
		assertThrows(NullPointerException.class, () -> RelaxedPropertySource.candidates(null));
	}

	@Test void a04_sourceGet_nullName_isMissing() {
		var src = new RelaxedPropertySource(name -> PropertyLookupResult.present("never"));
		assertFalse(src.get(null).isPresent());
	}

	@Test void a05_sourceGet_stillProbesCandidatesInOrder() {
		var seen = new ArrayList<String>();
		var src = new RelaxedPropertySource(name -> {
			seen.add(name);
			return name.equals("MEMO_A05_KEY") ? PropertyLookupResult.present("v") : PropertyLookupResult.missing();
		});
		assertEquals("v", src.get("memo.a05.key").value().orElse(null));
		assertEquals(List.of("memo.a05.key", "MEMO_A05_KEY"), seen);
	}
}
