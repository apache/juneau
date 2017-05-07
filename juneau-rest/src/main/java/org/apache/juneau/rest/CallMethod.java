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
import static org.apache.juneau.dto.swagger.SwaggerBuilder.*;
import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.rest.CallMethod.ParamType.*;
import static org.apache.juneau.rest.RestContext.*;
import static org.apache.juneau.rest.annotation.Inherit.*;
import static org.apache.juneau.serializer.SerializerContext.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.lang.reflect.Method;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.juneau.*;
import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.html.*;
import org.apache.juneau.http.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.annotation.Properties;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.urlencoding.*;

/**
 * Represents a single Java servlet/resource method annotated with {@link RestMethod @RestMethod}.
 */
@SuppressWarnings("hiding")
class CallMethod implements Comparable<CallMethod>  {
	private final java.lang.reflect.Method method;
	private final String httpMethod;
	private final UrlPathPattern pathPattern;
	private final CallMethod.MethodParam[] params;
	private final RestGuard[] guards;
	private final RestMatcher[] optionalMatchers;
	private final RestMatcher[] requiredMatchers;
	private final RestConverter[] converters;
	private final SerializerGroup serializers;
	private final ParserGroup parsers;
	private final EncoderGroup encoders;
	private final UrlEncodingParser urlEncodingParser;
	private final UrlEncodingSerializer urlEncodingSerializer;
	private final ObjectMap properties;
	private final Map<String,String> defaultRequestHeaders;
	private final String defaultCharset;
	private final boolean deprecated;
	private final String description, tags, summary, externalDocs;
	private final Integer priority;
	private final org.apache.juneau.rest.annotation.Parameter[] parameters;
	private final Response[] responses;
	private final RestContext context;
	private final String pageTitle, pageText, pageLinks;

	CallMethod(Object servlet, java.lang.reflect.Method method, RestContext context) throws RestServletException {
		Builder b = new Builder(servlet, method, context);
		this.context = context;
		this.method = method;
		this.httpMethod = b.httpMethod;
		this.pathPattern = b.pathPattern;
		this.params = b.params;
		this.guards = b.guards;
		this.optionalMatchers = b.optionalMatchers;
		this.requiredMatchers = b.requiredMatchers;
		this.converters = b.converters;
		this.serializers = b.serializers;
		this.parsers = b.parsers;
		this.encoders = b.encoders;
		this.urlEncodingParser = b.urlEncodingParser;
		this.urlEncodingSerializer = b.urlEncodingSerializer;
		this.properties = b.properties;
		this.defaultRequestHeaders = b.defaultRequestHeaders;
		this.defaultCharset = b.defaultCharset;
		this.deprecated = b.deprecated;
		this.description = b.description;
		this.tags = b.tags;
		this.summary = b.summary;
		this.externalDocs = b.externalDocs;
		this.priority = b.priority;
		this.parameters = b.parameters;
		this.responses = b.responses;
		this.pageTitle = b.pageTitle;
		this.pageText = b.pageText;
		this.pageLinks = b.pageLinks;
	}

	private static class Builder  {
		private String httpMethod, defaultCharset, description, tags, summary, externalDocs, pageTitle, pageText, pageLinks;
		private UrlPathPattern pathPattern;
		private CallMethod.MethodParam[] params;
		private RestGuard[] guards;
		private RestMatcher[] optionalMatchers, requiredMatchers;
		private RestConverter[] converters;
		private SerializerGroup serializers;
		private ParserGroup parsers;
		private EncoderGroup encoders;
		private UrlEncodingParser urlEncodingParser;
		private UrlEncodingSerializer urlEncodingSerializer;
		private ObjectMap properties;
		private Map<String,String> defaultRequestHeaders;
		private boolean plainParams, deprecated;
		private Integer priority;
		private org.apache.juneau.rest.annotation.Parameter[] parameters;
		private Response[] responses;

