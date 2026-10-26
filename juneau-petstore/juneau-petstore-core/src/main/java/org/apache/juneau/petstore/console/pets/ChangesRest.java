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
package org.apache.juneau.petstore.console.pets;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import org.apache.juneau.commons.beanquery.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.petstore.console.*;
import org.apache.juneau.petstore.console.pages.*;
import org.apache.juneau.petstore.service.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.console.datatables.*;
import org.apache.juneau.console.datatables.adapter.*;
import org.apache.juneau.rest.server.view.*;
import org.apache.juneau.rest.server.view.freemarker.*;
import org.apache.juneau.console.views.*;

/**
 * Pending changes: the staged-edits table.  Persistent selection is limited to {@code PENDING} rows, an aggregate
 * <b>Discard</b> returns a {@link BulkResult}, a perRow <b>Apply</b> writes each change to its pet, and a perRow
 * <b>Retry</b> re-applies a failed change.  Also the data source of the header pending-changes badge.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	selection: { rowIdField:'id', selectAll:true, scope:'persistent', labelField:'summary',
 * 	             selectableWhen:[{field:'state', op:'eq', value:'PENDING', reason:'Only pending changes can be discarded or applied.'}] },
 * 	bulk: { actions: [
 * 	  { id:'discard', label:'Discard', bulkMode:'aggregate', endpoint:'/console/pets/changes/discard', method:'POST', tone:'danger' },
 * 	  { id:'apply',   label:'Apply',   bulkMode:'perRow',    endpoint:'/console/pets/changes/{id}/apply', method:'POST' } ] }
 * </p>
 * <p class='bjava'>
 * 	<ja>@RestPost</ja>(path=<js>"/discard"</js>)
 * 	<jk>public</jk> BulkResult discard(RestRequest <jv>req</jv>, <ja>@Content</ja> BulkRequest <jv>body</jv>) {
 * 		<jk>var</jk> <jv>o</jv> = store().pendingChanges().discard(<jv>body</jv>.<jf>ids</jf>, ConsoleWrites.<jsm>actor</jsm>(<jv>req</jv>));
 * 		<jk>var</jk> <jv>b</jv> = BulkResult.<jsm>create</jsm>().succeeded(<jv>o</jv>.succeeded().toArray(String[]::<jk>new</jk>)).notFound(...);
 * 		<jv>o</jv>.failed().forEach(<jv>f</jv> -&gt; <jv>b</jv>.failed(<jk>new</jk> BulkResult.Failure(<jv>f</jv>.id(), <jv>f</jv>.message())));
 * 		<jk>return</jk> <jv>b</jv>.build();
 * 	}
 * </p>
 */
@Rest(path="/changes", title="Pending changes")
@SuppressWarnings({
	"java:S110" // Inheritance depth comes from the BasicRestServlet hierarchy, not this page.
})
public class ChangesRest extends PetstoreConsolePage {

	private static final long serialVersionUID = 1L;

	/** Replayed discard results by idempotency key (demo-sized; cleared when it grows past the bound). */
	private final transient Map<String,BulkResult> replays = new ConcurrentHashMap<>();

	/**
	 * The Pending changes page.
	 *
	 * @param req The request; {@code ?builder} renders the builder twin.
	 * @return The page.
	 */
	@RestGet(path="/")
	public View page(RestRequest req) {
		return builder(req) ? twin(req, ChangesPageBuilders.changes()) : FreemarkerView.of("changes.ftlh");
	}

	/**
	 * Server-mode DataTables query over staged changes.
	 *
	 * @param req The DataTables request.
	 * @return One page of changes, or a 200 {@code error} envelope for a malformed search expression.
	 */
	@RestPost(path="/query")
	public DataTablesResults<PendingChange> query(@Content DataTablesRequest req) {
		try (var s = store().queryPendingChanges()) {
			return DataTablesQuery.run(req, s);
		} catch (BeanQuerySyntaxException e) {
			return DataTablesResults.error(req.getDraw(), e.getMessage());
		}
	}

