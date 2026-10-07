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
package org.apache.juneau.petstore.console.ops;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.http.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.petstore.console.*;
import org.apache.juneau.petstore.dto.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.view.*;
import org.apache.juneau.rest.server.view.freemarker.*;
import org.apache.juneau.rest.server.views.*;
import org.apache.juneau.rest.server.widgets.*;

/**
 * Jobs: the console's async-job reference.  A restock submits once per idempotency key, answers
 * 202 with an {@link AsyncJobRef}, and the views runtime follows its {@code streamUrl} until the job settles.
 *
 * <p>
 * The registry de-duplicates on the idempotency key itself ({@link AsyncJobRegistry#tryCreate(IdempotencyKey)}), so a
 * resubmit with the same key returns the same job and this resource keeps no key map of its own.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>var</jk> <jv>job</jv> = <jf>jobs</jf>.tryCreate(IdempotencyKey.<jsm>of</jsm>(<jv>key</jv>, <js>"restock"</js>, <js>"restock"</js>))
 * 		.orElseThrow(() -&gt; <jk>new</jk> TooManyRequests(<js>"Job limit reached"</js>));
 * 	<jv>pool</jv>.execute(() -&gt; {
 * 		<jv>job</jv>.markEffectStarted();
 * 		<jk>var</jk> <jv>pets</jv> = store().restock(<jv>body</jv>.perSpecies(), <js>"job:restock"</js>);
 * 		<jv>job</jv>.complete(ActionResult.<jsm>success</jsm>(<jk>null</jk>).message(<jsm>f</jsm>(<js>"Restocked %s pets"</js>, <jv>pets</jv>.size())));
 * 	});
 * 	<jk>return</jk> AsyncJobRef.<jsm>of</jsm>(<jv>job</jv>);
 * </p>
 */
@Rest(path="/jobs", title="Jobs")
@SuppressWarnings({
	"java:S110", // Inheritance depth comes from the BasicRestServlet hierarchy, not this demo page.
	"java:S1192", // Duplicated literals are wire keys of the job rows; constants would obscure the schema.
	"java:S2654", // The demo intentionally runs restocks on its own pool and serializes the idempotency check.
	"resource" // The registry and pool are owned by this resource and closed in closeJobs(); Eclipse JDT @Owning warning is by design.
})
public class JobsRest extends PetstoreConsolePage implements AsyncJobsMixin {

	private static final long serialVersionUID = 1L;

	/** System property: the most restocks that may run at once (default 2; the registry's own cap is 8). */
	public static final String LIMIT_PROPERTY = "petstore.jobs.limit";

	private static final String ACTION = "restock";

	private final transient AsyncJobRegistry jobs = new AsyncJobRegistry(Duration.ofSeconds(30));
	private final transient ExecutorService pool = Executors.newCachedThreadPool(r -> {
		var t = new Thread(r, "petstore-restock");
		t.setDaemon(true);
		return t;
	});
	/** Submitted jobs, newest first, for the Jobs table. */
	private final transient Deque<JsonMap> submitted = new ConcurrentLinkedDeque<>();

	/** How many submitted jobs the table remembers. */
	static final int MAX_SUBMITTED = 100;

	/**
	 * The restock request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Wire form: {perSpecies:{DOG:2, CAT:1}, idempotencyKey:'7f3c...'}</jc>
	 * 	<jk>var</jk> <jv>r</jv> = <jk>new</jk> Restock(Map.<jsm>of</jsm>(Species.<jsf>DOG</jsf>, 2), <js>"7f3c"</js>);
	 * </p>
	 *
	 * @param perSpecies How many pets of each species to add (1-50 each).
	 * @param idempotencyKey The client-minted key; resubmits with the same key return the same job.
	 */
	public record Restock(Map<Species,Integer> perSpecies, String idempotencyKey) {

		/**
		 * Reads a restock from either wire shape: the API form {@code {perSpecies:{DOG:2}, idempotencyKey}}, or the
		 * dialog form the views runtime posts, {@code {action, targetId, idempotencyKey, fields:{DOG:"2"}}} (blank
		 * or {@code 0} counts mean "none of that species").
		 *
		 * @param body The request body.
		 * @return The restock.
		 * @throws BadRequest If a species is unknown or a count is not a whole number.
		 */
		public static Restock of(JsonMap body) {
			var per = new LinkedHashMap<Species,Integer>();
			try {
				var api = body.getMap("perSpecies");
				var form = body.getMap("fields");
				if (api != null) {
					api.forEach((k, v) -> per.put(Species.valueOf(k), v == null ? null : Integer.valueOf(String.valueOf(v))));
				} else if (form != null) {
					form.forEach((k, v) -> {
						var s = v == null ? "" : String.valueOf(v).trim();
						if (! s.isEmpty() && ! "0".equals(s))
							per.put(Species.valueOf(k), Integer.valueOf(s));
					});
				}
			} catch (RuntimeException e) { // NumberFormat/IllegalArgument for bad counts or species; a conversion error when perSpecies/fields is not a map.
				throw new BadRequest("Restock counts must be whole numbers for known species");
			}
			return new Restock(per, body.getString("idempotencyKey"));
		}
	}

