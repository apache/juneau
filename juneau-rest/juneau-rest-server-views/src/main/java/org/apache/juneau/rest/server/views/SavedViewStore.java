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

/**
 * The consumer-injectable persistence seam behind {@link SavedViewsMixin} &mdash; the server half of the saved-views
 * persistence SPI (TODO-444 §3.3).
 *
 * <p>
 * Every operation is keyed on a <b>structured tuple</b> &mdash; {@code (principalName, pageId, viewId, name)} for the
 * per-view operations, {@code (principalName, pageId, viewId)} for the scope-level ones &mdash; passed as separate
 * arguments, <b>never</b> a {@code .}-joined or otherwise delimiter-concatenated composite key.  A structured tuple
 * cannot be spoofed by a value that happens to contain the delimiter, and it lets a durable backend map each
 * dimension to its own column/field.  The {@code enc()}/{@code dec()} segment codec that {@code juneau-config.js}'s
 * {@code localStorage} provider uses to flatten those dimensions into one string key is a
 * <b>localStorage-only</b> concern; it is <b>not</b> part of this seam (§3.3).
 *
 * <h5 class='section'>Identity comes from the request principal, always</h5>
 * <p>
 * {@code principalName} is {@link SavedViewsMixin}'s canonicalized {@link java.security.Principal#getName()} &mdash;
 * derived server-side and never client-supplied.  Because it is the <b>first</b> key dimension, a user saving
 * arbitrary {@code page}/{@code view}/{@code name} strings only ever writes inside <i>their own</i> namespace, so
 * there is no cross-user IDOR to defend against here beyond the principal-first keying plus the quotas below.
 * <b>Residual risk:</b> the framework keys on {@code getName()} alone, so two IdPs (or tenants) that mint the same
 * name would share a namespace &mdash; a multi-tenant consumer whose principal names are not globally unique MUST
 * override this store to prefix a tenant discriminator onto {@code principalName}.
 *
 * <h5 class='section'>Atomicity + race-safe quotas</h5>
 * <p>
 * {@link #saveAndActivate(String, String, String, String, String)} MUST write the blob AND flip the active pointer
 * as ONE serialized mutation for a given {@code (principalName, pageId, viewId)}, so a concurrent arrival can never
 * observe a half-applied blob-without-active or active-without-blob (§3.2).  The two count quotas
 * ({@link #MAX_VIEWS_PER_SCOPE}, {@link #MAX_VIEWS_PER_USER}) MUST be checked INSIDE the write op, never in a
 * separate pre-flight that could race, and the aggregate check MUST be serialized at {@code principalName} scope so
 * two parallel creates in DIFFERENT {@code (page,view)} scopes cannot both slip past the ceiling.  A create that
 * would exceed either count throws {@link SavedViewQuotaException}.  The count applies to a CREATE (a new name), not
 * to a replace (overwriting an existing name).
 *
 * <h5 class='section'>Non-durable default</h5>
 * <p>
 * The framework ships {@link InMemorySavedViewStore} as the out-of-the-box default (returned by
 * {@link SavedViewsMixin#savedViewStore()}); it is <b>in-memory and non-persistent</b> &mdash; lost on restart &mdash;
 * and exists so a host composing {@link SavedViewsMixin} works for dev/demo/tests with zero setup.  A consumer
 * overrides {@code savedViewStore()} to supply a durable implementation against its own datastore.  The framework
 * owns the wire (endpoint shape + blob schema) and this seam; the storage backend stays out of the framework, since
 * an Apache category-A module cannot take a persistence dependency.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link SavedViewsMixin}
 * 	<li class='jc'>{@link InMemorySavedViewStore}
 * 	<li class='jc'>{@link SavedViewQuotaException}
 * </ul>
 *
 * @since 10.0.0
 */
public interface SavedViewStore {

	/**
	 * Default per-{@code (user,page,view)} saved-view count cap.
	 *
	 * <p>
	 * A textually SEPARATE copy of {@code juneau-config.js}'s {@code LOCALSTORAGE_MAX_VIEWS_PER_SCOPE} (the two are a
	 * stated, testable parity invariant, never a shared source-of-truth across the JS/Java boundary): change one and
	 * change the other in the same commit.
	 */
	int MAX_VIEWS_PER_SCOPE = 50;

	/**
	 * Default per-user aggregate saved-view count cap across ALL {@code (page,view)} scopes.
	 *
	 * <p>
	 * A DISTINCT number from {@link #MAX_VIEWS_PER_SCOPE}: it bounds the abuse of minting unlimited {@code (page,view)}
	 * scopes to evade the per-scope cap.  Mirrors {@code juneau-config.js}'s {@code LOCALSTORAGE_MAX_VIEWS_PER_USER}
	 * (same parity invariant as above).
	 */
	int MAX_VIEWS_PER_USER = 500;

	/**
	 * Lists a scope's saved views and its resolved active pointer.
	 *
	 * <p>
	 * The returned {@link Listing#active()} MUST already have dangling-active resolved: an {@code active} pointing at a
	 * name that is not among {@link Listing#names()} resolves to {@code null} (Default), never an error (§3.2).
	 *
	 * @param principalName The canonicalized request-principal name.  Must not be <jk>null</jk> or blank.
	 * @param pageId The enclosing page id, or <jk>null</jk> for a standalone view.
	 * @param viewId The view id.  Must not be <jk>null</jk> or blank.
	 * @return The scope listing.  Never <jk>null</jk> (an unknown scope lists as {@code (null, [])}).
	 */
	Listing list(String principalName, String pageId, String viewId);

