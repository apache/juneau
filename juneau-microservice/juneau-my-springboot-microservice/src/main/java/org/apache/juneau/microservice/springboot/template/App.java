// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.microservice.springboot.template;

import javax.servlet.*;

import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.springboot.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.*;
import org.springframework.boot.web.servlet.*;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.filter.*;

/**
 * Entry point for Examples REST application when deployed as a Spring Boot application.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../../index.html#my-springboot-microservice">my-springboot-microservice</a>
 * </ul>
 */
@SpringBootApplication
@Controller
public class App {

	/**
	 * Entry point method.
	 * @param args Command-line arguments.
	 */
	@SuppressWarnings("resource")
	public static void main(String[] args) {
		new SpringApplicationBuilder(App.class).run(args);
	}

	/**
	 * Our root REST bean.
	 * <p>
	 * Note that this must extend from {@link SpringRestServlet} so that child resources can be resolved as Spring
	 * beans.
	 * <p>
	 * All REST objects are attached to this bean using the {@link Rest#children()} annotation.
	 *
	 * @return The root resources REST bean.
	 */
	@Bean
	public RootResources getRootResources() {
		return new RootResources();
	}

	/**
	 * Optionally return the {@link HelloWorldResource} object as an injectable bean.
	 *
	 * @return The hello-world REST bean.
	 */
	@Bean
	public HelloWorldResource getHelloWorldResource() {
		return new HelloWorldResource("Hello Spring user!");
	}

	/**
	 * Optionally return an injectable message provider for the {@link HelloWorldResource} class.
	 *
	 * @return The message provider for the hello-world REST bean.
	 */
	@Bean
	public HelloWorldMessageProvider getHelloWorldMessageProvider() {
		return new HelloWorldMessageProvider("Hello Spring injection user!");
	}

	/**
	 * @param rootResources The root REST resource servlet
	 * @return The servlet registration mapped to "/*".
	 */
	@Bean
	public ServletRegistrationBean<Servlet> getRootServlet(RootResources rootResources) {
		return new ServletRegistrationBean<>(rootResources, "/*");
	}

	/**
	 * We want to be able to consume url-encoded-form-post bodies, but HiddenHttpMethodFilter triggers the HTTP
	 * body to be consumed.  So disable it.
	 *
	 * @param filter The filter.
	 * @return Filter registration bean.
	 */
	@Bean
	public FilterRegistrationBean<HiddenHttpMethodFilter> registration(HiddenHttpMethodFilter filter) {
		FilterRegistrationBean<HiddenHttpMethodFilter> registration = new FilterRegistrationBean<>(filter);
		registration.setEnabled(false);
		return registration;
	}
}
