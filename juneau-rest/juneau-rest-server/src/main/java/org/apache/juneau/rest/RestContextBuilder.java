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

import static org.apache.juneau.internal.ArrayUtils.*;
import static org.apache.juneau.internal.ReflectionUtils.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.rest.RestContext.*;
import static org.apache.juneau.serializer.Serializer.*;
import static org.apache.juneau.parser.Parser.*;

import java.lang.reflect.Method;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.juneau.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.encoders.Encoder;
import org.apache.juneau.http.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.ini.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.response.*;
import org.apache.juneau.rest.widget.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.svl.vars.*;
import org.apache.juneau.utils.*;

/**
 * Defines the initial configuration of a <code>RestServlet</code> or <code>@RestResource</code> annotated object.
 *
 * <p>
 * An extension of the {@link ServletConfig} object used during servlet initialization.
 *
 * <p>
 * Provides access to the following initialized resources:
 * <ul>
 * 	<li>{@link #getConfigFile()} - The external configuration file for this resource.
 * 	<li>{@link #getProperties()} - The modifiable configuration properties for this resource.
 * 	<li>{@link #getVarResolverBuilder()} - The variable resolver for this resource.
 * </ul>
 *
 * <p>
 * Methods are provided for overriding or augmenting the information provided by the <ja>@RestResource</ja> annotation.
 * In general, most information provided in the <ja>@RestResource</ja> annotation can be specified programmatically
 * through calls on this object.
 *
 * <p>
 * To interact with this object, simply implement the following init method in your resource class:
 * <p class='bcode'>
 * 	<jk>public synchronized void</jk> init(RestContextBuilder builder) <jk>throws</jk> Exception {
 * 		builder
 * 			.pojoSwaps(CalendarSwap.<jsf>RFC2822DTZ</jsf>.<jk>class</jk>)
 * 			.set(<jsf>PARSER_debug</jsf>, <jk>true</jk>);
 * 		<jk>super</jk>.init(builder); <jc>// Make sure this is the last line! (or just leave it out entirely)</jc>
 * 	}
 * </p>
 *
 * <p>
 * Note that this method is identical to {@link HttpServlet#init(ServletConfig)} except you get access to
 * this object instead.  Also, this method can throw any exception, not just a {@link ServletException}.
 *
 * <p>
 * The parent <code>init(RestServletConfig)</code> method will construct a read-only {@link RestContext} object
 * that contains a snapshot of these settings.  If you call <code><jk>super</jk>.init(RestServletConfig)</code> before
 * you modify this config object, you won't see the changes!
 */
public class RestContextBuilder extends BeanContextBuilder implements ServletConfig {

	final ServletConfig inner;
	
	Class<?> resourceClass;
	Object resource;
	ServletContext servletContext;
	RestContext parentContext;

	//---------------------------------------------------------------------------
	// The following fields are meant to be modifiable.
	// They should not be declared final.
	// Read-only snapshots of these will be made in RestServletContext.
	//---------------------------------------------------------------------------

	ObjectMap properties;
	ConfigFile configFile;
	VarResolverBuilder varResolverBuilder;
	String path;
	HtmlDocBuilder htmlDocBuilder;

	/**
	 * Constructor for top-level servlets when using dependency injection.
	 *
	 * <p>
	 * Work-in-progress.
	 *
	 * @param config
	 * 	The servlet config object we're extending.
	 * @param resourceClass
	 * 	The class annotated with the {@link RestResource @RestResource} annotation.
	 * @throws ServletException
	 */
	public RestContextBuilder(ServletConfig config, Class<?> resourceClass) throws ServletException {
		this(config, resourceClass, null);
	}

