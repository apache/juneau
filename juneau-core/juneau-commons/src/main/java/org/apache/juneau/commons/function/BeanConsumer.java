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
 * A lifecycle-aware consumer for use as a parser target, receiving deserialized beans one at a time.
 *
 * <p>
 * Extends {@link ThrowingConsumer} so that {@link #acceptThrows(Object)} supports checked exceptions
 * directly — no wrapping in {@link RuntimeException} required for operations such as JDBC inserts.
 *
 * <p>
 * The parser drives the full lifecycle:
 * <ol>
 * 	<li>Calls {@link #begin()} before the first element
 * 	<li>Calls {@link #acceptThrows(Object)} for each parsed element
 * 	<li>If {@link #acceptThrows(Object)} throws, calls {@link #onError(Exception)}:
 * 	    <ul>
 * 	    	<li>If {@code onError()} absorbs the exception (doesn't rethrow), parsing continues to the
 * 	    	    next element (skip-and-continue / fault-tolerant ingestion)
 * 	    	<li>If {@code onError()} rethrows, parsing stops and the exception propagates
 * 	    </ul>
 * 	<li>Always calls {@link #complete()} after all elements (like {@code finally}) — whether parsing
 * 	    succeeded or {@code onError()} rethrew. Use this for resource cleanup (close connections, etc.)
 * </ol>
 *
 * <p>
 * For round-trip support (same property used for both serialization and parsing), use
 * {@link BeanChannel} instead, which extends both {@link BeanConsumer} and {@link BeanSupplier}.
 *
 * <p>
 * If a {@link BeanConsumer} is encountered during serialization (not parsing), the serializer will
 * throw an {@link IllegalArgumentException} with a message recommending {@link BeanSupplier} or
 * {@link BeanChannel}.
 *
 * <h5 class='section'>Example (DB-backed consumer with batch commits):</h5>
 * <p class='bjava'>
 * 	<ja>@Bean</ja>(factory=ItemConsumerFactory.<jk>class</jk>)
 * 	<jk>public class</jk> ItemConsumer <jk>implements</jk> BeanConsumer&lt;Item&gt; {
 * 		<jk>private</jk> Connection <jv>conn</jv>;
 * 		<jk>private</jk> PreparedStatement <jv>stmt</jv>;
 * 		<jk>private int</jk> <jv>count</jv>;
 *
 * 		<ja>@Override</ja> <jk>public void</jk> begin() <jk>throws</jk> Exception {
 * 			<jv>conn</jv> = <jv>ds</jv>.getConnection();
 * 			<jv>conn</jv>.setAutoCommit(<jk>false</jk>);
 * 			<jv>stmt</jv> = <jv>conn</jv>.prepareStatement(<js>"INSERT INTO items (name) VALUES (?)"</js>);
 * 		}
 *
 * 		<ja>@Override</ja> <jk>public void</jk> acceptThrows(Item <jv>item</jv>) <jk>throws</jk> Exception {
 * 			<jv>stmt</jv>.setString(1, <jv>item</jv>.getName());
 * 			<jv>stmt</jv>.executeUpdate();
 * 			<jk>if</jk> (++<jv>count</jv> % 500 == 0) <jv>conn</jv>.commit(); <jc>// batch commit</jc>
 * 		}
 *
 * 		<ja>@Override</ja> <jk>public void</jk> onError(Exception <jv>e</jv>) <jk>throws</jk> Exception {
 * 			<jv>conn</jv>.rollback(); <jk>throw</jk> <jv>e</jv>;
 * 		}
 *
 * 		<ja>@Override</ja> <jk>public void</jk> complete() <jk>throws</jk> Exception {
 * 			<jv>conn</jv>.commit(); <jv>stmt</jv>.close(); <jv>conn</jv>.close();
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link BeanSupplier} - Serialize-only lifecycle interface
 * 	<li class='jc'>{@link BeanChannel} - Round-trip lifecycle interface (extends both)
 * 	<li class='jc'>{@link BeanFactory} - Universal factory for DI framework integration
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Marshalling">Marshalling</a>
 * </ul>
 *
 * @param <T> The type of bean received by this consumer.
 */
@SuppressWarnings({
	"java:S112" // throws Exception intentional - lifecycle methods may throw any checked exception
})
public interface BeanConsumer<T> extends ThrowingConsumer<T> {

	/**
	 * Called before the first element is accepted.
	 *
	 * <p>
	 * Use this to open database connections, prepare statements, or perform other setup.
	 *
	 * @throws Exception If setup fails.
	 */
	default void begin() throws Exception {}

	/**
	 * Always called after all elements have been processed (like {@code finally}).
	 *
	 * <p>
	 * Use this to close database connections, statements, or release other resources.
	 * This method is called whether parsing succeeded or {@link #onError(Exception)} rethrew.
	 *
	 * @throws Exception If cleanup fails.
	 */
	default void complete() throws Exception {}

	/**
	 * Called when {@link #acceptThrows(Object)} throws an exception.
	 *
	 * <p>
	 * The default implementation rethrows the exception, stopping parsing immediately.
	 * Override to absorb the exception for fault-tolerant ingestion (e.g., log and skip bad records).
	 * Note that {@link #complete()} is always called afterward regardless.
	 *
	 * @param e The exception thrown by {@link #acceptThrows(Object)}.
	 * @throws Exception If the error cannot be recovered from. Rethrow {@code e} to stop parsing.
	 */
	default void onError(Exception e) throws Exception {
		throw e;
	}
}
