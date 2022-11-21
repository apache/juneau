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

import static org.junit.runners.MethodSorters.*;
import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.common.internal.StringUtils.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.collections.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class MsgPack_Test {

	@Test
	public void a01_to() throws Exception {
		Object in1 = "foo", in2 = JsonMap.of("foo", "bar");
		String expected1 = "A3666F6F", expected2 = "81A3666F6FA3626172";

		assertBytes(MsgPack.of(in1)).asHex().is(expected1);
		assertBytes(bytes(MsgPack.of(in1,baos()))).asHex().is(expected1);
		assertBytes(MsgPack.of(in2)).asHex().is(expected2);
		assertBytes(bytes(MsgPack.of(in2,baos()))).asHex().is(expected2);
	}

	@Test
	public void a02_from() throws Exception {
		String in1 = "A3666F6F", in2 = "81A3666F6FA3626172";
		String expected1 = "foo", expected2 = "{foo:'bar'}";

		assertString(MsgPack.to(in1, String.class)).is(expected1);
		assertString(MsgPack.to(fromHex(in1), String.class)).is(expected1);
		assertObject(MsgPack.to(in2, Map.class, String.class, String.class)).asJson().is(expected2);
		assertObject(MsgPack.to(fromHex(in2), Map.class, String.class, String.class)).asJson().is(expected2);
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
