//***************************************************************************************************************************
//* Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
//* distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
//* to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
//* with the License.  You may obtain a copy of the License at                                                              *
//*                                                                                                                         *
//*  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
//*                                                                                                                         *
//* Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
//* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
//* specific language governing permissions and limitations under the License.                                              *
//***************************************************************************************************************************
package org.apache.juneau.http;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.http.HttpHeaders.*;
import static org.apache.juneau.http.HttpParts.*;
import static org.junit.Assert.*;

import java.util.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.part.*;
import org.apache.juneau.internal.*;
import org.junit.jupiter.api.*;

class HttpHeaders_Test extends SimpleTestBase {

	@Test void a01_cast() {
		var x1 = part("X1","1");
		var x2 = serializedPart("X2","2");
		var x3 = header("X3","3");
		var x4 = serializedHeader("X4","4");
		Map.Entry<String,Object> x5 = CollectionUtils.map("X5",(Object)"5").entrySet().iterator().next();
		org.apache.http.message.BasicNameValuePair x6 = new org.apache.http.message.BasicNameValuePair("X6","6");
		NameValuePairable x7 = () -> part("X7","7");
		Headerable x8 = () -> header("X8","8");
		SerializedPart x9 = serializedPart("X9",()->"9");

		assertTypeAndJson(HttpHeaders.cast(x1), Header.class, "'X1: 1'");
		assertTypeAndJson(HttpHeaders.cast(x2), Header.class, "'X2: 2'");
		assertTypeAndJson(HttpHeaders.cast(x3), Header.class, "'X3: 3'");
		assertTypeAndJson(HttpHeaders.cast(x4), Header.class, "'X4: 4'");
		assertTypeAndJson(HttpHeaders.cast(x5), Header.class, "'X5: 5'");
		assertTypeAndJson(HttpHeaders.cast(x6), Header.class, "'X6: 6'");
		assertTypeAndJson(HttpHeaders.cast(x7), Header.class, "'X7: 7'");
		assertTypeAndJson(HttpHeaders.cast(x8), Header.class, "'X8: 8'");
		assertTypeAndJson(HttpHeaders.cast(x9), Header.class, "'X9: 9'");

		assertThrowsWithMessage(BasicRuntimeException.class, "Object of type java.lang.String could not be converted to a Header.", ()->HttpHeaders.cast("X"));
		assertThrowsWithMessage(BasicRuntimeException.class, "Object of type null could not be converted to a Header.", ()->HttpHeaders.cast(null));

		assertTrue(HttpHeaders.canCast(x1));
		assertTrue(HttpHeaders.canCast(x2));
		assertTrue(HttpHeaders.canCast(x3));
		assertTrue(HttpHeaders.canCast(x4));
		assertTrue(HttpHeaders.canCast(x5));
		assertTrue(HttpHeaders.canCast(x6));
		assertTrue(HttpHeaders.canCast(x7));
		assertTrue(HttpHeaders.canCast(x8));
		assertTrue(HttpHeaders.canCast(x9));

		assertFalse(HttpHeaders.canCast("X"));
		assertFalse(HttpHeaders.canCast(null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Utility methods
	//------------------------------------------------------------------------------------------------------------------

	private BasicHeader header(String name, Object val) {
		return basicHeader(name, val);
	}

	private BasicPart part(String name, Object val) {
		return basicPart(name, val);
	}

}