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
import org.apache.juneau.common.internal.*;
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

		// Basic property setters
		assertBean(
			t.setDescription("a")
				.setExamples(map("b1","b2","b3",alist("b4")))
				.setHeaders(map("c1",headerInfo("c2")))
				.setSchema(schemaInfo().setTitle("d")),
			"description,examples,headers{c1{type}},schema{title}",
			"a,{b1=b2,b3=[b4]},{{c2}},{d}"
		);

		// Null values
		assertBean(
			t.setDescription(null).setExamples((Map<String,Object>)null).setHeaders((Map<String,HeaderInfo>)null),
			"description,examples,headers",
			"null,null,null"
		);

		// Other methods - empty collections
		assertBean(
			t.setExamples(map()).setHeaders(map()),
			"examples,headers",
			"{},{}"
		);
		assertMap(
			t.setExamples(map()).addExample("text/a", "a").addExample("text/b", null).addExample(null, "c").getExamples(),
			"text/a,text/b,<NULL>",
			"a,null,c"
		);
	}

	/**
	 * Test method for {@link ResponseInfo#set(java.lang.String, java.lang.Object)}.
	 */
	@Test void b01_set() {
		var t = new ResponseInfo();

		t
			.set("description", "a")
			.set("examples", map("b1","b2","b3",alist("b4")))
			.set("headers", map("c1", headerInfo("c2")))
			.set("schema", schemaInfo().setType("d"))
			.set("$ref", "e");

		// Comprehensive object state validation
		assertBean(t,
			"description,schema{type},headers{c1{type}},examples,$ref",
			"a,{d},{{c2}},{b1=b2,b3=[b4]},e");

		t
			.set("description", "a")
			.set("examples", "{b1:'b2',b3:['b4']}")
			.set("headers", "{c1:{type:'c2'}}")
			.set("schema", "{type:'d'}")
			.set("$ref", "e");

		assertBean(t,
			"description,examples,headers{c1{type}},schema{type},$ref",
			"a,{b1=b2,b3=[b4]},{{c2}},{d},e");

		t
			.set("description", Utils.sb("a"))
			.set("examples", Utils.sb("{b1:'b2',b3:['b4']}"))
			.set("headers", Utils.sb("{c1:{type:'c2'}}"))
			.set("schema", Utils.sb("{type:'d'}"))
			.set("$ref", Utils.sb("e"));

		assertBean(t,
			"description,examples,headers{c1{type}},schema{type},$ref",
			"a,{b1=b2,b3=[b4]},{{c2}},{d},e");

		assertMapped(t, (obj,prop) -> obj.get(prop, String.class),
			"description,examples,headers,schema,$ref",
			"a,{b1:'b2',b3:['b4']},{c1:{type:'c2'}},{type:'d'},e");

		assertMapped(t, (obj,prop) -> obj.get(prop, Object.class).getClass().getSimpleName(),
			"description,examples,headers,schema,$ref",
			"String,LinkedHashMap,LinkedHashMap,SchemaInfo,StringBuilder");

		assertType(HeaderInfo.class, t.get("headers", Map.class).values().iterator().next());

		t.set("null", null).set(null, "null");
		assertNull(t.get("null", Object.class));
		assertNull(t.get(null, Object.class));
		assertNull(t.get("foo", Object.class));
	}

	@Test void b02_roundTripJson() {
		var s = "{description:'a',schema:{type:'d'},headers:{a:{type:'a1'}},examples:{foo:'bar',baz:['qux']},'$ref':'ref'}";  // Order is determined by @Bean annotation.
		assertJson(s, JsonParser.DEFAULT.parse(s, ResponseInfo.class));
	}

	@Test void b03_copy() {
		var t = new ResponseInfo();

		t = t.copy();

		assertJson("{}", t);

		t
			.set("description", "a")
			.set("examples", map("b1","b2","b3",alist("b4")))
			.set("headers", map("c1", headerInfo("c2")))
			.set("schema", schemaInfo().setType("d"))
			.set("$ref", "e")
			.copy();

		assertBean(t,
			"description,examples,headers{c1{type}},schema{type},$ref",
			"a,{b1=b2,b3=[b4]},{{c2}},{d},e");
	}

	@Test void b04_keySet() {
		var t = new ResponseInfo();

		assertEmpty(t.keySet());

		t
			.set("description", "a")
			.set("examples", map("b1","b2","b3",alist("b4")))
			.set("headers", map("c1", headerInfo("c2")))
			.set("schema", schemaInfo().setType("d"))
			.set("$ref", "e");

		assertSet(t.keySet(), "description", "examples", "headers", "schema", "$ref");
	}
}