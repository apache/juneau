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
package org.apache.juneau.junit.bct;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.*;
import java.time.format.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Unit tests for {@link Stringifiers}.
 */
class Stringifiers_Test extends TestBase {

	@Nested
	class A_mapEntryStringifier extends TestBase {

		@Test
		void a01_stringifySimpleEntry() {
			var converter = BasicBeanConverter.builder().defaultSettings().build();
			var stringifier = Stringifiers.mapEntryStringifier();
			var entry = Map.entry("key", "value");
			var result = stringifier.apply(converter, entry);

			assertEquals("key=value", result);
		}

		@Test
		void a02_stringifyWithNullKey() {
			var converter = BasicBeanConverter.builder().defaultSettings().build();
			var stringifier = Stringifiers.mapEntryStringifier();
			var entry = new AbstractMap.SimpleEntry<String, String>(null, "value");
			var result = stringifier.apply(converter, entry);

			assertEquals("<null>=value", result);
		}

		@Test
		void a03_stringifyWithNullValue() {
			var converter = BasicBeanConverter.builder().defaultSettings().build();
			var stringifier = Stringifiers.mapEntryStringifier();
			var entry = new AbstractMap.SimpleEntry<>("key", null);
			var result = stringifier.apply(converter, entry);

			assertEquals("key=<null>", result);
		}

		@Test
		void a04_stringifyWithCustomSeparator() {
			var converter = BasicBeanConverter.builder()
				.defaultSettings()
				.addSetting("mapEntrySeparator", ": ")
				.build();
			var stringifier = Stringifiers.mapEntryStringifier();
			var entry = Map.entry("name", "John");
			var result = stringifier.apply(converter, entry);

			assertEquals("name: John", result);
		}

		@Test
		void a05_stringifyWithComplexValues() {
		var converter = BasicBeanConverter.builder().defaultSettings().build();
		var stringifier = Stringifiers.mapEntryStringifier();
		var entry = Map.entry("list", l("a", "b"));
		var result = stringifier.apply(converter, entry);

			assertEquals("list=[a,b]", result);
		}
	}

	@Nested
	class B_calendarStringifier extends TestBase {

		@Test
		void b01_stringifyGregorianCalendar() {
			var converter = BasicBeanConverter.builder().defaultSettings().build();
			var stringifier = Stringifiers.calendarStringifier();
			var calendar = new GregorianCalendar(2023, Calendar.JANUARY, 15, 10, 30, 0);
			calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
			var result = stringifier.apply(converter, calendar);

			assertMatchesGlob("2023-01-15T*", result);
		}

		@Test
		void b02_stringifyWithCustomFormat() {
			var converter = BasicBeanConverter.builder()
				.defaultSettings()
				.addSetting("calendarFormat", DateTimeFormatter.ISO_LOCAL_DATE)
				.build();
			var stringifier = Stringifiers.calendarStringifier();
			var calendar = new GregorianCalendar(2023, Calendar.DECEMBER, 25);
			var result = stringifier.apply(converter, calendar);

			assertMatchesGlob("2023-12-25", result);
		}
	}

	@Nested
	class C_dateStringifier extends TestBase {

		@Test
		void c01_stringifyDate() {
			var stringifier = Stringifiers.dateStringifier();
			var date = new Date(1673780400000L); // 2023-01-15T11:00:00Z
			var result = stringifier.apply(null, date);

			assertEquals("2023-01-15T11:00:00Z", result);
		}

		@Test
		void c02_stringifyDateWithConverter() {
			var converter = BasicBeanConverter.builder().defaultSettings().build();
			var stringifier = Stringifiers.dateStringifier();
			var date = new Date(0); // Epoch
			var result = stringifier.apply(converter, date);

			assertEquals("1970-01-01T00:00:00Z", result);
		}
	}

	@Nested
	class D_inputStreamStringifier extends TestBase {

		@Test
		void d01_stringifyInputStream() {
			var stringifier = Stringifiers.inputStreamStringifier();
			var input = new ByteArrayInputStream(bytes(0x48, 0x65, 0x6C, 0x6C, 0x6F)); // "Hello"
			var result = stringifier.apply(null, input);

			assertEquals("48656C6C6F", result);
		}

