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

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.microservice.*;

import jakarta.servlet.*;

/**
 * Zero-config convenience launcher for a Jetty-backed Juneau microservice.
 *
 * <p>
 * Wraps the {@link Microservice} + {@link JettyConfiguration} bootstrap in a single static call so
 * a consumer can stand up a Jetty microservice with one Maven dependency and one line of code:
 *
 * <p class='bjava'>
 * 	<jc>// Boot Jetty + Juneau on the default port (10000) with the interactive console enabled.</jc>
 * 	<jk>public static void</jk> main(String[] args) <jk>throws</jk> Exception {
 * 		JettyMicroservice.<jsm>run</jsm>(args, <jk>new</jk> RootResources());
 * 	}
 * </p>
 *
 * <p>
 * Bundled defaults (all overridable):
 * <ul>
 * 	<li>Port <c>10000</c> (surfaced as <c>Jetty/port</c> in the bundled <c>juneau.cfg</c>).
 * 	<li>Interactive console enabled (use {@link #run(String[], Servlet, boolean)} with
 * 		{@code startConsole=false} to suppress).
 * 	<li>Classpath defaults for <c>jetty.xml</c> and <c>juneau.cfg</c> &mdash; a consumer-supplied
 * 		copy on the working-directory or classpath wins via the existing
 * 		{@link org.apache.juneau.microservice.Microservice} resolution chain.
 * </ul>
 *
 * <p>
 * The supplied root servlet is registered as a {@code @Bean Servlet} in an external bean store and
 * auto-mounted by {@link org.apache.juneau.microservice.jetty.JettyServerComponent JettyServerComponent}
 * at the path declared by its {@link org.apache.juneau.rest.server.Rest @Rest} annotation.  Consumers
 * who want full control over the bean store / configuration classes / listener wiring should call
 * {@link Microservice#create()} directly &mdash; this facade is a thin convenience over that builder.
 *
 * @since 10.0.0
 */
public final class JettyMicroservice {

	private JettyMicroservice() {}

	/**
	 * Boots a Jetty microservice with the supplied root servlet and the bundled defaults,
	 * with the interactive console enabled.
	 *
	 * <p>
	 * Equivalent to calling {@link #run(String[], Servlet, boolean) run(args, rootServlet, true)}.
	 *
	 * @param args         The {@code main(String[])} arguments.  Forwarded to {@link org.apache.juneau.microservice.Microservice.Builder#args(String...)}.
	 * @param rootServlet  The root REST servlet (typically annotated with
	 *                     {@link org.apache.juneau.rest.server.Rest @Rest}).
	 * @return The started {@link Microservice} instance.  Callers typically chain {@link Microservice#join()}.
	 * @throws Exception Error occurred during bootstrap.
	 */
	@SuppressWarnings({
		"java:S112" // throws Exception intentional - mirrors Microservice.start() lifecycle contract.
	})
	public static Microservice run(String[] args, Servlet rootServlet) throws Exception {
		return run(args, rootServlet, true);
	}

	/**
	 * Boots a Jetty microservice with the supplied root servlet and the bundled defaults.
	 *
	 * <p>
	 * Constructs the {@link Microservice}, registers the supplied servlet as a {@code @Bean Servlet} so
	 * {@link org.apache.juneau.microservice.jetty.JettyServerComponent JettyServerComponent} auto-mounts
	 * it, applies {@link JettyConfiguration} so Jetty itself is wired, and starts the lifecycle.  The
	 * returned microservice has not yet been {@link Microservice#join() joined}; callers wanting the
	 * standard "start and block forever" loop should chain {@code .join()}.
	 *
	 * @param args          The {@code main(String[])} arguments.  Forwarded to {@link org.apache.juneau.microservice.Microservice.Builder#args(String...)}.
	 * @param rootServlet   The root REST servlet (typically annotated with
	 *                      {@link org.apache.juneau.rest.server.Rest @Rest}).
	 * @param startConsole  If <jk>true</jk>, also starts the interactive console thread via
	 *                      {@link Microservice#startConsole()}.
	 * @return The started {@link Microservice} instance.
	 * @throws Exception Error occurred during bootstrap.
	 */
	@SuppressWarnings({
		"java:S112", // throws Exception intentional - mirrors Microservice.start() lifecycle contract.
		"resource"   // beanStore lifetime is managed by the returned Microservice; not an independent resource.
	})
	public static Microservice run(String[] args, Servlet rootServlet, boolean startConsole) throws Exception {
		var beanStore = new BasicBeanStore();
		beanStore.addBean(Servlet.class, rootServlet);
		return run(args, beanStore, startConsole, JettyConfiguration.class);
	}

	/**
	 * Power-user form: boots a Jetty microservice with a caller-supplied bean store and configuration
	 * class list.
	 *
	 * <p>
	 * Equivalent to writing the {@link Microservice} builder chain by hand, but with the boilerplate
	 * collapsed.  Use this when the simpler {@link #run(String[], Servlet)} overloads are too
	 * restrictive &mdash; e.g. multiple {@code @Bean Servlet} contributions, custom
	 * {@code MicroserviceListener}s, or a parent bean store.
	 *
	 * @param args            The {@code main(String[])} arguments.  Forwarded to {@link org.apache.juneau.microservice.Microservice.Builder#args(String...)}.
	 * @param beanStore       The bean store to use for dependency injection.  Pre-populated bean entries
	 *                        ({@code @Bean Servlet}, {@code MicroserviceListener}, etc.) are picked up by
	 *                        {@link JettyConfiguration} and the microservice runtime.
	 * @param startConsole    If <jk>true</jk>, also starts the interactive console thread.
	 * @param configurations  The {@code @Configuration}-annotated classes to register.  Always includes
	 *                        {@link JettyConfiguration}; pass an empty list to use only that.
	 * @return The started {@link Microservice} instance.
	 * @throws Exception Error occurred during bootstrap.
	 */
	@SuppressWarnings({
		"java:S112" // throws Exception intentional - mirrors Microservice.start() lifecycle contract.
	})
	public static Microservice run(String[] args, WritableBeanStore beanStore, boolean startConsole, Class<?>... configurations) throws Exception {
		var ms = Microservice
			.create()
			.args(args)
			.beanStore(beanStore)
			.configurations(configurations)
			.build()
			.start();
		if (startConsole)
			ms.startConsole();
		return ms;
	}
}
