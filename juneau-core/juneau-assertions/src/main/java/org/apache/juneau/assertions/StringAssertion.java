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

import static org.apache.juneau.common.internal.StringUtils.*;

import java.io.*;
import java.util.function.*;
import java.util.regex.*;

import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;

/**
 * Used for assertion calls against string objects.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Validates the response body of an HTTP call is the text "OK".</jc>
 * 	<jsm>assertString</jsm>(<jv>httpBody</jv>).is(<js>"OK"</js>);
 * </p>
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
 */
@FluentSetters(returns="StringAssertion")
public class StringAssertion extends FluentStringAssertion<StringAssertion> {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
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

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

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

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public StringAssertion setMsg(String msg, Object...args) {
		super.setMsg(msg, args);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public StringAssertion setOut(PrintStream value) {
		super.setOut(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public StringAssertion setSilent() {
		super.setSilent();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public StringAssertion setStdOut() {
		super.setStdOut();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public StringAssertion setThrowable(Class<? extends java.lang.RuntimeException> value) {
		super.setThrowable(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.FluentStringAssertion */
	public StringAssertion asJavaStrings() {
		super.asJavaStrings();
		return this;
	}

	// </FluentSetters>
}
