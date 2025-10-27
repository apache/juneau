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
package org.apache.juneau.objecttools;

import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;

/**
 * POJO model sorter.
 *
 * <p>
 * 	This class is designed to sort arrays and collections of maps or beans.
 * </p>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	MyBean[] <jv>arrayOfBeans</jv> = ...;
 * 	ObjectSorter <jv>sorter</jv> = ObjectSorter.<jsm>create</jsm>();
 *
 * 	<jc>// Returns a list of beans sorted accordingly.</jc>
 * 	List&lt;MyBean&gt; <jv>result</jv> = <jv>sorter</jv>.run(<jv>arrayOfBeans</jv>, <js>"foo,bar-"</js>);
 * </p>
 * <p>
 * 	The tool can be used against the following data types:
 * </p>
 * <ul>
 * 	<li>Arrays/collections of maps or beans.
 * </ul>
 * <p>
 * 	The arguments are a simple comma-delimited list of property names optionally suffixed with <js>'+'</js> and <js>'-'</js> to
 * 	denote ascending/descending order.
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ObjectTools">Object Tools</a>

 * </ul>
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class ObjectSorter implements ObjectTool<SortArgs> {
	private static class SortEntry implements Comparable {
		Object o;
		ClassMeta<?> cm;
		BeanSession bs;

		Object sortVal;
		boolean isDesc;

		SortEntry(BeanSession bs, Object o) {
			this.o = o;
			this.bs = bs;
			this.cm = bs.getClassMetaForObject(o);
		}

		@Override
		public int compareTo(Object o) {
			if (isDesc)
				return compare(((SortEntry)o).sortVal, this.sortVal);
			return compare(this.sortVal, ((SortEntry)o).sortVal);
		}

		void setSort(String sortCol, boolean isDesc) {
			this.isDesc = isDesc;

			if (cm == null)
				sortVal = null;
			else if (cm.isMap())
				sortVal = ((Map)o).get(sortCol);
			else if (cm.isBean())
				sortVal = bs.toBeanMap(o).get(sortCol);
			else
				sortVal = null;
		}
	}

	/**
	 * Default reusable searcher.
	 */
	public static final ObjectSorter DEFAULT = new ObjectSorter();

	/**
	 * Static creator.
	 *
	 * @return A new {@link ObjectSorter} object.
	 */
	public static ObjectSorter create() {
		return new ObjectSorter();
	}

	@Override /* Overridden from ObjectTool */
	public Object run(BeanSession session, Object input, SortArgs args) {
		if (input == null)
			return null;

		// If sort or view isn't empty, then we need to make sure that all entries in the
		// list are maps.
		Map<String,Boolean> sort = args.getSort();

		if (sort.isEmpty())
			return input;

		ClassMeta type = session.getClassMetaForObject(input);

		if (! type.isCollectionOrArray())
			return input;

		ArrayList<SortEntry> l = null;

		if (type.isArray()) {
			int size = Array.getLength(input);
			l = listOfSize(size);
			for (int i = 0; i < size; i++)
				l.add(new SortEntry(session, Array.get(input, i)));
		} else /* isCollection() */ {
			Collection c = (Collection)input;
			l = listOfSize(c.size());
			List<SortEntry> l2 = l;
			c.forEach(x -> l2.add(new SortEntry(session, x)));
		}

		// We reverse the list and sort last to first.
		List<String> columns = toList(sort.keySet());
		Collections.reverse(columns);

		List<SortEntry> l3 = l;
		columns.forEach(c -> {
			final boolean isDesc = sort.get(c);
			l3.forEach(se -> se.setSort(c, isDesc));
			Collections.sort(l3);
		});

		List<Object> l2 = listOfSize(l.size());
		l.forEach(x -> l2.add(x.o));

		return l2;
	}

	/**
	 * Convenience method for executing the sorter.
	 *
	 * @param <R> The return type.
	 * @param input The input.
	 * @param sortArgs The sort arguments.  See {@link SortArgs} for format.
	 * @return A list of maps/beans matching the
	 */
	public <R> List<R> run(Object input, String sortArgs) {
		Object r = run(BeanContext.DEFAULT_SESSION, input, SortArgs.create(sortArgs));
		if (r instanceof List)
			return (List<R>)r;
		if (r instanceof Collection)
			return new ArrayList<R>((Collection)r);
		if (isArray(r))
			return Arrays.asList((R[])r);
		return null;
	}
}