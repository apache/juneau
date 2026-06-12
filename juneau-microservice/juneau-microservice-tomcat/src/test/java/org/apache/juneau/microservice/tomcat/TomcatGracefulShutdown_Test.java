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
package org.apache.juneau.microservice.tomcat;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.server.health.*;
import org.junit.jupiter.api.*;

/**
 * Graceful-shutdown / readiness-gating tests for {@link TomcatServerComponent} (TODO-174a).
 *
 * <p>
 * Mirrors {@code JettyGracefulShutdown_Test} so both embedded servers share one zero-downtime contract:
 * <ul>
 * 	<li>On stop, readiness flips out of service <i>before</i> the connector is paused / the server stops; on start
 * 		it is marked ready.
 * 	<li>A sensible default drain timeout ({@link TomcatServerComponent#DEFAULT_STOP_TIMEOUT}) backs the bounded
 * 		in-flight drain, overridable via {@link TomcatSettings#stopTimeout(Duration)} / {@code Tomcat/stopTimeout}.
 * </ul>
 *
 * @since 10.0.0
 */
@org.apache.juneau.testing.annotations.TomcatMicroserviceTest
class TomcatGracefulShutdown_Test extends TestBase {

	private static Microservice create(Class<?>... configurations) throws Exception {
		var classes = new Class<?>[configurations.length + 1];
		System.arraycopy(configurations, 0, classes, 0, configurations.length);
		classes[configurations.length] = TomcatConfiguration.class;
		return Microservice.create().configurations(classes).build();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// A.  Readiness flips out of service on stop, ready on start.
	//-----------------------------------------------------------------------------------------------------------------

	@Configuration
	static class A_Config {
		@Bean TomcatSettings tomcatSettings() { return TomcatSettings.create().ports(0).build(); }
		@Bean ReadinessState readinessState() { return new ReadinessState(); }
	}

	@Test void a01_readinessFlipsOutOfServiceOnStop() throws Exception {
		var ms = create(A_Config.class);
		ReadinessState rs;
		boolean readyAfterStart;
		try {
			ms.start();
			rs = ms.getBeanStore().getBean(ReadinessState.class).orElseThrow();
			readyAfterStart = rs.isReady();
		} finally {
			ms.stop();
		}
		assertTrue(readyAfterStart, "Service should be ready after start");
		assertFalse(rs.isReady(), "Service should be out of service after stop");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// B.  Default drain timeout + settings round-trip (parity with the Jetty contract).
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_defaultStopTimeoutConstant() {
		assertEquals(Duration.ofSeconds(30), TomcatServerComponent.DEFAULT_STOP_TIMEOUT);
	}

	@Test void b02_settingsRoundTrip() {
		var s = TomcatSettings.create().stopTimeout(Duration.ofSeconds(5)).shutdownSettleDelay(Duration.ofSeconds(2)).build();
		assertEquals(Duration.ofSeconds(5), s.getStopTimeout());
		assertEquals(Duration.ofSeconds(2), s.getShutdownSettleDelay());
	}
}
