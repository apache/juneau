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

import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.parser.Parser.*;
import static org.apache.juneau.rest.RestContext.*;
import static org.apache.juneau.rest.HttpRuntimeException.*;
import static org.apache.juneau.serializer.Serializer.*;
import static java.util.Arrays.*;

import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.nio.charset.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import javax.servlet.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.config.*;
import org.apache.juneau.config.vars.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.exception.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.logging.*;
import org.apache.juneau.rest.reshandlers.*;
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
 * Provides access to the following initialized resources:
 * <ul>
 * 	<li>{@link #getConfig()} - The external configuration for this resource.
 * 	<li>{@link #getVarResolverBuilder()} - The variable resolver for this resource.
 * </ul>
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
public class RestContextBuilder extends BeanContextBuilder implements ServletConfig {

	final ServletConfig inner;
	final Class<?> resourceClass;
	final RestContext parentContext;
	final BeanStore beanStore;

	//-----------------------------------------------------------------------------------------------------------------
	// The following fields are meant to be modifiable.
	// They should not be declared final.
	// Read-only snapshots of these will be made in RestServletContext.
	//-----------------------------------------------------------------------------------------------------------------

	Supplier<?> resource;
	ServletContext servletContext;

	Config config;
	VarResolverBuilder varResolverBuilder;

	RestContextBuilder(Optional<RestContext> parentContext, Optional<ServletConfig> servletConfig, Class<?> resourceClass, Optional<Object> resource) throws ServletException {
		try {

			this.resourceClass = resourceClass;
			this.inner = servletConfig.orElse(null);
			this.parentContext = parentContext.orElse(null);

			ClassInfo rci = ClassInfo.of(resourceClass);

			// Default values.
			partSerializer(OpenApiSerializer.class);
			partParser(OpenApiParser.class);
			encoders(IdentityEncoder.INSTANCE);
			responseHandlers(
				ReaderHandler.class,
				InputStreamHandler.class,
				DefaultHandler.class
			);

			// Pass-through default values.
			if (parentContext.isPresent()) {
				RestContext pc = parentContext.get();
				ContextProperties pcp = pc.getContextProperties();
				set(REST_callLoggerDefault, pcp.get(REST_callLoggerDefault).orElse(null));
				set(REST_debugDefault, pcp.get(REST_debugDefault).orElse(null));
				set(REST_staticFilesDefault, pcp.get(REST_staticFilesDefault).orElse(null));
				set(REST_fileFinderDefault, pcp.get(REST_fileFinderDefault).orElse(null));
			}

			beanStore = createBeanStore(parentContext, resource);
			beanStore.addBean(RestContextBuilder.class, this);
			beanStore.addBean(ServletConfig.class, servletConfig.orElse(this));
			beanStore.addBean(ServletContext.class, servletConfig.orElse(this).getServletContext());

			varResolverBuilder = new VarResolverBuilder()
				.defaultVars()
				.vars(ConfigVar.class)
				.vars(FileVar.class)
				.bean(FileFinder.class, FileFinder.create().cp(resourceClass,null,true).build());

			VarResolver vr = varResolverBuilder.build();
			beanStore.addBean(VarResolver.class, vr);

			// Find our config file.  It's the last non-empty @RestResource(config).
			config = createConfig(resourceClass, beanStore);
			beanStore.addBean(Config.class, config);

			// Add our config file to the variable resolver.
			varResolverBuilder.bean(Config.class, config);
			vr = varResolverBuilder.build();
			beanStore.addBean(VarResolver.class, vr);

			// Add the servlet init parameters to our properties.
			if (servletConfig.isPresent()) {
				ServletConfig sc = servletConfig.get();
				for (Enumeration<String> ep = sc.getInitParameterNames(); ep.hasMoreElements();) {
					String p = ep.nextElement();
					String initParam = sc.getInitParameter(p);
					set(vr.resolve(p), vr.resolve(initParam));
				}
			}

			applyAnnotations(rci.getAnnotationList(ConfigAnnotationFilter.INSTANCE), vr.createSession());

		} catch (Exception e) {
			throw new ServletException(e);
		}
	}

	@Override /* BeanContextBuilder */
	public RestContext build() {
		try {
			Class<? extends RestContext> c = getContextProperties().getClass(REST_contextClass, RestContext.class).orElse(getDefaultImplClass());
			return BeanStore.of(beanStore, resource.get()).addBeans(RestContextBuilder.class, this).createBean(c);
		} catch (Exception e) {
			throw toHttpException(e, InternalServerError.class);
		}
	}

	/**
	 * Specifies the default implementation class if not specified via {@link #REST_contextClass}.
	 *
	 * @return The default implementation class if not specified via {@link #REST_contextClass}.
	 */
	protected Class<? extends RestContext> getDefaultImplClass() {
		return RestContext.class;
	}

	/**
	 * Creates the bean store for this builder.
	 *
	 * @param parentContext The parent context if there is one.
	 * @param resource The resource object if it's instantiated at this time.
	 * @return A new bean store.
	 * @throws Exception If bean store could not be instantiated.
	 */
	protected BeanStore createBeanStore(Optional<RestContext> parentContext, Optional<Object> resource) throws Exception {
		BeanStore x = null;
		if (resource.isPresent()) {
			Object r = resource.get();
			x = BeanStore
				.of(parentContext.isPresent() ? parentContext.get().getRootBeanStore() : null, r)
				.beanCreateMethodFinder(BeanStore.class, resource)
				.find("createBeanStore")
				.run();
		}
		if (x == null && parentContext.isPresent()) {
			x = parentContext.get().getRootBeanStore();
		}
		return BeanStore.of(x, resource.orElse(null));
	}

	/**
	 * Creates the config for this builder.
	 *
	 * @param resourceClass The resource class.
	 * @param beanStore The bean store to use for creating the config.
	 * @return A new bean store.
	 * @throws Exception If bean store could not be instantiated.
	 */
	protected Config createConfig(Class<?> resourceClass, BeanStore beanStore) throws Exception {
		ClassInfo rci = ClassInfo.of(resourceClass);
		Config x = null;
		Object o = resource == null ? null : resource.get();
		if (o instanceof Config)
			x = (Config)o;
		if (x == null)
			x = beanStore.getBean(Config.class).orElse(null);

		// Find our config file.  It's the last non-empty @RestResource(config).
		String configPath = "";
		for (AnnotationInfo<Rest> r : rci.getAnnotationInfos(Rest.class))
			if (! r.getAnnotation().config().isEmpty())
				configPath = r.getAnnotation().config();
		VarResolver vr = beanStore.getBean(VarResolver.class).orElseThrow(()->new RuntimeException("VarResolver not found."));
		String cf = vr.resolve(configPath);

		if ("SYSTEM_DEFAULT".equals(cf))
			x = Config.getSystemDefault();

		if (x == null) {
			ConfigBuilder cb = Config.create().varResolver(vr);
			if (! cf.isEmpty())
				cb.name(cf);
			x = cb.build();
		}
		return x;
	}

	/*
	 * Calls all @RestHook(INIT) methods on the specified resource object.
	 */
	RestContextBuilder init(Object resource) throws ServletException {
		this.resource = resource instanceof Supplier ? (Supplier<?>)resource : ()->resource;

		ClassInfo rci = ClassInfo.ofProxy(resource);

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
			List<ClassInfo> paramTypes = m.getParamTypes();

			List<ClassInfo> missing = beanStore.getMissingParamTypes(paramTypes);
			if (!missing.isEmpty())
				throw new RestServletException("Could not call @RestHook(INIT) method {0}.{1}.  Could not find prerequisites: {2}.", m.getDeclaringClass().getSimpleName(), m.getSignature(), missing.stream().map(x->x.getSimpleName()).collect(Collectors.joining(",")));

			try {
				m.invoke(resource, beanStore.getParams(paramTypes));
			} catch (Exception e) {
				throw new RestServletException(e, "Exception thrown from @RestHook(INIT) method {0}.{1}.", m.getDeclaringClass().getSimpleName(), m.getSignature());
			}
		}
		return this;
	}

	/**
	 * Adds the specified {@link Var} classes to this config.
	 *
	 * <p>
	 * These variables affect the variable resolver returned by {@link RestRequest#getVarResolverSession()} which is
	 * used to resolve string variables of the form <js>"$X{...}"</js>.
	 *
	 * <p>
	 * See {@link RestContext#getVarResolver()} for a list of predefined variables.
	 *
	 * @param vars The {@link Var} classes to add to this config.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder vars(Class<?>...vars) {
		this.varResolverBuilder.vars(vars);
		return this;
	}

	/**
	 * Adds a var context object to this config.
	 *
	 * <p>
	 * Var context objects are read-only objects associated with the variable resolver for vars that require external
	 * information.
	 *
	 * <p>
	 * For example, the {@link ConfigVar} needs access to this resource's {@link Config} object
	 *
	 * In this case, we call the following code to add it to the context map:
	 * <p class='bcode w800'>
	 * 	config.varBean(Config.<jk>class</jk>, configFile);
	 * </p>
	 *
	 * @param beanType The bean type being added.
	 * @param bean The bean being added.
	 * @param <T> The bean type being added.
	 * @return This object (for method chaining).
	 */
	public <T> RestContextBuilder varBean(Class<T> beanType, T bean) {
		this.varResolverBuilder.bean(beanType, bean);
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
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder config(Config config) {
		this.config = config;
		return this;
	}

	/**
	 * Creates a new {@link ContextProperties} object initialized with the properties defined in this config.
	 *
	 * @return A new property store.
	 */
	protected ContextPropertiesBuilder createContextPropertiesBuilder() {
		return ContextProperties.create();
	}


	//----------------------------------------------------------------------------------------------------
	// Methods that give access to the config file, var resolver, and properties.
	//----------------------------------------------------------------------------------------------------

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
	public Config getConfig() {
		return config;
	}

	/**
	 * Creates the variable resolver for this resource.
	 *
	 * <p>
	 * The variable resolver returned by this method can resolve the following variables:
	 * <ul>
	 * 	<li>{@link SystemPropertiesVar}
	 * 	<li>{@link EnvVariablesVar}
	 * 	<li>{@link ConfigVar}
	 * 	<li>{@link IfVar}
	 * 	<li>{@link SwitchVar}
	 * </ul>
	 *
	 * <p>
	 * Note that the variables supported here are only a subset of those returned by
	 * {@link RestRequest#getVarResolverSession()}.
	 *
	 * @return The variable resolver for this resource.  Never <jk>null</jk>.
	 */
	public VarResolverBuilder getVarResolverBuilder() {
		return varResolverBuilder;
	}

	/**
	 * Returns the REST path defined on this builder.
	 *
	 * @return The REST path defined on this builder.
	 */
	public String getPath() {
		Object p = peek(REST_path);
		return p == null ? "" : p.toString();
	}

	//----------------------------------------------------------------------------------------------------
	// Properties
	//----------------------------------------------------------------------------------------------------

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Allowed header URL parameters.
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
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_allowedHeaderParams}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is <jk>true</jk>.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder allowedHeaderParams(String value) {
		return set(REST_allowedHeaderParams, value);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Allowed method headers.
	 *
	 * <p>
	 * A comma-delimited list of HTTP method names that are allowed to be passed as values in an <c>X-Method</c> HTTP header
	 * to override the real HTTP method name.
	 * <p>
	 * Allows you to override the actual HTTP method with a simulated method.
	 * <br>For example, if an HTTP Client API doesn't support <c>PATCH</c> but does support <c>POST</c> (because
	 * <c>PATCH</c> is not part of the original HTTP spec), you can add a <c>X-Method: PATCH</c> header on a normal
	 * <c>HTTP POST /foo</c> request call which will make the HTTP call look like a <c>PATCH</c> request in any of the REST APIs.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_allowedMethodHeaders}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is <jk>true</jk>.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder allowedMethodHeaders(String value) {
		return set(REST_allowedMethodHeaders, value);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Allowed method parameters.
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
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_allowedMethodParams}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is <code>[<js>"HEAD"</js>,<js>"OPTIONS"</js>]</code>.
	 * 	<br>Individual values can also be comma-delimited lists.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder allowedMethodParams(String value) {
		return set(REST_allowedMethodParams, value);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Bean store.
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
	 * 	<li class='jf'>{@link RestContext#REST_beanStore}
	 * 	<li class='link'>{@doc RestInjection}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is {@link BasicRestLogger}.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder beanStore(Class<? extends BeanStore> value) {
		return set(REST_beanStore, value);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Bean store.
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
	 * Note that the <c>Spr÷ingRestServlet</c> classes uses the <c>SpringBeanStore</c> class to allow for any
	 * Spring beans to be injected into your REST resources.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_beanStore}
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
		return set(REST_beanStore, value);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  REST call logger.
	 *
	 * <p>
	 * Specifies the logger to use for logging of HTTP requests and responses.
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc RestLoggingAndDebugging}
	 * 	<li class='jf'>{@link RestContext#REST_callLogger}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is {@link BasicRestLogger}.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder callLogger(Class<? extends RestLogger> value) {
		return set(REST_callLogger, value);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  REST call logger.
	 *
	 * <p>
	 * Specifies the logger to use for logging of HTTP requests and responses.
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc RestLoggingAndDebugging}
	 * 	<li class='jf'>{@link RestContext#REST_callLogger}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is {@link BasicRestLogger}.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder callLogger(RestLogger value) {
		return set(REST_callLogger, value);
	}

	/**
	 * Configuration property:  Default REST call logger.
	 *
	 * <p>
	 * The default logger to use if one is not specified.
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is {@link BasicRestLogger}.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder callLoggerDefault(Class<? extends RestLogger> value) {
		return set(REST_callLoggerDefault, value);
	}

	/**
	 * Configuration property:  Default REST call logger.
	 *
	 * <p>
	 * The default logger to use if one is not specified.
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is {@link BasicRestLogger}.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder callLoggerDefault(RestLogger value) {
		return set(REST_callLoggerDefault, value);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Children.
	 *
	 * <p>
	 * Defines children of this resource.
	 *
	 * <p>
	 * A REST child resource is simply another servlet that is initialized as part of the parent resource and has a
	 * servlet path directly under the parent servlet path.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_children}
	 * </ul>
	 *
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder children(Class<?>...values) {
		return prependTo(REST_children, values);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Children.
	 *
	 * <p>
	 * Same as {@link #children(Class...)} except input is pre-constructed instances.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_children}
	 * </ul>
	 *
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder children(Object...values) {
		return prependTo(REST_children, values);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Children.
	 *
	 * <p>
	 * Shortcut for adding a single child to this resource.
	 *
	 * <p>
	 * This can be used for resources that don't have a {@link Rest#path() @Rest(path)} annotation.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_children}
	 * </ul>
	 *
	 * @param path The child path relative to the parent resource URI.
	 * @param child The child to add to this resource.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder child(String path, Object child) {
		return prependTo(REST_children, new RestChild(path, child));
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Client version header.
	 *
	 * <p>
	 * Specifies the name of the header used to denote the client version on HTTP requests.
	 *
	 * <p>
	 * The client version is used to support backwards compatibility for breaking REST interface changes.
	 * <br>Used in conjunction with {@link RestOp#clientVersion() @RestOp(clientVersion)} annotation.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_clientVersionHeader}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is <js>"X-Client-Version"</js>.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder clientVersionHeader(String value) {
		return set(REST_clientVersionHeader, value);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  REST context class.
	 *
	 * <p>
	 * Allows you to extend the {@link RestContext} class to modify how any of the methods are implemented.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_contextClass}
	 * </ul>
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder contextClass(Class<? extends RestContext> value) {
		return set(REST_contextClass, value);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Class-level response converters.
	 *
	 * <p>
	 * Associates one or more {@link RestConverter converters} with a resource class.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_converters}
	 * </ul>
	 *
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder converters(Class<?>...values) {
		return prependTo(REST_converters, values);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Response converters.
	 *
	 * <p>
	 * Same as {@link #converters(Class...)} except input is pre-constructed instances.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_converters}
	 * </ul>
	 *
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder converters(RestConverter...values) {
		return prependTo(REST_converters, values);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Debug mode.
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
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder debug(Enablement value) {
		return set(REST_debug, value);
	}

	/**
	 * Configuration property:  Default debug mode.
	 *
	 * <p>
	 * The default value for the {@link #REST_debug} setting.
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder debugDefault(Enablement value) {
		return set(REST_debugDefault, value);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Debug enablement bean.
	 *
	 * TODO
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder debugEnablement(Class<? extends DebugEnablement> value) {
		return set(REST_debugEnablement, value);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Debug enablement bean.
	 *
	 * TODO
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder debugEnablement(DebugEnablement value) {
		return set(REST_debugEnablement, value);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Debug mode on specified classes/methods.
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
	 * 	<li class='jf'>{@link RestContext#REST_debugOn}
	 * </ul>
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder debugOn(String value) {
		return set(REST_debugOn, value);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Default character encoding.
	 *
	 * <p>
	 * The default character encoding for the request and response if not specified on the request.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_defaultCharset}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is <js>"utf-8"</js>.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder defaultCharset(String value) {
		return set(REST_defaultCharset, value);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Default character encoding.
	 *
	 * <p>
	 * Same as {@link #defaultCharset(Charset)} but takes in an instance of {@link Charset}.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_defaultCharset}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is <js>"utf-8"</js>.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder defaultCharset(Charset value) {
		return set(REST_defaultCharset, value);
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
			defaultRequestHeaders(Accept.of(value));
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
			defaultRequestHeaders(ContentType.of(value));
		return this;
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Default request attribute.
	 *
	 * <p>
	 * Adds a single default request attribute.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_defaultRequestAttributes}
	 * </ul>
	 *
	 * @param name The attribute name.
	 * @param value The attribute value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder defaultRequestAttribute(String name, Object value) {
		return defaultRequestAttributes(BasicNamedAttribute.of(name, value));
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Default request attribute.
	 *
	 * <p>
	 * Adds a single default request attribute.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_defaultRequestAttributes}
	 * </ul>
	 *
	 * @param name The attribute name.
	 * @param value The attribute value supplier.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder defaultRequestAttribute(String name, Supplier<?> value) {
		return defaultRequestAttributes(BasicNamedAttribute.of(name, value));
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Default request attributes.
	 *
	 * <p>
	 * Adds multiple default request attributes.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_defaultRequestAttributes}
	 * </ul>
	 *
	 * @param values The attributes.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder defaultRequestAttributes(NamedAttribute...values) {
		asList(values).stream().forEach(x -> appendTo(REST_defaultRequestAttributes, x));
		return this;
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Default request headers.
	 *
	 * <p>
	 * Adds a single default request header.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_defaultRequestHeaders}
	 * </ul>
	 *
	 * @param name The HTTP header name.
	 * @param value The HTTP header value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder defaultRequestHeader(String name, Object value) {
		return defaultRequestHeaders(BasicHeader.of(name, value));
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Default request headers.
	 *
	 * <p>
	 * Adds a single default request header.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_defaultRequestHeaders}
	 * </ul>
	 *
	 * @param name The HTTP header name.
	 * @param value The HTTP header value supplier.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder defaultRequestHeader(String name, Supplier<?> value) {
		return defaultRequestHeaders(BasicHeader.of(name, value));
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Default request headers.
	 *
	 * <p>
	 * Specifies default values for request headers if they're not passed in through the request.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_defaultRequestHeaders}
	 * </ul>
	 *
	 * @param values The headers to add.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder defaultRequestHeaders(Header...values) {
		asList(values).stream().forEach(x -> appendTo(REST_defaultRequestHeaders, x));
		return this;
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Default response headers.
	 *
	 * <p>
	 * Adds a single default response header.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_defaultResponseHeaders}
	 * </ul>
	 *
	 * @param name The response header name.
	 * @param value The response header value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder defaultResponseHeader(String name, Object value) {
		return defaultResponseHeaders(BasicHeader.of(name, value));
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Default response headers.
	 *
	 * <p>
	 * Adds a single default response header.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_defaultResponseHeaders}
	 * </ul>
	 *
	 * @param name The response header name.
	 * @param value The response header value supplier.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder defaultResponseHeader(String name, Supplier<?> value) {
		return defaultResponseHeaders(BasicHeader.of(name, value));
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Default response headers.
	 *
	 * <p>
	 * Specifies default values for response headers if they're not set after the Java REST method is called.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_defaultResponseHeaders}
	 * </ul>
	 *
	 * @param values The headers to add.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder defaultResponseHeaders(Header...values) {
		asList(values).stream().forEach(x -> appendTo(REST_defaultResponseHeaders, x));
		return this;
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Allow body URL parameter.
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
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_disableAllowBodyParam}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder disableAllowBodyParam() {
		return set(REST_disableAllowBodyParam);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Compression encoders.
	 *
	 * <p>
	 * These can be used to enable various kinds of compression (e.g. <js>"gzip"</js>) on requests and responses.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_encoders}
	 * </ul>
	 *
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder encoders(Class<?>...values) {
		return prependTo(REST_encoders, values);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Compression encoders.
	 *
	 * <p>
	 * Same as {@link #encoders(Class...)} except input a pre-constructed instances.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_encoders}
	 * </ul>
	 *
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder encoders(Encoder...values) {
		return prependTo(REST_encoders, values);
	}

	/**
	 * Configuration property:  File finder.
	 *
	 * <p>
	 * Used to retrieve localized files from the classpath for a variety of purposes including:
	 * <ul>
	 * 	<li>Resolution of {@link FileVar $F} variable contents.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_fileFinder}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is {@link BasicFileFinder}.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder fileFinder(Class<? extends FileFinder> value) {
		return set(REST_fileFinder, value);
	}

	/**
	 * Configuration property:  File finder.
	 *
	 * <p>
	 * Used to retrieve localized files from the classpath for a variety of purposes including:
	 * <ul>
	 * 	<li>Resolution of {@link FileVar $F} variable contents.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_fileFinder}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is {@link BasicFileFinder}.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder fileFinder(FileFinder value) {
		return set(REST_fileFinder, value);
	}

	/**
	 * Configuration property:  File finder default.
	 *
	 * <p>
	 * The default file finder.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_fileFinderDefault}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder fileFinderDefault(Class<? extends FileFinder> value) {
		return set(REST_fileFinderDefault, value);
	}

	/**
	 * Configuration property:  File finder default.
	 *
	 * <p>
	 * The default file finder.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_fileFinderDefault}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder fileFinderDefault(FileFinder value) {
		return set(REST_fileFinderDefault, value);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Class-level guards.
	 *
	 * <p>
	 * Associates one or more {@link RestGuard RestGuards} with all REST methods defined in this class.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_guards}
	 * </ul>
	 *
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder guards(Class<?>...values) {
		return prependTo(REST_guards, values);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Class-level guards.
	 *
	 * <p>
	 * Same as {@link #guards(Class...)} except input is pre-constructed instances.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_guards}
	 * </ul>
	 *
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder guards(RestGuard...values) {
		return prependTo(REST_guards, values);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  The maximum allowed input size (in bytes) on HTTP requests.
	 *
	 * <p>
	 * Useful for alleviating DoS attacks by throwing an exception when too much input is received instead of resulting
	 * in out-of-memory errors which could affect system stability.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_maxInput}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is <js>"100M"</js>.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder maxInput(String value) {
		return set(REST_maxInput, value);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Messages.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_messages}
	 * </ul>
	 *
	 * @param baseClass
	 * 	The base class that the bundle path is relative to.
	 * 	<br>If <jk>null</jk>, assumed to be the resource class itself.
	 * @param bundlePath The bundle path relative to the base class.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder messages(Class<?> baseClass, String bundlePath) {
		return prependTo(REST_messages, Tuple2.of(baseClass, bundlePath));
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Messages.
	 *
	 * <p>
	 * Same as {@link #messages(Class,String)} except assumes the base class is the resource class itself.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_messages}
	 * </ul>
	 *
	 * @param bundlePath The bundle path relative to the base class.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder messages(String bundlePath) {
		return prependTo(REST_messages, Tuple2.of(null, bundlePath));
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
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Parsers.
	 *
	 * <p>
	 * Adds class-level parsers to this resource.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_parsers}
	 * </ul>
	 *
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder parsers(Class<?>...values) {
		return prependTo(REST_parsers, values);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Parsers.
	 *
	 * <p>
	 * Same as {@link #parsers(Class...)} except input is pre-constructed instances.
	 *
	 * <p>
	 * Parser instances are considered set-in-stone and do NOT inherit properties and transforms defined on the
	 * resource class or method.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_parsers}
	 * </ul>
	 *
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder parsers(Object...values) {
		return prependTo(REST_parsers, values);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Parsers.
	 *
	 * <p>
	 * Same as {@link #parsers(Class...)} except allows you to overwrite the previous value.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_parsers}
	 * </ul>
	 *
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder parsersReplace(Object...values) {
		return set(REST_parsers, values);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  HTTP part parser.
	 *
	 * <p>
	 * Specifies the {@link HttpPartParser} to use for parsing headers, query/form parameters, and URI parts.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_partParser}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is {@link OpenApiParser}.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder partParser(Class<? extends HttpPartParser> value) {
		if (value != HttpPartParser.Null.class)
			set(REST_partParser, value);
		return this;
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  HTTP part parser.
	 *
	 * <p>
	 * Same as {@link #partParser(Class)} except input is a pre-constructed instance.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_partParser}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is {@link OpenApiParser}.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder partParser(HttpPartParser value) {
		return set(REST_partParser, value);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  HTTP part serializer.
	 *
	 * <p>
	 * Specifies the {@link HttpPartSerializer} to use for serializing headers, query/form parameters, and URI parts.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_partSerializer}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is {@link OpenApiSerializer}.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder partSerializer(Class<? extends HttpPartSerializer> value) {
		if (value != HttpPartSerializer.Null.class)
			set(REST_partSerializer, value);
		return this;
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  HTTP part serializer.
	 *
	 * <p>
	 * Same as {@link #partSerializer(Class)} except input is a pre-constructed instance.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_partSerializer}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is {@link OpenApiSerializer}.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder partSerializer(HttpPartSerializer value) {
		return set(REST_partSerializer, value);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Resource path.
	 *
	 * <p>
	 * Identifies the URL subpath relative to the parent resource.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_path}
	 * </ul>
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder path(String value) {
		if (startsWith(value, '/'))
			value = value.substring(1);
		set(REST_path, value);
		return this;
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Render response stack traces in responses.
	 *
	 * <p>
	 * Render stack traces in HTTP response bodies when errors occur.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_renderResponseStackTraces}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder renderResponseStackTraces(boolean value) {
		return set(REST_renderResponseStackTraces, value);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Render response stack traces in responses.
	 *
	 * <p>
	 * Shortcut for calling <code>renderResponseStackTraces(<jk>true</jk>)</code>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_renderResponseStackTraces}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder renderResponseStackTraces() {
		return set(REST_renderResponseStackTraces);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Response handlers.
	 *
	 * <p>
	 * Specifies a list of {@link ResponseHandler} classes that know how to convert POJOs returned by REST methods or
	 * set via {@link RestResponse#setOutput(Object)} into appropriate HTTP responses.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_responseHandlers}
	 * </ul>
	 *
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder responseHandlers(Class<?>...values) {
		return prependTo(REST_responseHandlers, values);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Response handlers.
	 *
	 * <p>
	 * Same as {@link #responseHandlers(Class...)} except input is pre-constructed instances.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_responseHandlers}
	 * </ul>
	 *
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder responseHandlers(ResponseHandler...values) {
		return prependTo(REST_responseHandlers, values);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  REST children class.
	 *
	 * <p>
	 * Allows you to extend the {@link RestChildren} class to modify how any of the methods are implemented.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_restChildrenClass}
	 * </ul>
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder restChildrenClass(Class<? extends RestChildren> value) {
		return set(REST_restChildrenClass, value);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  REST method context class.
	 *
	 * <p>
	 * Allows you to extend the {@link RestOperationContext} class to modify how any of the methods are implemented.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_restOperationContextClass}
	 * </ul>
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder restOperationContextClass(Class<? extends RestOperationContext> value) {
		return set(REST_restOperationContextClass, value);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Java method parameter resolvers.
	 *
	 * <p>
	 * By default, the Juneau framework will automatically Java method parameters of various types (e.g.
	 * <c>RestRequest</c>, <c>Accept</c>, <c>Reader</c>).
	 * This annotation allows you to provide your own resolvers for your own class types that you want resolved.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_restOperationArgs}
	 * </ul>
	 *
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	@SuppressWarnings("unchecked")
	public RestContextBuilder restOperationArgs(Class<? extends RestOperationArg>...values) {
		return prependTo(REST_restOperationArgs, values);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  REST methods class.
	 *
	 * <p>
	 * Allows you to extend the {@link RestOperations} class to modify how any of the methods are implemented.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_restOperationsClass}
	 * </ul>
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder restOperationsClass(Class<? extends RestOperations> value) {
		return set(REST_restOperationsClass, value);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Declared roles.
	 *
	 * <p>
	 * A comma-delimited list of all possible user roles.
	 *
	 * <p>
	 * Used in conjunction with {@link RestContextBuilder#roleGuard(String)} is used with patterns.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@Rest</ja>(
	 * 		rolesDeclared=<js>"ROLE_ADMIN,ROLE_READ_WRITE,ROLE_READ_ONLY,ROLE_SPECIAL"</js>,
	 * 		roleGuard=<js>"ROLE_ADMIN || (ROLE_READ_WRITE &amp;&amp; ROLE_SPECIAL)"</js>
	 * 	)
	 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestServlet {
	 * 		...
	 * 	}
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_rolesDeclared}
	 * </ul>
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder rolesDeclared(String...values) {
		return addTo(REST_rolesDeclared, values);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Role guard.
	 *
	 * <p>
	 * An expression defining if a user with the specified roles are allowed to access methods on this class.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@Rest</ja>(
	 * 		path=<js>"/foo"</js>,
	 * 		roleGuard=<js>"ROLE_ADMIN || (ROLE_READ_WRITE &amp;&amp; ROLE_SPECIAL)"</js>
	 * 	)
	 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestServlet {
	 * 		...
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports any of the following expression constructs:
	 * 		<ul>
	 * 			<li><js>"foo"</js> - Single arguments.
	 * 			<li><js>"foo,bar,baz"</js> - Multiple OR'ed arguments.
	 * 			<li><js>"foo | bar | bqz"</js> - Multiple OR'ed arguments, pipe syntax.
	 * 			<li><js>"foo || bar || bqz"</js> - Multiple OR'ed arguments, Java-OR syntax.
	 * 			<li><js>"fo*"</js> - Patterns including <js>'*'</js> and <js>'?'</js>.
	 * 			<li><js>"fo* &amp; *oo"</js> - Multiple AND'ed arguments, ampersand syntax.
	 * 			<li><js>"fo* &amp;&amp; *oo"</js> - Multiple AND'ed arguments, Java-AND syntax.
	 * 			<li><js>"fo* || (*oo || bar)"</js> - Parenthesis.
	 * 		</ul>
	 * 	<li>
	 * 		AND operations take precedence over OR operations (as expected).
	 * 	<li>
	 * 		Whitespace is ignored.
	 * 	<li>
	 * 		<jk>null</jk> or empty expressions always match as <jk>false</jk>.
	 * 	<li>
	 * 		If patterns are used, you must specify the list of declared roles using {@link Rest#rolesDeclared()} or {@link RestContext#REST_rolesDeclared}.
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * @param value The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder roleGuard(String value) {
		return addTo(REST_roleGuard, value);
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
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Serializers.
	 *
	 * <p>
	 * Adds class-level serializers to this resource.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_serializers}
	 * </ul>
	 *
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder serializers(Class<?>...values) {
		return prependTo(REST_serializers, values);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Serializers.
	 *
	 * <p>
	 * Same as {@link #serializers(Class[])} but replaces any existing values.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_serializers}
	 * </ul>
	 *
	 * @param values The values to set on this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder serializersReplace(Class<?>...values) {
		return prependTo(REST_serializers, values);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Serializers.
	 *
	 * <p>
	 * Same as {@link #serializers(Class...)} except input is pre-constructed instances.
	 *
	 * <p>
	 * Serializer instances are considered set-in-stone and do NOT inherit properties and transforms defined on the
	 * resource class or method.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_serializers}
	 * </ul>
	 *
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder serializers(Object...values) {
		return prependTo(REST_serializers, values);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Serializers.
	 *
	 * <p>
	 * Same as {@link #serializers(Class...)} except allows you to overwrite the previous value.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_serializers}
	 * </ul>
	 *
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder serializersReplace(Object...values) {
		return set(REST_serializers, values);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Supported accept media types.
	 *
	 * <p>
	 * Overrides the media types inferred from the serializers that identify what media types can be produced by the resource.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_produces}
	 * </ul>
	 *
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder produces(String...values) {
		return prependTo(REST_produces, values);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Supported accept media types.
	 *
	 * <p>
	 * Same as {@link #produces(String...)} but replaces any previous values.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_produces}
	 * </ul>
	 *
	 * @param values The values to set on this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder producesReplace(String...values) {
		return set(REST_produces, values);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Supported accept media types.
	 *
	 * <p>
	 * Same as {@link #produces(String...)} except input is {@link MediaType} instances.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_produces}
	 * </ul>
	 *
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder produces(MediaType...values) {
		return prependTo(REST_produces, values);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Supported accept media types.
	 *
	 * <p>
	 * Same as {@link #produces(MediaType...)} but replaces any previous values.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_produces}
	 * </ul>
	 *
	 * @param values The values to set on this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder producesReplace(MediaType...values) {
		return set(REST_produces, values);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Supported content media types.
	 *
	 * <p>
	 * Overrides the media types inferred from the parsers that identify what media types can be consumed by the resource.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_consumes}
	 * </ul>
	 *
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder consumes(String...values) {
		return prependTo(REST_consumes, values);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Supported content media types.
	 *
	 * <p>
	 * Same as {@link #consumes(String...)} but replaces any existing values.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_consumes}
	 * </ul>
	 *
	 * @param values The values to set on this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder consumesReplace(String...values) {
		return set(REST_consumes, values);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Supported content media types.
	 *
	 * <p>
	 * Same as {@link #consumes(String...)} except input is {@link MediaType} instances.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_consumes}
	 * </ul>
	 *
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder consumes(MediaType...values) {
		return prependTo(REST_consumes, values);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Supported content media types.
	 *
	 * <p>
	 * Same as {@link #consumes(MediaType...)} except replaces any existing values.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_consumes}
	 * </ul>
	 *
	 * @param values The values to set on this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder consumesReplace(MediaType...values) {
		return set(REST_consumes, values);
	}

	/**
	 * Configuration property:  Static files finder.
	 *
	 * <p>
	 * Used to retrieve localized files to be served up as static files through the REST API via the following
	 * predefined methods:
	 * <ul class='javatree'>
	 * 	<li class='jm'>{@link BasicRestObject#getHtdoc(String, Locale)}.
	 * 	<li class='jm'>{@link BasicRestServlet#getHtdoc(String, Locale)}.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_staticFiles}
	 * </ul>
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder staticFiles(Class<? extends StaticFiles> value) {
		return set(REST_staticFiles, value);
	}

	/**
	 * Configuration property:  Static files finder.
	 *
	 * <p>
	 * Used to retrieve localized files to be served up as static files through the REST API via the following
	 * predefined methods:
	 * <ul class='javatree'>
	 * 	<li class='jm'>{@link BasicRestObject#getHtdoc(String, Locale)}.
	 * 	<li class='jm'>{@link BasicRestServlet#getHtdoc(String, Locale)}.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_staticFiles}
	 * </ul>
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder staticFiles(StaticFiles value) {
		return set(REST_staticFiles, value);
	}

	/**
	 * Configuration property:  Static files finder default.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_staticFilesDefault}
	 * </ul>
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder staticFilesDefault(Class<? extends StaticFiles> value) {
		return set(REST_staticFilesDefault, value);
	}

	/**
	 * Configuration property:  Static files finder default.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_staticFilesDefault}
	 * </ul>
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder staticFilesDefault(StaticFiles value) {
		return set(REST_staticFilesDefault, value);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Swagger provider.
	 *
	 * <p>
	 * Class used to retrieve swagger information about a resource.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_swaggerProvider}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is {@link BasicSwaggerProvider}.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder swaggerProvider(Class<? extends SwaggerProvider> value) {
		return set(REST_swaggerProvider, value);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Swagger provider.
	 *
	 * <p>
	 * Same as {@link #swaggerProvider(Class)} except input is a pre-constructed instance.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_swaggerProvider}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is {@link BasicSwaggerProvider}.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder swaggerProvider(SwaggerProvider value) {
		return set(REST_swaggerProvider, value);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Resource authority path.
	 *
	 * <p>
	 * Overrides the authority path value for this resource and any child resources.
	 *
	 * <p>
	 * This setting is useful if you want to resolve relative URIs to absolute paths and want to explicitly specify the hostname/port.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_uriAuthority}
	 * </ul>
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder uriAuthority(String value) {
		if (! value.isEmpty())
			set(REST_uriAuthority, value);
		return this;
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Resource context path.
	 *
	 * <p>
	 * Overrides the context path value for this resource and any child resources.
	 *
	 * <p>
	 * This setting is useful if you want to use <js>"context:/child/path"</js> URLs in child resource POJOs but
	 * the context path is not actually specified on the servlet container.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_uriContext}
	 * </ul>
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder uriContext(String value) {
		if (! value.isEmpty())
			set(REST_uriContext, value);
		return this;
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  URI resolution relativity.
	 *
	 * <p>
	 * Specifies how relative URIs should be interpreted by serializers.
	 *
	 * <p>
	 * See {@link UriResolution} for possible values.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_uriRelativity}
	 * </ul>
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder uriRelativity(String value) {
		if (! value.isEmpty())
			set(REST_uriRelativity, value);
		return this;
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  URI resolution.
	 *
	 * <p>
	 * Specifies how relative URIs should be interpreted by serializers.
	 *
	 * <p>
	 * See {@link UriResolution} for possible values.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_uriResolution}
	 * </ul>
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder uriResolution(String value) {
		if (! value.isEmpty())
			set(REST_uriResolution, value);
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
	public RestContextBuilder applyAnnotations(AnnotationList al, VarResolverSession r) {
		super.applyAnnotations(al, r);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestContextBuilder debug() {
		super.debug();
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestContextBuilder locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestContextBuilder mediaType(MediaType value) {
		super.mediaType(value);
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
	public RestContextBuilder timeZone(TimeZone value) {
		super.timeZone(value);
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

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder beanClassVisibility(Visibility value) {
		super.beanClassVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder beanConstructorVisibility(Visibility value) {
		super.beanConstructorVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder beanFieldVisibility(Visibility value) {
		super.beanFieldVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder beanInterceptor(Class<?> on, Class<? extends org.apache.juneau.transform.BeanInterceptor<?>> value) {
		super.beanInterceptor(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder beanMapPutReturnsOldValue() {
		super.beanMapPutReturnsOldValue();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder beanMethodVisibility(Visibility value) {
		super.beanMethodVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder beanProperties(Map<String,Object> values) {
		super.beanProperties(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder beanProperties(Class<?> beanClass, String properties) {
		super.beanProperties(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder beanProperties(String beanClassName, String properties) {
		super.beanProperties(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder beanPropertiesExcludes(Map<String,Object> values) {
		super.beanPropertiesExcludes(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder beanPropertiesExcludes(Class<?> beanClass, String properties) {
		super.beanPropertiesExcludes(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder beanPropertiesExcludes(String beanClassName, String properties) {
		super.beanPropertiesExcludes(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder beanPropertiesReadOnly(Map<String,Object> values) {
		super.beanPropertiesReadOnly(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder beanPropertiesReadOnly(Class<?> beanClass, String properties) {
		super.beanPropertiesReadOnly(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder beanPropertiesReadOnly(String beanClassName, String properties) {
		super.beanPropertiesReadOnly(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder beanPropertiesWriteOnly(Map<String,Object> values) {
		super.beanPropertiesWriteOnly(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder beanPropertiesWriteOnly(Class<?> beanClass, String properties) {
		super.beanPropertiesWriteOnly(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder beanPropertiesWriteOnly(String beanClassName, String properties) {
		super.beanPropertiesWriteOnly(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder beansRequireDefaultConstructor() {
		super.beansRequireDefaultConstructor();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder beansRequireSerializable() {
		super.beansRequireSerializable();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder beansRequireSettersForGetters() {
		super.beansRequireSettersForGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder dictionary(Object...values) {
		super.dictionary(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder dictionaryOn(Class<?> on, java.lang.Class<?>...values) {
		super.dictionaryOn(on, values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder disableBeansRequireSomeProperties() {
		super.disableBeansRequireSomeProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder disableIgnoreMissingSetters() {
		super.disableIgnoreMissingSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder disableIgnoreTransientFields() {
		super.disableIgnoreTransientFields();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder disableIgnoreUnknownNullBeanProperties() {
		super.disableIgnoreUnknownNullBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder disableInterfaceProxies() {
		super.disableInterfaceProxies();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> RestContextBuilder example(Class<T> pojoClass, T o) {
		super.example(pojoClass, o);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> RestContextBuilder example(Class<T> pojoClass, String json) {
		super.example(pojoClass, json);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder findFluentSetters() {
		super.findFluentSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder findFluentSetters(Class<?> on) {
		super.findFluentSetters(on);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder ignoreInvocationExceptionsOnGetters() {
		super.ignoreInvocationExceptionsOnGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder ignoreInvocationExceptionsOnSetters() {
		super.ignoreInvocationExceptionsOnSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder ignoreUnknownBeanProperties() {
		super.ignoreUnknownBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder implClass(Class<?> interfaceClass, Class<?> implClass) {
		super.implClass(interfaceClass, implClass);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder implClasses(Map<Class<?>,Class<?>> values) {
		super.implClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder interfaceClass(Class<?> on, Class<?> value) {
		super.interfaceClass(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder interfaces(java.lang.Class<?>...value) {
		super.interfaces(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder notBeanClasses(Object...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder notBeanPackages(Object...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder propertyNamer(Class<? extends org.apache.juneau.PropertyNamer> value) {
		super.propertyNamer(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder propertyNamer(Class<?> on, Class<? extends org.apache.juneau.PropertyNamer> value) {
		super.propertyNamer(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder sortProperties() {
		super.sortProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder sortProperties(java.lang.Class<?>...on) {
		super.sortProperties(on);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder stopClass(Class<?> on, Class<?> value) {
		super.stopClass(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder swaps(Object...values) {
		super.swaps(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder typeName(Class<?> on, String value) {
		super.typeName(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder typePropertyName(String value) {
		super.typePropertyName(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder typePropertyName(Class<?> on, String value) {
		super.typePropertyName(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder useEnumNames() {
		super.useEnumNames();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestContextBuilder useJavaBeanIntrospector() {
		super.useJavaBeanIntrospector();
		return this;
	}

	// </FluentSetters>

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
