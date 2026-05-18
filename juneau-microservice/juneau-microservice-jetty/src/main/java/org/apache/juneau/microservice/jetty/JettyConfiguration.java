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

/**
 * <c>@Configuration</c> class that wires Jetty into a {@link Microservice} bootstrap.
 *
 * <p>
 * Add this class to {@link org.apache.juneau.microservice.Microservice.Builder#configurations(Class...) configurations(...)} to enable a
 * Jetty-backed microservice:
 *
 * <p class='bjava'>
 * 	Microservice
 * 		.<jsm>create</jsm>()
 * 		.args(<jv>args</jv>)
 * 		.configurations(JettyConfiguration.<jk>class</jk>, MyAppConfig.<jk>class</jk>)
 * 		.build()
 * 		.start()
 * 		.startConsole()
 * 		.join();
 * </p>
 *
 * <p>
 * Contributes the following beans (each gated on {@code @ConditionalOnMissingBean} so user-supplied beans of the
 * same type win):
 * <ul>
 * 	<li>{@link JettyServerFactory} — defaults to {@link BasicJettyServerFactory}, used to build the Jetty
 * 		{@link org.eclipse.jetty.server.Server Server} from <c>jetty.xml</c>.
 * 	<li>{@link JettySettings} — empty defaults; user-supplied <c>@Bean JettySettings</c> can override ports,
 * 		<c>jetty.xml</c> contents, and var-resolution.
 * 	<li>{@link JettyServerComponent} (always contributed, also registered under {@link MicroserviceListener}) —
 * 		drives the Jetty server lifecycle via the microservice's {@code start()} / {@code stop()} fan-out.
 * </ul>
 *
 * <p>
 * <c>@Bean Servlet</c> methods in user-supplied <c>@Configuration</c> classes are auto-mounted at
 * {@link org.apache.juneau.rest.annotation.Rest#path()} during start-up.  See {@link JettyServerComponent} for
 * full details.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauMicroserviceJettyBasics">juneau-microservice-jetty Basics</a>
 * </ul>
 *
 * @since 9.5.0
 */
@Configuration
public class JettyConfiguration {

	private final JettyServerComponent component = new JettyServerComponent();

	/**
	 * Contributes the default Jetty server factory used to build the {@link org.eclipse.jetty.server.Server Server}
	 * from a <c>jetty.xml</c> file.
	 *
	 * @return The default factory.
	 */
	@Bean
	@ConditionalOnMissingBean(JettyServerFactory.class)
	public JettyServerFactory jettyServerFactory() {
		return new BasicJettyServerFactory();
	}

	/**
	 * Contributes an empty default {@link JettySettings} so {@link JettyServerComponent} always finds one in the
	 * store.  Users who need programmatic overrides supply their own <c>@Bean JettySettings</c>.
	 *
	 * @return The default empty settings.
	 */
	@Bean
	@ConditionalOnMissingBean(JettySettings.class)
	public JettySettings jettySettings() {
		return JettySettings.create().build();
	}

	/**
	 * Contributes the {@link JettyServerComponent} bean under its own type for direct lookup by external callers.
	 *
	 * @return The component instance.
	 */
	@Bean
	public JettyServerComponent jettyServerComponent() {
		return component;
	}

	/**
	 * Contributes the same {@link JettyServerComponent} instance under {@link MicroserviceListener} (named
	 * <c>jettyServerListener</c>) so it participates in the {@link Microservice#start()} / {@link Microservice#stop()}
	 * fan-out without clobbering any user-supplied unnamed {@code MicroserviceListener}.
	 *
	 * @return The component instance.
	 */
	@Bean(name = "jettyServerListener")
	public MicroserviceListener jettyServerListener() {
		return component;
	}
}
