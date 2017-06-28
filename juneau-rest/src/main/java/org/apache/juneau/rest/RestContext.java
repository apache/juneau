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
import static org.apache.juneau.internal.ArrayUtils.*;
import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.FileUtils.*;
import static org.apache.juneau.internal.IOUtils.*;
import static org.apache.juneau.internal.ReflectionUtils.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import javax.activation.*;
import javax.servlet.*;

import org.apache.juneau.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.html.*;
import org.apache.juneau.http.*;
import org.apache.juneau.ini.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.annotation.Properties;
import org.apache.juneau.rest.vars.*;
import org.apache.juneau.rest.widget.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.utils.*;

/**
 * Contains all the configuration on a REST resource and the entry points for handling REST calls.
 *
 * <p>
 * See {@link PropertyStore} for more information about context properties.
 */
public final class RestContext extends Context {

	/**
	 * <b>Configuration property:</b>  Enable header URL parameters.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RestServlet.allowHeaderParams"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>true</jk>
	 * </ul>
	 *
	 * <p>
	 * When enabled, headers such as <js>"Accept"</js> and <js>"Content-Type"</js> to be passed in as URL query
	 * parameters.
	 * For example:  <js>"?Accept=text/json&amp;Content-Type=text/json"</js>
	 *
	 * <p>
	 * Parameter names are case-insensitive.
	 *
	 * <p>
	 * Useful for debugging REST interface using only a browser.
	 *
	 * <p>
	 * Applicable to servlet class only.
	 */
	public static final String REST_allowHeaderParams = "RestServlet.allowHeaderParams";

	/**
	 * <b>Configuration property:</b>  Enable <js>"method"</js> URL parameter for specific HTTP methods.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RestServlet.allowMethodParam"</js>
	 * 	<li><b>Data type:</b> <code>String</code>
	 * 	<li><b>Default:</b> <js>""</js>
	 * </ul>
	 *
	 * <p>
	 * When specified, the HTTP method can be overridden by passing in a <js>"method"</js> URL parameter on a regular
	 * GET request.
	 * For example:  <js>"?method=OPTIONS"</js>
	 *
	 * <p>
	 * Format is a comma-delimited list of HTTP method names that can be passed in as a method parameter.
	 * Parameter name is case-insensitive.
	 * Use "*" to represent all methods.
	 * For backwards compatibility, "true" also means "*".
	 *
	 * <p>
	 * Note that per the <a class="doclink"
	 * href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html">HTTP specification</a>, special care should
	 * be taken when allowing non-safe (POST, PUT, DELETE) methods to be invoked through GET requests.
	 *
	 * <p>
	 * Applicable to servlet class only.
	 *
	 * <p>
	 * Example: <js>"HEAD,OPTIONS"</js>
	 */
	public static final String REST_allowMethodParam = "RestServlet.allowMethodParam";

	/**
	 * <b>Configuration property:</b>  Enable <js>"body"</js> URL parameter.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RestServlet.allowBodyParam"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>true</jk>
	 * </ul>
	 *
	 * <p>
	 * When enabled, the HTTP body content on PUT and POST requests can be passed in as text using the <js>"body"</js>
	 * URL parameter.
	 * For example:  <js>"?body={name:'John%20Smith',age:45}"</js>
	 *
	 * <p>
	 * Parameter name is case-insensitive.
	 *
	 * <p>
	 * Useful for debugging PUT and POST methods using only a browser.
	 *
	 * <p>
	 * Applicable to servlet class only.
	 */
	public static final String REST_allowBodyParam = "RestServlet.allowBodyParam";

	/**
	 * <b>Configuration property:</b>  Render stack traces.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RestServlet.renderResponseStackTraces"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * </ul>
	 *
	 * <p>
	 * Render stack traces in HTTP response bodies when errors occur.
	 *
	 * <p>
	 * When enabled, Java stack traces will be rendered in the output response.
	 * Useful for debugging, although allowing stack traces to be rendered may cause security concerns.
	 *
	 * <p>
	 * Applicable to servlet class only.
	 */
	public static final String REST_renderResponseStackTraces = "RestServlet.renderResponseStackTraces";

	/**
	 * <b>Configuration property:</b>  Use stack trace hashes.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RestServlet.useStackTraceHashes"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>true</jk>
	 * </ul>
	 *
	 * <p>
	 * When enabled, the number of times an exception has occurred will be determined based on stack trace hashsums,
	 * made available through the {@link RestException#getOccurrence()} method.
	 *
	 * <p>
	 * Applicable to servlet class only.
	 */
	public static final String REST_useStackTraceHashes = "RestServlet.useStackTraceHashes";

	/**
	 * <b>Configuration property:</b>  Default character encoding.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RestServlet.defaultCharset"</js>
	 * 	<li><b>Data type:</b> <code>String</code>
	 * 	<li><b>Default:</b> <js>"utf-8"</js>
	 * </ul>
	 *
	 * <p>
	 * The default character encoding for the request and response if not specified on the request.
	 *
	 * <p>
	 * Applicable to servlet class and methods.
	 */
	public static final String REST_defaultCharset = "RestServlet.defaultCharset";

	/**
	 * <b>Configuration property:</b>  Expected format of request parameters.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RestServlet.paramFormat"</js>
	 * 	<li><b>Data type:</b> <code>String</code>
	 * 	<li><b>Default:</b> <js>"UON"</js>
	 * </ul>
	 * <p>
	 * Possible values:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		<js>"UON"</js> - URL-Encoded Object Notation.
	 * 		<br>This notation allows for request parameters to contain arbitrarily complex POJOs.
	 * 	<li>
	 * 		<js>"PLAIN"</js> - Plain text.
	 * 		<br>This treats request parameters as plain text.
	 * 		<br>Only POJOs directly convertible from <l>Strings</l> can be represented in parameters when using this
	 * 		mode.
	 * </ul>
	 *
	 * <p>
	 * Note that the parameter value <js>"(foo)"</js> is interpreted as <js>"(foo)"</js> when using plain mode, but
	 * <js>"foo"</js> when using UON mode.
	 *
	 * <p>
	 * The format can also be specified per-parameter using the {@link FormData#format() @FormData.format()} and
	 * {@link Query#format() @Query.format()} annotations.
	 *
	 * <p>
	 * Applicable to servlet class and methods.
	 */
	public static final String REST_paramFormat = "RestServlet.paramFormat";


	//--------------------------------------------------------------------------------
	// Automatically added properties.
	//--------------------------------------------------------------------------------

	/**
	 * The request servlet path.
	 *
	 * <p>
	 * Automatically added to properties returned by {@link SerializerSession#getProperty(String)} and
	 * {@link ParserSession#getProperty(String)}.
	 *
	 * <p>
	 * Equivalent to the value returned by {@link RestRequest#getServletPath()}
	 */
	public static final String REST_servletPath = "RestServlet.servletPath";

	/**
	 * The request servlet URI.
	 *
	 * <p>
	 * Equivalent to the value returned by {@link UriContext#getRootRelativeServletPath()}
	 */
	public static final String REST_servletURI = "RestServlet.servletURI";

	/**
	 * The request URI path info.
	 *
	 * <p>
	 * Automatically added to properties returned by {@link SerializerSession#getProperty(String)} and
	 * {@link ParserSession#getProperty(String)}.
	 *
	 * <p>
	 * Equivalent to the value returned by {@link RestRequest#getPathInfo()}
	 */
	public static final String REST_pathInfo = "RestServlet.pathInfo";

	/**
	 * The request URI.
	 *
	 * <p>
	 * Automatically added to properties returned by {@link SerializerSession#getProperty(String)} and
	 * {@link ParserSession#getProperty(String)}.
	 *
	 * <p>
	 * Equivalent to the value returned by {@link RestRequest#getRequestURI()}
	 */
	public static final String REST_requestURI = "RestServlet.requestURI";

	/**
	 * The request method.
	 *
	 * <p>
	 * Automatically added to properties returned by {@link SerializerSession#getProperty(String)} and
	 * {@link ParserSession#getProperty(String)}.
	 *
	 * <p>
	 * Equivalent to the value returned by {@link RestRequest#getMethod()}
	 */
	public static final String REST_method = "RestServlet.method";

	/**
	 * The localized servlet title.
	 *
	 * <p>
	 * Automatically added to properties returned by {@link SerializerSession#getProperty(String)} and
	 * {@link ParserSession#getProperty(String)}.
	 *
	 * <p>
	 * Equivalent to the value returned by {@link RestRequest#getServletTitle()}
	 */
	public static final String REST_servletTitle = "RestServlet.servletTitle";

