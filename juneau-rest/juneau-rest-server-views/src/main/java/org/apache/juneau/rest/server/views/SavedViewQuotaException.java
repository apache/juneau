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

/**
 * Thrown by a {@link SavedViewStore} when a create would exceed one of the store's count quotas &mdash; the
 * per-scope cap ({@link SavedViewStore#MAX_VIEWS_PER_SCOPE}) or the per-user aggregate cap
 * ({@link SavedViewStore#MAX_VIEWS_PER_USER}) &mdash; so {@link SavedViewsMixin} can map it to the quota HTTP
 * status the client's server-persisted provider classifies as a {@code quota}-typed error (TODO-444 §3.2).
 *
 * <p>
 * The count quotas are the store's responsibility (not the mixin's) precisely because they are stateful and
 * race-sensitive: two parallel creates in different {@code (page,view)} scopes for the same user each pass a naive
 * per-scope check, so the aggregate ceiling can only be enforced correctly under the store's own per-user
 * serialization (see {@link InMemorySavedViewStore}).  A consumer's durable store implementation throws this same
 * exception so it gets the identical HTTP mapping without re-implementing the mixin.
 *
 * <p>
 * The per-blob size cap is a stateless check enforced on the mixin (a different HTTP status), not here &mdash; see
 * {@link SavedViewsMixin} for the full status mapping.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link SavedViewStore}
 * 	<li class='jc'>{@link SavedViewsMixin}
 * </ul>
 *
 * @since 10.0.0
 */
public class SavedViewQuotaException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Which count quota was exceeded.
	 */
	public enum Kind {

		/** The per-{@code (user,page,view)} saved-view count cap ({@link SavedViewStore#MAX_VIEWS_PER_SCOPE}). */
		SCOPE,

		/** The per-user aggregate saved-view count cap across all scopes ({@link SavedViewStore#MAX_VIEWS_PER_USER}). */
		AGGREGATE
	}

	private final Kind kind;

	/**
	 * Constructor.
	 *
	 * @param kind The quota that was exceeded.  Must not be <jk>null</jk>.
	 * @param message A human-readable description of the exceeded quota.
	 */
	public SavedViewQuotaException(Kind kind, String message) {
		super(message);
		this.kind = kind;
	}

	/**
	 * The quota that was exceeded.
	 *
	 * @return The quota kind.  Never <jk>null</jk>.
	 */
	public Kind kind() { return kind; }
}
