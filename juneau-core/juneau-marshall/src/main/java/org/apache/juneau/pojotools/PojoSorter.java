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
package org.apache.juneau.pojotools;

import static org.apache.juneau.internal.CollectionUtils.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;

/**
 * Sorts arrays and collections of maps and beans.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc jm.PojoTools}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
@SuppressWarnings({"unchecked","rawtypes"})
public final class PojoSorter implements PojoTool<SortArgs> {

	@Override /* PojoTool */
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
			l = list(size);
			for (int i = 0; i < size; i++)
				l.add(new SortEntry(session, Array.get(input, i)));
		} else /* isCollection() */ {
			Collection c = (Collection)input;
			l = list(c.size());
			List<SortEntry> l2 = l;
			c.forEach(x -> l2.add(new SortEntry(session, x)));
		}

		// We reverse the list and sort last to first.
		List<String> columns = listFrom(sort.keySet());
		Collections.reverse(columns);

		List<SortEntry> l3 = l;
		columns.forEach(c -> {
			final boolean isDesc = sort.get(c);
			l3.forEach(se -> se.setSort(c, isDesc));
			Collections.sort(l3);
		});

		List<Object> l2 = list(l.size());
		l.forEach(x -> l2.add(x.o));

		return l2;
	}

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

		@Override
		public int compareTo(Object o) {
			if (isDesc)
				return ObjectUtils.compare(((SortEntry)o).sortVal, this.sortVal);
			return ObjectUtils.compare(this.sortVal, ((SortEntry)o).sortVal);
		}
	}
}