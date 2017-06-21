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

import java.io.*;
import java.util.*;

import javax.activation.*;
import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.juneau.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.encoders.Encoder;
import org.apache.juneau.html.*;
import org.apache.juneau.http.*;
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

/**
 * Defines the initial configuration of a <code>RestServlet</code> or <code>@RestResource</code> annotated object.
 * <p>
 * An extension of the {@link ServletConfig} object used during servlet initialization.
 * <p>
 * Provides access to the following initialized resources:
 * <ul>
 * 	<li>{@link #getConfigFile()} - The external configuration file for this resource.
 * 	<li>{@link #getProperties()} - The modifiable configuration properties for this resource.
 * 	<li>{@link #getVarResolverBuilder()} - The variable resolver for this resource.
 * </ul>
 * <p>
 * Methods are provided for overriding or augmenting the information provided by the <ja>@RestResource</ja> annotation.
 * In general, most information provided in the <ja>@RestResource</ja> annotation can be specified programmatically
 * through calls on this object.
 * <p>
 * To interact with this object, simply implement the following init method in your resource class:
 * <p class='bcode'>
 * 	<jk>public synchronized void</jk> init(RestConfig config) <jk>throws</jk> Exception {
 * 		config.addPojoSwaps(CalendarSwap.<jsf>RFC2822DTZ</jsf>.<jk>class</jk>);
 * 		config.setProperty(<jsf>PARSER_debug</jsf>, <jk>true</jk>);
 * 		<jk>super</jk>.init(config); <jc>// Make sure this is the last line! (or just leave it out entirely)</jc>
 * 	}
 * </p>
 * <p>
 * Note that this method is identical to {@link HttpServlet#init(ServletConfig)} except you get access to
 * this object instead.  Also, this method can throw any exception, not just a {@link ServletException}.
 * <p>
 * The parent <code>init(RestServletConfig)</code> method will construct a read-only {@link RestContext} object
 * that contains a snapshot of these settings.  If you call <code><jk>super</jk>.init(RestServletConfig)<code> before
 * you modify this config object, you won't see the changes!
 */
@SuppressWarnings({"hiding"})
public class RestConfig implements ServletConfig {

	final ServletConfig inner;

	//---------------------------------------------------------------------------
	// The following fields are meant to be modifiable.
	// They should not be declared final.
	// Read-only snapshots of these will be made in RestServletContext.
	//---------------------------------------------------------------------------

	ObjectMap properties;
	ConfigFile configFile;
	VarResolverBuilder varResolverBuilder;

	List<Class<?>>
		beanFilters = new ArrayList<Class<?>>(),
		pojoSwaps = new ArrayList<Class<?>>(),
		paramResolvers = new ArrayList<Class<?>>();
	Class<? extends SerializerListener> serializerListener;
	Class<? extends ParserListener> parserListener;
	SerializerGroupBuilder serializers = new SerializerGroupBuilder();
	ParserGroupBuilder parsers = new ParserGroupBuilder();
	EncoderGroupBuilder encoders = new EncoderGroupBuilder().append(IdentityEncoder.INSTANCE);
	List<Object> converters = new ArrayList<Object>();
	List<Object> guards = new ArrayList<Object>();
	MimetypesFileTypeMap mimeTypes = new MimetypesFileTypeMap();
	Map<String,String> defaultRequestHeaders = new TreeMap<String,String>(String.CASE_INSENSITIVE_ORDER);
	Map<String,Object> defaultResponseHeaders = new LinkedHashMap<String,Object>();
	List<Object> responseHandlers = new ArrayList<Object>();
	List<Object> childResources = new ArrayList<Object>();
	List<MediaType> supportedContentTypes, supportedAcceptTypes;
	List<Object> styleSheets;
	Object favIcon;
	List<Object> staticFiles;
	RestContext parentContext;
	String path, htmlTitle, htmlDescription, htmlBranding, htmlLinks, htmlHeader, htmlNav, htmlAside, htmlFooter,
		htmlCss, htmlCssUrl, htmlNoResultsMessage;
	String clientVersionHeader = "X-Client-Version";

	Object resourceResolver = RestResourceResolver.class;
	Object logger = RestLogger.Normal.class;
	Object callHandler = RestCallHandler.class;
	Object infoProvider = RestInfoProvider.class;

	boolean htmlNoWrap;
	Object htmlTemplate = HtmlDocTemplateBasic.class;

	Class<?> resourceClass;
	Map<String,Widget> widgets = new HashMap<String,Widget>();

	/**
	 * Constructor.
	 * @param config The servlet config passed into the servlet by the servlet container.
	 * @param resource The class annotated with <ja>@RestResource</ja>.
	 * @throws ServletException Something bad happened.
	 */
	RestConfig(ServletConfig config, Class<?> resourceClass, RestContext parentContext) throws ServletException {
		this.inner = config;
		this.resourceClass = resourceClass;
		this.parentContext = parentContext;
		try {

			ConfigFileBuilder cfb = new ConfigFileBuilder();

			properties = new ObjectMap();
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
				addSerializers(r.serializers());
				addParsers(r.parsers());
				addEncoders(r.encoders());
				addDefaultRequestHeaders(r.defaultRequestHeaders());
				addDefaultResponseHeaders(r.defaultResponseHeaders());
				addResponseHandlers(r.responseHandlers());
				addConverters(r.converters());
				addGuards(reverse(r.guards()));
				addChildResources(r.children());
				addBeanFilters(r.beanFilters());
				addPojoSwaps(r.pojoSwaps());
				addParamResolvers(r.paramResolvers());
				serializerListener(r.serializerListener());
				parserListener(r.parserListener());
				if (! r.stylesheet().isEmpty())
					setStyleSheet(c, r.stylesheet());
				if (! r.favicon().isEmpty())
					setFavIcon(c, r.favicon());
				if (! r.staticFiles().isEmpty())
					addStaticFiles(c, r.staticFiles());
				if (! r.path().isEmpty())
					setPath(r.path());
				if (! r.clientVersionHeader().isEmpty())
					setClientVersionHeader(r.clientVersionHeader());

				if (r.resourceResolver() != RestResourceResolver.class)
					setResourceResolver(r.resourceResolver());
				if (r.logger() != RestLogger.Normal.class)
					setLogger(r.logger());
				if (r.callHandler() != RestCallHandler.class)
					setCallHandler(r.callHandler());
				if (r.infoProvider() != RestInfoProvider.class)
					setInfoProvider(r.infoProvider());

				for (Class<? extends Widget> cw : r.widgets())
					addWidget(cw);

				HtmlDoc hd = r.htmldoc();
				if (! hd.title().isEmpty())
					setHtmlTitle(hd.title());
				if (! hd.description().isEmpty())
					setHtmlDescription(hd.description());
				if (! hd.branding().isEmpty())
					setHtmlBranding(hd.branding());
				if (! hd.header().isEmpty())
					setHtmlHeader(hd.header());
				if (! hd.links().isEmpty())
					setHtmlLinks(hd.links());
				if (! hd.nav().isEmpty())
					setHtmlNav(hd.nav());
				if (! hd.aside().isEmpty())
					setHtmlAside(hd.aside());
				if (! hd.footer().isEmpty())
					setHtmlFooter(hd.footer());
				if (! hd.css().isEmpty())
					setHtmlCss(hd.css());
				if (! hd.cssUrl().isEmpty())
					setHtmlCssUrl(hd.cssUrl());
				if (! hd.noResultsMessage().isEmpty())
					setHtmlNoResultsMessage(hd.noResultsMessage());
				if (hd.nowrap())
					setHtmlNoWrap(true);
				if (hd.template() != HtmlDocTemplate.class)
					setHtmlTemplate(hd.template());
			}

			addResponseHandlers(
				StreamableHandler.class,
				WritableHandler.class,
				ReaderHandler.class,
				InputStreamHandler.class,
				RedirectHandler.class,
				DefaultHandler.class
			);

			addMimeTypes(
				"text/css css CSS",
				"text/html html htm HTML",
				"text/plain txt text TXT",
				"application/javascript js",
				"image/png png",
				"image/gif gif",
				"application/xml xml XML",
				"application/json json JSON"
			);
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}

