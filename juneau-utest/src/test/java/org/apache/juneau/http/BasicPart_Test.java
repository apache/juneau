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

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.http.HttpHeaders.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;
import static org.apache.juneau.http.HttpParts.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.util.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.juneau.http.part.*;
import org.apache.juneau.http.header.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class BasicPart_Test {

	@Test
	public void a01_ofPair() {
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

	@Test
	public void a02_of() {
		BasicPart x;
		x = part("Foo","bar");
		assertObject(x).asJson().is("'Foo=bar'");
		x = part("Foo",()->"bar");
		assertObject(x).asJson().is("'Foo=bar'");
	}

	@Test
	public void a03_cast() {
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

		assertObject(BasicPart.cast(x1)).isType(NameValuePair.class).asJson().is("'X1=1'");
		assertObject(BasicPart.cast(x2)).isType(NameValuePair.class).asJson().is("'X2=2'");
		assertObject(BasicPart.cast(x3)).isType(NameValuePair.class).asJson().is("'X3: 3'");
		assertObject(BasicPart.cast(x4)).isType(NameValuePair.class).asJson().is("'X4: 4'");
		assertObject(BasicPart.cast(x5)).isType(NameValuePair.class).asJson().is("'X5=5'");
		assertObject(BasicPart.cast(x6)).isType(NameValuePair.class).asJson().is("{name:'X6',value:'6'}");
		assertObject(BasicPart.cast(x7)).isType(NameValuePair.class).asJson().is("'X7=7'");
		assertObject(BasicPart.cast(x8)).isType(NameValuePair.class).asJson().is("'X8=8'");

		assertThrown(()->BasicPart.cast("X")).asMessage().is("Object of type java.lang.String could not be converted to a Part.");
		assertThrown(()->BasicPart.cast(null)).asMessage().is("Object of type null could not be converted to a Part.");

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

	@Test
	public void a04_asHeader() {
		BasicPart x = part("X1","1");
		assertObject(x.asHeader()).isType(Header.class).asJson().is("'X1: 1'");
	}

	@Test
	public void a05_assertions() {
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
