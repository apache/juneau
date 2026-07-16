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
package org.apache.juneau.commons.lang;

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.junit.jupiter.api.Assertions.*;

import java.math.*;
import java.util.*;

import org.apache.juneau.commons.*;
import org.apache.juneau.commons.function.*;
import org.junit.jupiter.api.*;

/**
 * Tests for the printf-only {@link StringFormat} engine.
 *
 * <p>
 * MessageFormat-grammar behavior is no longer owned by this engine; it lives behind
 * {@code Shorts.mf(...)}/{@code Shorts.mfs(...)} / {@code StringUtils.mformat(...)} (backed by
 * {@link java.text.MessageFormat}) and is exercised in {@code Shorts_Test}.
 */
@SuppressWarnings({
	"java:S5961" // High assertion count acceptable in comprehensive test
})
class StringFormat_Test extends TestBase {

	private static StringFormat fs(String pattern) {
		return StringFormat.ofPrintf(pattern);
	}

	private static String stringify(ThrowingSupplier<String> supplier) {
		try {
			return supplier.get();
		} catch (Throwable t) {
			return cns(t) + ": " + t.getLocalizedMessage();
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
			actual = cns(t) + ": " + t.getLocalizedMessage();
		}
		if (!expected.equals(actual)) {
			System.out.println("Pattern: " + pattern);
			var toPattern = o(fmt).map(x -> x.toPattern()).orElse(null);
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
			actual = cns(t) + ": " + t.getLocalizedMessage();
		}
		if (!expected.equals(actual)) {
			System.out.println("Pattern: " + pattern);
			var toPattern = o(fmt).map(x -> x.toPattern()).orElse(null);
			System.out.println("toPattern(): " + toPattern);
			fail("Pattern: " + pattern + ", toPattern(): " + toPattern + ", expected: <" + expected + "> but was: <" + actual + ">");
		}
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
	// Printf value-adds beyond raw String.format
	//====================================================================================================
	@Test void a04_printfValueAdds() {
		// { and ' are literal text on the printf path (java.util.Formatter semantics).
		assertEquals("Hello {0} world", fs("Hello {0} world").format("ignored-extra-arg"));
		assertEquals("a '{0}' b", fs("a '{0}' b").format("ignored"));

		// BigDecimal with %d - String.format throws exception, but our optimized code handles it via longValue().
		assertEquals("Number: 42", fs("Number: %d").format(new BigDecimal("42")));

		// Lenient null handling for simple string conversions (renders "null" rather than throwing).
		assertEquals("Value: null", fs("Value: %s").format((Object)null));
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
		assertSame(fs("Hello %s"), fs("Hello %s"));

		// Different patterns should return different instances
		assertNotSame(fs("Hello %s"), fs("Hello %d"));

		// Constructor doesn't use cache, so instances should be different
		var fmt1 = new StringFormat("Hello %s");
		var fmt2 = new StringFormat("Hello %s");
		assertNotSame(fmt1, fmt2);
		assertEquals(fmt1, fmt2); // But they should be equal
	}

	@Test void a07_equalsAndHashCode() {
		var fmt1 = StringFormat.ofPrintf("Hello %s");
		var fmt2 = StringFormat.ofPrintf("Hello %s");
		var fmt3 = StringFormat.ofPrintf("Hello %d");

		// Test equals
		assertEquals(fmt1, fmt2);
		assertNotEquals(fmt1, fmt3);

		// Test equals with null (instanceof check fails)
		assertNotEquals(null, fmt1);

		// Test equals with different type (instanceof check fails)
		assertNotEquals("Hello %s", fmt1);
		assertNotEquals(fmt1, new Object());

		// Test equals with different pattern
		var fmt4 = StringFormat.ofPrintf("Different pattern");
		assertNotEquals(fmt1, fmt4);

		// Test hashCode
		assertEquals(fmt1.hashCode(), fmt2.hashCode());
	}

	@Test void a08_toString() {
		assertEquals("Hello %s", fs("Hello %s").toString());
	}

	@Test void a09_toPattern() {
		// Literal tokens ({ and ' are literal on the printf path)
		assertEquals("[L:Hello ]", fs("Hello ").toPattern());
		assertEquals("[L:a '{0}' b]", fs("a '{0}' b").toPattern());
		assertEquals("[L:Hello {0}]", fs("Hello {0}").toPattern());

		// StringFormat (printf) tokens
		assertEquals("[L:Hello ][S:s0:%s]", fs("Hello %s").toPattern());  // Simple format: 's'
		assertEquals("[L:Number: ][S:d0:%d]", fs("Number: %d").toPattern());  // Simple format: 'd'
		assertEquals("[L:Hex: ][S:x0:%x]", fs("Hex: %x").toPattern());  // Simple format: 'x'
		assertEquals("[L:Float: ][S:z0:%.2f]", fs("Float: %.2f").toPattern());  // Complex format: 'z' (other)
		assertEquals("[L:ID: ][S:z0:%05d]", fs("ID: %05d").toPattern());  // Complex format: 'z' (other)
		assertEquals("[L:Hello ][S:s0:%s][L: ][S:s1:%s]", fs("Hello %s %s").toPattern());

		// Time conversions (2-character) - 't' or 'T' handling
		assertEquals("[L:Month: ][S:z0:%tm]", fs("Month: %tm").toPattern());  // %tm is 2-character time conversion
		assertEquals("[L:Year: ][S:z0:%tY]", fs("Year: %tY").toPattern());  // %tY is 2-character time conversion
		assertEquals("[L:Date: ][S:z0:%TD]", fs("Date: %TD").toPattern());  // %TD is 2-character time conversion

		// %n doesn't consume an argument - it's handled as a LiteralToken
		var lineSep = System.lineSeparator();
		assertEquals("[L:Line 1][L:" + lineSep + "][L:Line 2]", fs("Line 1%nLine 2").toPattern());
		assertEquals("[S:s0:%s][L: ][L:" + lineSep + "][L: ][S:s1:%s]", fs("%s %n %s").toPattern());  // %n is a literal, so second %s gets index 1
	}

	@Test void a10_veryLongPattern() {
		var sb = new StringBuilder("Start:");
		var args = new Object[10];
		for (var i = 0; i < 10; i++) {
			sb.append(" %s");
			args[i] = i;
		}
		assertStringFormat(sb.toString(), args);
	}

	@Test void a12_localeHandling() {
		// Test with null locale (covers locale == null on both lines)
		assertStringFormat("Hello %s", (Locale)null, "John");
		assertStringFormat("Number: %d", (Locale)null, 42);
		assertStringFormat("Float: %.2f", (Locale)null, 3.14);  // Use .2f for consistent formatting

		// Test with default locale (covers locale.equals(Locale.getDefault()))
		assertStringFormat("Hello %s", Locale.getDefault(), "John");
		assertStringFormat("Number: %d", Locale.getDefault(), 42);
		assertStringFormat("Float: %.2f", Locale.getDefault(), 3.14);  // Use .2f for consistent formatting

		// Test with non-default locale (covers else branch and false case)
		assertStringFormat("Hello %s", Locale.FRANCE, "John");
		assertStringFormat("Number: %d", Locale.GERMANY, 42);
		assertStringFormat("Float: %.2f", Locale.JAPAN, 3.14);  // Use .2f for consistent formatting
	}

	//====================================================================================================
	// formatPrintf(String, Locale, Object...)
	//====================================================================================================
	@Test void a13_formatPrintf_withLocale() {
		// Test with empty args (args.length == 0, return pattern)
		assertEquals("Hello", StringFormat.formatPrintf("Hello", Locale.US));
		assertEquals("Test pattern", StringFormat.formatPrintf("Test pattern", Locale.FRANCE));

		// Test with args
		assertEquals("Hello World", StringFormat.formatPrintf("Hello %s", Locale.US, "World"));

		// Test with null locale and empty args
		assertEquals("Test", StringFormat.formatPrintf("Test", (Locale)null));

		// Default-locale convenience entry point
		assertEquals("Hello World", StringFormat.formatPrintf("Hello %s", "World"));
		assertEquals("NoArgs", StringFormat.formatPrintf("NoArgs"));
	}

	//====================================================================================================
	// Missing-argument behavior in StringFormatToken.append()
	//====================================================================================================
	@Test void a15_stringFormatTokenBranches() {
		// Missing args -> MissingFormatArgumentException (matches String.format)
		var fmt1 = StringFormat.ofPrintf("Hello %s");
		assertThrows(MissingFormatArgumentException.class, () -> fmt1.format((Object[])null));

		var fmt2 = StringFormat.ofPrintf("Price: %.2f");
		assertThrows(MissingFormatArgumentException.class, () -> fmt2.format((Object[])null));

		var fmt3 = StringFormat.ofPrintf("First: %1$s, Second: %2$s");
		assertThrows(MissingFormatArgumentException.class, () -> fmt3.format((Object[])null));
	}
}
