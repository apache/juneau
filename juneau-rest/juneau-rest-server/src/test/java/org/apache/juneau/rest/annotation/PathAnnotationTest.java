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

import java.util.*;

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
		a.request("GET", "/bad?noTrace=true").execute().assertStatus(404);
	}
	@Test
	public void a01_noPath() throws Exception {
		a.request("GET", null).execute().assertBody("GET");
		a.request("GET", "").execute().assertBody("GET");
	}
	@Test
	public void a02_simplePath() throws Exception {
		a.request("GET", "/a").execute().assertBody("GET /a");
	}
	@Test
	public void a03_simplePathOneVar() throws Exception {
		a.request("GET", "/a/foo").execute().assertBody("GET /a foo");
	}
	@Test
	public void a04_simplePathTwoVars() throws Exception {
		a.request("GET", "/a/foo/bar").execute().assertBody("GET /a foo,bar");
	}
	@Test
	public void a05_simplePathWithRemainder() throws Exception {
		a.request("GET", "/a/foo/123/baz").execute().assertBody("GET /a foo,123,r=baz");
	}
	@Test
	public void a06_urlEncodedPathPart() throws Exception {
		// URL-encoded part should not get decoded before finding method to invoke.
		// This should match /get1/{foo} and not /get1/{foo}/{bar}
		// NOTE:  When testing on Tomcat, must specify the following system property:
		// -Dorg.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true
		a.request("GET", "/a/x%2Fy").execute().assertBody("GET /a x/y");
		a.request("GET", "/a/x%2Fy/x%2Fy").execute().assertBody("GET /a x/y,x/y");
	}

	//=================================================================================================================
	// Primitives
	//=================================================================================================================

	@RestResource
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
	static MockRest b = MockRest.create(B.class);

	@Test
	public void b01_int() throws Exception {
		b.request("GET", "/int/123/foo").execute().assertBody("123");
		b.request("GET", "/int/bad/foo?noTrace=true").execute().assertStatus(400);
	}
	@Test
	public void b02_short() throws Exception {
		b.request("GET", "/short/123/foo").execute().assertBody("123");
		b.request("GET", "/short/bad/foo?noTrace=true").execute().assertStatus(400);
	}
	@Test
	public void b03_long() throws Exception {
		b.request("GET", "/long/123/foo").execute().assertBody("123");
		b.request("GET", "/long/bad/foo?noTrace=true").execute().assertStatus(400);
	}
	@Test
	public void b04_char() throws Exception {
//		b.request("GET", "/char/c/foo").execute().assertBody("c");
		b.request("GET", "/char/bad/foo?noTrace=true").execute().assertStatus(400);
	}
	@Test
	public void b05_float() throws Exception {
		b.request("GET", "/float/1.23/foo").execute().assertBody("1.23");
		b.request("GET", "/float/bad/foo?noTrace=true").execute().assertStatus(400);
	}
	@Test
	public void b06_double() throws Exception {
		b.request("GET", "/double/1.23/foo").execute().assertBody("1.23");
		b.request("GET", "/double/bad/foo?noTrace=true").execute().assertStatus(400);
	}
	@Test
	public void b07_byte() throws Exception {
		b.request("GET", "/byte/123/foo").execute().assertBody("123");
		b.request("GET", "/byte/bad/foo?noTrace=true").execute().assertStatus(400);
	}
	@Test
	public void b08_boolean() throws Exception {
		b.request("GET", "/boolean/true/foo").execute().assertBody("true");
		b.request("GET", "/boolean/bad/foo?noTrace=true").execute().assertStatus(400);
	}
	
	//=================================================================================================================
	// Primitive objects
	//=================================================================================================================

	@RestResource
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
	static MockRest c = MockRest.create(C.class);

	@Test
	public void c01_Integer() throws Exception {
		c.request("GET", "/Integer/123/foo").execute().assertBody("123");
		c.request("GET", "/Integer/bad/foo?noTrace=true").execute().assertStatus(400);
	}
	@Test
	public void c02_Short() throws Exception {
		c.request("GET", "/Short/123/foo").execute().assertBody("123");
		c.request("GET", "/Short/bad/foo?noTrace=true").execute().assertStatus(400);
	}
	@Test
	public void c03_Long() throws Exception {
		c.request("GET", "/Long/123/foo").execute().assertBody("123");
		c.request("GET", "/Long/bad/foo?noTrace=true").execute().assertStatus(400);
	}
	@Test
	public void c04_Char() throws Exception {
		c.request("GET", "/Character/c/foo").execute().assertBody("c");
		c.request("GET", "/Character/bad/foo?noTrace=true").execute().assertStatus(400);
	}
	@Test
	public void c05_Float() throws Exception {
		c.request("GET", "/Float/1.23/foo").execute().assertBody("1.23");
		c.request("GET", "/Float/bad/foo?noTrace=true").execute().assertStatus(400);
	}
	@Test
	public void c06_Double() throws Exception {
		c.request("GET", "/Double/1.23/foo").execute().assertBody("1.23");
		c.request("GET", "/Double/bad/foo?noTrace=true").execute().assertStatus(400);
	}
	@Test
	public void c07_Byte() throws Exception {
		c.request("GET", "/Byte/123/foo").execute().assertBody("123");
		c.request("GET", "/Byte/bad/foo?noTrace=true").execute().assertStatus(400);
	}
	@Test
	public void c08_Boolean() throws Exception {
		c.request("GET", "/Boolean/true/foo").execute().assertBody("true");
		c.request("GET", "/Boolean/bad/foo?noTrace=true").execute().assertStatus(400);
	}

	//=================================================================================================================
	// POJOs convertible from strings
	//=================================================================================================================

	@RestResource
	public static class D {
		// Object with forString(String) method
		@RestMethod(name=GET, path="/uuid/{uuid}")
		public UUID uuid(RestResponse res, @Path("uuid") UUID uuid) {
			return uuid;
		}
	}
	static MockRest d = MockRest.create(D.class);

	@Test
	public void d01_uuid() throws Exception {
		UUID uuid = UUID.randomUUID();
		d.request("GET", "/uuid/" + uuid).execute().assertBody(uuid.toString());
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	//=================================================================================================================
	// Overlapping method names
	//=================================================================================================================

//
//		//	// Bunch of different argument types
//		//	@RestMethod(name=POST,pattern="/person/{person}")
//		//	public void doPost(RestRequest req, RestResponse res, Person p) {
//		//		res.setOutput("POST, /person, name="+p.name+", age="+p.age+" remainder="+req.getRemainder());
//		//	}
//		r = client.doPost(URL + "/person/(name='John+Smith',birthDate='Jan+12,+1952')", "");
//		assertEquals("POST /person/{name=John Smith,birthDate.year=1952} remainder=null", r.getResponse(String.class));
//
//		// Fall through to top-level POST
//		r = client.doPost(URL + "/person/(name:'John+Smith',age:123)/foo", "");
//		assertEquals("POST remainder=person/(name:'John Smith',age:123)/foo", r.getResponse(String.class));
//
//		//	// Various primitive types
//		//	@RestMethod(name=PUT,pattern="/primitives/{xInt}.{xShort},{xLong}/{xChar}/{xFloat}/{xDouble}/{xByte}/{xBoolean}")
//		//	public void doPut1(RestRequest req, RestResponse res, int xInt, short xShort, long xLong, char xChar, float xFloat, double xDouble, byte xByte, boolean xBoolean) {
//		//		res.setOutput("PUT, /primitives/"+xInt+"."+xShort+","+xLong+"/"+xChar+"/"+xFloat+"/"+xDouble+"/"+xByte+"/"+xBoolean);
//		//	}
//		r = client.doPut(URL + "/primitives/1/2/3/x/4/5/6/true", "");
//		assertEquals("PUT /primitives/1/2/3/x/4.0/5.0/6/true", r.getResponse(String.class));
//
//		//	// Various primitive objects
//		//	@RestMethod(name=PUT,pattern="/primitiveObjects/{xInt}/{xShort}/{xLong}/{xChar}/{xFloat}/{xDouble}/{xByte}/{xBoolean}")
//		//	public void doPut1(RestRequest req, RestResponse res, Integer xInt, Short xShort, Long xLong, Character xChar, Float xFloat, Double xDouble, Byte xByte, Boolean xBoolean) {
//		//		res.setOutput("PUT /primitives/"+xInt+"/"+xShort+"/"+xLong+"/"+xChar+"/"+xFloat+"/"+xDouble+"/"+xByte+"/"+xBoolean);
//		//	}
//		r = client.doPut(URL + "/primitiveObjects/1/2/3/x/4/5/6/true", "");
//		assertEquals("PUT /primitiveObjects/1/2/3/x/4.0/5.0/6/true", r.getResponse(String.class));
//
//		//	// Object with forString(String) method
//		//	@RestMethod(name=PUT,pattern="/uuid/{uuid}")
//		//	public void doPut1(RestRequest req, RestResponse res, UUID uuid) {
//		//		res.setOutput("PUT /uuid/"+uuid);
//		//	}
//		UUID uuid = UUID.randomUUID();
//		r = client.doPut(URL + "/uuid/"+uuid, "");
//		assertEquals("PUT /uuid/"+uuid, r.getResponse(String.class));
	}
	
	
		
