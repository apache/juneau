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
import javax.servlet.http.*;

import org.apache.juneau.*;
import org.apache.juneau.encoders.*;
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
import org.apache.juneau.svl.vars.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.utils.*;

/**
 * Contains all the configuration on a REST resource and the entry points for handling REST calls.
 *
 * <p>
 * See {@link PropertyStore} for more information about context properties.
 */
public final class RestContext extends Context {

	private final Object resource;
	final RestConfig config;
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
		contextPath;

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
	private final RestResourceResolver resourceResolver;

	// Lifecycle methods
	private final Method[]
		postInitMethods,
		postInitChildFirstMethods,
		preCallMethods,
		postCallMethods,
		startCallMethods,
		endCallMethods,
		destroyMethods;
	private final RestParam[][]
		preCallMethodParams,
		postCallMethodParams;
	private final Class<?>[][]
		postInitMethodParams,
		postInitChildFirstMethodParams,
		startCallMethodParams,
		endCallMethodParams,
		destroyMethodParams;

	// In-memory cache of images and stylesheets in the org.apache.juneau.rest.htdocs package.
	private final Map<String,StreamResource> staticFilesCache = new ConcurrentHashMap<>();

	private final ResourceFinder resourceFinder;
	private final ConcurrentHashMap<Integer,AtomicInteger> stackTraceHashes = new ConcurrentHashMap<>();


