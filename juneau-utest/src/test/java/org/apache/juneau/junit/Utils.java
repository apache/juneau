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
package org.apache.juneau.junit;

import static java.util.Optional.*;

import java.lang.reflect.*;
import java.text.*;
import java.util.*;
import java.util.function.*;
import java.util.regex.*;

import org.opentest4j.*;

/**
 * Utility class providing common helper methods for testing operations.
 *
 * <p>This class contains static utility methods that support various testing scenarios including:
 * <ul>
 *   <li><b>String formatting and messaging:</b> Methods for formatting messages with parameters</li>
 *   <li><b>Object comparison and validation:</b> Equality checking and argument validation</li>
 *   <li><b>Pattern matching:</b> Wildcard to regex pattern conversion</li>
 *   <li><b>Data conversion:</b> Array to list conversion and Java string escaping</li>
 *   <li><b>Exception handling:</b> Safe execution wrappers and assertion utilities</li>
 * </ul>
 *
 * <h5 class='section'>Usage Examples:</h5>
 * <p class='bjava'>
 *   <jk>import static</jk> com.sfdc.junit.bct.Utils.*;
 *
 *   <jc>// String formatting</jc>
 *   String <jv>msg</jv> = <jsm>f</jsm>(<js>"User {0} has {1} items"</js>, <js>"Alice"</js>, 5);
 *
 *   <jc>// String formatting with supplier</jc>
 *   Supplier&lt;String&gt; <jv>msg</jv> = <jsm>fs</jsm>(<js>"User {0} has {1} items"</js>, <js>"Alice"</js>, 5);
 *
 *   <jc>// Object validation</jc>
 *   String <jv>validated</jv> = <jsm>assertArgNotNull</jsm>(<js>"username"</js>, <jv>username</jv>);
 *
 *   <jc>// Pattern matching</jc>
 *   Pattern <jv>pattern</jv> = <jsm>getGlobMatchPattern</jsm>(<js>"user_*_temp"</js>);
 * </p>
 *
 * <p><b>Thread Safety:</b> All methods in this class are thread-safe as they are stateless static methods.
 */
class Utils {

	private Utils() {}

	/**
	 * Converts an array to a {@link List} of objects.
	 *
	 * <p>This method uses reflection to iterate through array elements and convert them to a list.
	 * It works with arrays of any type including primitive arrays.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 *   <jk>int</jk>[] <jv>array</jv> = {1, 2, 3};
	 *   List&lt;Object&gt; <jv>list</jv> = <jsm>arrayToList</jsm>(<jv>array</jv>);
	 *   <jc>// list contains [1, 2, 3] as Objects</jc>
	 * </p>
	 *
	 * @param o The array object to convert. Must be an actual array.
	 * @return A new {@link ArrayList} containing all array elements as objects.
	 * @throws IllegalArgumentException If the object is not an array.
	 * @throws NullPointerException If the array parameter is null.
	 */
	public static List<Object> arrayToList(Object o) {
		var l = new ArrayList<>();
		for (var i = 0; i < Array.getLength(o); i++)
			l.add(Array.get(o, i));
		return l;
	}

	/**
	 * Validates a boolean expression and throws an {@link IllegalArgumentException} if false.
	 *
	 * <p>This method provides a convenient way to validate method arguments with descriptive error messages.
	 * The message template supports {@link MessageFormat} syntax for parameter substitution.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 *   <jsm>assertArg</jsm>(<jv>age</jv> &gt;= 0, <js>"Age must be non-negative but was {0}"</js>, <jv>age</jv>);
	 *   <jsm>assertArg</jsm>(<jv>name</jv> != <jk>null</jk> &amp;&amp; !<jv>name</jv>.isEmpty(), <js>"Name cannot be null or empty"</js>);
	 * </p>
	 *
	 * @param expression The boolean expression to validate.
	 * @param msg The error message template using {@link MessageFormat} syntax.
	 * @param args Optional parameters for the message template.
	 * @throws IllegalArgumentException If the expression evaluates to false.
	 */
	public static final void assertArg(boolean expression, String msg, Object...args) throws IllegalArgumentException {
		if (! expression)
			throw new IllegalArgumentException(MessageFormat.format(msg, args));
	}

