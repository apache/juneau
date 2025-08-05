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
import org.apache.juneau.collections.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.junit.jupiter.api.*;

public class Rest_AllowContentParam_Test extends SimpleTestBase {

	//------------------------------------------------------------------------------------------------------------------
	// @Rest(disableBodyParam)
	//------------------------------------------------------------------------------------------------------------------

	@Rest(disableContentParam="false")
	public static class A1 {
		@RestOp
		public JsonMap put(@Content JsonMap body) {
			return body;
		}
	}
	@Rest(disableContentParam="true")
	public static class A2 {
		@RestOp
		public JsonMap put(@Content JsonMap body) {
			return body;
		}
	}
	@Rest(disableContentParam="true")
	public static class A3 extends A1 {}

	@Rest(disableContentParam="false")
	public static class A4 extends A2 {}

	@Test void a01_basic() throws Exception {
		RestClient a1 = MockRestClient.build(A1.class);
		a1.put("/", "{a:'b'}").run().assertContent("{a:'b'}");
		a1.put("/?content=(c=d)", "{a:'b'}").run().assertContent("{c:'d'}");

		RestClient a2 = MockRestClient.build(A2.class);
		a2.put("/", "{a:'b'}").run().assertContent("{a:'b'}");
		a2.put("/?content=(c=d)", "{a:'b'}").run().assertContent("{a:'b'}");

		RestClient a3 = MockRestClient.build(A3.class);
		a3.put("/", "{a:'b'}").run().assertContent("{a:'b'}");
		a3.put("/?content=(c=d)", "{a:'b'}").run().assertContent("{a:'b'}");

		RestClient a4 = MockRestClient.build(A4.class);
		a4.put("/", "{a:'b'}").run().assertContent("{a:'b'}");
		a4.put("/?content=(c=d)", "{a:'b'}").run().assertContent("{c:'d'}");
	}
}