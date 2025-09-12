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
package org.apache.juneau.junit.bct;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * Collection of standard swapper implementations for the Bean-Centric Testing framework.
 *
 * <p>This class provides built-in object transformation strategies that handle common wrapper
 * types and asynchronous objects. These swappers are automatically registered when using
 * {@link BasicBeanConverter.Builder#defaultSettings()}.</p>
 *
 * <h5 class='section'>Purpose:</h5>
 * <p>Swappers pre-process objects before stringification or listification, enabling the converter
 * to handle wrapper types, lazy evaluation objects, and asynchronous results consistently. They
 * transform objects into more primitive forms that can be easily converted to strings or lists.</p>
 *
 * <h5 class='section'>Built-in Swappers:</h5>
 * <ul>
 *    <li><b>{@link #optionalSwapper()}</b> - Unwraps {@link Optional} values to their contained objects or <jk>null</jk></li>
 *    <li><b>{@link #supplierSwapper()}</b> - Evaluates {@link Supplier} functions to get their supplied values</li>
 *    <li><b>{@link #futureSwapper()}</b> - Extracts completed {@link Future} results or returns status messages</li>
 * </ul>
 *
 * <h5 class='section'>Usage Example:</h5>
 * <p class='bjava'>
 *    <jc>// Register swappers using builder</jc>
 *    <jk>var</jk> <jv>converter</jv> = BasicBeanConverter.<jsm>builder</jsm>()
 *       .defaultSettings()
 *       .addSwapper(Optional.<jk>class</jk>, Swappers.<jsm>optionalSwapper</jsm>())
 *       .addSwapper(Supplier.<jk>class</jk>, Swappers.<jsm>supplierSwapper</jsm>())
 *       .addSwapper(Future.<jk>class</jk>, Swappers.<jsm>futureSwapper</jsm>())
 *       .build();
 * </p>
 *
 * <h5 class='section'>Custom Swapper Development:</h5>
 * <p>When creating custom swappers, follow these patterns:</p>
 * <ul>
 *    <li><b>Null Safety:</b> Handle <jk>null</jk> inputs gracefully</li>
 *    <li><b>Exception Handling:</b> Convert exceptions to meaningful error messages</li>
 *    <li><b>Status Indication:</b> Provide clear status for incomplete or error states</li>
 *    <li><b>Non-blocking:</b> Avoid blocking operations that might hang tests</li>
 *    <li><b>Recursion Prevention:</b> Ensure swappers don't create circular transformations that could lead to {@link StackOverflowError}.
 *        The framework does not check for recursion, so developers must avoid registering swappers that transform objects
 *        back to types that would trigger the same or other swappers in an endless cycle.</li>
 * </ul>
 *
 * @see Swapper
 * @see BasicBeanConverter.Builder#addSwapper(Class, Swapper)
 * @see BasicBeanConverter.Builder#defaultSettings()
 */
@SuppressWarnings("rawtypes")
public class Swappers {

	/**
	 * Constructor.
	 */
	private Swappers() {}

	/**
	 * Returns a swapper for {@link Optional} objects that unwraps them to their contained values.
	 *
	 * <p>This swapper extracts the value from Optional instances, returning the contained object
	 * if present, or <jk>null</jk> if the Optional is empty. This allows Optional-wrapped values
	 * to be processed naturally by the converter without special handling.</p>
	 *
	 * <h5 class='section'>Behavior:</h5>
	 * <ul>
	 *    <li><b>Present value:</b> Returns the contained object for further processing</li>
	 *    <li><b>Empty Optional:</b> Returns <jk>null</jk>, which will be rendered as the configured null value</li>
	 * </ul>
	 *
	 * <h5 class='section'>Usage Examples:</h5>
	 * <p class='bjava'>
	 *    <jc>// Test Optional with value</jc>
	 *    <jk>var</jk> <jv>optional</jv> = Optional.<jsm>of</jsm>(<js>"Hello"</js>);
	 *    <jsm>assertBean</jsm>(<jv>optional</jv>, <js>"&lt;self&gt;"</js>, <js>"Hello"</js>);
	 *
	 *    <jc>// Test empty Optional</jc>
	 *    <jk>var</jk> <jv>empty</jv> = Optional.<jsm>empty</jsm>();
	 *    <jsm>assertBean</jsm>(<jv>empty</jv>, <js>"&lt;self&gt;"</js>, <js>"&lt;null&gt;"</js>);
	 *
	 *    <jc>// Test nested Optional in object</jc>
	 *    <jk>var</jk> <jv>user</jv> = <jk>new</jk> User().setName(Optional.<jsm>of</jsm>(<js>"John"</js>));
	 *    <jsm>assertBean</jsm>(<jv>user</jv>, <js>"name"</js>, <js>"John"</js>);
	 * </p>
	 *
	 * <h5 class='section'>Integration:</h5>
	 * <p>This swapper is particularly useful when working with modern Java APIs that return
	 * Optional values. It allows test assertions to focus on the actual data rather than
	 * Optional wrapper mechanics.</p>
	 *
	 * @return A {@link Swapper} for {@link Optional} objects
	 * @see Optional
	 */
	public static Swapper<Optional> optionalSwapper() {
		return (bc, optional) -> optional.orElse(null);
	}

	/**
	 * Returns a swapper for {@link Supplier} objects that evaluates them to get their supplied values.
	 *
	 * <p>This swapper calls the {@link Supplier#get()} method to obtain the value that the supplier
	 * provides. This enables testing of lazy-evaluated or dynamically-computed values as if they
	 * were regular objects.</p>
	 *
	 * <h5 class='section'>Behavior:</h5>
	 * <ul>
	 *    <li><b>Successful evaluation:</b> Returns the result of calling {@link Supplier#get()}</li>
	 *    <li><b>Exception during evaluation:</b> Allows the exception to propagate (no special handling)</li>
	 * </ul>
	 *
	 * <h5 class='section'>Usage Examples:</h5>
	 * <p class='bjava'>
	 *    <jc>// Test supplier with simple value</jc>
	 *    <jk>var</jk> <jv>supplier</jv> = () -&gt; <js>"Hello World"</js>;
	 *    <jsm>assertBean</jsm>(<jv>supplier</jv>, <js>"&lt;self&gt;"</js>, <js>"Hello World"</js>);
	 *
	 *    <jc>// Test supplier in object property</jc>
	 *    <jk>var</jk> <jv>config</jv> = <jk>new</jk> Configuration().setDynamicValue(() -&gt; calculateValue());
	 *    <jsm>assertBean</jsm>(<jv>config</jv>, <js>"dynamicValue"</js>, <js>"computed-result"</js>);
	 *
	 *    <jc>// Test supplier returning null</jc>
	 *    <jk>var</jk> <jv>nullSupplier</jv> = () -&gt; <jk>null</jk>;
	 *    <jsm>assertBean</jsm>(<jv>nullSupplier</jv>, <js>"&lt;self&gt;"</js>, <js>"&lt;null&gt;"</js>);
	 * </p>
	 *
	 * <h5 class='section'>Important Notes:</h5>
	 * <ul>
	 *    <li><b>Side Effects:</b> The supplier will be evaluated during conversion, which may cause side effects</li>
	 *    <li><b>Performance:</b> Expensive computations in suppliers will impact test performance</li>
	 *    <li><b>Exception Handling:</b> Exceptions from suppliers are not caught by this swapper</li>
	 * </ul>
	 *
	 * @return A {@link Swapper} for {@link Supplier} objects
	 * @see Supplier
	 */
	public static Swapper<Supplier> supplierSwapper() {
		return (bc, supplier) -> supplier.get();
	}

	/**
	 * Returns a swapper for {@link Future} objects that extracts completed results or status information.
	 *
	 * <p>This swapper handles {@link Future} objects in a non-blocking manner, providing meaningful
	 * information about the future's state without waiting for completion:</p>
	 *
	 * <h5 class='section'>Behavior:</h5>
	 * <ul>
	 *    <li><b>Completed successfully:</b> Returns the actual result value</li>
	 *    <li><b>Completed with exception:</b> Returns <js>"&lt;error: {message}&gt;"</js> format</li>
	 *    <li><b>Cancelled:</b> Returns <js>"&lt;cancelled&gt;"</js></li>
	 *    <li><b>Still pending:</b> Returns <js>"&lt;pending&gt;"</js></li>
	 * </ul>
	 *
	 * <h5 class='section'>Usage Examples:</h5>
	 * <p class='bjava'>
	 *    <jc>// Test completed future</jc>
	 *    <jk>var</jk> <jv>future</jv> = CompletableFuture.<jsm>completedFuture</jsm>(<js>"Hello"</js>);
	 *    <jsm>assertBean</jsm>(<jv>future</jv>, <js>"&lt;self&gt;"</js>, <js>"Hello"</js>);
	 *
	 *    <jc>// Test pending future</jc>
	 *    <jk>var</jk> <jv>pending</jv> = <jk>new</jk> CompletableFuture&lt;String&gt;();
	 *    <jsm>assertBean</jsm>(<jv>pending</jv>, <js>"&lt;self&gt;"</js>, <js>"&lt;pending&gt;"</js>);
	 *
	 *    <jc>// Test cancelled future</jc>
	 *    <jk>var</jk> <jv>cancelled</jv> = <jk>new</jk> CompletableFuture&lt;String&gt;();
	 *    <jv>cancelled</jv>.cancel(<jk>true</jk>);
	 *    <jsm>assertBean</jsm>(<jv>cancelled</jv>, <js>"&lt;self&gt;"</js>, <js>"&lt;cancelled&gt;"</js>);
	 *
	 *    <jc>// Test failed future</jc>
	 *    <jk>var</jk> <jv>failed</jv> = <jk>new</jk> CompletableFuture&lt;String&gt;();
	 *    <jv>failed</jv>.completeExceptionally(<jk>new</jk> RuntimeException(<js>"Test error"</js>));
	 *    <jsm>assertBean</jsm>(<jv>failed</jv>, <js>"&lt;self&gt;"</js>, <js>"&lt;error: Test error&gt;"</js>);
	 * </p>
	 *
	 * <h5 class='section'>Non-blocking Guarantee:</h5>
	 * <p>This swapper never calls {@link Future#get()} without first checking {@link Future#isDone()},
	 * ensuring that test execution is never blocked by incomplete futures. This makes it safe to use
	 * in unit tests without risking hangs or timeouts.</p>
	 *
	 * <h5 class='section'>Error Handling:</h5>
	 * <p>When a future completes exceptionally, the swapper extracts the exception message and
	 * formats it as <js>"&lt;error: {message}&gt;"</js>. This provides useful debugging information
	 * while maintaining a consistent string format for assertions.</p>
	 *
	 * @return A {@link Swapper} for {@link Future} objects
	 * @see Future
	 * @see CompletableFuture
	 */
	public static Swapper<Future> futureSwapper() {
		return (bc, future) -> {
			if (future.isDone() && !future.isCancelled()) {
				try {
					return future.get();
				} catch (Exception e) {  // NOSONAR
					return "<error: " + e.getMessage() + ">";
				}
			}
			return future.isCancelled() ? "<cancelled>" : "<pending>";
		};
	}
}
