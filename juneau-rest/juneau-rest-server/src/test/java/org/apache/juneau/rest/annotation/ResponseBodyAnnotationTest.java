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

import static org.apache.juneau.testutils.TestUtils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.json.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.utils.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests the @Response annotation.
 */
@SuppressWarnings({"javadoc","serial"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ResponseBodyAnnotationTest {

	//=================================================================================================================
	// Setup
	//=================================================================================================================

	private static Swagger getSwagger(Object resource) {
		try {
			RestContext rc = RestContext.create(resource).build();
			RestRequest req = rc.getCallHandler().createRequest(new MockServletRequest());
			RestInfoProvider ip = rc.getInfoProvider();
			return ip.getSwagger(req);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}


	//=================================================================================================================
	// Basic tests
	//=================================================================================================================

	//-----------------------------------------------------------------------------------------------------------------
	// Basic
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource
	public static class A {

		@RestMethod
		public void m01(@ResponseBody Value<A01> body) {
			body.set(new A01());
		}

		@RestMethod
		public void m02(Value<A02> body) {
			body.set(new A02());
		}

		@RestMethod
		@ResponseBody
		public A01 m03() {
			return new A01();
		}

		@RestMethod
		public A02 m04() {
			return new A02();
		}
	}

	public static class A01 {
		@Override
		public String toString() {return "foo";}
	}

	@ResponseBody
	public static class A02 {
		@Override
		public String toString() {return "foo";}
	}

	static MockRest a = MockRest.create(A.class);

	@Test
	public void a01_basic_onParameter() throws Exception {
		a.get("/m01").execute().assertStatus(200).assertBody("foo");
	}
	@Test
	public void a02_basic_onType() throws Exception {
		a.get("/m02").execute().assertStatus(200).assertBody("foo");
	}
	@Test
	public void a03_basic_onMethod() throws Exception {
		a.get("/m03").execute().assertStatus(200).assertBody("foo");
	}
	@Test
	public void a04_basic_onReturnedType() throws Exception {
		a.get("/m04").execute().assertStatus(200).assertBody("foo");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Basic swagger
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource
	public static class B {

		@RestMethod
		public void m01(@ResponseBody(schema=@Schema(description="b01", collectionFormat="pipes")) Value<List<Integer>> body) {
			body.set(AList.create(1,2));
		}

		@RestMethod
		public void m02(Value<B01> body) {
			body.set(new B01());
		}

		@RestMethod
		@ResponseBody(schema=@Schema(description="b03", collectionFormat="pipes"))
		public List<Integer> m03() {
			return AList.create(1,2);
		}

		@RestMethod
		public B01 m04() {
			return new B01();
		}
	}

	@ResponseBody(schema=@Schema(description="b01", collectionFormat="pipes"))
	public static class B01 extends ArrayList<Integer> {
		public B01() {
			add(1);
			add(2);
		}
	}

	static MockRest b = MockRest.create(B.class);
	static Swagger sb = getSwagger(new B());

	@Test
	public void b01a_basic_onParameter() throws Exception {
		b.get("/m01").execute().assertStatus(200).assertBody("1|2");
	}
	@Test
	public void b01b_basic_onParameter_swagger() throws Exception {
		ResponseInfo ri = sb.getResponseInfo("/m01", "get", 200);
		assertObjectEquals("{description:'OK',schema:{description:'b01',collectionFormat:'pipes'}}", ri);
	}
	@Test
	public void b02a_basic_onType() throws Exception {
		b.get("/m02").execute().assertStatus(200).assertBody("1|2");
	}
	@Test
	public void b02b_basic_onParameter_swagger() throws Exception {
		ResponseInfo ri = sb.getResponseInfo("/m02", "get", 200);
		assertObjectEquals("{description:'OK',schema:{description:'b01',collectionFormat:'pipes'}}", ri);
	}
	@Test
	public void b03a_basic_onMethod() throws Exception {
		b.get("/m03").execute().assertStatus(200).assertBody("1|2");
	}
	@Test
	public void b03b_basic_onParameter_swagger() throws Exception {
		ResponseInfo ri = sb.getResponseInfo("/m03", "get", 200);
		assertObjectEquals("{description:'OK',schema:{description:'b03',collectionFormat:'pipes'}}", ri);
	}
	@Test
	public void b04a_basic_onReturnedType() throws Exception {
		b.get("/m04").execute().assertStatus(200).assertBody("1|2");
	}
	@Test
	public void b04b_basic_onParameter_swagger() throws Exception {
		ResponseInfo ri = sb.getResponseInfo("/m04", "get", 200);
		assertObjectEquals("{description:'OK',schema:{description:'b01',collectionFormat:'pipes'}}", ri);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test JSON Accept
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource(serializers=SimpleJsonSerializer.class)
	public static class C {

		@RestMethod
		public void m01(@ResponseBody Value<List<Integer>> body) {
			body.set(AList.create(1,2));
		}

		@RestMethod
		public void m02(Value<C01> body) {
			body.set(new C01());
		}

		@RestMethod
		@ResponseBody
		public List<Integer> m03() {
			return AList.create(1,2);
		}

		@RestMethod
		public C01 m04() {
			return new C01();
		}
	}

	@ResponseBody
	public static class C01 extends ArrayList<Integer> {
		public C01() {
			add(1);
			add(2);
		}
	}

	static MockRest c = MockRest.create(C.class);
	static Swagger sc = getSwagger(new C());

	@Test
	public void c01a_basic_onParameter() throws Exception {
		c.get("/m01").json().execute().assertStatus(200).assertBody("[1,2]");
	}
	@Test
	public void c01b_basic_onParameter_swagger() throws Exception {
		ResponseInfo ri = sc.getResponseInfo("/m01", "get", 200);
		assertObjectEquals("{description:'OK'}", ri);
	}
	@Test
	public void c02a_basic_onType() throws Exception {
		c.get("/m02").json().execute().assertStatus(200).assertBody("[1,2]");
	}
	@Test
	public void c02b_basic_onParameter_swagger() throws Exception {
		ResponseInfo ri = sc.getResponseInfo("/m02", "get", 200);
		assertObjectEquals("{description:'OK'}", ri);
	}
	@Test
	public void c03a_basic_onMethod() throws Exception {
		c.get("/m03").json().execute().assertStatus(200).assertBody("[1,2]");
	}
	@Test
	public void c03b_basic_onParameter_swagger() throws Exception {
		ResponseInfo ri = sc.getResponseInfo("/m03", "get", 200);
		assertObjectEquals("{description:'OK'}", ri);
	}
	@Test
	public void c04a_basic_onReturnedType() throws Exception {
		c.get("/m04").json().execute().assertStatus(200).assertBody("[1,2]");
	}
	@Test
	public void c04b_basic_onParameter_swagger() throws Exception {
		ResponseInfo ri = sc.getResponseInfo("/m04", "get", 200);
		assertObjectEquals("{description:'OK'}", ri);
	}


	//=================================================================================================================
	// PartSerializers
	//=================================================================================================================

	public static class XPartSerializer implements HttpPartSerializer {
		@Override
		public HttpPartSerializerSession createSession(SerializerSessionArgs args) {
			return new HttpPartSerializerSession() {
				@Override
				public String serialize(HttpPartType partType, HttpPartSchema schema, Object value) throws SerializeException, SchemaValidationException {
					return "x" + value + "x";
				}
			};
		}

		@Override
		public String serialize(HttpPartType partType, HttpPartSchema schema, Object value) throws SchemaValidationException, SerializeException {
			return createSession(null).serialize(partType, schema, value);
		}

		@Override
		public String serialize(HttpPartSchema schema, Object value) throws SchemaValidationException, SerializeException {
			return createSession(null).serialize(null, schema, value);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @ResponseBody(usePartSerializer), partSerializer on class
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource(partSerializer=XPartSerializer.class)
	public static class D {

		@RestMethod
		public void m01(@ResponseBody(usePartSerializer=true) Value<List<Integer>> body) {
			body.set(AList.create(1,2));
		}

		@RestMethod
		public void m02(Value<D01> body) {
			body.set(new D01());
		}

		@RestMethod
		@ResponseBody(usePartSerializer=true)
		public List<Integer> m03() {
			return AList.create(1,2);
		}

		@RestMethod
		public D01 m04() {
			return new D01();
		}
	}

	@ResponseBody(usePartSerializer=true)
	public static class D01 extends ArrayList<Integer> {
		public D01() {
			add(1);
			add(2);
		}
	}

	static MockRest d = MockRest.create(D.class);
	static Swagger sd = getSwagger(new D());

	@Test
	public void d01a_basic_onParameter() throws Exception {
		d.get("/m01").execute().assertStatus(200).assertBody("x[1, 2]x");
	}
	@Test
	public void d01b_basic_onParameter_swagger() throws Exception {
		ResponseInfo ri = sd.getResponseInfo("/m01", "get", 200);
		assertObjectEquals("{description:'OK',schema:{type:'array',items:{type:'integer',format:'int32'}}}", ri);
	}
	@Test
	public void d02a_basic_onType() throws Exception {
		d.get("/m02").execute().assertStatus(200).assertBody("x[1, 2]x");
	}
	@Test
	public void d02b_basic_onParameter_swagger() throws Exception {
		ResponseInfo ri = sd.getResponseInfo("/m02", "get", 200);
		assertObjectEquals("{description:'OK',schema:{type:'array',items:{type:'integer',format:'int32'}}}", ri);
	}
	@Test
	public void d03a_basic_onMethod() throws Exception {
		d.get("/m03").execute().assertStatus(200).assertBody("x[1, 2]x");
	}
	@Test
	public void d03b_basic_onParameter_swagger() throws Exception {
		ResponseInfo ri = sd.getResponseInfo("/m03", "get", 200);
		assertObjectEquals("{description:'OK',schema:{type:'array',items:{type:'integer',format:'int32'}}}", ri);
	}
	@Test
	public void d04a_basic_onReturnedType() throws Exception {
		d.get("/m04").execute().assertStatus(200).assertBody("x[1, 2]x");
	}
	@Test
	public void d04b_basic_onParameter_swagger() throws Exception {
		ResponseInfo ri = sd.getResponseInfo("/m04", "get", 200);
		assertObjectEquals("{description:'OK',schema:{type:'array',items:{type:'integer',format:'int32'}}}", ri);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @ResponseBody(usePartSerializer), partSerializer on part.
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource
	public static class E {

		@RestMethod
		public void m01(@ResponseBody(partSerializer=XPartSerializer.class) Value<List<Integer>> body) {
			body.set(AList.create(1,2));
		}

		@RestMethod
		public void m02(Value<E01> body) {
			body.set(new E01());
		}

		@RestMethod
		@ResponseBody(partSerializer=XPartSerializer.class)
		public List<Integer> m03() {
			return AList.create(1,2);
		}

		@RestMethod
		public E01 m04() {
			return new E01();
		}
	}

	@ResponseBody(partSerializer=XPartSerializer.class)
	public static class E01 extends ArrayList<Integer> {
		public E01() {
			add(1);
			add(2);
		}
	}

	static MockRest e = MockRest.create(E.class);
	static Swagger se = getSwagger(new E());

	@Test
	public void e01a_basic_onParameter() throws Exception {
		e.get("/m01").execute().assertStatus(200).assertBody("x[1, 2]x");
	}
	@Test
	public void e01b_basic_onParameter_swagger() throws Exception {
		ResponseInfo ri = se.getResponseInfo("/m01", "get", 200);
		assertObjectEquals("{description:'OK',schema:{type:'array',items:{type:'integer',format:'int32'}}}", ri);
	}
	@Test
	public void e02a_basic_onType() throws Exception {
		e.get("/m02").execute().assertStatus(200).assertBody("x[1, 2]x");
	}
	@Test
	public void e02b_basic_onParameter_swagger() throws Exception {
		ResponseInfo ri = se.getResponseInfo("/m02", "get", 200);
		assertObjectEquals("{description:'OK',schema:{type:'array',items:{type:'integer',format:'int32'}}}", ri);
	}
	@Test
	public void e03a_basic_onMethod() throws Exception {
		e.get("/m03").execute().assertStatus(200).assertBody("x[1, 2]x");
	}
	@Test
	public void e03b_basic_onParameter_swagger() throws Exception {
		ResponseInfo ri = se.getResponseInfo("/m03", "get", 200);
		assertObjectEquals("{description:'OK',schema:{type:'array',items:{type:'integer',format:'int32'}}}", ri);
	}
	@Test
	public void e04a_basic_onReturnedType() throws Exception {
		e.get("/m04").execute().assertStatus(200).assertBody("x[1, 2]x");
	}
	@Test
	public void e04b_basic_onParameter_swagger() throws Exception {
		ResponseInfo ri = se.getResponseInfo("/m04", "get", 200);
		assertObjectEquals("{description:'OK',schema:{type:'array',items:{type:'integer',format:'int32'}}}", ri);
	}

}
