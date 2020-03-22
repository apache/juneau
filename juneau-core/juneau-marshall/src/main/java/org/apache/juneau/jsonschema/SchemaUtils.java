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

import org.apache.juneau.*;
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
	public static ObjectMap asMap(Schema a) throws ParseException {
		if (a == null)
			return ObjectMap.EMPTY_MAP;
		ObjectMap om = new ObjectMap();
		if (empty(a))
			return om;
		if (a.value().length > 0)
			om.putAll(parseMap(a.value()));
	return om
		.appendSkipEmpty("additionalProperties", toObjectMap(a.additionalProperties()))
		.appendSkipEmpty("allOf", joinnl(a.allOf()))
		.appendSkipEmpty("collectionFormat", a.collectionFormat())
		.appendSkipEmpty("default", joinnl(a._default()))
		.appendSkipEmpty("discriminator", a.discriminator())
		.appendSkipEmpty("description", joinnl(a.description()))
		.appendSkipEmpty("enum", toSet(a._enum()))
		.appendSkipEmpty("examples", parseMap(a.examples()))
		.appendSkipFalse("exclusiveMaximum", a.exclusiveMaximum())
		.appendSkipFalse("exclusiveMinimum", a.exclusiveMinimum())
		.appendSkipEmpty("externalDocs", merge(om.getObjectMap("externalDocs"), a.externalDocs()))
		.appendSkipEmpty("format", a.format())
		.appendSkipEmpty("ignore", a.ignore() ? "true" : null)
		.appendSkipEmpty("items", merge(om.getObjectMap("items"), a.items()))
		.appendSkipEmpty("maximum", a.maximum())
		.appendSkipMinusOne("maxItems", a.maxItems())
		.appendSkipMinusOne("maxLength", a.maxLength())
		.appendSkipMinusOne("maxProperties", a.maxProperties())
		.appendSkipEmpty("minimum", a.minimum())
		.appendSkipMinusOne("minItems", a.minItems())
		.appendSkipMinusOne("minLength", a.minLength())
		.appendSkipMinusOne("minProperties", a.minProperties())
		.appendSkipEmpty("multipleOf", a.multipleOf())
		.appendSkipEmpty("pattern", a.pattern())
		.appendSkipEmpty("properties", toObjectMap(a.properties()))
		.appendSkipFalse("readOnly", a.readOnly())
		.appendSkipFalse("required", a.required())
		.appendSkipEmpty("title", a.title())
		.appendSkipEmpty("type", a.type())
		.appendSkipFalse("uniqueItems", a.uniqueItems())
		.appendSkipEmpty("xml", joinnl(a.xml()))
		.appendSkipEmpty("x-example", joinnl(a.example()))
		.appendSkipEmpty("$ref", a.$ref())
	;
	}

	private static ObjectMap toObjectMap(String[] ss) throws ParseException {
		if (ss.length == 0)
			return null;
		String s = joinnl(ss);
		if (s.isEmpty())
			return null;
		if (! isObjectMap(s, true))
			s = "{" + s + "}";
		return new ObjectMap(s);
	}

	private static ObjectMap parseMap(Object o) throws ParseException {
		if (o == null)
			return null;
		if (o instanceof String[])
			o = joinnl((String[])o);
		if (o instanceof String) {
			String s = o.toString();
			if (s.isEmpty())
				return null;
			if ("IGNORE".equalsIgnoreCase(s))
				return new ObjectMap().append("ignore", true);
			if (! isObjectMap(s, true))
				s = "{" + s + "}";
			return new ObjectMap(s);
		}
		if (o instanceof ObjectMap)
			return (ObjectMap)o;
		throw new ParseException("Unexpected data type ''{0}''.  Expected ObjectMap or String.", o.getClass().getName());
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

	private static ObjectMap merge(ObjectMap om, Items a) throws ParseException {
		if (empty(a))
			return om;
		if (a.value().length > 0)
			om.putAll(parseMap(a.value()));
		return om
			.appendSkipEmpty("collectionFormat", a.collectionFormat())
			.appendSkipEmpty("default", joinnl(a._default()))
			.appendSkipEmpty("enum", toSet(a._enum()))
			.appendSkipEmpty("format", a.format())
			.appendSkipFalse("exclusiveMaximum", a.exclusiveMaximum())
			.appendSkipFalse("exclusiveMinimum", a.exclusiveMinimum())
			.appendSkipEmpty("items", merge(om.getObjectMap("items"), a.items()))
			.appendSkipEmpty("maximum", a.maximum())
			.appendSkipMinusOne("maxItems", a.maxItems())
			.appendSkipMinusOne("maxLength", a.maxLength())
			.appendSkipEmpty("minimum", a.minimum())
			.appendSkipMinusOne("minItems", a.minItems())
			.appendSkipMinusOne("minLength", a.minLength())
			.appendSkipEmpty("multipleOf", a.multipleOf())
			.appendSkipEmpty("pattern", a.pattern())
			.appendSkipFalse("uniqueItems", a.uniqueItems())
			.appendSkipEmpty("type", a.type())
			.appendSkipEmpty("$ref", a.$ref())
		;
	}

	private static ObjectMap merge(ObjectMap om, SubItems a) throws ParseException {
		if (empty(a))
			return om;
		if (a.value().length > 0)
			om.putAll(parseMap(a.value()));
		return om
			.appendSkipEmpty("collectionFormat", a.collectionFormat())
			.appendSkipEmpty("default", joinnl(a._default()))
			.appendSkipEmpty("enum", toSet(a._enum()))
			.appendSkipFalse("exclusiveMaximum", a.exclusiveMaximum())
			.appendSkipFalse("exclusiveMinimum", a.exclusiveMinimum())
			.appendSkipEmpty("format", a.format())
			.appendSkipEmpty("items", toObjectMap(a.items()))
			.appendSkipEmpty("maximum", a.maximum())
			.appendSkipMinusOne("maxItems", a.maxItems())
			.appendSkipMinusOne("maxLength", a.maxLength())
			.appendSkipEmpty("minimum", a.minimum())
			.appendSkipMinusOne("minItems", a.minItems())
			.appendSkipMinusOne("minLength", a.minLength())
			.appendSkipEmpty("multipleOf", a.multipleOf())
			.appendSkipEmpty("pattern", a.pattern())
			.appendSkipEmpty("type", a.type())
			.appendSkipFalse("uniqueItems", a.uniqueItems())
			.appendSkipEmpty("$ref", a.$ref())
		;
	}

	private static ObjectMap merge(ObjectMap om, ExternalDocs a) throws ParseException {
		if (empty(a))
			return om;
		if (a.value().length > 0)
			om.putAll(parseMap(a.value()));
		return om
			.appendSkipEmpty("description", joinnl(a.description()))
			.appendSkipEmpty("url", a.url())
		;
	}
}