		private Builder(Object servlet, java.lang.reflect.Method method, RestContext context) throws RestServletException {
			try {

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
				parameters = m.parameters();
				responses = m.responses();
				serializers = context.getSerializers();
				parsers = context.getParsers();
				urlEncodingSerializer = context.getUrlEncodingSerializer();
				urlEncodingParser = context.getUrlEncodingParser();
				encoders = context.getEncoders();
				properties = context.getProperties();

				pageTitle = m.pageTitle().isEmpty() ? context.getPageTitle() : m.pageTitle();
				pageText = m.pageText().isEmpty() ? context.getPageText() : m.pageText();
				pageLinks = m.pageLinks().isEmpty() ? context.getPageLinks() : m.pageLinks();

				List<Inherit> si = Arrays.asList(m.serializersInherit());
				List<Inherit> pi = Arrays.asList(m.parsersInherit());

				SerializerGroupBuilder sgb = null;
				ParserGroupBuilder pgb = null;
				UrlEncodingParserBuilder uepb = null;

				if (m.serializers().length > 0 || m.parsers().length > 0 || m.properties().length > 0 || m.beanFilters().length > 0 || m.pojoSwaps().length > 0) {
					sgb = new SerializerGroupBuilder();
					pgb = new ParserGroupBuilder();
					uepb = new UrlEncodingParserBuilder(urlEncodingParser.createPropertyStore());

					if (si.contains(SERIALIZERS) || m.serializers().length == 0)
						sgb.append(serializers.getSerializers());

					if (pi.contains(PARSERS) || m.parsers().length == 0)
						pgb.append(parsers.getParsers());
				}

				httpMethod = m.name().toUpperCase(Locale.ENGLISH);
				if (httpMethod.equals("") && method.getName().startsWith("do"))
					httpMethod = method.getName().substring(2).toUpperCase(Locale.ENGLISH);
				if (httpMethod.equals(""))
					httpMethod = "GET";
				if (httpMethod.equals("METHOD"))
					httpMethod = "*";

				priority = m.priority();

				String p = m.path();
				converters = new RestConverter[m.converters().length];
				for (int i = 0; i < converters.length; i++)
					converters[i] = m.converters()[i].newInstance();

				guards = new RestGuard[m.guards().length];
				for (int i = 0; i < guards.length; i++)
					guards[i] = m.guards()[i].newInstance();

				List<RestMatcher> optionalMatchers = new LinkedList<RestMatcher>(), requiredMatchers = new LinkedList<RestMatcher>();
				for (int i = 0; i < m.matchers().length; i++) {
					Class<? extends RestMatcher> c = m.matchers()[i];
					RestMatcher matcher = null;
					if (ClassUtils.isParentClass(RestMatcherReflecting.class, c))
						matcher = c.getConstructor(Object.class, Method.class).newInstance(servlet, method);
					else
						matcher = c.newInstance();
					if (matcher.mustMatch())
						requiredMatchers.add(matcher);
					else
						optionalMatchers.add(matcher);
				}
				if (! m.clientVersion().isEmpty())
					requiredMatchers.add(new ClientVersionMatcher(context.getClientVersionHeader(), method));

				this.requiredMatchers = requiredMatchers.toArray(new RestMatcher[requiredMatchers.size()]);
				this.optionalMatchers = optionalMatchers.toArray(new RestMatcher[optionalMatchers.size()]);

				Class<?>[] beanFilters = context.getBeanFilters(), pojoSwaps = context.getPojoSwaps();

				if (sgb != null) {
					sgb.append(m.serializers());
					if (si.contains(TRANSFORMS))
						sgb.beanFilters(beanFilters).pojoSwaps(pojoSwaps);
					if (si.contains(PROPERTIES))
						sgb.properties(properties);
					for (Property p1 : m.properties())
						sgb.property(p1.name(), p1.value());
					sgb.beanFilters(m.beanFilters());
					sgb.pojoSwaps(m.pojoSwaps());
				}

				if (pgb != null) {
					pgb.append(m.parsers());
					if (pi.contains(TRANSFORMS))
						pgb.beanFilters(beanFilters).pojoSwaps(pojoSwaps);
					if (pi.contains(PROPERTIES))
						pgb.properties(properties);
					for (Property p1 : m.properties())
						pgb.property(p1.name(), p1.value());
					pgb.beanFilters(m.beanFilters());
					pgb.pojoSwaps(m.pojoSwaps());
				}

				if (uepb != null) {
					for (Property p1 : m.properties())
						uepb.property(p1.name(), p1.value());
					uepb.beanFilters(m.beanFilters());
					uepb.pojoSwaps(m.pojoSwaps());
				}

				if (m.properties().length > 0) {
					properties = new ObjectMap().setInner(properties);
					for (Property p1 : m.properties()) {
						properties.put(p1.name(), p1.value());
					}
				}

				if (m.encoders().length > 0 || ! m.inheritEncoders()) {
					EncoderGroupBuilder g = new EncoderGroupBuilder();
					if (m.inheritEncoders())
						g.append(encoders);
					else
						g.append(IdentityEncoder.INSTANCE);

					for (Class<? extends Encoder> c : m.encoders()) {
						try {
							g.append(c);
						} catch (Exception e) {
							throw new RestServletException("Exception occurred while trying to instantiate Encoder ''{0}''", c.getSimpleName()).initCause(e);
						}
					}
					encoders = g.build();
				}

				defaultRequestHeaders = new TreeMap<String,String>(String.CASE_INSENSITIVE_ORDER);
				for (String s : m.defaultRequestHeaders()) {
					String[] h = RestUtils.parseHeader(s);
					if (h == null)
						throw new RestServletException("Invalid default request header specified: ''{0}''.  Must be in the format: ''Header-Name: header-value''", s);
					defaultRequestHeaders.put(h[0], h[1]);
				}

				defaultCharset = properties.getString(REST_defaultCharset, context.getDefaultCharset());
				String paramFormat = properties.getString(REST_paramFormat, context.getParamFormat());
				plainParams = paramFormat.equals("PLAIN");

				pathPattern = new UrlPathPattern(p);

				int attrIdx = 0;
				Type[] pt = method.getGenericParameterTypes();
				Annotation[][] pa = method.getParameterAnnotations();
				params = new CallMethod.MethodParam[pt.length];
				for (int i = 0; i < params.length; i++) {
					params[i] = new CallMethod.MethodParam(pt[i], method, pa[i], plainParams, pathPattern, attrIdx);
					attrIdx = params[i].attrIdx;
				}

				if (sgb != null)
					serializers = sgb.build();
				if (pgb != null)
					parsers = pgb.build();
				if (uepb != null)
					urlEncodingParser = uepb.build();

				// Need this to access methods in anonymous inner classes.
				method.setAccessible(true);
			} catch (RestServletException e) {
				throw e;
			} catch (Exception e) {
				throw new RestServletException("Exception occurred while initializing method ''{0}''", method.getName()).initCause(e);
			}
		}
	}

