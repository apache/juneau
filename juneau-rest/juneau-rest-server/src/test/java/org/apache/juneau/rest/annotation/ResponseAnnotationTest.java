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

import static org.apache.juneau.rest.testutils.TestUtils.*;
import static org.junit.Assert.assertEquals;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.json.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.testutils.*;
import org.apache.juneau.utils.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests the @Response annotation.
 */
@SuppressWarnings({"javadoc","serial"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ResponseAnnotationTest {

	//=================================================================================================================
	// Status codes
	//=================================================================================================================

	//-----------------------------------------------------------------------------------------------------------------
	// HTTP status code
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource
	public static class A {
		@RestMethod
		public A01 a01() {
			return new A01();
		}
		@RestMethod
		public String a02() throws A02 {
			throw new A02();
		}
	}

	@Response(code=201)
	public static class A01 {
		@Override
		public String toString() {return "foo";}
	}

	@Response(code=501)
	public static class A02 extends Exception {
		@Override
		public String toString() {return "foo";}
	}

	static MockRest a = MockRest.create(A.class);

	@Test
	public void a01_codeOnClass() throws Exception {
		a.get("/a01").execute().assertStatus(201).assertBody("foo");
	}
	@Test
	public void a02_codeOnThrown() throws Exception {
		a.get("/a02").execute().assertStatus(501);
	}

	//=================================================================================================================
	// PartSerializers
	//=================================================================================================================

	//-----------------------------------------------------------------------------------------------------------------
	// @Response(usePartSerializer)
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource(partSerializer=XPartSerializer.class)
	public static class B {

		@Response(usePartSerializer=true)
		@RestMethod
		public String b01() {
			return "foo";
		}
		@Response(usePartSerializer=false)
		@RestMethod
		public String b02() {
			return "foo";
		}
		@RestMethod
		public B03 b03() {
			return new B03();
		}
		@RestMethod
		public B04 b04() {
			return new B04();
		}
		@RestMethod
		public String b05() throws B05 {
			throw new B05();
		}
		@RestMethod
		public String b06() throws B06 {
			throw new B06();
		}
		@RestMethod
		public void b07(@Response(usePartSerializer=true) Value<String> value) {
			value.set("foo");
		}
		@RestMethod
		public void b08(@Response(usePartSerializer=false) Value<String> value) {
			value.set("foo");
		}
	}

	@Response(usePartSerializer=true)
	public static class B03 {
		@Override
		public String toString() {return "foo";}
	}

	@Response(usePartSerializer=false)
	public static class B04 {
		@Override
		public String toString() {return "foo";}
	}

	@Response(usePartSerializer=true)
	public static class B05 extends Exception {
		@Override
		public String toString() {return "foo";}
	}

	@Response(usePartSerializer=false)
	public static class B06 extends Exception {
		@Override
		public String toString() {return "foo";}
	}

	static MockRest b = MockRest.create(B.class);

	@Test
	public void b01_useOnMethod() throws Exception {
		b.get("/b01").execute().assertStatus(200).assertBody("xfoox");
	}
	@Test
	public void b02_dontUseOnMethod() throws Exception {
		b.get("/b02").execute().assertStatus(200).assertBody("foo");
	}
	@Test
	public void b03_useOnClass() throws Exception {
		b.get("/b03").execute().assertStatus(200).assertBody("xfoox");
	}
	@Test
	public void b04_dontUseOnClass() throws Exception {
		b.get("/b04").execute().assertStatus(200).assertBody("foo");
	}
	@Test
	public void b05_useOnThrown() throws Exception {
		b.get("/b05").execute().assertStatus(500).assertBody("xfoox");
	}
	@Test
	public void b06_dontUseOnThrown() throws Exception {
		b.get("/b06").execute().assertStatus(500).assertBodyContains("foo");
	}
	@Test
	public void b07_useOnParameter() throws Exception {
		b.get("/b07").execute().assertStatus(200).assertBody("xfoox");
	}
	@Test
	public void b08_dontUseOnParameter() throws Exception {
		b.get("/b08").execute().assertStatus(200).assertBody("foo");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Response(partSerializer)
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource
	public static class C {

		@Response(partSerializer=XPartSerializer.class)
		@RestMethod
		public String c01() {
			return "foo";
		}
		@ResponseBody
		@RestMethod
		public String c02() {
			return "foo";
		}
		@RestMethod
		public C03 c03() {
			return new C03();
		}
		@RestMethod
		public C04 c04() {
			return new C04();
		}
		@RestMethod
		public String c05() throws C05 {
			throw new C05();
		}
		@RestMethod
		public String c06() throws C06 {
			throw new C06();
		}
		@RestMethod
		public void c07(@Response(partSerializer=XPartSerializer.class) Value<String> value) {
			value.set("foo");
		}
		@RestMethod
		public void c08(@Response Value<String> value) {
			value.set("foo");
		}
	}

	@Response(partSerializer=XPartSerializer.class)
	public static class C03 {
		@Override
		public String toString() {return "foo";}
	}

	@Response
	public static class C04 {
		@Override
		public String toString() {return "foo";}
	}

	@Response(partSerializer=XPartSerializer.class)
	public static class C05 extends Exception {
		@Override
		public String toString() {return "foo";}
	}

	@Response
	public static class C06 extends Exception {
		@Override
		public String toString() {return "foo";}
	}

	static MockRest c = MockRest.create(C.class);

	@Test
	public void c01_useOnMethod() throws Exception {
		c.get("/c01").execute().assertStatus(200).assertBody("xfoox");
	}
	@Test
	public void c02_dontUseOnMethod() throws Exception {
		c.get("/c02").execute().assertStatus(200).assertBody("foo");
	}
	@Test
	public void c03_useOnClass() throws Exception {
		c.get("/c03").execute().assertStatus(200).assertBody("xfoox");
	}
	@Test
	public void c04_dontUseOnClass() throws Exception {
		c.get("/c04").execute().assertStatus(200).assertBody("foo");
	}
	@Test
	public void c05_useOnThrown() throws Exception {
		c.get("/c05").execute().assertStatus(500).assertBody("xfoox");
	}
	@Test
	public void c06_dontUseOnThrown() throws Exception {
		c.get("/c06").execute().assertStatus(500).assertBodyContains("foo");
	}
	@Test
	public void c07_useOnParameter() throws Exception {
		c.get("/c07").execute().assertStatus(200).assertBody("xfoox");
	}
	@Test
	public void c08_dontUseOnParameter() throws Exception {
		c.get("/c08").execute().assertStatus(200).assertBody("foo");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Response(partSerializer) with schemas
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource
	public static class D {

		@Response(schema=@Schema(collectionFormat="pipes"),usePartSerializer=true)
		@RestMethod
		public String[] d01() {
			return new String[]{"foo","bar"};
		}
		@RestMethod
		public D02 d02() {
			return new D02();
		}
		@RestMethod
		public String d03() throws D03 {
			throw new D03();
		}
		@RestMethod
		public void d04(@Response(schema=@Schema(collectionFormat="pipes"),usePartSerializer=true) Value<String[]> value) {
			value.set(new String[]{"foo","bar"});
		}
		@Response(schema=@Schema(type="string",format="byte"),usePartSerializer=true)
		@RestMethod
		public byte[] d05() {
			return "foo".getBytes();
		}
		@RestMethod
		public D06 d06() {
			return new D06();
		}
		@RestMethod
		public String d07() throws D07 {
			throw new D07();
		}
		@RestMethod
		public void d08(@Response(schema=@Schema(type="string",format="byte"),usePartSerializer=true) Value<byte[]> value) {
			value.set("foo".getBytes());
		}
	}

	@Response(schema=@Schema(type="array",collectionFormat="pipes"),usePartSerializer=true)
	public static class D02 {
		public String[] toStringArray() {
			return new String[]{"foo","bar"};
		}
	}

	@Response(schema=@Schema(type="array",collectionFormat="pipes"),usePartSerializer=true)
	public static class D03 extends Exception {
		public String[] toStringArray() {
			return new String[]{"foo","bar"};
		}
	}

	@Response(schema=@Schema(format="byte"),usePartSerializer=true)
	public static class D06 {
		public byte[] toBytes() {
			return "foo".getBytes();
		}
	}

	@Response(schema=@Schema(format="byte"),usePartSerializer=true)
	public static class D07 extends Exception {
		public byte[] toBytes() {
			return "foo".getBytes();
		}
	}

	static MockRest d = MockRest.create(D.class);

	@Test
	public void d01_useOnMethod() throws Exception {
		d.get("/d01").execute().assertStatus(200).assertBody("foo|bar");
	}
	@Test
	public void d02_useOnClass() throws Exception {
		d.get("/d02").execute().assertStatus(200).assertBody("foo|bar");
	}
	@Test
	public void d03_useOnThrown() throws Exception {
		d.get("/d03").execute().assertStatus(500).assertBody("foo|bar");
	}
	@Test
	public void d04_useOnParameter() throws Exception {
		d.get("/d04").execute().assertStatus(200).assertBody("foo|bar");
	}
	@Test
	public void d05_useOnMethodBytes() throws Exception {
		d.get("/d05").execute().assertStatus(200).assertBody("Zm9v");
	}
	@Test
	public void d06_useOnClassBytes() throws Exception {
		d.get("/d06").execute().assertStatus(200).assertBody("Zm9v");
	}
	@Test
	public void d07_useOnThrownBytes() throws Exception {
		d.get("/d07").execute().assertStatus(500).assertBody("Zm9v");
	}
	@Test
	public void d08_useOnParameterBytes() throws Exception {
		d.get("/d08").execute().assertStatus(200).assertBody("Zm9v");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Basic
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource
	public static class E {
		@RestMethod
		public void e01(@Response Value<E01> body) {
			body.set(new E01());
		}
		@RestMethod
		public void e02(Value<E02> body) {
			body.set(new E02());
		}
		@RestMethod
		@Response
		public E01 e03() {
			return new E01();
		}
		@RestMethod
		public E02 e04() {
			return new E02();
		}
	}

	public static class E01 {
		@Override
		public String toString() {return "foo";}
	}

	@Response
	public static class E02 {
		@Override
		public String toString() {return "foo";}
	}

	static MockRest e = MockRest.create(E.class);

	@Test
	public void e01_basic_onParameter() throws Exception {
		e.get("/e01").execute().assertStatus(200).assertBody("foo");
	}
	@Test
	public void e02_basic_onType() throws Exception {
		e.get("/e02").execute().assertStatus(200).assertBody("foo");
	}
	@Test
	public void e03_basic_onMethod() throws Exception {
		e.get("/e03").execute().assertStatus(200).assertBody("foo");
	}
	@Test
	public void e04_basic_onReturnedType() throws Exception {
		e.get("/e04").execute().assertStatus(200).assertBody("foo");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Basic swagger
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource
	public static class F {
		@RestMethod
		public void f01(@Response(schema=@Schema(description="f01", collectionFormat="pipes")) Value<List<Integer>> body) {
			body.set(AList.create(1,2));
		}
		@RestMethod
		public void f02(Value<F01> body) {
			body.set(new F01());
		}
		@RestMethod
		@Response(schema=@Schema(description="f03", collectionFormat="pipes"))
		public List<Integer> f03() {
			return AList.create(1,2);
		}
		@RestMethod
		public F01 f04() {
			return new F01();
		}
	}

	@Response(schema=@Schema(description="f01", collectionFormat="pipes"))
	public static class F01 extends ArrayList<Integer> {
		public F01() {
			add(1);
			add(2);
		}
	}

	static MockRest f = MockRest.create(F.class);
	static Swagger sf = getSwagger(F.class);

	@Test
	public void f01a_basic_onParameter() throws Exception {
		f.get("/f01").execute().assertStatus(200).assertBody("1|2");
	}
	@Test
	public void f01b_basic_onParameter_swagger() throws Exception {
		ResponseInfo ri = sf.getResponseInfo("/f01", "get", 200);
		assertObjectEquals("{description:'OK',schema:{description:'f01',collectionFormat:'pipes'}}", ri);
	}
	@Test
	public void f02a_basic_onType() throws Exception {
		f.get("/f02").execute().assertStatus(200).assertBody("1|2");
	}
	@Test
	public void f02b_basic_onParameter_swagger() throws Exception {
		ResponseInfo ri = sf.getResponseInfo("/f02", "get", 200);
		assertObjectEquals("{description:'OK',schema:{description:'f01',collectionFormat:'pipes'}}", ri);
	}
	@Test
	public void f03a_basic_onMethod() throws Exception {
		f.get("/f03").execute().assertStatus(200).assertBody("1|2");
	}
	@Test
	public void f03b_basic_onParameter_swagger() throws Exception {
		ResponseInfo ri = sf.getResponseInfo("/f03", "get", 200);
		assertObjectEquals("{description:'OK',schema:{description:'f03',collectionFormat:'pipes'}}", ri);
	}
	@Test
	public void f04a_basic_onReturnedType() throws Exception {
		f.get("/f04").execute().assertStatus(200).assertBody("1|2");
	}
	@Test
	public void f04b_basic_onParameter_swagger() throws Exception {
		ResponseInfo ri = sf.getResponseInfo("/f04", "get", 200);
		assertObjectEquals("{description:'OK',schema:{description:'f01',collectionFormat:'pipes'}}", ri);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test JSON Accept
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource(serializers=SimpleJsonSerializer.class)
	public static class G {
		@RestMethod
		public void g01(@Response Value<List<Integer>> body) {
			body.set(AList.create(1,2));
		}
		@RestMethod
		public void g02(Value<G01> body) {
			body.set(new G01());
		}
		@RestMethod
		@Response
		public List<Integer> g03() {
			return AList.create(1,2);
		}
		@RestMethod
		public G01 g04() {
			return new G01();
		}
	}

	@Response
	public static class G01 extends ArrayList<Integer> {
		public G01() {
			add(1);
			add(2);
		}
	}

	static MockRest g = MockRest.create(G.class);
	static Swagger sg = getSwagger(G.class);

	@Test
	public void g01a_basic_onParameter() throws Exception {
		g.get("/g01").json().execute().assertStatus(200).assertBody("[1,2]");
	}
	@Test
	public void g01b_basic_onParameter_swagger() throws Exception {
		ResponseInfo ri = sg.getResponseInfo("/g01", "get", 200);
		assertObjectEquals("{description:'OK'}", ri);
	}
	@Test
	public void g02a_basic_onType() throws Exception {
		g.get("/g02").json().execute().assertStatus(200).assertBody("[1,2]");
	}
	@Test
	public void g02b_basic_onParameter_swagger() throws Exception {
		ResponseInfo ri = sg.getResponseInfo("/g02", "get", 200);
		assertObjectEquals("{description:'OK'}", ri);
	}
	@Test
	public void g03a_basic_onMethod() throws Exception {
		g.get("/g03").json().execute().assertStatus(200).assertBody("[1,2]");
	}
	@Test
	public void g03b_basic_onParameter_swagger() throws Exception {
		ResponseInfo ri = sg.getResponseInfo("/g03", "get", 200);
		assertObjectEquals("{description:'OK'}", ri);
	}
	@Test
	public void g04a_basic_onReturnedType() throws Exception {
		g.get("/g04").json().execute().assertStatus(200).assertBody("[1,2]");
	}
	@Test
	public void g04b_basic_onParameter_swagger() throws Exception {
		ResponseInfo ri = sg.getResponseInfo("/g04", "get", 200);
		assertObjectEquals("{description:'OK'}", ri);
	}

	//=================================================================================================================
	// PartSerializers
	//=================================================================================================================

	//-----------------------------------------------------------------------------------------------------------------
	// @ResponseBody(usePartSerializer), partSerializer on class
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource(partSerializer=XPartSerializer.class)
	public static class H {
		@RestMethod
		public void h01(@Response(usePartSerializer=true) Value<List<Integer>> body) {
			body.set(AList.create(1,2));
		}
		@RestMethod
		public void h02(Value<H01> body) {
			body.set(new H01());
		}
		@RestMethod
		@Response(usePartSerializer=true)
		public List<Integer> h03() {
			return AList.create(1,2);
		}
		@RestMethod
		public H01 h04() {
			return new H01();
		}
	}

	@Response(usePartSerializer=true)
	public static class H01 extends ArrayList<Integer> {
		public H01() {
			add(1);
			add(2);
		}
	}

	static MockRest h = MockRest.create(H.class);
	static Swagger sh = getSwagger(H.class);

	@Test
	public void h01a_basic_onParameter() throws Exception {
		h.get("/h01").execute().assertStatus(200).assertBody("x[1, 2]x");
	}
	@Test
	public void h01b_basic_onParameter_swagger() throws Exception {
		ResponseInfo ri = sh.getResponseInfo("/h01", "get", 200);
		assertObjectEquals("{description:'OK',schema:{type:'array',items:{type:'integer',format:'int32'}}}", ri);
	}
	@Test
	public void h02a_basic_onType() throws Exception {
		h.get("/h02").execute().assertStatus(200).assertBody("x[1, 2]x");
	}
	@Test
	public void h02b_basic_onParameter_swagger() throws Exception {
		ResponseInfo ri = sh.getResponseInfo("/h02", "get", 200);
		assertObjectEquals("{description:'OK',schema:{type:'array',items:{type:'integer',format:'int32'}}}", ri);
	}
	@Test
	public void h03a_basic_onMethod() throws Exception {
		h.get("/h03").execute().assertStatus(200).assertBody("x[1, 2]x");
	}
	@Test
	public void h03b_basic_onParameter_swagger() throws Exception {
		ResponseInfo ri = sh.getResponseInfo("/h03", "get", 200);
		assertObjectEquals("{description:'OK',schema:{type:'array',items:{type:'integer',format:'int32'}}}", ri);
	}
	@Test
	public void h04a_basic_onReturnedType() throws Exception {
		h.get("/h04").execute().assertStatus(200).assertBody("x[1, 2]x");
	}
	@Test
	public void h04b_basic_onParameter_swagger() throws Exception {
		ResponseInfo ri = sh.getResponseInfo("/h04", "get", 200);
		assertObjectEquals("{description:'OK',schema:{type:'array',items:{type:'integer',format:'int32'}}}", ri);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @ResponseBody(usePartSerializer), partSerializer on part.
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource
	public static class I {
		@RestMethod
		public void i01(@Response(partSerializer=XPartSerializer.class) Value<List<Integer>> body) {
			body.set(AList.create(1,2));
		}
		@RestMethod
		public void i02(Value<I01> body) {
			body.set(new I01());
		}
		@RestMethod
		@Response(partSerializer=XPartSerializer.class)
		public List<Integer> i03() {
			return AList.create(1,2);
		}
		@RestMethod
		public I01 i04() {
			return new I01();
		}
	}

	@Response(partSerializer=XPartSerializer.class)
	public static class I01 extends ArrayList<Integer> {
		public I01() {
			add(1);
			add(2);
		}
	}

	static MockRest i = MockRest.create(I.class);
	static Swagger si = getSwagger(I.class);

	@Test
	public void i01a_basic_onParameter() throws Exception {
		i.get("/i01").execute().assertStatus(200).assertBody("x[1, 2]x");
	}
	@Test
	public void i01b_basic_onParameter_swagger() throws Exception {
		ResponseInfo ri = si.getResponseInfo("/i01", "get", 200);
		assertObjectEquals("{description:'OK',schema:{type:'array',items:{type:'integer',format:'int32'}}}", ri);
	}
	@Test
	public void i02a_basic_onType() throws Exception {
		i.get("/i02").execute().assertStatus(200).assertBody("x[1, 2]x");
	}
	@Test
	public void i02b_basic_onParameter_swagger() throws Exception {
		ResponseInfo ri = si.getResponseInfo("/i02", "get", 200);
		assertObjectEquals("{description:'OK',schema:{type:'array',items:{type:'integer',format:'int32'}}}", ri);
	}
	@Test
	public void i03a_basic_onMethod() throws Exception {
		i.get("/i03").execute().assertStatus(200).assertBody("x[1, 2]x");
	}
	@Test
	public void i03b_basic_onParameter_swagger() throws Exception {
		ResponseInfo ri = si.getResponseInfo("/i03", "get", 200);
		assertObjectEquals("{description:'OK',schema:{type:'array',items:{type:'integer',format:'int32'}}}", ri);
	}
	@Test
	public void i04a_basic_onReturnedType() throws Exception {
		i.get("/i04").execute().assertStatus(200).assertBody("x[1, 2]x");
	}
	@Test
	public void i04b_basic_onParameter_swagger() throws Exception {
		ResponseInfo ri = si.getResponseInfo("/i04", "get", 200);
		assertObjectEquals("{description:'OK',schema:{type:'array',items:{type:'integer',format:'int32'}}}", ri);
	}


	//=================================================================================================================
	// @Response on RestMethod
	//=================================================================================================================

	@RestResource(serializers=SimpleJsonSerializer.class, parsers=JsonParser.class)
	public static class J {

		@RestMethod(name="POST")
		@Response(usePartSerializer=true)
		public String j01(@Body(usePartParser=true) String body) {
			return body;
		}
	}
	static MockRest j = MockRest.create(J.class);

	@Test
	public void j01a_basic() throws Exception {
		j.post("/j01", "foo").execute().assertStatus(200).assertBody("foo").assertHeader("Content-Type", "text/plain");
	}


	//=================================================================================================================
	// @Response on POJO
	//=================================================================================================================

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource
	public static class SA {

		@Response(
			description={"a","b"},
			schema=@Schema(type="string"),
			headers=@ResponseHeader(name="foo",type="string"),
			example="'a'",
			examples=" {foo:'a'} "
		)
		public static class SA01 {}
		@RestMethod
		public void sa01a(Value<SA01> r) {}
		@RestMethod
		public SA01 sa01b() {return null;}

		@Response(
			api={
				"description:'a\nb',",
				"schema:{type:'string'},",
				"headers:{foo:{type:'string'}},",
				"example:'a',",
				"examples:{foo:'a'}"
			}
		)
		public static class SA02 {}
		@RestMethod
		public void sa02a(Value<SA02> r) {}
		@RestMethod
		public SA02 sa02b() {return null;}

		@Response(
			api={
				"description:'b',",
				"schema:{type:'number'},",
				"headers:{bar:{type:'number'}},",
				"example:'b',",
				"examples:{bar:'b'}"
			},
			description={"a","b"},
			schema=@Schema(type="string"),
			headers=@ResponseHeader(name="foo",type="string"),
			example="'a'",
			examples=" {foo:'a'} "
		)
		public static class SA03 {}
		@RestMethod
		public void sa03a(Value<SA03> r) {}
		@RestMethod
		public SA03 sa03b() {return null;}

		@Response(code=100)
		public static class SA04 {}
		@RestMethod
		public void sa04a(Value<SA04> r) {}
		@RestMethod
		public SA04 sa04b() {return null;}

		@Response(100)
		public static class SA05 {}
		@RestMethod
		public void sa05a(Value<SA05> r) {}
		@RestMethod
		public SA05 sa05b() {return null;}

		@Response(headers=@ResponseHeader(name="foo",api=" type:'b' "))
		public static class SA06 {}
		@RestMethod
		public void sa06a(Value<SA06> r) {}
		@RestMethod
		public SA06 sa06b() {return null;}
	}

	static Swagger sa = getSwagger(SA.class);

	@Test
	public void sa01a_Response_onPojo_basic() throws Exception {
		ResponseInfo x = sa.getResponseInfo("/sa01a","get",200);
		assertEquals("a\nb", x.getDescription());
		assertObjectEquals("{type:'string'}", x.getSchema());
		assertObjectEquals("{foo:{type:'string'}}", x.getHeaders());
		assertEquals("'a'", x.getExample());
		assertObjectEquals("{foo:'a'}", x.getExamples());
	}
	@Test
	public void sa01b_Response_onPojo_basic() throws Exception {
		ResponseInfo x = sa.getResponseInfo("/sa01b","get",200);
		assertEquals("a\nb", x.getDescription());
		assertObjectEquals("{type:'string'}", x.getSchema());
		assertObjectEquals("{foo:{type:'string'}}", x.getHeaders());
		assertEquals("'a'", x.getExample());
		assertObjectEquals("{foo:'a'}", x.getExamples());
	}
	@Test
	public void sa02a_Response_onPojo_api() throws Exception {
		ResponseInfo x = sa.getResponseInfo("/sa02a","get",200);
		assertEquals("a\nb", x.getDescription());
		assertObjectEquals("{type:'string'}", x.getSchema());
		assertObjectEquals("{foo:{type:'string'}}", x.getHeaders());
		assertEquals("a", x.getExample());
		assertObjectEquals("{foo:'a'}", x.getExamples());
	}
	@Test
	public void sa02b_Response_onPojo_api() throws Exception {
		ResponseInfo x = sa.getResponseInfo("/sa02b","get",200);
		assertEquals("a\nb", x.getDescription());
		assertObjectEquals("{type:'string'}", x.getSchema());
		assertObjectEquals("{foo:{type:'string'}}", x.getHeaders());
		assertEquals("a", x.getExample());
		assertObjectEquals("{foo:'a'}", x.getExamples());
	}
	@Test
	public void sa03a_Response_onPojo_mixed() throws Exception {
		ResponseInfo x = sa.getResponseInfo("/sa03a","get",200);
		assertEquals("a\nb", x.getDescription());
		assertObjectEquals("{type:'string'}", x.getSchema());
		assertObjectEquals("{bar:{type:'number'},foo:{type:'string'}}", x.getHeaders());
		assertEquals("'a'", x.getExample());
		assertObjectEquals("{foo:'a'}", x.getExamples());
	}
	@Test
	public void sa03b_Response_onPojo_mixed() throws Exception {
		ResponseInfo x = sa.getResponseInfo("/sa03b","get",200);
		assertEquals("a\nb", x.getDescription());
		assertObjectEquals("{type:'string'}", x.getSchema());
		assertObjectEquals("{bar:{type:'number'},foo:{type:'string'}}", x.getHeaders());
		assertEquals("'a'", x.getExample());
		assertObjectEquals("{foo:'a'}", x.getExamples());
	}
	@Test
	public void sa04a_Response_onPojo_code() throws Exception {
		ResponseInfo x = sa.getResponseInfo("/sa04a","get",100);
		assertEquals("Continue", x.getDescription());
	}
	@Test
	public void sa04b_Response_onPojo_code() throws Exception {
		ResponseInfo x = sa.getResponseInfo("/sa04b","get",100);
		assertEquals("Continue", x.getDescription());
	}
	@Test
	public void sa05a_Response_onPojo_value() throws Exception {
		ResponseInfo x = sa.getResponseInfo("/sa05a","get",100);
		assertEquals("Continue", x.getDescription());
	}
	@Test
	public void sa05b_Response_onPojo_value() throws Exception {
		ResponseInfo x = sa.getResponseInfo("/sa05b","get",100);
		assertEquals("Continue", x.getDescription());
	}
	@Test
	public void sa06a_Response_onPojo_headers() throws Exception {
		ResponseInfo x = sa.getResponseInfo("/sa06a","get",200);
		assertObjectEquals("{foo:{type:'b'}}", x.getHeaders());
	}
	@Test
	public void sa06b_Response_onPojo_headers() throws Exception {
		ResponseInfo x = sa.getResponseInfo("/sa06b","get",200);
		assertObjectEquals("{foo:{type:'b'}}", x.getHeaders());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Schema
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource
	public static class SB {

		@Response(schema=@Schema(" type:'number' "))
		public static class SB01 {}
		@RestMethod
		public void sb01a(Value<SB01> r) {}
		@RestMethod
		public SB01 sb01b() {return null;}

		@Response(usePartSerializer=true)
		public static class SB02 {
			public String f1;
		}
		@RestMethod
		public void sb02a(Value<SB02> b) {}
		@RestMethod
		public SB02 sb02b() {return null;}

		@Response(usePartSerializer=true)
		public static class SB03 extends LinkedList<String> {
			private static final long serialVersionUID = 1L;
		}
		@RestMethod
		public void sb03a(Value<SB03> b) {}
		@RestMethod
		public SB03 sb03b() {return null;}

		@Response(usePartSerializer=true)
		public static class SB04 {}
		@RestMethod
		public void sb04a(Value<SB04> b) {}
		@RestMethod
		public SB04 sb04b() {return null;}
	}

	static Swagger sb = getSwagger(SB.class);

	@Test
	public void sb01a_Response_onPojo_schemaValue() throws Exception {
		ResponseInfo x = sb.getResponseInfo("/sb01a","get",200);
		assertObjectEquals("{type:'number'}", x.getSchema());
	}
	@Test
	public void sb01b_Response_onPojo_schemaValue() throws Exception {
		ResponseInfo x = sb.getResponseInfo("/sb01b","get",200);
		assertObjectEquals("{type:'number'}", x.getSchema());
	}
	@Test
	public void sb02a_Response_onPojo_autoDetectBean() throws Exception {
		ResponseInfo x = sb.getResponseInfo("/sb02a","get",200);
		assertObjectEquals("{type:'object',properties:{f1:{type:'string'}}}", x.getSchema());
	}
	@Test
	public void sb02b_Response_onPojo_autoDetectBean() throws Exception {
		ResponseInfo x = sb.getResponseInfo("/sb02b","get",200);
		assertObjectEquals("{type:'object',properties:{f1:{type:'string'}}}", x.getSchema());
	}
	@Test
	public void sb03a_Response_onPojo_autoDetectList() throws Exception {
		ResponseInfo x = sb.getResponseInfo("/sb03a","get",200);
		assertObjectEquals("{type:'array',items:{type:'string'}}", x.getSchema());
	}
	@Test
	public void sb03b_Response_onPojo_autoDetectList() throws Exception {
		ResponseInfo x = sb.getResponseInfo("/sb03b","get",200);
		assertObjectEquals("{type:'array',items:{type:'string'}}", x.getSchema());
	}
	@Test
	public void sb04a_Response_onPojo_autoDetectStringObject() throws Exception {
		ResponseInfo x = sb.getResponseInfo("/sb04a","get",200);
		assertObjectEquals("{type:'string'}", x.getSchema());
	}
	@Test
	public void sb04b_Response_onPojo_autoDetectStringObject() throws Exception {
		ResponseInfo x = sb.getResponseInfo("/sb04b","get",200);
		assertObjectEquals("{type:'string'}", x.getSchema());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Examples
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource
	public static class SC {

		@Response(example="{f1:'a'}")
		public static class SC01 {}
		@RestMethod
		public void sc01a(Value<SC01> r) {}
		@RestMethod
		public SC01 sc01b() {return null;}

		@Response(examples={" foo:'b' "})
		public static class SC02 {}
		@RestMethod
		public void sc02a(Value<SC02> r) {}
		@RestMethod
		public SC02 sc02b() {return null;}
	}

	static Swagger sc = getSwagger(SC.class);

	@Test
	public void sc01a_Response_onPojo_example() throws Exception {
		ResponseInfo x = sc.getResponseInfo("/sc01a","get",200);
		assertEquals("{f1:'a'}", x.getExample());
	}
	@Test
	public void sc01b_Response_onPojo_example() throws Exception {
		ResponseInfo x = sc.getResponseInfo("/sc01b","get",200);
		assertEquals("{f1:'a'}", x.getExample());
	}
	@Test
	public void sc02a_Response_onPojo_examples() throws Exception {
		ResponseInfo x = sc.getResponseInfo("/sc02a","get",200);
		assertObjectEquals("{foo:'b'}", x.getExamples());
	}
	@Test
	public void sc02b_Response_onPojo_examples() throws Exception {
		ResponseInfo x = sc.getResponseInfo("/sc02b","get",200);
		assertObjectEquals("{foo:'b'}", x.getExamples());
	}

	//=================================================================================================================
	// @Response on throwable
	//=================================================================================================================

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource
	@SuppressWarnings({"unused"})
	public static class UA {

		@Response(
			description= {"a","b"},
			schema=@Schema(type="string"),
			headers=@ResponseHeader(name="foo",type="string"),
			example=" 'a' ",
			examples=" {foo:'a'} "
		)
		public static class UA01 extends Throwable {}
		@RestMethod
		public void ua01() throws UA01 {}

		@Response(
			api={
				"description:'a\nb',",
				"schema:{type:'string'},",
				"headers:{foo:{type:'string'}},",
				"example:'a',",
				"examples:{foo:'a'}"
			}
		)
		public static class UA02 extends Throwable {}
		@RestMethod
		public void ua02() throws UA02 {}

		@Response(
			api={
				"description:'b',",
				"schema:{type:'number'},",
				"headers:{bar:{type:'number'}},",
				"example:'b',",
				"examples:{bar:'b'}"
			},
			description= {"a","b"},
			schema=@Schema(type="string"),
			headers=@ResponseHeader(name="foo",type="string"),
			example=" 'a' ",
			examples=" {foo:'a'} "
		)
		public static class UA03 extends Throwable {}
		@RestMethod
		public void ua03() throws UA03 {}

		@Response(code=100)
		public static class UA04 extends Throwable {}
		@RestMethod
		public void ua04() throws UA04 {}

		@Response(code=100)
		public static class UA05 extends Throwable {}
		@RestMethod
		public void ua05() throws UA05 {}

		@Response(headers=@ResponseHeader(name="foo", api=" {type:'number'} "))
		public static class UA06 extends Throwable {}
		@RestMethod
		public void ua06() throws UA06 {}
	}

	static Swagger ua = getSwagger(UA.class);

	@Test
	public void ua01_Response_onThrowable_basic() throws Exception {
		ResponseInfo x = ua.getResponseInfo("/ua01","get",500);
		assertEquals("a\nb", x.getDescription());
		assertObjectEquals("{type:'string'}", x.getSchema());
		assertObjectEquals("{foo:{type:'string'}}", x.getHeaders());
		assertEquals("'a'", x.getExample());
		assertObjectEquals("{foo:'a'}", x.getExamples());
	}
	@Test
	public void ua02_Response_onThrowable_api() throws Exception {
		ResponseInfo x = ua.getResponseInfo("/ua02","get",500);
		assertEquals("a\nb", x.getDescription());
		assertObjectEquals("{type:'string'}", x.getSchema());
		assertObjectEquals("{foo:{type:'string'}}", x.getHeaders());
		assertObjectEquals("'a'", x.getExample());
		assertObjectEquals("{foo:'a'}", x.getExamples());
	}
	@Test
	public void ua03_Response_onThrowable_mixed() throws Exception {
		ResponseInfo x = ua.getResponseInfo("/ua03","get",500);
		assertEquals("a\nb", x.getDescription());
		assertObjectEquals("{type:'string'}", x.getSchema());
		assertObjectEquals("{bar:{type:'number'},foo:{type:'string'}}", x.getHeaders());
		assertEquals("'a'", x.getExample());
		assertObjectEquals("{foo:'a'}", x.getExamples());
	}
	@Test
	public void ua04_Response_onThrowable_code() throws Exception {
		ResponseInfo x = ua.getResponseInfo("/ua04","get",100);
		assertEquals("Continue", x.getDescription());
	}
	@Test
	public void ua05_Response_onThrowable_value() throws Exception {
		ResponseInfo x = ua.getResponseInfo("/ua05","get",100);
		assertEquals("Continue", x.getDescription());
	}
	@Test
	public void ua06_Response_onThrowable_headers1() throws Exception {
		ResponseInfo x = ua.getResponseInfo("/ua06","get",500);
		assertObjectEquals("{foo:{type:'number'}}", x.getHeaders());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Schema
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource
	@SuppressWarnings({"unused"})
	public static class UB {

		@Response(schema=@Schema(" type:'number' "))
		public static class UB01 extends Throwable {}
		@RestMethod
		public void ub01() throws UB01 {}
	}

	static Swagger ub = getSwagger(UB.class);

	@Test
	public void ub01_Response_onThrowable_schemaValue() throws Exception {
		ResponseInfo x = ub.getResponseInfo("/ub01","get",500);
		assertObjectEquals("{type:'number'}", x.getSchema());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Examples
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource
	@SuppressWarnings({"unused"})
	public static class UC {

		@Response(example={" {f1:'b'} "})
		public static class UC01 extends Throwable {}
		@RestMethod
		public void uc01() throws UC01 {}

		@Response(examples={" foo:'b' "})
		public static class UC02 extends Throwable {}
		@RestMethod
		public void uc02() throws UC02 {}
	}

	static Swagger uc = getSwagger(UC.class);

	@Test
	public void uc01_Response_onThrowable_example() throws Exception {
		ResponseInfo x = uc.getResponseInfo("/uc01","get",500);
		assertEquals("{f1:'b'}", x.getExample());
	}
	@Test
	public void uc02_Response_onThrowable_examples() throws Exception {
		ResponseInfo x = uc.getResponseInfo("/uc02","get",500);
		assertObjectEquals("{foo:'b'}", x.getExamples());
	}
}