	/**
	 * Adds the specified {@link Var} classes to this config.
	 * <p>
	 * These variables affect the variable resolver returned by {@link RestRequest#getVarResolverSession()} which is
	 * used to resolve string variables of the form <js>"$X{...}"</js>.
	 * <p>
	 * By default, this config includes the following variables:
	 * <ul class='spaced-list'>
	 * 	<li>{@link SystemPropertiesVar}
	 * 	<li>{@link EnvVariablesVar}
	 * 	<li>{@link ConfigFileVar}
	 * 	<li>{@link IfVar}
	 * 	<li>{@link SwitchVar}
	 * </ul>
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
	public RestConfig addVars(Class<?>...vars) {
		this.varResolverBuilder.vars(vars);
		return this;
	}

	/**
	 * Adds a var context object to this config.
	 * <p>
	 * Var context objects are read-only objects associated with the variable resolver for
	 * vars that require external information.
	 * <p>
	 * For example, the {@link ConfigFileVar} needs access to this resource's {@link ConfigFile} through the {@link ConfigFileVar#SESSION_config}
	 * object that can be specified as either a session object (temporary) or context object (permanent).
	 * In this case, we call the following code to add it to the context map:
	 * <p class='bcode'>
	 * 	config.addVarContextObject(<jsf>SESSION_config</jsf>, configFile);
	 * </p>
	 *
	 * @param name The context object key (i.e. the name that the Var class looks for).
	 * @param object The context object.
	 * @return This object (for method chaining).
	 */
	public RestConfig addVarContextObject(String name, Object object) {
		this.varResolverBuilder.contextObject(name, object);
		return this;
	}

	/**
	 * Overwrites the default config file with a custom config file.
	 * <p>
	 * By default, the config file is determined using the {@link RestResource#config() @RestResource.config()} annotation.
	 * This method allows you to programmatically override it with your own custom config file.
	 *
	 * @param configFile The new config file.
	 * @return This object (for method chaining).
	 */
	public RestConfig setConfigFile(ConfigFile configFile) {
		this.configFile = configFile;
		return this;
	}

	/**
	 * Sets a property on this resource.
	 * <p>
	 * This is the programmatic equivalent to the {@link RestResource#properties()} annotation.
	 *
	 * @param key The property name.
	 * @param value The property value.
	 * @return This object (for method chaining).
	 */
	public RestConfig setProperty(String key, Object value) {
		this.properties.put(key, value);
		return this;
	}

	/**
	 * Sets multiple properties on this resource.
	 * <p>
	 * This is the programmatic equivalent to the {@link RestResource#properties() @RestResource.properties()} annotation.
	 * <p>
	 * Values in the map are added to the existing properties and are overwritten if duplicates are found.
	 *
	 * @param properties The new properties to add to this config.
	 * @return This object (for method chaining).
	 */
	public RestConfig setProperties(Map<String,Object> properties) {
		this.properties.putAll(properties);
		return this;
	}

	/**
	 * Adds class-level bean filters to this resource.
	 * <p>
	 * This is the programmatic equivalent to the {@link RestResource#beanFilters() @RestResource.beanFilters()} annotation.
	 * <p>
	 * Values are added AFTER those found in the annotation and therefore take precedence over those defined via the annotation.
	 *
	 * @param beanFilters The bean filters to add to this config.
	 * @return This object (for method chaining).
	 */
	public RestConfig addBeanFilters(Class<?>...beanFilters) {
		this.beanFilters.addAll(Arrays.asList(beanFilters));
		return this;
	}

	/**
	 * Adds class-level pojo swaps to this resource.
	 * <p>
	 * This is the programmatic equivalent to the {@link RestResource#pojoSwaps() @RestResource.pojoSwaps()} annotation.
	 * <p>
	 * Values are added AFTER those found in the annotation and therefore take precedence over those defined via the annotation.
	 *
	 * @param pojoSwaps The pojo swaps to add to this config.
	 * @return This object (for method chaining).
	 */
	public RestConfig addPojoSwaps(Class<?>...pojoSwaps) {
		this.pojoSwaps.addAll(Arrays.asList(pojoSwaps));
		return this;
	}

	/**
	 * Specifies the serializer listener class to use for listening to non-fatal serialization errors.
	 * <p>
	 * This is the programmatic equivalent to the {@link RestResource#serializerListener() @RestResource.serializerListener()} annotation.
	 *
	 * @param listener The listener to add to this config.
	 * @return This object (for method chaining).
	 */
	public RestConfig serializerListener(Class<? extends SerializerListener> listener) {
		if (listener != SerializerListener.class)
			this.serializerListener = listener;
		return this;
	}

	/**
	 * Specifies the parser listener class to use for listening to non-fatal parse errors.
	 * <p>
	 * This is the programmatic equivalent to the {@link RestResource#parserListener() @RestResource.parserListener()} annotation.
	 *
	 * @param listener The listener to add to this config.
	 * @return This object (for method chaining).
	 */
	public RestConfig parserListener(Class<? extends ParserListener> listener) {
		if (listener != ParserListener.class)
			this.parserListener = listener;
		return this;
	}

	/**
	 * Adds class-level parameter resolvers to this resource.
	 * <p>
	 * This is the programmatic equivalent to the {@link RestResource#paramResolvers() @RestResource.paramResolvers()} annotation.
	 *
	 * @param paramResolvers The parameter resolvers to add to this config.
	 * @return This object (for method chaining).
	 */
	public RestConfig addParamResolvers(Class<? extends RestParam>...paramResolvers) {
		this.paramResolvers.addAll(Arrays.asList(paramResolvers));
		return this;
	}