	/**
	 * Represents a single parameter in the Java method.
	 */
	private static class MethodParam {

		private final ParamType paramType;
		private final Type type;
		private final String name;
		private final boolean multiPart, plainParams;
		private final int attrIdx;

		private MethodParam(Type type, Method method, Annotation[] annotations, boolean methodPlainParams, UrlPathPattern pathPattern, int attrIdx) throws ServletException {
			this.type = type;

			ParamType _paramType = null;
			String _name = "";
			boolean _multiPart = false, _plainParams = false;

			boolean isClass = type instanceof Class;
			if (isClass && isParentClass(HttpServletRequest.class, (Class<?>)type))
				_paramType = REQ;
			else if (isClass && isParentClass(HttpServletResponse.class, (Class<?>)type))
				_paramType = RES;
			else if (isClass && isParentClass(Accept.class, (Class<?>)type))
				_paramType = ACCEPT;
			else if (isClass && isParentClass(AcceptEncoding.class, (Class<?>)type))
				_paramType = ACCEPTENCODING;
			else if (isClass && isParentClass(ContentType.class, (Class<?>)type))
				_paramType = CONTENTTYPE;
			else for (Annotation a : annotations) {
				if (a instanceof Path) {
					Path a2 = (Path)a;
					_paramType = PATH;
					_name = a2.value();
				} else if (a instanceof Header) {
					Header h = (Header)a;
					_paramType = HEADER;
					_name = h.value();
				} else if (a instanceof FormData) {
					FormData p = (FormData)a;
					if (p.multipart())
						assertCollection(type, method);
					_paramType = FORMDATA;
					_multiPart = p.multipart();
					_plainParams = p.format().equals("INHERIT") ? methodPlainParams : p.format().equals("PLAIN");
					_name = p.value();
				} else if (a instanceof Query) {
					Query p = (Query)a;
					if (p.multipart())
						assertCollection(type, method);
					_paramType = QUERY;
					_multiPart = p.multipart();
					_plainParams = p.format().equals("INHERIT") ? methodPlainParams : p.format().equals("PLAIN");
					_name = p.value();
				} else if (a instanceof HasFormData) {
					HasFormData p = (HasFormData)a;
					_paramType = HASFORMDATA;
					_name = p.value();
				} else if (a instanceof HasQuery) {
					HasQuery p = (HasQuery)a;
					_paramType = HASQUERY;
					_name = p.value();
				} else if (a instanceof Body) {
					_paramType = BODY;
				} else if (a instanceof org.apache.juneau.rest.annotation.Method) {
					_paramType = METHOD;
					if (type != String.class)
						throw new ServletException("@Method parameters must be of type String");
				} else if (a instanceof PathRemainder) {
					_paramType = PATHREMAINDER;
					if (type != String.class)
						throw new ServletException("@PathRemainder parameters must be of type String");
				} else if (a instanceof Properties) {
					_paramType = PROPS;
					_name = "PROPERTIES";
				} else if (a instanceof Messages) {
					_paramType = MESSAGES;
					_name = "MESSAGES";
				}
			}
			if (_paramType == null)
				_paramType = PATH;

			if (_paramType == PATH && _name.isEmpty()) {
				int idx = attrIdx++;
				String[] vars = pathPattern.getVars();
				if (vars.length <= idx)
					throw new RestServletException("Number of attribute parameters in method ''{0}'' exceeds the number of URL pattern variables.", method.getName());

				// Check for {#} variables.
				String idxs = String.valueOf(idx);
				for (int i = 0; i < vars.length; i++)
					if (StringUtils.isNumeric(vars[i]) && vars[i].equals(idxs))
						_name = vars[i];

				if (_name.isEmpty())
					_name = pathPattern.getVars()[idx];
			}

			this.paramType = _paramType;
			this.name = _name;
			this.multiPart = _multiPart;
			this.plainParams = _plainParams;
			this.attrIdx = attrIdx;
		}

