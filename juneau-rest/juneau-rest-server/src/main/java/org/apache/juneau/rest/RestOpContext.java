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

import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.ExceptionUtils.*;
import static org.apache.juneau.internal.ObjectUtils.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.httppart.HttpPartType.*;
import static org.apache.juneau.rest.HttpRuntimeException.*;
import static java.util.Collections.*;
import static java.util.Optional.*;
import java.lang.reflect.Method;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.part.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.httppart.bean.*;
import org.apache.juneau.internal.HttpUtils;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.logging.*;
import org.apache.juneau.rest.util.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.utils.*;

/**
 * Represents a single Java servlet/resource method annotated with {@link RestOp @RestOp}.
 * {@review}
 */
public class RestOpContext extends Context implements Comparable<RestOpContext>  {

	/** Represents a null value for the {@link RestOp#contextClass()} annotation.*/
	@SuppressWarnings("javadoc")
	public static final class Null extends RestOpContext {
		public Null(RestOpContextBuilder builder) throws Exception {
			super(builder);
		}
	}

	private final String httpMethod;
	private final UrlPathMatcher[] pathMatchers;
	private final RestOpArg[] opArgs;
	private final RestGuard[] guards;
	private final RestMatcher[] requiredMatchers, optionalMatchers;
	private final RestConverter[] converters;
	private final RestContext context;
	private final Method method;
	private final MethodInvoker methodInvoker;
	private final MethodInfo mi;
	private final BeanContext beanContext;
	private final SerializerGroup serializers;
	private final ParserGroup parsers;
	private final EncoderGroup encoders;
	private final HttpPartSerializer partSerializer;
	private final HttpPartParser partParser;
	private final JsonSchemaGenerator jsonSchemaGenerator;
	private final HeaderList defaultRequestHeaders, defaultResponseHeaders;
	private final PartList defaultRequestQueryData, defaultRequestFormData;
	private final NamedAttributeList defaultRequestAttributes;
	private final Charset defaultCharset;
	private final long maxInput;
	private final List<MediaType>
		supportedAcceptTypes,
		supportedContentTypes;
	private final RestLogger callLogger;

	private final Map<Class<?>,ResponseBeanMeta> responseBeanMetas = new ConcurrentHashMap<>();
	private final Map<Class<?>,ResponsePartMeta> headerPartMetas = new ConcurrentHashMap<>();
	private final Map<Class<?>,ResponsePartMeta> bodyPartMetas = new ConcurrentHashMap<>();
	private final ResponseBeanMeta responseMeta;
	private final int hierarchyDepth;
	private final DebugEnablement debug;

	/**
	 * Creator.
	 *
	 * @param method The Java method this context belongs to.
	 * @param context The Java class context.
	 * @return A new builder.
	 */
	public static RestOpContextBuilder create(java.lang.reflect.Method method, RestContext context) {
		return new RestOpContextBuilder(method, context);
	}

