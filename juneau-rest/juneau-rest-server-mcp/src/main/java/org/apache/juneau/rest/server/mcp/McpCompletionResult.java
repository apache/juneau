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
package org.apache.juneau.rest.server.mcp;

import java.util.*;

import org.apache.juneau.bean.jsonrpc.*;

/**
 * Revision-neutral, as-returned-by-the-application result of an {@link McpCompleter} invocation.
 *
 * <p>
 * Supersedes the wire-level {@code CompleteResult}/{@code Completion} beans. An instance of this class
 * is the completer's raw, unvalidated output; call {@link #normalize(McpCompletionResult)} exactly once
 * per completion dispatch to obtain the {@link Normalized} shape both dated adapters map to their wire
 * result.
 *
 * <h5 class='section'>Normalization contract:</h5>
 * <p>
 * Applied by {@link #normalize(McpCompletionResult)} before wire mapping:
 * <ul>
 * 	<li><jk>null</jk> {@link #getValues() values} normalizes to an empty list;
 * 	<li>order is preserved (application-owned ranking) and duplicates are preserved;
 * 	<li>a <jk>null</jk> element within {@code values} is an <b>internal handler failure</b>;
 * 	<li>the first {@value #MAX_VALUES} values are emitted; truncation forces {@code hasMore=true};
 * 	<li>a supplied non-negative {@link #getTotal() total} is preserved; a negative {@code total} is an
 * 		<b>internal handler failure</b>;
 * 	<li>without truncation, a nullable {@link #getHasMore() hasMore} is passed through unchanged; and
 * 	<li>a completer returning <jk>null</jk> itself (rather than an {@code McpCompletionResult} with
 * 		<jk>null</jk> fields) is also an <b>internal handler failure</b>.
 * </ul>
 *
 * <h5 class='section'>Internal handler failure representation:</h5>
 * <p>
 * Each of the three "internal handler failure" cases above is surfaced identically: as an
 * {@link McpException} carrying {@link #CODE_INTERNAL_ERROR} ({@code -32603}), the JSON-RPC 2.0
 * standard "Internal error" code. That code is fixed by the JSON-RPC specification, not chosen per
 * revision, which is why it is a local constant here rather than routed through
 * {@code McpRevision#errorCode(McpErrorKind)} - the same reasoning {@code McpParamUtils} documents for
 * its {@code -32602} constant. Both dated adapters' existing top-level {@code catch (McpException e)}
 * dispatch handling (see {@code McpRevision#dispatch}) already converts any {@link McpException} thrown
 * during {@code completion/complete} handling straight into a JSON-RPC error response with this code, so
 * adapters do not need any additional mapping step beyond calling
 * {@link #normalize(McpCompletionResult)} and letting a thrown {@link McpException} propagate.
 */
public class McpCompletionResult {

	/**
	 * The maximum number of completion values a {@link Normalized} result ever carries. Juneau performs
	 * no ranking, filtering, deduplication, or pagination of its own; this is strictly a truncation cap.
	 */
	public static final int MAX_VALUES = 100;

	/**
	 * JSON-RPC error code raised by {@link #normalize(McpCompletionResult)} for any internal handler
	 * failure. See the class Javadoc "Internal handler failure representation" section.
	 */
	public static final int CODE_INTERNAL_ERROR = -32603;

	private List<String> values;
	private Integer total;
	private Boolean hasMore;

	/**
	 * Creates the neutral empty-completion result, the shape both dated adapters map to
	 * {@code {"completion":{"values":[]}}}.
	 *
	 * @return A new result with an empty {@link #getValues() values} list and no {@link #getTotal()
	 * 	total}/{@link #getHasMore() hasMore}. Never <jk>null</jk>.
	 */
	public static McpCompletionResult empty() {
		return new McpCompletionResult().setValues(List.of());
	}

	/**
	 * The completion suggestions, in application-ranked order.
	 *
	 * @return The values, or <jk>null</jk> if not set. Normalized to an empty list by
	 * 	{@link #normalize(McpCompletionResult)}.
	 */
	public List<String> getValues() {
		return values;
	}

	/**
	 * Sets the completion suggestions.
	 *
	 * @param value The new value. Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public McpCompletionResult setValues(List<String> value) {
		values = value;
		return this;
	}

	/**
	 * The total number of completions available, if known, independent of how many values are emitted.
	 *
	 * @return The total, or <jk>null</jk> if not set/unknown.
	 */
	public Integer getTotal() {
		return total;
	}

	/**
	 * Sets the total number of completions available.
	 *
	 * @param value The new value. Must be non-negative if non-<jk>null</jk>; see
	 * 	{@link #normalize(McpCompletionResult)}. Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public McpCompletionResult setTotal(Integer value) {
		total = value;
		return this;
	}

	/**
	 * Whether more completions exist beyond {@link #getValues() values}.
	 *
	 * @return The flag, or <jk>null</jk> if not set/unknown.
	 */
	public Boolean getHasMore() {
		return hasMore;
	}

	/**
	 * Sets whether more completions exist beyond {@link #getValues() values}.
	 *
	 * @param value The new value. Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public McpCompletionResult setHasMore(Boolean value) {
		hasMore = value;
		return this;
	}

	/**
	 * Applies the normalization contract documented on this class to a completer's raw return value.
	 *
	 * @param result The raw result returned by an {@link McpCompleter}. Can be <jk>null</jk>.
	 * @return The normalized shape ready for wire mapping. Never <jk>null</jk>.
	 * @throws McpException With {@link #CODE_INTERNAL_ERROR} if {@code result} is <jk>null</jk>, if
	 * 	{@code result}'s values contain a <jk>null</jk> element, or if {@code result}'s total is negative.
	 */
	public static Normalized normalize(McpCompletionResult result) {
		if (result == null)
			throw new McpException(CODE_INTERNAL_ERROR, "Completer returned a null McpCompletionResult.");

		var raw = result.getValues();
		var source = raw == null ? List.<String>of() : raw;
		for (var v : source)
			if (v == null)
				throw new McpException(CODE_INTERNAL_ERROR, "Completer result values must not contain a null element.");

		var total = result.getTotal();
		if (total != null && total < 0)
			throw new McpException(CODE_INTERNAL_ERROR, "Completer result total must not be negative: " + total);

		var hasMore = result.getHasMore();
		List<String> values;
		if (source.size() > MAX_VALUES) {
			values = List.copyOf(source.subList(0, MAX_VALUES));
			hasMore = Boolean.TRUE;
		} else {
			values = List.copyOf(source);
		}
		return new Normalized(values, total, hasMore);
	}

	/**
	 * The normalized shape of a completion result, ready for wire mapping.
	 *
	 * @param values The (possibly truncated) completion values, in order, with duplicates preserved.
	 * 	Immutable. Never <jk>null</jk>; empty when the completer supplied none.
	 * @param total The completer-supplied total, or <jk>null</jk> if unknown/unset.
	 * @param hasMore Whether more completions exist. <jk>true</jk> when truncation occurred; otherwise
	 * 	the completer-supplied value, which may be <jk>null</jk>.
	 */
	public record Normalized(List<String> values, Integer total, Boolean hasMore) {}
}