		/**
		 * Throws an exception if the specified type isn't an array or collection.
		 */
		private static void assertCollection(Type t, Method m) throws ServletException {
			ClassMeta<?> cm = BeanContext.DEFAULT.getClassMeta(t);
			if (! cm.isCollectionOrArray())
				throw new ServletException("Use of multipart flag on parameter that's not an array or Collection on method" + m);
		}

		private Object getValue(RestRequest req, RestResponse res) throws Exception {
			BeanSession session = req.getBeanSession();
			switch(paramType) {
				case REQ:        return req;
				case RES:        return res;
				case PATH:       return req.getPathParams().get(name, type);
				case BODY:       return req.getBody().asType(type);
				case HEADER:     return req.getHeaders().get(name, type);
				case METHOD:     return req.getMethod();
				case FORMDATA: {
					if (multiPart)
						return req.getFormData().getAll(name, type);
					if (plainParams)
						return session.convertToType(req.getFormData(name), session.getClassMeta(type));
					return req.getFormData().get(name, type);
				}
				case QUERY: {
					if (multiPart)
						return req.getQuery().getAll(name, type);
					if (plainParams)
						return session.convertToType(req.getQuery(name), session.getClassMeta(type));
					return req.getQuery().get(name, type);
				}
				case HASFORMDATA:   return session.convertToType(req.getFormData().containsKey(name), session.getClassMeta(type));
				case HASQUERY:      return session.convertToType(req.getQuery().containsKey(name), session.getClassMeta(type));
				case PATHREMAINDER: return req.getPathRemainder();
				case PROPS:         return res.getProperties();
				case MESSAGES:      return req.getResourceBundle();
				case ACCEPT:        return req.getHeaders().getAccept();
				case ACCEPTENCODING:return req.getHeaders().getAcceptEncoding();
				case CONTENTTYPE:   return req.getHeaders().getContentType();
				default:            return null;
			}
		}
	}

	static enum ParamType {
		REQ, RES, PATH, BODY, HEADER, METHOD, FORMDATA, QUERY, HASFORMDATA, HASQUERY, PATHREMAINDER, PROPS, MESSAGES, ACCEPT, ACCEPTENCODING, CONTENTTYPE;

