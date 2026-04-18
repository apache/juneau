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
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.annotation.Items;
import org.apache.juneau.commons.annotation.SubItems;
import org.apache.juneau.commons.annotation.Schema;
import org.apache.juneau.commons.httppart.SchemaValidationException;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.http.annotation.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"java:S5961", // High assertion count acceptable in comprehensive tests
	"java:S1186", // Empty method body intentional for callback testing
	"removal" // Tests deprecated boolean-style exclusiveMaximum/exclusiveMinimum for backward compatibility
})
class HttpPartSchema_Body_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Basic test
	//-----------------------------------------------------------------------------------------------------------------
	@Test void a01_basic() {
		assertDoesNotThrow(()->HttpPartSchema.create().build());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Body
	//-----------------------------------------------------------------------------------------------------------------

	@Content
	@Schema(
		d={"b1","b2"},
		$ref="c1",
		r=true
	)
	public static class A02 {}

	@Test void a02_basic_onClass() {
		var s = HttpPartSchema.create().applyAll(Content.class, A02.class).noValidate().build();
		assertTrue(s.isRequired());
	}

	public static class A03 {
		public void a(
				@Content
				@Schema(
					d={"b1","b2"},
					$ref="c1",
					r=true
				)
				String x
			) {

		}
	}

	@Test void a03_basic_onParameter() throws Exception {
		var mpi = MethodInfo.of(A03.class.getMethod("a", String.class)).getParameter(0);
		var s = HttpPartSchema.create().applyAll(Content.class, mpi).noValidate().build();
		assertTrue(s.isRequired());
	}

	public static class A04 {
		public void a(
				@Content
				@Schema(
					d={"b3","b3"},
					$ref="c3",
					r=true
				)
				A02 x
			) {

		}
	}

	@Test void a04_basic_onParameterAndClass() throws Exception {
		var mpi = MethodInfo.of(A04.class.getMethod("a", A02.class)).getParameter(0);
		var s = HttpPartSchema.create().applyAll(Content.class, mpi).noValidate().build();
		assertTrue(s.isRequired());
	}

	@Content
	@Schema(
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
				e="e5,e6"
			)
		)
	)
	public static class A05 {}

	@Test void a05_basic_nestedItems_onClass() {
		var s = HttpPartSchema.create().applyAll(Content.class, A05.class).noValidate().build();

		assertBean(
			s,
			"type,format,maximum,minimum,multipleOf,pattern,maxLength,minLength,maxItems,minItems,maxProperties,minProperties,exclusiveMaximum,exclusiveMinimum,uniqueItems,enum,default",
			"NUMBER,INT32,1,2,3,4,1,2,3,4,5,6,true,true,true,[e1,e2],c1\nc2"
		);

		var items = s.getItems();
		assertBean(
			items,
			"type,format,collectionFormat,maximum,minimum,multipleOf,pattern,maxLength,minLength,maxItems,minItems,exclusiveMaximum,exclusiveMinimum,uniqueItems,enum,default",
			"INTEGER,INT64,SSV,5,6,7,8,5,6,7,8,false,false,false,[e3,e4],c3\nc4"
		);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// String input validations.
	//-----------------------------------------------------------------------------------------------------------------

	@Content @Schema(required=true)
	public static class B01a {}

	@Test void b01a_required() throws Exception {
		var s = HttpPartSchema.create().applyAll(Content.class, B01a.class).build();

		s.validateInput("x");
		assertThrowsWithMessage(SchemaValidationException.class, "No value specified.", ()->s.validateInput(null));
		assertThrowsWithMessage(SchemaValidationException.class, "Empty value not allowed.", ()->s.validateInput(""));
	}

	@Content
	@Schema(p="x.*",aev=true)
	public static class B02a {}

	@Test void b02a_pattern() throws Exception {
		var s = HttpPartSchema.create().applyAll(Content.class, B02a.class).build();

		s.validateInput("x");
		s.validateInput("xx");

		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected pattern.  Must match pattern: x.*", ()->s.validateInput(""));
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected pattern.  Must match pattern: x.*", ()->s.validateInput("y"));
	}

	@Content
	@Schema(
		items=@Items(
			p="w.*",
			items=@SubItems(
				p="x.*"
			)
		)
	)
	public static class B02b {}

	@Content
	@Schema(
		minl=2, maxl=3
	)
	public static class B03a {}

	@Test void b03a_length() throws Exception {
		var s = HttpPartSchema.create().applyAll(Content.class, B03a.class).build();
		s.validateInput("12");
		s.validateInput("123");
		s.validateInput(null);
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum length of value not met.", ()->s.validateInput("1"));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum length of value exceeded.", ()->s.validateInput("1234"));
	}

	@Content
	@Schema(
		items=@Items(
			minl=2, maxl=3,
			items=@SubItems(
				minl=3, maxl=4
			)
		)
	)
	public static class B03b {}

	@Test void b03b_length_items() throws Exception {
		var s = HttpPartSchema.create().applyAll(Content.class, B03b.class).build();

		s.getItems().validateInput("12");
		s.getItems().getItems().validateInput("123");

		s.getItems().validateInput("123");
		s.getItems().getItems().validateInput("1234");

		s.getItems().validateInput(null);
		s.getItems().getItems().validateInput(null);

		assertThrowsWithMessage(SchemaValidationException.class, "Minimum length of value not met.", ()->s.getItems().validateInput("1"));
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum length of value not met.", ()->s.getItems().getItems().validateInput("12"));

		assertThrowsWithMessage(SchemaValidationException.class, "Maximum length of value exceeded.", ()->s.getItems().validateInput("1234"));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum length of value exceeded.", ()->s.getItems().getItems().validateInput("12345"));
	}

	@Content
	@Schema(
		e="X,Y"
	)
	public static class B04a {}

	@Test void b04a_enum() throws Exception {
		var s = HttpPartSchema.create().applyAll(Content.class, B04a.class).build();
		s.validateInput("X");
		s.validateInput("Y");
		s.validateInput(null);
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match one of the expected values.  Must be one of the following:  X, Y", ()->s.validateInput("Z"));
	}

	@Content
	@Schema(
		e=" X , Y "
	)
	public static class B04b {}

	@Test void b04b_enum() throws Exception {
		var s = HttpPartSchema.create().applyAll(Content.class, B04b.class).build();
		s.validateInput("X");
		s.validateInput("Y");
		s.validateInput(null);
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match one of the expected values.  Must be one of the following:  X, Y", ()->s.validateInput("Z"));
	}

	@Content
	@Schema(
		e="X,Y"
	)
	public static class B04c {}

	@Test void b04c_enum_json() throws Exception {
		var s = HttpPartSchema.create().applyAll(Content.class, B04c.class).build();
		s.validateInput("X");
		s.validateInput("Y");
		s.validateInput(null);
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match one of the expected values.  Must be one of the following:  X, Y", ()->s.validateInput("Z"));
	}

	@Content
	@Schema(
		items=@Items(
			e="W",
			items=@SubItems(
				e="X"
			)
		)
	)
	public static class B04d {}

	@Test void b04d_enum_items() throws Exception {
		var s = HttpPartSchema.create().applyAll(Content.class, B04d.class).build();

		s.getItems().validateInput("W");
		s.getItems().getItems().validateInput("X");

		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match one of the expected values.  Must be one of the following:  W", ()->s.getItems().validateInput("V"));
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match one of the expected values.  Must be one of the following:  X", ()->s.getItems().getItems().validateInput("V"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Numeric validations
	//-----------------------------------------------------------------------------------------------------------------

	@Content
	@Schema(
		min="10", max="100"
	)
	public static class C01a {}

	@Test void c01a_minmax_ints() throws Exception {
		var s = HttpPartSchema.create().applyAll(Content.class, C01a.class).build();
		s.validateOutput(10);
		s.validateOutput(100);
		s.validateOutput(null);
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.validateOutput(9));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.validateOutput(101));
	}

	@Content
	@Schema(
		items=@Items(
			min="10", max="100",
			items=@SubItems(
				min="100", max="1000"
			)
		)
	)
	public static class C01b {}

	@Test void c01b_minmax_ints_items() throws Exception {
		var s = HttpPartSchema.create().applyAll(Content.class, C01b.class).build();

		s.getItems().validateOutput(10);
		s.getItems().getItems().validateOutput(100);

		s.getItems().validateOutput(100);
		s.getItems().getItems().validateOutput(1000);

		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.getItems().validateOutput(9));
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.getItems().getItems().validateOutput(99));

		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.getItems().validateOutput(101));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.getItems().getItems().validateOutput(1001));
	}

	@Content
	@Schema(
		min="10", max="100", emin=true, emax=true
	)
	public static class C02a {}

	@Test void c02a_minmax_exclusive() throws Exception {
		var s = HttpPartSchema.create().applyAll(Content.class, C02a.class).build();
		s.validateOutput(11);
		s.validateOutput(99);
		s.validateOutput(null);
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.validateOutput(10));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.validateOutput(100));
	}

	@Content
	@Schema(
		items=@Items(
			min="10", max="100", emin=true, emax=true,
			items=@SubItems(
				min="100", max="1000", emin=true, emax=true
			)
		)
	)
	public static class C02b {}

	@Test void c02b_minmax_exclusive_items() throws Exception {
		var s = HttpPartSchema.create().applyAll(Content.class, C02b.class).build();

		s.getItems().validateOutput(11);
		s.getItems().getItems().validateOutput(101);

		s.getItems().validateOutput(99);
		s.getItems().getItems().validateOutput(999);

		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.getItems().validateOutput(10));
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.getItems().getItems().validateOutput(100));

		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.getItems().validateOutput(100));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.getItems().getItems().validateOutput(1000));
	}

	@Content
	@Schema(
		min="10.1", max="100.1"
	)
	public static class C03a {}

	@Test void c03_minmax_floats() throws Exception {
		var s = HttpPartSchema.create().applyAll(Content.class, C03a.class).build();
		s.validateOutput(10.1f);
		s.validateOutput(100.1f);
		s.validateOutput(null);
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.validateOutput(10f));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.validateOutput(100.2f));
	}

	@Content
	@Schema(
		items=@Items(
			min="10.1", max="100.1",
			items=@SubItems(
				min="100.1", max="1000.1"
			)
		)
	)
	public static class C03b {}

	@Test void c03b_minmax_floats_items() throws Exception {
		var s = HttpPartSchema.create().applyAll(Content.class, C03b.class).build();

		s.getItems().validateOutput(10.1f);
		s.getItems().getItems().validateOutput(100.1f);

		s.getItems().validateOutput(100.1f);
		s.getItems().getItems().validateOutput(1000.1f);

		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.getItems().validateOutput(10f));
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.getItems().getItems().validateOutput(100f));

		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.getItems().validateOutput(100.2f));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.getItems().getItems().validateOutput(1000.2f));
	}

	@Content
	@Schema(
		min="10.1", max="100.1", emin=true, emax=true
	)
	public static class C04a {}

	@Test void c04a_minmax_floats_exclusive() throws Exception {
		var s = HttpPartSchema.create().applyAll(Content.class, C04a.class).build();
		s.validateOutput(10.2f);
		s.validateOutput(100f);
		s.validateOutput(null);
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.validateOutput(10.1f));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.validateOutput(100.1f));
	}

	@Content
	@Schema(
		items=@Items(
			min="10.1", max="100.1", emin=true, emax=true,
			items=@SubItems(
				min="100.1", max="1000.1", emin=true, emax=true
			)
		)
	)
	public static class C04b {}

	@Test void c04b_minmax_floats_exclusive_items() throws Exception {
		var s = HttpPartSchema.create().applyAll(Content.class, C04b.class).build();

		s.getItems().validateOutput(10.2f);
		s.getItems().getItems().validateOutput(100.2f);

		s.getItems().validateOutput(100f);
		s.getItems().getItems().validateOutput(1000f);

		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.getItems().validateOutput(10.1f));
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.getItems().getItems().validateOutput(100.1f));

		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.getItems().validateOutput(100.1f));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.getItems().getItems().validateOutput(1000.1f));
	}

	@Content
	@Schema(
		mo="10"
	)
	public static class C05a {}

	@Test void c05a_multipleOf() throws Exception {
		var s = HttpPartSchema.create().applyAll(Content.class, C05a.class).build();
		s.validateOutput(0);
		s.validateOutput(10);
		s.validateOutput(20);
		s.validateOutput(10f);
		s.validateOutput(20f);
		s.validateOutput(null);
		assertThrowsWithMessage(SchemaValidationException.class, "Multiple-of not met.", ()->s.validateOutput(11));
	}

	@Content
	@Schema(
		items=@Items(
			mo="10",
			items=@SubItems(
				mo="100"
			)
		)
	)
	public static class C05b {}

	@Test void c05b_multipleOf_items() throws Exception {
		var s = HttpPartSchema.create().applyAll(Content.class, C05b.class).build();

		s.getItems().validateOutput(0);
		s.getItems().getItems().validateOutput(0);

		s.getItems().validateOutput(10);
		s.getItems().getItems().validateOutput(100);

		s.getItems().validateOutput(20);
		s.getItems().getItems().validateOutput(200);

		s.getItems().validateOutput(10f);
		s.getItems().getItems().validateOutput(100f);

		s.getItems().validateOutput(20f);
		s.getItems().getItems().validateOutput(200f);

		assertThrowsWithMessage(SchemaValidationException.class, "Multiple-of not met.", ()->s.getItems().validateOutput(11));
		assertThrowsWithMessage(SchemaValidationException.class, "Multiple-of not met.", ()->s.getItems().getItems().validateOutput(101));
	}

	@Content
	@Schema(
		mo="10.1"
	)
	public static class C06a {}

	@Test void c06a_multipleOf_floats() throws Exception {
		var s = HttpPartSchema.create().applyAll(Content.class, C06a.class).build();
		s.validateOutput(0);
		s.validateOutput(10.1f);
		s.validateOutput(20.2f);
		s.validateOutput(null);
		assertThrowsWithMessage(SchemaValidationException.class, "Multiple-of not met.", ()->s.validateOutput(10.2f));
	}

	@Content
	@Schema(
		items=@Items(
			mo="10.1",
			items=@SubItems(
				mo="100.1"
			)
		)
	)
	public static class C06b {}

	@Test void c06b_multipleOf_floats_items() throws Exception {
		var s = HttpPartSchema.create().applyAll(Content.class, C06b.class).build();

		s.getItems().validateOutput(0);
		s.getItems().getItems().validateOutput(0);

		s.getItems().validateOutput(10.1f);
		s.getItems().getItems().validateOutput(100.1f);

		s.getItems().validateOutput(20.2f);
		s.getItems().getItems().validateOutput(200.2f);

		assertThrowsWithMessage(SchemaValidationException.class, "Multiple-of not met.", ()->s.getItems().validateOutput(10.2f));
		assertThrowsWithMessage(SchemaValidationException.class, "Multiple-of not met.", ()->s.getItems().getItems().validateOutput(100.2f));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Collections/Array validations
	//-----------------------------------------------------------------------------------------------------------------

	@Content
	@Schema(
		items=@Items(
			ui=true,
			items=@SubItems(
				ui=true
			)
		)
	)
	public static class D01 {}

	@Test void d01a_uniqueItems_arrays() throws Exception {
		var s = HttpPartSchema.create().applyAll(Content.class, D01.class).build();

		var good = split("a,b");
		var bad = split("a,a");

		s.getItems().validateOutput(good);
		s.getItems().getItems().validateOutput(good);
		s.getItems().validateOutput(null);

		assertThrowsWithMessage(SchemaValidationException.class, "Duplicate items not allowed.", ()->s.getItems().validateOutput(bad));
		assertThrowsWithMessage(SchemaValidationException.class, "Duplicate items not allowed.", ()->s.getItems().getItems().validateOutput(bad));
	}

	@Test void d01b_uniqueItems_collections() throws Exception {
		var s = HttpPartSchema.create().applyAll(Content.class, D01.class).build();

		var good = l("a","b");
		var bad = l("a","a");

		s.getItems().validateOutput(good);
		s.getItems().getItems().validateOutput(good);
		s.getItems().validateOutput(null);

		assertThrowsWithMessage(SchemaValidationException.class, "Duplicate items not allowed.", ()->s.getItems().validateOutput(bad));
		assertThrowsWithMessage(SchemaValidationException.class, "Duplicate items not allowed.", ()->s.getItems().getItems().validateOutput(bad));
	}

	@Content
	@Schema(
		items=@Items(
			mini=1, maxi=2,
			items=@SubItems(
				mini=2, maxi=3
			)
		)
	)
	public static class D02 {}

	@Test void d02a_minMaxItems_arrays() throws Exception {
		var s = HttpPartSchema.create().applyAll(Content.class, D02.class).build();

		s.getItems().validateOutput(split("1"));
		s.getItems().getItems().validateOutput(split("1,2"));

		s.getItems().validateOutput(split("1,2"));
		s.getItems().getItems().validateOutput(split("1,2,3"));

		assertThrowsWithMessage(SchemaValidationException.class, "Minimum number of items not met.", ()->s.getItems().validateOutput(new String[0]));
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum number of items not met.", ()->s.getItems().getItems().validateOutput(split("1")));

		assertThrowsWithMessage(SchemaValidationException.class, "Maximum number of items exceeded.", ()->s.getItems().validateOutput(split("1,2,3")));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum number of items exceeded.", ()->s.getItems().getItems().validateOutput(split("1,2,3,4")));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// JSON Schema Draft 2020-12 validation tests
	//-----------------------------------------------------------------------------------------------------------------

	@Content
	@Schema(const_="CONSTANT_VALUE")
	public static class D01a {}

	@Test void d01a_const_valid() throws Exception {
		var s = HttpPartSchema.create().applyAll(Content.class, D01a.class).build();
		s.validateInput("CONSTANT_VALUE");
		s.validateInput(null);  // null is allowed when not required
	}

	@Test void d01a_const_invalid() {
		var s = HttpPartSchema.create().applyAll(Content.class, D01a.class).build();
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match constant.  Must be: CONSTANT_VALUE", ()->s.validateInput("OTHER_VALUE"));
	}

	@Content
	@Schema(const_="CONSTANT_VALUE", required=true)
	public static class D01b {}

	@Test void d01b_const_required() throws Exception {
		var s = HttpPartSchema.create().applyAll(Content.class, D01b.class).build();
		s.validateInput("CONSTANT_VALUE");
		assertThrowsWithMessage(SchemaValidationException.class, "No value specified.", ()->s.validateInput(null));
	}

	@Content
	@Schema(t="integer", exclusiveMaximumValue="100", exclusiveMinimumValue="0")
	public static class D02a {}

	@Test void d02a_exclusiveNumericBounds() throws Exception {
		var s = HttpPartSchema.create().applyAll(Content.class, D02a.class).build();
		s.validateOutput(1);
		s.validateOutput(50);
		s.validateOutput(99);
		s.validateOutput(null);
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.validateOutput(0));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.validateOutput(100));
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.validateOutput(-1));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.validateOutput(101));
	}

	@Content
	@Schema(t="number", exclusiveMaximumValue="10.5", exclusiveMinimumValue="0.5")
	public static class D02b {}

	@Test void d02b_exclusiveNumericBounds_doubles() throws Exception {
		var s = HttpPartSchema.create().applyAll(Content.class, D02b.class).build();
		s.validateOutput(0.6);
		s.validateOutput(5.0);
		s.validateOutput(10.4);
		s.validateOutput(null);
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.validateOutput(0.5));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.validateOutput(10.5));
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.validateOutput(0.4));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.validateOutput(10.6));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Backward compatibility: Old boolean exclusiveMaximum/exclusiveMinimum
	//-----------------------------------------------------------------------------------------------------------------

	@Content
	@Schema(t="integer", exclusiveMaximum=true, exclusiveMinimum=true, maximum="100", minimum="0")
	public static class D03a {}

	@Test void d03a_exclusiveBooleanBounds() throws Exception {
		var s = HttpPartSchema.create().applyAll(Content.class, D03a.class).build();
		s.validateOutput(1);
		s.validateOutput(50);
		s.validateOutput(99);
		s.validateOutput(null);
		// With boolean flags, 0 and 100 are excluded
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.validateOutput(0));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.validateOutput(100));
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.validateOutput(-1));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.validateOutput(101));
	}

	@Content
	@Schema(t="integer", exclusiveMaximum=false, exclusiveMinimum=false, maximum="100", minimum="0")
	public static class D03b {}

	@Test void d03b_inclusiveBounds() throws Exception {
		var s = HttpPartSchema.create().applyAll(Content.class, D03b.class).build();
		// With boolean flags set to false, 0 and 100 are included
		s.validateOutput(0);
		s.validateOutput(1);
		s.validateOutput(50);
		s.validateOutput(99);
		s.validateOutput(100);
		s.validateOutput(null);
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.validateOutput(-1));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.validateOutput(101));
	}

	@Content
	@Schema(t="integer", exclusiveMaximumValue="100", exclusiveMinimumValue="0", exclusiveMaximum=false, exclusiveMinimum=false)
	public static class D03c {}

	@Test void d03c_newStyleTakesPrecedence() throws Exception {
		var s = HttpPartSchema.create().applyAll(Content.class, D03c.class).build();
		// New numeric style should take precedence over old boolean flags
		s.validateOutput(1);
		s.validateOutput(50);
		s.validateOutput(99);
		s.validateOutput(null);
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met.", ()->s.validateOutput(0));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded.", ()->s.validateOutput(100));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Deprecated property (no validation, just ensures it's settable)
	//-----------------------------------------------------------------------------------------------------------------

	@Content
	@Schema(deprecated_=true)
	public static class D04a {}

	@Test void d04a_deprecated() {
		var s = HttpPartSchema.create().applyAll(Content.class, D04a.class).build();
		// deprecated is just a flag, doesn't affect validation
		assertBean(s, "deprecated", "true");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Examples (no validation, documentation only)
	//-----------------------------------------------------------------------------------------------------------------

	@Content
	@Schema(examples={"example1", "example2", "example3"})
	public static class D05a {}

	@Test void d05a_examples() {
		var s = HttpPartSchema.create().applyAll(Content.class, D05a.class).build();
		// examples are documentation only, doesn't affect validation
		assertBean(s, "examples", "[example1,example2,example3]");
	}
}