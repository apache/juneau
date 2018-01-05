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
import org.apache.juneau.rest.vars.*;
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

	List<Object> childResources = new ArrayList<>();
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
					SwitchVar.class
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
				childResources(r.children());
				beanFilters(r.beanFilters());
				pojoSwaps(r.pojoSwaps());
				paramResolvers(r.paramResolvers());
				if (r.serializerListener() != SerializerListener.Null.class)
					serializerListener(r.serializerListener());
				if (r.parserListener() != ParserListener.Null.class)
					parserListener(r.parserListener());
				contextPath(r.contextPath());
				for (String mapping : r.staticFiles())
					staticFiles(c, vr.resolve(mapping));
				staticFileResponseHeaders(resolveVars(vr, r.staticFileResponseHeaders()));
				if (! r.useClasspathResourceCaching().isEmpty())
					useClasspathResourceCaching(Boolean.valueOf(vr.resolve(r.useClasspathResourceCaching())));
				if (r.classpathResourceFinder() != ClasspathResourceFinder.Null.class)
					classpathResourceFinder(r.classpathResourceFinder());
				if (! r.path().isEmpty())
					path(r.path());
				if (! r.clientVersionHeader().isEmpty())
					clientVersionHeader(r.clientVersionHeader());
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
	 * By default, this config includes the following variables:
	 * <ul>
	 * 	<li>{@link SystemPropertiesVar}
	 * 	<li>{@link EnvVariablesVar}
	 * 	<li>{@link ConfigFileVar}
	 * 	<li>{@link IfVar}
	 * 	<li>{@link SwitchVar}
	 * </ul>
	 *
	 * <p>
	 * Later during the construction of {@link RestContext}, we add the following variables:
	 * <ul>
	 * 	<li>{@link LocalizationVar}
	 * 	<li>{@link RequestVar}
	 * 	<li>{@link SerializedRequestAttrVar}
	 * 	<li>{@link ServletInitParamVar}
	 * 	<li>{@link UrlVar}
	 * 	<li>{@link UrlEncodeVar}
	 * 	<li>{@link WidgetVar}
	 * </ul>
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
	 * Adds a child resource to this resource.
	 *
	 * <p>
	 * Child resources are resources that are accessed under the path of the parent resource.
	 *
	 * <p>
	 * This is the programmatic equivalent to the {@link RestResource#children() @RestResource.children()} annotation.
	 *
	 * @param path The child path of the resource.  Must conform to {@link RestResource#path()} format.
	 * @param child The child resource.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder childResource(String path, Object child) {
		this.childResources.add(new Pair<>(path, child));
		return this;
	}

	/**
	 * Add child resources to this resource.
	 *
	 * <p>
	 * Child resources are resources that are accessed under the path of the parent resource.
	 *
	 * <p>
	 * This is the programmatic equivalent to the {@link RestResource#children() @RestResource.children()} annotation.
	 *
	 * @param children The child resources to add to this resource.
	 * Children must be annotated with {@link RestResource#path()} to identify the child path.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder childResources(Object...children) {
		this.childResources.addAll(Arrays.asList(children));
		return this;
	}

	/**
	 * Add child resources to this resource.
	 *
	 * <p>
	 * Child resources are resources that are accessed under the path of the parent resource.
	 *
	 * <p>
	 * This is the programmatic equivalent to the {@link RestResource#children() @RestResource.children()} annotation.
	 *
	 * @param children The child resources to add to this resource.
	 * Children must be annotated with {@link RestResource#path()} to identify the child path.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder childResources(Class<?>...children) {
		this.childResources.addAll(Arrays.asList(children));
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
	 * <b>Configuration property:</b>  Resource path.   
	 *
	 * <p>
	 * Identifies the URL subpath relative to the parent resource.
	 *
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_path}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#path()}
	 * 		</ul> 
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#path(String)} 
	 * 		</ul>
	 * 	<li>This annotation is ignored on top-level servlets (i.e. servlets defined in <code>web.xml</code> files).
	 * 		<br>Therefore, implementers can optionally specify a path value for documentation purposes.
	 * 	<li>Typically, this setting is only applicable to resources defined as children through the 
	 * 		{@link RestResource#children()} annotation.
	 * 		<br>However, it may be used in other ways (e.g. defining paths for top-level resources in microservices).
	 *	</ul>
	 *
	 * @param path The URL path of this resource.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder path(String path) {
		if (startsWith(path, '/'))
			path = path.substring(1);
		this.path = path;
		return this;
	}

	/**
	 * <b>Configuration property:</b>  Resource context path. 
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
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_contextPath}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#contextPath()} 
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>@link RestContextBuilder#contextPath(String)} 
	 * 		</ul>
	 *	</ul>
	 *
	 * @param contextPath The context path for this resource and any child resources.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder contextPath(String contextPath) {
		if (! contextPath.isEmpty())
			set(REST_contextPath, contextPath);
		return this;
	}

	/**
	 * <b>Configuration property:</b>  Allow header URL parameters.
	 *
	 * <p>
	 * When enabled, headers such as <js>"Accept"</js> and <js>"Content-Type"</js> to be passed in as URL query
	 * parameters.
	 * <br>For example:  <js>"?Accept=text/json&amp;Content-Type=text/json"</js>
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_allowHeaderParams}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#allowHeaderParams()}
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#allowHeaderParams(boolean)}
	 * 		</ul>
	 * 	<li>Format is a comma-delimited list of HTTP method names that can be passed in as a method parameter.
	 * 	<li>Parameter name is case-insensitive.
	 * 	<li>Useful for debugging REST interface using only a browser.
	 *	</ul>
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder allowHeaderParams(boolean value) {
		return set(REST_allowHeaderParams, value);
	}

	/**
	 * <b>Configuration property:</b>  Allow body URL parameter.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RestContext.allowBodyParam.b"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>true</jk>
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 *
	 * <p>
	 * When enabled, the HTTP body content on PUT and POST requests can be passed in as text using the <js>"body"</js>
	 * URL parameter.
	 * <br>
	 * For example:  <js>"?body=(name='John%20Smith',age=45)"</js>
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_allowBodyParam}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#allowBodyParam()}
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#allowBodyParam(boolean)}
	 * 		</ul>
	 * 	<li>Parameter name is case-insensitive.
	 * 	<li>Useful for debugging PUT and POST methods using only a browser.
	 *	</ul>
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder allowBodyParam(boolean value) {
		return set(REST_allowBodyParam, value);
	}

	/**
	 * <b>Configuration property:</b>  Allowed method parameters.
	 *
	 * <p>
	 * When specified, the HTTP method can be overridden by passing in a <js>"method"</js> URL parameter on a regular
	 * GET request.
	 * <br>
	 * For example:  <js>"?method=OPTIONS"</js>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_allowedMethodParams}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#allowedMethodParams()}
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#allowedMethodParams(String...)}
	 * 		</ul>
	 * 	<li>Parameter name is case-insensitive.
	 * 	<li>Use "*" to represent all methods.
	 *	</ul>
	 *
	 * <p>
	 * Note that per the <a class="doclink"
	 * href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html">HTTP specification</a>, special care should
	 * be taken when allowing non-safe (POST, PUT, DELETE) methods to be invoked through GET requests.
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder allowedMethodParams(String...value) {
		return set(REST_allowedMethodParams, StringUtils.join(value, ','));
	}

	/**
	 * <b>Configuration property:</b>  Render response stack traces in responses.
	 *
	 * <p>
	 * Render stack traces in HTTP response bodies when errors occur.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_renderResponseStackTraces}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#renderResponseStackTraces()}
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#renderResponseStackTraces(boolean)}
	 * 		</ul>
	 * 	<li>Useful for debugging, although allowing stack traces to be rendered may cause security concerns so use
	 * 		caution when enabling.
	 *	</ul>
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder renderResponseStackTraces(boolean value) {
		return set(REST_renderResponseStackTraces, value);
	}

	/**
	 * <b>Configuration property:</b>  Use stack trace hashes.
	 *
	 * <p>
	 * When enabled, the number of times an exception has occurred will be determined based on stack trace hashsums,
	 * made available through the {@link RestException#getOccurrence()} method.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_useStackTraceHashes}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#useStackTraceHashes()} 
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#useStackTraceHashes(boolean)}
	 * 		</ul>
	 *	</ul>
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder useStackTraceHashes(boolean value) {
		return set(REST_useStackTraceHashes, value);
	}

	/**
	 * <b>Configuration property:</b>  Default character encoding.
	 * 
	 * <p>
	 * The default character encoding for the request and response if not specified on the request.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_defaultCharset}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#defaultCharset()}
					<li>{@link RestMethod#defaultCharset()}
				</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#defaultCharset(String)}
	 * 		</ul>
	 *	</ul>
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder defaultCharset(String value) {
		return set(REST_defaultCharset, value);
	}

	/**
	 * <b>Configuration property:</b>  The maximum allowed input size (in bytes) on HTTP requests.
	 *
	 * <p>
	 * Useful for alleviating DoS attacks by throwing an exception when too much input is received instead of resulting
	 * in out-of-memory errors which could affect system stability.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_maxInput}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#maxInput()}
	 * 			<li>{@link RestMethod#maxInput()}
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#maxInput(String)}
	 * 		</ul>
	 * 	<li>String value that gets resolved to a <jk>long</jk>.
	 * 	<li>Can be suffixed with any of the following representing kilobytes, megabytes, and gigabytes:  
	 * 		<js>'K'</js>, <js>'M'</js>, <js>'G'</js>.
	 * 	<li>A value of <js>"-1"</js> can be used to represent no limit.
	 *	</ul>
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder maxInput(String value) {
		return set(REST_maxInput, value);
	}

	/**
	 * <b>Configuration property:</b>  Java method parameter resolvers.
	 *
	 * <p>
	 * By default, the Juneau framework will automatically Java method parameters of various types (e.g.
	 * <code>RestRequest</code>, <code>Accept</code>, <code>Reader</code>).
	 * This annotation allows you to provide your own resolvers for your own class types that you want resolved.
	 *
	 * <p>
	 * For example, if you want to pass in instances of <code>MySpecialObject</code> to your Java method, define
	 * the following resolver:
	 * <p class='bcode'>
	 * 	<jk>public class</jk> MyRestParam <jk>extends</jk> RestParam {
	 *
	 * 		<jc>// Must have no-arg constructor!</jc>
	 * 		<jk>public</jk> MyRestParam() {
	 * 			<jc>// First two parameters help with Swagger doc generation.</jc>
	 * 			<jk>super</jk>(<jsf>QUERY</jsf>, <js>"myparam"</js>, MySpecialObject.<jk>class</jk>);
	 * 		}
	 *
	 * 		<jc>// The method that creates our object.
	 * 		// In this case, we're taking in a query parameter and converting it to our object.</jc>
	 * 		<jk>public</jk> Object resolve(RestRequest req, RestResponse res) <jk>throws</jk> Exception {
	 * 			<jk>return new</jk> MySpecialObject(req.getQuery().get(<js>"myparam"</js>));
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_paramResolvers}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#paramResolvers()}
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#paramResolvers(Class...)}
	 * 		</ul>
	 * 	<li>{@link RestParam} classes must have either a no-arg or {@link PropertyStore} argument constructors.
	 *	</ul>
	 *
	 * @param paramResolvers The parameter resolvers to add to this config.
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings("unchecked")
	public RestContextBuilder paramResolvers(Class<? extends RestParam>...paramResolvers) {
		return addTo(REST_paramResolvers, paramResolvers);
	}

	/**
	 * <b>Configuration property:</b>  Java method parameter resolvers.
	 *
	 * <p>
	 * By default, the Juneau framework will automatically Java method parameters of various types (e.g.
	 * <code>RestRequest</code>, <code>Accept</code>, <code>Reader</code>).
	 * This annotation allows you to provide your own resolvers for your own class types that you want resolved.
	 *
	 * <p>
	 * For example, if you want to pass in instances of <code>MySpecialObject</code> to your Java method, define
	 * the following resolver:
	 * <p class='bcode'>
	 * 	<jk>public class</jk> MyRestParam <jk>extends</jk> RestParam {
	 *
	 * 		<jc>// Must have no-arg constructor!</jc>
	 * 		<jk>public</jk> MyRestParam() {
	 * 			<jc>// First two parameters help with Swagger doc generation.</jc>
	 * 			<jk>super</jk>(<jsf>QUERY</jsf>, <js>"myparam"</js>, MySpecialObject.<jk>class</jk>);
	 * 		}
	 *
	 * 		<jc>// The method that creates our object.
	 * 		// In this case, we're taking in a query parameter and converting it to our object.</jc>
	 * 		<jk>public</jk> Object resolve(RestRequest req, RestResponse res) <jk>throws</jk> Exception {
	 * 			<jk>return new</jk> MySpecialObject(req.getQuery().get(<js>"myparam"</js>));
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_paramResolvers}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#paramResolvers()}
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#paramResolvers(Class...)}
	 * 		</ul>
	 * 	<li>{@link RestParam} classes must have either a no-arg or {@link PropertyStore} argument constructors.
	 *	</ul>
	 *
	 * @param paramResolvers The parameter resolvers to add to this config.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder paramResolvers(RestParam...paramResolvers) {
		return addTo(REST_paramResolvers, paramResolvers);
	}

	/**
	 * <b>Configuration property:</b>  Response converters.
	 *
	 * <p>
	 * Associates one or more {@link RestConverter converters} with a resource class.
	 * These converters get called immediately after execution of the REST method in the same order specified in the
	 * annotation.
	 *
	 * <p>
	 * Can be used for performing post-processing on the response object before serialization.
	 *
	 * <p>
	 * Default converter implementations are provided in the <a class='doclink'
	 * href='../converters/package-summary.html#TOC'>org.apache.juneau.rest.converters</a> package.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_converters}
	 * 	<li>Annotation:
	 * 		<ul>
	 * 			<li>{@link RestResource#converters()}
	 * 			<li>{@link RestMethod#converters()}
	 * 		</ul>
	 * 	<li>Method:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#converters(Class...)}
	 * 			<li>{@link RestContextBuilder#converters(RestConverter...)}
	 * 		</ul>
	 * 	<li>{@link RestConverter} classes must have either a no-arg or {@link PropertyStore} argument constructors.
	 *	</ul>
	 *
	 * @param converters The converter classes to add to this config.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder converters(Class<?>...converters) {
		return addTo(REST_converters, converters);
	}

	/**
	 * <b>Configuration property:</b>  Response converters.
	 *
	 * <p>
	 * Associates one or more {@link RestConverter converters} with a resource class.
	 * These converters get called immediately after execution of the REST method in the same order specified in the
	 * annotation.
	 *
	 * <p>
	 * Can be used for performing post-processing on the response object before serialization.
	 *
	 * <p>
	 * Default converter implementations are provided in the <a class='doclink'
	 * href='../converters/package-summary.html#TOC'>org.apache.juneau.rest.converters</a> package.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_converters}
	 * 	<li>Annotation:
	 * 		<ul>
	 * 			<li>{@link RestResource#converters()}
	 * 			<li>{@link RestMethod#converters()}
	 * 		</ul>
	 * 	<li>Method:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#converters(Class...)}
	 * 			<li>{@link RestContextBuilder#converters(RestConverter...)}
	 * 		</ul>
	 * 	<li>{@link RestConverter} classes must have either a no-arg or {@link PropertyStore} argument constructors.
	 *	</ul>
	 *
	 * @param converters The converter classes to add to this config.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder converters(RestConverter...converters) {
		return addTo(REST_converters, converters);
	}

	/**
	 * <b>Configuration property:</b>  Class-level guards.
	 *
	 * <p>
	 * Associates one or more {@link RestGuard RestGuards} with all REST methods defined in this class.
	 * These guards get called immediately before execution of any REST method in this class.
	 *
	 * <p>
	 * Typically, guards will be used for permissions checking on the user making the request, but it can also be used
	 * for other purposes like pre-call validation of a request.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_guards}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#guards()}
	 * 			<li>{@link RestMethod#guards()}
	 * 		</ul>
	 * 	<li>Method:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#guards(Class...)}
	 * 			<li>{@link RestContextBuilder#guards(RestGuard...)}
	 * 		</ul>
	 * 	<li>{@link RestGuard} classes must have either a no-arg or {@link PropertyStore} argument constructors.
	 * 	<li>Values are added AFTER those found in the annotation and therefore take precedence over those defined via the
	 * 		annotation.
	 *	</ul>
	 *
	 * @param guards The guard classes to add to this config.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder guards(Class<?>...guards) {
		return addTo(REST_guards, guards);
	}

	/**
	 * <b>Configuration property:</b>  Class-level guards.
	 *
	 * <p>
	 * Associates one or more {@link RestGuard RestGuards} with all REST methods defined in this class.
	 * These guards get called immediately before execution of any REST method in this class.
	 *
	 * <p>
	 * Typically, guards will be used for permissions checking on the user making the request, but it can also be used
	 * for other purposes like pre-call validation of a request.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_guards}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#guards()}
	 * 			<li>{@link RestMethod#guards()}
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#guards(Class...)}
	 * 			<li>{@link RestContextBuilder#guards(RestGuard...)}
	 * 		</ul>
	 * 	<li>{@link RestGuard} classes must have either a no-arg or {@link PropertyStore} argument constructors.
	 * 	<li>Values are added AFTER those found in the annotation and therefore take precedence over those defined via the
	 * 		annotation.
	 *	</ul>
	 *
	 * @param guards The guard classes to add to this config.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder guards(RestGuard...guards) {
		return addTo(REST_guards, guards);
	}

	/**
	 * <b>Configuration property:</b>  Response handlers.
	 *
	 * <p>
	 * Specifies a list of {@link ResponseHandler} classes that know how to convert POJOs returned by REST methods or
	 * set via {@link RestResponse#setOutput(Object)} into appropriate HTTP responses.
	 *
	 * <p>
	 * By default, the following response handlers are provided out-of-the-box:
	 * <ul>
	 * 	<li>{@link StreamableHandler}
	 * 	<li>{@link WritableHandler}
	 * 	<li>{@link ReaderHandler}
	 * 	<li>{@link InputStreamHandler}
	 * 	<li>{@link RedirectHandler}
	 * 	<li>{@link DefaultHandler}
	 * </ul>

	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_responseHandlers}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#responseHandlers()} 
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#responseHandlers(Class...)}
	 * 			<li>{@link RestContextBuilder#responseHandlers(ResponseHandler...)}
	 * 		</ul>
	 * 	<li>{@link ResponseHandler} classes must have either a no-arg or {@link PropertyStore} argument constructors.
	 *	</ul>
	 *
	 * @param responseHandlers The response handlers to add to this config.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder responseHandlers(Class<?>...responseHandlers) {
		return addTo(REST_responseHandlers, responseHandlers);
	}

	/**
	 * <b>Configuration property:</b>  Response handlers.
	 *
	 * <p>
	 * Specifies a list of {@link ResponseHandler} classes that know how to convert POJOs returned by REST methods or
	 * set via {@link RestResponse#setOutput(Object)} into appropriate HTTP responses.
	 *
	 * <p>
	 * By default, the following response handlers are provided out-of-the-box:
	 * <ul>
	 * 	<li>{@link StreamableHandler}
	 * 	<li>{@link WritableHandler}
	 * 	<li>{@link ReaderHandler}
	 * 	<li>{@link InputStreamHandler}
	 * 	<li>{@link RedirectHandler}
	 * 	<li>{@link DefaultHandler}
	 * </ul>

	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_responseHandlers}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#responseHandlers()} 
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#responseHandlers(Class...)}
	 * 			<li>{@link RestContextBuilder#responseHandlers(ResponseHandler...)}
	 * 		</ul>
	 * 	<li>{@link ResponseHandler} classes must have either a no-arg or {@link PropertyStore} argument constructors.
	 *	</ul>
	 *
	 * @param responseHandlers The response handlers to add to this config.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder responseHandlers(ResponseHandler...responseHandlers) {
		return addTo(REST_responseHandlers, responseHandlers);
	}

	/**
	 * <b>Configuration property:</b>  Default request headers.
	 *
	 * <p>
	 * Adds class-level default HTTP request headers to this resource.
	 *
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_defaultRequestHeaders}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#defaultRequestHeaders()}
	 * 			<li>{@link RestMethod#defaultRequestHeaders()} 
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#defaultRequestHeader(String,Object)}
	 * 			<li>{@link RestContextBuilder#defaultRequestHeaders(String...)}
	 * 		</ul>
	 * 	<li>Affects values returned by {@link RestRequest#getHeader(String)} when the header is not present on the request.
	 * 	<li>The most useful reason for this annotation is to provide a default <code>Accept</code> header when one is not
	 * 		specified so that a particular default {@link Serializer} is picked.
	 *	</ul>
	 *
	 * @param name The HTTP header name.
	 * @param value The HTTP header value.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder defaultRequestHeader(String name, Object value) {
		return addTo(REST_defaultRequestHeaders, name, value);
	}

	/**
	 * <b>Configuration property:</b>  Default request headers.
	 *
	 * <p>
	 * Adds class-level default HTTP request headers to this resource.
	 *
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_defaultRequestHeaders}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#defaultRequestHeaders()}
	 * 			<li>{@link RestMethod#defaultRequestHeaders()} 
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#defaultRequestHeader(String,Object)}
	 * 			<li>{@link RestContextBuilder#defaultRequestHeaders(String...)}
	 * 		</ul>
	 * 	<li>Strings are of the format <js>"Header-Name: header-value"</js>.
	 * 	<li>You can use either <js>':'</js> or <js>'='</js> as the key/value delimiter.
	 * 	<li>Key and value is trimmed of whitespace.
	 * 	<li>Only one header value can be specified per entry (i.e. it's not a delimited list of header entries).
	 * 	<li>Affects values returned by {@link RestRequest#getHeader(String)} when the header is not present on the request.
	 * 	<li>The most useful reason for this annotation is to provide a default <code>Accept</code> header when one is not
	 * 		specified so that a particular default {@link Serializer} is picked.
	 *	</ul>
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
	 * <b>Configuration property:</b>  Default response headers.
	 *
	 * <p>
	 * Specifies default values for response headers.
	 *
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_defaultResponseHeaders}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#defaultResponseHeaders()} 
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#defaultResponseHeader(String,Object)}
	 * 			<li>{@link RestContextBuilder#defaultResponseHeaders(String...)}
	 * 		</ul>
	 * 	<li>This is equivalent to calling {@link RestResponse#setHeader(String, String)} programmatically in each of 
	 * 		the Java methods.
	 * 	<li>The header value will not be set if the header value has already been specified (hence the 'default' in the name).
	 * 	<li>Values are added AFTER those found in the annotation and therefore take precedence over those defined via the
	 * 		annotation.
	 *	</ul>
	 *
	 * @param name The HTTP header name.
	 * @param value The HTTP header value.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder defaultResponseHeader(String name, Object value) {
		return addTo(REST_defaultResponseHeaders, name, value);
	}

	/**
	 * <b>Configuration property:</b>  Default response headers.
	 *
	 * <p>
	 * Specifies default values for response headers.
	 *
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_defaultResponseHeaders}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#defaultResponseHeaders()} 
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#defaultResponseHeader(String,Object)}
	 * 			<li>{@link RestContextBuilder#defaultResponseHeaders(String...)}
	 * 		</ul>
	 * 	<li>Strings are of the format <js>"Header-Name: header-value"</js>.
	 * 	<li>You can use either <js>':'</js> or <js>'='</js> as the key/value delimiter.
	 * 	<li>Key and value is trimmed of whitespace.
	 * 	<li>Only one header value can be specified per entry (i.e. it's not a delimited list of header entries).
	 * 	<li>This is equivalent to calling {@link RestResponse#setHeader(String, String)} programmatically in each of 
	 * 		the Java methods.
	 * 	<li>The header value will not be set if the header value has already been specified (hence the 'default' in the name).
	 * 	<li>Values are added AFTER those found in the annotation and therefore take precedence over those defined via the
	 * 		annotation.
	 *	</ul>
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
	 * <b>Configuration property:</b>  Supported accept media types.
	 *
	 * <p>
	 * Overrides the media types inferred from the serializers that identify what media types can be produced by the resource.
	 * 
	 * <p>
	 * This affects the values returned by {@link RestRequest#getSupportedAcceptTypes()} and the supported accept
	 * types shown in {@link RestInfoProvider#getSwagger(RestRequest)}.
	 *
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_supportedAcceptTypes}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#supportedAcceptTypes()}
	 * 			<li>{@link RestMethod#supportedAcceptTypes()}
	 * 		</ul> 
	 * 	<li>Methods:  
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#supportedAcceptTypes(boolean,String...)}
	 * 			<li>{@link RestContextBuilder#supportedAcceptTypes(boolean,MediaType...)}
	 * 		</ul>
	 *	</ul>
	 *
	 * @param append
	 * 	If <jk>true</jk>, append to the existing list, otherwise overwrite the previous value. 
	 * @param mediaTypes The new list of media types supported by this resource.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder supportedAcceptTypes(boolean append, String...mediaTypes) {
		return set(append, REST_supportedAcceptTypes, mediaTypes);
	}

	/**
	 * <b>Configuration property:</b>  Supported accept media types.
	 *
	 * <p>
	 * Overrides the media types inferred from the serializers that identify what media types can be produced by the resource.
	 * 
	 * <p>
	 * This affects the values returned by {@link RestRequest#getSupportedAcceptTypes()} and the supported accept
	 * types shown in {@link RestInfoProvider#getSwagger(RestRequest)}.
	 *
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_supportedAcceptTypes}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#supportedAcceptTypes()}
	 * 			<li>{@link RestMethod#supportedAcceptTypes()}
	 * 		</ul> 
	 * 	<li>Methods:  
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#supportedAcceptTypes(boolean,String...)}
	 * 			<li>{@link RestContextBuilder#supportedAcceptTypes(boolean,MediaType...)}
	 * 		</ul>
	 *	</ul>
	 *
	 * @param append
	 * 	If <jk>true</jk>, append to the existing list, otherwise overwrite the previous value. 
	 * @param mediaTypes The new list of media types supported by this resource.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder supportedAcceptTypes(boolean append, MediaType...mediaTypes) {
		return set(append, REST_supportedAcceptTypes, mediaTypes);
	}

	/**
	 * <b>Configuration property:</b>  Supported content media types.
	 *
	 * <p>
	 * Overrides the media types inferred from the parsers that identify what media types can be consumed by the resource.
	 *
	 * <p>
	 * This affects the values returned by {@link RestRequest#getSupportedContentTypes()} and the supported content
	 * types shown in {@link RestInfoProvider#getSwagger(RestRequest)}.
	 * 
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_supportedContentTypes}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#supportedContentTypes()}
	 * 			<li>{@link RestMethod#supportedContentTypes()}
	 * 		</ul> 
	 * 	<li>Methods:  
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#supportedContentTypes(boolean,String...)}
	 * 			<li>{@link RestContextBuilder#supportedContentTypes(boolean,MediaType...)}
	 * 		</ul>
	 *	</ul>
	 *
	 * @param append
	 * 	If <jk>true</jk>, append to the existing list, otherwise overwrite the previous value. 
	 * @param mediaTypes The new list of media types supported by this resource.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder supportedContentTypes(boolean append, String...mediaTypes) {
		return set(append, REST_supportedContentTypes, mediaTypes);
	}

	/**
	 * <b>Configuration property:</b>  Supported content media types.
	 *
	 * <p>
	 * Overrides the media types inferred from the parsers that identify what media types can be consumed by the resource.
	 *
	 * <p>
	 * This affects the values returned by {@link RestRequest#getSupportedContentTypes()} and the supported content
	 * types shown in {@link RestInfoProvider#getSwagger(RestRequest)}.
	 * 
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_supportedContentTypes}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#supportedContentTypes()}
	 * 			<li>{@link RestMethod#supportedContentTypes()}
	 * 		</ul> 
	 * 	<li>Methods:  
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#supportedContentTypes(boolean,String...)}
	 * 			<li>{@link RestContextBuilder#supportedContentTypes(boolean,MediaType...)}
	 * 		</ul>
	 *	</ul>
	 *
	 * @param append
	 * 	If <jk>true</jk>, append to the existing list, otherwise overwrite the previous value. 
	 * @param mediaTypes The new list of media types supported by this resource.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder supportedContentTypes(boolean append, MediaType...mediaTypes) {
		return set(append, REST_supportedContentTypes, mediaTypes);
	}

	/**
	 * <b>Configuration property:</b>  Client version header.
	 *
	 * <p>
	 * Specifies the name of the header used to denote the client version on HTTP requests.
	 *
	 * <p>
	 * The client version is used to support backwards compatibility for breaking REST interface changes.
	 * <br>Used in conjunction with {@link RestMethod#clientVersion()} annotation.
	 * 
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_clientVersionHeader}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#clientVersionHeader()} 
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#clientVersionHeader(String)}
	 * 		</ul>
	 *	</ul>
	 *
	 * @param clientVersionHeader The name of the HTTP header that denotes the client version.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder clientVersionHeader(String clientVersionHeader) {
		return set(REST_clientVersionHeader, clientVersionHeader);
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
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_resourceResolver}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#resourceResolver()} 
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#resourceResolver(Class)}
	 * 			<li>{@link RestContextBuilder#resourceResolver(RestResourceResolver)}
	 * 		</ul>
	 * 	<li>Unless overridden, resource resolvers are inherited from parent resources.
	 *	</ul>
	 *
	 * @param resourceResolver The new resource resolver.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder resourceResolver(Class<? extends RestResourceResolver> resourceResolver) {
		return set(REST_resourceResolver, resourceResolver);
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
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_resourceResolver}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#resourceResolver()} 
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#resourceResolver(Class)}
					<li>{@link RestContextBuilder#resourceResolver(RestResourceResolver)}
				</ul>
	 * 	<li>Unless overridden, resource resolvers are inherited from parent resources.
	 *	</ul>
	 *
	 * @param resourceResolver The new resource resolver.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder resourceResolver(RestResourceResolver resourceResolver) {
		return set(REST_resourceResolver, resourceResolver);
	}

	/**
	 * <b>Configuration property:</b>  REST logger.
	 * 
	 * <p>
	 * Specifies the logger to use for logging.
	 *
	 * <p>
	 * The default logger performs basic error logging to the Java logger.
	 * <br>Subclasses can be used to customize logging behavior on the resource.
	 * 
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_logger}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#logger()} 
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#logger(Class)}
	 * 			<li>{@link RestContextBuilder#logger(RestLogger)} 
	 * 		</ul>
	 *	</ul>
	 *
	 * @param logger The new logger for this resource.  Can be <jk>null</jk> to disable logging.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder logger(Class<? extends RestLogger> logger) {
		return set(REST_logger, logger);
	}

	/**
	 * <b>Configuration property:</b>  REST logger.
	 * 
	 * <p>
	 * Specifies the logger to use for logging.
	 *
	 * <p>
	 * The default logger performs basic error logging to the Java logger.
	 * <br>Subclasses can be used to customize logging behavior on the resource.
	 *
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_logger}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#logger()} 
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#logger(Class)}
	 * 			<li>{@link RestContextBuilder#logger(RestLogger)} 
	 * 		</ul>
	 *	</ul>
	 *
	 * @param logger The new logger for this resource.  Can be <jk>null</jk> to disable logging.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder logger(RestLogger logger) {
		return set(REST_logger, logger);
	}

	/**
	 * <b>Configuration property:</b>  REST call handler.
	 *
	 * <p>
	 * This class handles the basic lifecycle of an HTTP REST call.
	 * <br>Subclasses can be used to customize how these HTTP calls are handled.

	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_callHandler}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#callHandler()} 
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#callHandler(Class)}
	 * 			<li>{@link RestContextBuilder#callHandler(RestCallHandler)} 
	 * 		</ul>
	 *	</ul>
	 *
	 * @param restHandler The new call handler for this resource.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder callHandler(Class<? extends RestCallHandler> restHandler) {
		return set(REST_callHandler, restHandler);
	}

	/**
	 * <b>Configuration property:</b>  REST call handler.
	 *
	 * <p>
	 * This class handles the basic lifecycle of an HTTP REST call.
	 * <br>Subclasses can be used to customize how these HTTP calls are handled.
	 * 
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_callHandler}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#callHandler()} 
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#callHandler(Class)}
	 * 			<li>{@link RestContextBuilder#callHandler(RestCallHandler)} 
	 * 		</ul>
	 *	</ul>
	 *
	 * @param restHandler The new call handler for this resource.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder callHandler(RestCallHandler restHandler) {
		return set(REST_callHandler, restHandler);
	}

	/**
	 * <b>Configuration property:</b>  REST info provider. 
	 *
	 * <p>
	 * Class used to retrieve title/description/swagger information about a resource.
	 *
	 * <p>
	 * Subclasses can be used to customize the documentation on a resource.
	 *
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_infoProvider}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#infoProvider()} 
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#infoProvider(Class)}
	 * 			<li>{@link RestContextBuilder#infoProvider(RestInfoProvider)} 
	 * 		</ul>
	 *	</ul>
	 *
	 * @param infoProvider The new info provider for this resource.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder infoProvider(Class<? extends RestInfoProvider> infoProvider) {
		return set(REST_infoProvider, infoProvider);
	}

	/**
	 * <b>Configuration property:</b>  REST info provider. 
	 *
	 * <p>
	 * Class used to retrieve title/description/swagger information about a resource.
	 *
	 * <p>
	 * Subclasses can be used to customize the documentation on a resource.
	 *
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_infoProvider}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#infoProvider()} 
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#infoProvider(Class)}
	 * 			<li>{@link RestContextBuilder#infoProvider(RestInfoProvider)} 
	 * 		</ul>
	 *	</ul>
	 *
	 * @param infoProvider The new info provider for this resource.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder infoProvider(RestInfoProvider infoProvider) {
		return set(REST_infoProvider, infoProvider);
	}

	/**
	 * <b>Configuration property:</b>  Static file mappings. 
	 *
	 * <p>
	 * Used to define paths and locations of statically-served files such as images or HTML documents.
	 * 
	 * <p>
	 * Static files are found by calling {@link RestContext#getClasspathResource(String,Locale)} which uses the registered 
	 * {@link ClasspathResourceFinder} for locating files on the classpath (or other location).
	 * 
	 * <p>
	 * An example where this class is used is in the {@link RestResource#staticFiles} annotation:
	 * <p class='bcode'>
	 * 	<jk>package</jk> com.foo.mypackage;
	 * 
	 * 	<ja>@RestResource</ja>(
	 * 		path=<js>"/myresource"</js>,
	 * 		staticFiles=<js>"htdocs:docs"</js>
	 * 	)
	 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestServletDefault {...}
	 * </p>
	 * 
	 * <p>
	 * In the example above, given a GET request to <l>/myresource/htdocs/foobar.html</l>, the servlet will attempt to find 
	 * the <l>foobar.html</l> file in the following ordered locations:
	 * <ol>
	 * 	<li><l>com.foo.mypackage.docs</l> package.
	 * 	<li><l>org.apache.juneau.rest.docs</l> package (since <l>RestServletDefault</l> is in <l>org.apache.juneau.rest</l>).
	 * 	<li><l>[working-dir]/docs</l> directory.
	 * </ol>
	 * 
	 * <h6 class='topic'>Notes:</h6>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_staticFiles}
	 * 	<li>Annotations: 
	 * 		<ul>
	 * 			<li>{@link RestResource#staticFiles()} 
	 * 		</ul>
	 * 	<li>Methods: 
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#staticFiles(String)},
	 * 			<li>{@link RestContextBuilder#staticFiles(Class,String)}
	 * 			<li>{@link RestContextBuilder#staticFiles(String,String)}
	 * 			<li>{@link RestContextBuilder#staticFiles(Class,String,String)} 
	 * 			<li>{@link RestContextBuilder#staticFiles(StaticFileMapping...)} 
	 * 		</ul>
	 * 	<li>Mappings are cumulative from parent to child.  
	 * 	<li>Child resources can override mappings made on parent resources.
	 * 	<li>The media type on the response is determined by the {@link RestContext#getMediaTypeForName(String)} method.
	 * 	<li>The resource finder is configured via the {@link RestContext#REST_classpathResourceFinder} setting, and can be
	 * 		overridden to provide customized handling of resource retrieval.
	 * 	<li>The {@link RestContext#REST_useClasspathResourceCaching} setting can be used to cache static files in memory
	 * 		to improve performance.
	 * </ul>
	 *
	 * @param sfm The static file mappings to add to this resource.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder staticFiles(StaticFileMapping...sfm) {
		return addTo(REST_staticFiles, sfm);
	}

	/**
	 * Same as {@link #staticFiles(StaticFileMapping...)}, except input is in the form of a mapping string.
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
	 * Same as {@link #staticFiles(String)}, except overrides the base class for retrieving the resource.
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
	 * Same as {@link #staticFiles(String)}, except path and location are already split values.
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
	 * Same as {@link #staticFiles(String,String)},  except overrides the base class for retrieving the resource.
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
	 * <b>Configuration property:</b>  Static file response headers. 
	 *
	 * <p>
	 * Used to customize the headers on responses returned for statically-served files.
	 * 
	 * <h6 class='topic'>Notes:</h6>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_staticFileResponseHeaders}
	 * 	<li>Annotations: 
	 * 		<ul>
	 * 			<li>{@link RestResource#staticFileResponseHeaders()} 
	 * 		</ul>
	 * 	<li>Methods: 
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#staticFileResponseHeaders(boolean,Map)}
	 * 			<li>{@link RestContextBuilder#staticFileResponseHeaders(String...)}
	 * 			<li>{@link RestContextBuilder#staticFileResponseHeader(String,String)}
	 * 		</ul>
	 * </ul>
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
	 * <b>Configuration property:</b>  Static file response headers. 
	 *
	 * <p>
	 * Used to customize the headers on responses returned for statically-served files.
	 * 
	 * <h6 class='topic'>Notes:</h6>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_staticFileResponseHeaders}
	 * 	<li>Annotations: 
	 * 		<ul>
	 * 			<li>{@link RestResource#staticFileResponseHeaders()} 
	 * 		</ul>
	 * 	<li>Methods: 
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#staticFileResponseHeaders(boolean,Map)}
	 * 			<li>{@link RestContextBuilder#staticFileResponseHeaders(String...)}
	 * 			<li>{@link RestContextBuilder#staticFileResponseHeader(String,String)}
	 * 		</ul>
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
	 * <b>Configuration property:</b>  Static file response headers. 
	 *
	 * <p>
	 * Used to customize the headers on responses returned for statically-served files.
	 * 
	 * <h6 class='topic'>Notes:</h6>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_staticFileResponseHeaders}
	 * 	<li>Annotations: 
	 * 		<ul>
	 * 			<li>{@link RestResource#staticFileResponseHeaders()} 
	 * 		</ul>
	 * 	<li>Methods: 
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#staticFileResponseHeaders(boolean,Map)}
	 * 			<li>{@link RestContextBuilder#staticFileResponseHeaders(String...)}
	 * 			<li>{@link RestContextBuilder#staticFileResponseHeader(String,String)}
	 * 		</ul>
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
	 * <b>Configuration property:</b>  Classpath resource finder. 
	 * 
	 * <p>
	 * Used to retrieve localized files from the classpath.
	 * 
	 * <h6 class='topic'>Notes:</h6>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_classpathResourceFinder}
	 * 	<li>Annotations: 
	 * 		<ul>
	 * 			<li>{@link RestResource#classpathResourceFinder()} 
	 * 		</ul>
	 * 	<li>Methods: 
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#classpathResourceFinder(Class)}
	 * 			<li>{@link RestContextBuilder#classpathResourceFinder(ClasspathResourceFinder)}
	 * 		</ul>
	 * 	<li>
	 * 		The default value is {@link ClasspathResourceFinderBasic} which provides basic support for finding localized
	 * 		resources on the classpath and JVM working directory.
	 * 		<br>The {@link ClasspathResourceFinderRecursive} is another option that also recursively searches for resources
	 * 		up the parent class hierarchy.
	 * 		<br>Each of these classes can be extended to provide customized handling of resource retrieval.
	 * </ul>
	 * 
	 * @param classpathResourceFinder The resource finder class.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder classpathResourceFinder(Class<? extends ClasspathResourceFinder> classpathResourceFinder) {
		return set(REST_classpathResourceFinder, classpathResourceFinder);
	}
	
	/**
	 * <b>Configuration property:</b>  Classpath resource finder. 
	 * 
	 * <p>
	 * Used to retrieve localized files from the classpath.
	 * 
	 * <h6 class='topic'>Notes:</h6>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_classpathResourceFinder}
	 * 	<li>Annotations: 
	 * 		<ul>
	 * 			<li>{@link RestResource#classpathResourceFinder()} 
	 * 		</ul>
	 * 	<li>Methods: 
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#classpathResourceFinder(Class)}
	 * 			<li>{@link RestContextBuilder#classpathResourceFinder(ClasspathResourceFinder)}
	 * 		</ul>
	 * 	<li>
	 * 		The default value is {@link ClasspathResourceFinderBasic} which provides basic support for finding localized
	 * 		resources on the classpath and JVM working directory.
	 * 		<br>The {@link ClasspathResourceFinderRecursive} is another option that also recursively searches for resources
	 * 		up the parent class hierarchy.
	 * 		<br>Each of these classes can be extended to provide customized handling of resource retrieval.
	 * </ul>
	 * 
	 * @param classpathResourceFinder The resource finder instance.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder classpathResourceFinder(ClasspathResourceFinder classpathResourceFinder) {
		return set(REST_classpathResourceFinder, classpathResourceFinder);
	}

	/**
	 * <b>Configuration property:</b>  Use classpath resource caching. 
	 *
	 * <p>
	 * When enabled, resources retrieved via {@link RestContext#getClasspathResource(String, Locale)} (and related 
	 * methods) will be cached in memory to speed subsequent lookups.
	 * 
	 * <h6 class='topic'>Notes:</h6>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_useClasspathResourceCaching}
	 * 	<li>Annotations: 
	 * 		<ul>
	 * 			<li>{@link RestResource#useClasspathResourceCaching()} 
	 * 		</ul>
	 * 	<li>Methods: 
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#useClasspathResourceCaching(boolean)}
	 * 		</ul>
	 * </ul>
	 * 
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder useClasspathResourceCaching(boolean value) {
		return set(REST_useClasspathResourceCaching, value);
	}

	/**
	 * <b>Configuration property:</b>  HTML Widgets. 
	 *
	 * <p>
	 * Defines widgets that can be used in conjunction with string variables of the form <js>"$W{name}"</js>to quickly
	 * generate arbitrary replacement text.
	 * 
	 * Widgets resolve the following variables:
	 * <ul>
	 * 	<li><js>"$W{name}"</js> - Contents returned by {@link Widget#getHtml(RestRequest)}.
	 * 	<li><js>"$W{name.script}"</js> - Contents returned by {@link Widget#getScript(RestRequest)}.
	 * 		<br>The script contents are automatically inserted into the <xt>&lt;head/script&gt;</xt> section
	 * 			 in the HTML page.
	 * 	<li><js>"$W{name.style}"</js> - Contents returned by {@link Widget#getStyle(RestRequest)}.
	 * 		<br>The styles contents are automatically inserted into the <xt>&lt;head/style&gt;</xt> section
	 * 			 in the HTML page.
	 * </ul>
	 *
	 * <p>
	 * The following examples shows how to associate a widget with a REST method and then have it rendered in the links
	 * and aside section of the page:
	 *
	 * <p class='bcode'>
	 * 	<ja>@RestMethod</ja>(
	 * 		widgets={
	 * 			MyWidget.<jk>class</jk>
	 * 		}
	 * 		htmldoc=<ja>@HtmlDoc</ja>(
	 * 			navlinks={
	 * 				<js>"$W{MyWidget}"</js>
	 * 			},
	 * 			aside={
	 * 				<js>"Check out this widget:  $W{MyWidget}"</js>
	 * 			}
	 * 		)
	 * 	)
	 * </p>
	 * 
	 * <h6 class='topic'>Notes:</h6>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_widgets}
	 * 	<li>Annotations: 
	 * 		<ul>
	 * 			<li>{@link HtmlDoc#widgets()} 
	 * 		</ul>
	 * 	<li>Methods: 
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#widgets(Class...)}
	 * 			<li>{@link RestContextBuilder#widgets(Widget...)}
	 * 			<li>{@link RestContextBuilder#widgets(boolean,Widget...)}
	 * 		</ul>
	 * 	<li>Widgets are inherited from parent to child, but can be overridden by reusing the widget name.
	 * 	<li>Values are appended to the existing list.
	 * </ul>
	 *
	 * @param values The widget classes to add.
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings("unchecked")
	public RestContextBuilder widgets(Class<? extends Widget>...values) {
		return addTo(REST_widgets, values);
	}
	
	/**
	 * <b>Configuration property:</b>  HTML Widgets. 
	 * 
	 * <p>
	 * Same as {@link #widgets(Class...)} but allows you to pass in pre-instantiated Widget objects.
	 * 
	 * <p>
	 * Values are appended to the existing list.
	 *
	 * @param values The widget objects to add.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder widgets(Widget...values) {
		return addTo(REST_widgets, values);
	}

	/**
	 * <b>Configuration property:</b>  HTML Widgets. 
	 * 
	 * <p>
	 * Same as {@link #widgets(Class...)} but allows you to pass in pre-instantiated Widget objects.
	 * 
	 * <p>
	 * Values are appended to the existing list.
	 * 
	 * @param append 
	 * 	If <jk>true</jk>, appends to the existing list of widgets.
	 * 	<br>Otherwise, replaces the previous list.
	 * @param values The widget objects to add.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder widgets(boolean append, Widget...values) {
		return set(append, REST_widgets, values);
	}

	/**
	 * <b>Configuration property:</b>  MIME types. 
	 *
	 * <p>
	 * Defines MIME-type file type mappings.
	 * 
	 * <p>
	 * Used for specifying the content type on file resources retrieved through the following methods:
	 * <ul>
	 * 	<li>{@link RestContext#resolveStaticFile(String)}
	 * 	<li>{@link RestRequest#getClasspathReaderResource(String,boolean,MediaType)}
	 * 	<li>{@link RestRequest#getClasspathReaderResource(String,boolean)}
	 * 	<li>{@link RestRequest#getClasspathReaderResource(String)}
	 * </ul>
	 * 
	 * <p>
	 * This list appends to the existing list provided by {@link ExtendedMimetypesFileTypeMap}.
	 * 
	 * <h6 class='topic'>Notes:</h6>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_mimeTypes}
	 * 	<li>Annotations: 
	 * 		<ul>
	 * 			<li>{@link RestResource#mimeTypes()} 
	 * 		</ul>
	 * 	<li>Methods: 
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#mimeTypes(String...)}
	 * 		</ul>
	 * 	<li>Values are .mime.types formatted entry string.
	 * 		<br>Example: <js>"image/svg+xml svg"</js>
	 * </ul>
	 *
	 * @param mimeTypes The MIME-types to add to this config.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder mimeTypes(String...mimeTypes) {
		return addTo(REST_mimeTypes, mimeTypes);
	}

	/**
	 * <b>Configuration property:</b>  Serializers. 
	 *
	 * <p>
	 * Adds class-level serializers to this resource.
	 * 
	 * <h6 class='topic'>Notes:</h6>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_serializers}
	 * 	<li>Annotations: 
	 * 		<ul>
	 * 			<li>{@link RestResource#serializers()} 
	 * 			<li>{@link RestMethod#serializers()} 
	 * 		</ul>
	 * 	<li>Methods: 
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#serializers(Class...)}
	 * 			<li>{@link RestContextBuilder#serializers(boolean,Class...)}
	 * 			<li>{@link RestContextBuilder#serializers(Serializer...)}
	 * 			<li>{@link RestContextBuilder#serializers(boolean,Serializer...)}
	 * 		</ul>
	 * 	<li>When defined as a class, properties/transforms defined on the resource/method are inherited.
	 * 	<li>When defined as an instance, properties/transforms defined on the resource/method are NOT inherited.
	 * 	<li>Values are added AFTER those found in the annotation and therefore take precedence over those defined via the
	 * 		annotation.
	 * </ul>
	 *
	 * @param serializers The serializer classes to add to this config.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder serializers(Class<?>...serializers) {
		return addTo(REST_serializers, serializers);
	}

	/**
	 * <b>Configuration property:</b>  Serializers. 
	 *
	 * <p>
	 * Same as {@link #serializers(Class...)} except allows you to overwrite the list of existing serializers instead
	 * of appending.
	 * 
	 * @param append
	 * 	If <jk>true</jk>, append to the existing list, otherwise overwrite the previous value. 
	 * @param serializers The serializer classes to add to this config.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder serializers(boolean append, Class<?>...serializers) {
		return set(append, REST_serializers, serializers);
	}

	/**
	 * <b>Configuration property:</b>  Serializers. 
	 *
	 * <p>
	 * Same as {@link #serializers(Class...)} except allows you to pass in serializer instances.
	 * 
	 * <p>
	 * Serializer instances are considered set-in-stone and do NOT inherit properties and transforms defined on the
	 * resource class or method. 
	 * 
	 * @param serializers The serializer to add to this config.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder serializers(Serializer...serializers) {
		return addTo(REST_serializers, serializers);
	}

	/**
	 * <b>Configuration property:</b>  Serializers. 
	 *
	 * <p>
	 * Same as {@link #serializers(Serializer...)} except allows you to overwrite the list of existing serializers instead
	 * of appending.
	 * 
	 * @param append
	 * 	If <jk>true</jk>, append to the existing list, otherwise overwrite the previous value. 
	 * @param serializers The serializer to add to this config.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder serializers(boolean append, Serializer...serializers) {
		return set(append, REST_serializers, serializers);
	}

	/**
	 * <b>Configuration property:</b>  Parsers. 
	 *
	 * <p>
	 * Adds class-level parsers to this resource.
	 * 
	 * <h6 class='topic'>Notes:</h6>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_parsers}
	 * 	<li>Annotations: 
	 * 		<ul>
	 * 			<li>{@link RestResource#parsers()} 
	 * 			<li>{@link RestMethod#parsers()} 
	 * 		</ul>
	 * 	<li>Methods: 
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#parsers(Class...)}
	 * 			<li>{@link RestContextBuilder#parsers(boolean,Class...)}
	 * 			<li>{@link RestContextBuilder#parsers(Parser...)}
	 * 			<li>{@link RestContextBuilder#parsers(boolean,Parser...)}
	 * 		</ul>
	 * 	<li>When defined as a class, properties/transforms defined on the resource/method are inherited.
	 * 	<li>When defined as an instance, properties/transforms defined on the resource/method are NOT inherited.
	 * 	<li>Values are added AFTER those found in the annotation and therefore take precedence over those defined via the
	 * 		annotation.
	 * </ul>
	 *
	 * @param parsers The parser classes to add to this config.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder parsers(Class<?>...parsers) {
		return addTo(REST_parsers, parsers);
	}

	/**
	 * <b>Configuration property:</b>  Parsers. 
	 *
	 * <p>
	 * Same as {@link #parsers(Class...)} except allows you to overwrite the list of existing parsers instead
	 * of appending.
	 * 
	 * @param append
	 * 	If <jk>true</jk>, append to the existing list, otherwise overwrite the previous value. 
	 * @param parsers The parser classes to add to this config.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder parsers(boolean append, Class<?>...parsers) {
		return set(append, REST_parsers, parsers);
	}

	/**
	 * <b>Configuration property:</b>  Parsers. 
	 *
	 * <p>
	 * Same as {@link #parsers(Class...)} except allows you to pass in parser instances.
	 * 
	 * <p>
	 * Parser instances are considered set-in-stone and do NOT inherit properties and transforms defined on the
	 * resource class or method. 
	 * 
	 * @param parsers The parsers to add to this config.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder parsers(Parser...parsers) {
		return addTo(REST_parsers, parsers);
	}

	/**
	 * <b>Configuration property:</b>  Parsers. 
	 *
	 * <p>
	 * Same as {@link #parsers(Parser...)} except allows you to overwrite the list of existing parsers instead
	 * of appending.
	 * 
	 * @param append
	 * 	If <jk>true</jk>, append to the existing list, otherwise overwrite the previous value. 
	 * @param parsers The parsers to add to this config.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder parsers(boolean append, Parser...parsers) {
		return set(append, REST_parsers, parsers);
	}

	/**
	 * <b>Configuration property:</b>  HTTP part serializer. 
	 *
	 * <p>
	 * Specifies the {@link HttpPartSerializer} to use for serializing headers, query/form parameters, and URI parts.
	 *
	 * <h6 class='topic'>Notes:</h6>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_partSerializer}
	 * 	<li>Annotations: 
	 * 		<ul>
	 * 			<li>{@link RestResource#partSerializer()} 
	 * 		</ul>
	 * 	<li>Methods: 
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#partSerializer(Class)}
	 * 			<li>{@link RestContextBuilder#partSerializer(HttpPartSerializer)}
	 * 		</ul>
	 * 	<li>When defined as a class, properties/transforms defined on the resource/method are inherited.
	 * 	<li>When defined as an instance, properties/transforms defined on the resource/method are NOT inherited.
	 * </ul>
	 *
	 * @param partSerializer The serializer class.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder partSerializer(Class<? extends HttpPartSerializer> partSerializer) {
		return set(REST_partSerializer, partSerializer);
	}

	/**
	 * <b>Configuration property:</b>  HTTP part serializer. 
	 *
	 * <p>
	 * Specifies the {@link HttpPartSerializer} to use for serializing headers, query/form parameters, and URI parts.
	 *
	 * <h6 class='topic'>Notes:</h6>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_partSerializer}
	 * 	<li>Annotations: 
	 * 		<ul>
	 * 			<li>{@link RestResource#partSerializer()} 
	 * 		</ul>
	 * 	<li>Methods: 
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#partSerializer(Class)}
	 * 			<li>{@link RestContextBuilder#partSerializer(HttpPartSerializer)}
	 * 		</ul>
	 * 	<li>When defined as a class, properties/transforms defined on the resource/method are inherited.
	 * 	<li>When defined as an instance, properties/transforms defined on the resource/method are NOT inherited.
	 * </ul>
	 *
	 * @param partSerializer The serializer instance.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder partSerializer(HttpPartSerializer partSerializer) {
		return set(REST_partSerializer, partSerializer);
	}

	/**
	 * <b>Configuration property:</b>  HTTP part parser. 
	 *
	 * <p>
	 * Specifies the {@link HttpPartParser} to use for parsing headers, query/form parameters, and URI parts.
	 * 
	 * <h6 class='topic'>Notes:</h6>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_partParser}
	 * 	<li>Annotations: 
	 * 		<ul>
	 * 			<li>{@link RestResource#partParser()} 
	 * 		</ul>
	 * 	<li>Methods: 
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#partParser(Class)}
	 * 			<li>{@link RestContextBuilder#partParser(HttpPartParser)}
	 * 		</ul>
	 * 	<li>When defined as a class, properties/transforms defined on the resource/method are inherited.
	 * 	<li>When defined as an instance, properties/transforms defined on the resource/method are NOT inherited.
	 * </ul>
	 *
	 * @param partParser The parser class.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder partParser(Class<? extends HttpPartParser> partParser) {
		return set(REST_partParser, partParser);
	}

	/**
	 * <b>Configuration property:</b>  HTTP part parser. 
	 *
	 * <p>
	 * Specifies the {@link HttpPartParser} to use for parsing headers, query/form parameters, and URI parts.
	 * 
	 * <h6 class='topic'>Notes:</h6>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_partParser}
	 * 	<li>Annotations: 
	 * 		<ul>
	 * 			<li>{@link RestResource#partParser()} 
	 * 		</ul>
	 * 	<li>Methods: 
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#partParser(Class)}
	 * 			<li>{@link RestContextBuilder#partParser(HttpPartParser)}
	 * 		</ul>
	 * 	<li>When defined as a class, properties/transforms defined on the resource/method are inherited.
	 * 	<li>When defined as an instance, properties/transforms defined on the resource/method are NOT inherited.
	 * </ul>
	 *
	 * @param partParser The parser instance.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder partParser(HttpPartParser partParser) {
		return set(REST_partParser, partParser);
	}

	/**
	 * <b>Configuration property:</b>  Compression encoders. 
	 *
	 * <p>
	 * These can be used to enable various kinds of compression (e.g. <js>"gzip"</js>) on requests and responses.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Servlet with automated support for GZIP compression</jc>
	 * 	<ja>@RestResource</ja>(encoders={GzipEncoder.<jk>class</jk>})
	 * 	<jk>public</jk> MyRestServlet <jk>extends</jk> RestServlet {
	 * 		...
	 * 	}
	 * </p>
	 * 
	 * <h6 class='topic'>Notes:</h6>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_encoders}
	 * 	<li>Annotations: 
	 * 		<ul>
	 * 			<li>{@link RestResource#encoders()} 
	 * 			<li>{@link RestMethod#encoders()} 
	 * 		</ul>
	 * 	<li>Methods: 
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#encoders(Class...)}
	 * 			<li>{@link RestContextBuilder#encoders(Encoder...)}
	 * 		</ul>
	 * 	<li>Instance classes must provide a public no-arg constructor, or a public constructor that takes in a
	 * 		{@link PropertyStore} object.
	 * 	<li>Instance class can be defined as an inner class of the REST resource class.
	 * </ul>
	 *
	 * @param encoders Encoder classes to add to this config.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder encoders(Class<?>...encoders) {
		return addTo(REST_encoders, encoders);
	}

	/**
	 * <b>Configuration property:</b>  Compression encoders. 
	 *
	 * <p>
	 * These can be used to enable various kinds of compression (e.g. <js>"gzip"</js>) on requests and responses.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Servlet with automated support for GZIP compression</jc>
	 * 	<ja>@RestResource</ja>(encoders={GzipEncoder.<jk>class</jk>})
	 * 	<jk>public</jk> MyRestServlet <jk>extends</jk> RestServlet {
	 * 		...
	 * 	}
	 * </p>
	 * 
	 * <h6 class='topic'>Notes:</h6>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_encoders}
	 * 	<li>Annotations: 
	 * 		<ul>
	 * 			<li>{@link RestResource#encoders()} 
	 * 			<li>{@link RestMethod#encoders()} 
	 * 		</ul>
	 * 	<li>Methods: 
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#encoders(Class...)}
	 * 			<li>{@link RestContextBuilder#encoders(Encoder...)}
	 * 		</ul>
	 * 	<li>Instance classes must provide a public no-arg constructor, or a public constructor that takes in a
	 * 		{@link PropertyStore} object.
	 * 	<li>Instance class can be defined as an inner class of the REST resource class.
	 * </ul>
	 *
	 * @param encoders Encoder instances to add to this config.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder encoders(Encoder...encoders) {
		return addTo(REST_encoders, encoders);
	}

	/**
	 * <b>Configuration property:</b>  Serializer listener.
	 * 
	 * <p>
	 * Specifies the serializer listener class to use for listening to non-fatal serialization errors.
	 *
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link Serializer#SERIALIZER_listener}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#serializerListener()} 
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#serializerListener(Class)} 
	 * 		</ul>
	 *	</ul>
	 *
	 * @param listener The listener to add to this config.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder serializerListener(Class<? extends SerializerListener> listener) {
		return set(SERIALIZER_listener, listener);
	}

	/**
	 * <b>Configuration property:</b>  Parser listener.
	 * 
	 * <p>
	 * Specifies the parser listener class to use for listening to non-fatal parsing errors.
	 *
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link Parser#PARSER_listener}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#parserListener()} 
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#parserListener(Class)}
	 * 		</ul> 
	 *	</ul>
	 *
	 * @param listener The listener to add to this config.
	 * @return This object (for method chaining).
	 */
	public RestContextBuilder parserListener(Class<? extends ParserListener> listener) {
		return set(PARSER_listener, listener);
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
	public RestContextBuilder notBeanPackages(String...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder notBeanPackages(Collection<String> values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder notBeanPackages(boolean append, String...values) {
		super.notBeanPackages(append, values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder notBeanPackages(boolean append, Collection<String> values) {
		super.notBeanPackages(append, values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder notBeanPackagesRemove(String...values) {
		super.notBeanPackagesRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder notBeanPackagesRemove(Collection<String> values) {
		super.notBeanPackagesRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder notBeanClasses(Class<?>...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder notBeanClasses(Collection<Class<?>> values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder notBeanClasses(boolean append, Class<?>...values) {
		super.notBeanClasses(append, values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder notBeanClasses(boolean append, Collection<Class<?>> values) {
		super.notBeanClasses(append, values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder notBeanClassesRemove(Class<?>...values) {
		super.notBeanClassesRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder notBeanClassesRemove(Collection<Class<?>> values) {
		super.notBeanClassesRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder beanFilters(Class<?>...values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder beanFilters(Collection<Class<?>> values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder beanFilters(boolean append, Class<?>...values) {
		super.beanFilters(append, values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder beanFilters(boolean append, Collection<Class<?>> values) {
		super.beanFilters(append, values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder beanFiltersRemove(Class<?>...values) {
		super.beanFiltersRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder beanFiltersRemove(Collection<Class<?>> values) {
		super.beanFiltersRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder pojoSwaps(Class<?>...values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder pojoSwaps(Collection<Class<?>> values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder pojoSwaps(boolean append, Class<?>...values) {
		super.pojoSwaps(append, values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder pojoSwaps(boolean append, Collection<Class<?>> values) {
		super.pojoSwaps(append, values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder pojoSwapsRemove(Class<?>...values) {
		super.pojoSwapsRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder pojoSwapsRemove(Collection<Class<?>> values) {
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
	public RestContextBuilder beanDictionary(Class<?>...values) {
		super.beanDictionary(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder beanDictionary(Collection<Class<?>> values) {
		super.beanDictionary(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder beanDictionary(boolean append, Class<?>...values) {
		super.beanDictionary(append, values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder beanDictionary(boolean append, Collection<Class<?>> values) {
		super.beanDictionary(append, values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder beanDictionaryRemove(Class<?>...values) {
		super.beanDictionaryRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder beanDictionaryRemove(Collection<Class<?>> values) {
		super.beanDictionaryRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder beanTypePropertyName(String value) {
		super.beanTypePropertyName(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestContextBuilder defaultParser(Class<?> value) {
		super.defaultParser(value);
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
