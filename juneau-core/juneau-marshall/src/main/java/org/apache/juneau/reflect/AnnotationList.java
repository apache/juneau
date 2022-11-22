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
package org.apache.juneau.reflect;

import static org.apache.juneau.internal.ConsumerUtils.*;

import java.lang.annotation.*;
import java.util.*;
import java.util.function.*;

/**
 * An ordered list of annotations and the classes/methods/packages they were found on.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 *
 * @serial exclude
 */
public final class AnnotationList extends ArrayList<AnnotationInfo<?>> {
	private static final long serialVersionUID = 1L;

	private static final Comparator<AnnotationInfo<?>> RANK_COMPARATOR = new Comparator<>() {
		@Override
		public int compare(AnnotationInfo<?> o1, AnnotationInfo<?> o2) {
			return o1.rank - o2.rank;
		}
	};

	/**
	 * Sort the annotations in this list based on rank.
	 *
	 * @return This object.
	 */
	public AnnotationList sort() {
		Collections.sort(this, RANK_COMPARATOR);
		return this;
	}

	/**
	 * Performs an action on the specified matching values from all annotations in this list.
	 *
	 * @param <T> The annotation value type.
	 * @param type The annotation value type.
	 * @param name The annotation value name.
	 * @param filter A predicate to apply to the value to determine if action should be performed.  Can be <jk>null</jk>.
	 * @param action An action to perform on the value.
	 * @return This object.
	 */
	public <T> AnnotationList forEachValue(Class<T> type, String name, Predicate<T> filter, Consumer<T> action) {
		forEach(x -> x.forEachValue(type, name, filter, action));
		return this;
	}

	/**
	 * Performs an action on all matching annotations in this list.
	 *
	 * @param <A> The annotation type.
	 * @param type The annotation type.
	 * @param filter A predicate to apply to the entries to determine if action should be performed.  Can be <jk>null</jk>.
	 * @param action An action to perform on the entry.
	 * @return This object.
	 */
	@SuppressWarnings("unchecked")
	public <A extends Annotation> AnnotationList forEach(Class<A> type, Predicate<AnnotationInfo<A>> filter, Consumer<AnnotationInfo<A>> action) {
		forEach(x -> {
			if (x.isType(type))
				consume(filter, action, (AnnotationInfo<A>)x);
		});
		return this;
	}

	/**
	 * Performs an action on all matching annotations in this list.
	 *
	 * @param <A> The annotation type.
	 * @param filter A predicate to apply to the entries to determine if action should be performed.  Can be <jk>null</jk>.
	 * @param action An action to perform on the entry.
	 * @return This object.
	 */
	public <A extends Annotation> AnnotationList forEach(Predicate<AnnotationInfo<?>> filter, Consumer<AnnotationInfo<?>> action) {
		forEach(x -> consume(filter, action, x));
		return this;
	}
}
