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
package org.apache.juneau.rest.client2;

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.httppart.HttpPartSchema.*;

import java.io.*;
import java.util.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client2.RestClient_Test.*;
import org.apache.juneau.rest.mock2.*;
import org.apache.juneau.testutils.*;
import org.apache.juneau.uon.*;
import org.junit.*;

public class RestClient_Query_Test {

	@Rest
	public static class A extends BasicRest {
		@RestMethod(path="/echo/*")
		public String getEcho(org.apache.juneau.rest.RestRequest req) {
			return req.toString();
		}
		@RestMethod
		public Reader getQuery(org.apache.juneau.rest.RestRequest req) {
			return new StringReader(req.getQuery().asQueryString());
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Query
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_query_basic() throws Exception {
		client().query("Foo","bar").query("Foo",new StringBuilder("baz")).build().get("/query").run().assertBody().is("Foo=bar&Foo=baz");
	}

	@Test
	public void a02_query_objects() throws Exception {
		client().queries(pair("Foo","f1")).queries(OMap.of("Foo","f2")).queries(AMap.of("Foo","f3")).queries(pairs("Foo","f4","Foo","f5")).queries(pair("Foo","f6"), pair("Foo","f7")).queries((Object)new NameValuePair[]{pair("Foo","f8")}).build().get("/query").run().assertBody().is("Foo=f1&Foo=f2&Foo=f3&Foo=f4&Foo=f5&Foo=f6&Foo=f7&Foo=f8");
		client().build().get("/query").queries(pair("Foo","f1")).queries(OMap.of("Foo","f2")).queries(AMap.of("Foo","f3")).queries(pairs("Foo","f4","Foo","f5")).queries(pair("Foo","f6"), pair("Foo","f7")).queries((Object)new NameValuePair[]{pair("Foo","f8")}).run().assertBody().is("Foo=f1&Foo=f2&Foo=f3&Foo=f4&Foo=f5&Foo=f6&Foo=f7&Foo=f8");
		client().queries((Object)null).build().get("/query").queries((Object)null).run().assertBody().is("");
		assertThrown(()->{client().queries("Baz");}).contains("Invalid type");
		assertThrown(()->{client().build().get("/query").queries("Baz");}).contains("Invalid type");
	}

	@Test
	public void a03_query_withSchema() throws Exception {
		client().query("Foo",AList.of("bar","baz"), T_ARRAY_PIPES).build().get("/query").run().assertBody().is("Foo=bar%7Cbaz").assertBody().urlDecodedIs("Foo=bar|baz");
	}

	@Test
	public void a04_query_withSchemaAndSerializer() throws Exception {
		client().query("Foo",AList.of("bar","baz"), T_ARRAY_PIPES, UonSerializer.DEFAULT).build().get("/query").run().assertBody().is("Foo=%40%28bar%2Cbaz%29").assertBody().urlDecodedIs("Foo=@(bar,baz)");
	}

	@Test
	public void a05_query_withSchemaAndSupplier() throws Exception {
		TestSupplier s = TestSupplier.of(AList.of("foo","bar"));
		RestClient x = client().query("Foo", s, T_ARRAY_PIPES).build();
		x.get("/query").query("Bar", s, T_ARRAY_PIPES).run().assertBody().is("Foo=foo%7Cbar&Bar=foo%7Cbar").assertBody().urlDecodedIs("Foo=foo|bar&Bar=foo|bar");
		s.set(new String[]{"bar","baz"});
		x.get("/query").query("Bar", s, T_ARRAY_PIPES).run().assertBody().is("Foo=bar%7Cbaz&Bar=bar%7Cbaz").assertBody().urlDecodedIs("Foo=bar|baz&Bar=bar|baz");
	}

	@Test
	public void a06_query_withSchemaAndSupplierAndSerializer() throws Exception {
		TestSupplier s = TestSupplier.of(AList.of("foo","bar"));
		RestClient x = client().query("Foo", s, T_ARRAY_PIPES, new K12a()).build();
		x.get("/query").run().assertBody().is("Foo=x%5B%27foo%27%2C%27bar%27%5D").assertBody().urlDecodedIs("Foo=x['foo','bar']");
		s.set(AList.of("bar","baz"));
		x.get("/query").run().assertBody().is("Foo=x%5B%27bar%27%2C%27baz%27%5D").assertBody().urlDecodedIs("Foo=x['bar','baz']");
	}

	@Test
	public void a07_query_withSupplier() throws Exception {
		TestSupplier s = TestSupplier.of(AList.of("foo","bar"));
		RestClient x = client().query("Foo", s).build();
		x.get("/query").run().assertBody().is("Foo=foo%2Cbar").assertBody().urlDecodedIs("Foo=foo,bar");
		s.set(AList.of("bar","baz"));
		x.get("/query").run().assertBody().is("Foo=bar%2Cbaz").assertBody().urlDecodedIs("Foo=bar,baz");
	}

	@Test
	public void a08_query_withNull() throws Exception {
		client().queries(pair("Foo","bar"), null).build().get("/query").run().assertBody().is("Foo=bar");
	}

	@Test
	public void a09_queryPairs() throws Exception {
		client().queryPairs("foo","bar","baz","qux").build().get("/query").run().assertBody().is("foo=bar&baz=qux");
		client().build().get("/query").queryPairs("foo","bar","baz","qux").run().assertBody().is("foo=bar&baz=qux");
		client().queryPairs("foo",AList.of("bar1","bar2"),"baz",AList.of("qux1","qux2")).build().get("/query").run().assertBody().is("foo=bar1%2Cbar2&baz=qux1%2Cqux2").assertBody().urlDecodedIs("foo=bar1,bar2&baz=qux1,qux2");
		client().build().get("/query").queryPairs("foo",AList.of("bar1","bar2"),"baz",AList.of("qux1","qux2")).run().assertBody().is("foo=bar1%2Cbar2&baz=qux1%2Cqux2").assertBody().urlDecodedIs("foo=bar1,bar2&baz=qux1,qux2");
		assertThrown(()->{client().queryPairs("foo","bar","baz");}).contains("Odd number of parameters");
		assertThrown(()->{client().build().get().queryPairs("foo","bar","baz");}).contains("Odd number of parameters");
	}

	public static class A11 {
		public String foo;
		A11 init() {
			this.foo = "baz";
			return this;
		}
	}

	@Test
	public void a11_query_request() throws Exception {
		client().build().get("/echo").query("foo", "bar").run().assertBody().contains("GET /echo?foo=bar");
		client().build().get("/echo").query("foo", AList.of("bar","baz"), T_ARRAY_PIPES).run().assertBody().contains("GET /echo?foo=bar%7Cbaz");
		client().query("foo","bar").build().get("/echo").query(EnumSet.of(AddFlag.PREPEND), "foo", "baz").run().assertBody().contains("GET /echo?foo=baz&foo=bar");
		client().query("foo","bar").build().get("/echo").query(EnumSet.of(AddFlag.PREPEND), "foo", AList.of("baz","qux"), T_ARRAY_PIPES).run().assertBody().contains("GET /echo?foo=baz%7Cqux&foo=bar");
		client().query("foo","bar").build().get("/echo").queries(pair("foo","baz"), pairs("foo","qux"), OMap.of("foo","quux")).run().assertBody().contains("GET /echo?foo=bar&foo=baz&foo=qux&foo=quux");
		client().query("foo","bar").build().get("/echo").queries(new A11().init()).run().assertBody().contains("GET /echo?foo=bar&foo=baz");
		assertThrown(()->{client().build().get("/echo").queries("foo=baz");}).is("Invalid type passed to queries(): java.lang.String");
	}

	@Test
	public void a12_query_NameValuePair() throws Exception {
		client().query(pair("foo", "bar")).build().get("/echo").query(pair("foo", "baz")).run().assertBody().contains("GET /echo?foo=bar&foo=baz");
	}

	public static class A13 extends Reader {
		@Override
		public int read(char[] cbuf, int off, int len) throws IOException {
			throw new IOException("foo");
		}
		@Override
		public void close() throws IOException {}
	}

	@Test
	public void a13_query_custom() throws Exception {
		client().build().get("/echo").queryCustom("foo=bar").run().assertBody().contains("GET /echo?foo=bar");
		assertThrown(()->{client().build().get("/echo").queryCustom(new A13());}).is("foo");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static NameValuePair pair(String name, Object val) {
		return BasicNameValuePair.of(name, val);
	}

	private static NameValuePairs pairs(Object...pairs) {
		return NameValuePairs.of(pairs);
	}

	private static RestClientBuilder client() {
		return MockRestClient.create(A.class).simpleJson();
	}
}
