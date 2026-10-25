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
package org.apache.juneau.rest.server.springboot;

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;

import java.util.*;
import java.util.concurrent.atomic.*;
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
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestServerSpringboot">juneau-rest-server-springboot</a>
 * 	<li class='jc'>{@link BasicBeanStore}
 * 	<li class='jic'>{@link BeanStore}
 * </ul>
 */
public class SpringBeanStore extends BasicBeanStore {

	private final ApplicationContext appContext;

	// The shared Environment bridge for appContext (null when appContext is null).  One per context,
	// reference-counted across stores; see SpringEnvironmentBridges.
	private final SpringEnvironmentBridges.Registration registration;

	// clear() may be called repeatedly; this store releases its reference at most once.
	private final AtomicBoolean released = new AtomicBoolean();

	/**
	 * Constructor.
	 *
	 * <p>
	 * If {@code appContext} is non-null, this constructor also makes sure the process-wide {@link Settings} singleton
	 * holds a {@link SpringEnvironmentPropertySource} for it, so that {@link Value @Value("&#123;...&#125;")}
	 * placeholders resolve against the Spring {@link org.springframework.core.env.Environment Environment} (i.e. against
	 * {@code application.yaml}, env vars, system properties, etc.).  There is one such source per application context,
	 * shared by every store built on it and kept at the position of the first registration.  It is removed when every
	 * store on the context has been {@link #clear() cleared}, or when the context closes (for a
	 * {@link org.springframework.context.ConfigurableApplicationContext ConfigurableApplicationContext}).
	 *
	 * @param appContext The Spring application context used to resolve beans. Can be <jk>null</jk> (no Spring environment bridge is installed).
	 * @param parent The parent bean store. Can be <jk>null</jk> for a root store with no parent chain.
	 */
	public SpringBeanStore(ApplicationContext appContext, BeanStore parent) {
		super(parent);
		this.appContext = appContext;
		// The bridge resolves appContext.getEnvironment() lazily on the first @Value("${...}") lookup, so the
		// constructor has no observable interaction with the application context.  Null appContext means no bridge.
		this.registration = nn(appContext) ? SpringEnvironmentBridges.acquire(appContext) : null;
	}

	@Override
	@SuppressWarnings({
		"resource" // super.clear() returns this; the discarded return is the store we already own
	})
	public SpringBeanStore clear() {
		try {
			super.clear();
		} finally {
			// Release even when super.clear() throws (it does once this store is closed), so the reference cannot leak.
			// Defensive: registration is null until this constructor finishes (BasicBeanStore's constructors do not call clear() today).
			if (nn(registration) && released.compareAndSet(false, true))
				SpringEnvironmentBridges.release(registration);
		}
		return this;
	}

	/**
	 * Returns the shared Environment bridge this store acquired.
	 *
	 * @return The bridge, or <jk>null</jk> if this store has no application context.
	 */
	SpringEnvironmentPropertySource environmentSource() {
		return registration == null ? null : registration.source;
	}

	/**
	 * Clears the miss cache of the Spring {@code Environment} bridge registered for the given application context.
	 *
	 * <p>
	 * Needed only after a Spring property source's contents change in place for a key that is not a valid Spring Boot
	 * property name and that Juneau already looked up and missed; see {@link SpringEnvironmentPropertySource#invalidate()}.
	 * The bridge is shared by every {@code SpringBeanStore} on the context, so this affects all of them.
	 *
	 * @param appContext The application context.  Can be <jk>null</jk>.
	 * @return <jk>true</jk> if the context had a registered bridge.
	 * @since 10.0.0
	 */
	public static boolean invalidateEnvironmentCache(ApplicationContext appContext) {
		return SpringEnvironmentBridges.invalidate(appContext);
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
			return bean != null ? o(bean) : oe();
		}

		return oe();
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
			return bean != null ? o(bean) : oe();
		}

		return oe();
	}

	@Override
	public <T> Map<String,T> getBeansOfType(Class<T> beanType) {
		// Get beans from parent stores and local store
		Map<String,T> result = super.getBeansOfType(beanType);

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

	// No getBeanSupplier(Class) override: BasicBeanStore.getBeanSupplier(Class) delegates via unqualified
	// this.getBeanSupplier(beanType, null), which polymorphically dispatches to this class's own
	// getBeanSupplier(Class,String) override below — its name==null branch already performs the Spring lookup.

	@Override
	public <T> Optional<Supplier<T>> getBeanSupplier(Class<T> beanType, String name) {
		// Try local store and parents first
		var supplier = super.getBeanSupplier(beanType, name);
		if (supplier.isPresent())
			return supplier;

		// Unnamed lookup: also consult Spring by type so beans like Environment are reachable when this
		// SpringBeanStore is installed in the BasicBeanStore.overridingParent slot (which always passes
		// name=null through the resolution chain).
		if (name == null && nn(appContext)) {
			var providerOpt = safeOpt(() -> appContext.getBeanProvider(beanType));
			if (providerOpt.isPresent()) {
				var provider = providerOpt.get();
				if (provider.getIfAvailable() != null)
					return o(provider::getIfAvailable);
			}
			return oe();
		}

		// Return a supplier that delegates to Spring context
		if (nn(appContext) && safeOpt(() -> appContext.containsBean(name) && appContext.isTypeMatch(name, beanType)).orElse(false)) {
			return o(() -> safeOpt(() -> appContext.getBean(name, beanType)).orElse(null));
		}

		return oe();
	}
}