	/**
	 * Adds class-level serializers to this resource.
	 * <p>
	 * This is the programmatic equivalent to the {@link RestResource#serializers() @RestResource.serializers()} annotation.
	 * <p>
	 * Values are added AFTER those found in the annotation and therefore take precedence over those defined via the annotation.
	 *
	 * @param serializers The serializer classes to add to this config.
	 * @return This object (for method chaining).
	 */
	public RestConfig addSerializers(Class<?>...serializers) {
		this.serializers.append(serializers);
		return this;
	}

	/**
	 * Adds class-level serializers to this resource.
	 * <p>
	 * Same as {@link #addSerializers(Class...)} except allows you to pass in serializer instances.
	 * The actual serializer ends up being the result of this operation using the bean filters, pojo swaps, and properties on this config:
	 * <p class='bcode'>
	 * 	serializer = serializer.builder().beanFilters(beanFilters).pojoSwaps(pojoSwaps).properties(properties).build();
	 * </p>
	 * <p>
	 * Values are added AFTER those found in the annotation and therefore take precedence over those defined via the annotation.
	 *
	 * @param serializers The serializers to add to this config.
	 * @return This object (for method chaining).
	 */
	public RestConfig addSerializers(Serializer...serializers) {
		this.serializers.append(serializers);
		return this;
	}

	/**
	 * Adds class-level parsers to this resource.
	 * <p>
	 * This is the programmatic equivalent to the {@link RestResource#parsers() @RestResource.parsers()} annotation.
	 * <p>
	 * Values are added AFTER those found in the annotation and therefore take precedence over those defined via the annotation.
	 *
	 * @param parsers The parser classes to add to this config.
	 * @return This object (for method chaining).
	 */
	public RestConfig addParsers(Class<?>...parsers) {
		this.parsers.append(parsers);
		return this;
	}

	/**
	 * Adds class-level parsers to this resource.
	 * <p>
	 * Same as {@link #addParsers(Class...)} except allows you to pass in parser instances.
	 * The actual parser ends up being the result of this operation using the bean filters, pojo swaps, and properties on this config:
	 * <p class='bcode'>
	 * 	parser = parser.builder().beanFilters(beanFilters).pojoSwaps(pojoSwaps).properties(properties).build();
	 * </p>
	 * <p>
	 * Values are added AFTER those found in the annotation and therefore take precedence over those defined via the annotation.
	 *
	 * @param parsers The parsers to add to this config.
	 * @return This object (for method chaining).
	 */
	public RestConfig addParsers(Parser...parsers) {
		this.parsers.append(parsers);
		return this;
	}

	/**
	 * Adds class-level encoders to this resource.
	 * <p>
	 * This is the programmatic equivalent to the {@link RestResource#encoders() @RestResource.encoders()} annotation.
	 * <p>
	 * Values are added AFTER those found in the annotation and therefore take precedence over those defined via the annotation.
	 * <p>
	 * By default, only the {@link IdentityEncoder} is included in this list.
	 *
	 * @param encoders The parser classes to add to this config.
	 * @return This object (for method chaining).
	 */
	public RestConfig addEncoders(Class<?>...encoders) {
		this.encoders.append(encoders);
		return this;
	}

	/**
	 * Adds class-level encoders to this resource.
	 * <p>
	 * Same as {@link #addEncoders(Class...)} except allows you to pass in encoder instances.
	 *
	 * @param encoders The encoders to add to this config.
	 * @return This object (for method chaining).
	 */
	public RestConfig addEncoders(Encoder...encoders) {
		this.encoders.append(encoders);
		return this;
	}

	/**
	 * Adds class-level converters to this resource.
	 * <p>
	 * This is the programmatic equivalent to the {@link RestResource#converters() @RestResource.converters()} annotation.
	 * <p>
	 * Values are added AFTER those found in the annotation and therefore take precedence over those defined via the annotation.
	 * <p>
	 * By default, this config includes the following converters:
	 * <ul class='spaced-list'>
	 * 	<li>{@link StreamableHandler}
	 * 	<li>{@link WritableHandler}
	 * 	<li>{@link ReaderHandler}
	 * 	<li>{@link InputStreamHandler}
	 * 	<li>{@link RedirectHandler}
	 * 	<li>{@link DefaultHandler}
	 * </ul>
	 *
	 * @param converters The converter classes to add to this config.
	 * @return This object (for method chaining).
	 */
	public RestConfig addConverters(Class<?>...converters) {
		this.converters.addAll(Arrays.asList(converters));
		return this;
	}

	/**
	 * Adds class-level encoders to this resource.
	 * <p>
	 * Same as {@link #addConverters(Class...)} except allows you to pass in converter instances.
	 *
	 * @param converters The converters to add to this config.
	 * @return This object (for method chaining).
	 */
	public RestConfig addConverters(RestConverter...converters) {
		this.converters.addAll(Arrays.asList(converters));
		return this;
	}

	/**
	 * Adds class-level guards to this resource.
	 * <p>
	 * This is the programmatic equivalent to the {@link RestResource#guards() @RestResource.guards()} annotation.
	 * <p>
	 * Values are added AFTER those found in the annotation and therefore take precedence over those defined via the annotation.
	 *
	 * @param guards The guard classes to add to this config.
	 * @return This object (for method chaining).
	 */
	public RestConfig addGuards(Class<?>...guards) {
		this.guards.addAll(Arrays.asList(guards));
		return this;
	}

	/**
	 * Adds class-level guards to this resource.
	 * <p>
	 * Same as {@link #addGuards(Class...)} except allows you to pass in guard instances.
	 *
	 * @param guards The guards to add to this config.
	 * @return This object (for method chaining).
	 */
	public RestConfig addGuards(RestGuard...guards) {
		this.guards.addAll(Arrays.asList(guards));
		return this;
	}

	/**
	 * Adds MIME-type definitions.
	 * <p>
	 * These definitions are used in the following locations for setting the media type on responses:
	 * <ul>
	 * 	<li>{@link RestRequest#getReaderResource(String)}
	 * 	<li>Static files resolved through {@link RestResource#staticFiles()}
	 * </ul>
	 * <p>
	 * Refer to {@link MimetypesFileTypeMap#addMimeTypes(String)} for an explanation of the format.
	 * <p>
	 * By default, this config includes the following mime-type definitions:
	 * <ul class='spaced-list'>
	 * 	<li><js>"text/css css CSS"</js>
	 * 	<li><js>"text/html html htm HTML"</js>
	 * 	<li><js>"text/plain txt text TXT"</js>
	 * 	<li><js>"application/javascript js"</js>
	 * 	<li><js>"image/png png"</js>
	 * 	<li><js>"image/gif gif"</js>
	 * 	<li><js>"application/xml xml XML"</js>
	 * 	<li><js>"application/json json JSON"</js>
	 * </ul>
	 *
	 * @param mimeTypes The MIME-types to add to this config.
	 * @return This object (for method chaining).
	 */
	public RestConfig addMimeTypes(String...mimeTypes) {
		for (String mimeType : mimeTypes)
			this.mimeTypes.addMimeTypes(mimeType);
		return this;
	}

