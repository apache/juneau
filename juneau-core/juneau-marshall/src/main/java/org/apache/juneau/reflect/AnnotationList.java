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

import static org.apache.juneau.internal.ThrowableUtils.*;

import java.lang.annotation.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.svl.*;

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

	private final Predicate<AnnotationInfo<?>> filter;

	/**
	 * Constructor with optional filter.
	 *
	 * @param filter The filter to use to filter entries added to this list.
	 */
	public AnnotationList(Predicate<AnnotationInfo<?>> filter) {
		this.filter = filter;
	}

	@Override /* List */
	public boolean add(AnnotationInfo<?> ai) {
		if (filter == null || filter.test(ai)) {
			super.add(ai);
			return true;
		}
		return false;
	}

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
	 * @return A list of all values found.
	 */
	public <T> List<T> getValues(Class<T> type, String name) {
		List<T> l = new ArrayList<>();
		for (AnnotationInfo<?> ai : this) {
			Optional<T> o = ai.getValue(type, name);
			if (o.isPresent())
				l.add(o.get());
		}
		return l;
	}

	/**
	 * Filters this list using the specified test.
	 *
	 * @param test The test to use to filter this list.
	 * @return A new list containing only the filtered elements.
	 */
	public AnnotationList filter(Predicate<AnnotationInfo<?>> test) {
		AnnotationList al = new AnnotationList(null);
		stream().filter(test).forEach(x->al.add(x));
		return al;
	}

	/**
	 * Takes the annotations in this list and produces a list of {@link AnnotationWork} objects to be applied to context builders.
	 *
	 * @param vrs The variable resolver session that gets passed in to the constructor of the {@link AnnotationApplier} objects that get created.
	 * @return A list of {@link AnnotationWork} objects.
	 */
	public AnnotationWorkList getWork(VarResolverSession vrs) {
		try {
			AnnotationWorkList l = new AnnotationWorkList();
			for (AnnotationInfo<?> ai : sort())
				for (AnnotationApplier<Annotation,Object> aa : ai.getApplies(vrs))
					l.add(ai, aa);
			return l;
		} catch (ExecutableException e) {
			throw runtimeException(e.getCause());
		}
	}
}