		private String getSwaggerParameterType() {
			switch(this) {
				case PATH: return "path";
				case HEADER:
				case ACCEPT:
				case ACCEPTENCODING:
				case CONTENTTYPE: return "header";
				case FORMDATA: return "formData";
				case QUERY: return "query";
				case BODY: return "body";
				default: return null;
			}
		}
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
	 * Returns the localized Swagger for this Java method.
	 */
	Operation getSwaggerOperation(RestRequest req) throws ParseException {
		Operation o = operation()
			.operationId(method.getName())
			.description(getDescription(req))
			.tags(getTags(req))
			.summary(getSummary(req))
			.externalDocs(getExternalDocs(req))
			.parameters(getParameters(req))
			.responses(getResponses(req));

		if (isDeprecated())
			o.deprecated(true);

		if (! parsers.getSupportedMediaTypes().equals(context.getParsers().getSupportedMediaTypes()))
			o.consumes(parsers.getSupportedMediaTypes());

		if (! serializers.getSupportedMediaTypes().equals(context.getSerializers().getSupportedMediaTypes()))
			o.produces(serializers.getSupportedMediaTypes());

		return o;
	}

	private Operation getSwaggerOperationFromFile(RestRequest req) {
		Swagger s = req.getSwaggerFromFile();
		if (s != null && s.getPaths() != null && s.getPaths().get(pathPattern.getPatternString()) != null)
			return s.getPaths().get(pathPattern.getPatternString()).get(httpMethod);
		return null;
	}

	/**
	 * Returns the localized summary for this Java method.
	 */
	String getSummary(RestRequest req) {
		VarResolverSession vr = req.getVarResolverSession();
		if (summary != null)
			return vr.resolve(summary);
		String summary = context.getMessages().findFirstString(req.getLocale(), method.getName() + ".summary");
		if (summary != null)
			return vr.resolve(summary);
		Operation o = getSwaggerOperationFromFile(req);
		if (o != null)
			return o.getSummary();
		return null;
	}

	/**
	 * Returns the localized description for this Java method.
	 */
	String getDescription(RestRequest req) {
		VarResolverSession vr = req.getVarResolverSession();
		if (description != null)
			return vr.resolve(description);
		String description = context.getMessages().findFirstString(req.getLocale(), method.getName() + ".description");
		if (description != null)
			return vr.resolve(description);
		Operation o = getSwaggerOperationFromFile(req);
		if (o != null)
			return o.getDescription();
		return null;
	}

	/**
	 * Returns the localized Swagger tags for this Java method.
	 */
	private List<String> getTags(RestRequest req) {
		VarResolverSession vr = req.getVarResolverSession();
		JsonParser jp = JsonParser.DEFAULT;
		try {
			if (tags != null)
				return jp.parse(vr.resolve(tags), ArrayList.class, String.class);
			String tags = context.getMessages().findFirstString(req.getLocale(), method.getName() + ".tags");
			if (tags != null)
				return jp.parse(vr.resolve(tags), ArrayList.class, String.class);
			Operation o = getSwaggerOperationFromFile(req);
			if (o != null)
				return o.getTags();
			return null;
		} catch (Exception e) {
			throw new RestException(SC_INTERNAL_SERVER_ERROR, e);
		}
	}

