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
package org.apache.juneau.rest.client;

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.httppart.HttpPartSchema.*;
import static org.junit.runners.MethodSorters.*;
import static org.apache.juneau.ListOperation.*;
import static org.apache.juneau.http.HttpParts.*;
import static org.apache.juneau.testutils.StreamUtils.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.http.part.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.testutils.*;
import org.apache.juneau.uon.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RestClient_Query_Test {

	@Rest
	public static class A extends BasicRestObject {
		@RestGet
		public Reader query(org.apache.juneau.rest.RestRequest req) {
			return reader(req.getQueryParams().asQueryString());
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Method tests
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_query_String_Object() throws Exception {
		client().queryData("foo","bar").queryData(part("foo",new StringBuilder("baz"),null)).build().get("/query").run().assertBody().is("foo=bar&foo=baz");
		client().build().get("/query").queryData("foo","bar").run().assertBody().contains("foo=bar");
	}

	@Test
	public void a02_query_String_Object_Schema() throws Exception {
		List<String> l = AList.of("bar","baz");
		client().build().get("/query").queryData(part("foo",l,T_ARRAY_PIPES)).run().assertBody().asString().urlDecode().is("foo=bar|baz");
		client().queryData(part("foo",l,T_ARRAY_PIPES)).build().get("/query").run().assertBody().asString().urlDecode().is("foo=bar|baz");
	}

	@Test
	public void a03_query_String_Object_Schema_Serializer() throws Exception {
		List<String> l = AList.of("bar","baz");
		client().queryData(part("foo",l,T_ARRAY_PIPES).serializer(UonSerializer.DEFAULT)).build().get("/query").run().assertBody().asString().urlDecode().is("foo=@(bar,baz)");
	}

	@Test
	public void a04_query_AddFlag_String_Object() throws Exception {
		client().queryData("foo","bar").build().get("/query").queryData(PREPEND,part("foo","baz")).run().assertBody().is("foo=baz&foo=bar");
		client().queryData("foo","bar").build().get("/query").queryData(PREPEND,part("foo","")).run().assertBody().is("foo=&foo=bar");
		client().queryData("foo","bar").build().get("/query").queryData(PREPEND,part("foo",null)).run().assertBody().is("foo=bar");
		client().queryData("foo","bar").build().get("/query").queryData(SET,part("foo","baz")).run().assertBody().is("foo=baz");
		client().queryData("foo","bar").build().get("/query").queryData(SET,part("foo",null)).run().assertBody().is("");
		client().queryData("foo","bar").build().get("/query").queryData(SET,part("foo","")).run().assertBody().is("foo=");
		client().queryData("bar","baz").build().get("/query").queryData(SET,part("foo","bar")).run().assertBody().is("bar=baz&foo=bar");
	}

	@Test
	public void a05_query_AddFlag_String_Object_Schema() throws Exception {
		List<String> l = AList.of("baz","qux");
		client().queryData("foo","bar").build().get("/query").queryData(PREPEND,part("foo",l,T_ARRAY_PIPES)).run().assertBody().asString().urlDecode().is("foo=baz|qux&foo=bar");
	}

	@Test
	public void a06_query_String_Supplier() throws Exception {
		List<String> l1 = AList.of("foo","bar"), l2 = AList.of("bar","baz");
		TestSupplier s = TestSupplier.of(l1);
		RestClient x = client().queryData(part("foo",s,null)).build();
		x.get("/query").run().assertBody().asString().urlDecode().is("foo=foo,bar");
		s.set(l2);
		x.get("/query").run().assertBody().asString().urlDecode().is("foo=bar,baz");
	}

	@Test
	public void a07_query_String_Supplier_Schema() throws Exception {
		String[] l1 = new String[]{"foo","bar"},l2 = new String[]{"bar","baz"};
		TestSupplier s = TestSupplier.of(l1);
		RestClient x = client().queryData(part("foo",s,T_ARRAY_PIPES)).build();
		x.get("/query").queryData(part("bar",s,T_ARRAY_PIPES)).run().assertBody().asString().urlDecode().is("foo=foo|bar&bar=foo|bar");
		s.set(l2);
		x.get("/query").queryData(part("bar",s,T_ARRAY_PIPES)).run().assertBody().asString().urlDecode().is("foo=bar|baz&bar=bar|baz");
	}

	@Test
	public void a08_query_String_Supplier_Schema_Serializer() throws Exception {
		List<String> l1 = AList.of("foo","bar"), l2 = AList.of("bar","baz");
		TestSupplier s = TestSupplier.of(l1);
		RestClient x = client().queryData(part("foo",s,T_ARRAY_PIPES).serializer(MockWriterSerializer.X)).build();
		x.get("/query").run().assertBody().asString().urlDecode().is("foo=xfoo|barx");
		s.set(l2);
		x.get("/query").run().assertBody().asString().urlDecode().is("foo=xbar|bazx");
	}

	@Test
	public void a09_query_NameValuePair() throws Exception {
		client().queryData(part("foo","bar")).build().get("/query").queryData(part("foo","baz")).run().assertBody().contains("foo=bar&foo=baz");
	}

	public static class A10 {
		public String foo="bar";
	}

	@Test
	public void a10_queries_Objects() throws Exception {
		client().build().get("/query").queryData(part("foo","bar")).run().assertBody().is("foo=bar");
	}

	public static class A12 extends Reader {
		@Override
		public int read(char[] cbuf,int off,int len) throws IOException {
			throw new IOException("foo");
		}
		@Override
		public void close() throws IOException {}
	}

	@Test
	public void a12_queryCustom_Object() throws Exception {
		client().build().get("/query").queryCustom("foo=bar").run().assertBody().contains("foo=bar");
		assertThrown(()->client().build().get("").queryCustom(new A12())).messages().contains("foo");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static BasicPart part(String name, Object val) {
		return basicPart(name, val);
	}

	private static SerializedPart part(String name, Object val, HttpPartSchema schema) {
		return serializedPart(name, val).schema(schema);
	}

	private static RestClient.Builder client() {
		return MockRestClient.create(A.class).simpleJson();
	}
}
