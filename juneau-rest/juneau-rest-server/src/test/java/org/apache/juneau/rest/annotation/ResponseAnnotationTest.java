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

import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.mock.*;
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
	// @Response on POJO
	//=================================================================================================================

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource
	public static class SA {

		@Response(
			description={"a","b"},
			schema=@Schema(type="a"),
			headers=@ResponseHeader(name="foo",type="a"),
			example="'a'",
			examples=" {foo:'a'} "
		)
		public static class SA01 {}
		@RestMethod(name=GET,path="/basic")
		public void sa01(SA01 r) {}

		@Response(
			api={
				"description:'a\nb',",
				"schema:{type:'a'},",
				"headers:{foo:{type:'a'}},",
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
				"schema:{type:'b'},",
				"headers:{bar:{type:'b'}},",
				"example:'b',",
				"examples:{bar:'b'}"
			},
			description={"a","b"},
			schema=@Schema(type="a"),
			headers=@ResponseHeader(name="foo",type="a"),
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
		assertObjectEquals("{type:'a'}", x.getSchema());
		assertObjectEquals("{foo:{type:'a'}}", x.getHeaders());
		assertObjectEquals("'a'", x.getExample());
		assertObjectEquals("{foo:'a'}", x.getExamples());
	}
	@Test
	public void sa02_Response_onPojo_api() throws Exception {
		ResponseInfo x = getSwagger(new SA()).getPaths().get("/api").get("get").getResponse(200);
		assertEquals("a\nb", x.getDescription());
		assertObjectEquals("{type:'a'}", x.getSchema());
		assertObjectEquals("{foo:{type:'a'}}", x.getHeaders());
		assertObjectEquals("'a'", x.getExample());
		assertObjectEquals("{foo:'a'}", x.getExamples());
	}
	@Test
	public void sa03_Response_onPojo_mixed() throws Exception {
		ResponseInfo x = getSwagger(new SA()).getPaths().get("/mixed").get("get").getResponse(200);
		assertEquals("a\nb", x.getDescription());
		assertObjectEquals("{type:'a'}", x.getSchema());
		assertObjectEquals("{foo:{type:'a'}}", x.getHeaders());
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

		@Response(schema=@Schema(" type:'b' "))
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
		assertObjectEquals("{type:'b'}", x.getSchema());
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
				schema=@Schema(type="a"),
				headers=@ResponseHeader(name="foo",type="a"),
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
					"schema:{type:'a'},",
					"headers:{foo:{type:'a'}},",
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
					"schema:{type:'b'},",
					"headers:{bar:{type:'b'}},",
					"example:'b',",
					"examples:{bar:'b'}"
				},
				description={"a","b"},
				schema=@Schema(type="a"),
				headers=@ResponseHeader(name="foo",type="a"),
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
		public void ta06(@Response(headers=@ResponseHeader(name="foo",api=" type:'b' ")) TA06 r) {}
	}

	@Test
	public void ta01_Response_onParameter_basic() throws Exception {
		ResponseInfo x = getSwagger(new TA()).getPaths().get("/basic").get("get").getResponse(200);
		assertEquals("a\nb", x.getDescription());
		assertObjectEquals("{type:'a'}", x.getSchema());
		assertObjectEquals("{foo:{type:'a'}}", x.getHeaders());
		assertObjectEquals("'a'", x.getExample());
		assertObjectEquals("{foo:'a'}", x.getExamples());
	}
	@Test
	public void ta02_Response_onParameter_api() throws Exception {
		ResponseInfo x = getSwagger(new TA()).getPaths().get("/api").get("get").getResponse(200);
		assertEquals("a\nb", x.getDescription());
		assertObjectEquals("{type:'a'}", x.getSchema());
		assertObjectEquals("{foo:{type:'a'}}", x.getHeaders());
		assertObjectEquals("'a'", x.getExample());
		assertObjectEquals("{foo:'a'}", x.getExamples());
	}
	@Test
	public void ta03_Response_onParameter_mixed() throws Exception {
		ResponseInfo x = getSwagger(new TA()).getPaths().get("/mixed").get("get").getResponse(200);
		assertEquals("a\nb", x.getDescription());
		assertObjectEquals("{type:'a'}", x.getSchema());
		assertObjectEquals("{foo:{type:'a'}}", x.getHeaders());
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
		assertObjectEquals("{foo:{type:'b'}}", x.getHeaders());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Schema
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource
	public static class TB {

		public static class TB01 {}
		@RestMethod(name=GET,path="/schemaValue")
		public void tb01(@Response(schema=@Schema(" type:'b' ")) TB01 r) {}

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
		assertObjectEquals("{type:'b'}", x.getSchema());
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
			schema=@Schema(type="a"),
			headers=@ResponseHeader(name="foo",type="a"),
			example=" 'a' ",
			examples=" {foo:'a'} "
		)
		public static class UA01 extends Throwable {}
		@RestMethod(name=GET,path="/basic")
		public void ua01() throws UA01 {}

		@Response(
			api={
				"description:'a\nb',",
				"schema:{type:'a'},",
				"headers:{foo:{type:'a'}},",
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
				"schema:{type:'b'},",
				"headers:{bar:{type:'b'}},",
				"example:'b',",
				"examples:{bar:'b'}"
			},
			description= {"a","b"},
			schema=@Schema(type="a"),
			headers=@ResponseHeader(name="foo",type="a"),
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

		@Response(headers=@ResponseHeader(name="foo", api=" {type:'b'} "))
		public static class UA06 extends Throwable {}
		@RestMethod(name=GET,path="/headers")
		public void ua06() throws UA06 {}
	}
	
	@Test
	public void ua01_Response_onThrowable_basic() throws Exception {
		ResponseInfo x = getSwagger(new UA()).getPaths().get("/basic").get("get").getResponse(500);
		assertEquals("a\nb", x.getDescription());
		assertObjectEquals("{type:'a'}", x.getSchema());
		assertObjectEquals("{foo:{type:'a'}}", x.getHeaders());
		assertObjectEquals("'a'", x.getExample());
		assertObjectEquals("{foo:'a'}", x.getExamples());
	}
	@Test
	public void ua02_Response_onThrowable_api() throws Exception {
		ResponseInfo x = getSwagger(new UA()).getPaths().get("/api").get("get").getResponse(500);
		assertEquals("a\nb", x.getDescription());
		assertObjectEquals("{type:'a'}", x.getSchema());
		assertObjectEquals("{foo:{type:'a'}}", x.getHeaders());
		assertObjectEquals("'a'", x.getExample());
		assertObjectEquals("{foo:'a'}", x.getExamples());
	}
	@Test
	public void ua03_Response_onThrowable_mixed() throws Exception {
		ResponseInfo x = getSwagger(new UA()).getPaths().get("/mixed").get("get").getResponse(500);
		assertEquals("a\nb", x.getDescription());
		assertObjectEquals("{type:'a'}", x.getSchema());
		assertObjectEquals("{foo:{type:'a'}}", x.getHeaders());
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
		assertObjectEquals("{foo:{type:'b'}}", x.getHeaders());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Schema
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource
	@SuppressWarnings({"unused"})
	public static class UB {		
		
		@Response(schema=@Schema(" type:'b' "))
		public static class UB01 extends Throwable {}
		@RestMethod(name=GET,path="/schemaValue")
		public void ub01() throws UB01 {}
	}

	@Test
	public void ub01_Response_onThrowable_schemaValue() throws Exception {
		ResponseInfo x = getSwagger(new UB()).getPaths().get("/schemaValue").get("get").getResponse(500);
		assertObjectEquals("{type:'b'}", x.getSchema());
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
