/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.parquet;

import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.parquet.ParquetSchemaElement.*;

import org.apache.juneau.serializer.SerializeException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.juneau.BeanContext;
import org.apache.juneau.BeanPropertyMeta;
import org.apache.juneau.ClassMeta;
import org.apache.juneau.annotation.ParentProperty;
import org.apache.juneau.commons.reflect.AnnotationProvider;

/**
 * Builds Parquet schema (list of SchemaElement) from Juneau bean ClassMeta.
 *
 * <p>
 * Maps Java types to Parquet physical and logical types per the
 * <a class="doclink" href="https://parquet.apache.org/docs/file-format/">Parquet specification</a>.
 */
@SuppressWarnings({
	"java:S1192" // Duplicated "value" is Parquet Optional key; constant would obscure schema mapping
})
public final class ParquetSchemaBuilder {

	private final BeanContext beanContext;
	private final boolean writeDatesAsTimestamp;
	private final ParquetCycleHandling cycleHandling;

	/**
	 * Constructor.
	 *
	 * @param beanContext The bean context for type resolution.
	 * @param writeDatesAsTimestamp If true, Date/Calendar/Temporal map to INT64+TIMESTAMP_MILLIS; else to BYTE_ARRAY+STRING.
	 * @param cycleHandling How to handle structural cycles when building schema.
	 */
	public ParquetSchemaBuilder(BeanContext beanContext, boolean writeDatesAsTimestamp, ParquetCycleHandling cycleHandling) {
		this.beanContext = beanContext;
		this.writeDatesAsTimestamp = writeDatesAsTimestamp;
		this.cycleHandling = cycleHandling != null ? cycleHandling : ParquetCycleHandling.NULL;
	}

	/**
	 * Builds the Parquet schema for the given bean class.
	 *
	 * @param cm The bean class metadata.
	 * @return The depth-first list of schema elements (root first).
	 */
	public List<ParquetSchemaElement> buildSchema(ClassMeta<?> cm) {
		return buildSchema(cm, null);
	}

	/**
	 * Builds the Parquet schema, using a sample bean to resolve collection element types when generics are erased.
	 *
	 * @param cm The bean class metadata.
	 * @param sampleBean Optional sample instance; when provided, collection element types are resolved from actual values.
	 * @return The depth-first list of schema elements (root first).
	 */
	public List<ParquetSchemaElement> buildSchema(ClassMeta<?> cm, Object sampleBean) {
		var elements = new ArrayList<ParquetSchemaElement>();
		var typesInProgress = new HashSet<Class<?>>();
		addSchemaElements(elements, cm, "root", null, true, sampleBean, typesInProgress);
		return elements;
	}

	/**
	 * Builds the Parquet schema for a Map root (single row, columns from map keys).
	 *
	 * @param map The map instance (keys become column names).
	 * @return The depth-first list of schema elements (root first).
	 */
	public List<ParquetSchemaElement> buildSchemaFromMap(Map<?, ?> map) {
		var elements = new ArrayList<ParquetSchemaElement>();
		var keys = map.keySet().stream()
			.filter(String.class::isInstance)
			.map(k -> (String) k)
			.sorted()
			.toList();
		elements.add(new ParquetSchemaElement(
			"root",
			null,
			null,
			null,
			keys.size(),
			null,
			null,
			null,
			null,
			null));
		var typesInProgress = new HashSet<Class<?>>();
		for (var key : keys) {
			var val = map.get(key);
			var cm = beanContext.getClassMeta(val != null ? val.getClass() : Object.class);
			addSchemaElements(elements, cm, key, "root", false, null, typesInProgress);
		}
		return elements;
	}

