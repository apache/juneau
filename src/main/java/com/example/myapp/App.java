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
package com.example.myapp;

import org.apache.juneau.rest.server.springboot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.builder.*;
import org.springframework.boot.web.servlet.*;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.*;

import jakarta.servlet.*;

/**
 * Spring Boot entry point.
 *
 * <p>Registers {@link RootResources} (a {@link SpringRestServlet} group) at {@code /*} so its children
 * resolve as Spring beans.
 */
@SpringBootApplication
@Controller
public class App {

	/**
	 * Entry point.
	 *
	 * @param args Command-line arguments.
	 */
	@SuppressWarnings({
		"resource" // Application context lifecycle is managed by Spring Boot.
	})
	public static void main(String[] args) {
		new SpringApplicationBuilder(App.class).run(args);
	}

	/**
	 * @return The root REST servlet group bean.
	 */
	@Bean
	public RootResources rootResources() {
		return new RootResources();
	}

	/**
	 * @param rootResources The root servlet group.
	 * @return The servlet registration mapped to {@code /*}.
	 */
	@Bean
	public ServletRegistrationBean<Servlet> rootServlet(RootResources rootResources) {
		return new ServletRegistrationBean<>(rootResources, "/*");
	}
}
