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

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.*;
import org.junit.jupiter.api.*;

/**
 * Direct unit tests for {@link ParquetSchemaBuilder} entry points and constructor overloads not reached
 * by the serializer session (which only uses the 5-arg constructor + {@code buildSchema(cm, sample)}).
 */
class ParquetSchemaBuilderDirect_Test extends TestBase {

	private static final MarshallingContext MC = MarshallingContext.DEFAULT;

	public static class Bean { public String name; public int age; }

	@Test
	void a01_threeArgConstructorAndSingleArgBuildSchema() {
		// 3-arg constructor + buildSchema(cm) single-arg overload.
		var b = new ParquetSchemaBuilder(MC, true, ParquetCycleHandling.NULL);
		var schema = b.buildSchema(MC.getClassMeta(Bean.class));
		assertFalse(schema.isEmpty());
	}

	@Test
	void a02_fourArgConstructor() {
		var b = new ParquetSchemaBuilder(MC, true, ParquetCycleHandling.NULL, 5);
		var schema = b.buildSchema(MC.getClassMeta(Bean.class));
		assertFalse(schema.isEmpty());
	}

	@Test
	void a03_nullCycleHandlingDefaultsToNull() {
		// cycleHandling == null falls back to NULL (the false branch of the constructor ternary).
		var b = new ParquetSchemaBuilder(MC, true, null, 5, false);
		assertFalse(b.buildSchema(MC.getClassMeta(Bean.class)).isEmpty());
	}

	@Test
	void a04_buildSchemaFromMapWithNullValue() {
		// A null map value resolves to Object ClassMeta (the val!=null false branch).
		var map = new LinkedHashMap<String,Object>();
		map.put("a", 1);
		map.put("b", null);
		var b = new ParquetSchemaBuilder(MC, true, ParquetCycleHandling.NULL, 5, false);
		var schema = b.buildSchemaFromMap(map);
		assertFalse(schema.isEmpty());
	}

	@Test
	void a05_buildSchemaForKeyValuePairsWithNullSamples() {
		// Null key/value samples resolve to Object ClassMeta (both false branches).
		var b = new ParquetSchemaBuilder(MC, true, ParquetCycleHandling.NULL, 5, false);
		var schema = b.buildSchemaForKeyValuePairs(null, null);
		assertFalse(schema.isEmpty());
		// And with concrete samples (the true branches).
		var schema2 = b.buildSchemaForKeyValuePairs("k", 42);
		assertFalse(schema2.isEmpty());
	}

	public static class Child {
		public String name;
		@ParentProperty public Parent parent;
	}

	public static class Parent {
		public String label;
		public Child child;
	}

	@Test
	void a06_parentPropertyExcludedFromSchema() {
		// @ParentProperty on a field is detected and the back-reference is excluded (isParentProperty field branch).
		var b = new ParquetSchemaBuilder(MC, true, ParquetCycleHandling.NULL, 5, false);
		var schema = b.buildSchema(MC.getClassMeta(Parent.class));
		// 'parent' back-reference under child must be omitted — no schema element path ends with ".parent".
		assertTrue(schema.stream().noneMatch(e -> e.path != null && e.path.endsWith(".parent")));
	}

	public static class GetterParent {
		private GetterParent up;
		public String label;
		@ParentProperty public GetterParent getUp() { return up; }
		public void setUp(GetterParent v) { up = v; }
		public GetterParent getChild() { return null; }
	}

	@Test
	void a07_parentPropertyOnGetter() {
		// @ParentProperty on a getter is detected via the getter branch of isParentProperty.
		var b = new ParquetSchemaBuilder(MC, true, ParquetCycleHandling.NULL, 5, false);
		var schema = b.buildSchema(MC.getClassMeta(GetterParent.class));
		assertTrue(schema.stream().noneMatch(e -> e.path != null && e.path.endsWith(".up")));
	}

	public static class SetterParent {
		private SetterParent up;
		public String label;
		public SetterParent getUp() { return up; }
		@ParentProperty public void setUp(SetterParent v) { up = v; }
	}

