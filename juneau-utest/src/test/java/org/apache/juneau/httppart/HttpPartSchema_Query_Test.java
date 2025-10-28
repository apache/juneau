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
package org.apache.juneau.httppart;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.common.utils.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.reflect.*;
import org.junit.jupiter.api.*;

class HttpPartSchema_Query_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Basic test
	//-----------------------------------------------------------------------------------------------------------------
	@Test void a01_basic() {
		assertDoesNotThrow(()->HttpPartSchema.create().build());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Query
	//-----------------------------------------------------------------------------------------------------------------

	@Query("x")
	public static class A01 {}

	@Test void a01_value() {
		var s = HttpPartSchema.create().applyAll(Query.class, A01.class).build();
		assertEquals("x", s.getName());
	}

	@Query("x")
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
		r=true,
		sie=true,
		d={"b1","b2"},
		df={"c1","c2"},
		items=@Items($ref="d1"),
		e="e1,e2,e3"
	)
	public static class A02 {}

	@Test void a02_basic_onClass() {
		assertBean(
			HttpPartSchema.create().applyAll(Query.class, A02.class).noValidate().build(),
			"name,type,format,collectionFormat,maximum,minimum,multipleOf,pattern,maxLength,minLength,maxItems,minItems,exclusiveMaximum,exclusiveMinimum,uniqueItems,required,skipIfEmpty,enum,default",
			"x,NUMBER,INT32,CSV,1,2,3,4,1,2,3,4,true,true,true,true,true,[e1,e2,e3],c1\nc2"
		);
	}

	public static class A03 {
		public void a(
				@Query("x")
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
					r=true,
					sie=true,
					d={"b1","b2"},
					df={"c1","c2"},
					items=@Items($ref="d1"),
					e="e1,e2,e3"
				)
				String x
			) {
			/* no-op */
		}
	}

	@Test void a03_basic_onParameter() throws Exception {
		var mpi = MethodInfo.of(A03.class.getMethod("a", String.class)).getParam(0);
		assertBean(
			HttpPartSchema.create().applyAll(Query.class, mpi).noValidate().build(),
			"name,type,format,collectionFormat,maximum,minimum,multipleOf,pattern,maxLength,minLength,maxItems,minItems,exclusiveMaximum,exclusiveMinimum,uniqueItems,required,skipIfEmpty,enum,default",
			"x,NUMBER,INT32,CSV,1,2,3,4,1,2,3,4,true,true,true,true,true,[e1,e2,e3],c1\nc2"
		);
	}

	public static class A04 {
		public void a(
				@Query("y")
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
					r=false,
					sie=false,
					d={"b3","b3"},
					df={"c3","c4"},
					items=@Items($ref="d2"),
					e="e4,e5,e6"
				)
				A01 x
			) {
			/* no-op */
		}
	}

	@Test void a04_basic_onParameterAndClass() throws Exception {
		var mpi = MethodInfo.of(A04.class.getMethod("a", A01.class)).getParam(0);
		assertBean(
			HttpPartSchema.create().applyAll(Query.class, mpi).noValidate().build(),
			"name,type,format,collectionFormat,maximum,minimum,multipleOf,pattern,maxLength,minLength,maxItems,minItems,exclusiveMaximum,exclusiveMinimum,uniqueItems,required,skipIfEmpty,enum,default",
			"y,INTEGER,INT64,SSV,5,6,7,8,5,6,7,8,false,false,false,false,false,[e4,e5,e6],c3\nc4"
		);
	}

	@Query("x")
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

	@Test void a05_basic_nestedItems_onClass() {
		var s = HttpPartSchema.create().applyAll(Query.class, A05.class).noValidate().build();
		assertEquals("x", s.getName());

		var items = s.getItems();
		assertBean(
			items,
			"type,format,collectionFormat,maximum,minimum,multipleOf,pattern,maxLength,minLength,maxItems,minItems,exclusiveMaximum,exclusiveMinimum,uniqueItems,enum,default",
			"NUMBER,INT32,CSV,1,2,3,4,1,2,3,4,true,true,true,[e1,e2],c1\nc2"
		);

		items = items.getItems();
		assertBean(
			items,
			"type,format,collectionFormat,maximum,minimum,multipleOf,pattern,maxLength,minLength,maxItems,minItems,exclusiveMaximum,exclusiveMinimum,uniqueItems,enum,default",
			"INTEGER,INT64,SSV,5,6,7,8,5,6,7,8,false,false,false,[e3,e4],c3\nc4"
		);

		items = items.getItems();
		assertBean(
			items,
			"type,format,collectionFormat,maximum,minimum,multipleOf,pattern,maxLength,minLength,maxItems,minItems,exclusiveMaximum,exclusiveMinimum,uniqueItems,enum,default",
			"STRING,FLOAT,TSV,9,10,11,12,9,10,11,12,true,true,true,[e5,e6],c5\nc6"
		);

		items = items.getItems();
		assertBean(
			items,
			"type,format,collectionFormat,maximum,minimum,multipleOf,pattern,maxLength,minLength,maxItems,minItems,exclusiveMaximum,exclusiveMinimum,uniqueItems,enum,default",
			"ARRAY,DOUBLE,PIPES,13,14,15,16,13,14,15,16,false,false,false,[e7,e8],c7\nc8"
		);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// String input validations.
	//-----------------------------------------------------------------------------------------------------------------

	@Query @Schema(r=true)
	public static class B01a {}

	@Test void b01a_required() throws Exception {
		var s = HttpPartSchema.create().applyAll(Query.class, B01a.class).build();

		s.validateInput("x");

		assertThrowsWithMessage(SchemaValidationException.class, "No value specified.", ()->s.validateInput(null));
		assertThrowsWithMessage(SchemaValidationException.class, "Empty value not allowed.", ()->s.validateInput(""));
	}

	@Query @Schema(aev=true)
	public static class B01b {}

	@Test void b01b_allowEmptyValue() throws Exception {
		var s = HttpPartSchema.create().applyAll(Query.class, B01b.class).build();

		s.validateInput("");
		s.validateInput(null);
	}

	@Query @Schema(r=true,aev=true)
	public static class B01c {}

	@Test void b01b_required_allowEmptyValue() throws Exception {
		var s = HttpPartSchema.create().applyAll(Query.class, B01c.class).build();

		s.validateInput("");

		assertThrowsWithMessage(SchemaValidationException.class, "No value specified.", ()->s.validateInput(null));
	}

	@Query @Schema(p="x.*")
	public static class B02a {}

	@Test void b02a_pattern() throws Exception {
		var s = HttpPartSchema.create().applyAll(Query.class, B02a.class).build();
		s.validateInput("x");
		s.validateInput("xx");
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected pattern.  Must match pattern: x.*", ()->s.validateInput("y"));
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected pattern.  Must match pattern: x.*", ()->s.validateInput("yx"));
		assertThrowsWithMessage(SchemaValidationException.class, "Empty value not allowed.", ()->s.validateInput(""));
	}

	@Query @Schema(minl=2, maxl=3)
	public static class B03a {}

	@Test void b03a_length() throws Exception {
		var s = HttpPartSchema.create().applyAll(Query.class, B03a.class).build();
		s.validateInput("12");
		s.validateInput("123");
		s.validateInput(null);
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum length of value not met.", ()->s.validateInput("1"));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum length of value exceeded.", ()->s.validateInput("1234"));
	}

	@Query
	@Schema(
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

	@Test void b03b_length_items() throws Exception {
		var s = HttpPartSchema.create().applyAll(Query.class, B03b.class).build();

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

	@Query @Schema(e="X,Y")
	public static class B04a {}

	@Test void b04a_enum() throws Exception {
		var s = HttpPartSchema.create().applyAll(Query.class, B04a.class).build();
		s.validateInput("X");
		s.validateInput("Y");
		s.validateInput(null);
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match one of the expected values.  Must be one of the following:  X, Y", ()->s.validateInput("Z"));
	}

	@Query @Schema(e=" X , Y ")
	public static class B04b {}

	@Test void b04b_enum() throws Exception {
		var s = HttpPartSchema.create().applyAll(Query.class, B04b.class).build();
		s.validateInput("X");
		s.validateInput("Y");
		s.validateInput(null);
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match one of the expected values.  Must be one of the following:  X, Y", ()->s.validateInput("Z"));
	}

	@Query @Schema(e="X,Y")
	public static class B04c {}

	@Test void b04c_enum_json() throws Exception {
		var s = HttpPartSchema.create().applyAll(Query.class, B04c.class).build();
		s.validateInput("X");
		s.validateInput("Y");
		s.validateInput(null);
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match one of the expected values.  Must be one of the following:  X, Y", ()->s.validateInput("Z"));
	}

	@Query
	@Schema(
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

	@Test void b04d_enum_items() throws Exception {
		var s = HttpPartSchema.create().applyAll(Query.class, B04d.class).build();

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

	@Query @Schema(min="10", max="100")
	public static class C01a {}

	@Test void c01a_minmax_ints() throws Exception {
		var s = HttpPartSchema.create().applyAll(Query.class, C01a.class).build();
		s.validateOutput(10, BeanContext.DEFAULT);
		s.validateOutput(100, BeanContext.DEFAULT);
		s.validateOutput(null, BeanContext.DEFAULT);
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.validateOutput(9, BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.validateOutput(101, BeanContext.DEFAULT));
	}

	@Query
	@Schema(
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

	@Test void c01b_minmax_ints_items() throws Exception {
		var s = HttpPartSchema.create().applyAll(Query.class, C01b.class).build();

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

	@Query @Schema(min="10", max="100", emin=true, emax=true)
	public static class C02a {}

	@Test void c02a_minmax_exclusive() throws Exception {
		var s = HttpPartSchema.create().applyAll(Query.class, C02a.class).build();
		s.validateOutput(11, BeanContext.DEFAULT);
		s.validateOutput(99, BeanContext.DEFAULT);
		s.validateOutput(null, BeanContext.DEFAULT);
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.validateOutput(10, BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.validateOutput(100, BeanContext.DEFAULT));
	}

	@Query
	@Schema(
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

	@Test void c02b_minmax_exclusive_items() throws Exception {
		var s = HttpPartSchema.create().applyAll(Query.class, C02b.class).build();

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

	@Query @Schema(min="10.1", max="100.1")
	public static class C03a {}

	@Test void c03_minmax_floats() throws Exception {
		var s = HttpPartSchema.create().applyAll(Query.class, C03a.class).build();
		s.validateOutput(10.1f, BeanContext.DEFAULT);
		s.validateOutput(100.1f, BeanContext.DEFAULT);
		s.validateOutput(null, BeanContext.DEFAULT);
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.validateOutput(10f, BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.validateOutput(100.2f, BeanContext.DEFAULT));
	}

	@Query
	@Schema(
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

	@Test void c03b_minmax_floats_items() throws Exception {
		var s = HttpPartSchema.create().applyAll(Query.class, C03b.class).build();

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

	@Query @Schema(min="10.1", max="100.1", emin=true, emax=true)
	public static class C04a {}

	@Test void c04a_minmax_floats_exclusive() throws Exception {
		var s = HttpPartSchema.create().applyAll(Query.class, C04a.class).build();
		s.validateOutput(10.2f, BeanContext.DEFAULT);
		s.validateOutput(100f, BeanContext.DEFAULT);
		s.validateOutput(null, BeanContext.DEFAULT);
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.validateOutput(10.1f, BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.validateOutput(100.1f, BeanContext.DEFAULT));
	}

	@Query
	@Schema(
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

	@Test void c04b_minmax_floats_exclusive_items() throws Exception {
		var s = HttpPartSchema.create().applyAll(Query.class, C04b.class).build();

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

	@Query @Schema(mo="10")
	public static class C05a {}

	@Test void c05a_multipleOf() throws Exception {
		var s = HttpPartSchema.create().applyAll(Query.class, C05a.class).build();
		s.validateOutput(0, BeanContext.DEFAULT);
		s.validateOutput(10, BeanContext.DEFAULT);
		s.validateOutput(20, BeanContext.DEFAULT);
		s.validateOutput(10f, BeanContext.DEFAULT);
		s.validateOutput(20f, BeanContext.DEFAULT);
		s.validateOutput(null, BeanContext.DEFAULT);
		assertThrowsWithMessage(SchemaValidationException.class, "Multiple-of not met.", ()->s.validateOutput(11, BeanContext.DEFAULT));
	}

	@Query
	@Schema(
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

	@Test void c05b_multipleOf_items() throws Exception {
		var s = HttpPartSchema.create().applyAll(Query.class, C05b.class).build();

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

	@Query @Schema(mo="10.1")
	public static class C06a {}

	@Test void c06a_multipleOf_floats() throws Exception {
		var s = HttpPartSchema.create().applyAll(Query.class, C06a.class).build();
		s.validateOutput(0, BeanContext.DEFAULT);
		s.validateOutput(10.1f, BeanContext.DEFAULT);
		s.validateOutput(20.2f, BeanContext.DEFAULT);
		s.validateOutput(null, BeanContext.DEFAULT);
		assertThrowsWithMessage(SchemaValidationException.class, "Multiple-of not met.", ()->s.validateOutput(10.2f, BeanContext.DEFAULT));
	}

	@Query
	@Schema(
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

	@Test void c06b_multipleOf_floats_items() throws Exception {
		var s = HttpPartSchema.create().applyAll(Query.class, C06b.class).build();

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

	@Query
	@Schema(
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

	@Test void d01a_uniqueItems_arrays() throws Exception {
		var s = HttpPartSchema.create().applyAll(Query.class, D01.class).build();

		var good = StringUtils.split("a,b");
		var bad = StringUtils.split("a,a");

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
		var s = HttpPartSchema.create().applyAll(Query.class, D01.class).build();

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

	@Query
	@Schema(
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

	@Test void d02a_minMaxItems_arrays() throws Exception {
		var s = HttpPartSchema.create().applyAll(Query.class, D02.class).build();

		s.getItems().validateOutput(StringUtils.split("1"), BeanContext.DEFAULT);
		s.getItems().getItems().validateOutput(StringUtils.split("1,2"), BeanContext.DEFAULT);
		s.getItems().getItems().getItems().validateOutput(StringUtils.split("1,2,3"), BeanContext.DEFAULT);
		s.getItems().getItems().getItems().getItems().validateOutput(StringUtils.split("1,2,3,4"), BeanContext.DEFAULT);

		s.getItems().validateOutput(StringUtils.split("1,2"), BeanContext.DEFAULT);
		s.getItems().getItems().validateOutput(StringUtils.split("1,2,3"), BeanContext.DEFAULT);
		s.getItems().getItems().getItems().validateOutput(StringUtils.split("1,2,3,4"), BeanContext.DEFAULT);
		s.getItems().getItems().getItems().getItems().validateOutput(StringUtils.split("1,2,3,4,5"), BeanContext.DEFAULT);

		assertThrowsWithMessage(SchemaValidationException.class, "Minimum number of items not met.", ()->s.getItems().validateOutput(new String[0], BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum number of items not met.", ()->s.getItems().getItems().validateOutput(StringUtils.split("1"), BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum number of items not met.", ()->s.getItems().getItems().getItems().validateOutput(StringUtils.split("1,2"), BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum number of items not met.", ()->s.getItems().getItems().getItems().getItems().validateOutput(StringUtils.split("1,2,3"), BeanContext.DEFAULT));

		assertThrowsWithMessage(SchemaValidationException.class, "Maximum number of items exceeded.", ()->s.getItems().validateOutput(StringUtils.split("1,2,3"), BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum number of items exceeded.", ()->s.getItems().getItems().validateOutput(StringUtils.split("1,2,3,4"), BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum number of items exceeded.", ()->s.getItems().getItems().getItems().validateOutput(StringUtils.split("1,2,3,4,5"), BeanContext.DEFAULT));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum number of items exceeded.", ()->s.getItems().getItems().getItems().getItems().validateOutput(StringUtils.split("1,2,3,4,5,6"), BeanContext.DEFAULT));
	}
}