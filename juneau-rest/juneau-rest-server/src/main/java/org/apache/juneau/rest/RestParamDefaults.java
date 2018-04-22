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

import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.internal.ObjectUtils.*;
import static org.apache.juneau.rest.RestParamType.*;

import java.io.*;
import java.lang.reflect.*;
import java.lang.reflect.Method;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.juneau.*;
import org.apache.juneau.config.*;
import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.Date;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.utils.*;

/**
 * Default REST method parameter resolvers.
 * 
 * <p>
 * Contains the default set of parameter resolvers for REST resource methods (i.e methods annotated with {@link RestResource @RestResource}).
 * 
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="../../../../overview-summary.html#juneau-rest-server.MethodParameters">Overview &gt; juneau-rest-server &gt; Java Method Parameters</a>
 * </ul>
 */
class RestParamDefaults {

	/**
	 * Standard set of method parameter resolvers.
	 */
	static final Map<Class<?>,RestMethodParam> STANDARD_RESOLVERS;

	static {
		Map<Class<?>,RestMethodParam> m = new HashMap<>();

		@SuppressWarnings("rawtypes")
		Class[] r = new Class[] {

			// Standard top-level objects
			HttpServletRequestObject.class,
			RestRequestObject.class,
			HttpServletResponseObject.class,
			RestResponseObject.class,

			// Headers
			AcceptHeader.class,
			AcceptCharsetHeader.class,
			AcceptEncodingHeader.class,
			AcceptLanguageHeader.class,
			AuthorizationHeader.class,
			CacheControlHeader.class,
			ConnectionHeader.class,
			ContentLengthHeader.class,
			ContentTypeHeader.class,
			DateHeader.class,
			ExpectHeader.class,
			FromHeader.class,
			HostHeader.class,
			IfMatchHeader.class,
			IfModifiedSinceHeader.class,
			IfNoneMatchHeader.class,
			IfRangeHeader.class,
			IfUnmodifiedSinceHeader.class,
			MaxForwardsHeader.class,
			PragmaHeader.class,
			ProxyAuthorizationHeader.class,
			RangeHeader.class,
			RefererHeader.class,
			TEHeader.class,
			UserAgentHeader.class,
			UpgradeHeader.class,
			ViaHeader.class,
			WarningHeader.class,
			TimeZoneHeader.class,

			// Other objects
			ResourceBundleObject.class,
			MessageBundleObject.class,
			InputStreamObject.class,
			ServletInputStreamObject.class,
			ReaderObject.class,
			OutputStreamObject.class,
			ServletOutputStreamObject.class,
			WriterObject.class,
			RequestHeadersObject.class,
			RequestQueryObject.class,
			RequestFormDataObject.class,
			HttpMethodObject.class,
			RestLoggerObject.class,
			RestContextObject.class,
			ParserObject.class,
			ReaderParserObject.class,
			InputStreamParserObject.class,
			LocaleObject.class,
			SwaggerObject.class,
			RequestPathMatchObject.class,
			RequestBodyObject.class,
			ConfigObject.class,
			UriContextObject.class,
			UriResolverObject.class,
			RestRequestPropertiesObject.class
		};

		for (Class<?> c : r) {
			try {
				RestMethodParam mpr = (RestMethodParam)c.newInstance();
				m.put(mpr.forClass(), mpr);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		STANDARD_RESOLVERS = Collections.unmodifiableMap(m);
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Request / Response retrievers
	//-------------------------------------------------------------------------------------------------------------------

	static final class HttpServletRequestObject extends RestMethodParam {

		protected HttpServletRequestObject() {
			super(OTHER, null, HttpServletRequest.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestRequest req, RestResponse res) {
			return req;
		}
	}

	static final class HttpServletResponseObject extends RestMethodParam {

		protected HttpServletResponseObject() {
			super(OTHER, null, HttpServletResponse.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestRequest req, RestResponse res) {
			return res;
		}
	}

	static final class RestRequestObject extends RestMethodParam {

		protected RestRequestObject() {
			super(OTHER, null, RestRequest.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestRequest req, RestResponse res) {
			return req;
		}
	}

	static final class RestResponseObject extends RestMethodParam {

		protected RestResponseObject() {
			super(OTHER, null, RestResponse.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestRequest req, RestResponse res) {
			return res;
		}
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Header retrievers
	//-------------------------------------------------------------------------------------------------------------------

	static final class AcceptHeader extends RestMethodParam {

		protected AcceptHeader() {
			super(HEADER, "Accept-Header", Accept.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getAccept();
		}
	}

	static final class AcceptCharsetHeader extends RestMethodParam {

		protected AcceptCharsetHeader() {
			super(HEADER, "Accept-Charset", AcceptCharset.class);
		}

		@Override /* RestMethodParam */
		public AcceptCharset resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getAcceptCharset();
		}
	}

	static final class AcceptEncodingHeader extends RestMethodParam {

		protected AcceptEncodingHeader() {
			super(HEADER, "Accept-Encoding", AcceptEncoding.class);
		}

		@Override
		public AcceptEncoding resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getAcceptEncoding();
		}
	}

	static final class AcceptLanguageHeader extends RestMethodParam {

		protected AcceptLanguageHeader() {
			super(HEADER, "Accept-Language", AcceptLanguage.class);
		}

		@Override
		public AcceptLanguage resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getAcceptLanguage();
		}
	}

	static final class AuthorizationHeader extends RestMethodParam {

		protected AuthorizationHeader() {
			super(HEADER, "Authorization", Authorization.class);
		}

		@Override
		public Authorization resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getAuthorization();
		}
	}

	static final class CacheControlHeader extends RestMethodParam {

		protected CacheControlHeader() {
			super(HEADER, "Cache-Control", CacheControl.class);
		}

		@Override
		public CacheControl resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getCacheControl();
		}
	}

	static final class ConnectionHeader extends RestMethodParam {

		protected ConnectionHeader() {
			super(HEADER, "Connection", Connection.class);
		}

		@Override
		public Connection resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getConnection();
		}
	}

	static final class ContentLengthHeader extends RestMethodParam {

		protected ContentLengthHeader() {
			super(HEADER, "Content-Length", ContentLength.class);
		}

		@Override
		public ContentLength resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getContentLength();
		}
	}

	static final class ContentTypeHeader extends RestMethodParam {

		protected ContentTypeHeader() {
			super(HEADER, "Content-Type", ContentType.class);
		}

		@Override
		public ContentType resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getContentType();
		}
	}

	static final class DateHeader extends RestMethodParam {

		protected DateHeader() {
			super(HEADER, "Date", Date.class);
		}

		@Override
		public Date resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getDate();
		}
	}

