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
import static org.apache.juneau.BeanContext.*;
import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.internal.ObjectUtils.*;
import static org.apache.juneau.rest.RestContext.*;
import static org.apache.juneau.rest.util.RestUtils.*;
import static org.apache.juneau.httppart.HttpPartType.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import javax.servlet.http.*;

import org.apache.juneau.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.annotation.FormData;
import org.apache.juneau.http.annotation.Header;
import org.apache.juneau.http.annotation.Query;
import org.apache.juneau.http.annotation.Response;
import org.apache.juneau.httppart.*;
import org.apache.juneau.httppart.bean.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.internal.HttpUtils;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.exception.*;
import org.apache.juneau.rest.util.RestUtils;
import org.apache.juneau.rest.util.UrlPathPattern;
import org.apache.juneau.rest.widget.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;

/**
 * Represents a single Java servlet/resource method annotated with {@link RestMethod @RestMethod}.
 */
public class RestJavaMethod implements Comparable<RestJavaMethod>  {
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

	RestJavaMethod(Object servlet, java.lang.reflect.Method method, RestContext context) throws RestServletException {
		Builder b = new Builder(servlet, method, context);
		this.context = context;
		this.method = method;
		this.info = MethodInfo.of(method);
		this.httpMethod = b.httpMethod;
		this.pathPattern = b.pathPattern;
		this.methodParams = b.methodParams;
		this.guards = b.guards;
		this.optionalMatchers = b.optionalMatchers;
		this.requiredMatchers = b.requiredMatchers;
		this.converters = b.converters;
		this.serializers = b.serializers;
		this.parsers = b.parsers;
		this.encoders = b.encoders;
		this.partParser = b.partParser;
		this.partSerializer = b.partSerializer;
		this.jsonSchemaGenerator = b.jsonSchemaGenerator;
		this.beanContext = b.beanContext;
		this.properties = b.properties;
		this.propertyStore = b.propertyStore;
		this.defaultRequestHeaders = b.defaultRequestHeaders;
		this.defaultQuery = b.defaultQuery;
		this.defaultFormData = b.defaultFormData;
		this.defaultCharset = b.defaultCharset;
		this.maxInput = b.maxInput;
		this.priority = b.priority;
		this.supportedAcceptTypes = b.supportedAcceptTypes;
		this.supportedContentTypes = b.supportedContentTypes;
		this.responseMeta = b.responseMeta;
		this.widgets = unmodifiableMap(b.widgets);
	}

	private static final class Builder  {
		String httpMethod, defaultCharset;
		UrlPathPattern pathPattern;
		RestMethodParam[] methodParams;
		RestGuard[] guards;
		RestMatcher[] optionalMatchers, requiredMatchers;
		RestConverter[] converters;
		SerializerGroup serializers;
		ParserGroup parsers;
		EncoderGroup encoders;
		HttpPartParser partParser;
		HttpPartSerializer partSerializer;
		JsonSchemaGenerator jsonSchemaGenerator;
		BeanContext beanContext;
		RestMethodProperties properties;
		PropertyStore propertyStore;
		Map<String,Object> defaultRequestHeaders, defaultQuery, defaultFormData;
		long maxInput;
		Integer priority;
		Map<String,Widget> widgets;
		List<MediaType> supportedAcceptTypes, supportedContentTypes;
		ResponseBeanMeta responseMeta;

