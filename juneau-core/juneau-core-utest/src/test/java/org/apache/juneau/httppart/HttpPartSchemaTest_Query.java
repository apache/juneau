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
package org.apache.juneau.httppart;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.apache.juneau.testutils.TestUtils.*;
import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.StringUtils.*;

import org.apache.juneau.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.jsonschema.annotation.Items;
import org.apache.juneau.jsonschema.annotation.SubItems;
import org.apache.juneau.reflect.*;
import org.apache.juneau.utils.*;
import org.junit.*;
import org.junit.runners.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HttpPartSchemaTest_Query {

	//-----------------------------------------------------------------------------------------------------------------
	// Basic test
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void testBasic() throws Exception {
		HttpPartSchema.create().build();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Query
	//-----------------------------------------------------------------------------------------------------------------

	@Query("x")
	public static class A01 {}

	@Test
	public void a01_value() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().apply(Query.class, A01.class).build();
		assertEquals("x", s.getName());
	}

	@Query(
		name="x",
		type="number",
		format="int32",
		collectionFormat="csv",
		maximum="1",
		minimum="2",
		multipleOf="3",
		pattern="4",
		maxLength=1,
		minLength=2,
		maxItems=3,
		minItems=4,
		exclusiveMaximum=true,
		exclusiveMinimum=true,
		uniqueItems=true,
		required=true,
		skipIfEmpty=true,
		description={"b1","b2"},
		_default={"c1","c2"},
		items=@Items($ref="d1"),
		_enum="e1,e2,e3",
		example="f1",
		api="{g1:true}"
	)
	public static class A02 {}

	@Test
	public void a02_basic_onClass() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().apply(Query.class, A02.class).noValidate().build();
		assertEquals("x", s.getName());
		assertEquals(HttpPartSchema.Type.NUMBER, s.getType());
		assertEquals(HttpPartSchema.Format.INT32, s.getFormat());
		assertEquals(HttpPartSchema.CollectionFormat.CSV, s.getCollectionFormat());
		assertEquals(1, s.getMaximum());
		assertEquals(2, s.getMinimum());
		assertEquals(3, s.getMultipleOf());
		assertEquals("4", s.getPattern().pattern());
		assertEquals(1, s.getMaxLength().longValue());
		assertEquals(2, s.getMinLength().longValue());
		assertEquals(3, s.getMaxItems().longValue());
		assertEquals(4, s.getMinItems().longValue());
		assertTrue(s.isExclusiveMaximum());
		assertTrue(s.isExclusiveMinimum());
		assertTrue(s.isUniqueItems());
		assertTrue(s.isRequired());
		assertTrue(s.isSkipIfEmpty());
		assertObjectEquals("['e1','e2','e3']", s.getEnum());
		assertEquals("c1\nc2", s.getDefault());
	}

	public static class A03 {
		public void a(
				@Query(
					name="x",
					type="number",
					format="int32",
					collectionFormat="csv",
					maximum="1",
					minimum="2",
					multipleOf="3",
					pattern="4",
					maxLength=1,
					minLength=2,
					maxItems=3,
					minItems=4,
					exclusiveMaximum=true,
					exclusiveMinimum=true,
					uniqueItems=true,
					required=true,
					skipIfEmpty=true,
					description={"b1","b2"},
					_default={"c1","c2"},
					items=@Items($ref="d1"),
					_enum="e1,e2,e3",
					example="f1",
					api="{g1:true}"
				) String x
			) {

		}
	}

	@Test
	public void a03_basic_onParameter() throws Exception {
		ParamInfo mpi = getMethodInfo(A03.class.getMethod("a", String.class)).getParam(0);
		HttpPartSchema s = HttpPartSchema.create().apply(Query.class, mpi).noValidate().build();
		assertEquals("x", s.getName());
		assertEquals(HttpPartSchema.Type.NUMBER, s.getType());
		assertEquals(HttpPartSchema.Format.INT32, s.getFormat());
		assertEquals(HttpPartSchema.CollectionFormat.CSV, s.getCollectionFormat());
		assertEquals(1, s.getMaximum());
		assertEquals(2, s.getMinimum());
		assertEquals(3, s.getMultipleOf());
		assertEquals("4", s.getPattern().pattern());
		assertEquals(1, s.getMaxLength().longValue());
		assertEquals(2, s.getMinLength().longValue());
		assertEquals(3, s.getMaxItems().longValue());
		assertEquals(4, s.getMinItems().longValue());
		assertTrue(s.isExclusiveMaximum());
		assertTrue(s.isExclusiveMinimum());
		assertTrue(s.isUniqueItems());
		assertTrue(s.isRequired());
		assertTrue(s.isSkipIfEmpty());
		assertObjectEquals("['e1','e2','e3']", s.getEnum());
		assertEquals("c1\nc2", s.getDefault());
	}

	public static class A04 {
		public void a(
				@Query(
					name="y",
					type="integer",
					format="int64",
					collectionFormat="ssv",
					maximum="5",
					minimum="6",
					multipleOf="7",
					pattern="8",
					maxLength=5,
					minLength=6,
					maxItems=7,
					minItems=8,
					exclusiveMaximum=false,
					exclusiveMinimum=false,
					uniqueItems=false,
					required=false,
					skipIfEmpty=false,
					description={"b3","b3"},
					_default={"c3","c4"},
					items=@Items($ref="d2"),
					_enum="e4,e5,e6",
					example="f2",
					api="{g2:true}"
				) A01 x
			) {

		}
	}

	@Test
	public void a04_basic_onParameterAndClass() throws Exception {
		ParamInfo mpi = getMethodInfo(A04.class.getMethod("a", A01.class)).getParam(0);
		HttpPartSchema s = HttpPartSchema.create().apply(Query.class, mpi).noValidate().build();
		assertEquals("y", s.getName());
		assertEquals(HttpPartSchema.Type.INTEGER, s.getType());
		assertEquals(HttpPartSchema.Format.INT64, s.getFormat());
		assertEquals(HttpPartSchema.CollectionFormat.SSV, s.getCollectionFormat());
		assertEquals(5, s.getMaximum());
		assertEquals(6, s.getMinimum());
		assertEquals(7, s.getMultipleOf());
		assertEquals("8", s.getPattern().pattern());
		assertEquals(5, s.getMaxLength().longValue());
		assertEquals(6, s.getMinLength().longValue());
		assertEquals(7, s.getMaxItems().longValue());
		assertEquals(8, s.getMinItems().longValue());
		assertFalse(s.isExclusiveMaximum());
		assertFalse(s.isExclusiveMinimum());
		assertFalse(s.isUniqueItems());
		assertFalse(s.isRequired());
		assertFalse(s.isSkipIfEmpty());
		assertObjectEquals("['e4','e5','e6']", s.getEnum());
		assertEquals("c3\nc4", s.getDefault());
	}

	@Query(
		name="x",
		items=@Items(
			type="number",
			format="int32",
			collectionFormat="csv",
			maximum="1",
			minimum="2",
			multipleOf="3",
			pattern="4",
			maxLength=1,
			minLength=2,
			maxItems=3,
			minItems=4,
			exclusiveMaximum=true,
			exclusiveMinimum=true,
			uniqueItems=true,
			_default={"c1","c2"},
			_enum="e1,e2",
			items=@SubItems(
				type="integer",
				format="int64",
				collectionFormat="ssv",
				maximum="5",
				minimum="6",
				multipleOf="7",
				pattern="8",
				maxLength=5,
				minLength=6,
				maxItems=7,
				minItems=8,
				exclusiveMaximum=false,
				exclusiveMinimum=false,
				uniqueItems=false,
				_default={"c3","c4"},
				_enum="e3,e4",
				items={
					"type:'string',",
					"format:'float',",
					"collectionFormat:'tsv',",
					"maximum:'9',",
					"minimum:'10',",
					"multipleOf:'11',",
					"pattern:'12',",
					"maxLength:9,",
					"minLength:10,",
					"maxItems:11,",
					"minItems:12,",
					"exclusiveMaximum:true,",
					"exclusiveMinimum:true,",
					"uniqueItems:true,",
					"default:'c5\\nc6',",
					"enum:['e5','e6'],",
					"items:{",
						"type:'array',",
						"format:'double',",
						"collectionFormat:'pipes',",
						"maximum:'13',",
						"minimum:'14',",
						"multipleOf:'15',",
						"pattern:'16',",
						"maxLength:13,",
						"minLength:14,",
						"maxItems:15,",
						"minItems:16,",
						"exclusiveMaximum:false,",
						"exclusiveMinimum:false,",
						"uniqueItems:false,",
						"default:'c7\\nc8',",
						"enum:['e7','e8']",
					"}"
				}
			)
		)
	)
	public static class A05 {}

	@Test
	public void a05_basic_nestedItems_onClass() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().apply(Query.class, A05.class).noValidate().build();
		assertEquals("x", s.getName());

		HttpPartSchema items = s.getItems();
		assertEquals(HttpPartSchema.Type.NUMBER, items.getType());
		assertEquals(HttpPartSchema.Format.INT32, items.getFormat());
		assertEquals(HttpPartSchema.CollectionFormat.CSV, items.getCollectionFormat());
		assertEquals(1, items.getMaximum());
		assertEquals(2, items.getMinimum());
		assertEquals(3, items.getMultipleOf());
		assertEquals("4", items.getPattern().pattern());
		assertEquals(1, items.getMaxLength().longValue());
		assertEquals(2, items.getMinLength().longValue());
		assertEquals(3, items.getMaxItems().longValue());
		assertEquals(4, items.getMinItems().longValue());
		assertTrue(items.isExclusiveMaximum());
		assertTrue(items.isExclusiveMinimum());
		assertTrue(items.isUniqueItems());
		assertObjectEquals("['e1','e2']", items.getEnum());
		assertEquals("c1\nc2", items.getDefault());

		items = items.getItems();
		assertEquals(HttpPartSchema.Type.INTEGER, items.getType());
		assertEquals(HttpPartSchema.Format.INT64, items.getFormat());
		assertEquals(HttpPartSchema.CollectionFormat.SSV, items.getCollectionFormat());
		assertEquals(5, items.getMaximum());
		assertEquals(6, items.getMinimum());
		assertEquals(7, items.getMultipleOf());
		assertEquals("8", items.getPattern().pattern());
		assertEquals(5, items.getMaxLength().longValue());
		assertEquals(6, items.getMinLength().longValue());
		assertEquals(7, items.getMaxItems().longValue());
		assertEquals(8, items.getMinItems().longValue());
		assertFalse(items.isExclusiveMaximum());
		assertFalse(items.isExclusiveMinimum());
		assertFalse(items.isUniqueItems());
		assertObjectEquals("['e3','e4']", items.getEnum());
		assertEquals("c3\nc4", items.getDefault());

		items = items.getItems();
		assertEquals(HttpPartSchema.Type.STRING, items.getType());
		assertEquals(HttpPartSchema.Format.FLOAT, items.getFormat());
		assertEquals(HttpPartSchema.CollectionFormat.TSV, items.getCollectionFormat());
		assertEquals(9, items.getMaximum());
		assertEquals(10, items.getMinimum());
		assertEquals(11, items.getMultipleOf());
		assertEquals("12", items.getPattern().pattern());
		assertEquals(9, items.getMaxLength().longValue());
		assertEquals(10, items.getMinLength().longValue());
		assertEquals(11, items.getMaxItems().longValue());
		assertEquals(12, items.getMinItems().longValue());
		assertTrue(items.isExclusiveMaximum());
		assertTrue(items.isExclusiveMinimum());
		assertTrue(items.isUniqueItems());
		assertObjectEquals("['e5','e6']", items.getEnum());
		assertEquals("c5\nc6", items.getDefault());

		items = items.getItems();
		assertEquals(HttpPartSchema.Type.ARRAY, items.getType());
		assertEquals(HttpPartSchema.Format.DOUBLE, items.getFormat());
		assertEquals(HttpPartSchema.CollectionFormat.PIPES, items.getCollectionFormat());
		assertEquals(13, items.getMaximum());
		assertEquals(14, items.getMinimum());
		assertEquals(15, items.getMultipleOf());
		assertEquals("16", items.getPattern().pattern());
		assertEquals(13, items.getMaxLength().longValue());
		assertEquals(14, items.getMinLength().longValue());
		assertEquals(15, items.getMaxItems().longValue());
		assertEquals(16, items.getMinItems().longValue());
		assertFalse(items.isExclusiveMaximum());
		assertFalse(items.isExclusiveMinimum());
		assertFalse(items.isUniqueItems());
		assertObjectEquals("['e7','e8']", items.getEnum());
		assertEquals("c7\nc8", items.getDefault());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// String input validations.
	//-----------------------------------------------------------------------------------------------------------------

	@Query(required=true)
	public static class B01a {}

	@Test
	public void b01a_required() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().apply(Query.class, B01a.class).build();

		s.validateInput("x");

		try {
			s.validateInput(null);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("No value specified.", e.getLocalizedMessage());
		}
		try {
			s.validateInput("");
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Empty value not allowed.", e.getLocalizedMessage());
		}
	}

	@Query(allowEmptyValue=true)
	public static class B01b {}

	@Test
	public void b01b_allowEmptyValue() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().apply(Query.class, B01b.class).build();

		s.validateInput("");
		s.validateInput(null);
	}

	@Query(required=true,allowEmptyValue=true)
	public static class B01c {}

	@Test
	public void b01b_required_allowEmptyValue() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().apply(Query.class, B01c.class).build();

		s.validateInput("");

		try {
			s.validateInput(null);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("No value specified.", e.getLocalizedMessage());
		}
	}

	@Query(pattern="x.*")
	public static class B02a {}

	@Test
	public void b02a_pattern() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().apply(Query.class, B02a.class).build();
		s.validateInput("x");
		s.validateInput("xx");
		try {
			s.validateInput("y");
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Value does not match expected pattern.  Must match pattern: x.*", e.getLocalizedMessage());
		}
		try {
			s.validateInput("yx");
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Value does not match expected pattern.  Must match pattern: x.*", e.getLocalizedMessage());
		}
		try {
			s.validateInput("");  // Empty headers are never allowed.
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Empty value not allowed.", e.getLocalizedMessage());
		}
	}

	@Query(minLength=2, maxLength=3)
	public static class B03a {}

	@Test
	public void b03a_length() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().apply(Query.class, B03a.class).build();
		s.validateInput("12");
		s.validateInput("123");
		s.validateInput(null);
		try {
			s.validateInput("1");
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Minimum length of value not met.", e.getLocalizedMessage());
		}
		try {
			s.validateInput("1234");
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Maximum length of value exceeded.", e.getLocalizedMessage());
		}
	}

	@Query(
		items=@Items(
			minLength=2, maxLength=3,
			items=@SubItems(
				minLength=3, maxLength=4,
				items={
					"minLength:4,maxLength:5,",
					"items:{minLength:5,maxLength:6}"
				}
			)
		)
	)
	public static class B03b {}

	@Test
	public void b03b_length_items() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().apply(Query.class, B03b.class).build();

		s.getItems().validateInput("12");
		s.getItems().getItems().validateInput("123");
		s.getItems().getItems().getItems().validateInput("1234");
		s.getItems().getItems().getItems().getItems().validateInput("12345");

		s.getItems().validateInput("123");
		s.getItems().getItems().validateInput("1234");
		s.getItems().getItems().getItems().validateInput("12345");
		s.getItems().getItems().getItems().getItems().validateInput("123456");

		s.getItems().validateInput(null);
		s.getItems().getItems().validateInput(null);
		s.getItems().getItems().getItems().validateInput(null);
		s.getItems().getItems().getItems().getItems().validateInput(null);

		try {
			s.getItems().validateInput("1");
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Minimum length of value not met.", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().validateInput("12");
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Minimum length of value not met.", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().getItems().validateInput("123");
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Minimum length of value not met.", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().getItems().getItems().validateInput("1234");
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Minimum length of value not met.", e.getLocalizedMessage());
		}

		try {
			s.getItems().validateInput("1234");
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Maximum length of value exceeded.", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().validateInput("12345");
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Maximum length of value exceeded.", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().getItems().validateInput("123456");
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Maximum length of value exceeded.", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().getItems().getItems().validateInput("1234567");
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Maximum length of value exceeded.", e.getLocalizedMessage());
		}
	}

	@Query(_enum="X,Y")
	public static class B04a {}

	@Test
	public void b04a_enum() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().apply(Query.class, B04a.class).build();
		s.validateInput("X");
		s.validateInput("Y");
		s.validateInput(null);
		try {
			s.validateInput("Z");
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Value does not match one of the expected values.  Must be one of the following: ['X','Y']", e.getLocalizedMessage());
		}
	}

	@Query(_enum=" X , Y ")
	public static class B04b {}

	@Test
	public void b04b_enum() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().apply(Query.class, B04b.class).build();
		s.validateInput("X");
		s.validateInput("Y");
		s.validateInput(null);
		try {
			s.validateInput("Z");
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Value does not match one of the expected values.  Must be one of the following: ['X','Y']", e.getLocalizedMessage());
		}
	}

	@Query(_enum="['X','Y']")
	public static class B04c {}

	@Test
	public void b04c_enum_json() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().apply(Query.class, B04c.class).build();
		s.validateInput("X");
		s.validateInput("Y");
		s.validateInput(null);
		try {
			s.validateInput("Z");
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Value does not match one of the expected values.  Must be one of the following: ['X','Y']", e.getLocalizedMessage());
		}
	}

	@Query(
		items=@Items(
			_enum="['W']",
			items=@SubItems(
				_enum="['X']",
				items={
					"enum:['Y'],",
					"items:{enum:['Z']}"
				}
			)
		)
	)
	public static class B04d {}

	@Test
	public void b04d_enum_items() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().apply(Query.class, B04d.class).build();

		s.getItems().validateInput("W");
		s.getItems().getItems().validateInput("X");
		s.getItems().getItems().getItems().validateInput("Y");
		s.getItems().getItems().getItems().getItems().validateInput("Z");

		try {
			s.getItems().validateInput("V");
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Value does not match one of the expected values.  Must be one of the following: ['W']", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().validateInput("V");
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Value does not match one of the expected values.  Must be one of the following: ['X']", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().getItems().validateInput("V");
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Value does not match one of the expected values.  Must be one of the following: ['Y']", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().getItems().getItems().validateInput("V");
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Value does not match one of the expected values.  Must be one of the following: ['Z']", e.getLocalizedMessage());
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Numeric validations
	//-----------------------------------------------------------------------------------------------------------------

	@Query(minimum="10", maximum="100")
	public static class C01a {}

	@Test
	public void c01a_minmax_ints() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().apply(Query.class, C01a.class).build();
		s.validateOutput(10, BeanContext.DEFAULT);
		s.validateOutput(100, BeanContext.DEFAULT);
		s.validateOutput(null, BeanContext.DEFAULT);
		try {
			s.validateOutput(9, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Minimum value not met.", e.getLocalizedMessage());
		}
		try {
			s.validateOutput(101, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Maximum value exceeded.", e.getLocalizedMessage());
		}
	}

	@Query(
		items=@Items(
			minimum="10", maximum="100",
			items=@SubItems(
				minimum="100", maximum="1000",
				items={
					"minimum:1000,maximum:10000,",
					"items:{minimum:10000,maximum:100000}"
				}
			)
		)
	)
	public static class C01b {}

	@Test
	public void c01b_minmax_ints_items() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().apply(Query.class, C01b.class).build();

		s.getItems().validateOutput(10, BeanContext.DEFAULT);
		s.getItems().getItems().validateOutput(100, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().validateOutput(1000, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().getItems().validateOutput(10000, BeanContext.DEFAULT);

		s.getItems().validateOutput(100, BeanContext.DEFAULT);
		s.getItems().getItems().validateOutput(1000, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().validateOutput(10000, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().getItems().validateOutput(100000, BeanContext.DEFAULT);

		try {
			s.getItems().validateOutput(9, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Minimum value not met.", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().validateOutput(99, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Minimum value not met.", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().getItems().validateOutput(999, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Minimum value not met.", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().getItems().getItems().validateOutput(9999, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Minimum value not met.", e.getLocalizedMessage());
		}

		try {
			s.getItems().validateOutput(101, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Maximum value exceeded.", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().validateOutput(1001, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Maximum value exceeded.", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().getItems().validateOutput(10001, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Maximum value exceeded.", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().getItems().getItems().validateOutput(100001, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Maximum value exceeded.", e.getLocalizedMessage());
		}
	}

	@Query(minimum="10", maximum="100", exclusiveMinimum=true, exclusiveMaximum=true)
	public static class C02a {}

	@Test
	public void c02a_minmax_exclusive() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().apply(Query.class, C02a.class).build();
		s.validateOutput(11, BeanContext.DEFAULT);
		s.validateOutput(99, BeanContext.DEFAULT);
		s.validateOutput(null, BeanContext.DEFAULT);
		try {
			s.validateOutput(10, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Minimum value not met.", e.getLocalizedMessage());
		}
		try {
			s.validateOutput(100, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Maximum value exceeded.", e.getLocalizedMessage());
		}
	}

	@Query(
		items=@Items(
			minimum="10", maximum="100", exclusiveMinimum=true, exclusiveMaximum=true,
			items=@SubItems(
				minimum="100", maximum="1000", exclusiveMinimum=true, exclusiveMaximum=true,
				items={
					"minimum:1000,maximum:10000,exclusiveMinimum:true,exclusiveMaximum:true,",
					"items:{minimum:10000,maximum:100000,exclusiveMinimum:true,exclusiveMaximum:true}"
				}
			)
		)
	)
	public static class C02b {}

	@Test
	public void c02b_minmax_exclusive_items() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().apply(Query.class, C02b.class).build();

		s.getItems().validateOutput(11, BeanContext.DEFAULT);
		s.getItems().getItems().validateOutput(101, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().validateOutput(1001, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().getItems().validateOutput(10001, BeanContext.DEFAULT);

		s.getItems().validateOutput(99, BeanContext.DEFAULT);
		s.getItems().getItems().validateOutput(999, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().validateOutput(9999, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().getItems().validateOutput(99999, BeanContext.DEFAULT);

		try {
			s.getItems().validateOutput(10, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Minimum value not met.", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().validateOutput(100, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Minimum value not met.", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().getItems().validateOutput(1000, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Minimum value not met.", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().getItems().getItems().validateOutput(10000, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Minimum value not met.", e.getLocalizedMessage());
		}

		try {
			s.getItems().validateOutput(100, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Maximum value exceeded.", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().validateOutput(1000, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Maximum value exceeded.", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().getItems().validateOutput(10000, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Maximum value exceeded.", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().getItems().getItems().validateOutput(100000, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Maximum value exceeded.", e.getLocalizedMessage());
		}
	}

	@Query(minimum="10.1", maximum="100.1")
	public static class C03a {}

	@Test
	public void c03_minmax_floats() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().apply(Query.class, C03a.class).build();
		s.validateOutput(10.1f, BeanContext.DEFAULT);
		s.validateOutput(100.1f, BeanContext.DEFAULT);
		s.validateOutput(null, BeanContext.DEFAULT);
		try {
			s.validateOutput(10f, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Minimum value not met.", e.getLocalizedMessage());
		}
		try {
			s.validateOutput(100.2f, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Maximum value exceeded.", e.getLocalizedMessage());
		}
	}

	@Query(
		items=@Items(
			minimum="10.1", maximum="100.1",
			items=@SubItems(
				minimum="100.1", maximum="1000.1",
				items={
					"minimum:1000.1,maximum:10000.1,",
					"items:{minimum:10000.1,maximum:100000.1}"
				}
			)
		)
	)
	public static class C03b {}

	@Test
	public void c03b_minmax_floats_items() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().apply(Query.class, C03b.class).build();

		s.getItems().validateOutput(10.1f, BeanContext.DEFAULT);
		s.getItems().getItems().validateOutput(100.1f, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().validateOutput(1000.1f, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().getItems().validateOutput(10000.1f, BeanContext.DEFAULT);

		s.getItems().validateOutput(100.1f, BeanContext.DEFAULT);
		s.getItems().getItems().validateOutput(1000.1f, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().validateOutput(10000.1f, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().getItems().validateOutput(100000.1f, BeanContext.DEFAULT);

		try {
			s.getItems().validateOutput(10f, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Minimum value not met.", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().validateOutput(100f, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Minimum value not met.", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().getItems().validateOutput(1000f, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Minimum value not met.", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().getItems().getItems().validateOutput(10000f, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Minimum value not met.", e.getLocalizedMessage());
		}

		try {
			s.getItems().validateOutput(100.2f, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Maximum value exceeded.", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().validateOutput(1000.2f, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Maximum value exceeded.", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().getItems().validateOutput(10000.2f, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Maximum value exceeded.", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().getItems().getItems().validateOutput(100000.2f, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Maximum value exceeded.", e.getLocalizedMessage());
		}
	}

	@Query(minimum="10.1", maximum="100.1", exclusiveMinimum=true, exclusiveMaximum=true)
	public static class C04a {}

	@Test
	public void c04a_minmax_floats_exclusive() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().apply(Query.class, C04a.class).build();
		s.validateOutput(10.2f, BeanContext.DEFAULT);
		s.validateOutput(100f, BeanContext.DEFAULT);
		s.validateOutput(null, BeanContext.DEFAULT);
		try {
			s.validateOutput(10.1f, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Minimum value not met.", e.getLocalizedMessage());
		}
		try {
			s.validateOutput(100.1f, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Maximum value exceeded.", e.getLocalizedMessage());
		}
	}

	@Query(
		items=@Items(
			minimum="10.1", maximum="100.1", exclusiveMinimum=true, exclusiveMaximum=true,
			items=@SubItems(
				minimum="100.1", maximum="1000.1", exclusiveMinimum=true, exclusiveMaximum=true,
				items={
					"minimum:1000.1,maximum:10000.1,exclusiveMinimum:true,exclusiveMaximum:true,",
					"items:{minimum:10000.1,maximum:100000.1,exclusiveMinimum:true,exclusiveMaximum:true}"
				}
			)
		)
	)
	public static class C04b {}

	@Test
	public void c04b_minmax_floats_exclusive_items() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().apply(Query.class, C04b.class).build();

		s.getItems().validateOutput(10.2f, BeanContext.DEFAULT);
		s.getItems().getItems().validateOutput(100.2f, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().validateOutput(1000.2f, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().getItems().validateOutput(10000.2f, BeanContext.DEFAULT);

		s.getItems().validateOutput(100f, BeanContext.DEFAULT);
		s.getItems().getItems().validateOutput(1000f, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().validateOutput(10000f, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().getItems().validateOutput(100000f, BeanContext.DEFAULT);

		try {
			s.getItems().validateOutput(10.1f, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Minimum value not met.", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().validateOutput(100.1f, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Minimum value not met.", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().getItems().validateOutput(1000.1f, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Minimum value not met.", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().getItems().getItems().validateOutput(10000.1f, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Minimum value not met.", e.getLocalizedMessage());
		}

		try {
			s.getItems().validateOutput(100.1f, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Maximum value exceeded.", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().validateOutput(1000.1f, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Maximum value exceeded.", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().getItems().validateOutput(10000.1f, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Maximum value exceeded.", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().getItems().getItems().validateOutput(100000.1f, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Maximum value exceeded.", e.getLocalizedMessage());
		}
	}

	@Query(multipleOf="10")
	public static class C05a {}

	@Test
	public void c05a_multipleOf() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().apply(Query.class, C05a.class).build();
		s.validateOutput(0, BeanContext.DEFAULT);
		s.validateOutput(10, BeanContext.DEFAULT);
		s.validateOutput(20, BeanContext.DEFAULT);
		s.validateOutput(10f, BeanContext.DEFAULT);
		s.validateOutput(20f, BeanContext.DEFAULT);
		s.validateOutput(null, BeanContext.DEFAULT);
		try {
			s.validateOutput(11, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Multiple-of not met.", e.getLocalizedMessage());
		}
	}

	@Query(
		items=@Items(
			multipleOf="10",
			items=@SubItems(
				multipleOf="100",
				items={
					"multipleOf:1000,",
					"items:{multipleOf:10000}"
				}
			)
		)
	)
	public static class C05b {}

	@Test
	public void c05b_multipleOf_items() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().apply(Query.class, C05b.class).build();

		s.getItems().validateOutput(0, BeanContext.DEFAULT);
		s.getItems().getItems().validateOutput(0, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().validateOutput(0, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().getItems().validateOutput(0, BeanContext.DEFAULT);

		s.getItems().validateOutput(10, BeanContext.DEFAULT);
		s.getItems().getItems().validateOutput(100, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().validateOutput(1000, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().getItems().validateOutput(10000, BeanContext.DEFAULT);

		s.getItems().validateOutput(20, BeanContext.DEFAULT);
		s.getItems().getItems().validateOutput(200, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().validateOutput(2000, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().getItems().validateOutput(20000, BeanContext.DEFAULT);

		s.getItems().validateOutput(10f, BeanContext.DEFAULT);
		s.getItems().getItems().validateOutput(100f, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().validateOutput(1000f, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().getItems().validateOutput(10000f, BeanContext.DEFAULT);

		s.getItems().validateOutput(20f, BeanContext.DEFAULT);
		s.getItems().getItems().validateOutput(200f, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().validateOutput(2000f, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().getItems().validateOutput(20000f, BeanContext.DEFAULT);

		try {
			s.getItems().validateOutput(11, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Multiple-of not met.", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().validateOutput(101, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Multiple-of not met.", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().getItems().validateOutput(1001, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Multiple-of not met.", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().getItems().getItems().validateOutput(10001, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Multiple-of not met.", e.getLocalizedMessage());
		}
	}

	@Query(multipleOf="10.1")
	public static class C06a {}

	@Test
	public void c06a_multipleOf_floats() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().apply(Query.class, C06a.class).build();
		s.validateOutput(0, BeanContext.DEFAULT);
		s.validateOutput(10.1f, BeanContext.DEFAULT);
		s.validateOutput(20.2f, BeanContext.DEFAULT);
		s.validateOutput(null, BeanContext.DEFAULT);
		try {
			s.validateOutput(10.2f, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Multiple-of not met.", e.getLocalizedMessage());
		}
	}

	@Query(
		items=@Items(
			multipleOf="10.1",
			items=@SubItems(
				multipleOf="100.1",
				items={
					"multipleOf:1000.1,",
					"items:{multipleOf:10000.1}"
				}
			)
		)
	)
	public static class C06b {}

	@Test
	public void c06b_multipleOf_floats_items() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().apply(Query.class, C06b.class).build();

		s.getItems().validateOutput(0, BeanContext.DEFAULT);
		s.getItems().getItems().validateOutput(0, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().validateOutput(0, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().getItems().validateOutput(0, BeanContext.DEFAULT);

		s.getItems().validateOutput(10.1f, BeanContext.DEFAULT);
		s.getItems().getItems().validateOutput(100.1f, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().validateOutput(1000.1f, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().getItems().validateOutput(10000.1f, BeanContext.DEFAULT);

		s.getItems().validateOutput(20.2f, BeanContext.DEFAULT);
		s.getItems().getItems().validateOutput(200.2f, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().validateOutput(2000.2f, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().getItems().validateOutput(20000.2f, BeanContext.DEFAULT);

		try {
			s.getItems().validateOutput(10.2f, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Multiple-of not met.", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().validateOutput(100.2f, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Multiple-of not met.", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().getItems().validateOutput(1000.2f, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Multiple-of not met.", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().getItems().getItems().validateOutput(10000.2f, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Multiple-of not met.", e.getLocalizedMessage());
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Collections/Array validations
	//-----------------------------------------------------------------------------------------------------------------

	@Query(
		items=@Items(
			uniqueItems=true,
			items=@SubItems(
				uniqueItems=true,
				items={
					"uniqueItems:true,",
					"items:{uniqueItems:true}"
				}
			)
		)

	)
	public static class D01 {}

	@Test
	public void d01a_uniqueItems_arrays() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().apply(Query.class, D01.class).build();

		String[] good = split("a,b"), bad = split("a,a");

		s.getItems().validateOutput(good, BeanContext.DEFAULT);
		s.getItems().getItems().validateOutput(good, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().validateOutput(good, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().getItems().validateOutput(good, BeanContext.DEFAULT);
		s.getItems().validateOutput(null, BeanContext.DEFAULT);

		try {
			s.getItems().validateOutput(bad, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Duplicate items not allowed.", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().validateOutput(bad, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Duplicate items not allowed.", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().getItems().validateOutput(bad, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Duplicate items not allowed.", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().getItems().getItems().validateOutput(bad, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Duplicate items not allowed.", e.getLocalizedMessage());
		}
	}

	@Test
	public void d01b_uniqueItems_collections() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().apply(Query.class, D01.class).build();

		AList<String>
			good = new AList<String>().appendAll(split("a,b")),
			bad = new AList<String>().appendAll(split("a,a"));

		s.getItems().validateOutput(good, BeanContext.DEFAULT);
		s.getItems().getItems().validateOutput(good, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().validateOutput(good, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().getItems().validateOutput(good, BeanContext.DEFAULT);
		s.getItems().validateOutput(null, BeanContext.DEFAULT);

		try {
			s.getItems().validateOutput(bad, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Duplicate items not allowed.", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().validateOutput(bad, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Duplicate items not allowed.", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().getItems().validateOutput(bad, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Duplicate items not allowed.", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().getItems().getItems().validateOutput(bad, BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Duplicate items not allowed.", e.getLocalizedMessage());
		}
	}

	@Query(
		items=@Items(
			minItems=1, maxItems=2,
			items=@SubItems(
				minItems=2, maxItems=3,
				items={
					"minItems:3,maxItems:4,",
					"items:{minItems:4,maxItems:5}"
				}
			)
		)

	)
	public static class D02 {}

	@Test
	public void d02a_minMaxItems_arrays() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().apply(Query.class, D02.class).build();

		s.getItems().validateOutput(split("1"), BeanContext.DEFAULT);
		s.getItems().getItems().validateOutput(split("1,2"), BeanContext.DEFAULT);
		s.getItems().getItems().getItems().validateOutput(split("1,2,3"), BeanContext.DEFAULT);
		s.getItems().getItems().getItems().getItems().validateOutput(split("1,2,3,4"), BeanContext.DEFAULT);

		s.getItems().validateOutput(split("1,2"), BeanContext.DEFAULT);
		s.getItems().getItems().validateOutput(split("1,2,3"), BeanContext.DEFAULT);
		s.getItems().getItems().getItems().validateOutput(split("1,2,3,4"), BeanContext.DEFAULT);
		s.getItems().getItems().getItems().getItems().validateOutput(split("1,2,3,4,5"), BeanContext.DEFAULT);

		try {
			s.getItems().validateOutput(new String[0], BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Minimum number of items not met.", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().validateOutput(split("1"), BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Minimum number of items not met.", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().getItems().validateOutput(split("1,2"), BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Minimum number of items not met.", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().getItems().getItems().validateOutput(split("1,2,3"), BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Minimum number of items not met.", e.getLocalizedMessage());
		}

		try {
			s.getItems().validateOutput(split("1,2,3"), BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Maximum number of items exceeded.", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().validateOutput(split("1,2,3,4"), BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Maximum number of items exceeded.", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().getItems().validateOutput(split("1,2,3,4,5"), BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Maximum number of items exceeded.", e.getLocalizedMessage());
		}
		try {
			s.getItems().getItems().getItems().getItems().validateOutput(split("1,2,3,4,5,6"), BeanContext.DEFAULT);
			fail();
		} catch (SchemaValidationException e) {
			assertEquals("Maximum number of items exceeded.", e.getLocalizedMessage());
		}
	}
}