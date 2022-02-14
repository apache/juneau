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
import static org.apache.juneau.dto.swagger.SwaggerBuilder.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.json.*;
import org.junit.*;

/**
 * Testcase for {@link ResponseInfo}.
 */
@FixMethodOrder(NAME_ASCENDING)
public class ResponseInfo_Test {

	/**
	 * Test method for {@link ResponseInfo#description(java.lang.Object)}.
	 */
	@Test
	public void a01_description() {
		ResponseInfo t = new ResponseInfo();

		t.description("foo");
		assertString(t.description()).is("foo");

		t.description(null);
		assertString(t.description()).isNull();
	}

	/**
	 * Test method for {@link ResponseInfo#schema(java.lang.Object)}.
	 */
	@Test
	public void a02_schema() {
		ResponseInfo t = new ResponseInfo();

		t.schema(schemaInfo().title("foo"));
		assertOptional(t.schema()).isType(SchemaInfo.class).asJson().is("{title:'foo'}");

		t.schema("{title:'foo'}");
		assertOptional(t.schema()).isType(SchemaInfo.class).asJson().is("{title:'foo'}");

		t.schema((String)null);
		assertOptional(t.schema()).isNull();
	}

	/**
	 * Test method for {@link ResponseInfo#setHeaders(java.util.Map)}.
	 */
	@Test
	public void a03_headers() {
		ResponseInfo t = new ResponseInfo();

		t.headers(map("foo",headerInfo("bar")));
		assertOptional(t.headers()).isType(Map.class).asJson().is("{foo:{type:'bar'}}");
		assertObject(t.headers().get().get("foo")).isType(HeaderInfo.class);

		t.headers(map());
		assertOptional(t.headers()).isType(Map.class).asJson().is("{}");

		t.headers((Map<String,HeaderInfo>)null);
		assertOptional(t.headers()).isNull();

		t.addHeaders(map("foo",headerInfo("bar")));
		assertOptional(t.headers()).isType(Map.class).asJson().is("{foo:{type:'bar'}}");
		assertObject(t.headers().get().get("foo")).isType(HeaderInfo.class);

		t.addHeaders(map());
		assertOptional(t.headers()).isType(Map.class).asJson().is("{foo:{type:'bar'}}");
		assertObject(t.headers().get().get("foo")).isType(HeaderInfo.class);

		t.addHeaders(null);
		assertOptional(t.headers()).isType(Map.class).asJson().is("{foo:{type:'bar'}}");
		assertObject(t.headers().get().get("foo")).isType(HeaderInfo.class);
	}

	/**
	 * Test method for {@link ResponseInfo#setExamples(java.util.Map)}.
	 */
	@Test
	public void a04_examples() {
		ResponseInfo t = new ResponseInfo();

		t.examples(map("foo","bar","baz",list("qux")));
		assertOptional(t.examples()).isType(Map.class).asJson().is("{foo:'bar',baz:['qux']}");

		t.examples(map());
		assertOptional(t.examples()).isType(Map.class).asJson().is("{}");

		t.examples((Map<String,Object>)null);
		assertOptional(t.examples()).isNull();

		t.addExamples(map("foo","bar","baz",list("qux")));
		assertOptional(t.examples()).isType(Map.class).asJson().is("{foo:'bar',baz:['qux']}");

		t.addExamples(map());
		assertOptional(t.examples()).isType(Map.class).asJson().is("{foo:'bar',baz:['qux']}");

		t.addExamples(null);
		assertOptional(t.examples()).isType(Map.class).asJson().is("{foo:'bar',baz:['qux']}");

		t.examples(map());
		t.example("text/a", "a");
		t.example("text/b", null);
		t.example(null, "c");

		assertOptional(t.examples()).asJson().is("{'text/a':'a','text/b':null,null:'c'}");
	}

