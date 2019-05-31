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
package org.apache.juneau.rest.annotation;

import static org.apache.juneau.rest.RestContext.*;
import static org.apache.juneau.rest.util.RestUtils.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.html.HtmlDocSerializer.*;
import static org.apache.juneau.internal.ArrayUtils.*;
import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.parser.Parser.*;

import org.apache.juneau.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.util.*;
import org.apache.juneau.rest.widget.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.utils.*;

/**
 * Applies {@link RestResource} annotations to a {@link PropertyStoreBuilder}.
 */
public class RestResourceConfigApply extends ConfigApply<RestResource> {

	/**
	 * Constructor.
	 *
	 * @param c The annotation class.
	 * @param r The resolver for resolving values in annotations.
	 */
	public RestResourceConfigApply(Class<RestResource> c, VarResolverSession r) {
		super(c, r);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void apply(AnnotationInfo<RestResource> ai, PropertyStoreBuilder psb) {
		RestResource a = ai.getAnnotation();
		String s = null;
		ClassInfo c = ai.getClassOn();

		for (Property p1 : a.properties()) {
			psb.set(p1.name(), string(p1.value()));  // >>> DEPRECATED - Remove in 9.0 <<<
			psb.addTo(REST_properties, string(p1.name()), string(p1.value()));
		}

		for (String p1 : a.flags()) {
			psb.set(p1, true);  // >>> DEPRECATED - Remove in 9.0 <<<
			psb.addTo(REST_properties, string(p1), true);
		}

		if (a.serializers().length > 0)
			psb.set(REST_serializers, merge(ObjectUtils.toType(psb.peek(REST_serializers), Object[].class), a.serializers()));

		if (a.parsers().length > 0)
			psb.set(REST_parsers, merge(ObjectUtils.toType(psb.peek(REST_parsers), Object[].class), a.parsers()));

		if (a.partSerializer() != HttpPartSerializer.Null.class)
			psb.set(REST_partSerializer, a.partSerializer());

		if (a.partParser() != HttpPartParser.Null.class)
			psb.set(REST_partParser, a.partParser());

		psb.addTo(REST_encoders, a.encoders());

		if (a.produces().length > 0)
			psb.set(REST_produces, strings(a.produces()));

		if (a.consumes().length > 0)
			psb.set(REST_consumes, strings(a.consumes()));

		for (String header : strings(a.defaultRequestHeaders())) {
			String[] h = RestUtils.parseHeader(header);
			if (h == null)
				throw new FormattedRuntimeException("Invalid default request header specified: ''{0}''.  Must be in the format: ''Header-Name: header-value''", header);
			if (isNotEmpty(h[1]))
				psb.addTo(REST_defaultRequestHeaders, h[0], h[1]);
		}

		if (a.defaultAccept().length() > 0) {
			s = string(a.defaultAccept());
			if (isNotEmpty(s))
				psb.addTo(REST_defaultRequestHeaders, "Accept", s);
		}

		if (a.defaultContentType().length() > 0) {
			s = string(a.defaultContentType());
			if (isNotEmpty(s))
				psb.addTo(REST_defaultRequestHeaders, "Content-Type", s);

		}

		for (String header : strings(a.defaultResponseHeaders())) {
			String[] h = parseHeader(header);
			if (h == null)
				throw new FormattedRuntimeException("Invalid default response header specified: ''{0}''.  Must be in the format: ''Header-Name: header-value''", header);
			if (isNotEmpty(h[1]))
				psb.addTo(REST_defaultResponseHeaders, h[0], h[1]);
		}

		psb.addTo(REST_responseHandlers, a.responseHandlers());

		psb.addTo(REST_converters, a.converters());

		psb.addTo(REST_guards, reverse(a.guards()));

		psb.addTo(REST_children, a.children());

		psb.set(BEAN_beanFilters, merge(ObjectUtils.toType(psb.peek(BEAN_beanFilters), Object[].class), a.beanFilters()));

		psb.set(BEAN_pojoSwaps, merge(ObjectUtils.toType(psb.peek(BEAN_pojoSwaps), Object[].class), a.pojoSwaps()));

		psb.addTo(REST_paramResolvers, a.paramResolvers());

		if (a.serializerListener() != SerializerListener.Null.class)
			psb.set(SERIALIZER_listener, a.serializerListener());

		if (a.parserListener() != ParserListener.Null.class)
			psb.set(PARSER_listener, a.parserListener());

		s = string(a.uriContext());
		if (isNotEmpty(s))
			psb.set(REST_uriContext, s);

		s = string(a.uriAuthority());
		if (isNotEmpty(s))
			psb.set(REST_uriAuthority, s);

		s = string(a.uriRelativity());
		if (isNotEmpty(s))
			psb.set(REST_uriRelativity, s);

		s = string(a.uriResolution());
		if (isNotEmpty(s))
			psb.set(REST_uriResolution, s);

		for (String mapping : a.staticFiles())
			if (isNotEmpty(mapping))
				psb.addTo(REST_staticFiles, new StaticFileMapping(c.inner(), string(mapping)));

		if (! a.messages().isEmpty())
			psb.addTo(REST_messages, new MessageBundleLocation(c.inner(), string(a.messages())));

		for (String header : strings(a.staticFileResponseHeaders())) {
			String[] h = RestUtils.parseHeader(header);
			if (h == null)
				throw new FormattedRuntimeException("Invalid static file response header specified: ''{0}''.  Must be in the format: ''Header-Name: header-value''", header);
			if (isNotEmpty(h[1]))
				psb.addTo(REST_staticFileResponseHeaders, h[0], h[1]);
		}

		if (! a.useClasspathResourceCaching().isEmpty())
			psb.set(REST_useClasspathResourceCaching, bool(a.useClasspathResourceCaching()));

		if (a.classpathResourceFinder() != ClasspathResourceFinder.Null.class)
			psb.set(REST_classpathResourceFinder, a.classpathResourceFinder());

		if (! a.path().isEmpty())
			psb.set(REST_path, trimLeadingSlash(string(a.path())));

		if (! a.clientVersionHeader().isEmpty())
			psb.set(REST_clientVersionHeader, string(a.clientVersionHeader()));

		if (a.resourceResolver() != RestResourceResolver.Null.class)
			psb.set(REST_resourceResolver, a.resourceResolver());

		if (a.logger() != RestLogger.Null.class)
			psb.set(REST_logger, a.logger());

		if (a.callHandler() != RestCallHandler.Null.class)
			psb.set(REST_callHandler, a.callHandler());

		if (a.infoProvider() != RestInfoProvider.Null.class)
			psb.set(REST_infoProvider, a.infoProvider());

		if (! a.allowHeaderParams().isEmpty())
			psb.set(REST_allowHeaderParams, bool(a.allowHeaderParams()));

		if (! a.allowedMethodParams().isEmpty())
			psb.set(REST_allowedMethodParams, string(a.allowedMethodParams()));

		if (! a.allowBodyParam().isEmpty())
			psb.set(REST_allowBodyParam, bool(a.allowBodyParam()));

		if (! a.renderResponseStackTraces().isEmpty())
			psb.set(REST_renderResponseStackTraces, bool(a.renderResponseStackTraces()));

		if (! a.useStackTraceHashes().isEmpty())
			psb.set(REST_useStackTraceHashes, bool(a.useStackTraceHashes()));

		if (! a.defaultCharset().isEmpty())
			psb.set(REST_defaultCharset, string(a.defaultCharset()));

		if (! a.maxInput().isEmpty())
			psb.set(REST_maxInput, string(a.maxInput()));

		if (! a.debug().isEmpty()) {
			psb.set(REST_debug, bool(a.debug()));
			psb.set(BEAN_debug, bool(a.debug()));
		}

		psb.addTo(REST_mimeTypes, strings(a.mimeTypes()));

		if (! a.rolesDeclared().isEmpty())
			psb.set(REST_rolesDeclared, string(a.rolesDeclared()));

		if (! a.roleGuard().isEmpty())
			psb.set(REST_roleGuard, string(a.roleGuard()));

		HtmlDoc hd = a.htmldoc();
		new HtmlDocBuilder(psb).process(hd);
		for (Class<? extends Widget> wc : hd.widgets()) {
			Widget w = castOrCreate(Widget.class, wc);
			psb.addTo(REST_widgets, w);
			psb.addTo(HTMLDOC_script, "$W{"+w.getName()+".script}");
			psb.addTo(HTMLDOC_script, "$W{"+w.getName()+".style}");
		}
	}

	private String trimLeadingSlash(String value) {
		if (startsWith(value, '/'))
			return value.substring(1);
		return value;
	}
}
