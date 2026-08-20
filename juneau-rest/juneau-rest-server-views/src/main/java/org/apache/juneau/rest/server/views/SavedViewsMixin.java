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

import java.io.*;
import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.http.response.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.rest.server.*;

/**
 * Mixin interface that exposes the server half of the saved-views persistence SPI on any Juneau REST resource
 * (TODO-444 §3.3) &mdash; the fixed-mount REST endpoints the {@code juneau-config.js} server-persisted provider
 * already calls, backed by a consumer-injectable {@link SavedViewStore}.
 *
 * <p>
 * A composing resource gets the endpoints for free and, unless it overrides {@link #savedViewStore()}, a
 * non-durable {@link InMemorySavedViewStore in-memory default} so the wire works out-of-the-box for dev/demo/tests.
 * A consumer wanting persistence overrides {@code savedViewStore()} to return a durable implementation.
 *
 * <h5 class='section'>Fixed mount (not overridable)</h5>
 * <p>
 * Every path is baked into the op-level {@code @RestGet/@RestPut/@RestDelete} annotations off
 * {@link #SAVED_VIEWS_PREFIX}, exactly like {@link AsyncJobsMixin#JOBS_PREFIX}.  A class-level {@code @Rest(path=...)}
 * on the composing host is <b>silently ignored</b> under the mixin pattern (see {@link ViewsMixin}), and an
 * interface constant cannot be overridden by a subclass, so the mount is a constraint, not a preference.  A consumer
 * that genuinely needs a different path does not compose this mixin at all &mdash; it re-implements the documented
 * wire at its own path and points the JS provider's {@code data-juneau-saved-views} base attribute there.  The
 * framework guarantees the <i>wire</i>, not the <i>mount</i>.
 *
 * <h5 class='section'>Endpoints</h5>
 * <table class='styled'>
 * 	<tr><th>Method + path</th><th>Query</th><th>Body</th><th>Store op</th></tr>
 * 	<tr><td>{@code GET  /juneau-saved-views}</td><td>{@code view},{@code page?}</td><td>&mdash;</td>
 * 		<td>{@link SavedViewStore#list list} &rarr; <code>{active,views:[{name}]}</code></td></tr>
 * 	<tr><td>{@code GET  /juneau-saved-views/item}</td><td>{@code view},{@code page?},{@code name}</td><td>&mdash;</td>
 * 		<td>{@link SavedViewStore#load load} &rarr; the blob, or {@code 404}</td></tr>
 * 	<tr><td>{@code PUT  /juneau-saved-views/item}</td><td>{@code view},{@code page?},{@code name}</td><td>blob</td>
 * 		<td>{@link SavedViewStore#save save}</td></tr>
 * 	<tr><td>{@code PUT  /juneau-saved-views/item?activate=1}</td><td>{@code view},{@code page?},{@code name}</td>
 * 		<td>blob</td><td>{@link SavedViewStore#saveAndActivate saveAndActivate}</td></tr>
 * 	<tr><td>{@code PUT  /juneau-saved-views/active}</td><td>{@code view},{@code page?}</td>
 * 		<td><code>{name}</code> / <code>{}</code></td><td>{@link SavedViewStore#setActive setActive} (or clear)</td></tr>
 * 	<tr><td>{@code DELETE /juneau-saved-views/item}</td><td>{@code view},{@code page?},{@code name}</td>
 * 		<td><code>{}</code></td><td>{@link SavedViewStore#delete delete}</td></tr>
 * </table>
 * <p>
 * There is deliberately <b>no</b> {@code GET .../active} endpoint: the JS provider's {@code getActive} reads the
 * active pointer off the {@code list} response (verified against slice-2 {@code juneau-config.js}), so a second
 * endpoint would be dead wire.  {@code activate} is a QUERY FLAG on {@code PUT .../item}, never a blob field &mdash;
 * an activate boolean buried in the blob would wrongly persist into every saved view (§3.3).  {@code name} is a
 * QUERY param, never a path segment, because {@code %2F} in a name is not reliably decoded inside a path segment
 * across servlet containers (§3.3).  The container URL-decodes {@code view}/{@code page}/{@code name}; this mixin
 * works with the DECODED strings and passes them straight into the store's structured tuple &mdash; it never
 * {@code enc()}s them, because the store keys on separate fields, not a delimiter-joined string, so no
 * delimiter-safety codec is needed (§3.3).
 *
 * <h5 class='section'>Identity: the request principal, always ({@code 401} on absent/blank)</h5>
 * <p>
 * EVERY op &mdash; including the safe {@code GET list} &mdash; resolves the caller from
 * {@link RestRequest#getUserPrincipal()}{@code .getName()}, canonicalized (trimmed), and answers {@code 401} when it
 * is absent or blank.  A missing principal is NEVER folded into an {@code "anonymous"} bucket &mdash; that would let
 * every unauthenticated caller share one saved-views namespace.  Any client-supplied user id in a query param or
 * body is IGNORED; identity comes only from the principal, which is the FIRST key dimension, so a user can only ever
 * read/write inside their own namespace (no cross-user IDOR).  <b>Residual risk:</b> identity is
 * {@code Principal.getName()} alone, so two IdPs/tenants minting the same name would collide on one namespace; a
 * multi-tenant consumer whose principal names are not globally unique MUST override {@link #savedViewStore()} to
 * prefix a tenant discriminator (see {@link SavedViewStore}).
 *
 * <h5 class='section'>CSRF / Origin / Host: the boundary filter's job, not this mixin's</h5>
 * <p>
 * The CSRF token, {@code Origin} and {@code Host} checks on writes are enforced by
 * {@link org.apache.juneau.rest.server.filter.LoopbackBoundaryFilter} (a {@code /*} servlet filter), which runs
 * BEFORE any request reaches this mixin and rejects a forged write with {@code 403}/{@code 421}.  This mixin does
 * NOT re-implement CSRF: it has no handle to the process {@link org.apache.juneau.rest.server.filter.SynchronizerToken
 * token} (only the filter does), and a second, independently-maintained CSRF control is exactly the kind of parallel
 * mechanism that drifts out of agreement with the real one.  As defense-in-depth AND so the check is exercisable
 * without a filter chain (a {@code MockRestClient} does not run filters), this mixin DOES independently require
 * {@code Content-Type: application/json} on every write and answers {@code 415} otherwise &mdash; the same status
 * {@link org.apache.juneau.rest.server.filter.LoopbackBoundary} returns, so the two layers agree.
 *
 * <h5 class='section'>Validation, quotas, and the HTTP-status &rarr; typed-error mapping</h5>
 * <p>
 * The mixin validates the decoded params and blob BEFORE touching the store, and the store enforces the count quotas
 * atomically (see {@link SavedViewStore}).  The statuses below are pinned to what the slice-2 JS provider's
 * {@code classifyStatus} already assumes (a discrepancy the plan left open for slice 3): {@code 413}/{@code 429}/
 * {@code 507}&rarr;{@code quota}, {@code 401}/{@code 403}&rarr;{@code unavailable}, {@code >=500}&rarr;{@code network},
 * everything else&rarr;{@code malformed}.  So {@code 409}/{@code 422} could NOT be used for a quota rejection (the JS
 * would misfile them as {@code malformed}); the count quota is {@code 507} and the per-blob size cap is {@code 413}.
 * <table class='styled'>
 * 	<tr><th>Condition</th><th>HTTP</th><th>JS typed code</th></tr>
 * 	<tr><td>Absent/blank principal</td><td>{@code 401}</td><td>{@code unavailable}</td></tr>
 * 	<tr><td>Forged/absent CSRF or {@code Origin} (filter)</td><td>{@code 403}</td><td>{@code unavailable}</td></tr>
 * 	<tr><td>Blank/reserved/over-length name, missing {@code view}, non-JSON body</td><td>{@code 400}</td>
 * 		<td>{@code malformed}</td></tr>
 * 	<tr><td>Non-{@code application/json} write</td><td>{@code 415}</td><td>{@code malformed}</td></tr>
 * 	<tr><td>{@code load} of a missing name</td><td>{@code 404}</td>
 * 		<td>{@code malformed} (the JS {@code load} maps {@code 404}&rarr;{@code null} before classifying)</td></tr>
 * 	<tr><td>Blob exceeds {@link #MAX_BLOB_BYTES}</td><td>{@code 413}</td><td>{@code quota}</td></tr>
 * 	<tr><td>Per-scope / per-user count quota</td><td>{@code 507}</td><td>{@code quota}</td></tr>
 * </table>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@Rest</ja>
 * 	<jk>public class</jk> MyResource <jk>extends</jk> BasicRestServlet <jk>implements</jk> SavedViewsMixin {
 * 		<jc>// Uses the non-durable in-memory default; override savedViewStore() for a durable backend.</jc>
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link SavedViewStore}
 * 	<li class='jc'>{@link InMemorySavedViewStore}
 * 	<li class='jc'>{@link SavedViewQuotaException}
 * 	<li class='jc'>{@link org.apache.juneau.rest.server.filter.LoopbackBoundaryFilter}
 * </ul>
 *
 * @since 10.0.0
 */
