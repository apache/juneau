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


import java.util.function.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;

/**
 * Used for fluent assertion calls.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Validates the response body of an HTTP call is the text "OK".</jc>
 * 	client
 * 		.get(<jsf>URL</jsf>)
 * 		.run()
 * 		.assertBody().equals(<js>"OK"</js>);
 * </p>
 * @param <R> The return type.
 */
public class FluentStringAssertion<R> {

	private final String text;
	private final R returns;

	/**
	 * Constructor.
	 *
	 * @param text The text being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentStringAssertion(String text, R returns) {
		this.text = text;
		this.returns = returns;
	}

	/**
	 * Asserts that the text equals the specified value.
	 *
	 * @param value The value to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R equals(String value) throws AssertionError {
		if (! StringUtils.isEquals(value, text)) {
			if (value != null && value.startsWith("x")) {
				StringBuilder sb = new StringBuilder();
				sb.append("Text did not equal expected.");
				sb.append("\nExpected: [").append(value.replaceAll("\\\\", "\\\\\\\\").replaceAll("\n", "\\\\n").replaceAll("\t", "\\\\t")).append("]");
				sb.append("\nActual  : [").append(text.replaceAll("\\\\", "\\\\\\\\").replaceAll("\n", "\\\\n").replaceAll("\t", "\\\\t")).append("]");
				System.err.println(sb.toString());
			}
			throw new BasicAssertionError("Text did not equal expected.\n\tExpected=[{0}]\n\tActual=[{1}]", value, text);
		}
		return returns;
	}

	/**
	 * Asserts that the text equals the specified value.
	 *
	 * <p>
	 * Equivalent to {@link #equals(String)}.
	 *
	 * @param value The value to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R is(String value) throws AssertionError {
		return equals(value);
	}

	/**
	 * Asserts that the text equals the specified value ignoring case.
	 *
	 * @param value The value to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R equalsIc(String value) throws AssertionError {
		if (! StringUtils.isEqualsIc(value, text)) {
			if (value != null && value.startsWith("x")) {
				StringBuilder sb = new StringBuilder();
				sb.append("Text did not equal expected.");
				sb.append("\nExpected: [").append(value.replaceAll("\\\\", "\\\\\\\\").replaceAll("\n", "\\\\n").replaceAll("\t", "\\\\t")).append("]");
				sb.append("\nActual  : [").append(text.replaceAll("\\\\", "\\\\\\\\").replaceAll("\n", "\\\\n").replaceAll("\t", "\\\\t")).append("]");
				System.err.println(sb.toString());
			}
			throw new BasicAssertionError("Text did not equal expected.\n\tExpected=[{0}]\n\tActual=[{1}]", value, text);
		}
		return returns;
	}

	/**
	 * Asserts that the text equals the specified value.
	 *
	 * @param value The value to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R doesNotEqual(String value) throws AssertionError {
		if (StringUtils.isEquals(value, text)) {
			if (value != null && value.startsWith("x")) {
				StringBuilder sb = new StringBuilder();
				sb.append("Text equaled unexpected.");
				sb.append("\nText: [").append(value.replaceAll("\\\\", "\\\\\\\\").replaceAll("\n", "\\\\n").replaceAll("\t", "\\\\t")).append("]");
				System.err.println(sb.toString());
			}
			throw new BasicAssertionError("Text equaled unexpected.\n\tText=[{1}]", value, text);
		}
		return returns;
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
		if (StringUtils.isEqualsIc(value, text)) {
			if (value != null && value.startsWith("x")) {
				StringBuilder sb = new StringBuilder();
				sb.append("Text equaled unexpected.");
				sb.append("\nText: [").append(value.replaceAll("\\\\", "\\\\\\\\").replaceAll("\n", "\\\\n").replaceAll("\t", "\\\\t")).append("]");
				System.err.println(sb.toString());
			}
			throw new BasicAssertionError("Text equaled unexpected.\n\tText=[{1}]", value, text);
		}
		return returns;
	}

	/**
	 * Asserts that the text contains all of the specified substrings.
	 *
	 * @param values The values to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R contains(String...values) throws AssertionError {
		for (String substring : values)
			if (! StringUtils.contains(text, substring)) {
				if (substring.startsWith("x")) {
					StringBuilder sb = new StringBuilder();
					sb.append("Text did not contain expected substring.");
					sb.append("\nSubstring: [").append(substring.replaceAll("\\\\", "\\\\\\\\").replaceAll("\n", "\\\\n").replaceAll("\t", "\\\\t")).append("]");
					sb.append("\nText     : [").append(text.replaceAll("\\\\", "\\\\\\\\").replaceAll("\n", "\\\\n").replaceAll("\t", "\\\\t")).append("]");
					System.err.println(sb.toString());
				}
				throw new BasicAssertionError("Text did not contain expected substring.\n\tExpected=[{0}]\n\tActual=[{1}]", substring, text);
			}
		return returns;
	}

	/**
	 * Asserts that the text doesn't contain any of the specified substrings.
	 *
	 * @param values The values to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R doesNotContain(String...values) throws AssertionError {
		for (String substring : values)
			if (StringUtils.contains(text, substring)) {
				if (substring.startsWith("x")) {
					StringBuilder sb = new StringBuilder();
					sb.append("Text contained unexpected substring.");
					sb.append("\nSubstring: [").append(substring.replaceAll("\\\\", "\\\\\\\\").replaceAll("\n", "\\\\n").replaceAll("\t", "\\\\t")).append("]");
					sb.append("\nText     : [").append(text.replaceAll("\\\\", "\\\\\\\\").replaceAll("\n", "\\\\n").replaceAll("\t", "\\\\t")).append("]");
					System.err.println(sb.toString());
				}
				throw new BasicAssertionError("Text contained unexpected substring.\n\tExpected=[{0}]\n\tActual=[{1}]", substring, text);
			}
		return returns;
	}

	/**
	 * Asserts that the text is not null.
	 *
	 * <p>
	 * Equivalent to {@link #isNotNull()}.
	 *
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R exists() throws AssertionError {
		return isNotNull();
	}

	/**
	 * Asserts that the text is not null.
	 *
	 * <p>
	 * Equivalent to {@link #isNull()}.
	 *
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R doesNotExist() throws AssertionError {
		return isNull();
	}

	/**
	 * Asserts that the text is not null.
	 *
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isNull() throws AssertionError {
		if (text != null)
			throw new BasicAssertionError("Text was not null.");
		return returns;
	}

	/**
	 * Asserts that the text is not null.
	 *
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isNotNull() throws AssertionError {
		if (text == null)
			throw new BasicAssertionError("Text was null.");
		return returns;
	}

	/**
	 * Asserts that the text is not empty.
	 *
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isEmpty() throws AssertionError {
		if (! text.isEmpty())
			throw new BasicAssertionError("Text was not empty.");
		return returns;
	}

	/**
	 * Asserts that the text is not null or empty.
	 *
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isNotEmpty() throws AssertionError {
		if (text == null)
			throw new BasicAssertionError("Text was null.");
		if (text.isEmpty())
			throw new BasicAssertionError("Text was empty.");
		return returns;
	}

	/**
	 * Asserts that the text passes the specified predicate test.
	 *
	 * @param test The predicate to use to test the value.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R passes(Predicate<String> test) throws AssertionError {
		if (! test.test(text))
			throw new BasicAssertionError("Text did not pass predicate test.\n\tText=[{0}]", text);
		return returns;
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
	 * Asserts that the text doesn't match the specified regular expression.
	 *
	 * @param regex The pattern to test for.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R doesNotMatch(String regex) throws AssertionError {
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
		Pattern p = Pattern.compile(regex, flags);
		if (! p.matcher(text).matches())
			throw new BasicAssertionError("Text did not match expected pattern.\n\tPattern=[{0}]\n\tText=[{1}]", regex, text);
		return returns;
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
		Pattern p = Pattern.compile(regex, flags);
		if (p.matcher(text).matches())
			throw new BasicAssertionError("Text matched unexpected pattern.\n\tPattern=[{0}]\n\tText=[{1}]", regex, text);
		return returns;
	}

	/**
	 * Asserts that the text matches the specified regular expression pattern.
	 *
	 * @param pattern The pattern to test for.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R matches(Pattern pattern) throws AssertionError {
		if (! pattern.matcher(text).matches())
			throw new BasicAssertionError("Text did not match expected pattern.\n\tPattern=[{0}]\n\tText=[{1}]", pattern.pattern(), text);
		return returns;
	}

	/**
	 * Asserts that the text doesn't match the specified regular expression pattern.
	 *
	 * @param pattern The pattern to test for.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R doesNotMatch(Pattern pattern) throws AssertionError {
		if (pattern.matcher(text).matches())
			throw new BasicAssertionError("Text matched unexpected pattern.\n\tPattern=[{0}]\n\tText=[{1}]", pattern.pattern(), text);
		return returns;
	}
}
