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
package org.apache.juneau.rest.server;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.server.logger.*;
import org.apache.juneau.test.junit.*;
import org.junit.jupiter.api.*;

/**
 * Validates the {@code overridingParent} component on
 * {@link RestContext.Args} threads through to the {@code overridingParent} slot of the
 * freshly-constructed {@link BasicBeanStore} so test-time overrides win over the resource's
 * local {@code @Bean} factory methods.
 *
 * <p>
 * Modeled after {@code RestContext_Precedence_Test}'s {@code B_BeanBeatsSpring}: a
 * resource declares a local {@code @Bean CallLogger callLogger()} factory, the test installs
 * a {@link TestBeanStore} as the overlay, and the overlay's binding wins.
 */
class RestContext_Args_OverridingParent_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Marker beans
	//-----------------------------------------------------------------------------------------------------------------

	private static final CallLogger BEAN_LOGGER = BasicCallLogger.create(BasicBeanStore.INSTANCE).build();
	private static final CallLogger OVERRIDE_LOGGER = BasicCallLogger.create(BasicBeanStore.INSTANCE).build();

	//-----------------------------------------------------------------------------------------------------------------
	// a — Args.overridingParent shadows the resource's @Bean factory entry (CallLogger)
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A_OverlayWinsOverBean {
		@Bean static CallLogger callLoggerCapture;
		@Bean public CallLogger callLogger() { return BEAN_LOGGER; }
	}

	@Test
	void a01_overlay_shadows_atBeanFactory_forFrameworkType() throws Exception {
		var overlay = new TestBeanStore().override(CallLogger.class, OVERRIDE_LOGGER);

		var args = new RestContext.Args(A_OverlayWinsOverBean.class, null, null, A_OverlayWinsOverBean::new, "", null, overlay, null, RestContext.ContextKind.ROOT);
		new RestContext(args).postInit().postInitChildFirst();

		assertSame(OVERRIDE_LOGGER, A_OverlayWinsOverBean.callLoggerCapture,
			"Args.overridingParent should win over the resource's @Bean callLogger() factory");
	}

	@Test
	void a02_noOverlay_beanFactoryWins() throws Exception {
		// Reset capture from previous tests.
		A_NoOverlay.callLoggerCapture = null;

		var args = new RestContext.Args(A_NoOverlay.class, null, null, A_NoOverlay::new, "", null, null, null, RestContext.ContextKind.ROOT);
		new RestContext(args).postInit().postInitChildFirst();

		assertSame(BEAN_LOGGER, A_NoOverlay.callLoggerCapture,
			"Without an overlay, the @Bean factory is the only binding");
	}

	@Rest
	public static class A_NoOverlay {
		@Bean static CallLogger callLoggerCapture;
		@Bean public CallLogger callLogger() { return BEAN_LOGGER; }
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b — user-defined types: overlay also wins for non-framework beans
	//-----------------------------------------------------------------------------------------------------------------

	public static class CustomBean {
		final String marker;
		CustomBean(String marker) { this.marker = marker; }
	}

	private static final CustomBean BEAN_CUSTOM = new CustomBean("from-bean");
	private static final CustomBean OVERLAY_CUSTOM = new CustomBean("from-overlay");

	@Rest
	public static class B_OverlayWinsForUserBean {
		@Bean static CustomBean customBeanCapture;
		@Bean public CustomBean customBean() { return BEAN_CUSTOM; }
	}

	@Test
	void b01_overlay_shadows_atBeanFactory_forUserType() throws Exception {
		var overlay = new TestBeanStore().override(CustomBean.class, OVERLAY_CUSTOM);

		var args = new RestContext.Args(B_OverlayWinsForUserBean.class, null, null, B_OverlayWinsForUserBean::new, "", null, overlay, null, RestContext.ContextKind.ROOT);
		new RestContext(args).postInit().postInitChildFirst();

		assertSame(OVERLAY_CUSTOM, B_OverlayWinsForUserBean.customBeanCapture,
			"Overlay should win over @Bean CustomBean factory for user-defined types too");
	}

}
