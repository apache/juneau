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

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

/**
 * Tests for the inject-aware Tomcat microservice bootstrap via {@link TomcatConfiguration}.
 *
 * <p>
 * Verifies that <c>@Configuration</c>-supplied <c>TomcatServerFactory</c> / <c>TomcatServerComponent</c>
 * / <c>TomcatSettings</c> / <c>Servlet</c> beans are picked up via the {@link TomcatConfiguration} bean wiring,
 * that auto-discovered <c>@Rest</c> servlets are mounted at <c>@Rest(path=...)</c>, and that duplicate
 * mount paths fail fast.
 */
@org.apache.juneau.testing.annotations.TomcatMicroserviceTest
class TomcatConfiguration_Test extends TestBase {

	private static Microservice create(Class<?>... configurations) throws Exception {
		var classes = new Class<?>[configurations.length + 1];
		System.arraycopy(configurations, 0, classes, 0, configurations.length);
		classes[configurations.length] = TomcatConfiguration.class;
		return Microservice.create().configurations(classes).build();
	}

	private static Set<String> mountedPaths(Microservice ms) {
		var ctx = ms.getBeanStore().getBean(TomcatServerComponent.class).orElseThrow().getContext();
		return new HashSet<>(Arrays.asList(ctx.findServletMappings()));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// A.  Bean store presence + self-registration of Tomcat-specific beans.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_beanStore_containsMicroservice() throws Exception {
		var ms = create();
		try {
			assertSame(ms, ms.getBeanStore().getBean(Microservice.class).orElse(null));
		} finally {
			ms.stop();
		}
	}

	@Test void a02_beanStore_containsTomcatServerComponent() throws Exception {
		var ms = create();
		try {
			assertNotNull(ms.getBeanStore().getBean(TomcatServerComponent.class).orElse(null));
		} finally {
			ms.stop();
		}
	}

	@Test void a03_beanStore_containsTomcatServerFactory() throws Exception {
		var ms = create();
		try {
			assertNotNull(ms.getBeanStore().getBean(TomcatServerFactory.class).orElse(null));
		} finally {
			ms.stop();
		}
	}

	@Test void a04_beanStore_containsTomcatSettings() throws Exception {
		var ms = create();
		try {
			assertNotNull(ms.getBeanStore().getBean(TomcatSettings.class).orElse(null));
		} finally {
			ms.stop();
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// B.  Resolution priority: user-supplied @Bean beats TomcatConfiguration default.
	//-----------------------------------------------------------------------------------------------------------------

	static class TestFactory extends BasicTomcatServerFactory {
		// Marker for identity checks; behavior inherited from BasicTomcatServerFactory.
	}

	@Configuration
	static class B_FactoryConfig {
		@Bean TomcatServerFactory factory() { return new TestFactory(); }
	}

	@Test void b01_userFactory_beatsConfigurationDefault() throws Exception {
		var ms = create(B_FactoryConfig.class);
		try {
			assertInstanceOf(TestFactory.class, ms.getBeanStore().getBean(TomcatServerFactory.class).orElse(null));
		} finally {
			ms.stop();
		}
	}

	@Configuration
	static class B_SettingsConfig {
		@Bean TomcatSettings tomcatSettings() { return TomcatSettings.create().ports(9999).build(); }
	}

	@Test void b02_userSettings_beatsConfigurationDefault() throws Exception {
		var ms = create(B_SettingsConfig.class);
		try {
			var resolved = ms.getBeanStore().getBean(TomcatSettings.class).orElseThrow();
			assertArrayEquals(new int[]{9999}, resolved.getPorts());
		} finally {
			ms.stop();
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// C.  End-to-end: server boots on an ephemeral port + @Bean Servlet auto-mounted at @Rest(path=...).
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(path = "/api")
	public static class ApiServlet extends RestServlet {
		private static final long serialVersionUID = 1L;
	}

	public static class PlainServlet extends HttpServlet {
		private static final long serialVersionUID = 1L;
	}

	@Configuration
	static class C_AutoMountConfig {
		@Bean TomcatSettings tomcatSettings() { return TomcatSettings.create().ports(0).build(); }
		// Declare return type as Servlet so the bean is registered under Servlet.class
		// (BeanStore.getBeansOfType is exact-type, so subtype lookup needs the supertype declaration).
		@Bean Servlet apiServlet() { return new ApiServlet(); }
	}

	@Test void c01_restServletAutoMounted_atRestPath() throws Exception {
		var ms = create(C_AutoMountConfig.class);
		try {
			ms.start();
			var component = ms.getBeanStore().getBean(TomcatServerComponent.class).orElseThrow();
			assertTrue(mountedPaths(ms).contains("/api/*"), "@Rest servlet should be auto-mounted at /api/*");
			assertTrue(component.getPort() > 0, "Server should bind to an ephemeral port");
			assertEquals("http", component.getProtocol());
			assertNotNull(component.getServer());
			assertNotNull(component.getURI());
		} finally {
			ms.stop();
		}
	}

	@Configuration
	static class C_NoRestServletConfig {
		@Bean TomcatSettings tomcatSettings() { return TomcatSettings.create().ports(0).build(); }
		@Bean Servlet plainServlet() { return new PlainServlet(); }
	}

	@Test void c02_servletWithoutRestAnnotation_isNotAutoMounted() throws Exception {
		var ms = create(C_NoRestServletConfig.class);
		try {
			ms.start();
			// PlainServlet has no @Rest, so nothing should be auto-mounted.
			assertTrue(mountedPaths(ms).isEmpty(), "plain Servlet without @Rest should not be auto-mounted");
		} finally {
			ms.stop();
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// D.  Path collision is a hard startup failure.
	//-----------------------------------------------------------------------------------------------------------------

	@Configuration
	static class D_CollisionConfig {
		@Bean TomcatSettings tomcatSettings() { return TomcatSettings.create().ports(0).build(); }
		@Bean Servlet apiServlet1() { return new ApiServlet(); }
		@Bean(name = "apiServletDup") Servlet apiServlet2() { return new ApiServlet(); }
	}

	@Test void d01_pathCollision_failsHard() throws Exception {
		var ms = create(D_CollisionConfig.class);
		try {
			var ex = assertThrows(Exception.class, ms::start);
			var root = rootCause(ex);
			assertTrue(root.getMessage().contains("Servlet mount path collision"),
				"expected path-collision error, got: " + root.getMessage());
		} finally {
			ms.stop();
		}
	}

	private static Throwable rootCause(Throwable t) {
		var cur = t;
		while (cur.getCause() != null && cur.getCause() != cur)
			cur = cur.getCause();
		return cur;
	}
}
