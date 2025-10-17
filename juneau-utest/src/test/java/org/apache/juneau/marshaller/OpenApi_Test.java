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
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.junit.jupiter.api.*;

class OpenApi_Test extends TestBase {

	@Test void a01_to() throws Exception {
		var in1 = "foo";
		var in2 = JsonMap.of("foo", "bar");
		var expected1 = "foo";
		var expected2 = "foo=bar";

		assertString(expected1, OpenApi.of(in1));
		assertString(expected1, OpenApi.of(in1,stringWriter()));
		assertString(expected2, OpenApi.of(in2));
		assertString(expected2, OpenApi.of(in2,stringWriter()));
	}

	@Test void a02_from() throws Exception {
		var in1 = "foo";
		var in2 = "foo=bar";
		var expected1 = "foo";
		var expected2 = "{foo:'bar'}";

		assertEquals(expected1, OpenApi.to(in1, String.class));
		assertEquals(expected1, OpenApi.to(stringReader(in1), String.class));
		assertJson(expected2, OpenApi.to(in2, Map.class, String.class, String.class));
		assertJson(expected2, OpenApi.to(stringReader(in2), Map.class, String.class, String.class));
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