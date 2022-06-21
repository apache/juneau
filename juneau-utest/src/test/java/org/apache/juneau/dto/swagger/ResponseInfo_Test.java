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
	 * Test method for getters and setters.
	 */
	@Test
	public void a01_gettersAndSetters() {
		ResponseInfo t = new ResponseInfo();
		assertString(t.setDescription("foo").getDescription()).is("foo");
		assertString(t.setDescription(null).getDescription()).isNull();
		assertObject(t.setSchema(schemaInfo().setTitle("foo")).getSchema()).isType(SchemaInfo.class).asJson().is("{title:'foo'}");
		assertObject(t.setHeaders(map("foo",headerInfo("bar"))).getHeaders()).isType(Map.class).asJson().is("{foo:{type:'bar'}}");
		assertObject(t.setHeaders(map("foo",headerInfo("bar"))).getHeaders().get("foo")).isType(HeaderInfo.class);
		assertObject(t.setHeaders(map()).getHeaders()).isType(Map.class).asJson().is("{}");
		assertObject(t.setHeaders((Map<String,HeaderInfo>)null).getHeaders()).isNull();
		assertObject(t.setExamples(map("foo","bar","baz",alist("qux"))).getExamples()).isType(Map.class).asJson().is("{foo:'bar',baz:['qux']}");
		assertObject(t.setExamples(map()).getExamples()).isType(Map.class).asJson().is("{}");
		assertObject(t.setExamples((Map<String,Object>)null).getExamples()).isNull();
		assertObject(t.setExamples(map("foo","bar","baz",alist("qux"))).getExamples()).isType(Map.class).asJson().is("{foo:'bar',baz:['qux']}");
		assertObject(t.setExamples(map()).addExample("text/a", "a").addExample("text/b", null).addExample(null, "c").getExamples()).asJson().is("{'text/a':'a','text/b':null,null:'c'}");
	}

	/**
	 * Test method for {@link ResponseInfo#set(java.lang.String, java.lang.Object)}.
	 */
	@Test
	public void b01_set() throws Exception {
		ResponseInfo t = new ResponseInfo();

		t
			.set("description", "a")
			.set("examples", map("foo","bar","baz",alist("qux")))
			.set("headers", map("a", headerInfo("a1")))
			.set("schema", schemaInfo().setType("d"))
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
			.set("examples", map("foo","bar","baz",alist("qux")))
			.set("headers", map("a", headerInfo("a1")))
			.set("schema", schemaInfo().setType("d"))
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
			.set("examples", map("foo","bar","baz",alist("qux")))
			.set("headers", map("a", headerInfo("a1")))
			.set("schema", schemaInfo().setType("d"))
			.set("$ref", "ref");

		assertObject(t.keySet()).asJson().is("['description','examples','headers','schema','$ref']");
	}
}
