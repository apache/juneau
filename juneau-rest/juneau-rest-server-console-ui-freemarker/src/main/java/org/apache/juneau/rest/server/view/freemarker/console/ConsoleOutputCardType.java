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
import org.apache.juneau.rest.server.views.*;

/**
 * The built-in {@code console-output} card type: validates a {@code {contractVersion:'1', output:{...}}} body
 * through {@link ConsoleOutputDef} and emits {@code {output: ...}}. {@code id=} is required and
 * {@code src=}/{@code template=} are rejected by {@code <@card>} itself before this handler runs.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	&lt;@card type="console-output" id="log"&gt;{ contractVersion: '1', output: { linesUrl: '/logs/1/lines' } }&lt;/@card&gt;
 * </p>
 *
 * @since 10.0.0
 */
public final class ConsoleOutputCardType implements CardTypeHandler {

	private static final Set<String> BODY_KEYS = new LinkedHashSet<>(List.of("contractVersion", "output"));

	/** Public no-arg constructor, required by {@link java.util.ServiceLoader}. */
	public ConsoleOutputCardType() {}

	@Override
	public String type() {
		return "console-output";
	}

	@Override
	@SuppressWarnings({
		"unchecked" // JSON5 object keys are always strings.
	})
	public JsonMap toFragment(CardSource source) {
		if (! source.hasJsonBody())
			throw source.error("type='console-output' requires a JSON5 body { contractVersion: '1', output: {...} }.");
		var envelope = source.json();
		for (var k : envelope.keySet())
			if (! BODY_KEYS.contains(k))
				throw source.error("type='console-output' unknown key '%s'; allowed: contractVersion, output.", k);
		var version = envelope.get("contractVersion");
		if (! eq("1", version))
			throw source.error("type='console-output' requires contractVersion: '1'; got '%s'.", version);
		if (! (envelope.get("output") instanceof Map<?,?> output))
			throw source.error("type='console-output' requires an output object.");
		var def = ConsoleOutputDef.fromMap(source.id(), (Map<String,?>)output).validate();
		var frag = new JsonMap();
		frag.put("output", def.toMap());
		return frag;
	}
}