	/**
	 * Loads a single saved-view blob.
	 *
	 * @param principalName The canonicalized request-principal name.  Must not be <jk>null</jk> or blank.
	 * @param pageId The enclosing page id, or <jk>null</jk> for a standalone view.
	 * @param viewId The view id.  Must not be <jk>null</jk> or blank.
	 * @param name The saved-view name (decoded).  Must not be <jk>null</jk> or blank.
	 * @return The stored blob JSON, or <jk>null</jk> if no such view exists.
	 */
	String load(String principalName, String pageId, String viewId, String name);

	/**
	 * Saves (creates or replaces) a saved-view blob without touching the active pointer.
	 *
	 * <p>
	 * Enforces the count quotas atomically (see the interface javadoc); throws {@link SavedViewQuotaException} when a
	 * CREATE would exceed one of them.
	 *
	 * @param principalName The canonicalized request-principal name.  Must not be <jk>null</jk> or blank.
	 * @param pageId The enclosing page id, or <jk>null</jk> for a standalone view.
	 * @param viewId The view id.  Must not be <jk>null</jk> or blank.
	 * @param name The saved-view name (decoded).  Must not be <jk>null</jk> or blank.
	 * @param blob The blob JSON to store.  Must not be <jk>null</jk>.
	 * @throws SavedViewQuotaException If a create would exceed a count quota.
	 */
	void save(String principalName, String pageId, String viewId, String name, String blob);

	/**
	 * Atomically saves a blob AND flips the active pointer to it as ONE serialized mutation (§3.2).
	 *
	 * <p>
	 * Enforces the count quotas inside the same atomic op; throws {@link SavedViewQuotaException} when a CREATE would
	 * exceed one of them.
	 *
	 * @param principalName The canonicalized request-principal name.  Must not be <jk>null</jk> or blank.
	 * @param pageId The enclosing page id, or <jk>null</jk> for a standalone view.
	 * @param viewId The view id.  Must not be <jk>null</jk> or blank.
	 * @param name The saved-view name (decoded).  Must not be <jk>null</jk> or blank.
	 * @param blob The blob JSON to store.  Must not be <jk>null</jk>.
	 * @throws SavedViewQuotaException If a create would exceed a count quota.
	 */
	void saveAndActivate(String principalName, String pageId, String viewId, String name, String blob);

	/**
	 * Sets or clears the scope's active pointer, without writing any blob (the bare set-active op).
	 *
	 * <p>
	 * A {@code name} that does not (yet) name a persisted view is honored as-is; the next {@link #list} resolves it
	 * back to Default via the dangling-active rule (§3.2).  A <jk>null</jk> {@code name} clears the pointer.
	 *
	 * @param principalName The canonicalized request-principal name.  Must not be <jk>null</jk> or blank.
	 * @param pageId The enclosing page id, or <jk>null</jk> for a standalone view.
	 * @param viewId The view id.  Must not be <jk>null</jk> or blank.
	 * @param name The saved-view name (decoded) to activate, or <jk>null</jk> to clear (select Default).
	 */
	void setActive(String principalName, String pageId, String viewId, String name);

	/**
	 * Deletes a saved view.  A no-op if it does not exist.  Clears the active pointer if it named this view.
	 *
	 * @param principalName The canonicalized request-principal name.  Must not be <jk>null</jk> or blank.
	 * @param pageId The enclosing page id, or <jk>null</jk> for a standalone view.
	 * @param viewId The view id.  Must not be <jk>null</jk> or blank.
	 * @param name The saved-view name (decoded).  Must not be <jk>null</jk> or blank.
	 */
	void delete(String principalName, String pageId, String viewId, String name);

	/**
	 * The resolved active-view name for a scope, or <jk>null</jk> for Default.
	 *
	 * <p>
	 * A client-side convenience derived uniformly from {@link #list} (so the dangling-active resolution is applied
	 * exactly once, in {@code list}), mirroring {@code juneau-config.js}'s {@code getActive}.  There is deliberately
	 * no separate {@code GET .../active} HTTP endpoint &mdash; the JS server provider reads the active pointer off the
	 * list response &mdash; so this method exists for direct Java callers of the seam, not for the wire.
	 *
	 * @param principalName The canonicalized request-principal name.  Must not be <jk>null</jk> or blank.
	 * @param pageId The enclosing page id, or <jk>null</jk> for a standalone view.
	 * @param viewId The view id.  Must not be <jk>null</jk> or blank.
	 * @return The active-view name, or <jk>null</jk> for Default.
	 */
	default String getActive(String principalName, String pageId, String viewId) {
		return list(principalName, pageId, viewId).active();
	}

	/**
	 * The result of {@link SavedViewStore#list(String, String, String)}: the resolved active-view name (or
	 * <jk>null</jk> for Default) and the names of every saved view in the scope.
	 *
	 * @param active The resolved active-view name, or <jk>null</jk> for Default (dangling already resolved).
	 * @param names The saved-view names in the scope.  Never <jk>null</jk> (empty when the scope has none).
	 */
	record Listing(String active, List<String> names) {}
}
