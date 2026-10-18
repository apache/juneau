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
import java.util.concurrent.atomic.*;
import java.util.function.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.petstore.console.*;
import org.apache.juneau.petstore.console.pages.*;
import org.apache.juneau.petstore.dto.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.view.*;
import org.apache.juneau.rest.server.view.freemarker.*;
import org.apache.juneau.rest.server.views.*;
import org.apache.juneau.rest.server.terminal.*;
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
 * The page's {@code job-log} card posts the same restock in its flat form and follows the job's stream.  A restock
 * with {@code slowMs} waits that long before it touches the store (cancel it then and nothing changes), and every
 * restock holds for {@code holdMs} (a second by default) after it has added its pets (cancel it then and the result
 * says the change was made).
 * The registry's hard timeout is 30 s, or whatever a {@link HardTimeout} bean on the host says.
 *
 * <p>
 * The page also hosts the terminal demo: a "groom pet" job ({@link GroomRuns}) whose output is served by
 * {@link TerminalMixin} and shown two ways &mdash; a FreeMarker terminal card over the in-memory output, and a second
 * card over the same run's file-backed transcript.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// POST /console/ops/jobs/restock {DOG:2, slowMs:3000, holdMs:1000, idempotencyKey:'7f3c...'}</jc>
 * 	<jk>var</jk> <jv>body</jv> = Restock.<jsm>of</jsm>(<jv>json</jv>);
 * 	<jk>var</jk> <jv>job</jv> = asyncJobRegistry().tryCreate(IdempotencyKey.<jsm>of</jsm>(<jv>body</jv>.idempotencyKey(), <js>"restock"</js>, <js>"restock"</js>)).orElse(<jk>null</jk>);
 * 	<jk>if</jk> (<jv>job</jv> == <jk>null</jk>)
 * 		<jk>return</jk> ActionResult.<jsm>refusal</jsm>(<js>"app:job-limit"</js>);  <jc>// answered 429</jc>
 * 	<jv>pool</jv>.execute(() -&gt; run(<jv>job</jv>, <jv>body</jv>, () -&gt; store().restock(<jv>body</jv>.perSpecies(), <js>"job:restock"</js>)));
 * 	<jk>return</jk> AsyncJobRef.<jsm>of</jsm>(<jv>job</jv>);  <jc>// answered 202</jc>
 * </p>
 */
@Rest(path="/jobs", title="Jobs")
@SuppressWarnings({
	"java:S110", // Inheritance depth comes from the BasicRestServlet hierarchy, not this demo page.
	"java:S1192", // Duplicated literals are wire keys of the job rows; constants would obscure the schema.
	"java:S2654", // The demo intentionally runs restocks on its own pool and serializes the idempotency check.
	"resource" // The registry and pool are owned by this resource and closed in closeJobs(); Eclipse JDT @Owning warning is by design.
})
public class JobsRest extends PetstoreConsolePage implements AsyncJobsMixin, TerminalMixin {

	private static final long serialVersionUID = 1L;

	/** System property: the most restocks that may run at once (default 2; the registry's own cap is 8). */
	public static final String LIMIT_PROPERTY = "petstore.jobs.limit";

	/**
	 * System property: how long a species row's restock holds after its effect, in milliseconds (default {@value #DEFAULT_HOLD_MS}, at most {@value #MAX_HOLD_MS}).
	 *
	 * <p>
	 * Read on every request, so a test can widen the window in which a cancel lands as {@code cancelled-after-effect}.
	 * A sample-only knob: the row form never accepts a hold.
	 */
	public static final String SPECIES_HOLD_PROPERTY = "petstore.jobs.speciesHoldMs";

	private static final String ACTION = "restock";

	/** Where this resource is mounted; the terminal URLs are built from it. */
	public static final String MOUNT = "/console/ops/jobs";

	private static final String GROOM_JOB = "groom";

	/** The species row's restock action; its keys come from the ledger, bound to the species and the caller. */
	static final String RESTOCK_SPECIES = "restock-species";

	/** The groom run row's "groom again" action. */
	static final String REGROOM = "regroom";

