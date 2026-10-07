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
package org.apache.juneau.petstore.console.dev.flavors;

import java.util.*;

import org.apache.juneau.petstore.dto.*;
import org.apache.juneau.petstore.service.*;

/**
 * The pet rows every rendering flavor shows: the first {@value #LIMIT} pets by id, with display-ready strings.
 *
 * <p>
 * Formatting happens here, once, so the three engines differ only in markup and the flavor tests can compare their
 * cells exactly.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	List&lt;Map&lt;String,String&gt;&gt; <jv>rows</jv> = FlavorPets.<jsm>rows</jsm>(<jv>store</jv>);
 * 	<jv>rows</jv>.get(0).get(<js>"name"</js>);   <jc>// "Mr. Frisky"</jc>
 * </p>
 */
public final class FlavorPets {

	/** How many pets each flavor shows. */
	public static final int LIMIT = 10;

	private FlavorPets() {}

	/**
	 * Returns the display rows.
	 *
	 * @param store The store.  Must not be <jk>null</jk>.
	 * @return Rows with keys {@code name}, {@code species}, {@code price} (two decimals) and {@code status}.
	 */
	public static List<Map<String,String>> rows(PetStore store) {
		return store.getPets().stream()
			.sorted(Comparator.comparingLong(Pet::getId))
			.limit(LIMIT)
			.map(FlavorPets::row)
			.toList();
	}

	private static Map<String,String> row(Pet p) {
		var m = new LinkedHashMap<String,String>();
		m.put("name", p.getName());
		m.put("species", String.valueOf(p.getSpecies()));
		m.put("price", String.format(Locale.ROOT, "%.2f", p.getPrice()));
		m.put("status", String.valueOf(p.getStatus()));
		return m;
	}
}