	/**
	 * Returns the localized Swagger external docs for this Java method.
	 */
	private ExternalDocumentation getExternalDocs(RestRequest req) {
		VarResolverSession vr = req.getVarResolverSession();
		JsonParser jp = JsonParser.DEFAULT;
		try {
			if (externalDocs != null)
				return jp.parse(vr.resolve(externalDocs), ExternalDocumentation.class);
			String externalDocs = context.getMessages().findFirstString(req.getLocale(), method.getName() + ".externalDocs");
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

	/**
	 * Returns the Swagger deprecated flag for this Java method.
	 */
	private boolean isDeprecated() {
		return deprecated;
	}

	/**
	 * Returns the localized Swagger parameter information for this Java method.
	 */
	private List<ParameterInfo> getParameters(RestRequest req) throws ParseException {
		Operation o = getSwaggerOperationFromFile(req);
		if (o != null && o.getParameters() != null)
			return o.getParameters();

		VarResolverSession vr = req.getVarResolverSession();
		JsonParser jp = JsonParser.DEFAULT;
		Map<String,ParameterInfo> m = new TreeMap<String,ParameterInfo>();

		// First parse @RestMethod.parameters() annotation.
		for (org.apache.juneau.rest.annotation.Parameter v : parameters) {
			String in = vr.resolve(v.in());
			ParameterInfo p = parameterInfo(in, vr.resolve(v.name()));

			if (! v.description().isEmpty())
				p.description(vr.resolve(v.description()));
			if (v.required())
				p.required(v.required());

			if ("body".equals(in)) {
				if (! v.schema().isEmpty())
					p.schema(jp.parse(vr.resolve(v.schema()), SchemaInfo.class));
			} else {
				if (v.allowEmptyValue())
					p.allowEmptyValue(v.allowEmptyValue());
				if (! v.collectionFormat().isEmpty())
					p.collectionFormat(vr.resolve(v.collectionFormat()));
				if (! v._default().isEmpty())
					p._default(vr.resolve(v._default()));
				if (! v.format().isEmpty())
					p.format(vr.resolve(v.format()));
				if (! v.items().isEmpty())
					p.items(jp.parse(vr.resolve(v.items()), Items.class));
				p.type(vr.resolve(v.type()));
			}
			m.put(p.getIn() + '.' + p.getName(), p);
		}

		// Next, look in resource bundle.
		String prefix = method.getName() + ".req";
		for (String key : context.getMessages().keySet(prefix)) {
			if (key.length() > prefix.length()) {
				String value = vr.resolve(context.getMessages().getString(key));
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
						p = parameterInfoStrict(in, name);
						m.put(k2, p);
					}

					if (field.equals("description"))
						p.description(value);
					else if (field.equals("required"))
						p.required(Boolean.valueOf(value));

					if ("body".equals(in)) {
						if (field.equals("schema"))
							p.schema(jp.parse(value, SchemaInfo.class));
					} else {
						if (field.equals("allowEmptyValue"))
							p.allowEmptyValue(Boolean.valueOf(value));
						else if (field.equals("collectionFormat"))
							p.collectionFormat(value);
						else if (field.equals("default"))
							p._default(value);
						else if (field.equals("format"))
							p.format(value);
						else if (field.equals("items"))
							p.items(jp.parse(value, Items.class));
						else if (field.equals("type"))
							p.type(value);
					}
				} else {
					System.err.println("Unknown bundle key '"+key+"'");
				}
			}
		}

		// Finally, look for parameters defined on method.
		for (CallMethod.MethodParam mp : this.params) {
			String in = mp.paramType.getSwaggerParameterType();
			if (in != null) {
				String k2 = in + '.' + ("body".equals(in) ? null : mp.name);
				ParameterInfo p = m.get(k2);
				if (p == null) {
					p = parameterInfoStrict(in, mp.name);
					m.put(k2, p);
				}
			}
		}

		if (m.isEmpty())
			return null;
		return new ArrayList<ParameterInfo>(m.values());
	}

