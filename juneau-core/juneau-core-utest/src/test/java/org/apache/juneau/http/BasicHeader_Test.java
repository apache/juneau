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
import org.apache.juneau.utils.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class BasicHeader_Test {

	@Test
	public void a01_ofPair() {
		BasicHeader x = BasicHeader.ofPair("Foo:bar");
		assertEquals("Foo", x.getName());
		assertEquals("bar", x.getValue());

		x = BasicHeader.ofPair(" Foo : bar ");
		assertEquals("Foo", x.getName());
		assertEquals("bar", x.getValue());

		x = BasicHeader.ofPair(" Foo : bar : baz ");
		assertEquals("Foo", x.getName());
		assertEquals("bar : baz", x.getValue());

		x = BasicHeader.ofPair("Foo");
		assertEquals("Foo", x.getName());
		assertEquals("", x.getValue());

		assertNull(BasicHeader.ofPair((String)null));
	}

	@Test
	public void a02_of() {
		BasicHeader x;
		x = header("Foo","bar");
		assertObject(x).json().is("'Foo: bar'");
		x = header("Foo",()->"bar");
		assertObject(x).json().is("'Foo: bar'");
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

		assertObject(BasicHeader.cast(x1)).isType(Header.class).json().is("'X1: 1'");
		assertObject(BasicHeader.cast(x2)).isType(Header.class).json().is("'X2: 2'");
		assertObject(BasicHeader.cast(x3)).isType(Header.class).json().is("'X3: 3'");
		assertObject(BasicHeader.cast(x4)).isType(Header.class).json().is("'X4: 4'");
		assertObject(BasicHeader.cast(x5)).isType(Header.class).json().is("'X5: 5'");
		assertObject(BasicHeader.cast(x6)).isType(Header.class).json().is("'X6: 6'");
		assertObject(BasicHeader.cast(x7)).isType(Header.class).json().is("'X7: 7'");
		assertObject(BasicHeader.cast(x8)).isType(Header.class).json().is("'X8: 8'");

		assertThrown(()->BasicHeader.cast("X")).is("Object of type java.lang.String could not be converted to a Header.");
		assertThrown(()->BasicHeader.cast(null)).is("Object of type null could not be converted to a Header.");

		assertTrue(BasicHeader.canCast(x1));
		assertTrue(BasicHeader.canCast(x2));
		assertTrue(BasicHeader.canCast(x3));
		assertTrue(BasicHeader.canCast(x4));
		assertTrue(BasicHeader.canCast(x5));
		assertTrue(BasicHeader.canCast(x6));
		assertTrue(BasicHeader.canCast(x7));
		assertTrue(BasicHeader.canCast(x8));

		assertFalse(BasicHeader.canCast("X"));
		assertFalse(BasicHeader.canCast(null));
	}

	@Test
	public void a05_assertions() {
		BasicHeader x = header("X1","1");
		x.assertName().is("X1").assertValue().is("1");
	}

	@Test
	public void a06_hashSet() {
		Set<BasicHeader> x = ASet.of(header("X1","1"),header("X1","1"),header("X1","2"),header("X2","1"),header("X2","2"),header("X3","3"),null);
		assertObject(x).json().stderr().is("['X1: 1','X1: 2','X2: 1','X2: 2','X3: 3',null]");
		assertFalse(header("X1","1").equals(null));
		assertFalse(header("X1","1").equals(header("X1","2")));
		assertFalse(header("X1","1").equals(header("X2","1")));
		assertTrue(header("X1","1").equals(header("X1","1")));
	}

	@Test
	public void a07_eqIC() {
		BasicHeader x = header("X1","1");
		assertTrue(x.eqIC("1"));
		assertFalse(x.eqIC("2"));
		assertFalse(x.eqIC(null));
	}

	@Test
	public void a08_getElements() {
		Mutable<Integer> m = Mutable.of(1);
		Header h1 = header("X1","1");
		Header h2 = header("X2",()->m);
		Header h3 = header("X3",null);

		HeaderElement[] x;

		x = h1.getElements();
		assertEquals(1, x.length);
		assertEquals("1", x[0].getName());
		x = h1.getElements();
		assertEquals(1, x.length);
		assertEquals("1", x[0].getName());

		x = h2.getElements();
		assertEquals(1, x.length);
		assertEquals("1", x[0].getName());
		m.set(2);
		x = h2.getElements();
		assertEquals(1, x.length);
		assertEquals("2", x[0].getName());

		x = h3.getElements();
		assertEquals(0, x.length);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Utility methods
	//------------------------------------------------------------------------------------------------------------------

	private BasicHeader header(String name, Object val) {
		return BasicHeader.of(name, val);
	}

	private BasicHeader header(String name, Supplier<?> val) {
		return BasicHeader.of(name, val);
	}

	private BasicNameValuePair pair(String name, Object val) {
		return BasicNameValuePair.of(name, val);
	}
}
