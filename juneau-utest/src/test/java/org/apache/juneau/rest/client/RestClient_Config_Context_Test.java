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

import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;
import static org.apache.juneau.serializer.Serializer.*;
import static org.apache.juneau.json.JsonSerializer.*;
import static org.apache.juneau.rest.mock.MockRestClient.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.json.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.transform.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RestClient_Config_Context_Test {

	@Rest
	public static class A extends BasicRestObject {
		@RestPost
		public Reader echoBody(org.apache.juneau.rest.RestRequest req) throws IOException {
			return req.getBody().getReader();
		}
	}


	public static class A1 {
		public String foo;
	}

	@Test
	public void a01_addMap() throws Exception {
		client().add(OMap.of(SERIALIZER_keepNullProperties,true)).build().post("/echoBody",new A1()).run().cacheBody().assertBody().is("{foo:null}").getBody().asType(A1.class);
	}

	public static class A2 {
		public String foo;
		@Override
		public String toString() {
			return foo;
		}
		public static A2 fromString(String s) {
			A2 p2 = new A2();
			p2.foo = s;
			return p2;
		}
	}

	@Test
	public void a02_addToStringObject() throws Exception {
		client().addTo(BEAN_notBeanClasses,A2.class).build().post("/echoBody",A2.fromString("bar")).run().cacheBody().assertBody().is("'bar'").getBody().asType(A2.class);
	}

	public static class A3a {
		public int foo;
		static A3a get() {
			A3a x = new A3a();
			x.foo = 1;
			return x;
		}
	}

	public static class A3b extends PojoSwap<A3a,Integer> {
		@Override
		public Integer swap(BeanSession session, A3a o) { return o.foo; }
		@Override
		public A3a unswap(BeanSession session, Integer f, ClassMeta<?> hint) {return A3a.get(); }
	}

	@Test
	public void a03_appendToStringObject() throws Exception {
		A3a x = client().appendTo(BEAN_swaps,A3b.class).build().post("/echoBody",A3a.get()).run().cacheBody().assertBody().is("1").getBody().asType(A3a.class);
		assertEquals(1,x.foo);
	}

	@Test
	public void a04_prependToStringObject() throws Exception {
		A3a x = client().prependTo(BEAN_swaps,A3b.class).build().post("/echoBody",A3a.get()).run().cacheBody().assertBody().is("1").getBody().asType(A3a.class);
		assertEquals(1,x.foo);
	}

	public static class A5 {
		public int foo;
		static A5 get() {
			A5 x = new A5();
			x.foo = 1;
			return x;
		}
	}

	@Test
	public void a05_apply() throws Exception {
		MockRestClient.create(A.class).json().apply(SimpleJsonSerializer.DEFAULT.getContextProperties()).build().post("/echoBody",A5.get()).run().cacheBody().assertBody().is("{foo:1}").getBody().asType(A5.class);
	}

	public static class A6a {
		public int foo,bar,baz;
		static A6a get() {
			A6a x = new A6a();
			x.foo = 1;
			x.bar = 2;
			x.baz = 3;
			return x;
		}
	}

	@org.apache.juneau.annotation.Bean(sort=true,on="A6a")
	public static class A6b {}

	@BeanConfig(sortProperties="true")
	public static class A6c {}

	public static class A6d {
		@BeanConfig(sortProperties="true")
		public void foo() {}
	}

	@Test
	public void a06_applyAnnotations() throws Exception {
		new A6b();
		new A6c();
		new A6d().foo();
		client().applyAnnotations(A6b.class).build().post("/echoBody",A6a.get()).run().cacheBody().assertBody().is("{bar:2,baz:3,foo:1}").getBody().asType(A6a.class);
		client().applyAnnotations(A6c.class).build().post("/echoBody",A6a.get()).run().cacheBody().assertBody().is("{bar:2,baz:3,foo:1}").getBody().asType(A6a.class);
		client().applyAnnotations(A6d.class.getMethod("foo")).build().post("/echoBody",A6a.get()).run().cacheBody().assertBody().is("{bar:2,baz:3,foo:1}").getBody().asType(A6a.class);
		AnnotationList al = ClassInfo.of(A6c.class).getAnnotationList(ConfigAnnotationFilter.INSTANCE);
		VarResolverSession vr = VarResolver.DEFAULT.createSession();
		client().applyAnnotations(al,vr).build().post("/echoBody",A6a.get()).run().cacheBody().assertBody().is("{bar:2,baz:3,foo:1}").getBody().asType(A6a.class);
	}

	@Test
	public void a07_removeFrom() throws Exception {
		A3a x = client().appendTo(BEAN_swaps,A3b.class).removeFrom(BEAN_swaps,A3b.class).build().post("/echoBody",A3a.get()).run().cacheBody().assertBody().is("{foo:1}").getBody().asType(A3a.class);
		assertEquals(1,x.foo);
	}

	@Test
	public void a08_setStringObject() throws Exception {
		MockRestClient.create(A.class).json().set(JSON_simpleMode,true).build().post("/echoBody",A3a.get()).run().cacheBody().assertBody().is("{foo:1}").getBody().asType(A3a.class);
	}

	@Test
	public void a09_annotations() throws Exception {
		client().annotations(BeanAnnotation.create(A6a.class).sort(true).build()).build().post("/echoBody",A6a.get()).run().cacheBody().assertBody().is("{bar:2,baz:3,foo:1}").getBody().asType(A6a.class);
	}

	public static interface A10a {
		void setFoo(int foo);
		int getFoo();
	}

	public static class A10b implements A10a {
		private int foo;
		@Override
		public int getFoo() {
			return foo;
		}
		@Override
		public void setFoo(int foo) {
			this.foo = foo;
		}
	}

	@Test
	public void a10_putAllTo() throws Exception {
		A10a x = client().implClass(A10a.class,A10b.class).build().post("/echoBody",new StringReader("{foo:1}")).run().getBody().asType(A10a.class);
		assertEquals(1,x.getFoo());
		assertTrue(x instanceof A10b);
	}

	public static class A11 {
		public int foo = 1;
	}

	@Test
	public void a11_set() throws Exception {
		client(null)
			.add(
				AMap.of(
					JSON_simpleMode,true,
					WSERIALIZER_quoteChar,"'",
					MOCKRESTCLIENT_restBean,A.class
				)
			)
			.json().build().post("/echoBody",new A11()).mediaType("text/json").run().assertBody().is("{foo:1}")
		;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static RestClientBuilder client() {
		return MockRestClient.create(A.class).simpleJson();
	}

	private static RestClientBuilder client(Class<?> c) {
		return MockRestClient.create(c).simpleJson();
	}
}
