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
package org.apache.juneau.marshall.hocon;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

import java.util.*;

/**
 * Tests that HOCON substitution resolution caps the total size of concatenated output.
 */
class HoconResolver_OutputCap_Test extends TestBase {

	@Test void a01_chainedConcatenationRejected() {
		var sb = new StringBuilder("a0 = \"xxxxxxxxxxxxxxxx\"\n");
		for (var i = 1; i <= 40; i++)
			sb.append("a").append(i).append(" = ${a").append(i - 1).append("}${a").append(i - 1).append("}\n");
		assertThrows(Exception.class, () -> HoconParser.DEFAULT.read(sb.toString(), Object.class));
	}

	@Test void a02_smallConcatenationAllowed() throws Exception {
		var m = HoconParser.DEFAULT.read("a = \"x\"\nb = ${a}${a}\n", Map.class);
		assertEquals("xx", m.get("b"));
	}
}
