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
import static org.apache.juneau.AddFlag.*;

import java.io.*;

import org.apache.http.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client2.RestClient_Test.*;
import org.apache.juneau.rest.mock2.*;
import org.apache.juneau.testutils.*;
import org.apache.juneau.uon.*;
import org.junit.*;

public class RestClient_FormData_Test {

	@Rest
	public static class A extends BasicRest {
		@RestMethod
		public Reader postFormData(org.apache.juneau.rest.RestRequest req) {
			return new StringReader(req.getFormData().asQueryString());
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_formData() throws Exception {
		client().formData("Foo","bar").formData("Foo",new StringBuilder("baz")).build().post("/formData").run().assertBody().is("Foo=bar&Foo=baz");
		client().build().post("/formData").formData("Foo","bar").formData("Foo",new StringBuilder("baz")).run().assertBody().is("Foo=bar&Foo=baz");
	}

	@Test
	public void a02_formDatas() throws Exception {
		client().formDatas(pair("Foo","f1")).formDatas(OMap.of("Foo","f2")).formDatas(AMap.of("Foo","f3")).formDatas(pairs("Foo","f4","Foo","f5")).formDatas(pair("Foo","f6"), pair("Foo","f7")).formDatas((Object)new NameValuePair[]{pair("Foo","f8")}).build().post("/formData").run().assertBody().is("Foo=f1&Foo=f2&Foo=f3&Foo=f4&Foo=f5&Foo=f6&Foo=f7&Foo=f8");
		client().build().post("/formData").formDatas(pair("Foo","f1")).formDatas(OMap.of("Foo","f2")).formDatas(AMap.of("Foo","f3")).formDatas(pairs("Foo","f4","Foo","f5")).formDatas(pair("Foo","f6"), pair("Foo","f7")).formDatas((Object)new NameValuePair[]{pair("Foo","f8")}).run().assertBody().is("Foo=f1&Foo=f2&Foo=f3&Foo=f4&Foo=f5&Foo=f6&Foo=f7&Foo=f8");
	}

	@Test
	public void a03_formData_withSchema() throws Exception {
		client().formData("Foo",AList.of("bar","baz"), T_ARRAY_PIPES).build().post("/formData").run().assertBody().is("Foo=bar%7Cbaz").assertBody().urlDecodedIs("Foo=bar|baz");
		client().build().post("/formData").formData("Foo",AList.of("bar","baz"), T_ARRAY_PIPES).run().assertBody().is("Foo=bar%7Cbaz").assertBody().urlDecodedIs("Foo=bar|baz");

		client().formData("Foo",AList.of("bar","baz"), T_ARRAY_PIPES).build().post("/formData").formData("Foo",AList.of("qux","quux"), T_ARRAY_PIPES).run().assertBody().is("Foo=bar%7Cbaz&Foo=qux%7Cquux").assertBody().urlDecodedIs("Foo=bar|baz&Foo=qux|quux");
		client().formData("Foo",AList.of("bar","baz"), T_ARRAY_PIPES).build().post("/formData").formData(APPEND,"Foo",AList.of("qux","quux"), T_ARRAY_PIPES).run().assertBody().is("Foo=bar%7Cbaz&Foo=qux%7Cquux").assertBody().urlDecodedIs("Foo=bar|baz&Foo=qux|quux");
		client().formData("Foo",AList.of("bar","baz"), T_ARRAY_PIPES).build().post("/formData").formData(PREPEND,"Foo",AList.of("qux","quux"), T_ARRAY_PIPES).run().assertBody().is("Foo=qux%7Cquux&Foo=bar%7Cbaz").assertBody().urlDecodedIs("Foo=qux|quux&Foo=bar|baz");
		client().formData("Foo",AList.of("bar","baz"), T_ARRAY_PIPES).build().post("/formData").formData(REPLACE,"Foo",AList.of("qux","quux"), T_ARRAY_PIPES).run().assertBody().is("Foo=qux%7Cquux").assertBody().urlDecodedIs("Foo=qux|quux");
	}

	@Test
	public void a03_formData_withSchemaAndSerializer() throws Exception {
		client().formData("Foo",AList.of("bar","baz"), T_ARRAY_PIPES, UonSerializer.DEFAULT).build().post("/formData").run().assertBody().urlDecodedIs("Foo=@(bar,baz)");
	}

	@Test
	public void a04_formData_withSupplier() throws Exception {
		TestSupplier s = TestSupplier.of(null);

		RestClient x1 = client().formData("Foo", s).build();
		s.set(OList.of("foo","bar"));
		x1.post("/formData").run().assertBody().is("Foo=foo%2Cbar").assertBody().urlDecodedIs("Foo=foo,bar");
		s.set(OList.of("bar","baz"));
		x1.post("/formData").run().assertBody().is("Foo=bar%2Cbaz").assertBody().urlDecodedIs("Foo=bar,baz");

		RestClient x2 = client().build();
		s.set(OList.of("foo","bar"));
		x2.post("/formData").formData("Foo", s).run().assertBody().is("Foo=foo%2Cbar").assertBody().urlDecodedIs("Foo=foo,bar");
		s.set(OList.of("bar","baz"));
		x2.post("/formData").formData("Foo", s).run().assertBody().is("Foo=bar%2Cbaz").assertBody().urlDecodedIs("Foo=bar,baz");
	}

	@Test
	public void a05_formData_withSupplierAndSerializer() throws Exception {
		TestSupplier s = TestSupplier.of(OList.of("foo","bar"));
		RestClient x = client().formData("Foo", s, T_ARRAY_PIPES, new K12a()).build();
		x.post("/formData").run().assertBody().is("Foo=x%5B%27foo%27%2C%27bar%27%5D").assertBody().urlDecodedIs("Foo=x['foo','bar']");
		s.set(OList.of("bar","baz"));
		x.post("/formData").run().assertBody().is("Foo=x%5B%27bar%27%2C%27baz%27%5D").assertBody().urlDecodedIs("Foo=x['bar','baz']");
	}

	@Test
	public void a06_formData_withSupplierAndSchema() throws Exception {
		TestSupplier s = TestSupplier.of(null);

		RestClient x1 = client().formData("Foo", s, T_ARRAY_PIPES).build();
		s.set(AList.of("foo","bar"));
		x1.post("/formData").run().assertBody().is("Foo=foo%7Cbar").assertBody().urlDecodedIs("Foo=foo|bar");
		s.set(AList.of("bar","baz"));
		x1.post("/formData").run().assertBody().is("Foo=bar%7Cbaz").assertBody().urlDecodedIs("Foo=bar|baz");

		RestClient x2 = client().build();
		s.set(AList.of("foo","bar"));
		x2.post("/formData").formData("Foo", s, T_ARRAY_PIPES).run().assertBody().is("Foo=foo%7Cbar").assertBody().urlDecodedIs("Foo=foo|bar");
		s.set(AList.of("bar","baz"));
		x2.post("/formData").formData("Foo", s, T_ARRAY_PIPES).run().assertBody().is("Foo=bar%7Cbaz").assertBody().urlDecodedIs("Foo=bar|baz");
	}

	@Test
	public void a07_formDatas_withNulls() throws Exception {
		client().formDatas(pair("Foo","bar"), null).build().post("/formData").run().assertBody().is("Foo=bar");
		client().build().post("/formData").formDatas(pair("Foo","bar"), null).run().assertBody().is("Foo=bar");

		client().formDatas(pair("Foo",null)).build().post("/formData").run().assertBody().is("");
		client().formDatas(pair(null,"Foo")).build().post("/formData").run().assertBody().is("null=Foo");
		client().formDatas(pair(null,null)).build().post("/formData").run().assertBody().is("");

		client().build().post("/formData").formDatas(pair("Foo",null)).run().assertBody().is("");
		client().build().post("/formData").formDatas(pair(null,"Foo")).run().assertBody().is("null=Foo");
		client().build().post("/formData").formDatas(pair(null,null)).run().assertBody().is("");
	}

	@Test
	public void a08_formDatas_invalid() throws Exception {
		assertThrown(()->{client().formDatas(pair("Foo","bar"), "Baz");}).is("Invalid type passed to formData():  java.lang.String");
	}

	@Test
	public void a09_formDataPairs() throws Exception {
		client().formDataPairs("foo","bar","baz","qux").build().post("/formData").run().assertBody().is("foo=bar&baz=qux");
		client().formDataPairs("foo",AList.of("bar1","bar2"),"baz",AList.of("qux1","qux2")).build().post("/formData").run().assertBody().is("foo=bar1%2Cbar2&baz=qux1%2Cqux2").assertBody().urlDecodedIs("foo=bar1,bar2&baz=qux1,qux2");
	}

	@Test
	public void a10_formDataPairs_invalid() throws Exception {
		assertThrown(()->{client().formDataPairs("foo","bar","baz");}).is("Odd number of parameters passed into formDataPairs(Object...)");
	}

	@Test
	public void a11_formDatas_serializedHeaderBuilder() throws Exception {
		client().formDatas(SerializedHeader.create().name("foo").value("bar")).build().post("/formData").run().assertBody().is("foo=bar");
	}

	@Test
	public void a12_formData_NameValuePair() throws Exception {
		client().formData(pair("foo", "bar")).build().post("/formData").formData(pair("foo", "baz")).run().assertBody().is("foo=bar&foo=baz");
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
