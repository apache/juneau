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
package org.apache.juneau.microservice.jetty;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.servlet.*;
import org.eclipse.jetty.ee11.servlet.*;
import org.eclipse.jetty.server.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

/**
 * Tests for the inject-aware Jetty microservice bootstrap via {@link JettyConfiguration}.
 *
 * <p>
 * Verifies that <c>@Configuration</c>-supplied <c>JettyServerFactory</c> / <c>JettyServerComponent</c>
 * / <c>Server</c> / <c>Servlet</c> beans are picked up via the {@link JettyConfiguration} bean wiring,
 * that auto-discovered <c>@Rest</c> servlets are mounted at <c>@Rest(path=...)</c>, and that duplicate
 * mount paths fail fast.
 */
class JettyConfiguration_Test extends TestBase {

	private static Microservice create(Class<?>... configurations) throws Exception {
		var classes = new Class<?>[configurations.length + 1];
		System.arraycopy(configurations, 0, classes, 0, configurations.length);
		classes[configurations.length] = JettyConfiguration.class;
		return Microservice.create().configurations(classes).build();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// A.  Bean store presence + self-registration of Jetty-specific beans.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_beanStore_containsMicroservice() throws Exception {
		var ms = create();
		try {
			assertSame(ms, ms.getBeanStore().getBean(Microservice.class).orElse(null));
		} finally {
			ms.stop();
		}
	}

	@Test void a02_beanStore_containsJettyServerComponent() throws Exception {
		var ms = create();
		try {
			assertNotNull(ms.getBeanStore().getBean(JettyServerComponent.class).orElse(null));
		} finally {
			ms.stop();
		}
	}

	@Test void a03_beanStore_containsJettyServerFactory() throws Exception {
		var ms = create();
		try {
			assertNotNull(ms.getBeanStore().getBean(JettyServerFactory.class).orElse(null));
		} finally {
			ms.stop();
		}
	}

	@Test void a04_beanStore_containsJettySettings() throws Exception {
		var ms = create();
		try {
			assertNotNull(ms.getBeanStore().getBean(JettySettings.class).orElse(null));
		} finally {
			ms.stop();
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// B.  Resolution priority: user-supplied @Bean beats JettyConfiguration default.
	//-----------------------------------------------------------------------------------------------------------------

	static class TestFactory extends BasicJettyServerFactory {
		// Marker for identity checks; behavior inherited from BasicJettyServerFactory.
	}

	@Configuration
	static class B_FactoryConfig {
		@Bean JettyServerFactory factory() { return new TestFactory(); }
	}

	@Test void b01_userFactory_beatsConfigurationDefault() throws Exception {
		var ms = create(B_FactoryConfig.class);
		try {
			assertInstanceOf(TestFactory.class, ms.getBeanStore().getBean(JettyServerFactory.class).orElse(null));
		} finally {
			ms.stop();
		}
	}

	@Configuration
	static class B_SettingsConfig {
		@Bean JettySettings jettySettings() { return JettySettings.create().ports(9999).build(); }
	}

	@Test void b02_userSettings_beatsConfigurationDefault() throws Exception {
		var ms = create(B_SettingsConfig.class);
		try {
			var resolved = ms.getBeanStore().getBean(JettySettings.class).orElseThrow();
			assertArrayEquals(new int[]{9999}, resolved.getPorts());
		} finally {
			ms.stop();
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// C.  End-to-end: @Bean Server + @Bean Servlet auto-mounted at @Rest(path=...).
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
		@Bean Server jettyServer() {
			var server = new Server();
			var connector = new ServerConnector(server);
			connector.setPort(0);
			server.addConnector(connector);
			var ctx = new ServletContextHandler();
			ctx.setContextPath("/");
			server.setAttribute("ServletContextHandler", ctx);
			server.setHandler(ctx);
			return server;
		}
		// Declare return type as Servlet so the bean is registered under Servlet.class
		// (BeanStore.getBeansOfType is exact-type, so subtype lookup needs the supertype declaration).
		@Bean Servlet apiServlet() { return new ApiServlet(); }
	}

	@Test void c01_restServletAutoMounted_atRestPath() throws Exception {
		var ms = create(C_AutoMountConfig.class);
		try {
			ms.start();
			var ctx = ms.getBeanStore().getBean(JettyServerComponent.class).orElseThrow().getServletContextHandler();
			var mounted = false;
			for (var h : ctx.getServletHandler().getServletMappings()) {
				for (var p : h.getPathSpecs()) {
					if ("/api/*".equals(p)) {
						mounted = true;
						break;
					}
				}
			}
			assertTrue(mounted, "@Rest servlet should be auto-mounted at /api/*");
		} finally {
			ms.stop();
		}
	}

	@Configuration
	static class C_NoRestServletConfig {
		@Bean Server jettyServer() {
			var server = new Server();
			var connector = new ServerConnector(server);
			connector.setPort(0);
			server.addConnector(connector);
			var ctx = new ServletContextHandler();
			ctx.setContextPath("/");
			server.setAttribute("ServletContextHandler", ctx);
			server.setHandler(ctx);
			return server;
		}
		@Bean Servlet plainServlet() { return new PlainServlet(); }
	}

	@Test void c02_servletWithoutRestAnnotation_isNotAutoMounted() throws Exception {
		var ms = create(C_NoRestServletConfig.class);
		try {
			ms.start();
			var ctx = ms.getBeanStore().getBean(JettyServerComponent.class).orElseThrow().getServletContextHandler();
			// PlainServlet has no @Rest, so its class name should not appear in any servlet mapping.
			var plainName = PlainServlet.class.getName();
			for (var holder : ctx.getServletHandler().getServlets()) {
				assertNotEquals(plainName, holder.getClassName(),
					"plain Servlet without @Rest should not be auto-mounted");
			}
		} finally {
			ms.stop();
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// D.  Path collision is a hard startup failure.
	//-----------------------------------------------------------------------------------------------------------------

	@Configuration
	static class D_CollisionConfig {
		@Bean Server jettyServer() {
			var server = new Server();
			var connector = new ServerConnector(server);
			connector.setPort(0);
			server.addConnector(connector);
			var ctx = new ServletContextHandler();
			ctx.setContextPath("/");
			server.setAttribute("ServletContextHandler", ctx);
			server.setHandler(ctx);
			return server;
		}
		@Bean Servlet apiServlet1() { return new ApiServlet(); }
		@Bean(name = "apiServletDup") Servlet apiServlet2() { return new ApiServlet(); }
	}

	@Test void d01_pathCollision_failsHard() throws Exception {
		var ms = create(D_CollisionConfig.class);
		try {
			var ex = assertThrows(Exception.class, ms::start);
			var root = ThrowableUtils.getCause(ex);
			assertTrue(root.getMessage().contains("Servlet mount path collision"),
				"expected path-collision error, got: " + root.getMessage());
		} finally {
			ms.stop();
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// E.  Helper for unwrapping wrapped exceptions.
	//-----------------------------------------------------------------------------------------------------------------

	static final class ThrowableUtils {
		static Throwable getCause(Throwable t) {
			var cur = t;
			while (cur.getCause() != null && cur.getCause() != cur)
				cur = cur.getCause();
			return cur;
		}
	}
}
