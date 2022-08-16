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

import javax.servlet.http.*;

import org.apache.juneau.http.header.*;
import org.apache.juneau.rest.RestRequest;
import org.apache.juneau.rest.RestResponse;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RestPostCall_Test {

	//------------------------------------------------------------------------------------------------------------------
	// @RestPostCall
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A extends A_Parent {
		private boolean post3Called;
		@RestPostCall
		public void post3() {
			post3Called = true;
		}
		@RestPostCall
		public void post4(HttpServletRequest req, HttpServletResponse res) {
			res.setHeader("post3-called", ""+post3Called);
			post3Called = false;
			if (res.getHeader("post4-called") != null)
				throw new RuntimeException("post4 called multiple times.");
			res.setHeader("post4-called", "true");
		}
		@RestGet(path="/")
		public String a() {
			return "OK";
		}
	}

	public static class A_Parent {
		private boolean post1Called;
		@RestPostCall
		public void post1() {
			post1Called = true;
		}
		@RestPostCall
		public void post2(Accept accept, RestRequest req, RestResponse res) {
			res.setHeader("post1-called", ""+post1Called);
			post1Called = false;
			if (res.getHeader("post2-called") != null)
				throw new RuntimeException("post2 called multiple times.");
			res.setHeader("post2-called", "true");
		}
	}

	@Test
	public void a01_postCall() throws Exception {
		RestClient a = MockRestClient.build(A.class);
		a.get("/").run()
			.assertHeader("post1-called").is("true")
			.assertHeader("post2-called").is("true")
			.assertHeader("post3-called").is("true")
			.assertHeader("post4-called").is("true");
	}
}
