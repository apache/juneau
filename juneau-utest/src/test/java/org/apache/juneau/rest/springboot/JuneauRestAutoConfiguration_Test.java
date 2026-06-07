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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.convention.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.test.context.runner.*;
import org.springframework.boot.web.servlet.*;
import org.springframework.context.annotation.*;

import jakarta.servlet.*;
import jakarta.servlet.ServletRegistration;

/**
 * Validates the opt-in Spring Boot auto-configuration {@link JuneauRestAutoConfiguration}:
 * <ul>
 * 	<li>The {@code juneauRestAutoRegistrar} bean is contributed only when
 * 		{@code juneau.rest.auto-register=true} (no default).
 * 	<li>The registrar mounts {@code RestServlet} beans at their self-declared paths and skips
 * 		servlets already wired into a manual {@code ServletRegistrationBean}.
 * </ul>
 *
 * @since 10.0.0
 */
@org.apache.juneau.testing.annotations.SpringbootTest
class JuneauRestAutoConfiguration_Test extends TestBase {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(JuneauRestAutoConfiguration.class));

	//-----------------------------------------------------------------------------------------------------------------
	// Property gating (activation model R1, matchIfMissing=false).
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_registrarPresentWhenPropertyTrue() {
		runner.withPropertyValues("juneau.rest.auto-register=true")
			.run(ctx -> assertThat(ctx).hasBean("juneauRestAutoRegistrar"));
	}

	@Test void a02_registrarAbsentWhenPropertyUnset() {
		runner.run(ctx -> assertThat(ctx).doesNotHaveBean("juneauRestAutoRegistrar"));
	}

	@Test void a03_registrarAbsentWhenPropertyFalse() {
		runner.withPropertyValues("juneau.rest.auto-register=false")
			.run(ctx -> assertThat(ctx).doesNotHaveBean("juneauRestAutoRegistrar"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Registrar behavior.
	//-----------------------------------------------------------------------------------------------------------------

	@Configuration
	static class ServletOnlyConfig {
		@org.springframework.context.annotation.Bean
		Servlet versionServlet() { return new VersionServlet(); }
	}

	@Test void b01_mountsRestServletAtSelfDeclaredPath() throws Exception {
		try (var appContext = new AnnotationConfigApplicationContext(ServletOnlyConfig.class)) {
			var dyn = mock(ServletRegistration.Dynamic.class);
			var servletContext = mock(ServletContext.class);
			when(servletContext.addServlet(anyString(), any(Servlet.class))).thenReturn(dyn);

			new JuneauRestAutoConfiguration().juneauRestAutoRegistrar(appContext).onStartup(servletContext);

			verify(servletContext).addServlet(anyString(), any(VersionServlet.class));
			verify(dyn).addMapping("/version/*");
		}
	}

	@Configuration
	static class ManuallyRegisteredConfig {
		private final VersionServlet servlet = new VersionServlet();
		@org.springframework.context.annotation.Bean
		Servlet versionServlet() { return servlet; }
		@org.springframework.context.annotation.Bean
		ServletRegistrationBean<VersionServlet> versionRegistration() {
			return new ServletRegistrationBean<>(servlet, "/custom/*");
		}
	}

	@Test void b02_skipsManuallyRegisteredServlet() throws Exception {
		try (var appContext = new AnnotationConfigApplicationContext(ManuallyRegisteredConfig.class)) {
			var servletContext = mock(ServletContext.class);

			new JuneauRestAutoConfiguration().juneauRestAutoRegistrar(appContext).onStartup(servletContext);

			verify(servletContext, never()).addServlet(anyString(), any(Servlet.class));
		}
	}
}
