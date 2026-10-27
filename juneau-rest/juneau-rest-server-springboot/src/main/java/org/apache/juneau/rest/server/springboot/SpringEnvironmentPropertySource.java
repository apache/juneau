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
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import org.springframework.boot.context.properties.source.*;
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
 * {@link SpringBeanStore} registers one instance per {@link org.springframework.context.ApplicationContext} in
 * {@link org.apache.juneau.commons.settings.Settings Settings}, shared by every store on that context.  It is removed
 * when every such store has been {@link SpringBeanStore#clear() cleared}, or when the context closes (for a
 * {@link org.springframework.context.ConfigurableApplicationContext ConfigurableApplicationContext}).  Callers that
 * want explicit lifecycle control can construct the source directly and add it via {@code Settings.get().addSource(...)};
 * such instances are not shared.
 *
 * <h5 class='section'>Miss cache:</h5>
 * <p>
 * Juneau's builder keys (e.g. {@code MarshallingContext.unsortedProperties}) are not valid Spring Boot property names,
 * so Spring walks every raw property source for each of them.  This source remembers up to 4096 such keys that were
 * absent.  Every lookup checks the Environment's property-source list by identity (an added, removed or replaced
 * source drops the cache) and re-checks {@link System#getProperty(String)} for a cached key.  Valid Boot names and
 * {@code juneau.profiles.active} are never cached, and hits are never cached.  A source whose <em>contents</em> change
 * in place (a mutated map, {@code MockEnvironment.setProperty}) is not detected for a key already cached as missing;
 * call {@link #invalidate()} after such a change.  Setting {@code juneau.spring.missCache.disable=true} in the Spring
 * Environment before the first lookup turns the cache off.
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
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ValueAnnotation">@Value Annotation Basics</a>
 * 	<li class='jc'>{@link SpringBeanStore}
 * </ul>
 */
public class SpringEnvironmentPropertySource implements org.apache.juneau.commons.settings.PropertySource {

	/** Soft cap on cached misses per bridge; same policy as J1037's source cache (copied, not referenced). */
	static final int MISS_CACHE_MAX_ENTRIES = 4096;

	/** Opt-out switch, read once at first resolve from the bridged Environment itself (never through Settings). */
	private static final String MISS_CACHE_DISABLE_KEY = "juneau.spring.missCache.disable";

	private final Supplier<Environment> envSupplier;

	// Written inside resolve() before resolved; readers read resolved first, so plain fields are safely published.
	private volatile boolean resolved;
	@SuppressWarnings({
		"java:S3077" // Publish-once reference: env is assigned before resolved is set (constructor or resolve()) and is never compound-mutated, so volatile visibility is sufficient.
	})
	private volatile Environment env;
	private boolean cacheEnabled;

	// The miss set for the source list it was built from; replaced, never edited, when the list changes.
	private final AtomicReference<MissGeneration> generation = new AtomicReference<>();

	/**
	 * Constructor that bridges a pre-built {@link Environment}.
	 *
	 * @param env The Spring environment to bridge.  Must not be <jk>null</jk>.
	 */
	public SpringEnvironmentPropertySource(Environment env) {
		this.env = Objects.requireNonNull(env, "env");
		this.envSupplier = null;
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

	private synchronized void resolve() {
		if (resolved)  // HTT: another thread resolved between the caller's check and lock acquisition; a single-threaded test cannot force it
			return;
		// safeOpt swallows exceptions thrown by the supplier, e.g. a mocked ApplicationContext that returns null or
		// throws during getEnvironment().
		var e = nn(envSupplier) ? safeOpt(envSupplier::get).orElse(null) : env;
		env = e;
		cacheEnabled = e instanceof ConfigurableEnvironment && ! safeOpt(() -> e.getProperty(MISS_CACHE_DISABLE_KEY)).map(Boolean::parseBoolean).orElse(false);
		resolved = true;
	}

	@SuppressWarnings({
		"java:S3776" // Single lookup flow: profile piggyback, miss-cache check and property read share locals, so splitting it would only obscure the order of checks
	})
	@Override
	public org.apache.juneau.commons.settings.PropertyLookupResult get(String name) {
		if (! resolved)
			resolve();
		// Read env only after resolved: reading it first could see null, then resolved == true, and miss wrongly.
		var e = env;
		if (e == null)
			return org.apache.juneau.commons.settings.PropertyLookupResult.missing();
		// Profile piggyback: a Juneau config-profile activation lookup resolves to Spring's active profiles, so a
		// Spring-Boot deployment has a single source of truth for which profiles are active.  Only answered when the
		// caller has NOT explicitly set juneau.profiles.active as a property (that exact key wins if present).
		if (eq(name, PROFILES_ACTIVE_KEY) && ! e.containsProperty(name)) {
			var active = e.getActiveProfiles();
			if (active.length > 0)  // getActiveProfiles() never returns null per the Environment contract.
				return org.apache.juneau.commons.settings.PropertyLookupResult.present(o(String.join(",", active)));
			return org.apache.juneau.commons.settings.PropertyLookupResult.missing();
		}
		var g = cacheEnabled && nn(name) ? currentGeneration((ConfigurableEnvironment)e) : null;
		if (nn(g) && g.misses.contains(name)) {
			// For an invalid Boot name, the raw systemProperties source (exact key) is the only one whose contents
			// can change without a source-list change; systemEnvironment wraps System.getenv(), which cannot.
			if (! g.hasSystemProperties || System.getProperty(name) == null)
				return org.apache.juneau.commons.settings.PropertyLookupResult.missing();
			g.misses.remove(name);
		}
		if (! e.containsProperty(name)) {
			// Valid Boot names are never cached: Boot's relaxed binding can answer them from a changed source.
			// isValid allocates, so it runs only here, on an uncached miss.
			if (nn(g) && g.misses.size() < MISS_CACHE_MAX_ENTRIES && ! ConfigurationPropertyName.isValid(name))
				g.misses.add(name);
			return org.apache.juneau.commons.settings.PropertyLookupResult.missing();
		}
		// Spring's getProperty() returns null for a key mapped to an explicit null value, and throws
		// IllegalArgumentException for an unresolvable nested placeholder.  o() maps the null to an empty value.
		var v = e.getProperty(name);
		return org.apache.juneau.commons.settings.PropertyLookupResult.present(o(v));
	}

	/**
	 * Clears this bridge's miss cache.
	 *
	 * <p>
	 * Needed only after a property source's contents change in place (for example a mutated
	 * {@code MapPropertySource} map, or {@code MockEnvironment.setProperty} in a test) for a key that is not a valid
	 * Spring Boot property name and that was already looked up and missed.  Adding, removing or replacing a property
	 * source, and setting a system property, are detected without it.  A lookup already in progress on another thread
	 * may still answer from the old cache; lookups that start after this method returns do not.
	 *
	 * @since 10.0.0
	 */
	public void invalidate() {
		generation.set(null);
	}

	/** @return The number of cached misses (tests). */
	int missCount() {
		var g = generation.get();
		return g == null ? 0 : g.misses.size();
	}

	private MissGeneration currentGeneration(ConfigurableEnvironment e) {
		var live = e.getPropertySources();
		var g = generation.get();
		if (nn(g) && g.matches(live))
			return g;
		// Racing threads may each build one; the loser's misses go to an unused generation, which only costs a walk.
		g = new MissGeneration(live);
		generation.set(g);
		return g;
	}

	/**
	 * Cached misses for one snapshot of the Environment's property-source list.  A new generation replaces the old one
	 * when the list changes, so a miss computed against an old list can only land in a generation nobody reads.
	 */
	private static final class MissGeneration {
		final org.springframework.core.env.PropertySource<?>[] sources;
		final boolean hasSystemProperties;
		final Set<String> misses = ConcurrentHashMap.newKeySet();

		MissGeneration(MutablePropertySources live) {
			sources = live.stream().toArray(org.springframework.core.env.PropertySource<?>[]::new);
			hasSystemProperties = Arrays.stream(sources).anyMatch(x -> StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME.equals(x.getName()));
		}

		// Identity, element by element, then count.  MutablePropertySources iterates its copy-on-write array without copying.
		boolean matches(MutablePropertySources live) {
			var i = 0;
			for (var s : live) {
				if (i >= sources.length || s != sources[i])
					return false;
				i++;
			}
			return i == sources.length;
		}
	}

	/** The Juneau config-profile activation key that piggybacks on Spring's active profiles. */
	private static final String PROFILES_ACTIVE_KEY = "juneau.profiles.active";
}
