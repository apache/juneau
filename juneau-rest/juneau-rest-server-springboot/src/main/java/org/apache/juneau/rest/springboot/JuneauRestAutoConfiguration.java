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

import java.util.*;

import jakarta.servlet.*;

import org.apache.juneau.commons.inject.BeanStore;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.servlet.*;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * Opt-in Spring Boot auto-configuration that mounts every {@link RestServlet} bean at its
 * self-declared top-level path without an explicit {@link ServletRegistrationBean} per servlet.
 *
 * <h5 class='section'>Activation:</h5>
 *
 * <p>
 * Gated behind the property {@code juneau.rest.auto-register=true} (no default &mdash;
 * {@code matchIfMissing=false}); when the property is absent or any value other than {@code true},
 * this auto-configuration contributes nothing and servlet registration stays fully manual. Set it
 * in {@code application.properties} / {@code application.yaml}:
 *
 * <p class='bcode'>
 * 	juneau.rest.auto-register=true
 * </p>
 *
 * <h5 class='section'>Behavior:</h5>
 *
 * <p>
 * When active, a single {@link ServletContextInitializer} bean walks all {@link RestServlet} beans
 * in the application context and, for each one:
 *
 * <ol class='spaced-list'>
 * 	<li>Skips it if its instance is already wired into a manual {@link ServletRegistrationBean}
 * 		(no double-mounts &mdash; explicit registrations always win).
 * 	<li>Resolves its mount paths via
 * 		{@link RestContext#resolveTopLevelPaths(Class, Object, BeanStore) RestContext.resolveTopLevelPaths(...)}
 * 		(reading the top-level {@code paths()} / {@code getPaths()} chain &mdash; <b>not</b>
 * 		{@code path()}), using the application's registered {@link BeanStore} for SVL resolution.
 * 	<li>Registers the servlet at each resolved path. A servlet that resolves to no top-level paths
 * 		is skipped (it is presumably composed as a child or mounted some other way).
 * </ol>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link JuneauServletRegistrations}
 * 	<li class='jm'>{@link RestContext#resolveTopLevelPaths(Class, Object, BeanStore)}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestServerSpringbootBasics">juneau-rest-server-springboot Basics</a>
 * </ul>
 *
 * @since 9.5.0
 */
@AutoConfiguration
@ConditionalOnProperty(name="juneau.rest.auto-register", havingValue="true", matchIfMissing=false)
public class JuneauRestAutoConfiguration {

	/**
	 * Contributes the auto-registration hook.
	 *
	 * @param appContext The Spring application context (source of {@link RestServlet} beans,
	 * 	existing {@link ServletRegistrationBean} beans, and the {@link BeanStore}).
	 * @return A {@link ServletContextInitializer} that mounts un-registered Juneau servlets.
	 */
	@Bean
	public ServletContextInitializer juneauRestAutoRegistrar(ApplicationContext appContext) {
		return servletContext -> registerAll(appContext, servletContext);
	}

	private static void registerAll(ApplicationContext appContext, ServletContext servletContext) {
		var servlets = appContext.getBeansOfType(RestServlet.class);
		if (servlets.isEmpty())
			return;

		// Identity set of servlet instances already wired into a manual ServletRegistrationBean — these
		// must never be auto-registered a second time (explicit registration wins).
		var manual = Collections.newSetFromMap(new IdentityHashMap<Servlet,Boolean>());
		for (var bean : appContext.getBeansOfType(ServletRegistrationBean.class).values()) {
			var s = bean.getServlet();
			if (s != null)
				manual.add(s);
		}

		var store = appContext.getBeanProvider(BeanStore.class).getIfAvailable();

		for (var e : servlets.entrySet()) {
			var servlet = e.getValue();
			if (manual.contains(servlet))
				continue;
			var resolved = RestContext.resolveTopLevelPaths(servlet.getClass(), servlet, store);
			if (resolved.length == 0)
				continue;
			var reg = servletContext.addServlet(e.getKey(), servlet);
			if (reg == null)
				continue; // A servlet with this name is already registered (container-level dedup).
			for (var path : resolved)
				reg.addMapping(toUrlMapping(path));
		}
	}

	private static String toUrlMapping(String rawPath) {
		var p = rawPath == null ? "" : rawPath;
		while (p.endsWith("/") && p.length() > 1)
			p = p.substring(0, p.length() - 1);
		if (p.isEmpty())
			return "/";
		if (! p.startsWith("/"))
			p = "/" + p;
		return p;
	}
}
