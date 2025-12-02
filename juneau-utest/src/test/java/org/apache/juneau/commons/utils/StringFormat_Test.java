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
package org.apache.juneau.commons.utils;

import static org.apache.juneau.commons.utils.Utils.*;
import static org.junit.jupiter.api.Assertions.*;
import static java.util.stream.Collectors.*;

import java.math.*;
import java.text.*;
import java.util.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.function.*;
import org.junit.jupiter.api.*;

class StringFormat_Test extends TestBase {

	private static StringFormat fs(String pattern) {
		return StringFormat.of(pattern);
	}

	private static String stringify(ThrowingSupplier<String> supplier) {
		try {
			return supplier.get();
		} catch (Throwable t) {
			return t.getClass().getSimpleName() + ": " + t.getLocalizedMessage();
		}
	}

	private static void assertStringFormat(String pattern, Locale locale, Object... args) {
		var expected = stringify(()->String.format(locale, pattern, args));
		var actual = "";
		var fmt = (StringFormat)null;
		try {
			var fmt2 = fs(pattern);
			fmt = fmt2;
			actual = stringify(()->fmt2.format(locale, args));
		} catch (Throwable t) {
			actual = t.getClass().getSimpleName() + ": " + t.getLocalizedMessage();
		}
		if (!expected.equals(actual)) {
			System.out.println("Pattern: " + pattern);
			var toPattern = opt(fmt).map(x -> x.toPattern()).orElse(null);
			System.out.println("toPattern(): " + toPattern);
			fail("Pattern: " + pattern + ", toPattern(): " + toPattern + ", expected: <" + expected + "> but was: <" + actual + ">");
		}
	}

	private static void assertStringFormat(String pattern, Object... args) {
		var expected = stringify(()->String.format(pattern, args));
		var actual = "";
		var fmt = (StringFormat)null;
		try {
			var fmt2 = fs(pattern);
			fmt = fmt2;
			actual = stringify(()->fmt2.format(args));
		} catch (Throwable t) {
			actual = t.getClass().getSimpleName() + ": " + t.getLocalizedMessage();
		}
		if (!expected.equals(actual)) {
			System.out.println("Pattern: " + pattern);
			var toPattern = opt(fmt).map(x -> x.toPattern()).orElse(null);
			System.out.println("toPattern(): " + toPattern);
			fail("Pattern: " + pattern + ", toPattern(): " + toPattern + ", expected: <" + expected + "> but was: <" + actual + ">");
		}
	}

	private static void assertMessageFormat(String pattern, Locale locale, Object... args) {
		var expected = stringify(()->new MessageFormat(pattern, locale).format(args));
		var actual = "";
		var fmt = (StringFormat)null;
		try {
			var fmt2 = fs(pattern);
			fmt = fmt2;
			actual = stringify(()->fmt2.format(locale, args));
		} catch (Throwable t) {
			actual = t.getClass().getSimpleName() + ": " + t.getLocalizedMessage();
		}
		if (!expected.equals(actual)) {
			System.out.println("Pattern: " + pattern);
			var toPattern = opt(fmt).map(x -> x.toPattern()).orElse(null);
			System.out.println("toPattern(): " + toPattern);
			fail("Pattern: " + pattern + ", toPattern(): " + toPattern + ", expected: <" + expected + "> but was: <" + actual + ">");
		}
	}

	private static void assertMessageFormat(String pattern, Object... args) {
		var expected = stringify(()->MessageFormat.format(pattern, args));
		var actual = "";
		var fmt = (StringFormat)null;
		try {
			var fmt2 = fs(pattern);
			fmt = fmt2;
			actual = stringify(()->fmt2.format(args));
		} catch (Throwable t) {
			actual = t.getClass().getSimpleName() + ": " + t.getLocalizedMessage();
		}
		if (!expected.equals(actual)) {
			System.out.println("Pattern: " + pattern);
			var toPattern = opt(fmt).map(x -> x.toPattern()).orElse(null);
			System.out.println("toPattern(): " + toPattern);
			fail("Pattern: " + pattern + ", toPattern(): " + toPattern + ", expected: <" + expected + "> but was: <" + actual + ">");
		}
	}

