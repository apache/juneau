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
package org.apache.juneau.http;

import static org.apache.juneau.http.HttpHeaders.*;
import static org.apache.juneau.http.HttpParts.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.part.*;
import org.junit.jupiter.api.*;

class BasicPart_Test extends SimpleTestBase {

	@Test void a01_ofPair() {
		BasicPart x = basicPart("Foo:bar");
		assertEquals("Foo", x.getName());
		assertEquals("bar", x.getValue());

		x = basicPart(" Foo : bar ");
		assertEquals("Foo", x.getName());
		assertEquals("bar", x.getValue());

		x = basicPart(" Foo : bar : baz ");
		assertEquals("Foo", x.getName());
		assertEquals("bar : baz", x.getValue());

		x = basicPart("Foo");
		assertEquals("Foo", x.getName());
		assertEquals("", x.getValue());

		assertNull(basicPart((String)null));
	}

	@Test void a02_of() {
		BasicPart x;
		x = part("Foo","bar");
		assertString("Foo=bar", x);
		x = part("Foo",()->"bar");
		assertString("Foo=bar", x);
	}

	@Test void a03_cast() {
		BasicPart x1 = part("X1","1");
		SerializedPart x2 = serializedPart("X2","2");
		Header x3 = header("X3","3");
		SerializedHeader x4 = serializedHeader("X4","4");
		Map.Entry<String,Object> x5 = map("X5",(Object)"5").entrySet().iterator().next();
		org.apache.http.message.BasicNameValuePair x6 = new org.apache.http.message.BasicNameValuePair("X6","6");
		NameValuePairable x7 = () -> part("X7","7");
		Headerable x8 = () -> header("X8","8");

		assertTypeAndJson(BasicPart.cast(x1), NameValuePair.class, "'X1=1'");
		assertTypeAndJson(BasicPart.cast(x2), NameValuePair.class, "'X2=2'");
		assertTypeAndJson(BasicPart.cast(x3), NameValuePair.class, "'X3: 3'");
		assertTypeAndJson(BasicPart.cast(x4), NameValuePair.class, "'X4: 4'");
		assertTypeAndJson(BasicPart.cast(x5), NameValuePair.class, "'X5=5'");
		assertTypeAndJson(BasicPart.cast(x6), NameValuePair.class, "{name:'X6',value:'6'}");
		assertTypeAndJson(BasicPart.cast(x7), NameValuePair.class, "'X7=7'");
		assertTypeAndJson(BasicPart.cast(x8), NameValuePair.class, "'X8=8'");

		assertThrows(BasicRuntimeException.class, ()->BasicPart.cast("X"), "Object of type java.lang.String could not be converted to a Part.");
		assertThrows(BasicRuntimeException.class, ()->BasicPart.cast(null), "Object of type null could not be converted to a Part.");

		assertTrue(BasicPart.canCast(x1));
		assertTrue(BasicPart.canCast(x2));
		assertTrue(BasicPart.canCast(x3));
		assertTrue(BasicPart.canCast(x4));
		assertTrue(BasicPart.canCast(x5));
		assertTrue(BasicPart.canCast(x6));
		assertTrue(BasicPart.canCast(x7));

		assertFalse(BasicPart.canCast("X"));
		assertFalse(BasicPart.canCast(null));
	}

	@Test void a04_asHeader() {
		BasicPart x = part("X1","1");
		assertString("X1: 1", x.asHeader());
	}

	@Test void a05_assertions() {
		BasicPart x = part("X1","1");
		x.assertName().is("X1").assertValue().is("1");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Utility methods
	//------------------------------------------------------------------------------------------------------------------

	private BasicHeader header(String name, Object val) {
		return basicHeader(name, val);
	}

	private BasicPart part(String name, Supplier<?> val) {
		return basicPart(name, val);
	}

	private BasicPart part(String name, Object val) {
		return basicPart(name, val);
	}
}