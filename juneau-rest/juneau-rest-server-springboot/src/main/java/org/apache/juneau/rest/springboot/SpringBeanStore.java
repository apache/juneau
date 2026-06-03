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
import org.apache.juneau.commons.settings.*;
import org.springframework.context.*;

/**
 * A bean store that uses Spring bean resolution to find beans if they're not already in this store.
 *
 * <p>
 * This implementation extends {@link BasicBeanStore} and delegates to Spring's {@link ApplicationContext}
 * for bean resolution when beans are not found in the local store or parent stores.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a Spring bean store</jc>
 * 	ApplicationContext <jv>appContext</jv> = ...;
 * 	SpringBeanStore <jv>beanStore</jv> = <jk>new</jk> SpringBeanStore(<jv>appContext</jv>, <jk>null</jk>);
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
 * 	<li class='jc'>{@link BasicBeanStore}
 * 	<li class='jic'>{@link BeanStore}
 * </ul>
 */
public class SpringBeanStore extends BasicBeanStore {

	private final ApplicationContext appContext;

	// Captured so clear() can deregister the bridge from the process-wide Settings singleton and
	// avoid cross-test bleed when multiple Spring contexts come and go in the same JVM.
	private final SpringEnvironmentPropertySource environmentSource;

	/**
	 * Constructor.
	 *
	 * <p>
	 * If {@code appContext} is non-null, this constructor also installs a
	 * {@link SpringEnvironmentPropertySource} on the process-wide {@link Settings} singleton so that
	 * {@link org.apache.juneau.commons.inject.Value @Value("&#123;...&#125;")} placeholders resolve against
	 * the Spring {@link org.springframework.core.env.Environment Environment} (i.e. against
	 * {@code application.yaml}, env vars, system properties, etc.).  The source is removed by
	 * {@link #clear()}.
	 *
	 * @param appContext The Spring application context used to resolve beans. Can be <jk>null</jk>.
	 * @param parent The parent bean store. Can be <jk>null</jk>.
	 */
	public SpringBeanStore(ApplicationContext appContext, BeanStore parent) {
		super(parent);
		this.appContext = appContext;
		// Register a lazy bridge to Spring's Environment. The supplier form defers the
		// appContext.getEnvironment() call to first @Value("${...}") lookup, so the constructor has
		// no observable interaction with the application context — important for callers (and
		// tests) that don't actually use Spring properties.  Null appContext means no bridge.
		if (nn(appContext)) {
			this.environmentSource = new SpringEnvironmentPropertySource(appContext::getEnvironment);
			Settings.get().addSource(this.environmentSource);
		} else {
			this.environmentSource = null;
		}
	}

	@Override
	@SuppressWarnings({
		"resource" // super.clear() returns this; the discarded return is the store we already own
	})
	public SpringBeanStore clear() {
		super.clear();
		if (nn(environmentSource))
			Settings.get().removeSource(environmentSource);
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
			var bean = safeOpt(() -> appContext.getBeanProvider(beanType).getIfAvailable()).orElse(null);
			return bean != null ? opt(bean) : opte();
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
		if (nn(appContext) && nn(name) && safeOpt(() -> appContext.containsBean(name)).orElse(false)) {
			var bean = safeOpt(() -> appContext.getBean(name, beanType)).orElse(null);
			return bean != null ? opt(bean) : opte();
		}

		return opte();
	}

	@Override
	public <T> Map<String, T> getBeansOfType(Class<T> beanType) {
		// Get beans from parent stores and local store
		Map<String, T> result = super.getBeansOfType(beanType);

		// Add beans from Spring context (if they don't already exist in the result)
		if (nn(appContext)) {
			safeOpt(() -> appContext.getBeansOfType(beanType)).ifPresent(springBeans -> springBeans.forEach(result::putIfAbsent));
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
			return safeOpt(() -> appContext.getBeanNamesForType(beanType))
				.map(beanNames -> beanNames.length > 0)
				.orElse(false);
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
			return safeOpt(() -> appContext.containsBean(name) && appContext.isTypeMatch(name, beanType))
				.orElse(false);
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
			var providerOpt = safeOpt(() -> appContext.getBeanProvider(beanType));
			if (providerOpt.isPresent()) {
				var provider = providerOpt.get();
				// Only return a supplier if the bean actually exists in Spring
				if (provider.getIfAvailable() != null) {
					return opt(provider::getIfAvailable);
				}
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

		// Unnamed lookup: also consult Spring by type so beans like Environment are reachable when this
		// SpringBeanStore is installed in the BasicBeanStore.overridingParent slot (which always passes
		// name=null through the resolution chain).  Mirrors the unnamed getBeanSupplier(Class) branch
		// without delegating to it (avoids polymorphic dispatch back into BasicBeanStore.getBeanSupplier
		// which would loop).
		if (name == null && nn(appContext)) {
			var providerOpt = safeOpt(() -> appContext.getBeanProvider(beanType));
			if (providerOpt.isPresent()) {
				var provider = providerOpt.get();
				if (provider.getIfAvailable() != null)
					return opt(provider::getIfAvailable);
			}
			return opte();
		}

		// Return a supplier that delegates to Spring context
		if (nn(appContext) && nn(name) && safeOpt(() -> appContext.containsBean(name) && appContext.isTypeMatch(name, beanType)).orElse(false)) {
			return opt(() -> safeOpt(() -> appContext.getBean(name, beanType)).orElse(null));
		}

		return opte();
	}
}