	/**
	 * Builds the Parquet schema for a Map root with non-String keys (flat list-of-pairs: root.key, root.value).
	 *
	 * <p>
	 * Each map entry becomes one Parquet row. Keys and values use native Parquet types.
	 *
	 * @param keySample Sample key for type inference.
	 * @param valueSample Sample value for type inference.
	 * @return The depth-first list of schema elements (root first).
	 */
	public List<ParquetSchemaElement> buildSchemaForKeyValuePairs(Object keySample, Object valueSample) {
		var elements = new ArrayList<ParquetSchemaElement>();
		var typesInProgress = new HashSet<Class<?>>();
		var keyCm = beanContext.getClassMeta(keySample != null ? keySample.getClass() : Object.class);
		var valueCm = beanContext.getClassMeta(valueSample != null ? valueSample.getClass() : Object.class);
		elements.add(new ParquetSchemaElement("root", null, null, null, 2, null, null, null, null, null));
		addSchemaElements(elements, keyCm, "key", "root", false, null, typesInProgress);
		addSchemaElements(elements, valueCm, "value", "root", false, null, typesInProgress);
		return elements;
	}

	private void addSchemaElements(List<ParquetSchemaElement> elements, ClassMeta<?> cm, String name, String parentPath, boolean isRoot, Object sampleBean, Set<Class<?>> typesInProgress) {
		if (cm.isOptional()) {
			addOptionalSchema(elements, cm, name, parentPath, isRoot, sampleBean, typesInProgress);
		} else if (cm.isBean()) {
			addBeanSchema(elements, cm, name, parentPath, isRoot, sampleBean, typesInProgress);
		} else if (cm.isCollection() || cm.isArray()) {
			addListSchema(elements, cm, name, parentPath, isRoot, sampleBean, typesInProgress);
		} else if (cm.isMap()) {
			addMapSchema(elements, cm, name, parentPath, isRoot, typesInProgress);
		} else {
			addLeafSchema(elements, cm, name, parentPath, isRoot);
		}
	}

	/** Optional&lt;X&gt; → optional group with "value" child of type X (2.2). */
	@SuppressWarnings({
		"java:S3776" // Cognitive Complexity: optional/value dispatch is inherently branchy
	})
	private void addOptionalSchema(List<ParquetSchemaElement> elements, ClassMeta<?> cm, String name, String parentPath, boolean isRoot, Object sampleBean, Set<Class<?>> typesInProgress) {
		var et = cm.getElementType();
		if (et == null)
			et = beanContext.getClassMeta(Object.class);
		Object innerSample = sampleBean instanceof Optional<?> o ? o.orElse(null) : sampleBean;
		var optPath = parentPath != null ? parentPath + "." + name : name;
		elements.add(new ParquetSchemaElement(name, null, null, isRoot ? null : OPTIONAL, 1, null, null, null, null, optPath));
		addSchemaElements(elements, et, "value", optPath, false, innerSample, typesInProgress);
	}

	@SuppressWarnings({
		"java:S3776" // Cognitive Complexity: bean/cycle/property dispatch is inherently branchy
	})
	private void addBeanSchema(List<ParquetSchemaElement> elements, ClassMeta<?> cm, String name, String parentPath, boolean isRoot, Object sampleBean, Set<Class<?>> typesInProgress) {
		var bm = cm.getBeanMeta();
		if (bm == null)
			throw illegalArg("Class ''{0}'' is not a bean", cm.getName());
		var beanClass = cm.inner();
		if (typesInProgress.contains(beanClass)) {
			if (cycleHandling == ParquetCycleHandling.THROW) {
				var path = parentPath != null ? parentPath + "." + name : name;
				throw new SerializeException("Cyclic type reference at ''{0}'' (type ''{1}''). Use @ParentProperty to exclude back-references or set cycleHandling(NULL).", path, beanClass.getName());
			}
			addLeafSchema(elements, beanContext.getClassMeta(String.class), name, parentPath, isRoot);
			return;
		}
		typesInProgress.add(beanClass);
		try {
			var ap = beanContext.getAnnotationProvider();
			var props = bm.getProperties().values().stream()
				.filter(BeanPropertyMeta::canRead)
				.filter(p -> !isParentProperty(ap, p))
				.toList();
			int numChildren = props.size();
			elements.add(new ParquetSchemaElement(
				name,
				null,
				null,
				isRoot ? null : OPTIONAL,
				numChildren,
				null,
				null,
				null,
				null,
				parentPath != null ? parentPath + "." + name : name));
			for (var p : props) {
				var childParentPath = parentPath != null ? parentPath + "." + name : name;
				Object childSample = null;
				if (sampleBean != null && sampleBean instanceof Map<?, ?> m)
					childSample = m.get(p.getName());
				var propCm = p.getClassMeta();
				// When property type is Object, infer from sample so Map/Collection get proper schema (2.2)
				if ((propCm == null || propCm.isObject()) && childSample != null)
					propCm = beanContext.getClassMeta(childSample.getClass());
				addSchemaElements(elements, propCm, p.getName(), childParentPath, false, childSample, typesInProgress);
			}
		} finally {
			typesInProgress.remove(beanClass);
		}
	}

