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
package org.apache.juneau.assertions;

import static org.apache.juneau.internal.StringUtils.*;

import java.util.*;
import java.util.function.*;
import java.util.regex.*;
import java.util.stream.*;

import org.apache.juneau.internal.*;

/**
 * Used for fluent assertion calls against strings.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Validates the response body of an HTTP call is the text "OK".</jc>
 * 	<jv>client</jv>
 * 		.get(<jsf>URL</jsf>)
 * 		.run()
 * 		.assertBody().is(<js>"OK"</js>);
 * </p>
 *
 * @param <R> The return type.
 */
@FluentSetters(returns="FluentStringAssertion<R>")
public class FluentStringAssertion<R> extends FluentObjectAssertion<R> {

	private String text;
	private boolean javaStrings;

	/**
	 * Constructor.
	 *
	 * @param text The text being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentStringAssertion(String text, R returns) {
		this(null, text, returns);
	}

	/**
	 * Constructor.
	 *
	 * @param creator The assertion that created this assertion.
	 * @param text The text being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentStringAssertion(Assertion creator, String text, R returns) {
		super(creator, text, returns);
		this.text = text;
	}

	/**
	 * When enabled, text in the message is converted to valid Java strings.
	 *
	 * <p class='bcode w800'>
	 * 	value.replaceAll(<js>"\\\\"</js>, <js>"\\\\\\\\"</js>).replaceAll(<js>"\n"</js>, <js>"\\\\n"</js>).replaceAll(<js>"\t"</js>, <js>"\\\\t"</js>);
	 * </p>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public FluentStringAssertion<R> javaStrings() {
		this.javaStrings = true;
		return this;
	}

	/**
	 * Performs the specified regular expression replacement on the underlying string.
	 *
	 * @param regex The regular expression to which this string is to be matched.
	 * @param replacement The string to be substituted for each match.
	 * @return This object (for method chaining).
	 */
	public FluentStringAssertion<R> replaceAll(String regex, String replacement) {
		assertNotNull("regex", regex);
		assertNotNull("replacement", replacement);
		return apply(x -> x == null ? null : text.replaceAll(regex, replacement));
	}

	/**
	 * Performs the specified substring replacement on the underlying string.
	 *
	 * @param target The sequence of char values to be replaced.
	 * @param replacement The replacement sequence of char values.
	 * @return This object (for method chaining).
	 */
	public FluentStringAssertion<R> replace(String target, String replacement) {
		assertNotNull("target", target);
		assertNotNull("replacement", replacement);
		return apply(x -> x == null ? null : text.replace(target, replacement));
	}

	/**
	 * URL-decodes the text in this assertion.
	 *
	 * @return The response object (for method chaining).
	 */
	public FluentStringAssertion<R> urlDecode() {
		return apply(x->StringUtils.urlDecode(x));
	}

	/**
	 * Sorts the contents of the text by lines.
	 *
	 * @return The response object (for method chaining).
	 */
	public FluentStringAssertion<R> sort() {
		return apply(x->x == null ? null : Arrays.asList(x.trim().split("[\r\n]+")).stream().sorted().collect(Collectors.joining("\n")));
	}

	/**
	 * Converts the text to lowercase.
	 *
	 * @return The response object (for method chaining).
	 */
	public FluentStringAssertion<R> lc() {
		return apply(x->x == null ? null : x.toLowerCase());
	}

	/**
	 * Converts the text to uppercase.
	 *
	 * @return The response object (for method chaining).
	 */
	public FluentStringAssertion<R> uc() {
		return apply(x->x == null ? null : x.toUpperCase());
	}

	/**
	 * Applies an abitrary function against the text in this assertion.
	 *
	 * @param f The function to apply.
	 * @return The response object (for method chaining).
	 */
	public FluentStringAssertion<R> apply(Function<String,String> f) {
		return new FluentStringAssertion<>(this, f.apply(text), returns());
	}