	/** Row keys: issued by the {@code /key} endpoints, checked and claimed by the row submits. */
	private final transient JobKeyLedger jobKeys = JobKeyLedger.create();

	/** When each species was last restocked from its row, for the row's detail panel. */
	private final transient Map<Species,Instant> lastRestock = new ConcurrentHashMap<>();

	/** The groom-pet demo runs; one verbose run is seeded so the page always has a log. */
	private final transient GroomRuns groom = GroomRuns.seeded(Long.getLong(GroomRuns.PERIOD_PROPERTY, GroomRuns.DEFAULT_PERIOD_MS));

	/** The default hard timeout of a restock job. */
	public static final Duration DEFAULT_HARD_TIMEOUT = Duration.ofSeconds(30);

	/** The longest {@code slowMs} a restock accepts. */
	public static final int MAX_SLOW_MS = 10_000;

	/** The longest {@code holdMs} a restock accepts. */
	public static final int MAX_HOLD_MS = 10_000;

	/** How long a restock holds after its effect by default, so a late cancel lands as {@code cancelled-after-effect}. */
	public static final int DEFAULT_HOLD_MS = 1000;

	private static final long SLOW_STEP_MS = 100;

	/**
	 * A per-host override of the jobs hard timeout.  Declare it as a {@code @Bean} on the host that mounts the
	 * console; it is per instance, so one test host's short timeout does not leak into another.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<ja>@Bean</ja> <jk>public</jk> JobsRest.HardTimeout jobsHardTimeout() {
	 * 		<jk>return new</jk> JobsRest.HardTimeout(Duration.<jsm>ofSeconds</jsm>(5));
	 * 	}
	 * </p>
	 *
	 * @param value The hard timeout of every restock job.  Must be positive.
	 */
	public record HardTimeout(Duration value) {}

	/**
	 * A per-host clock for the row-key ledger, so a test can age a key past its TTL.  Declare it as a {@code @Bean} on
	 * the host, as {@link HardTimeout}.
	 *
	 * @param value The clock.
	 */
	public record KeyClock(Clock value) {}

	@SuppressWarnings({
		"java:S2226" // the field is set by the framework's @Bean injection after construction, so it cannot be final.
	})
	@Bean
	private transient HardTimeout hardTimeout;

	@SuppressWarnings({
		"java:S2226" // the field is set by the framework's @Bean injection after construction, so it cannot be final.
	})
	@Bean
	private transient KeyClock keyClock;

