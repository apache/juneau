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
import static org.apache.juneau.internal.CollectionUtils.*;

import java.lang.annotation.*;
import java.nio.charset.*;
import java.util.*;

import javax.servlet.*;

import org.apache.juneau.*;
import org.apache.juneau.config.*;
import org.apache.juneau.config.vars.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.html.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.exception.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.reshandlers.*;
import org.apache.juneau.rest.util.RestUtils;
import org.apache.juneau.rest.vars.*;
import org.apache.juneau.rest.widget.*;
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
 * 	<li>{@link #getProperties()} - The modifiable configuration properties for this resource.
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
 * 				.pojoSwaps(TemporalCalendarSwap.IsoLocalDateTime.<jk>class</jk>)
 * 				.set(<jsf>PARSER_debug</jsf>, <jk>true</jk>);
 * 	}
 *
 * 	<jc>// Option #2 - Use an INIT hook.</jc>
 * 	<ja>@RestHook</ja>(<jsf>INIT</jsf>)
 * 	<jk>public void</jk> init(RestContextBuilder builder) <jk>throws</jk> Exception {
 * 			builder
 * 				.pojoSwaps(TemporalCalendarSwap.IsoLocalDateTime.<jk>class</jk>)
 * 				.set(<jsf>PARSER_debug</jsf>, <jk>true</jk>);
 * 	}
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-rest-server.RestContext}
 * </ul>
 */
public class RestContextBuilder extends BeanContextBuilder implements ServletConfig {

	final ServletConfig inner;

	Class<?> resourceClass;
	Object resource;
	ServletContext servletContext;
	RestContext parentContext;

	//-----------------------------------------------------------------------------------------------------------------
	// The following fields are meant to be modifiable.
	// They should not be declared final.
	// Read-only snapshots of these will be made in RestServletContext.
	//-----------------------------------------------------------------------------------------------------------------

	RestContextProperties properties;
	Config config;
	VarResolverBuilder varResolverBuilder;

	@SuppressWarnings("deprecation")
	RestContextBuilder(ServletConfig servletConfig, Class<?> resourceClass, RestContext parentContext) throws ServletException {
		this.inner = servletConfig;
		this.resourceClass = resourceClass;
		this.parentContext = parentContext;
		this.properties = new RestContextProperties();

		ClassInfo rci = ClassInfo.of(resourceClass);

		// Default values.
		logger(BasicRestLogger.class);
		partSerializer(OpenApiSerializer.class);
		partParser(OpenApiParser.class);
		staticFileResponseHeader("Cache-Control", "max-age=86400, public");
		encoders(IdentityEncoder.INSTANCE);
		responseHandlers(
			ReaderHandler.class,
			InputStreamHandler.class,
			DefaultHandler.class
		);

		try {

			varResolverBuilder = new VarResolverBuilder()
				.defaultVars()
				.vars(ConfigVar.class)
				.vars(FileVar.class)
				.contextObject("crm", new ClasspathResourceManager(resourceClass));

			VarResolver vr = varResolverBuilder.build();

			List<AnnotationInfo<RestResource>> restResourceAnnotationsParentFirst = rci.getAnnotationInfos(RestResource.class);
			List<AnnotationInfo<Rest>> restAnnotationsParentFirst = rci.getAnnotationInfos(Rest.class);

			// Find our config file.  It's the last non-empty @RestResource(config).
			String configPath = "";
			for (AnnotationInfo<RestResource> r : restResourceAnnotationsParentFirst)
				if (! r.getAnnotation().config().isEmpty())
					configPath = r.getAnnotation().config();
			for (AnnotationInfo<Rest> r : restAnnotationsParentFirst)
				if (! r.getAnnotation().config().isEmpty())
					configPath = r.getAnnotation().config();
			String cf = vr.resolve(configPath);

			if ("SYSTEM_DEFAULT".equals(cf))
				this.config = Config.getSystemDefault();

			if (this.config == null) {
				ConfigBuilder cb = Config.create().varResolver(vr);
				if (! cf.isEmpty())
					cb.name(cf);
				this.config = cb.build();
			}

			// Add our config file to the variable resolver.
			varResolverBuilder.contextObject(ConfigVar.SESSION_config, config);
			vr = varResolverBuilder.build();

			// Add the servlet init parameters to our properties.
			if (servletConfig != null) {
				for (Enumeration<String> ep = servletConfig.getInitParameterNames(); ep.hasMoreElements();) {
					String p = ep.nextElement();
					String initParam = servletConfig.getInitParameter(p);
					set(vr.resolve(p), vr.resolve(initParam));
				}
			}

			applyAnnotations(rci.getAnnotationList(ConfigAnnotationFilter.INSTANCE), vr.createSession());

			// Load stuff from parent-to-child order.
			// This allows child settings to overwrite parent settings.
			for (AnnotationInfo<RestResource> e : restResourceAnnotationsParentFirst) {
				RestResource r = e.getAnnotation();
				for (Property p : r.properties())
					set(vr.resolve(p.name()), vr.resolve(p.value()));
				for (String p : r.flags())
					set(p, true);
			}
			for (AnnotationInfo<Rest> e : restAnnotationsParentFirst) {
				Rest r = e.getAnnotation();
				for (Property p : r.properties())
					set(vr.resolve(p.name()), vr.resolve(p.value()));
				for (String p : r.flags())
					set(p, true);
			}

		} catch (Exception e) {
			throw new ServletException(e);
		}
	}

	@Override /* BeanContextBuilder */
	public RestContext build() {
		try {
			return new RestContext(this);
		} catch (Exception e) {
			throw toHttpException(e, InternalServerError.class);
		}
	}

