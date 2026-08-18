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

	// -----------------------------------------------------------------------------------------------------------
	// Bounded failure messages: gpg's own output must not reach the message (finding F2).
	// -----------------------------------------------------------------------------------------------------------

	private static String signFailureMessage(int exitCode, String output) {
		var runner = new RecordingRunner(
				List.of(new ProcessRunner.ProcResult(0, "sec ...\n"), new ProcessRunner.ProcResult(exitCode, output)));
		var r = new GpgValidator(runner).validate("wrong", "ABCD1234");
		assertFalse(r.valid());
		return r.message();
	}

	@Test
	void subprocessOutputNeverReachesTheMessage() {
		// The regression guard for F2, in the shape SecretsOffArgvTest uses: a sentinel that must not appear. The
		// sentinel stands in for anything gpg might print -- and the reason this matters is not that gpg is known
		// to echo a passphrase, but that the message is an unbounded channel from another program's stderr into a
		// JSON response, the credential card and a table column.
		var sentinel = "SENTINEL-a7f3c9-DO-NOT-SURFACE";
		for (var output : List.of(sentinel, "gpg: signing failed: " + sentinel, "bad passphrase\n" + sentinel))
			assertFalse(signFailureMessage(2, output).contains(sentinel),
					() -> "gpg output leaked into the UI message for: " + output);
	}

	@Test
	void failureReasonsAreEnumerated() {
		assertEquals("The passphrase was rejected by gpg.", signFailureMessage(2, "gpg: Bad passphrase"));
		assertEquals("gpg could not read the passphrase non-interactively (pinentry).",
				signFailureMessage(2, "gpg: problem with pinentry"));
		assertEquals("The signing key has expired.", signFailureMessage(2, "gpg: key has expired"));
		assertEquals("The signing key has been revoked.", signFailureMessage(2, "gpg: key was revoked"));
		assertEquals("gpg is not installed or not on the PATH.", signFailureMessage(127, "gpg: command not found"));
	}

	@Test
	void unrecognizedFailureCarriesTheExitCodeAndNothingElse() {
		// The fallback still has to be actionable, and an exit code is a bounded integer rather than free text.
		assertEquals("gpg refused to sign (exit code 42).", signFailureMessage(42, "something entirely unexpected"));
	}

	@Test
	void missingToolIsDistinguishedFromMissingKey() {
		// Both fail the first gpg call, and telling the user "no secret key for X" when gpg is not installed sends
		// them to generate a key they may already have.
		var absent = new RecordingRunner(List.of(new ProcessRunner.ProcResult(127, "gpg: command not found")));
		assertEquals("gpg is not installed or not on the PATH.",
				new GpgValidator(absent).validate("s3cret", "ABCD1234").message());

		var noKey = new RecordingRunner(List.of(new ProcessRunner.ProcResult(2, "gpg: error reading key")));
		assertEquals("No secret key for NOPE.", new GpgValidator(noKey).validate("s3cret", "NOPE").message());
	}
}
