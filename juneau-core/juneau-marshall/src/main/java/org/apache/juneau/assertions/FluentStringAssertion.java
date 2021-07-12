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

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.internal.StringUtils.*;
import static java.util.stream.Collectors.*;
import static java.util.Arrays.*;

import java.io.*;
import java.util.*;
import java.util.function.*;
import java.util.regex.*;

import org.apache.juneau.cp.*;
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
public class FluentStringAssertion<R> extends FluentObjectAssertion<String,R> {

	private static final Messages MESSAGES = Messages.of(FluentStringAssertion.class, "Messages");
	static final String
		MSG_stringDifferedAtPosition = MESSAGES.getString("stringDifferedAtPosition"),
		MSG_expectedStringHadDifferentNumbersOfLines = MESSAGES.getString("expectedStringHadDifferentNumbersOfLines"),
		MSG_expectedStringHadDifferentValuesAtLine = MESSAGES.getString("expectedStringHadDifferentValuesAtLine"),
		MSG_unexpectedValue = MESSAGES.getString("unexpectedValue"),
		MSG_stringEqualedUnexpected = MESSAGES.getString("stringEqualedUnexpected"),
		MSG_stringDidNotContainExpectedSubstring = MESSAGES.getString("stringDidNotContainExpectedSubstring"),
		MSG_stringContainedUnexpectedSubstring = MESSAGES.getString("stringContainedUnexpectedSubstring"),
		MSG_stringWasNotEmpty = MESSAGES.getString("stringWasNotEmpty"),
		MSG_stringWasNull = MESSAGES.getString("stringWasNull"),
		MSG_stringWasEmpty = MESSAGES.getString("stringWasEmpty"),
		MSG_stringDidNotMatchExpectedPattern = MESSAGES.getString("stringDidNotMatchExpectedPattern"),
		MSG_stringMatchedUnexpectedPattern = MESSAGES.getString("stringMatchedUnexpectedPattern"),
		MSG_stringDidNotStartWithExpected = MESSAGES.getString("stringDidNotStartWithExpected"),
		MSG_stringDidNotEndWithExpected = MESSAGES.getString("stringDidNotEndWithExpected");

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
		assertArgNotNull("regex", regex);
		assertArgNotNull("replacement", replacement);
		return apply(x -> x == null ? null : x.replaceAll(regex, replacement));
	}

	/**
	 * Performs the specified substring replacement on the underlying string.
	 *
	 * @param target The sequence of char values to be replaced.
	 * @param replacement The replacement sequence of char values.
	 * @return This object (for method chaining).
	 */
	public FluentStringAssertion<R> replace(String target, String replacement) {
		assertArgNotNull("target", target);
		assertArgNotNull("replacement", replacement);
		return apply(x -> x == null ? null : x.replace(target, replacement));
	}

	/**
	 * URL-decodes the text in this assertion.
	 *
	 * @return The response object (for method chaining).
	 */
	public FluentStringAssertion<R> urlDecode() {
		return apply(StringUtils::urlDecode);
	}

	/**
	 * Sorts the contents of the text by lines.
	 *
	 * @return The response object (for method chaining).
	 */
	public FluentStringAssertion<R> sort() {
		return apply(x->x == null ? null : stream(x.trim().split("[\r\n]+")).sorted().collect(joining("\n")));
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
		return new FluentStringAssertion<>(this, f.apply(orElse(null)), returns());
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
	 * 		.assertBody().isEqualTo(<js>"OK"</js>);
	 * </p>
	 *
	 * @param value
	 * 	The value to check against.
	 * 	<br>If multiple values are specified, they are concatenated with newlines.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isEqualTo(String value) throws AssertionError {
		String s = orElse(null);
		if (ne(value, s))
			throw error(MSG_stringDifferedAtPosition, diffPosition(value, s), fix(value), fix(s));
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
	 * 		.assertBody().isEqualLinesTo(
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
	public R isEqualLinesTo(String...lines) throws AssertionError {
		assertArgNotNull("lines", lines);
		String v = join(lines, '\n');
		String s = value();
		if (ne(v, s))
			throw error(MSG_stringDifferedAtPosition, diffPosition(v, s), fix(v), fix(s));
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
	 * 		.assertBody().isEqualSortedLinesTo(
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
	public R isEqualSortedLinesTo(String...lines) {
		assertArgNotNull("lines", lines);

		// Must work for windows too.
		String[] e = StringUtils.join(lines, '\n').trim().split("[\r\n]+"), a = value().trim().split("[\r\n]+");

		if (e.length != a.length)
			throw error(MSG_expectedStringHadDifferentNumbersOfLines, e.length, a.length);

		Arrays.sort(e);
		Arrays.sort(a);

		for (int i = 0; i < e.length; i++)
			if (! e[i].equals(a[i]))
				throw error(MSG_expectedStringHadDifferentValuesAtLine, i+1, e[i], a[i]);

		return returns();
	}

	/**
	 * Asserts that the text equals the specified value.
	 *
	 * <p>
	 * Similar to {@link #isEqualTo(String)} except error message doesn't state diff position.
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
		String s = orElse(null);
		if (ne(value, s))
			throw error(MSG_unexpectedValue, fix(value), fix(s));
		return isEqualTo(value);
	}

	/**
	 * Asserts that the text equals the specified value ignoring case.
	 *
	 * @param value The value to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isEqualIgnoreCaseTo(String value) throws AssertionError {
		String s = orElse(null);
		if (neic(value, s))
			throw error(MSG_stringDifferedAtPosition, diffPositionIc(value, s), fix(value), fix(s));
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
		String s = orElse(null);
		if (eq(value, s))
			throw error(MSG_stringEqualedUnexpected, fix(s));
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
		String s = orElse(null);
		if (eqic(value, s))
			throw error(MSG_stringEqualedUnexpected, fix(s));
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
		assertArgNotNull("values", values);
		String s = orElse(null);
		for (String substring : values)
			if (substring != null && ! StringUtils.contains(s, substring))
				throw error(MSG_stringDidNotContainExpectedSubstring, fix(substring), fix(s));
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
		assertArgNotNull("values", values);
		String s = orElse(null);
		for (String substring : values)
			if (substring != null && StringUtils.contains(s, substring))
				throw error(MSG_stringContainedUnexpectedSubstring, fix(substring), fix(s));
		return returns();
	}

	/**
	 * Asserts that the text is empty.
	 *
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isEmpty() throws AssertionError {
		String s = orElse(null);
		if (s != null && ! s.isEmpty())
			throw error(MSG_stringWasNotEmpty, fix(s));
		return returns();
	}

	/**
	 * Asserts that the text is not null or empty.
	 *
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isNotEmpty() throws AssertionError {
		String s = orElse(null);
		if (s == null)
			throw error(MSG_stringWasNull);
		if (s.isEmpty())
			throw error(MSG_stringWasEmpty);
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
		assertArgNotNull("searchPattern", searchPattern);
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
		assertArgNotNull("regex", regex);
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
		assertArgNotNull("regex", regex);
		Pattern p = Pattern.compile(regex, flags);
		String s = value();
		if (! p.matcher(s).matches())
			throw error(MSG_stringDidNotMatchExpectedPattern, fix(regex), fix(s));
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
		assertArgNotNull("regex", regex);
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
		assertArgNotNull("pattern", pattern);
		String s = value();
		if (! pattern.matcher(s).matches())
			throw error(MSG_stringDidNotMatchExpectedPattern, fix(pattern.pattern()), fix(s));
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
		assertArgNotNull("pattern", pattern);
		String s = orElse(null);
		if (s != null && pattern.matcher(s).matches())
			throw error(MSG_stringMatchedUnexpectedPattern, fix(pattern.pattern()), fix(s));
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
		assertArgNotNull("string", string);
		String s = value();
		if (! s.startsWith(string))
			throw error(MSG_stringDidNotStartWithExpected, fix(string), fix(s));
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
		assertArgNotNull("string", string);
		String s = value();
		if (! s.endsWith(string))
			throw error(MSG_stringDidNotEndWithExpected, fix(string), fix(s));
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
	public FluentStringAssertion<R> out(PrintStream value) {
		super.out(value);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentStringAssertion<R> silent() {
		super.silent();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentStringAssertion<R> stdout() {
		super.stdout();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentStringAssertion<R> throwable(Class<? extends java.lang.RuntimeException> value) {
		super.throwable(value);
		return this;
	}

	// </FluentSetters>
}
