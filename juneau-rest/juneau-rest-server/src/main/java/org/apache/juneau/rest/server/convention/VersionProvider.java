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
package org.apache.juneau.rest.server.convention;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.util.*;
import java.util.function.*;
import java.util.jar.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.rest.server.*;

/**
 * Flavor-neutral version/build-metadata worker bean shared by the version {@code @Rest} flavors.
 *
 * <p>
 * Holds the capability state (the assembled {@code name}/{@code version}/{@code git*}/{@code buildTime}/
 * {@code javaVersion} entry map) and the serve logic (write the map as readable JSON, format-pinned to
 * {@code application/json} so the endpoint serves JSON even on a vanilla {@link org.apache.juneau.rest.server.servlet.RestServlet} host that
 * hasn't wired up JSON serializers). The {@link VersionMixin} (mixin), {@link VersionResource} (child),
 * and {@link VersionServlet} (servlet) flavors are independent {@code @Rest} classes that each hold a
 * {@code VersionProvider} worker and delegate to it &mdash; so the three deployment forms cannot drift, and no
 * flavor is another flavor's worker.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link VersionMixin}
 * 	<li class='jc'>{@link VersionResource}
 * 	<li class='jc'>{@link VersionServlet}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server &mdash; Composition (mixins, paths)</a>
 * </ul>
 *
 * @since 10.0.0
 */
// @formatter:off
@SuppressWarnings({
	"java:S1192" // Duplicate string literals are version-related property keys; intentional
})
public class VersionProvider {

	/** Sentinel value returned for entries that the worker couldn't resolve. */
	public static final String UNKNOWN = "(unknown)";

	/**
	 * Creates a new builder, with {@link Value @Value}-annotated fields populated through
	 * {@link BeanInstantiator} from the active
	 * {@link org.apache.juneau.commons.settings.Settings Settings} chain.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return create(BasicBeanStore.INSTANCE);
	}

	/**
	 * Creates a new builder using the supplied {@link BeanStore} for {@link Value @Value}-resolution
	 * and dependency injection.
	 *
	 * @param beanStore The bean store to use for dependency injection.
	 * @return A new builder.
	 */
	public static Builder create(BeanStore beanStore) {
		return BeanInstantiator.of(Builder.class, beanStore).run();
	}

	private final Map<String,String> info;

	/**
	 * No-arg constructor &mdash; reads {@code MANIFEST.MF}, {@code git.properties}, and the JVM
	 * version from the framework classloader. Equivalent to {@code create().build()}.
	 */
	public VersionProvider() {
		this(create());
	}

	/**
	 * Builder constructor.
	 *
	 * <p>
	 * Applies the default lookup chain ({@link Builder#fromManifest()} +
	 * {@link Builder#fromGitProperties()} + {@link Builder#fromJavaVersion()}) when no other
	 * builder method has been called &mdash; ensuring that a freshly-created builder always produces
	 * a non-empty payload.
	 *
	 * @param builder The builder.
	 */
	protected VersionProvider(Builder builder) {
		if (! builder.explicit)
			builder.fromManifest().fromGitProperties().fromJavaVersion();
		info = Collections.unmodifiableMap(new LinkedHashMap<>(builder.entries));
	}

	/**
	 * Serves the assembled metadata as a JSON map.
	 *
	 * <p>
	 * Format-pinned to {@code application/json} &mdash; bypasses the host's content negotiation so
	 * the endpoint serves JSON even on a vanilla {@link org.apache.juneau.rest.server.servlet.RestServlet} host that hasn't wired up JSON
	 * serializers explicitly.
	 *
	 * @param res The current REST response.
	 * @throws IOException If an I/O error occurs while writing the response.
	 */
	public void serve(RestResponse res) throws IOException {
		try (var w = res.getDirectWriter("application/json")) {
			JsonSerializer.DEFAULT_READABLE.write(info, w);
		}
	}

	/**
	 * Returns the configured info map (test/inspection helper).
	 *
	 * @return The info map.
	 */
	public Map<String,String> getInfoMap() {
		return info;
	}

