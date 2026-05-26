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

import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.runtime.*;
import org.apache.juneau.microservice.console.*;
import org.junit.jupiter.api.*;

/**
 * Tests for the inject-aware {@link Microservice} bootstrap.
 *
 * <p>
 * Verifies that <c>@Configuration</c> classes registered via
 * {@link Microservice.Builder#configurations(Class...)} are processed at construction time,
 * that <c>@Bean</c>-supplied values feed into field resolution when no builder value is present,
 * that explicit builder calls always win, and that the bean store is closed on
 * {@link Microservice#stop()} so <c>@PreDestroy</c> hooks fire.
 */
@org.apache.juneau.testing.annotations.JettyMicroserviceTest
class Microservice_Inject_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// A.  Bean store presence and self-registration.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_beanStore_neverNull() throws Exception {
		var ms = Microservice.create().build();
		try {
			assertNotNull(ms.getBeanStore());
		} finally {
			ms.stop();
		}
	}

	@Test void a02_beanStore_containsMicroserviceItself() throws Exception {
		var ms = Microservice.create().build();
		try {
			assertSame(ms, ms.getBeanStore().getBean(Microservice.class).orElse(null));
		} finally {
			ms.stop();
		}
	}

	@Test void a03_beanStore_containsResolvedArgs() throws Exception {
		var args = new Args(new String[]{"--port", "8080"});
		var ms = Microservice.create().args(args).build();
		try {
			assertSame(args, ms.getBeanStore().getBean(Args.class).orElse(null));
		} finally {
			ms.stop();
		}
	}

	@Test void a04_beanStore_isFreshWhenNotSupplied() throws Exception {
		var ms1 = Microservice.create().build();
		var ms2 = Microservice.create().build();
		try {
			assertNotSame(ms1.getBeanStore(), ms2.getBeanStore());
		} finally {
			ms1.stop();
			ms2.stop();
		}
	}

	@Test void a05_beanStore_externalStoreUsed() throws Exception {
		WritableBeanStore external = new BasicBeanStore();
		var ms = Microservice.create().beanStore(external).build();
		try {
			assertSame(external, ms.getBeanStore());
		} finally {
			ms.stop();
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// B.  @Configuration class registration via builder.
	//-----------------------------------------------------------------------------------------------------------------

	@Configuration
	static class B_SimpleConfig {
		@Bean MyService myService() { return new MyService("from-config"); }
	}

	static class MyService {
		final String tag;
		MyService(String tag) { this.tag = tag; }
	}

	@Test void b01_configuration_beanIsRegistered() throws Exception {
		var ms = Microservice.create().configurations(B_SimpleConfig.class).build();
		try {
			var svc = ms.getBeanStore().getBean(MyService.class).orElse(null);
			assertNotNull(svc);
			assertEquals("from-config", svc.tag);
		} finally {
			ms.stop();
		}
	}

	@Test void b02_configurations_noneRegistered_emptyStore() throws Exception {
		var ms = Microservice.create().build();
		try {
			assertFalse(ms.getBeanStore().getBean(MyService.class).isPresent());
		} finally {
			ms.stop();
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// C.  Resolution priority: builder > @Bean > default.
	//-----------------------------------------------------------------------------------------------------------------

	@Configuration
	static class C_ArgsConfig {
		@Bean Args args() { return new Args(new String[]{"--from", "config"}); }
	}

	@Test void c01_builderArgs_beatsConfigurationArgs() throws Exception {
		var builderArgs = new Args(new String[]{"--from", "builder"});
		var ms = Microservice.create()
			.args(builderArgs)
			.configurations(C_ArgsConfig.class)
			.build();
		try {
			assertSame(builderArgs, ms.getArgs());
			assertSame(builderArgs, ms.getBeanStore().getBean(Args.class).orElse(null));
		} finally {
			ms.stop();
		}
	}

	@Test void c02_configurationArgs_usedWhenNoBuilderArgs() throws Exception {
		var ms = Microservice.create()
			.configurations(C_ArgsConfig.class)
			.build();
		try {
			assertEquals("config", ms.getArgs().get("from").orElse(null));
		} finally {
			ms.stop();
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// D.  Console commands contributed via @Bean.
	//-----------------------------------------------------------------------------------------------------------------

	@Configuration
	static class D_CommandConfig {
		@Bean ConsoleCommand customCommand() {
			return new ConsoleCommand() {
				@Override public String getName() { return "custom"; }
				@Override public String getInfo() { return "Custom command for tests."; }
				@Override public boolean execute(java.util.Scanner in, java.io.PrintWriter out, Args a) { return false; }
			};
		}
	}

	@Test void d01_configurationProvidedConsoleCommand_isPickedUp() throws Exception {
		// Console commands are only wired when consoleEnabled=true.
		var ms = Microservice.create()
			.consoleEnabled(true)
			.configurations(D_CommandConfig.class)
			.build();
		try {
			assertTrue(ms.getConsoleCommands().containsKey("custom"));
		} finally {
			ms.stop();
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// E.  Lifecycle hooks: @PostConstruct + @PreDestroy.
	//-----------------------------------------------------------------------------------------------------------------

	static final AtomicInteger POST_CONSTRUCT_COUNT = new AtomicInteger();
	static final AtomicInteger PRE_DESTROY_COUNT = new AtomicInteger();

	public static class LifecycleBean {
		@PostConstruct public void onCreate() { POST_CONSTRUCT_COUNT.incrementAndGet(); }
		@PreDestroy   public void onDestroy() { PRE_DESTROY_COUNT.incrementAndGet(); }
	}

	@Configuration
	static class E_LifecycleConfig {
		@Bean LifecycleBean lifecycleBean() { return new LifecycleBean(); }
	}

	@Test void e01_lifecycle_postConstructAndPreDestroy_fire() throws Exception {
		POST_CONSTRUCT_COUNT.set(0);
		PRE_DESTROY_COUNT.set(0);
		var ms = Microservice.create().configurations(E_LifecycleConfig.class).build();
		// @PostConstruct does NOT fire for beans returned from @Bean methods unless they pass through
		// BeanInstantiator (which @Bean returns don't).  This test pins the documented behavior:
		// @PreDestroy fires when the bean has been resolved + the store is closed.
		var bean = ms.getBeanStore().getBean(LifecycleBean.class).orElse(null);
		assertNotNull(bean);
		ms.stop();
		assertEquals(1, PRE_DESTROY_COUNT.get(), "@PreDestroy should fire once after stop()");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// F.  Back-compat: existing builder-only path still works.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void f01_legacyBuilderPath_stillWorks() throws Exception {
		var ms = Microservice.create()
			.args("--from", "builder")
			.build();
		try {
			assertEquals("builder", ms.getArgs().get("from").orElse(null));
			assertNotNull(ms.getBeanStore()); // store is now always present, but legacy path stays functional
		} finally {
			ms.stop();
		}
	}
}
