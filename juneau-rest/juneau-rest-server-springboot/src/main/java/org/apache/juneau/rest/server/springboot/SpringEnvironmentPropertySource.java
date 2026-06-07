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

import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;
import java.util.function.*;

import org.springframework.core.env.*;

/**
 * Bridges a Spring {@link Environment} into the Juneau {@link org.apache.juneau.commons.settings.Settings Settings}
 * singleton as a {@link org.apache.juneau.commons.settings.PropertySource PropertySource}.
 *
 * <p>
 * Allows {@link org.apache.juneau.commons.inject.Value @Value("&#123;spring.property.key&#125;")} resolution
 * against the same property sources that Spring Boot would resolve through
 * {@code @org.springframework.beans.factory.annotation.Value}: {@code application.yaml},
 * {@code application.properties}, command-line args, environment variables, and any custom
 * {@code org.springframework.core.env.PropertySource} that Spring loads.
 *
 * <h5 class='section'>Lifecycle:</h5>
 * <p>
 * Instances are auto-registered by {@link SpringBeanStore#SpringBeanStore(org.springframework.context.ApplicationContext,
 * org.apache.juneau.commons.inject.BeanStore) SpringBeanStore}'s constructor and removed from
 * {@link org.apache.juneau.commons.settings.Settings#removeSource(org.apache.juneau.commons.settings.PropertySource)
 * Settings} when {@link SpringBeanStore#clear()} is called.  Callers that want explicit lifecycle
 * control can construct the source directly and add it via {@code Settings.get().addSource(...)}.
 *
 * <h5 class='section'>Lazy resolution:</h5>
 * <p>
 * The {@code Environment} is resolved lazily on the first {@link #get(String)} call so that
 * constructors that build the source from an {@code ApplicationContext} do not interact with the
 * context until a lookup actually happens.  This matters for tests that mock the context and assert
 * "no interactions" until a Spring-side bean is requested.
 *
 * <h5 class='section'>Ordering:</h5>
 * <p>
 * Because {@code Settings} walks property sources in reverse insertion order, the Spring
 * {@code Environment} bridge — registered when Spring creates the bean store — shadows any
 * classpath-default {@code juneau.cfg} loaded earlier through the SPI-discovered
 * {@code ConfigPropertySourceProvider}.  Programmatic {@code addSource()} calls made after the
 * Spring context is built (e.g. from a user-provided microservice {@code Config}) still win.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ValueAnnotationBasics">{@code @Value} basics</a>
 * 	<li class='jc'>{@link SpringBeanStore}
 * </ul>
 */
public class SpringEnvironmentPropertySource implements org.apache.juneau.commons.settings.PropertySource {

	private final Supplier<Environment> envSupplier;

	// Guarded by the synchronized block in get(...); volatile reads cover the fast-path check.
	private volatile boolean resolved;
	@SuppressWarnings({
		"java:S3077" // Publish-once reference: env is fully resolved before its single assignment (constructor or inside the synchronized block) and is never compound-mutated, so volatile visibility is sufficient.
	})
	private volatile Environment env;

	/**
	 * Constructor that bridges a pre-built {@link Environment}.
	 *
	 * @param env The Spring environment to bridge.  Must not be <jk>null</jk>.
	 */
	public SpringEnvironmentPropertySource(Environment env) {
		this.env = Objects.requireNonNull(env, "env");
		this.envSupplier = null;
		this.resolved = true;
	}

	/**
	 * Constructor that lazily resolves the {@link Environment} on first lookup.
	 *
	 * <p>
	 * Use this overload when wiring through {@link org.springframework.context.ApplicationContext}: pass
	 * {@code appContext::getEnvironment} so the application context isn't invoked until a lookup
	 * actually occurs.
	 *
	 * @param envSupplier Supplier of the Spring environment.  Must not be <jk>null</jk>.  May return
	 * 	<jk>null</jk>, in which case this source returns
	 * 	{@link org.apache.juneau.commons.settings.PropertyLookupResult#missing()} for every key.
	 */
	public SpringEnvironmentPropertySource(Supplier<Environment> envSupplier) {
		this.envSupplier = Objects.requireNonNull(envSupplier, "envSupplier");
	}

	@Override
	public org.apache.juneau.commons.settings.PropertyLookupResult get(String name) {
		var e = env;
		if (! resolved) {
			synchronized (this) {
				if (! resolved) {
					// safeOpt swallows exceptions thrown by the supplier — e.g. a mocked
					// ApplicationContext that returns null or throws during getEnvironment().
					env = e = safeOpt(envSupplier::get).orElse(null);
					resolved = true;
				} else {
					e = env;
				}
			}
		}
		if (e == null || ! e.containsProperty(name))
			return org.apache.juneau.commons.settings.PropertyLookupResult.missing();
		// Spring's getProperty() returns null only for unresolved placeholders, which
		// containsProperty() already filtered out.  Wrap defensively anyway.
		var v = e.getProperty(name);
		return org.apache.juneau.commons.settings.PropertyLookupResult.present(opt(v));
	}
}