	/**
	 * Constructor.
	 *
	 * @param config The servlet config passed into the servlet by the servlet container.
	 * @param resource The class annotated with <ja>@RestResource</ja>.
	 * @throws ServletException Something bad happened.
	 */
	RestContextBuilder(ServletConfig config, Class<?> resourceClass, RestContext parentContext) throws ServletException {
		this.inner = config;
		this.resourceClass = resourceClass;
		this.parentContext = parentContext;
		
		logger(RestLogger.Normal.class);
		staticFileResponseHeader("Cache-Control", "max-age=86400, public");
		encoders(IdentityEncoder.INSTANCE);

		try {

			ConfigFileBuilder cfb = new ConfigFileBuilder();

			properties = new ObjectMap();
			htmlDocBuilder = new HtmlDocBuilder(properties);
			configFile = cfb.build();
			varResolverBuilder = new VarResolverBuilder()
				.vars(
					SystemPropertiesVar.class,
					EnvVariablesVar.class,
					ConfigFileVar.class,
					IfVar.class,
					SwitchVar.class,
					CoalesceVar.class,
					CoalesceAndRecurseVar.class
				);

			VarResolver vr = varResolverBuilder.build();

			Map<Class<?>,RestResource> restResourceAnnotationsParentFirst = findAnnotationsMapParentFirst(RestResource.class, resourceClass);

			// Find our config file.  It's the last non-empty @RestResource.config().
			String configPath = "";
			for (RestResource r : restResourceAnnotationsParentFirst.values())
				if (! r.config().isEmpty())
					configPath = r.config();
			String cf = vr.resolve(configPath);
			if (! cf.isEmpty())
				configFile = cfb.build(cf);
			configFile = configFile.getResolving(vr);

			// Add our config file to the variable resolver.
			varResolverBuilder.contextObject(ConfigFileVar.SESSION_config, configFile);
			vr = varResolverBuilder.build();

			// Add the servlet init parameters to our properties.
			for (Enumeration<String> ep = config.getInitParameterNames(); ep.hasMoreElements();) {
				String p = ep.nextElement();
				String initParam = config.getInitParameter(p);
				properties.put(vr.resolve(p), vr.resolve(initParam));
			}

			// Load stuff from parent-to-child order.
			// This allows child settings to overwrite parent settings.
			for (Map.Entry<Class<?>,RestResource> e : restResourceAnnotationsParentFirst.entrySet()) {
				Class<?> c = e.getKey();
				RestResource r = e.getValue();
				for (Property p : r.properties())
					properties.append(vr.resolve(p.name()), vr.resolve(p.value()));
				for (String p : r.flags())
					properties.append(p, true);
				serializers(r.serializers());
				parsers(r.parsers());
				encoders(r.encoders());
				if (r.supportedAcceptTypes().length > 0)
					supportedAcceptTypes(false, resolveVars(vr, r.supportedAcceptTypes()));
				if (r.supportedContentTypes().length > 0)
					supportedContentTypes(false, resolveVars(vr, r.supportedContentTypes()));
				defaultRequestHeaders(resolveVars(vr, r.defaultRequestHeaders()));
				defaultResponseHeaders(resolveVars(vr, r.defaultResponseHeaders()));
				responseHandlers(r.responseHandlers());
				converters(r.converters());
				guards(reverse(r.guards()));
				children(r.children());
				beanFilters((Object[])r.beanFilters());
				pojoSwaps(r.pojoSwaps());
				paramResolvers(r.paramResolvers());
				if (r.serializerListener() != SerializerListener.Null.class)
					serializerListener(r.serializerListener());
				if (r.parserListener() != ParserListener.Null.class)
					parserListener(r.parserListener());
				contextPath(vr.resolve(r.contextPath()));
				for (String mapping : r.staticFiles())
					staticFiles(c, vr.resolve(mapping));
				if (! r.messages().isEmpty())
					messages(c, vr.resolve(r.messages()));
				staticFileResponseHeaders(resolveVars(vr, r.staticFileResponseHeaders()));
				if (! r.useClasspathResourceCaching().isEmpty())
					useClasspathResourceCaching(Boolean.valueOf(vr.resolve(r.useClasspathResourceCaching())));
				if (r.classpathResourceFinder() != ClasspathResourceFinder.Null.class)
					classpathResourceFinder(r.classpathResourceFinder());
				if (! r.path().isEmpty())
					path(vr.resolve(r.path()));
				if (! r.clientVersionHeader().isEmpty())
					clientVersionHeader(vr.resolve(r.clientVersionHeader()));
				if (r.resourceResolver() != RestResourceResolver.class)
					resourceResolver(r.resourceResolver());
				if (r.logger() != RestLogger.Normal.class)
					logger(r.logger());
				if (r.callHandler() != RestCallHandler.class)
					callHandler(r.callHandler());
				if (r.infoProvider() != RestInfoProvider.class)
					infoProvider(r.infoProvider());
				if (! r.allowHeaderParams().isEmpty())
					allowHeaderParams(Boolean.valueOf(vr.resolve(r.allowHeaderParams())));
				if (! r.allowedMethodParams().isEmpty())
					allowedMethodParams(vr.resolve(r.allowedMethodParams()));
				if (! r.allowBodyParam().isEmpty())
					allowBodyParam(Boolean.valueOf(vr.resolve(r.allowBodyParam())));
				if (! r.renderResponseStackTraces().isEmpty())
					renderResponseStackTraces(Boolean.valueOf(vr.resolve(r.renderResponseStackTraces())));
				if (! r.useStackTraceHashes().isEmpty())
					useStackTraceHashes(Boolean.valueOf(vr.resolve(r.useStackTraceHashes())));
				if (! r.defaultCharset().isEmpty())
					defaultCharset(vr.resolve(r.defaultCharset()));
				if (! r.maxInput().isEmpty())
					maxInput(vr.resolve(r.maxInput()));
				mimeTypes(resolveVars(vr, r.mimeTypes()));

				HtmlDoc hd = r.htmldoc();
				widgets(hd.widgets());
				htmlDocBuilder.process(hd);
			}

			responseHandlers(
				StreamableHandler.class,
				WritableHandler.class,
				ReaderHandler.class,
				InputStreamHandler.class,
				RedirectHandler.class,
				DefaultHandler.class
			);

		} catch (Exception e) {
			throw new ServletException(e);
		}
	}
	
