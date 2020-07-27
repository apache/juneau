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

import java.util.logging.*;

import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.internal.ArrayUtils.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.AnnotationUtils;
import org.apache.juneau.rest.annotation.Logging;
import org.apache.juneau.rest.util.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.utils.*;

/**
 * Applies {@link Rest} annotations to a {@link PropertyStoreBuilder}.
 */
public class RestConfigApply extends ConfigApply<Rest> {

	/**
	 * Constructor.
	 *
	 * @param c The annotation class.
	 * @param r The resolver for resolving values in annotations.
	 */
	public RestConfigApply(Class<Rest> c, VarResolverSession r) {
		super(c, r);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void apply(AnnotationInfo<Rest> ai, PropertyStoreBuilder psb) {
		Rest a = ai.getAnnotation();
		String s = null;
		ClassInfo c = ai.getClassOn();

		for (Property p1 : a.properties()) {
			psb.set(p1.name(), string(p1.value()));  // >>> DEPRECATED - Remove in 9.0 <<<
			psb.putTo(REST_properties, string(p1.name()), string(p1.value()));
		}

		for (String p1 : a.flags()) {
			psb.set(p1, true);  // >>> DEPRECATED - Remove in 9.0 <<<
			psb.putTo(REST_properties, string(p1), true);
		}

		if (a.serializers().length > 0)
			psb.set(REST_serializers, merge(ConverterUtils.toType(psb.peek(REST_serializers), Object[].class), a.serializers()));

		if (a.parsers().length > 0)
			psb.set(REST_parsers, merge(ConverterUtils.toType(psb.peek(REST_parsers), Object[].class), a.parsers()));

		if (a.partSerializer() != HttpPartSerializer.Null.class)
			psb.set(REST_partSerializer, a.partSerializer());

		if (a.partParser() != HttpPartParser.Null.class)
			psb.set(REST_partParser, a.partParser());

		psb.prependTo(REST_encoders, a.encoders());

		if (a.produces().length > 0)
			psb.set(REST_produces, strings(a.produces()));

		if (a.consumes().length > 0)
			psb.set(REST_consumes, strings(a.consumes()));

		for (String ra : strings(a.attrs())) {
			String[] ra2 = RestUtils.parseKeyValuePair(ra);
			if (ra2 == null)
				throw new BasicRuntimeException("Invalid default request attribute specified: ''{0}''.  Must be in the format: ''Name: value''", ra);
			if (isNotEmpty(ra2[1]))
				psb.putTo(REST_attrs, ra2[0], ra2[1]);
		}

		for (String ra : strings(a.reqAttrs())) {
			String[] ra2 = RestUtils.parseKeyValuePair(ra);
			if (ra2 == null)
				throw new BasicRuntimeException("Invalid default request attribute specified: ''{0}''.  Must be in the format: ''Name: value''", ra);
			if (isNotEmpty(ra2[1]))
				psb.putTo(REST_reqAttrs, ra2[0], ra2[1]);
		}

		for (String header : strings(a.defaultRequestHeaders())) {
			String[] h = RestUtils.parseHeader(header);
			if (h == null)
				throw new BasicRuntimeException("Invalid default request header specified: ''{0}''.  Must be in the format: ''Header-Name: header-value''", header);
			if (isNotEmpty(h[1]))
				psb.putTo(REST_defaultRequestHeaders, h[0], h[1]);
		}

		for (String header : strings(a.reqHeaders())) {
			String[] h = RestUtils.parseHeader(header);
			if (h == null)
				throw new BasicRuntimeException("Invalid default request header specified: ''{0}''.  Must be in the format: ''Header-Name: header-value''", header);
			if (isNotEmpty(h[1]))
				psb.putTo(REST_reqHeaders, h[0], h[1]);
		}

		if (a.defaultAccept().length() > 0) {
			s = string(a.defaultAccept());
			if (isNotEmpty(s))
				psb.putTo(REST_reqHeaders, "Accept", s);
		}

		if (a.defaultContentType().length() > 0) {
			s = string(a.defaultContentType());
			if (isNotEmpty(s))
				psb.putTo(REST_reqHeaders, "Content-Type", s);

		}

		for (String header : strings(a.defaultResponseHeaders())) {
			String[] h = parseHeader(header);
			if (h == null)
				throw new BasicRuntimeException("Invalid default response header specified: ''{0}''.  Must be in the format: ''Header-Name: header-value''", header);
			if (isNotEmpty(h[1]))
				psb.putTo(REST_defaultResponseHeaders, h[0], h[1]);
		}

		for (String header : strings(a.resHeaders())) {
			String[] h = parseHeader(header);
			if (h == null)
				throw new BasicRuntimeException("Invalid default response header specified: ''{0}''.  Must be in the format: ''Header-Name: header-value''", header);
			if (isNotEmpty(h[1]))
				psb.putTo(REST_resHeaders, h[0], h[1]);
		}

		psb.prependTo(REST_responseHandlers, a.responseHandlers());

		psb.prependTo(REST_converters, a.converters());

		psb.prependTo(REST_guards, reverse(a.guards()));

		psb.prependTo(REST_children, a.children());

		psb.prependTo(REST_paramResolvers, a.paramResolvers());

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

		for (String mapping : a.staticFiles()) {
			try {
				for (StaticFileMapping sfm : StaticFileMapping.parse(c.inner(), string(mapping)).riterable())
					psb.prependTo(REST_staticFiles, sfm);
			} catch (ParseException e) {
				throw new ConfigException(e, "Invalid @Resource(staticFiles) value on class ''{0}''", c);
			}
		}

		psb.prependTo(REST_messages, Tuple2.of(c.inner(), string(a.messages())));

		for (String header : strings(a.staticFileResponseHeaders())) {
			String[] h = RestUtils.parseHeader(header);
			if (h == null)
				throw new BasicRuntimeException("Invalid static file response header specified: ''{0}''.  Must be in the format: ''Header-Name: header-value''", header);
			if (isNotEmpty(h[1]))
				psb.putTo(REST_staticFileResponseHeaders, h[0], h[1]);
		}

		if (! a.useClasspathResourceCaching().isEmpty())
			psb.set(REST_useClasspathResourceCaching, bool(a.useClasspathResourceCaching()));

		if (a.classpathResourceFinder() != ResourceFinder.Null.class)
			psb.set(REST_classpathResourceFinder, a.classpathResourceFinder());

		if (! a.path().isEmpty())
			psb.set(REST_path, trimLeadingSlash(string(a.path())));

		if (! a.clientVersionHeader().isEmpty())
			psb.set(REST_clientVersionHeader, string(a.clientVersionHeader()));

		if (a.resourceResolver() != RestResourceResolver.Null.class)
			psb.set(REST_resourceResolver, a.resourceResolver());

		if (a.callLogger() != RestCallLogger.Null.class)
			psb.set(REST_callLogger, a.callLogger());

		if (! AnnotationUtils.empty(a.logging())) {
			Logging al = a.logging();
			OMap m = new OMap(psb.peek(OMap.class, REST_callLoggerConfig));

			if (! al.useStackTraceHashing().isEmpty())
				m.append("useStackTraceHashing", bool(al.useStackTraceHashing()));

			if (! al.stackTraceHashingTimeout().isEmpty())
				m.append("stackTraceHashingTimeout", integer(al.stackTraceHashingTimeout(), "@Logging(stackTraceHashingTimeout)"));

			if (! al.disabled().isEmpty())
				m.append("disabled", enablement(al.disabled()));

			if (! al.level().isEmpty())
				m.append("level", level(al.level(), "@Logging(level)"));

			if (al.rules().length > 0) {
				OList ol = new OList();
				for (LoggingRule a2 : al.rules()) {
					OMap m2 = new OMap();

					if (! a2.codes().isEmpty())
						m2.append("codes", string(a2.codes()));

					if (! a2.exceptions().isEmpty())
						m2.append("exceptions", string(a2.exceptions()));

					if (! a2.debugOnly().isEmpty())
						 m2.append("debugOnly", bool(a2.debugOnly()));

					if (! a2.level().isEmpty())
						m2.append("level", level(a2.level(), "@LoggingRule(level)"));

					if (! a2.req().isEmpty())
						m2.append("req", string(a2.req()));

					if (! a2.res().isEmpty())
						m2.append("res", string(a2.res()));

					if (! a2.verbose().isEmpty())
						m2.append("verbose", bool(a2.verbose()));

					if (! a2.disabled().isEmpty())
						m2.append("disabled", bool(a2.disabled()));

					ol.add(m2);
				}
				m.put("rules", ol.appendAll(m.getList("rules")));
			}

			psb.set(REST_callLoggerConfig, m);
		}

		if (a.callHandler() != RestCallHandler.Null.class)
			psb.set(REST_callHandler, a.callHandler());

		if (a.infoProvider() != RestInfoProvider.Null.class)
			psb.set(REST_infoProvider, a.infoProvider());

		if (! a.allowBodyParam().isEmpty())
			psb.set(REST_allowBodyParam, bool(a.allowBodyParam()));

		if (! a.allowedHeaderParams().isEmpty())
			psb.set(REST_allowedHeaderParams, string(a.allowedHeaderParams()));

		if (! a.allowedMethodHeaders().isEmpty())
			psb.set(REST_allowedMethodHeaders, string(a.allowedMethodHeaders()));

		if (! a.allowedMethodParams().isEmpty())
			psb.set(REST_allowedMethodParams, string(a.allowedMethodParams()));

		if (! a.renderResponseStackTraces().isEmpty())
			psb.set(REST_renderResponseStackTraces, bool(a.renderResponseStackTraces()));

		if (! a.defaultCharset().isEmpty())
			psb.set(REST_defaultCharset, string(a.defaultCharset()));

		if (! a.maxInput().isEmpty())
			psb.set(REST_maxInput, string(a.maxInput()));

		if (! a.debug().isEmpty())
			psb.set(REST_debug, string(a.debug()));

		if (! a.debugOn().isEmpty())
			psb.set(REST_debugOn, string(a.debugOn()));

		psb.addTo(REST_mimeTypes, strings(a.mimeTypes()));

		if (! a.rolesDeclared().isEmpty())
			psb.addTo(REST_rolesDeclared, strings(a.rolesDeclared()));

		if (! a.roleGuard().isEmpty())
			psb.addTo(REST_roleGuard, string(a.roleGuard()));
	}

	private String trimLeadingSlash(String value) {
		if (startsWith(value, '/'))
			return value.substring(1);
		return value;
	}

	private Enablement enablement(String in) {
		return Enablement.fromString(string(in));
	}

	private Level level(String in, String loc) {
		try {
			return Level.parse(string(in).toUpperCase());
		} catch (Exception e) {
			throw new ConfigException("Invalid syntax for level on annotation @Rest({0}): {1}", loc, in);
		}
	}
}
