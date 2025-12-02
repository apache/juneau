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
package org.apache.juneau.commons.collections;

import static org.apache.juneau.commons.utils.Utils.*;

import org.apache.juneau.commons.function.*;

/**
 * A simple mutable boolean flag.
 *
 * <p>
 * This class provides a thread-unsafe alternative to {@link java.util.concurrent.atomic.AtomicBoolean} for cases
 * where atomic operations are not required. It is useful in situations where you need to pass a mutable boolean
 * reference to lambdas, inner classes, or methods.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>
 * 		This class is <b>not thread-safe</b>. For concurrent access, use {@link java.util.concurrent.atomic.AtomicBoolean} instead.
 * 	<li class='note'>
 * 		This class supports only two states (<c>true</c>/<c>false</c>). If you need to represent three states
 * 		(<c>true</c>/<c>false</c>/<c>null</c>), use {@link BooleanValue} instead.
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a flag to track if an operation was performed</jc>
 * 	Flag <jv>processed</jv> = Flag.<jsm>create</jsm>();
 *
 * 	<jc>// Use in a lambda</jc>
 * 	list.forEach(<jv>x</jv> -&gt; {
 * 		<jk>if</jk> (<jv>x</jv>.needsProcessing()) {
 * 			<jv>processed</jv>.set();
 * 			process(<jv>x</jv>);
 * 		}
 * 	});
 *
 * 	<jk>if</jk> (<jv>processed</jv>.isSet()) {
 * 		<jsm>log</jsm>(<js>"Processing completed"</js>);
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonsCollections">juneau-commons-collections</a>
 * 	<li class='jc'>{@link BooleanValue}
 * </ul>
 */
public class Flag {

	/**
	 * Creates a new flag initialized to <jk>false</jk>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Flag <jv>flag</jv> = Flag.<jsm>create</jsm>();
	 * 	<jsm>assertTrue</jsm>(<jv>flag</jv>.isUnset());
	 * </p>
	 *
	 * @return A new flag.
	 */
	public static Flag create() {
		return of(false);
	}

	/**
	 * Creates a new flag with the specified initial state.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Flag <jv>flag</jv> = Flag.<jsm>of</jsm>(<jk>true</jk>);
	 * 	<jsm>assertTrue</jsm>(<jv>flag</jv>.isSet());
	 * </p>
	 *
	 * @param value The initial state of the flag.
	 * @return A new flag.
	 */
	public static Flag of(boolean value) {
		return new Flag(value);
	}

	private boolean value;

	private Flag(boolean value) {
		this.value = value;
	}

	/**
	 * Sets the flag to <jk>true</jk> and returns the previous value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Flag <jv>flag</jv> = Flag.<jsm>create</jsm>();
	 * 	<jk>boolean</jk> <jv>wasSet</jv> = <jv>flag</jv>.getAndSet();  <jc>// Returns false, flag is now true</jc>
	 * 	<jk>boolean</jk> <jv>wasSet2</jv> = <jv>flag</jv>.getAndSet(); <jc>// Returns true, flag remains true</jc>
	 * </p>
	 *
	 * @return The value before it was set to <jk>true</jk>.
	 */
	public boolean getAndSet() {
		var b = value;
		value = true;
		return b;
	}

	/**
	 * Sets the flag to <jk>false</jk> and returns the previous value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Flag <jv>flag</jv> = Flag.<jsm>of</jsm>(<jk>true</jk>);
	 * 	<jk>boolean</jk> <jv>wasSet</jv> = <jv>flag</jv>.getAndUnset();  <jc>// Returns true, flag is now false</jc>
	 * 	<jk>boolean</jk> <jv>wasSet2</jv> = <jv>flag</jv>.getAndUnset(); <jc>// Returns false, flag remains false</jc>
	 * </p>
	 *
	 * @return The value before it was set to <jk>false</jk>.
	 */
	public boolean getAndUnset() {
		var v = value;
		value = false;
		return v;
	}