	/**
	 * The localized servlet description.
	 *
	 * <p>
	 * Automatically added to properties returned by {@link SerializerSession#getProperty(String)} and
	 * {@link ParserSession#getProperty(String)}.
	 *
	 * <p>
	 * Equivalent to the value returned by {@link RestRequest#getServletDescription()}
	 */
	public static final String REST_servletDescription = "RestServlet.servletDescription";

	/**
	 * The localized method summary.
	 *
	 * <p>
	 * Automatically added to properties returned by {@link SerializerSession#getProperty(String)} and
	 * {@link ParserSession#getProperty(String)}.
	 *
	 * <p>
	 * Equivalent to the value returned by {@link RestRequest#getMethodSummary()}
	 */
	public static final String REST_methodSummary = "RestServlet.methodSummary";

	/**
	 * The localized method description.
	 *
	 * <p>
	 * Automatically added to properties returned by {@link SerializerSession#getProperty(String)} and
	 * {@link ParserSession#getProperty(String)}.
	 *
	 * <p>
	 * Equivalent to the value returned by {@link RestRequest#getMethodDescription()}
	 */
	public static final String REST_methodDescription = "RestServlet.methodDescription";


	private final Object resource;
	private final RestConfig config;
	private final boolean
		allowHeaderParams,
		allowBodyParam,
		renderResponseStackTraces,
		useStackTraceHashes;
	private final String
		defaultCharset,
		paramFormat,
		clientVersionHeader,
		fullPath,
		htmlTitle,
		htmlDescription,
		htmlBranding,
		htmlHeader,
		htmlLinks,
		htmlNav,
		htmlAside,
		htmlCss,
		htmlCssUrl,
		htmlFooter,
		htmlNoResultsMessage;
	private final boolean htmlNoWrap;
	private final HtmlDocTemplate htmlTemplate;
	private final Map<String,Widget> widgets;

	private final Set<String> allowMethodParams;

	private final ObjectMap properties;
	private final Class<?>[]
		beanFilters,
		pojoSwaps;
	private final Map<Class<?>,RestParam> paramResolvers;
	private final SerializerGroup serializers;
	private final ParserGroup parsers;
	private final UrlEncodingSerializer urlEncodingSerializer;
	private final UrlEncodingParser urlEncodingParser;
	private final EncoderGroup encoders;
	private final MediaType[]
		supportedContentTypes,
		supportedAcceptTypes;
	private final Map<String,String> defaultRequestHeaders;
	private final Map<String,Object> defaultResponseHeaders;
	private final BeanContext beanContext;
	private final RestConverter[] converters;
	private final RestGuard[] guards;
	private final ResponseHandler[] responseHandlers;
	private final MimetypesFileTypeMap mimetypesFileTypeMap;
	private final StreamResource styleSheet, favIcon;
	private final Map<String,String> staticFilesMap;
	private final String[] staticFilesPrefixes;
	private final MessageBundle msgs;
	private final ConfigFile configFile;
	private final VarResolver varResolver;
	private final Map<String,CallRouter> callRouters;
	private final Map<String,CallMethod> callMethods;
	private final Map<String,RestContext> childResources;
	private final RestLogger logger;
	private final RestCallHandler callHandler;
	private final RestInfoProvider infoProvider;
	private final RestException initException;
	private final RestContext parentContext;

	// In-memory cache of images and stylesheets in the org.apache.juneau.rest.htdocs package.
	private final Map<String,StreamResource> staticFilesCache = new ConcurrentHashMap<String,StreamResource>();
	private final Map<String,byte[]> resourceStreams = new ConcurrentHashMap<String,byte[]>();
	private final Map<String,String> resourceStrings = new ConcurrentHashMap<String,String>();
	private final ConcurrentHashMap<Integer,AtomicInteger> stackTraceHashes = new ConcurrentHashMap<Integer,AtomicInteger>();


