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
package org.apache.juneau;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.commons.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.swap.*;
import org.junit.jupiter.api.*;

/**
 * Exercises the {@code validateSchema} flag end-to-end through {@link JsonParser} and {@link JsonSerializer} after
 * {@code juneau-bean-jsonschema} registers its {@code PropertyValidatorFactory} via {@code ServiceLoader}.
 */
class MarshallingContext_ValidateSchema_Test extends TestBase {

	// =================================================================================================================
	// Test beans
	// =================================================================================================================

	public static class StringBean {
		@Schema(minLength=2, maxLength=5)
		public String name;
	}

	public static class PatternBean {
		@Schema(pattern="^[a-z]+$")
		public String code;
	}

	public static class NumberBean {
		@Schema(minimum="0", maximum="100")
		public int age;
	}

	public static class EnumBean {
		@Schema(enum_={"A", "B", "C"})
		public String status;
	}

	public static class ArrayBean {
		@Schema(minItems=1, maxItems=3)
		public List<String> tags;
	}

	public static class RequiredBean {
		@Schema(required=true)
		public String name;
		public Integer age;
	}

	/** Bean whose @Schema is on the getter — exercises the getter-discovery branch in installSchemaValidationTransforms. */
	public static class GetterAnnotatedBean {
		private String code;
		@Schema(minLength=2, maxLength=4)
		public String getCode() { return code; }
		public void setCode(String value) { this.code = value; }
	}

	/** Bean whose @Schema is on the setter — exercises the setter-discovery branch in installSchemaValidationTransforms. */
	public static class SetterAnnotatedBean {
		private String code;
		public String getCode() { return code; }
		@Schema(minLength=2, maxLength=4)
		public void setCode(String value) { this.code = value; }
	}

	/** Bean with no @Schema constraints — exercises the "merged map is null" early-return branch. */
	public static class NoSchemaBean {
		public String name;
		public Integer age;
	}

	/** Bean with @Schema on field AND getter — exercises the acc!=null merge branch in applyToMap. */
	public static class DualAnnotatedBean {
		@Schema(minLength=2)
		private String code;
		@Schema(maxLength=4)
		public String getCode() { return code; }
		public void setCode(String value) { this.code = value; }
	}

	/** Pass-through string swap (wraps with @ markers) — combined with @Schema to chain through both transforms. */
	public static class AtAtSwap extends StringSwap<String> {
		@Override public String swap(MarshallingSession session, String o) {
			return "@" + o + "@";
		}
		@Override public String unswap(MarshallingSession session, String f, ClassMeta<?> hint) throws SerializeException {
			if (f.length() < 2 || ! f.startsWith("@") || ! f.endsWith("@"))
				throw new SerializeException("invalid wrapper");
			return f.substring(1, f.length() - 1);
		}
	}

	/** Bean property with @Swap AND @Schema — exercises the innerRead/innerWrite chaining in validation transforms. */
	public static class SwapAndSchemaBean {
		@Swap(AtAtSwap.class)
		@Schema(minLength=1, maxLength=5)
		public String code;
	}

	// =================================================================================================================
	// Parsing (write side)
	// =================================================================================================================

	@Test void a01_parse_minLengthPasses() throws Exception {
		var p = JsonParser.create().validateSchema().build();
		var b = p.read("{\"name\":\"abc\"}", StringBean.class);
		assertEquals("abc", b.name);
	}

	@Test void a01_parse_minLengthFails() {
		var p = JsonParser.create().validateSchema().build();
		var ex = assertThrows(RuntimeException.class, () -> p.read("{\"name\":\"a\"}", StringBean.class));
		assertTrue(rootMessage(ex).contains("minLength"), "actual: " + rootMessage(ex));
	}

	@Test void a01_parse_maxLengthFails() {
		var p = JsonParser.create().validateSchema().build();
		var ex = assertThrows(RuntimeException.class, () -> p.read("{\"name\":\"abcdef\"}", StringBean.class));
		assertTrue(rootMessage(ex).contains("maxLength"), "actual: " + rootMessage(ex));
	}

	@Test void a02_parse_patternFails() {
		var p = JsonParser.create().validateSchema().build();
		var ex = assertThrows(RuntimeException.class, () -> p.read("{\"code\":\"ABC\"}", PatternBean.class));
		assertTrue(rootMessage(ex).contains("pattern"), "actual: " + rootMessage(ex));
	}

	@Test void a03_parse_numberOutOfRange() {
		var p = JsonParser.create().validateSchema().build();
		var ex = assertThrows(RuntimeException.class, () -> p.read("{\"age\":200}", NumberBean.class));
		assertTrue(rootMessage(ex).contains("maximum"), "actual: " + rootMessage(ex));
	}

