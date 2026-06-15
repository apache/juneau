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
package org.apache.juneau.rest.server;

import static jakarta.servlet.http.HttpServletResponse.*;
import static java.util.Collections.*;
import static org.apache.juneau.commons.reflect.AnnotationTraversal.*;
import static org.apache.juneau.commons.reflect.ReflectionUtils.*;
import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.IoUtils.*;
import static org.apache.juneau.commons.utils.PredicateUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.emptyIfNull;
import static org.apache.juneau.commons.utils.StringUtils.isEmpty;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;
import static org.apache.juneau.commons.utils.Utils.eq;
import static org.apache.juneau.rest.server.RestOpAnnotation.*;
import static org.apache.juneau.rest.server.RestServerConstants.*;
import static org.apache.juneau.rest.server.processor.ResponseProcessor.*;
import static org.apache.juneau.rest.server.util.RestUtils.*;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.lang.reflect.Method;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.logging.*;
import java.util.stream.*;

import org.apache.juneau.bean.openapi3.OpenApi;
import org.apache.juneau.bean.rfc7807.*;
import org.apache.juneau.bean.rfc7807.adapter.*;
import org.apache.juneau.bean.swagger.Swagger;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.commons.function.*;
import org.apache.juneau.commons.http.MediaType;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.lang.*;
import org.apache.juneau.commons.logging.Logger;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.commons.reflect.ParameterInfo;
import org.apache.juneau.commons.settings.*;
import org.apache.juneau.commons.svl.*;
import org.apache.juneau.commons.utils.*;
import org.apache.juneau.config.*;
import org.apache.juneau.config.vars.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.Response;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.cp.*;
import org.apache.juneau.marshall.encoders.*;
import org.apache.juneau.marshall.html.*;
import org.apache.juneau.marshall.httppart.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.jsonschema.*;
import org.apache.juneau.marshall.oapi.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.rest.server.arg.*;
import org.apache.juneau.rest.server.config.*;
import org.apache.juneau.rest.server.debug.*;
import org.apache.juneau.rest.server.debug.format.*;
import org.apache.juneau.rest.server.httppart.*;
import org.apache.juneau.rest.server.logger.*;
import org.apache.juneau.rest.server.metrics.*;
import org.apache.juneau.rest.server.openapi.*;
import org.apache.juneau.rest.server.processor.*;
import org.apache.juneau.rest.server.rrpc.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.staticfile.*;
import org.apache.juneau.rest.server.stats.*;
import org.apache.juneau.rest.server.swagger.*;
import org.apache.juneau.rest.server.tracing.*;
import org.apache.juneau.rest.server.util.*;
import org.apache.juneau.rest.server.validation.*;
import org.apache.juneau.rest.server.vars.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

