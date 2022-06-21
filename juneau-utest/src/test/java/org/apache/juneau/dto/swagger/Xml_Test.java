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
package org.apache.juneau.dto.swagger;

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.json.*;
import org.junit.*;

/**
 * Testcase for {@link Xml}.
 */
@FixMethodOrder(NAME_ASCENDING)
public class Xml_Test {

	/**
	 * Test method for getters and setters.
	 */
	@Test
	public void a01_gettersAndSetters() {
		Xml t = new Xml();
		assertString(t.setName("foo").getName()).is("foo");
		assertString(t.setName(null).getName()).isNull();
		assertString(t.setNamespace("foo").getNamespace()).is("foo");
		assertString(t.setNamespace(null).getNamespace()).isNull();
		assertString(t.setPrefix("foo").getPrefix()).is("foo");
		assertString(t.setPrefix(null).getPrefix()).isNull();
		assertObject(t.setAttribute(true).getAttribute()).isType(Boolean.class).is(true);
		assertObject(t.setWrapped(true).getWrapped()).isType(Boolean.class).is(true);
	}

	/**
	 * Test method for {@link Xml#set(java.lang.String, java.lang.Object)}.
	 */
	@Test
	public void b01_set() throws Exception {
		Xml t = new Xml();

		t
			.set("attribute", true)
			.set("name", "a")
			.set("namespace", "b")
			.set("prefix", "c")
			.set("wrapped", true)
			.set("$ref", "ref");

		assertObject(t).asJson().is("{name:'a',namespace:'b',prefix:'c',attribute:true,wrapped:true,'$ref':'ref'}");

		t
			.set("attribute", "true")
			.set("name", "a")
			.set("namespace", "b")
			.set("prefix", "c")
			.set("wrapped", "true")
			.set("$ref", "ref");

		assertObject(t).asJson().is("{name:'a',namespace:'b',prefix:'c',attribute:true,wrapped:true,'$ref':'ref'}");

		t
			.set("attribute", new StringBuilder("true"))
			.set("name", new StringBuilder("a"))
			.set("namespace", new StringBuilder("b"))
			.set("prefix", new StringBuilder("c"))
			.set("wrapped", new StringBuilder("true"))
			.set("$ref", new StringBuilder("ref"));

		assertObject(t).asJson().is("{name:'a',namespace:'b',prefix:'c',attribute:true,wrapped:true,'$ref':'ref'}");

		assertString(t.get("attribute", String.class)).is("true");
		assertString(t.get("name", String.class)).is("a");
		assertString(t.get("namespace", String.class)).is("b");
		assertString(t.get("prefix", String.class)).is("c");
		assertString(t.get("wrapped", String.class)).is("true");
		assertString(t.get("$ref", String.class)).is("ref");

		assertObject(t.get("attribute", Object.class)).isType(Boolean.class);
		assertObject(t.get("name", Object.class)).isType(String.class);
		assertObject(t.get("namespace", Object.class)).isType(String.class);
		assertObject(t.get("prefix", Object.class)).isType(String.class);
		assertObject(t.get("wrapped", Object.class)).isType(Boolean.class);
		assertObject(t.get("$ref", Object.class)).isType(StringBuilder.class);

		t.set("null", null).set(null, "null");
		assertNull(t.get("null", Object.class));
		assertNull(t.get(null, Object.class));
		assertNull(t.get("foo", Object.class));

		String s = "{name:'a',namespace:'b',prefix:'c',attribute:true,wrapped:true,'$ref':'ref'}";
		assertObject(JsonParser.DEFAULT.parse(s, Xml.class)).asJson().is(s);
	}

	@Test
	public void b02_copy() throws Exception {
		Xml t = new Xml();

		t = t.copy();

		assertObject(t).asJson().is("{}");

		t
			.set("attribute", true)
			.set("name", "a")
			.set("namespace", "b")
			.set("prefix", "c")
			.set("wrapped", true)
			.set("$ref", "ref")
			.copy();

		assertObject(t).asJson().is("{name:'a',namespace:'b',prefix:'c',attribute:true,wrapped:true,'$ref':'ref'}");
	}

	@Test
	public void b03_keySet() throws Exception {
		Xml t = new Xml();

		assertObject(t.keySet()).asJson().is("[]");

		t
			.set("attribute", true)
			.set("name", "a")
			.set("namespace", "b")
			.set("prefix", "c")
			.set("wrapped", true)
			.set("$ref", "ref");

		assertObject(t.keySet()).asJson().is("['attribute','name','namespace','prefix','wrapped','$ref']");
	}
}
