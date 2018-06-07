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
import org.apache.juneau.rest.annotation.Items;
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
	
	@RestResource()
	public static class SA {

		@Header(
			name="H", 
			_default="a",
			allowEmptyValue="true",
			collectionFormat="A",
			description={"a","b"},
			exclusiveMaximum="true",
			exclusiveMinimum="true",
			format="a",
			maximum="1",
			maxItems="1",
			maxLength="1",
			minimum="1",
			minItems="1",
			minLength="1",
			multipleOf="1",
			pattern="a",
			required="true",
			type="a",
			uniqueItems="true",
			_enum="A,B,C",
			example="a",
			items=@Items(type="a"),
			schema=@Schema(type="a")
		)
		public static class SA00 {
			public SA00(String x) {}
		}

		@RestMethod(name=GET,path="/basic")
		public void sa00(SA00 h) {}
		
		@Header(name="H", _enum="['A','B','C']")
		public static class SA03 {}
		
		@RestMethod(name=GET,path="/_enum2")
		public void sa03(SA03 h) {}
		
		@Header(name="H", example="{f1:'a'}")
		public static class SA08b {
			public String f1;
		}
		
		@RestMethod(name=GET,path="/example2")
		public void sa08b(SA08b h) {}
		
		@Header(name="H", items=@Items(" type:'a' "))
		public static class SA13 {}
		
		@RestMethod(name=GET,path="/items2")
		public void sa13(SA13 h) {}
		
		@Header(name="H", schema=@Schema(" type:'a' "))
		public static class SA23b {}
		
		@RestMethod(name=GET,path="/schema2")
		public void sa23(SA23b h) {}

		@Header(name="H")
		public static class SA23c {
			public String f1;
		}

		@RestMethod(name=GET,path="/schema3")
		public void sa23c(SA23c b) {}

		@Header(name="H")
		public static class SA23d extends LinkedList<String> {
			private static final long serialVersionUID = 1L;
		}

		@RestMethod(name=GET,path="/schema4")
		public void sa23d(SA23d b) {}

		@Header(name="H")
		public static class SA23e {}

		@RestMethod(name=GET,path="/schema5")
		public void sa23e(SA23e b) {}
	}
	
	@Test
	public void sa00_Header_onPojo_basic() throws Exception {
		ParameterInfo x = getSwagger(new SA()).getPaths().get("/basic").get("get").getParameter("header", "H");
		assertObjectEquals("'a'", x.getDefault());
		assertEquals(true, x.getAllowEmptyValue());
		assertEquals("A", x.getCollectionFormat());
		assertEquals("a\nb", x.getDescription());
		assertEquals(true, x.getExclusiveMaximum());
		assertEquals(true, x.getExclusiveMinimum());
		assertEquals("a", x.getFormat());
		assertObjectEquals("1", x.getMaximum());
		assertObjectEquals("1", x.getMaxItems());
		assertObjectEquals("1", x.getMaxLength());
		assertObjectEquals("1", x.getMinimum());
		assertObjectEquals("1", x.getMinItems());
		assertObjectEquals("1", x.getMinLength());
		assertObjectEquals("1", x.getMultipleOf());
		assertObjectEquals("'a'", x.getPattern());
		assertObjectEquals("true", x.getRequired());
		assertObjectEquals("'a'", x.getType());
		assertObjectEquals("true", x.getUniqueItems());
		assertObjectEquals("['A','B','C']", x.getEnum());
		assertEquals("a", x.getExample());
		assertObjectEquals("{type:'a'}", x.getItems());
		assertObjectEquals("{type:'a'}", x.getSchema());
	}
	@Test
	public void sa03_Header_onPojo_enum2() throws Exception {
		ParameterInfo x = getSwagger(new SA()).getPaths().get("/_enum2").get("get").getParameter("header", "H");
		assertObjectEquals("['A','B','C']", x.getEnum());
	}
	@Test
	public void sa08b_Header_onPojo_example2() throws Exception {
		ParameterInfo x = getSwagger(new SA()).getPaths().get("/example2").get("get").getParameter("header", "H");
		assertObjectEquals("{f1:'a'}", x.getExample());
	}
	@Test
	public void sa13_Header_onPojo_items2() throws Exception {
		ParameterInfo x = getSwagger(new SA()).getPaths().get("/items2").get("get").getParameter("header", "H");
		assertObjectEquals("{type:'a'}", x.getItems());
	}
	@Test
	public void sa23b_Header_onPojo_schema2() throws Exception {
		ParameterInfo x = getSwagger(new SA()).getPaths().get("/schema2").get("get").getParameter("header", "H");
		assertObjectEquals("{type:'a'}", x.getSchema());
	}
	@Test
	public void sa23c_Header_onPojo_schema3() throws Exception {
		ParameterInfo x = getSwagger(new SA()).getPaths().get("/schema3").get("get").getParameter("header", "H");
		assertObjectEquals("{type:'object',properties:{f1:{type:'string'}}}", x.getSchema());
	}
	@Test
	public void sa23d_Header_onPojo_schema4() throws Exception {
		ParameterInfo x = getSwagger(new SA()).getPaths().get("/schema4").get("get").getParameter("header", "H");
		assertObjectEquals("{type:'array',items:{type:'string'}}", x.getSchema());
	}
	@Test
	public void sa23e_Header_onPojo_schema5() throws Exception {
		ParameterInfo x = getSwagger(new SA()).getPaths().get("/schema5").get("get").getParameter("header", "H");
		assertObjectEquals("{type:'string'}", x.getSchema());
	}

	//=================================================================================================================
	// @Header on sarameter
	//=================================================================================================================

	@RestResource()
	public static class SB {

		@RestMethod(name=GET,path="/basic")
		public void sb00(
			@Header(
				name="H",
				description={"a","b"},
				type="a",
				format="a",
				pattern="a",
				collectionFormat="a",
				maximum="1",
				minimum="1",
				multipleOf="1",
				maxLength="1",
				minLength="1",
				maxItems="1",
				minItems="1",
				allowEmptyValue="true",
				exclusiveMaximum="true",
				exclusiveMinimum="true",
				uniqueItems="true",
				schema=@Schema(type="a"),
				_default="a",
				_enum="a,b",
				items=@Items(type="a"),
				example="a,b"
			) String h) {}
		
		@RestMethod(name=GET,path="/value")
		public void sb02(@Header("H") String h) {}
		
		@RestMethod(name=GET,path="/schema2")
		public void sb19b(@Header(name="H", schema=@Schema(" type:'b' ")) String h) {}

		public static class SB19c {
			public String f1;
		}

		@RestMethod(name=GET,path="/schema3")
		public void sb19c(@Header("H") SB19c b) {}

		public static class SB19d extends LinkedList<String> {
			private static final long serialVersionUID = 1L;
		}

		@RestMethod(name=GET,path="/schema4")
		public void sb19d(@Header("H") SB19d b) {}

		public static class SB19e {}

		@RestMethod(name=GET,path="/schema5")
		public void sb19e(@Header("H") SB19e b) {}

		@RestMethod(name=GET,path="/schema6")
		public void sb19f(@Header("H") Integer b) {}

		@RestMethod(name=GET,path="/schema7")
		public void sb19g(@Header("H") Boolean b) {}

		@RestMethod(name=GET,path="/_default2")
		public void sb20b(@Header(name="H", _default={"a","b"}) String h) {}

		@RestMethod(name=GET,path="/_enum2")
		public void sb21b(@Header(name="H", _enum={"['a','b']"}) String h) {}

		@RestMethod(name=GET,path="/items2")
		public void sb22b(@Header(name="H", items=@Items(" type:'b' ")) String h) {}

		@RestMethod(name=GET,path="/example2")
		public void sb23b(@Header(name="H", example={"a","b"}) String h) {}
	}

	@Test
	public void sb00_Header_onParameter_basic() throws Exception {
		ParameterInfo x = getSwagger(new SB()).getPaths().get("/basic").get("get").getParameter("header", "H");
		assertEquals("H", x.getName());
		assertEquals("a\nb", x.getDescription());
		assertEquals("a", x.getType());
		assertEquals("a", x.getFormat());
		assertEquals("a", x.getPattern());
		assertEquals("a", x.getCollectionFormat());
		assertObjectEquals("1", x.getMaximum());
		assertObjectEquals("1", x.getMinimum());
		assertObjectEquals("1", x.getMultipleOf());
		assertObjectEquals("1", x.getMaxLength());
		assertObjectEquals("1", x.getMinLength());
		assertObjectEquals("1", x.getMaxItems());
		assertObjectEquals("1", x.getMinItems());
		assertObjectEquals("true", x.getAllowEmptyValue());
		assertObjectEquals("true", x.getExclusiveMaximum());
		assertObjectEquals("true", x.getExclusiveMinimum());
		assertObjectEquals("true", x.getUniqueItems());
		assertObjectEquals("{type:'a'}", x.getSchema());
		assertObjectEquals("'a'", x.getDefault());
		assertObjectEquals("['a','b']", x.getEnum());
		assertObjectEquals("{type:'a'}", x.getItems());
		assertEquals("a,b", x.getExample());
	}
	@Test
	public void sb02_Header_onParameter_value() throws Exception {
		ParameterInfo x = getSwagger(new SB()).getPaths().get("/value").get("get").getParameter("header", "H");
		assertEquals("H", x.getName());
	}
	@Test
	public void sb19b_Header_onParameter_schema2() throws Exception {
		ParameterInfo x = getSwagger(new SB()).getPaths().get("/schema2").get("get").getParameter("header", "H");
		assertObjectEquals("{type:'b'}", x.getSchema());
	}
	@Test
	public void sb19c_Header_onParameter_schema3() throws Exception {
		ParameterInfo x = getSwagger(new SB()).getPaths().get("/schema3").get("get").getParameter("header", "H");
		assertObjectEquals("{type:'object',properties:{f1:{type:'string'}}}", x.getSchema());
	}
	@Test
	public void sb19d_Header_onParameter_schema4() throws Exception {
		ParameterInfo x = getSwagger(new SB()).getPaths().get("/schema4").get("get").getParameter("header", "H");
		assertObjectEquals("{type:'array',items:{type:'string'}}", x.getSchema());
	}
	@Test
	public void sb19e_Header_onParameter_schema5() throws Exception {
		ParameterInfo x = getSwagger(new SB()).getPaths().get("/schema5").get("get").getParameter("header", "H");
		assertObjectEquals("{type:'string'}", x.getSchema());
	}
	@Test
	public void sb19f_Header_onParameter_schema6() throws Exception {
		ParameterInfo x = getSwagger(new SB()).getPaths().get("/schema6").get("get").getParameter("header", "H");
		assertObjectEquals("{format:'int32',type:'integer'}", x.getSchema());
	}
	@Test
	public void sb19g_Header_onParameter_schema7() throws Exception {
		ParameterInfo x = getSwagger(new SB()).getPaths().get("/schema7").get("get").getParameter("header", "H");
		assertObjectEquals("{type:'boolean'}", x.getSchema());
	}
	@Test
	public void sb20b_Header_onParameter__default2() throws Exception {
		ParameterInfo x = getSwagger(new SB()).getPaths().get("/_default2").get("get").getParameter("header", "H");
		assertObjectEquals("'a\\nb'", x.getDefault());
	}
	@Test
	public void sb21b_Header_onParameter__enum2() throws Exception {
		ParameterInfo x = getSwagger(new SB()).getPaths().get("/_enum2").get("get").getParameter("header", "H");
		assertObjectEquals("['a','b']", x.getEnum());
	}
	@Test
	public void sb22b_Header_onParameter_items2() throws Exception {
		ParameterInfo x = getSwagger(new SB()).getPaths().get("/items2").get("get").getParameter("header", "H");
		assertObjectEquals("{type:'b'}", x.getItems());
	}
	@Test
	public void sb23b_Header_onParameter_example2() throws Exception {
		ParameterInfo x = getSwagger(new SB()).getPaths().get("/example2").get("get").getParameter("header", "H");
		assertEquals("a\nb", x.getExample());
	}
}
