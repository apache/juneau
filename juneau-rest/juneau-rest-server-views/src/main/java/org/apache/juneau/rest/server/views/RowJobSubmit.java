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

import java.util.*;

import org.apache.juneau.http.response.*;
import org.apache.juneau.marshall.collections.*;

/**
 * The action-envelope submit of a row job: {@code {action, targetId, idempotencyKey, fields}}, the shape the
 * row-action dialog and the job-log card's {@code envelope: "action"} both send.
 *
 * <p>
 * {@link #parse(JsonMap)} checks the shape; {@link #verify(JobKeyLedger, String, String)} checks the action, then the
 * key's recorded binding.  The row identity is {@link #targetId()} plus that binding &mdash; never anything in
 * {@link #fields()}, which is user-tamperable request data.  An endpoint that also carries the row id in its path
 * should check {@code targetId().equals(pathId)} first.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>var</jk> <jv>submit</jv> = RowJobSubmit.<jsm>parse</jsm>(<jv>json</jv>);
 * 	<jk>if</jk> (! <jv>submit</jv>.targetId().equals(<jv>id</jv>))
 * 		<jk>return</jk> refuse(<jv>res</jv>, RowJobSubmit.<jsm>bindingRefusal</jsm>());
 * 	<jk>var</jk> <jv>grant</jv> = <jv>submit</jv>.verify(<jv>jobKeys</jv>, <js>"restock-species"</js>, <jv>actor</jv>);
 * 	<jk>if</jk> (<jv>grant</jv>.isEmpty())
 * 		<jk>return</jk> refuse(<jv>res</jv>, RowJobSubmit.<jsm>bindingRefusal</jsm>());  <jc>// answered 409</jc>
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link JobKeyLedger}
 * </ul>
 *
 * @param action The submitted action id.
 * @param targetId The submitted row id.
 * @param idempotencyKey The submitted key value.
 * @param fields The submitted fields; empty when the body omitted them.
 *
 * @since 10.0.0
 */
public record RowJobSubmit(String action, String targetId, String idempotencyKey, JsonMap fields) {

	/** The HTTP status to answer {@link #bindingRefusal()} with. */
	public static final int REFUSAL_STATUS = 409;

	/**
	 * Parses the envelope.
	 *
	 * @param body The request body.
	 * @return The submit.
	 * @throws BadRequest On a non-object body, a blank {@code action}, {@code targetId} or {@code idempotencyKey}, or
	 * 	a {@code fields} member that is not an object.  A missing {@code fields} reads as empty.
	 */
	public static RowJobSubmit parse(JsonMap body) {
		if (body == null)
			throw new BadRequest("Expected a JSON object body");
		var action = text(body, "action");
		var targetId = text(body, "targetId");
		var key = text(body, "idempotencyKey");
		var f = body.get("fields");
		JsonMap fields;
		if (f == null)
			fields = new JsonMap();
		else if (f instanceof Map<?,?> m)
			fields = new JsonMap(m);
		else
			throw new BadRequest("'fields' must be an object");
		return new RowJobSubmit(action, targetId, key, fields);
	}

	private static String text(JsonMap body, String name) {
		var v = body.get(name);
		if (! (v instanceof CharSequence cs) || cs.toString().isBlank())
			throw new BadRequest("Missing '%s'", name);
		return cs.toString();
	}

	/**
	 * Checks the action, then the key's binding to {@code (action, targetId)}.
	 *
	 * @param ledger The ledger the key was issued from.
	 * @param expectedAction The action this endpoint performs.
	 * @return The grant, or empty when the action differs or the key does not verify.
	 */
	public Optional<JobKeyLedger.Grant> verify(JobKeyLedger ledger, String expectedAction) {
		if (! Objects.equals(action, expectedAction))
			return Optional.empty();
		return ledger.verify(idempotencyKey, action, targetId);
	}

	/**
	 * Checks the action, then the key's binding to {@code (action, targetId, principal)}.
	 *
	 * @param ledger The ledger the key was issued from.
	 * @param expectedAction The action this endpoint performs.
	 * @param principal The actor, resolved the same way the key endpoint resolved it.
	 * @return The grant, or empty when the action differs or the key does not verify.
	 */
	public Optional<JobKeyLedger.Grant> verify(JobKeyLedger ledger, String expectedAction, String principal) {
		if (! Objects.equals(action, expectedAction))
			return Optional.empty();
		return ledger.verify(idempotencyKey, action, targetId, principal);
	}

	/**
	 * The standard refusal for a failed verify; answer it with {@link #REFUSAL_STATUS}.
	 *
	 * <p>
	 * One code and one message for every kind of miss (see {@link JobKeyLedger}).
	 *
	 * @return A new refusal.
	 */
	public static ActionResult bindingRefusal() {
		return ActionResult.refusal(JobKeyLedger.REFUSAL_CODE).message("This key is not valid for this row.");
	}

	@Override /* Object */
	public String toString() {
		return "RowJobSubmit(action=" + action + ",targetId=" + targetId + ",idempotencyKey=<redacted>,fields=" + fields + ")";
	}
}