	/**
	 * Test method for {@link ResponseInfo#set(java.lang.String, java.lang.Object)}.
	 */
	@Test
	public void b01_set() throws Exception {
		ResponseInfo t = new ResponseInfo();

		t
			.set("description", "a")
			.set("examples", map("foo","bar","baz",list("qux")))
			.set("headers", map("a", headerInfo("a1")))
			.set("schema", schemaInfo().type("d"))
			.set("$ref", "ref");

		assertObject(t).asJson().is("{description:'a',schema:{type:'d'},headers:{a:{type:'a1'}},examples:{foo:'bar',baz:['qux']},'$ref':'ref'}");

		t
			.set("description", "a")
			.set("examples", "{foo:'bar',baz:['qux']}")
			.set("headers", "{a:{type:'a1'}}")
			.set("schema", "{type:'d'}")
			.set("$ref", "ref");

		assertObject(t).asJson().is("{description:'a',schema:{type:'d'},headers:{a:{type:'a1'}},examples:{foo:'bar',baz:['qux']},'$ref':'ref'}");

		t
			.set("description", new StringBuilder("a"))
			.set("examples", new StringBuilder("{foo:'bar',baz:['qux']}"))
			.set("headers", new StringBuilder("{a:{type:'a1'}}"))
			.set("schema", new StringBuilder("{type:'d'}"))
			.set("$ref", new StringBuilder("ref"));

		assertObject(t).asJson().is("{description:'a',schema:{type:'d'},headers:{a:{type:'a1'}},examples:{foo:'bar',baz:['qux']},'$ref':'ref'}");

		assertString(t.get("description", String.class)).is("a");
		assertString(t.get("examples", String.class)).is("{foo:'bar',baz:['qux']}");
		assertString(t.get("headers", String.class)).is("{a:{type:'a1'}}");
		assertString(t.get("schema", String.class)).is("{type:'d'}");
		assertString(t.get("$ref", String.class)).is("ref");

		assertObject(t.get("description", Object.class)).isType(String.class);
		assertObject(t.get("examples", Object.class)).isType(Map.class);
		assertObject(t.get("headers", Object.class)).isType(Map.class);
		assertObject(t.get("headers", Map.class).values().iterator().next()).isType(HeaderInfo.class);
		assertObject(t.get("schema", Object.class)).isType(SchemaInfo.class);
		assertObject(t.get("$ref", Object.class)).isType(StringBuilder.class);

		t.set("null", null).set(null, "null");
		assertNull(t.get("null", Object.class));
		assertNull(t.get(null, Object.class));
		assertNull(t.get("foo", Object.class));

		String s = "{description:'a',schema:{type:'d'},headers:{a:{type:'a1'}},examples:{foo:'bar',baz:['qux']},'$ref':'ref'}";
		assertObject(JsonParser.DEFAULT.parse(s, ResponseInfo.class)).asJson().is(s);
	}

	@Test
	public void b02_copy() throws Exception {
		ResponseInfo t = new ResponseInfo();

		t = t.copy();

		assertObject(t).asJson().is("{}");

		t
			.set("description", "a")
			.set("examples", map("foo","bar","baz",list("qux")))
			.set("headers", map("a", headerInfo("a1")))
			.set("schema", schemaInfo().type("d"))
			.set("$ref", "ref")
			.copy();

		assertObject(t).asJson().is("{description:'a',schema:{type:'d'},headers:{a:{type:'a1'}},examples:{foo:'bar',baz:['qux']},'$ref':'ref'}");
	}

	@Test
	public void b03_keySet() throws Exception {
		ResponseInfo t = new ResponseInfo();

		assertObject(t.keySet()).asJson().is("[]");

		t
			.set("description", "a")
			.set("examples", map("foo","bar","baz",list("qux")))
			.set("headers", map("a", headerInfo("a1")))
			.set("schema", schemaInfo().type("d"))
			.set("$ref", "ref");

		assertObject(t.keySet()).asJson().is("['description','examples','headers','schema','$ref']");
	}
}
