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
		.ase("collectionFormat", a.collectionFormat())
		.ase("default", joinnl(a._default()))
		.ase("discriminator", a.discriminator())
		.ase("description", joinnl(a.description()))
		.ase("enum", toSet(a._enum()))
		.ase("examples", parseMap(a.examples()))
		.asf("exclusiveMaximum", a.exclusiveMaximum())
		.asf("exclusiveMinimum", a.exclusiveMinimum())
		.ase("externalDocs", merge(om.getMap("externalDocs"), a.externalDocs()))
		.ase("format", a.format())
		.ase("ignore", a.ignore() ? "true" : null)
		.ase("items", merge(om.getMap("items"), a.items()))
		.ase("maximum", a.maximum())
		.asmo("maxItems", a.maxItems())
		.asmo("maxLength", a.maxLength())
		.asmo("maxProperties", a.maxProperties())
		.ase("minimum", a.minimum())
		.asmo("minItems", a.minItems())
		.asmo("minLength", a.minLength())
		.asmo("minProperties", a.minProperties())
		.ase("multipleOf", a.multipleOf())
		.ase("pattern", a.pattern())
		.ase("properties", toOMap(a.properties()))
		.asf("readOnly", a.readOnly())
		.asf("required", a.required())
		.ase("title", a.title())
		.ase("type", a.type())
		.asf("uniqueItems", a.uniqueItems())
		.ase("xml", joinnl(a.xml()))
		.ase("x-example", joinnl(a.example()))
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
			.ase("collectionFormat", a.collectionFormat())
			.ase("default", joinnl(a._default()))
			.ase("enum", toSet(a._enum()))
			.ase("format", a.format())
			.asf("exclusiveMaximum", a.exclusiveMaximum())
			.asf("exclusiveMinimum", a.exclusiveMinimum())
			.ase("items", merge(om.getMap("items"), a.items()))
			.ase("maximum", a.maximum())
			.asmo("maxItems", a.maxItems())
			.asmo("maxLength", a.maxLength())
			.ase("minimum", a.minimum())
			.asmo("minItems", a.minItems())
			.asmo("minLength", a.minLength())
			.ase("multipleOf", a.multipleOf())
			.ase("pattern", a.pattern())
			.asf("uniqueItems", a.uniqueItems())
			.ase("type", a.type())
			.ase("$ref", a.$ref())
		;
	}

	private static OMap merge(OMap om, SubItems a) throws ParseException {
		if (empty(a))
			return om;
		if (a.value().length > 0)
			om.putAll(parseMap(a.value()));
		return om
			.ase("collectionFormat", a.collectionFormat())
			.ase("default", joinnl(a._default()))
			.ase("enum", toSet(a._enum()))
			.asf("exclusiveMaximum", a.exclusiveMaximum())
			.asf("exclusiveMinimum", a.exclusiveMinimum())
			.ase("format", a.format())
			.ase("items", toOMap(a.items()))
			.ase("maximum", a.maximum())
			.asmo("maxItems", a.maxItems())
			.asmo("maxLength", a.maxLength())
			.ase("minimum", a.minimum())
			.asmo("minItems", a.minItems())
			.asmo("minLength", a.minLength())
			.ase("multipleOf", a.multipleOf())
			.ase("pattern", a.pattern())
			.ase("type", a.type())
			.asf("uniqueItems", a.uniqueItems())
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
}
