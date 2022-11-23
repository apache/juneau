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

import static org.apache.juneau.common.internal.ArgUtils.*;
import static org.apache.juneau.common.internal.StringUtils.*;

import java.io.*;
import java.util.*;
import java.util.function.*;
import java.util.regex.*;

import org.apache.juneau.common.internal.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;

/**
 * Used for fluent assertion calls against strings.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Validates the response body of an HTTP call is the text "OK".</jc>
 * 	<jv>client</jv>
 * 		.get(<jsf>URL</jsf>)
 * 		.run()
 * 		.assertContent().is(<js>"OK"</js>);
 * </p>
 *
 *
 * <h5 class='section'>Test Methods:</h5>
 * <p>
 * <ul class='javatree'>
 * 	<li class='jc'>{@link FluentStringAssertion}
 * 	<ul class='javatreec'>
 * 		<li class='jm'>{@link FluentStringAssertion#is(String) is(String)}
 * 		<li class='jm'>{@link FluentStringAssertion#isNot(String) isNot(String)}
 * 		<li class='jm'>{@link FluentStringAssertion#isLines(String...) isLines(String...)}
 * 		<li class='jm'>{@link FluentStringAssertion#isSortedLines(String...) isSortedLines(String...)}
 * 		<li class='jm'>{@link FluentStringAssertion#isIc(String) isIc(String)}
 * 		<li class='jm'>{@link FluentStringAssertion#isNotIc(String) isNotIc(String)}
 * 		<li class='jm'>{@link FluentStringAssertion#isContains(String...) isContains(String...)}
 * 		<li class='jm'>{@link FluentStringAssertion#isNotContains(String...) isNotContains(String...)}
 * 		<li class='jm'>{@link FluentStringAssertion#isEmpty() isEmpty()}
 * 		<li class='jm'>{@link FluentStringAssertion#isNotEmpty() isNotEmpty()}
 * 		<li class='jm'>{@link FluentStringAssertion#isString(Object) isString(Object)}
 * 		<li class='jm'>{@link FluentStringAssertion#isMatches(String) isMatches(String)}
 * 		<li class='jm'>{@link FluentStringAssertion#isPattern(String) isPattern(String)}
 * 		<li class='jm'>{@link FluentStringAssertion#isPattern(String,int) isPattern(String,int)}
 * 		<li class='jm'>{@link FluentStringAssertion#isPattern(Pattern) isPattern(Pattern)}
 * 		<li class='jm'>{@link FluentStringAssertion#isStartsWith(String) isStartsWith(String)}
 * 		<li class='jm'>{@link FluentStringAssertion#isEndsWith(String) isEndsWith(String)}
 * 	</ul>
 * 	<li class='jc'>{@link FluentObjectAssertion}
 * 	<ul class='javatreec'>
 * 		<li class='jm'>{@link FluentObjectAssertion#isExists() isExists()}
 * 		<li class='jm'>{@link FluentObjectAssertion#is(Object) is(Object)}
 * 		<li class='jm'>{@link FluentObjectAssertion#is(Predicate) is(Predicate)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isNot(Object) isNot(Object)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isAny(Object...) isAny(Object...)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isNotAny(Object...) isNotAny(Object...)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isNull() isNull()}
 * 		<li class='jm'>{@link FluentObjectAssertion#isNotNull() isNotNull()}
 * 		<li class='jm'>{@link FluentObjectAssertion#isString(String) isString(String)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isJson(String) isJson(String)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isSame(Object) isSame(Object)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isSameJsonAs(Object) isSameJsonAs(Object)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isSameSortedJsonAs(Object) isSameSortedJsonAs(Object)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isSameSerializedAs(Object, WriterSerializer) isSameSerializedAs(Object, WriterSerializer)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isType(Class) isType(Class)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isExactType(Class) isExactType(Class)}
 * 	</ul>
 * </ul>
 *
 * <h5 class='section'>Transform Methods:</h5>
 * <p>
 * <ul class='javatree'>
 * 	<li class='jc'>{@link FluentStringAssertion}
 * 	<ul class='javatreec'>
 * 		<li class='jm'>{@link FluentStringAssertion#asReplaceAll(String,String) asReplaceAll(String,String)}
 * 		<li class='jm'>{@link FluentStringAssertion#asReplace(String,String) asReplace(String,String)}
 * 		<li class='jm'>{@link FluentStringAssertion#asUrlDecode() asUrlDecode()}
 * 		<li class='jm'>{@link FluentStringAssertion#asLc() asLc()}
 * 		<li class='jm'>{@link FluentStringAssertion#asUc() asUc()}
 * 		<li class='jm'>{@link FluentStringAssertion#asLines() asLines()}
 * 		<li class='jm'>{@link FluentStringAssertion#asSplit(String) asSplit(String)}
 * 		<li class='jm'>{@link FluentStringAssertion#asLength() asLength()}
 * 		<li class='jm'>{@link FluentStringAssertion#asOneLine() asOneLine()}
  * 	</ul>
 * 	<li class='jc'>{@link FluentObjectAssertion}
 * 	<ul class='javatreec'>
 * 		<li class='jm'>{@link FluentObjectAssertion#asString() asString()}
 * 		<li class='jm'>{@link FluentObjectAssertion#asString(WriterSerializer) asString(WriterSerializer)}
 * 		<li class='jm'>{@link FluentObjectAssertion#asString(Function) asString(Function)}
 * 		<li class='jm'>{@link FluentObjectAssertion#asJson() asJson()}
 * 		<li class='jm'>{@link FluentObjectAssertion#asJsonSorted() asJsonSorted()}
 * 		<li class='jm'>{@link FluentObjectAssertion#asTransformed(Function) asApplied(Function)}
 * 		<li class='jm'>{@link FluentObjectAssertion#asAny() asAny()}
 *	</ul>
 * </ul>
 *
 * <h5 class='section'>Configuration Methods:</h5>
 * <p>
 * <ul class='javatree'>
 * 	<li class='jc'>{@link Assertion}
 * 	<ul class='javatreec'>
 * 		<li class='jm'>{@link Assertion#setMsg(String, Object...) setMsg(String, Object...)}
 * 		<li class='jm'>{@link Assertion#setOut(PrintStream) setOut(PrintStream)}
 * 		<li class='jm'>{@link Assertion#setSilent() setSilent()}
 * 		<li class='jm'>{@link Assertion#setStdOut() setStdOut()}
 * 		<li class='jm'>{@link Assertion#setThrowable(Class) setThrowable(Class)}
 * 	</ul>
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#ja.Overview">Overview &gt; juneau-assertions &gt; Overview</a>
 * </ul>
 *
 * @param <R> The return type.
 */
