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
package org.apache.juneau.http.annotation;

import static org.apache.juneau.internal.StringUtils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.utils.*;

/**
 * Various reusable utility methods when working with annotations.
 */
public class AnnotationUtils {

	//=================================================================================================================
	// Methods for merging annotation values.
	//=================================================================================================================

	private static ObjectMap newMap(ObjectMap om) {
		if (om == null)
			return new ObjectMap();
		return om.modifiable();
	}

	/**
	 * Merges the contents of the specified annotation into the specified map.
	 *
	 * @param om The map to add the annotation values to.
	 * @param a The annotation.
	 * @return The same map with merged results, or a new map if the map was <jk>null</jk>.
	 */
	public static ObjectMap merge(ObjectMap om, Body a) {
		if (empty(a))
			return om;
		om = newMap(om);
		return om
			.appendSkipEmpty("description", joinnl(a.description()))
			.appendSkipEmpty("example", joinnl(a.example()))
			.appendSkipEmpty("examples", joinnl(a.examples()))
			.appendSkipFalse("required", a.required())
			.appendSkipEmpty("schema", merge(om.getObjectMap("schema"), a.schema()))
			.appendSkipEmpty("_value", joinnl(a.value()))
			.appendSkipEmpty("_value", joinnl(a.api()))
		;
	}

	/**
	 * Merges the contents of the specified annotation into the specified map.
	 *
	 * @param om The map to add the annotation values to.
	 * @param a The annotation.
	 * @return The same map with merged results, or a new map if the map was <jk>null</jk>.
	 */
	public static ObjectMap merge(ObjectMap om, ExternalDocs a) {
		if (empty(a))
			return om;
		om = newMap(om);
		return om
			.appendSkipEmpty("description", joinnl(a.description()))
			.appendSkipEmpty("url", a.url())
			.appendSkipEmpty("_value", joinnl(a.value()))
		;
	}

	/**
	 * Merges the contents of the specified annotation into the specified map.
	 *
	 * @param om The map to add the annotation values to.
	 * @param a The annotation.
	 * @return The same map with merged results, or a new map if the map was <jk>null</jk>.
	 */
	public static ObjectMap merge(ObjectMap om, Schema a) {
		if (empty(a))
			return om;
		om = newMap(om);
		return om
			.appendSkipEmpty("additionalProperties", toObjectMap(a.additionalProperties()))
			.appendSkipEmpty("allOf", joinnl(a.allOf()))
			.appendSkipEmpty("collectionFormat", a.collectionFormat())
			.appendSkipEmpty("default", joinnl(a._default()))
			.appendSkipEmpty("discriminator", a.discriminator())
			.appendSkipEmpty("description", joinnl(a.description()))
			.appendSkipEmpty("enum", toSet(a._enum()))
			.appendSkipEmpty("example", joinnl(a.example()))
			.appendSkipEmpty("examples", joinnl(a.examples()))
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
			.appendSkipEmpty("_value", joinnl(a.value()))
			.appendSkipEmpty("$ref", a.$ref())
		;
	}

	/**
	 * Merges the contents of the specified annotation into the specified map.
	 *
	 * @param om The map to add the annotation values to.
	 * @param a The annotation.
	 * @return The same map with merged results, or a new map if the map was <jk>null</jk>.
	 */
	public static ObjectMap merge(ObjectMap om, Response a) {
		if (empty(a))
			return om;
		om = newMap(om);
		return om
			.appendSkipEmpty("description", joinnl(a.description()))
			.appendSkipEmpty("example", joinnl(a.example()))
			.appendSkipEmpty("examples", joinnl(a.examples()))
			.appendSkipEmpty("headers", merge(om.getObjectMap("headers"), a.headers()))
			.appendSkipEmpty("schema", merge(om.getObjectMap("schema"), a.schema()))
			.appendSkipEmpty("_value", joinnl(a.api()))
		;
	}