	/**
	 * Constructor.
	 *
	 * @param resource The resource class (a class annotated with {@link RestResource @RestResource}).
	 * @param config The servlet configuration object.
	 * @throws Exception If any initialization problems were encountered.
	 */
	@SuppressWarnings("unchecked")
	public RestContext(Object resource, RestConfig config) throws Exception {
		super(null);
		RestException _initException = null;
		try {
			this.resource = resource;
			this.config = config;
			this.parentContext = config.parentContext;

			Builder b = new Builder(resource, config);
			this.allowHeaderParams = b.allowHeaderParams;
			this.allowBodyParam = b.allowBodyParam;
			this.renderResponseStackTraces = b.renderResponseStackTraces;
			this.useStackTraceHashes = b.useStackTraceHashes;
			this.allowMethodParams = Collections.unmodifiableSet(b.allowMethodParams);
			this.defaultCharset = b.defaultCharset;
			this.paramFormat = b.paramFormat;
			this.varResolver = b.varResolver;
			this.configFile = b.configFile;
			this.properties = b.properties;
			this.beanFilters = b.beanFilters;
			this.pojoSwaps = b.pojoSwaps;
			this.paramResolvers = Collections.unmodifiableMap(b.paramResolvers);
			this.serializers = b.serializers;
			this.parsers = b.parsers;
			this.urlEncodingSerializer = b.urlEncodingSerializer;
			this.urlEncodingParser = b.urlEncodingParser;
			this.encoders = b.encoders;
			this.supportedContentTypes = toObjectArray(b.supportedContentTypes, MediaType.class);
			this.supportedAcceptTypes = toObjectArray(b.supportedAcceptTypes, MediaType.class);
			this.clientVersionHeader = b.clientVersionHeader;
			this.defaultRequestHeaders = Collections.unmodifiableMap(b.defaultRequestHeaders);
			this.defaultResponseHeaders = Collections.unmodifiableMap(b.defaultResponseHeaders);
			this.beanContext = b.beanContext;
			this.converters = b.converters.toArray(new RestConverter[b.converters.size()]);
			this.guards = b.guards.toArray(new RestGuard[b.guards.size()]);
			this.responseHandlers = toObjectArray(b.responseHandlers, ResponseHandler.class);
			this.mimetypesFileTypeMap = b.mimetypesFileTypeMap;
			this.styleSheet = b.styleSheet;
			this.favIcon = b.favIcon;
			this.staticFilesMap = Collections.unmodifiableMap(b.staticFilesMap);
			this.staticFilesPrefixes = b.staticFilesPrefixes;
			this.msgs = b.messageBundle;
			this.childResources = Collections.synchronizedMap(new LinkedHashMap<String,RestContext>());  // Not unmodifiable on purpose so that children can be replaced.
			this.logger = b.logger;
			this.fullPath = b.fullPath;
			this.widgets = Collections.unmodifiableMap(b.widgets);

			this.htmlTitle = b.htmlTitle;
			this.htmlDescription = b.htmlDescription;
			this.htmlBranding = b.htmlBranding;
			this.htmlHeader = b.htmlHeader;
			this.htmlLinks = b.htmlLinks;
			this.htmlNav = b.htmlNav;
			this.htmlAside = b.htmlAside;
			this.htmlCss = b.htmlCss;
			this.htmlCssUrl = b.htmlCssUrl;
			this.htmlFooter = b.htmlFooter;
			this.htmlNoWrap = b.htmlNoWrap;
			this.htmlNoResultsMessage = b.htmlNoResultsMessage;
			this.htmlTemplate = b.htmlTemplate;

			//----------------------------------------------------------------------------------------------------
			// Initialize the child resources.
			// Done after initializing fields above since we pass this object to the child resources.
			//----------------------------------------------------------------------------------------------------
			List<String> methodsFound = new LinkedList<String>();   // Temporary to help debug transient duplicate method issue.
			Map<String,CallRouter.Builder> routers = new LinkedHashMap<String,CallRouter.Builder>();
			Map<String,CallMethod> _javaRestMethods = new LinkedHashMap<String,CallMethod>();
			for (java.lang.reflect.Method method : resource.getClass().getMethods()) {
				if (method.isAnnotationPresent(RestMethod.class)) {
					RestMethod a = method.getAnnotation(RestMethod.class);
					methodsFound.add(method.getName() + "," + a.name() + "," + a.path());
					try {
						if (! Modifier.isPublic(method.getModifiers()))
							throw new RestServletException("@RestMethod method {0}.{1} must be defined as public.", this.getClass().getName(), method.getName());

						CallMethod sm = new CallMethod(resource, method, this);
						String httpMethod = sm.getHttpMethod();

						// PROXY is a special case where a method returns an interface that we
						// can perform REST calls against.
						// We override the CallMethod.invoke() method to insert our logic.
						if ("PROXY".equals(httpMethod)) {

							final ClassMeta<?> interfaceClass = beanContext.getClassMeta(method.getGenericReturnType());
							final Map<String,Method> remoteableMethods = interfaceClass.getRemoteableMethods();
							if (remoteableMethods.isEmpty())
								throw new RestException(SC_INTERNAL_SERVER_ERROR, "Method {0} returns an interface {1} that doesn't define any remoteable methods.", getMethodSignature(method), interfaceClass.getReadableName());

							sm = new CallMethod(resource, method, this) {

								@Override
								int invoke(String pathInfo, RestRequest req, RestResponse res) throws RestException {

									int rc = super.invoke(pathInfo, req, res);
									if (rc != SC_OK)
										return rc;

									final Object o = res.getOutput();

									if ("GET".equals(req.getMethod())) {
										res.setOutput(getMethodInfo(remoteableMethods.values()));
										return SC_OK;

									} else if ("POST".equals(req.getMethod())) {
										if (pathInfo.indexOf('/') != -1)
											pathInfo = pathInfo.substring(pathInfo.lastIndexOf('/')+1);
										pathInfo = urlDecode(pathInfo);
										java.lang.reflect.Method m = remoteableMethods.get(pathInfo);
										if (m != null) {
											try {
												// Parse the args and invoke the method.
												Parser p = req.getBody().getParser();
												Object input = p.isReaderParser() ? req.getReader() : req.getInputStream();
												Object output = m.invoke(o, p.parseArgs(input, m.getGenericParameterTypes()));
												res.setOutput(output);
												return SC_OK;
											} catch (Exception e) {
												throw new RestException(SC_INTERNAL_SERVER_ERROR, e);
											}
										}
									}
									return SC_NOT_FOUND;
								}
							};

							_javaRestMethods.put(method.getName(), sm);
							addToRouter(routers, "GET", sm);
							addToRouter(routers, "POST", sm);

						} else {
							_javaRestMethods.put(method.getName(), sm);
							addToRouter(routers, httpMethod, sm);
						}
					} catch (RestServletException e) {
						throw new RestServletException("Problem occurred trying to serialize methods on class {0}, methods={1}", this.getClass().getName(), JsonSerializer.DEFAULT_LAX.serialize(methodsFound)).initCause(e);
					}
				}
			}
			this.callMethods = Collections.unmodifiableMap(_javaRestMethods);

			Map<String,CallRouter> _callRouters = new LinkedHashMap<String,CallRouter>();
			for (CallRouter.Builder crb : routers.values())
				_callRouters.put(crb.getHttpMethodName(), crb.build());
			this.callRouters = Collections.unmodifiableMap(_callRouters);

			// Initialize our child resources.
			RestResourceResolver rrr = resolve(RestResourceResolver.class, config.resourceResolver);
			for (Object o : config.childResources) {
				String path = null;
				Object r = null;
				if (o instanceof Pair) {
					Pair<String,Object> p = (Pair<String,Object>)o;
					path = p.first();
					r = p.second();
				} else if (o instanceof Class<?>) {
					Class<?> c = (Class<?>)o;
					r = c;
				} else {
					r = o;
				}

				RestConfig childConfig = null;

				if (o instanceof Class) {
					Class<?> oc = (Class<?>)o;
					childConfig = new RestConfig(config.inner, oc, this);
					r = rrr.resolve(oc, childConfig);
				} else {
					r = o;
					childConfig = new RestConfig(config.inner, o.getClass(), this);
				}

				if (r instanceof RestServlet) {
					RestServlet rs = (RestServlet)r;
					rs.init(childConfig);
					if (rs.getContext() == null)
						throw new RestException(SC_INTERNAL_SERVER_ERROR, "Servlet {0} not initialized.  init(RestConfig) was not called.  This can occur if you've overridden this method but didn't call super.init(RestConfig).", rs.getClass().getName());
					path = childConfig.path;
					childResources.put(path, rs.getContext());
				} else {

					// Call the init(RestConfig) method.
					java.lang.reflect.Method m2 = findPublicMethod(r.getClass(), "init", Void.class, RestConfig.class);
					if (m2 != null)
						m2.invoke(r, childConfig);

					RestContext rc2 = new RestContext(r, childConfig);

					// Call the init(RestContext) method.
					m2 = findPublicMethod(r.getClass(), "init", Void.class, RestContext.class);
					if (m2 != null)
						m2.invoke(r, rc2);

					path = childConfig.path;
					childResources.put(path, rc2);
				}
			}

			callHandler = config.callHandler == null ? new RestCallHandler(this) : resolve(RestCallHandler.class, config.callHandler, this);
			infoProvider = config.infoProvider == null ? new RestInfoProvider(this) : resolve(RestInfoProvider.class, config.infoProvider, this);

		} catch (RestException e) {
			_initException = e;
			throw e;
		} catch (Exception e) {
			_initException = new RestException(SC_INTERNAL_SERVER_ERROR, e);
			throw e;
		} finally {
			initException = _initException;
		}
	}

	private static void addToRouter(Map<String, CallRouter.Builder> routers, String httpMethodName, CallMethod cm) throws RestServletException {
		if (! routers.containsKey(httpMethodName))
			routers.put(httpMethodName, new CallRouter.Builder(httpMethodName));
		routers.get(httpMethodName).add(cm);
	}

	private static class Builder {

		boolean allowHeaderParams, allowBodyParam, renderResponseStackTraces, useStackTraceHashes;
		VarResolver varResolver;
		ConfigFile configFile;
		ObjectMap properties;
		Class<?>[] beanFilters;
		Class<?>[] pojoSwaps;
		Map<Class<?>,RestParam> paramResolvers = new HashMap<Class<?>,RestParam>();
		SerializerGroup serializers;
		ParserGroup parsers;
		UrlEncodingSerializer urlEncodingSerializer;
		UrlEncodingParser urlEncodingParser;
		EncoderGroup encoders;
		String clientVersionHeader = "", defaultCharset, paramFormat, htmlTitle, htmlDescription, htmlBranding,
			htmlHeader, htmlLinks, htmlNav, htmlAside, htmlCss, htmlCssUrl, htmlFooter, htmlNoResultsMessage;
		boolean htmlNoWrap;
		HtmlDocTemplate htmlTemplate;

		List<MediaType> supportedContentTypes, supportedAcceptTypes;
		Map<String,String> defaultRequestHeaders = new TreeMap<String,String>(String.CASE_INSENSITIVE_ORDER);
		Map<String,Object> defaultResponseHeaders;
		BeanContext beanContext;
		List<RestConverter> converters = new ArrayList<RestConverter>();
		List<RestGuard> guards = new ArrayList<RestGuard>();
		List<ResponseHandler> responseHandlers = new ArrayList<ResponseHandler>();
		MimetypesFileTypeMap mimetypesFileTypeMap;
		StreamResource styleSheet, favIcon;
		Map<String,String> staticFilesMap;
		String[] staticFilesPrefixes;
		MessageBundle messageBundle;
		Set<String> allowMethodParams = new LinkedHashSet<String>();
		RestLogger logger;
		String fullPath;
		Map<String,Widget> widgets;

