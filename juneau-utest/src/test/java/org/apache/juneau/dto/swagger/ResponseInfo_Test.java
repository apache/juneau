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

import static org.apache.juneau.bean.swagger.SwaggerBuilder.*;
import static org.apache.juneau.internal.CollectionUtils.*;
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
		ResponseInfo t = new ResponseInfo();
		assertEquals("foo", t.setDescription("foo").getDescription());
		assertNull(t.setDescription(null).getDescription());
		assertJson(t.setSchema(schemaInfo().setTitle("foo")).getSchema(), "{title:'foo'}");
		assertJson(t.setHeaders(map("foo",headerInfo("bar"))).getHeaders(), "{foo:{type:'bar'}}");
		assertType(HeaderInfo.class, t.setHeaders(map("foo",headerInfo("bar"))).getHeaders().get("foo"));
		assertJson(t.setHeaders(map()).getHeaders(), "{}");
		assertNull(t.setHeaders((Map<String,HeaderInfo>)null).getHeaders());
		assertJson(t.setExamples(map("foo","bar","baz",alist("qux"))).getExamples(), "{foo:'bar',baz:['qux']}");
		assertJson(t.setExamples(map()).getExamples(), "{}");
		assertNull(t.setExamples((Map<String,Object>)null).getExamples());
		assertJson(t.setExamples(map("foo","bar","baz",alist("qux"))).getExamples(), "{foo:'bar',baz:['qux']}");
		assertJson(t.setExamples(map()).addExample("text/a", "a").addExample("text/b", null).addExample(null, "c").getExamples(), "{'text/a':'a','text/b':null,null:'c'}");
	}

	/**
	 * Test method for {@link ResponseInfo#set(java.lang.String, java.lang.Object)}.
	 */
	@Test void b01_set() throws Exception {
		ResponseInfo t = new ResponseInfo();

		t
			.set("description", "a")
			.set("examples", map("foo","bar","baz",alist("qux")))
			.set("headers", map("a", headerInfo("a1")))
			.set("schema", schemaInfo().setType("d"))
			.set("$ref", "ref");

		assertJson(t, "{description:'a',schema:{type:'d'},headers:{a:{type:'a1'}},examples:{foo:'bar',baz:['qux']},'$ref':'ref'}");

		t
			.set("description", "a")
			.set("examples", "{foo:'bar',baz:['qux']}")
			.set("headers", "{a:{type:'a1'}}")
			.set("schema", "{type:'d'}")
			.set("$ref", "ref");

		assertJson(t, "{description:'a',schema:{type:'d'},headers:{a:{type:'a1'}},examples:{foo:'bar',baz:['qux']},'$ref':'ref'}");

		t
			.set("description", new StringBuilder("a"))
			.set("examples", new StringBuilder("{foo:'bar',baz:['qux']}"))
			.set("headers", new StringBuilder("{a:{type:'a1'}}"))
			.set("schema", new StringBuilder("{type:'d'}"))
			.set("$ref", new StringBuilder("ref"));

		assertJson(t, "{description:'a',schema:{type:'d'},headers:{a:{type:'a1'}},examples:{foo:'bar',baz:['qux']},'$ref':'ref'}");

		assertEquals("a", t.get("description", String.class));
		assertEquals("{foo:'bar',baz:['qux']}", t.get("examples", String.class));
		assertEquals("{a:{type:'a1'}}", t.get("headers", String.class));
		assertEquals("{type:'d'}", t.get("schema", String.class));
		assertEquals("ref", t.get("$ref", String.class));

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

		String s = "{description:'a',schema:{type:'d'},headers:{a:{type:'a1'}},examples:{foo:'bar',baz:['qux']},'$ref':'ref'}";
		assertJson(JsonParser.DEFAULT.parse(s, ResponseInfo.class), s);
	}

	@Test void b02_copy() {
		ResponseInfo t = new ResponseInfo();

		t = t.copy();

		assertJson(t, "{}");

		t
			.set("description", "a")
			.set("examples", map("foo","bar","baz",alist("qux")))
			.set("headers", map("a", headerInfo("a1")))
			.set("schema", schemaInfo().setType("d"))
			.set("$ref", "ref")
			.copy();

		assertJson(t, "{description:'a',schema:{type:'d'},headers:{a:{type:'a1'}},examples:{foo:'bar',baz:['qux']},'$ref':'ref'}");
	}

	@Test void b03_keySet() {
		ResponseInfo t = new ResponseInfo();

		assertJson(t.keySet(), "[]");

		t
			.set("description", "a")
			.set("examples", map("foo","bar","baz",alist("qux")))
			.set("headers", map("a", headerInfo("a1")))
			.set("schema", schemaInfo().setType("d"))
			.set("$ref", "ref");

		assertJson(t.keySet(), "['description','examples','headers','schema','$ref']");
	}
}