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
import static org.apache.juneau.AddFlag.*;
import static org.apache.juneau.http.header.StandardHttpHeaders.*;

import java.io.*;
import java.util.*;

import org.apache.http.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.testutils.*;
import org.apache.juneau.testutils.pojos.ABean;
import org.apache.juneau.uon.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RestClient_FormData_Test {

	@Rest
	public static class A extends BasicRestObject {
		@RestPost
		public Reader formData(org.apache.juneau.rest.RestRequest req) {
			return new StringReader(req.getFormParams().asQueryString());
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Method tests
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_formData_String_Object() throws Exception {
		client().formData("foo","bar").formData("foo",new StringBuilder("baz")).build().post("/formData").run().assertBody().is("foo=bar&foo=baz");
		client().build().post("/formData").formData("foo","bar").formData("foo",new StringBuilder("baz")).run().assertBody().is("foo=bar&foo=baz");
		client().build().post("/formData").formData(null,"bar").run().assertBody().is("");
		client().build().post("/formData").formData("foo",null).run().assertBody().is("");
		client().build().post("/formData").formData(null,null).run().assertBody().is("");
	}

	@Test
	public void a02_formData_AddFlag_String_Object() throws Exception {
		client().formData(pair("foo","bar")).build().post("/formData").formData(APPEND,"foo","baz").run().assertBody().is("foo=bar&foo=baz");
		client().formData(pair("foo","bar")).build().post("/formData").formData(PREPEND,"foo","baz").run().assertBody().is("foo=baz&foo=bar");
		client().formData(pair("foo","bar")).build().post("/formData").formData(REPLACE,"foo","baz").run().assertBody().is("foo=baz");
		client().formData(pair("foo","bar")).build().post("/formData").formData(REPLACE,"bar","baz").run().assertBody().is("foo=bar&bar=baz");
	}

	@Test
	public void a03_formData_NameValuePair() throws Exception {
		client().formData(pair("foo","bar")).build().post("/formData").formData(pair("foo","baz")).run().assertBody().is("foo=bar&foo=baz");
	}

	@Test
	public void a04_formDatas_Objects() throws Exception {
		client().formDatas(pair("foo","bar")).build().post("/formData").run().assertBody().is("foo=bar");
		client().formDatas(OMap.of("foo","bar")).build().post("/formData").run().assertBody().is("foo=bar");
		client().formDatas(AMap.of("foo","bar")).build().post("/formData").run().assertBody().is("foo=bar");
		client().formDatas(pairs("foo","bar","foo","baz")).build().post("/formData").run().assertBody().is("foo=bar&foo=baz");
		client().formDatas(pair("foo","bar"),pair("foo","baz")).build().post("/formData").run().assertBody().is("foo=bar&foo=baz");
		client().formDatas(AList.of(pair("foo","bar"),pair("foo","baz"))).build().post("/formData").run().assertBody().is("foo=bar&foo=baz");
		client().formDatas((Object)new NameValuePair[]{pair("foo","bar")}).build().post("/formData").run().assertBody().is("foo=bar");

		client().build().post("/formData").formDatas(pair("foo","bar")).run().assertBody().is("foo=bar");
		client().build().post("/formData").formDatas(OMap.of("foo","bar")).run().assertBody().is("foo=bar");
		client().build().post("/formData").formDatas(AMap.of("foo","bar")).run().assertBody().is("foo=bar");
		client().build().post("/formData").formDatas(pairs("foo","bar","foo","baz")).run().assertBody().is("foo=bar&foo=baz");
		client().build().post("/formData").formDatas(pair("foo","bar"),pair("foo","baz")).run().assertBody().is("foo=bar&foo=baz");
		client().build().post("/formData").formDatas(AList.of(pair("foo","bar"),pair("foo","baz"))).run().assertBody().is("foo=bar&foo=baz");
		client().build().post("/formData").formDatas((Object)new NameValuePair[]{pair("foo","bar")}).run().assertBody().is("foo=bar");

		client().build().post("/formData").formDatas(ABean.get()).run().assertBody().is("a=1&b=foo");

		client().formDatas(pair("foo","bar"),null).build().post("/formData").run().assertBody().is("foo=bar");
		client().build().post("/formData").formDatas(pair("foo","bar"),null).run().assertBody().is("foo=bar");
		client().formDatas(pair("foo",null)).build().post("/formData").run().assertBody().is("");
		client().formDatas(pair(null,"foo")).build().post("/formData").run().assertBody().is("null=foo");
		client().formDatas(pair(null,null)).build().post("/formData").run().assertBody().is("");

		client().build().post("/formData").formDatas(pair("foo",null)).run().assertBody().is("");
		client().build().post("/formData").formDatas(pair(null,"foo")).run().assertBody().is("null=foo");
		client().build().post("/formData").formDatas(pair(null,null)).run().assertBody().is("");

		client().formDatas(serializedHeader("foo","bar")).build().post("/formData").run().assertBody().is("foo=bar");
		client().formDatas(SerializedNameValuePair.of("foo","bar").schema(null)).build().post("/formData").run().assertBody().is("foo=bar");
		client().formDatas(SerializedNameValuePair.of("foo",null).schema(null)).build().post("/formData").run().assertBody().is("");
		client().formDatas(SerializedNameValuePair.of("foo",null).skipIfEmpty().schema(HttpPartSchema.create()._default("bar").build())).build().post("/formData").run().assertBody().is("foo=bar");

		assertThrown(()->client().build().post("/formData").formDatas("bad")).is("Invalid type passed to formDatas(): java.lang.String");
		assertThrown(()->client().formDatas(pair("foo","bar"),"baz")).is("Invalid type passed to formData():  java.lang.String");
	}

	@Test
	public void a05_formDataPairs_Objects() throws Exception {
		List<String> l1 = AList.of("bar1","bar2"), l2 = AList.of("qux1","qux2");

		client().formDataPairs("foo","bar","baz","qux").build().post("/formData").run().assertBody().is("foo=bar&baz=qux");
		client().formDataPairs("foo",l1,"baz",l2).build().post("/formData").run().assertBody().asString().urlDecode().is("foo=bar1,bar2&baz=qux1,qux2");

		client().build().post("/formData").formDataPairs("foo","bar","baz","qux").run().assertBody().is("foo=bar&baz=qux");
		client().build().post("/formData").formDataPairs("foo",l1,"baz",l2).run().assertBody().asString().urlDecode().is("foo=bar1,bar2&baz=qux1,qux2");

		assertThrown(()->client().formDataPairs("foo","bar","baz")).is("Odd number of parameters passed into formDataPairs()");
		assertThrown(()->client().build().post("").formDataPairs("foo","bar","baz")).is("Odd number of parameters passed into formDataPairs()");
	}

	@Test
	public void a06_formData_String_Object_Schema() throws Exception {
		List<String> l = AList.of("bar","baz"), l2 = AList.of("qux","quux");
		client().formData("foo",l,T_ARRAY_PIPES).build().post("/formData").formData("foo",l2,T_ARRAY_PIPES).run().assertBody().asString().urlDecode().is("foo=bar|baz&foo=qux|quux");
	}

	@Test
	public void a07_formData_String_Object_Schema_Serializer() throws Exception {
		List<String> l = AList.of("bar","baz");
		client().formData("foo",l,T_ARRAY_PIPES,UonSerializer.DEFAULT).build().post("/formData").run().assertBody().asString().urlDecode().is("foo=@(bar,baz)");
	}

	@Test
	public void a08_formData_AddFlag_String_Object_Schema() throws Exception {
		List<String> l = AList.of("qux","quux");
		client().formData("foo","bar").build().post("/formData").formData(APPEND,"foo",l,T_ARRAY_PIPES).run().assertBody().asString().urlDecode().is("foo=bar&foo=qux|quux");
		client().formData("foo","bar").build().post("/formData").formData(PREPEND,"foo",l,T_ARRAY_PIPES).run().assertBody().asString().urlDecode().is("foo=qux|quux&foo=bar");
		client().formData("foo","bar").build().post("/formData").formData(REPLACE,"foo",l,T_ARRAY_PIPES).run().assertBody().asString().urlDecode().is("foo=qux|quux");
	}

	@Test
	public void a09_formData_String_Supplier() throws Exception {
		TestSupplier s = TestSupplier.of(null);

		RestClient x1 = client().formData("foo",s).build();
		s.set(OList.of("foo","bar"));
		x1.post("/formData").run().assertBody().asString().urlDecode().is("foo=foo,bar");
		s.set(OList.of("bar","baz"));
		x1.post("/formData").run().assertBody().asString().urlDecode().is("foo=bar,baz");

		RestClient x2 = client().build();
		s.set(OList.of("foo","bar"));
		x2.post("/formData").formData("foo",s).run().assertBody().asString().urlDecode().is("foo=foo,bar");
		s.set(OList.of("bar","baz"));
		x2.post("/formData").formData("foo",s).run().assertBody().asString().urlDecode().is("foo=bar,baz");
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
	public void a10_formData_String_Supplier_Schema_Serializer() throws Exception {
		TestSupplier s = TestSupplier.of(OList.of("foo","bar"));
		RestClient x = client().formData("foo",s,T_ARRAY_PIPES,new A8()).build();
		x.post("/formData").run().assertBody().asString().urlDecode().is("foo=x['foo','bar']");
		s.set(OList.of("bar","baz"));
		x.post("/formData").run().assertBody().asString().urlDecode().is("foo=x['bar','baz']");
	}

	@Test
	public void a11_formData_String_Supplier_Schema() throws Exception {
		List<String> l1 = AList.of("foo","bar"), l2 = AList.of("bar","baz");
		TestSupplier s = TestSupplier.of(null);

		RestClient x1 = client().formData("foo",s,T_ARRAY_PIPES).build();
		s.set(l1);
		x1.post("/formData").run().assertBody().asString().urlDecode().is("foo=foo|bar");
		s.set(l2);
		x1.post("/formData").run().assertBody().asString().urlDecode().is("foo=bar|baz");

		RestClient x2 = client().build();
		s.set(l1);
		x2.post("/formData").formData("foo",s,T_ARRAY_PIPES).run().assertBody().asString().urlDecode().is("foo=foo|bar");
		s.set(l2);
		x2.post("/formData").formData("foo",s,T_ARRAY_PIPES).run().assertBody().asString().urlDecode().is("foo=bar|baz");
	}

	public static class A12 implements HttpPartSerializer {
		@Override
		public HttpPartSerializerSession createPartSession(SerializerSessionArgs args) {
			return new HttpPartSerializerSession() {
				@Override
				public String serialize(HttpPartType type, HttpPartSchema schema, Object value) throws SerializeException, SchemaValidationException {
					throw new SerializeException("bad");
				}
			};
		}
	}

	@Test
	public void a12_badSerialization() throws Exception {
		assertThrown(()->client().formData(SerializedNameValuePair.of("Foo","bar").serializer(new A12())).build().get()).contains("bad");
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
