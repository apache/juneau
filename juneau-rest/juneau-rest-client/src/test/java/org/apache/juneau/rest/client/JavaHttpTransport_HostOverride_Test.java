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

import java.net.*;
import java.net.http.*;

import org.junit.jupiter.api.*;

/**
 * Verifies the {@code allowHostHeaderOverride()} opt-in of {@code JavaHttpTransportBuilder}, which sets a JVM-wide property.
 */
@SuppressWarnings("resource") // Transport instances are short-lived test fixtures.
class JavaHttpTransport_HostOverride_Test {

	private static final String PROP = "jdk.httpclient.allowRestrictedHeaders";

	private String saved;

	@BeforeEach void save() {
		saved = System.getProperty(PROP);
		System.clearProperty(PROP);
	}

	@AfterEach void restore() {
		if (saved == null)
			System.clearProperty(PROP);
		else
			System.setProperty(PROP, saved);
	}

	@Test void a01_defaultBuild_doesNotTouchTheProperty() {
		JavaHttpTransport.builder().build();
		assertNull(System.getProperty(PROP));
	}

	@Test void a02_optIn_addsHost() {
		JavaHttpTransport.builder().allowHostHeaderOverride().build();
		assertEquals("host", System.getProperty(PROP));
	}

	@Test void a03_optIn_appendsToExistingValue() {
		System.setProperty(PROP, "connection");
		JavaHttpTransport.builder().allowHostHeaderOverride().build();
		assertEquals("connection,host", System.getProperty(PROP));
	}

	@Test void a04_optIn_leavesAnExistingHostEntryAlone() {
		System.setProperty(PROP, "Host,connection");
		JavaHttpTransport.builder().allowHostHeaderOverride().build();
		assertEquals("Host,connection", System.getProperty(PROP));
	}

	@Test void a05_withoutOptIn_pinnedRequestWithHostOverrideFailsClosed() {
		// The JDK caches the restricted-header list on first use in the JVM; skip when that already allowed Host.
		var probe = HttpRequest.newBuilder(URI.create("http://127.0.0.1/"));
		try {
			probe.header("Host", "example.com");
			Assumptions.abort("the JVM already allows a Host header");
		} catch (IllegalArgumentException e) {
			// Restricted, as expected.
		}
		var transport = JavaHttpTransport.builder().build();
		var request = TransportRequest.builder().method("GET").uri("http://localhost:1/").remoteUrlPolicy(true, false).build();
		var e = assertThrows(TransportException.class, () -> transport.execute(request));
		assertNotNull(e.getMessage());
	}
}
