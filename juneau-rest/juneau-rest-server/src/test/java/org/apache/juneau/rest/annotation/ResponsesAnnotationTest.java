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


import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests the @Responses annotation.
 */
@SuppressWarnings({"javadoc"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ResponsesAnnotationTest {
	
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
	// @Responses on POJO
	//=================================================================================================================
	
	@RestResource
	public static class A {

		@Responses({
			@Response(code=100),
			@Response(code=101)
		})
		public static class A01 {}
		@RestMethod(name=GET,path="/code")
		public void a01(A01 r) {}

		@Responses({
			@Response(code=100),
			@Response(code=101)
		})
		public static class A02 {}
		@RestMethod(name=GET,path="/value")
		public void a02(A02 r) {}

		@Responses({
			@Response(code=100, description="a"),
			@Response(code=101, description={"a","b"})
		})
		public static class A03 {}
		@RestMethod(name=GET,path="/description")
		public void a03(A03 r) {}

		@Responses({
			@Response(code=100, schema=@Schema(type="number")),
			@Response(code=101, schema=@Schema(" type:'integer' "))
		})
		public static class A04 {}
		@RestMethod(name=GET,path="/schema")
		public void a04(A04 r) {}

		@Responses({
			@Response(code=100, headers=@ResponseHeader(name="foo", type="number")),
			@Response(code=101, headers=@ResponseHeader(name="foo", api=" type:'integer' "))
		})
		public static class A05 {}
		@RestMethod(name=GET,path="/headers")
		public void a05(A05 r) {}

		@Responses({
			@Response(code=100, example="'a'"),
			@Response(code=101, example="{f1:'a'}")
		})
		public static class A06 {}
		@RestMethod(name=GET,path="/example")
		public void a06(A06 r) {}

		@Responses({
			@Response(code=100, examples=" {foo:'a'} "),
			@Response(code=101, examples={" foo:'b' "})
		})
		public static class A07 {}
		@RestMethod(name=GET,path="/examples")
		public void a07(A07 r) {}
	}
	
	@Test
	public void a01_Responses_onPojo_code() throws Exception {
		Operation x = getSwagger(new A()).getPaths().get("/code").get("get");
		assertEquals("Continue", x.getResponse(100).getDescription());
		assertEquals("Switching Protocols", x.getResponse(101).getDescription());
	}
	@Test
	public void a02_Responses_onPojo_value() throws Exception {
		Operation x = getSwagger(new A()).getPaths().get("/value").get("get");
		assertEquals("Continue", x.getResponse(100).getDescription());
		assertEquals("Switching Protocols", x.getResponse(101).getDescription());
	}
	@Test
	public void a03_Responses_onPojo_description() throws Exception {
		Operation x = getSwagger(new A()).getPaths().get("/description").get("get");
		assertEquals("a", x.getResponse(100).getDescription());
		assertEquals("a\nb", x.getResponse(101).getDescription());
	}
	@Test
	public void a04_Responses_onPojo_schema() throws Exception {
		Operation x = getSwagger(new A()).getPaths().get("/schema").get("get");
		assertObjectEquals("{type:'number'}", x.getResponse(100).getSchema());
		assertObjectEquals("{type:'integer'}", x.getResponse(101).getSchema());
	}
	@Test
	public void a05_Responses_onPojo_headers() throws Exception {
		Operation x = getSwagger(new A()).getPaths().get("/headers").get("get");
		assertObjectEquals("{foo:{type:'number'}}", x.getResponse(100).getHeaders());
		assertObjectEquals("{foo:{type:'integer'}}", x.getResponse(101).getHeaders());
	}
	@Test
	public void a06_Responses_onPojo_example() throws Exception {
		Operation x = getSwagger(new A()).getPaths().get("/example").get("get");
		assertObjectEquals("'a'", x.getResponse(100).getExample());
		assertObjectEquals("{f1:'a'}", x.getResponse(101).getExample());
	}
	@Test
	public void a07_Responses_onPojo_examples() throws Exception {
		Operation x = getSwagger(new A()).getPaths().get("/examples").get("get");
		assertObjectEquals("{foo:'a'}", x.getResponse(100).getExamples());
		assertObjectEquals("{foo:'b'}", x.getResponse(101).getExamples());
	}
	
	//=================================================================================================================
	// @Responses on parameter
	//=================================================================================================================

	@RestResource
	public static class B {

		public static class B01 {}
		@RestMethod(name=GET,path="/code")
		public void b01(
			@Responses({
				@Response(code=100),
				@Response(code=101)
			}) 
			B01 r) {}

		public static class B02 {}
		@RestMethod(name=GET,path="/value")
		public void b02(
			@Responses({
				@Response(code=100),
				@Response(code=101)
			})
			B02 r) {}

		public static class B03 {}
		@RestMethod(name=GET,path="/description")
		public void b03(
			@Responses({
				@Response(code=100, description="a"),
				@Response(code=101, description={"a","b"})
			})
			B03 r) {}

		public static class B04 {}
		@RestMethod(name=GET,path="/schema")
		public void b04(
			@Responses({
				@Response(code=100, schema=@Schema(type="number")),
				@Response(code=101, schema=@Schema(" type:'integer' "))
			})
			B04 r) {}

		public static class B05 {}
		@RestMethod(name=GET,path="/headers")
		public void b05(
			@Responses({
				@Response(code=100, headers=@ResponseHeader(name="foo",type="number")),
				@Response(code=101, headers=@ResponseHeader(name="foo", api=" type:'integer' "))
			})
			B05 r) {}

		public static class B06 {}
		@RestMethod(name=GET,path="/example")
		public void b06(
			@Responses({
				@Response(code=100, example=" 'a' "),
				@Response(code=101, example=" {f1:'b'} ")
			})
			B06 r) {}

		public static class B07 {}
		@RestMethod(name=GET,path="/examples")
		public void b07(
			@Responses({
				@Response(code=100, examples=" {foo:'a'} "),
				@Response(code=101, examples={" foo:'b' "})
			})
			B07 r) {}
	}
	
	@Test
	public void b01_Response_onParameter_code() throws Exception {
		Operation x = getSwagger(new B()).getPaths().get("/code").get("get");
		assertEquals("Continue", x.getResponse(100).getDescription());
		assertEquals("Switching Protocols", x.getResponse(101).getDescription());
	}
	@Test
	public void b02_Response_onParameter_value() throws Exception {
		Operation x = getSwagger(new B()).getPaths().get("/value").get("get");
		assertEquals("Continue", x.getResponse(100).getDescription());
		assertEquals("Switching Protocols", x.getResponse(101).getDescription());
	}
	@Test
	public void b03_Response_onParameter_description() throws Exception {
		Operation x = getSwagger(new B()).getPaths().get("/description").get("get");
		assertEquals("a", x.getResponse(100).getDescription());
		assertEquals("a\nb", x.getResponse(101).getDescription());
	}
	@Test
	public void b04_Response_onParameter_schema() throws Exception {
		Operation x = getSwagger(new B()).getPaths().get("/schema").get("get");
		assertObjectEquals("{type:'number'}", x.getResponse(100).getSchema());
		assertObjectEquals("{type:'integer'}", x.getResponse(101).getSchema());
	}
	@Test
	public void b05_Response_onParameter_headers() throws Exception {
		Operation x = getSwagger(new B()).getPaths().get("/headers").get("get");
		assertObjectEquals("{foo:{type:'number'}}", x.getResponse(100).getHeaders());
		assertObjectEquals("{foo:{type:'integer'}}", x.getResponse(101).getHeaders());
	}
	@Test
	public void b06_Response_onParameter_example() throws Exception {
		Operation x = getSwagger(new B()).getPaths().get("/example").get("get");
		assertObjectEquals("'a'", x.getResponse(100).getExample());
		assertObjectEquals("{f1:'b'}", x.getResponse(101).getExample());
	}
	@Test
	public void b07_Response_onParameter_examples() throws Exception {
		Operation x = getSwagger(new B()).getPaths().get("/examples").get("get");
		assertObjectEquals("{foo:'a'}", x.getResponse(100).getExamples());
		assertObjectEquals("{foo:'b'}", x.getResponse(101).getExamples());
	}
}