	/**
	 * Context constructor.
	 *
	 * @param builder The builder for this object.
	 * @throws ServletException If context could not be created.
	 */
	public RestOpContext(RestOpContextBuilder builder) throws ServletException {
		super(builder);

		try {
			context = builder.restContext;
			method = builder.restMethod;

			if (builder.debug == null)
				debug = context.getDebugEnablement();
			else
				debug = DebugEnablement.create().enable(builder.debug, "*").build();

			methodInvoker = new MethodInvoker(method, context.getMethodExecStats(method));
			mi = MethodInfo.of(method).accessible();
			Object r = context.getResource();

			BeanStore bs = BeanStore.of(context.getRootBeanStore(), r)
				.addBean(RestOpContext.class, this)
				.addBean(Method.class, method)
				.addBean(AnnotationWorkList.class, builder.getApplied());
			bs.addBean(BeanStore.class, bs);

			beanContext = bs.add(BeanContext.class, builder.getBeanContext().orElse(context.getBeanContext()));
			encoders = bs.add(EncoderGroup.class, builder.getEncoders().orElse(context.getEncoders()));
			serializers = bs.add(SerializerGroup.class, builder.getSerializers().orElse(context.getSerializers()));
			parsers = bs.add(ParserGroup.class, builder.getParsers().orElse(context.getParsers()));
			partSerializer = bs.add(HttpPartSerializer.class, builder.getPartSerializer().orElse(context.getPartSerializer()));
			partParser = bs.add(HttpPartParser.class, builder.getPartParser().orElse(context.getPartParser()));
			jsonSchemaGenerator = bs.add(JsonSchemaGenerator.class, builder.getJsonSchemaGenerator().orElse(context.getJsonSchemaGenerator()));
			converters = bs.add(RestConverter[].class, builder.converters().build().asArray());
			guards = bs.add(RestGuard[].class, builder.getGuards().asArray());

			RestMatcherList matchers = builder.getMatchers(context);
			optionalMatchers = matchers.getOptionalEntries();
			requiredMatchers = matchers.getRequiredEntries();

			pathMatchers = bs.add(UrlPathMatcher[].class, builder.getPathMatchers().asArray());
			bs.addBean(UrlPathMatcher.class, pathMatchers.length > 0 ? pathMatchers[0] : null);

			supportedAcceptTypes = unmodifiableList(ofNullable(builder.produces).orElse(serializers.getSupportedMediaTypes()));
			supportedContentTypes = unmodifiableList(ofNullable(builder.consumes).orElse(parsers.getSupportedMediaTypes()));

			defaultRequestHeaders = builder.defaultRequestHeaders().build();
			defaultResponseHeaders = builder.defaultResponseHeaders().build();
			defaultRequestQueryData = builder.defaultRequestQueryData().build();
			defaultRequestFormData = builder.defaultRequestFormData().build();
			defaultRequestAttributes = builder.defaultRequestAttributes().build();

			int _hierarchyDepth = 0;
			Class<?> sc = method.getDeclaringClass().getSuperclass();
			while (sc != null) {
				_hierarchyDepth++;
				sc = sc.getSuperclass();
			}
			hierarchyDepth = _hierarchyDepth;

			String _httpMethod = builder.httpMethod;
			if (_httpMethod == null)
				_httpMethod = HttpUtils.detectHttpMethod(method, true, "GET");
			if ("METHOD".equals(_httpMethod))
				_httpMethod = "*";
			httpMethod = _httpMethod.toUpperCase(Locale.ENGLISH);

			defaultCharset = ofNullable(builder.defaultCharset).orElse(context.defaultCharset);
			maxInput = ofNullable(builder.maxInput).orElse(context.maxInput);

			responseMeta = ResponseBeanMeta.create(mi, builder.getApplied());

			opArgs = context.findRestOperationArgs(mi.inner(), bs);

			this.callLogger = context.getCallLogger();
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}

	/**
	 * Returns the bean context associated with this context.
	 *
	 * @return The bean context associated with this context.
	 */
	public BeanContext getBeanContext() {
		return beanContext;
	}

	/**
	 * Returns metadata about the specified response object if it's annotated with {@link Response @Response}.
	 *
 	 * @param o The response POJO.
	 * @return Metadata about the specified response object, or <jk>null</jk> if it's not annotated with {@link Response @Response}.
	 */
	public ResponseBeanMeta getResponseBeanMeta(Object o) {
		if (o == null)
			return null;
		Class<?> c = o.getClass();
		ResponseBeanMeta rbm = responseBeanMetas.get(c);
		if (rbm == null) {
			rbm = ResponseBeanMeta.create(c, new AnnotationWorkList());
			if (rbm == null)
				rbm = ResponseBeanMeta.NULL;
			responseBeanMetas.put(c, rbm);
		}
		if (rbm == ResponseBeanMeta.NULL)
			return null;
		return rbm;
	}

	/**
	 * Returns metadata about the specified response object if it's annotated with {@link ResponseHeader @ResponseHeader}.
	 *
 	 * @param o The response POJO.
	 * @return Metadata about the specified response object, or <jk>null</jk> if it's not annotated with {@link ResponseHeader @ResponseHeader}.
	 */
	public ResponsePartMeta getResponseHeaderMeta(Object o) {
		if (o == null)
			return null;
		Class<?> c = o.getClass();
		ResponsePartMeta pm = headerPartMetas.get(c);
		if (pm == null) {
			ResponseHeader a = c.getAnnotation(ResponseHeader.class);
			if (a != null) {
				HttpPartSchema schema = HttpPartSchema.create(a);
				HttpPartSerializer serializer = createPartSerializer(schema.getSerializer(), partSerializer);
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

	/**
	 * Returns metadata about the specified response object if it's annotated with {@link ResponseBody @ResponseBody}.
	 *
 	 * @param o The response POJO.
	 * @return Metadata about the specified response object, or <jk>null</jk> if it's not annotated with {@link ResponseBody @ResponseBody}.
	 */
	public ResponsePartMeta getResponseBodyMeta(Object o) {
		if (o == null)
			return null;
		Class<?> c = o.getClass();
		ResponsePartMeta pm = bodyPartMetas.get(c);
		if (pm == null) {
			ResponseBody a = c.getAnnotation(ResponseBody.class);
			if (a != null) {
				HttpPartSchema schema = HttpPartSchema.create(a);
				HttpPartSerializer serializer = createPartSerializer(schema.getSerializer(), partSerializer);
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
	 * Returns the HTTP method name (e.g. <js>"GET"</js>).
	 *
	 * @return The HTTP method name.
	 */
	public String getHttpMethod() {
		return httpMethod;
	}

	/**
	 * Returns the path pattern for this method.
	 *
	 * @return The path pattern.
	 */
	public String getPathPattern() {
		return pathMatchers[0].toString();
	}

	/**
	 * Returns the serializers to use for this method.
	 *
	 * @return The serializers to use for this method.
	 */
	public SerializerGroup getSerializers() {
		return serializers;
	}

	/**
	 * Returns the parsers to use for this method.
	 *
	 * @return The parsers to use for this method.
	 */
	public ParserGroup getParsers() {
		return parsers;
	}

	/**
	 * Returns the compression encoders to use for this method.
	 *
	 * @return The compression encoders to use for this method.
	 */
	public EncoderGroup getEncoders() {
		return encoders;
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

	/**
	 * Returns the underlying Java method that this context belongs to.
	 *
	 * @return The underlying Java method that this context belongs to.
	 */
	public Method getJavaMethod() {
		return method;
	}

	/**
	 * Returns the default request headers.
	 *
	 * @return The default request headers.  Never <jk>null</jk>.
	 */
	public HeaderList getDefaultRequestHeaders() {
		return defaultRequestHeaders;
	}

	/**
	 * Returns the default response headers.
	 *
	 * @return The default response headers.  Never <jk>null</jk>.
	 */
	public HeaderList getDefaultResponseHeaders() {
		return defaultResponseHeaders;
	}

	/**
	 * Returns the default request query parameters.
	 *
	 * @return The default request query parameters.  Never <jk>null</jk>.
	 */
	public PartList getDefaultRequestQueryData() {
		return defaultRequestQueryData;
	}

	/**
	 * Returns the default form data parameters.
	 *
	 * @return The default form data parameters.  Never <jk>null</jk>.
	 */
	public PartList getDefaultRequestFormData() {
		return defaultRequestFormData;
	}

	/**
	 * Returns the default request attributes.
	 *
	 * @return The default request attributes.  Never <jk>null</jk>.
	 */
	public NamedAttributeList getDefaultRequestAttributes() {
		return defaultRequestAttributes;
	}

	/**
	 * Returns the default charset.
	 *
	 * @return The default charset.  Never <jk>null</jk>.
	 */
	public Charset getDefaultCharset() {
		return defaultCharset;
	}

	/**
	 * Returns the max number of bytes to process in the input body.
	 *
	 * @return The max number of bytes to process in the input body.
	 */
	public long getMaxInput() {
		return maxInput;
	}

	/**
	 * Returns the list of supported content types.
	 *
	 * @return An unmodifiable list.
	 */
	public List<MediaType> getSupportedContentTypes() {
		return supportedContentTypes;
	}

	/**
	 * Returns a list of supported accept types.
	 *
	 * @return An unmodifiable list.
	 */
	public List<MediaType> getSupportedAcceptTypes() {
		return supportedAcceptTypes;
	}

	/**
	 * Returns the response bean meta if this method returns a {@link Response}-annotated bean.
	 *
	 * @return The response bean meta or <jk>null</jk> if it's not a {@link Response}-annotated bean.
	 */
	public ResponseBeanMeta getResponseMeta() {
		return responseMeta;
	}

	/**
	 * Identifies if this method can process the specified call.
	 *
	 * <p>
	 * To process the call, the following must be true:
	 * <ul>
	 * 	<li>Path pattern must match.
	 * 	<li>Matchers (if any) must match.
	 * </ul>
	 *
	 * @param call The call to check.
	 * @return
	 * 	One of the following values:
	 * 	<ul>
	 * 		<li><c>0</c> - Path doesn't match.
	 * 		<li><c>1</c> - Path matched but matchers did not.
	 * 		<li><c>2</c> - Matches.
	 * 	</ul>
	 */
	protected int match(RestCall call) {

		UrlPathMatch pm = matchPattern(call);

		if (pm == null)
			return 0;

		if (requiredMatchers.length == 0 && optionalMatchers.length == 0) {
			call.urlPathMatch(pm);  // Cache so we don't have to recalculate.
			return 2;
		}

		try {
			HttpServletRequest req = call.getRequest();

			// If the method implements matchers, test them.
			for (RestMatcher m :  requiredMatchers)
				if (! m.matches(req))
					return 1;
			if (optionalMatchers.length > 0) {
				boolean matches = false;
				for (RestMatcher m : optionalMatchers)
					matches |= m.matches(req);
				if (! matches)
					return 1;
			}

			call.urlPathMatch(pm);  // Cache so we don't have to recalculate.
			return 2;
		} catch (Exception e) {
			throw new InternalServerError(e);
		}
	}

	/**
	 * Workhorse method.
	 *
	 * @param call Invokes the specified call against this Java method.
	 * @throws Throwable Typically an HTTP exception.  Anything else will result in an HTTP 500.
	 */
	protected void invoke(RestCall call) throws Throwable {

		call.restOpContext(this);

		RestRequest req = call.getRestRequest();
		RestResponse res = call.getRestResponse();

		context.preCall(call);

		call.logger(callLogger);

		call.debug(debug.isDebug(this, call.getRequest()));

		Object[] args = new Object[opArgs.length];
		for (int i = 0; i < opArgs.length; i++) {
			ParamInfo pi = methodInvoker.inner().getParam(i);
			try {
				args[i] = opArgs[i].resolve(call);
			} catch (Exception e) {
				throw toHttpException(e, BadRequest.class, "Could not convert resolve parameter {0} of type ''{1}'' on method ''{2}''.", i, pi.getParameterType(), mi.getFullName());
			}
		}

		try {

			for (RestGuard guard : guards)
				if (! guard.guard(req, res))
					return;

			Object output;
			try {
				output = methodInvoker.invoke(context.getResource(), args);

				// Handle manual call to req.setDebug().
				Boolean debug = req.getAttribute("Debug").asType(Boolean.class).orElse(null);
				if (debug == Boolean.TRUE) {
					call.debug(true);
				} else if (debug == Boolean.FALSE) {
					call.debug(false);
				}

				if (res.getStatus() == 0)
					res.setStatus(200);
				if (! method.getReturnType().equals(Void.TYPE)) {
					if (output != null || ! res.getOutputStreamCalled())
						res.setOutput(output);
				}
			} catch (ExecutableException e) {
				Throwable e2 = e.unwrap();  // Get the throwable thrown from the doX() method.
				res.setStatus(500);  // May be overridden later.
				Class<?> c = e2.getClass();
				if (e2 instanceof HttpResponse || c.getAnnotation(Response.class) != null || c.getAnnotation(ResponseBody.class) != null) {
					res.setOutput(e2);
				} else {
					throw e;
				}
			}

			context.postCall(call);

			Optional<Optional<Object>> o = res.getOutput();
			if (o.isPresent())
				for (RestConverter converter : converters)
					res.setOutput(converter.convert(req, o.get().orElse(null)));

		} catch (IllegalArgumentException e) {
			throw new BadRequest(e,
				"Invalid argument type passed to the following method: ''{0}''.\n\tArgument types: {1}",
				mi.toString(), mi.getFullName()
			);
		} catch (ExecutableException e) {
			throw e.unwrap();
		}
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public ContextBuilder copy() {
		throw unsupportedOperationException("Method not implemented.");
	}

	@Override /* Context */
	public Session createSession(SessionArgs args) {
		throw unsupportedOperationException("Method not implemented.");
	}

	@Override /* Context */
	public SessionArgs createDefaultSessionArgs() {
		throw unsupportedOperationException("Method not implemented.");
	}

	/*
	 * compareTo() method is used to keep SimpleMethods ordered in the RestCallRouter list.
	 * It maintains the order in which matches are made during requests.
	 */
	@Override /* Comparable */
	public int compareTo(RestOpContext o) {
		int c;

		for (int i = 0; i < Math.min(pathMatchers.length, o.pathMatchers.length); i++) {
			c = pathMatchers[i].compareTo(o.pathMatchers[i]);
			if (c != 0)
				return c;
		}

		c = compare(o.hierarchyDepth, hierarchyDepth);
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

		c = compare(method.getName(), o.method.getName());
		if (c != 0)
			return c;

		c = compare(method.getParameterCount(), o.method.getParameterCount());
		if (c != 0)
			return c;

		for (int i = 0; i < method.getParameterCount(); i++) {
			c = compare(method.getParameterTypes()[i].getName(), o.method.getParameterTypes()[i].getName());
			if (c != 0)
				return c;
		}

		c = compare(method.getReturnType().getName(), o.method.getReturnType().getName());
		if (c != 0)
			return c;

		return 0;
	}

	@Override /* Object */
	public boolean equals(Object o) {
		return (o instanceof RestOpContext) && eq(this, (RestOpContext)o, (x,y)->x.method.equals(y.method));
	}

	@Override /* Object */
	public int hashCode() {
		return method.hashCode();
	}

	@Override /* Context */
	public OMap toMap() {
		return super.toMap()
			.a(
				"RestOpContext",
				OMap
				.create()
				.filtered()
				.a("defaultRequestFormData", defaultRequestFormData)
				.a("defaultRequestHeaders", defaultRequestHeaders)
				.a("defaultRequestQueryData", defaultRequestQueryData)
				.a("httpMethod", httpMethod)
			);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//-----------------------------------------------------------------------------------------------------------------

	private static HttpPartSerializer createPartSerializer(Class<? extends HttpPartSerializer> c, HttpPartSerializer _default) {
		HttpPartSerializer hps = castOrCreate(HttpPartSerializer.class, c, true);
		return hps == null ? _default : hps;
	}

	private UrlPathMatch matchPattern(RestCall call) {
		UrlPathMatch pm = null;
		for (UrlPathMatcher pp : pathMatchers)
			if (pm == null)
				pm = pp.match(call.getUrlPath());
		return pm;
	}
}