	@Test void a04_parse_enumRejectsUnknown() {
		var p = JsonParser.create().validateSchema().build();
		assertThrows(RuntimeException.class, () -> p.read("{\"status\":\"X\"}", EnumBean.class));
	}

	@Test void a05_parse_arraySizeBounds() throws Exception {
		var p = JsonParser.create().validateSchema().build();
		p.read("{\"tags\":[\"a\"]}", ArrayBean.class);
		assertThrows(RuntimeException.class, () -> p.read("{\"tags\":[]}", ArrayBean.class));
		assertThrows(RuntimeException.class, () -> p.read("{\"tags\":[\"a\",\"b\",\"c\",\"d\"]}", ArrayBean.class));
	}

	// =================================================================================================================
	// Serialization (read side)
	// =================================================================================================================

	@Test void b01_serialize_violatingValueRaises() {
		var s = JsonSerializer.create().validateSchema().build();
		var b = new StringBean();
		b.name = "x";  // too short
		assertThrows(RuntimeException.class, () -> s.write(b));
	}

	@Test void b02_serialize_passesWhenValid() throws Exception {
		var s = JsonSerializer.create().validateSchema().build();
		var b = new StringBean();
		b.name = "abc";
		var out = s.write(b);
		assertTrue(out.contains("abc"));
	}

	// =================================================================================================================
	// Off by default
	// =================================================================================================================

	@Test void c01_validation_disabledByDefault_parse() throws Exception {
		var p = JsonParser.create().build();
		var b = p.read("{\"name\":\"x\"}", StringBean.class);  // would fail with validation
		assertEquals("x", b.name);
	}

	@Test void c02_validation_disabledByDefault_serialize() {
		var s = JsonSerializer.create().build();
		var b = new StringBean();
		b.name = "x";
		assertDoesNotThrow(() -> s.write(b));  // validation disabled by default — serialization must not throw
	}

	@Test void c03_validateSchema_falseExplicitOverride() {
		var p = JsonParser.create().validateSchema(false).build();
		var b = assertDoesNotThrow(() -> p.read("{\"name\":\"x\"}", StringBean.class));  // explicit validateSchema(false) — parse must not throw
		assertEquals("x", b.name);
	}

	// =================================================================================================================
	// Configuration & context
	// =================================================================================================================

	@Test void d01_isValidateSchemaFlagPlumbedIntoContext() {
		var ctx = JsonParser.create().validateSchema().build();
		assertTrue(ctx.getMarshallingContext().isValidateSchema());
	}

	@Test void d02_validateSchema_offByDefault() {
		var ctx = JsonParser.create().build();
		assertFalse(ctx.getMarshallingContext().isValidateSchema());
	}

	@Test void d03_validateSchemaPropagatedThroughBuilderChain() {
		// Builder chain returns JsonParser thanks to covariant validateSchema() override.
		var p = JsonParser.create().validateSchema().build();
		assertTrue(p.getMarshallingContext().isValidateSchema());
	}

	@SuppressWarnings({
		"java:S5961" // Comprehensive single-feature check: every format's covariant validateSchema() override is verified in one test.
	})
	@Test void d04_validateSchemaCovariantOnAllFormats() {
		// Each format's Builder must override validateSchema() to preserve the fluent chain through format-specific
		// methods. If any of these calls return the parent builder type, the test will fail to compile.
		assertNotNull(org.apache.juneau.marshall.hjson.HjsonSerializer.create().validateSchema().useMultilineStrings(true).build());
		assertNotNull(org.apache.juneau.marshall.hjson.HjsonParser.create().validateSchema(true).build());
		assertNotNull(org.apache.juneau.marshall.toml.TomlSerializer.create().validateSchema().build());
		assertNotNull(org.apache.juneau.marshall.toml.TomlParser.create().validateSchema().build());
		assertNotNull(org.apache.juneau.marshall.prototext.PrototextSerializer.create().validateSchema().build());
		assertNotNull(org.apache.juneau.marshall.prototext.PrototextParser.create().validateSchema().build());
		assertNotNull(org.apache.juneau.marshall.cbor.CborSerializer.create().validateSchema().build());
		assertNotNull(org.apache.juneau.marshall.cbor.CborParser.create().validateSchema().build());
		assertNotNull(org.apache.juneau.marshall.bson.BsonSerializer.create().validateSchema().build());
		assertNotNull(org.apache.juneau.marshall.bson.BsonParser.create().validateSchema().build());
		assertNotNull(org.apache.juneau.marshall.parquet.ParquetSerializer.create().validateSchema().build());
		assertNotNull(org.apache.juneau.marshall.parquet.ParquetParser.create().validateSchema().build());
		assertNotNull(org.apache.juneau.marshall.ini.IniSerializer.create().validateSchema().build());
		assertNotNull(org.apache.juneau.marshall.ini.IniParser.create().validateSchema().build());
		assertNotNull(org.apache.juneau.marshall.hocon.HoconSerializer.create().validateSchema().build());
		assertNotNull(org.apache.juneau.marshall.hocon.HoconParser.create().validateSchema().build());
	}

