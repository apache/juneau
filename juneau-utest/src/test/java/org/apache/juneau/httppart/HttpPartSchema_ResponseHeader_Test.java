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

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.reflect.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class HttpPartSchema_ResponseHeader_Test {

	//-----------------------------------------------------------------------------------------------------------------
	// Basic test
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void testBasic() throws Exception {
		HttpPartSchema.create().build();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Header
	//-----------------------------------------------------------------------------------------------------------------

	@Header("x")
	public static class A01 {}

	@Test
	public void a01_value() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().applyAll(Header.class, A01.class).build();
		assertEquals("x", s.getName());
	}

	@Header("x")
	@Schema(
		t="number",
		f="int32",
		cf="csv",
		max="1",
		min="2",
		mo="3",
		p="4",
		maxl=1,
		minl=2,
		maxi=3,
		mini=4,
		emax=true,
		emin=true,
		ui=true,
		d={"b1","b2"},
		df={"c1","c2"},
		items=@Items($ref="d1"),
		e="e1,e2,e3"
	)
	public static class A02 {}

	@Test
	public void a02_basic_onClass() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().applyAll(Header.class, A02.class).noValidate().build();
		assertEquals("x", s.getName());
		assertEquals(HttpPartDataType.NUMBER, s.getType());
		assertEquals(HttpPartFormat.INT32, s.getFormat());
		assertEquals(HttpPartCollectionFormat.CSV, s.getCollectionFormat());
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
		assertObject(s.getEnum()).asJson().is("['e1','e2','e3']");
		assertEquals("c1\nc2", s.getDefault());
	}

	public static class A03 {
		public void a(
				@Header("x")
				@Schema(
					t="number",
					f="int32",
					cf="csv",
					max="1",
					min="2",
					mo="3",
					p="4",
					maxl=1,
					minl=2,
					maxi=3,
					mini=4,
					emax=true,
					emin=true,
					ui=true,
					d={"b1","b2"},
					df={"c1","c2"},
					items=@Items($ref="d1"),
					e="e1,e2,e3"
				)
				String x
			) {

		}
	}

	@Test
	public void a03_basic_onParameter() throws Exception {
		ParamInfo mpi = MethodInfo.of(A03.class.getMethod("a", String.class)).getParam(0);
		HttpPartSchema s = HttpPartSchema.create().applyAll(Header.class, mpi).noValidate().build();
		assertEquals("x", s.getName());
		assertEquals(HttpPartDataType.NUMBER, s.getType());
		assertEquals(HttpPartFormat.INT32, s.getFormat());
		assertEquals(HttpPartCollectionFormat.CSV, s.getCollectionFormat());
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
		assertObject(s.getEnum()).asJson().is("['e1','e2','e3']");
		assertEquals("c1\nc2", s.getDefault());
	}

	public static class A04 {
		public void a(
				@Header("y")
				@Schema(
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
					d={"b3","b3"},
					df={"c3","c4"},
					items=@Items($ref="d2"),
					e="e4,e5,e6"
				)
				A01 x
			) {

		}
	}

	@Test
	public void a04_basic_onParameterAndClass() throws Exception {
		ParamInfo mpi = MethodInfo.of(A04.class.getMethod("a", A01.class)).getParam(0);
		HttpPartSchema s = HttpPartSchema.create().applyAll(Header.class, mpi).noValidate().build();
		assertEquals("y", s.getName());
		assertEquals(HttpPartDataType.INTEGER, s.getType());
		assertEquals(HttpPartFormat.INT64, s.getFormat());
		assertEquals(HttpPartCollectionFormat.SSV, s.getCollectionFormat());
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
		assertObject(s.getEnum()).asJson().is("['e4','e5','e6']");
		assertEquals("c3\nc4", s.getDefault());
	}

	@Header("x")
	@Schema(
		items=@Items(
			t="number",
			f="int32",
			cf="csv",
			max="1",
			min="2",
			mo="3",
			p="4",
			maxl=1,
			minl=2,
			maxi=3,
			mini=4,
			emax=true,
			emin=true,
			ui=true,
			df={"c1","c2"},
			e="e1,e2",
			items=@SubItems(
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
		HttpPartSchema s = HttpPartSchema.create().applyAll(Header.class, A05.class).noValidate().build();
		assertEquals("x", s.getName());

		HttpPartSchema items = s.getItems();
		assertEquals(HttpPartDataType.NUMBER, items.getType());
		assertEquals(HttpPartFormat.INT32, items.getFormat());
		assertEquals(HttpPartCollectionFormat.CSV, items.getCollectionFormat());
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
		assertObject(items.getEnum()).asJson().is("['e1','e2']");
		assertEquals("c1\nc2", items.getDefault());

		items = items.getItems();
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
		assertObject(items.getEnum()).asJson().is("['e3','e4']");
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
		assertObject(items.getEnum()).asJson().is("['e5','e6']");
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
		assertObject(items.getEnum()).asJson().is("['e7','e8']");
		assertEquals("c7\nc8", items.getDefault());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// String input validations.
	//-----------------------------------------------------------------------------------------------------------------

	@Header @Schema(p="x.*")
	public static class B02a {}

	@Test
	public void b02a_pattern() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().applyAll(Header.class, B02a.class).build();
		s.validateInput("x");
		s.validateInput("xx");
		assertThrown(()->s.validateInput("y")).asMessage().is("Value does not match expected pattern.  Must match pattern: x.*");
		assertThrown(()->s.validateInput("yx")).asMessage().is("Value does not match expected pattern.  Must match pattern: x.*");
		assertThrown(()->s.validateInput("")).asMessage().is("Empty value not allowed.");
	}

	@Header @Schema(minl=2, maxl=3)
	public static class B03a {}

	@Test
	public void b03a_length() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().applyAll(Header.class, B03a.class).build();
		s.validateInput("12");
		s.validateInput("123");
		s.validateInput(null);
		assertThrown(()->s.validateInput("1")).asMessage().is("Minimum length of value not met.");
		assertThrown(()->s.validateInput("1234")).asMessage().is("Maximum length of value exceeded.");
	}

	@Header @Schema(
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
	public static class B03b {}

	@Test
	public void b03b_length_items() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().applyAll(Header.class, B03b.class).build();

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

		assertThrown(()->s.getItems().validateInput("1")).asMessage().is("Minimum length of value not met.");
		assertThrown(()->s.getItems().getItems().validateInput("12")).asMessage().is("Minimum length of value not met.");
		assertThrown(()->s.getItems().getItems().getItems().validateInput("123")).asMessage().is("Minimum length of value not met.");
		assertThrown(()->s.getItems().getItems().getItems().getItems().validateInput("1234")).asMessage().is("Minimum length of value not met.");

		assertThrown(()->s.getItems().validateInput("1234")).asMessage().is("Maximum length of value exceeded.");
		assertThrown(()->s.getItems().getItems().validateInput("12345")).asMessage().is("Maximum length of value exceeded.");
		assertThrown(()->s.getItems().getItems().getItems().validateInput("123456")).asMessage().is("Maximum length of value exceeded.");
		assertThrown(()->s.getItems().getItems().getItems().getItems().validateInput("1234567")).asMessage().is("Maximum length of value exceeded.");
	}

	@Header @Schema(e="X,Y")
	public static class B04a {}

	@Test
	public void b04a_enum() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().applyAll(Header.class, B04a.class).build();
		s.validateInput("X");
		s.validateInput("Y");
		s.validateInput(null);
		assertThrown(()->s.validateInput("Z")).asMessage().is("Value does not match one of the expected values.  Must be one of the following:  X, Y");
	}

	@Header @Schema(e=" X , Y ")
	public static class B04b {}

	@Test
	public void b04b_enum() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().applyAll(Header.class, B04b.class).build();
		s.validateInput("X");
		s.validateInput("Y");
		s.validateInput(null);
		assertThrown(()->s.validateInput("Z")).asMessage().is("Value does not match one of the expected values.  Must be one of the following:  X, Y");
	}

	@Header @Schema(e="X,Y")
	public static class B04c {}

	@Test
	public void b04c_enum_json() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().applyAll(Header.class, B04c.class).build();
		s.validateInput("X");
		s.validateInput("Y");
		s.validateInput(null);
		assertThrown(()->s.validateInput("Z")).asMessage().is("Value does not match one of the expected values.  Must be one of the following:  X, Y");
	}

	@Header @Schema(
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
	public static class B04d {}

	@Test
	public void b04d_enum_items() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().applyAll(Header.class, B04d.class).build();

		s.getItems().validateInput("W");
		s.getItems().getItems().validateInput("X");
		s.getItems().getItems().getItems().validateInput("Y");
		s.getItems().getItems().getItems().getItems().validateInput("Z");

		assertThrown(()->s.getItems().validateInput("V")).asMessage().is("Value does not match one of the expected values.  Must be one of the following:  W");
		assertThrown(()->s.getItems().getItems().validateInput("V")).asMessage().is("Value does not match one of the expected values.  Must be one of the following:  X");
		assertThrown(()->s.getItems().getItems().getItems().validateInput("V")).asMessage().is("Value does not match one of the expected values.  Must be one of the following:  Y");
		assertThrown(()->s.getItems().getItems().getItems().getItems().validateInput("V")).asMessage().is("Value does not match one of the expected values.  Must be one of the following:  Z");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Numeric validations
	//-----------------------------------------------------------------------------------------------------------------

	@Header @Schema(min="10", max="100")
	public static class C01a {}

	@Test
	public void c01a_minmax_ints() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().applyAll(Header.class, C01a.class).build();
		s.validateOutput(10, BeanContext.DEFAULT);
		s.validateOutput(100, BeanContext.DEFAULT);
		s.validateOutput(null, BeanContext.DEFAULT);
		assertThrown(()->s.validateOutput(9, BeanContext.DEFAULT)).asMessage().is("Minimum value not met.");
		assertThrown(()->s.validateOutput(101, BeanContext.DEFAULT)).asMessage().is("Maximum value exceeded.");
	}

	@Header @Schema(
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
	public static class C01b {}

	@Test
	public void c01b_minmax_ints_items() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().applyAll(Header.class, C01b.class).build();

		s.getItems().validateOutput(10, BeanContext.DEFAULT);
		s.getItems().getItems().validateOutput(100, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().validateOutput(1000, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().getItems().validateOutput(10000, BeanContext.DEFAULT);

		s.getItems().validateOutput(100, BeanContext.DEFAULT);
		s.getItems().getItems().validateOutput(1000, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().validateOutput(10000, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().getItems().validateOutput(100000, BeanContext.DEFAULT);

		assertThrown(()->s.getItems().validateOutput(9, BeanContext.DEFAULT)).asMessage().is("Minimum value not met.");
		assertThrown(()->s.getItems().getItems().validateOutput(99, BeanContext.DEFAULT)).asMessage().is("Minimum value not met.");
		assertThrown(()->s.getItems().getItems().getItems().validateOutput(999, BeanContext.DEFAULT)).asMessage().is("Minimum value not met.");
		assertThrown(()->s.getItems().getItems().getItems().getItems().validateOutput(9999, BeanContext.DEFAULT)).asMessage().is("Minimum value not met.");

		assertThrown(()->s.getItems().validateOutput(101, BeanContext.DEFAULT)).asMessage().is("Maximum value exceeded.");
		assertThrown(()->s.getItems().getItems().validateOutput(1001, BeanContext.DEFAULT)).asMessage().is("Maximum value exceeded.");
		assertThrown(()->s.getItems().getItems().getItems().validateOutput(10001, BeanContext.DEFAULT)).asMessage().is("Maximum value exceeded.");
		assertThrown(()->s.getItems().getItems().getItems().getItems().validateOutput(100001, BeanContext.DEFAULT)).asMessage().is("Maximum value exceeded.");
	}

	@Header @Schema(min="10", max="100", emin=true, emax=true)
	public static class C02a {}

	@Test
	public void c02a_minmax_exclusive() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().applyAll(Header.class, C02a.class).build();
		s.validateOutput(11, BeanContext.DEFAULT);
		s.validateOutput(99, BeanContext.DEFAULT);
		s.validateOutput(null, BeanContext.DEFAULT);
		assertThrown(()->s.validateOutput(10, BeanContext.DEFAULT)).asMessage().is("Minimum value not met.");
		assertThrown(()->s.validateOutput(100, BeanContext.DEFAULT)).asMessage().is("Maximum value exceeded.");
	}

	@Header @Schema(
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
	public static class C02b {}

	@Test
	public void c02b_minmax_exclusive_items() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().applyAll(Header.class, C02b.class).build();

		s.getItems().validateOutput(11, BeanContext.DEFAULT);
		s.getItems().getItems().validateOutput(101, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().validateOutput(1001, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().getItems().validateOutput(10001, BeanContext.DEFAULT);

		s.getItems().validateOutput(99, BeanContext.DEFAULT);
		s.getItems().getItems().validateOutput(999, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().validateOutput(9999, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().getItems().validateOutput(99999, BeanContext.DEFAULT);

		assertThrown(()->s.getItems().validateOutput(10, BeanContext.DEFAULT)).asMessage().is("Minimum value not met.");
		assertThrown(()->s.getItems().getItems().validateOutput(100, BeanContext.DEFAULT)).asMessage().is("Minimum value not met.");
		assertThrown(()->s.getItems().getItems().getItems().validateOutput(1000, BeanContext.DEFAULT)).asMessage().is("Minimum value not met.");
		assertThrown(()->s.getItems().getItems().getItems().getItems().validateOutput(10000, BeanContext.DEFAULT)).asMessage().is("Minimum value not met.");

		assertThrown(()->s.getItems().validateOutput(100, BeanContext.DEFAULT)).asMessage().is("Maximum value exceeded.");
		assertThrown(()->s.getItems().getItems().validateOutput(1000, BeanContext.DEFAULT)).asMessage().is("Maximum value exceeded.");
		assertThrown(()->s.getItems().getItems().getItems().validateOutput(10000, BeanContext.DEFAULT)).asMessage().is("Maximum value exceeded.");
		assertThrown(()->s.getItems().getItems().getItems().getItems().validateOutput(100000, BeanContext.DEFAULT)).asMessage().is("Maximum value exceeded.");
	}

	@Header @Schema(min="10.1", max="100.1")
	public static class C03a {}

	@Test
	public void c03_minmax_floats() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().applyAll(Header.class, C03a.class).build();
		s.validateOutput(10.1f, BeanContext.DEFAULT);
		s.validateOutput(100.1f, BeanContext.DEFAULT);
		s.validateOutput(null, BeanContext.DEFAULT);
		assertThrown(()->s.validateOutput(10f, BeanContext.DEFAULT)).asMessage().is("Minimum value not met.");
		assertThrown(()->s.validateOutput(100.2f, BeanContext.DEFAULT)).asMessage().is("Maximum value exceeded.");
	}

	@Header @Schema(
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
	public static class C03b {}

	@Test
	public void c03b_minmax_floats_items() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().applyAll(Header.class, C03b.class).build();

		s.getItems().validateOutput(10.1f, BeanContext.DEFAULT);
		s.getItems().getItems().validateOutput(100.1f, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().validateOutput(1000.1f, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().getItems().validateOutput(10000.1f, BeanContext.DEFAULT);

		s.getItems().validateOutput(100.1f, BeanContext.DEFAULT);
		s.getItems().getItems().validateOutput(1000.1f, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().validateOutput(10000.1f, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().getItems().validateOutput(100000.1f, BeanContext.DEFAULT);

		assertThrown(()->s.getItems().validateOutput(10f, BeanContext.DEFAULT)).asMessage().is("Minimum value not met.");
		assertThrown(()->s.getItems().getItems().validateOutput(100f, BeanContext.DEFAULT)).asMessage().is("Minimum value not met.");
		assertThrown(()->s.getItems().getItems().getItems().validateOutput(1000f, BeanContext.DEFAULT)).asMessage().is("Minimum value not met.");
		assertThrown(()->s.getItems().getItems().getItems().getItems().validateOutput(10000f, BeanContext.DEFAULT)).asMessage().is("Minimum value not met.");

		assertThrown(()->s.getItems().validateOutput(100.2f, BeanContext.DEFAULT)).asMessage().is("Maximum value exceeded.");
		assertThrown(()->s.getItems().getItems().validateOutput(1000.2f, BeanContext.DEFAULT)).asMessage().is("Maximum value exceeded.");
		assertThrown(()->s.getItems().getItems().getItems().validateOutput(10000.2f, BeanContext.DEFAULT)).asMessage().is("Maximum value exceeded.");
		assertThrown(()->s.getItems().getItems().getItems().getItems().validateOutput(100000.2f, BeanContext.DEFAULT)).asMessage().is("Maximum value exceeded.");
	}

	@Header @Schema(min="10.1", max="100.1", emin=true, emax=true)
	public static class C04a {}

	@Test
	public void c04a_minmax_floats_exclusive() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().applyAll(Header.class, C04a.class).build();
		s.validateOutput(10.2f, BeanContext.DEFAULT);
		s.validateOutput(100f, BeanContext.DEFAULT);
		s.validateOutput(null, BeanContext.DEFAULT);
		assertThrown(()->s.validateOutput(10.1f, BeanContext.DEFAULT)).asMessage().is("Minimum value not met.");
		assertThrown(()->s.validateOutput(100.1f, BeanContext.DEFAULT)).asMessage().is("Maximum value exceeded.");
	}

	@Header @Schema(
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
	public static class C04b {}

	@Test
	public void c04b_minmax_floats_exclusive_items() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().applyAll(Header.class, C04b.class).build();

		s.getItems().validateOutput(10.2f, BeanContext.DEFAULT);
		s.getItems().getItems().validateOutput(100.2f, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().validateOutput(1000.2f, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().getItems().validateOutput(10000.2f, BeanContext.DEFAULT);

		s.getItems().validateOutput(100f, BeanContext.DEFAULT);
		s.getItems().getItems().validateOutput(1000f, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().validateOutput(10000f, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().getItems().validateOutput(100000f, BeanContext.DEFAULT);

		assertThrown(()->s.getItems().validateOutput(10.1f, BeanContext.DEFAULT)).asMessage().is("Minimum value not met.");
		assertThrown(()->s.getItems().getItems().validateOutput(100.1f, BeanContext.DEFAULT)).asMessage().is("Minimum value not met.");
		assertThrown(()->s.getItems().getItems().getItems().validateOutput(1000.1f, BeanContext.DEFAULT)).asMessage().is("Minimum value not met.");
		assertThrown(()->s.getItems().getItems().getItems().getItems().validateOutput(10000.1f, BeanContext.DEFAULT)).asMessage().is("Minimum value not met.");

		assertThrown(()->s.getItems().validateOutput(100.1f, BeanContext.DEFAULT)).asMessage().is("Maximum value exceeded.");
		assertThrown(()->s.getItems().getItems().validateOutput(1000.1f, BeanContext.DEFAULT)).asMessage().is("Maximum value exceeded.");
		assertThrown(()->s.getItems().getItems().getItems().validateOutput(10000.1f, BeanContext.DEFAULT)).asMessage().is("Maximum value exceeded.");
		assertThrown(()->s.getItems().getItems().getItems().getItems().validateOutput(100000.1f, BeanContext.DEFAULT)).asMessage().is("Maximum value exceeded.");
	}

	@Header @Schema(mo="10")
	public static class C05a {}

	@Test
	public void c05a_multipleOf() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().applyAll(Header.class, C05a.class).build();
		s.validateOutput(0, BeanContext.DEFAULT);
		s.validateOutput(10, BeanContext.DEFAULT);
		s.validateOutput(20, BeanContext.DEFAULT);
		s.validateOutput(10f, BeanContext.DEFAULT);
		s.validateOutput(20f, BeanContext.DEFAULT);
		s.validateOutput(null, BeanContext.DEFAULT);
		assertThrown(()->s.validateOutput(11, BeanContext.DEFAULT)).asMessage().is("Multiple-of not met.");
	}

	@Header @Schema(
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
	public static class C05b {}

	@Test
	public void c05b_multipleOf_items() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().applyAll(Header.class, C05b.class).build();

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

		assertThrown(()->s.getItems().validateOutput(11, BeanContext.DEFAULT)).asMessage().is("Multiple-of not met.");
		assertThrown(()->s.getItems().getItems().validateOutput(101, BeanContext.DEFAULT)).asMessage().is("Multiple-of not met.");
		assertThrown(()->s.getItems().getItems().getItems().validateOutput(1001, BeanContext.DEFAULT)).asMessage().is("Multiple-of not met.");
		assertThrown(()->s.getItems().getItems().getItems().getItems().validateOutput(10001, BeanContext.DEFAULT)).asMessage().is("Multiple-of not met.");
	}

	@Header @Schema(mo="10.1")
	public static class C06a {}

	@Test
	public void c06a_multipleOf_floats() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().applyAll(Header.class, C06a.class).build();
		s.validateOutput(0, BeanContext.DEFAULT);
		s.validateOutput(10.1f, BeanContext.DEFAULT);
		s.validateOutput(20.2f, BeanContext.DEFAULT);
		s.validateOutput(null, BeanContext.DEFAULT);
		assertThrown(()->s.validateOutput(10.2f, BeanContext.DEFAULT)).asMessage().is("Multiple-of not met.");
	}

	@Header @Schema(
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
	public static class C06b {}

	@Test
	public void c06b_multipleOf_floats_items() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().applyAll(Header.class, C06b.class).build();

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

		assertThrown(()->s.getItems().validateOutput(10.2f, BeanContext.DEFAULT)).asMessage().is("Multiple-of not met.");
		assertThrown(()->s.getItems().getItems().validateOutput(100.2f, BeanContext.DEFAULT)).asMessage().is("Multiple-of not met.");
		assertThrown(()->s.getItems().getItems().getItems().validateOutput(1000.2f, BeanContext.DEFAULT)).asMessage().is("Multiple-of not met.");
		assertThrown(()->s.getItems().getItems().getItems().getItems().validateOutput(10000.2f, BeanContext.DEFAULT)).asMessage().is("Multiple-of not met.");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Collections/Array validations
	//-----------------------------------------------------------------------------------------------------------------

	@Header @Schema(
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
	public static class D01 {}

	@Test
	public void d01a_uniqueItems_arrays() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().applyAll(Header.class, D01.class).build();

		String[] good = split("a,b"), bad = split("a,a");

		s.getItems().validateOutput(good, BeanContext.DEFAULT);
		s.getItems().getItems().validateOutput(good, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().validateOutput(good, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().getItems().validateOutput(good, BeanContext.DEFAULT);
		s.getItems().validateOutput(null, BeanContext.DEFAULT);

		assertThrown(()->s.getItems().validateOutput(bad, BeanContext.DEFAULT)).asMessage().is("Duplicate items not allowed.");
		assertThrown(()->s.getItems().getItems().validateOutput(bad, BeanContext.DEFAULT)).asMessage().is("Duplicate items not allowed.");
		assertThrown(()->s.getItems().getItems().getItems().validateOutput(bad, BeanContext.DEFAULT)).asMessage().is("Duplicate items not allowed.");
		assertThrown(()->s.getItems().getItems().getItems().getItems().validateOutput(bad, BeanContext.DEFAULT)).asMessage().is("Duplicate items not allowed.");
	}

	@Test
	public void d01b_uniqueItems_collections() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().applyAll(Header.class, D01.class).build();

		List<String>
			good = alist("a","b"),
			bad = alist("a","a");

		s.getItems().validateOutput(good, BeanContext.DEFAULT);
		s.getItems().getItems().validateOutput(good, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().validateOutput(good, BeanContext.DEFAULT);
		s.getItems().getItems().getItems().getItems().validateOutput(good, BeanContext.DEFAULT);
		s.getItems().validateOutput(null, BeanContext.DEFAULT);

		assertThrown(()->s.getItems().validateOutput(bad, BeanContext.DEFAULT)).asMessage().is("Duplicate items not allowed.");
		assertThrown(()->s.getItems().getItems().validateOutput(bad, BeanContext.DEFAULT)).asMessage().is("Duplicate items not allowed.");
		assertThrown(()->s.getItems().getItems().getItems().validateOutput(bad, BeanContext.DEFAULT)).asMessage().is("Duplicate items not allowed.");
		assertThrown(()->s.getItems().getItems().getItems().getItems().validateOutput(bad, BeanContext.DEFAULT)).asMessage().is("Duplicate items not allowed.");
	}

	@Header @Schema(
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
	public static class D02 {}

	@Test
	public void d02a_minMaxItems_arrays() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().applyAll(Header.class, D02.class).build();

		s.getItems().validateOutput(split("1"), BeanContext.DEFAULT);
		s.getItems().getItems().validateOutput(split("1,2"), BeanContext.DEFAULT);
		s.getItems().getItems().getItems().validateOutput(split("1,2,3"), BeanContext.DEFAULT);
		s.getItems().getItems().getItems().getItems().validateOutput(split("1,2,3,4"), BeanContext.DEFAULT);

		s.getItems().validateOutput(split("1,2"), BeanContext.DEFAULT);
		s.getItems().getItems().validateOutput(split("1,2,3"), BeanContext.DEFAULT);
		s.getItems().getItems().getItems().validateOutput(split("1,2,3,4"), BeanContext.DEFAULT);
		s.getItems().getItems().getItems().getItems().validateOutput(split("1,2,3,4,5"), BeanContext.DEFAULT);

		assertThrown(()->s.getItems().validateOutput(new String[0], BeanContext.DEFAULT)).asMessage().is("Minimum number of items not met.");
		assertThrown(()->s.getItems().getItems().validateOutput(split("1"), BeanContext.DEFAULT)).asMessage().is("Minimum number of items not met.");
		assertThrown(()->s.getItems().getItems().getItems().validateOutput(split("1,2"), BeanContext.DEFAULT)).asMessage().is("Minimum number of items not met.");
		assertThrown(()->s.getItems().getItems().getItems().getItems().validateOutput(split("1,2,3"), BeanContext.DEFAULT)).asMessage().is("Minimum number of items not met.");

		assertThrown(()->s.getItems().validateOutput(split("1,2,3"), BeanContext.DEFAULT)).asMessage().is("Maximum number of items exceeded.");
		assertThrown(()->s.getItems().getItems().validateOutput(split("1,2,3,4"), BeanContext.DEFAULT)).asMessage().is("Maximum number of items exceeded.");
		assertThrown(()->s.getItems().getItems().getItems().validateOutput(split("1,2,3,4,5"), BeanContext.DEFAULT)).asMessage().is("Maximum number of items exceeded.");
		assertThrown(()->s.getItems().getItems().getItems().getItems().validateOutput(split("1,2,3,4,5,6"), BeanContext.DEFAULT)).asMessage().is("Maximum number of items exceeded.");
	}
}