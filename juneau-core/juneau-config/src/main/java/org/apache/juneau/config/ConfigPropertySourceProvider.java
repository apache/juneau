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
package org.apache.juneau.config;

import org.apache.juneau.commons.settings.*;

/**
 * Auto-registers a classpath-default {@code juneau.cfg} (and the other names returned by
 * {@link Config#getCandidateSystemDefaultConfigNames()}) as a {@link PropertySource} for the
 * process-wide {@link Settings} singleton.
 *
 * <p>
 * Discovered by {@link org.apache.juneau.commons.settings.Settings.Builder#useServiceLoader() ServiceLoader-based wiring} through a
 * {@code META-INF/services/org.apache.juneau.commons.settings.PropertySourceProvider} entry
 * shipped in this module.
 *
 * <p>
 * Lookup semantics: the registered source defers calling {@link Config#getSystemDefault()} to the
 * first {@code get(name)} invocation. This is deliberate — calling {@code getSystemDefault()} from
 * inside {@link #create()} would create a static-initialization cycle because
 * {@code Settings.<clinit>} fires the ServiceLoader at class-load time, and {@code Config}'s
 * static initializers transitively reference {@link Settings} (through {@code Cache} / {@code Utils.env(...)})
 * which would re-enter the {@code Settings.<clinit>} block.
 *
 * <p>
 * Lazy resolution makes the cycle a non-issue: by the time {@code @Value("${...}")} fires its first
 * lookup, all participating classes are fully initialized.
 *
 * <h5 class='section'>Silence on missing config:</h5>
 * <p>
 * If no system-default {@link Config} is discoverable (no {@code juneau.cfg} / {@code default.cfg}
 * / {@code application.cfg} / etc. on the classpath or working directory), {@code get(name)}
 * returns {@link PropertyLookupResult#missing()} for every key.  No logging, no exception — many
 * deployments have no classpath config and shouldn't see noise.
 *
 * <h5 class='section'>Ordering:</h5>
 * <p>
 * {@link #order()} returns {@code 100}, which is "low" in the {@link PropertySourceProvider}
 * scheme — provider-discovered classpath defaults register near the bottom of the source list so
 * that programmatic {@code Settings.get().addSource(...)} calls (e.g. per-microservice config or
 * the Spring {@code Environment} bridge) shadow the classpath default.  {@link Settings} walks
 * sources in reverse insertion order, so the most-recently-added source wins.
 */
public class ConfigPropertySourceProvider implements PropertySourceProvider {

	@Override
	public PropertySource create() {
		// Return a lazy wrapper.  Resolving Config.getSystemDefault() eagerly here would trigger
		// Config.<clinit>, which can transitively re-enter Settings.<clinit> through Cache /
		// Utils.env(...) — the SPI registration is invoked from inside the Settings static init
		// block, so any cycle here surfaces as ExceptionInInitializerError.
		return new LazyConfigPropertySource();
	}

	@Override
	public int order() {
		// Low ordering: classpath defaults register first, so any programmatic
		// addSource(...) added afterwards shadows the classpath defaults at lookup time
		// (Settings walks sources in reverse insertion order).
		return 100;
	}

	/**
	 * Defers {@link Config#getSystemDefault()} resolution to the first {@link #get(String)} call to
	 * avoid the static-initialization cycle described in the enclosing class' Javadoc.
	 *
	 * <p>
	 * Additionally protects against three runtime cycles at first-use time:
	 * <ol>
	 * 	<li><b>Bootstrap-static-init cycle (poisoning).</b> If this source is called from inside
	 * 		<i>any</i> JVM static-initializer chain that transitively loads {@code Config}, the
	 * 		downstream {@code Config.findSystemDefault()} → {@code FileStore.<clinit>} →
	 * 		{@code Context.Builder.build()} chain can fail with {@link NullPointerException}
	 * 		because {@code Context.<clinit>} hasn't finished yet. That failure <i>poisons</i>
	 * 		{@code FileStore} (every subsequent reference raises {@link NoClassDefFoundError}).
	 * 		To prevent the poisoning, the source detects when it's called from a static-initializer
	 * 		frame by walking the current stack for a {@code "<clinit>"} method name and short-circuits
	 * 		to {@link PropertyLookupResult#missing()} <i>without</i> memoizing — so the next call,
	 * 		from regular code, can resolve normally.
	 * 	<li><b>Reentrant resolution cycle.</b> {@link Config#findSystemDefault()} looks up the
	 * 		{@code "juneau.configFile"} system property via {@link Settings#get(String)}, which
	 * 		walks every registered source — including this one. Without a guard, that re-enters
	 * 		{@code Config.getSystemDefault()} on the same thread, which re-enters
	 * 		{@code findSystemDefault()}, which re-enters {@code Settings.get("juneau.configFile")},
	 * 		ad infinitum (a {@link StackOverflowError}). A {@link ThreadLocal} sentinel short-circuits
	 * 		any re-entrant lookup on the same thread to {@code missing()}.
	 * 	<li><b>Defensive throwable catch.</b> Any unexpected throwable (e.g. a different class
	 * 		loader's {@code Config} init failure that the {@code <clinit>}-sniff missed) is
	 * 		swallowed and treated as "not yet available" — {@code get(name)} returns
	 * 		{@link PropertyLookupResult#missing()} without memoizing the failure.
	 * </ol>
	 */
	@SuppressWarnings({
		"java:S1181" // Intentional broad Throwable catch — see Javadoc.
	})
	static final class LazyConfigPropertySource implements PropertySource {

