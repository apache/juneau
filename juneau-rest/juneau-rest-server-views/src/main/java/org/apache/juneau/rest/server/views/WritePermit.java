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
package org.apache.juneau.rest.server.views;

import static org.apache.juneau.commons.utils.Shorts.*;

/**
 * The views-toolkit's own structural gate token for {@link BulkMutateDef}: proof that the caller's own write-gate
 * was consulted before a bulk-mutating affordance is configured (design doc §9.3; {@code TODO-428}'s HIGH-5
 * separability guarantee).
 *
 * <h5 class='section'>Why this exists, and why it is NOT the console's own {@code WritePermit}</h5>
 * <p>
 * A consuming application (e.g. the Sandbox Support Console) may already have its own, much richer write-gate
 * token &mdash; one bound to a journal, a box {@code Mode}, an idempotency key, and a target validator (see that
 * application's own {@code write.WritePermit}/{@code WriteGuard}). That type is deliberately <b>not</b> reused
 * here: its constructor is package-private to its own {@code write} package, it is defined in a console-ui
 * artifact this framework module must not depend on, and its shape carries concerns (journal binding, SAFE/LIVE
 * mode) that are that application's business, not this framework's. Reaching into it would be a layering
 * violation in the wrong direction (framework depending on a specific consumer).
 *
 * <p>
 * This type is therefore this module's own, minimal, independent token &mdash; the same <i>discipline</i>
 * (a required-parameter capability proof), a deliberately smaller <i>shape</i> (no journal/mode/idempotency
 * concerns, which belong to the consumer's own gate). A consuming application's own write-gate is expected to
 * mint one (typically by wrapping/adapting its own gate result) only after ITS OWN checks have passed, then hand
 * it to {@link BulkMutateDef#create(WritePermit, SelectionDef)}. This module does not, and cannot, verify that the
 * caller's gate logic is correct &mdash; that is out of scope for a UI-rendering toolkit &mdash; but it does
 * guarantee, by construction, that {@link BulkMutateDef} cannot be reached at all without a permit in hand: there
 * is no zero-argument overload, so omitting one is a <b>compile</b> failure, not a missed javadoc sentence
 * (mirroring the discipline of the console's own {@code WriteGuard}, source design §5.4).
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// After the consumer's OWN write-gate has already approved this capability:</jc>
 * 	WritePermit <jv>permit</jv> = WritePermit.<jsm>forCapability</jsm>(<js>"incident:bulk-ack"</js>);
 * 	BulkMutateDef <jv>bulk</jv> = BulkMutateDef.<jsm>create</jsm>(<jv>permit</jv>, <jv>selection</jv>)
 * 		.actions(RowAction.<jsm>create</jsm>(<js>"ack"</js>).endpoint(<js>"servlet:/incidents/{id}/ack"</js>).method(RowAction.Method.<jsf>POST</jsf>));
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link BulkMutateDef}
 * 	<li class='jc'>{@link SelectionDef}
 * </ul>
 *
 * @since 10.0.0
 */
public final class WritePermit {

	private final String capability;

	private WritePermit(String capability) {
		this.capability = capability;
	}

	/**
	 * Mints a permit for the named capability.
	 *
	 * <p>
	 * This is a bare proof-of-authorization token, not an enforcement seam: minting one asserts that the CALLER's
	 * own write-gate has already approved {@code capability}. This module trusts that assertion &mdash; it has no
	 * journal, arming, or idempotency logic of its own to check it against &mdash; its only job is to make the
	 * bulk-mutate opt-in unreachable without one.
	 *
	 * @param capability A stable, human-readable identifier for the capability this permit authorizes (e.g.
	 * 	{@code "incident:bulk-ack"}). Must not be <jk>null</jk> or blank.
	 * @return A new {@link WritePermit}.
	 * @throws IllegalArgumentException If {@code capability} is <jk>null</jk> or blank.
	 */
	public static WritePermit forCapability(String capability) {
		if (capability == null || capability.isBlank())
			throw iaex("WritePermit capability must not be null or blank.");
		return new WritePermit(capability);
	}

	/**
	 * The capability this permit authorizes.
	 *
	 * @return The capability identifier passed to {@link #forCapability(String)}.
	 */
	public String capability() {
		return capability;
	}

	@Override /* Object */
	public String toString() {
		return "WritePermit(capability=" + capability + ")";
	}
}
