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
import static org.apache.juneau.rest.RestParamType.*;

import java.io.*;
import java.lang.reflect.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.juneau.*;
import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.Date;
import org.apache.juneau.httppart.*;
import org.apache.juneau.ini.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.utils.*;

/**
 * Default REST method parameter resolvers.
 */
class RestParamDefaults {

	/**
	 * Standard set of method parameter resolvers.
	 */
	static final Map<Class<?>,RestParam> STANDARD_RESOLVERS;

	static {
		Map<Class<?>,RestParam> m = new HashMap<>();

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
			LoggerObject.class,
			JuneauLoggerObject.class,
			RestContextObject.class,
			ParserObject.class,
			LocaleObject.class,
			SwaggerObject.class,
			RequestPathMatchObject.class,
			RequestBodyObject.class,
			ConfigFileObject.class,
			UriContextObject.class,
			UriResolverObject.class,
		};

		for (Class<?> c : r) {
			try {
				RestParam mpr = (RestParam)c.newInstance();
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

	static final class HttpServletRequestObject extends RestParam {

		protected HttpServletRequestObject() {
			super(OTHER, null, HttpServletRequest.class);
		}

		@Override /* RestParam */
		public Object resolve(RestRequest req, RestResponse res) {
			return req;
		}
	}

	static final class HttpServletResponseObject extends RestParam {

		protected HttpServletResponseObject() {
			super(OTHER, null, HttpServletResponse.class);
		}

		@Override /* RestParam */
		public Object resolve(RestRequest req, RestResponse res) {
			return res;
		}
	}

	static final class RestRequestObject extends RestParam {

		protected RestRequestObject() {
			super(OTHER, null, RestRequest.class);
		}

		@Override /* RestParam */
		public Object resolve(RestRequest req, RestResponse res) {
			return req;
		}
	}

	static final class RestResponseObject extends RestParam {

		protected RestResponseObject() {
			super(OTHER, null, RestResponse.class);
		}

		@Override /* RestParam */
		public Object resolve(RestRequest req, RestResponse res) {
			return res;
		}
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Header retrievers
	//-------------------------------------------------------------------------------------------------------------------

	static final class AcceptHeader extends RestParam {

		protected AcceptHeader() {
			super(HEADER, "Accept-Header", Accept.class);
		}

		@Override /* RestParam */
		public Object resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getAccept();
		}
	}

	static final class AcceptCharsetHeader extends RestParam {

		protected AcceptCharsetHeader() {
			super(HEADER, "Accept-Charset", AcceptCharset.class);
		}

		@Override /* RestParam */
		public AcceptCharset resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getAcceptCharset();
		}
	}

	static final class AcceptEncodingHeader extends RestParam {

		protected AcceptEncodingHeader() {
			super(HEADER, "Accept-Encoding", AcceptEncoding.class);
		}

		@Override
		public AcceptEncoding resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getAcceptEncoding();
		}
	}

	static final class AcceptLanguageHeader extends RestParam {

		protected AcceptLanguageHeader() {
			super(HEADER, "Accept-Language", AcceptLanguage.class);
		}

