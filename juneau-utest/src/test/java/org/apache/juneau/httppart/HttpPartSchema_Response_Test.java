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

import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.utest.utils.Utils2.*;
import static org.junit.Assert.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.http.annotation.*;
import org.junit.jupiter.api.*;

public class HttpPartSchema_Response_Test extends SimpleTestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Basic test
	//-----------------------------------------------------------------------------------------------------------------
	@Test void testBasic() {
		assertNotThrown(()->HttpPartSchema.create().build());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Response
	//-----------------------------------------------------------------------------------------------------------------

	@Response(
		schema=@Schema(
			t="number",
			f="int32",
			max="1",
			min="2",
			mo="3",
			p="4",
			maxl=1,
			minl=2,
			maxi=3,
			mini=4,
			maxp=5,
			minp=6,
			emax=true,
			emin=true,
			ui=true,
			df={"c1","c2"},
			e="e1,e2",
			items=@Items(
				t="integer",
				f="int64",
				cf="ssv",
				max="5",
				min="6",
				mo="7",
				p="8",
				maxl=5,
				minl=6,
				maxi=7,
				mini=8,
				emax=false,
				emin=false,
				ui=false,
				df={"c3","c4"},
				e="e3,e4",
				items=@SubItems(
					t="string",
					f="float",
					cf="tsv",
					max="9",
					min="10",
					mo="11",
					p="12",
					maxl=9,
					minl=10,
					maxi=11,
					mini=12,
					emax=true,
					emin=true,
					ui=true,
					df={"c5","c6"},
					e="e5,e6",
					items={
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
					}
				)
			)
		)
	)
	public static class A05 {}

	@Test void a05_basic_nestedItems_onClass() {
		HttpPartSchema s = HttpPartSchema.create().apply(Response.class, A05.class).noValidate().build();

		assertEquals(HttpPartDataType.NUMBER, s.getType());
		assertEquals(HttpPartFormat.INT32, s.getFormat());
		assertEquals(1, s.getMaximum());
		assertEquals(2, s.getMinimum());
		assertEquals(3, s.getMultipleOf());
		assertEquals("4", s.getPattern().pattern());
		assertEquals(1, s.getMaxLength().longValue());
		assertEquals(2, s.getMinLength().longValue());
		assertEquals(3, s.getMaxItems().longValue());
		assertEquals(4, s.getMinItems().longValue());
		assertEquals(5, s.getMaxProperties().longValue());
		assertEquals(6, s.getMinProperties().longValue());
		assertTrue(s.isExclusiveMaximum());
		assertTrue(s.isExclusiveMinimum());
		assertTrue(s.isUniqueItems());
		assertJson(s.getEnum(), "['e1','e2']");
		assertEquals("c1\nc2", s.getDefault());

		HttpPartSchema items = s.getItems();
		assertEquals(HttpPartDataType.INTEGER, items.getType());
		assertEquals(HttpPartFormat.INT64, items.getFormat());
		assertEquals(HttpPartCollectionFormat.SSV, items.getCollectionFormat());
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
		assertJson(items.getEnum(), "['e3','e4']");
		assertEquals("c3\nc4", items.getDefault());

		items = items.getItems();
		assertEquals(HttpPartDataType.STRING, items.getType());
		assertEquals(HttpPartFormat.FLOAT, items.getFormat());
		assertEquals(HttpPartCollectionFormat.TSV, items.getCollectionFormat());
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
		assertJson(items.getEnum(), "['e5','e6']");
		assertEquals("c5\nc6", items.getDefault());

		items = items.getItems();
		assertEquals(HttpPartDataType.ARRAY, items.getType());
		assertEquals(HttpPartFormat.DOUBLE, items.getFormat());
		assertEquals(HttpPartCollectionFormat.PIPES, items.getCollectionFormat());
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
		assertJson(items.getEnum(), "['e7','e8']");
		assertEquals("c7\nc8", items.getDefault());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// String input validations.
	//-----------------------------------------------------------------------------------------------------------------

	@Response(
		schema=@Schema(
			p="x.*",
			aev=true
		)
	)
	public static class B02a {}

	@Test void b02a_pattern() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().apply(Response.class, B02a.class).build();

		s.validateInput("x");
		s.validateInput("xx");

		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected pattern.  Must match pattern: x.*", ()->s.validateInput(""));
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected pattern.  Must match pattern: x.*", ()->s.validateInput("y"));
	}

	@Response(
		schema=@Schema(
			items=@Items(
				p="w.*",
				items=@SubItems(
					p="x.*",
					items={
						"pattern:'y.*',",
						"items:{pattern:'z.*'}"
					}
				)
			)
		)
	)
	public static class B02b {}

	@Response(
		schema=@Schema(
			minl=2, maxl=3
		)
	)
	public static class B03a {}

	@Test void b03a_length() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().apply(Response.class, B03a.class).build();
		s.validateInput("12");
		s.validateInput("123");
		s.validateInput(null);
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum length of value not met.", ()->s.validateInput("1"));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum length of value exceeded.", ()->s.validateInput("1234"));
	}

	@Response(
		schema=@Schema(
			items=@Items(
				minl=2, maxl=3,
				items=@SubItems(
					minl=3, maxl=4,
					items={
						"minLength:4,maxLength:5,",
						"items:{minLength:5,maxLength:6}"
					}
				)
			)
		)
	)
	public static class B03b {}

	@Test void b03b_length_items() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().apply(Response.class, B03b.class).build();

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

		assertThrowsWithMessage(SchemaValidationException.class, "Minimum length of value not met.", ()->s.getItems().validateInput("1"));
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum length of value not met.", ()->s.getItems().getItems().validateInput("12"));
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum length of value not met.", ()->s.getItems().getItems().getItems().validateInput("123"));
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum length of value not met.", ()->s.getItems().getItems().getItems().getItems().validateInput("1234"));

		assertThrowsWithMessage(SchemaValidationException.class, "Maximum length of value exceeded.", ()->s.getItems().validateInput("1234"));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum length of value exceeded.", ()->s.getItems().getItems().validateInput("12345"));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum length of value exceeded.", ()->s.getItems().getItems().getItems().validateInput("123456"));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum length of value exceeded.", ()->s.getItems().getItems().getItems().getItems().validateInput("1234567"));
	}

	@Response(schema=@Schema(e="X,Y"))
	public static class B04a {}

	@Test void b04a_enum() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().apply(Response.class, B04a.class).build();
		s.validateInput("X");
		s.validateInput("Y");
		s.validateInput(null);
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match one of the expected values.  Must be one of the following:  X, Y", ()->s.validateInput("Z"));
	}

	@Response(schema=@Schema(e=" X , Y "))
	public static class B04b {}

	@Test void b04b_enum() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().apply(Response.class, B04b.class).build();
		s.validateInput("X");
		s.validateInput("Y");
		s.validateInput(null);
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match one of the expected values.  Must be one of the following:  X, Y", ()->s.validateInput("Z"));
	}

	@Response(schema=@Schema(e="X,Y"))
	public static class B04c {}

	@Test void b04c_enum_json() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().apply(Response.class, B04c.class).build();
		s.validateInput("X");
		s.validateInput("Y");
		s.validateInput(null);
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match one of the expected values.  Must be one of the following:  X, Y", ()->s.validateInput("Z"));
	}

	@Response(
		schema=@Schema(
			items=@Items(
				e="W",
				items=@SubItems(
					e="X",
					items={
						"enum:['Y'],",
						"items:{enum:['Z']}"
					}
				)
			)
		)
	)
	public static class B04d {}

	@Test void b04d_enum_items() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().apply(Response.class, B04d.class).build();

		s.getItems().validateInput("W");
		s.getItems().getItems().validateInput("X");
		s.getItems().getItems().getItems().validateInput("Y");
		s.getItems().getItems().getItems().getItems().validateInput("Z");

		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match one of the expected values.  Must be one of the following:  W", ()->s.getItems().validateInput("V"));
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match one of the expected values.  Must be one of the following:  X", ()->s.getItems().getItems().validateInput("V"));
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match one of the expected values.  Must be one of the following:  Y", ()->s.getItems().getItems().getItems().validateInput("V"));
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match one of the expected values.  Must be one of the following:  Z", ()->s.getItems().getItems().getItems().getItems().validateInput("V"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Numeric validations
	//-----------------------------------------------------------------------------------------------------------------

	@Response(schema=@Schema(min="10", max="100"))
	public static class C01a {}

	@Test void c01a_minmax_ints() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().apply(Response.class, C01a.class).build();
		s.validateOutput(10, BeanContext.DEFAULT);
		s.validateOutput(100, BeanContext.DEFAULT);
		s.validateOutput(null, BeanContext.DEFAULT);
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.validateOutput(9, BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.validateOutput(101, BeanContext.DEFAULT));
	}

	@Response(
		schema=@Schema(
			items=@Items(
				min="10", max="100",
				items=@SubItems(
					min="100", max="1000",
					items={
						"minimum:1000,maximum:10000,",
						"items:{minimum:10000,maximum:100000}"
					}
				)
			)
		)
	)
	public static class C01b {}

	@Test void c01b_minmax_ints_items() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().apply(Response.class, C01b.class).build();

		s.getItems().validateOutput(10, BeanContext.DEFAULT);
		s.getItems().getItems().validateOutput(100, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().validateOutput(1000, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().getItems().validateOutput(10000, BeanContext.DEFAULT);

		s.getItems().validateOutput(100, BeanContext.DEFAULT);
		s.getItems().getItems().validateOutput(1000, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().validateOutput(10000, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().getItems().validateOutput(100000, BeanContext.DEFAULT);

		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.getItems().validateOutput(9, BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.getItems().getItems().validateOutput(99, BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.getItems().getItems().getItems().validateOutput(999, BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.getItems().getItems().getItems().getItems().validateOutput(9999, BeanContext.DEFAULT));

		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.getItems().validateOutput(101, BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.getItems().getItems().validateOutput(1001, BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.getItems().getItems().getItems().validateOutput(10001, BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.getItems().getItems().getItems().getItems().validateOutput(100001, BeanContext.DEFAULT));
	}

	@Response(schema=@Schema(min="10", max="100", emin=true, emax=true))
	public static class C02a {}

	@Test void c02a_minmax_exclusive() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().apply(Response.class, C02a.class).build();
		s.validateOutput(11, BeanContext.DEFAULT);
		s.validateOutput(99, BeanContext.DEFAULT);
		s.validateOutput(null, BeanContext.DEFAULT);
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.validateOutput(10, BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.validateOutput(100, BeanContext.DEFAULT));
	}

	@Response(
		schema=@Schema(
			items=@Items(
				min="10", max="100", emin=true, emax=true,
				items=@SubItems(
					min="100", max="1000", emin=true, emax=true,
					items={
						"minimum:1000,maximum:10000,exclusiveMinimum:true,exclusiveMaximum:true,",
						"items:{minimum:10000,maximum:100000,exclusiveMinimum:true,exclusiveMaximum:true}"
					}
				)
			)
		)
	)
	public static class C02b {}

	@Test void c02b_minmax_exclusive_items() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().apply(Response.class, C02b.class).build();

		s.getItems().validateOutput(11, BeanContext.DEFAULT);
		s.getItems().getItems().validateOutput(101, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().validateOutput(1001, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().getItems().validateOutput(10001, BeanContext.DEFAULT);

		s.getItems().validateOutput(99, BeanContext.DEFAULT);
		s.getItems().getItems().validateOutput(999, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().validateOutput(9999, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().getItems().validateOutput(99999, BeanContext.DEFAULT);

		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.getItems().validateOutput(10, BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.getItems().getItems().validateOutput(100, BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.getItems().getItems().getItems().validateOutput(1000, BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.getItems().getItems().getItems().getItems().validateOutput(10000, BeanContext.DEFAULT));

		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.getItems().validateOutput(100, BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.getItems().getItems().validateOutput(1000, BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.getItems().getItems().getItems().validateOutput(10000, BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.getItems().getItems().getItems().getItems().validateOutput(100000, BeanContext.DEFAULT));
	}

	@Response(schema=@Schema(min="10.1", max="100.1"))
	public static class C03a {}

	@Test void c03_minmax_floats() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().apply(Response.class, C03a.class).build();
		s.validateOutput(10.1f, BeanContext.DEFAULT);
		s.validateOutput(100.1f, BeanContext.DEFAULT);
		s.validateOutput(null, BeanContext.DEFAULT);
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.validateOutput(10f, BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.validateOutput(100.2f, BeanContext.DEFAULT));
	}

	@Response(
		schema=@Schema(
			items=@Items(
				min="10.1", max="100.1",
				items=@SubItems(
					min="100.1", max="1000.1",
					items={
						"minimum:1000.1,maximum:10000.1,",
						"items:{minimum:10000.1,maximum:100000.1}"
					}
				)
			)
		)
	)
	public static class C03b {}

	@Test void c03b_minmax_floats_items() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().apply(Response.class, C03b.class).build();

		s.getItems().validateOutput(10.1f, BeanContext.DEFAULT);
		s.getItems().getItems().validateOutput(100.1f, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().validateOutput(1000.1f, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().getItems().validateOutput(10000.1f, BeanContext.DEFAULT);

		s.getItems().validateOutput(100.1f, BeanContext.DEFAULT);
		s.getItems().getItems().validateOutput(1000.1f, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().validateOutput(10000.1f, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().getItems().validateOutput(100000.1f, BeanContext.DEFAULT);

		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.getItems().validateOutput(10f, BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.getItems().getItems().validateOutput(100f, BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.getItems().getItems().getItems().validateOutput(1000f, BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.getItems().getItems().getItems().getItems().validateOutput(10000f, BeanContext.DEFAULT));

		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.getItems().validateOutput(100.2f, BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.getItems().getItems().validateOutput(1000.2f, BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.getItems().getItems().getItems().validateOutput(10000.2f, BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.getItems().getItems().getItems().getItems().validateOutput(100000.2f, BeanContext.DEFAULT));
	}

	@Response(schema=@Schema(min="10.1", max="100.1", emin=true, emax=true))
	public static class C04a {}

	@Test void c04a_minmax_floats_exclusive() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().apply(Response.class, C04a.class).build();
		s.validateOutput(10.2f, BeanContext.DEFAULT);
		s.validateOutput(100f, BeanContext.DEFAULT);
		s.validateOutput(null, BeanContext.DEFAULT);
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.validateOutput(10.1f, BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.validateOutput(100.1f, BeanContext.DEFAULT));
	}

	@Response(
		schema=@Schema(
			items=@Items(
				min="10.1", max="100.1", emin=true, emax=true,
				items=@SubItems(
					min="100.1", max="1000.1", emin=true, emax=true,
					items={
						"minimum:1000.1,maximum:10000.1,exclusiveMinimum:true,exclusiveMaximum:true,",
						"items:{minimum:10000.1,maximum:100000.1,exclusiveMinimum:true,exclusiveMaximum:true}"
					}
				)
			)
		)
	)
	public static class C04b {}

	@Test void c04b_minmax_floats_exclusive_items() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().apply(Response.class, C04b.class).build();

		s.getItems().validateOutput(10.2f, BeanContext.DEFAULT);
		s.getItems().getItems().validateOutput(100.2f, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().validateOutput(1000.2f, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().getItems().validateOutput(10000.2f, BeanContext.DEFAULT);

		s.getItems().validateOutput(100f, BeanContext.DEFAULT);
		s.getItems().getItems().validateOutput(1000f, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().validateOutput(10000f, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().getItems().validateOutput(100000f, BeanContext.DEFAULT);

		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.getItems().validateOutput(10.1f, BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.getItems().getItems().validateOutput(100.1f, BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.getItems().getItems().getItems().validateOutput(1000.1f, BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.getItems().getItems().getItems().getItems().validateOutput(10000.1f, BeanContext.DEFAULT));

		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.getItems().validateOutput(100.1f, BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.getItems().getItems().validateOutput(1000.1f, BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.getItems().getItems().getItems().validateOutput(10000.1f, BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.getItems().getItems().getItems().getItems().validateOutput(100000.1f, BeanContext.DEFAULT));
	}

	@Response(schema=@Schema(mo="10"))
	public static class C05a {}

	@Test void c05a_multipleOf() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().apply(Response.class, C05a.class).build();
		s.validateOutput(0, BeanContext.DEFAULT);
		s.validateOutput(10, BeanContext.DEFAULT);
		s.validateOutput(20, BeanContext.DEFAULT);
		s.validateOutput(10f, BeanContext.DEFAULT);
		s.validateOutput(20f, BeanContext.DEFAULT);
		s.validateOutput(null, BeanContext.DEFAULT);
		assertThrowsWithMessage(SchemaValidationException.class, "Multiple-of not met.", ()->s.validateOutput(11, BeanContext.DEFAULT));
	}

	@Response(
		schema=@Schema(
			items=@Items(
				mo="10",
				items=@SubItems(
					mo="100",
					items={
						"multipleOf:1000,",
						"items:{multipleOf:10000}"
					}
				)
			)
		)
	)
	public static class C05b {}

	@Test void c05b_multipleOf_items() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().apply(Response.class, C05b.class).build();

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

		assertThrowsWithMessage(SchemaValidationException.class, "Multiple-of not met.", ()->s.getItems().validateOutput(11, BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Multiple-of not met.", ()->s.getItems().getItems().validateOutput(101, BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Multiple-of not met.", ()->s.getItems().getItems().getItems().validateOutput(1001, BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Multiple-of not met.", ()->s.getItems().getItems().getItems().getItems().validateOutput(10001, BeanContext.DEFAULT));
	}

	@Response(schema=@Schema(mo="10.1"))
	public static class C06a {}

	@Test void c06a_multipleOf_floats() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().apply(Response.class, C06a.class).build();
		s.validateOutput(0, BeanContext.DEFAULT);
		s.validateOutput(10.1f, BeanContext.DEFAULT);
		s.validateOutput(20.2f, BeanContext.DEFAULT);
		s.validateOutput(null, BeanContext.DEFAULT);
		assertThrowsWithMessage(SchemaValidationException.class, "Multiple-of not met.", ()->s.validateOutput(10.2f, BeanContext.DEFAULT));
	}

	@Response(
		schema=@Schema(
			items=@Items(
				mo="10.1",
				items=@SubItems(
					mo="100.1",
					items={
						"multipleOf:1000.1,",
						"items:{multipleOf:10000.1}"
					}
				)
			)
		)
	)
	public static class C06b {}

	@Test void c06b_multipleOf_floats_items() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().apply(Response.class, C06b.class).build();

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

		assertThrowsWithMessage(SchemaValidationException.class, "Multiple-of not met.", ()->s.getItems().validateOutput(10.2f, BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Multiple-of not met.", ()->s.getItems().getItems().validateOutput(100.2f, BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Multiple-of not met.", ()->s.getItems().getItems().getItems().validateOutput(1000.2f, BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Multiple-of not met.", ()->s.getItems().getItems().getItems().getItems().validateOutput(10000.2f, BeanContext.DEFAULT));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Collections/Array validations
	//-----------------------------------------------------------------------------------------------------------------

	@Response(
		schema=@Schema(
			items=@Items(
				ui=true,
				items=@SubItems(
					ui=true,
					items={
						"uniqueItems:true,",
						"items:{uniqueItems:true}"
					}
				)
			)
		)
	)
	public static class D01 {}

	@Test void d01a_uniqueItems_arrays() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().apply(Response.class, D01.class).build();

		String[] good = split("a,b"), bad = split("a,a");

		s.getItems().validateOutput(good, BeanContext.DEFAULT);
		s.getItems().getItems().validateOutput(good, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().validateOutput(good, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().getItems().validateOutput(good, BeanContext.DEFAULT);
		s.getItems().validateOutput(null, BeanContext.DEFAULT);

		assertThrowsWithMessage(SchemaValidationException.class, "Duplicate items not allowed.", ()->s.getItems().validateOutput(bad, BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Duplicate items not allowed.", ()->s.getItems().getItems().validateOutput(bad, BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Duplicate items not allowed.", ()->s.getItems().getItems().getItems().validateOutput(bad, BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Duplicate items not allowed.", ()->s.getItems().getItems().getItems().getItems().validateOutput(bad, BeanContext.DEFAULT));
	}

	@Test void d01b_uniqueItems_collections() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().apply(Response.class, D01.class).build();

		List<String>
			good = alist("a","b"),
			bad = alist("a","a");

		s.getItems().validateOutput(good, BeanContext.DEFAULT);
		s.getItems().getItems().validateOutput(good, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().validateOutput(good, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().getItems().validateOutput(good, BeanContext.DEFAULT);
		s.getItems().validateOutput(null, BeanContext.DEFAULT);

		assertThrowsWithMessage(SchemaValidationException.class, "Duplicate items not allowed.", ()->s.getItems().validateOutput(bad, BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Duplicate items not allowed.", ()->s.getItems().getItems().validateOutput(bad, BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Duplicate items not allowed.", ()->s.getItems().getItems().getItems().validateOutput(bad, BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Duplicate items not allowed.", ()->s.getItems().getItems().getItems().getItems().validateOutput(bad, BeanContext.DEFAULT));
	}

	@Response(
		schema=@Schema(
			items=@Items(
				mini=1, maxi=2,
				items=@SubItems(
					mini=2, maxi=3,
					items={
						"minItems:3,maxItems:4,",
						"items:{minItems:4,maxItems:5}"
					}
				)
			)
		)
	)
	public static class D02 {}

	@Test void d02a_minMaxItems_arrays() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().apply(Response.class, D02.class).build();

		s.getItems().validateOutput(split("1"), BeanContext.DEFAULT);
		s.getItems().getItems().validateOutput(split("1,2"), BeanContext.DEFAULT);
		s.getItems().getItems().getItems().validateOutput(split("1,2,3"), BeanContext.DEFAULT);
		s.getItems().getItems().getItems().getItems().validateOutput(split("1,2,3,4"), BeanContext.DEFAULT);

		s.getItems().validateOutput(split("1,2"), BeanContext.DEFAULT);
		s.getItems().getItems().validateOutput(split("1,2,3"), BeanContext.DEFAULT);
		s.getItems().getItems().getItems().validateOutput(split("1,2,3,4"), BeanContext.DEFAULT);
		s.getItems().getItems().getItems().getItems().validateOutput(split("1,2,3,4,5"), BeanContext.DEFAULT);

		assertThrowsWithMessage(SchemaValidationException.class, "Minimum number of items not met.", ()->s.getItems().validateOutput(new String[0], BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum number of items not met.", ()->s.getItems().getItems().validateOutput(split("1"), BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum number of items not met.", ()->s.getItems().getItems().getItems().validateOutput(split("1,2"), BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum number of items not met.", ()->s.getItems().getItems().getItems().getItems().validateOutput(split("1,2,3"), BeanContext.DEFAULT));

		assertThrowsWithMessage(SchemaValidationException.class, "Maximum number of items exceeded.", ()->s.getItems().validateOutput(split("1,2,3"), BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum number of items exceeded.", ()->s.getItems().getItems().validateOutput(split("1,2,3,4"), BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum number of items exceeded.", ()->s.getItems().getItems().getItems().validateOutput(split("1,2,3,4,5"), BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum number of items exceeded.", ()->s.getItems().getItems().getItems().getItems().validateOutput(split("1,2,3,4,5,6"), BeanContext.DEFAULT));
	}
}