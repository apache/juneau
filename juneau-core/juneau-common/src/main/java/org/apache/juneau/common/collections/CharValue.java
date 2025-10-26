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
package org.apache.juneau.common.collections;

/**
 * A simple mutable character value.
 *
 * <p>
 * This class extends {@link Value}&lt;{@link Character}&gt; and provides a convenient way to pass mutable
 * character references to lambdas, inner classes, or methods.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>
 * 		This class is <b>not thread-safe</b>.
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a character value to track the last character seen</jc>
 * 	CharValue <jv>lastChar</jv> = CharValue.<jsm>create</jsm>();
 *
 * 	<jc>// Use in a lambda to track state</jc>
 * 	charStream.forEach(<jv>ch</jv> -&gt; {
 * 		<jk>if</jk> (Character.isUpperCase(<jv>ch</jv>)) {
 * 			<jv>lastChar</jv>.set(<jv>ch</jv>);
 * 		}
 * 	});
 *
 * 	<jsm>log</jsm>(<js>"Last uppercase char: "</js> + <jv>lastChar</jv>.get());
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonCollections">juneau-common-collections</a>
 * </ul>
 */
public class CharValue extends Value<Character> {

	/**
	 * Creates a new character value initialized to <c>'\0'</c> (null character).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	CharValue <jv>value</jv> = CharValue.<jsm>create</jsm>();
	 * 	<jsm>assertEquals</jsm>('\0', <jv>value</jv>.get());
	 * </p>
	 *
	 * @return A new character value.
	 */
	public static CharValue create() {
		return of('\0');
	}

	/**
	 * Creates a new character value with the specified initial value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	CharValue <jv>value</jv> = CharValue.<jsm>of</jsm>('A');
	 * 	<jsm>assertEquals</jsm>('A', <jv>value</jv>.get());
	 * </p>
	 *
	 * @param value The initial value.
	 * @return A new character value.
	 */
	public static CharValue of(Character value) {
		return new CharValue(value);
	}

	/**
	 * Constructor.
	 *
	 * @param value The initial value.
	 */
	public CharValue(Character value) {
		super(value);
	}
}
