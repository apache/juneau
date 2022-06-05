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

import java.util.function.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.http.header.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class BasicHeader_Test {

	@Test
	public void a01_ofPair() {
		Header x = stringHeader("Foo:bar");
		assertEquals("Foo", x.getName());
		assertEquals("bar", x.getValue());

		x = stringHeader(" Foo : bar ");
		assertEquals("Foo", x.getName());
		assertEquals("bar", x.getValue());

		x = stringHeader(" Foo : bar : baz ");
		assertEquals("Foo", x.getName());
		assertEquals("bar : baz", x.getValue());

		x = stringHeader("Foo");
		assertEquals("Foo", x.getName());
		assertEquals("", x.getValue());

		assertNull(stringHeader((String)null));
	}

	@Test
	public void a02_of() {
		BasicHeader x;
		x = header("Foo","bar");
		assertObject(x).asJson().is("'Foo: bar'");
		x = header("Foo",()->"bar");
		assertObject(x).asJson().is("'Foo: bar'");
	}

	@Test
	public void a05_assertions() {
		BasicHeader x = header("X1","1");
		x.assertName().is("X1").assertStringValue().is("1");
	}

	@Test
	public void a07_eqIC() {
		BasicHeader x = header("X1","1");
		assertTrue(x.equalsIgnoreCase("1"));
		assertFalse(x.equalsIgnoreCase("2"));
		assertFalse(x.equalsIgnoreCase(null));
	}

	@Test
	public void a08_getElements() {
		Value<Integer> m = Value.of(1);
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
		assertEquals("Value(1)", x[0].getName());
		m.set(2);
		x = h2.getElements();
		assertEquals(1, x.length);
		assertEquals("Value(2)", x[0].getName());

		x = h3.getElements();
		assertEquals(0, x.length);
	}

	@SuppressWarnings("unlikely-arg-type")
	@Test
	public void a09_equals() {
		BasicHeader h1 = header("Foo","bar"), h2 = header("Foo","bar"), h3 = header("Bar","bar"), h4 = header("Foo","baz");
		assertInteger(h1.hashCode()).isExists();
		assertBoolean(h1.equals(h2)).isTrue();
		assertBoolean(h1.equals(h3)).isFalse();
		assertBoolean(h1.equals(h4)).isFalse();
		assertBoolean(h1.equals("foo")).isFalse();

	}

	//------------------------------------------------------------------------------------------------------------------
	// Utility methods
	//------------------------------------------------------------------------------------------------------------------

	private BasicHeader header(String name, Object val) {
		return basicHeader(name, val);
	}

	private BasicHeader header(String name, Supplier<?> val) {
		return basicHeader(name, val);
	}
}
