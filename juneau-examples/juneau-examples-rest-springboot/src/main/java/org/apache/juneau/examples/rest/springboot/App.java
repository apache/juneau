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
package org.apache.juneau.examples.rest.springboot;

import org.apache.juneau.examples.rest.*;
import org.apache.juneau.rest.springboot.*;
import org.apache.juneau.rest.springboot.annotation.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.*;
import org.springframework.boot.web.servlet.*;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.filter.*;

/**
 * Entry point for Examples REST application when deployed as a Spring Boot application.
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@source}
 * </ul>
 */
@SpringBootApplication
@Controller
public class App {

	/**
	 * Entry point method.
	 * @param args Command-line arguments.
	 */
	public static void main(String[] args) {
		new SpringApplicationBuilder(App.class)
			.initializers(new JuneauRestInitializer(App.class))
			.run(args);

		System.err.println(System.getProperty("server.port"));
	}

	/**
	 * @return Our root resource.
	 */
	@Bean @JuneauRestRoot
	public RootResources getRootResources() {
		return new RootResources();
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
