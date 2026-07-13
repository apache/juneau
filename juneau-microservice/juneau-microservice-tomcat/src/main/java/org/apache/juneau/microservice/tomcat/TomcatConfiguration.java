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

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.microservice.*;

/**
 * <c>@Configuration</c> class that wires embedded Tomcat into a {@link Microservice} bootstrap.
 *
 * <p>
 * Add this class to {@link org.apache.juneau.microservice.Microservice.Builder#configurations(Class...) configurations(...)} to enable a
 * Tomcat-backed microservice:
 *
 * <p class='bjava'>
 * 	Microservice
 * 		.<jsm>create</jsm>()
 * 		.args(<jv>args</jv>)
 * 		.configurations(TomcatConfiguration.<jk>class</jk>, MyAppConfig.<jk>class</jk>)
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
 * 	<li>{@link TomcatServerFactory} — defaults to {@link BasicTomcatServerFactory}, used to build the
 * 		{@link org.apache.catalina.startup.Tomcat Tomcat} instance programmatically.
 * 	<li>{@link TomcatSettings} — empty defaults; user-supplied <c>@Bean TomcatSettings</c> can override ports
 * 		and the Catalina base directory.
 * 	<li>{@link TomcatServerComponent} (always contributed, also registered under {@link MicroserviceListener}) —
 * 		drives the Tomcat server lifecycle via the microservice's {@code start()} / {@code stop()} fan-out.
 * </ul>
 *
 * <p>
 * <c>@Bean Servlet</c> methods in user-supplied <c>@Configuration</c> classes are auto-mounted at
 * {@link org.apache.juneau.rest.server.Rest#path()} during start-up.  See {@link TomcatServerComponent} for
 * full details.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauMicroserviceTomcat">juneau-microservice-tomcat Basics</a>
 * </ul>
 *
 * @since 10.0.0
 */
@Configuration
public class TomcatConfiguration {

	private final TomcatServerComponent component = new TomcatServerComponent();

	/**
	 * Constructor.
	 */
	public TomcatConfiguration() { /* intentionally empty */ }

	/**
	 * Contributes the default Tomcat server factory used to build the
	 * {@link org.apache.catalina.startup.Tomcat Tomcat} instance.
	 *
	 * @return The default factory.
	 */
	@Bean
	@ConditionalOnMissingBean(TomcatServerFactory.class)
	public TomcatServerFactory tomcatServerFactory() {
		return new BasicTomcatServerFactory();
	}

	/**
	 * Contributes an empty default {@link TomcatSettings} so {@link TomcatServerComponent} always finds one in the
	 * store.  Users who need programmatic overrides supply their own <c>@Bean TomcatSettings</c>.
	 *
	 * @return The default empty settings.
	 */
	@Bean
	@ConditionalOnMissingBean(TomcatSettings.class)
	public TomcatSettings tomcatSettings() {
		return TomcatSettings.create().build();
	}

	/**
	 * Contributes the {@link TomcatServerComponent} bean under its own type for direct lookup by external callers.
	 *
	 * @return The component instance.
	 */
	@Bean
	public TomcatServerComponent tomcatServerComponent() {
		return component;
	}

	/**
	 * Contributes the same {@link TomcatServerComponent} instance under {@link MicroserviceListener} (named
	 * <c>tomcatServerListener</c>) so it participates in the {@link Microservice#start()} / {@link Microservice#stop()}
	 * fan-out without clobbering any user-supplied unnamed {@code MicroserviceListener}.
	 *
	 * @return The component instance.
	 */
	@Bean(name = "tomcatServerListener")
	public MicroserviceListener tomcatServerListener() {
		return component;
	}
}
