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
package org.apache.juneau.commons.reflect;

/**
 * Defines how array types should be formatted when rendered as strings.
 *
 * <p>
 * Controls the notation used to represent array dimensions in class names.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Given class: String[][]</jc>
 *
 * 	ClassArrayFormat.<jsf>JVM</jsf>        <jc>// "[[Ljava.lang.String;"</jc>
 * 	ClassArrayFormat.<jsf>BRACKETS</jsf>   <jc>// "String[][]"</jc>
 * 	ClassArrayFormat.<jsf>WORD</jsf>       <jc>// "StringArrayArray"</jc>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-commons">Common</a>
 * </ul>
 */
public enum ClassArrayFormat {

	/**
	 * JVM bytecode notation - prefix with <c>[</c> for each dimension.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><js>"[Ljava.lang.String;"</js> - <c>String[]</c>
	 * 	<li><js>"[[Ljava.lang.String;"</js> - <c>String[][]</c>
	 * 	<li><js>"[I"</js> - <c>int[]</c>
	 * 	<li><js>"[[I"</js> - <c>int[][]</c>
	 * 	<li><js>"[Z"</js> - <c>boolean[]</c>
	 * </ul>
	 *
	 * <p>
	 * This is the format returned by {@link Class#getName()} for array types.
	 * It uses JVM internal type descriptors where object arrays are prefixed with <c>[L</c>
	 * and suffixed with <c>;</c>, and primitive arrays use single-letter codes
	 * (<c>I</c>, <c>Z</c>, <c>B</c>, <c>C</c>, <c>D</c>, <c>F</c>, <c>J</c>, <c>S</c>).
	 *
	 * <p>
	 * This format is primarily used for reflection and JVM internals.
	 */
	JVM,

	/**
	 * Source code notation - suffix with <c>[]</c> for each dimension.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><js>"String[]"</js> - <c>String[]</c>
	 * 	<li><js>"String[][]"</js> - <c>String[][]</c>
	 * 	<li><js>"int[]"</js> - <c>int[]</c>
	 * 	<li><js>"int[][]"</js> - <c>int[][]</c>
	 * 	<li><js>"boolean[]"</js> - <c>boolean[]</c>
	 * </ul>
	 *
	 * <p>
	 * This is the format used in Java source code and is the most human-readable.
	 * It's returned by {@link Class#getSimpleName()} and {@link Class#getCanonicalName()}
	 * for array types.
	 *
	 * <p>
	 * This is the most common format for documentation and display purposes.
	 */
	BRACKETS,

	/**
	 * Word notation - suffix with <js>"Array"</js> for each dimension.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><js>"StringArray"</js> - <c>String[]</c>
	 * 	<li><js>"StringArrayArray"</js> - <c>String[][]</c>
	 * 	<li><js>"intArray"</js> - <c>int[]</c>
	 * 	<li><js>"intArrayArray"</js> - <c>int[][]</c>
	 * 	<li><js>"booleanArray"</js> - <c>boolean[]</c>
	 * </ul>
	 *
	 * <p>
	 * This format is useful for generating variable names, method names, or other
	 * identifiers where special characters like <c>[]</c> are not allowed.
	 *
	 * <p>
	 * This format is particularly useful in code generation scenarios where you need
	 * valid Java identifiers.
	 */
	WORD
}
