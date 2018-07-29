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
import static org.apache.juneau.rest.annotation.HookEvent.*;

import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.utils.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests aspects of @RestHook.
 */
@SuppressWarnings({"javadoc"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RestHookTest {

	//=================================================================================================================
	// @RestHook(PRE_CALL)
	//=================================================================================================================

	@RestResource(
		parsers=A01.class,
		properties={
			@Property(name="p1",value="sp1"), // Unchanged servlet-level property.
			@Property(name="p2",value="sp2"), // Servlet-level property overridden by onPreCall.
			@Property(name="p3",value="sp3"), // Servlet-level property overridded by method.
			@Property(name="p4",value="sp4")  // Servlet-level property overridden by method then onPreCall.
		}
	)
	public static class A {

		@RestHook(PRE_CALL)
		public void onPreCall(RestRequest req) {
			RequestProperties properties = req.getProperties();
			properties.put("p2", "xp2");
			properties.put("p4", "xp4");
			properties.put("p5", "xp5"); // New property
			String overrideContentType = req.getHeader("Override-Content-Type");
			if (overrideContentType != null)
				req.getHeaders().put("Content-Type", overrideContentType);
		}

		@RestMethod(name=PUT, path="/propertiesOverriddenByAnnotation",
			properties={
				@Property(name="p3",value="mp3"),
				@Property(name="p4",value="mp4")
			}
		)
		public String a01(@Body String in) {
			return in;
		}

		@RestMethod(name=PUT, path="/propertiesOverriddenProgrammatically")
		public String a02(RestRequest req, RequestProperties properties) throws Exception {
			properties.put("p3", "pp3");
			properties.put("p4", "pp4");
			return req.getBody().asType(String.class);
		}
	}
	static MockRest a = MockRest.create(A.class);

	public static class A01 extends ReaderParser {
		public A01(PropertyStore ps) {
			super(ps, "text/a1", "text/a2", "text/a3");
		}
		@Override /* Parser */
		public ReaderParserSession createSession(ParserSessionArgs args) {
			return new ReaderParserSession(args) {
				@Override /* ParserSession */
				@SuppressWarnings("unchecked")
				protected <T> T doParse(ParserPipe pipe, ClassMeta<T> type) throws Exception {
					String matchingContentType = getProperty("mediaType", String.class);
					return (T)("p1="+getProperty("p1", String.class)+",p2="+getProperty("p2", String.class)+",p3="+getProperty("p3", String.class)+",p4="+getProperty("p4", String.class)+",p5="+getProperty("p5", String.class)+",contentType="+matchingContentType);
				}
			};
		}
	}

	@Test
	public void a01_preCall_propertiesOverriddenByAnnotation() throws Exception {
		a.put("/propertiesOverriddenByAnnotation", null).contentType("text/a1").execute().assertBody("p1=sp1,p2=xp2,p3=mp3,p4=xp4,p5=xp5,contentType=text/a1");
		a.put("/propertiesOverriddenByAnnotation", null).contentType("text/a1").header("Override-Content-Type", "text/a2").execute().assertBody("p1=sp1,p2=xp2,p3=mp3,p4=xp4,p5=xp5,contentType=text/a2");
	}

	@Test
	public void a02_preCall_propertiesOverriddenProgrammatically() throws Exception {
		a.put("/propertiesOverriddenProgrammatically", null).contentType("text/a1").execute().assertBody("p1=sp1,p2=xp2,p3=pp3,p4=pp4,p5=xp5,contentType=text/a1");
		a.put("/propertiesOverriddenProgrammatically", null).contentType("text/a1").header("Override-Content-Type", "text/a2").execute().assertBody("p1=sp1,p2=xp2,p3=pp3,p4=pp4,p5=xp5,contentType=text/a2");
	}

	//=================================================================================================================
	// @RestHook(POST_CALL)
	//=================================================================================================================

	@RestResource(
		serializers=B01.class,
		properties={
			@Property(name="p1",value="sp1"), // Unchanged servlet-level property.
			@Property(name="p2",value="sp2"), // Servlet-level property overridden by onPostCall.
			@Property(name="p3",value="sp3"), // Servlet-level property overridded by method.
			@Property(name="p4",value="sp4")  // Servlet-level property overridden by method then onPostCall.
		}
	)
	public static class B {

		@RestHook(POST_CALL)
		public void onPostCall(RestRequest req, RestResponse res) {
			RequestProperties properties = req.getProperties();
			properties.put("p2", "xp2");
			properties.put("p4", "xp4");
			properties.put("p5", "xp5"); // New property
			String overrideAccept = req.getHeader("Override-Accept");
			if (overrideAccept != null)
				req.getHeaders().put("Accept", overrideAccept);
			String overrideContentType = req.getHeader("Override-Content-Type");
			if (overrideContentType != null)
				properties.put("Override-Content-Type", overrideContentType);
		}

		@RestMethod(name=PUT, path="/propertiesOverridenByAnnotation",
			properties={
				@Property(name="p3",value="mp3"),
				@Property(name="p4",value="mp4")
			},
			defaultRequestHeaders="Accept: text/s2"
		)
		public String b01() {
			return null;
		}

		@RestMethod(name=PUT, path="/propertiesOverriddenProgramatically")
		public String b02(RestRequest req, RequestProperties properties) throws Exception {
			properties.put("p3", "pp3");
			properties.put("p4", "pp4");
			String accept = req.getHeader("Accept");
			if (accept == null || accept.isEmpty())
				req.getHeaders().put("Accept", "text/s2");
			return null;
		}
	}
	static MockRest b = MockRest.create(B.class);

	public static class B01 extends WriterSerializer {
		public B01(PropertyStore ps) {
			super(ps, "test/s1", "text/s1,text/s2,text/s3");
		}
		@Override /* Serializer */
		public WriterSerializerSession createSession(SerializerSessionArgs args) {
			return new WriterSerializerSession(args) {
				@Override /* SerializerSession */
				protected void doSerialize(SerializerPipe out, Object o) throws Exception {
					out.getWriter().write("p1="+getProperty("p1", String.class)+",p2="+getProperty("p2", String.class)+",p3="+getProperty("p3", String.class)+",p4="+getProperty("p4", String.class)+",p5="+getProperty("p5", String.class)+",contentType="+getProperty("mediaType", String.class));
				}
				@Override /* SerializerSession */
				public Map<String,String> getResponseHeaders() {
					ObjectMap p = getProperties();
					if (p.containsKey("Override-Content-Type"))
						return new AMap<String,String>().append("Content-Type", p.getString("Override-Content-Type"));
					return Collections.emptyMap();
				}
			};
		}
	}

	@Test
	public void b01a_postCall_propertiesOverridenByAnnotation() throws Exception {
		b.put("/propertiesOverridenByAnnotation", null).accept("text/s1").execute().assertBody("p1=sp1,p2=xp2,p3=mp3,p4=xp4,p5=xp5,contentType=text/s1");
		b.put("/propertiesOverridenByAnnotation", null).accept("text/s1").header("Override-Accept", "text/s2").execute().assertBody("p1=sp1,p2=xp2,p3=mp3,p4=xp4,p5=xp5,contentType=text/s2");
		b.put("/propertiesOverridenByAnnotation", null).accept("text/s1").header("Override-Content-Type", "text/s3").execute().assertBody("p1=sp1,p2=xp2,p3=mp3,p4=xp4,p5=xp5,contentType=text/s1");
	}
	@Test
	public void b01b_postCall_propertiesOverridenByAnnotation_defaultAccept() throws Exception {
		b.put("/propertiesOverridenByAnnotation", null).execute().assertBody("p1=sp1,p2=xp2,p3=mp3,p4=xp4,p5=xp5,contentType=text/s2");
		b.put("/propertiesOverridenByAnnotation", null).header("Override-Accept", "text/s3").execute().assertBody("p1=sp1,p2=xp2,p3=mp3,p4=xp4,p5=xp5,contentType=text/s3");
		b.put("/propertiesOverridenByAnnotation", null).header("Override-Content-Type", "text/s3").execute().assertBody("p1=sp1,p2=xp2,p3=mp3,p4=xp4,p5=xp5,contentType=text/s2");
	}
	@Test
	public void b02a_postCall_propertiesOverriddenProgramatically() throws Exception {
		b.put("/propertiesOverriddenProgramatically", null).accept("text/s1").execute().assertBody("p1=sp1,p2=xp2,p3=pp3,p4=xp4,p5=xp5,contentType=text/s1");
		b.put("/propertiesOverriddenProgramatically", null).accept("text/s1").header("Override-Accept", "text/s2").execute().assertBody("p1=sp1,p2=xp2,p3=pp3,p4=xp4,p5=xp5,contentType=text/s2");
		b.put("/propertiesOverriddenProgramatically", null).accept("text/s1").header("Override-Content-Type", "text/s3").execute().assertBody("p1=sp1,p2=xp2,p3=pp3,p4=xp4,p5=xp5,contentType=text/s1");
	}
	@Test
	public void b02b_postCall_propertiesOverriddenProgramatically_defaultAccept() throws Exception {
		b.put("/propertiesOverriddenProgramatically", null).execute().assertBody("p1=sp1,p2=xp2,p3=pp3,p4=xp4,p5=xp5,contentType=text/s2");
		b.put("/propertiesOverriddenProgramatically", null).header("Override-Accept", "text/s3").execute().assertBody("p1=sp1,p2=xp2,p3=pp3,p4=xp4,p5=xp5,contentType=text/s3");
		b.put("/propertiesOverriddenProgramatically", null).header("Override-Content-Type", "text/s3").execute().assertBody("p1=sp1,p2=xp2,p3=pp3,p4=xp4,p5=xp5,contentType=text/s2");
	}

	//====================================================================================================
	// @RestHook(INIT)
	//====================================================================================================

	@RestResource(children={C_Super.class,C_Sub.class})
	public static class C {}
	static MockRest c = MockRest.create(C.class);

	@RestResource(path="/super")
	public static class C_Super {
		protected ObjectList events = new ObjectList();
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
		@RestMethod
		public ObjectList getEvents() {
			return events;
		}
	}

	@RestResource(path="/sub", children={C_Child.class})
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

	@RestResource(path="/child")
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
		c.get("/super/events").execute().assertBody("['super-1a','super-1b','super-1c','super-2a']");
		c.get("/sub/events").execute().assertBody("['sub-1a','sub-1b','sub-1c','super-2a','sub-2b']");
		c.get("/sub/child/events").execute().assertBody("['super-1a','super-1b','child-1c','super-2a','child-2b']");
	}

	//====================================================================================================
	// @RestHook(POST_INIT)
	//====================================================================================================
	@RestResource(children={D_Super.class,D_Sub.class})
	public static class D {}
	static MockRest d = MockRest.create(D.class);

	@RestResource(path="/super")
	public static class D_Super {
		protected ObjectList events = new ObjectList();
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
		@RestMethod
		public ObjectList getEvents() {
			return events;
		}
	}

	@RestResource(path="/sub",children={D_Child.class})
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
		@RestMethod
		public String getLastCalled() {
			return LAST_CALLED;
		}
	}

	@RestResource(path="/child")
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
		d.get("/super/events").execute().assertBody("['super-1a','super-1b','super-1c','super-2a']");
		d.get("/sub/events").execute().assertBody("['sub-1a','sub-1b','sub-1c','super-2a','sub-2b']");
		d.get("/sub/child/events").execute().assertBody("['super-1a','super-1b','child-1c','super-2a','child-2b']");
	}
	@Test
	public void d02_postInit_order() throws Exception {
		d.get("/sub/lastCalled").execute().assertBody("CHILD");
	}

	//====================================================================================================
	// @RestHook(POST_INIT_CHILD_FIRST)
	//====================================================================================================

	@RestResource(
		children={
			E_Super.class,
			E_Sub.class
		}
	)
	public static class E {}
	static MockRest e = MockRest.create(E.class);

	@RestResource(path="/super")
	public static class E_Super {
		protected ObjectList events = new ObjectList();
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
		@RestMethod
		public ObjectList getPostInitChildFirstEvents() {
			return events;
		}
	}

	@RestResource(path="/sub", children={E_Child.class})
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
		@RestMethod
		public String getLastCalled() {
			return LAST_CALLED;
		}
	}

	@RestResource(path="/child")
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
		e.get("/super/postInitChildFirstEvents").execute().assertBody("['super-1a','super-1b','super-1c','super-2a']");
		e.get("/sub/postInitChildFirstEvents").execute().assertBody("['sub-1a','sub-1b','sub-1c','super-2a','sub-2b']");
		e.get("/sub/child/postInitChildFirstEvents").execute().assertBody("['super-1a','super-1b','child-1c','super-2a','child-2b']");
	}
	@Test
	public void e02_postInitChildFirst_order() throws Exception {
		e.get("/sub/lastCalled").execute().assertBody("PARENT");
	}

	//====================================================================================================
	// @RestHook(START_CALL)
	//====================================================================================================

	@RestResource
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
		@RestMethod(path="/")
		public ObjectMap getHeaders(RestRequest req, RestResponse res) {
			return new ObjectMap()
				.append("1", res.getHeader("start1-called"))
				.append("2", res.getHeader("start2-called"))
				.append("3", res.getHeader("start3-called"))
				.append("4", res.getHeader("start4-called"));
		}
	}
	static MockRest f = MockRest.create(F.class);

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
		f.get("/").execute().assertBody("{'1':'true','2':'true','3':'true','4':'true'}");
	}

	//====================================================================================================
	// @RestHook(PRE_CALL)
	//====================================================================================================

	@RestResource
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
		@RestMethod(path="/")
		public ObjectMap getHeaders(RestRequest req, RestResponse res) {
			return new ObjectMap()
				.append("1", res.getHeader("pre1-called"))
				.append("2", res.getHeader("pre2-called"))
				.append("3", res.getHeader("pre3-called"))
				.append("4", res.getHeader("pre4-called"));
		}
	}
	static MockRest g = MockRest.create(G.class);

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
		g.get("/").execute().assertBody("{'1':'true','2':'true','3':'true','4':'true'}");
	}

	//====================================================================================================
	// @RestHook(POST_CALL)
	//====================================================================================================

	@RestResource
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
		@RestMethod(path="/")
		public String doGet() {
			return "OK";
		}
	}
	static MockRest h = MockRest.create(H.class);

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
		h.get("/").execute()
			.assertHeader("post1-called", "true")
			.assertHeader("post2-called", "true")
			.assertHeader("post3-called", "true")
			.assertHeader("post4-called", "true");
	}
}
