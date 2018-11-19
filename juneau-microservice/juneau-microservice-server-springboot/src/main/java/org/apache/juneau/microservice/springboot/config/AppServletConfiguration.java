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
package org.apache.juneau.microservice.springboot.config;


import org.apache.juneau.microservice.BasicRestServletJenaGroup;
import org.apache.juneau.microservice.springboot.annotations.EnabledJuneauIntegration;
import org.apache.juneau.rest.RestResourceResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.HiddenHttpMethodFilter;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;


/**@Configuration
 * Spring configuration for servlets.
 */
@Configuration
public class AppServletConfiguration {

    private static final String CONTEXT_ROOT = "";

    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    public BasicRestServletJenaGroup root(SpringRestResourceResolver resolver) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Map<String, Object> beansWithAnnotation = applicationContext.getBeansWithAnnotation(EnabledJuneauIntegration.class);

        if (beansWithAnnotation == null) {
            throw new RuntimeException();
        }

        for (String item : beansWithAnnotation.keySet()) {
            Class bdf = beansWithAnnotation.get(item).getClass();
            if (bdf == null) {
                continue;
            }
            EnabledJuneauIntegration loadConfig = (EnabledJuneauIntegration) bdf.getAnnotation(EnabledJuneauIntegration.class);
            if (loadConfig == null) {
                continue;
            }
            return loadConfig.rootResources().getDeclaredConstructor(RestResourceResolver.class).newInstance(resolver);
        }

        throw new RuntimeException();
    }

    @Bean
    public ServletRegistrationBean<BasicRestServletJenaGroup> rootRegistration(BasicRestServletJenaGroup root) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        return new ServletRegistrationBean<>(root, CONTEXT_ROOT, CONTEXT_ROOT + "/", CONTEXT_ROOT + "/*");
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
