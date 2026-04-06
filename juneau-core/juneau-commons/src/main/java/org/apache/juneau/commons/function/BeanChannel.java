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
 * A lifecycle-aware round-trip interface that combines both {@link BeanSupplier} (for serialization)
 * and {@link BeanConsumer} (for parsing) on the same property or class.
 *
 * <p>
 * Use this interface when the same object needs to:
 * <ul>
 * 	<li>Provide beans to a serializer (via {@link #iterator()})
 * 	<li>Receive beans from a parser (via {@link #acceptThrows(Object)})
 * </ul>
 *
 * <p>
 * The lifecycle methods ({@link #begin()}, {@link #complete()}, {@link #onError(Exception)}) are
 * shared across both directions. The implementation implicitly knows its direction from which data
 * method is called first after {@link #begin()}:
 * <ul>
 * 	<li>{@link #iterator()} called first → read mode (serialization)
 * 	<li>{@link #acceptThrows(Object)} called first → write mode (parsing)
 * </ul>
 *
 * <p>
 * The framework drives the lifecycle:
 * <ol>
 * 	<li>Calls {@link #begin()} before the first data operation
 * 	<li>Reads via {@link #iterator()} (serializer) or writes via {@link #acceptThrows(Object)} (parser)
 * 	<li>If an exception occurs, calls {@link #onError(Exception)}
 * 	<li>Always calls {@link #complete()} at the end (like {@code finally})
 * </ol>
 *
 * <p>
 * Since {@code BeanChannel} extends {@link BeanConsumer}, a {@code BeanChannel} instance passes
 * the parser's {@code instanceof BeanConsumer} check. Since it extends {@link BeanSupplier}, it
 * also passes the serializer's {@code instanceof BeanSupplier} check — both directions work.
 *
 * <h5 class='section'>Example (DB-backed round-trip channel):</h5>
 * <p class='bjava'>
 * 	<ja>@Bean</ja>(factory=ItemChannelFactory.<jk>class</jk>)
 * 	<jk>public class</jk> ItemChannel <jk>implements</jk> BeanChannel&lt;Item&gt; {
 * 		<jk>private</jk> Connection <jv>conn</jv>;
 * 		<jk>private boolean</jk> <jv>writeMode</jv>;
 *
 * 		<ja>@Override</ja> <jk>public void</jk> begin() <jk>throws</jk> Exception {
 * 			<jv>conn</jv> = <jv>ds</jv>.getConnection();
 * 			<jv>conn</jv>.setAutoCommit(<jk>false</jk>);
 * 		}
 *
 * 		<jc>// Serializer calls iterator() — read mode</jc>
 * 		<ja>@Override</ja> <jk>public</jk> Iterator&lt;Item&gt; iterator() {
 * 			<jk>return new</jk> ResultSetIterator&lt;&gt;(...);
 * 		}
 *
 * 		<jc>// Parser calls acceptThrows() — write mode</jc>
 * 		<ja>@Override</ja> <jk>public void</jk> acceptThrows(Item <jv>item</jv>) <jk>throws</jk> Exception {
 * 			<jv>writeMode</jv> = <jk>true</jk>;
 * 			<jc>// insert item...</jc>
 * 		}
 *
 * 		<ja>@Override</ja> <jk>public void</jk> onError(Exception <jv>e</jv>) <jk>throws</jk> Exception {
 * 			<jk>if</jk> (<jv>writeMode</jv>) <jv>conn</jv>.rollback();
 * 			<jk>throw</jk> <jv>e</jv>;
 * 		}
 *
 * 		<ja>@Override</ja> <jk>public void</jk> complete() <jk>throws</jk> Exception {
 * 			<jk>if</jk> (<jv>writeMode</jv>) <jv>conn</jv>.commit();
 * 			<jv>conn</jv>.close();
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link BeanConsumer} - Parse-only lifecycle interface
 * 	<li class='jc'>{@link BeanSupplier} - Serialize-only lifecycle interface
 * 	<li class='jc'>{@link BeanFactory} - Universal factory for DI framework integration
 * 	<li class='jc'>{@link ListBeanChannel} - Simple in-memory implementation
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Marshalling">Marshalling</a>
 * </ul>
 *
 * @param <T> The type of bean read and written by this channel.
 */
@SuppressWarnings({
	"java:S112" // throws Exception intentional - lifecycle methods may throw any checked exception
})
public interface BeanChannel<T> extends BeanSupplier<T>, BeanConsumer<T> {

	@Override
	default void begin() throws Exception {}

	@Override
	default void complete() throws Exception {}

	@Override
	default void onError(Exception e) throws Exception {
		throw e;
	}
}
