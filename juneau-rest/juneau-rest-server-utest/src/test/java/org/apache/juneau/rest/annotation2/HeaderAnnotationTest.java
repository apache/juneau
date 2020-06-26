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

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.http.HttpMethodName.*;
import static org.apache.juneau.rest.testutils.TestUtils.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.json.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock2.*;
import org.apache.juneau.rest.testutils.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class HeaderAnnotationTest {

	//=================================================================================================================
	// Optional header parameter.
	//=================================================================================================================

	@Rest(serializers=SimpleJsonSerializer.class)
	public static class A {
		@RestMethod(name=GET,path="/a")
		public Object a(@Header("f1") Optional<Integer> f1) throws Exception {
			assertNotNull(f1);
			return f1;
		}
		@RestMethod(name=GET,path="/b")
		public Object b(@Header("f1") Optional<ABean> f1) throws Exception {
			assertNotNull(f1);
			return f1;
		}
		@RestMethod(name=GET,path="/c")
		public Object c(@Header("f1") Optional<List<ABean>> f1) throws Exception {
			assertNotNull(f1);
			return f1;
		}
		@RestMethod(name=GET,path="/d")
		public Object d(@Header("f1") List<Optional<ABean>> f1) throws Exception {
			return f1;
		}
	}
	static MockRestClient a = MockRestClient.buildJson(A.class);

	@Test
	public void a01_optionalParam_integer() throws Exception {
		a.get("/a").header("f1", 123)
			.run()
			.assertStatus().is(200)
			.assertBody().is("123");
		a.get("/a")
			.run()
			.assertStatus().is(200)
			.assertBody().is("null");
	}

	@Test
	public void a02_optionalParam_bean() throws Exception {
		a.get("/b")
			.header("f1", "a=1,b=foo")
			.run()
			.assertStatus().is(200)
			.assertBody().is("{a:1,b:'foo'}");
		a.get("/b")
			.run()
			.assertStatus().is(200)
			.assertBody().is("null");
	}

	@Test
	public void a03_optionalParam_listOfBeans() throws Exception {
		a.get("/c")
			.header("f1", "@((a=1,b=foo))")
			.run()
			.assertStatus().is(200)
			.assertBody().is("[{a:1,b:'foo'}]");
		a.get("/c")
			.run()
			.assertStatus().is(200)
			.assertBody().is("null");
	}

	@Test
	public void a04_optionalParam_listOfOptionals() throws Exception {
		a.get("/d")
			.header("f1", "@((a=1,b=foo))")
			.run()
			.assertStatus().is(200)
			.assertBody().is("[{a:1,b:'foo'}]");
		a.get("/d")
			.run()
			.assertStatus().is(200)
			.assertBody().is("null");
	}


	//=================================================================================================================
	// @Header on POJO
	//=================================================================================================================

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class SA {

		@Header(
			name="H",
			description={"a","b"},
			type="string"
		)
		public static class SA01 {
			public SA01(String x) {}
		}
		@RestMethod
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
		@RestMethod
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
		@RestMethod
		public void sa03(SA03 h) {}
	}

	static Swagger sa = getSwagger(SA.class);

	@Test
	public void sa01_Header_onPojo_basic() throws Exception {
		ParameterInfo x = sa.getParameterInfo("/sa01","get","header","H");
		assertEquals("a\nb", x.getDescription());
		assertObject(x.getType()).json().is("'string'");
	}
	@Test
	public void sa02_Header_onPojo_api() throws Exception {
		ParameterInfo x = sa.getParameterInfo("/sa02","get","header","H");
		assertEquals("a\nb", x.getDescription());
		assertObject(x.getType()).json().is("'string'");
	}
	@Test
	public void sa03_Header_onPojo_mixed() throws Exception {
		ParameterInfo x = sa.getParameterInfo("/sa03","get","header","H");
		assertEquals("a\nb", x.getDescription());
		assertObject(x.getType()).json().is("'string'");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Schema
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class SB {

		@Header(name="H")
		public static class SB01 {}
		@RestMethod
		public void sb01(SB01 h) {}

		@Header(name="H")
		public static class SB02 {
			public String f1;
		}
		@RestMethod
		public void sb02(SB02 b) {}

		@Header(name="H")
		public static class SB03 extends LinkedList<String> {
			private static final long serialVersionUID = 1L;
		}
		@RestMethod
		public void sb03(SB03 b) {}

		@Header(name="H")
		public static class SB04 {}
		@RestMethod
		public void sb04(SB04 b) {}
	}

	static Swagger sb = getSwagger(SB.class);

	@Test
	public void sb01_Header_onPojo_schemaValue() throws Exception {
		ParameterInfo x = sb.getParameterInfo("/sb01","get","header","H");
		assertObject(x).json().is("{'in':'header',name:'H',type:'string'}");
	}
	@Test
	public void sb02_Header_onPojo_autoDetectBean() throws Exception {
		ParameterInfo x = sb.getParameterInfo("/sb02","get","header","H");
		assertObject(x).json().is("{'in':'header',name:'H',type:'object',schema:{properties:{f1:{type:'string'}}}}");
	}
	@Test
	public void sb03_Header_onPojo_autoDetectList() throws Exception {
		ParameterInfo x = sb.getParameterInfo("/sb03","get","header","H");
		assertObject(x).json().is("{'in':'header',name:'H',type:'array',items:{type:'string'}}");
	}
	@Test
	public void sb04_Header_onPojo_autoDetectStringObject() throws Exception {
		ParameterInfo x = sb.getParameterInfo("/sb04","get","header","H");
		assertObject(x).json().is("{'in':'header',name:'H',type:'string'}");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Examples
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class SC {

		@Header(name="H", example="{f1:'a'}")
		public static class SC01 {
			public String f1;
		}
		@RestMethod
		public void sc01(SC01 h) {}
	}

	static Swagger sc = getSwagger(SC.class);

	@Test
	public void sc01_Header_onPojo_example() throws Exception {
		ParameterInfo x = sc.getParameterInfo("/sc01","get","header","H");
		assertEquals("{f1:'a'}", x.getExample());
	}

	//=================================================================================================================
	// @Header on parameter
	//=================================================================================================================

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class TA {

		@RestMethod
		public void ta01(
			@Header(
				name="H",
				description={"a","b"},
				type="string"
			) String h) {}

		@RestMethod
		public void ta02(
			@Header(
				name="H",
				api={
					"description:'a\nb',",
					"type:'string'",
				}
			) String h) {}

		@RestMethod
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

		@RestMethod
		public void ta04(@Header("H") String h) {}
	}

	static Swagger ta = getSwagger(TA.class);

	@Test
	public void ta01_Header_onParameter_basic() throws Exception {
		ParameterInfo x = ta.getParameterInfo("/ta01","get","header","H");
		assertEquals("H", x.getName());
		assertEquals("a\nb", x.getDescription());
		assertEquals("string", x.getType());
	}
	@Test
	public void ta02_Header_onParameter_api() throws Exception {
		ParameterInfo x = ta.getParameterInfo("/ta02","get","header","H");
		assertEquals("H", x.getName());
		assertEquals("a\nb", x.getDescription());
		assertEquals("string", x.getType());
	}
	@Test
	public void ta03_Header_onParameter_mixed() throws Exception {
		ParameterInfo x = ta.getParameterInfo("/ta03","get","header","H");
		assertEquals("H", x.getName());
		assertEquals("a\nb", x.getDescription());
		assertEquals("string", x.getType());
	}
	@Test
	public void ta04_Header_onParameter_value() throws Exception {
		ParameterInfo x = ta.getParameterInfo("/ta04","get","header","H");
		assertEquals("H", x.getName());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Schema
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class TB {

		@RestMethod
		public void tb01(@Header(name="H") String h) {}

		public static class TB02 {
			public String f1;
		}
		@RestMethod
		public void tb02(@Header("H") TB02 b) {}

		public static class TB03 extends LinkedList<String> {
			private static final long serialVersionUID = 1L;
		}
		@RestMethod
		public void tb03(@Header("H") TB03 b) {}

		public static class TB04 {}
		@RestMethod
		public void tb04(@Header("H") TB04 b) {}

		@RestMethod
		public void tb05(@Header("H") Integer b) {}

		@RestMethod
		public void tb06(@Header("H") Boolean b) {}
	}

	static Swagger tb = getSwagger(TB.class);

	@Test
	public void tb01_Header_onParameter_string() throws Exception {
		ParameterInfo x = tb.getParameterInfo("/tb01","get","header","H");
		assertObject(x).json().is("{'in':'header',name:'H',type:'string'}");
	}
	@Test
	public void tb02_Header_onParameter_bean() throws Exception {
		ParameterInfo x = tb.getParameterInfo("/tb02","get","header","H");
		assertObject(x).json().is("{'in':'header',name:'H',type:'object',schema:{properties:{f1:{type:'string'}}}}");
	}
	@Test
	public void tb03_Header_onParameter_array() throws Exception {
		ParameterInfo x = tb.getParameterInfo("/tb03","get","header","H");
		assertObject(x).json().is("{'in':'header',name:'H',type:'array',items:{type:'string'}}");
	}
	@Test
	public void tb04_Header_onParameter_beanAsString() throws Exception {
		ParameterInfo x = tb.getParameterInfo("/tb04","get","header","H");
		assertObject(x).json().is("{'in':'header',name:'H',type:'string'}");
	}
	@Test
	public void tb05_Header_onParameter_Integer() throws Exception {
		ParameterInfo x = tb.getParameterInfo("/tb05","get","header","H");
		assertObject(x).json().is("{'in':'header',name:'H',type:'integer',format:'int32'}");
	}
	@Test
	public void tb06_Header_onParameter_Boolean() throws Exception {
		ParameterInfo x = tb.getParameterInfo("/tb06","get","header","H");
		assertObject(x).json().is("{'in':'header',name:'H',type:'boolean'}");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Examples
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class TC {

		@RestMethod
		public void tc01(@Header(name="H", example={"a","b"}) String h) {}
	}

	static Swagger tc = getSwagger(TC.class);

	@Test
	public void tc01_Header_onParameter_example() throws Exception {
		ParameterInfo x = tc.getParameterInfo("/tc01","get","header","H");
		assertEquals("a\nb", x.getExample());
	}
}
