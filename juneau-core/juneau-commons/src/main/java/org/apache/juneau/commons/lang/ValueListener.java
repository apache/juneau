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
package org.apache.juneau.commons.lang;

/**
 * Listener interface for monitoring value changes in {@link Holder} objects.
 *
 * <p>
 * This functional interface allows you to register a callback that will be invoked whenever a {@link Holder}'s
 * content is changed via the {@link Holder#set(Object)} method. This is useful for logging, validation,
 * synchronization, or triggering side effects when values change.
 *
 * <p>
 * Only one listener can be registered per {@link Holder} object. Setting a new listener will replace any
 * previously registered listener.
 *
 * <h5 class='section'>Usage:</h5>
 * <p>
 * The listener is triggered every time {@link Holder#set(Object)} is called, including when setting
 * to <jk>null</jk>. Other methods like {@link Holder#getAndSet(Object)} also trigger the listener
 * since they call {@code set()} internally.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Simple logging listener</jc>
 * 	Holder&lt;String&gt; <jv>value</jv> = Holder.<jsm>of</jsm>(<js>"initial"</js>);
 * 	<jv>value</jv>.listener(<jv>newValue</jv> -&gt; <jsm>System.out</jsm>.println(<js>"Value changed to: "</js> + <jv>newValue</jv>));
 * 	<jv>value</jv>.set(<js>"updated"</js>);  <jc>// Prints "Value changed to: updated"</jc>
 *
 * 	<jc>// Validation listener</jc>
 * 	Holder&lt;Integer&gt; <jv>age</jv> = Holder.<jsm>of</jsm>(0);
 * 	<jv>age</jv>.listener(<jv>newAge</jv> -&gt; {
 * 		<jk>if</jk> (<jv>newAge</jv> != <jk>null</jk> &amp;&amp; <jv>newAge</jv> &lt; 0) {
 * 			<jk>throw new</jk> IllegalArgumentException(<js>"Age cannot be negative"</js>);
 * 		}
 * 	});
 *
 * 	<jc>// Synchronization listener</jc>
 * 	Holder&lt;String&gt; <jv>primary</jv> = Holder.<jsm>of</jsm>(<js>"data"</js>);
 * 	Holder&lt;String&gt; <jv>secondary</jv> = Holder.<jsm>empty</jsm>();
 * 	<jv>primary</jv>.listener(<jv>secondary</jv>::set);  <jc>// Keep secondary in sync with primary</jc>
 * 	<jv>primary</jv>.set(<js>"new data"</js>);
 * 	<jsm>assertEquals</jsm>(<jv>primary</jv>.get(), <jv>secondary</jv>.get());
 *
 * 	<jc>// Accumulator listener</jc>
 * 	IntegerHolder <jv>sum</jv> = IntegerHolder.<jsm>create</jsm>();
 * 	Holder&lt;Integer&gt; <jv>number</jv> = Holder.<jsm>of</jsm>(0);
 * 	<jv>number</jv>.listener(<jv>n</jv> -&gt; <jv>sum</jv>.set(<jv>sum</jv>.get() + (<jv>n</jv> != <jk>null</jk> ? <jv>n</jv> : 0)));
 * 	<jv>number</jv>.set(5).set(10).set(15);  <jc>// sum = 30</jc>
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>
 * 		The listener is called <b>after</b> the value has been changed, so {@link Holder#get()} will return
 * 		the new value.
 * 	<li class='note'>
 * 		The listener is <b>not</b> called by {@link Holder#getAndUnset()}, as that method sets the value to
 * 		<jk>null</jk> directly without going through {@link Holder#set(Object)}.
 * 	<li class='note'>
 * 		Exceptions thrown by the listener will propagate to the caller of {@link Holder#set(Object)}.
 * 	<li class='note'>
 * 		This interface is a functional interface and can be used with lambda expressions or method references.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonsLang">Lang Package</a>
 * 	<li class='jc'>{@link Holder}
 * </ul>
 *
 * @param <T> The type of value being monitored.
 */
@FunctionalInterface
public interface ValueListener<T> {

	/**
	 * Called whenever {@link Holder#set(Object)} is invoked on the associated {@link Holder} object.
	 *
	 * <p>
	 * This method is called <b>after</b> the value has been updated, so calling {@link Holder#get()} on the
	 * associated value object will return the new value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Holder&lt;String&gt; <jv>value</jv> = Holder.<jsm>of</jsm>(<js>"old"</js>);
	 * 	<jv>value</jv>.listener((<jv>newValue</jv>) -&gt; {
	 * 		<jsm>System.out</jsm>.println(<js>"Changed to: "</js> + <jv>newValue</jv>);
	 * 	});
	 * 	<jv>value</jv>.set(<js>"new"</js>);  <jc>// Prints "Changed to: new"</jc>
	 * </p>
	 *
	 * @param newValue The new value that was set. Can be <jk>null</jk>.
	 */
	void onSet(T newValue);
}