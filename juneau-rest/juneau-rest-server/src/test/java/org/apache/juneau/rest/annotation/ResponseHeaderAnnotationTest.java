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
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests related to @ResponseHeader annotation.
 */
@SuppressWarnings("javadoc")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ResponseHeaderAnnotationTest {
	
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
	// @ResponseHeader on POJO
	//=================================================================================================================
	
	@RestResource
	public static class SA {

		@ResponseHeader(
			name="H",
			description="a",
			type="string"
		)
		public static class SA01 {}
		@RestMethod(name=GET,path="/basic")
		public void sa01(SA01 h) {}

		@ResponseHeader(
			name="H",
			api={
				"description:'a',",
				"type:'string'"
			}
		)
		public static class SA02 {}
		@RestMethod(name=GET,path="/api")
		public void sa02(SA02 h) {}

		@ResponseHeader(
			name="H",
			api={
				"description:'b',",
				"type:'number'"
			},
			description="a",
			type="string"
		)
		public static class SA03 {}
		@RestMethod(name=GET,path="/mixed")
		public void sa03(SA03 h) {}

		@ResponseHeader(name="H", code=100)
		public static class SA04 {}
		@RestMethod(name=GET,path="/code")
		public void sa04(SA04 h) {}

		@ResponseHeader(name="H", code={100,101})
		public static class SA05 {}
		@RestMethod(name=GET,path="/codes")
		public void sa05(SA05 h) {}

		@ResponseHeader(name="H", description="a")
		public static class SA07 {}
		@RestMethod(name=GET,path="/nocode")
		public void sa07(SA07 h) {}

		@ResponseHeader("H")
		public static class SA08 {}
		@RestMethod(name=GET,path="/value")
		public void sa08(SA08 h) {}
	}
	
	@Test
	public void sa01_ResponseHeader_onPojo_basic() throws Exception {
		HeaderInfo x = getSwagger(new SA()).getPaths().get("/basic").get("get").getResponse(200).getHeader("H");
		assertEquals("a", x.getDescription());
		assertEquals("string", x.getType());
	}
	@Test
	public void sa02_ResponseHeader_onPojo_api() throws Exception {
		HeaderInfo x = getSwagger(new SA()).getPaths().get("/api").get("get").getResponse(200).getHeader("H");
		assertEquals("a", x.getDescription());
		assertEquals("string", x.getType());
	}
	@Test
	public void sa03_ResponseHeader_onPojo_mixed() throws Exception {
		HeaderInfo x = getSwagger(new SA()).getPaths().get("/mixed").get("get").getResponse(200).getHeader("H");
		assertEquals("a", x.getDescription());
		assertEquals("string", x.getType());
	}
	@Test
	public void sa04_ResponseHeader_onPojo_code() throws Exception {
		HeaderInfo x = getSwagger(new SA()).getPaths().get("/code").get("get").getResponse(100).getHeader("H");
		assertNotNull(x);
	}
	@Test
	public void sa05_ResponseHeader_onPojo_codes() throws Exception {
		Operation x = getSwagger(new SA()).getPaths().get("/codes").get("get");
		assertNotNull(x.getResponse(100).getHeader("H"));
		assertNotNull(x.getResponse(101).getHeader("H"));
	}
	@Test
	public void sa07_ResponseHeader_onPojo_nocode() throws Exception {
		HeaderInfo x = getSwagger(new SA()).getPaths().get("/nocode").get("get").getResponse(200).getHeader("H");
		assertEquals("a", x.getDescription());
	}
	@Test
	public void sa08_ResponseHeader_onPojo_value() throws Exception {
		HeaderInfo x = getSwagger(new SA()).getPaths().get("/value").get("get").getResponse(200).getHeader("H");
		assertNotNull(x);
	}

	//=================================================================================================================
	// @ResponseHeader on parameter
	//=================================================================================================================
	
	@RestResource
	public static class SB {

		public static class SB01 {}
		@RestMethod(name=GET,path="/basic")
		public void sb01(
			@ResponseHeader(
				name="H",
				description="a",
				type="string"
			) SB01 h) {}
		
		public static class SB02 {}
		@RestMethod(name=GET,path="/api")
		public void sb02(
			@ResponseHeader(
				name="H",
				api={
					"description:'a',",
					"type:'string'"
				}
			) SB02 h) {}

		public static class SB03 {}
		@RestMethod(name=GET,path="/mixed")
		public void sb03(
			@ResponseHeader(
				name="H",
				api={
					"description:'b',",
					"type:'number'"
				},
				description="a",
				type="string"
			) SB03 h) {}

		public static class SB04 {}
		@RestMethod(name=GET,path="/code")
		public void sb04(@ResponseHeader(name="H", code=100) SB04 h) {}

		public static class SB05 {}
		@RestMethod(name=GET,path="/codes")
		public void sb05(@ResponseHeader(name="H", code={100,101}) SB05 h) {}

		public static class SB07 {}
		@RestMethod(name=GET,path="/nocode")
		public void sb07(@ResponseHeader(name="H", description="a") SB07 h) {}

		public static class SB08 {}
		@RestMethod(name=GET,path="/value")
		public void sb08(@ResponseHeader("H") SB08 h) {}
	}
	
	@Test
	public void sb01_ResponseHeader_onPojo_basic() throws Exception {
		HeaderInfo x = getSwagger(new SB()).getPaths().get("/basic").get("get").getResponse(200).getHeader("H");
		assertEquals("a", x.getDescription());
		assertEquals("string", x.getType());
	}
	@Test
	public void sb02_ResponseHeader_onPojo_api() throws Exception {
		HeaderInfo x = getSwagger(new SB()).getPaths().get("/api").get("get").getResponse(200).getHeader("H");
		assertEquals("a", x.getDescription());
		assertEquals("string", x.getType());
	}
	@Test
	public void sb03_ResponseHeader_onPojo_mixed() throws Exception {
		HeaderInfo x = getSwagger(new SB()).getPaths().get("/mixed").get("get").getResponse(200).getHeader("H");
		assertEquals("a", x.getDescription());
		assertEquals("string", x.getType());
	}
	@Test
	public void sb04_ResponseHeader_onPojo_code() throws Exception {
		HeaderInfo x = getSwagger(new SB()).getPaths().get("/code").get("get").getResponse(100).getHeader("H");
		assertNotNull(x);
	}
	@Test
	public void sb05_ResponseHeader_onPojo_codes() throws Exception {
		Operation x = getSwagger(new SB()).getPaths().get("/codes").get("get");
		assertNotNull(x.getResponse(100).getHeader("H"));
		assertNotNull(x.getResponse(101).getHeader("H"));
	}
	@Test
	public void sb07_ResponseHeader_onPojo_nocode() throws Exception {
		HeaderInfo x = getSwagger(new SB()).getPaths().get("/nocode").get("get").getResponse(200).getHeader("H");
		assertEquals("a", x.getDescription());
	}
	@Test
	public void sb08_ResponseHeader_onPojo_value() throws Exception {
		HeaderInfo x = getSwagger(new SB()).getPaths().get("/value").get("get").getResponse(200).getHeader("H");
		assertNotNull(x);
	}
}
