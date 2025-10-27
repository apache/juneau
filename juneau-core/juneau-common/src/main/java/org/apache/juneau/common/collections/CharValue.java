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

import static org.apache.juneau.common.utils.Utils.*;

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

	/**
	 * Increments the value by 1.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	CharValue <jv>value</jv> = CharValue.<jsm>of</jsm>('A');
	 * 	<jv>value</jv>.increment();
	 * 	<jsm>assertEquals</jsm>('B', <jv>value</jv>.get());
	 * </p>
	 *
	 * @return This object.
	 */
	public CharValue increment() {
		var v = get();
		set((char)((v == null ? 0 : v) + 1));
		return this;
	}

	/**
	 * Decrements the value by 1.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	CharValue <jv>value</jv> = CharValue.<jsm>of</jsm>('B');
	 * 	<jv>value</jv>.decrement();
	 * 	<jsm>assertEquals</jsm>('A', <jv>value</jv>.get());
	 * </p>
	 *
	 * @return This object.
	 */
	public CharValue decrement() {
		var v = get();
		set((char)((v == null ? 0 : v) - 1));
		return this;
	}

	/**
	 * Increments the value by 1 and returns the new value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	CharValue <jv>value</jv> = CharValue.<jsm>of</jsm>('A');
	 * 	<jk>char</jk> <jv>result</jv> = <jv>value</jv>.incrementAndGet();  <jc>// Returns 'B'</jc>
	 * 	<jsm>assertEquals</jsm>('B', <jv>value</jv>.get());
	 * </p>
	 *
	 * @return The incremented value.
	 */
	public Character incrementAndGet() {
		var v = get();
		var result = (char)((v == null ? 0 : v) + 1);
		set(result);
		return result;
	}

	/**
	 * Decrements the value by 1 and returns the new value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	CharValue <jv>value</jv> = CharValue.<jsm>of</jsm>('B');
	 * 	<jk>char</jk> <jv>result</jv> = <jv>value</jv>.decrementAndGet();  <jc>// Returns 'A'</jc>
	 * 	<jsm>assertEquals</jsm>('A', <jv>value</jv>.get());
	 * </p>
	 *
	 * @return The decremented value.
	 */
	public Character decrementAndGet() {
		var v = get();
		var result = (char)((v == null ? 0 : v) - 1);
		set(result);
		return result;
	}

	/**
	 * Adds the specified value to the current value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	CharValue <jv>value</jv> = CharValue.<jsm>of</jsm>('A');
	 * 	<jv>value</jv>.add((<jk>char</jk>)5);
	 * 	<jsm>assertEquals</jsm>('F', <jv>value</jv>.get());
	 * </p>
	 *
	 * @param x The value to add.
	 * @return This object.
	 */
	public CharValue add(Character x) {
		var v = get();
		set((char)((v == null ? 0 : v) + (x == null ? 0 : x)));
		return this;
	}

	/**
	 * Adds the specified value to the current value and returns the new value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	CharValue <jv>value</jv> = CharValue.<jsm>of</jsm>('A');
	 * 	<jk>char</jk> <jv>result</jv> = <jv>value</jv>.addAndGet((<jk>char</jk>)5);  <jc>// Returns 'F'</jc>
	 * 	<jsm>assertEquals</jsm>('F', <jv>value</jv>.get());
	 * </p>
	 *
	 * @param x The value to add.
	 * @return The new value after addition.
	 */
	public Character addAndGet(Character x) {
		var v = get();
		var result = (char)((v == null ? 0 : v) + (x == null ? 0 : x));
		set(result);
		return result;
	}

	/**
	 * Checks if the current value is equal to the specified character.
	 *
	 * <p>
	 * Uses {@link Utils#eq(Object, Object)} for deep equality comparison, which handles nulls safely.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	CharValue <jv>value</jv> = CharValue.<jsm>of</jsm>('A');
	 * 	<jsm>assertTrue</jsm>(<jv>value</jv>.is('A'));
	 * 	<jsm>assertFalse</jsm>(<jv>value</jv>.is('B'));
	 * </p>
	 *
	 * @param value The character to compare to.
	 * @return <jk>true</jk> if the current value is equal to the specified character.
	 */
	public boolean is(Character value) {
		return eq(get(), value);
	}

	/**
	 * Checks if the current value matches any of the specified characters.
	 *
	 * <p>
	 * Uses {@link Utils#eq(Object, Object)} for deep equality comparison of each character.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	CharValue <jv>value</jv> = CharValue.<jsm>of</jsm>('B');
	 * 	<jsm>assertTrue</jsm>(<jv>value</jv>.isAny('A', 'B', 'C'));
	 * 	<jsm>assertFalse</jsm>(<jv>value</jv>.isAny('X', 'Y'));
	 * </p>
	 *
	 * @param values The characters to compare to.
	 * @return <jk>true</jk> if the current value matches any of the specified characters.
	 */
	public boolean isAny(Character... values) {
		var current = get();
		for (var value : values)
			if (eq(current, value))
				return true;
		return false;
	}

	/**
	 * Checks if the current value matches any character in the specified string.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	CharValue <jv>value</jv> = CharValue.<jsm>of</jsm>('B');
	 * 	<jsm>assertTrue</jsm>(<jv>value</jv>.isAny(<js>"ABC"</js>));
	 * 	<jsm>assertFalse</jsm>(<jv>value</jv>.isAny(<js>"XYZ"</js>));
	 *
	 * 	<jc>// Null/empty string returns false</jc>
	 * 	<jsm>assertFalse</jsm>(<jv>value</jv>.isAny((<jk>null</jk>));
	 * 	<jsm>assertFalse</jsm>(<jv>value</jv>.isAny(<js>""</js>));
	 * </p>
	 *
	 * @param values The string containing characters to compare to.
	 * @return <jk>true</jk> if the current value matches any character in the string.
	 */
	public boolean isAny(String values) {
		if (values == null || values.isEmpty())
			return false;
		var current = get();
		if (current == null)
			return false;
		return values.indexOf(current) >= 0;
	}
}
