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

import org.apache.juneau.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;
import org.junit.runners.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ClientFuturesTest {

	//=================================================================================================================
	// Basic tests
	//=================================================================================================================

	@RestResource
	public static class A {
		@RestMethod
		public ObjectMap get(RestRequest req) throws Exception {
			return new ObjectMap().append("foo","bar");
		}
	}
	static RestClient a = RestClient.create().mockHttpConnection(MockRest.create(A.class)).build();

	@Test
	public void a01() throws Exception {
		Future<Integer> f = a.doGet("").runFuture();
		assertEquals(200, f.get().intValue());

		Future<ObjectMap> f2 = a.doGet("").getResponseFuture(ObjectMap.class);
		assertObjectEquals("{foo:'bar'}", f2.get());

		Future<String> f3 = a.doGet("").getResponseAsStringFuture();
		assertObjectEquals("'{foo:\\'bar\\'}'", f3.get());
	}
}