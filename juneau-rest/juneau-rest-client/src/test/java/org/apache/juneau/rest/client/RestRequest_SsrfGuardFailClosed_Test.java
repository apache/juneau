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
import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.http.remote.*;
import org.junit.jupiter.api.*;

/**
 * Red-on-broken verification-gate test for the design's "unknown/caller-supplied {@link HttpTransport} fails
 * closed" requirement (see {@code TODO-392-remote-url-ssrf-resolved-address.md} "Test notes" and
 * {@link HttpTransport#supportsUrlPolicy()}).
 *
 * <p>
 * {@link StubTransport} never overrides {@link HttpTransport#supportsUrlPolicy()}, so it inherits the
 * fail-closed {@code false} default — modeling any third-party/caller-supplied transport that has not
 * affirmatively opted in to the SSRF guardrail's connect-time contract. This must fail against the pre-guardrail
 * behavior (which had no such check and would have connected regardless) and pass now that
 * {@code RestRequest#run()} rejects policy-covered requests against it.
 */
class RestRequest_SsrfGuardFailClosed_Test extends TestBase {

	@Remote(path = "/api")
	interface TestApi {
		@RemoteGet("/echo")
		String echo();
	}

	/** A minimal {@link HttpTransport} that does not affirmatively opt in to {@link HttpTransport#supportsUrlPolicy()}. */
	private static final class StubTransport implements HttpTransport {
		boolean executed;

		@Override
		@SuppressWarnings({
			"resource" // Hands the built TransportResponse to the caller, mirroring the real HttpTransport.execute() contract; Eclipse JDT @Owning warning is by design.
		})
		public TransportResponse execute(TransportRequest request) {
			executed = true;
			return TransportResponse.builder().statusCode(200).body(new ByteArrayInputStream(new byte[0])).build();
		}
	}

	@Test void a01_policyCoveredRequest_unknownTransport_failsClosed_beforeAnyExecute() throws Exception {
		@SuppressWarnings({
			"resource" // stub is closed transitively -- RestClient.close() (invoked by the try-with-resources below) delegates to transport.close().
		})
		var stub = new StubTransport();
		try (var client = RestClient.builder().transport(stub).rootUrl("http://example.com").build()) {
			var proxy = client.remote(TestApi.class);
			// The proxy interface method does not declare `throws IOException`, so the checked TransportException
			// thrown by RestRequest#run() surfaces through the JDK dynamic-proxy dispatch as an
			// UndeclaredThrowableException -- assert on its TransportException cause.
			var thrown = assertThrows(UndeclaredThrowableException.class, proxy::echo);
			assertInstanceOf(TransportException.class, thrown.getCause());
			assertTrue(thrown.getCause().getMessage().contains("cannot honor the SSRF guardrail"),
				"Unexpected message: " + thrown.getCause().getMessage());
			assertFalse(stub.executed, "the stub transport must never be invoked once the request is rejected as fail-closed");
		}
	}

	@Test void a02_policyCoveredRequest_unknownTransport_allowPrivateUrls_bypassesFailClosed_andExecutes() throws Exception {
		@SuppressWarnings({
			"resource" // stub is closed transitively -- RestClient.close() (invoked by the try-with-resources below) delegates to transport.close().
		})
		var stub = new StubTransport();
		try (var client = RestClient.builder().transport(stub).rootUrl("http://example.com").allowPrivateUrls(true).build()) {
			// allowPrivateUrls(true) deactivates the guard entirely (isSsrfGuardActive() == false), so the
			// fail-closed check for an unknown transport's supportsUrlPolicy() is never reached.
			client.remote(TestApi.class).echo();
			assertTrue(stub.executed, "with allowPrivateUrls(true) the guard is inactive, so the stub transport must be invoked normally");
		}
	}
}
