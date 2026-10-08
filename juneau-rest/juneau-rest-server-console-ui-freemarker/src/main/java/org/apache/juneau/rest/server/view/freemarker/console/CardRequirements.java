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
package org.apache.juneau.rest.server.view.freemarker.console;

import java.util.*;

/**
 * The toolkit packs each card type requires, plus the per-card {@code requires=} check.
 *
 * <p>
 * Built-in mapping: {@code datatables} &rarr; {@code "datatables-glue"}.  Apps append more through
 * {@code ConsoleFreemarkerMixin.Builder.cardRequires(...)}.
 *
 * @since 10.0.0
 */
final class CardRequirements {

	private final Map<String,List<String>> byType;
	private final ToolkitPackRegistry packs;

	private CardRequirements(Builder b, ToolkitPackRegistry packs) {
		var m = new LinkedHashMap<String,List<String>>();
		b.byType.forEach((type, names) -> m.put(type, List.copyOf(names)));
		byType = Map.copyOf(m);
		this.packs = packs;
	}

	static Builder create() {
		return new Builder();
	}

	/**
	 * @param type The card type ({@code html}, {@code datatables}, {@code console-output}, {@code run-view}).
	 * @return The packs the type requires, in order.
	 */
	List<String> forType(String type) {
		return byType.getOrDefault(type, List.of());
	}

	/**
	 * The packs one card requires: its type's packs, then its {@code requires=} packs, each once.
	 *
	 * @param type The card type.
	 * @param id The card id, for the message.
	 * @param requires The card's {@code requires=} pack names.
	 * @return The pack names, in order.
	 * @throws IllegalArgumentException If a {@code requires=} name is not a registered pack.
	 */
	List<String> forCard(String type, String id, List<String> requires) {
		for (var name : requires)
			if (! packs.contains(name))
				throw new IllegalArgumentException(String.format("Unknown toolkit pack '%s' (<@card id='%s'> requires=).", name, id));
		var out = new LinkedHashSet<>(forType(type));
		out.addAll(requires);
		return List.copyOf(out);
	}

	static final class Builder {

		final Map<String,List<String>> byType = new LinkedHashMap<>();

		Builder() {
			add("datatables", ToolkitPackRegistry.PACK_DATATABLES_GLUE);
		}

		Builder add(String type, String...names) {
			byType.computeIfAbsent(type, k -> new ArrayList<>()).addAll(Arrays.asList(names));
			return this;
		}

		CardRequirements build(ToolkitPackRegistry packs) {
			byType.forEach((type, names) -> {
				for (var name : names)
					if (! packs.contains(name))
						throw new IllegalArgumentException(String.format("Unknown toolkit pack '%s' (cardRequires for type '%s').", name, type));
			});
			return new CardRequirements(this, packs);
		}
	}
}