	/**
	 * Adds class-level default HTTP request headers to this resource.
	 * <p>
	 * Default request headers are default values for when HTTP requests do not specify a header value.
	 * For example, you can specify a default value for <code>Accept</code> if a request does not specify that header value.
	 * <p>
	 * This is the programmatic equivalent to the {@link RestResource#defaultRequestHeaders() @RestResource.defaultRequestHeaders()} annotation.
	 *
	 * @param name The HTTP header name.
	 * @param value The HTTP header value.
	 * @return This object (for method chaining).
	 */
	public RestConfig addDefaultRequestHeader(String name, Object value) {
		this.defaultRequestHeaders.put(name, StringUtils.toString(value));
		return this;
	}

	/**
	 * Adds class-level default HTTP request headers to this resource.
	 * <p>
	 * Default request headers are default values for when HTTP requests do not specify a header value.
	 * For example, you can specify a default value for <code>Accept</code> if a request does not specify that header value.
	 * <p>
	 * This is the programmatic equivalent to the {@link RestResource#defaultRequestHeaders() @RestResource.defaultRequestHeaders()} annotation.
	 *
	 * @param headers HTTP headers of the form <js>"Name: Value"</js>.
	 * @return This object (for method chaining).
	 * @throws RestServletException If header string is not correctly formatted.
	 */
	public RestConfig addDefaultRequestHeaders(String...headers) throws RestServletException {
		for (String header : headers) {
			String[] h = RestUtils.parseHeader(header);
			if (h == null)
				throw new RestServletException("Invalid default request header specified: ''{0}''.  Must be in the format: ''Header-Name: header-value''", header);
			addDefaultRequestHeader(h[0], h[1]);
		}
		return this;
	}

	/**
	 * Adds class-level default HTTP response headers to this resource.
	 * <p>
	 * Default response headers are headers that will be appended to all responses if those headers have not already been
	 * 	set on the response object.
	 * <p>
	 * This is the programmatic equivalent to the {@link RestResource#defaultResponseHeaders() @RestResource.defaultResponseHeaders()} annotation.
	 * <p>
	 * Values are added AFTER those found in the annotation and therefore take precedence over those defined via the annotation.
	 *
	 * @param name The HTTP header name.
	 * @param value The HTTP header value.
	 * @return This object (for method chaining).
	 */
	public RestConfig addDefaultResponseHeader(String name, Object value) {
		this.defaultResponseHeaders.put(name, value);
		return this;
	}

	/**
	 * Adds class-level default HTTP response headers to this resource.
	 * <p>
	 * Default response headers are headers that will be appended to all responses if those headers have not already been
	 * 	set on the response object.
	 * <p>
	 * This is the programmatic equivalent to the {@link RestResource#defaultResponseHeaders() @RestResource.defaultResponseHeaders()} annotation.
	 *
	 * @param headers HTTP headers of the form <js>"Name: Value"</js>.
	 * @return This object (for method chaining).
	 * @throws RestServletException If header string is not correctly formatted.
	 */
	public RestConfig addDefaultResponseHeaders(String...headers) throws RestServletException {
		for (String header : headers) {
			String[] h = RestUtils.parseHeader(header);
			if (h == null)
				throw new RestServletException("Invalid default response header specified: ''{0}''.  Must be in the format: ''Header-Name: header-value''", header);
			addDefaultResponseHeader(h[0], h[1]);
		}
		return this;
	}

	/**
	 * Adds class-level response handler classes to this resource.
	 * <p>
	 * Response handlers are responsible for converting various POJOs returned by REST methods into actual HTTP responses.
	 * <p>
	 * By default, this config includes the following response handlers:
	 * <ul class='spaced-list'>
	 * 	<li>{@link StreamableHandler}
	 * 	<li>{@link WritableHandler}
	 * 	<li>{@link ReaderHandler}
	 * 	<li>{@link InputStreamHandler}
	 * 	<li>{@link RedirectHandler}
	 * 	<li>{@link DefaultHandler}
	 * </ul>
	 * <p>
	 * This is the programmatic equivalent to the {@link RestResource#responseHandlers() @RestResource.responseHandlers()} annotation.
	 *
	 * @param responseHandlers The response handlers to add to this config.
	 * @return This object (for method chaining).
	 */
	public RestConfig addResponseHandlers(Class<?>...responseHandlers) {
		this.responseHandlers.addAll(Arrays.asList(responseHandlers));
		return this;
	}

	/**
	 * Adds class-level response handlers to this resource.
	 * <p>
	 * Same as {@link #addResponseHandlers(Class...)} except allows you to pass in response handler instances.
	 *
	 * @param responseHandlers The response handlers to add to this config.
	 * @return This object (for method chaining).
	 */
	public RestConfig addResponseHandlers(ResponseHandler...responseHandlers) {
		this.responseHandlers.addAll(Arrays.asList(responseHandlers));
		return this;
	}

	/**
	 * Adds a child resource to this resource.
	 * <p>
	 * Child resources are resources that are accessed under the path of the parent resource.
	 * <p>
	 * This is the programmatic equivalent to the {@link RestResource#children() @RestResource.children()} annotation.
	 *
	 * @param path The child path of the resource.  Must conform to {@link RestResource#path()} format.
	 * @param child The child resource.
	 * @return This object (for method chaining).
	 */
	public RestConfig addChildResource(String path, Object child) {
		this.childResources.add(new Pair<String,Object>(path, child));
		return this;
	}

	/**
	 * Add child resources to this resource.
	 * <p>
	 * Child resources are resources that are accessed under the path of the parent resource.
	 * <p>
	 * This is the programmatic equivalent to the {@link RestResource#children() @RestResource.children()} annotation.
	 *
	 * @param children The child resources to add to this resource.
	 * Children must be annotated with {@link RestResource#path()} to identify the child path.
	 * @return This object (for method chaining).
	 */
	public RestConfig addChildResources(Object...children) {
		this.childResources.addAll(Arrays.asList(children));
		return this;
	}

	/**
	 * Add child resources to this resource.
	 * <p>
	 * Child resources are resources that are accessed under the path of the parent resource.
	 * <p>
	 * This is the programmatic equivalent to the {@link RestResource#children() @RestResource.children()} annotation.
	 *
	 * @param children The child resources to add to this resource.
	 * Children must be annotated with {@link RestResource#path()} to identify the child path.
	 * @return This object (for method chaining).
	 */
	public RestConfig addChildResources(Class<?>...children) {
		this.childResources.addAll(Arrays.asList(children));
		return this;
	}

	/**
	 * Specifies the list of supported <code>Accept</code> media types for this resource.
	 * <p>
	 * This overrides the media types inferred from the parsers on this resource.
	 * <p>
	 * There is no annotation equivalent to this method call.
	 *
	 * @param mediaTypes The new list of media types supported by this resource.
	 * @return This object (for method chaining).
	 */
	public RestConfig setSupportedAcceptTypes(String...mediaTypes) {
		supportedAcceptTypes = new ArrayList<MediaType>();
		for (String mediaType : mediaTypes)
			supportedAcceptTypes.add(MediaType.forString(mediaType));
		return this;
	}