	/**
	 * Builder for {@link VersionProvider} instances.
	 */
	public static class Builder {

		private final Map<String,String> entries = new LinkedHashMap<>();
		private boolean explicit;

		/**
		 * Env-driven default for the {@code javaVersion} entry; resolved from the {@code java.version}
		 * system property (defaulting to the literal {@code (unknown)}, matching {@link VersionProvider#UNKNOWN}).
		 * Populated by {@link BeanInstantiator} via {@link Value @Value} field injection.
		 *
		 * <p>
		 * The default is inlined as a string literal rather than referencing {@code UNKNOWN} via
		 * concatenation, per the project-wide rule that every {@code @Value} expression is a
		 * fully self-contained string literal.
		 */
		@Value("${java.version:(unknown)}")
		String javaVersionDefault;

		/** Constructor &mdash; protected access for {@link VersionProvider#create()}. */
		protected Builder() {}

		/**
		 * Sets a single entry, overwriting any prior value for that key.
		 *
		 * @param key The entry key. Must not be <jk>null</jk> or blank.
		 * @param value The entry value (a <jk>null</jk> value is recorded as {@link VersionProvider#UNKNOWN}).
		 * @return This object.
		 */
		public Builder entry(String key, String value) {
			entries.put(key, value == null ? UNKNOWN : value);
			explicit = true;
			return this;
		}

		/**
		 * Bulk-set entries from a map.
		 *
		 * @param values Entries to add. <jk>null</jk> values are recorded as {@link VersionProvider#UNKNOWN}.
		 * @return This object.
		 */
		public Builder entries(Map<String,String> values) {
			if (values != null)
				values.forEach(this::entry);
			explicit = true;
			return this;
		}

		/**
		 * Reads {@code Implementation-Title}, {@code Implementation-Version},
		 * {@code Implementation-Vendor}, and {@code Build-Jdk} from
		 * {@code /META-INF/MANIFEST.MF} on the {@link VersionProvider} classloader.
		 *
		 * <p>
		 * Missing keys are recorded as {@link VersionProvider#UNKNOWN}. A missing {@code MANIFEST.MF} resource
		 * is silently skipped.
		 *
		 * @return This object.
		 */
		public Builder fromManifest() {
			return fromManifest(VersionProvider.class.getClassLoader());
		}

		/**
		 * Reads manifest attributes from {@code /META-INF/MANIFEST.MF} on the supplied classloader.
		 *
		 * <p>
		 * Useful under Spring Boot fat jars, where the importer's app manifest is reachable from
		 * its own classloader but not the framework's. Missing keys are recorded as
		 * {@link VersionProvider#UNKNOWN}; a missing {@code MANIFEST.MF} resource is silently skipped.
		 *
		 * @param classLoader The classloader to walk.
		 * @return This object.
		 */
		public Builder fromManifest(ClassLoader classLoader) {
			var attrs = readManifestAttributes(classLoader);
			ifNotEmpty(attrs, "Implementation-Title", v -> entry("name", v));
			ifNotEmpty(attrs, "Implementation-Version", v -> entry("version", v));
			ifNotEmpty(attrs, "Implementation-Vendor", v -> entry("vendor", v));
			ifNotEmpty(attrs, "Build-Jdk", v -> entry("javaVersion", v));
			entries.putIfAbsent("name", UNKNOWN);
			entries.putIfAbsent("version", UNKNOWN);
			explicit = true;
			return this;
		}

		/**
		 * Reads a {@link Manifest} that the importer registered as a bean (e.g.
		 * {@code @Bean Manifest appManifest()}).
		 *
		 * @param manifest The manifest to read. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder fromManifest(Manifest manifest) {
			var main = manifest.getMainAttributes();
			var attrs = new HashMap<String,String>();
			for (var e : main.entrySet())
				attrs.put(e.getKey().toString(), String.valueOf(e.getValue()));
			ifNotEmpty(attrs, "Implementation-Title", v -> entry("name", v));
			ifNotEmpty(attrs, "Implementation-Version", v -> entry("version", v));
			ifNotEmpty(attrs, "Implementation-Vendor", v -> entry("vendor", v));
			ifNotEmpty(attrs, "Build-Jdk", v -> entry("javaVersion", v));
			explicit = true;
			return this;
		}

		/**
		 * Reads {@code git.commit.id}, {@code git.branch}, and {@code git.build.time} from
		 * {@code /git.properties} on the {@link VersionProvider} classloader.
		 *
		 * @return This object.
		 */
		public Builder fromGitProperties() {
			return fromGitProperties(VersionProvider.class.getClassLoader());
		}

