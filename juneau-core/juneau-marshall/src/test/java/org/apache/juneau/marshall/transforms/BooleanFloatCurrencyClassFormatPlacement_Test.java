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
package org.apache.juneau.marshall.transforms;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.json5.*;
import org.junit.jupiter.api.*;

/**
 * Precedence chain tests for {@link BooleanFormat}, {@link FloatFormat}, {@link CurrencyFormat}, and
 * {@link ClassFormat}: {@code @MarshalledProp} &gt; {@code @Marshalled} &gt; {@code MarshallingContext}
 * &gt; default.
 */
class BooleanFloatCurrencyClassFormatPlacement_Test {

	private static final Currency USD = Currency.getInstance("USD");

	//------------------------------------------------------------------------------------------------------------------
	// BooleanFormat
	//------------------------------------------------------------------------------------------------------------------

	public static class A01 { public boolean b = true; }

	@Test void a01_boolean_defaultIsTrueFalse() throws Exception {
		var s = Json5Serializer.create().build();
		assertEquals("{b:true}", s.serialize(new A01()));
	}

	@Test void a02_boolean_contextOverridesDefault() throws Exception {
		var s = Json5Serializer.create().booleanFormat(BooleanFormat.YES_NO).build();
		assertEquals("{b:'yes'}", s.serialize(new A01()));
	}

	@Test void a02b_boolean_contextZeroOneEmitsNumericToken() throws Exception {
		var s = Json5Serializer.create().booleanFormat(BooleanFormat.ZERO_ONE).build();
		// Numeric 1, not the quoted string "1".
		assertEquals("{b:1}", s.serialize(new A01()));
	}

	@Marshalled(booleanFormat = BooleanFormat.Y_N)
	public static class A03 { public boolean b = true; }

	@Test void a03_boolean_classOverridesContext() throws Exception {
		var s = Json5Serializer.create().booleanFormat(BooleanFormat.YES_NO).build();
		assertEquals("{b:'Y'}", s.serialize(new A03()));
	}

	@Marshalled(booleanFormat = BooleanFormat.Y_N)
	public static class A04 {
		@MarshalledProp(booleanFormat = BooleanFormat.ON_OFF)
		public boolean b = true;
	}

	@Test void a04_boolean_propertyOverridesClass() throws Exception {
		var s = Json5Serializer.create().build();
		assertEquals("{b:'on'}", s.serialize(new A04()));
	}

	//------------------------------------------------------------------------------------------------------------------
	// FloatFormat
	//------------------------------------------------------------------------------------------------------------------

	// Boxed Double — primitive double/float bean properties intentionally bypass the float swap so
	// Juneau's null-to-primitive-default convention is preserved (BeanMap_Test relies on this).  Users
	// who want NaN/null/string transforms on a float field must declare it as the boxed type.
	public static class B01 { public Double n = Double.NaN; }

	@Test void b01_float_defaultIsNanAsNull() throws Exception {
		// keepNullProperties(true) so the JSON layer doesn't suppress the null output of NaN_AS_NULL.
		var s = Json5Serializer.create().keepNullProperties().build();
		assertEquals("{n:null}", s.serialize(new B01()));
	}

	@Test void b02_float_contextNanAsString() throws Exception {
		var s = Json5Serializer.create().floatFormat(FloatFormat.NaN_AS_STRING).build();
		assertEquals("{n:'NaN'}", s.serialize(new B01()));
	}

	@Marshalled(floatFormat = FloatFormat.NaN_AS_STRING)
	public static class B03 { public Double n = Double.NaN; }

	@Test void b03_float_classOverridesContext() throws Exception {
		var s = Json5Serializer.create().floatFormat(FloatFormat.NaN_AS_NULL).build();
		assertEquals("{n:'NaN'}", s.serialize(new B03()));
	}

	@Marshalled(floatFormat = FloatFormat.NaN_AS_NULL)
	public static class B04 {
		@MarshalledProp(floatFormat = FloatFormat.NaN_AS_STRING)
		public Double n = Double.NaN;
	}

	@Test void b04_float_propertyOverridesClass() throws Exception {
		var s = Json5Serializer.create().build();
		assertEquals("{n:'NaN'}", s.serialize(new B04()));
	}

	public static class B05 { public Double n = Double.POSITIVE_INFINITY; }

	@Test void b05_float_nanAsStringPositiveInfinity() throws Exception {
		var s = Json5Serializer.create().floatFormat(FloatFormat.NaN_AS_STRING).build();
		assertEquals("{n:'Infinity'}", s.serialize(new B05()));
	}

	//------------------------------------------------------------------------------------------------------------------
	// CurrencyFormat
	//------------------------------------------------------------------------------------------------------------------

	public static class C01 { public Currency c = USD; }

	private static String expectedUsdSymbolInUs() {
		// JDK locale provider may not localize USD -> $ depending on -Djava.locale.providers; query at runtime.
		return USD.getSymbol(Locale.US);
	}

	@Test void c01_currency_defaultIsIsoCode() throws Exception {
		var s = Json5Serializer.create().build();
		assertEquals("{c:'USD'}", s.serialize(new C01()));
	}

	@Test void c02_currency_contextSymbol() throws Exception {
		var s = Json5Serializer.create().locale(Locale.US).currencyFormat(CurrencyFormat.SYMBOL).build();
		assertEquals("{c:'" + expectedUsdSymbolInUs() + "'}", s.serialize(new C01()));
	}