	private static void assertMixedFormat(String expected, String pattern, Locale locale, Object... args) {
		var actual = "";
		var fmt = (StringFormat)null;
		try {
			var fmt2 = fs(pattern);
			fmt = fmt2;
			actual = stringify(()->fmt2.format(locale, args));
		} catch (Throwable t) {
			actual = t.getClass().getSimpleName() + ": " + t.getLocalizedMessage();
		}
		if (!expected.equals(actual)) {
			System.out.println("Pattern: " + pattern);
			var toPattern = opt(fmt).map(x -> x.toPattern()).orElse(null);
			System.out.println("toPattern(): " + toPattern);
			fail("Pattern: " + pattern + ", toPattern(): " + toPattern + ", expected: <" + expected + "> but was: <" + actual + ">");
		}
	}

	private static void assertMixedFormat(String expected, String pattern, Object... args) {
		var actual = "";
		var fmt = (StringFormat)null;
		try {
			var fmt2 = fs(pattern);
			fmt = fmt2;
			actual = stringify(()->fmt2.format(args));
		} catch (Throwable t) {
			actual = t.getClass().getSimpleName() + ": " + t.getLocalizedMessage();
		}
		if (!expected.equals(actual)) {
			System.out.println("Pattern: " + pattern);
			var toPattern = opt(fmt).map(x -> x.toPattern()).orElse(null);
			System.out.println("toPattern(): " + toPattern);
			fail("Pattern: " + pattern + ", toPattern(): " + toPattern + ", expected: <" + expected + "> but was: <" + actual + ">");
		}
	}

	//====================================================================================================
	// MessageFormat tests
	//====================================================================================================
	@Test void a01_messageFormat() {
		assertMessageFormat("Hello {0}", "John");
		assertMessageFormat("Price: {0,number,currency}", 19.99);
		assertMessageFormat("{0} has {1} items and {2} friends", "John", 5, 3);
		assertMessageFormat("Hello {0} world", "John");
		assertMessageFormat("Count: {0,number,integer}", 1234);
		assertMessageFormat("Date: {0,date,short}", new Date(0));
		assertMessageFormat("Time: {0,time,short}", new Date(0));
		// Simple {0} with Date - uses DATE_FORMAT_CACHE for formatting
		assertMessageFormat("Date: {0}", new Date(0));
		assertMessageFormat("Value: {0}", (String)null);
		assertMessageFormat("Name: {0}", "");
		assertMessageFormat("Text: {0}\nNewline\tTab", "Hello");
		assertMessageFormat("Unicode: {0} 中文", "Test");
		assertMessageFormat("{0}{1}", "A", "B");
		assertMessageFormat("{0} and {0} again", "Hello");
		assertMessageFormat("Price: {0,number,currency}, Count: {1,number,integer}, Date: {2,date,short}", 19.99, 42, new java.util.Date());
		assertMessageFormat("Price: {0,number,currency}", Locale.US, 19.99);
		assertMessageFormat("Price: {0,number,currency}", Locale.FRANCE, 19.99);
		assertMessageFormat("a '{0}' b");
		assertMessageFormat("a ''{0}'' b", 1);
		assertMessageFormat("'{0}'");
		assertMessageFormat("''{0}''", 1);

		// Errors
		assertMessageFormat("Set: {{0}}", 50);
		assertMessageFormat("Set: {{0}} and {{1}}", "A", "B");
		assertMessageFormat("Hello {0}");
		assertMessageFormat("{0} has {1} items and {2} friends", "John", 5);
		assertMessageFormat("Hello {");
		assertMessageFormat("Hello {0");
		assertMessageFormat("Hello '");
		assertMessageFormat("Hello 'x");
	}