//
//		// Test method name with overlapping name, remainder allowed.
//		@RestMethod(name="GET2")
//		public void get2(RestRequest req, RestResponse res) {
//			res.setOutput("GET2 remainder="+req.getPathMatch().getRemainder());
//		}
//
//		// Default POST
//		@RestMethod(name=POST)
//		public void doPost(RestRequest req, RestResponse res) {
//			res.setOutput("POST remainder="+req.getPathMatch().getRemainder());
//		}
//
//		// Bean parameter
//		@RestMethod(name=POST, path="/person/{person}")
//		public void doPost(RestRequest req, RestResponse res, @Path("person") Person p) {
//			res.setOutput("POST /person/{name="+p.name+",birthDate.year="+p.birthDate.get(Calendar.YEAR)+"} remainder="+req.getPathMatch().getRemainder());
//		}
//
//		// Various primitive types
//		@RestMethod(name=PUT, path="/primitives/{xInt}/{xShort}/{xLong}/{xChar}/{xFloat}/{xDouble}/{xByte}/{xBoolean}")
//		public void doPut1(
//				RestResponse res, 
//				@Path("xInt") int xInt, 
//				@Path("xShort") short xShort, 
//				@Path("xLong") long xLong, 
//				@Path("xChar") char xChar, 
//				@Path("xFloat") float xFloat, 
//				@Path("xDouble") double xDouble, 
//				@Path("xByte") byte xByte, 
//				@Path("xBoolean") boolean xBoolean
//			) {
//			res.setOutput("PUT /primitives/"+xInt+"/"+xShort+"/"+xLong+"/"+xChar+"/"+xFloat+"/"+xDouble+"/"+xByte+"/"+xBoolean);
//		}
//
//		// Various primitive objects
//		@RestMethod(name=PUT, path="/primitiveObjects/{xInt}/{xShort}/{xLong}/{xChar}/{xFloat}/{xDouble}/{xByte}/{xBoolean}")
//		public void doPut2(
//				RestResponse res, 
//				@Path("xInt") Integer xInt, 
//				@Path("xShort") Short xShort, 
//				@Path("xLong") Long xLong, 
//				@Path("xChar") Character xChar, 
//				@Path("xFloat") Float xFloat, 
//				@Path("xDouble") Double xDouble, 
//				@Path("xByte") Byte xByte, 
//				@Path("xBoolean") Boolean xBoolean
//			) {
//			res.setOutput("PUT /primitiveObjects/"+xInt+"/"+xShort+"/"+xLong+"/"+xChar+"/"+xFloat+"/"+xDouble+"/"+xByte+"/"+xBoolean);
//		}
//
//		// Object with forString(String) method
//		@RestMethod(name=PUT, path="/uuid/{uuid}")
//		public void doPut1(RestResponse res, @Path("uuid") UUID uuid) {
//			res.setOutput("PUT /uuid/"+uuid);
//		}
//	}
