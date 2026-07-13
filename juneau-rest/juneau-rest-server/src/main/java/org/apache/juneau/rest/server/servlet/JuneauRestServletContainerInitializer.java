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
package org.apache.juneau.rest.server.servlet;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.rest.server.*;

import jakarta.servlet.*;
import jakarta.servlet.annotation.*;

/**
 * Plain Servlet-spec {@link ServletContainerInitializer} that auto-mounts {@link RestServlet}
 * subclasses at their self-declared top-level paths in non-Spring (e.g. {@code web.xml} / external
 * WAR) deployments.
 *
 * <p>
 * Discovered by the container via {@code META-INF/services/jakarta.servlet.ServletContainerInitializer}
 * and (through {@link HandlesTypes @HandlesTypes(RestServlet.class)}) handed every concrete
 * {@link RestServlet} on the webapp classpath. It mirrors the Jetty-microservice and Spring Boot
 * auto-registration mechanisms: the mount paths are read from the top-level chain
 * ({@link Rest#paths() @Rest(paths)} / {@code getPaths()}) via
 * {@link RestContext#resolveTopLevelPaths(Class, Object, org.apache.juneau.commons.inject.BeanStore)
 * RestContext.resolveTopLevelPaths(...)} &mdash; <b>not</b> {@code path()} (child-composition).
 *
 * <h5 class='section'>Activation:</h5>
 *
 * <p>
 * Opt-in via the context init parameter {@code juneau.rest.auto-register=true} (parity with the
 * Spring Boot {@code juneau.rest.auto-register} property). When unset or not {@code true}, the
 * initializer does nothing, so it never collides with servlets the deployer declared manually in
 * {@code web.xml}. Declare it in {@code web.xml}:
 *
 * <p class='bxml'>
 * 	<xt>&lt;context-param&gt;</xt>
 * 		<xt>&lt;param-name&gt;</xt>juneau.rest.auto-register<xt>&lt;/param-name&gt;</xt>
 * 		<xt>&lt;param-value&gt;</xt>true<xt>&lt;/param-value&gt;</xt>
 * 	<xt>&lt;/context-param&gt;</xt>
 * </p>
 *
 * <h5 class='section'>Behavior:</h5>
 *
 * <p>
 * For each handled class that is concrete, carries a {@link Rest @Rest} annotation, is no-arg
 * instantiable, and resolves to at least one top-level path, an instance is created and added to the
 * servlet context at each resolved path. Classes that resolve to no top-level paths (pure mixins /
 * child-composed resources) or that cannot be instantiated with a no-arg constructor are skipped.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jm'>{@link RestContext#resolveTopLevelPaths(Class, Object, org.apache.juneau.commons.inject.BeanStore)}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server &mdash; Mixins and Multi-Mount Paths</a>
 * </ul>
 *
 * @since 10.0.0
 */
@HandlesTypes(RestServlet.class)
public class JuneauRestServletContainerInitializer implements ServletContainerInitializer {

	/** Context init parameter that opts a webapp into auto-registration. */
	public static final String AUTO_REGISTER_PARAM = "juneau.rest.auto-register";

	@Override /* ServletContainerInitializer */
	public void onStartup(Set<Class<?>> classes, ServletContext ctx) throws ServletException {
		if (! "true".equalsIgnoreCase(ctx.getInitParameter(AUTO_REGISTER_PARAM)))
			return;
		if (classes == null)
			return;

		for (var c : classes)
			tryMount(c, ctx);
	}

	private static void tryMount(Class<?> c, ServletContext ctx) {
		if (! isMountable(c))
			return;
		var servlet = instantiate(c);
		if (servlet == null)
			return;
		var resolved = RestContext.resolveTopLevelPaths(c, servlet, null);
		if (resolved.length == 0)
			return;
		var reg = ctx.addServlet(c.getName(), servlet);
		if (reg == null)
			return; // A servlet with this name is already registered (e.g. via web.xml).
		for (var path : resolved)
			reg.addMapping(toUrlMapping(path));
	}

	private static boolean isMountable(Class<?> c) {
		if (c == null || c.isInterface() || Modifier.isAbstract(c.getModifiers()))
			return false;
		return c.isAnnotationPresent(Rest.class);
	}

	private static RestServlet instantiate(Class<?> c) {
		try {
			return (RestServlet) c.getDeclaredConstructor().newInstance();
		} catch (ReflectiveOperationException | RuntimeException e) {
			// Best-effort: a servlet that needs constructor-injected collaborators can't be
			// auto-mounted by the plain Servlet-spec path and is simply skipped here.
			return null;
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
