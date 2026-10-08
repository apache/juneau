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

import java.io.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.http.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.marshaller.*;
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
 * <p>
 * The page also hosts the console-output demo: a "groom pet" job ({@link GroomRuns}) whose log is served by
 * {@link ConsoleOutputMixin} and shown three ways &mdash; a FreeMarker console card over the in-memory log, a second
 * card over the same run's file-backed transcript, and a compact console in the Groom runs table's row detail.
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
public class JobsRest extends PetstoreConsolePage implements AsyncJobsMixin, ConsoleOutputMixin {

	private static final long serialVersionUID = 1L;

	/** System property: the most restocks that may run at once (default 2; the registry's own cap is 8). */
	public static final String LIMIT_PROPERTY = "petstore.jobs.limit";

	private static final String ACTION = "restock";

	/** Where this resource is mounted; the console-output URLs are built from it. */
	public static final String MOUNT = "/console/ops/jobs";

	private static final String GROOM = "groom";

	/** The groom-pet demo runs; one verbose run is seeded so the page always has a log. */
	private final transient GroomRuns groom = GroomRuns.seeded(Long.getLong(GroomRuns.PERIOD_PROPERTY, GroomRuns.DEFAULT_PERIOD_MS));

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
	 * Stops the restock pool, the registry's timeout scheduler and the groom runs when the resource is destroyed.
	 */
	@RestDestroy
	public void closeJobs() {
		pool.shutdownNow();
		jobs.close();
		groom.close();
	}

	/**
	 * Renders the Jobs page.
	 *
	 * @param run The groom run to show, or <jk>null</jk> for the newest.
	 * @return The page view.
	 * @throws NotFound If there is no groom run to show.
	 */
	@RestGet(path="/")
	public View page(@Query("run") String run) {
		var r = groom.get(run).orElseGet(groom::latest);
		if (r == null)
			throw new NotFound("No groom run to show");
		var main = ConsoleOutputDef.forMixin("groom-output", MOUNT, r.id()).toMap();
		var file = ConsoleOutputDef.forMixin("groom-file", MOUNT, r.id() + GroomRuns.FILE_SUFFIX).toMap();
		var detail = ConsoleOutputDef.forMixin("output", MOUNT, "{id}").type(RegionDef.TYPE_ROW_DETAIL).compact(true).rows(12).validate().toMap();
		return FreemarkerView.of("jobs.ftlh")
			.attr("groomLinesUrl", main.get("linesUrl"))
			.attr("groomDownloadUrl", main.get("downloadUrl"))
			.attr("groomFileLinesUrl", file.get("linesUrl"))
			.attr("groomFileDownloadUrl", file.get("downloadUrl"))
			.attr("groomDetailParams", scriptSafe(Json.of(detail)));
	}

	/**
	 * Makes JSON safe to inline in a {@code <script>} block: {@code </} cannot end the block and the two line
	 * separators cannot end a JavaScript string.
	 *
	 * @param json The JSON text.
	 * @return The text with {@code </} as {@code <\/}, U+2028 as {@code \u2028} and U+2029 as {@code \u2029}.
	 */
	static String scriptSafe(String json) {
		return json.replace("</", "<\\/").replace("\u2028", "\\u2028").replace("\u2029", "\\u2029");
	}

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

	@Override /* ConsoleOutputMixin */
	public Optional<ConsoleOutputSource> consoleOutputSource(String logId, RestRequest req) {
		return groom.source(logId); // The run id is an unguessable 128-bit secret; knowing it is the access rule.
	}

	/**
	 * The Groom dialog's form source.
	 *
	 * @return The modal.
	 */
	@RestGet(path="/groom-form")
	public ModalDef groomForm() {
		var form = FormDef.create();
		form.field(FormDef.Input.of("pet", "Pet name", "text").value("Rex"));
		form.field(FormDef.Input.of("verbose", "Verbose (8000 extra lines)", "checkbox"));
		return ModalDef.create("Groom a pet").form(form)
			.idempotencyKey(IdempotencyKey.mintSelfTargeted(GROOM).value()).selfTargeted(true);
	}

	/**
	 * Starts a groom run.
	 *
	 * <p>
	 * Accepts the API form {@code {pet, verbose}} or the dialog form {@code {action, targetId, idempotencyKey,
	 * fields:{pet, verbose}}}; a resubmit with the same {@code idempotencyKey} returns the same run.
	 *
	 * @param json The body.
	 * @return The new run's row.
	 * @throws IOException If the run's temp file cannot be created.
	 */
	@RestPost(path="/groom")
	public ActionResult groom(@Content JsonMap json) throws IOException {
		var fields = json.getMap("fields");
		var src = fields != null ? fields : json;
		var run = groom.start(src.getString("pet"), isOn(src.get("verbose")), json.getString("idempotencyKey"));
		return ActionResult.success(run.row()).message(f("Grooming %s", run.pet()));
	}

	private static boolean isOn(Object v) {
		var s = v == null ? "" : String.valueOf(v);
		return "true".equals(s) || "on".equals(s);
	}

	/**
	 * @return The groom runs, newest first.
	 */
	@RestGet(path="/groom-rows")
	public List<JsonMap> groomRows() {
		return groom.list().stream().map(GroomRuns.Run::row).toList();
	}

	/**
	 * The Groom runs table's row-detail envelope.
	 *
	 * @param id The run id.
	 * @return {@code {contractVersion:'1', fields:{...}}}.
	 * @throws NotFound If the run is unknown or has been dropped.
	 */
	@RestGet(path="/groom-runs/{id}")
	public JsonMap groomRun(@Path("id") String id) {
		var r = groom.get(id).orElseThrow(() -> new NotFound("Unknown groom run '%s'", id));
		return JsonMap.of("contractVersion", "1", "fields", r.row());
	}

	/**
	 * The same-origin image the groom log shows.
	 *
	 * @param res The response.
	 * @throws IOException If the response cannot be written.
	 */
	@RestGet(path="/groom-photo.svg")
	public void groomPhoto(RestResponse res) throws IOException {
		var w = res.getDirectWriter("image/svg+xml");
		w.write(GroomRuns.PHOTO_SVG);
		w.flush();
	}
}
