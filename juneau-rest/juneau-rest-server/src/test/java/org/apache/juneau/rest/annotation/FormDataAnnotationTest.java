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

import org.apache.juneau.httppart.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.urlencoding.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests related to @FormData annotation.
 */
@SuppressWarnings("javadoc")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FormDataAnnotationTest {
	
	//====================================================================================================
	// Simple tests
	//====================================================================================================
	@RestResource(parsers=UrlEncodingParser.class)
	public static class A {
		@RestMethod(name=POST)
		public String post(RestRequest req, @FormData("p1") String p1, @FormData("p2") int p2) throws Exception {
			RequestFormData f = req.getFormData();
			return "p1=["+p1+","+req.getFormData().getString("p1")+","+f.get("p1", String.class)+"],p2=["+p2+","+req.getFormData().getString("p2")+","+f.get("p2", int.class)+"]";
		}
	}
	static MockRest a = MockRest.create(A.class);
	
	@Test
	public void a01() throws Exception {
		a.request("POST", "").body("p1=p1&p2=2").contentType("application/x-www-form-urlencoded").execute().assertBody("p1=[p1,p1,p1],p2=[2,2,2]");
		a.request("POST", "").body("p1&p2").contentType("application/x-www-form-urlencoded").execute().assertBody("p1=[null,null,null],p2=[0,null,0]");
		a.request("POST", "").body("p1=&p2=").contentType("application/x-www-form-urlencoded").execute().assertBody("p1=[,,],p2=[0,,0]");
		a.request("POST", "").contentType("application/x-www-form-urlencoded").execute().assertBody("p1=[null,null,null],p2=[0,null,0]");
		a.request("POST", "").body("p1").contentType("application/x-www-form-urlencoded").execute().assertBody("p1=[null,null,null],p2=[0,null,0]");
		a.request("POST", "").body("p1=").contentType("application/x-www-form-urlencoded").execute().assertBody("p1=[,,],p2=[0,null,0]");
		a.request("POST", "").body("p2").contentType("application/x-www-form-urlencoded").execute().assertBody("p1=[null,null,null],p2=[0,null,0]");
		a.request("POST", "").body("p2=").contentType("application/x-www-form-urlencoded").execute().assertBody("p1=[null,null,null],p2=[0,,0]");
		a.request("POST", "").body("p1=foo&p2").contentType("application/x-www-form-urlencoded").execute().assertBody("p1=[foo,foo,foo],p2=[0,null,0]");
		a.request("POST", "").body("p1&p2=1").contentType("application/x-www-form-urlencoded").execute().assertBody("p1=[null,null,null],p2=[1,1,1]");
		String x = "a%2Fb%25c%3Dd+e"; // [x/y%z=a+b]
		a.request("POST", "").body("p1="+x+"&p2=1").contentType("application/x-www-form-urlencoded").execute().assertBody("p1=[a/b%c=d e,a/b%c=d e,a/b%c=d e],p2=[1,1,1]");
	}
	
	//====================================================================================================
	// Plain parameters
	//====================================================================================================
	@RestResource
	public static class B {
		@RestMethod(name=POST)
		public String post(RestRequest req, @FormData(value="p1",parser=SimplePartParser.class) String p1) throws Exception {
			RequestFormData f = req.getFormData();
			return "p1=["+p1+","+req.getFormData().getString("p1")+","+f.get("p1", String.class)+"]";
		}
	}
	static MockRest b = MockRest.create(B.class);
	
	@Test
	public void b01() throws Exception {
		b.request("POST", "").body("p1=p1").contentType("application/x-www-form-urlencoded").execute().assertBody("p1=[p1,p1,p1]");
		b.request("POST", "").body("p1='p1'").contentType("application/x-www-form-urlencoded").execute().assertBody("p1=['p1','p1',p1]");
	}
}
