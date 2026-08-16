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

	private RestDebugPipeline() {}

	/**
	 * Emits one debug record for the completed call, if the resolved logger's effective level warrants it.
	 *
	 * <p>
	 * The emitted record is always stamped at {@link Level#INFO}; the resolved tier controls only which
	 * sections are rendered into the message body.
	 *
	 * @param session The completed REST session. Must not be <jk>null</jk>.
	 */
	public static void emit(RestSession session) {
		var opSession = session.getOpSessionOrNull();
		var logger = opSession != null ? opSession.getContext().getLogger() : session.getContext().getLogger();
		if (logger == null)
			return;

		var tier = resolveTier(logger);
		if (tier == null)
			return;

		var msg = render(session, opSession, tier);

		var record = new LogRecord(Level.INFO, msg);
		record.setLoggerName(logger.getName());
		var thrown = session.getException();
		if (thrown != null)
			record.setThrown(thrown);
		logger.log(record);
	}

	private static String render(RestSession session, RestOpSession opSession, Level level) {
		// No operation resolved (404/no-op path): only the basic status line is renderable — there is no
		// RestRequest/RestResponse to drive the formatter tiers through.
		if (opSession == null)
			return BasicRestDebugFormatter.statusLine(session.getRequest(), session.getResponse());

		var formatter = resolveFormatter(session);
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
