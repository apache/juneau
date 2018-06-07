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
import static org.junit.Assert.assertEquals;

import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests related to @ResponseStatus annotation.
 */
@SuppressWarnings("javadoc")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ResponseStatusAnnotationTest {
	
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
	// @ResponseStatus on POJO
	//=================================================================================================================

	@RestResource()
	public static class SA {
		@ResponseStatus({
			@Status(code=100),
			@Status(code=101)
		})
		public static class SA01 {}
		
		@RestMethod(name=GET,path="/code")
		public void sa01(SA01 r) {}

		@ResponseStatus({
			@Status(100),
			@Status(101)
		})
		public static class SA02 {}
		
		@RestMethod(name=GET,path="/salue")
		public void sa02(SA02 r) {}

		@ResponseStatus({
			@Status(code=100, description="a"),
			@Status(code=101, description="a\nb")
		})
		public static class SA03 {}
		
		@RestMethod(name=GET,path="/description")
		public void sa03(SA03 r) {}
	}

	@Test
	public void sa01_ResponseStatus_onPojo_code() throws Exception {
		Operation x = getSwagger(new SA()).getPaths().get("/code").get("get");
		assertEquals("Continue", x.getResponse(100).getDescription());
		assertEquals("Switching Protocols", x.getResponse(101).getDescription());
	}
	@Test
	public void sa02_ResponseStatus_onPojo_salue() throws Exception {
		Operation x = getSwagger(new SA()).getPaths().get("/salue").get("get");
		assertEquals("Continue", x.getResponse(100).getDescription());
		assertEquals("Switching Protocols", x.getResponse(101).getDescription());
	}
	@Test
	public void sa03_ResponseStatus_onPojo_description() throws Exception {
		Operation x = getSwagger(new SA()).getPaths().get("/description").get("get");
		assertEquals("a", x.getResponse(100).getDescription());
		assertEquals("a\nb", x.getResponse(101).getDescription());
	}
	
	//=================================================================================================================
	// @ResponseStatus on parameter
	//=================================================================================================================

	@RestResource()
	public static class SB {
		public static class SB01 {}
		
		@RestMethod(name=GET,path="/code")
		public void sb01(
				@ResponseStatus({
					@Status(code=100),
					@Status(code=101)
				})
				SB01 r
			) {}

		public static class SB02 {}
		
		@RestMethod(name=GET,path="/salue")
		public void sb02(
				@ResponseStatus({
					@Status(100),
					@Status(101)
				})
				SB02 r
			) {}

		public static class SB03 {}
		
		@RestMethod(name=GET,path="/description")
		public void sb03(
				@ResponseStatus({
					@Status(code=100, description="a"),
					@Status(code=101, description="a\nb")
				})
				SB03 r
			) {}
	}
	
	@Test
	public void sb01_ResponseStatus_onParameter_code() throws Exception {
		Operation x = getSwagger(new SB()).getPaths().get("/code").get("get");
		assertEquals("Continue", x.getResponse(100).getDescription());
		assertEquals("Switching Protocols", x.getResponse(101).getDescription());
	}
	@Test
	public void sb02_ResponseStatus_onParameter_sblue() throws Exception {
		Operation x = getSwagger(new SB()).getPaths().get("/salue").get("get");
		assertEquals("Continue", x.getResponse(100).getDescription());
		assertEquals("Switching Protocols", x.getResponse(101).getDescription());
	}
	@Test
	public void sb03_ResponseStatus_onParameter_description() throws Exception {
		Operation x = getSwagger(new SB()).getPaths().get("/description").get("get");
		assertEquals("a", x.getResponse(100).getDescription());
		assertEquals("a\nb", x.getResponse(101).getDescription());
	}
}
