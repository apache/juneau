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
package org.apache.juneau.commons.svl.functions;

import java.util.*;

import org.apache.juneau.commons.svl.*;

/**
 * Structure-producing and collection functions: {@code ul}, {@code ol}, {@code block}, {@code table}.
 *
 * <p>
 * Each returns a {@link Structure} from {@link VarFunction#invokeValue}; the string form is {@link Structure#toString()}.
 * {@code ul}/{@code ol} treat a single {@link Collection} argument as the items list, otherwise the arguments are the items.
 * Consequently {@code ul(list1)} cannot express a single item that is itself a list; use {@code ul(x, y)} or wrap the
 * list in another list.
 *
 * <p>
 * A collection longer than the session's {@code maxCollectionSize} (items of {@code ul}/{@code ol}, header, rows and each
 * row of {@code table}) is cut to its first {@code maxCollectionSize} elements in lenient mode and throws in strict mode.
 *
 * <p>
 * {@code table(header, rows)} requires every row to have exactly as many cells as the header has columns, and no
 * null header element; otherwise a {@link VarResolverException} is thrown.
 */
@SuppressWarnings({
	"java:S1192" // Duplicated function-name literals read more clearly inline than as constants
})
public final class StructureFunctions {

	private StructureFunctions() {}

	/** All function classes in this category. */
	@SuppressWarnings({
		"java:S2386", // ALL is an immutable compile-time registry; exposed as an array for the cross-package/varargs functions(...) API.
		"unchecked" // Cast is safe: type verified by caller context.
	})
	public static final Class<? extends VarFunction>[] ALL = new Class[] {
		Ul.class, Ol.class, Block.class, Table.class
	};

	/** Base class: string form is the structure's text. */
	abstract static class Base implements VarFunction {
		private final String name;
		private final int min;
		private final int max;

		Base(String name, int min, int max) {
			this.name = name;
			this.min = min;
			this.max = max;
		}

		@Override public String name() { return name; }
		@Override public int minArity() { return min; }
		@Override public int maxArity() { return max; }

		@Override
		public String invoke(VarResolverSession session, List<Object> args) {
			return String.valueOf(invokeValue(session, args));
		}

		@Override
		public abstract Object invokeValue(VarResolverSession session, List<Object> args);
	}

	private static List<?> itemsOf(VarResolverSession session, String fn, List<Object> args) {
		if (args.size() == 1 && args.get(0) instanceof Collection<?> c)
			return CollectionFunctions.bounded(session, fn, c);
		return CollectionFunctions.bounded(session, fn, args);
	}

	/** {@code #{ul(items...)}} or {@code #{ul(list)}}. */
	public static class Ul extends Base {
		/** Constructor. */
		public Ul() { super("ul", 1, Integer.MAX_VALUE); }
		@Override public Object invokeValue(VarResolverSession session, List<Object> args) { return Structure.ul(itemsOf(session, "ul", args)); }
	}

	/** {@code #{ol(items...)}} or {@code #{ol(list)}}. */
	public static class Ol extends Base {
		/** Constructor. */
		public Ol() { super("ol", 1, Integer.MAX_VALUE); }
		@Override public Object invokeValue(VarResolverSession session, List<Object> args) { return Structure.ol(itemsOf(session, "ol", args)); }
	}

	/** {@code #{block(x)}}. */
	public static class Block extends Base {
		/** Constructor. */
		public Block() { super("block", 1, 1); }
		@Override public Object invokeValue(VarResolverSession session, List<Object> args) { return Structure.block(args.get(0)); }
	}

	/** {@code #{table(headerList, rowsList)}}; each row is a list (or collection). */
	public static class Table extends Base {
		/** Constructor. */
		public Table() { super("table", 2, 2); }
		private static List<?> listArg(VarResolverSession session, String fn, int idx, Object o) {
			if (o instanceof Collection<?> c)
				return CollectionFunctions.bounded(session, fn, c);
			throw new VarResolverException("Function '%s' argument %s must be a list but got %s", fn, idx + 1, o == null ? "null" : o.getClass().getSimpleName());
		}
		@Override public Object invokeValue(VarResolverSession session, List<Object> args) {
			var cols = new ArrayList<String>();
			for (var o : listArg(session, "table", 0, args.get(0))) {
				if (o == null)
					throw new VarResolverException("Function 'table' header must not contain null");
				cols.add(String.valueOf(o));
			}
			var rows = new ArrayList<List<?>>();
			int n = 0;
			for (var r : listArg(session, "table", 1, args.get(1))) {
				var row = listArg(session, "table", 1, r);
				if (row.size() != cols.size())
					throw new VarResolverException("Function 'table' row %s has %s cells but the header has %s columns", n, row.size(), cols.size());
				rows.add(row);
				n++;
			}
			return Structure.table(cols, rows);
		}
	}
}