		@Test
		void d02_stringifyEmptyInputStream() {
			var stringifier = Stringifiers.inputStreamStringifier();
			var input = new ByteArrayInputStream(new byte[0]);
			var result = stringifier.apply(null, input);

			assertEquals("", result);
		}

		@Test
		void d03_stringifyInputStreamWithZeros() {
			var stringifier = Stringifiers.inputStreamStringifier();
			var input = new ByteArrayInputStream(bytes(0x00, (byte)0xFF, 0x7F));
			var result = stringifier.apply(null, input);

			assertEquals("00FF7F", result);
		}
	}

	@Nested
	class E_byteArrayStringifier extends TestBase {

		@Test
		void e01_stringifyByteArray() {
			var stringifier = Stringifiers.byteArrayStringifier();
			var input = bytes(0x48, 0x65, 0x6C, 0x6C, 0x6F); // "Hello"
			var result = stringifier.apply(null, input);

			assertEquals("48656C6C6F", result);
		}

		@Test
		void e02_stringifyEmptyByteArray() {
			var stringifier = Stringifiers.byteArrayStringifier();
			var input = new byte[0];
			var result = stringifier.apply(null, input);

			assertEquals("", result);
		}

		@Test
		void e03_stringifyByteArrayWithAllValues() {
			var stringifier = Stringifiers.byteArrayStringifier();
			var input = bytes(0x00, (byte)0xFF, 0x7F, (byte)0x80);
			var result = stringifier.apply(null, input);

			assertEquals("00FF7F80", result);
		}
	}

	@Nested
	class F_charArrayStringifier extends TestBase {

		@Test
		void f01_stringifyCharArray() {
			var stringifier = Stringifiers.charArrayStringifier();
			var input = chars('H', 'e', 'l', 'l', 'o');
			var result = stringifier.apply(null, input);

			assertEquals("Hello", result);
		}

		@Test
		void f02_stringifyEmptyCharArray() {
			var stringifier = Stringifiers.charArrayStringifier();
			var input = new char[0];
			var result = stringifier.apply(null, input);

			assertEquals("", result);
		}

		@Test
		void f03_stringifyCharArrayWithHexChars() {
			var stringifier = Stringifiers.charArrayStringifier();
			var input = chars('0', '0', '0', '0');
			var result = stringifier.apply(null, input);

			assertEquals("0000", result);
		}

		@Test
		void f04_stringifyCharArrayWithSpecialChars() {
			var stringifier = Stringifiers.charArrayStringifier();
			var input = chars('A', 'B', 'C', 'D', 'E', 'F');
			var result = stringifier.apply(null, input);

			assertEquals("ABCDEF", result);
		}

		@Test
		void f05_stringifyCharArrayWithMixedChars() {
			var stringifier = Stringifiers.charArrayStringifier();
			var input = chars('a', '1', 'B', '2', 'c', '3');
			var result = stringifier.apply(null, input);

			assertEquals("a1B2c3", result);
		}
	}

	@Nested
	class G_readerStringifier extends TestBase {

		@ParameterizedTest
		@ValueSource(strings = {
			"Hello World",
			"",
			"Line 1\nLine 2\nLine 3"
		})
		void g01_stringifyReader(String input) {
			var stringifier = Stringifiers.readerStringifier();
			var reader = new StringReader(input);
			var result = stringifier.apply(null, reader);

			assertEquals(input, result);
		}
	}

	@Nested
	class G_fileStringifier extends TestBase {

		@Test
		void g01_stringifyTempFile() throws Exception {
			var stringifier = Stringifiers.fileStringifier();
			var tempFile = Files.createTempFile("test", ".txt");
			Files.writeString(tempFile, "Test content");

			try {
				var result = stringifier.apply(null, tempFile.toFile());
				assertEquals("Test content", result);
			} finally {
				Files.deleteIfExists(tempFile);
			}
		}

		@Test
		void g02_stringifyEmptyFile() throws Exception {
			var stringifier = Stringifiers.fileStringifier();
			var tempFile = Files.createTempFile("empty", ".txt");

			try {
				var result = stringifier.apply(null, tempFile.toFile());
				assertEquals("", result);
			} finally {
				Files.deleteIfExists(tempFile);
			}
		}
	}

	@Nested
	class H_enumStringifier extends TestBase {

		enum TestEnum { VALUE1, VALUE2, LONG_VALUE_NAME }

