// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.rest;

import static javax.servlet.http.HttpServletResponse.*;
import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.collections.OMap.*;
import static org.apache.juneau.http.HttpHeaders.*;
import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.ThrowableUtils.*;
import static org.apache.juneau.internal.IOUtils.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.internal.SystemEnv.*;
import static org.apache.juneau.rest.HttpRuntimeException.*;
import static org.apache.juneau.rest.logging.RestLoggingDetail.*;
import static org.apache.juneau.rest.processor.ResponseProcessor.*;
import static java.util.Arrays.*;
import static java.util.Collections.*;
import static java.util.Optional.*;
import static java.util.logging.Level.*;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.lang.reflect.Method;
import java.nio.charset.*;
import java.time.*;
import java.util.*;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.logging.*;
import java.util.stream.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.http.Header;
import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.config.*;
import org.apache.juneau.config.vars.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.dto.swagger.Swagger;
import org.apache.juneau.encoders.*;
import org.apache.juneau.html.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.httppart.bean.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.parser.ParseException;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.arg.*;
import org.apache.juneau.rest.debug.*;
import org.apache.juneau.rest.httppart.*;
import org.apache.juneau.rest.logging.*;
import org.apache.juneau.rest.processor.*;
import org.apache.juneau.rest.rrpc.*;
import org.apache.juneau.rest.staticfile.*;
import org.apache.juneau.rest.stats.*;
import org.apache.juneau.rest.swagger.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.util.*;
import org.apache.juneau.rest.vars.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.svl.vars.*;

