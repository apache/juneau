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
package org.apache.juneau.rest.server.mcp.v20260728;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.util.*;

import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.marshall.marshaller.*;

/**
 * Static helper that parses typed {@link ElicitResult}s out of an {@link McpMrtrResumeContext} (MCP
 * {@code 2026-07-28} SEP-2322 elicitation, riding the Multi-Round-Trip Requests loop).
 *
 * <p>
 * {@link McpMrtrResumeContext#inputResponses()} values are generic JSON (a {@code JsonMap}/{@code JsonList}/
 * boxed primitive/{@code String} after wire deserialization, never a typed bean automatically); this helper
 * converts each to {@link ElicitResult} via the same {@code Json.to(Json.of(value), type)} technique
 * {@link McpMrtrResumeContext#continuationAs(Class)} already uses for the continuation side, just applied
 * per-key across the map instead of to the single continuation value.
 */
public final class ElicitationResponses {

	private ElicitationResponses() {}

	/**
	 * Returns the typed answer for a single question id.
	 *
	 * @param ctx The resume context.  Must not be <jk>null</jk>.
	 * @param id The server-assigned id this question was posed under.  Must not be <jk>null</jk>.
	 * @return The typed answer, or <jk>null</jk> if {@code id} is absent from {@code ctx}'s responses, or maps
	 * 	to <jk>null</jk>.
	 * @throws IllegalArgumentException If {@code ctx} or {@code id} is <jk>null</jk>.
	 * @throws RuntimeException If the answer's decoded shape cannot be converted to {@link ElicitResult}.
	 */
	public static ElicitResult get(McpMrtrResumeContext ctx, String id) {
		assertArgNotNull("ctx", ctx);
		assertArgNotNull("id", id);
		var value = ctx.inputResponses().get(id);
		if (value == null)
			return null;
		return Json.to(Json.of(value), ElicitResult.class);
	}

	/**
	 * Returns every answer in {@code ctx}, converted to {@link ElicitResult}.
	 *
	 * @param ctx The resume context.  Must not be <jk>null</jk>.
	 * @return A keyed map of typed answers.  Never <jk>null</jk> (empty if {@code ctx} carried none). Values
	 * 	may be <jk>null</jk> when the corresponding raw entry is absent/<jk>null</jk> (see {@link #get}).
	 * @throws IllegalArgumentException If {@code ctx} is <jk>null</jk>.
	 * @throws RuntimeException If any answer's decoded shape cannot be converted to {@link ElicitResult}.
	 */
	public static Map<String,ElicitResult> all(McpMrtrResumeContext ctx) {
		assertArgNotNull("ctx", ctx);
		Map<String,ElicitResult> out = new LinkedHashMap<>();
		ctx.inputResponses().keySet().forEach(id -> out.put(id, get(ctx, id)));
		return out;
	}

	/**
	 * Returns a boolean field's value from a single question's answer, decline-safe.
	 *
	 * <p>
	 * Treats a missing id, a missing field, or a non-{@link ElicitAction#ACCEPT} action (declined, cancelled)
	 * as <jk>false</jk> rather than requiring the caller to null-check {@link #get} and its content map by
	 * hand &mdash; the safe default for a confirmation question is to never proceed on ambiguity.
	 *
	 * @param ctx The resume context.  Must not be <jk>null</jk>.
	 * @param id The server-assigned id this question was posed under.  Must not be <jk>null</jk>.
	 * @param field The field name within the answer's content map.  Must not be <jk>null</jk>.
	 * @return <jk>true</jk> only if {@code id} is present, the action is {@link ElicitAction#ACCEPT}, and
	 * 	{@code field} is present in the content map with value {@link Boolean#TRUE}. <jk>false</jk> in every
	 * 	other case.
	 * @throws IllegalArgumentException If {@code ctx}, {@code id}, or {@code field} is <jk>null</jk>.
	 * @throws RuntimeException If the answer's decoded shape cannot be converted to {@link ElicitResult}.
	 */
	public static boolean getBoolean(McpMrtrResumeContext ctx, String id, String field) {
		assertArgNotNull("ctx", ctx);
		assertArgNotNull("id", id);
		assertArgNotNull("field", field);
		var answer = get(ctx, id);
		if (answer == null || answer.getAction() != ElicitAction.ACCEPT)
			return false;
		var content = answer.getContent();
		return content != null && Boolean.TRUE.equals(content.get(field));
	}

	/**
	 * Returns a string field's value from a single question's answer, decline-safe.
	 *
	 * <p>
	 * Treats a missing id, a missing field, or a non-{@link ElicitAction#ACCEPT} action (declined, cancelled)
	 * as <jk>null</jk> rather than requiring the caller to null-check {@link #get} and its content map by hand.
	 *
	 * @param ctx The resume context.  Must not be <jk>null</jk>.
	 * @param id The server-assigned id this question was posed under.  Must not be <jk>null</jk>.
	 * @param field The field name within the answer's content map.  Must not be <jk>null</jk>.
	 * @return The field's value as a {@link String}, or <jk>null</jk> if {@code id} is absent, the action is
	 * 	not {@link ElicitAction#ACCEPT}, {@code field} is absent, or its value is not a {@link String}.
	 * @throws IllegalArgumentException If {@code ctx}, {@code id}, or {@code field} is <jk>null</jk>.
	 * @throws RuntimeException If the answer's decoded shape cannot be converted to {@link ElicitResult}.
	 */
	public static String getString(McpMrtrResumeContext ctx, String id, String field) {
		assertArgNotNull("ctx", ctx);
		assertArgNotNull("id", id);
		assertArgNotNull("field", field);
		var answer = get(ctx, id);
		if (answer == null || answer.getAction() != ElicitAction.ACCEPT)
			return null;
		var content = answer.getContent();
		if (content == null)
			return null;
		return content.get(field) instanceof String s ? s : null;
	}
}