	//====================================================================================================
	// StringFormat (printf) tests
	//====================================================================================================
	@Test void a02_stringFormat() {
		assertStringFormat("Hello %s", "John");
		assertStringFormat("Price: $%.2f", 19.99);
		assertStringFormat("Name: %-10s Age: %3d", "John", 25);
		assertStringFormat("Color: #%06X", 0xFF5733);
		assertStringFormat("Hello world");
		assertStringFormat("Progress: %d%%", 50);
		assertStringFormat("");
		assertStringFormat("%1$s loves %2$s, and %1$s also loves %3$s", "Alice", "Bob", "Charlie");
		assertStringFormat("Hello %1$s", "John");
		assertStringFormat("Price: %1$.2f", 19.99);
		assertStringFormat("Octal: %o", 64);
		assertStringFormat("Octal: %o", 255);
		assertStringFormat("Octal: %o", (Number)null);
		assertStringFormat("Flag: %b", true);
		assertStringFormat("Flag: %b", false);
		assertStringFormat("Flag: %b", (Boolean)null);
		// %B uppercase boolean formatting
		assertStringFormat("Flag: %B", true);
		assertStringFormat("Flag: %B", false);
		assertStringFormat("Flag: %B", (Boolean)null);  // Line 281: null -> "FALSE"
		assertStringFormat("Flag: %B", "hello");  // Line 285: non-Boolean -> "TRUE"
		assertStringFormat("Flag: %B", 42);  // Line 285: non-Boolean -> "TRUE"
		assertStringFormat("Char: %c", 'A');
		assertStringFormat("Char: %c", "A");
		assertStringFormat("Char: %c", (String)null);
		assertStringFormat("Value: %.2e", 1234567.0);
		assertStringFormat("Value: %.2e", (Number)null);
		assertStringFormat("Number: %+10.2f", 19.99);
		assertStringFormat("ID: %05d", 42);
		assertStringFormat("Value: %d", (Number)null);  // Line 304: null -> "null"
		assertStringFormat("Value: %s", (String)null);
		assertStringFormat("Name: %s", "");
		assertStringFormat("%s%s", "A", "B");
		assertStringFormat("Progress: %d%% Complete: %d%%", 50, 75);
		assertStringFormat("%1$s and %1$s again", "Hello");
		assertStringFormat("Hex: 0x%08X, Decimal: %+d, Float: %10.3f", 255, 42, 3.14159);
		assertStringFormat("Price: %.2f", Locale.US, 19.99);
		assertStringFormat("Price: %.2f", Locale.FRANCE, 19.99);
		assertStringFormat("Value: %s", (Object)null);
		assertStringFormat("Int: %d", 42);
		assertStringFormat("Long: %d", 1234567890L);
		assertStringFormat("Byte: %d", (byte)127);
		assertStringFormat("Short: %d", (short)32767);
		assertStringFormat("Int: %d", Locale.FRANCE, 1234);
		assertStringFormat("Long: %d", Locale.GERMANY, 1234567L);
		assertStringFormat("Value: %d", "not-a-number");
		assertStringFormat("Hex: %x", 255);  // Line 328: Integer -> Integer.toHexString()
		assertStringFormat("Hex: %x", 255L);
		assertStringFormat("Hex: %x", (byte)255);
		assertStringFormat("Hex: %x", (Number)null);  // Line 324: null -> "null"
		assertStringFormat("Hex: %X", 255);
		assertStringFormat("Hex: %X", 0xABCL);
		assertStringFormat("Hex: %X", (short)255);
		assertStringFormat("Hex: %X", (Number)null);  // Line 337: null -> "null"
		assertStringFormat("Octal: %o", 255L);
		assertStringFormat("Octal: %o", (byte)64);
		assertStringFormat("Value: %b", "hello");
		assertStringFormat("Value: %b", 42);
		assertStringFormat("Char: %c", 65);
		assertStringFormat("Char: %c", 65L);
		assertStringFormat("Char: %c", "X");
		assertStringFormat("Char: %C", (Character)null);  // Line 376: null -> "null"
		assertStringFormat("Char: %C", 66);  // Line 382-383: Number (Integer) -> Character.toUpperCase((char)o2.intValue())
		assertStringFormat("Char: %C", 66L);  // Line 382-383: Number (Long) -> Character.toUpperCase((char)o2.intValue())
		assertStringFormat("Float: %f", 3.14f);
		assertStringFormat("Double: %f", 3.14159);
		assertStringFormat("Float: %f", Locale.FRANCE, 3.14f);
		assertStringFormat("Double: %f", Locale.GERMANY, 1234.56);
		assertStringFormat("Value: %f", (Number)null);  // Line 389: null -> "null"
		assertStringFormat("Value: %f", "not-a-number");
		assertStringFormat("Value: %.2e", 1234.56);
		assertStringFormat("Value: %S", "hello");
		assertStringFormat("Value: %S", (String)null);  // Line 297: null -> "null"
		assertStringFormat("Value: %B", true);
		assertStringFormat("Char: %C", 'a');
		assertStringFormat("Float: %F", 3.14);
		// %n doesn't consume an argument - test sequential index behavior
		assertStringFormat("Line 1%nLine 2");
		assertStringFormat("First: %s%nSecond: %s", "one", "two");
		assertStringFormat("%s %n %s", "first", "second");

		// Errors
		assertStringFormat("Hello %s");
		assertStringFormat("Hello %s and %s", "John");
		assertStringFormat("Hello %");
		assertStringFormat("Hello %s and %", "John");
		assertStringFormat("Hello %x$s", "John");
	}

