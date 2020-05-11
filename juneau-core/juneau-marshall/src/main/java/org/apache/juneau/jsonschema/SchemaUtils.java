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
package org.apache.juneau.jsonschema;

import static org.apache.juneau.http.annotation.AnnotationUtils.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.util.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.jsonschema.annotation.*;
import org.apache.juneau.parser.*;

/**
 * Utilities for working with the schema annotations.
 */
public class SchemaUtils {

	/**
	 * Converts the specified <ja>@Schema</ja> annotation into a generic map.
	 *
	 * @param a The annotation instance.  Can be <jk>null</jk>.
	 * @return The schema converted to a map, or and empty map if the annotation was null.
	 * @throws ParseException Malformed input encountered.
	 */
	public static OMap asMap(Schema a) throws ParseException {
		if (a == null)
			return OMap.EMPTY_MAP;
		OMap om = new OMap();
		if (empty(a))
			return om;
		if (a.value().length > 0)
			om.putAll(parseMap(a.value()));
	return om
		.ase("additionalProperties", toOMap(a.additionalProperties()))
		.ase("allOf", joinnl(a.allOf()))
		.ase("collectionFormat", a.collectionFormat(), a.cf())
		.ase("default", joinnl(a._default(), a.df()))
		.ase("discriminator", a.discriminator())
		.ase("description", joinnl(a.description(), a.d()))
		.ase("enum", toSet(a._enum()), toSet(a.e()))
		.ase("examples", parseMap(a.examples()), parseMap(a.exs()))
		.asf("exclusiveMaximum", a.exclusiveMaximum() || a.emax())
		.asf("exclusiveMinimum", a.exclusiveMinimum() || a.emin())
		.ase("externalDocs", merge(om.getMap("externalDocs"), a.externalDocs()))
		.ase("format", a.format(), a.f())
		.ase("ignore", a.ignore() ? "true" : null)
		.ase("items", merge(om.getMap("items"), a.items()))
		.ase("maximum", a.maximum(), a.max())
		.asmo("maxItems", a.maxItems(), a.maxi())
		.asmo("maxLength", a.maxLength(), a.maxl())
		.asmo("maxProperties", a.maxProperties(), a.maxp())
		.ase("minimum", a.minimum(), a.min())
		.asmo("minItems", a.minItems(), a.mini())
		.asmo("minLength", a.minLength(), a.minl())
		.asmo("minProperties", a.minProperties(), a.minp())
		.ase("multipleOf", a.multipleOf(), a.mo())
		.ase("pattern", a.pattern(), a.p())
		.ase("properties", toOMap(a.properties()))
		.asf("readOnly", a.readOnly() || a.ro())
		.asf("required", a.required() || a.r())
		.ase("title", a.title())
		.ase("type", a.type(), a.t())
		.asf("uniqueItems", a.uniqueItems() || a.ui())
		.ase("xml", joinnl(a.xml()))
		.ase("x-example", joinnl(a.example(), a.ex()))
		.ase("$ref", a.$ref())
	;
	}

	private static OMap toOMap(String[] ss) throws ParseException {
		if (ss.length == 0)
			return null;
		String s = joinnl(ss);
		if (s.isEmpty())
			return null;
		if (! isJsonObject(s, true))
			s = "{" + s + "}";
		return OMap.ofJson(s);
	}

	private static OMap parseMap(Object o) throws ParseException {
		if (o == null)
			return null;
		if (o instanceof String[])
			o = joinnl((String[])o);
		if (o instanceof String) {
			String s = o.toString();
			if (s.isEmpty())
				return null;
			if ("IGNORE".equalsIgnoreCase(s))
				return OMap.of("ignore", true);
			if (! isJsonObject(s, true))
				s = "{" + s + "}";
			return OMap.ofJson(s);
		}
		if (o instanceof OMap)
			return (OMap)o;
		throw new ParseException("Unexpected data type ''{0}''.  Expected OMap or String.", o.getClass().getName());
	}

	private static Set<String> toSet(String[] ss) throws ParseException {
		if (ss.length == 0)
			return null;
		String s = joinnl(ss);
		if (s.isEmpty())
			return null;
		Set<String> set = ASet.of();
		for (Object o : StringUtils.parseListOrCdl(s))
			set.add(o.toString());
		return set;
	}

	private static OMap merge(OMap om, Items a) throws ParseException {
		if (empty(a))
			return om;
		if (a.value().length > 0)
			om.putAll(parseMap(a.value()));
		return om
			.ase("collectionFormat", a.collectionFormat(), a.cf())
			.ase("default", joinnl(a._default(), a.df()))
			.ase("enum", toSet(a._enum()), toSet(a.e()))
			.ase("format", a.format(), a.f())
			.asf("exclusiveMaximum", a.exclusiveMaximum() || a.emax())
			.asf("exclusiveMinimum", a.exclusiveMinimum() || a.emin())
			.ase("items", merge(om.getMap("items"), a.items()))
			.ase("maximum", a.maximum(), a.max())
			.asmo("maxItems", a.maxItems(), a.maxi())
			.asmo("maxLength", a.maxLength(), a.maxl())
			.ase("minimum", a.minimum(), a.min())
			.asmo("minItems", a.minItems(), a.mini())
			.asmo("minLength", a.minLength(), a.minl())
			.ase("multipleOf", a.multipleOf(), a.mo())
			.ase("pattern", a.pattern(), a.p())
			.asf("uniqueItems", a.uniqueItems() || a.ui())
			.ase("type", a.type(), a.t())
			.ase("$ref", a.$ref())
		;
	}

	private static OMap merge(OMap om, SubItems a) throws ParseException {
		if (empty(a))
			return om;
		if (a.value().length > 0)
			om.putAll(parseMap(a.value()));
		return om
			.ase("collectionFormat", a.collectionFormat(), a.cf())
			.ase("default", joinnl(a._default(), a.df()))
			.ase("enum", toSet(a._enum()), toSet(a.e()))
			.asf("exclusiveMaximum", a.exclusiveMaximum() || a.emax())
			.asf("exclusiveMinimum", a.exclusiveMinimum() || a.emin())
			.ase("format", a.format(), a.f())
			.ase("items", toOMap(a.items()))
			.ase("maximum", a.maximum(), a.max())
			.asmo("maxItems", a.maxItems(), a.maxi())
			.asmo("maxLength", a.maxLength(), a.maxl())
			.ase("minimum", a.minimum(), a.min())
			.asmo("minItems", a.minItems(), a.mini())
			.asmo("minLength", a.minLength(), a.minl())
			.ase("multipleOf", a.multipleOf(), a.mo())
			.ase("pattern", a.pattern(), a.p())
			.ase("type", a.type(), a.t())
			.asf("uniqueItems", a.uniqueItems() || a.ui())
			.ase("$ref", a.$ref())
		;
	}

	private static OMap merge(OMap om, ExternalDocs a) throws ParseException {
		if (empty(a))
			return om;
		if (a.value().length > 0)
			om.putAll(parseMap(a.value()));
		return om
			.ase("description", joinnl(a.description()))
			.ase("url", a.url())
		;
	}

	private static String joinnl(String[]...s) {
		for (String[] ss : s) {
			if (ss.length != 0)
			return StringUtils.joinnl(ss).trim();
		}
		return "";
	}
}