		@SuppressWarnings("deprecation")
		Builder(Object servlet, java.lang.reflect.Method method, RestContext context) throws RestServletException {
			String sig = method.getDeclaringClass().getName() + '.' + method.getName();
			MethodInfo mi = getMethodInfo(servlet.getClass(), method);

			try {

				RestMethod m = mi.getAnnotation(RestMethod.class);
				if (m == null)
					throw new RestServletException("@RestMethod annotation not found on method ''{0}''", sig);

				VarResolver vr = context.getVarResolver();
				boolean hasConfigAnnotations = mi.hasConfigAnnotations();

				serializers = context.getSerializers();
				parsers = context.getParsers();
				partSerializer = context.getPartSerializer();
				partParser = context.getPartParser();
				jsonSchemaGenerator = context.getJsonSchemaGenerator();
				beanContext = context.getBeanContext();
				encoders = context.getEncoders();
				properties = new RestMethodProperties(context.getProperties());
				defaultCharset = context.getDefaultCharset();
				maxInput = context.getMaxInput();
				AnnotationsMap configAnnotationsMap = hasConfigAnnotations ? mi.getConfigAnnotationsMapParentFirst() : context.getConfigAnnotationsMap();

				if (! m.defaultCharset().isEmpty())
					defaultCharset = vr.resolve(m.defaultCharset());
				if (! m.maxInput().isEmpty())
					maxInput = StringUtils.parseLongWithSuffix(vr.resolve(m.maxInput()));

				HtmlDocBuilder hdb = new HtmlDocBuilder(properties);

				HtmlDoc hd = m.htmldoc();
				hdb.process(hd);

				widgets = new HashMap<>(context.getWidgets());
				for (Class<? extends Widget> wc : hd.widgets()) {
					Widget w = castOrCreate(Widget.class, wc);
					widgets.put(w.getName(), w);
					hdb.script("INHERIT", "$W{"+w.getName()+".script}");
					hdb.style("INHERIT", "$W{"+w.getName()+".style}");
				}

				SerializerGroupBuilder sgb = null;
				ParserGroupBuilder pgb = null;
				ParserBuilder uepb = null;
				BeanContextBuilder bcb = null;
				JsonSchemaGeneratorBuilder jsgb = null;
				PropertyStore cps = context.getPropertyStore();

				Object[] mSerializers = merge(cps.getArrayProperty(REST_serializers, Object.class), m.serializers());
				Object[] mParsers = merge(cps.getArrayProperty(REST_parsers, Object.class), m.parsers());
				Object[] mPojoSwaps = merge(cps.getArrayProperty(BEAN_pojoSwaps, Object.class), m.pojoSwaps());
				Object[] mBeanFilters = merge(cps.getArrayProperty(BEAN_beanFilters, Object.class), m.beanFilters());

				if (m.serializers().length > 0 || m.parsers().length > 0 || m.properties().length > 0 || m.flags().length > 0
						|| m.beanFilters().length > 0 || m.pojoSwaps().length > 0 || m.bpi().length > 0
						|| m.bpx().length > 0 || hasConfigAnnotations) {
					sgb = SerializerGroup.create();
					pgb = ParserGroup.create();
					uepb = Parser.create();
					bcb = beanContext.builder();
					jsgb = JsonSchemaGenerator.create();
					sgb.append(mSerializers);
					pgb.append(mParsers);
				}

				//String p = trimTrailingSlashes(m.path());
				String p = fixMethodPath(m.path());
				if (isEmpty(p))
					p = HttpUtils.detectHttpPath(method, true);

				httpMethod = emptyIfNull(firstNonEmpty(m.name(), m.method())).toUpperCase(Locale.ENGLISH);
				if (httpMethod.isEmpty())
					httpMethod = HttpUtils.detectHttpMethod(method, true, "GET");
				if ("METHOD".equals(httpMethod))
					httpMethod = "*";

				priority = m.priority();

				converters = new RestConverter[m.converters().length];
				for (int i = 0; i < converters.length; i++)
					converters[i] = castOrCreate(RestConverter.class, m.converters()[i]);

				guards = new RestGuard[m.guards().length];
				for (int i = 0; i < guards.length; i++)
					guards[i] = castOrCreate(RestGuard.class, m.guards()[i]);

				List<RestMatcher> optionalMatchers = new LinkedList<>(), requiredMatchers = new LinkedList<>();
				for (int i = 0; i < m.matchers().length; i++) {
					Class<? extends RestMatcher> c = m.matchers()[i];
					RestMatcher matcher = castOrCreate(RestMatcher.class, c, true, servlet, method);
					if (matcher.mustMatch())
						requiredMatchers.add(matcher);
					else
						optionalMatchers.add(matcher);
				}
				if (! m.clientVersion().isEmpty())
					requiredMatchers.add(new ClientVersionMatcher(context.getClientVersionHeader(), mi));

				this.requiredMatchers = requiredMatchers.toArray(new RestMatcher[requiredMatchers.size()]);
				this.optionalMatchers = optionalMatchers.toArray(new RestMatcher[optionalMatchers.size()]);

				VarResolverSession sr = vr.createSession();

				PropertyStoreBuilder psb = PropertyStore.create().apply(context.getPropertyStore2()).set(BEAN_beanFilters, mBeanFilters).set(BEAN_pojoSwaps, mPojoSwaps);

				for (Property p1 : m.properties())
					psb.set(p1.name(), p1.value());
				for (String p1 : m.flags())
					psb.set(p1, true);
				if (hasConfigAnnotations)
					psb.applyAnnotations(configAnnotationsMap, sr);
				this.propertyStore = psb.build();

				if (sgb != null) {
					sgb.apply(propertyStore);
					if (m.bpi().length > 0) {
						Map<String,String> bpiMap = new LinkedHashMap<>();
						for (String s : m.bpi()) {
							for (String s2 : split(s, ';')) {
								int i = s2.indexOf(':');
								if (i == -1)
									throw new RestServletException(
										"Invalid format for @RestMethod(bpi) on method ''{0}''.  Must be in the format \"ClassName: comma-delimited-tokens\".  \nValue: {1}", sig, s);
								bpiMap.put(s2.substring(0, i).trim(), s2.substring(i+1).trim());
							}
						}
						sgb.includeProperties(bpiMap);
					}
					if (m.bpx().length > 0) {
						Map<String,String> bpxMap = new LinkedHashMap<>();
						for (String s : m.bpx()) {
							for (String s2 : split(s, ';')) {
								int i = s2.indexOf(':');
								if (i == -1)
									throw new RestServletException(
										"Invalid format for @RestMethod(bpx) on method ''{0}''.  Must be in the format \"ClassName: comma-delimited-tokens\".  \nValue: {1}", sig, s);
								bpxMap.put(s2.substring(0, i).trim(), s2.substring(i+1).trim());
							}
						}
						sgb.excludeProperties(bpxMap);
					}
					sgb.beanFilters(mBeanFilters);
					sgb.pojoSwaps(mPojoSwaps);
				}

				if (pgb != null) {
					pgb.apply(propertyStore);
					pgb.beanFilters(mBeanFilters);
					pgb.pojoSwaps(mPojoSwaps);
				}

				if (uepb != null) {
					uepb.apply(propertyStore);
					uepb.beanFilters(mBeanFilters);
					uepb.pojoSwaps(mPojoSwaps);
				}

				if (bcb != null) {
					bcb.apply(propertyStore);
					bcb.beanFilters(mBeanFilters);
					bcb.pojoSwaps(mPojoSwaps);
				}

				if (jsgb != null) {
					jsgb.apply(propertyStore);
					jsgb.beanFilters(mBeanFilters);
					jsgb.pojoSwaps(mPojoSwaps);
				}

				if (m.properties().length > 0 || m.flags().length > 0) {
					properties = new RestMethodProperties(properties);
					for (Property p1 : m.properties())
						properties.put(p1.name(), p1.value());
					for (String p1 : m.flags())
						properties.put(p1, true);
				}

				if (m.encoders().length > 0) {
					EncoderGroupBuilder g = EncoderGroup.create().append(IdentityEncoder.INSTANCE);
					for (Class<?> c : m.encoders()) {
						try {
							g.append(c);
						} catch (Exception e) {
							throw new RestServletException(
								"Exception occurred while trying to instantiate ConfigEncoder on method ''{0}'': ''{1}''", sig, c.getSimpleName()).initCause(e);
						}
					}
					encoders = g.build();
				}

				defaultRequestHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
				for (String s : m.defaultRequestHeaders()) {
					String[] h = RestUtils.parseKeyValuePair(vr.resolve(s));
					if (h == null)
						throw new RestServletException(
							"Invalid default request header specified on method ''{0}'': ''{1}''.  Must be in the format: ''name[:=]value''", sig, s);
					defaultRequestHeaders.put(h[0], h[1]);
				}

				String defaultAccept = vr.resolve(m.defaultAccept());
				if (isNotEmpty(defaultAccept))
					defaultRequestHeaders.put("Accept", defaultAccept);

				String defaultContentType = vr.resolve(m.defaultContentType());
				if (isNotEmpty(defaultContentType))
					defaultRequestHeaders.put("Content-Type", defaultAccept);

				defaultQuery = new LinkedHashMap<>();
				for (String s : m.defaultQuery()) {
					String[] h = RestUtils.parseKeyValuePair(vr.resolve(s));
					if (h == null)
						throw new RestServletException(
							"Invalid default query parameter specified on method ''{0}'': ''{1}''.  Must be in the format: ''name[:=]value''", sig, s);
					defaultQuery.put(h[0], h[1]);
				}

				defaultFormData = new LinkedHashMap<>();
				for (String s : m.defaultFormData()) {
					String[] h = RestUtils.parseKeyValuePair(vr.resolve(s));
					if (h == null)
						throw new RestServletException(
							"Invalid default form data parameter specified on method ''{0}'': ''{1}''.  Must be in the format: ''name[:=]value''", sig, s);
					defaultFormData.put(h[0], h[1]);
				}

				Type[] pt = method.getGenericParameterTypes();
				Annotation[][] pa = method.getParameterAnnotations();
				for (int i = 0; i < pt.length; i++) {
					for (Annotation a : pa[i]) {
						if (a instanceof Header) {
							Header h = (Header)a;
							if (h._default().length > 0)
								defaultRequestHeaders.put(firstNonEmpty(h.name(), h.value()), parseAnything(joinnl(h._default())));
						} else if (a instanceof Query) {
							Query q = (Query)a;
							if (q._default().length > 0)
								defaultQuery.put(firstNonEmpty(q.name(), q.value()), parseAnything(joinnl(q._default())));
						} else if (a instanceof FormData) {
							FormData f = (FormData)a;
							if (f._default().length > 0)
								defaultFormData.put(firstNonEmpty(f.name(), f.value()), parseAnything(joinnl(f._default())));
						}
					}
				}

				pathPattern = new UrlPathPattern(p);

				if (sgb != null)
					serializers = sgb.build();
				if (pgb != null)
					parsers = pgb.build();
				if (uepb != null && partParser instanceof Parser) {
					Parser pp = (Parser)partParser;
					partParser = (HttpPartParser)pp
						.builder()
						.apply(uepb.getPropertyStore())
						.applyAnnotations(configAnnotationsMap, sr)
						.build();
				}
				if (bcb != null)
					beanContext = bcb.build();
				if (jsgb != null)
					jsonSchemaGenerator = jsgb.build();

				supportedAcceptTypes =
					m.produces().length > 0
					? immutableList(MediaType.forStrings(resolveVars(vr, m.produces())))
					: serializers.getSupportedMediaTypes();
				supportedContentTypes =
					m.consumes().length > 0
					? immutableList(MediaType.forStrings(resolveVars(vr, m.consumes())))
					: parsers.getSupportedMediaTypes();

				methodParams = context.findParams(mi, false, pathPattern);

				if (mi.hasAnnotation(Response.class))
					responseMeta = ResponseBeanMeta.create(mi, serializers.getPropertyStore());

				// Need this to access methods in anonymous inner classes.
				mi.setAccessible();
			} catch (RestServletException e) {
				throw e;
			} catch (Exception e) {
				throw new RestServletException("Exception occurred while initializing method ''{0}''", sig).initCause(e);
			}
		}
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
	public int compareTo(RestJavaMethod o) {
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
		if (! (o instanceof RestJavaMethod))
			return false;
		return (compareTo((RestJavaMethod)o) == 0);
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