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

package org.apache.juneau.releng.log;

import static org.apache.juneau.commons.utils.Shorts.*;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.juneau.releng.util.ProcessRunner;

/** Fires an optional Slack ping at a run's vote deadline. Re-armed from persisted deadline on boot. */
public class VoteDeadlineTimer {

	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
		var t = new Thread(r, "vote-deadline-timer");
		t.setDaemon(true);
		return t;
	});
	private final ProcessRunner runner;
	private final String slackWebhook;

	public VoteDeadlineTimer(ProcessRunner runner, String slackWebhook) {
		this.runner = runner;
		this.slackWebhook = slackWebhook;
	}

	/** Arm (or re-arm) a ping for the given version at {@code deadline}. No-op if webhook empty or deadline past. */
	public void arm(String version, Instant deadline) {
		if (ib(slackWebhook))
			return;
		var delay = Duration.between(Instant.now(), deadline).toMillis();
		if (delay <= 0)
			return;
		scheduler.schedule(() -> ping(version), delay, TimeUnit.MILLISECONDS);
	}

	private void ping(String version) {
		var payload = "{\"text\":\"Juneau " + version + " vote window has closed — tally the result.\"}";
		runner.run(List.of("curl", "-sf", "-X", "POST", "-H", "Content-Type: application/json", "-d", payload,
				slackWebhook), null, Map.of());
	}
}
