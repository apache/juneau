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
package org.apache.juneau.rest.springboot;

import org.apache.juneau.rest.RestServlet;
import org.apache.juneau.rest.annotation.RestResource;
import org.apache.juneau.rest.springboot.annotations.EnabledJuneauIntegration;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.web.servlet.ServletRegistrationBean;

import javax.servlet.Servlet;

public class ServletConfiguration implements BeanDefinitionRegistryPostProcessor {

    private EnabledJuneauIntegration annotation = null;

    public ServletConfiguration(EnabledJuneauIntegration annotation) {
        this.annotation = annotation;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {

        if (annotation.rootResources() == null) {
            return;
        }

        for (Class clazz : annotation.rootResources()) {

            RestServlet restServlet = createServlet(clazz);
            RestResource restResource = restServlet.getClass().getAnnotation(RestResource.class);
            registerServlet(registry, restServlet, restResource);
        }

    }

    private void registerServlet(BeanDefinitionRegistry registry, Servlet restServlet, RestResource restResource) {
        ServletRegistrationBean registration = new ServletRegistrationBean(restServlet, restResource.path());
        registry.registerBeanDefinition(registration.getServletName(), new RootBeanDefinition(ServletRegistrationBean.class,
                () -> registration));

    }

    private RestServlet createServlet(Class clazz) {
        try {
            return (RestServlet) clazz.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }
}
