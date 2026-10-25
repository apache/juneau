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
package org.apache.juneau.rest.server.springboot;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.inject.Value;
import org.apache.juneau.commons.settings.Settings;
import org.apache.juneau.marshall.json.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.test.context.*;
import org.springframework.context.*;
import org.springframework.context.annotation.*;
import org.springframework.core.env.*;
import org.springframework.test.annotation.*;
import org.springframework.test.context.*;

/**
 * Real-Spring-Boot tests for the shared, miss-cached Environment bridge (J1038): a Juneau builder key set through
 * Spring is honoured, a source added after a miss is seen, and two Spring REST servlets on one context share one
 * bridge that the context's close removes.
 */
@org.apache.juneau.testing.SpringbootTest
@SpringBootTest(classes = SpringEnvironmentPropertySource_MissCache_Integration_Springboot_Test.TestApp.class,
	webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
	"spring.boot.demo.key=from-test-property-source",
	"MarshallingContext.unsortedProperties=true"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ResourceLock("org.apache.juneau.commons.settings.Settings.singleton")
@SuppressWarnings({
	"java:S2093", // Stores are released with clear() (bridge deregistration), not close(); try-with-resources would call the wrong method.
	"resource" // Named stores and contexts are test-local; each is cleared or closed explicitly.
})
class SpringEnvironmentPropertySource_MissCache_Integration_Springboot_Test extends TestBase {

	@SpringBootConfiguration
	@EnableAutoConfiguration
	public static class TestApp {
	}

	public static class ValueBean {
		@Value("${spring.boot.demo.key}")
		String value;
	}

	public static class C03_ServletA extends BasicSpringRestServlet {
		private static final long serialVersionUID = 1L;
	}

	public static class C03_ServletB extends BasicSpringRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Autowired
	private ApplicationContext appContext;

	private static boolean unsorted() {
		return JsonSerializer.create().build().getMarshallingContext().isUnsortedProperties();
	}

	@Test void c01_builderKeyFromSpring_isHonoured() {
		assertFalse(SpringEnvironmentBridges.isRegistered(appContext), "No store has been built on this context yet");
		var store = new SpringBeanStore(appContext, null);
		try {
			assertEquals("from-test-property-source", BeanInstantiator.of(ValueBean.class, new BasicBeanStore(null)).run().value);
			assertTrue(unsorted());
			assertTrue(unsorted(), "A hit is never cached as a miss");
		} finally {
			store.clear();
		}
		assertFalse(SpringEnvironmentBridges.isRegistered(appContext));
		assertFalse(unsorted(), "With the bridge gone, the key is absent from Settings");
	}

	@Test void c02_missThenAddFirst_thenHit() {
		var k = "Juneau.C02.Key";
		var sources = ((ConfigurableEnvironment)appContext.getEnvironment()).getPropertySources();
		var store = new SpringBeanStore(appContext, null);
		try {
			assertEquals("none", Settings.get().get(k, "none"));
			sources.addFirst(new MapPropertySource("c02", Map.<String,Object>of(k, "v")));
			assertEquals("v", Settings.get().get(k, "none"));
		} finally {
			sources.remove("c02");
			store.clear();
		}
	}

	@Test void c03_twoServletsOneContext_oneBridge_removedOnClose() {
		var base = Settings.get().sourceCount();
		var ctx = new AnnotationConfigApplicationContext();
		try {
			ctx.registerBean(C03_ServletA.class);
			ctx.registerBean(C03_ServletB.class);
			ctx.refresh();
			// RestContext init never runs here, so call the servlets' bean-store hook the way it would.
			var a = (SpringBeanStore)ctx.getBean(C03_ServletA.class).createBeanStore(Optional.empty());
			var b = (SpringBeanStore)ctx.getBean(C03_ServletB.class).createBeanStore(Optional.empty());
			assertEquals(base + 1, Settings.get().sourceCount());
			assertSame(a.environmentSource(), b.environmentSource());
		} finally {
			ctx.close();
		}
		assertFalse(SpringEnvironmentBridges.isRegistered(ctx));
		assertEquals(base, Settings.get().sourceCount());
	}
}
