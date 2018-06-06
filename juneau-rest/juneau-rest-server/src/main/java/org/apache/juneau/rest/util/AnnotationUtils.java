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
package org.apache.juneau.rest.util;

import static org.apache.juneau.internal.StringUtils.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.exception.*;

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
			.appendSkipEmpty("_value", joinnl(a.api()))
			.appendSkipEmpty("description", joinnl(a.description()))
			.appendSkipEmpty("required", a.required())
			.appendSkipEmpty("example", joinnl(a.example()))
			.appendSkipEmpty("examples", joinnl(a.examples()))
			.append("schema", merge(om.getObjectMap("schema"), a.schema()));
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
			.appendSkipEmpty("_value", joinnl(a.value()))
			.appendSkipEmpty("description", joinnl(a.description()))
			.appendSkipEmpty("url", a.url());
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
			.appendSkipEmpty("_value", joinnl(a.value()))
			.appendSkipEmpty("$ref", a.$ref())
			.appendSkipEmpty("format", a.format())
			.appendSkipEmpty("title", a.title())
			.appendSkipEmpty("description", joinnl(a.description()))
			.appendSkipEmpty("default", joinnl(a._default()))
			.appendSkipEmpty("multipleOf", a.multipleOf())
			.appendSkipEmpty("maximum", a.maximum())
			.appendSkipEmpty("exclusiveMaximum", a.exclusiveMaximum())
			.appendSkipEmpty("minimum", a.minimum())
			.appendSkipEmpty("exclusiveMinimum", a.exclusiveMinimum())
			.appendSkipEmpty("maxLength", a.maxLength())
			.appendSkipEmpty("minLength", a.minLength())
			.appendSkipEmpty("pattern", a.pattern())
			.appendSkipEmpty("maxItems", a.maxItems())
			.appendSkipEmpty("minItems", a.minItems())
			.appendSkipEmpty("uniqueItems", a.uniqueItems())
			.appendSkipEmpty("maxProperties", a.maxProperties())
			.appendSkipEmpty("minProperties", a.minProperties())
			.appendSkipEmpty("required", a.required())
			.appendSkipEmpty("enum", joinnl(a._enum()))
			.appendSkipEmpty("type", a.type())
			.append("items", merge(om.getObjectMap("items"), a.items()))
			.appendSkipEmpty("allOf", joinnl(a.allOf()))
			.appendSkipEmpty("properties", joinnl(a.properties()))
			.appendSkipEmpty("additionalProperties", joinnl(a.additionalProperties()))
			.appendSkipEmpty("discriminator", a.discriminator())
			.appendSkipEmpty("readOnly", a.readOnly())
			.appendSkipEmpty("xml", joinnl(a.xml()))
			.append("externalDocs", merge(om.getObjectMap("externalDocs"), a.externalDocs()))
			.appendSkipEmpty("example", joinnl(a.example()))
			.appendSkipEmpty("examples", joinnl(a.examples()))
			.appendSkipEmpty("ignore", a.ignore() ? "true" : null);
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
			.appendSkipEmpty("_value", joinnl(a.api()))
			.appendSkipEmpty("description", joinnl(a.description()))
			.appendSkipEmpty("example", joinnl(a.example()))
			.appendSkipEmpty("examples", joinnl(a.examples()))
			.append("schema", merge(om.getObjectMap("schema"), a.schema()))
			.append("headers", merge(om.getObjectMap("headers"), a.headers()));
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
				throw new InternalServerError("@ResponseHeader used without name or value.");
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
			.appendSkipEmpty("_value", joinnl(a.value()))
			.appendSkipEmpty("type", a.type())
			.appendSkipEmpty("format", a.format())
			.appendSkipEmpty("collectionFormat", a.collectionFormat())
			.appendSkipEmpty("pattern", a.pattern())
			.appendSkipEmpty("$ref", a.$ref())
			.appendSkipEmpty("maximum", a.maximum())
			.appendSkipEmpty("minimum", a.minimum())
			.appendSkipEmpty("multipleOf", a.multipleOf())
			.appendSkipEmpty("maxLength", a.maxLength())
			.appendSkipEmpty("minLength", a.minLength())
			.appendSkipEmpty("maxItems", a.maxItems())
			.appendSkipEmpty("minItems", a.minItems())
			.appendSkipEmpty("exclusiveMaximum", a.exclusiveMaximum())
			.appendSkipEmpty("exclusiveMinimum", a.exclusiveMinimum())
			.appendSkipEmpty("uniqueItems", a.uniqueItems())
			.appendSkipEmpty("default", joinnl(a._default()))
			.appendSkipEmpty("enum", joinnl(a._enum()));
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
			.appendSkipEmpty("_value", joinnl(a.api()))
			.appendSkipEmpty("$ref", a.$ref())
			.appendSkipEmpty("description", joinnl(a.description()))
			.appendSkipEmpty("type", a.type())
			.appendSkipEmpty("format", a.format())
			.appendSkipEmpty("collectionFormat", a.collectionFormat())
			.appendSkipEmpty("maximum", a.maximum())
			.appendSkipEmpty("minimum", a.minimum())
			.appendSkipEmpty("multipleOf", a.multipleOf())
			.appendSkipEmpty("maxLength", a.maxLength())
			.appendSkipEmpty("minLength", a.minLength())
			.appendSkipEmpty("maxItems", a.maxItems())
			.appendSkipEmpty("minItems", a.minItems())
			.appendSkipEmpty("exclusiveMaximum", a.exclusiveMaximum())
			.appendSkipEmpty("exclusiveMinimum", a.exclusiveMinimum())
			.appendSkipEmpty("uniqueItems", a.uniqueItems())
			.appendSkipEmpty("default", joinnl(a._default()))
			.append("items", merge(om.getObjectMap("items"), a.items()))
			.appendSkipEmpty("default", joinnl(a._default()))
			.appendSkipEmpty("enum", joinnl(a._enum()))
			.appendSkipEmpty("example", joinnl(a.example()));
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
			.appendSkipEmpty("_value", joinnl(a.api()))
			.appendSkipEmpty("description", joinnl(a.description()))
			.appendSkipEmpty("type", a.type())
			.appendSkipEmpty("format", a.format())
			.appendSkipEmpty("pattern", a.pattern())
			.appendSkipEmpty("maximum", a.maximum())
			.appendSkipEmpty("minimum", a.minimum())
			.appendSkipEmpty("multipleOf", a.multipleOf())
			.appendSkipEmpty("maxLength", a.maxLength())
			.appendSkipEmpty("minLength", a.minLength())
			.appendSkipEmpty("allowEmptyValue", a.allowEmptyValue())
			.appendSkipEmpty("exclusiveMaximum", a.exclusiveMaximum())
			.appendSkipEmpty("exclusiveMinimum", a.exclusiveMinimum())
			.append("schema", merge(om.getObjectMap("schema"), a.schema()))
			.appendSkipEmpty("enum", joinnl(a._enum()))
			.appendSkipEmpty("example", joinnl(a.example()));
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
			.appendSkipEmpty("_value", joinnl(a.api()))
			.appendSkipEmpty("description", joinnl(a.description()))
			.appendSkipEmpty("required", a.required())
			.appendSkipEmpty("type", a.type())
			.appendSkipEmpty("format", a.format())
			.appendSkipEmpty("pattern", a.pattern())
			.appendSkipEmpty("collectionFormat", a.collectionFormat())
			.appendSkipEmpty("maximum", a.maximum())
			.appendSkipEmpty("minimum", a.minimum())
			.appendSkipEmpty("multipleOf", a.multipleOf())
			.appendSkipEmpty("maxLength", a.maxLength())
			.appendSkipEmpty("minLength", a.minLength())
			.appendSkipEmpty("maxItems", a.maxItems())
			.appendSkipEmpty("minItems", a.minItems())
			.appendSkipEmpty("allowEmptyValue", a.allowEmptyValue())
			.appendSkipEmpty("exclusiveMaximum", a.exclusiveMaximum())
			.appendSkipEmpty("exclusiveMinimum", a.exclusiveMinimum())
			.appendSkipEmpty("uniqueItems", a.uniqueItems())
			.append("schema", merge(om.getObjectMap("schema"), a.schema()))
			.appendSkipEmpty("default", joinnl(a._default()))
			.appendSkipEmpty("enum", joinnl(a._enum()))
			.append("items", merge(om.getObjectMap("items"), a.items()))
			.appendSkipEmpty("example", joinnl(a.example()));
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
			.appendSkipEmpty("_value", joinnl(a.api()))
			.appendSkipEmpty("description", joinnl(a.description()))
			.appendSkipEmpty("required", a.required())
			.appendSkipEmpty("type", a.type())
			.appendSkipEmpty("format", a.format())
			.appendSkipEmpty("pattern", a.pattern())
			.appendSkipEmpty("collectionFormat", a.collectionFormat())
			.appendSkipEmpty("maximum", a.maximum())
			.appendSkipEmpty("minimum", a.minimum())
			.appendSkipEmpty("multipleOf", a.multipleOf())
			.appendSkipEmpty("maxLength", a.maxLength())
			.appendSkipEmpty("minLength", a.minLength())
			.appendSkipEmpty("maxItems", a.maxItems())
			.appendSkipEmpty("minItems", a.minItems())
			.appendSkipEmpty("allowEmptyValue", a.allowEmptyValue())
			.appendSkipEmpty("exclusiveMaximum", a.exclusiveMaximum())
			.appendSkipEmpty("exclusiveMinimum", a.exclusiveMinimum())
			.appendSkipEmpty("uniqueItems", a.uniqueItems())
			.append("schema", merge(om.getObjectMap("schema"), a.schema()))
			.appendSkipEmpty("default", joinnl(a._default()))
			.appendSkipEmpty("enum", joinnl(a._enum()))
			.append("items", merge(om.getObjectMap("items"), a.items()))
			.appendSkipEmpty("example", joinnl(a.example()));
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
			.appendSkipEmpty("_value", joinnl(a.api()))
			.appendSkipEmpty("description", joinnl(a.description()))
			.appendSkipEmpty("required", a.required())
			.appendSkipEmpty("type", a.type())
			.appendSkipEmpty("format", a.format())
			.appendSkipEmpty("pattern", a.pattern())
			.appendSkipEmpty("collectionFormat", a.collectionFormat())
			.appendSkipEmpty("maximum", a.maximum())
			.appendSkipEmpty("minimum", a.minimum())
			.appendSkipEmpty("multipleOf", a.multipleOf())
			.appendSkipEmpty("maxLength", a.maxLength())
			.appendSkipEmpty("minLength", a.minLength())
			.appendSkipEmpty("maxItems", a.maxItems())
			.appendSkipEmpty("minItems", a.minItems())
			.appendSkipEmpty("allowEmptyValue", a.allowEmptyValue())
			.appendSkipEmpty("exclusiveMaximum", a.exclusiveMaximum())
			.appendSkipEmpty("exclusiveMinimum", a.exclusiveMinimum())
			.appendSkipEmpty("uniqueItems", a.uniqueItems())
			.append("schema", merge(om.getObjectMap("schema"), a.schema()))
			.appendSkipEmpty("default", joinnl(a._default()))
			.appendSkipEmpty("enum", joinnl(a._enum()))
			.append("items", merge(om.getObjectMap("items"), a.items()))
			.appendSkipEmpty("example", joinnl(a.example()));
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
			empty(a.description(), a._default(), a.example(), a.api())
			&& empty(
				a.name(), a.value(), a.required(), a.type(), a.format(), a.pattern(), a.collectionFormat(), a.maximum(), a.minimum(), a.multipleOf(), a.maxLength(), a.minLength(),
				a.maxItems(), a.minItems(), a.allowEmptyValue(), a.exclusiveMaximum(), a.exclusiveMinimum(), a.uniqueItems()
			)
			&& empty(a.schema())
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
			empty(a.description(), a._default(), a._enum(), a.example(), a.api())
			&& empty(
				a.name(), a.value(), a.required(), a.type(), a.format(), a.pattern(), a.collectionFormat(), a.maximum(), a.minimum(), a.multipleOf(), a.maxLength(), 
				a.minLength(), a.maxItems(), a.minItems(), a.allowEmptyValue(), a.exclusiveMaximum(), a.exclusiveMinimum(), a.uniqueItems()
			)
			&& empty(a.schema())
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
			empty(a.description(), a._default(), a._enum(), a.example(), a.api())
			&& empty(
				a.name(), a.value(), a.required(), a.type(), a.format(), a.pattern(), a.collectionFormat(), a.maximum(), a.minimum(), a.multipleOf(), a.maxLength(), 
				a.minLength(), a.maxItems(), a.minItems(), a.allowEmptyValue(), a.exclusiveMaximum(), a.exclusiveMinimum(), a.uniqueItems()
			)
			&& empty(a.schema())
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
			empty(a.description(), a.example(), a.examples(), a.api())
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
			empty(a.description(), a._default(), a._enum(), a.example(), a.api())
			&& empty(
				a.name(), a.value(), a.type(), a.format(), a.collectionFormat(), a.$ref(), a.maximum(), a.minimum(), a.multipleOf(), 
				a.maxLength(), a.minLength(), a.exclusiveMaximum(), a.exclusiveMinimum(), a.uniqueItems()
			)
			&& empty(a.items());
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
			empty(
				a.value(), a.description(), a._default(), a._enum(), a.allOf(), a.properties(), a.additionalProperties(), a.xml(), a.example(), a.examples()
			)
			&& empty(
				a.$ref(), a.format(), a.title(), a.multipleOf(), a.maximum(), a.exclusiveMaximum(), a.minimum(), a.exclusiveMinimum(), a.maxLength(), 
				a.minLength(), a.pattern(), a.maxItems(), a.minItems(), a.uniqueItems(), a.maxProperties(), a.minProperties(), a.required(),
				a.type(), a.discriminator(), a.readOnly()
			)
			&& ! a.ignore()
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
			empty(a.value(), a.description()) 
			&& empty(a.url());
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
			empty(a.description(), a.example(), a.examples(), a.api()) 
			&& empty(a.required())
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
			empty(a.value())
			&& empty(a.name(), a.url(), a.email());
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
			empty(a.value())
			&& empty(a.name(), a.url());
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
			empty(a.value(), a._default(), a._enum())
			&& empty(
				a.type(), a.format(), a.collectionFormat(), a.pattern(), a.$ref(), a.maximum(), a.minimum(), a.multipleOf(), 
				a.maxLength(), a.minLength(), a.maxItems(), a.minItems(), a.exclusiveMaximum(), a.exclusiveMinimum(), a.uniqueItems()
			);
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
			empty(a.description(), a._enum(), a.example(), a.api())
			&& empty(
				a.name(), a.value(), a.type(), a.format(), a.pattern(), a.maximum(), a.minimum(), a.multipleOf(), a.maxLength(), 
				a.minLength(), a.allowEmptyValue(), a.exclusiveMaximum(), a.exclusiveMinimum()
			)
			&& empty(a.schema());
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation contains all default values.
	 * 
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if the specified annotation contains all default values.
	 */
	public static boolean empty(ResourceSwagger a) {
		if (a == null)
			return true;
		return 
			empty(a.version())
			&& empty(a.title(), a.description(), a.value())
			&& empty(a.contact())
			&& empty(a.license())
			&& empty(a.externalDocs())
			&& a.tags().length == 0;
	}

	private static boolean empty(String...strings) {
		for (String s : strings)
			if (! s.isEmpty())
				return false;
		return true;
	}

	private static boolean empty(String[]...strings) {
		for (String[] s : strings)
			if (s.length != 0)
				return false;
		return true;
	}
}
