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
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.json.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock2.*;
import org.apache.juneau.rest.testutils.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class PathAnnotationTest {

	//=================================================================================================================
	// Basic tests
	//=================================================================================================================

	@Rest
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
	static MockRest a = MockRest.build(A.class);

	@Test
	public void a00_nonExistentPath() throws Exception {
		a.get("/bad?noTrace=true").execute().assertStatus(404);
	}
	@Test
	public void a01_noPath() throws Exception {
		a.get(null).execute().assertBody("GET");
		a.get().execute().assertBody("GET");
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

	@Rest
	public static class B  {
		@RestMethod(name=GET, path="/int/{x}/foo")
		public String b01(@Path("x") int x) {
			return String.valueOf(x);
		}
		@RestMethod(name=GET, path="/short/{x}/foo")
		public String b02(@Path("x") short x) {
			return String.valueOf(x);
		}
		@RestMethod(name=GET, path="/long/{x}/foo")
		public String b03(@Path("x") long x) {
			return String.valueOf(x);
		}
		@RestMethod(name=GET, path="/char/{x}/foo")
		public String b04(@Path("x") char x) {
			return String.valueOf(x);
		}
		@RestMethod(name=GET, path="/float/{x}/foo")
		public String b05(@Path("x") float x) {
			return String.valueOf(x);
		}
		@RestMethod(name=GET, path="/double/{x}/foo")
		public String b06(@Path("x") double x) {
			return String.valueOf(x);
		}
		@RestMethod(name=GET, path="/byte/{x}/foo")
		public String b07(@Path("x") byte x) {
			return String.valueOf(x);
		}
		@RestMethod(name=GET, path="/boolean/{x}/foo")
		public String b08(@Path("x") boolean x) {
			return String.valueOf(x);
		}
	}
	static MockRest b = MockRest.build(B.class);

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

	@Rest
	public static class C  {
		@RestMethod(name=GET, path="/Integer/{x}/foo")
		public String c01(@Path("x") Integer x) {
			return String.valueOf(x);
		}
		@RestMethod(name=GET, path="/Short/{x}/foo")
		public String c02(@Path("x") Short x) {
			return String.valueOf(x);
		}
		@RestMethod(name=GET, path="/Long/{x}/foo")
		public String c03(@Path("x") Long x) {
			return String.valueOf(x);
		}
		@RestMethod(name=GET, path="/Character/{x}/foo")
		public String c04(@Path("x") Character x) {
			return String.valueOf(x);
		}
		@RestMethod(name=GET, path="/Float/{x}/foo")
		public String c05(@Path("x") Float x) {
			return String.valueOf(x);
		}
		@RestMethod(name=GET, path="/Double/{x}/foo")
		public String c06(@Path("x") Double x) {
			return String.valueOf(x);
		}
		@RestMethod(name=GET, path="/Byte/{x}/foo")
		public String c07(@Path("x") Byte x) {
			return String.valueOf(x);
		}
		@RestMethod(name=GET, path="/Boolean/{x}/foo")
		public String c08(@Path("x") Boolean x) {
			return String.valueOf(x);
		}
	}
	static MockRest c = MockRest.build(C.class);

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

	@Rest
	public static class D {
		// Object with forString(String) method
		@RestMethod(name=GET, path="/uuid/{uuid}")
		public UUID uuid(RestResponse res, @Path("uuid") UUID uuid) {
			return uuid;
		}
	}
	static MockRest d = MockRest.build(D.class);

	@Test
	public void d01_uuid() throws Exception {
		UUID uuid = UUID.randomUUID();
		d.get("/uuid/" + uuid).execute().assertBody(uuid.toString());
	}

	//=================================================================================================================
	// @Path annotation without name.
	//=================================================================================================================

	@Rest
	public static class E  {
		@RestMethod(name=GET, path="/x/{foo}/{bar}")
		public Object normal1(@Path String foo, @Path String bar) {
			return OMap.of("m", "normal1", "foo", foo, "bar", bar);
		}
		@RestMethod(name=GET, path="/x/{foo}/x/{bar}/x")
		public Object normal2(@Path String foo, @Path String bar) {
			return OMap.of("m", "normal2", "foo", foo, "bar", bar);
		}
		@RestMethod(name=GET, path="/y/{0}/{1}")
		public Object numbers1(@Path String foo, @Path String bar) {
			return OMap.of("m", "numbers1", "0", foo, "1", bar);
		}
		@RestMethod(name=GET, path="/y/{0}/y/{1}/y")
		public Object numbers2(@Path String foo, @Path String bar) {
			return OMap.of("m", "numbers2", "0", foo, "1", bar);
		}
		@RestMethod(name=GET, path="/z/{1}/z/{0}/z")
		public Object numbers3(@Path String foo, @Path String bar) {
			return OMap.of("m", "numbers3", "0", foo, "1", bar);
		}
	}
	static MockRest e = MockRest.build(E.class);

	@Test
	public void e01_normal1() throws Exception {
		e.get("/x/x1/x2").execute().assertBody("{m:'normal1',foo:'x1',bar:'x2'}");
	}
	@Test
	public void e02_normal2() throws Exception {
		e.get("/x/x1/x/x2/x").execute().assertBody("{m:'normal2',foo:'x1',bar:'x2'}");
	}
	@Test
	public void e03_numbers1() throws Exception {
		e.get("/y/y1/y2").execute().assertBody("{m:'numbers1','0':'y1','1':'y2'}");
	}
	@Test
	public void e04_numbers2() throws Exception {
		e.get("/y/y1/y/y2/y").execute().assertBody("{m:'numbers2','0':'y1','1':'y2'}");
	}
	@Test
	public void e05_numbers3() throws Exception {
		e.get("/z/z1/z/z2/z").execute().assertBody("{m:'numbers3','0':'z2','1':'z1'}");
	}

	//=================================================================================================================
	// Path variables on class.
	//=================================================================================================================

	@Rest(path="/f/{a}/{b}")
	public static class F  {
		@RestMethod(name=GET, path="/")
		public String noPath(RequestPath path) {
			return format("noPath: {0}", path);
		}
		@RestMethod(name=GET, path="/*")
		public String noPath2(RequestPath path) {
			return format("noPath2: {0}", path);
		}
		@RestMethod(name=GET, path="/a")
		public String noVars(RequestPath path) {
			return format("noVars: {0}", path);
		}
		@RestMethod(name=GET, path="/b/{c}/{d}")
		public String twoVars(RequestPath path) {
			return format("twoVars: {0}", path);
		}
		@RestMethod(name=GET, path="/c/{a}/{b}")
		public String twoVarsOverlapping(RequestPath path) {
			return format("twoVarsOverlapping: {0}", path);
		}
		@RestMethod(name=GET, path="/d/{c}/{d}/*")
		public String withRemainder(RequestPath path) {
			return format("withRemainder: {0}", path);
		}
		private String format(String msg, Object...args) {
			return SimpleJson.DEFAULT.format(msg, args);
		}
	}
	static MockRest f = MockRest.create(F.class).servletPath("/f").build();

	@Test
	public void f01a_noPath() throws Exception {
		f.get("/f/x1/x2").execute().assertBody("noPath: {a:'x1',b:'x2'}");
	}

	@Test
	public void f01b_incompletePath() throws Exception {
		f.get("/f/x1").execute().assertStatus(404);
		f.get("/f").execute().assertStatus(404);
	}

	@Test
	public void f01c_noPath_blanks() throws Exception {
		f.get("/f//").execute().assertStatus(404);
		f.get("/f/x/").execute().assertStatus(404);
		f.get("/f//x").execute().assertStatus(404);
	}

	@Test
	public void f02a_noPath2() throws Exception {
		f.get("/f/x1/x2/foo").execute().assertBody("noPath2: {'/*':'foo','/**':'foo',a:'x1',b:'x2'}");
	}

	@Test
	public void f02b_noPath2_blanks() throws Exception {
		f.get("/f///foo").execute().assertStatus(404);
		f.get("/f/x1//foo").execute().assertStatus(404);
		f.get("/f//x2/foo").execute().assertStatus(404);
	}

	@Test
	public void f03a_noVars() throws Exception {
		f.get("/f/x1/x2/a").execute().assertBody("noVars: {a:'x1',b:'x2'}");
	}

	@Test
	public void f03b_noVars_blanks() throws Exception {
		f.get("/f///a").execute().assertStatus(404);
		f.get("/f/x1//a").execute().assertStatus(404);
		f.get("/f//x2/a").execute().assertStatus(404);
	}

	@Test
	public void f04a_twoVars() throws Exception {
		f.get("/f/x1/x2/b/x3/x4").execute().assertBody("twoVars: {a:'x1',b:'x2',c:'x3',d:'x4'}");
	}

	@Test
	public void f04b_twoVars_blanks() throws Exception {
		f.get("/f//x2/b/x3/x4").execute().assertStatus(404);
		f.get("/f/x1//b/x3/x4").execute().assertStatus(404);
		f.get("/f/x1/x2/b//x4").execute().assertStatus(200);
		f.get("/f/x1/x2/b/x3/").execute().assertStatus(200);
		f.get("/f///b//").execute().assertStatus(404);
	}

	@Test
	public void f05_twoVarsOverlapping() throws Exception {
		f.get("/f/x1/x2/c/x3/x4").execute().assertBody("twoVarsOverlapping: {a:'x3',b:'x4'}");
	}

	@Test
	public void f06a_withRemainder() throws Exception {
		f.get("/f/x1/x2/d/x3/x4").execute().assertBody("withRemainder: {a:'x1',b:'x2',c:'x3',d:'x4'}");
	}

	@Test
	public void f06b_withRemainder_lank() throws Exception {
		f.get("/f/x1/x2/d/x3/x4/").execute().assertBody("withRemainder: {'/*':'','/**':'',a:'x1',b:'x2',c:'x3',d:'x4'}");
	}

	@Test
	public void f06c_withRemainderWithStuff() throws Exception {
		f.get("/f/x1/x2/d/x3/x4/foo/bar").execute().assertBody("withRemainder: {'/*':'foo/bar','/**':'foo/bar',a:'x1',b:'x2',c:'x3',d:'x4'}");
	}

	//=================================================================================================================
	// Path variables on child class.
	//=================================================================================================================

	@Rest(children={F.class})
	public static class G {}

	static MockRest g = MockRest.create(G.class).build();

	@Test
	public void g01a_noPath() throws Exception {
		g.get("/f/x1/x2").execute().assertBody("noPath: {a:'x1',b:'x2'}");
	}

	@Test
	public void g01b_incompletePath() throws Exception {
		g.get("/f/x1").execute().assertStatus(404);
		g.get("/f").execute().assertStatus(404);
	}

	@Test
	public void g01c_noPath_blanks() throws Exception {
		g.get("/f//").execute().assertStatus(404);
		g.get("/f/x1/").execute().assertStatus(404);
		g.get("/f//x2").execute().assertStatus(404);
	}

	@Test
	public void g02a_noVars() throws Exception {
		g.get("/f/x1/x2/a").execute().assertBody("noVars: {a:'x1',b:'x2'}");
	}

	@Test
	public void g02b_noVars_blanks() throws Exception {
		g.get("/f///a").execute().assertStatus(404);
		g.get("/f/x1//a").execute().assertStatus(404);
		g.get("/f//x2/a").execute().assertStatus(404);
	}

	@Test
	public void g03a_twoVars() throws Exception {
		g.get("/f/x1/x2/b/x3/x4").execute().assertBody("twoVars: {a:'x1',b:'x2',c:'x3',d:'x4'}");
	}

	@Test
	public void g03b_twoVars_blanks() throws Exception {
		g.get("/f//x2/b/x3/x4").execute().assertStatus(404);
		g.get("/f/x1//b/x3/x4").execute().assertStatus(404);
		g.get("/f/x1/x2/b//x4").execute().assertStatus(200);
		g.get("/f/x1/x2/b/x3/").execute().assertStatus(200);
		g.get("/f///b//").execute().assertStatus(404);
	}

	@Test
	public void g04_twoVarsOverlapping() throws Exception {
		g.get("/f/x1/x2/c/x3/x4").execute().assertBody("twoVarsOverlapping: {a:'x3',b:'x4'}");
	}

	@Test
	public void g05a_withRemainder() throws Exception {
		g.get("/f/x1/x2/d/x3/x4").execute().assertBody("withRemainder: {a:'x1',b:'x2',c:'x3',d:'x4'}");
	}

	@Test
	public void g05b_withRemainderBlank() throws Exception {
		g.get("/f/x1/x2/d/x3/x4/").execute().assertBody("withRemainder: {'/*':'','/**':'',a:'x1',b:'x2',c:'x3',d:'x4'}");
	}

	@Test
	public void g05c_withRemainderWithStuff() throws Exception {
		g.get("/f/x1/x2/d/x3/x4/foo/bar").execute().assertBody("withRemainder: {'/*':'foo/bar','/**':'foo/bar',a:'x1',b:'x2',c:'x3',d:'x4'}");
	}

	//=================================================================================================================
	// Path variables on parent and child class.
	//=================================================================================================================

	@Rest(path="/h/{ha}/{hb}", children={F.class})
	public static class H {}

	static MockRest h = MockRest.create(H.class).servletPath("/h").build();

	@Test
	public void h01a_noPath() throws Exception {
		h.get("/h/ha1/hb1/f/x1/x2").execute().assertBody("noPath: {a:'x1',b:'x2',ha:'ha1',hb:'hb1'}");
	}

	@Test
	public void h01b_incompletePath() throws Exception {
		// These are 405 instead of 404 because when children don't match, we try to find a matching Java method.
		h.get("/h/ha1/hb1/f/x1").execute().assertStatus(404);
		h.get("/h/ha1/hb1/f").execute().assertStatus(404);
		h.get("/h/ha1/hb1").execute().assertStatus(404);
		h.get("/h/ha1").execute().assertStatus(404);
		h.get("/h").execute().assertStatus(404);
	}

	@Test
	public void h01c_noPath_blanks() throws Exception {
		h.get("/h//hb1/f/x1/x2").execute().assertStatus(404);
		h.get("/h/ha1//f/x1/x2").execute().assertStatus(404);
		h.get("/h/ha1/hb1/f//x2").execute().assertStatus(404);
		h.get("/h/ha1/hb1/f/x1/").execute().assertStatus(404);
		h.get("/h///f//").execute().assertStatus(404);
	}

	@Test
	public void h02a_noPath2() throws Exception {
		h.get("/h/ha1/hb1/f/x1/x2/foo").execute().assertBody("noPath2: {'/*':'foo','/**':'foo',a:'x1',b:'x2',ha:'ha1',hb:'hb1'}");
	}

	@Test
	public void h02b_noPath2_blanks() throws Exception {
		h.get("/h//hb1/f/x1/x2/foo").execute().assertStatus(404);
		h.get("/h/ha1//f/x1/x2/foo").execute().assertStatus(404);
		h.get("/h/ha1/hb1/f//x2/foo").execute().assertStatus(404);
		h.get("/h/ha1/hb1/f/x1//foo").execute().assertStatus(404);
		h.get("/h///f///foo").execute().assertStatus(404);
	}

	@Test
	public void h03a_noVars() throws Exception {
		h.get("/h/ha1/hb1/f/x1/x2/a").execute().assertBody("noVars: {a:'x1',b:'x2',ha:'ha1',hb:'hb1'}");
	}

	@Test
	public void h03b_noVars_blanks() throws Exception {
		h.get("/h//hb1/f/x1/x2/a").execute().assertStatus(404);
		h.get("/h/ha1//f/x1/x2/a").execute().assertStatus(404);
		h.get("/h/ha1/hb1/f//x2/a").execute().assertStatus(404);
		h.get("/h/ha1/hb1/f/x1//a").execute().assertStatus(404);
		h.get("/h///f///a").execute().assertStatus(404);
	}

	@Test
	public void h04_twoVars() throws Exception {
		h.get("/h/ha1/hb1/f/x1/x2/b/x3/x4").execute().assertBody("twoVars: {a:'x1',b:'x2',c:'x3',d:'x4',ha:'ha1',hb:'hb1'}");
	}

	@Test
	public void h05_twoVarsOverlapping() throws Exception {
		h.get("/h/ha1/hb1/f/x1/x2/c/x3/x4").execute().assertBody("twoVarsOverlapping: {a:'x3',b:'x4',ha:'ha1',hb:'hb1'}");
	}

	@Test
	public void h06a_withRemainder() throws Exception {
		h.get("/h/ha1/hb1/f/x1/x2/d/x3/x4").execute().assertBody("withRemainder: {a:'x1',b:'x2',c:'x3',d:'x4',ha:'ha1',hb:'hb1'}");
	}

	@Test
	public void h06b_withRemainderBlank() throws Exception {
		h.get("/h/ha1/hb1/f/x1/x2/d/x3/x4/").execute().assertBody("withRemainder: {'/*':'','/**':'',a:'x1',b:'x2',c:'x3',d:'x4',ha:'ha1',hb:'hb1'}");
	}

	@Test
	public void h06c_withRemainderWithStuff() throws Exception {
		h.get("/h/ha1/hb1/f/x1/x2/d/x3/x4/foo/bar").execute().assertBody("withRemainder: {'/*':'foo/bar','/**':'foo/bar',a:'x1',b:'x2',c:'x3',d:'x4',ha:'ha1',hb:'hb1'}");
	}

	//=================================================================================================================
	// Path variables on parents and child class.
	//=================================================================================================================

	@Rest(path="/i/{ia}/{ib}", children={H.class})
	public static class I {}

	static MockRest i = MockRest.create(I.class).servletPath("/i").build();

	@Test
	public void i01a_noPath() throws Exception {
		i.get("/i/ia1/ib1/h/ha1/hb1/f/x1/x2").execute().assertBody("noPath: {a:'x1',b:'x2',ha:'ha1',hb:'hb1',ia:'ia1',ib:'ib1'}");
	}

	@Test
	public void i01b_incompletePath() throws Exception {
		i.get("/i/ia1/ib1/h/ha1/hb1/f/x1").execute().assertStatus(404);
		i.get("/i/ia1/ib1/h/ha1/hb1/f").execute().assertStatus(404);
		i.get("/i/ia1/ib1/h/ha1/hb1").execute().assertStatus(404);
		i.get("/i/ia1/ib1/h/ha1").execute().assertStatus(404);
		i.get("/i/ia1/ib1/h").execute().assertStatus(404);
		i.get("/i/ia1/ib1").execute().assertStatus(404);
		i.get("/i/ia1").execute().assertStatus(404);
		i.get("/i").execute().assertStatus(404);
	}

	@Test
	public void i01c_noPath_blanks() throws Exception {
		i.get("/i//ib1/h/ha1/hb1/f/x1/x2").execute().assertStatus(404);
		i.get("/i/ia1//h/ha1/hb1/f/x1/x2").execute().assertStatus(404);
		i.get("/i/ia1/ib1/h//hb1/f/x1/x2").execute().assertStatus(404);
		i.get("/i/ia1/ib1/h/ha1//f/x1/x2").execute().assertStatus(404);
		i.get("/i/ia1/ib1/h/ha1/hb1/f//x2").execute().assertStatus(404);
		i.get("/i/ia1/ib1/h/ha1/hb1/f/x1/").execute().assertStatus(404);
		i.get("/i///h///f//").execute().assertStatus(404);
	}

	@Test
	public void i02_noPath2() throws Exception {
		i.get("/i/ia1/ib1/h/ha1/hb1/f/x1/x2/foo").execute().assertBody("noPath2: {'/*':'foo','/**':'foo',a:'x1',b:'x2',ha:'ha1',hb:'hb1',ia:'ia1',ib:'ib1'}");
	}

	@Test
	public void i03_noVars() throws Exception {
		i.get("/i/ia1/ib1/h/ha1/hb1/f/x1/x2/a").execute().assertBody("noVars: {a:'x1',b:'x2',ha:'ha1',hb:'hb1',ia:'ia1',ib:'ib1'}");
	}

	@Test
	public void i04_twoVars() throws Exception {
		i.get("/i/ia1/ib1/h/ha1/hb1/f/x1/x2/b/x3/x4").execute().assertBody("twoVars: {a:'x1',b:'x2',c:'x3',d:'x4',ha:'ha1',hb:'hb1',ia:'ia1',ib:'ib1'}");
	}

	@Test
	public void i05_twoVarsOverlapping() throws Exception {
		i.get("/i/ia1/ib1/h/ha1/hb1/f/x1/x2/c/x3/x4").execute().assertBody("twoVarsOverlapping: {a:'x3',b:'x4',ha:'ha1',hb:'hb1',ia:'ia1',ib:'ib1'}");
	}

	@Test
	public void i06a_withRemainder() throws Exception {
		i.get("/i/ia1/ib1/h/ha1/hb1/f/x1/x2/d/x3/x4").execute().assertBody("withRemainder: {a:'x1',b:'x2',c:'x3',d:'x4',ha:'ha1',hb:'hb1',ia:'ia1',ib:'ib1'}");
	}

	@Test
	public void i06b_withRemainderBlank() throws Exception {
		i.get("/i/ia1/ib1/h/ha1/hb1/f/x1/x2/d/x3/x4/").execute().assertBody("withRemainder: {'/*':'','/**':'',a:'x1',b:'x2',c:'x3',d:'x4',ha:'ha1',hb:'hb1',ia:'ia1',ib:'ib1'}");
	}

	@Test
	public void i06c_withRemainderWithStuff() throws Exception {
		i.get("/i/ia1/ib1/h/ha1/hb1/f/x1/x2/d/x3/x4/foo/bar").execute().assertBody("withRemainder: {'/*':'foo/bar','/**':'foo/bar',a:'x1',b:'x2',c:'x3',d:'x4',ha:'ha1',hb:'hb1',ia:'ia1',ib:'ib1'}");
	}

	//=================================================================================================================
	// Optional path parameter.
	//=================================================================================================================

	@Rest(serializers=SimpleJsonSerializer.class)
	public static class J {
		@RestMethod(name=GET,path="/a/{f1}")
		public Object a(@Path("f1") Optional<Integer> f1) throws Exception {
			assertNotNull(f1);
			return f1;
		}
		@RestMethod(name=GET,path="/b/{f1}")
		public Object b(@Path("f1") Optional<ABean> f1) throws Exception {
			assertNotNull(f1);
			return f1;
		}
		@RestMethod(name=GET,path="/c/{f1}")
		public Object c(@Path("f1") Optional<List<ABean>> f1) throws Exception {
			assertNotNull(f1);
			return f1;
		}
		@RestMethod(name=GET,path="/d/{f1}")
		public Object d(@Path("f1") List<Optional<ABean>> f1) throws Exception {
			return f1;
		}
	}
	static MockRest j = MockRest.buildJson(J.class);

	@Test
	public void j01_optionalParam_integer() throws Exception {
		j.get("/a/123").execute().assertStatus(200).assertBody("123");
	}

	@Test
	public void j02_optionalParam_bean() throws Exception {
		j.get("/b/a=1,b=foo").execute().assertStatus(200).assertBody("{a:1,b:'foo'}");
	}

	@Test
	public void j03_optionalParam_listOfBeans() throws Exception {
		j.get("/c/@((a=1,b=foo))").execute().assertStatus(200).assertBody("[{a:1,b:'foo'}]");
	}

	@Test
	public void j04_optionalParam_listOfOptionals() throws Exception {
		j.get("/d/@((a=1,b=foo))").execute().assertStatus(200).assertBody("[{a:1,b:'foo'}]");
	}


	//=================================================================================================================
	// @Path on POJO
	//=================================================================================================================

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class SA {

		@Path(
			n="P",
			d={"a","b"},
			t="string",
			e="a,b",
			ex="a"
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
			n="P",
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
			n="P",
			api={
				"description:'b\nc',",
				"type:'string',",
				"enum:['b','c'],",
				"x-example:'b'"
			},
			d={"a","b"},
			t="string",
			e="a,b",
			ex="a"
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

		@Path(n="P",e={" ['a','b'] "})
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

	@Rest
	public static class SB {

		@Path(n="P")
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

	@Rest
	public static class SC {

		@Path(n="P",ex={" {f1:'a'} "})
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

	@Rest
	public static class TA {

		@RestMethod(name=GET,path="/basic/{P}")
		public void ta01(@Path(
			n="P",
			d="a",
			t="string"
		) String h) {}

		@RestMethod(name=GET,path="/api/{P}")
		public void ta02(@Path(
			n="P",
			api={
				"description:'a',",
				"type:'string'"
			}
		) String h) {}

		@RestMethod(name=GET,path="/mixed/{P}")
		public void ta03(@Path(
			n="P",
			api={
				"description:'b',",
				"type:'string'"
			},
			d="a",
			t="string"
		) String h) {}

		@RestMethod(name=GET,path="/value/{P}")
		public void ta04(@Path("P") String h) {}

		@RestMethod(name=GET,path="/enum/{P}")
		public void ta05(@Path(n="P",e={" ['a','b'] "}) String h) {}
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

	@Rest
	public static class TB {

		@RestMethod(name=GET,path="/schemaValue/{P}")
		public void tb01(@Path("P") String h) {}

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

	@Rest
	public static class TC {

		@RestMethod(name=GET,path="/example/{P}")
		public void ta21(@Path(n="P",ex="{f1:'b'}") String h) {}
	}

	static Swagger tc = getSwagger(TC.class);

	@Test
	public void tc01_Path_onParameter_example2() throws Exception {
		ParameterInfo x = tc.getParameterInfo("/example/{P}","get","path","P");
		assertEquals("{f1:'b'}", x.getExample());
	}


	//=================================================================================================================
	// Basic tests
	//=================================================================================================================

	@Rest(path="/u1/{u1}",children=U2.class)
	public static class U1 {
	}
	@Rest(path="/u2")
	public static class U2 {
		@RestMethod(path="/")
		public String doGet(@Path(n="u1",r=false) String u1) {
			return u1 == null ? "nil" : u1;
		}
		@RestMethod(path="/foo/{bar}")
		public String doGetFoo(@Path(n="u1",r=false) String u1, @Path(n="bar",r=false) String bar) {
			return (u1 == null ? "nil" : u1) + "," + (bar == null ? "nil" : bar);
		}
	}

	static MockRest u1 = MockRest.build(U1.class);
	static MockRest u2 = MockRest.build(U2.class);

	@Test
	public void u01_nonRequiredPath() throws Exception {
		u1.get("/u1/foo/u2").execute().assertStatus(200).assertBody("foo");
		u1.get("/u1/foo/u2/foo/xxx").execute().assertStatus(200).assertBody("foo,xxx");
		u2.get("/").execute().assertStatus(200).assertBody("nil");
		u2.get("/foo/xxx").execute().assertStatus(200).assertBody("nil,xxx");
	}

	//=================================================================================================================
	// Multiple paths
	//=================================================================================================================

	@Rest(path="/v1/{v1}",children=V2.class)
	public static class V1 {
	}
	@Rest(path="/v2")
	public static class V2 {
		@RestMethod(paths={"/","/{foo}"})
		public String doGet(@Path(n="v1",r=false) String v1, @Path(n="foo",r=false) String foo) {
			return "1," + (v1 == null ? "nil" : v1) + "," + (foo == null ? "nil" : foo);
		}
		@RestMethod(paths={"/foo","/foo/{foo}"})
		public String doGet2(@Path(n="v1",r=false) String v1, @Path(n="foo",r=false) String foo) {
			return "2," + (v1 == null ? "nil" : v1) + "," + (foo == null ? "nil" : foo);
		}
	}

	static MockRest v1 = MockRest.build(V1.class);
	static MockRest v2 = MockRest.build(V2.class);

	@Test
	public void v01_multiplePaths() throws Exception {
		v1.get("/v1/v1foo/v2").execute().assertStatus(200).assertBody("1,v1foo,nil");
		v1.get("/v1/v1foo/v2/v2foo").execute().assertStatus(200).assertBody("1,v1foo,v2foo");
		v1.get("/v1/v1foo/v2/foo").execute().assertStatus(200).assertBody("2,v1foo,nil");
		v1.get("/v1/v1foo/v2/foo/v2foo").execute().assertStatus(200).assertBody("2,v1foo,v2foo");
		v2.get("/v2").execute().assertStatus(200).assertBody("1,nil,nil");
		v2.get("/v2/v2foo").execute().assertStatus(200).assertBody("1,nil,v2foo");
		v2.get("/v2/foo").execute().assertStatus(200).assertBody("2,nil,nil");
		v2.get("/v2/foo/v2foo").execute().assertStatus(200).assertBody("2,nil,v2foo");
	}
}