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
public class N3_Test {

	private static String EOL = System.getProperty("line.separator");

	@Test
	public void a01_to() throws Exception {
		Object in1 = "foo", in2 = JsonMap.of("foo", "bar");
		String
			expected1 = ""
				+ "@prefix jp:      <http://www.apache.org/juneaubp/> ." + EOL
				+ "@prefix j:       <http://www.apache.org/juneau/> ." + EOL + EOL
				+ "[]    j:value \"foo\" ." + EOL,
			expected2 = ""
				+ "@prefix jp:      <http://www.apache.org/juneaubp/> ." + EOL
				+ "@prefix j:       <http://www.apache.org/juneau/> ." + EOL + EOL
				+ "[]    jp:foo  \"bar\" ." + EOL;

		assertString(N3.of(in1)).is(expected1);
		assertString(N3.of(in1,stringWriter())).is(expected1);
		assertString(N3.of(in2)).is(expected2);
		assertString(N3.of(in2,stringWriter())).is(expected2);
	}

	@Test
	public void a02_from() throws Exception {
		String
			in1 = ""
				+ "@prefix jp:      <http://www.apache.org/juneaubp/> ." + EOL
				+ "@prefix j:       <http://www.apache.org/juneau/> ." + EOL + EOL
				+ "[]    j:value \"foo\" ." + EOL,
			in2 = ""
				+ "@prefix jp:      <http://www.apache.org/juneaubp/> ." + EOL
				+ "@prefix j:       <http://www.apache.org/juneau/> ." + EOL + EOL
				+ "[]    jp:foo  \"bar\" ." + EOL;
		String expected1 = "foo", expected2 = "{foo:'bar'}";

		assertString(N3.to(in1, String.class)).is(expected1);
		assertString(N3.to(stringReader(in1), String.class)).is(expected1);
		assertObject(N3.to(in2, Map.class, String.class, String.class)).asJson().is(expected2);
		assertObject(N3.to(stringReader(in2), Map.class, String.class, String.class)).asJson().is(expected2);
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
