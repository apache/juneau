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

import static org.apache.juneau.internal.CollectionUtils.*;
import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

class RestClient_Config_Serializer_Test extends SimpleTestBase {

	public static class ABean {
		public int f;
		static ABean get() {
			var x = new ABean();
			x.f = 1;
			return x;
		}
	}

	private static ABean bean = ABean.get();

	@Rest
	public static class A extends BasicRestObject {
		@RestPost
		public Reader echoBody(org.apache.juneau.rest.RestRequest req) throws IOException {
			return req.getContent().getReader();
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Serializer properties
	//-----------------------------------------------------------------------------------------------------------------

	public static class A1 {
		public Object f1;
		static A1 get() {
			var x = new A1();
			x.f1 = A2.get();
			return x;
		}
	}

	@Test void a01_addBeanTypes() throws Exception {
		A1 l1 = A1.get();
		client().addBeanTypes().build().post("/echoBody",l1).run().assertContent("{f1:{_type:'L',f2:1}}");
	}

	@org.apache.juneau.annotation.Bean(typeName="L")
	public static class A2 {
		public int f2;
		static A2 get() {
			var x = new A2();
			x.f2 = 1;
			return x;
		}
	}

	@Test void a02_addRootType() throws Exception {
		A2 l2 = A2.get();
		client().addBeanTypes().addRootType().build().post("/echoBody",l2).run().assertContent("{_type:'L',f2:1}");
	}

	@Test void a03_detectRecursions() {
		var l1 = new A1();
		l1.f1 = l1;
		assertThrowsWithMessage(Exception.class, "Recursion occurred", ()->client().detectRecursions().build().post("/echoBody",l1).run());
	}

	@Test void a04_ignoreRecursions() throws Exception {
		var l1 = new A1();
		l1.f1 = l1;
		client().ignoreRecursions().build().post("/echoBody",l1).run().assertContent("{}");
	}

	@Test void a05_initialDepth() throws Exception {
		client().initialDepth(2).ws().build().post("/echoBody",bean).run().assertContent("\t\t{\n\t\t\tf: 1\n\t\t}");
	}

	public static class A6 {
		public ABean f;
		static A6 get() {
			var x = new A6();
			x.f = bean;
			return x;
		}
	}

	@Test void a06_maxDepth() throws Exception {
		client().maxDepth(1).build().post("/echoBody",A6.get()).run().assertContent("{}");
	}

	@Test void a07_sortCollections() throws Exception {
		String[] x = {"c","a","b"};
		client().sortCollections().build().post("/echoBody",x).run().assertContent("['a','b','c']");
	}

	@Test void a08_sortMapsBoolean() throws Exception {
		Map<String,Integer> x = map("c",3,"a",1,"b",2);
		client().sortMaps().build().post("/echoBody",x).run().assertContent("{a:1,b:2,c:3}");
	}

	public static class A9 {
		public List<String> f1 = list();
		public String[] f2 = {};
	}

	@Test void a09_trimEmptyCollections() throws Exception {
		var x = new A9();
		client().trimEmptyCollections().build().post("/echoBody",x).run().assertContent("{}");
	}

	public static class A10 {
		public Map<String,String> f1 = map();
		public JsonMap f2 = JsonMap.create();
	}

	@Test void a10_trimEmptyMaps() throws Exception {
		var x = new A10();
		client().trimEmptyMaps().build().post("/echoBody",x).run().assertContent("{}");
	}

	public static class A11 {
		public String f;
	}

	@Test void a11_trimNullPropertiesBoolean() throws Exception {
		var x = new A11();
		client().keepNullProperties().build().post("/echoBody",x).run().assertContent("{f:null}");
	}

	public static class A12 {
		public String f = " foo ";
	}

	@Test void a12_trimStringsOnWrite() throws Exception {
		var x = new A12();
		client().trimStringsOnWrite().build().post("/echoBody",x).run().assertContent("{f:'foo'}");
	}

	public static class A13 {
		@Uri
		public String f = "foo";
	}

	@Test void a13_uriContext_uriResolution_uriRelativity() throws Exception {
		var x = new A13();
		client().uriResolution(UriResolution.ABSOLUTE).uriRelativity(UriRelativity.PATH_INFO).uriContext(UriContext.of("http://localhost:80","/context","/resource","/path")).build().post("/echoBody",x).run().assertContent("{f:'http://localhost:80/context/resource/foo'}");
		client().uriResolution(UriResolution.NONE).uriRelativity(UriRelativity.RESOURCE).uriContext(UriContext.of("http://localhost:80","/context","/resource","/path")).build().post("/echoBody",x).run().assertContent("{f:'foo'}");
	}

	public static class A14 {
		public int f1;
		public A14 f2;

		static A14 get() {
			var x = new A14();
			var x2 = new A14();
			var x3 = new A14();
			x.f1 = 1;
			x2.f1 = 2;
			x3.f1 = 3;
			x.f2 = x2;
			x2.f2 = x3;
			return x;
		}
	}

	@Test void a14_maxIndent() throws Exception {
		A14 x = A14.get();
		client().maxIndent(2).ws().build().post("/echoBody",x).run().assertContent("{\n\tf1: 1,\n\tf2: {\n\t\tf1: 2,\n\t\tf2: {f1:3}\n\t}\n}");
	}

	public static class A15 {
		public String f1 = "foo";
	}

	@Test void a15_quoteChar() throws Exception {
		var x = new A15();
		MockRestClient.create(A.class).json().quoteChar('\'').build().post("/echoBody",x).run().assertContent("{'f1':'foo'}");
		MockRestClient.create(A.class).json().quoteChar('|').build().post("/echoBody",x).run().assertContent("{|f1|:|foo|}");
	}

	@Test void a16_sq() throws Exception {
		var x = new A15();
		MockRestClient.create(A.class).json().sq().build().post("/echoBody",x).run().assertContent("{'f1':'foo'}");
		client().sq().build().post("/echoBody",x).run().assertContent("{f1:'foo'}");
	}

	@Test void a17_useWhitespace() throws Exception {
		var x = new A15();
		client().ws().build().post("/echoBody",x).run().assertContent("{\n\tf1: 'foo'\n}");
		client().useWhitespace().build().post("/echoBody",x).run().assertContent("{\n\tf1: 'foo'\n}");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static RestClient.Builder client() {
		return MockRestClient.create(A.class).json5();
	}
}