public interface SavedViewsMixin {

	/** The fixed URL path prefix for the saved-views endpoints (relative to the host mount).  Not overridable. */
	String SAVED_VIEWS_PREFIX = "/juneau-saved-views";

	/** The {@code /item} sub-path (load/save/saveAndActivate/delete). */
	String ITEM_PATH = SAVED_VIEWS_PREFIX + "/item";

	/** The {@code /active} sub-path (set/clear the active pointer). */
	String ACTIVE_PATH = SAVED_VIEWS_PREFIX + "/active";

	/**
	 * Decoded saved-view-name cap.
	 *
	 * <p>
	 * A textually SEPARATE copy of {@code juneau-config.js}'s {@code MAX_NAME_LEN} (a stated, testable parity
	 * invariant &mdash; change one, change the other in the same commit) so the client and the mixin reject an
	 * over-length name at the exact same boundary.
	 */
	int MAX_NAME_LEN = 128;

	/**
	 * Per-blob size cap in bytes (64&nbsp;KB), a STATELESS check enforced here on the mixin (§3.2).
	 *
	 * <p>
	 * Mirrors {@code juneau-config.js}'s {@code LOCALSTORAGE_MAX_BLOB_BYTES}.  A blob whose UTF-8 length exceeds this
	 * is rejected with {@code 413} (the count quotas, which are stateful/race-sensitive, are the store's job and map
	 * to {@code 507} &mdash; see the interface javadoc).
	 */
	long MAX_BLOB_BYTES = 64L * 1024;