	//====================================================================================================
	// Mixed format tests
	//====================================================================================================
	@Test void a03_mixedFormat() {
		assertMixedFormat("Hello John, you have 5 items", "Hello {0}, you have %d items", "John", 5);
		assertMixedFormat("User Alice has admin and 10 items", "User {0} has %s and {2} items", "Alice", "admin", 10);
		assertMixedFormat("Alice loves Bob, and Alice also loves Charlie", "%1$s loves %2$s, and {0} also loves %3$s", "Alice", "Bob", "Charlie");
		assertMixedFormat("Alice has 5 items, Bob has 3 items, total: 8", "{0} has %d items, {2} has %d items, total: %d", "Alice", 5, "Bob", 3, 8);
		assertMixedFormat("Alice Bob Charlie", "{0} %2$s {2}", "Alice", "Bob", "Charlie");
		assertMixedFormat("Hello John, you have 5 items", "Hello {0}, you have %d items", "John", 5);
		assertMixedFormat("A B B D C", "{0} %s {1} %s {2}", "A", "B", "C", "D");
		assertMixedFormat("ABB", "{0}%s{1}", "A", "B", "C");
		assertMixedFormat("Hello and Hello are the same", "{0} and %1$s are the same", "Hello");

		// Errors
		assertMixedFormat("MissingFormatArgumentException: Format specifier '%s'", "Hello {0} and %s", "John");
		assertMixedFormat("John has 5 items and {2} friends", "{0} has %d items and {2} friends", "John", 5);
		assertMixedFormat("MissingFormatArgumentException: Format specifier '%s'", "%1$s loves %2$s, and {0} also loves %3$s", "Alice", "Bob");
	}

	//====================================================================================================
	// Supported but deviates from MessageFormat/String.format
	//====================================================================================================
	@Test void a04_supportedButDeviatesFromMessageFormat() {
		// {} is not supported by MessageFormat, only by StringFormat as an extension
		assertMixedFormat("Hello John world", "Hello {} world", "John");
		assertMixedFormat("A B C", "{} {} {}", "A", "B", "C");
		// BigDecimal with %d - String.format throws exception, but our optimized code handles it
		assertMixedFormat("Number: 42", "Number: %d", new BigDecimal("42"));
		// MessageFormat throws NullPointerException when locale is null, but StringFormat handles it
		// So we test StringFormat's behavior directly instead of comparing with MessageFormat
		// Use Locale.US to get dollar sign for currency formatting
		assertMixedFormat("Price: $19.99", "Price: {0,number,currency}", Locale.US, 19.99);
	}

	//====================================================================================================
	// Error handling
	//====================================================================================================
	@Test void a05_errors() {
		assertThrows(IllegalArgumentException.class, () -> new StringFormat(null));
		assertThrows(IllegalArgumentException.class, () -> fs(null));
	}

	@Test void a06_caching() {
		// Should return the same instance due to caching
		assertSame(fs("Hello {0}"), fs("Hello {0}"));

		// Different patterns should return different instances
		assertNotSame(fs("Hello {0}"), fs("Hello %s"));

		// Constructor doesn't use cache, so instances should be different
		var fmt1 = new StringFormat("Hello {0}");
		var fmt2 = new StringFormat("Hello {0}");
		assertNotSame(fmt1, fmt2);
		assertEquals(fmt1, fmt2); // But they should be equal
	}

	@Test void a07_equalsAndHashCode() {
		var fmt1 = StringFormat.of("Hello {0}");
		var fmt2 = StringFormat.of("Hello {0}");
		var fmt3 = StringFormat.of("Hello %s");

		assertEquals(fmt1, fmt2);
		assertNotEquals(fmt1, fmt3);
		assertEquals(fmt1.hashCode(), fmt2.hashCode());
	}

	@Test void a08_toString() {
		assertEquals("Hello {0}", fs("Hello {0}").toString());
	}

