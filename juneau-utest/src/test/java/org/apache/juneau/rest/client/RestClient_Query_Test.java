/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.rest.client;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.http.HttpParts.*;
import static org.apache.juneau.httppart.HttpPartSchema.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.http.part.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.servlet.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.utest.utils.*;
import org.junit.jupiter.api.*;

class RestClient_Query_Test extends TestBase {

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

	@Test void a01_query_String_Object() throws Exception {
		client().queryData("foo","bar").queryData(part("foo",new StringBuilder("baz"),null)).build().get("/query").run().assertContent("foo=bar&foo=baz");
		client().build().get("/query").queryData("foo","bar").run().assertContent().isContains("foo=bar");
	}

	@Test void a02_query_String_Object_Schema() throws Exception {
		var l = l("bar","baz");
		client().build().get("/query").queryData(part("foo",l,T_ARRAY_PIPES)).run().assertContent().asString().asUrlDecode().is("foo=bar|baz");
		client().queryData(part("foo",l,T_ARRAY_PIPES)).build().get("/query").run().assertContent().asString().asUrlDecode().is("foo=bar|baz");
	}

	@Test void a03_query_String_Object_Schema_Serializer() throws Exception {
		var l = l("bar","baz");
		client().queryData(part("foo",l,T_ARRAY_PIPES).serializer(UonSerializer.DEFAULT)).build().get("/query").run().assertContent().asString().asUrlDecode().is("foo=@(bar,baz)");
	}

	@Test void a06_query_String_Supplier() throws Exception {
		var l1 = l("foo","bar");
		var l2 = l("bar","baz");
		var s = MutableSupplier.of(l1);
		var x = client().queryData(part("foo",s,null)).build();
		x.get("/query").run().assertContent().asString().asUrlDecode().is("foo=foo,bar");
		s.set(l2);
		x.get("/query").run().assertContent().asString().asUrlDecode().is("foo=bar,baz");
	}

	@Test void a07_query_String_Supplier_Schema() throws Exception {
		var l1 = a("foo","bar");
		var l2 = a("bar","baz");
		var s = MutableSupplier.of(l1);
		var x = client().queryData(part("foo",s,T_ARRAY_PIPES)).build();
		x.get("/query").queryData(part("bar",s,T_ARRAY_PIPES)).run().assertContent().asString().asUrlDecode().is("foo=foo|bar&bar=foo|bar");
		s.set(l2);
		x.get("/query").queryData(part("bar",s,T_ARRAY_PIPES)).run().assertContent().asString().asUrlDecode().is("foo=bar|baz&bar=bar|baz");
	}

	@Test void a08_query_String_Supplier_Schema_Serializer() throws Exception {
		var l1 = l("foo","bar");
		var l2 = l("bar","baz");
		var s = MutableSupplier.of(l1);
		var x = client().queryData(part("foo",s,T_ARRAY_PIPES).serializer(FakeWriterSerializer.X)).build();
		x.get("/query").run().assertContent().asString().asUrlDecode().is("foo=xfoo|barx");
		s.set(l2);
		x.get("/query").run().assertContent().asString().asUrlDecode().is("foo=xbar|bazx");
	}

	@Test void a09_query_NameValuePair() throws Exception {
		client().queryData(part("foo","bar")).build().get("/query").queryData(part("foo","baz")).run().assertContent().isContains("foo=bar&foo=baz");
	}

	public static class A10 {
		public String foo="bar";
	}

	@Test void a10_queries_Objects() throws Exception {
		client().build().get("/query").queryData(part("foo","bar")).run().assertContent("foo=bar");
	}

	public static class A12 extends Reader {
		@Override
		public int read(char[] cbuf,int off,int len) throws IOException {
			throw new IOException("foo");
		}
		@Override
		public void close() throws IOException {}  // NOSONAR
	}

	@Test void a12_queryCustom_Object() throws Exception {
		client().build().get("/query").queryCustom("foo=bar").run().assertContent().isContains("foo=bar");
		assertThrowsWithMessage(Exception.class, "foo", ()->client().build().get("").queryCustom(new A12()));
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
		return MockRestClient.create(A.class).json5();
	}
}