	/** The reserved saved-view name (case-insensitive) &mdash; it IS the catalog defaults and can never be saved. */
	String RESERVED_DEFAULT_NAME = "Default";

	/**
	 * Resolves the context-path-aware saved-views REST base for stamping {@code data-juneau-saved-views}
	 * on a table or page shell.
	 *
	 * <p>
	 * The JS server-provider reads that attribute via {@code table.closest('[data-juneau-saved-views]')} and
	 * fails closed when it is absent &mdash; never a hardcoded {@code /}-rooted fallback.  The mount itself is
	 * fixed ({@link #SAVED_VIEWS_PREFIX}); only the resolved URL varies with the servlet context path.
	 *
	 * @param req The current request.  Must not be <jk>null</jk>.
	 * @return The resolved URL of {@link #SAVED_VIEWS_PREFIX}.
	 */
	static String resolvedBaseUrl(RestRequest req) {
		return req.getUriResolver().resolve("servlet:" + SAVED_VIEWS_PREFIX);
	}

	/**
	 * Returns the {@link SavedViewStore} backing this resource's saved views.
	 *
	 * <p>
	 * Defaults to the process-wide, non-durable {@link InMemorySavedViewStore} (owner-locked in-memory default, item
	 * 8) so a host that overrides nothing still gets a working &mdash; if non-persistent &mdash; endpoint.  This
	 * DIFFERS deliberately from {@link AsyncJobsMixin#asyncJobRegistry()}, which is abstract with no default.  A
	 * consumer overrides this to supply a durable store.
	 *
	 * @return The store.  Never <jk>null</jk>.
	 */
	default SavedViewStore savedViewStore() {
		return InMemorySavedViewStore.shared();
	}

	/**
	 * [GET /juneau-saved-views] &mdash; list a scope's saved views and its resolved active pointer.
	 *
	 * @param req The REST request.
	 * @return <code>{active:&lt;name|null&gt;, views:[{name}]}</code> with dangling-active already resolved.
	 */
	@RestGet(path=SAVED_VIEWS_PREFIX, summary="List saved views", swagger=@OpSwagger(ignore=true))
	default JsonMap listSavedViews(RestRequest req) {
		var user = requirePrincipal(req);
		var viewId = requireViewId(req);
		var pageId = optionalPageId(req);
		var listing = savedViewStore().list(user, pageId, viewId);
		var views = new ArrayList<JsonMap>(listing.names().size());
		listing.names().forEach(n -> views.add(JsonMap.of("name", n)));
		return JsonMap.create().append("active", listing.active()).append("views", views);
	}

