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
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.rest.RestContext.*;
import static org.apache.juneau.rest.util.RestUtils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.util.*;
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

		psb.addTo(REST_converters, a.converters());

		psb.addTo(REST_guards, reverse(a.guards()));

		psb.set(BEAN_beanFilters, merge(ObjectUtils.toType(psb.peek(BEAN_beanFilters), Object[].class), a.beanFilters()));

		psb.set(BEAN_pojoSwaps, merge(ObjectUtils.toType(psb.peek(BEAN_pojoSwaps), Object[].class), a.pojoSwaps()));

		if (a.bpi().length > 0) {
			Map<String,String> bpiMap = new LinkedHashMap<>();
			for (String s1 : a.bpi()) {
				for (String s2 : split(s1, ';')) {
					int i = s2.indexOf(':');
					if (i == -1)
						throw new ConfigException(
							"Invalid format for @RestMethod(bpi).  Must be in the format \"ClassName: comma-delimited-tokens\".  \nValue: {0}", s1);
					bpiMap.put(s2.substring(0, i).trim(), s2.substring(i+1).trim());
				}
			}
			psb.addTo(BEAN_includeProperties, bpiMap);
		}

		if (a.bpx().length > 0) {
			Map<String,String> bpxMap = new LinkedHashMap<>();
			for (String s1 : a.bpx()) {
				for (String s2 : split(s1, ';')) {
					int i = s2.indexOf(':');
					if (i == -1)
						throw new ConfigException(
							"Invalid format for @RestMethod(bpx).  Must be in the format \"ClassName: comma-delimited-tokens\".  \nValue: {0}", s1);
					bpxMap.put(s2.substring(0, i).trim(), s2.substring(i+1).trim());
				}
			}
			psb.addTo(BEAN_excludeProperties, bpxMap);
		}

		if (! a.defaultCharset().isEmpty())
			psb.set(REST_defaultCharset, string(a.defaultCharset()));

		if (! a.maxInput().isEmpty())
			psb.set(REST_maxInput, string(a.maxInput()));

		if (! a.maxInput().isEmpty())
			psb.set(REST_maxInput, string(a.maxInput()));

		if (! a.rolesDeclared().isEmpty())
			psb.set(REST_rolesDeclared, string(a.rolesDeclared()));

		if (! a.roleGuard().isEmpty())
			psb.set(REST_roleGuard, string(a.roleGuard()));
	}
}
