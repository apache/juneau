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

import org.apache.juneau.rest.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests related to @HasFormData annotation.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SuppressWarnings("javadoc")
public class HasFormDataAnnotationTest {
	
	//====================================================================================================
	// Simple tests
	//====================================================================================================
	@RestResource
	public static class A {
		@RestMethod(name=POST)
		public String post(RestRequest req, @HasFormData("p1") boolean p1, @HasFormData("p2") Boolean p2) throws Exception {
			RequestFormData f = req.getFormData();
			return "p1=["+p1+","+f.containsKey("p1")+"],p2=["+p2+","+f.containsKey("p2")+"]";
		}

	}
	static MockRest a = MockRest.create(A.class);
	
	@Test
	public void a01_post() throws Exception {
		a.request("POST", "").body("p1=p1&p2=2").execute().assertBody("p1=[true,true],p2=[true,true]");
		a.request("POST", "").body("p1&p2").execute().assertBody("p1=[true,true],p2=[true,true]");
		a.request("POST", "").body("p1=&p2=").execute().assertBody("p1=[true,true],p2=[true,true]");
		a.request("POST", "/").execute().assertBody("p1=[false,false],p2=[false,false]");
		a.request("POST", "").body("p1").execute().assertBody("p1=[true,true],p2=[false,false]");
		a.request("POST", "").body("p1=").execute().assertBody("p1=[true,true],p2=[false,false]");
		a.request("POST", "").body("p2").execute().assertBody("p1=[false,false],p2=[true,true]");
		a.request("POST", "").body("p2=").execute().assertBody("p1=[false,false],p2=[true,true]");
		a.request("POST", "").body("p1=foo&p2").execute().assertBody("p1=[true,true],p2=[true,true]");
		a.request("POST", "").body("p1&p2=1").execute().assertBody("p1=[true,true],p2=[true,true]");
		String x = "a%2Fb%25c%3Dd+e"; // [x/y%z=a+b]
		a.request("POST", "").body("p1="+x+"&p2=1").execute().assertBody("p1=[true,true],p2=[true,true]");
	}
}
