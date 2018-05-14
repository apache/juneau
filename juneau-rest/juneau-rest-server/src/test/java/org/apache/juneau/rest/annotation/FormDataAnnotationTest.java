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

import org.apache.juneau.*;
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
	
	//=================================================================================================================
	// Simple tests
	//=================================================================================================================

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
		a.request("POST", "", "p1=p1&p2=2").contentType("application/x-www-form-urlencoded").execute().assertBody("p1=[p1,p1,p1],p2=[2,2,2]");
		a.request("POST", "", "p1&p2").contentType("application/x-www-form-urlencoded").execute().assertBody("p1=[null,null,null],p2=[0,null,0]");
		a.request("POST", "", "p1=&p2=").contentType("application/x-www-form-urlencoded").execute().assertBody("p1=[,,],p2=[0,,0]");
		a.request("POST", "").contentType("application/x-www-form-urlencoded").execute().assertBody("p1=[null,null,null],p2=[0,null,0]");
		a.request("POST", "", "p1").contentType("application/x-www-form-urlencoded").execute().assertBody("p1=[null,null,null],p2=[0,null,0]");
		a.request("POST", "", "p1=").contentType("application/x-www-form-urlencoded").execute().assertBody("p1=[,,],p2=[0,null,0]");
		a.request("POST", "", "p2").contentType("application/x-www-form-urlencoded").execute().assertBody("p1=[null,null,null],p2=[0,null,0]");
		a.request("POST", "", "p2=").contentType("application/x-www-form-urlencoded").execute().assertBody("p1=[null,null,null],p2=[0,,0]");
		a.request("POST", "", "p1=foo&p2").contentType("application/x-www-form-urlencoded").execute().assertBody("p1=[foo,foo,foo],p2=[0,null,0]");
		a.request("POST", "", "p1&p2=1").contentType("application/x-www-form-urlencoded").execute().assertBody("p1=[null,null,null],p2=[1,1,1]");
		String x = "a%2Fb%25c%3Dd+e"; // [x/y%z=a+b]
		a.request("POST", "", "p1="+x+"&p2=1").contentType("application/x-www-form-urlencoded").execute().assertBody("p1=[a/b%c=d e,a/b%c=d e,a/b%c=d e],p2=[1,1,1]");
	}
	
	//=================================================================================================================
	// Plain parameters
	//=================================================================================================================

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
		b.request("POST", "", "p1=p1").contentType("application/x-www-form-urlencoded").execute().assertBody("p1=[p1,p1,p1]");
		b.request("POST", "", "p1='p1'").contentType("application/x-www-form-urlencoded").execute().assertBody("p1=['p1','p1',p1]");
	}
	
	//=================================================================================================================
	// Default values.
	//=================================================================================================================

	@RestResource
	public static class C {
		@RestMethod(name=POST, path="/defaultFormData", defaultFormData={"f1:1","f2=2"," f3 : 3 "})
		public ObjectMap c01(RequestFormData formData) {
			return new ObjectMap()
				.append("f1", formData.getString("f1"))
				.append("f2", formData.getString("f2"))
				.append("f3", formData.getString("f3"));
		}
		@RestMethod(name=POST, path="/annotatedFormData")
		public ObjectMap c02(@FormData("f1") String f1, @FormData("f2") String f2, @FormData("f3") String f3) {
			return new ObjectMap()
				.append("f1", f1)
				.append("f2", f2)
				.append("f3", f3);
		}
		@RestMethod(name=POST, path="/annotatedFormDataDefault")
		public ObjectMap c03(@FormData(value="f1",_default="1") String f1, @FormData(value="f2",_default="2") String f2, @FormData(value="f3",_default="3") String f3) {
			return new ObjectMap()
				.append("f1", f1)
				.append("f2", f2)
				.append("f3", f3);
		}
		@RestMethod(name=POST, path="/annotatedAndDefaultFormData", defaultFormData={"f1:1","f2=2"," f3 : 3 "})
		public ObjectMap c04(@FormData(value="f1",_default="4") String f1, @FormData(value="f2",_default="5") String f2, @FormData(value="f3",_default="6") String f3) {
			return new ObjectMap()
				.append("f1", f1)
				.append("f2", f2)
				.append("f3", f3);
		}
	}
	static MockRest c = MockRest.create(C.class);

	@Test
	public void c01_defaultFormData() throws Exception {
		c.request("POST", "/defaultFormData").contentType("application/x-www-form-urlencoded").execute().assertBody("{f1:'1',f2:'2',f3:'3'}");
		c.request("POST", "/defaultFormData").contentType("application/x-www-form-urlencoded").formData("f1",4).formData("f2",5).formData("f3",6).execute().assertBody("{f1:'4',f2:'5',f3:'6'}");
	}

	@Test
	public void c02_annotatedFormData() throws Exception {
		c.request("POST", "/annotatedFormData").contentType("application/x-www-form-urlencoded").execute().assertBody("{f1:null,f2:null,f3:null}");
		c.request("POST", "/annotatedFormData").contentType("application/x-www-form-urlencoded").formData("f1",4).formData("f2",5).formData("f3",6).execute().assertBody("{f1:'4',f2:'5',f3:'6'}");
	}

	@Test
	public void c03_annotatedFormDataDefault() throws Exception {
		c.request("POST", "/annotatedFormDataDefault").contentType("application/x-www-form-urlencoded").execute().assertBody("{f1:'1',f2:'2',f3:'3'}");
		c.request("POST", "/annotatedFormDataDefault").contentType("application/x-www-form-urlencoded").formData("f1",4).formData("f2",5).formData("f3",6).execute().assertBody("{f1:'4',f2:'5',f3:'6'}");
	}

	@Test
	public void c04_annotatedAndDefaultFormData() throws Exception {
		c.request("POST", "/annotatedAndDefaultFormData").contentType("application/x-www-form-urlencoded").execute().assertBody("{f1:'4',f2:'5',f3:'6'}");
		c.request("POST", "/annotatedAndDefaultFormData").contentType("application/x-www-form-urlencoded").formData("f1",7).formData("f2",8).formData("f3",9).execute().assertBody("{f1:'7',f2:'8',f3:'9'}");
	}
}
