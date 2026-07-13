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
package org.apache.juneau.marshall.parquet;

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.marshall.parquet.ParquetSchemaElement.*;

import java.time.*;
import java.util.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.serializer.*;

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

	private final MarshallingContext marshallingContext;
	private final boolean writeDatesAsTimestamp;
	private final ParquetCycleHandling cycleHandling;
	private final int maxRecursionDepth;
	private final boolean nativeLogicalTypes;

	/** Fixed scale/precision for INT64-backed native DECIMAL emission (GAP-9). 9 fractional digits, 18 total. */
	static final int DECIMAL_SCALE = 9;
	static final int DECIMAL_PRECISION = 18;

	/**
	 * Constructor.
	 *
	 * @param marshallingContext The bean context for type resolution.
	 * @param writeDatesAsTimestamp If true, Date/Calendar/Temporal map to INT64+TIMESTAMP_MILLIS; else to BYTE_ARRAY+STRING.
	 * @param cycleHandling How to handle structural cycles when building schema.
	 */
	public ParquetSchemaBuilder(MarshallingContext marshallingContext, boolean writeDatesAsTimestamp, ParquetCycleHandling cycleHandling) {
		this(marshallingContext, writeDatesAsTimestamp, cycleHandling, 5, false);
	}

	/**
	 * Constructor.
	 *
	 * @param marshallingContext The bean context for type resolution.
	 * @param writeDatesAsTimestamp If true, Date/Calendar/Temporal map to INT64+TIMESTAMP_MILLIS; else to BYTE_ARRAY+STRING.
	 * @param cycleHandling How to handle structural cycles when building schema.
	 * @param maxRecursionDepth Maximum expansion depth for self-referential bean types (GAP-13).
	 */
	public ParquetSchemaBuilder(MarshallingContext marshallingContext, boolean writeDatesAsTimestamp, ParquetCycleHandling cycleHandling, int maxRecursionDepth) {
		this(marshallingContext, writeDatesAsTimestamp, cycleHandling, maxRecursionDepth, false);
	}

	/**
	 * Constructor.
	 *
	 * @param marshallingContext The bean context for type resolution.
	 * @param writeDatesAsTimestamp If true, Date/Calendar/Temporal map to INT64+TIMESTAMP_MILLIS; else to BYTE_ARRAY+STRING.
	 * @param cycleHandling How to handle structural cycles when building schema.
	 * @param maxRecursionDepth Maximum expansion depth for self-referential bean types (GAP-13).
	 * @param nativeLogicalTypes If true, emit binary-native DECIMAL/DATE/TIME/TIMESTAMP-micros logical types
	 * 	instead of the default string / TIMESTAMP-millis normalization (GAP-9/10).
	 */
	public ParquetSchemaBuilder(MarshallingContext marshallingContext, boolean writeDatesAsTimestamp, ParquetCycleHandling cycleHandling, int maxRecursionDepth, boolean nativeLogicalTypes) {
		this.marshallingContext = marshallingContext;
		this.writeDatesAsTimestamp = writeDatesAsTimestamp;
		this.cycleHandling = cycleHandling != null ? cycleHandling : ParquetCycleHandling.NULL;
		this.maxRecursionDepth = Math.max(1, maxRecursionDepth);
		this.nativeLogicalTypes = nativeLogicalTypes;
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
		var typesInProgress = new HashMap<Class<?>, Integer>();
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
		var typesInProgress = new HashMap<Class<?>, Integer>();
		for (var key : keys) {
			var val = map.get(key);
			var cm = marshallingContext.getClassMeta(val != null ? val.getClass() : Object.class);
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
		var typesInProgress = new HashMap<Class<?>, Integer>();
		var keyCm = marshallingContext.getClassMeta(keySample != null ? keySample.getClass() : Object.class);
		var valueCm = marshallingContext.getClassMeta(valueSample != null ? valueSample.getClass() : Object.class);
		elements.add(new ParquetSchemaElement("root", null, null, null, 2, null, null, null, null, null));
		addSchemaElements(elements, keyCm, "key", "root", false, null, typesInProgress);
		addSchemaElements(elements, valueCm, "value", "root", false, null, typesInProgress);
		return elements;
	}

	private void addSchemaElements(List<ParquetSchemaElement> elements, ClassMeta<?> cm, String name, String parentPath, boolean isRoot, Object sampleBean, Map<Class<?>, Integer> typesInProgress) {
		if (cm.isOptional()) {
			addOptionalSchema(elements, cm, name, parentPath, isRoot, sampleBean, typesInProgress);
		} else if (cm.isBean()) {
			addBeanSchema(elements, cm, name, parentPath, isRoot, sampleBean, typesInProgress);
		} else if (cm.isByteArray()) {
			addLeafSchema(elements, cm, name, parentPath, isRoot);
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
	private void addOptionalSchema(List<ParquetSchemaElement> elements, ClassMeta<?> cm, String name, String parentPath, boolean isRoot, Object sampleBean, Map<Class<?>, Integer> typesInProgress) {
		var et = cm.getElementType();
		if (et == null) // HTT: ClassMeta always resolves Optional's type parameter to Object at minimum
			et = marshallingContext.getClassMeta(Object.class);
		Object innerSample = sampleBean instanceof Optional<?> o ? o.orElse(null) : sampleBean;
		var optPath = parentPath != null ? parentPath + "." + name : name;
		elements.add(new ParquetSchemaElement(name, null, null, isRoot ? null : OPTIONAL, 1, null, null, null, null, optPath));
		addSchemaElements(elements, et, "value", optPath, false, innerSample, typesInProgress);
	}

	@SuppressWarnings({
		"java:S3776" // Cognitive Complexity: bean/cycle/property dispatch is inherently branchy
	})
	private void addBeanSchema(List<ParquetSchemaElement> elements, ClassMeta<?> cm, String name, String parentPath, boolean isRoot, Object sampleBean, Map<Class<?>, Integer> typesInProgress) {
		var bm = cm.getBeanMeta();
		if (bm == null) // HTT: addBeanSchema is only called after isBean() check; getBeanMeta() is non-null
			throw iaex("Class ''{0}'' is not a bean", cm.getName());
		var beanClass = cm.inner();
		// Recursion reached here indirectly (e.g. through Optional/collection of the same type) at the depth
		// limit: keep the legacy String back-reference placeholder so the enclosing group's child count stays
		// consistent.  Direct bean-property recursion is handled by the per-property filter below (truncated
		// by omission, which round-trips as null).
		if (typesInProgress.getOrDefault(beanClass, 0) >= maxRecursionDepth) {
			if (cycleHandling == ParquetCycleHandling.THROW) {
				var path = parentPath != null ? parentPath + "." + name : name; // HTT: parentPath is always non-null when recursion limit is reached at depth ≥ 2
				throw new SerializeException("Cyclic type reference at ''{0}'' (type ''{1}''). Use @ParentProperty to exclude back-references or set cycleHandling(NULL).", path, beanClass.getName());
			}
			addLeafSchema(elements, marshallingContext.getClassMeta(String.class), name, parentPath, isRoot);
			return;
		}
		typesInProgress.merge(beanClass, 1, Integer::sum);
		try {
			var ap = marshallingContext.getAnnotationProvider();
			var props = bm.getProperties().values().stream()
				.filter(BeanPropertyMeta::canRead)
				.filter(p -> !isParentProperty(ap, p))
				.toList();
			var childParentPath = parentPath != null ? parentPath + "." + name : name;
			// Resolve each property (incl. Object->sample inference) and drop direct recursive bean properties
			// that would exceed the depth limit (GAP-13).  Omitting them keeps the schema acyclic; on read the
			// missing column reconstructs as null (documented-lossy beyond the limit) instead of a toString.
			var entries = new ArrayList<ResolvedProp>();
			for (var p : props) {
				Object childSample = null;
				if (sampleBean instanceof Map<?, ?> m)
					childSample = m.get(p.getName());
				ClassMeta<?> propCm = (ClassMeta<?>) p.getBeanInfo();
				// Native logical types (GAP-9/10): a java.time temporal property carries a swap, so its
				// resolved ClassMeta is an Object/String surrogate that hides the concrete type.  Recover the
				// declared field/getter type so the DATE/TIME/TIMESTAMP-micros leaf branches can fire.  Only
				// overrides when the declared type is one of the native logical types we emit.
				if (nativeLogicalTypes) {
					var declared = declaredNativeType(p);
					if (declared != null)
						propCm = marshallingContext.getClassMeta(declared);
				}
				// When property type is Object, infer from sample so Map/Collection get proper schema (2.2)
				if ((propCm == null || propCm.isObject()) && childSample != null) // HTT: propCm is non-null (getBeanInfo always non-null for bean properties)
					propCm = marshallingContext.getClassMeta(childSample.getClass());
				// When property type is the abstract java.lang.Number, re-type from a concrete numeric sample so
				// the column uses the actual physical width (INT32/INT64/DOUBLE) and fractional values survive
				// (GAP-12).  Only fires when the sample is itself a Number — a swapped property whose raw sample
				// is non-numeric stays on the lossless INT64 default.
				else if (propCm != null && propCm.is(Number.class) && childSample instanceof Number) // HTT: propCm is non-null (same invariant as above)
					propCm = marshallingContext.getClassMeta(childSample.getClass());
				if (propCm != null && propCm.isBean() && typesInProgress.getOrDefault(propCm.inner(), 0) >= maxRecursionDepth) { // HTT: propCm is non-null
					if (cycleHandling == ParquetCycleHandling.THROW)
						throw new SerializeException("Cyclic type reference at ''{0}.{1}'' (type ''{2}''). Use @ParentProperty to exclude back-references or set cycleHandling(NULL).", childParentPath, p.getName(), propCm.inner().getName());
					continue;
				}
				entries.add(new ResolvedProp(p.getName(), propCm, childSample));
			}
			elements.add(new ParquetSchemaElement(
				name,
				null,
				null,
				isRoot ? null : OPTIONAL,
				entries.size(),
				null,
				null,
				null,
				null,
				childParentPath));
			for (var e : entries)
				addSchemaElements(elements, e.cm, e.name, childParentPath, false, e.sample, typesInProgress);
		} finally {
			typesInProgress.merge(beanClass, -1, (a, b) -> a + b == 0 ? null : a + b);
		}
	}

	private record ResolvedProp(String name, ClassMeta<?> cm, Object sample) {}

	/**
	 * Returns the property's declared field/getter class when it is a binary-native logical type
	 * (java.time temporal, {@link java.math.BigDecimal}, or {@link java.math.BigInteger}); otherwise
	 * <jk>null</jk>.  Used to look past a registered swap whose resolved ClassMeta hides the concrete
	 * type, so the native logical-type leaf branches in {@link #addLeafSchema} can fire (GAP-9/10).
	 */
	private static Class<?> declaredNativeType(BeanPropertyMeta p) {
		// Resolve the declared class from the field (preferred) or, for getter-only properties, the getter.
		var f = p.getField();
		Class<?> c = f != null ? f.getFieldType().inner() : p.getGetter().getReturnType().inner();
		if (c == LocalDate.class || c == LocalTime.class || c == OffsetTime.class
			|| c == java.math.BigDecimal.class || c == java.math.BigInteger.class
			|| java.util.Date.class.isAssignableFrom(c) || java.util.Calendar.class.isAssignableFrom(c)
			|| java.time.temporal.Temporal.class.isAssignableFrom(c))
			return c;
		return null;
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

	private void addListSchema(List<ParquetSchemaElement> elements, ClassMeta<?> cm, String name, String parentPath, boolean isRoot, Object sampleBean, Map<Class<?>, Integer> typesInProgress) {
		var et = cm.getElementType();
		if (et == null) // HTT: ClassMeta always provides an element type (at least Object) for list types
			throw iaex("List element type cannot be determined for ''{0}''", cm.getName());
		// Resolve element type from sample when generics are erased (et is Object) for proper list-of-bean
		// expansion into leaf columns (e.g. members.list.element.name, members.list.element.age)
		var sampleCollection = extractSampleCollection(sampleBean);
		if (ine(sampleCollection)) {
			var first = sampleCollection.iterator().next();
			if (first != null)
				et = marshallingContext.getClassMeta(first.getClass());
		}
		var listPath = parentPath != null ? parentPath + "." + name : name;
		elements.add(new ParquetSchemaElement(name, null, null, isRoot ? null : OPTIONAL, 1, CONVERTED_LIST, null, null, null, null));
		elements.add(new ParquetSchemaElement("list", null, null, REPEATED, 1, null, null, null, null, null));
		Object elementSample = null;
		if (ine(sampleCollection))
			elementSample = sampleCollection.iterator().next();
		addSchemaElements(elements, et, "element", listPath + ".list", false, elementSample, typesInProgress);
	}

	private static Collection<?> extractSampleCollection(Object sampleBean) {
		if (sampleBean instanceof Collection<?> c)
			return c;
		return Collections.emptyList();
	}

	private void addMapSchema(List<ParquetSchemaElement> elements, ClassMeta<?> cm, String name, String parentPath, boolean isRoot, Map<Class<?>, Integer> typesInProgress) {
		var vt = cm.getValueType();
		// Parquet MAP stores keys as STRING; non-String keys (e.g. Enum) are serialized via toString/name()
		if (vt == null) // HTT: ClassMeta always provides a value type (at least Object) for map types
			vt = marshallingContext.getClassMeta(Object.class);
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
			Integer ct;
			if (cm.is(byte.class) || cm.is(Byte.class))
				ct = CONVERTED_INT_8;
			else if (cm.is(short.class) || cm.is(Short.class))
				ct = CONVERTED_INT_16;
			else  // int/Integer — the only remaining type the enclosing guard admits.
				ct = CONVERTED_INT_32;
			elements.add(new ParquetSchemaElement(name, TYPE_INT32, null, repetition, null, ct, null, null, null, path));
		} else if (cm.is(Number.class)) {
			// A statically-typed java.lang.Number leaf has no fixed physical width.  Mapping it to INT32 silently
			// truncates values above 2^31 (e.g. 5_000_000_000L -> 705032704) — GAP-12.  Default to INT64, which
			// is lossless for every integral Number and keeps the wire value integral.  Integral matters for
			// round-trip correctness: an ObjectSwap whose swap type is exactly java.lang.Number (e.g.
			// enum-as-ordinal) round-trips through String.valueOf(...), and "0" parses where "0.0" would not.
			// When a concrete runtime sample is available (addBeanSchema), the column is re-typed to the actual
			// subtype (INT32/INT64/DOUBLE) so fractional values are preserved too.  BigInteger/BigDecimal have
			// their own lossless string mappings and never reach this branch.
			elements.add(new ParquetSchemaElement(name, TYPE_INT64, null, repetition, null, CONVERTED_INT_64, null, null, null, path));
		} else if (cm.is(long.class) || cm.is(Long.class)) {
			elements.add(new ParquetSchemaElement(name, TYPE_INT64, null, repetition, null, CONVERTED_INT_64, null, null, null, path));
		} else if (cm.is(float.class) || cm.is(Float.class)) {
			elements.add(new ParquetSchemaElement(name, TYPE_FLOAT, null, repetition, null, null, null, null, null, path));
		} else if (cm.is(double.class) || cm.is(Double.class)) {
			elements.add(new ParquetSchemaElement(name, TYPE_DOUBLE, null, repetition, null, null, null, null, null, path));
		} else if (cm.isAssignableTo(CharSequence.class)) {  // String is a CharSequence, so this covers both.
			elements.add(new ParquetSchemaElement(name, TYPE_BYTE_ARRAY, null, repetition, null, CONVERTED_UTF8, LOGICAL_TYPE_STRING, null, null, path));
		} else if (cm.isEnum()) {
			elements.add(new ParquetSchemaElement(name, TYPE_BYTE_ARRAY, null, repetition, null, CONVERTED_ENUM, null, null, null, path));
		} else if (cm.isByteArray()) {
			elements.add(new ParquetSchemaElement(name, TYPE_BYTE_ARRAY, null, repetition, null, null, null, null, null, path));
		} else if (nativeLogicalTypes && cm.inner() == LocalDate.class) {
			// DATE (GAP-10): days since epoch as INT32.  Match on inner() (the concrete class resolved from a
			// runtime sample) since a swapped property ClassMeta reports neither is(LocalDate) nor isDateOrCalendarOrTemporal.
			elements.add(new ParquetSchemaElement(name, TYPE_INT32, null, repetition, null, CONVERTED_DATE, LOGICAL_TYPE_DATE, null, null, path));
		} else if (nativeLogicalTypes && (cm.inner() == LocalTime.class || cm.inner() == OffsetTime.class)) {
			// TIME (GAP-10): micros since midnight as INT64 (TIME_MICROS).
			elements.add(new ParquetSchemaElement(name, TYPE_INT64, null, repetition, null, CONVERTED_TIME_MICROS, LOGICAL_TYPE_TIME, null, null, path));
		} else if (nativeLogicalTypes && cm.isDateOrCalendarOrTemporal()) {
			// TIMESTAMP (GAP-10): micros since epoch as INT64 (TIMESTAMP_MICROS) — sub-millisecond precise.
			elements.add(new ParquetSchemaElement(name, TYPE_INT64, null, repetition, null, CONVERTED_TIMESTAMP_MICROS, LOGICAL_TYPE_TIMESTAMP, null, null, path));
		} else if (cm.isDateOrCalendarOrTemporal() && writeDatesAsTimestamp) {
			elements.add(new ParquetSchemaElement(name, TYPE_INT64, null, repetition, null, CONVERTED_TIMESTAMP_MILLIS, LOGICAL_TYPE_TIMESTAMP, null, null, path));
		} else if (cm.isDateOrCalendarOrTemporal()) {
			elements.add(new ParquetSchemaElement(name, TYPE_BYTE_ARRAY, null, repetition, null, CONVERTED_UTF8, LOGICAL_TYPE_STRING, null, null, path));
		} else if (cm.isDuration()) {
			elements.add(new ParquetSchemaElement(name, TYPE_BYTE_ARRAY, null, repetition, null, CONVERTED_UTF8, LOGICAL_TYPE_STRING, null, null, path));
		} else if (nativeLogicalTypes && (cm.is(java.math.BigDecimal.class) || cm.is(java.math.BigInteger.class))) {
			// DECIMAL (GAP-9): unscaled value as INT64 with scale/precision in the schema.  Scale is fixed at
			// DECIMAL_SCALE; values requiring more fractional digits are rounded (HALF_UP) on write.
			elements.add(new ParquetSchemaElement(name, TYPE_INT64, null, repetition, null, CONVERTED_DECIMAL, LOGICAL_TYPE_DECIMAL, DECIMAL_SCALE, DECIMAL_PRECISION, path));
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
