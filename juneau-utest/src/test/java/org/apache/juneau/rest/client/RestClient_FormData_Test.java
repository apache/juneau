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
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.junit.runners.MethodSorters.*;
import static org.apache.juneau.http.HttpParts.*;
import static org.apache.juneau.testutils.StreamUtils.*;

import java.io.*;
import java.util.*;

import org.apache.http.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.part.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.servlet.*;
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
			return reader(req.getFormParams().asQueryString());
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Method tests
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_formData_String_Object() throws Exception {
		client().formData("foo","bar").formData(part("foo",new StringBuilder("baz"),null)).build().post("/formData").run().assertContent("foo=bar&foo=baz");
		client().build().post("/formData").formData("foo","bar").formData("foo",new StringBuilder("baz")).run().assertContent("foo=bar&foo=baz");
		client().build().post("/formData").formData(null,"bar").run().assertContent("");
		client().build().post("/formData").formData("foo",null).run().assertContent("");
		client().build().post("/formData").formData(null,(String)null).run().assertContent("");
	}

	@Test
	public void a03_formData_NameValuePair() throws Exception {
		client().formData(part("foo","bar")).build().post("/formData").formData(part("foo","baz")).run().assertContent("foo=bar&foo=baz");
	}

	@Test
	public void a04_formDatas_Objects() throws Exception {
		client().formData(part("foo","bar")).build().post("/formData").run().assertContent("foo=bar");
		client().formData(parts("foo","bar","foo","baz").getAll()).build().post("/formData").run().assertContent("foo=bar&foo=baz");
		client().formData(part("foo","bar"),part("foo","baz")).build().post("/formData").run().assertContent("foo=bar&foo=baz");

		client().build().post("/formData").formData(part("foo","bar")).run().assertContent("foo=bar");
		client().build().post("/formData").formData(part("foo","bar"),part("foo","baz")).run().assertContent("foo=bar&foo=baz");

		client().build().post("/formData").formDataBean(ABean.get()).run().assertContent("a=1&b=foo");

		client().formData(part("foo","bar"),null).build().post("/formData").run().assertContent("foo=bar");
		client().formData(part("foo",null)).build().post("/formData").run().assertContent("");
		client().formData(part(null,null)).build().post("/formData").run().assertContent("");

		client().build().post("/formData").formData(part("foo",null)).run().assertContent("");
		client().build().post("/formData").formData(part(null,"foo")).run().assertContent("");
		client().build().post("/formData").formData(part(null,null)).run().assertContent("");

		client().formData(part("foo","bar",null)).build().post("/formData").run().assertContent("foo=bar");
		client().formData(part("foo",null,null)).build().post("/formData").run().assertContent("");
		client().formData(part("foo",null,null).skipIfEmpty().schema(HttpPartSchema.create()._default("bar").build())).build().post("/formData").run().assertContent("foo=bar");
	}

	@Test
	public void a06_formData_String_Object_Schema() throws Exception {
		List<String> l = list("bar","baz"), l2 = list("qux","quux");
		client().formData(part("foo",l,T_ARRAY_PIPES)).build().post("/formData").formData(part("foo",l2,T_ARRAY_PIPES)).run().assertContent().asString().asUrlDecode().is("foo=bar|baz&foo=qux|quux");
	}

	@Test
	public void a07_formData_String_Object_Schema_Serializer() throws Exception {
		List<String> l = list("bar","baz");
		client().formData(part("foo",l,T_ARRAY_PIPES).serializer(UonSerializer.DEFAULT)).build().post("/formData").run().assertContent().asString().asUrlDecode().is("foo=@(bar,baz)");
	}

	@Test
	public void a09_formData_String_Supplier() throws Exception {
		TestSupplier s = TestSupplier.of(null);

		RestClient x1 = client().formData(part("foo",s,null)).build();
		s.set(JsonList.of("foo","bar"));
		x1.post("/formData").run().assertContent().asString().asUrlDecode().is("foo=foo,bar");
		s.set(JsonList.of("bar","baz"));
		x1.post("/formData").run().assertContent().asString().asUrlDecode().is("foo=bar,baz");

		RestClient x2 = client().build();
		s.set(JsonList.of("foo","bar"));
		x2.post("/formData").formData("foo",s).run().assertContent().asString().asUrlDecode().is("foo=foo,bar");
		s.set(JsonList.of("bar","baz"));
		x2.post("/formData").formData("foo",s).run().assertContent().asString().asUrlDecode().is("foo=bar,baz");
	}

	@Test
	public void a10_formData_String_Supplier_Schema_Serializer() throws Exception {
		TestSupplier s = TestSupplier.of(JsonList.of("foo","bar"));
		RestClient x = client().formData(part("foo",s,T_ARRAY_PIPES).serializer(MockWriterSerializer.X)).build();
		x.post("/formData").run().assertContent().asString().asUrlDecode().is("foo=xfoo|barx");
		s.set(JsonList.of("bar","baz"));
		x.post("/formData").run().assertContent().asString().asUrlDecode().is("foo=xbar|bazx");
	}

	@Test
	public void a11_formData_String_Supplier_Schema() throws Exception {
		List<String> l1 = list("foo","bar"), l2 = list("bar","baz");
		TestSupplier s = TestSupplier.of(null);

		RestClient x1 = client().formData(part("foo",s,T_ARRAY_PIPES)).build();
		s.set(l1);
		x1.post("/formData").run().assertContent().asString().asUrlDecode().is("foo=foo|bar");
		s.set(l2);
		x1.post("/formData").run().assertContent().asString().asUrlDecode().is("foo=bar|baz");

		RestClient x2 = client().build();
		s.set(l1);
		x2.post("/formData").formData(part("foo",s,T_ARRAY_PIPES)).run().assertContent().asString().asUrlDecode().is("foo=foo|bar");
		s.set(l2);
		x2.post("/formData").formData(part("foo",s,T_ARRAY_PIPES)).run().assertContent().asString().asUrlDecode().is("foo=bar|baz");
	}

	public static class A12 implements HttpPartSerializer {
		@Override
		public HttpPartSerializerSession getPartSession() {
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
		assertThrown(()->client().formData(part("Foo","bar",null).serializer(new A12())).build().post("/").run()).asMessages().isContains("bad");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static NameValuePair part(String name, Object val) {
		return basicPart(name, val);
	}

	private static SerializedPart part(String name, Object val, HttpPartSchema schema) {
		return serializedPart(name, val).schema(schema);
	}

	private static PartList parts(String...pairs) {
		return partList(pairs);
	}

	private static RestClient.Builder client() {
		return MockRestClient.create(A.class).json5();
	}
}
