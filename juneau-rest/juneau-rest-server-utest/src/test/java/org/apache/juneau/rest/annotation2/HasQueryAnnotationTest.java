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

import org.apache.juneau.http.annotation.HasQuery;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock2.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests related to @HasQuery annotation.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HasQueryAnnotationTest {

	//=================================================================================================================
	// Simple tests
	//=================================================================================================================

	@RestResource
	public static class A {
		@RestMethod(name=GET,path="/")
		public String get(RestRequest req, @HasQuery("p1") boolean p1, @HasQuery("p2") Boolean p2) throws Exception {
			RequestQuery q = req.getQuery();
			return "p1=["+p1+","+q.containsKey("p1")+"],p2=["+p2+","+q.containsKey("p2")+"]";
		}
		@RestMethod(name=POST,path="/")
		public String post(RestRequest req, @HasQuery("p1") boolean p1, @HasQuery("p2") Boolean p2) throws Exception {
			RequestQuery q = req.getQuery();
			return "p1=["+p1+","+q.containsKey("p1")+"],p2=["+p2+","+q.containsKey("p2")+"]";
		}
	}
	static MockRest a = MockRest.build(A.class, null);

	@Test
	public void a01_get() throws Exception {
		a.get("?p1=p1&p2=2").execute().assertBody("p1=[true,true],p2=[true,true]");
		a.get("?p1&p2").execute().assertBody("p1=[true,true],p2=[true,true]");
		a.get("?p1=&p2=").execute().assertBody("p1=[true,true],p2=[true,true]");
		a.get("/").execute().assertBody("p1=[false,false],p2=[false,false]");
		a.get("?p1").execute().assertBody("p1=[true,true],p2=[false,false]");
		a.get("?p1=").execute().assertBody("p1=[true,true],p2=[false,false]");
		a.get("?p2").execute().assertBody("p1=[false,false],p2=[true,true]");
		a.get("?p2=").execute().assertBody("p1=[false,false],p2=[true,true]");
		a.get("?p1=foo&p2").execute().assertBody("p1=[true,true],p2=[true,true]");
		a.get("?p1&p2=1").execute().assertBody("p1=[true,true],p2=[true,true]");
		String x = "a%2Fb%25c%3Dd+e"; // [x/y%z=a+b]
		a.get("?p1="+x+"&p2=1").execute().assertBody("p1=[true,true],p2=[true,true]");
	}
	@Test
	public void a02_post() throws Exception {
		a.post("?p1=p1&p2=2", null).execute().assertBody("p1=[true,true],p2=[true,true]");
		a.post("?p1&p2", null).execute().assertBody("p1=[true,true],p2=[true,true]");
		a.post("?p1=&p2=", null).execute().assertBody("p1=[true,true],p2=[true,true]");
		a.post("/", null).execute().assertBody("p1=[false,false],p2=[false,false]");
		a.post("?p1", null).execute().assertBody("p1=[true,true],p2=[false,false]");
		a.post("?p1=", null).execute().assertBody("p1=[true,true],p2=[false,false]");
		a.post("?p2", null).execute().assertBody("p1=[false,false],p2=[true,true]");
		a.post("?p2=", null).execute().assertBody("p1=[false,false],p2=[true,true]");
		a.post("?p1=foo&p2", null).execute().assertBody("p1=[true,true],p2=[true,true]");
		a.post("?p1&p2=1", null).execute().assertBody("p1=[true,true],p2=[true,true]");
		String x = "a%2Fb%25c%3Dd+e"; // [x/y%z=a+b]
		a.post("?p1="+x+"&p2=1", null).execute().assertBody("p1=[true,true],p2=[true,true]");
	}
}
