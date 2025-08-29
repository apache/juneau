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

import org.apache.juneau.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.mock.*;
import org.junit.jupiter.api.*;

class HasFormData_Test extends SimpleTestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Simple tests
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A {
		@RestPost
		public String a(RestRequest req, @HasFormData("p1") boolean p1, @HasFormData("p2") Boolean p2) throws Exception {
			var f = req.getFormParams();
			return "p1=["+p1+","+f.contains("p1")+"],p2=["+p2+","+f.contains("p2")+"]";
		}
	}

	@Test void a01_basic() throws Exception {
		var a = MockRestClient.build(A.class);
		a.post("/a", "p1=p1&p2=2").run().assertContent("p1=[true,true],p2=[true,true]");
		a.post("/a", "p1&p2").run().assertContent("p1=[true,true],p2=[true,true]");
		a.post("/a", "p1=&p2=").run().assertContent("p1=[true,true],p2=[true,true]");
		a.post("/a", null).run().assertContent("p1=[false,false],p2=[false,false]");
		a.post("/a", "p1").run().assertContent("p1=[true,true],p2=[false,false]");
		a.post("/a", "p1=").run().assertContent("p1=[true,true],p2=[false,false]");
		a.post("/a", "p2").run().assertContent("p1=[false,false],p2=[true,true]");
		a.post("/a", "p2=").run().assertContent("p1=[false,false],p2=[true,true]");
		a.post("/a", "p1=foo&p2").run().assertContent("p1=[true,true],p2=[true,true]");
		a.post("/a", "p1&p2=1").run().assertContent("p1=[true,true],p2=[true,true]");
		String x = "a%2Fb%25c%3Dd+e"; // [x/y%z=a+b]
		a.post("/a", "p1="+x+"&p2=1").run().assertContent("p1=[true,true],p2=[true,true]");
	}
}