	/**
	 * Merges the contents of the specified annotation into the specified map.
	 *
	 * @param om The map to add the annotation values to.
	 * @param a The annotation.
	 * @return The same map with merged results, or a new map if the map was <jk>null</jk>.
	 */
	public static ObjectMap merge(ObjectMap om, ResponseHeader[] a) {
		if (a.length == 0)
			return om;
		om = newMap(om);
		for (ResponseHeader aa : a) {
			String name = firstNonEmpty(aa.name(), aa.value());
			if (isEmpty(name))
				throw new RuntimeException("@ResponseHeader used without name or value.");
			om.getObjectMap(name, true).putAll(merge(null, aa));
		}
		return om;
	}

	/**
	 * Merges the contents of the specified annotation into the specified map.
	 *
	 * @param om The map to add the annotation values to.
	 * @param a The annotation.
	 * @return The same map with merged results, or a new map if the map was <jk>null</jk>.
	 */
	public static ObjectMap merge(ObjectMap om, Items a) {
		if (empty(a))
			return om;
		om = newMap(om);
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
			.appendSkipEmpty("_value", joinnl(a.value()))
		;
	}

	/**
	 * Merges the contents of the specified annotation into the specified map.
	 *
	 * @param om The map to add the annotation values to.
	 * @param a The annotation.
	 * @return The same map with merged results, or a new map if the map was <jk>null</jk>.
	 */
	public static ObjectMap merge(ObjectMap om, SubItems a) {
		if (empty(a))
			return om;
		om = newMap(om);
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
			.appendSkipEmpty("_value", joinnl(a.value()))
		;
	}

	/**
	 * Merges the contents of the specified annotation into the specified map.
	 *
	 * @param om The map to add the annotation values to.
	 * @param a The annotation.
	 * @return The same map with merged results, or a new map if the map was <jk>null</jk>.
	 */
	public static ObjectMap merge(ObjectMap om, ResponseHeader a) {
		if (empty(a))
			return om;
		om = newMap(om);
		return om
			.appendSkipEmpty("collectionFormat", a.collectionFormat())
			.appendSkipEmpty("default", joinnl(a._default()))
			.appendSkipEmpty("description", joinnl(a.description()))
			.appendSkipEmpty("enum", toSet(a._enum()))
			.appendSkipEmpty("example", joinnl(a.example()))
			.appendSkipFalse("exclusiveMaximum", a.exclusiveMaximum())
			.appendSkipFalse("exclusiveMinimum", a.exclusiveMinimum())
			.appendSkipEmpty("format", a.format())
			.appendSkipEmpty("items", merge(om.getObjectMap("items"), a.items()))
			.appendSkipEmpty("maximum", a.maximum())
			.appendSkipMinusOne("maxItems", a.maxItems())
			.appendSkipMinusOne("maxLength", a.maxLength())
			.appendSkipMinusOne("maxItems", a.maxItems())
			.appendSkipEmpty("minimum", a.minimum())
			.appendSkipMinusOne("minItems", a.minItems())
			.appendSkipMinusOne("minLength", a.minLength())
			.appendSkipEmpty("multipleOf", a.multipleOf())
			.appendSkipEmpty("pattern", a.pattern())
			.appendSkipEmpty("type", a.type())
			.appendSkipFalse("uniqueItems", a.uniqueItems())
			.appendSkipEmpty("$ref", a.$ref())
			.appendSkipEmpty("_value", joinnl(a.api()))
		;
	}

	/**
	 * Merges the contents of the specified annotation into the specified map.
	 *
	 * @param om The map to add the annotation values to.
	 * @param a The annotation.
	 * @return The same map with merged results, or a new map if the map was <jk>null</jk>.
	 */
	public static ObjectMap merge(ObjectMap om, ResponseStatus a) {
		if (empty(a))
			return om;
		om = newMap(om);
		return om
			.appendSkipEmpty("_value", joinnl(a.api()))
			.appendSkipEmpty("description", joinnl(a.description()));
	}

