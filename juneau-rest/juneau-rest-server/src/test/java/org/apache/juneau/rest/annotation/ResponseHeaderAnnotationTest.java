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
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.Items;
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
	
	@RestResource()
	public static class SA {

		@ResponseHeader(
			name="H",
			description="a",
			type="a",
			format="a",
			collectionFormat="a",
			maximum="1",
			minimum="1",
			multipleOf="1",
			maxLength="1",
			minLength="1",
			maxItems="1",
			minItems="1",
			exclusiveMaximum="true",
			exclusiveMinimum="true",
			uniqueItems="true",
			items=@Items(type="a"),
			_default="'a'",
			_enum=" a,b ",
			example="'a'"
		)
		public static class SA00 {}
		
		@RestMethod(name=GET,path="/basic")
		public void sa00(SA00 h) {}

		@ResponseHeader(name="H", code=100)
		public static class SA01a {}
		
		@RestMethod(name=GET,path="/code")
		public void sa01a(SA01a h) {}

		@ResponseHeader(name="H", codes={100,101})
		public static class SA01b {}
		
		@RestMethod(name=GET,path="/codes")
		public void sa01b(SA01b h) {}

		@ResponseHeader(name="H", code=100,codes={101})
		public static class SA01c {}
		
		@RestMethod(name=GET,path="/codeAndCodes")
		public void sa01c(SA01c h) {}

		@ResponseHeader(name="H", description="a")
		public static class SA01d {}
		
		@RestMethod(name=GET,path="/nocode")
		public void sa01d(SA01d h) {}

		@ResponseHeader("H")
		public static class SA02 {}
		
		@RestMethod(name=GET,path="/value")
		public void sa02(SA02 h) {}

		@ResponseHeader(name="H", items=@Items(" type:'b' "))
		public static class SA03 {}
		
		@RestMethod(name=GET,path="/items1")
		public void sa03(SA03 h) {}

		@ResponseHeader(name="H", _default={" {f1:'b'} "})
		public static class SA04 {}
		
		@RestMethod(name=GET,path="/default1")
		public void sa04(SA04 h) {}

		@ResponseHeader(name="H", _enum={" ['a','b'] "})
		public static class SA05 {}
		
		@RestMethod(name=GET,path="/enum1")
		public void sa05(SA05 h) {}

		@ResponseHeader(name="H", example={" {f1:'b'} "})
		public static class SA06 {}
		
		@RestMethod(name=GET,path="/example1")
		public void sa06(SA06 h) {}
	}
	
	@Test
	public void sa00_ResponseHeader_onPojo_basic() throws Exception {
		HeaderInfo x = getSwagger(new SA()).getPaths().get("/basic").get("get").getResponse(200).getHeader("H");
		assertEquals("a", x.getDescription());
		assertEquals("a", x.getType());
		assertEquals("a", x.getFormat());
		assertEquals("a", x.getCollectionFormat());
		assertObjectEquals("1", x.getMaximum());
		assertObjectEquals("1", x.getMinimum());
		assertObjectEquals("1", x.getMultipleOf());
		assertObjectEquals("1", x.getMaxLength());
		assertObjectEquals("1", x.getMinLength());
		assertObjectEquals("1", x.getMaxItems());
		assertObjectEquals("1", x.getMinItems());
		assertObjectEquals("true", x.getExclusiveMaximum());
		assertObjectEquals("true", x.getExclusiveMinimum());
		assertObjectEquals("true", x.getUniqueItems());
		assertObjectEquals("{type:'a'}", x.getItems());
		assertObjectEquals("'a'", x.getDefault());
		assertObjectEquals("['a','b']", x.getEnum());
		assertObjectEquals("'a'", x.getExample());
	}
	@Test
	public void sa01a_ResponseHeader_onPojo_code() throws Exception {
		HeaderInfo x = getSwagger(new SA()).getPaths().get("/code").get("get").getResponse(100).getHeader("H");
		assertNotNull(x);
	}
	@Test
	public void sa01b_ResponseHeader_onPojo_codes() throws Exception {
		Operation x = getSwagger(new SA()).getPaths().get("/codes").get("get");
		assertNotNull(x.getResponse(100).getHeader("H"));
		assertNotNull(x.getResponse(101).getHeader("H"));
	}
	@Test
	public void sa01c_ResponseHeader_onPojo_codeAndCodes() throws Exception {
		Operation x = getSwagger(new SA()).getPaths().get("/codeAndCodes").get("get");
		assertNotNull(x.getResponse(100).getHeader("H"));
		assertNotNull(x.getResponse(101).getHeader("H"));
	}
	@Test
	public void sa01d_ResponseHeader_onPojo_nocode() throws Exception {
		HeaderInfo x = getSwagger(new SA()).getPaths().get("/nocode").get("get").getResponse(200).getHeader("H");
		assertEquals("a", x.getDescription());
	}
	@Test
	public void sa02_ResponseHeader_onPojo_value() throws Exception {
		HeaderInfo x = getSwagger(new SA()).getPaths().get("/value").get("get").getResponse(200).getHeader("H");
		assertNotNull(x);
	}
	@Test
	public void sa03_ResponseHeader_onPojo_items1() throws Exception {
		HeaderInfo x = getSwagger(new SA()).getPaths().get("/items1").get("get").getResponse(200).getHeader("H");
		assertObjectEquals("{type:'b'}", x.getItems());
	}
	@Test
	public void sa04_ResponseHeader_onPojo_default1() throws Exception {
		HeaderInfo x = getSwagger(new SA()).getPaths().get("/default1").get("get").getResponse(200).getHeader("H");
		assertObjectEquals("{f1:'b'}", x.getDefault());
	}
	@Test
	public void sa05_ResponseHeader_onPojo_enum1() throws Exception {
		HeaderInfo x = getSwagger(new SA()).getPaths().get("/enum1").get("get").getResponse(200).getHeader("H");
		assertObjectEquals("['a','b']", x.getEnum());
	}
	@Test
	public void sa06_ResponseHeader_onPojo_example1() throws Exception {
		HeaderInfo x = getSwagger(new SA()).getPaths().get("/example1").get("get").getResponse(200).getHeader("H");
		assertObjectEquals("{f1:'b'}", x.getExample());
	}

	//=================================================================================================================
	// @ResponseHeader on parameter
	//=================================================================================================================
	
	@RestResource()
	public static class SB {

		public static class SB00 {}
		
		@RestMethod(name=GET,path="/basic")
		public void sb00(
			@ResponseHeader(
				name="H",
				description="a",
				type="a",
				format="a",
				collectionFormat="a",
				maximum="1",
				minimum="1",
				multipleOf="1",
				maxLength="1",
				minLength="1",
				maxItems="1",
				minItems="1",
				exclusiveMaximum="true",
				exclusiveMinimum="true",
				uniqueItems="true",
				items=@Items(type="a"),
				_default="'a'",
				_enum=" a,b ",
				example="'a'"
			) SB00 h) {}
		
		public static class SB01a {}
		
		@RestMethod(name=GET,path="/code")
		public void sb01a(@ResponseHeader(name="H", code=100) SB01a h) {}

		public static class SB01b {}
		
		@RestMethod(name=GET,path="/codes")
		public void sb01b(@ResponseHeader(name="H", codes={100,101}) SB01b h) {}

		public static class SB01c {}
		
		@RestMethod(name=GET,path="/codeAndCodes")
		public void sb01c(@ResponseHeader(name="H", code=100,codes={101}) SB01c h) {}

		public static class SB01d {}
		
		@RestMethod(name=GET,path="/nocode")
		public void sb01d(@ResponseHeader(name="H", description="a") SB01d h) {}

		public static class SB02 {}
		
		@RestMethod(name=GET,path="/value")
		public void sb02(@ResponseHeader("H") SB02 h) {}
		
		public static class SB03 {}
		
		@RestMethod(name=GET,path="/items1")
		public void sb03(@ResponseHeader(name="H", items=@Items(" type:'b' ")) SB03 h) {}

		public static class SB04 {}
		
		@RestMethod(name=GET,path="/default1")
		public void sb04(@ResponseHeader(name="H", _default={" {f1:'b'} "}) SB04 h) {}

		public static class SB05 {}
		
		@RestMethod(name=GET,path="/enum1")
		public void sb05(@ResponseHeader(name="H", _enum={" ['a','b'] "}) SB05 h) {}

		public static class SB06 {}
		
		@RestMethod(name=GET,path="/example1")
		public void sb06(@ResponseHeader(name="H", example={" {f1:'b'} "}) SB06 h) {}
	}
	
	@Test
	public void sb00_ResponseHeader_onPojo_basic() throws Exception {
		HeaderInfo x = getSwagger(new SB()).getPaths().get("/basic").get("get").getResponse(200).getHeader("H");
		assertEquals("a", x.getDescription());
		assertEquals("a", x.getType());
		assertEquals("a", x.getFormat());
		assertEquals("a", x.getCollectionFormat());
		assertObjectEquals("1", x.getMaximum());
		assertObjectEquals("1", x.getMinimum());
		assertObjectEquals("1", x.getMultipleOf());
		assertObjectEquals("1", x.getMaxLength());
		assertObjectEquals("1", x.getMinLength());
		assertObjectEquals("1", x.getMaxItems());
		assertObjectEquals("1", x.getMinItems());
		assertObjectEquals("true", x.getExclusiveMaximum());
		assertObjectEquals("true", x.getExclusiveMinimum());
		assertObjectEquals("true", x.getUniqueItems());
		assertObjectEquals("{type:'a'}", x.getItems());
		assertObjectEquals("'a'", x.getDefault());
		assertObjectEquals("['a','b']", x.getEnum());
		assertObjectEquals("'a'", x.getExample());
	}
	@Test
	public void sb01a_ResponseHeader_onPojo_code() throws Exception {
		HeaderInfo x = getSwagger(new SB()).getPaths().get("/code").get("get").getResponse(100).getHeader("H");
		assertNotNull(x);
	}
	@Test
	public void sb01b_ResponseHeader_onPojo_codes() throws Exception {
		Operation x = getSwagger(new SB()).getPaths().get("/codes").get("get");
		assertNotNull(x.getResponse(100).getHeader("H"));
		assertNotNull(x.getResponse(101).getHeader("H"));
	}
	@Test
	public void sb01c_ResponseHeader_onPojo_codeAndCodes() throws Exception {
		Operation x = getSwagger(new SB()).getPaths().get("/codeAndCodes").get("get");
		assertNotNull(x.getResponse(100).getHeader("H"));
		assertNotNull(x.getResponse(101).getHeader("H"));
	}
	@Test
	public void sb01d_ResponseHeader_onPojo_nocode() throws Exception {
		HeaderInfo x = getSwagger(new SB()).getPaths().get("/nocode").get("get").getResponse(200).getHeader("H");
		assertEquals("a", x.getDescription());
	}
	@Test
	public void sb02_ResponseHeader_onPojo_value() throws Exception {
		HeaderInfo x = getSwagger(new SB()).getPaths().get("/value").get("get").getResponse(200).getHeader("H");
		assertNotNull(x);
	}
	@Test
	public void sb03_ResponseHeader_onPojo_items1() throws Exception {
		HeaderInfo x = getSwagger(new SB()).getPaths().get("/items1").get("get").getResponse(200).getHeader("H");
		assertObjectEquals("{type:'b'}", x.getItems());
	}
	@Test
	public void sb04_ResponseHeader_onPojo_default1() throws Exception {
		HeaderInfo x = getSwagger(new SB()).getPaths().get("/default1").get("get").getResponse(200).getHeader("H");
		assertObjectEquals("{f1:'b'}", x.getDefault());
	}
	@Test
	public void sb05_ResponseHeader_onPojo_enum1() throws Exception {
		HeaderInfo x = getSwagger(new SB()).getPaths().get("/enum1").get("get").getResponse(200).getHeader("H");
		assertObjectEquals("['a','b']", x.getEnum());
	}
	@Test
	public void sb06_ResponseHeader_onPojo_example1() throws Exception {
		HeaderInfo x = getSwagger(new SB()).getPaths().get("/example1").get("get").getResponse(200).getHeader("H");
		assertObjectEquals("{f1:'b'}", x.getExample());
	}
}