	/**
	 * [GET /juneau-saved-views/item] &mdash; load a single saved-view blob.
	 *
	 * @param req The REST request.
	 * @return The stored blob JSON.
	 */
	@RestGet(path=ITEM_PATH, summary="Load a saved view", swagger=@OpSwagger(ignore=true))
	default JsonMap loadSavedView(RestRequest req) {
		var user = requirePrincipal(req);
		var viewId = requireViewId(req);
		var pageId = optionalPageId(req);
		var name = requireValidName(nameParam(req));
		var blob = savedViewStore().load(user, pageId, viewId, name);
		if (blob == null)
			throw new NotFound("No saved view named '%s'.", name);
		return parseObject(blob, () -> new InternalServerError("Stored saved-view blob is not valid JSON."));
	}

	/**
	 * [PUT /juneau-saved-views/item] &mdash; save (or save+activate, with {@code ?activate=1}) a saved-view blob.
	 *
	 * @param req The REST request.
	 * @throws IOException If the request body could not be read.
	 */
	@RestPut(path=ITEM_PATH, summary="Save a saved view", swagger=@OpSwagger(ignore=true))
	default void saveSavedView(RestRequest req) throws IOException {
		var user = requirePrincipal(req);
		requireJsonContentType(req);
		var viewId = requireViewId(req);
		var pageId = optionalPageId(req);
		var name = requireValidName(nameParam(req));
		var blob = requireValidBlob(req.getContent().asString());
		try {
			if (isActivate(req))
				savedViewStore().saveAndActivate(user, pageId, viewId, name, blob);
			else
				savedViewStore().save(user, pageId, viewId, name, blob);
		} catch (SavedViewQuotaException e) {
			throw new InsufficientStorage(e.getMessage());
		}
	}

	/**
	 * [PUT /juneau-saved-views/active] &mdash; set the active pointer (body <code>{name}</code>) or clear it
	 * (body <code>{}</code> / <code>{name:null}</code>).
	 *
	 * @param req The REST request.
	 * @throws IOException If the request body could not be read.
	 */
	@RestPut(path=ACTIVE_PATH, summary="Set or clear the active saved view", swagger=@OpSwagger(ignore=true))
	default void setActiveSavedView(RestRequest req) throws IOException {
		var user = requirePrincipal(req);
		requireJsonContentType(req);
		var viewId = requireViewId(req);
		var pageId = optionalPageId(req);
		var body = parseObject(bodyOrEmpty(req.getContent().asString()),
			() -> new BadRequest("Request body is not a valid JSON object."));
		var name = body.getString("name");
		if (name != null)
			name = requireValidName(name);
		savedViewStore().setActive(user, pageId, viewId, name);
	}

	/**
	 * [DELETE /juneau-saved-views/item] &mdash; delete a saved view (a no-op if it does not exist).
	 *
	 * @param req The REST request.
	 */
	@RestDelete(path=ITEM_PATH, summary="Delete a saved view", swagger=@OpSwagger(ignore=true))
	default void deleteSavedView(RestRequest req) {
		var user = requirePrincipal(req);
		requireJsonContentType(req);
		var viewId = requireViewId(req);
		var pageId = optionalPageId(req);
		var name = requireValidName(nameParam(req));
		savedViewStore().delete(user, pageId, viewId, name);
	}

	// ------------------------------------------------------------------------------------------------------------------
	// Helpers (static so they stay off the endpoint surface and are unit-testable)
	// ------------------------------------------------------------------------------------------------------------------

	/**
	 * Resolves the canonical (trimmed) principal name, or answers {@code 401} when it is absent or blank.
	 *
	 * @param req The REST request.
	 * @return The canonical principal name.  Never <jk>null</jk> or blank.
	 */
	static String requirePrincipal(RestRequest req) {
		var p = req.getUserPrincipal();
		var name = p == null ? null : p.getName();
		if (name == null || name.isBlank())
			throw new Unauthorized("Authentication is required to access saved views.");
		return name.trim();
	}

	/**
	 * Resolves the required {@code view} query param, or answers {@code 400} when it is absent or blank.
	 *
	 * @param req The REST request.
	 * @return The view id.
	 */
	static String requireViewId(RestRequest req) {
		var v = req.getQueryParam("view").asString().orElse(null);
		if (v == null || v.isBlank())
			throw new BadRequest("Missing required query parameter 'view'.");
		return v;
	}

	/**
	 * Resolves the optional {@code page} query param, treating absent/blank as {@code null} (a standalone view).
	 *
	 * @param req The REST request.
	 * @return The page id, or <jk>null</jk>.
	 */
	static String optionalPageId(RestRequest req) {
		var v = req.getQueryParam("page").asString().orElse(null);
		return (v == null || v.isBlank()) ? null : v;
	}

