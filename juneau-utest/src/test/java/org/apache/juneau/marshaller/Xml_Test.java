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

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.runners.MethodSorters.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.collections.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Xml_Test {

	@Test
	public void a01_to() throws Exception {
		Object in1 = "foo", in2 = JsonMap.of("foo", "bar");
		String expected1 = "<string>foo</string>", expected2 = "<object><foo>bar</foo></object>";

		assertString(Xml.of(in1)).is(expected1);
		assertString(Xml.of(in1,stringWriter())).is(expected1);
		assertString(Xml.of(in2)).is(expected2);
		assertString(Xml.of(in2,stringWriter())).is(expected2);
	}

	@Test
	public void a02_from() throws Exception {
		String in1 = "<string>foo</string>", in2 = "<object><foo>bar</foo></object>";
		String expected1 = "foo", expected2 = "{foo:'bar'}";

		assertString(Xml.to(in1, String.class)).is(expected1);
		assertString(Xml.to(stringReader(in1), String.class)).is(expected1);
		assertObject(Xml.to(in2, Map.class, String.class, String.class)).asJson().is(expected2);
		assertObject(Xml.to(stringReader(in2), Map.class, String.class, String.class)).asJson().is(expected2);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helper methods
	//-----------------------------------------------------------------------------------------------------------------

	private Writer stringWriter() {
		return new StringWriter();
	}

	private Reader stringReader(String s) {
		return new StringReader(s);
	}

}
