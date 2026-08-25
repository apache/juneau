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

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.nio.charset.*;
import java.security.*;
import java.util.*;

/**
 * A server-minted idempotency key for a row-action submit, bound at mint time to the {@code (action, targetId)} pair
 * it was issued for (design doc §6.2; the idempotency half of the row-action result contract).
 *
 * <h5 class='section'>Why the key is server-minted and bound, not a client UUID</h5>
 * <p>
 * The key is minted <b>when the modal opens</b> &mdash; the server is already in the loop then, since the modal-open
 * confirmation is server-rendered against the current record &mdash; so a double-click, an impatient re-submit and a
 * browser retry all carry the <b>same</b> key and collapse to one effect.  The modal owns the key's <i>carry</i>
 * lifecycle; it does <b>not</b> mint it.
 * <p>
 * A client-generated key is not the security property.  A globally-unique key looked up <i>only by key</i> would let
 * a recorded terminal outcome for incident&nbsp;A be replayed under a submit for incident&nbsp;B &mdash; returning
 * A's "success" for a write B never performed, and resurrecting the "resolve an incident that was re-triggered in the
 * interim" case.  This key is therefore bound to {@code (action, targetId)} at mint time: a submit whose
 * {@code (action, targetId)} does not {@link #matches(String, String) match} the binding is a <b>named refusal</b>,
 * never a replayed success.
 *
 * <h5 class='section'>Unforgeability</h5>
 * <p>
 * The key value is {@value #KEY_BYTES} random bytes ({@value #KEY_BITS}&nbsp;bits) from {@link SecureRandom},
 * hex-encoded &mdash; well beyond the ≥128-bit bar, matching {@link org.apache.juneau.rest.server.filter.SynchronizerToken}'s
 * 256-bit reference.  The binding is what makes it safe; the width is what makes it unguessable.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// At modal-open: mint a key bound to this action + this row.</jc>
 * 	IdempotencyKey <jv>key</jv> = IdempotencyKey.<jsm>mint</jsm>(<js>"ack"</js>, <jv>incidentId</jv>);
 * 	<jv>modal</jv>.idempotencyKey(<jv>key</jv>.value());
 *
 * 	<jc>// At submit: rehydrate the recorded binding and check it against the submitted (action, targetId).</jc>
 * 	<jk>if</jk> (! <jv>recorded</jv>.matches(<jv>submittedAction</jv>, <jv>submittedTargetId</jv>))
 * 		<jk>return</jk> ActionResult.<jsm>refusal</jsm>(<js>"write-guard:idempotency-binding-mismatch"</js>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link org.apache.juneau.rest.server.widgets.ModalDef}
 * 	<li class='jc'>{@link ActionResult}
 * 	<li class='jc'>{@link org.apache.juneau.rest.server.filter.SynchronizerToken}
 * </ul>
 *
 * @since 10.0.0
 */
public final class IdempotencyKey {

	/** Width of a minted key value, in bits &mdash; well beyond the ≥128-bit bar. */
	private static final int KEY_BITS = 256;

	/** Number of random bytes behind a minted key value, derived from {@link #KEY_BITS}. */
	private static final int KEY_BYTES = KEY_BITS / 8;

	/** Shared secure random source for minting key values; {@link SecureRandom} is safe for concurrent use. */
	private static final SecureRandom RANDOM = new SecureRandom();

	private final String value;
	private final String action;
	private final String targetId;

	private IdempotencyKey(String value, String action, String targetId) {
		this.value = value;
		this.action = action;
		this.targetId = targetId;
	}

	/**
	 * Mints a fresh key bound to the specified {@code (action, targetId)} pair.
	 *
	 * @param action The stable action id this key is issued for.  Must not be <jk>null</jk> or blank.
	 * @param targetId The id of the row/target this key is issued for.  Must not be <jk>null</jk> or blank.
	 * @return A new key holding a fresh {@value #KEY_BITS}-bit secret, hex-encoded, bound to {@code (action, targetId)}.
	 * @throws IllegalArgumentException If {@code action} or {@code targetId} is <jk>null</jk> or blank.
	 */
	public static IdempotencyKey mint(String action, String targetId) {
		requireNonBlank("action", action);
		requireNonBlank("targetId", targetId);
		var bytes = new byte[KEY_BYTES];
		RANDOM.nextBytes(bytes);
		return new IdempotencyKey(HexFormat.of().formatHex(bytes), action, targetId);
	}

	/**
	 * Rehydrates a previously-minted key from its recorded value and binding.
	 *
	 * <p>
	 * Intended for the submit path: the consumer looks up the recorded {@code (value, action, targetId)} it minted at
	 * modal-open and reconstructs this to {@link #matches(String, String) check} it against the submitted pair.
	 *
	 * @param value The recorded key value.  Must not be <jk>null</jk> or blank.
	 * @param action The action id the key was bound to at mint time.  Must not be <jk>null</jk> or blank.
	 * @param targetId The target id the key was bound to at mint time.  Must not be <jk>null</jk> or blank.
	 * @return A key holding the recorded value and binding.
	 * @throws IllegalArgumentException If any argument is <jk>null</jk> or blank.
	 */
	public static IdempotencyKey of(String value, String action, String targetId) {
		requireNonBlank("value", value);
		requireNonBlank("action", action);
		requireNonBlank("targetId", targetId);
		return new IdempotencyKey(value, action, targetId);
	}

	/**
	 * The opaque key value, for carrying in the modal and echoing back on the submit.
	 *
	 * @return The key value.  Never <jk>null</jk> or blank.
	 */
	public String value() { return value; }

	/**
	 * The action id this key is bound to.
	 *
	 * @return The bound action id.
	 */
	public String action() { return action; }

	/**
	 * The target id this key is bound to.
	 *
	 * @return The bound target id.
	 */
	public String targetId() { return targetId; }

	/**
	 * Whether a submitted {@code (action, targetId)} matches this key's binding.
	 *
	 * <p>
	 * A mismatch must be answered with a <b>named refusal</b>, never a replayed success: it is a key being submitted
	 * against a different action or a different target than it was minted for.  Compares the target id in constant
	 * time so a mismatch cannot be probed character-by-character.
	 *
	 * @param submittedAction The action id presented by the submit.  Can be <jk>null</jk>.
	 * @param submittedTargetId The target id presented by the submit.  Can be <jk>null</jk>.
	 * @return <jk>true</jk> only if both the action and the target id match this key's binding exactly.
	 */
	public boolean matches(String submittedAction, String submittedTargetId) {
		if (submittedAction == null || submittedTargetId == null)
			return false;
		return constantTimeEquals(action, submittedAction) && constantTimeEquals(targetId, submittedTargetId);
	}

	private static boolean constantTimeEquals(String a, String b) {
		return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
	}

	private static void requireNonBlank(String name, String value) {
		assertArgNotNull(name, value);
		if (value.isBlank())
			throw iaex("Argument ''%s'' must not be blank.", name);
	}

	/**
	 * Returns a description that does <b>not</b> include the key value.
	 *
	 * <p>
	 * The value is a secret; a bean-dumping logger or a debug view that stringifies it would otherwise write it
	 * somewhere it can be read back and replayed.  The binding is safe to show.
	 *
	 * @return A value-free description.
	 */
	@Override /* Object */
	public String toString() {
		return "IdempotencyKey(value=<redacted>,action=" + action + ",targetId=" + targetId + ")";
	}
}
