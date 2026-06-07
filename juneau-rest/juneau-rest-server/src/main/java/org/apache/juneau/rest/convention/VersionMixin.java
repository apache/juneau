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

import java.io.*;
import java.util.*;
import java.util.jar.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.servlet.*;

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
 * Resolution happens once at {@link RestContext} construction time.
 *
 * <p>
 * Override accepts bare token ({@code version}), leading slash ({@code /version}), or trailing
 * slash ({@code version/}) &mdash; all resolve to the same mount.
 *
 * <p>
 * <b>Migration note (10.0.0):</b> Earlier development snapshots of this mixin mounted at three
 * historical aliases &mdash; {@code /version}, {@code /info}, and {@code /about} &mdash; on a
 * single op. That multi-path default has been collapsed to a single SVL-configurable mount as
 * part of the "single path per op" principle. Deployers who relied on the
 * {@code /info} or {@code /about} aliases must now either set
 * {@code -Djuneau.version.path=info} (or {@code about}), compose a second instance with the
 * desired override, or subclass and supply their own {@code @RestGet(path=...)} method.
 *
 * <h5 class='section'>Mixin-only deployment:</h5>
 *
 * <p>
 * This resource is designed for composition via {@code @Rest(mixins=...)}. The mount path is
 * pinned at the op level by {@link RestGet @RestGet(path="/&#123;juneau.version.path:version&#125;")} on
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
 * @since 10.0.0
 */
// @formatter:off
@Rest
@SuppressWarnings({
	"java:S1192" // Duplicate string literals are REST endpoint paths and content-type values; intentional
})
public class VersionMixin extends RestMixin {

	/** Sentinel value returned for entries that the worker couldn't resolve. */
	public static final String UNKNOWN = VersionProvider.UNKNOWN;

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
		return new Builder(VersionProvider.create(beanStore));
	}

	private final VersionProvider worker;

	/**
	 * No-arg constructor &mdash; delegates to a default {@link VersionProvider} worker (reads
	 * {@code MANIFEST.MF}, {@code git.properties}, and the JVM version from the framework
	 * classloader).
	 */
	public VersionMixin() {
		this(new VersionProvider());
	}

	/**
	 * Worker constructor.
	 *
	 * @param worker The shared {@link VersionProvider} worker this flavor delegates to. Must not be
	 * 	<jk>null</jk>.
	 */
	protected VersionMixin(VersionProvider worker) {
		this.worker = worker;
	}

	/**
	 * Worker + builder constructor.
	 *
	 * <p>
	 * Used by {@link Builder#build()} to both delegate to the shared {@link VersionProvider} worker and stash the
	 * programmatic {@link RestBuilder}(carrying any {@code @Rest}-level overrides such as {@code path}) so those
	 * values take precedence over the mixin class's own {@link Rest @Rest} annotation.
	 *
	 * <h5 class='section'>Why worker + builder, not the flavor Builder:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Takes the already-built <b>worker bean</b> (not the flavor {@link Builder}) so the flavor can be
	 * 		constructed from ANY independently-supplied worker &mdash; e.g. a user's own {@code @Bean VersionProvider}
	 * 		or BeanStore bean, per the delegate-bean model &mdash; not only via this flavor's own builder.
	 * 	<li>Takes the generic {@link RestBuilder}(here {@code this} from {@link Builder#build()}) so it honors the
	 * 		uniform &sect;2.4 {@code Foo(RestBuilder<?>)} injection contract the base class and DI resolution key on; the
	 * 		base knows nothing about the concrete flavor builder or the worker type.
	 * 	<li>Holds the finished worker product (the {@code final} {@link VersionProvider} field), not a transient
	 * 		builder; the worker is materialized exactly once at {@link Builder#build()} time.
	 * 	<li>Keeps the capability worker and the REST-level config as two distinct inputs.
	 * </ul>
	 *
	 * @param worker The shared {@link VersionProvider} worker this flavor delegates to. Must not be <jk>null</jk>.
	 * @param builder The programmatic configuration builder. May be <jk>null</jk>.
	 */
	protected VersionMixin(VersionProvider worker, RestBuilder<?> builder) {
		super(builder);
		this.worker = worker;
	}

	/**
	 * [GET /version] &mdash; emit the assembled metadata as a JSON map.
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
		worker.serve(res);
	}

	/**
	 * Returns the configured info map (test/inspection helper).
	 *
	 * @return The info map.
	 */
	public Map<String,String> getInfoMap() {
		return worker.getInfoMap();
	}

	/**
	 * Builder for {@link VersionMixin} instances.
	 *
	 * <p>
	 * Mirrors {@link VersionProvider.Builder}'s configuration methods on the mixin's own surface and
	 * forwards each call to an underlying {@link VersionProvider.Builder}, which builds the shared worker
	 * the mixin delegates to.
	 *
	 * <p>
	 * Extends {@link org.apache.juneau.rest.servlet.RestMixin.Builder} so the mixin's bespoke worker-config setters chain
	 * with true covariant returns alongside the inherited {@link RestBuilder}surface (e.g. {@code path},
	 * {@code roleGuard}). The worker config is forwarded once into {@link VersionProvider.Builder} and the REST
	 * config is inherited once from {@link AbstractRestBuilder} &mdash; no triplication across the Version flavors.
	 */
	public static class Builder extends RestMixin.Builder<VersionMixin, Builder> {

		private final VersionProvider.Builder worker;

		/** Constructor &mdash; protected access for {@link VersionMixin#create()}. */
		protected Builder(VersionProvider.Builder worker) {
			super(VersionMixin.class);
			this.worker = worker;
		}

		/**
		 * Sets a single entry, overwriting any prior value for that key.
		 *
		 * @param key The entry key. Must not be <jk>null</jk> or blank.
		 * @param value The entry value (a <jk>null</jk> value is recorded as
		 * 	{@link VersionMixin#UNKNOWN}).
		 * @return This object.
		 */
		public Builder entry(String key, String value) {
			worker.entry(key, value);
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
			worker.entries(values);
			return this;
		}

		/**
		 * Reads {@code Implementation-Title}, {@code Implementation-Version},
		 * {@code Implementation-Vendor}, and {@code Build-Jdk} from
		 * {@code /META-INF/MANIFEST.MF} on the framework classloader.
		 *
		 * @return This object.
		 */
		public Builder fromManifest() {
			worker.fromManifest();
			return this;
		}

		/**
		 * Reads manifest attributes from {@code /META-INF/MANIFEST.MF} on the supplied classloader.
		 *
		 * @param classLoader The classloader to walk.
		 * @return This object.
		 */
		public Builder fromManifest(ClassLoader classLoader) {
			worker.fromManifest(classLoader);
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
			worker.fromManifest(manifest);
			return this;
		}

		/**
		 * Reads {@code git.commit.id}, {@code git.branch}, and {@code git.build.time} from
		 * {@code /git.properties} on the framework classloader.
		 *
		 * @return This object.
		 */
		public Builder fromGitProperties() {
			worker.fromGitProperties();
			return this;
		}

		/**
		 * Reads {@code git.properties} from the supplied classloader.
		 *
		 * @param classLoader The classloader to walk.
		 * @return This object.
		 */
		public Builder fromGitProperties(ClassLoader classLoader) {
			worker.fromGitProperties(classLoader);
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
			worker.fromJavaVersion();
			return this;
		}

		/**
		 * Builds a {@link VersionMixin} instance.
		 *
		 * @return A configured instance.
		 */
		@Override /* AbstractRestBuilder */
		public VersionMixin build() {
			return new VersionMixin(worker.build(), this);
		}
	}
}
