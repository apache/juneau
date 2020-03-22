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

import static org.apache.juneau.dto.swagger.SwaggerBuilder.*;
import static org.apache.juneau.testutils.TestUtils.*;
import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.json.*;
import org.apache.juneau.utils.*;
import org.junit.*;

/**
 * Testcase for {@link ResponseInfo}.
 */
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
		assertInstanceOf(String.class, t.getDescription());

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
		assertObjectEquals("{title:'foo'}", t.getSchema());

		t.schema("{title:'foo'}");
		assertObjectEquals("{title:'foo'}", t.getSchema());
		assertInstanceOf(SchemaInfo.class, t.getSchema());

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
		assertObjectEquals("{foo:{type:'bar'}}", t.getHeaders());
		assertInstanceOf(Map.class, t.getHeaders());
		assertInstanceOf(HeaderInfo.class, t.getHeaders().get("foo"));

		t.setHeaders(AMap.of());
		assertObjectEquals("{}", t.getHeaders());
		assertInstanceOf(Map.class, t.getHeaders());

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
		assertObjectEquals("{foo:{type:'bar'}}", t.getHeaders());
		assertInstanceOf(Map.class, t.getHeaders());
		assertInstanceOf(HeaderInfo.class, t.getHeaders().get("foo"));

		t.addHeaders(AMap.of());
		assertObjectEquals("{foo:{type:'bar'}}", t.getHeaders());
		assertInstanceOf(Map.class, t.getHeaders());
		assertInstanceOf(HeaderInfo.class, t.getHeaders().get("foo"));

		t.addHeaders(null);
		assertObjectEquals("{foo:{type:'bar'}}", t.getHeaders());
		assertInstanceOf(Map.class, t.getHeaders());
		assertInstanceOf(HeaderInfo.class, t.getHeaders().get("foo"));
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

		assertObjectEquals("{a:{type:'a1'},b:null,null:{type:'c1'}}", t.getHeaders());
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

		assertObjectEquals("{a:{type:'a1'},b:{type:'b1'},c:{type:'c1'}}", t.getHeaders());
	}

	/**
	 * Test method for {@link ResponseInfo#setExamples(java.util.Map)}.
	 */
	@Test
	public void testSetExamples() {
		ResponseInfo t = new ResponseInfo();

		t.setExamples(AMap.of("foo","bar","baz",AList.of("qux")));
		assertObjectEquals("{foo:'bar',baz:['qux']}", t.getExamples());
		assertInstanceOf(Map.class, t.getExamples());

		t.setExamples(AMap.of());
		assertObjectEquals("{}", t.getExamples());
		assertInstanceOf(Map.class, t.getExamples());

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
		assertObjectEquals("{foo:'bar',baz:['qux']}", t.getExamples());
		assertInstanceOf(Map.class, t.getExamples());

		t.addExamples(AMap.of());
		assertObjectEquals("{foo:'bar',baz:['qux']}", t.getExamples());
		assertInstanceOf(Map.class, t.getExamples());

		t.addExamples(null);
		assertObjectEquals("{foo:'bar',baz:['qux']}", t.getExamples());
		assertInstanceOf(Map.class, t.getExamples());
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

		assertObjectEquals("{'text/a':'a','text/b':null,null:'c'}", t.getExamples());
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

		assertObjectEquals("{'1':['a'],'2':{c1:'c2'}}", t.getExamples());
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

		assertObjectEquals("{description:'a',schema:{type:'d'},headers:{a:{type:'a1'}},examples:{foo:'bar',baz:['qux']},'$ref':'ref'}", t);

		t
			.set("description", "a")
			.set("examples", "{foo:'bar',baz:['qux']}")
			.set("headers", "{a:{type:'a1'}}")
			.set("schema", "{type:'d'}")
			.set("$ref", "ref");

		assertObjectEquals("{description:'a',schema:{type:'d'},headers:{a:{type:'a1'}},examples:{foo:'bar',baz:['qux']},'$ref':'ref'}", t);

		t
			.set("description", new StringBuilder("a"))
			.set("examples", new StringBuilder("{foo:'bar',baz:['qux']}"))
			.set("headers", new StringBuilder("{a:{type:'a1'}}"))
			.set("schema", new StringBuilder("{type:'d'}"))
			.set("$ref", new StringBuilder("ref"));

		assertObjectEquals("{description:'a',schema:{type:'d'},headers:{a:{type:'a1'}},examples:{foo:'bar',baz:['qux']},'$ref':'ref'}", t);

		assertEquals("a", t.get("description", String.class));
		assertEquals("{foo:'bar',baz:['qux']}", t.get("examples", String.class));
		assertEquals("{a:{type:'a1'}}", t.get("headers", String.class));
		assertEquals("{type:'d'}", t.get("schema", String.class));
		assertEquals("ref", t.get("$ref", String.class));

		assertInstanceOf(String.class, t.get("description", Object.class));
		assertInstanceOf(Map.class, t.get("examples", Object.class));
		assertInstanceOf(Map.class, t.get("headers", Object.class));
		assertInstanceOf(HeaderInfo.class, t.get("headers", Map.class).values().iterator().next());
		assertInstanceOf(SchemaInfo.class, t.get("schema", Object.class));
		assertInstanceOf(StringBuilder.class, t.get("$ref", Object.class));

		t.set("null", null).set(null, "null");
		assertNull(t.get("null", Object.class));
		assertNull(t.get(null, Object.class));
		assertNull(t.get("foo", Object.class));

		String s = "{description:'a',schema:{type:'d'},headers:{a:{type:'a1'}},examples:{foo:'bar',baz:['qux']},'$ref':'ref'}";
		assertObjectEquals(s, JsonParser.DEFAULT.parse(s, ResponseInfo.class));
	}
}
