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
package org.apache.juneau.commons.function;

/**
 * A functional interface representing a supplier of results that may throw a checked exception.
 *
 * <p>
 * This interface extends the standard Java {@link java.util.function.Supplier} pattern to allow
 * the {@link #get()} method to throw checked exceptions. This is useful when you need to pass
 * suppliers that may throw exceptions (such as I/O operations) to methods that expect functional
 * interfaces.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li>Functional interface - can be used with lambda expressions and method references
 * 	<li>Exception support - allows checked exceptions to be thrown
 * 	<li>Compatible with Supplier - can be used where Supplier is expected (exceptions are wrapped)
 * </ul>
 *
 * <h5 class='section'>Use Cases:</h5>
 * <ul class='spaced-list'>
 * 	<li>Suppliers that perform I/O operations (file reading, network calls)
 * 	<li>Suppliers that may throw business logic exceptions
 * 	<li>Lazy initialization that may fail
 * 	<li>Resource creation that may throw exceptions
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Lambda with exception</jc>
 * 	ThrowingSupplier&lt;String&gt; <jv>fileReader</jv> = () -&gt; {
 * 		<jk>return new</jk> String(Files.readAllBytes(Paths.get(<js>"data.txt"</js>)));
 * 	};
 *
 * 	<jc>// Method reference</jc>
 * 	ThrowingSupplier&lt;Connection&gt; <jv>dbConnection</jv> = <jv>dataSource</jv>::getConnection;
 *
 * 	<jc>// Usage in try-catch</jc>
 * 	<jk>try</jk> {
 * 		String <jv>content</jv> = <jv>fileReader</jv>.get();
 * 	} <jk>catch</jk> (Exception <jv>e</jv>) {
 * 		<jc>// Handle exception</jc>
 * 	}
 * </p>
 *
 * <h5 class='section'>Comparison with Supplier:</h5>
 * <ul class='spaced-list'>
 * 	<li><b>Supplier:</b> Cannot throw checked exceptions (must catch and wrap)
 * 	<li><b>ThrowingSupplier:</b> Can throw checked exceptions directly
 * 	<li><b>Supplier:</b> Used for standard functional programming patterns
 * 	<li><b>ThrowingSupplier:</b> Used when operations may fail with checked exceptions
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link ThrowingFunction} - Function that throws exceptions
 * 	<li class='jc'>{@link ThrowingConsumer} - Consumer that throws exceptions
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonFunction">juneau-common-function</a>
 * </ul>
 *
 * @param <T> The type of results supplied by this supplier.
 */
@FunctionalInterface
public interface ThrowingSupplier<T> {

	/**
	 * Gets a result, potentially throwing a checked exception.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	ThrowingSupplier&lt;String&gt; <jv>supplier</jv> = () -&gt; {
	 * 		<jk>if</jk> (<jv>fileNotFound</jv>) {
	 * 			<jk>throw new</jk> FileNotFoundException(<js>"File not found"</js>);
	 * 		}
	 * 		<jk>return</jk> <js>"content"</js>;
	 * 	};
	 *
	 * 	<jk>try</jk> {
	 * 		String <jv>result</jv> = <jv>supplier</jv>.get();
	 * 	} <jk>catch</jk> (FileNotFoundException <jv>e</jv>) {
	 * 		<jc>// Handle exception</jc>
	 * 	}
	 * </p>
	 *
	 * @return A result.
	 * @throws Exception If an error occurs during result computation.
	 */
	T get() throws Exception;
}