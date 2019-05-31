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
import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ObjectUtils.*;
import static org.apache.juneau.httppart.HttpPartType.*;
import static org.apache.juneau.rest.RestContext.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.httppart.bean.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.internal.HttpUtils;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.exception.*;
import org.apache.juneau.rest.util.*;
import org.apache.juneau.rest.widget.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;

/**
 * Represents a single Java servlet/resource method annotated with {@link RestMethod @RestMethod}.
 */
@ConfigurableContext(nocache=true)
public class RestMethodContext extends BeanContext implements Comparable<RestMethodContext>  {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	static final String PREFIX = "RestMethodContext";

	/**
	 * Configuration property:  Client version pattern matcher.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestMethodContext.clientVersion.s"</js>
	 * 	<li><b>Data type:</b>  <code>String</code>
	 * 	<li><b>Default:</b>  empty string
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link RestMethod#clientVersion()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * Specifies whether this method can be called based on the client version.
	 *
	 * <p>
	 * The client version is identified via the HTTP request header identified by
	 * {@link RestResource#clientVersionHeader() @RestResource(clientVersionHeader)} which by default is <js>"X-Client-Version"</js>.
	 *
	 * <p>
	 * This is a specialized kind of {@link RestMatcher} that allows you to invoke different Java methods for the same
	 * method/path based on the client version.
	 *
	 * <p>
	 * The format of the client version range is similar to that of OSGi versions.
	 *
	 * <p>
	 * In the following example, the Java methods are mapped to the same HTTP method and URL <js>"/foobar"</js>.
	 * <p class='bcode w800'>
	 * 	<jc>// Call this method if X-Client-Version is at least 2.0.
	 * 	// Note that this also matches 2.0.1.</jc>
	 * 	<ja>@RestMethod</ja>(name=<jsf>GET</jsf>, path=<js>"/foobar"</js>, clientVersion=<js>"2.0"</js>)
	 * 	<jk>public</jk> Object method1()  {...}
	 *
	 * 	<jc>// Call this method if X-Client-Version is at least 1.1, but less than 2.0.</jc>
	 * 	<ja>@RestMethod</ja>(name=<jsf>GET</jsf>, path=<js>"/foobar"</js>, clientVersion=<js>"[1.1,2.0)"</js>)
	 * 	<jk>public</jk> Object method2()  {...}
	 *
	 * 	<jc>// Call this method if X-Client-Version is less than 1.1.</jc>
	 * 	<ja>@RestMethod</ja>(name=<jsf>GET</jsf>, path=<js>"/foobar"</js>, clientVersion=<js>"[0,1.1)"</js>)
	 * 	<jk>public</jk> Object method3()  {...}
	 * </p>
	 *
	 * <p>
	 * It's common to combine the client version with transforms that will convert new POJOs into older POJOs for
	 * backwards compatibility.
	 * <p class='bcode w800'>
	 * 	<jc>// Call this method if X-Client-Version is at least 2.0.</jc>
	 * 	<ja>@RestMethod</ja>(name=<jsf>GET</jsf>, path=<js>"/foobar"</js>, clientVersion=<js>"2.0"</js>)
	 * 	<jk>public</jk> NewPojo newMethod()  {...}
	 *
	 * 	<jc>// Call this method if X-Client-Version is at least 1.1, but less than 2.0.</jc>
	 * 	<ja>@RestMethod</ja>(name=<jsf>GET</jsf>, path=<js>"/foobar"</js>, clientVersion=<js>"[1.1,2.0)"</js>, transforms={NewToOldPojoSwap.<jk>class</jk>})
	 * 	<jk>public</jk> NewPojo oldMethod() {
	 * 		<jk>return</jk> newMethod();
	 * 	}
	 *
	 * <p>
	 * Note that in the previous example, we're returning the exact same POJO, but using a transform to convert it into
	 * an older form.
	 * The old method could also just return back a completely different object.
	 * The range can be any of the following:
	 * <ul>
	 * 	<li><js>"[0,1.0)"</js> = Less than 1.0.  1.0 and 1.0.0 does not match.
	 * 	<li><js>"[0,1.0]"</js> = Less than or equal to 1.0.  Note that 1.0.1 will match.
	 * 	<li><js>"1.0"</js> = At least 1.0.  1.0 and 2.0 will match.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RestContext#REST_clientVersionHeader}
	 * </ul>
	 */
	public static final String RESTMETHOD_clientVersion = PREFIX + ".clientVersion.s";

