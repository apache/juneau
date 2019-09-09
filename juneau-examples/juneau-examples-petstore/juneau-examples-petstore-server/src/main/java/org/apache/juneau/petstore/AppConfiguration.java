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
package org.apache.juneau.petstore;

import org.apache.juneau.petstore.rest.*;
import org.apache.juneau.petstore.service.*;
import org.apache.juneau.rest.springboot.annotation.JuneauRestRoot;
import org.springframework.boot.web.servlet.*;
import org.springframework.context.annotation.*;
import org.springframework.web.filter.*;

@Configuration
public class AppConfiguration {

    //-----------------------------------------------------------------------------------------------------------------
    // Services
    //-----------------------------------------------------------------------------------------------------------------

    @Bean
    public PetStoreService petStoreService() {
        return new PetStoreService();
    }

    //-----------------------------------------------------------------------------------------------------------------
    // REST
    //-----------------------------------------------------------------------------------------------------------------

    @Bean
    @JuneauRestRoot
    public RootResources rootResources() {
        return new RootResources();
    }

    @Bean
    public PetStoreResource petStoreResource() {
        return new PetStoreResource();
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
