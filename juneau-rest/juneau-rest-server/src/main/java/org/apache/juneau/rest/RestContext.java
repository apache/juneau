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
import static org.apache.juneau.http.HttpHeaders.*;
import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.IOUtils.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.rest.HttpRuntimeException.*;
import static org.apache.juneau.rest.ResponseProcessor.*;
import static java.util.Collections.*;
import static java.util.Optional.*;

import java.io.*;
import java.lang.reflect.*;
import java.lang.reflect.Method;
import java.nio.charset.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.logging.*;
import java.util.stream.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.config.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.dto.swagger.Swagger;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.http.annotation.Response;
import org.apache.juneau.httppart.*;
import org.apache.juneau.httppart.bean.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.mstat.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.logging.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.util.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.utils.*;

/**
 * Contains all the configuration on a REST resource and the entry points for handling REST calls.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc RestContext}
 * </ul>
 */
@ConfigurableContext(nocache=true)
public class RestContext extends Context {

	/**
	 * Represents a null value for the {@link Rest#contextClass()} annotation.
	 */
	@SuppressWarnings("javadoc")
	public static final class Null extends RestContext {
		public Null(RestContextBuilder builder) throws Exception {
			super(builder);
		}
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Static
	//-------------------------------------------------------------------------------------------------------------------

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
	 * Static create method.
	 *
	 * <p>
	 * This is the primary method for creating {@link RestContextBuilder} objects.
	 *
	 * <p>
	 * The builder class can be subclassed by using the {@link Rest#builder()} annotation.
	 * This can be useful when you want to perform any customizations on the builder class, typically by overriding protected methods that create
	 * 	the various builders used in the created {@link RestContext} object (which itself can be overridden via {@link RestContextBuilder#type(Class)}).
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
	public static RestContextBuilder create(Class<?> resourceClass, RestContext parentContext, ServletConfig servletConfig) throws ServletException {

		Value<Class<? extends RestContextBuilder>> v = Value.of(RestContextBuilder.class);
		ClassInfo.of(resourceClass)
			.getAnnotations(Rest.class)
			.stream()
			.filter(x -> x.builder() != RestContextBuilder.Null.class)
			.forEach(x -> v.set(x.builder()));

		if (v.get() == RestContextBuilder.class)
			return new RestContextBuilder(resourceClass, parentContext, servletConfig);

		return BeanStore
			.of(parentContext == null ? null : parentContext.getRootBeanStore())
			.addBean(Class.class, resourceClass)
			.addBean(RestContext.class, parentContext)
			.addBean(ServletConfig.class, servletConfig)
			.createBean(v.get());
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final Supplier<?> resource;
	private final Class<?> resourceClass;

	final RestContextBuilder builder;
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

	private final Class<? extends RestOpArg>[] restOpArgs, hookMethodArgs;
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
	private final BeanStore rootBeanStore;
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

	final Enablement debugDefault;
	final DefaultClassList defaultClasses;

	// Lifecycle methods
	private final MethodInvoker[]
		postInitMethods,
		postInitChildFirstMethods,
		startCallMethods,
		endCallMethods,
		destroyMethods;

	private final RestOpInvoker[]
		preCallMethods,
		postCallMethods;

	private final FileFinder fileFinder;
	private final StaticFiles staticFiles;
	private final RestLogger callLogger;
	private final DebugEnablement debugEnablement;

	private final ThreadLocal<RestCall> call = new ThreadLocal<>();

	// Gets set when postInitChildFirst() gets called.
	private final AtomicBoolean initialized = new AtomicBoolean(false);

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing the settings for this bean.
	 * @throws Exception If any initialization problems were encountered.
	 */
	public RestContext(RestContextBuilder builder) throws Exception {
		super(builder);

		startTime = Instant.now();

		REGISTRY.put(builder.resourceClass, this);

		BasicHttpException _initException = null;

		try {
			this.builder = builder;

			resourceClass = builder.resourceClass;
			resource = builder.resource;
			parentContext = builder.parentContext;
			rootBeanStore = builder.beanStore();
			defaultClasses = builder.defaultClasses();

			BeanStore bs = beanStore = rootBeanStore.copy().build();
			beanStore
				.addBean(BeanStore.class, beanStore)
				.addBean(RestContext.class, this)
				.addBean(Object.class, resource.get())
				.addBean(RestContextBuilder.class, builder)
				.addBean(AnnotationWorkList.class, builder.getApplied());

			logger = bs.add(Logger.class, builder.logger());
			thrownStore = bs.add(ThrownStore.class, builder.thrownStore().build());
			methodExecStore = bs.add(MethodExecStore.class, builder.methodExecStore().thrownStoreOnce(thrownStore).build());
			messages = bs.add(Messages.class, builder.messages().build());
			varResolver = bs.add(VarResolver.class, builder.varResolver().bean(Messages.class, messages).build());
			config = bs.add(Config.class, builder.config().resolving(varResolver.createSession()));
			responseProcessors = bs.add(ResponseProcessor[].class, builder.responseProcessors().build().toArray());
			debugDefault = builder.debugDefault;
			callLogger = bs.add(RestLogger.class, builder.callLogger().beanStore(beanStore).loggerOnce(logger).thrownStoreOnce(thrownStore).build());
			partSerializer = bs.add(HttpPartSerializer.class, builder.partSerializer().create());
			partParser = bs.add(HttpPartParser.class, builder.partParser().create());
			jsonSchemaGenerator = bs.add(JsonSchemaGenerator.class, builder.jsonSchemaGenerator().build());
			fileFinder = bs.add(FileFinder.class, builder.fileFinder().build());
			staticFiles = bs.add(StaticFiles.class, builder.staticFiles().build());
			defaultRequestHeaders = bs.add("RestContext.defaultRequestHeaders", builder.defaultRequestHeaders().build());
			defaultResponseHeaders = bs.add("RestContext.defaultResponseHeaders", builder.defaultResponseHeaders().build());
			defaultRequestAttributes = bs.add("RestContext.defaultRequestAttributes", builder.defaultRequestAttributes());
			restOpArgs = builder.restOpArgs().build().asArray();
			hookMethodArgs = builder.hookMethodArgs().build().asArray();

			Object r = resource.get();

			uriContext = builder.uriContext;
			uriAuthority = builder.uriAuthority;
			uriResolution = builder.uriResolution;
			uriRelativity = builder.uriRelativity;

			allowBodyParam = ! builder.disableBodyParam;
			allowedHeaderParams = newCaseInsensitiveSet(ofNullable(builder.allowedHeaderParams).map(x -> "NONE".equals(x) ? "" : x).orElse(""));
			allowedMethodParams = newCaseInsensitiveSet(ofNullable(builder.allowedMethodParams).map(x -> "NONE".equals(x) ? "" : x).orElse(""));
			allowedMethodHeaders = newCaseInsensitiveSet(ofNullable(builder.allowedMethodHeaders).map(x -> "NONE".equals(x) ? "" : x).orElse(""));
			renderResponseStackTraces = builder.renderResponseStackTraces;
			clientVersionHeader = builder.clientVersionHeader;
			defaultCharset = builder.defaultCharset;
			maxInput = builder.maxInput;

			debugEnablement = createDebugEnablement(r, builder, bs);

			path = ofNullable(builder.path).orElse("");
			fullPath = (parentContext == null ? "" : (parentContext.fullPath + '/')) + path;

			String p = path;
			if (! p.endsWith("/*"))
				p += "/*";
			pathMatcher = UrlPathMatcher.of(p);

			startCallMethods = createStartCallMethods(r, builder, bs).stream().map(this::toMethodInvoker).toArray(MethodInvoker[]::new);
			endCallMethods = createEndCallMethods(r, builder, bs).stream().map(this::toMethodInvoker).toArray(MethodInvoker[]::new);
			postInitMethods = createPostInitMethods(r, builder, bs).stream().map(this::toMethodInvoker).toArray(MethodInvoker[]::new);
			postInitChildFirstMethods = createPostInitChildFirstMethods(r, builder, bs).stream().map(this::toMethodInvoker).toArray(MethodInvoker[]::new);
			destroyMethods = createDestroyMethods(r, builder, bs).stream().map(this::toMethodInvoker).toArray(MethodInvoker[]::new);

			preCallMethods = createPreCallMethods(r, builder, bs).stream().map(this::toRestOpInvoker).toArray(RestOpInvoker[]:: new);
			postCallMethods = createPostCallMethods(r, builder, bs).stream().map(this::toRestOpInvoker).toArray(RestOpInvoker[]:: new);

			restOperations = createRestOperations(r, builder, bs);

			List<RestOpContext> opContexts = restOperations.getOpContexts();

			if (builder.produces != null)
				produces = AList.unmodifiable(builder.produces);
			else {
				Set<MediaType> s = opContexts.isEmpty() ? emptySet() : new LinkedHashSet<>(opContexts.get(0).getSerializers().getSupportedMediaTypes());
				opContexts.forEach(x -> s.retainAll(x.getSerializers().getSupportedMediaTypes()));
				produces = AList.unmodifiable(s);
			}

			if (builder.consumes != null)
				consumes = AList.unmodifiable(builder.consumes);
			else {
				Set<MediaType> s = opContexts.isEmpty() ? emptySet() : new LinkedHashSet<>(opContexts.get(0).getParsers().getSupportedMediaTypes());
				opContexts.forEach(x -> s.retainAll(x.getParsers().getSupportedMediaTypes()));
				consumes = AList.unmodifiable(s);
			}

			restChildren = createRestChildren(r, builder, bs, builder.inner);

			swaggerProvider = createSwaggerProvider(r, builder, bs, fileFinder, messages, varResolver);

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

	private MethodInvoker toRestOpInvoker(Method m) {
		return new RestOpInvoker(m, findHookMethodArgs(m, getBeanStore()), getMethodExecStats(m));
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

	/**
	 * Instantiates the REST info provider for this REST resource.
	 *
	 * <p>
	 * Instantiates based on the following logic:
	 * <ul>
	 * 	<li>Returns the resource class itself is an instance of {@link SwaggerProvider}.
	 * 	<li>Looks for swagger provider set via any of the following:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#swaggerProvider(Class)}/{@link RestContextBuilder#swaggerProvider(SwaggerProvider)}
	 * 			<li>{@link Rest#swaggerProvider()}.
	 * 		</ul>
	 * 	<li>Looks for a static or non-static <c>createSwaggerProvider()</> method that returns {@link SwaggerProvider} on the
	 * 		resource class with any of the following arguments:
	 * 		<ul>
	 * 			<li>{@link RestContext}
	 * 			<li>{@link BeanStore}
	 * 			<li>Any {@doc RestInjection injected beans}.
	 * 		</ul>
	 * 	<li>Resolves it via the bean store registered in this context.
	 * 	<li>Instantiates a default {@link BasicSwaggerProvider}.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContextBuilder#swaggerProvider(Class)}
	 * 	<li class='jm'>{@link RestContextBuilder#swaggerProvider(SwaggerProvider)}
	 * </ul>
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param builder
	 * 	The builder for this object.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link RestContextBuilder#beanStore()}.
	 * @param fileFinder The file finder configured on this bean created by {@link RestContextBuilder#createFileFinder(BeanStore,Supplier)}.
	 * @param messages The localized messages configured on this bean.
	 * @param varResolver The variable resolver configured on this bean.
	 * @return The info provider for this REST resource.
	 * @throws Exception If info provider could not be instantiated.
	 */
	protected SwaggerProvider createSwaggerProvider(Object resource, RestContextBuilder builder, BeanStore beanStore, FileFinder fileFinder, Messages messages, VarResolver varResolver) throws Exception {

		SwaggerProvider x = builder.swaggerProvider.value().orElse(null);

		if (resource instanceof SwaggerProvider)
			x = (SwaggerProvider)resource;

		if (x == null)
			x = beanStore.getBean(SwaggerProvider.class).orElse(null);

		if (x == null)
			x = createSwaggerProviderBuilder(resource, builder, beanStore, fileFinder, messages, varResolver).build();

		x = BeanStore
			.of(beanStore, resource)
			.addBean(SwaggerProvider.class, x)
			.beanCreateMethodFinder(SwaggerProvider.class, resource)
			.find("createSwaggerProvider")
			.withDefault(x)
			.run();

		return x;
	}

	/**
	 * Instantiates the REST API builder for this REST resource.
	 *
	 * <p>
	 * Allows subclasses to intercept and modify the builder used by the {@link #createSwaggerProvider(Object,RestContextBuilder,BeanStore,FileFinder,Messages,VarResolver)} method.
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param builder
	 * 	The builder for this object.
	 * 	<br>Consists of all properties gathered through the builder and annotations on this class and all parent classes.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link RestContextBuilder#beanStore()}.
	 * @param fileFinder The file finder configured on this bean created by {@link RestContextBuilder#createFileFinder(BeanStore,Supplier)}.
	 * @param messages The localized messages configured on this bean.
	 * @param varResolver The variable resolver configured on this bean.
	 * @return The REST API builder for this REST resource.
	 * @throws Exception If REST API builder could not be instantiated.
	 */
	protected SwaggerProviderBuilder createSwaggerProviderBuilder(Object resource, RestContextBuilder builder, BeanStore beanStore, FileFinder fileFinder, Messages messages, VarResolver varResolver) throws Exception {

		Class<? extends SwaggerProvider> c = builder.swaggerProvider.type().orElse(null);

		SwaggerProviderBuilder x = SwaggerProvider
				.create()
				.beanStore(beanStore)
				.fileFinder(fileFinder)
				.messages(messages)
				.varResolver(varResolver)
				.jsonSchemaGenerator(beanStore.getBean(JsonSchemaGenerator.class).get())
				.implClass(c);

		x = BeanStore
			.of(beanStore, resource)
			.addBean(SwaggerProviderBuilder.class, x)
			.beanCreateMethodFinder(SwaggerProviderBuilder.class, resource)
			.find("createSwaggerProviderBuilder")
			.withDefault(x)
			.run();

		return x;

	}

	/**
	 * Instantiates the debug enablement bean for this REST object.
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param builder
	 * 	The builder for this object.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link RestContextBuilder#beanStore()}.
	 * @return The debug enablement bean for this REST object.
	 * @throws Exception If bean could not be created.
	 */
	protected DebugEnablement createDebugEnablement(Object resource, RestContextBuilder builder, BeanStore beanStore) throws Exception {
		DebugEnablement x = null;

		if (resource instanceof DebugEnablement)
			x = (DebugEnablement)resource;

		if (x == null)
			x = builder.debugEnablement.value().orElse(null);

		if (x == null)
			x = beanStore.getBean(DebugEnablement.class).orElse(null);

		if (x == null)
			x = createDebugEnablementBuilder(resource, builder, beanStore).build();

		x = BeanStore
			.of(beanStore, resource)
			.addBean(DebugEnablement.class, x)
			.beanCreateMethodFinder(DebugEnablement.class, resource)
			.find("createDebugEnablement")
			.withDefault(x)
			.run();

		return x;
	}

	/**
	 * Instantiates the debug enablement bean builder for this REST object.
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param builder
	 * 	The builder for this object.
	 * 	<br>Consists of all properties gathered through the builder and annotations on this class and all parent classes.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link RestContextBuilder#beanStore()}.
	 * @return The debug enablement bean builder for this REST object.
	 * @throws Exception If bean builder could not be created.
	 */
	protected DebugEnablementBuilder createDebugEnablementBuilder(Object resource, RestContextBuilder builder, BeanStore beanStore) throws Exception {

		Class<? extends DebugEnablement> c = builder.debugEnablement.type().orElse(null);

		DebugEnablementBuilder x = DebugEnablement
			.create()
			.beanStore(beanStore)
			.implClass(c);

		x = BeanStore
			.of(beanStore, resource)
			.addBean(DebugEnablementBuilder.class, x)
			.beanCreateMethodFinder(DebugEnablementBuilder.class, resource)
			.find("createDebugEnablementBuilder")
			.withDefault(x)
			.run();

		Enablement defaultDebug = builder.debug;

		if (defaultDebug == null)
			defaultDebug = builder.debugDefault;

		if (defaultDebug == null)
			defaultDebug = isDebug() ? Enablement.ALWAYS : Enablement.NEVER;

		x.defaultEnable(defaultDebug);

		for (Map.Entry<String,String> e : splitMap(ofNullable(builder.debugOn).orElse(""), true).entrySet()) {
			String k = e.getKey(), v = e.getValue();
			if (v.isEmpty())
				v = "ALWAYS";
			if (! k.isEmpty())
				x.enable(Enablement.fromString(v), k);
		}

		for (MethodInfo mi : ClassInfo.ofProxy(resource).getPublicMethods()) {
			Optional<String> o = mi.getAnnotationGroupList(RestOp.class).getValues(String.class, "debug").stream().filter(y->!y.isEmpty()).findFirst();
			if (o.isPresent())
				x.enable(Enablement.fromString(o.get()), mi.getFullName());
		}

		return x;
	}

	/**
	 * Creates the set of {@link RestOpContext} objects that represent the methods on this resource.
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param builder
	 * 	The builder for this object.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link RestContextBuilder#beanStore()}.
	 * @return The builder for the {@link RestOperations} object.
	 * @throws Exception An error occurred.
	 */
	protected RestOperations createRestOperations(Object resource, RestContextBuilder builder, BeanStore beanStore) throws Exception {

		RestOperations x = createRestOperationsBuilder(resource, builder, beanStore).build();

		x = BeanStore
			.of(beanStore, resource)
			.addBean(RestOperations.class, x)
			.beanCreateMethodFinder(RestOperations.class, resource)
			.find("createRestOperations")
			.withDefault(x)
			.run();

		return x;
	}

	/**
	 * Instantiates the REST methods builder for this REST resource.
	 *
	 * <p>
	 * Allows subclasses to intercept and modify the builder used by the {@link #createRestOperations(Object,RestContextBuilder,BeanStore)} method.
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param builder
	 * 	The builder for this object.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link RestContextBuilder#beanStore()}.
	 * @return The REST methods builder for this REST resource.
	 * @throws Exception If REST methods builder could not be instantiated.
	 */
	protected RestOperationsBuilder createRestOperationsBuilder(Object resource, RestContextBuilder builder, BeanStore beanStore) throws Exception {

		RestOperationsBuilder x = RestOperations
			.create()
			.beanStore(beanStore)
			.implClass(builder.operationsClass);

		ClassInfo rci = ClassInfo.of(resource);

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
						.create(mi.inner(), this)
						.beanStore(beanStore)
						.type(builder.opContextClass)
						.build();

					String httpMethod = roc.getHttpMethod();

					// RRPC is a special case where a method returns an interface that we
					// can perform REST calls against.
					// We override the CallMethod.invoke() method to insert our logic.
					if ("RRPC".equals(httpMethod)) {

						RestOpContext roc2 = RestOpContext
							.create(mi.inner(), this)
							.dotAll()
							.beanStore(rootBeanStore)
							.type(RrpcRestOpContext.class)
							.build();
						x
							.add("GET", roc2)
							.add("POST", roc2);

					} else {
						x.add(roc);
					}
				} catch (Throwable e) {
					throw new RestServletException(e, "Problem occurred trying to initialize methods on class {0}", rci.inner().getName());
				}
			}
		}

		x = BeanStore
			.of(beanStore, resource)
			.addBean(RestOperationsBuilder.class, x)
			.beanCreateMethodFinder(RestOperationsBuilder.class, resource)
			.find("createRestOperationsBuilder")
			.withDefault(x)
			.run();

		return x;
	}

	/**
	 * Creates the builder for the children of this resource.
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param builder
	 * 	The builder for this object.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link RestContextBuilder#beanStore()}.
	 * @param servletConfig
	 * 	The servlet config passed into the servlet by the servlet container.
	 * @return The builder for the {@link RestChildren} object.
	 * @throws Exception An error occurred.
	 */
	protected RestChildren createRestChildren(Object resource, RestContextBuilder builder, BeanStore beanStore, ServletConfig servletConfig) throws Exception {

		RestChildren x = createRestChildrenBuilder(resource, builder, beanStore, servletConfig).build();

		x = BeanStore
			.of(beanStore, resource)
			.addBean(RestChildren.class, x)
			.beanCreateMethodFinder(RestChildren.class, resource)
			.find("createRestChildren")
			.withDefault(x)
			.run();

		return x;
	}

	/**
	 * Instantiates the REST children builder for this REST resource.
	 *
	 * <p>
	 * Allows subclasses to intercept and modify the builder used by the {@link #createRestChildren(Object,RestContextBuilder,BeanStore,ServletConfig)} method.
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param builder
	 * 	The builder for this object.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link RestContextBuilder#beanStore()}.
	 * @param servletConfig
	 * 	The servlet config passed into the servlet by the servlet container.
	 * @return The REST children builder for this REST resource.
	 * @throws Exception If REST children builder could not be instantiated.
	 */
	protected RestChildrenBuilder createRestChildrenBuilder(Object resource, RestContextBuilder builder, BeanStore beanStore, ServletConfig servletConfig) throws Exception {

		RestChildrenBuilder x = RestChildren
			.create()
			.beanStore(beanStore)
			.implClass(builder.childrenClass);

		// Initialize our child resources.
		for (Object o : builder.children) {
			String path = null;

			if (o instanceof RestChild) {
				RestChild rc = (RestChild)o;
				path = rc.path;
				o = rc.resource;
			}

			RestContextBuilder cb = null;

			if (o instanceof Class) {
				Class<?> oc = (Class<?>)o;
				// Don't allow specifying yourself as a child.  Causes an infinite loop.
				if (oc == builder.resourceClass)
					continue;
				cb = RestContext.create(oc, this, servletConfig);
				BeanStore bf = BeanStore.of(beanStore, resource).addBean(RestContextBuilder.class, cb);
				if (bf.getBean(oc).isPresent()) {
					o = (Supplier<?>)()->bf.getBean(oc).get();  // If we resolved via injection, always get it this way.
				} else {
					o = bf.createBean(oc);
				}
			} else {
				cb = RestContext.create(o.getClass(), this, servletConfig);
			}

			if (path != null)
				cb.path(path);

			RestContext cc = cb.init(o).build();

			MethodInfo mi = ClassInfo.of(o).getMethod("setContext", RestContext.class);
			if (mi != null)
				mi.accessible().invoke(o, cc);

			x.add(cc);
		}

		x = BeanStore
			.of(beanStore, resource)
			.addBean(RestChildrenBuilder.class, x)
			.beanCreateMethodFinder(RestChildrenBuilder.class, resource)
			.find("createRestChildrenBuilder")
			.withDefault(x)
			.run();

		return x;
	}

	/**
	 * Instantiates the list of {@link HookEvent#START_CALL} methods.
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param builder
	 * 	The builder for this object.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link RestContextBuilder#beanStore()}.
	 * @return The default response headers for this REST object.
	 * @throws Exception If list could not be instantiated.
	 */
	protected MethodList createStartCallMethods(Object resource, RestContextBuilder builder, BeanStore beanStore) throws Exception {
		Map<String,Method> x = AMap.create();

		for (MethodInfo m : ClassInfo.ofProxy(resource).getAllMethodsParentFirst())
			for (RestHook h : m.getAnnotations(RestHook.class))
				if (h.value() == HookEvent.START_CALL)
					x.put(m.getSignature(), m.accessible().inner());

		MethodList x2 = MethodList.of(x.values());

		x2 = BeanStore
			.of(beanStore, resource)
			.addBean(MethodList.class, x2)
			.beanCreateMethodFinder(MethodList.class, resource)
			.find("createStartCallMethods")
			.withDefault(x2)
			.run();

		return x2;
	}

	/**
	 * Instantiates the list of {@link HookEvent#END_CALL} methods.
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param builder
	 * 	The builder for this object.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link RestContextBuilder#beanStore()}.
	 * @return The default response headers for this REST object.
	 * @throws Exception If list could not be instantiated.
	 */
	protected MethodList createEndCallMethods(Object resource, RestContextBuilder builder, BeanStore beanStore) throws Exception {
		Map<String,Method> x = AMap.create();

		for (MethodInfo m : ClassInfo.ofProxy(resource).getAllMethodsParentFirst())
			for (RestHook h : m.getAnnotations(RestHook.class))
				if (h.value() == HookEvent.END_CALL)
					x.put(m.getSignature(), m.accessible().inner());

		MethodList x2 = MethodList.of(x.values());

		x2 = BeanStore
			.of(beanStore, resource)
			.addBean(MethodList.class, x2)
			.beanCreateMethodFinder(MethodList.class, resource)
			.find("createEndCallMethods")
			.withDefault(x2)
			.run();

		return x2;
	}

	/**
	 * Instantiates the list of {@link HookEvent#POST_INIT} methods.
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param builder
	 * 	The builder for this object.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link RestContextBuilder#beanStore()}.
	 * @return The default response headers for this REST object.
	 * @throws Exception If list could not be instantiated.
	 */
	protected MethodList createPostInitMethods(Object resource, RestContextBuilder builder, BeanStore beanStore) throws Exception {
		Map<String,Method> x = AMap.create();

		for (MethodInfo m : ClassInfo.ofProxy(resource).getAllMethodsParentFirst())
			for (RestHook h : m.getAnnotations(RestHook.class))
				if (h.value() == HookEvent.POST_INIT)
					x.put(m.getSignature(), m.accessible().inner());

		MethodList x2 = MethodList.of(x.values());

		x2 = BeanStore
			.of(beanStore, resource)
			.addBean(MethodList.class, x2)
			.beanCreateMethodFinder(MethodList.class, resource)
			.find("createPostInitMethods")
			.withDefault(x2)
			.run();

		return x2;
	}

	/**
	 * Instantiates the list of {@link HookEvent#POST_INIT_CHILD_FIRST} methods.
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param builder
	 * 	The builder for this object.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link RestContextBuilder#beanStore()}.
	 * @return The default response headers for this REST object.
	 * @throws Exception If list could not be instantiated.
	 */
	protected MethodList createPostInitChildFirstMethods(Object resource, RestContextBuilder builder, BeanStore beanStore) throws Exception {
		Map<String,Method> x = AMap.create();

		for (MethodInfo m : ClassInfo.ofProxy(resource).getAllMethodsParentFirst())
			for (RestHook h : m.getAnnotations(RestHook.class))
				if (h.value() == HookEvent.POST_INIT_CHILD_FIRST)
					x.put(m.getSignature(), m.accessible().inner());

		MethodList x2 = MethodList.of(x.values());

		x2 = BeanStore
			.of(beanStore, resource)
			.addBean(MethodList.class, x2)
			.beanCreateMethodFinder(MethodList.class, resource)
			.find("createPostInitChildFirstMethods")
			.withDefault(x2)
			.run();

		return x2;
	}

	/**
	 * Instantiates the list of {@link HookEvent#DESTROY} methods.
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param builder
	 * 	The builder for this object.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link RestContextBuilder#beanStore()}.
	 * @return The default response headers for this REST object.
	 * @throws Exception If list could not be instantiated.
	 */
	protected MethodList createDestroyMethods(Object resource, RestContextBuilder builder, BeanStore beanStore) throws Exception {
		Map<String,Method> x = AMap.create();

		for (MethodInfo m : ClassInfo.ofProxy(resource).getAllMethodsParentFirst())
			for (RestHook h : m.getAnnotations(RestHook.class))
				if (h.value() == HookEvent.DESTROY)
					x.put(m.getSignature(), m.accessible().inner());

		MethodList x2 = MethodList.of(x.values());

		x2 = BeanStore
			.of(beanStore, resource)
			.addBean(MethodList.class, x2)
			.beanCreateMethodFinder(MethodList.class, resource)
			.find("createDestroyMethods")
			.withDefault(x2)
			.run();

		return x2;
	}

	/**
	 * Instantiates the list of {@link HookEvent#PRE_CALL} methods.
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param builder
	 * 	The builder for this object.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link RestContextBuilder#beanStore()}.
	 * @return The default response headers for this REST object.
	 * @throws Exception If list could not be instantiated.
	 */
	protected MethodList createPreCallMethods(Object resource, RestContextBuilder builder, BeanStore beanStore) throws Exception {
		Map<String,Method> x = AMap.create();

		for (MethodInfo m : ClassInfo.ofProxy(resource).getAllMethodsParentFirst())
			for (RestHook h : m.getAnnotations(RestHook.class))
				if (h.value() == HookEvent.PRE_CALL)
					x.put(m.getSignature(), m.accessible().inner());

		MethodList x2 = MethodList.of(x.values());

		x2 = BeanStore
			.of(beanStore, resource)
			.addBean(MethodList.class, x2)
			.beanCreateMethodFinder(MethodList.class, resource)
			.find("createPreCallMethods")
			.withDefault(x2)
			.run();

		return x2;
	}

	/**
	 * Instantiates the list of {@link HookEvent#POST_CALL} methods.
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param builder
	 * 	The builder for this object.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link RestContextBuilder#beanStore()}.
	 * @return The default response headers for this REST object.
	 * @throws Exception If list could not be instantiated.
	 */
	protected MethodList createPostCallMethods(Object resource, RestContextBuilder builder, BeanStore beanStore) throws Exception {
		Map<String,Method> x = AMap.create();

		for (MethodInfo m : ClassInfo.ofProxy(resource).getAllMethodsParentFirst())
			for (RestHook h : m.getAnnotations(RestHook.class))
				if (h.value() == HookEvent.POST_CALL)
					x.put(m.getSignature(), m.accessible().inner());

		MethodList x2 = MethodList.of(x.values());

		x2 = BeanStore
			.of(beanStore, resource)
			.addBean(MethodList.class, x2)
			.beanCreateMethodFinder(MethodList.class, resource)
			.find("createPostCallMethods")
			.withDefault(x2)
			.run();

		return x2;
	}

	/**
	 * Returns the bean store associated with this context.
	 *
	 * <p>
	 * The bean store is used for instantiating child resource classes.
	 *
	 * @return The resource resolver associated with this context.
	 */
	protected BeanStore getBeanStore() {
		return beanStore;
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
	 * 	<li class='link'>{@doc RestSvlVariables}
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
	 * 	<li class='jm'>{@link RestContextBuilder#config(Config)}
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
	 * {@link RestContextBuilder#path(String)} method.
	 *
	 * <p>
	 * If path is not specified, returns <js>""</js>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContextBuilder#path(String)}
	 * </ul>
	 *
	 * @return The servlet path.
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Returns the path for this resource as defined by the {@link Rest#path() @Rest(path)} annotation or
	 * {@link RestContextBuilder#path(String)} method concatenated with those on all parent classes.
	 *
	 * <p>
	 * If path is not specified, returns <js>""</js>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContextBuilder#path(String)}
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
	 * 	<li class='jm'>{@link RestContextBuilder#callLogger()}
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
	 * 	<li class='jm'>{@link RestContextBuilder#swaggerProvider(Class)}
	 * 	<li class='jm'>{@link RestContextBuilder#swaggerProvider(SwaggerProvider)}
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
	public Map<String,RestContext> getChildResources() {
		return restChildren.asMap();
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
	 * 	<li class='jf'>{@link RestContextBuilder#disableBodyParam()}
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
	 * 	<li class='jm'>{@link RestContextBuilder#allowedHeaderParams(String)}
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
	 * 	<li class='jm'>{@link RestContextBuilder#allowedMethodHeaders(String)}
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
	 * 	<li class='jm'>{@link RestContextBuilder#allowedMethodParams(String)}
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
	 * 	<li class='jm'>{@link RestContextBuilder#clientVersionHeader(String)}
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
	 * Can be overridden by {@link RestContextBuilder#produces(MediaType...)}.
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
	 * Can be overridden by {@link RestContextBuilder#consumes(MediaType...)}.
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
	 * 	<li class='jm'>{@link RestContextBuilder#defaultRequestHeaders(org.apache.http.Header...)}
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
	 * 	<li class='jm'>{@link RestContextBuilder#defaultRequestAttributes(NamedAttribute...)}
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
	 * 	<li class='jm'>{@link RestContextBuilder#defaultResponseHeaders(org.apache.http.Header...)}
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
	 * 	<li class='jm'>{@link RestContextBuilder#uriAuthority(String)}
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
	 * 	<li class='jm'>{@link RestContextBuilder#uriContext(String)}
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
	 * 	<li class='jm'>{@link RestContextBuilder#uriRelativity(UriRelativity)}
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
	 * 	<li class='jm'>{@link RestContextBuilder#uriResolution(UriResolution)}
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
	public List<RestOpContext> getOpContexts() {
		return restOperations.getOpContexts();
	}

	/**
	 * Returns timing information on all method executions on this class.
	 *
	 * <p>
	 * Timing information is maintained for any <ja>@RestResource</ja>-annotated and hook methods.
	 *
	 * @return A list of timing statistics ordered by average execution time descending.
	 */
	public List<MethodExecStats> getMethodExecStats() {
		return methodExecStore.getStats().stream().sorted(Comparator.comparingLong(MethodExecStats::getTotalTime).reversed()).collect(Collectors.toList());
	}

	/**
	 * Gives access to the internal stack trace database.
	 *
	 * @return The stack trace database.
	 */
	public RestContextStats getStats() {
		return new RestContextStats(startTime, getMethodExecStats());
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
	 * Returns the timing information returned by {@link #getMethodExecStats()} in a readable format.
	 *
	 * @return A report of all method execution times ordered by .
	 */
	public String getMethodExecStatsReport() {
		StringBuilder sb = new StringBuilder()
			.append(" Method                         Runs      Running   Errors   Avg          Total     \n")
			.append("------------------------------ --------- --------- -------- ------------ -----------\n");
		getMethodExecStats()
			.stream()
			.sorted(Comparator.comparingDouble(MethodExecStats::getTotalTime).reversed())
			.forEach(x -> sb.append(String.format("%30s %9d %9d %9d %10dms %10dms\n", x.getMethod(), x.getRuns(), x.getRunning(), x.getErrors(), x.getAvgTime(), x.getTotalTime())));
		return sb.toString();
	}

	/**
	 * Finds the {@link RestOpArg} instances to handle resolving objects on the calls to the specified Java method.
	 *
	 * @param m The Java method being called.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link RestContextBuilder#beanStore()}.
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
					ra[i] = beanStore.createBean(c);
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
	 * Finds the {@link RestOpArg} instances to handle resolving objects on pre-call and post-call Java methods.
	 *
	 * @param m The Java method being called.
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * 	<br>Created by {@link RestContextBuilder#beanStore()}.
	 * @return The array of resolvers.
	 */
	protected RestOpArg[] findHookMethodArgs(Method m, BeanStore beanStore) {
		MethodInfo mi = MethodInfo.of(m);
		List<ClassInfo> pt = mi.getParamTypes();
		RestOpArg[] ra = new RestOpArg[pt.size()];

		beanStore = BeanStore.of(beanStore, getResource());

		for (int i = 0; i < pt.size(); i++) {
			ParamInfo pi = mi.getParam(i);
			beanStore.addBean(ParamInfo.class, pi);
			for (Class<? extends RestOpArg> c : hookMethodArgs) {
				try {
					ra[i] = beanStore.createBean(c);
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

	//------------------------------------------------------------------------------------------------------------------
	// Call handling
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Wraps an incoming servlet request/response pair into a single {@link RestCall} object.
	 *
	 * <p>
	 * This is the first method called by {@link #execute(Object, HttpServletRequest, HttpServletResponse)}.
	 *
	 * @param resource
	 * 	The REST servlet or bean that this context defines.
	 * @param req The rest request.
	 * @param res The rest response.
	 * @return The wrapped request/response pair.
	 */
	protected RestCall createCall(Object resource, HttpServletRequest req, HttpServletResponse res) {
		return new RestCall(resource, this, req, res).logger(getCallLogger());
	}

	/**
	 * Creates a {@link RestRequest} object based on the specified incoming {@link HttpServletRequest} object.
	 *
	 * <p>
	 * This method is called immediately after {@link #startCall(RestCall)} has been called.
	 *
	 * @param call The current REST call.
	 * @return The wrapped request object.
	 * @throws Exception If any errors occur trying to interpret the request.
	 */
	public RestRequest createRequest(RestCall call) throws Exception {
		return new RestRequest(call);
	}

	/**
	 * Creates a {@link RestResponse} object based on the specified incoming {@link HttpServletResponse} object
	 * and the request returned by {@link #createRequest(RestCall)}.
	 *
	 * @param call The current REST call.
	 * @return The wrapped response object.
	 * @throws Exception If any errors occur trying to interpret the request or response.
	 */
	public RestResponse createResponse(RestCall call) throws Exception {
		return new RestResponse(call);
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

		RestCall call = createCall(resource, r1, r2);

		// Must be careful not to bleed thread-locals.
		if (this.call.get() != null)
			System.err.println("WARNING:  Thread-local call object was not cleaned up from previous request.  " + this + ", thread=["+Thread.currentThread().getId()+"]");
		this.call.set(call);

		try {

			if (initException != null)
				throw initException;

			// If the resource path contains variables (e.g. @Rest(path="/f/{a}/{b}"), then we want to resolve
			// those variables and push the servletPath to include the resolved variables.  The new pathInfo will be
			// the remainder after the new servletPath.
			// Only do this for the top-level resource because the logic for child resources are processed next.
			if (pathMatcher.hasVars() && parentContext == null) {
				String sp = call.getServletPath();
				String pi = call.getPathInfoUndecoded();
				UrlPath upi2 = UrlPath.of(pi == null ? sp : sp + pi);
				UrlPathMatch uppm = pathMatcher.match(upi2);
				if (uppm != null && ! uppm.hasEmptyVars()) {
					call.pathVars(uppm.getVars());
					call.request(
						new OverrideableHttpServletRequest(call.getRequest())
							.pathInfo(nullIfEmpty(urlDecode(uppm.getSuffix())))
							.servletPath(uppm.getPrefix())
					);
				} else {
					call.debug(isDebug(call)).status(SC_NOT_FOUND).finish();
					return;
				}
			}

			// If this resource has child resources, try to recursively call them.
			Optional<RestChildMatch> childMatch = restChildren.findMatch(call);
			if (childMatch.isPresent()) {
				UrlPathMatch uppm = childMatch.get().getPathMatch();
				RestContext rc = childMatch.get().getChildContext();
				if (! uppm.hasEmptyVars()) {
					call.pathVars(uppm.getVars());
					HttpServletRequest childRequest = new OverrideableHttpServletRequest(call.getRequest())
						.pathInfo(nullIfEmpty(urlDecode(uppm.getSuffix())))
						.servletPath(call.getServletPath() + uppm.getPrefix());
					rc.execute(rc.getResource(), childRequest, call.getResponse());
				} else {
					call.debug(isDebug(call)).status(SC_NOT_FOUND).finish();
				}
				return;
			}

			call.debug(isDebug(call));

			startCall(call);

			// If the specified method has been defined in a subclass, invoke it.
			try {
				restOperations.findOperation(call).invoke(call);
			} catch (NotFound e) {
				if (call.getStatus() == 0)
					call.status(404);
				call.exception(e);
				handleNotFound(call);
			}

			if (call.hasOutput()) {
				// Now serialize the output if there was any.
				// Some subclasses may write to the OutputStream or Writer directly.
				processResponse(call);
			}


		} catch (Throwable e) {
			handleError(call, convertThrowable(e));
		} finally {
			clearState();
		}

		call.finish();
		finishCall(call);
	}

	private boolean isDebug(RestCall call) {
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
	 * looking for the first one whose {@link ResponseProcessor#process(RestCall)} method returns
	 * <jk>true</jk>.
	 *
	 * @param call The HTTP call.
	 * @throws IOException Thrown by underlying stream.
	 * @throws BasicHttpException Non-200 response.
	 * @throws NotImplemented No registered response processors could handle the call.
	 */
	public void processResponse(RestCall call) throws IOException, BasicHttpException, NotImplemented {

		// Loop until we find the correct processor for the POJO.
		int loops = 5;
		for (int i = 0; i < responseProcessors.length; i++) {
			int j = responseProcessors[i].process(call);
			if (j == FINISHED)
				return;
			if (j == RESTART) {
				if (loops-- < 0)
					throw new InternalServerError("Too many processing loops.");
				i = -1;  // Start over.
			}
		}

		Object output = call.getRestResponse().getOutput().get().orElse(null);
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
	 * @return The converted thrown object.
	 */
	public Throwable convertThrowable(Throwable t) {

		ClassInfo ci = ClassInfo.ofc(t);
		if (ci.is(InvocationTargetException.class)) {
			t = ((InvocationTargetException)t).getTargetException();
			ci = ClassInfo.ofc(t);
		}

		if (ci.is(HttpRuntimeException.class)) {
			t = ((HttpRuntimeException)t).getInner();
			ci = ClassInfo.ofc(t);
		}

		if (ci.hasAnnotation(Response.class))
			return t;

		if (t instanceof ParseException || t instanceof InvalidDataConversionException)
			return new BadRequest(t);

		String n = className(t);

		if (n.contains("AccessDenied") || n.contains("Unauthorized"))
			return new Unauthorized(t);

		if (n.contains("Empty") || n.contains("NotFound"))
			return new NotFound(t);

		return t;
	}

	/**
	 * Handle the case where a matching method was not found.
	 *
	 * <p>
	 * Subclasses can override this method to provide a 2nd-chance for specifying a response.
	 * The default implementation will simply throw an exception with an appropriate message.
	 *
	 * @param call The HTTP call.
	 * @throws Exception Any exception can be thrown.
	 */
	public void handleNotFound(RestCall call) throws Exception {
		String pathInfo = call.getPathInfo();
		String methodUC = call.getMethod();
		int rc = call.getStatus();
		String onPath = pathInfo == null ? " on no pathInfo"  : String.format(" on path '%s'", pathInfo);
		if (rc == SC_NOT_FOUND)
			throw new NotFound("Method ''{0}'' not found on resource with matching pattern{1}.", methodUC, onPath);
		else if (rc == SC_PRECONDITION_FAILED)
			throw new PreconditionFailed("Method ''{0}'' not found on resource{1} with matching matcher.", methodUC, onPath);
		else if (rc == SC_METHOD_NOT_ALLOWED)
			throw new MethodNotAllowed("Method ''{0}'' not found on resource{1}.", methodUC, onPath);
		else
			throw new ServletException("Invalid method response: " + rc, call.getException());
	}

	/**
	 * Method for handling response errors.
	 *
	 * <p>
	 * Subclasses can override this method to provide their own custom error response handling.
	 *
	 * @param call The rest call.
	 * @param e The exception that occurred.
	 * @throws IOException Can be thrown if a problem occurred trying to write to the output stream.
	 */
	public synchronized void handleError(RestCall call, Throwable e) throws IOException {

		call.exception(e);

		if (call.isDebug())
			e.printStackTrace();

		int code = 500;

		ClassInfo ci = ClassInfo.ofc(e);
		Response r = ci.getLastAnnotation(Response.class);
		if (r != null)
			if (r.code().length > 0)
				code = r.code()[0];

		BasicHttpException e2 = (e instanceof BasicHttpException ? (BasicHttpException)e : BasicHttpException.create(BasicHttpException.class).causedBy(e).statusCode(code).build());

		HttpServletRequest req = call.getRequest();
		HttpServletResponse res = call.getResponse();

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
	 * @param call The current request.
	 * @throws BasicHttpException If thrown from call methods.
	 */
	protected void startCall(RestCall call) throws BasicHttpException {
		for (MethodInvoker x : startCallMethods) {
			try {
				x.invokeUsingFactory(call.getBeanStore(), call.getContext().getResource());
			} catch (ExecutableException e) {
				throw toHttpException(e.unwrap(), InternalServerError.class);
			}
		}
	}

	/**
	 * Called during a request to invoke all {@link HookEvent#PRE_CALL} methods.
	 *
	 * @param call The current request.
	 * @throws BasicHttpException If thrown from call methods.
	 */
	protected void preCall(RestCall call) throws BasicHttpException {
		for (RestOpInvoker m : preCallMethods)
			m.invokeFromCall(call, getResource());
	}

	/**
	 * Called during a request to invoke all {@link HookEvent#POST_CALL} methods.
	 *
	 * @param call The current request.
	 * @throws BasicHttpException If thrown from call methods.
	 */
	protected void postCall(RestCall call) throws BasicHttpException {
		for (RestOpInvoker m : postCallMethods)
			m.invokeFromCall(call, getResource());
	}

	/**
	 * Called at the end of a request to invoke all {@link HookEvent#END_CALL} methods.
	 *
	 * <p>
	 * This is the very last method called in {@link #execute(Object, HttpServletRequest, HttpServletResponse)}.
	 *
	 * @param call The current request.
	 */
	protected void finishCall(RestCall call) {
		for (MethodInvoker x : endCallMethods) {
			try {
				x.invokeUsingFactory(call.getBeanStore(), call.getResource());
			} catch (ExecutableException e) {
				logger.log(Level.WARNING, e.unwrap(), ()->format("Error occurred invoking finish-call method ''{0}''.", x.getFullName()));
			}
		}
	}

	/**
	 * Called during servlet initialization to invoke all {@link HookEvent#POST_INIT} methods.
	 *
	 * @return This object (for method chaining).
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
				x.invokeUsingFactory(beanStore, getResource());
			} catch (ExecutableException e) {
				throw new ServletException(e.unwrap());
			}
		}
		restChildren.postInit();
		return this;
	}

	/**
	 * Called during servlet initialization to invoke all {@link HookEvent#POST_INIT_CHILD_FIRST} methods.
	 *
	 * @return This object (for method chaining).
	 * @throws ServletException Error occurred.
	 */
	public RestContext postInitChildFirst() throws ServletException {
		if (initialized.get())
			return this;
		restChildren.postInitChildFirst();
		for (MethodInvoker x : postInitChildFirstMethods) {
			try {
				x.invokeUsingFactory(beanStore, getResource());
			} catch (ExecutableException e) {
				throw new ServletException(e.unwrap());
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
				x.invokeUsingFactory(beanStore, getResource());
			} catch (ExecutableException e) {
				getLogger().log(Level.WARNING, e.unwrap(), ()->format("Error occurred invoking servlet-destroy method ''{0}''.", x.getFullName()));
			}
		}

		restChildren.destroy();
	}

	/**
	 * Returns the HTTP request object for the current request.
	 *
	 * @return The HTTP request object, or <jk>null</jk> if it hasn't been created.
	 */
	public RestRequest getRequest() {
		return getCall().getRestRequest();
	}

	/**
	 * Returns the HTTP response object for the current request.
	 *
	 * @return The HTTP response object, or <jk>null</jk> if it hasn't been created.
	 */
	public RestResponse getResponse() {
		return getCall().getRestResponse();
	}

	/**
	 * Returns the HTTP call for the current request.
	 *
	 * @return The HTTP call for the current request, never <jk>null</jk>?
	 * @throws InternalServerError If no active request exists on the current thread.
	 */
	public RestCall getCall() {
		RestCall rc = call.get();
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

	/**
	 * Clear any request state information on this context.
	 * This should always be called in a finally block in the RestServlet.
	 */
	void clearState() {
		call.remove();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods.
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public OMap toMap() {
		return super.toMap()
			.a(
				"RestContext",
				OMap
					.create()
					.filtered()
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
					.a("uriResolution", uriResolution)
			);
	}

	@Override
	public ContextBuilder copy() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Session createSession(SessionArgs args) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SessionArgs createDefaultSessionArgs() {
		// TODO Auto-generated method stub
		return null;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helpers.
	//-----------------------------------------------------------------------------------------------------------------

}
