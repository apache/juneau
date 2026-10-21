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
package org.apache.juneau.releng.rest;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.apache.juneau.commons.inject.Bean;
import org.apache.juneau.http.Content;
import org.apache.juneau.http.Path;
import org.apache.juneau.http.response.BadRequest;
import org.apache.juneau.http.response.Conflict;
import org.apache.juneau.http.response.InternalServerError;
import org.apache.juneau.marshall.collections.JsonMap;
import org.apache.juneau.rest.server.Mutating;
import org.apache.juneau.rest.server.Rest;
import org.apache.juneau.rest.server.RestGet;
import org.apache.juneau.rest.server.RestPost;
import org.apache.juneau.rest.server.RestRequest;
import org.apache.juneau.rest.server.servlet.BasicRestResource;
import org.apache.juneau.rest.server.staticfile.WebJarsMixin;
import org.apache.juneau.rest.server.terminal.FileTerminalSource;
import org.apache.juneau.rest.server.terminal.TerminalMixin;
import org.apache.juneau.rest.server.terminal.TerminalSource;
import org.apache.juneau.rest.server.views.IdempotencyKey;
import org.apache.juneau.rest.server.views.ViewsMixin;
import org.apache.juneau.rest.server.view.View;
import org.apache.juneau.rest.server.view.freemarker.FreemarkerMixin;
import org.apache.juneau.rest.server.view.freemarker.FreemarkerViewRenderer;
import org.apache.juneau.rest.server.view.freemarker.console.ConsoleFreemarkerMixin;
import org.apache.juneau.releng.run.ReleaseProcessService;
import org.apache.juneau.releng.run.RunFiles;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Release tab: runs {@code scripts/release.py} under a terminal, brokers its prompts, and replays past attempts.
 *
 * <p>{@code disableContentParam} is set for the reason given on {@code CredentialRest}: Juneau's default lets a
 * {@code POST} body arrive in a {@code &content=} query parameter, which puts every action payload into browser
 * history and access logs. That matters most here, where a prompt answer can be a secret.
 */
@SuppressWarnings({
	"java:S1192" // JSON response keys read more clearly inline than as constants.
})
@Rest(path = "/runs", title = "Release", responseProcessors = FreemarkerViewRenderer.class,
	disableContentParam = "true", mixins = { ViewsMixin.class, WebJarsMixin.class })
public class ReleaseRunRest extends BasicRestResource implements TerminalMixin {

	/**
	 * The action id every start key is minted for.
	 */
	static final String START_ACTION = "start-run";

	/**
	 * How many start keys are remembered; the oldest is forgotten first.
	 */
	private static final int MAX_START_KEYS = 100;

	private static final Pattern ATTEMPT_ID = Pattern.compile("attempt-(\\d{8}-\\d{6}-\\d{3})");

	/**
	 * The per-call logger of {@link #answer}, held so its level stays pinned: a prompt answer can be a secret, and
	 * Juneau logs a call's body on {@code <resource logger>.<java method>} at {@code FINEST}.
	 */
	private static final Logger ANSWER_CALL_LOG = Logger.getLogger(ReleaseRunRest.class.getName() + ".answer");

	static {
		ANSWER_CALL_LOG.setLevel(Level.OFF);
	}

	private final RunFiles runFiles;
	private final ReleaseProcessService processes;