	private final transient AtomicReference<AsyncJobRegistry> jobs = new AtomicReference<>();
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
	 * 	<jk>var</jk> <jv>r</jv> = <jk>new</jk> Restock(Map.<jsm>of</jsm>(Species.<jsf>DOG</jsf>, 2), <js>"7f3c"</js>, 0, 1000);
	 * </p>
	 *
	 * @param perSpecies How many pets of each species to add (1-50 each).
	 * @param idempotencyKey The client-minted key; resubmits with the same key return the same job.
	 * @param slowMs How long to wait before restocking, in whole milliseconds (0-{@value JobsRest#MAX_SLOW_MS}), so a demo can
	 * 	cancel before the effect.
	 * @param holdMs How long to hold after restocking, in whole milliseconds (0-{@value JobsRest#MAX_HOLD_MS}, default
	 * 	{@value JobsRest#DEFAULT_HOLD_MS}), so a demo can cancel after the effect.
	 */
	public record Restock(Map<Species,Integer> perSpecies, String idempotencyKey, int slowMs, int holdMs) {

		/**
		 * Reads a restock from any of its wire shapes: the API form {@code {perSpecies:{DOG:2}, idempotencyKey}}, the
		 * dialog form the views runtime posts, {@code {action, targetId, idempotencyKey, fields:{DOG:"2"}}}, or the
		 * job-log card's flat form {@code {DOG:2, slowMs:0, idempotencyKey}}.  In the dialog and flat forms a blank,
		 * <jk>null</jk> or {@code 0} count means "none of that species".  Any form may carry {@code slowMs} and
		 * {@code holdMs}; each must be a whole number of milliseconds, so a decimal such as {@code 1.5} is rejected
		 * rather than truncated.
		 *
		 * @param body The request body.
		 * @return The restock.
		 * @throws BadRequest If a species is unknown, a count is not a whole number, {@code slowMs} is not a whole
		 * 	number 0-{@value JobsRest#MAX_SLOW_MS}, or {@code holdMs} is not a whole number 0-{@value JobsRest#MAX_HOLD_MS}.
		 */
		public static Restock of(JsonMap body) {
			var per = new LinkedHashMap<Species,Integer>();
			try {
				var api = body.getMap("perSpecies");
				var form = body.getMap("fields");
				if (api != null) {
					api.forEach((k, v) -> per.put(Species.valueOf(k), v == null ? null : Integer.valueOf(String.valueOf(v))));
				} else if (form != null) {
					form.forEach((k, v) -> putCount(per, Species.valueOf(k), v));
				} else {
					for (var s : Species.values())
						putCount(per, s, body.get(s.name()));
				}
			} catch (RuntimeException e) { // NumberFormat/IllegalArgument for bad counts or species; a conversion error when perSpecies/fields is not a map.
				throw new BadRequest("Restock counts must be whole numbers for known species");
			}
			return new Restock(per, body.getString("idempotencyKey"), millis(body, "slowMs", 0, MAX_SLOW_MS), millis(body, "holdMs", DEFAULT_HOLD_MS, MAX_HOLD_MS));
		}

		private static void putCount(Map<Species,Integer> per, Species s, Object v) {
			var x = v == null ? "" : String.valueOf(v).trim();
			if (! x.isEmpty() && ! "0".equals(x))
				per.put(s, Integer.valueOf(x));
		}

		private static int millis(JsonMap body, String name, int dflt, int max) {
			var v = body.get(name);
			if (v == null || String.valueOf(v).isBlank())
				return dflt;
			try {
				var n = Integer.parseInt(String.valueOf(v).trim());
				if (n >= 0 && n <= max)
					return n;
			} catch (NumberFormatException e) {
				// Not a whole number (a decimal included): falls through to the 400 below.
			}
			throw new BadRequest("Invalid %s '%s'; must be a whole number 0-%s", name, v, max);
		}
	}

	/** @return The row-key ledger, on the host's {@link KeyClock} if it declares one. */
	private JobKeyLedger jobKeys() {
		var c = keyClock;
		return c == null ? jobKeys : jobKeys.clock(c.value());
	}

	@Override /* AsyncJobsMixin */
	public AsyncJobRegistry asyncJobRegistry() {
		var j = jobs.get();
		if (j == null) {
			synchronized (this) {
				j = jobs.get();
				if (j == null) {
					j = new AsyncJobRegistry(hardTimeout == null ? DEFAULT_HARD_TIMEOUT : hardTimeout.value());
					jobs.set(j);
				}
			}
		}
		return j;
	}

	/**
	 * Stops the restock pool, the registry's timeout scheduler and the groom runs when the resource is destroyed.
	 */
	@RestDestroy
	public void closeJobs() {
		pool.shutdownNow();
		var j = jobs.get();
		if (j != null)
			j.close();
		groom.close();
	}

	/**
	 * Renders the Jobs page.
	 *
	 * @param req The request; {@code ?builder} renders the builder twin.
	 * @param run The groom run to show, or <jk>null</jk> for the newest.
	 * @return The page view.
	 * @throws NotFound If there is no groom run to show.
	 */
	@RestGet(path="/")
	public View page(RestRequest req, @Query("run") String run) {
		var r = groom.get(run).orElseGet(groom::latest);
		if (r == null)
			throw new NotFound("No groom run to show");
		var main = terminal("groom-output", r.id(), "Groom log");
		var file = terminal("groom-file", r.id() + GroomRuns.FILE_SUFFIX, "Transcript file");
		if (builder(req))
			return twin(req, OpsPageBuilders.jobs(main, file));
		return FreemarkerView.of("jobs.ftlh")
			.attr("groomBytesUrl", main.bytesUrl)
			.attr("groomFileBytesUrl", file.bytesUrl);
	}

	private static TerminalDef terminal(String id, String terminalId, String title) {
		var def = TerminalDef.forMixin(id, MOUNT, terminalId).title(title);
		def.inputId = null; // Output only: the groom terminals take no keystrokes.
		return def.validate();
	}

	/**
	 * The restock dialog's form source: a read-only GET returning the modal with a freshly minted self-targeted key.
	 *
	 * <p>
	 * The dialog is the plain restock, so it asks only for counts and always runs with no wait and the default
	 * {@value #DEFAULT_HOLD_MS} ms hold.  The timing knobs ({@code slowMs}, {@code holdMs}) belong to the job-log card,
	 * which exists to demonstrate cancelling before and after the effect.
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
	 * @return The job ref (202), or a 429 {@code app:job-limit} refusal whose message names the running count, so a
	 * 	JSON client such as the job-log card can show it.
	 * @throws BadRequest On a missing key, an out-of-range count, or a bad {@code slowMs} or {@code holdMs} (see
	 * 	{@link Restock#of(JsonMap)}).
	 */
	@RestPost(path="/restock")
	public Object restock(RestRequest req, RestResponse res, @Content JsonMap json) {
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
		var registry = asyncJobRegistry();
		AsyncJob job;
		synchronized (submitted) {
			// Resolve the idempotency key first: a replay returns its existing job and never counts against the limit.
			var prior = submitted.stream().filter(x -> k.equals(x.getString("idempotencyKey"))).findFirst();
			job = prior.flatMap(x -> registry.get(x.getString("jobId"))).orElse(null);
			if (job == null) {
				prior.ifPresent(submitted::remove); // The job aged out of the registry; this is a fresh submission.
				var created = registry.runningCount() >= limit ? null : registry.tryCreate(key).orElse(null);
				if (created == null)
					return limitReached(res, registry.runningCount());
				job = created;
				submitted.addFirst(JsonMap.of("jobId", created.id(), "submitted", created.createdAt().toString(), "request", body.perSpecies(), "idempotencyKey", k));
				while (submitted.size() > MAX_SUBMITTED)
					submitted.removeLast();
				pool.execute(() -> run(created, body, () -> store().restock(body.perSpecies(), "job:restock:" + actor)));
			}
		}
		res.setStatus(202);
		return AsyncJobRef.of(job);
	}

	private static ActionResult limitReached(RestResponse res, int running) {
		res.setStatus(429);
		return ActionResult.refusal("app:job-limit").message(f("Job limit reached; '%s' running", running));
	}

	/**
	 * Runs one restock: the optional slow wait (cancellable, nothing changed yet), the restock itself, then the
	 * after-effect hold.  Every step stops as soon as the job has settled (a cancel or the hard timeout).
	 *
	 * @param job The job.
	 * @param body The restock.
	 * @param effect The restock itself; returns the added pets.
	 */
	static void run(AsyncJob job, Restock body, Supplier<List<Pet>> effect) {
		run(job, body, effect, () -> null);
	}

	/**
	 * Same as {@link #run(AsyncJob, Restock, Supplier)}, but the success result carries a value, read after the
	 * effect: a row job returns its row's new state, which the card's {@code output} writes back into the row.
	 *
	 * @param job The job.
	 * @param body The restock.
	 * @param effect The restock itself; returns the added pets.
	 * @param value The success result's value.
	 */
	static void run(AsyncJob job, Restock body, Supplier<List<Pet>> effect, Supplier<Object> value) {
		try {
			if (body.slowMs() > 0) {
				job.progress(f("Waiting %s ms before restocking", body.slowMs()));
				if (! hold(job, body.slowMs()))
					return;
			}
			// One lock acquisition, shared with cancel and the timeout: a job settled after the wait is left as it was.
			if (! job.markEffectStarted())
				return;
			var total = body.perSpecies().values().stream().mapToInt(Integer::intValue).sum();
			job.progress(f("Restocking %s pets", total));
			var pets = effect.get();
			pets.forEach(p -> job.progress(f("Added %s", p.getName())));
			if (hold(job, body.holdMs()))
				job.complete(ActionResult.success(value.get()).message(f("Restocked %s pets", pets.size())));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			job.complete(ActionResult.unknown().message("Interrupted"));
		} catch (RuntimeException e) {
			job.complete(ActionResult.failure().message(e.getMessage()));
		}
	}

	/** Sleeps in short steps; returns <jk>false</jk> as soon as the job has settled. */
	private static boolean hold(AsyncJob job, long ms) throws InterruptedException {
		for (var left = ms; left > 0; left -= SLOW_STEP_MS) {
			if (job.isTerminal())
				return false;
			Thread.sleep(Math.min(SLOW_STEP_MS, left));
		}
		return ! job.isTerminal();
	}

	/**
	 * @param id The job id.
	 * @return {@code {state: RUNNING|DONE, result}}.
	 * @throws NotFound If the job is unknown or has aged out.
	 */
	@RestGet(path="/{id}")
	public JsonMap job(@Path("id") String id) {
		if (asyncJobRegistry().get(id).isEmpty())
			throw new NotFound("Unknown job '%s'", id);
		return state(id);
	}

	private JsonMap state(String id) {
		var j = asyncJobRegistry().get(id);
		if (j.isEmpty())
			return JsonMap.of("state", "EXPIRED");
		return j.get().isTerminal()
			? JsonMap.of("state", "DONE", "result", j.get().result())
			: JsonMap.of("state", "RUNNING");
	}

	@Override /* TerminalMixin */
	public Optional<TerminalSource> terminalSource(String id, RestRequest req) {
		return groom.source(id); // The run id is an unguessable 128-bit secret; knowing it is the access rule.
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
			.idempotencyKey(IdempotencyKey.mintSelfTargeted(GROOM_JOB).value()).selfTargeted(true);
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

	//-----------------------------------------------------------------------------------------------------------------
	// Row jobs: a key bound to one row, issued by the server and checked on submit.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * @return One row per species: {@code {id, species, available}}.
	 */
	@RestGet(path="/species")
	public List<JsonMap> speciesRows() {
		return Arrays.stream(Species.values()).map(this::speciesRow).toList();
	}

	/**
	 * A species row's detail panel.
	 *
	 * @param id The species name, for example {@code DOG}.
	 * @return {@code {contractVersion, fields:{available, lastRestock}}}.
	 * @throws NotFound If the species is unknown.
	 */
	@RestGet(path="/species/{id}")
	public JsonMap speciesDetail(@Path("id") String id) {
		var s = species(id);
		var last = lastRestock.get(s);
		var fields = new JsonMap().append("available", available(s)).append("lastRestock", last == null ? null : last.toString());
		return RowDetail.response(fields);
	}

	/**
	 * Issues a restock key bound to one species and the caller.
	 *
	 * <p>
	 * A POST, so it sits behind the console's CSRF boundary like every other console write; it changes nothing but the
	 * key ledger.
	 *
	 * @param req The request.
	 * @param id The species name.
	 * @return {@code {contractVersion, action, targetId, idempotencyKey}}.
	 * @throws NotFound If the species is unknown.
	 */
	@RestPost(path="/species/{id}/key")
	public Map<String,Object> speciesKey(RestRequest req, @Path("id") String id) {
		var s = species(id);
		return JobKeyLedger.response(jobKeys().issue(RESTOCK_SPECIES, s.name(), ConsoleWrites.actor(req)));
	}

	/**
	 * Restocks one species from its row's detail panel.
	 *
	 * <p>
	 * The species is the path's {@code {id}}, which must equal the body's {@code targetId}, which the key must be bound
	 * to.  The body's {@code fields} supply only {@code count}; any other field (the panel's hidden {@code species}
	 * included) is ignored.  A replay of a claimed key returns the same job, or a {@link JobKeyLedger#SPENT_CODE}
	 * refusal once the registry has aged the job out.
	 *
	 * @param req The request.
	 * @param res The response (202, or 409 on a refusal).
	 * @param id The species name.
	 * @param json The action envelope {@code {action, targetId, idempotencyKey, fields:{count}}}.
	 * @return The job ref (202), a 409 refusal, or a 429 {@code app:job-limit} refusal.
	 * @throws NotFound If the species is unknown.
	 * @throws BadRequest If the envelope is malformed or {@code count} is not a whole number 1-50.
	 */
	@RestPost(path="/species/{id}/restock")
	public Object restockSpecies(RestRequest req, RestResponse res, @Path("id") String id, @Content JsonMap json) {
		var s = species(id);
		var submit = RowJobSubmit.parse(json);
		if (! s.name().equals(submit.targetId()))
			return refused(res, RowJobSubmit.bindingRefusal());
		var actor = ConsoleWrites.actor(req);
		var ledger = jobKeys();
		var registry = asyncJobRegistry();
		AsyncJob job;
		// The ledger's own monitor: the verify that reads the claim and the claim itself happen under one lock, so of
		// two concurrent submits of one key the second sees the first's job and replays it instead of starting it again.
		synchronized (ledger) {
			var grant = submit.verify(ledger, RESTOCK_SPECIES, actor).orElse(null);
			if (grant == null)
				return refused(res, RowJobSubmit.bindingRefusal());
			var claimed = grant.jobId().orElse(null);
			if (claimed != null) {
				var r = replay(claimed, registry);
				res.setStatus(r instanceof ActionResult ? RowJobSubmit.REFUSAL_STATUS : 202);
				return r;
			}
			// After the replay check: a replay answers its job even if this resubmit's fields have changed.
			var body = new Restock(Map.of(s, count(submit.fields())), grant.key().value(), 0,
				Math.max(0, Math.min(Integer.getInteger(SPECIES_HOLD_PROPERTY, DEFAULT_HOLD_MS), MAX_HOLD_MS)));
			var created = registry.runningCount() >= Integer.getInteger(LIMIT_PROPERTY, 2) ? null : registry.tryCreate(grant.key()).orElse(null);
			if (created == null)
				return limitReached(res, registry.runningCount());
			ledger.claim(grant.key(), created.id());
			job = created;
			remember(created, body);
			pool.execute(() -> run(created, body, () -> restocked(s, body, actor), () -> speciesRow(s)));
		}
		res.setStatus(202);
		return AsyncJobRef.of(job);
	}

	/**
	 * A claimed key's replay: its job's ref, or a {@link JobKeyLedger#SPENT_CODE} refusal once the registry has reaped
	 * the job.  Never a second job: the key was spent on the first.
	 *
	 * @param jobId The job the key was claimed for.
	 * @param registry The registry.
	 * @return The {@link AsyncJobRef}, or the refusal.
	 */
	static Object replay(String jobId, AsyncJobRegistry registry) {
		return registry.get(jobId).<Object>map(AsyncJobRef::of)
			.orElseGet(() -> ActionResult.refusal(JobKeyLedger.SPENT_CODE).message("This key's job has aged out; fetch a new key"));
	}

	private List<Pet> restocked(Species s, Restock body, String actor) {
		var pets = store().restock(body.perSpecies(), "job:restock-species:" + actor);
		lastRestock.put(s, Instant.now());
		return pets;
	}

	/**
	 * A groom run's detail panel.
	 *
	 * @param id The run id.
	 * @return {@code {contractVersion, fields:<the run's row>}}.
	 * @throws NotFound If the run is unknown.
	 */
	@RestGet(path="/groom-runs/{id}")
	public JsonMap groomRunDetail(@Path("id") String id) {
		return RowDetail.response(groomRun(id).row());
	}

	/**
	 * Issues a regroom key bound to one groom run and the caller.
	 *
	 * @param req The request.
	 * @param id The run id.
	 * @return {@code {contractVersion, action, targetId, idempotencyKey}}.
	 * @throws NotFound If the run is unknown.
	 */
	@RestPost(path="/groom-runs/{id}/key")
	public Map<String,Object> groomRunKey(RestRequest req, @Path("id") String id) {
		var r = groomRun(id);
		return JobKeyLedger.response(jobKeys().issue(REGROOM, r.id(), ConsoleWrites.actor(req)));
	}

	/**
	 * Grooms a run's pet again.
	 *
	 * <p>
	 * Ledger keys only, bound to the path's run: the pet comes from the server's run record, never the request.
	 * {@code /groom} stays the dialog's self-targeted endpoint; one endpoint taking both kinds of key would be only as
	 * strong as the self-targeted one.  A replay returns the same new run while it is kept, then a
	 * {@link JobKeyLedger#SPENT_CODE} refusal once the run list has evicted it; never a second run.
	 *
	 * @param req The request.
	 * @param res The response (409 on a refusal).
	 * @param id The run id.
	 * @param json The action envelope {@code {action, targetId, idempotencyKey}}.
	 * @return The new run's row, or a 409 refusal.
	 * @throws NotFound If the key is unclaimed and the run is unknown.
	 * @throws BadRequest If the envelope is malformed.
	 * @throws IOException If the new run's temp file cannot be created.
	 */
	@RestPost(path="/groom-runs/{id}/regroom")
	public ActionResult regroom(RestRequest req, RestResponse res, @Path("id") String id, @Content JsonMap json) throws IOException {
		var submit = RowJobSubmit.parse(json);
		if (! id.equals(submit.targetId()))
			return refused(res, RowJobSubmit.bindingRefusal());
		var ledger = jobKeys();
		// As restockSpecies: the claim is read and recorded under the ledger's lock.  The replay check comes before the
		// run lookup, because GroomRuns evicts the oldest runs first and a replay must still answer once its target is gone.
		synchronized (ledger) {
			var grant = submit.verify(ledger, REGROOM, ConsoleWrites.actor(req)).orElse(null);
			if (grant == null)
				return refused(res, RowJobSubmit.bindingRefusal());
			var claimed = grant.jobId().orElse(null);
			if (claimed != null)
				return groom.get(claimed).map(x -> ActionResult.success(x.row()).message(f("Grooming %s again", x.pet())))
					.orElseGet(() -> refused(res, ActionResult.refusal(JobKeyLedger.SPENT_CODE).message("This key's run has aged out; fetch a new key")));
			var again = groom.start(groomRun(id).pet(), false, grant.key().value());
			ledger.claim(grant.key(), again.id());
			return ActionResult.success(again.row()).message(f("Grooming %s again", again.pet()));
		}
	}

	private static ActionResult refused(RestResponse res, ActionResult refusal) {
		res.setStatus(RowJobSubmit.REFUSAL_STATUS);
		return refusal;
	}

	private void remember(AsyncJob job, Restock body) {
		synchronized (submitted) {
			submitted.addFirst(JsonMap.of("jobId", job.id(), "submitted", job.createdAt().toString(), "request", body.perSpecies(), "idempotencyKey", body.idempotencyKey()));
			while (submitted.size() > MAX_SUBMITTED)
				submitted.removeLast();
		}
	}

	private JsonMap speciesRow(Species s) {
		return JsonMap.of("id", s.name(), "species", s.name(), "available", available(s));
	}

	private long available(Species s) {
		return store().getPets().stream().filter(p -> p.getSpecies() == s && p.getStatus() == PetStatus.AVAILABLE).count();
	}

	private static Species species(String id) {
		for (var s : Species.values())
			if (s.name().equals(id))
				return s;
		throw new NotFound("Unknown species '%s'", id);
	}

	private GroomRuns.Run groomRun(String id) {
		return groom.get(id).orElseThrow(() -> new NotFound("Unknown groom run '%s'", id));
	}

	private static int count(JsonMap fields) {
		var v = fields == null ? null : fields.get("count");
		try {
			var n = v == null ? 0 : Integer.parseInt(String.valueOf(v).trim());
			if (n >= 1 && n <= 50)
				return n;
		} catch (NumberFormatException e) {
			// Not a whole number (a decimal included): falls through to the 400 below.
		}
		throw new BadRequest("Field 'count' must be a whole number between 1 and 50");
	}
}