	/**
	 * Asserts that the text equals the specified value.
	 *
	 * <p>
	 * Similar to {@link #is(String)} except error message states diff position.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the response body of an HTTP call is the text "OK".</jc>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.assertBody().isEquals(<js>"OK"</js>);
	 * </p>
	 *
	 * @param value
	 * 	The value to check against.
	 * 	<br>If multiple values are specified, they are concatenated with newlines.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isEqual(String value) throws AssertionError {
		if (! StringUtils.isEquals(value, text))
			throw error("Text differed at position {0}.\n\tExpected=[{1}]\n\tActual=[{2}]", diffPosition(value, text), fix(value), fix(text));
		return returns();
	}

	/**
	 * Asserts that the lines of text equals the specified value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the response body of an HTTP call is the text "OK".</jc>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.assertBody().isEqualLines(
	 * 			<js>"Line 1"</js>,
	 * 			<js>"Line 2"</js>,
	 * 			<js>"Line 3"</js>
	 * 		);
	 * </p>
	 *
	 * @param lines
	 * 	The value to check against.
	 * 	<br>If multiple values are specified, they are concatenated with newlines.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isEqualLines(String...lines) throws AssertionError {
		assertNotNull("lines", lines);
		String v = join(lines, '\n');
		if (! StringUtils.isEquals(v, text))
			throw error("Text differed at position {0}.\n\tExpected=[{1}]\n\tActual=[{2}]", diffPosition(v, text), fix(v), fix(text));
		return returns();
	}

	/**
	 * Asserts that the text equals the specified value after splitting both by newlines and sorting the rows.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the response body of an HTTP call is the text "OK".</jc>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.assertBody().isEqualSortedLines(
	 * 			<js>"Line 1"</js>,
	 * 			<js>"Line 2"</js>,
	 * 			<js>"Line 3"</js>
	 * 		);
	 * </p>
	 *
	 * @param lines
	 * 	The value to check against.
	 * 	<br>If multiple values are specified, they are concatenated with newlines.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isEqualSortedLines(String...lines) {
		assertNotNull("lines", lines);
		exists();

		// Must work for windows too.
		String[] e = StringUtils.join(lines, '\n').trim().split("[\r\n]+"), a = this.text.trim().split("[\r\n]+");

		if (e.length != a.length)
			throw error("Expected text had different numbers of lines.\n\tExpected=[{0}]\n\tActual=[{1}]", e.length, a.length);

		Arrays.sort(e);
		Arrays.sort(a);

		for (int i = 0; i < e.length; i++)
			if (! e[i].equals(a[i]))
				throw error("Expected text had different values at line {0}.\n\tExpected=[{1}]\n\tActual=[{2}]", i+1, e[i], a[i]);

		return returns();
	}

	/**
	 * Asserts that the text equals the specified value.
	 *
	 * <p>
	 * Similar to {@link #isEqual(String)} except error message doesn't state diff position.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the response body of an HTTP call is the text "OK".</jc>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.assertBody().is(<js>"OK"</js>);
	 * </p>
	 *
	 * @param value
	 * 	The value to check against.
	 * 	<br>If multiple values are specified, they are concatenated with newlines.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R is(String value) throws AssertionError {
		if (! StringUtils.isEquals(value, text))
			throw error("Unexpected value.\n\tExpected=[{0}]\n\tActual=[{1}]", fix(value), fix(text));
		return isEqual(value);
	}

	/**
	 * Asserts that the text equals the specified value ignoring case.
	 *
	 * @param value The value to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isEqualIc(String value) throws AssertionError {
		if (! StringUtils.isEqualsIc(value, text))
			throw error("Text differed at position {0}.\n\tExpected=[{1}]\n\tActual=[{2}]", diffPositionIc(value, text), fix(value), fix(text));
		return returns();
	}

	/**
	 * Asserts that the text equals the specified value.
	 *
	 * @param value The value to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R doesNotEqual(String value) throws AssertionError {
		if (StringUtils.isEquals(value, text))
			throw error("Text equaled unexpected.\n\tText=[{0}]", fix(text));
		return returns();
	}

	/**
	 * Asserts that the text equals the specified value.
	 *
	 * <p>
	 * Equivalent to {@link #doesNotEqual(String)}.
	 *
	 * @param value The value to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isNot(String value) throws AssertionError {
		return doesNotEqual(value);
	}

	/**
	 * Asserts that the text does not equal the specified value ignoring case.
	 *
	 * @param value The value to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R doesNotEqualIc(String value) throws AssertionError {
		if (StringUtils.isEqualsIc(value, text))
			throw error("Text equaled unexpected.\n\tText=[{0}]", fix(text));
		return returns();
	}

	/**
	 * Asserts that the text contains all of the specified substrings.
	 *
	 * @param values The values to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R contains(String...values) throws AssertionError {
		assertNotNull("values", values);
		for (String substring : values)
			if (substring != null && ! StringUtils.contains(text, substring))
				throw error("Text did not contain expected substring.\n\tSubstring=[{0}]\n\tText=[{1}]", fix(substring), fix(text));
		return returns();
	}

	/**
	 * Asserts that the text doesn't contain any of the specified substrings.
	 *
	 * @param values The values to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R doesNotContain(String...values) throws AssertionError {
		assertNotNull("values", values);
		for (String substring : values)
			if (substring != null && StringUtils.contains(text, substring))
				throw error("Text contained unexpected substring.\n\tSubstring=[{0}]\n\tText=[{1}]", fix(substring), fix(text));
		return returns();
	}

	/**
	 * Asserts that the text is not empty.
	 *
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isEmpty() throws AssertionError {
		if (text != null && ! text.isEmpty())
			throw error("Text was not empty.\n\tText=[{0}]", fix(text));
		return returns();
	}

	/**
	 * Asserts that the text is not null or empty.
	 *
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isNotEmpty() throws AssertionError {
		if (text == null)
			throw error("Text was null.");
		if (text.isEmpty())
			throw error("Text was empty.");
		return returns();
	}

	/**
	 * Asserts that the text matches the specified regular expression.
	 *
	 * @param regex The pattern to test for.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R matches(String regex) throws AssertionError {
		return matches(regex, 0);
	}

	/**
	 * Asserts that the text matches the specified pattern containing <js>"*"</js> meta characters.
	 *
	 * <p>
	 * The <js>"*"</js> meta character can be used to represent zero or more characters..
	 *
	 * @param searchPattern The search pattern.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R matchesSimple(String searchPattern) throws AssertionError {
		assertNotNull("searchPattern", searchPattern);
		return matches(getMatchPattern(searchPattern));
	}

	/**
	 * Asserts that the text doesn't match the specified regular expression.
	 *
	 * @param regex The pattern to test for.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R doesNotMatch(String regex) throws AssertionError {
		assertNotNull("regex", regex);
		return doesNotMatch(regex, 0);
	}

	/**
	 * Asserts that the text matches the specified regular expression.
	 *
	 * @param regex The pattern to test for.
	 * @param flags Pattern match flags.  See {@link Pattern#compile(String, int)}.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R matches(String regex, int flags) throws AssertionError {
		assertNotNull("regex", regex);
		exists();
		Pattern p = Pattern.compile(regex, flags);
		if (! p.matcher(text).matches())
			throw error("Text did not match expected pattern.\n\tPattern=[{0}]\n\tText=[{1}]", fix(regex), fix(text));
		return returns();
	}

	/**
	 * Asserts that the text doesn't match the specified regular expression.
	 *
	 * @param regex The pattern to test for.
	 * @param flags Pattern match flags.  See {@link Pattern#compile(String, int)}.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R doesNotMatch(String regex, int flags) throws AssertionError {
		assertNotNull("regex", regex);
		return doesNotMatch(Pattern.compile(regex, flags));
	}

	/**
	 * Asserts that the text matches the specified regular expression pattern.
	 *
	 * @param pattern The pattern to test for.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R matches(Pattern pattern) throws AssertionError {
		assertNotNull("pattern", pattern);
		exists();
		if (! pattern.matcher(text).matches())
			throw error("Text did not match expected pattern.\n\tPattern=[{0}]\n\tText=[{1}]", fix(pattern.pattern()), fix(text));
		return returns();
	}

	/**
	 * Asserts that the text doesn't match the specified regular expression pattern.
	 *
	 * @param pattern The pattern to test for.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R doesNotMatch(Pattern pattern) throws AssertionError {
		assertNotNull("pattern", pattern);
		if (text != null && pattern.matcher(text).matches())
			throw error("Text matched unexpected pattern.\n\tPattern=[{0}]\n\tText=[{1}]", fix(pattern.pattern()), fix(text));
		return returns();
	}

	/**
	 * Asserts that the text starts with the specified string.
	 *
	 * @param string The string to test for.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R startsWith(String string) {
		exists();
		assertNotNull("string", string);
		if (! text.startsWith(string))
			throw error("Text did not start with expected string.\n\tString=[{0}]\n\tText=[{1}]", fix(string), fix(text));
		return returns();
	}

	/**
	 * Asserts that the text ends with the specified string.
	 *
	 * @param string The string to test for.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R endsWith(String string) {
		exists();
		assertNotNull("string", string);
		if (! text.endsWith(string))
			throw error("Text did not end with expected string.\n\tString=[{0}]\n\tText=[{1}]", fix(string), fix(text));
		return returns();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Utility methods
	//------------------------------------------------------------------------------------------------------------------

	private String fix(String text) {
		if (javaStrings)
			text = text.replaceAll("\\\\", "\\\\\\\\").replaceAll("\n", "\\\\n").replaceAll("\t", "\\\\t");
		return text;
	}

	// <FluentSetters>

	@Override /* GENERATED - Assertion */
	public FluentStringAssertion<R> msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentStringAssertion<R> stderr() {
		super.stderr();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentStringAssertion<R> stdout() {
		super.stdout();
		return this;
	}

	// </FluentSetters>
}
