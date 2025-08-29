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

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.bean.swagger.SwaggerBuilder.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.swagger.*;
import org.apache.juneau.json.*;
import org.junit.jupiter.api.*;

/**
 * Testcase for {@link ResponseInfo}.
 */
class ResponseInfo_Test extends SimpleTestBase {

	/**
	 * Test method for getters and setters.
	 */
	@Test void a01_gettersAndSetters() {
		var t = new ResponseInfo();

		// General
		assertBean(
			t.setDescription("a").setSchema(schemaInfo().setTitle("b")).setHeaders(map("c",headerInfo("d"))).setExamples(map("e","f","g",alist("h"))),
			"description,schema{title},headers{c{type}},examples",
			"a,{b},{{d}},{e=f,g=[h]}"
		);

		// Edge cases for collections and nulls.
		assertNull(t.setDescription(null).getDescription());
		assertEmpty(t.setHeaders(map()).getHeaders());
		assertNull(t.setHeaders((Map<String,HeaderInfo>)null).getHeaders());
		assertEmpty(t.setExamples(map()).getExamples());
		assertNull(t.setExamples((Map<String,Object>)null).getExamples());

		// Examples addExample method.
		assertMap(
			t.setExamples(map()).addExample("text/a", "a").addExample("text/b", null).addExample(null, "c").getExamples(),
			"text/a,text/b,<<<NULL>>>",
			"a,null,c"
		);
	}

	/**
	 * Test method for {@link ResponseInfo#set(java.lang.String, java.lang.Object)}.
	 */
	@Test void b01_set() throws Exception {
		var t = new ResponseInfo();

		t
			.set("description", "a")
			.set("examples", map("foo","bar","baz",alist("qux")))
			.set("headers", map("a", headerInfo("a1")))
			.set("schema", schemaInfo().setType("d"))
			.set("$ref", "ref");

		// Comprehensive object state validation
		assertBean(t,
			"description,schema{type},headers{a{type}},examples,$ref",
			"a,{d},{{a1}},{foo=bar,baz=[qux]},ref");

		t
			.set("description", "a")
			.set("examples", "{foo:'bar',baz:['qux']}")
			.set("headers", "{a:{type:'a1'}}")
			.set("schema", "{type:'d'}")
			.set("$ref", "ref");

		assertBean(t,
			"description,schema{type},headers{a{type}},examples,$ref",
			"a,{d},{{a1}},{foo=bar,baz=[qux]},ref");

		t
			.set("description", new StringBuilder("a"))
			.set("examples", new StringBuilder("{foo:'bar',baz:['qux']}"))
			.set("headers", new StringBuilder("{a:{type:'a1'}}"))
			.set("schema", new StringBuilder("{type:'d'}"))
			.set("$ref", new StringBuilder("ref"));

		assertBean(t,
			"description,schema{type},headers{a{type}},examples,$ref",
			"a,{d},{{a1}},{foo=bar,baz=[qux]},ref");

		assertMapped(t, (obj,prop) -> obj.get(prop, String.class),
			"description,examples,headers,schema,$ref",
			"a,{foo:'bar',baz:['qux']},{a:{type:'a1'}},{type:'d'},ref");

		assertType(String.class, t.get("description", Object.class));
		assertType(Map.class, t.get("examples", Object.class));
		assertType(Map.class, t.get("headers", Object.class));
		assertType(HeaderInfo.class, t.get("headers", Map.class).values().iterator().next());
		assertType(SchemaInfo.class, t.get("schema", Object.class));
		assertType(StringBuilder.class, t.get("$ref", Object.class));

		t.set("null", null).set(null, "null");
		assertNull(t.get("null", Object.class));
		assertNull(t.get(null, Object.class));
		assertNull(t.get("foo", Object.class));

		var s = "{description:'a',schema:{type:'d'},headers:{a:{type:'a1'}},examples:{foo:'bar',baz:['qux']},'$ref':'ref'}";
		assertJson(JsonParser.DEFAULT.parse(s, ResponseInfo.class), s);
	}

	@Test void b02_copy() {
		var t = new ResponseInfo();

		t = t.copy();

		assertJson(t, "{}");

		t
			.set("description", "a")
			.set("examples", map("foo","bar","baz",alist("qux")))
			.set("headers", map("a", headerInfo("a1")))
			.set("schema", schemaInfo().setType("d"))
			.set("$ref", "ref")
			.copy();

		assertBean(t,
			"description,schema{type},headers{a{type}},examples,$ref",
			"a,{d},{{a1}},{foo=bar,baz=[qux]},ref");
	}

	@Test void b03_keySet() {
		var t = new ResponseInfo();

		assertEmpty(t.keySet());

		t
			.set("description", "a")
			.set("examples", map("foo","bar","baz",alist("qux")))
			.set("headers", map("a", headerInfo("a1")))
			.set("schema", schemaInfo().setType("d"))
			.set("$ref", "ref");

		assertSet(t.keySet(), "description,examples,headers,schema,$ref");
	}
}