	/**
	 * Specifies the list of supported <code>Accept</code> media types for this resource.
	 * <p>
	 * This overrides the media types inferred from the parsers on this resource.
	 * <p>
	 * There is no annotation equivalent to this method call.
	 *
	 * @param mediaTypes The new list of media types supported by this resource.
	 * @return This object (for method chaining).
	 */
	public RestConfig setSupportedAcceptTypes(MediaType...mediaTypes) {
		supportedAcceptTypes = Arrays.asList(mediaTypes);
		return this;
	}

	/**
	 * Specifies the list of supported <code>Content-Type</code> media types for this resource.
	 * <p>
	 * This overrides the media types inferred from the serializers on this resource.
	 * <p>
	 * There is no annotation equivalent to this method call.
	 *
	 * @param mediaTypes The new list of media types supported by this resource.
	 * @return This object (for method chaining).
	 */
	public RestConfig setSupportedContentTypes(String...mediaTypes) {
		supportedContentTypes = new ArrayList<MediaType>();
		for (String mediaType : mediaTypes)
			supportedContentTypes.add(MediaType.forString(mediaType));
		return this;
	}

	/**
	 * Specifies the list of supported <code>Content-Type</code> media types for this resource.
	 * <p>
	 * This overrides the media types inferred from the serializers on this resource.
	 * <p>
	 * There is no annotation equivalent to this method call.
	 *
	 * @param mediaTypes The new list of media types supported by this resource.
	 * @return This object (for method chaining).
	 */
	public RestConfig setSupportedContentTypes(MediaType...mediaTypes) {
		supportedContentTypes = Arrays.asList(mediaTypes);
		return this;
	}

	/**
	 * Specifies the stylesheets that make up the contents of the page <js>"/resource-path/styles.css"</js>.
	 * <p>
	 * This is the programmatic equivalent to the {@link RestResource#stylesheet() @RestResource.stylesheet()} annotation.
	 * <p>
	 * The object types can be any of the following:
	 * <ul>
	 * 	<li>{@link InputStream}
	 * 	<li>{@link Reader}
	 * 	<li>{@link File}
	 * 	<li>{@link CharSequence}
	 * 	<li><code><jk>byte</jk>[]</code>
	 * </ul>
	 * The contents of all these stylesheets will be aggregated into a single page in the order they are specified in this list.
	 *
	 * @param styleSheets The new list of style sheets that make up the <code>styles.css</code> page.
	 * @return This object (for method chaining).
	 */
	public RestConfig setStyleSheet(Object...styleSheets) {
		this.styleSheets = new ArrayList<Object>(Arrays.asList(styleSheets));
		return this;
	}

	/**
	 * Specifies the stylesheet that make up the contents of the page <js>"/resource-path/styles.css"</js>.
	 * <p>
	 * This is the programmatic equivalent to the {@link RestResource#stylesheet() @RestResource.stylesheet()} annotation.
	 * <p>
	 * Use this method to specify a resource located in the classpath.
	 * This call uses the {@link Class#getResourceAsStream(String)} method to retrieve the stylesheet contents.
	 *
	 * @param resourceClass The resource class used to resolve the resource stream.
	 * @param resourcePath The path passed to the {@link Class#getResourceAsStream(String)} method.
	 * Can also be a path starting with <js>"file://"</js> denoting a location to pull from the file system.
	 * @return This object (for method chaining).
	 */
	public RestConfig setStyleSheet(Class<?> resourceClass, String resourcePath) {
		this.styleSheets = new ArrayList<Object>();
		this.styleSheets.add(new Pair<Class<?>,String>(resourceClass, resourcePath));
		return this;
	}

	/**
	 * Adds to the stylesheet that make up the contents of the page <js>"/resource-path/styles.css"</js>.
	 * <p>
	 * Same as {@link #setStyleSheet(Object...)} except appends to the existing list instead of replacing.
	 *
	 * @param styleSheets The list of style sheets to add that make up the <code>styles.css</code> page.
	 * @return This object (for method chaining).
	 */
	public RestConfig addStyleSheet(Object...styleSheets) {
		if (this.styleSheets == null)
			this.styleSheets = new ArrayList<Object>();
		this.styleSheets.addAll(Arrays.asList(styleSheets));
		return this;
	}

	/**
	 * Adds to the stylesheet that make up the contents of the page <js>"/resource-path/styles.css"</js>.
	 * <p>
	 * Same as {@link #setStyleSheet(Class,String)} except appends to the existing list instead of replacing.
	 *
	 * @param resourceClass The resource class used to resolve the resource stream.
	 * @param resourcePath The path passed to the {@link Class#getResourceAsStream(String)} method.
	 * @return This object (for method chaining).
	 */
	public RestConfig addStyleSheet(Class<?> resourceClass, String resourcePath) {
		if (this.styleSheets == null)
			this.styleSheets = new ArrayList<Object>();
		this.styleSheets.add(new Pair<Class<?>,String>(resourceClass, resourcePath));
		return this;
	}

	/**
	 * Specifies the icon contents that make up the contents of the page <js>"/resource-path/favicon.ico"</js>.
	 * <p>
	 * This is the programmatic equivalent to the {@link RestResource#favicon() @RestResource.favicon()} annotation.
	 * <p>
	 * The object type can be any of the following:
	 * <ul>
	 * 	<li>{@link InputStream}
	 * 	<li>{@link File}
	 * 	<li><code><jk>byte</jk>[]</code>
	 * </ul>
	 *
	 * @param favIcon The contents that make up the <code>favicon.ico</code> page.
	 * @return This object (for method chaining).
	 */
	public RestConfig setFavIcon(Object favIcon) {
		this.favIcon = favIcon;
		return this;
	}

	/**
	 * Specifies the icon contents that make up the contents of the page <js>"/resource-path/favicon.ico"</js>.
	 * <p>
	 * This is the programmatic equivalent to the {@link RestResource#favicon() @RestResource.favicon()} annotation.
	 * <p>
	 * Use this method to specify a resource located in the classpath.
	 * This call uses the {@link Class#getResourceAsStream(String)} method to retrieve the stylesheet contents.
	 *
	 * @param resourceClass The resource class used to resolve the resource stream.
	 * @param resourcePath The path passed to the {@link Class#getResourceAsStream(String)} method.
	 * Can also be a path starting with <js>"file://"</js> denoting a location to pull from the file system.
	 * @return This object (for method chaining).
	 */
	public RestConfig setFavIcon(Class<?> resourceClass, String resourcePath) {
		this.favIcon = new Pair<Class<?>,String>(resourceClass, resourcePath);
		return this;
	}