		@SuppressWarnings("unchecked")
		private Builder(Object resource, RestConfig sc) throws Exception {

			PropertyStore ps = sc.createPropertyStore();

			LinkedHashMap<Class<?>,RestResource> restResourceAnnotationsChildFirst = findAnnotationsMap(RestResource.class, resource.getClass());

			allowHeaderParams = ps.getProperty(REST_allowHeaderParams, boolean.class, true);
			allowBodyParam = ps.getProperty(REST_allowBodyParam, boolean.class, true);
			renderResponseStackTraces = ps.getProperty(REST_renderResponseStackTraces, boolean.class, false);
			useStackTraceHashes = ps.getProperty(REST_useStackTraceHashes, boolean.class, true);
			defaultCharset = ps.getProperty(REST_defaultCharset, String.class, "utf-8");
			paramFormat = ps.getProperty(REST_paramFormat, String.class, "");

			for (String m : split(ps.getProperty(REST_allowMethodParam, String.class, "")))
				if (m.equals("true"))  // For backwards compatibility when this was a boolean field.
					allowMethodParams.add("*");
				else
					allowMethodParams.add(m.toUpperCase());

			varResolver = sc.varResolverBuilder
				.vars(LocalizationVar.class, RequestVar.class, SerializedRequestAttrVar.class, ServletInitParamVar.class, UrlVar.class, UrlEncodeVar.class, WidgetVar.class)
				.build()
			;
			configFile = sc.configFile.getResolving(this.varResolver);
			properties = sc.properties;
			Collections.reverse(sc.beanFilters);
			Collections.reverse(sc.pojoSwaps);
			beanFilters = toObjectArray(sc.beanFilters, Class.class);
			pojoSwaps = toObjectArray(sc.pojoSwaps, Class.class);

			for (Class<?> c : sc.paramResolvers) {
				RestParam rp = newInstance(RestParam.class, c);
				paramResolvers.put(rp.forClass(), rp);
			}

			clientVersionHeader = sc.clientVersionHeader;

			// Find resource resource bundle location.
			for (Map.Entry<Class<?>,RestResource> e : restResourceAnnotationsChildFirst.entrySet()) {
				Class<?> c = e.getKey();
				RestResource r = e.getValue();
				if (! r.messages().isEmpty()) {
					if (messageBundle == null)
						messageBundle = new MessageBundle(c, r.messages());
					else
						messageBundle.addSearchPath(c, r.messages());
				}
			}

			if (messageBundle == null)
				messageBundle = new MessageBundle(resource.getClass(), "");

			ps.addBeanFilters(beanFilters).addPojoSwaps(pojoSwaps).setProperties(properties);

			serializers = sc.serializers.beanFilters(beanFilters).pojoSwaps(pojoSwaps).properties(properties).listener(sc.serializerListener).build();
			parsers = sc.parsers.beanFilters(beanFilters).pojoSwaps(pojoSwaps).properties(properties).listener(sc.parserListener).build();
			urlEncodingSerializer = new UrlEncodingSerializer(ps);
			urlEncodingParser = new UrlEncodingParser(ps);
			encoders = sc.encoders.build();
			supportedContentTypes = sc.supportedContentTypes != null ? sc.supportedContentTypes : serializers.getSupportedMediaTypes();
			supportedAcceptTypes = sc.supportedAcceptTypes != null ? sc.supportedAcceptTypes : parsers.getSupportedMediaTypes();
			defaultRequestHeaders.putAll(sc.defaultRequestHeaders);
			defaultResponseHeaders = Collections.unmodifiableMap(new LinkedHashMap<String,Object>(sc.defaultResponseHeaders));
			beanContext = ps.getBeanContext();

			for (Object o : sc.converters)
				converters.add(resolve(RestConverter.class, o));

			for (Object o : sc.guards)
				guards.add(resolve(RestGuard.class, o));

			for (Object o : sc.responseHandlers)
				responseHandlers.add(resolve(ResponseHandler.class, o));

			mimetypesFileTypeMap = sc.mimeTypes;

			VarResolver vr = sc.getVarResolverBuilder().build();

			if (sc.styleSheets != null) {
				List<InputStream> contents = new ArrayList<InputStream>();
				for (Object o : sc.styleSheets) {
					if (o instanceof Pair) {
						Pair<Class<?>,String> p = (Pair<Class<?>,String>)o;
						for (String path : split(vr.resolve(StringUtils.toString(p.second()))))
							if (path.startsWith("file://"))
								contents.add(new FileInputStream(path));
							else
								contents.add(ReflectionUtils.getResource(p.first(), path));
					} else {
						contents.add(toInputStream(o));
					}
				}
				styleSheet = new StreamResource(MediaType.forString("text/css"), contents.toArray());
			}

			if (sc.favIcon != null) {
				Object o = sc.favIcon;
				InputStream is = null;
				if (o instanceof Pair) {
					Pair<Class<?>,String> p = (Pair<Class<?>,String>)o;
					is = ReflectionUtils.getResource(p.first(), vr.resolve(p.second()));
				} else {
					is = toInputStream(o);
				}
				if (is != null)
					favIcon = new StreamResource(MediaType.forString("image/x-icon"), is);
			}

			staticFilesMap = new LinkedHashMap<String,String>();
			if (sc.staticFiles != null) {
				for (Object o : sc.staticFiles) {
					if (o instanceof Pair) {
						Pair<Class<?>,String> p = (Pair<Class<?>,String>)o;
						// TODO - Currently doesn't take parent class location into account.
						staticFilesMap.putAll(JsonParser.DEFAULT.parse(vr.resolve(p.second()), LinkedHashMap.class));
					} else {
						throw new RuntimeException("TODO");
					}
				}
			}
			staticFilesPrefixes = staticFilesMap.keySet().toArray(new String[0]);

			logger = sc.logger == null ? new RestLogger.NoOp() : resolve(RestLogger.class, sc.logger);

			fullPath = (sc.parentContext == null ? "" : (sc.parentContext.fullPath + '/')) + sc.path;

			widgets = sc.widgets;

			htmlTitle = sc.htmlTitle;
			htmlDescription = sc.htmlDescription;
			htmlBranding = sc.htmlBranding;
			htmlHeader = sc.htmlHeader;
			htmlLinks = sc.htmlLinks;
			htmlNav = sc.htmlNav;
			htmlAside = sc.htmlAside;
			htmlCss = sc.htmlCss;
			htmlCssUrl = sc.htmlCssUrl;
			htmlFooter = sc.htmlFooter;
			htmlNoWrap = sc.htmlNoWrap;
			htmlNoResultsMessage = sc.htmlNoResultsMessage;
			htmlTemplate = ClassUtils.newInstance(HtmlDocTemplate.class, sc.htmlTemplate);
		}
	}

	/**
	 * Returns the variable resolver for this servlet.
	 *
	 * <p>
	 * Variable resolvers are used to replace variables in property values.
	 *
	 * <h6 class='figure'>Example:</h6>
	 * <p class='bcode'>
	 * 	<ja>@RestResource</ja>(
	 * 		messages=<js>"nls/Messages"</js>,
	 * 		properties={
	 * 			<ja>@Property</ja>(name=<js>"title"</js>,value=<js>"$L{title}"</js>),  <jc>// Localized variable in Messages.properties</jc>
	 * 			<ja>@Property</ja>(name=<js>"javaVendor"</js>,value=<js>"$S{java.vendor,Oracle}"</js>),  <jc>// System property with default value</jc>
	 * 			<ja>@Property</ja>(name=<js>"foo"</js>,value=<js>"bar"</js>),
	 * 			<ja>@Property</ja>(name=<js>"bar"</js>,value=<js>"baz"</js>),
	 * 			<ja>@Property</ja>(name=<js>"v1"</js>,value=<js>"$R{foo}"</js>),  <jc>// Request variable. value="bar"</jc>
	 * 			<ja>@Property</ja>(name=<js>"v2"</js>,value=<js>"$R{$R{foo}}"</js>)  <jc>// Nested request variable. value="baz"</jc>
	 * 		}
	 * 	)
	 * 	<jk>public class</jk> MyRestResource <jk>extends</jk> RestServletDefault {
	 * </p>
	 *
	 * <p>
	 * A typical usage pattern is using variables for resolving URL links when rendering HTML:
	 * <p class='bcode'>
	 * 	<ja>@RestMethod</ja>(
	 * 		name=<js>"GET"</js>, path=<js>"/{name}/*"</js>,
	 * 		properties={
	 * 			<ja>@Property</ja>(
	 * 				name=<jsf>HTMLDOC_links</jsf>,
	 * 				value=<js>"{up:'$R{requestParentURI}', options:'servlet:/?method=OPTIONS', editLevel:'servlet:/editLevel?logger=$R{attribute.name}'}"</js>
	 * 			)
	 * 		}
	 * 	)
	 * 	<jk>public</jk> LoggerEntry getLogger(RestRequest req, <ja>@Path</ja> String name) <jk>throws</jk> Exception {
	 * </p>
	 *
	 * <p>
	 * Calls to <code>req.getProperties().getString(<js>"key"</js>)</code> returns strings with variables resolved.
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
	 * <ul>
	 * 	<li>{@link RestResource#config() @RestResource.config()} annotation.
	 * 	<li>{@link RestConfig#setConfigFile(ConfigFile)} method.
	 * </ul>
	 *
	 * @return The resolving config file associated with this servlet.  Never <jk>null</jk>.
	 */
	public ConfigFile getConfigFile() {
		return configFile;
	}