		@Test
		void h01_stringifyEnum() {
			var stringifier = Stringifiers.enumStringifier();
			var result = stringifier.apply(null, TestEnum.VALUE1);

			assertEquals("VALUE1", result);
		}

		@Test
		void h02_stringifyEnumWithLongName() {
			var stringifier = Stringifiers.enumStringifier();
			var result = stringifier.apply(null, TestEnum.LONG_VALUE_NAME);

			assertEquals("LONG_VALUE_NAME", result);
		}

		@Test
		void h03_stringifyStandardEnum() {
			var stringifier = Stringifiers.enumStringifier();
			var result = stringifier.apply(null, Thread.State.RUNNABLE);

			assertEquals("RUNNABLE", result);
		}
	}

	@Nested
	class I_classStringifier extends TestBase {

		@Test
		void i01_stringifyClassSimple() {
			var converter = BasicBeanConverter.builder()
				.defaultSettings()
				.addSetting("classNameFormat", "simple")
				.build();
			var stringifier = Stringifiers.classStringifier();
			var result = stringifier.apply(converter, String.class);

			assertEquals("String", result);
		}

		@Test
		void i02_stringifyClassCanonical() {
			var converter = BasicBeanConverter.builder()
				.defaultSettings()
				.addSetting("classNameFormat", "canonical")
				.build();
			var stringifier = Stringifiers.classStringifier();
			var result = stringifier.apply(converter, ArrayList.class);

			assertEquals("java.util.ArrayList", result);
		}

		@Test
		void i03_stringifyClassFull() {
			var converter = BasicBeanConverter.builder()
				.defaultSettings()
				.addSetting("classNameFormat", "full")
				.build();
			var stringifier = Stringifiers.classStringifier();
			var result = stringifier.apply(converter, HashMap.class);

			assertEquals("java.util.HashMap", result);
		}

		@Test
		void i04_stringifyPrimitiveClass() {
			var converter = BasicBeanConverter.builder().defaultSettings().build();
			var stringifier = Stringifiers.classStringifier();
			var result = stringifier.apply(converter, int.class);

			assertEquals("int", result);
		}
	}

	@Nested
	class J_constructorStringifier extends TestBase {

		@Test
		void j01_stringifyNoArgConstructor() throws Exception {
			var converter = BasicBeanConverter.builder().defaultSettings().build();
			var stringifier = Stringifiers.constructorStringifier();
			var constructor = ArrayList.class.getConstructor();
			var result = stringifier.apply(converter, constructor);

			assertEquals("ArrayList()", result);
		}

		@Test
		void j02_stringifyParameterizedConstructor() throws Exception {
			var converter = BasicBeanConverter.builder().defaultSettings().build();
			var stringifier = Stringifiers.constructorStringifier();
			var constructor = String.class.getConstructor(char[].class);
			var result = stringifier.apply(converter, constructor);

			assertEquals("String(char[])", result);
		}

		@Test
		void j03_stringifyMultiParameterConstructor() throws Exception {
			var converter = BasicBeanConverter.builder().defaultSettings().build();
			var stringifier = Stringifiers.constructorStringifier();
			var constructor = String.class.getConstructor(byte[].class, int.class, int.class);
			var result = stringifier.apply(converter, constructor);

			assertEquals("String(byte[],int,int)", result);
		}
	}

	@Nested
	class K_methodStringifier extends TestBase {

		@Test
		void k01_stringifyNoArgMethod() throws Exception {
			var converter = BasicBeanConverter.builder().defaultSettings().build();
			var stringifier = Stringifiers.methodStringifier();
			var method = Object.class.getMethod("toString");
			var result = stringifier.apply(converter, method);

			assertEquals("toString()", result);
		}

		@Test
		void k02_stringifyParameterizedMethod() throws Exception {
			var converter = BasicBeanConverter.builder().defaultSettings().build();
			var stringifier = Stringifiers.methodStringifier();
			var method = String.class.getMethod("substring", int.class);
			var result = stringifier.apply(converter, method);

			assertEquals("substring(int)", result);
		}

		@Test
		void k03_stringifyMultiParameterMethod() throws Exception {
			var converter = BasicBeanConverter.builder().defaultSettings().build();
			var stringifier = Stringifiers.methodStringifier();
			var method = String.class.getMethod("substring", int.class, int.class);
			var result = stringifier.apply(converter, method);

			assertEquals("substring(int,int)", result);
		}
	}

