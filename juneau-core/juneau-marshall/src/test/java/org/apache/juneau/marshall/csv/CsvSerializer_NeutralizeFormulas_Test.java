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
package org.apache.juneau.marshall.csv;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests the optional spreadsheet formula neutralization of {@link CsvSerializer}.
 */
class CsvSerializer_NeutralizeFormulas_Test extends TestBase {

	public static class A {
		public String a;
		public int b;
		A(String a, int b) { this.a = a; this.b = b; }
	}

	@Test void a01_defaultLeavesCellsUnchanged() throws Exception {
		var out = CsvSerializer.DEFAULT.write(List.of(new A("=1+1", -5)));
		assertTrue(out.contains("=1+1"), out);
		assertFalse(out.contains("'="), out);
	}

	@Test void a02_enabledPrefixesStringCells() throws Exception {
		var s = CsvSerializer.create().neutralizeFormulas(true).build();
		for (var v : new String[] { "=1+1", "+1", "-1", "@SUM(A1)", "\tfoo" }) {
			var out = s.write(List.of(new A(v, 1)));
			assertTrue(out.contains("'" + v), v + " -> " + out);
		}
	}

	@Test void a03_enabledLeavesNumbersAndPlainText() throws Exception {
		var s = CsvSerializer.create().neutralizeFormulas(true).build();
		var out = s.write(List.of(new A("hello", -5)));
		assertEquals("a,b\nhello,-5\n", out);
	}

	@Test void a04_enabledAppliesToMapCells() throws Exception {
		var s = CsvSerializer.create().neutralizeFormulas(true).build();
		var out = s.write(List.of(Map.of("k", "=cmd")));
		assertTrue(out.contains("'=cmd"), out);
	}
}