/**
 * Defines the initial configuration of a <c>RestServlet</c> or <c>@Rest</c> annotated object.
 *
 * <p>
 * An extension of the {@link ServletConfig} object used during servlet initialization.
 *
 * <p>
 * Methods are provided for overriding or augmenting the information provided by the <ja>@Rest</ja> annotation.
 * In general, most information provided in the <ja>@Rest</ja> annotation can be specified programmatically
 * through calls on this object.
 *
 * <p>
 * To interact with this object, simply pass it in as a constructor argument or in an INIT hook.
 * <p class='bcode w800'>
 * 	<jc>// Option #1 - Pass in through constructor.</jc>
 * 	<jk>public</jk> MyResource(Builder builder) {
 * 			builder
 * 				.swaps(TemporalCalendarSwap.IsoLocalDateTime.<jk>class</jk>)
 * 				.set(<jsf>PARSER_debug</jsf>);
 * 	}
 *
 * 	<jc>// Option #2 - Use an INIT hook.</jc>
 * 	<ja>@RestHook</ja>(<jsf>INIT</jsf>)
 * 	<jk>public void</jk> init(Builder builder) <jk>throws</jk> Exception {
 * 			builder
 * 				.swaps(TemporalCalendarSwap.IsoLocalDateTime.<jk>class</jk>)
 * 				.set(<jsf>PARSER_debug</jsf>);
 * 	}
 * </p>
 *
 * <ul class='spaced-list'>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc jrs.RestContext}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public class RestContext extends Context {

	//-------------------------------------------------------------------------------------------------------------------
	// Static
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Represents a null value for the {@link Rest#contextClass()} annotation.
	 */
	public static final class Null extends RestContext {
		private Null(Builder builder) throws Exception {
			super(builder);
		}
	}

	private static final Map<Class<?>, RestContext> REGISTRY = new ConcurrentHashMap<>();

	/**
	 * Returns a registry of all created {@link RestContext} objects.
	 *
	 * @return An unmodifiable map of resource classes to {@link RestContext} objects.
	 */
	public static final Map<Class<?>, RestContext> getGlobalRegistry() {
		return Collections.unmodifiableMap(REGISTRY);
	}

	/**
	 * Creates a new builder for this object.
	 *
	 * <p>
	 * The builder class can be subclassed by using the {@link Rest#builder()} annotation.
	 * This can be useful when you want to perform any customizations on the builder class, typically by overriding protected methods that create
	 * 	the various builders used in the created {@link RestContext} object (which itself can be overridden via {@link RestContext.Builder#type(Class)}).
	 * The subclass must contain a public constructor that takes in the same arguments passed in to this method.
	 *
	 * @param resourceClass
	 * 	The class annotated with <ja>@Rest</ja>.
	 * 	<br>Must not be <jk>null</jk>.
	 * @param parentContext
	 * 	The parent context if the REST bean was registered via {@link Rest#children()}.
	 * 	<br>Can be <jk>null</jk> if the bean is a top-level resource.
	 * @param servletConfig
	 * 	The servlet config passed into the servlet by the servlet container.
	 * 	<br>Can be <jk>null</jk> if not available.
	 * 	<br>If <jk>null</jk>, then some features (such as access to servlet init params) will not be available.
	 *
	 * @return A new builder object.
	 * @throws ServletException Something bad happened.
	 */
	public static Builder create(Class<?> resourceClass, RestContext parentContext, ServletConfig servletConfig) throws ServletException {

		Value<Class<? extends Builder>> v = Value.of(Builder.class);
		ClassInfo.of(resourceClass)
			.getAnnotations(Rest.class)
			.stream()
			.filter(x -> x.builder() != Builder.Null.class)
			.forEach(x -> v.set(x.builder()));

		if (v.get() == Builder.class)
			return new Builder(resourceClass, parentContext, servletConfig);

		return BeanStore
			.of(parentContext == null ? null : parentContext.getRootBeanStore())
			.addBean(Class.class, resourceClass)
			.addBean(RestContext.class, parentContext)
			.addBean(ServletConfig.class, servletConfig)
			.creator(Builder.class)
			.type(v.get())
			.run();
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Builder
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters(ignore={"set"})
	public static class Builder extends Context.Builder implements ServletConfig {

		/**
		 * Represents a <jk>null</jk> value for the {@link Rest#builder()} annotation.
		 */
		public static final class Null extends Builder {
			private Null(Class<?> resourceClass, RestContext parentContext, ServletConfig servletConfig) {
				super(resourceClass, parentContext, servletConfig);
			}
		}

		//-----------------------------------------------------------------------------------------------------------------
		// The following fields are meant to be modifiable.
		// They should not be declared final.
		// Read-only snapshots of these will be made in RestServletContext.
		//-----------------------------------------------------------------------------------------------------------------

		private boolean initialized;

		Supplier<?> resource;
		ServletContext servletContext;

		final ServletConfig inner;
		final Class<?> resourceClass;
		final RestContext parentContext;

		private DefaultClassList defaultClasses;
		private DefaultSettingsMap defaultSettings;

		private BeanStore rootBeanStore, beanStore;
		private Config config;
		private VarResolver.Builder varResolver;
		private Logger logger;
		private ThrownStore.Builder thrownStore;
		private MethodExecStore.Builder methodExecStore;
		private Messages.Builder messages;
		private ResponseProcessorList.Builder responseProcessors;
		private RestLogger.Builder callLogger;
		private HttpPartSerializer.Creator partSerializer;
		private HttpPartParser.Creator partParser;
		private JsonSchemaGenerator.Builder jsonSchemaGenerator;
		private FileFinder.Builder fileFinder;
		private StaticFiles.Builder staticFiles;
		private HeaderList.Builder defaultRequestHeaders, defaultResponseHeaders;
		private NamedAttributeList.Builder defaultRequestAttributes;
		private RestOpArgList.Builder restOpArgs;
		private DebugEnablement.Builder debugEnablement;
		private MethodList startCallMethods, endCallMethods, postInitMethods, postInitChildFirstMethods, destroyMethods, preCallMethods, postCallMethods;
		private RestOperations.Builder restOperations;
		private RestChildren.Builder restChildren;
		private SwaggerProvider.Builder swaggerProvider;
		private BeanContext.Builder beanContext;
		private EncoderSet.Builder encoders;
		private SerializerSet.Builder serializers;
		private ParserSet.Builder parsers;

		String
			allowedHeaderParams = env("RestContext.allowedHeaderParams", "Accept,Content-Type"),
			allowedMethodHeaders = env("RestContext.allowedMethodHeaders", ""),
			allowedMethodParams = env("RestContext.allowedMethodParams", "HEAD,OPTIONS"),
			clientVersionHeader = env("RestContext.clientVersionHeader", "Client-Version"),
			debugOn = env("RestContext.debugOn", null),
			path = null,
			uriAuthority = env("RestContext.uriAuthority", (String)null),
			uriContext = env("RestContext.uriContext", (String)null);
		UriRelativity uriRelativity = env("RestContext.uriRelativity", UriRelativity.RESOURCE);
		UriResolution uriResolution = env("RestContext.uriResolution", UriResolution.ROOT_RELATIVE);
		Charset defaultCharset = env("RestContext.defaultCharset", IOUtils.UTF8);
		long maxInput = parseLongWithSuffix(env("RestContext.maxInput", "100M"));
		List<MediaType> consumes, produces;
		boolean disableBodyParam = env("RestContext.disableBodyParam", false);
		boolean renderResponseStackTraces = env("RestContext.renderResponseStackTraces", false);

		Class<? extends RestChildren> childrenClass = RestChildren.class;
		Class<? extends RestOpContext> opContextClass = RestOpContext.class;
		Class<? extends RestOperations> operationsClass = RestOperations.class;

		List<Object> children = new ArrayList<>();

		/**
		 * Constructor.
		 *
		 * @param resourceClass
		 * 	The REST servlet/bean type that this context is defined against.
		 * @param parentContext The parent context if this is a child of another resource.
		 * @param servletConfig The servlet config if available.
		 */
		protected Builder(Class<?> resourceClass, RestContext parentContext, ServletConfig servletConfig) {
			type(RestContext.class);

			this.resourceClass = resourceClass;
			this.inner = servletConfig;
			this.parentContext = parentContext;

			// Pass-through default values.
			if (parentContext != null) {
				defaultClasses = parentContext.defaultClasses.copy();
				defaultSettings = parentContext.defaultSettings.copy();
				rootBeanStore = parentContext.rootBeanStore;
			} else {
				defaultClasses = DefaultClassList.create();
				defaultSettings = DefaultSettingsMap.create();
			}
		}

		@Override /* Context.Builder */
		public Builder copy() {
			throw new NoSuchMethodError("Not implemented.");
		}

		@Override /* BeanContext.Builder */
		public RestContext build() {
			try {
				return BeanCreator.of(RestContext.class).outer(resource.get()).store(beanStore()).builder(this).type(getType().orElse(RestContext.class)).run();
			} catch (Exception e) {
				throw new InternalServerError(e, "Could not instantiate RestContext.");
			}
		}

		/**
		 * Performs initialization on this builder against the specified REST servlet/bean instance.
		 *
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @return This object.
		 * @throws ServletException If hook method calls failed.
		 */
		public Builder init(Supplier<?> resource) throws ServletException {

			if (initialized)
				return this;
			initialized = true;

			this.resource = resource;
			Supplier<?> r = this.resource;
			Class<?> rc = resourceClass;

			beanStore = createBeanStore(rc, r)
				.build()
				.addBean(Builder.class, this)
				.addBean(ServletConfig.class, ofNullable(inner).orElse(this))
				.addBean(ServletContext.class, ofNullable(inner).orElse(this).getServletContext());
			BeanStore bs = beanStore;

			if (rootBeanStore == null)
				rootBeanStore = beanStore.copy().build();

			varResolver = createVarResolver(bs, rc);
			beanStore.add(VarResolver.class, varResolver.build());
			config = beanStore.add(Config.class, createConfig(bs, rc));
			beanStore.add(VarResolver.class, varResolver.bean(Config.class, config).build());

			ClassInfo rci = ClassInfo.of(resourceClass);

			VarResolverSession vrs = varResolver().build().createSession();
			AnnotationWorkList al = rci.getAnnotationList(ContextApplyFilter.INSTANCE).getWork(vrs);
			apply(al);
			beanContext().apply(al);
			partSerializer().apply(al);
			partParser().apply(al);
			jsonSchemaGenerator().apply(al);

			runInitHooks(bs, resource());

			return this;
		}

		private void runInitHooks(BeanStore beanStore, Supplier<?> resource) throws ServletException {

			Object r = resource.get();
			ClassInfo rci = ClassInfo.ofProxy(r);

			Map<String,MethodInfo> map = new LinkedHashMap<>();
			for (MethodInfo m : rci.getAllMethodsParentFirst()) {
				if (m.hasAnnotation(RestHook.class) && m.getLastAnnotation(RestHook.class).value() == HookEvent.INIT) {
					m.setAccessible();
					String sig = m.getSignature();
					if (! map.containsKey(sig))
						map.put(sig, m);
				}
			}

			for (MethodInfo m : map.values()) {
				List<ParamInfo> params = m.getParams();

				List<ClassInfo> missing = beanStore.getMissingParamTypes(params);
				if (! missing.isEmpty())
					throw new RestServletException("Could not call @RestHook(INIT) method {0}.{1}.  Could not find prerequisites: {2}.", m.getDeclaringClass().getSimpleName(), m.getSignature(), missing.stream().map(x->x.getSimpleName()).collect(Collectors.joining(",")));

				try {
					m.invoke(r, beanStore.getParams(params));
				} catch (Exception e) {
					throw new RestServletException(e, "Exception thrown from @RestHook(INIT) method {0}.{1}.", m.getDeclaringClass().getSimpleName(), m.getSignature());
				}
			}
		}

		/**
		 * Returns the REST servlet/bean instance that this context is defined against.
		 *
		 * @return The REST servlet/bean instance that this context is defined against.
		 */
		public final Supplier<?> resource() {
			if (resource == null)
				throw runtimeException("Resource not available.  init(Object) has not been called.");
			return resource;
		}

		/**
		 * Returns the REST servlet/bean instance that this context is defined against if it's the specified type.
		 *
		 * @param type The expected type of the resource bean.
		 * @return The bean cast to that instance, or {@link Optional#empty()} if it's not the specified type.
		 */
		@SuppressWarnings("unchecked")
		public final <T> Optional<T> resourceAs(Class<T> type) {
			Object r = resource().get();
			return Optional.ofNullable(type.isInstance(r) ? (T)r : null);
		}

		//-----------------------------------------------------------------------------------------------------------------
		// defaultClasses
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * Returns the default implementation class list.
		 *
		 * <p>
		 * This defines the implementation classes for a variety of bean types.
		 *
		 * <p>
		 * Default classes are inherited from the parent REST object.
		 * Typically used on the top-level {@link RestContext.Builder} to affect class types for that REST object and all children.
		 *
		 * <p>
		 * Modifying the default class list on this builder does not affect the default class list on the parent builder, but changes made
		 * here are inherited by child builders.
		 *
		 * @return The default implementation class list.
		 */
		public final DefaultClassList defaultClasses() {
			return defaultClasses;
		}

		/**
		 * Adds to the default implementation class list.
		 *
		 * <p>
		 * A shortcut for the following code:
		 *
		 * <p class='bcode w800'>
		 * 	<jv>builder</jv>.defaultClasses().add(<jv>values</jv>);
		 * </p>
		 *
		 * @param values The values to add to the list of default classes.
		 * @return This object.
		 * @see #defaultClasses()
		 */
		public final Builder defaultClasses(Class<?>...values) {
			defaultClasses().add(values);
			return this;
		}

		//-----------------------------------------------------------------------------------------------------------------
		// defaultSettings
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * Returns the default settings map.
		 *
		 * <p>
		 * Default settings are inherited from the parent REST object.
		 * Typically used on the top-level {@link RestContext.Builder} to affect settings for that REST object and all children.
		 *
		 * <p>
		 * Modifying the default settings map on this builder does not affect the default settings on the parent builder, but changes made
		 * here are inherited by child builders.
		 *
		 * @return The default settings map.
		 */
		public final DefaultSettingsMap defaultSettings() {
			return defaultSettings;
		}

		/**
		 * Sets a value in the default settings map.
		 *
		 * <p>
		 * A shortcut for the following code:
		 *
		 * <p class='bcode w800'>
		 * 	<jv>builder</jv>.defaultSettings().add(<jv>key</jv>, <jv>value</jv>);
		 *
		 * </p>
		 * @param key The setting key.
		 * @param value The setting value.
		 * @return This object.
		 * @see #defaultSettings()
		 */
		public final Builder defaultSetting(String key, Object value) {
			defaultSettings().set(key, value);
			return this;
		}

		//-----------------------------------------------------------------------------------------------------------------
		// beanStore
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * Returns the bean store in this builder.
		 *
		 * <p>
		 * Can be used to add more beans to the bean store.
		 *
		 * <p>
		 * The bean store is created by the constructor using the {@link #createBeanStore(Class,Supplier)} method and is initialized with the following beans:
		 * <ul>
		 * 	<li>{@link RestContext.Builder}
		 * 	<li>{@link ServletConfig}
		 * 	<li>{@link ServletContext}
		 * 	<li>{@link VarResolver}
		 * 	<li>{@link Config}
		 * </ul>
		 *
		 * @return The bean store in this builder.
		 */
		public final BeanStore beanStore() {
			return beanStore;
		}

		/**
		 * Applies an operation to the bean store in this builder.
		 *
		 * <p>
		 * Typically used to allow you to execute operations without breaking the fluent flow of the context builder.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	RestContext <jv>context</jv> = RestContext
		 * 		.<jsm>create</jsm>(<jv>resourceClass</jv>, <jv>parentContext</jv>, <jv>servletConfig</jv>)
		 * 		.beanStore(<jv>x</jv> -&gt; <jv>x</jv>.addSupplier(MyBean.<jk>class</jk>, ()-&gt;<jsm>getMyBean</jsm>()))
		 * 		.build();
		 * </p>
		 *
		 * @param operation The operation to apply.
		 * @return This object.
		 */
		public final Builder beanStore(Consumer<BeanStore> operation) {
			operation.accept(beanStore());
			return this;
		}

		/**
		 * Returns the root bean store.
		 *
		 * @return The root bean store.
		 */
		public final BeanStore rootBeanStore() {
			return rootBeanStore;
		}

		/**
		 * Creates the bean store in this builder.
		 *
		 * <p>
		 * Gets called in the constructor to create the bean store used for finding other beans.
		 *
		 * <p>
		 * The bean store is created with the parent root bean store as the parent, allowing any beans in the root bean store to be available
		 * in this builder.  The root bean store typically pulls from an injection framework such as Spring to allow injected beans to be used.
		 *
		 * The resource class can optionally define a <c><jk>public static</jk> BeanStore <jsm>createBeanStore</jk>(...);</c> method to override
		 * the default bean store created by this method.  The parameters can be any beans available in the root bean store (such as any available
		 * Spring beans if the top level resource is an instance of SpringRestServlet).
		 *
		 * @param resourceClass
		 * 	The REST servlet/bean type that this context is defined against.
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @return A new bean store.
		 */
		protected BeanStore.Builder createBeanStore(Class<?> resourceClass, Supplier<?> resource) {

			// Default value.
			Value<BeanStore.Builder> v = Value.of(
				BeanStore
					.create()
					.parent(rootBeanStore())
					.outer(resource.get())
			);

			// Apply @Rest(beanStore).
			ClassInfo.of(resourceClass)
				.getAnnotations(Rest.class)
				.stream()
				.map(x -> x.beanStore())
				.filter(x -> x != BeanStore.Null.class)
				.reduce((x1,x2)->x2)
				.ifPresent(x -> v.get().type(x));

			// Replace with builder from:  public [static] BeanStore.Builder createBeanStore(<args>)
			v.get().build()
				.beanCreateMethodFinder(BeanStore.Builder.class)
				.find("createBeanStore")
				.run(x -> v.set(x));

			// Replace with bean from:  public [static] BeanStore createBeanStore(<args>)
			v.get().build()
				.beanCreateMethodFinder(BeanStore.class)
				.find("createBeanStore")
				.run(x -> v.get().impl(x));

			return v.get();
		}

		//-----------------------------------------------------------------------------------------------------------------
		// varResolver
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * Returns the variable resolver sub-builder.
		 *
		 * <p>
		 * Can be used to add more variables or context objects to the variable resolver.
		 * These variables affect the variable resolver returned by {@link RestRequest#getVarResolverSession()} which is
		 * used to resolve string variables of the form <js>"$X{...}"</js> in various places such as annotations on the REST class and methods.
		 *
		 * <p>
		 * The var resolver is created by the constructor using the {@link #createVarResolver(BeanStore,Class)} method and is initialized with the following beans:
		 * <ul>
		 * 	<li>{@link ConfigVar}
		 * 	<li>{@link FileVar}
		 * 	<li>{@link SystemPropertiesVar}
		 * 	<li>{@link EnvVariablesVar}
		 * 	<li>{@link ArgsVar}
		 * 	<li>{@link ManifestFileVar}
		 * 	<li>{@link SwitchVar}
		 * 	<li>{@link IfVar}
		 * 	<li>{@link CoalesceVar}
		 * 	<li>{@link PatternMatchVar}
		 * 	<li>{@link PatternReplaceVar}
		 * 	<li>{@link PatternExtractVar}
		 * 	<li>{@link UpperCaseVar}
		 * 	<li>{@link LowerCaseVar}
		 * 	<li>{@link NotEmptyVar}
		 * 	<li>{@link LenVar}
		 * 	<li>{@link SubstringVar}
		 * </ul>
		 *
		 * @return The variable resolver sub-builder.
		 */
		public final VarResolver.Builder varResolver() {
			return varResolver;
		}

		/**
		 * Applies an operation to the variable resolver sub-builder.
		 *
		 * <p>
		 * Typically used to allow you to execute operations without breaking the fluent flow of the context builder.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	RestContext <jv>context</jv> = RestContext
		 * 		.<jsm>create</jsm>(<jv>resourceClass</jv>, <jv>parentContext</jv>, <jv>servletConfig</jv>)
		 * 		.varResolver(<jv>x</jv> -&gt; <jv>x</jv>.vars(MyVar.<jk>class</jk>))
		 * 		.build();
		 * </p>
		 *
		 * @param operation The operation to apply.
		 * @return This object.
		 */
		public final Builder varResolver(Consumer<VarResolver.Builder> operation) {
			operation.accept(varResolver());
			return this;
		}

		/**
		 * Creates the variable resolver sub-builder.
		 *
		 * <p>
		 * Gets called in the constructor to create the var resolver for performing variable resolution in strings.
		 *
		 * <p>
		 * Instantiates based on the following logic:
		 * <ol>
		 * 	<li>
		 * 		Looks for the following method on the resource class and sets it as the implementation bean:
		 * 		<br><c><jk>public static</jk> VarResolver createVarResolver(<any-bean-types-in-bean-store>) {}</c>
		 * 	<li>
		 * 		Looks for bean of type {@link org.apache.juneau.svl.VarResolver} in bean store and sets it as the implementation bean.
		 * 	<li>
		 * 		Looks for the following method on the resource class:
		 * 		<br><c><jk>public static</jk> VarResolver.Builder createVarResolver(<any-bean-types-in-bean-store>) {}</c>
		 * 	<li>
		 * 		Looks for bean of type {@link org.apache.juneau.svl.VarResolver.Builder} in bean store and returns a copy of it.
		 * 	<li>
		 * 		Creates a default builder with default variables pulled from {@link #createVars(BeanStore,Class)}.
		 * </ol>
		 *
		 * @param beanStore The bean store containing injected beans.
		 * @param resourceClass
		 * 	The REST servlet/bean type that this context is defined against.
		 * @return A new variable resolver sub-builder.
		 */
		protected VarResolver.Builder createVarResolver(BeanStore beanStore, Class<?> resourceClass) {

			// Default value.
			Value<VarResolver.Builder> v = Value.of(
				VarResolver
					.create()
					.defaultVars()
					.vars(createVars(beanStore, resourceClass))
					.vars(FileVar.class)
					.bean(FileFinder.class, FileFinder.create().cp(resourceClass,null,true).build())
			);

			// Replace with builder from bean store.
			beanStore
				.getBean(VarResolver.Builder.class)
				.map(y -> y.copy())
				.ifPresent(x -> v.set(x));

			// Replace with bean from bean store.
			beanStore
				.getBean(VarResolver.class)
				.ifPresent(x -> v.get().impl(x));

			// Replace with builder from:  public [static] VarResolver.Builder createVarResolver(<args>)
			beanStore
				.beanCreateMethodFinder(VarResolver.Builder.class)
				.find("createVarResolver")
				.run(x -> v.set(x));

			// Replace with bean from:  public [static] VarResolver createVarResolver(<args>)
			beanStore
				.beanCreateMethodFinder(VarResolver.class)
				.addBean(VarResolver.Builder.class, v.get())
				.find("createVarResolver")
				.run(x -> v.get().impl(x));

			return v.get();
		}

		/**
		 * Instantiates the variable resolver variables for this REST resource.
		 *
		 * <p>
		 * Instantiates based on the following logic:
		 * <ol>
		 * 	<li>
		 * 		Looks for the following method on the resource class:
		 * 		<br><c><jk>public static</jk> VarList createVars(<any-bean-types-in-bean-store>) {}</c>
		 * 	<li>
		 * 		Looks for bean of type {@link org.apache.juneau.svl.VarList} in bean store and returns a copy of it.
		 * 	<li>
		 * 		Creates a default builder with default variables.
		 * </ol>
		 *
		 * @param beanStore The bean store containing injected beans.
		 * @param resourceClass
		 * 	The REST servlet/bean type that this context is defined against.
		 * @return A new var resolver variable list.
		 */
		protected VarList createVars(BeanStore beanStore, Class<?> resourceClass) {

			// Default value.
			Value<VarList> v = Value.of(
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
			);

			// Replace with bean from bean store.
			beanStore
				.getBean(VarList.class)
				.map(x -> x.copy())
				.ifPresent(x -> v.set(x));

			// Replace with bean from:  public [static] VarList createVars(<args>)
			beanStore
				.beanCreateMethodFinder(VarList.class)
				.addBean(VarList.class, v.get())
				.find("createVars")
				.run(x -> v.set(x));

			return v.get();
		}

		//-----------------------------------------------------------------------------------------------------------------
		// config
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * Returns the external configuration file for this resource.
		 *
		 * <p>
		 * The configuration file location is determined via the {@link Rest#config() @Rest(config)}
		 * annotation on the resource.
		 *
		 * <p>
		 * The config file can be programmatically overridden by adding the following method to your resource:
		 * <p class='bcode w800'>
		 * 	<jk>public</jk> Config createConfig(ServletConfig servletConfig) <jk>throws</jk> ServletException;
		 * </p>
		 *
		 * <p>
		 * If a config file is not set up, then an empty config file will be returned that is not backed by any file.
		 *
		 * @return The external configuration file for this resource.
		 */
		public final Config config() {
			return config;
		}

		/**
		 * Applies an operation to the external configuration file for this resource.
		 *
		 * <p>
		 * Typically used to allow you to execute operations without breaking the fluent flow of the context builder.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	RestContext <jv>context</jv> = RestContext
		 * 		.<jsm>create</jsm>(<jv>resourceClass</jv>, <jv>parentContext</jv>, <jv>servletConfig</jv>)
		 * 		.config(<jv>x</jv> -&gt; <jv>x</jv>.set(<js>"Foo/bar"</js>, <js>"baz"</js>).save())
		 * 		.build();
		 * </p>
		 *
		 * @param operation The operation to apply.
		 * @return This object.
		 */
		public final Builder config(Consumer<Config> operation) {
			operation.accept(config());
			return this;
		}

		/**
		 * Overwrites the default config file with a custom config file.
		 *
		 * <p>
		 * By default, the config file is determined using the {@link Rest#config() @Rest(config)}
		 * annotation.
		 * This method allows you to programmatically override it with your own custom config file.
		 *
		 * @param config The new config file.
		 * @return This object.
		 */
		@FluentSetter
		public final Builder config(Config config) {
			this.config = config;
			return this;
		}

		/**
		 * Creates the config for this builder.
		 *
		 * @param beanStore The bean store to use for creating the config.
		 * @param resourceClass
		 * 	The REST servlet/bean type that this context is defined against.
		 * @return A new config.
		 */
		protected Config createConfig(BeanStore beanStore, Class<?> resourceClass) {

			Value<Config> v = Value.empty();

			// Find our config file.  It's the last non-empty @RestResource(config).
			VarResolver vr = beanStore.getBean(VarResolver.class).orElseThrow(()->runtimeException("VarResolver not found."));
			String cf = ClassInfo
				.of(resourceClass)
				.getAnnotations(Rest.class)
				.stream()
				.map(x -> x.config())
				.filter(x -> ! x.isEmpty())
				.reduce((x1,x2)->x2)
				.map(x -> vr.resolve(x))
				.orElse("");

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
			beanStore
				.getBean(Config.class)
				.ifPresent(x -> v.set(x));

			// Replace with bean from:  public [static] Config createConfig(<args>)
			beanStore
				.beanCreateMethodFinder(Config.class)
				.addBean(Config.class, v.get())
				.find("createConfig")
				.run(x -> v.set(x));

			return v.get();
		}

		//-----------------------------------------------------------------------------------------------------------------
		// logger
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * Returns the logger for this resource.
		 *
		 * @return The logger for this resource.
		 * @throws RuntimeException If {@link #init(Supplier)} has not been called.
		 */
		public final Logger logger() {
			if (logger == null)
				logger = createLogger(beanStore(), resourceClass);
			return logger;
		}

		/**
		 * Applies an operation to the logger for this resource.
		 *
		 * <p>
		 * Typically used to allow you to execute operations without breaking the fluent flow of the context builder.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	RestContext <jv>context</jv> = RestContext
		 * 		.<jsm>create</jsm>(<jv>resourceClass</jv>, <jv>parentContext</jv>, <jv>servletConfig</jv>)
		 * 		.logger(<jv>x</jv> -&gt; <jv>x</jv>.setFilter(<jv>logFilter</jv>))
		 * 		.build();
		 * </p>
		 *
		 * @param operation The operation to apply.
		 * @return This object.
		 */
		public final Builder logger(Consumer<Logger> operation) {
			operation.accept(logger());
			return this;
		}

		/**
		 * Sets the logger for this resource.
		 *
		 * <p>
		 * If not specified, the logger used is created by {@link #createLogger(BeanStore, Class)}.
		 *
		 * @param value The logger to use for the REST resource.
		 * @return This object.
		 */
		public final Builder logger(Logger value) {
			logger = value;
			return this;
		}

		/**
		 * Instantiates the logger for this resource.
		 *
		 * <p>
		 * Instantiates based on the following logic:
		 * <ul>
		 * 	<li>Looks for a static or non-static <c>createLogger()</> method that returns <c>{@link Logger}</c> on the
		 * 		resource class with any of the following arguments:
		 * 		<ul>
		 * 			<li>{@link RestContext}
		 * 			<li>{@link BeanStore}
		 * 			<li>Any {@doc jrs.Injection injected beans}.
		 * 		</ul>
		 * 	<li>Resolves it via the bean store registered in this context.
		 * 	<li>Instantiates via <c>Logger.<jsm>getLogger</jsm>(<jv>resource</jv>.getClass().getName())</c>.
		 * </ul>
		 *
		 * @param resourceClass
		 * 	The REST servlet/bean class that this context is defined against.
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * 	<br>Created by {@link RestContext.Builder#beanStore()}.
		 * @return A new logger.
		 */
		protected Logger createLogger(BeanStore beanStore, Class<?> resourceClass) {

			// Default value.
			Value<Logger> v = Value.of(
				Logger.getLogger(resourceClass.getClass().getName())
			);

			// Replace with bean from bean store.
			beanStore
				.getBean(Logger.class)
				.ifPresent(x -> v.set(x));

			// Replace with bean from:  public [static] Logger createLogger(<args>)
			beanStore
				.beanCreateMethodFinder(Logger.class)
				.addBean(Logger.class, v.get())
				.find("createLogger")
				.run(x -> v.set(x));

			return v.get();
		}

		//-----------------------------------------------------------------------------------------------------------------
		// thrownStore
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * Returns the thrown-store sub-builder.
		 *
		 * @return The builder for the {@link ThrownStore} object in the REST context.
		 */
		public final ThrownStore.Builder thrownStore() {
			if (thrownStore == null)
				thrownStore = createThrownStore(beanStore(), resource(), parentContext);
			return thrownStore;
		}

		/**
		 * Applies an operation to the thrown-store sub-builder.
		 *
		 * <p>
		 * Typically used to allow you to execute operations without breaking the fluent flow of the context builder.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	RestContext <jv>context</jv> = RestContext
		 * 		.<jsm>create</jsm>(<jv>resourceClass</jv>, <jv>parentContext</jv>, <jv>servletConfig</jv>)
		 * 		.logger(<jv>x</jv> -&gt; <jv>x</jv>.statsImplClass(MyStatsImplClass.<jk>class</jk>))
		 * 		.build();
		 * </p>
		 *
		 * @param operation The operation to apply.
		 * @return This object.
		 */
		public final Builder thrownStore(Consumer<ThrownStore.Builder> operation) {
			operation.accept(thrownStore());
			return this;
		}

		/**
		 * Instantiates the thrown-store sub-builder.
		 *
		 * <p>
		 * Instantiates based on the following logic:
		 * <ul>
		 * 	<li>Looks for a static or non-static <c>createThrownStore()</> method that returns <c>{@link ThrownStore}</c> on the
		 * 		resource class with any of the following arguments:
		 * 		<ul>
		 * 			<li>{@link RestContext}
		 * 			<li>{@link BeanStore}
		 * 			<li>Any {@doc jrs.Injection injected beans}.
		 * 		</ul>
		 * 	<li>Resolves it via the bean store registered in this context.
		 * 	<li>Returns {@link ThrownStore#GLOBAL}.
		 * </ul>
		 *
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @param parent
		 * 	The parent context if the REST bean was registered via {@link Rest#children()}.
		 * 	<br>Will be <jk>null</jk> if the bean is a top-level resource.
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * 	<br>Created by {@link RestContext.Builder#beanStore()}.
		 * @return A new thrown-store sub-builder.
		 */
		protected ThrownStore.Builder createThrownStore(BeanStore beanStore, Supplier<?> resource, RestContext parent) {

			// Default value.
			Value<ThrownStore.Builder> v = Value.of(
				ThrownStore
					.create()
					.beanStore(beanStore)
					.impl(parent == null ? null : parent.getThrownStore())
			);

			// Specify the implementation class if its set as a default.
			defaultClasses()
				.get(ThrownStore.class)
				.ifPresent(x -> v.get().type(x));

			// Replace with builder from bean store.
			beanStore
				.getBean(ThrownStore.Builder.class)
				.map(x -> x.copy())
				.ifPresent(x->v.set(x));

			// Replace with bean from bean store.
			beanStore
				.getBean(ThrownStore.class)
				.ifPresent(x->v.get().impl(x));

			// Replace with builder from:  public [static] ThrownStore.Builder createThrownStore(<args>)
			beanStore
				.beanCreateMethodFinder(ThrownStore.Builder.class)
				.addBean(ThrownStore.Builder.class, v.get())
				.find("createThrownStore")
				.run(x -> v.set(x));

			// Replace with bean from:  public [static] ThrownStore createThrownStore(<args>)
			beanStore
				.beanCreateMethodFinder(ThrownStore.class)
				.addBean(ThrownStore.Builder.class, v.get())
				.find("createThrownStore")
				.run(x -> v.get().impl(x));

			return v.get();
		}

		//-----------------------------------------------------------------------------------------------------------------
		// encoders
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * Returns the encoder group sub-builder.
		 *
		 * @return The builder for the {@link EncoderSet} object in the REST context.
		 */
		public final EncoderSet.Builder encoders() {
			if (encoders == null)
				encoders = createEncoders(beanStore(), resource());
			return encoders;
		}

		/**
		 * Applies an operation to the encoder group sub-builder.
		 *
		 * <p>
		 * Typically used to allow you to execute operations without breaking the fluent flow of the context builder.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	RestContext <jv>context</jv> = RestContext
		 * 		.<jsm>create</jsm>(<jv>resourceClass</jv>, <jv>parentContext</jv>, <jv>servletConfig</jv>)
		 * 		.encoders(<jv>x</jv> -&gt; <jv>x</jv>.add(MyEncoder.<jk>class</jk>))
		 * 		.build();
		 * </p>
		 *
		 * @param operation The operation to apply.
		 * @return This object.
		 */
		public final Builder encoders(Consumer<EncoderSet.Builder> operation) {
			operation.accept(encoders());
			return this;
		}

		/**
		 * Instantiates the encoder group sub-builder.
		 *
		 * <p>
		 * Instantiates based on the following logic:
		 * <ul>
		 * 	<li>Looks for encoders set via any of the following:
		 * 		<ul>
		 * 			<li>{@link RestOpContext.Builder#encoders()}
		 * 			<li>{@link RestOp#encoders()}.
		 * 			<li>{@link Rest#encoders()}.
		 * 		</ul>
		 * 	<li>Looks for a static or non-static <c>createEncoders()</> method that returns <c>{@link Encoder}[]</c> on the
		 * 		resource class with any of the following arguments:
		 * 		<ul>
		 * 			<li>{@link Method} - The Java method this context belongs to.
		 * 			<li>{@link RestContext}
		 * 			<li>{@link BeanStore}
		 * 			<li>Any {@doc jrs.Injection injected beans}.
		 * 		</ul>
		 * 	<li>Resolves it via the bean store registered in this context.
		 * 	<li>Instantiates a <c>Encoder[0]</c>.
		 * </ul>
		 *
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * 	<br>Created by {@link RestContext.Builder#beanStore()}.
		 * @return A new encoder group sub-builder.
		 */
		protected EncoderSet.Builder createEncoders(BeanStore beanStore, Supplier<?> resource) {

			// Default value.
			Value<EncoderSet.Builder> v = Value.of(
				EncoderSet
					.create()
					.beanStore(beanStore)
					.add(IdentityEncoder.INSTANCE)
			);

			// Specify the implementation class if its set as a default.
			defaultClasses()
				.get(EncoderSet.class)
				.ifPresent(x -> v.get().type(x));

			// Replace with builder from bean store.
			beanStore
				.getBean(EncoderSet.Builder.class)
				.map(x -> x.copy())
				.ifPresent(x->v.set(x));

			// Replace with bean from bean store.
			beanStore
				.getBean(EncoderSet.class)
				.ifPresent(x->v.get().impl(x));

			// Replace with builder from:  public [static] EncoderSet.Builder createEncoders(<args>)
			beanStore
				.beanCreateMethodFinder(EncoderSet.Builder.class)
				.addBean(EncoderSet.Builder.class, v.get())
				.find("createEncoders")
				.run(x -> v.set(x));

			// Replace with bean from:  public [static] EncoderSet createEncoders(<args>)
			beanStore
				.beanCreateMethodFinder(EncoderSet.class)
				.addBean(EncoderSet.Builder.class, v.get())
				.find("createEncoders")
				.run(x -> v.get().impl(x));

			return v.get();
		}

		//-----------------------------------------------------------------------------------------------------------------
		// serializers
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * Returns the serializer group sub-builder.
		 *
		 * @return The serializer group sub-builder.
		 */
		public final SerializerSet.Builder serializers() {
			if (serializers == null)
				serializers = createSerializers(beanStore(), resource());
			return serializers;
		}

		/**
		 * Applies an operation to the serializer group sub-builder.
		 *
		 * <p>
		 * Typically used to allow you to execute operations without breaking the fluent flow of the context builder.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	RestContext <jv>context</jv> = RestContext
		 * 		.<jsm>create</jsm>(<jv>resourceClass</jv>, <jv>parentContext</jv>, <jv>servletConfig</jv>)
		 * 		.serializers(<jv>x</jv> -&gt; <jv>x</jv>.add(MySerializer.<jk>class</jk>))
		 * 		.build();
		 * </p>
		 *
		 * @param operation The operation to apply.
		 * @return This object.
		 */
		public final Builder serializers(Consumer<SerializerSet.Builder> operation) {
			operation.accept(serializers());
			return this;
		}

		/**
		 * Instantiates the serializer group sub-builder.
		 *
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * 	<br>Created by {@link RestContext.Builder#beanStore()}.
		 * @return A new serializer group sub-builder.
		 */
		protected SerializerSet.Builder createSerializers(BeanStore beanStore, Supplier<?> resource) {

			// Default value.
			Value<SerializerSet.Builder> v = Value.of(
				SerializerSet
					.create()
					.beanStore(beanStore)
			);

			// Specify the implementation class if its set as a default.
			defaultClasses()
				.get(SerializerSet.class)
				.ifPresent(x -> v.get().type(x));

			// Replace with builder from bean store.
			beanStore
				.getBean(SerializerSet.Builder.class)
				.map(x -> x.copy())
				.ifPresent(x->v.set(x));

			// Replace with bean from bean store.
			beanStore
				.getBean(SerializerSet.class)
				.ifPresent(x->v.get().impl(x));

			// Replace with builder from:  public [static] SerializerSet.Builder createSerializers(<args>)
			beanStore
				.beanCreateMethodFinder(SerializerSet.Builder.class)
				.addBean(SerializerSet.Builder.class, v.get())
				.find("createSerializers")
				.run(x -> v.set(x));

			// Replace with bean from:  public [static] SerializerSet createSerializers(<args>)
			beanStore
				.beanCreateMethodFinder(SerializerSet.class)
				.addBean(SerializerSet.Builder.class, v.get())
				.find("createSerializers")
				.run(x -> v.get().impl(x));

			return v.get();
		}

		//-----------------------------------------------------------------------------------------------------------------
		// parsers
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * Returns the parser group sub-builder.
		 *
		 * @return The parser group sub-builder.
		 */
		public final ParserSet.Builder parsers() {
			if (parsers == null)
				parsers = createParsers(beanStore(), resource());
			return parsers;
		}

		/**
		 * Applies an operation to the parser group sub-builder.
		 *
		 * <p>
		 * Typically used to allow you to execute operations without breaking the fluent flow of the context builder.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	RestContext <jv>context</jv> = RestContext
		 * 		.<jsm>create</jsm>(<jv>resourceClass</jv>, <jv>parentContext</jv>, <jv>servletConfig</jv>)
		 * 		.parsers(<jv>x</jv> -&gt; <jv>x</jv>.add(MyParser.<jk>class</jk>))
		 * 		.build();
		 * </p>
		 *
		 * @param operation The operation to apply.
		 * @return This object.
		 */
		public final Builder parsers(Consumer<ParserSet.Builder> operation) {
			operation.accept(parsers());
			return this;
		}

		/**
		 * Instantiates the parser group sub-builder.
		 *
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * 	<br>Created by {@link RestContext.Builder#beanStore()}.
		 * @return A new parser group sub-builder.
		 */
		protected ParserSet.Builder createParsers(BeanStore beanStore, Supplier<?> resource) {

			// Default value.
			Value<ParserSet.Builder> v = Value.of(
				ParserSet
					.create()
					.beanStore(beanStore)
			);

			// Specify the implementation class if its set as a default.
			defaultClasses()
				.get(ParserSet.class)
				.ifPresent(x -> v.get().type(x));

			// Replace with builder from bean store.
			beanStore
				.getBean(ParserSet.Builder.class)
				.map(x -> x.copy())
				.ifPresent(x->v.set(x));

			// Replace with bean from bean store.
			beanStore
				.getBean(ParserSet.class)
				.ifPresent(x->v.get().impl(x));

			// Replace with builder from:  public [static] ParserSet.Builder createParsers(<args>)
			beanStore
				.beanCreateMethodFinder(ParserSet.Builder.class)
				.addBean(ParserSet.Builder.class, v.get())
				.find("createParsers")
				.run(x -> v.set(x));

			// Replace with bean from:  public [static] ParserSet createParsers(<args>)
			beanStore
				.beanCreateMethodFinder(ParserSet.class)
				.addBean(ParserSet.Builder.class, v.get())
				.find("createParsers")
				.run(x -> v.get().impl(x));

			return v.get();
		}

		//-----------------------------------------------------------------------------------------------------------------
		// methodExecStore
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * Returns the method execution statistics store sub-builder.
		 *
		 * @return The method execution statistics store sub-builder.
		 */
		public final MethodExecStore.Builder methodExecStore() {
			if (methodExecStore == null)
				methodExecStore = createMethodExecStore(beanStore(), resource());
			return methodExecStore;
		}

		/**
		 * Applies an operation to the method execution statistics store sub-builder.
		 *
		 * <p>
		 * Typically used to allow you to execute operations without breaking the fluent flow of the context builder.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	RestContext <jv>context</jv> = RestContext
		 * 		.<jsm>create</jsm>(<jv>resourceClass</jv>, <jv>parentContext</jv>, <jv>servletConfig</jv>)
		 * 		.methodExecStore(<jv>x</jv> -&gt; <jv>x</jv>.statsImplClass(MyStats.<jk>class</jk>))
		 * 		.build();
		 * </p>
		 *
		 * @param operation The operation to apply.
		 * @return This object.
		 */
		public final Builder methodExecStore(Consumer<MethodExecStore.Builder> operation) {
			operation.accept(methodExecStore());
			return this;
		}

		/**
		 * Instantiates the method execution statistics store sub-builder.
		 *
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @return A new method execution statistics store sub-builder.
		 */
		protected MethodExecStore.Builder createMethodExecStore(BeanStore beanStore, Supplier<?> resource) {

			// Default value.
			Value<MethodExecStore.Builder> v = Value.of(
				MethodExecStore
					.create()
					.beanStore(beanStore)
			);

			// Specify the implementation class if its set as a default.
			defaultClasses()
				.get(MethodExecStore.class)
				.ifPresent(x -> v.get().type(x));

			// Replace with builder from bean store.
			beanStore
				.getBean(MethodExecStore.Builder.class)
				.map(x -> x.copy())
				.ifPresent(x->v.set(x));

			// Replace with bean from bean store.
			beanStore
				.getBean(MethodExecStore.class)
				.ifPresent(x->v.get().impl(x));

			// Replace with builder from:  public [static] MethodExecStore.Builder createThrownStore(<args>)
			beanStore
				.beanCreateMethodFinder(MethodExecStore.Builder.class)
				.addBean(MethodExecStore.Builder.class, v.get())
				.find("createMethodExecStore")
				.run(x -> v.set(x));

			// Replace with bean from:  public [static] MethodExecStore createThrownStore(<args>)
			beanStore
				.beanCreateMethodFinder(MethodExecStore.class)
				.addBean(MethodExecStore.Builder.class, v.get())
				.find("createMethodExecStore")
				.run(x -> v.get().impl(x));

			return v.get();
		}

		//-----------------------------------------------------------------------------------------------------------------
		// messages
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * Returns the messages sub-builder.
		 *
		 * @return The messages sub-builder.
		 */
		public final Messages.Builder messages() {
			if (messages == null)
				messages = createMessages(beanStore(), resource());
			return messages;
		}

		/**
		 * Applies an operation to the messages sub-builder.
		 *
		 * <p>
		 * Typically used to allow you to execute operations without breaking the fluent flow of the context builder.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	RestContext <jv>context</jv> = RestContext
		 * 		.<jsm>create</jsm>(<jv>resourceClass</jv>, <jv>parentContext</jv>, <jv>servletConfig</jv>)
		 * 		.messages(<jv>x</jv> -&gt; <jv>x</jv>.name(<js>"MyMessages"</js>))
		 * 		.build();
		 * </p>
		 *
		 * @param operation The operation to apply.
		 * @return This object.
		 */
		public final Builder messages(Consumer<Messages.Builder> operation) {
			operation.accept(messages());
			return this;
		}

		/**
		 * Instantiates the messages sub-builder.
		 *
		 * <p>
		 * By default, the resource bundle name is assumed to match the class name.  For example, given the class
		 * <c>MyClass.java</c>, the resource bundle is assumed to be <c>MyClass.properties</c>.  This property
		 * allows you to override this setting to specify a different location such as <c>MyMessages.properties</c> by
		 * specifying a value of <js>"MyMessages"</js>.
		 *
		 * <p>
		 * 	Resource bundles are searched using the following base name patterns:
		 * 	<ul>
		 * 		<li><js>"{package}.{name}"</js>
		 * 		<li><js>"{package}.i18n.{name}"</js>
		 * 		<li><js>"{package}.nls.{name}"</js>
		 * 		<li><js>"{package}.messages.{name}"</js>
		 * 	</ul>
		 *
		 * <p>
		 * This annotation is used to provide request-localized (based on <c>Accept-Language</c>) messages for the following methods:
		 * <ul class='javatree'>
		 * 	<li class='jm'>{@link RestRequest#getMessage(String, Object...)}
		 * 	<li class='jm'>{@link RestContext#getMessages() RestContext.getMessages()}
		 * </ul>
		 *
		 * <p>
		 * Request-localized messages are also available by passing either of the following parameter types into your Java method:
		 * <ul class='javatree'>
		 * 	<li class='jc'>{@link ResourceBundle} - Basic Java resource bundle.
		 * 	<li class='jc'>{@link Messages} - Extended resource bundle with several convenience methods.
		 * </ul>
		 *
		 * The value can be a relative path like <js>"nls/Messages"</js>, indicating to look for the resource bundle
		 * <js>"com.foo.sample.nls.Messages"</js> if the resource class is in <js>"com.foo.sample"</js>, or it can be an
		 * absolute path like <js>"com.foo.sample.nls.Messages"</js>
		 *
		 * <h5 class='section'>Examples:</h5>
		 * <p class='bcode w800'>
		 * 	<cc># Contents of org/apache/foo/nls/MyMessages.properties</cc>
		 *
		 * 	<ck>HelloMessage</ck> = <cv>Hello {0}!</cv>
		 * </p>
		 * <p class='bcode w800'>
		 * 	<jc>// Contents of org/apache/foo/MyResource.java</jc>
		 *
		 * 	<ja>@Rest</ja>(messages=<js>"nls/MyMessages"</js>)
		 * 	<jk>public class</jk> MyResource {...}
		 *
		 * 		<ja>@RestGet</ja>(<js>"/hello/{you}"</js>)
		 * 		<jk>public</jk> Object helloYou(RestRequest <jv>req</jv>, Messages <jv>messages</jv>, <ja>@Path</ja>(<js>"name"</js>) String <jv>you</jv>) {
		 * 			String <jv>s</jv>;
		 *
		 * 			<jc>// Get it from the RestRequest object.</jc>
		 * 			<jv>s</jv> = <jv>req</jv>.getMessage(<js>"HelloMessage"</js>, <jv>you</jv>);
		 *
		 * 			<jc>// Or get it from the method parameter.</jc>
		 * 			<jv>s</jv> = <jv>messages</jv>.getString(<js>"HelloMessage"</js>, <jv>you</jv>);
		 *
		 * 			<jc>// Or get the message in a locale different from the request.</jc>
		 * 			<jv>s</jv> = <jv>messages</jv>.forLocale(Locale.<jsf>UK</jsf>).getString(<js>"HelloMessage"</js>, <jv>you</jv>);
		 *
		 * 			<jk>return</jk> <jv>s</jv>;
		 * 		}
		 * 	}
		 * </p>
		 *
		 * <ul class='notes'>
		 * 	<li>Mappings are cumulative from super classes.
		 * 		<br>Therefore, you can find and retrieve messages up the class-hierarchy chain.
		 * </ul>
		 *
		 * <ul class='seealso'>
		 * 	<li class='jc'>{@link Messages}
		 * 	<li class='link'>{@doc jrs.Messages}
		 * </ul>
		 *
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @return A new messages sub-builder.
		 */
		protected Messages.Builder createMessages(BeanStore beanStore, Supplier<?> resource) {

			// Default value.
			Value<Messages.Builder> v = Value.of(
				Messages
				.create(resourceClass)
			);

			// Replace with builder from bean store.
			beanStore
				.getBean(Messages.Builder.class)
				.map(x -> x.copy())
				.ifPresent(x->v.set(x));

			// Replace with bean from bean store.
			beanStore
				.getBean(Messages.class)
				.ifPresent(x->v.get().impl(x));

			// Replace with builder from:  public [static] Messages.Builder createMessages(<args>)
			beanStore
				.beanCreateMethodFinder(Messages.Builder.class)
				.find("createMessages")
				.run(x -> v.set(x));

			// Replace with bean from:  public [static] Messages createMessages(<args>)
			beanStore
				.beanCreateMethodFinder(Messages.class)
				.addBean(Messages.Builder.class, v.get())
				.find("createMessages")
				.run(x -> v.get().impl(x));

			return v.get();
		}

		//-----------------------------------------------------------------------------------------------------------------
		// responseProcessors
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * Returns the response processor list sub-builder.
		 *
		 * <p>
		 * Specifies a list of {@link ResponseProcessor} classes that know how to convert POJOs returned by REST methods or
		 * set via {@link RestResponse#setOutput(Object)} into appropriate HTTP responses.
		 *
		 * <p>
		 * By default, the following response handlers are provided in the specified order:
		 * <ul>
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
		 * <p class='bcode w800'>
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
		 * 				<jk>try</jk> (Writer <jv>w</jv> = <jv>res</jv>.getNegotiatedWriter()) {
		 * 					<jc>//Pipe it to the writer ourselves.</jc>
		 * 				}
		 *
		 * 				<jk>return</jk> <jsf>FINISHED</jsf>;  <jc>// We handled it.</jc>
		 *			}
		 * 		}
		 * 	}
		 *
		 * 	<jc>// Option #1 - Defined via annotation.</jc>
		 * 	<ja>@Rest</ja>(responseProcessors=MyResponseProcessor.<jk>class</jk>)
		 * 	<jk>public class</jk> MyResource {
		 *
		 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
		 * 		<jk>public</jk> MyResource(Builder <jv>builder</jv>) <jk>throws</jk> Exception {
		 *
		 * 			<jc>// Using method on builder.</jc>
		 * 			<jv>builder</jv>.responseProcessors(MyResponseProcessor.<jk>class</jk>);
		 * 		}
		 *
		 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
		 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
		 * 		<jk>public void</jk> init(Builder <jv>builder</jv>) <jk>throws</jk> Exception {
		 * 			<jv>builder</jv>.responseProcessors(MyResponseProcessors.<jk>class</jk>);
		 * 		}
		 *
		 * 		<ja>@RestGet</ja>(...)
		 * 		<jk>public</jk> Object myMethod() {
		 * 			<jc>// Return a special object for our handler.</jc>
		 * 			<jk>return new</jk> MySpecialObject();
		 * 		}
		 * 	}
		 * </p>
		 *
		 * <ul class='notes'>
		 * 	<li>
		 * 		Response processors are always inherited from ascendant resources.
		 * 	<li>
		 * 		When defined as a class, the implementation must have one of the following constructors:
		 * 		<ul>
		 * 			<li><code><jk>public</jk> T(RestContext)</code>
		 * 			<li><code><jk>public</jk> T()</code>
		 * 			<li><code><jk>public static</jk> T <jsm>create</jsm>(RestContext)</code>
		 * 			<li><code><jk>public static</jk> T <jsm>create</jsm>()</code>
		 * 		</ul>
		 * 	<li>
		 * 		Inner classes of the REST resource class are allowed.
		 * </ul>
		 *
		 * @return The response processor list sub-builder.
		 */
		public final ResponseProcessorList.Builder responseProcessors() {
			if (responseProcessors == null)
				responseProcessors = createResponseProcessors(beanStore(), resource());
			return responseProcessors;
		}

		/**
		 * Applies an operation to the response processor list sub-builder.
		 *
		 * <p>
		 * Typically used to allow you to execute operations without breaking the fluent flow of the context builder.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	RestContext <jv>context</jv> = RestContext
		 * 		.<jsm>create</jsm>(<jv>resourceClass</jv>, <jv>parentContext</jv>, <jv>servletConfig</jv>)
		 * 		.responseProcessors(<jv>x</jv> -&gt; <jv>x</jv>.add(MyResponseProcessor.<jk>class</jk>))
		 * 		.build();
		 * </p>
		 *
		 * @param operation The operation to apply.
		 * @return This object.
		 */
		public final Builder responseProcessors(Consumer<ResponseProcessorList.Builder> operation) {
			operation.accept(responseProcessors());
			return this;
		}

		/**
		 * Instantiates the response processor list sub-builder.
		 *
		 * <p>
		 * Instantiates based on the following logic:
		 * <ul>
		 * 	<li>Looks for response processors set via any of the following:
		 * 		<ul>
		 * 			<li>{@link RestContext.Builder#responseProcessors()}
		 * 			<li>{@link Rest#responseProcessors()}.
		 * 		</ul>
		 * 	<li>Looks for a static or non-static <c>createResponseProcessors()</> method that returns <c>{@link ResponseProcessor}[]</c> on the
		 * 		resource class with any of the following arguments:
		 * 		<ul>
		 * 			<li>{@link RestContext}
		 * 			<li>{@link BeanStore}
		 * 			<li>Any {@doc jrs.Injection injected beans}.
		 * 		</ul>
		 * 	<li>Resolves it via the bean store registered in this context.
		 * 	<li>Instantiates a <c>ResponseProcessor[0]</c>.
		 * </ul>
		 *
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @return A new response processor list sub-builder.
		 */
		protected ResponseProcessorList.Builder createResponseProcessors(BeanStore beanStore, Supplier<?> resource) {

			// Default value.
			Value<ResponseProcessorList.Builder> v = Value.of(
				 ResponseProcessorList
				 	.create()
				 	.beanStore(beanStore)
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

			// Replace with builder from bean store.
			beanStore
				.getBean(ResponseProcessorList.Builder.class)
				.map(x -> x.copy())
				.ifPresent(x -> v.set(x));

			// Replace with bean from bean store.
			beanStore
				.getBean(ResponseProcessorList.class)
				.ifPresent(x -> v.get().impl(x));

			// Replace with builder from:  public [static] ResponseProcessorList.Builder createResponseProcessors(<args>)
			beanStore
				.beanCreateMethodFinder(ResponseProcessorList.Builder.class)
				.addBean(ResponseProcessorList.Builder.class, v.get())
				.find("createResponseProcessors")
				.run(x -> v.set(x));

			// Replace with bean from:  public [static] ResponseProcessorList createResponseProcessors(<args>)
			beanStore
				.beanCreateMethodFinder(ResponseProcessorList.class)
				.addBean(ResponseProcessorList.Builder.class, v.get())
				.find("createResponseProcessors")
				.run(x -> v.get().impl(x));

			return v.get();
		}

		//-----------------------------------------------------------------------------------------------------------------
		// callLogger
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * Returns the call logger sub-builder.
		 *
		 * <p>
		 * Specifies the logger to use for logging of HTTP requests and responses.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	<jc>// Our customized logger.</jc>
		 * 	<jk>public class</jk> MyLogger <jk>extends</jk> BasicRestLogger {
		 *
		 * 		<ja>@Override</ja>
		 * 			<jk>protected void</jk> log(Level <jv>level</jv>, String <jv>msg</jv>, Throwable <jv>e</jv>) {
		 * 			<jc>// Handle logging ourselves.</jc>
		 * 		}
		 * 	}
		 *
		 * 	<jc>// Option #1 - Registered via annotation resolving to a config file setting with default value.</jc>
		 * 	<ja>@Rest</ja>(callLogger=MyLogger.<jk>class</jk>)
		 * 	<jk>public class</jk> MyResource {
		 *
		 * 		<jc>// Option #2 - Registered via builder passed in through resource constructor.</jc>
		 * 		<jk>public</jk> MyResource(Builder <jv>builder</jv>) <jk>throws</jk> Exception {
		 *
		 * 			<jc>// Using method on builder.</jc>
		 * 			<jv>builder</jv>.callLogger(MyLogger.<jk>class</jk>);
		 * 		}
		 *
		 * 		<jc>// Option #3 - Registered via builder passed in through init method.</jc>
		 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
		 * 		<jk>public void</jk> init(Builder <jv>builder</jv>) <jk>throws</jk> Exception {
		 * 			<jv>builder</jv>.callLogger(MyLogger.<jk>class</jk>);
		 * 		}
		 * 	}
		 * </p>
		 *
		 * <ul class='notes'>
		 * 	<li>
		 * 		The default call logger if not specified is {@link BasicRestLogger}.
		 * 	<li>
		 * 		The resource class itself will be used if it implements the {@link RestLogger} interface and not
		 * 		explicitly overridden via this annotation.
		 * 	<li>
		 * 		When defined as a class, the implementation must have one of the following constructors:
		 * 		<ul>
		 * 			<li><code><jk>public</jk> T(RestContext)</code>
		 * 			<li><code><jk>public</jk> T()</code>
		 * 			<li><code><jk>public static</jk> T <jsm>create</jsm>(RestContext)</code>
		 * 			<li><code><jk>public static</jk> T <jsm>create</jsm>()</code>
		 * 		</ul>
		 * 	<li>
		 * 		Inner classes of the REST resource class are allowed.
		 * </ul>
		 *
		 * <ul class='seealso'>
		 * 	<li class='link'>{@doc jrs.LoggingAndDebugging}
		 * 	<li class='ja'>{@link Rest#callLogger()}
		 * </ul>
		 *
		 * @return The call logger sub-builder.
		 * @throws RuntimeException If {@link #init(Supplier)} has not been called.
		 */
		public final RestLogger.Builder callLogger() {
			if (callLogger == null)
				callLogger = createCallLogger(beanStore(), resource());
			return callLogger;
		}

		/**
		 * Applies an operation to the call logger sub-builder.
		 *
		 * <p>
		 * Typically used to allow you to execute operations without breaking the fluent flow of the context builder.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	RestContext <jv>context</jv> = RestContext
		 * 		.<jsm>create</jsm>(<jv>resourceClass</jv>, <jv>parentContext</jv>, <jv>servletConfig</jv>)
		 * 		.callLogger(<jv>x</jv> -&gt; <jv>x</jv>.disabled())
		 * 		.build();
		 * </p>
		 *
		 * @param operation The operation to apply.
		 * @return This object.
		 */
		public final Builder callLogger(Consumer<RestLogger.Builder> operation) {
			operation.accept(callLogger());
			return this;
		}

		/**
		 * Instantiates the call logger sub-builder.
		 *
		 * <p>
		 * Instantiates based on the following logic:
		 * <ul>
		 * 	<li>Returns the resource class itself is an instance of RestLogger.
		 * 	<li>Looks for REST call logger set via any of the following:
		 * 		<ul>
		 * 			<li>{@link RestContext.Builder#callLogger()}
		 * 			<li>{@link Rest#callLogger()}.
		 * 		</ul>
		 * 	<li>Looks for a static or non-static <c>createCallLogger()</> method that returns {@link RestLogger} on the
		 * 		resource class with any of the following arguments:
		 * 		<ul>
		 * 			<li>{@link RestContext}
		 * 			<li>{@link BeanStore}
		 * 			<li>{@link BasicFileFinder}
		 * 			<li>Any {@doc jrs.Injection injected beans}.
		 * 		</ul>
		 * 	<li>Resolves it via the bean store registered in this context.
		 * 	<li>Instantiates a {@link BasicFileFinder}.
		 * </ul>
		 *
		 * <ul class='seealso'>
		 * 	<li class='jm'>{@link RestContext.Builder#callLogger()}
		 * </ul>
		 *
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @return A new call logger sub-builder.
		 */
		protected RestLogger.Builder createCallLogger(BeanStore beanStore, Supplier<?> resource) {

			// Default value.
			Value<RestLogger.Builder> v = Value.of(
				RestLogger
					.create()
					.beanStore(beanStore)
					.normalRules(  // Rules when debugging is not enabled.
						RestLoggerRule.create()  // Log 500+ errors with status-line and header information.
							.statusFilter(a -> a >= 500)
							.level(SEVERE)
							.requestDetail(HEADER)
							.responseDetail(HEADER)
							.build(),
						RestLoggerRule.create()  // Log 400-500 errors with just status-line information.
							.statusFilter(a -> a >= 400)
							.level(WARNING)
							.requestDetail(STATUS_LINE)
							.responseDetail(STATUS_LINE)
							.build()
					)
					.debugRules(  // Rules when debugging is enabled.
						RestLoggerRule.create()  // Log everything with full details.
							.level(SEVERE)
							.requestDetail(ENTITY)
							.responseDetail(ENTITY)
							.build()
					)
			);

			// Replace with builder from bean store.
			beanStore
				.getBean(RestLogger.Builder.class)
				.map(x -> x.copy())
				.ifPresent(x-> v.set(x));

			// Replace with bean from bean store.
			beanStore
				.getBean(RestLogger.class)
				.ifPresent(x-> v.get().impl(x));

			// Replace with this bean.
			resourceAs(RestLogger.class)
				.ifPresent(x -> v.get().impl(x));

			// Specify the bean type if its set as a default.
			defaultClasses()
				.get(RestLogger.class)
				.ifPresent(x -> v.get().type(x));

			// Replace with builder from:  public [static] RestLogger.Builder createCallLogger(<args>)
			beanStore
				.beanCreateMethodFinder(RestLogger.Builder.class)
				.addBean(RestLogger.Builder.class, v.get())
				.find("createCallLogger")
				.run(x -> v.set(x));

			// Replace with bean from:  public [static] RestLogger createCallLogger(<args>)
			beanStore
				.beanCreateMethodFinder(RestLogger.class)
				.addBean(RestLogger.Builder.class, v.get())
				.find("createCallLogger")
				.run(x -> v.get().impl(x));

			return v.get();
		}

		//-----------------------------------------------------------------------------------------------------------------
		// partSerializer
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * Returns the bean context sub-builder.
		 *
		 * @return The bean context sub-builder.
		 */
		public final BeanContext.Builder beanContext() {
			if (beanContext == null)
				beanContext = createBeanContext(beanStore(), resource());
			return beanContext;
		}

		/**
		 * Applies an operation to the bean context sub-builder.
		 *
		 * <p>
		 * Typically used to allow you to execute operations without breaking the fluent flow of the context builder.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	RestContext <jv>context</jv> = RestContext
		 * 		.<jsm>create</jsm>(<jv>resourceClass</jv>, <jv>parentContext</jv>, <jv>servletConfig</jv>)
		 * 		.beanContext(<jv>x</jv> -&gt; <jv>x</jv>.interfaces(MyInterface.<jk>class</jk>))
		 * 		.build();
		 * </p>
		 *
		 * @param operation The operation to apply.
		 * @return This object.
		 */
		public final Builder beanContext(Consumer<BeanContext.Builder> operation) {
			operation.accept(beanContext());
			return this;
		}

		/**
		 * Instantiates the bean context sub-builder.
		 *
		 * <p>
		 * Instantiates based on the following logic:
		 * <ul>
		 * 	<li>Returns the resource class itself is an instance of {@link HttpPartSerializer}.
		 * 	<li>Looks for part serializer set via any of the following:
		 * 		<ul>
		 * 			<li>{@link RestContext.Builder#partSerializer()}
		 * 			<li>{@link Rest#partSerializer()}.
		 * 		</ul>
		 * 	<li>Looks for a static or non-static <c>createPartSerializer()</> method that returns <c>{@link HttpPartSerializer}</c> on the
		 * 		resource class with any of the following arguments:
		 * 		<ul>
		 * 			<li>{@link RestContext}
		 * 			<li>{@link BeanStore}
		 * 			<li>Any {@doc jrs.Injection injected beans}.
		 * 		</ul>
		 * 	<li>Resolves it via the bean store registered in this context.
		 * 	<li>Instantiates an {@link OpenApiSerializer}.
		 * </ul>
		 *
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @return A new bean context sub-builder.
		 */
		protected BeanContext.Builder createBeanContext(BeanStore beanStore, Supplier<?> resource) {

			// Default value.
			Value<BeanContext.Builder> v = Value.of(
				BeanContext.create()
			);

			// Replace with builder from bean store.
			beanStore
				.getBean(BeanContext.Builder.class)
				.map(x -> x.copy())
				.ifPresent(x -> v.set(x));

			// Replace with bean from bean store.
			beanStore
				.getBean(BeanContext.class)
				.ifPresent(x -> v.get().impl(x));

			// Replace with builder from:  public [static] BeanContext.Builder createBeanContext(<args>)
			beanStore
				.beanCreateMethodFinder(BeanContext.Builder.class)
				.addBean(BeanContext.Builder.class, v.get())
				.find("createBeanContext")
				.run(x -> v.set(x));

			// Replace with builder from:  public [static] BeanContext createBeanContext(<args>)
			beanStore
				.beanCreateMethodFinder(BeanContext.class)
				.addBean(BeanContext.Builder.class, v.get())
				.find("createBeanContext")
				.run(x -> v.get().impl(x));

			return v.get();
		}

		//-----------------------------------------------------------------------------------------------------------------
		// partSerializer
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * Returns the part serializer sub-builder.
		 *
		 * @return The part serializer sub-builder.
		 */
		public final HttpPartSerializer.Creator partSerializer() {
			if (partSerializer == null)
				partSerializer = createPartSerializer(beanStore(), resource());
			return partSerializer;
		}

		/**
		 * Applies an operation to the part serializer sub-builder.
		 *
		 * <p>
		 * Typically used to allow you to execute operations without breaking the fluent flow of the context builder.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	RestContext <jv>context</jv> = RestContext
		 * 		.<jsm>create</jsm>(<jv>resourceClass</jv>, <jv>parentContext</jv>, <jv>servletConfig</jv>)
		 * 		.partSerializer(<jv>x</jv> -&gt; <jv>x</jv>.builder(OpenApiSerializer.Builder.<jk>class</jk>, <jv>y</jv> -&gt; <jv>y</jv>.sortProperties()))
		 * 		.build();
		 * </p>
		 *
		 * @param operation The operation to apply.
		 * @return This object.
		 */
		public final Builder partSerializer(Consumer<HttpPartSerializer.Creator> operation) {
			operation.accept(partSerializer());
			return this;
		}

		/**
		 * Instantiates the part serializer sub-builder.
		 *
		 * <p>
		 * Instantiates based on the following logic:
		 * <ul>
		 * 	<li>Returns the resource class itself is an instance of {@link HttpPartSerializer}.
		 * 	<li>Looks for part serializer set via any of the following:
		 * 		<ul>
		 * 			<li>{@link RestContext.Builder#partSerializer()}
		 * 			<li>{@link Rest#partSerializer()}.
		 * 		</ul>
		 * 	<li>Looks for a static or non-static <c>createPartSerializer()</> method that returns <c>{@link HttpPartSerializer}</c> on the
		 * 		resource class with any of the following arguments:
		 * 		<ul>
		 * 			<li>{@link RestContext}
		 * 			<li>{@link BeanStore}
		 * 			<li>Any {@doc jrs.Injection injected beans}.
		 * 		</ul>
		 * 	<li>Resolves it via the bean store registered in this context.
		 * 	<li>Instantiates an {@link OpenApiSerializer}.
		 * </ul>
		 *
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @return A new part serializer sub-builder.
		 */
		protected HttpPartSerializer.Creator createPartSerializer(BeanStore beanStore, Supplier<?> resource) {

			// Default value.
			Value<HttpPartSerializer.Creator> v = Value.of(
				HttpPartSerializer
					.creator()
					.type(OpenApiSerializer.class)
			);

			// Replace with builder from bean store.
			beanStore
				.getBean(HttpPartSerializer.Creator.class)
				.map(x -> x.copy())
				.ifPresent(x -> v.set(x));

			// Replace with bean from bean store.
			beanStore
				.getBean(HttpPartSerializer.class)
				.ifPresent(x -> v.get().impl(x));

			// Replace with this bean.
			resourceAs(HttpPartSerializer.class)
				.ifPresent(x -> v.get().impl(x));

			// Specify the bean type if its set as a default.
			defaultClasses()
				.get(HttpPartSerializer.class)
				.ifPresent(x -> v.get().type(x));

			// Replace with builder from:  public [static] HttpPartSerializer.Creator createPartSerializer(<args>)
			beanStore
				.beanCreateMethodFinder(HttpPartSerializer.Creator.class)
				.addBean(HttpPartSerializer.Creator.class, v.get())
				.find("createPartSerializer")
				.run(x -> v.set(x));

			// Replace with bean from:  public [static] HttpPartSerializer createPartSerializer(<args>)
			beanStore
				.beanCreateMethodFinder(HttpPartSerializer.class)
				.addBean(HttpPartSerializer.Creator.class, v.get())
				.find("createPartSerializer")
				.run(x -> v.get().impl(x));

			return v.get();
		}

		//-----------------------------------------------------------------------------------------------------------------
		// partParser
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * Returns the part parser sub-builder.
		 *
		 * @return The part parser sub-builder.
		 */
		public final HttpPartParser.Creator partParser() {
			if (partParser == null)
				partParser = createPartParser(beanStore(), resource());
			return partParser;
		}

		/**
		 * Applies an operation to the part parser sub-builder.
		 *
		 * <p>
		 * Typically used to allow you to execute operations without breaking the fluent flow of the context builder.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	RestContext <jv>context</jv> = RestContext
		 * 		.<jsm>create</jsm>(<jv>resourceClass</jv>, <jv>parentContext</jv>, <jv>servletConfig</jv>)
		 * 		.partParser(<jv>x</jv> -&gt; <jv>x</jv>.builder(OpenApiParser.Builder.<jk>class</jk>, <jv>y</jv> -&gt; <jv>y</jv>.ignoreUnknownBeanProperties()))
		 * 		.build();
		 * </p>
		 *
		 * @param operation The operation to apply.
		 * @return This object.
		 */
		public final Builder partParser(Consumer<HttpPartParser.Creator> operation) {
			operation.accept(partParser());
			return this;
		}

		/**
		 * Instantiates the part parser sub-builder.
		 *
		 * <p>
		 * Instantiates based on the following logic:
		 * <ul>
		 * 	<li>Returns the resource class itself is an instance of {@link HttpPartParser}.
		 * 	<li>Looks for part parser set via any of the following:
		 * 		<ul>
		 * 			<li>{@link RestContext.Builder#partParser()}
		 * 			<li>{@link Rest#partParser()}.
		 * 		</ul>
		 * 	<li>Looks for a static or non-static <c>createPartParser()</> method that returns <c>{@link HttpPartParser}</c> on the
		 * 		resource class with any of the following arguments:
		 * 		<ul>
		 * 			<li>{@link RestContext}
		 * 			<li>{@link BeanStore}
		 * 			<li>Any {@doc jrs.Injection injected beans}.
		 * 		</ul>
		 * 	<li>Resolves it via the bean store registered in this context.
		 * 	<li>Instantiates an {@link OpenApiParser}.
		 * </ul>
		 *
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @return A new part parser sub-builder.
		 */
		protected HttpPartParser.Creator createPartParser(BeanStore beanStore, Supplier<?> resource) {

			// Default value.
			Value<HttpPartParser.Creator> v = Value.of(
				HttpPartParser
					.creator()
					.type(OpenApiParser.class)
			);

			// Replace with builder from bean store.
			beanStore
				.getBean(HttpPartParser.Creator.class)
				.map(x -> x.copy())
				.ifPresent(x -> v.set(x));

			// Replace with bean from bean store.
			beanStore
				.getBean(HttpPartParser.class)
				.ifPresent(x -> v.get().impl(x));

			// Replace with this bean.
			resourceAs(HttpPartParser.class)
				.ifPresent(x -> v.get().impl(x));

			// Specify the bean type if its set as a default.
			defaultClasses()
				.get(HttpPartParser.class)
				.ifPresent(x -> v.get().type(x));

			// Replace with builder from:  public [static] HttpPartParser.Creator createPartParser(<args>)
			beanStore
				.beanCreateMethodFinder(HttpPartParser.Creator.class)
				.addBean(HttpPartParser.Creator.class, v.get())
				.find("createPartParser")
				.run(x -> v.set(x));

			// Replace with bean from:  public [static] HttpPartParser createPartParser(<args>)
			beanStore
				.beanCreateMethodFinder(HttpPartParser.class)
				.addBean(HttpPartParser.Creator.class, v.get())
				.find("createPartParser")
				.run(x -> v.get().impl(x));

			return v.get();
		}

		//-----------------------------------------------------------------------------------------------------------------
		// jsonSchemaGenerator
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * Returns the JSON schema generator sub-builder.
		 *
		 * @return The JSON schema generator sub-builder.
		 */
		public final JsonSchemaGenerator.Builder jsonSchemaGenerator() {
			if (jsonSchemaGenerator == null)
				jsonSchemaGenerator = createJsonSchemaGenerator(beanStore(), resource());
			return jsonSchemaGenerator;
		}

		/**
		 * Applies an operation to the JSON schema generator sub-builder.
		 *
		 * <p>
		 * Typically used to allow you to execute operations without breaking the fluent flow of the context builder.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	RestContext <jv>context</jv> = RestContext
		 * 		.<jsm>create</jsm>(<jv>resourceClass</jv>, <jv>parentContext</jv>, <jv>servletConfig</jv>)
		 * 		.jsonSchemaGenerator(<jv>x</jv> -&gt; <jv>x</jv>.allowNestedExamples()))
		 * 		.build();
		 * </p>
		 *
		 * @param operation The operation to apply.
		 * @return This object.
		 */
		public final Builder jsonSchemaGenerator(Consumer<JsonSchemaGenerator.Builder> operation) {
			operation.accept(jsonSchemaGenerator());
			return this;
		}

		/**
		 * Instantiates the JSON schema generator sub-builder.
		 *
		 * <p>
		 * Instantiates based on the following logic:
		 * <ul>
		 * 	<li>Looks for a static or non-static <c>createJsonSchemaGenerator()</> method that returns <c>{@link JsonSchemaGenerator}</c> on the
		 * 		resource class with any of the following arguments:
		 * 		<ul>
		 * 			<li>{@link RestContext}
		 * 			<li>{@link BeanStore}
		 * 			<li>Any {@doc jrs.Injection injected beans}.
		 * 		</ul>
		 * 	<li>Resolves it via the bean store registered in this context.
		 * 	<li>Instantiates a new {@link JsonSchemaGenerator} using the property store of this context..
		 * </ul>
		 *
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @return A new JSON schema generator sub-builder.
		 */
		protected JsonSchemaGenerator.Builder createJsonSchemaGenerator(BeanStore beanStore, Supplier<?> resource) {

			// Default value.
			Value<JsonSchemaGenerator.Builder> v = Value.of(
				JsonSchemaGenerator.create()
			);

			// Replace with builder from bean store.
			beanStore
				.getBean(JsonSchemaGenerator.Builder.class)
				.map(x -> x.copy())
				.ifPresent(x -> v.set(x));

			// Replace with bean from bean store.
			beanStore
				.getBean(JsonSchemaGenerator.class)
				.ifPresent(x -> v.get().impl(x));

			// Replace with builder from:  public [static] JsonSchemaGeneratorBuilder createJsonSchemaGenerator(<args>)
			beanStore
				.beanCreateMethodFinder(JsonSchemaGenerator.Builder.class)
				.addBean(JsonSchemaGenerator.Builder.class, v.get())
				.find("createJsonSchemaGenerator")
				.run(x -> v.set(x));

			// Replace with bean from:  public [static] JsonSchemaGenerator createJsonSchemaGenerator(<args>)
			beanStore
				.beanCreateMethodFinder(JsonSchemaGenerator.class)
				.addBean(JsonSchemaGenerator.Builder.class, v.get())
				.find("createJsonSchemaGenerator")
				.run(x -> v.get().impl(x));

			return v.get();
		}

		//-----------------------------------------------------------------------------------------------------------------
		// fileFinder
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * Returns the file finder sub-builder.
		 *
		 * @return The file finder sub-builder.
		 */
		public FileFinder.Builder fileFinder() {
			if (fileFinder == null)
				fileFinder = createFileFinder(beanStore(), resource());
			return fileFinder;
		}

		/**
		 * Applies an operation to the file finder sub-builder.
		 *
		 * <p>
		 * Typically used to allow you to execute operations without breaking the fluent flow of the context builder.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	RestContext <jv>context</jv> = RestContext
		 * 		.<jsm>create</jsm>(<jv>resourceClass</jv>, <jv>parentContext</jv>, <jv>servletConfig</jv>)
		 * 		.fileFinder(<jv>x</jv> -&gt; <jv>x</jv>.dir(<js>"/mydir"</js>)))
		 * 		.build();
		 * </p>
		 *
		 * @param operation The operation to apply.
		 * @return This object.
		 */
		public final Builder fileFinder(Consumer<FileFinder.Builder> operation) {
			operation.accept(fileFinder());
			return this;
		}

		/**
		 * Instantiates the file finder sub-builder.
		 *
		 * <p>
		 * The file finder is used to retrieve localized files from the classpath.
		 *
		 * <p>
		 * Used to retrieve localized files from the classpath for a variety of purposes including:
		 * <ul>
		 * 	<li>Resolution of {@link FileVar $F} variable contents.
		 * </ul>
		 *
		 * <p>
		 * The file finder can be accessed through the following methods:
		 * <ul class='javatree'>
		 * 	<li class='jm'>{@link RestContext#getFileFinder()}
		 * 	<li class='jm'>{@link RestRequest#getFileFinder()}
		 * </ul>
		 *
		 * <p>
		 * The file finder is instantiated via the {@link RestContext.Builder#createFileFinder(BeanStore,Supplier)} method which in turn instantiates
		 * based on the following logic:
		 * <ul>
		 * 	<li>Returns the resource class itself if it's an instance of {@link FileFinder}.
		 * 	<li>Looks for file finder setting.
		 * 	<li>Looks for a public <c>createFileFinder()</> method on the resource class with an optional {@link RestContext} argument.
		 * 	<li>Instantiates the default file finder as specified via file finder default setting.
		 * 	<li>Instantiates a {@link BasicFileFinder} which provides basic support for finding localized
		 * 		resources on the classpath and JVM working directory.
		 * </ul>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	<jc>// Create a file finder that looks for files in the /files working subdirectory, but overrides the find()
		 * 	// method for special handling of special cases.</jc>
		 * 	<jk>public class</jk> MyFileFinder <jk>extends</jk> BasicFileFinder {
		 *
		 * 		<jk>public</jk> MyFileFinder() {
		 * 			<jk>super</jk>(
		 * 				<jk>new</jk> FileFinderBuilder()
		 * 					.dir(<js>"/files"</js>)
		 *			);
		 * 		}
		 *
		 *		<ja>@Override</ja> <jc>// FileFinder</jc>
		 * 		<jk>protected</jk> Optional&lt;InputStream&gt; find(String <jv>name</jv>, Locale <jv>locale</jv>) <jk>throws</jk> IOException {
		 * 			<jc>// Do special handling or just call super.find().</jc>
		 * 			<jk>return super</jk>.find(<jv>name</jv>, <jv>locale</jv>);
		 * 		}
		 * 	}
		 * </p>
		 *
		 * 	<jc>// Option #1 - Registered via annotation.</jc>
		 * 	<ja>@Rest</ja>(fileFinder=MyFileFinder.<jk>class</jk>)
		 * 	<jk>public class</jk> MyResource {
		 *
		 * 		<jc>// Option #2 - Created via createFileFinder() method.</jc>
		 * 		<jk>public</jk> FileFinder createFileFinder(RestContext <jv>context</jv>) <jk>throws</jk> Exception {
		 * 			<jk>return new</jk> MyFileFinder();
		 * 		}
		 *
		 * 		<jc>// Option #3 - Registered via builder passed in through resource constructor.</jc>
		 * 		<jk>public</jk> MyResource(Builder <jv>builder</jv>) <jk>throws</jk> Exception {
		 *
		 * 			<jc>// Using method on builder.</jc>
		 * 			<jv>builder</jv>.fileFinder(MyFileFinder.<jk>class</jk>);
		 *
		 * 			<jc>// Use a pre-instantiated object instead.</jc>
		 * 			<jv>builder</jv>.fileFinder(<jk>new</jk> MyFileFinder());
		 * 		}
		 *
		 * 		<jc>// Option #4 - Registered via builder passed in through init method.</jc>
		 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
		 * 		<jk>public void</jk> init(Builder <jv>builder</jv>) <jk>throws</jk> Exception {
		 * 			<jv>builder</jv>.fileFinder(MyFileFinder.<jk>class</jk>);
		 * 		}
		 *
		 * 		<jc>// Create a REST method that uses the file finder.</jc>
		 * 		<ja>@RestGet</ja>
		 * 		<jk>public</jk> InputStream foo(RestRequest <jv>req</jv>) {
		 * 			<jk>return</jk> <jv>req</jv>.getFileFinder().getStream(<js>"foo.json"</js>).orElseThrow(NotFound::<jk>new</jk>);
		 * 		}
		 * 	}
		 * </p>
		 * <p>
		 * Instantiates based on the following logic:
		 * <ul>
		 * 	<li>Returns the resource class itself is an instance of {@link FileFinder}.
		 * 	<li>Looks for file finder value set via any of the following:
		 * 		<ul>
		 * 			<li>{@link RestContext.Builder#fileFinder()}
		 * 			<li>{@link Rest#fileFinder()}.
		 * 		</ul>
		 * 	<li>Resolves it via the {@link RestContext.Builder#beanStore() bean store} registered in this context (including Spring beans if using SpringRestServlet).
		 * 	<li>Looks for file finder default setting.
		 * </ul>
		 *
		 * <p>
		 * Your REST class can also implement a create method called <c>createFileFinder()</c> to instantiate your own
		 * file finder.
		 *
		 * <h5 class='figure'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	<ja>@Rest</ja>
		 * 	<jk>public class</jk> MyRestClass {
		 *
		 * 		<jk>public</jk> FileFinder createFileFinder() <jk>throws</jk> Exception {
		 * 			<jc>// Create your own file finder here.</jc>
		 * 		}
		 * 	}
		 * </p>
		 *
		 * <p>
		 * The <c>createFileFinder()</c> method can be static or non-static can contain any of the following arguments:
		 * <ul>
		 * 	<li>{@link FileFinder} - The file finder that would have been returned by this method.
		 * 	<li>{@link RestContext} - This REST context.
		 * 	<li>{@link BeanStore} - The bean store of this REST context.
		 * 	<li>Any {@doc jrs.Injection injected bean} types.  Use {@link Optional} arguments for beans that may not exist.
		 * </ul>
		 *
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @return A new file finder sub-builder.
		 */
		protected FileFinder.Builder createFileFinder(BeanStore beanStore, Supplier<?> resource) {

			// Default value.
			Value<FileFinder.Builder> v = Value.of(
				FileFinder
					.create()
					.beanStore(beanStore)
					.dir("static")
					.dir("htdocs")
					.cp(resourceClass, "htdocs", true)
					.cp(resourceClass, "/htdocs", true)
					.caching(1_000_000)
					.exclude("(?i).*\\.(class|properties)")
			);

			// Replace with builder from bean store.
			beanStore
				.getBean(FileFinder.Builder.class)
				.map(x -> x.copy())
				.ifPresent(x -> v.set(x));

			// Replace with bean from bean store.
			beanStore
				.getBean(FileFinder.class)
				.ifPresent(x -> v.get().impl(x));

			// Replace with this bean.
			resourceAs(FileFinder.class)
				.ifPresent(x -> v.get().impl(x));

			// Specify the bean type if its set as a default.
			defaultClasses()
				.get(FileFinder.class)
				.ifPresent(x -> v.get().type(x));

			// Replace with builder from:  public [static] FileFinder.Builder createFileFinder(<args>)
			beanStore
				.beanCreateMethodFinder(FileFinder.Builder.class)
				.addBean(FileFinder.Builder.class, v.get())
				.find("createFileFinder")
				.run(x -> v.set(x));

			// Replace with bean from:  public [static] FileFinder createFileFinder(<args>)
			beanStore
				.beanCreateMethodFinder(FileFinder.class)
				.addBean(FileFinder.Builder.class, v.get())
				.find("createFileFinder")
				.run(x -> v.get().impl(x));

			return v.get();
		}

		//-----------------------------------------------------------------------------------------------------------------
		// staticFiles
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * Returns the static files sub-builder.
		 *
		 * @return The static files sub-builder.
		 */
		public StaticFiles.Builder staticFiles() {
			if (staticFiles == null)
				staticFiles = createStaticFiles(beanStore(), resource());
			return staticFiles;
		}

		/**
		 * Applies an operation to the static files sub-builder.
		 *
		 * <p>
		 * Typically used to allow you to execute operations without breaking the fluent flow of the context builder.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	RestContext <jv>context</jv> = RestContext
		 * 		.<jsm>create</jsm>(<jv>resourceClass</jv>, <jv>parentContext</jv>, <jv>servletConfig</jv>)
		 * 		.staticFiles(<jv>x</jv> -&gt; <jv>x</jv>.dir(<js>"/mydir"</js>)))
		 * 		.build();
		 * </p>
		 *
		 * @param operation The operation to apply.
		 * @return This object.
		 */
		public final Builder staticFiles(Consumer<StaticFiles.Builder> operation) {
			operation.accept(staticFiles());
			return this;
		}

		/**
		 * Instantiates the static files sub-builder.
		 *
		 * <p>
		 * Used to retrieve localized files to be served up as static files through the REST API via the following
		 * predefined methods:
		 * <ul class='javatree'>
		 * 	<li class='jm'>{@link BasicRestObject#getHtdoc(String, Locale)}.
		 * 	<li class='jm'>{@link BasicRestServlet#getHtdoc(String, Locale)}.
		 * </ul>
		 *
		 * <p>
		 * The static file finder can be accessed through the following methods:
		 * <ul class='javatree'>
		 * 	<li class='jm'>{@link RestContext#getStaticFiles()}
		 * 	<li class='jm'>{@link RestRequest#getStaticFiles()}
		 * </ul>
		 *
		 * <p>
		 * The static file finder is instantiated via the {@link RestContext.Builder#createStaticFiles(BeanStore,Supplier)} method which in turn instantiates
		 * based on the following logic:
		 * <ul>
		 * 	<li>Returns the resource class itself is an instance of {@link StaticFiles}.
		 * 	<li>Looks for a public <c>createStaticFiles()</> method on the resource class with an optional {@link RestContext} argument.
		 * 	<li>Instantiates a {@link BasicStaticFiles} which provides basic support for finding localized
		 * 		resources on the classpath and JVM working directory..
		 * </ul>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	<jc>// Create a static file finder that looks for files in the /files working subdirectory, but overrides the find()
		 * 	// and resolve methods for special handling of special cases and adds a Foo header to all requests.</jc>
		 * 	<jk>public class</jk> MyStaticFiles <jk>extends</jk> StaticFiles {
		 *
		 * 		<jk>public</jk> MyStaticFiles() <jk>extends</jk> BasicStaticFiles {
		 * 			<jk>super</jk>(
		 * 				<jk>new</jk> StaticFilesBuilder()
		 * 					.dir(<js>"/files"</js>)
		 * 					.headers(BasicStringHeader.<jsm>of</jsm>(<js>"Foo"</js>, <js>"bar"</js>))
		 * 			);
		 * 		}
		 *
		 *		<ja>@Override</ja> <jc>// FileFinder</jc>
		 * 		<jk>protected</jk> Optional&lt;InputStream&gt; find(String <jv>name</jv>, Locale <jv>locale</jv>) <jk>throws</jk> IOException {
		 * 			<jc>// Do special handling or just call super.find().</jc>
		 * 			<jk>return super</jk>.find(<jv>name</jv>, <jv>locale</jv>);
		 * 		}
		 *
		 *		<ja>@Override</ja> <jc>// staticFiles</jc>
		 * 		<jk>public</jk> Optional&lt;BasicHttpResource&gt; resolve(String <jv>path</jv>, Locale <jv>locale</jv>) {
		 * 			<jc>// Do special handling or just call super.resolve().</jc>
		 * 			<jk>return super</jk>.resolve(<jv>path</jv>, <jv>locale</jv>);
		 * 		}
		 * 	}
		 * </p>
		 *
		 * 	<jc>// Option #1 - Registered via annotation.</jc>
		 * 	<ja>@Rest</ja>(staticFiles=MyStaticFiles.<jk>class</jk>)
		 * 	<jk>public class</jk> MyResource {
		 *
		 * 		<jc>// Option #2 - Created via createStaticFiles() method.</jc>
		 * 		<jk>public</jk> StaticFiles createStaticFiles(RestContext <jv>context</jv>) <jk>throws</jk> Exception {
		 * 			<jk>return new</jk> MyStaticFiles();
		 * 		}
		 *
		 * 		<jc>// Option #3 - Registered via builder passed in through resource constructor.</jc>
		 * 		<jk>public</jk> MyResource(Builder <jv>builder</jv>) <jk>throws</jk> Exception {
		 *
		 * 			<jc>// Using method on builder.</jc>
		 * 			<jv>builder</jv>.staticFiles(MyStaticFiles.<jk>class</jk>);
		 *
		 * 			<jc>// Use a pre-instantiated object instead.</jc>
		 * 			<jv>builder</jv>.staticFiles(<jk>new</jk> MyStaticFiles());
		 * 		}
		 *
		 * 		<jc>// Option #4 - Registered via builder passed in through init method.</jc>
		 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
		 * 		<jk>public void</jk> init(Builder <jv>builder</jv>) <jk>throws</jk> Exception {
		 * 			<jv>builder</jv>.staticFiles(MyStaticFiles.<jk>class</jk>);
		 * 		}
		 *
		 * 		<jc>// Create a REST method that uses the static files finder.</jc>
		 * 		<ja>@RestGet<ja>(<js>"/htdocs/*"</js>)
		 * 		<jk>public</jk> HttpResource htdocs(RestRequest <jv>req</jv>, <ja>@Path</ja>("/*") String <jv>path</jv>, Locale <jv>locale</jv>) <jk>throws</jk> NotFound {
		 * 			<jk>return</jk> <jv>req</jv>.getStaticFiles().resolve(<jv>path</jv>, <jv>locale</jv>).orElseThrow(NotFound::<jk>new</jk>);
		 * 		}
		 * 	}
		 * </p>
		 *
		 * <p>
		 * Instantiates based on the following logic:
		 * <ul>
		 * 	<li>Returns the resource class itself is an instance of FileFinder.
		 * 	<li>Looks for static files set via any of the following:
		 * 		<ul>
		 * 			<li>{@link RestContext.Builder#staticFiles()}
		 * 			<li>{@link Rest#staticFiles()}.
		 * 		</ul>
		 * 	<li>Looks for a static or non-static <c>createStaticFiles()</> method that returns {@link StaticFiles} on the
		 * 		resource class with any of the following arguments:
		 * 		<ul>
		 * 			<li>{@link RestContext}
		 * 			<li>{@link BeanStore}
		 * 			<li>{@link BasicFileFinder}
		 * 			<li>Any {@doc jrs.Injection injected beans}.
		 * 		</ul>
		 * 	<li>Resolves it via the bean store registered in this context.
		 * 	<li>Looks for value in default static files setting.
		 * 	<li>Instantiates a {@link BasicStaticFiles}.
		 * </ul>
		 *
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @return A new static files sub-builder.
		 */
		protected StaticFiles.Builder createStaticFiles(BeanStore beanStore, Supplier<?> resource) {

			// Default value.
			Value<StaticFiles.Builder> v = Value.of(
				StaticFiles
					.create()
					.beanStore(beanStore)
					.dir("static")
					.dir("htdocs")
					.cp(resourceClass, "htdocs", true)
					.cp(resourceClass, "/htdocs", true)
					.caching(1_000_000)
					.exclude("(?i).*\\.(class|properties)")
					.headers(cacheControl("max-age=86400, public"))
			);

			// Replace with builder from bean store.
			beanStore
				.getBean(StaticFiles.Builder.class)
				.map(x -> x.copy())
				.ifPresent(x -> v.set(x));

			// Replace with bean from bean store.
			beanStore
				.getBean(StaticFiles.class)
				.ifPresent(x -> v.get().impl(x));

			// Replace with this bean.
			resourceAs(StaticFiles.class)
				.ifPresent(x -> v.get().impl(x));

			// Specify the bean type if its set as a default.
			defaultClasses()
				.get(StaticFiles.class)
				.ifPresent(x -> v.get().type(x));

			// Replace with builder from:  public [static] StaticFiles.Builder createStaticFiles(<args>)
			beanStore
				.beanCreateMethodFinder(StaticFiles.Builder.class)
				.addBean(StaticFiles.Builder.class, v.get())
				.find("createStaticFiles")
				.run(x -> v.set(x));

			// Replace with bean from:  public [static] StaticFiles createStaticFiles(<args>)
			beanStore
				.beanCreateMethodFinder(StaticFiles.class)
				.addBean(StaticFiles.Builder.class, v.get())
				.find("createStaticFiles")
				.run(x -> v.get().impl(x));

			return v.get();
		}

		//-----------------------------------------------------------------------------------------------------------------
		// defaultRequestHeaders
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * Returns the default request headers sub-builder.
		 *
		 * @return The default request headers sub-builder.
		 */
		public final HeaderList.Builder defaultRequestHeaders() {
			if (defaultRequestHeaders == null)
				defaultRequestHeaders = createDefaultRequestHeaders(beanStore(), resource());
			return defaultRequestHeaders;
		}

		/**
		 * Applies an operation to the default request headers sub-builder.
		 *
		 * <p>
		 * Typically used to allow you to execute operations without breaking the fluent flow of the context builder.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	RestContext <jv>context</jv> = RestContext
		 * 		.<jsm>create</jsm>(<jv>resourceClass</jv>, <jv>parentContext</jv>, <jv>servletConfig</jv>)
		 * 		.defaultRequestHeaders(<jv>x</jv> -&gt; <jv>x</jv>.remove(<js>"Foo"</js>)))
		 * 		.build();
		 * </p>
		 *
		 * @param operation The operation to apply.
		 * @return This object.
		 */
		public final Builder defaultRequestHeaders(Consumer<HeaderList.Builder> operation) {
			operation.accept(defaultRequestHeaders());
			return this;
		}

		/**
		 * Default request headers.
		 *
		 * <p>
		 * Specifies default values for request headers if they're not passed in through the request.
		 *
		 * <ul class='notes'>
		 * 	<li>
		 * 		Affects values returned by {@link RestRequest#getHeader(String)} when the header is not present on the request.
		 * 	<li>
		 * 		The most useful reason for this annotation is to provide a default <c>Accept</c> header when one is not
		 * 		specified so that a particular default {@link Serializer} is picked.
		 * </ul>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	<jc>// Option #1 - Defined via annotation resolving to a config file setting with default value.</jc>
		 * 	<ja>@Rest</ja>(defaultRequestHeaders={<js>"Accept: application/json"</js>, <js>"My-Header=$C{REST/myHeaderValue}"</js>})
		 * 	<jk>public class</jk> MyResource {
		 *
		 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
		 * 		<jk>public</jk> MyResource(Builder <jv>builder</jv>) <jk>throws</jk> Exception {
		 *
		 * 			<jc>// Using method on builder.</jc>
		 * 			<jv>builder</jv>
		 * 				.defaultRequestHeaders(
		 * 					Accept.<jsm>of</jsm>(<js>"application/json"</js>),
		 * 					BasicHeader.<jsm>of</jsm>(<js>"My-Header"</js>, <js>"foo"</js>)
		 * 				);
		 * 		}
		 *
		 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
		 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
		 * 		<jk>public void</jk> init(Builder <jv>builder</jv>) <jk>throws</jk> Exception {
		 * 			<jv>builder</jv>.defaultRequestHeaders(Accept.<jsm>of</jsm>(<js>"application/json"</js>));
		 * 		}
		 *
		 * 		<jc>// Override at the method level.</jc>
		 * 		<ja>@RestGet</ja>(defaultRequestHeaders={<js>"Accept: text/xml"</js>})
		 * 		<jk>public</jk> Object myMethod() {...}
		 * 	}
		 * </p>
		 *
		 * <ul class='seealso'>
		 * 	<li class='ja'>{@link Rest#defaultRequestHeaders}
		 * 	<li class='ja'>{@link RestOp#defaultRequestHeaders}
		 * 	<li class='ja'>{@link RestGet#defaultRequestHeaders}
		 * 	<li class='ja'>{@link RestPut#defaultRequestHeaders}
		 * 	<li class='ja'>{@link RestPost#defaultRequestHeaders}
		 * 	<li class='ja'>{@link RestDelete#defaultRequestHeaders}
		 * </ul>
		 *
		 * @param values The headers to add.
		 * @return This object.
		 */
		@FluentSetter
		public Builder defaultRequestHeaders(Header...values) {
			defaultRequestHeaders().setDefault(values);
			return this;
		}

		/**
		 * Specifies a default <c>Accept</c> header value if not specified on a request.
		 *
		 * @param value
		 * 	The default value of the <c>Accept</c> header.
		 * 	<br>Ignored if <jk>null</jk> or empty.
		 * @return This object.
		 */
		@FluentSetter
		public Builder defaultAccept(String value) {
			if (isNotEmpty(value))
				defaultRequestHeaders(accept(value));
			return this;
		}

		/**
		 * Specifies a default <c>Content-Type</c> header value if not specified on a request.
		 *
		 * @param value
		 * 	The default value of the <c>Content-Type</c> header.
		 * 	<br>Ignored if <jk>null</jk> or empty.
		 * @return This object.
		 */
		@FluentSetter
		public Builder defaultContentType(String value) {
			if (isNotEmpty(value))
				defaultRequestHeaders(contentType(value));
			return this;
		}

		/**
		 * Instantiates the default request headers sub-builder.
		 *
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @return A new default request headers sub-builder.
		 */
		protected HeaderList.Builder createDefaultRequestHeaders(BeanStore beanStore, Supplier<?> resource) {

			// Default value.
			Value<HeaderList.Builder> v = Value.of(
				HeaderList.create()
			);

			// Replace with builder from bean store.
			beanStore
				.getBean("RestContext.defaultRequestHeaders", HeaderList.Builder.class)
				.map(x -> x.copy())
				.ifPresent(x -> v.set(x));

			// Replace with bean from bean store.
			beanStore
				.getBean("RestContext.defaultRequestHeaders", HeaderList.class)
				.ifPresent(x -> v.get().impl(x));

			// Replace with builder from:  public [static] HeaderList.Builder createDefaultRequestHeaders(<args>)
			beanStore
				.beanCreateMethodFinder(HeaderList.Builder.class)
				.addBean(HeaderList.Builder.class, v.get())
				.find("createDefaultRequestHeaders")
				.run(x -> v.set(x));

			// Replace with bean from:  public [static] HeaderList createDefaultRequestHeaders(<args>)
			beanStore
				.beanCreateMethodFinder(HeaderList.class)
				.addBean(HeaderList.Builder.class, v.get())
				.find("createDefaultRequestHeaders")
				.run(x -> v.get().impl(x));

			return v.get();
		}

		//-----------------------------------------------------------------------------------------------------------------
		// defaultResponseHeaders
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * Returns the default response headers sub-builder.
		 *
		 * @return The default response headers sub-builder.
		 */
		public final HeaderList.Builder defaultResponseHeaders() {
			if (defaultResponseHeaders == null)
				defaultResponseHeaders = createDefaultResponseHeaders(beanStore(), resource());
			return defaultResponseHeaders;
		}

		/**
		 * Applies an operation to the default response headers sub-builder.
		 *
		 * <p>
		 * Typically used to allow you to execute operations without breaking the fluent flow of the context builder.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	RestContext <jv>context</jv> = RestContext
		 * 		.<jsm>create</jsm>(<jv>resourceClass</jv>, <jv>parentContext</jv>, <jv>servletConfig</jv>)
		 * 		.defaultResponseHeaders(<jv>x</jv> -&gt; <jv>x</jv>.remove(<js>"Foo"</js>)))
		 * 		.build();
		 * </p>
		 *
		 * @param operation The operation to apply.
		 * @return This object.
		 */
		public final Builder defaultResponseHeaders(Consumer<HeaderList.Builder> operation) {
			operation.accept(defaultResponseHeaders());
			return this;
		}

		/**
		 * Default response headers.
		 *
		 * <p>
		 * Specifies default values for response headers if they're not set after the Java REST method is called.
		 *
		 * <ul class='notes'>
		 * 	<li>
		 * 		This is equivalent to calling {@link RestResponse#setHeader(String, String)} programmatically in each of
		 * 		the Java methods.
		 * 	<li>
		 * 		The header value will not be set if the header value has already been specified (hence the 'default' in the name).
		 * </ul>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	<jc>// Option #1 - Defined via annotation resolving to a config file setting with default value.</jc>
		 * 	<ja>@Rest</ja>(defaultResponseHeaders={<js>"Content-Type: $C{REST/defaultContentType,text/plain}"</js>,<js>"My-Header: $C{REST/myHeaderValue}"</js>})
		 * 	<jk>public class</jk> MyResource {
		 *
		 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
		 * 		<jk>public</jk> MyResource(Builder <jv>builder</jv>) <jk>throws</jk> Exception {
		 *
		 * 			<jc>// Using method on builder.</jc>
		 * 			<jv>builder</jv>
		 * 				.defaultResponseHeaders(
		 * 					ContentType.<jsm>of</jsm>(<js>"text/plain"</js>),
		 * 					BasicHeader.<jsm>ofPair</jsm>(<js>"My-Header: foo"</js>)
		 * 				);
		 * 		}
		 *
		 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
		 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
		 * 		<jk>public void</jk> init(Builder <jv>builder</jv>) <jk>throws</jk> Exception {
		 * 			<jv>builder</jv>.defaultResponseHeaders(ContentType.<jsm>of</jsm>(<js>"text/plain"</js>));
		 * 		}
		 * 	}
		 * </p>
		 *
		 * <ul class='seealso'>
		 * 	<li class='ja'>{@link Rest#defaultResponseHeaders}
		 * 	<li class='ja'>{@link RestOp#defaultResponseHeaders}
		 * 	<li class='ja'>{@link RestGet#defaultResponseHeaders}
		 * 	<li class='ja'>{@link RestPut#defaultResponseHeaders}
		 * 	<li class='ja'>{@link RestPost#defaultResponseHeaders}
		 * 	<li class='ja'>{@link RestDelete#defaultResponseHeaders}
		 * </ul>
		 *
		 * @param values The headers to add.
		 * @return This object.
		 */
		@FluentSetter
		public Builder defaultResponseHeaders(Header...values) {
			defaultResponseHeaders().setDefault(values);
			return this;
		}

		/**
		 * Instantiates the default response headers sub-builder.
		 *
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @return A new default response headers sub-builder.
		 */
		protected HeaderList.Builder createDefaultResponseHeaders(BeanStore beanStore, Supplier<?> resource) {

			// Default value.
			Value<HeaderList.Builder> v = Value.of(
				HeaderList.create()
			);

			// Replace with builder from bean store.
			beanStore
				.getBean("RestContext.defaultResponseHeaders", HeaderList.Builder.class)
				.map(x -> x.copy())
				.ifPresent(x -> v.set(x));

			// Replace with bean from bean store.
			beanStore
				.getBean("RestContext.defaultResponseHeaders", HeaderList.class)
				.ifPresent(x -> v.get().impl(x));

			// Replace with builder from:  public [static] HeaderList.Builder createDefaultResponseHeaders(<args>)
			beanStore
				.beanCreateMethodFinder(HeaderList.Builder.class)
				.addBean(HeaderList.Builder.class, v.get())
				.find("createDefaultResponseHeaders")
				.run(x -> v.set(x));

			// Replace with bean from:  public [static] HeaderList createDefaultResponseHeaders(<args>)
			beanStore
				.beanCreateMethodFinder(HeaderList.class)
				.addBean(HeaderList.Builder.class, v.get())
				.find("createDefaultResponseHeaders")
				.run(x -> v.get().impl(x));

			return v.get();
		}

		//-----------------------------------------------------------------------------------------------------------------
		// defaultRequestAttributes
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * Returns the default request attributes sub-builder.
		 *
		 * @return The default request attributes sub-builder.
		 */
		public final NamedAttributeList.Builder defaultRequestAttributes() {
			if (defaultRequestAttributes == null)
				defaultRequestAttributes = createDefaultRequestAttributes(beanStore(), resource());
			return defaultRequestAttributes;
		}

		/**
		 * Applies an operation to the default request attributes sub-builder.
		 *
		 * <p>
		 * Typically used to allow you to execute operations without breaking the fluent flow of the context builder.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	RestContext <jv>context</jv> = RestContext
		 * 		.<jsm>create</jsm>(<jv>resourceClass</jv>, <jv>parentContext</jv>, <jv>servletConfig</jv>)
		 * 		.defaultRequestAttributes(<jv>x</jv> -&gt; <jv>x</jv>.add(BasicNamedAttribute.<jsm>of</jsm>(<js>"Foo"</js>, ()-&gt;<jsm>getFoo</jsm>()))
		 * 		.build();
		 * </p>
		 *
		 * @param operation The operation to apply.
		 * @return This object.
		 */
		public final Builder defaultRequestAttributes(Consumer<NamedAttributeList.Builder> operation) {
			operation.accept(defaultRequestAttributes());
			return this;
		}

		/**
		 * Default request attributes.
		 *
		 * <p>
		 * Specifies default values for request attributes if they're not already set on the request.
		 *
		 * Affects values returned by the following methods:
		 * <ul>
		 * 	<li class='jm'>{@link RestRequest#getAttribute(String)}.
		 * 	<li class='jm'>{@link RestRequest#getAttributes()}.
		 * </ul>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	<jc>// Option #1 - Defined via annotation resolving to a config file setting with default value.</jc>
		 * 	<ja>@Rest</ja>(defaultRequestAttributes={<js>"Foo=bar"</js>, <js>"Baz: $C{REST/myAttributeValue}"</js>})
		 * 	<jk>public class</jk> MyResource {
		 *
		 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
		 * 		<jk>public</jk> MyResource(Builder <jv>builder</jv>) <jk>throws</jk> Exception {
		 *
		 * 			<jc>// Using method on builder.</jc>
		 * 			<jv>builder</jv>
		 * 				.defaultRequestAttributes(
		 * 					BasicNamedAttribute.<jsm>of</jsm>(<js>"Foo"</js>, <js>"bar"</js>),
		 * 					BasicNamedAttribute.<jsm>of</jsm>(<js>"Baz"</js>, <jk>true</jk>)
		 * 				);
		 * 		}
		 *
		 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
		 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
		 * 		<jk>public void</jk> init(Builder <jv>builder</jv>) <jk>throws</jk> Exception {
		 * 			<jv>builder</jv>.defaultRequestAttribute(<js>"Foo"</js>, <js>"bar"</js>);
		 * 		}
		 *
		 * 		<jc>// Override at the method level.</jc>
		 * 		<ja>@RestGet</ja>(defaultRequestAttributes={<js>"Foo: bar"</js>})
		 * 		<jk>public</jk> Object myMethod() {...}
		 * 	}
		 * </p>
		 *
		 * <ul class='notes'>
		 * 	<li>Use {@link BasicNamedAttribute#of(String, Supplier)} to provide a dynamically changeable attribute value.
		 * </ul>
		 *
		 * @param values The attributes.
		 * @return This object.
		 */
		@FluentSetter
		public Builder defaultRequestAttributes(NamedAttribute...values) {
			defaultRequestAttributes().add(values);
			return this;
		}

		/**
		 * Instantiates the default request attributes sub-builder.
		 *
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @return A new default request attributes sub-builder.
		 */
		protected NamedAttributeList.Builder createDefaultRequestAttributes(BeanStore beanStore, Supplier<?> resource) {

			// Default value.
			Value<NamedAttributeList.Builder> v = Value.of(
				NamedAttributeList.create()
			);

			// Replace with bean from bean store.
			beanStore
				.getBean("RestContext.defaultRequestAttributes", NamedAttributeList.Builder.class)
				.map(x -> x.copy())
				.ifPresent(x -> v.set(x));

			beanStore
				.getBean("RestContext.defaultRequestAttributes", NamedAttributeList.class)
				.ifPresent(x -> v.get().impl(x));

			// Replace with bean from:  public [static] NamedAttributeList.Builder createDefaultRequestAttributes(<args>)
			beanStore
				.beanCreateMethodFinder(NamedAttributeList.Builder.class)
				.addBean(NamedAttributeList.Builder.class, v.get())
				.find("createDefaultRequestAttributes")
				.run(x -> v.set(x));

			// Replace with bean from:  public [static] NamedAttributeList createDefaultRequestAttributes(<args>)
			beanStore
				.beanCreateMethodFinder(NamedAttributeList.class)
				.addBean(NamedAttributeList.Builder.class, v.get())
				.find("createDefaultRequestAttributes")
				.run(x -> v.get().impl(x));

			return v.get();
		}

		//-----------------------------------------------------------------------------------------------------------------
		// restOpArgs
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * Returns the REST operation args sub-builder.
		 *
		 * @return The REST operation args sub-builder.
		 */
		public final RestOpArgList.Builder restOpArgs() {
			if (restOpArgs == null)
				restOpArgs = createRestOpArgs(beanStore(), resource());
			return restOpArgs;
		}

		/**
		 * Applies an operation to the REST operation args sub-builder.
		 *
		 * <p>
		 * Typically used to allow you to execute operations without breaking the fluent flow of the context builder.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	RestContext <jv>context</jv> = RestContext
		 * 		.<jsm>create</jsm>(<jv>resourceClass</jv>, <jv>parentContext</jv>, <jv>servletConfig</jv>)
		 * 		.restOpArgs(<jv>x</jv> -&gt; <jv>x</jv>.add(MyRestOpArg.<jk>class</jk>))
		 * 		.build();
		 * </p>
		 *
		 * @param operation The operation to apply.
		 * @return This object.
		 */
		public final Builder restOpArgs(Consumer<RestOpArgList.Builder> operation) {
			operation.accept(restOpArgs());
			return this;
		}

		/**
		 * Java method parameter resolvers.
		 *
		 * <p>
		 * By default, the Juneau framework will automatically Java method parameters of various types (e.g.
		 * <c>RestRequest</c>, <c>Accept</c>, <c>Reader</c>).
		 * This annotation allows you to provide your own resolvers for your own class types that you want resolved.
		 *
		 * <p>
		 * For example, if you want to pass in instances of <c>MySpecialObject</c> to your Java method, define
		 * the following resolver:
		 * <p class='bcode w800'>
		 * 	<jc>// Define a parameter resolver for resolving MySpecialObject objects.</jc>
		 * 	<jk>public class</jk> MyRestOpArg <jk>implements</jk> RestOpArg {
		 *
		 *		<jc>// Must implement a static creator method that takes in a ParamInfo that describes the parameter
		 *		// being checked.  If the parameter isn't of type MySpecialObject, then it should return null.</jc>
		 *		<jk>public static</jk> MyRestOpArg <jsm>create</jsm>(ParamInfo <jv>paramInfo</jv>) {
		 *			<jk>if</jk> (<jv>paramInfo</jv>.isType(MySpecialObject.<jk>class</jk>)
		 *				<jk>return new</jk> MyRestParam();
		 *			<jk>return null</jk>;
		 *		}
		 *
		 * 		<jk>public</jk> MyRestOpArg(ParamInfo <jv>paramInfo</jv>) {}
		 *
		 * 		<jc>// The method that creates our object.
		 * 		// In this case, we're taking in a query parameter and converting it to our object.</jc>
		 * 		<ja>@Override</ja>
		 * 		<jk>public</jk> Object resolve(RestCall <jv>call</jv>) <jk>throws</jk> Exception {
		 * 			<jk>return new</jk> MySpecialObject(<jv>call</jv>.getRestRequest().getQuery().get(<js>"myparam"</js>));
		 * 		}
		 * 	}
		 *
		 * 	<jc>// Option #1 - Registered via annotation.</jc>
		 * 	<ja>@Rest</ja>(restOpArgs=MyRestParam.<jk>class</jk>)
		 * 	<jk>public class</jk> MyResource {
		 *
		 * 		<jc>// Option #2 - Registered via builder passed in through resource constructor.</jc>
		 * 		<jk>public</jk> MyResource(Builder <jv>builder</jv>) <jk>throws</jk> Exception {
		 *
		 * 			<jc>// Using method on builder.</jc>
		 * 			<jv>builder</jv>.restOpArgs(MyRestParam.<jk>class</jk>);
		 * 		}
		 *
		 * 		<jc>// Option #3 - Registered via builder passed in through init method.</jc>
		 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
		 * 		<jk>public void</jk> init(Builder <jv>builder</jv>) <jk>throws</jk> Exception {
		 * 			<jv>builder</jv>.restOpArgs(MyRestParam.<jk>class</jk>);
		 * 		}
		 *
		 * 		<jc>// Now pass it into your method.</jc>
		 * 		<ja>@RestPost</ja>(...)
		 * 		<jk>public</jk> Object doMyMethod(MySpecialObject <jv>mySpecialObject</jv>) {
		 * 			<jc>// Do something with it.</jc>
		 * 		}
		 * 	}
		 * </p>
		 *
		 * <ul class='notes'>
		 * 	<li>
		 * 		Inner classes of the REST resource class are allowed.
		 * 	<li>
		 * 		Refer to {@link RestOpArg} for the list of predefined parameter resolvers.
		 * </ul>
		 *
		 * @param values The values to add to this setting.
		 * @return This object.
		 * @throws IllegalArgumentException if any class does not extend from {@link RestOpArg}.
		 */
		@FluentSetter
		public Builder restOpArgs(Class<?>...values) {
			restOpArgs().add(assertClassArrayArgIsType("values", RestOpArg.class, values));
			return this;
		}

		/**
		 * Instantiates the REST operation args sub-builder.
		 *
		 * <p>
		 * Instantiates based on the following logic:
		 * <ul>
		 * 	<li>Looks for REST op args set via any of the following:
		 * 		<ul>
		 * 			<li>{@link RestContext.Builder#restOpArgs(Class...)}/{@link RestContext.Builder#restOpArgs(Class...)}
		 * 			<li>{@link Rest#restOpArgs()}.
		 * 		</ul>
		 * 	<li>Looks for a static or non-static <c>createRestParams()</> method that returns <c>{@link Class}[]</c>.
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
		protected RestOpArgList.Builder createRestOpArgs(BeanStore beanStore, Supplier<?> resource) {

			// Default value.
			Value<RestOpArgList.Builder> v = Value.of(
				RestOpArgList
					.of(
						AttributeArg.class,
						BodyArg.class,
						FormDataArg.class,
						HasFormDataArg.class,
						HasQueryArg.class,
						HeaderArg.class,
						HttpServletRequestArgs.class,
						HttpServletResponseArgs.class,
						HttpSessionArgs.class,
						InputStreamParserArg.class,
						MethodArg.class,
						ParserArg.class,
						PathArg.class,
						QueryArg.class,
						ReaderParserArg.class,
						RequestBeanArg.class,
						ResponseBeanArg.class,
						ResponseHeaderArg.class,
						ResponseCodeArg.class,
						RestContextArgs.class,
						RestSessionArgs.class,
						RestOpContextArgs.class,
						RestOpSessionArgs.class,
						RestRequestArgs.class,
						RestResponseArgs.class,
						DefaultArg.class
					)
			);

			// Replace with builder from bean store.
			beanStore
				.getBean("RestContext.restOpArgs", RestOpArgList.Builder.class)
				.map(x -> x.copy())
				.ifPresent(x -> v.set(x));

			// Replace with bean from bean store.
			beanStore
				.getBean("RestContext.restOpArgs", RestOpArgList.class)
				.ifPresent(x -> v.get().impl(x));

			// Replace with builder from:  public [static] RestOpArgList.Builder createRestOpArgs(<args>)
			beanStore
				.beanCreateMethodFinder(RestOpArgList.Builder.class)
				.addBean(RestOpArgList.Builder.class, v.get())
				.find("createRestOpArgs")
				.run(x -> v.set(x));

			// Replace with bean from:  public [static] RestOpArgList createRestOpArgs(<args>)
			beanStore
				.beanCreateMethodFinder(RestOpArgList.class)
				.addBean(RestOpArgList.Builder.class, v.get())
				.find("createRestOpArgs")
				.run(x -> v.get().impl(x));

			return v.get();
		}

		//-----------------------------------------------------------------------------------------------------------------
		// debugEnablement
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * Returns the debug enablement sub-builder.
		 *
		 * <p>
		 * Enables the following:
		 * <ul class='spaced-list'>
		 * 	<li>
		 * 		HTTP request/response bodies are cached in memory for logging purposes.
		 * 	<li>
		 * 		Request/response messages are automatically logged always or per request.
		 * </ul>
		 *
		 * @return The debug enablement sub-builder.
		 */
		public final DebugEnablement.Builder debugEnablement() {
			if (debugEnablement == null)
				debugEnablement = createDebugEnablement(beanStore(), resource());
			return debugEnablement;
		}

		/**
		 * Applies an operation to the debug enablement sub-builder.
		 *
		 * <p>
		 * Typically used to allow you to execute operations without breaking the fluent flow of the context builder.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	RestContext <jv>context</jv> = RestContext
		 * 		.<jsm>create</jsm>(<jv>resourceClass</jv>, <jv>parentContext</jv>, <jv>servletConfig</jv>)
		 * 		.debugEnablement(<jv>x</jv> -&gt; <jv>x</jv>.defaultEnable(<jsf>ALWAYS</jsf>))
		 * 		.build();
		 * </p>
		 *
		 * @param operation The operation to apply.
		 * @return This object.
		 */
		public final Builder debugEnablement(Consumer<DebugEnablement.Builder> operation) {
			operation.accept(debugEnablement());
			return this;
		}

		/**
		 * Sets the debug default value.
		 *
		 * <p>
		 * The default debug value is the enablement value if not otherwise overridden at the class or method level.
		 *
		 * @param value The debug default value.
		 * @return This object.
		 */
		@FluentSetter
		public Builder debugDefault(Enablement value) {
			defaultSettings().set("RestContext.debugDefault", value);
			return this;
		}

		/**
		 * Specifies the debug level on this REST resource.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public Builder debug(Enablement value) {
			debugEnablement().enable(value, this.resourceClass);
			return this;
		}

		/**
		 * Debug mode on specified classes/methods.
		 *
		 * Enables the following:
		 * <ul class='spaced-list'>
		 * 	<li>
		 * 		HTTP request/response bodies are cached in memory for logging purposes.
		 * 	<li>
		 * 		Request/response messages are automatically logged.
		 * </ul>
		 *
		 * <ul class='seealso'>
		 * 	<li class='ja'>{@link Rest#debugOn}
		 * </ul>
		 *
		 * @param value The new value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder debugOn(String value) {
			for (Map.Entry<String,String> e : splitMap(ofNullable(value).orElse(""), true).entrySet()) {
				String k = e.getKey(), v = e.getValue();
				if (v.isEmpty())
					v = "ALWAYS";
				if (! k.isEmpty())
					debugEnablement().enable(Enablement.fromString(v), k);
			}
			return this;
		}

		/**
		 * Instantiates the debug enablement sub-builder.
		 *
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @return A new debug enablement sub-builder.
		 */
		protected DebugEnablement.Builder createDebugEnablement(BeanStore beanStore, Supplier<?> resource) {

			// Default value.
			Value<DebugEnablement.Builder> v = Value.of(
				DebugEnablement
					.create()
					.beanStore(beanStore)
			);

			// Default debug enablement if not overridden at class/method level.
			Enablement debugDefault = defaultSettings.get(Enablement.class, "RestContext.debugDefault").orElse(isDebug() ? Enablement.ALWAYS : Enablement.NEVER);
			v.get().defaultEnable(debugDefault);

			// Gather @RestOp(debug) settings.
			for (MethodInfo mi : ClassInfo.ofProxy(resource.get()).getPublicMethods()) {
				mi
					.getAnnotationGroupList(RestOp.class)
					.getValues(String.class, "debug")
					.stream()
					.filter(y->!y.isEmpty())
					.findFirst()
					.ifPresent(x -> v.get().enable(Enablement.fromString(x), mi.getFullName()));
			}

			// Replace with builder from bean store.
			beanStore
				.getBean(DebugEnablement.Builder.class)
				.map(x -> x.copy())
				.ifPresent(x -> v.set(x));

			// Replace with bean from bean store.
			beanStore
				.getBean(DebugEnablement.class)
				.ifPresent(x -> v.get().impl(x));

			// Replace with this bean.
			resourceAs(DebugEnablement.class)
				.ifPresent(x -> v.get().impl(x));

			// Specify the implementation class if its set as a default.
			defaultClasses()
				.get(DebugEnablement.class)
				.ifPresent(x -> v.get().type(x));

			// Replace with builder from:  public [static] DebugEnablement.Builder createDebugEnablement(<args>)
			beanStore
				.beanCreateMethodFinder(DebugEnablement.Builder.class)
				.addBean(DebugEnablement.Builder.class, v.get())
				.find("createDebugEnablement")
				.run(x -> v.set(x));

			// Replace with bean from:  public [static] DebugEnablement createDebugEnablement(<args>)
			beanStore
				.beanCreateMethodFinder(DebugEnablement.class)
				.addBean(DebugEnablement.Builder.class, v.get())
				.find("createDebugEnablement")
				.run(x -> v.get().impl(x));

			return v.get();
		}

		//-----------------------------------------------------------------------------------------------------------------
		// HookEvent.START_CALL methods
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * Returns the start call method list.
		 *
		 * @return The start call method list.
		 */
		public final MethodList startCallMethods() {
			if (startCallMethods == null)
				startCallMethods = createStartCallMethods(beanStore(), resource());
			return startCallMethods;
		}

		/**
		 * Applies an operation to the start call method list.
		 *
		 * <p>
		 * Typically used to allow you to execute operations without breaking the fluent flow of the context builder.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	RestContext <jv>context</jv> = RestContext
		 * 		.<jsm>create</jsm>(<jv>resourceClass</jv>, <jv>parentContext</jv>, <jv>servletConfig</jv>)
		 * 		.startCallMethods(<jv>x</jv> -&gt; <jv>x</jv>.add(<jv>extraMethod</jv>))
		 * 		.build();
		 * </p>
		 *
		 * @param operation The operation to apply.
		 * @return This object.
		 */
		public final Builder startCallMethods(Consumer<MethodList> operation) {
			operation.accept(startCallMethods());
			return this;
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
		protected MethodList createStartCallMethods(BeanStore beanStore, Supplier<?> resource) {

			// Default value.
			Value<MethodList> v = Value.of(
				getHookMethods(resource, HookEvent.START_CALL)
			);

			// Replace with bean from:  public [static] MethodList createStartCallMethods(<args>)
			beanStore
				.beanCreateMethodFinder(MethodList.class)
				.addBean(MethodList.class, v.get())
				.find("createStartCallMethods")
				.run(x -> v.set(x));

			return v.get();
		}

		//-----------------------------------------------------------------------------------------------------------------
		// HookEvent.END_CALL methods
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * Returns the end call method list.
		 *
		 * @return The end call method list.
		 */
		public final MethodList endCallMethods() {
			if (endCallMethods == null)
				endCallMethods = createEndCallMethods(beanStore(), resource());
			return endCallMethods;
		}

		/**
		 * Applies an operation to the end call method list.
		 *
		 * <p>
		 * Typically used to allow you to execute operations without breaking the fluent flow of the context builder.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	RestContext <jv>context</jv> = RestContext
		 * 		.<jsm>create</jsm>(<jv>resourceClass</jv>, <jv>parentContext</jv>, <jv>servletConfig</jv>)
		 * 		.endCallMethods(<jv>x</jv> -&gt; <jv>x</jv>.add(<jv>extraMethod</jv>))
		 * 		.build();
		 * </p>
		 *
		 * @param operation The operation to apply.
		 * @return This object.
		 */
		public final Builder endCallMethods(Consumer<MethodList> operation) {
			operation.accept(endCallMethods());
			return this;
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
		protected MethodList createEndCallMethods(BeanStore beanStore, Supplier<?> resource) {

			// Default value.
			Value<MethodList> v = Value.of(
				getHookMethods(resource, HookEvent.END_CALL)
			);

			// Replace with bean from:  public [static] MethodList createEndCallMethods(<args>)
			beanStore
				.beanCreateMethodFinder(MethodList.class)
				.addBean(MethodList.class, v.get())
				.find("createEndCallMethods")
				.run(x -> v.set(x));

			return v.get();
		}

		//-----------------------------------------------------------------------------------------------------------------
		// HookEvent.POST_INIT methods
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * Returns the post-init method list.
		 *
		 * @return The post-init method list.
		 */
		public final MethodList postInitMethods() {
			if (postInitMethods == null)
				postInitMethods = createPostInitMethods(beanStore(), resource());
			return postInitMethods;
		}

		/**
		 * Applies an operation to the post-init method list.
		 *
		 * <p>
		 * Typically used to allow you to execute operations without breaking the fluent flow of the context builder.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	RestContext <jv>context</jv> = RestContext
		 * 		.<jsm>create</jsm>(<jv>resourceClass</jv>, <jv>parentContext</jv>, <jv>servletConfig</jv>)
		 * 		.postInitMethods(<jv>x</jv> -&gt; <jv>x</jv>.add(<jv>extraMethod</jv>))
		 * 		.build();
		 * </p>
		 *
		 * @param operation The operation to apply.
		 * @return This object.
		 */
		public final Builder postInitMethods(Consumer<MethodList> operation) {
			operation.accept(postInitMethods());
			return this;
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
		protected MethodList createPostInitMethods(BeanStore beanStore, Supplier<?> resource) {

			// Default value.
			Value<MethodList> v = Value.of(
				getHookMethods(resource, HookEvent.POST_INIT)
			);

			// Replace with bean from:  public [static] MethodList createPostInitMethods(<args>)
			beanStore
				.beanCreateMethodFinder(MethodList.class)
				.addBean(MethodList.class, v.get())
				.find("createPostInitMethods")
				.run(x -> v.set(x));

			return v.get();
		}

		//-----------------------------------------------------------------------------------------------------------------
		// HookEvent.POST_INIT_CHILD_FIRST methods
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * Returns the post-init-child-first method list.
		 *
		 * @return The post-init-child-first method list.
		 */
		public final MethodList postInitChildFirstMethods() {
			if (postInitChildFirstMethods == null)
				postInitChildFirstMethods = createPostInitChildFirstMethods(beanStore(), resource());
			return postInitChildFirstMethods;
		}

		/**
		 * Applies an operation to the post-init-child-first method list.
		 *
		 * <p>
		 * Typically used to allow you to execute operations without breaking the fluent flow of the context builder.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	RestContext <jv>context</jv> = RestContext
		 * 		.<jsm>create</jsm>(<jv>resourceClass</jv>, <jv>parentContext</jv>, <jv>servletConfig</jv>)
		 * 		.postInitChildFirstMethods(<jv>x</jv> -&gt; <jv>x</jv>.add(<jv>extraMethod</jv>))
		 * 		.build();
		 * </p>
		 *
		 * @param operation The operation to apply.
		 * @return This object.
		 */
		public final Builder postInitChildFirstMethods(Consumer<MethodList> operation) {
			operation.accept(postInitChildFirstMethods());
			return this;
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
		protected MethodList createPostInitChildFirstMethods(BeanStore beanStore, Supplier<?> resource) {

			// Default value.
			Value<MethodList> v = Value.of(
				getHookMethods(resource, HookEvent.POST_INIT_CHILD_FIRST)
			);

			// Replace with bean from:  public [static] MethodList createPostInitChildFirstMethods(<args>)
			beanStore
				.beanCreateMethodFinder(MethodList.class)
				.addBean(MethodList.class, v.get())
				.find("createPostInitChildFirstMethods")
				.run(x -> v.set(x));

			return v.get();
		}

		//-----------------------------------------------------------------------------------------------------------------
		// HookEvent.DESTROY methods
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * Returns the destroy method list.
		 *
		 * @return The destroy method list.
		 */
		public final MethodList destroyMethods() {
			if (destroyMethods == null)
				destroyMethods = createDestroyMethods(beanStore(), resource());
			return destroyMethods;
		}

		/**
		 * Applies an operation to the destroy method list.
		 *
		 * <p>
		 * Typically used to allow you to execute operations without breaking the fluent flow of the context builder.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	RestContext <jv>context</jv> = RestContext
		 * 		.<jsm>create</jsm>(<jv>resourceClass</jv>, <jv>parentContext</jv>, <jv>servletConfig</jv>)
		 * 		.destroyMethods(<jv>x</jv> -&gt; <jv>x</jv>.add(<jv>extraMethod</jv>))
		 * 		.build();
		 * </p>
		 *
		 * @param operation The operation to apply.
		 * @return This object.
		 */
		public final Builder destroyMethods(Consumer<MethodList> operation) {
			operation.accept(destroyMethods());
			return this;
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
		protected MethodList createDestroyMethods(BeanStore beanStore, Supplier<?> resource) {

			// Default value.
			Value<MethodList> v = Value.of(
				getHookMethods(resource, HookEvent.DESTROY)
			);

			// Replace with bean from:  public [static] MethodList createDestroyMethods(<args>)
			beanStore
				.beanCreateMethodFinder(MethodList.class)
				.addBean(MethodList.class, v.get())
				.find("createDestroyMethods")
				.run(x -> v.set(x));

			return v.get();
		}

		//-----------------------------------------------------------------------------------------------------------------
		// HookEvent.PRE_CALL methods
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * Returns the pre-call method list.
		 *
		 * <p>
		 * The list of methods that gets called immediately before the <ja>@RestOp</ja> annotated method gets called.
		 *
		 * @return The pre-call method list.
		 */
		public final MethodList preCallMethods() {
			if (preCallMethods == null)
				preCallMethods = createPreCallMethods(beanStore(), resource());
			return preCallMethods;
		}

		/**
		 * Applies an operation to the pre-call method list.
		 *
		 * <p>
		 * Typically used to allow you to execute operations without breaking the fluent flow of the context builder.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	RestContext <jv>context</jv> = RestContext
		 * 		.<jsm>create</jsm>(<jv>resourceClass</jv>, <jv>parentContext</jv>, <jv>servletConfig</jv>)
		 * 		.preCallMethods(<jv>x</jv> -&gt; <jv>x</jv>.add(<jv>extraMethod</jv>))
		 * 		.build();
		 * </p>
		 *
		 * @param operation The operation to apply.
		 * @return This object.
		 */
		public final Builder preCallMethods(Consumer<MethodList> operation) {
			operation.accept(preCallMethods());
			return this;
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
		protected MethodList createPreCallMethods(BeanStore beanStore, Supplier<?> resource) {

			// Default value.
			Value<MethodList> v = Value.of(
				getHookMethods(resource, HookEvent.PRE_CALL)
			);

			// Replace with bean from:  public [static] MethodList createPreCallMethods(<args>)
			beanStore
				.beanCreateMethodFinder(MethodList.class)
				.addBean(MethodList.class, v.get())
				.find("createPreCallMethods")
				.run(x -> v.set(x));

			return v.get();
		}

		//-----------------------------------------------------------------------------------------------------------------
		// HookEvent.POST_CALL methods
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * Returns the post-call method list.
		 *
		 * <p>
		 * The list of methods that gets called immediately after the <ja>@RestOp</ja> annotated method gets called..
		 *
		 * @return The list of methods that gets called immediately after the <ja>@RestOp</ja> annotated method gets called..
		 */
		public final MethodList postCallMethods() {
			if (postCallMethods == null)
				postCallMethods = createPostCallMethods(beanStore(), resource());
			return postCallMethods;
		}

		/**
		 * Applies an operation to the post-call method list.
		 *
		 * <p>
		 * Typically used to allow you to execute operations without breaking the fluent flow of the context builder.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	RestContext <jv>context</jv> = RestContext
		 * 		.<jsm>create</jsm>(<jv>resourceClass</jv>, <jv>parentContext</jv>, <jv>servletConfig</jv>)
		 * 		.postCallMethods(<jv>x</jv> -&gt; <jv>x</jv>.add(<jv>extraMethod</jv>))
		 * 		.build();
		 * </p>
		 *
		 * @param operation The operation to apply.
		 * @return This object.
		 */
		public final Builder postCallMethods(Consumer<MethodList> operation) {
			operation.accept(postCallMethods());
			return this;
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
		protected MethodList createPostCallMethods(BeanStore beanStore, Supplier<?> resource) {

			// Default value.
			Value<MethodList> v = Value.of(
				getHookMethods(resource, HookEvent.POST_CALL)
			);

			// Replace with bean from:  public [static] MethodList createPostCallMethods(<args>)
			beanStore
				.beanCreateMethodFinder(MethodList.class)
				.addBean(MethodList.class, v.get())
				.find("createPostCallMethods")
				.run(x -> v.set(x));

			return v.get();
		}

		//-----------------------------------------------------------------------------------------------------------------
		// restOperations
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * Returns the REST operations list.
		 *
		 * @param restContext The rest context.
		 * @return The REST operations list.
		 * @throws RestServletException If a problem occurred instantiating one of the child rest contexts.
		 */
		public final RestOperations.Builder restOperations(RestContext restContext) throws RestServletException {
			if (restOperations == null)
				restOperations = createRestOperations(beanStore(), resource(), restContext);
			return restOperations;
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
		 * @throws RestServletException If a problem occurred instantiating one of the child rest contexts.
		 */
		protected RestOperations.Builder createRestOperations(BeanStore beanStore, Supplier<?> resource, RestContext restContext) throws RestServletException {

			// Default value.
			Value<RestOperations.Builder> v = Value.of(
				RestOperations.create()
			);

			ClassInfo rci = ClassInfo.of(resource.get());

			for (MethodInfo mi : rci.getPublicMethods()) {
				AnnotationList al = mi.getAnnotationGroupList(RestOp.class);

				// Also include methods on @Rest-annotated interfaces.
				if (al.size() == 0) {
					for (Method mi2 : mi.getMatching()) {
						Class<?> ci2 = mi2.getDeclaringClass();
						if (ci2.isInterface() && ci2.getAnnotation(Rest.class) != null) {
							al.add(AnnotationInfo.of(MethodInfo.of(mi2), RestOpAnnotation.DEFAULT));
						}
					}
				}
				if (al.size() > 0) {
					try {
						if (mi.isNotPublic())
							throw new RestServletException("@RestOp method {0}.{1} must be defined as public.", rci.inner().getName(), mi.getSimpleName());

						RestOpContext roc = RestOpContext
							.create(mi.inner(), restContext)
							.beanStore(beanStore)
							.type(opContextClass)
							.build();

						String httpMethod = roc.getHttpMethod();

						// RRPC is a special case where a method returns an interface that we
						// can perform REST calls against.
						// We override the CallMethod.invoke() method to insert our logic.
						if ("RRPC".equals(httpMethod)) {

							RestOpContext roc2 = RestOpContext
								.create(mi.inner(), restContext)
								.dotAll()
								.beanStore(restContext.getRootBeanStore())
								.type(RrpcRestOpContext.class)
								.build();
							v.get()
								.add("GET", roc2)
								.add("POST", roc2);

						} else {
							v.get().add(roc);
						}
					} catch (Throwable e) {
						throw new RestServletException(e, "Problem occurred trying to initialize methods on class {0}", rci.inner().getName());
					}
				}
			}

			// Replace with builder from:  public [static] RestOperations.Builder createRestOperations(<args>)
			beanStore
				.beanCreateMethodFinder(RestOperations.Builder.class)
				.addBean(RestOperations.Builder.class, v.get())
				.find("createRestOperations")
				.run(x -> v.set(x));

			// Replace with bean from:  public [static] RestOperations createRestOperations(<args>)
			beanStore
				.beanCreateMethodFinder(RestOperations.class)
				.addBean(RestOperations.Builder.class, v.get())
				.find("createRestOperations")
				.run(x -> v.get().impl(x));

			return v.get();
		}

		//-----------------------------------------------------------------------------------------------------------------
		// restChildren
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * Returns the REST children list.
		 *
		 * @param restContext The rest context.
		 * @return The REST children list.
		 * @throws Exception If a problem occurred instantiating one of the child rest contexts.
		 */
		public final RestChildren.Builder restChildren(RestContext restContext) throws Exception {
			if (restChildren == null)
				restChildren = createRestChildren(beanStore(), resource(), restContext);
			return restChildren;
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
		protected RestChildren.Builder createRestChildren(BeanStore beanStore, Supplier<?> resource, RestContext restContext) throws Exception {

			// Default value.
			Value<RestChildren.Builder> v = Value.of(
				RestChildren
					.create()
					.beanStore(beanStore)
					.type(childrenClass)
				);

			// Initialize our child resources.
			for (Object o : children) {
				String path = null;
				Supplier<?> so;

				if (o instanceof RestChild) {
					RestChild rc = (RestChild)o;
					path = rc.path;
					Object o2 = rc.resource;
					so = ()->o2;
				}

				Builder cb = null;

				if (o instanceof Class) {
					Class<?> oc = (Class<?>)o;
					// Don't allow specifying yourself as a child.  Causes an infinite loop.
					if (oc == resourceClass)
						continue;
					cb = RestContext.create(oc, restContext, inner);
					if (beanStore.getBean(oc).isPresent()) {
						so = ()->beanStore.getBean(oc).get();  // If we resolved via injection, always get it this way.
					} else {
						Object o2 = beanStore.creator(oc).builder(cb).outer(resource).run();
						so = ()->o2;
					}
				} else {
					cb = RestContext.create(o.getClass(), restContext, inner);
					so = ()->o;
				}

				if (path != null)
					cb.path(path);

				RestContext cc = cb.init(so).build();

				MethodInfo mi = ClassInfo.of(o).getMethod("setContext", RestContext.class);
				if (mi != null)
					mi.accessible().invoke(o, cc);

				v.get().add(cc);
			}

			// Replace with builder from:  public [static] RestChildren.Builder createRestChildren(<args>)
			beanStore
				.beanCreateMethodFinder(RestChildren.Builder.class)
				.addBean(RestChildren.Builder.class, v.get())
				.find("createRestChildren")
				.run(x -> v.set(x));

			// Replace with bean from:  public [static] RestChildren createRestChildren(<args>)
			beanStore
				.beanCreateMethodFinder(RestChildren.class)
				.addBean(RestChildren.Builder.class, v.get())
				.find("createRestChildren")
				.run(x -> v.get().impl(x));

			return v.get();
		}

		//-----------------------------------------------------------------------------------------------------------------
		// swaggerProvider
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * Returns the swagger provider sub-builder.
		 *
		 * @return The swagger provider sub-builder.
		 */
		public final SwaggerProvider.Builder swaggerProvider() {
			if (swaggerProvider == null)
				swaggerProvider = createSwaggerProvider(beanStore(), resource());
			return swaggerProvider;
		}

		/**
		 * Applies an operation to the swagger provider sub-builder.
		 *
		 * <p>
		 * Typically used to allow you to execute operations without breaking the fluent flow of the context builder.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	RestContext <jv>context</jv> = RestContext
		 * 		.<jsm>create</jsm>(<jv>resourceClass</jv>, <jv>parentContext</jv>, <jv>servletConfig</jv>)
		 * 		.swaggerProvider(<jv>x</jv> -&gt; <jv>x</jv>.fileFinder(<jv>aDifferentFileFinder</jv>))
		 * 		.build();
		 * </p>
		 *
		 * @param operation The operation to apply.
		 * @return This object.
		 */
		public final Builder swaggerProvider(Consumer<SwaggerProvider.Builder> operation) {
			operation.accept(swaggerProvider());
			return this;
		}

		/**
		 * Instantiates the swagger provider sub-builder.
		 *
		 * <p>
		 * Instantiates based on the following logic:
		 * <ul>
		 * 	<li>Returns the resource class itself is an instance of {@link SwaggerProvider}.
		 * 	<li>Looks for swagger provider set via any of the following:
		 * 		<ul>
		 * 			<li>{@link RestContext.Builder#swaggerProvider(Class)}/{@link RestContext.Builder#swaggerProvider(SwaggerProvider)}
		 * 			<li>{@link Rest#swaggerProvider()}.
		 * 		</ul>
		 * 	<li>Looks for a static or non-static <c>createSwaggerProvider()</> method that returns {@link SwaggerProvider} on the
		 * 		resource class with any of the following arguments:
		 * 		<ul>
		 * 			<li>{@link RestContext}
		 * 			<li>{@link BeanStore}
		 * 			<li>Any {@doc jrs.Injection injected beans}.
		 * 		</ul>
		 * 	<li>Resolves it via the bean store registered in this context.
		 * 	<li>Instantiates a default {@link BasicSwaggerProvider}.
		 * </ul>
		 *
		 * <ul class='seealso'>
		 * 	<li class='jm'>{@link RestContext.Builder#swaggerProvider(Class)}
		 * 	<li class='jm'>{@link RestContext.Builder#swaggerProvider(SwaggerProvider)}
		 * </ul>
		 *
		 * @param beanStore
		 * 	The factory used for creating beans and retrieving injected beans.
		 * @param resource
		 * 	The REST servlet/bean instance that this context is defined against.
		 * @return A new swagger provider sub-builder.
		 */
		protected SwaggerProvider.Builder createSwaggerProvider(BeanStore beanStore, Supplier<?> resource) {

			// Default value.
			Value<SwaggerProvider.Builder> v = Value.of(
				SwaggerProvider
					.create()
					.beanStore(beanStore)
					.varResolver(()->beanStore.getBean(VarResolver.class).orElseThrow(()->runtimeException("VarResolver bean not found.")))
					.fileFinder(()->beanStore.getBean(FileFinder.class).orElseThrow(()->runtimeException("FileFinder bean not found.")))
					.messages(()->beanStore.getBean(Messages.class).orElseThrow(()->runtimeException("Messages bean not found.")))
					.jsonSchemaGenerator(()->beanStore.getBean(JsonSchemaGenerator.class).orElseThrow(()->runtimeException("JsonSchemaGenerator bean not found.")))
			);

			// Replace with builder from bean store.
			beanStore
				.getBean(SwaggerProvider.Builder.class)
				.map(x -> x.copy())
				.ifPresent(x -> v.set(x));

			// Replace with builder from bean store.
			beanStore
				.getBean(SwaggerProvider.class)
				.ifPresent(x -> v.get().impl(x));

			// Replace with this bean.
			resourceAs(SwaggerProvider.class)
				.ifPresent(x -> v.get().impl(x));

			// Replace with builder from:  public [static] SwaggerProvider.Builder createSwaggerProvider(<args>)
			beanStore
				.beanCreateMethodFinder(SwaggerProvider.Builder.class)
				.addBean(SwaggerProvider.Builder.class, v.get())
				.find("createSwaggerProvider")
				.run(x -> v.set(x));

			// Replace with bean from:  public [static] SwaggerProvider createSwaggerProvider(<args>)
			beanStore
				.beanCreateMethodFinder(SwaggerProvider.class)
				.addBean(SwaggerProvider.Builder.class, v.get())
				.find("createSwaggerProvider")
				.run(x -> v.get().impl(x));

			return v.get();
		}

		//-----------------------------------------------------------------------------------------------------------------
		// Miscellaneous settings
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * Allowed header URL parameters.
		 *
		 * <p>
		 * When specified, allows headers such as <js>"Accept"</js> and <js>"Content-Type"</js> to be passed in as URL query
		 * parameters.
		 * <br>
		 * For example:
		 * <p class='bcode w800'>
		 *  ?Accept=text/json&amp;Content-Type=text/json
		 * </p>
		 *
		 * <ul class='notes'>
		 * 	<li>
		 * 		Useful for debugging REST interface using only a browser so that you can quickly simulate header values
		 * 		in the URL bar.
		 * 	<li>
		 * 		Header names are case-insensitive.
		 * 	<li>
		 * 		Use <js>"*"</js> to allow any headers to be specified as URL parameters.
		 * 	<li>
		 * 		Use <js>"NONE"</js> (case insensitive) to suppress inheriting a value from a parent class.
		 * </ul>

		 * <ul class='seealso'>
		 * 	<li class='ja'>{@link Rest#allowedHeaderParams}
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>The default is the first value found:
		 * 	<ul>
		 * 		<li>System property <js>"RestContext.allowedHeaderParams"
		 * 		<li>Environment variable <js>"RESTCONTEXT_ALLOWEDHEADERPARAMS"
		 * 		<li><js>"Accept,Content-Type"</js>
		 * 	</ul>
		 * @return This object.
		 */
		@FluentSetter
		public Builder allowedHeaderParams(String value) {
			allowedHeaderParams = value;
			return this;
		}

		/**
		 * Allowed method headers.
		 *
		 * <p>
		 * A comma-delimited list of HTTP method names that are allowed to be passed as values in an <c>X-Method</c> HTTP header
		 * to override the real HTTP method name.
		 *
		 * <p>
		 * Allows you to override the actual HTTP method with a simulated method.
		 * <br>For example, if an HTTP Client API doesn't support <c>PATCH</c> but does support <c>POST</c> (because
		 * <c>PATCH</c> is not part of the original HTTP spec), you can add a <c>X-Method: PATCH</c> header on a normal
		 * <c>HTTP POST /foo</c> request call which will make the HTTP call look like a <c>PATCH</c> request in any of the REST APIs.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	<jc>// Option #1 - Defined via annotation resolving to a config file setting with default value.</jc>
		 * 	<ja>@Rest</ja>(allowedMethodHeaders=<js>"PATCH"</js>)
		 * 	<jk>public class</jk> MyResource {
		 *
		 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
		 * 		<jk>public</jk> MyResource(Builder <jv>builder</jv>) <jk>throws</jk> Exception {
		 *
		 * 			<jc>// Using method on builder.</jc>
		 * 			<jv>builder</jv>.allowedMethodHeaders(<js>"PATCH"</js>);
		 * 		}
		 *
		 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
		 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
		 * 		<jk>public void</jk> init(Builder <jv>builder</jv>) <jk>throws</jk> Exception {
		 * 			<jv>builder</jv>.allowedMethodHeaders(<js>"PATCH"</js>);
		 * 		}
		 * 	}
		 * </p>
		 *
		 * <ul class='notes'>
		 * 	<li>
		 * 		Method names are case-insensitive.
		 * 	<li>
		 * 		Use <js>"*"</js> to represent all methods.
		 * 	<li>
		 * 		Use <js>"NONE"</js> (case insensitive) to suppress inheriting a value from a parent class.
		 * </ul>
		 *
		 * <ul class='seealso'>
		 * 	<li class='ja'>{@link Rest#allowedMethodHeaders}
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>The default is the first value found:
		 * 	<ul>
		 * 		<li>System property <js>"RestContext.allowedMethodHeaders"
		 * 		<li>Environment variable <js>"RESTCONTEXT_ALLOWEDMETHODHEADERS"
		 * 		<li><js>""</js>
		 * 	</ul>
		 * @return This object.
		 */
		@FluentSetter
		public Builder allowedMethodHeaders(String value) {
			allowedMethodHeaders = value;
			return this;
		}

		/**
		 * Allowed method parameters.
		 *
		 * <p>
		 * When specified, the HTTP method can be overridden by passing in a <js>"method"</js> URL parameter on a regular
		 * GET request.
		 * <br>
		 * For example:
		 * <p class='bcode w800'>
		 *  ?method=OPTIONS
		 * </p>
		 *
		 * <p>
		 * 	Useful in cases where you want to simulate a non-GET request in a browser by simply adding a parameter.
		 * 	<br>Also useful if you want to construct hyperlinks to non-GET REST endpoints such as links to <c>OPTIONS</c>
		 * pages.
		 *
		 * <p>
		 * Note that per the {@doc ext.RFC2616.section9 HTTP specification}, special care should
		 * be taken when allowing non-safe (<c>POST</c>, <c>PUT</c>, <c>DELETE</c>) methods to be invoked through GET requests.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	<jc>// Option #1 - Defined via annotation.</jc>
		 * 	<ja>@Rest</ja>(allowedMethodParams=<js>"HEAD,OPTIONS,PUT"</js>)
		 * 	<jk>public class</jk> MyResource {
		 *
		 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
		 * 		<jk>public</jk> MyResource(Builder <jv>builder</jv>) <jk>throws</jk> Exception {
		 *
		 * 			<jc>// Using method on builder.</jc>
		 * 			<jv>builder</jv>.allowedMethodParams(<js>"HEAD,OPTIONS,PUT"</js>);
		 * 		}
		 *
		 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
		 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
		 * 		<jk>public void</jk> init(Builder builder) <jk>throws</jk> Exception {
		 * 			<jv>builder</jv>.allowedMethodParams(<js>"HEAD,OPTIONS,PUT"</js>);
		 * 		}
		 * 	}
		 * </p>
		 *
		 * <ul class='notes'>
		 * 	<li>
		 * 		Format is a comma-delimited list of HTTP method names that can be passed in as a method parameter.
		 * 	<li>
		 * 		<js>'method'</js> parameter name is case-insensitive.
		 * 	<li>
		 * 		Use <js>"*"</js> to represent all methods.
		 * 	<li>
		 * 		Use <js>"NONE"</js> (case insensitive) to suppress inheriting a value from a parent class.
		 * </ul>
		 *
		 * <ul class='seealso'>
		 * 	<li class='ja'>{@link Rest#allowedMethodParams}
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>The default is the first value found:
		 * 	<ul>
		 * 		<li>System property <js>"RestContext.allowedMethodParams"
		 * 		<li>Environment variable <js>"RESTCONTEXT_ALLOWEDMETHODPARAMS"
		 * 		<li><js>"HEAD,OPTIONS"</js>
		 * 	</ul>
		 * @return This object.
		 */
		@FluentSetter
		public Builder allowedMethodParams(String value) {
			allowedMethodParams = value;
			return this;
		}

		/**
		 * Client version header.
		 *
		 * <p>
		 * Specifies the name of the header used to denote the client version on HTTP requests.
		 *
		 * <p>
		 * The client version is used to support backwards compatibility for breaking REST interface changes.
		 * <br>Used in conjunction with {@link RestOp#clientVersion() @RestOp(clientVersion)} annotation.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	<jc>// Option #1 - Defined via annotation resolving to a config file setting with default value.</jc>
		 * 	<ja>@Rest</ja>(clientVersionHeader=<js>"$C{REST/clientVersionHeader,Client-Version}"</js>)
		 * 	<jk>public class</jk> MyResource {
		 *
		 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
		 * 		<jk>public</jk> MyResource(Builder <jv>builder</jv>) <jk>throws</jk> Exception {
		 *
		 * 			<jc>// Using method on builder.</jc>
		 * 			<jv>builder</jv>.clientVersionHeader(<js>"Client-Version"</js>);
		 * 		}
		 *
		 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
		 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
		 * 		<jk>public void</jk> init(Builder <jv>builder</jv>) <jk>throws</jk> Exception {
		 * 			<jv>builder</jv>.clientVersionHeader(<js>"Client-Version"</js>);
		 * 		}
		 * 	}
		 * </p>
		 *
		 * <p class='bcode w800'>
		 * 	<jc>// Call this method if Client-Version is at least 2.0.
		 * 	// Note that this also matches 2.0.1.</jc>
		 * 	<ja>@RestGet/ja>(path=<js>"/foobar"</js>, clientVersion=<js>"2.0"</js>)
		 * 	<jk>public</jk> Object method1() {
		 * 		...
		 * 	}
		 *
		 * 	<jc>// Call this method if Client-Version is at least 1.1, but less than 2.0.</jc>
		 * 	<ja>@RestGet</ja>(path=<js>"/foobar"</js>, clientVersion=<js>"[1.1,2.0)"</js>)
		 * 	<jk>public</jk> Object method2() {
		 * 		...
		 * 	}
		 *
		 * 	<jc>// Call this method if Client-Version is less than 1.1.</jc>
		 * 	<ja>@RestGet</ja>(path=<js>"/foobar"</js>, clientVersion=<js>"[0,1.1)"</js>)
		 * 	<jk>public</jk> Object method3() {
		 * 		...
		 * 	}
		 * </p>
		 *
		 * <ul class='seealso'>
		 * 	<li class='ja'>{@link Rest#clientVersionHeader}
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>The default is the first value found:
		 * 	<ul>
		 * 		<li>System property <js>"RestContext.clientVersionHeader"
		 * 		<li>Environment variable <js>"RESTCONTEXT_CLIENTVERSIONHEADER"
		 * 		<li><js>"Client-Version"</js>
		 * 	</ul>
		 * @return This object.
		 */
		@FluentSetter
		public Builder clientVersionHeader(String value) {
			clientVersionHeader = value;
			return this;
		}

		/**
		 * Default character encoding.
		 *
		 * <p>
		 * The default character encoding for the request and response if not specified on the request.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	<jc>// Option #1 - Defined via annotation resolving to a config file setting with default value.</jc>
		 * 	<ja>@Rest</ja>(defaultCharset=<js>"$C{REST/defaultCharset,US-ASCII}"</js>)
		 * 	<jk>public class</jk> MyResource {
		 *
		 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
		 * 		<jk>public</jk> MyResource(Builder <jv>builder</jv>) <jk>throws</jk> Exception {
		 *
		 * 			<jc>// Using method on builder.</jc>
		 * 			<jv>builder</jv>.defaultCharset(<js>"US-ASCII"</js>);
		 * 		}
		 *
		 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
		 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
		 * 		<jk>public void</jk> init(Builder <jv>builder</jv>) <jk>throws</jk> Exception {
		 * 			<jv>builder</jv>.defaultCharset(<js>"US-ASCII"</js>);
		 * 		}
		 *
		 * 		<jc>// Override at the method level.</jc>
		 * 		<ja>@RestGet</ja>(defaultCharset=<js>"UTF-16"</js>)
		 * 		<jk>public</jk> Object myMethod() {...}
		 * 	}
		 * </p>
		 *
		 * <ul class='seealso'>
		 * 	<li class='ja'>{@link Rest#defaultCharset}
		 * 	<li class='ja'>{@link RestOp#defaultCharset}
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>The default is the first value found:
		 * 	<ul>
		 * 		<li>System property <js>"RestContext.defaultCharset"
		 * 		<li>Environment variable <js>"RESTCONTEXT_defaultCharset"
		 * 		<li><js>"utf-8"</js>
		 * 	</ul>
		 * @return This object.
		 */
		@FluentSetter
		public Builder defaultCharset(Charset value) {
			defaultCharset = value;
			return this;
		}

		/**
		 * Disable body URL parameter.
		 *
		 * <p>
		 * When enabled, the HTTP body content on PUT and POST requests can be passed in as text using the <js>"body"</js>
		 * URL parameter.
		 * <br>
		 * For example:
		 * <p class='bcode w800'>
		 *  ?body=(name='John%20Smith',age=45)
		 * </p>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	<jc>// Option #1 - Defined via annotation resolving to a config file setting with default value.</jc>
		 * 	<ja>@Rest</ja>(disableBodyParam=<js>"$C{REST/disableBodyParam,true}"</js>)
		 * 	<jk>public class</jk> MyResource {
		 *
		 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
		 * 		<jk>public</jk> MyResource(Builder <jv>builder</jv>) <jk>throws</jk> Exception {
		 *
		 * 			<jc>// Using method on builder.</jc>
		 * 			<jv>builder</jv>.disableBodyParam();
		 * 		}
		 *
		 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
		 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
		 * 		<jk>public void</jk> init(Builder <jv>builder</jv>) <jk>throws</jk> Exception {
		 * 			<jv>builder</jv>.disableBodyParam();
		 * 		}
		 * 	}
		 * </p>
		 *
		 * <ul class='notes'>
		 * 	<li>
		 * 		<js>'body'</js> parameter name is case-insensitive.
		 * 	<li>
		 * 		Useful for debugging PUT and POST methods using only a browser.
		 * </ul>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder disableBodyParam() {
			return disableBodyParam(true);
		}

		/**
		 * Disable body URL parameter.
		 *
		 * <p>
		 * Same as {@link #disableBodyParam()} but allows you to set it as a boolean value.
		 *
		 * @param value The new value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder disableBodyParam(boolean value) {
			disableBodyParam = value;
			return this;
		}

		/**
		 * The maximum allowed input size (in bytes) on HTTP requests.
		 *
		 * <p>
		 * Useful for alleviating DoS attacks by throwing an exception when too much input is received instead of resulting
		 * in out-of-memory errors which could affect system stability.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	<jc>// Option #1 - Defined via annotation resolving to a config file setting with default value.</jc>
		 * 	<ja>@Rest</ja>(maxInput=<js>"$C{REST/maxInput,10M}"</js>)
		 * 	<jk>public class</jk> MyResource {
		 *
		 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
		 * 		<jk>public</jk> MyResource(Builder <jv>builder</jv>) <jk>throws</jk> Exception {
		 *
		 * 			<jc>// Using method on builder.</jc>
		 * 			<jv>builder</jv>.maxInput(<js>"10M"</js>);
		 * 		}
		 *
		 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
		 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
		 * 		<jk>public void</jk> init(Builder <jv>builder</jv>) <jk>throws</jk> Exception {
		 * 			<jv>builder</jv>.maxInput(<js>"10M"</js>);
		 * 		}
		 *
		 * 		<jc>// Override at the method level.</jc>
		 * 		<ja>@RestPost</ja>(maxInput=<js>"10M"</js>)
		 * 		<jk>public</jk> Object myMethod() {...}
		 * 	}
		 * </p>
		 *
		 * <ul class='notes'>
		 * 	<li>
		 * 		String value that gets resolved to a <jk>long</jk>.
		 * 	<li>
		 * 		Can be suffixed with any of the following representing kilobytes, megabytes, and gigabytes:
		 * 		<js>'K'</js>, <js>'M'</js>, <js>'G'</js>.
		 * 	<li>
		 * 		A value of <js>"-1"</js> can be used to represent no limit.
		 * </ul>
		 *
		 * <ul class='seealso'>
		 * 	<li class='ja'>{@link Rest#maxInput}
		 * 	<li class='ja'>{@link RestOp#maxInput}
		 * 	<li class='jm'>{@link RestOpContext.Builder#maxInput(String)}
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>The default is the first value found:
		 * 	<ul>
		 * 		<li>System property <js>"RestContext.maxInput"
		 * 		<li>Environment variable <js>"RESTCONTEXT_MAXINPUT"
		 * 		<li><js>"100M"</js>
		 * 	</ul>
		 * 	<br>The default is <js>"100M"</js>.
		 * @return This object.
		 */
		@FluentSetter
		public Builder maxInput(String value) {
			maxInput = StringUtils.parseLongWithSuffix(value);
			return this;
		}

		/**
		 * <i><l>RestContext</l> configuration property:&emsp;</i>  Render response stack traces in responses.
		 *
		 * <p>
		 * Render stack traces in HTTP response bodies when errors occur.
		 *
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>The default is <jk>false</jk>.
		 * @return This object.
		 */
		@FluentSetter
		public Builder renderResponseStackTraces(boolean value) {
			renderResponseStackTraces = value;
			return this;
		}

		/**
		 * <i><l>RestContext</l> configuration property:&emsp;</i>  Render response stack traces in responses.
		 *
		 * <p>
		 * Shortcut for calling <code>renderResponseStackTraces(<jk>true</jk>)</code>.
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder renderResponseStackTraces() {
			renderResponseStackTraces = true;
			return this;
		}

		/**
		 * Resource authority path.
		 *
		 * <p>
		 * Overrides the authority path value for this resource and any child resources.
		 *
		 * <p>
		 * This setting is useful if you want to resolve relative URIs to absolute paths and want to explicitly specify the hostname/port.
		 *
		 * <p>
		 * Affects the following methods:
		 * <ul class='javatree'>
		 * 	<li class='jm'>{@link RestRequest#getAuthorityPath()}
		 * </ul>
		 *
		 * <p>
		 * If you do not specify the authority, it is automatically calculated via the following:
		 *
		 * <p class='bcode w800'>
		 * 	String <jv>scheme</jv> = <jv>request</jv>.getScheme();
		 * 	<jk>int</jk> <jv>port</jv> = <jv>request</jv>.getServerPort();
		 * 	StringBuilder <jv>sb</jv> = <jk>new</jk> StringBuilder(<jv>request</jv>.getScheme()).append(<js>"://"</js>).append(<jv>request</jv>.getServerName());
		 * 	<jk>if</jk> (! (<jv>port</jv> == 80 &amp;&amp; <js>"http"</js>.equals(<jv>scheme</jv>) || port == 443 &amp;&amp; <js>"https"</js>.equals(<jv>scheme</jv>)))
		 * 		<jv>sb</jv>.append(<js>':'</js>).append(<jv>port</jv>);
		 * 	<jv>authorityPath</jv> = <jv>sb</jv>.toString();
		 * </p>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	<jc>// Option #1 - Defined via annotation resolving to a config file setting with default value.</jc>
		 * 	<ja>@Rest</ja>(
		 * 		path=<js>"/servlet"</js>,
		 * 		uriAuthority=<js>"$C{REST/authorityPathOverride,http://localhost:10000}"</js>
		 * 	)
		 * 	<jk>public class</jk> MyResource {
		 *
		 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
		 * 		<jk>public</jk> MyResource(Builder <jv>builder</jv>) <jk>throws</jk> Exception {
		 *
		 * 			<jc>// Using method on builder.</jc>
		 * 			<jv>builder</jv>.uriAuthority(<js>"http://localhost:10000"</js>);
		 * 		}
		 *
		 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
		 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
		 * 		<jk>public void</jk> init(Builder <jv>builder</jv>) <jk>throws</jk> Exception {
		 * 			<jv>builder</jv>.uriAuthority(<js>"http://localhost:10000"</js>);
		 * 		}
		 * 	}
		 * </p>
		 *
		 * <ul class='seealso'>
		 * 	<li class='ja'>{@link Rest#uriAuthority}
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>The default is the first value found:
		 * 	<ul>
		 * 		<li>System property <js>"RestContext.uriAuthority"
		 * 		<li>Environment variable <js>"RESTCONTEXT_URIAUTHORITY"
		 * 		<li><jk>null</jk>
		 * 	</ul>
		 * @return This object.
		 */
		@FluentSetter
		public Builder uriAuthority(String value) {
			uriAuthority = value;
			return this;
		}

		/**
		 * Resource context path.
		 *
		 * <p>
		 * Overrides the context path value for this resource and any child resources.
		 *
		 * <p>
		 * This setting is useful if you want to use <js>"context:/child/path"</js> URLs in child resource POJOs but
		 * the context path is not actually specified on the servlet container.
		 *
		 * <p>
		 * Affects the following methods:
		 * <ul class='javatree'>
		 * 	<li class='jm'>{@link RestRequest#getContextPath()} - Returns the overridden context path for the resource.
		 * 	<li class='jm'>{@link RestRequest#getServletPath()} - Includes the overridden context path for the resource.
		 * </ul>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	<jc>// Option #1 - Defined via annotation resolving to a config file setting with default value.</jc>
		 * 	<ja>@Rest</ja>(
		 * 		path=<js>"/servlet"</js>,
		 * 		uriContext=<js>"$C{REST/contextPathOverride,/foo}"</js>
		 * 	)
		 * 	<jk>public class</jk> MyResource {
		 *
		 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
		 * 		<jk>public</jk> MyResource(Builder <jv>builder</jv>) <jk>throws</jk> Exception {
		 *
		 * 			<jc>// Using method on builder.</jc>
		 * 			<jv>builder</jv>.uriContext(<js>"/foo"</js>);
		 * 		}
		 *
		 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
		 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
		 * 		<jk>public void</jk> init(Builder <jv>builder</jv>) <jk>throws</jk> Exception {
		 * 			<jv>builder</jv>.uriContext(<js>"/foo"</js>);
		 * 		}
		 * 	}
		 * </p>
		 *
		 * <ul class='seealso'>
		 * 	<li class='ja'>{@link Rest#uriContext}
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>The default is the first value found:
		 * 	<ul>
		 * 		<li>System property <js>"RestContext.uriContext"
		 * 		<li>Environment variable <js>"RESTCONTEXT_URICONTEXT"
		 * 		<li><jk>null</jk>
		 * 	</ul>
		 * @return This object.
		 */
		@FluentSetter
		public Builder uriContext(String value) {
			uriContext = value;
			return this;
		}

		/**
		 * URI resolution relativity.
		 *
		 * <p>
		 * Specifies how relative URIs should be interpreted by serializers.
		 *
		 * <p>
		 * See {@link UriResolution} for possible values.
		 *
		 * <p>
		 * Affects the following methods:
		 * <ul class='javatree'>
		 * 	<li class='jm'>{@link RestRequest#getUriResolver()}
		 * </ul>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	<jc>// Option #1 - Defined via annotation resolving to a config file setting with default value.</jc>
		 * 	<ja>@Rest</ja>(
		 * 		path=<js>"/servlet"</js>,
		 * 		uriRelativity=<js>"$C{REST/uriRelativity,PATH_INFO}"</js>
		 * 	)
		 * 	<jk>public class</jk> MyResource {
		 *
		 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
		 * 		<jk>public</jk> MyResource(Builder <jv>builder</jv>) <jk>throws</jk> Exception {
		 *
		 * 			<jc>// Using method on builder.</jc>
		 * 			<jv>builder</jv>.uriRelativity(<jsf>PATH_INFO</jsf>);
		 * 		}
		 *
		 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
		 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
		 * 		<jk>public void</jk> init(Builder <jv>builder</jv>) <jk>throws</jk> Exception {
		 * 			<jv>builder</jv>.uriRelativity(<jsf>PATH_INFO</jsf>);
		 * 		}
		 * 	}
		 * </p>
		 *
		 * <ul class='seealso'>
		 * 	<li class='ja'>{@link Rest#uriRelativity}
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>The default is the first value found:
		 * 	<ul>
		 * 		<li>System property <js>"RestContext.uriRelativity"
		 * 		<li>Environment variable <js>"RESTCONTEXT_URIRELATIVITY"
		 * 		<li>{@link UriRelativity#RESOURCE}
		 * 	</ul>
		 * @return This object.
		 */
		@FluentSetter
		public Builder uriRelativity(UriRelativity value) {
			uriRelativity = value;
			return this;
		}

		/**
		 * URI resolution.
		 *
		 * <p>
		 * Specifies how relative URIs should be interpreted by serializers.
		 *
		 * <p>
		 * See {@link UriResolution} for possible values.
		 *
		 * <p>
		 * Affects the following methods:
		 * <ul class='javatree'>
		 * 	<li class='jm'>{@link RestRequest#getUriResolver()}
		 * </ul>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	<jc>// Option #1 - Defined via annotation resolving to a config file setting with default value.</jc>
		 * 	<ja>@Rest</ja>(
		 * 		path=<js>"/servlet"</js>,
		 * 		uriResolution=<js>"$C{REST/uriResolution,ABSOLUTE}"</js>
		 * 	)
		 * 	<jk>public class</jk> MyResource {
		 *
		 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
		 * 		<jk>public</jk> MyResource(Builder <jv>builder</jv>) <jk>throws</jk> Exception {
		 *
		 * 			<jc>// Using method on builder.</jc>
		 * 			<jv>builder</jv>.uriResolution(<jsf>ABSOLUTE</jsf>);
		 * 		}
		 *
		 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
		 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
		 * 		<jk>public void</jk> init(Builder <jv>builder</jv>) <jk>throws</jk> Exception {
		 * 			<jv>builder</jv>.uriResolution(<jsf>ABSOLUTE</jsf>);
		 * 		}
		 * 	}
		 * </p>
		 *
		 * <ul class='seealso'>
		 * 	<li class='ja'>{@link Rest#uriResolution}
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>The default is the first value found:
		 * 	<ul>
		 * 		<li>System property <js>"RestContext.uriResolution"
		 * 		<li>Environment variable <js>"RESTCONTEXT_URIRESOLUTION"
		 * 		<li>{@link UriResolution#ROOT_RELATIVE}
		 * 	</ul>
		 * @return This object.
		 */
		@FluentSetter
		public Builder uriResolution(UriResolution value) {
			uriResolution = value;
			return this;
		}

		//----------------------------------------------------------------------------------------------------
		// Methods that give access to the config file, var resolver, and properties.
		//----------------------------------------------------------------------------------------------------

		/**
		 * Returns the serializer group builder containing the serializers for marshalling POJOs into response bodies.
		 *
		 * <p>
		 * Serializer are used to convert POJOs to HTTP response bodies.
		 * <br>Any of the Juneau framework serializers can be used in this setting.
		 * <br>The serializer selected is based on the request <c>Accept</c> header matched against the values returned by the following method
		 * using a best-match algorithm:
		 * <ul class='javatree'>
		 * 	<li class='jm'>{@link Serializer#getMediaTypeRanges()}
		 * </ul>
		 *
		 * <p>
		 * The builder is initialized with serializers defined via the {@link Rest#serializers()} annotation.  That annotation is applied
		 * from parent-to-child order with child entries given priority over parent entries.
		 *
		 * <ul class='seealso'>
		 * 	<li class='link'>{@doc jrs.Marshalling}
		 * </ul>
		 *
		 * @return The serializer group builder for this context builder.
		 */
		public SerializerSet.Builder getSerializers() {
			return serializers;
		}

		/**
		 * Returns the parser group builder containing the parsers for converting HTTP request bodies into POJOs.
		 *
		 * <p>
		 * Parsers are used to convert the body of HTTP requests into POJOs.
		 * <br>Any of the Juneau framework parsers can be used in this setting.
		 * <br>The parser selected is based on the request <c>Content-Type</c> header matched against the values returned by the following method
		 * using a best-match algorithm:
		 * <ul class='javatree'>
		 * 	<li class='jm'>{@link Parser#getMediaTypes()}
		 * </ul>
		 *
		 * <p>
		 * The builder is initialized with parsers defined via the {@link Rest#parsers()} annotation.  That annotation is applied
		 * from parent-to-child order with child entries given priority over parent entries.
		 *
		 * <ul class='seealso'>
		 * 	<li class='link'>{@doc jrs.Marshalling}
		 * </ul>
		 *
		 * @return The parser group builder for this context builder.
		 */
		public ParserSet.Builder getParsers() {
			return parsers;
		}

		/**
		 * Returns the encoder group builder containing the encoders for compressing/decompressing input and output streams.
		 *
		 * <p>
		 * These can be used to enable various kinds of compression (e.g. <js>"gzip"</js>) on requests and responses.
		 *
		 * <p>
		 * The builder is initialized with encoders defined via the {@link Rest#encoders()} annotation.  That annotation is applied
		 * from parent-to-child order with child entries given priority over parent entries.
		 *
		 * <ul class='seealso'>
		 * 	<li class='link'>{@doc jrs.Encoders}
		 * </ul>
		 *
		 * @return The encoder group builder for this context builder.
		 */
		public EncoderSet.Builder getEncoders() {
			return encoders;
		}

		//----------------------------------------------------------------------------------------------------
		// Properties
		//----------------------------------------------------------------------------------------------------

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
		 * relative to the ascendant path UNLESS you use the {@link RestContext.Builder#child(String, Object)} method to register it.
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
		 * <p class='bcode w800'>
		 * 	<jc>// Our child resource.</jc>
		 * 	<ja>@Rest</ja>(path=<js>"/child"</js>)
		 * 	<jk>public class</jk> MyChildResource {...}
		 *
		 * 	<jc>// Option #1 - Registered via annotation.</jc>
		 * 	<ja>@Rest</ja>(children={MyChildResource.<jk>class</jk>})
		 * 	<jk>public class</jk> MyResource {
		 *
		 * 		<jc>// Option #2 - Registered via builder passed in through resource constructor.</jc>
		 * 		<jk>public</jk> MyResource(Builder <jv>builder</jv>) <jk>throws</jk> Exception {
		 *
		 * 			<jc>// Using method on builder.</jc>
		 * 			<jv>builder</jv>.children(MyChildResource.<jk>class</jk>);
		 *
		 * 			<jc>// Use a pre-instantiated object instead.</jc>
		 * 			<jv>builder</jv>.child(<js>"/child"</js>, <jk>new</jk> MyChildResource());
		 * 		}
		 *
		 * 		<jc>// Option #3 - Registered via builder passed in through init method.</jc>
		 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
		 * 		<jk>public void</jk> init(Builder <jv>builder</jv>) <jk>throws</jk> Exception {
		 * 			<jv>builder</jv>.children(MyChildResource.<jk>class</jk>);
		 * 		}
		 * 	}
		 * </p>
		 *
		 * <ul class='notes'>
		 * 	<li>
		 * 		When defined as classes, instances are resolved using the registered bean store which
		 * 		by default is {@link BeanStore} which requires the class have one of the following
		 * 		constructors:
		 * 		<ul>
		 * 			<li><code><jk>public</jk> T(Builder)</code>
		 * 			<li><code><jk>public</jk> T()</code>
		 * 		</ul>
		 * </ul>
		 *
		 * <ul class='seealso'>
		 * 	<li class='ja'>{@link Rest#children()}
		 * </ul>
		 *
		 * @param values
		 * 	The values to add to this setting.
		 * 	<br>Objects can be any of the specified types:
		 * 	<ul>
		 * 		<li>A class that has a constructor described above.
		 * 		<li>An instantiated resource object (such as a servlet object instantiated by a servlet container).
		 * 		<li>An instance of {@link RestChild} containing an instantiated resource object and a subpath.
		 * 	</ul>
		 * @return This object.
		 */
		@FluentSetter
		public Builder children(Object...values) {
			children.addAll(asList(values));
			return this;
		}

		/**
		 * Add a child REST resource.
		 *
		 * <p>
		 * Shortcut for adding a single child to this resource.
		 *
		 * <p>
		 * This can be used for resources that don't have a {@link Rest#path() @Rest(path)} annotation.
		 *
		 * @param path The child path relative to the parent resource URI.
		 * @param child The child to add to this resource.
		 * @return This object.
		 */
		@FluentSetter
		public Builder child(String path, Object child) {
			children.add(new RestChild(path, child));
			return this;
		}

		/**
		 * <i><l>RestContext</l> configuration property:&emsp;</i>  Parser listener.
		 *
		 * <p>
		 * Specifies the parser listener class to use for listening to non-fatal parsing errors.
		 *
		 * <ul class='seealso'>
		 * 	<li class='jm'>{@link org.apache.juneau.parser.Parser.Builder#listener(Class)}
		 * </ul>
		 *
		 * @param value The new value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder parserListener(Class<? extends ParserListener> value) {
			if (value != ParserListener.Null.class)
				parsers.forEach(x -> x.listener(value));
			return this;
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
		 * <p class='bcode w800'>
		 * 	<jc>// Option #1 - Defined via annotation.</jc>
		 * 	<ja>@Rest</ja>(path=<js>"/myResource"</js>)
		 * 	<jk>public class</jk> MyResource {
		 *
		 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
		 * 		<jk>public</jk> MyResource(Builder <jv>builder</jv>) <jk>throws</jk> Exception {
		 *
		 * 			<jc>// Using method on builder.</jc>
		 * 			<jv>builder</jv>.path(<js>"/myResource"</js>);
		 * 		}
		 *
		 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
		 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
		 * 		<jk>public void</jk> init(Builder <jv>builder</jv>) <jk>throws</jk> Exception {
		 * 			<jv>builder</jv>.path(<js>"/myResource"</js>);
		 * 		}
		 * 	}
		 * </p>
		 *
		 * <p>
		 * <ul class='notes'>
		 * 	<li>
		 * 		This annotation is ignored on top-level servlets (i.e. servlets defined in <c>web.xml</c> files).
		 * 		<br>Therefore, implementers can optionally specify a path value for documentation purposes.
		 * 	<li>
		 * 		Typically, this setting is only applicable to resources defined as children through the
		 * 		{@link Rest#children() @Rest(children)} annotation.
		 * 		<br>However, it may be used in other ways (e.g. defining paths for top-level resources in microservices).
		 * 	<li>
		 * 		Slashes are trimmed from the path ends.
		 * 		<br>As a convention, you may want to start your path with <js>'/'</js> simple because it make it easier to read.
		 * 	<li>
		 * 		This path is available through the following method:
		 * 		<ul>
		 * 			<li class='jm'>{@link RestContext#getPath() RestContext.getPath()}
		 * 		</ul>
		 * </ul>
		 *
		 * <ul class='seealso'>
		 * 	<li class='ja'>{@link Rest#path}
		 * </ul>
		 *
		 * @param value The new value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder path(String value) {
			value = trimLeadingSlashes(value);
			if (! isEmpty(value))
				path = value;
			return this;
		}

		/**
		 * REST children class.
		 *
		 * <p>
		 * Allows you to extend the {@link RestChildren} class to modify how any of the methods are implemented.
		 *
		 * <p>
		 * The subclass must have a public constructor that takes in any of the following arguments:
		 * <ul>
		 * 	<li>{@link RestChildren.Builder} - The builder for the object.
		 * 	<li>Any beans found in the specified bean store.
		 * 	<li>Any {@link Optional} beans that may or may not be found in the specified bean store.
		 * </ul>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	<jc>// Our extended context class</jc>
		 * 	<jk>public</jk> MyRestChildren <jk>extends</jk> RestChildren {
		 * 		<jk>public</jk> MyRestChildren(RestChildrenBuilder <jv>builder</jv>, ARequiredSpringBean <jv>bean1</jv>, Optional&lt;AnOptionalSpringBean&gt; <jv>bean2</jv>) {
		 * 			<jk>super</jk>(<jv>builder</jv>);
		 * 		}
		 *
		 * 		<jc>// Override any methods.</jc>
		 *
		 * 		<ja>@Override</ja>
		 * 		<jk>public</jk> Optional&lt;RestChildMatch&gt; findMatch(RestCall <jv>call</jv>) {
		 * 			String <jv>path</jv> = <jv>call</jv>.getPathInfo();
		 * 			<jk>if</jk> (<jv>path</jv>.endsWith(<js>"/foo"</js>)) {
		 * 				<jc>// Do our own special handling.</jc>
		 * 			}
		 * 			<jk>return super</jk>.findMatch(<jv>call</jv>);
		 * 		}
		 * 	}
		 * </p>
		 * <p class='bcode w800'>
		 * 	<jc>// Option #1 - Defined via annotation.</jc>
		 * 	<ja>@Rest</ja>(restChildrenClass=MyRestChildren.<jk>class</jk>)
		 * 	<jk>public class</jk> MyResource {
		 * 		...
		 *
		 * 		<jc>// Option #2 - Defined via builder passed in through init method.</jc>
		 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
		 * 		<jk>public void</jk> init(Builder <jv>builder</jv>) <jk>throws</jk> Exception {
		 * 			<jv>builder</jv>.restChildrenClass(MyRestChildren.<jk>class</jk>);
		 * 		}
		 * 	}
		 * </p>
		 *
		 * @param value The new value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder restChildrenClass(Class<? extends RestChildren> value) {
			childrenClass = value;
			return this;
		}

		/**
		 * REST operation context class.
		 *
		 * <p>
		 * Allows you to extend the {@link RestOpContext} class to modify how any of the methods are implemented.
		 *
		 * <p>
		 * The subclass must have a public constructor that takes in any of the following arguments:
		 * <ul>
		 * 	<li>{@link RestOpContext.Builder} - The builder for the object.
		 * 	<li>Any beans found in the specified bean store.
		 * 	<li>Any {@link Optional} beans that may or may not be found in the specified bean store.
		 * </ul>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	<jc>// Our extended context class that adds a request attribute to all requests.</jc>
		 * 	<jc>// The attribute value is provided by an injected spring bean.</jc>
		 * 	<jk>public</jk> MyRestOperationContext <jk>extends</jk> RestOpContext {
		 *
		 * 		<jk>private final</jk> Optional&lt;? <jk>extends</jk> Supplier&lt;Object&gt;&gt; <jf>fooSupplier</jf>;
		 *
		 * 		<jc>// Constructor that takes in builder and optional injected attribute provider.</jc>
		 * 		<jk>public</jk> MyRestOperationContext(RestOpContext.Builder <jv>builder</jv>, Optional&lt;AnInjectedFooSupplier&gt; <jv>fooSupplier</jv>) {
		 * 			<jk>super</jk>(<jv>builder</jv>);
		 * 			<jk>this</jk>.<jf>fooSupplier</jf> = <jv>fooSupplier</jv>.orElseGet(()-><jk>null</jk>);
		 * 		}
		 *
		 * 		<jc>// Override the method used to create default request attributes.</jc>
		 * 		<ja>@Override</ja>
		 * 		<jk>protected</jk> NamedAttributeList createDefaultRequestAttributes(Object <jv>resource</jv>, BeanStore <jv>beanStore</jv>, Method <jv>method</jv>, RestContext <jv>context</jv>) <jk>throws</jk> Exception {
		 * 			<jk>return super</jk>
		 * 				.createDefaultRequestAttributes(<jv>resource</jv>, <jv>beanStore</jv>, <jv>method</jv>, <jv>context</jv>)
		 * 				.append(NamedAttribute.<jsm>of</jsm>(<js>"foo"</js>, ()-><jf>fooSupplier</jf>.get());
		 * 		}
		 * 	}
		 * </p>
		 * <p class='bcode w800'>
		 * 	<jc>// Option #1 - Defined via annotation.</jc>
		 * 	<ja>@Rest</ja>(restOpContextClass=MyRestOperationContext.<jk>class</jk>)
		 * 	<jk>public class</jk> MyResource {
		 * 		...
		 *
		 * 		<jc>// Option #2 - Defined via builder passed in through init method.</jc>
		 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
		 * 		<jk>public void</jk> init(Builder <jv>builder</jv>) <jk>throws</jk> Exception {
		 * 			<jv>builder</jv>.methodContextClass(MyRestOperationContext.<jk>class</jk>);
		 * 		}
		 *
		 * 		<ja>@RestGet</ja>
		 * 		<jk>public</jk> Object foo(RequestAttributes <jv>attributes</jv>) {
		 * 			<jk>return</jk> <jv>attributes</jv>.get(<js>"foo"</js>);
		 * 		}
		 * 	}
		 * </p>
		 *
		 * @param value The new value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder restOpContextClass(Class<? extends RestOpContext> value) {
			opContextClass = value;
			return this;
		}

		/**
		 * REST operations class.
		 *
		 * <p>
		 * Allows you to extend the {@link RestOperations} class to modify how any of the methods are implemented.
		 *
		 * <p>
		 * The subclass must have a public constructor that takes in any of the following arguments:
		 * <ul>
		 * 	<li>{@link RestOperations.Builder} - The builder for the object.
		 * 	<li>Any beans found in the specified bean store.
		 * 	<li>Any {@link Optional} beans that may or may not be found in the specified bean store.
		 * </ul>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	<jc>// Our extended context class</jc>
		 * 	<jk>public</jk> MyRestOperations <jk>extends</jk> RestOperations {
		 * 		<jk>public</jk> MyRestOperations(RestOperationsBuilder <jv>builder</jv>, ARequiredSpringBean <jv>bean1</jv>, Optional&lt;AnOptionalSpringBean&gt; <jv>bean2</jv>) {
		 * 			<jk>super</jk>(<jv>builder</jv>);
		 * 		}
		 *
		 * 		<jc>// Override any methods.</jc>
		 *
		 * 		<ja>@Override</ja>
		 * 		<jk>public</jk> RestOpContext findMethod(RestCall <jv>call</jv>) <jk>throws</jk> MethodNotAllowed, PreconditionFailed, NotFound {
		 * 			String <jv>path</jv> = <jv>call</jv>.getPathInfo();
		 * 			<jk>if</jk> (<jv>path</jv>.endsWith(<js>"/foo"</js>)) {
		 * 				<jc>// Do our own special handling.</jc>
		 * 			}
		 * 			<jk>return super</jk>.findMethod(<jv>call</jv>);
		 * 		}
		 * 	}
		 * </p>
		 * <p class='bcode w800'>
		 * 	<jc>// Option #1 - Defined via annotation.</jc>
		 * 	<ja>@Rest</ja>(restMethodsClass=MyRestOperations.<jk>class</jk>)
		 * 	<jk>public class</jk> MyResource {
		 * 		...
		 *
		 * 		<jc>// Option #2 - Defined via builder passed in through init method.</jc>
		 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
		 * 		<jk>public void</jk> init(Builder <jv>builder</jv>) <jk>throws</jk> Exception {
		 * 			<jv>builder</jv>.restMethodsClass(MyRestOperations.<jk>class</jk>);
		 * 		}
		 * 	}
		 * </p>
		 *
		 * @param value The new value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder restOperationsClass(Class<? extends RestOperations> value) {
			operationsClass = value;
			return this;
		}

		/**
		 * <i><l>RestContext</l> configuration property:&emsp;</i>  Serializer listener.
		 *
		 * <p>
		 * Specifies the serializer listener class to use for listening to non-fatal serialization errors.
		 *
		 * <ul class='seealso'>
		 * 	<li class='jm'>{@link org.apache.juneau.serializer.Serializer.Builder#listener(Class)}
		 * </ul>
		 *
		 * @param value The new value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder serializerListener(Class<? extends SerializerListener> value) {
			if (value != SerializerListener.Null.class)
				serializers.forEach(x -> x.listener(value));
			return this;
		}

		/**
		 * Supported accept media types.
		 *
		 * <p>
		 * Overrides the media types inferred from the serializers that identify what media types can be produced by the resource.
		 * <br>An example where this might be useful if you have serializers registered that handle media types that you
		 * don't want exposed in the Swagger documentation.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	<jc>// Option #1 - Defined via annotation resolving to a config file setting with default value.</jc>
		 * 	<ja>@Rest</ja>(produces={<js>"$C{REST/supportedProduces,application/json}"</js>})
		 * 	<jk>public class</jk> MyResource {
		 *
		 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
		 * 		<jk>public</jk> MyResource(Builder <jv>builder</jv>) <jk>throws</jk> Exception {
		 *
		 * 			<jc>// Using method on builder.</jc>
		 * 			<jv>builder</jv>.produces(<jk>false</jk>, <js>"application/json"</js>)
		 * 		}
		 *
		 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
		 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
		 * 		<jk>public void</jk> init(Builder <jv>builder</jv>) <jk>throws</jk> Exception {
		 * 			<jv>builder</jv>.produces(<jk>false</jk>, <js>"application/json"</js>);
		 * 		}
		 * 	}
		 * </p>
		 *
		 * <p>
		 * This affects the returned values from the following:
		 * <ul class='javatree'>
		 * 	<li class='jm'>{@link RestContext#getProduces() RestContext.getProduces()}
		 * 	<li class='jm'>{@link SwaggerProvider#getSwagger(RestContext,Locale)} - Affects produces field.
		 * </ul>
		 *
		 * <ul class='seealso'>
		 * 	<li class='ja'>{@link Rest#produces}
		 * 	<li class='ja'>{@link RestOp#produces}
		 * 	<li class='ja'>{@link RestGet#produces}
		 * 	<li class='ja'>{@link RestPut#produces}
		 * 	<li class='ja'>{@link RestPost#produces}
		 * </ul>
		 *
		 * @param values The values to add to this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder produces(MediaType...values) {
			if (produces == null)
				produces = new ArrayList<>(Arrays.asList(values));
			else
				produces.addAll(Arrays.asList(values));
			return this;
		}

		/**
		 * Returns the media types produced by this resource if it's manually specified.
		 *
		 * @return The media types.
		 */
		public Optional<List<MediaType>> produces() {
			return ofNullable(produces);
		}

		/**
		 * Supported content media types.
		 *
		 * <p>
		 * Overrides the media types inferred from the parsers that identify what media types can be consumed by the resource.
		 * <br>An example where this might be useful if you have parsers registered that handle media types that you
		 * don't want exposed in the Swagger documentation.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	<jc>// Option #1 - Defined via annotation resolving to a config file setting with default value.</jc>
		 * 	<ja>@Rest</ja>(consumes={<js>"$C{REST/supportedConsumes,application/json}"</js>})
		 * 	<jk>public class</jk> MyResource {
		 *
		 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
		 * 		<jk>public</jk> MyResource(Builder <jv>builder</jv>) <jk>throws</jk> Exception {
		 *
		 * 			<jc>// Using method on builder.</jc>
		 * 			<jv>builder</jv>.consumes(<jk>false</jk>, <js>"application/json"</js>)
		 * 		}
		 *
		 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
		 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
		 * 		<jk>public void</jk> init(Builder <jv>builder</jv>) <jk>throws</jk> Exception {
		 * 			<jv>builder</jv>.consumes(<jk>false</jk>, <js>"application/json"</js>);
		 * 		}
		 * 	}
		 * </p>
		 *
		 * <p>
		 * This affects the returned values from the following:
		 * <ul class='javatree'>
		 * 	<li class='jm'>{@link RestContext#getConsumes() RestContext.getConsumes()}
		 * </ul>
		 *
		 * <ul class='seealso'>
		 * 	<li class='ja'>{@link Rest#consumes}
		 * 	<li class='ja'>{@link RestOp#consumes}
		 * 	<li class='ja'>{@link RestPut#consumes}
		 * 	<li class='ja'>{@link RestPost#consumes}
		 * </ul>
		 *
		 * @param values The values to add to this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder consumes(MediaType...values) {
			if (consumes == null)
				consumes = new ArrayList<>(Arrays.asList(values));
			else
				consumes.addAll(Arrays.asList(values));
			return this;
		}

		/**
		 * Returns the media types consumed by this resource if it's manually specified.
		 *
		 * @return The media types.
		 */
		public Optional<List<MediaType>> consumes() {
			return ofNullable(consumes);
		}

		/**
		 * Swagger provider.
		 *
		 * <p>
		 * Class used to retrieve swagger information about a resource.
		 *
		 * <p>
		 * This setting is inherited from the parent context if not specified.
		 * <br>The default is {@link BasicSwaggerProvider}.
		 *
		 * <ul class='seealso'>
		 * 	<li class='ja'>{@link Rest#swaggerProvider}
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder swaggerProvider(Class<? extends SwaggerProvider> value) {
			swaggerProvider().type(value);
			return this;
		}

		/**
		 * Swagger provider.
		 *
		 * <p>
		 * Same as {@link #swaggerProvider(Class)} except input is a pre-constructed instance.
		 *
		 * @param value
		 * 	The new value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder swaggerProvider(SwaggerProvider value) {
			swaggerProvider().impl(value);
			return this;
		}

		// <FluentSetters>

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder annotations(Annotation...values) {
			super.annotations(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder apply(AnnotationWorkList work) {
			super.apply(work);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder applyAnnotations(java.lang.Class<?>...fromClasses) {
			super.applyAnnotations(fromClasses);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder applyAnnotations(Method...fromMethods) {
			super.applyAnnotations(fromMethods);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder debug() {
			super.debug();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder debug(boolean value) {
			super.debug(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder impl(Context value) {
			super.impl(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder type(Class<?> value) {
			super.type(value);
			return this;
		}

		// </FluentSetters>

		//----------------------------------------------------------------------------------------------------
		// Helper methods
		//----------------------------------------------------------------------------------------------------

		private static MethodList getHookMethods(Supplier<?> resource, HookEvent event) {
			Map<String,Method> x = AMap.create();
			Object r = resource.get();

			for (MethodInfo m : ClassInfo.ofProxy(r).getAllMethodsParentFirst())
				for (RestHook h : m.getAnnotations(RestHook.class))
					if (h.value() == event)
						x.put(m.getSignature(), m.accessible().inner());

			MethodList x2 = MethodList.of(x.values());
			return x2;
		}

		//----------------------------------------------------------------------------------------------------
		// Methods inherited from ServletConfig
		//----------------------------------------------------------------------------------------------------

		@Override /* ServletConfig */
		public String getInitParameter(String name) {
			return inner == null ? null : inner.getInitParameter(name);
		}

		@Override /* ServletConfig */
		public Enumeration<String> getInitParameterNames() {
			return inner == null ? new Vector<String>().elements() : inner.getInitParameterNames();
		}

		@Override /* ServletConfig */
		public ServletContext getServletContext() {
			return inner != null ? inner.getServletContext() : parentContext != null ? parentContext.getBuilder().getServletContext() : null;
		}

		@Override /* ServletConfig */
		public String getServletName() {
			return inner == null ? null : inner.getServletName();
		}
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final Supplier<?> resource;
	private final Class<?> resourceClass;

	final Builder builder;
	private final boolean
		allowBodyParam,
		renderResponseStackTraces;
	private final String
		clientVersionHeader,
		uriAuthority,
		uriContext;
	private final String path, fullPath;
	private final UrlPathMatcher pathMatcher;

	private final Set<String> allowedMethodParams, allowedHeaderParams, allowedMethodHeaders;

	private final Class<? extends RestOpArg>[] restOpArgs;
	private final BeanContext beanContext;
	private final EncoderSet encoders;
	private final SerializerSet serializers;
	private final ParserSet parsers;
	private final HttpPartSerializer partSerializer;
	private final HttpPartParser partParser;
	private final JsonSchemaGenerator jsonSchemaGenerator;
	private final List<MediaType> consumes, produces;
	private final HeaderList defaultRequestHeaders, defaultResponseHeaders;
	private final NamedAttributeList defaultRequestAttributes;
	private final ResponseProcessor[] responseProcessors;
	private final Messages messages;
	private final Config config;
	private final VarResolver varResolver;
	private final RestOperations restOperations;
	private final RestChildren restChildren;
	private final Logger logger;
	private final SwaggerProvider swaggerProvider;
	private final BasicHttpException initException;
	private final RestContext parentContext;
	private final BeanStore beanStore;
	private final UriResolution uriResolution;
	private final UriRelativity uriRelativity;
	private final MethodExecStore methodExecStore;
	private final ThrownStore thrownStore;
	private final ConcurrentHashMap<Locale,Swagger> swaggerCache = new ConcurrentHashMap<>();
	private final Instant startTime;
	private final Map<Class<?>,ResponseBeanMeta> responseBeanMetas = new ConcurrentHashMap<>();
	final Charset defaultCharset;
	final long maxInput;

	final DefaultClassList defaultClasses;
	final DefaultSettingsMap defaultSettings;
	final BeanStore rootBeanStore;

	// Lifecycle methods
	private final MethodInvoker[]
		postInitMethods,
		postInitChildFirstMethods,
		startCallMethods,
		endCallMethods,
		destroyMethods;

	private final MethodList
		preCallMethods,
		postCallMethods;

	private final FileFinder fileFinder;
	private final StaticFiles staticFiles;
	private final RestLogger callLogger;
	private final DebugEnablement debugEnablement;

	private final ThreadLocal<RestSession> localSession = new ThreadLocal<>();

	// Gets set when postInitChildFirst() gets called.
	private final AtomicBoolean initialized = new AtomicBoolean(false);

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing the settings for this bean.
	 * @throws Exception If any initialization problems were encountered.
	 */
	protected RestContext(Builder builder) throws Exception {
		super(builder);

		startTime = Instant.now();

		REGISTRY.put(builder.resourceClass, this);

		BasicHttpException _initException = null;

		try {
			this.builder = builder;

			resourceClass = builder.resourceClass;
			resource = builder.resource;
			parentContext = builder.parentContext;
			rootBeanStore = builder.rootBeanStore();
			defaultClasses = builder.defaultClasses();
			defaultSettings = builder.defaultSettings();

			BeanStore bs = beanStore = builder.beanStore();
			beanStore
				.addBean(BeanStore.class, beanStore)
				.addBean(RestContext.class, this)
				.addBean(Object.class, resource.get())
				.addBean(Builder.class, builder)
				.addBean(AnnotationWorkList.class, builder.getApplied());

			path = ofNullable(builder.path).orElse("");
			fullPath = (parentContext == null ? "" : (parentContext.fullPath + '/')) + path;
			String p = path;
			if (! p.endsWith("/*"))
				p += "/*";
			pathMatcher = UrlPathMatcher.of(p);

			allowBodyParam = ! builder.disableBodyParam;
			allowedHeaderParams = newCaseInsensitiveSet(ofNullable(builder.allowedHeaderParams).map(x -> "NONE".equals(x) ? "" : x).orElse(""));
			allowedMethodParams = newCaseInsensitiveSet(ofNullable(builder.allowedMethodParams).map(x -> "NONE".equals(x) ? "" : x).orElse(""));
			allowedMethodHeaders = newCaseInsensitiveSet(ofNullable(builder.allowedMethodHeaders).map(x -> "NONE".equals(x) ? "" : x).orElse(""));
			clientVersionHeader = builder.clientVersionHeader;
			defaultCharset = builder.defaultCharset;
			maxInput = builder.maxInput;
			renderResponseStackTraces = builder.renderResponseStackTraces;
			uriContext = builder.uriContext;
			uriAuthority = builder.uriAuthority;
			uriResolution = builder.uriResolution;
			uriRelativity = builder.uriRelativity;

			beanContext = bs.add(BeanContext.class, builder.beanContext().build());
			encoders = bs.add(EncoderSet.class, builder.encoders().build());
			serializers = bs.add(SerializerSet.class, builder.serializers().build());
			parsers = bs.add(ParserSet.class, builder.parsers().build());
			logger = bs.add(Logger.class, builder.logger());
			thrownStore = bs.add(ThrownStore.class, builder.thrownStore().build());
			methodExecStore = bs.add(MethodExecStore.class, builder.methodExecStore().thrownStoreOnce(thrownStore).build());
			messages = bs.add(Messages.class, builder.messages().build());
			varResolver = bs.add(VarResolver.class, builder.varResolver().bean(Messages.class, messages).build());
			config = bs.add(Config.class, builder.config().resolving(varResolver.createSession()));
			responseProcessors = bs.add(ResponseProcessor[].class, builder.responseProcessors().build().toArray());
			callLogger = bs.add(RestLogger.class, builder.callLogger().beanStore(beanStore).loggerOnce(logger).thrownStoreOnce(thrownStore).build());
			partSerializer = bs.add(HttpPartSerializer.class, builder.partSerializer().create());
			partParser = bs.add(HttpPartParser.class, builder.partParser().create());
			jsonSchemaGenerator = bs.add(JsonSchemaGenerator.class, builder.jsonSchemaGenerator().build());
			fileFinder = bs.add(FileFinder.class, builder.fileFinder().build());
			staticFiles = bs.add(StaticFiles.class, builder.staticFiles().build());
			defaultRequestHeaders = bs.add("RestContext.defaultRequestHeaders", builder.defaultRequestHeaders().build());
			defaultResponseHeaders = bs.add("RestContext.defaultResponseHeaders", builder.defaultResponseHeaders().build());
			defaultRequestAttributes = bs.add("RestContext.defaultRequestAttributes", builder.defaultRequestAttributes().build());
			restOpArgs = builder.restOpArgs().build().asArray();
			debugEnablement = builder.debugEnablement().build();
			startCallMethods = builder.startCallMethods().stream().map(this::toMethodInvoker).toArray(MethodInvoker[]::new);
			endCallMethods = builder.endCallMethods().stream().map(this::toMethodInvoker).toArray(MethodInvoker[]::new);
			postInitMethods = builder.postInitMethods().stream().map(this::toMethodInvoker).toArray(MethodInvoker[]::new);
			postInitChildFirstMethods = builder.postInitChildFirstMethods().stream().map(this::toMethodInvoker).toArray(MethodInvoker[]::new);
			destroyMethods = builder.destroyMethods().stream().map(this::toMethodInvoker).toArray(MethodInvoker[]::new);
			preCallMethods = builder.preCallMethods();
			postCallMethods = builder.postCallMethods();
			restOperations = builder.restOperations(this).build();
			restChildren = builder.restChildren(this).build();
			swaggerProvider = builder.swaggerProvider().build();

			List<RestOpContext> opContexts = restOperations.getOpContexts();

			produces = builder.produces().orElseGet(
				()->{
					Set<MediaType> s = opContexts.isEmpty() ? emptySet() : new LinkedHashSet<>(opContexts.get(0).getSerializers().getSupportedMediaTypes());
					opContexts.forEach(x -> s.retainAll(x.getSerializers().getSupportedMediaTypes()));
					return AList.unmodifiable(s);
				}
			);
			consumes = builder.consumes().orElseGet(
				()->{
					Set<MediaType> s = opContexts.isEmpty() ? emptySet() : new LinkedHashSet<>(opContexts.get(0).getParsers().getSupportedMediaTypes());
					opContexts.forEach(x -> s.retainAll(x.getParsers().getSupportedMediaTypes()));
					return AList.unmodifiable(s);
				}
			);

		} catch (BasicHttpException e) {
			_initException = e;
			throw e;
		} catch (Exception e) {
			_initException = new InternalServerError(e);
			throw e;
		} finally {
			initException = _initException;
		}
	}

	private MethodInvoker toMethodInvoker(Method m) {
		return new MethodInvoker(m, getMethodExecStats(m));
	}

	private Set<String> newCaseInsensitiveSet(String value) {
		Set<String> s = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER) {
			private static final long serialVersionUID = 1L;
			@Override
			public boolean contains(Object v) {
				return v == null ? false : super.contains(v);
			}
		};
		for (String v : StringUtils.split(value))
			s.add(v);
		return Collections.unmodifiableSet(s);
	}

	@Override /* Context */
	public RestSession.Builder createSession() {
		return RestSession.create(this);
	}

	/**
	 * Returns the bean store associated with this context.
	 *
	 * <p>
	 * The bean store is used for instantiating child resource classes.
	 *
	 * @return The resource resolver associated with this context.
	 */
	public BeanStore getBeanStore() {
		return beanStore;
	}

	/**
	 * Returns the bean context associated with this context.
	 *
	 * @return The bean store associated with this context.
	 */
	public BeanContext getBeanContext() {
		return beanContext;
	}

	/**
	 * Returns the encoders associated with this context.
	 *
	 * @return The encoders associated with this context.
	 */
	public EncoderSet getEncoders() {
		return encoders;
	}

	/**
	 * Returns the serializers associated with this context.
	 *
	 * @return The serializers associated with this context.
	 */
	public SerializerSet getSerializers() {
		return serializers;
	}

	/**
	 * Returns the parsers associated with this context.
	 *
	 * @return The parsers associated with this context.
	 */
	public ParserSet getParsers() {
		return parsers;
	}

	/**
	 * Returns the time statistics gatherer for the specified method.
	 *
	 * @param m The method to get statistics for.
	 * @return The cached time-stats object.
	 */
	protected MethodExecStats getMethodExecStats(Method m) {
		return this.methodExecStore.getStats(m);
	}

	/**
	 * Returns the variable resolver for this servlet.
	 *
	 * <p>
	 * Variable resolvers are used to replace variables in property values.
	 * They can be nested arbitrarily deep.
	 * They can also return values that themselves contain other variables.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
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
	 * <p class='bcode w800'>
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
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc jrs.SvlVariables}
	 * </ul>
	 *
	 * @return The var resolver in use by this resource.
	 */
	public VarResolver getVarResolver() {
		return varResolver;
	}

	/**
	 * Returns the config file associated with this servlet.
	 *
	 * <p>
	 * The config file is identified via one of the following:
	 * <ul class='javatree'>
	 * 	<li class='ja'>{@link Rest#config()}
	 * 	<li class='jm'>{@link RestContext.Builder#config(Config)}
	 * </ul>
	 *
	 * @return
	 * 	The resolving config file associated with this servlet.
	 * 	<br>Never <jk>null</jk>.
	 */
	public Config getConfig() {
		return config;
	}


	/**
	 * Returns the path for this resource as defined by the {@link Rest#path() @Rest(path)} annotation or
	 * {@link RestContext.Builder#path(String)} method.
	 *
	 * <p>
	 * If path is not specified, returns <js>""</js>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContext.Builder#path(String)}
	 * </ul>
	 *
	 * @return The servlet path.
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Returns the path for this resource as defined by the {@link Rest#path() @Rest(path)} annotation or
	 * {@link RestContext.Builder#path(String)} method concatenated with those on all parent classes.
	 *
	 * <p>
	 * If path is not specified, returns <js>""</js>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContext.Builder#path(String)}
	 * </ul>
	 *
	 * @return The full path.
	 */
	public String getFullPath() {
		return fullPath;
	}

	/**
	 * Returns the call logger to use for this resource.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContext.Builder#callLogger()}
	 * </ul>
	 *
	 * @return
	 * 	The call logger to use for this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public RestLogger getCallLogger() {
		return callLogger;
	}

	/**
	 * Returns the resource bundle used by this resource.
	 *
	 * @return
	 * 	The resource bundle for this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public Messages getMessages() {
		return messages;
	}

	/**
	 * Returns the Swagger provider used by this resource.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContext.Builder#swaggerProvider(Class)}
	 * 	<li class='jm'>{@link RestContext.Builder#swaggerProvider(SwaggerProvider)}
	 * </ul>
	 *
	 * @return
	 * 	The information provider for this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public SwaggerProvider getSwaggerProvider() {
		return swaggerProvider;
	}

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
	public Object getResource() {
		return resource.get();
	}

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
	 * Returns the child resources associated with this servlet.
	 *
	 * @return
	 * 	An unmodifiable map of child resources.
	 * 	Keys are the {@link Rest#path() @Rest(path)} annotation defined on the child resource.
	 */
	public RestChildren getRestChildren() {
		return restChildren;
	}

	/**
	 * Returns whether it's safe to render stack traces in HTTP responses.
	 *
	 * @return <jk>true</jk> if setting is enabled.
	 */
	public boolean isRenderResponseStackTraces() {
		return renderResponseStackTraces;
	}

	/**
	 * Returns whether it's safe to pass the HTTP body as a <js>"body"</js> GET parameter.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContext.Builder#disableBodyParam()}
	 * </ul>
	 *
	 * @return <jk>true</jk> if setting is enabled.
	 */
	public boolean isAllowBodyParam() {
		return allowBodyParam;
	}

	/**
	 * Allowed header URL parameters.
	 *
	 * <ul class='seealso'>
	 * 	<li class='ja'>{@link Rest#allowedHeaderParams}
	 * 	<li class='jm'>{@link RestContext.Builder#allowedHeaderParams(String)}
	 * </ul>
	 *
	 * @return
	 * 	The header names allowed to be passed as URL parameters.
	 * 	<br>The set is case-insensitive ordered and unmodifiable.
	 */
	public Set<String> getAllowedHeaderParams() {
		return allowedHeaderParams;
	}

	/**
	 * Allowed method headers.
	 *
	 * <ul class='seealso'>
	 * 	<li class='ja'>{@link Rest#allowedMethodHeaders}
	 * 	<li class='jm'>{@link RestContext.Builder#allowedMethodHeaders(String)}
	 * </ul>
	 *
	 * @return
	 * 	The method names allowed to be passed as <c>X-Method</c> headers.
	 * 	<br>The set is case-insensitive ordered and unmodifiable.
	 */
	public Set<String> getAllowedMethodHeaders() {
		return allowedMethodHeaders;
	}

	/**
	 * Allowed method URL parameters.
	 *
	 * <ul class='seealso'>
	 * 	<li class='ja'>{@link Rest#allowedMethodParams}
	 * 	<li class='jm'>{@link RestContext.Builder#allowedMethodParams(String)}
	 * </ul>
	 *
	 * @return
	 * 	The method names allowed to be passed as <c>method</c> URL parameters.
	 * 	<br>The set is case-insensitive ordered and unmodifiable.
	 */
	public Set<String> getAllowedMethodParams() {
		return allowedMethodParams;
	}

	/**
	 * Returns the name of the client version header name used by this resource.
	 *
	 * <ul class='seealso'>
	 * 	<li class='ja'>{@link Rest#clientVersionHeader}
	 * 	<li class='jm'>{@link RestContext.Builder#clientVersionHeader(String)}
	 * </ul>
	 *
	 * @return
	 * 	The name of the client version header used by this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public String getClientVersionHeader() {
		return clientVersionHeader;
	}

	/**
	 * Returns the file finder associated with this context.
	 *
	 * @return
	 * 	The file finder for this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public FileFinder getFileFinder() {
		return fileFinder;
	}

	/**
	 * Returns the static files associated with this context.
	 *
	 * @return
	 * 	The static files for this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public StaticFiles getStaticFiles() {
		return staticFiles;
	}

	/**
	 * Returns the logger associated with this context.
	 *
	 * @return
	 * 	The logger for this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public Logger getLogger() {
		return logger;
	}

	/**
	 * Returns the stack trace database associated with this context.
	 *
	 * @return
	 * 	The stack trace database for this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public ThrownStore getThrownStore() {
		return thrownStore;
	}

	/**
	 * Returns the HTTP-part parser associated with this resource.
	 *
	 * @return
	 * 	The HTTP-part parser associated with this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public HttpPartParser getPartParser() {
		return partParser;
	}

	/**
	 * Returns the HTTP-part serializer associated with this resource.
	 *
	 * @return
	 * 	The HTTP-part serializer associated with this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public HttpPartSerializer getPartSerializer() {
		return partSerializer;
	}

	/**
	 * Returns the JSON-Schema generator associated with this resource.
	 *
	 * @return
	 * 	The HTTP-part serializer associated with this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public JsonSchemaGenerator getJsonSchemaGenerator() {
		return jsonSchemaGenerator;
	}

	/**
	 * Returns the explicit list of supported accept types for this resource.
	 *
	 * <p>
	 * Consists of the media types for production common to all operations on this class.
	 *
	 * <p>
	 * Can be overridden by {@link RestContext.Builder#produces(MediaType...)}.
	 *
	 * @return
	 * 	An unmodifiable list of supported <c>Accept</c> header values for this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public List<MediaType> getProduces() {
		return produces;
	}

	/**
	 * Returns the explicit list of supported content types for this resource.
	 *
	 * <p>
	 * Consists of the media types for consumption common to all operations on this class.
	 *
	 * <p>
	 * Can be overridden by {@link RestContext.Builder#consumes(MediaType...)}.
	 *
	 * @return
	 * 	An unmodifiable list of supported <c>Content-Type</c> header values for this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public List<MediaType> getConsumes() {
		return consumes;
	}

	/**
	 * Returns the default request headers for this resource.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContext.Builder#defaultRequestHeaders(org.apache.http.Header...)}
	 * </ul>
	 *
	 * @return
	 * 	The default request headers for this resource in an unmodifiable list.
	 * 	<br>Never <jk>null</jk>.
	 */
	public HeaderList getDefaultRequestHeaders() {
		return defaultRequestHeaders;
	}

	/**
	 * Returns the default request attributes for this resource.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContext.Builder#defaultRequestAttributes(NamedAttribute...)}
	 * </ul>
	 *
	 * @return
	 * 	The default request headers for this resource in an unmodifiable list.
	 * 	<br>Never <jk>null</jk>.
	 */
	public NamedAttributeList getDefaultRequestAttributes() {
		return defaultRequestAttributes;
	}

	/**
	 * Returns the default response headers for this resource.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContext.Builder#defaultResponseHeaders(org.apache.http.Header...)}
	 * </ul>
	 *
	 * @return
	 * 	The default response headers for this resource in an unmodifiable list.
	 * 	<br>Never <jk>null</jk>.
	 */
	public HeaderList getDefaultResponseHeaders() {
		return defaultResponseHeaders;
	}

	/**
	 * Returns the authority path of the resource.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContext.Builder#uriAuthority(String)}
	 * </ul>
	 *
	 * @return
	 * 	The authority path of this resource.
	 * 	<br>If not specified, returns the context path of the ascendant resource.
	 */
	public String getUriAuthority() {
		if (uriAuthority != null)
			return uriAuthority;
		if (parentContext != null)
			return parentContext.getUriAuthority();
		return null;
	}

	/**
	 * Returns the context path of the resource.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContext.Builder#uriContext(String)}
	 * </ul>
	 *
	 * @return
	 * 	The context path of this resource.
	 * 	<br>If not specified, returns the context path of the ascendant resource.
	 */
	public String getUriContext() {
		if (uriContext != null)
			return uriContext;
		if (parentContext != null)
			return parentContext.getUriContext();
		return null;
	}

	/**
	 * Returns the setting on how relative URIs should be interpreted as relative to.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContext.Builder#uriRelativity(UriRelativity)}
	 * </ul>
	 *
	 * @return
	 * 	The URI-resolution relativity setting value.
	 * 	<br>Never <jk>null</jk>.
	 */
	public UriRelativity getUriRelativity() {
		return uriRelativity;
	}

	/**
	 * Returns the setting on how relative URIs should be resolved.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContext.Builder#uriResolution(UriResolution)}
	 * </ul>
	 *
	 * @return
	 * 	The URI-resolution setting value.
	 * 	<br>Never <jk>null</jk>.
	 */
	public UriResolution getUriResolution() {
		return uriResolution;
	}

	/**
	 * Returns the REST Java methods defined in this resource.
	 *
	 * <p>
	 * These are the methods annotated with the {@link RestOp @RestOp} annotation.
	 *
	 * @return
	 * 	An unmodifiable map of Java method names to call method objects.
	 */
	public RestOperations getRestOperations() {
		return restOperations;
	}

	/**
	 * Returns the timing statistics on all method executions on this class.
	 *
	 * @return The timing statistics on all method executions on this class.
	 */
	public MethodExecStore getMethodExecStore() {
		return methodExecStore;
	}

	/**
	 * Gives access to the internal statistics on this context.
	 *
	 * @return The context statistics.
	 */
	public RestContextStats getStats() {
		return new RestContextStats(startTime, getMethodExecStore().getStatsByTotalTime());
	}

	/**
	 * Returns the resource class type.
	 *
	 * @return The resource class type.
	 */
	public Class<?> getResourceClass() {
		return resourceClass;
	}

	/**
	 * Returns the builder that created this context.
	 *
	 * @return The builder that created this context.
	 */
	public ServletConfig getBuilder() {
		return builder;
	}

	/**
	 * Returns the path matcher for this context.
	 *
	 * @return The path matcher for this context.
	 */
	public UrlPathMatcher getPathMatcher() {
		return pathMatcher;
	}

	/**
	 * Returns the root bean store for this context.
	 *
	 * @return The root bean store for this context.
	 */
	public BeanStore getRootBeanStore() {
		return rootBeanStore;
	}

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
				s = swaggerProvider.getSwagger(this, locale);
				if (s != null)
					swaggerCache.put(locale, s);
			} catch (Exception e) {
				throw toHttpException(e, InternalServerError.class);
			}
		}
		return Optional.ofNullable(s);
	}

	/**
	 * Finds the {@link RestOpArg} instances to handle resolving objects on the calls to the specified Java method.
	 *
	 * @param m The Java method being called.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link RestContext.Builder#beanStore()}.
	 * @return The array of resolvers.
	 */
	protected RestOpArg[] findRestOperationArgs(Method m, BeanStore beanStore) {

		MethodInfo mi = MethodInfo.of(m);
		List<ClassInfo> pt = mi.getParamTypes();
		RestOpArg[] ra = new RestOpArg[pt.size()];

		beanStore = BeanStore.of(beanStore, getResource());

		for (int i = 0; i < pt.size(); i++) {
			ParamInfo pi = mi.getParam(i);
			beanStore.addBean(ParamInfo.class, pi);
			for (Class<? extends RestOpArg> c : restOpArgs) {
				try {
					ra[i] = beanStore.creator(RestOpArg.class).type(c).run();
					if (ra[i] != null)
						break;
				} catch (ExecutableException e) {
					throw new InternalServerError(e.unwrap(), "Could not resolve parameter {0} on method {1}.", i, mi.getFullName());
				}
			}
			if (ra[i] == null)
				throw new InternalServerError("Could not resolve parameter {0} on method {1}.", i, mi.getFullName());
		}

		return ra;
	}

	/**
	 * Returns the list of methods to invoke before the actual REST method is called.
	 *
	 * @return The list of methods to invoke before the actual REST method is called.
	 */
	protected MethodList getPreCallMethods() {
		return preCallMethods;
	}

	/**
	 * Returns the list of methods to invoke after the actual REST method is called.
	 *
	 * @return The list of methods to invoke after the actual REST method is called.
	 */
	protected MethodList getPostCallMethods() {
		return postCallMethods;
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
	public void execute(Object resource, HttpServletRequest r1, HttpServletResponse r2) throws ServletException, IOException {

		// Must be careful not to bleed thread-locals.
		if (localSession.get() != null)
			System.err.println("WARNING:  Thread-local call object was not cleaned up from previous request.  " + this + ", thread=["+Thread.currentThread().getId()+"]");

		RestSession.Builder sb = createSession().resource(resource).req(r1).res(r2).logger(getCallLogger());

		try {

			if (initException != null)
				throw initException;

			// If the resource path contains variables (e.g. @Rest(path="/f/{a}/{b}"), then we want to resolve
			// those variables and push the servletPath to include the resolved variables.  The new pathInfo will be
			// the remainder after the new servletPath.
			// Only do this for the top-level resource because the logic for child resources are processed next.
			if (pathMatcher.hasVars() && parentContext == null) {
				String sp = sb.req().getServletPath();
				String pi = sb.getPathInfoUndecoded();
				UrlPath upi2 = UrlPath.of(pi == null ? sp : sp + pi);
				UrlPathMatch uppm = pathMatcher.match(upi2);
				if (uppm != null && ! uppm.hasEmptyVars()) {
					sb.pathVars(uppm.getVars());
					sb.req(
						new OverrideableHttpServletRequest(sb.req())
							.pathInfo(nullIfEmpty(urlDecode(uppm.getSuffix())))
							.servletPath(uppm.getPrefix())
					);
				} else {
					RestSession call = sb.build();
					call.debug(isDebug(call)).status(SC_NOT_FOUND).finish();
					return;
				}
			}

			// If this resource has child resources, try to recursively call them.
			Optional<RestChildMatch> childMatch = restChildren.findMatch(sb);
			if (childMatch.isPresent()) {
				UrlPathMatch uppm = childMatch.get().getPathMatch();
				RestContext rc = childMatch.get().getChildContext();
				if (! uppm.hasEmptyVars()) {
					sb.pathVars(uppm.getVars());
					HttpServletRequest childRequest = new OverrideableHttpServletRequest(sb.req())
						.pathInfo(nullIfEmpty(urlDecode(uppm.getSuffix())))
						.servletPath(sb.req().getServletPath() + uppm.getPrefix());
					rc.execute(rc.getResource(), childRequest, sb.res());
				} else {
					RestSession call = sb.build();
					call.debug(isDebug(call)).status(SC_NOT_FOUND).finish();
				}
				return;
			}

		} catch (Throwable e) {
			handleError(sb.build(), convertThrowable(e, InternalServerError.class));
		}

		RestSession s = sb.build();

		try {
			localSession.set(s);
			s.debug(isDebug(s));
			startCall(s);
			s.run();
		} catch (Throwable e) {
			handleError(s, convertThrowable(e, InternalServerError.class));
		} finally {
			try {
				s.finish();
				finishCall(s);
			} finally {
				localSession.remove();
			}
		}
	}

	private boolean isDebug(RestSession call) {
		return debugEnablement.isDebug(this, call.getRequest());
	}

	/**
	 * Returns the debug enablement bean for this context.
	 *
	 * @return The debug enablement bean for this context.
	 */
	public DebugEnablement getDebugEnablement() {
		return debugEnablement;
	}

	/**
	 * The main method for serializing POJOs passed in through the {@link RestResponse#setOutput(Object)} method or
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
	protected void processResponse(RestOpSession opSession) throws IOException, BasicHttpException, NotImplemented {

		// Loop until we find the correct processor for the POJO.
		int loops = 5;
		for (int i = 0; i < responseProcessors.length; i++) {
			int j = responseProcessors[i].process(opSession);
			if (j == FINISHED)
				return;
			if (j == RESTART) {
				if (loops-- < 0)
					throw new InternalServerError("Too many processing loops.");
				i = -1;  // Start over.
			}
		}

		Object output = opSession.getResponse().getOutput().orElse(null);
		throw new NotImplemented("No response processors found to process output of type ''{0}''", className(output));
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
	 * @param defaultThrowable The default throwable class to create.
	 * @return The converted thrown object.
	 */
	public Throwable convertThrowable(Throwable t, Class<?> defaultThrowable) {

		ClassInfo ci = ClassInfo.ofc(t);

		if (ci.is(InvocationTargetException.class)) {
			t = ((InvocationTargetException)t).getTargetException();
			ci = ClassInfo.ofc(t);
		}

		if (ci.is(HttpRuntimeException.class)) {
			t = ((HttpRuntimeException)t).getInner();
			ci = ClassInfo.ofc(t);
		}

		if (ci.is(ExecutableException.class)) {
			t = ((ExecutableException)t).getTargetException();
			ci = ClassInfo.ofc(t);
		}

		if (ci.hasAnnotation(Response.class))
			return t;

		if (ci.isChildOf(ParseException.class) || ci.is(InvalidDataConversionException.class))
			return new BadRequest(t);

		String n = className(t);

		if (n.contains("AccessDenied") || n.contains("Unauthorized"))
			return new Unauthorized(t);

		if (n.contains("Empty") || n.contains("NotFound"))
			return new NotFound(t);

		if (defaultThrowable == null)
			return new InternalServerError(t);

		ClassInfo eci = ClassInfo.ofc(defaultThrowable);

		try {
			ConstructorInfo cci = eci.getPublicConstructor(Throwable.class, String.class, Object[].class);
			if (cci != null)
	 			return toHttpException((Throwable)cci.invoke(t), InternalServerError.class);

			cci = eci.getPublicConstructor(Throwable.class);
			if (cci != null)
				return toHttpException((Throwable)cci.invoke(t), InternalServerError.class);

			System.err.println("WARNING:  Class '"+eci+"' does not have a public constructor that takes in valid arguments.");
			return new InternalServerError(t);
		} catch (ExecutableException e) {
			return new InternalServerError(e.getCause());
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
	protected void handleNotFound(RestSession session) throws Exception {
		String pathInfo = session.getPathInfo();
		String methodUC = session.getMethod();
		int rc = session.getStatus();
		String onPath = pathInfo == null ? " on no pathInfo"  : String.format(" on path '%s'", pathInfo);
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

		ClassInfo ci = ClassInfo.ofc(e);
		StatusCode r = ci.getLastAnnotation(StatusCode.class);
		if (r != null)
			if (r.value().length > 0)
				code = r.value()[0];

		BasicHttpException e2 = (e instanceof BasicHttpException ? (BasicHttpException)e : BasicHttpException.create(BasicHttpException.class).causedBy(e).statusCode(code).build());

		HttpServletRequest req = session.getRequest();
		HttpServletResponse res = session.getResponse();

		Throwable t = null;
		if (e instanceof HttpRuntimeException)
			t = ((HttpRuntimeException)e).getInner();
		if (t == null)
			t = e2.getRootCause();
		if (t != null) {
			Thrown t2 = thrown(t);
			res.setHeader(t2.getName(), t2.getValue());
		}

		try {
			res.setContentType("text/plain");
			res.setHeader("Content-Encoding", "identity");
			int statusCode = e2.getStatusLine().getStatusCode();
			res.setStatus(statusCode);

			PrintWriter w = null;
			try {
				w = res.getWriter();
			} catch (IllegalStateException x) {
				w = new PrintWriter(new OutputStreamWriter(res.getOutputStream(), UTF8));
			}

			try (PrintWriter w2 = w) {
				String httpMessage = RestUtils.getHttpResponseText(statusCode);
				if (httpMessage != null)
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
	 * Called at the start of a request to invoke all {@link HookEvent#START_CALL} methods.
	 *
	 * @param session The current request.
	 * @throws BasicHttpException If thrown from call methods.
	 */
	protected void startCall(RestSession session) throws BasicHttpException {
		for (MethodInvoker x : startCallMethods) {
			try {
				x.invoke(session.getBeanStore(), session.getContext().getResource());
			} catch (IllegalAccessException|IllegalArgumentException e) {
				throw InternalServerError.create().message("Error occurred invoking start-call method ''{0}''.", x.getFullName()).causedBy(e).build();
			} catch (InvocationTargetException e) {
				throw toHttpException(e.getTargetException(), InternalServerError.class);
			}
		}
	}

	/**
	 * Called during a request to invoke all {@link HookEvent#PRE_CALL} methods.
	 *
	 * @param session The current request.
	 * @throws Throwable If thrown from call methods.
	 */
	protected void preCall(RestOpSession session) throws Throwable {
		for (RestOpInvoker m : session.getContext().getPreCallMethods())
			m.invoke(session);
	}

	/**
	 * Called during a request to invoke all {@link HookEvent#POST_CALL} methods.
	 *
	 * @param session The current request.
	 * @throws Throwable If thrown from call methods.
	 */
	protected void postCall(RestOpSession session) throws Throwable {
		for (RestOpInvoker m : session.getContext().getPostCallMethods())
			m.invoke(session);
	}

	/**
	 * Called at the end of a request to invoke all {@link HookEvent#END_CALL} methods.
	 *
	 * <p>
	 * This is the very last method called in {@link #execute(Object, HttpServletRequest, HttpServletResponse)}.
	 *
	 * @param session The current request.
	 */
	protected void finishCall(RestSession session) {
		for (MethodInvoker x : endCallMethods) {
			try {
				x.invoke(session.getBeanStore(), session.getResource());
			} catch (Exception e) {
				logger.log(Level.WARNING, unwrap(e), ()->format("Error occurred invoking finish-call method ''{0}''.", x.getFullName()));
			}
		}
	}

	/**
	 * Called during servlet initialization to invoke all {@link HookEvent#POST_INIT} methods.
	 *
	 * @return This object.
	 * @throws ServletException Error occurred.
	 */
	public synchronized RestContext postInit() throws ServletException {
		if (initialized.get())
			return this;
		Object resource = getResource();
		MethodInfo mi = ClassInfo.of(getResource()).getMethod("setContext", RestContext.class);
		if (mi != null) {
			try {
				mi.accessible().invoke(resource, this);
			} catch (ExecutableException e) {
				throw new ServletException(e.unwrap());
			}
		}
		for (MethodInvoker x : postInitMethods) {
			try {
				x.invoke(beanStore, getResource());
			} catch (Exception e) {
				throw new ServletException(unwrap(e));
			}
		}
		restChildren.postInit();
		return this;
	}

	/**
	 * Called during servlet initialization to invoke all {@link HookEvent#POST_INIT_CHILD_FIRST} methods.
	 *
	 * @return This object.
	 * @throws ServletException Error occurred.
	 */
	public RestContext postInitChildFirst() throws ServletException {
		if (initialized.get())
			return this;
		restChildren.postInitChildFirst();
		for (MethodInvoker x : postInitChildFirstMethods) {
			try {
				x.invoke(beanStore, getResource());
			} catch (Exception e) {
				throw new ServletException(unwrap(e));
			}
		}
		initialized.set(true);
		return this;
	}

	/**
	 * Called during servlet destruction to invoke all {@link HookEvent#DESTROY} methods.
	 */
	protected void destroy() {
		for (MethodInvoker x : destroyMethods) {
			try {
				x.invoke(beanStore, getResource());
			} catch (Exception e) {
				getLogger().log(Level.WARNING, unwrap(e), ()->format("Error occurred invoking servlet-destroy method ''{0}''.", x.getFullName()));
			}
		}

		restChildren.destroy();
	}

	/**
	 * Returns the HTTP call for the current request.
	 *
	 * @return The HTTP call for the current request, never <jk>null</jk>?
	 * @throws InternalServerError If no active request exists on the current thread.
	 */
	public RestSession getLocalSession() {
		RestSession rc = localSession.get();
		if (rc == null)
			throw new InternalServerError("No active request on current thread.");
		return rc;
	}

	/**
	 * If the specified object is annotated with {@link Response}, this returns the response metadata about that object.
	 *
	 * @param o The object to check.
	 * @return The response metadata, or <jk>null</jk> if it wasn't annotated with {@link Response}.
	 */
	public ResponseBeanMeta getResponseBeanMeta(Object o) {
		if (o == null)
			return null;
		Class<?> c = o.getClass();
		ResponseBeanMeta rbm = responseBeanMetas.get(c);
		if (rbm == null) {
			rbm = ResponseBeanMeta.create(c, getAnnotations());
			if (rbm == null)
				rbm = ResponseBeanMeta.NULL;
			responseBeanMetas.put(c, rbm);
		}
		if (rbm == ResponseBeanMeta.NULL)
			return null;
		return rbm;
	}

	/**
	 * Returns the annotations applied to this context.
	 *
	 * @return The annotations applied to this context.
	 */
	public AnnotationWorkList getAnnotations() {
		return builder.getApplied();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helper methods
	//-----------------------------------------------------------------------------------------------------------------

	private Throwable unwrap(Throwable t) {
		if (t instanceof InvocationTargetException) {
			return ((InvocationTargetException)t).getTargetException();
		}
		return t;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	protected OMap properties() {
		return filteredMap()
			.a("allowBodyParam", allowBodyParam)
			.a("allowedMethodHeader", allowedMethodHeaders)
			.a("allowedMethodParams", allowedMethodParams)
			.a("allowedHeaderParams", allowedHeaderParams)
			.a("beanStore", beanStore)
			.a("clientVersionHeader", clientVersionHeader)
			.a("consumes", consumes)
			.a("defaultRequestHeaders", defaultRequestHeaders)
			.a("defaultResponseHeaders", defaultResponseHeaders)
			.a("fileFinder", fileFinder)
			.a("restOpArgs", restOpArgs)
			.a("partParser", partParser)
			.a("partSerializer", partSerializer)
			.a("produces", produces)
			.a("renderResponseStackTraces", renderResponseStackTraces)
			.a("responseProcessors", responseProcessors)
			.a("staticFiles", staticFiles)
			.a("swaggerProvider", swaggerProvider)
			.a("uriAuthority", uriAuthority)
			.a("uriContext", uriContext)
			.a("uriRelativity", uriRelativity)
			.a("uriResolution", uriResolution);
	}
}
