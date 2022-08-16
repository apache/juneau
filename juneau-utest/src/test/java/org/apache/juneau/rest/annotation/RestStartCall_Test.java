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

import org.apache.juneau.collections.*;
import org.apache.juneau.rest.RestRequest;
import org.apache.juneau.rest.RestResponse;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RestStartCall_Test {

	//------------------------------------------------------------------------------------------------------------------
	// @RestStartCall
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A extends A_Parent {
		private boolean start3Called;
		@RestStartCall
		public void start3() {
			start3Called = true;
		}
		@RestStartCall
		public void start4(HttpServletRequest req, HttpServletResponse res) {
			res.setHeader("start3-called", ""+start3Called);
			start3Called = false;
			if (res.getHeader("start4-called") != null)
				throw new RuntimeException("start4 called multiple times.");
			res.setHeader("start4-called", "true");
		}
		@RestGet(path="/")
		public JsonMap a(RestRequest req, RestResponse res) {
			return JsonMap.create()
				.append("1", res.getHeader("start1-called"))
				.append("2", res.getHeader("start2-called"))
				.append("3", res.getHeader("start3-called"))
				.append("4", res.getHeader("start4-called"));
		}
	}

	public static class A_Parent {
		private boolean start1Called;
		@RestStartCall
		public void start1() {
			start1Called = true;
		}
		@RestStartCall
		public void start2(HttpServletRequest req, HttpServletResponse res) {
			res.setHeader("start1-called", ""+start1Called);
			start1Called = false;
			if (res.getHeader("start2-called") != null)
				throw new RuntimeException("start2 called multiple times.");
			res.setHeader("start2-called", "true");
		}
	}

	@Test
	public void a01_startCall() throws Exception {
		RestClient a = MockRestClient.build(A.class);
		a.get("/").run().assertContent("{'1':'true','2':'true','3':'true','4':'true'}");
	}
}
