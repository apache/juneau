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
import static org.junit.runners.MethodSorters.*;

import java.net.*;

import org.apache.juneau.json.*;
import org.junit.*;

/**
 * Testcase for {@link ExternalDocumentation}.
 */
@FixMethodOrder(NAME_ASCENDING)
public class ExternalDocumentation_Test {

	/**
	 * Test method for getters and setters.
	 */
	@Test
	public void a01_gettersAndSetters() {
		ExternalDocumentation t = new ExternalDocumentation();
		assertString(t.setDescription("foo").getDescription()).is("foo");
		assertString(t.setDescription(null).getDescription()).isNull();
		assertString(t.setUrl(URI.create("http://bar")).getUrl()).is("http://bar");
	}

	/**
	 * Test method for {@link ExternalDocumentation#set(java.lang.String, java.lang.Object)}.
	 */
	@Test
	public void b01_set() throws Exception {
		ExternalDocumentation t = new ExternalDocumentation();

		t
			.set("description", "foo")
			.set("url", "bar")
			.set("$ref", "baz");

		assertObject(t).asJson().is("{description:'foo',url:'bar','$ref':'baz'}");

		t
			.set("description", new StringBuilder("foo"))
			.set("url", new StringBuilder("bar"))
			.set("$ref", new StringBuilder("baz"));

		assertObject(t).asJson().is("{description:'foo',url:'bar','$ref':'baz'}");

		assertObject(t.get("description", String.class)).isType(String.class).is("foo");
		assertObject(t.get("url", URI.class)).isType(URI.class).asString().is("bar");
		assertObject(t.get("$ref", String.class)).isType(String.class).is("baz");

		t.set("null", null).set(null, "null");
		assertObject(t.get("null", Object.class)).isNull();
		assertObject(t.get(null, Object.class)).isNull();
		assertObject(t.get("foo", Object.class)).isNull();

		assertObject(JsonParser.DEFAULT.parse("{description:'foo',url:'bar','$ref':'baz'}", ExternalDocumentation.class)).asJson().is("{description:'foo',url:'bar','$ref':'baz'}");
	}

	@Test
	public void b02_copy() throws Exception {
		ExternalDocumentation t = new ExternalDocumentation();

		t = t.copy();

		assertObject(t).asJson().is("{}");

		t
			.set("description", "foo")
			.set("url", "bar")
			.set("$ref", "baz")
			.copy();

		assertObject(t).asJson().is("{description:'foo',url:'bar','$ref':'baz'}");
	}

	@Test
	public void b03_keySet() throws Exception {
		ExternalDocumentation t = new ExternalDocumentation();

		assertObject(t.keySet()).asJson().is("[]");

		t
			.set("description", "foo")
			.set("url", "bar")
			.set("$ref", "baz");

		assertObject(t.keySet()).asJson().is("['description','url','$ref']");
	}
}
