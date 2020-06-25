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

import static org.apache.juneau.assertions.ObjectAssertion.*;
import static org.apache.juneau.dto.swagger.SwaggerBuilder.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.json.*;
import org.junit.*;

/**
 * Testcase for {@link ResponseInfo}.
 */
@FixMethodOrder(NAME_ASCENDING)
public class ResponseInfoTest {

	/**
	 * Test method for {@link ResponseInfo#description(java.lang.Object)}.
	 */
	@Test
	public void testDescription() {
		ResponseInfo t = new ResponseInfo();

		t.description("foo");
		assertEquals("foo", t.getDescription());

		t.description(new StringBuilder("foo"));
		assertEquals("foo", t.getDescription());
		assertObject(t.getDescription()).isType(String.class);

		t.description(null);
		assertNull(t.getDescription());
	}

	/**
	 * Test method for {@link ResponseInfo#schema(java.lang.Object)}.
	 */
	@Test
	public void testSchema() {
		ResponseInfo t = new ResponseInfo();

		t.schema(schemaInfo().title("foo"));
		assertObject(t.getSchema()).json().is("{title:'foo'}");

		t.schema("{title:'foo'}");
		assertObject(t.getSchema()).json().is("{title:'foo'}");
		assertObject(t.getSchema()).isType(SchemaInfo.class);

		t.schema(null);
		assertNull(t.getSchema());
	}

	/**
	 * Test method for {@link ResponseInfo#setHeaders(java.util.Map)}.
	 */
	@Test
	public void testSetHeaders() {
		ResponseInfo t = new ResponseInfo();

		t.setHeaders(AMap.of("foo",headerInfo("bar")));
		assertObject(t.getHeaders()).json().is("{foo:{type:'bar'}}");
		assertObject(t.getHeaders()).isType(Map.class);
		assertObject(t.getHeaders().get("foo")).isType(HeaderInfo.class);

		t.setHeaders(AMap.of());
		assertObject(t.getHeaders()).json().is("{}");
		assertObject(t.getHeaders()).isType(Map.class);

		t.setHeaders(null);
		assertNull(t.getExamples());
	}

	/**
	 * Test method for {@link ResponseInfo#addHeaders(java.util.Map)}.
	 */
	@Test
	public void testAddHeaders() {
		ResponseInfo t = new ResponseInfo();

		t.addHeaders(AMap.of("foo",headerInfo("bar")));
		assertObject(t.getHeaders()).json().is("{foo:{type:'bar'}}");
		assertObject(t.getHeaders()).isType(Map.class);
		assertObject(t.getHeaders().get("foo")).isType(HeaderInfo.class);

		t.addHeaders(AMap.of());
		assertObject(t.getHeaders()).json().is("{foo:{type:'bar'}}");
		assertObject(t.getHeaders()).isType(Map.class);
		assertObject(t.getHeaders().get("foo")).isType(HeaderInfo.class);

		t.addHeaders(null);
		assertObject(t.getHeaders()).json().is("{foo:{type:'bar'}}");
		assertObject(t.getHeaders()).isType(Map.class);
		assertObject(t.getHeaders().get("foo")).isType(HeaderInfo.class);
	}

	/**
	 * Test method for {@link ResponseInfo#header(java.lang.String, org.apache.juneau.dto.swagger.HeaderInfo)}.
	 */
	@Test
	public void testHeader() {
		ResponseInfo t = new ResponseInfo();

		t.header("a", headerInfo("a1"));
		t.header("b", null);
		t.header(null, headerInfo("c1"));

		assertObject(t.getHeaders()).json().is("{a:{type:'a1'},b:null,null:{type:'c1'}}");
	}

	/**
	 * Test method for {@link ResponseInfo#headers(java.lang.Object[])}.
	 */
	@Test
	public void testHeaders() {
		ResponseInfo t = new ResponseInfo();

		t.headers(AMap.of("a", headerInfo("a1")));
		t.headers(AMap.of("b", "{type:'b1'}"));
		t.headers("{c:{type:'c1'}}");
		t.headers("{}");
		t.headers((Object[])null);

		assertObject(t.getHeaders()).json().is("{a:{type:'a1'},b:{type:'b1'},c:{type:'c1'}}");
	}

