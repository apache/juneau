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
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests the @Header annotation.
 */
@SuppressWarnings({"javadoc"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HeaderAnnotationTest {

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
	// @Header on POJO
	//=================================================================================================================

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource
	public static class SA {

		@Header(
			name="H",
			description={"a","b"},
			type="string"
		)
		public static class SA01 {
			public SA01(String x) {}
		}
		@RestMethod(name=GET,path="/basic")
		public void sa01(SA01 h) {}

		@Header(
			name="H",
			api={
				"description:'a\nb',",
				"type:'string'"
			}
		)
		public static class SA02 {
			public SA02(String x) {}
		}
		@RestMethod(name=GET,path="/api")
		public void sa02(SA02 h) {}

		@Header(
			name="H",
			api={
				"description:'b\nc',",
				"type:'string'"
			},
			description={"a","b"},
			type="string"
		)
		public static class SA03 {
			public SA03(String x) {}
		}
		@RestMethod(name=GET,path="/mixed")
		public void sa03(SA03 h) {}
	}

	@Test
	public void sa01_Header_onPojo_basic() throws Exception {
		ParameterInfo x = getSwagger(new SA()).getPaths().get("/basic").get("get").getParameter("header", "H");
		assertEquals("a\nb", x.getDescription());
		assertObjectEquals("'string'", x.getType());
	}
	@Test
	public void sa02_Header_onPojo_api() throws Exception {
		ParameterInfo x = getSwagger(new SA()).getPaths().get("/api").get("get").getParameter("header", "H");
		assertEquals("a\nb", x.getDescription());
		assertObjectEquals("'string'", x.getType());
	}
	@Test
	public void sa03_Header_onPojo_mixed() throws Exception {
		ParameterInfo x = getSwagger(new SA()).getPaths().get("/mixed").get("get").getParameter("header", "H");
		assertEquals("a\nb", x.getDescription());
		assertObjectEquals("'string'", x.getType());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Schema
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource
	public static class SB {

		@Header(name="H")
		public static class SB01 {}
		@RestMethod(name=GET,path="/schemaValue")
		public void sb01(SB01 h) {}

		@Header(name="H")
		public static class SB02 {
			public String f1;
		}
		@RestMethod(name=GET,path="/autoDetectBean")
		public void sb02(SB02 b) {}

		@Header(name="H")
		public static class SB03 extends LinkedList<String> {
			private static final long serialVersionUID = 1L;
		}
		@RestMethod(name=GET,path="/autoDetectList")
		public void sb03(SB03 b) {}

		@Header(name="H")
		public static class SB04 {}
		@RestMethod(name=GET,path="/autoDetectStringObject")
		public void sb04(SB04 b) {}
	}
	@Test
	public void sb01_Header_onPojo_schemaValue() throws Exception {
		ParameterInfo x = getSwagger(new SB()).getPaths().get("/schemaValue").get("get").getParameter("header", "H");
		assertObjectEquals("{type:'string'}", x.getSchema());
	}
	@Test
	public void sb02_Header_onPojo_autoDetectBean() throws Exception {
		ParameterInfo x = getSwagger(new SB()).getPaths().get("/autoDetectBean").get("get").getParameter("header", "H");
		assertObjectEquals("{type:'object',properties:{f1:{type:'string'}}}", x.getSchema());
	}
	@Test
	public void sb03_Header_onPojo_autoDetectList() throws Exception {
		ParameterInfo x = getSwagger(new SB()).getPaths().get("/autoDetectList").get("get").getParameter("header", "H");
		assertObjectEquals("{type:'array',items:{type:'string'}}", x.getSchema());
	}
	@Test
	public void sb04_Header_onPojo_autoDetectStringObject() throws Exception {
		ParameterInfo x = getSwagger(new SB()).getPaths().get("/autoDetectStringObject").get("get").getParameter("header", "H");
		assertObjectEquals("{type:'string'}", x.getSchema());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Examples
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource
	public static class SC {

		@Header(name="H", example="{f1:'a'}")
		public static class SC01 {
			public String f1;
		}
		@RestMethod(name=GET,path="/example")
		public void sc01(SC01 h) {}
	}

	@Test
	public void sc01_Header_onPojo_example() throws Exception {
		ParameterInfo x = getSwagger(new SC()).getPaths().get("/example").get("get").getParameter("header", "H");
		assertEquals("{f1:'a'}", x.getExample());
	}

	//=================================================================================================================
	// @Header on parameter
	//=================================================================================================================

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource
	public static class TA {

		@RestMethod(name=GET,path="/basic")
		public void ta01(
			@Header(
				name="H",
				description={"a","b"},
				type="string"
			) String h) {}

		@RestMethod(name=GET,path="/api")
		public void ta02(
			@Header(
				name="H",
				api={
					"description:'a\nb',",
					"type:'string'",
				}
			) String h) {}

		@RestMethod(name=GET,path="/mixed")
		public void ta03(
			@Header(
				name="H",
				api={
					"description:'b\nc',",
					"type:'string'",
				},
				description={"a","b"},
				type="string"
			) String h) {}

		@RestMethod(name=GET,path="/value")
		public void ta04(@Header("H") String h) {}
	}

	@Test
	public void ta01_Header_onParameter_basic() throws Exception {
		ParameterInfo x = getSwagger(new TA()).getPaths().get("/basic").get("get").getParameter("header", "H");
		assertEquals("H", x.getName());
		assertEquals("a\nb", x.getDescription());
		assertEquals("string", x.getType());
	}
	@Test
	public void ta02_Header_onParameter_api() throws Exception {
		ParameterInfo x = getSwagger(new TA()).getPaths().get("/api").get("get").getParameter("header", "H");
		assertEquals("H", x.getName());
		assertEquals("a\nb", x.getDescription());
		assertEquals("string", x.getType());
	}
	@Test
	public void ta03_Header_onParameter_mixed() throws Exception {
		ParameterInfo x = getSwagger(new TA()).getPaths().get("/mixed").get("get").getParameter("header", "H");
		assertEquals("H", x.getName());
		assertEquals("a\nb", x.getDescription());
		assertEquals("string", x.getType());
	}
	@Test
	public void ta04_Header_onParameter_value() throws Exception {
		ParameterInfo x = getSwagger(new TA()).getPaths().get("/value").get("get").getParameter("header", "H");
		assertEquals("H", x.getName());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Schema
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource
	public static class TB {

		@RestMethod(name=GET,path="/schemaValue")
		public void tb01(@Header(name="H") String h) {}

		public static class TB02 {
			public String f1;
		}
		@RestMethod(name=GET,path="/autoDetectBean")
		public void tb02(@Header("H") TB02 b) {}

		public static class TB03 extends LinkedList<String> {
			private static final long serialVersionUID = 1L;
		}
		@RestMethod(name=GET,path="/autoDetectList")
		public void tb03(@Header("H") TB03 b) {}

		public static class TB04 {}
		@RestMethod(name=GET,path="/autoDetectStringObject")
		public void tb04(@Header("H") TB04 b) {}

		@RestMethod(name=GET,path="/autoDetectInteger")
		public void tb05(@Header("H") Integer b) {}

		@RestMethod(name=GET,path="/autoDetectBoolean")
		public void tbo6(@Header("H") Boolean b) {}
	}

	@Test
	public void tb01_Header_onParameter_schema2() throws Exception {
		ParameterInfo x = getSwagger(new TB()).getPaths().get("/schemaValue").get("get").getParameter("header", "H");
		assertObjectEquals("{type:'string'}", x.getSchema());
	}
	@Test
	public void tb02_Header_onParameter_schema3() throws Exception {
		ParameterInfo x = getSwagger(new TB()).getPaths().get("/autoDetectBean").get("get").getParameter("header", "H");
		assertObjectEquals("{type:'object',properties:{f1:{type:'string'}}}", x.getSchema());
	}
	@Test
	public void tb03_Header_onParameter_schema4() throws Exception {
		ParameterInfo x = getSwagger(new TB()).getPaths().get("/autoDetectList").get("get").getParameter("header", "H");
		assertObjectEquals("{type:'array',items:{type:'string'}}", x.getSchema());
	}
	@Test
	public void tb04_Header_onParameter_schema5() throws Exception {
		ParameterInfo x = getSwagger(new TB()).getPaths().get("/autoDetectStringObject").get("get").getParameter("header", "H");
		assertObjectEquals("{type:'string'}", x.getSchema());
	}
	@Test
	public void tb05_Header_onParameter_schema6() throws Exception {
		ParameterInfo x = getSwagger(new TB()).getPaths().get("/autoDetectInteger").get("get").getParameter("header", "H");
		assertObjectEquals("{format:'int32',type:'integer'}", x.getSchema());
	}
	@Test
	public void tb06_Header_onParameter_schema7() throws Exception {
		ParameterInfo x = getSwagger(new TB()).getPaths().get("/autoDetectBoolean").get("get").getParameter("header", "H");
		assertObjectEquals("{type:'boolean'}", x.getSchema());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Examples
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource
	public static class TC {

		@RestMethod(name=GET,path="/example")
		public void tc01(@Header(name="H", example={"a","b"}) String h) {}
	}

	@Test
	public void tc01_Header_onParameter_example() throws Exception {
		ParameterInfo x = getSwagger(new TC()).getPaths().get("/example").get("get").getParameter("header", "H");
		assertEquals("a\nb", x.getExample());
	}
}
