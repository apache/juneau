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
package org.apache.juneau.http;

import java.util.function.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;

/**
 * Superclass of all headers defined in this package.
 */
@BeanIgnore
public class BasicHeader extends org.apache.http.message.BasicHeader {
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 *
	 * @param name Header name.
	 * @param value Header value.
	 */
	public BasicHeader(String name, String value) {
		super(name, value);
	}

	/**
	 * Returns this header as a simple string value.
	 *
	 * <p>
	 * Functionally equivalent to calling {@link #toString()}.
	 *
	 * @return This header as a simple string.
	 */
	public String asString() {
		return getValue();
	}


	/**
	 * Returns <jk>true</jk> if the specified value is the same using {@link String#equalsIgnoreCase(String)}.
	 *
	 * @param compare The value to compare against.
	 * @return <jk>true</jk> if the specified value is the same.
	 */
	public boolean eqIC(String compare) {
		return asString().equalsIgnoreCase(compare);
	}

	/**
	 * Returns <jk>true</jk> if the specified value is the same using {@link String#equals(Object)}.
	 *
	 * @param compare The value to compare against.
	 * @return <jk>true</jk> if the specified value is the same.
	 */
	public boolean eq(String compare) {
		return asString().equals(compare);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Assertions.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Asserts that the header value is not <jk>null</jk>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the content type header is provided.</jc>
	 * 	Accept accept = ...;
	 * 	accept.assertExists();
	 * </p>
	 *
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public BasicHeader assertExists() throws AssertionError {
		if (getValue() == null)
			throw new BasicAssertionError("Response did not have the expected header {0}.", getName());
		return this;
	}

	/**
	 * Asserts that the header equals the specified value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the content type is JSON.</jc>
	 * 	Accept accept = ...;
	 * 	accept.assertValue(<js>"application/json"</js>);
	 * </p>
	 *
	 * @param value The value to test for.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public BasicHeader assertValue(String value) throws AssertionError {
		if (! StringUtils.isEquals(value, asString()))
			throw new BasicAssertionError("Response did not have the expected value for header {0}.\n\tExpected=[{1}]\n\tActual=[{2}]", getName(), value, asString());
		return this;
	}

	/**
	 * Asserts that the header passes the specified predicate test.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the content type is JSON.</jc>
	 * 	Accept accept = ...;
	 * 	accept.assertValue(x -&gt; x.equals(<js>"application/json"</js>);
	 * </p>
	 *
	 * @param test The predicate to test for.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public BasicHeader assertValue(Predicate<String> test) throws AssertionError {
		String text = asString();
		if (! test.test(text))
			throw new BasicAssertionError("Response did not have the expected value for header {0}.\n\tActual=[{1}]", getName(), text);
		return this;
	}

	/**
	 * Asserts that the header contains all the specified substrings.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the content type is JSON.</jc>
	 * 	Accept accept = ...;
	 * 	accept.assertContains(<js>"json"</js>);
	 * </p>
	 *
	 * @param values The substrings to test for.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public BasicHeader assertContains(String...values) throws AssertionError {
		String text = asString();
		for (String substring : values)
			if (! StringUtils.contains(text, substring))
				throw new BasicAssertionError("Response did not have the expected substring in header {0}.\n\tExpected=[{1}]\n\tHeader=[{2}]", getName(), substring, text);
		return this;
	}

	/**
	 * Asserts that the header matches the specified regular expression.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the content type is JSON.</jc>
	 * 	Accept accept = ...;
	 * 	accept.assertValueMatches(<js>".*json.*"</js>);
	 * </p>
	 *
	 * @param regex The pattern to test for.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public BasicHeader assertMatches(String regex) throws AssertionError {
		return assertMatches(regex, 0);
	}

	/**
	 * Asserts that the header matches the specified regular expression.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the content type is JSON.</jc>
	 * 	Accept accept = ...;
	 * 	accept.assertValueMatches(<js>".*json.*"</js>, <jsf>CASE_INSENSITIVE</jsf>);
	 * </p>
	 *
	 * @param regex The pattern to test for.
	 * @param flags Pattern match flags.  See {@link Pattern#compile(String, int)}.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public BasicHeader assertMatches(String regex, int flags) throws AssertionError {
		String text = asString();
		if (! Pattern.compile(regex, flags).matcher(text).matches())
			throw new BasicAssertionError("Response did not match expected pattern in header {0}.\n\tpattern=[{1}]\n\tHeader=[{2}]", getName(), regex, text);
		return this;
	}

	/**
	 * Asserts that the header matches the specified pattern.
	 *
	 * <p>
	 * The pattern can contain <js>"*"</js> to represent zero or more arbitrary characters.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the content type is JSON.</jc>
	 * 	Pattern p = Pattern.<jsm>compile</jsm>(<js>".*application\\/json.*"</js>);
	 * 	Accept accept = ...;
	 * 	accept.assertValueMatches(p);
	 * </p>
	 *
	 * @param pattern The pattern to test for.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public BasicHeader assertMatches(Pattern pattern) throws AssertionError {
		String text = asString();
		if (! pattern.matcher(text).matches())
			throw new BasicAssertionError("Response did not match expected pattern in header {0}.\n\tpattern=[{1}]\n\tHeader=[{2}]", getName(), pattern.pattern(), text);
		return this;
	}

	@Override /* Object */
	public String toString() {
		return asString();
	}
}