	/**
	 * Merges the contents of the specified annotation into the specified map.
	 *
	 * @param om The map to add the annotation values to.
	 * @param a The annotation.
	 * @return The same map with merged results, or a new map if the map was <jk>null</jk>.
	 */
	public static ObjectMap merge(ObjectMap om, Path a) {
		if (empty(a))
			return om;
		om = newMap(om);
		return om
			.appendSkipEmpty("collectionFormat", a.collectionFormat())
			.appendSkipEmpty("description", joinnl(a.description()))
			.appendSkipEmpty("enum", toSet(a._enum()))
			.appendSkipEmpty("example", joinnl(a.example()))
			.appendSkipFalse("exclusiveMaximum", a.exclusiveMaximum())
			.appendSkipFalse("exclusiveMinimum", a.exclusiveMinimum())
			.appendSkipEmpty("format", a.format())
			.appendSkipEmpty("items", merge(om.getObjectMap("items"), a.items()))
			.appendSkipEmpty("maximum", a.maximum())
			.appendSkipMinusOne("maxLength", a.maxLength())
			.appendSkipEmpty("minimum", a.minimum())
			.appendSkipMinusOne("minLength", a.minLength())
			.appendSkipEmpty("multipleOf", a.multipleOf())
			.appendSkipEmpty("pattern", a.pattern())
			.appendSkipEmpty("type", a.type())
			.appendSkipEmpty("_value", joinnl(a.api()))
		;
	}

	/**
	 * Merges the contents of the specified annotation into the specified map.
	 *
	 * @param om The map to add the annotation values to.
	 * @param a The annotation.
	 * @return The same map with merged results, or a new map if the map was <jk>null</jk>.
	 */
	public static ObjectMap merge(ObjectMap om, Query a) {
		if (empty(a))
			return om;
		om = newMap(om);
		return om
			.appendSkipFalse("allowEmptyValue", a.allowEmptyValue())
			.appendSkipEmpty("collectionFormat", a.collectionFormat())
			.appendSkipEmpty("default", joinnl(a._default()))
			.appendSkipEmpty("description", joinnl(a.description()))
			.appendSkipEmpty("enum", toSet(a._enum()))
			.appendSkipEmpty("example", joinnl(a.example()))
			.appendSkipFalse("exclusiveMaximum", a.exclusiveMaximum())
			.appendSkipFalse("exclusiveMinimum", a.exclusiveMinimum())
			.appendSkipEmpty("format", a.format())
			.appendSkipEmpty("items", merge(om.getObjectMap("items"), a.items()))
			.appendSkipEmpty("maximum", a.maximum())
			.appendSkipMinusOne("maxItems", a.maxItems())
			.appendSkipMinusOne("maxLength", a.maxLength())
			.appendSkipEmpty("minimum", a.minimum())
			.appendSkipMinusOne("minItems", a.minItems())
			.appendSkipMinusOne("minLength", a.minLength())
			.appendSkipEmpty("multipleOf", a.multipleOf())
			.appendSkipEmpty("pattern", a.pattern())
			.appendSkipFalse("required", a.required())
			.appendSkipEmpty("type", a.type())
			.appendSkipFalse("uniqueItems", a.uniqueItems())
			.appendSkipEmpty("_value", joinnl(a.api()))
		;
	}