		@Override
		public AcceptLanguage resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getAcceptLanguage();
		}
	}

	static final class AuthorizationHeader extends RestParam {

		protected AuthorizationHeader() {
			super(HEADER, "Authorization", Authorization.class);
		}

		@Override
		public Authorization resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getAuthorization();
		}
	}

	static final class CacheControlHeader extends RestParam {

		protected CacheControlHeader() {
			super(HEADER, "Cache-Control", CacheControl.class);
		}

		@Override
		public CacheControl resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getCacheControl();
		}
	}

	static final class ConnectionHeader extends RestParam {

		protected ConnectionHeader() {
			super(HEADER, "Connection", Connection.class);
		}

		@Override
		public Connection resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getConnection();
		}
	}

	static final class ContentLengthHeader extends RestParam {

		protected ContentLengthHeader() {
			super(HEADER, "Content-Length", ContentLength.class);
		}

		@Override
		public ContentLength resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getContentLength();
		}
	}

	static final class ContentTypeHeader extends RestParam {

		protected ContentTypeHeader() {
			super(HEADER, "Content-Type", ContentType.class);
		}

		@Override
		public ContentType resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getContentType();
		}
	}

	static final class DateHeader extends RestParam {

		protected DateHeader() {
			super(HEADER, "Date", Date.class);
		}

		@Override
		public Date resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getDate();
		}
	}

	static final class ExpectHeader extends RestParam {

		protected ExpectHeader() {
			super(HEADER, "Expect", Expect.class);
		}

		@Override
		public Expect resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getExpect();
		}
	}

	static final class FromHeader extends RestParam {

		protected FromHeader() {
			super(HEADER, "From", From.class);
		}

		@Override
		public From resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getFrom();
		}
	}

	static final class HostHeader extends RestParam {

		protected HostHeader() {
			super(HEADER, "Host", Host.class);
		}

		@Override
		public Host resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getHost();
		}
	}

	static final class IfMatchHeader extends RestParam {

		protected IfMatchHeader() {
			super(HEADER, "If-Match", IfMatch.class);
		}

		@Override
		public IfMatch resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getIfMatch();
		}
	}

	static final class IfModifiedSinceHeader extends RestParam {

		protected IfModifiedSinceHeader() {
			super(HEADER, "If-Modified-Since", IfModifiedSince.class);
		}

		@Override
		public IfModifiedSince resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getIfModifiedSince();
		}
	}

	static final class IfNoneMatchHeader extends RestParam {

		protected IfNoneMatchHeader() {
			super(HEADER, "If-None-Match", IfNoneMatch.class);
		}

		@Override
		public IfNoneMatch resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getIfNoneMatch();
		}
	}

	static final class IfRangeHeader extends RestParam {

		protected IfRangeHeader() {
			super(HEADER, "If-Range", IfRange.class);
		}

		@Override
		public IfRange resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getIfRange();
		}
	}

	static final class IfUnmodifiedSinceHeader extends RestParam {

		protected IfUnmodifiedSinceHeader() {
			super(HEADER, "If-Unmodified-Since", IfUnmodifiedSince.class);
		}

		@Override
		public IfUnmodifiedSince resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getIfUnmodifiedSince();
		}
	}

	static final class MaxForwardsHeader extends RestParam {

		protected MaxForwardsHeader() {
			super(HEADER, "Max-Forwards", MaxForwards.class);
		}

		@Override
		public MaxForwards resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getMaxForwards();
		}
	}

	static final class PragmaHeader extends RestParam {

		protected PragmaHeader() {
			super(HEADER, "Pragma", Pragma.class);
		}

		@Override
		public Pragma resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getPragma();
		}
	}

	static final class ProxyAuthorizationHeader extends RestParam {

		protected ProxyAuthorizationHeader() {
			super(HEADER, "Proxy-Authorization", ProxyAuthorization.class);
		}

		@Override
		public ProxyAuthorization resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getProxyAuthorization();
		}
	}

	static final class RangeHeader extends RestParam {

		protected RangeHeader() {
			super(HEADER, "Range", Range.class);
		}

		@Override
		public Range resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getRange();
		}
	}

	static final class RefererHeader extends RestParam {

		protected RefererHeader() {
			super(HEADER, "Referer", Referer.class);
		}

		@Override
		public Referer resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getReferer();
		}
	}

	static final class TEHeader extends RestParam {

		protected TEHeader() {
			super(HEADER, "TE", TE.class);
		}

		@Override
		public TE resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getTE();
		}
	}

	static final class UserAgentHeader extends RestParam {

		protected UserAgentHeader() {
			super(HEADER, "User-Agent", UserAgent.class);
		}

		@Override
		public UserAgent resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getUserAgent();
		}
	}

	static final class UpgradeHeader extends RestParam {

		protected UpgradeHeader() {
			super(HEADER, "Upgrade", Upgrade.class);
		}

		@Override
		public Upgrade resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getUpgrade();
		}
	}

	static final class ViaHeader extends RestParam {

		protected ViaHeader() {
			super(HEADER, "Via", Via.class);
		}

		@Override
		public Via resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getVia();
		}
	}

	static final class WarningHeader extends RestParam {

		protected WarningHeader() {
			super(HEADER, "Warning", Warning.class);
		}

		@Override
		public Warning resolve(RestRequest req, RestResponse res) {
			return req.getHeaders().getWarning();
		}
	}

	static final class TimeZoneHeader extends RestParam {

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

	static final class PathParameterObject extends RestParam {

		protected PathParameterObject(String name, Type type) {
			super(PATH, name, type);
		}

		@Override /* RestParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getPathMatch().get(name, type);
		}
	}

	static final class BodyObject extends RestParam {

		protected BodyObject(Type type) {
			super(BODY, null, type);
		}

		@Override /* RestParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getBody().asType(type);
		}
	}

	static final class HeaderObject extends RestParam {
		private final HttpPartParser partParser;

		protected HeaderObject(Header a, Type type, PropertyStore ps) {
			super(HEADER, firstNonEmpty(a.name(), a.value()), type);
			this.partParser = a.parser() == HttpPartParser.Null.class ? null : ClassUtils.newInstance(HttpPartParser.class, a.parser(), true, ps);
		}

		@Override /* RestParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getHeaders().get(partParser, name, type);
		}
	}

	static final class MethodObject extends RestParam {

		protected MethodObject(Method method, Type type) throws ServletException {
			super(OTHER, null, null);
			if (type != String.class)
				throw new RestServletException("Use of @Method annotation on parameter that is not a String on method ''{0}''", method);
		}

		@Override /* RestParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getMethod();
		}
	}

	static final class FormDataObject extends RestParam {
		private final boolean multiPart;
		private final HttpPartParser partParser;

		protected FormDataObject(Method method, FormData a, Type type, PropertyStore ps) throws ServletException {
			super(FORMDATA, firstNonEmpty(a.name(), a.value()), type);
			if (a.multipart() && ! isCollection(type))
					throw new RestServletException("Use of multipart flag on @FormData parameter that's not an array or Collection on method ''{0}''", method);
			this.multiPart = a.multipart();
			this.partParser = a.parser() == HttpPartParser.Null.class ? null : ClassUtils.newInstance(HttpPartParser.class, a.parser(), true, ps);
		}

		@Override /* RestParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			if (multiPart)
				return req.getFormData().getAll(partParser, name, type);
			return req.getFormData().get(partParser, name, type);
		}
	}

	static final class QueryObject extends RestParam {
		private final boolean multiPart;
		private final HttpPartParser partParser;

		protected QueryObject(Method method, Query a, Type type, PropertyStore ps) throws ServletException {
			super(QUERY, firstNonEmpty(a.name(), a.value()), type);
			if (a.multipart() && ! isCollection(type))
					throw new RestServletException("Use of multipart flag on @Query parameter that's not an array or Collection on method ''{0}''", method);
			this.multiPart = a.multipart();
			this.partParser = a.parser() == HttpPartParser.Null.class ? null : ClassUtils.newInstance(HttpPartParser.class, a.parser(), true, ps);
		}

		@Override /* RestParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			if (multiPart)
				return req.getQuery().getAll(partParser, name, type);
			return req.getQuery().get(partParser, name, type);
		}
	}

	static final class HasFormDataObject extends RestParam {

		protected HasFormDataObject(Method method, HasFormData a, Type type) throws ServletException {
			super(FORMDATA, firstNonEmpty(a.name(), a.value()), type);
			if (type != Boolean.class && type != boolean.class)
				throw new RestServletException("Use of @HasForm annotation on parameter that is not a boolean on method ''{0}''", method);
	}

		@Override /* RestParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			BeanSession bs = req.getBeanSession();
			return bs.convertToType(req.getFormData().containsKey(name), bs.getClassMeta(type));
		}
	}

	static final class HasQueryObject extends RestParam {

		protected HasQueryObject(Method method, HasQuery a, Type type) throws ServletException {
			super(QUERY, firstNonEmpty(a.name(), a.value()), type);
			if (type != Boolean.class && type != boolean.class)
				throw new RestServletException("Use of @HasQuery annotation on parameter that is not a boolean on method ''{0}''", method);
		}

		@Override /* RestParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			BeanSession bs = req.getBeanSession();
			return bs.convertToType(req.getQuery().containsKey(name), bs.getClassMeta(type));
		}
	}

	static final class PathRemainderObject extends RestParam {

		protected PathRemainderObject(Method method, Type type) throws ServletException {
			super(OTHER, null, null);
			if (type != String.class)
				throw new RestServletException("Use of @PathRemainder annotation on parameter that is not a String on method ''{0}''", method);
		}

		@Override /* RestParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getPathMatch().getRemainder();
		}
	}

	static final class PropsObject extends RestParam {

		protected PropsObject(Method method, Type type) throws ServletException {
			super(OTHER, null, null);
			if (type != ObjectMap.class)
				throw new RestServletException("Use of @Properties annotation on parameter that is not an ObjectMap on method ''{0}''", method);
		}

		@Override /* RestParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getProperties();
		}
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Other retrievers
	//-------------------------------------------------------------------------------------------------------------------

	static final class ResourceBundleObject extends RestParam {

		protected ResourceBundleObject() {
			super(OTHER, null, ResourceBundle.class);
		}

		@Override /* RestParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getResourceBundle();
		}
	}

	static final class MessageBundleObject extends RestParam {

		protected MessageBundleObject() {
			super(OTHER, null, MessageBundle.class);
		}

		@Override /* RestParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getResourceBundle();
		}
	}

	static final class InputStreamObject extends RestParam {

		protected InputStreamObject() {
			super(OTHER, null, InputStream.class);
		}

		@Override /* RestParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getInputStream();
		}
	}

	static final class ServletInputStreamObject extends RestParam {

		protected ServletInputStreamObject() {
			super(OTHER, null, ServletInputStream.class);
		}

		@Override /* RestParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getInputStream();
		}
	}

	static final class ReaderObject extends RestParam {

		protected ReaderObject() {
			super(OTHER, null, Reader.class);
		}

		@Override /* RestParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getReader();
		}
	}

	static final class OutputStreamObject extends RestParam {

		protected OutputStreamObject() {
			super(OTHER, null, OutputStream.class);
		}

		@Override /* RestParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return res.getOutputStream();
		}
	}

	static final class ServletOutputStreamObject extends RestParam {

		protected ServletOutputStreamObject() {
			super(OTHER, null, ServletOutputStream.class);
		}

		@Override /* RestParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return res.getOutputStream();
		}
	}

	static final class WriterObject extends RestParam {

		protected WriterObject() {
			super(OTHER, null, Writer.class);
		}

		@Override /* RestParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return res.getWriter();
		}
	}

	static final class RequestHeadersObject extends RestParam {

		protected RequestHeadersObject() {
			super(OTHER, null, RequestHeaders.class);
		}

		@Override /* RestParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getHeaders();
		}
	}

	static final class RequestQueryObject extends RestParam {

		protected RequestQueryObject() {
			super(OTHER, null, RequestQuery.class);
		}

		@Override /* RestParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getQuery();
		}
	}

	static final class RequestFormDataObject extends RestParam {

		protected RequestFormDataObject() {
			super(OTHER, null, RequestFormData.class);
		}

		@Override /* RestParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getFormData();
		}
	}

	static final class HttpMethodObject extends RestParam {

		protected HttpMethodObject() {
			super(OTHER, null, HttpMethod.class);
		}

		@Override /* RestParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getHttpMethod();
		}
	}

	static final class LoggerObject extends RestParam {

		protected LoggerObject() {
			super(OTHER, null, Logger.class);
		}

		@Override /* RestParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getContext().getLogger().getLogger();
		}
	}

	static final class JuneauLoggerObject extends RestParam {

		protected JuneauLoggerObject() {
			super(OTHER, null, JuneauLogger.class);
		}

		@Override /* RestParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getContext().getLogger().getLogger();
		}
	}

	static final class RestContextObject extends RestParam {

		protected RestContextObject() {
			super(OTHER, null, RestContext.class);
		}

		@Override /* RestParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getContext();
		}
	}

	static final class ParserObject extends RestParam {

		protected ParserObject() {
			super(OTHER, null, Parser.class);
		}

		@Override /* RestParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getBody().getParser();
		}
	}

	static final class LocaleObject extends RestParam {

		protected LocaleObject() {
			super(OTHER, null, Locale.class);
		}

		@Override /* RestParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getLocale();
		}
	}

	static final class SwaggerObject extends RestParam {

		protected SwaggerObject() {
			super(OTHER, null, Swagger.class);
		}

		@Override /* RestParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getSwagger();
		}
	}

	static final class RequestPathMatchObject extends RestParam {

		protected RequestPathMatchObject() {
			super(OTHER, null, RequestPathMatch.class);
		}

		@Override /* RestParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getPathMatch();
		}
	}

	static final class RequestBodyObject extends RestParam {

		protected RequestBodyObject() {
			super(BODY, null, RequestBody.class);
		}

		@Override /* RestParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getBody();
		}
	}

	static final class ConfigFileObject extends RestParam {

		protected ConfigFileObject() {
			super(OTHER, null, ConfigFile.class);
		}

		@Override /* RestParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getConfigFile();
		}
	}

	static final class UriContextObject extends RestParam {

		protected UriContextObject() {
			super(OTHER, null, UriContext.class);
		}

		@Override /* RestParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getUriContext();
		}
	}

	static final class UriResolverObject extends RestParam {

		protected UriResolverObject() {
			super(OTHER, null, UriResolver.class);
		}

		@Override /* RestParam */
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return req.getUriResolver();
		}
	}

	static final boolean isCollection(Type t) {
		return BeanContext.DEFAULT.getClassMeta(t).isCollectionOrArray();
	}
}
