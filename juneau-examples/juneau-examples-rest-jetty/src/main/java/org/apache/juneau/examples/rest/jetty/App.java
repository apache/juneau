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
package org.apache.juneau.examples.rest.jetty;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.examples.rest.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.microservice.jetty.*;

import jakarta.servlet.*;

/**
 * An example of an extended REST microservice.
 *
 * <p>
 * The {@link Microservice} class will locate the <c>examples.cfg</c> file in the home directory and initialize
 * the resources and commands defined in that file.
 */
public class App {

	/**
	 * Entry point method.
	 *
	 * @param args Command line arguments.
	 * @throws Exception General exception occurred.
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
		 * Provides the top-level REST servlet, auto-mounted by {@link JettyServerComponent} at
		 * {@link org.apache.juneau.rest.annotation.Rest#path()}.
		 *
		 * @return The root servlet.
		 */
		@Bean
		public Servlet rootResources() {
			return new RootResources();
		}
	}
}