@FluentSetters(returns="FluentStringAssertion<R>")
public class FluentStringAssertion<R> extends FluentObjectAssertion<String,R> {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final Messages MESSAGES = Messages.of(FluentStringAssertion.class, "Messages");
	private static final String
		MSG_stringDifferedAtPosition = MESSAGES.getString("stringDifferedAtPosition"),
		MSG_expectedStringHadDifferentNumbersOfLines = MESSAGES.getString("expectedStringHadDifferentNumbersOfLines"),
		MSG_expectedStringHadDifferentValuesAtLine = MESSAGES.getString("expectedStringHadDifferentValuesAtLine"),
		MSG_stringEqualedUnexpected = MESSAGES.getString("stringEqualedUnexpected"),
		MSG_stringDidNotContainExpectedSubstring = MESSAGES.getString("stringDidNotContainExpectedSubstring"),
		MSG_stringContainedUnexpectedSubstring = MESSAGES.getString("stringContainedUnexpectedSubstring"),
		MSG_stringWasNotEmpty = MESSAGES.getString("stringWasNotEmpty"),
		MSG_stringWasNull = MESSAGES.getString("stringWasNull"),
		MSG_stringWasEmpty = MESSAGES.getString("stringWasEmpty"),
		MSG_stringDidNotMatchExpectedPattern = MESSAGES.getString("stringDidNotMatchExpectedPattern"),
		MSG_stringDidNotStartWithExpected = MESSAGES.getString("stringDidNotStartWithExpected"),
		MSG_stringDidNotEndWithExpected = MESSAGES.getString("stringDidNotEndWithExpected");

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private boolean javaStrings;

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The object being tested.
	 * 	<br>Can be <jk>null</jk>.
	 * @param returns
	 * 	The object to return after a test method is called.
	 * 	<br>If <jk>null</jk>, the test method returns this object allowing multiple test method calls to be
	 * used on the same assertion.
	 */
	public FluentStringAssertion(String value, R returns) {
		this(null, value, returns);
	}

