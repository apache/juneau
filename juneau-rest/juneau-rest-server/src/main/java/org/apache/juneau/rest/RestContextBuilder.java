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

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.http.HttpHeaders.*;
import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.ExceptionUtils.*;
import static org.apache.juneau.internal.ObjectUtils.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.parser.Parser.*;
import static org.apache.juneau.rest.HttpRuntimeException.*;
import static org.apache.juneau.rest.logging.RestLoggingDetail.*;
import static org.apache.juneau.serializer.Serializer.*;
import static java.util.Arrays.*;
import static java.util.Optional.*;
import static java.util.logging.Level.*;

import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.nio.charset.*;
import java.util.*;
import java.util.function.*;
import java.util.logging.*;
import java.util.stream.*;

import javax.servlet.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.config.*;
import org.apache.juneau.config.vars.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.html.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.mstat.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.args.*;
import org.apache.juneau.rest.logging.*;
import org.apache.juneau.rest.processors.*;
import org.apache.juneau.rest.vars.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.svl.vars.*;
import org.apache.juneau.utils.*;

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
 * 	<jk>public</jk> MyResource(RestContextBuilder builder) {
 * 			builder
 * 				.swaps(TemporalCalendarSwap.IsoLocalDateTime.<jk>class</jk>)
 * 				.set(<jsf>PARSER_debug</jsf>);
 * 	}
 *
 * 	<jc>// Option #2 - Use an INIT hook.</jc>
 * 	<ja>@RestHook</ja>(<jsf>INIT</jsf>)
 * 	<jk>public void</jk> init(RestContextBuilder builder) <jk>throws</jk> Exception {
 * 			builder
 * 				.swaps(TemporalCalendarSwap.IsoLocalDateTime.<jk>class</jk>)
 * 				.set(<jsf>PARSER_debug</jsf>);
 * 	}
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc RestContext}
 * </ul>
 */
@FluentSetters(ignore={"set"})
public class RestContextBuilder extends ContextBuilder implements ServletConfig {

