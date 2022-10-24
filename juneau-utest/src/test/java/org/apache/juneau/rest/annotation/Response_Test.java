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
package org.apache.juneau.rest.annotation;

import static org.apache.juneau.internal.CollectionUtils.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.json.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;

@SuppressWarnings({"serial"})
@FixMethodOrder(NAME_ASCENDING)
public class Response_Test {

	//-----------------------------------------------------------------------------------------------------------------
	// HTTP status code
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A {
		@RestGet
		public A1 a() {
			return new A1();
		}
		@RestGet
		public String b() throws A2 {
			throw new A2();
		}
	}

	@Response @StatusCode(201)
	public static class A1 {
		@Override
		public String toString() {return "foo";}
	}

	@Response @StatusCode(501)
	public static class A2 extends Exception {
		@Override
		public String toString() {return "foo";}
	}

	@Test
	public void a01_httpstatusCodes() throws Exception {
		RestClient a = MockRestClient.buildLax(A.class);
		a.get("/a")
			.run()
			.assertStatus(201)
			.assertContent("foo");
		a.get("/b")
			.run()
			.assertStatus(501);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// OpenApiSerializer
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(serializers=OpenApiSerializer.class,defaultAccept="text/openapi")
	public static class B {
		@Response
		@RestGet
		public String a() {
			return "foo";
		}
		@RestGet
		public B1 b() {
			return new B1();
		}
		@RestGet
		public String c() throws B2 {
			throw new B2();
		}
		@RestGet
		public void d(@Response Value<String> value) {
			value.set("foo");
		}
	}

	@Response
	public static class B1 {
		@Override
		public String toString() {return "foo";}
	}

	@Response
	public static class B2 extends Exception {
		@Override
		public String toString() {return "foo";}
	}

	@Test
	public void b01_openApi() throws Exception {
		RestClient b = MockRestClient.buildLax(B.class);
		b.get("/a")
			.run()
			.assertStatus(200)
			.assertContent("foo");
		b.get("/b")
			.run()
			.assertStatus(200)
			.assertContent("foo");
		b.get("/c")
			.run()
			.assertStatus(500)
			.assertContent("foo");
		b.get("/d")
			.run()
			.assertStatus(200)
			.assertContent("foo");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// OpenAPI with schema
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(serializers=OpenApiSerializer.class,defaultAccept="text/openapi")
	public static class D {
		@Response(schema=@Schema(collectionFormat="pipes"))
		@RestGet
		public String[] a() {
			return new String[]{"foo","bar"};
		}
		@Response(schema=@Schema(type="string",format="byte"))
		@RestGet
		public byte[] b() {
			return "foo".getBytes();
		}
		@RestGet
		public D1 c() {
			return new D1();
		}
		@RestGet
		public D2 d() {
			return new D2();
		}
		@RestGet
		public String e() throws D3 {
			throw new D3();
		}
		@RestGet
		public String f() throws D4 {
			throw new D4();
		}
		@RestGet
		public void g(@Response(schema=@Schema(collectionFormat="pipes")) Value<String[]> value) {
			value.set(new String[]{"foo","bar"});
		}
		@RestGet
		public void h(@Response(schema=@Schema(type="string",format="byte")) Value<byte[]> value) {
			value.set("foo".getBytes());
		}
	}

	@Response(schema=@Schema(type="array",collectionFormat="pipes"))
	public static class D1 {
		public String[] toStringArray() {
			return new String[]{"foo","bar"};
		}
	}

	@Response(schema=@Schema(format="byte"))
	public static class D2 {
		public byte[] toByteArray() {
			return "foo".getBytes();
		}
	}

	@Response(schema=@Schema(type="array",collectionFormat="pipes"))
	public static class D3 extends Exception {
		public String[] toStringArray() {
			return new String[]{"foo","bar"};
		}
	}

	@Response(schema=@Schema(format="byte"))
	public static class D4 extends Exception {
		public byte[] toByteArray() {
			return "foo".getBytes();
		}
	}

	@Test
	public void d01_openApi_withSchema() throws Exception {
		RestClient d = MockRestClient.buildLax(D.class);
		d.get("/a")
			.run()
			.assertStatus(200)
			.assertContent("foo|bar");
		d.get("/b")
			.run()
			.assertStatus(200)
			.assertContent("Zm9v");
		d.get("/c")
			.run()
			.assertStatus(200)
			.assertContent("foo|bar");
		d.get("/d")
			.run()
			.assertStatus(200)
			.assertContent("Zm9v");
		d.get("/e")
			.run()
			.assertStatus(500)
			.assertContent("foo|bar");
		d.get("/f")
			.run()
			.assertStatus(500)
			.assertContent("Zm9v");
		d.get("/g")
			.run()
			.assertStatus(200)
			.assertContent("foo|bar");
		d.get("/h")
			.run()
			.assertStatus(200)
			.assertContent("Zm9v");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// No serializers
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class E {
		@RestGet
		public void a(@Response Value<E1> body) {
			body.set(new E1());
		}
		@RestGet
		public void b(Value<E2> body) {
			body.set(new E2());
		}
		@RestGet
		@Response
		public E1 c() {
			return new E1();
		}
		@RestGet
		public E2 d() {
			return new E2();
		}
	}

	public static class E1 {
		@Override
		public String toString() {return "foo";}
	}

	@Response
	public static class E2 {
		@Override
		public String toString() {return "foo";}
	}

	@Test
	public void e01_defaultSerialization() throws Exception {
		RestClient e = MockRestClient.build(E.class);
		e.get("/a")
			.run()
			.assertStatus(200)
			.assertContent("foo");
		e.get("/b")
			.run()
			.assertStatus(200)
			.assertContent("foo");
		e.get("/c")
			.run()
			.assertStatus(200)
			.assertContent("foo");
		e.get("/d")
			.run()
			.assertStatus(200)
			.assertContent("foo");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// JSON Accept
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(serializers=Json5Serializer.class)
	public static class G {
		@RestGet
		public void a(@Response Value<List<Integer>> body) {
			body.set(list(1,2));
		}
		@RestGet
		public void b(Value<G1> body) {
			body.set(new G1());
		}
		@RestGet
		@Response
		public List<Integer> c() {
			return list(1,2);
		}
		@RestGet
		public G1 d() {
			return new G1();
		}
	}

	@Response
	public static class G1 extends ArrayList<Integer> {
		public G1() {
			add(1);
			add(2);
		}
	}

	@Test
	public void g01_json() throws Exception {
		RestClient g = MockRestClient.build(G.class);
		g.get("/a").json()
			.run()
			.assertStatus(200)
			.assertContent("[1,2]");
		g.get("/b").json()
			.run()
			.assertStatus(200)
			.assertContent("[1,2]");
		g.get("/c").json()
			.run()
			.assertStatus(200)
			.assertContent("[1,2]");
		g.get("/d").json()
			.run()
			.assertStatus(200)
			.assertContent("[1,2]");
	}
}
