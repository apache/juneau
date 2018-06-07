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

	@RestResource()
	public static class SA {

		@Response(
			description={"a","b"},
			schema=@Schema(type="a"),
			headers=@ResponseHeader(name="foo",type="a"),
			example="'a'",
			examples=" {foo:'a'} "
		)
		public static class SA00 {}
		
		@RestMethod(name=GET,path="/basic")
		public void sa00(SA00 r) {}

		@Response(code=100)
		public static class SA01 {}
		
		@RestMethod(name=GET,path="/code")
		public void sa01(SA01 r) {}

		@Response(code=100)
		public static class SA02 {}
		
		@RestMethod(name=GET,path="/value")
		public void sa02(SA02 r) {}

		@Response(schema=@Schema(" type:'b' "))
		public static class SA04b {}
		
		@RestMethod(name=GET,path="/schema2")
		public void sa04b(SA04b r) {}

		@Response
		public static class SA04c {
			public String f1;
		}

		@RestMethod(name=GET,path="/schema3")
		public void sa04c(SA04c b) {}

		@Response
		public static class SA04d extends LinkedList<String> {
			private static final long serialVersionUID = 1L;
		}

		@RestMethod(name=GET,path="/schema4")
		public void sa04d(SA04d b) {}

		@Response
		public static class SA04e {}

		@RestMethod(name=GET,path="/schema5")
		public void sa04e(SA04e b) {}

		@Response(headers=@ResponseHeader(name="foo",api=" type:'b' "))
		public static class SA05b {}
		
		@RestMethod(name=GET,path="/headers2")
		public void sa05b(SA05b r) {}

		@Response(example="{f1:'a'}")
		public static class SA06b {}
		
		@RestMethod(name=GET,path="/example2")
		public void sa06b(SA06b r) {}

		@Response(examples={" foo:'b' "})
		public static class SA07b {}
		
		@RestMethod(name=GET,path="/examples2")
		public void sa07b(SA07b r) {}
	}
	
	@Test
	public void sa00_Response_onPojo_basic() throws Exception {
		ResponseInfo x = getSwagger(new SA()).getPaths().get("/basic").get("get").getResponse(200);
		assertEquals("a\nb", x.getDescription());
		assertObjectEquals("{type:'a'}", x.getSchema());
		assertObjectEquals("{foo:{type:'a'}}", x.getHeaders());
		assertObjectEquals("'a'", x.getExample());
		assertObjectEquals("{foo:'a'}", x.getExamples());
	}
	@Test
	public void sa01_Response_onPojo_code() throws Exception {
		ResponseInfo x = getSwagger(new SA()).getPaths().get("/code").get("get").getResponse(100);
		assertEquals("Continue", x.getDescription());
	}
	@Test
	public void sa02_Response_onPojo_value() throws Exception {
		ResponseInfo x = getSwagger(new SA()).getPaths().get("/value").get("get").getResponse(100);
		assertEquals("Continue", x.getDescription());
	}
	@Test
	public void sa04b_Response_onPojo_schema2() throws Exception {
		ResponseInfo x = getSwagger(new SA()).getPaths().get("/schema2").get("get").getResponse(200);
		assertObjectEquals("{type:'b'}", x.getSchema());
	}
	@Test
	public void sa04c_Response_onPojo_schema3() throws Exception {
		ResponseInfo x = getSwagger(new SA()).getPaths().get("/schema3").get("get").getResponse(200);
		assertObjectEquals("{type:'object',properties:{f1:{type:'string'}}}", x.getSchema());
	}
	@Test
	public void sa04d_Response_onPojo_schema4() throws Exception {
		ResponseInfo x = getSwagger(new SA()).getPaths().get("/schema4").get("get").getResponse(200);
		assertObjectEquals("{type:'array',items:{type:'string'}}", x.getSchema());
	}
	@Test
	public void sa04e_Response_onPojo_schema5() throws Exception {
		ResponseInfo x = getSwagger(new SA()).getPaths().get("/schema5").get("get").getResponse(200);
		assertObjectEquals("{type:'string'}", x.getSchema());
	}
	@Test
	public void sa05b_Response_onPojo_headers2() throws Exception {
		ResponseInfo x = getSwagger(new SA()).getPaths().get("/headers2").get("get").getResponse(200);
		assertObjectEquals("{foo:{type:'b'}}", x.getHeaders());
	}
	@Test
	public void sa06b_Response_onPojo_example2() throws Exception {
		ResponseInfo x = getSwagger(new SA()).getPaths().get("/example2").get("get").getResponse(200);
		assertObjectEquals("{f1:'a'}", x.getExample());
	}
	@Test
	public void sa07b_Response_onPojo_examples2() throws Exception {
		ResponseInfo x = getSwagger(new SA()).getPaths().get("/examples2").get("get").getResponse(200);
		assertObjectEquals("{foo:'b'}", x.getExamples());
	}
	
	//=================================================================================================================
	// @Response on parameter
	//=================================================================================================================

	@RestResource()
	public static class SB {

		public static class SB00 {}

		@RestMethod(name=GET,path="/basic")
		public void sb00(
			@Response(
				description={"a","b"},
				schema=@Schema(type="a"),
				headers=@ResponseHeader(name="foo",type="a"),
				example=" 'a' ",
				examples=" {foo:'a'} "
			) SB00 r
		) {}

		public static class SB01 {}

		@RestMethod(name=GET,path="/code")
		public void sb01(@Response(code=100) SB01 r) {}

		public static class SB02 {}

		@RestMethod(name=GET,path="/value")
		public void sb02(@Response(code=100) SB02 r) {}

		public static class SB05b {}

		@RestMethod(name=GET,path="/schema2")
		public void sb05b(@Response(schema=@Schema(" type:'b' ")) SB05b r) {}

		public static class SB05c {
			public String f1;
		}

		@RestMethod(name=GET,path="/schema3")
		public void sb05c(@Response SB05c b) {}

		public static class SB05d extends LinkedList<String> {
			private static final long serialVersionUID = 1L;
		}

		@RestMethod(name=GET,path="/schema4")
		public void sb05d(@Response SB05d b) {}

		public static class SB05e {}

		@RestMethod(name=GET,path="/schema5")
		public void sb05e(@Response SB05e b) {}

		public static class SB08 {}

		@RestMethod(name=GET,path="/headers2")
		public void sb08(@Response(headers=@ResponseHeader(name="foo",api=" type:'b' ")) SB08 r) {}

		public static class SB10 {}

		@RestMethod(name=GET,path="/example2")
		public void sb10(@Response(example=" {f1:'b'} ") SB10 r) {}

		public static class SB12 {}

		@RestMethod(name=GET,path="/examples2")
		public void sb12(@Response(examples={" foo:'b' "}) SB12 r) {}
	}
	
	@Test
	public void sb00_Response_onParameter_basic() throws Exception {
		ResponseInfo x = getSwagger(new SB()).getPaths().get("/basic").get("get").getResponse(200);
		assertEquals("a\nb", x.getDescription());
		assertObjectEquals("{type:'a'}", x.getSchema());
		assertObjectEquals("{foo:{type:'a'}}", x.getHeaders());
		assertObjectEquals("'a'", x.getExample());
		assertObjectEquals("{foo:'a'}", x.getExamples());
	}
	@Test
	public void sb01_Response_onParameter_code() throws Exception {
		ResponseInfo x = getSwagger(new SB()).getPaths().get("/code").get("get").getResponse(100);
		assertEquals("Continue", x.getDescription());
	}
	@Test
	public void sb02_Response_onParameter_value() throws Exception {
		ResponseInfo x = getSwagger(new SB()).getPaths().get("/value").get("get").getResponse(100);
		assertEquals("Continue", x.getDescription());
	}
	@Test
	public void sb05b_Response_onParameter_schema2() throws Exception {
		ResponseInfo x = getSwagger(new SB()).getPaths().get("/schema2").get("get").getResponse(200);
		assertObjectEquals("{type:'b'}", x.getSchema());
	}
	@Test
	public void sb05c_Response_onParameter_schema3() throws Exception {
		ResponseInfo x = getSwagger(new SB()).getPaths().get("/schema3").get("get").getResponse(200);
		assertObjectEquals("{type:'object',properties:{f1:{type:'string'}}}", x.getSchema());
	}
	@Test
	public void sb05d_Response_onParameter_schema4() throws Exception {
		ResponseInfo x = getSwagger(new SB()).getPaths().get("/schema4").get("get").getResponse(200);
		assertObjectEquals("{type:'array',items:{type:'string'}}", x.getSchema());
	}
	@Test
	public void sb05e_Response_onParameter_schema5() throws Exception {
		ResponseInfo x = getSwagger(new SB()).getPaths().get("/schema5").get("get").getResponse(200);
		assertObjectEquals("{type:'string'}", x.getSchema());
	}
	@Test
	public void sb08_Response_onParameter_headers2() throws Exception {
		ResponseInfo x = getSwagger(new SB()).getPaths().get("/headers2").get("get").getResponse(200);
		assertObjectEquals("{foo:{type:'b'}}", x.getHeaders());
	}
	@Test
	public void sb10_Response_onParameter_example2() throws Exception {
		ResponseInfo x = getSwagger(new SB()).getPaths().get("/example2").get("get").getResponse(200);
		assertObjectEquals("{f1:'b'}", x.getExample());
	}
	@Test
	public void sb12_Response_onParameter_examples2() throws Exception {
		ResponseInfo x = getSwagger(new SB()).getPaths().get("/examples2").get("get").getResponse(200);
		assertObjectEquals("{foo:'b'}", x.getExamples());
	}

	//=================================================================================================================
	// @Response on throwable
	//=================================================================================================================

	@RestResource()
	@SuppressWarnings({"unused"})
	public static class SC {		
		
		@Response(
			description= {"a","b"},
			schema=@Schema(type="a"),
			headers=@ResponseHeader(name="foo",type="a"),
			example=" 'a' ",
			examples=" {foo:'a'} "
		)
		public static class SC00 extends Throwable {}

		@RestMethod(name=GET,path="/basic")
		public void sc00() throws SC00 {}

		@Response(code=100)
		public static class SC01 extends Throwable {}

		@RestMethod(name=GET,path="/code")
		public void sc01() throws SC01 {}

		@Response(code=100)
		public static class SC02 extends Throwable {}

		@RestMethod(name=GET,path="/value")
		public void sc02() throws SC02 {}

		@Response(schema=@Schema(" type:'b' "))
		public static class SC03 extends Throwable {}

		@RestMethod(name=GET,path="/schema1")
		public void sc03() throws SC03 {}

		@Response(headers=@ResponseHeader(name="foo", api=" {type:'b'} "))
		public static class SC04 extends Throwable {}

		@RestMethod(name=GET,path="/headers1")
		public void sc04() throws SC04 {}

		@Response(example={" {f1:'b'} "})
		public static class SC05 extends Throwable {}

		@RestMethod(name=GET,path="/example1")
		public void sc05() throws SC05 {}

		@Response(examples={" foo:'b' "})
		public static class SC06 extends Throwable {}

		@RestMethod(name=GET,path="/examples1")
		public void sc06() throws SC06 {}
	}
	
	@Test
	public void sc00_Response_onThrowable_basic() throws Exception {
		ResponseInfo x = getSwagger(new SC()).getPaths().get("/basic").get("get").getResponse(500);
		assertEquals("a\nb", x.getDescription());
		assertObjectEquals("{type:'a'}", x.getSchema());
		assertObjectEquals("{foo:{type:'a'}}", x.getHeaders());
		assertObjectEquals("'a'", x.getExample());
		assertObjectEquals("{foo:'a'}", x.getExamples());
	}
	@Test
	public void sc01_Response_onThrowable_code() throws Exception {
		ResponseInfo x = getSwagger(new SC()).getPaths().get("/code").get("get").getResponse(100);
		assertEquals("Continue", x.getDescription());
	}
	@Test
	public void sc02_Response_onThrowable_value() throws Exception {
		ResponseInfo x = getSwagger(new SC()).getPaths().get("/value").get("get").getResponse(100);
		assertEquals("Continue", x.getDescription());
	}
	@Test
	public void sc03_Response_onThrowable_schema1() throws Exception {
		ResponseInfo x = getSwagger(new SC()).getPaths().get("/schema1").get("get").getResponse(500);
		assertObjectEquals("{type:'b'}", x.getSchema());
	}
	@Test
	public void sc04_Response_onThrowable_headers1() throws Exception {
		ResponseInfo x = getSwagger(new SC()).getPaths().get("/headers1").get("get").getResponse(500);
		assertObjectEquals("{foo:{type:'b'}}", x.getHeaders());
	}
	@Test
	public void sc05_Response_onThrowable_example1() throws Exception {
		ResponseInfo x = getSwagger(new SC()).getPaths().get("/example1").get("get").getResponse(500);
		assertObjectEquals("{f1:'b'}", x.getExample());
	}
	@Test
	public void sc6_Response_onThrowable_examples1() throws Exception {
		ResponseInfo x = getSwagger(new SC()).getPaths().get("/examples1").get("get").getResponse(500);
		assertObjectEquals("{foo:'b'}", x.getExamples());
	}

	//=================================================================================================================
	// @Responses on POJO
	//=================================================================================================================
	
	@RestResource()
	public static class SD {

		@Responses({
			@Response(code=100),
			@Response(code=101)
		})
		public static class SD01 {}
		
		@RestMethod(name=GET,path="/code")
		public void sd01(SD01 r) {}

		@Responses({
			@Response(code=100),
			@Response(code=101)
		})
		public static class SD02 {}
		
		@RestMethod(name=GET,path="/value")
		public void sd02(SD02 r) {}

		@Responses({
			@Response(code=100, description="a"),
			@Response(code=101, description={"a","b"})
		})
		public static class SD03 {}
		
		@RestMethod(name=GET,path="/description")
		public void sd03(SD03 r) {}

		@Responses({
			@Response(code=100, schema=@Schema(type="a")),
			@Response(code=101, schema=@Schema(" type:'b' "))
		})
		public static class SD04 {}
		
		@RestMethod(name=GET,path="/schema")
		public void sd04(SD04 r) {}

		@Responses({
			@Response(code=100, headers=@ResponseHeader(name="foo", type="a")),
			@Response(code=101, headers=@ResponseHeader(name="foo", api=" type:'b' "))
		})
		public static class SD05 {}
		
		@RestMethod(name=GET,path="/headers")
		public void sd05(SD05 r) {}

		@Responses({
			@Response(code=100, example="'a'"),
			@Response(code=101, example="{f1:'a'}")
		})
		public static class SD06 {}
		
		@RestMethod(name=GET,path="/example")
		public void sd06(SD06 r) {}

		@Responses({
			@Response(code=100, examples=" {foo:'a'} "),
			@Response(code=101, examples={" foo:'b' "})
		})
		public static class SD07 {}
		
		@RestMethod(name=GET,path="/examples")
		public void sd07(SD07 r) {}
	}
	
	@Test
	public void sd01_Responses_onPojo_code() throws Exception {
		Operation x = getSwagger(new SD()).getPaths().get("/code").get("get");
		assertEquals("Continue", x.getResponse(100).getDescription());
		assertEquals("Switching Protocols", x.getResponse(101).getDescription());
	}
	@Test
	public void sd02_Responses_onPojo_value() throws Exception {
		Operation x = getSwagger(new SD()).getPaths().get("/value").get("get");
		assertEquals("Continue", x.getResponse(100).getDescription());
		assertEquals("Switching Protocols", x.getResponse(101).getDescription());
	}
	@Test
	public void sd03_Responses_onPojo_description() throws Exception {
		Operation x = getSwagger(new SD()).getPaths().get("/description").get("get");
		assertEquals("a", x.getResponse(100).getDescription());
		assertEquals("a\nb", x.getResponse(101).getDescription());
	}
	@Test
	public void sd04_Responses_onPojo_schema() throws Exception {
		Operation x = getSwagger(new SD()).getPaths().get("/schema").get("get");
		assertObjectEquals("{type:'a'}", x.getResponse(100).getSchema());
		assertObjectEquals("{type:'b'}", x.getResponse(101).getSchema());
	}
	@Test
	public void sd05_Responses_onPojo_headers() throws Exception {
		Operation x = getSwagger(new SD()).getPaths().get("/headers").get("get");
		assertObjectEquals("{foo:{type:'a'}}", x.getResponse(100).getHeaders());
		assertObjectEquals("{foo:{type:'b'}}", x.getResponse(101).getHeaders());
	}
	@Test
	public void sd06_Responses_onPojo_example() throws Exception {
		Operation x = getSwagger(new SD()).getPaths().get("/example").get("get");
		assertObjectEquals("'a'", x.getResponse(100).getExample());
		assertObjectEquals("{f1:'a'}", x.getResponse(101).getExample());
	}
	@Test
	public void sd07_Responses_onPojo_examples() throws Exception {
		Operation x = getSwagger(new SD()).getPaths().get("/examples").get("get");
		assertObjectEquals("{foo:'a'}", x.getResponse(100).getExamples());
		assertObjectEquals("{foo:'b'}", x.getResponse(101).getExamples());
	}
	
	//=================================================================================================================
	// @Responses on parameter
	//=================================================================================================================

	@RestResource()
	public static class SE {

		public static class SE01 {}

		@RestMethod(name=GET,path="/code")
		public void se01(
			@Responses({
				@Response(code=100),
				@Response(code=101)
			}) 
			SE01 r) {}

		public static class SE02 {}

		@RestMethod(name=GET,path="/valse")
		public void se02(
			@Responses({
				@Response(code=100),
				@Response(code=101)
			})
			SE02 r) {}

		public static class SE03 {}

		@RestMethod(name=GET,path="/description")
		public void se03(
			@Responses({
				@Response(code=100, description="a"),
				@Response(code=101, description={"a","b"})
			})
			SE03 r) {}

		public static class SE04 {}

		@RestMethod(name=GET,path="/schema")
		public void se04(
			@Responses({
				@Response(code=100, schema=@Schema(type="a")),
				@Response(code=101, schema=@Schema(" type:'b' "))
			})
			SE04 r) {}

		public static class SE05 {}

		@RestMethod(name=GET,path="/headers")
		public void se05(
			@Responses({
				@Response(code=100, headers=@ResponseHeader(name="foo",type="a")),
				@Response(code=101, headers=@ResponseHeader(name="foo", api=" type:'b' "))
			})
			SE05 r) {}

		public static class SE06 {}

		@RestMethod(name=GET,path="/example")
		public void se06(
			@Responses({
				@Response(code=100, example=" 'a' "),
				@Response(code=101, example=" {f1:'b'} ")
			})
			SE06 r) {}

		public static class SE07 {}

		@RestMethod(name=GET,path="/examples")
		public void se07(
			@Responses({
				@Response(code=100, examples=" {foo:'a'} "),
				@Response(code=101, examples={" foo:'b' "})
			})
			SE07 r) {}
	}
	
	@Test
	public void se01_Responses_onParameter_code() throws Exception {
		Operation x = getSwagger(new SE()).getPaths().get("/code").get("get");
		assertEquals("Continue", x.getResponse(100).getDescription());
		assertEquals("Switching Protocols", x.getResponse(101).getDescription());
	}
	@Test
	public void se02_Responses_onParameter_valse() throws Exception {
		Operation x = getSwagger(new SE()).getPaths().get("/valse").get("get");
		assertEquals("Continue", x.getResponse(100).getDescription());
		assertEquals("Switching Protocols", x.getResponse(101).getDescription());
	}
	@Test
	public void se03_Responses_onParameter_description() throws Exception {
		Operation x = getSwagger(new SE()).getPaths().get("/description").get("get");
		assertEquals("a", x.getResponse(100).getDescription());
		assertEquals("a\nb", x.getResponse(101).getDescription());
	}
	@Test
	public void se04_Responses_onParameter_schema() throws Exception {
		Operation x = getSwagger(new SE()).getPaths().get("/schema").get("get");
		assertObjectEquals("{type:'a'}", x.getResponse(100).getSchema());
		assertObjectEquals("{type:'b'}", x.getResponse(101).getSchema());
	}
	@Test
	public void se05_Responses_onParameter_headers() throws Exception {
		Operation x = getSwagger(new SE()).getPaths().get("/headers").get("get");
		assertObjectEquals("{foo:{type:'a'}}", x.getResponse(100).getHeaders());
		assertObjectEquals("{foo:{type:'b'}}", x.getResponse(101).getHeaders());
	}
	@Test
	public void se06_Responses_onParameter_example() throws Exception {
		Operation x = getSwagger(new SE()).getPaths().get("/example").get("get");
		assertObjectEquals("'a'", x.getResponse(100).getExample());
		assertObjectEquals("{f1:'b'}", x.getResponse(101).getExample());
	}
	@Test
	public void se07_Responses_onParameter_examples() throws Exception {
		Operation x = getSwagger(new SE()).getPaths().get("/examples").get("get");
		assertObjectEquals("{foo:'a'}", x.getResponse(100).getExamples());
		assertObjectEquals("{foo:'b'}", x.getResponse(101).getExamples());
	}
}
