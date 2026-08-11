/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.rest.client;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.lang.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link RestRequest#run()}.
 */
@SuppressWarnings("resource") // 'transport' lambdas build a TransportResponse per call; the built response is handed to (and closed by) the enclosing RestResponse/RestClient under test.
class RestRequest_Run_Test extends TestBase {

	/**
	 * Ensures a thrown {@code onConnect} interceptor does not leave the already-assigned {@link RestResponse}
	 * unclosed — since it never reaches the caller (who would otherwise be responsible for closing it),
	 * {@link RestRequest#run()} itself must close it before the exception propagates.
	 */
	@Test
	void a01_throwingOnConnectInterceptor_closesTheAssignedResponse() throws Exception {
		var closed = Flag.create();
		HttpTransport transport = tReq -> TransportResponse.builder()
			.statusCode(200)
			.closeCallback(closed::set)
			.build();
		RestCallInterceptor faulty = new RestCallInterceptor() {
			@Override public void onConnect(RestRequest req, RestResponse res) throws Exception {
				throw new IOException("boom");
			}
		};
		try (var client = RestClient.builder().transport(transport).interceptors(faulty).build()) {
			assertThrows(TransportException.class, () -> client.get("http://x/").run());
			assertTrue(closed.isSet(), "a throwing onConnect interceptor must not leak the assigned response");
		}
	}

	/**
	 * Contrast case: a well-behaved {@code onConnect} interceptor lets the response reach the caller
	 * unclosed, exactly per {@link RestRequest#run()}'s "caller is responsible for closing" contract.
	 */
	@Test
	void a02_successfulRun_leavesTheResponseOpenForTheCaller() throws Exception {
		var closed = Flag.create();
		HttpTransport transport = tReq -> TransportResponse.builder()
			.statusCode(200)
			.closeCallback(closed::set)
			.build();
		try (var client = RestClient.builder().transport(transport).build()) {
			try (var res = client.get("http://x/").run()) {
				assertFalse(closed.isSet(), "run() must not close a response it successfully returns to the caller");
			}
		}
		assertTrue(closed.isSet(), "the caller's own try-with-resources close must still work as normal");
	}
}
