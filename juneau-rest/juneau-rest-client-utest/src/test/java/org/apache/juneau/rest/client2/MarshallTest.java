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
package org.apache.juneau.rest.client2;

import static org.junit.Assert.*;

import org.apache.juneau.http.annotation.Body;
import org.apache.juneau.http.annotation.Header;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock2.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests the @Body annotation.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MarshallTest {

	public static class Bean {
		public int f;

		public static Bean create() {
			Bean b = new Bean();
			b.f = 1;
			return b;
		}
	}

	//=================================================================================================================
	// Basic tests - JSON
	//=================================================================================================================

	@Rest(debug="true")
	public static class A extends BasicRest {
		@RestMethod
		public Bean postA01(@Body Bean b, @Header("Accept") String accept, @Header("Content-Type") String ct, @Header("X-Accept") String xaccept, @Header("X-Content-Type") String xct) {
			assertEquals(xaccept, accept);
			assertEquals(xct, ct);
			return b;
		}
	}

	private static RestClient a1 = MockRestClient.create(A.class).simpleJson().build();

//	@Test
//	public void a01_int() throws Exception {
//		Bean b = Bean.create();
//		a1.post("/a01", b)
//			.header("X-Accept", "application/json+simple")
//			.header("X-Content-Type", "application/json+simple")
//			.run()
//			.assertStatusCode(200)
//			.getBody().assertValue("{f:1}");
//	}
}