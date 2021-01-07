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

import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.nio.charset.*;
import java.util.*;

import javax.servlet.*;

import org.apache.juneau.*;
import org.apache.juneau.config.*;
import org.apache.juneau.config.vars.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.exception.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.logging.*;
import org.apache.juneau.rest.reshandlers.*;
import org.apache.juneau.rest.util.RestUtils;
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

	RestContextBuilder(ServletConfig servletConfig, Class<?> resourceClass, RestContext parentContext) throws ServletException {
		this.inner = servletConfig;
		this.resourceClass = resourceClass;
		this.parentContext = parentContext;
		this.properties = new RestContextProperties();

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

		try {

			varResolverBuilder = new VarResolverBuilder()
				.defaultVars()
				.vars(ConfigVar.class)
				.vars(FileVar.class)
				.contextObject("crm", FileFinder.create().cp(resourceClass,null,true).build());

			VarResolver vr = varResolverBuilder.build();

			List<AnnotationInfo<Rest>> restAnnotationsParentFirst = rci.getAnnotationInfos(Rest.class);

			// Find our config file.  It's the last non-empty @RestResource(config).
			String configPath = "";
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

			if (parentContext != null) {
				set(REST_callLoggerDefault, parentContext.getProperty(REST_callLoggerDefault));
				set(REST_debugDefault, parentContext.getProperty(REST_debugDefault));
				set(REST_staticFilesDefault, parentContext.getProperty(REST_staticFilesDefault));
				set(REST_fileFinderDefault, parentContext.getProperty(REST_fileFinderDefault));
			}

			applyAnnotations(rci.getAnnotationList(ConfigAnnotationFilter.INSTANCE), vr.createSession());

			// Load stuff from parent-to-child order.
			// This allows child settings to overwrite parent settings.
			for (AnnotationInfo<Rest> e : restAnnotationsParentFirst) {
				Rest r = e.getAnnotation();
				for (Property p : r.properties())
					set(vr.resolve(p.name()), vr.resolve(p.value()));
				for (String p : r.flags())
					set(p);
			}

		} catch (Exception e) {
			throw new ServletException(e);
		}
	}

	@Override /* BeanContextBuilder */
	public RestContext build() {
		try {
			PropertyStore ps = getPropertyStore();
			Class<? extends RestContext> c = ps.getClassProperty(REST_context, RestContext.class, RestContext.class);
			ConstructorInfo ci = ClassInfo.of(c).getConstructor(Visibility.PUBLIC, RestContextBuilder.class);
			if (ci == null)
				throw new InternalServerError("Invalid class specified for REST_context.  Must extend from RestContext and provide a public constructor of the form T(RestContextBuilder).");
			return ci.invoke(this);
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
				throw new RestServletException(e, "Exception thrown from @RestHook(INIT) method {0}.{0}.", m.getDeclaringClass().getSimpleName(), m.getSignature());
			}
		}
		return this;
	}

	private static void assertArgsOnlyOfType(MethodInfo m, Class<?>...args) {
		if (! m.argsOnlyOfType(args))
			throw new BasicIllegalArgumentException("Invalid arguments passed to method {0}.  Only arguments of type {1} are allowed.", m, args);
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
		return p == null ? "" : p.toString();
	}

	//----------------------------------------------------------------------------------------------------
	// Properties
	//----------------------------------------------------------------------------------------------------

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
	@FluentSetter
	public RestContextBuilder clientVersionHeader(String value) {
		return set(REST_clientVersionHeader, value);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  REST context class.
	 *
	 * <review>NEEDS REVIEW</review>
	 * <p>
	 * Allows you to extend the {@link RestContext} class to modify how any of the methods are implemented.
	 *
	 * <p>
	 * The subclass must provide the following:
	 * <ul>
	 * 	<li>A public constructor that takes in one parameter that should be passed to the super constructor:  {@link RestContextBuilder}.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Our REST class</jc>
	 * 	<ja>@Rest</ja>(context=MyRestContext.<jk>class</jk>)
	 * 	<jk>public class</jk> MyResource {
	 * 		...
	 * 	}
	 * </p>
	 * <p class='bcode w800'>
	 * 	<ja>@Rest</ja>
	 * 	<jk>public class</jk> MyResource {
	 * 		...
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
	 * 			<jv>builder</jv>.context(MyRestContext.<jk>class</jk>);
	 * 		}
	 * 	}
	 * </p>
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestContextBuilder context(Class<? extends RestContext> value) {
		return set(REST_context, value);
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
	@FluentSetter
	public RestContextBuilder defaultContentType(String value) {
		if (isNotEmpty(value))
			reqHeader("Content-Type", value);
		return this;
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
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  REST info provider.
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
	@FluentSetter
	public RestContextBuilder infoProvider(Class<? extends RestInfoProvider> value) {
		return set(REST_infoProvider, value);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  REST info provider.
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
	@FluentSetter
	public RestContextBuilder infoProvider(RestInfoProvider value) {
		return set(REST_infoProvider, value);
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
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Java method parameter resolvers.
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
	@FluentSetter
	@SuppressWarnings("unchecked")
	public RestContextBuilder paramResolvers(Class<? extends RestMethodParam>...values) {
		return prependTo(REST_paramResolvers, values);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Java method parameter resolvers.
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
	@FluentSetter
	public RestContextBuilder paramResolvers(RestMethodParam...values) {
		return prependTo(REST_paramResolvers, values);
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
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Default request attribute.
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
	@FluentSetter
	public RestContextBuilder reqAttr(String name, Object value) {
		return putTo(REST_reqAttrs, name, value);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Default request attributes.
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
	@FluentSetter
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
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Default request headers.
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
	@FluentSetter
	public RestContextBuilder reqHeader(String name, Object value) {
		return putTo(REST_reqHeaders, name, value);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Default request headers.
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
	@FluentSetter
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
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Default response headers.
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
	@FluentSetter
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
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Default response headers.
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
	@FluentSetter
	public RestContextBuilder resHeader(String name, Object value) {
		return putTo(REST_resHeaders, name, value);
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
	@FluentSetter
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
	@FluentSetter
	public RestContextBuilder resourceResolver(RestResourceResolver value) {
		return set(REST_resourceResolver, value);
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
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Properties.
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
	@FluentSetter
	public RestContextBuilder restProperties(Map<String,Object> values) {
		return putAllTo(REST_properties, values);
	}

	/**
	 * <i><l>RestContext</l> configuration property:&emsp;</i>  Properties.
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
	@FluentSetter
	public RestContextBuilder property(String name, Object value) {
		return putTo(REST_properties, name, value);
	}

	/**
	 * Configuration property:  Static files finder.
	 *
	 * <p>
	 * Used to retrieve localized files to be served up as static files through the REST API via the following
	 * predefined methods:
	 * <ul class='javatree'>
	 * 	<li class='jm'>{@link BasicRest#getHtdoc(String, Locale)}.
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
	 * 	<li class='jm'>{@link BasicRest#getHtdoc(String, Locale)}.
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
		this.properties.put(name, value);
		putTo(REST_properties, name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public RestContextBuilder set(String name) {
		super.set(name);
		this.properties.put(name, true);
		putTo(REST_properties, name, true);
		return this;
	}

	@Override /* ContextBuilder */
	public RestContextBuilder set(Map<String,Object> properties) {
		super.set(properties);
		this.properties.clear();
		this.properties.putAll(properties);
		putAllTo(REST_properties, properties);
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
	public RestContextBuilder apply(PropertyStore copyFrom) {
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
