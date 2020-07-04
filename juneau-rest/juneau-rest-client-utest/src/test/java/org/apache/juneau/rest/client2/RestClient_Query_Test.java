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
import static org.junit.runners.MethodSorters.*;
import static org.apache.juneau.AddFlag.*;

import java.io.*;
import java.util.*;

import org.apache.http.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock2.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.testutils.*;
import org.apache.juneau.uon.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RestClient_Query_Test {

	@Rest
	public static class A extends BasicRest {
		@RestMethod
		public Reader getQuery(org.apache.juneau.rest.RestRequest req) {
			return new StringReader(req.getQuery().asQueryString());
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Method tests
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_query_String_Object() throws Exception {
		client().query("foo","bar").query("foo",new StringBuilder("baz")).build().get("/query").run().assertBody().is("foo=bar&foo=baz");
		client().build().get("/query").query("foo","bar").run().assertBody().contains("foo=bar");
	}

	@Test
	public void a02_query_String_Object_Schema() throws Exception {
		List<String> l = AList.of("bar","baz");
		client().build().get("/query").query("foo",l,T_ARRAY_PIPES).run().assertBody().urlDecode().is("foo=bar|baz");
		client().query("foo",l,T_ARRAY_PIPES).build().get("/query").run().assertBody().urlDecode().is("foo=bar|baz");
	}

	@Test
	public void a03_query_String_Object_Schema_Serializer() throws Exception {
		List<String> l = AList.of("bar","baz");
		client().query("foo",l,T_ARRAY_PIPES,UonSerializer.DEFAULT).build().get("/query").run().assertBody().urlDecode().is("foo=@(bar,baz)");
	}

	@Test
	public void a04_query_AddFlag_String_Object() throws Exception {
		client().query("foo","bar").build().get("/query").query(PREPEND,"foo","baz").run().assertBody().is("foo=baz&foo=bar");
		client().query("foo","bar").build().get("/query").query(PREPEND,"foo","").run().assertBody().is("foo=&foo=bar");
		client().query("foo","bar").build().get("/query").query(PREPEND,"foo",null).run().assertBody().is("foo=bar");
		client().query("foo","bar").build().get("/query").query(REPLACE,"foo","baz").run().assertBody().is("foo=baz");
		client().query("foo","bar").build().get("/query").query(REPLACE,"foo",null).run().assertBody().is("foo=bar");
		client().query("foo","bar").build().get("/query").query(REPLACE,"foo","").run().assertBody().is("foo=");
		client().query("bar","baz").build().get("/query").query(REPLACE,"foo","bar").run().assertBody().is("bar=baz&foo=bar");
	}

	@Test
	public void a05_query_AddFlag_String_Object_Schema() throws Exception {
		List<String> l = AList.of("baz","qux");
		client().query("foo","bar").build().get("/query").query(PREPEND,"foo",l,T_ARRAY_PIPES).run().assertBody().urlDecode().is("foo=baz|qux&foo=bar");
	}

	@Test
	public void a06_query_String_Supplier() throws Exception {
		List<String> l1 = AList.of("foo","bar"), l2 = AList.of("bar","baz");
		TestSupplier s = TestSupplier.of(l1);
		RestClient x = client().query("foo",s).build();
		x.get("/query").run().assertBody().urlDecode().is("foo=foo,bar");
		s.set(l2);
		x.get("/query").run().assertBody().urlDecode().is("foo=bar,baz");
	}

	@Test
	public void a07_query_String_Supplier_Schema() throws Exception {
		String[] l1 = new String[]{"foo","bar"},l2 = new String[]{"bar","baz"};
		TestSupplier s = TestSupplier.of(l1);
		RestClient x = client().query("foo",s,T_ARRAY_PIPES).build();
		x.get("/query").query("bar",s,T_ARRAY_PIPES).run().assertBody().urlDecode().is("foo=foo|bar&bar=foo|bar");
		s.set(l2);
		x.get("/query").query("bar",s,T_ARRAY_PIPES).run().assertBody().urlDecode().is("foo=bar|baz&bar=bar|baz");
	}

	public static class A8 extends SimplePartSerializer {
		@Override
		public SimplePartSerializerSession createPartSession(SerializerSessionArgs args) {
			return new SimplePartSerializerSession() {
				@Override
				public String serialize(HttpPartType type, HttpPartSchema schema, Object value) {
					return "x" + SimpleJson.DEFAULT.toString(value);
				}
			};
		}
	}

	@Test
	public void a08_query_String_Supplier_Schema_Serializer() throws Exception {
		List<String> l1 = AList.of("foo","bar"), l2 = AList.of("bar","baz");
		TestSupplier s = TestSupplier.of(l1);
		RestClient x = client().query("foo",s,T_ARRAY_PIPES,new A8()).build();
		x.get("/query").run().assertBody().urlDecode().is("foo=x['foo','bar']");
		s.set(l2);
		x.get("/query").run().assertBody().urlDecode().is("foo=x['bar','baz']");
	}

	@Test
	public void a09_query_NameValuePair() throws Exception {
		client().query(pair("foo","bar")).build().get("/query").query(pair("foo","baz")).run().assertBody().contains("foo=bar&foo=baz");
	}

	public static class A10 {
		public String foo="bar";
	}

	@Test
	public void a10_queries_Objects() throws Exception {
		client().queries(pair("foo","bar")).build().get("/query").run().assertBody().is("foo=bar");
		client().queries(OMap.of("foo","bar")).build().get("/query").run().assertBody().is("foo=bar");
		client().queries(AMap.of("foo","bar")).build().get("/query").run().assertBody().is("foo=bar");
		client().queries(pairs("foo","bar","foo","baz")).build().get("/query").run().assertBody().is("foo=bar&foo=baz");
		client().queries(pair("foo","bar"),pair("foo","baz")).build().get("/query").run().assertBody().is("foo=bar&foo=baz");
		client().queries(AList.of(pair("foo","bar"),pair("foo","baz"))).build().get("/query").run().assertBody().is("foo=bar&foo=baz");
		client().queries((Object)new NameValuePair[]{pair("foo","bar")}).build().get("/query").run().assertBody().is("foo=bar");


		client().build().get("/query").queries(pair("foo","bar")).run().assertBody().is("foo=bar");
		client().build().get("/query").queries(OMap.of("foo","bar")).run().assertBody().is("foo=bar");
		client().build().get("/query").queries(AMap.of("foo","bar")).run().assertBody().is("foo=bar");
		client().build().get("/query").queries(pairs("foo","bar","foo","baz")).run().assertBody().is("foo=bar&foo=baz");
		client().build().get("/query").queries(pair("foo","bar"),pair("foo","baz")).run().assertBody().is("foo=bar&foo=baz");
		client().build().get("/query").queries(AList.of(pair("foo","bar"),pair("foo","baz"))).run().assertBody().is("foo=bar&foo=baz");
		client().build().get("/query").queries((Object)new NameValuePair[]{pair("foo","bar")}).run().assertBody().is("foo=bar");
		client().build().get("/query").queries(new A10()).run().assertBody().is("foo=bar");

		client().queries((Object)null).build().get("/query").queries((Object)null).run().assertBody().is("");
		assertThrown(()->client().queries("baz")).contains("Invalid type");
		assertThrown(()->client().build().get("/query").queries("baz")).contains("Invalid type");

		client().queries(pair("foo","bar"),null).build().get("/query").run().assertBody().is("foo=bar");
	}

	@Test
	public void a11_queryPairs_Objects() throws Exception {
		List<String> l1 = AList.of("bar1","bar2"), l2 = AList.of("qux1","qux2");
		client().queryPairs("foo","bar","baz","qux").build().get("/query").run().assertBody().is("foo=bar&baz=qux");
		client().build().get("/query").queryPairs("foo","bar","baz","qux").run().assertBody().is("foo=bar&baz=qux");
		client().queryPairs("foo",l1,"baz",l2).build().get("/query").run().assertBody().urlDecode().is("foo=bar1,bar2&baz=qux1,qux2");
		client().build().get("/query").queryPairs("foo",l1,"baz",l2).run().assertBody().urlDecode().is("foo=bar1,bar2&baz=qux1,qux2");
		assertThrown(()->client().queryPairs("foo","bar","baz")).contains("Odd number of parameters");
		assertThrown(()->client().build().get().queryPairs("foo","bar","baz")).contains("Odd number of parameters");
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
		assertThrown(()->client().build().get("").queryCustom(new A12())).contains("foo");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static NameValuePair pair(String name, Object val) {
		return BasicNameValuePair.of(name, val);
	}

	private static NameValuePairSupplier pairs(Object...pairs) {
		return NameValuePairSupplier.ofPairs(pairs);
	}

	private static RestClientBuilder client() {
		return MockRestClient.create(A.class).simpleJson();
	}
}