	/**
	 * Resolve a static resource file.
	 *
	 * <p>
	 * The location of static resources are defined via one of the following:
	 * <ul>
	 * 	<li>{@link RestResource#staticFiles() @RestResource.staticFiles()} annotation.
	 * 	<li>{@link RestConfig#addStaticFiles(Class, String)} method.
	 * </ul>
	 *
	 * @param pathInfo The unencoded path info.
	 * @return The resource, or <jk>null</jk> if the resource could not be resolved.
	 * @throws IOException
	 */
	public StreamResource resolveStaticFile(String pathInfo) throws IOException {
		if (! staticFilesCache.containsKey(pathInfo)) {
			String p = urlDecode(trimSlashes(pathInfo));
			if (p.indexOf("..") != -1)
				throw new RestException(SC_NOT_FOUND, "Invalid path");
			for (Map.Entry<String,String> e : staticFilesMap.entrySet()) {
				String key = trimSlashes(e.getKey());
				if (p.startsWith(key)) {
					String remainder = (p.equals(key) ? "" : p.substring(key.length()));
					if (remainder.isEmpty() || remainder.startsWith("/")) {
						String p2 = trimSlashes(e.getValue()) + remainder;
						InputStream is = getResource(p2, null);
						if (is != null) {
							try {
								int i = p2.lastIndexOf('/');
								String name = (i == -1 ? p2 : p2.substring(i+1));
								String mediaType = mimetypesFileTypeMap.getContentType(name);
								ObjectMap headers = new ObjectMap().append("Cache-Control", "max-age=86400, public");
								staticFilesCache.put(pathInfo, new StreamResource(MediaType.forString(mediaType), headers, is));
								return staticFilesCache.get(pathInfo);
							} finally {
								is.close();
							}
						}
					}
				}
			}
		}
		return staticFilesCache.get(pathInfo);
	}

	/**
	 * Same as {@link Class#getResourceAsStream(String)} except if it doesn't find the resource on this class, searches
	 * up the parent hierarchy chain.
	 *
	 * <p>
	 * If the resource cannot be found in the classpath, then an attempt is made to look in the JVM working directory.
	 *
	 * <p>
	 * If the <code>locale</code> is specified, then we look for resources whose name matches that locale.
	 * For example, if looking for the resource <js>"MyResource.txt"</js> for the Japanese locale, we will look for
	 * files in the following order:
	 * <ol>
	 * 	<li><js>"MyResource_ja_JP.txt"</js>
	 * 	<li><js>"MyResource_ja.txt"</js>
	 * 	<li><js>"MyResource.txt"</js>
	 * </ol>
	 *
	 * @param name The resource name.
	 * @param locale Optional locale.
	 * @return An input stream of the resource, or <jk>null</jk> if the resource could not be found.
	 * @throws IOException
	 */
	protected InputStream getResource(String name, Locale locale) throws IOException {
		String n = (locale == null || locale.toString().isEmpty() ? name : name + '|' + locale);
		if (! resourceStreams.containsKey(n)) {
			InputStream is = getLocalizedResource(resource.getClass(), name, locale);
			if (is == null && name.indexOf("..") == -1) {
				for (String n2 : getCandidateFileNames(name, locale)) {
					File f = new File(n2);
					if (f.exists() && f.canRead()) {
						is = new FileInputStream(f);
						break;
					}
				}
			}
			if (is != null) {
				try {
					resourceStreams.put(n, ByteArrayCache.DEFAULT.cache(is));
				} finally {
					is.close();
				}
			}
		}
		byte[] b = resourceStreams.get(n);
		return b == null ? null : new ByteArrayInputStream(b);
	}

	/**
	 * Reads the input stream from {@link #getResource(String, Locale)} into a String.
	 *
	 * @param name The resource name.
	 * @param locale Optional locale.
	 * @return The contents of the stream as a string, or <jk>null</jk> if the resource could not be found.
	 * @throws IOException If resource could not be found.
	 */
	public String getResourceAsString(String name, Locale locale) throws IOException {
		String n = (locale == null || locale.toString().isEmpty() ? name : name + '|' + locale);
		if (! resourceStrings.containsKey(n)) {
			String s = read(getResource(name, locale));
			if (s == null)
				throw new IOException("Resource '"+name+"' not found.");
			resourceStrings.put(n, s);
		}
		return resourceStrings.get(n);
	}

	/**
	 * Reads the input stream from {@link #getResource(String, Locale)} and parses it into a POJO using the parser
	 * matched by the specified media type.
	 *
	 * <p>
	 * Useful if you want to load predefined POJOs from JSON files in your classpath.
	 *
	 * @param c The class type of the POJO to create.
	 * @param mediaType The media type of the data in the stream (e.g. <js>"text/json"</js>)
	 * @param name The resource name (e.g. "htdocs/styles.css").
	 * @param locale Optional locale.
	 * @return The parsed resource, or <jk>null</jk> if the resource could not be found.
	 * @throws IOException
	 * @throws ServletException If the media type was unknown or the input could not be parsed into a POJO.
	 */
	public <T> T getResource(Class<T> c, MediaType mediaType, String name, Locale locale) throws IOException, ServletException {
		InputStream is = getResource(name, locale);
		if (is == null)
			return null;
		try {
			Parser p = parsers.getParser(mediaType);
			if (p != null) {
				try {
					if (p.isReaderParser())
						return p.parse(new InputStreamReader(is, UTF8), c);
					return p.parse(is, c);
				} catch (ParseException e) {
					throw new ServletException("Could not parse resource '' as media type '"+mediaType+"'.");
				}
			}
			throw new ServletException("Unknown media type '"+mediaType+"'");
		} catch (Exception e) {
			throw new ServletException("Could not parse resource with name '"+name+"'", e);
		}
	}

	/**
	 * Returns the path for this resource as defined by the {@link RestResource#path()} annotation or
	 * {@link RestConfig#setPath(String)} method concatenated with those on all parent classes.
	 *
	 * <p>
	 * If path is not specified, returns <js>"/"</js>.
	 *
	 * <p>
	 * Path always starts with <js>"/"</js>.
	 *
	 * @return The servlet path.
	 */
	public String getPath() {
		return fullPath;
	}

	/**
	 * The HTML page title.
	 *
	 * <p>
	 * Defined by the {@link HtmlDoc#title()} annotation or {@link RestConfig#setHtmlTitle(String)} method.
	 *
	 * @return The HTML page title.
	 */
	public String getHtmlTitle() {
		return htmlTitle;
	}

	/**
	 * The HTML page description.
	 *
	 * <p>
	 * Defined by the {@link HtmlDoc#description()} annotation or {@link RestConfig#setHtmlDescription(String)} method.
	 *
	 * @return The HTML page description.
	 */
	public String getHtmlDescription() {
		return htmlDescription;
	}

	/**
	 * The HTML page branding.
	 *
	 * <p>
	 * Defined by the {@link HtmlDoc#branding()} annotation or {@link RestConfig#setHtmlBranding(String)} method.
	 *
	 * @return The HTML page description.
	 */
	public String getHtmlBranding() {
		return htmlBranding;
	}

	/**
	 * The HTML page header contents.
	 *
	 * <p>
	 * Defined by the {@link HtmlDoc#header()} annotation or {@link RestConfig#setHtmlHeader(String)} method.
	 *
	 * @return The HTML page header contents.
	 */
	public String getHtmlHeader() {
		return htmlHeader;
	}

	/**
	 * The HTML page nav section links.
	 *
	 * <p>
	 * Defined by the {@link HtmlDoc#links()} annotation or {@link RestConfig#setHtmlLinks(String)} method.
	 *
	 * @return The HTML page nav section links.
	 */
	public String getHtmlLinks() {
		return htmlLinks;
	}

	/**
	 * The HTML page nav section contents.
	 *
	 * <p>
	 * Defined by the {@link HtmlDoc#nav()} annotation or {@link RestConfig#setHtmlNav(String)} method.
	 *
	 * @return The HTML page nav section contents.
	 */
	public String getHtmlNav() {
		return htmlNav;
	}

	/**
	 * The HTML page aside section contents.
	 *
	 * <p>
	 * Defined by the {@link HtmlDoc#aside()} annotation or {@link RestConfig#setHtmlAside(String)} method.
	 *
	 * @return The HTML page aside section contents.
	 */
	public String getHtmlAside() {
		return htmlAside;
	}

	/**
	 * The HTML page footer section contents.
	 *
	 * <p>
	 * Defined by the {@link HtmlDoc#footer()} annotation or {@link RestConfig#setHtmlFooter(String)} method.
	 *
	 * @return The HTML page footer section contents.
	 */
	public String getHtmlFooter() {
		return htmlFooter;
	}

	/**
	 * The HTML page CSS URL.
	 *
	 * <p>
	 * Defined by the {@link HtmlDoc#cssUrl()} annotation or {@link RestConfig#setHtmlCssUrl(String)} method.
	 *
	 * @return The HTML page CSS URL.
	 */
	public String getHtmlCssUrl() {
		return htmlCssUrl;
	}

	/**
	 * The HTML page CSS contents.
	 *
	 * <p>
	 * Defined by the {@link HtmlDoc#css()} annotation or {@link RestConfig#setHtmlCss(String)} method.
	 *
	 * @return The HTML page CSS contents.
	 */
	public String getHtmlCss() {
		return htmlCss;
	}

