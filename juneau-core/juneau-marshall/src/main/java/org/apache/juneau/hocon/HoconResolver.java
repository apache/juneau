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
package org.apache.juneau.hocon;

import static org.apache.juneau.commons.utils.ThrowableUtils.rex;

import java.util.*;
import java.util.function.Function;

/**
 * Resolves ${var} and ${?var} substitutions in the parsed HOCON tree.
 *
 * <p>
 * Resolution algorithm: single-pass depth-first over the tree. Substitutions reference paths in the
 * root object (e.g., ${database.host}). Optional substitutions (${?var}) are silently removed if
 * the path does not exist. Required substitutions (${var}) throw if the path is missing.
 * Circular references are detected and cause an error.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://github.com/lightbend/config/blob/main/HOCON.md">HOCON Specification</a>
 * </ul>
 */
public class HoconResolver {

	private final HoconValue.HoconObject root;
	private final Set<String> inProgress = new HashSet<>();

	/**
	 * Constructor.
	 *
	 * @param root The root HOCON object (after parsing, before resolution).
	 */
	public HoconResolver(HoconValue.HoconObject root) {
		this.root = root;
	}

	/**
	 * Resolves all substitutions in-place in the tree.
	 *
	 * @throws RuntimeException If a required substitution path is missing or circular.
	 */
	public void resolve() {
		resolveObject(root);
	}

	private void resolveObject(HoconValue.HoconObject obj) {
		var it = obj.getMembers().entrySet().iterator();
		while (it.hasNext()) {
			var e = it.next();
			var resolved = resolveValue(e.getValue());
			if (resolved == null)
				it.remove();
			else
				e.setValue(resolved);
		}
	}

	private HoconValue resolveValue(HoconValue val) {
		if (val == null)
			return null;
		return switch (val.getType()) {
			case STRING, NUMBER, BOOLEAN, NULL -> val;
			case OBJECT -> {
				resolveObject((HoconValue.HoconObject) val);
				yield val;
			}
			case ARRAY -> {
				var arr = (HoconValue.HoconArray) val;
				var elements = arr.getElements();
				var toRemove = new ArrayList<Integer>();
				for (var i = 0; i < elements.size(); i++) {
					var resolved = resolveValue(elements.get(i));
					if (resolved == null)
						toRemove.add(i);
					else
						elements.set(i, resolved);
				}
				for (var i = toRemove.size() - 1; i >= 0; i--)
					elements.remove(toRemove.get(i).intValue());
				yield val;
			}
			case SUBSTITUTION -> resolveSubstitution((HoconValue.HoconSubstitution) val, false);
			case OPT_SUBSTITUTION -> resolveSubstitution((HoconValue.HoconSubstitution) val, true);
			case CONCAT -> resolveConcat((HoconValue.HoconConcat) val);
			default -> val;
		};
	}

	private HoconValue resolveSubstitution(HoconValue.HoconSubstitution sub, boolean optional) {
		var path = sub.getPath();
		if (inProgress.contains(path))
			throw rex("Circular substitution: {0}", path);
		inProgress.add(path);
		try {
			var found = lookup(path);
			if (found == null) {
				if (optional)
					return null;
				throw rex("Substitution ''${0}'' not found", path);
			}
			var resolved = resolveValue(found);
			if (resolved != null)
				return resolved;
			return optional ? null : found;
		} finally {
			inProgress.remove(path);
		}
	}

	private HoconValue resolveConcat(HoconValue.HoconConcat concat) {
		var sb = new StringBuilder();
		for (var p : concat.getParts()) {
			var resolved = resolveValue(p);
			if (resolved != null)
				sb.append(resolveConcatPart(resolved));
		}
		return new HoconValue.HoconString(sb.toString());
	}

	private static String resolveConcatPart(HoconValue val) {
		if (val == null)
			return "";
		return switch (val.getType()) {
			case STRING -> ((HoconValue.HoconString) val).getValue();
			case NUMBER -> ((HoconValue.HoconNumber) val).getValue().toString();
			case BOOLEAN -> String.valueOf(((HoconValue.HoconBoolean) val).getValue());
			case NULL -> "null";
			default -> val.toString();
		};
	}

	/**
	 * Looks up a dotted path in the root object.
	 *
	 * @param path The path (e.g., "database.host").
	 * @return The value at that path, or <jk>null</jk>.
	 */
	public HoconValue lookup(String path) {
		if (path == null || path.isEmpty())
			return null;
		var parts = path.split("\\.", -1);
		return root.getPath(parts);
	}

	/**
	 * Resolves a concatenation value with a custom lookup (for self-referential substitution).
	 * When the concat contains ${path}, the lookup is used instead of the root object.
	 * This allows <code>path = ${path}"/bin"</code> to append to the previous value.
	 *
	 * @param concat The concatenation to resolve.
	 * @param lookup Custom lookup; for path X returns the value to use (e.g. previous value for self-ref).
	 * @return The resolved string value.
	 */
	public HoconValue resolveConcatWithLookup(HoconValue.HoconConcat concat, Function<String, HoconValue> lookup) {
		var sb = new StringBuilder();
		for (var p : concat.getParts()) {
			var resolved = resolveValueWithLookup(p, lookup);
			if (resolved != null)
				sb.append(resolveConcatPart(resolved));
		}
		return new HoconValue.HoconString(sb.toString());
	}

	private HoconValue resolveValueWithLookup(HoconValue val, Function<String, HoconValue> lookup) {
		if (val == null)
			return null;
		return switch (val.getType()) {
			case STRING, NUMBER, BOOLEAN, NULL -> val;
			case OBJECT -> {
				resolveObject((HoconValue.HoconObject) val);
				yield val;
			}
			case ARRAY -> {
				var arr = (HoconValue.HoconArray) val;
				for (var i = 0; i < arr.getElements().size(); i++)
					arr.getElements().set(i, resolveValueWithLookup(arr.getElements().get(i), lookup));
				yield val;
			}
			case SUBSTITUTION -> resolveSubstitutionWithLookup((HoconValue.HoconSubstitution) val, false, lookup);
			case OPT_SUBSTITUTION -> resolveSubstitutionWithLookup((HoconValue.HoconSubstitution) val, true, lookup);
			case CONCAT -> resolveConcatWithLookup((HoconValue.HoconConcat) val, lookup);
			default -> val;
		};
	}

	private HoconValue resolveSubstitutionWithLookup(HoconValue.HoconSubstitution sub, boolean optional,
			Function<String, HoconValue> lookup) {
		var path = sub.getPath();
		if (inProgress.contains(path))
			throw rex("Circular substitution: {0}", path);
		inProgress.add(path);
		try {
			var found = lookup.apply(path);
			if (found == null) {
				if (optional)
					return null;
				throw rex("Substitution ''${0}'' not found", path);
			}
			var resolved = resolveValueWithLookup(found, lookup);
			if (resolved != null)
				return resolved;
			return optional ? null : found;
		} finally {
			inProgress.remove(path);
		}
	}

}
