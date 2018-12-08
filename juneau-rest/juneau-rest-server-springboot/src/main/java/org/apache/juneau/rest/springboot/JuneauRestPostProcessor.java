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
import org.apache.juneau.rest.springboot.annotation.*;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.*;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.*;
import org.springframework.core.type.*;

import java.util.*;

import javax.servlet.Servlet;

/**
 * Processes the {@link JuneauRest} annotation on the Spring application class and <ja>@Bean</ja> methods.
 */
public class JuneauRestPostProcessor implements BeanDefinitionRegistryPostProcessor {

	private final Class<?> appClass;
	private final RestResourceResolver restResourceResolver;
	private BeanDefinitionRegistry registry;

	/**
	 * Constructor.
	 *
	 * @param ctx The spring application context.
	 * @param appClass The spring application class.
	 */
	public JuneauRestPostProcessor(ConfigurableApplicationContext ctx, Class<?> appClass) {
		this.appClass = appClass;
		this.restResourceResolver = new SpringRestResourceResolver(ctx);
	}

	@Override /* BeanDefinitionRegistryPostProcessor */
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
		this.registry = registry;
	}

	@Override /* BeanDefinitionRegistryPostProcessor */
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		try {
			Map<String,RestServlet> m = new LinkedHashMap<>();

			// @JuneauRest on App class.
			if (appClass != null) {
				JuneauRest a = appClass.getAnnotation(JuneauRest.class);
				if (a != null)
					for (Class<? extends RestServlet> c : a.servlets())
						m.put(c.getName(), c.newInstance());
			}

			// @JuneauRest on classes.
			for (Map.Entry<String,Object> e : beanFactory.getBeansWithAnnotation(JuneauRest.class).entrySet())
				if (e.getValue() instanceof RestServlet)
					m.put(e.getKey(), (RestServlet) e.getValue());

			// @JuneauRest on @Bean method.
			for (String beanName : beanFactory.getBeanNamesForType(RestServlet.class)) {
				BeanDefinition bd = beanFactory.getBeanDefinition(beanName);
				if (bd.getSource() instanceof AnnotatedTypeMetadata) {
					AnnotatedTypeMetadata metadata = (AnnotatedTypeMetadata) bd.getSource();
					if (metadata.isAnnotated(JuneauRest.class.getName()))
						m.put(beanName, (RestServlet) beanFactory.getBean(beanName));
				}
			}

			for (RestServlet rs : m.values()) {
				rs.setRestResourceResolver(restResourceResolver);
				ServletRegistrationBean<Servlet> reg = new ServletRegistrationBean<>(rs, '/' + rs.getPath());
				registry.registerBeanDefinition(reg.getServletName(), new RootBeanDefinition(ServletRegistrationBean.class, () -> reg));
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