	/**
	 * Appends to the static files resource map.
	 * <p>
	 * Use this method to specify resources located in the classpath to be served up as static files.
	 * <p>
	 * This is the programmatic equivalent to the {@link RestResource#staticFiles() @RestResource.staticFiles()} annotation.
	 *
	 * @param resourceClass The resource class used to resolve the resource streams.
	 * @param staticFilesString A JSON string denoting a map of child URLs to classpath subdirectories.
	 * For example, if this string is <js>"{htdocs:'docs'}"</js> with class <code>com.foo.MyResource</code>,
	 * then URLs of the form <js>"/resource-path/htdocs/..."</js> will resolve to files located in the <code>com.foo.docs</code> package.
	 * @return This object (for method chaining).
	 */
	public RestConfig addStaticFiles(Class<?> resourceClass, String staticFilesString) {
		if (staticFiles == null)
			staticFiles = new ArrayList<Object>();
		staticFiles.add(new Pair<Class<?>,Object>(resourceClass, staticFilesString));
		return this;
	}

	/**
	 * Overrides the default REST resource resolver.
	 * <p>
	 * The resource resolver is used to resolve instances from {@link Class} objects defined in the {@link RestResource#children()} annotation.
	 * The default value is the base class {@link RestResourceResolver}.
	 * <p>
	 * This is the programmatic equivalent to the {@link RestResource#resourceResolver() @RestResource.resourceResolver()} annotation.
	 *
	 * @param resourceResolver The new resource resolver.
	 * @return This object (for method chaining).
	 */
	public RestConfig setResourceResolver(Class<? extends RestResourceResolver> resourceResolver) {
		this.resourceResolver = resourceResolver;
		return this;
	}

	/**
	 * Overrides the default REST resource resolver.
	 * <p>
	 * Same as {@link #setResourceResolver(Class)} except allows you to specify an instance instead of a class.
	 *
	 * @param resourceResolver The new resource resolver.
	 * @return This object (for method chaining).
	 */
	public RestConfig setResourceResolver(RestResourceResolver resourceResolver) {
		this.resourceResolver = resourceResolver;
		return this;
	}

	/**
	 * Sets the URL path of the resource <js>"/foobar"</js>.
	 * <p>
	 * This is the programmatic equivalent to the {@link RestResource#path() @RestResource.path()} annotation.
	 *
	 * @param path The URL path of this resource.
	 * @return This object (for method chaining).
	 */
	public RestConfig setPath(String path) {
		if (startsWith(path, '/'))
			path = path.substring(1);
		this.path = path;
		return this;
	}

	/**
	 * Sets name of the header used to denote the client version on HTTP requests.
	 * <p>
	 * This is the programmatic equivalent to the {@link RestResource#clientVersionHeader() @RestResource.clientVersionHeader()} annotation.
	 *
	 * @param clientVersionHeader The name of the HTTP header that denotes the client version.
	 * @return This object (for method chaining).
	 */
	public RestConfig setClientVersionHeader(String clientVersionHeader) {
		this.clientVersionHeader = clientVersionHeader;
		return this;
	}

	/**
	 * Sets the HTML page title.
	 * <p>
	 * The format of this value is plain text.
	 * <p>
	 * It gets wrapped in a <code><xt>&lt;h3&gt; <xa>class</xa>=<xs>'title'</xs>&gt;</xt></code> element and then added
	 * 	to the <code><xt>&lt;header&gt;</code> section on the page.
	 * <p>
	 * If not specified, the page title is pulled from one of the following locations:
	 * <ol>
	 * 	<li><code>{servletClass}.{methodName}.pageTitle</code> resource bundle value.
	 * 	<li><code>{servletClass}.pageTitle</code> resource bundle value.
	 * 	<li><code><ja>@RestResource</ja>(title)</code> annotation.
	 * 	<li><code>{servletClass}.title</code> resource bundle value.
	 * 	<li><code>info/title</code> entry in swagger file.
	 * <ol>
	 * <p>
	 * This field can contain variables (e.g. <js>"$L{my.localized.variable}"</js>).
	 * <p>
	 * A value of <js>"NONE"</js> can be used to force no value.
	 * <p>
	 * <ul class='doctree'>
	 * 	<li class='info'>
	 * 		In most cases, you'll simply want to use the <code>@RestResource(title)</code> annotation to specify the
	 * 		page title.
	 * 		However, this annotation is provided in cases where you want the page title to be different that the one
	 * 		shown in the swagger document.
	 * </ul>
	 * <p>
	 * This is the programmatic equivalent to the {@link HtmlDoc#title() @HtmlDoc.title()} annotation.
	 *
	 * @param value The HTML page title.
	 * @return This object (for method chaining).
	 */
	public RestConfig setHtmlTitle(String value) {
		this.htmlTitle = value;
		return this;
	}

	/**
	 * Sets the HTML page description.
	 * <p>
	 * The format of this value is plain text.
	 * <p>
	 * It gets wrapped in a <code><xt>&lt;h5&gt; <xa>class</xa>=<xs>'description'</xs>&gt;</xt></code> element and then
	 * 	added to the <code><xt>&lt;header&gt;</code> section on the page.
	 * <p>
	 * If not specified, the page title is pulled from one of the following locations:
	 * <ol>
	 * 	<li><code>{servletClass}.{methodName}.pageText</code> resource bundle value.
	 * 	<li><code>{servletClass}.pageText</code> resource bundle value.
	 * 	<li><code><ja>@RestMethod</ja>(summary)</code> annotation.
	 * 	<li><code>{servletClass}.{methodName}.summary</code> resource bundle value.
	 * 	<li><code>summary</code> entry in swagger file for method.
	 * 	<li><code>{servletClass}.description</code> resource bundle value.
	 * 	<li><code>info/description</code> entry in swagger file.
	 * <ol>
	 * <p>
	 * This field can contain variables (e.g. <js>"$L{my.localized.variable}"</js>).
	 * <p>
	 * A value of <js>"NONE"</js> can be used to force no value.
	 * <p>
	 * <ul class='doctree'>
	 * 	<li class='info'>
	 * 		In most cases, you'll simply want to use the <code>@RestResource(description)</code> or
	 * 		<code>@RestMethod(summary)</code> annotations to specify the page text.
	 * 		However, this annotation is provided in cases where you want the text to be different that the values shown
	 * 		in the swagger document.
	 * </ul>
	 * <p>
	 * This is the programmatic equivalent to the {@link HtmlDoc#description() @HtmlDoc.description()} annotation.
	 *
	 * @param value The HTML page description.
	 * @return This object (for method chaining).
	 */
	public RestConfig setHtmlDescription(String value) {
		this.htmlDescription = value;
		return this;
	}

	/**
	 * Sets the HTML page branding in the header section of the page generated by the default HTML doc template.
	 * <p>
	 * The format of this value is HTML.
	 * <p>
	 * This is arbitrary HTML that can be added to the header section to provide basic custom branding on the page.
	 * <p>
	 * This field can contain variables (e.g. <js>"$L{my.localized.variable}"</js>).
	 * <p>
	 * A value of <js>"NONE"</js> can be used to force no value.
	 * <p>
	 * This is the programmatic equivalent to the {@link HtmlDoc#branding() @HtmlDoc.branding()} annotation.
	 *
	 * @param value The HTML page branding.
	 * @return This object (for method chaining).
	 */
	public RestConfig setHtmlBranding(String value) {
		this.htmlBranding = value;
		return this;
	}

