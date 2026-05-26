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
package org.apache.juneau.microservice;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.config.event.*;
import org.junit.jupiter.api.*;

/**
 * Verifies the {@link MicroserviceListener} fan-out behavior in {@link Microservice}.
 *
 * <p>
 * Listeners contributed via {@link Microservice.Builder#listener(MicroserviceListener)} and any
 * {@code @Bean MicroserviceListener} methods supplied by {@code @Configuration} classes are all invoked
 * for each lifecycle event ({@link Microservice#start()}, {@link Microservice#stop()}, and config-change
 * notifications). {@code onStart} runs in registration order; {@code onStop} runs in the reverse order.
 */
@org.apache.juneau.testing.annotations.JettyMicroserviceTest
class Microservice_Listener_Fanout_Test extends TestBase {

	/**
	 * Recording listener that captures invocation order across instances.
	 */
	static final class Recorder implements MicroserviceListener {
		final String name;
		final List<String> log;

		Recorder(String name, List<String> log) {
			this.name = name;
			this.log = log;
		}

		@Override
		public void onStart(Microservice ms) {
			log.add("start:" + name);
		}

		@Override
		public void onStop(Microservice ms) {
			log.add("stop:" + name);
		}

		@Override
		public void onConfigChange(Microservice ms, ConfigEvents events) {
			log.add("config:" + name);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// A.  Fan-out: explicit + @Bean listeners both fire in registration order on start, reverse on stop.
	//-----------------------------------------------------------------------------------------------------------------

	static final List<String> A_LOG = Collections.synchronizedList(new ArrayList<>());

	@Configuration
	static class A_TwoBeanListeners {
		@Bean(name = "first") MicroserviceListener first() { return new Recorder("first", A_LOG); }
		@Bean(name = "second") MicroserviceListener second() { return new Recorder("second", A_LOG); }
	}

	@Test void a01_fanOut_startThenReverseStop() throws Exception {
		A_LOG.clear();
		var explicit = new Recorder("explicit", A_LOG);
		var ms = Microservice.create()
			.listener(explicit)
			.configurations(A_TwoBeanListeners.class)
			.build();
		try {
			ms.start();
		} finally {
			ms.stop();
		}
		// All three listeners must fire once for start and once for stop.
		assertEquals(6, A_LOG.size(), () -> "log=" + A_LOG);
		var startLog = A_LOG.subList(0, 3);
		var stopLog = A_LOG.subList(3, 6);
		assertTrue(startLog.stream().allMatch(s -> s.startsWith("start:")));
		assertTrue(stopLog.stream().allMatch(s -> s.startsWith("stop:")));
		// Each listener fired in both phases.
		for (var who : List.of("first", "second", "explicit")) {
			assertTrue(startLog.contains("start:" + who), () -> "missing start:" + who + " in " + startLog);
			assertTrue(stopLog.contains("stop:" + who), () -> "missing stop:" + who + " in " + stopLog);
		}
		// stop order is the exact reverse of start order — this is the lifecycle contract.
		var startNames = startLog.stream().map(s -> s.substring("start:".length())).toList();
		var stopNames = stopLog.stream().map(s -> s.substring("stop:".length())).toList();
		var reversed = new ArrayList<>(startNames);
		Collections.reverse(reversed);
		assertEquals(reversed, stopNames, "stop order must be reverse of start order");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// B.  Default listener: registered iff no listeners exist after configurations + builder are processed.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_noListeners_defaultBasicListenerRegistered() throws Exception {
		var ms = Microservice.create().build();
		try {
			var listeners = ms.getBeanStore().getBeansOfType(MicroserviceListener.class).values();
			assertEquals(1, listeners.size());
			assertInstanceOf(BasicMicroserviceListener.class, listeners.iterator().next());
		} finally {
			ms.stop();
		}
	}

	@Configuration
	static class B_NamedListenerOnly {
		@Bean(name = "only") MicroserviceListener only() { return new BasicMicroserviceListener(); }
	}

	@Test void b02_namedListenerPresent_noDefaultAdded() throws Exception {
		var ms = Microservice.create().configurations(B_NamedListenerOnly.class).build();
		try {
			// Only the named listener should be present; no unnamed default added.
			var listeners = ms.getBeanStore().getBeansOfType(MicroserviceListener.class);
			assertEquals(1, listeners.size());
			assertEquals("only", listeners.keySet().iterator().next());
		} finally {
			ms.stop();
		}
	}
}