	/**
	 * The raw (decoded) {@code name} query param, or <jk>null</jk> when absent.
	 *
	 * @param req The REST request.
	 * @return The name, or <jk>null</jk>.
	 */
	static String nameParam(RestRequest req) {
		return req.getQueryParam("name").asString().orElse(null);
	}

	/**
	 * Validates a saved-view name exactly as {@code juneau-config.js}'s {@code validateNameBasic} does &mdash; blank,
	 * over-{@link #MAX_NAME_LEN}, or reserved {@code Default} (case-insensitive) each answer {@code 400}.
	 *
	 * <p>
	 * Deliberately applies NO allowed-charset or encoded-segment-length check: those are localStorage-KEY concerns
	 * (the server keys on a structured tuple, so a {@code /} or {@code %} in a decoded name is harmless), and the
	 * slice-2 server provider itself validates with {@code validateNameBasic}, not the localStorage variant.
	 *
	 * @param name The decoded name.  Can be <jk>null</jk>.
	 * @return The name unchanged (not trimmed &mdash; the raw name is the stored key).
	 */
	static String requireValidName(String name) {
		if (name == null || name.isBlank())
			throw new BadRequest("Saved-view name must not be blank.");
		if (name.length() > MAX_NAME_LEN)
			throw new BadRequest("Saved-view name exceeds MAX_NAME_LEN (%s).", MAX_NAME_LEN);
		if (RESERVED_DEFAULT_NAME.equalsIgnoreCase(name.trim()))
			throw new BadRequest("'Default' is reserved and cannot be used as a saved-view name.");
		return name;
	}

	/**
	 * Enforces {@code Content-Type: application/json} on a write, answering {@code 415} otherwise (defense-in-depth
	 * with the loopback boundary filter; see the interface javadoc).  Base type only &mdash;
	 * {@code application/json;charset=utf-8} passes.
	 *
	 * @param req The REST request.
	 */
	static void requireJsonContentType(RestRequest req) {
		var ct = req.getContentType();
		var semi = ct == null ? -1 : ct.indexOf(';');
		var base = ct == null ? null : (semi < 0 ? ct : ct.substring(0, semi)).trim();
		if (base == null || ! base.equalsIgnoreCase("application/json"))
			throw new UnsupportedMediaType("A state-changing saved-views request must use content type 'application/json'.");
	}

	/**
	 * Validates the incoming blob: present, {@code <=} {@link #MAX_BLOB_BYTES} (else {@code 413}), and a valid JSON
	 * object (else {@code 400}).  The size check runs BEFORE the parse so an over-large body is rejected without
	 * being parsed.
	 *
	 * @param blob The raw request body.  Can be <jk>null</jk>.
	 * @return The blob unchanged.
	 */
	static String requireValidBlob(String blob) {
		if (blob == null || blob.isBlank())
			throw new BadRequest("A saved-view blob body is required.");
		if (blob.getBytes(StandardCharsets.UTF_8).length > MAX_BLOB_BYTES)
			throw new PayloadTooLarge("Saved view exceeds the per-blob size cap (%s bytes).", MAX_BLOB_BYTES);
		parseObject(blob, () -> new BadRequest("Saved-view blob body is not a valid JSON object."));
		return blob;
	}

	/**
	 * Whether the {@code ?activate=1} flag routes a save to {@link SavedViewStore#saveAndActivate}.  Any present,
	 * non-blank value activates; the fork is a QUERY flag, never a blob field (§3.3).
	 *
	 * @param req The REST request.
	 * @return <jk>true</jk> to save-and-activate.
	 */
	static boolean isActivate(RestRequest req) {
		var v = req.getQueryParam("activate").asString().orElse(null);
		return v != null && ! v.isBlank();
	}

	/** A blank/absent body is treated as {@code "{}"} (the JS always sends a real body, but a parser must not 400 on empty). */
	private static String bodyOrEmpty(String body) {
		return (body == null || body.isBlank()) ? "{}" : body;
	}

	/** Parses {@code json} as a JSON object, throwing the supplied HTTP exception (not a raw {@link ParseException}) on failure. */
	private static JsonMap parseObject(String json, java.util.function.Supplier<? extends RuntimeException> onError) {
		try {
			return JsonMap.ofString(json);
		} catch (ParseException e) {
			throw onError.get();
		}
	}
}
