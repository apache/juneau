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
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.junit.jupiter.api.*;

class RdfProto_Test extends TestBase {

	@Test void a01_to() throws Exception {
		var in1 = "foo";
		var in2 = JsonMap.of("foo", "bar");
		var out1 = RdfProto.of(in1);
		var out2 = RdfProto.of(in2);

		assertNotNull(out1);
		assertTrue(out1.length > 0);
		assertNotNull(out2);
		assertTrue(out2.length > 0);
		// Verify the OutputStream variant also produces non-empty output
		var baosOut = bytes(RdfProto.of(in1, baos()));
		assertNotNull(baosOut);
		assertTrue(baosOut.length > 0);
		// Verify both outputs are semantically equivalent via roundtrip
		assertEquals(RdfProto.to(out1, String.class), RdfProto.to(baosOut, String.class));
	}

	@Test void a02_from() throws Exception {
		var in1 = JsonMap.of("foo", "bar");
		var bytes = RdfProto.of(in1);
		var result = RdfProto.to(bytes, Map.class, String.class, String.class);
		assertJson("{foo:'bar'}", result);
		assertJson("{foo:'bar'}", RdfProto.to(fromHex(toHex(bytes)), Map.class, String.class, String.class));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helper methods
	//-----------------------------------------------------------------------------------------------------------------

	private static OutputStream baos() {
		return new ByteArrayOutputStream();
	}

	private static byte[] bytes(Object o) {
		return ((ByteArrayOutputStream) o).toByteArray();
	}
}
