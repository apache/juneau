package org.apache.juneau.http;

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.http.HttpHeaders.*;
import static org.apache.juneau.http.HttpParts.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.junit.Assert.*;

import java.util.*;

import org.apache.http.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.part.*;
import org.junit.*;

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
public class HttpHeaders_Test {

	@Test
	public void a01_cast() {
		BasicPart x1 = part("X1","1");
		SerializedPart x2 = serializedPart("X2","2");
		Header x3 = header("X3","3");
		SerializedHeader x4 = serializedHeader("X4","4");
		Map.Entry<String,Object> x5 = map("X5",(Object)"5").entrySet().iterator().next();
		org.apache.http.message.BasicNameValuePair x6 = new org.apache.http.message.BasicNameValuePair("X6","6");
		NameValuePairable x7 = new NameValuePairable() {
			@Override
			public NameValuePair asNameValuePair() {
				return part("X7","7");
			}
		};
		Headerable x8 = new Headerable() {
			@Override
			public Header asHeader() {
				return header("X8","8");
			}
		};
		SerializedPart x9 = serializedPart("X9",()->"9");

		assertObject(HttpHeaders.cast(x1)).isType(Header.class).asJson().is("'X1: 1'");
		assertObject(HttpHeaders.cast(x2)).isType(Header.class).asJson().is("'X2: 2'");
		assertObject(HttpHeaders.cast(x3)).isType(Header.class).asJson().is("'X3: 3'");
		assertObject(HttpHeaders.cast(x4)).isType(Header.class).asJson().is("'X4: 4'");
		assertObject(HttpHeaders.cast(x5)).isType(Header.class).asJson().is("'X5: 5'");
		assertObject(HttpHeaders.cast(x6)).isType(Header.class).asJson().is("'X6: 6'");
		assertObject(HttpHeaders.cast(x7)).isType(Header.class).asJson().is("'X7: 7'");
		assertObject(HttpHeaders.cast(x8)).isType(Header.class).asJson().is("'X8: 8'");
		assertObject(HttpHeaders.cast(x9)).isType(Header.class).asJson().is("'X9: 9'");

		assertThrown(()->HttpHeaders.cast("X")).asMessage().is("Object of type java.lang.String could not be converted to a Header.");
		assertThrown(()->HttpHeaders.cast(null)).asMessage().is("Object of type null could not be converted to a Header.");

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
