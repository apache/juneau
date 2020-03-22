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
package org.apache.juneau.rest.annotation2;

import static org.apache.juneau.rest.testutils.TestUtils.*;
import static org.junit.Assert.assertEquals;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.annotation.Body;
import org.apache.juneau.http.annotation.Response;
import org.apache.juneau.json.*;
import org.apache.juneau.jsonschema.annotation.Schema;
import org.apache.juneau.oapi.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock2.*;
import org.apache.juneau.utils.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests the @Response annotation.
 */
@SuppressWarnings({"serial"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ResponseAnnotationTest {

	//=================================================================================================================
	// Status codes
	//=================================================================================================================

	//-----------------------------------------------------------------------------------------------------------------
	// HTTP status code
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
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

	static MockRest a = MockRest.build(A.class);

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

	@Rest(serializers=OpenApiSerializer.class,defaultAccept="text/openapi")
	public static class B {

		@Response
		@RestMethod
		public String b01() {
			return "foo";
		}
		@RestMethod
		public B03 b03() {
			return new B03();
		}
		@RestMethod
		public String b05() throws B05 {
			throw new B05();
		}
		@RestMethod
		public void b07(@Response Value<String> value) {
			value.set("foo");
		}
	}

	@Response
	public static class B03 {
		@Override
		public String toString() {return "foo";}
	}

	@Response
	public static class B05 extends Exception {
		@Override
		public String toString() {return "foo";}
	}

	static MockRest b = MockRest.build(B.class);

	@Test
	public void b01_useOnMethod() throws Exception {
		b.get("/b01").execute().assertStatus(200).assertBody("foo");
	}
	@Test
	public void b03_useOnClass() throws Exception {
		b.get("/b03").execute().assertStatus(200).assertBody("foo");
	}
	@Test
	public void b05_useOnThrown() throws Exception {
		b.get("/b05").execute().assertStatus(500).assertBody("foo");
	}
	@Test
	public void b07_useOnParameter() throws Exception {
		b.get("/b07").execute().assertStatus(200).assertBody("foo");
	}


	//-----------------------------------------------------------------------------------------------------------------
	// @Response(partSerializer) with schemas
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(serializers=OpenApiSerializer.class,defaultAccept="text/openapi")
	public static class D {

		@Response(schema=@Schema(collectionFormat="pipes"))
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
		public void d04(@Response(schema=@Schema(collectionFormat="pipes")) Value<String[]> value) {
			value.set(new String[]{"foo","bar"});
		}
		@Response(schema=@Schema(type="string",format="byte"))
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
		public void d08(@Response(schema=@Schema(type="string",format="byte")) Value<byte[]> value) {
			value.set("foo".getBytes());
		}
	}

	@Response(schema=@Schema(type="array",collectionFormat="pipes"))
	public static class D02 {
		public String[] toStringArray() {
			return new String[]{"foo","bar"};
		}
	}

	@Response(schema=@Schema(type="array",collectionFormat="pipes"))
	public static class D03 extends Exception {
		public String[] toStringArray() {
			return new String[]{"foo","bar"};
		}
	}

	@Response(schema=@Schema(format="byte"))
	public static class D06 {
		public byte[] toByteArray() {
			return "foo".getBytes();
		}
	}

	@Response(schema=@Schema(format="byte"))
	public static class D07 extends Exception {
		public byte[] toByteArray() {
			return "foo".getBytes();
		}
	}

	static MockRest d = MockRest.build(D.class);

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

	@Rest
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

	static MockRest e = MockRest.build(E.class);

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

	@Rest(serializers=OpenApiSerializer.class,defaultAccept="text/openapi")
	public static class F {
		@RestMethod
		public void f01(@Response(schema=@Schema(description="f01", collectionFormat="pipes")) Value<List<Integer>> body) {
			body.set(AList.of(1,2));
		}
		@RestMethod
		public void f02(Value<F01> body) {
			body.set(new F01());
		}
		@RestMethod
		@Response(schema=@Schema(description="f03", collectionFormat="pipes"))
		public List<Integer> f03() {
			return AList.of(1,2);
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

	static MockRest f = MockRest.build(F.class);
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

	@Rest(serializers=SimpleJsonSerializer.class)
	public static class G {
		@RestMethod
		public void g01(@Response Value<List<Integer>> body) {
			body.set(AList.of(1,2));
		}
		@RestMethod
		public void g02(Value<G01> body) {
			body.set(new G01());
		}
		@RestMethod
		@Response
		public List<Integer> g03() {
			return AList.of(1,2);
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

	static MockRest g = MockRest.build(G.class);
	static Swagger sg = getSwagger(G.class);

	@Test
	public void g01a_basic_onParameter() throws Exception {
		g.get("/g01").json().execute().assertStatus(200).assertBody("[1,2]");
	}
	@Test
	public void g01b_basic_onParameter_swagger() throws Exception {
		ResponseInfo ri = sg.getResponseInfo("/g01", "get", 200);
		assertObjectEquals("{description:'OK',schema:{type:'array',items:{type:'integer',format:'int32'}}}", ri);
	}
	@Test
	public void g02a_basic_onType() throws Exception {
		g.get("/g02").json().execute().assertStatus(200).assertBody("[1,2]");
	}
	@Test
	public void g02b_basic_onParameter_swagger() throws Exception {
		ResponseInfo ri = sg.getResponseInfo("/g02", "get", 200);
		assertObjectEquals("{description:'OK',schema:{type:'array',items:{type:'integer',format:'int32'}}}", ri);
	}
	@Test
	public void g03a_basic_onMethod() throws Exception {
		g.get("/g03").json().execute().assertStatus(200).assertBody("[1,2]");
	}
	@Test
	public void g03b_basic_onParameter_swagger() throws Exception {
		ResponseInfo ri = sg.getResponseInfo("/g03", "get", 200);
		assertObjectEquals("{description:'OK',schema:{type:'array',items:{type:'integer',format:'int32'}}}", ri);
	}
	@Test
	public void g04a_basic_onReturnedType() throws Exception {
		g.get("/g04").json().execute().assertStatus(200).assertBody("[1,2]");
	}
	@Test
	public void g04b_basic_onParameter_swagger() throws Exception {
		ResponseInfo ri = sg.getResponseInfo("/g04", "get", 200);
		assertObjectEquals("{description:'OK',schema:{type:'array',items:{type:'integer',format:'int32'}}}", ri);
	}

	//=================================================================================================================
	// PartSerializers
	//=================================================================================================================


	//=================================================================================================================
	// @Response on RestMethod
	//=================================================================================================================

	@Rest(serializers=SimpleJsonSerializer.class, parsers=JsonParser.class)
	public static class J {

		@RestMethod(name="POST")
		public String j01(@Body String body) {
			return body;
		}
	}
	static MockRest j = MockRest.build(J.class);

	@Test
	public void j01a_basic() throws Exception {
		j.post("/j01", "foo").accept("text/plain").execute().assertStatus(200).assertBody("foo").assertHeader("Content-Type", "text/plain");
	}


	//=================================================================================================================
	// @Response on POJO
	//=================================================================================================================

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class SA {

		@Response(
			description={"a","b"},
			schema=@Schema(type="string"),
			headers=@ResponseHeader(name="foo",type="string"),
			example="'a'",
			examples=" {foo:'a'} "
		)
		public static class SA01 {
			public SA01(String x){}
		}
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
		public static class SA02 {
			public SA02(String x){}
		}
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
		public static class SA03 {
			public SA03(String x){}
		}
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

	@Rest
	public static class SB {

		@Response(schema=@Schema(" type:'number' "))
		public static class SB01 {}
		@RestMethod
		public void sb01a(Value<SB01> r) {}
		@RestMethod
		public SB01 sb01b() {return null;}

		@Response
		public static class SB02 {
			public String f1;
		}
		@RestMethod
		public void sb02a(Value<SB02> b) {}
		@RestMethod
		public SB02 sb02b() {return null;}

		@Response
		public static class SB03 extends LinkedList<String> {
			private static final long serialVersionUID = 1L;
		}
		@RestMethod
		public void sb03a(Value<SB03> b) {}
		@RestMethod
		public SB03 sb03b() {return null;}

		@Response
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

	@Rest
	public static class SC {

		@Response(example="{f1:'a'}")
		public static class SC01 {
			public String f1;
		}
		@RestMethod
		public void sc01a(Value<SC01> r) {}
		@RestMethod
		public SC01 sc01b() {return null;}

		@Response(examples={" foo:'b' "})
		public static class SC02 {
			public SC02(String x){}
		}
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

	@Rest
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

	@Rest
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

	@Rest
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