	// =================================================================================================================
	// Getter / setter / no-schema discovery branches
	// =================================================================================================================

	@Test void e01_parse_getterAnnotation_isInstalled() throws Exception {
		// @Schema on getter is discovered via the b.getter loop in installSchemaValidationTransforms.
		var p = JsonParser.create().validateSchema().build();
		var b = p.read("{\"code\":\"ok\"}", GetterAnnotatedBean.class);
		assertEquals("ok", b.code);
		assertThrows(RuntimeException.class, () -> p.read("{\"code\":\"x\"}", GetterAnnotatedBean.class));
	}

	@Test void e02_serialize_getterAnnotation_failsForViolation() {
		var s = JsonSerializer.create().validateSchema().build();
		var b = new GetterAnnotatedBean();
		b.code = "x";  // too short
		assertThrows(RuntimeException.class, () -> s.write(b));
	}

	@Test void e03_parse_setterAnnotation_isInstalled() throws Exception {
		// @Schema on setter is discovered via the b.setter loop.
		var p = JsonParser.create().validateSchema().build();
		var b = p.read("{\"code\":\"ok\"}", SetterAnnotatedBean.class);
		assertEquals("ok", b.code);
		assertThrows(RuntimeException.class, () -> p.read("{\"code\":\"x\"}", SetterAnnotatedBean.class));
	}

	@Test void e04_parse_noSchema_validateSchemaTrue_noopInstallSkippedPerProperty() throws Exception {
		// validateSchema=true but no @Schema annotations on the bean — exercises the merged==null early-return in
		// installSchemaValidationTransforms (and confirms parsing still works normally).
		var p = JsonParser.create().validateSchema().build();
		var b = p.read("{\"name\":\"abc\",\"age\":42}", NoSchemaBean.class);
		assertEquals("abc", b.name);
		assertEquals(42, b.age);
	}

	@Test void e05_serialize_noSchema_doesNotThrow() throws Exception {
		var s = JsonSerializer.create().validateSchema().build();
		var b = new NoSchemaBean();
		b.name = "abc";
		b.age = 1;
		assertTrue(s.write(b).contains("abc"));
	}

	@Test void e06_session_isValidateSchemaPropagatedFromContext() {
		// Serializer round-trip exercises MarshallingSession.isValidateSchema()-driven branch in transforms.
		var s = JsonSerializer.create().validateSchema().build();
		var b = new StringBean();
		b.name = "abc";
		assertDoesNotThrow(() -> s.write(b));  // valid value with validateSchema=true session — must not throw
	}

	@Test void e07_parse_dualAnnotations_mergedConstraintsApplied() throws Exception {
		// @Schema(minLength=2) on field + @Schema(maxLength=4) on getter → merged map enforces both bounds.
		var p = JsonParser.create().validateSchema().build();
		p.read("{\"code\":\"abc\"}", DualAnnotatedBean.class);
		assertThrows(RuntimeException.class, () -> p.read("{\"code\":\"a\"}", DualAnnotatedBean.class));
		assertThrows(RuntimeException.class, () -> p.read("{\"code\":\"toolong\"}", DualAnnotatedBean.class));
	}

	@Test void e08_serialize_swapAndSchemaProperty_chainsBothTransforms() throws Exception {
		// @Swap + @Schema — swap transform runs first (writes "@x@"), validator then sees the post-swap value.
		var s = JsonSerializer.create().validateSchema().build();
		var b = new SwapAndSchemaBean();
		b.code = "abc";  // swap → "@abc@" (length 5, in bounds)
		assertTrue(s.write(b).contains("@abc@"));
	}

	@Test void e09_parse_swapAndSchemaProperty_chainsWriteTransform() throws Exception {
		// Parse hits the writeTransform path: validator runs first, then swap unswaps.
		var p = JsonParser.create().validateSchema().build();
		var b = p.read("{\"code\":\"@x@\"}", SwapAndSchemaBean.class);
		assertEquals("x", b.code);
	}

	// =================================================================================================================
	// Helpers
	// =================================================================================================================

	private static String rootMessage(Throwable t) {
		Throwable cur = t;
		while (cur.getCause() != null && cur.getCause() != cur)
			cur = cur.getCause();
		var msg = cur.getMessage();
		return msg == null ? "" : msg;
	}

	@SuppressWarnings({
		"unused"  // Unused in this context; kept for API consistency or future use.
	})
	private static boolean isBeanRuntime(Throwable t) {
		while (t != null) {
			if (t instanceof BeanRuntimeException)
				return true;
			t = t.getCause();
		}
		return false;
	}
}