	/**
	 * Sets the HTML header section contents.
	 * <p>
	 * The format of this value is HTML.
	 * <p>
	 * The page header normally contains the title and description, but this value can be used to override the contents
	 * 	to be whatever you want.
	 * <p>
	 * When a value is specified, the {@link #setHtmlTitle(String)} and {@link #setHtmlDescription(String)} values will be ignored.
	 * <p>
	 * A value of <js>"NONE"</js> can be used to force no header.
	 * <p>
	 * This field can contain variables (e.g. <js>"$L{my.localized.variable}"</js>).
	 * <p>
	 * This is the programmatic equivalent to the {@link HtmlDoc#header() @HtmlDoc.header()} annotation.
	 *
	 * @param value The HTML header section contents.
	 * @return This object (for method chaining).
	 */
	public RestConfig setHtmlHeader(String value) {
		this.htmlHeader = value;
		return this;
	}

	/**
	 * Sets the links in the HTML nav section.
	 * <p>
	 * The format of this value is a lax-JSON map of key/value pairs where the keys are the link text and the values are
	 * 	relative (to the servlet) or absolute URLs.
	 * <p>
	 * The page links are positioned immediately under the title and text.
	 * <p>
	 * This field can contain variables (e.g. <js>"$L{my.localized.variable}"</js>).
	 * <p>
	 * A value of <js>"NONE"</js> can be used to force no value.
	 * <p>
	 * This field can also use URIs of any support type in {@link UriResolver}.
	 * <p>
	 * This is the programmatic equivalent to the {@link HtmlDoc#links() @HtmlDoc.links()} annotation.
	 *
	 * @param value The HTML nav section links links.
	 * @return This object (for method chaining).
	 */
	public RestConfig setHtmlLinks(String value) {
		this.htmlLinks = value;
		return this;
	}

	/**
	 * Sets the HTML nav section contents.
	 * <p>
	 * The format of this value is HTML.
	 * <p>
	 * The nav section of the page contains the links.
	 * <p>
	 * The format of this value is HTML.
	 * <p>
	 * When a value is specified, the {@link #setHtmlLinks(String)} value will be ignored.
	 * <p>
	 * This field can contain variables (e.g. <js>"$L{my.localized.variable}"</js>).
	 * <p>
	 * A value of <js>"NONE"</js> can be used to force no value.
	 * <p>
	 * This is the programmatic equivalent to the {@link HtmlDoc#nav() @HtmlDoc.nav()} annotation.
	 *
	 * @param value The HTML nav section contents.
	 * @return This object (for method chaining).
	 */
	public RestConfig setHtmlNav(String value) {
		this.htmlNav = value;
		return this;
	}

	/**
	 * Sets the HTML aside section contents.
	 * <p>
	 * The format of this value is HTML.
	 * <p>
	 * The aside section typically floats on the right side of the page.
	 * <p>
	 * This field can contain variables (e.g. <js>"$L{my.localized.variable}"</js>).
	 * <p>
	 * A value of <js>"NONE"</js> can be used to force no value.
	 * <p>
	 * This is the programmatic equivalent to the {@link HtmlDoc#aside() @HtmlDoc.aside()} annotation.
	 *
	 * @param value The HTML aside section contents.
	 * @return This object (for method chaining).
	 */
	public RestConfig setHtmlAside(String value) {
		this.htmlAside = value;
		return this;
	}

	/**
	 * Sets the HTML footer section contents.
	 * <p>
	 * The format of this value is HTML.
	 * <p>
	 * The footer section typically floats on the bottom of the page.
	 * <p>
	 * This field can contain variables (e.g. <js>"$L{my.localized.variable}"</js>).
	 * <p>
	 * A value of <js>"NONE"</js> can be used to force no value.
	 * <p>
	 * This is the programmatic equivalent to the {@link HtmlDoc#footer() @HtmlDoc.footer()} annotation.
	 *
	 * @param value The HTML footer section contents.
	 * @return This object (for method chaining).
	 */
	public RestConfig setHtmlFooter(String value) {
		this.htmlFooter = value;
		return this;
	}

	/**
	 * Sets the HTML CSS style section contents.
	 * <p>
	 * The format of this value is CSS.
	 * <p>
	 * This field can contain variables (e.g. <js>"$L{my.localized.variable}"</js>).
	 * <p>
	 * A value of <js>"NONE"</js> can be used to force no value.
	 * <p>
	 * This is the programmatic equivalent to the {@link HtmlDoc#css() @HtmlDoc.css()} annotation.
	 *
	 * @param value The HTML CSS style section contents.
	 * @return This object (for method chaining).
	 */
	public RestConfig setHtmlCss(String value) {
		this.htmlCss = value;
		return this;
	}

	/**
	 * Sets the CSS URL in the HTML CSS style section.
	 * <p>
	 * The format of this value is a URL.
	 * <p>
	 * Specifies the URL to the stylesheet to add as a link in the style tag in the header.
	 * <p>
	 * The format of this value is CSS.
	 * <p>
	 * This field can contain variables (e.g. <js>"$L{my.localized.variable}"</js>) and can use URL protocols defined
	 * 	by {@link UriResolver}.
	 * <p>
	 * This is the programmatic equivalent to the {@link HtmlDoc#cssUrl() @HtmlDoc.cssUrl()} annotation.
	 *
	 * @param value The CSS URL in the HTML CSS style section.
	 * @return This object (for method chaining).
	 */
	public RestConfig setHtmlCssUrl(String value) {
		this.htmlCssUrl = value;
		return this;
	}

	/**
	 * Shorthand method for forcing the rendered HTML content to be no-wrap.
	 * <p>
	 * This is the programmatic equivalent to the {@link HtmlDoc#nowrap() @HtmlDoc.nowrap()} annotation.
	 *
	 * @param value The new nowrap setting.
	 * @return This object (for method chaining).
	 */
	public RestConfig setHtmlNoWrap(boolean value) {
		this.htmlNoWrap = value;
		return this;
	}

	/**
	 * Specifies the text to display when serializing an empty array or collection.
	 * <p>
	 * This is the programmatic equivalent to the {@link HtmlDoc#noResultsMessage() @HtmlDoc.noResultsMessage()} annotation.
	 *
	 * @param value The text to display when serializing an empty array or collection.
	 * @return This object (for method chaining).
	 */
	public RestConfig setHtmlNoResultsMessage(String value) {
		this.htmlNoResultsMessage = value;
		return this;
	}

	/**
	 * Specifies the template class to use for rendering the HTML page.
	 * <p>
	 * By default, uses {@link HtmlDocTemplateBasic} to render the contents, although you can provide
	 * 	 your own custom renderer or subclasses from the basic class to have full control over how the page is
	 * 	rendered.
	 * <p>
	 * This is the programmatic equivalent to the {@link HtmlDoc#template() @HtmlDoc.template()} annotation.
	 *
	 * @param value The HTML page template to use to render the HTML page.
	 * @return This object (for method chaining).
	 */
	public RestConfig setHtmlTemplate(Class<? extends HtmlDocTemplate> value) {
		this.htmlTemplate = value;
		return this;
	}