	/**
	 * Validates that an argument is not null and throws an {@link IllegalArgumentException} if it is.
	 *
	 * <p>This method provides fluent argument validation by returning the validated argument,
	 * allowing it to be used inline in assignment statements.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 *   <jk>public</jk> <jk>void</jk> setUsername(String <jp>username</jp>) {
	 *       <jk>this</jk>.<jv>username</jv> = <jsm>assertArgNotNull</jsm>(<js>"username"</js>, <jp>username</jp>);
	 *   }
	 * </p>
	 *
	 * @param <T> The argument data type.
	 * @param name The argument name for error reporting.
	 * @param o The object to validate.
	 * @return The same argument if not <jk>null</jk>.
	 * @throws IllegalArgumentException If the argument is null.
	 */
	public static final <T> T assertArgNotNull(String name, T o) throws IllegalArgumentException {
		assertArg(o != null, "Argument ''{0}'' cannot be null.", name);
		return o;
	}

	/**
	 * Creates an {@link AssertionFailedError} for failed equality assertions.
	 *
	 * <p>This method constructs a properly formatted assertion failure with expected and actual values
	 * for use in test frameworks. The message follows JUnit's standard format for assertion failures.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 *   <jk>if</jk> (<jsm>ne</jsm>(<jv>expected</jv>, <jv>actual</jv>)) {
	 *       <jk>throw</jk> <jsm>assertEqualsFailed</jsm>(<jv>expected</jv>, <jv>actual</jv>, () -&gt; <js>"Custom context message with arg {0}"</js>, <jv>arg</jv>);
	 *   }
	 * </p>
	 *
	 * @param expected The expected value.
	 * @param actual The actual value that was encountered.
	 * @param messageSupplier Optional supplier for additional context message.
	 * @return A new {@link AssertionFailedError} with formatted message and values.
	 */
	public static AssertionFailedError assertEqualsFailed(Object expected, Object actual, Supplier<String> messageSupplier) {
		return new AssertionFailedError(ofNullable(messageSupplier).map(x -> x.get()).orElse("Equals assertion failed.") + f(" ==> expected: <{0}> but was: <{1}>", expected, actual), expected, actual);
	}

	/**
	 * Tests two objects for equality using the provided test predicate.
	 *
	 * <p>This method provides null-safe equality testing with custom comparison logic.
	 * It handles null values appropriately and checks for reference equality before
	 * applying the custom test predicate. This method is primarily designed for
	 * implementing {@code equals(Object)} methods in a concise and safe manner.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 *   <jc>// Implementing equals() method</jc>
	 *   <ja>@Override</ja>
	 *   <jk>public boolean</jk> equals(Object obj) {
	 *       <jk>return</jk> (obj <jk>instanceof</jk> Token <jv>other</jv>) &amp;&amp; <jsm>eq</jsm>(<jk>this</jk>, <jv>other</jv>, (<jp>x</jp>,<jp>y</jp>) -&gt;
	 *           <jsm>eq</jsm>(<jp>x</jp>.<jv>value</jv>, <jp>y</jp>.<jv>value</jv>) &amp;&amp; <jsm>eq</jsm>(<jp>x</jp>.<jv>nested</jv>, <jp>y</jp>.<jv>nested</jv>));
	 *   }
	 *
	 *   <jc>// Case-insensitive string comparison</jc>
	 *   <jk>boolean</jk> <jv>equal</jv> = <jsm>eq</jsm>(<jv>str1</jv>, <jv>str2</jv>, (<jp>s1</jp>, <jp>s2</jp>) -&gt; <jp>s1</jp>.equalsIgnoreCase(<jp>s2</jp>));
	 * </p>
	 *
	 * @param <T> The type of the first object.
	 * @param <U> The type of the second object.
	 * @param o1 The first object to compare.
	 * @param o2 The second object to compare.
	 * @param test The custom equality test predicate.
	 * @return true if the objects are equal according to the test predicate.
	 */
	public static <T,U> boolean eq(T o1, U o2, BiPredicate<T,U> test) {
		if (o1 == null) { return o2 == null; }
		if (o2 == null) { return false; }
		if (o1 == o2) { return true; }
		return test.test(o1, o2);
	}

	/**
	 * Tests two objects for equality using {@link Objects#equals(Object, Object)}.
	 *
	 * <p>This method provides null-safe equality testing using the standard Java equals contract.
	 * It's a convenience wrapper around {@code <jsm>Objects.equals</jsm>()}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 *   <jk>boolean</jk> equal = <jsm>eq</jsm>(<js>"hello"</js>, <js>"hello"</js>);  <jc>// true</jc>
	 *   <jk>boolean</jk> equal = <jsm>eq</jsm>(<jk>null</jk>, <jk>null</jk>);      <jc>// true</jc>
	 *   <jk>boolean</jk> equal = <jsm>eq</jsm>(<js>"hello"</js>, <jk>null</jk>);   <jc>// false</jc>
	 * </p>
	 *
	 * @param <T> The type of objects to compare.
	 * @param o1 The first object to compare.
	 * @param o2 The second object to compare.
	 * @return true if the objects are equal according to {@link Objects#equals(Object, Object)}.
	 */
	public static <T> boolean eq(T o1, T o2) {
		return Objects.equals(o1, o2);
	}

