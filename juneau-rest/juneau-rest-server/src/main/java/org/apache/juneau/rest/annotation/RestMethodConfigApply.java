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

import static org.apache.juneau.BeanContext.*;
import static org.apache.juneau.internal.ArrayUtils.*;
import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.rest.RestContext.*;
import static org.apache.juneau.rest.RestMethodContext.*;
import static org.apache.juneau.rest.util.RestUtils.*;
import static org.apache.juneau.html.HtmlDocSerializer.*;
import java.util.*;
import java.util.logging.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.AnnotationUtils;
import org.apache.juneau.rest.util.*;
import org.apache.juneau.rest.widget.*;
import org.apache.juneau.svl.*;

/**
 * Applies {@link RestMethod} annotations to a {@link PropertyStoreBuilder}.
 */
public class RestMethodConfigApply extends ConfigApply<RestMethod> {

	/**
	 * Constructor.
	 *
	 * @param c The annotation class.
	 * @param r The resolver for resolving values in annotations.
	 */
	public RestMethodConfigApply(Class<RestMethod> c, VarResolverSession r) {
		super(c, r);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void apply(AnnotationInfo<RestMethod> ai, PropertyStoreBuilder psb) {
		RestMethod a = ai.getAnnotation();
		MethodInfo mi = ai.getMethodOn();
		String sig = mi == null ? "Unknown" : mi.getSignature();
		String s = null;

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

		if (a.encoders().length > 0)
			psb.set(REST_encoders, merge(ObjectUtils.toType(psb.peek(REST_encoders), Object[].class), a.encoders()));

		if (a.produces().length > 0)
			psb.set(REST_produces, strings(a.produces()));

		if (a.consumes().length > 0)
			psb.set(REST_consumes, strings(a.consumes()));

		for (String header : strings(a.defaultRequestHeaders())) {
			String[] h = RestUtils.parseHeader(header);
			if (h == null)
				throw new ConfigException("Invalid default request header specified on method ''{0}'': ''{1}''.  Must be in the format: ''Header-Name: header-value''", sig, header);
			if (isNotEmpty(h[1]))
				psb.addTo(REST_defaultRequestHeaders, h[0], h[1]);
		}

		for (String header : strings(a.reqHeaders())) {
			String[] h = RestUtils.parseHeader(header);
			if (h == null)
				throw new ConfigException("Invalid default request header specified on method ''{0}'': ''{1}''.  Must be in the format: ''Header-Name: header-value''", sig, header);
			if (isNotEmpty(h[1]))
				psb.addTo(REST_reqHeaders, h[0], h[1]);
		}

		if (a.defaultAccept().length() > 0) {
			s = string(a.defaultAccept());
			if (isNotEmpty(s))
				psb.addTo(REST_reqHeaders, "Accept", s);
		}

		if (a.defaultContentType().length() > 0) {
			s = string(a.defaultContentType());
			if (isNotEmpty(s))
				psb.addTo(REST_reqHeaders, "Content-Type", s);
		}

		psb.addTo(REST_converters, a.converters());

		psb.addTo(REST_guards, reverse(a.guards()));

		psb.addTo(RESTMETHOD_matchers, a.matchers());

		if (! a.clientVersion().isEmpty())
			psb.set(RESTMETHOD_clientVersion, a.clientVersion());

		psb.set(BEAN_beanFilters, merge(ObjectUtils.toType(psb.peek(BEAN_beanFilters), Object[].class), a.beanFilters()));

		psb.set(BEAN_pojoSwaps, merge(ObjectUtils.toType(psb.peek(BEAN_pojoSwaps), Object[].class), a.pojoSwaps()));

		if (a.bpi().length > 0) {
			Map<String,String> bpiMap = new LinkedHashMap<>();
			for (String s1 : a.bpi()) {
				for (String s2 : split(s1, ';')) {
					int i = s2.indexOf(':');
					if (i == -1)
						throw new ConfigException(
							"Invalid format for @RestMethod(bpi) on method ''{0}''.  Must be in the format \"ClassName: comma-delimited-tokens\".  \nValue: {1}", sig, s1);
					bpiMap.put(s2.substring(0, i).trim(), s2.substring(i+1).trim());
				}
			}
			psb.addTo(BEAN_bpi, bpiMap);
		}

		if (a.bpx().length > 0) {
			Map<String,String> bpxMap = new LinkedHashMap<>();
			for (String s1 : a.bpx()) {
				for (String s2 : split(s1, ';')) {
					int i = s2.indexOf(':');
					if (i == -1)
						throw new ConfigException(
							"Invalid format for @RestMethod(bpx) on method ''{0}''.  Must be in the format \"ClassName: comma-delimited-tokens\".  \nValue: {1}", sig, s1);
					bpxMap.put(s2.substring(0, i).trim(), s2.substring(i+1).trim());
				}
			}
			psb.addTo(BEAN_bpx, bpxMap);
		}

		if (! a.defaultCharset().isEmpty())
			psb.set(REST_defaultCharset, string(a.defaultCharset()));

		if (! a.maxInput().isEmpty())
			psb.set(REST_maxInput, string(a.maxInput()));

		if (! a.maxInput().isEmpty())
			psb.set(REST_maxInput, string(a.maxInput()));

		if (! a.path().isEmpty())
			psb.set(RESTMETHOD_path, string(a.path()));

		if (! a.rolesDeclared().isEmpty())
			psb.addTo(REST_rolesDeclared, strings(a.rolesDeclared()));

		if (! a.roleGuard().isEmpty())
			psb.addTo(REST_roleGuard, string(a.roleGuard()));

		for (String h : a.defaultRequestHeaders()) {
			String[] h2 = RestUtils.parseKeyValuePair(string(h));
			if (h2 == null)
				throw new ConfigException(
					"Invalid default request header specified on method ''{0}'': ''{1}''.  Must be in the format: ''name[:=]value''", sig, s);
			psb.addTo(RESTMETHOD_defaultRequestHeaders, h2[0], h2[1]);
		}

		for (String h : a.reqHeaders()) {
			String[] h2 = RestUtils.parseKeyValuePair(string(h));
			if (h2 == null)
				throw new ConfigException(
					"Invalid default request header specified on method ''{0}'': ''{1}''.  Must be in the format: ''name[:=]value''", sig, s);
			psb.addTo(RESTMETHOD_reqHeaders, h2[0], h2[1]);
		}

		for (String ra : a.attrs()) {
			String[] ra2 = RestUtils.parseKeyValuePair(string(ra));
			if (ra2 == null)
				throw new ConfigException(
					"Invalid default request attribute specified on method ''{0}'': ''{1}''.  Must be in the format: ''name[:=]value''", sig, s);
			psb.addTo(RESTMETHOD_attrs, ra2[0], ra2[1]);
		}

		for (String ra : a.reqAttrs()) {
			String[] ra2 = RestUtils.parseKeyValuePair(string(ra));
			if (ra2 == null)
				throw new ConfigException(
					"Invalid default request attribute specified on method ''{0}'': ''{1}''.  Must be in the format: ''name[:=]value''", sig, s);
			psb.addTo(RESTMETHOD_reqAttrs, ra2[0], ra2[1]);
		}

		if (! a.defaultAccept().isEmpty())
			psb.addTo(RESTMETHOD_reqHeaders, "Accept", string(a.defaultAccept()));

		if (! a.defaultContentType().isEmpty())
			psb.addTo(RESTMETHOD_reqHeaders, string(a.defaultContentType()));

		for (String h : a.defaultQuery()) {
			String[] h2 = RestUtils.parseKeyValuePair(string(h));
			if (h == null)
				throw new ConfigException(
					"Invalid default query parameter specified on method ''{0}'': ''{1}''.  Must be in the format: ''name[:=]value''", sig, s);
			psb.addTo(RESTMETHOD_defaultQuery, h2[0], h2[1]);
		}

		for (String h : a.defaultFormData()) {
			String[] h2 = RestUtils.parseKeyValuePair(string(h));
			if (h == null)
				throw new ConfigException(
					"Invalid default form data parameter specified on method ''{0}'': ''{1}''.  Must be in the format: ''name[:=]value''", sig, s);
			psb.addTo(RESTMETHOD_defaultFormData, h2[0], h2[1]);
		}

		if (! a.method().isEmpty())
			psb.set(RESTMETHOD_httpMethod, string(a.method()));

		if (! a.name().isEmpty())
			psb.set(RESTMETHOD_httpMethod, string(a.name()));

		if (a.priority() != 0)
			psb.set(RESTMETHOD_priority, a.priority());

		if (! a.debug().isEmpty())
			psb.set(RESTMETHOD_debug, string(a.debug()));

		if (! AnnotationUtils.empty(a.logging())) {
			Logging al = a.logging();
			ObjectMap m = new ObjectMap(psb.peek(ObjectMap.class, RESTMETHOD_callLoggerConfig));

			if (! al.useStackTraceHashing().isEmpty())
				m.append("useStackTraceHashing", bool(al.useStackTraceHashing()));

			if (! al.stackTraceHashingTimeout().isEmpty())
				m.append("stackTraceHashingTimeout", integer(al.stackTraceHashingTimeout(), "@Logging(stackTraceHashingTimeout)"));

			if (! al.disabled().isEmpty())
				m.append("disabled", enablement(al.disabled()));

			if (! al.level().isEmpty())
				m.append("level", level(al.level(), "@Logging(level)"));

			if (al.rules().length > 0) {
				ObjectList ol = new ObjectList();
				for (LoggingRule a2 : al.rules()) {
					ObjectMap m2 = new ObjectMap();

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
				m.put("rules", ol.appendAll(m.getObjectList("rules")));
			}

			psb.set(RESTMETHOD_callLoggerConfig, m);
		}

		HtmlDoc hd = a.htmldoc();
		new HtmlDocBuilder(psb).process(hd);
		for (Class<? extends Widget> wc : hd.widgets()) {
			Widget w = castOrCreate(Widget.class, wc);
			psb.addTo(REST_widgets, w);
			psb.addTo(HTMLDOC_script, "$W{"+w.getName()+".script}");
			psb.addTo(HTMLDOC_script, "$W{"+w.getName()+".style}");
		}
	}

	private Enablement enablement(String in) {
		return Enablement.fromString(string(in));
	}

	private Level level(String in, String loc) {
		try {
			return Level.parse(string(in));
		} catch (Exception e) {
			throw new ConfigException("Invalid syntax for level on annotation @RestMethod({1}): {2}", loc, in);
		}
	}
}
