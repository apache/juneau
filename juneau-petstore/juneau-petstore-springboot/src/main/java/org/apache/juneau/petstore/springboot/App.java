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

import org.apache.juneau.petstore.console.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.builder.*;
import org.springframework.boot.web.servlet.*;
import org.springframework.context.annotation.*;
import org.springframework.core.*;
import org.springframework.stereotype.*;

import jakarta.servlet.*;
import jakarta.servlet.DispatcherType;

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
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauPetstore">juneau-petstore</a>
 * </ul>
 */
@SuppressWarnings({
	"resource" // ApplicationContext is owned by Spring; not a leak.
})
@SpringBootApplication
@Controller
public class App {

	/**
	 * Entry point.
	 *
	 * @param args Command-line arguments forwarded to Spring Boot.
	 */
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
	 * Returns {@link HelloResource} as a Spring bean with its dependency injected via constructor.
	 *
	 * @param helloMessageProvider The Spring-managed message provider bean.
	 * @return The hello-world REST resource.
	 */
	@Bean
	public HelloResource helloResource(HelloMessageProvider helloMessageProvider) {
		return new HelloResource(helloMessageProvider);
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
	 * The API's CSRF boundary: one instance, held here rather than exposed as a {@code Filter} bean so Spring Boot does
	 * not also auto-register it on {@code /*}.  Shared by {@link #consoleBoundary()} and {@link #apiBoundary()}.
	 */
	private final PetstoreApiBoundary api = new PetstoreApiBoundary();

	/**
	 * Registers the console's CSRF boundary on {@code /console/*}.
	 *
	 * <p>
	 * The {@code /petstore} API is covered by {@link #apiBoundary()}; {@code /petstore-ui} is
	 * outside both.  See {@link PetstoreConsoleBoundary}.  The console boundary is built over the same
	 * {@link PetstoreApiBoundary} the API registration mounts, so the API token its pages carry is one the API filter
	 * accepts.
	 *
	 * <p>
	 * The filter runs on forwarded, async and error dispatches as well as on the original request, so a dispatch that
	 * lands on a console path is checked too.
	 *
	 * @return The filter registration.
	 */
	@Bean
	public FilterRegistrationBean<PetstoreConsoleBoundary> consoleBoundary() {
		var reg = new FilterRegistrationBean<>(new PetstoreConsoleBoundary(api));
		reg.addUrlPatterns(PetstoreConsoleBoundary.URL_PATTERN);
		reg.setOrder(Ordered.HIGHEST_PRECEDENCE);
		reg.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR, DispatcherType.FORWARD);
		return reg;
	}

	/**
	 * Registers the API's CSRF boundary on {@code /petstore/*}.
	 *
	 * <p>
	 * It runs only on the original request: an internal forward or error dispatch to the API is not a new
	 * attacker-controlled request.  The order keeps it just behind the console boundary; the two patterns never overlap.
	 *
	 * @return The filter registration.
	 */
	@Bean
	public FilterRegistrationBean<PetstoreApiBoundary> apiBoundary() {
		var reg = new FilterRegistrationBean<>(api);
		reg.addUrlPatterns(PetstoreApiBoundary.URL_PATTERN);
		reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
		reg.setDispatcherTypes(DispatcherType.REQUEST);
		return reg;
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
