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
package org.apache.juneau.marshaller;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.junit.jupiter.api.*;

class Csv_Test extends TestBase{

	@Test void a01_to() throws Exception {
		var in1 = "foo";
		var in2 = ao(JsonMap.of("a","foo","b","bar"));
		var expected1 = "value\nfoo\n";
		var expected2 = "a,b\nfoo,bar\n";

		assertString(expected1, Csv.of(in1));
		assertString(expected1, Csv.of(in1,stringWriter()));
		assertString(expected2, Csv.of(in2));
		assertString(expected2, Csv.of(in2,stringWriter()));
	}

	@SuppressWarnings("unchecked")
	@Test void a02_from() throws Exception {
		// Parser is now fully implemented.
		var csv1 = "value\nfoo\n";
		var csv2 = "a,b\nfoo,bar\n";

		// Parse a single-column list of strings
		var r1 = (List<String>) Csv.to(csv1, List.class, String.class);
		assertEquals(1, r1.size());
		assertEquals("foo", r1.get(0));

		// Parse from Reader
		var r2 = (List<String>) Csv.to(stringReader(csv1), List.class, String.class);
		assertEquals(1, r2.size());

		// Parse into a map
		var r3 = (Map<?, ?>) Csv.to(csv2, Map.class);
		assertEquals("foo", r3.get("a"));
		assertEquals("bar", r3.get("b"));

		// Parse from Reader into a map
		var r4 = (Map<?, ?>) Csv.to(stringReader(csv2), Map.class);
		assertEquals("foo", r4.get("a"));
	}
	//-----------------------------------------------------------------------------------------------------------------
	// Helper methods
	//-----------------------------------------------------------------------------------------------------------------

	private static Writer stringWriter() {
		return new StringWriter();
	}

	private static Reader stringReader(String s) {
		return new StringReader(s);
	}
}