	/**
	 * Configuration property:  Method-level matchers.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestMethodContext.matchers.lo"</js>
	 * 	<li><b>Data type:</b>  <code>List&lt;{@link RestMatcher} | Class&lt;? <jk>extends</jk> {@link RestMatcher}&gt;&gt;</code>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link RestMethod#matchers()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Associates one or more {@link RestMatcher RestMatchers} with the specified method.
	 *
	 * <p>
	 * If multiple matchers are specified, <b>ONE</b> matcher must pass.
	 * <br>Note that this is different than guards where <b>ALL</b> guards needs to pass.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Inner classes of the REST resource class are allowed.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='link'>{@doc juneau-rest-server.Matchers}
	 * </ul>
	 */
	public static final String RESTMETHOD_matchers = PREFIX + ".matchers.lo";

	/**
	 * Configuration property:  Resource method path.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestMethodContext.path.s"</js>
	 * 	<li><b>Data type:</b>  <code>String</code>
	 * 	<li><b>Default:</b>  <jk>null</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link RestMethod#path()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Identifies the URL subpath relative to the servlet class.
	 *
	 *
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		This method is only applicable for Java methods.
	 * 	<li>
	 * 		Slashes are trimmed from the path ends.
	 * 		<br>As a convention, you may want to start your path with <js>'/'</js> simple because it make it easier to read.
	 * </ul>
	 */
	public static final String RESTMETHOD_path = PREFIX + ".path.s";

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final String httpMethod;
	private final UrlPathPattern pathPattern;
	final RestMethodParam[] methodParams;
	private final RestGuard[] guards;
	private final RestMatcher[] optionalMatchers;
	private final RestMatcher[] requiredMatchers;
	private final RestConverter[] converters;
	private final RestMethodProperties properties;
	private final Integer priority;
	private final RestContext context;
	final java.lang.reflect.Method method;
	final MethodInfo info;
	final PropertyStore propertyStore;
	final SerializerGroup serializers;
	final ParserGroup parsers;
	final EncoderGroup encoders;
	final HttpPartSerializer partSerializer;
	final HttpPartParser partParser;
	final JsonSchemaGenerator jsonSchemaGenerator;
	final Map<String,Object>
		defaultRequestHeaders,
		defaultQuery,
		defaultFormData;
	final String defaultCharset;
	final long maxInput;
	final BeanContext beanContext;
	final Map<String,Widget> widgets;
	final List<MediaType>
		supportedAcceptTypes,
		supportedContentTypes;

	final Map<Class<?>,ResponseBeanMeta> responseBeanMetas = new ConcurrentHashMap<>();
	final Map<Class<?>,ResponsePartMeta> headerPartMetas = new ConcurrentHashMap<>();
	final Map<Class<?>,ResponsePartMeta> bodyPartMetas = new ConcurrentHashMap<>();
	final ResponseBeanMeta responseMeta;

