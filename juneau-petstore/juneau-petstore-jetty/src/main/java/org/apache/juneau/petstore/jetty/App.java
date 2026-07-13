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
package org.apache.juneau.petstore.jetty;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.microservice.jetty.*;

import jakarta.servlet.*;

/**
 * Petstore Jetty/Microservice deployment entry point.
 *
 * <p>
 * Locates {@code juneau-petstore-jetty.cfg} (in the working directory or on the classpath), wires Jetty via
 * {@link JettyConfiguration}, and mounts {@link RootResources} as the root servlet.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauPetstore">juneau-petstore</a>
 * </ul>
 */
public class App {

	/**
	 * Entry point.
	 *
	 * @param args Command-line arguments forwarded to {@link Microservice}.
	 * @throws Exception If the microservice fails to start.
	 */
	public static void main(String[] args) throws Exception {
		// @formatter:off
		Microservice
			.create()
			.args(args)
			.configurations(JettyConfiguration.class, AppConfig.class)
			.build()
			.start()
			.startConsole()
			.join();
		// @formatter:on
	}

	/**
	 * Application-specific configuration class contributing the top-level REST servlet.
	 */
	@Configuration
	public static class AppConfig {

		/**
		 * Provides the top-level REST servlet, auto-mounted by {@code JettyServerComponent} at
		 * {@link org.apache.juneau.rest.server.Rest#path()}.
		 *
		 * @return The root servlet.
		 */
		@Bean
		public Servlet rootResources() {
			return new RootResources();
		}
	}
}
