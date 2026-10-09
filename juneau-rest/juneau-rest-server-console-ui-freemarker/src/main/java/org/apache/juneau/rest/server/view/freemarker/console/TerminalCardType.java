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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.server.console.*;
import org.apache.juneau.rest.server.terminal.*;

/**
 * The built-in {@code terminal} card type: validates a {@code {contractVersion:'1', terminal:{...}}} body through
 * {@link TerminalDef} and emits {@code {terminal: ...}}. {@code id=} is required and {@code src=}/{@code template=}
 * are rejected by {@code <@card>} itself before this handler runs.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	&lt;@card type="terminal" id="build"&gt;{ contractVersion: '1', terminal: { bytesUrl: '/runs/juneau-terminal/r42/bytes' } }&lt;/@card&gt;
 * </p>
 *
 * @since 10.0.0
 */
public final class TerminalCardType implements CardTypeHandler {

	private static final Set<String> BODY_KEYS = new LinkedHashSet<>(List.of("contractVersion", "terminal"));

	/** Public no-arg constructor, required by {@link java.util.ServiceLoader}. */
	public TerminalCardType() {}

	@Override
	public String type() {
		return "terminal";
	}

	@Override
	@SuppressWarnings({
		"unchecked" // JSON5 object keys are always strings.
	})
	public JsonMap toFragment(CardSource source) {
		if (! source.hasJsonBody())
			throw source.error("type='terminal' requires a JSON5 body { contractVersion: '1', terminal: {...} }.");
		var envelope = source.json();
		for (var k : envelope.keySet())
			if (! BODY_KEYS.contains(k))
				throw source.error("type='terminal' unknown key '%s'; allowed: contractVersion, terminal.", k);
		var version = envelope.get("contractVersion");
		if (! eq("1", version))
			throw source.error("type='terminal' requires contractVersion: '1'; got '%s'.", version);
		if (! (envelope.get("terminal") instanceof Map<?,?> terminal))
			throw source.error("type='terminal' requires a terminal object.");
		var def = TerminalDef.fromMap(source.id(), (Map<String,?>)terminal).validate();
		var frag = new JsonMap();
		frag.put("terminal", def.toMap());
		return frag;
	}
}