		/**
		 * Reads {@code git.properties} from the supplied classloader.
		 *
		 * @param classLoader The classloader to walk.
		 * @return This object.
		 */
		public Builder fromGitProperties(ClassLoader classLoader) {
			var props = readProperties(classLoader, "git.properties");
			if (props == null)
				return this;
			ifNotEmptyValue(props.getProperty("git.commit.id"), v -> entry("gitCommit", v));
			ifNotEmptyValue(props.getProperty("git.commit.id.abbrev"), v -> entries.putIfAbsent("gitCommit", v));
			ifNotEmptyValue(props.getProperty("git.branch"), v -> entry("gitBranch", v));
			ifNotEmptyValue(props.getProperty("git.build.time"), v -> entry("buildTime", v));
			explicit = true;
			return this;
		}

		/**
		 * Adds the running JVM's {@code java.version} system property as the {@code javaVersion}
		 * entry, unless an earlier {@link #fromManifest() fromManifest(...)} call already supplied
		 * one from {@code Build-Jdk}.
		 *
		 * @return This object.
		 */
		public Builder fromJavaVersion() {
			entries.putIfAbsent("javaVersion", o(javaVersionDefault).orElse(UNKNOWN));
			explicit = true;
			return this;
		}

		/**
		 * Builds a {@link VersionProvider} instance.
		 *
		 * @return A configured instance.
		 */
		public VersionProvider build() {
			return new VersionProvider(this);
		}

		private static Map<String,String> readManifestAttributes(ClassLoader cl) {
			Manifest fallback = null;
			try {
				var resources = cl.getResources("META-INF/MANIFEST.MF");
				while (resources.hasMoreElements()) {
					var u = resources.nextElement();
					try (var in = u.openStream()) {
						var m = new Manifest(in);
						// Prefer an Implementation-Title-bearing manifest; otherwise hold the
						// most recent one as a fallback. ClassLoader.getResources walks the
						// parent chain FIRST, so caller-supplied URLs come last in the
						// enumeration — last-wins ensures the explicit classloader argument
						// trumps any unrelated MANIFEST.MF leaked from a parent (e.g. JDK
						// module manifests in the boot layer, Surefire's manifest-jar, etc.).
						if (m.getMainAttributes().getValue("Implementation-Title") != null)
							return toMap(m);
						fallback = m;
					}
				}
			} catch (IOException e) {
				// Best-effort path: a malformed classpath manifest yields an empty map rather
				// than bubbling up an exception during startup.
				return Map.of();
			}
			return fallback == null ? Map.of() : toMap(fallback);
		}

		private static Map<String,String> toMap(Manifest m) {
			var attrs = new HashMap<String,String>();
			for (var k : m.getMainAttributes().keySet())
				attrs.put(k.toString(), String.valueOf(m.getMainAttributes().get(k)));
			return attrs;
		}

		@SuppressWarnings({
			"java:S1168" // null is a meaningful "resource absent" sentinel: fromGitProperties() skips entirely (and leaves the explicit flag unset) when no git.properties is present.
		})
		private static Properties readProperties(ClassLoader cl, String path) {
			try (var in = cl.getResourceAsStream(path)) {
				if (in == null)
					return null;
				var p = new Properties();
				p.load(in);
				return p;
			} catch (IOException e) {
				return null;
			}
		}

		private static void ifNotEmpty(Map<String,String> attrs, String key, Consumer<String> sink) {
			var v = attrs.get(key);
			if (ine(v))
				sink.accept(v);
		}

		private static void ifNotEmptyValue(String v, Consumer<String> sink) {
			if (ine(v))
				sink.accept(v);
		}
	}
}