	/**
	 * Start key to the attempt it started, in submission order. Kept in memory only: it collapses a double-submitted
	 * Start, and nothing more.
	 */
	private final Map<String, String> startedByKey = new LinkedHashMap<>() {
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
			return size() > MAX_START_KEYS;
		}
	};

	/**
	 * Wires the run-file reader and the process service this resource's endpoints delegate to.
	 */
	public ReleaseRunRest(RunFiles runFiles, ReleaseProcessService processes) {
		this.runFiles = runFiles;
		this.processes = processes;
	}

	/**
	 * The Freemarker mixin used to render this resource's templates.
	 */
	// Return type stays FreemarkerMixin - FreemarkerViewRenderer does an exact-type bean lookup (see
	// ConsoleFreemarkerMixin's class Javadoc).
	@Bean
	public FreemarkerMixin freemarker() {
		var builder = ConsoleFreemarkerMixin.create();
		builder.adopterAssets(getClass().getClassLoader(), "static");
		builder.basePath("/templates/").templateSuffix(".ftlh");
		return builder.build();
	}

	/**
	 * Human page. Everything on it is loaded from {@link #current()} and kept fresh from {@code /events/runs}.
	 */
	@RestGet("/")
	public View page(HttpServletRequest req) {
		return ConsolePage.of("release", req).attr("navTab", "release")
			.attr("startKey", IdempotencyKey.mintSelfTargeted(START_ACTION).value());
	}

	/**
	 * The page's state: the active run, the live process, the pending prompt, and the attempt history.
	 */
	@RestGet("/current")
	public Map<String, Object> current() {
		Map<String, Object> m = new LinkedHashMap<>();
		runFiles.active().ifPresent(r -> m.put("active", r));
		processes.live().ifPresent(l -> {
			m.put("live", m("attemptId", l.attemptId(), "command", l.command(), "dryRun", l.dryRun(), "pid", l.broker().pid()));
			l.broker().pending().ifPresent(p -> m.put("pending", p));
		});
		m.put("history", runFiles.history());
		return m;
	}

	/**
	 * Starts a new run. A repeat of the key that started the live attempt returns that attempt, so a double-submit is
	 * a replay.
	 */
	@Mutating("spawns scripts/release.py, which builds and stages a release candidate")
	@RestPost("/start")
	public Map<String, Object> start(@Content StartRequest body) {
		if (body == null)
			throw new BadRequest("A request body is required");
		var key = ine(body.idempotencyKey) ? IdempotencyKey.of(body.idempotencyKey, START_ACTION, body.idempotencyKey).value() : null;
		synchronized (startedByKey) {
			if (key != null) {
				var prior = startedByKey.get(key);
				var live = processes.live();
				if (prior != null && live.isPresent() && live.get().attemptId().equals(prior))
					return m("attemptId", prior);
				startedByKey.remove(key);
			}
			if (runFiles.active().isPresent())
				throw new Conflict("A release is in progress: resume it or drop its RC");
			var id = launch(() -> processes.start(body.dryRun));
			if (key != null)
				startedByKey.put(key, id);
			return m("attemptId", id);
		}
	}

	/**
	 * Resumes the active run.
	 */
	@Mutating("spawns scripts/release.py --resume")
	@RestPost("/resume")
	public Map<String, Object> resume(@Content DryRunRequest body) {
		if (runFiles.active().isEmpty())
			throw new Conflict("No active release to resume");
		return m("attemptId", launch(() -> processes.resume(body != null && body.dryRun)));
	}

	/**
	 * Drops the active run's RC. The typed {@code RC<n>} is checked here and is the answer to the script's own
	 * confirm.
	 */
	@Mutating("spawns scripts/release.py --revert, which drops the RC from Nexus and dist SVN")
	@RestPost("/drop-rc")
	public Map<String, Object> dropRc(@Content DropRcRequest body) {
		if (body == null)
			throw new BadRequest("A request body is required");
		var active = runFiles.active().orElseThrow(() -> new Conflict("No active release"));
		if ("promote".equals(active.phase) || "released".equals(active.phase))
			throw new Conflict("The release is past the vote; its RC cannot be dropped");
		if (active.rc <= 0)
			throw new BadRequest("The active run records no RC number, so there is nothing to confirm against");
		if (neq(body.confirm, "RC" + active.rc))
			throw new BadRequest("Type RC%s to confirm", active.rc);
		return m("attemptId", launch(() -> processes.dropRc(body.confirm, body.dryRun)));
	}

	/**
	 * Cancels the live process. release.py records the attempt {@code cancelled}.
	 */
	@Mutating("stops the running scripts/release.py")
	@RestPost("/cancel")
	public Map<String, Object> cancel() {
		processes.cancel();
		return m();
	}

	/**
	 * Answers the pending prompt. The body is never logged (see {@link #ANSWER_CALL_LOG}).
	 */
	@Mutating("answers a release.py prompt")
	@RestPost("/prompts/{id}")
	public Map<String, Object> answer(@Path("id") String id, @Content JsonMap body) {
		var live = processes.live().orElseThrow(() -> new Conflict("No release process is running"));
		try {
			live.broker().answer(id, body == null ? new JsonMap() : body);
		} catch (IllegalStateException e) {
			throw new Conflict(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw new BadRequest(e.getMessage());
		}
		return m();
	}

	/**
	 * An attempt's terminal. The id is {@code attempt-<attempt dir name>}: the live process's own source while it
	 * runs, then the dir's {@code terminal.log}, then (for a CLI attempt run without one) its {@code full.log}.
	 */
	@Override /* TerminalMixin */
	public Optional<TerminalSource> terminalSource(String id, RestRequest req) {
		var live = processes.live();
		if (live.isPresent() && live.get().attemptId().equals(id))
			return Optional.of(live.get().process().terminal());
		var mt = ATTEMPT_ID.matcher(id);
		if (! mt.matches())
			return Optional.empty();
		var dir = runFiles.home().resolve("attempts").resolve(mt.group(1));
		for (var name : new String[] { "terminal.log", "full.log" }) {
			var f = dir.resolve(name);
			if (Files.isRegularFile(f))
				return Optional.of(FileTerminalSource.create(f).terminal(() -> true).build());
		}
		return Optional.empty();
	}

	private static String launch(java.util.function.Supplier<ReleaseProcessService.Live> spawn) {
		try {
			return spawn.get().attemptId();
		} catch (ReleaseProcessService.BusyException e) {
			throw new Conflict(e.getMessage());
		} catch (IllegalStateException e) {
			// Not the single-process conflict: the runner script is missing or the attempt dir already holds a run.
			// Other runtime failures propagate and Juneau answers 500 for them too.
			throw new InternalServerError(e, "Could not launch the release process: %s", e.getMessage());
		}
	}

	/**
	 * Body for {@link #start(StartRequest)}.
	 */
	public static class StartRequest {
		public String idempotencyKey;
		public boolean dryRun;
	}

	/**
	 * Body for {@link #resume(DryRunRequest)}.
	 */
	public static class DryRunRequest {
		public boolean dryRun;
	}

	/**
	 * Body for {@link #dropRc(DropRcRequest)}.
	 */
	public static class DropRcRequest {
		public String confirm;
		public boolean dryRun;
	}
}