	/**
	 * Represents a <jk>null</jk> value for the {@link Rest#builder()} annotation.
	 */
	@SuppressWarnings("javadoc")
	public static final class Null extends RestContextBuilder {
		protected Null(Class<?> resourceClass, RestContext parentContext, ServletConfig servletConfig) throws ServletException {
			super(resourceClass, parentContext, servletConfig);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// The following fields are meant to be modifiable.
	// They should not be declared final.
	// Read-only snapshots of these will be made in RestServletContext.
	//-----------------------------------------------------------------------------------------------------------------

	Supplier<?> resource;
	ServletContext servletContext;

	final ServletConfig inner;
	final Class<?> resourceClass;
	final RestContext parentContext;

	private DefaultClassList defaultClasses;
	private DefaultSettingsMap defaultSettings;

	private BeanStore beanStore;
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
	private JsonSchemaGeneratorBuilder jsonSchemaGenerator;
	private FileFinder.Builder fileFinder;
	private StaticFiles.Builder staticFiles;
	private HeaderList.Builder defaultRequestHeaders, defaultResponseHeaders;
	private NamedAttributeList defaultRequestAttributes;
	private RestOpArgList.Builder restOpArgs, hookMethodArgs;
	private DebugEnablement.Builder debugEnablement;
	private MethodList startCallMethods, endCallMethods, postInitMethods, postInitChildFirstMethods, destroyMethods, preCallMethods, postCallMethods;

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

	BeanRef<SwaggerProvider> swaggerProvider = BeanRef.of(SwaggerProvider.class);
	EncoderGroup.Builder encoders = EncoderGroup.create().add(IdentityEncoder.INSTANCE);
	SerializerGroup.Builder serializers = SerializerGroup.create();
	ParserGroup.Builder parsers = ParserGroup.create();

	List<Object> children = new ArrayList<>();

	/**
	 * Constructor.
	 *
	 * @param resourceClass The resource class.
	 * @param parentContext The parent context if this is a child of another resource.
	 * @param servletConfig The servlet config if available.
	 * @throws ServletException Initialization failed.
	 */
	protected RestContextBuilder(Class<?> resourceClass, RestContext parentContext, ServletConfig servletConfig) throws ServletException {
		try {
			type(RestContext.class);

			this.resourceClass = resourceClass;
			this.inner = servletConfig;
			this.parentContext = parentContext;

			// Pass-through default values.
			if (parentContext != null) {
				defaultClasses = parentContext.defaultClasses.copy();
				defaultSettings = parentContext.defaultSettings.copy();
			} else {
				defaultClasses = DefaultClassList.create();
				defaultSettings = DefaultSettingsMap.create();
			}

			beanStore = createBeanStore(resourceClass, parentContext)
				.build()
				.addBean(RestContextBuilder.class, this)
				.addBean(ServletConfig.class, ofNullable(servletConfig).orElse(this))
				.addBean(ServletContext.class, ofNullable(servletConfig).orElse(this).getServletContext());

			varResolver = createVarResolver(beanStore, resourceClass);

			VarResolver vr = varResolver.build();
			beanStore.addBean(VarResolver.class, vr);

			// Find our config file.  It's the last non-empty @RestResource(config).
			config = createConfig(beanStore, resourceClass);
			beanStore.addBean(Config.class, config);

			// Add our config file to the variable resolver.
			varResolver.bean(Config.class, config);
			vr = varResolver.build();
			beanStore.addBean(VarResolver.class, vr);

			// Add the servlet init parameters to our properties.
			if (servletConfig != null) {
				for (Enumeration<String> ep = servletConfig.getInitParameterNames(); ep.hasMoreElements();) {
					String p = ep.nextElement();
					String initParam = servletConfig.getInitParameter(p);
					set(vr.resolve(p), vr.resolve(initParam));
				}
			}

		} catch (Exception e) {
			throw new ServletException(e);
		}
	}

	@Override /* ContextBuilder */
	public RestContextBuilder copy() {
		throw new NoSuchMethodError("Not implemented.");
	}

	@Override /* BeanContextBuilder */
	public RestContext build() {
		try {
			return (RestContext) BeanStore.of(beanStore(), resource.get()).addBeans(RestContextBuilder.class, this).createBean(getType().orElse(RestContext.class));
		} catch (Exception e) {
			throw toHttpException(e, InternalServerError.class);
		}
	}

	/**
	 * Performs initialization on this builder.
	 *
	 * Calls all @RestHook(INIT) methods on the specified resource object.
	 *
	 * @param resource The resource bean. Required.
	 * @return This object.
	 * @throws ServletException If hook method calls failed.
	 */
	public RestContextBuilder init(Object resource) throws ServletException {
		this.resource = resource instanceof Supplier ? (Supplier<?>)resource : ()->resource;

		ClassInfo rci = ClassInfo.of(resourceClass);
		BeanStore bs = beanStore();

		runInitHooks(bs, resource());

		VarResolverSession vrs = varResolver().build().createSession();
		AnnotationWorkList al = rci.getAnnotationList(ContextApplyFilter.INSTANCE).getWork(vrs);
		apply(al);
		partSerializer().apply(al);
		partParser().apply(al);
		jsonSchemaGenerator().apply(al);

		return this;
	}

	private Supplier<?> resource() {
		if (resource == null)
			throw runtimeException("Resource not available.  init(Object) has not been called.");
		return resource;
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

	//-----------------------------------------------------------------------------------------------------------------
	// defaultClasses
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the default classes list.
	 *
	 * <p>
	 * This defines the implementation classes for a variety of bean types.
	 *
	 * <p>
	 * Default classes are inherited from the parent REST object.
	 * Typically used on the top-level {@link RestContextBuilder} to affect class types for that REST object and all children.
	 *
	 * <p>
	 * Modifying the default class list on this builder does not affect the default class list on the parent builder, but changes made
	 * here are inherited by child builders.
	 *
	 * @return The default classes list for this builder.
	 */
	public DefaultClassList defaultClasses() {
		return defaultClasses;
	}

	/**
	 * Adds default implementation classes to use.
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
	public RestContextBuilder defaultClasses(Class<?>...values) {
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
	 * Typically used on the top-level {@link RestContextBuilder} to affect settings for that REST object and all children.
	 *
	 * <p>
	 * Modifying the default settings map on this builder does not affect the default settings on the parent builder, but changes made
	 * here are inherited by child builders.
	 *
	 * @return The default settings map for this builder.
	 */
	public DefaultSettingsMap defaultSettings() {
		return defaultSettings;
	}

	/**
	 * Sets a default setting.
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
	public RestContextBuilder defaultSetting(String key, Object value) {
		defaultSettings().set(key, value);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// beanStore
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns access to the bean store being used by this builder.
	 *
	 * <p>
	 * Can be used to add more beans to the bean store.
	 *
	 * <p>
	 * The bean store is created by the constructor using the {@link #createBeanStore(Class,RestContext)} method and is initialized with the following beans:
	 * <ul>
	 * 	<li>{@link RestContextBuilder}
	 * 	<li>{@link ServletConfig}
	 * 	<li>{@link ServletContext}
	 * 	<li>{@link VarResolver}
	 * 	<li>{@link Config}
	 * </ul>
	 *
	 * @return The bean store being used by this builder.
	 */
	public final BeanStore beanStore() {
		return beanStore;
	}

	/**
	 * Sets the bean store for this builder.
	 *
	 * <p>
	 * The resolver used for resolving instances of child resources and various other beans including:
	 * <ul>
	 * 	<li>{@link RestLogger}
	 * 	<li>{@link SwaggerProvider}
	 * 	<li>{@link FileFinder}
	 * 	<li>{@link StaticFiles}
	 * </ul>
	 *
	 * <p>
	 * Note that the <c>SpringRestServlet</c> classes uses the <c>SpringBeanStore</c> class to allow for any
	 * Spring beans to be injected into your REST resources.
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc RestInjection}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is {@link BasicRestLogger}.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder beanStore(BeanStore value) {
		beanStore = value;
		return this;
	}

	/**
	 * Creates the bean store for this builder.
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
	 * @param resourceClass The resource class.
	 * @param parentContext The parent context if there is one.
	 * @return A new bean store.
	 */
	protected BeanStore.Builder createBeanStore(Class<?> resourceClass, RestContext parentContext) {

		// Create default builder.
		Value<BeanStore.Builder> v = Value.of(BeanStore.create().parent(parentContext == null ? null : parentContext.getRootBeanStore()));

		// Apply @Rest(beanStore).
		ClassInfo.of(resourceClass)
			.getAnnotations(Rest.class)
			.stream()
			.map(x -> x.beanStore())
			.filter(x -> x != BeanStore.Null.class)
			.reduce((x1,x2)->x2)
			.ifPresent(x -> v.get().implClass(x));

		// Replace with builder:  public static BeanStore.Builder createBeanStore()
		v.get().build()
			.beanCreateMethodFinder(BeanStore.Builder.class, resourceClass)
			.find("createBeanStore")
			.run(x -> v.set(x));

		// Replace with implementations:  public static BeanStore createBeanStore()
		v.get().build()
			.beanCreateMethodFinder(BeanStore.class, resourceClass)
			.find("createBeanStore")
			.run(x -> v.get().impl(x));

		return v.get();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// varResolver
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns access to the variable resolver builder.
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
	 * @return The var resolver builder.
	 */
	public final VarResolver.Builder varResolver() {
		return varResolver;
	}

	/**
	 * Creates the var resolver builder.
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
	 * @param resourceClass The resource class.
	 * @return A new var resolver builder.
	 */
	protected VarResolver.Builder createVarResolver(BeanStore beanStore, Class<?> resourceClass) {

		Value<VarResolver.Builder> v = Value.empty();

		// Get builder from:  public static VarResolver.Builder createVarResolver()
		BeanStore
			.of(beanStore)
			.beanCreateMethodFinder(VarResolver.Builder.class, resourceClass)
			.find("createVarResolver")
			.run(x -> v.set(x));

		// Get builder from bean store.
		if (v.isEmpty())
			beanStore.getBean(VarResolver.Builder.class).map(y -> y.copy()).ifPresent(x -> v.set(x));

		// Create default builder.
		if (v.isEmpty()) {
			v.set(
				VarResolver
					.create()
					.defaultVars()
					.vars(createVars(beanStore, resourceClass))
					.vars(FileVar.class)
					.bean(FileFinder.class, FileFinder.create().cp(resourceClass,null,true).build())
			);
		}

		// Get implementation from bean store.
		beanStore.getBean(VarResolver.class).ifPresent(x -> v.get().impl(x));

		// Get implementation from:  public static VarResolver createVarResolver()
		BeanStore
			.of(beanStore)
			.addBean(VarResolver.Builder.class, v.get())
			.beanCreateMethodFinder(VarResolver.class, resourceClass)
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
	 * @param resourceClass The resource class.
	 * @return A new var resolver variable list.
	 */
	protected VarList createVars(BeanStore beanStore, Class<?> resourceClass) {

		Value<VarList> v = Value.empty();

		// Get implementation from bean store.
		beanStore.getBean(VarList.class).map(x -> x.copy()).ifPresent(x -> v.set(x));

		// Create default.
		if (v.isEmpty()) {
			v.set(
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
		}

		// Get implementation from:  public static VarList createVars()
		BeanStore
			.of(beanStore, resourceClass)
			.addBean(VarList.class, v.get())
			.beanCreateMethodFinder(VarList.class, resourceClass)
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
	 * @return The external config file for this resource.  Never <jk>null</jk>.
	 */
	public final Config config() {
		return config;
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
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder config(Config config) {
		this.config = config;
		return this;
	}

	/**
	 * Creates the config for this builder.
	 *
	 * @param beanStore The bean store to use for creating the config.
	 * @param resourceClass The resource class.
	 * @return A new bean store.
	 * @throws Exception If bean store could not be instantiated.
	 */
	protected Config createConfig(BeanStore beanStore, Class<?> resourceClass) throws Exception {

		Value<Config> v = Value.empty();

		// Get implementation from:  public static Config createConfig()
		beanStore
			.beanCreateMethodFinder(Config.class, resourceClass)
			.find("createConfig")
			.run(x -> v.set(x));

		// Get implementation from bean store.
		if (v.isEmpty())
			beanStore.getBean(Config.class).ifPresent(x -> v.set(x));

		// Find our config file.  It's the last non-empty @RestResource(config).
		VarResolver vr = beanStore.getBean(VarResolver.class).orElseThrow(()->runtimeException("VarResolver not found."));
		String cf = ClassInfo.of(resourceClass)
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
			ConfigBuilder cb = Config.create().varResolver(vr);
			if (! cf.isEmpty())
				cb.name(cf);
			v.set(cb.build());
		}

		return v.get();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// logger
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the logger to use for the REST resource.
	 *
	 * @return The logger to use for the REST resource.
	 * @throws RuntimeException If {@link #init(Object)} has not been called.
	 */
	public final Logger logger() {
		if (logger == null)
			logger = createLogger(beanStore(), resource());
		return logger;
	}

	/**
	 * Sets the logger to use for the REST resource.
	 *
	 * <p>
	 * If not specified, the logger used is created by {@link #createLogger(BeanStore, Supplier)}.
	 *
	 * @param value The logger to use for the REST resource.
	 * @return This object.
	 */
	public final RestContextBuilder logger(Logger value) {
		logger = value;
		return this;
	}

	/**
	 * Instantiates logger for this REST resource.
	 *
	 * <p>
	 * Instantiates based on the following logic:
	 * <ul>
	 * 	<li>Looks for a static or non-static <c>createLogger()</> method that returns <c>{@link Logger}</c> on the
	 * 		resource class with any of the following arguments:
	 * 		<ul>
	 * 			<li>{@link RestContext}
	 * 			<li>{@link BeanStore}
	 * 			<li>Any {@doc RestInjection injected beans}.
	 * 		</ul>
	 * 	<li>Resolves it via the bean store registered in this context.
	 * 	<li>Instantiates via <c>Logger.<jsm>getLogger</jsm>(<jv>resource</jv>.getClass().getName())</c>.
	 * </ul>
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link RestContextBuilder#beanStore()}.
	 * @return The logger for this REST resource.
	 */
	protected Logger createLogger(BeanStore beanStore, Supplier<?> resource) {

		Value<Logger> v = Value.empty();
		Object r = resource.get();

		beanStore.getBean(Logger.class).ifPresent(x -> v.set(x));

		if (v.isEmpty())
			v.set(Logger.getLogger(className(r)));

		BeanStore
			.of(beanStore, r)
			.addBean(Logger.class, v.get())
			.beanCreateMethodFinder(Logger.class, r)
			.find("createLogger")
			.run(x -> v.set(x));

		return v.get();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// thrownStore
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the builder for the {@link ThrownStore} object in the REST context.
	 *
	 * @return The builder for the {@link ThrownStore} object in the REST context.
	 */
	public final ThrownStore.Builder thrownStore() {
		if (thrownStore == null)
			thrownStore = createThrownStore(beanStore(), resource(), parentContext);
		return thrownStore;
	}

	/**
	 * Instantiates the thrown exception store for this REST resource.
	 *
	 * <p>
	 * Instantiates based on the following logic:
	 * <ul>
	 * 	<li>Looks for a static or non-static <c>createThrownStore()</> method that returns <c>{@link ThrownStore}</c> on the
	 * 		resource class with any of the following arguments:
	 * 		<ul>
	 * 			<li>{@link RestContext}
	 * 			<li>{@link BeanStore}
	 * 			<li>Any {@doc RestInjection injected beans}.
	 * 		</ul>
	 * 	<li>Resolves it via the bean store registered in this context.
	 * 	<li>Returns {@link ThrownStore#GLOBAL}.
	 * </ul>
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param parent
	 * 	The parent context if the REST bean was registered via {@link Rest#children()}.
	 * 	<br>Will be <jk>null</jk> if the bean is a top-level resource.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link RestContextBuilder#beanStore()}.
	 * @return The stack trace store for this REST resource.
	 */
	protected ThrownStore.Builder createThrownStore(BeanStore beanStore, Supplier<?> resource, RestContext parent) {

		Value<ThrownStore.Builder> v = Value.empty();
		Object r = resource.get();

		beanStore.getBean(ThrownStore.Builder.class).map(x -> x.copy()).ifPresent(x->v.set(x));

		BeanStore
			.of(beanStore, r)
			.addBean(ThrownStore.Builder.class, v.get())
			.beanCreateMethodFinder(ThrownStore.Builder.class, r)
			.find("createThrownStore")
			.run(x -> v.set(x));

		if (v.isEmpty()) {
			v.set(
				ThrownStore
					.create()
					.beanStore(beanStore)
					.impl(parent == null ? null : parent.getThrownStore())
			);
		}

		// Specify the implementation class if its set as a default.
		defaultClasses().get(ThrownStore.class).ifPresent(x -> v.get().implClass(x));

		BeanStore
			.of(beanStore, r)
			.addBean(ThrownStore.Builder.class, v.get())
			.beanCreateMethodFinder(ThrownStore.class, r)
			.find("createThrownStore")
			.run(x -> v.get().impl(x));

		return v.get();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// methodExecStore
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the builder for the {@link MethodExecStore} object in the REST context.
	 *
	 * @return The builder for the {@link MethodExecStore} object in the REST context.
	 */
	public final MethodExecStore.Builder methodExecStore() {
		if (methodExecStore == null)
			methodExecStore = createMethodExecStore(beanStore(), resource());
		return methodExecStore;
	}

	/**
	 * Instantiates the method execution statistics store for this REST resource.
	 *
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @return The stack trace store for this REST resource.
	 */
	protected MethodExecStore.Builder createMethodExecStore(BeanStore beanStore, Supplier<?> resource) {

		Value<MethodExecStore.Builder> v = Value.empty();
		Object r = resource.get();

		beanStore.getBean(MethodExecStore.Builder.class).map(x -> x.copy()).ifPresent(x->v.set(x));

		BeanStore
			.of(beanStore, r)
			.addBean(MethodExecStore.Builder.class, v.get())
			.beanCreateMethodFinder(MethodExecStore.Builder.class, r)
			.find("createMethodExecStore")
			.run(x -> v.set(x));

		if (v.isEmpty()) {
			v.set(
				MethodExecStore
					.create()
					.beanStore(beanStore)
			);
		}

		// Specify the implementation class if its set as a default.
		defaultClasses().get(MethodExecStore.class).ifPresent(x -> v.get().implClass(x));

		BeanStore
			.of(beanStore, r)
			.addBean(MethodExecStore.Builder.class, v.get())
			.beanCreateMethodFinder(MethodExecStore.class, r)
			.find("createMethodExecStore")
			.run(x -> v.get().impl(x));

		return v.get();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// messages
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the builder for the {@link Messages} object in the REST context.
	 *
	 * @return The builder for the {@link Messages} object in the REST context.
	 */
	public final Messages.Builder messages() {
		if (messages == null)
			messages = createMessages(beanStore(), resource());
		return messages;
	}

	/**
	 * Instantiates the messages for this REST object.
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
	 * 	<li class='link'>{@doc RestMessages}
	 * </ul>
	 *
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @return The messages builder for this REST object.
	 */
	protected Messages.Builder createMessages(BeanStore beanStore, Supplier<?> resource) {

		Value<Messages.Builder> v = Value.empty();
		Object r = resource.get();

		BeanStore
			.of(beanStore, r)
			.beanCreateMethodFinder(Messages.Builder.class, r)
			.find("createMessages")
			.run(x -> v.set(x));

		if (v.isEmpty()) {
			v.set(
				Messages
					.create(r.getClass())
			);
		}

		BeanStore
			.of(beanStore, r)
			.addBean(Messages.Builder.class, v.get())
			.beanCreateMethodFinder(Messages.class, r)
			.find("createMessages")
			.run(x -> v.get().impl(x));

		return v.get();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// responseProcessors
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the builder for the {@link ResponseProcessorList} object in the REST context.
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
	 * 		<jk>public int</jk> process(RestCall <jv>call</jv>) <jk>throws</jk> IOException {
	 *
	 * 				RestResponse <jv>res</jv> = <jv>call</jv>.getRestResponse();
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
	 * 		<jk>public</jk> MyResource(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			<jv>builder</jv>.responseProcessors(MyResponseProcessor.<jk>class</jk>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
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
	 * @return The builder for the {@link ResponseProcessorList} object in the REST context.
	 */
	public final ResponseProcessorList.Builder responseProcessors() {
		if (responseProcessors == null)
			responseProcessors = createResponseProcessors(beanStore(), resource());
		return responseProcessors;
	}

	/**
	 * Instantiates the response handlers for this REST resource.
	 *
	 * <p>
	 * Instantiates based on the following logic:
	 * <ul>
	 * 	<li>Looks for response processors set via any of the following:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#responseProcessors()}
	 * 			<li>{@link Rest#responseProcessors()}.
	 * 		</ul>
	 * 	<li>Looks for a static or non-static <c>createResponseProcessors()</> method that returns <c>{@link ResponseProcessor}[]</c> on the
	 * 		resource class with any of the following arguments:
	 * 		<ul>
	 * 			<li>{@link RestContext}
	 * 			<li>{@link BeanStore}
	 * 			<li>Any {@doc RestInjection injected beans}.
	 * 		</ul>
	 * 	<li>Resolves it via the bean store registered in this context.
	 * 	<li>Instantiates a <c>ResponseProcessor[0]</c>.
	 * </ul>
	 *
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @return The response handler builder for this REST resource.
	 */
	protected ResponseProcessorList.Builder createResponseProcessors(BeanStore beanStore, Supplier<?> resource) {

		Value<ResponseProcessorList.Builder> v = Value.empty();
		Object r = resource.get();

		beanStore.getBean(ResponseProcessorList.Builder.class).map(x -> x.copy()).ifPresent(x -> v.set(x));

		if (v.isEmpty()) {
			v.set(
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
		}

		BeanStore
			.of(beanStore, r)
			.addBean(ResponseProcessorList.Builder.class, v.get())
			.beanCreateMethodFinder(ResponseProcessorList.Builder.class, r)
			.find("createResponseProcessors")
			.run(x -> v.set(x));

		beanStore.getBean(ResponseProcessorList.class).ifPresent(x -> v.get().impl(x));

		BeanStore
			.of(beanStore, r)
			.addBean(ResponseProcessorList.Builder.class, v.get())
			.beanCreateMethodFinder(ResponseProcessorList.class, r)
			.find("createResponseProcessors")
			.run(x -> v.get().impl(x));

		return v.get();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// callLogger
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the builder for the {@link RestLogger} object in the REST context.
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
	 * 		<jk>public</jk> MyResource(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			<jv>builder</jv>.callLogger(MyLogger.<jk>class</jk>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Registered via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
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
	 * 	<li class='link'>{@doc RestLoggingAndDebugging}
	 * 	<li class='ja'>{@link Rest#callLogger()}
	 * </ul>
	 *
	 * @return The builder for the {@link RestLogger} object in the REST context.
	 * @throws RuntimeException If {@link #init(Object)} has not been called.
	 */
	public final RestLogger.Builder callLogger() {
		if (callLogger == null)
			callLogger = createCallLogger(beanStore(), resource());
		return callLogger;
	}

	/**
	 * Instantiates the call logger this REST resource.
	 *
	 * <p>
	 * Instantiates based on the following logic:
	 * <ul>
	 * 	<li>Returns the resource class itself is an instance of RestLogger.
	 * 	<li>Looks for REST call logger set via any of the following:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#callLogger()}
	 * 			<li>{@link Rest#callLogger()}.
	 * 		</ul>
	 * 	<li>Looks for a static or non-static <c>createCallLogger()</> method that returns {@link RestLogger} on the
	 * 		resource class with any of the following arguments:
	 * 		<ul>
	 * 			<li>{@link RestContext}
	 * 			<li>{@link BeanStore}
	 * 			<li>{@link BasicFileFinder}
	 * 			<li>Any {@doc RestInjection injected beans}.
	 * 		</ul>
	 * 	<li>Resolves it via the bean store registered in this context.
	 * 	<li>Instantiates a {@link BasicFileFinder}.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContextBuilder#callLogger()}
	 * </ul>
	 *
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @return The call logger builder for this REST resource.
	 */
	protected RestLogger.Builder createCallLogger(BeanStore beanStore, Supplier<?> resource) {

		Value<RestLogger.Builder> v = Value.empty();
		Object r = resource.get();

		beanStore.getBean(RestLogger.Builder.class).map(x -> x.copy()).ifPresent(x-> v.set(x));

		BeanStore
			.of(beanStore, r)
			.addBean(RestLogger.Builder.class, v.get())
			.beanCreateMethodFinder(RestLogger.Builder.class, r)
			.find("createCallLogger")
			.run(x -> v.set(x));

		if (v.isEmpty()) {
			v.set(
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
		}

		if (r instanceof RestLogger)
			v.get().impl((RestLogger)r);

		beanStore.getBean(RestLogger.class).ifPresent(x-> v.get().impl(x));

		// Specify the implementation class if its set as a default.
		defaultClasses().get(RestLogger.class).ifPresent(x -> v.get().implClass(x));

		BeanStore
			.of(beanStore, r)
			.addBean(RestLogger.Builder.class, v.get())
			.beanCreateMethodFinder(RestLogger.class, r)
			.find("createCallLogger")
			.run(x -> v.get().impl(x));

		return v.get();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// partSerializer
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the part serializer builder for this context.
	 *
	 * @return The part serializer builder for this context.
	 */
	public final HttpPartSerializer.Creator partSerializer() {
		if (partSerializer == null)
			partSerializer = createPartSerializer(beanStore(), resource());
		return partSerializer;
	}

	/**
	 * Instantiates the HTTP part serializer for this REST resource.
	 *
	 * <p>
	 * Instantiates based on the following logic:
	 * <ul>
	 * 	<li>Returns the resource class itself is an instance of {@link HttpPartSerializer}.
	 * 	<li>Looks for part serializer set via any of the following:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#partSerializer()}
	 * 			<li>{@link Rest#partSerializer()}.
	 * 		</ul>
	 * 	<li>Looks for a static or non-static <c>createPartSerializer()</> method that returns <c>{@link HttpPartSerializer}</c> on the
	 * 		resource class with any of the following arguments:
	 * 		<ul>
	 * 			<li>{@link RestContext}
	 * 			<li>{@link BeanStore}
	 * 			<li>Any {@doc RestInjection injected beans}.
	 * 		</ul>
	 * 	<li>Resolves it via the bean store registered in this context.
	 * 	<li>Instantiates an {@link OpenApiSerializer}.
	 * </ul>
	 *
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @return The HTTP part serializer for this REST resource.
	 */
	protected HttpPartSerializer.Creator createPartSerializer(BeanStore beanStore, Supplier<?> resource) {

		Value<HttpPartSerializer.Creator> v = Value.empty();
		Object r = resource.get();

		// Get builder from bean store.
		beanStore.getBean(HttpPartSerializer.Creator.class).map(x -> x.copy()).ifPresent(x -> v.set(x));

		// Create default.
		if (v.isEmpty()) {
			v.set(
				HttpPartSerializer
					.creator()
					.type(OpenApiSerializer.class)
			);
		}

		// Set implementation if in bean store.
		beanStore.getBean(HttpPartSerializer.class).ifPresent(x -> v.get().impl(x));

		// Set default type.
		defaultClasses.get(HttpPartSerializer.class).ifPresent(x -> v.get().type(x));

		// Call:  public [static] HttpPartSerializer.Creator createPartSerializer(<anything-in-bean-store>)
		BeanStore
			.of(beanStore, r)
			.addBean(HttpPartSerializer.Creator.class, v.get())
			.beanCreateMethodFinder(HttpPartSerializer.Creator.class, r)
			.find("createPartSerializer")
			.run(x -> v.set(x));

		// Call:  public [static] HttpPartSerializer createPartSerializer(<anything-in-bean-store>)
		BeanStore
			.of(beanStore, r)
			.addBean(HttpPartSerializer.Creator.class, v.get())
			.beanCreateMethodFinder(HttpPartSerializer.class, r)
			.find("createPartSerializer")
			.run(x -> v.get().impl(x));

		return v.get();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// partParser
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the part parser builder for this context.
	 *
	 * @return The part parser builder for this context.
	 */
	public final HttpPartParser.Creator partParser() {
		if (partParser == null)
			partParser = createPartParser(beanStore(), resource());
		return partParser;
	}

	/**
	 * Instantiates the HTTP part parser for this REST resource.
	 *
	 * <p>
	 * Instantiates based on the following logic:
	 * <ul>
	 * 	<li>Returns the resource class itself is an instance of {@link HttpPartParser}.
	 * 	<li>Looks for part parser set via any of the following:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#partParser()}
	 * 			<li>{@link Rest#partParser()}.
	 * 		</ul>
	 * 	<li>Looks for a static or non-static <c>createPartParser()</> method that returns <c>{@link HttpPartParser}</c> on the
	 * 		resource class with any of the following arguments:
	 * 		<ul>
	 * 			<li>{@link RestContext}
	 * 			<li>{@link BeanStore}
	 * 			<li>Any {@doc RestInjection injected beans}.
	 * 		</ul>
	 * 	<li>Resolves it via the bean store registered in this context.
	 * 	<li>Instantiates an {@link OpenApiParser}.
	 * </ul>
	 *
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @return The HTTP part serializer for this REST resource.
	 */
	protected HttpPartParser.Creator createPartParser(BeanStore beanStore, Supplier<?> resource) {

		Value<HttpPartParser.Creator> v = Value.empty();
		Object r = resource.get();

		// Get builder from bean store.
		beanStore.getBean(HttpPartParser.Creator.class).map(x -> x.copy()).ifPresent(x -> v.set(x));

		// Create default.
		if (v.isEmpty()) {
			v.set(
				HttpPartParser
					.creator()
					.type(OpenApiParser.class)
			);
		}

		// Set implementation if in bean store.
		beanStore.getBean(HttpPartParser.class).ifPresent(x -> v.get().impl(x));

		// Set default type.
		defaultClasses.get(HttpPartParser.class).ifPresent(x -> v.get().type(x));

		// Call:  public [static] HttpPartParser.Creator createPartParser(<anything-in-bean-store>)
		BeanStore
			.of(beanStore, r)
			.addBean(HttpPartParser.Creator.class, v.get())
			.beanCreateMethodFinder(HttpPartParser.Creator.class, r)
			.find("createPartParser")
			.run(x -> v.set(x));

		// Call:  public [static] HttpPartParser createPartParser(<anything-in-bean-store>)
		BeanStore
			.of(beanStore, r)
			.addBean(HttpPartParser.Creator.class, v.get())
			.beanCreateMethodFinder(HttpPartParser.class, r)
			.find("createPartParser")
			.run(x -> v.get().impl(x));

		return v.get();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// jsonSchemaGenerator
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the JSON schema generator builder for this context.
	 *
	 * @return The JSON schema generator builder for this context.
	 */
	public final JsonSchemaGeneratorBuilder jsonSchemaGenerator() {
		if (jsonSchemaGenerator == null)
			jsonSchemaGenerator = createJsonSchemaGenerator(beanStore(), resource());
		return jsonSchemaGenerator;
	}

	/**
	 * Instantiates the JSON schema generator for this REST resource.
	 *
	 * <p>
	 * Instantiates based on the following logic:
	 * <ul>
	 * 	<li>Looks for a static or non-static <c>createJsonSchemaGenerator()</> method that returns <c>{@link JsonSchemaGenerator}</c> on the
	 * 		resource class with any of the following arguments:
	 * 		<ul>
	 * 			<li>{@link RestContext}
	 * 			<li>{@link BeanStore}
	 * 			<li>Any {@doc RestInjection injected beans}.
	 * 		</ul>
	 * 	<li>Resolves it via the bean store registered in this context.
	 * 	<li>Instantiates a new {@link JsonSchemaGenerator} using the property store of this context..
	 * </ul>
	 *
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @return The JSON schema generator builder for this REST resource.
	 */
	protected JsonSchemaGeneratorBuilder createJsonSchemaGenerator(BeanStore beanStore, Supplier<?> resource) {

		Value<JsonSchemaGeneratorBuilder> v = Value.empty();
		Object r = resource.get();

		beanStore.getBean(JsonSchemaGeneratorBuilder.class).map(x -> x.copy()).ifPresent(x -> v.set(x));

		if (v.isEmpty()) {
			v.set(
				JsonSchemaGenerator.create()
			);
		}

		beanStore.getBean(JsonSchemaGenerator.class).ifPresent(x -> v.get().impl(x));

		BeanStore
			.of(beanStore, r)
			.addBean(JsonSchemaGeneratorBuilder.class, v.get())
			.beanCreateMethodFinder(JsonSchemaGeneratorBuilder.class, r)
			.find("createJsonSchemaGenerator")
			.run(x -> v.set(x));

		BeanStore
			.of(beanStore, r)
			.addBean(JsonSchemaGeneratorBuilder.class, v.get())
			.beanCreateMethodFinder(JsonSchemaGenerator.class, r)
			.find("createJsonSchemaGenerator")
			.run(x -> v.get().impl(x));

		return v.get();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// fileFinder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the file finder builder for this context.
	 *
	 * @return The file finder builder for this context.
	 */
	public FileFinder.Builder fileFinder() {
		if (fileFinder == null)
			fileFinder = createFileFinder(beanStore(), resource());
		return fileFinder;
	}

	/**
	 * Instantiates the file finder for this REST resource.
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
	 * The file finder is instantiated via the {@link RestContextBuilder#createFileFinder(BeanStore,Supplier)} method which in turn instantiates
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
	 * 		<jk>public</jk> MyResource(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
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
	 * 		<jk>public void</jk> init(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
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
	 * 			<li>{@link RestContextBuilder#fileFinder()}
	 * 			<li>{@link Rest#fileFinder()}.
	 * 		</ul>
	 * 	<li>Resolves it via the {@link RestContextBuilder#beanStore() bean store} registered in this context (including Spring beans if using SpringRestServlet).
	 * 	<li>Looks for file finder default setting.
	 * </ul>
	 *
	 * <p>
	 * Your REST class can also implement a create method called <c>createFileFinder()</c> to instantiate your own
	 * file finder.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bpcode w800'>
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
	 * 	<li>Any {@doc RestInjection injected bean} types.  Use {@link Optional} arguments for beans that may not exist.
	 * </ul>
	 *
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @return The file finder for this REST resource.
	 */
	protected FileFinder.Builder createFileFinder(BeanStore beanStore, Supplier<?> resource) {

		Value<FileFinder.Builder> v = Value.empty();
		Object r = resource.get();

		beanStore.getBean(FileFinder.Builder.class).map(x -> x.copy()).ifPresent(x -> v.set(x));

		if (v.isEmpty()) {
			v.set(
				FileFinder
					.create()
					.beanStore(beanStore)
					.dir("static")
					.dir("htdocs")
					.cp(r.getClass(), "htdocs", true)
					.cp(r.getClass(), "/htdocs", true)
					.caching(1_000_000)
					.exclude("(?i).*\\.(class|properties)")
			);
		}

		beanStore.getBean(FileFinder.class).ifPresent(x -> v.get().impl(x));

		if (r instanceof FileFinder)
			v.get().impl((FileFinder)r);

		defaultClasses.get(FileFinder.class).ifPresent(x -> v.get().type(x));

		BeanStore
			.of(beanStore, r)
			.addBean(FileFinder.Builder.class, v.get())
			.beanCreateMethodFinder(FileFinder.Builder.class, r)
			.find("createFileFinder")
			.run(x -> v.set(x));

		BeanStore
			.of(beanStore, r)
			.addBean(FileFinder.Builder.class, v.get())
			.beanCreateMethodFinder(FileFinder.class, r)
			.find("createFileFinder")
			.run(x -> v.get().impl(x));

		return v.get();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// staticFiles
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the static files builder for this context.
	 *
	 * @return The static files builder for this context.
	 */
	public StaticFiles.Builder staticFiles() {
		if (staticFiles == null)
			staticFiles = createStaticFiles(beanStore(), resource());
		return staticFiles;
	}


	/**
	 * Instantiates the static files finder for this REST resource.
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
	 * The static file finder is instantiated via the {@link RestContextBuilder#createStaticFiles(BeanStore,Supplier)} method which in turn instantiates
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
	 * 		<jk>public</jk> MyResource(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
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
	 * 		<jk>public void</jk> init(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
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
	 * 			<li>{@link RestContextBuilder#staticFiles()}
	 * 			<li>{@link Rest#staticFiles()}.
	 * 		</ul>
	 * 	<li>Looks for a static or non-static <c>createStaticFiles()</> method that returns {@link StaticFiles} on the
	 * 		resource class with any of the following arguments:
	 * 		<ul>
	 * 			<li>{@link RestContext}
	 * 			<li>{@link BeanStore}
	 * 			<li>{@link BasicFileFinder}
	 * 			<li>Any {@doc RestInjection injected beans}.
	 * 		</ul>
	 * 	<li>Resolves it via the bean store registered in this context.
	 * 	<li>Looks for value in default static files setting.
	 * 	<li>Instantiates a {@link BasicStaticFiles}.
	 * </ul>
	 *
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @return The file finder for this REST resource.
	 */
	protected StaticFiles.Builder createStaticFiles(BeanStore beanStore, Supplier<?> resource) {

		Value<StaticFiles.Builder> v = Value.empty();
		Object r = resource.get();

		beanStore.getBean(StaticFiles.Builder.class).map(x -> x.copy()).ifPresent(x -> v.set(x));

		if (v.isEmpty()) {
			v.set(
				StaticFiles
					.create()
					.beanStore(beanStore)
					.dir("static")
					.dir("htdocs")
					.cp(r.getClass(), "htdocs", true)
					.cp(r.getClass(), "/htdocs", true)
					.caching(1_000_000)
					.exclude("(?i).*\\.(class|properties)")
					.headers(cacheControl("max-age=86400, public"))
			);
		}

		beanStore.getBean(StaticFiles.class).ifPresent(x -> v.get().impl(x));

		if (r instanceof StaticFiles)
			v.get().impl((StaticFiles)r);

		defaultClasses.get(StaticFiles.class).ifPresent(x -> v.get().type(x));

		BeanStore
			.of(beanStore, r)
			.addBean(StaticFiles.Builder.class, v.get())
			.beanCreateMethodFinder(StaticFiles.Builder.class, r)
			.find("createStaticFiles")
			.run(x -> v.set(x));

		BeanStore
			.of(beanStore, r)
			.addBean(StaticFiles.Builder.class, v.get())
			.beanCreateMethodFinder(StaticFiles.class, r)
			.find("createStaticFiles")
			.run(x -> v.get().impl(x));

		return v.get();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// defaultRequestHeaders
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the builder for the default request headers in the REST context.
	 *
	 * @return The builder for the default request headers in the REST context.
	 */
	public final HeaderList.Builder defaultRequestHeaders() {
		if (defaultRequestHeaders == null)
			defaultRequestHeaders = createDefaultRequestHeaders(beanStore(), resource());
		return defaultRequestHeaders;
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
	 * 		<jk>public</jk> MyResource(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
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
	 * 		<jk>public void</jk> init(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
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
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder defaultRequestHeaders(Header...values) {
		defaultRequestHeaders().setDefault(values);
		return this;
	}

	/**
	 * Specifies a default <c>Accept</c> header value if not specified on a request.
	 *
	 * @param value
	 * 	The default value of the <c>Accept</c> header.
	 * 	<br>Ignored if <jk>null</jk> or empty.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder defaultAccept(String value) {
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
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder defaultContentType(String value) {
		if (isNotEmpty(value))
			defaultRequestHeaders(contentType(value));
		return this;
	}

	/**
	 * Instantiates the default request headers for this REST object.
	 *
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @return The default request headers for this REST object.
	 */
	protected HeaderList.Builder createDefaultRequestHeaders(BeanStore beanStore, Supplier<?> resource) {

		Value<HeaderList.Builder> v = Value.empty();
		Object r = resource.get();

		beanStore.getBean("RestContext.defaultRequestHeaders", HeaderList.Builder.class).map(x -> x.copy()).ifPresent(x -> v.set(x));

		if (v.isEmpty())
			v.set(HeaderList.create());

		beanStore.getBean("RestContext.defaultRequestHeaders", HeaderList.class).ifPresent(x -> v.get().impl(x));

		BeanStore
			.of(beanStore, r)
			.addBean(HeaderList.Builder.class, v.get())
			.beanCreateMethodFinder(HeaderList.Builder.class, r)
			.find("createDefaultRequestHeaders")
			.run(x -> v.set(x));

		BeanStore
			.of(beanStore, r)
			.addBean(HeaderList.Builder.class, v.get())
			.beanCreateMethodFinder(HeaderList.class, r)
			.find("createDefaultRequestHeaders")
			.run(x -> v.get().impl(x));

		return v.get();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// defaultResponseHeaders
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the builder for the default response headers in the REST context.
	 *
	 * @return The builder for the default response headers in the REST context.
	 */
	public final HeaderList.Builder defaultResponseHeaders() {
		if (defaultResponseHeaders == null)
			defaultResponseHeaders = createDefaultResponseHeaders(beanStore(), resource());
		return defaultResponseHeaders;
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
	 * 		<jk>public</jk> MyResource(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
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
	 * 		<jk>public void</jk> init(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
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
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder defaultResponseHeaders(Header...values) {
		defaultResponseHeaders().setDefault(values);
		return this;
	}

	/**
	 * Instantiates the default response headers for this REST object.
	 *
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @return The default response headers for this REST object.
	 */
	protected HeaderList.Builder createDefaultResponseHeaders(BeanStore beanStore, Supplier<?> resource) {

		Value<HeaderList.Builder> v = Value.empty();
		Object r = resource.get();

		beanStore.getBean("RestContext.defaultResponseHeaders", HeaderList.Builder.class).map(x -> x.copy()).ifPresent(x -> v.set(x));

		if (v.isEmpty())
			v.set(HeaderList.create());

		beanStore.getBean("RestContext.defaultResponseHeaders", HeaderList.class).ifPresent(x -> v.get().impl(x));

		BeanStore
			.of(beanStore, r)
			.addBean(HeaderList.Builder.class, v.get())
			.beanCreateMethodFinder(HeaderList.Builder.class, r)
			.find("createDefaultResponseHeaders")
			.run(x -> v.set(x));

		BeanStore
			.of(beanStore, r)
			.addBean(HeaderList.Builder.class, v.get())
			.beanCreateMethodFinder(HeaderList.class, r)
			.find("createDefaultResponseHeaders")
			.run(x -> v.get().impl(x));

		return v.get();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// defaultRequestAttributes
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the builder for the default requests attributes in the REST context.
	 *
	 * @return The builder for the default request attributer object in the REST context.
	 */
	public final NamedAttributeList defaultRequestAttributes() {
		if (defaultRequestAttributes == null)
			defaultRequestAttributes = createDefaultRequestAttributes(beanStore(), resource());
		return defaultRequestAttributes;
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
	 * 		<jk>public</jk> MyResource(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
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
	 * 		<jk>public void</jk> init(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
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
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder defaultRequestAttributes(NamedAttribute...values) {
		defaultRequestAttributes().appendUnique(values);
		return this;
	}

	/**
	 * Instantiates the default response headers for this REST object.
	 *
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @return The default response headers for this REST object.
	 */
	protected NamedAttributeList createDefaultRequestAttributes(BeanStore beanStore, Supplier<?> resource) {

		Value<NamedAttributeList> v = Value.empty();
		Object r = resource.get();

		beanStore.getBean("RestContext.defaultRequestAttributes", NamedAttributeList.class).map(x -> x.copy()).ifPresent(x -> v.set(x));

		if (v.isEmpty())
			v.set(NamedAttributeList.create());

		BeanStore
			.of(beanStore, r)
			.addBean(NamedAttributeList.class, v.get())
			.beanCreateMethodFinder(NamedAttributeList.class, r)
			.find("createDefaultRequestAttributes")
			.run(x -> v.set(x));

		return v.get();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// restOpArgs
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the builder for the default requests attributes in the REST context.
	 *
	 * @return The builder for the default request attributer object in the REST context.
	 */
	public final RestOpArgList.Builder restOpArgs() {
		if (restOpArgs == null)
			restOpArgs = createRestOpArgs(beanStore(), resource());
		return restOpArgs;
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
	 * 		<jk>public</jk> MyResource(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			<jv>builder</jv>.restOpArgs(MyRestParam.<jk>class</jk>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Registered via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
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
	 * @return This object (for method chaining).
	 * @throws IllegalArgumentException if any class does not extend from {@link RestOpArg}.
	 */
	@FluentSetter
	public RestContextBuilder restOpArgs(Class<?>...values) {
		restOpArgs().add(assertClassArrayArgIsType("values", RestOpArg.class, values));
		return this;
	}

	/**
	 * Instantiates the REST method parameter resolvers for this REST resource.
	 *
	 * <p>
	 * Instantiates based on the following logic:
	 * <ul>
	 * 	<li>Looks for REST op args set via any of the following:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#restOpArgs(Class...)}/{@link RestContextBuilder#restOpArgs(Class...)}
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
	 * 	The REST servlet or bean that this context defines.
	 * @return The REST method parameter resolvers for this REST resource.
	 */
	protected RestOpArgList.Builder createRestOpArgs(BeanStore beanStore, Supplier<?> resource) {

		Value<RestOpArgList.Builder> v = Value.empty();
		Object r = resource.get();

		beanStore.getBean("RestContext.restOpArgs", RestOpArgList.Builder.class).map(x -> x.copy()).ifPresent(x -> v.set(x));

		if (v.isEmpty()) {
			v.set(
				RestOpArgList
					.of(
						AttributeArg.class,
						BodyArg.class,
						ConfigArg.class,
						FormDataArg.class,
						HasFormDataArg.class,
						HasQueryArg.class,
						HeaderArg.class,
						HttpServletRequestArg.class,
						HttpServletResponseArg.class,
						InputStreamArg.class,
						InputStreamParserArg.class,
						LocaleArg.class,
						MessagesArg.class,
						MethodArg.class,
						OutputStreamArg.class,
						ParserArg.class,
						PathArg.class,
						QueryArg.class,
						ReaderArg.class,
						ReaderParserArg.class,
						RequestAttributesArg.class,
						RequestBeanArg.class,
						RequestBodyArg.class,
						RequestFormDataArg.class,
						RequestHeadersArg.class,
						RequestPathArg.class,
						RequestQueryArg.class,
						ResourceBundleArg.class,
						ResponseBeanArg.class,
						ResponseHeaderArg.class,
						ResponseStatusArg.class,
						RestContextArg.class,
						RestRequestArg.class,
						ServetInputStreamArg.class,
						ServletOutputStreamArg.class,
						SwaggerArg.class,
						TimeZoneArg.class,
						UriContextArg.class,
						UriResolverArg.class,
						WriterArg.class,
						DefaultArg.class
					)
			);
		}

		beanStore.getBean("RestContext.restOpArgs", RestOpArgList.class).ifPresent(x -> v.get().impl(x));

		BeanStore
			.of(beanStore, r)
			.addBean(RestOpArgList.Builder.class, v.get())
			.beanCreateMethodFinder(RestOpArgList.Builder.class, r)
			.find("createRestOpArgs")
			.run(x -> v.set(x));

		BeanStore
			.of(beanStore, r)
			.addBean(RestOpArgList.Builder.class, v.get())
			.beanCreateMethodFinder(RestOpArgList.class, r)
			.find("createRestOpArgs")
			.run(x -> v.get().impl(x));

		return v.get();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// hookMethodArgs
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the builder for the default requests attributes in the REST context.
	 *
	 * @return The builder for the default request attributer object in the REST context.
	 */
	public final RestOpArgList.Builder hookMethodArgs() {
		if (hookMethodArgs == null)
			hookMethodArgs = createHookMethodArgs(beanStore(), resource());
		return hookMethodArgs;
	}

	/**
	 * Instantiates the hook method parameter resolvers for this REST resource.
	 *
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @return The REST method parameter resolvers for this REST resource.
	 */
	protected RestOpArgList.Builder createHookMethodArgs(BeanStore beanStore, Supplier<?> resource) {

		Value<RestOpArgList.Builder> v = Value.empty();
		Object r = resource.get();

		beanStore.getBean("RestContext.hookMethodArgs", RestOpArgList.Builder.class).map(x -> x.copy()).ifPresent(x -> v.set(x));

		if (v.isEmpty()) {
			v.set(
				RestOpArgList
					.of(
						ConfigArg.class,
						HeaderArg.class,
						HttpServletRequestArg.class,
						HttpServletResponseArg.class,
						InputStreamArg.class,
						LocaleArg.class,
						MessagesArg.class,
						MethodArg.class,
						OutputStreamArg.class,
						ReaderArg.class,
						ResourceBundleArg.class,
						RestContextArg.class,
						RestRequestArg.class,
						ServetInputStreamArg.class,
						ServletOutputStreamArg.class,
						TimeZoneArg.class,
						WriterArg.class,
						DefaultArg.class
					)
			);
		}

		beanStore.getBean("RestContext.hookMethodArgs", RestOpArgList.class).ifPresent(x -> v.get().impl(x));

		BeanStore
			.of(beanStore, r)
			.addBean(RestOpArgList.Builder.class, v.get())
			.beanCreateMethodFinder(RestOpArgList.Builder.class, r)
			.find("createHookMethodArgs")
			.run(x -> v.set(x));

		BeanStore
			.of(beanStore, r)
			.addBean(RestOpArgList.Builder.class, v.get())
			.beanCreateMethodFinder(RestOpArgList.class, r)
			.find("createHookMethodArgs")
			.run(x -> v.get().impl(x));

		return v.get();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// debugEnablement
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the builder for the debug enablement bean in the REST context.
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
	 * @return The builder for the debug enablement bean in the REST context.
	 */
	public final DebugEnablement.Builder debugEnablement() {
		if (debugEnablement == null)
			debugEnablement = createDebugEnablement(beanStore(), resource());
		return debugEnablement;
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
	public RestContextBuilder debugDefault(Enablement value) {
		defaultSettings().set("RestContext.debugDefault", value);
		return this;
	}

	/**
	 * Specifies the debug level on this REST resource.
	 *
	 * @param value The value for this setting.
	 * @return This object.
	 */
	public RestContextBuilder debug(Enablement value) {
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
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder debugOn(String value) {
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
	 * Instantiates the debug enablement bean for this REST object.
	 *
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @return The debug enablement bean for this REST object.
	 */
	protected DebugEnablement.Builder createDebugEnablement(BeanStore beanStore, Supplier<?> resource) {

		Value<DebugEnablement.Builder> v = Value.empty();
		Object r = resource.get();

		beanStore.getBean(DebugEnablement.Builder.class).map(x -> x.copy()).ifPresent(x -> v.set(x));

		if (v.isEmpty()) {
			DebugEnablement.Builder b = DebugEnablement
				.create()
				.beanStore(beanStore);

			// Default debug enablement if not overridden at class/method level.
			Enablement debugDefault = defaultSettings.get(Enablement.class, "RestContext.debugDefault").orElse(isDebug() ? Enablement.ALWAYS : Enablement.NEVER);
			b.defaultEnable(debugDefault);

			// Gather @RestOp(debug) settings.
			for (MethodInfo mi : ClassInfo.ofProxy(r).getPublicMethods()) {
				mi
					.getAnnotationGroupList(RestOp.class)
					.getValues(String.class, "debug")
					.stream()
					.filter(y->!y.isEmpty())
					.findFirst()
					.ifPresent(x -> b.enable(Enablement.fromString(x), mi.getFullName()));
			}

			v.set(b);
		}

		if (r instanceof DebugEnablement)
			v.get().impl((DebugEnablement)r);

		defaultClasses.get(DebugEnablement.class).ifPresent(x -> v.get().type(x));

		beanStore.getBean(DebugEnablement.class).ifPresent(x -> v.get().impl(x));

		BeanStore
			.of(beanStore, r)
			.addBean(DebugEnablement.Builder.class, v.get())
			.beanCreateMethodFinder(DebugEnablement.Builder.class, r)
			.find("createDebugEnablement")
			.run(x -> v.set(x));

		BeanStore
			.of(beanStore, r)
			.addBean(DebugEnablement.Builder.class, v.get())
			.beanCreateMethodFinder(DebugEnablement.class, r)
			.find("createDebugEnablement")
			.run(x -> v.get().impl(x));

		return v.get();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// HookEvent.START_CALL methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the list of methods that get executed at the start of an HTTP call.
	 *
	 * @return The list of methods that get executed at the start of an HTTP call.
	 */
	public final MethodList startCallMethods() {
		if (startCallMethods == null)
			startCallMethods = createStartCallMethods(beanStore(), resource());
		return startCallMethods;
	}

	/**
	 * Instantiates the list of {@link HookEvent#START_CALL} methods.
	 *
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @return The default response headers for this REST object.
	 */
	protected MethodList createStartCallMethods(BeanStore beanStore, Supplier<?> resource) {

		Value<MethodList> v = Value.of(getHookMethods(resource, HookEvent.START_CALL));
		Object r = resource.get();

		BeanStore
			.of(beanStore, r)
			.addBean(MethodList.class, v.get())
			.beanCreateMethodFinder(MethodList.class, r)
			.find("createStartCallMethods")
			.run(x -> v.set(x));

		return v.get();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// HookEvent.END_CALL methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the list of methods that get executed at the end of an HTTP call.
	 *
	 * @return The list of methods that get executed at the end of an HTTP call.
	 */
	public final MethodList endCallMethods() {
		if (endCallMethods == null)
			endCallMethods = createEndCallMethods(beanStore(), resource());
		return endCallMethods;
	}

	/**
	 * Instantiates the list of {@link HookEvent#END_CALL} methods.
	 *
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @return The default response headers for this REST object.
	 */
	protected MethodList createEndCallMethods(BeanStore beanStore, Supplier<?> resource) {

		Value<MethodList> v = Value.of(getHookMethods(resource, HookEvent.END_CALL));
		Object r = resource.get();

		BeanStore
			.of(beanStore, r)
			.addBean(MethodList.class, v.get())
			.beanCreateMethodFinder(MethodList.class, r)
			.find("createEndCallMethods")
			.run(x -> v.set(x));

		return v.get();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// HookEvent.POST_INIT methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the list of methods that get executed immediately after initialization.
	 *
	 * @return The list of methods that get executed immediately after initialization.
	 */
	public final MethodList postInitMethods() {
		if (postInitMethods == null)
			postInitMethods = createPostInitMethods(beanStore(), resource());
		return postInitMethods;
	}

	/**
	 * Instantiates the list of {@link HookEvent#POST_INIT} methods.
	 *
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @return The default response headers for this REST object.
	 */
	protected MethodList createPostInitMethods(BeanStore beanStore, Supplier<?> resource) {

		Value<MethodList> v = Value.of(getHookMethods(resource, HookEvent.POST_INIT));
		Object r = resource.get();

		BeanStore
			.of(beanStore, r)
			.addBean(MethodList.class, v.get())
			.beanCreateMethodFinder(MethodList.class, r)
			.find("createPostInitMethods")
			.run(x -> v.set(x));

		return v.get();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// HookEvent.POST_INIT_CHILD_FIRST methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the list of methods that get executed immediately after initialization in child-to-parent order.
	 *
	 * @return The list of methods that get executed immediately after initialization in child-to-parent order.
	 */
	public final MethodList postInitChildFirstMethods() {
		if (postInitChildFirstMethods == null)
			postInitChildFirstMethods = createPostInitChildFirstMethods(beanStore(), resource());
		return postInitChildFirstMethods;
	}

	/**
	 * Instantiates the list of {@link HookEvent#POST_INIT_CHILD_FIRST} methods.
	 *
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @return The default response headers for this REST object.
	 */
	protected MethodList createPostInitChildFirstMethods(BeanStore beanStore, Supplier<?> resource) {

		Value<MethodList> v = Value.of(getHookMethods(resource, HookEvent.POST_INIT_CHILD_FIRST));
		Object r = resource.get();

		BeanStore
			.of(beanStore, r)
			.addBean(MethodList.class, v.get())
			.beanCreateMethodFinder(MethodList.class, r)
			.find("createPostInitChildFirstMethods")
			.run(x -> v.set(x));

		return v.get();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// HookEvent.DESTROY methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the list of methods that get executed during servlet destruction.
	 *
	 * @return The list of methods that get executed during servlet destruction.
	 */
	public final MethodList destroyMethods() {
		if (destroyMethods == null)
			destroyMethods = createDestroyMethods(beanStore(), resource());
		return destroyMethods;
	}
	/**
	 * Instantiates the list of {@link HookEvent#DESTROY} methods.
	 *
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @return The default response headers for this REST object.
	 */
	protected MethodList createDestroyMethods(BeanStore beanStore, Supplier<?> resource) {

		Value<MethodList> v = Value.of(getHookMethods(resource, HookEvent.DESTROY));
		Object r = resource.get();

		BeanStore
			.of(beanStore, r)
			.addBean(MethodList.class, v.get())
			.beanCreateMethodFinder(MethodList.class, r)
			.find("createDestroyMethods")
			.run(x -> v.set(x));

		return v.get();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// HookEvent.PRE_CALL methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the list of methods that gets called immediately before the <ja>@RestOp</ja> annotated method gets called..
	 *
	 * @return The list of methods that gets called immediately before the <ja>@RestOp</ja> annotated method gets called..
	 */
	public final MethodList preCallMethods() {
		if (preCallMethods == null)
			preCallMethods = createPreCallMethods(beanStore(), resource());
		return preCallMethods;
	}

	/**
	 * Instantiates the list of {@link HookEvent#PRE_CALL} methods.
	 *
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @return The default response headers for this REST object.
	 */
	protected MethodList createPreCallMethods(BeanStore beanStore, Supplier<?> resource) {

		Value<MethodList> v = Value.of(getHookMethods(resource, HookEvent.PRE_CALL));
		Object r = resource.get();

		BeanStore
			.of(beanStore, r)
			.addBean(MethodList.class, v.get())
			.beanCreateMethodFinder(MethodList.class, r)
			.find("createPreCallMethods")
			.run(x -> v.set(x));

		return v.get();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// HookEvent.POST_CALL methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the list of methods that gets called immediately after the <ja>@RestOp</ja> annotated method gets called..
	 *
	 * @return The list of methods that gets called immediately after the <ja>@RestOp</ja> annotated method gets called..
	 */
	public final MethodList postCallMethods() {
		if (postCallMethods == null)
			postCallMethods = createPostCallMethods(beanStore(), resource());
		return postCallMethods;
	}

	/**
	 * Instantiates the list of {@link HookEvent#POST_CALL} methods.
	 *
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @return The default response headers for this REST object.
	 */
	protected MethodList createPostCallMethods(BeanStore beanStore, Supplier<?> resource) {

		Value<MethodList> v = Value.of(getHookMethods(resource, HookEvent.POST_CALL));
		Object r = resource.get();

		BeanStore
			.of(beanStore, r)
			.addBean(MethodList.class, v.get())
			.beanCreateMethodFinder(MethodList.class, r)
			.find("createPostCallMethods")
			.run(x -> v.set(x));

		return v.get();
	}

	private int TODO;

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
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder allowedHeaderParams(String value) {
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
	 * 		<jk>public</jk> MyResource(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			<jv>builder</jv>.allowedMethodHeaders(<js>"PATCH"</js>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
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
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder allowedMethodHeaders(String value) {
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
	 * Note that per the {@doc ExtRFC2616.section9 HTTP specification}, special care should
	 * be taken when allowing non-safe (<c>POST</c>, <c>PUT</c>, <c>DELETE</c>) methods to be invoked through GET requests.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Option #1 - Defined via annotation.</jc>
	 * 	<ja>@Rest</ja>(allowedMethodParams=<js>"HEAD,OPTIONS,PUT"</js>)
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			<jv>builder</jv>.allowedMethodParams(<js>"HEAD,OPTIONS,PUT"</js>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder builder) <jk>throws</jk> Exception {
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
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder allowedMethodParams(String value) {
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
	 * 		<jk>public</jk> MyResource(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			<jv>builder</jv>.clientVersionHeader(<js>"Client-Version"</js>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
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
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder clientVersionHeader(String value) {
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
	 * 		<jk>public</jk> MyResource(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			<jv>builder</jv>.defaultCharset(<js>"US-ASCII"</js>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
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
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder defaultCharset(Charset value) {
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
	 * 		<jk>public</jk> MyResource(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			<jv>builder</jv>.disableBodyParam();
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
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
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder disableBodyParam() {
		return disableBodyParam(true);
	}

	/**
	 * Disable body URL parameter.
	 *
	 * <p>
	 * Same as {@link #disableBodyParam()} but allows you to set it as a boolean value.
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder disableBodyParam(boolean value) {
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
	 * 		<jk>public</jk> MyResource(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			<jv>builder</jv>.maxInput(<js>"10M"</js>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
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
	 * 	<li class='jm'>{@link RestOpContextBuilder#maxInput(String)}
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
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder maxInput(String value) {
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
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder renderResponseStackTraces(boolean value) {
		renderResponseStackTraces = value;
		return this;
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Render response stack traces in responses.
	 *
	 * <p>
	 * Shortcut for calling <code>renderResponseStackTraces(<jk>true</jk>)</code>.
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder renderResponseStackTraces() {
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
	 * 		<jk>public</jk> MyResource(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			<jv>builder</jv>.uriAuthority(<js>"http://localhost:10000"</js>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
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
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder uriAuthority(String value) {
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
	 * 		<jk>public</jk> MyResource(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			<jv>builder</jv>.uriContext(<js>"/foo"</js>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
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
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder uriContext(String value) {
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
	 * 		<jk>public</jk> MyResource(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			<jv>builder</jv>.uriRelativity(<jsf>PATH_INFO</jsf>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
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
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder uriRelativity(UriRelativity value) {
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
	 * 		<jk>public</jk> MyResource(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			<jv>builder</jv>.uriResolution(<jsf>ABSOLUTE</jsf>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
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
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder uriResolution(UriResolution value) {
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
	 * 	<li class='link'>{@doc RestSerializers}
	 * </ul>
	 *
	 * @return The serializer group builder for this context builder.
	 */
	public SerializerGroup.Builder getSerializers() {
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
	 * 	<li class='link'>{@doc RestParsers}
	 * </ul>
	 *
	 * @return The parser group builder for this context builder.
	 */
	public ParserGroup.Builder getParsers() {
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
	 * 	<li class='link'>{@doc RestEncoders}
	 * </ul>
	 *
	 * @return The encoder group builder for this context builder.
	 */
	public EncoderGroup.Builder getEncoders() {
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
	 * relative to the ascendant path UNLESS you use the {@link RestContextBuilder#child(String, Object)} method to register it.
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
	 * 		<jk>public</jk> MyResource(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
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
	 * 		<jk>public void</jk> init(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
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
	 * 			<li><code><jk>public</jk> T(RestContextBuilder)</code>
	 * 			<li><code><jk>public</jk> T()</code>
	 * 		</ul>
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc RestChildren}
	 * 	<li class='ja'>{@link Rest#children()}
	 * </ul>
	 *
	 * @param values
	 * 	The values to add to this setting.
	 * 	<br>Objects can be any of the specified types:
	 * 	<ul>
	 * 		<li>A class that has a constructor described above.
	 * 		<li>An instantiated resource object (such as a servlet object instantiated by a servlet container).
	 * 		<li>An instance of {@link RestChild} containing an instantied resource object and a subpath.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder children(Object...values) {
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
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder child(String path, Object child) {
		children.add(new RestChild(path, child));
		return this;
	}

	@Override
	public RestContextBuilder type(Class<? extends Context> value) {
		super.type(value);
		return this;
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Parser listener.
	 *
	 * <p>
	 * Specifies the parser listener class to use for listening to non-fatal parsing errors.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Parser#PARSER_listener}
	 * </ul>
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder parserListener(Class<? extends ParserListener> value) {
		if (value != ParserListener.Null.class)
			set(PARSER_listener, value);
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
	 * 		<jk>public</jk> MyResource(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			<jv>builder</jv>.path(<js>"/myResource"</js>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
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
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder path(String value) {
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
	 * 	<li>{@link RestChildrenBuilder} - The builder for the object.
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
	 * 		<jk>public void</jk> init(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
	 * 			<jv>builder</jv>.restChildrenClass(MyRestChildren.<jk>class</jk>);
	 * 		}
	 * 	}
	 * </p>
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder restChildrenClass(Class<? extends RestChildren> value) {
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
	 * 	<li>{@link RestOpContextBuilder} - The builder for the object.
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
	 * 		<jk>public</jk> MyRestOperationContext(RestOpContextBuilder <jv>builder</jv>, Optional&lt;AnInjectedFooSupplier&gt; <jv>fooSupplier</jv>) {
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
	 * 		<jk>public void</jk> init(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
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
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder restOpContextClass(Class<? extends RestOpContext> value) {
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
	 * 	<li>{@link RestOperationsBuilder} - The builder for the object.
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
	 * 		<jk>public void</jk> init(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
	 * 			<jv>builder</jv>.restMethodsClass(MyRestOperations.<jk>class</jk>);
	 * 		}
	 * 	}
	 * </p>
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder restOperationsClass(Class<? extends RestOperations> value) {
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
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_listener}
	 * </ul>
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder serializerListener(Class<? extends SerializerListener> value) {
		if (value != SerializerListener.Null.class)
			set(SERIALIZER_listener, value);
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
	 * 		<jk>public</jk> MyResource(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			<jv>builder</jv>.produces(<jk>false</jk>, <js>"application/json"</js>)
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
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
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder produces(MediaType...values) {
		if (produces == null)
			produces = new ArrayList<>(Arrays.asList(values));
		else
			produces.addAll(Arrays.asList(values));
		return this;
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
	 * 		<jk>public</jk> MyResource(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			<jv>builder</jv>.consumes(<jk>false</jk>, <js>"application/json"</js>)
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
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
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder consumes(MediaType...values) {
		if (consumes == null)
			consumes = new ArrayList<>(Arrays.asList(values));
		else
			consumes.addAll(Arrays.asList(values));
		return this;
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
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder swaggerProvider(Class<? extends SwaggerProvider> value) {
		swaggerProvider.type(value);
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
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder swaggerProvider(SwaggerProvider value) {
		swaggerProvider.value(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestContextBuilder apply(AnnotationWorkList work) {
		super.apply(work);
		return this;
	}

	@Override /* ContextBuilder */
	public RestContextBuilder set(String name, Object value) {
		super.set(name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public RestContextBuilder set(String name) {
		super.set(name);
		return this;
	}

	@Override /* ContextBuilder */
	public RestContextBuilder set(Map<String,Object> properties) {
		super.set(properties);
		return this;
	}

	// <FluentSetters>

	@Override /* GENERATED - ContextBuilder */
	public RestContextBuilder add(Map<String,Object> properties) {
		super.add(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestContextBuilder addTo(String name, Object value) {
		super.addTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestContextBuilder appendTo(String name, Object value) {
		super.appendTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestContextBuilder apply(ContextProperties copyFrom) {
		super.apply(copyFrom);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestContextBuilder applyAnnotations(java.lang.Class<?>...fromClasses) {
		super.applyAnnotations(fromClasses);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestContextBuilder applyAnnotations(Method...fromMethods) {
		super.applyAnnotations(fromMethods);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestContextBuilder debug() {
		super.debug();
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestContextBuilder prependTo(String name, Object value) {
		super.prependTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestContextBuilder putAllTo(String name, Object value) {
		super.putAllTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestContextBuilder putTo(String name, String key, Object value) {
		super.putTo(name, key, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestContextBuilder removeFrom(String name, Object value) {
		super.removeFrom(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestContextBuilder unset(String name) {
		super.unset(name);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder annotations(Annotation...values) {
		super.annotations(values);
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