	/**
	 * Specifies the template class to use for rendering the HTML page.
	 * <p>
	 * By default, uses {@link HtmlDocTemplateBasic} to render the contents, although you can provide
	 * 	 your own custom renderer or subclasses from the basic class to have full control over how the page is
	 * 	rendered.
	 * <p>
	 * This is the programmatic equivalent to the {@link HtmlDoc#template() @HtmlDoc.template()} annotation.
	 *
	 * @param value The HTML page template to use to render the HTML page.
	 * @return This object (for method chaining).
	 */
	public RestConfig setHtmlTemplate(HtmlDocTemplate value) {
		this.htmlTemplate = value;
		return this;
	}

	/**
	 * Defines widgets that can be used in conjunction with string variables of the form <js>"$W{name}"</js>to quickly
	 * 	generate arbitrary replacement text.
	 * <p>
	 * Widgets are inherited from parent to child, but can be overridden by reusing the widget name.
	 *
	 * @param value The widget class to add.
	 * @return This object (for method chaining).
	 */
	public RestConfig addWidget(Class<? extends Widget> value) {
		Widget w = ClassUtils.newInstance(Widget.class, value);
		this.widgets.put(w.getName(), w);
		return this;
	}

	/**
	 * Overrides the logger for the resource.
	 * <p>
	 * This is the programmatic equivalent to the {@link RestResource#logger() @RestResource.logger()} annotation.
	 *
	 * @param logger The new logger for this resource.  Can be <jk>null</jk> to disable logging.
	 * @return This object (for method chaining).
	 */
	public RestConfig setLogger(Class<? extends RestLogger> logger) {
		this.logger = logger;
		return this;
	}

	/**
	 * Overrides the logger for the resource.
	 * <p>
	 * This is the programmatic equivalent to the {@link RestResource#logger() @RestResource.logger()} annotation.
	 *
	 * @param logger The new logger for this resource.  Can be <jk>null</jk> to disable logging.
	 * @return This object (for method chaining).
	 */
	public RestConfig setLogger(RestLogger logger) {
		this.logger = logger;
		return this;
	}

	/**
	 * Overrides the call handler for the resource.
	 * <p>
	 * The call handler is the object that handles execution of REST HTTP calls.
	 * Subclasses can be created that customize the behavior of how REST calls are handled.
	 * <p>
	 * This is the programmatic equivalent to the {@link RestResource#callHandler() @RestResource.callHandler()} annotation.
	 *
	 * @param restHandler The new call handler for this resource.
	 * @return This object (for method chaining).
	 */
	public RestConfig setCallHandler(Class<? extends RestCallHandler> restHandler) {
		this.callHandler = restHandler;
		return this;
	}

	/**
	 * Overrides the call handler for the resource.
	 * <p>
	 * The call handler is the object that handles execution of REST HTTP calls.
	 * Subclasses can be created that customize the behavior of how REST calls are handled.
	 * <p>
	 * This is the programmatic equivalent to the {@link RestResource#callHandler() @RestResource.callHandler()} annotation.
	 *
	 * @param restHandler The new call handler for this resource.
	 * @return This object (for method chaining).
	 */
	public RestConfig setCallHandler(RestCallHandler restHandler) {
		this.callHandler = restHandler;
		return this;
	}

	/**
	 * Overrides the info provider for the resource.
	 * <p>
	 * The info provider provides all the various information about a resource such as the Swagger documentation.
	 * Subclasses can be created that customize the information.
	 * <p>
	 * This is the programmatic equivalent to the {@link RestResource#infoProvider() @RestResource.infoProvider()} annotation.
	 *
	 * @param infoProvider The new info provider for this resource.
	 * @return This object (for method chaining).
	 */
	public RestConfig setInfoProvider(Class<? extends RestInfoProvider> infoProvider) {
		this.infoProvider = infoProvider;
		return this;
	}

	/**
	 * Overrides the info provider for the resource.
	 * <p>
	 * The info provider provides all the various information about a resource such as the Swagger documentation.
	 * Subclasses can be created that customize the information.
	 * <p>
	 * This is the programmatic equivalent to the {@link RestResource#infoProvider() @RestResource.infoProvider()} annotation.
	 *
	 * @param infoProvider The new info provider for this resource.
	 * @return This object (for method chaining).
	 */
	public RestConfig setInfoProvider(RestInfoProvider infoProvider) {
		this.infoProvider = infoProvider;
		return this;
	}

	/**
	 * Creates a new {@link PropertyStore} object initialized with the properties defined in this config.
	 * @return A new property store.
	 */
	protected PropertyStore createPropertyStore() {
		return PropertyStore.create().addProperties(properties);
	}


	//----------------------------------------------------------------------------------------------------
	// Methods that give access to the config file, var resolver, and properties.
	//----------------------------------------------------------------------------------------------------

	/**
	 * Returns the external configuration file for this resource.
	 * <p>
	 * The configuration file location is determined via the {@link RestResource#config() @RestResource.config()} annotation on the resource.
	 * <p>
	 * The config file can be programmatically overridden by adding the following method to your resource:
	 * <p class='bcode'>
	 * 	<jk>public</jk> ConfigFile createConfigFile(ServletConfig servletConfig) <jk>throws</jk> ServletException;
	 * </p>
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
	 * <p>
	 * The configuration properties are determined via the {@link RestResource#properties()} annotation on the resource.
	 * <p>
	 * The configuration properties can be augmented programmatically by adding the following method to your resource:
	 * <p class='bcode'>
	 * 	<jk>public</jk> ObjectMap createProperties(ServletConfig servletConfig) <jk>throws</jk> ServletException;
	 * </p>
	 * <p>
	 * These properties can be modified during servlet initialization.
	 * However, any modifications made after {@link RestServlet#init(RestConfig)} has been called will have no effect.
	 *
	 * @return The configuration properties for this resource.  Never <jk>null</jk>.
	 */
	public ObjectMap getProperties() {
		return properties;
	}

	/**
	 * Creates the variable resolver for this resource.
	 * <p>
	 * The variable resolver returned by this method can resolve the following variables:
	 * <ul>
	 * 	<li>{@link SystemPropertiesVar}
	 * 	<li>{@link EnvVariablesVar}
	 * 	<li>{@link ConfigFileVar}
	 * 	<li>{@link IfVar}
	 * 	<li>{@link SwitchVar}
	 * </ul>
	 * <p>
	 * Note that the variables supported here are only a subset of those returned by {@link RestRequest#getVarResolverSession()}.
	 *
	 * @return The variable resolver for this resource.  Never <jk>null</jk>.
	 */
	public VarResolverBuilder getVarResolverBuilder() {
		return varResolverBuilder;
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
