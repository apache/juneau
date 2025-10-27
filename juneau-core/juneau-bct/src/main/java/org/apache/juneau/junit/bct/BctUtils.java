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
package org.apache.juneau.junit.bct;

import static java.util.Optional.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.util.*;
import java.util.function.*;

import org.opentest4j.*;

/**
 * Bean-Centric Testing utility methods.
 *
 * <p>
 * This class contains static utility methods specific to the Bean-Centric Testing framework.
 * For general-purpose utility methods, use the classes in {@code org.apache.juneau.common.utils} package.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 *   <li class='jc'>{@link org.apache.juneau.common.utils.Utils} - General utility methods
 *   <li class='jc'>{@link org.apache.juneau.common.utils.AssertionUtils} - Argument validation methods
 *   <li class='jc'>{@link org.apache.juneau.common.utils.StringUtils} - String manipulation methods
 * </ul>
 */
public class BctUtils {

	// BCT-specific methods

	/**
	 * Creates an {@link AssertionFailedError} for failed equality assertions.
	 *
	 * <p>This method constructs a properly formatted assertion failure with expected and actual values
	 * for use in test frameworks. The message follows JUnit's standard format for assertion failures.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 *   <jk>if</jk> (!<jsm>eq</jsm>(<jv>expected</jv>, <jv>actual</jv>)) {
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

	private BctUtils() {}
}

