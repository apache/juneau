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
package org.apache.juneau.rest.convention;

import static org.apache.juneau.commons.utils.Utils.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.function.*;
import java.util.jar.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.json.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;

/**
 * Mixin that serves deployment-introspection metadata at {@code /version} (configurable).
 *
 * <p>
 * Sibling of {@link FaviconMixin}, {@link SeoMixin}, and
 * {@link WellKnownMixin}. All four classes live in the
 * {@code org.apache.juneau.rest.convention} convention-endpoints mixin pack.
 *
 * <p>
 * Compose into a host resource via
 * {@link Rest#mixins() @Rest(mixins=VersionMixin.class)}; the three URLs become available
 * alongside the host's own endpoints with no further wiring.
 *
 * <h5 class='section'>Configurable mount path:</h5>
 *
 * <p>
 * The default mount {@code /version} can be overridden via the SVL variable
 * {@code ${juneau.version.path:version}} &mdash; set via system property
 * ({@code -Djuneau.version.path=build-info}), environment variable
 * ({@code JUNEAU_VERSION_PATH=build-info}), or {@code Config} key
 * ({@code juneau.version.path = build-info}) to change the runtime mount without subclassing.
 * Resolution happens once at {@link RestContext} construction time; see the FINISHED-99 archive
 * (SVL resolution in {@code @RestOp(path)}) for the full resolution chain.
 *
 * <p>
 * Override accepts bare token ({@code version}), leading slash ({@code /version}), or trailing
 * slash ({@code version/}) &mdash; all resolve to the same mount.
 *
 * <p>
 * <b>Migration note (9.5.0):</b> Earlier development snapshots of this mixin mounted at three
 * historical aliases &mdash; {@code /version}, {@code /info}, and {@code /about} &mdash; on a
 * single op. That multi-path default has been collapsed to a single SVL-configurable mount as
 * part of the "single path per op" principle (see FINISHED-101). Deployers who relied on the
 * {@code /info} or {@code /about} aliases must now either set
 * {@code -Djuneau.version.path=info} (or {@code about}), compose a second instance with the
 * desired override, or subclass and supply their own {@code @RestGet(path=...)} method.
 *
 * <h5 class='section'>Mixin-only deployment:</h5>
 *
 * <p>
 * This resource is designed for composition via {@code @Rest(mixins=...)}. The mount path is
 * pinned at the op level by {@link RestGet @RestGet(path="/${juneau.version.path:version}")} on
 * {@link #getInfo}; a class-level {@code @Rest(paths=...)} declaration would be silently
 * ignored under the mixin pattern (see {@link Rest#paths() @Rest(paths)} Javadoc).
 *
 * <h5 class='figure'>Composition example:</h5>
 *
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(path=<js>"/api"</js>, mixins=VersionMixin.<jk>class</jk>)
 * 	<jk>public class</jk> ApiResource <jk>extends</jk> RestServlet { }
 *
 * 	<jc>// Default behavior reads MANIFEST.MF + git.properties from the importer's classpath.</jc>
 * 	<jc>// Override programmatically via a @Bean factory:</jc>
 * 	<ja>@Bean</ja> VersionMixin version() {
 * 		<jk>return</jk> VersionMixin.<jsm>create</jsm>()
 * 			.entry(<js>"name"</js>, <js>"my-app"</js>)
 * 			.entry(<js>"version"</js>, <js>"1.2.3"</js>)
 * 			.build();
 * 	}
 * </p>
 *
 * <h5 class='section'>Default lookup chain:</h5>
 *
 * <p>
 * The default {@link Builder#fromManifest() fromManifest()} reader walks the
 * {@link VersionMixin} classloader for {@code /META-INF/MANIFEST.MF} and reads the standard
 * {@code Implementation-Title}, {@code Implementation-Version}, {@code Implementation-Vendor}, and
 * {@code Build-Jdk} attributes (lowercased to {@code name}, {@code version}, {@code vendor},
 * {@code javaVersion}). The default {@link Builder#fromGitProperties() fromGitProperties()} reader
 * walks the same classloader for {@code /git.properties} (the canonical
 * {@code git-commit-id-maven-plugin} output) and surfaces {@code git.commit.id},
 * {@code git.branch}, and {@code git.build.time} as {@code gitCommit}, {@code gitBranch}, and
 * {@code buildTime}. Missing files / missing keys map to {@code "(unknown)"} or are simply omitted
 * &mdash; never an exception.
 *
 * <p>
 * <b>Spring Boot fat-jar caveat:</b> Spring Boot rewrites {@code MANIFEST.MF} during repackaging.
 * The mixin's no-arg-default reader uses {@code VersionMixin.class.getClassLoader()};
 * users who want the importer's app manifest under a Spring Boot executable jar should register an
 * explicit {@link Builder#fromManifest(ClassLoader) fromManifest(importerClassLoader)} call so the
 * lookup starts from the importer's classloader rather than the framework's.
 *
 * <h5 class='section'>Output:</h5>
 *
 * <p>
 * All three URLs return the same JSON map &mdash; e.g.:
 * <p class='bjson'>
 * 	{
 * 		<jok>"name"</jok>: <jov>"my-app"</jov>,
 * 		<jok>"version"</jok>: <jov>"1.2.3"</jov>,
 * 		<jok>"gitCommit"</jok>: <jov>"abc123"</jov>,
 * 		<jok>"gitBranch"</jok>: <jov>"main"</jov>,
 * 		<jok>"buildTime"</jok>: <jov>"2026-05-24T18:00:00Z"</jov>,
 * 		<jok>"javaVersion"</jok>: <jov>"21"</jov>
 * 	}
 * </p>
 *
 * <p>
 * Endpoints are excluded from generated Swagger / OpenAPI specs via
 * {@link OpSwagger#ignore() @OpSwagger(ignore=true)}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link FaviconMixin}
 * 	<li class='jc'>{@link SeoMixin}
 * 	<li class='jc'>{@link WellKnownMixin}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server &mdash; Composition (mixins, paths)</a>
 * </ul>
 *
 * @since 9.5.0
 */
