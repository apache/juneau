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
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.servlet.*;
import org.eclipse.jetty.ee11.servlet.*;
import org.eclipse.jetty.server.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

/**
 * Tests for the inject-aware {@link JettyMicroservice} bootstrap.
 *
 * <p>
 * Verifies that <c>@Configuration</c>-supplied <c>JettyServerFactory</c> /
 * <c>JettyMicroserviceListener</c> / <c>Server</c> / <c>Servlet</c> beans are picked up,
 * that explicit builder calls take precedence, that auto-discovered <c>@Rest</c> servlets
 * are mounted at <c>@Rest(path=...)</c>, and that duplicate mount paths fail fast.
 */
class JettyMicroservice_Inject_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// A.  Bean store presence + self-registration of Jetty-specific beans.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_beanStore_containsJettyMicroservice() throws Exception {
		var ms = JettyMicroservice.create().build();
		try {
			assertSame(ms, ms.getBeanStore().getBean(JettyMicroservice.class).orElse(null));
		} finally {
			ms.stop();
		}
	}

	@Test void a02_beanStore_containsJettyListener() throws Exception {
		var ms = JettyMicroservice.create().build();
		try {
			assertNotNull(ms.getBeanStore().getBean(JettyMicroserviceListener.class).orElse(null));
		} finally {
			ms.stop();
		}
	}

	@Test void a03_beanStore_containsJettyServerFactory() throws Exception {
		var ms = JettyMicroservice.create().build();
		try {
			assertNotNull(ms.getBeanStore().getBean(JettyServerFactory.class).orElse(null));
		} finally {
			ms.stop();
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// B.  Resolution priority: explicit builder > @Bean > default.
	//-----------------------------------------------------------------------------------------------------------------

	static class TestFactory extends BasicJettyServerFactory {
		// Marker for identity checks; behavior inherited from BasicJettyServerFactory.
	}

	@Configuration
	static class B_FactoryConfig {
		@Bean JettyServerFactory factory() { return new TestFactory(); }
	}

	@Test void b01_explicitFactory_beatsConfigurationFactory() throws Exception {
		var explicit = new BasicJettyServerFactory();
		var ms = JettyMicroservice.create()
			.jettyServerFactory(explicit)
			.configurations(B_FactoryConfig.class)
			.build();
		try {
			assertSame(explicit, ms.getBeanStore().getBean(JettyServerFactory.class).orElse(null));
		} finally {
			ms.stop();
		}
	}

	@Test void b02_configurationFactory_usedWhenNoBuilderFactory() throws Exception {
		var ms = JettyMicroservice.create()
			.configurations(B_FactoryConfig.class)
			.build();
		try {
			assertInstanceOf(TestFactory.class, ms.getBeanStore().getBean(JettyServerFactory.class).orElse(null));
		} finally {
			ms.stop();
		}
	}

	@Configuration
	static class B_ListenerConfig {
		@Bean JettyMicroserviceListener listener() { return new BasicJettyMicroserviceListener() {}; }
	}

	@Test void b03_configurationListener_usedWhenNoBuilderListener() throws Exception {
		var ms = JettyMicroservice.create()
			.configurations(B_ListenerConfig.class)
			.build();
		try {
			// The contributed listener bean should be the resolved one.
			var configured = ms.getBeanStore().getBeansOfType(JettyMicroserviceListener.class).values().iterator().next();
			assertNotNull(configured);
			assertNotSame(BasicJettyMicroserviceListener.class, configured.getClass()); // it's the anonymous subclass
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
		var ms = JettyMicroservice.create()
			.configurations(C_AutoMountConfig.class)
			.build();
		try {
			ms.createServer();
			var ctx = ms.getServletContextHandler();
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
			var ctx = new ServletContextHandler();
			ctx.setContextPath("/");
			server.setAttribute("ServletContextHandler", ctx);
			server.setHandler(ctx);
			return server;
		}
		@Bean Servlet plainServlet() { return new PlainServlet(); }
	}

	@Test void c02_servletWithoutRestAnnotation_isNotAutoMounted() throws Exception {
		var ms = JettyMicroservice.create()
			.configurations(C_NoRestServletConfig.class)
			.build();
		try {
			ms.createServer();
			var ctx = ms.getServletContextHandler();
			// PlainServlet has no @Rest, so it should NOT be auto-mounted.
			assertEquals(0, ctx.getServletHandler().getServletMappings().length,
				"plain Servlet without @Rest should not be auto-mounted");
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
		var ms = JettyMicroservice.create()
			.configurations(D_CollisionConfig.class)
			.build();
		try {
			var ex = assertThrows(Exception.class, ms::createServer);
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