	@Test
	void a08_parentPropertyOnSetter() {
		// @ParentProperty on a setter is detected via the setter branch of isParentProperty.
		var b = new ParquetSchemaBuilder(MC, true, ParquetCycleHandling.NULL, 5, false);
		var schema = b.buildSchema(MC.getClassMeta(SetterParent.class));
		assertTrue(schema.stream().noneMatch(e -> e.path != null && e.path.endsWith(".up")));
	}

	@Test
	void a09_rawOptionalElementType() {
		// A raw Optional (no type parameter) resolves its element type to Object (addOptionalSchema et-null branch).
		var b = new ParquetSchemaBuilder(MC, true, ParquetCycleHandling.NULL, 5, false);
		var schema = b.buildSchema(MC.getClassMeta(Optional.class));
		assertFalse(schema.isEmpty());
	}

	@Test
	void a10_rawMapValueType() {
		// A raw Map (no value type parameter) resolves its value type to Object (addMapSchema vt-null branch).
		var b = new ParquetSchemaBuilder(MC, true, ParquetCycleHandling.NULL, 5, false);
		var schema = b.buildSchema(MC.getClassMeta(Map.class));
		assertFalse(schema.isEmpty());
	}

	// Self-referential through a COLLECTION (not a direct property), so the type-level recursion check in
	// addBeanSchema (not the per-property filter) governs expansion.
	public static class Tree {
		public String label;
		public List<Tree> children;
	}

	@Test
	void a11_typeLevelRecursionPlaceholderNullMode() {
		// NULL cycle handling: at the depth limit the recursive bean becomes a String placeholder.
		var b = new ParquetSchemaBuilder(MC, true, ParquetCycleHandling.NULL, 2, false);
		var schema = b.buildSchema(MC.getClassMeta(Tree.class), null);
		assertFalse(schema.isEmpty());
	}

	@Test
	void a12_typeLevelRecursionThrowMode() {
		// THROW cycle handling: the same depth-limit hit raises a SerializeException (addBeanSchema THROW branch).
		var b = new ParquetSchemaBuilder(MC, true, ParquetCycleHandling.THROW, 2, false);
		assertThrows(Exception.class, () -> b.buildSchema(MC.getClassMeta(Tree.class), null));
	}

	public static class NativeBean {
		public java.time.LocalDate date;
		public java.time.LocalTime time;
		public java.time.OffsetTime offsetTime;
		public java.time.Instant ts;
		public java.math.BigDecimal dec;
		public java.math.BigInteger bigint;
	}

	@Test
	void a13_nativeLogicalTypeLeafBranches() {
		// nativeLogicalTypes=true drives the DATE / TIME / TIMESTAMP-micros / DECIMAL leaf-schema branches.
		// A sample bean is required so the temporal property types (which carry a java.time swap and resolve
		// to Object on the declared ClassMeta) are re-typed to their concrete classes.
		var sample = new NativeBean();
		sample.date = java.time.LocalDate.parse("2026-06-17");
		sample.time = java.time.LocalTime.parse("01:02:03");
		sample.offsetTime = java.time.OffsetTime.parse("01:02:03+00:00");
		sample.ts = java.time.Instant.parse("2026-06-17T00:00:00Z");
		sample.dec = new java.math.BigDecimal("1.5");
		sample.bigint = java.math.BigInteger.TEN;
		var b = new ParquetSchemaBuilder(MC, true, ParquetCycleHandling.NULL, 5, true);
		var schema = b.buildSchema(MC.getClassMeta(NativeBean.class), sample);
		// The native branches produce DATE (CONVERTED_DATE), TIME_MICROS, TIMESTAMP_MICROS, and DECIMAL columns.
		assertTrue(schema.stream().anyMatch(e -> e.convertedType != null && e.convertedType == ParquetSchemaElement.CONVERTED_DATE));
		assertTrue(schema.stream().anyMatch(e -> e.convertedType != null && e.convertedType == ParquetSchemaElement.CONVERTED_DECIMAL));
		assertTrue(schema.stream().anyMatch(e -> e.convertedType != null && e.convertedType == ParquetSchemaElement.CONVERTED_TIME_MICROS));
	}
}
