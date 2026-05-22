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
package org.apache.juneau.bean.rfc7807;

import java.util.*;

/**
 * Aggregator bean for registering multiple {@link ProblemMapper}s with a single bean-store entry.
 *
 * <p>
 * The Juneau {@code @Bean} bean-store walk pairs each {@code @Bean} method with its declared return type
 * (via the underlying {@code BeanStore.createBeanFromMethod(Class, ...)} lookup), which means multiple
 * {@code @Bean public ProblemMapper foo()} factory methods on the same resource class collide on the
 * {@code ProblemMapper.class} key. To register more than one mapper, declare a single
 * {@code @Bean public ProblemMapperList mappers()} factory and let the list carry the dispatch order:
 *
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(problemDetails=<js>"true"</js>)
 * 	<jk>public class</jk> MyResource {
 *
 * 		<ja>@Bean</ja> <jk>public</jk> ProblemMapperList problemMappers() {
 * 			<jk>return</jk> ProblemMapperList.<jsm>of</jsm>(<jk>new</jk> InsufficientCreditMapper(), <jk>new</jk> NotFoundMapper());
 * 		}
 * 	}
 * </p>
 *
 * <p>
 * For the single-mapper case the conventional pattern still works &mdash;
 * {@code @Bean public ProblemMapper<MyException> mapper()} registers under {@code ProblemMapper.class}
 * and the server-side processor picks it up via {@code BeanStore.getBean(ProblemMapper.class)} without
 * needing a list wrapper.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link ProblemMapper}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerProblemDetails">REST Server &mdash; RFC 7807 problem-details</a>
 * </ul>
 */
public class ProblemMapperList {

	private final List<ProblemMapper<?>> mappers;

	/**
	 * Creates an empty list. Mappers can be added via {@link #append(ProblemMapper)} /
	 * {@link #append(Collection)}.
	 */
	public ProblemMapperList() {
		mappers = new ArrayList<>();
	}

	/**
	 * Creates a list populated with the supplied mappers in argument order.
	 *
	 * @param mappers The mappers to add. Each must be non-{@code null}; duplicates are allowed and preserved.
	 * @return A new {@link ProblemMapperList} containing the supplied mappers.
	 */
	public static ProblemMapperList of(ProblemMapper<?>... mappers) {
		var l = new ProblemMapperList();
		if (mappers != null)
			for (var m : mappers)
				if (m != null)
					l.mappers.add(m);
		return l;
	}

	/**
	 * Appends a mapper to the end of the list.
	 *
	 * @param mapper The mapper to add. Must not be <jk>null</jk>.
	 * @return This object for fluent chaining.
	 */
	public ProblemMapperList append(ProblemMapper<?> mapper) {
		if (mapper != null)
			mappers.add(mapper);
		return this;
	}

	/**
	 * Appends a collection of mappers to the end of the list, in iteration order.
	 *
	 * @param values The mappers to add. {@code null} entries are skipped.
	 * @return This object for fluent chaining.
	 */
	public ProblemMapperList append(Collection<? extends ProblemMapper<?>> values) {
		if (values != null)
			for (var m : values)
				if (m != null)
					mappers.add(m);
		return this;
	}

	/**
	 * Returns the registered mappers in their declared order.
	 *
	 * @return An unmodifiable view of the underlying list. Never {@code null}.
	 */
	public List<ProblemMapper<?>> asList() {
		return Collections.unmodifiableList(mappers);
	}

	/**
	 * Returns {@code true} when this list contains no mappers.
	 *
	 * @return {@code true} when {@link #asList()} is empty.
	 */
	public boolean isEmpty() {
		return mappers.isEmpty();
	}
}