	private static String[] resolveVars(VarResolver vr, String[] in) {
		String[] out = new String[in.length];
		for (int i = 0; i < in.length; i++) 
			out[i] = vr.resolve(in[i]);
		return out;
	}

	/*
	 * Calls all @RestHook(INIT) methods on the specified resource object.
	 */
	void init(Object resource) throws ServletException {
		this.resource = resource;

		// Once we have the resource object, we can construct the Widgets.
		// We want to do that here so that we can update the script/style properties while they're still modifiable.
		HtmlDocBuilder hdb = getHtmlDocBuilder();
		PropertyStore ps = getPropertyStore();
		Widget[] widgets = ps.getInstanceArrayProperty(REST_widgets, Widget.class, new Widget[0], true, ps, resource);
		for (Widget w : widgets) {
			hdb.script("INHERIT", "$W{"+w.getName()+".script}");
			hdb.style("INHERIT", "$W{"+w.getName()+".style}");
		}
		widgets(false, widgets);
		
		Map<String,Method> map = new LinkedHashMap<>();
		for (Method m : ClassUtils.getAllMethods(this.resourceClass, true)) {
			if (m.isAnnotationPresent(RestHook.class) && m.getAnnotation(RestHook.class).value() == HookEvent.INIT) {
				Visibility.setAccessible(m);
				String sig = ClassUtils.getMethodSignature(m);
				if (! map.containsKey(sig))
					map.put(sig, m);
			}
		}
		for (Method m : map.values()) {
			ClassUtils.assertArgsOfType(m, RestContextBuilder.class, ServletConfig.class);
			Class<?>[] argTypes = m.getParameterTypes();
			Object[] args = new Object[argTypes.length];
			for (int i = 0; i < args.length; i++) {
				if (argTypes[i] == RestContextBuilder.class)
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
	 * For example, the {@link ConfigFileVar} needs access to this resource's {@link ConfigFile} through the
	 * {@link ConfigFileVar#SESSION_config} object that can be specified as either a session object (temporary) or
	 * context object (permanent).
	 * In this case, we call the following code to add it to the context map:
	 * <p class='bcode'>
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
	 * By default, the config file is determined using the {@link RestResource#config() @RestResource.config()}
	 * annotation.
	 * This method allows you to programmatically override it with your own custom config file.
	 *
	 * @param configFile The new config file.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder configFile(ConfigFile configFile) {
		this.configFile = configFile;
		return this;
	}

	/**
	 * Sets a property on this resource.
	 *
	 * <p>
	 * This is the programmatic equivalent to the {@link RestResource#properties()} annotation.
	 *
	 * @param key The property name.
	 * @param value The property value.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder setProperty(String key, Object value) {
		this.properties.put(key, value);
		return this;
	}

	/**
	 * Sets multiple properties on this resource.
	 *
	 * <p>
	 * This is the programmatic equivalent to the {@link RestResource#properties() @RestResource.properties()} annotation.
	 *
	 * <p>
	 * Values in the map are added to the existing properties and are overwritten if duplicates are found.
	 *
	 * @param properties The new properties to add to this config.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder setProperties(Map<String,Object> properties) {
		this.properties.putAll(properties);
		return this;
	}

	/**
	 * Returns an instance of an HTMLDOC builder for setting HTMLDOC-related properties.
	 * 
	 * @return An instance of an HTMLDOC builder for setting HTMLDOC-related properties.
	 */
	public HtmlDocBuilder getHtmlDocBuilder() {
		return htmlDocBuilder;
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
	 * The configuration file location is determined via the {@link RestResource#config() @RestResource.config()}
	 * annotation on the resource.
	 *
	 * <p>
	 * The config file can be programmatically overridden by adding the following method to your resource:
	 * <p class='bcode'>
	 * 	<jk>public</jk> ConfigFile createConfigFile(ServletConfig servletConfig) <jk>throws</jk> ServletException;
	 * </p>
	 *
	 * <p>
	 * If a config file is not set up, then an empty config file will be returned that is not backed by any file.
	 *
	 * @return The external config file for this resource.  Never <jk>null</jk>.
	 */
	public ConfigFile getConfigFile() {
		return configFile;
	}

	/**
	 * Returns the configuration properties for this resource.
	 *
	 * <p>
	 * The configuration properties are determined via the {@link RestResource#properties()} annotation on the resource.
	 *
	 * <p>
	 * The configuration properties can be augmented programmatically by adding the following method to your resource:
	 * <p class='bcode'>
	 * 	<jk>public</jk> ObjectMap createProperties(ServletConfig servletConfig) <jk>throws</jk> ServletException;
	 * </p>
	 *
	 * <p>
	 * These properties can be modified during servlet initialization.
	 * However, any modifications made after {@link RestServlet#init(ServletConfig)} has been called will have no effect.
	 *
	 * @return The configuration properties for this resource.  Never <jk>null</jk>.
	 */
	public ObjectMap getProperties() {
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
	 * 	<li>{@link ConfigFileVar}
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
	 * For example:  <js>"?body=(name='John%20Smith',age=45)"</js>
	 * 
	 * <p>
	 * See {@link RestContext#REST_allowBodyParam} for more information.
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder allowBodyParam(boolean value) {
		return set(REST_allowBodyParam, value);
	}

	/**
	 * Configuration property:  Allowed method parameters.
	 *
	 * <p>
	 * When specified, the HTTP method can be overridden by passing in a <js>"method"</js> URL parameter on a regular
	 * GET request.
	 * <br>
	 * For example:  <js>"?method=OPTIONS"</js>
	 * 
	 * <p>
	 * See {@link RestContext#REST_allowedMethodParams} for more information.
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder allowedMethodParams(String...value) {
		return set(REST_allowedMethodParams, StringUtils.join(value, ','));
	}

	/**
	 * Configuration property:  Allow header URL parameters.
	 *
	 * <p>
	 * When enabled, headers such as <js>"Accept"</js> and <js>"Content-Type"</js> to be passed in as URL query
	 * parameters.
	 * <br>For example:  <js>"?Accept=text/json&amp;Content-Type=text/json"</js>
	 * 
	 * <p>
	 * See {@link RestContext#REST_allowHeaderParams} for more information.
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder allowHeaderParams(boolean value) {
		return set(REST_allowHeaderParams, value);
	}

	/**
	 * Configuration property:  REST call handler.
	 *
	 * <p>
	 * This class handles the basic lifecycle of an HTTP REST call.
	 * <br>Subclasses can be used to customize how these HTTP calls are handled.
	 * 
	 * <p>
	 * See {@link RestContext#REST_callHandler} for more information.
	 *
	 * @param value The new value for this setting.
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
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder callHandler(RestCallHandler value) {
		return set(REST_callHandler, value);
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
	 * <p>
	 * See {@link RestContext#REST_children} for more information.
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
	 * This can be used for resources that don't have a {@link RestResource#path()} annotation.
	 * 
	 * <p>
	 * See {@link RestContext#REST_children} for more information.
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
	 * <p>
	 * See {@link RestContext#REST_classpathResourceFinder} for more information.
	 * 
	 * @param value The new value for this setting.
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
	 * @param value The new value for this setting.
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
	 * <br>Used in conjunction with {@link RestMethod#clientVersion()} annotation.
	 * 
	 * <p>
	 * See {@link RestContext#REST_clientVersionHeader} for more information.
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder clientVersionHeader(String value) {
		return set(REST_clientVersionHeader, value);
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
	 * The net effect is that the {@link RestRequest#getContextPath()} and {@link RestRequest#getServletPath()} methods
	 * will return this value instead of the actual context path of the web app.
	 * 
	 * <p>
	 * See {@link RestContext#REST_contextPath} for more information.
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder contextPath(String value) {
		if (! value.isEmpty())
			set(REST_contextPath, value);
		return this;
	}

	/**
	 * Configuration property:  Class-level response converters.
	 *
	 * <p>
	 * Associates one or more {@link RestConverter converters} with a resource class.
	 * These converters get called immediately after execution of the REST method in the same order specified in the
	 * annotation.
	 *
	 * <p>
	 * See {@link RestContext#REST_converters} for more information.
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
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder converters(RestConverter...values) {
		return addTo(REST_converters, values);
	}

	/**
	 * Configuration property:  Default character encoding.
	 * 
	 * <p>
	 * The default character encoding for the request and response if not specified on the request.
	 *
	 * <p>
	 * See {@link RestContext#REST_defaultCharset} for more information.
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder defaultCharset(String value) {
		return set(REST_defaultCharset, value);
	}

	/**
	 * Configuration property:  Default request headers.
	 *
	 * <p>
	 * Adds class-level default HTTP request headers to this resource.
	 *
	 * <p>
	 * See {@link RestContext#REST_defaultRequestHeaders} for more information.
	 *
	 * @param headers The headers in the format <js>"Header-Name: header-value"</js>.
	 * @return This object (for method chaining).
	 * @throws RestServletException If malformed header is found.
	 */
	public RestContextBuilder defaultRequestHeaders(String...headers) throws RestServletException {
		for (String header : headers) {
			String[] h = RestUtils.parseHeader(header);
			if (h == null)
				throw new RestServletException("Invalid default request header specified: ''{0}''.  Must be in the format: ''Header-Name: header-value''", header);
			defaultRequestHeader(h[0], h[1]);
		}
		return this;
	}

	/**
	 * Configuration property:  Default request headers.
	 *
	 * <p>
	 * Same as {@link #defaultRequestHeaders(String...)} but adds a single header name/value pair.
	 *
	 * @param name The HTTP header name.
	 * @param value The HTTP header value.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder defaultRequestHeader(String name, Object value) {
		return addTo(REST_defaultRequestHeaders, name, value);
	}

	/**
	 * Configuration property:  Default response headers.
	 *
	 * <p>
	 * Specifies default values for response headers.
	 *
	 * <p>
	 * See {@link RestContext#REST_defaultResponseHeaders} for more information.
	 *
	 * @param headers The headers in the format <js>"Header-Name: header-value"</js>.
	 * @return This object (for method chaining).
	 * @throws RestServletException If malformed header is found.
	 */
	public RestContextBuilder defaultResponseHeaders(String...headers) throws RestServletException {
		for (String header : headers) {
			String[] h = RestUtils.parseHeader(header);
			if (h == null)
				throw new RestServletException("Invalid default response header specified: ''{0}''.  Must be in the format: ''Header-Name: header-value''", header);
			defaultResponseHeader(h[0], h[1]);
		}
		return this;
	}

	/**
	 * Configuration property:  Default response headers.
	 *
	 * <p>
	 * Same as {@link #defaultResponseHeaders(String...)} but adds a single header name/value pair.
	 *
	 * @param name The HTTP header name.
	 * @param value The HTTP header value.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder defaultResponseHeader(String name, Object value) {
		return addTo(REST_defaultResponseHeaders, name, value);
	}
	
	/**
	 * Configuration property:  Compression encoders. 
	 *
	 * <p>
	 * These can be used to enable various kinds of compression (e.g. <js>"gzip"</js>) on requests and responses.
	 *
	 * <p>
	 * See {@link RestContext#REST_encoders} for more information.
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
	 * These guards get called immediately before execution of any REST method in this class.
	 *
	 * <p>
	 * Typically, guards will be used for permissions checking on the user making the request, but it can also be used
	 * for other purposes like pre-call validation of a request.
	 *
	 * <p>
	 * See {@link RestContext#REST_guards} for more information.
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
	 * <p>
	 * See {@link RestContext#REST_infoProvider} for more information.
	 *
	 * @param value The new value for this setting.
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
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder infoProvider(RestInfoProvider value) {
		return set(REST_infoProvider, value);
	}

	/**
	 * Configuration property:  REST logger.
	 * 
	 * <p>
	 * Specifies the logger to use for logging.
	 *
	 * <p>
	 * See {@link RestContext#REST_logger} for more information.
	 *
	 * @param value The new value for this setting.  Can be <jk>null</jk> to disable logging.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder logger(Class<? extends RestLogger> value) {
		return set(REST_logger, value);
	}

	/**
	 * Configuration property:  REST logger.
	 * 
	 * <p>
	 * Same as {@link #logger(Class)} except input is a pre-constructed instance.
	 *
	 * @param value The new value for this setting.  Can be <jk>null</jk> to disable logging.
	 * @return This object (for method chaining).
	 */
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
	 * <p>
	 * See {@link RestContext#REST_maxInput} for more information.
	 *
	 * @param value The new value for this setting.
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
	 * <p>
	 * See {@link RestContext#REST_messages} for more information.
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
	 * <p>
	 * See {@link RestContext#REST_mimeTypes} for more information.
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
	 * <code>RestRequest</code>, <code>Accept</code>, <code>Reader</code>).
	 * This annotation allows you to provide your own resolvers for your own class types that you want resolved.
	 *
	 * <p>
	 * See {@link RestContext#REST_paramResolvers} for more information.
	 *
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings("unchecked")
	public RestContextBuilder paramResolvers(Class<? extends RestParam>...values) {
		return addTo(REST_paramResolvers, values);
	}

	/**
	 * Configuration property:  Java method parameter resolvers.
	 *
	 * <p>
	 * Same as {@link #paramResolvers(Class...)} except input is pre-constructed instances.
	 *
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder paramResolvers(RestParam...values) {
		return addTo(REST_paramResolvers, values);
	}

	/**
	 * Configuration property:  Parser listener.
	 * 
	 * <p>
	 * Specifies the parser listener class to use for listening to non-fatal parsing errors.
	 *
	 * <p>
	 * See {@link Parser#PARSER_listener} for more information.
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder parserListener(Class<? extends ParserListener> value) {
		return set(PARSER_listener, value);
	}

	/**
	 * Configuration property:  Parsers. 
	 *
	 * <p>
	 * Adds class-level parsers to this resource.
	 * 
	 * <p>
	 * See {@link RestContext#REST_parsers} for more information.
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
	 * Same as {@link #parsers(Class...)} except allows you to overwrite the previous value.
	 * 
	 * @param append
	 * 	If <jk>true</jk>, append to the existing list, otherwise overwrite the previous value. 
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder parsers(boolean append, Object...values) {
		return set(append, REST_parsers, values);
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
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder parsers(Object...values) {
		return addTo(REST_parsers, values);
	}

	/**
	 * Configuration property:  HTTP part parser. 
	 *
	 * <p>
	 * Specifies the {@link HttpPartParser} to use for parsing headers, query/form parameters, and URI parts.
	 * 
	 * <p>
	 * See {@link RestContext#REST_partParser} for more information.
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder partParser(Class<? extends HttpPartParser> value) {
		return set(REST_partParser, value);
	}

	/**
	 * Configuration property:  HTTP part parser. 
	 *
	 * <p>
	 * Same as {@link #partParser(Class)} except input is a pre-constructed instance.
	 *
	 * @param value The new value for this setting.
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
	 * <p>
	 * See {@link RestContext#REST_partSerializer} for more information.
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder partSerializer(Class<? extends HttpPartSerializer> value) {
		return set(REST_partSerializer, value);
	}

	/**
	 * Configuration property:  HTTP part serializer. 
	 *
	 * <p>
	 * Same as {@link #partSerializer(Class)} except input is a pre-constructed instance.
	 *
	 * @param value The new value for this setting.
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
	 * <p>
	 * See {@link RestContext#REST_path} for more information.
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder path(String value) {
		if (startsWith(value, '/'))
			value = value.substring(1);
		this.path = value;
		return this;
	}

	/**
	 * Configuration property:  Render response stack traces in responses.
	 *
	 * <p>
	 * Render stack traces in HTTP response bodies when errors occur.
	 * 
	 * <p>
	 * See {@link RestContext#REST_renderResponseStackTraces} for more information.
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder renderResponseStackTraces(boolean value) {
		return set(REST_renderResponseStackTraces, value);
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
	 * <p>
	 * See {@link RestContext#REST_resourceResolver} for more information.
	 *
	 * @param value The new value for this setting.
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
	 * @param value The new value for this setting.
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
	 * <p>
	 * See {@link RestContext#REST_responseHandlers} for more information.
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
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder responseHandlers(ResponseHandler...values) {
		return addTo(REST_responseHandlers, values);
	}

	/**
	 * Configuration property:  Serializer listener.
	 * 
	 * <p>
	 * Specifies the serializer listener class to use for listening to non-fatal serialization errors.
	 *
	 * <p>
	 * See {@link Serializer#SERIALIZER_listener} for more information.
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder serializerListener(Class<? extends SerializerListener> value) {
		return set(SERIALIZER_listener, value);
	}

	/**
	 * Configuration property:  Serializers. 
	 *
	 * <p>
	 * Adds class-level serializers to this resource.
	 * 
	 * <p>
	 * See {@link RestContext#REST_serializers} for more information.
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
	 * Same as {@link #serializers(Class...)} except allows you to overwrite the previous value.
	 * 
	 * @param append
	 * 	If <jk>true</jk>, append to the existing list, otherwise overwrite the previous value. 
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder serializers(boolean append, Object...values) {
		return set(append, REST_serializers, values);
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
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder serializers(Object...values) {
		return addTo(REST_serializers, values);
	}

	/**
	 * Configuration property:  Static file response headers. 
	 *
	 * <p>
	 * Used to customize the headers on responses returned for statically-served files.
	 * 
	 * <p>
	 * See {@link RestContext#REST_staticFileResponseHeaders} for more information.
	 * 
	 * @param append
	 * 	If <jk>true</jk>, append to the existing list, otherwise overwrite the previous value. 
	 * @param headers The headers to add to this list.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder staticFileResponseHeaders(boolean append, Map<String,String> headers) {
		return set(append, REST_staticFileResponseHeaders, headers);
	}

	/**
	 * Configuration property:  Static file response headers. 
	 *
	 * <p>
	 * Same as {@link #staticFileResponseHeaders(boolean, Map)} with append=<jk>true</jk> except headers are strings 
	 * composed of key/value pairs.
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
	 * <p>
	 * See {@link RestContext#REST_staticFiles} for more information.
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
	 * @param mappingString The static file mapping string.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder staticFiles(String mappingString) {
		return staticFiles(new StaticFileMapping(resourceClass, mappingString));
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
	 * @param baseClass 
	 * 	Overrides the default class to use for retrieving the classpath resource. 
	 * 	<br>If <jk>null<jk>, uses the REST resource class.
	 * @param mappingString The static file mapping string.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder staticFiles(Class<?> baseClass, String mappingString) {
		return staticFiles(new StaticFileMapping(baseClass, mappingString));
	}
	
	/**
	 * Configuration property:  Static file mappings. 
	 *
	 * <p>
	 * Same as {@link #staticFiles(String)} except path and location are already split values.
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
		return staticFiles(new StaticFileMapping(null, path, location, null));
	}

	/**
	 * Configuration property:  Static file mappings. 
	 *
	 * <p>
	 * Same as {@link #staticFiles(String,String)} except overrides the base class for retrieving the resource.
	 * 
	 * @param baseClass 
	 * 	Overrides the default class to use for retrieving the classpath resource. 
	 * 	<br>If <jk>null<jk>, uses the REST resource class.
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
	 * <p>
	 * See {@link RestContext#REST_supportedAcceptTypes} for more information.
	 *
	 * @param append
	 * 	If <jk>true</jk>, append to the existing list, otherwise overwrite the previous value. 
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder supportedAcceptTypes(boolean append, String...values) {
		return set(append, REST_supportedAcceptTypes, values);
	}

	/**
	 * Configuration property:  Supported accept media types.
	 *
	 * <p>
	 * Same as {@link #supportedAcceptTypes(boolean, String...)} except input is {@link MediaType} instances.
	 *
	 * @param append
	 * 	If <jk>true</jk>, append to the existing list, otherwise overwrite the previous value. 
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder supportedAcceptTypes(boolean append, MediaType...values) {
		return set(append, REST_supportedAcceptTypes, values);
	}

	/**
	 * Configuration property:  Supported content media types.
	 *
	 * <p>
	 * Overrides the media types inferred from the parsers that identify what media types can be consumed by the resource.
	 *
	 * <p>
	 * See {@link RestContext#REST_supportedContentTypes} for more information.
	 *
	 * @param append
	 * 	If <jk>true</jk>, append to the existing list, otherwise overwrite the previous value. 
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder supportedContentTypes(boolean append, String...values) {
		return set(append, REST_supportedContentTypes, values);
	}

	/**
	 * Configuration property:  Supported content media types.
	 *
	 * <p>
	 * Same as {@link #supportedContentTypes(boolean, String...)} except input is {@link MediaType} instances.
	 *
	 * @param append
	 * 	If <jk>true</jk>, append to the existing list, otherwise overwrite the previous value. 
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder supportedContentTypes(boolean append, MediaType...values) {
		return set(append, REST_supportedContentTypes, values);
	}

	/**
	 * Configuration property:  Use classpath resource caching. 
	 *
	 * <p>
	 * When enabled, resources retrieved via {@link RestContext#getClasspathResource(String, Locale)} (and related 
	 * methods) will be cached in memory to speed subsequent lookups.
	 * 
	 * <p>
	 * See {@link RestContext#REST_useClasspathResourceCaching} for more information.
	 * 
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder useClasspathResourceCaching(boolean value) {
		return set(REST_useClasspathResourceCaching, value);
	}

	/**
	 * Configuration property:  Use stack trace hashes.
	 *
	 * <p>
	 * When enabled, the number of times an exception has occurred will be determined based on stack trace hashsums,
	 * made available through the {@link RestException#getOccurrence()} method.
	 *
	 * <p>
	 * See {@link RestContext#REST_useStackTraceHashes} for more information.
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder useStackTraceHashes(boolean value) {
		return set(REST_useStackTraceHashes, value);
	}

	/**
	 * Configuration property:  HTML Widgets. 
	 *
	 * <p>
	 * Defines widgets that can be used in conjunction with string variables of the form <js>"$W{name}"</js>to quickly
	 * generate arbitrary replacement text.
	 * 
	 * <p>
	 * See {@link RestContext#REST_widgets} for more information.
	 *
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings("unchecked")
	public RestContextBuilder widgets(Class<? extends Widget>...values) {
		return addTo(REST_widgets, values);
	}
	
	/**
	 * Configuration property:  HTML Widgets. 
	 * 
	 * <p>
	 * Same as {@link #widgets(Class...)} except input is pre-constructed instances.
	 * 
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder widgets(Widget...values) {
		return addTo(REST_widgets, values);
	}

	/**
	 * Configuration property:  HTML Widgets. 
	 * 
	 * <p>
	 * Same as {@link #widgets(Widget...)} except allows you to overwrite the previous value.
	 * 
	 * @param append 
	 * 	If <jk>true</jk>, appends to the existing list of widgets.
	 * 	<br>Otherwise, replaces the previous list.
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder widgets(boolean append, Widget...values) {
		return set(append, REST_widgets, values);
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder beansRequireDefaultConstructor(boolean value) {
		super.beansRequireDefaultConstructor(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder beansRequireSerializable(boolean value) {
		super.beansRequireSerializable(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder beansRequireSettersForGetters(boolean value) {
		super.beansRequireSettersForGetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder beansRequireSomeProperties(boolean value) {
		super.beansRequireSomeProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder beanMapPutReturnsOldValue(boolean value) {
		super.beanMapPutReturnsOldValue(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder beanConstructorVisibility(Visibility value) {
		super.beanConstructorVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder beanClassVisibility(Visibility value) {
		super.beanClassVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder beanFieldVisibility(Visibility value) {
		super.beanFieldVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder methodVisibility(Visibility value) {
		super.methodVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder useJavaBeanIntrospector(boolean value) {
		super.useJavaBeanIntrospector(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder useInterfaceProxies(boolean value) {
		super.useInterfaceProxies(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder ignoreUnknownBeanProperties(boolean value) {
		super.ignoreUnknownBeanProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder ignoreUnknownNullBeanProperties(boolean value) {
		super.ignoreUnknownNullBeanProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder ignorePropertiesWithoutSetters(boolean value) {
		super.ignorePropertiesWithoutSetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder ignoreInvocationExceptionsOnGetters(boolean value) {
		super.ignoreInvocationExceptionsOnGetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder ignoreInvocationExceptionsOnSetters(boolean value) {
		super.ignoreInvocationExceptionsOnSetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder sortProperties(boolean value) {
		super.sortProperties(value);
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
	public RestContextBuilder notBeanPackages(boolean append, Object...values) {
		super.notBeanPackages(append, values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder notBeanPackagesRemove(Object...values) {
		super.notBeanPackagesRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder notBeanClasses(Object...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder notBeanClasses(Class<?>...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder notBeanClasses(boolean append, Object...values) {
		super.notBeanClasses(append, values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder notBeanClassesRemove(Object...values) {
		super.notBeanClassesRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder beanFilters(Object...values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder beanFilters(Class<?>...values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder beanFilters(boolean append, Object...values) {
		super.beanFilters(append, values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder beanFiltersRemove(Object...values) {
		super.beanFiltersRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder pojoSwaps(Object...values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder pojoSwaps(Class<?>...values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder pojoSwaps(boolean append, Object...values) {
		super.pojoSwaps(append, values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder pojoSwapsRemove(Object...values) {
		super.pojoSwapsRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder implClasses(Map<String,Class<?>> values) {
		super.implClasses(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public <T> RestContextBuilder implClass(Class<T> interfaceClass, Class<? extends T> implClass) {
		super.implClass(interfaceClass, implClass);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder includeProperties(Map<String,String> values) {
		super.includeProperties(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder includeProperties(String beanClassName, String properties) {
		super.includeProperties(beanClassName, properties);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder includeProperties(Class<?> beanClass, String properties) {
		super.includeProperties(beanClass, properties);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder excludeProperties(Map<String,String> values) {
		super.excludeProperties(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder excludeProperties(String beanClassName, String properties) {
		super.excludeProperties(beanClassName, properties);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder excludeProperties(Class<?> beanClass, String properties) {
		super.excludeProperties(beanClass, properties);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder beanDictionary(Object...values) {
		super.beanDictionary(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder beanDictionary(Class<?>...values) {
		super.beanDictionary(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder beanDictionary(boolean append, Object...values) {
		super.beanDictionary(append, values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder beanDictionaryRemove(Object...values) {
		super.beanDictionaryRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder beanTypePropertyName(String value) {
		super.beanTypePropertyName(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder timeZone(TimeZone value) {
		super.timeZone(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder mediaType(MediaType value) {
		super.mediaType(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder debug() {
		super.debug();
		return this;
	}

	@Override /* ContextBuilder */
	public RestContextBuilder set(String name, Object value) {
		super.set(name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public RestContextBuilder set(boolean append, String name, Object value) {
		super.set(append, name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public RestContextBuilder set(Map<String,Object> properties) {
		super.set(properties);
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

	@Override /* BeanContextBuilder */
	public BeanContext build() {
		// We don't actually generate bean context objects from this class yet.
		return null;
	}
}
