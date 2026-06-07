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
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.*;

@org.apache.juneau.testing.annotations.TomcatMicroserviceTest
class Tomcat_Paths_Test extends TestBase {

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

	@Rest(paths={"/h1","/h2"})
	public static class A_PathsServlet extends RestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Configuration
	static class A_Config {
		@Bean TomcatSettings tomcatSettings() { return TomcatSettings.create().ports(0).build(); }
		@Bean Servlet pathsServlet() { return new A_PathsServlet(); }
	}

	@Test void a01_restPathsMountServletAtAllDeclaredPaths() throws Exception {
		var ms = create(A_Config.class);
		try {
			ms.start();
			var mapped = mountedPaths(ms);
			assertTrue(mapped.contains("/h1"), "Expected /h1 to be mounted; got: " + mapped);
			assertTrue(mapped.contains("/h2"), "Expected /h2 to be mounted; got: " + mapped);
		} finally {
			ms.stop();
		}
	}

	@Rest(paths={"/same"})
	public static class B_FirstServlet extends RestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Rest(paths={"/same"})
	public static class B_SecondServlet extends RestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Configuration
	static class B_CollisionConfig {
		@Bean TomcatSettings tomcatSettings() { return TomcatSettings.create().ports(0).build(); }
		@Bean Servlet first() { return new B_FirstServlet(); }
		@Bean(name="second") Servlet second() { return new B_SecondServlet(); }
	}

	@Test void a02_duplicateRestPathsFailFast() throws Exception {
		var ms = create(B_CollisionConfig.class);
		try {
			var ex = assertThrows(Exception.class, ms::start);
			var root = rootCause(ex);
			assertTrue(root.getMessage().contains("Servlet mount path collision"), root.getMessage());
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