	/**
	 * The header badge source: pending changes, flagged {@code mine} for the caller.
	 *
	 * @param req The request.
	 * @return {@code {total, mine, items:[{id,label,href,mine,kind}]}}.
	 */
	@RestGet(path="/pending")
	public JsonMap pending(RestRequest req) {
		var me = ConsoleWrites.author(ConsoleWrites.actor(req));
		var items = new JsonList();
		var mine = 0;
		for (var c : store().pendingChanges().pending()) {
			var isMine = eq(c.getAuthor(), me);
			if (isMine)
				mine++;
			items.add(JsonMap.of("id", String.valueOf(c.getId()), "label", c.getSummary(), "href", f("/console/pets/%s/summary", c.getPetId()), "mine", isMine, "kind", "Pet"));
		}
		return JsonMap.of("total", items.size(), "mine", mine, "items", items);
	}

	/**
	 * Applies one change (the bulk perRow action).
	 *
	 * @param req The request.
	 * @param id The change id.
	 * @return {@code success} with the change when applied, {@code failure} with the reason when the write failed.
	 * @throws NotFound If the change does not exist.
	 * @throws Conflict If it is not pending.
	 */
	@RestPost(path="/{id}/apply")
	public ActionResult apply(RestRequest req, @Path("id") long id) {
		var actor = ConsoleWrites.actor(req);
		return outcome(call(() -> store().pendingChanges().apply(id, actor)));
	}

	/**
	 * Retries a failed change.
	 *
	 * @param req The request.
	 * @param id The change id.
	 * @return As {@link #apply(RestRequest, long)}.
	 * @throws NotFound If the change does not exist.
	 * @throws Conflict If it has not failed.
	 */
	@RestPost(path="/{id}/retry")
	public ActionResult retry(RestRequest req, @Path("id") long id) {
		var actor = ConsoleWrites.actor(req);
		return outcome(call(() -> store().pendingChanges().retry(id, actor)));
	}

	/**
	 * Discards changes in one aggregate call.  The same key gets the same answer.
	 *
	 * @param req The request.
	 * @param body {@code {ids, idempotencyKey}}.
	 * @param fail {@code discard} to simulate a transport failure (demo toggle).
	 * @return The per-id outcome.
	 * @throws BadRequest If the key or the ids are missing.
	 * @throws ServiceUnavailable On {@code ?fail=discard}.
	 */
	@RestPost(path="/discard")
	public BulkResult discard(RestRequest req, @Content BulkRequest body, @Query("fail") String fail) {
		var actor = ConsoleWrites.actor(req);
		if ("discard".equals(fail))
			throw new ServiceUnavailable("Discard is unavailable (demo toggle '%s')", fail);
		if (body.ids == null)
			throw new BadRequest("Missing 'ids'");
		var key = body.idempotencyKey;
		if (ib(key))
			throw new BadRequest("Missing 'idempotencyKey'");
		if (replays.size() > 1_000)
			replays.clear();
		return replays.computeIfAbsent(key, k -> {
			var o = store().pendingChanges().discard(body.ids, actor);
			var b = BulkResult.create().succeeded(o.succeeded().toArray(String[]::new)).notFound(o.notFound().toArray(String[]::new));
			o.failed().forEach(x -> b.failed(new BulkResult.Failure(x.id(), x.message())));
			return b.build();
		});
	}

	private static ActionResult outcome(PendingChange c) {
		return c.getState() == PendingChange.State.APPLIED
			? ActionResult.success(c).message(f("Applied '%s'", c.getSummary()))
			: ActionResult.failure().row(c).message(c.getMessage());
	}

	private static PendingChange call(Supplier<PendingChange> s) {
		try {
			return s.get();
		} catch (PetstoreNotFoundException e) {
			throw new NotFound(e.getMessage());
		} catch (PetstoreConflictException e) {
			throw new Conflict(e.getMessage());
		}
	}
}