	@Override /* AsyncJobsMixin */
	public AsyncJobRegistry asyncJobRegistry() { return jobs; }

	/**
	 * Stops the restock pool and the registry's timeout scheduler when the resource is destroyed.
	 */
	@RestDestroy
	public void closeJobs() {
		pool.shutdownNow();
		jobs.close();
	}

	/**
	 * Renders the Jobs page.
	 *
	 * @return The page view.
	 */
	@RestGet(path="/")
	public View page() { return FreemarkerView.of("jobs.ftlh"); }

	/**
	 * The restock dialog's form source: a read-only GET returning the modal with a freshly minted self-targeted key.
	 *
	 * @return The modal.
	 */
	@RestGet(path="/restock-form")
	public ModalDef restockForm() {
		var form = FormDef.create();
		for (var s : Species.values())
			form.field(FormDef.Input.of(s.name(), s.name().charAt(0) + s.name().substring(1).toLowerCase() + " (0-50)", "text").value("0"));
		return ModalDef.create("Restock the store").form(form)
			.idempotencyKey(IdempotencyKey.mintSelfTargeted(ACTION).value()).selfTargeted(true);
	}

	/**
	 * @return Submitted jobs, newest first, with their current state.
	 */
	@RestGet(path="/rows")
	public List<JsonMap> rows() {
		return submitted.stream().map(x -> {
			var row = new JsonMap(state(x.getString("jobId"))).append(x);
			row.remove("idempotencyKey");
			return row;
		}).toList();
	}

	/**
	 * Submits a restock.
	 *
	 * @param req The request.
	 * @param res The response (202).
	 * @param json The restock body (see {@link Restock#of(JsonMap)}).
	 * @return The job ref.
	 * @throws BadRequest On a missing key or out-of-range count.
	 * @throws TooManyRequests When the limit is reached.
	 */
	@RestPost(path="/restock")
	public AsyncJobRef restock(RestRequest req, RestResponse res, @Content JsonMap json) {
		var actor = ConsoleWrites.actor(req);
		var body = Restock.of(json);
		var k = body.idempotencyKey();
		if (k == null || k.isBlank())
			throw new BadRequest("Missing 'idempotencyKey'");
		var key = IdempotencyKey.of(k, ACTION, k.equals(json.getString("targetId")) ? k : ACTION);
		if (body.perSpecies().isEmpty())
			throw new BadRequest("Nothing to restock");
		body.perSpecies().forEach((s, n) -> {
			if (n == null || n < 1 || n > 50)
				throw new BadRequest("Count for '%s' must be between 1 and 50", s);
		});
		var limit = Integer.getInteger(LIMIT_PROPERTY, 2);
		AsyncJob job;
		synchronized (submitted) {
			// Resolve the idempotency key first: a replay returns its existing job and never counts against the limit.
			var prior = submitted.stream().filter(x -> k.equals(x.getString("idempotencyKey"))).findFirst();
			job = prior.flatMap(x -> jobs.get(x.getString("jobId"))).orElse(null);
			if (job == null) {
				prior.ifPresent(submitted::remove); // The job aged out of the registry; this is a fresh submission.
				if (jobs.runningCount() >= limit)
					throw new TooManyRequests("Job limit reached; '%s' running", jobs.runningCount());
				var created = jobs.tryCreate(key).orElseThrow(() -> new TooManyRequests("Job limit reached; '%s' running", jobs.runningCount()));
				job = created;
				submitted.addFirst(JsonMap.of("jobId", created.id(), "submitted", created.createdAt().toString(), "request", body.perSpecies(), "idempotencyKey", k));
				while (submitted.size() > MAX_SUBMITTED)
					submitted.removeLast();
				pool.execute(() -> {
					created.markEffectStarted();
					try {
						var pets = store().restock(body.perSpecies(), "job:restock:" + actor);
						created.complete(ActionResult.success(null).message(f("Restocked %s pets", pets.size())));
					} catch (RuntimeException e) {
						created.complete(ActionResult.failure().message(e.getMessage()));
					}
				});
			}
		}
		res.setStatus(202);
		return AsyncJobRef.of(job);
	}

	/**
	 * @param id The job id.
	 * @return {@code {state: RUNNING|DONE, result}}.
	 * @throws NotFound If the job is unknown or has aged out.
	 */
	@RestGet(path="/{id}")
	public JsonMap job(@Path("id") String id) {
		if (jobs.get(id).isEmpty())
			throw new NotFound("Unknown job '%s'", id);
		return state(id);
	}

	private JsonMap state(String id) {
		var j = jobs.get(id);
		if (j.isEmpty())
			return JsonMap.of("state", "EXPIRED");
		return j.get().isTerminal()
			? JsonMap.of("state", "DONE", "result", j.get().result())
			: JsonMap.of("state", "RUNNING");
	}
}