/**
 * Defines the initial configuration of a <c>RestServlet</c> or <c>@Rest</c> annotated object.
 *
 * <p>
 * An extension of the {@link ServletConfig} object used during servlet initialization.
 *
 * <p>
 * Configuration is supplied declaratively through the {@link Rest @Rest} annotation on the resource class
 * (and inherited from any parent classes), and programmatically through {@link Bean @Bean}-annotated
 * methods/fields that contribute named beans (e.g. <c>encoders</c>, <c>parsers</c>, <c>callLogger</c>) to the REST
 * resource's bean store. Where direct construction is needed (test rigs, mock clients, embedded usage),
 * the public constructor takes a {@link RestContext.Args} record carrying the bootstrap state.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(path=<js>"/myresource"</js>, serializers=JsonSerializer.<jk>class</jk>, parsers=JsonParser.<jk>class</jk>)
 * 	<jk>public class</jk> MyResource {
 *
 * 		<jc>// Programmatically contribute a bean to the resource's bean store.</jc>
 * 		<ja>@Bean</ja>
 * 		<jk>public</jk> CallLogger callLogger() {
 * 			<jk>return new</jk> MyCustomCallLogger();
 * 		}
 * 	}
 * </p>
 *
 * <p>
 * The historical <c>public MyResource(RestContext.Builder builder)</c> constructor-injection pattern and the
 * <c>{@link RestInit @RestInit} public void init(RestContext.Builder builder)</c> method-injection pattern were both
 * removed in 10.0. {@code RestContext.Builder} itself is on the deletion path; new code should not depend on it.
 * See <a class="doclink" href="https://juneau.apache.org/docs/topics/V10.0MigrationGuide">v10.0 Migration Guide</a>
 * for replacement recipes.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestContext">RestContext</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S112",  // RuntimeException used in memoizer lambdas to re-wrap checked exceptions (ServletException/Exception) that Supplier<T> cannot declare
	"java:S115",  // Constants use UPPER_snakeCase convention (e.g., PROP_allowContentParam)
	"java:S1200", // Class has many dependencies; acceptable for this core context class
	"java:S1192", // Duplicate string literals are property key names and REST annotation attribute values; intentional
	"java:S125",  // Explanatory comments reference SVL ($C{...}/$E{...}) and annotation (@Bean/@Rest) tokens that Sonar misreads as commented-out code
	"java:S3776", // Cognitive complexity in field-initializer lambdas (memoizer wiring); cannot annotate at lambda scope
	"java:S6539", // Monster class; RestContext is intentionally a central hub for REST framework configuration
	"resource"    // Streams and session objects returned to callers; lifecycle managed by the servlet container or RestCall
})
public class RestContext extends Context {

	private static final Logger LOG = Logger.getLogger(RestContext.class);

	// Property name constants
	private static final String PROP_allowContentParam = "allowContentParam";
	private static final String PROP_beanStore = "beanStore";
	private static final String PROP_consumes = "consumes";
	private static final String PROP_defaultRequestAttributes = "defaultRequestAttributes";
	private static final String PROP_defaultRequestHeaders = "defaultRequestHeaders";
	private static final String PROP_defaultResponseHeaders = "defaultResponseHeaders";
	private static final String PROP_partParser = "partParser";
	private static final String PROP_partSerializer = "partSerializer";
	private static final String PROP_produces = "produces";
	private static final String PROP_responseProcessors = "responseProcessors";
	private static final String PROP_restOpArgs = "restOpArgs";
	private static final String PROP_bootstrapVarResolver = "bootstrapVarResolver";
	private static final String PROP_staticFiles = "staticFiles";
	private static final String PROP_swaggerProvider = "swaggerProvider";
	private static final String PROP_openApiProvider = "openApiProvider";

	/**
	 * Bootstrap arguments for {@link RestContext}.
	 *
	 * <p>
	 * Bundles the small, fixed set of inputs needed to construct a {@link RestContext} into a single immutable record.
	 *
	 * <p>
	 * All non-required values default sensibly:
	 * <ul>
	 * 	<li>{@code parentContext}, {@code servletConfig} — {@code null} (top-level resource, no servlet container)
	 * 	<li>{@code path} — {@code ""} (no path prefix)
	 * 	<li>{@code beanStoreConfigurer} — no-op (no pre-build bean-store mutation)
	 * 	<li>{@code overridingParent} — {@code null} (no test-time override layer)
	 * </ul>
	 *
	 * <p>
	 * The {@code beanStoreConfigurer} hook gives test fixtures and integration code a chance to register beans on
	 * the {@link WritableBeanStore} after the resource has been wired in but before the
	 * {@code findXxx()} memoizers fire.
	 *
	 * <p>
	 * The {@code overridingParent} hook installs a {@link BeanStore} into the {@code overridingParent} slot of the
	 * freshly-constructed {@link BasicBeanStore} so that test-time overrides resolve at tier 1 of the lookup chain
	 * (above the resource's local {@code @Bean} factory entries and the Spring bridge).  This is the
	 * canonical Mode INJECT wiring point for {@code juneau-junit5}'s {@code TestBeanStore}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>var</jk> <jv>ctx</jv> = <jk>new</jk> RestContext(<jk>new</jk> RestContext.Args(MyResource.<jk>class</jk>, () -&gt; <jk>new</jk> MyResource()));
	 * </p>
	 *
	 * @param resourceClass The {@link Rest @Rest}-annotated REST resource class. Must not be {@code null}.
	 * @param parentContext The parent {@link RestContext}, or {@code null} if this is a top-level resource.
	 * @param servletConfig The {@link ServletConfig} from the servlet container, or {@code null} when none is available.
	 * @param resource The supplier that provides the resource instance during initialization. Must not be {@code null}.
	 * @param path The path prefix relative to the parent. Defaults to {@code ""}.
	 * @param beanStoreConfigurer A pre-build hook that runs against the resolved {@link WritableBeanStore}
	 * 	after the resource has been wired in but before any {@code findXxx()} memoizer fires. Defaults to a no-op.
	 * @param overridingParent An optional test-time override layer installed into the {@code overridingParent}
	 * 	slot of the freshly-constructed {@link BasicBeanStore}.  May be {@code null}.  Honored only when the
	 * 	resource does <i>not</i> declare an {@code @Bean WritableBeanStore} factory method; user-supplied bean
	 * 	stores are passed through unchanged.
	 * @param paths An optional programmatic override for the top-level mount-paths array (the highest-priority
	 * 	rung in the runtime-override chain documented on {@link RestContext#getPaths()}).  {@code null} (the
	 * 	default) means &quot;no programmatic override&quot; &mdash; resolution falls through to the resource's
	 * 	{@code getPaths()} getter, then {@link Rest#paths()} (SVL-resolved per element and comma-split).
	 * 	An empty array explicitly clears the mount list (no top-level mounts).
	 * @param mixinContext {@code true} when this context is a per-mixin sub-context constructed by the host's
	 * 	{@code restOperations} memoizer (parent-linked to the host's {@link RestContext} so that
	 * 	{@link RestContext#getRestAnnotationsForProperty(String) annotation-property walks} prepend the host's
	 * 	{@code @Rest} chain before the mixin's own).  {@code false} for top-level resources and for
	 * 	{@link Rest#children() @Rest(children)} sub-resources (which retain pre-10.0.0 isolated resolution).
	 *
	 * @since 10.0.0
	 */
	@SuppressWarnings({
		"java:S6218" // Args is a transient constructor-parameter holder; it is never compared for equality or used as a map key, so array-identity semantics on the paths[] component are irrelevant.
	})
	public static record Args(
		Class<?> resourceClass,
		RestContext parentContext,
		ServletConfig servletConfig,
		Supplier<?> resource,
		String path,
		Consumer<WritableBeanStore> beanStoreConfigurer,
		BeanStore overridingParent,
		String[] paths,
		boolean mixinContext,
		RestBuilder<?> restBuilder
	) {

		/**
		 * Compact canonical constructor — null-coalesces optional fields and validates required ones.
		 */
		public Args {
			assertArgNotNull("resourceClass", resourceClass);
			assertArgNotNull("resource", resource);
			if (path == null)
				path = "";
			if (beanStoreConfigurer == null)
				beanStoreConfigurer = bs -> {};
		}

		/**
		 * Back-compatible constructor without the {@code restBuilder} component (defaults it to <jk>null</jk>).
		 *
		 * <p>
		 * The {@code restBuilder} is then resolved during {@link RestContext} construction from the
		 * resource instance's stashed builder, so call sites that don't carry one keep working unchanged.
		 *
		 * @param resourceClass The resource class.
		 * @param parentContext The parent context, or <jk>null</jk>.
		 * @param servletConfig The servlet config, or <jk>null</jk>.
		 * @param resource The resource supplier.
		 * @param path The mount path override, or <jk>null</jk>.
		 * @param beanStoreConfigurer The bean-store configurer, or <jk>null</jk>.
		 * @param overridingParent The overriding parent bean store, or <jk>null</jk>.
		 * @param paths The programmatic mount-paths override, or <jk>null</jk>.
		 * @param mixinContext Whether this is a mixin sub-context.
		 */
		public Args(Class<?> resourceClass, RestContext parentContext, ServletConfig servletConfig, Supplier<?> resource, String path, Consumer<WritableBeanStore> beanStoreConfigurer, BeanStore overridingParent, String[] paths, boolean mixinContext) {
			this(resourceClass, parentContext, servletConfig, resource, path, beanStoreConfigurer, overridingParent, paths, mixinContext, null);
		}
	}

	/**
	 * Builder class.
	 *
	 * <p>
	 * Demoted to package-private in the April 2026 refactor (2026-04-19). User code should construct a
	 * {@link RestContext} via the public {@link #RestContext(Args)} constructor; this Builder
	 * exists only as internal bootstrap state for the framework and is slated for inlining in a later phase.
	 */
	@SuppressWarnings({
		"rawtypes" // Raw types required: annotation type parameter is unknown at static analysis time.
	})
	static class Builder extends Context.Builder implements ServletConfig {

		private final Class<?> resourceClass;
		private final RestContext parentContext;
		private final ServletConfig inner;
		final Args args;

		/**
		 * Programmatic top-level mount-paths override.
		 *
		 * <p>
		 * Highest-priority rung in the runtime-override chain documented on {@link RestContext#getPaths()}.
		 * Set via {@link #paths(String...)}.
		 *
		 * <ul>
		 * 	<li>{@code null} (default) &mdash; no programmatic override; falls through to the resource's
		 * 		{@code getPaths()} getter, then the {@code @Rest(paths=...)} annotation default (SVL-resolved
		 * 		per element and comma-split).
		 * 	<li>Empty array &mdash; explicit clear; resource is resolved with no top-level mounts.
		 * 	<li>Non-empty array &mdash; replaces all lower rungs.
		 * </ul>
		 */
		String[] paths;

		/**
		 * Programmatic override for the MDC async propagation flag.
		 *
		 * <p>
		 * {@code null} (default) &mdash; defers to the {@code RestContext.mdcAsyncPropagation} env-driven
		 * default (itself defaulting to {@code true} when SLF4J is detected). Set via
		 * {@link #mdcAsyncPropagation(boolean)}.
		 */
		Boolean mdcAsyncPropagation;

		/**
		 * Programmatic override for the async-completion executor bean name.
		 *
		 * <p>
		 * {@code null} (default) &mdash; resolved from the {@code @Rest(asyncCompletionExecutor)} annotation chain.
		 * Set via {@link #asyncCompletionExecutor(String)} to supply the bean-name programmatically (useful in
		 * test rigs that construct a {@link RestContext} directly without annotations).
		 */
		String asyncCompletionExecutorName;

		/**
		 * Programmatic override for the lazy-children flag.
		 *
		 * <p>
		 * {@code null} (default) &mdash; defers to the {@link Rest#lazyChildren() @Rest(lazyChildren)} annotation
		 * chain, then to the {@code RestContext.lazyChildren} env-driven default (itself defaulting to
		 * {@code false}). Set via {@link #lazyChildInit(boolean)}.
		 *
		 * <p>
		 * When {@code true}, all children registered via {@link Rest#children() @Rest(children)} on this resource
		 * are built lazily on first invocation rather than eagerly at parent startup.
		 */
		Boolean lazyChildInit;

		/**
		 * Package-private constructor.
		 *
		 * <p>
		 * Minimized in the May 2026 refactor — the beanStore setup, {@code @Bean} processing,
		 * {@code @RestInit} hooks, and {@code beanStoreConfigurer} call were all moved into
		 * {@link RestContext#RestContext(Builder)}. The Builder now only stores the three fields
		 * that are final and needed by the {@link ServletConfig} overrides and factory methods.
		 *
		 * @param rci The bootstrap arguments. Must not be {@code null}.
		 */
		Builder(Args rci) {
			this.resourceClass = rci.resourceClass();
			this.inner = rci.servletConfig();
			this.parentContext = rci.parentContext();
			this.args = rci;
			this.paths = rci.paths();
		}

		/**
		 * Programmatic override for the top-level mount-paths array.
		 *
		 * <p>
		 * This is the highest-priority rung in the runtime-override chain &mdash; see
		 * {@link RestContext#getPaths()} for the full precedence order.
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>
		 * 		Passing {@code null} resets the override and lets the lower rungs ({@code getPaths()} getter,
		 * 		{@code @Rest(paths)} annotation) resolve.
		 * 	<li class='note'>
		 * 		Passing {@code new String[0]} explicitly clears the mount list &mdash; the resource ends up with
		 * 		no top-level mounts, which surfaces a clear &quot;no mounts&quot; error from the hosting runtime.
		 * </ul>
		 *
		 * @param value The new mount paths, or {@code null} to inherit. Empty array clears.
		 * @return This object.
		 * @since 10.0.0
		 */
		public Builder paths(String...value) {
			paths = value;
			return this;
		}

		/**
		 * Enables or disables SLF4J MDC propagation from the request thread to
		 * {@link java.util.concurrent.CompletableFuture} completion threads.
		 *
		 * <p>
		 * Default is {@code true} (enabled) when SLF4J is on the runtime classpath.
		 * When enabled, the {@link org.apache.juneau.rest.server.processor.MdcAsyncListener} snapshots the
		 * request thread's MDC map before the async dispatch and restores it inside the
		 * {@link java.util.concurrent.CompletionStage#whenComplete(java.util.function.BiConsumer) whenComplete}
		 * callback so that log statements emitted during async completion see the original request context.
		 *
		 * <p>
		 * Call {@code mdcAsyncPropagation(false)} to opt a resource out of MDC propagation. The global default
		 * can also be overridden via the {@code RestContext.mdcAsyncPropagation} env-driven default.
		 *
		 * @param value {@code false} to disable MDC propagation on this resource.
		 * @return This object.
		 * @since 10.0.0
		 */
		public Builder mdcAsyncPropagation(boolean value) {
			mdcAsyncPropagation = value;
			return this;
		}

		/**
		 * Programmatically sets the {@link java.util.concurrent.Executor} bean name used to route
		 * {@link java.util.concurrent.CompletableFuture} completion callbacks through a dedicated thread pool
		 * Equivalent to {@link Rest#asyncCompletionExecutor()} on the resource class.
		 *
		 * <p>
		 * The value is looked up by name in the resource's bean store at context-build time. If the name does
		 * not resolve to an {@link java.util.concurrent.Executor} bean, the context constructor throws. Pass
		 * {@code null} (or omit this call) to clear the override and fall back to the annotation chain.
		 *
		 * @param beanName The bean-store name of the {@link java.util.concurrent.Executor} to use, or {@code null} to clear.
		 * @return This object.
		 * @since 10.0.0
		 */
		public Builder asyncCompletionExecutor(String beanName) {
			asyncCompletionExecutorName = beanName;
			return this;
		}

		/**
		 * Programmatically opt this resource into (or out of) deferred child construction.
		 *
		 * <p>
		 * When {@code true}, all children registered via {@link Rest#children() @Rest(children)} are built
		 * lazily on first invocation instead of eagerly at parent startup.  The routing entry for each child
		 * is always populated at startup so URL matching works immediately.
		 *
		 * <p>
		 * This is the highest-priority knob — it overrides both the
		 * {@link Rest#lazyChildren() @Rest(lazyChildren)} annotation and the
		 * {@code RestContext.lazyChildren} env-driven default.
		 *
		 * @param value {@code true} to defer child construction to first invocation.
		 * @return This object.
		 * @since 10.0.0
		 */
		public Builder lazyChildInit(boolean value) {
			lazyChildInit = value;
			return this;
		}

		@Override /* Context.Builder<?> is abstract - copy() is not meaningful for the transient RestContext bootstrap state. */
		public Builder copy() {
			throw new NoSuchMethodError("Not implemented.");
		}

		@Override /* Overridden from ServletConfig */
		public String getInitParameter(String name) {
			return inner == null ? null : inner.getInitParameter(name);
		}

		@Override /* Overridden from ServletConfig */
		public Enumeration<String> getInitParameterNames() { return opt(inner).map(ServletConfig::getInitParameterNames).orElseGet(() -> new Vector<String>().elements()); }

		@Override /* Overridden from ServletConfig */
		public ServletContext getServletContext() {
			if (nn(inner)) {
				return inner.getServletContext();
			} else if (nn(parentContext)) {
				return parentContext.getBuilder().getServletContext();
			} else {
				return null;
			}
		}

		@Override /* Overridden from ServletConfig */
		public String getServletName() { return opt(inner).map(ServletConfig::getServletName).orElse(null); }

	}

	private static final Map<Class<?>,RestContext> REGISTRY = new ConcurrentHashMap<>();

	/**
	 * Returns a registry of all created {@link RestContext} objects.
	 *
	 * @return An unmodifiable map of resource classes to {@link RestContext} objects.
	 */
	public static final Map<Class<?>,RestContext> getGlobalRegistry() { return u(REGISTRY); }

	static ServletException servletException(String msg, Object...args) {
		return new ServletException(f(msg, args));
	}

	static ServletException servletException(Throwable t, String msg, Object...args) {
		return new ServletException(f(msg, args), t);
	}

	private static <T extends Annotation> Stream<Method> getAnnotatedMethods(Supplier<?> resource, Class<T> annotation) {
		return ClassInfo.ofProxy(resource.get()).getAllMethodsTopDown().stream()
			.filter(y -> y.hasAnnotation(annotation))
			.filter(distinctByKey(MethodInfo::getSignature))
			.map(y -> y.accessible().inner());
	}

	/**
	 * Resolves the top-level mount paths for a resource using the runtime-override chain.
	 *
	 * <p>
	 * Walks the three rungs in precedence order (highest first):
	 * <ol>
	 * 	<li>Programmatic override on {@link Builder#paths} (set by {@link Builder#paths(String...)} or seeded from
	 * 		{@link Args#paths()}).
	 * 	<li>The resource's {@code getPaths()} getter (when non-{@code null}).
	 * 	<li>{@link Rest#paths() @Rest(paths)} annotation default, walked top-down with the most-derived non-empty
	 * 		array winning.  Each element is SVL-resolved against a session of {@code vr} backed by
	 * 		{@code beanStore} (so {@code $C{key}} consults whatever {@link Config} is registered), then
	 * 		comma-split.
	 * </ol>
	 *
	 * @return The resolved paths array (never {@code null}; possibly empty).
	 */
	private static String[] resolveMountPaths(Builder builder, Object resource, VarResolver vr, WritableBeanStore beanStore, List<AnnotationInfo<Rest>> annotations) {
		// Rung 1: programmatic override (Builder.paths / Args.paths).
		var programmatic = builder.paths;
		if (programmatic != null)
			return programmatic.clone();
		return resolvePathsCore(annotations, resource, vr, beanStore);
	}

	/**
	 * Public mount-time path resolver for use by hosting runtimes that mount {@code @Rest}-annotated resources
	 * before the {@link RestContext} has been constructed (e.g. {@code JettyServerComponent}'s auto-discovery
	 * loop, or Spring Boot's {@code ServletRegistrationBean} wiring).
	 *
	 * <p>
	 * Operates on the same chain as {@link RestContext#getPaths()} but skips the programmatic Builder rung
	 * (which is only meaningful inside the {@link RestContext} constructor):
	 * <ol>
	 * 	<li>The resource's {@code getPaths()} virtual getter (defined on {@code RestServlet} /
	 * 		{@code RestResource}). {@code null} return falls through; non-{@code null} (including empty array) wins.
	 * 	<li>{@link Rest#paths() @Rest(paths)} annotation default on the most-derived class, SVL-resolved per
	 * 		element and comma-split.  A {@code VarResolver} is looked up on {@code store} (when present); SVL
	 * 		variables like {@code $C{key}} resolve against beans visible to the bean store.
	 * </ol>
	 *
	 * @param resourceClass The {@code @Rest}-annotated resource class. Must not be {@code null}.
	 * @param resource The resource instance. May be {@code null} (in which case the getter rung is skipped).
	 * @param store The bean store used to look up the {@link VarResolver} for SVL substitution on annotation
	 * 	elements, and to back the resolver session for bean-driven SVL vars like {@code $C{key}}. May be
	 * 	{@code null} (SVL is then skipped; literals pass through unchanged).
	 * @return The resolved paths array (never {@code null}; possibly empty).
	 * @since 10.0.0
	 */
	public static String[] resolveTopLevelPaths(Class<?> resourceClass, Object resource, BeanStore store) {
		var ap = AnnotationProvider.INSTANCE;
		var ci = ClassInfo.of(resourceClass);
		var annotations = rstream(ap.find(Rest.class, ci)).toList();
		var vr = (store == null) ? null : store.getBean(VarResolver.class).orElse(null);
		// Cast for the bean-store-backed resolver session only — when store is a plain BeanStore (not
		// WritableBeanStore) we skip the session form and call vr.resolve(...) directly.
		var ws = (store instanceof WritableBeanStore wbs) ? wbs : null;
		return resolvePathsCore(annotations, resource, vr, ws);
	}

	private static String[] resolvePathsCore(List<AnnotationInfo<Rest>> annotations, Object resource, VarResolver vr, WritableBeanStore beanStore) {
		// Rung 2: getPaths() virtual getter on the resource (RestServlet / RestResource / any matching class).
		// The getter return is normalized to a flat List<String> of raw leaves (any String / String[] /
		// Collection / Iterable / Stream / nested mix of those), then each leaf flows through the same
		// SVL-resolve + comma-split pipeline used for @Rest(paths=...) annotation elements.
		var raw = invokeGetPaths(resource);
		if (raw != null) {
			var leaves = normalizePaths(raw);
			return expandPathsElements(leaves.toArray(new String[0]), vr, beanStore);
		}

		// Rung 3: @Rest(paths) annotation default — most-derived non-empty array wins, with each element
		// SVL-resolved then comma-split (trim each piece, drop empties).  A single template element can
		// therefore expand to zero, one, or many mount paths.  Annotation arrays from javac are never
		// null, so the only filter needed is the empty-array case.
		return annotations.stream()
			.map(ai -> ai.inner().paths())
			.filter(arr -> arr.length > 0)
			.findFirst()
			.map(arr -> expandPathsElements(arr, vr, beanStore))
			.orElseGet(() -> new String[0]);
	}

	/**
	 * Applies the per-element SVL + comma-split pipeline to a {@code @Rest(paths=...)} array.
	 *
	 * <p>
	 * Each input element is SVL-resolved (via a session of the bootstrap {@link VarResolver} that's backed
	 * by {@code beanStore} when one is available, so bean-driven vars like {@code $C{key}} can find the
	 * registered {@link Config}), then split on {@code ,}; each piece is trimmed and empty pieces are
	 * dropped.  This lets a single element like {@code "$C{health.paths}"} or
	 * {@code "$E{HEALTH_PATHS,/healthz,/readyz}"} expand to multiple mount paths.
	 */
	private static String[] expandPathsElements(String[] elements, VarResolver vr, WritableBeanStore beanStore) {
		// Open a single session for the whole array — reusing it across elements avoids re-walking the
		// Var catalog and lets ConfigVar / FileVar / etc. all share one bean-lookup context.
		var session = (vr == null) ? null : vr.createSession(beanStore);
		var out = new ArrayList<String>(elements.length);
		for (var e : elements) {
			var resolved = applySvl(e, session);
			splitPathsValue(resolved, out);
		}
		return out.toArray(new String[0]);
	}

	private static String applySvl(String value, VarResolverSession session) {
		// Callers only pass annotation-array elements, which javac guarantees non-null.  An empty string
		// has no SVL markers to resolve, so we short-circuit it (avoids a pointless session.resolve call).
		if (session == null || value.isEmpty())
			return value;
		try {
			return session.resolve(value);
		} catch (@SuppressWarnings("unused") Exception e) {
			return value; // SVL failure: prefer the literal over throwing during construction.
		}
	}

	private static void splitPathsValue(String value, List<String> out) {
		// Callers pass the return of applySvl, which never returns null (we control both branches above).
		if (value.isBlank())
			return;
		var parts = value.split(",");
		for (var p : parts) {
			var t = p.trim();
			if (! t.isEmpty())
				out.add(t);
		}
	}

	/**
	 * Flattens any {@link Object} returned from {@code getPaths()} (String, {@code String[]},
	 * {@link Collection}, {@link Iterable}, {@link Stream}, primitive array, or nested mixes of these)
	 * into a flat {@link List} of raw leaf strings.
	 *
	 * <p>
	 * Non-{@link String} leaves are coerced via {@link String#valueOf(Object)}.  The returned list
	 * preserves traversal order; {@code null} leaves are dropped.  Comma-split + SVL resolution are
	 * intentionally <i>not</i> applied here &mdash; they happen downstream in
	 * {@link #expandPathsElements(String[], VarResolver, WritableBeanStore)} so SVL substitution sees the
	 * raw element (preserving {@code $E{NAME,defaultWithCommas}}-style defaults) before the post-SVL
	 * comma-split fires.
	 */
	private static List<String> normalizePaths(Object o) {
		if (o == null)
			return List.of();
		var out = new ArrayList<String>();
		CollectionUtils.<Object>accumulate(o).forEach(x -> {
			if (x != null)
				out.add(String.valueOf(x));
		});
		return out;
	}

	/**
	 * Reflectively invokes a no-arg {@code getPaths()} method on the resource (defined on
	 * {@link org.apache.juneau.rest.server.servlet.RestServlet RestServlet} and
	 * {@link org.apache.juneau.rest.server.servlet.RestResource RestResource}). Reflection is used to avoid coupling this
	 * static utility to either base class &mdash; if a custom {@code @Rest}-annotated POJO declares
	 * {@code public Object getPaths()} (or any covariant return type such as {@code String[]},
	 * {@code List<String>}, etc.), it participates uniformly.
	 *
	 * @return The raw getter return value, or {@code null} when no method exists, the method returns
	 * 	{@code void}, the method returned {@code null}, or reflective invocation failed.
	 */
	private static Object invokeGetPaths(Object resource) {
		if (resource == null)
			return null;
		Method m;
		try {
			m = resource.getClass().getMethod("getPaths");
		} catch (@SuppressWarnings("unused") NoSuchMethodException e) {
			return null;
		}
		if (m.getReturnType() == void.class)
			return null;
		try {
			return m.invoke(resource);
		} catch (@SuppressWarnings("unused") IllegalAccessException | java.lang.reflect.InvocationTargetException e) {
			return null;
		}
	}

	private static boolean isBeanMethod(MethodInfo mi) {
		return isBeanMethod(mi, null);
	}

	private static boolean isBeanMethod(MethodInfo mi, String name) {
		return mi.getAnnotations(Bean.class)
			.map(AnnotationInfo::inner)
			.anyMatch(x -> nn(x) && x.methodScope().length == 0 && (n(name) || eq(x.name(), name)));
	}

	protected final AtomicBoolean initialized = new AtomicBoolean(false);
	protected final BasicHttpException initException;
	protected final WritableBeanStore beanStore;
	protected final WritableBeanStore bootstrapBeanStore;  // Writable during bootstrap; exposed for child-context composition.
	protected final Builder builder;
	protected final Class<?> resourceClass;
	protected final ConcurrentHashMap<Locale,Swagger> swaggerCache = new ConcurrentHashMap<>();
	protected final ConcurrentHashMap<Locale,OpenApi> openApiCache = new ConcurrentHashMap<>();
	protected final Instant startTime;
	protected final RestContext parentContext;
	protected final boolean isMixinContext;
	protected final String fullPath;
	protected final String path;
	protected final String[] paths;
	protected final ThreadLocal<RestSession> localSession = new ThreadLocal<>();
	protected final UrlPathMatcher pathMatcher;
	private final Supplier<?> resource;
	private AnnotationWorkList annotationWork;

	/**
	 * The programmatic configuration builder for this resource, or <jk>null</jk> when configured purely
	 * by annotation.  Resolved during construction from {@link Args#restBuilder()} or, failing that, from the
	 * resource instance's stashed builder.  When non-<jk>null</jk>, a synthetic highest-priority {@code @Rest}
	 * annotation built from its set members is prepended to {@link #getRestAnnotations()} so builder-supplied
	 * values take precedence over the resource class's own {@code @Rest} annotation values.
	 */
	private final RestBuilder<?> restBuilder;

	/**
	 * The synthetic {@code @Rest} annotation built from {@link #restBuilder}'s set members, or <jk>null</jk> when
	 * there is no programmatic builder.  Prepended at the most-derived (child) position of
	 * {@link #getRestAnnotations()} so builder-supplied values win over the resource class's own annotation.
	 */
	private final Rest builderRestAnnotation;

	// Private accessors used by memoizer lambdas to satisfy Java's definite-assignment rules for blank final fields.
	private WritableBeanStore beanStore() { return beanStore; }
	private Supplier<?> resource() { return resource; }
	private Class<?> resourceClass() { return resourceClass; }
	private boolean isMixinContextField() { return isMixinContext; }

	/**
	 * Creates the bean store for this context.
	 *
	 * <p>
	 * The 10.0 precedence model places the parent (Spring or parent-resource bootstrap) as the
	 * overriding parent so it wins over local entries.  Memoizer-backed framework defaults are
	 * registered later in the constructor via {@code addDefaultSupplier}, putting them at the
	 * bottom of the resolution order.
	 *
	 * <p>
	 * Resolution:
	 * <ol>
	 * 	<li>If the resource declares an {@code @Bean} factory method returning a
	 * 		{@link WritableBeanStore} (e.g. {@code SpringRestServlet.createBeanStore(Optional<BeanStore>)}),
	 * 		that store is used directly.  Spring integration relies on this hook.
	 * 	<li>Otherwise a fresh {@link BasicBeanStore} is created with {@code parentBs} as its
	 * 		regular parent and {@code overridingParent} (if non-{@code null}) installed in the
	 * 		overriding-parent slot so test-time overrides win over local entries.
	 * </ol>
	 *
	 * @param parentBs
	 * 	The parent (bootstrap) bean store to layer onto, or {@code null} for root resources.
	 * @param resource
	 * 	The REST servlet/bean instance that this context is defined against.
	 * @param overridingParent
	 * 	Optional test-time override layer for the {@code overridingParent} slot of the freshly-constructed
	 * 	{@link BasicBeanStore}.  May be {@code null}.  Ignored when the resource supplies its own
	 * 	{@link WritableBeanStore} via an {@code @Bean} factory method &mdash; user-supplied stores are
	 * 	passed through unchanged.
	 * @return The bean store for this context.
	 */
	private WritableBeanStore createBeanStore(BeanStore parentBs, Supplier<?> resource, BeanStore overridingParent) {
		var defaultBs = new BasicBeanStore(parentBs, overridingParent);
		return defaultBs.createBeanFromMethod(WritableBeanStore.class, resource.get(), RestContext::isBeanMethod)
			.orElse(defaultBs);
	}
	private RestContext parentContext() { return parentContext; }
	private RestOperations restOperations() { return restOperations.get(); }

	/**
	 * Returns {@code true} when this context is a per-mixin sub-context constructed by the host's
	 * {@code restOperations} memoizer.
	 *
	 * <p>
	 * A mixin sub-context is parent-linked to the host's {@link RestContext} so that
	 * {@link #getRestAnnotationsForProperty(String) annotation-property walks} prepend the host's {@code @Rest}
	 * chain before the mixin's own &mdash; serializers, parsers, encoders, converters, response processors,
	 * REST op args, guards, callLogger, debugEnablement, messages, and varResolver tokens all inherit from the
	 * host first, with the mixin's contributions appended.  Use {@link Rest#noInherit() @Rest(noInherit)} on the
	 * mixin class to cut off inheritance for any specific property.
	 *
	 * <p>
	 * {@link Rest#children() @Rest(children)} sub-resources are <i>not</i> mixin contexts &mdash; they retain
	 * pre-10.0.0 isolated resolution (no parent-context walk).  The mixin-vs-child divergence is intentional:
	 * mixins are inline composers that share the host's URL namespace; children are heavyweight independent
	 * resources mounted at their own URL prefix.
	 *
	 * @return {@code true} if this is a mixin sub-context, {@code false} for top-level resources and child
	 * 	resources.
	 * @since 10.0.0
	 */
	public boolean isMixinContext() { return isMixinContext; }

	/**
	 * Returns the parent {@link RestContext}, or {@code null} for top-level resources.
	 *
	 * <p>
	 * The parent linkage drives the inheritance walk in {@link #getRestAnnotationsForProperty(String)} for
	 * {@linkplain #isMixinContext() mixin sub-contexts}, and the {@code parentContext} walks performed by
	 * {@link #getMessages()}, {@link #getVarResolver()}, {@code getFullPath()}, and the bean-store layering.
	 *
	 * @return The parent context, or {@code null} for top-level (host) resources.
	 * @since 10.0.0
	 */
	public RestContext getParentContext() { return parentContext; }

	/**
	 * Registers memoizer-backed default suppliers for every framework-managed bean type on the supplied
	 * bean store.
	 *
	 * <p>
	 * Default suppliers sit at the bottom of the bean-store resolution order: they fire only when no
	 * {@link Bean @Bean} method, no programmatic {@code addBean(...)} call, and no Spring/overriding-parent
	 * binding has been registered for the type.  This is the mechanism that replaces the old
	 * {@code DELAYED_INJECTION} list — by registering the framework's own factories as defaults
	 * <i>before</i> the {@link Bean @Bean} method walk runs, any {@link Bean @Bean} method whose parameters
	 * include framework types can now resolve those parameters lazily through the bean store without
	 * requiring a hand-maintained skip list.
	 */
	private void registerFrameworkDefaults(WritableBeanStore bs) {
		// @formatter:off
		// Unnamed framework types backed by per-resource memoizers.
		bs.addDefaultSupplier(MarshallingContext.class, marshallingContext::get);
		bs.addDefaultSupplier(EncoderSet.class, encoders::get);
		bs.addDefaultSupplier(SerializerSet.class, serializers::get);
		bs.addDefaultSupplier(ParserSet.class, parsers::get);
		bs.addDefaultSupplier(Logger.class, logger::get);
		bs.addDefaultSupplier(java.util.logging.Logger.class, logger::get);
		bs.addDefaultSupplier(ThrownStore.class, thrownStore::get);
		bs.addDefaultSupplier(MethodExecStore.class, methodExecStore::get);
		bs.addDefaultSupplier(Messages.class, messages::get);
		bs.addDefaultSupplier(VarResolver.class, varResolver::get);
		bs.addDefaultSupplier(Config.class, config::get);
		bs.addDefaultSupplier(ResponseProcessor[].class, responseProcessors::get);
		bs.addDefaultSupplier(CallLogger.class, callLogger::get);
		bs.addDefaultSupplier(HttpPartSerializer.class, partSerializer::get);
		bs.addDefaultSupplier(HttpPartParser.class, partParser::get);
		bs.addDefaultSupplier(JsonSchemaGenerator.class, jsonSchemaGenerator::get);
		bs.addDefaultSupplier(StaticFiles.class, staticFiles::get);
		bs.addDefaultSupplier(FileFinder.class, staticFiles::get);
		bs.addDefaultSupplier(DebugEnablement.class, debugEnablement::get);
		bs.addDefaultSupplier(DebugConfig.class, debugConfig::get);
		bs.addDefaultSupplier(SwaggerProvider.class, swaggerProvider::get);
		bs.addDefaultSupplier(OpenApiProvider.class, openApiProvider::get);
		bs.addDefaultSupplier(RestOperations.class, restOperations::get);
		bs.addDefaultSupplier(RestChildren.class, restChildren::get);
		// Named framework beans (replaces DELAYED_INJECTION_NAMES).
		bs.addDefaultSupplier(VarResolver.class, this::getBootstrapVarResolver, PROP_bootstrapVarResolver);
		bs.addDefaultSupplier(HttpHeaderList.class, defaultRequestHeaders::get, PROP_defaultRequestHeaders);
		bs.addDefaultSupplier(HttpHeaderList.class, defaultResponseHeaders::get, PROP_defaultResponseHeaders);
		bs.addDefaultSupplier(NamedAttributeMap.class, defaultRequestAttributes::get, PROP_defaultRequestAttributes);
		bs.addDefaultSupplier(MethodList.class, () -> destroyInvokerPair.get().methods, "destroyMethods");
		bs.addDefaultSupplier(MethodList.class, () -> endCallInvokerPair.get().methods, "endCallMethods");
		bs.addDefaultSupplier(MethodList.class, postCallMethods::get, "postCallMethods");
		bs.addDefaultSupplier(MethodList.class, () -> postInitChildFirstInvokerPair.get().methods, "postInitChildFirstMethods");
		bs.addDefaultSupplier(MethodList.class, () -> postInitInvokerPair.get().methods, "postInitMethods");
		bs.addDefaultSupplier(MethodList.class, preCallMethods::get, "preCallMethods");
		bs.addDefaultSupplier(MethodList.class, () -> startCallInvokerPair.get().methods, "startCallMethods");
		// @formatter:on
	}

	/**
	 * Registers a {@link PropertySource} bean named {@code "rest.config"} in this resource's
	 * {@code BeanStore} so that {@code @Value("${cfg-key}")} fields on the resource bean (and on
	 * request-scoped beans whose {@code BeanStore} parent-walks to this one) resolve against the
	 * resource's {@code @Rest(config=...)} {@link Config}(s).
	 *
	 * <p>
	 * Lookup order honored by the registered source:
	 * <ol>
	 * 	<li>The child class's resolved {@link Config} &mdash; sourced from {@link #rawConfig} so that
	 * 		any {@link Bean @Bean Config} factory-method override on the resource bean is preserved.
	 * 	<li>Any additional, distinct {@code @Rest(config=...)} {@code Config}s declared on parent
	 * 		classes (child-to-parent order, dedup'd by resolved file name). Child keys win on
	 * 		collision; parent keys fill the gaps.
	 * </ol>
	 *
	 * <p>
	 * Critically, this registration lives on the per-resource {@code BeanStore} &mdash; <i>not</i> on
	 * the process-wide {@code Settings.get()} singleton. The static {@code RestContext} cache in
	 * {@code MockRestClient} (and any other long-lived cache) is therefore benign: leaked
	 * {@code RestContext}s carry their {@code PropertySource}s in their own {@code BeanStore}s.
	 * The global {@code Settings} source list does not grow, and {@code Settings.get(name)}'s
	 * reverse-order walk stays O(constant).
	 */
	private void registerRestConfigPropertySources() {
		var sources = collectRestConfigPropertySources();
		if (sources.isEmpty())
			return;
		final List<PropertySource> chain = List.copyOf(sources);
		PropertySource src;
		if (chain.size() == 1) {
			src = chain.get(0);
		} else {
			src = name -> {
				for (var s : chain) {
					var r = s.get(name);
					if (r.isPresent())
						return r;
				}
				return PropertyLookupResult.missing();
			};
		}
		beanStore.addBean(PropertySource.class, src, "rest.config");
	}

	/**
	 * Walks the {@code @Rest(config=...)} annotation chain (child-first) and builds a
	 * {@link ConfigPropertySource} for each distinct, non-empty config attribute.
	 *
	 * <p>
	 * The first (most-derived) slot reuses {@code rawConfig.get()} so that any {@link Bean @Bean}
	 * {@code Config} factory-method override on the resource bean is preserved &mdash; the rest of
	 * the framework already sees that exact instance via {@link #getConfig()} and {@code @RestInit}
	 * parameter resolution. Parent slots load via {@code Config.create().name(...)} since {@code @Bean}
	 * overrides apply to the most-derived class only.
	 *
	 * <p>
	 * Edge case: a resource with no {@code @Rest(config=...)} annotation but with a {@link Bean @Bean}
	 * {@code Config} factory method has a non-null {@code rawConfig.get()} and no annotation-driven
	 * slot. That single {@code rawConfig} is still registered so user-supplied {@code Config}s flow
	 * through to {@code @Value} resolution.
	 */
	@SuppressWarnings({
		"java:S3776", // Cognitive complexity acceptable for multi-source REST config property collection
		"java:S135"   // Per-annotation continue guards are clearer than restructuring the config-source filter chain.
	})
	private List<PropertySource> collectRestConfigPropertySources() {
		var bs = beanStore();
		var vr = bs.getBean(VarResolver.class, PROP_bootstrapVarResolver).orElseGet(this::getBootstrapVarResolver);
		var result = new ArrayList<PropertySource>();
		var seen = new LinkedHashSet<String>();
		// AnnotationProvider returns child-first; iterate in the same order so child wins on collision.
		var anns = AnnotationProvider.INSTANCE.find(Rest.class, info(resourceClass));
		for (var i = 0; i < anns.size(); i++) {
			var raw = anns.get(i).inner().config();
			if (isEmpty(raw))
				continue;
			var resolvedName = vr.resolve(raw);
			if (isEmpty(resolvedName))
				continue;
			if (! seen.add(resolvedName))
				continue;
			Config cfg;
			if (result.isEmpty()) {
				// Most-derived child slot. rawConfig.get() captures @Bean Config override; reuse it.
				cfg = rawConfig.get();
			} else if ("SYSTEM_DEFAULT".equals(resolvedName)) {
				cfg = Config.getSystemDefault();
			} else {
				cfg = Config.create().varResolver(vr).name(resolvedName).build();
			}
			if (cfg != null)
				result.add(new ConfigPropertySource(cfg));
		}
		// Edge case: no @Rest(config=...) annotation produced a slot, but rawConfig.get() is non-null
		// (e.g. a @Bean Config factory method without a matching @Rest(config=...) attribute, or the
		// SYSTEM_DEFAULT branch fired without an annotation). Still register rawConfig as the sole source.
		if (result.isEmpty()) {
			var rc = rawConfig.get();
			if (rc != null)
				result.add(new ConfigPropertySource(rc));
		}
		return result;
	}

	private static final class LifecycleInvokerPair {
		final MethodList methods;
		final MethodInvoker[] invokers;

		LifecycleInvokerPair(MethodList methods, MethodInvoker[] invokers) {
			this.methods = methods;
			this.invokers = invokers;
		}
	}

	/**
	 * The {@link MarshallingContext} for this resource.
	 *
	 * <p>
	 * Triggered eagerly in the constructor after {@code annotationWork} is set, so that annotation
	 * work is applied before the builder is used by any other memoizer.
	 */
	private final Memoizer<MarshallingContext.Builder> beanContextBuilder = memoizer(() -> {
		var v = Holder.of(MarshallingContext.create());
		v.get().apply(annotationWork);
		return v.get();
	});

	/**
	 * The {@link MarshallingContext} for this resource.
	 */
	private final Memoizer<MarshallingContext> marshallingContext = memoizer(() -> beanContextBuilder.get().build());

	/**
	 * The unresolved (bootstrap-time) {@link Config} for this resource.
	 *
	 * <p>
	 * Resolved by reading {@code @Rest(config)} from the annotation chain against the bootstrap
	 * {@link VarResolver} (which must already be in the bean store). Triggered eagerly in the constructor
	 * so the raw config is registered before {@code @RestInit} hooks run.
	 */
	private final Memoizer<Config> rawConfig = memoizer(() -> {
		var bs = beanStore();
		var v = Holder.<Config>empty();
		// Bootstrap VarResolver is registered under PROP_bootstrapVarResolver during construction; the
		// unnamed VarResolver slot is reserved for the full runtime resolver default supplier.
		var vr = bs.getBean(VarResolver.class, PROP_bootstrapVarResolver).orElseGet(this::getBootstrapVarResolver);
		var cfv = Holder.<String>empty();
		rstream(AnnotationProvider.INSTANCE.find(Rest.class, info(resourceClass()))).map(x -> x.inner().config()).filter(Utils::ne).forEach(x -> cfv.set(vr.resolve(x)));
		var cf = cfv.orElse("");
		if (v.isEmpty() && "SYSTEM_DEFAULT".equals(cf))
			v.set(Config.getSystemDefault());
		if (v.isEmpty() && cf.isEmpty() && isMixinContextField() && getParentContext() != null)
			// Mixin sub-contexts with no own @Rest(config) inherit the host's raw Config, so that $C{...}
			// SVL variables embedded in inherited class-level config (e.g. the @HtmlDocConfig theme/header/
			// footer declared via $C{REST/...} on BasicRestConfig) resolve against the host's loaded config
			// rather than an empty one.  This mirrors the way messages and varResolver tokens already inherit
			// from the host for mixin sub-contexts.
			v.set(getParentContext().rawConfig.get());
		if (v.isEmpty()) {
			Config.Builder cb = Config.create().varResolver(vr);
			if (!cf.isEmpty())
				cb.name(cf);
			v.set(cb.build());
		}
		bs.createBeanFromMethod(Config.class, resource().get(), RestContext::isBeanMethod, v.get()).ifPresent(v::set);
		return v.get();
	});

	/**
	 * The bootstrap-time {@link VarResolver} — no {@link Messages} or {@link Config} bean available yet.
	 *
	 * <p>
	 * Builds the same {@link Var} catalog as the runtime resolver but without {@link Messages} or {@link Config}
	 * beans wired in — those depend on settings that are themselves resolved against this resolver.
	 * Triggered eagerly in the constructor so the result is available in the bean store before
	 * {@link Config} and annotation work are initialized.
	 */
	// @formatter:off
	private final Memoizer<VarResolver> bootstrapVarResolver = memoizer(() -> {
		var bs = beanStore();
		Holder<VarResolver> v = Holder.of(
			VarResolver.create()
				.defaultVars()
				.defaultFunctions()
				.vars(VarList.of(
					ConfigVar.class, FileVar.class, LocalizationVar.class,
					RequestAttributeVar.class, RequestFormDataVar.class, RequestHeaderVar.class,
					RequestPathVar.class, RequestQueryVar.class, RequestVar.class,
					RequestSwaggerVar.class, SerializedRequestAttrVar.class, ServletInitParamVar.class,
					SwaggerVar.class, OpenApiVar.class, UrlVar.class, UrlEncodeVar.class, HtmlWidgetVar.class
				).addDefault())
				.bean(FileFinder.class, FileFinder.create(bs).cp(resourceClass(), null, true).build())
				.build()
		);
		bs.createBeanFromMethod(VarResolver.class, resource().get(), x -> isBeanMethod(x, PROP_bootstrapVarResolver)).ifPresent(v::set);
		return v.get();
	});
	// @formatter:on

	/**
	 * The {@link CallLogger} for this resource.
	 *
	 * <p>
	 * Defaults to {@link BasicCallLogger}. {@code @Rest(callLogger=X)} most-derived non-{@code Void} class wins.
	 * A bean-store override or {@code @Bean} factory method REPLACES the result.
	 */
	private final Memoizer<CallLogger> callLogger = memoizer(() -> {
		var bs = beanStore();
		// Test-installed beans (registered via Args.beanStoreConfigurer / addSupplier) live in the
		// bean store's entries deque and are returned by bs.getBean(CallLogger.class) before falling
		// through to this memoizer's default supplier (which is what the @Rest(callLogger) chain
		// produces).  The memoizer therefore only needs to produce the framework default.
		var creator = BeanInstantiator.of(CallLogger.class, bs).type(BasicCallLogger.class).noBuilder();
		bs.getBeanType(CallLogger.class).ifPresent(creator::type);
		// @Rest(callLogger=X) — most-derived non-Void wins.
		// getRestAnnotationsForProperty(...) yields parent-to-child order (rstream reversal); reduce-last
		// keeps the most-derived value, mirroring the prior "apply each annotation, child overrides parent" semantics.
		getRestAnnotationsForProperty(PROPERTY_callLogger)
			.map(ai -> ai.inner().callLogger())
			.filter(c -> c != CallLogger.Void.class)
			.reduce((first, second) -> second)
			.ifPresent(creator::type);
		bs.createBeanFromMethod(CallLogger.class, resource().get(), RestContext::isBeanMethod).ifPresent(creator::impl);
		return creator.asOptional().orElse(null);
	});

	/**
	 * The resolving {@link Config} for this resource.
	 *
	 * <p>
	 * Wraps the builder's unresolved config with a {@link VarResolver} session so that SVL variables
	 * in config values are expanded on access.
	 */
	private final Memoizer<Config> config = memoizer(() -> rawConfig.get().resolving(getVarResolver().createSession()));

	/**
	 * The supported request content types ({@code Content-Type} media types) for this resource.
	 *
	 * <p>
	 * Walks {@code @Rest(consumes)} annotations parent-to-child and returns the union. When no annotation
	 * declares types, falls back to the intersection of all op-level parser supported media types.
	 */
	private final Memoizer<List<MediaType>> consumes = memoizer(() -> {
		// Walk @Rest(consumes=...) chain (parent-to-child); concat all entries in chain order.
		var fromAnnotations = new ArrayList<MediaType>();
		getRestAnnotationsForProperty(PROPERTY_consumes)
			.forEach(ai -> stream(ai.inner().consumes()).map(MediaType::of).forEach(fromAnnotations::add));
		if (! fromAnnotations.isEmpty())
			return u(fromAnnotations);
		// Fall back to the intersection of opContexts' parser-supported media types — matches legacy behavior.
		var opContexts = restOperations().getOpContexts();
		Set<MediaType> s = opContexts.isEmpty() ? emptySet() : toSet(opContexts.get(0).getParsers().getSupportedMediaTypes());
		opContexts.forEach(x -> s.retainAll(x.getParsers().getSupportedMediaTypes()));
		return u(toList(s));
	});

	//---------------------------------------------------------------------------------------------
	// @Value-injected env-driven defaults
	//
	// Populated by ClassInfo.of(this).inject(this, beanStore) during construction; consumed by the
	// memoizer lambdas below (which run lazily, i.e. after injection has completed). Each field
	// is the env-driven default that flows into mergeReplacedStringAttribute / mergeReplacedBooleanAttribute
	// as the initial value before the @Rest annotation chain is walked.
	//---------------------------------------------------------------------------------------------

	/** Env-driven default for {@code @Rest(debugDefault)}; consumed by the {@link #debugEnablement} memoizer. */
	@Value("${RestContext.debugDefault:}")
	private String defaultDebugDefault;

	/** Env-driven default for the call-logger debug level fallback in the {@link #debugConfig} memoizer. */
	@Value("${juneau.restLogger.level:INFO}")
	private String defaultDebugLevel;

	/** Env-driven default for {@code @Rest(allowedHeaderParams)}. */
	@Value("${RestContext.allowedHeaderParams:Accept,Content-Type}")
	private String defaultAllowedHeaderParams;

	/** Env-driven default for {@code @Rest(allowedMethodHeaders)}. */
	@Value("${RestContext.allowedMethodHeaders:}")
	private String defaultAllowedMethodHeaders;

	/** Env-driven default for {@code @Rest(allowedMethodParams)}. */
	@Value("${RestContext.allowedMethodParams:HEAD,OPTIONS}")
	private String defaultAllowedMethodParams;

	/** Env-driven default for {@code @Rest(disableContentParam)}. */
	@Value("${RestContext.disableContentParam:false}")
	private boolean defaultDisableContentParam;

	/** Env-driven default for {@code @Rest(renderResponseStackTraces)}. */
	@Value("${RestContext.renderResponseStackTraces:false}")
	private boolean defaultRenderResponseStackTraces;

	/** Env-driven default for {@code @Rest(problemDetails)}. */
	@Value("${RestContext.problemDetails:false}")
	private boolean defaultProblemDetails;

	/** Env-driven default for {@code @Rest(virtualThreads)}. */
	@Value("${RestContext.virtualThreads:false}")
	private boolean defaultVirtualThreads;

	/**
	 * Env-driven default controlling whether the {@link TraceContextResponseProcessor} is registered so the
	 * server writes W3C {@code traceparent} / {@code tracestate} response headers when a {@link TracerHook} is
	 * active. Defaults to {@code true} (on-when-tracer); set {@code RestContext.responseTraceparent=false}
	 * to keep the processor out of the chain entirely.
	 */
	@Value("${RestContext.responseTraceparent:true}")
	private boolean defaultResponseTraceparent;

	/**
	 * Env-driven default controlling whether {@link org.apache.juneau.rest.server.processor.MdcAsyncListener} propagates
	 * the request thread's SLF4J MDC map to {@link java.util.concurrent.CompletableFuture} completion threads
	 * Defaults to {@code true} (on when SLF4J is detected). Set
	 * {@code RestContext.mdcAsyncPropagation=false} to disable globally, or call
	 * {@link Builder#mdcAsyncPropagation(boolean)} per resource.
	 */
	@Value("${RestContext.mdcAsyncPropagation:true}")
	private boolean defaultMdcAsyncPropagation;

	/** Env-driven default for {@code @Rest(eagerInit)}. */
	@Value("${RestContext.eagerInit:false}")
	private boolean defaultEagerInit;

	/** Env-driven default for {@code @Rest(lazyChildren)}: deferred child construction opt-in. */
	@Value("${RestContext.lazyChildren:false}")
	private boolean defaultLazyChildren;

	/** Env-driven default for {@code @Rest(clientVersionHeader)}. */
	@Value("${RestContext.clientVersionHeader:Client-Version}")
	private String defaultClientVersionHeader;

	/** Env-driven default for {@code @Rest(uriRelativity)}; resolves to empty string when unset (treated as "no default"). */
	@Value("${RestContext.uriRelativity:}")
	private String defaultUriRelativity;

	/** Env-driven default for {@code @Rest(uriAuthority)}; {@link Optional#empty()} when unset (preserves null-vs-empty distinction). */
	@Value("${RestContext.uriAuthority}")
	private Optional<String> defaultUriAuthority;

	/** Env-driven default for {@code @Rest(uriContext)}; {@link Optional#empty()} when unset (preserves null-vs-empty distinction). */
	@Value("${RestContext.uriContext}")
	private Optional<String> defaultUriContext;

	/** Env-driven default for {@code @Rest(uriResolution)}; resolves to empty string when unset (treated as "no default"). */
	@Value("${RestContext.uriResolution:}")
	private String defaultUriResolution;

	/**
	 * The {@link DebugEnablement} for this resource.
	 *
	 * <p>
	 * Resolved from {@code @Rest(debugDefault)} (most-derived non-blank wins), falling back to
	 * {@code @Rest(debug=true|false)}. The resolved {@link Enablement} is published into the bean store
	 * so that {@link BasicDebugEnablement} subclasses can pick it up. Defaults to {@link BasicDebugEnablement}.
	 * A bean-store override or {@code @Bean} factory method REPLACES the result.
	 */
	private final Memoizer<DebugEnablement> debugEnablement = memoizer(() -> {
		// @Rest(debugDefault="ALWAYS|NEVER|CONDITIONAL") — most-derived non-blank value wins, with parent inheritance
		// gated by @Rest(noInherit={"debugDefault"}). Resolved value is published as an Enablement bean so that
		// BasicDebugEnablement.init() (and any subclass) can pick it up via beanStore().getBean(Enablement.class).
		// Annotation value overrides any pre-registered Enablement bean (e.g. mock-client default).
		// If neither a debugDefault annotation value NOR a pre-registered Enablement bean is present, fall back
		// to the @Rest(debug=true|false) boolean flag — ALWAYS when set, NEVER otherwise.
		var bs = beanStore();
		String debugDefaultStr = mergeReplacedStringAttribute(PROPERTY_debugDefault, defaultDebugDefault);
		Enablement resolvedDebugDefault = null;
		if (nn(debugDefaultStr) && !debugDefaultStr.isBlank())
			resolvedDebugDefault = Enablement.fromString(debugDefaultStr);
		if (nn(resolvedDebugDefault))
			bs.addBean(Enablement.class, resolvedDebugDefault);
		else if (bs.getBean(Enablement.class).isEmpty())
			bs.addBean(Enablement.class, isDebug() ? Enablement.ALWAYS : Enablement.NEVER);
		var creator = BeanInstantiator.of(DebugEnablement.class, bs).type(BasicDebugEnablement.class).noBuilder();
		bs.getBeanType(DebugEnablement.class).ifPresent(creator::type);
		bs.createBeanFromMethod(DebugEnablement.class, resource().get(), RestContext::isBeanMethod).ifPresent(creator::impl);
		return creator.asOptional().orElse(null);
	});

	/**
	 * The {@link DebugConfig} for this resource.
	 */
	private final Memoizer<DebugConfig> debugConfig = memoizer(() -> {
		var bs = beanStore();
		var mode = getRestAnnotationsForProperty(PROPERTY_debug)
			.map(ai -> resolve(ai.inner().debug().value()))
			.filter(StringUtils::isNotBlank)
			.reduce((first, second) -> second)
			.orElse("");
		var formatType = getRestAnnotationsForProperty(PROPERTY_debug)
			.map(ai -> ai.inner().debug().format())
			.filter(c -> c != DebugFormat.Void.class)
			.reduce((first, second) -> second)
			.orElse(null);
		var levelStr = getRestAnnotationsForProperty(PROPERTY_debug)
			.map(ai -> resolve(ai.inner().debug().level()))
			.filter(StringUtils::isNotBlank)
			.reduce((first, second) -> second)
			.orElse("");
		var format = formatType == null ? new BasicTextFormat() : BeanInstantiator.of(DebugFormat.class, bs).type(formatType).run();
		var level = StringUtils.isNotBlank(levelStr) ? Level.parse(levelStr) : Level.parse(defaultDebugLevel);
		var mode2 = mode;
		return new DebugConfig(bs) {
			@Override
			public DebugResult resolve(RestContext context, HttpServletRequest req) {
				var enabled = isTrue(cast(Boolean.class, req.getAttribute("Debug")));
				if (!enabled) {
					if ("always".equalsIgnoreCase(mode2) || "true".equalsIgnoreCase(mode2))
						enabled = true;
					else if ("conditional".equalsIgnoreCase(mode2))
						enabled = "true".equalsIgnoreCase(req.getHeader("Debug"));
				}
				var cacheBodies = enabled;
				return new DebugResult(enabled, format, level, cacheBodies);
			}

			@Override
			public DebugResult resolve(RestOpContext context, HttpServletRequest req) {
				var opDebug = AnnotationProvider.INSTANCE.find(RestOp.class, MethodInfo.of(context.getJavaMethod())).stream().findFirst();
				if (opDebug.isPresent()) {
					var v = RestContext.this.resolve(opDebug.get().inner().debug().value());
					if (StringUtils.isNotBlank(v)) {
						if ("always".equalsIgnoreCase(v) || "true".equalsIgnoreCase(v))
							return new DebugResult(true, format, level, true);
						if ("never".equalsIgnoreCase(v) || "false".equalsIgnoreCase(v))
							return new DebugResult(false, format, level, false);
						if ("conditional".equalsIgnoreCase(v))
							return new DebugResult("true".equalsIgnoreCase(req.getHeader("Debug")), format, level, true);
					}
				}
				return resolve(context.getContext(), req);
			}
		};
	});

	/**
	 * The default request attributes contributed by {@code @Rest(defaultRequestAttributes)} for this resource.
	 *
	 * <p>
	 * Walks {@code @Rest} annotations parent-to-child, resolving each attribute string and parsing it as a
	 * key=value or key:value pair. A named bean-store override or {@code @Bean} factory method REPLACES
	 * the accumulated result.
	 */
	private final Memoizer<NamedAttributeMap> defaultRequestAttributes = memoizer(() -> {
		var v = Holder.of(NamedAttributeMap.create());
		getRestAnnotationsTopDown().forEach(ai -> Arrays.stream(ai.inner().defaultRequestAttributes())
			.filter(StringUtils::isNotBlank)
			.map(this::resolve)
			.filter(StringUtils::isNotBlank)
			.map(BasicNamedAttribute::ofPair)
			.forEach(v.get()::add));
		beanStore().createBeanFromMethod(NamedAttributeMap.class, resource().get(), x -> isBeanMethod(x, PROP_defaultRequestAttributes), v.get()).ifPresent(v::set);
		return v.get();
	});

	/**
	 * The default request headers contributed by {@code @Rest(defaultRequestHeaders)},
	 * {@code @Rest(defaultAccept)}, and {@code @Rest(defaultContentType)} for this resource.
	 *
	 * <p>
	 * Walks {@code @Rest} annotations parent-to-child, resolving each header string. A named bean-store
	 * override or {@code @Bean} factory method REPLACES the accumulated result.
	 */
	private final Memoizer<HttpHeaderList> defaultRequestHeaders = memoizer(() -> {
		var v = Holder.of(HttpHeaderList.create());
		getRestAnnotationsTopDown().forEach(ai -> {
			Rest a = ai.inner();
			Arrays.stream(a.defaultRequestHeaders()).filter(StringUtils::isNotBlank).map(this::resolve).filter(StringUtils::isNotBlank).map(HttpStringHeader::ofPair).forEach(v.get()::setDefault);
			var defaultAccept = resolve(a.defaultAccept());
			if (isNotBlank(defaultAccept))
				v.get().setDefault(Accept.of(defaultAccept));
			var defaultContentType = resolve(a.defaultContentType());
			if (isNotBlank(defaultContentType))
				v.get().setDefault(ContentType.of(defaultContentType));
		});
		beanStore().createBeanFromMethod(HttpHeaderList.class, resource().get(), x -> isBeanMethod(x, PROP_defaultRequestHeaders), v.get()).ifPresent(v::set);
		return v.get();
	});

	/**
	 * The default response headers contributed by {@code @Rest(defaultResponseHeaders)} for this resource.
	 *
	 * <p>
	 * Walks {@code @Rest} annotations parent-to-child, resolving each header string. A named bean-store
	 * override or {@code @Bean} factory method REPLACES the accumulated result.
	 */
	private final Memoizer<HttpHeaderList> defaultResponseHeaders = memoizer(() -> {
		var v = Holder.of(HttpHeaderList.create());
		getRestAnnotationsTopDown().forEach(ai -> Arrays.stream(ai.inner().defaultResponseHeaders()).filter(StringUtils::isNotBlank).map(this::resolve).filter(StringUtils::isNotBlank).map(HttpStringHeader::ofPair).forEach(v.get()::setDefault));
		beanStore().createBeanFromMethod(HttpHeaderList.class, resource().get(), x -> isBeanMethod(x, PROP_defaultResponseHeaders), v.get()).ifPresent(v::set);
		return v.get();
	});

	/**
	 * Methods annotated with {@link org.apache.juneau.rest.server.RestDestroy @RestDestroy} and their invokers.
	 */
	private final Memoizer<LifecycleInvokerPair> destroyInvokerPair = memoizer(() -> buildLifecycleInvokerPair(() -> {
		var bs = beanStore();
		var v = Holder.of(MethodList.of(getAnnotatedMethods(resource(), RestDestroy.class).toList()));
		bs.createBeanFromMethod(MethodList.class, resource().get(), x -> isBeanMethod(x, "destroyMethods"), v.get()).ifPresent(v::set);
		return v.get();
	}));

	/**
	 * Fully-configured {@link EncoderSet.Builder} for this resource, populated from the
	 * {@code @Rest(encoders)} annotation chain and any {@code @Bean} override.
	 *
	 * <p>
	 * Starts with {@link IdentityEncoder} as the implicit default. A bean-store type override
	 * or bean-store instance override REPLACES the builder or its impl. Annotation entries
	 * (parent-to-child) are appended after the default. A {@code @Bean} factory method
	 * REPLACES the impl with the returned value.
	 */
	private final Memoizer<EncoderSet.Builder> encodersBuilder = memoizer(() -> {
		var bs = beanStore();
		var v = Holder.of(EncoderSet.create(bs));
		getRestAnnotationsForProperty(PROPERTY_encoders).forEach(ai -> v.get().add(ai.inner().encoders()));
		bs.createBeanFromMethod(EncoderSet.class, resource().get(), RestContext::isBeanMethod, v.get()).ifPresent(x -> v.get().impl(x));
		return v.get();
	});

	/**
	 * The {@link EncoderSet} for this resource, built from the self-contained encoder builder memoizer.
	 */
	private final Memoizer<EncoderSet> encoders = memoizer(() -> encodersBuilder.get().build());

	/**
	 * Methods annotated with {@link org.apache.juneau.rest.server.RestEndCall @RestEndCall} and their invokers.
	 */
	private final Memoizer<LifecycleInvokerPair> endCallInvokerPair = memoizer(() -> buildLifecycleInvokerPair(() -> {
		var bs = beanStore();
		var v = Holder.of(MethodList.of(getAnnotatedMethods(resource(), RestEndCall.class).toList()));
		bs.createBeanFromMethod(MethodList.class, resource().get(), x -> isBeanMethod(x, "endCallMethods"), v.get()).ifPresent(v::set);
		return v.get();
	}));

	/**
	 * The {@link JsonSchemaGenerator.Builder} for this resource, with annotation work applied.
	 */
	private final Memoizer<JsonSchemaGenerator.Builder> jsonSchemaGeneratorBuilder = memoizer(() -> {
		var bs = beanStore();
		var v = Holder.of(JsonSchemaGenerator.create());
		bs.createBeanFromMethod(JsonSchemaGenerator.class, resource().get(), RestContext::isBeanMethod).ifPresent(x -> v.get().impl(x));
		v.get().apply(annotationWork);
		return v.get();
	});

	/**
	 * The {@link JsonSchemaGenerator} for this resource.
	 */
	private final Memoizer<JsonSchemaGenerator> jsonSchemaGenerator = memoizer(() -> jsonSchemaGeneratorBuilder.get().build());

	/**
	 * The {@link Logger} for this resource.
	 *
	 * <p>
	 * Defaults to {@code Logger.getLogger(resourceClass.getName())}. A bean-store override or
	 * {@code @Bean} factory method REPLACES the default.
	 */
	private final Memoizer<Logger> logger = memoizer(() -> {
		var v = Holder.of(Logger.getLogger(cn(resourceClass())));
		beanStore().createBeanFromMethod(Logger.class, resource().get(), RestContext::isBeanMethod, v.get()).ifPresent(v::set);
		return v.get();
	});

	/**
	 * The {@link Messages} bundle for this resource.
	 *
	 * <p>
	 * Walks {@code @Rest} annotations parent-to-child, resolving each {@code messages} location string
	 * against the bootstrap resolver (full resolver not yet available — it depends on {@code getMessages()}).
	 * A bean-store override or {@code @Bean} factory method REPLACES the result.
	 */
	private final Memoizer<Messages> messages = memoizer(() -> {
		var b = Messages.create(resourceClass());
		// Walk @Rest annotations parent-to-child; child wins because location() prepends.
		// Resolve location strings against the simple resolver — full resolver isn't available yet
		// (it depends on getMessages()).
		var vrs = getBootstrapVarResolver().createSession();
		getRestAnnotationsTopDown().forEach(ai -> ai.getString(PROPERTY_messages).filter(StringUtils::isNotBlank).ifPresent(s -> b.location(vrs.resolve(s))));
		var override = beanStore().createBeanFromMethod(Messages.class, resource().get(), RestContext::isBeanMethod, b).orElse(null);
		var local = nn(override) ? override : b.build();
		var parent = parentContext();
		if (isMixinContextField() && nn(parent) && !isNoInheritLiteral(PROPERTY_messages))
			return Messages.chain(local, parent.getMessages());
		return local;
	});

	/**
	 * The {@link MethodExecStore} for this resource, wired to the {@link ThrownStore}.
	 *
	 * <p>
	 * A bean-store override or {@code @Bean} factory method REPLACES the result.
	 */
	private final Memoizer<MethodExecStore> methodExecStore = memoizer(() -> {
		var bs = beanStore();
		var b = MethodExecStore.create(bs).thrownStoreOnce(getThrownStore());
		bs.createBeanFromMethod(MethodExecStore.class, resource().get(), RestContext::isBeanMethod, b).ifPresent(b::impl);
		return b.build();
	});

	/**
	 * Fully-configured {@link ParserSet.Builder} for this resource, populated from the
	 * {@code @Rest(parsers)} annotation chain and any {@code @Bean} override.
	 *
	 * <p>
	 * Starts with an empty set. A bean-store type or instance override REPLACES the builder or its impl.
	 * Annotation entries (parent-to-child) are appended. A {@code @Bean} factory method REPLACES the impl.
	 */
	private final Memoizer<ParserSet.Builder> parsersBuilder = memoizer(() -> {
		var bs = beanStore();
		var v = Holder.of(ParserSet.create(bs));
		getRestAnnotationsForProperty(PROPERTY_parsers).forEach(ai -> v.get().add(ai.inner().parsers()));
		bs.createBeanFromMethod(ParserSet.class, resource().get(), RestContext::isBeanMethod, v.get()).ifPresent(x -> v.get().impl(x));
		return v.get();
	});

	/**
	 * The {@link ParserSet} for this resource, built from the self-contained parser builder memoizer.
	 */
	private final Memoizer<ParserSet> parsers = memoizer(() -> parsersBuilder.get().build());

	/**
	 * The {@link HttpPartParser.Creator} for this resource, with annotation work applied.
	 */
	private final Memoizer<HttpPartParser.Creator> partParserCreator = memoizer(() -> {
		var bs = beanStore();
		Holder<HttpPartParser.Creator> v = Holder.of(HttpPartParser.creator().type(OpenApiParser.class));
		opt(resource().get() instanceof HttpPartParser x ? x : null).ifPresent(x -> v.get().impl(x));
		bs.createBeanFromMethod(HttpPartParser.class, resource().get(), RestContext::isBeanMethod).ifPresent(x -> v.get().impl(x));
		v.get().apply(annotationWork);
		return v.get();
	});

	/**
	 * The {@link HttpPartParser} for this resource.
	 *
	 * <p>
	 * Starts from the creator (general config annotations already applied) and adds the
	 * {@code @Rest(partParser)} type override from the most specific annotation in the hierarchy.
	 */
	private final Memoizer<HttpPartParser> partParser = memoizer(() -> {
		var creator = partParserCreator.get();
		getRestAnnotationsForProperty(PROPERTY_partParser)
			.map(ai -> ai.inner().partParser())
			.filter(ClassUtils::isNotVoid)
			.reduce((a, b) -> b)
			.ifPresent(creator::type);
		return creator.create();
	});

	/**
	 * The {@link HttpPartSerializer.Creator} for this resource, with annotation work applied.
	 */
	private final Memoizer<HttpPartSerializer.Creator> partSerializerCreator = memoizer(() -> {
		var bs = beanStore();
		Holder<HttpPartSerializer.Creator> v = Holder.of(HttpPartSerializer.creator().type(OpenApiSerializer.class));
		opt(resource().get() instanceof HttpPartSerializer x ? x : null).ifPresent(x -> v.get().impl(x));
		bs.createBeanFromMethod(HttpPartSerializer.class, resource().get(), RestContext::isBeanMethod).ifPresent(x -> v.get().impl(x));
		v.get().apply(annotationWork);
		return v.get();
	});

	/**
	 * The {@link HttpPartSerializer} for this resource.
	 *
	 * <p>
	 * Starts from the creator (general config annotations already applied) and adds the
	 * {@code @Rest(partSerializer)} type override from the most specific annotation in the hierarchy.
	 */
	private final Memoizer<HttpPartSerializer> partSerializer = memoizer(() -> {
		var creator = partSerializerCreator.get();
		getRestAnnotationsForProperty(PROPERTY_partSerializer)
			.map(ai -> ai.inner().partSerializer())
			.filter(ClassUtils::isNotVoid)
			.reduce((a, b) -> b)
			.ifPresent(creator::type);
		return creator.create();
	});

	/**
	 * Methods annotated with {@link org.apache.juneau.rest.server.RestPostCall @RestPostCall}.
	 */
	private final Memoizer<MethodList> postCallMethods = memoizer(() -> {
		var bs = beanStore();
		var v = Holder.of(MethodList.of(getAnnotatedMethods(resource(), RestPostCall.class).toList()));
		bs.createBeanFromMethod(MethodList.class, resource().get(), x -> isBeanMethod(x, "postCallMethods"), v.get()).ifPresent(v::set);
		return v.get();
	});

	/**
	 * Pre-resolved invokers for this context's local {@code @RestPostCall} methods, bound to this
	 * context's resource instance and bean-store-resolved arg resolvers. Used by the {@link #postCall(RestOpSession)}
	 * parent-chain walk so that host hooks fire before mixin hooks on mixin-endpoint requests.
	 *
	 * <p>
	 * Hook methods aren't op-specific, so the wrapping bean store stubs in the op-only {@link UrlPathMatcher}
	 * binding with {@code null} so that {@link org.apache.juneau.rest.server.arg.PathArg#create PathArg.create()} (and
	 * other op-aware resolvers) can be called by {@link #findRestOperationArgs(java.lang.reflect.Method, BeanStore)}
	 * and abstain (return {@code null}) for parameters that aren't {@code @Path}-annotated. Using
	 * {@code @Path} on a {@code @RestPostCall} method is not supported.
	 */
	private final Memoizer<RestOpInvoker[]> localPostCallInvokers = memoizer(() -> {
		var bs = new BasicBeanStore(beanStore());
		var pm = getPathMatcher() != null ? getPathMatcher() : UrlPathMatcher.of("");
		bs.addBean(UrlPathMatcher.class, pm);
		bs.addBean(UrlPathMatcher[].class, new UrlPathMatcher[]{pm});
		return postCallMethods.get().stream()
			.map(x -> new RestOpInvoker(x, findRestOperationArgs(x, bs), getMethodExecStats(x), this::getResource))
			.toArray(RestOpInvoker[]::new);
	});

	/**
	 * Methods annotated with {@link org.apache.juneau.rest.server.RestPostInit @RestPostInit}{@code (childFirst=true)} and their invokers.
	 */
	private final Memoizer<LifecycleInvokerPair> postInitChildFirstInvokerPair = memoizer(() -> buildLifecycleInvokerPair(() -> {
		var bs = beanStore();
		var v = Holder.of(MethodList.of(getAnnotatedMethods(resource(), RestPostInit.class)
			.filter(m -> rstream(AnnotationProvider.INSTANCE.find(RestPostInit.class, MethodInfo.of(m))).map(AnnotationInfo::inner).anyMatch(RestPostInit::childFirst))
			.toList()));
		bs.createBeanFromMethod(MethodList.class, resource().get(), x -> isBeanMethod(x, "postInitChildFirstMethods"), v.get()).ifPresent(v::set);
		return v.get();
	}));

	/**
	 * Methods annotated with {@link org.apache.juneau.rest.server.RestPostInit @RestPostInit} and their invokers.
	 */
	private final Memoizer<LifecycleInvokerPair> postInitInvokerPair = memoizer(() -> buildLifecycleInvokerPair(() -> {
		var bs = beanStore();
		var v = Holder.of(MethodList.of(getAnnotatedMethods(resource(), RestPostInit.class)
			.filter(m -> rstream(AnnotationProvider.INSTANCE.find(RestPostInit.class, MethodInfo.of(m))).map(AnnotationInfo::inner).anyMatch(x -> !x.childFirst()))
			.toList()));
		bs.createBeanFromMethod(MethodList.class, resource().get(), x -> isBeanMethod(x, "postInitMethods"), v.get()).ifPresent(v::set);
		return v.get();
	}));

	/**
	 * Methods annotated with {@link org.apache.juneau.rest.server.RestPreCall @RestPreCall}.
	 */
	private final Memoizer<MethodList> preCallMethods = memoizer(() -> {
		var bs = beanStore();
		var v = Holder.of(MethodList.of(getAnnotatedMethods(resource(), RestPreCall.class).toList()));
		bs.createBeanFromMethod(MethodList.class, resource().get(), x -> isBeanMethod(x, "preCallMethods"), v.get()).ifPresent(v::set);
		return v.get();
	});

	/**
	 * Pre-resolved invokers for this context's local {@code @RestPreCall} methods, bound to this
	 * context's resource instance and bean-store-resolved arg resolvers. Used by the {@link #preCall(RestOpSession)}
	 * parent-chain walk so that host hooks fire before mixin hooks on mixin-endpoint requests.
	 *
	 * <p>
	 * Hook methods aren't op-specific, so the wrapping bean store stubs in the op-only {@link UrlPathMatcher}
	 * binding with {@code null} so that {@link org.apache.juneau.rest.server.arg.PathArg#create PathArg.create()} (and
	 * other op-aware resolvers) can be called by {@link #findRestOperationArgs(java.lang.reflect.Method, BeanStore)}
	 * and abstain (return {@code null}) for parameters that aren't {@code @Path}-annotated. Using
	 * {@code @Path} on a {@code @RestPreCall} method is not supported.
	 */
	private final Memoizer<RestOpInvoker[]> localPreCallInvokers = memoizer(() -> {
		var bs = new BasicBeanStore(beanStore());
		var pm = getPathMatcher() != null ? getPathMatcher() : UrlPathMatcher.of("");
		bs.addBean(UrlPathMatcher.class, pm);
		bs.addBean(UrlPathMatcher[].class, new UrlPathMatcher[]{pm});
		return preCallMethods.get().stream()
			.map(x -> new RestOpInvoker(x, findRestOperationArgs(x, bs), getMethodExecStats(x), this::getResource))
			.toArray(RestOpInvoker[]::new);
	});

	/**
	 * The supported response content types ({@code Accept} media types) for this resource.
	 *
	 * <p>
	 * Walks {@code @Rest(produces)} annotations parent-to-child and returns the union. When no annotation
	 * declares types, falls back to the intersection of all op-level serializer supported media types.
	 */
	private final Memoizer<List<MediaType>> produces = memoizer(() -> {
		// Walk @Rest(produces=...) chain (parent-to-child); concat all entries in chain order.
		var fromAnnotations = new ArrayList<MediaType>();
		getRestAnnotationsForProperty(PROPERTY_produces)
			.forEach(ai -> stream(ai.inner().produces()).map(MediaType::of).forEach(fromAnnotations::add));
		if (! fromAnnotations.isEmpty())
			return u(fromAnnotations);
		// Fall back to the intersection of opContexts' serializer-supported media types — matches legacy behavior.
		var opContexts = restOperations().getOpContexts();
		Set<MediaType> s = opContexts.isEmpty() ? emptySet() : toSet(opContexts.get(0).getSerializers().getSupportedMediaTypes());
		opContexts.forEach(x -> s.retainAll(x.getSerializers().getSupportedMediaTypes()));
		return u(toList(s));
	});

	/**
	 * JVM-wide cache of {@link ResponseProcessor} provider classes discovered via {@link ServiceLoader}.
	 *
	 * <p>
	 * Populated lazily on first use and shared across all {@link RestContext} instances. Empty on a bare
	 * {@code juneau-rest-server} classpath; non-empty only when an opt-in module (e.g.
	 * {@code juneau-rest-server-reactive}) ships a
	 * {@code META-INF/services/org.apache.juneau.rest.server.processor.ResponseProcessor} provider file.
	 */
	@SuppressWarnings({
		"java:S3077" // volatile is required for correct double-checked-locking safe-publication of this JVM-wide cache; the reference is publish-once and never compound-mutated.
	})
	private static volatile List<Class<? extends ResponseProcessor>> serviceLoaderResponseProcessors;

	/**
	 * Returns the module-contributed {@link ResponseProcessor} classes discovered via {@link ServiceLoader},
	 * caching the result for the lifetime of the JVM.
	 *
	 * <p>
	 * Provider <em>types</em> are resolved (not instantiated) so each {@link RestContext} can instantiate its
	 * own bean-store-injected copy. Discovery failures are swallowed at {@code FINE} so a malformed provider
	 * never fails resource startup; on a bare classpath the returned list is empty and the default processor
	 * chain is unchanged.
	 *
	 * @return The discovered processor classes (possibly empty, never {@code null}).
	 */
	private static List<Class<? extends ResponseProcessor>> discoverServiceLoaderResponseProcessors() {
		var p = serviceLoaderResponseProcessors;
		if (p == null) {
			synchronized (RestContext.class) {
				p = serviceLoaderResponseProcessors;
				if (p == null) {
					p = loadServiceLoaderResponseProcessors();
					serviceLoaderResponseProcessors = p;
				}
			}
		}
		return p;
	}

	private static List<Class<? extends ResponseProcessor>> loadServiceLoaderResponseProcessors() {
		var out = new ArrayList<Class<? extends ResponseProcessor>>();
		try {
			for (var it = ServiceLoader.load(ResponseProcessor.class).stream().iterator(); it.hasNext();)
				addNextServiceLoaderResponseProcessor(out, it);
		} catch (ServiceConfigurationError | RuntimeException e) {
			LOG.log(Level.FINE, e, () -> "ServiceLoader for ResponseProcessor failed: " + e.getMessage());
		}
		return List.copyOf(out);
	}

	private static void addNextServiceLoaderResponseProcessor(List<Class<? extends ResponseProcessor>> out, Iterator<ServiceLoader.Provider<ResponseProcessor>> it) {
		try {
			out.add(it.next().type());
		} catch (ServiceConfigurationError | RuntimeException e) {
			LOG.log(Level.FINE, e, () -> "Skipping ServiceLoader-discovered ResponseProcessor: " + e.getMessage());
		}
	}

	/**
	 * The ordered array of {@link ResponseProcessor} instances for this resource.
	 *
	 * <p>
	 * Walks {@code @Rest(responseProcessors)} annotations parent-to-child (append order). A bean-store
	 * override or {@code @Bean} factory method REPLACES the entire list.
	 */
	private final Memoizer<ResponseProcessor[]> responseProcessors = memoizer(() -> {
		// Walk @Rest(responseProcessors=...) chain (parent-to-child via getRestAnnotationsForProperty).
		// DefaultConfig contributes the framework defaults at the top of the chain; resource-class entries append.
		// ResponseProcessorList.Builder.add(...) uses addAll (append) — final order: [DefaultConfig, parent, child].
		var bs = beanStore();
		var b = ResponseProcessorList.create(bs);
		// When enabled (default on-when-tracer), front-load the W3C trace-context processor so it
		// writes traceparent/tracestate headers before the body-rendering processors run. It short-circuits
		// at request time when no TracerHook stashed a trace context, so it's zero-cost on the no-tracer path.
		if (defaultResponseTraceparent)
			b.add(TraceContextResponseProcessor.class);
		// Front-load any module-contributed ResponseProcessors discovered via
		// ServiceLoader (e.g. the opt-in juneau-rest-server-reactive module's ReactiveResponseProcessor,
		// which must run ahead of AsyncResponseProcessor). Inert on a bare juneau-rest-server classpath —
		// when no module ships a META-INF/services/...ResponseProcessor provider file the list is empty and
		// the chain is identical to the pre-feature default.
		discoverServiceLoaderResponseProcessors().forEach(b::add);
		getRestAnnotationsForProperty(PROPERTY_responseProcessors)
			.forEach(ai -> b.add(ai.inner().responseProcessors()));
		// @Bean method override REPLACES the entire annotation-derived list.
		var override = bs.createBeanFromMethod(ResponseProcessorList.class, resource().get(), RestContext::isBeanMethod, b).orElse(null);
		return (nn(override) ? override : b.build()).toArray();
	});

	/**
	 * The ordered array of {@link RestOpArg} argument-resolver types for this resource.
	 *
	 * <p>
	 * Walks {@code @Rest(restOpArgs)} annotations parent-to-child (prepend order, so child entries
	 * take priority). A bean-store override or {@code @Bean} factory method REPLACES the entire list.
	 */
	private final Memoizer<Class<? extends RestOpArg>[]> restOpArgs = memoizer(() -> {
		// Walk @Rest(restOpArgs=...) chain (parent-to-child via getRestAnnotationsForProperty).
		// DefaultConfig contributes the framework arg-resolver list; resource-class entries override.
		// RestOpArgList.Builder.add(...) uses prependAll — applying per-annotation in chain order yields
		// final order: [child, parent, DefaultConfig], matching the legacy apply-pass behavior.
		var bs = beanStore();
		var b = RestOpArgList.create(bs);
		getRestAnnotationsForProperty(PROPERTY_restOpArgs)
			.forEach(ai -> b.add(ai.inner().restOpArgs()));
		// @Bean method override REPLACES the entire annotation-derived list.
		var override = bs.createBeanFromMethod(RestOpArgList.class, resource().get(), RestContext::isBeanMethod, b).orElse(null);
		return (nn(override) ? override : b.build()).asArray();
	});

	/**
	 * Fully-configured {@link SerializerSet.Builder} for this resource, populated from the
	 * {@code @Rest(serializers)} annotation chain and any {@code @Bean} override.
	 *
	 * <p>
	 * Starts with an empty set. A bean-store type or instance override REPLACES the builder or its impl.
	 * Annotation entries (parent-to-child) are appended. A {@code @Bean} factory method REPLACES the impl.
	 */
	private final Memoizer<SerializerSet.Builder> serializersBuilder = memoizer(() -> {
		var bs = beanStore();
		var v = Holder.of(SerializerSet.create(bs));
		getRestAnnotationsForProperty(PROPERTY_serializers).forEach(ai -> v.get().add(ai.inner().serializers()));
		bs.createBeanFromMethod(SerializerSet.class, resource().get(), RestContext::isBeanMethod, v.get()).ifPresent(x -> v.get().impl(x));
		return v.get();
	});

	/**
	 * The {@link SerializerSet} for this resource, built from the self-contained serializer builder memoizer.
	 */
	private final Memoizer<SerializerSet> serializers = memoizer(() -> serializersBuilder.get().build());

	/**
	 * Methods annotated with {@link org.apache.juneau.rest.server.RestStartCall @RestStartCall} and their invokers.
	 */
	private final Memoizer<LifecycleInvokerPair> startCallInvokerPair = memoizer(() -> buildLifecycleInvokerPair(() -> {
		var bs = beanStore();
		var v = Holder.of(MethodList.of(getAnnotatedMethods(resource(), RestStartCall.class).toList()));
		bs.createBeanFromMethod(MethodList.class, resource().get(), x -> isBeanMethod(x, "startCallMethods"), v.get()).ifPresent(v::set);
		return v.get();
	}));

	/**
	 * The {@link StaticFiles} provider for this resource.
	 *
	 * <p>
	 * Defaults to {@link BasicStaticFiles}. {@code @Rest(staticFiles=X)} most-derived non-{@code Void} class wins.
	 * A bean-store override or {@code @Bean} factory method REPLACES the result.
	 */
	private final Memoizer<StaticFiles> staticFiles = memoizer(() -> {
		var bs = beanStore();
		var creator = BeanInstantiator.of(StaticFiles.class, bs).type(BasicStaticFiles.class).noBuilder();
		bs.getBeanType(StaticFiles.class).ifPresent(creator::type);
		// @Rest(staticFiles=X) — most-derived non-Void wins. See callLogger for the reduce-last rationale.
		getRestAnnotationsForProperty(PROPERTY_staticFiles)
			.map(ai -> ai.inner().staticFiles())
			.filter(c -> c != StaticFiles.Void.class)
			.reduce((first, second) -> second)
			.ifPresent(creator::type);
		bs.createBeanFromMethod(StaticFiles.class, resource().get(), RestContext::isBeanMethod).ifPresent(creator::impl);
		return creator.asOptional().orElse(null);
	});

	/**
	 * The {@link SwaggerProvider} for this resource.
	 *
	 * <p>
	 * Defaults to {@link BasicSwaggerProvider}. {@code @Rest(swaggerProvider=X)} most-derived
	 * non-{@code Void} class wins. A bean-store override or {@code @Bean} factory method REPLACES the result.
	 */
	private final Memoizer<SwaggerProvider> swaggerProvider = memoizer(() -> {
		var bs = beanStore();
		// Register the resource class as a SwaggerResource bean so SwaggerProvider implementations
		// can resolve it via constructor injection (rather than reading it off a Builder).
		bs.addBean(SwaggerResource.class, SwaggerResource.of(resourceClass()));
		var creator = BeanInstantiator.of(SwaggerProvider.class, bs).type(BasicSwaggerProvider.class).noBuilder();
		bs.getBeanType(SwaggerProvider.class).ifPresent(creator::type);
		// @Rest(swaggerProvider=X) — most-derived non-Void wins. See callLogger for the reduce-last rationale.
		getRestAnnotationsForProperty(PROPERTY_swaggerProvider)
			.map(ai -> ai.inner().swaggerProvider())
			.filter(c -> c != SwaggerProvider.Void.class)
			.reduce((first, second) -> second)
			.ifPresent(creator::type);
		bs.createBeanFromMethod(SwaggerProvider.class, resource().get(), RestContext::isBeanMethod).ifPresent(creator::impl);
		return creator.asOptional().orElse(null);
	});

	/**
	 * The {@link OpenApiProvider} for this resource.
	 *
	 * <p>
	 * Defaults to {@link BasicOpenApiProvider}. {@code @Rest(openApiProvider=X)} most-derived
	 * non-{@code Void} class wins. A bean-store override or {@code @Bean} factory method REPLACES the result.
	 */
	private final Memoizer<OpenApiProvider> openApiProvider = memoizer(() -> {
		var bs = beanStore();
		bs.addBean(OpenApiResource.class, OpenApiResource.of(resourceClass()));
		var creator = BeanInstantiator.of(OpenApiProvider.class, bs).type(BasicOpenApiProvider.class).noBuilder();
		bs.getBeanType(OpenApiProvider.class).ifPresent(creator::type);
		// @Rest(openApiProvider=X) — most-derived non-Void wins.
		getRestAnnotationsForProperty(PROPERTY_openApiProvider)
			.map(ai -> ai.inner().openApiProvider())
			.filter(c -> c != OpenApiProvider.Void.class)
			.reduce((first, second) -> second)
			.ifPresent(creator::type);
		bs.createBeanFromMethod(OpenApiProvider.class, resource().get(), RestContext::isBeanMethod).ifPresent(creator::impl);
		return creator.asOptional().orElse(null);
	});

	/**
	 * The {@link ThrownStore} for this resource, used to track exception statistics.
	 *
	 * <p>
	 * Inherits from the parent context's store when one is present. A bean-store override or
	 * {@code @Bean} factory method REPLACES the result.
	 */
	private final Memoizer<ThrownStore> thrownStore = memoizer(() -> {
		var bs = beanStore();
		var b = ThrownStore.create(bs).impl(parentContext() == null ? null : parentContext().getThrownStore());
		bs.createBeanFromMethod(ThrownStore.class, resource().get(), RestContext::isBeanMethod, b).ifPresent(b::impl);
		return b.build();
	});

	/**
	 * The runtime {@link VarResolver} — wraps the bootstrap resolver and adds {@link Messages} and {@link Config} beans.
	 *
	 * <p>
	 * The bootstrap {@link Config} is pulled from the {@code rawConfig} memoizer to avoid a circular
	 * dependency: the runtime {@link Config} wraps the bootstrap config in a session backed by this resolver.
	 * A bean-store override or {@code @Bean} factory method REPLACES the result.
	 */
	private final Memoizer<VarResolver> varResolver = memoizer(() -> {
		var bs = beanStore();
		var b = getBootstrapVarResolver().copy()
			.bean(Messages.class, getMessages())
			.bean(Config.class, rawConfig.get());
		var override = bs.createBeanFromMethod(VarResolver.class, resource().get(), RestContext::isBeanMethod, b).orElse(null);
		return nn(override) ? override : b.build();
	});

	/**
	 * Per-mixin {@link RestContext} sub-contexts, keyed by the mixin class.
	 *
	 * <p>
	 * Built lazily (alongside {@link #restOperations}) and only on the host's {@link RestContext} &mdash; mixin
	 * sub-contexts themselves never spawn further sub-contexts (the flat-inheritance rule: a mixin's
	 * {@code @Rest(mixins=B)} discovers B as a mixin of the host, not as a nested mixin of A).
	 *
	 * <p>
	 * Each sub-context is constructed with {@link Args#mixinContext()} set to {@code true}, with its
	 * {@code parentContext} pointing at this host context.  That parent-linkage drives the inheritance walk in
	 * {@link #getRestAnnotationsForProperty(String)} for serializers, parsers, encoders, converters, response
	 * processors, REST op args, guards, callLogger, debugEnablement, messages, varResolver tokens, etc.
	 *
	 * @since 10.0.0
	 */
	private final Memoizer<Map<Class<?>,RestContext>> mixinContexts = memoizer(() -> {
		if (isMixinContextField())
			return Map.of();
		var out = new LinkedHashMap<Class<?>,RestContext>();
		for (var mixinClass : getRestMixinClasses()) {
			if (mixinClass == resourceClass())
				continue;
			RestContext mixinCtx;
			try {
				mixinCtx = buildMixinContext(mixinClass);
			} catch (RuntimeException e) {
				throw e;
			} catch (Exception e) {
				throw rex(e, "Failed to construct mixin sub-context for {0}: {1}", mixinClass.getName(), e.getMessage());
			}
			out.put(mixinClass, mixinCtx);
		}
		return u(out);
	});

	/**
	 * Builds a per-mixin sub-{@link RestContext} for the given mixin class.
	 *
	 * <p>
	 * Instantiates the mixin resource via this host's bean store, constructs a new {@link RestContext} with
	 * {@code parentContext=this} and {@link Args#mixinContext() mixinContext=true}, and reflectively invokes
	 * any {@code setContext(RestContext)} method on the mixin instance (mirroring the existing
	 * {@code RestChildren.buildChildContext(...)} contract).  The host's {@link ServletConfig} is propagated
	 * so the mixin sees the same servlet container settings.
	 *
	 * <p>
	 * Mixin instance resolution: an importer-supplied {@code @Bean <MixinClass>} factory in the host's
	 * bean store wins over reflective instantiation. When the host registers
	 * {@code @Bean public MyMixin myMixin() { ... }} (or a Spring {@code @Bean} of the same type), that
	 * pre-built instance is used as the mixin resource; otherwise the mixin is instantiated via
	 * {@code beanStore.instantiate(mixinClass)} (no-arg / builder / injected-constructor path). This lets
	 * convention-endpoint mixins (favicon, SEO, version, well-known) be configured by the importer using
	 * their own builder before the mixin walk wires them in.
	 */
	private RestContext buildMixinContext(Class<?> mixinClass) throws Exception {
		Object preBuilt = beanStore.getBean(mixinClass).orElse(null);
		final var mixinResource = preBuilt != null ? preBuilt : beanStore.instantiate(mixinClass);
		var args = new Args(mixinClass, this, builder.inner, () -> mixinResource, "", null, null, null, true);
		var mixinCtx = new RestContext(args);
		var setCtx = ClassInfo.of(mixinResource).getMethod(x -> x.hasName("setContext") && x.hasParameterTypes(RestContext.class)).orElse(null);
		if (setCtx != null)
			setCtx.accessible().invoke(mixinResource, mixinCtx);
		return mixinCtx;
	}

	/**
	 * Returns the per-mixin {@link RestContext} sub-contexts indexed by mixin class.
	 *
	 * <p>
	 * Returns an empty map when this context is itself a mixin sub-context, or when no mixins are declared.
	 *
	 * @return An unmodifiable map of mixin class to its sub-{@link RestContext}; never {@code null}.
	 * @since 10.0.0
	 */
	public Map<Class<?>,RestContext> getMixinContexts() { return mixinContexts.get(); }

	/**
	 * The {@link RestOperations} for this resource — all {@link RestOpContext} instances built from
	 * methods annotated with {@link org.apache.juneau.rest.server.RestOp @RestOp} (and related).
	 *
	 * <p>
	 * Eagerly initialized in the constructor (via an explicit {@code .get()} call inside the try-catch block)
	 * so that any {@link ServletException} surfaces at construction time rather than lazily on first request.
	 * Subsequent calls return the cached instance.
	 *
	 * <p>
	 * For mixin classes declared via {@link Rest#mixins() @Rest(mixins)}, each mixin's {@code @RestOp} methods
	 * are bound to a per-mixin {@link RestContext} sub-context (see {@link #mixinContexts}) so that the
	 * mixin's annotation chain inherits from the host's via {@link #getRestAnnotationsForProperty(String)}.
	 * Mixin sub-contexts themselves never participate in mixin discovery &mdash; the {@code if (!isMixinContext)}
	 * guard below short-circuits the flat-inheritance rule (a mixin's {@code @Rest(mixins=B)} is collected at
	 * the host level, not nested under A).
	 */
	@SuppressWarnings({
		"java:S3776", // High cognitive complexity is inherent in REST operation set initialization.
		"java:S1141"  // Nested try/catch blocks required for granular error reporting during initialization.
	})
	private final Memoizer<RestOperations> restOperations = memoizer(() -> safe(() -> {
		initializeFrameworkBeansForRestOps();
		var bs = beanStore();
		var b = RestOperations.create(bs);
		var ap = getMarshallingContext().getAnnotationProvider();
		var rci = ClassInfo.of(resource().get());
		if (!isMixinContextField()) {
			var contexts = mixinContexts.get();
			for (var e : contexts.entrySet()) {
				var mixinClass = e.getKey();
				var mixinCtx = e.getValue();
				addRestOperationsForClass(b, ap, ClassInfo.of(mixinClass), mixinCtx, mixinCtx::getResource);
			}
		}
		addRestOperationsForClass(b, ap, rci, this, this::getResource);
		var override = bs.createBeanFromMethod(RestOperations.class, resource().get(), RestContext::isBeanMethod, b).orElse(null);
		return nn(override) ? override : b.build();
	}));

	private void addRestOperationsForClass(RestOperations.Builder b, AnnotationProvider ap, ClassInfo classInfo, RestContext opContext, Supplier<Object> targetSupplier) throws ServletException {
		for (var mi : classInfo.getPublicMethods()) {
			var al = rstream(ap.find(mi)).filter(REST_OP_GROUP).collect(Collectors.toList());
			if (al.isEmpty()) {
				Predicate<MethodInfo> isRestAnnotatedInterface = x -> x.getDeclaringClass().isInterface()
					&& nn(x.getDeclaringClass().getAnnotations(Rest.class).findFirst().map(AnnotationInfo::inner).orElse(null));
				mi.getMatchingMethods().stream().filter(isRestAnnotatedInterface).forEach(x -> al.add(AnnotationInfo.of(x, RestOpAnnotation.DEFAULT)));
			}
			if (!al.isEmpty()) {
				try {
					if (mi.isNotPublic())
						throw servletException("@RestOp method {0}.{1} must be defined as public.", classInfo.inner().getName(), mi.getNameSimple());
					var roc = new RestOpContext(mi.inner(), opContext, targetSupplier);
					if ("RRPC".equals(roc.getHttpMethod())) {
						RestOpContext roc2 = new RrpcRestOpContext(mi.inner(), opContext, targetSupplier);
						b.add("GET", roc2).add("POST", roc2);
					} else {
						b.add(roc);
					}
				} catch (Exception e) {
					throw servletException(e, "Problem occurred trying to initialize methods on class {0}", classInfo.inner().getName());
				}
			}
		}
	}

	private LinkedHashSet<Class<?>> getRestMixinClasses() {
		var out = new LinkedHashSet<Class<?>>();
		var visited = new HashSet<Class<?>>();
		getRestAnnotationsForProperty(PROPERTY_mixins).forEach(ai -> {
			for (var mixin : ai.inner().mixins())
				collectRestMixins(mixin, out, visited);
		});
		return out;
	}

	private void collectRestMixins(Class<?> mixin, LinkedHashSet<Class<?>> out, Set<Class<?>> visited) {
		if (mixin == null || mixin == resourceClass())
			return;
		if (!visited.add(mixin))
			return;
		out.add(mixin);
		var r = mixin.getAnnotation(Rest.class);
		if (r != null) {
			for (var nested : r.mixins())
				collectRestMixins(nested, out, visited);
		}
	}

	/**
	 * The {@link RestChildren} for this resource — child {@link RestContext} instances registered via
	 * {@link Rest#children() @Rest(children)}.
	 *
	 * <p>
	 * Reads child classes directly from the {@code @Rest(children)} annotation chain (parent-to-child),
	 * deduplicates, instantiates each child via the bean store, and builds a {@link RestContext} for each.
	 * Eagerly initialized in the constructor (via an explicit {@code .get()} call inside the try-catch
	 * block) so that any construction failure surfaces at initialization time rather than lazily.
	 */
	@SuppressWarnings({
		"java:S3776" // cognitive complexity acceptable for child-context construction
	})
	private final Memoizer<RestChildren> restChildren = memoizer(() -> safe(() -> {
		var bs = beanStore();
		var servletConfig = bs.getBean(ServletConfig.class).orElse(null);
		var b = RestChildren.create(this, bs, servletConfig);

		// Collect child classes from @Rest(children) on the annotation chain (parent-to-child order).
		// Deduplicate so the same child class registered on both a parent and child annotation
		// doesn't create two contexts.
		var seen = new LinkedHashSet<Class<?>>();
		getRestAnnotations().forEach(ai -> seen.addAll(Arrays.asList(ai.inner().children())));

		var lazy = isLazyChildren();

		for (var rc2 : seen) {
			if (rc2 == resourceClass())
				continue;  // Guard against self-reference infinite loop.
			if (lazy) {
				// Lazy: register a routing stub now; defer full RestContext construction to first request.
				b.addLazy(rc2, "");
			} else {
				// Eager (default): build the full child RestContext immediately.
				b.add(RestChildren.buildChildContext(this, bs, servletConfig, rc2, null, ""));
			}
		}

		// @Bean override — allows replacing the entire RestChildren instance.
		var override = bs.createBeanFromMethod(RestChildren.class, resource().get(), RestContext::isBeanMethod, b).orElse(null);
		return nn(override) ? override : b.build();
	}));

	/**
	 * Constructor.
	 *
	 * @param args The bootstrap arguments. Must not be <jk>null</jk>.
	 * @throws Exception If any initialization problems were encountered.
	 * @since 10.0.0
	 */
	public RestContext(Args args) throws Exception {
		this(new Builder(args));
	}

	/**
	 * Internal constructor.
	 *
	 * <p>
	 * Privatized in the April 2026 refactor (2026-04-19). External callers must use
	 * {@link #RestContext(Args)} instead.
	 *
	 * @param builder The builder containing the settings for this bean.
	 * @throws Exception If any initialization problems were encountered.
	 */
	@SuppressWarnings({
		"java:S3776", // High cognitive complexity is inherent in REST context constructor initialization.
		"java:S1141"  // Nested try/catch blocks required for granular error reporting during initialization.
	})
	private RestContext(Builder builder) throws Exception {
		super(builder);

		startTime = Instant.now();

		REGISTRY.put(builder.resourceClass, this);

		BasicHttpException initExceptionTemp = null;

		try {
			this.builder = builder;

			parentContext = builder.parentContext;
			isMixinContext = builder.args.mixinContext();
			resourceClass = builder.resourceClass;
			var rs = new ResourceSupplier(resourceClass, assertArgNotNull("resource", builder.args.resource()));
			resource = rs;

			// Resolve the programmatic configuration builder.  Prefer the one carried on Args (set by
			// RestServlet.init()); otherwise read the builder stashed on the resource instance (non-reflective) so
			// programmatic construction via MockRestClient, child mounting, and mixin composition all honor it.
			var rb = builder.args.restBuilder();
			if (rb == null)
				rb = stashedRestBuilder(rs.get());
			restBuilder = rb;
			if (restBuilder instanceof AbstractRestBuilder<?,?> arb) {
				// Synthetic, highest-priority @Rest carrying the builder-set members; prepended (most-derived
				// child position) to the @Rest chain by the restAnnotations memoizer so builder values win.
				builderRestAnnotation = arb.toRestAnnotation();
				// Programmatic mdcAsyncPropagation knob (no @Rest member) wins over the env-driven default.
				if (arb.getMdcAsyncPropagation() != null && builder.mdcAsyncPropagation == null)
					builder.mdcAsyncPropagation = arb.getMdcAsyncPropagation();
			} else {
				builderRestAnnotation = null;
			}

			// --- beanStore setup (May 2026 refactor; precedence-flipped 10.0) ---

			// Determine the parent (bootstrap) store: inherited from parent resource if present.
			//
			// For mixin sub-contexts, the parent is the host's FULL beanStore (not just the bootstrap layer)
			// so that resource-class-level @Bean factory methods declared on the host class are visible to
			// the mixin's resolution chain.  Mixin endpoints resolved through the host's beanStore directly;
			// with per-mixin sub-contexts, the same effect is
			// achieved by parent-linking the mixin's beanStore to the host's full beanStore.
			//
			// For top-level resources and @Rest(children) sub-resources, the parent remains the bootstrap
			// layer (children are intentionally independent of the host's resource-class beans).
			WritableBeanStore parentBs;
			if (parentContext != null && isMixinContext)
				parentBs = parentContext.beanStore;
			else if (parentContext != null)
				parentBs = parentContext.bootstrapBeanStore;
			else
				parentBs = null;

			// Build the initial beanStore; honor an optional @Bean WritableBeanStore override.
			// In the new 10.0 precedence model, the parent (Spring or parent-resource bootstrap) is
			// installed as the overriding parent so it wins over local entries.
			// Args.overridingParent threads into the per-resource (final beanStore) overridingParent
			// slot so test-time overlays (TestBeanStore) resolve at tier 1 of the chain ahead of
			// any @Bean factory result registered as a local entry below.
			// @formatter:off
			var argsOverlay = builder.args.overridingParent();
			WritableBeanStore bs = createBeanStore(parentBs, rs, parentBs == null ? null : argsOverlay)
				.addBean(ResourceSupplier.class, rs)
				.addBean(ServletConfig.class, nn(builder.inner) ? builder.inner : builder)
				.addBean(ServletContext.class, (nn(builder.inner) ? builder.inner : builder).getServletContext());
			// @formatter:on

			// If no parent store, promote bs to bootstrap and layer a fresh per-resource v2 store on top.
			// The overlay belongs on the per-resource store so it wins over @Bean factories registered
			// as local entries below; the bootstrap deliberately does NOT get the overlay (it sits above
			// the per-resource entries via the parent chain, which is too low for an overlay).
			if (parentBs == null) {
				bootstrapBeanStore = bs;
				bs = new BasicBeanStore(bootstrapBeanStore, argsOverlay);
			} else {
				bootstrapBeanStore = parentBs;
			}
			beanStore = bs;

			beanStore.addBean(WritableBeanStore.class, beanStore);
			// Register the bootstrap VarResolver as a NAMED entry only.  The unnamed VarResolver
			// slot is intentionally left to the registerFrameworkDefaults() default supplier (which
			// resolves to the full runtime VarResolver) so that internal accessors such as
			// getVarResolver() return the right thing when routed through beanStore.getBean(...).
			beanStore.add(VarResolver.class, getBootstrapVarResolver(), PROP_bootstrapVarResolver);
			// Force-build raw Config now (fail fast if @Rest(config) is misconfigured).  The unnamed
			// Config slot in the bean store is intentionally left to the default supplier (the full
			// runtime Config) — @RestInit hooks that take Config as a parameter will see the fully
			// resolved instance instead of the raw bootstrap Config (10.0 behavior change).
			rawConfig.get();
			// Per-RestContext @Value resolution bridge: register the @Rest(config=...) Configs as
			// PropertySource beans inside THIS resource's BeanStore (NOT on the process-wide
			// Settings.get() singleton — that path was a 6.7x perf regression because
			// MockRestClient's static RestContext cache leaks instances and Settings.get()'s source
			// list grew unbounded). Per-resource isolation comes for free from the BeanStore scope.
			registerRestConfigPropertySources();

			// Register memoizer-backed defaults for every framework-managed type.  These sit at the
			// bottom of the precedence order and only fire when no @Bean method, no programmatic
			// add, and no Spring/overriding-parent bean has been registered for the type.  This is
			// what removes the need for the old DELAYED_INJECTION gate-keeping list — the @Bean
			// walk below can now invoke any framework type's factory and still resolve framework
			// dependencies through the bean store.
			registerFrameworkDefaults(beanStore);

			// For mixin sub-contexts, the bean store is parent-linked to the host's full beanStore so that
			// host-declared @Bean factory results (e.g. @Bean(name="db") HealthIndicator dbIndicator()) are
			// visible through the mixin's lookup chain.  But the parent walk also picks up the host's
			// framework defaults (SerializerSet, ParserSet, CallLogger, ...) at the parent's tier-4 slot
			// before this store's tier-4 defaults can fire — which would shadow this mixin's per-context
			// framework objects.  Promoting our defaults into local entries (tier 2) makes them resolve
			// ahead of the parent walk while still letting an overriding parent (e.g. Spring) and explicit
			// local @Bean registrations win.  Top-level resources and @Rest(children=...) sub-resources keep
			// the original tier-4 default semantics so a parent-supplied framework bean (e.g. Spring's
			// SerializerSet) still wins as it did before.
			if (isMixinContext && beanStore instanceof BasicBeanStore mixinBs)
				mixinBs.promoteDefaultsToLocalSuppliers();

			var rci2 = ClassInfo.of(resourceClass);

			// Register @Bean fields that already have a value.
			// @formatter:off
			rci2.getAllFields().stream()
				.filter(x -> x.hasAnnotation(Bean.class))
				.forEach(x -> opt(x.get(resource.get())).ifPresent(
					y -> beanStore.add(
						x.getFieldType().inner(),
						y,
						BeanAnnotation.name(x.getAnnotations(Bean.class).findFirst().map(AnnotationInfo::inner).orElse(null))
					)
				));
			// @formatter:on

			// Run @Bean methods and register their results as LOCAL entries (level 2 of resolve()).
			//
			// For non-framework types: invoke the @Bean method directly via createBeanFromMethod
			// and store the result via addBean.
			//
			// For framework types (those with a default supplier registered above): the @Bean
			// scan already ran inside the corresponding memoizer body (see e.g. createCallLogger()),
			// so re-invoking createBeanFromMethod here would create a SECOND instance and produce
			// inconsistent state between the framework's memoizer-backed bean and the bean store's
			// local entry.  Instead, PROMOTE the existing default supplier (which is memoizer-backed
			// and resolves to the @Bean value when one was supplied) into a local-entry supplier.
			// Promoting at level 2 means @Bean results win over a parent (Spring) at level 3.
			//
			// Net effect: @Bean method results uniformly take precedence over Spring/parent
			// bindings for both framework and user-defined types.  This auto-derives the legacy
			// DELAYED_INJECTION list from the default-supplier registrations.
			rci2.getAllMethods().stream().filter(x -> x.hasAnnotation(Bean.class)).forEach(x -> {
				var rt = x.getReturnType().<Object>inner();
				var name = BeanAnnotation.name(x.getAnnotations(Bean.class).findFirst().map(AnnotationInfo::inner).orElse(null));
				// Skip the WritableBeanStore factory (already consumed by createBeanStore()).
				if (WritableBeanStore.class.equals(rt) || BeanStore.class.equals(rt))
					return;
				if (beanStore instanceof BasicBeanStore bbs2 && bbs2.hasDefaultSupplier(rt, name)) {
					bbs2.getDefaultSupplier(rt, name).ifPresent(sup -> beanStore.addSupplier(rt, sup, name));
					return;
				}
				beanStore.createBeanFromMethod(rt, resource.get(), RestContext::isBeanMethod)
					.ifPresent(y -> beanStore.addBean(rt, y, name));
			});

			// Run @RestInit-annotated methods on the resource object (deduplicated by signature, top-down order).
			// Note: pre-10.0 this filter also excluded `y.hasParameter(RestOpContext.Builder.class)` because the
			// per-op `@RestInit(RestOpContext.Builder)` flow handled those separately. That flow was deleted in
			// April 2026 refactor Route B (zero real-world callers), so the exclusion is gone too. Likewise, the
			// class-level `@RestInit(RestContext.Builder)` injection protocol was deleted in the same refactor
			// (zero non-test callers anywhere) — `RestContext.Builder` is no longer added to the bean store, so
			// any straggling `@RestInit` method that still declares either Builder type as a parameter will now
			// surface a "missing prerequisites" error here, which is the desired loud-failure signal.
			var r = resource.get();
			var initMap = CollectionUtils.<String,MethodInfo>map();
			ClassInfo.ofProxy(r).getAllMethodsTopDown().stream()
				.filter(y -> y.hasAnnotation(RestInit.class))
				.forEach(y -> { var sig = y.getSignature(); if (!initMap.containsKey(sig)) initMap.put(sig, y.accessible()); });
			for (var m : initMap.values()) {
				if (!m.canResolveAllParameters(beanStore, r))
					throw servletException("Could not call @RestInit method {0}.{1}.  Could not find prerequisites: {2}.", cns(m.getDeclaringClass()), m.getSignature(), m.getMissingParameterTypes(beanStore, r));
				try {
					m.inject(beanStore, r);
				} catch (Exception e) {
					throw servletException(e, "Exception thrown from @RestInit method {0}.{1}.", cns(m.getDeclaringClass()), m.getSignature());
				}
			}

			// Back-fill @Bean fields that were null before init hooks ran.
			// @formatter:off
			rci2.getAllFields().stream()
				.filter(x -> x.hasAnnotation(Bean.class))
				.forEach(x -> x.setIfNull(
					resource.get(),
					beanStore.getBean(
						x.getFieldType().inner(),
						BeanAnnotation.name(x.getAnnotations(Bean.class).findFirst().map(AnnotationInfo::inner).orElse(null))
					).orElse(null)
				));
			// @formatter:on

			// Populate @Inject-annotated fields and methods on the resource, then fire @PostConstruct
			// callbacks.  This covers the JSR-330 / Spring @Autowired / jakarta.inject.Inject FQNs as
			// well as the Juneau-owned @Inject in commons.inject — recognition is FQN-based, see JsrSupport.
			rci2.inject(resource.get(), beanStore);

			builder.args.beanStoreConfigurer().accept(beanStore);

			// --- end beanStore setup ---

			// Path resolution: explicit Args.path (non-empty) wins so callers like
			// RestChildren.addChild(String,Object) can mount the same class at a custom path; otherwise read
			// from the @Rest(path) annotation chain (most-derived class wins). This restores override behavior
			// removed in the May 2026 Builder.path elimination, without resurrecting the staging field.
			var argsPath = builder.args.path();
			if (argsPath != null && ! argsPath.isEmpty()) {
				path = trimLeadingSlashes(argsPath);
			} else {
				path = getRestAnnotations().stream()
					.map(ai -> ai.inner().path())
					.filter(StringUtils::isNotEmpty)
					.findFirst()
					.map(s -> trimLeadingSlashes(s))
					.orElse("");
			}
			fullPath = (parentContext == null ? "" : (parentContext.fullPath + '/')) + path;
			var p = path;
			if (! p.endsWith("/*"))
				p += "/*";
			pathMatcher = UrlPathMatcher.of(p);

			// Top-level mount-paths resolution chain (programmatic > getter > annotation default).
			// See javadoc on getPaths() for the full precedence order. Resolution happens once at construction;
			// the resolved array is what hosting runtimes (Jetty auto-discovery, Spring Boot ServletRegistrationBean)
			// should consume rather than reading the raw @Rest(paths) annotation directly.  Each @Rest(paths)
			// element is SVL-resolved through a VarResolver session backed by the live bean store — so
			// $C{key} consults whatever Config is currently registered (test overlay, @Bean factory, or the
			// framework's runtime Config) and $E{NAME,default} / $S{prop,default} use the bootstrap variable
			// catalog without needing any beans.  The session-with-beanstore form is what makes test-time
			// Config injection visible to SVL without firing the full runtime VarResolver memoizer.
			paths = resolveMountPaths(builder, resource.get(), getBootstrapVarResolver(), beanStore, getRestAnnotations());

			// Build annotation work list, then trigger beanContextBuilder (which applies it).
			var vrs = getBootstrapVarResolver().createSession();
			annotationWork = AnnotationWorkList.of(vrs, rstream(AnnotationProvider.INSTANCE.find(rci2)).filter(CONTEXT_APPLY_FILTER));
			beanContextBuilder.get(); // force init with annotationWork now set

			// @formatter:off
			beanStore
				.addBean(RestContext.class, this)
				.addBean(Object.class, resource.get())
				.addBean(Builder.class, builder)
				.addBean(AnnotationWorkList.class, annotationWork);
			// @formatter:on

		// Inject @Value-annotated env-driven defaults onto this RestContext instance so that the
		// memoizer lambdas below (which run lazily on first .get() call) read fully resolved values.
		// Must run BEFORE isEagerInit() since the eagerInit memoizer itself reads defaultEagerInit.
		ClassInfo.of(this).inject(this, beanStore);

		// Startup-fail if @Rest(observability="true") is set but neither MetricsRecorder nor TracerHook is
		// supplied. The check runs after all @Bean method injection so that consumer-provided beans are visible.
		checkObservabilityBackendPresent();

		// Startup-fail: if asyncCompletionExecutor is configured (either by annotation or
		// programmatically), force-evaluate the memoizer now so "bean not found" surfaces at context
		// init time rather than lazily on the first request.
		checkAsyncCompletionExecutorPresent();

		if (isEagerInit()) {
				// Force-fire the framework-bean memoizers in dependency-friendly order so their @Rest()
				// annotation walks (e.g. `@Rest(partParser=…)`, `@Rest(partSerializer=…)`, `@Rest(encoders=…)`,
				// etc.) execute eagerly inside the try-catch.  These walks MUTATE the cached creators that
				// downstream RestOpContext memoizers later copy from, so they must run before
				// getRestOperations() builds the per-op contexts.  We do NOT re-add results to the bean
				// store — the default suppliers registered earlier already cover lookups.
				initializeFrameworkBeansForRestOps();

				// Force-initialize restOperations and restChildren now so that any construction failures
				// (e.g. bad @RestOp method or invalid child class) surface here inside the try-catch.
				getRestOperations();
				getRestChildren();
			}

			// produces/consumes are resolved lazily via the produces/consumes memoizers below
			// (April 2026 refactor, 2026-04-19) — they walk the @Rest(produces=...) / @Rest(consumes=...)
			// chain first, then fall back to deriving the intersection of opContexts' supported media types.

		} catch (BasicHttpException e) {
			initExceptionTemp = e;
			throw e;
		} catch (Exception e) {
			initExceptionTemp = new InternalServerError(e);
			throw e;
		} finally {
			initException = initExceptionTemp;
		}
	}

	@Override /* Overridden from Context */
	public RestSession.Builder createSession() {
		return RestSession.create(this);
	}

	//---------------------------------------------------------------------------------------------
	// Memoized allowlist fields
	//---------------------------------------------------------------------------------------------

	/**
	 * Memoized parser session-option keys from {@code @Rest(allowedParserOptions)}, after SVL resolution and comma expansion.
	 *
	 * <p>
	 * When inheritance is not blocked, keys from {@link #parentContext} are included first, then this resource's own tokens.
	 * Leading-hyphen tokens (e.g. {@code -foo}) remove earlier positive tokens.
	 */
	private final Memoizer<SortedSet<String>> allowedParserOptions = memoizer(() -> {
		var l = new ArrayList<String>();
		var p = PROPERTY_allowedParserOptions;
		var pc = parentContext();
		if (isInherited(p) && pc != null)
			l.addAll(pc.getAllowedParserOptions());
		getRestAnnotationsForProperty(p).forEach(x -> resolveCdl(x.getStringArray(p)).forEach(l::add));
		return u(treeSetCi(removeNegations(l)));
	});

	/**
	 * Memoized serializer session-option keys from {@code @Rest(allowedSerializerOptions)}, after SVL resolution and comma expansion.
	 *
	 * <p>
	 * When inheritance is not blocked, keys from {@link #parentContext} are included first, then this resource's own tokens.
	 * Leading-hyphen tokens remove earlier positive tokens.
	 */
	private final Memoizer<SortedSet<String>> allowedSerializerOptions = memoizer(() -> {
		var l = new ArrayList<String>();
		var p = PROPERTY_allowedSerializerOptions;
		var pc = parentContext();
		if (isInherited(p) && pc != null)
			l.addAll(pc.getAllowedSerializerOptions());
		getRestAnnotationsForProperty(p).forEach(x -> resolveCdl(x.getStringArray(p)).forEach(l::add));
		return u(treeSetCi(removeNegations(l)));
	});

	/**
	 * Memoized value of the {@code noInherit} annotation attribute from the nearest {@code @Rest} annotation.
	 *
	 * <p>
	 * {@code noInherit} itself is never inherited; it only applies to the {@code @Rest} that declares it.
	 */
	private final Memoizer<SortedSet<String>> noInherit = memoizer(() ->
		getRestAnnotation()
			.map(x -> x.getStringArray("noInherit").orElse(StringUtils.EMPTY_STRING_ARRAY))
			.map(x -> treeSet(String.CASE_INSENSITIVE_ORDER, resolveCdl(x).toList()))
			.orElseGet(Collections::emptySortedSet)
	);

	/**
	 * Memoized list of every {@link Rest} annotation on the resource class and its supertypes, in child-to-parent order.
	 *
	 * <p>
	 * For <b>host</b> contexts (top-level resources), {@link DefaultConfig} is always synthesized as the top-most
	 * (parent-most) entry when not already present in the class hierarchy. This makes {@code DefaultConfig.@Rest(...)}
	 * the framework's single source of truth for default lists (response processors, REST op args, encoders, parsers,
	 * serializers, ...), while still letting bare {@code @Rest}-annotated resources inherit the framework defaults
	 * without explicitly implementing {@link BasicUniversalConfig} or any other {@code DefaultConfig} descendant.
	 * Resources that <i>do</i> implement {@code DefaultConfig} (transitively or directly) won't get a duplicate
	 * entry — the list is dedup'd by the {@link Class} the annotation was declared on.
	 *
	 * <p>
	 * For <b>mixin</b> sub-contexts the synthesized {@code DefaultConfig} fallback is omitted: the mixin's annotation
	 * chain is composed with the host's chain by
	 * {@link #getRestAnnotationsForProperty(String) getRestAnnotationsForProperty(...)}, and the host's chain already
	 * supplies the {@code DefaultConfig} fallback (synthesized or transitive). Including it on the mixin too would
	 * place a second {@code DefaultConfig} entry between the host's most-derived {@code @Rest} and the mixin's own
	 * annotations in the combined parent-to-child walk, which would let the framework defaults silently override
	 * a host's explicit per-property values for any property the mixin doesn't itself declare (e.g.
	 * {@code partSerializer}, {@code partParser}).
	 */
	private final Memoizer<List<AnnotationInfo<Rest>>> restAnnotations = memoizer(() -> prependBuilderRestAnnotation(computeRawRestAnnotations()));

	/**
	 * Computes the raw {@code @Rest} annotation chain (most-derived first), folding in the framework
	 * {@code DefaultConfig} annotations for non-mixin contexts.  Does not include the synthetic builder annotation.
	 *
	 * @return The raw annotation chain.
	 */
	private List<AnnotationInfo<Rest>> computeRawRestAnnotations() {
		var raw = getAnnotationProvider().find(Rest.class, ClassInfo.of(getResourceClass()));
		if (isMixinContextField())
			return raw;
		var hasDefaultConfig = raw.stream().anyMatch(ai -> ai.getAnnotatable() instanceof ClassInfo ci && DefaultConfig.class.equals(ci.inner()));
		if (hasDefaultConfig)
			return raw;
		var defaultConfigAnnotations = getAnnotationProvider().find(Rest.class, ClassInfo.of(DefaultConfig.class));
		if (defaultConfigAnnotations.isEmpty())
			return raw;
		var combined = new ArrayList<>(raw);
		combined.addAll(defaultConfigAnnotations);
		return Collections.unmodifiableList(combined);
	}

	/**
	 * Prepends the synthetic builder-supplied {@code @Rest} annotation at the most-derived (child)
	 * position so its set members win in both child-first ({@code findFirst}) and parent-to-child
	 * ({@code reduce-last}) resolution walks.  Returns {@code base} unchanged when there is no programmatic builder.
	 *
	 * @param base The raw annotation chain.
	 * @return The chain with the synthetic annotation prepended (when present).
	 */
	private List<AnnotationInfo<Rest>> prependBuilderRestAnnotation(List<AnnotationInfo<Rest>> base) {
		if (builderRestAnnotation == null)
			return base;
		var combined = new ArrayList<AnnotationInfo<Rest>>(base.size() + 1);
		combined.add(AnnotationInfo.of(ClassInfo.of(getResourceClass()), builderRestAnnotation));
		combined.addAll(base);
		return Collections.unmodifiableList(combined);
	}

	/**
	 * Non-reflectively reads the programmatic configuration builder stashed on a resource/mixin instance,
	 * or returns <jk>null</jk> when the instance carries none or is not a builder-aware base type.
	 *
	 * @param r The resource instance.
	 * @return The stashed builder, or <jk>null</jk>.
	 */
	private static RestBuilder<?> stashedRestBuilder(Object r) {
		if (r instanceof RestServlet x)
			return x.getRestBuilder();
		if (r instanceof RestResource x)
			return x.getRestBuilder();
		if (r instanceof RestMixin x)
			return x.getRestBuilder();
		return null;
	}

	/**
	 * The {@code @Rest} annotation list for this resource in parent-to-child (top-down) order.
	 *
	 * <p>
	 * This is the reverse of {@link #restAnnotations}: most-ancestor annotation first, most-derived last.
	 * Use this when walking the annotation chain in inheritance order so that child values append after or
	 * override parent values.
	 */
	private final Memoizer<List<AnnotationInfo<Rest>>> restAnnotationsTopDown = memoizer(() -> {
		var list = new ArrayList<>(getRestAnnotations());
		Collections.reverse(list);
		return Collections.unmodifiableList(list);
	});

	private Stream<AnnotationInfo<Rest>> restAnnotationsForPropertySortedByRank(String propertyName) {
		return getRestAnnotationsForProperty(propertyName).sorted(Comparator.comparingInt(AnnotationInfo::getRank));
	}

	private static String leadingEnumToken(String s) {
		s = trim(s);
		if (isEmpty(s))
			return s;
		int i = 0;
		while (i < s.length() && (Character.isLetter(s.charAt(i)) || s.charAt(i) == '_'))
			i++;
		return s.substring(0, i);
	}

	private <E extends Enum<E>> Optional<E> parseEnumConstant(Class<E> enumClass, String resolved) {
		var t = leadingEnumToken(trim(emptyIfNull(resolved)));
		if (isEmpty(t))
			return opte();
		try {
			return opt(Enum.valueOf(enumClass, t));
		} catch (IllegalArgumentException ignored) {
			return opte();
		}
	}

	String mergeReplacedStringAttribute(String propertyName, String initialFromEnvOrNull) {
		var v = new AtomicReference<>(initialFromEnvOrNull == null ? null : resolve(initialFromEnvOrNull));
		restAnnotationsForPropertySortedByRank(propertyName).forEach(ai ->
			ai.getString(propertyName)
				.filter(StringUtils::isNotBlank)
				.map(this::resolve)
				.filter(StringUtils::isNotBlank)
				.ifPresent(v::set));
		return v.get();
	}

	private boolean mergeReplacedBooleanAttribute(String propertyName, boolean envDefault) {
		var v = new AtomicReference<>(envDefault);
		restAnnotationsForPropertySortedByRank(propertyName).forEach(ai ->
			ai.getString(propertyName)
				.filter(StringUtils::isNotBlank)
				.map(this::resolve)
				.filter(StringUtils::isNotBlank)
				.ifPresent(s -> v.set(Boolean.parseBoolean(s))));
		return v.get();
	}

	/**
	 * Header names that may be passed via URL query parameter; resolved from {@code @Rest(allowedHeaderParams)},
	 * default {@code "Accept,Content-Type"}.
	 */
	private final Memoizer<Set<String>> allowedHeaderParams = memoizer(() ->
		Collections.unmodifiableSet(newCaseInsensitiveSet(mergeReplacedStringAttribute(PROPERTY_allowedHeaderParams, defaultAllowedHeaderParams))));

	/**
	 * HTTP method names that may be specified via a request header; resolved from {@code @Rest(allowedMethodHeaders)},
	 * default empty.
	 */
	private final Memoizer<Set<String>> allowedMethodHeaders = memoizer(() ->
		Collections.unmodifiableSet(newCaseInsensitiveSet(mergeReplacedStringAttribute(PROPERTY_allowedMethodHeaders, defaultAllowedMethodHeaders))));

	/**
	 * HTTP method names that may be specified via URL query parameter; resolved from {@code @Rest(allowedMethodParams)},
	 * default {@code "HEAD,OPTIONS"}.
	 */
	private final Memoizer<Set<String>> allowedMethodParams = memoizer(() ->
		Collections.unmodifiableSet(newCaseInsensitiveSet(mergeReplacedStringAttribute(PROPERTY_allowedMethodParams, defaultAllowedMethodParams))));

	/**
	 * Whether a {@code &content=} URL parameter may override the request body; inverse of
	 * {@code @Rest(disableContentParam)}.
	 */
	private final Memoizer<Boolean> allowContentParam = memoizer(() ->
		!mergeReplacedBooleanAttribute(PROPERTY_disableContentParam, defaultDisableContentParam));

	/**
	 * Whether exception stack traces are rendered in error responses; resolved from
	 * {@code @Rest(renderResponseStackTraces)}.
	 */
	private final Memoizer<Boolean> renderResponseStackTraces = memoizer(() ->
		mergeReplacedBooleanAttribute(PROPERTY_renderResponseStackTraces, defaultRenderResponseStackTraces));

	/**
	 * Whether the resource emits RFC 7807 {@code application/problem+json} responses; resolved from
	 * {@code @Rest(problemDetails)}.
	 */
	private final Memoizer<Boolean> problemDetails = memoizer(() ->
		mergeReplacedBooleanAttribute(PROPERTY_problemDetails, defaultProblemDetails));

	/**
	 * Whether the resource opts into per-request virtual-thread dispatch on Java 21+; resolved from
	 * {@code @Rest(virtualThreads)}.
	 *
	 * <p>
	 * Detection happens at context-init via {@link #mergeReplacedBooleanAttribute(String, boolean)}; on JVMs older
	 * than Java 21 the flag is logged once and ignored — see {@link #virtualThreadExecutor}.
	 */
	private final Memoizer<Boolean> virtualThreadsEnabled = memoizer(() ->
		mergeReplacedBooleanAttribute(PROPERTY_virtualThreads, defaultVirtualThreads));

	/**
	 * Resource-level observability tri-state from {@code @Rest(observability)}: {@code "true"} (strict opt-in),
	 * {@code "false"} (explicit opt-out), or {@code null} / empty (default inherit behavior).
	 *
	 * <p>
	 * Per-operation overrides (from verb / {@code @RestOp} annotations) are resolved independently in
	 * {@link RestOpContext}.
	 */
	private final Memoizer<String> observabilityAttribute = memoizer(() ->
		mergeReplacedStringAttribute(PROPERTY_observability, null));

	/**
	 * Configurable async-response timeout (milliseconds) applied by {@code AsyncResponseProcessor} to
	 * {@link CompletableFuture}-returning handlers; resolved from {@code @Rest(asyncTimeoutMillis)}.
	 *
	 * <p>
	 * {@code 0} disables the timeout. The default 30-second fallback is applied by {@code AsyncResponseProcessor}
	 * itself when neither the resource nor the operation declares a value.
	 */
	private final Memoizer<Long> asyncTimeoutMillis = memoizer(() -> {
		var s = mergeReplacedStringAttribute(PROPERTY_asyncTimeoutMillis, null);
		if (isEmpty(s))
			return -1L;
		try {
			return Long.parseLong(s.trim());
		} catch (NumberFormatException nfe) {
			ASYNC_LOG.log(Level.WARNING, () -> "Invalid @Rest(asyncTimeoutMillis) value '" + s + "' — falling back to default.");
			return -1L;
		}
	});

	/**
	 * The async-completion executor for this resource, resolved by name from the bean store; {@code null} when
	 * {@code @Rest(asyncCompletionExecutor)} is blank (the common case).
	 *
	 * <p>
	 * The memoizer throws {@link IllegalStateException} at first access if the bean name was supplied but no
	 * matching {@link Executor} bean is found — this surfaces as a startup failure, not a silent no-op.
	 */
	private final Memoizer<Executor> asyncCompletionExecutor = memoizer(() -> {
		// Programmatic override takes priority over the annotation chain.
		var name = resolveAsyncCompletionExecutorName();
		if (isBlank(name))
			return null;
		var resolved = beanStore().getBean(Executor.class, name);
		// Try ExecutorService as well (a common supertype users supply that is-a Executor).
		if (resolved.isEmpty())
			resolved = beanStore().getBean(ExecutorService.class, name).map(e -> (Executor) e);
		if (resolved.isEmpty())
			throw new IllegalStateException(
				"@Rest(asyncCompletionExecutor) on " + getResourceClass().getName()
					+ " references bean name '" + name + "' but no Executor (or ExecutorService) bean"
					+ " with that name is registered in the resource's bean store.");
		return resolved.get();
	});

	// Extracted to a method so the compiler does not flag a blank-final-field access inside a field-initializer lambda.
	private String resolveAsyncCompletionExecutorName() {
		return builder.asyncCompletionExecutorName != null
			? builder.asyncCompletionExecutorName
			: mergeReplacedStringAttribute(PROPERTY_asyncCompletionExecutor, null);
	}

	/**
	 * Lazily-instantiated virtual-thread executor used by {@link RestOpInvoker} when
	 * {@code @Rest(virtualThreads=true)} is set on this resource and the runtime is Java 21+.
	 *
	 * <p>
	 * On Java 17/18/19/20 the supplier returns {@code null} and emits a one-shot {@code WARNING} log so the
	 * resource degrades gracefully to caller-thread dispatch.
	 */
	private final Memoizer<Executor> virtualThreadExecutor = memoizer(() -> {
		// NOTE: Intentionally not gated on resource-level {@code virtualThreadsEnabled.get()} — per-op
		// {@code @RestOp(virtualThreads="true")} can opt in even when the enclosing {@code @Rest} doesn't.
		// Callers ({@link RestOpInvoker#invokeOp}) only reach this method when the effective op-level flag is true,
		// so the warning emitted on Java &lt;21 is appropriate at first call regardless of where the flag is set.
		if (Runtime.version().feature() < 21) {
			ASYNC_LOG.log(Level.WARNING, () -> "virtualThreads=true configured on " + getResourceClass().getName()
				+ " but runtime is Java " + Runtime.version().feature() + " — virtual-thread dispatch requires Java 21+. "
				+ "Falling back to caller-thread dispatch.");
			return null;
		}
		try {
			var m = Executors.class.getMethod("newVirtualThreadPerTaskExecutor");
			return (Executor) m.invoke(null);
		} catch (ReflectiveOperationException e) {
			ASYNC_LOG.log(Level.WARNING, e, () -> "Reflective creation of virtual-thread executor failed on "
				+ getResourceClass().getName() + " — falling back to caller-thread dispatch.");
			return null;
		}
	});

	/** Logger for async / virtual-thread setup events. Used at memoizer init before {@link #getLogger()} may be wired up. */
	private static final Logger ASYNC_LOG = Logger.getLogger(RestContext.class.getName() + ".async");

	/**
	 * Whether framework memoizers and operation/child contexts should be force-initialized during constructor execution;
	 * resolved from {@code @Rest(eagerInit)}.
	 */
	private final Memoizer<Boolean> eagerInit = memoizer(() ->
		mergeReplacedBooleanAttribute(PROPERTY_eagerInit, defaultEagerInit));

	/**
	 * Annotation + env-driven component of the lazy-children flag.
	 *
	 * <p>
	 * Reads the {@link Rest#lazyChildren() @Rest(lazyChildren)} annotation chain and falls back to the
	 * {@code RestContext.lazyChildren} env-driven default.  The programmatic
	 * {@link Builder#lazyChildInit(boolean)} knob is applied in {@link #isLazyChildren()} instead of here
	 * because blank-final-field rules prevent the memoizer lambda from safely capturing {@link #builder}.
	 */
	private final Memoizer<Boolean> lazyChildrenAnnotation = memoizer(() ->
		mergeReplacedBooleanAttribute(PROPERTY_lazyChildren, defaultLazyChildren));

	/**
	 * The request header used for client-version matching; resolved from {@code @Rest(clientVersionHeader)},
	 * default {@code "Client-Version"}.
	 */
	private final Memoizer<String> clientVersionHeader = memoizer(() ->
		mergeReplacedStringAttribute(PROPERTY_clientVersionHeader, defaultClientVersionHeader));

	/**
	 * The {@link UriRelativity} strategy for URI resolution in this resource.
	 *
	 * <p>
	 * Resolved from {@code @Rest(uriRelativity)} annotations (most-derived non-blank wins), with env-var
	 * fallback. Defaults to {@link UriRelativity#RESOURCE}.
	 */
	private final Memoizer<UriRelativity> uriRelativity = memoizer(() -> {
		var v = new AtomicReference<>(
			parseEnumConstant(UriRelativity.class, resolve(emptyIfNull(defaultUriRelativity)))
				.orElse(UriRelativity.RESOURCE)
		);
		restAnnotationsForPropertySortedByRank(PROPERTY_uriRelativity).forEach(ai -> ai.getString(PROPERTY_uriRelativity).filter(StringUtils::isNotBlank).ifPresent(s ->
			parseEnumConstant(UriRelativity.class, resolve(s)).ifPresent(v::set)
		));
		return v.get();
	});

	/**
	 * The URI authority (scheme + host + port) override for this resource.
	 *
	 * <p>
	 * Resolved from {@code @Rest(uriAuthority)} annotations and env-var; inherited from parent when not
	 * blocked by {@code noInherit}. {@code null} means no override.
	 */
	private final Memoizer<String> uriAuthority = memoizer(() -> {
		String local = mergeReplacedStringAttribute(PROPERTY_uriAuthority, defaultUriAuthority.orElse(null));
		if (nn(local))
			return local;
		var pc = parentContext();
		return isInherited(PROPERTY_uriAuthority) && pc != null ? pc.getUriAuthority() : null;
	});

	/**
	 * The URI context path override for this resource.
	 *
	 * <p>
	 * Resolved from {@code @Rest(uriContext)} annotations and env-var; inherited from parent when not
	 * blocked by {@code noInherit}. {@code null} means no override.
	 */
	private final Memoizer<String> uriContext = memoizer(() -> {
		String local = mergeReplacedStringAttribute(PROPERTY_uriContext, defaultUriContext.orElse(null));
		if (nn(local))
			return local;
		var pc = parentContext();
		return isInherited(PROPERTY_uriContext) && pc != null ? pc.getUriContext() : null;
	});

	/**
	 * The {@link UriResolution} strategy for URI resolution in this resource.
	 *
	 * <p>
	 * Resolved from {@code @Rest(uriResolution)} annotations (most-derived non-blank wins), with env-var
	 * fallback. Defaults to {@link UriResolution#ROOT_RELATIVE}.
	 */
	private final Memoizer<UriResolution> uriResolution = memoizer(() -> {
		var v = new AtomicReference<>(
			parseEnumConstant(UriResolution.class, resolve(emptyIfNull(defaultUriResolution)))
				.orElse(UriResolution.ROOT_RELATIVE)
		);
		restAnnotationsForPropertySortedByRank(PROPERTY_uriResolution).forEach(ai -> ai.getString(PROPERTY_uriResolution).filter(StringUtils::isNotBlank).ifPresent(s ->
			parseEnumConstant(UriResolution.class, resolve(s)).ifPresent(v::set)
		));
		return v.get();
	});

	/**
	 * Property names that describe how a class is mounted on the URL tree, and therefore must NOT inherit from a
	 * parent context when walking annotations on a {@linkplain #isMixinContext() mixin sub-context}.
	 *
	 * <p>
	 * These properties are intentionally host-only:
	 * <ul>
	 * 	<li>{@code path} / {@code paths} &mdash; top-level mount declarations only meaningful for the class registered
	 * 		with the servlet container.  A mixin sub-context inheriting the host's mount paths could cause
	 * 		nonsensical routing decisions.
	 * 	<li>{@code mixins} &mdash; the host owns the mixin discovery chain; a mixin sub-context inheriting the host's
	 * 		mixin list would loop or duplicate route registration.
	 * 	<li>{@code children} &mdash; child sub-resources are tied to the host's URL namespace, not the mixin's.
	 * </ul>
	 *
	 * <p>
	 * The host's {@link RestContext} reads {@code path}/{@code paths} from its own annotation chain only (via
	 * {@link #getRestAnnotations()} rather than {@link #getRestAnnotationsForProperty(String)}); this allowlist
	 * preserves that invariant when a mixin sub-context walks annotations for these properties.
	 */
	private static final Set<String> HOST_ONLY_PROPERTIES = Set.of(
		PROPERTY_path, PROPERTY_paths, PROPERTY_mixins, "children"
	);

	/**
	 * Returns the {@link Rest} annotations on the resource class hierarchy for the specified property,
	 * in <b>parent-to-child</b> order, with {@code noInherit} cutoff applied.
	 *
	 * <p>
	 * Used by both this class and {@link RestOpContext} to walk the class-level annotation chain when
	 * computing memoized op-level settings that inherit from class-level {@code @Rest(...)} attributes.
	 *
	 * <p>
	 * When this context is a {@linkplain #isMixinContext() mixin sub-context}, the parent context's annotation
	 * chain is prepended to the mixin's own &mdash; serializers, parsers, encoders, converters, response
	 * processors, guards, callLogger, debugEnablement, messages, and other contribution lists inherit from the
	 * host first, with the mixin's contributions appended.  The local {@code @Rest(noInherit)} on the mixin
	 * cuts off the parent walk for any specific property; the {@link #HOST_ONLY_PROPERTIES} allowlist
	 * unconditionally skips the parent walk for properties whose semantics are host-only (mount paths, the
	 * mixin discovery chain, and child sub-resources).
	 *
	 * @param name The annotation property name (e.g. {@code "converters"}, {@code "guards"}).
	 * @return A stream of {@link AnnotationInfo} entries in parent-to-child order, never {@code null}.
	 */
	Stream<AnnotationInfo<Rest>> getRestAnnotationsForProperty(String name) {
		var localAnnotations = getRestAnnotations();
		List<AnnotationInfo<Rest>> annotations;
		if (isMixinContextField() && parentContext != null && !HOST_ONLY_PROPERTIES.contains(name) && !noInherit.get().contains(name)) {
			var combined = new ArrayList<AnnotationInfo<Rest>>(localAnnotations.size() + parentContext.getRestAnnotations().size());
			// Local (child-to-parent) annotations first; then parent's (child-to-parent).
			// Since the final stream is reversed by rstream(), the combined order becomes:
			//   parent's parent-to-child, then mixin's parent-to-child — i.e. host annotations come first, then mixin.
			combined.addAll(localAnnotations);
			combined.addAll(parentContext.getRestAnnotations());
			annotations = combined;
		} else {
			annotations = localAnnotations;
		}
		var cutoff = annotations.size();
		for (var i = 0; i < annotations.size(); i++) {
			if (resolveCdl(annotations.get(i).getStringArray(PROPERTY_noInherit)).anyMatch(name::equalsIgnoreCase)) {
				cutoff = i + 1;
				break;
			}
		}
		return rstream(annotations.subList(0, cutoff));
	}

	/**
	 * For a {@linkplain #isMixinContext() mixin sub-context}, returns the <b>host</b> resource class's
	 * class-level annotation infos (host class chain, in parent-to-child order) so a mixin operation can
	 * inherit the host's class-level {@link org.apache.juneau.marshall.ContextApply @ContextApply} config
	 * (e.g. {@link org.apache.juneau.marshall.html.HtmlDocConfig @HtmlDocConfig},
	 * {@link org.apache.juneau.marshall.serializer.SerializerConfig @SerializerConfig}).
	 *
	 * <p>
	 * Used by {@link RestOpContext.Builder} to prepend the host's class-level config annotations <i>ahead of</i>
	 * the mixin class's own class annotations in the op's annotation work-list, so the effective precedence is
	 * method &gt; mixin-class &gt; host-class.  This is the page-decoration counterpart of the context-level
	 * inheritance walk in {@link #getRestAnnotationsForProperty(String)}: that walk inherits {@code @Rest}
	 * properties (serializers, parsers, guards, ...) from the host, while this method inherits standalone
	 * class-level config annotations that are not {@code @Rest} attributes.
	 *
	 * <p>
	 * Inheritance is automatic for every mixin sub-context.  A mixin can opt out of inheriting a specific host
	 * config annotation by naming its annotation type in {@link Rest#noInherit() @Rest(noInherit)} on the mixin
	 * class (e.g. {@code @Rest(noInherit={"HtmlDocConfig"})}), reusing the same {@code noInherit} resolution that
	 * gates the context-level walk so the opt-out is uniform.  For non-mixin contexts (top-level resources and
	 * child resources) this returns an empty stream &mdash; only mixin ops inherit from a composition host.
	 *
	 * @param ap The annotation provider to resolve the host class chain with.
	 * @return The host class chain's annotation infos in parent-to-child order, or an empty stream when this is
	 * 	not a mixin sub-context (or has no parent).
	 */
	Stream<AnnotationInfo<?>> getInheritedHostClassAnnotations(AnnotationProvider ap) {
		if (! isMixinContextField() || parentContext == null)
			return Stream.empty();
		var blocked = noInherit.get();
		return rstream(ap.find(ClassInfo.of(parentContext.getResourceClass()), SELF, PARENTS))
			.filter(ai -> ! blocked.contains(ai.annotationType().getSimpleName()));
	}

	/**
	 * Returns all {@link Rest} annotations on the resource class hierarchy, in child-to-parent order.
	 *
	 * @return An unmodifiable list of {@link AnnotationInfo} for {@link Rest}, never {@code null}.
	 */
	protected List<AnnotationInfo<Rest>> getRestAnnotations() {
		return restAnnotations.get();
	}

	/**
	 * Returns the nearest {@link Rest} annotation on this resource.
	 *
	 * @return An {@link Optional} containing the first (most-derived) {@link Rest} {@link AnnotationInfo}.
	 */
	protected Optional<AnnotationInfo<Rest>> getRestAnnotation() {
		return getRestAnnotations().stream().findFirst();
	}

	/**
	 * Returns the {@link Rest} annotations for this resource in parent-to-child (top-down) order.
	 *
	 * <p>
	 * This is the reverse of {@link #getRestAnnotations()}: most-ancestor annotation first,
	 * most-derived last. Use this when annotation values should accumulate in inheritance order
	 * (e.g. parent values are applied first and child values append or override them).
	 *
	 * @return An unmodifiable list of {@link AnnotationInfo} for {@link Rest}, never {@code null}.
	 */
	protected List<AnnotationInfo<Rest>> getRestAnnotationsTopDown() {
		return restAnnotationsTopDown.get();
	}

	/**
	 * Returns {@code true} if values for the given annotation attribute may be inherited from {@link #parentContext}.
	 *
	 * <p>
	 * Inheritance is blocked when the nearest {@code @Rest(noInherit)} lists the property name (case-insensitive).
	 *
	 * @param property The annotation attribute name (e.g. {@code "allowedSerializerOptions"}).
	 * @return {@code true} if parent values should be included.
	 */
	protected boolean isInherited(String property) {
		return RestContext.this.parentContext != null && !noInherit.get().contains(property);
	}

	/**
	 * Returns {@code true} if the local {@code @Rest(noInherit=...)} list contains {@code property} as a literal token.
	 *
	 * <p>
	 * Unlike the {@link #noInherit} memoizer this performs a raw, SVL-free, comma-split check on the local
	 * {@code @Rest} annotation. It exists for callers that are themselves on the dependency chain of the
	 * {@code varResolver} memoizer (e.g. the {@code messages} memoizer) — those callers can't reach the
	 * regular {@link #noInherit} memoizer without inducing a cycle, because the regular path runs
	 * {@code resolveCdl(...)} → {@link #getVarResolver()} → {@link #getMessages()} → back to messages.
	 *
	 * @param property The property name (e.g. {@code "messages"}). Case-insensitive comparison.
	 * @return {@code true} if {@code property} is named as a literal token in the local {@code @Rest(noInherit=...)}.
	 */
	private boolean isNoInheritLiteral(String property) {
		return getRestAnnotation()
			.map(x -> x.getStringArray("noInherit").orElse(StringUtils.EMPTY_STRING_ARRAY))
			.stream()
			.flatMap(Arrays::stream)
			.filter(Objects::nonNull)
			.flatMap(s -> StringUtils.split(s, ',').stream())
			.map(String::trim)
			.filter(StringUtils::isNotBlank)
			.anyMatch(property::equalsIgnoreCase);
	}

	/**
	 * Resolves comma-delimited annotation values with SVL variable substitution.
	 *
	 * @param values Raw annotation attribute values.
	 * @return A stream of trimmed, non-blank tokens.
	 */
	private Stream<String> resolveCdl(String...values) {
		if (values == null || values.length == 0)
			return Stream.empty();
		return Arrays.stream(values)
			.filter(Objects::nonNull)
			.map(this::resolve)
			.map(StringUtils::split)
			.flatMap(Collection::stream)
			.map(String::trim)
			.filter(StringUtils::isNotBlank);
	}

	private Stream<String> resolveCdl(Optional<String[]> values) {
		return values.isEmpty() ? Stream.empty() : resolveCdl(values.get());
	}

	/**
	 * Resolves SVL variables in the given string.
	 *
	 * @param s The raw string. Can be {@code null}.
	 * @return The resolved string.
	 */
	protected String resolve(String s) {
		return getVarResolver().resolve(s);
	}

	/**
	 * Returns the parser session-option keys allowed for this resource.
	 *
	 * @return An unmodifiable case-insensitive sorted set, never {@code null}.
	 */
	public SortedSet<String> getAllowedParserOptions() {
		return allowedParserOptions.get();
	}

	/**
	 * Returns the serializer session-option keys allowed for this resource.
	 *
	 * @return An unmodifiable case-insensitive sorted set, never {@code null}.
	 */
	public SortedSet<String> getAllowedSerializerOptions() {
		return allowedSerializerOptions.get();
	}

	/**
	 * Called during servlet destruction to invoke all {@link RestDestroy} methods.
	 *
	 * <p>
	 * Fires this context's own {@code @RestDestroy} methods first, then walks any mixin sub-contexts
	 * (host destroys before each mixin's), then any {@code @Rest(children=...)} sub-resources, then closes
	 * the bean store. Mixin sub-contexts recursively destroy themselves via the same path; flat-inheritance
	 * guarantees no further mixin descent occurs from inside a mixin sub-context.
	 */
	public void destroy() {
		for (var x : destroyInvokerPair.get().invokers) {
			try {
				x.invoke(beanStore, getResource());
			} catch (Exception e) {
				getLogger().log(Level.WARNING, unwrap(e), () -> String.format("Error occurred invoking servlet-destroy method '%s'.", x.getFullName()));
			}
		}
		if (!isMixinContext) {
			for (var mctx : mixinContexts.get().values()) {
				try {
					mctx.destroy();
				} catch (Exception e) {
					getLogger().log(Level.WARNING, unwrap(e), () -> String.format("Error occurred destroying mixin sub-context '%s'.", mctx.getResourceClass().getName()));
				}
			}
		}
		var childrenRef = getRestChildren();
		if (nn(childrenRef))
			childrenRef.destroy();
		try {
			beanStore.close();
		} catch (Exception e) {
			getLogger().log(Level.WARNING, unwrap(e), () -> "Error occurred closing bean store.");
		}
	}

	/**
	 * The main service method.
	 *
	 * <p>
	 * Subclasses can optionally override this method if they want to tailor the behavior of requests.
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * 	<br>Note that this bean may not be the same bean used during initialization as it may have been replaced at runtime.
	 * @param r1 The incoming HTTP servlet request object.
	 * @param r2 The incoming HTTP servlet response object.
	 * @throws ServletException General servlet exception.
	 * @throws IOException Thrown by underlying stream.
	 */
	@SuppressWarnings({
		"java:S3776" // Cognitive complexity acceptable for request execution logic
	})
	public void execute(Object resource, HttpServletRequest r1, HttpServletResponse r2) throws ServletException, IOException {

		// Must be careful not to bleed thread-locals.
		if (nn(localSession.get()))
			LOG.warning("WARNING:  Thread-local call object was not cleaned up from previous request.  {}, thread=[{}]", this, Thread.currentThread().getName());

		RestSession.Builder sb = createSession().resource(resource).req(r1).res(r2).logger(getCallLogger());

		try {

			if (nn(initException))
				throw initException;

			// If the resource path contains variables (e.g. @Rest(path="/f/{a}/{b}"), then we want to resolve
			// those variables and push the servletPath to include the resolved variables.  The new pathInfo will be
			// the remainder after the new servletPath.
			// Only do this for the top-level resource because the logic for child resources are processed next.
			if (pathMatcher.hasVars() && parentContext == null) {
				var sp = sb.req().getServletPath();
				var pi = sb.getPathInfoUndecoded();
				var upi2 = UrlPath.of(pi == null ? sp : sp + pi);
				var uppm = pathMatcher.match(upi2);
			if (nn(uppm) && ! uppm.hasEmptyVars()) {
				sb.pathVars(uppm.getVars());
				var pathInfo = opt(validatePathInfo(nullIfEmpty(urlDecode(uppm.getSuffix())))).orElse("\u0000");
				var servletPath = validateServletPath(uppm.getPrefix());
				sb.req(new HttpServletRequestWrapper(sb.req()) {
					@Override
					public String getPathInfo() {
						return pathInfo.charAt(0) == (char)0 ? null : pathInfo;
					}
					@Override
					public String getServletPath() {
						return servletPath;
					}
				});
			} else {
					var call = sb.build();
					call.debug(isDebug(call)).status(SC_NOT_FOUND).finish();
					return;
				}
			}

			// If this resource has child resources, try to recursively call them.
			var childMatch = getRestChildren().findMatch(sb);
			if (childMatch.isPresent()) {
				var uppm = childMatch.get().getPathMatch();
				var rc = childMatch.get().getChildContext();
				if (! uppm.hasEmptyVars()) {
					sb.pathVars(uppm.getVars());
					var pathInfo = opt(validatePathInfo(nullIfEmpty(urlDecode(uppm.getSuffix())))).orElse("\u0000");
					var servletPath = validateServletPath(sb.req().getServletPath() + uppm.getPrefix());
					var childRequest = new HttpServletRequestWrapper(sb.req()) {
						@Override
						public String getPathInfo() {
							return pathInfo.charAt(0) == (char)0 ? null : pathInfo;
						}
						@Override
						public String getServletPath() {
							return servletPath;
						}
					};
					rc.execute(rc.getResource(), childRequest, sb.res());
				} else {
					var call = sb.build();
					call.debug(isDebug(call)).status(SC_NOT_FOUND).finish();
				}
				return;
			}

		} catch (Exception e) {
			handleError(sb.build(), convertThrowable(e));
		}

		var s = sb.build();

		try {
			localSession.set(s);
			s.debug(isDebug(s));
			startCall(s);
			s.run();
		} catch (Exception e) {
			handleError(s, convertThrowable(e));
		} finally {
			try {
				s.finish();
				endCall(s);
				// For mixin endpoints, dual-fire endCall on the mixin sub-context after the host's so host
				// hooks fire first, then the mixin's. Skip when no operation was resolved (e.g. 404 paths).
				var os = s.getOpSessionOrNull();
				if (os != null) {
					var opRestContext = os.getContext().getContext();
					if (opRestContext.isMixinContext)
						opRestContext.endCall(s);
				}
			} finally {
				localSession.remove();
			}
		}
	}

	/**
	 * Allowed header URL parameters.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='ja'>{@link Rest#allowedHeaderParams}
	 * </ul>
	 *
	 * @return
	 * 	The header names allowed to be passed as URL parameters.
	 * 	<br>The set is case-insensitive ordered and unmodifiable.
	 */
	public Set<String> getAllowedHeaderParams() { return allowedHeaderParams.get(); }

	/**
	 * Allowed method headers.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='ja'>{@link Rest#allowedMethodHeaders}
	 * </ul>
	 *
	 * @return
	 * 	The method names allowed to be passed as <c>X-Method</c> headers.
	 * 	<br>The set is case-insensitive ordered and unmodifiable.
	 */
	public Set<String> getAllowedMethodHeaders() { return allowedMethodHeaders.get(); }

	/**
	 * Allowed method URL parameters.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='ja'>{@link Rest#allowedMethodParams}
	 * </ul>
	 *
	 * @return
	 * 	The method names allowed to be passed as <c>method</c> URL parameters.
	 * 	<br>The set is case-insensitive ordered and unmodifiable.
	 */
	public Set<String> getAllowedMethodParams() { return allowedMethodParams.get(); }

	/**
	 * Returns the annotations applied to this context.
	 *
	 * @return The annotations applied to this context.
	 */
	public AnnotationWorkList getAnnotations() { return annotationWork; }

	/**
	 * Returns the bean context associated with this context.
	 *
	 * @return The bean store associated with this context.
	 */
	public MarshallingContext getMarshallingContext() { return beanStore.getBean(MarshallingContext.class).orElse(null); }

	MarshallingContext.Builder         getBeanContextBuilder()          { return beanContextBuilder.get(); }
	EncoderSet.Builder          getEncodersBuilder()             { return encodersBuilder.get(); }
	JsonSchemaGenerator.Builder getJsonSchemaGeneratorBuilder()  { return jsonSchemaGeneratorBuilder.get(); }
	ParserSet.Builder           getParsersBuilder()              { return parsersBuilder.get(); }
	HttpPartParser.Creator      getPartParserCreator()           { return partParserCreator.get(); }
	HttpPartSerializer.Creator  getPartSerializerCreator()       { return partSerializerCreator.get(); }
	SerializerSet.Builder       getSerializersBuilder()          { return serializersBuilder.get(); }

	/**
	 * Returns the bean store associated with this context.
	 *
	 * <p>
	 * The bean store is used for instantiating child resource classes.
	 *
	 * @return The resource resolver associated with this context.
	 */
	public WritableBeanStore getBeanStore() { return beanStore; }

	/**
	 * Returns the builder that created this context.
	 *
	 * @return The builder that created this context.
	 */
	public ServletConfig getBuilder() { return builder; }

	/**
	 * Returns the call logger to use for this resource.
	 *
	 * <p>
	 * The default call logger is {@link BasicCallLogger}. Override via {@link Rest#callLogger() @Rest(callLogger)}
	 * on the resource class, by registering a {@link CallLogger} bean in the bean store, or by declaring a
	 * {@link Bean @Bean}-annotated static method on the resource class:
	 * <p class='bjava'>
	 * 	<ja>@Bean</ja> <jk>public static</jk> CallLogger myCallLogger(<i>&lt;args&gt;</i>) {...}
	 * </p>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='ja'>{@link Rest#callLogger}
	 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerLoggingAndDebugging">Logging / Debugging</a>
	 * </ul>
	 *
	 * @return
	 * 	The call logger to use for this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public CallLogger getCallLogger() { return beanStore.getBean(CallLogger.class).orElse(null); }

	/**
	 * Returns the name of the client version header name used by this resource.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='ja'>{@link Rest#clientVersionHeader}
	 * </ul>
	 *
	 * @return
	 * 	The name of the client version header used by this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public String getClientVersionHeader() { return clientVersionHeader.get(); }

	/**
	 * Returns the config file associated with this servlet.
	 *
	 * <p>
	 * The config file is identified via {@link Rest#config()}.
	 *
	 * @return
	 * 	The resolving config file associated with this servlet.
	 * 	<br>Never <jk>null</jk>.
	 */
	public Config getConfig() { return beanStore.getBean(Config.class).orElse(null); }

	/**
	 * Returns the explicit list of supported content types for this resource.
	 *
	 * <p>
	 * Consists of the media types for consumption common to all operations on this class.
	 *
	 * <p>
	 * Derived from {@link Rest#consumes()} across the resource annotation chain.
	 *
	 * @return
	 * 	An unmodifiable list of supported <c>Content-Type</c> header values for this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public List<MediaType> getConsumes() { return consumes.get(); }

	/**
	 * Returns the debug enablement bean for this context.
	 *
	 * @return The debug enablement bean for this context.
	 */
	public DebugEnablement getDebugEnablement() { return beanStore.getBean(DebugEnablement.class).orElse(null); }

	/**
	 * Returns the debug configuration bean for this context.
	 *
	 * @return The debug configuration bean for this context.
	 */
	public DebugConfig getDebugConfig() { return beanStore.getBean(DebugConfig.class).orElse(null); }

	/**
	 * Returns the default request attributes for this resource.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='ja'>{@link Rest#defaultRequestAttributes()}
	 * </ul>
	 *
	 * @return
	 * 	The default request headers for this resource in an unmodifiable list.
	 * 	<br>Never <jk>null</jk>.
	 */
	public NamedAttributeMap getDefaultRequestAttributes() { return beanStore.getBean(NamedAttributeMap.class, PROP_defaultRequestAttributes).orElse(null); }

	/**
	 * Returns the default request headers for this resource.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='ja'>{@link Rest#defaultRequestHeaders()}
	 * </ul>
	 *
	 * @return
	 * 	The default request headers for this resource in an unmodifiable list.
	 * 	<br>Never <jk>null</jk>.
	 */
	public HttpHeaderList getDefaultRequestHeaders() { return beanStore.getBean(HttpHeaderList.class, PROP_defaultRequestHeaders).orElse(null); }

	/**
	 * Returns the default response headers for this resource.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='ja'>{@link Rest#defaultResponseHeaders()}
	 * </ul>
	 *
	 * @return
	 * 	The default response headers for this resource in an unmodifiable list.
	 * 	<br>Never <jk>null</jk>.
	 */
	public HttpHeaderList getDefaultResponseHeaders() { return beanStore.getBean(HttpHeaderList.class, PROP_defaultResponseHeaders).orElse(null); }

	/**
	 * Returns the encoders associated with this context.
	 *
	 * @return The encoders associated with this context.
	 */
	public EncoderSet getEncoders() { return beanStore.getBean(EncoderSet.class).orElse(null); }

	/**
	 * Returns the path for this resource as defined by the {@link Rest#path() @Rest(path)} annotation or
	 * {@link Rest#path()} annotation attribute concatenated with those on all parent classes.
	 *
	 * <p>
	 * If path is not specified, returns <js>""</js>.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='ja'>{@link Rest#path()}
	 * </ul>
	 *
	 * @return The full path.
	 */
	public String getFullPath() { return fullPath; }

	/**
	 * Returns the JSON-Schema generator associated with this resource.
	 *
	 * @return
	 * 	The HTTP-part serializer associated with this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public JsonSchemaGenerator getJsonSchemaGenerator() { return beanStore.getBean(JsonSchemaGenerator.class).orElse(null); }

	/**
	 * Returns the HTTP call for the current request.
	 *
	 * @return The HTTP call for the current request, never <jk>null</jk>?
	 * @throws InternalServerError If no active request exists on the current thread.
	 */
	public RestSession getLocalSession() {
		var rc = localSession.get();
		if (rc == null)
			throw new InternalServerError("No active request on current thread.");
		return rc;
	}

	/**
	 * Returns the logger associated with this context.
	 *
	 * @return
	 * 	The logger for this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public Logger getLogger() { return beanStore.getBean(Logger.class).orElse(null); }

	/**
	 * Returns the resource bundle used by this resource.
	 *
	 * @return
	 * 	The resource bundle for this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public Messages getMessages() { return beanStore.getBean(Messages.class).orElse(null); }

	/**
	 * Returns the timing statistics on all method executions on this class.
	 *
	 * @return The timing statistics on all method executions on this class.
	 */
	public MethodExecStore getMethodExecStore() { return beanStore.getBean(MethodExecStore.class).orElse(null); }

	/**
	 * Returns the parsers associated with this context.
	 *
	 * @return The parsers associated with this context.
	 */
	public ParserSet getParsers() { return beanStore.getBean(ParserSet.class).orElse(null); }

	/**
	 * Returns the HTTP-part parser associated with this resource.
	 *
	 * @return
	 * 	The HTTP-part parser associated with this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public HttpPartParser getPartParser() { return beanStore.getBean(HttpPartParser.class).orElse(null); }

	/**
	 * Returns the HTTP-part serializer associated with this resource.
	 *
	 * @return
	 * 	The HTTP-part serializer associated with this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public HttpPartSerializer getPartSerializer() { return beanStore.getBean(HttpPartSerializer.class).orElse(null); }

	/**
	 * Returns the path for this resource as defined by the {@link Rest#path() @Rest(path)} annotation or
	 * {@link Rest#path()} annotation attribute.
	 *
	 * <p>
	 * If path is not specified, returns <js>""</js>.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='ja'>{@link Rest#path()}
	 * </ul>
	 *
	 * @return The servlet path.
	 */
	public String getPath() { return path; }

	/**
	 * Returns the resolved top-level mount paths for this resource.
	 *
	 * <p>
	 * Computed once during {@link RestContext} construction by walking the runtime-override resolution chain
	 * (highest precedence first):
	 * <ol>
	 * 	<li><b>Programmatic</b> &mdash; {@link Builder#paths(String...)} setter (or {@link Args#paths()} record
	 * 		field). Highest precedence; an empty array (not {@code null}) explicitly clears the mount list.
	 * 	<li><b>Getter</b> &mdash; the resource's {@code getPaths()} virtual method (defined on
	 * 		{@code RestServlet} and {@code RestResource}). {@code null} return inherits; non-{@code null} (including
	 * 		empty array) wins over the annotation default.
	 * 	<li><b>Annotation default</b> &mdash; {@link Rest#paths()}, walked top-down across the resource-class
	 * 		hierarchy with the most-derived non-empty value winning. Each element is
	 * 		<a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerSvlVariables">SVL</a>-resolved
	 * 		against the bootstrap {@link org.apache.juneau.commons.svl.VarResolver VarResolver}, then comma-split (trim
	 * 		each piece, drop empties).  A single element like {@code "$C{health.paths}"} can therefore expand
	 * 		to multiple mount paths.
	 * </ol>
	 *
	 * <p>
	 * The resolved array is what hosting runtimes &mdash; Jetty's auto-discovery flow in
	 * {@code JettyServerComponent}, and Spring Boot's {@code ServletRegistrationBean} wiring &mdash; should
	 * consume when mounting the resource at the top level.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Returns an empty array (not {@code null}) when none of the rungs resolve. Hosting runtimes interpret
	 * 		an empty array as &quot;no top-level mounts&quot; and surface a clear error naming the resource.
	 * 	<li class='note'>
	 * 		The scalar {@link #getPath()} accessor remains orthogonal to this method.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='ja'>{@link Rest#paths()}
	 * </ul>
	 *
	 * @return The resolved mount paths (never {@code null}; possibly empty).
	 * @since 10.0.0
	 */
	public String[] getPaths() { return paths; }

	/**
	 * Returns the path matcher for this context.
	 *
	 * @return The path matcher for this context.
	 */
	public UrlPathMatcher getPathMatcher() { return pathMatcher; }

	/**
	 * Returns the explicit list of supported accept types for this resource.
	 *
	 * <p>
	 * Consists of the media types for production common to all operations on this class.
	 *
	 * <p>
	 * Derived from {@link Rest#produces()} across the resource annotation chain.
	 *
	 * @return
	 * 	An unmodifiable list of supported <c>Accept</c> header values for this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public List<MediaType> getProduces() { return produces.get(); }

	/**
	 * Returns the resource object.
	 *
	 * <p>
	 * This is the instance of the class annotated with the {@link Rest @Rest} annotation, usually
	 * an instance of {@link RestServlet}.
	 *
	 * @return
	 * 	The resource object.
	 * 	<br>Never <jk>null</jk>.
	 */
	public Object getResource() { return resource.get(); }

	/**
	 * Returns the resource class type.
	 *
	 * @return The resource class type.
	 */
	public Class<?> getResourceClass() { return resourceClass; }

	/**
	 * Returns the response processors registered on this resource.
	 *
	 * <p>
	 * Returned in the order they're invoked by {@link #processResponse(RestOpSession)}.
	 * The returned array is the live backing array — callers must not mutate it.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='ja'>{@link Rest#responseProcessors}
	 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ResponseProcessors">Response Processors</a>
	 * </ul>
	 *
	 * @return
	 * 	The response processors for this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public ResponseProcessor[] getResponseProcessors() { return beanStore.getBean(ResponseProcessor[].class).orElse(null); }

	/**
	 * Returns the {@link RestOpArg} classes registered on this resource.
	 *
	 * <p>
	 * Per-op {@link RestOpArg} instances are resolved separately at per-op setup via the bean store
	 * (see {@link #findRestOperationArgs(Method, BeanStore)}); this getter returns the class list
	 * that drives that resolution.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='ja'>{@link Rest#restOpArgs}
	 * </ul>
	 *
	 * @return
	 * 	The REST-op-arg classes for this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public Class<? extends RestOpArg>[] getRestOpArgs() { return restOpArgs.get(); }

	/**
	 * Returns the child resources associated with this servlet.
	 *
	 * @return
	 * 	An unmodifiable map of child resources.
	 * 	Keys are the {@link Rest#path() @Rest(path)} annotation defined on the child resource.
	 */
	public RestChildren getRestChildren() { return beanStore.getBean(RestChildren.class).orElse(null); }

	/**
	 * Returns the REST Java methods defined in this resource.
	 *
	 * <p>
	 * These are the methods annotated with the {@link RestOp @RestOp} annotation.
	 *
	 * @return
	 * 	An unmodifiable map of Java method names to call method objects.
	 */
	public RestOperations getRestOperations() { return beanStore.getBean(RestOperations.class).orElse(null); }

	/**
	 * Returns the bootstrap bean store for this context.
	 *
	 * <p>
	 * This is the bean store inherited from the parent resource and does not include
	 * any beans added by this class.
	 *
	 * @return The bootstrap bean store for this context.
	 */
	public WritableBeanStore getBootstrapBeanStore() { return bootstrapBeanStore; }

	/**
	 * Returns the serializers associated with this context.
	 *
	 * @return The serializers associated with this context.
	 */
	public SerializerSet getSerializers() { return beanStore.getBean(SerializerSet.class).orElse(null); }

	/**
	 * Returns the servlet init parameter returned by {@link ServletConfig#getInitParameter(String)}.
	 *
	 * @param name The init parameter name.
	 * @return The servlet init parameter, or <jk>null</jk> if not found.
	 */
	public String getServletInitParameter(String name) {
		return builder.getInitParameter(name);
	}

	/**
	 * Returns the static files associated with this context.
	 *
	 * @return
	 * 	The static files for this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public StaticFiles getStaticFiles() { return beanStore.getBean(StaticFiles.class).orElse(null); }

	/**
	 * Gives access to the internal statistics on this context.
	 *
	 * @return The context statistics.
	 */
	public RestContextStats getStats() { return new RestContextStats(startTime, getMethodExecStore().getStatsByTotalTime()); }

	/**
	 * Returns the swagger for the REST resource.
	 *
	 * @param locale The locale of the swagger to return.
	 * @return The swagger as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Swagger> getSwagger(Locale locale) {
		Swagger s = swaggerCache.get(locale);
		if (s == null) {
			try {
				var provider = getSwaggerProvider();
				if (provider == null)
					return opte();
				s = provider.getSwagger(this, locale);
				if (nn(s))
					swaggerCache.put(locale, s);
			} catch (Exception e) {
				throw new InternalServerError(e);
			}
		}
		return opt(s);
	}

	/**
	 * Returns the Swagger provider used by this resource.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='ja'>{@link Rest#swaggerProvider()}
	 * </ul>
	 *
	 * @return
	 * 	The information provider for this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public SwaggerProvider getSwaggerProvider() { return beanStore.getBean(SwaggerProvider.class).orElse(null); }

	/**
	 * Returns the OpenAPI 3.1 document for the REST resource.
	 *
	 * @param locale The locale of the document to return.
	 * @return The OpenAPI document as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<OpenApi> getOpenApi(Locale locale) {
		OpenApi o = openApiCache.get(locale);
		if (o == null) {
			try {
				var provider = getOpenApiProvider();
				if (provider == null)
					return opte();
				o = provider.getOpenApi(this, locale);
				if (nn(o))
					openApiCache.put(locale, o);
			} catch (Exception e) {
				throw new InternalServerError(e);
			}
		}
		return opt(o);
	}

	/**
	 * Returns the OpenAPI 3.1 provider used by this resource.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='ja'>{@link Rest#openApiProvider()}
	 * </ul>
	 *
	 * @return
	 * 	The OpenAPI 3.1 provider for this resource.
	 * 	<br>May be <jk>null</jk>.
	 */
	public OpenApiProvider getOpenApiProvider() { return beanStore.getBean(OpenApiProvider.class).orElse(null); }

	/**
	 * Returns the stack trace database associated with this context.
	 *
	 * @return
	 * 	The stack trace database for this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public ThrownStore getThrownStore() { return beanStore.getBean(ThrownStore.class).orElse(null); }

	/**
	 * Returns the authority path of the resource.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='ja'>{@link Rest#uriAuthority()}
	 * </ul>
	 *
	 * @return
	 * 	The authority path of this resource.
	 * 	<br>If not specified, returns the context path of the ascendant resource.
	 */
	public String getUriAuthority() {
		return uriAuthority.get();
	}

	/**
	 * Returns the context path of the resource.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='ja'>{@link Rest#uriContext()}
	 * </ul>
	 *
	 * @return
	 * 	The context path of this resource.
	 * 	<br>If not specified, returns the context path of the ascendant resource.
	 */
	public String getUriContext() {
		return uriContext.get();
	}

	/**
	 * Returns the setting on how relative URIs should be interpreted as relative to.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='ja'>{@link Rest#uriRelativity()}
	 * </ul>
	 *
	 * @return
	 * 	The URI-resolution relativity setting value.
	 * 	<br>Never <jk>null</jk>.
	 */
	public UriRelativity getUriRelativity() { return uriRelativity.get(); }

	/**
	 * Returns the setting on how relative URIs should be resolved.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='ja'>{@link Rest#uriResolution()}
	 * </ul>
	 *
	 * @return
	 * 	The URI-resolution setting value.
	 * 	<br>Never <jk>null</jk>.
	 */
	public UriResolution getUriResolution() { return uriResolution.get(); }

	/**
	 * Returns the variable resolver for this servlet.
	 *
	 * <p>
	 * Variable resolvers are used to replace variables in property values.
	 * They can be nested arbitrarily deep.
	 * They can also return values that themselves contain other variables.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bjava'>
	 * 	<ja>@Rest</ja>(
	 * 		messages=<js>"nls/Messages"</js>,
	 * 		properties={
	 * 			<ja>@Property</ja>(name=<js>"title"</js>,value=<js>"$L{title}"</js>),  <jc>// Localized variable in Messages.properties</jc>
	 * 			<ja>@Property</ja>(name=<js>"javaVendor"</js>,value=<js>"$S{java.vendor,Oracle}"</js>),  <jc>// System property with default value</jc>
	 * 			<ja>@Property</ja>(name=<js>"foo"</js>,value=<js>"bar"</js>),
	 * 			<ja>@Property</ja>(name=<js>"bar"</js>,value=<js>"baz"</js>),
	 * 			<ja>@Property</ja>(name=<js>"v1"</js>,value=<js>"$R{foo}"</js>),  <jc>// Request variable.  value="bar"</jc>
	 * 			<ja>@Property</ja>(name=<js>"v1"</js>,value=<js>"$R{foo,bar}"</js>),  <jc>// Request variable.  value="bar"</jc>
	 * 		}
	 * 	)
	 * 	<jk>public class</jk> MyRestResource <jk>extends</jk> BasicRestServlet {
	 * </p>
	 *
	 * <p>
	 * A typical usage pattern involves using variables inside the {@link HtmlDocConfig @HtmlDocConfig} annotation:
	 * <p class='bjava'>
	 * 	<ja>@RestGet</ja>(<js>"/{name}/*"</js>)
	 * 	<ja>@HtmlDocConfig</ja>(
	 * 		navlinks={
	 * 			<js>"up: $R{requestParentURI}"</js>,
	 * 			<js>"api: servlet:/api"</js>,
	 * 			<js>"stats: servlet:/stats"</js>,
	 * 			<js>"editLevel: servlet:/editLevel?logger=$A{attribute.name, OFF}"</js>
	 * 		}
	 * 		header={
	 * 			<js>"&lt;h1&gt;$L{MyLocalizedPageTitle}&lt;/h1&gt;"</js>
	 * 		},
	 * 		aside={
	 * 			<js>"$F{resources/AsideText.html}"</js>
	 * 		}
	 * 	)
	 * 	<jk>public</jk> LoggerEntry getLogger(RestRequest <jv>req</jv>, <ja>@Path</ja> String <jv>name</jv>) <jk>throws</jk> Exception {
	 * </p>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerSvlVariables">SVL Variables</a>
	 * </ul>
	 *
	 * @return The var resolver in use by this resource.
	 */
	public VarResolver getVarResolver() { return beanStore.getBean(VarResolver.class).orElse(null); }

	/**
	 * Returns the bootstrap (pre-runtime) variable resolver used during context construction.
	 *
	 * <p>
	 * The bootstrap resolver has the same {@link Var} catalog as {@link #getVarResolver()} but does not have
	 * {@link Messages} or {@link Config} beans wired in — it is used to resolve annotation attribute values
	 * (e.g. <c>@Rest(messages=...)</c>) before those beans are built. Override via
	 * {@link Bean @Bean(name="bootstrapVarResolver")} on a static method of the resource class.
	 *
	 * @return The bootstrap var resolver in use by this resource.
	 */
	public VarResolver getBootstrapVarResolver() { return bootstrapVarResolver.get(); }

	/**
	 * Returns whether it's safe to pass the HTTP content as a <js>"content"</js> GET parameter.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='ja'>{@link Rest#disableContentParam()}
	 * </ul>
	 *
	 * @return <jk>true</jk> if setting is enabled.
	 */
	public boolean isAllowContentParam() { return allowContentParam.get(); }

	/**
	 * Returns whether it's safe to render stack traces in HTTP responses.
	 *
	 * @return <jk>true</jk> if setting is enabled.
	 */
	public boolean isRenderResponseStackTraces() { return renderResponseStackTraces.get(); }

	/**
	 * Returns whether this resource is opted into
	 * <a class="doclink" href="https://www.rfc-editor.org/rfc/rfc7807">RFC 7807</a>
	 * {@code application/problem+json} error responses.
	 *
	 * <p>
	 * When {@code true}:
	 * <ul class='spaced-list'>
	 * 	<li>Thrown {@code BasicHttpException}s are serialized as {@code application/problem+json} via
	 * 		{@code RestContext.handleError} &mdash; regardless of the client's {@code Accept} header.
	 * 	<li>{@code @RestOp} methods that return a {@link org.apache.juneau.bean.rfc7807.Problem} (or throw a
	 * 		{@link org.apache.juneau.bean.rfc7807.ProblemException}) are serialized by
	 * 		{@link org.apache.juneau.rest.server.processor.ProblemDetailsProcessor}, but only when the client's
	 * 		{@code Accept} header matches {@code application/problem+json} (or {@code *&#47;*}).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='ja'>{@link Rest#problemDetails()}
	 * </ul>
	 *
	 * @return <jk>true</jk> if RFC 7807 problem-details responses are enabled on this resource.
	 */
	public boolean isProblemDetails() { return problemDetails.get(); }

	/**
	 * Returns whether the resource opted into per-request virtual-thread dispatch (Java 21+) via
	 * {@code @Rest(virtualThreads=true)}.
	 *
	 * <p>
	 * The flag is honored only when {@link #getVirtualThreadExecutor()} returns a non-{@code null} executor; on
	 * runtimes older than Java 21 the executor is {@code null} and the flag is logged once at context init.
	 *
	 * @return <jk>true</jk> if virtual-thread dispatch is configured on this resource.
	 */
	public boolean isVirtualThreadsEnabled() { return virtualThreadsEnabled.get(); }

	/**
	 * Returns the raw resource-level observability attribute value from {@code @Rest(observability)}.
	 *
	 * <ul class='values'>
	 * 	<li>{@code "true"} &mdash; strict opt-in: all operations on this resource require a wired backend.
	 * 	<li>{@code "false"} &mdash; explicit opt-out: the observability block is disabled for all operations.
	 * 	<li>{@code null} / empty &mdash; default (inherit / no-op fallback) behavior.
	 * </ul>
	 *
	 * <p>
	 * Per-operation overrides from {@link org.apache.juneau.rest.server.RestOp#observability()} (and verb
	 * annotations) take precedence over this value; they are resolved in {@link RestOpContext}.
	 *
	 * @return The resolved {@code @Rest(observability)} string, or {@code null} when not set.
	 */
	public String getObservabilityAttribute() { return observabilityAttribute.get(); }

	/**
	 * Returns whether the resource explicitly opted out of observability via {@code @Rest(observability="false")}.
	 *
	 * @return <jk>true</jk> when the resource-level observability is disabled.
	 */
	public boolean isObservabilityDisabled() {
		var v = observabilityAttribute.get();
		return v != null && v.equalsIgnoreCase("false");
	}

	/**
	 * Returns whether the server writes W3C {@code traceparent} / {@code tracestate} response headers when a
	 * {@link TracerHook} is active on the request.
	 *
	 * <p>
	 * Defaults to {@code true} (on-when-tracer); resolved from the {@code RestContext.responseTraceparent}
	 * env-driven default. When {@code true}, a {@link TraceContextResponseProcessor} is registered in the
	 * response-processor chain &mdash; it writes the headers only when a non-no-op {@code TracerHook} stashed a
	 * trace context for the request, so it stays zero-cost on the no-tracer path. When {@code false}, the
	 * processor is not registered at all.
	 *
	 * @return <jk>true</jk> if trace-context response-header propagation is enabled on this resource.
	 */
	public boolean isResponseTraceparent() { return defaultResponseTraceparent; }

	/**
	 * Returns whether SLF4J MDC propagation is enabled for {@link java.util.concurrent.CompletableFuture}
	 * completion threads on this resource.
	 *
	 * <p>
	 * Defaults to {@code true}; resolved first from the programmatic {@link Builder#mdcAsyncPropagation(boolean)}
	 * override (if set), then from the {@code RestContext.mdcAsyncPropagation} env-driven default.
	 * When {@code true}, {@link org.apache.juneau.rest.server.processor.MdcAsyncListener} wraps the
	 * {@link java.util.concurrent.CompletionStage#whenComplete(java.util.function.BiConsumer) whenComplete} callback
	 * registered by {@link org.apache.juneau.rest.server.processor.AsyncResponseProcessor} so the completion thread sees
	 * the request thread's MDC context. When {@code false}, no MDC work is done.
	 *
	 * @return <jk>true</jk> if MDC async propagation is enabled on this resource.
	 */
	public boolean isMdcAsyncPropagation() {
		return builder.mdcAsyncPropagation != null ? builder.mdcAsyncPropagation : defaultMdcAsyncPropagation;
	}

	/**
	 * Returns the lazily-instantiated virtual-thread executor for this resource, or {@code null} when
	 * {@code @Rest(virtualThreads=true)} is unset, the runtime is older than Java 21, or executor construction
	 * failed (in which case a {@code WARNING} was logged at context init).
	 *
	 * @return The executor, or {@code null} if virtual-thread dispatch is not active for this resource.
	 */
	public Executor getVirtualThreadExecutor() { return virtualThreadExecutor.get(); }

	/**
	 * Returns the configured async-response timeout (milliseconds) for this resource. {@code -1} indicates that no
	 * value was supplied at the resource level — the per-op value (or {@code AsyncResponseProcessor}'s 30-second
	 * default) applies in that case.
	 *
	 * @return The async timeout in milliseconds, or {@code -1} when unset.
	 */
	public long getAsyncTimeoutMillis() { return asyncTimeoutMillis.get(); }

	/**
	 * Returns the {@link java.util.concurrent.Executor} configured for routing
	 * {@link java.util.concurrent.CompletableFuture} completion callbacks on this resource, or
	 * {@code null} when {@code @Rest(asyncCompletionExecutor)} is unset (natural completion thread).
	 *
	 * @return The executor, or {@code null} if no async-completion executor is configured.
	 * @since 10.0.0
	 */
	public Executor getAsyncCompletionExecutor() { return asyncCompletionExecutor.get(); }

	/**
	 * Returns whether framework beans and operation/child contexts are eagerly initialized at construction time.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='ja'>{@link Rest#eagerInit()}
	 * </ul>
	 *
	 * @return <jk>true</jk> if eager initialization is enabled.
	 */
	public boolean isEagerInit() { return eagerInit.get(); }

	/**
	 * Returns whether this resource's {@code @Rest(children=...)} entries are built lazily on first invocation
	 * rather than eagerly at parent startup.
	 *
	 * <p>
	 * Resolution order (highest wins):
	 * <ol>
	 *   <li>{@link Builder#lazyChildInit(boolean)} programmatic knob (if explicitly set).</li>
	 *   <li>{@link Rest#lazyChildren() @Rest(lazyChildren)} annotation chain (most-derived wins).</li>
	 *   <li>{@code RestContext.lazyChildren} env-driven default (default {@code false}).</li>
	 * </ol>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='ja'>{@link Rest#lazyChildren()}
	 * 	<li class='jm'>{@link Builder#lazyChildInit(boolean)}
	 * </ul>
	 *
	 * @return <jk>true</jk> if lazy child initialization is enabled.
	 * @since 10.0.0
	 */
	public boolean isLazyChildren() {
		return builder.lazyChildInit != null ? builder.lazyChildInit : lazyChildrenAnnotation.get();
	}

	/**
	 * Called during servlet initialization to invoke all {@link RestPostInit} child-last methods.
	 *
	 * @return This object.
	 * @throws ServletException Error occurred.
	 */
	public synchronized RestContext postInit() throws ServletException {
		if (initialized.get())
			return this;
		var resource2 = getResource();
		initializeResourceContext(resource2);
		for (var x : postInitInvokerPair.get().invokers) {
			try {
				x.invoke(beanStore, getResource());
			} catch (Exception e) {
				throw new ServletException(unwrap(e));
			}
		}
		getRestChildren().postInit();
		return this;
	}

	private void initializeResourceContext(Object resource2) {
		var mi = ClassInfo.of(resource2).getMethod(x -> x.hasName("setContext") && x.hasParameterTypes(RestContext.class)).orElse(null);
		if (nn(mi)) {
			try {
				mi.accessible().invoke(resource2, this);
			} catch (ExecutableException e) {
				throw new RuntimeException(e.unwrap());
			}
		}
	}

	/**
	 * Called during servlet initialization to invoke all {@link RestPostInit} child-first methods.
	 *
	 * @return This object.
	 * @throws ServletException Error occurred.
	 */
	public RestContext postInitChildFirst() throws ServletException {
		if (initialized.get())
			return this;
		getRestChildren().postInitChildFirst();
		for (var x : postInitChildFirstInvokerPair.get().invokers) {
			try {
				x.invoke(beanStore, getResource());
			} catch (Exception e) {
				throw new ServletException(unwrap(e));
			}
		}
		initialized.set(true);
		return this;
	}

	private boolean isDebug(RestSession call) {
		return getDebugConfig().resolve(this, call.getRequest()).enabled();
	}

	/**
	 * Validates that when {@code @Rest(observability="true")} is declared on this resource, at least one real
	 * observability backend ({@link org.apache.juneau.rest.server.metrics.MetricsRecorder} or
	 * {@link org.apache.juneau.rest.server.tracing.TracerHook}) is registered in the bean store.
	 *
	 * <p>
	 * Called once during {@link RestContext} construction, after all {@code @Bean} method injection has run.
	 * If the attribute is {@code "true"} and both beans resolve to their {@code NoOp} singletons, construction
	 * fails with a {@link org.apache.juneau.rest.server.BasicHttpException InternalServerError} whose message precisely
	 * identifies the missing backends.
	 *
	 * <p>
	 * Per-operation {@code @RestOp(observability="true")} startup-fails are handled the same way inside
	 * {@link RestOpContext}.
	 *
	 * @throws InternalServerError when {@code @Rest(observability="true")} is set but no backend is wired.
	 */
	private void checkObservabilityBackendPresent() {
		var attr = observabilityAttribute.get();
		if (!"true".equalsIgnoreCase(attr))
			return;
		var recorder = beanStore.getBean(MetricsRecorder.class).orElse(null);
		var tracer = beanStore.getBean(TracerHook.class).orElse(null);
		boolean hasRecorder = recorder != null && !(recorder instanceof NoOpMetricsRecorder);
		boolean hasTracer = tracer != null && !(tracer instanceof NoOpTracerHook);
		if (!hasRecorder && !hasTracer)
			throw new InternalServerError(
				"@Rest(observability=\"true\") is set on " + getResourceClass().getSimpleName()
				+ " but no MetricsRecorder or TracerHook @Bean is registered. "
				+ "Either wire a backend or change observability to \"\" (inherit) or \"false\" (opt-out).");
	}

	/**
	 * Eagerly evaluates the {@link #asyncCompletionExecutor} memoizer when the annotation or programmatic
	 * override is non-blank, ensuring "bean not found" surfaces as a startup failure.
	 *
	 * @throws IllegalStateException if the named executor bean is not in the bean store.
	 */
	private void checkAsyncCompletionExecutorPresent() {
		var name = builder.asyncCompletionExecutorName != null
			? builder.asyncCompletionExecutorName
			: mergeReplacedStringAttribute(PROPERTY_asyncCompletionExecutor, null);
		if (name != null && !name.isBlank())
			asyncCompletionExecutor.get(); // throws IllegalStateException if bean is missing
	}

	private void initializeFrameworkBeansForRestOps() {
		getMarshallingContext();
		getEncoders();
		getSerializers();
		getParsers();
		getLogger();
		getThrownStore();
		getMethodExecStore();
		getMessages();
		getVarResolver();
		getConfig();
		getResponseProcessors();
		getCallLogger();
		getPartSerializer();
		getPartParser();
		getJsonSchemaGenerator();
		getStaticFiles();
		getDefaultRequestHeaders();
		getDefaultResponseHeaders();
		getDefaultRequestAttributes();
		getDebugEnablement();
		getDebugConfig();
		getSwaggerProvider();
		getOpenApiProvider();
	}

	private static Set<String> newCaseInsensitiveSet(String value) {
		var s = new TreeSet<>(String.CASE_INSENSITIVE_ORDER) {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean contains(Object v) {
				return v != null && super.contains(v);
			}
		};
		StringUtils.split(value, s::add);
		return u(s);
	}

	private LifecycleInvokerPair buildLifecycleInvokerPair(Supplier<MethodList> methods) {
		var ml = MethodList.of(methods.get());
		var inv = ml.stream().map(this::toMethodInvoker).toArray(MethodInvoker[]::new);
		return new LifecycleInvokerPair(ml, inv);
	}

	private MethodInvoker toMethodInvoker(Method m) {
		return new MethodInvoker(m, getMethodExecStats(m));
	}

	private static Throwable unwrap(Throwable t) {
		if (t instanceof InvocationTargetException t2)
			return t2.getTargetException();
		return t;
	}

	/**
	 * Method that can be subclassed to allow uncaught throwables to be treated as other types of throwables.
	 *
	 * <p>
	 * The default implementation looks at the throwable class name to determine whether it can be converted to another type:
	 *
	 * <ul>
	 * 	<li><js>"*AccessDenied*"</js> - Converted to {@link Unauthorized}.
	 * 	<li><js>"*Empty*"</js>,<js>"*NotFound*"</js> - Converted to {@link NotFound}.
	 * </ul>
	 *
	 * @param t The thrown object.
	 * @return The converted thrown object.
	 */
	public Throwable convertThrowable(Throwable t) {

		if (t instanceof InvocationTargetException t2)
			t = t2.getTargetException();

		if (t instanceof ExecutableException t2)
			t = t2.getTargetException();

		if (t instanceof BasicHttpException t2)
			return t2;

		var ci = ClassInfo.of(t);

		if (ci.hasAnnotation(Response.class))
			return t;

		if (ci.isAssignableTo(ParseException.class) || ci.is(InvalidDataConversionException.class))
			return new BadRequest(t);

		String n = cn(t);

		if (co(n, "AccessDenied") || co(n, "Unauthorized"))
			return new Unauthorized(t);

		if (co(n, "Empty") || co(n, "NotFound"))
			return new NotFound(t);

		return t;
	}

	/**
	 * Called at the end of a request to invoke all {@link RestEndCall} methods on this context.
	 *
	 * <p>
	 * Fires this context's own {@code @RestEndCall} methods against its own resource instance. The host's
	 * {@code endCall(...)} is invoked from {@link #execute(Object, HttpServletRequest, HttpServletResponse)};
	 * for mixin-endpoint requests, that path additionally calls {@code endCall} on the mixin sub-context
	 * after the host's so host hooks fire first, then the mixin's.
	 *
	 * @param session The current request.
	 */
	protected void endCall(RestSession session) {
		for (var x : endCallInvokerPair.get().invokers) {
			try {
				x.invoke(session.getBeanStore(), getResource());
			} catch (Exception e) {
				getLogger().log(Level.WARNING, unwrap(e), () -> String.format("Error occurred invoking finish-call method '%s'.", x.getFullName()));
			}
		}
	}

	/**
	 * Finds the {@link RestOpArg} instances to handle resolving objects on the calls to the specified Java method.
	 *
	 * @param m The Java method being called.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created during context bootstrap.
	 * @return The array of resolvers.
	 */
	protected RestOpArg[] findRestOperationArgs(Method m, BeanStore beanStore) {

		var mi = MethodInfo.of(m);
		var params = mi.getParameters();
		var ra = new RestOpArg[params.size()];

		var bs = new BasicBeanStore(beanStore);
		var roa = getRestOpArgs();

		for (var i = 0; i < params.size(); i++) {
			var pi = params.get(i);
			bs.addBean(ParameterInfo.class, pi);
			for (var c : roa) {
				try {
					ra[i] = BeanInstantiator.of(RestOpArg.class, bs)
						.type(c)
						.factoryMethodNames("getInstance", "create")
						.factoryAbstainOnNull()
						.run();
					if (nn(ra[i]))
						break;
				} catch (ExecutableException e) {
					throw new InternalServerError(e.unwrap(), "Could not resolve parameter {0} on method {1}.", i, mi.getNameFull());
				}
			}
			if (ra[i] == null)
				throw new InternalServerError("Could not resolve parameter {0} on method {1}.", i, mi.getNameFull());
		}

		return ra;
	}

	/**
	 * Returns the time statistics gatherer for the specified method.
	 *
	 * @param m The method to get statistics for.
	 * @return The cached time-stats object.
	 */
	protected MethodExecStats getMethodExecStats(Method m) {
		return getMethodExecStore().getStats(m);
	}

	/**
	 * Returns the list of methods to invoke after the actual REST method is called.
	 *
	 * @return The list of methods to invoke after the actual REST method is called.
	 */
	public MethodList getPostCallMethods() { return postCallMethods.get(); }

	/**
	 * Returns the list of methods to invoke before the actual REST method is called.
	 *
	 * @return The list of methods to invoke before the actual REST method is called.
	 */
	public MethodList getPreCallMethods() { return preCallMethods.get(); }

	/**
	 * Returns the list of methods to invoke during servlet destruction.
	 *
	 * @return The destroy method list, never {@code null}.
	 */
	public MethodList getDestroyMethods() { return destroyInvokerPair.get().methods; }

	/**
	 * Returns the list of methods to invoke at the end of each HTTP request.
	 *
	 * @return The end-call method list, never {@code null}.
	 */
	public MethodList getEndCallMethods() { return endCallInvokerPair.get().methods; }

	/**
	 * Returns the list of methods to invoke during servlet post-initialization (child-last phase).
	 *
	 * @return The post-init method list, never {@code null}.
	 */
	public MethodList getPostInitMethods() { return postInitInvokerPair.get().methods; }

	/**
	 * Returns the list of methods to invoke during servlet post-initialization (child-first phase).
	 *
	 * @return The post-init-child-first method list, never {@code null}.
	 */
	public MethodList getPostInitChildFirstMethods() { return postInitChildFirstInvokerPair.get().methods; }

	/**
	 * Returns the list of methods to invoke at the start of each HTTP request.
	 *
	 * @return The start-call method list, never {@code null}.
	 */
	public MethodList getStartCallMethods() { return startCallInvokerPair.get().methods; }

	/**
	 * Method for handling response errors.
	 *
	 * <p>
	 * Subclasses can override this method to provide their own custom error response handling.
	 *
	 * @param session The rest call.
	 * @param e The exception that occurred.
	 * @throws IOException Can be thrown if a problem occurred trying to write to the output stream.
	 */
	protected synchronized void handleError(RestSession session, Throwable e) throws IOException {

		session.exception(e);

		if (session.isDebug())
			getLogger().log(Level.WARNING, e, () -> "Error occurred during REST call.");

		int code = 500;

		var ci = ClassInfo.of(e);
		var r = ci.getAnnotations(StatusCode.class).findFirst().map(AnnotationInfo::inner).orElse(null);
		if (nn(r) && r.value().length > 0)
			code = r.value()[0];

		var e2 = (e instanceof BasicHttpException e22 ? e22 : new BasicHttpException(code, null, e));

		var req = session.getRequest();
		var res = session.getResponse();

		Throwable t = e2.getRootCause();
		if (nn(t)) {
			Thrown t2 = Thrown.of(t);
			res.setHeader(t2.getName(), t2.getValue());
		}

		try {
			var statusCode = e2.getStatusLine().getStatusCode();

			if (e2 instanceof ValidationException ve && writeValidationErrorBody(res, ve, statusCode, isProblemDetails()))
				return;

			if (isProblemDetails() && writeProblemDetailsBody(res, e2, statusCode))
				return;

			res.setContentType("text/plain");
			res.setHeader("Content-Encoding", "identity");
			res.setStatus(statusCode);

			PrintWriter w = getResponseWriter(res);

			try (PrintWriter w2 = w) {
				var httpMessage = RestUtils.getHttpResponseText(statusCode);
				if (nn(httpMessage))
					w2.append("HTTP ").append(String.valueOf(statusCode)).append(": ").append(httpMessage).append("\n\n");
				if (isRenderResponseStackTraces())
					e.printStackTrace(w2);
				else
					w2.append(e2.getFullStackMessage(true));
			}

		} catch (Exception e1) {
			req.setAttribute("Exception", e1);
		}
	}

	/**
	 * Writes a {@code BasicHttpException} as an {@code application/problem+json} body. Q5(A): the {@code Accept}
	 * header is intentionally ignored on the error path so opted-in resources always emit Problem JSON for thrown
	 * exceptions.
	 *
	 * @return {@code true} if the body was written (response committed). {@code false} on serialization failure so
	 * 	the caller can fall back to the legacy {@code text/plain} writer.
	 */
	private static boolean writeProblemDetailsBody(HttpServletResponse res, BasicHttpException e, int statusCode) {
		try {
			var problem = ProblemAdapters.fromException(e);
			res.setStatus(statusCode);
			res.setContentType(ContentType.APPLICATION_PROBLEM_JSON.getValue());
			res.setHeader("Content-Encoding", "identity");
			var os = res.getOutputStream();
			JsonSerializer.DEFAULT.serialize(problem, os);
			os.flush();
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

	/**
	 * Writes a {@link ValidationException} as a structured JSON body carrying the per-field
	 * {@link ValidationViolation} list, so client UIs can render field-level errors.
	 *
	 * <p>
	 * Two shapes depending on the resource's {@code @Rest(problemDetails)} opt-in:
	 * <ul class='spaced-list'>
	 * 	<li><b>Problem-details ON</b> &mdash; emits {@code application/problem+json} with the standard
	 * 		RFC 7807 {@code status}/{@code title}/{@code detail} members plus an {@code errors[]} extension
	 * 		array populated from {@link ValidationException#getViolations()}. Bypasses the generic
	 * 		{@link ProblemAdapters#fromException(BasicHttpException)} path because that adapter is
	 * 		intentionally narrow (no extensions) &mdash; validation needs its own per-field payload.
	 * 	<li><b>Problem-details OFF (default)</b> &mdash; emits {@code application/json} with the simple
	 * 		{@code { "status":400, "errors":[ ... ] }} envelope. Keeps the response usable from clients that
	 * 		can't parse {@code application/problem+json} without making validation responses look like a
	 * 		text/plain stack trace.
	 * </ul>
	 *
	 * @return {@code true} if the body was written (response committed). {@code false} on serialization
	 * 	failure so the caller can fall back to the legacy {@code text/plain} writer.
	 */
	private static boolean writeValidationErrorBody(HttpServletResponse res, ValidationException ve, int statusCode, boolean problemDetails) {
		try {
			res.setStatus(statusCode);
			res.setHeader("Content-Encoding", "identity");
			var os = res.getOutputStream();
			if (problemDetails) {
				var problem = new Problem()
					.setStatus(statusCode)
					.setTitle(ve.getStatusLine().getReasonPhrase())
					.setDetail(ve.getMessage())
					.set("errors", ve.getViolations());
				res.setContentType(ContentType.APPLICATION_PROBLEM_JSON.getValue());
				JsonSerializer.DEFAULT.serialize(problem, os);
			} else {
				var envelope = new LinkedHashMap<String,Object>();
				envelope.put("status", statusCode);
				envelope.put("errors", ve.getViolations());
				res.setContentType("application/json");
				JsonSerializer.DEFAULT.serialize(envelope, os);
			}
			os.flush();
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

	/**
	 * Handle the case where a matching method was not found.
	 *
	 * <p>
	 * Subclasses can override this method to provide a 2nd-chance for specifying a response.
	 * The default implementation will simply throw an exception with an appropriate message.
	 *
	 * @param session The HTTP call.
	 * @throws Exception Any exception can be thrown.
	 */
	void invokeRestInitMethod(MethodInfo m, Supplier<?> resource, BeanStore beanStore) throws ServletException {
		try {
			m.inject(beanStore, resource.get());
		} catch (Exception e) {
			throw servletException(e, "Exception thrown from @RestInit method {0}.{1}.", cns(m.getDeclaringClass()), m.getSignature());
		}
	}

	private static PrintWriter getResponseWriter(HttpServletResponse res) throws IOException {
		try {
			return res.getWriter();
		} catch (@SuppressWarnings("unused") IllegalStateException x) {
			return new PrintWriter(new OutputStreamWriter(res.getOutputStream(), UTF8));
		}
	}

	@SuppressWarnings({
		"java:S112" // throws Exception intentional - callback/lifecycle method
	})
	protected void handleNotFound(RestSession session) throws Exception {
		var pathInfo = session.getPathInfo();
		var methodUC = session.getMethod();
		var rc = session.getStatus();
		var onPath = pathInfo == null ? " on no pathInfo" : (" on path '" + pathInfo + "'");
		if (rc == SC_NOT_FOUND)
			throw new NotFound("Method ''{0}'' not found on resource with matching pattern{1}.", methodUC, onPath);
		else if (rc == SC_PRECONDITION_FAILED)
			throw new PreconditionFailed("Method ''{0}'' not found on resource{1} with matching matcher.", methodUC, onPath);
		else if (rc == SC_METHOD_NOT_ALLOWED)
			throw new MethodNotAllowed("Method ''{0}'' not found on resource{1}.", methodUC, onPath);
		else
			throw new ServletException("Invalid method response: " + rc, session.getException());
	}

	/**
	 * Called during a request to invoke all {@link RestPostCall} methods.
	 *
	 * <p>
	 * For mixin-endpoint requests, fires the host's {@code @RestPostCall} methods first (via the host's
	 * {@code localPostCallInvokers}), then the mixin's per-op invokers ({@link RestOpContext#getPostCallMethods()}).
	 * Host-endpoint requests fire only the host's per-op invokers (the established path; unchanged from pre-10.0.0).
	 *
	 * @param session The current request.
	 * @throws Exception If thrown from call methods.
	 */
	protected void postCall(RestOpSession session) throws Exception {
		var opCtx = session.getContext().getContext();
		if (opCtx.isMixinContext && opCtx.parentContext != null)
			for (var m : opCtx.parentContext.localPostCallInvokers.get())
				m.invoke(session);
		for (var m : session.getContext().getPostCallMethods())
			m.invoke(session);
	}

	/**
	 * Called during a request to invoke all {@link RestPreCall} methods.
	 *
	 * <p>
	 * For mixin-endpoint requests, fires the host's {@code @RestPreCall} methods first (via the host's
	 * {@code localPreCallInvokers}), then the mixin's per-op invokers ({@link RestOpContext#getPreCallMethods()}).
	 * Host-endpoint requests fire only the host's per-op invokers (the established path; unchanged from pre-10.0.0).
	 *
	 * @param session The current request.
	 * @throws Exception If thrown from call methods.
	 */
	protected void preCall(RestOpSession session) throws Exception {
		var opCtx = session.getContext().getContext();
		if (opCtx.isMixinContext && opCtx.parentContext != null)
			for (var m : opCtx.parentContext.localPreCallInvokers.get())
				m.invoke(session);
		for (var m : session.getContext().getPreCallMethods())
			m.invoke(session);
	}

	/**
	 * The main method for serializing POJOs passed in through the {@link RestResponse#setContent(Object)} method or
	 * returned by the Java method.
	 *
	 * <p>
	 * Subclasses may override this method if they wish to modify the way the output is rendered or support other output
	 * formats.
	 *
	 * <p>
	 * The default implementation simply iterates through the response handlers on this resource
	 * looking for the first one whose {@link ResponseProcessor#process(RestOpSession)} method returns
	 * <jk>true</jk>.
	 *
	 * @param opSession The HTTP call.
	 * @throws IOException Thrown by underlying stream.
	 * @throws BasicHttpException Non-200 response.
	 * @throws NotImplemented No registered response processors could handle the call.
	 */
	@SuppressWarnings({
		"java:S127" // Loop counter i resets to -1 on RESTART
	})
	public void processResponse(RestOpSession opSession) throws IOException, BasicHttpException, NotImplemented {

		// Loop until we find the correct processor for the POJO.
		int loops = 5;
		var rp = getResponseProcessors();
		for (var i = 0; i < rp.length; i++) {
			var j = rp[i].process(opSession);
			if (j == FINISHED)
				return;
			if (j == RESTART) {
				if (loops-- < 0)
					throw new InternalServerError("Too many processing loops.");
				i = -1;  // Start over.
			}
		}

		var output = opSession.getResponse().getContent().orElse(null);
		throw new NotImplemented("No response processors found to process output of type ''{0}''", cn(output));
	}

	@Override /* Overridden from Context */
	protected FluentMap<String,Object> properties() {
		return super.properties()
			.a(PROP_allowContentParam, isAllowContentParam())
			.a(PROPERTY_allowedHeaderParams, getAllowedHeaderParams())
			.a(PROPERTY_allowedMethodHeaders, getAllowedMethodHeaders())
			.a(PROPERTY_allowedMethodParams, getAllowedMethodParams())
			.a(PROP_beanStore, beanStore)
			.a(PROPERTY_clientVersionHeader, getClientVersionHeader())
			.a(PROP_consumes, getConsumes())
			.a(PROP_defaultRequestHeaders, getDefaultRequestHeaders())
			.a(PROP_defaultResponseHeaders, getDefaultResponseHeaders())
			.a(PROP_partParser, getPartParser())
			.a(PROP_partSerializer, getPartSerializer())
			.a(PROP_produces, getProduces())
			.a(PROPERTY_renderResponseStackTraces, isRenderResponseStackTraces())
			.a(PROP_responseProcessors, getResponseProcessors())
			.a(PROP_restOpArgs, getRestOpArgs())
			.a(PROP_staticFiles, getStaticFiles())
			.a(PROP_swaggerProvider, getSwaggerProvider())
			.a(PROP_openApiProvider, getOpenApiProvider())
			.a(PROPERTY_uriAuthority, getUriAuthority())
			.a(PROPERTY_uriContext, getUriContext())
			.a(PROPERTY_uriRelativity, getUriRelativity())
			.a(PROPERTY_uriResolution, getUriResolution());
	}

	/**
	 * Called at the start of a request to invoke all {@link RestStartCall} methods on this context.
	 *
	 * <p>
	 * Fires this context's own {@code @RestStartCall} methods against its own resource instance. The host
	 * version is invoked from {@link #execute(Object, HttpServletRequest, HttpServletResponse)} before the
	 * operation is resolved; once the operation is known to belong to a mixin sub-context,
	 * {@link RestSession#run()} additionally calls {@code startCall} on the mixin context so host hooks fire
	 * first, then the mixin's.
	 *
	 * @param session The current request.
	 * @throws BasicHttpException If thrown from call methods.
	 */
	protected void startCall(RestSession session) throws BasicHttpException {
		for (var x : startCallInvokerPair.get().invokers) {
			try {
				x.invoke(session.getBeanStore(), getResource());
			} catch (IllegalAccessException | IllegalArgumentException e) {
				throw new InternalServerError(e, "Error occurred invoking start-call method ''{0}''.", x.getFullName());
			} catch (InvocationTargetException e) {
				var t = e.getTargetException();
				if (t instanceof BasicHttpException t2)
					throw t2;
				throw new InternalServerError(t);
			}
		}
	}
}