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

import java.lang.annotation.*;
import java.util.*;
import java.util.function.*;

/**
 * An ordered list of annotations and the classes/methods/packages they were found on.
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@source}
 * </ul>
 *
 * @serial exclude
 */
public class AnnotationList extends ArrayList<AnnotationInfo<?>> {
	private static final long serialVersionUID = 1L;

	private static final Comparator<AnnotationInfo<?>> RANK_COMPARATOR = new Comparator<AnnotationInfo<?>>() {
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
	 * Returns the specified values from all annotations in this list.
	 *
	 * @param type The annotation value type.
	 * @param name The annotation value name.
	 * @param predicate The predicate.
	 * @param consumer The consumer.
	 * @return This object.
	 */
	public <T> AnnotationList getValues(Class<T> type, String name, Predicate<T> predicate, Consumer<T> consumer) {
		for (AnnotationInfo<?> ai : this)
			ai.getValue(type, name, predicate, consumer);
		return this;
	}

	/**
	 * Consumes all annotations in this list that match the specified type and predicate.
	 *
	 * @param a The annotation type.
	 * @param predicate The predicate.
	 * @param consumer The consumer.
	 * @return This object.
	 */
	@SuppressWarnings("unchecked")
	public <A extends Annotation> AnnotationList forEach(Class<A> a, Predicate<AnnotationInfo<A>> predicate, Consumer<AnnotationInfo<A>> consumer) {
		for (AnnotationInfo<?> ai : this)
			if (ai.isType(a) && predicate.test((AnnotationInfo<A>)ai))
				consumer.accept((AnnotationInfo<A>)ai);
		return this;
	}

	/**
	 * Consumes all annotations in this list that match the specified predicate.
	 *
	 * @param predicate The predicate.
	 * @param consumer The consumer.
	 * @return This object.
	 */
	public <A extends Annotation> AnnotationList forEach(Predicate<AnnotationInfo<?>> predicate, Consumer<AnnotationInfo<?>> consumer) {
		for (AnnotationInfo<?> ai : this)
			if (predicate.test(ai))
				consumer.accept(ai);
		return this;
	}
}