	/**
	 * Merges the contents of the specified annotation into the specified map.
	 *
	 * @param om The map to add the annotation values to.
	 * @param a The annotation.
	 * @return The same map with merged results, or a new map if the map was <jk>null</jk>.
	 */
	public static ObjectMap merge(ObjectMap om, Header a) {
		if (empty(a))
			return om;
		om = newMap(om);
		return om
			.appendSkipEmpty("collectionFormat", a.collectionFormat())
			.appendSkipEmpty("default", joinnl(a._default()))
			.appendSkipEmpty("description", joinnl(a.description()))
			.appendSkipEmpty("enum", toSet(a._enum()))
			.appendSkipEmpty("example", joinnl(a.example()))
			.appendSkipFalse("exclusiveMaximum", a.exclusiveMaximum())
			.appendSkipFalse("exclusiveMinimum", a.exclusiveMinimum())
			.appendSkipEmpty("format", a.format())
			.appendSkipEmpty("items", merge(om.getObjectMap("items"), a.items()))
			.appendSkipEmpty("maximum", a.maximum())
			.appendSkipMinusOne("maxItems", a.maxItems())
			.appendSkipMinusOne("maxLength", a.maxLength())
			.appendSkipEmpty("minimum", a.minimum())
			.appendSkipMinusOne("minItems", a.minItems())
			.appendSkipMinusOne("minLength", a.minLength())
			.appendSkipEmpty("multipleOf", a.multipleOf())
			.appendSkipEmpty("pattern", a.pattern())
			.appendSkipFalse("required", a.required())
			.appendSkipEmpty("type", a.type())
			.appendSkipFalse("uniqueItems", a.uniqueItems())
			.appendSkipEmpty("_value", joinnl(a.api()))
		;
	}

	/**
	 * Merges the contents of the specified annotation into the specified map.
	 *
	 * @param om The map to add the annotation values to.
	 * @param a The annotation.
	 * @return The same map with merged results, or a new map if the map was <jk>null</jk>.
	 */
	public static ObjectMap merge(ObjectMap om, FormData a) {
		if (empty(a))
			return om;
		om = newMap(om);
		return om
			.appendSkipFalse("allowEmptyValue", a.allowEmptyValue())
			.appendSkipEmpty("collectionFormat", a.collectionFormat())
			.appendSkipEmpty("default", joinnl(a._default()))
			.appendSkipEmpty("description", joinnl(a.description()))
			.appendSkipEmpty("enum", toSet(a._enum()))
			.appendSkipEmpty("example", joinnl(a.example()))
			.appendSkipFalse("exclusiveMaximum", a.exclusiveMaximum())
			.appendSkipFalse("exclusiveMinimum", a.exclusiveMinimum())
			.appendSkipEmpty("format", a.format())
			.appendSkipEmpty("items", merge(om.getObjectMap("items"), a.items()))
			.appendSkipEmpty("maximum", a.maximum())
			.appendSkipMinusOne("maxItems", a.maxItems())
			.appendSkipMinusOne("maxLength", a.maxLength())
			.appendSkipEmpty("minimum", a.minimum())
			.appendSkipMinusOne("minItems", a.minItems())
			.appendSkipMinusOne("minLength", a.minLength())
			.appendSkipEmpty("multipleOf", a.multipleOf())
			.appendSkipEmpty("pattern", a.pattern())
			.appendSkipFalse("required", a.required())
			.appendSkipEmpty("type", a.type())
			.appendSkipFalse("uniqueItems", a.uniqueItems())
			.appendSkipEmpty("_value", joinnl(a.api()))
		;
	}

	//=================================================================================================================
	// Methods for checking if annotations are empty.
	//=================================================================================================================