	/**
	 * Escapes a string for safe inclusion in Java source code.
	 *
	 * <p>This method converts special characters to their Java escape sequences and
	 * converts non-printable ASCII characters to Unicode escape sequences.
	 *
	 * <h5 class='section'>Escape mappings:</h5>
	 * <ul>
	 *   <li>{@code "} → {@code \"}</li>
	 *   <li>{@code \} → {@code \\}</li>
	 *   <li>{@code \n} → {@code \\n}</li>
	 *   <li>{@code \r} → {@code \\r}</li>
	 *   <li>{@code \t} → {@code \\t}</li>
	 *   <li>{@code \f} → {@code \\f}</li>
	 *   <li>{@code \b} → {@code \\b}</li>
	 *   <li>Non-printable characters → {@code \\uXXXX}</li>
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 *   <jk>var</jk> <jv>escaped</jv> = <jsm>escapeForJava</jsm>(<js>"Hello\nWorld\"Test\""</js>);
	 *   <jc>// Returns: "Hello\\nWorld\\\"Test\\\""</jc>
	 * </p>
	 *
	 * @param s The string to escape.
	 * @return The escaped string safe for Java source code.
	 * @throws NullPointerException If the string parameter is null.
	 */
	public static String escapeForJava(String s) {
		var sb = new StringBuilder();
		for (var c : s.toCharArray()) {
			switch (c) {
				case '\"': sb.append("\\\""); break;
				case '\\': sb.append("\\\\"); break;
				case '\n': sb.append("\\n"); break;
				case '\r': sb.append("\\r"); break;
				case '\t': sb.append("\\t"); break;
				case '\f': sb.append("\\f"); break;
				case '\b': sb.append("\\b"); break;
				default:
					if (c < 0x20 || c > 0x7E) {
						sb.append(String.format("\\u%04x", (int)c));
					} else {
						sb.append(c);
					}
			}
		}
		return sb.toString();
	}

	/**
	 * Formats a message template with parameters using {@link MessageFormat}.
	 *
	 * <p>This method provides convenient message formatting with support for various parameter types.
	 * If no arguments are provided, the message is returned as-is. Otherwise, it uses
	 * {@link MessageFormat#format(String, Object...)} for parameter substitution.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 *   <jk>var</jk> <jv>msg</jv> = <jsm>f</jsm>(<js>"User {0} has {1} items"</js>, <js>"Alice"</js>, 5);
	 *   <jc>// Returns: "User Alice has 5 items"</jc>
	 *
	 *   <jk>var</jk> <jv>simple</jv> = <jsm>f</jsm>(<js>"No parameters"</js>);
	 *   <jc>// Returns: "No parameters"</jc>
	 * </p>
	 *
	 * @param msg The message template with {@link MessageFormat} placeholders.
	 * @param args Optional parameters for template substitution.
	 * @return The formatted message string.
	 * @throws IllegalArgumentException If the message format is invalid.
	 */
	public static String f(String msg, Object...args) {
		return args.length == 0 ? msg : MessageFormat.format(msg, args);
	}

	/**
	 * Creates a {@link Supplier} that formats a message template with parameters.
	 *
	 * <p>This method returns a supplier that, when called, will format the message using
	 * the {@link #f(String, Object...)} method. This is useful for lazy evaluation of
	 * expensive formatting operations.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 *   Supplier&lt;String&gt; <jv>msgSupplier</jv> = <jsm>fs</jsm>(<js>"Processing {0} records"</js>, <jv>recordCount</jv>);
	 *   <jc>// Later, when needed:</jc>
	 *   String <jv>msg</jv> = <jv>msgSupplier</jv>.get();  <jc>// "Processing 150 records"</jc>
	 * </p>
	 *
	 * @param msg The message template with {@link MessageFormat} placeholders.
	 * @param args Optional parameters for template substitution.
	 * @return A {@link Supplier} that produces the formatted message when called.
	 */
	public static Supplier<String> fs(String msg, Object...args) {
		return ()->f(msg, args);
	}

