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
package org.apache.juneau.rest;

import org.apache.juneau.commons.http.MediaType;
import static jakarta.servlet.http.HttpServletResponse.*;
import static java.util.Collections.*;
import static org.apache.juneau.commons.reflect.ReflectionUtils.*;
import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.PredicateUtils.*;
import static org.apache.juneau.rest.RestServerConstants.*;
import static org.apache.juneau.commons.utils.IoUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;
import static org.apache.juneau.http.HttpHeaders.*;
import static org.apache.juneau.rest.annotation.RestOpAnnotation.*;
import static org.apache.juneau.rest.processor.ResponseProcessor.*;
import static org.apache.juneau.rest.util.RestUtils.*;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.lang.reflect.Method;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import java.util.logging.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.swagger.Swagger;
import org.apache.juneau.commons.collections.FluentMap;
import org.apache.juneau.commons.function.Memoizer;
import org.apache.juneau.commons.lang.*;
import org.apache.juneau.commons.logging.Logger;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.commons.utils.*;
import org.apache.juneau.config.*;
import org.apache.juneau.config.vars.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.html.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.httppart.HttpPartSerializer.*;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.parser.ParseException;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.arg.*;
import org.apache.juneau.rest.config.*;
import org.apache.juneau.rest.debug.*;
import org.apache.juneau.rest.httppart.*;
import org.apache.juneau.rest.logger.*;
import org.apache.juneau.rest.processor.*;
import org.apache.juneau.rest.rrpc.*;
import org.apache.juneau.rest.servlet.*;
import org.apache.juneau.rest.staticfile.*;
import org.apache.juneau.rest.stats.*;
import org.apache.juneau.rest.swagger.*;
import org.apache.juneau.rest.util.*;
import org.apache.juneau.rest.vars.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;
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
 * (and inherited from any parent classes), and programmatically through {@link RestInject @RestInject}-annotated
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
 * 		<ja>@RestInject</ja>
 * 		<jk>public</jk> CallLogger callLogger() {
 * 			<jk>return new</jk> MyCustomCallLogger();
 * 		}
 * 	}
 * </p>
 *
 * <p>
 * The historical <c>public MyResource(RestContext.Builder builder)</c> constructor-injection pattern and the
 * <c>{@link RestInit @RestInit} public void init(RestContext.Builder builder)</c> method-injection pattern were both
 * removed in 9.5. {@code RestContext.Builder} itself is on the deletion path; new code should not depend on it.
 * See <a class="doclink" href="https://juneau.apache.org/docs/topics/V9.5MigrationGuide">v9.5 Migration Guide</a>
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
	 * </ul>
	 *
	 * <p>
	 * The {@code beanStoreConfigurer} hook gives test fixtures and integration code a chance to register beans on
	 * the {@link BasicBeanStore} after the resource has been wired in but before the
	 * {@code findXxx()} memoizers fire.
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
	 * @param beanStoreConfigurer A pre-build hook that runs against the resolved {@link BasicBeanStore}
	 * 	after the resource has been wired in but before any {@code findXxx()} memoizer fires. Defaults to a no-op.
	 *
	 * @since 9.5.0
	 */
	public static record Args(
		Class<?> resourceClass,
		RestContext parentContext,
		ServletConfig servletConfig,
		Supplier<?> resource,
		String path,
		Consumer<BasicBeanStore> beanStoreConfigurer
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

	}

	/**
	 * Builder class.
	 *
	 * <p>
	 * Demoted to package-private in the April 2026 refactor (2026-04-19). User code should construct a
	 * {@link RestContext} via the public {@link #RestContext(Args)} constructor; this Builder
	 * exists only as internal bootstrap state for the framework and is slated for inlining in a later phase.
	 */
	static class Builder extends Context.Builder implements ServletConfig {

		private static final AnnotationProvider AP = AnnotationProvider.INSTANCE;

		// @formatter:off
		private static final Set<Class<?>> DELAYED_INJECTION = set(
			BeanContext.Builder.class,
			BasicBeanStore.Builder.class,
			BasicBeanStore.class,
			CallLogger.class,
			Config.class,
			DebugEnablement.class,
			EncoderSet.Builder.class,
			EncoderSet.class,
			FileFinder.Builder.class,
			FileFinder.class,
			HttpPartParser.class,
			HttpPartParser.Creator.class,
			HttpPartSerializer.class,
			HttpPartSerializer.Creator.class,
			JsonSchemaGenerator.Builder.class,
			JsonSchemaGenerator.class,
			Logger.class,
			Messages.class,
			MethodExecStore.class,
			ParserSet.Builder.class,
			ParserSet.class,
			ResponseProcessorList.Builder.class,
			ResponseProcessorList.class,
			RestChildren.Builder.class,
			RestChildren.class,
			RestOpArgList.Builder.class,
			RestOpArgList.class,
			RestOperations.Builder.class,
			RestOperations.class,
			SerializerSet.Builder.class,
			SerializerSet.class,
			StaticFiles.class,
			SwaggerProvider.class,
			ThrownStore.class,
			VarList.class,
			VarResolver.class
		);

		private static final Set<String> DELAYED_INJECTION_NAMES = set(
			PROP_bootstrapVarResolver,
			"destroyMethods",
			"endCallMethods",
			"postCallMethods",
			"postInitChildFirstMethods",
			"postInitMethods",
			"preCallMethods",
			"startCallMethods"
		);
		// @formatter:on

		//-----------------------------------------------------------------------------------------------------------------
		// The following fields are meant to be modifiable.
		// They should not be declared final.
		// Read-only snapshots of these will be made in RestServletContext.
		//-----------------------------------------------------------------------------------------------------------------

		private static <T extends Annotation> Stream<Method> getAnnotatedMethods(Supplier<?> resource, Class<T> annotation) {
			return ClassInfo.ofProxy(resource.get()).getAllMethodsTopDown().stream()
				.filter(y -> y.hasAnnotation(annotation))
				.filter(distinctByKey(MethodInfo::getSignature))
				.map(y -> y.accessible().inner());
		}

		private static boolean isRestInjectMethod(MethodInfo mi) {
			return isRestInjectMethod(mi, null);
		}

		private static boolean isRestInjectMethod(MethodInfo mi, String name) {
			return mi.getAnnotations(RestInject.class)
				.map(AnnotationInfo::inner)
				.anyMatch(x -> nn(x) && x.methodScope().length == 0 && (n(name) || eq(x.name(), name)));
		}

		private BeanContext.Builder beanContext;
		private BasicBeanStore beanStore;
		private BasicBeanStore bootstrapBeanStore;
		private final Class<?> resourceClass;
		private Config config;
		private HttpPartParser.Creator partParser;
		private HttpPartSerializer.Creator partSerializer;
		private JsonSchemaGenerator.Builder jsonSchemaGenerator;
		private List<Object> children = list();
		private final RestContext parentContext;
		private RestOpArgList.Builder restOpArgs;
		private ResponseProcessorList.Builder responseProcessors;
		private ResourceSupplier resource;
		private final ServletConfig inner;
		private String path = null;
		private VarResolver bootstrapVarResolver;

		/**
		 * Package-private constructor.
		 *
		 * <p>
		 * Demoted from {@code protected} to package-private in the April 2026 refactor (2026-04-19). Only
		 * {@link RestContext#RestContext(Args)} instantiates this type now.
		 *
		 * @param rci The bootstrap arguments. Must not be {@code null}.
		 * @throws ServletException If hook method calls failed.
		 */
		Builder(Args rci) throws ServletException {

			this.resourceClass = rci.resourceClass();
			this.inner = rci.servletConfig();
			this.parentContext = rci.parentContext();

			if (nn(parentContext))
				bootstrapBeanStore = parentContext.bootstrapBeanStore;

			init(rci.resource());

			if (! rci.path().isEmpty())
				path(rci.path());
			rci.beanStoreConfigurer().accept(beanStore());
		}

		@Override /* Context.Builder is abstract - copy() is not meaningful for the transient RestContext bootstrap state. */
		public Builder copy() {
			throw new NoSuchMethodError("Not implemented.");
		}

		/**
		 * Returns the bean context sub-builder.
		 *
		 * <p>
		 * The bean context is used to retrieve metadata on Java beans.
		 *
		 * <p>
		 * The default bean context can overridden via any of the following:
		 * <ul class='spaced-list'>
		 * 	<li>Injected via bean store.
		 * </ul>
		 *
		 * @return The bean context sub-builder.
		 */
		public BeanContext.Builder beanContext() {
			if (beanContext == null)
				beanContext = createBeanContext(beanStore(), resource());
			return beanContext;
		}

		/**
		 * Returns the bean store in this builder.
		 *
		 * <p>
		 * The bean store is a simple storage database for beans keyed by type and name.
		 *
		 * <p>
		 * The bean store is created with the parent root bean store as the parent, allowing any beans in the root bean store to be available
		 * in this builder.  The root bean store typically pulls from an injection framework such as Spring to allow injected beans to be used.
		 *
		 * <p>
		 * The default bean store can be overridden via any of the following:
		 * <ul class='spaced-list'>
		 * 	<li>Class annotation:  {@link Rest#beanStore() @Rest(beanStore)}
		 * 	<li>{@link RestInject @RestInject}-annotated methods:
		 * 		<p class='bjava'>
		 * 	<ja>@RestInject</ja> <jk>public</jk> [<jk>static</jk>] BasicBeanStore myMethod(<i>&lt;args&gt;</i>) {...}
		 * 		</p>
		 * 		Args can be any injected bean including {@link org.apache.juneau.cp.BasicBeanStore.Builder}, the default builder.
		 * </ul>
		 *
		 * @return The bean store in this builder.
		 */
		public BasicBeanStore beanStore() {
			return beanStore;
		}

		/**
		 * Child REST resources.
		 *
		 * <p>
		 * Defines children of this resource.
		 *
		 * <p>
		 * A REST child resource is simply another servlet or object that is initialized as part of the ascendant resource and has a
		 * servlet path directly under the ascendant resource object path.
		 * <br>The main advantage to defining servlets as REST children is that you do not need to define them in the
		 * <c>web.xml</c> file of the web application.
		 * <br>This can cut down on the number of entries that show up in the <c>web.xml</c> file if you are defining
		 * large numbers of servlets.
		 *
		 * <p>
		 * Child resources must specify a value for {@link Rest#path() @Rest(path)} that identifies the subpath of the child resource
		 * relative to the ascendant path.
		 *
		 * <p>
		 * Child resources can be nested arbitrarily deep using this technique (i.e. children can also have children).
		 *
		 * <dl>
		 * 	<dt>Servlet initialization:</dt>
		 * 	<dd>
		 * 		<p>
		 * 			A child resource will be initialized immediately after the ascendant servlet/resource is initialized.
		 * 			<br>The child resource receives the same servlet config as the ascendant servlet/resource.
		 * 			<br>This allows configuration information such as servlet initialization parameters to filter to child
		 * 			resources.
		 * 		</p>
		 * 	</dd>
		 * 	<dt>Runtime behavior:</dt>
		 * 	<dd>
		 * 		<p>
		 * 			As a rule, methods defined on the <c>HttpServletRequest</c> object will behave as if the child
		 * 			servlet were deployed as a top-level resource under the child's servlet path.
		 * 			<br>For example, the <c>getServletPath()</c> and <c>getPathInfo()</c> methods on the
		 * 			<c>HttpServletRequest</c> object will behave as if the child resource were deployed using the
		 * 			child's servlet path.
		 * 			<br>Therefore, the runtime behavior should be equivalent to deploying the child servlet in the
		 * 			<c>web.xml</c> file of the web application.
		 * 		</p>
		 * 	</dd>
		 * </dl>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Our child resource.</jc>
		 * 	<ja>@Rest</ja>(path=<js>"/child"</js>)
		 * 	<jk>public class</jk> MyChildResource {...}
		 *
		 * 	<jc>// Registered via annotation.</jc>
		 * 	<ja>@Rest</ja>(children={MyChildResource.<jk>class</jk>})
		 * 	<jk>public class</jk> MyResource { ... }
		 * </p>
		 *
		 * <p>
		 * For programmatic registration of pre-instantiated child resources, supply them via
		 * {@link Rest#children() @Rest(children)} when constructing the context directly.
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>
		 * 		When defined as classes, instances are resolved using the registered bean store
		 * 		({@link BasicBeanStore} by default), which instantiates them via their public no-arg
		 * 		constructor or via bean-store-resolved arguments.
		 * </ul>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link Rest#children()}
		 * </ul>
		 *
		 * @param values The child resources to add.
		 * 	<br>Cannot contain <jk>null</jk> values.
		 * 	<br>Objects can be any of the specified types:
		 * 	<ul>
		 * 		<li>A class that has a constructor described above.
		 * 		<li>An instantiated resource object (such as a servlet object instantiated by a servlet container).
		 * 	</ul>
		 * @return This object.
		 */
		public Builder children(Object...values) {
			assertArgNoNulls("values", values);
			addAll(children, values);
			return this;
		}


		/**
		 * Returns the external configuration file for this resource.
		 *
		 * <p>
		 * The config file contains arbitrary configuration information that can be accessed by this class, usually
		 * via <c>$C</c> variables.
		 *
		 * <p>
		 * The default config can be overridden via any of the following:
		 * <ul class='spaced-list'>
		 * 	<li>Injected via bean store.
		 * 	<li>Class annotation:  {@link Rest#config() @Rest(config)}
		 * 	<li>{@link RestInject @RestInject}-annotated method:
		 * 		<p class='bjava'>
		 * 	<ja>@RestInject</ja> <jk>public</jk> [<jk>static</jk>] Config myMethod(<i>&lt;args&gt;</i>) {...}
		 * 		</p>
		 * 		Args can be any injected bean.
		 * </ul>
		 *
		 * <p>
		 * If a config file is not set up, then an empty config file will be returned that is not backed by any file.
		 *
		 * <p>
		 * This bean can be accessed directly via {@link RestContext#getConfig()} or passed in as a parameter
		 * on a {@link RestOp}-annotated method.
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ConfigurationFiles">Configuration Files</a>
		 * </ul>
		 *
		 * @return The external configuration file for this resource.
		 */
		public Config config() {
			return config;
		}

		@Override /* Overridden from ServletConfig */
		public String getInitParameter(String name) {
			return inner == null ? null : inner.getInitParameter(name);
		}

		@Override /* Overridden from ServletConfig */
		public Enumeration<String> getInitParameterNames() { return inner == null ? new Vector<String>().elements() : inner.getInitParameterNames(); }

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
		public String getServletName() { return inner == null ? null : inner.getServletName(); }

		private Builder init(Supplier<?> resource) throws ServletException {

			this.resource = new ResourceSupplier(resourceClass, assertArgNotNull("resource", resource));
			var r = this.resource;
			var rc = resourceClass;

			// @formatter:off
			// Note: pre-9.5 this also called .addBean(Builder.class, this) so user code could request the
			// in-flight RestContext.Builder via @RestInit method parameters or @RestInject method-finder
			// resolution. That injection protocol was deleted in the April 2026 refactor (zero non-test callers
			// across the entire codebase) — the Builder is now a private staging detail of RestContext
			// construction, not a user-visible bean.
			beanStore = createBeanStore(resource)
				.build()
				.addBean(ResourceSupplier.class, this.resource)
				.addBean(ServletConfig.class, nn(inner) ? inner : this)
				.addBean(ServletContext.class, (nn(inner) ? inner : this).getServletContext());
			// @formatter:on

			if (bootstrapBeanStore == null) {
				bootstrapBeanStore = beanStore;
				beanStore = BasicBeanStore.of(bootstrapBeanStore);
			}
			var bs = beanStore;

			beanStore.add(BasicBeanStore.class, bs);
			beanStore.add(VarResolver.class, bootstrapVarResolver());
			config = beanStore.add(Config.class, createConfig(bs, r, rc));

			var rci = ClassInfo.of(resourceClass);

			// Get @RestInject fields initialized with values.
			// @formatter:off
			rci.getAllFields().stream()
				.filter(x -> x.hasAnnotation(RestInject.class))
				.forEach(x -> opt(x.get(resource.get())).ifPresent(
					y -> beanStore.add(
						x.getFieldType().inner(),
						y,
						RestInjectAnnotation.name(x.getAnnotations(RestInject.class).findFirst().map(AnnotationInfo::inner).orElse(null))
					)
				));
			// @formatter:on

			rci.getAllMethods().stream().filter(x -> x.hasAnnotation(RestInject.class)).forEach(x -> {
				var rt = x.getReturnType().<Object>inner();
				var name = RestInjectAnnotation.name(x.getAnnotations(RestInject.class).findFirst().map(AnnotationInfo::inner).orElse(null));
				if (! (DELAYED_INJECTION.contains(rt) || DELAYED_INJECTION_NAMES.contains(name))) {
					// @formatter:off
					new BeanCreateMethodFinder<>(rt, resource.get(), beanStore)
						.find(Builder::isRestInjectMethod)
						.run(y -> beanStore.add(rt, y, name));
					// @formatter:on
				}
			});

			var vrs = bootstrapVarResolver().createSession();
			var work = AnnotationWorkList.of(vrs, rstream(AP.find(rci)).filter(CONTEXT_APPLY_FILTER));

			apply(work);
			beanContext().apply(work);
			partSerializer().apply(work);
			partParser().apply(work);
			jsonSchemaGenerator().apply(work);

			runInitHooks(bs, resource());

			// Set @RestInject fields not initialized with values.
			// @formatter:off
			rci.getAllFields().stream()
				.filter(x -> x.hasAnnotation(RestInject.class))
				.forEach(x -> x.setIfNull(
					resource.get(),
					beanStore.getBean(
						x.getFieldType().inner(),
						RestInjectAnnotation.name(x.getAnnotations(RestInject.class).findFirst().map(AnnotationInfo::inner).orElse(null))
					).orElse(null)
				));
			// @formatter:on

			return this;
		}

		/**
		 * Returns the JSON schema generator sub-builder.
		 *
		 * <p>
		 * The JSON schema generator is used for generating JSON schema in the auto-generated Swagger documentation.
		 *
		 * <p>
		 * The default JSON schema generator is a default {@link JsonSchemaGenerator}.
		 * It can overridden via any of the following:
		 * <ul class='spaced-list'>
		 * 	<li>Injected via bean store.
		 * 	<li>{@link RestInject @RestInject}-annotated method:
		 * 		<p class='bjava'>
		 * 	<ja>@RestInject</ja> <jk>public</jk> [<jk>static</jk>] JsonSchemaGenerator myMethod(<i>&lt;args&gt;</i>) {...}
		 * 		</p>
		 * 		Args can be any injected bean including JsonSchemaGenerator.Builder, the default builder.
		 * </ul>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanSwagger2">juneau-bean-swagger-v2</a>
		 * </ul>
		 *
		 * @return The JSON schema generator sub-builder.
		 */
		public JsonSchemaGenerator.Builder jsonSchemaGenerator() {
			if (jsonSchemaGenerator == null)
				jsonSchemaGenerator = createJsonSchemaGenerator(beanStore(), resource());
			return jsonSchemaGenerator;
		}

		/**
		 * Returns the part parser sub-builder.
		 *
		 * <p>
		 * The part parser is used for parsing HTTP parts such as request headers and query/form/path parameters.
		 *
		 * <p>
		 * The default part parser is an {@link OpenApiParser}.
		 * It can overridden via any of the following:
		 * <ul class='spaced-list'>
		 * 	<li>Injected via bean store.
		 * 	<li>{@link RestInject @RestInject}-annotated method:
		 * 		<p class='bjava'>
		 * 	<ja>@RestInject</ja> <jk>public</jk> [<jk>static</jk>] HttpPartParser myMethod(<i>&lt;args&gt;</i>) {...}
		 * 		</p>
		 * 		Args can be any injected bean including HttpPartParser.Builder, the default builder.
		 * </ul>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/HttpParts">HTTP Parts</a>
		 * </ul>
		 *
		 * @return The part parser sub-builder.
		 */
		public HttpPartParser.Creator partParser() {
			if (partParser == null)
				partParser = createPartParser(beanStore(), resource());
			return partParser;
		}

		/**
		 * Returns the part serializer sub-builder.
		 *
		 * <p>
		 * The part serializer is used for serializing HTTP parts such as response headers.
		 *
		 * <p>
		 * The default part serializer is an {@link OpenApiSerializer}.
		 * It can overridden via any of the following:
		 * <ul class='spaced-list'>
		 * 	<li>Injected via bean store.
		 * 	<li>{@link RestInject @RestInject}-annotated method:
		 * 		<p class='bjava'>
		 * 	<ja>@RestInject</ja> <jk>public</jk> [<jk>static</jk>] HttpPartSerializer myMethod(<i>&lt;args&gt;</i>) {...}
		 * 		</p>
		 * 		Args can be any injected bean including HttpPartSerializer.Builder, the default builder.
		 * </ul>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/HttpParts">HTTP Parts</a>
		 * </ul>
		 *
		 * @return The part serializer sub-builder.
		 */
		public HttpPartSerializer.Creator partSerializer() {
			if (partSerializer == null)
				partSerializer = createPartSerializer(beanStore(), resource());
			return partSerializer;
		}

		/**
		 * Resource path.
		 *
		 * <p>
		 * Identifies the URL subpath relative to the parent resource.
		 *
		 * <p>
		 * This setting is critical for the routing of HTTP requests from ascendant to child resources.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Defined via annotation.</jc>
		 * 	<ja>@Rest</ja>(path=<js>"/myResource"</js>)
		 * 	<jk>public class</jk> MyResource { ... }
		 * </p>
		 *
		 * <p>
		 * For programmatic construction, supply the path via {@link RestContext.Args} when building a {@link RestContext}
		 * directly.
		 *
		 * <p>
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>
		 * 		This annotation is ignored on top-level servlets (i.e. servlets defined in <c>web.xml</c> files).
		 * 		<br>Therefore, implementers can optionally specify a path value for documentation purposes.
		 * 	<li class='note'>
		 * 		Typically, this setting is only applicable to resources defined as children through the
		 * 		{@link Rest#children() @Rest(children)} annotation.
		 * 		<br>However, it may be used in other ways (e.g. defining paths for top-level resources in microservices).
		 * 	<li class='note'>
		 * 		Slashes are trimmed from the path ends.
		 * 		<br>As a convention, you may want to start your path with <js>'/'</js> simple because it make it easier to read.
		 * 	<li class='note'>
		 * 		This path is available through the following method:
		 * 		<ul>
		 * 			<li class='jm'>{@link RestContext#getPath() RestContext.getPath()}
		 * 		</ul>
		 * </ul>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link Rest#path}
		 * </ul>
		 *
		 * @param value The new value for this setting.
		 * 	<br>Can be <jk>null</jk> or empty (path will not be set, defaults to empty string).
		 * @return This object.
		 */
		public Builder path(String value) {
			value = trimLeadingSlashes(value);
			if (ne(value))
				path = value;
			return this;
		}

		/**
		 * Returns the REST servlet/bean instance that this context is defined against.
		 *
		 * @return The REST servlet/bean instance that this context is defined against.
		 */
		@SuppressWarnings({
			"java:S1452"  // Wildcard required - Supplier<?> for generic REST resource instance
		})
		public Supplier<?> resource() {
			return Objects.requireNonNull(resource, "Resource not available. init(Object) has not been called.");
		}

		/**
		 * Returns the response processor list sub-builder.
		 *
		 * <p>
		 * Specifies a list of {@link ResponseProcessor} classes that know how to convert POJOs returned by REST methods or
		 * set via {@link RestResponse#setContent(Object)} into appropriate HTTP responses.
		 *
		 * <p>
		 * By default, the following response handlers are provided in the specified order:
		 * <ul class='javatreec'>
		 * 	<li class='jc'>{@link ReaderProcessor}
		 * 	<li class='jc'>{@link InputStreamProcessor}
		 * 	<li class='jc'>{@link ThrowableProcessor}
		 * 	<li class='jc'>{@link HttpResponseProcessor}
		 * 	<li class='jc'>{@link HttpResourceProcessor}
		 * 	<li class='jc'>{@link HttpEntityProcessor}
		 * 	<li class='jc'>{@link ResponseBeanProcessor}
		 * 	<li class='jc'>{@link PlainTextPojoProcessor}
		 * 	<li class='jc'>{@link SerializedPojoProcessor}
		 * </ul>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Our custom response processor for Foo objects. </jc>
		 * 	<jk>public class</jk> MyResponseProcessor <jk>implements</jk> ResponseProcessor {
		 *
		 * 		<ja>@Override</ja>
		 * 		<jk>public int</jk> process(RestOpSession <jv>opSession</jv>) <jk>throws</jk> IOException {
		 *
		 * 				RestResponse <jv>res</jv> = <jv>opSession</jv>.getResponse();
		 * 				Foo <jv>foo</jv> = <jv>res</jv>.getOutput(Foo.<jk>class</jk>);
		 *
		 * 				<jk>if</jk> (<jv>foo</jv> == <jk>null</jk>)
		 * 					<jk>return</jk> <jsf>NEXT</jsf>;  <jc>// Let the next processor handle it.</jc>
		 *
		 * 				<jk>try</jk> (Writer <jv>writer</jv> = <jv>res</jv>.getNegotiatedWriter()) {
		 * 					<jc>//Pipe it to the writer ourselves.</jc>
		 * 				}
		 *
		 * 				<jk>return</jk> <jsf>FINISHED</jsf>;  <jc>// We handled it.</jc>
		 *			}
		 * 		}
		 * 	}
		 *
		 * 	<jc>// Defined via annotation.</jc>
		 * 	<ja>@Rest</ja>(responseProcessors=MyResponseProcessor.<jk>class</jk>)
		 * 	<jk>public class</jk> MyResource {
		 *
		 * 		<ja>@RestGet</ja>(...)
		 * 		<jk>public</jk> Object myMethod() {
		 * 			<jc>// Return a special object for our handler.</jc>
		 * 			<jk>return new</jk> MySpecialObject();
		 * 		}
		 * 	}
		 * </p>
		 *
		 * <p>
		 * The default response processors can overridden via any of the following:
		 * <ul class='spaced-list'>
		 * 	<li>Injected via bean store.
		 * 	<li>Class annotation:  {@link Rest#responseProcessors() @Rest(responseProcessors)}
		 * 	<li>{@link RestInject @RestInject}-annotated method:
		 * 		<p class='bjava'>
		 * 	<ja>@RestInject</ja> <jk>public</jk> [<jk>static</jk>] ResponseProcessorList myMethod(<i>&lt;args&gt;</i>) {...}
		 * 		</p>
		 * 		Args can be any injected bean including ResponseProcessorList.Builder, the default builder.
		 * </ul>
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>
		 * 		Response processors are always inherited from ascendant resources.
		 * 	<li class='note'>
		 * 		When defined as a class, the implementation must have one of the following constructors:
		 * 		<ul>
		 * 			<li><code><jk>public</jk> T(RestContext)</code>
		 * 			<li><code><jk>public</jk> T()</code>
		 * 			<li><code><jk>public static</jk> T <jsm>create</jsm>(RestContext)</code>
		 * 			<li><code><jk>public static</jk> T <jsm>create</jsm>()</code>
		 * 		</ul>
		 * 	<li class='note'>
		 * 		Inner classes of the REST resource class are allowed.
		 * </ul>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ResponseProcessors">Response Processors</a>
		 * </ul>
		 *
		 * @return The response processor list sub-builder.
		 */
		public ResponseProcessorList.Builder responseProcessors() {
			if (responseProcessors == null)
				responseProcessors = createResponseProcessors(beanStore(), resource());
			return responseProcessors;
		}

		/**
		 * Returns the REST operation args sub-builder.
		 *
		 * @return The REST operation args sub-builder.
		 */
		public RestOpArgList.Builder restOpArgs() {
			if (restOpArgs == null)
				restOpArgs = createRestOpArgs(beanStore(), resource());
			return restOpArgs;
		}

		/**
		 * Returns the bootstrap bean store.
		 *
		 * <p>
		 * This is the bean store inherited from the parent resource and does not include
		 * any beans added by this class.
		 *
		 * @return The bootstrap bean store.
		 */
		public BasicBeanStore bootstrapBeanStore() {
			return bootstrapBeanStore;
		}

		/**
		 * Returns the bootstrap (pre-runtime) variable resolver for this REST context.
		 *
		 * <p>
		 * The bootstrap resolver is used during context construction to resolve SVL variables in annotation attribute values
		 * (e.g. <c>@Rest(messages=...)</c>, <c>@Rest(config=...)</c>) before the runtime {@link VarResolver} — which has
		 * {@link Messages} and {@link Config} beans wired in — is available. It exposes the same {@link Var} catalog as
		 * the runtime resolver, but {@link LocalizationVar} and {@link ConfigVar} resolve to empty strings because their
		 * backing beans haven't been built yet.
		 *
		 * <p>
		 * To override the bootstrap resolver, declare a named {@link RestInject @RestInject} static method on the resource
		 * class:
		 * <p class='bjava'>
		 * 	<ja>@RestInject</ja>(name=<js>"bootstrapVarResolver"</js>) <jk>public static</jk> VarResolver myBootstrapResolver(<i>&lt;args&gt;</i>) {...}
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerSvlVariables">SVL Variables</a>
		 * </ul>
		 *
		 * @return The bootstrap (pre-runtime) variable resolver. Cached on first call.
		 */
		public VarResolver bootstrapVarResolver() {
			if (bootstrapVarResolver == null)
				bootstrapVarResolver = createBootstrapVarResolver(beanStore, resource, resourceClass);
			return bootstrapVarResolver;
		}

		private static void runInitHooks(BasicBeanStore beanStore, Supplier<?> resource) throws ServletException {

			var r = resource.get();

			var map = CollectionUtils.<String,MethodInfo>map();
			// @formatter:off
			// Note: pre-9.5 this filter also excluded `y.hasParameter(RestOpContext.Builder.class)` because the
			// per-op `@RestInit(RestOpContext.Builder)` flow handled those separately. That flow was deleted in
			// April 2026 refactor Route B (zero real-world callers), so the exclusion is gone too. Likewise, the
			// class-level `@RestInit(RestContext.Builder)` injection protocol was deleted in the same refactor
			// (zero non-test callers anywhere) — `RestContext.Builder` is no longer added to the bean store, so
			// any straggling `@RestInit` method that still declares either Builder type as a parameter will now
			// surface a "missing prerequisites" error here, which is the desired loud-failure signal.
			ClassInfo.ofProxy(r).getAllMethodsTopDown().stream()
				.filter(y -> y.hasAnnotation(RestInit.class))
				.forEach(y -> {
					var sig = y.getSignature();
					if (! map.containsKey(sig))
						map.put(sig, y.accessible());
				}
			);
			// @formatter:on

			for (var m : map.values()) {
				if (! beanStore.hasAllParams(m, r))
					throw servletException("Could not call @RestInit method {0}.{1}.  Could not find prerequisites: {2}.", cns(m.getDeclaringClass()), m.getSignature(), beanStore.getMissingParams(m, r));
				try {
					m.invoke(r, beanStore.getParams(m, r));
				} catch (Exception e) {
					throw servletException(e, "Exception thrown from @RestInit method {0}.{1}.", cns(m.getDeclaringClass()), m.getSignature());
				}
			}
		}

		/**
		 * Instantiates the bean context sub-builder.
		 *
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @return A new bean context sub-builder.
		 */
		protected BeanContext.Builder createBeanContext(BasicBeanStore beanStore, Supplier<?> resource) {

			// Default value.
			Value<BeanContext.Builder> v = Value.of(BeanContext.create());

			// Replace with builder from bean store.
			// @formatter:off
			beanStore
				.getBean(BeanContext.Builder.class)
				.map(BeanContext.Builder::copy)
				.ifPresent(v::set);
			// @formatter:on

			// Replace with bean from bean store.
			beanStore.getBean(BeanContext.class).ifPresent(x -> v.get().impl(x));

			return v.get();
		}

		/**
		 * Creates the bean store in this builder.
		 *
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @return A new bean store builder.
		 */
		protected BasicBeanStore.Builder createBeanStore(Supplier<?> resource) {

			// Default value.
			// @formatter:off
			var v = Value.of(
				BasicBeanStore
					.create()
					.parent(bootstrapBeanStore())
				);
			// @formatter:on

			// Apply @Rest(beanStore).
			rstream(AP.find(Rest.class, info(resourceClass))).map(x -> x.inner().beanStore()).filter(ClassUtils::isNotVoid).forEach(x -> v.get().type(x));

			// Replace with bean from:  @RestInject public [static] BasicBeanStore xxx(<args>)
			// @formatter:off
			var bs = v.get().build();
			new BeanCreateMethodFinder<>(BasicBeanStore.class, resource.get(), bs)
				.find(Builder::isRestInjectMethod)
				.run(x -> v.get().impl(x));
			// @formatter:on

			return v.get();
		}

		/**
		 * Creates the config for this builder.
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ConfigurationFiles">Configuration Files</a>
		 * 	<li class='jm'>{@link #config()}
		 * </ul>
		 *
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @param resourceClass
		 * 	The REST servlet/bean type that this context is defined against.
		 * @return A new config.
		 */
		protected Config createConfig(BasicBeanStore beanStore, Supplier<?> resource, Class<?> resourceClass) {

			var v = Value.<Config>empty();

			// Find our config file.  It's the last non-empty @RestResource(config).
			var vr = beanStore.getBean(VarResolver.class).orElseThrow(() -> new IllegalArgumentException("VarResolver not found."));
			var cfv = Value.<String>empty();
			rstream(AP.find(Rest.class, info(resourceClass))).map(x -> x.inner().config()).filter(Utils::ne).forEach(x -> cfv.set(vr.resolve(x)));
			var cf = cfv.orElse("");

			// If not specified or value is set to SYSTEM_DEFAULT, use system default config.
			if (v.isEmpty() && "SYSTEM_DEFAULT".equals(cf))
				v.set(Config.getSystemDefault());

			// Otherwise build one.
			if (v.isEmpty()) {
				Config.Builder cb = Config.create().varResolver(vr);
				if (! cf.isEmpty())
					cb.name(cf);
				v.set(cb.build());
			}

			// Replace with bean from bean store.
			beanStore.getBean(Config.class).ifPresent(v::set);

			// Replace with bean from:  @RestInject public [static] Config xxx(<args>)
			new BeanCreateMethodFinder<>(Config.class, resource.get(), beanStore).addBean(Config.class, v.get()).find(Builder::isRestInjectMethod).run(v::set);

			return v.get();
		}

		/**
		 * Instantiates the destroy method list.
		 *
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @return A new destroy method list.
		 */
		protected MethodList createDestroyMethods(BasicBeanStore beanStore, Supplier<?> resource) {

			// Default value.
			var v = Value.of(MethodList.of(getAnnotatedMethods(resource, RestDestroy.class).toList()));

			// Replace with bean from:  @RestInject(name="destroyMethods") public [static] MethodList xxx(<args>)
			new BeanCreateMethodFinder<>(MethodList.class, resource.get(), beanStore).addBean(MethodList.class, v.get()).find(x -> isRestInjectMethod(x, "destroyMethods")).run(v::set);

			return v.get();
		}

		/**
		 * Instantiates the end call method list.
		 *
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @return A new end call method list.
		 */
		protected MethodList createEndCallMethods(BasicBeanStore beanStore, Supplier<?> resource) {

			// Default value.
			Value<MethodList> v = Value.of(MethodList.of(getAnnotatedMethods(resource, RestEndCall.class).toList()));

			// Replace with bean from:  @RestInject(name="endCallMethods") public [static] MethodList xxx(<args>)
			new BeanCreateMethodFinder<>(MethodList.class, resource.get(), beanStore).addBean(MethodList.class, v.get()).find(x -> isRestInjectMethod(x, "endCallMethods")).run(v::set);

			return v.get();
		}

		/**
		 * Instantiates the JSON schema generator sub-builder.
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanSwagger2">juneau-bean-swagger-v2</a>
		 * </ul>
		 *
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @return A new JSON schema generator sub-builder.
		 */
		protected JsonSchemaGenerator.Builder createJsonSchemaGenerator(BasicBeanStore beanStore, Supplier<?> resource) {

			// Default value.
			var v = Value.of(JsonSchemaGenerator.create());

			// Replace with builder from bean store.
			beanStore.getBean(JsonSchemaGenerator.Builder.class).map(JsonSchemaGenerator.Builder::copy).ifPresent(v::set);

			// Replace with bean from bean store.
			beanStore.getBean(JsonSchemaGenerator.class).ifPresent(x -> v.get().impl(x));

			// Replace with bean from:  @RestInject public [static] JsonSchemaGenerator xxx(<args>)
			new BeanCreateMethodFinder<>(JsonSchemaGenerator.class, resource.get(), beanStore).addBean(JsonSchemaGenerator.Builder.class, v.get()).find(Builder::isRestInjectMethod).run(x -> v.get().impl(x));

			return v.get();
		}


		/**
		 * Instantiates the part parser sub-builder.
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/HttpParts">HTTP Parts</a>
		 * </ul>
		 *
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @return A new part parser sub-builder.
		 */
		protected HttpPartParser.Creator createPartParser(BasicBeanStore beanStore, Supplier<?> resource) {

			// Default value.
			Value<HttpPartParser.Creator> v = Value.of(HttpPartParser.creator().type(OpenApiParser.class));

			// Replace with builder from bean store.
			beanStore.getBean(HttpPartParser.Creator.class).map(HttpPartParser.Creator::copy).ifPresent(v::set);

			// Replace with bean from bean store.
			beanStore.getBean(HttpPartParser.class).ifPresent(x -> v.get().impl(x));

			// Replace with this bean.
			var r = resource.get();
			opt(r instanceof HttpPartParser x ? x : null).ifPresent(x -> v.get().impl(x));

			// Specify the bean type if its set as a default.
			beanStore.getBeanType(HttpPartParser.class).ifPresent(x -> v.get().type(x));

			// Replace with bean from:  @RestInject public [static] HttpPartParser xxx(<args>)
			new BeanCreateMethodFinder<>(HttpPartParser.class, resource.get(), beanStore).addBean(HttpPartParser.Creator.class, v.get()).find(Builder::isRestInjectMethod).run(x -> v.get().impl(x));

			return v.get();
		}

		/**
		 * Instantiates the part serializer sub-builder.
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/HttpParts">HTTP Parts</a>
		 * </ul>
		 *
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @return A new part serializer sub-builder.
		 */
		protected HttpPartSerializer.Creator createPartSerializer(BasicBeanStore beanStore, Supplier<?> resource) {

			// Default value.
			Value<HttpPartSerializer.Creator> v = Value.of(HttpPartSerializer.creator().type(OpenApiSerializer.class));

			// Replace with builder from bean store.
			beanStore.getBean(HttpPartSerializer.Creator.class).map(Creator::copy).ifPresent(v::set);

			// Replace with bean from bean store.
			beanStore.getBean(HttpPartSerializer.class).ifPresent(x -> v.get().impl(x));

			// Replace with this bean.
			var r = resource.get();
			opt(r instanceof HttpPartSerializer x ? x : null).ifPresent(x -> v.get().impl(x));

			// Specify the bean type if its set as a default.
			beanStore.getBeanType(HttpPartSerializer.class).ifPresent(x -> v.get().type(x));

			// Replace with bean from:  @RestInject public [static] HttpPartSerializer xxx(<args>)
			new BeanCreateMethodFinder<>(HttpPartSerializer.class, resource.get(), beanStore).addBean(HttpPartSerializer.Creator.class, v.get()).find(Builder::isRestInjectMethod).run(x -> v.get().impl(x));

			return v.get();
		}

		/**
		 * Instantiates the post-call method list.
		 *
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @return A new post-call method list.
		 */
		protected MethodList createPostCallMethods(BasicBeanStore beanStore, Supplier<?> resource) {

			// Default value.
			Value<MethodList> v = Value.of(MethodList.of(getAnnotatedMethods(resource, RestPostCall.class).toList()));

			// Replace with bean from:  @RestInject(name="postCallMethods") public [static] MethodList xxx(<args>)
			new BeanCreateMethodFinder<>(MethodList.class, resource.get(), beanStore).addBean(MethodList.class, v.get()).find(x -> isRestInjectMethod(x, "postCallMethods")).run(v::set);

			return v.get();
		}

		/**
		 * Instantiates the post-init-child-first method list.
		 *
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @return A new post-init-child-first method list.
		 */
		protected MethodList createPostInitChildFirstMethods(BasicBeanStore beanStore, Supplier<?> resource) {

			// Default value.
			var ap = AP;
			Value<MethodList> v = Value.of(MethodList.of(getAnnotatedMethods(resource, RestPostInit.class)
				.filter(m -> {
					var mi = MethodInfo.of(m);
					return rstream(ap.find(RestPostInit.class, mi))
						.map(AnnotationInfo::inner)
						.anyMatch(RestPostInit::childFirst);
				})
				.toList()));

			// Replace with bean from:  @RestInject(name="postInitChildFirstMethods") public [static] MethodList xxx(<args>)
			new BeanCreateMethodFinder<>(MethodList.class, resource.get(), beanStore).addBean(MethodList.class, v.get()).find(x -> isRestInjectMethod(x, "postInitChildFirstMethods")).run(v::set);

			return v.get();
		}

		/**
		 * Instantiates the post-init method list.
		 *
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @return A new post-init method list.
		 */
		protected MethodList createPostInitMethods(BasicBeanStore beanStore, Supplier<?> resource) {

			// Default value.
			var ap = AP;
			Value<MethodList> v = Value.of(MethodList.of(getAnnotatedMethods(resource, RestPostInit.class)
				.filter(m -> {
					var mi = MethodInfo.of(m);
					return rstream(ap.find(RestPostInit.class, mi))
						.map(AnnotationInfo::inner)
						.anyMatch(x -> ! x.childFirst());
				})
				.toList()));

			// Replace with bean from:  @RestInject(name="postInitMethods") public [static] MethodList xxx(<args>)
			new BeanCreateMethodFinder<>(MethodList.class, resource.get(), beanStore).addBean(MethodList.class, v.get()).find(x -> isRestInjectMethod(x, "postInitMethods")).run(v::set);

			return v.get();
		}

		/**
		 * Instantiates the pre-call method list.
		 *
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @return A new pre-call method list.
		 */
		protected MethodList createPreCallMethods(BasicBeanStore beanStore, Supplier<?> resource) {

			// Default value.
			Value<MethodList> v = Value.of(MethodList.of(getAnnotatedMethods(resource, RestPreCall.class).toList()));

			// Replace with bean from:  @RestInject(name="preCallMethods") public [static] MethodList xxx(<args>)
			new BeanCreateMethodFinder<>(MethodList.class, resource.get(), beanStore).addBean(MethodList.class, v.get()).find(x -> isRestInjectMethod(x, "preCallMethods")).run(v::set);

			return v.get();
		}

		/**
		 * Instantiates the response processor list sub-builder.
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ResponseProcessors">Response Processors</a>
		 * </ul>
		 *
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @return A new response processor list sub-builder.
		 */
		protected ResponseProcessorList.Builder createResponseProcessors(BasicBeanStore beanStore, Supplier<?> resource) {

			// Default value.
			// @formatter:off
			Value<ResponseProcessorList.Builder> v = Value.of(
				 ResponseProcessorList
				 	.create(beanStore)
				 	.add(
						ReaderProcessor.class,
						InputStreamProcessor.class,
						ThrowableProcessor.class,
						HttpResponseProcessor.class,
						HttpResourceProcessor.class,
						HttpEntityProcessor.class,
						ResponseBeanProcessor.class,
						PlainTextPojoProcessor.class,
						SerializedPojoProcessor.class
					)
			);
			// @formatter:on

			// Replace with bean from bean store.
			beanStore.getBean(ResponseProcessorList.class).ifPresent(x -> v.get().impl(x));

			// Replace with bean from:  @RestInject public [static] ResponseProcessorList xxx(<args>)
			new BeanCreateMethodFinder<>(ResponseProcessorList.class, resource.get(), beanStore).addBean(ResponseProcessorList.Builder.class, v.get()).find(Builder::isRestInjectMethod).run(x -> v.get().impl(x));

			return v.get();
		}

		/**
		 * Instantiates the REST children list.
		 *
		 * @param restContext The rest context.
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @return A new REST children list.
		 * @throws Exception If a problem occurred instantiating one of the child rest contexts.
		 */
		@SuppressWarnings({
			"java:S3776", // Cognitive complexity acceptable for REST children creation
			"java:S112"   // throws Exception intentional - callback/lifecycle method
		})
		protected RestChildren.Builder createRestChildren(BasicBeanStore beanStore, Supplier<?> resource, RestContext restContext) throws Exception {

			// Default value.
			Value<RestChildren.Builder> v = Value.of(RestChildren.create(beanStore).type(RestChildren.class));

			// Initialize our child resources.
			for (var o : children) {
				Supplier<?> so;
				Class<?> rc2;

				if (o instanceof Class<?> oc) {
					// Don't allow specifying yourself as a child.  Causes an infinite loop.
					if (oc == resourceClass)
						continue;
					rc2 = oc;
					if (beanStore.getBean(oc).isPresent()) {
						so = () -> beanStore.getBean(oc).get();  // If we resolved via injection, always get it this way.
					} else {
						// Decision #23 Phase C-2 (2026-04-19): the legacy ctor-takes-RestContext.Builder injection
						// protocol is dropped in 9.5.  Child resources now self-configure via @Rest annotations +
						// @RestInject members instead of imperative builder calls in their constructors.
						Object o2 = BeanCreator.of(oc, beanStore).run();
						so = () -> o2;
					}
				} else {
					rc2 = o.getClass();
					so = () -> o;
				}

			var cc = new RestContext(new Args(rc2, restContext, inner, so, "", null));

				var mi = ClassInfo.of(so.get()).getMethod(x -> x.hasName("setContext") && x.hasParameterTypes(RestContext.class)).orElse(null);
				if (nn(mi))
					mi.accessible().invoke(so.get(), cc);

				v.get().add(cc);
			}

			// Replace with bean from:  @RestInject public [static] RestChildren xxx(<args>)
			new BeanCreateMethodFinder<>(RestChildren.class, resource.get(), beanStore).addBean(RestChildren.Builder.class, v.get()).find(Builder::isRestInjectMethod).run(x -> v.get().impl(x));

			return v.get();
		}

		/**
		 * Instantiates the REST operation args sub-builder.
		 *
		 * <p>
		 * Instantiates based on the following logic:
		 * <ul>
		 * 	<li>Looks for REST op args set via any of the following:
		 * 		<ul>
		 * 			<li>{@link Rest#restOpArgs()}
		 * 			<li>{@link Rest#restOpArgs()}.
		 * 		</ul>
		 * 	<li>Looks for a static or non-static <c>createRestParams()</c> method that returns <c>{@link Class}[]</c>.
		 * 	<li>Resolves it via the bean store registered in this context.
		 * 	<li>Instantiates a default set of parameters.
		 * </ul>
		 *
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @return A new REST operation args sub-builder.
		 */
		protected RestOpArgList.Builder createRestOpArgs(BasicBeanStore beanStore, Supplier<?> resource) {

			// Default value.
			Value<RestOpArgList.Builder> v = Value.of(RestOpArgList.create(beanStore).add(AttributeArg.class, ContentArg.class, FormDataArg.class, HasFormDataArg.class, HasQueryArg.class,
				HeaderArg.class, HttpServletRequestArgs.class, HttpServletResponseArgs.class, HttpSessionArgs.class, InputStreamParserArg.class, MethodArg.class, ParserArg.class, PathArg.class,
				PathRemainderArg.class, QueryArg.class, ReaderParserArg.class, RequestBeanArg.class, ResponseBeanArg.class, ResponseHeaderArg.class, ResponseCodeArg.class, RestContextArgs.class,
				RestSessionArgs.class, RestOpContextArgs.class, RestOpSessionArgs.class, RestRequestArgs.class, RestResponseArgs.class, DefaultArg.class));

			// Replace with bean from bean store.
			beanStore.getBean(RestOpArgList.class).ifPresent(x -> v.get().impl(x));

			// Replace with bean from:  @RestInject public [static] RestOpArgList xxx(<args>)
			new BeanCreateMethodFinder<>(RestOpArgList.class, resource.get(), beanStore).addBean(RestOpArgList.Builder.class, v.get()).find(Builder::isRestInjectMethod).run(x -> v.get().impl(x));

			return v.get();
		}

		/**
		 * Instantiates the REST operations list.
		 *
		 * <p>
		 * The set of {@link RestOpContext} objects that represent the methods on this resource.
		 *
		 * @param restContext The rest context.
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @return A new REST operations list.
		 * @throws ServletException If a problem occurred instantiating one of the child rest contexts.
		 */
		@SuppressWarnings({
			"java:S3776" // Cognitive complexity acceptable for REST operations creation
		})
		protected RestOperations.Builder createRestOperations(BasicBeanStore beanStore, Supplier<?> resource, RestContext restContext) throws ServletException {

			// Default value.
			Value<RestOperations.Builder> v = Value.of(RestOperations.create(beanStore));
			var ap = restContext.getBeanContext().getAnnotationProvider();

			var rci = ClassInfo.of(resource.get());

			// April 2026 refactor (Route B, 2026-04-19): The per-op `@RestInit(RestOpContext.Builder)` protocol
			// was deleted. Audit confirmed zero real-world callers (only stale javadoc/docs examples). Users who
			// previously injected `RestOpContext.Builder` per op now configure operations declaratively via
			// `@RestOp(...)` attributes, `@RestInject`-named beans, or the class-level `@RestInit(RestContext.Builder)`
			// hook (still supported until the parent Builder is deleted). The corresponding `initMap` scan
			// (filtering for `y.hasParameter(RestOpContext.Builder.class)`) and the per-op invocation loop are gone.

			for (var mi : rci.getPublicMethods()) {
				var al = rstream(ap.find(mi)).filter(REST_OP_GROUP).collect(Collectors.toList());

				// Also include methods on @Rest-annotated interfaces.
				if (al.isEmpty()) {
					Predicate<MethodInfo> isRestAnnotatedInterface = x -> x.getDeclaringClass().isInterface()
						&& nn(x.getDeclaringClass().getAnnotations(Rest.class).findFirst().map(AnnotationInfo::inner).orElse(null));
					mi.getMatchingMethods().stream().filter(isRestAnnotatedInterface).forEach(x -> al.add(AnnotationInfo.of(x, RestOpAnnotation.DEFAULT)));
				}

				if (!al.isEmpty()) {
					try {
						if (mi.isNotPublic())
							throw servletException("@RestOp method {0}.{1} must be defined as public.", rci.inner().getName(), mi.getNameSimple());

						// `RestOpContext.create(method, context).beanStore(beanStore).type(RestOpContext.class).build()` →
						// `new RestOpContext(method, context)`. The `.beanStore(beanStore)` override is equivalent to
						// the ctor default `BasicBeanStore.of(context.getBeanStore())` since `beanStore` here IS the
						// resource-context's bean store; `.type(RestOpContext.class)` was the default. 
						var roc = new RestOpContext(mi.inner(), restContext);

						String httpMethod = roc.getHttpMethod();

						// RRPC is a special case where a method returns an interface that we
						// can perform REST calls against.
						// We override the CallMethod.invoke() method to insert our logic.
						if ("RRPC".equals(httpMethod)) {

							// `RestOpContext.create(method, context).beanStore(restContext.getBootstrapBeanStore()).type(RrpcRestOpContext.class).build()` →
							// `new RrpcRestOpContext(method, context)`. The bean-store override (root, not the
							// resource-layered store) is preserved verbatim inside the new 2-arg ctor on
							// `RrpcRestOpContext`.
							// RRPC operations auto-append `/*` inside `Builder.getPathMatchers()`.
							RestOpContext roc2 = new RrpcRestOpContext(mi.inner(), restContext);
							v.get()
								.add("GET", roc2)
								.add("POST", roc2);

						} else {
							v.get().add(roc);
						}
					} catch (Exception e) {
						throw servletException(e, "Problem occurred trying to initialize methods on class {0}", rci.inner().getName());
					}
				}
			}

			// Replace with bean from:  @RestInject public [static] RestOperations xxx(<args>)
			new BeanCreateMethodFinder<>(RestOperations.class, resource.get(), beanStore).addBean(RestOperations.Builder.class, v.get()).find(Builder::isRestInjectMethod).run(x -> v.get().impl(x));

			return v.get();
		}

		/**
		 * Instantiates the start call method list.
		 *
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @return A new start call method list.
		 */
		protected MethodList createStartCallMethods(BasicBeanStore beanStore, Supplier<?> resource) {

			// Default value.
			Value<MethodList> v = Value.of(MethodList.of(getAnnotatedMethods(resource, RestStartCall.class).toList()));

			// Replace with bean from:  @RestInject(name="startCallMethods") public [static] MethodList xxx(<args>)
			new BeanCreateMethodFinder<>(MethodList.class, resource.get(), beanStore).addBean(MethodList.class, v.get()).find(x -> isRestInjectMethod(x, "startCallMethods")).run(v::set);

			return v.get();
		}

		/**
		 * Creates the bootstrap (pre-runtime) variable resolver.
		 *
		 * <p>
		 * Builds the same {@link Var} catalog as the runtime resolver but without {@link Messages} or {@link Config}
		 * beans wired in — those depend on settings that are themselves resolved against this resolver, so they're
		 * added later by {@link RestContext#findVarResolver()}. Override via
		 * {@link RestInject @RestInject(name="bootstrapVarResolver")} on a static method.
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerSvlVariables">SVL Variables</a>
		 * </ul>
		 *
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @param resourceClass
		 * 	The REST servlet/bean type that this context is defined against.
		 * @return The built bootstrap variable resolver.
		 */
		protected VarResolver createBootstrapVarResolver(BasicBeanStore beanStore, Supplier<?> resource, Class<?> resourceClass) {

			// Default value.
			// @formatter:off
			Value<VarResolver> v = Value.of(
				VarResolver
					.create()
					.defaultVars()
					.vars(
						VarList.of(
							ConfigVar.class,
							FileVar.class,
							LocalizationVar.class,
							RequestAttributeVar.class,
							RequestFormDataVar.class,
							RequestHeaderVar.class,
							RequestPathVar.class,
							RequestQueryVar.class,
							RequestVar.class,
							RequestSwaggerVar.class,
							SerializedRequestAttrVar.class,
							ServletInitParamVar.class,
							SwaggerVar.class,
							UrlVar.class,
							UrlEncodeVar.class,
							HtmlWidgetVar.class
						)
						.addDefault()
					)
					.bean(FileFinder.class, FileFinder.create(beanStore).cp(resourceClass,null,true).build())
					.build()
			);
			// @formatter:on

			// Replace with named bean from bean store (PROP_bootstrapVarResolver).
			beanStore.getBean(VarResolver.class, PROP_bootstrapVarResolver).ifPresent(v::set);

			// Replace with bean from:  @RestInject(name="bootstrapVarResolver") public [static] VarResolver xxx(<args>)
			new BeanCreateMethodFinder<>(VarResolver.class, resource.get(), beanStore).find(x -> isRestInjectMethod(x, PROP_bootstrapVarResolver)).run(v::set);

			return v.get();
		}
	}

	/**
	 * Applies {@link Rest} annotations to a {@link Builder}.
	 *
	 * <p>
	 * Wired into the annotation-processing machinery via {@link org.apache.juneau.annotation.ContextApply @ContextApply}
	 * on {@link Rest}. Moved into {@code RestContext} during the April 2026 refactor (2026-04-19) so that
	 * {@link Builder} can be demoted to package-private without breaking the cross-package reference
	 * that used to live in {@code RestAnnotation.RestContextApply}.
	 */
	public static class RestContextApply extends org.apache.juneau.AnnotationApplier<Rest,Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public RestContextApply(VarResolverSession vr) {
			super(Rest.class, Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<Rest> ai, Builder b) {
			Rest a = ai.inner();

		b.children((java.lang.Object[])a.children());
			string(a.path()).ifPresent(b::path);
		}
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

	protected final AtomicBoolean initialized = new AtomicBoolean(false);
	protected final BasicHttpException initException;
	protected final BasicBeanStore beanStore;
	protected final BasicBeanStore bootstrapBeanStore;
	protected final Builder builder;
	protected final Class<?> resourceClass;
	protected final ConcurrentHashMap<Locale,Swagger> swaggerCache = new ConcurrentHashMap<>();
	protected final Instant startTime;
	protected final RestContext parentContext;
	protected final String fullPath;
	protected final String path;
	protected final ThreadLocal<RestSession> localSession = new ThreadLocal<>();
	protected final UrlPathMatcher pathMatcher;
	private final Supplier<?> resource;

	// Private accessors used by memoizer lambdas to satisfy Java's definite-assignment rules for blank final fields.
	private BasicBeanStore beanStore() { return beanStore; }
	private Builder builder() { return builder; }
	private Supplier<?> resource() { return resource; }
	private Class<?> resourceClass() { return resourceClass; }
	private RestContext parentContext() { return parentContext; }
	private RestOperations restOperations() { return restOperations.get(); }

	private static final class LifecycleInvokerPair {
		final MethodList methods;
		final MethodInvoker[] invokers;

		LifecycleInvokerPair(MethodList methods, MethodInvoker[] invokers) {
			this.methods = methods;
			this.invokers = invokers;
		}
	}

	/**
	 * The {@link BeanContext} for this resource.
	 *
	 * <p>
	 * Built from the builder's sub-builder, which is lazy-created and then mutated by the annotation
	 * apply pass during {@code init()} before this memoizer fires — so the builder is in its final
	 * configured state by the time {@code build()} is called.
	 */
	private final Memoizer<BeanContext> beanContext = memoizer(() -> builder().beanContext().build());

	/**
	 * The bootstrap-time {@link VarResolver} — no {@link Messages} or {@link Config} bean available yet.
	 *
	 * <p>
	 * Cached on the builder so that {@code init()} and the {@link #messages} / {@link #config}
	 * memoizers all see the same instance during construction.
	 */
	private final Memoizer<VarResolver> bootstrapVarResolver = memoizer(() -> builder().bootstrapVarResolver());

	/**
	 * The {@link CallLogger} for this resource.
	 *
	 * <p>
	 * Defaults to {@link BasicCallLogger}. {@code @Rest(callLogger=X)} most-derived non-{@code Void} class wins.
	 * A bean-store override or {@code @RestInject} factory method REPLACES the result.
	 */
	private final Memoizer<CallLogger> callLogger = memoizer(() -> {
		var bs = beanStore();
		var creator = BeanCreator.of(CallLogger.class, bs).type(BasicCallLogger.class);
		// Order matters — annotations override defaults so they need to be applied last.
		bs.getBeanType(CallLogger.class).ifPresent(creator::type);
		// @Rest(callLogger=X) — most-derived non-Void wins.
		// getRestAnnotationsForProperty(...) yields parent-to-child order (rstream reversal); reduce-last
		// keeps the most-derived value, mirroring the prior "apply each annotation, child overrides parent" semantics.
		getRestAnnotationsForProperty(PROPERTY_callLogger)
			.map(ai -> ai.inner().callLogger())
			.filter(c -> c != CallLogger.Void.class)
			.reduce((first, second) -> second)
			.ifPresent(creator::type);
		bs.getBean(CallLogger.class).ifPresent(creator::impl);
		new BeanCreateMethodFinder<>(CallLogger.class, resource().get(), bs).find(Builder::isRestInjectMethod).run(creator::impl);
		return creator.orElse(null);
	});

	/**
	 * The resolving {@link Config} for this resource.
	 *
	 * <p>
	 * Wraps the builder's unresolved config with a {@link VarResolver} session so that SVL variables
	 * in config values are expanded on access.
	 */
	private final Memoizer<Config> config = memoizer(() -> builder().config().resolving(getVarResolver().createSession()));

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

	/**
	 * The {@link DebugEnablement} for this resource.
	 *
	 * <p>
	 * Resolved from {@code @Rest(debugDefault)} (most-derived non-blank wins), falling back to
	 * {@code @Rest(debug=true|false)}. The resolved {@link Enablement} is published into the bean store
	 * so that {@link BasicDebugEnablement} subclasses can pick it up. Defaults to {@link BasicDebugEnablement}.
	 * A bean-store override or {@code @RestInject} factory method REPLACES the result.
	 */
	private final Memoizer<DebugEnablement> debugEnablement = memoizer(() -> {
		// @Rest(debugDefault="ALWAYS|NEVER|CONDITIONAL") — most-derived non-blank value wins, with parent inheritance
		// gated by @Rest(noInherit={"debugDefault"}). Resolved value is published as an Enablement bean so that
		// BasicDebugEnablement.init() (and any subclass) can pick it up via beanStore().getBean(Enablement.class).
		// Annotation value overrides any pre-registered Enablement bean (e.g. mock-client default).
		// If neither a debugDefault annotation value NOR a pre-registered Enablement bean is present, fall back
		// to the @Rest(debug=true|false) boolean flag — ALWAYS when set, NEVER otherwise.
		var bs = beanStore();
		String debugDefaultStr = mergeReplacedStringAttribute(PROPERTY_debugDefault, env("RestContext.debugDefault").orElse(null));
		Enablement resolvedDebugDefault = null;
		if (nn(debugDefaultStr) && !debugDefaultStr.isBlank())
			resolvedDebugDefault = Enablement.fromString(debugDefaultStr);
		if (nn(resolvedDebugDefault))
			bs.addBean(Enablement.class, resolvedDebugDefault);
		else if (bs.getBean(Enablement.class).isEmpty())
			bs.addBean(Enablement.class, builder().isDebug() ? Enablement.ALWAYS : Enablement.NEVER);
		var creator = BeanCreator.of(DebugEnablement.class, bs).type(BasicDebugEnablement.class);
		// Order matters — annotations override defaults so they need to be applied last.
		bs.getBeanType(DebugEnablement.class).ifPresent(creator::type);
		// @Rest(debugEnablement=X) — most-derived non-Void wins. See callLogger for the reduce-last rationale.
		getRestAnnotationsForProperty(PROPERTY_debugEnablement)
			.map(ai -> ai.inner().debugEnablement())
			.filter(c -> c != DebugEnablement.Void.class)
			.reduce((first, second) -> second)
			.ifPresent(creator::type);
		bs.getBean(DebugEnablement.class).ifPresent(creator::impl);
		new BeanCreateMethodFinder<>(DebugEnablement.class, resource().get(), bs).find(Builder::isRestInjectMethod).run(creator::impl);
		return creator.orElse(null);
	});

	/**
	 * The default request attributes contributed by {@code @Rest(defaultRequestAttributes)} for this resource.
	 *
	 * <p>
	 * Walks {@code @Rest} annotations parent-to-child, resolving each attribute string and parsing it as a
	 * key=value or key:value pair. A named bean-store override or {@code @RestInject} factory method REPLACES
	 * the accumulated result.
	 */
	private final Memoizer<NamedAttributeMap> defaultRequestAttributes = memoizer(() -> {
		var v = Value.of(NamedAttributeMap.create());
		var anns = new ArrayList<>(getRestAnnotations());
		Collections.reverse(anns);
		anns.forEach(ai -> Arrays.stream(ai.inner().defaultRequestAttributes())
			.filter(StringUtils::isNotBlank)
			.map(this::resolve)
			.filter(StringUtils::isNotBlank)
			.map(BasicNamedAttribute::ofPair)
			.forEach(v.get()::add));
		beanStore().getBean(NamedAttributeMap.class, PROP_defaultRequestAttributes).ifPresent(v::set);
		new BeanCreateMethodFinder<>(NamedAttributeMap.class, resource().get(), beanStore()).addBean(NamedAttributeMap.class, v.get()).find(x -> Builder.isRestInjectMethod(x, PROP_defaultRequestAttributes)).run(v::set);
		return v.get();
	});

	/**
	 * The default request headers contributed by {@code @Rest(defaultRequestHeaders)},
	 * {@code @Rest(defaultAccept)}, and {@code @Rest(defaultContentType)} for this resource.
	 *
	 * <p>
	 * Walks {@code @Rest} annotations parent-to-child, resolving each header string. A named bean-store
	 * override or {@code @RestInject} factory method REPLACES the accumulated result.
	 */
	private final Memoizer<HeaderList> defaultRequestHeaders = memoizer(() -> {
		var v = Value.of(HeaderList.create());
		var anns = new ArrayList<>(getRestAnnotations());
		Collections.reverse(anns);
		anns.forEach(ai -> {
			Rest a = ai.inner();
			Arrays.stream(a.defaultRequestHeaders()).filter(StringUtils::isNotBlank).map(this::resolve).filter(StringUtils::isNotBlank).map(s -> stringHeader(s)).forEach(v.get()::setDefault);
			var defaultAccept = resolve(a.defaultAccept());
			if (isNotBlank(defaultAccept))
				v.get().setDefault(accept(defaultAccept));
			var defaultContentType = resolve(a.defaultContentType());
			if (isNotBlank(defaultContentType))
				v.get().setDefault(contentType(defaultContentType));
		});
		beanStore().getBean(HeaderList.class, PROP_defaultRequestHeaders).ifPresent(v::set);
		new BeanCreateMethodFinder<>(HeaderList.class, resource().get(), beanStore()).addBean(HeaderList.class, v.get()).find(x -> Builder.isRestInjectMethod(x, PROP_defaultRequestHeaders)).run(v::set);
		return v.get();
	});

	/**
	 * The default response headers contributed by {@code @Rest(defaultResponseHeaders)} for this resource.
	 *
	 * <p>
	 * Walks {@code @Rest} annotations parent-to-child, resolving each header string. A named bean-store
	 * override or {@code @RestInject} factory method REPLACES the accumulated result.
	 */
	private final Memoizer<HeaderList> defaultResponseHeaders = memoizer(() -> {
		var v = Value.of(HeaderList.create());
		var anns = new ArrayList<>(getRestAnnotations());
		Collections.reverse(anns);
		anns.forEach(ai -> Arrays.stream(ai.inner().defaultResponseHeaders()).filter(StringUtils::isNotBlank).map(this::resolve).filter(StringUtils::isNotBlank).map(s -> stringHeader(s)).forEach(v.get()::setDefault));
		beanStore().getBean(HeaderList.class, PROP_defaultResponseHeaders).ifPresent(v::set);
		new BeanCreateMethodFinder<>(HeaderList.class, resource().get(), beanStore()).addBean(HeaderList.class, v.get()).find(x -> Builder.isRestInjectMethod(x, PROP_defaultResponseHeaders)).run(v::set);
		return v.get();
	});

	/**
	 * Methods annotated with {@link org.apache.juneau.rest.annotation.RestDestroy @RestDestroy} and their invokers.
	 */
	private final Memoizer<LifecycleInvokerPair> destroyInvokerPair = memoizer(() -> buildLifecycleInvokerPair(() -> builder().createDestroyMethods(beanStore(), resource())));

	/**
	 * Fully-configured {@link EncoderSet.Builder} for this resource, populated from the
	 * {@code @Rest(encoders)} annotation chain and any {@code @RestInject} override.
	 *
	 * <p>
	 * Starts with {@link IdentityEncoder} as the implicit default. A bean-store type override
	 * or bean-store instance override REPLACES the builder or its impl. Annotation entries
	 * (parent-to-child) are appended after the default. A {@code @RestInject} factory method
	 * REPLACES the impl with the returned value.
	 */
	private final Memoizer<EncoderSet.Builder> encodersBuilder = memoizer(() -> {
		var bs = beanStore();
		var v = Value.of(EncoderSet.create(bs));
		bs.getBeanType(EncoderSet.class).ifPresent(x -> v.get().type(x));
		bs.getBean(EncoderSet.class).ifPresent(x -> v.get().impl(x));
		getRestAnnotationsForProperty(PROPERTY_encoders).forEach(ai -> v.get().add(ai.inner().encoders()));
		new BeanCreateMethodFinder<>(EncoderSet.class, resource().get(), bs).addBean(EncoderSet.Builder.class, v.get()).find(Builder::isRestInjectMethod).run(x -> v.get().impl(x));
		return v.get();
	});

	/**
	 * The {@link EncoderSet} for this resource, built from the self-contained encoder builder memoizer.
	 */
	private final Memoizer<EncoderSet> encoders = memoizer(() -> encodersBuilder.get().build());

	/**
	 * Methods annotated with {@link org.apache.juneau.rest.annotation.RestEndCall @RestEndCall} and their invokers.
	 */
	private final Memoizer<LifecycleInvokerPair> endCallInvokerPair = memoizer(() -> buildLifecycleInvokerPair(() -> builder().createEndCallMethods(beanStore(), resource())));

	/**
	 * The {@link JsonSchemaGenerator} for this resource, built from the builder's JSON-schema sub-builder.
	 */
	private final Memoizer<JsonSchemaGenerator> jsonSchemaGenerator = memoizer(() -> builder().jsonSchemaGenerator().build());

	/**
	 * The {@link Logger} for this resource.
	 *
	 * <p>
	 * Defaults to {@code Logger.getLogger(resourceClass.getName())}. A bean-store override or
	 * {@code @RestInject} factory method REPLACES the default.
	 */
	private final Memoizer<Logger> logger = memoizer(() -> {
		var v = Value.of(Logger.getLogger(cn(resourceClass())));
		beanStore().getBean(Logger.class).ifPresent(v::set);
		new BeanCreateMethodFinder<>(Logger.class, resource().get(), beanStore()).addBean(Logger.class, v.get()).find(Builder::isRestInjectMethod).run(v::set);
		return v.get();
	});

	/**
	 * The {@link Messages} bundle for this resource.
	 *
	 * <p>
	 * Walks {@code @Rest} annotations parent-to-child, resolving each {@code messages} location string
	 * against the bootstrap resolver (full resolver not yet available — it depends on {@code getMessages()}).
	 * A bean-store override or {@code @RestInject} factory method REPLACES the result.
	 */
	private final Memoizer<Messages> messages = memoizer(() -> {
		var b = Messages.create(resourceClass());
		// Walk @Rest annotations parent-to-child; child wins because location() prepends.
		var anns = new ArrayList<>(getRestAnnotations());
		Collections.reverse(anns);
		// Resolve location strings against the simple resolver — full resolver isn't available yet
		// (it depends on getMessages()).
		var vrs = getBootstrapVarResolver().createSession();
		anns.forEach(ai -> ai.getString(PROPERTY_messages).filter(StringUtils::isNotBlank).ifPresent(s -> b.location(vrs.resolve(s))));
		beanStore().getBean(Messages.class).ifPresent(b::impl);
		new BeanCreateMethodFinder<>(Messages.class, resource().get(), beanStore()).addBean(Messages.Builder.class, b).find(Builder::isRestInjectMethod).run(b::impl);
		return b.build();
	});

	/**
	 * The {@link MethodExecStore} for this resource, wired to the {@link ThrownStore}.
	 *
	 * <p>
	 * A bean-store override or {@code @RestInject} factory method REPLACES the result.
	 */
	private final Memoizer<MethodExecStore> methodExecStore = memoizer(() -> {
		var bs = beanStore();
		var b = MethodExecStore.create(bs).thrownStoreOnce(getThrownStore());
		bs.getBeanType(MethodExecStore.class).ifPresent(b::type);
		bs.getBean(MethodExecStore.class).ifPresent(b::impl);
		new BeanCreateMethodFinder<>(MethodExecStore.class, resource().get(), bs).addBean(MethodExecStore.Builder.class, b).find(Builder::isRestInjectMethod).run(b::impl);
		return b.build();
	});

	/**
	 * Fully-configured {@link ParserSet.Builder} for this resource, populated from the
	 * {@code @Rest(parsers)} annotation chain and any {@code @RestInject} override.
	 *
	 * <p>
	 * Starts with an empty set. A bean-store type or instance override REPLACES the builder or its impl.
	 * Annotation entries (parent-to-child) are appended. A {@code @RestInject} factory method REPLACES the impl.
	 */
	private final Memoizer<ParserSet.Builder> parsersBuilder = memoizer(() -> {
		var bs = beanStore();
		var v = Value.of(ParserSet.create(bs));
		bs.getBeanType(ParserSet.class).ifPresent(x -> v.get().type(x));
		bs.getBean(ParserSet.class).ifPresent(x -> v.get().impl(x));
		getRestAnnotationsForProperty(PROPERTY_parsers).forEach(ai -> v.get().add(ai.inner().parsers()));
		new BeanCreateMethodFinder<>(ParserSet.class, resource().get(), bs).addBean(ParserSet.Builder.class, v.get()).find(Builder::isRestInjectMethod).run(x -> v.get().impl(x));
		return v.get();
	});

	/**
	 * The {@link ParserSet} for this resource, built from the self-contained parser builder memoizer.
	 */
	private final Memoizer<ParserSet> parsers = memoizer(() -> parsersBuilder.get().build());

	/**
	 * The {@link HttpPartParser} for this resource.
	 *
	 * <p>
	 * Uses the builder's part-parser creator (which has general config annotations applied by {@code init()}).
	 * Adds the {@code @Rest(partParser)} type override from the most specific (child-most)
	 * annotation in the hierarchy, replacing the factory default.
	 */
	private final Memoizer<HttpPartParser> partParser = memoizer(() -> {
		var creator = builder().partParser();
		getRestAnnotationsForProperty(PROPERTY_partParser)
			.map(ai -> ai.inner().partParser())
			.filter(ClassUtils::isNotVoid)
			.reduce((a, b) -> b)
			.ifPresent(creator::type);
		return creator.create();
	});

	/**
	 * The {@link HttpPartSerializer} for this resource.
	 *
	 * <p>
	 * Uses the builder's part-serializer creator (which has general config annotations applied by {@code init()}).
	 * Adds the {@code @Rest(partSerializer)} type override from the most specific (child-most)
	 * annotation in the hierarchy, replacing the factory default.
	 */
	private final Memoizer<HttpPartSerializer> partSerializer = memoizer(() -> {
		var creator = builder().partSerializer();
		getRestAnnotationsForProperty(PROPERTY_partSerializer)
			.map(ai -> ai.inner().partSerializer())
			.filter(ClassUtils::isNotVoid)
			.reduce((a, b) -> b)
			.ifPresent(creator::type);
		return creator.create();
	});

	/**
	 * Methods annotated with {@link org.apache.juneau.rest.annotation.RestPostCall @RestPostCall}.
	 */
	private final Memoizer<MethodList> postCallMethods = memoizer(() -> builder().createPostCallMethods(beanStore(), resource()));

	/**
	 * Methods annotated with {@link org.apache.juneau.rest.annotation.RestPostInit @RestPostInit}{@code (childFirst=true)} and their invokers.
	 */
	private final Memoizer<LifecycleInvokerPair> postInitChildFirstInvokerPair = memoizer(() -> buildLifecycleInvokerPair(() -> builder().createPostInitChildFirstMethods(beanStore(), resource())));

	/**
	 * Methods annotated with {@link org.apache.juneau.rest.annotation.RestPostInit @RestPostInit} and their invokers.
	 */
	private final Memoizer<LifecycleInvokerPair> postInitInvokerPair = memoizer(() -> buildLifecycleInvokerPair(() -> builder().createPostInitMethods(beanStore(), resource())));

	/**
	 * Methods annotated with {@link org.apache.juneau.rest.annotation.RestPreCall @RestPreCall}.
	 */
	private final Memoizer<MethodList> preCallMethods = memoizer(() -> builder().createPreCallMethods(beanStore(), resource()));

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
	 * The ordered array of {@link ResponseProcessor} instances for this resource.
	 *
	 * <p>
	 * Walks {@code @Rest(responseProcessors)} annotations parent-to-child (append order). A bean-store
	 * override or {@code @RestInject} factory method REPLACES the entire list.
	 */
	private final Memoizer<ResponseProcessor[]> responseProcessors = memoizer(() -> {
		// Walk @Rest(responseProcessors=...) chain (parent-to-child via getRestAnnotationsForProperty).
		// DefaultConfig contributes the framework defaults at the top of the chain; resource-class entries append.
		// ResponseProcessorList.Builder.add(...) uses addAll (append) — final order: [DefaultConfig, parent, child].
		var bs = beanStore();
		var v = Value.of(ResponseProcessorList.create(bs));
		getRestAnnotationsForProperty(PROPERTY_responseProcessors)
			.forEach(ai -> v.get().add(ai.inner().responseProcessors()));
		// Bean-store override REPLACES the entire annotation-derived list.
		bs.getBean(ResponseProcessorList.class).ifPresent(x -> v.get().impl(x));
		// @RestInject method override REPLACES the entire annotation-derived list.
		new BeanCreateMethodFinder<>(ResponseProcessorList.class, resource().get(), bs)
			.addBean(ResponseProcessorList.Builder.class, v.get())
			.find(Builder::isRestInjectMethod)
			.run(x -> v.get().impl(x));
		return v.get().build().toArray();
	});

	/**
	 * The ordered array of {@link RestOpArg} argument-resolver types for this resource.
	 *
	 * <p>
	 * Walks {@code @Rest(restOpArgs)} annotations parent-to-child (prepend order, so child entries
	 * take priority). A bean-store override or {@code @RestInject} factory method REPLACES the entire list.
	 */
	private final Memoizer<Class<? extends RestOpArg>[]> restOpArgs = memoizer(() -> {
		// Walk @Rest(restOpArgs=...) chain (parent-to-child via getRestAnnotationsForProperty).
		// DefaultConfig contributes the framework arg-resolver list; resource-class entries override.
		// RestOpArgList.Builder.add(...) uses prependAll — applying per-annotation in chain order yields
		// final order: [child, parent, DefaultConfig], matching the legacy apply-pass behavior.
		var bs = beanStore();
		var v = Value.of(RestOpArgList.create(bs));
		getRestAnnotationsForProperty(PROPERTY_restOpArgs)
			.forEach(ai -> v.get().add(ai.inner().restOpArgs()));
		// Bean-store override REPLACES the entire annotation-derived list.
		bs.getBean(RestOpArgList.class).ifPresent(x -> v.get().impl(x));
		// @RestInject method override REPLACES the entire annotation-derived list.
		new BeanCreateMethodFinder<>(RestOpArgList.class, resource().get(), bs)
			.addBean(RestOpArgList.Builder.class, v.get())
			.find(Builder::isRestInjectMethod)
			.run(x -> v.get().impl(x));
		return v.get().build().asArray();
	});

	/**
	 * Fully-configured {@link SerializerSet.Builder} for this resource, populated from the
	 * {@code @Rest(serializers)} annotation chain and any {@code @RestInject} override.
	 *
	 * <p>
	 * Starts with an empty set. A bean-store type or instance override REPLACES the builder or its impl.
	 * Annotation entries (parent-to-child) are appended. A {@code @RestInject} factory method REPLACES the impl.
	 */
	private final Memoizer<SerializerSet.Builder> serializersBuilder = memoizer(() -> {
		var bs = beanStore();
		var v = Value.of(SerializerSet.create(bs));
		bs.getBeanType(SerializerSet.class).ifPresent(x -> v.get().type(x));
		bs.getBean(SerializerSet.class).ifPresent(x -> v.get().impl(x));
		getRestAnnotationsForProperty(PROPERTY_serializers).forEach(ai -> v.get().add(ai.inner().serializers()));
		new BeanCreateMethodFinder<>(SerializerSet.class, resource().get(), bs).addBean(SerializerSet.Builder.class, v.get()).find(Builder::isRestInjectMethod).run(x -> v.get().impl(x));
		return v.get();
	});

	/**
	 * The {@link SerializerSet} for this resource, built from the self-contained serializer builder memoizer.
	 */
	private final Memoizer<SerializerSet> serializers = memoizer(() -> serializersBuilder.get().build());

	/**
	 * Methods annotated with {@link org.apache.juneau.rest.annotation.RestStartCall @RestStartCall} and their invokers.
	 */
	private final Memoizer<LifecycleInvokerPair> startCallInvokerPair = memoizer(() -> buildLifecycleInvokerPair(() -> builder().createStartCallMethods(beanStore(), resource())));

	/**
	 * The {@link StaticFiles} provider for this resource.
	 *
	 * <p>
	 * Defaults to {@link BasicStaticFiles}. {@code @Rest(staticFiles=X)} most-derived non-{@code Void} class wins.
	 * A bean-store override or {@code @RestInject} factory method REPLACES the result.
	 */
	private final Memoizer<StaticFiles> staticFiles = memoizer(() -> {
		var bs = beanStore();
		var creator = BeanCreator.of(StaticFiles.class, bs).type(BasicStaticFiles.class);
		// Order matters — annotations override defaults so they need to be applied last.
		bs.getBeanType(StaticFiles.class).ifPresent(creator::type);
		// @Rest(staticFiles=X) — most-derived non-Void wins. See callLogger for the reduce-last rationale.
		getRestAnnotationsForProperty(PROPERTY_staticFiles)
			.map(ai -> ai.inner().staticFiles())
			.filter(c -> c != StaticFiles.Void.class)
			.reduce((first, second) -> second)
			.ifPresent(creator::type);
		bs.getBean(StaticFiles.class).ifPresent(creator::impl);
		new BeanCreateMethodFinder<>(StaticFiles.class, resource().get(), bs).find(Builder::isRestInjectMethod).run(creator::impl);
		return creator.orElse(null);
	});

	/**
	 * The {@link SwaggerProvider} for this resource.
	 *
	 * <p>
	 * Defaults to {@link BasicSwaggerProvider}. {@code @Rest(swaggerProvider=X)} most-derived
	 * non-{@code Void} class wins. A bean-store override or {@code @RestInject} factory method REPLACES the result.
	 */
	private final Memoizer<SwaggerProvider> swaggerProvider = memoizer(() -> {
		var bs = beanStore();
		var creator = BeanCreator.of(SwaggerProvider.class, bs).type(BasicSwaggerProvider.class);
		// Order matters — annotations override defaults so they need to be applied last.
		bs.getBeanType(SwaggerProvider.class).ifPresent(creator::type);
		// @Rest(swaggerProvider=X) — most-derived non-Void wins. See callLogger for the reduce-last rationale.
		getRestAnnotationsForProperty(PROPERTY_swaggerProvider)
			.map(ai -> ai.inner().swaggerProvider())
			.filter(c -> c != SwaggerProvider.Void.class)
			.reduce((first, second) -> second)
			.ifPresent(creator::type);
		bs.getBean(SwaggerProvider.class).ifPresent(creator::impl);
		new BeanCreateMethodFinder<>(SwaggerProvider.class, resource().get(), bs).find(Builder::isRestInjectMethod).run(creator::impl);
		return creator.orElse(null);
	});

	/**
	 * The {@link ThrownStore} for this resource, used to track exception statistics.
	 *
	 * <p>
	 * Inherits from the parent context's store when one is present. A bean-store override or
	 * {@code @RestInject} factory method REPLACES the result.
	 */
	private final Memoizer<ThrownStore> thrownStore = memoizer(() -> {
		var bs = beanStore();
		var b = ThrownStore.create(bs).impl(parentContext() == null ? null : parentContext().getThrownStore());
		bs.getBeanType(ThrownStore.class).ifPresent(b::type);
		bs.getBean(ThrownStore.class).ifPresent(b::impl);
		new BeanCreateMethodFinder<>(ThrownStore.class, resource().get(), bs).addBean(ThrownStore.Builder.class, b).find(Builder::isRestInjectMethod).run(b::impl);
		return b.build();
	});

	/**
	 * The runtime {@link VarResolver} — wraps the bootstrap resolver and adds {@link Messages} and {@link Config} beans.
	 *
	 * <p>
	 * The bootstrap {@link Config} is pulled from the builder to avoid a circular dependency: the runtime
	 * {@link Config} wraps the bootstrap config in a session backed by this resolver.
	 * A bean-store override or {@code @RestInject} factory method REPLACES the result.
	 */
	private final Memoizer<VarResolver> varResolver = memoizer(() -> {
		var bs = beanStore();
		var b = getBootstrapVarResolver().copy()
			.bean(Messages.class, getMessages())
			.bean(Config.class, builder().config());
		bs.getBean(VarResolver.class).ifPresent(b::impl);
		new BeanCreateMethodFinder<>(VarResolver.class, resource().get(), bs).addBean(VarResolver.Builder.class, b).find(Builder::isRestInjectMethod).run(b::impl);
		return b.build();
	});

	/**
	 * The {@link RestOperations} for this resource — all {@link RestOpContext} instances built from
	 * methods annotated with {@link org.apache.juneau.rest.annotation.RestOp @RestOp} (and related).
	 *
	 * <p>
	 * Eagerly initialized in the constructor (via an explicit {@code .get()} call inside the try-catch block)
	 * so that any {@link ServletException} thrown by {@link Builder#createRestOperations} surfaces at
	 * construction time rather than lazily on first request. Subsequent calls return the cached instance.
	 */
	private final Memoizer<RestOperations> restOperations = memoizer(() -> {
		try {
			return builder().createRestOperations(beanStore(), resource(), this).build();
		} catch (ServletException e) {
			throw new RuntimeException(e);
		}
	});

	/**
	 * The {@link RestChildren} for this resource — child {@link RestContext} instances registered via
	 * {@link Rest#children() @Rest(children)}.
	 *
	 * <p>
	 * Eagerly initialized in the constructor (via an explicit {@code .get()} call inside the try-catch block)
	 * so that any exception thrown by {@link Builder#createRestChildren} surfaces at construction time
	 * rather than lazily. Subsequent calls return the cached instance.
	 */
	private final Memoizer<RestChildren> restChildren = memoizer(() -> {
		try {
			return builder().createRestChildren(beanStore(), resource(), this).build();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	});

	/**
	 * Constructor.
	 *
	 * @param args The bootstrap arguments. Must not be <jk>null</jk>.
	 * @throws Exception If any initialization problems were encountered.
	 * @since 9.5.0
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
	private RestContext(Builder builder) throws Exception {
		super(builder);

		startTime = Instant.now();

		REGISTRY.put(builder.resourceClass, this);

		BasicHttpException initExceptionTemp = null;

		try {
			this.builder = builder;

			parentContext = builder.parentContext;
			resource = builder.resource;
			resourceClass = builder.resourceClass;
			bootstrapBeanStore = builder.bootstrapBeanStore();

			BasicBeanStore bs = beanStore = builder.beanStore();
			// @formatter:off
			beanStore
				.addBean(BasicBeanStore.class, beanStore)
				.addBean(RestContext.class, this)
				.addBean(Object.class, resource.get())
				.addBean(Builder.class, builder)
				.addBean(AnnotationWorkList.class, builder.getApplied());
			// @formatter:on

			path = nn(builder.path) ? builder.path : "";
			fullPath = (parentContext == null ? "" : (parentContext.fullPath + '/')) + path;
			var p = path;
			if (! p.endsWith("/*"))
				p += "/*";
			pathMatcher = UrlPathMatcher.of(p);

			bs.addBean(BeanContext.class, getBeanContext());
			bs.add(EncoderSet.class, getEncoders());
			bs.add(SerializerSet.class, getSerializers());
			bs.add(ParserSet.class, getParsers());
			var lg = getLogger();
			bs.addBean(Logger.class, lg);
			bs.addBean(java.util.logging.Logger.class, lg); // Also register under java.util.logging.Logger for CallLogger compatibility
			bs.addBean(ThrownStore.class, getThrownStore());
			bs.addBean(MethodExecStore.class, getMethodExecStore());
			var msgs = getMessages();
			bs.addBean(Messages.class, msgs);
			bs.add(VarResolver.class, getVarResolver());
			bs.add(Config.class, getConfig());
			bs.add(ResponseProcessor[].class, getResponseProcessors());
			bs.addBean(CallLogger.class, getCallLogger());
			bs.add(HttpPartSerializer.class, getPartSerializer());
			bs.add(HttpPartParser.class, getPartParser());
			bs.add(JsonSchemaGenerator.class, getJsonSchemaGenerator());
			var sf = getStaticFiles();
			bs.addBean(StaticFiles.class, sf);
			bs.addBean(FileFinder.class, sf);
			bs.add(HeaderList.class, getDefaultRequestHeaders(), PROP_defaultRequestHeaders);
			bs.add(HeaderList.class, getDefaultResponseHeaders(), PROP_defaultResponseHeaders);
			bs.add(NamedAttributeMap.class, getDefaultRequestAttributes(), PROP_defaultRequestAttributes);
			bs.addBean(DebugEnablement.class, getDebugEnablement());
			// Force-initialize restOperations and restChildren now so that any construction failures
			// (e.g. bad @RestOp method or invalid child class) surface here inside the try-catch and
			// propagate as a proper init exception rather than lazily on first use.
			getRestOperations();
			getRestChildren();
			bs.addBean(SwaggerProvider.class, getSwaggerProvider());

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
	 * {@link DefaultConfig} is always synthesized as the top-most (parent-most) entry when not already present in the
	 * class hierarchy. This makes {@code DefaultConfig.@Rest(...)} the framework's single source of truth for default
	 * lists (response processors, REST op args, encoders, parsers, serializers, ...), while still letting bare
	 * {@code @Rest}-annotated resources inherit the framework defaults without explicitly implementing
	 * {@link BasicUniversalConfig} or any other {@code DefaultConfig} descendant. Resources that <i>do</i> implement
	 * {@code DefaultConfig} (transitively or directly) won't get a duplicate entry — the list is dedup'd by the
	 * {@link Class} the annotation was declared on.
	 */
	private final Memoizer<List<AnnotationInfo<Rest>>> restAnnotations = memoizer(() -> {
		var raw = getAnnotationProvider().find(Rest.class, ClassInfo.of(getResourceClass()));
		var hasDefaultConfig = raw.stream().anyMatch(ai -> ai.getAnnotatable() instanceof ClassInfo ci && DefaultConfig.class.equals(ci.inner()));
		if (hasDefaultConfig)
			return raw;
		var defaultConfigAnnotations = getAnnotationProvider().find(Rest.class, ClassInfo.of(DefaultConfig.class));
		if (defaultConfigAnnotations.isEmpty())
			return raw;
		var combined = new ArrayList<>(raw);
		combined.addAll(defaultConfigAnnotations);
		return Collections.unmodifiableList(combined);
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
			return Optional.empty();
		try {
			return Optional.of(Enum.valueOf(enumClass, t));
		} catch (IllegalArgumentException ignored) {
			return Optional.empty();
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

	/** Header names that may be passed via URL query parameter; resolved from {@code @Rest(allowedHeaderParams)}, default {@code "Accept,Content-Type"}. */
	private final Memoizer<Set<String>> allowedHeaderParams      = memoizer(() -> Collections.unmodifiableSet(newCaseInsensitiveSet(mergeReplacedStringAttribute(PROPERTY_allowedHeaderParams, env("RestContext.allowedHeaderParams", "Accept,Content-Type")))));
	/** HTTP method names that may be specified via a request header; resolved from {@code @Rest(allowedMethodHeaders)}, default empty. */
	private final Memoizer<Set<String>> allowedMethodHeaders     = memoizer(() -> Collections.unmodifiableSet(newCaseInsensitiveSet(mergeReplacedStringAttribute(PROPERTY_allowedMethodHeaders, env("RestContext.allowedMethodHeaders").orElse("")))));
	/** HTTP method names that may be specified via URL query parameter; resolved from {@code @Rest(allowedMethodParams)}, default {@code "HEAD,OPTIONS"}. */
	private final Memoizer<Set<String>> allowedMethodParams      = memoizer(() -> Collections.unmodifiableSet(newCaseInsensitiveSet(mergeReplacedStringAttribute(PROPERTY_allowedMethodParams, env("RestContext.allowedMethodParams", "HEAD,OPTIONS")))));
	/** Whether a {@code &content=} URL parameter may override the request body; inverse of {@code @Rest(disableContentParam)}. */
	private final Memoizer<Boolean>     allowContentParam        = memoizer(() -> !mergeReplacedBooleanAttribute(PROPERTY_disableContentParam, env("RestContext.disableContentParam", false)));
	/** Whether exception stack traces are rendered in error responses; resolved from {@code @Rest(renderResponseStackTraces)}. */
	private final Memoizer<Boolean>     renderResponseStackTraces = memoizer(() -> mergeReplacedBooleanAttribute(PROPERTY_renderResponseStackTraces, env("RestContext.renderResponseStackTraces", false)));
	/** The request header used for client-version matching; resolved from {@code @Rest(clientVersionHeader)}, default {@code "Client-Version"}. */
	private final Memoizer<String>      clientVersionHeader      = memoizer(() -> mergeReplacedStringAttribute(PROPERTY_clientVersionHeader, env("RestContext.clientVersionHeader", "Client-Version")));

	/**
	 * The {@link UriRelativity} strategy for URI resolution in this resource.
	 *
	 * <p>
	 * Resolved from {@code @Rest(uriRelativity)} annotations (most-derived non-blank wins), with env-var
	 * fallback. Defaults to {@link UriRelativity#RESOURCE}.
	 */
	private final Memoizer<UriRelativity> uriRelativity = memoizer(() -> {
		var v = new AtomicReference<>(
			parseEnumConstant(UriRelativity.class, resolve(emptyIfNull(env("RestContext.uriRelativity").get())))
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
		String local = mergeReplacedStringAttribute(PROPERTY_uriAuthority, env("RestContext.uriAuthority").orElse(null));
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
		String local = mergeReplacedStringAttribute(PROPERTY_uriContext, env("RestContext.uriContext").orElse(null));
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
			parseEnumConstant(UriResolution.class, resolve(emptyIfNull(env("RestContext.uriResolution").get())))
				.orElse(UriResolution.ROOT_RELATIVE)
		);
		restAnnotationsForPropertySortedByRank(PROPERTY_uriResolution).forEach(ai -> ai.getString(PROPERTY_uriResolution).filter(StringUtils::isNotBlank).ifPresent(s ->
			parseEnumConstant(UriResolution.class, resolve(s)).ifPresent(v::set)
		));
		return v.get();
	});

	/**
	 * Returns the {@link Rest} annotations on the resource class hierarchy for the specified property,
	 * in <b>parent-to-child</b> order, with {@code noInherit} cutoff applied.
	 *
	 * <p>
	 * Used by both this class and {@link RestOpContext} to walk the class-level annotation chain when
	 * computing memoized op-level settings that inherit from class-level {@code @Rest(...)} attributes.
	 *
	 * @param name The annotation property name (e.g. {@code "converters"}, {@code "guards"}).
	 * @return A stream of {@link AnnotationInfo} entries in parent-to-child order, never {@code null}.
	 */
	Stream<AnnotationInfo<Rest>> getRestAnnotationsForProperty(String name) {
		var annotations = getRestAnnotations();
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
	 */
	public void destroy() {
		for (var x : destroyInvokerPair.get().invokers) {
			try {
				x.invoke(beanStore, getResource());
			} catch (Exception e) {
				getLogger().log(Level.WARNING, unwrap(e), () -> f("Error occurred invoking servlet-destroy method ''{0}''.", x.getFullName()));
			}
		}

		getRestChildren().destroy();
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
	public AnnotationWorkList getAnnotations() { return builder.getApplied(); }

	/**
	 * Returns the bean context associated with this context.
	 *
	 * @return The bean store associated with this context.
	 */
	public BeanContext getBeanContext() { return beanContext.get(); }

	BeanContext.Builder         getBeanContextBuilder()          { return builder.beanContext(); }
	EncoderSet.Builder          getEncodersBuilder()             { return encodersBuilder.get(); }
	JsonSchemaGenerator.Builder getJsonSchemaGeneratorBuilder()  { return builder.jsonSchemaGenerator(); }
	ParserSet.Builder           getParsersBuilder()              { return parsersBuilder.get(); }
	HttpPartParser.Creator      getPartParserCreator()           { return builder.partParser(); }
	HttpPartSerializer.Creator  getPartSerializerCreator()       { return builder.partSerializer(); }
	SerializerSet.Builder       getSerializersBuilder()          { return serializersBuilder.get(); }

	/**
	 * Returns the bean store associated with this context.
	 *
	 * <p>
	 * The bean store is used for instantiating child resource classes.
	 *
	 * @return The resource resolver associated with this context.
	 */
	public BasicBeanStore getBeanStore() { return beanStore; }

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
	 * {@link RestInject @RestInject}-annotated static method on the resource class:
	 * <p class='bjava'>
	 * 	<ja>@RestInject</ja> <jk>public static</jk> CallLogger myCallLogger(<i>&lt;args&gt;</i>) {...}
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
	public CallLogger getCallLogger() { return callLogger.get(); }

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
	public Config getConfig() { return config.get(); }

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
	public DebugEnablement getDebugEnablement() { return debugEnablement.get(); }

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
	public NamedAttributeMap getDefaultRequestAttributes() { return defaultRequestAttributes.get(); }

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
	public HeaderList getDefaultRequestHeaders() { return defaultRequestHeaders.get(); }

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
	public HeaderList getDefaultResponseHeaders() { return defaultResponseHeaders.get(); }

	/**
	 * Returns the encoders associated with this context.
	 *
	 * @return The encoders associated with this context.
	 */
	public EncoderSet getEncoders() { return encoders.get(); }

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
	public JsonSchemaGenerator getJsonSchemaGenerator() { return jsonSchemaGenerator.get(); }

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
	public Logger getLogger() { return logger.get(); }

	/**
	 * Returns the resource bundle used by this resource.
	 *
	 * @return
	 * 	The resource bundle for this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public Messages getMessages() { return messages.get(); }

	/**
	 * Returns the timing statistics on all method executions on this class.
	 *
	 * @return The timing statistics on all method executions on this class.
	 */
	public MethodExecStore getMethodExecStore() { return methodExecStore.get(); }

	/**
	 * Returns the parsers associated with this context.
	 *
	 * @return The parsers associated with this context.
	 */
	public ParserSet getParsers() { return parsers.get(); }

	/**
	 * Returns the HTTP-part parser associated with this resource.
	 *
	 * @return
	 * 	The HTTP-part parser associated with this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public HttpPartParser getPartParser() { return partParser.get(); }

	/**
	 * Returns the HTTP-part serializer associated with this resource.
	 *
	 * @return
	 * 	The HTTP-part serializer associated with this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public HttpPartSerializer getPartSerializer() { return partSerializer.get(); }

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
	public ResponseProcessor[] getResponseProcessors() { return responseProcessors.get(); }

	/**
	 * Returns the {@link RestOpArg} classes registered on this resource.
	 *
	 * <p>
	 * Per-op {@link RestOpArg} instances are resolved separately at per-op setup via the bean store
	 * (see {@link #findRestOperationArgs(Method, BasicBeanStore)}); this getter returns the class list
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
	public RestChildren getRestChildren() { return restChildren.get(); }

	/**
	 * Returns the REST Java methods defined in this resource.
	 *
	 * <p>
	 * These are the methods annotated with the {@link RestOp @RestOp} annotation.
	 *
	 * @return
	 * 	An unmodifiable map of Java method names to call method objects.
	 */
	public RestOperations getRestOperations() { return restOperations.get(); }

	/**
	 * Returns the bootstrap bean store for this context.
	 *
	 * <p>
	 * This is the bean store inherited from the parent resource and does not include
	 * any beans added by this class.
	 *
	 * @return The bootstrap bean store for this context.
	 */
	public BasicBeanStore getBootstrapBeanStore() { return bootstrapBeanStore; }

	/**
	 * Returns the serializers associated with this context.
	 *
	 * @return The serializers associated with this context.
	 */
	public SerializerSet getSerializers() { return serializers.get(); }

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
	public StaticFiles getStaticFiles() { return staticFiles.get(); }

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
				s = getSwaggerProvider().getSwagger(this, locale);
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
	public SwaggerProvider getSwaggerProvider() { return swaggerProvider.get(); }

	/**
	 * Returns the stack trace database associated with this context.
	 *
	 * @return
	 * 	The stack trace database for this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public ThrownStore getThrownStore() { return thrownStore.get(); }

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
	public VarResolver getVarResolver() { return varResolver.get(); }

	/**
	 * Returns the bootstrap (pre-runtime) variable resolver used during context construction.
	 *
	 * <p>
	 * The bootstrap resolver has the same {@link Var} catalog as {@link #getVarResolver()} but does not have
	 * {@link Messages} or {@link Config} beans wired in — it is used to resolve annotation attribute values
	 * (e.g. <c>@Rest(messages=...)</c>) before those beans are built. Override via
	 * {@link RestInject @RestInject(name="bootstrapVarResolver")} on a static method of the resource class.
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
	 * Called during servlet initialization to invoke all {@link RestPostInit} child-last methods.
	 *
	 * @return This object.
	 * @throws ServletException Error occurred.
	 */
	public synchronized RestContext postInit() throws ServletException {
		if (initialized.get())
			return this;
		var resource2 = getResource();
		var mi = ClassInfo.of(getResource()).getPublicMethod(x -> x.hasName("setContext") && x.hasParameterTypes(RestContext.class)).orElse(null);
		if (nn(mi)) {
			try {
				mi.accessible().invoke(resource2, this);
			} catch (ExecutableException e) {
				throw new ServletException(e.unwrap());
			}
		}
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
		return getDebugEnablement().isDebug(this, call.getRequest());
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
	protected Throwable convertThrowable(Throwable t) {

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
	 * Called at the end of a request to invoke all {@link RestEndCall} methods.
	 *
	 * <p>
	 * This is the very last method called in {@link #execute(Object, HttpServletRequest, HttpServletResponse)}.
	 *
	 * @param session The current request.
	 */
	protected void endCall(RestSession session) {
		for (var x : endCallInvokerPair.get().invokers) {
			try {
				x.invoke(session.getBeanStore(), session.getResource());
			} catch (Exception e) {
				getLogger().log(Level.WARNING, unwrap(e), () -> f("Error occurred invoking finish-call method ''{0}''.", x.getFullName()));
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
	protected RestOpArg[] findRestOperationArgs(Method m, BasicBeanStore beanStore) {

		var mi = MethodInfo.of(m);
		var params = mi.getParameters();
		var ra = new RestOpArg[params.size()];

		beanStore = BasicBeanStore.of(beanStore);
		var roa = getRestOpArgs();

		for (var i = 0; i < params.size(); i++) {
			var pi = params.get(i);
			beanStore.addBean(ParameterInfo.class, pi);
			for (var c : roa) {
				try {
					ra[i] = BeanCreator.of(RestOpArg.class, beanStore).type(c).run();
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
			e.printStackTrace();

		int code = 500;

		var ci = ClassInfo.of(e);
		var r = ci.getAnnotations(StatusCode.class).findFirst().map(AnnotationInfo::inner).orElse(null);
		if (nn(r) && r.value().length > 0)
			code = r.value()[0];

		var e2 = (e instanceof BasicHttpException e22 ? e22 : new BasicHttpException(code, e));

		var req = session.getRequest();
		var res = session.getResponse();

		Throwable t = e2.getRootCause();
		if (nn(t)) {
			Thrown t2 = thrown(t);
			res.setHeader(t2.getName(), t2.getValue());
		}

		try {
			res.setContentType("text/plain");
			res.setHeader("Content-Encoding", "identity");
			var statusCode = e2.getStatusLine().getStatusCode();
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
	 * Handle the case where a matching method was not found.
	 *
	 * <p>
	 * Subclasses can override this method to provide a 2nd-chance for specifying a response.
	 * The default implementation will simply throw an exception with an appropriate message.
	 *
	 * @param session The HTTP call.
	 * @throws Exception Any exception can be thrown.
	 */
	void invokeRestInitMethod(MethodInfo m, Supplier<?> resource, BasicBeanStore beanStore) throws ServletException {
		try {
			m.invoke(resource.get(), beanStore.getParams(m, resource.get()));
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
		var onPath = pathInfo == null ? " on no pathInfo" : String.format(" on path '%s'", pathInfo);
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
		 * @param session The current request.
	 * @throws Exception If thrown from call methods.
	 */
	protected void postCall(RestOpSession session) throws Exception {
		for (var m : session.getContext().getPostCallMethods())
			m.invoke(session);
	}

	/**
	 * Called during a request to invoke all {@link RestPreCall} methods.
	 *
	 * @param session The current request.
	 * @throws Exception If thrown from call methods.
	 */
	protected void preCall(RestOpSession session) throws Exception {
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
	protected void processResponse(RestOpSession opSession) throws IOException, BasicHttpException, NotImplemented {

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
			.a(PROPERTY_uriAuthority, getUriAuthority())
			.a(PROPERTY_uriContext, getUriContext())
			.a(PROPERTY_uriRelativity, getUriRelativity())
			.a(PROPERTY_uriResolution, getUriResolution());
	}

	/**
	 * Called at the start of a request to invoke all {@link RestStartCall} methods.
	 *
	 * @param session The current request.
	 * @throws BasicHttpException If thrown from call methods.
	 */
	protected void startCall(RestSession session) throws BasicHttpException {
		for (var x : startCallInvokerPair.get().invokers) {
			try {
				x.invoke(session.getBeanStore(), session.getContext().getResource());
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