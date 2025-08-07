// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.marshaller;

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.junit.jupiter.api.*;

class MsgPack_Test extends SimpleTestBase {

	@Test void a01_to() throws Exception {
		Object in1 = "foo", in2 = JsonMap.of("foo", "bar");
		String expected1 = "A3666F6F", expected2 = "81A3666F6FA3626172";

		assertEquals(toHex(MsgPack.of(in1)), expected1);
		assertEquals(toHex(bytes(MsgPack.of(in1,baos()))), expected1);
		assertEquals(toHex(MsgPack.of(in2)), expected2);
		assertEquals(toHex(bytes(MsgPack.of(in2,baos()))), expected2);
	}

	@Test void a02_from() throws Exception {
		String in1 = "A3666F6F", in2 = "81A3666F6FA3626172";
		String expected1 = "foo", expected2 = "{foo:'bar'}";

		assertEquals(expected1, MsgPack.to(in1, String.class));
		assertEquals(expected1, MsgPack.to(fromHex(in1), String.class));
		assertJson(MsgPack.to(in2, Map.class, String.class, String.class), expected2);
		assertJson(MsgPack.to(fromHex(in2), Map.class, String.class, String.class), expected2);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helper methods
	//-----------------------------------------------------------------------------------------------------------------

	private OutputStream baos() {
		return new ByteArrayOutputStream();
	}

	private byte[] bytes(Object o) {
		return ((ByteArrayOutputStream)o).toByteArray();
	}
}