	/*
	 * Calls all @RestHook(INIT) methods on the specified resource object.
	 */
	RestContextBuilder init(Object resource) throws ServletException {
		this.resource = resource;
		ClassInfo rci = ClassInfo.of(resource).resolved();

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
			assertArgsOnlyOfType(m, RestContextBuilder.class, ServletConfig.class);
			Class<?>[] pt = (Class<?>[])m.getRawParamTypes().toArray();
			Object[] args = new Object[pt.length];
			for (int i = 0; i < args.length; i++) {
				if (pt[i] == RestContextBuilder.class)
					args[i] = this;
				else
					args[i] = this.inner;
			}
			try {
				m.invoke(resource, args);
			} catch (Exception e) {
				throw new RestServletException("Exception thrown from @RestHook(INIT) method {0}.", m).initCause(e);
			}
		}
		return this;
	}

	private static void assertArgsOnlyOfType(MethodInfo m, Class<?>...args) {
		if (! m.argsOnlyOfType(args))
			throw new FormattedIllegalArgumentException("Invalid arguments passed to method {0}.  Only arguments of type {1} are allowed.", m, args);
	}

	RestContextBuilder servletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
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
	 * For example, the {@link ConfigVar} needs access to this resource's {@link Config} through the
	 * {@link ConfigVar#SESSION_config} object that can be specified as either a session object (temporary) or
	 * context object (permanent).
	 * In this case, we call the following code to add it to the context map:
	 * <p class='bcode w800'>
	 * 	config.addVarContextObject(<jsf>SESSION_config</jsf>, configFile);
	 * </p>
	 *
	 * @param name The context object key (i.e. the name that the Var class looks for).
	 * @param object The context object.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder varContextObject(String name, Object object) {
		this.varResolverBuilder.contextObject(name, object);
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
	 * Creates a new {@link PropertyStore} object initialized with the properties defined in this config.
	 *
	 * @return A new property store.
	 */
	protected PropertyStoreBuilder createPropertyStore() {
		return PropertyStore.create().add(properties);
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
	 * Returns the configuration properties for this resource.
	 *
	 * <p>
	 * The configuration properties are determined via the {@link Rest#properties() @Rest(properties)} annotation on the resource.
	 *
	 * <p>
	 * The configuration properties can be augmented programmatically by adding the following method to your resource:
	 * <p class='bcode w800'>
	 * 	<jk>public</jk> RestContextProperties createProperties(ServletConfig servletConfig) <jk>throws</jk> ServletException;
	 * </p>
	 *
	 * <p>
	 * These properties can be modified during servlet initialization.
	 * However, any modifications made after {@link RestServlet#init(ServletConfig)} has been called will have no effect.
	 *
	 * @return The configuration properties for this resource.  Never <jk>null</jk>.
	 */
	public RestContextProperties getProperties() {
		return properties;
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
		return p == null ? "_" : p.toString();
	}

	//----------------------------------------------------------------------------------------------------
	// Properties
	//----------------------------------------------------------------------------------------------------

	/**
	 * Configuration property:  Allow body URL parameter.
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
	 * 	<li class='jf'>{@link RestContext#REST_allowBodyParam}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is <jk>true</jk>.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder allowBodyParam(boolean value) {
		return set(REST_allowBodyParam, value);
	}

	/**
	 * Configuration property:  Allowed header URL parameters.
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
	public RestContextBuilder allowedHeaderParams(String value) {
		return set(REST_allowedHeaderParams, value);
	}

	/**
	 * Configuration property:  Allowed method headers.
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
	public RestContextBuilder allowedMethodHeaders(String value) {
		return set(REST_allowedMethodHeaders, value);
	}

	/**
	 * Configuration property:  Allowed method parameters.
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
	public RestContextBuilder allowedMethodParams(String value) {
		return set(REST_allowedMethodParams, value);
	}

	/**
	 * Configuration property:  Allow header URL parameters.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #allowedHeaderParams(String)}
	 * </div>
	 *
	 * <p>
	 * When enabled, headers such as <js>"Accept"</js> and <js>"Content-Type"</js> to be passed in as URL query
	 * parameters.
	 * <br>
	 * For example:
	 * <p class='bcode w800'>
	 *  ?Accept=text/json&amp;Content-Type=text/json
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_allowHeaderParams}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is <jk>true</jk>.
	 * @return This object (for method chaining).
	 */
	@Deprecated
	public RestContextBuilder allowHeaderParams(boolean value) {
		return set(REST_allowedHeaderParams, value ? "*" : null);
	}

	/**
	 * Configuration property:  REST call handler.
	 *
	 * <p>
	 * This class handles the basic lifecycle of an HTTP REST call.
	 * <br>Subclasses can be used to customize how these HTTP calls are handled.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_callHandler}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is {@link BasicRestCallHandler}.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder callHandler(Class<? extends RestCallHandler> value) {
		return set(REST_callHandler, value);
	}

	/**
	 * Configuration property:  REST call handler.
	 *
	 * <p>
	 * Same as {@link #callHandler(Class)} except input is a pre-constructed instance.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_callHandler}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is {@link BasicRestCallHandler}.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder callHandler(RestCallHandler value) {
		return set(REST_callHandler, value);
	}

	/**
	 * Configuration property:  REST call logger.
	 *
	 * <p>
	 * Specifies the logger to use for logging of HTTP requests and responses.
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc juneau-rest-server.LoggingAndDebugging}
	 * 	<li class='jf'>{@link RestContext#REST_callLogger}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is {@link BasicRestCallLogger}.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder callLogger(Class<? extends RestCallLogger> value) {
		return set(REST_callLogger, value);
	}

	/**
	 * Configuration property:  REST call logger.
	 *
	 * <p>
	 * Specifies the logger to use for logging of HTTP requests and responses.
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc juneau-rest-server.LoggingAndDebugging}
	 * 	<li class='jf'>{@link RestContext#REST_callLogger}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is {@link BasicRestCallLogger}.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder callLogger(RestCallLogger value) {
		return set(REST_callLogger, value);
	}

	/**
	 * Configuration property:  REST call logging rules.
	 *
	 * <p>
	 * Specifies rules on how to handle logging of HTTP requests/responses.
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc juneau-rest-server.LoggingAndDebugging}
	 * 	<li class='jf'>{@link RestContext#REST_callLoggerConfig}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is {@link RestCallLoggerConfig#DEFAULT}.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder callLoggerConfig(RestCallLoggerConfig value) {
		return set(REST_callLoggerConfig, value);
	}

	/**
	 * Configuration property:  Children.
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
	public RestContextBuilder children(Class<?>...values) {
		return addTo(REST_children, values);
	}

	/**
	 * Configuration property:  Children.
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
	public RestContextBuilder children(Object...values) {
		return addTo(REST_children, values);
	}

	/**
	 * Configuration property:  Children.
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
	public RestContextBuilder child(String path, Object child) {
		return addTo(REST_children, new RestChild(path, child));
	}

	/**
	 * Configuration property:  Classpath resource finder.
	 *
	 * <p>
	 * Used to retrieve localized files from the classpath.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_classpathResourceFinder}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is {@link ClasspathResourceFinderBasic}.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder classpathResourceFinder(Class<? extends ClasspathResourceFinder> value) {
		return set(REST_classpathResourceFinder, value);
	}

	/**
	 * Configuration property:  Classpath resource finder.
	 *
	 * <p>
	 * Same as {@link #classpathResourceFinder(ClasspathResourceFinder)} except input is a pre-constructed instance.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_classpathResourceFinder}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is {@link ClasspathResourceFinderBasic}.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder classpathResourceFinder(ClasspathResourceFinder value) {
		return set(REST_classpathResourceFinder, value);
	}

	/**
	 * Configuration property:  Client version header.
	 *
	 * <p>
	 * Specifies the name of the header used to denote the client version on HTTP requests.
	 *
	 * <p>
	 * The client version is used to support backwards compatibility for breaking REST interface changes.
	 * <br>Used in conjunction with {@link RestMethod#clientVersion() @RestMethod(clientVersion)} annotation.
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
	public RestContextBuilder clientVersionHeader(String value) {
		return set(REST_clientVersionHeader, value);
	}

	/**
	 * Configuration property:  Class-level response converters.
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
	public RestContextBuilder converters(Class<?>...values) {
		return addTo(REST_converters, values);
	}

	/**
	 * Configuration property:  Response converters.
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
	public RestContextBuilder converters(RestConverter...values) {
		return addTo(REST_converters, values);
	}

	/**
	 * Configuration property:  Debug mode.
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
	 * 	<li class='jf'>{@link RestContext#REST_debug}
	 * 	<li class='jf'>{@link BeanContext#BEAN_debug}
	 * </ul>
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@Override
	public RestContextBuilder debug(boolean value) {
		super.debug(value);
		return debug(Enablement.TRUE);
	}

	/**
	 * Configuration property:  Debug mode.
	 *
	 * <p>
	 * Enables the following:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		HTTP request/response bodies are cached in memory for logging purposes.
	 * </ul>
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder debug(Enablement value) {
		return set(REST_debug, value);
	}

	/**
	 * Configuration property:  Default character encoding.
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
	public RestContextBuilder defaultCharset(String value) {
		return set(REST_defaultCharset, value);
	}

	/**
	 * Configuration property:  Default character encoding.
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
	public RestContextBuilder defaultCharset(Charset value) {
		return set(REST_defaultCharset, value);
	}

	/**
	 * Configuration property:  Default request attributes.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #reqAttrs(String...)}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@Deprecated
	public RestContextBuilder attrs(String...values) throws RestServletException {
		return reqAttrs(values);
	}

	/**
	 * Configuration property:  Default request headers.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #reqHeaders(String...)}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@Deprecated
	public RestContextBuilder defaultRequestHeaders(String...headers) throws RestServletException {
		return reqHeaders(headers);
	}

	/**
	 * Specifies a default <c>Accept</c> header value if not specified on a request.
	 *
	 * @param value
	 * 	The default value of the <c>Accept</c> header.
	 * 	<br>Ignored if <jk>null</jk> or empty.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder defaultAccept(String value) {
		if (isNotEmpty(value))
			reqHeader("Accept", value);
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
	public RestContextBuilder defaultContentType(String value) {
		if (isNotEmpty(value))
			reqHeader("Content-Type", value);
		return this;
	}

	/**
	 * Configuration property:  Default request attribute.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #reqAttr(String, Object)}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@Deprecated
	public RestContextBuilder attr(String name, Object value) {
		return reqAttr(name, value);
	}

	/**
	 * Configuration property:  Default request headers.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #reqHeader(String,Object)}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@Deprecated
	public RestContextBuilder defaultRequestHeader(String name, Object value) {
		return reqHeader(name, value);
	}

	/**
	 * Configuration property:  Default response headers.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #resHeaders(String...)}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@Deprecated
	public RestContextBuilder defaultResponseHeaders(String...headers) throws RestServletException {
		return resHeaders(headers);
	}

	/**
	 * Configuration property:  Default response headers.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #resHeader(String, Object)}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@Deprecated
	public RestContextBuilder defaultResponseHeader(String name, Object value) {
		return resHeader(name, value);
	}

	/**
	 * Configuration property:  Compression encoders.
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
	public RestContextBuilder encoders(Class<?>...values) {
		return addTo(REST_encoders, values);
	}

	/**
	 * Configuration property:  Compression encoders.
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
	public RestContextBuilder encoders(Encoder...values) {
		return addTo(REST_encoders, values);
	}

	/**
	 * Configuration property:  Class-level guards.
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
	public RestContextBuilder guards(Class<?>...values) {
		return addTo(REST_guards, values);
	}

	/**
	 * Configuration property:  Class-level guards.
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
	public RestContextBuilder guards(RestGuard...values) {
		return addTo(REST_guards, values);
	}

	/**
	 * Configuration property:  REST info provider.
	 *
	 * <p>
	 * Class used to retrieve title/description/swagger information about a resource.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_infoProvider}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is {@link BasicRestInfoProvider}.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder infoProvider(Class<? extends RestInfoProvider> value) {
		return set(REST_infoProvider, value);
	}

	/**
	 * Configuration property:  REST info provider.
	 *
	 * <p>
	 * Same as {@link #infoProvider(Class)} except input is a pre-constructed instance.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_infoProvider}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is {@link BasicRestInfoProvider}.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder infoProvider(RestInfoProvider value) {
		return set(REST_infoProvider, value);
	}

	/**
	 * Configuration property:  REST logger.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #callLogger(Class)}
	 * </div>
	 *
	 * <p>
	 * Specifies the logger to use for logging.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_logger}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is {@link BasicRestCallLogger}.
	 * 	<br>Can be <jk>null</jk> to disable logging.
	 * @return This object (for method chaining).
	 */
	@Deprecated
	public RestContextBuilder logger(Class<? extends RestLogger> value) {
		return set(REST_logger, value);
	}

	/**
	 * Configuration property:  REST logger.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #callLogger(RestCallLogger)}
	 * </div>
	 *
	 * <p>
	 * Same as {@link #logger(Class)} except input is a pre-constructed instance.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_logger}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is {@link BasicRestLogger}.
	 * 	<br>Can be <jk>null</jk> to disable logging.
	 * @return This object (for method chaining).
	 */
	@Deprecated
	public RestContextBuilder logger(RestLogger value) {
		return set(REST_logger, value);
	}

	/**
	 * Configuration property:  The maximum allowed input size (in bytes) on HTTP requests.
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
	public RestContextBuilder maxInput(String value) {
		return set(REST_maxInput, value);
	}

	/**
	 * Configuration property:  Messages.
	 *
	 * <p>
	 * Identifies the location of the resource bundle for this class.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_messages}
	 * </ul>
	 *
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder messages(MessageBundleLocation...values) {
		return addTo(REST_messages, values);
	}

	/**
	 * Configuration property:  Messages.
	 *
	 * <p>
	 * Same as {@link #messages(MessageBundleLocation...)} except allows you to pass in the base class and bundle
	 * path separately.
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
	public RestContextBuilder messages(Class<?> baseClass, String bundlePath) {
		return addTo(REST_messages, new MessageBundleLocation(baseClass, bundlePath));
	}

	/**
	 * Configuration property:  Messages.
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
	public RestContextBuilder messages(String bundlePath) {
		return addTo(REST_messages, new MessageBundleLocation(null, bundlePath));
	}

	/**
	 * Configuration property:  MIME types.
	 *
	 * <p>
	 * Defines MIME-type file type mappings.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_mimeTypes}
	 * </ul>
	 *
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder mimeTypes(String...values) {
		return addTo(REST_mimeTypes, values);
	}

	/**
	 * Configuration property:  Java method parameter resolvers.
	 *
	 * <p>
	 * By default, the Juneau framework will automatically Java method parameters of various types (e.g.
	 * <c>RestRequest</c>, <c>Accept</c>, <c>Reader</c>).
	 * This annotation allows you to provide your own resolvers for your own class types that you want resolved.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_paramResolvers}
	 * </ul>
	 *
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings("unchecked")
	public RestContextBuilder paramResolvers(Class<? extends RestMethodParam>...values) {
		return addTo(REST_paramResolvers, values);
	}

	/**
	 * Configuration property:  Java method parameter resolvers.
	 *
	 * <p>
	 * Same as {@link #paramResolvers(Class...)} except input is pre-constructed instances.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_paramResolvers}
	 * </ul>
	 *
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder paramResolvers(RestMethodParam...values) {
		return addTo(REST_paramResolvers, values);
	}

	/**
	 * Configuration property:  Parser listener.
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
	public RestContextBuilder parserListener(Class<? extends ParserListener> value) {
		if (value != ParserListener.Null.class)
			set(PARSER_listener, value);
		return this;
	}

	/**
	 * Configuration property:  Parsers.
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
	public RestContextBuilder parsers(Class<?>...values) {
		return addTo(REST_parsers, values);
	}

	/**
	 * Configuration property:  Parsers.
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
	public RestContextBuilder parsers(Object...values) {
		return addTo(REST_parsers, values);
	}

	/**
	 * Configuration property:  Parsers.
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
	public RestContextBuilder parsersReplace(Object...values) {
		return set(REST_parsers, values);
	}

	/**
	 * Configuration property:  HTTP part parser.
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
	public RestContextBuilder partParser(Class<? extends HttpPartParser> value) {
		if (value != HttpPartParser.Null.class)
			set(REST_partParser, value);
		return this;
	}

	/**
	 * Configuration property:  HTTP part parser.
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
	public RestContextBuilder partParser(HttpPartParser value) {
		return set(REST_partParser, value);
	}

	/**
	 * Configuration property:  HTTP part serializer.
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
	public RestContextBuilder partSerializer(Class<? extends HttpPartSerializer> value) {
		if (value != HttpPartSerializer.Null.class)
			set(REST_partSerializer, value);
		return this;
	}

	/**
	 * Configuration property:  HTTP part serializer.
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
	public RestContextBuilder partSerializer(HttpPartSerializer value) {
		return set(REST_partSerializer, value);
	}

	/**
	 * Configuration property:  Resource path.
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
	public RestContextBuilder path(String value) {
		if (startsWith(value, '/'))
			value = value.substring(1);
		set(REST_path, value);
		return this;
	}

	/**
	 * Configuration property:  Render response stack traces in responses.
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
	public RestContextBuilder renderResponseStackTraces(boolean value) {
		return set(REST_renderResponseStackTraces, value);
	}

	/**
	 * Configuration property:  Render response stack traces in responses.
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
	public RestContextBuilder renderResponseStackTraces() {
		return set(REST_renderResponseStackTraces, true);
	}

	/**
	 * Configuration property:  Default request attribute.
	 *
	 * <p>
	 * Same as {@link #reqAttrs(String...)} but adds a single attribute name/value pair.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_reqAttrs}
	 * </ul>
	 *
	 * @param name The HTTP header name.
	 * @param value The HTTP header value.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder reqAttr(String name, Object value) {
		return addTo(REST_reqAttrs, name, value);
	}

	/**
	 * Configuration property:  Default request attributes.
	 *
	 * <p>
	 * Specifies default values for request attributes if they're not already set on the request.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_reqAttrs}
	 * </ul>
	 *
	 * @param values The attributes in the format <js>"Name: value"</js>.
	 * @return This object (for method chaining).
	 * @throws RestServletException If malformed header is found.
	 */
	public RestContextBuilder reqAttrs(String...values) throws RestServletException {
		for (String v : values) {
			String[] p = RestUtils.parseKeyValuePair(v);
			if (p == null)
				throw new RestServletException("Invalid default request attribute specified: ''{0}''.  Must be in the format: ''Name: value''", v);
			reqHeader(p[0], p[1]);
		}
		return this;
	}

	/**
	 * Configuration property:  Default request headers.
	 *
	 * <p>
	 * Same as {@link #reqHeaders(String...)} but adds a single header name/value pair.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_reqHeaders}
	 * </ul>
	 *
	 * @param name The HTTP header name.
	 * @param value The HTTP header value.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder reqHeader(String name, Object value) {
		return addTo(REST_reqHeaders, name, value);
	}

	/**
	 * Configuration property:  Default request headers.
	 *
	 * <p>
	 * Specifies default values for request headers if they're not passed in through the request.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_reqHeaders}
	 * </ul>
	 *
	 * @param headers The headers in the format <js>"Header-Name: header-value"</js>.
	 * @return This object (for method chaining).
	 * @throws RestServletException If malformed header is found.
	 */
	public RestContextBuilder reqHeaders(String...headers) throws RestServletException {
		for (String header : headers) {
			String[] h = RestUtils.parseHeader(header);
			if (h == null)
				throw new RestServletException("Invalid default request header specified: ''{0}''.  Must be in the format: ''Header-Name: header-value''", header);
			reqHeader(h[0], h[1]);
		}
		return this;
	}

	/**
	 * Configuration property:  Default response headers.
	 *
	 * <p>
	 * Specifies default values for response headers if they're not set after the Java REST method is called.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_resHeaders}
	 * </ul>
	 *
	 * @param headers The headers in the format <js>"Header-Name: header-value"</js>.
	 * @return This object (for method chaining).
	 * @throws RestServletException If malformed header is found.
	 */
	public RestContextBuilder resHeaders(String...headers) throws RestServletException {
		for (String header : headers) {
			String[] h = RestUtils.parseHeader(header);
			if (h == null)
				throw new RestServletException("Invalid default response header specified: ''{0}''.  Must be in the format: ''Header-Name: header-value''", header);
			resHeader(h[0], h[1]);
		}
		return this;
	}

	/**
	 * Configuration property:  Default response headers.
	 *
	 * <p>
	 * Same as {@link #resHeaders(String...)} but adds a single header name/value pair.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_resHeaders}
	 * </ul>
	 *
	 * @param name The HTTP header name.
	 * @param value The HTTP header value.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder resHeader(String name, Object value) {
		return addTo(REST_resHeaders, name, value);
	}

	/**
	 * REST resource resolver.
	 *
	 * <p>
	 * The resolver used for resolving child resources.
	 *
	 * <p>
	 * Can be used to provide customized resolution of REST resource class instances (e.g. resources retrieve from Spring).
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_resourceResolver}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is {@link BasicRestResourceResolver}.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder resourceResolver(Class<? extends RestResourceResolver> value) {
		return set(REST_resourceResolver, value);
	}

	/**
	 * REST resource resolver.
	 *
	 * <p>
	 * Same as {@link #resourceResolver(Class)} except input is a pre-constructed instance.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_resourceResolver}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is {@link BasicRestResourceResolver}.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder resourceResolver(RestResourceResolver value) {
		return set(REST_resourceResolver, value);
	}

	/**
	 * Configuration property:  Response handlers.
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
	public RestContextBuilder responseHandlers(Class<?>...values) {
		return addTo(REST_responseHandlers, values);
	}

	/**
	 * Configuration property:  Response handlers.
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
	public RestContextBuilder responseHandlers(ResponseHandler...values) {
		return addTo(REST_responseHandlers, values);
	}

	/**
	 * Configuration property:  Declared roles.
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
	public RestContextBuilder rolesDeclared(String...values) {
		return addTo(REST_rolesDeclared, values);
	}

	/**
	 * Configuration property:  Role guard.
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
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * @param value The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder roleGuard(String value) {
		return addTo(REST_roleGuard, value);
	}

	/**
	 * Configuration property:  Serializer listener.
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
	public RestContextBuilder serializerListener(Class<? extends SerializerListener> value) {
		if (value != SerializerListener.Null.class)
			set(SERIALIZER_listener, value);
		return this;
	}

	/**
	 * Configuration property:  Serializers.
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
	public RestContextBuilder serializers(Class<?>...values) {
		return addTo(REST_serializers, values);
	}

	/**
	 * Configuration property:  Serializers.
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
	public RestContextBuilder serializersReplace(Class<?>...values) {
		return set(REST_serializers, values);
	}

	/**
	 * Configuration property:  Serializers.
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
	public RestContextBuilder serializers(Object...values) {
		return addTo(REST_serializers, values);
	}

	/**
	 * Configuration property:  Serializers.
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
	public RestContextBuilder serializersReplace(Object...values) {
		return set(REST_serializers, values);
	}

	/**
	 * Configuration property:  Static file response headers.
	 *
	 * <p>
	 * Used to customize the headers on responses returned for statically-served files.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_staticFileResponseHeaders}
	 * </ul>
	 *
	 * @param headers
	 * 	The headers to add to this list.
	 * 	<br>The default is <code>{<js>'Cache-Control'</js>: <js>'max-age=86400, public</js>}</code>.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder staticFileResponseHeaders(Map<String,String> headers) {
		return addTo(REST_staticFileResponseHeaders, headers);
	}

	/**
	 * Configuration property:  Static file response headers.
	 *
	 * <p>
	 * Same as {@link #staticFileResponseHeaders(Map)} but replaces any previous values.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_staticFileResponseHeaders}
	 * </ul>
	 *
	 * @param headers
	 * 	The headers to set on this list.
	 * 	<br>The default is <code>{<js>'Cache-Control'</js>: <js>'max-age=86400, public</js>}</code>.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder staticFileResponseHeadersReplace(Map<String,String> headers) {
		return set(REST_staticFileResponseHeaders, headers);
	}

	/**
	 * Configuration property:  Static file response headers.
	 *
	 * <p>
	 * Same as {@link #staticFileResponseHeaders(Map)} with append=<jk>true</jk> except headers are strings
	 * composed of key/value pairs.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_staticFileResponseHeaders}
	 * </ul>
	 *
	 * @param headers The headers in the format <js>"Header-Name: header-value"</js>.
	 * @return This object (for method chaining).
	 * @throws RestServletException If malformed header is found.
	 */
	public RestContextBuilder staticFileResponseHeaders(String...headers) throws RestServletException {
		for (String header : headers) {
			String[] h = RestUtils.parseHeader(header);
			if (h == null)
				throw new RestServletException("Invalid static file response header specified: ''{0}''.  Must be in the format: ''Header-Name: header-value''", header);
			staticFileResponseHeader(h[0], h[1]);
		}
		return this;
	}

	/**
	 * Configuration property:  Static file response headers.
	 *
	 * <p>
	 * Same as {@link #staticFileResponseHeaders(String...)} except header is broken into name/value pair.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_staticFileResponseHeaders}
	 * </ul>
	 *
	 * @param name The HTTP header name.
	 * @param value The HTTP header value.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder staticFileResponseHeader(String name, String value) {
		return addTo(REST_staticFileResponseHeaders, name, value);
	}

	/**
	 * Configuration property:  Static file mappings.
	 *
	 * <p>
	 * Used to define paths and locations of statically-served files such as images or HTML documents.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_staticFiles}
	 * </ul>
	 *
	 * @param values The values to append to this setting.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder staticFiles(StaticFileMapping...values) {
		return addTo(REST_staticFiles, values);
	}

	/**
	 * Configuration property:  Static file mappings.
	 *
	 * <p>
	 * Same as {@link #staticFiles(StaticFileMapping...)} except input is in the form of a mapping string.
	 *
	 * <p>
	 * Mapping string must be one of these formats:
	 * <ul>
	 * 	<li><js>"path:location"</js> (e.g. <js>"foodocs:docs/foo"</js>)
	 * 	<li><js>"path:location:headers-json"</js> (e.g. <js>"foodocs:docs/foo:{'Cache-Control':'max-age=86400, public'}"</js>)
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_staticFiles}
	 * </ul>
	 *
	 * @param mappingString The static file mapping string.
	 * @throws ParseException If mapping string is malformed.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder staticFiles(String mappingString) throws ParseException{
		for (StaticFileMapping sfm : riterable(StaticFileMapping.parse(resourceClass, mappingString)))
			staticFiles(sfm);
		return this;
	}

	/**
	 * Configuration property:  Static file mappings.
	 *
	 * <p>
	 * Same as {@link #staticFiles(String)} except overrides the base class for retrieving the resource.
	 *
	 * <p>
	 * Mapping string must be one of these formats:
	 * <ul>
	 * 	<li><js>"path:location"</js> (e.g. <js>"foodocs:docs/foo"</js>)
	 * 	<li><js>"path:location:headers-json"</js> (e.g. <js>"foodocs:docs/foo:{'Cache-Control':'max-age=86400, public'}"</js>)
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_staticFiles}
	 * </ul>
	 *
	 * @param baseClass
	 * 	Overrides the default class to use for retrieving the classpath resource.
	 * 	<br>If <jk>null</jk>, uses the REST resource class.
	 * @param mappingString The static file mapping string.
	 * @return This object (for method chaining).
	 * @throws ParseException If mapping string is malformed.
	 */
	public RestContextBuilder staticFiles(Class<?> baseClass, String mappingString) throws ParseException {
		for (StaticFileMapping sfm : riterable(StaticFileMapping.parse(baseClass, mappingString)))
			staticFiles(sfm);
		return this;
	}

	/**
	 * Configuration property:  Static file mappings.
	 *
	 * <p>
	 * Same as {@link #staticFiles(String)} except path and location are already split values.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_staticFiles}
	 * </ul>
	 *
	 * @param path
	 * 	The mapped URI path.
	 * 	<br>Leading and trailing slashes are trimmed.
	 * @param location
	 * 	The location relative to the resource class.
	 * 	<br>Leading and trailing slashes are trimmed.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder staticFiles(String path, String location) {
		return staticFiles(new StaticFileMapping(resourceClass, path, location, null));
	}

	/**
	 * Configuration property:  Static file mappings.
	 *
	 * <p>
	 * Same as {@link #staticFiles(String,String)} except overrides the base class for retrieving the resource.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_staticFiles}
	 * </ul>
	 *
	 * @param baseClass
	 * 	Overrides the default class to use for retrieving the classpath resource.
	 * 	<br>If <jk>null</jk>, uses the REST resource class.
	 * @param path
	 * 	The mapped URI path.
	 * 	<br>Leading and trailing slashes are trimmed.
	 * @param location
	 * 	The location relative to the resource class.
	 * 	<br>Leading and trailing slashes are trimmed.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder staticFiles(Class<?> baseClass, String path, String location) {
		return staticFiles(new StaticFileMapping(baseClass, path, location, null));
	}

	/**
	 * Configuration property:  Supported accept media types.
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
	public RestContextBuilder produces(String...values) {
		return addTo(REST_produces, values);
	}

	/**
	 * Configuration property:  Supported accept media types.
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
	public RestContextBuilder producesReplace(String...values) {
		return set(REST_produces, values);
	}

	/**
	 * Configuration property:  Supported accept media types.
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
	public RestContextBuilder produces(MediaType...values) {
		return addTo(REST_produces, values);
	}

	/**
	 * Configuration property:  Supported accept media types.
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
	public RestContextBuilder producesReplace(MediaType...values) {
		return set(REST_produces, values);
	}

	/**
	 * Configuration property:  Supported content media types.
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
	public RestContextBuilder consumes(String...values) {
		return addTo(REST_consumes, values);
	}

	/**
	 * Configuration property:  Supported content media types.
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
	public RestContextBuilder consumesReplace(String...values) {
		return set(REST_consumes, values);
	}

	/**
	 * Configuration property:  Supported content media types.
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
	public RestContextBuilder consumes(MediaType...values) {
		return addTo(REST_consumes, values);
	}

	/**
	 * Configuration property:  Supported content media types.
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
	public RestContextBuilder consumesReplace(MediaType...values) {
		return set(REST_consumes, values);
	}

	/**
	 * Configuration property:  Properties.
	 *
	 * <p>
	 * Shortcut to add properties to the bean contexts of all serializers and parsers on all methods in the class.
	 *
	 * <p>
	 * Any of the properties defined on {@link RestContext} or any of the serializers and parsers can be specified.
	 *
	 * <p>
	 * Property values will be converted to the appropriate type.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContext#REST_properties}
	 * </ul>
	 *
	 * @param values The values to set on this setting.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder properties(Map<String,Object> values) {
		return addTo(REST_properties, values);
	}

	/**
	 * Configuration property:  Properties.
	 *
	 * <p>
	 * Shortcut to add properties to the bean contexts of all serializers and parsers on all methods in the class.
	 *
	 * <p>
	 * Any of the properties defined on {@link RestContext} or any of the serializers and parsers can be specified.
	 *
	 * <p>
	 * Property values will be converted to the appropriate type.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContext#REST_properties}
	 * </ul>
	 *
	 * @param name The key to add to the properties.
	 * @param value The value to add to the properties.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder property(String name, Object value) {
		return addTo(REST_properties, name, value);
	}

	/**
	 * Configuration property:  Resource authority path.
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
	public RestContextBuilder uriAuthority(String value) {
		if (! value.isEmpty())
			set(REST_uriAuthority, value);
		return this;
	}

	/**
	 * Configuration property:  Resource context path.
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
	public RestContextBuilder uriContext(String value) {
		if (! value.isEmpty())
			set(REST_uriContext, value);
		return this;
	}

	/**
	 * Configuration property:  URI resolution relativity.
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
	public RestContextBuilder uriRelativity(String value) {
		if (! value.isEmpty())
			set(REST_uriRelativity, value);
		return this;
	}

	/**
	 * Configuration property:  URI resolution.
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
	public RestContextBuilder uriResolution(String value) {
		if (! value.isEmpty())
			set(REST_uriResolution, value);
		return this;
	}

	/**
	 * Configuration property:  Use classpath resource caching.
	 *
	 * <p>
	 * When enabled, resources retrieved via {@link RestContext#getClasspathResource(String, Locale)} (and related
	 * methods) will be cached in memory to speed subsequent lookups.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_useClasspathResourceCaching}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is <jk>true</jk>.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder useClasspathResourceCaching(boolean value) {
		return set(REST_useClasspathResourceCaching, value);
	}

	/**
	 * Configuration property:  Use stack trace hashes.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #callLoggerConfig(RestCallLoggerConfig)}
	 * </div>
	 *
	 * <p>
	 * When enabled, the number of times an exception has occurred will be determined based on stack trace hashsums,
	 * made available through the {@link RestException#getOccurrence()} method.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_useStackTraceHashes}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is <jk>true</jk>.
	 * @return This object (for method chaining).
	 */
	@Deprecated
	public RestContextBuilder useStackTraceHashes(boolean value) {
		return set(REST_useStackTraceHashes, value);
	}

	/**
	 * Configuration property:  HTML Widgets.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link HtmlDocSerializerBuilder#widgets(Class[])}
	 * </div>
	 *
	 * <p>
	 * Defines widgets that can be used in conjunction with string variables of the form <js>"$W{name}"</js>to quickly
	 * generate arbitrary replacement text.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_widgets}
	 * </ul>
	 *
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 *
	 */
	@SuppressWarnings("unchecked")
	@Deprecated
	public RestContextBuilder widgets(Class<? extends Widget>...values) {
		return addTo(REST_widgets, values);
	}

	/**
	 * Configuration property:  HTML Widgets.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link HtmlDocSerializerBuilder#widgetsReplace(Class[])}
	 * </div>
	 *
	 * <p>
	 * Same as {@link #widgets(Class...)} but replaces any previous values.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_widgets}
	 * </ul>
	 *
	 * @param values The values to set on this setting.
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings("unchecked")
	@Deprecated
	public RestContextBuilder widgetsReplace(Class<? extends Widget>...values) {
		return set(REST_widgets, values);
	}

	/**
	 * Configuration property:  HTML Widgets.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link HtmlDocSerializerBuilder#widgets(HtmlWidget[])}
	 * </div>
	 *
	 * <p>
	 * Same as {@link #widgets(Class...)} except input is pre-constructed instances.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_widgets}
	 * </ul>
	 *
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	@Deprecated
	public RestContextBuilder widgets(Widget...values) {
		return addTo(REST_widgets, values);
	}

	/**
	 * Configuration property:  HTML Widgets.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link HtmlDocSerializerBuilder#widgetsReplace(HtmlWidget[])}
	 * </div>
	 *
	 * <p>
	 * Same as {@link #widgets(Widget...)} except allows you to overwrite the previous value.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_widgets}
	 * </ul>
	 *
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	@Deprecated
	public RestContextBuilder widgetsReplace(Widget...values) {
		return set(REST_widgets, values);
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder beanClassVisibility(Visibility value) {
		super.beanClassVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder beanConstructorVisibility(Visibility value) {
		super.beanConstructorVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	@Deprecated
	public RestContextBuilder beanDictionary(Class<?>...values) {
		super.beanDictionary(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	@Deprecated
	public RestContextBuilder beanDictionary(Object...values) {
		super.beanDictionary(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	@Deprecated
	public RestContextBuilder beanDictionaryReplace(Class<?>...values) {
		super.beanDictionaryReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	@Deprecated
	public RestContextBuilder beanDictionaryReplace(Object...values) {
		super.beanDictionaryReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	@Deprecated
	public RestContextBuilder beanDictionaryRemove(Class<?>...values) {
		super.beanDictionaryRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	@Deprecated
	public RestContextBuilder beanDictionaryRemove(Object...values) {
		super.beanDictionaryRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder beanFieldVisibility(Visibility value) {
		super.beanFieldVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder beanFilters(Class<?>...values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder beanFilters(Object...values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder beanFiltersReplace(Class<?>...values) {
		super.beanFiltersReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder beanFiltersReplace(Object...values) {
		super.beanFiltersReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder beanFiltersRemove(Class<?>...values) {
		super.beanFiltersRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder beanFiltersRemove(Object...values) {
		super.beanFiltersRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder beanMapPutReturnsOldValue(boolean value) {
		super.beanMapPutReturnsOldValue(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder beanMapPutReturnsOldValue() {
		super.beanMapPutReturnsOldValue();
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder beanMethodVisibility(Visibility value) {
		super.beanMethodVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder beansRequireDefaultConstructor(boolean value) {
		super.beansRequireDefaultConstructor(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder beansRequireDefaultConstructor() {
		super.beansRequireDefaultConstructor();
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder beansRequireSerializable(boolean value) {
		super.beansRequireSerializable(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder beansRequireSerializable() {
		super.beansRequireSerializable();
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder beansRequireSettersForGetters(boolean value) {
		super.beansRequireSettersForGetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder beansRequireSettersForGetters() {
		super.beansRequireSettersForGetters();
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder beansRequireSomeProperties(boolean value) {
		super.beansRequireSomeProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder beanTypePropertyName(String value) {
		super.beanTypePropertyName(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder bpi(Class<?> beanClass, String value) {
		super.bpi(beanClass, value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder bpi(Map<String,String> values) {
		super.bpi(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder bpi(String beanClassName, String value) {
		super.bpi(beanClassName, value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder bpx(Class<?> beanClass, String properties) {
		super.bpx(beanClass, properties);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder bpx(Map<String,String> values) {
		super.bpx(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder bpx(String beanClassName, String value) {
		super.bpx(beanClassName, value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder bpro(Class<?> beanClass, String value) {
		super.bpro(beanClass, value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder bpro(Map<String,String> values) {
		super.bpro(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder bpro(String beanClassName, String value) {
		super.bpro(beanClassName, value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder bpwo(Class<?> beanClass, String properties) {
		super.bpwo(beanClass, properties);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder bpwo(Map<String,String> values) {
		super.bpwo(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder bpwo(String beanClassName, String value) {
		super.bpwo(beanClassName, value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder debug() {
		debug(Enablement.TRUE);
		super.debug();
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder dictionary(Class<?>...values) {
		super.dictionary(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder dictionary(Object...values) {
		super.dictionary(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder dictionaryReplace(Class<?>...values) {
		super.dictionaryReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder dictionaryReplace(Object...values) {
		super.dictionaryReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder dictionaryRemove(Class<?>...values) {
		super.dictionaryRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder dictionaryRemove(Object...values) {
		super.dictionaryRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public <T> RestContextBuilder example(Class<T> c, T o) {
		super.example(c, o);
		return this;
	}

	@Override /* BeanContextBuilder */
	public <T> RestContextBuilder exampleJson(Class<T> c, String value) {
		super.exampleJson(c, value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder ignoreInvocationExceptionsOnGetters(boolean value) {
		super.ignoreInvocationExceptionsOnGetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder ignoreInvocationExceptionsOnGetters() {
		super.ignoreInvocationExceptionsOnGetters();
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder ignoreInvocationExceptionsOnSetters(boolean value) {
		super.ignoreInvocationExceptionsOnSetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder ignoreInvocationExceptionsOnSetters() {
		super.ignoreInvocationExceptionsOnSetters();
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder ignorePropertiesWithoutSetters(boolean value) {
		super.ignorePropertiesWithoutSetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder ignoreTransientFields(boolean value) {
		super.ignoreTransientFields(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder ignoreUnknownBeanProperties(boolean value) {
		super.ignoreUnknownBeanProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder ignoreUnknownBeanProperties() {
		super.ignoreUnknownBeanProperties();
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder ignoreUnknownNullBeanProperties(boolean value) {
		super.ignoreUnknownNullBeanProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder implClass(Class<?> interfaceClass, Class<?> implClass) {
		super.implClass(interfaceClass, implClass);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder implClasses(Map<String,Class<?>> values) {
		super.implClasses(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder mediaType(MediaType value) {
		super.mediaType(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder notBeanClasses(Class<?>...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder notBeanClasses(Object...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder notBeanClassesReplace(Class<?>...values) {
		super.notBeanClassesReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder notBeanClassesReplace(Object...values) {
		super.notBeanClassesReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder notBeanClassesRemove(Class<?>...values) {
		super.notBeanClassesRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder notBeanClassesRemove(Object...values) {
		super.notBeanClassesRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder notBeanPackages(Object...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder notBeanPackages(String...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder notBeanPackagesReplace(String...values) {
		super.notBeanPackagesReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder notBeanPackagesReplace(Object...values) {
		super.notBeanPackagesReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder notBeanPackagesRemove(String...values) {
		super.notBeanPackagesRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder notBeanPackagesRemove(Object...values) {
		super.notBeanPackagesRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder pojoSwaps(Class<?>...values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder pojoSwaps(Object...values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder pojoSwapsReplace(Class<?>...values) {
		super.pojoSwapsReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder pojoSwapsReplace(Object...values) {
		super.pojoSwapsReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder pojoSwapsRemove(Class<?>...values) {
		super.pojoSwapsRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder pojoSwapsRemove(Object...values) {
		super.pojoSwapsRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder sortProperties(boolean value) {
		super.sortProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder sortProperties() {
		super.sortProperties();
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder timeZone(TimeZone value) {
		super.timeZone(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder useEnumNames(boolean value) {
		super.useEnumNames(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder useEnumNames() {
		super.useEnumNames();
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder useInterfaceProxies(boolean value) {
		super.useInterfaceProxies(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder useJavaBeanIntrospector(boolean value) {
		super.useJavaBeanIntrospector(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder useJavaBeanIntrospector() {
		super.useJavaBeanIntrospector();
		return this;
	}

	@Override /* ContextBuilder */
	public RestContextBuilder annotations(Annotation...values) {
		super.annotations(values);
		return this;
	}

	@Override /* ContextBuilder */
	public RestContextBuilder set(String name, Object value) {
		super.set(name, value);
		this.properties.put(name, value);
		addTo(REST_properties, name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public RestContextBuilder set(Map<String,Object> properties) {
		super.set(properties);
		this.properties.clear();
		this.properties.putAll(properties);
		addTo(REST_properties, properties);
		return this;
	}

	@Override /* ContextBuilder */
	public RestContextBuilder add(Map<String,Object> properties) {
		super.add(properties);
		return this;
	}

	@Override /* ContextBuilder */
	public RestContextBuilder addTo(String name, Object value) {
		super.addTo(name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public RestContextBuilder addTo(String name, String key, Object value) {
		super.addTo(name, key, value);
		return this;
	}

	@Override /* ContextBuilder */
	public RestContextBuilder removeFrom(String name, Object value) {
		super.removeFrom(name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public RestContextBuilder apply(PropertyStore copyFrom) {
		super.apply(copyFrom);
		return this;
	}

	@Override /* ContextBuilder */
	public RestContextBuilder applyAnnotations(AnnotationList al, VarResolverSession vrs) {
		super.applyAnnotations(al, vrs);
		return this;
	}

	@Override /* ContextBuilder */
	public RestContextBuilder applyAnnotations(Class<?>...fromClasses) {
		super.applyAnnotations(fromClasses);
		return this;
	}

	@Override /* ContextBuilder */
	public RestContextBuilder applyAnnotations(java.lang.reflect.Method...fromMethods) {
		super.applyAnnotations(fromMethods);
		return this;
	}

	//----------------------------------------------------------------------------------------------------
	// Methods inherited from ServletConfig
	//----------------------------------------------------------------------------------------------------

	@Override /* ServletConfig */
	public String getInitParameter(String name) {
		return inner.getInitParameter(name);
	}

	@Override /* ServletConfig */
	public Enumeration<String> getInitParameterNames() {
		return inner.getInitParameterNames();
	}

	@Override /* ServletConfig */
	public ServletContext getServletContext() {
		return inner.getServletContext();
	}

	@Override /* ServletConfig */
	public String getServletName() {
		return inner.getServletName();
	}
}
