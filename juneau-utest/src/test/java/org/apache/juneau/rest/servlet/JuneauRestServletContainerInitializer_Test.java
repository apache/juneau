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
package org.apache.juneau.rest.servlet;

import static org.mockito.Mockito.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.convention.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.*;

/**
 * Validates the non-Spring {@link JuneauRestServletContainerInitializer}: it auto-mounts concrete
 * {@code @Rest}-annotated servlets that self-declare top-level paths, but only when the webapp opts
 * in via the {@code juneau.rest.auto-register} context init parameter.
 *
 * @since 10.0.0
 */
class JuneauRestServletContainerInitializer_Test extends TestBase {

	private static ServletContext mockContext(String autoRegisterValue, ServletRegistration.Dynamic dyn) {
		var ctx = mock(ServletContext.class);
		when(ctx.getInitParameter(JuneauRestServletContainerInitializer.AUTO_REGISTER_PARAM))
			.thenReturn(autoRegisterValue);
		when(ctx.addServlet(anyString(), any(Servlet.class))).thenReturn(dyn);
		return ctx;
	}

	@Test void a01_mountsWhenOptedIn() throws Exception {
		var dyn = mock(ServletRegistration.Dynamic.class);
		var ctx = mockContext("true", dyn);

		new JuneauRestServletContainerInitializer()
			.onStartup(Set.of(VersionServlet.class), ctx);

		verify(ctx).addServlet(eq(VersionServlet.class.getName()), any(VersionServlet.class));
		verify(dyn).addMapping("/version/*");
	}

	@Test void a02_noopWhenParamUnset() throws Exception {
		var dyn = mock(ServletRegistration.Dynamic.class);
		var ctx = mockContext(null, dyn);

		new JuneauRestServletContainerInitializer()
			.onStartup(Set.of(VersionServlet.class), ctx);

		verify(ctx, never()).addServlet(anyString(), any(Servlet.class));
	}

	@Test void a03_skipsMixinResolvingToNoPaths() throws Exception {
		var dyn = mock(ServletRegistration.Dynamic.class);
		var ctx = mockContext("true", dyn);

		// VersionMixin is a plain mixin (no top-level paths, not even a RestServlet) — it must
		// be filtered out and never mounted.
		new JuneauRestServletContainerInitializer()
			.onStartup(Set.of(VersionMixin.class), ctx);

		verify(ctx, never()).addServlet(anyString(), any(Servlet.class));
	}

	@Test void a04_nullClassSetIsSafe() throws Exception {
		var ctx = mockContext("true", mock(ServletRegistration.Dynamic.class));
		new JuneauRestServletContainerInitializer().onStartup(null, ctx);
		verify(ctx, never()).addServlet(anyString(), any(Servlet.class));
	}
}