	/**
	 * Returns <jk>true</jk> if the specified annotation contains all default values.
	 *
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if the specified annotation contains all default values.
	 */
	public static boolean empty(Query a) {
		if (a == null)
			return true;
		return
			allEmpty(a.description(), a._default(), a.example(), a.api())
			&& allEmpty(a.name(), a.value(), a.type(), a.format(), a.pattern(), a.collectionFormat(), a.maximum(), a.minimum(), a.multipleOf())
			&& allFalse(a.allowEmptyValue(), a.exclusiveMaximum(), a.exclusiveMinimum(), a.required(), a.uniqueItems())
			&& allMinusOne(a.maxLength(), a.minLength(), a.maxItems(), a.minItems())
			&& empty(a.items());
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation contains all default values.
	 *
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if the specified annotation contains all default values.
	 */
	public static boolean empty(Header a) {
		if (a == null)
			return true;
		return
			allEmpty(a.description(), a._default(), a._enum(), a.example(), a.api())
			&& allEmpty(a.name(), a.value(), a.type(), a.format(), a.pattern(), a.collectionFormat(), a.maximum(), a.minimum(), a.multipleOf())
			&& allFalse(a.exclusiveMaximum(), a.exclusiveMinimum(), a.required(), a.uniqueItems())
			&& allMinusOne(a.maxLength(), a.minLength(), a.maxItems(), a.minItems())
			&& empty(a.items());
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation contains all default values.
	 *
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if the specified annotation contains all default values.
	 */
	public static boolean empty(FormData a) {
		if (a == null)
			return true;
		return
			allEmpty(a.description(), a._default(), a._enum(), a.example(), a.api())
			&& allEmpty(a.name(), a.value(), a.type(), a.format(), a.pattern(), a.collectionFormat(), a.maximum(), a.minimum(), a.multipleOf())
			&& allFalse(a.allowEmptyValue(), a.exclusiveMaximum(), a.exclusiveMinimum(), a.required(), a.uniqueItems())
			&& allMinusOne(a.maxLength(), a.minLength(), a.maxItems(), a.minItems())
			&& empty(a.items());
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation contains all default values.
	 *
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if the specified annotation contains all default values.
	 */
	public static boolean empty(Response a) {
		if (a == null)
			return true;
		return
			allEmpty(a.description(), a.example(), a.examples(), a.api())
			&& a.headers().length == 0
			&& empty(a.schema())
		;
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation contains all default values.
	 *
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if the specified annotation contains all default values.
	 */
	public static boolean empty(ResponseHeader a) {
		if (a == null)
			return true;
		return
			allEmpty(a.description(), a._default(), a._enum(), a.example(), a.api())
			&& allEmpty(a.name(), a.value(), a.type(), a.format(), a.collectionFormat(), a.$ref(), a.maximum(), a.minimum(), a.multipleOf())
			&& allFalse(a.exclusiveMaximum(), a.exclusiveMinimum(), a.uniqueItems())
			&& allMinusOne(a.maxLength(), a.minLength(), a.maxItems(), a.minItems())
			&& empty(a.items());
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation contains all default values.
	 *
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if the specified annotation contains all default values.
	 */
	public static boolean empty(ResponseStatus a) {
		if (a == null)
			return true;
		return
			allEmpty(a.description(), a.api());
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation contains all default values.
	 *
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if the specified annotation contains all default values.
	 */
	public static boolean empty(Schema a) {
		if (a == null)
			return true;
		return
			allEmpty(a.value(), a.description(), a._default(), a._enum(), a.allOf(), a.properties(), a.additionalProperties(), a.xml(), a.example(), a.examples())
			&& allEmpty(a.$ref(), a.format(), a.title(), a.multipleOf(), a.maximum(), a.minimum(), a.pattern(), a.type(), a.discriminator())
			&& allMinusOne(a.maxProperties(), a.minProperties())
			&& allFalse(a.ignore(), a.exclusiveMaximum(), a.exclusiveMinimum(), a.readOnly(), a.required(), a.uniqueItems())
			&& allMinusOne(a.maxLength(), a.minLength(), a.maxItems(), a.minItems())
			&& empty(a.items())
			&& empty(a.externalDocs());
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation contains all default values.
	 *
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if the specified annotation contains all default values.
	 */
	public static boolean empty(ExternalDocs a) {
		if (a == null)
			return true;
		return
			allEmpty(a.value(), a.description())
			&& allEmpty(a.url());
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation contains all default values.
	 *
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if the specified annotation contains all default values.
	 */
	public static boolean empty(Body a) {
		if (a == null)
			return true;
		return
			allEmpty(a.description(), a.example(), a.examples(), a.api(), a.value())
			&& allFalse(a.required())
			&& empty(a.schema());
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation contains all default values.
	 *
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if the specified annotation contains all default values.
	 */
	public static boolean empty(Contact a) {
		if (a == null)
			return true;
		return
			allEmpty(a.value())
			&& allEmpty(a.name(), a.url(), a.email());
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation contains all default values.
	 *
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if the specified annotation contains all default values.
	 */
	public static boolean empty(License a) {
		if (a == null)
			return true;
		return
			allEmpty(a.value())
			&& allEmpty(a.name(), a.url());
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation contains all default values.
	 *
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if the specified annotation contains all default values.
	 */
	public static boolean empty(Items a) {
		if (a == null)
			return true;
		return
			allEmpty(a.value(), a._default(), a._enum())
			&& allEmpty(a.type(), a.format(), a.collectionFormat(), a.pattern(), a.$ref(), a.maximum(), a.minimum(), a.multipleOf())
			&& allFalse(a.exclusiveMaximum(), a.exclusiveMinimum(), a.uniqueItems())
			&& allMinusOne(a.maxLength(), a.minLength(), a.maxItems(), a.minItems())
			&& empty(a.items());
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation contains all default values.
	 *
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if the specified annotation contains all default values.
	 */
	public static boolean empty(SubItems a) {
		if (a == null)
			return true;
		return
			allEmpty(a.value(), a._default(), a._enum(), a.items())
			&& allEmpty(a.type(), a.format(), a.collectionFormat(), a.pattern(), a.$ref(), a.maximum(), a.minimum(), a.multipleOf())
			&& allFalse(a.exclusiveMaximum(), a.exclusiveMinimum(), a.uniqueItems())
			&& allMinusOne(a.maxLength(), a.minLength(), a.maxItems(), a.minItems());
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation contains all default values.
	 *
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if the specified annotation contains all default values.
	 */
	public static boolean empty(Path a) {
		if (a == null)
			return true;
		return
			allEmpty(a.description(), a._enum(), a.example(), a.api())
			&& allEmpty(a.name(), a.value(), a.type(), a.format(), a.pattern(), a.maximum(), a.minimum(), a.multipleOf())
			&& allFalse(a.exclusiveMaximum(), a.exclusiveMinimum())
			&& allMinusOne(a.maxLength(), a.minLength())
			&& empty(a.items());
	}

	/**
	 * Returns <jk>true</jk> if all the specified strings are empty or null.
	 *
	 * @param strings The strings to test.
	 * @return <jk>true</jk> if all the specified strings are empty or null.
	 */
	protected static boolean allEmpty(String...strings) {
		for (String s : strings)
			if (s != null && ! s.isEmpty())
				return false;
		return true;
	}

	/**
	 * Returns <jk>true</jk> if all the specified strings are empty or null.
	 *
	 * @param strings The strings to test.
	 * @return <jk>true</jk> if all the specified strings are empty or null.
	 */
	protected static boolean allEmpty(String[]...strings) {
		for (String[] s : strings)
			if (s.length != 0 || ! allEmpty(s))
				return false;
		return true;
	}

	/**
	 * Returns <jk>true</jk> if all the specified booleans are false.
	 *
	 * @param booleans The booleans to test.
	 * @return <jk>true</jk> if all the specified booleans are false.
	 */
	protected static boolean allFalse(boolean...booleans) {
		for (boolean b : booleans)
			if (b)
				return false;
		return true;
	}

	/**
	 * Returns <jk>true</jk> if all the specified longs are -1.
	 *
	 * @param longs The booleans to test.
	 * @return <jk>true</jk> if all the specified longs are -1.
	 */
	protected static boolean allMinusOne(long...longs) {
		for (long i : longs)
			if (i != -1)
				return false;
		return true;
	}

	private static Set<String> toSet(String[] s) {
		return toSet(joinnl(s));
	}

	private static Set<String> toSet(String s) {
		if (isEmpty(s))
			return null;
		Set<String> set = new ASet<>();
		try {
			for (Object o : StringUtils.parseListOrCdl(s))
				set.add(o.toString());
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
		return set;
	}

	final static ObjectMap toObjectMap(String[] ss) {
		String s = joinnl(ss);
		if (s.isEmpty())
			return null;
		if (! isObjectMap(s, true))
			s = "{" + s + "}";
		try {
			return new ObjectMap(s);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}
}
