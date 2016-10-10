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
package org.apache.juneau.server;

import static java.lang.String.*;
import static java.util.logging.Level.*;
import static javax.servlet.http.HttpServletResponse.*;
import static org.apache.juneau.internal.ArrayUtils.*;
import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.serializer.SerializerContext.*;
import static org.apache.juneau.server.RestServlet.ParamType.*;
import static org.apache.juneau.server.RestServletContext.*;
import static org.apache.juneau.server.annotation.Inherit.*;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.lang.reflect.Method;
import java.nio.charset.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

import javax.activation.*;
import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.juneau.*;
import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.encoders.Encoder;
import org.apache.juneau.ini.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.parser.ParseException;
import org.apache.juneau.serializer.*;
import org.apache.juneau.server.annotation.*;
import org.apache.juneau.server.annotation.Parameter;
import org.apache.juneau.server.annotation.Properties;
import org.apache.juneau.server.response.*;
import org.apache.juneau.server.vars.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.svl.vars.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.utils.*;

/**
 * Servlet implementation of a REST resource.
 * <p>
 * 	Refer to <a class='doclink' href='package-summary.html#TOC'>REST Servlet API</a> for information about using this class.
 * </p>
 *
 * @author jbognar
 */
@SuppressWarnings({"rawtypes","hiding"})
public abstract class RestServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	static final SortedMap<String,Charset> availableCharsets = new TreeMap<String,Charset>(String.CASE_INSENSITIVE_ORDER);
	static {
		availableCharsets.putAll(Charset.availableCharsets());
	}
	// Map of HTTP method names (e.g. GET/PUT/...) to ResourceMethod implementations for it.  Populated during resource initialization.
	private final Map<String,ResourceMethod> restMethods = new LinkedHashMap<String,ResourceMethod>();

	// The list of all @RestMethod annotated methods in the order they appear in the class.
	private final Map<String,MethodMeta> javaRestMethods = new LinkedHashMap<String,MethodMeta>();

	// Child resources of this resource defined through getX() methods on this class.
	private final Map<String,RestServlet> childResources = new LinkedHashMap<String,RestServlet>();

	private RestServlet parentResource;

	private ServletConfig servletConfig;
	private volatile boolean isInitialized = false;
	private Exception initException;                       // Exception thrown by init() method (cached so it can be thrown on all subsequent requests).
	private JuneauLogger logger;
	private MessageBundle msgs;                           // NLS messages.

	private Map<Integer,Integer> stackTraceHashes = new HashMap<Integer,Integer>();
	private String path;

	private LinkedHashMap<Class<?>,RestResource> restResourceAnnotationsChildFirst, restResourceAnnotationsParentFirst;

	private UrlEncodingSerializer urlEncodingSerializer;
	private UrlEncodingParser urlEncodingParser;
	private ObjectMap properties;
	private RestGuard[] guards;
	private Class<?>[] beanFilters, pojoSwaps;
	private RestConverter[] converters;
	private TreeMap<String,String> defaultRequestHeaders;
	private Map<String,Object> defaultResponseHeaders;
	private EncoderGroup encoders;
	private SerializerGroup serializers;
	private ParserGroup parsers;
	private MimetypesFileTypeMap mimetypesFileTypeMap;
	private BeanContext beanContext;
	private VarResolver varResolver;
	private String title, description, termsOfService, contact, license, version, tags, externalDocs;
	private Map<String,byte[]> resourceStreams = new ConcurrentHashMap<String,byte[]>();
	private Map<String,String> resourceStrings = new ConcurrentHashMap<String,String>();
	private ConfigFile configFile, resolvingConfigFile;
	private String configPath;
	private StreamResource styleSheet, favIcon;
	private Map<String,String> staticFilesMap;
	private String[] staticFilesPrefixes;
	private ResponseHandler[] responseHandlers;
	private String clientVersionHeader = "";
	private ConcurrentHashMap<Locale,Swagger> swaggers = new ConcurrentHashMap<Locale,Swagger>();

	RestServletContext context;

	// In-memory cache of images and stylesheets in the org.apache.juneau.server.htdocs package.
	private Map<String,StreamResource> staticFilesCache = new ConcurrentHashMap<String,StreamResource>();

	// The following code block is executed before the constructor is called to
	// allow the config file to be accessed during object creation.
	// e.g. private String myConfig = getConfig().getString("myConfig");
	{
		varResolver = createVarResolver();

		// @RestResource annotations from bottom to top.
		restResourceAnnotationsChildFirst = ReflectionUtils.findAnnotationsMap(RestResource.class, getClass());

		// @RestResource annotations from top to bottom.
		restResourceAnnotationsParentFirst = CollectionUtils.reverse(restResourceAnnotationsChildFirst);

		for (RestResource r : restResourceAnnotationsParentFirst.values()) {
			if (! r.config().isEmpty())
				configPath = r.config();
		}

		try {
			configFile = createConfigFile();
			varResolver.setContextObject(ConfigFileVar.SESSION_config, configFile);
		} catch (IOException e) {
			this.initException = e;
		}
	}

	@Override /* Servlet */
	public synchronized void init(ServletConfig servletConfig) throws ServletException {
		try {
			log(FINE, "Servlet {0} init called.", getClass().getName());
			this.servletConfig = servletConfig;

			if (isInitialized)
				return;

			super.init(servletConfig);

			// Find resource resource bundle location.
			for (Map.Entry<Class<?>,RestResource> e : restResourceAnnotationsChildFirst.entrySet()) {
				Class<?> c = e.getKey();
				RestResource r = e.getValue();
				if (! r.messages().isEmpty()) {
					if (msgs == null)
						msgs = new MessageBundle(c, r.messages());
					else
						msgs.addSearchPath(c, r.messages());
				}
			}
			for (RestResource r : restResourceAnnotationsParentFirst.values()) {
				if (! r.title().isEmpty())
					title = r.title();
				if (! r.description().isEmpty())
					description = r.description();
				if (! r.clientVersionHeader().isEmpty())
					clientVersionHeader = r.clientVersionHeader();
				if (! r.termsOfService().isEmpty())
					termsOfService = r.termsOfService();
				if (! r.contact().isEmpty())
					contact = r.contact();
				if (! r.license().isEmpty())
					license = r.license();
				if (! r.version().isEmpty())
					version = r.version();
				if (! r.tags().isEmpty())
					tags = r.tags();
				if (! r.externalDocs().isEmpty())
					externalDocs = r.externalDocs();
			}

			if (msgs == null)
				msgs = new MessageBundle(this.getClass(), "");
			if (clientVersionHeader.isEmpty())
				clientVersionHeader = "X-Client-Version";

			styleSheet = createStyleSheet();
			favIcon = createFavIcon();
			staticFilesMap = Collections.unmodifiableMap(createStaticFilesMap());
			staticFilesPrefixes = staticFilesMap.keySet().toArray(new String[0]);

			properties = createProperties();
			beanFilters = createBeanFilters();
			pojoSwaps = createPojoSwaps();
			context = ContextFactory.create().setProperties(properties).getContext(RestServletContext.class);
			beanContext = createBeanContext(properties, beanFilters, pojoSwaps);
			urlEncodingSerializer = createUrlEncodingSerializer(properties, beanFilters, pojoSwaps).lock();
			urlEncodingParser = createUrlEncodingParser(properties, beanFilters, pojoSwaps).lock();
			serializers = createSerializers(properties, beanFilters, pojoSwaps).lock();
			parsers = createParsers(properties, beanFilters, pojoSwaps).lock();
			converters = createConverters(properties);
			encoders = createEncoders(properties);
			guards = createGuards(properties);
			mimetypesFileTypeMap = createMimetypesFileTypeMap(properties);
			defaultRequestHeaders = new TreeMap<String,String>(String.CASE_INSENSITIVE_ORDER);
			defaultRequestHeaders.putAll(createDefaultRequestHeaders(properties));
			defaultResponseHeaders = createDefaultResponseHeaders(properties);
			responseHandlers = createResponseHandlers(properties);

			// Discover the @RestMethod methods available on the resource.
			List<String> methodsFound = new LinkedList<String>();   // Temporary to help debug transient duplicate method issue.
			for (java.lang.reflect.Method method : this.getClass().getMethods()) {
				if (method.isAnnotationPresent(RestMethod.class)) {
					RestMethod a = method.getAnnotation(RestMethod.class);
					methodsFound.add(method.getName() + "," + a.name() + "," + a.path());
					try {
						if (! Modifier.isPublic(method.getModifiers()))
							throw new RestServletException("@RestMethod method {0}.{1} must be defined as public.", this.getClass().getName(), method.getName());

						MethodMeta sm = new MethodMeta(method);
						javaRestMethods.put(method.getName(), sm);
						ResourceMethod rm = restMethods.get(sm.httpMethod);
						if (rm == null)
							restMethods.put(sm.httpMethod, sm);
						else if (rm instanceof MultiMethod)
							((MultiMethod)rm).addSimpleMethod(sm);
						else
							restMethods.put(sm.httpMethod, new MultiMethod((MethodMeta)rm, sm));
					} catch (RestServletException e) {
						throw new RestServletException("Problem occurred trying to serialize methods on class {0}, methods={1}", this.getClass().getName(), JsonSerializer.DEFAULT_LAX.serialize(methodsFound)).initCause(e);
					}
				}
			}

			for (ResourceMethod m : restMethods.values())
				m.complete();

			// Discover the child resources.
			childResources.putAll(createChildrenMap());

			for (RestServlet child : childResources.values())
				child.init(servletConfig);

			varResolver.addVars(
				LocalizationVar.class,
				RequestVar.class,
				SerializedRequestAttrVar.class,
				ServletInitParamVar.class,
				UrlEncodeVar.class
			);

		} catch (RestException e) {
			// Thrown RestExceptions are simply caught and rethrown on subsequent calls to service().
			initException = e;
			log(SEVERE, e, "Servlet init error on class ''{0}''", getClass().getName());
			title = String.valueOf(initException.getLocalizedMessage());
		} catch (ServletException e) {
			initException = e;
			log(SEVERE, e, "Servlet init error on class ''{0}''", getClass().getName());
			title = String.valueOf(initException.getLocalizedMessage());
			throw e;
		} catch (Exception e) {
			initException = e;
			log(SEVERE, e, "Servlet init error on class ''{0}''", getClass().getName());
			title = String.valueOf(initException.getLocalizedMessage());
			throw new ServletException(e);
		} catch (Throwable e) {
			initException = new Exception(e);
			log(SEVERE, e, "Servlet init error on class ''{0}''", getClass().getName());
			title = String.valueOf(initException.getLocalizedMessage());
			throw new ServletException(e);
		} finally {
			isInitialized = true;
		}
	}

	//--------------------------------------------------------------------------------
	// Initialization methods
	//--------------------------------------------------------------------------------

	/**
	 * Creates the child resources of this resource.
	 * <p>
	 * 	Default implementation calls {@link #createChildren()} and uses the {@link RestResource#path() @RestResource.path()} annotation
	 * 		on each child to identify the subpath for the resource which become the keys in this map.
	 * 	It then calls the {@link #setParent(RestServlet)} method on the child resource.
	 * </p>
	 * <p>
	 * 	Subclasses can override this method to programatically create child resources
	 * 		without using the {@link RestResource#children() @RestResource.children()} annotation.
	 * 	When overridding this method, you are responsible for calling {@link #setParent(RestServlet)} on the
	 * 		child resources.
	 * </p>
	 *
	 * @return The new mutable list of child resource instances.
	 * @throws Exception If an error occurred during servlet instantiation.
	 */
	protected Map<String,RestServlet> createChildrenMap() throws Exception {
		Map<String,RestServlet> m = new LinkedHashMap<String,RestServlet>();
		for (RestServlet r : createChildren()) {
			r.setParent(this);
			String p = r.findPath();
			if (p == null)
				throw new RestServletException("Child resource ''{0}'' does not define a ''@RestResource.path'' attribute.", r.getClass().getName());
			m.put(p, r);
		}
		return m;
	}

	/**
	 * Creates instances of child resources for this servlet.
	 * <p>
	 * 	Default implementation uses the {@link RestResource#children() @RestResource.children()} annotation to identify and
	 * 		instantiate children.
	 * </p>
	 * <p>
	 * 	Subclasses can override this method to programatically create child resources
	 * 		without using the {@link RestResource#children() @RestResource.children()} annotation.
	 * </p>
	 *
	 * @return The new mutable list of child resource instances.
	 * @throws Exception If an error occurred during servlet instantiation.
	 */
	protected List<RestServlet> createChildren() throws Exception {
		List<RestServlet> l = new LinkedList<RestServlet>();
		for (Class<?> c : getChildClasses()) {
			if (isParentClass(RestServlet.class, c))
				l.add((RestServlet)c.newInstance());
			else
				l.add(resolveChild(c));
		}
		return l;
	}

	/**
	 * Programmatic equivalent to the {@link RestResource#children() @RestResource.children()} annotation.
	 * <p>
	 * 	Subclasses can override this method to provide customized list of child resources.
	 * 		(e.g. different children based on values specified in the config file).
	 * </p>
	 * <p>
	 * 	Default implementation simply returns the value from the {@link RestResource#children() @RestResource.children()} annotation.
	 * </p>
	 *
	 * @return The new mutable list of child resource instances.
	 * @throws Exception If an error occurred during servlet instantiation.
	 */
	protected Class<?>[] getChildClasses() throws Exception {
		List<Class<?>> l = new ArrayList<Class<?>>();
		List<RestResource> rr = ReflectionUtils.findAnnotations(RestResource.class, getClass());
		for (RestResource r : rr)
			l.addAll(Arrays.asList(r.children()));
		return l.toArray(new Class<?>[l.size()]);
	}

	/**
	 * Creates the class-level properties associated with this servlet.
	 * <p>
	 * 	Subclasses can override this method to provide their own class-level properties for this servlet, typically
	 * 		by calling <code><jk>super</jk>.createProperties()</code> and appending to the map.
	 *	 However, in most cases, the existing set of properties can be added to by overridding {@link #getProperties()}
	 * 		and appending to the map returned by <code><jk>super</jk>.getProperties()</code>
	 * </p>
	 * <p>
	 * 	By default, the map returned by this method contains the following:
	 * </p>
	 * <ul class='spaced-list'>
	 * 	<li>Servlet-init parameters.
	 * 	<li>{@link RestResource#properties()} annotations in parent-to-child order.
	 * 	<li>{@link SerializerContext#SERIALIZER_relativeUriBase} from {@link ServletConfig#getServletContext()}.
	 * </ul>
	 *
	 * @return The resource properties as an {@link ObjectMap}.
	 */
	protected ObjectMap createProperties() {
		ObjectMap m = new ObjectMap();

		ServletContext ctx = servletConfig.getServletContext();

		// Workaround for bug in Jetty that causes context path to always end in "null".
		String ctxPath = ctx.getContextPath();
		if (ctxPath.endsWith("null"))
			ctxPath = ctxPath.substring(0, ctxPath.length()-4);
		m.put(SERIALIZER_relativeUriBase, ctxPath);

		// Get the initialization parameters.
		for (Enumeration ep = servletConfig.getInitParameterNames(); ep.hasMoreElements();) {
			String p = (String)ep.nextElement();
			String initParam = servletConfig.getInitParameter(p);
			m.put(p, initParam);
		}

		// Properties are loaded in parent-to-child order to allow overrides.
		for (RestResource r : restResourceAnnotationsParentFirst.values())
			for (Property p : r.properties())
				m.append(getVarResolver().resolve(p.name()), getVarResolver().resolve(p.value()));

		return m;
	}

	/**
	 * Creates the class-level bean filters associated with this servlet.
	 * <p>
	 * Subclasses can override this method to provide their own class-level bean filters for this servlet.
	 * <p>
	 * By default, returns the bean filters specified through the {@link RestResource#beanFilters() @RestResource.beanFilters()} annotation in child-to-parent order.
	 * 	(i.e. bean filters will be applied in child-to-parent order with child annotations overriding parent annotations when
	 * 	the same filters are applied).
	 *
	 * @return The new set of transforms associated with this servet.
	 */
	protected Class<?>[] createBeanFilters() {
		List<Class<?>> l = new LinkedList<Class<?>>();

		// Bean filters are loaded in parent-to-child order to allow overrides.
		for (RestResource r : restResourceAnnotationsChildFirst.values())
			for (Class c : r.beanFilters())
				l.add(c);

		return l.toArray(new Class<?>[l.size()]);
	}

	/**
	 * Creates the class-level POJO swaps associated with this servlet.
	 * <p>
	 * Subclasses can override this method to provide their own class-level POJO swaps for this servlet.
	 * <p>
	 * By default, returns the transforms specified through the {@link RestResource#pojoSwaps() @RestResource.pojoSwaps()} annotation in child-to-parent order.
	 * 	(i.e. POJO swaps will be applied in child-to-parent order with child annotations overriding parent annotations when
	 * 	the same swaps are applied).
	 *
	 * @return The new set of transforms associated with this servet.
	 */
	protected Class<?>[] createPojoSwaps() {
		List<Class<?>> l = new LinkedList<Class<?>>();

		// Swaps are loaded in parent-to-child order to allow overrides.
		for (RestResource r : restResourceAnnotationsChildFirst.values())
			for (Class c : r.pojoSwaps())
				l.add(c);

		return l.toArray(new Class<?>[l.size()]);
	}

	/**
	 * Creates the {@link BeanContext} object used for parsing path variables and header values.
	 * <p>
	 * Subclasses can override this method to provide their own specialized bean context.
	 *
	 * @param properties Servlet-level properties returned by {@link #createProperties()}.
	 * @param beanFilters Servlet-level bean filters returned by {@link #createBeanFilters()}.
	 * @param pojoSwaps Servlet-level POJO swaps returned by {@link #createPojoSwaps()}.
	 * @return The new bean context.
	 * @throws Exception If bean context not be constructed for any reason.
	 */
	protected BeanContext createBeanContext(ObjectMap properties, Class<?>[] beanFilters, Class<?>[] pojoSwaps) throws Exception {
		return ContextFactory.create().addBeanFilters(beanFilters).addPojoSwaps(pojoSwaps).setProperties(properties).getBeanContext();
	}

	/**
	 * Creates the URL-encoding serializer used for serializing object passed to {@link Redirect}.
	 * <p>
	 * Subclasses can override this method to provide their own specialized serializer.
	 *
	 * @param properties Servlet-level properties returned by {@link #createProperties()}.
	 * @param beanFilters Servlet-level bean filters returned by {@link #createBeanFilters()}.
	 * @param pojoSwaps Servlet-level POJO swaps returned by {@link #createPojoSwaps()}.
	 * @return The new URL-Encoding serializer.
	 * @throws Exception If the serializer could not be constructed for any reason.
	 */
	protected UrlEncodingSerializer createUrlEncodingSerializer(ObjectMap properties, Class<?>[] beanFilters, Class<?>[] pojoSwaps) throws Exception {
		return new UrlEncodingSerializer().setProperties(properties).addBeanFilters(beanFilters).addPojoSwaps(pojoSwaps);
	}

	/**
	 * Creates the URL-encoding parser used for parsing URL query parameters.
	 * <p>
	 * Subclasses can override this method to provide their own specialized parser.
	 *
	 * @param properties Servlet-level properties returned by {@link #createProperties()}.
	 * @param beanFilters Servlet-level bean filters returned by {@link #createBeanFilters()}.
	 * @param pojoSwaps Servlet-level POJO swaps returned by {@link #createPojoSwaps()}.
	 * @return The new URL-Encoding parser.
	 * @throws Exception If the parser could not be constructed for any reason.
	 */
	protected UrlEncodingParser createUrlEncodingParser(ObjectMap properties, Class<?>[] beanFilters, Class<?>[] pojoSwaps) throws Exception {
		return new UrlEncodingParser().setProperties(properties).addBeanFilters(beanFilters).addPojoSwaps(pojoSwaps);
	}

	/**
	 * Creates the serializer group containing serializers used for serializing output POJOs in HTTP responses.
	 * <p>
	 * Subclasses can override this method to provide their own set of serializers for this servlet.
	 * They can do this by either creating a new {@link SerializerGroup} from scratch, or appending to the
	 * 	group returned by <code><jk>super</jk>.createSerializers()</code>.
	 * <p>
	 * By default, returns the serializers defined through {@link RestResource#serializers() @RestResource.serializers()} on this class
	 * 	and all parent classes.
	 *
	 * @param properties Servlet-level properties returned by {@link #createProperties()}.
	 * @param beanFilters Servlet-level bean filters returned by {@link #createBeanFilters()}.
	 * @param pojoSwaps Servlet-level POJO swaps returned by {@link #createPojoSwaps()}.
	 * @return The group of serializers.
	 * @throws Exception If serializer group could not be constructed for any reason.
	 */
	protected SerializerGroup createSerializers(ObjectMap properties, Class<?>[] beanFilters, Class<?>[] pojoSwaps) throws Exception {
		SerializerGroup g = new SerializerGroup();

		// Serializers are loaded in parent-to-child order to allow overrides.
		for (RestResource r : restResourceAnnotationsParentFirst.values())
			for (Class<? extends Serializer> c : reverse(r.serializers()))
				try {
					g.append(c);
				} catch (Exception e) {
					throw new RestServletException("Exception occurred while trying to instantiate Serializer ''{0}''", c.getSimpleName()).initCause(e);
				}

		g.setProperties(properties);
		g.addBeanFilters(beanFilters).addPojoSwaps(pojoSwaps);
		return g;
	}

	/**
	 * Creates the parser group containing parsers used for parsing input into POJOs from HTTP requests.
	 * <p>
	 * Subclasses can override this method to provide their own set of parsers for this servlet.
	 * They can do this by either creating a new {@link ParserGroup} from scratch, or appending to the
	 * 	group returned by <code><jk>super</jk>.createParsers()</code>.
	 * <p>
	 * By default, returns the parsers defined through {@link RestResource#parsers() @RestResource.parsers()} on this class
	 * 	and all parent classes.
	 *
	 * @param properties Servlet-level properties returned by {@link #createProperties()}.
	 * @param beanFilters Servlet-level bean filters returned by {@link #createBeanFilters()}.
	 * @param pojoSwaps Servlet-level POJO swaps returned by {@link #createPojoSwaps()}.
	 * @return The group of parsers.
	 * @throws Exception If parser group could not be constructed for any reason.
	 */
	protected ParserGroup createParsers(ObjectMap properties, Class<?>[] beanFilters, Class<?>[] pojoSwaps) throws Exception {
		ParserGroup g = new ParserGroup();

		// Parsers are loaded in parent-to-child order to allow overrides.
		for (RestResource r : restResourceAnnotationsParentFirst.values())
			for (Class<? extends Parser> p : reverse(r.parsers()))
				try {
					g.append(p);
				} catch (Exception e) {
					throw new RestServletException("Exception occurred while trying to instantiate Parser ''{0}''", p.getSimpleName()).initCause(e);
				}

		g.setProperties(properties);
		g.addBeanFilters(beanFilters).addPojoSwaps(pojoSwaps);
		return g;
	}

	/**
	 * Creates the class-level converters associated with this servlet.
	 * <p>
	 * Subclasses can override this method to provide their own class-level converters for this servlet.
	 * <p>
	 * By default, returns the converters specified through the {@link RestResource#converters() @RestResource.converters()} annotation in child-to-parent order.
	 * 	(e.g. converters on children will be called before converters on parents).
	 *
	 * @param properties Servlet-level properties returned by {@link #createProperties()}.
	 * @return The new set of transforms associated with this servet.
	 * @throws RestServletException
	 */
	protected RestConverter[] createConverters(ObjectMap properties) throws RestServletException {
		List<RestConverter> l = new LinkedList<RestConverter>();

		// Converters are loaded in child-to-parent order.
		for (RestResource r : restResourceAnnotationsChildFirst.values())
			for (Class<? extends RestConverter> c : r.converters())
				try {
					l.add(c.newInstance());
				} catch (Exception e) {
					throw new RestServletException("Exception occurred while trying to instantiate RestConverter ''{0}''", c.getSimpleName()).initCause(e);
				}

		return l.toArray(new RestConverter[l.size()]);
	}

	/**
	 * Creates the {@link EncoderGroup} for this servlet for handling various encoding schemes.
	 * <p>
	 * Subclasses can override this method to provide their own encoder group, typically by
	 * 	appending to the group returned by <code><jk>super</jk>.createEncoders()</code>.
	 * <p>
	 * By default, returns a group containing {@link IdentityEncoder#INSTANCE} and all encoders
	 * 	specified through {@link RestResource#encoders() @RestResource.encoders()} annotations in parent-to-child order.
	 *
	 * @param properties Servlet-level properties returned by {@link #createProperties()}.
	 * @return The new encoder group associated with this servet.
	 * @throws RestServletException
	 */
	protected EncoderGroup createEncoders(ObjectMap properties) throws RestServletException {
		EncoderGroup g = new EncoderGroup().append(IdentityEncoder.INSTANCE);

		// Encoders are loaded in parent-to-child order to allow overrides.
		for (RestResource r : restResourceAnnotationsParentFirst.values())
			for (Class<? extends Encoder> c : reverse(r.encoders()))
				try {
					g.append(c);
				} catch (Exception e) {
					throw new RestServletException("Exception occurred while trying to instantiate Encoder ''{0}''", c.getSimpleName()).initCause(e);
				}

		return g;
	}

	/**
	 * Creates the class-level guards associated with this servlet.
	 * <p>
	 * Subclasses can override this method to provide their own class-level guards for this servlet.
	 * <p>
	 * By default, returns the guards specified through the {@link RestResource#guards() @RestResource.guards()} annotation in child-to-parent order.
	 * 	(i.e. guards on children will be called before guards on parents).
	 *
	 * @param properties Servlet-level properties returned by {@link #createProperties()}.
	 * @return The new set of guards associated with this servet.
	 * @throws RestServletException
	 */
	protected RestGuard[] createGuards(ObjectMap properties) throws RestServletException {
		List<RestGuard> l = new LinkedList<RestGuard>();

		// Guards are loaded in child-to-parent order.
		for (RestResource r : restResourceAnnotationsChildFirst.values())
			for (Class<? extends RestGuard> c : reverse(r.guards()))
				try {
					l.add(c.newInstance());
				} catch (Exception e) {
					throw new RestServletException("Exception occurred while trying to instantiate RestGuard ''{0}''", c.getSimpleName()).initCause(e);
				}

		return l.toArray(new RestGuard[l.size()]);
	}

	/**
	 * Creates an instance of {@link MimetypesFileTypeMap} that is used to determine
	 * 	the media types of static files.
	 * <p>
	 * Subclasses can override this method to provide their own mappings, or augment the existing
	 * 	map by appending to <code><jk>super</jk>.createMimetypesFileTypeMap()</code>
	 *
	 * @param properties Servlet-level properties returned by {@link #createProperties()}.
	 * @return A new reusable MIME-types map.
	 */
	protected MimetypesFileTypeMap createMimetypesFileTypeMap(ObjectMap properties) {
		MimetypesFileTypeMap m = new MimetypesFileTypeMap();
		m.addMimeTypes("text/css css CSS");
		m.addMimeTypes("text/html html htm HTML");
		m.addMimeTypes("text/plain txt text TXT");
		m.addMimeTypes("application/javascript js");
		m.addMimeTypes("image/png png");
		m.addMimeTypes("image/gif gif");
		m.addMimeTypes("application/xml xml XML");
		m.addMimeTypes("application/json json JSON");
		return m;
	}

	/**
	 * Creates the set of default request headers for this servlet.
	 * <p>
	 * Default request headers are default values for when HTTP requests do not specify a header value.
	 * For example, you can specify a default value for <code>Accept</code> if a request does not specify that header value.
	 * <p>
	 * Subclasses can override this method to provide their own class-level default request headers for this servlet.
	 * <p>
	 * By default, returns the default request headers specified through the {@link RestResource#defaultRequestHeaders() @RestResource.defaultRequestHeaders()}
	 * 	annotation in parent-to-child order.
	 * (e.g. headers defined on children will override the same headers defined on parents).
	 *
	 * @param properties Servlet-level properties returned by {@link #createProperties()}.
	 * @return The new set of default request headers associated with this servet.
	 * @throws RestServletException
	 */
	protected Map<String,String> createDefaultRequestHeaders(ObjectMap properties) throws RestServletException {
		Map<String,String> m = new HashMap<String,String>();

		// Headers are loaded in parent-to-child order to allow overrides.
		for (RestResource r : restResourceAnnotationsParentFirst.values()) {
			for (String s : r.defaultRequestHeaders()) {
				String[] h = parseHeader(s);
				if (h == null)
					throw new RestServletException("Invalid default request header specified: ''{0}''.  Must be in the format: ''Header-Name: header-value''", s);
				m.put(h[0], h[1]);
			}
		}

		return m;
	}

	/**
	 * Creates the set of default response headers for this servlet.
	 * <p>
	 * Default response headers are headers that will be appended to all responses if those headers have not already been
	 * 	set on the response object.
	 * <p>
	 * Subclasses can override this method to provide their own class-level default response headers for this servlet.
	 * <p>
	 * By default, returns the default response headers specified through the {@link RestResource#defaultResponseHeaders() @RestResource.defaultResponseHeaders()}
	 * 	annotation in parent-to-child order.
	 * (e.g. headers defined on children will override the same headers defined on parents).
	 *
	 * @param properties Servlet-level properties returned by {@link #createProperties()}.
	 * @return The new set of default response headers associated with this servet.
	 * @throws RestServletException
	 */
	protected Map<String,Object> createDefaultResponseHeaders(ObjectMap properties) throws RestServletException {
		Map<String,Object> m = new LinkedHashMap<String,Object>();

		// Headers are loaded in parent-to-child order to allow overrides.
		for (RestResource r : restResourceAnnotationsParentFirst.values()) {
			for (String s : r.defaultResponseHeaders()) {
				String[] h = parseHeader(s);
				if (h == null)
					throw new RestServletException("Invalid default response header specified: ''{0}''.  Must be in the format: ''Header-Name: header-value''", s);
				m.put(h[0], h[1]);
			}
		}

		return m;
	}

	/**
	 * Creates the class-level response handlers associated with this servlet.
	 * <p>
	 * Subclasses can override this method to provide their own class-level response handlers for this servlet.
	 * <p>
	 * By default, returns the handlers specified through the {@link RestResource#responseHandlers() @RestResource.responseHandlers()}
	 * 	annotation in parent-to-child order.
	 * (e.g. handlers on children will be called before handlers on parents).
	 *
	 * @param properties Servlet-level properties returned by {@link #createProperties()}.
	 * @return The new set of response handlers associated with this servet.
	 * @throws RestException
	 */
	protected ResponseHandler[] createResponseHandlers(ObjectMap properties) throws RestException {
		List<ResponseHandler> l = new LinkedList<ResponseHandler>();

		// Loaded in parent-to-child order to allow overrides.
		for (RestResource r : restResourceAnnotationsParentFirst.values())
			for (Class<? extends ResponseHandler> c : r.responseHandlers())
				try {
					l.add(c.newInstance());
				} catch (Exception e) {
					throw new RestException(SC_INTERNAL_SERVER_ERROR, e);
				}

		// Add the default handlers.
		l.add(new StreamableHandler());
		l.add(new WritableHandler());
		l.add(new ReaderHandler());
		l.add(new InputStreamHandler());
		l.add(new RedirectHandler());
		l.add(new DefaultHandler());

		return l.toArray(new ResponseHandler[l.size()]);
	}


	//--------------------------------------------------------------------------------
	// Other methods
	//--------------------------------------------------------------------------------

	/**
	 * Returns the localized Swagger from the file system.
	 * <p>
	 * Looks for a file called <js>"{ServletClass}_{locale}.json"</js> in the same package
	 * as this servlet and returns it as a parsed {@link Swagger} object.
	 * <p>
	 * Returned objects are cached for later quick-lookup.
	 *
	 * @param locale The locale of the swagger.
	 * @return The parsed swagger object, or <jk>null</jk> if the swagger file could not be found.
	 * @throws RestException
	 */
	protected Swagger getSwaggerFromFile(Locale locale) throws RestException {
		Swagger s = swaggers.get(locale);
		if (s == null) {
			try {
				s = getResource(Swagger.class, "text/json", getClass().getSimpleName() + ".json", locale);
				swaggers.putIfAbsent(locale, s == null ? Swagger.NULL : s);
			} catch (Exception e) {
				throw new RestException(SC_INTERNAL_SERVER_ERROR, e);
			}
		}
		return s == Swagger.NULL ? null : s;
	}


	/**
	 * Returns the localized swagger for this REST resource.
	 *
	 * @param req The incoming HTTP request.
	 * @return A new Swagger instance.
	 * @throws RestException
	 */
	protected Swagger getSwagger(RestRequest req) throws RestException {
		try {
			// If a file is defined, use that.
			Swagger s = req.getSwaggerFromFile();
			if (s != null)
				return s;

			Info info = Info.create(getTitle(req), getVersion(req))
				.setContact(getContact(req))
				.setLicense(getLicense(req))
				.setDescription(getDescription(req))
				.setTermsOfService(getTermsOfService(req));

			s = Swagger.create(info)
				.addConsumes(getSupportedAcceptTypes())
				.addProduces(getSupportedContentTypes())
				.setTags(getTags(req))
				.setExternalDocs(getExternalDocs(req));

			for (MethodMeta sm : javaRestMethods.values()) {
				if (sm.isRequestAllowed(req)) {
					Operation o = sm.getSwaggerOperation(req);
					s.addPath(
						sm.pathPattern.patternString,
						sm.httpMethod.toLowerCase(),
						o
					);
				}
			}
			return s;
		} catch (RestException e) {
			throw e;
		} catch (Exception e) {
			throw new RestException(SC_INTERNAL_SERVER_ERROR, e);
		}
	}

	/**
	 * Sets the parent of this resource.
	 *
	 * @param parent The parent of this resource.
	 */
	protected void setParent(RestServlet parent) {
		this.parentResource = parent;
	}

	/**
	 * Returns the parent of this resource.
	 *
	 * @return The parent of this resource, or <jk>null</jk> if resource has no parent.
	 */
	public RestServlet getParent() {
		return this.parentResource;
	}

	private String[] parseHeader(String s) {
		int i = s.indexOf(':');
		if (i == -1)
			return null;
		String name = s.substring(0, i).trim().toLowerCase(Locale.ENGLISH);
		String val = s.substring(i+1).trim();
		return new String[]{name,val};
	}

	/**
	 * Creates a {@link RestRequest} object based on the specified incoming {@link HttpServletRequest} object.
	 * <p>
	 * 	Subclasses may choose to override this method to provide a specialized request object.
	 * </p>
	 *
	 * @param req The request object from the {@link #service(HttpServletRequest, HttpServletResponse)} method.
	 * @return The wrapped request object.
	 * @throws ServletException If any errors occur trying to interpret the request.
	 */
	protected RestRequest createRequest(HttpServletRequest req) throws ServletException {
		return new RestRequest(this, req);
	}

	/**
	 * Creates a {@link RestResponse} object based on the specified incoming {@link HttpServletResponse} object
	 * 	 and the request returned by {@link #createRequest(HttpServletRequest)}.
	 * <p>
	 * 	Subclasses may choose to override this method to provide a specialized response object.
	 * </p>
	 *
	 * @param req The request object returned by {@link #createRequest(HttpServletRequest)}.
	 * @param res The response object from the {@link #service(HttpServletRequest, HttpServletResponse)} method.
	 * @return The wrapped response object.
	 * @throws ServletException If any erros occur trying to interpret the request or response.
	 */
	protected RestResponse createResponse(RestRequest req, HttpServletResponse res) throws ServletException {
		return new RestResponse(this, req, res);
	}

	/**
	 * Returns whether this resource class can provide an OPTIONS page.
	 * <p>
	 * 	By default, returns <jk>false</jk>.
	 * </p>
	 * <p>
	 * 	Subclasses can override this method to cause the <code>options</code> link to show up in the HTML serialized output.
	 * </p>
	 *
	 * @return <jk>true</jk> if this resource has implemented a {@code getOptions()} method.
	 */
	public boolean hasOptionsPage() {
		return false;
	}

	/**
	 * Specify a class-level property.
	 * <p>
	 * 	Typically, properties in {@link RestServletContext} can be set in the {@link Servlet#init(ServletConfig)} method.
	 * </p>
	 *
	 * @param key The property name.
	 * @param value The property value.
	 * @return This object (for method chaining).
	 */
	public synchronized RestServlet setProperty(String key, Object value) {
		getProperties().put(key, value);
		return this;
	}

	/**
	 * The main service method.
	 * <p>
	 * 	Subclasses can optionally override this method if they want to tailor the behavior of requests.
	 * </p>
	 */
	@Override /* Servlet */
	public void service(HttpServletRequest r1, HttpServletResponse r2) throws ServletException, IOException {

		log(FINE, "HTTP: {0} {1}", r1.getMethod(), r1.getRequestURI());
		long startTime = System.currentTimeMillis();

		try {

			if (initException != null) {
				if (initException instanceof RestException)
					throw (RestException)initException;
				throw new RestException(SC_INTERNAL_SERVER_ERROR, initException);
			}

			if (! isInitialized)
				throw new RestException(SC_INTERNAL_SERVER_ERROR, "Servlet has not been initialized");

			String pathInfo = RestUtils.getPathInfoUndecoded(r1);  // Can't use r1.getPathInfo() because we don't want '%2F' resolved.

			// If this resource has child resources, try to recursively call them.
			if (pathInfo != null && (! childResources.isEmpty()) && (! pathInfo.equals("/"))) {
				int i = pathInfo.indexOf('/', 1);
				String pathInfoPart = i == -1 ? pathInfo.substring(1) : pathInfo.substring(1, i);
				RestServlet childResource = childResources.get(pathInfoPart);
				if (childResource != null) {
					final String pathInfoRemainder = (i == -1 ? null : pathInfo.substring(i));
					final String servletPath = r1.getServletPath() + "/" + pathInfoPart;
					final HttpServletRequest childRequest = new HttpServletRequestWrapper(r1) {
						@Override /* ServletRequest */
						public String getPathInfo() {
							return RestUtils.decode(pathInfoRemainder);
						}
						@Override /* ServletRequest */
						public String getServletPath() {
							return servletPath;
						}
					};
					childResource.service(childRequest, r2);
					return;
				}
			}

			RestRequest req = createRequest(r1);
			RestResponse res = createResponse(req, r2);
			String method = req.getMethod();
			String methodUC = method.toUpperCase(Locale.ENGLISH);

			StreamResource r = null;
			if (pathInfo != null) {
				String p = pathInfo.substring(1);
				if (p.equals("favicon.ico"))
					r = favIcon;
				else if (p.equals("style.css"))
					r = styleSheet;
				else if (StringUtils.pathStartsWith(p, staticFilesPrefixes))
					r = resolveStaticFile(p);
			}

			if (r != null) {
				res.setStatus(SC_OK);
				res.setOutput(r);
			} else {
				// If the specified method has been defined in a subclass, invoke it.
				int rc = SC_METHOD_NOT_ALLOWED;
				if (restMethods.containsKey(methodUC)) {
					rc = restMethods.get(methodUC).invoke(method, pathInfo, this, req, res);
				} else if (restMethods.containsKey("*")) {
					rc = restMethods.get("*").invoke(method, pathInfo, this, req, res);
				}

				// If not invoked above, see if it's an OPTIONs request
				if (rc != SC_OK)
					handleNotFound(rc, req, res);
			}

			if (res.hasOutput()) {
				Object output = res.getOutput();

				// Do any class-level transforming.
				for (RestConverter converter : getConverters())
					output = converter.convert(req, output, getBeanContext().getClassMetaForObject(output));

				res.setOutput(output);

				// Now serialize the output if there was any.
				// Some subclasses may write to the OutputStream or Writer directly.
				handleResponse(req, res, output);
			}

			onSuccess(req, res, System.currentTimeMillis() - startTime);

		} catch (RestException e) {
			handleError(r1, r2, e);
		} catch (Throwable e) {
			handleError(r1, r2, new RestException(SC_INTERNAL_SERVER_ERROR, e));
		}
		log(FINE, "HTTP: [{0} {1}] finished in {2}ms", r1.getMethod(), r1.getRequestURI(), System.currentTimeMillis()-startTime);
	}

	/**
	 * Handle the case where a matching method was not found.
	 * <p>
	 * 	Subclasses can override this method to provide a 2nd-chance for specifying a response.
	 * 	The default implementation will simply throw an exception with an appropriate message.
	 * </p>
	 *
	 * @param rc The HTTP response code.
	 * @param req The HTTP request.
	 * @param res The HTTP response.
	 * @throws Exception
	 */
	protected void handleNotFound(int rc, RestRequest req, RestResponse res) throws Exception {
		String pathInfo = req.getPathInfo();
		String methodUC = req.getMethod();
		String onPath = pathInfo == null ? " on no pathInfo"  : format(" on path '%s'", pathInfo);
		if (rc == SC_NOT_FOUND)
			throw new RestException(rc, "Method ''{0}'' not found on resource with matching pattern{1}.", methodUC, onPath);
		else if (rc == SC_PRECONDITION_FAILED)
			throw new RestException(rc, "Method ''{0}'' not found on resource{1} with matching matcher.", methodUC, onPath);
		else if (rc == SC_METHOD_NOT_ALLOWED)
			throw new RestException(rc, "Method ''{0}'' not found on resource.", methodUC);
		else
			throw new ServletException("Invalid method response: " + rc);
	}

	private synchronized void handleError(HttpServletRequest req, HttpServletResponse res, RestException e) throws IOException {
		Integer c = 1;
		if (context.useStackTraceHashes) {
			int h = e.hashCode();
			c = stackTraceHashes.get(h);
			if (c == null)
				c = 1;
			else
				c++;
			stackTraceHashes.put(h, c);
			e.setOccurrence(c);
		}
		onError(req, res, e);
		renderError(req, res, e);
	}

	/**
	 * Method for rendering response errors.
	 * <p>
	 * 	The default implementation renders a plain text English message, optionally with a stack trace
	 * 		if {@link RestServletContext#REST_renderResponseStackTraces} is enabled.
	 * </p>
	 * <p>
	 * 	Subclasses can override this method to provide their own custom error response handling.
	 * </p>
	 *
	 * @param req The servlet request.
	 * @param res The servlet response.
	 * @param e The exception that occurred.
	 * @throws IOException Can be thrown if a problem occurred trying to write to the output stream.
	 */
	protected void renderError(HttpServletRequest req, HttpServletResponse res, RestException e) throws IOException {

		int status = e.getStatus();
		res.setStatus(status);
		res.setContentType("text/plain");
		res.setHeader("Content-Encoding", "identity");
		PrintWriter w = null;
		try {
			w = res.getWriter();
		} catch (IllegalStateException e2) {
			w = new PrintWriter(new OutputStreamWriter(res.getOutputStream(), IOUtils.UTF8));
		}
		String httpMessage = RestUtils.getHttpResponseText(status);
		if (httpMessage != null)
			w.append("HTTP ").append(String.valueOf(status)).append(": ").append(httpMessage).append("\n\n");
		if (context.renderResponseStackTraces)
			e.printStackTrace(w);
		else
			w.append(e.getFullStackMessage(true));
		w.flush();
		w.close();
	}

	/**
	 * Callback method for logging errors during HTTP requests.
	 * <p>
	 * 	Typically, subclasses will override this method and log errors themselves.
	 * <p>
	 * </p>
	 * 	The default implementation simply logs errors to the <code>RestServlet</code> logger.
	 * </p>
	 * <p>
	 * 	Here's a typical implementation showing how stack trace hashing can be used to reduce log file sizes...
	 * </p>
	 * <p class='bcode'>
	 * 	<jk>protected void</jk> onError(HttpServletRequest req, HttpServletResponse res, RestException e, <jk>boolean</jk> noTrace) {
	 * 		String qs = req.getQueryString();
	 * 		String msg = <js>"HTTP "</js> + req.getMethod() + <js>" "</js> + e.getStatus() + <js>" "</js> + req.getRequestURI() + (qs == <jk>null</jk> ? <js>""</js> : <js>"?"</js> + qs);
	 * 		<jk>int</jk> c = e.getOccurrence();
	 *
	 * 		<jc>// REST_useStackTraceHashes is disabled, so we have to log the exception every time.</jc>
	 * 		<jk>if</jk> (c == 0)
	 * 			myLogger.log(Level.<jsf>WARNING</jsf>, <jsm>format</jsm>(<js>"[%s] %s"</js>, e.getStatus(), msg), e);
	 *
	 * 		<jc>// This is the first time we've countered this error, so log a stack trace
	 * 		// unless ?noTrace was passed in as a URL parameter.</jc>
	 * 		<jk>else if</jk> (c == 1 && ! noTrace)
	 * 			myLogger.log(Level.<jsf>WARNING</jsf>, <jsm>format</jsm>(<js>"[%h.%s.%s] %s"</js>, e.hashCode(), e.getStatus(), c, msg), e);
	 *
	 * 		<jc>// This error occurred before.
	 * 		// Only log the message, not the stack trace.</jc>
	 * 		<jk>else</jk>
	 * 			myLogger.log(Level.<jsf>WARNING</jsf>, <jsm>format</jsm>(<js>"[%h.%s.%s] %s, %s"</js>, e.hashCode(), e.getStatus(), c, msg, e.getLocalizedMessage()));
	 * 	}
	 * </p>
	 *
	 * @param req The servlet request object.
	 * @param res The servlet response object.
	 * @param e Exception indicating what error occurred.
	 */
	protected void onError(HttpServletRequest req, HttpServletResponse res, RestException e) {
		if (shouldLog(req, res, e)) {
			String qs = req.getQueryString();
			String msg = "HTTP " + req.getMethod() + " " + e.getStatus() + " " + req.getRequestURI() + (qs == null ? "" : "?" + qs);
			int c = e.getOccurrence();
			if (shouldLogStackTrace(req, res, e)) {
				msg = '[' + Integer.toHexString(e.hashCode()) + '.' + e.getStatus() + '.' + c + "] " + msg;
				log(Level.WARNING, e, msg);
			} else {
				msg = '[' + Integer.toHexString(e.hashCode()) + '.' + e.getStatus() + '.' + c + "] " + msg + ", " + e.getLocalizedMessage();
				log(Level.WARNING, msg);
			}
		}
	}

	/**
	 * Returns <jk>true</jk> if the specified exception should be logged.
	 * <p>
	 * 	Subclasses can override this method to provide their own logic for determining when exceptions are logged.
	 * </p>
	 * <p>
	 * 	The default implementation will return <jk>false</jk> if <js>"noTrace=true"</js> is passed in the query string.
	 * </p>
	 *
	 * @param req The HTTP request.
	 * @param res The HTTP response.
	 * @param e The exception.
	 * @return <jk>true</jk> if exception should be logged.
	 */
	protected boolean shouldLog(HttpServletRequest req, HttpServletResponse res, RestException e) {
		String q = req.getQueryString();
		return (q == null ? true : q.indexOf("noTrace=true") == -1);
	}

	/**
	 * Returns <jk>true</jk> if a stack trace should be logged for this exception.
	 * <p>
	 * 	Subclasses can override this method to provide their own logic for determining when stack traces are logged.
	 * </p>
	 * <p>
	 * 	The default implementation will only log a stack trace if {@link RestException#getOccurrence()} returns <code>1</code>
	 * 		and the exception is not one of the following:
	 * </p>
	 * <ul>
	 * 	<li>{@link HttpServletResponse#SC_UNAUTHORIZED}
	 * 	<li>{@link HttpServletResponse#SC_FORBIDDEN}
	 * 	<li>{@link HttpServletResponse#SC_NOT_FOUND}
	 * </ul>
	 *
	 * @param req The HTTP request.
	 * @param res The HTTP response.
	 * @param e The exception.
	 * @return <jk>true</jk> if stack trace should be logged.
	 */
	protected boolean shouldLogStackTrace(HttpServletRequest req, HttpServletResponse res, RestException e) {
		if (e.getOccurrence() == 1) {
			switch (e.getStatus()) {
				case SC_UNAUTHORIZED:
				case SC_FORBIDDEN:
				case SC_NOT_FOUND:  return false;
				default:            return true;
			}
		}
		return false;
	}

	/**
	 * Log a message.
	 * <p>
	 * 	Equivalent to calling <code>log(level, <jk>null</jk>, msg, args);</code>
	 * </p>
	 *
	 * @param level The log level.
	 * @param msg The message to log.
	 * @param args {@link MessageFormat} style arguments in the message.
	 */
	protected void log(Level level, String msg, Object...args) {
		log(level, null, msg, args);
	}

	/**
	 * Same as {@link #log(Level, String, Object...)} excepts runs the
	 *  arguments through {@link JsonSerializer#DEFAULT_LAX_READABLE}.
	 * <p>
	 * 	Serialization of arguments do not occur if message is not logged, so
	 * 		it's safe to use this method from within debug log statements.
	 *	</p>
	 *
	 * <h6 class='topic'>Example:</h6>
	 * <p class='bcode'>
	 * 	logObjects(<jsf>DEBUG</jsf>, <js>"Pojo contents:\n{0}"</js>, myPojo);
	 * </p>
	 *
	 * @param level The log level.
	 * @param msg The message to log.
	 * @param args {@link MessageFormat} style arguments in the message.
	 */
	protected void logObjects(Level level, String msg, Object...args) {
		for (int i = 0; i < args.length; i++)
			args[i] = JsonSerializer.DEFAULT_LAX_READABLE.toStringObject(args[i]);
		log(level, null, msg, args);
	}

	/**
	 * Log a message to the logger returned by {@link #getLogger()}.
	 * <p>
	 * 	Subclasses can override this method if they wish to log messages using a library other than
	 * 		Java Logging (e.g. Apache Commons Logging).
	 * </p>
	 *
	 * @param level The log level.
	 * @param cause The cause.
	 * @param msg The message to log.
	 * @param args {@link MessageFormat} style arguments in the message.
	 */
	protected void log(Level level, Throwable cause, String msg, Object...args) {
		JuneauLogger log = getLogger();
		if (args.length > 0)
			msg = MessageFormat.format(msg, args);
		log.log(level, msg, cause);
	}

	/**
	 * Callback method for listening for successful completion of requests.
	 * <p>
	 * 	Subclasses can override this method for gathering performance statistics.
	 * </p>
	 * <p>
	 * 	The default implementation does nothing.
	 * </p>
	 *
	 * @param req The HTTP request.
	 * @param res The HTTP response.
	 * @param time The time in milliseconds it took to process the request.
	 */
	protected void onSuccess(RestRequest req, RestResponse res, long time) {}

	/**
	 * Callback method that gets invoked right before the REST Java method is invoked.
	 * <p>
	 * 	Subclasses can override this method to override request headers or set request-duration properties
	 * 		before the Java method is invoked.
	 * </p>
	 *
	 * @param req The HTTP servlet request object.
	 * @throws RestException If any error occurs.
	 */
	protected void onPreCall(RestRequest req) throws RestException {}

	/**
	 * Callback method that gets invoked right after the REST Java method is invoked, but before
	 * 	the serializer is invoked.
	 * <p>
	 * 	Subclasses can override this method to override request and response headers, or
	 * 		set/override properties used by the serializer.
	 * </p>
	 *
	 * @param req The HTTP servlet request object.
	 * @param res The HTTP servlet response object.
	 * @throws RestException If any error occurs.
	 */
	protected void onPostCall(RestRequest req, RestResponse res) throws RestException {}

	/**
	 * The main method for serializing POJOs passed in through the {@link RestResponse#setOutput(Object)} method.
	 * <p>
	 * 	Subclasses may override this method if they wish to modify the way the output is rendered, or support
	 * 	other output formats.
	 * </p>
	 *
	 * @param req The HTTP request.
	 * @param res The HTTP response.
	 * @param output The output to serialize in the response.
	 * @throws IOException
	 * @throws RestException
	 */
	protected void handleResponse(RestRequest req, RestResponse res, Object output) throws IOException, RestException {
		// Loop until we find the correct handler for the POJO.
		for (ResponseHandler h : getResponseHandlers())
			if (h.handle(req, res, output))
				return;
		throw new RestException(SC_NOT_IMPLEMENTED, "No response handlers found to process output of type '"+(output == null ? null : output.getClass().getName())+"'");
	}

	@Override /* GenericServlet */
	public ServletConfig getServletConfig() {
		return servletConfig;
	}

	@Override /* GenericServlet */
	public void destroy() {
		for (RestServlet r : childResources.values())
			r.destroy();
		super.destroy();
	}

	/**
	 * Resolve a static resource file.
	 * <p>
	 * 	Subclasses can override this method to provide their own way to resolve files.
	 *	</p>
	 *
	 * @param pathInfo The unencoded path info.
	 * @return The resource, or <jk>null</jk> if the resource could not be resolved.
	 * @throws IOException
	 */
	protected StreamResource resolveStaticFile(String pathInfo) throws IOException {
		if (! staticFilesCache.containsKey(pathInfo)) {
			String p = RestUtils.decode(RestUtils.trimSlashes(pathInfo));
			if (p.indexOf("..") != -1)
				throw new RestException(SC_NOT_FOUND, "Invalid path");
			for (Map.Entry<String,String> e : staticFilesMap.entrySet()) {
				String key = RestUtils.trimSlashes(e.getKey());
				if (p.startsWith(key)) {
					String remainder = (p.equals(key) ? "" : p.substring(key.length()));
					if (remainder.isEmpty() || remainder.startsWith("/")) {
						String p2 = RestUtils.trimSlashes(e.getValue()) + remainder;
						InputStream is = getResource(p2, null);
						if (is != null) {
							try {
								int i = p2.lastIndexOf('/');
								String name = (i == -1 ? p2 : p2.substring(i+1));
								String mediaType = getMimetypesFileTypeMap().getContentType(name);
								staticFilesCache.put(pathInfo, new StreamResource(is, mediaType).setHeader("Cache-Control", "max-age=86400, public"));
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
	 * Returns a list of valid {@code Accept} content types for this resource.
	 * <p>
	 * 	Typically used by subclasses during {@code OPTIONS} requests.
	 * </p>
	 * <p>
	 * 	The default implementation resturns the list from {@link ParserGroup#getSupportedMediaTypes()}
	 * 		from the parser group returned by {@link #getParsers()}.
	 * </p>
	 * <p>
	 * 	Subclasses can override or expand this list as they see fit.
	 * </p>
	 *
	 * @return The list of valid {@code Accept} content types for this resource.
	 * @throws RestServletException
	 */
	public Collection<String> getSupportedAcceptTypes() throws RestServletException {
		return getParsers().getSupportedMediaTypes();
	}

	/**
	 * Returns a list of valid {@code Content-Types} for input for this resource.
	 * <p>
	 * 	Typically used by subclasses during {@code OPTIONS} requests.
	 * </p>
	 * <p>
	 * 	The default implementation resturns the list from {@link SerializerGroup#getSupportedMediaTypes()}
	 * 		from the parser group returned by {@link #getSerializers()}.
	 * </p>
	 * <p>
	 * 	Subclasses can override or expand this list as they see fit.
	 * </p>
	 *
	 * @return The list of valid {@code Content-Type} header values for this resource.
	 * @throws RestServletException
	 */
	public Collection<String> getSupportedContentTypes() throws RestServletException {
		return getSerializers().getSupportedMediaTypes();
	}

	/**
	 * Returns the localized summary of the specified java method on this servlet.
	 * <p>
	 * 	Subclasses can override this method to provide their own summary.
	 * </p>
	 * <p>
	 * 	The default implementation returns the summary from the following locations (whichever matches first):
	 * </p>
	 * <ol>
	 * 	<li>{@link RestMethod#summary() @RestMethod.summary()} annotation on the method.
	 * 	<li><ck>[ClassName].[javaMethodName].summary</ck> property in resource bundle identified by {@link RestResource#messages() @RestResource.messages()}
	 * 		annotation for this class, then any parent classes.
	 * 	<li><ck>[javaMethodName].summary</ck> property in resource bundle identified by {@link RestResource#messages() @RestResource.messages()}
	 * 		annotation for this class, then any parent classes.
	 * </ol>
	 *
	 * @param javaMethodName The name of the Java method whose description we're retrieving.
	 * @param req The current request.
	 * @return The localized summary of the method, or a blank string if no summary was found.
	 */
	public String getMethodSummary(String javaMethodName, RestRequest req) {
		MethodMeta m = javaRestMethods.get(javaMethodName);
		if (m != null)
			return m.getSummary(req);
		return "";
	}

	/**
	 * Returns the localized description of the specified java method on this servlet.
	 * <p>
	 * 	Subclasses can override this method to provide their own description.
	 * </p>
	 * <p>
	 * 	The default implementation returns the description from the following locations (whichever matches first):
	 * </p>
	 * <ol>
	 * 	<li>{@link RestMethod#description() @RestMethod.description()} annotation on the method.
	 * 	<li><ck>[ClassName].[javaMethodName].description</ck> property in resource bundle identified by {@link RestResource#messages() @RestResource.messages()}
	 * 		annotation for this class, then any parent classes.
	 * 	<li><ck>[javaMethodName].description</ck> property in resource bundle identified by {@link RestResource#messages() @RestResource.messages()}
	 * 		annotation for this class, then any parent classes.
	 * </ol>
	 *
	 * @param javaMethodName The name of the Java method whose description we're retrieving.
	 * @param req The current request.
	 * @return The localized description of the method, or a blank string if no description was found.
	 */
	public String getMethodDescription(String javaMethodName, RestRequest req) {
		MethodMeta m = javaRestMethods.get(javaMethodName);
		if (m != null)
			return m.getDescription(req);
		return "";
	}

	/**
	 * Returns the localized title of this REST resource.
	 * <p>
	 * 	Subclasses can override this method to provide their own title.
	 * <p>
	 * 	The default implementation returns the description from the following locations (whichever matches first):
	 * <p>
	 * <ol>
	 * 	<li>{@link RestResource#title() @RestResourcel.title()} annotation on this class, and then any parent classes.
	 * 	<li><ck>[ClassName].title</ck> property in resource bundle identified by {@link RestResource#messages() @ResourceBundle.messages()}
	 * 		annotation for this class, then any parent classes.
	 * 	<li><ck>title</ck> in resource bundle identified by {@link RestResource#messages() @RestResource.messages()}
	 * 		annotation for this class, then any parent classes.
	 * 	<li><ck>/info/title</ck> entry in swagger file.
	 * </ol>
	 *
	 * @param req The current request.
	 * @return The localized description of this REST resource, or <jk>null</jk> if no resource description was found.
	 */
	public String getTitle(RestRequest req) {
		VarResolverSession vr = req.getVarResolverSession();
		if (title != null)
			return vr.resolve(title);
		String title = msgs.findFirstString(req.getLocale(), "title");
		if (title != null)
			return vr.resolve(title);
		Swagger s = req.getSwaggerFromFile();
		if (s != null && s.getInfo() != null)
			return s.getInfo().getTitle();
		return null;
	}

	/**
	 * Returns the localized description of this REST resource.
	 * <p>
	 * 	Subclasses can override this method to provide their own description.
	 * <p>
	 * 	The default implementation returns the description from the following locations (whichever matches first):
	 * <ol>
	 * 	<li>{@link RestResource#description() @RestResource.description()} annotation on this class, and then any parent classes.
	 * 	<li><ck>[ClassName].description</ck> property in resource bundle identified by {@link RestResource#messages() @RestResource.messages()}
	 * 		annotation for this class, then any parent classes.
	 * 	<li><ck>description</ck> property in resource bundle identified by {@link RestResource#messages() @RestResource.messages()}
	 * 		annotation for this class, then any parent classes.
	 * 	<li><ck>/info/description</ck> entry in swagger file.
	 * </ol>
	 *
	 * @param req The current request.
	 * @return The localized description of this REST resource, or <jk>null</jk> if no resource description was found.
	 */
	public String getDescription(RestRequest req) {
		VarResolverSession vr = req.getVarResolverSession();
		if (description != null)
			return vr.resolve(description);
		String description = msgs.findFirstString(req.getLocale(), "description");
		if (description != null)
			return vr.resolve(description);
		Swagger s = req.getSwaggerFromFile();
		if (s != null && s.getInfo() != null)
			return s.getInfo().getDescription();
		return null;
	}

	/**
	 * Returns the localized contact information of this REST resource.
	 * <p>
	 * 	Subclasses can override this method to provide their own contact information.
	 * <p>
	 * 	The default implementation returns the contact information from the following locations (whichever matches first):
	 * <ol>
	 * 	<li>{@link RestResource#contact() @RestResource.contact()} annotation on this class, and then any parent classes.
	 * 	<li><ck>[ClassName].contact</ck> property in resource bundle identified by {@link RestResource#messages() @RestResource.messages()}
	 * 		annotation for this class, then any parent classes.
	 * 	<li><ck>contact</ck> property in resource bundle identified by {@link RestResource#messages() @RestResource.messages()}
	 * 		annotation for this class, then any parent classes.
	 * 	<li><ck>/info/contact</ck> entry in swagger file.
	 * </ol>
	 *
	 * @param req The current request.
	 * @return The localized contact information of this REST resource, or <jk>null</jk> if no contact information was found.
	 */
	public Contact getContact(RestRequest req) {
		VarResolverSession vr = req.getVarResolverSession();
		JsonParser jp = JsonParser.DEFAULT;
		try {
			if (contact != null)
				return jp.parse(vr.resolve(contact), Contact.class);
			String contact = msgs.findFirstString(req.getLocale(), "contact");
			if (contact != null)
				return jp.parse(vr.resolve(contact), Contact.class);
			Swagger s = req.getSwaggerFromFile();
			if (s != null && s.getInfo() != null)
				return s.getInfo().getContact();
			return null;
		} catch (ParseException e) {
			throw new RestException(SC_INTERNAL_SERVER_ERROR, e);
		}
	}

	/**
	 * Returns the localized license information of this REST resource.
	 * <p>
	 * 	Subclasses can override this method to provide their own license information.
	 * <p>
	 * 	The default implementation returns the license information from the following locations (whichever matches first):
	 * <ol>
	 * 	<li>{@link RestResource#license() @RestResource.license()} annotation on this class, and then any parent classes.
	 * 	<li><ck>[ClassName].license</ck> property in resource bundle identified by {@link RestResource#messages() @RestResource.messages()}
	 * 		annotation for this class, then any parent classes.
	 * 	<li><ck>license</ck> property in resource bundle identified by {@link RestResource#messages() @RestResource.messages()}
	 * 		annotation for this class, then any parent classes.
	 * 	<li><ck>/info/license</ck> entry in swagger file.
	 * </ol>
	 *
	 * @param req The current request.
	 * @return The localized contact information of this REST resource, or <jk>null</jk> if no contact information was found.
	 */
	public License getLicense(RestRequest req) {
		VarResolverSession vr = req.getVarResolverSession();
		JsonParser jp = JsonParser.DEFAULT;
		try {
			if (license != null)
				return jp.parse(vr.resolve(license), License.class);
			String license = msgs.findFirstString(req.getLocale(), "license");
			if (license != null)
				return jp.parse(vr.resolve(license), License.class);
			Swagger s = req.getSwaggerFromFile();
			if (s != null && s.getInfo() != null)
				return s.getInfo().getLicense();
			return null;
		} catch (ParseException e) {
			throw new RestException(SC_INTERNAL_SERVER_ERROR, e);
		}
	}

	/**
	 * Returns the terms-of-service information of this REST resource.
	 * <p>
	 * 	Subclasses can override this method to provide their own terms-of-service information.
	 * <p>
	 * 	The default implementation returns the terms-of-service information from the following locations (whichever matches first):
	 * <ol>
	 * 	<li>{@link RestResource#termsOfService() @RestResource.termsOfService()} annotation on this class, and then any parent classes.
	 * 	<li><ck>[ClassName].termsOfService</ck> property in resource bundle identified by {@link RestResource#messages() @RestResource.messages()}
	 * 		annotation for this class, then any parent classes.
	 * 	<li><ck>termsOfService</ck> property in resource bundle identified by {@link RestResource#messages() @RestResource.messages()}
	 * 		annotation for this class, then any parent classes.
	 * 	<li><ck>/info/termsOfService</ck> entry in swagger file.
	 * </ol>
	 *
	 * @param req The current request.
	 * @return The localized contact information of this REST resource, or <jk>null</jk> if no contact information was found.
	 */
	public String getTermsOfService(RestRequest req) {
		VarResolverSession vr = req.getVarResolverSession();
		if (termsOfService != null)
			return vr.resolve(termsOfService);
		String termsOfService = msgs.findFirstString(req.getLocale(), "termsOfService");
		if (termsOfService != null)
			return vr.resolve(termsOfService);
		Swagger s = req.getSwaggerFromFile();
		if (s != null && s.getInfo() != null)
			return s.getInfo().getTermsOfService();
		return null;
	}

	/**
	 * Returns the version information of this REST resource.
	 * <p>
	 * 	Subclasses can override this method to provide their own version information.
	 * <p>
	 * 	The default implementation returns the version information from the following locations (whichever matches first):
	 * <ol>
	 * 	<li>{@link RestResource#version() @RestResource.version()} annotation on this class, and then any parent classes.
	 * 	<li><ck>[ClassName].version</ck> property in resource bundle identified by {@link RestResource#messages() @RestResource.messages()}
	 * 		annotation for this class, then any parent classes.
	 * 	<li><ck>version</ck> property in resource bundle identified by {@link RestResource#messages() @RestResource.messages()}
	 * 		annotation for this class, then any parent classes.
	 * 	<li><ck>/info/version</ck> entry in swagger file.
	 * </ol>
	 *
	 * @param req The current request.
	 * @return The localized contact information of this REST resource, or <jk>null</jk> if no contact information was found.
	 */
	public String getVersion(RestRequest req) {
		VarResolverSession vr = req.getVarResolverSession();
		if (version != null)
			return vr.resolve(version);
		String version = msgs.findFirstString(req.getLocale(), "version");
		if (version != null)
			return vr.resolve(version);
		Swagger s = req.getSwaggerFromFile();
		if (s != null && s.getInfo() != null)
			return s.getInfo().getVersion();
		return null;
	}

	/**
	 * Returns the version information of this REST resource.
	 * <p>
	 * 	Subclasses can override this method to provide their own version information.
	 * <p>
	 * 	The default implementation returns the version information from the following locations (whichever matches first):
	 * <ol>
	 * 	<li>{@link RestResource#version() @RestResource.version()} annotation on this class, and then any parent classes.
	 * 	<li><ck>[ClassName].version</ck> property in resource bundle identified by {@link RestResource#messages() @RestResource.messages()}
	 * 		annotation for this class, then any parent classes.
	 * 	<li><ck>version</ck> property in resource bundle identified by {@link RestResource#messages() @RestResource.messages()}
	 * 		annotation for this class, then any parent classes.
	 * 	<li><ck>/info/version</ck> entry in swagger file.
	 * </ol>
	 *
	 * @param req The current request.
	 * @return The localized contact information of this REST resource, or <jk>null</jk> if no contact information was found.
	 */
	@SuppressWarnings("unchecked")
	public List<Tag> getTags(RestRequest req) {
		VarResolverSession vr = req.getVarResolverSession();
		JsonParser jp = JsonParser.DEFAULT;
		try {
			if (tags != null)
				return jp.parseCollection(vr.resolve(tags), ArrayList.class, Tag.class);
			String tags = msgs.findFirstString(req.getLocale(), "tags");
			if (tags != null)
				return jp.parseCollection(vr.resolve(tags), ArrayList.class, Tag.class);
			Swagger s = req.getSwaggerFromFile();
			if (s != null)
				return s.getTags();
			return null;
		} catch (Exception e) {
			throw new RestException(SC_INTERNAL_SERVER_ERROR, e);
		}
	}

	/**
	 * Returns the version information of this REST resource.
	 * <p>
	 * 	Subclasses can override this method to provide their own version information.
	 * <p>
	 * 	The default implementation returns the version information from the following locations (whichever matches first):
	 * <ol>
	 * 	<li>{@link RestResource#version() @RestResource.version()} annotation on this class, and then any parent classes.
	 * 	<li><ck>[ClassName].version</ck> property in resource bundle identified by {@link RestResource#messages() @RestResource.messages()}
	 * 		annotation for this class, then any parent classes.
	 * 	<li><ck>version</ck> property in resource bundle identified by {@link RestResource#messages() @RestResource.messages()}
	 * 		annotation for this class, then any parent classes.
	 * 	<li><ck>/info/version</ck> entry in swagger file.
	 * </ol>
	 *
	 * @param req The current request.
	 * @return The localized contact information of this REST resource, or <jk>null</jk> if no contact information was found.
	 */
	public ExternalDocumentation getExternalDocs(RestRequest req) {
		VarResolverSession vr = req.getVarResolverSession();
		JsonParser jp = JsonParser.DEFAULT;
		try {
			if (externalDocs != null)
				return jp.parse(vr.resolve(externalDocs), ExternalDocumentation.class);
			String externalDocs = msgs.findFirstString(req.getLocale(), "externalDocs");
			if (externalDocs != null)
				return jp.parse(vr.resolve(externalDocs), ExternalDocumentation.class);
			Swagger s = req.getSwaggerFromFile();
			if (s != null)
				return s.getExternalDocs();
			return null;
		} catch (Exception e) {
			throw new RestException(SC_INTERNAL_SERVER_ERROR, e);
		}
	}

	/**
	 * Returns the resource bundle identified by the {@link RestResource#messages() @RestResource.messages()} annotation for the default locale.
	 *
	 * @return The resource bundle.  Never <jk>null</jk>.
	 */
	public MessageBundle getMessages() {
		return msgs;
	}

	/**
	 * Returns the resource bundle identified by the {@link RestResource#messages() @RestResource.messages()} annotation for the specified locale.
	 *
	 * @param locale The resource bundle locale.
	 * @return The resource bundle.  Never <jk>null</jk>.
	 */
	public MessageBundle getMessages(Locale locale) {
		return msgs.getBundle(locale);
	}

	/**
	 * Gets a localized message from the resource bundle identified by the {@link RestResource#messages() @RestResource.messages()} annotation.
	 * <p>
	 * 	If resource bundle location was not specified, or the resource bundle was not found,
	 * 	returns the string <js>"{!!key}"</js>.
	 * </p>
	 * <p>
	 * 	If message was not found in the resource bundle, returns the string <js>"{!key}"</js>.
	 * </p>
	 *
	 * @param locale The client locale.
	 * @param key The resource bundle key.
	 * @param args Optional {@link java.text.MessageFormat} variable values to replace.
	 * @return The localized message.
	 */
	public String getMessage(Locale locale, String key, Object...args) {
		return msgs.getString(locale, key, args);
	}

	/**
	 * Programmatically adds the specified resource as a child to this resource.
	 * <p>
	 * 	This method can be used in a resources {@link #init()} method to define child resources
	 * 	accessible through a child URL.
	 * </p>
	 * <p>
	 * 	Typically, child methods are defined via {@link RestResource#children() @RestResource.children()}.  However, this
	 * 	method is provided to handle child resources determined at runtime.
	 * </p>
	 *
	 * @param name The sub-URL under which this resource is accessible.<br>
	 * 	For example, if the parent resource URL is <js>"/foo"</js>, and this name is <js>"bar"</js>, then
	 * 	the child resource will be accessible via the URL <js>"/foo/bar"</js>.
	 * @param resource The child resource.
	 * @throws ServletException Thrown by the child init() method.
	 */
	protected void addChildResource(String name, RestServlet resource) throws ServletException {
		resource.init(getServletConfig());
		childResources.put(name, resource);
	}

	/**
	 * Returns the child resources associated with this servlet.
	 *
	 * @return An unmodifiable map of child resources.
	 * 	Keys are the {@link RestResource#path() @RestResource.path()} annotation defined on the child resource.
	 */
	public Map<String,RestServlet> getChildResources() {
		return Collections.unmodifiableMap(childResources);
	}

	/**
	 * Returns the path for this servlet as defined by the {@link RestResource#path()} annotation
	 * on this class concatenated with those on all parent classes.
	 * <p>
	 * 	If path is not specified, returns <js>"/"</js>.
	 * </p>
	 * <p>
	 * 	Path always starts with <js>"/"</js>.
	 * </p>
	 *
	 * @return The servlet path.
	 */
	public String getPath() {
		if (path == null) {
			LinkedList<String> l = new LinkedList<String>();
			RestServlet r = this;
			while (r != null) {
				String p = r.findPath();
				if (p == null)
					break;
				l.addFirst(p);
				r = r.parentResource;
			}
			StringBuilder sb = new StringBuilder();
			for (String p : l)
				sb.append('/').append(p);
			path = sb.toString();
		}
		return path;
	}

	private String findPath() {
		List<RestResource> rrc = ReflectionUtils.findAnnotations(RestResource.class, getClass());
		for (RestResource rc : rrc) {
			String p = rc.path();
			if (StringUtils.startsWith(p, '/'))
				p = p.substring(1);
			if (! p.isEmpty())
				return p;
		}
		return null;
	}

	/**
	 * Returns the config file for this servlet.
	 * <p>
	 * 	Subclasses can override this method to provide their own config file.
	 * </p>
	 * <p>
	 * 	The default implementation uses the path defined by the {@link RestResource#config() @RestResource.config()} property resolved
	 *  		by {@link ConfigMgr#DEFAULT}.
	 * </p>
	 *
	 * @return The config file for this servlet.
	 * @throws IOException
	 */
	protected ConfigFile createConfigFile() throws IOException {
		String cf = varResolver.resolve(configPath);
		if (cf.isEmpty())
			return getConfigMgr().create();
		return getConfigMgr().get(cf);
	}

	/**
	 * Creates the stylesheet for this servlet.
	 * <p>
	 * 	The stylesheet is made available on the path <js>"/servlet-path/style.css"</js>.
	 * </p>
	 * <p>
	 * 	Subclasses can override this method to provide their own stylesheet.
	 * </p>
	 * <p>
	 * 	The default implementation uses the {@link RestResource#stylesheet() @RestResource.stylesheet()} annotation
	 * 		to determine the stylesheet name and then searches the classpath then working directory
	 * 		for that stylesheet.
	 * </p>
	 *
	 * @return The stylesheet to use for this servlet, or <jk>null</jk> if the stylesheet could not be found.
	 * @throws IOException If stylesheet could not be loaded.
	 */
	protected StreamResource createStyleSheet() throws IOException {
		for (RestResource r : restResourceAnnotationsChildFirst.values()) {
			if (! r.stylesheet().isEmpty()) {
				String path = getVarResolver().resolve(r.stylesheet());
				InputStream is = getResource(path, null);
				if (is != null) {
					try {
						return new StreamResource(is, "text/css");
					} finally {
						is.close();
					}
				}
			}
		}
		return null;
	}

	/**
	 * Creates the favicon for this servlet.
	 * <p>
	 * 	The favicon is made available on the path <js>"/servlet-path/favicon.ico"</js>.
	 * </p>
	 * <p>
	 * 	Subclasses can override this method to provide their own favorites icon.
	 * </p>
	 * <p>
	 * 	The default implementation uses the {@link RestResource#favicon() @RestResource.favicon()} annotation
	 * 		to determine the file name and then searches the classpath then working directory
	 * 		for that file.
	 * </p>
	 *
	 * @return The icon file to use for this servlet.
	 * @throws IOException If icon file could not be loaded.
	 */
	protected StreamResource createFavIcon() throws IOException {
		for (RestResource r : restResourceAnnotationsChildFirst.values()) {
			if (! r.favicon().isEmpty()) {
				String path = getVarResolver().resolve(r.favicon());
				InputStream is = getResource(path, null);
				if (is != null) {
					try {
						return new StreamResource(is, "image/x-icon");
					} finally {
						is.close();
					}
				}
			}
		}
		return null;
	}

	/**
	 * Creates the static files map for this servlet.
	 * <p>
	 * 	This map defines static files that can be served up through subpaths on this servlet.
	 * 	The map keys are subpaths (e.g. <js>"htdocs"</js>) and the values are locations to look in
	 * 		the classpath and working directory for those files.
	 * </p>
	 * <p>
	 * 	Subclasses can override this method to provide their own mappings.
	 * </p>
	 * <p>
	 * 	The default implementation uses the {@link RestResource#staticFiles() @RestResource.staticFiles()} annotation
	 * 		to determine the mappings.
	 * </p>
	 *
	 * @return The list of static file mappings.
	 * @throws ParseException
	 */
	@SuppressWarnings("unchecked")
	protected Map<String,String> createStaticFilesMap() throws ParseException {
		Map<String,String> m = new LinkedHashMap<String,String>();
		for (RestResource r : restResourceAnnotationsParentFirst.values())
			if (! r.staticFiles().isEmpty())
				m.putAll(JsonParser.DEFAULT.parseMap(getVarResolver().resolve(r.staticFiles()), LinkedHashMap.class, String.class, String.class));
		return m;
	}

	/**
	 * Returns the config manager used to create the config file in {@link #createConfigFile()}.
	 * <p>
	 * 	The default implementation return {@link ConfigMgr#DEFAULT}, but subclasses can override
	 * 		this if they want to provide their own customized config manager.
	 * </p>
	 *
	 * @return The config file manager.
	 */
	protected ConfigMgr getConfigMgr() {
		return ConfigMgr.DEFAULT;
	}

	/**
	 * Returns the logger associated with this servlet.
	 * <p>
	 * 	Subclasses can override this method to provide their own Java Logging logger.
	 * </p>
	 * <p>
	 * 	Subclasses that use other logging libraries such as Apache Commons Logging should
	 * 		override the {@link #log(Level, Throwable, String, Object...)} method instead.
	 * </p>
	 *
	 * @return The logger associated with this servlet.
	 */
	protected JuneauLogger getLogger() {
		if (logger == null)
			logger =  JuneauLogger.getLogger(getClass());
		return logger;
	}

	private abstract class ResourceMethod {
		abstract int invoke(String methodName, String pathInfo, RestServlet resource, RestRequest req, RestResponse res) throws RestException;

		void complete() {
			// Do nothing by default.
		}
	}

	static enum ParamType {
		REQ, RES, PATH, BODY, HEADER, METHOD, FORMDATA, QUERY, HASFORMDATA, HASQUERY, PATHREMAINDER, PROPS, MESSAGES;

		boolean isOneOf(ParamType...pt) {
			for (ParamType ptt : pt)
				if (this == ptt)
					return true;
			return false;
		}

		public String getSwaggerParameterType() {
			switch(this) {
				case PATH: return "path";
				case HEADER: return "header";
				case FORMDATA: return "formData";
				case QUERY: return "query";
				case BODY: return "body";
				default: return null;
			}
		}
	}

	static class MethodParam {

		ParamType paramType;
		Type type;
		String name = "";
		boolean multiPart, plainParams;

		MethodParam(MethodMeta mm, Type type, Method method, Annotation[] annotations) throws ServletException {
			this.type = type;
			boolean isClass = type instanceof Class;
			if (isClass && isParentClass(HttpServletRequest.class, (Class)type))
				paramType = REQ;
			else if (isClass && isParentClass(HttpServletResponse.class, (Class)type))
				paramType = RES;
			else for (Annotation a : annotations) {
				if (a instanceof Path) {
					Path a2 = (Path)a;
					paramType = PATH;
					name = a2.value();
				} else if (a instanceof Header) {
					Header h = (Header)a;
					paramType = HEADER;
					name = h.value();
				} else if (a instanceof FormData) {
					FormData p = (FormData)a;
					if (p.multipart())
						assertCollection(type, method);
					paramType = FORMDATA;
					multiPart = p.multipart();
					plainParams = p.format().equals("INHERIT") ? mm.mPlainParams : p.format().equals("PLAIN");
					name = p.value();
				} else if (a instanceof Query) {
					Query p = (Query)a;
					if (p.multipart())
						assertCollection(type, method);
					paramType = QUERY;
					multiPart = p.multipart();
					plainParams = p.format().equals("INHERIT") ? mm.mPlainParams : p.format().equals("PLAIN");
					name = p.value();
				} else if (a instanceof HasFormData) {
					HasFormData p = (HasFormData)a;
					paramType = HASFORMDATA;
					name = p.value();
				} else if (a instanceof HasQuery) {
					HasQuery p = (HasQuery)a;
					paramType = HASQUERY;
					name = p.value();
				} else if (a instanceof Body) {
					paramType = BODY;
				} else if (a instanceof org.apache.juneau.server.annotation.Method) {
					paramType = METHOD;
					if (type != String.class)
						throw new ServletException("@Method parameters must be of type String");
				} else if (a instanceof PathRemainder) {
					paramType = PATHREMAINDER;
					if (type != String.class)
						throw new ServletException("@PathRemainder parameters must be of type String");
				} else if (a instanceof Properties) {
					paramType = PROPS;
					name = "PROPERTIES";
				} else if (a instanceof Messages) {
					paramType = MESSAGES;
					name = "MESSAGES";
				}
			}
			if (paramType == null)
				paramType = PATH;
		}

		/**
		 * Throws an exception if the specified type isn't an array or collection.
		 */
		private void assertCollection(Type t, Method m) throws ServletException {
			ClassMeta<?> cm = BeanContext.DEFAULT.getClassMeta(t);
			if (! (cm.isArray() || cm.isCollection()))
				throw new ServletException("Use of multipart flag on parameter that's not an array or Collection on method" + m);
		}

		@SuppressWarnings("unchecked")
		private Object getValue(RestRequest req, RestResponse res) throws Exception {
			BeanContext bc = req.getServlet().getBeanContext();
			switch(paramType) {
				case REQ:        return req;
				case RES:        return res;
				case PATH:       return req.getPathParameter(name, type);
				case BODY:       return req.getBody(type);
				case HEADER:     return req.getHeader(name, type);
				case METHOD:     return req.getMethod();
				case FORMDATA: {
					if (multiPart)
						return req.getFormDataParameters(name, type);
					if (plainParams)
						return bc.convertToType(req.getFormDataParameter(name), bc.getClassMeta(type));
					return req.getFormDataParameter(name, type);
				}
				case QUERY: {
					if (multiPart)
						return req.getQueryParameters(name, type);
					if (plainParams)
						return bc.convertToType(req.getQueryParameter(name), bc.getClassMeta(type));
					return req.getQueryParameter(name, type);
				}
				case HASFORMDATA:   return bc.convertToType(req.hasFormDataParameter(name), bc.getClassMeta(type));
				case HASQUERY:      return bc.convertToType(req.hasQueryParameter(name), bc.getClassMeta(type));
				case PATHREMAINDER: return req.getPathRemainder();
				case PROPS:         return res.getProperties();
				case MESSAGES:      return req.getResourceBundle();
			}
			return null;
		}
	}

	/*
	 * Represents a single Java servlet method annotated with @RestMethod.
	 */
	private class MethodMeta extends ResourceMethod implements Comparable<MethodMeta>  {
		private String httpMethod;
		private java.lang.reflect.Method method;
		private UrlPathPattern pathPattern;
		private MethodParam[] params;
		private RestGuard[] guards;
		private RestMatcher[] optionalMatchers, requiredMatchers;
		private RestConverter[] mConverters;
		private SerializerGroup mSerializers;                   // Method-level serializers
		private ParserGroup mParsers;                           // Method-level parsers
		private EncoderGroup mEncoders;                         // Method-level encoders
		private UrlEncodingParser mUrlEncodingParser;           // Method-level URL parameter parser.
		private UrlEncodingSerializer mUrlEncodingSerializer;   // Method-level URL parameter serializer.
		private ObjectMap mProperties;                          // Method-level properties
		private Map<String,String> mDefaultRequestHeaders;      // Method-level default request headers
		private String mDefaultEncoding;
		private boolean mPlainParams, deprecated;
		private String description, tags, summary, externalDocs;
		private Integer priority;
		private org.apache.juneau.server.annotation.Parameter[] parameters;
		private Response[] responses;

		private MethodMeta(java.lang.reflect.Method method) throws RestServletException {
			try {
				this.method = method;

				RestMethod m = method.getAnnotation(RestMethod.class);
				if (m == null)
					throw new RestServletException("@RestMethod annotation not found on method ''{0}.{1}''", method.getDeclaringClass().getName(), method.getName());

				if (! m.description().isEmpty())
					description = m.description();
				if (! m.tags().isEmpty())
					tags = m.tags();
				if (! m.summary().isEmpty())
					summary = m.summary();
				if (! m.externalDocs().isEmpty())
					externalDocs = m.externalDocs();
				deprecated = m.deprecated();
				this.parameters = m.parameters();
				this.responses = m.responses();
				this.mSerializers = getSerializers();
				this.mParsers = getParsers();
				this.mUrlEncodingParser = getUrlEncodingParser();
				this.mUrlEncodingSerializer = getUrlEncodingSerializer();
				this.mProperties = getProperties();
				this.mEncoders = getEncoders();

				ArrayList<Inherit> si = new ArrayList<Inherit>(Arrays.asList(m.serializersInherit()));
				ArrayList<Inherit> pi = new ArrayList<Inherit>(Arrays.asList(m.parsersInherit()));

				if (m.serializers().length > 0 || m.parsers().length > 0 || m.properties().length > 0 || m.beanFilters().length > 0 || m.pojoSwaps().length > 0) {
					mSerializers = (si.contains(SERIALIZERS) || m.serializers().length == 0 ? mSerializers.clone() : new SerializerGroup());
					mParsers = (pi.contains(PARSERS) || m.parsers().length == 0 ? mParsers.clone() : new ParserGroup());
					mUrlEncodingParser = mUrlEncodingParser.clone();
				}

				httpMethod = m.name().toUpperCase(Locale.ENGLISH);
				if (httpMethod.equals("") && method.getName().startsWith("do"))
					httpMethod = method.getName().substring(2).toUpperCase(Locale.ENGLISH);
				if (httpMethod.equals(""))
					throw new RestServletException("@RestMethod name not specified on method ''{0}.{1}''", method.getDeclaringClass().getName(), method.getName());
				if (httpMethod.equals("METHOD"))
					httpMethod = "*";

				priority = m.priority();

				String p = m.path();
				mConverters = new RestConverter[m.converters().length];
				for (int i = 0; i < mConverters.length; i++)
					mConverters[i] = m.converters()[i].newInstance();

				guards = new RestGuard[m.guards().length];
				for (int i = 0; i < guards.length; i++)
					guards[i] = m.guards()[i].newInstance();

				List<RestMatcher> optionalMatchers = new LinkedList<RestMatcher>(), requiredMatchers = new LinkedList<RestMatcher>();
				for (int i = 0; i < m.matchers().length; i++) {
					Class<? extends RestMatcher> c = m.matchers()[i];
					RestMatcher matcher = null;
					if (ClassUtils.isParentClass(RestMatcherReflecting.class, c))
						matcher = c.getConstructor(RestServlet.class, Method.class).newInstance(RestServlet.this, method);
					else
						matcher = c.newInstance();
					if (matcher.mustMatch())
						requiredMatchers.add(matcher);
					else
						optionalMatchers.add(matcher);
				}
				if (! m.clientVersion().isEmpty())
					requiredMatchers.add(new ClientVersionMatcher(RestServlet.this, method));

				this.requiredMatchers = requiredMatchers.toArray(new RestMatcher[requiredMatchers.size()]);
				this.optionalMatchers = optionalMatchers.toArray(new RestMatcher[optionalMatchers.size()]);

				if (m.serializers().length > 0) {
					mSerializers.append(m.serializers());
					if (si.contains(TRANSFORMS))
						mSerializers.addBeanFilters(getBeanFilters()).addPojoSwaps(getPojoSwaps());
					if (si.contains(PROPERTIES))
						mSerializers.setProperties(getProperties());
				}

				if (m.parsers().length > 0) {
					mParsers.append(m.parsers());
					if (pi.contains(TRANSFORMS))
						mParsers.addBeanFilters(getBeanFilters()).addPojoSwaps(getPojoSwaps());
					if (pi.contains(PROPERTIES))
						mParsers.setProperties(getProperties());
				}

				if (m.properties().length > 0) {
					mProperties = new ObjectMap().setInner(getProperties());
					for (Property p1 : m.properties()) {
						String n = p1.name(), v = p1.value();
						mProperties.put(n, v);
						mSerializers.setProperty(n, v);
						mParsers.setProperty(n, v);
						mUrlEncodingParser.setProperty(n, v);
					}
				}

				if (m.beanFilters().length > 0) {
					mSerializers.addBeanFilters(m.beanFilters());
					mParsers.addBeanFilters(m.beanFilters());
					mUrlEncodingParser.addBeanFilters(m.beanFilters());
				}

				if (m.pojoSwaps().length > 0) {
					mSerializers.addPojoSwaps(m.pojoSwaps());
					mParsers.addPojoSwaps(m.pojoSwaps());
					mUrlEncodingParser.addPojoSwaps(m.pojoSwaps());
				}

				if (m.encoders().length > 0 || ! m.inheritEncoders()) {
					EncoderGroup g = new EncoderGroup();
					if (m.inheritEncoders())
						g.append(mEncoders);
					else
						g.append(IdentityEncoder.INSTANCE);

					for (Class<? extends Encoder> c : m.encoders()) {
						try {
							g.append(c);
						} catch (Exception e) {
							throw new RestServletException("Exception occurred while trying to instantiate Encoder ''{0}''", c.getSimpleName()).initCause(e);
						}
					}
					mEncoders = g;
				}

				mDefaultRequestHeaders = new TreeMap<String,String>(String.CASE_INSENSITIVE_ORDER);
				for (String s : m.defaultRequestHeaders()) {
					String[] h = parseHeader(s);
					if (h == null)
						throw new RestServletException("Invalid default request header specified: ''{0}''.  Must be in the format: ''Header-Name: header-value''", s);
					mDefaultRequestHeaders.put(h[0], h[1]);
				}

				mDefaultEncoding = mProperties.getString(REST_defaultCharset, RestServlet.this.context.defaultCharset);
				String paramFormat = mProperties.getString(REST_paramFormat, RestServlet.this.context.paramFormat);
				mPlainParams = paramFormat.equals("PLAIN");

				pathPattern = new UrlPathPattern(p);

				int attrIdx = 0;
				Type[] pt = method.getGenericParameterTypes();
				Annotation[][] pa = method.getParameterAnnotations();
				params = new MethodParam[pt.length];
				for (int i = 0; i < params.length; i++) {
					params[i] = new MethodParam(this, pt[i], method, pa[i]);
					if (params[i].paramType == PATH && params[i].name.isEmpty()) {
						if (pathPattern.vars.length <= attrIdx)
							throw new RestServletException("Number of attribute parameters in method ''{0}'' exceeds the number of URL pattern variables.", method.getName());
						params[i].name = pathPattern.vars[attrIdx++];
					}
				}

				mSerializers.lock();
				mParsers.lock();
				mUrlEncodingParser.lock();

				// Need this to access methods in anonymous inner classes.
				method.setAccessible(true);
			} catch (Exception e) {
				throw new RestServletException("Exception occurred while initializing method ''{0}''", method.getName()).initCause(e);
			}
		}

		private Operation getSwaggerOperation(RestRequest req) throws ParseException {
			Operation o = Operation.create()
				.setOperationId(method.getName())
				.setDescription(getDescription(req))
				.setTags(getTags(req))
				.setSummary(getSummary(req))
				.setExternalDocs(getExternalDocs(req))
				.setParameters(getParameters(req))
				.setResponses(getResponses(req));

			if (isDeprecated())
				o.setDeprecated(true);

			if (! mParsers.getSupportedMediaTypes().equals(getParsers().getSupportedMediaTypes()))
				o.setConsumes(mParsers.getSupportedMediaTypes());

			if (! mSerializers.getSupportedMediaTypes().equals(getSerializers().getSupportedMediaTypes()))
				o.setProduces(mSerializers.getSupportedMediaTypes());

			return o;
		}

		private Operation getSwaggerOperationFromFile(RestRequest req) {
			Swagger s = req.getSwaggerFromFile();
			if (s != null && s.getPaths() != null && s.getPaths().get(pathPattern.patternString) != null)
				return s.getPaths().get(pathPattern.patternString).get(httpMethod);
			return null;
		}

		private String getDescription(RestRequest req) {
			VarResolverSession vr = req.getVarResolverSession();
			if (description != null)
				return vr.resolve(description);
			String description = msgs.findFirstString(req.getLocale(), method.getName() + ".description");
			if (description != null)
				return vr.resolve(description);
			Operation o = getSwaggerOperationFromFile(req);
			if (o != null)
				return o.getDescription();
			return null;
		}

		@SuppressWarnings("unchecked")
		private List<String> getTags(RestRequest req) {
			VarResolverSession vr = req.getVarResolverSession();
			JsonParser jp = JsonParser.DEFAULT;
			try {
				if (tags != null)
					return jp.parseCollection(vr.resolve(tags), ArrayList.class, String.class);
				String tags = msgs.findFirstString(req.getLocale(), method.getName() + ".tags");
				if (tags != null)
					return jp.parseCollection(vr.resolve(tags), ArrayList.class, String.class);
				Operation o = getSwaggerOperationFromFile(req);
				if (o != null)
					return o.getTags();
				return null;
			} catch (Exception e) {
				throw new RestException(SC_INTERNAL_SERVER_ERROR, e);
			}
		}

		private String getSummary(RestRequest req) {
			VarResolverSession vr = req.getVarResolverSession();
			if (summary != null)
				return vr.resolve(summary);
			String summary = msgs.findFirstString(req.getLocale(), method.getName() + ".summary");
			if (summary != null)
				return vr.resolve(summary);
			Operation o = getSwaggerOperationFromFile(req);
			if (o != null)
				return o.getSummary();
			return null;
		}

		private ExternalDocumentation getExternalDocs(RestRequest req) {
			VarResolverSession vr = req.getVarResolverSession();
			JsonParser jp = JsonParser.DEFAULT;
			try {
				if (externalDocs != null)
					return jp.parse(vr.resolve(externalDocs), ExternalDocumentation.class);
				String externalDocs = msgs.findFirstString(req.getLocale(), method.getName() + ".externalDocs");
				if (externalDocs != null)
					return jp.parse(vr.resolve(externalDocs), ExternalDocumentation.class);
				Operation o = getSwaggerOperationFromFile(req);
				if (o != null)
					return o.getExternalDocs();
				return null;
			} catch (Exception e) {
				throw new RestException(SC_INTERNAL_SERVER_ERROR, e);
			}
		}

		private boolean isDeprecated() {
			return deprecated;
		}

		private List<ParameterInfo> getParameters(RestRequest req) throws ParseException {
			Operation o = getSwaggerOperationFromFile(req);
			if (o != null && o.getParameters() != null)
				return o.getParameters();

			VarResolverSession vr = req.getVarResolverSession();
			JsonParser jp = JsonParser.DEFAULT;
			Map<String,ParameterInfo> m = new TreeMap<String,ParameterInfo>();

			// First parse @RestMethod.parameters() annotation.
			for (org.apache.juneau.server.annotation.Parameter v : parameters) {
				String in = vr.resolve(v.in());
				ParameterInfo p = ParameterInfo.createStrict(in, vr.resolve(v.name()));

				if (! v.description().isEmpty())
					p.setDescription(vr.resolve(v.description()));
				if (v.required())
					p.setRequired(v.required());

				if ("body".equals(in)) {
					if (! v.schema().isEmpty())
						p.setSchema(jp.parse(vr.resolve(v.schema()), SchemaInfo.class));
				} else {
					if (v.allowEmptyValue())
						p.setAllowEmptyValue(v.allowEmptyValue());
					if (! v.collectionFormat().isEmpty())
						p.setCollectionFormat(vr.resolve(v.collectionFormat()));
					if (! v._default().isEmpty())
						p.setDefault(vr.resolve(v._default()));
					if (! v.format().isEmpty())
						p.setFormat(vr.resolve(v.format()));
					if (! v.items().isEmpty())
						p.setItems(jp.parse(vr.resolve(v.items()), Items.class));
					p.setType(vr.resolve(v.type()));
				}
				m.put(p.getIn() + '.' + p.getName(), p);
			}

			// Next, look in resource bundle.
			String prefix = method.getName() + ".req";
			for (String key : msgs.keySet(prefix)) {
				if (key.length() > prefix.length()) {
					String value = vr.resolve(msgs.getString(key));
					String[] parts = key.substring(prefix.length() + 1).split("\\.");
					String in = parts[0], name, field;
					boolean isBody = "body".equals(in);
					if (parts.length == (isBody ? 2 : 3)) {
						if ("body".equals(in)) {
							name = null;
							field = parts[1];
						} else {
							name = parts[1];
							field = parts[2];
						}
						String k2 = in + '.' + name;
						ParameterInfo p = m.get(k2);
						if (p == null) {
							p = ParameterInfo.createStrict(in, name);
							m.put(k2, p);
						}

						if (field.equals("description"))
							p.setDescription(value);
						else if (field.equals("required"))
							p.setRequired(Boolean.valueOf(value));

						if ("body".equals(in)) {
							if (field.equals("schema"))
								p.setSchema(jp.parse(value, SchemaInfo.class));
						} else {
							if (field.equals("allowEmptyValue"))
								p.setAllowEmptyValue(Boolean.valueOf(value));
							else if (field.equals("collectionFormat"))
								p.setCollectionFormat(value);
							else if (field.equals("default"))
								p.setDefault(value);
							else if (field.equals("format"))
								p.setFormat(value);
							else if (field.equals("items"))
								p.setItems(jp.parse(value, Items.class));
							else if (field.equals("type"))
								p.setType(value);
						}
					} else {
						System.err.println("Unknown bundle key '"+key+"'");
					}
				}
			}

			// Finally, look for parameters defined on method.
			for (MethodParam mp : this.params) {
				String in = mp.paramType.getSwaggerParameterType();
				if (in != null) {
					String k2 = in + '.' + ("body".equals(in) ? null : mp.name);
					ParameterInfo p = m.get(k2);
					if (p == null) {
						p = ParameterInfo.createStrict(in, mp.name);
						m.put(k2, p);
					}
				}
			}

			if (m.isEmpty())
				return null;
			return new ArrayList<ParameterInfo>(m.values());
		}

		@SuppressWarnings("unchecked")
		private Map<String,ResponseInfo> getResponses(RestRequest req) throws ParseException {
			Operation o = getSwaggerOperationFromFile(req);
			if (o != null && o.getResponses() != null)
				return o.getResponses();

			VarResolverSession vr = req.getVarResolverSession();
			JsonParser jp = JsonParser.DEFAULT;
			Map<String,ResponseInfo> m = new TreeMap<String,ResponseInfo>();
			Map<String,HeaderInfo> m2 = new TreeMap<String,HeaderInfo>();

			// First parse @RestMethod.parameters() annotation.
			for (Response r : responses) {
				String httpCode = String.valueOf(r.value());
				String description = r.description().isEmpty() ? RestUtils.getHttpResponseText(r.value()) : vr.resolve(r.description());
				ResponseInfo r2 = ResponseInfo.create(description);

				if (r.headers().length > 0) {
					for (Parameter v : r.headers()) {
						HeaderInfo h = HeaderInfo.createStrict(vr.resolve(v.type()));
						if (! v.collectionFormat().isEmpty())
							h.setCollectionFormat(vr.resolve(v.collectionFormat()));
						if (! v._default().isEmpty())
							h.setDefault(vr.resolve(v._default()));
						if (! v.description().isEmpty())
							h.setDescription(vr.resolve(v.description()));
						if (! v.format().isEmpty())
							h.setFormat(vr.resolve(v.format()));
						if (! v.items().isEmpty())
							h.setItems(jp.parse(vr.resolve(v.items()), Items.class));
						r2.addHeader(v.name(), h);
						m2.put(httpCode + '.' + v.name(), h);
					}
				}
				m.put(httpCode, r2);
			}

			// Next, look in resource bundle.
			String prefix = method.getName() + ".res";
			for (String key : msgs.keySet(prefix)) {
				if (key.length() > prefix.length()) {
					String value = vr.resolve(msgs.getString(key));
					String[] parts = key.substring(prefix.length() + 1).split("\\.");
					String httpCode = parts[0];
					ResponseInfo r2 = m.get(httpCode);
					if (r2 == null) {
						r2 = ResponseInfo.create(null);
						m.put(httpCode, r2);
					}

					String name = parts.length > 1 ? parts[1] : "";

					if ("header".equals(name) && parts.length > 3) {
						String headerName = parts[2];
						String field = parts[3];

						String k2 = httpCode + '.' + headerName;
						HeaderInfo h = m2.get(k2);
						if (h == null) {
							h = HeaderInfo.createStrict("string");
							m2.put(k2, h);
							r2.addHeader(name, h);
						}
						if (field.equals("collectionFormat"))
							h.setCollectionFormat(value);
						else if (field.equals("default"))
							h.setDefault(value);
						else if (field.equals("description"))
							h.setDescription(value);
						else if (field.equals("format"))
							h.setFormat(value);
						else if (field.equals("items"))
							h.setItems(jp.parse(value, Items.class));
						else if (field.equals("type"))
							h.setType(value);

					} else if ("description".equals(name)) {
						r2.setDescription(value);
					} else if ("schema".equals(name)) {
						r2.setSchema(jp.parse(value, SchemaInfo.class));
					} else if ("examples".equals(name)) {
						r2.setExamples(jp.parseMap(value, TreeMap.class, String.class, Object.class));
					} else {
						System.err.println("Unknown bundle key '"+key+"'");
					}
				}
			}

			return m.isEmpty() ? null : m;
		}

		private boolean isRequestAllowed(RestRequest req) {
			for (RestGuard guard : guards) {
				req.javaMethod = method;
				if (! guard.isRequestAllowed(req))
					return false;
			}
			return true;
		}

		@Override /* ResourceMethod */
		int invoke(String methodName, String pathInfo, RestServlet resource, RestRequest req, RestResponse res) throws RestException {

			String[] patternVals = pathPattern.match(pathInfo);
			if (patternVals == null)
				return SC_NOT_FOUND;

			String remainder = null;
			if (patternVals.length > pathPattern.vars.length)
				remainder = patternVals[pathPattern.vars.length];
			for (int i = 0; i < pathPattern.vars.length; i++)
				req.setPathParameter(pathPattern.vars[i], patternVals[i]);

			req.init(method, remainder, createRequestProperties(mProperties, req), mDefaultRequestHeaders, mDefaultEncoding, mSerializers, mParsers, mUrlEncodingParser);
			res.init(req.getProperties(), mDefaultEncoding, mSerializers, mUrlEncodingSerializer, mEncoders);

			// Class-level guards
			for (RestGuard guard : getGuards())
				if (! guard.guard(req, res))
					return SC_UNAUTHORIZED;

			// If the method implements matchers, test them.
			for (RestMatcher m : requiredMatchers)
				if (! m.matches(req))
					return SC_PRECONDITION_FAILED;
			if (optionalMatchers.length > 0) {
				boolean matches = false;
				for (RestMatcher m : optionalMatchers)
					matches |= m.matches(req);
				if (! matches)
					return SC_PRECONDITION_FAILED;
			}

			onPreCall(req);

			Object[] args = new Object[params.length];
			for (int i = 0; i < params.length; i++) {
				try {
					args[i] = params[i].getValue(req, res);
				} catch (RestException e) {
					throw e;
				} catch (Exception e) {
					throw new RestException(SC_BAD_REQUEST,
						"Invalid data conversion.  Could not convert {0} ''{1}'' to type ''{2}'' on method ''{3}.{4}''.",
						params[i].paramType.name(), params[i].name, params[i].type, method.getDeclaringClass().getName(), method.getName()
					).initCause(e);
				}
			}

			try {

				for (RestGuard guard : guards)
					if (! guard.guard(req, res))
						return SC_OK;

				Object output = method.invoke(resource, args);
				if (! method.getReturnType().equals(Void.TYPE))
					if (output != null || ! res.getOutputStreamCalled())
						res.setOutput(output);

				onPostCall(req, res);

				if (res.hasOutput()) {
					output = res.getOutput();
					for (RestConverter converter : mConverters)
						output = converter.convert(req, output, getBeanContext().getClassMetaForObject(output));
					res.setOutput(output);
				}
			} catch (IllegalArgumentException e) {
				throw new RestException(SC_BAD_REQUEST,
					"Invalid argument type passed to the following method: ''{0}''.\n\tArgument types: {1}",
					method.toString(), ClassUtils.getReadableClassNames(args)
				);
			} catch (InvocationTargetException e) {
				Throwable e2 = e.getTargetException();		// Get the throwable thrown from the doX() method.
				if (e2 instanceof RestException)
					throw (RestException)e2;
				if (e2 instanceof ParseException)
					throw new RestException(SC_BAD_REQUEST, e2);
				if (e2 instanceof InvalidDataConversionException)
					throw new RestException(SC_BAD_REQUEST, e2);
				throw new RestException(SC_INTERNAL_SERVER_ERROR, e2);
			} catch (RestException e) {
				throw e;
			} catch (Exception e) {
				throw new RestException(SC_INTERNAL_SERVER_ERROR, e);
			}
			return SC_OK;
		}

		@Override /* Object */
		public String toString() {
			return "SimpleMethod: name=" + httpMethod + ", path=" + pathPattern.patternString;
		}

		/*
		 * compareTo() method is used to keep SimpleMethods ordered in the MultiMethod.tempCache list.
		 * It maintains the order in which matches are made during requests.
		 */
		@Override /* Comparable */
		public int compareTo(MethodMeta o) {
			int c;

			c = priority.compareTo(o.priority);
			if (c != 0)
				return c;

			c = pathPattern.compareTo(o.pathPattern);
			if (c != 0)
				return c;

			c = Utils.compare(o.requiredMatchers.length, requiredMatchers.length);
			if (c != 0)
				return c;

			c = Utils.compare(o.optionalMatchers.length, optionalMatchers.length);
			if (c != 0)
				return c;

			c = Utils.compare(o.guards.length, guards.length);
			if (c != 0)
				return c;

			return 0;
		}

		@Override /* Object */
		public boolean equals(Object o) {
			if (! (o instanceof MethodMeta))
				return false;
			return (compareTo((MethodMeta)o) == 0);
		}

		@Override /* Object */
		public int hashCode() {
			return super.hashCode();
		}
	}

	/*
	 * Represents a group of SimpleMethods that all belong to the same HTTP method (e.g. "GET").
	 */
	private class MultiMethod extends ResourceMethod {
		MethodMeta[] childMethods;
		List<MethodMeta> tempCache = new LinkedList<MethodMeta>();
		Set<String> collisions = new HashSet<String>();

		private MultiMethod(MethodMeta... simpleMethods) throws RestServletException {
			for (MethodMeta m : simpleMethods)
				addSimpleMethod(m);
		}

		private void addSimpleMethod(MethodMeta m) throws RestServletException {
			if (m.guards.length == 0 && m.requiredMatchers.length == 0 && m.optionalMatchers.length == 0) {
				String p = m.httpMethod + ":" + m.pathPattern.toRegEx();
				if (collisions.contains(p))
					throw new RestServletException("Duplicate Java methods assigned to the same method/pattern:  method=''{0}'', path=''{1}''", m.httpMethod, m.pathPattern);
				collisions.add(p);
			}
			tempCache.add(m);
		}

		@Override /* ResourceMethod */
		void complete() {
			Collections.sort(tempCache);
			collisions = null;
			childMethods = tempCache.toArray(new MethodMeta[tempCache.size()]);
		}

		@Override /* ResourceMethod */
		int invoke(String methodName, String pathInfo, RestServlet resource, RestRequest req, RestResponse res) throws RestException {
			int maxRc = 0;
			for (MethodMeta m : childMethods) {
				int rc = m.invoke(methodName, pathInfo, resource, req, res);
				//if (rc == SC_UNAUTHORIZED)
				//	return SC_UNAUTHORIZED;
				if (rc == SC_OK)
					return SC_OK;
				maxRc = Math.max(maxRc, rc);
			}
			return maxRc;
		}

		@Override /* Object */
		public String toString() {
			StringBuilder sb = new StringBuilder("MultiMethod: [\n");
			for (MethodMeta sm : childMethods)
				sb.append("\t" + sm + "\n");
			sb.append("]");
			return sb.toString();
		}
	}

	/**
	 * Returns the variable resolver for this servlet created by the {@link #createVarResolver()} method.
	 * <p>
	 * 	Variable resolvers are used to replace variables in property values.
	 * </p>
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
	 * <p>
	 * 	A typical usage pattern is using variables for resolving URL links when rendering HTML:
	 * </p>
	 * <p class='bcode'>
	 * 	<ja>@RestMethod</ja>(
	 * 		name=<js>"GET"</js>, path=<js>"/{name}/*"</js>,
	 * 		properties={
	 * 			<ja>@Property</ja>(
	 * 				name=<jsf>HTMLDOC_links</jsf>,
	 * 				value=<js>"{up:'$R{requestParentURI}', options:'?method=OPTIONS', editLevel:'$R{servletURI}/editLevel?logger=$R{attribute.name}'}"</js>
	 * 			)
	 * 		}
	 * 	)
	 * 	<jk>public</jk> LoggerEntry getLogger(RestRequest req, <ja>@Path</ja> String name) <jk>throws</jk> Exception {
	 * </p>
	 * <p>
	 * 	Calls to <code>req.getProperties().getString(<js>"key"</js>)</code> returns strings with variables resolved.
	 * </p>
	 *
	 * @return The var resolver created by {@link #createVarResolver()}.
	 */
	protected VarResolver getVarResolver() {
		return varResolver;
	}

	/**
	 * Returns the config file associated with this servlet.
	 *
	 * @return The resolving config file associated with this servlet, or <jk>null</jk> if there is no config file associated with this servlet.
	 */
	protected ConfigFile getConfig() {
		if (resolvingConfigFile == null) {
			ConfigFile cf = configFile;
			if (cf != null)
				resolvingConfigFile = cf.getResolving(getVarResolver());
		}
		return resolvingConfigFile;
	}

	/**
	 * Creates the reusable variable resolver for this servlet.
	 * <p>
	 * 	Subclasses can override this method to provide their own or augment the existing
	 * 		variable provider.
	 * </p>
	 * <ul class='spaced-list'>
	 * 	<li><code>$C{...}</code> - Values from the config file returned by {@link #getConfig()}.
	 * 	<li><code>$S{...}</code> - System properties.
	 * 	<li><code>$E{...}</code> - Environment variables.
	 * 	<li><code>$I{...}</code> - Servlet initialization parameters.
	 * </ul>
	 * <p>
	 * 	All variables can provide a 2nd parameter as a default value.
	 * </p>
	 * <p>
	 * 	Example: <js>$S{myBooleanProperty,true}"</js>.
	 * </p>
	 * <p>
	 * 	Like all other variables, keys and default values can themselves be arbitrarily nested.
	 * </p>
	 * <p>
	 * 	Example: <js>$S{$E{BOOLEAN_PROPERTY_NAME},$E{BOOLEAN_DEFAULT}}"</js>.
	 * </p>
	 * <p>
	 * 	Subclasses can augment this list by adding their own variables.
	 * </p>
	 * <h6 class='topic'>Example:</h6>
	 * <p class='bcode'>
	 * 	<ja>@Override</ja>
	 * 	<jk>protected</jk> StringVarResolver createVarResolver() {
	 *
	 * 		StringVarResolver r = <jk>super</jk>.createVarResolver();
	 *
	 * 		<jc>// Wrap all strings inside [] brackets
	 * 		// e.g. "$BRACKET{foobar}" -> "[foobar]"</jc>
	 * 		r.addVar(<js>"BRACKET"</js>, <jk>new</jk> StringVar() {
	 * 			<ja>@Override</ja>
	 * 			<jk>public</jk> String resolve(String varVal) {
	 * 				<jk>return</jk> <js>'['</js> + varVal + <js>']'</js>;
	 * 			}
	 * 		});
	 *
	 * 		<jk>return</jk> s;
	 * 	}
	 * </p>
	 *
	 * @return The reusable variable resolver for this servlet.
	 */
	protected VarResolver createVarResolver() {
		return new VarResolver()
			.addVars(
				SystemPropertiesVar.class,
				EnvVariablesVar.class,
				ConfigFileVar.class
			);
	}

	/**
	 * Creates a properties map for the specified request.
	 * <p>
	 * 	This map will automatically resolve any <js>"$X{...}"</js> variables using the {@link VarResolver}
	 * 	returned by {@link #getVarResolver()}.
	 *
	 * @param methodProperties The method-level properties.
	 * @param req The HTTP servlet request.
	 * @return The new object map for the request.
	 */
	protected ObjectMap createRequestProperties(final ObjectMap methodProperties, final RestRequest req) {
		@SuppressWarnings("serial")
		ObjectMap m = new ObjectMap() {
			@Override /* Map */
			public Object get(Object key) {
				Object o = super.get(key);
				if (o == null) {
					String k = key.toString();
					if (k.indexOf('.') != -1) {
						String prefix = k.substring(0, k.indexOf('.'));
						String remainder = k.substring(k.indexOf('.')+1);
						if ("path".equals(prefix))
							return req.getPathParameter(remainder);
						if ("query".equals(prefix))
							return req.getQueryParameter(remainder);
						if ("formData".equals(prefix))
							return req.getFormDataParameter(remainder);
						if ("header".equals(prefix))
							return req.getHeader(remainder);
					}
					if (k.equals(SERIALIZER_absolutePathUriBase)) {
						int serverPort = req.getServerPort();
						String serverName = req.getServerName();
						return req.getScheme() + "://" + serverName + (serverPort == 80 || serverPort == 443 ? "" : ":" + serverPort);
					}
					if (k.equals(REST_servletPath))
						return req.getServletPath();
					if (k.equals(REST_servletURI))
						return req.getServletURI();
					if (k.equals(REST_relativeServletURI))
						return req.getRelativeServletURI();
					if (k.equals(REST_pathInfo))
						return req.getPathInfo();
					if (k.equals(REST_requestURI))
						return req.getRequestURI();
					if (k.equals(REST_method))
						return req.getMethod();
					if (k.equals(REST_servletTitle))
						return req.getServletTitle();
					if (k.equals(REST_servletDescription))
						return req.getServletDescription();
					if (k.equals(REST_methodSummary))
						return req.getMethodSummary();
					if (k.equals(REST_methodDescription))
						return req.getMethodDescription();
					o = req.getPathParameter(k);
					if (o == null)
						o = req.getHeader(k);
				}
				if (o instanceof String)
					o = req.getVarResolverSession().resolve(o.toString());
				return o;
			}
		};
		m.setInner(methodProperties);
		return m;
	}

	/**
	 * Returns the class-level properties associated with this servlet.
	 * <p>
	 * 	Created by the {@link #createGuards(ObjectMap)} method.
	 * <p>
	 * 	<b>Important note:</b>  The returned {@code Map} is mutable.  Therefore, subclasses are free to override
	 * 	or set additional initialization parameters in their {@code init()} method.
	 * <p>
	 * 	This method can be called from {@link HttpServlet#init(ServletConfig)} or {@link HttpServlet#init()}.
	 *
	 * <h6 class='topic'>Example:</h6>
	 * <p class='bcode'>
	 * 	<jc>// Old way of getting a boolean init parameter</jc>
	 * 	String s = getInitParam(<js>"allowMethodParam"</js>);
	 * 	if (s == <jk>null</jk>)
	 * 		s = <js>"false"</js>;
	 * 	<jk>boolean</jk> b = Boolean.parseBoolean(s);
	 *
	 * 	<jc>// New simplified way of getting a boolean init parameter</jc>
	 * 	<jk>boolean</jk> b = getProperties().getBoolean(<js>"allowMethodParam"</js>, <jk>false</jk>);
	 * </p>
	 *
	 * @return The resource properties as an {@link ObjectMap}.
	 */
	public ObjectMap getProperties() {
		if (properties == null)
			properties = createProperties();
		return properties;
	}

	/**
	 * Returns the class-level guards associated with this servlet.
	 * <p>
	 * Created by the {@link #createGuards(ObjectMap)} method.
	 *
	 * @return The class-level guards associated with this servlet.
	 */
	public RestGuard[] getGuards()  {
		return guards;
	}

	/**
	 * Returns the class-level bean filters associated with this servlet.
	 * <p>
	 * Created by the {@link #createBeanFilters()} method.
	 *
	 * @return The class-level bean filters associated with this servlet.
	 */
	public Class<?>[] getBeanFilters() {
		return beanFilters;
	}

	/**
	 * Returns the class-level POJO swaps associated with this servlet.
	 * <p>
	 * Created by the {@link #createPojoSwaps()} method.
	 *
	 * @return The class-level POJO swaps associated with this servlet.
	 */
	public Class<?>[] getPojoSwaps() {
		return pojoSwaps;
	}

	/**
	 * Returns the class-level converters associated with this servlet.
	 * <p>
	 * Created by the {@link #createConverters(ObjectMap)} method.
	 *
	 * @return The class-level converters associated with this servlet.
	 */
	public RestConverter[] getConverters() {
		return converters;
	}

	/**
	 * Returns the class-level default request headers associated with this servlet.
	 * <p>
	 * Created by the {@link #createDefaultRequestHeaders(ObjectMap)} method.
	 *
	 * @return The class-level default request headers associated with this servlet.
	 */
	public TreeMap<String,String> getDefaultRequestHeaders() {
		return defaultRequestHeaders;
	}

	/**
	 * Returns the class-level default response headers associated with this servlet.
	 * <p>
	 * Created by the {@link #createDefaultResponseHeaders(ObjectMap)} method.
	 *
	 * @return The class-level default response headers associated with this servlet.
	 */
	protected Map<String,Object> getDefaultResponseHeaders() {
		return defaultResponseHeaders;
	}

	/**
	 * Returns the class-level response handlers associated with this servlet.
	 * <p>
	 * Created by the {@link #createResponseHandlers(ObjectMap)} method.
	 *
	 * @return The class-level response handlers associated with this servlet.
	 */
	protected ResponseHandler[] getResponseHandlers() {
		return responseHandlers;
	}


	/**
	 * Returns the class-level encoder group associated with this servlet.
	 * <p>
	 * Created by the {@link #createEncoders(ObjectMap)} method.
	 *
	 * @return The class-level encoder group associated with this servlet.
	 */
	public EncoderGroup getEncoders() {
		return encoders;
	}

	/**
	 * Returns the serializer group containing serializers used for serializing output POJOs in HTTP responses.
	 * <p>
	 * Created by the {@link #createSerializers(ObjectMap, Class[], Class[])} method.
	 *
	 * @return The group of serializers.
	 */
	public SerializerGroup getSerializers() {
		return serializers;
	}

	/**
	 * Returns the parser group containing parsers used for parsing input into POJOs from HTTP requests.
	 * <p>
	 * Created by the {@link #createParsers(ObjectMap, Class[], Class[])} method.
	 *
	 * @return The group of parsers.
	 */
	public ParserGroup getParsers() {
		return parsers;
	}

	/**
	 * Returns the URL-encoding parser used for parsing URL query parameters.
	 * <p>
	 * Created by the {@link #createUrlEncodingParser(ObjectMap, Class[], Class[])} method.
	 *
	 * @return The URL-Encoding parser to use for parsing URL query parameters.
	 */
	public UrlEncodingParser getUrlEncodingParser() {
		return urlEncodingParser;
	}

	/**
	 * Returns the URL-encoding serializer used for serializing arguments in {@link Redirect} objects.
	 * <p>
	 * Created by the {@link #createUrlEncodingSerializer(ObjectMap, Class[], Class[])} method.
	 *
	 * @return The URL-Encoding serializer.
	 */
	public UrlEncodingSerializer getUrlEncodingSerializer() {
		return urlEncodingSerializer;
	}

	/**
	 * Returns the {@link MimetypesFileTypeMap}  that is used to determine the media types of files served up through <code>/htdocs</code>.
	 * <p>
	 * Created by the {@link #createMimetypesFileTypeMap(ObjectMap)} method.
	 *
	 * @return The reusable MIME-types map.
	 */
	public MimetypesFileTypeMap getMimetypesFileTypeMap() {
		return mimetypesFileTypeMap;
	}

	/**
	 * Returns the {@link BeanContext} object used for parsing path variables and header values.
	 * <p>
	 * Created by the {@link #createBeanContext(ObjectMap, Class[], Class[])} method.
	 *
	 * @return The bean context used for parsing path variables and header values.
	 */
	public BeanContext getBeanContext() {
		return beanContext;
	}

	/**
	 * Returns the name of the header used to identify the client version.
	 * <p>
	 * The default implementation pulls from the {@link RestResource#clientVersionHeader()} annotation.
	 * <p>
	 * Subclasses can override this method to programmatically specify a different header name.
	 *
	 * @return The name of the header used to identify the client version.
	 */
	public String getClientVersionHeader() {
		return clientVersionHeader;
	}

	/**
	 * Subclasses can override this method to provide a way to resolve interfaces
	 * 	in {@link org.apache.juneau.server.annotation.RestResource#children() @RestResource.children()} annotations.
	 *
	 * @param interfaceClass The interface to provide an implementation for.
	 * @return The servlet that implements the specified interface.
	 * @throws ServletException
	 */
	protected RestServlet resolveChild(Class<?> interfaceClass) throws ServletException {
		throw new ServletException("Invalid child class specified.  Must be an instance of RestServlet.  Class=["+interfaceClass.getName()+"]");
	}


	/**
	 * This method can be use to replace a child resource with another instance while
	 * 	the parent resource is still running.
	 *
	 * @param servlet The new servlet instance.
	 * @throws ServletException If a matching servlet could not be found.
	 */
	protected void replaceChild(RestServlet servlet) throws ServletException {
		synchronized (childResources) {
			RestServlet old = findRestServlet(servlet);
			if (old == null)
				throw new ServletException("Could not find servlet with class "+servlet.getClass().getName()+" for update.");
			RestServlet parent = old.getParent();
			servlet.setParent(parent);
			servlet.init(parent.getServletConfig());
			parent.childResources.put(servlet.getPath(), servlet);
			old.destroy();
		}
	}

	/*
	 * Finds the RestServlet matching the class of the specified servlet in all
	 * child decendents of this servlet.
	 * Returns null if it wasn't found.
	 */
	private RestServlet findRestServlet(RestServlet servlet) {
		for (RestServlet c : getChildResources().values()) {
			if (c.getClass().equals(servlet.getClass()))
				return c;
			RestServlet c2 = c.findRestServlet(servlet);
			if (c2 != null)
				return c2;
		}
		return null;
	}

	/**
	 * Same as {@link Class#getResourceAsStream(String)} except if it doesn't find the resource
	 * 	on this class, searches up the parent hierarchy chain.
	 * <p>
	 * 	If the resource cannot be found in the classpath, then an attempt is made to look in the
	 * 		JVM working directory.
	 * <p>
	 * 	If the <code>locale</code> is specified, then we look for resources whose name matches that locale.
	 * 	For example, if looking for the resource <js>"MyResource.txt"</js> for the Japanese locale, we will
	 * 	look for files in the following order:
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
			InputStream is = ReflectionUtils.getLocalizedResource(getClass(), name, locale);
			if (is == null && name.indexOf("..") == -1) {
				for (String n2 : FileUtils.getCandidateFileNames(name, locale)) {
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
	 * @throws IOException
	 */
	public String getResourceAsString(String name, Locale locale) throws IOException {
		String n = (locale == null || locale.toString().isEmpty() ? name : name + '|' + locale);
		if (! resourceStrings.containsKey(n)) {
			String s = IOUtils.read(getResource(name, locale));
			if (s == null)
				throw new IOException("Resource '"+name+"' not found.");
			resourceStrings.put(n, s);
		}
		return resourceStrings.get(n);
	}

	/**
	 * Reads the input stream from {@link #getResource(String, Locale)} and parses it into a POJO
	 * 	using the parser matched by the specified media type.
	 * <p>
	 * 	Useful if you want to load predefined POJOs from JSON files in your classpath.
	 * </p>
	 *
	 * @param c The class type of the POJO to create.
	 * @param mediaType The media type of the data in the stream (e.g. <js>"text/json"</js>)
	 * @param name The resource name (e.g. "htdocs/styles.css").
	 * @param locale Optional locale.
	 * @return The parsed resource, or <jk>null</jk> if the resource could not be found.
	 * @throws IOException
	 * @throws ServletException
	 */
	public <T> T getResource(Class<T> c, String mediaType, String name, Locale locale) throws IOException, ServletException {
		InputStream is = getResource(name, locale);
		if (is == null)
			return null;
		try {
			Parser p = getParsers().getParser(mediaType);
			if (p != null) {
				try {
					if (p.isReaderParser())
						return p.parse(new InputStreamReader(is, IOUtils.UTF8), c);
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
	 * Returns the session objects for the specified request.
	 * <p>
	 * The default implementation simply returns a single map containing <code>{'req':req}</code>.
	 *
	 * @param req The REST request.
	 * @return The session objects for that request.
	 */
	public Map<String,Object> getSessionObjects(RestRequest req) {
		Map<String,Object> m = new HashMap<String,Object>();
		m.put(RequestVar.SESSION_req, req);
		return m;
	}
}
