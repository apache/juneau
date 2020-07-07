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
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.juneau.collections.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class BasicNameValuePair_Test {

	@Test
	public void a01_ofPair() {
		BasicNameValuePair x = BasicNameValuePair.ofPair("Foo:bar");
		assertEquals("Foo", x.getName());
		assertEquals("bar", x.getValue());

		x = BasicNameValuePair.ofPair(" Foo : bar ");
		assertEquals("Foo", x.getName());
		assertEquals("bar", x.getValue());

		x = BasicNameValuePair.ofPair(" Foo : bar : baz ");
		assertEquals("Foo", x.getName());
		assertEquals("bar : baz", x.getValue());

		x = BasicNameValuePair.ofPair("Foo");
		assertEquals("Foo", x.getName());
		assertEquals("", x.getValue());

		assertNull(BasicNameValuePair.ofPair((String)null));
	}

	@Test
	public void a02_of() {
		BasicNameValuePair x;
		x = pair("Foo","bar");
		assertObject(x).json().is("'Foo=bar'");
		x = pair("Foo",()->"bar");
		assertObject(x).json().is("'Foo=bar'");
	}

	@Test
	public void a03_cast() {
		BasicNameValuePair x1 = pair("X1","1");
		SerializedNameValuePairBuilder x2 = SerializedNameValuePair.create().name("X2").value("2");
		Header x3 = header("X3","3");
		SerializedHeaderBuilder x4 = SerializedHeader.create().name("X4").value("4");
		Map.Entry<String,Object> x5 = AMap.of("X5",(Object)"5").entrySet().iterator().next();
		org.apache.http.message.BasicNameValuePair x6 = new org.apache.http.message.BasicNameValuePair("X6","6");
		NameValuePairable x7 = new NameValuePairable() {
			@Override
			public NameValuePair asNameValuePair() {
				return pair("X7","7");
			}
		};
		Headerable x8 = new Headerable() {
			@Override
			public Header asHeader() {
				return header("X8","8");
			}
		};

		assertObject(BasicNameValuePair.cast(x1)).isType(NameValuePair.class).json().is("'X1=1'");
		assertObject(BasicNameValuePair.cast(x2)).isType(NameValuePair.class).json().is("'X2=2'");
		assertObject(BasicNameValuePair.cast(x3)).isType(NameValuePair.class).json().is("'X3: 3'");
		assertObject(BasicNameValuePair.cast(x4)).isType(NameValuePair.class).json().is("'X4: 4'");
		assertObject(BasicNameValuePair.cast(x5)).isType(NameValuePair.class).json().is("'X5=5'");
		assertObject(BasicNameValuePair.cast(x6)).isType(NameValuePair.class).json().is("{name:'X6',value:'6'}");
		assertObject(BasicNameValuePair.cast(x7)).isType(NameValuePair.class).json().is("'X7=7'");
		assertObject(BasicNameValuePair.cast(x8)).isType(NameValuePair.class).json().is("'X8: 8'");

		assertThrown(()->BasicNameValuePair.cast("X")).is("Object of type java.lang.String could not be converted to a NameValuePair.");
		assertThrown(()->BasicNameValuePair.cast(null)).is("Object of type null could not be converted to a NameValuePair.");

		assertTrue(BasicNameValuePair.canCast(x1));
		assertTrue(BasicNameValuePair.canCast(x2));
		assertTrue(BasicNameValuePair.canCast(x3));
		assertTrue(BasicNameValuePair.canCast(x4));
		assertTrue(BasicNameValuePair.canCast(x5));
		assertTrue(BasicNameValuePair.canCast(x6));
		assertTrue(BasicNameValuePair.canCast(x7));
		assertTrue(BasicNameValuePair.canCast(x8));

		assertFalse(BasicNameValuePair.canCast("X"));
		assertFalse(BasicNameValuePair.canCast(null));
	}

	@Test
	public void a04_asHeader() {
		BasicNameValuePair x = pair("X1","1");
		assertObject(x.asHeader()).isType(Header.class).json().is("'X1: 1'");
	}

	@Test
	public void a05_assertions() {
		BasicNameValuePair x = pair("X1","1");
		x.assertName().is("X1").assertValue().is("1");
	}

	@Test
	public void a06_hashSet() {
		Set<BasicNameValuePair> x = ASet.of(pair("X1","1"),pair("X1","1"),pair("X1","2"),pair("X2","1"),pair("X2","2"),pair("X3","3"),null);
		assertObject(x).json().stderr().is("['X1=1','X1=2','X2=1','X2=2','X3=3',null]");
		assertFalse(pair("X1","1").equals(null));
		assertFalse(pair("X1","1").equals(pair("X1","2")));
		assertFalse(pair("X1","1").equals(pair("X2","1")));
		assertTrue(pair("X1","1").equals(pair("X1","1")));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Utility methods
	//------------------------------------------------------------------------------------------------------------------

	private BasicHeader header(String name, Object val) {
		return BasicHeader.of(name, val);
	}

	private BasicNameValuePair pair(String name, Supplier<?> val) {
		return BasicNameValuePair.of(name, val);
	}

	private BasicNameValuePair pair(String name, Object val) {
		return BasicNameValuePair.of(name, val);
	}
}