// @formatter:off
@Rest
public class VersionMixin {

	/** Sentinel value returned for entries that the mixin couldn't resolve. */
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
	 * version from the framework classloader. Equivalent to
	 * {@code create().build()}; the builder applies the same defaults whether instantiated
	 * directly or via the {@link org.apache.juneau.commons.inject.BeanInstantiator BeanInstantiator}
	 * builder-detection path.
	 */
	public VersionMixin() {
		this(create());
	}

	/**
	 * Builder constructor.
	 *
	 * <p>
	 * Applies the default lookup chain ({@link Builder#fromManifest()} +
	 * {@link Builder#fromGitProperties()} + {@link Builder#fromJavaVersion()}) when no other
	 * builder method has been called &mdash; ensuring that a freshly-created builder
	 * (whether instantiated directly via {@code new VersionMixin(create())} or via the
	 * {@link org.apache.juneau.commons.inject.BeanInstantiator BeanInstantiator}'s
	 * builder-detection path) always produces a non-empty payload.
	 *
	 * @param builder The builder.
	 */
	protected VersionMixin(Builder builder) {
		if (! builder.explicit)
			builder.fromManifest().fromGitProperties().fromJavaVersion();
		info = Collections.unmodifiableMap(new LinkedHashMap<>(builder.entries));
	}

