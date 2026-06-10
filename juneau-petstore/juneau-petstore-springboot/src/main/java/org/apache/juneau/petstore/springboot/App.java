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
package org.apache.juneau.petstore.springboot;

import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.builder.*;
import org.springframework.boot.web.servlet.*;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.*;

import jakarta.servlet.*;

/**
 * Petstore Spring Boot deployment entry point.
 *
 * <p>
 * Boots a Spring Boot embedded servlet container, wires {@link RootResources} as a Spring bean, and mounts it at
 * {@code /*} via {@link ServletRegistrationBean}.
 *
 * <p>
 * Provides a {@code @Bean HelloMessageProvider} to demonstrate Spring {@code @Autowired} injection into the
 * companion {@link HelloResource} child — preserving the deployment-glue Spring-injection demo.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauPetstoreOverview">juneau-petstore</a>
 * </ul>
 */
@SpringBootApplication
@Controller
public class App {

	/**
	 * Entry point.
	 *
	 * @param args Command-line arguments forwarded to Spring Boot.
	 */
	@SuppressWarnings({
		"resource" // ApplicationContext is owned by Spring; not a leak.
	})
	public static void main(String[] args) {
		new SpringApplicationBuilder(App.class).run(args);
	}

	/**
	 * Spring-injected message provider consumed by {@link HelloResource}.
	 *
	 * @return The hello-message provider bean.
	 */
	@Bean
	public HelloMessageProvider helloMessageProvider() {
		return new HelloMessageProvider("Hello from Spring-injected bean!");
	}

	/**
	 * Returns {@link HelloResource} as a Spring bean so {@code @Autowired} fields resolve.
	 *
	 * @return The hello-world REST resource.
	 */
	@Bean
	public HelloResource helloResource() {
		return new HelloResource();
	}

	/**
	 * Returns the root router as a Spring bean so child resources resolve as Spring beans.
	 *
	 * @return The root router REST resource.
	 */
	@Bean
	public RootResources rootResources() {
		return new RootResources();
	}

	/**
	 * Mounts the root router servlet at {@code /*}.
	 *
	 * @param rootResources The Spring-managed root router.
	 * @return The servlet registration mapped to {@code /*}.
	 */
	@Bean
	public ServletRegistrationBean<Servlet> rootServlet(RootResources rootResources) {
		return new ServletRegistrationBean<>(rootResources, "/*");
	}
}