	RestMethodContext(RestMethodContextBuilder b) throws ServletException {
		super(b.getPropertyStore());

		this.context = b.context;
		this.method = b.method;
		this.httpMethod = b.httpMethod;
		this.info = MethodInfo.of(method);

		PropertyStore ps = getPropertyStore();

		this.defaultCharset = getProperty(REST_defaultCharset, String.class, context.getDefaultCharset());

		this.maxInput = StringUtils.parseLongWithSuffix(getProperty(REST_maxInput, String.class, String.valueOf(context.getMaxInput())));

		this.serializers = SerializerGroup.create().append(getArrayProperty(REST_serializers, Object.class)).apply(ps).build();

		this.parsers = ParserGroup.create().append(getArrayProperty(REST_parsers, Object.class)).apply(ps).build();

		HttpPartParser hpp = context.getPartParser();
		if (hpp instanceof Parser) {
			Parser pp = (Parser)hpp;
			hpp = (HttpPartParser)pp.builder().apply(ps).build();
		}
		this.partParser = hpp;

		this.partSerializer = context.getPartSerializer();

		this.responseMeta = ResponseBeanMeta.create(info, ps);

		this.pathPattern = new UrlPathPattern(getProperty(RESTMETHOD_path, String.class, HttpUtils.detectHttpPath(method, true)));

		this.methodParams = context.findParams(info, false, pathPattern);

		this.converters = getInstanceArrayProperty(REST_converters, RestConverter.class, new RestConverter[0], context.getResourceResolver(), context.getResource(), this);

		this.guards = getInstanceArrayProperty(REST_guards, RestGuard.class, new RestGuard[0], context.getResourceResolver(), context.getResource(), this);

		List<RestMatcher> optionalMatchers = new LinkedList<>(), requiredMatchers = new LinkedList<>();
		for (RestMatcher matcher : getInstanceArrayProperty(RESTMETHOD_matchers, RestMatcher.class, new RestMatcher[0], context.getResourceResolver(), context.getResource(), this)) {
			if (matcher.mustMatch())
				requiredMatchers.add(matcher);
			else
				optionalMatchers.add(matcher);
		}
		String clientVersion = getProperty(RESTMETHOD_clientVersion, String.class, null);
		if (clientVersion != null)
			requiredMatchers.add(new ClientVersionMatcher(context.getClientVersionHeader(), info));

		this.requiredMatchers = requiredMatchers.toArray(new RestMatcher[requiredMatchers.size()]);
		this.optionalMatchers = optionalMatchers.toArray(new RestMatcher[optionalMatchers.size()]);

		this.encoders = b.encoders;
		this.jsonSchemaGenerator = b.jsonSchemaGenerator;
		this.beanContext = b.beanContext;
		this.properties = b.properties;
		this.propertyStore = b.propertyStore;
		this.defaultRequestHeaders = b.defaultRequestHeaders;
		this.defaultQuery = b.defaultQuery;
		this.defaultFormData = b.defaultFormData;
		this.priority = b.priority;
		this.widgets = unmodifiableMap(b.widgets);

		this.supportedAcceptTypes = getListProperty(REST_produces, MediaType.class, serializers.getSupportedMediaTypes());
		this.supportedContentTypes = getListProperty(REST_consumes, MediaType.class, parsers.getSupportedMediaTypes());
	}

	ResponseBeanMeta getResponseBeanMeta(Object o) {
		if (o == null)
			return null;
		Class<?> c = o.getClass();
		ResponseBeanMeta rbm = responseBeanMetas.get(c);
		if (rbm == null) {
			rbm = ResponseBeanMeta.create(c, serializers.getPropertyStore());
			if (rbm == null)
				rbm = ResponseBeanMeta.NULL;
			responseBeanMetas.put(c, rbm);
		}
		if (rbm == ResponseBeanMeta.NULL)
			return null;
		return rbm;
	}

	ResponsePartMeta getResponseHeaderMeta(Object o) {
		if (o == null)
			return null;
		Class<?> c = o.getClass();
		ResponsePartMeta pm = headerPartMetas.get(c);
		if (pm == null) {
			ResponseHeader a = c.getAnnotation(ResponseHeader.class);
			if (a != null) {
				HttpPartSchema schema = HttpPartSchema.create(a);
				HttpPartSerializer serializer = createPartSerializer(schema.getSerializer(), serializers.getPropertyStore(), partSerializer);
				pm = new ResponsePartMeta(HEADER, schema, serializer);
			}
			if (pm == null)
				pm = ResponsePartMeta.NULL;
			headerPartMetas.put(c, pm);
		}
		if (pm == ResponsePartMeta.NULL)
			return null;
		return pm;
	}

