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
package org.apache.juneau.rest.test.client;

import static org.apache.juneau.rest.testutils.TestUtils.*;
import static org.junit.Assert.*;

import java.util.concurrent.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.rest.RestRequest;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client2.*;
import org.apache.juneau.rest.client2.RestResponse;
import org.apache.juneau.rest.mock2.*;
import org.junit.*;
import org.junit.runners.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ClientFuturesTest {

	//=================================================================================================================
	// Basic tests
	//=================================================================================================================

	@Rest
	public static class A {
		@RestMethod
		public OMap get(RestRequest req) throws Exception {
			return OMap.of("foo","bar");
		}
	}
	static RestClient a = MockRestClient.build(A.class);

	@Test
	public void a01() throws Exception {
		Future<RestResponse> f = a.get("").runFuture();
		assertEquals(200, f.get().getStatusCode());

		Future<RestResponse> f2 = a.get("").runFuture();
		Future<OMap> m = f2.get().getBody().asFuture(OMap.class);
		assertObjectEquals("{foo:'bar'}", m.get());

		Future<RestResponse> f3 = a.get("").runFuture();
		Future<String> s = f3.get().getBody().asStringFuture();
		assertObjectEquals("'{foo:\\'bar\\'}'", s.get());
	}
}