	/**
	 * Test method for {@link ResponseInfo#setExamples(java.util.Map)}.
	 */
	@Test
	public void testSetExamples() {
		ResponseInfo t = new ResponseInfo();

		t.setExamples(AMap.of("foo","bar","baz",AList.of("qux")));
		assertObject(t.getExamples()).json().is("{foo:'bar',baz:['qux']}");
		assertObject(t.getExamples()).isType(Map.class);

		t.setExamples(AMap.of());
		assertObject(t.getExamples()).json().is("{}");
		assertObject(t.getExamples()).isType(Map.class);

		t.setExamples(null);
		assertNull(t.getExamples());
	}

	/**
	 * Test method for {@link ResponseInfo#addExamples(java.util.Map)}.
	 */
	@Test
	public void testAddExamples() {
		ResponseInfo t = new ResponseInfo();

		t.addExamples(AMap.of("foo","bar","baz",AList.of("qux")));
		assertObject(t.getExamples()).json().is("{foo:'bar',baz:['qux']}");
		assertObject(t.getExamples()).isType(Map.class);

		t.addExamples(AMap.of());
		assertObject(t.getExamples()).json().is("{foo:'bar',baz:['qux']}");
		assertObject(t.getExamples()).isType(Map.class);

		t.addExamples(null);
		assertObject(t.getExamples()).json().is("{foo:'bar',baz:['qux']}");
		assertObject(t.getExamples()).isType(Map.class);
	}

	/**
	 * Test method for {@link ResponseInfo#example(java.lang.String, java.lang.Object)}.
	 */
	@Test
	public void testExample() {
		ResponseInfo t = new ResponseInfo();

		t.example("text/a", "a");
		t.example("text/b", null);
		t.example(null, "c");

		assertObject(t.getExamples()).json().is("{'text/a':'a','text/b':null,null:'c'}");
	}

	/**
	 * Test method for {@link ResponseInfo#examples(java.lang.Object[])}.
	 */
	@Test
	public void testExamples() {
		ResponseInfo t = new ResponseInfo();

		t.examples(AMap.of("1",AList.of("a")));
		t.examples("{2:{c1:'c2'}}");
		t.examples("{}");
		t.examples((Object)null);

		assertObject(t.getExamples()).json().is("{'1':['a'],'2':{c1:'c2'}}");
	}

	/**
	 * Test method for {@link ResponseInfo#set(java.lang.String, java.lang.Object)}.
	 */
	@Test
	public void testSet() throws Exception {
		ResponseInfo t = new ResponseInfo();

		t
			.set("description", "a")
			.set("examples", AMap.of("foo","bar","baz",AList.of("qux")))
			.set("headers", AMap.of("a", headerInfo("a1")))
			.set("schema", schemaInfo().type("d"))
			.set("$ref", "ref");

		assertObject(t).json().is("{description:'a',schema:{type:'d'},headers:{a:{type:'a1'}},examples:{foo:'bar',baz:['qux']},'$ref':'ref'}");

		t
			.set("description", "a")
			.set("examples", "{foo:'bar',baz:['qux']}")
			.set("headers", "{a:{type:'a1'}}")
			.set("schema", "{type:'d'}")
			.set("$ref", "ref");

		assertObject(t).json().is("{description:'a',schema:{type:'d'},headers:{a:{type:'a1'}},examples:{foo:'bar',baz:['qux']},'$ref':'ref'}");

		t
			.set("description", new StringBuilder("a"))
			.set("examples", new StringBuilder("{foo:'bar',baz:['qux']}"))
			.set("headers", new StringBuilder("{a:{type:'a1'}}"))
			.set("schema", new StringBuilder("{type:'d'}"))
			.set("$ref", new StringBuilder("ref"));

		assertObject(t).json().is("{description:'a',schema:{type:'d'},headers:{a:{type:'a1'}},examples:{foo:'bar',baz:['qux']},'$ref':'ref'}");

		assertEquals("a", t.get("description", String.class));
		assertEquals("{foo:'bar',baz:['qux']}", t.get("examples", String.class));
		assertEquals("{a:{type:'a1'}}", t.get("headers", String.class));
		assertEquals("{type:'d'}", t.get("schema", String.class));
		assertEquals("ref", t.get("$ref", String.class));

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
		assertObject(JsonParser.DEFAULT.parse(s, ResponseInfo.class)).json().is(s);
	}
}