	private static boolean isParentProperty(AnnotationProvider ap, BeanPropertyMeta p) {
		var g = p.getGetter();
		if (g != null && ap.has(ParentProperty.class, g))
			return true;
		var s = p.getSetter();
		if (s != null && ap.has(ParentProperty.class, s))
			return true;
		var f = p.getField();
		return f != null && ap.has(ParentProperty.class, f);
	}

	private void addListSchema(List<ParquetSchemaElement> elements, ClassMeta<?> cm, String name, String parentPath, boolean isRoot, Object sampleBean, Set<Class<?>> typesInProgress) {
		var et = cm.getElementType();
		if (et == null)
			throw illegalArg("List element type cannot be determined for ''{0}''", cm.getName());
		// Resolve element type from sample when generics are erased (et is Object) for proper list-of-bean
		// expansion into leaf columns (e.g. members.list.element.name, members.list.element.age)
		var sampleCollection = extractSampleCollection(sampleBean);
		if (sampleCollection != null && !sampleCollection.isEmpty()) {
			var first = sampleCollection.iterator().next();
			if (first != null)
				et = beanContext.getClassMeta(first.getClass());
		}
		var listPath = parentPath != null ? parentPath + "." + name : name;
		elements.add(new ParquetSchemaElement(name, null, null, isRoot ? null : OPTIONAL, 1, CONVERTED_LIST, null, null, null, null));
		elements.add(new ParquetSchemaElement("list", null, null, REPEATED, 1, null, null, null, null, null));
		Object elementSample = null;
		if (sampleCollection != null && !sampleCollection.isEmpty())
			elementSample = sampleCollection.iterator().next();
		addSchemaElements(elements, et, "element", listPath + ".list", false, elementSample, typesInProgress);
	}

	private static Collection<?> extractSampleCollection(Object sampleBean) {
		if (sampleBean instanceof Collection<?> c)
			return c;
		return Collections.emptyList();
	}

	private void addMapSchema(List<ParquetSchemaElement> elements, ClassMeta<?> cm, String name, String parentPath, boolean isRoot, Set<Class<?>> typesInProgress) {
		var vt = cm.getValueType();
		// Parquet MAP stores keys as STRING; non-String keys (e.g. Enum) are serialized via toString/name()
		if (vt == null)
			vt = beanContext.getClassMeta(Object.class);
		var mapPath = parentPath != null ? parentPath + "." + name : name;
		elements.add(new ParquetSchemaElement(name, null, null, isRoot ? null : OPTIONAL, 1, CONVERTED_MAP, null, null, null, null));
		elements.add(new ParquetSchemaElement("key_value", null, null, REPEATED, 2, null, null, null, null, null));
		elements.add(new ParquetSchemaElement("key", TYPE_BYTE_ARRAY, null, REQUIRED, null, CONVERTED_UTF8, LOGICAL_TYPE_STRING, null, null, mapPath + ".key_value.key"));
		addSchemaElements(elements, vt, "value", mapPath + ".key_value", false, null, typesInProgress);
	}

