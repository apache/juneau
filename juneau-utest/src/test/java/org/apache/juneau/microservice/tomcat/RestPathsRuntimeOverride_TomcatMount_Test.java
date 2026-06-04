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
import org.apache.juneau.config.*;
import org.apache.juneau.config.store.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.*;

/**
 * Validates that {@link TomcatServerComponent}'s auto-discovery loop honors the runtime-override
 * resolution chain when computing top-level mount paths.  In particular:
 * <ul>
 * 	<li>A {@link RestServlet#getPaths() RestServlet.getPaths()} override beats
 * 		{@link Rest#paths() @Rest(paths)}.
 * 	<li>SVL substitution applied per-element to {@code @Rest(paths={"$C{key}"})} resolves through the
 * 		microservice's bean store ({@link Config} bean) before the comma-split and produces the
 * 		runtime-resolved mount paths.
 * 	<li>Collision detection still runs on the resolved paths.
 * </ul>
 *
 * <p>
 * Mirrors {@code RestPathsRuntimeOverride_JettyMount_Test} for the embedded-Tomcat backend.
 *
 * @since 10.0.0
 */
@org.apache.juneau.testing.annotations.TomcatMicroserviceTest
class RestPathsRuntimeOverride_TomcatMount_Test extends TestBase {

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
	// a — getPaths() getter override beats @Rest(paths) at Tomcat mount time
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(paths={"/from-annotation-1", "/from-annotation-2"})
	public static class A_GetterOverride extends RestServlet {
		private static final long serialVersionUID = 1L;
		@Override public String[] getPaths() { return new String[]{"/from-getter-1", "/from-getter-2"}; }
	}

	@Configuration
	static class A_Config {
		@Bean TomcatSettings tomcatSettings() { return TomcatSettings.create().ports(0).build(); }
		@Bean Servlet pathsServlet() { return new A_GetterOverride(); }
	}

	@Test
	void a01_getterOverride_mountsAtGetterPaths() throws Exception {
		var ms = create(A_Config.class);
		try {
			ms.start();
			var mapped = mountedPaths(ms);
			assertTrue(mapped.contains("/from-getter-1"), "Expected /from-getter-1 to be mounted; got: " + mapped);
			assertTrue(mapped.contains("/from-getter-2"), "Expected /from-getter-2 to be mounted; got: " + mapped);
			assertFalse(mapped.contains("/from-annotation-1"), "Annotation default must be skipped when getter overrides; got: " + mapped);
			assertFalse(mapped.contains("/from-annotation-2"), "Annotation default must be skipped when getter overrides; got: " + mapped);
		} finally {
			ms.stop();
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b — SVL ($C{...}) inside @Rest(paths) element resolves through Config and comma-splits
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(paths={"$C{b.paths}"})
	public static class B_SvlConfigKey extends RestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Configuration
	static class B_Config {
		@Bean TomcatSettings tomcatSettings() { return TomcatSettings.create().ports(0).build(); }
		@Bean Servlet pathsServlet() { return new B_SvlConfigKey(); }
		@Bean Config testConfig() {
			var store = MemoryStore.create().build();
			store.write("b.cfg", null, "b.paths = /from-config-1, /from-config-2\n");
			return Config.create().store(store).name("b.cfg").build();
		}
	}

	@Test
	void b01_svlConfigKey_mountsAtResolvedPaths() throws Exception {
		var ms = create(B_Config.class);
		try {
			ms.start();
			var mapped = mountedPaths(ms);
			assertTrue(mapped.contains("/from-config-1"), "Expected /from-config-1 to be mounted; got: " + mapped);
			assertTrue(mapped.contains("/from-config-2"), "Expected /from-config-2 to be mounted; got: " + mapped);
		} finally {
			ms.stop();
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c — Collision detection still runs on resolved (post-override) paths
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(paths={"/c-original"})
	public static class C_FirstWithGetterCollision extends RestServlet {
		private static final long serialVersionUID = 1L;
		@Override public String[] getPaths() { return new String[]{"/c-collision"}; }
	}

	@Rest(paths={"/c-collision"})
	public static class C_SecondPlain extends RestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Configuration
	static class C_CollisionConfig {
		@Bean TomcatSettings tomcatSettings() { return TomcatSettings.create().ports(0).build(); }
		@Bean Servlet first() { return new C_FirstWithGetterCollision(); }
		@Bean(name="second") Servlet second() { return new C_SecondPlain(); }
	}

	@Test
	void c01_resolvedPaths_collisionFailsFast() throws Exception {
		var ms = create(C_CollisionConfig.class);
		try {
			var ex = assertThrows(Exception.class, ms::start);
			var root = rootCause(ex);
			assertTrue(root.getMessage().contains("Servlet mount path collision"),
				"Collision should fire on resolved (post-override) paths: " + root.getMessage());
			assertTrue(root.getMessage().contains("/c-collision"),
				"Error should name the colliding resolved path: " + root.getMessage());
		} finally {
			ms.stop();
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d — Empty getPaths() (clear) → annotation default never fires
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(paths={"/from-annotation"})
	public static class D_GetterClears extends RestServlet {
		private static final long serialVersionUID = 1L;
		@Override public String[] getPaths() { return new String[0]; }
	}

	@Configuration
	static class D_Config {
		@Bean TomcatSettings tomcatSettings() { return TomcatSettings.create().ports(0).build(); }
		@Bean Servlet pathsServlet() { return new D_GetterClears(); }
	}

	@Test
	void d01_emptyGetter_clearsAllMounts() throws Exception {
		var ms = create(D_Config.class);
		try {
			ms.start();
			var mapped = mountedPaths(ms);
			assertFalse(mapped.contains("/from-annotation"),
				"Empty getPaths() return should clear and prevent the annotation default from mounting; got: " + mapped);
		} finally {
			ms.stop();
		}
	}

	private static Throwable rootCause(Throwable t) {
		var x = t;
		while (x.getCause() != null && x.getCause() != x)
			x = x.getCause();
		return x;
	}
}