	ResponsePartMeta getResponseBodyMeta(Object o) {
		if (o == null)
			return null;
		Class<?> c = o.getClass();
		ResponsePartMeta pm = bodyPartMetas.get(c);
		if (pm == null) {
			ResponseBody a = c.getAnnotation(ResponseBody.class);
			if (a != null) {
				HttpPartSchema schema = HttpPartSchema.create(a);
				HttpPartSerializer serializer = createPartSerializer(schema.getSerializer(), serializers.getPropertyStore(), partSerializer);
				pm = new ResponsePartMeta(BODY, schema, serializer);
			}
			if (pm == null)
				pm = ResponsePartMeta.NULL;
			bodyPartMetas.put(c, pm);
		}
		if (pm == ResponsePartMeta.NULL)
			return null;
		return pm;
	}

	/**
	 * Returns <jk>true</jk> if this Java method has any guards or matchers.
	 */
	boolean hasGuardsOrMatchers() {
		return (guards.length != 0 || requiredMatchers.length != 0 || optionalMatchers.length != 0);
	}

	/**
	 * Returns the HTTP method name (e.g. <js>"GET"</js>).
	 */
	String getHttpMethod() {
		return httpMethod;
	}

	/**
	 * Returns the path pattern for this method.
	 */
	String getPathPattern() {
		return pathPattern.toString();
	}

	/**
	 * Returns <jk>true</jk> if the specified request object can call this method.
	 */
	boolean isRequestAllowed(RestRequest req) {
		for (RestGuard guard : guards) {
			req.setJavaMethod(method);
			if (! guard.isRequestAllowed(req))
				return false;
		}
		return true;
	}

	/**
	 * Workhorse method.
	 *
	 * @param pathInfo The value of {@link HttpServletRequest#getPathInfo()} (sorta)
	 * @return The HTTP response code.
	 */
	int invoke(String pathInfo, RestRequest req, RestResponse res) throws Throwable {

		String[] patternVals = pathPattern.match(pathInfo);
		if (patternVals == null)
			return SC_NOT_FOUND;

		String remainder = null;
		if (patternVals.length > pathPattern.getVars().length)
			remainder = patternVals[pathPattern.getVars().length];
		for (int i = 0; i < pathPattern.getVars().length; i++)
			req.getPathMatch().put(pathPattern.getVars()[i], patternVals[i]);
		req.getPathMatch().pattern(pathPattern.getPatternString()).remainder(remainder);

		RequestProperties requestProperties = new RequestProperties(req.getVarResolverSession(), properties);

		req.init(this, requestProperties);
		res.init(this, requestProperties);

		// Class-level guards
		for (RestGuard guard : context.getGuards())
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

		context.preCall(req, res);

		Object[] args = new Object[methodParams.length];
		for (int i = 0; i < methodParams.length; i++) {
			try {
				args[i] = methodParams[i].resolve(req, res);
			} catch (RestException e) {
				throw e;
			} catch (Exception e) {
				throw new BadRequest(e,
					"Invalid data conversion.  Could not convert {0} ''{1}'' to type ''{2}'' on method ''{3}.{4}''.",
					methodParams[i].getParamType().name(), methodParams[i].getName(), methodParams[i].getType(), info.getDeclaringClass().getFullName(), info.getSimpleName()
				);
			}
		}

		try {

			for (RestGuard guard : guards)
				if (! guard.guard(req, res))
					return SC_OK;

			Object output;
			try {
				output = method.invoke(context.getResource(), args);
				if (res.getStatus() == 0)
					res.setStatus(200);
				if (! method.getReturnType().equals(Void.TYPE)) {
					if (output != null || ! res.getOutputStreamCalled())
						res.setOutput(output);
				}
			} catch (InvocationTargetException e) {
				Throwable e2 = e.getTargetException();		// Get the throwable thrown from the doX() method.
				res.setStatus(500);
				if (getResponseBodyMeta(e2) != null || getResponseBeanMeta(e2) != null) {
					res.setOutput(e2);
				} else {
					throw e;
				}
			}

			context.postCall(req, res);

			if (res.hasOutput())
				for (RestConverter converter : converters)
					res.setOutput(converter.convert(req, res.getOutput()));

		} catch (IllegalArgumentException e) {
			throw new BadRequest(e,
				"Invalid argument type passed to the following method: ''{0}''.\n\tArgument types: {1}",
				info.toString(), info.getFullName()
			);
		} catch (InvocationTargetException e) {
			Throwable e2 = e.getTargetException();		// Get the throwable thrown from the doX() method.
			if (e2 instanceof RestException)
				throw (RestException)e2;
			if (e2 instanceof ParseException)
				throw new BadRequest(e2);
			if (e2 instanceof InvalidDataConversionException)
				throw new BadRequest(e2);
			throw e2;
		}
		return SC_OK;
	}