	/**
	 * The HTML page nowrap setting.
	 *
	 * <p>
	 * Defined by the {@link HtmlDoc#nowrap()} annotation or {@link RestConfig#setHtmlNoWrap(boolean)} method.
	 *
	 * @return The HTML page nowrap setting.
	 */
	public boolean getHtmlNoWrap() {
		return htmlNoWrap;
	}

	/**
	 * The HTML page template.
	 *
	 * <p>
	 * Defined by the {@link HtmlDoc#template()} annotation or {@link RestConfig#setHtmlTemplate(Class)} method.
	 *
	 * @return The HTML page template.
	 */
	public HtmlDocTemplate getHtmlTemplate() {
		return htmlTemplate;
	}

	/**
	 * The HTML page no-results message.
	 *
	 * <p>
	 * Defined by the {@link HtmlDoc#noResultsMessage()} annotation or {@link RestConfig#setHtmlNoResultsMessage(String)}
	 * method.
	 *
	 * @return The HTML page no-results message.
	 */
	public String getHtmlNoResultsMessage() {
		return htmlNoResultsMessage;
	}

	/**
	 * The widgets used for resolving <js>"$W{...}"<js> variables.
	 *
	 * <p>
	 * Defined by the {@link RestResource#widgets()} annotation or {@link RestConfig#addWidget(Class)} method.
	 *
	 * @return The var resolver widgets as a map with keys being the name returned by {@link Widget#getName()}.
	 */
	public Map<String,Widget> getWidgets() {
		return widgets;
	}

	/**
	 * Returns the logger to use for this resource.
	 *
	 * <p>
	 * The logger for a resource is defined via one of the following:
	 * <ul>
	 * 	<li>{@link RestResource#logger() @RestResource.logger()} annotation.
	 * 	<li>{@link RestConfig#setLogger(Class)}/{@link RestConfig#setLogger(RestLogger)} methods.
	 * </ul>
	 *
	 * @return The logger to use for this resource.  Never <jk>null</jk>.
	 */
	public RestLogger getLogger() {
		return logger;
	}

	/**
	 * Returns the resource bundle used by this resource.
	 *
	 * <p>
	 * The resource bundle is defined via one of the following:
	 * <ul>
	 * 	<li>{@link RestResource#messages() @RestResource.messages()} annotation.
	 * </ul>
	 *
	 * @return The resource bundle for this resource.  Never <jk>null</jk>.
	 */
	public MessageBundle getMessages() {
		return msgs;
	}

	/**
	 * Returns the REST information provider used by this resource.
	 *
	 * <p>
	 * The information provider is defined via one of the following:
	 * <ul>
	 * 	<li>{@link RestResource#infoProvider() @RestResource.infoProvider()} annotation.
	 * 	<li>{@link RestConfig#setInfoProvider(Class)}/{@link RestConfig#setInfoProvider(RestInfoProvider)} methods.
	 * </ul>
	 *
	 * @return The information provider for this resource.  Never <jk>null</jk>.
	 */
	public RestInfoProvider getInfoProvider() {
		return infoProvider;
	}

	/**
	 * Returns the REST call handler used by this resource.
	 *
	 * <p>
	 * The call handler is defined via one of the following:
	 * <ul>
	 * 	<li>{@link RestResource#callHandler() @RestResource.callHandler()} annotation.
	 * 	<li>{@link RestConfig#setCallHandler(Class)}/{@link RestConfig#setCallHandler(RestCallHandler)} methods.
	 * </ul>
	 *
	 * @return The call handler for this resource.  Never <jk>null</jk>.
	 */
	protected RestCallHandler getCallHandler() {
		return callHandler;
	}

	/**
	 * Returns a map of HTTP method names to call routers.
	 *
	 * @return A map with HTTP method names upper-cased as the keys, and call routers as the values.
	 */
	protected Map<String,CallRouter> getCallRouters() {
		return callRouters;
	}

	/**
	 * Returns the resource object.
	 *
	 * <p>
	 * This is the instance of the class annotated with the {@link RestResource @RestResource} annotation, usually
	 * an instance of {@link RestServlet}.
	 *
	 * @return The resource object.  Never <jk>null</jk>.
	 */
	public Object getResource() {
		return resource;
	}

	/**
	 * Returns the resource object as a {@link RestServlet}.
	 *
	 * @return
	 * 	The resource object cast to {@link RestServlet}, or <jk>null</jk> if the resource doesn't subclass from
	 * 	{@link RestServlet}
	 */
	public RestServlet getRestServlet() {
		return resource instanceof RestServlet ? (RestServlet)resource : null;
	}

	/**
	 * Throws a {@link RestException} if an exception occurred in the constructor of this object.
	 *
	 * @throws RestException The initialization exception wrapped in a {@link RestException}.
	 */
	protected void checkForInitException() throws RestException {
		if (initException != null)
			throw initException;
	}

	/**
	 * Returns the parent resource context (if this resource was initialized from a parent).
	 *
	 * <p>
	 * From this object, you can get access to the parent resource class itself using {@link #getResource()} or
	 * {@link #getRestServlet()}
	 *
	 * @return The parent resource context, or <jk>null</jk> if there is no parent context.
	 */
	public RestContext getParentContext() {
		return parentContext;
	}

	/**
	 * Returns the {@link BeanContext} object used for parsing path variables and header values.
	 *
	 * @return The bean context used for parsing path variables and header values.
	 */
	public BeanContext getBeanContext() {
		return beanContext;
	}

	/**
	 * Returns the class-level properties associated with this servlet.
	 *
	 * <p>
	 * Properties at the class level are defined via one of the following:
	 * <ul>
	 * 	<li>{@link RestResource#properties() @RestResource.properties()} annotation.
	 * 	<li>{@link RestConfig#setProperty(String, Object)}/{@link RestConfig#setProperties(Map)} methods.
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>The returned {@code Map} is mutable.  Therefore, subclasses are free to override
	 * 	or set additional initialization parameters in their {@code init()} method.
	 * </ul>
	 *
	 * @return The resource properties as an {@link ObjectMap}.
	 */
	public ObjectMap getProperties() {
		return properties;
	}

	/**
	 * Returns the serializers registered with this resource.
	 *
	 * <p>
	 * Serializers at the class level are defined via one of the following:
	 * <ul>
	 * 	<li>{@link RestResource#serializers() @RestResource.serializers()} annotation.
	 * 	<li>{@link RestConfig#addSerializers(Class...)}/{@link RestConfig#addSerializers(Serializer...)} methods.
	 * </ul>
	 *
	 * @return The serializers registered with this resource.
	 */
	public SerializerGroup getSerializers() {
		return serializers;
	}

	/**
	 * Returns the parsers registered with this resource.
	 *
	 * <p>
	 * Parsers at the class level are defined via one of the following:
	 * <ul>
	 * 	<li>{@link RestResource#parsers() @RestResource.parsers()} annotation.
	 * 	<li>{@link RestConfig#addParsers(Class...)}/{@link RestConfig#addParsers(Parser...)} methods.
	 * </ul>
	 *
	 * @return The parsers registered with this resource.
	 */
	public ParserGroup getParsers() {
		return parsers;
	}

	/**
	 * Returns the servlet init parameter returned by {@link ServletConfig#getInitParameter(String)}.
	 *
	 * @param name The init parameter name.
	 * @return The servlet init parameter, or <jk>null</jk> if not found.
	 */
	public String getServletInitParameter(String name) {
		return config.getInitParameter(name);
	}

	/**
	 * Returns the child resources associated with this servlet.
	 *
	 * @return
	 * 	An unmodifiable map of child resources.
	 * 	Keys are the {@link RestResource#path() @RestResource.path()} annotation defined on the child resource.
	 */
	public Map<String,RestContext> getChildResources() {
		return Collections.unmodifiableMap(childResources);
	}

	/**
	 * Returns the number of times this exception was thrown based on a hash of its stacktrace.
	 *
	 * @param e The exception to check.
	 * @return
	 * 	The number of times this exception was thrown, or <code>0</code> if <code>stackTraceHashes</code>
	 * 	setting is not enabled.
	 */
	protected int getStackTraceOccurrence(Throwable e) {
		if (! useStackTraceHashes)
			return 0;
		int h = e.hashCode();
		stackTraceHashes.putIfAbsent(h, new AtomicInteger());
		return stackTraceHashes.get(h).incrementAndGet();
	}

