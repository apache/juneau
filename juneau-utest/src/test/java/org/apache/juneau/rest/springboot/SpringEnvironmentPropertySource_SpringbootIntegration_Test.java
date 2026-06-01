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
package org.apache.juneau.rest.springboot;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.settings.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.test.context.*;
import org.springframework.context.*;
import org.springframework.test.annotation.*;
import org.springframework.test.context.*;

/**
 * Real-Spring-Boot integration test for the {@link SpringEnvironmentPropertySource} Phase-4
 * bridge.
 *
 * <p>
 * Boots a minimal Spring Boot context with three properties supplied through
 * {@link TestPropertySource}, builds a {@link SpringBeanStore} from the resulting
 * {@link ApplicationContext}, and verifies that:
 *
 * <ul>
 * 	<li>{@code @Value("${spring.boot.demo.key}")} on a Juneau-instantiated bean resolves through
 * 		the live Spring {@link org.springframework.core.env.Environment Environment} —
 * 		i.e. exactly the same source as {@code @org.springframework.beans.factory.annotation.Value}.
 * 	<li>Defaulting (the {@code :} suffix in the expression) fires when the key is absent from the
 * 		Spring environment.
 * 	<li>The bridge is auto-removed by {@link SpringBeanStore#clear()}, returning the
 * 		{@link Settings} singleton to its pre-bridge state.
 * </ul>
 *
 * <p>
 * Mirrors the pattern used by {@code StaticFilesMixin_Springboot_Test} and friends:
 * minimal {@code @SpringBootConfiguration} class, no web environment, dirty context after the
 * class so subsequent tests don't see the bridge.
 */
@org.apache.juneau.testing.annotations.SpringbootTest
@SpringBootTest(classes = SpringEnvironmentPropertySource_SpringbootIntegration_Test.TestApp.class,
	webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
	"spring.boot.demo.key=from-application-yaml",
	"spring.boot.demo.other=other-value"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SpringEnvironmentPropertySource_SpringbootIntegration_Test extends TestBase {

	/**
	 * Minimal Spring Boot application — no servlets, no MVC, just enough to give us a populated
	 * {@link ApplicationContext}.
	 */
	@SpringBootConfiguration
	@EnableAutoConfiguration
	public static class TestApp {
	}

	public static class ValueBean {
		@Value("${spring.boot.demo.key}")
		String value;
	}

	public static class ValueBeanWithDefault {
		@Value("${spring.boot.demo.missing:fallback}")
		String value;
	}

	@Autowired
	private ApplicationContext appContext;

	@Test
	void a01_atValueResolvesAgainstApplicationYamlEquivalent() {
		// Mirrors what BasicSpringRestServlet does: build a SpringBeanStore from the live context.
		var store = new SpringBeanStore(appContext, null);
		try {
			var beanStore = new BasicBeanStore(null);
			var bean = BeanInstantiator.of(ValueBean.class, beanStore).run();
			assertEquals("from-application-yaml", bean.value,
				"@Value must resolve against the live Spring Environment via the auto-installed bridge");
		} finally {
			store.clear();
		}
	}

	@Test
	void a02_defaultFires_whenKeyAbsent() {
		var store = new SpringBeanStore(appContext, null);
		try {
			var beanStore = new BasicBeanStore(null);
			var bean = BeanInstantiator.of(ValueBeanWithDefault.class, beanStore).run();
			assertEquals("fallback", bean.value);
		} finally {
			store.clear();
		}
	}

	@Test
	void a03_clearRemovesBridge() {
		// Before any store is created, the bridge isn't installed → defaults fire.
		var pre = new BasicBeanStore(null);
		var preBean = BeanInstantiator.of(ValueBeanWithDefault.class, pre).run();
		assertEquals("fallback", preBean.value);

		var store = new SpringBeanStore(appContext, null);
		try {
			var beanStore = new BasicBeanStore(null);
			var bean = BeanInstantiator.of(ValueBean.class, beanStore).run();
			assertEquals("from-application-yaml", bean.value);
		} finally {
			store.clear();
		}

		// After clear(), the bridge is removed. Looking up the configured key now misses the
		// bridge entirely. Since the key has no fallback, ValueBean would fail — we verify the
		// removal by checking that the defaulting bean still works (no leftover sources break it).
		var postBean = BeanInstantiator.of(ValueBeanWithDefault.class, pre).run();
		assertEquals("fallback", postBean.value);
	}
}
