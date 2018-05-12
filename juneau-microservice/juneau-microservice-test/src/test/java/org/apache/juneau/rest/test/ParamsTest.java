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

import static org.junit.Assert.*;


import org.apache.http.entity.*;
import org.apache.juneau.*;
import org.apache.juneau.rest.client.*;
import org.junit.*;

public class ParamsTest extends RestTestcase {

	private static String URL = "/testParams";


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
}
