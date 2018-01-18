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
package org.apache.juneau.rest.test;

import static javax.servlet.http.HttpServletResponse.*;
import static org.apache.juneau.rest.test.TestUtils.*;
import static org.junit.Assert.*;

import java.io.*;
import java.util.*;

import org.apache.http.*;
import org.apache.http.client.entity.*;
import org.apache.http.entity.*;
import org.apache.http.message.*;
import org.apache.juneau.*;
import org.apache.juneau.rest.client.*;
import org.junit.*;

public class ParamsTest extends RestTestcase {

	private static String URL = "/testParams";
	private static boolean debug = false;

	private static RestClient CLIENT = TestMicroservice.DEFAULT_CLIENT;

	//====================================================================================================
	// Basic tests
	//====================================================================================================
	@Test
	public void testBasic() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT;
		RestCall r;

		//		@Override
		//		@RestMethod(name=GET,pattern="/")
		//		public void doGet(RestRequest req, RestResponse res) {
		//			res.setOutput("No args");
		//		}
		r = client.doGet(URL);
		assertEquals("GET", r.getResponse(String.class));

		r = client.doGet(URL + "/getx?noTrace=true");
		try {
			r.connect();
			fail("Connection should have failed.");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_FOUND, "Method 'GET' not found on resource with matching pattern on path '/getx'");
		}

		//	@RestMethod(name=GET,pattern="/get1")
		//	public void doGet1(RestRequest req, RestResponse res) {
		//		res.setOutput("/get1");
		//	}
		r = client.doGet(URL + "/get1");
		assertEquals("GET /get1", r.getResponse(String.class));

		r = client.doGet(URL + "/get1a?noTrace=true");
		try {
			r.connect();
			fail("Connection should have failed.");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_FOUND, "Method 'GET' not found on resource with matching pattern on path '/get1a'");
		}

		//	@RestMethod(name=GET,pattern="/get1/{foo}")
		//	public void doGet(RestRequest req, RestResponse res, String foo) {
		//		res.setOutput("/get1/" + foo);
		//	}
		r = client.doGet(URL + "/get1/foo");
		assertEquals("GET /get1a foo", r.getResponse(String.class));

		// URL-encoded part should not get decoded before finding method to invoke.
		// This should match /get1/{foo} and not /get1/{foo}/{bar}
		// NOTE:  When testing on Tomcat, must specify the following system property:
		// -Dorg.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true
		String x = "x%2Fy";  // [x/y]
		r = client.doGet(URL + "/get1/"+x);
		assertEquals("GET /get1a x/y", r.getResponse(String.class));

		r = client.doGet(URL + "/get1/"+x+"/"+x);
		assertEquals("GET /get1b x/y,x/y", r.getResponse(String.class));

		r = client.doGet(URL + "/get1/foo");
		assertEquals("GET /get1a foo", r.getResponse(String.class));

		r = client.doGet(URL + "/get1/foo/bar/baz?noTrace=true");
		try {
			r.connect();
			fail("Connection should have failed.");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_FOUND, "Method 'GET' not found on resource with matching pattern on path '/get1/foo/bar/baz'");
		}

		//	@RestMethod(name=GET,pattern="/get3/{foo}/{bar}/*")
		//	public void doGet3(RestRequest req, RestResponse res, String foo, int bar) {
		//		res.setOutput("/get3/"+foo+"/"+bar+", remainder="+req.getRemainder());
		//	}
		r = client.doGet(URL + "/get3/foo/123");
		assertEquals("GET /get3/foo/123 remainder=null", r.getResponse(String.class));

		r = client.doGet(URL + "/get3/foo/123/xxx");
		assertEquals("GET /get3/foo/123 remainder=xxx", r.getResponse(String.class));

		//	// Test method name with overlapping name, remainder allowed.
		//	@RestMethod(name="GET2")
		//	public void get2(RestRequest req, RestResponse res) {
		//		res.setOutput("GET2, remainder="+req.getRemainder());
		//	}
		r = client.doGet(URL + "?method=get2");
		assertEquals("GET2 remainder=null", r.getResponse(String.class));
		r = client.doGet(URL + "/foo/bar?method=get2");
		assertEquals("GET2 remainder=foo/bar", r.getResponse(String.class));
		r = client.doGet(URL + "/foo/bar?method=GET2");
		assertEquals("GET2 remainder=foo/bar", r.getResponse(String.class));

		//	// Default POST
		//	@Override
		//	public void doPost(RestRequest req, RestResponse res) {
		//		res.setOutput("POST, remainder="+req.getRemainder());
		//	}
		r = client.doPost(URL, "");
		assertEquals("POST remainder=null", r.getResponse(String.class));
		r = client.doPost(URL + "/foo", "");
		assertEquals("POST remainder=foo", r.getResponse(String.class));

		//	// Bunch of different argument types
		//	@RestMethod(name=POST,pattern="/person/{person}")
		//	public void doPost(RestRequest req, RestResponse res, Person p) {
		//		res.setOutput("POST, /person, name="+p.name+", age="+p.age+" remainder="+req.getRemainder());
		//	}
		r = client.doPost(URL + "/person/(name='John+Smith',birthDate='Jan+12,+1952')", "");
		assertEquals("POST /person/{name=John Smith,birthDate.year=1952} remainder=null", r.getResponse(String.class));

		// Fall through to top-level POST
		r = client.doPost(URL + "/person/(name:'John+Smith',age:123)/foo", "");
		assertEquals("POST remainder=person/(name:'John Smith',age:123)/foo", r.getResponse(String.class));

		//	// Various primitive types
		//	@RestMethod(name=PUT,pattern="/primitives/{xInt}.{xShort},{xLong}/{xChar}/{xFloat}/{xDouble}/{xByte}/{xBoolean}")
		//	public void doPut1(RestRequest req, RestResponse res, int xInt, short xShort, long xLong, char xChar, float xFloat, double xDouble, byte xByte, boolean xBoolean) {
		//		res.setOutput("PUT, /primitives/"+xInt+"."+xShort+","+xLong+"/"+xChar+"/"+xFloat+"/"+xDouble+"/"+xByte+"/"+xBoolean);
		//	}
		r = client.doPut(URL + "/primitives/1/2/3/x/4/5/6/true", "");
		assertEquals("PUT /primitives/1/2/3/x/4.0/5.0/6/true", r.getResponse(String.class));

		//	// Various primitive objects
		//	@RestMethod(name=PUT,pattern="/primitiveObjects/{xInt}/{xShort}/{xLong}/{xChar}/{xFloat}/{xDouble}/{xByte}/{xBoolean}")
		//	public void doPut1(RestRequest req, RestResponse res, Integer xInt, Short xShort, Long xLong, Character xChar, Float xFloat, Double xDouble, Byte xByte, Boolean xBoolean) {
		//		res.setOutput("PUT /primitives/"+xInt+"/"+xShort+"/"+xLong+"/"+xChar+"/"+xFloat+"/"+xDouble+"/"+xByte+"/"+xBoolean);
		//	}
		r = client.doPut(URL + "/primitiveObjects/1/2/3/x/4/5/6/true", "");
		assertEquals("PUT /primitiveObjects/1/2/3/x/4.0/5.0/6/true", r.getResponse(String.class));

		//	// Object with forString(String) method
		//	@RestMethod(name=PUT,pattern="/uuid/{uuid}")
		//	public void doPut1(RestRequest req, RestResponse res, UUID uuid) {
		//		res.setOutput("PUT /uuid/"+uuid);
		//	}
		UUID uuid = UUID.randomUUID();
		r = client.doPut(URL + "/uuid/"+uuid, "");
		assertEquals("PUT /uuid/"+uuid, r.getResponse(String.class));
	}

	//====================================================================================================
	// @FormData annotation - GET
	//====================================================================================================
	@Test
	public void testParamGet() throws Exception {
		RestClient client = TestMicroservice.client().accept("text/plain").build();
		String r;
		String url = URL + "/testParamGet";

		r = client.doGet(url + "?p1=p1&p2=2").getResponseAsString();
		assertEquals("p1=[p1,p1,p1],p2=[2,2,2]", r);

		r = client.doGet(url + "?p1&p2").getResponseAsString();
		assertEquals("p1=[,,],p2=[0,,0]", r);

		r = client.doGet(url).getResponseAsString();
		assertEquals("p1=[null,null,null],p2=[0,null,0]", r);

		r = client.doGet(url + "?p1").getResponseAsString();
		assertEquals("p1=[,,],p2=[0,null,0]", r);

		r = client.doGet(url + "?p2").getResponseAsString();
		assertEquals("p1=[null,null,null],p2=[0,,0]", r);

		r = client.doGet(url + "?p1=foo&p2").getResponseAsString();
		assertEquals("p1=[foo,foo,foo],p2=[0,,0]", r);

		r = client.doGet(url + "?p1&p2=1").getResponseAsString();
		assertEquals("p1=[,,],p2=[1,1,1]", r);

		String x = "a%2Fb%25c%3Dd+e"; // [x/y%z=a+b]
		r = client.doGet(url + "?p1="+x+"&p2=1").getResponseAsString();
		assertEquals("p1=[a/b%c=d e,a/b%c=d e,a/b%c=d e],p2=[1,1,1]", r);

		client.closeQuietly();
	}

	//====================================================================================================
	// @FormData(format=PLAIN) annotation - GET
	//====================================================================================================
	@Test
	public void testPlainParamGet() throws Exception {
		RestClient client = TestMicroservice.client().accept("text/plain").build();
		String r;
		String url = URL + "/testPlainParamGet";

		r = client.doGet(url + "?p1=p1").getResponseAsString();
		assertEquals("p1=[p1,p1,p1]", r);

		r = client.doGet(url + "?p1='p1'").getResponseAsString();
		assertEquals("p1=['p1','p1',p1]", r);

		client.closeQuietly();
	}

	//====================================================================================================
	// @FormData annotation - POST
	//====================================================================================================
	@Test
	public void testParamPost() throws Exception {
		RestClient client = TestMicroservice.client().accept("text/plain").build();
		String r;
		String url = URL + "/testParamPost";

		r = client.doFormPost(url, new ObjectMap("{p1:'p1',p2:2}")).getResponseAsString();
		assertEquals("p1=[p1,p1,p1],p2=[2,2,2]", r);

		r = client.doFormPost(url, new ObjectMap("{p1:null,p2:0}")).getResponseAsString();
		assertEquals("p1=[null,null,null],p2=[0,0,0]", r);

		r = client.doFormPost(url, new ObjectMap("{}")).getResponseAsString();
		assertEquals("p1=[null,null,null],p2=[0,null,0]", r);

		r = client.doFormPost(url, new ObjectMap("{p1:null}")).getResponseAsString();
		assertEquals("p1=[null,null,null],p2=[0,null,0]", r);

		r = client.doFormPost(url, new ObjectMap("{p2:0}")).getResponseAsString();
		assertEquals("p1=[null,null,null],p2=[0,0,0]", r);

		r = client.doFormPost(url, new ObjectMap("{p1:'foo',p2:0}")).getResponseAsString();
		assertEquals("p1=[foo,foo,foo],p2=[0,0,0]", r);

		r = client.doFormPost(url, new ObjectMap("{p1:null,p2:1}")).getResponseAsString();
		assertEquals("p1=[null,null,null],p2=[1,1,1]", r);

		r = client.doFormPost(url, new ObjectMap("{p1:'a/b%c=d e,f/g%h=i j',p2:1}")).getResponseAsString();
		assertEquals("p1=[a/b%c=d e,f/g%h=i j,'a/b%c=d e,f/g%h=i j',a/b%c=d e,f/g%h=i j],p2=[1,1,1]", r);

		client.closeQuietly();
	}

	//====================================================================================================
	// @FormData(format=PLAIN) annotation - POST
	//====================================================================================================
	@Test
	public void testPlainParamPost() throws Exception {
		RestClient client = TestMicroservice.client().accept("text/plain").build();
		String r;
		String url = URL + "/testPlainParamPost";

		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("p1", "p1"));
		HttpEntity he = new UrlEncodedFormEntity(nvps);

		r = client.doPost(url, he).getResponseAsString();
		assertEquals("p1=[p1,p1,p1]", r);

		nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("p1", "'p1'"));
		he = new UrlEncodedFormEntity(nvps);

		r = client.doFormPost(url, he).getResponseAsString();
		assertEquals("p1=['p1','p1',p1]", r);

		client.closeQuietly();
	}

	//====================================================================================================
	// @Query annotation - GET
	//====================================================================================================
	@Test
	public void testQParamGet() throws Exception {
		RestClient client = TestMicroservice.client().accept("text/plain").build();
		String r;
		String url = URL + "/testQParamGet";

		r = client.doGet(url + "?p1=p1&p2=2").getResponseAsString();
		assertEquals("p1=[p1,p1,p1],p2=[2,2,2]", r);

		r = client.doGet(url + "?p1&p2").getResponseAsString();
		assertEquals("p1=[,,],p2=[0,,0]", r);

		r = client.doGet(url).getResponseAsString();
		assertEquals("p1=[null,null,null],p2=[0,null,0]", r);

		r = client.doGet(url + "?p1").getResponseAsString();
		assertEquals("p1=[,,],p2=[0,null,0]", r);

		r = client.doGet(url + "?p2").getResponseAsString();
		assertEquals("p1=[null,null,null],p2=[0,,0]", r);

		r = client.doGet(url + "?p1=foo&p2").getResponseAsString();
		assertEquals("p1=[foo,foo,foo],p2=[0,,0]", r);

		r = client.doGet(url + "?p1&p2=1").getResponseAsString();
		assertEquals("p1=[,,],p2=[1,1,1]", r);

		String x = "a%2Fb%25c%3Dd+e"; // [x/y%z=a+b]
		r = client.doGet(url + "?p1="+x+"&p2=1").getResponseAsString();
		assertEquals("p1=[a/b%c=d e,a/b%c=d e,a/b%c=d e],p2=[1,1,1]", r);

		client.closeQuietly();
	}

	//====================================================================================================
	// @Query(format=PLAIN) annotation - GET
	//====================================================================================================
	@Test
	public void testPlainQParamGet() throws Exception {
		RestClient client = TestMicroservice.client().accept("text/plain").build();
		String r;
		String url = URL + "/testPlainQParamGet";

		r = client.doGet(url + "?p1=p1").getResponseAsString();
		assertEquals("p1=[p1,p1,p1]", r);

		r = client.doGet(url + "?p1='p1'").getResponseAsString();
		assertEquals("p1=['p1','p1',p1]", r);

		client.closeQuietly();
	}

	//====================================================================================================
	// @Query annotation - POST
	//====================================================================================================
	@Test
	public void testQParamPost() throws Exception {
		RestClient client = TestMicroservice.client().accept("text/plain").build();
		String r;
		String url = URL + "/testQParamPost";

		r = client.doFormPost(url, new ObjectMap("{p1:'p1',p2:2}")).getResponseAsString();
		assertEquals("p1=[null,null,null],p2=[0,null,0]", r);

		r = client.doFormPost(url, new ObjectMap("{p1:null,p2:0}")).getResponseAsString();
		assertEquals("p1=[null,null,null],p2=[0,null,0]", r);

		r = client.doFormPost(url, new ObjectMap("{}")).getResponseAsString();
		assertEquals("p1=[null,null,null],p2=[0,null,0]", r);

		r = client.doFormPost(url, new ObjectMap("{p1:null}")).getResponseAsString();
		assertEquals("p1=[null,null,null],p2=[0,null,0]", r);

		r = client.doFormPost(url, new ObjectMap("{p2:0}")).getResponseAsString();
		assertEquals("p1=[null,null,null],p2=[0,null,0]", r);

		r = client.doFormPost(url, new ObjectMap("{p1:'foo',p2:0}")).getResponseAsString();
		assertEquals("p1=[null,null,null],p2=[0,null,0]", r);

		r = client.doFormPost(url, new ObjectMap("{p1:null,p2:1}")).getResponseAsString();
		assertEquals("p1=[null,null,null],p2=[0,null,0]", r);

		r = client.doFormPost(url, new ObjectMap("{p1:'a/b%c=d e,f/g%h=i j',p2:1}")).getResponseAsString();
		assertEquals("p1=[null,null,null],p2=[0,null,0]", r);

		client.closeQuietly();
	}

	//====================================================================================================
	// @HasQuery annotation - GET
	//====================================================================================================
	@Test
	public void testHasParamGet() throws Exception {
		RestClient client = TestMicroservice.client().accept("text/plain").build();
		String r;
		String url = URL + "/testHasParamGet";

		r = client.doGet(url + "?p1=p1&p2=2").getResponseAsString();
		assertEquals("p1=[true,true],p2=[true,true]", r);

		r = client.doGet(url + "?p1&p2").getResponseAsString();
		assertEquals("p1=[true,true],p2=[true,true]", r);

		r = client.doGet(url).getResponseAsString();
		assertEquals("p1=[false,false],p2=[false,false]", r);

		r = client.doGet(url + "?p1").getResponseAsString();
		assertEquals("p1=[true,true],p2=[false,false]", r);

		r = client.doGet(url + "?p2").getResponseAsString();
		assertEquals("p1=[false,false],p2=[true,true]", r);

		r = client.doGet(url + "?p1=foo&p2").getResponseAsString();
		assertEquals("p1=[true,true],p2=[true,true]", r);

		r = client.doGet(url + "?p1&p2=1").getResponseAsString();
		assertEquals("p1=[true,true],p2=[true,true]", r);

		String x = "x%2Fy%25z%3Da+b"; // [x/y%z=a+b]
		r = client.doGet(url + "?p1="+x+"&p2=1").getResponseAsString();
		assertEquals("p1=[true,true],p2=[true,true]", r);

		client.closeQuietly();
	}

	//====================================================================================================
	// @HasQuery annotation - POST
	//====================================================================================================
	@Test
	public void testHasParamPost() throws Exception {
		RestClient client = TestMicroservice.client().accept("text/plain").build();
		String r;
		String url = URL + "/testHasParamPost";

		r = client.doFormPost(url, new ObjectMap("{p1:'p1',p2:2}")).getResponseAsString();
		assertEquals("p1=[true,true],p2=[true,true]", r);

		r = client.doFormPost(url, new ObjectMap("{p1:null,p2:0}")).getResponseAsString();
		assertEquals("p1=[true,true],p2=[true,true]", r);

		r = client.doFormPost(url, new ObjectMap("{}")).getResponseAsString();
		assertEquals("p1=[false,false],p2=[false,false]", r);

		r = client.doFormPost(url, new ObjectMap("{p1:null}")).getResponseAsString();
		assertEquals("p1=[true,true],p2=[false,false]", r);

		r = client.doFormPost(url, new ObjectMap("{p2:0}")).getResponseAsString();
		assertEquals("p1=[false,false],p2=[true,true]", r);

		r = client.doFormPost(url, new ObjectMap("{p1:'foo',p2:0}")).getResponseAsString();
		assertEquals("p1=[true,true],p2=[true,true]", r);

		r = client.doFormPost(url, new ObjectMap("{p1:null,p2:1}")).getResponseAsString();
		assertEquals("p1=[true,true],p2=[true,true]", r);

		r = client.doFormPost(url, new ObjectMap("{p1:'a/b%c=d e,f/g%h=i j',p2:1}")).getResponseAsString();
		assertEquals("p1=[true,true],p2=[true,true]", r);

		client.closeQuietly();
	}

	//====================================================================================================
	// @HasQuery annotation - GET
	//====================================================================================================
	@Test
	public void testHasQParamGet() throws Exception {
		RestClient client = TestMicroservice.client().accept("text/plain").build();
		String r;
		String url = URL + "/testHasQParamGet";

		r = client.doGet(url + "?p1=p1&p2=2").getResponseAsString();
		assertEquals("p1=[true,true],p2=[true,true]", r);

		r = client.doGet(url + "?p1&p2").getResponseAsString();
		assertEquals("p1=[true,true],p2=[true,true]", r);

		r = client.doGet(url).getResponseAsString();
		assertEquals("p1=[false,false],p2=[false,false]", r);

		r = client.doGet(url + "?p1").getResponseAsString();
		assertEquals("p1=[true,true],p2=[false,false]", r);

		r = client.doGet(url + "?p2").getResponseAsString();
		assertEquals("p1=[false,false],p2=[true,true]", r);

		r = client.doGet(url + "?p1=foo&p2").getResponseAsString();
		assertEquals("p1=[true,true],p2=[true,true]", r);

		r = client.doGet(url + "?p1&p2=1").getResponseAsString();
		assertEquals("p1=[true,true],p2=[true,true]", r);

		String x = "x%2Fy%25z%3Da+b"; // [x/y%z=a+b]
		r = client.doGet(url + "?p1="+x+"&p2=1").getResponseAsString();
		assertEquals("p1=[true,true],p2=[true,true]", r);

		client.closeQuietly();
	}

	//====================================================================================================
	// @HasQuery annotation - POST
	//====================================================================================================
	@Test
	public void testHasQParamPost() throws Exception {
		RestClient client = TestMicroservice.client().accept("text/plain").build();
		String r;
		String url = URL + "/testHasQParamPost";

		r = client.doFormPost(url, new ObjectMap("{p1:'p1',p2:2}")).getResponseAsString();
		assertEquals("p1=[false,false],p2=[false,false]", r);

		r = client.doFormPost(url, new ObjectMap("{p1:null,p2:0}")).getResponseAsString();
		assertEquals("p1=[false,false],p2=[false,false]", r);

		r = client.doFormPost(url, new ObjectMap("{}")).getResponseAsString();
		assertEquals("p1=[false,false],p2=[false,false]", r);

		r = client.doFormPost(url, new ObjectMap("{p1:null}")).getResponseAsString();
		assertEquals("p1=[false,false],p2=[false,false]", r);

		r = client.doFormPost(url, new ObjectMap("{p2:0}")).getResponseAsString();
		assertEquals("p1=[false,false],p2=[false,false]", r);

		r = client.doFormPost(url, new ObjectMap("{p1:'foo',p2:0}")).getResponseAsString();
		assertEquals("p1=[false,false],p2=[false,false]", r);

		r = client.doFormPost(url, new ObjectMap("{p1:null,p2:1}")).getResponseAsString();
		assertEquals("p1=[false,false],p2=[false,false]", r);

		r = client.doFormPost(url, new ObjectMap("{p1:'a/b%c=d e,f/g%h=i j',p2:1}")).getResponseAsString();
		assertEquals("p1=[false,false],p2=[false,false]", r);

		client.closeQuietly();
	}

	//====================================================================================================
	// Form POSTS with @Body parameter
	//====================================================================================================
	@Test
	public void testFormPostAsContent() throws Exception {
		RestClient client = TestMicroservice.client().accept("text/plain").build();
		String r;
		String url = URL + "/testFormPostAsContent";

		r = client.doFormPost(url, new ObjectMap("{p1:'p1',p2:2}")).getResponseAsString();
		assertEquals("bean=[{p1:'p1',p2:2}],qp1=[null],qp2=[0],hqp1=[false],hqp2=[false]", r);

		r = client.doFormPost(url, new ObjectMap("{}")).getResponseAsString();
		assertEquals("bean=[{p2:0}],qp1=[null],qp2=[0],hqp1=[false],hqp2=[false]", r);

		r = client.doFormPost(url+"?p1=p3&p2=4", new ObjectMap("{p1:'p1',p2:2}")).getResponseAsString();
		assertEquals("bean=[{p1:'p1',p2:2}],qp1=[p3],qp2=[4],hqp1=[true],hqp2=[true]", r);

		r = client.doFormPost(url+"?p1=p3&p2=4", new ObjectMap("{}")).getResponseAsString();
		assertEquals("bean=[{p2:0}],qp1=[p3],qp2=[4],hqp1=[true],hqp2=[true]", r);

		client.closeQuietly();
	}

	//====================================================================================================
	// Test @FormData and @Query annotations when using multi-part parameters (e.g. &key=val1,&key=val2).
	//====================================================================================================
	@Test
	public void testMultiPartParams() throws Exception {
		RestClient client = TestMicroservice.client().accept("text/plain").build();
		String r;
		String url = URL + "/testMultiPartParams";

		String in = ""
			+ "?p1=a&p1=b"
			+ "&p2=1&p2=2"
			+ "&p3=a&p3=b"
			+ "&p4=1&p4=2"
			+ "&p5=a&p5=b"
			+ "&p6=1&p6=2"
			+ "&p7=a&p7=b"
			+ "&p8=1&p8=2"
			+ "&p9=(a=1,b=2,c=false)&p9=(a=3,b=4,c=true)"
			+ "&p10=(a=1,b=2,c=false)&p10=(a=3,b=4,c=true)"
			+ "&p11=(a=1,b=2,c=false)&p11=(a=3,b=4,c=true)"
			+ "&p12=(a=1,b=2,c=false)&p12=(a=3,b=4,c=true)";
		r = client.doGet(url + in).getResponseAsString();
		String e = "{"
			+ "p1:['a','b'],"
			+ "p2:[1,2],"
			+ "p3:['a','b'],"
			+ "p4:[1,2],"
			+ "p5:['a','b'],"
			+ "p6:[1,2],"
			+ "p7:['a','b'],"
			+ "p8:[1,2],"
			+ "p9:[{a:'1',b:2,c:false},{a:'3',b:4,c:true}],"
			+ "p10:[{a:'1',b:2,c:false},{a:'3',b:4,c:true}],"
			+ "p11:[{a:'1',b:2,c:false},{a:'3',b:4,c:true}],"
			+ "p12:[{a:'1',b:2,c:false},{a:'3',b:4,c:true}]"
		+"}";
		assertEquals(e, r);

		client.closeQuietly();
	}

	//====================================================================================================
	// Same as testMultiPartParams(), except make sure single values are still interpreted as collections.
	//====================================================================================================
	@Test
	public void testMultiPartParamsSingleValues() throws Exception {
		RestClient client = TestMicroservice.client().accept("text/plain").build();
		String r;
		String url = URL + "/testMultiPartParams";

		String in = ""
			+ "?p1=a"
			+ "&p2=1"
			+ "&p3=a"
			+ "&p4=1"
			+ "&p5=a"
			+ "&p6=1"
			+ "&p7=a"
			+ "&p8=1"
			+ "&p9=(a=1,b=2,c=false)"
			+ "&p10=(a=1,b=2,c=false)"
			+ "&p11=(a=1,b=2,c=false)"
			+ "&p12=(a=1,b=2,c=false)";
		r = client.doGet(url + in).getResponseAsString();
		String e = "{"
			+ "p1:['a'],"
			+ "p2:[1],"
			+ "p3:['a'],"
			+ "p4:[1],"
			+ "p5:['a'],"
			+ "p6:[1],"
			+ "p7:['a'],"
			+ "p8:[1],"
			+ "p9:[{a:'1',b:2,c:false}],"
			+ "p10:[{a:'1',b:2,c:false}],"
			+ "p11:[{a:'1',b:2,c:false}],"
			+ "p12:[{a:'1',b:2,c:false}]"
		+"}";
		assertEquals(e, r);

		client.closeQuietly();
	}

	//====================================================================================================
	// Test multi-part parameter keys on bean properties of type array/Collection (i.e. &key=val1,&key=val2)
	// using URLENC_expandedParams property.
	// A simple round-trip test to verify that both serializing and parsing works.
	//====================================================================================================
	@Test
	public void testFormPostsWithMultiParamsUsingProperty() throws Exception {
		RestClient client = TestMicroservice.client()
			.contentType("application/x-www-form-urlencoded")
			.accept("application/x-www-form-urlencoded")
			.build();
		String r;
		String url = URL + "/testFormPostsWithMultiParamsUsingProperty";

		String in = ""
			+ "f01=a&f01=b"
			+ "&f02=c&f02=d"
			+ "&f03=1&f03=2"
			+ "&f04=3&f04=4"
			+ "&f05=@(e,f)&f05=@(g,h)"
			+ "&f06=@(i,j)&f06=@(k,l)"
			+ "&f07=(a=a,b=1,c=true)&f07=(a=b,b=2,c=false)"
			+ "&f08=(a=a,b=1,c=true)&f08=(a=b,b=2,c=false)"
			+ "&f09=@((a=a,b=1,c=true))&f09=@((a=b,b=2,c=false))"
			+ "&f10=@((a=a,b=1,c=true))&f10=@((a=b,b=2,c=false))"
			+ "&f11=a&f11=b"
			+ "&f12=c&f12=d"
			+ "&f13=1&f13=2"
			+ "&f14=3&f14=4"
			+ "&f15=@(e,f)&f15=@(g,h)"
			+ "&f16=@(i,j)&f16=@(k,l)"
			+ "&f17=(a=a,b=1,c=true)&f17=(a=b,b=2,c=false)"
			+ "&f18=(a=a,b=1,c=true)&f18=(a=b,b=2,c=false)"
			+ "&f19=@((a=a,b=1,c=true))&f19=@((a=b,b=2,c=false))"
			+ "&f20=@((a=a,b=1,c=true))&f20=@((a=b,b=2,c=false))";
		r = client.doPost(url, new StringEntity(in)).getResponseAsString();
		assertEquals(in, r);

		client.closeQuietly();
	}

	//====================================================================================================
	// Test multi-part parameter keys on bean properties of type array/Collection (i.e. &key=val1,&key=val2)
	// using @UrlEncoding(expandedParams=true) annotation.
	// A simple round-trip test to verify that both serializing and parsing works.
	//====================================================================================================
	@Test
	public void testFormPostsWithMultiParamsUsingAnnotation() throws Exception {
		RestClient client = TestMicroservice.client()
			.contentType("application/x-www-form-urlencoded")
			.accept("application/x-www-form-urlencoded")
			.build();
		String r;
		String url = URL + "/testFormPostsWithMultiParamsUsingAnnotation";

		String in = ""
			+ "f01=a&f01=b"
			+ "&f02=c&f02=d"
			+ "&f03=1&f03=2"
			+ "&f04=3&f04=4"
			+ "&f05=@(e,f)&f05=@(g,h)"
			+ "&f06=@(i,j)&f06=@(k,l)"
			+ "&f07=(a=a,b=1,c=true)&f07=(a=b,b=2,c=false)"
			+ "&f08=(a=a,b=1,c=true)&f08=(a=b,b=2,c=false)"
			+ "&f09=@((a=a,b=1,c=true))&f09=@((a=b,b=2,c=false))"
			+ "&f10=@((a=a,b=1,c=true))&f10=@((a=b,b=2,c=false))"
			+ "&f11=a&f11=b"
			+ "&f12=c&f12=d"
			+ "&f13=1&f13=2"
			+ "&f14=3&f14=4"
			+ "&f15=@(e,f)&f15=@(g,h)"
			+ "&f16=@(i,j)&f16=@(k,l)"
			+ "&f17=(a=a,b=1,c=true)&f17=(a=b,b=2,c=false)"
			+ "&f18=(a=a,b=1,c=true)&f18=(a=b,b=2,c=false)"
			+ "&f19=@((a=a,b=1,c=true))&f19=@((a=b,b=2,c=false))"
			+ "&f20=@((a=a,b=1,c=true))&f20=@((a=b,b=2,c=false))";
		r = client.doPost(url, new StringEntity(in)).getResponseAsString();
		assertEquals(in, r);

		client.closeQuietly();
	}


	//====================================================================================================
	// Test other available object types as parameters.
	//====================================================================================================

	@Test
	public void testOtherResourceBundle() throws Exception {
		String r = CLIENT.doGet(URL + "/otherObjects/ResourceBundle").acceptLanguage("en-US").getResponseAsString();
		assertEquals("\"bar\"", r);
		r = CLIENT.doGet(URL + "/otherObjects/ResourceBundle").acceptLanguage("ja-JP").getResponseAsString();
		assertEquals("\"baz\"", r);
	}

	@Test
	public void testOtherMessages() throws Exception {
		String r = CLIENT.doGet(URL + "/otherObjects/MessageBundle").acceptLanguage("en-US").getResponseAsString();
		assertEquals("\"bar\"", r);
		r = CLIENT.doGet(URL + "/otherObjects/MessageBundle").acceptLanguage("ja-JP").getResponseAsString();
		assertEquals("\"baz\"", r);
	}

	@Test
	public void testOtherInputStream() throws IOException {
		String r = CLIENT.doPost(URL + "/otherObjects/InputStream").input(new StringReader("foo")).getResponseAsString();
		assertEquals("\"foo\"", r);
	}

	@Test
	public void testOtherServletInputStream() throws Exception {
		String r = CLIENT.doPost(URL + "/otherObjects/ServletInputStream").input(new StringReader("foo")).getResponseAsString();
		assertEquals("\"foo\"", r);
	}

	@Test
	public void testOtherReader() throws Exception {
		String r = CLIENT.doPost(URL + "/otherObjects/Reader").input(new StringReader("foo")).getResponseAsString();
		assertEquals("\"foo\"", r);
	}

	@Test
	public void testOtherOutputStream() throws Exception {
		String r = CLIENT.doGet(URL + "/otherObjects/OutputStream").getResponseAsString();
		assertEquals("OK", r);
	}

	@Test
	public void testOtherServletOutputStream() throws Exception {
		String r = CLIENT.doGet(URL + "/otherObjects/ServletOutputStream").getResponseAsString();
		assertEquals("OK", r);
	}

	@Test
	public void testOtherWriter() throws Exception {
		String r = CLIENT.doGet(URL + "/otherObjects/Writer").getResponseAsString();
		assertEquals("OK", r);
	}

	@Test
	public void testOtherRequestHeaders() throws Exception {
		String r = CLIENT.doGet(URL + "/otherObjects/RequestHeaders").getResponseAsString();
		assertEquals("true", r);
	}

	@Test
	public void testOtherRequestQuery() throws Exception {
		String r = CLIENT.doGet(URL + "/otherObjects/RequestQuery").getResponseAsString();
		assertEquals("true", r);
	}

	@Test
	public void testOtherRequestFormData() throws Exception {
		String r = CLIENT.doGet(URL + "/otherObjects/RequestFormData").getResponseAsString();
		assertEquals("true", r);
	}

	@Test
	public void testOtherHttpMethod() throws Exception {
		String r = CLIENT.doGet(URL + "/otherObjects/HttpMethod").getResponseAsString();
		assertEquals("\"GET\"", r);
	}

	@Test
	public void testOtherRestLogger() throws Exception {
		String r = CLIENT.doGet(URL + "/otherObjects/RestLogger").getResponseAsString();
		assertEquals("true", r);
	}

	@Test
	public void testOtherRestContext() throws Exception {
		String r = CLIENT.doGet(URL + "/otherObjects/RestContext").getResponseAsString();
		assertEquals("true", r);
	}

	@Test
	public void testOtherParser() throws Exception {
		String r = CLIENT.doGet(URL + "/otherObjects/Parser").getResponseAsString();
		assertEquals("\"org.apache.juneau.json.JsonParser\"", r);
	}

	@Test
	public void testOtherLocale() throws Exception {
		String r = CLIENT.doGet(URL + "/otherObjects/Locale").acceptLanguage("en-US").getResponseAsString();
		assertEquals("\"en_US\"", r);
		r = CLIENT.doGet(URL + "/otherObjects/Locale").acceptLanguage("ja-JP").getResponseAsString();
		assertEquals("\"ja_JP\"", r);
	}

	@Test
	public void testOtherSwagger() throws Exception {
		String r = CLIENT.doGet(URL + "/otherObjects/Swagger").getResponseAsString();
		assertEquals("true", r);
	}

	@Test
	public void testOtherRequestPathMatch() throws Exception {
		String r = CLIENT.doGet(URL + "/otherObjects/RequestPathMatch").getResponseAsString();
		assertEquals("true", r);
	}

	@Test
	public void testOtherRequestBody() throws Exception {
		String r = CLIENT.doGet(URL + "/otherObjects/RequestBody").getResponseAsString();
		assertEquals("true", r);
	}

	@Test
	public void testOtherConfigFile() throws Exception {
		String r = CLIENT.doGet(URL + "/otherObjects/ConfigFile").getResponseAsString();
		assertEquals("true", r);
	}
}
