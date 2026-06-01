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

import jakarta.servlet.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.*;
import org.springframework.boot.web.servlet.*;

/**
 * Always-available factory helper that builds a Spring Boot {@link ServletRegistrationBean} whose
 * URL mappings are derived from a Juneau servlet's self-declared top-level paths.
 *
 * <p>
 * Use this from an explicit {@code @Bean} method when you want the mount path to come from the
 * servlet's own {@link org.apache.juneau.rest.annotation.Rest#paths() @Rest(paths)} declaration
 * (or its {@code getPaths()} override) rather than a hard-coded string in your configuration class:
 *
 * <p class='bjava'>
 * 	<ja>@Bean</ja>
 * 	<jk>public</jk> ServletRegistrationBean&lt;?&gt; jspServlet(BeanStore <jv>beanStore</jv>) {
 * 		<jk>return</jk> JuneauServletRegistrations.<jsm>forServlet</jsm>(<jk>new</jk> JspServlet(), <jv>beanStore</jv>);
 * 	}
 * </p>
 *
 * <p>
 * The mapping is resolved via
 * {@link RestContext#resolveTopLevelPaths(Class, Object, BeanStore) RestContext.resolveTopLevelPaths(...)}
 * &mdash; the same runtime-override resolution chain the Jetty microservice auto-discovery loop and
 * the Spring Boot auto-configuration use &mdash; so behavior is identical across runtimes. No path
 * string is hard-coded here; a servlet that resolves to no top-level paths is rejected with an
 * {@link IllegalArgumentException} (declare {@code @Rest(paths=...)} or override {@code getPaths()}).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link JuneauRestAutoConfiguration}
 * 	<li class='jm'>{@link RestContext#resolveTopLevelPaths(Class, Object, BeanStore)}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestServerSpringbootBasics">juneau-rest-server-springboot Basics</a>
 * </ul>
 *
 * @since 9.5.0
 */
public class JuneauServletRegistrations {

	private JuneauServletRegistrations() {}

	/**
	 * Builds a {@link ServletRegistrationBean} for the supplied Juneau servlet, mapping it at the
	 * top-level paths it self-declares.
	 *
	 * @param <T> The servlet type.
	 * @param servlet The Juneau REST servlet to register. Must not be {@code null} and must carry a
	 * 	{@link org.apache.juneau.rest.annotation.Rest @Rest} annotation that resolves to at least one
	 * 	top-level path (via {@code paths()} or a {@code getPaths()} override).
	 * @param beanStore The bean store used for SVL resolution of {@code @Rest(paths)} elements
	 * 	(e.g. {@code $C{key}} / {@code ${env}} templates). May be {@code null} (SVL is then skipped;
	 * 	literal paths pass through unchanged).
	 * @return A registration bean mapping the servlet at its self-declared paths.
	 * @throws IllegalArgumentException If {@code servlet} is {@code null} or resolves to no
	 * 	top-level paths.
	 */
	public static <T extends Servlet> ServletRegistrationBean<T> forServlet(T servlet, BeanStore beanStore) {
		if (servlet == null)
			throw new IllegalArgumentException("Argument 'servlet' must not be null");
		var resolved = RestContext.resolveTopLevelPaths(servlet.getClass(), servlet, beanStore);
		if (resolved.length == 0)
			throw new IllegalArgumentException("Servlet "
				+ servlet.getClass().getName()
				+ " resolves to no top-level paths; declare @Rest(paths=...) or override getPaths().");
		var mappings = new String[resolved.length];
		for (var i = 0; i < resolved.length; i++)
			mappings[i] = toUrlMapping(resolved[i]);
		var bean = new ServletRegistrationBean<>(servlet, mappings);
		bean.setName(servlet.getClass().getName());
		return bean;
	}

	/**
	 * Normalizes a resolved top-level path into a servlet URL mapping.
	 *
	 * <p>
	 * Mirrors the microservice {@code JettyServerComponent.normalizeExactPathSpec(...)} contract:
	 * trims trailing slashes, ensures a leading slash, and leaves any {@code /*} suffix intact (so a
	 * prefix mount like {@code /jsp/*} stays a prefix mapping while an exact mount like
	 * {@code /healthz} stays exact) &mdash; cross-runtime parity with the Jetty mount path.
	 */
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
