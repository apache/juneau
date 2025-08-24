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

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.parser.*;
import org.junit.jupiter.api.*;

class Csv_Test extends SimpleTestBase{

	@Test void a01_to() throws Exception {
		var in1 = "foo";
		var in2 = new Object[]{JsonMap.of("a","foo","b","bar")};
		String expected1="value\nfoo\n", expected2 = "a,b\nfoo,bar\n";

		assertString(expected1, Csv.of(in1));
		assertString(expected1, Csv.of(in1,stringWriter()));
		assertString(expected2, Csv.of(in2));
		assertString(expected2, Csv.of(in2,stringWriter()));
	}

	@Test void a02_from() {
		String in1 = "'foo'", in2 = "{foo:'bar'}";

		assertThrowsWithMessage(ParseException.class, "Not implemented.", ()->Csv.to(in1, String.class));
		assertThrowsWithMessage(ParseException.class, "Not implemented.", ()->Csv.to(stringReader(in1), String.class));
		assertThrowsWithMessage(ParseException.class, "Not implemented.", ()->Csv.to(in2, Map.class, String.class, String.class));
		assertThrowsWithMessage(ParseException.class, "Not implemented.", ()->Csv.to(stringReader(in2), Map.class, String.class, String.class));
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