	@Override /* Object */
	public String toString() {
		return "SimpleMethod: name=" + httpMethod + ", path=" + pathPattern.getPatternString();
	}

	/*
	 * compareTo() method is used to keep SimpleMethods ordered in the RestCallRouter list.
	 * It maintains the order in which matches are made during requests.
	 */
	@Override /* Comparable */
	public int compareTo(RestMethodContext o) {
		int c;

		c = priority.compareTo(o.priority);
		if (c != 0)
			return c;

		c = pathPattern.compareTo(o.pathPattern);
		if (c != 0)
			return c;

		c = compare(o.requiredMatchers.length, requiredMatchers.length);
		if (c != 0)
			return c;

		c = compare(o.optionalMatchers.length, optionalMatchers.length);
		if (c != 0)
			return c;

		c = compare(o.guards.length, guards.length);
		if (c != 0)
			return c;

		return 0;
	}

	/**
	 * Bean property getter:  <property>serializers</property>.
	 *
	 * @return The value of the <property>serializers</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public SerializerGroup getSerializers() {
		return serializers;
	}

	/**
	 * Bean property getter:  <property>parsers</property>.
	 *
	 * @return The value of the <property>parsers</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public ParserGroup getParsers() {
		return parsers;
	}

	/**
	 * Bean property getter:  <property>partSerializer</property>.
	 *
	 * @return The value of the <property>partSerializer</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public HttpPartSerializer getPartSerializer() {
		return partSerializer;
	}

	/**
	 * Bean property getter:  <property>partParser</property>.
	 *
	 * @return The value of the <property>partParser</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public HttpPartParser getPartParser() {
		return partParser;
	}

	/**
	 * Returns the JSON-Schema generator applicable to this Java method.
	 *
	 * @return The JSON-Schema generator applicable to this Java method.
	 */
	public JsonSchemaGenerator getJsonSchemaGenerator() {
		return jsonSchemaGenerator;
	}

	@Override /* Object */
	public boolean equals(Object o) {
		if (! (o instanceof RestMethodContext))
			return false;
		return (compareTo((RestMethodContext)o) == 0);
	}

	@Override /* Object */
	public int hashCode() {
		return method.hashCode();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Utility methods.
	//-----------------------------------------------------------------------------------------------------------------
	static String[] resolveVars(VarResolver vr, String[] in) {
		String[] out = new String[in.length];
		for (int i = 0; i < in.length; i++)
			out[i] = vr.resolve(in[i]);
		return out;
	}

	static HttpPartSerializer createPartSerializer(Class<? extends HttpPartSerializer> c, PropertyStore ps, HttpPartSerializer _default) {
		HttpPartSerializer hps = castOrCreate(HttpPartSerializer.class, c, true, ps);
		return hps == null ? _default : hps;
	}
}