	/**
	 * Constructor.
	 *
	 * @param resource The resource class (a class annotated with {@link RestResource @RestResource}).
	 * @param servletContext
	 * 	The servlet context object.
	 * 	Can be <jk>null</jk> if this isn't a
	 * @param config The servlet configuration object.
	 * @throws Exception If any initialization problems were encountered.
	 */
	@SuppressWarnings("unchecked")
	public RestContext(Object resource, ServletContext servletContext, RestConfig config) throws Exception {
		super(null);
		RestException _initException = null;
		try {
			this.resource = resource;
			this.config = config;
			this.resourceFinder = new ResourceFinder(resource.getClass());
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
			this.staticFilesMap = Collections.unmodifiableMap(b.staticFilesMap);
			this.staticFilesPrefixes = b.staticFilesPrefixes;
			this.msgs = b.messageBundle;
			this.childResources = Collections.synchronizedMap(new LinkedHashMap<String,RestContext>());  // Not unmodifiable on purpose so that children can be replaced.
			this.logger = b.logger;
			this.fullPath = b.fullPath;
			this.contextPath = nullIfEmpty(b.contextPath);
			this.widgets = Collections.unmodifiableMap(b.widgets);

			//----------------------------------------------------------------------------------------------------
			// Initialize the child resources.
			// Done after initializing fields above since we pass this object to the child resources.
			//----------------------------------------------------------------------------------------------------
			List<String> methodsFound = new LinkedList<>();   // Temporary to help debug transient duplicate method issue.
			Map<String,CallRouter.Builder> routers = new LinkedHashMap<>();
			Map<String,CallMethod> _javaRestMethods = new LinkedHashMap<>();
			Map<String,Method>
				_startCallMethods = new LinkedHashMap<>(),
				_preCallMethods = new LinkedHashMap<>(),
				_postCallMethods = new LinkedHashMap<>(),
				_endCallMethods = new LinkedHashMap<>(),
				_postInitMethods = new LinkedHashMap<>(),
				_postInitChildFirstMethods = new LinkedHashMap<>(),
				_destroyMethods = new LinkedHashMap<>();
			List<RestParam[]>
				_preCallMethodParams = new ArrayList<>(),
				_postCallMethodParams = new ArrayList<>();
			List<Class<?>[]>
				_startCallMethodParams = new ArrayList<>(),
				_endCallMethodParams = new ArrayList<>(),
				_postInitMethodParams = new ArrayList<>(),
				_postInitChildFirstMethodParams = new ArrayList<>(),
				_destroyMethodParams = new ArrayList<>();

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
												try (Closeable in = p.isReaderParser() ? req.getReader() : req.getInputStream()) {
													Object output = m.invoke(o, p.parseArgs(in, m.getGenericParameterTypes()));
													res.setOutput(output);
												}
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

			for (Method m : ClassUtils.getAllMethods(resource.getClass(), true)) {
				if (ClassUtils.isPublic(m) && m.isAnnotationPresent(RestHook.class)) {
					HookEvent he = m.getAnnotation(RestHook.class).value();
					String sig = ClassUtils.getMethodSignature(m);
					switch(he) {
						case PRE_CALL: {
							if (! _preCallMethods.containsKey(sig)) {
								Visibility.setAccessible(m);
								_preCallMethods.put(sig, m);
								_preCallMethodParams.add(findParams(m, false, null, true));
							}
							break;
						}
						case POST_CALL: {
							if (! _postCallMethods.containsKey(sig)) {
								Visibility.setAccessible(m);
								_postCallMethods.put(sig, m);
								_postCallMethodParams.add(findParams(m, false, null, true));
							}
							break;
						}
						case START_CALL: {
							if (! _startCallMethods.containsKey(sig)) {
								Visibility.setAccessible(m);
								_startCallMethods.put(sig, m);
								_startCallMethodParams.add(m.getParameterTypes());
								ClassUtils.assertArgsOfType(m, HttpServletRequest.class, HttpServletResponse.class);
							}
							break;
						}
						case END_CALL: {
							if (! _endCallMethods.containsKey(sig)) {
								Visibility.setAccessible(m);
								_endCallMethods.put(sig, m);
								_endCallMethodParams.add(m.getParameterTypes());
								ClassUtils.assertArgsOfType(m, HttpServletRequest.class, HttpServletResponse.class);
							}
							break;
						}
						case POST_INIT: {
							if (! _postInitMethods.containsKey(sig)) {
								Visibility.setAccessible(m);
								_postInitMethods.put(sig, m);
								_postInitMethodParams.add(m.getParameterTypes());
								ClassUtils.assertArgsOfType(m, RestContext.class);
							}
							break;
						}
						case POST_INIT_CHILD_FIRST: {
							if (! _postInitChildFirstMethods.containsKey(sig)) {
								Visibility.setAccessible(m);
								_postInitChildFirstMethods.put(sig, m);
								_postInitChildFirstMethodParams.add(m.getParameterTypes());
								ClassUtils.assertArgsOfType(m, RestContext.class);
							}
							break;
						}
						case DESTROY: {
							if (! _destroyMethods.containsKey(sig)) {
								Visibility.setAccessible(m);
								_destroyMethods.put(sig, m);
								_destroyMethodParams.add(m.getParameterTypes());
								ClassUtils.assertArgsOfType(m, RestContext.class);
							}
							break;
						}
						default: // Ignore INIT
					}
				}
			}

			this.callMethods = Collections.unmodifiableMap(_javaRestMethods);
			this.preCallMethods = _preCallMethods.values().toArray(new Method[_preCallMethods.size()]);
			this.postCallMethods = _postCallMethods.values().toArray(new Method[_postCallMethods.size()]);
			this.startCallMethods = _startCallMethods.values().toArray(new Method[_startCallMethods.size()]);
			this.endCallMethods = _endCallMethods.values().toArray(new Method[_endCallMethods.size()]);
			this.postInitMethods = _postInitMethods.values().toArray(new Method[_postInitMethods.size()]);
			this.postInitChildFirstMethods = _postInitChildFirstMethods.values().toArray(new Method[_postInitChildFirstMethods.size()]);
			this.destroyMethods = _destroyMethods.values().toArray(new Method[_destroyMethods.size()]);
			this.preCallMethodParams = _preCallMethodParams.toArray(new RestParam[_preCallMethodParams.size()][]);
			this.postCallMethodParams = _postCallMethodParams.toArray(new RestParam[_postCallMethodParams.size()][]);
			this.startCallMethodParams = _startCallMethodParams.toArray(new Class[_startCallMethodParams.size()][]);
			this.endCallMethodParams = _endCallMethodParams.toArray(new Class[_endCallMethodParams.size()][]);
			this.postInitMethodParams = _postInitMethodParams.toArray(new Class[_postInitMethodParams.size()][]);
			this.postInitChildFirstMethodParams = _postInitChildFirstMethodParams.toArray(new Class[_postInitChildFirstMethodParams.size()][]);
			this.destroyMethodParams = _destroyMethodParams.toArray(new Class[_destroyMethodParams.size()][]);

			Map<String,CallRouter> _callRouters = new LinkedHashMap<>();
			for (CallRouter.Builder crb : routers.values())
				_callRouters.put(crb.getHttpMethodName(), crb.build());
			this.callRouters = Collections.unmodifiableMap(_callRouters);

			// Initialize our child resources.
			resourceResolver = resolve(resource, RestResourceResolver.class, b.resourceResolver);
			for (Object o : config.childResources) {
				String path = null;
				Object r = null;
				if (o instanceof Pair) {
					Pair<String,Object> p = (Pair<String,Object>)o;
					path = p.first();
					r = p.second();
				} else if (o instanceof Class<?>) {
					Class<?> c = (Class<?>)o;
					// Don't allow specifying yourself as a child.  Causes an infinite loop.
					if (c == config.resourceClass)
						continue;
					r = c;
				} else {
					r = o;
				}

				RestConfig childConfig = null;

				if (o instanceof Class) {
					Class<?> oc = (Class<?>)o;
					childConfig = new RestConfig(config.inner, oc, this);
					r = resourceResolver.resolve(oc, childConfig);
				} else {
					r = o;
					childConfig = new RestConfig(config.inner, o.getClass(), this);
				}

				childConfig.init(r);
				if (r instanceof RestServlet)
					((RestServlet)r).innerInit(childConfig);
				RestContext rc2 = new RestContext(r, servletContext, childConfig);
				if (r instanceof RestServlet)
					((RestServlet)r).setContext(rc2);
				path = childConfig.path;
				childResources.put(path, rc2);
			}

			callHandler = config.callHandler == null ? new RestCallHandler(this) : resolve(resource, RestCallHandler.class, config.callHandler, this);
			infoProvider = config.infoProvider == null ? new RestInfoProvider(this) : resolve(resource, RestInfoProvider.class, config.infoProvider, this);

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
		Map<Class<?>,RestParam> paramResolvers = new HashMap<>();
		SerializerGroup serializers;
		ParserGroup parsers;
		UrlEncodingSerializer urlEncodingSerializer;
		UrlEncodingParser urlEncodingParser;
		EncoderGroup encoders;
		String clientVersionHeader = "", defaultCharset, paramFormat;

		List<MediaType> supportedContentTypes, supportedAcceptTypes;
		Map<String,String> defaultRequestHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		Map<String,Object> defaultResponseHeaders;
		BeanContext beanContext;
		List<RestConverter> converters = new ArrayList<>();
		List<RestGuard> guards = new ArrayList<>();
		List<ResponseHandler> responseHandlers = new ArrayList<>();
		MimetypesFileTypeMap mimetypesFileTypeMap;
		Map<String,String> staticFilesMap;
		String[] staticFilesPrefixes;
		MessageBundle messageBundle;
		Set<String> allowMethodParams = new LinkedHashSet<>();
		RestLogger logger;
		String fullPath;
		Map<String,Widget> widgets;
		Object resourceResolver;
		String contextPath;

		@SuppressWarnings("unchecked")
		private Builder(Object resource, RestConfig sc) throws Exception {

			PropertyStore ps = sc.createPropertyStore();

			LinkedHashMap<Class<?>,RestResource> restResourceAnnotationsChildFirst = findAnnotationsMap(RestResource.class, resource.getClass());

			allowHeaderParams = getBoolean(sc.allowHeaderParams, "juneau.allowHeaderParams", true);
			allowBodyParam = getBoolean(sc.allowBodyParam, "juneau.allowBodyParam", true);
			renderResponseStackTraces = getBoolean(sc.renderResponseStackTraces, "juneau.renderResponseStackTraces", false);
			useStackTraceHashes = getBoolean(sc.useStackTraceHashes, "juneau.useStackTraceHashes", true);
			defaultCharset = getString(sc.defaultCharset, "juneau.defaultCharset", "utf-8");
			paramFormat = getString(sc.paramFormat, "juneau.paramFormat", "UON");
			resourceResolver = sc.resourceResolver;

			String amp = getString(sc.allowMethodParam, "juneau.allowMethodParam", "HEAD,OPTIONS");
			if ("true".equals(amp))
				amp = "*";// For backwards compatibility when this was a boolean field.
			else
				amp = amp.toUpperCase();
			allowMethodParams.addAll(Arrays.asList(StringUtils.split(amp)));

			varResolver = sc.varResolverBuilder
				.vars(FileVar.class, LocalizationVar.class, RequestVar.class, SerializedRequestAttrVar.class, ServletInitParamVar.class, UrlVar.class, UrlEncodeVar.class, WidgetVar.class)
				.build()
			;
			configFile = sc.configFile.getResolving(this.varResolver);
			properties = sc.properties;
			Collections.reverse(sc.beanFilters);
			Collections.reverse(sc.pojoSwaps);
			beanFilters = toObjectArray(sc.beanFilters, Class.class);
			pojoSwaps = toObjectArray(sc.pojoSwaps, Class.class);

			for (Class<?> c : sc.paramResolvers) {
				RestParam rp = newInstanceFromOuter(resource, RestParam.class, c);
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
			defaultResponseHeaders = Collections.unmodifiableMap(new LinkedHashMap<>(sc.defaultResponseHeaders));
			beanContext = ps.getBeanContext();
			contextPath = sc.contextPath;

			for (Object o : sc.converters)
				converters.add(resolve(resource, RestConverter.class, o));

			for (Object o : sc.guards)
				guards.add(resolve(resource, RestGuard.class, o));

			for (Object o : sc.responseHandlers)
				responseHandlers.add(resolve(resource, ResponseHandler.class, o));

			mimetypesFileTypeMap = sc.mimeTypes;

			VarResolver vr = sc.getVarResolverBuilder().build();

			staticFilesMap = new LinkedHashMap<>();
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

			logger = sc.logger == null ? new RestLogger.NoOp() : resolve(resource, RestLogger.class, sc.logger);

			fullPath = (sc.parentContext == null ? "" : (sc.parentContext.fullPath + '/')) + sc.path;

			HtmlDocBuilder hdb = new HtmlDocBuilder(sc.properties);

			this.widgets = new LinkedHashMap<>();

			for (Class<? extends Widget> wc : sc.widgets) {
				Widget w = resolve(resource, Widget.class, wc);
				String n = w.getName();
				this.widgets.put(n, w);
				hdb.script("INHERIT", "$W{"+n+".script}");
				hdb.style("INHERIT", "$W{"+n+".style}");
			}
		}
	}

	private static boolean getBoolean(Object o, String systemProperty, boolean def) {
		if (o == null)
			o = SystemUtils.getFirstBoolean(def, systemProperty);
		return "true".equalsIgnoreCase(o.toString());
	}

	private static String getString(Object o, String systemProperty, String def) {
		if (o == null)
			o = SystemUtils.getFirstString(def, systemProperty);
		return o.toString();
	}

	/**
	 * Returns the resource resolver associated with this context.
	 *
	 * <p>
	 * The resource resolver is used for instantiating child resource classes.
	 *
	 * <p>
	 * Unless overridden via the {@link RestResource#resourceResolver()} annotation or the {@link RestConfig#setResourceResolver(Class)}
	 * method, this value is always inherited from parent to child.
	 * This allows a single resource resolver to be passed in to the top-level servlet to handle instantiation of all
	 * child resources.
	 *
	 * @return The resource resolver associated with this context.
	 */
	protected RestResourceResolver getResourceResolver() {
		return resourceResolver;
	}

	/**
	 * Returns the variable resolver for this servlet.
	 *
	 * <p>
	 * Variable resolvers are used to replace variables in property values.
	 * They can be nested arbitrarily deep.
	 * They can also return values that themselves contain other variables.
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
	 * 			<ja>@Property</ja>(name=<js>"v1"</js>,value=<js>"$R{foo}"</js>),  <jc>// Request variable.  value="bar"</jc>
	 * 			<ja>@Property</ja>(name=<js>"v2"</js>,value=<js>"$R{$R{foo}}"</js>)  <jc>// Nested request variable. value="baz"</jc>
	 * 		}
	 * 	)
	 * 	<jk>public class</jk> MyRestResource <jk>extends</jk> RestServletDefault {
	 * </p>
	 *
	 * <p>
	 * A typical usage pattern involves using variables inside the {@link HtmlDoc} annotation:
	 * <p class='bcode'>
	 * 	<ja>@RestMethod</ja>(
	 * 		name=<jsf>GET</jsf>, path=<js>"/{name}/*"</js>,
	 * 		htmldoc=@HtmlDoc(
	 * 			navlinks={
	 * 				<js>"up: $R{requestParentURI}"</js>,
	 * 				<js>"options: servlet:/?method=OPTIONS"</js>,
	 * 				<js>"editLevel: servlet:/editLevel?logger=$A{attribute.name, OFF}"</js>
	 * 			}
	 * 			header={
	 * 				<js>"&lt;h1&gt;$L{MyLocalizedPageTitle}&lt;/h1&gt;"</js>
	 * 			},
	 * 			aside={
	 * 				<js>"$F{resources/AsideText.html}"</js>
	 * 			}
	 * 		)
	 * 	)
	 * 	<jk>public</jk> LoggerEntry getLogger(RestRequest req, <ja>@Path</ja> String name) <jk>throws</jk> Exception {
	 * </p>
	 *
	 * <p>
	 * The following is the default list of supported variables:
	 * <ul>
	 * 	<li><code>$C{key[,defaultValue]}</code> - Config file entry. See {@link ConfigFileVar}.
	 * 	<li><code>$E{envVar[,defaultValue]}</code> - Environment variable. See {@link EnvVariablesVar}.
	 * 	<li><code>$F{path[,defaultValue]}</code> - File resource. See {@link FileVar}.
	 * 	<li><code>$I{name[,defaultValue]}</code> - Servlet init parameter. See {@link ServletInitParamVar}.
	 * 	<li><code>$L{key[,args...]}</code> - Localized message. See {@link LocalizationVar}.
	 * 	<li><code>$R{key[,args...]}</code> - Request variable. See {@link RequestVar}.
	 * 	<li><code>$S{systemProperty[,defaultValue]}</code> - System property. See {@link SystemPropertiesVar}.
	 * 	<li><code>$SA{contentType,key[,defaultValue]}</code> - Serialized request attribute. See {@link SerializedRequestAttrVar}.
	 * 	<li><code>$U{uri}</code> - URI resolver. See {@link UrlVar}.
	 * 	<li><code>$UE{uriPart}</code> - URL-Encoder. See {@link UrlEncodeVar}.
	 * 	<li><code>$W{widgetName}</code> - HTML widget variable. See {@link WidgetVar}.
	 * </ul>
	 *
	 * <p>
	 * The following syntax variables are also provided:
	 * <ul>
	 * 	<li><code>$IF{booleanArg,thenValue[,elseValue]}</code> - If/else variable. See {@link IfVar}.
	 * 	<li><code>$SW{stringArg(,pattern,thenValue)+[,elseValue]}</code> - Switch variable. See {@link SwitchVar}.
	 * </ul>
	 *
	 * <p>
	 * The list of variables can be extended using the {@link RestConfig#addVars(Class...)} method.
	 * For example, this is used to add support for the Args and Manifest-File variables in the microservice
	 * <code>Resource</code> class.
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
						try (InputStream is = getResource(p2, null)) {
							if (is != null) {
								int i = p2.lastIndexOf('/');
								String name = (i == -1 ? p2 : p2.substring(i+1));
								String mediaType = mimetypesFileTypeMap.getContentType(name);
								ObjectMap headers = new ObjectMap().append("Cache-Control", "max-age=86400, public");
								staticFilesCache.put(pathInfo, new StreamResource(MediaType.forString(mediaType), headers, is));
								return staticFilesCache.get(pathInfo);
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
		return resourceFinder.getResourceAsStream(name, locale);
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
		return resourceFinder.getResourceAsString(name, locale);
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
					try (Closeable in = p.isReaderParser() ? new InputStreamReader(is, UTF8) : is) {
						return p.parse(in, c);
					}
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
	 * The widgets used for resolving <js>"$W{...}"<js> variables.
	 *
	 * <p>
	 * Defined by the {@link HtmlDoc#widgets()} annotation or {@link RestConfig#addWidget(Class)} method.
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
	 * Returns the value of the {@link RestResource#renderResponseStackTraces()} setting.
	 *
	 * @return The value of the {@link RestResource#renderResponseStackTraces()} setting.
	 */
	protected boolean isRenderResponseStackTraces() {
		return renderResponseStackTraces;
	}

	/**
	 * Returns the value of the {@link RestResource#allowHeaderParams()} setting.
	 *
	 * @return The value of the {@link RestResource#allowHeaderParams()} setting.
	 */
	protected boolean isAllowHeaderParams() {
		return allowHeaderParams;
	}

	/**
	 * Returns the value of the {@link RestResource#allowBodyParam()} setting.
	 *
	 * @return The value of the {@link RestResource#allowBodyParam()} setting.
	 */
	protected boolean isAllowBodyParam() {
		return allowBodyParam;
	}

	/**
	 * Returns the value of the {@link RestResource#defaultCharset()} setting.
	 *
	 * @return The value of the {@link RestResource#defaultCharset()} setting.
	 */
	protected String getDefaultCharset() {
		return defaultCharset;
	}

	/**
	 * Returns the value of the {@link RestResource#paramFormat()} setting.
	 *
	 * @return The value of the {@link RestResource#paramFormat()} setting.
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
	 * @param isPreOrPost Whether this is a <ja>@RestMethodPre</ja> or <ja>@RestMethodPost</ja>.
	 * @return The array of resolvers.
	 * @throws ServletException If an annotation usage error was detected.
	 */
	protected RestParam[] findParams(Method method, boolean methodPlainParams, UrlPathPattern pathPattern, boolean isPreOrPost) throws ServletException {

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

				if (isPreOrPost)
					throw new RestServletException("Invalid parameter specified for method ''{0}'' at index position {1}", method, i);

				Path p = null;
				for (Annotation a : pa[i])
					if (a instanceof Path)
						p = (Path)a;

				String name = (p == null ? "" : firstNonEmpty(p.name(), p.value()));

				if (isEmpty(name)) {
					int idx = attrIndex++;
					String[] vars = pathPattern.getVars();
					if (vars.length <= idx)
						throw new RestServletException("Number of attribute parameters in method ''{0}'' exceeds the number of URL pattern variables.", method);

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

	/*
	 * Calls all @RestHook(PRE) methods.
	 */
	void preCall(RestRequest req, RestResponse res) throws RestException {
		for (int i = 0; i < preCallMethods.length; i++)
			preOrPost(resource, preCallMethods[i], preCallMethodParams[i], req, res);
	}

	/*
	 * Calls all @RestHook(POST) methods.
	 */
	void postCall(RestRequest req, RestResponse res) throws RestException {
		for (int i = 0; i < postCallMethods.length; i++)
			preOrPost(resource, postCallMethods[i], postCallMethodParams[i], req, res);
	}

	private static void preOrPost(Object resource, Method m, RestParam[] mp, RestRequest req, RestResponse res) throws RestException {
		if (m != null) {
			Object[] args = new Object[mp.length];
			for (int i = 0; i < mp.length; i++) {
				try {
					args[i] = mp[i].resolve(req, res);
				} catch (RestException e) {
					throw e;
				} catch (Exception e) {
					throw new RestException(SC_BAD_REQUEST,
						"Invalid data conversion.  Could not convert {0} ''{1}'' to type ''{2}'' on method ''{3}.{4}''.",
						mp[i].getParamType().name(), mp[i].getName(), mp[i].getType(), m.getDeclaringClass().getName(), m.getName()
					).initCause(e);
				}
			}
			try {
				m.invoke(resource, args);
			} catch (RestException e) {
				throw e;
			} catch (Exception e) {
				throw new RestException(SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage()).initCause(e);
			}
		}
	}

	/*
	 * Calls all @RestHook(START) methods.
	 */
	void startCall(HttpServletRequest req, HttpServletResponse res) {
		for (int i = 0; i < startCallMethods.length; i++)
			startOrFinish(resource, startCallMethods[i], startCallMethodParams[i], req, res);
	}

	/*
	 * Calls all @RestHook(FINISH) methods.
	 */
	void finishCall(HttpServletRequest req, HttpServletResponse res) {
		for (int i = 0; i < endCallMethods.length; i++)
			startOrFinish(resource, endCallMethods[i], endCallMethodParams[i], req, res);
	}

	private static void startOrFinish(Object resource, Method m, Class<?>[] p, HttpServletRequest req, HttpServletResponse res) {
		if (m != null) {
			Object[] args = new Object[p.length];
			for (int i = 0; i < p.length; i++) {
				if (p[i] == HttpServletRequest.class)
					args[i] = req;
				else if (p[i] == HttpServletResponse.class)
					args[i] = res;
			}
			try {
				m.invoke(resource, args);
			} catch (RestException e) {
				throw e;
			} catch (Exception e) {
				throw new RestException(SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage()).initCause(e);
			}
		}
	}

	/*
	 * Calls all @RestHook(POST_INIT) methods.
	 */
	void postInit() throws ServletException {
		for (int i = 0; i < postInitMethods.length; i++)
			postInitOrDestroy(resource, postInitMethods[i], postInitMethodParams[i]);
		for (RestContext childContext : this.childResources.values())
			childContext.postInit();
	}

	/*
	 * Calls all @RestHook(POST_INIT_CHILD_FIRST) methods.
	 */
	void postInitChildFirst() throws ServletException {
		for (RestContext childContext : this.childResources.values())
			childContext.postInitChildFirst();
		for (int i = 0; i < postInitChildFirstMethods.length; i++)
			postInitOrDestroy(resource, postInitChildFirstMethods[i], postInitChildFirstMethodParams[i]);
	}

	private void postInitOrDestroy(Object r, Method m, Class<?>[] p) {
		if (m != null) {
			Object[] args = new Object[p.length];
			for (int i = 0; i < p.length; i++) {
				if (p[i] == RestContext.class)
					args[i] = this;
				else if (p[i] == RestConfig.class)
					args[i] = this.config;
				else if (p[i] == ServletConfig.class)
					args[i] = this.config.inner;
			}
			try {
				m.invoke(r, args);
			} catch (RestException e) {
				throw e;
			} catch (Exception e) {
				throw new RestException(SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage()).initCause(e);
			}
		}
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
		for (int i = 0; i < destroyMethods.length; i++) {
			try {
				postInitOrDestroy(resource, destroyMethods[i], destroyMethodParams[i]);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

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

	/**
	 * Returns the context path of the resource if it's specified via the {@link RestResource#contextPath()} setting
	 * on this or a parent resource.
	 *
	 * @return The {@link RestResource#contextPath()} setting value, or <jk>null</jk> if it's not specified.
	 */
	protected String getContextPath() {
		if (contextPath != null)
			return contextPath;
		if (parentContext != null)
			return parentContext.getContextPath();
		return null;
	}

	//----------------------------------------------------------------------------------------------------
	// Utility methods
	//----------------------------------------------------------------------------------------------------

	/**
	 * Takes in an object of type T or a Class<T> and either casts or constructs a T.
	 */
	private static <T> T resolve(Object outer, Class<T> c, Object o, Object...cArgs) throws RestServletException {
		try {
			return ClassUtils.newInstanceFromOuter(outer, c, o, cArgs);
		} catch (Exception e) {
			throw new RestServletException("Exception occurred while constructing class ''{0}''", c).initCause(e);
		}
	}
}
