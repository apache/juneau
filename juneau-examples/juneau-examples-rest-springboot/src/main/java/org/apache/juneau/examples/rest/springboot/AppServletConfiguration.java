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
import org.apache.juneau.rest.*;
import org.springframework.boot.web.servlet.*;
import org.springframework.context.annotation.*;
import org.springframework.web.filter.*;

/**
 * Spring configuration for servlets.
 */
@Configuration
public class AppServletConfiguration {

	private static final String CONTEXT_ROOT = "";

	@Bean
	public RootResources root(RestResourceResolver resolver) {
		return (RootResources)new RootResources().setRestResourceResolver(resolver);
	}

	@Bean
	public ServletRegistrationBean<RootResources> rootRegistration(RootResources root) {
		return new ServletRegistrationBean<>(root, CONTEXT_ROOT, CONTEXT_ROOT+"/", CONTEXT_ROOT+"/"+root.getPath());
	}

	/**
	 * We want to be able to consume url-encoded-form-post bodies, but HiddenHttpMethodFilter triggers the HTTP
	 * body to be consumed.  So disable it.
	 */
	@Bean
	public FilterRegistrationBean<HiddenHttpMethodFilter> registration(HiddenHttpMethodFilter filter) {
	    FilterRegistrationBean<HiddenHttpMethodFilter> registration = new FilterRegistrationBean<>(filter);
	    registration.setEnabled(false);
	    return registration;
	}
}
