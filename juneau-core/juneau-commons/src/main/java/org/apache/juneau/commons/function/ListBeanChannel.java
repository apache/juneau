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

import java.util.*;

/**
 * A simple in-memory {@link BeanChannel} implementation backed by an {@link ArrayList}.
 *
 * <p>
 * Collects parsed elements into an in-memory list on the parser side, and iterates over the list
 * on the serializer side. No factory or DI framework required — instantiated directly via no-arg
 * constructor.
 *
 * <p>
 * This is the default built-in implementation suitable for use cases where holding all elements
 * in memory is acceptable. For large datasets or database-backed scenarios, implement
 * {@link BeanChannel} (or {@link BeanConsumer} / {@link BeanSupplier}) directly.
 *
 * <h5 class='section'>Example (getter-only round-trip property):</h5>
 * <p class='bjava'>
 * 	<jk>public class</jk> ItemCollection {
 * 		<jk>private final</jk> ListBeanChannel&lt;Item&gt; <jv>items</jv> = <jk>new</jk> ListBeanChannel&lt;&gt;();
 *
 * 		<ja>@Beanp</ja>(elementType=Item.<jk>class</jk>)
 * 		<jk>public</jk> ListBeanChannel&lt;Item&gt; getItems() { <jk>return</jk> <jv>items</jv>; }
 * 		<jc>// No setter needed — parser calls acceptThrows() on the existing instance</jc>
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link BeanChannel} - Round-trip lifecycle interface
 * 	<li class='jc'>{@link BeanConsumer} - Parse-only lifecycle interface
 * 	<li class='jc'>{@link BeanSupplier} - Serialize-only lifecycle interface
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Marshalling">Marshalling</a>
 * </ul>
 *
 * @param <T> The type of bean stored in this channel.
 */
public class ListBeanChannel<T> implements BeanChannel<T> {

	private final List<T> list = new ArrayList<>();

	/**
	 * Constructor.
	 */
	public ListBeanChannel() {}

	@Override
	public void acceptThrows(T item) {
		list.add(item);
	}

	@Override
	public Iterator<T> iterator() {
		return list.iterator();
	}

	/**
	 * Returns the underlying list of elements.
	 *
	 * @return The list of elements collected during parsing, or to be serialized.
	 */
	public List<T> getList() {
		return list;
	}
}
