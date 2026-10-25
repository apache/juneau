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
package org.apache.juneau.examples.guides.springbootrestapi;

// region application
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.builder.*;
import org.springframework.boot.web.servlet.*;
import org.springframework.context.annotation.*;

/**
 * Starts Spring Boot with the pet API on embedded Tomcat (port 8080 by default).
 */
@SpringBootApplication
public class PetApplication {

	/**
	 * Starts the application.
	 *
	 * @param args Spring Boot arguments, for example {@code --server.port=9090}.
	 */
	public static void main(String[] args) {
		new SpringApplicationBuilder(PetApplication.class).run(args);
	}

	/**
	 * The store shared by every request.
	 *
	 * @return A new store.
	 */
	@Bean
	public PetStore petStore() {
		return new PetStore();
	}

	/**
	 * The pet API, as a Spring bean so the store is injected.
	 *
	 * @param store The store bean.
	 * @return The resource.
	 */
	@Bean
	public PetResource petResource(PetStore store) {
		return new PetResource(store);
	}

	/**
	 * The root servlet.
	 *
	 * @return The servlet.
	 */
	@Bean
	public PetRootServlet petRootServlet() {
		return new PetRootServlet();
	}

	/**
	 * Mounts the root servlet at {@code /*}, so the API answers at {@code /pets}.
	 *
	 * @param servlet The root servlet bean.
	 * @return The registration.
	 */
	@Bean
	public ServletRegistrationBean<PetRootServlet> petServlet(PetRootServlet servlet) {
		return new ServletRegistrationBean<>(servlet, "/*");
	}
}
// endregion application