	/**
	 * Returns the value of the {@link #REST_renderResponseStackTraces} setting.
	 *
	 * @return The value of the {@link #REST_renderResponseStackTraces} setting.
	 */
	protected boolean isRenderResponseStackTraces() {
		return renderResponseStackTraces;
	}

	/**
	 * Returns the value of the {@link #REST_allowHeaderParams} setting.
	 *
	 * @return The value of the {@link #REST_allowHeaderParams} setting.
	 */
	protected boolean isAllowHeaderParams() {
		return allowHeaderParams;
	}

	/**
	 * Returns the value of the {@link #REST_allowBodyParam} setting.
	 *
	 * @return The value of the {@link #REST_allowBodyParam} setting.
	 */
	protected boolean isAllowBodyParam() {
		return allowBodyParam;
	}

	/**
	 * Returns the value of the {@link #REST_defaultCharset} setting.
	 *
	 * @return The value of the {@link #REST_defaultCharset} setting.
	 */
	protected String getDefaultCharset() {
		return defaultCharset;
	}

	/**
	 * Returns the value of the {@link #REST_paramFormat} setting.
	 *
	 * @return The value of the {@link #REST_paramFormat} setting.
	 */
	protected String getParamFormat() {
		return paramFormat;
	}

	/**
	 * Returns the name of the client version header name used by this resource.
	 *
	 * <p>
	 * The client version header is the name of the HTTP header on requests that identify a client version.
	 *
	 * <p>
	 * The client version header is defined via one of the following:
	 * <ul>
	 * 	<li>{@link RestResource#clientVersionHeader() @RestResource.clientVersion()} annotation.
	 * </ul>
	 *
	 * @return The name of the client version header used by this resource.  Never <jk>null</jk>.
	 */
	protected String getClientVersionHeader() {
		return clientVersionHeader;
	}

	/**
	 * Returns <jk>true</jk> if the specified <code>Method</code> GET parameter value can be used to override
	 * the method name in the HTTP header.
	 *
	 * @param m The method name, upper-cased.
	 * @return <jk>true</jk> if this resource allows the specified method to be overridden.
	 */
	protected boolean allowMethodParam(String m) {
		return (! isEmpty(m) && (allowMethodParams.contains(m) || allowMethodParams.contains("*")));
	}

	/**
	 * Returns the bean filters associated with this resource.
	 *
	 * <p>
	 * Bean filters at the class level are defined via one of the following:
	 * <ul>
	 * 	<li>{@link RestResource#beanFilters() @RestResource.beanFilters()} annotation.
	 * 	<li>{@link RestConfig#addBeanFilters(Class...)} method.
	 * </ul>
	 *
	 * @return The bean filters associated with this resource.  Never <jk>null</jk>.
	 */
	protected Class<?>[] getBeanFilters() {
		return beanFilters;
	}

	/**
	 * Returns the POJO swaps associated with this resource.
	 *
	 * <p>
	 * POJO swaps at the class level are defined via one of the following:
	 * <ul>
	 * 	<li>{@link RestResource#pojoSwaps() @RestResource.pojoSwaps()} annotation.
	 * 	<li>{@link RestConfig#addPojoSwaps(Class...)} method.
	 * </ul>
	 *
	 * @return The POJO swaps associated with this resource.  Never <jk>null</jk>.
	 */
	protected Class<?>[] getPojoSwaps() {
		return pojoSwaps;
	}

	/**
	 * Finds the {@link RestParam} instances to handle resolving objects on the calls to the specified Java method.
	 *
	 * @param method The Java method being called.
	 * @param methodPlainParams Whether plain-params setting is specified.
	 * @param pathPattern The parsed URL path pattern.
	 * @return The array of resolvers.
	 * @throws ServletException If an annotation usage error was detected.
	 */
	protected RestParam[] findParams(Method method, boolean methodPlainParams, UrlPathPattern pathPattern) throws ServletException {

		Type[] pt = method.getGenericParameterTypes();
		Annotation[][] pa = method.getParameterAnnotations();
		RestParam[] rp = new RestParam[pt.length];
		int attrIndex = 0;

		for (int i = 0; i < pt.length; i++) {

			Type t = pt[i];
			if (t instanceof Class) {
				Class<?> c = (Class<?>)t;
				rp[i] = paramResolvers.get(c);
				if (rp[i] == null)
					rp[i] = RestParamDefaults.STANDARD_RESOLVERS.get(c);
			}

			if (rp[i] == null) {
				for (Annotation a : pa[i]) {
					if (a instanceof Header)
						rp[i] = new RestParamDefaults.HeaderObject((Header)a, t);
					else if (a instanceof FormData)
						rp[i] = new RestParamDefaults.FormDataObject(method, (FormData)a, t, methodPlainParams);
					else if (a instanceof Query)
						rp[i] = new RestParamDefaults.QueryObject(method, (Query)a, t, methodPlainParams);
					else if (a instanceof HasFormData)
						rp[i] = new RestParamDefaults.HasFormDataObject(method, (HasFormData)a, t);
					else if (a instanceof HasQuery)
						rp[i] = new RestParamDefaults.HasQueryObject(method, (HasQuery)a, t);
					else if (a instanceof Body)
						rp[i] = new RestParamDefaults.BodyObject(t);
					else if (a instanceof org.apache.juneau.rest.annotation.Method)
						rp[i] = new RestParamDefaults.MethodObject(method, t);
					else if (a instanceof PathRemainder)
						rp[i] = new RestParamDefaults.PathRemainderObject(method, t);
					else if (a instanceof Properties)
						rp[i] = new RestParamDefaults.PropsObject(method, t);
					else if (a instanceof Messages)
						rp[i] = new RestParamDefaults.MessageBundleObject();
				}
			}

			if (rp[i] == null) {
				Path p = null;
				for (Annotation a : pa[i])
					if (a instanceof Path)
						p = (Path)a;

				String name = (p == null ? "" : firstNonEmpty(p.name(), p.value()));

				if (isEmpty(name)) {
					int idx = attrIndex++;
					String[] vars = pathPattern.getVars();
					if (vars.length <= idx)
						throw new RestServletException("Number of attribute parameters in method ''{0}'' exceeds the number of URL pattern variables.", method.getName());

					// Check for {#} variables.
					String idxs = String.valueOf(idx);
					for (int j = 0; j < vars.length; j++)
						if (isNumeric(vars[j]) && vars[j].equals(idxs))
							name = vars[j];

					if (isEmpty(name))
						name = pathPattern.getVars()[idx];
				}
				rp[i] = new RestParamDefaults.PathParameterObject(name, t);
			}
		}

		return rp;
	}

	/**
	 * Returns the URL-encoding parser associated with this resource.
	 *
	 * @return The URL-encoding parser associated with this resource.  Never <jk>null</jk>.
	 */
	protected UrlEncodingParser getUrlEncodingParser() {
		return urlEncodingParser;
	}

	/**
	 * Returns the URL-encoding serializer associated with this resource.
	 *
	 * @return The URL-encoding serializer associated with this resource.  Never <jk>null</jk>.
	 */
	protected UrlEncodingSerializer getUrlEncodingSerializer() {
		return urlEncodingSerializer;
	}

	/**
	 * Returns the encoders associated with this resource.
	 *
	 * <p>
	 * Encoders are used to provide various types of encoding such as <code>gzip</code> encoding.
	 *
	 * <p>
	 * Encoders at the class level are defined via one of the following:
	 * <ul>
	 * 	<li>{@link RestResource#encoders() @RestResource.encoders()} annotation.
	 * 	<li>{@link RestConfig#addEncoders(Class...)}/{@link RestConfig#addEncoders(org.apache.juneau.encoders.Encoder...)}
	 * 		methods.
	 * </ul>
	 *
	 * @return The encoders associated with this resource.  Never <jk>null</jk>.
	 */
	protected EncoderGroup getEncoders() {
		return encoders;
	}

	/**
	 * Returns the explicit list of supported accept types for this resource.
	 *
	 * <p>
	 * By default, this is simply the list of accept types supported by the registered parsers, but
	 * can be overridden via the {@link RestConfig#setSupportedAcceptTypes(MediaType...)}/{@link RestConfig#setSupportedAcceptTypes(String...)}
	 * methods.
	 *
	 * @return The supported <code>Accept</code> header values for this resource.  Never <jk>null</jk>.
	 */
	protected MediaType[] getSupportedAcceptTypes() {
		return supportedAcceptTypes;
	}