	/**
	 * [GET /version] &mdash; emit the assembled metadata as a JSON map.
	 *
	 * <p>
	 * Format-pinned to {@code application/json} per the v1 resolved decision &mdash; bypasses
	 * the host's content negotiation so the endpoint serves JSON even on a vanilla
	 * {@code RestServlet} host that hasn't wired up JSON serializers explicitly.
	 *
	 * @param res The current REST response.
	 * @throws IOException If an I/O error occurs while writing the response.
	 */
	@RestGet(
		path="/#{pathToken(${juneau.version.path:version})}",
		summary="Version / build metadata",
		description="Deployment-introspection metadata (name, version, git, build).",
		swagger=@OpSwagger(ignore=true)
	)
	public void getInfo(RestResponse res) throws IOException {
		try (var w = res.getDirectWriter("application/json")) {
			JsonSerializer.DEFAULT_READABLE.serialize(info, w);
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
	 * Builder for {@link VersionMixin} instances.
	 */
	public static class Builder {

		private final Map<String,String> entries = new LinkedHashMap<>();
		private boolean explicit;

		/**
		 * Env-driven default for the {@code javaVersion} entry; resolved from the {@code java.version}
		 * system property (defaulting to the literal {@code (unknown)}, matching
		 * {@link VersionMixin#UNKNOWN}). Populated by {@link BeanInstantiator} via
		 * {@link Value @Value} field injection.
		 *
		 * <p>
		 * The default is inlined as a string literal rather than referencing {@code UNKNOWN} via
		 * concatenation, per the project-wide rule that every {@code @Value} expression is a
		 * fully self-contained string literal.
		 */
		@Value("${java.version:(unknown)}")
		String javaVersionDefault;

		/** Constructor &mdash; protected access for {@link VersionMixin#create()}. */
		protected Builder() {}

		/**
		 * Sets a single entry, overwriting any prior value for that key.
		 *
		 * @param key The entry key. Must not be <jk>null</jk> or blank.
		 * @param value The entry value (a <jk>null</jk> value is recorded as
		 * 	{@link VersionMixin#UNKNOWN}).
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
		 * @param values Entries to add. <jk>null</jk> values are recorded as
		 * 	{@link VersionMixin#UNKNOWN}.
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
		 * {@code /META-INF/MANIFEST.MF} on the {@link VersionMixin} classloader.
		 *
		 * <p>
		 * Missing keys are recorded as {@link VersionMixin#UNKNOWN}. A missing
		 * {@code MANIFEST.MF} resource is silently skipped.
		 *
		 * @return This object.
		 */
		public Builder fromManifest() {
			return fromManifest(VersionMixin.class.getClassLoader());
		}

		/**
		 * Reads manifest attributes from {@code /META-INF/MANIFEST.MF} on the supplied classloader.
		 *
		 * <p>
		 * Useful under Spring Boot fat jars, where the importer's app manifest is reachable from
		 * its own classloader but not the framework's. Missing keys are recorded as
		 * {@link VersionMixin#UNKNOWN}; a missing {@code MANIFEST.MF} resource is silently
		 * skipped.
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
		 * <p>
		 * Convenience for callers that already loaded the manifest via Spring Boot's
		 * {@code BuildProperties} autoconfiguration or by hand.
		 *
		 * @param manifest The manifest to read. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder fromManifest(Manifest manifest) {
			var main = manifest.getMainAttributes();
			var attrs = new HashMap<String,String>();
			for (var k : main.keySet())
				attrs.put(k.toString(), String.valueOf(main.get(k)));
			ifNotEmpty(attrs, "Implementation-Title", v -> entry("name", v));
			ifNotEmpty(attrs, "Implementation-Version", v -> entry("version", v));
			ifNotEmpty(attrs, "Implementation-Vendor", v -> entry("vendor", v));
			ifNotEmpty(attrs, "Build-Jdk", v -> entry("javaVersion", v));
			explicit = true;
			return this;
		}

		/**
		 * Reads {@code git.commit.id}, {@code git.branch}, and {@code git.build.time} from
		 * {@code /git.properties} on the {@link VersionMixin} classloader.
		 *
		 * <p>
		 * Output of the
		 * <a href="https://github.com/git-commit-id/git-commit-id-maven-plugin">git-commit-id-maven-plugin</a>;
		 * other values written to the same file are also surfaced (each {@code git.foo.bar} key is
		 * remapped to {@code gitFooBar}).
		 *
		 * @return This object.
		 */
		public Builder fromGitProperties() {
			return fromGitProperties(VersionMixin.class.getClassLoader());
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
			entries.putIfAbsent("javaVersion", opt(javaVersionDefault).orElse(UNKNOWN));
			explicit = true;
			return this;
		}

		/**
		 * Builds a {@link VersionMixin} instance.
		 *
		 * <p>
		 * The constructor applies the default lookup chain ({@link #fromManifest()} +
		 * {@link #fromGitProperties()} + {@link #fromJavaVersion()}) when no builder method has
		 * been called &mdash; so a freshly-created builder always produces a non-empty payload
		 * regardless of which entry point is used.
		 *
		 * @return A configured instance.
		 */
		public VersionMixin build() {
			return new VersionMixin(this);
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
			if (v != null && !v.isEmpty())
				sink.accept(v);
		}

		private static void ifNotEmptyValue(String v, Consumer<String> sink) {
			if (v != null && !v.isEmpty())
				sink.accept(v);
		}
	}
}