	/**
	 * Chained constructor.
	 *
	 * <p>
	 * Used when transforming one assertion into another so that the assertion config can be used by the new assertion.
	 *
	 * @param creator
	 * 	The assertion that created this assertion.
	 * 	<br>Should be <jk>null</jk> if this is the top-level assertion.
	 * @param value
	 * 	The object being tested.
	 * 	<br>Can be <jk>null</jk>.
	 * @param returns
	 * 	The object to return after a test method is called.
	 * 	<br>If <jk>null</jk>, the test method returns this object allowing multiple test method calls to be
	 * used on the same assertion.
	 */
	public FluentStringAssertion(Assertion creator, String value, R returns) {
		super(creator, value, returns);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Config methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * When enabled, text in the message is converted to valid Java strings.
	 *
	 * <p class='bjava'>
	 * 	<jv>value</jv>.replaceAll(<js>"\\\\"</js>, <js>"\\\\\\\\"</js>).replaceAll(<js>"\n"</js>, <js>"\\\\n"</js>).replaceAll(<js>"\t"</js>, <js>"\\\\t"</js>);
	 * </p>
	 *
	 * @return This object.
	 */
	@FluentSetter
	public FluentStringAssertion<R> asJavaStrings() {
		this.javaStrings = true;
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Transform methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* FluentObjectAssertion */
	public FluentStringAssertion<R> asTransformed(Function<String,String> function) {
		return new FluentStringAssertion<>(this, function.apply(orElse(null)), returns());
	}

	/**
	 * Performs the specified regular expression replacement on the underlying string.
	 *
	 * @param regex The regular expression to which this string is to be matched.
	 * @param replacement The string to be substituted for each match.
	 * @return This object.
	 */
	public FluentStringAssertion<R> asReplaceAll(String regex, String replacement) {
		assertArgNotNull("regex", regex);
		assertArgNotNull("replacement", replacement);
		return asTransformed(x -> x == null ? null : x.replaceAll(regex, replacement));
	}

	/**
	 * Performs the specified substring replacement on the underlying string.
	 *
	 * @param target The sequence of char values to be replaced.
	 * @param replacement The replacement sequence of char values.
	 * @return This object.
	 */
	public FluentStringAssertion<R> asReplace(String target, String replacement) {
		assertArgNotNull("target", target);
		assertArgNotNull("replacement", replacement);
		return asTransformed(x -> x == null ? null : x.replace(target, replacement));
	}

	/**
	 * URL-decodes the text in this assertion.
	 *
	 * @return This object.
	 */
	public FluentStringAssertion<R> asUrlDecode() {
		return asTransformed(StringUtils::urlDecode);
	}

	/**
	 * Converts the text to lowercase.
	 *
	 * @return This object.
	 */
	public FluentStringAssertion<R> asLc() {
		return asTransformed(x->x == null ? null : x.toLowerCase());
	}

	/**
	 * Converts the text to uppercase.
	 *
	 * @return This object.
	 */
	public FluentStringAssertion<R> asUc() {
		return asTransformed(x->x == null ? null : x.toUpperCase());
	}

	/**
	 * Splits the string into lines.
	 *
	 * @return This object.
	 */
	public FluentListAssertion<String,R> asLines() {
		return asSplit("[\r\n]+");
	}

	/**
	 * Splits the string into lines using the specified regular expression.
	 *
	 * @param regex The delimiting regular expression
	 * @return This object.
	 */
	public FluentListAssertion<String,R> asSplit(String regex) {
		assertArgNotNull("regex", regex);
		return new FluentListAssertion<>(this, valueIsNull() ? null : Arrays.asList(value().trim().split(regex)), returns());
	}

	/**
	 * Returns the length of this string as an integer assertion.
	 *
	 * @return This object.
	 */
	public FluentIntegerAssertion<R> asLength() {
		return new FluentIntegerAssertion<>(this, valueIsNull() ? null : value().length(), returns());
	}

	/**
	 * Removes any newlines from the string.
	 *
	 * @return This object.
	 */
	public FluentStringAssertion<R> asOneLine() {
		return asTransformed(x->x == null ? null : x.replaceAll("\\s*[\r\n]+\\s*","  "));
	}

	/**
	 * Removes any leading/trailing whitespace from the string.
	 *
	 * @return This object.
	 */
	public FluentStringAssertion<R> asTrimmed() {
		return new FluentStringAssertion<>(this, valueIsNull() ? null : value().trim(), returns());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Asserts that the text equals the specified value.
	 *
	 * <p>
	 * Similar to {@link #is(String)} except error message states diff position.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Validates the response body of an HTTP call is the text "OK".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.assertContent().is(<js>"OK"</js>);
	 * </p>
	 *
	 * @param value
	 * 	The value to check against.
	 * 	<br>If multiple values are specified, they are concatenated with newlines.
	 * @return The fluent return object.
	 * @throws AssertionError If assertion failed.
	 */
	@Override
	public R is(String value) throws AssertionError {
		String s = orElse(null);
		if (ne(value, s))
			throw error(MSG_stringDifferedAtPosition, diffPosition(value, s), fix(value), fix(s));
		return returns();
	}

	/**
	 * Asserts that the text equals the specified value.
	 *
	 * @param value The value to check against.
	 * @return The fluent return object.
	 * @throws AssertionError If assertion failed.
	 */
	@Override
	public R isNot(String value) throws AssertionError {
		String s = orElse(null);
		if (eq(value, s))
			throw error(MSG_stringEqualedUnexpected, fix(s));
		return returns();
	}

	/**
	 * Asserts that the lines of text equals the specified value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Validates the response body of an HTTP call is the text "OK".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.assertContent().isLines(
	 * 			<js>"Line 1"</js>,
	 * 			<js>"Line 2"</js>,
	 * 			<js>"Line 3"</js>
	 * 		);
	 * </p>
	 *
	 * @param lines
	 * 	The value to check against.
	 * 	<br>If multiple values are specified, they are concatenated with newlines.
	 * @return The fluent return object.
	 * @throws AssertionError If assertion failed.
	 */
	public R isLines(String...lines) throws AssertionError {
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
	 * <p class='bjava'>
	 * 	<jc>// Validates the response body of an HTTP call is the text "OK".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.assertContent().isSortedLines(
	 * 			<js>"Line 1"</js>,
	 * 			<js>"Line 2"</js>,
	 * 			<js>"Line 3"</js>
	 * 		);
	 * </p>
	 *
	 * @param lines
	 * 	The value to check against.
	 * 	<br>If multiple values are specified, they are concatenated with newlines.
	 * @return The fluent return object.
	 * @throws AssertionError If assertion failed.
	 */
	public R isSortedLines(String...lines) {
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
	 * Asserts that the text equals the specified value ignoring case.
	 *
	 * @param value The value to check against.
	 * @return The fluent return object.
	 * @throws AssertionError If assertion failed.
	 */
	public R isIc(String value) throws AssertionError {
		String s = orElse(null);
		if (neic(value, s))
			throw error(MSG_stringDifferedAtPosition, diffPositionIc(value, s), fix(value), fix(s));
		return returns();
	}

	/**
	 * Asserts that the text does not equal the specified value ignoring case.
	 *
	 * @param value The value to check against.
	 * @return The fluent return object.
	 * @throws AssertionError If assertion failed.
	 */
	public R isNotIc(String value) throws AssertionError {
		String s = orElse(null);
		if (eqic(value, s))
			throw error(MSG_stringEqualedUnexpected, fix(s));
		return returns();
	}

	/**
	 * Asserts that the text contains all of the specified substrings.
	 *
	 * @param values The values to check against.
	 * @return The fluent return object.
	 * @throws AssertionError If assertion failed.
	 */
	public R isContains(String...values) throws AssertionError {
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
	 * @return The fluent return object.
	 * @throws AssertionError If assertion failed.
	 */
	public R isNotContains(String...values) throws AssertionError {
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
	 * @return The fluent return object.
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
	 * @return The fluent return object.
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
	 * Asserts that the text matches the specified pattern containing <js>"*"</js> meta characters.
	 *
	 * <p>
	 * The <js>"*"</js> meta character can be used to represent zero or more characters..
	 *
	 * @param searchPattern The search pattern.
	 * @return The fluent return object.
	 * @throws AssertionError If assertion failed.
	 */
	public R isMatches(String searchPattern) throws AssertionError {
		assertArgNotNull("searchPattern", searchPattern);
		return isPattern(getMatchPattern(searchPattern));
	}

	/**
	 * Asserts that the text matches the specified regular expression.
	 *
	 * @param regex The pattern to test for.
	 * @return The fluent return object.
	 * @throws AssertionError If assertion failed.
	 */
	public R isPattern(String regex) throws AssertionError {
		return isPattern(regex, 0);
	}

	/**
	 * Asserts that the text matches the specified regular expression.
	 *
	 * @param regex The pattern to test for.
	 * @param flags Pattern match flags.  See {@link Pattern#compile(String, int)}.
	 * @return The fluent return object.
	 * @throws AssertionError If assertion failed.
	 */
	public R isPattern(String regex, int flags) throws AssertionError {
		assertArgNotNull("regex", regex);
		Pattern p = Pattern.compile(regex, flags);
		String s = value();
		if (! p.matcher(s).matches())
			throw error(MSG_stringDidNotMatchExpectedPattern, fix(regex), fix(s));
		return returns();
	}

	/**
	 * Asserts that the text matches the specified regular expression pattern.
	 *
	 * @param pattern The pattern to test for.
	 * @return The fluent return object.
	 * @throws AssertionError If assertion failed.
	 */
	public R isPattern(Pattern pattern) throws AssertionError {
		assertArgNotNull("pattern", pattern);
		String s = value();
		if (! pattern.matcher(s).matches())
			throw error(MSG_stringDidNotMatchExpectedPattern, fix(pattern.pattern()), fix(s));
		return returns();
	}

	/**
	 * Asserts that the text starts with the specified string.
	 *
	 * @param string The string to test for.
	 * @return The fluent return object.
	 * @throws AssertionError If assertion failed.
	 */
	public R isStartsWith(String string) {
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
	 * @return The fluent return object.
	 * @throws AssertionError If assertion failed.
	 */
	public R isEndsWith(String string) {
		assertArgNotNull("string", string);
		String s = value();
		if (! s.endsWith(string))
			throw error(MSG_stringDidNotEndWithExpected, fix(string), fix(s));
		return returns();
	}

	/**
	 * Asserts that the text equals the specified object after calling {@link #toString()} on the object.
	 *
	 * @param value The value to check against.
	 * @return The fluent return object.
	 */
	public R isString(Object value) {
		return is(value == null ? null : toString());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Fluent setters
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentStringAssertion<R> setMsg(String msg, Object...args) {
		super.setMsg(msg, args);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentStringAssertion<R> setOut(PrintStream value) {
		super.setOut(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentStringAssertion<R> setSilent() {
		super.setSilent();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentStringAssertion<R> setStdOut() {
		super.setStdOut();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentStringAssertion<R> setThrowable(Class<? extends java.lang.RuntimeException> value) {
		super.setThrowable(value);
		return this;
	}

	// </FluentSetters>

	//------------------------------------------------------------------------------------------------------------------
	// Utility methods
	//------------------------------------------------------------------------------------------------------------------

	private String fix(String text) {
		if (javaStrings)
			text = text.replaceAll("\\\\", "\\\\\\\\").replaceAll("\n", "\\\\n").replaceAll("\t", "\\\\t");
		return text;
	}
}
