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
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"resource" // Stream/reader instances are intentional short-lived test fixtures; auto-close not required for these assertions.
})
class MsgPack_Test extends TestBase {

	@Test void a01_to() throws Exception {
		var in1 = "foo";
		var in2 = JsonMap.of("foo", "bar");
		var expected1 = "A3666F6F";
		var expected2 = "81A3666F6FA3626172";

		assertEquals(toHex(MsgPack.DEFAULT.of(in1)), expected1);
		{ var baos1 = baos(); MsgPack.DEFAULT.of(in1, baos1); assertEquals(toHex(bytes(baos1)), expected1); }
		assertEquals(toHex(MsgPack.DEFAULT.of(in2)), expected2);
		{ var baos2 = baos(); MsgPack.DEFAULT.of(in2, baos2); assertEquals(toHex(bytes(baos2)), expected2); }
	}

	@Test void a02_from() throws Exception {
		var in1 = "A3666F6F";
		var in2 = "81A3666F6FA3626172";
		var expected1 = "foo";
		var expected2 = "{foo:'bar'}";

		assertEquals(expected1, MsgPack.DEFAULT.to(in1, String.class));
		assertEquals(expected1, MsgPack.DEFAULT.to(fromHex(in1), String.class));
		assertJson(expected2, MsgPack.DEFAULT.to(in2, Map.class, String.class, String.class));
		assertJson(expected2, MsgPack.DEFAULT.to(fromHex(in2), Map.class, String.class, String.class));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helper methods
	//-----------------------------------------------------------------------------------------------------------------

	private static OutputStream baos() {
		return new ByteArrayOutputStream();
	}

	private static byte[] bytes(Object o) {
		return ((ByteArrayOutputStream)o).toByteArray();
	}
}