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

package org.apache.juneau.releng.credential;

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.juneau.releng.util.ProcessRunner;
import org.junit.jupiter.api.Test;

class GpgValidatorTest {

	/** Records invocations and returns queued results. */
	static class RecordingRunner implements ProcessRunner {
		final List<List<String>> calls = new ArrayList<>();
		final List<String> stdins = new ArrayList<>();
		final List<ProcResult> results;
		int i = 0;

		RecordingRunner(List<ProcResult> results) {
			this.results = results;
		}

		@Override
		public List<String> runLines(List<String> c) {
			throw uoex();
		}

		@Override
		public String runText(List<String> c) {
			throw uoex();
		}

		@Override
		public ProcResult run(List<String> c, String stdin, Map<String, String> env) {
			calls.add(c);
			stdins.add(stdin);
			return results.get(i++);
		}
	}

	@Test
	void passesPassphraseOnStdinAndReportsSuccess() {
		var runner = new RecordingRunner(List.of(new ProcessRunner.ProcResult(0, "sec ...\n"), // list-secret-keys
				new ProcessRunner.ProcResult(0, ""))); // test-sign
		var result = new GpgValidator(runner).validate("s3cret", "ABCD1234");
		assertTrue(result.valid());
		assertEquals("s3cret\n", runner.stdins.get(1), "passphrase must go on stdin, not argv");
		assertFalse(runner.calls.get(1).contains("s3cret"), "passphrase must never appear in argv");
		assertTrue(runner.calls.get(1).containsAll(List.of("--pinentry-mode", "loopback", "--passphrase-fd", "0")));
	}

	@Test
	void failsWhenKeyMissing() {
		var runner = new RecordingRunner(List.of(new ProcessRunner.ProcResult(2, "gpg: error")));
		var result = new GpgValidator(runner).validate("s3cret", "NOPE");
		assertFalse(result.valid());
	}

	@Test
	void failsWhenSignRejected() {
		var runner = new RecordingRunner(List.of(new ProcessRunner.ProcResult(0, "sec ...\n"),
				new ProcessRunner.ProcResult(2, "bad passphrase")));
		var result = new GpgValidator(runner).validate("wrong", "ABCD1234");
		assertFalse(result.valid());
	}
}
