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

import static org.apache.juneau.http.HttpMethod.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.json.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.RestResponse;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.testutils.pojos.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Path_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A  {
		@RestMethod(method=GET, path="/")
		public void a(RestResponse res) {
			res.setOutput(GET);
		}
		@RestMethod(method=GET, path="/a")
		public String b() {
			return "GET /a";
		}
		@RestMethod(method=GET, path="/a/{foo}")
		public String c(RestResponse res, @Path("foo") String foo) {
			return "GET /a " + foo;
		}
		@RestMethod(method=GET, path="/a/{foo}/{bar}")
		public String d(RestResponse res, @Path("foo") String foo, @Path("bar") String bar) {
			return "GET /a " + foo + "," + bar;
		}
		@RestMethod(method=GET, path="/a/{foo}/{bar}/*")
		public String e(@Path("foo") String foo, @Path("bar") int bar, @Path("/*") String remainder) {
			return "GET /a "+foo+","+bar+",r="+remainder;
		}
	}

	@Test
	public void a01_basic() throws Exception {
		RestClient a = MockRestClient.buildLax(A.class);

		a.get("/bad?noTrace=true")
			.run()
			.assertCode().is(404);

		a.get(null)
			.run()
			.assertBody().is("GET");
		a.get()
			.run()
			.assertBody().is("GET");

		a.get("/a")
			.run()
			.assertBody().is("GET /a");

		a.get("/a/foo")
			.run()
			.assertBody().is("GET /a foo");

		a.get("/a/foo/bar")
			.run()
			.assertBody().is("GET /a foo,bar");

		a.get("/a/foo/123/baz")
			.run()
			.assertBody().is("GET /a foo,123,r=baz");

		// URL-encoded part should not get decoded before finding method to invoke.
		// This should match /get1/{foo} and not /get1/{foo}/{bar}
		// NOTE:  When testing on Tomcat, must specify the following system property:
		// -Dorg.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true
		a.get("/a/x%2Fy")
			.run()
			.assertBody().is("GET /a x/y");
		a.get("/a/x%2Fy/x%2Fy")
			.run()
			.assertBody().is("GET /a x/y,x/y");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Primitives
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B  {
		@RestMethod(method=GET, path="/a/{x}/foo")
		public String a(@Path("x") int x) {
			return String.valueOf(x);
		}
		@RestMethod(method=GET, path="/b/{x}/foo")
		public String b(@Path("x") short x) {
			return String.valueOf(x);
		}
		@RestMethod(method=GET, path="/c/{x}/foo")
		public String c(@Path("x") long x) {
			return String.valueOf(x);
		}
		@RestMethod(method=GET, path="/d/{x}/foo")
		public String d(@Path("x") char x) {
			return String.valueOf(x);
		}
		@RestMethod(method=GET, path="/e/{x}/foo")
		public String e(@Path("x") float x) {
			return String.valueOf(x);
		}
		@RestMethod(method=GET, path="/f/{x}/foo")
		public String f(@Path("x") double x) {
			return String.valueOf(x);
		}
		@RestMethod(method=GET, path="/g/{x}/foo")
		public String g(@Path("x") byte x) {
			return String.valueOf(x);
		}
		@RestMethod(method=GET, path="/h/{x}/foo")
		public String h(@Path("x") boolean x) {
			return String.valueOf(x);
		}
	}

	@Test
	public void b01_primitives() throws Exception {
		RestClient b = MockRestClient.buildLax(B.class);

		b.get("/a/123/foo")
			.run()
			.assertBody().is("123");
		b.get("/a/bad/foo?noTrace=true")
			.run()
			.assertCode().is(400);

		b.get("/b/123/foo")
			.run()
			.assertBody().is("123");
		b.get("/b/bad/foo?noTrace=true")
			.run()
			.assertCode().is(400);

		b.get("/c/123/foo")
			.run()
			.assertBody().is("123");
		b.get("/c/bad/foo?noTrace=true")
			.run()
			.assertCode().is(400);

		b.get("/d/c/foo")
			.run()
			.assertBody().is("c");
		b.get("/d/bad/foo?noTrace=true")
			.run()
			.assertCode().is(400);

		b.get("/e/1.23/foo")
			.run()
			.assertBody().is("1.23");
		b.get("/e/bad/foo?noTrace=true")
			.run()
			.assertCode().is(400);

		b.get("/f/1.23/foo")
			.run()
			.assertBody().is("1.23");
		b.get("/f/bad/foo?noTrace=true")
			.run()
			.assertCode().is(400);

		b.get("/g/123/foo")
			.run()
			.assertBody().is("123");
		b.get("/g/bad/foo?noTrace=true")
			.run()
			.assertCode().is(400);

		b.get("/h/true/foo")
			.run()
			.assertBody().is("true");
		b.get("/h/bad/foo?noTrace=true")
			.run()
			.assertCode().is(400);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Primitive objects
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class C  {
		@RestMethod(method=GET, path="/a/{x}/foo")
		public String a(@Path("x") Integer x) {
			return String.valueOf(x);
		}
		@RestMethod(method=GET, path="/b/{x}/foo")
		public String b(@Path("x") Short x) {
			return String.valueOf(x);
		}
		@RestMethod(method=GET, path="/c/{x}/foo")
		public String c(@Path("x") Long x) {
			return String.valueOf(x);
		}
		@RestMethod(method=GET, path="/d/{x}/foo")
		public String d(@Path("x") Character x) {
			return String.valueOf(x);
		}
		@RestMethod(method=GET, path="/e/{x}/foo")
		public String e(@Path("x") Float x) {
			return String.valueOf(x);
		}
		@RestMethod(method=GET, path="/f/{x}/foo")
		public String f(@Path("x") Double x) {
			return String.valueOf(x);
		}
		@RestMethod(method=GET, path="/g/{x}/foo")
		public String g(@Path("x") Byte x) {
			return String.valueOf(x);
		}
		@RestMethod(method=GET, path="/h/{x}/foo")
		public String h(@Path("x") Boolean x) {
			return String.valueOf(x);
		}
	}

	@Test
	public void c01_primitiveObjects() throws Exception {
		RestClient c = MockRestClient.buildLax(C.class);

		c.get("/a/123/foo")
			.run()
			.assertBody().is("123");
		c.get("/a/bad/foo?noTrace=true")
			.run()
			.assertCode().is(400);

		c.get("/b/123/foo")
			.run()
			.assertBody().is("123");
		c.get("/b/bad/foo?noTrace=true")
			.run()
			.assertCode().is(400);

		c.get("/c/123/foo")
			.run()
			.assertBody().is("123");
		c.get("/c/bad/foo?noTrace=true")
			.run()
			.assertCode().is(400);

		c.get("/d/c/foo")
			.run()
			.assertBody().is("c");
		c.get("/d/bad/foo?noTrace=true")
			.run()
			.assertCode().is(400);

		c.get("/e/1.23/foo")
			.run()
			.assertBody().is("1.23");
		c.get("/e/bad/foo?noTrace=true")
			.run()
			.assertCode().is(400);

		c.get("/f/1.23/foo")
			.run()
			.assertBody().is("1.23");
		c.get("/f/bad/foo?noTrace=true")
			.run()
			.assertCode().is(400);

		c.get("/g/123/foo")
			.run()
			.assertBody().is("123");
		c.get("/g/bad/foo?noTrace=true")
			.run()
			.assertCode().is(400);

		c.get("/h/true/foo")
			.run()
			.assertBody().is("true");
		c.get("/h/bad/foo?noTrace=true")
			.run()
			.assertCode().is(400);
	}

	//------------------------------------------------------------------------------------------------------------------
	// POJOs convertible from strings
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class D {
		// Object with forString(String) method
		@RestMethod(method=GET, path="/a/{uuid}")
		public UUID a(RestResponse res, @Path("uuid") UUID uuid) {
			return uuid;
		}
	}

	@Test
	public void d01_pojosConvertibleFromStrings() throws Exception {
		RestClient d = MockRestClient.build(D.class);

		UUID uuid = UUID.randomUUID();
		d.get("/a/" + uuid)
			.run()
			.assertBody().is(uuid.toString());
	}

	//------------------------------------------------------------------------------------------------------------------
	// @Path annotation without name.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class E  {
		@RestMethod(method=GET, path="/x/{foo}/{bar}")
		public Object a(@Path String foo, @Path String bar) {
			return OMap.of("m", "normal1", "foo", foo, "bar", bar);
		}
		@RestMethod(method=GET, path="/x/{foo}/x/{bar}/x")
		public Object b(@Path String foo, @Path String bar) {
			return OMap.of("m", "normal2", "foo", foo, "bar", bar);
		}
		@RestMethod(method=GET, path="/y/{0}/{1}")
		public Object c(@Path String foo, @Path String bar) {
			return OMap.of("m", "numbers1", "0", foo, "1", bar);
		}
		@RestMethod(method=GET, path="/y/{0}/y/{1}/y")
		public Object d(@Path String foo, @Path String bar) {
			return OMap.of("m", "numbers2", "0", foo, "1", bar);
		}
		@RestMethod(method=GET, path="/z/{1}/z/{0}/z")
		public Object e(@Path String foo, @Path String bar) {
			return OMap.of("m", "numbers3", "0", foo, "1", bar);
		}
	}

	@Test
	public void e01_withoutName() throws Exception {
		RestClient e = MockRestClient.build(E.class);
		e.get("/x/x1/x2")
			.run()
			.assertBody().is("{m:'normal1',foo:'x1',bar:'x2'}");
		e.get("/x/x1/x/x2/x")
			.run()
			.assertBody().is("{m:'normal2',foo:'x1',bar:'x2'}");
		e.get("/y/y1/y2")
			.run()
			.assertBody().is("{m:'numbers1','0':'y1','1':'y2'}");
		e.get("/y/y1/y/y2/y")
			.run()
			.assertBody().is("{m:'numbers2','0':'y1','1':'y2'}");
		e.get("/z/z1/z/z2/z")
			.run()
			.assertBody().is("{m:'numbers3','0':'z2','1':'z1'}");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Path variables on class.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(path="/f/{a}/{b}")
	public static class F  {
		@RestMethod(method=GET, path="/")
		public String a(RequestPath path) {
			return format("a: {0}", path);
		}
		@RestMethod(method=GET, path="/*")
		public String b(RequestPath path) {
			return format("b: {0}", path);
		}
		@RestMethod(method=GET, path="/fc")
		public String c(RequestPath path) {
			return format("c: {0}", path);
		}
		@RestMethod(method=GET, path="/fd/{c}/{d}")
		public String d(RequestPath path) {
			return format("d: {0}", path);
		}
		@RestMethod(method=GET, path="/fe/{a}/{b}")
		public String e(RequestPath path) {
			return format("e: {0}", path);
		}
		@RestMethod(method=GET, path="/ff/{c}/{d}/*")
		public String f(RequestPath path) {
			return format("f: {0}", path);
		}
		private String format(String msg, Object...args) {
			return SimpleJson.DEFAULT.format(msg, args);
		}
	}

	@Test
	public void f01_pathVariablesOnClass() throws Exception {
		RestClient f = MockRestClient.createLax(F.class).servletPath("/f").build();
		f.get("http://localhost/f/x1/x2")
			.run()
			.assertBody().is("a: {a:'x1',b:'x2'}");
		f.get("http://localhost/f/x1")
			.run()
			.assertCode().is(404);
		f.get("http://localhost/f")
			.run()
			.assertCode().is(404);
		f.get("http://localhost/f//")
			.run()
			.assertCode().is(404);
		f.get("http://localhost/f/x/")
			.run()
			.assertCode().is(404);
		f.get("http://localhost/f//x")
			.run()
			.assertCode().is(404);
		f.get("http://localhost/f/x1/x2/foo")
			.run()
			.assertBody().is("b: {'/*':'foo','/**':'foo',a:'x1',b:'x2'}");
		f.get("http://localhost/f///foo")
			.run()
			.assertCode().is(404);
		f.get("http://localhost/f/x1//foo")
			.run()
			.assertCode().is(404);
		f.get("http://localhost/f//x2/foo")
			.run()
			.assertCode().is(404);
		f.get("http://localhost/f/x1/x2/fc")
			.run()
			.assertBody().is("c: {a:'x1',b:'x2'}");
		f.get("http://localhost/f///a")
			.run()
			.assertCode().is(404);
		f.get("http://localhost/f/x1//a")
			.run()
			.assertCode().is(404);
		f.get("http://localhost/f//x2/a")
			.run()
			.assertCode().is(404);
		f.get("http://localhost/f/x1/x2/fd/x3/x4")
			.run()
			.assertBody().is("d: {a:'x1',b:'x2',c:'x3',d:'x4'}");
		f.get("http://localhost/f//x2/b/x3/x4")
			.run()
			.assertCode().is(404);
		f.get("http://localhost/f/x1//b/x3/x4")
			.run()
			.assertCode().is(404);
		f.get("http://localhost/f/x1/x2/b//x4")
			.run()
			.assertCode().is(200);
		f.get("http://localhost/f/x1/x2/b/x3/")
			.run()
			.assertCode().is(200);
		f.get("http://localhost/f///b//")
			.run()
			.assertCode().is(404);
		f.get("http://localhost/f/x1/x2/fe/x3/x4")
			.run()
			.assertBody().is("e: {a:'x3',b:'x4'}");
		f.get("http://localhost/f/x1/x2/ff/x3/x4")
			.run()
			.assertBody().is("f: {a:'x1',b:'x2',c:'x3',d:'x4'}");
		f.get("http://localhost/f/x1/x2/ff/x3/x4/")
			.run()
			.assertBody().is("f: {'/*':'','/**':'',a:'x1',b:'x2',c:'x3',d:'x4'}");
		f.get("http://localhost/f/x1/x2/ff/x3/x4/foo/bar")
			.run()
			.assertBody().is("f: {'/*':'foo/bar','/**':'foo/bar',a:'x1',b:'x2',c:'x3',d:'x4'}");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Path variables on child class.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(children={F.class})
	public static class G {}


	@Test
	public void g01_pathVariablesOnChildClass() throws Exception {
		RestClient g = MockRestClient.buildLax(G.class);
		g.get("http://localhost/f/x1/x2")
			.run()
			.assertBody().is("a: {a:'x1',b:'x2'}");
		g.get("http://localhost/f/x1")
			.run()
			.assertCode().is(404);
		g.get("http://localhost/f")
			.run()
			.assertCode().is(404);
		g.get("http://localhost/f//")
			.run()
			.assertCode().is(404);
		g.get("http://localhost/f/x1/")
			.run()
			.assertCode().is(404);
		g.get("http://localhost/f//x2")
			.run()
			.assertCode().is(404);
		g.get("http://localhost/f/x1/x2/fc")
			.run()
			.assertBody().is("c: {a:'x1',b:'x2'}");
		g.get("http://localhost/f///a")
			.run()
			.assertCode().is(404);
		g.get("http://localhost/f/x1//a")
			.run()
			.assertCode().is(404);
		g.get("http://localhost/f//x2/a")
			.run()
			.assertCode().is(404);
		g.get("http://localhost/f/x1/x2/fd/x3/x4")
			.run()
			.assertBody().is("d: {a:'x1',b:'x2',c:'x3',d:'x4'}");
		g.get("http://localhost/f//x2/b/x3/x4")
			.run()
			.assertCode().is(404);
		g.get("http://localhost/f/x1//b/x3/x4")
			.run()
			.assertCode().is(404);
		g.get("http://localhost/f/x1/x2/b//x4")
			.run()
			.assertCode().is(200);
		g.get("http://localhost/f/x1/x2/b/x3/")
			.run()
			.assertCode().is(200);
		g.get("http://localhost/f///b//")
			.run()
			.assertCode().is(404);
		g.get("http://localhost/f/x1/x2/fe/x3/x4")
			.run()
			.assertBody().is("e: {a:'x3',b:'x4'}");
		g.get("http://localhost/f/x1/x2/ff/x3/x4")
			.run()
			.assertBody().is("f: {a:'x1',b:'x2',c:'x3',d:'x4'}");
		g.get("http://localhost/f/x1/x2/ff/x3/x4/")
			.run()
			.assertBody().is("f: {'/*':'','/**':'',a:'x1',b:'x2',c:'x3',d:'x4'}");
		g.get("http://localhost/f/x1/x2/ff/x3/x4/foo/bar")
			.run()
			.assertBody().is("f: {'/*':'foo/bar','/**':'foo/bar',a:'x1',b:'x2',c:'x3',d:'x4'}");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Path variables on parent and child class.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(path="/h/{ha}/{hb}", children={F.class})
	public static class H {}

	@Test
	public void h01_pathVariablesOnParentAndChildClass() throws Exception {
		RestClient h = MockRestClient.createLax(H.class).servletPath("/h").build();
		h.get("http://localhost/h/ha1/hb1/f/x1/x2")
			.run()
			.assertBody().is("a: {a:'x1',b:'x2',ha:'ha1',hb:'hb1'}");
		// These are 405 instead of 404 because when children don't match, we try to find a matching Java method.
		h.get("http://localhost/h/ha1/hb1/f/x1")
			.run()
			.assertCode().is(404);
		h.get("http://localhost/h/ha1/hb1/f")
			.run()
			.assertCode().is(404);
		h.get("http://localhost/h/ha1/hb1")
			.run()
			.assertCode().is(404);
		h.get("http://localhost/h/ha1")
			.run()
			.assertCode().is(404);
		h.get("http://localhost/h")
			.run()
			.assertCode().is(404);
		h.get("http://localhost/h//hb1/f/x1/x2")
			.run()
			.assertCode().is(404);
		h.get("http://localhost/h/ha1//f/x1/x2")
			.run()
			.assertCode().is(404);
		h.get("http://localhost/h/ha1/hb1/f//x2")
			.run()
			.assertCode().is(404);
		h.get("http://localhost/h/ha1/hb1/f/x1/")
			.run()
			.assertCode().is(404);
		h.get("http://localhost/h///f//")
			.run()
			.assertCode().is(404);
		h.get("http://localhost/h/ha1/hb1/f/x1/x2/foo")
			.run()
			.assertBody().is("b: {'/*':'foo','/**':'foo',a:'x1',b:'x2',ha:'ha1',hb:'hb1'}");
		h.get("http://localhost/h//hb1/f/x1/x2/foo")
			.run()
			.assertCode().is(404);
		h.get("http://localhost/h/ha1//f/x1/x2/foo")
			.run()
			.assertCode().is(404);
		h.get("http://localhost/h/ha1/hb1/f//x2/foo")
			.run()
			.assertCode().is(404);
		h.get("http://localhost/h/ha1/hb1/f/x1//foo")
			.run()
			.assertCode().is(404);
		h.get("http://localhost/h///f///foo")
			.run()
			.assertCode().is(404);
		h.get("http://localhost/h/ha1/hb1/f/x1/x2/fc")
			.run()
			.assertBody().is("c: {a:'x1',b:'x2',ha:'ha1',hb:'hb1'}");
		h.get("http://localhost/h//hb1/f/x1/x2/a")
			.run()
			.assertCode().is(404);
		h.get("http://localhost/h/ha1//f/x1/x2/a")
			.run()
			.assertCode().is(404);
		h.get("http://localhost/h/ha1/hb1/f//x2/a")
			.run()
			.assertCode().is(404);
		h.get("http://localhost/h/ha1/hb1/f/x1//a")
			.run()
			.assertCode().is(404);
		h.get("http://localhost/h///f///a")
			.run()
			.assertCode().is(404);
		h.get("http://localhost/h/ha1/hb1/f/x1/x2/fd/x3/x4")
			.run()
			.assertBody().is("d: {a:'x1',b:'x2',c:'x3',d:'x4',ha:'ha1',hb:'hb1'}");
		h.get("http://localhost/h/ha1/hb1/f/x1/x2/fe/x3/x4")
			.run()
			.assertBody().is("e: {a:'x3',b:'x4',ha:'ha1',hb:'hb1'}");
		h.get("http://localhost/h/ha1/hb1/f/x1/x2/ff/x3/x4")
			.run()
			.assertBody().is("f: {a:'x1',b:'x2',c:'x3',d:'x4',ha:'ha1',hb:'hb1'}");
		h.get("http://localhost/h/ha1/hb1/f/x1/x2/ff/x3/x4/")
			.run()
			.assertBody().is("f: {'/*':'','/**':'',a:'x1',b:'x2',c:'x3',d:'x4',ha:'ha1',hb:'hb1'}");
		h.get("http://localhost/h/ha1/hb1/f/x1/x2/ff/x3/x4/foo/bar")
			.run()
			.assertBody().is("f: {'/*':'foo/bar','/**':'foo/bar',a:'x1',b:'x2',c:'x3',d:'x4',ha:'ha1',hb:'hb1'}");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Path variables on parents and child class.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(path="/i/{ia}/{ib}", children={H.class})
	public static class I {}

	@Test
	public void i01_pathVariablesOnParentAndChildClass() throws Exception {
		RestClient i = MockRestClient.createLax(I.class).servletPath("/i").build();
		i.get("http://localhost/i/ia1/ib1/h/ha1/hb1/f/x1/x2")
			.run()
			.assertBody().is("a: {a:'x1',b:'x2',ha:'ha1',hb:'hb1',ia:'ia1',ib:'ib1'}");
		i.get("http://localhost/i/ia1/ib1/h/ha1/hb1/f/x1")
			.run()
			.assertCode().is(404);
		i.get("http://localhost/i/ia1/ib1/h/ha1/hb1/f")
			.run()
			.assertCode().is(404);
		i.get("http://localhost/i/ia1/ib1/h/ha1/hb1")
			.run()
			.assertCode().is(404);
		i.get("http://localhost/i/ia1/ib1/h/ha1")
			.run()
			.assertCode().is(404);
		i.get("http://localhost/i/ia1/ib1/h")
			.run()
			.assertCode().is(404);
		i.get("http://localhost/i/ia1/ib1")
			.run()
			.assertCode().is(404);
		i.get("http://localhost/i/ia1")
			.run()
			.assertCode().is(404);
		i.get("http://localhost/i")
			.run()
			.assertCode().is(404);
		i.get("http://localhost/i//ib1/h/ha1/hb1/f/x1/x2")
			.run()
			.assertCode().is(404);
		i.get("http://localhost/i/ia1//h/ha1/hb1/f/x1/x2")
			.run()
			.assertCode().is(404);
		i.get("http://localhost/i/ia1/ib1/h//hb1/f/x1/x2")
			.run()
			.assertCode().is(404);
		i.get("http://localhost/i/ia1/ib1/h/ha1//f/x1/x2")
			.run()
			.assertCode().is(404);
		i.get("http://localhost/i/ia1/ib1/h/ha1/hb1/f//x2")
			.run()
			.assertCode().is(404);
		i.get("http://localhost/i/ia1/ib1/h/ha1/hb1/f/x1/")
			.run()
			.assertCode().is(404);
		i.get("http://localhost/i///h///f//")
			.run()
			.assertCode().is(404);
		i.get("http://localhost/i/ia1/ib1/h/ha1/hb1/f/x1/x2/foo")
			.run()
			.assertBody().is("b: {'/*':'foo','/**':'foo',a:'x1',b:'x2',ha:'ha1',hb:'hb1',ia:'ia1',ib:'ib1'}");
		i.get("http://localhost/i/ia1/ib1/h/ha1/hb1/f/x1/x2/fc")
			.run()
			.assertBody().is("c: {a:'x1',b:'x2',ha:'ha1',hb:'hb1',ia:'ia1',ib:'ib1'}");
		i.get("http://localhost/i/ia1/ib1/h/ha1/hb1/f/x1/x2/fd/x3/x4")
			.run()
			.assertBody().is("d: {a:'x1',b:'x2',c:'x3',d:'x4',ha:'ha1',hb:'hb1',ia:'ia1',ib:'ib1'}");
		i.get("http://localhost/i/ia1/ib1/h/ha1/hb1/f/x1/x2/fe/x3/x4")
			.run()
			.assertBody().is("e: {a:'x3',b:'x4',ha:'ha1',hb:'hb1',ia:'ia1',ib:'ib1'}");
		i.get("http://localhost/i/ia1/ib1/h/ha1/hb1/f/x1/x2/ff/x3/x4")
			.run()
			.assertBody().is("f: {a:'x1',b:'x2',c:'x3',d:'x4',ha:'ha1',hb:'hb1',ia:'ia1',ib:'ib1'}");
		i.get("http://localhost/i/ia1/ib1/h/ha1/hb1/f/x1/x2/ff/x3/x4/")
			.run()
			.assertBody().is("f: {'/*':'','/**':'',a:'x1',b:'x2',c:'x3',d:'x4',ha:'ha1',hb:'hb1',ia:'ia1',ib:'ib1'}");
		i.get("http://localhost/i/ia1/ib1/h/ha1/hb1/f/x1/x2/ff/x3/x4/foo/bar")
			.run()
			.assertBody().is("f: {'/*':'foo/bar','/**':'foo/bar',a:'x1',b:'x2',c:'x3',d:'x4',ha:'ha1',hb:'hb1',ia:'ia1',ib:'ib1'}");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Optional path parameter.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(serializers=SimpleJsonSerializer.class)
	public static class J {
		@RestMethod(method=GET,path="/a/{f1}")
		public Object a(@Path("f1") Optional<Integer> f1) throws Exception {
			assertNotNull(f1);
			return f1;
		}
		@RestMethod(method=GET,path="/b/{f1}")
		public Object b(@Path("f1") Optional<ABean> f1) throws Exception {
			assertNotNull(f1);
			return f1;
		}
		@RestMethod(method=GET,path="/c/{f1}")
		public Object c(@Path("f1") Optional<List<ABean>> f1) throws Exception {
			assertNotNull(f1);
			return f1;
		}
		@RestMethod(method=GET,path="/d/{f1}")
		public Object d(@Path("f1") List<Optional<ABean>> f1) throws Exception {
			return f1;
		}
	}

	@Test
	public void j01_optionalParam() throws Exception {
		RestClient j = MockRestClient.buildJson(J.class);
		j.get("/a/123")
			.run()
			.assertCode().is(200)
			.assertBody().is("123");
		j.get("/b/a=1,b=foo")
			.run()
			.assertCode().is(200)
			.assertBody().is("{a:1,b:'foo'}");
		j.get("/c/@((a=1,b=foo))")
			.run()
			.assertCode().is(200)
			.assertBody().is("[{a:1,b:'foo'}]");
		j.get("/d/@((a=1,b=foo))")
			.run()
			.assertCode().is(200)
			.assertBody().is("[{a:1,b:'foo'}]");
	}


	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	@Rest(path="/k1/{k1}",children=K2.class)
	public static class K1 {}

	@Rest(path="/k2")
	public static class K2 {
		@RestMethod(path="/")
		public String a(@Path(n="k1",r=false) String k1) {
			return k1 == null ? "nil" : k1;
		}
		@RestMethod(path="/foo/{bar}")
		public String b(@Path(n="k1",r=false) String k1, @Path(n="bar",r=false) String bar) {
			return (k1 == null ? "nil" : k1) + "," + (bar == null ? "nil" : bar);
		}
	}

	@Test
	public void k01_basic() throws Exception {
		RestClient k1 = MockRestClient.build(K1.class);
		RestClient k2 = MockRestClient.build(K2.class);

		k1.get("http://localhost/k1/foo/k2")
			.run()
			.assertCode().is(200)
			.assertBody().is("foo");
		k1.get("http://localhost/k1/foo/k2/foo/xxx")
			.run()
			.assertCode().is(200)
			.assertBody().is("foo,xxx");
		k2.get("/")
			.run()
			.assertCode().is(200)
			.assertBody().is("nil");
		k2.get("/foo/xxx")
			.run()
			.assertCode().is(200)
			.assertBody().is("nil,xxx");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Multiple paths
	//------------------------------------------------------------------------------------------------------------------

	@Rest(path="/l1/{l1}",children=L2.class)
	public static class L1 {}

	@Rest(path="/l2")
	public static class L2 {
		@RestMethod(paths={"/","/{foo}"})
		public String a(@Path(n="l1",r=false) String l1, @Path(n="foo",r=false) String foo) {
			return "1," + (l1 == null ? "nil" : l1) + "," + (foo == null ? "nil" : foo);
		}
		@RestMethod(paths={"/foo","/foo/{foo}"})
		public String b(@Path(n="l1",r=false) String l1, @Path(n="foo",r=false) String foo) {
			return "2," + (l1 == null ? "nil" : l1) + "," + (foo == null ? "nil" : foo);
		}
	}

	@Test
	public void l01_multiplePaths() throws Exception {
		RestClient l1 = MockRestClient.build(L1.class);
		RestClient l2 = MockRestClient.build(L2.class);

		l1.get("http://localhost/l1/l1foo/l2")
			.run()
			.assertCode().is(200)
			.assertBody().is("1,l1foo,nil");
		l1.get("http://localhost/l1/l1foo/l2/l2foo")
			.run()
			.assertCode().is(200)
			.assertBody().is("1,l1foo,l2foo");
		l1.get("http://localhost/l1/l1foo/l2/foo")
			.run()
			.assertCode().is(200)
			.assertBody().is("2,l1foo,nil");
		l1.get("http://localhost/l1/l1foo/l2/foo/l2foo")
			.run()
			.assertCode().is(200)
			.assertBody().is("2,l1foo,l2foo");
		l2.get("http://localhost/l2")
			.run()
			.assertCode().is(200)
			.assertBody().is("1,nil,nil");
		l2.get("http://localhost/l2/l2foo")
			.run()
			.assertCode().is(200)
			.assertBody().is("1,nil,l2foo");
		l2.get("http://localhost/l2/foo")
			.run()
			.assertCode().is(200)
			.assertBody().is("2,nil,nil");
		l2.get("http://localhost/l2/foo/l2foo")
			.run()
			.assertCode().is(200)
			.assertBody().is("2,nil,l2foo");
	}
}