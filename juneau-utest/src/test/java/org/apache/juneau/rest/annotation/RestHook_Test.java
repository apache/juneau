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

import static org.apache.juneau.rest.annotation.HookEvent.*;
import static org.junit.runners.MethodSorters.*;

import java.io.IOException;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.annotation.Body;
import org.apache.juneau.http.header.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.RestRequest;
import org.apache.juneau.rest.RestResponse;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.serializer.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RestHook_Test {

	//------------------------------------------------------------------------------------------------------------------
	// @RestHook(PRE_CALL)
	//------------------------------------------------------------------------------------------------------------------

	@Rest(
		parsers=A1.class,
		defaultRequestAttributes={
			"p1:sp1", // Unchanged servlet-level property.
			"p2:sp2", // Servlet-level property overridden by onPreCall.
			"p3:sp3", // Servlet-level property overridded by method.
			"p4:sp4"  // Servlet-level property overridden by method then onPreCall.
		}
	)
	public static class A {

		@RestHook(PRE_CALL)
		public void onPreCall(RestRequest req) {
			RequestAttributes attrs = req.getAttributes();
			attrs.put("p2", "xp2");
			attrs.put("p4", "xp4");
			attrs.put("p5", "xp5"); // New property
			String overrideContentType = req.getHeader("Override-Content-Type");
			if (overrideContentType != null)
				req.getRequestHeaders().put("Content-Type", overrideContentType);
		}

		@RestPut(
			defaultRequestAttributes={
				"p3:mp3",
				"p4:mp4"
			}
		)
		public String a(@Body String in) {
			return in;
		}

		@RestPut
		public String b(RestRequest req, RequestAttributes attrs) throws Exception {
			attrs.put("p3", "pp3");
			attrs.put("p4", "pp4");
			return req.getBody().asType(String.class);
		}
	}

	public static class A1 extends ReaderParser {
		public A1(ContextProperties cp) {
			super(cp, "text/a1", "text/a2", "text/a3");
		}
		@Override /* Parser */
		public ReaderParserSession createSession(ParserSessionArgs args) {
			return new ReaderParserSession(args) {
				@Override /* ParserSession */
				@SuppressWarnings("unchecked")
				protected <T> T doParse(ParserPipe pipe, ClassMeta<T> type) throws IOException, ParseException, ExecutableException {
					SessionProperties sp = getSessionProperties();
					return (T)("p1="+sp.get("p1").orElse(null)+",p2="+sp.get("p2").orElse(null)+",p3="+sp.get("p3").orElse(null)+",p4="+sp.get("p4").orElse(null)+",p5="+sp.get("p5").orElse(null));
				}
			};
		}
	}

	@Test
	public void a01_preCall() throws Exception {
		RestClient a = MockRestClient.build(A.class);
		a.put("/a", null).contentType("text/a1").run().assertBody().is("p1=sp1,p2=xp2,p3=mp3,p4=xp4,p5=xp5");
		a.put("/a", null).contentType("text/a1").header("Override-Content-Type", "text/a2").run().assertBody().is("p1=sp1,p2=xp2,p3=mp3,p4=xp4,p5=xp5");
		a.put("/b", null).contentType("text/a1").run().assertBody().is("p1=sp1,p2=xp2,p3=pp3,p4=pp4,p5=xp5");
		a.put("/b", null).contentType("text/a1").header("Override-Content-Type", "text/a2").run().assertBody().is("p1=sp1,p2=xp2,p3=pp3,p4=pp4,p5=xp5");
	}

	//------------------------------------------------------------------------------------------------------------------
	// @RestHook(POST_CALL)
	//------------------------------------------------------------------------------------------------------------------

	@Rest(
		serializers=B1.class,
		defaultRequestAttributes={
			"p1:sp1", // Unchanged servlet-level property.
			"p2:sp2", // Servlet-level property overridden by onPostCall.
			"p3:sp3", // Servlet-level property overridded by method.
			"p4:sp4"  // Servlet-level property overridden by method then onPostCall.
		}
	)
	public static class B {

		@RestHook(POST_CALL)
		public void onPostCall(RestRequest req, RestResponse res) {
			RequestAttributes attrs = req.getAttributes();
			attrs.put("p2", "xp2");
			attrs.put("p4", "xp4");
			attrs.put("p5", "xp5"); // New property
			String overrideAccept = req.getHeader("Override-Accept");
			if (overrideAccept != null)
				req.getRequestHeaders().put("Accept", overrideAccept);
			String overrideContentType = req.getHeader("Override-Content-Type");
			if (overrideContentType != null)
				attrs.put("Override-Content-Type", overrideContentType);
		}

		@RestPut(
			defaultRequestAttributes={
				"p3:mp3",
				"p4:mp4"
			},
			defaultRequestHeaders="Accept: text/s2"
		)
		public String a() {
			return null;
		}

		@RestPut
		public String b(RestRequest req, RequestAttributes attrs) throws Exception {
			attrs.put("p3", "pp3");
			attrs.put("p4", "pp4");
			String accept = req.getHeader("Accept");
			if (accept == null || accept.isEmpty())
				req.getRequestHeaders().put("Accept", "text/s2");
			return null;
		}
	}

	public static class B1 extends WriterSerializer {
		public B1(ContextProperties cp) {
			super(cp, "test/s1", "text/s1,text/s2,text/s3");
		}
		@Override /* Serializer */
		public WriterSerializerSession createSession(SerializerSessionArgs args) {
			return new WriterSerializerSession(args) {
				@Override /* SerializerSession */
				protected void doSerialize(SerializerPipe out, Object o) throws IOException, SerializeException {
					SessionProperties sp = getSessionProperties();
					out.getWriter().write("p1="+sp.get("p1").orElse(null)+",p2="+sp.get("p2").orElse(null)+",p3="+sp.get("p3").orElse(null)+",p4="+sp.get("p4").orElse(null)+",p5="+sp.get("p5").orElse(null));
				}
				@Override /* SerializerSession */
				public Map<String,String> getResponseHeaders() {
					SessionProperties sp = getSessionProperties();
					if (sp.contains("Override-Content-Type"))
						return AMap.of("Content-Type",sp.getString("Override-Content-Type").orElse(null));
					return Collections.emptyMap();
				}
			};
		}
	}

	@Test
	public void b01_postCall() throws Exception {
		RestClient b = MockRestClient.build(B.class);
		b.put("/a", null).accept("text/s1").run().assertBody().is("p1=sp1,p2=xp2,p3=mp3,p4=xp4,p5=xp5");
		b.put("/a", null).accept("text/s1").header("Override-Accept", "text/s2").run().assertBody().is("p1=sp1,p2=xp2,p3=mp3,p4=xp4,p5=xp5");
		b.put("/a", null).accept("text/s1").header("Override-Content-Type", "text/s3").run().assertBody().is("p1=sp1,p2=xp2,p3=mp3,p4=xp4,p5=xp5");
		b.put("/a", null).run().assertBody().is("p1=sp1,p2=xp2,p3=mp3,p4=xp4,p5=xp5");
		b.put("/a", null).header("Override-Accept", "text/s3").run().assertBody().is("p1=sp1,p2=xp2,p3=mp3,p4=xp4,p5=xp5");
		b.put("/a", null).header("Override-Content-Type", "text/s3").run().assertBody().is("p1=sp1,p2=xp2,p3=mp3,p4=xp4,p5=xp5");
		b.put("/b", null).accept("text/s1").run().assertBody().is("p1=sp1,p2=xp2,p3=pp3,p4=xp4,p5=xp5");
		b.put("/b", null).accept("text/s1").header("Override-Accept", "text/s2").run().assertBody().is("p1=sp1,p2=xp2,p3=pp3,p4=xp4,p5=xp5");
		b.put("/b", null).accept("text/s1").header("Override-Content-Type", "text/s3").run().assertBody().is("p1=sp1,p2=xp2,p3=pp3,p4=xp4,p5=xp5");
		b.put("/b", null).run().assertBody().is("p1=sp1,p2=xp2,p3=pp3,p4=xp4,p5=xp5");
		b.put("/b", null).header("Override-Accept", "text/s3").run().assertBody().is("p1=sp1,p2=xp2,p3=pp3,p4=xp4,p5=xp5");
		b.put("/b", null).header("Override-Content-Type", "text/s3").run().assertBody().is("p1=sp1,p2=xp2,p3=pp3,p4=xp4,p5=xp5");
	}

	//------------------------------------------------------------------------------------------------------------------
	// @RestHook(INIT)
	//------------------------------------------------------------------------------------------------------------------

	@Rest(children={C_Super.class,C_Sub.class})
	public static class C {}

	@Rest(path="/super")
	public static class C_Super {
		protected OList events = new OList();
		@RestHook(INIT)
		public void init1c(RestContextBuilder builder) {
			events.add("super-1c");
		}
		@RestHook(INIT)
		public void init1a(ServletConfig config) {
			events.add("super-1a");
		}
		@RestHook(INIT)
		public void init1b() {
			events.add("super-1b");
		}
		@RestHook(INIT)
		public void init2a() {
			events.add("super-2a");
		}
		@RestGet
		public OList getEvents() {
			return events;
		}
	}

	@Rest(path="/sub", children={C_Child.class})
	public static class C_Sub extends C_Super {
		@Override
		@RestHook(INIT)
		public void init1c(RestContextBuilder builder) {
			events.add("sub-1c");
		}
		@Override
		@RestHook(INIT)
		public void init1a(ServletConfig config) {
			events.add("sub-1a");
		}
		@Override
		@RestHook(INIT)
		public void init1b() {
			events.add("sub-1b");
		}
		@RestHook(INIT)
		public void init2b() {
			events.add("sub-2b");
		}
	}

	@Rest(path="/child")
	public static class C_Child extends C_Super {
		@Override
		@RestHook(INIT)
		public void init1c(RestContextBuilder builder) {
			events.add("child-1c");
		}
		@RestHook(INIT)
		public void init2b() {
			events.add("child-2b");
		}
	}

	@Test
	public void c01_init() throws Exception {
		RestClient c = MockRestClient.build(C.class);
		c.get("/super/events").run().assertBody().is("['super-1a','super-1b','super-1c','super-2a']");
		c.get("/sub/events").run().assertBody().is("['sub-1a','sub-1b','sub-1c','super-2a','sub-2b']");
		c.get("/sub/child/events").run().assertBody().is("['super-1a','super-1b','child-1c','super-2a','child-2b']");
	}

	//------------------------------------------------------------------------------------------------------------------
	// @RestHook(POST_INIT)
	//------------------------------------------------------------------------------------------------------------------
	@Rest(children={D_Super.class,D_Sub.class})
	public static class D {}

	@Rest(path="/super")
	public static class D_Super {
		protected OList events = new OList();
		@RestHook(POST_INIT)
		public void postInit1c(RestContext context) {
			events.add("super-1c");
		}
		@RestHook(POST_INIT)
		public void postInit1a(RestContext context) {
			events.add("super-1a");
		}
		@RestHook(POST_INIT)
		public void postInit1b() {
			events.add("super-1b");
		}
		@RestHook(POST_INIT)
		public void postInit2a() {
			events.add("super-2a");
		}
		@RestGet
		public OList getEvents() {
			return events;
		}
	}

	@Rest(path="/sub",children={D_Child.class})
	public static class D_Sub extends D_Super {
		protected static String LAST_CALLED;
		@Override
		@RestHook(POST_INIT)
		public void postInit1c(RestContext context) {
			events.add("sub-1c");
		}
		@Override
		@RestHook(POST_INIT)
		public void postInit1a(RestContext context) {
			events.add("sub-1a");
		}
		@Override
		@RestHook(POST_INIT)
		public void postInit1b() {
			events.add("sub-1b");
		}
		@RestHook(POST_INIT)
		public void postInit2b() {
			events.add("sub-2b");
		}
		@RestHook(POST_INIT)
		public void postInitOrderTestSub() {
			LAST_CALLED = "PARENT";
		}
		@RestGet
		public String getLastCalled() {
			return LAST_CALLED;
		}
	}

	@Rest(path="/child")
	public static class D_Child extends D_Super {
		@Override
		@RestHook(POST_INIT)
		public void postInit1c(RestContext context) {
			events.add("child-1c");
		}
		@RestHook(POST_INIT)
		public void postInit2b() {
			events.add("child-2b");
		}
		@RestHook(POST_INIT)
		public void postInitOrderTestSub() {
			D_Sub.LAST_CALLED = "CHILD";
		}
	}

	@Test
	public void d01_postInit() throws Exception {
		RestClient d = MockRestClient.build(D.class);
		d.get("/super/events").run().assertBody().is("['super-1a','super-1b','super-1c','super-2a']");
		d.get("/sub/events").run().assertBody().is("['sub-1a','sub-1b','sub-1c','super-2a','sub-2b']");
		d.get("/sub/child/events").run().assertBody().is("['super-1a','super-1b','child-1c','super-2a','child-2b']");
		d.get("/sub/lastCalled").run().assertBody().is("CHILD");
	}

	//------------------------------------------------------------------------------------------------------------------
	// @RestHook(POST_INIT_CHILD_FIRST)
	//------------------------------------------------------------------------------------------------------------------

	@Rest(
		children={
			E_Super.class,
			E_Sub.class
		}
	)
	public static class E {}

	@Rest(path="/super")
	public static class E_Super {
		protected OList events = new OList();
		@RestHook(POST_INIT_CHILD_FIRST)
		public void postInitChildFirst1c(RestContext context) {
			events.add("super-1c");
		}
		@RestHook(POST_INIT_CHILD_FIRST)
		public void postInitChildFirst1a(RestContext context) {
			events.add("super-1a");
		}
		@RestHook(POST_INIT_CHILD_FIRST)
		public void postInitChildFirst1b() {
			events.add("super-1b");
		}
		@RestHook(POST_INIT_CHILD_FIRST)
		public void postInitChildFirst2a() {
			events.add("super-2a");
		}
		@RestGet
		public OList getPostInitChildFirstEvents() {
			return events;
		}
	}

	@Rest(path="/sub", children={E_Child.class})
	public static class E_Sub extends E_Super {
		protected static String LAST_CALLED;
		@Override
		@RestHook(POST_INIT_CHILD_FIRST)
		public void postInitChildFirst1c(RestContext context) {
			events.add("sub-1c");
		}
		@Override
		@RestHook(POST_INIT_CHILD_FIRST)
		public void postInitChildFirst1a(RestContext context) {
			events.add("sub-1a");
		}
		@Override
		@RestHook(POST_INIT_CHILD_FIRST)
		public void postInitChildFirst1b() {
			events.add("sub-1b");
		}
		@RestHook(POST_INIT_CHILD_FIRST)
		public void postInitChildFirst2b() {
			events.add("sub-2b");
		}
		@RestHook(POST_INIT_CHILD_FIRST)
		public void postInitChildFirstOrderTestSub() {
			LAST_CALLED = "PARENT";
		}
		@RestGet
		public String getLastCalled() {
			return LAST_CALLED;
		}
	}

	@Rest(path="/child")
	public static class E_Child extends E_Super {
		@Override
		@RestHook(POST_INIT_CHILD_FIRST)
		public void postInitChildFirst1c(RestContext context) {
			events.add("child-1c");
		}
		@RestHook(POST_INIT_CHILD_FIRST)
		public void postInitChildFirst2b() {
			events.add("child-2b");
		}
		@RestHook(POST_INIT_CHILD_FIRST)
		public void postInitChildFirstOrderTestSub() {
			E_Sub.LAST_CALLED = "CHILD";
		}
	}

	@Test
	public void e01_postInitChildFirst() throws Exception {
		RestClient e = MockRestClient.build(E.class);
		e.get("/super/postInitChildFirstEvents").run().assertBody().is("['super-1a','super-1b','super-1c','super-2a']");
		e.get("/sub/postInitChildFirstEvents").run().assertBody().is("['sub-1a','sub-1b','sub-1c','super-2a','sub-2b']");
		e.get("/sub/child/postInitChildFirstEvents").run().assertBody().is("['super-1a','super-1b','child-1c','super-2a','child-2b']");
		e.get("/sub/lastCalled").run().assertBody().is("PARENT");
	}

	//------------------------------------------------------------------------------------------------------------------
	// @RestHook(START_CALL)
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class F extends F_Parent {
		private boolean start3Called;
		@RestHook(START_CALL)
		public void start3() {
			start3Called = true;
		}
		@RestHook(START_CALL)
		public void start4(HttpServletRequest req, HttpServletResponse res) {
			res.setHeader("start3-called", ""+start3Called);
			start3Called = false;
			if (res.getHeader("start4-called") != null)
				throw new RuntimeException("start4 called multiple times.");
			res.setHeader("start4-called", "true");
		}
		@RestGet(path="/")
		public OMap a(RestRequest req, RestResponse res) {
			return OMap.create()
				.a("1", res.getHeader("start1-called"))
				.a("2", res.getHeader("start2-called"))
				.a("3", res.getHeader("start3-called"))
				.a("4", res.getHeader("start4-called"));
		}
	}

	public static class F_Parent {
		private boolean start1Called;
		@RestHook(START_CALL)
		public void start1() {
			start1Called = true;
		}
		@RestHook(START_CALL)
		public void start2(HttpServletRequest req, HttpServletResponse res) {
			res.setHeader("start1-called", ""+start1Called);
			start1Called = false;
			if (res.getHeader("start2-called") != null)
				throw new RuntimeException("start2 called multiple times.");
			res.setHeader("start2-called", "true");
		}
	}

	@Test
	public void f01_startCall() throws Exception {
		RestClient f = MockRestClient.build(F.class);
		f.get("/").run().assertBody().is("{'1':'true','2':'true','3':'true','4':'true'}");
	}

	//------------------------------------------------------------------------------------------------------------------
	// @RestHook(PRE_CALL)
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class G extends G_Parent {
		private boolean pre3Called;
		@RestHook(PRE_CALL)
		public void pre3() {
			pre3Called = true;
		}
		@RestHook(PRE_CALL)
		public void pre4(HttpServletRequest req, HttpServletResponse res) {
			res.setHeader("pre3-called", ""+pre3Called);
			pre3Called = false;
			if (res.getHeader("pre4-called") != null)
				throw new RuntimeException("pre4 called multiple times.");
			res.setHeader("pre4-called", "true");
		}
		@RestGet(path="/")
		public OMap a(RestRequest req, RestResponse res) {
			return OMap.create()
				.a("1", res.getHeader("pre1-called"))
				.a("2", res.getHeader("pre2-called"))
				.a("3", res.getHeader("pre3-called"))
				.a("4", res.getHeader("pre4-called"));
		}
	}

	public static class G_Parent {
		private boolean pre1Called;
		@RestHook(PRE_CALL)
		public void pre1() {
			pre1Called = true;
		}
		@RestHook(PRE_CALL)
		public void pre2(Accept accept, RestRequest req, RestResponse res) {
			res.setHeader("pre1-called", ""+pre1Called);
			pre1Called = false;
			if (res.getHeader("pre2-called") != null)
				throw new RuntimeException("pre2 called multiple times.");
			res.setHeader("pre2-called", "true");
		}
	}

	@Test
	public void g01_preCall() throws Exception {
		RestClient g = MockRestClient.build(G.class);
		g.get("/").run().assertBody().is("{'1':'true','2':'true','3':'true','4':'true'}");
	}

	//------------------------------------------------------------------------------------------------------------------
	// @RestHook(POST_CALL)
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class H extends H_Parent {
		private boolean post3Called;
		@RestHook(POST_CALL)
		public void post3() {
			post3Called = true;
		}
		@RestHook(POST_CALL)
		public void post4(HttpServletRequest req, HttpServletResponse res) {
			res.setHeader("post3-called", ""+post3Called);
			post3Called = false;
			if (res.getHeader("post4-called") != null)
				throw new RuntimeException("post4 called multiple times.");
			res.setHeader("post4-called", "true");
		}
		@RestGet(path="/")
		public String a() {
			return "OK";
		}
	}

	public static class H_Parent {
		private boolean post1Called;
		@RestHook(POST_CALL)
		public void post1() {
			post1Called = true;
		}
		@RestHook(POST_CALL)
		public void post2(Accept accept, RestRequest req, RestResponse res) {
			res.setHeader("post1-called", ""+post1Called);
			post1Called = false;
			if (res.getHeader("post2-called") != null)
				throw new RuntimeException("post2 called multiple times.");
			res.setHeader("post2-called", "true");
		}
	}

	@Test
	public void h01_postCall() throws Exception {
		RestClient h = MockRestClient.build(H.class);
		h.get("/").run()
			.assertStringHeader("post1-called").is("true")
			.assertStringHeader("post2-called").is("true")
			.assertStringHeader("post3-called").is("true")
			.assertStringHeader("post4-called").is("true");
	}
}