	/**
	 * Returns the explicit list of supported content types for this resource.
	 *
	 * <p>
	 * By default, this is simply the list of content types supported by the registered serializers, but can be
	 * overridden via the {@link RestConfig#setSupportedContentTypes(MediaType...)}/{@link RestConfig#setSupportedContentTypes(String...)}
	 * methods.
	 *
	 * @return The supported <code>Content-Type</code> header values for this resource.  Never <jk>null</jk>.
	 */
	protected MediaType[] getSupportedContentTypes() {
		return supportedContentTypes;
	}

	/**
	 * Returns the default request headers for this resource.
	 *
	 * <p>
	 * These are headers automatically added to requests if not present.
	 *
	 * <p>
	 * Default request headers are defined via one of the following:
	 * <ul>
	 * 	<li>{@link RestResource#defaultRequestHeaders() @RestResource.defaultRequestHeaders()} annotation.
	 * 	<li>{@link RestConfig#addDefaultRequestHeader(String, Object)}/{@link RestConfig#addDefaultRequestHeaders(String...)} methods.
	 * </ul>
	 *
	 * @return The default request headers for this resource.  Never <jk>null</jk>.
	 */
	protected Map<String,String> getDefaultRequestHeaders() {
		return defaultRequestHeaders;
	}

	/**
	 * Returns the default response headers for this resource.
	 *
	 * <p>
	 * These are headers automatically added to responses if not otherwise specified during the request.
	 *
	 * <p>
	 * Default response headers are defined via one of the following:
	 * <ul>
	 * 	<li>{@link RestResource#defaultResponseHeaders() @RestResource.defaultResponseHeaders()} annotation.
	 * 	<li>{@link RestConfig#addDefaultResponseHeader(String, Object)}/{@link RestConfig#addDefaultResponseHeaders(String...)}
	 * 		methods.
	 * </ul>
	 *
	 * @return The default response headers for this resource.  Never <jk>null</jk>.
	 */
	public Map<String,Object> getDefaultResponseHeaders() {
		return defaultResponseHeaders;
	}

	/**
	 * Returns the converters associated with this resource at the class level.
	 *
	 * <p>
	 * Converters are used to 'convert' POJOs from one form to another before being passed of to the response handlers.
	 *
	 * <p>
	 * Converters at the class level are defined via one of the following:
	 * <ul>
	 * 	<li>{@link RestResource#converters() @RestResource.converters()} annotation.
	 * 	<li>{@link RestConfig#addConverters(Class...)}/{@link RestConfig#addConverters(RestConverter...)} methods.
	 * </ul>
	 *
	 * @return The converters associated with this resource.  Never <jk>null</jk>.
	 */
	protected RestConverter[] getConverters() {
		return converters;
	}

	/**
	 * Returns the guards associated with this resource at the class level.
	 *
	 * <p>
	 * Guards are used to restrict access to resources.
	 *
	 * <p>
	 * Guards at the class level are defined via one of the following:
	 * <ul>
	 * 	<li>{@link RestResource#guards() @RestResource.guards()} annotation.
	 * 	<li>{@link RestConfig#addGuards(Class...)}/{@link RestConfig#addGuards(RestGuard...)} methods.
	 * </ul>
	 *
	 * @return The guards associated with this resource.  Never <jk>null</jk>.
	 */
	protected RestGuard[] getGuards() {
		return guards;
	}

	/**
	 * Returns the response handlers associated with this resource.
	 *
	 * <p>
	 * Response handlers are used to convert POJOs returned by REST Java methods into actual HTTP responses.
	 *
	 * <p>
	 * Response handlers are defined via one of the following:
	 * <ul>
	 * 	<li>{@link RestResource#responseHandlers() @RestResource.responseHandlers()} annotation.
	 * 	<li>{@link RestConfig#addResponseHandlers(Class...)}/{@link RestConfig#addResponseHandlers(ResponseHandler...)}
	 * 		methods.
	 * </ul>
	 *
	 * @return The response handlers associated with this resource.  Never <jk>null</jk>.
	 */
	protected ResponseHandler[] getResponseHandlers() {
		return responseHandlers;
	}

	/**
	 * Returns the media type for the specified file name.
	 *
	 * <p>
	 * The list of MIME-type mappings can be augmented through the {@link RestConfig#addMimeTypes(String...)} method.
	 * See that method for a description of predefined MIME-type mappings.
	 *
	 * @param name The file name.
	 * @return The MIME-type, or <jk>null</jk> if it could not be determined.
	 */
	protected String getMediaTypeForName(String name) {
		return mimetypesFileTypeMap.getContentType(name);
	}

	/**
	 * Returns the favicon of the resource.
	 *
	 * <p>
	 * This is the icon served up under <js>"/favicon.ico"</jk> recognized by browsers.
	 *
	 * <p>
	 * The favicon is defined via one of the following:
	 * <ul>
	 * 	<li>{@link RestResource#favicon() @RestResource.favicon()} annotation.
	 * 	<li>{@link RestConfig#setFavIcon(Object)}/{@link RestConfig#setFavIcon(Class, String)} methods.
	 * </ul>
	 *
	 * @return The favicon of this resource.  Can be <jk>null</jk>.
	 */
	protected StreamResource getFavIcon() {
		return favIcon;
	}

	/**
	 * Returns the stylesheet for use in the HTML views of the resource.
	 *
	 * <p>
	 * This is the contents of the page served up under <js>"/styles.css"</jk>.
	 *
	 * <p>
	 * The stylesheet is defined via one of the following:
	 * <ul>
	 * 	<li>{@link RestResource#stylesheet() @RestResource.stylesheet()} annotation.
	 * 	<li>{@link RestConfig#setStyleSheet(Object...)}/{@link RestConfig#setStyleSheet(Class, String)} methods.
	 * </ul>
	 *
	 * @return The aggregated stylesheet of this resource.  Never <jk>null</jk>.
	 */
	protected StreamResource getStyleSheet() {
		return styleSheet;
	}

	/**
	 * Returns <jk>true</jk> if the specified path refers to a static file.
	 *
	 * <p>
	 * Static files are files pulled from the classpath and served up directly to the browser.
	 *
	 * <p>
	 * Static files are defined via one of the following:
	 * <ul>
	 * 	<li>{@link RestResource#staticFiles() @RestResource.staticFiles()} annotation.
	 * 	<li>{@link RestConfig#addStaticFiles(Class, String)} method.
	 * </ul>
	 *
	 * @param p The URL path remainder after the servlet match.
	 * @return <jk>true</jk> if the specified path refers to a static file.
	 */
	protected boolean isStaticFile(String p) {
		return pathStartsWith(p, staticFilesPrefixes);
	}

	/**
	 * Returns the REST Java methods defined in this resource.
	 *
	 * <p>
	 * These are the methods annotated with the {@link RestMethod @RestMethod} annotation.
	 *
	 * @return A map of Java method names to call method objects.
	 */
	protected Map<String,CallMethod> getCallMethods() {
		return callMethods;
	}

	/**
	 * Calls {@link Servlet#destroy()} on any child resources defined on this resource.
	 */
	protected void destroy() {
		for (RestContext r : childResources.values()) {
			r.destroy();
			if (r.resource instanceof Servlet)
				((Servlet)r.resource).destroy();
		}
	}

	/**
	 * Returns <jk>true</jk> if this resource has any child resources associated with it.
	 *
	 * @return <jk>true</jk> if this resource has any child resources associated with it.
	 */
	protected boolean hasChildResources() {
		return ! childResources.isEmpty();
	}

	/**
	 * Returns the context of the child resource associated with the specified path.
	 *
	 * @param path The path of the child resource to resolve.
	 * @return The resolved context, or <jk>null</jk> if it could not be resolved.
	 */
	protected RestContext getChildResource(String path) {
		return childResources.get(path);
	}


	//----------------------------------------------------------------------------------------------------
	// Utility methods
	//----------------------------------------------------------------------------------------------------

	/**
	 * Takes in an object of type T or a Class<T> and either casts or constructs a T.
	 */
	@SuppressWarnings("unchecked")
	private static <T> T resolve(Class<T> c, Object o, Object...cArgs) throws RestServletException {
		if (c.isInstance(o))
			return (T)o;
		if (! (o instanceof Class))
			throw new RestServletException("Invalid object type passed to resolve:  ''{0}''.  Must be an object of type T or a Class<? extend T>.", o.getClass());
		Constructor<T> n = findPublicConstructor((Class<T>)o, cArgs);
		if (n == null)
			throw new RestServletException("Could not find public constructor for class ''{0}'' that takes in args {1}", c, JsonSerializer.DEFAULT_LAX.toString(getClasses(cArgs)));
		try {
			return n.newInstance(cArgs);
		} catch (Exception e) {
			throw new RestServletException("Exception occurred while constructing class ''{0}''", c).initCause(e);
		}
	}
}
