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

import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.http.annotation.HasQuery;
import org.apache.juneau.rest.RestRequest;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.httppart.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class HasQuery_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Simple tests
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A {
		@RestGet
		public String a(RestRequest req, @HasQuery("p1") boolean p1, @HasQuery("p2") Boolean p2) throws Exception {
			RequestQueryParams q = req.getQueryParams();
			return "p1=["+p1+","+q.contains("p1")+"],p2=["+p2+","+q.contains("p2")+"]";
		}
		@RestPost
		public String b(RestRequest req, @HasQuery("p1") boolean p1, @HasQuery("p2") Boolean p2) throws Exception {
			RequestQueryParams q = req.getQueryParams();
			return "p1=["+p1+","+q.contains("p1")+"],p2=["+p2+","+q.contains("p2")+"]";
		}
	}

	@Test
	public void a01_basic() throws Exception {
		RestClient a = MockRestClient.build(A.class);

		a.get("/a?p1=p1&p2=2").run().assertContent("p1=[true,true],p2=[true,true]");
		a.get("/a?p1&p2").run().assertContent("p1=[true,true],p2=[true,true]");
		a.get("/a?p1=&p2=").run().assertContent("p1=[true,true],p2=[true,true]");
		a.get("/a").run().assertContent("p1=[false,false],p2=[false,false]");
		a.get("/a?p1").run().assertContent("p1=[true,true],p2=[false,false]");
		a.get("/a?p1=").run().assertContent("p1=[true,true],p2=[false,false]");
		a.get("/a?p2").run().assertContent("p1=[false,false],p2=[true,true]");
		a.get("/a?p2=").run().assertContent("p1=[false,false],p2=[true,true]");
		a.get("/a?p1=foo&p2").run().assertContent("p1=[true,true],p2=[true,true]");
		a.get("/a?p1&p2=1").run().assertContent("p1=[true,true],p2=[true,true]");
		String x1 = "a%2Fb%25c%3Dd+e"; // [x/y%z=a+b]
		a.get("/a?p1="+x1+"&p2=1").run().assertContent("p1=[true,true],p2=[true,true]");

		a.post("/b?p1=p1&p2=2", null).run().assertContent("p1=[true,true],p2=[true,true]");
		a.post("/b?p1&p2", null).run().assertContent("p1=[true,true],p2=[true,true]");
		a.post("/b?p1=&p2=", null).run().assertContent("p1=[true,true],p2=[true,true]");
		a.post("/b", null).run().assertContent("p1=[false,false],p2=[false,false]");
		a.post("/b?p1", null).run().assertContent("p1=[true,true],p2=[false,false]");
		a.post("/b?p1=", null).run().assertContent("p1=[true,true],p2=[false,false]");
		a.post("/b?p2", null).run().assertContent("p1=[false,false],p2=[true,true]");
		a.post("/b?p2=", null).run().assertContent("p1=[false,false],p2=[true,true]");
		a.post("/b?p1=foo&p2", null).run().assertContent("p1=[true,true],p2=[true,true]");
		a.post("/b?p1&p2=1", null).run().assertContent("p1=[true,true],p2=[true,true]");
		String x2 = "a%2Fb%25c%3Dd+e"; // [x/y%z=a+b]
		a.post("/b?p1="+x2+"&p2=1", null).run().assertContent("p1=[true,true],p2=[true,true]");
	}
}
