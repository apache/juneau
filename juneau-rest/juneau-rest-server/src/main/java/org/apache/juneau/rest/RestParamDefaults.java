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
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.rest.RestParamType.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.juneau.*;
import org.apache.juneau.config.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.annotation.Body;
import org.apache.juneau.http.annotation.FormData;
import org.apache.juneau.http.annotation.HasFormData;
import org.apache.juneau.http.annotation.HasQuery;
import org.apache.juneau.http.annotation.Header;
import org.apache.juneau.http.annotation.Path;
import org.apache.juneau.http.annotation.Query;
import org.apache.juneau.httppart.*;
import org.apache.juneau.httppart.bean.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.http.exception.*;
import org.apache.juneau.rest.util.UrlPathMatcher;
import org.apache.juneau.serializer.*;

/**
 * Default REST method parameter resolvers.
 *
 * <p>
 * Contains the default set of parameter resolvers for REST resource methods (i.e methods annotated with {@link RestResource @RestResource}).
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc RestmParameters}
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
			RequestAttributesObject.class,
			RequestQueryObject.class,
			RequestFormDataObject.class,
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
			UriResolverObject.class
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
			super(OTHER, HttpServletRequest.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestCall call) {
			return call.getRequest();
		}
	}

	static final class HttpServletResponseObject extends RestMethodParam {

		protected HttpServletResponseObject() {
			super(OTHER, HttpServletResponse.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestCall call) {
			return call.getResponse();
		}
	}

	static final class RestRequestObject extends RestMethodParam {

		protected RestRequestObject() {
			super(OTHER, RestRequest.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestCall call) {
			return call.getRestRequest();
		}
	}

	static final class RestResponseObject extends RestMethodParam {

		protected RestResponseObject() {
			super(OTHER, RestResponse.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestCall call) {
			return call.getRestResponse();
		}
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Header retrievers
	//-------------------------------------------------------------------------------------------------------------------

	static final class TimeZoneHeader extends RestMethodParam {

		protected TimeZoneHeader() {
			super(HEADER, "Time-Zone", TimeZone.class);
		}

		@Override
		public TimeZone resolve(RestCall call) {
			return call.getRestRequest().getHeaders().getTimeZone();
		}
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Annotated retrievers
	//-------------------------------------------------------------------------------------------------------------------

	static final class PathObject extends RestMethodParam {
		private final HttpPartParser partParser;
		private final HttpPartSchema schema;

		protected PathObject(ParamInfo mpi, PropertyStore ps, UrlPathMatcher pathMatcher) {
			super(PATH, mpi, getName(mpi, pathMatcher));
			this.schema = HttpPartSchema.create(Path.class, mpi);
			this.partParser = createPartParser(schema.getParser(), ps);
		}

		private static String getName(ParamInfo mpi, UrlPathMatcher pathMatcher) {
			String p = null;
			for (Path h : mpi.getAnnotations(Path.class))
				p = firstNonEmpty(h.name(), h.n(), h.value(), p);
			if (p != null)
				return p;
			if (pathMatcher != null) {
				int idx = 0;
				int i = mpi.getIndex();
				MethodInfo mi = mpi.getMethod();

				for (int j = 0; j < i; j++)
					if (mi.getParam(i).getLastAnnotation(Path.class) != null)
						idx++;

				String[] vars = pathMatcher.getVars();
				if (vars.length <= idx)
					throw new InternalServerError("Number of attribute parameters in method ''{0}'' exceeds the number of URL pattern variables.", mi.getFullName());

				// Check for {#} variables.
				String idxs = String.valueOf(idx);
				for (int j = 0; j < vars.length; j++)
					if (StringUtils.isNumeric(vars[j]) && vars[j].equals(idxs))
						return vars[j];

				return pathMatcher.getVars()[idx];
			}
			throw new InternalServerError("@Path used without name or value on method parameter ''{0}''.", mpi);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestCall call) throws Exception {
			RestRequest req = call.getRestRequest();
			HttpPartParserSession ps = partParser == null ? req.getPartParser() : partParser.createPartSession(req.getParserSessionArgs());
			return call.getRestRequest().getPathMatch().get(ps, schema, name, type);
		}
	}

	static final class BodyObject extends RestMethodParam {
		private final HttpPartSchema schema;

		protected BodyObject(ParamInfo mpi, PropertyStore ps) {
			super(BODY, mpi);
			this.schema = HttpPartSchema.create(Body.class, mpi);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestCall call) throws Exception {
			return call.getRestRequest().getBody().schema(schema).asType(type);
		}
	}

	static final class HeaderObject extends RestMethodParam {
		private final HttpPartParser partParser;
		private final HttpPartSchema schema;
		private final boolean multi;

		protected HeaderObject(ParamInfo mpi, PropertyStore ps) {
			super(HEADER, mpi, getName(mpi));
			this.schema = HttpPartSchema.create(Header.class, mpi);
			this.partParser = createPartParser(schema.getParser(), ps);
			this.multi = getMulti(mpi);

			if (multi && ! isCollection(type))
				throw new InternalServerError("Use of multipart flag on @Header parameter that's not an array or Collection on method ''{0}''", mpi.getMethod());
		}

		private static String getName(ParamInfo mpi) {
			String n = null;
			for (Header h : mpi.getAnnotations(Header.class))
				n = firstNonEmpty(h.name(), h.n(), h.value(), n);
			if (n == null)
				throw new InternalServerError("@Header used without name or value on method parameter ''{0}''.", mpi);
			return n;
		}

		private static boolean getMulti(ParamInfo mpi) {
			for (Header h : mpi.getAnnotations(Header.class))
				if (h.multi())
					return true;
			return false;
		}

		@Override /* RestMethodParam */
		public Object resolve(RestCall call) throws Exception {
			RestRequest req = call.getRestRequest();
			HttpPartParserSession ps = partParser == null ? req.getPartParser() : partParser.createPartSession(req.getParserSessionArgs());
			RequestHeaders rh = call.getRestRequest().getHeaders();
			return multi ? rh.getAll(ps, schema, name, type) : rh.get(ps, schema, name, type);
		}
	}

	static final class AttributeObject extends RestMethodParam {

		protected AttributeObject(ParamInfo mpi, PropertyStore ps) {
			super(OTHER, mpi, getName(mpi));
		}

		private static String getName(ParamInfo mpi) {
			String n = null;
			for (Attr h : mpi.getAnnotations(Attr.class))
				n = firstNonEmpty(h.name(), h.value(), n);
			if (n == null)
				throw new InternalServerError("@Attr used without name or value on method parameter ''{0}''.", mpi);
			return n;
		}

		@Override /* RestMethodParam */
		public Object resolve(RestCall call) throws Exception {
			return call.getRestRequest().getAttributes().get(name, type);
		}
	}

	static final class RequestObject extends RestMethodParam {
		private final RequestBeanMeta meta;

		protected RequestObject(ParamInfo mpi, PropertyStore ps) {
			super(RESPONSE_BODY, mpi);
			this.meta = RequestBeanMeta.create(mpi, ps);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestCall call) throws Exception {
			return call.getRestRequest().getRequest(meta);
		}
	}

	static final class ResponseHeaderObject extends RestMethodParam {
		final ResponsePartMeta meta;

		protected ResponseHeaderObject(ParamInfo mpi, PropertyStore ps) {
			super(RESPONSE_HEADER, mpi, getName(mpi));
			HttpPartSchema schema = HttpPartSchema.create(ResponseHeader.class, mpi);
			this.meta = new ResponsePartMeta(HttpPartType.HEADER, schema, createPartSerializer(schema.getSerializer(), ps));

			if (getTypeClass() != Value.class)
				throw new InternalServerError("Invalid type {0} specified with @ResponseHeader annotation.  It must be Value.", type);
		}

		private static String getName(ParamInfo mpi) {
			String n = null;
			for (ResponseHeader h : mpi.getAnnotations(ResponseHeader.class))
				n = firstNonEmpty(h.name(), h.n(), h.value(), n);
			if (n == null)
				throw new InternalServerError("@ResponseHeader used without name or value on method parameter ''{0}''.", mpi);
			return n;
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override /* RestMethodParam */
		public Object resolve(final RestCall call) throws Exception {
			Value<Object> v = (Value<Object>)getTypeClass().newInstance();
			v.listener(new ValueListener() {
				@Override
				public void onSet(Object o) {
					try {
						RestRequest req = call.getRestRequest();
						RestResponse res = call.getRestResponse();
						ResponsePartMeta rpm = req.getResponseHeaderMeta(o);
						if (rpm == null)
							rpm = ResponseHeaderObject.this.meta;
						HttpPartSerializerSession pss = rpm.getSerializer() == null ? req.getPartSerializer() : rpm.getSerializer().createPartSession(req.getSerializerSessionArgs());
						res.setHeader(new HttpPart(name, HttpPartType.HEADER, rpm.getSchema(), pss, o));
					} catch (SerializeException | SchemaValidationException e) {
						throw new RuntimeException(e);
					}
				}
			});
			return v;
		}
	}

	static final class ResponseObject extends RestMethodParam {
		final ResponseBeanMeta meta;

		protected ResponseObject(ParamInfo mpi, PropertyStore ps) {
			super(RESPONSE, mpi);
			this.meta = ResponseBeanMeta.create(mpi, ps);
			if (getTypeClass() != Value.class)
				throw new InternalServerError("Invalid type {0} specified with @Response annotation.  It must be Value.", type);
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override /* RestMethodParam */
		public Object resolve(final RestCall call) throws Exception {
			Value<Object> v = (Value<Object>)c.newInstance();
			v.listener(new ValueListener() {
				@Override
				public void onSet(Object o) {
					RestRequest req = call.getRestRequest();
					RestResponse res = call.getRestResponse();
					ResponseBeanMeta meta = req.getResponseBeanMeta(o);
					if (meta == null)
						meta = ResponseObject.this.meta;
					res.setResponseMeta(meta);
					res.setOutput(o);
				}
			});
			return v;
		}
	}

	static class ResponseStatusObject extends RestMethodParam {

		protected ResponseStatusObject(ClassInfo t) {
			super(RESPONSE_STATUS, t);
			if (getTypeClass() != Value.class || Value.getParameterType(t.innerType()) != Integer.class)
				throw new InternalServerError("Invalid type {0} specified with @ResponseStatus annotation.  It must Value<Integer>.", type);
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override /* RestMethodParam */
		public Object resolve(final RestCall call) throws Exception {
			Value<Object> v = (Value<Object>)c.newInstance();
			v.listener(new ValueListener() {
				@Override
				public void onSet(Object o) {
					call.getRestResponse().setStatus(Integer.parseInt(o.toString()));
				}
			});
			return v;
		}
	}

	static final class MethodObject extends RestMethodParam {

		protected MethodObject(MethodInfo m, ClassInfo t, ParamInfo mpi) {
			super(OTHER, mpi);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestCall call) throws Exception {
			return call.getRestRequest().getMethod();
		}
	}

	static final class BeanFactoryObject extends RestMethodParam {

		private final ClassInfo type;

		protected BeanFactoryObject(MethodInfo m, ClassInfo t, ParamInfo mpi) {
			super(OTHER, mpi);
			this.type = t;
		}

		@Override /* RestMethodParam */
		public Object resolve(RestCall call) throws Exception {
			return call.getBeanFactory().getBean(type.inner()).orElseThrow(()->new ServletException("Could not resolve bean type: " + type.inner().getName()));
		}
	}

	static final class FormDataObject extends RestMethodParam {
		private final boolean multi;
		private final HttpPartParser partParser;
		private final HttpPartSchema schema;

		protected FormDataObject(ParamInfo mpi, PropertyStore ps) {
			super(FORM_DATA, mpi, getName(mpi));
			this.schema = HttpPartSchema.create(FormData.class, mpi);
			this.partParser = createPartParser(schema.getParser(), ps);
			this.multi = getMulti(mpi) || schema.getCollectionFormat() == HttpPartCollectionFormat.MULTI;

			if (multi && ! isCollection(type))
				throw new InternalServerError("Use of multipart flag on @FormData parameter that's not an array or Collection on method ''{0}''", mpi.getMethod());
		}

		private static String getName(ParamInfo mpi) {
			String n = null;
			for (FormData h : mpi.getAnnotations(FormData.class))
				n = firstNonEmpty(h.name(), h.n(), h.value(), n);
			if (n == null)
				throw new InternalServerError("@FormData used without name or value on method parameter ''{0}''.", mpi);
			return n;
		}

		private static boolean getMulti(ParamInfo mpi) {
			for (FormData f : mpi.getAnnotations(FormData.class))
				if (f.multi())
					return true;
			return false;
		}

		@Override /* RestMethodParam */
		public Object resolve(RestCall call) throws Exception {
			RestRequest req = call.getRestRequest();
			HttpPartParserSession ps = partParser == null ? req.getPartParser() : partParser.createPartSession(req.getParserSessionArgs());
			RequestFormData fd = req.getFormData();
			return multi ? fd.getAll(ps, schema, name, type) : fd.get(ps, schema, name, type);
		}
	}

	static final class QueryObject extends RestMethodParam {
		private final boolean multi;
		private final HttpPartParser partParser;
		private final HttpPartSchema schema;

		protected QueryObject(ParamInfo mpi, PropertyStore ps) {
			super(QUERY, mpi, getName(mpi));
			this.schema = HttpPartSchema.create(Query.class, mpi);
			this.partParser = createPartParser(schema.getParser(), ps);
			this.multi = getMulti(mpi) || schema.getCollectionFormat() == HttpPartCollectionFormat.MULTI;

			if (multi && ! isCollection(type))
				throw new InternalServerError("Use of multipart flag on @Query parameter that's not an array or Collection on method ''{0}''", mpi.getMethod());
		}

		private static String getName(ParamInfo mpi) {
			String n = null;
			for (Query h : mpi.getAnnotations(Query.class))
				n = firstNonEmpty(h.name(), h.n(), h.value(), n);
			if (n == null)
				throw new InternalServerError("@Query used without name or value on method param ''{0}''.", mpi);
			return n;
		}

		private static boolean getMulti(ParamInfo mpi) {
			for (Query q : mpi.getAnnotations(Query.class))
				if (q.multi())
					return true;
			return false;
		}

		@Override /* RestMethodParam */
		public Object resolve(RestCall call) throws Exception {
			RestRequest req = call.getRestRequest();
			HttpPartParserSession ps = partParser == null ? req.getPartParser() : partParser.createPartSession(req.getParserSessionArgs());
			RequestQuery rq = req.getQuery();
			return multi ? rq.getAll(ps, schema, name, type) : rq.get(ps, schema, name, type);
		}
	}

	static final class HasFormDataObject extends RestMethodParam {

		protected HasFormDataObject(ParamInfo mpi) {
			super(FORM_DATA, mpi, getName(mpi));
		}

		private static String getName(ParamInfo mpi) {
			String n = null;
			for (HasFormData h : mpi.getAnnotations(HasFormData.class))
				n = firstNonEmpty(h.name(), h.n(), h.value(), n);
			if (n == null)
				throw new InternalServerError("@HasFormData used without name or value on method parameter ''{o}''.", mpi);
			return n;
		}

		@Override /* RestMethodParam */
		public Object resolve(RestCall call) throws Exception {
			RestRequest req = call.getRestRequest();
			BeanSession bs = req.getBeanSession();
			return bs.convertToType(req.getFormData().containsKey(name), bs.getClassMeta(type));
		}
	}

	static final class HasQueryObject extends RestMethodParam {

		protected HasQueryObject(ParamInfo mpi) {
			super(QUERY, mpi, getName(mpi));
		}

		private static String getName(ParamInfo mpi) {
			String n = null;
			for (HasQuery h : mpi.getAnnotations(HasQuery.class))
				n = firstNonEmpty(h.name(), h.n(), h.value(), n);
			if (n == null)
				throw new InternalServerError("@HasQuery used without name or value on method parameter ''{0}''.", mpi);
			return n;
		}

		@Override /* RestMethodParam */
		public Object resolve(RestCall call) throws Exception {
			RestRequest req = call.getRestRequest();
			BeanSession bs = req.getBeanSession();
			return bs.convertToType(req.getQuery().containsKey(name), bs.getClassMeta(type));
		}
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Other retrievers
	//-------------------------------------------------------------------------------------------------------------------

	static final class ResourceBundleObject extends RestMethodParam {

		protected ResourceBundleObject() {
			super(OTHER, ResourceBundle.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestCall call) throws Exception {
			return call.getRestRequest().getMessages();
		}
	}

	static final class MessageBundleObject extends RestMethodParam {

		protected MessageBundleObject() {
			super(OTHER, Messages.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestCall call) throws Exception {
			return call.getRestRequest().getMessages();
		}
	}

	static final class InputStreamObject extends RestMethodParam {

		protected InputStreamObject() {
			super(OTHER, InputStream.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestCall call) throws Exception {
			return call.getRestRequest().getInputStream();
		}
	}

	static final class ServletInputStreamObject extends RestMethodParam {

		protected ServletInputStreamObject() {
			super(OTHER, ServletInputStream.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestCall call) throws Exception {
			return call.getRestRequest().getInputStream();
		}
	}

	static final class ReaderObject extends RestMethodParam {

		protected ReaderObject() {
			super(OTHER, Reader.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestCall call) throws Exception {
			return call.getRestRequest().getReader();
		}
	}

	static final class OutputStreamObject extends RestMethodParam {

		protected OutputStreamObject() {
			super(OTHER, OutputStream.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestCall call) throws Exception {
			return call.getRestResponse().getOutputStream();
		}
	}

	static final class ServletOutputStreamObject extends RestMethodParam {

		protected ServletOutputStreamObject() {
			super(OTHER, ServletOutputStream.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestCall call) throws Exception {
			return call.getRestResponse().getOutputStream();
		}
	}

	static final class WriterObject extends RestMethodParam {

		protected WriterObject() {
			super(OTHER, Writer.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestCall call) throws Exception {
			return call.getRestResponse().getWriter();
		}
	}

	static final class RequestHeadersObject extends RestMethodParam {

		protected RequestHeadersObject() {
			super(OTHER, RequestHeaders.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestCall call) throws Exception {
			return call.getRestRequest().getHeaders();
		}
	}

	static final class RequestAttributesObject extends RestMethodParam {

		protected RequestAttributesObject() {
			super(OTHER, RequestAttributes.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestCall call) throws Exception {
			return call.getRestRequest().getAttributes();
		}
	}

	static final class RequestQueryObject extends RestMethodParam {

		protected RequestQueryObject() {
			super(OTHER, RequestQuery.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestCall call) throws Exception {
			return call.getRestRequest().getQuery();
		}
	}

	static final class RequestFormDataObject extends RestMethodParam {

		protected RequestFormDataObject() {
			super(OTHER, RequestFormData.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestCall call) throws Exception {
			return call.getRestRequest().getFormData();
		}
	}

	static final class RestContextObject extends RestMethodParam {

		protected RestContextObject() {
			super(OTHER, RestContext.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestCall call) throws Exception {
			return call.getRestRequest().getContext();
		}
	}

	static final class ParserObject extends RestMethodParam {

		protected ParserObject() {
			super(OTHER, Parser.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestCall call) throws Exception {
			return call.getRestRequest().getBody().getParser();
		}
	}

	static final class ReaderParserObject extends RestMethodParam {

		protected ReaderParserObject() {
			super(OTHER, ReaderParser.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestCall call) throws Exception {
			return call.getRestRequest().getBody().getReaderParser();
		}
	}

	static final class InputStreamParserObject extends RestMethodParam {

		protected InputStreamParserObject() {
			super(OTHER, InputStreamParser.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestCall call) throws Exception {
			return call.getRestRequest().getBody().getInputStreamParser();
		}
	}

	static final class LocaleObject extends RestMethodParam {

		protected LocaleObject() {
			super(OTHER, Locale.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestCall call) throws Exception {
			return call.getRestRequest().getLocale();
		}
	}

	static final class SwaggerObject extends RestMethodParam {

		protected SwaggerObject() {
			super(OTHER, Swagger.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestCall call) throws Exception {
			return call.getRestRequest().getSwagger();
		}
	}

	static final class RequestPathMatchObject extends RestMethodParam {

		protected RequestPathMatchObject() {
			super(OTHER, RequestPath.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestCall call) throws Exception {
			return call.getRestRequest().getPathMatch();
		}
	}

	static final class RequestBodyObject extends RestMethodParam {

		protected RequestBodyObject() {
			super(OTHER, RequestBody.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestCall call) throws Exception {
			return call.getRestRequest().getBody();
		}
	}

	static final class ConfigObject extends RestMethodParam {

		protected ConfigObject() {
			super(OTHER, Config.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestCall call) throws Exception {
			return call.getRestRequest().getConfig();
		}
	}

	static final class UriContextObject extends RestMethodParam {

		protected UriContextObject() {
			super(OTHER, UriContext.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestCall call) throws Exception {
			return call.getRestRequest().getUriContext();
		}
	}

	static final class UriResolverObject extends RestMethodParam {

		protected UriResolverObject() {
			super(OTHER, UriResolver.class);
		}

		@Override /* RestMethodParam */
		public Object resolve(RestCall call) throws Exception {
			return call.getRestRequest().getUriResolver();
		}
	}

	//=================================================================================================================
	// Utility methods
	//=================================================================================================================

	static final boolean isCollection(Type t) {
		return BeanContext.DEFAULT.getClassMeta(t).isCollectionOrArray();
	}

	static final HttpPartParser createPartParser(Class<? extends HttpPartParser> p, PropertyStore ps) {
		return castOrCreate(HttpPartParser.class, p, true, ps);
	}

	static final HttpPartSerializer createPartSerializer(Class<? extends HttpPartSerializer> s, PropertyStore ps) {
		return castOrCreate(HttpPartSerializer.class, s, true, ps);
	}
}
