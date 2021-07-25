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

import java.io.*;
import java.util.function.*;
import java.util.regex.*;

import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;

/**
 * Used for assertion calls against string objects.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Validates the response body of an HTTP call is the text "OK".</jc>
 * 	<jsm>assertString</jsm>(<jv>httpBody</jv>).is(<js>"OK"</js>);
 * </p>
 *
 *
 * <h5 class='topic'>Test Methods</h5>
 * 	<ul>
 * 		<li class='jm'>{@link FluentStringAssertion#is(String)}
 * 		<li class='jm'>{@link FluentStringAssertion#isNot(String)}
 * 		<li class='jm'>{@link FluentStringAssertion#isLines(String...)}
 * 		<li class='jm'>{@link FluentStringAssertion#isSortedLines(String...)}
 * 		<li class='jm'>{@link FluentStringAssertion#isIc(String)}
 * 		<li class='jm'>{@link FluentStringAssertion#isNotIc(String)}
 * 		<li class='jm'>{@link FluentStringAssertion#contains(String...)}
 * 		<li class='jm'>{@link FluentStringAssertion#doesNotContain(String...)}
 * 		<li class='jm'>{@link FluentStringAssertion#isEmpty()}
 * 		<li class='jm'>{@link FluentStringAssertion#isNotEmpty()}
 * 		<li class='jm'>{@link FluentStringAssertion#matches(String)}
 * 		<li class='jm'>{@link FluentStringAssertion#regex(String)}
 * 		<li class='jm'>{@link FluentStringAssertion#regex(String,int)}
 * 		<li class='jm'>{@link FluentStringAssertion#regex(Pattern)}
 * 		<li class='jm'>{@link FluentStringAssertion#startsWith(String)}
 * 		<li class='jm'>{@link FluentStringAssertion#endsWith(String)}
 * 		<li class='jm'>{@link FluentObjectAssertion#exists()}
 * 		<li class='jm'>{@link FluentObjectAssertion#is(Object)}
 * 		<li class='jm'>{@link FluentObjectAssertion#is(Predicate)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isNot(Object)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isAny(Object...)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isNotAny(Object...)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isNull()}
 * 		<li class='jm'>{@link FluentObjectAssertion#isNotNull()}
 * 		<li class='jm'>{@link FluentObjectAssertion#isString(String)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isJson(String)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isSame(Object)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isSameJsonAs(Object)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isSameSortedJsonAs(Object)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isSameSerializedAs(Object, WriterSerializer)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isType(Class)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isExactType(Class)}
 * 	</ul>
 *
 * <h5 class='topic'>Transform Methods</h5>
 * 	<ul>
 * 		<li class='jm'>{@link FluentStringAssertion#replaceAll(String,String)}
 * 		<li class='jm'>{@link FluentStringAssertion#replace(String,String)}
 * 		<li class='jm'>{@link FluentStringAssertion#urlDecode()}
 * 		<li class='jm'>{@link FluentStringAssertion#lc()}
 * 		<li class='jm'>{@link FluentStringAssertion#uc()}
 * 		<li class='jm'>{@link FluentStringAssertion#lines()}
 * 		<li class='jm'>{@link FluentStringAssertion#split(String)}
 * 		<li class='jm'>{@link FluentStringAssertion#length()}
 * 		<li class='jm'>{@link FluentStringAssertion#oneLine()}
 * 		<li class='jm'>{@link FluentObjectAssertion#asString()}
 * 		<li class='jm'>{@link FluentObjectAssertion#asString(WriterSerializer)}
 * 		<li class='jm'>{@link FluentObjectAssertion#asString(Function)}
 * 		<li class='jm'>{@link FluentObjectAssertion#asJson()}
 * 		<li class='jm'>{@link FluentObjectAssertion#asJsonSorted()}
 * 		<li class='jm'>{@link FluentObjectAssertion#apply(Function)}
 * 		<li class='jm'>{@link FluentObjectAssertion#asAny()}
 *	</ul>
 *
 * <h5 class='topic'>Configuration Methods</h5>
 * 	<ul>
 * 		<li class='jm'>{@link FluentStringAssertion#javaStrings()}
 * 		<li class='jm'>{@link Assertion#msg(String, Object...)}
 * 		<li class='jm'>{@link Assertion#out(PrintStream)}
 * 		<li class='jm'>{@link Assertion#silent()}
 * 		<li class='jm'>{@link Assertion#stdout()}
 * 		<li class='jm'>{@link Assertion#throwable(Class)}
 * 	</ul>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc Assertions}
 * </ul>
 */
@FluentSetters(returns="StringAssertion")
public class StringAssertion extends FluentStringAssertion<StringAssertion> {

	//-----------------------------------------------------------------------------------------------------------------
	// Constructors
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The object being tested.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new assertion object.
	 */
	public static StringAssertion create(Object value) {
		return new StringAssertion(value);
	}

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The object being tested.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public StringAssertion(Object value) {
		super(stringify(value), null);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Fluent setters
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - Assertion */
	public StringAssertion msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public StringAssertion out(PrintStream value) {
		super.out(value);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public StringAssertion silent() {
		super.silent();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public StringAssertion stdout() {
		super.stdout();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public StringAssertion throwable(Class<? extends java.lang.RuntimeException> value) {
		super.throwable(value);
		return this;
	}

	@Override /* GENERATED - FluentStringAssertion */
	public StringAssertion javaStrings() {
		super.javaStrings();
		return this;
	}

	// </FluentSetters>
}
