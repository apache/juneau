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
package org.apache.juneau.rest.springboot;

import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;
import java.util.function.*;

import org.apache.juneau.commons.inject.*;
import org.springframework.context.*;

/**
 * A bean store that uses Spring bean resolution to find beans if they're not already in this store.
 *
 * <p>
 * This implementation extends {@link BasicBeanStore2} and delegates to Spring's {@link ApplicationContext}
 * for bean resolution when beans are not found in the local store or parent stores.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a Spring bean store</jc>
 * 	ApplicationContext <jv>appContext</jv> = ...;
 * 	SpringBeanStore2 <jv>beanStore</jv> = <jk>new</jk> SpringBeanStore2(<jv>appContext</jv>, <jk>null</jk>);
 *
 * 	<jc>// Get bean from Spring context</jc>
 * 	Optional&lt;MyService&gt; <jv>service</jv> = <jv>beanStore</jv>.getBean(MyService.<jk>class</jk>);
 *
 * 	<jc>// Get named bean from Spring context</jc>
 * 	Optional&lt;MyService&gt; <jv>namedService</jv> = <jv>beanStore</jv>.getBean(MyService.<jk>class</jk>, <js>"primary"</js>);
 *
 * 	<jc>// Get all beans of a type from Spring context</jc>
 * 	Map&lt;String, MyService&gt; <jv>allServices</jv> = <jv>beanStore</jv>.getBeansOfType(MyService.<jk>class</jk>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestServerSpringbootBasics">juneau-rest-server-springboot Basics</a>
 * 	<li class='jc'>{@link BasicBeanStore2}
 * 	<li class='jic'>{@link BeanStore}
 * </ul>
 */
public class SpringBeanStore2 extends BasicBeanStore2 {

	private final ApplicationContext appContext;

	/**
	 * Constructor.
	 *
	 * @param appContext The Spring application context used to resolve beans. Can be <jk>null</jk>.
	 * @param parent The parent bean store. Can be <jk>null</jk>.
	 */
	public SpringBeanStore2(ApplicationContext appContext, BeanStore parent) {
		super(parent);
		this.appContext = appContext;
	}

	@Override
	public SpringBeanStore2 clear() {
		super.clear();
		return this;
	}

	@Override
	public <T> Optional<T> getBean(Class<T> beanType) {
		// Try local store first
		var o = super.getBean(beanType);
		if (o.isPresent())
			return o;

		// Fall back to Spring context
		if (nn(appContext)) {
			try {
				return opt(appContext.getBeanProvider(beanType).getIfAvailable());
			} catch (Exception e) {
				// Bean not found in Spring context
			}
		}

		return opte();
	}

	@Override
	public <T> Optional<T> getBean(Class<T> beanType, String name) {
		// Try local store first
		var o = super.getBean(beanType, name);
		if (o.isPresent())
			return o;

		// Fall back to Spring context
		if (nn(appContext) && nn(name)) {
			try {
				if (appContext.containsBean(name)) {
					return opt(appContext.getBean(name, beanType));
				}
			} catch (Exception e) {
				// Bean not found in Spring context
			}
		}

		return opte();
	}

	@Override
	public <T> Map<String, T> getBeansOfType(Class<T> beanType) {
		// Get beans from parent stores and local store
		Map<String, T> result = super.getBeansOfType(beanType);

		// Add beans from Spring context (if they don't already exist in the result)
		if (nn(appContext)) {
			try {
				Map<String, T> springBeans = appContext.getBeansOfType(beanType);
				springBeans.forEach((name, bean) -> {
					// Only add if not already present (local beans take precedence)
					result.putIfAbsent(name, bean);
				});
			} catch (Exception e) {
				// Bean type not found in Spring context
			}
		}

		return result;
	}

	@Override
	public boolean hasBean(Class<?> beanType) {
		// Check local store and parents first
		if (super.hasBean(beanType))
			return true;

		// Check Spring context
		if (nn(appContext)) {
			try {
				String[] beanNames = appContext.getBeanNamesForType(beanType);
				return beanNames.length > 0;
			} catch (Exception e) {
				// Bean type not found in Spring context
			}
		}

		return false;
	}

	@Override
	public boolean hasBean(Class<?> beanType, String name) {
		// Check local store and parents first
		if (super.hasBean(beanType, name))
			return true;

		// Check Spring context
		if (nn(appContext) && nn(name)) {
			try {
				return appContext.containsBean(name) && appContext.isTypeMatch(name, beanType);
			} catch (Exception e) {
				// Bean not found in Spring context
			}
		}

		return false;
	}

	@Override
	public <T> Optional<Supplier<T>> getBeanSupplier(Class<T> beanType) {
		// Try local store and parents first
		var supplier = super.getBeanSupplier(beanType);
		if (supplier.isPresent())
			return supplier;

		// Return a supplier that delegates to Spring context
		if (nn(appContext)) {
			try {
				var provider = appContext.getBeanProvider(beanType);
				// Only return a supplier if the bean actually exists in Spring
				if (provider.getIfAvailable() != null) {
					return opt(() -> provider.getIfAvailable());
				}
			} catch (Exception e) {
				// Bean not found in Spring context
			}
		}

		return opte();
	}

	@Override
	public <T> Optional<Supplier<T>> getBeanSupplier(Class<T> beanType, String name) {
		// Try local store and parents first
		var supplier = super.getBeanSupplier(beanType, name);
		if (supplier.isPresent())
			return supplier;

		// Return a supplier that delegates to Spring context
		if (nn(appContext) && nn(name)) {
			try {
				if (appContext.containsBean(name) && appContext.isTypeMatch(name, beanType)) {
					return opt(() -> appContext.getBean(name, beanType));
				}
			} catch (Exception e) {
				// Bean not found in Spring context
			}
		}

		return opte();
	}
}

