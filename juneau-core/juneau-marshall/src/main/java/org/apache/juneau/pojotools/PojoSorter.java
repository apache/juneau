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

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;

/**
 * Sorts arrays and collections of maps and beans.
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
			l = new ArrayList<>(size);
			for (int i = 0; i < size; i++)
				l.add(new SortEntry(session, Array.get(input, i)));
		} else /* isCollection() */ {
			Collection c = (Collection)input;
			l = new ArrayList<>(c.size());
			for (Object o : c)
				l.add(new SortEntry(session, o));
		}

		// We reverse the list and sort last to first.
		List<String> columns = new ArrayList<>(sort.keySet());
		Collections.reverse(columns);

		for (final String c : columns) {
			final boolean isDesc = sort.get(c);
			for (SortEntry se : l)
				se.setSort(c, isDesc);
			Collections.sort(l);
		}

		ArrayList<Object> l2 = new ArrayList<>(l.size());
		for (SortEntry se : l)
			l2.add(se.o);

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