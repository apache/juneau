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

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.junit.jupiter.api.*;

class Jcs_Test extends TestBase {

	@Test
	void e01_of() throws Exception {
		var m = JsonMap.of("name", "Alice", "age", 30);
		var s = Jcs.of(m);
		assertEquals("{\"age\":30,\"name\":\"Alice\"}", s);
	}

	@Test
	void e02_to() throws Exception {
		var s = "{\"age\":30,\"name\":\"Alice\"}";
		var m = Jcs.to(s, JsonMap.class);
		assertEquals(30, m.getInt("age"));
		assertEquals("Alice", m.getString("name"));
	}

	@Test
	void e03_roundTrip() throws Exception {
		var m = JsonMap.of("a", 1, "b", 2, "c", 3);
		var s = Jcs.of(m);
		var m2 = Jcs.to(s, JsonMap.class);
		assertEquals(m, m2);
	}

	@Test
	void e04_defaultInstance() throws Exception {
		var m = JsonMap.of("x", 1);
		assertEquals("{\"x\":1}", Jcs.DEFAULT.write(m));
	}
}
