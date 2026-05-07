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
package org.apache.juneau.rest;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.logger.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.stats.*;
import org.junit.jupiter.api.*;

/**
 * Validates the 9.5 precedence model for framework-managed beans on a {@link RestContext}:
 * <ol>
 * 	<li>Beans inherited from an "overriding parent" bean store (e.g. Spring) win.
 * 	<li>{@code @RestInject} factory methods on the resource win over defaults.
 * 	<li>Memoizer-backed framework defaults are the last-resort fallback.
 * </ol>
 *
 * <p>
 * Spring is simulated here by wiring a {@link BasicBeanStore} as the {@code overridingParent} of the
 * resource's bean store via {@link Rest#beanStore()} on a custom {@link BasicBeanStore} subclass.  This
 * avoids pulling in spring-boot at test time but exercises the same code path that
 * {@code SpringRestServlet.createBeanStore(...)} drives at runtime.
 */
class RestContext_Precedence_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Marker beans
	//-----------------------------------------------------------------------------------------------------------------

	private static final CallLogger SPRING_LOGGER = BasicCallLogger.create(BasicBeanStore.INSTANCE).build();
	private static final CallLogger RESTINJECT_LOGGER = BasicCallLogger.create(BasicBeanStore.INSTANCE).build();
	private static final ThrownStore RESTINJECT_THROWN_STORE = ThrownStore.create().build();

	//-----------------------------------------------------------------------------------------------------------------
	// Spring-substitute bean store (acts as the overriding parent layer)
	//-----------------------------------------------------------------------------------------------------------------

	/** A bean store seeded with a "Spring" {@link CallLogger} via the overriding-parent mechanism. */
	public static class SpringLikeBeanStore extends BasicBeanStore {
		protected SpringLikeBeanStore(Builder builder) {
			super(builder.overridingParent(BasicBeanStore.create().build().addBean(CallLogger.class, SPRING_LOGGER)));
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// 1. @RestInject beats default (no Spring layer present)
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A_RestInjectOnly {
		@RestInject static CallLogger callLoggerCapture;
		@RestInject public CallLogger callLogger() { return RESTINJECT_LOGGER; }
	}

	@Test
	void a01_restInject_beatsDefault() {
		MockRestClient.buildLax(A_RestInjectOnly.class);
		assertSame(RESTINJECT_LOGGER, A_RestInjectOnly.callLoggerCapture);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// 2. Spring (overriding parent) beats @RestInject
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(beanStore=SpringLikeBeanStore.class)
	public static class B_SpringWins {
		@RestInject static CallLogger callLoggerCapture;
		@RestInject public CallLogger callLogger() { return RESTINJECT_LOGGER; }
	}

	@Test
	void b01_spring_beatsRestInject() {
		MockRestClient.buildLax(B_SpringWins.class);
		assertSame(SPRING_LOGGER, B_SpringWins.callLoggerCapture, "Spring (overriding parent) should win over @RestInject method");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// 3. Spring (overriding parent) beats default (no @RestInject method)
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(beanStore=SpringLikeBeanStore.class)
	public static class C_SpringOnly {
		@RestInject static CallLogger callLoggerCapture;
	}

	@Test
	void c01_spring_beatsDefault() {
		MockRestClient.buildLax(C_SpringOnly.class);
		assertSame(SPRING_LOGGER, C_SpringOnly.callLoggerCapture);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// 4. With NO overriding-parent binding for the type, @RestInject still wins over the default
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(beanStore=SpringLikeBeanStore.class)
	public static class D_PartialSpring {
		@RestInject static CallLogger callLoggerCapture;
		@RestInject static ThrownStore thrownStoreCapture;
		@RestInject public ThrownStore thrownStore() { return RESTINJECT_THROWN_STORE; }
	}

	@Test
	void d01_partialSpring_restInjectStillBeatsDefaultForUnboundType() {
		MockRestClient.buildLax(D_PartialSpring.class);
		assertSame(SPRING_LOGGER, D_PartialSpring.callLoggerCapture);                  // Spring binding wins
		assertSame(RESTINJECT_THROWN_STORE, D_PartialSpring.thrownStoreCapture);       // @RestInject wins (Spring has no binding)
	}
}