	/**
	 * Executes a code snippet if the flag is <jk>false</jk>.
	 *
	 * <p>
	 * This method is useful for conditional execution based on the flag state, particularly in lambda expressions
	 * or method chains.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Flag <jv>initialized</jv> = Flag.<jsm>create</jsm>();
	 *
	 * 	<jc>// Initialize only once</jc>
	 * 	<jv>initialized</jv>.ifNotSet(() -&gt; {
	 * 		<jsm>initialize</jsm>();
	 * 		<jv>initialized</jv>.set();
	 * 	});
	 * </p>
	 *
	 * @param snippet The code snippet to execute if the flag is <jk>false</jk>.
	 * @return This object.
	 */
	public Flag ifNotSet(Snippet snippet) {
		if (! value)
			safe(snippet);
		return this;
	}

	/**
	 * Executes a code snippet if the flag is <jk>true</jk>.
	 *
	 * <p>
	 * This method is useful for conditional execution based on the flag state, particularly in lambda expressions
	 * or method chains.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Flag <jv>hasErrors</jv> = Flag.<jsm>create</jsm>();
	 *
	 * 	<jc>// Log only if errors occurred</jc>
	 * 	<jv>hasErrors</jv>.ifSet(() -&gt; <jsm>logErrors</jsm>());
	 * </p>
	 *
	 * @param snippet The code snippet to execute if the flag is <jk>true</jk>.
	 * @return This object.
	 */
	public Flag ifSet(Snippet snippet) {
		if (value)
			safe(snippet);
		return this;
	}

	/**
	 * Returns <jk>true</jk> if the flag is set.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Flag <jv>flag</jv> = Flag.<jsm>of</jsm>(<jk>true</jk>);
	 * 	<jsm>assertTrue</jsm>(<jv>flag</jv>.isSet());
	 * </p>
	 *
	 * @return <jk>true</jk> if the flag is set, <jk>false</jk> otherwise.
	 */
	public boolean isSet() { return value; }

	/**
	 * Returns <jk>true</jk> if the flag is not set.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Flag <jv>flag</jv> = Flag.<jsm>create</jsm>();
	 * 	<jsm>assertTrue</jsm>(<jv>flag</jv>.isUnset());
	 * </p>
	 *
	 * @return <jk>true</jk> if the flag is not set, <jk>false</jk> otherwise.
	 */
	public boolean isUnset() { return ! value; }

	/**
	 * Sets the flag to <jk>true</jk>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Flag <jv>flag</jv> = Flag.<jsm>create</jsm>();
	 * 	<jv>flag</jv>.set();
	 * 	<jsm>assertTrue</jsm>(<jv>flag</jv>.isSet());
	 * </p>
	 *
	 * @return This object.
	 */
	public Flag set() {
		value = true;
		return this;
	}

	/**
	 * Sets the flag to <jk>true</jk> if the specified value is <jk>true</jk>.
	 *
	 * <p>
	 * This method uses a logical OR operation, so once the flag is set, it remains set regardless of subsequent
	 * calls with <jk>false</jk>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Flag <jv>flag</jv> = Flag.<jsm>create</jsm>();
	 * 	<jv>flag</jv>.setIf(<jk>false</jk>);  <jc>// Flag remains false</jc>
	 * 	<jv>flag</jv>.setIf(<jk>true</jk>);   <jc>// Flag becomes true</jc>
	 * 	<jv>flag</jv>.setIf(<jk>false</jk>);  <jc>// Flag remains true</jc>
	 * </p>
	 *
	 * @param value If <jk>true</jk>, the flag will be set to <jk>true</jk>.
	 * @return This object.
	 */
	public Flag setIf(boolean value) {
		this.value |= value;
		return this;
	}

	/**
	 * Sets the flag to <jk>false</jk>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Flag <jv>flag</jv> = Flag.<jsm>of</jsm>(<jk>true</jk>);
	 * 	<jv>flag</jv>.unset();
	 * 	<jsm>assertTrue</jsm>(<jv>flag</jv>.isUnset());
	 * </p>
	 *
	 * @return This object.
	 */
	public Flag unset() {
		value = false;
		return this;
	}
}