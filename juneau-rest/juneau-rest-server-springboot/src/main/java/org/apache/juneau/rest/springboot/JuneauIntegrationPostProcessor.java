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

import org.apache.juneau.rest.*;
import org.apache.juneau.rest.springboot.annotations.JuneauIntegration;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.*;

import javax.servlet.Servlet;

/**
 * Processes the {@link JuneauIntegration} annotation on the Spring application class.
 */
public class JuneauIntegrationPostProcessor implements BeanDefinitionRegistryPostProcessor {

    private final ConfigurableApplicationContext ctx;
    private final Class<?> appClass;

    /**
     * Constructor.
     *
     * @param ctx The spring application context.
     * @param appClass The spring application class.
     */
    public JuneauIntegrationPostProcessor(ConfigurableApplicationContext ctx, Class<?> appClass) {
        this.appClass = appClass;
        this.ctx = ctx;
    }

    @Override /* BeanDefinitionRegistryPostProcessor */
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {

    	JuneauIntegration a = appClass.getAnnotation(JuneauIntegration.class);

    	if (a == null || a.rootResources().length == 0)
    		return;

    	RestResourceResolver rrr = new SpringRestResourceResolver(ctx);

    	for (Class<? extends RestServlet> c : a.rootResources()) {
			try {
	            RestServlet rs = c.newInstance().setRestResourceResolver(rrr);
		        ServletRegistrationBean<Servlet> reg = new ServletRegistrationBean<>(rs, '/' + rs.getPath());
		        registry.registerBeanDefinition(reg.getServletName(), new RootBeanDefinition(ServletRegistrationBean.class, () -> reg));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
        }
    }

    @Override /* BeanDefinitionRegistryPostProcessor */
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    	// No-op
    }
}
