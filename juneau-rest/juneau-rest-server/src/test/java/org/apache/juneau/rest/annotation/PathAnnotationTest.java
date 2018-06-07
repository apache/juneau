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
 * Tests related to @Path annotation.
 */
@SuppressWarnings({"javadoc"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PathAnnotationTest {
	
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
		public String simplePathWithRemainder(@Path("foo") String foo, @Path("bar") int bar, @PathRemainder String remainder) {
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

	@RestResource()
	public static class SA {

		@Path(
			name="P",
			description={"a","b"},
			type="a",
			format="a",
			pattern="a",
			maximum="1",
			minimum="1",
			multipleOf="1",
			maxLength="1",
			minLength="1",
			allowEmptyValue="true",
			exclusiveMaximum="true",
			exclusiveMinimum="true",
			schema=@Schema(type="a"),
			_enum="a,b",
			example="'a'"
		)
		public static class SA00 {
			public SA00(String x) {}
		}
		
		@RestMethod(name=GET,path="/basic/{P}")
		public void sa00(SA00 f) {}

		@Path("P")
		public static class SA02 {}
		
		@RestMethod(name=GET,path="/value/{P}")
		public void sa02(SA02 f) {}

		@Path(name="P", description="a")
		public static class SA03 {}
		
		@Path(name="P", schema=@Schema(" type:'b' "))
		public static class SA16b {}
		
		@RestMethod(name=GET,path="/schema2/{P}")
		public void sa16b(SA16b f) {}

		@Path("P")
		public static class SA16c {
			public String f1;
		}

		@RestMethod(name=GET,path="/schema3/{P}")
		public void sa16c(SA16c b) {}

		@Path("P")
		public static class SA16d extends LinkedList<String> {
			private static final long serialVersionUID = 1L;
		}

		@RestMethod(name=GET,path="/schema4/{P}")
		public void sa16d(SA16d b) {}

		@Path("P")
		public static class SA16e {}

		@RestMethod(name=GET,path="/schema5/{P}")
		public void sa16e(SA16e b) {}

		@Path(name="P", _enum={" ['a','b'] "})
		public static class SA19 {}
		
		@RestMethod(name=GET,path="/_enum2/{P}")
		public void sa19(SA19 f) {}

		@Path(name="P", example={" {f1:'a'} "})
		public static class SA21 {
			public String f1;
		}
		
		@RestMethod(name=GET,path="/example2/{P}")
		public void sa21(SA21 f) {}
	}

	@Test
	public void sa00_Path_onPojo_basic() throws Exception {
		ParameterInfo x = getSwagger(new SA()).getPaths().get("/basic/{P}").get("get").getParameter("path", "P");
		assertEquals("P", x.getName());
		assertEquals("a\nb", x.getDescription());
		assertEquals("a", x.getType());
		assertEquals("a", x.getFormat());
		assertEquals("a", x.getPattern());
		assertObjectEquals("1", x.getMaximum());
		assertObjectEquals("1", x.getMinimum());
		assertObjectEquals("1", x.getMultipleOf());
		assertObjectEquals("1", x.getMaxLength());
		assertObjectEquals("1", x.getMinLength());
		assertObjectEquals("true", x.getAllowEmptyValue());
		assertObjectEquals("true", x.getExclusiveMaximum());
		assertObjectEquals("true", x.getExclusiveMinimum());
		assertObjectEquals("{type:'a'}", x.getSchema());
		assertObjectEquals("['a','b']", x.getEnum());
		assertObjectEquals("'a'", x.getExample());
	}
	@Test
	public void sa02_Path_onPojo_value() throws Exception {
		ParameterInfo x = getSwagger(new SA()).getPaths().get("/value/{P}").get("get").getParameter("path", "P");
		assertEquals("P", x.getName());
	}
	@Test
	public void sa16b_Path_onPojo_schema2() throws Exception {
		ParameterInfo x = getSwagger(new SA()).getPaths().get("/schema2/{P}").get("get").getParameter("path", "P");
		assertObjectEquals("{type:'b'}", x.getSchema());
	}
	@Test
	public void sa16c_Path_onPojo_schema3() throws Exception {
		ParameterInfo x = getSwagger(new SA()).getPaths().get("/schema3/{P}").get("get").getParameter("path", "P");
		assertObjectEquals("{type:'object',properties:{f1:{type:'string'}}}", x.getSchema());
	}
	@Test
	public void sa16d_Path_onPojo_schema4() throws Exception {
		ParameterInfo x = getSwagger(new SA()).getPaths().get("/schema4/{P}").get("get").getParameter("path", "P");
		assertObjectEquals("{type:'array',items:{type:'string'}}", x.getSchema());
	}
	@Test
	public void sa16e_Path_onPojo_schema5() throws Exception {
		ParameterInfo x = getSwagger(new SA()).getPaths().get("/schema5/{P}").get("get").getParameter("path", "P");
		assertObjectEquals("{type:'string'}", x.getSchema());
	}
	@Test
	public void sa19_Path_onPojo__enum2() throws Exception {
		ParameterInfo x = getSwagger(new SA()).getPaths().get("/_enum2/{P}").get("get").getParameter("path", "P");
		assertObjectEquals("['a','b']", x.getEnum());
	}
	@Test
	public void sa21_Path_onPojo_example2() throws Exception {
		ParameterInfo x = getSwagger(new SA()).getPaths().get("/example2/{P}").get("get").getParameter("path", "P");
		assertObjectEquals("{f1:'a'}", x.getExample());
	}

	//=================================================================================================================
	// @Path on parameter
	//=================================================================================================================

	@RestResource()
	public static class SB {

		@RestMethod(name=GET,path="/basic/{P}")
		public void sb00(@Path(
			name="P",
			description="a",
			type="a",
			format="a",
			pattern="a",
			maximum="1",
			minimum="1",
			multipleOf="1",
			maxLength="1",
			minLength="1",
			allowEmptyValue="true",
			exclusiveMaximum="true",
			exclusiveMinimum="true",
			schema=@Schema(type="a"),
			_enum=" a,b ",
			example="'a'"
		) String h) {}

		@RestMethod(name=GET,path="/value/{P}")
		public void sb02(@Path("P") String h) {}

		@RestMethod(name=GET,path="/schema2/{P}")
		public void sb16b(@Path(name="P", schema=@Schema(" type:'b' ")) String h) {}

		public static class SB16c {
			public String f1;
		}

		@RestMethod(name=GET,path="/schema3/{P}")
		public void sb16c(@Path("P") SB16c b) {}

		public static class SB16d extends LinkedList<String> {
			private static final long serialVersionUID = 1L;
		}

		@RestMethod(name=GET,path="/schema4/{P}")
		public void sb16d(@Path("P") SB16d b) {}

		public static class SB16e {}

		@RestMethod(name=GET,path="/schema5/{P}")
		public void sb16e(@Path("P") SB16e b) {}

		@RestMethod(name=GET,path="/schema6/{P}")
		public void sb16f(@Path("P") Integer b) {}

		@RestMethod(name=GET,path="/schema7/{P}")
		public void sb16g(@Path("P") Boolean b) {}
		
		@RestMethod(name=GET,path="/enum2/{P}")
		public void sb19(@Path(name="P", _enum={" ['a','b'] "}) String h) {}

		@RestMethod(name=GET,path="/example2/{P}")
		public void sb21(@Path(name="P", example="{f1:'b'}") String h) {}
	}
	
	@Test
	public void sb00_Path_onParameter_basic() throws Exception {
		ParameterInfo x = getSwagger(new SB()).getPaths().get("/basic/{P}").get("get").getParameter("path", "P");
		assertEquals("a", x.getDescription());
		assertEquals("a", x.getType());
		assertEquals("a", x.getFormat());
		assertEquals("a", x.getPattern());
		assertObjectEquals("1", x.getMaximum());
		assertObjectEquals("1", x.getMinimum());
		assertObjectEquals("1", x.getMultipleOf());
		assertObjectEquals("1", x.getMaxLength());
		assertObjectEquals("1", x.getMinLength());
		assertObjectEquals("true", x.getAllowEmptyValue());
		assertObjectEquals("true", x.getExclusiveMaximum());
		assertObjectEquals("true", x.getExclusiveMinimum());
		assertObjectEquals("{type:'a'}", x.getSchema());
		assertObjectEquals("['a','b']", x.getEnum());
		assertObjectEquals("'a'", x.getExample());
	}
	@Test
	public void sb02_Path_onParameter_value() throws Exception {
		ParameterInfo x = getSwagger(new SB()).getPaths().get("/value/{P}").get("get").getParameter("path", "P");
		assertEquals("P", x.getName());
	}
	@Test
	public void sb16b_Path_onParameter_schema2() throws Exception {
		ParameterInfo x = getSwagger(new SB()).getPaths().get("/schema2/{P}").get("get").getParameter("path", "P");
		assertObjectEquals("{type:'b'}", x.getSchema());
	}
	@Test
	public void sb16c_Path_onParameter_schema3() throws Exception {
		ParameterInfo x = getSwagger(new SB()).getPaths().get("/schema3/{P}").get("get").getParameter("path", "P");
		assertObjectEquals("{type:'object',properties:{f1:{type:'string'}}}", x.getSchema());
	}
	@Test
	public void sb16d_Path_onParameter_schema4() throws Exception {
		ParameterInfo x = getSwagger(new SB()).getPaths().get("/schema4/{P}").get("get").getParameter("path", "P");
		assertObjectEquals("{type:'array',items:{type:'string'}}", x.getSchema());
	}
	@Test
	public void sb16e_Path_onParameter_schema5() throws Exception {
		ParameterInfo x = getSwagger(new SB()).getPaths().get("/schema5/{P}").get("get").getParameter("path", "P");
		assertObjectEquals("{type:'string'}", x.getSchema());
	}
	@Test
	public void sb16f_Path_onParameter_schema6() throws Exception {
		ParameterInfo x = getSwagger(new SB()).getPaths().get("/schema6/{P}").get("get").getParameter("path", "P");
		assertObjectEquals("{format:'int32',type:'integer'}", x.getSchema());
	}
	@Test
	public void sb16g_Path_onParameter_schema7() throws Exception {
		ParameterInfo x = getSwagger(new SB()).getPaths().get("/schema7/{P}").get("get").getParameter("path", "P");
		assertObjectEquals("{type:'boolean'}", x.getSchema());
	}
	@Test
	public void sb19_Path_onParameter__enum2() throws Exception {
		ParameterInfo x = getSwagger(new SB()).getPaths().get("/enum2/{P}").get("get").getParameter("path", "P");
		assertObjectEquals("['a','b']", x.getEnum());
	}
	@Test
	public void sb21_Path_onParameter_example2() throws Exception {
		ParameterInfo x = getSwagger(new SB()).getPaths().get("/example2/{P}").get("get").getParameter("path", "P");
		assertObjectEquals("{f1:'b'}", x.getExample());
	}
}