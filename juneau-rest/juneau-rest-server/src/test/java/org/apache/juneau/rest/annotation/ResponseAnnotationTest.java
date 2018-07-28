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

import static org.apache.juneau.http.HttpMethodName.*;
import static org.apache.juneau.testutils.TestUtils.*;
import static org.junit.Assert.assertEquals;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.serializer.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests the @Response annotation.
 */
@SuppressWarnings({"javadoc","serial"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ResponseAnnotationTest {

	//=================================================================================================================
	// Setup
	//=================================================================================================================

	private static Swagger getSwagger(Object resource) throws Exception {
		RestContext rc = RestContext.create(resource).build();
		RestRequest req = rc.getCallHandler().createRequest(new MockServletRequest());
		RestInfoProvider ip = rc.getInfoProvider();
		return ip.getSwagger(req);
	}


	//=================================================================================================================
	// Status codes
	//=================================================================================================================

	//-----------------------------------------------------------------------------------------------------------------
	// HTTP status code
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource
	public static class A {

		@Response(code=201)
		@RestMethod(name=GET,path="/codeOnMethod")
		public String a01() {
			return "foo";
		}

		@RestMethod(name=GET,path="/codeOnClass")
		public A02 a02() {
			return new A02();
		}

		@RestMethod(name=GET,path="/codeOnThrown")
		public String a03() throws A03 {
			throw new A03();
		}

		@Response(code=201)
		@RestMethod(name=GET,path="/codeOnParameter")
		public void a04(@Response(code=201) Value<String> value) {
			value.set("foo");
		}
	}

	@Response(code=201)
	public static class A02 {
		@Override
		public String toString() {return "foo";}
	}

	@Response(code=501)
	public static class A03 extends Exception {
		@Override
		public String toString() {return "foo";}
	}

	static MockRest a = MockRest.create(A.class);

	@Test
	public void a01_codeOnMethod() throws Exception {
		a.get("/codeOnMethod").execute().assertStatus(201).assertBody("foo");
	}

	@Test
	public void a02_codeOnClass() throws Exception {
		a.get("/codeOnClass").execute().assertStatus(201).assertBody("foo");
	}

	@Test
	public void a03_codeOnThrown() throws Exception {
		a.get("/codeOnThrown").execute().assertStatus(501);
	}

	@Test
	public void a04_codeOnParameter() throws Exception {
		a.get("/codeOnParameter").execute().assertStatus(201).assertBody("foo");
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
	// @Response(usePartSerializer)
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource(partSerializer=XPartSerializer.class)
	public static class B {

		@Response(usePartSerializer=true)
		@RestMethod(name=GET,path="/useOnMethod")
		public String b01() {
			return "foo";
		}

		@Response(usePartSerializer=false)
		@RestMethod(name=GET,path="/dontUseOnMethod")
		public String b02() {
			return "foo";
		}

		@RestMethod(name=GET,path="/useOnClass")
		public B03 b03() {
			return new B03();
		}

		@RestMethod(name=GET,path="/dontUseOnClass")
		public B04 b04() {
			return new B04();
		}

		@RestMethod(name=GET,path="/useOnThrown")
		public String b05() throws B05 {
			throw new B05();
		}

		@RestMethod(name=GET,path="/dontUseOnThrown")
		public String b06() throws B06 {
			throw new B06();
		}

		@RestMethod(name=GET,path="/useOnParameter")
		public void b07(@Response(usePartSerializer=true) Value<String> value) {
			value.set("foo");
		}

		@RestMethod(name=GET,path="/dontUseOnParameter")
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
		b.get("/useOnMethod").execute().assertStatus(200).assertBody("xfoox");
	}

	@Test
	public void b02_dontUseOnMethod() throws Exception {
		b.get("/dontUseOnMethod").execute().assertStatus(200).assertBody("foo");
	}

	@Test
	public void b03_useOnClass() throws Exception {
		b.get("/useOnClass").execute().assertStatus(200).assertBody("xfoox");
	}

	@Test
	public void b04_dontUseOnClass() throws Exception {
		b.get("/dontUseOnClass").execute().assertStatus(200).assertBody("foo");
	}

	@Test
	public void b05_useOnThrown() throws Exception {
		b.get("/useOnThrown").execute().assertStatus(500).assertBody("xfoox");
	}

	@Test
	public void b06_dontUseOnThrown() throws Exception {
		b.get("/dontUseOnThrown").execute().assertStatus(500).assertBodyContains("HTTP 500: Internal Server Error");
	}

	@Test
	public void b07_useOnParameter() throws Exception {
		b.get("/useOnParameter").execute().assertStatus(200).assertBody("xfoox");
	}

	@Test
	public void b08_dontUseOnParameter() throws Exception {
		b.get("/dontUseOnParameter").execute().assertStatus(200).assertBody("foo");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Response(partSerializer)
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource
	public static class C {

		@Response(partSerializer=XPartSerializer.class)
		@RestMethod(name=GET,path="/useOnMethod")
		public String c01() {
			return "foo";
		}

		@Response
		@RestMethod(name=GET,path="/dontUseOnMethod")
		public String c02() {
			return "foo";
		}

		@RestMethod(name=GET,path="/useOnClass")
		public C03 c03() {
			return new C03();
		}

		@RestMethod(name=GET,path="/dontUseOnClass")
		public C04 c04() {
			return new C04();
		}

		@RestMethod(name=GET,path="/useOnThrown")
		public String c05() throws C05 {
			throw new C05();
		}

		@RestMethod(name=GET,path="/dontUseOnThrown")
		public String c06() throws C06 {
			throw new C06();
		}

		@RestMethod(name=GET,path="/useOnParameter")
		public void c07(@Response(partSerializer=XPartSerializer.class) Value<String> value) {
			value.set("foo");
		}

		@RestMethod(name=GET,path="/dontUseOnParameter")
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
		c.get("/useOnMethod").execute().assertStatus(200).assertBody("xfoox");
	}

	@Test
	public void c02_dontUseOnMethod() throws Exception {
		c.get("/dontUseOnMethod").execute().assertStatus(200).assertBody("foo");
	}

	@Test
	public void c03_useOnClass() throws Exception {
		c.get("/useOnClass").execute().assertStatus(200).assertBody("xfoox");
	}

	@Test
	public void c04_dontUseOnClass() throws Exception {
		c.get("/dontUseOnClass").execute().assertStatus(200).assertBody("foo");
	}

	@Test
	public void c05_useOnThrown() throws Exception {
		c.get("/useOnThrown").execute().assertStatus(500).assertBody("xfoox");
	}

	@Test
	public void c06_dontUseOnThrown() throws Exception {
		c.get("/dontUseOnThrown").execute().assertStatus(500).assertBodyContains("HTTP 500: Internal Server Error");
	}

	@Test
	public void c07_useOnParameter() throws Exception {
		c.get("/useOnParameter").execute().assertStatus(200).assertBody("xfoox");
	}

	@Test
	public void c08_dontUseOnParameter() throws Exception {
		c.get("/dontUseOnParameter").execute().assertStatus(200).assertBody("foo");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Response(partSerializer) with schemas
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource
	public static class D {

		@Response(schema=@Schema(collectionFormat="pipes"),usePartSerializer=true)
		@RestMethod(name=GET,path="/useOnMethod")
		public String[] d01() {
			return new String[]{"foo","bar"};
		}

		@RestMethod(name=GET,path="/useOnClass")
		public D02 d02() {
			return new D02();
		}

		@RestMethod(name=GET,path="/useOnThrown")
		public String d03() throws D03 {
			throw new D03();
		}

		@RestMethod(name=GET,path="/useOnParameter")
		public void d04(@Response(schema=@Schema(collectionFormat="pipes"),usePartSerializer=true) Value<String[]> value) {
			value.set(new String[]{"foo","bar"});
		}

		@Response(schema=@Schema(type="string",format="byte"),usePartSerializer=true)
		@RestMethod(name=GET,path="/useOnMethodBytes")
		public byte[] d05() {
			return "foo".getBytes();
		}

		@RestMethod(name=GET,path="/useOnClassBytes")
		public D06 d06() {
			return new D06();
		}

		@RestMethod(name=GET,path="/useOnThrownBytes")
		public String d07() throws D07 {
			throw new D07();
		}


		@RestMethod(name=GET,path="/useOnParameterBytes")
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
		d.get("/useOnMethod").execute().assertStatus(200).assertBody("foo|bar");
	}

	@Test
	public void d02_useOnClass() throws Exception {
		d.get("/useOnClass").execute().assertStatus(200).assertBody("foo|bar");
	}

	@Test
	public void d03_useOnThrown() throws Exception {
		d.get("/useOnThrown").execute().assertStatus(500).assertBody("foo|bar");
	}

	@Test
	public void d04_useOnParameter() throws Exception {
		d.get("/useOnParameter").execute().assertStatus(200).assertBody("foo|bar");
	}

	@Test
	public void d05_useOnMethodBytes() throws Exception {
		d.get("/useOnMethodBytes").execute().assertStatus(200).assertBody("Zm9v");
	}

	@Test
	public void d06_useOnClassBytes() throws Exception {
		d.get("/useOnClassBytes").execute().assertStatus(200).assertBody("Zm9v");
	}

	@Test
	public void d07_useOnThrownBytes() throws Exception {
		d.get("/useOnThrownBytes").execute().assertStatus(500).assertBody("Zm9v");
	}

	@Test
	public void d08_useOnParameterBytes() throws Exception {
		d.get("/useOnParameterBytes").execute().assertStatus(200).assertBody("Zm9v");
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
		@RestMethod(name=GET,path="/basic")
		public void sa01(SA01 r) {}

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
		@RestMethod(name=GET,path="/api")
		public void sa02(SA02 r) {}

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
		@RestMethod(name=GET,path="/mixed")
		public void sa03(SA03 r) {}

		@Response(code=100)
		public static class SA04 {}
		@RestMethod(name=GET,path="/code")
		public void sa04(SA04 r) {}

		@Response(100)
		public static class SA05 {}
		@RestMethod(name=GET,path="/value")
		public void sa05(SA05 r) {}

		@Response(headers=@ResponseHeader(name="foo",api=" type:'b' "))
		public static class SA06 {}
		@RestMethod(name=GET,path="/headers")
		public void sa06(SA06 r) {}
	}

	@Test
	public void sa01_Response_onPojo_basic() throws Exception {
		ResponseInfo x = getSwagger(new SA()).getPaths().get("/basic").get("get").getResponse(200);
		assertEquals("a\nb", x.getDescription());
		assertObjectEquals("{type:'string'}", x.getSchema());
		assertObjectEquals("{foo:{type:'string'}}", x.getHeaders());
		assertObjectEquals("'a'", x.getExample());
		assertObjectEquals("{foo:'a'}", x.getExamples());
	}
	@Test
	public void sa02_Response_onPojo_api() throws Exception {
		ResponseInfo x = getSwagger(new SA()).getPaths().get("/api").get("get").getResponse(200);
		assertEquals("a\nb", x.getDescription());
		assertObjectEquals("{type:'string'}", x.getSchema());
		assertObjectEquals("{foo:{type:'string'}}", x.getHeaders());
		assertObjectEquals("'a'", x.getExample());
		assertObjectEquals("{foo:'a'}", x.getExamples());
	}
	@Test
	public void sa03_Response_onPojo_mixed() throws Exception {
		ResponseInfo x = getSwagger(new SA()).getPaths().get("/mixed").get("get").getResponse(200);
		assertEquals("a\nb", x.getDescription());
		assertObjectEquals("{type:'string'}", x.getSchema());
		assertObjectEquals("{foo:{type:'string'}}", x.getHeaders());
		assertObjectEquals("'a'", x.getExample());
		assertObjectEquals("{foo:'a'}", x.getExamples());
	}
	@Test
	public void sa04_Response_onPojo_code() throws Exception {
		ResponseInfo x = getSwagger(new SA()).getPaths().get("/code").get("get").getResponse(100);
		assertEquals("Continue", x.getDescription());
	}
	@Test
	public void sa05_Response_onPojo_value() throws Exception {
		ResponseInfo x = getSwagger(new SA()).getPaths().get("/value").get("get").getResponse(100);
		assertEquals("Continue", x.getDescription());
	}
	@Test
	public void sa06_Response_onPojo_headers() throws Exception {
		ResponseInfo x = getSwagger(new SA()).getPaths().get("/headers").get("get").getResponse(200);
		assertObjectEquals("{foo:{type:'b'}}", x.getHeaders());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Schema
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource
	public static class SB {

		@Response(schema=@Schema(" type:'number' "))
		public static class SB01 {}
		@RestMethod(name=GET,path="/schemaValue")
		public void sb01(SB01 r) {}

		@Response
		public static class SB02 {
			public String f1;
		}
		@RestMethod(name=GET,path="/autoDetectBean")
		public void sb02(SB02 b) {}

		@Response
		public static class SB03 extends LinkedList<String> {
			private static final long serialVersionUID = 1L;
		}
		@RestMethod(name=GET,path="/autoDetectList")
		public void sb03(SB03 b) {}

		@Response
		public static class SB04 {}
		@RestMethod(name=GET,path="/autoDetectStringObject")
		public void sb04(SB04 b) {}
	}

	@Test
	public void sb01_Response_onPojo_schemaValue() throws Exception {
		ResponseInfo x = getSwagger(new SB()).getPaths().get("/schemaValue").get("get").getResponse(200);
		assertObjectEquals("{type:'number'}", x.getSchema());
	}
	@Test
	public void sb02_Response_onPojo_autoDetectBean() throws Exception {
		ResponseInfo x = getSwagger(new SB()).getPaths().get("/autoDetectBean").get("get").getResponse(200);
		assertObjectEquals("{type:'object',properties:{f1:{type:'string'}}}", x.getSchema());
	}
	@Test
	public void sb03_Response_onPojo_autoDetectList() throws Exception {
		ResponseInfo x = getSwagger(new SB()).getPaths().get("/autoDetectList").get("get").getResponse(200);
		assertObjectEquals("{type:'array',items:{type:'string'}}", x.getSchema());
	}
	@Test
	public void sb04_Response_onPojo_autoDetectStringObject() throws Exception {
		ResponseInfo x = getSwagger(new SB()).getPaths().get("/autoDetectStringObject").get("get").getResponse(200);
		assertObjectEquals("{type:'string'}", x.getSchema());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Examples
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource
	public static class SC {

		@Response(example="{f1:'a'}")
		public static class SC01 {}
		@RestMethod(name=GET,path="/example")
		public void sc01(SC01 r) {}

		@Response(examples={" foo:'b' "})
		public static class SC02 {}
		@RestMethod(name=GET,path="/examples")
		public void sc02(SC02 r) {}
	}

	@Test
	public void sc01_Response_onPojo_example() throws Exception {
		ResponseInfo x = getSwagger(new SC()).getPaths().get("/example").get("get").getResponse(200);
		assertObjectEquals("{f1:'a'}", x.getExample());
	}
	@Test
	public void sc02_Response_onPojo_examples() throws Exception {
		ResponseInfo x = getSwagger(new SC()).getPaths().get("/examples").get("get").getResponse(200);
		assertObjectEquals("{foo:'b'}", x.getExamples());
	}

	//=================================================================================================================
	// @Response on parameter
	//=================================================================================================================

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource
	public static class TA {

		public static class TA01 {}
		@RestMethod(name=GET,path="/basic")
		public void ta01(
			@Response(
				description={"a","b"},
				schema=@Schema(type="string"),
				headers=@ResponseHeader(name="foo",type="string"),
				example=" 'a' ",
				examples=" {foo:'a'} "
			) TA01 r
		) {}

		public static class TA02 {}
		@RestMethod(name=GET,path="/api")
		public void ta02(
			@Response(
				api={
					"description:'a\nb',",
					"schema:{type:'string'},",
					"headers:{foo:{type:'string'}},",
					"example:'a',",
					"examples:{foo:'a'}"
				}
			) TA02 r
		) {}

		public static class TA03 {}
		@RestMethod(name=GET,path="/mixed")
		public void ta03(
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
				example=" 'a' ",
				examples=" {foo:'a'} "
			) TA03 r
		) {}

		public static class TA04 {}
		@RestMethod(name=GET,path="/code")
		public void ta04(@Response(code=100) TA04 r) {}

		public static class TA05 {}
		@RestMethod(name=GET,path="/value")
		public void ta05(@Response(code=100) TA05 r) {}

		public static class TA06 {}
		@RestMethod(name=GET,path="/headers")
		public void ta06(@Response(headers=@ResponseHeader(name="foo",api=" type:'number' ")) TA06 r) {}
	}

	@Test
	public void ta01_Response_onParameter_basic() throws Exception {
		ResponseInfo x = getSwagger(new TA()).getPaths().get("/basic").get("get").getResponse(200);
		assertEquals("a\nb", x.getDescription());
		assertObjectEquals("{type:'string'}", x.getSchema());
		assertObjectEquals("{foo:{type:'string'}}", x.getHeaders());
		assertObjectEquals("'a'", x.getExample());
		assertObjectEquals("{foo:'a'}", x.getExamples());
	}
	@Test
	public void ta02_Response_onParameter_api() throws Exception {
		ResponseInfo x = getSwagger(new TA()).getPaths().get("/api").get("get").getResponse(200);
		assertEquals("a\nb", x.getDescription());
		assertObjectEquals("{type:'string'}", x.getSchema());
		assertObjectEquals("{foo:{type:'string'}}", x.getHeaders());
		assertObjectEquals("'a'", x.getExample());
		assertObjectEquals("{foo:'a'}", x.getExamples());
	}
	@Test
	public void ta03_Response_onParameter_mixed() throws Exception {
		ResponseInfo x = getSwagger(new TA()).getPaths().get("/mixed").get("get").getResponse(200);
		assertEquals("a\nb", x.getDescription());
		assertObjectEquals("{type:'string'}", x.getSchema());
		assertObjectEquals("{foo:{type:'string'}}", x.getHeaders());
		assertObjectEquals("'a'", x.getExample());
		assertObjectEquals("{foo:'a'}", x.getExamples());
	}
	@Test
	public void ta04_Response_onParameter_code() throws Exception {
		ResponseInfo x = getSwagger(new TA()).getPaths().get("/code").get("get").getResponse(100);
		assertEquals("Continue", x.getDescription());
	}
	@Test
	public void ta05_Response_onParameter_value() throws Exception {
		ResponseInfo x = getSwagger(new TA()).getPaths().get("/value").get("get").getResponse(100);
		assertEquals("Continue", x.getDescription());
	}
	@Test
	public void ta06_Response_onParameter_headers() throws Exception {
		ResponseInfo x = getSwagger(new TA()).getPaths().get("/headers").get("get").getResponse(200);
		assertObjectEquals("{foo:{type:'number'}}", x.getHeaders());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Schema
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource
	public static class TB {

		public static class TB01 {}
		@RestMethod(name=GET,path="/schemaValue")
		public void tb01(@Response(schema=@Schema(" type:'number' ")) TB01 r) {}

		public static class TB02 {
			public String f1;
		}
		@RestMethod(name=GET,path="/autoDetectBean")
		public void tb02(@Response TB02 b) {}

		public static class TB03 extends LinkedList<String> {
			private static final long serialVersionUID = 1L;
		}
		@RestMethod(name=GET,path="/autoDetectList")
		public void tb03(@Response TB03 b) {}

		public static class TB04 {}
		@RestMethod(name=GET,path="/autoDetectStringObject")
		public void tb04(@Response TB04 b) {}
	}

	@Test
	public void tb01_Response_onParameter_schemaValue() throws Exception {
		ResponseInfo x = getSwagger(new TB()).getPaths().get("/schemaValue").get("get").getResponse(200);
		assertObjectEquals("{type:'number'}", x.getSchema());
	}
	@Test
	public void tb02_Response_onParameter_autoDetectBean() throws Exception {
		ResponseInfo x = getSwagger(new TB()).getPaths().get("/autoDetectBean").get("get").getResponse(200);
		assertObjectEquals("{type:'object',properties:{f1:{type:'string'}}}", x.getSchema());
	}
	@Test
	public void tb03_Response_onParameter_autoDetectList() throws Exception {
		ResponseInfo x = getSwagger(new TB()).getPaths().get("/autoDetectList").get("get").getResponse(200);
		assertObjectEquals("{type:'array',items:{type:'string'}}", x.getSchema());
	}
	@Test
	public void tb04_Response_onParameter_autoDetectStringObject() throws Exception {
		ResponseInfo x = getSwagger(new TB()).getPaths().get("/autoDetectStringObject").get("get").getResponse(200);
		assertObjectEquals("{type:'string'}", x.getSchema());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Examples
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource
	public static class TC {

		public static class TC01 {}
		@RestMethod(name=GET,path="/example")
		public void tc01(@Response(example=" {f1:'b'} ") TC01 r) {}

		public static class TC02 {}
		@RestMethod(name=GET,path="/examples")
		public void tc02(@Response(examples={" foo:'b' "}) TC02 r) {}
	}

	@Test
	public void tc01_Response_onParameter_example() throws Exception {
		ResponseInfo x = getSwagger(new TC()).getPaths().get("/example").get("get").getResponse(200);
		assertObjectEquals("{f1:'b'}", x.getExample());
	}
	@Test
	public void tc02_Response_onParameter_examples() throws Exception {
		ResponseInfo x = getSwagger(new TC()).getPaths().get("/examples").get("get").getResponse(200);
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
		@RestMethod(name=GET,path="/basic")
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
		@RestMethod(name=GET,path="/api")
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
		@RestMethod(name=GET,path="/mixed")
		public void ua03() throws UA03 {}

		@Response(code=100)
		public static class UA04 extends Throwable {}
		@RestMethod(name=GET,path="/code")
		public void ua04() throws UA04 {}

		@Response(code=100)
		public static class UA05 extends Throwable {}
		@RestMethod(name=GET,path="/value")
		public void ua05() throws UA05 {}

		@Response(headers=@ResponseHeader(name="foo", api=" {type:'number'} "))
		public static class UA06 extends Throwable {}
		@RestMethod(name=GET,path="/headers")
		public void ua06() throws UA06 {}
	}

	@Test
	public void ua01_Response_onThrowable_basic() throws Exception {
		ResponseInfo x = getSwagger(new UA()).getPaths().get("/basic").get("get").getResponse(500);
		assertEquals("a\nb", x.getDescription());
		assertObjectEquals("{type:'string'}", x.getSchema());
		assertObjectEquals("{foo:{type:'string'}}", x.getHeaders());
		assertObjectEquals("'a'", x.getExample());
		assertObjectEquals("{foo:'a'}", x.getExamples());
	}
	@Test
	public void ua02_Response_onThrowable_api() throws Exception {
		ResponseInfo x = getSwagger(new UA()).getPaths().get("/api").get("get").getResponse(500);
		assertEquals("a\nb", x.getDescription());
		assertObjectEquals("{type:'string'}", x.getSchema());
		assertObjectEquals("{foo:{type:'string'}}", x.getHeaders());
		assertObjectEquals("'a'", x.getExample());
		assertObjectEquals("{foo:'a'}", x.getExamples());
	}
	@Test
	public void ua03_Response_onThrowable_mixed() throws Exception {
		ResponseInfo x = getSwagger(new UA()).getPaths().get("/mixed").get("get").getResponse(500);
		assertEquals("a\nb", x.getDescription());
		assertObjectEquals("{type:'string'}", x.getSchema());
		assertObjectEquals("{foo:{type:'string'}}", x.getHeaders());
		assertObjectEquals("'a'", x.getExample());
		assertObjectEquals("{foo:'a'}", x.getExamples());
	}
	@Test
	public void ua04_Response_onThrowable_code() throws Exception {
		ResponseInfo x = getSwagger(new UA()).getPaths().get("/code").get("get").getResponse(100);
		assertEquals("Continue", x.getDescription());
	}
	@Test
	public void ua05_Response_onThrowable_value() throws Exception {
		ResponseInfo x = getSwagger(new UA()).getPaths().get("/value").get("get").getResponse(100);
		assertEquals("Continue", x.getDescription());
	}
	@Test
	public void ua06_Response_onThrowable_headers1() throws Exception {
		ResponseInfo x = getSwagger(new UA()).getPaths().get("/headers").get("get").getResponse(500);
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
		@RestMethod(name=GET,path="/schemaValue")
		public void ub01() throws UB01 {}
	}

	@Test
	public void ub01_Response_onThrowable_schemaValue() throws Exception {
		ResponseInfo x = getSwagger(new UB()).getPaths().get("/schemaValue").get("get").getResponse(500);
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
		@RestMethod(name=GET,path="/example")
		public void uc01() throws UC01 {}

		@Response(examples={" foo:'b' "})
		public static class UC02 extends Throwable {}
		@RestMethod(name=GET,path="/examples")
		public void uc02() throws UC02 {}
	}

	@Test
	public void uc01_Response_onThrowable_example() throws Exception {
		ResponseInfo x = getSwagger(new UC()).getPaths().get("/example").get("get").getResponse(500);
		assertObjectEquals("{f1:'b'}", x.getExample());
	}
	@Test
	public void uc02_Response_onThrowable_examples() throws Exception {
		ResponseInfo x = getSwagger(new UC()).getPaths().get("/examples").get("get").getResponse(500);
		assertObjectEquals("{foo:'b'}", x.getExamples());
	}
}
