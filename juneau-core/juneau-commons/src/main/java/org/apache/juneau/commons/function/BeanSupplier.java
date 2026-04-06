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
 * A lifecycle-aware producer for use as a serializer source, providing beans lazily one at a time.
 *
 * <p>
 * Extends {@link Iterable} so that {@code ClassMeta}'s existing {@code ITERABLE} category picks it
 * up automatically — no new type category is needed in the framework.
 *
 * <p>
 * The serializer drives the full lifecycle:
 * <ol>
 * 	<li>Calls {@link #begin()} before iteration starts
 * 	<li>Iterates elements via {@link #iterator()}, serializing each
 * 	<li>If an exception occurs during iteration, calls {@link #onError(Exception)}
 * 	<li>Always calls {@link #complete()} after iteration (like {@code finally}) — whether
 * 	    serialization succeeded or failed. Use this for resource cleanup (close cursors, connections, etc.)
 * </ol>
 *
 * <p>
 * For round-trip support (same property used for both serialization and parsing), use
 * {@link BeanChannel} instead, which extends both {@link BeanSupplier} and {@link BeanConsumer}.
 *
 * <p>
 * If a {@link BeanSupplier} is encountered during parsing (not serialization), the parser will
 * throw an {@link IllegalArgumentException} with a message recommending {@link BeanConsumer} or
 * {@link BeanChannel}.
 *
 * <h5 class='section'>Example (DB-backed supplier via JDBC cursor):</h5>
 * <p class='bjava'>
 * 	<ja>@Bean</ja>(factory=ItemSupplierFactory.<jk>class</jk>)
 * 	<jk>public class</jk> ItemSupplier <jk>implements</jk> BeanSupplier&lt;Item&gt; {
 * 		<jk>private</jk> Connection <jv>conn</jv>;
 * 		<jk>private</jk> ResultSet <jv>rs</jv>;
 *
 * 		<ja>@Override</ja> <jk>public void</jk> begin() <jk>throws</jk> Exception {
 * 			<jv>conn</jv> = <jv>ds</jv>.getConnection();
 * 			<jv>rs</jv> = <jv>conn</jv>.prepareStatement(<js>"SELECT * FROM items"</js>).executeQuery();
 * 		}
 *
 * 		<ja>@Override</ja> <jk>public</jk> Iterator&lt;Item&gt; iterator() {
 * 			<jk>return new</jk> ResultSetIterator&lt;&gt;(<jv>rs</jv>, Item::<jv>fromRow</jv>);
 * 		}
 *
 * 		<ja>@Override</ja> <jk>public void</jk> onError(Exception <jv>e</jv>) <jk>throws</jk> Exception {
 * 			<jv>rs</jv>.close(); <jk>throw</jk> <jv>e</jv>;
 * 		}
 *
 * 		<ja>@Override</ja> <jk>public void</jk> complete() <jk>throws</jk> Exception {
 * 			<jv>rs</jv>.close(); <jv>conn</jv>.close();
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link BeanConsumer} - Parse-only lifecycle interface
 * 	<li class='jc'>{@link BeanChannel} - Round-trip lifecycle interface (extends both)
 * 	<li class='jc'>{@link BeanFactory} - Universal factory for DI framework integration
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Marshalling">Marshalling</a>
 * </ul>
 *
 * @param <T> The type of bean provided by this supplier.
 */
@SuppressWarnings({
	"java:S112" // throws Exception intentional - lifecycle methods may throw any checked exception
})
public interface BeanSupplier<T> extends Iterable<T> {

	/**
	 * Called before iteration starts.
	 *
	 * <p>
	 * Use this to open database connections, execute queries, or perform other setup.
	 *
	 * @throws Exception If setup fails.
	 */
	default void begin() throws Exception {}

	/**
	 * Always called after iteration ends (like {@code finally}).
	 *
	 * <p>
	 * Use this to close database cursors, connections, or release other resources.
	 * This method is called whether serialization succeeded or {@link #onError(Exception)} rethrew.
	 *
	 * @throws Exception If cleanup fails.
	 */
	default void complete() throws Exception {}

	/**
	 * Called when an exception occurs during iteration.
	 *
	 * <p>
	 * The default implementation rethrows the exception, stopping serialization immediately.
	 * Note that {@link #complete()} is always called afterward regardless.
	 *
	 * @param e The exception that occurred during iteration.
	 * @throws Exception If the error cannot be recovered from. Rethrow {@code e} to stop serialization.
	 */
	default void onError(Exception e) throws Exception {
		throw e;
	}
}
