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
package org.apache.juneau.rest.test;

import static org.apache.juneau.rest.test.TestUtils.*;
import static org.junit.Assert.*;

import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.client.*;
import org.junit.*;

public class ClientFuturesTest extends RestTestcase {

	private static String URL = "/testClientFutures";

	//====================================================================================================
	// Basic tests
	//====================================================================================================
	@Test
	public void test() throws Exception {
		RestClient client = null;
		try {
			ExecutorService es = new ThreadPoolExecutor(1, 1, 5, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(1));
			client = TestMicroservice.client().executorService(es, true).build();

			Future<Integer> f = client.doGet(URL).runFuture();
			assertEquals(200, f.get().intValue());

			Future<ObjectMap> f2 = client.doGet(URL).getResponseFuture(ObjectMap.class);
			assertObjectEquals("{foo:'bar'}", f2.get());

			Future<String> f3 = client.doGet(URL).getResponseAsStringFuture();
			assertObjectEquals("'{\"foo\":\"bar\"}'", f3.get());
		} finally {
			client.closeQuietly();
		}
	}
}