		// Thread-local re-entrance guard. Static so it covers both the lazy-init path and any
		// concurrent lookup that may also reach a Config.getSystemDefault() call (defensively).
		private static final ThreadLocal<Boolean> RESOLVING = ThreadLocal.withInitial(() -> Boolean.FALSE);

		// Guarded by the synchronized block in get(...); volatile reads cover the fast-path check.
		private volatile boolean resolved;
		private volatile ConfigPropertySource delegate;

		@Override
		@SuppressWarnings({
			"java:S3776" // Cognitive complexity acceptable for lazy config property source resolution
		})
		public PropertyLookupResult get(String name) {

			// Re-entrant guard: Config.findSystemDefault() walks Settings.get("juneau.configFile"),
			// which round-trips through every source including this one. Short-circuit on
			// re-entry so the outer Config.getSystemDefault() can make progress.
			if (Boolean.TRUE.equals(RESOLVING.get()))
				return PropertyLookupResult.missing();

			// Bootstrap guard: if we're being called from inside a <clinit> frame (e.g. another
			// class's static-initializer chain transitively invokes Settings.get(...) → this
			// source), don't attempt to resolve Config — doing so would force Config / FileStore /
			// Context class loading from inside an already-in-progress class init, which poisons
			// FileStore.<clinit> with a NoClassDefFoundError that survives the rest of the JVM
			// lifetime. Short-circuit without memoizing; the next non-<clinit> call resolves.
			if (! resolved && isInClinit())
				return PropertyLookupResult.missing();

			if (! resolved) {
				synchronized (this) {
					if (! resolved) {
						// Re-check the <clinit> guard inside the lock — another thread may have
						// flipped resolved between the first check and the lock acquire.
						if (isInClinit())
							return PropertyLookupResult.missing();

						Config cfg;
						RESOLVING.set(Boolean.TRUE);
						try {
							cfg = Config.getSystemDefault();
						} catch (@SuppressWarnings("unused") Throwable t) {
							// Defense-in-depth: a Config-side init failure the <clinit> sniff
							// missed. Return missing() without memoizing so the next lookup can
							// retry once Config init has completed.
							return PropertyLookupResult.missing();
						} finally {
							RESOLVING.remove();
						}
						// Silent when no juneau.cfg / default.cfg / etc. is discoverable — the
						// SPI consumer sees missing() for every key but no log noise.
						delegate = (cfg == null) ? null : new ConfigPropertySource(cfg);
						resolved = true;
					}
				}
			}
			return delegate == null ? PropertyLookupResult.missing() : delegate.get(name);
		}

		/**
		 * Returns <jk>true</jk> if any frame in the current thread's call stack is a Java
		 * static-initializer ({@code "<clinit>"}). Used to detect — and avoid — the bootstrap-time
		 * cycle that would otherwise poison {@code FileStore.<clinit>}.
		 *
		 * <p>
		 * Cost: a single {@link Thread#getStackTrace()} call on the first lookup per source-thread
		 * pair. {@code resolved} is volatile so subsequent calls skip the stack walk entirely.
		 *
		 * @return <jk>true</jk> if the current call is being made from inside a {@code <clinit>}
		 * 	frame anywhere in the stack.
		 */
		private static boolean isInClinit() {
			var trace = Thread.currentThread().getStackTrace();
			for (var frame : trace) {
				if ("<clinit>".equals(frame.getMethodName()))
					return true;
			}
			return false;
		}
	}
}