	@SuppressWarnings({
		"java:S3776" // Cognitive Complexity: type-to-Parquet mapping requires many branches
	})
	private void addLeafSchema(List<ParquetSchemaElement> elements, ClassMeta<?> cm, String name, String parentPath, boolean isRoot) {
		int repetition = isRoot ? REQUIRED : OPTIONAL;
		String path = parentPath != null ? parentPath + "." + name : name;

		if (cm.isBoolean()) {
			elements.add(new ParquetSchemaElement(name, TYPE_BOOLEAN, null, repetition, null, null, null, null, null, path));
		} else if (cm.is(byte.class) || cm.is(Byte.class) || cm.is(short.class) || cm.is(Short.class) || cm.is(int.class) || cm.is(Integer.class)) {
			Integer ct = null;
			if (cm.is(byte.class) || cm.is(Byte.class))
				ct = CONVERTED_INT_8;
			else if (cm.is(short.class) || cm.is(Short.class))
				ct = CONVERTED_INT_16;
			else if (cm.is(int.class) || cm.is(Integer.class))
				ct = CONVERTED_INT_32;
			elements.add(new ParquetSchemaElement(name, TYPE_INT32, null, repetition, null, ct, null, null, null, path));
		} else if (cm.is(long.class) || cm.is(Long.class)) {
			elements.add(new ParquetSchemaElement(name, TYPE_INT64, null, repetition, null, CONVERTED_INT_64, null, null, null, path));
		} else if (cm.is(float.class) || cm.is(Float.class)) {
			elements.add(new ParquetSchemaElement(name, TYPE_FLOAT, null, repetition, null, null, null, null, null, path));
		} else if (cm.is(double.class) || cm.is(Double.class)) {
			elements.add(new ParquetSchemaElement(name, TYPE_DOUBLE, null, repetition, null, null, null, null, null, path));
		} else if (cm.isAssignableTo(CharSequence.class) || cm.isAssignableTo(String.class)) {
			elements.add(new ParquetSchemaElement(name, TYPE_BYTE_ARRAY, null, repetition, null, CONVERTED_UTF8, LOGICAL_TYPE_STRING, null, null, path));
		} else if (cm.isEnum()) {
			elements.add(new ParquetSchemaElement(name, TYPE_BYTE_ARRAY, null, repetition, null, CONVERTED_ENUM, null, null, null, path));
		} else if (cm.isByteArray()) {
			elements.add(new ParquetSchemaElement(name, TYPE_BYTE_ARRAY, null, repetition, null, null, null, null, null, path));
		} else if (cm.isDateOrCalendarOrTemporal() && writeDatesAsTimestamp) {
			elements.add(new ParquetSchemaElement(name, TYPE_INT64, null, repetition, null, CONVERTED_TIMESTAMP_MILLIS, LOGICAL_TYPE_TIMESTAMP, null, null, path));
		} else if (cm.isDateOrCalendarOrTemporal()) {
			elements.add(new ParquetSchemaElement(name, TYPE_BYTE_ARRAY, null, repetition, null, CONVERTED_UTF8, LOGICAL_TYPE_STRING, null, null, path));
		} else if (cm.isDuration()) {
			elements.add(new ParquetSchemaElement(name, TYPE_BYTE_ARRAY, null, repetition, null, CONVERTED_UTF8, LOGICAL_TYPE_STRING, null, null, path));
		} else if (cm.isDecimal()) {
			// Store as UTF-8 string to match JSON/CBOR behavior; DECIMAL binary encoding is not used since
			// the reader always reads BYTE_ARRAY as a UTF-8 string for type conversion via BeanSession.
			elements.add(new ParquetSchemaElement(name, TYPE_BYTE_ARRAY, null, repetition, null, CONVERTED_UTF8, LOGICAL_TYPE_STRING, null, null, path));
		} else if (cm.isAssignableTo(UUID.class)) {
			elements.add(new ParquetSchemaElement(name, TYPE_FIXED_LEN_BYTE_ARRAY, 16, repetition, null, null, LOGICAL_TYPE_UUID, null, null, path));
		} else {
			elements.add(new ParquetSchemaElement(name, TYPE_BYTE_ARRAY, null, repetition, null, CONVERTED_UTF8, LOGICAL_TYPE_STRING, null, null, path));
		}
	}

	/**
	 * Returns leaf (primitive) schema elements suitable for column chunk writing.
	 *
	 * @param schema The full schema from buildSchema.
	 * @return List of leaf elements with non-null paths.
	 */
	public static List<ParquetSchemaElement> getLeafColumns(List<ParquetSchemaElement> schema) {
		var leaves = new ArrayList<ParquetSchemaElement>();
		for (var e : schema)
			if (e.isLeaf())
				leaves.add(e);
		return leaves;
	}
}
