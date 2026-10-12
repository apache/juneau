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

import org.junit.jupiter.api.*;

/**
 * Verifies that {@link JavaHttpTransport} reports a refused pin-on-connect for an SSRF-guard-active request as a
 * {@link TransportException} (with the policy's {@link IllegalArgumentException} as its cause), per the
 * {@link PolicyEnforcedRedirects.HopExecutor} contract.
 *
 * <p>
 * The pin is refused before any request is sent, so no server is needed.  The unguarded case checks that a request the
 * JDK refuses to build is not misreported as a Host-header pinning failure.
 */
@SuppressWarnings({
	"resource" // The transport and any response are short-lived test fixtures; no request reaches the network.
})
class JavaHttpTransport_SsrfGuard_Test {

	private static TransportRequest guarded(String uri) {
		return TransportRequest.builder().method("GET").uri(uri).remoteUrlPolicy(true, false).build();
	}

	private static TransportException assertPinRefused(String uri) {
		var transport = JavaHttpTransport.create();
		var ex = assertThrows(TransportException.class, () -> transport.execute(guarded(uri)).close());
		assertInstanceOf(IllegalArgumentException.class, ex.getCause());
		return ex;
	}

	@Test
	void a01_loopbackLiteral_pinRefused_transportException() {
		var ex = assertPinRefused("http://127.0.0.1:1/x");
		assertTrue(ex.getMessage().contains("No allowed address for host '127.0.0.1'"), ex.getMessage());
	}

	@Test
	void a02_numericLoopbackHost_pinRefused_transportException() {
		// 2130706433 is 127.0.0.1 written as one 32-bit number; the pin resolves it to loopback and refuses it.
		var ex = assertPinRefused("http://2130706433:1/x");
		assertTrue(ex.getMessage().contains("No allowed address for host '2130706433'"), ex.getMessage());
	}

	@Test
	void a03_underscoreHost_pinRefused_transportException() {
		var ex = assertPinRefused("http://my_service:1/x");
		assertTrue(ex.getMessage().contains("my_service"), ex.getMessage());
	}

	@Test
	void b01_unguarded_underscoreHost_notReportedAsHostHeaderFailure() {
		var transport = JavaHttpTransport.create();
		var request = TransportRequest.builder().method("GET").uri("http://my_service:1/x").build();
		var ex = assertThrows(TransportException.class, () -> transport.execute(request).close());
		assertInstanceOf(IllegalArgumentException.class, ex.getCause());
		assertFalse(ex.getMessage().contains("Host header"), ex.getMessage());
		assertTrue(ex.getMessage().contains("http://my_service:1/x"), ex.getMessage());
	}
}
