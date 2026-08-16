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
package org.apache.juneau.rest.server.logging;

import java.util.logging.*;

import org.apache.juneau.commons.logging.RichLogger;
import org.apache.juneau.rest.server.*;

/**
 * The Phase B (request-completion) half of the JUL-level-driven REST debug pipeline.
 *
 * <p>
 * Phase A (request start) is handled by {@code RestContext}/{@code RestOpContext}, which install bounded body-caching
 * wrappers only when the resolved logger is loggable at {@link Level#FINEST FINEST}. This class re-derives the cumulative
 * tier from the resolved logger, renders a single cumulative message through the resolved {@link RestDebugFormatter},
 * and emits exactly one {@link LogRecord}:
 * <ul>
 * 	<li>tier {@code INFO} &mdash; {@code formatBasic} only.
 * 	<li>tier {@code FINE} &mdash; {@code +formatHeaders}.
 * 	<li>tier {@code FINEST} &mdash; {@code +formatBody} (from the cached bytes).
 * 	<li>coarser than {@code INFO} ({@code WARNING}, {@code SEVERE}, {@code OFF}) &mdash; nothing is emitted.
 * </ul>
 * The emitted record level is always {@link Level#INFO}; the resolved tier controls message detail only.
 *
 * @since 10.0.0
 */
public class RestDebugPipeline {

	/** Contains any diagnostic-formatting failure on the completion path (mirrors {@code RestSession.finish()}). */
	private static final RichLogger LOG = RichLogger.getLogger(RestDebugPipeline.class);

	private RestDebugPipeline() {}

	/**
	 * Emits one debug record for the completed call on the synchronous request thread, if the resolved logger's
	 * effective level warrants it.
	 *
	 * <p>
	 * This is the synchronous entry point: it resolves the logger/formatter/detail-tier <i>now</i> (the operation is
	 * already resolved by finish time) and renders/emits immediately. The emitted record is always stamped at
	 * {@link Level#INFO}; the resolved tier controls only which sections are rendered into the message body.
	 *
	 * @param session The completed REST session. Must not be <jk>null</jk>.
	 */
	public static void emit(RestSession session) {
		emit(session, snapshot(session));
	}

	/**
	 * Resolves the logger/formatter/detail-tier snapshot for the given session and stashes it on the session for
	 * later completion-path emission.
	 *
	 * <p>
	 * Called on the request thread at the async-dispatch handoff, before any completion callback is registered, so the
	 * immutable snapshot is safely published to the completion thread. Stashes <jk>null</jk> when access logging is off
	 * (no allocation), keeping {@link #emitOnCompletion(RestSession)} a no-op in that case.
	 *
	 * @param session The REST session being handed off to async dispatch. Must not be <jk>null</jk>.
	 */
	public static void captureAsyncSnapshot(RestSession session) {
		session.stashDebugSnapshot(snapshot(session));
	}

	/**
	 * Emits one debug record for a completed asynchronous call on the response-completion thread.
	 *
	 * <p>
	 * Reads the snapshot published on the request thread (via {@link #captureAsyncSnapshot(RestSession)}), sets
	 * finish-time attributes (such as {@code ExecTime}) immediately before rendering, and renders from the final
	 * request/response state now that the async body and headers have been written. No-ops when no snapshot was
	 * stashed (access logging off). Any diagnostic-formatting failure is contained so it cannot disrupt request
	 * cleanup or async-context completion.
	 *
	 * @param session The completed REST session. Can be <jk>null</jk> (no-op).
	 */
	public static void emitOnCompletion(RestSession session) {
		if (session == null)
			return;
		// Contain diagnostic formatting/emission on the completion thread exactly as RestSession.finish() does on the
		// request thread: log only a fixed token — never the message, body, or a second formatter pass — so a
		// scrubber throwing new RuntimeException(body) cannot re-leak the secret the placeholder just refused.
		try {
			session.setFinishTimeAttributes();
			emit(session, (RestDebugSnapshot) session.getDebugSnapshot());
		} catch (Throwable t) {  // NOSONAR - deliberate containment of any diagnostic failure at request completion.
			LOG.log(Level.WARNING, "debug formatter failed");
		}
	}

	/**
	 * Resolves the logger/formatter/detail-tier for the given session.
	 *
	 * <p>
	 * Resolves the operation logger when an operation was created, else the resource logger. Returns <jk>null</jk> when
	 * access logging is off (no logger, or the logger is coarser than {@code INFO}), in which case nothing is emitted.
	 *
	 * @param session The REST session. Must not be <jk>null</jk>.
	 * @return The resolved snapshot, or <jk>null</jk> if access logging is off.
	 */
	static RestDebugSnapshot snapshot(RestSession session) {
		var opSession = session.getOpSessionOrNull();
		var logger = opSession != null ? opSession.getContext().getLogger() : session.getContext().getLogger();
		if (logger == null)
			return null;

		var tier = resolveTier(logger);
		if (tier == null)
			return null;

		return new RestDebugSnapshot(logger, resolveFormatter(session), tier);
	}

	/**
	 * Emits one debug record from the supplied resolution snapshot, reading the final request/response state at call
	 * time.
	 *
	 * <p>
	 * No-ops on a <jk>null</jk> snapshot. The emitted record is always stamped at {@link Level#INFO}; the snapshot's
	 * detail tier controls only how much of the message is rendered.
	 *
	 * @param session The completed REST session. Must not be <jk>null</jk>.
	 * @param snapshot The resolution snapshot, or <jk>null</jk> (no-op).
	 */
	static void emit(RestSession session, RestDebugSnapshot snapshot) {
		if (snapshot == null)
			return;

		var opSession = session.getOpSessionOrNull();
		var msg = render(session, opSession, snapshot);

		var record = new LogRecord(Level.INFO, msg);
		record.setLoggerName(snapshot.logger().getName());
		var thrown = session.getException();
		if (thrown != null)
			record.setThrown(thrown);
		snapshot.logger().log(record);
	}

	private static String render(RestSession session, RestOpSession opSession, RestDebugSnapshot snapshot) {
		var formatter = snapshot.formatter();

		// No operation resolved (404/no-op path): only the basic status line is renderable — there is no
		// RestRequest/RestResponse to drive the formatter tiers through. The sanitized, length-capped instance
		// statusLine (not a static-only path) renders it.
		if (opSession == null)
			return formatter.statusLine(session.getRequest(), session.getResponse());

		var level = snapshot.tier();
		var req = opSession.getRequest();
		var res = opSession.getResponse();

		var sb = new StringBuilder(formatter.formatBasic(req, res));
		if (level.intValue() <= Level.FINE.intValue())
			sb.append(formatter.formatHeaders(req, res));
		if (level.intValue() <= Level.FINEST.intValue())
			sb.append(formatter.formatBody(req, res));
		return sb.toString();
	}

	private static RestDebugFormatter resolveFormatter(RestSession session) {
		var f = session.getContext().getRestDebugFormatter();
		return f != null ? f : new BasicRestDebugFormatter();
	}

	/**
	 * Maps the resolved logger's effective level to the cumulative debug tier.
	 *
	 * @param logger The resolved logger.
	 * @return {@code FINEST}/{@code FINE}/{@code INFO}, or <jk>null</jk> if the logger is coarser than {@code INFO}.
	 */
	static Level resolveTier(Logger logger) {
		if (logger.isLoggable(Level.FINEST))
			return Level.FINEST;
		if (logger.isLoggable(Level.FINE))
			return Level.FINE;
		if (logger.isLoggable(Level.INFO))
			return Level.INFO;
		return null;
	}
}