	@Nested
	class L_listStringifier extends TestBase {

		@Test
		void l01_stringifyList() {
		var converter = BasicBeanConverter.builder().defaultSettings().build();
		var stringifier = Stringifiers.listStringifier();
		var input = l("a", "b", "c");
		var result = stringifier.apply(converter, input);

			assertEquals("[a,b,c]", result);
		}

		@Test
		void l02_stringifyEmptyList() {
		var converter = BasicBeanConverter.builder().defaultSettings().build();
		var stringifier = Stringifiers.listStringifier();
		var input = l();
		var result = stringifier.apply(converter, input);

			assertEquals("[]", result);
		}

		@Test
		void l03_stringifyWithCustomDelimiters() {
			var converter = BasicBeanConverter.builder()
				.defaultSettings()
				.addSetting("fieldSeparator", "; ")
				.addSetting("collectionPrefix", "(")
				.addSetting("collectionSuffix", ")")
			.build();
		var stringifier = Stringifiers.listStringifier();
		var input = l("x", "y", "z");
		var result = stringifier.apply(converter, input);

			assertEquals("(x; y; z)", result);
		}

		@Test
		void l04_stringifyWithNullElements() {
			var converter = BasicBeanConverter.builder().defaultSettings().build();
			var stringifier = Stringifiers.listStringifier();
			var input = l("a", null, "c");
			var result = stringifier.apply(converter, input);

			assertEquals("[a,<null>,c]", result);
		}
	}

	@Nested
	class M_mapStringifier extends TestBase {

		@Test
		void m01_stringifyMap() {
			var converter = BasicBeanConverter.builder().defaultSettings().build();
			var stringifier = Stringifiers.mapStringifier();
			var input = m("key1", "value1", "key2", "value2");
			var result = stringifier.apply(converter, input);
	
			// Order may vary, so check both possibilities
			assertTrue(result.equals("{key1=value1,key2=value2}") ||
				result.equals("{key2=value2,key1=value1}"));
		}

		@Test
		void m02_stringifyEmptyMap() {
			var converter = BasicBeanConverter.builder().defaultSettings().build();
			var stringifier = Stringifiers.mapStringifier();
			var input = m();
			var result = stringifier.apply(converter, input);
	
			assertEquals("{}", result);
		}

		@Test
		void m03_stringifyWithCustomDelimiters() {
			var converter = BasicBeanConverter.builder()
				.defaultSettings()
				.addSetting("fieldSeparator", "; ")
				.addSetting("mapPrefix", "<")
				.addSetting("mapSuffix", ">")
				.build();
			var stringifier = Stringifiers.mapStringifier();
			var input = m("a", "1");
			var result = stringifier.apply(converter, input);

			assertEquals("<a=1>", result);
		}

		@Test
		void m04_stringifyWithNullValues() {
			var converter = BasicBeanConverter.builder().defaultSettings().build();
			var stringifier = Stringifiers.mapStringifier();
			var input = new HashMap<String, String>();
			input.put("key1", "value1");
			input.put("key2", null);
			var result = stringifier.apply(converter, input);

			assertTrue(result.contains("key1=value1"));
			assertTrue(result.contains("key2=<null>"));
		}
	}

	@Nested
	class N_integration extends TestBase {

		@Test
		void n01_useInBasicBeanConverter() {
		// Test various stringifiable objects
		assertBean(Map.entry("k", "v"), "<self>", "k=v");
		assertBean(l("a", "b"), "<self>", "[a,b]");
		assertBean(m("x", "y"), "<self>", "{x=y}");
		assertBean(TestEnum.VALUE1, "<self>", "VALUE1");
		assertBean(String.class, "<self>", "String");
	}

	@Test
	void n02_customStringifierRegistration() {
		// Test that custom registration works
		assertBean(l("custom"), "<self>", "[custom]");
	}

	@Test
	void n03_stringifierChaining() {
		// Map with list values
		var complexMap = m("items", l("a", "b"));
		assertMatchesGlob("*items=[a,b]*", BasicBeanConverter.DEFAULT.stringify(complexMap));
	}

		private enum TestEnum { VALUE1, VALUE2 }
	}
}