	/**
	 * Converts a string containing glob-style wildcard characters to a regular expression {@link Pattern}.
	 *
	 * <p>This method converts glob-style patterns to regular expressions with the following mappings:
	 * <ul>
	 *   <li>{@code *} matches any sequence of characters (including none)</li>
	 *   <li>{@code ?} matches exactly one character</li>
	 *   <li>All other characters are treated literally</li>
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 *   <jk>var</jk> <jv>pattern</jv> = <jsm>getGlobMatchPattern</jsm>(<js>"user_*_temp"</js>);
	 *   <jk>boolean</jk> <jv>matches</jv> = <jv>pattern</jv>.matcher(<js>"user_alice_temp"</js>).matches();  <jc>// true</jc>
	 *   <jv>matches</jv> = <jv>pattern</jv>.matcher(<js>"user_bob_temp"</js>).matches();    <jc>// true</jc>
	 *   <jv>matches</jv> = <jv>pattern</jv>.matcher(<js>"admin_alice_temp"</js>).matches(); <jc>// false</jc>
	 * </p>
	 *
	 * @param s The glob-style wildcard pattern string.
	 * @return A compiled {@link Pattern} object, or null if the input string is null.
	 */
	public static Pattern getGlobMatchPattern(String s) {
		return getGlobMatchPattern(s, 0);
	}

	/**
	 * Converts a string containing glob-style wildcard characters to a regular expression {@link Pattern} with flags.
	 *
	 * <p>This method converts glob-style patterns to regular expressions with the following mappings:
	 * <ul>
	 *   <li>{@code *} matches any sequence of characters (including none)</li>
	 *   <li>{@code ?} matches exactly one character</li>
	 *   <li>All other characters are treated literally</li>
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 *   <jc>// Case-insensitive matching</jc>
	 *   <jk>var</jk> <jv>pattern</jv> = <jsm>getGlobMatchPattern</jsm>(<js>"USER_*"</js>, Pattern.<jsf>CASE_INSENSITIVE</jsf>);
	 *   <jk>boolean</jk> <jv>matches</jv> = <jv>pattern</jv>.matcher(<js>"user_alice"</js>).matches();  <jc>// true</jc>
	 * </p>
	 *
	 * @param s The glob-style wildcard pattern string.
	 * @param flags Regular expression flags (see {@link Pattern} constants).
	 * @return A compiled {@link Pattern} object, or null if the input string is null.
	 */
	public static Pattern getGlobMatchPattern(String s, int flags) {
		if (s == null)
			return null;
		var sb = new StringBuilder();
		sb.append("\\Q");
		for (var i = 0; i < s.length(); i++) {
			var c = s.charAt(i);
			if (c == '*')
				sb.append("\\E").append(".*").append("\\Q");
			else if (c == '?')
				sb.append("\\E").append(".").append("\\Q");
			else
				sb.append(c);
		}
		sb.append("\\E");
		return Pattern.compile(sb.toString(), flags);
	}

	/**
	 * Tests two objects for inequality using {@link Objects#equals(Object, Object)}.
	 *
	 * <p>This method is the logical negation of {@link #eq(Object, Object)}.
	 * It provides null-safe inequality testing using the standard Java equals contract.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 *   <jk>boolean</jk> notEqual = <jsm>ne</jsm>(<js>"hello"</js>, <js>"world"</js>);  <jc>// true</jc>
	 *   <jk>boolean</jk> notEqual = <jsm>ne</jsm>(<js>"hello"</js>, <js>"hello"</js>);  <jc>// false</jc>
	 *   <jk>boolean</jk> notEqual = <jsm>ne</jsm>(<jk>null</jk>, <jk>null</jk>);        <jc>// false</jc>
	 * </p>
	 *
	 * @param <T> The type of objects to compare.
	 * @param o1 The first object to compare.
	 * @param o2 The second object to compare.
	 * @return true if the objects are not equal according to {@link Objects#equals(Object, Object)}.
	 */
	public static <T> boolean ne(T o1, T o2) {
		return ! eq(o1, o2);
	}