	@Marshalled(currencyFormat = CurrencyFormat.SYMBOL)
	public static class C03 { public Currency c = USD; }

	@Test void c03_currency_classOverridesContext() throws Exception {
		var s = Json5Serializer.create().locale(Locale.US).currencyFormat(CurrencyFormat.ISO_CODE).build();
		assertEquals("{c:'" + expectedUsdSymbolInUs() + "'}", s.serialize(new C03()));
	}

	@Marshalled(currencyFormat = CurrencyFormat.ISO_CODE)
	public static class C04 {
		@MarshalledProp(currencyFormat = CurrencyFormat.SYMBOL)
		public Currency c = USD;
	}

	@Test void c04_currency_propertyOverridesClass() throws Exception {
		var s = Json5Serializer.create().locale(Locale.US).build();
		assertEquals("{c:'" + expectedUsdSymbolInUs() + "'}", s.serialize(new C04()));
	}

	//------------------------------------------------------------------------------------------------------------------
	// ClassFormat
	//------------------------------------------------------------------------------------------------------------------

	public static class D01 { public Class<?> c = String.class; }

	@Test void d01_class_defaultIsFqcn() throws Exception {
		var s = Json5Serializer.create().build();
		assertEquals("{c:'java.lang.String'}", s.serialize(new D01()));
	}

	@Test void d02_class_contextSimpleName() throws Exception {
		var s = Json5Serializer.create().classFormat(ClassFormat.SIMPLE_NAME).build();
		assertEquals("{c:'String'}", s.serialize(new D01()));
	}

	@Marshalled(classFormat = ClassFormat.SIMPLE_NAME)
	public static class D03 { public Class<?> c = String.class; }

	@Test void d03_class_classOverridesContext() throws Exception {
		var s = Json5Serializer.create().classFormat(ClassFormat.FQCN).build();
		assertEquals("{c:'String'}", s.serialize(new D03()));
	}

	@Marshalled(classFormat = ClassFormat.FQCN)
	public static class D04 {
		@MarshalledProp(classFormat = ClassFormat.SIMPLE_NAME)
		public Class<?> c = String.class;
	}

	@Test void d04_class_propertyOverridesClass() throws Exception {
		var s = Json5Serializer.create().build();
		assertEquals("{c:'String'}", s.serialize(new D04()));
	}

	public static class D05 { public Class<?> c = Map.Entry.class; }

	@Test void d05_class_binaryName() throws Exception {
		var s = Json5Serializer.create().classFormat(ClassFormat.BINARY_NAME).build();
		assertEquals("{c:'java.util.Map$Entry'}", s.serialize(new D05()));
	}

	//------------------------------------------------------------------------------------------------------------------
	// @MarshalledConfig integration
	//------------------------------------------------------------------------------------------------------------------

	@MarshalledConfig(
		booleanFormat = BooleanFormat.YES_NO,
		floatFormat = FloatFormat.NaN_AS_STRING,
		currencyFormat = CurrencyFormat.SYMBOL,
		classFormat = ClassFormat.SIMPLE_NAME
	)
	static class E01Config {}

	public static class E01Bean {
		public boolean b = true;
		public Double n = Double.NaN;
		public Currency c = USD;
		public Class<?> k = String.class;
	}

	@Test void e01_marshalledConfig_applies() throws Exception {
		var s = Json5Serializer.create().locale(Locale.US).applyAnnotations(E01Config.class).build();
		var json = s.serialize(new E01Bean());
		assertTrue(json.contains("b:'yes'"), "boolean: " + json);
		assertTrue(json.contains("n:'NaN'"), "float: " + json);
		assertTrue(json.contains("c:'" + expectedUsdSymbolInUs() + "'"), "currency: " + json);
		assertTrue(json.contains("k:'String'"), "class: " + json);
	}

	//------------------------------------------------------------------------------------------------------------------
	// NOT_SET sentinel falls through to next-higher precedence
	//------------------------------------------------------------------------------------------------------------------

	@Marshalled(
		booleanFormat = BooleanFormat.NOT_SET,
		floatFormat = FloatFormat.NOT_SET,
		currencyFormat = CurrencyFormat.NOT_SET,
		classFormat = ClassFormat.NOT_SET
	)
	public static class F01 {
		public boolean b = true;
		public Double n = Double.NaN;
		public Currency c = USD;
		public Class<?> k = String.class;
	}

	@Test void f01_notSet_fallsThroughToContext() throws Exception {
		var s = Json5Serializer.create()
			.locale(Locale.US)
			.booleanFormat(BooleanFormat.YES_NO)
			.floatFormat(FloatFormat.NaN_AS_STRING)
			.currencyFormat(CurrencyFormat.SYMBOL)
			.classFormat(ClassFormat.SIMPLE_NAME)
			.build();
		var json = s.serialize(new F01());
		assertTrue(json.contains("b:'yes'"), "boolean: " + json);
		assertTrue(json.contains("n:'NaN'"), "float: " + json);
		assertTrue(json.contains("c:'" + expectedUsdSymbolInUs() + "'"), "currency: " + json);
		assertTrue(json.contains("k:'String'"), "class: " + json);
	}
}
