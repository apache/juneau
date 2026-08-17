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
package org.apache.juneau.rest.client;

import java.util.*;
import java.util.logging.*;

import org.apache.juneau.commons.logging.LogRecordContext;
import org.apache.juneau.commons.logging.RichLogger;

final class RestClientDebugPipeline {

	private RestClientDebugPipeline() {}

	static Level resolveTier(RichLogger logger) {
		if (logger.isLoggable(Level.FINEST))
			return Level.FINEST;
		if (logger.isLoggable(Level.FINE))
			return Level.FINE;
		if (logger.isLoggable(Level.INFO))
			return Level.INFO;
		return null;
	}

	static void emit(RichLogger logger, RestClientDebugFormatter formatter, Level level, RestRequest req, RestResponse res, Throwable thrown) {
		if (level == null)
			return;

		var sb = new StringBuilder(formatter.formatBasic(req, res));
		if (level.intValue() <= Level.FINE.intValue())
			sb.append(formatter.formatHeaders(req, res));
		if (level.intValue() <= Level.FINEST.intValue())
			sb.append(formatter.formatBody(req, res));

		// Stable-INFO emission: the record is always stamped INFO so the basic access line survives under an
		// INFO handler even when a logger is raised to FINE/FINEST for diagnostics. `level` (the resolved tier)
		// continues to drive only the cumulative section-inclusion checks above.
		var record = new LogRecord(Level.INFO, sb.toString());
		record.setLoggerName(logger.getName());
		if (thrown != null)
			record.setThrown(thrown);

		// Stamp-from-field: attach the resolved correlation id as a structured field so it survives to emission
		// regardless of any log-context scope's lifetime (the load-bearing client fix).  Prefer the server-confirmed
		// echoed id, else the sent id.  Map.of rejects null, so skip the attach entirely when neither exists (e.g.
		// sendRequestId(false)) — a null-valued map would throw.
		var effectiveId = (res != null && res.getRequestId() != null) ? res.getRequestId() : (req != null ? req.getRequestId() : null);
		if (effectiveId != null)
			LogRecordContext.attachIfAbsent(record, Map.of("requestId", effectiveId));

		logger.log(record);
	}
}
