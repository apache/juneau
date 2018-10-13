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

import static org.apache.juneau.http.HttpMethodName.*;
import static org.apache.juneau.rest.testutils.TestUtils.*;
import static org.junit.Assert.assertEquals;

import java.util.*;

import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.http.annotation.Path;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests related to @Path annotation.
 */
@SuppressWarnings({"javadoc"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PathAnnotationTest {

	//=================================================================================================================
	// Basic tests
	//=================================================================================================================

	@RestResource
	public static class A  {
		@RestMethod(name=GET, path="/")
		public void noPath(RestResponse res) {
			res.setOutput(GET);
		}
		@RestMethod(name=GET, path="/a")
		public String simplePath() {
			return "GET /a";
		}
		@RestMethod(name=GET, path="/a/{foo}")
		public String simplePathOneVar(RestResponse res, @Path("foo") String foo) {
			return "GET /a " + foo;
		}
		@RestMethod(name=GET, path="/a/{foo}/{bar}")
		public String simplePathTwoVars(RestResponse res, @Path("foo") String foo, @Path("bar") String bar) {
			return "GET /a " + foo + "," + bar;
		}
		@RestMethod(name=GET, path="/a/{foo}/{bar}/*")
		public String simplePathWithRemainder(@Path("foo") String foo, @Path("bar") int bar, @Path("/*") String remainder) {
			return "GET /a "+foo+","+bar+",r="+remainder;
		}
	}
	static MockRest a = MockRest.create(A.class);

	@Test
	public void a00_nonExistentPath() throws Exception {
		a.get("/bad?noTrace=true").execute().assertStatus(404);
	}
	@Test
	public void a01_noPath() throws Exception {
		a.get(null).execute().assertBody("GET");
		a.get("").execute().assertBody("GET");
	}
	@Test
	public void a02_simplePath() throws Exception {
		a.get("/a").execute().assertBody("GET /a");
	}
	@Test
	public void a03_simplePathOneVar() throws Exception {
		a.get("/a/foo").execute().assertBody("GET /a foo");
	}
	@Test
	public void a04_simplePathTwoVars() throws Exception {
		a.get("/a/foo/bar").execute().assertBody("GET /a foo,bar");
	}
	@Test
	public void a05_simplePathWithRemainder() throws Exception {
		a.get("/a/foo/123/baz").execute().assertBody("GET /a foo,123,r=baz");
	}
	@Test
	public void a06_urlEncodedPathPart() throws Exception {
		// URL-encoded part should not get decoded before finding method to invoke.
		// This should match /get1/{foo} and not /get1/{foo}/{bar}
		// NOTE:  When testing on Tomcat, must specify the following system property:
		// -Dorg.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true
		a.get("/a/x%2Fy").execute().assertBody("GET /a x/y");
		a.get("/a/x%2Fy/x%2Fy").execute().assertBody("GET /a x/y,x/y");
	}

	//=================================================================================================================
	// Primitives
	//=================================================================================================================

	@RestResource
	public static class B  {
		@RestMethod(name=GET, path="/int/{x}/foo")
		public String b01(@Path(name="x") int x) {
			return String.valueOf(x);
		}
		@RestMethod(name=GET, path="/short/{x}/foo")
		public String b02(@Path(name="x") short x) {
			return String.valueOf(x);
		}
		@RestMethod(name=GET, path="/long/{x}/foo")
		public String b03(@Path(name="x") long x) {
			return String.valueOf(x);
		}
		@RestMethod(name=GET, path="/char/{x}/foo")
		public String b04(@Path(name="x") char x) {
			return String.valueOf(x);
		}
		@RestMethod(name=GET, path="/float/{x}/foo")
		public String b05(@Path(name="x") float x) {
			return String.valueOf(x);
		}
		@RestMethod(name=GET, path="/double/{x}/foo")
		public String b06(@Path(name="x") double x) {
			return String.valueOf(x);
		}
		@RestMethod(name=GET, path="/byte/{x}/foo")
		public String b07(@Path(name="x") byte x) {
			return String.valueOf(x);
		}
		@RestMethod(name=GET, path="/boolean/{x}/foo")
		public String b08(@Path(name="x") boolean x) {
			return String.valueOf(x);
		}
	}
	static MockRest b = MockRest.create(B.class);

	@Test
	public void b01_int() throws Exception {
		b.get("/int/123/foo").execute().assertBody("123");
		b.get("/int/bad/foo?noTrace=true").execute().assertStatus(400);
	}
	@Test
	public void b02_short() throws Exception {
		b.get("/short/123/foo").execute().assertBody("123");
		b.get("/short/bad/foo?noTrace=true").execute().assertStatus(400);
	}
	@Test
	public void b03_long() throws Exception {
		b.get("/long/123/foo").execute().assertBody("123");
		b.get("/long/bad/foo?noTrace=true").execute().assertStatus(400);
	}
	@Test
	public void b04_char() throws Exception {
		b.get("/char/c/foo").execute().assertBody("c");
		b.get("/char/bad/foo?noTrace=true").execute().assertStatus(400);
	}
	@Test
	public void b05_float() throws Exception {
		b.get("/float/1.23/foo").execute().assertBody("1.23");
		b.get("/float/bad/foo?noTrace=true").execute().assertStatus(400);
	}
	@Test
	public void b06_double() throws Exception {
		b.get("/double/1.23/foo").execute().assertBody("1.23");
		b.get("/double/bad/foo?noTrace=true").execute().assertStatus(400);
	}
	@Test
	public void b07_byte() throws Exception {
		b.get("/byte/123/foo").execute().assertBody("123");
		b.get("/byte/bad/foo?noTrace=true").execute().assertStatus(400);
	}
	@Test
	public void b08_boolean() throws Exception {
		b.get("/boolean/true/foo").execute().assertBody("true");
		b.get("/boolean/bad/foo?noTrace=true").execute().assertStatus(400);
	}

	//=================================================================================================================
	// Primitive objects
	//=================================================================================================================

	@RestResource
	public static class C  {
		@RestMethod(name=GET, path="/Integer/{x}/foo")
		public String c01(@Path(name="x") Integer x) {
			return String.valueOf(x);
		}
		@RestMethod(name=GET, path="/Short/{x}/foo")
		public String c02(@Path(name="x") Short x) {
			return String.valueOf(x);
		}
		@RestMethod(name=GET, path="/Long/{x}/foo")
		public String c03(@Path(name="x") Long x) {
			return String.valueOf(x);
		}
		@RestMethod(name=GET, path="/Character/{x}/foo")
		public String c04(@Path(name="x") Character x) {
			return String.valueOf(x);
		}
		@RestMethod(name=GET, path="/Float/{x}/foo")
		public String c05(@Path(name="x") Float x) {
			return String.valueOf(x);
		}
		@RestMethod(name=GET, path="/Double/{x}/foo")
		public String c06(@Path(name="x") Double x) {
			return String.valueOf(x);
		}
		@RestMethod(name=GET, path="/Byte/{x}/foo")
		public String c07(@Path(name="x") Byte x) {
			return String.valueOf(x);
		}
		@RestMethod(name=GET, path="/Boolean/{x}/foo")
		public String c08(@Path(name="x") Boolean x) {
			return String.valueOf(x);
		}
	}
	static MockRest c = MockRest.create(C.class);

	@Test
	public void c01_Integer() throws Exception {
		c.get("/Integer/123/foo").execute().assertBody("123");
		c.get("/Integer/bad/foo?noTrace=true").execute().assertStatus(400);
	}
	@Test
	public void c02_Short() throws Exception {
		c.get("/Short/123/foo").execute().assertBody("123");
		c.get("/Short/bad/foo?noTrace=true").execute().assertStatus(400);
	}
	@Test
	public void c03_Long() throws Exception {
		c.get("/Long/123/foo").execute().assertBody("123");
		c.get("/Long/bad/foo?noTrace=true").execute().assertStatus(400);
	}
	@Test
	public void c04_Char() throws Exception {
		c.get("/Character/c/foo").execute().assertBody("c");
		c.get("/Character/bad/foo?noTrace=true").execute().assertStatus(400);
	}
	@Test
	public void c05_Float() throws Exception {
		c.get("/Float/1.23/foo").execute().assertBody("1.23");
		c.get("/Float/bad/foo?noTrace=true").execute().assertStatus(400);
	}
	@Test
	public void c06_Double() throws Exception {
		c.get("/Double/1.23/foo").execute().assertBody("1.23");
		c.get("/Double/bad/foo?noTrace=true").execute().assertStatus(400);
	}
	@Test
	public void c07_Byte() throws Exception {
		c.get("/Byte/123/foo").execute().assertBody("123");
		c.get("/Byte/bad/foo?noTrace=true").execute().assertStatus(400);
	}
	@Test
	public void c08_Boolean() throws Exception {
		c.get("/Boolean/true/foo").execute().assertBody("true");
		c.get("/Boolean/bad/foo?noTrace=true").execute().assertStatus(400);
	}

	//=================================================================================================================
	// POJOs convertible from strings
	//=================================================================================================================

	@RestResource
	public static class D {
		// Object with forString(String) method
		@RestMethod(name=GET, path="/uuid/{uuid}")
		public UUID uuid(RestResponse res, @Path(name="uuid") UUID uuid) {
			return uuid;
		}
	}
	static MockRest d = MockRest.create(D.class);

	@Test
	public void d01_uuid() throws Exception {
		UUID uuid = UUID.randomUUID();
		d.get("/uuid/" + uuid).execute().assertBody(uuid.toString());
	}

	//=================================================================================================================
	// @Path on POJO
	//=================================================================================================================

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource
	public static class SA {

		@Path(
			name="P",
			description={"a","b"},
			type="string",
			_enum="a,b",
			example="a"
		)
		public static class SA01 {
			public SA01(String x) {}
			@Override
			public String toString() {
				return "sa01";
			}
		}
		@RestMethod(name=GET,path="/basic/{P}")
		public void sa01(SA01 f) {}

		@Path(
			name="P",
			api={
				"description:'a\nb',",
				"type:'string',",
				"enum:['a','b'],",
				"x-example:'a'"
			}
		)
		public static class SA02 {
			public SA02(String x) {}
			@Override
			public String toString() {
				return "sa02";
			}
		}
		@RestMethod(name=GET,path="/api/{P}")
		public void sa02(SA02 f) {}

		@Path(
			name="P",
			api={
				"description:'b\nc',",
				"type:'string',",
				"enum:['b','c'],",
				"x-example:'b'"
			},
			description={"a","b"},
			type="string",
			_enum="a,b",
			example="a"
		)
		public static class SA03 {
			public SA03(String x) {}
			@Override
			public String toString() {
				return "sa03";
			}
		}
		@RestMethod(name=GET,path="/mixed/{P}")
		public void sa03(SA03 f) {}


		@Path("P")
		public static class SA04 {
			@Override
			public String toString() {
				return "sa04";
			}
		}
		@RestMethod(name=GET,path="/value/{P}")
		public void sa04(SA04 f) {}

		@Path(name="P", _enum={" ['a','b'] "})
		public static class SA19 {
			@Override
			public String toString() {
				return "sa19";
			}
		}
		@RestMethod(name=GET,path="/enum/{P}")
		public void sa19(SA19 f) {}
	}

	static Swagger sa = getSwagger(SA.class);

	@Test
	public void sa01_Path_onPojo_basic() throws Exception {
		ParameterInfo x = sa.getParameterInfo("/basic/{P}","get","path","P");
		assertEquals("P", x.getName());
		assertEquals("a\nb", x.getDescription());
		assertEquals("string", x.getType());
		assertObjectEquals("['a','b']", x.getEnum());
		assertEquals("a", x.getExample());
		assertObjectEquals("{'in':'path',name:'P',type:'string',description:'a\\nb',required:true,'enum':['a','b'],'x-example':'a','x-examples':{example:'/basic/a'}}", x);
	}
	@Test
	public void sa02_Path_onPojo_api() throws Exception {
		ParameterInfo x = sa.getParameterInfo("/api/{P}","get","path","P");
		assertEquals("P", x.getName());
		assertEquals("a\nb", x.getDescription());
		assertEquals("string", x.getType());
		assertObjectEquals("['a','b']", x.getEnum());
		assertEquals("a", x.getExample());
		assertObjectEquals("{'in':'path',name:'P',type:'string',description:'a\\nb',required:true,'enum':['a','b'],'x-example':'a','x-examples':{example:'/api/a'}}", x);
	}
	@Test
	public void sa03_Path_onPojo_mixed() throws Exception {
		ParameterInfo x = sa.getParameterInfo("/mixed/{P}","get","path","P");
		assertEquals("P", x.getName());
		assertEquals("a\nb", x.getDescription());
		assertEquals("string", x.getType());
		assertObjectEquals("['a','b']", x.getEnum());
		assertEquals("a", x.getExample());
		assertObjectEquals("{'in':'path',name:'P',type:'string',description:'a\\nb',required:true,'enum':['a','b'],'x-example':'a','x-examples':{example:'/mixed/a'}}", x);
	}
	@Test
	public void sa04_Path_onPojo_value() throws Exception {
		ParameterInfo x = sa.getParameterInfo("/value/{P}","get","path","P");
		assertEquals("P", x.getName());
		assertObjectEquals("{'in':'path',name:'P',type:'string',required:true}", x);
	}
	@Test
	public void sa05_Path_onPojo_enum() throws Exception {
		ParameterInfo x = sa.getParameterInfo("/enum/{P}","get","path","P");
		assertObjectEquals("['a','b']", x.getEnum());
		assertObjectEquals("{'in':'path',name:'P',type:'string',required:true,'enum':['a','b']}", x);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Schema
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource
	public static class SB {

		@Path(name="P")
		public static class SB01 {}
		@RestMethod(name=GET,path="/schemaValue/{P}")
		public void sb01(SB01 f) {}

		@Path("P")
		public static class SB02 {
			public String f1;
		}
		@RestMethod(name=GET,path="/autoDetectBean/{P}")
		public void sb02(SB02 b) {}

		@Path("P")
		public static class SB03 extends LinkedList<String> {
			private static final long serialVersionUID = 1L;
		}
		@RestMethod(name=GET,path="/autoDetectList/{P}")
		public void sb03(SB03 b) {}

		@Path("P")
		public static class SB04 {}
		@RestMethod(name=GET,path="/autoDetectStringObject/{P}")
		public void sb04(SB04 b) {}
	}

	static Swagger sb = getSwagger(SB.class);

	@Test
	public void sb01_Path_onPojo_schemaValue() throws Exception {
		ParameterInfo x = sb.getParameterInfo("/schemaValue/{P}","get","path","P");
		assertObjectEquals("{'in':'path',name:'P',type:'string',required:true}", x);
	}
	@Test
	public void sb02_Path_onPojo_autoDetectBean() throws Exception {
		ParameterInfo x = sb.getParameterInfo("/autoDetectBean/{P}","get","path","P");
		assertObjectEquals("{'in':'path',name:'P',type:'object',required:true,schema:{properties:{f1:{type:'string'}}}}", x);
	}
	@Test
	public void sb03_Path_onPojo_autoDetectList() throws Exception {
		ParameterInfo x = sb.getParameterInfo("/autoDetectList/{P}","get","path","P");
		assertObjectEquals("{'in':'path',name:'P',type:'array',required:true,items:{type:'string'}}", x);
	}
	@Test
	public void sb04_Path_onPojo_autoDetectStringObject() throws Exception {
		ParameterInfo x = sb.getParameterInfo("/autoDetectStringObject/{P}","get","path","P");
		assertObjectEquals("{'in':'path',name:'P',type:'string',required:true}", x);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Examples
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource
	public static class SC {

		@Path(name="P", example={" {f1:'a'} "})
		public static class SC01 {
			public String f1;
		}
		@RestMethod(name=GET,path="/example/{P}")
		public void sc01(SC01 f) {}
	}


	static Swagger sc = getSwagger(SC.class);

	@Test
	public void sc01_Path_onPojo_example() throws Exception {
		ParameterInfo x = sc.getParameterInfo("/example/{P}","get","path","P");
		assertEquals("{f1:'a'}", x.getExample());
	}

	//=================================================================================================================
	// @Path on parameter
	//=================================================================================================================

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource
	public static class TA {

		@RestMethod(name=GET,path="/basic/{P}")
		public void ta01(@Path(
			name="P",
			description="a",
			type="string"
		) String h) {}

		@RestMethod(name=GET,path="/api/{P}")
		public void ta02(@Path(
			name="P",
			api={
				"description:'a',",
				"type:'string'"
			}
		) String h) {}

		@RestMethod(name=GET,path="/mixed/{P}")
		public void ta03(@Path(
			name="P",
			api={
				"description:'b',",
				"type:'string'"
			},
			description="a",
			type="string"
		) String h) {}

		@RestMethod(name=GET,path="/value/{P}")
		public void ta04(@Path("P") String h) {}

		@RestMethod(name=GET,path="/enum/{P}")
		public void ta05(@Path(name="P", _enum={" ['a','b'] "}) String h) {}
	}

	static Swagger ta = getSwagger(TA.class);

	@Test
	public void ta01_Path_onParameter_basic() throws Exception {
		ParameterInfo x = ta.getParameterInfo("/basic/{P}","get","path","P");
		assertEquals("a", x.getDescription());
		assertEquals("string", x.getType());
	}
	@Test
	public void ta02_Path_onParameter_api() throws Exception {
		ParameterInfo x = ta.getParameterInfo("/api/{P}","get","path","P");
		assertEquals("a", x.getDescription());
		assertEquals("string", x.getType());
	}
	@Test
	public void ta03_Path_onParameter_mixed() throws Exception {
		ParameterInfo x = ta.getParameterInfo("/mixed/{P}","get","path","P");
		assertEquals("a", x.getDescription());
		assertEquals("string", x.getType());
	}
	@Test
	public void ta04_Path_onParameter_value() throws Exception {
		ParameterInfo x = ta.getParameterInfo("/value/{P}","get","path","P");
		assertEquals("P", x.getName());
	}
	@Test
	public void ta05_Path_onParameter_enum() throws Exception {
		ParameterInfo x = ta.getParameterInfo("/enum/{P}","get","path","P");
		assertObjectEquals("['a','b']", x.getEnum());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Schema
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource
	public static class TB {

		@RestMethod(name=GET,path="/schemaValue/{P}")
		public void tb01(@Path(name="P") String h) {}

		public static class TB02 {
			public String f1;
		}
		@RestMethod(name=GET,path="/autoDetectBean/{P}")
		public void tb02(@Path("P") TB02 b) {}

		public static class TB03 extends LinkedList<String> {
			private static final long serialVersionUID = 1L;
		}
		@RestMethod(name=GET,path="/autoDetectList/{P}")
		public void tb03(@Path("P") TB03 b) {}

		public static class TB04 {}
		@RestMethod(name=GET,path="/autoDetectStringObject/{P}")
		public void tb04(@Path("P") TB04 b) {}

		@RestMethod(name=GET,path="/autoDetectInteger/{P}")
		public void tb05(@Path("P") Integer b) {}

		@RestMethod(name=GET,path="/autoDetectBoolean/{P}")
		public void tb06(@Path("P") Boolean b) {}
	}

	static Swagger tb = getSwagger(TB.class);

	@Test
	public void tb01_Path_onParameter_schemaValue() throws Exception {
		ParameterInfo x = tb.getParameterInfo("/schemaValue/{P}","get","path","P");
		assertObjectEquals("{'in':'path',name:'P',type:'string',required:true}", x);
	}
	@Test
	public void tb02_Path_onParameter_autoDetectBean() throws Exception {
		ParameterInfo x = tb.getParameterInfo("/autoDetectBean/{P}","get","path","P");
		assertObjectEquals("{'in':'path',name:'P',type:'object',required:true,schema:{properties:{f1:{type:'string'}}}}", x);
	}
	@Test
	public void tb03_Path_onParameter_autoDetectList() throws Exception {
		ParameterInfo x = tb.getParameterInfo("/autoDetectList/{P}","get","path","P");
		assertObjectEquals("{'in':'path',name:'P',type:'array',required:true,items:{type:'string'}}", x);
	}
	@Test
	public void tb04_Path_onParameter_autoDetectStringObject() throws Exception {
		ParameterInfo x = tb.getParameterInfo("/autoDetectStringObject/{P}","get","path","P");
		assertObjectEquals("{'in':'path',name:'P',type:'string',required:true}", x);
	}
	@Test
	public void tb05_Path_onParameter_autoDetectInteger() throws Exception {
		ParameterInfo x = tb.getParameterInfo("/autoDetectInteger/{P}","get","path","P");
		assertObjectEquals("{'in':'path',name:'P',type:'integer',required:true,format:'int32'}", x);
	}
	@Test
	public void tb06_Path_onParameter_autoDetectBoolean() throws Exception {
		ParameterInfo x = tb.getParameterInfo("/autoDetectBoolean/{P}","get","path","P");
		assertObjectEquals("{'in':'path',name:'P',type:'boolean',required:true}", x);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Examples
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource
	public static class TC {

		@RestMethod(name=GET,path="/example/{P}")
		public void ta21(@Path(name="P", example="{f1:'b'}") String h) {}
	}

	static Swagger tc = getSwagger(TC.class);

	@Test
	public void tc01_Path_onParameter_example2() throws Exception {
		ParameterInfo x = tc.getParameterInfo("/example/{P}","get","path","P");
		assertEquals("{f1:'b'}", x.getExample());
	}
}