	static final class ExpectHeader extends RestMethodParam {

		protected ExpectHeader() {
			super(HEADER, "Expect", Expect.class);
		}

		@Override
		public Expect resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getExpect();
		}
	}

	static final class FromHeader extends RestMethodParam {

		protected FromHeader() {
			super(HEADER, "From", From.class);
		}

		@Override
		public From resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getFrom();
		}
	}

	static final class HostHeader extends RestMethodParam {

		protected HostHeader() {
			super(HEADER, "Host", Host.class);
		}

		@Override
		public Host resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getHost();
		}
	}

	static final class IfMatchHeader extends RestMethodParam {

		protected IfMatchHeader() {
			super(HEADER, "If-Match", IfMatch.class);
		}

		@Override
		public IfMatch resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getIfMatch();
		}
	}

	static final class IfModifiedSinceHeader extends RestMethodParam {

		protected IfModifiedSinceHeader() {
			super(HEADER, "If-Modified-Since", IfModifiedSince.class);
		}

		@Override
		public IfModifiedSince resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getIfModifiedSince();
		}
	}

	static final class IfNoneMatchHeader extends RestMethodParam {

		protected IfNoneMatchHeader() {
			super(HEADER, "If-None-Match", IfNoneMatch.class);
		}

		@Override
		public IfNoneMatch resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getIfNoneMatch();
		}
	}

	static final class IfRangeHeader extends RestMethodParam {

		protected IfRangeHeader() {
			super(HEADER, "If-Range", IfRange.class);
		}

		@Override
		public IfRange resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getIfRange();
		}
	}

	static final class IfUnmodifiedSinceHeader extends RestMethodParam {

		protected IfUnmodifiedSinceHeader() {
			super(HEADER, "If-Unmodified-Since", IfUnmodifiedSince.class);
		}

		@Override
		public IfUnmodifiedSince resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getIfUnmodifiedSince();
		}
	}

	static final class MaxForwardsHeader extends RestMethodParam {

		protected MaxForwardsHeader() {
			super(HEADER, "Max-Forwards", MaxForwards.class);
		}

		@Override
		public MaxForwards resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getMaxForwards();
		}
	}

	static final class PragmaHeader extends RestMethodParam {

		protected PragmaHeader() {
			super(HEADER, "Pragma", Pragma.class);
		}

		@Override
		public Pragma resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getPragma();
		}
	}

	static final class ProxyAuthorizationHeader extends RestMethodParam {

		protected ProxyAuthorizationHeader() {
			super(HEADER, "Proxy-Authorization", ProxyAuthorization.class);
		}

		@Override
		public ProxyAuthorization resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getProxyAuthorization();
		}
	}

	static final class RangeHeader extends RestMethodParam {

		protected RangeHeader() {
			super(HEADER, "Range", Range.class);
		}

		@Override
		public Range resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getRange();
		}
	}

	static final class RefererHeader extends RestMethodParam {

		protected RefererHeader() {
			super(HEADER, "Referer", Referer.class);
		}

		@Override
		public Referer resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getReferer();
		}
	}

	static final class TEHeader extends RestMethodParam {

		protected TEHeader() {
			super(HEADER, "TE", TE.class);
		}

		@Override
		public TE resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getTE();
		}
	}

	static final class UserAgentHeader extends RestMethodParam {

		protected UserAgentHeader() {
			super(HEADER, "User-Agent", UserAgent.class);
		}

		@Override
		public UserAgent resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getUserAgent();
		}
	}

	static final class UpgradeHeader extends RestMethodParam {

		protected UpgradeHeader() {
			super(HEADER, "Upgrade", Upgrade.class);
		}

		@Override
		public Upgrade resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getUpgrade();
		}
	}

	static final class ViaHeader extends RestMethodParam {

		protected ViaHeader() {
			super(HEADER, "Via", Via.class);
		}

		@Override
		public Via resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getVia();
		}
	}

	static final class WarningHeader extends RestMethodParam {

		protected WarningHeader() {
			super(HEADER, "Warning", Warning.class);
		}

		@Override
		public Warning resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getWarning();
		}
	}

	static final class TimeZoneHeader extends RestMethodParam {

		protected TimeZoneHeader() {
			super(HEADER, "Time-Zone", TimeZone.class);
		}

		@Override
		public TimeZone resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getTimeZone();
		}
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Annotated retrievers
	//-------------------------------------------------------------------------------------------------------------------

	static final class PathParameterObject extends RestMethodParam {

		protected PathParameterObject(String name, Path a, Type type) {
			super(PATH, name, type, getMetaData(a));
		}

		@Override /* RestMethodParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getPathMatch().get(name, type);
		}
		
		private static final ObjectMap getMetaData(Path a) {
			if (a == null)
				return ObjectMap.EMPTY_MAP;
			return new ObjectMap()
				.appendSkipEmpty("description", a.description())
				.appendSkipEmpty("type", a.type())
				.appendSkipEmpty("format", a.format())
				.appendSkipEmpty("pattern", a.pattern())
				.appendSkipEmpty("maximum", a.maximum())
				.appendSkipEmpty("minimum", a.minimum())
				.appendSkipEmpty("multipleOf", a.multipleOf())
				.appendSkipEmpty("maxLength", a.maxLength())
				.appendSkipEmpty("minLength", a.minLength())
				.appendSkipEmpty("allowEmptyVals", a.allowEmptyVals())
				.appendSkipEmpty("exclusiveMaximum", a.exclusiveMaximum())
				.appendSkipEmpty("exclusiveMimimum", a.exclusiveMimimum())
				.appendSkipEmpty("schema", a.schema())
				.appendSkipEmpty("enum", a._enum())
				.appendSkipEmpty("example", a.example())
			;
		}
	}

	static final class BodyObject extends RestMethodParam {

		protected BodyObject(Method method, Body a, Type type, RestMethodParam existing) {
			super(BODY, null, type, getMetaData(a, castOrNull(existing, BodyObject.class)));
		}

		@Override /* RestMethodParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getBody().asType(type);
		}
		
		private static final ObjectMap getMetaData(Body a, BodyObject existing) {
			ObjectMap om = existing == null ? new ObjectMap() : existing.metaData;
			if (a == null)
				return om;
			return om
				.appendSkipEmpty("description", join(a.description()))
				.appendSkipEmpty("required", a.required())
				.appendSkipEmpty("type", a.type())
				.appendSkipEmpty("format", a.format())
				.appendSkipEmpty("pattern", a.pattern())
				.appendSkipEmpty("collectionFormat", a.collectionFormat())
				.appendSkipEmpty("maximum", a.maximum())
				.appendSkipEmpty("minimum", a.minimum())
				.appendSkipEmpty("multipleOf", a.multipleOf())
				.appendSkipEmpty("maxLength", a.maxLength())
				.appendSkipEmpty("minLength", a.minLength())
				.appendSkipEmpty("maxItems", a.maxItems())
				.appendSkipEmpty("minItems", a.minItems())
				.appendSkipEmpty("allowEmptyVals", a.allowEmptyVals())
				.appendSkipEmpty("exclusiveMaximum", a.exclusiveMaximum())
				.appendSkipEmpty("exclusiveMimimum", a.exclusiveMimimum())
				.appendSkipEmpty("uniqueItems", a.uniqueItems())
				.appendSkipEmpty("schema", join(a.schema()))
				.appendSkipEmpty("default", join(a._default()))
				.appendSkipEmpty("enum", join(a._enum()))
				.appendSkipEmpty("items", join(a.items()))
				.appendSkipEmpty("example", join(a.example()))
			;
		}
	}

	static final class HeaderObject extends RestMethodParam {
		private final HttpPartParser partParser;

		protected HeaderObject(Method method, Header a, Type type, PropertyStore ps, RestMethodParam existing) {
			super(HEADER, firstNonEmpty(a.name(), a.value()), type, getMetaData(a, castOrNull(existing, HeaderObject.class)));
			this.partParser = a.parser() == HttpPartParser.Null.class ? null : ClassUtils.newInstance(HttpPartParser.class, a.parser(), true, ps);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getHeaders().get(partParser, name, type);
		}
		
		private static ObjectMap getMetaData(Header a, HeaderObject existing) {
			ObjectMap om = existing == null ? new ObjectMap() : existing.metaData;
			if (a == null)
				return om;
			return om
				.appendSkipEmpty("description", a.description())
				.appendSkipEmpty("required", a.required())
				.appendSkipEmpty("type", a.type())
				.appendSkipEmpty("format", a.format())
				.appendSkipEmpty("pattern", a.pattern())
				.appendSkipEmpty("collectionFormat", a.collectionFormat())
				.appendSkipEmpty("maximum", a.maximum())
				.appendSkipEmpty("minimum", a.minimum())
				.appendSkipEmpty("multipleOf", a.multipleOf())
				.appendSkipEmpty("maxLength", a.maxLength())
				.appendSkipEmpty("minLength", a.minLength())
				.appendSkipEmpty("maxItems", a.maxItems())
				.appendSkipEmpty("minItems", a.minItems())
				.appendSkipEmpty("allowEmptyVals", a.allowEmptyVals())
				.appendSkipEmpty("exclusiveMaximum", a.exclusiveMaximum())
				.appendSkipEmpty("exclusiveMinimum", a.exclusiveMinimum())
				.appendSkipEmpty("uniqueItems", a.uniqueItems())
				.appendSkipEmpty("schema", a.schema())
				.appendSkipEmpty("default", a._default())
				.appendSkipEmpty("enum", a._enum())
				.appendSkipEmpty("items", a.items())
				.appendSkipEmpty("example", a.example())
			;
		}
	}

	static final class ResponseHeaderObject extends RestMethodParam {
		final HttpPartSerializer partSerializer;

		protected ResponseHeaderObject(Method method, ResponseHeader a, Type type, PropertyStore ps, RestMethodParam existing) {
			super(RESPONSE_HEADER, firstNonEmpty(a.name(), a.value(), "Unknown"), type, getMetaData(a, castOrNull(existing, ResponseHeaderObject.class)));
			this.partSerializer = a.serializer() == HttpPartSerializer.Null.class ? null : ClassUtils.newInstance(HttpPartSerializer.class, a.serializer(), true, ps);
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override /* RestMethodParam */
		public Object resolve(RestRequest req, final RestResponse res) throws Exception {
			if (type instanceof Class) {
				Class<?> c = (Class<?>)type;
				if (ClassUtils.isParentClass(Value.class, c)) {
					try {
						Value<Object> v = (Value<Object>)c.newInstance();
						v.listener(new ValueListener() {
							@Override
							public void onSet(Object newValue) {
								res.setHeader(name, partSerializer.serialize(HttpPartType.HEADER, newValue));
							}
						});
						String def = getMetaData().getString("default");
						if (def != null) {
							Class<?> pc = ClassUtils.resolveParameterType(Value.class, 0, c);
							v.set(JsonParser.DEFAULT.parse(def, req.getBeanSession().getClassMeta(pc)));
						}
							
						return v;
					} catch (Exception e) {
						throw new RestException(500, "Invalid type {0} specified with @ResponseHeader annotation.  It must have a public no-arg constructor.", type);
					}
				}
			}
			throw new RestException(500, "Invalid type {0} specified with @ResponseHeader annotation.  It must be a subclass of Value.", type);
		}
		
		public HttpPartSerializer getPartSerializer() {
			return partSerializer;
		}
		
		private static ObjectMap getMetaData(ResponseHeader a, ResponseHeaderObject existing) {
			ObjectMap om = existing == null ? new ObjectMap() : existing.metaData;
			if (a == null)
				return om;
			String code = firstNonEmpty(a.code(), "200");
			for (String c : StringUtils.split(code)) {
				ObjectMap om2 = om.getObjectMap(c, true);
				om2
					.appendSkipEmpty("description", join(a.description()))
					.appendSkipEmpty("type", a.type())
					.appendSkipEmpty("format", a.format())
					.appendSkipEmpty("collectionFormat", a.collectionFormat())
					.appendSkipEmpty("maximum", a.maximum())
					.appendSkipEmpty("minimum", a.minimum())
					.appendSkipEmpty("multipleOf", a.multipleOf())
					.appendSkipEmpty("maxLength", a.maxLength())
					.appendSkipEmpty("minLength", a.minLength())
					.appendSkipEmpty("maxItems", a.maxItems())
					.appendSkipEmpty("minItems", a.minItems())
					.appendSkipEmpty("exclusiveMaximum", a.exclusiveMaximum())
					.appendSkipEmpty("exclusiveMimimum", a.exclusiveMinimum())
					.appendSkipEmpty("uniqueItems", a.uniqueItems())
					.appendSkipEmpty("default", join(a._default()))
					.appendSkipEmpty("enum", join(a._enum()))
					.appendSkipEmpty("items", join(a.items()))
					.appendSkipEmpty("example", join(a.example()))
				;
			}
			return om;
		}
	}

	static final class ResponseObject extends RestMethodParam {

		protected ResponseObject(Method method, Response a, Type type, PropertyStore ps, RestMethodParam existing) {
			super(RESPONSE, "body", type, getMetaData(a, castOrNull(existing, ResponseObject.class)));
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override /* RestMethodParam */
		public Object resolve(RestRequest req, final RestResponse res) throws Exception {
			if (type instanceof Class) {
				Class<?> c = (Class<?>)type;
				if (ClassUtils.isParentClass(Value.class, c)) {
					try {
						Value<Object> v = (Value<Object>)c.newInstance();
						v.listener(new ValueListener() {
							@Override
							public void onSet(Object newValue) {
								res.setOutput(newValue);
							}
						});
						String def = getMetaData().getString("default");
						if (def != null) {
							Class<?> pc = ClassUtils.resolveParameterType(Value.class, 0, c);
							v.set(JsonParser.DEFAULT.parse(def, req.getBeanSession().getClassMeta(pc)));
						}
							
						return v;
					} catch (Exception e) {
						throw new RestException(500, "Invalid type {0} specified with @ResponseHeader annotation.  It must have a public no-arg constructor.", type);
					}
				}
			}
			throw new RestException(500, "Invalid type {0} specified with @ResponseHeader annotation.  It must be a subclass of Value.", type);
		}
		
		private static ObjectMap getMetaData(Response a, ResponseObject existing) {
			ObjectMap om = existing == null ? new ObjectMap() : existing.metaData;
			if (a == null)
				return om;
			int status = ObjectUtils.firstNonZero(a.code(), a.value(), 200);
			ObjectMap om2 = om.getObjectMap(String.valueOf(status), true);
			om2
				.appendSkipEmpty("description", join(a.description()))
				.appendSkipEmpty("schema", join(a.schema()))
				.appendSkipEmpty("headers", join(a.headers()))
				.appendSkipEmpty("example", join(a.example()))
			;
			return om;
		}
	}

	static final class ResponseStatusObject extends RestMethodParam {

		protected ResponseStatusObject(Method method, ResponseStatus a, Type type, PropertyStore ps, RestMethodParam existing) {
			super(RESPONSE_STATUS, "", type, getMetaData(a, castOrNull(existing, ResponseStatusObject.class)));
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override /* RestMethodParam */
		public Object resolve(RestRequest req, final RestResponse res) throws Exception {
			if (type instanceof Class) {
				Class<?> c = (Class<?>)type;
				if (ClassUtils.isParentClass(Value.class, c)) {
					try {
						Value<Object> v = (Value<Object>)c.newInstance();
						v.listener(new ValueListener() {
							@Override
							public void onSet(Object newValue) {
								res.setStatus(Integer.parseInt(newValue.toString()));
							}
						});
						String def = getMetaData().getString("default");
						if (def != null) {
							Class<?> pc = ClassUtils.resolveParameterType(Value.class, 0, c);
							v.set(JsonParser.DEFAULT.parse(def, req.getBeanSession().getClassMeta(pc)));
						}
							
						return v;
					} catch (Exception e) {
						throw new RestException(500, "Invalid type {0} specified with @ResponseStatus annotation.  It must have a public no-arg constructor.", type);
					}
				}
			}
			throw new RestException(500, "Invalid type {0} specified with @ResponseStatus annotation.  It must be a subclass of Value.", type);
		}
		
		private static ObjectMap getMetaData(ResponseStatus a, ResponseStatusObject existing) {
			ObjectMap om = existing == null ? new ObjectMap() : existing.metaData;
			if (a == null)
				return om;
			int status = firstNonZero(a.code(), a.value(), 200);
			ObjectMap om2 = om.getObjectMap(String.valueOf(status), true);
			om2
				.appendSkipEmpty("description", join(a.description()))
			;
			return om;
		}
	}

	static final class MethodObject extends RestMethodParam {

		protected MethodObject(Method method, Type type) throws ServletException {
			super(OTHER, null, null);
			if (type != String.class)
				throw new RestServletException("Use of @Method annotation on parameter that is not a String on method ''{0}''", method);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getMethod();
		}
	}

	static final class FormDataObject extends RestMethodParam {
		private final boolean multiPart;
		private final HttpPartParser partParser;

		protected FormDataObject(Method method, FormData a, Type type, PropertyStore ps, RestMethodParam existing) throws ServletException {
			super(FORM_DATA, firstNonEmpty(a.name(), a.value()), type, getMetaData(a, castOrNull(existing, FormDataObject.class)));
			if (a.multipart() && ! isCollection(type))
					throw new RestServletException("Use of multipart flag on @FormData parameter that's not an array or Collection on method ''{0}''", method);
			this.multiPart = a.multipart();
			this.partParser = a.parser() == HttpPartParser.Null.class ? null : ClassUtils.newInstance(HttpPartParser.class, a.parser(), true, ps);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			if (multiPart)
				return req.getFormData().getAll(partParser, name, type);
			return req.getFormData().get(partParser, name, type);
		}
		
		private static final ObjectMap getMetaData(FormData a, FormDataObject existing) {
			ObjectMap om = existing == null ? new ObjectMap() : existing.metaData;
			if (a == null)
				return om;
			return om
				.appendSkipEmpty("description", a.description())
				.appendSkipEmpty("required", a.required())
				.appendSkipEmpty("type", a.type())
				.appendSkipEmpty("format", a.format())
				.appendSkipEmpty("pattern", a.pattern())
				.appendSkipEmpty("collectionFormat", a.collectionFormat())
				.appendSkipEmpty("maximum", a.maximum())
				.appendSkipEmpty("minimum", a.minimum())
				.appendSkipEmpty("multipleOf", a.multipleOf())
				.appendSkipEmpty("maxLength", a.maxLength())
				.appendSkipEmpty("minLength", a.minLength())
				.appendSkipEmpty("maxItems", a.maxItems())
				.appendSkipEmpty("minItems", a.minItems())
				.appendSkipEmpty("allowEmptyVals", a.allowEmptyVals())
				.appendSkipEmpty("exclusiveMaximum", a.exclusiveMaximum())
				.appendSkipEmpty("exclusiveMimimum", a.exclusiveMimimum())
				.appendSkipEmpty("uniqueItems", a.uniqueItems())
				.appendSkipEmpty("schema", a.schema())
				.appendSkipEmpty("default", a._default())
				.appendSkipEmpty("enum", a._enum())
				.appendSkipEmpty("items", a.items())
				.appendSkipEmpty("example", a.example())
			;
		}
	}

	static final class QueryObject extends RestMethodParam {
		private final boolean multiPart;
		private final HttpPartParser partParser;

		protected QueryObject(Method method, Query a, Type type, PropertyStore ps, RestMethodParam existing) throws ServletException {
			super(QUERY, firstNonEmpty(a.name(), a.value()), type, getMetaData(a, castOrNull(existing, QueryObject.class)));
			if (a.multipart() && ! isCollection(type))
					throw new RestServletException("Use of multipart flag on @Query parameter that's not an array or Collection on method ''{0}''", method);
			this.multiPart = a.multipart();
			this.partParser = a.parser() == HttpPartParser.Null.class ? null : ClassUtils.newInstance(HttpPartParser.class, a.parser(), true, ps);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			if (multiPart)
				return req.getQuery().getAll(partParser, name, type);
			return req.getQuery().get(partParser, name, type);
		}
		
		private static final ObjectMap getMetaData(Query a, QueryObject existing) {
			ObjectMap om = existing == null ? new ObjectMap() : existing.metaData;
			if (a == null)
				return om;
			return om
				.appendSkipEmpty("description", a.description())
				.appendSkipEmpty("required", a.required())
				.appendSkipEmpty("type", a.type())
				.appendSkipEmpty("format", a.format())
				.appendSkipEmpty("pattern", a.pattern())
				.appendSkipEmpty("collectionFormat", a.collectionFormat())
				.appendSkipEmpty("maximum", a.maximum())
				.appendSkipEmpty("minimum", a.minimum())
				.appendSkipEmpty("multipleOf", a.multipleOf())
				.appendSkipEmpty("maxLength", a.maxLength())
				.appendSkipEmpty("minLength", a.minLength())
				.appendSkipEmpty("maxItems", a.maxItems())
				.appendSkipEmpty("minItems", a.minItems())
				.appendSkipEmpty("allowEmptyVals", a.allowEmptyVals())
				.appendSkipEmpty("exclusiveMaximum", a.exclusiveMaximum())
				.appendSkipEmpty("exclusiveMimimum", a.exclusiveMimimum())
				.appendSkipEmpty("uniqueItems", a.uniqueItems())
				.appendSkipEmpty("schema", a.schema())
				.appendSkipEmpty("default", a._default())
				.appendSkipEmpty("enum", a._enum())
				.appendSkipEmpty("items", a.items())
				.appendSkipEmpty("example", a.example())
			;
		}

	}

	static final class HasFormDataObject extends RestMethodParam {

		protected HasFormDataObject(Method method, HasFormData a, Type type) throws ServletException {
			super(FORM_DATA, firstNonEmpty(a.name(), a.value()), type);
			if (type != Boolean.class && type != boolean.class)
				throw new RestServletException("Use of @HasForm annotation on parameter that is not a boolean on method ''{0}''", method);
	}

		@Override /* RestMethodParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			BeanSession bs = req.getBeanSession();
			return bs.convertToType(req.getFormData().containsKey(name), bs.getClassMeta(type));
		}
	}

	static final class HasQueryObject extends RestMethodParam {

		protected HasQueryObject(Method method, HasQuery a, Type type) throws ServletException {
			super(QUERY, firstNonEmpty(a.name(), a.value()), type);
			if (type != Boolean.class && type != boolean.class)
				throw new RestServletException("Use of @HasQuery annotation on parameter that is not a boolean on method ''{0}''", method);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			BeanSession bs = req.getBeanSession();
			return bs.convertToType(req.getQuery().containsKey(name), bs.getClassMeta(type));
		}
	}

	static final class PathRemainderObject extends RestMethodParam {

		protected PathRemainderObject(Method method, Type type) throws ServletException {
			super(OTHER, null, null);
			if (type != String.class)
				throw new RestServletException("Use of @PathRemainder annotation on parameter that is not a String on method ''{0}''", method);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getPathMatch().getRemainder();
		}
	}

	static final class RestRequestPropertiesObject extends RestMethodParam {

		protected RestRequestPropertiesObject() {
			super(OTHER, null, RequestProperties.class);
		}

		@Override /* RestMethodParam */
		public RequestProperties resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getProperties();
		}
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Other retrievers
	//-------------------------------------------------------------------------------------------------------------------

	static final class ResourceBundleObject extends RestMethodParam {

		protected ResourceBundleObject() {
			super(OTHER, null, ResourceBundle.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getMessageBundle();
		}
	}

	static final class MessageBundleObject extends RestMethodParam {

		protected MessageBundleObject() {
			super(OTHER, null, MessageBundle.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getMessageBundle();
		}
	}

	static final class InputStreamObject extends RestMethodParam {

		protected InputStreamObject() {
			super(OTHER, null, InputStream.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getInputStream();
		}
	}

	static final class ServletInputStreamObject extends RestMethodParam {

		protected ServletInputStreamObject() {
			super(OTHER, null, ServletInputStream.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getInputStream();
		}
	}

	static final class ReaderObject extends RestMethodParam {

		protected ReaderObject() {
			super(OTHER, null, Reader.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getReader();
		}
	}

	static final class OutputStreamObject extends RestMethodParam {

		protected OutputStreamObject() {
			super(OTHER, null, OutputStream.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return res.getOutputStream();
		}
	}

	static final class ServletOutputStreamObject extends RestMethodParam {

		protected ServletOutputStreamObject() {
			super(OTHER, null, ServletOutputStream.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return res.getOutputStream();
		}
	}

	static final class WriterObject extends RestMethodParam {

		protected WriterObject() {
			super(OTHER, null, Writer.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return res.getWriter();
		}
	}

	static final class RequestHeadersObject extends RestMethodParam {

		protected RequestHeadersObject() {
			super(OTHER, null, RequestHeaders.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getHeaders();
		}
	}

	static final class RequestQueryObject extends RestMethodParam {

		protected RequestQueryObject() {
			super(OTHER, null, RequestQuery.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getQuery();
		}
	}

	static final class RequestFormDataObject extends RestMethodParam {

		protected RequestFormDataObject() {
			super(OTHER, null, RequestFormData.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getFormData();
		}
	}

	static final class HttpMethodObject extends RestMethodParam {

		protected HttpMethodObject() {
			super(OTHER, null, HttpMethod.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getHttpMethod();
		}
	}

	static final class RestLoggerObject extends RestMethodParam {

		protected RestLoggerObject() {
			super(OTHER, null, RestLogger.class);
		}

		@Override /* RestMethodParam */
		public RestLogger resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getContext().getLogger();
		}
	}

	static final class RestContextObject extends RestMethodParam {

		protected RestContextObject() {
			super(OTHER, null, RestContext.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getContext();
		}
	}

	static final class ParserObject extends RestMethodParam {

		protected ParserObject() {
			super(OTHER, null, Parser.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getBody().getParser();
		}
	}

	static final class ReaderParserObject extends RestMethodParam {

		protected ReaderParserObject() {
			super(OTHER, null, ReaderParser.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getBody().getReaderParser();
		}
	}

	static final class InputStreamParserObject extends RestMethodParam {

		protected InputStreamParserObject() {
			super(OTHER, null, InputStreamParser.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getBody().getInputStreamParser();
		}
	}

	static final class LocaleObject extends RestMethodParam {

		protected LocaleObject() {
			super(OTHER, null, Locale.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getLocale();
		}
	}

	static final class SwaggerObject extends RestMethodParam {

		protected SwaggerObject() {
			super(OTHER, null, Swagger.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getSwagger();
		}
	}

	static final class RequestPathMatchObject extends RestMethodParam {

		protected RequestPathMatchObject() {
			super(OTHER, null, RequestPathMatch.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getPathMatch();
		}
	}

	static final class RequestBodyObject extends RestMethodParam {

		protected RequestBodyObject() {
			super(BODY, null, RequestBody.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getBody();
		}
	}

	static final class ConfigObject extends RestMethodParam {

		protected ConfigObject() {
			super(OTHER, null, Config.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getConfig();
		}
	}

	static final class UriContextObject extends RestMethodParam {

		protected UriContextObject() {
			super(OTHER, null, UriContext.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getUriContext();
		}
	}

	static final class UriResolverObject extends RestMethodParam {

		protected UriResolverObject() {
			super(OTHER, null, UriResolver.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getUriResolver();
		}
	}

	static final boolean isCollection(Type t) {
		return BeanContext.DEFAULT.getClassMeta(t).isCollectionOrArray();
	}
	
	static final String join(String...s) {
		return StringUtils.join(s, '\n');
	}
}
