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
package org.apache.juneau.petstore.console.dev;

import java.util.*;
import java.util.stream.*;

import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.petstore.dto.*;
import org.apache.juneau.rest.server.console.*;

/**
 * The server half of the Custom cards page's validated card type: an inventory gauge for one species.
 *
 * <p>
 * The JSON5 body is {@code {species, low, high}} ({@code low} defaults to 0, {@code high} to 100).  A bad body fails
 * the page at render time, so the page fails loudly instead of drawing a broken gauge.  A good one is shaped into
 * {@code {gauge:{species, low, high, dataUrl}}}, which the {@code inventory-gauge} handler in {@code petstore-cards.js}
 * draws.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bftl'>
 * 	&lt;@card id="gauge" type="inventory-gauge" title="Dog inventory"&gt;{species:'DOG', low:20, high:120}&lt;/@card&gt;
 * </p>
 * <p class='bjava'>
 * 	<jc>// Registered once, on the console's FreeMarker mixin.</jc>
 * 	ConsoleFreemarkerMixin.<jsm>create</jsm>().cardType(<jk>new</jk> InventoryGaugeCardType()).build();
 * </p>
 */
public class InventoryGaugeCardType implements CardTypeHandler {

	/** The card type name. */
	public static final String TYPE = "inventory-gauge";

	/** The endpoint the gauge reads its counts from, before the {@code species} query parameter. */
	public static final String DATA_URL = "/console/dev/cards/inventory?species=";

	private static final String SPECIES_NAMES = Arrays.stream(Species.values()).map(Enum::name).collect(Collectors.joining(", "));

	@Override /* CardTypeHandler */
	public String type() {
		return TYPE;
	}

	@Override /* CardTypeHandler */
	public JsonMap toFragment(CardSource source) {
		var body = source.json();
		var raw = body.getString("species");
		if (raw == null)
			throw source.error("type='%s' requires 'species'.", TYPE);
		var species = species(source, raw);
		var low = whole(source, body, "low", 0);
		var high = whole(source, body, "high", 100);
		if (low < 0)
			throw source.error("low '%s' must not be negative.", low);
		if (low >= high)
			throw source.error("low '%s' must be below high '%s'.", low, high);
		var gauge = new JsonMap()
			.append("species", species.name())
			.append("low", low)
			.append("high", high)
			.append("dataUrl", DATA_URL + species.name());
		return new JsonMap().append("gauge", gauge);
	}

	private static int whole(CardSource source, JsonMap body, String key, int def) {
		var v = body.get(key);
		if (v == null)
			return def;
		if (v instanceof Number n) {
			var d = n.doubleValue();
			if (d == Math.rint(d) && d >= Integer.MIN_VALUE && d <= Integer.MAX_VALUE)
				return n.intValue();
		}
		throw source.error("%s '%s' must be a whole number.", key, v);
	}

	private static Species species(CardSource source, String raw) {
		try {
			return Species.valueOf(raw.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			throw source.error("unknown species '%s'; choose one of: '%s'.", raw, SPECIES_NAMES);
		}
	}
}