	@Test void a09_toPattern() {
		// Literal tokens
		assertEquals("[L:Hello ]", fs("Hello ").toPattern());
		assertEquals("[L:a ][L:{0}][L: b]", fs("a '{0}' b").toPattern());  // Single quotes don't escape MessageFormat

		// MessageFormat tokens - simple (content == null) - Line 228: content == null branch
		assertEquals("[L:Hello ][M:s0]", fs("Hello {0}").toPattern());
		assertEquals("[L:Hello ][M:s0][L: ][M:s1]", fs("Hello {0} {1}").toPattern());

		// MessageFormat tokens - complex (content != null) - Line 228: content != null branch
		assertEquals("[L:Price: ][M:o0:{0,number,currency}]", fs("Price: {0,number,currency}").toPattern());
		assertEquals("[L:Count: ][M:o0:{0,number,integer}]", fs("Count: {0,number,integer}").toPattern());
		assertEquals("[L:Date: ][M:o0:{0,date,short}]", fs("Date: {0,date,short}").toPattern());

		// StringFormat tokens - Line 406: StringFormatToken.toString()
		assertEquals("[L:Hello ][S:s0:%s]", fs("Hello %s").toPattern());  // Simple format: 's'
		assertEquals("[L:Number: ][S:d0:%d]", fs("Number: %d").toPattern());  // Simple format: 'd'
		assertEquals("[L:Hex: ][S:x0:%x]", fs("Hex: %x").toPattern());  // Simple format: 'x'
		assertEquals("[L:Float: ][S:z0:%.2f]", fs("Float: %.2f").toPattern());  // Complex format: 'z' (other)
		assertEquals("[L:ID: ][S:z0:%05d]", fs("ID: %05d").toPattern());  // Complex format: 'z' (other)

		// Mixed formats
		assertEquals("[L:Hello ][M:s0][L:, you have ][S:d1:%d][L: items]", fs("Hello {0}, you have %d items").toPattern());
		assertEquals("[L:Price: ][M:o0:{0,number,currency}][L: and ][S:s1:%s]", fs("Price: {0,number,currency} and %s").toPattern());

		// Time conversions (2-character) - Line 529: 't' or 'T' handling
		assertEquals("[L:Month: ][S:z0:%tm]", fs("Month: %tm").toPattern());  // %tm is 2-character time conversion
		assertEquals("[L:Year: ][S:z0:%tY]", fs("Year: %tY").toPattern());  // %tY is 2-character time conversion
		assertEquals("[L:Date: ][S:z0:%TD]", fs("Date: %TD").toPattern());  // %TD is 2-character time conversion

		// %n doesn't consume an argument - it's handled as a LiteralToken
		var lineSep = System.lineSeparator();
		assertEquals("[L:Line 1][L:" + lineSep + "][L:Line 2]", fs("Line 1%nLine 2").toPattern());
		assertEquals("[S:s0:%s][L: ][L:" + lineSep + "][L: ][S:s1:%s]", fs("%s %n %s").toPattern());  // %n is a literal, so second %s gets index 1
	}

	@Test void a10_veryLongPattern() {
		var pattern = "Start: " + IntStream.range(0, 10).mapToObj(i -> "{" + i + "}").collect(joining(" ")) + " ";
		var args = IntStream.range(0, 10).boxed().toArray();
		assertMessageFormat(pattern, args);
	}

	@Test void a11_parseIndexErrors() {
		assertThrows(IllegalArgumentException.class, () -> fs("Hello {abc}"));
	}

	@Test void a12_localeHandling() {
		// Lines 259-260: Test locale null checks and default locale detection in StringFormatToken
		// Line 259: var l = locale == null ? Locale.getDefault() : locale;
		// Line 260: var dl = locale == null || locale.equals(Locale.getDefault());

		// Test with null locale (covers locale == null on both lines)
		assertStringFormat("Hello %s", (Locale)null, "John");
		assertStringFormat("Number: %d", (Locale)null, 42);
		assertStringFormat("Float: %.2f", (Locale)null, 3.14);  // Use .2f for consistent formatting

		// Test with default locale (covers locale.equals(Locale.getDefault()) on line 260)
		assertStringFormat("Hello %s", Locale.getDefault(), "John");
		assertStringFormat("Number: %d", Locale.getDefault(), 42);
		assertStringFormat("Float: %.2f", Locale.getDefault(), 3.14);  // Use .2f for consistent formatting

		// Test with non-default locale (covers else branch on line 259 and false case on line 260)
		assertStringFormat("Hello %s", Locale.FRANCE, "John");
		assertStringFormat("Number: %d", Locale.GERMANY, 42);
		assertStringFormat("Float: %.2f", Locale.JAPAN, 3.14);  // Use .2f for consistent formatting
	}
}