	/**
	 * Returns the localized Swagger response information about this Java method.
	 */
	@SuppressWarnings("unchecked")
	private Map<Integer,ResponseInfo> getResponses(RestRequest req) throws ParseException {
		Operation o = getSwaggerOperationFromFile(req);
		if (o != null && o.getResponses() != null)
			return o.getResponses();

		VarResolverSession vr = req.getVarResolverSession();
		JsonParser jp = JsonParser.DEFAULT;
		Map<Integer,ResponseInfo> m = new TreeMap<Integer,ResponseInfo>();
		Map<String,HeaderInfo> m2 = new TreeMap<String,HeaderInfo>();

		// First parse @RestMethod.parameters() annotation.
		for (Response r : responses) {
			int httpCode = r.value();
			String description = r.description().isEmpty() ? RestUtils.getHttpResponseText(r.value()) : vr.resolve(r.description());
			ResponseInfo r2 = responseInfo(description);

			if (r.headers().length > 0) {
				for (org.apache.juneau.rest.annotation.Parameter v : r.headers()) {
					HeaderInfo h = headerInfoStrict(vr.resolve(v.type()));
					if (! v.collectionFormat().isEmpty())
						h.collectionFormat(vr.resolve(v.collectionFormat()));
					if (! v._default().isEmpty())
						h._default(vr.resolve(v._default()));
					if (! v.description().isEmpty())
						h.description(vr.resolve(v.description()));
					if (! v.format().isEmpty())
						h.format(vr.resolve(v.format()));
					if (! v.items().isEmpty())
						h.items(jp.parse(vr.resolve(v.items()), Items.class));
					r2.header(v.name(), h);
					m2.put(httpCode + '.' + v.name(), h);
				}
			}
			m.put(httpCode, r2);
		}

		// Next, look in resource bundle.
		String prefix = method.getName() + ".res";
		for (String key : context.getMessages().keySet(prefix)) {
			if (key.length() > prefix.length()) {
				String value = vr.resolve(context.getMessages().getString(key));
				String[] parts = key.substring(prefix.length() + 1).split("\\.");
				int httpCode = Integer.parseInt(parts[0]);
				ResponseInfo r2 = m.get(httpCode);
				if (r2 == null) {
					r2 = responseInfo(null);
					m.put(httpCode, r2);
				}

				String name = parts.length > 1 ? parts[1] : "";

				if ("header".equals(name) && parts.length > 3) {
					String headerName = parts[2];
					String field = parts[3];

					String k2 = httpCode + '.' + headerName;
					HeaderInfo h = m2.get(k2);
					if (h == null) {
						h = headerInfoStrict("string");
						m2.put(k2, h);
						r2.header(name, h);
					}
					if (field.equals("collectionFormat"))
						h.collectionFormat(value);
					else if (field.equals("default"))
						h._default(value);
					else if (field.equals("description"))
						h.description(value);
					else if (field.equals("format"))
						h.format(value);
					else if (field.equals("items"))
						h.items(jp.parse(value, Items.class));
					else if (field.equals("type"))
						h.type(value);

				} else if ("description".equals(name)) {
					r2.description(value);
				} else if ("schema".equals(name)) {
					r2.schema(jp.parse(value, SchemaInfo.class));
				} else if ("examples".equals(name)) {
					r2.examples(jp.parse(value, TreeMap.class));
				} else {
					System.err.println("Unknown bundle key '"+key+"'");
				}
			}
		}

		return m.isEmpty() ? null : m;
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
	int invoke(String pathInfo, RestRequest req, RestResponse res) throws RestException {

		String[] patternVals = pathPattern.match(pathInfo);
		if (patternVals == null)
			return SC_NOT_FOUND;

		String remainder = null;
		if (patternVals.length > pathPattern.getVars().length)
			remainder = patternVals[pathPattern.getVars().length];
		for (int i = 0; i < pathPattern.getVars().length; i++)
			req.setPathParameter(pathPattern.getVars()[i], patternVals[i]);

		ObjectMap requestProperties = createRequestProperties(properties, req);
		req.init(method, remainder, requestProperties, defaultRequestHeaders, defaultCharset, serializers, parsers, urlEncodingParser, encoders, pageTitle, pageText, pageLinks);
		res.init(requestProperties, defaultCharset, serializers, urlEncodingSerializer, encoders);

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

		context.getCallHandler().onPreCall(req);

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

			Object output = method.invoke(context.getResource(), args);
			if (! method.getReturnType().equals(Void.TYPE))
				if (output != null || ! res.getOutputStreamCalled())
					res.setOutput(output);

			context.getCallHandler().onPostCall(req, res);

			if (res.hasOutput()) {
				output = res.getOutput();
				for (RestConverter converter : converters)
					output = converter.convert(req, output, context.getBeanContext().getClassMetaForObject(output));
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

	/**
	 * This method creates all the request-time properties.
	 */
	ObjectMap createRequestProperties(final ObjectMap methodProperties, final RestRequest req) {
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
							return req.getQuery(remainder);
						if ("formData".equals(prefix))
							return req.getFormData(remainder);
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
					if (k.equals(HtmlDocSerializerContext.HTMLDOC_title))
						return req.getPageTitle();
					if (k.equals(HtmlDocSerializerContext.HTMLDOC_text))
						return req.getPageText();
					if (k.equals(HtmlDocSerializerContext.HTMLDOC_links))
						return req.getPageLinks();
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

	@Override /* Object */
	public String toString() {
		return "SimpleMethod: name=" + httpMethod + ", path=" + pathPattern.getPatternString();
	}

	/*
	 * compareTo() method is used to keep SimpleMethods ordered in the CallRouter list.
	 * It maintains the order in which matches are made during requests.
	 */
	@Override /* Comparable */
	public int compareTo(CallMethod o) {
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
		if (! (o instanceof CallMethod))
			return false;
		return (compareTo((CallMethod)o) == 0);
	}

	@Override /* Object */
	public int hashCode() {
		return super.hashCode();
	}
}