	/**
	 * Safely executes a {@link ThrowingSupplier} and wraps checked exceptions as {@link RuntimeException}.
	 *
	 * <p>This method allows checked exceptions to be handled uniformly by converting them to
	 * runtime exceptions. {@link RuntimeException}s are re-thrown as-is, while checked
	 * exceptions are wrapped in a new {@link RuntimeException}.
	 *
	 * Typically used in situations where you want to embed code that throws checked exceptions inside fluent
	 * chained method calls.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 *   <jc>// Safe file reading without explicit exception handling</jc>
	 *   <jk>var</jk> <jv>content</jv> = <jsm>safe</jsm>(() -&gt; Files.readString(Paths.get(<js>"file.txt"</js>)));
	 *
	 *   <jc>// Safe URL connection</jc>
	 *   <jk>var</jk> <jv>response</jv> = <jsm>safe</jsm>(() -&gt; {
	 *       URL <jv>url</jv> = <jk>new</jk> URL(<js>"http://example.com"</js>);
	 *       <jk>return</jk> <jv>url</jv>.openConnection().getInputStream().readAllBytes();
	 *   });
	 * </p>
	 *
	 * @param <T> The return type of the supplier.
	 * @param s The {@link ThrowingSupplier} to execute safely.
	 * @return The result of the supplier execution.
	 * @throws RuntimeException If the supplier throws any checked exception.
	 */
	public static <T> T safe(ThrowingSupplier<T> s) {
		try {
			return s.get();
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns the simple class name of an object, or null if the object is null.
	 *
	 * <p>This method provides a safe way to get type information for debugging and logging.
	 * It handles <jk>null</jk> values gracefully by returning <jk>null</jk> instead of throwing an exception.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 *   <jk>var</jk> <jv>type</jv> = <jsm>t</jsm>(<js>"hello"</js>);        <jc>// "String"</jc>
	 *   <jv>type</jv> = <jk>var</jk><jsm>t</jsm>(<jk>new</jk> ArrayList()); <jc>// "ArrayList"</jc>
	 *   <jv>type</jv> = <jsm>t</jsm>(<jk>null</jk>);            <jc>// null</jc>
	 * </p>
	 *
	 * @param o The object to get the type name for.
	 * @return The simple class name of the object, or <jk>null</jk> if the object is <jk>null</jk>.
	 */
	public static String t(Object o) {
		return o == null ? null : o.getClass().getSimpleName();
	}

	/**
	 * Tokenizes a string into a list of {@link NestedTokenizer.Token} objects.
	 *
	 * <p>This method delegates to {@link NestedTokenizer#tokenize(String)} to parse
	 * structured field strings into tokens. It's commonly used for parsing field lists
	 * and nested property expressions.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 *   <jk>var</jk> <jv>tokens</jv> = <jsm>tokenize</jsm>(<js>"name,address{street,city},age"</js>);
	 *   <jc>// Parses nested field expressions</jc>
	 * </p>
	 *
	 * @param fields The field string to tokenize.
	 * @return A list of parsed tokens.
	 * @see NestedTokenizer#tokenize(String)
	 */
	public static List<NestedTokenizer.Token> tokenize(String fields) {
		return NestedTokenizer.tokenize(fields);
	}

	/**
	 * Safely converts an object to its string representation, handling any exceptions that may occur.
	 *
	 * <p>This method provides a fail-safe way to call {@code toString()} on any object, ensuring that
	 * exceptions thrown by problematic {@code toString()} implementations don't propagate up the call stack.
	 * Instead, it returns a descriptive error message containing the exception type and message.</p>
	 *
	 * <h5 class='section'>Exception Handling:</h5>
	 * <p>If the object's {@code toString()} method throws any {@code Throwable}, this method catches it
	 * and returns a formatted string in the form: {@code "<ExceptionType>: <exception message>"}.</p>
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 *    <jc>// Normal case - returns object's toString() result</jc>
	 *    String result = <jsm>safeToString</jsm>(<js>"Hello"</js>);
	 *    <jc>// result = "Hello"</jc>
	 *
	 *    <jc>// Exception case - returns formatted error message</jc>
	 *    Object problematic = <jk>new</jk> Object() {
	 *       <ja>@Override</ja>
	 *       <jk>public</jk> String toString() {
	 *          <jk>throw new</jk> RuntimeException(<js>"Cannot convert"</js>);
	 *       }
	 *    };
	 *    String result = <jsm>safeToString</jsm>(<jv>problematic</jv>);
	 *    <jc>// result = "RuntimeException: Cannot convert"</jc>
	 * </p>
	 *
	 * <h5 class='section'>Use Cases:</h5>
	 * <ul>
	 *    <li><b>Object stringification in converters:</b> Safe conversion of arbitrary objects to strings</li>
	 *    <li><b>Debugging and logging:</b> Ensures log statements never fail due to toString() exceptions</li>
	 *    <li><b>Error handling:</b> Graceful degradation when objects have problematic string representations</li>
	 *    <li><b>Third-party object integration:</b> Safe handling of objects from external libraries</li>
	 * </ul>
	 *
	 * @param o The object to convert to a string. May be any object including <jk>null</jk>.
	 * @return The string representation of the object, or a formatted error message if toString() throws an exception.
	 *    Returns <js>"null"</js> if the object is <jk>null</jk>.
	 */
	public static String safeToString(Object o) {
		try {
			return o.toString();
		} catch (Throwable t) {  // NOSONAR
			return t(t) + ": " + t.getMessage();
		}
	}
}
