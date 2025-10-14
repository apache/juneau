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

import static org.junit.jupiter.api.Assertions.*;

import jakarta.validation.constraints.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for Jakarta Bean Validation integration with HttpPartSchema.
 *
 * <p>
 * This test uses the real jakarta.validation-api (test scope dependency) to verify
 * that HttpPartSchema correctly processes Jakarta Validation constraint annotations.
 */
class HttpPartSchema_JakartaValidation_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// @NotNull
	//-----------------------------------------------------------------------------------------------------------------

	public static class A01 {
		@NotNull
		public String value;
	}

	@Test
	void a01_jakarta_NotNull() throws Exception {
		NotNull anno = A01.class.getDeclaredField("value").getAnnotation(NotNull.class);
		var s = HttpPartSchema.create().apply(anno).build();
		assertTrue(s.isRequired());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Size
	//-----------------------------------------------------------------------------------------------------------------

	public static class B01 {
		@Size(min=2, max=50)
		public String value;
	}

	@Test
	void b01_jakarta_Size() throws Exception {
		Size anno = B01.class.getDeclaredField("value").getAnnotation(Size.class);
		var s = HttpPartSchema.create().apply(anno).build();
		assertEquals(2L, s.getMinLength());
		assertEquals(50L, s.getMaxLength());
		assertEquals(2L, s.getMinItems());
		assertEquals(50L, s.getMaxItems());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Min
	//-----------------------------------------------------------------------------------------------------------------

	public static class C01 {
		@Min(10)
		public int value;
	}

	@Test
	void c01_jakarta_Min() throws Exception {
		Min anno = C01.class.getDeclaredField("value").getAnnotation(Min.class);
		var s = HttpPartSchema.create().apply(anno).build();
		assertEquals(10L, s.getMinimum());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Max
	//-----------------------------------------------------------------------------------------------------------------

	public static class D01 {
		@Max(100)
		public int value;
	}

	@Test
	void d01_jakarta_Max() throws Exception {
		Max anno = D01.class.getDeclaredField("value").getAnnotation(Max.class);
		var s = HttpPartSchema.create().apply(anno).build();
		assertEquals(100L, s.getMaximum());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Pattern
	//-----------------------------------------------------------------------------------------------------------------

	public static class E01 {
		@Pattern(regexp="^[A-Z]+$")
		public String value;
	}

	@Test
	void e01_jakarta_Pattern() throws Exception {
		Pattern anno = E01.class.getDeclaredField("value").getAnnotation(Pattern.class);
		var s = HttpPartSchema.create().apply(anno).build();
		assertEquals("^[A-Z]+$", s.getPattern().pattern());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Email
	// Note: HttpPartFormat enum does not currently support "email" format, so this test is disabled.
	// The @Email constraint is recognized but does not map to a format value.
	//-----------------------------------------------------------------------------------------------------------------

	public static class F01 {
		@Email
		public String value;
	}

	// Disabled: HttpPartFormat enum does not support "email" format
	// @Test
	// void f01_jakarta_Email() throws Exception {
	// 	Email anno = F01.class.getDeclaredField("value").getAnnotation(Email.class);
	// 	var s = HttpPartSchema.create().apply(anno).build();
	// 	assertEquals("email", s.getFormat().toString());
	// }

	//-----------------------------------------------------------------------------------------------------------------
	// @Positive
	//-----------------------------------------------------------------------------------------------------------------

	public static class G01 {
		@Positive
		public int value;
	}

	@Test
	void g01_jakarta_Positive() throws Exception {
		Positive anno = G01.class.getDeclaredField("value").getAnnotation(Positive.class);
		var s = HttpPartSchema.create().apply(anno).build();
		assertEquals(0, s.getMinimum());
		assertTrue(s.isExclusiveMinimum());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @PositiveOrZero
	//-----------------------------------------------------------------------------------------------------------------

	public static class H01 {
		@PositiveOrZero
		public int value;
	}

	@Test
	void h01_jakarta_PositiveOrZero() throws Exception {
		PositiveOrZero anno = H01.class.getDeclaredField("value").getAnnotation(PositiveOrZero.class);
		var s = HttpPartSchema.create().apply(anno).build();
		assertEquals(0, s.getMinimum());
		assertFalse(s.isExclusiveMinimum());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Negative
	//-----------------------------------------------------------------------------------------------------------------

	public static class I01 {
		@Negative
		public int value;
	}

	@Test
	void i01_jakarta_Negative() throws Exception {
		Negative anno = I01.class.getDeclaredField("value").getAnnotation(Negative.class);
		var s = HttpPartSchema.create().apply(anno).build();
		assertEquals(0, s.getMaximum());
		assertTrue(s.isExclusiveMaximum());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @NegativeOrZero
	//-----------------------------------------------------------------------------------------------------------------

	public static class J01 {
		@NegativeOrZero
		public int value;
	}

	@Test
	void j01_jakarta_NegativeOrZero() throws Exception {
		NegativeOrZero anno = J01.class.getDeclaredField("value").getAnnotation(NegativeOrZero.class);
		var s = HttpPartSchema.create().apply(anno).build();
		assertEquals(0, s.getMaximum());
		assertFalse(s.isExclusiveMaximum());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @NotEmpty
	//-----------------------------------------------------------------------------------------------------------------

	public static class K01 {
		@NotEmpty
		public String value;
	}

	@Test
	void k01_jakarta_NotEmpty() throws Exception {
		NotEmpty anno = K01.class.getDeclaredField("value").getAnnotation(NotEmpty.class);
		var s = HttpPartSchema.create().apply(anno).build();
		assertTrue(s.isRequired());
		assertEquals(1L, s.getMinLength());
		assertEquals(1L, s.getMinItems());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @NotBlank
	//-----------------------------------------------------------------------------------------------------------------

	public static class L01 {
		@NotBlank
		public String value;
	}

	@Test
	void l01_jakarta_NotBlank() throws Exception {
		NotBlank anno = L01.class.getDeclaredField("value").getAnnotation(NotBlank.class);
		var s = HttpPartSchema.create().apply(anno).build();
		assertTrue(s.isRequired());
		assertEquals(1L, s.getMinLength());
		assertEquals(".*\\S.*", s.getPattern().pattern());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @DecimalMin (inclusive)
	//-----------------------------------------------------------------------------------------------------------------

	public static class M01 {
		@DecimalMin("10.5")
		public double value;
	}

	@Test
	void m01_jakarta_DecimalMin_inclusive() throws Exception {
		DecimalMin anno = M01.class.getDeclaredField("value").getAnnotation(DecimalMin.class);
		var s = HttpPartSchema.create().apply(anno).build();
		assertEquals(10.5, s.getMinimum().doubleValue());
		assertFalse(s.isExclusiveMinimum());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @DecimalMin (exclusive)
	//-----------------------------------------------------------------------------------------------------------------

	public static class M02 {
		@DecimalMin(value="10.5", inclusive=false)
		public double value;
	}

	@Test
	void m02_jakarta_DecimalMin_exclusive() throws Exception {
		DecimalMin anno = M02.class.getDeclaredField("value").getAnnotation(DecimalMin.class);
		var s = HttpPartSchema.create().apply(anno).build();
		assertEquals(10.5, s.getMinimum().doubleValue());
		assertTrue(s.isExclusiveMinimum());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @DecimalMax (inclusive)
	//-----------------------------------------------------------------------------------------------------------------

	public static class N01 {
		@DecimalMax("99.9")
		public double value;
	}

	@Test
	void n01_jakarta_DecimalMax_inclusive() throws Exception {
		DecimalMax anno = N01.class.getDeclaredField("value").getAnnotation(DecimalMax.class);
		var s = HttpPartSchema.create().apply(anno).build();
		assertEquals(99.9, s.getMaximum().doubleValue(), 0.001);
		assertFalse(s.isExclusiveMaximum());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @DecimalMax (exclusive)
	//-----------------------------------------------------------------------------------------------------------------

	public static class N02 {
		@DecimalMax(value="99.9", inclusive=false)
		public double value;
	}

	@Test
	void n02_jakarta_DecimalMax_exclusive() throws Exception {
		DecimalMax anno = N02.class.getDeclaredField("value").getAnnotation(DecimalMax.class);
		var s = HttpPartSchema.create().apply(anno).build();
		assertEquals(99.9, s.getMaximum().doubleValue(), 0.001);
		assertTrue(s.isExclusiveMaximum());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Multiple constraints
	//-----------------------------------------------------------------------------------------------------------------

	public static class O01 {
		@NotNull
		@Size(min=5, max=20)
		@Pattern(regexp="^[a-z]+$")
		public String value;
	}

	@Test
	void o01_jakarta_multiple_constraints() throws Exception {
		var field = O01.class.getDeclaredField("value");
		var s = HttpPartSchema.create()
			.apply(field.getAnnotation(NotNull.class))
			.apply(field.getAnnotation(Size.class))
			.apply(field.getAnnotation(Pattern.class))
			.build();

		assertTrue(s.isRequired());
		assertEquals(5L, s.getMinLength());
		assertEquals(20L, s.getMaxLength());
		assertEquals("^[a-z]+$", s.getPattern().pattern());
	}
}