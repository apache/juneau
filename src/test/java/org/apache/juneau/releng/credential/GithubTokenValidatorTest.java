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

import java.util.*;

import org.apache.juneau.releng.util.ProcessRunner;
import org.junit.jupiter.api.Test;

/**
 * {@link GithubTokenValidator}: the token travels by environment variable rather than argv, and {@code gh}'s own
 * output does not reach the message on either path.
 */
class GithubTokenValidatorTest {

	static class StubRunner implements ProcessRunner {
		final ProcResult result;
		List<String> command;
		Map<String,String> env;

		StubRunner(ProcResult result) {
			this.result = result;
		}

		@Override public List<String> runLines(List<String> c) { throw uoex(); }
		@Override public String runText(List<String> c) { throw uoex(); }

		@Override
		public ProcResult run(List<String> c, String stdin, Map<String,String> e) {
			command = c;
			env = e;
			return result;
		}
	}

	private static String message(int exitCode, String output) {
		return new GithubTokenValidator(new StubRunner(new ProcessRunner.ProcResult(exitCode, output)))
			.validate("ghp_s3cret", "token").message();
	}

	@Test
	void a01_tokenTravelsByEnvironmentAndNotArgv() {
		var runner = new StubRunner(new ProcessRunner.ProcResult(0, "octocat\n"));
		assertTrue(new GithubTokenValidator(runner).validate("ghp_s3cret", "token").valid());
		assertEquals("ghp_s3cret", runner.env.get("GH_TOKEN"));
		assertFalse(runner.command.contains("ghp_s3cret"), "token must never appear in argv");
	}

	@Test
	void b01_successReportsAWellFormedLogin() {
		assertEquals("GitHub token OK (user octocat).", message(0, "octocat\n"));
	}

	@Test
	void b02_successWithUnexpectedOutputOmitsIt() {
		// The success path echoed gh's stdout too. It is normally the login, but "normally" is not a rule, so the
		// value is shown only when it looks like a GitHub username and dropped otherwise.
		assertEquals("GitHub token OK.", message(0, "octocat\nwarning: something unexpected"));
		assertEquals("GitHub token OK.", message(0, "not a login: has spaces and punctuation!"));
		assertEquals("GitHub token OK.", message(0, ""));
	}

	@Test
	void c01_subprocessOutputNeverReachesTheFailureMessage() {
		var sentinel = "SENTINEL-b41e07-DO-NOT-SURFACE";
		for (var output : List.of(sentinel, "gh: 401 " + sentinel, "HTTP 403: " + sentinel))
			assertFalse(message(1, output).contains(sentinel),
					() -> "gh output leaked into the UI message for: " + output);
	}

	@Test
	void b03_theSuccessPathIsBoundedByGithubsUsernameGrammar() {
		// The success path echoed gh's stdout too, and the bound on it is a grammar rather than a blanket refusal:
		// a value that GitHub could actually have issued as a username is displayed, because showing which account
		// the token belongs to is the useful half of the message.
		//
		// The grammar is the bound. 39 characters of [A-Za-z0-9-] cannot carry a multi-line stderr dump, a
		// passphrase with punctuation, or markup -- and a GitHub token does not fit it either (ghp_ tokens are 40
		// characters and contain an underscore). Anything outside it is dropped entirely rather than truncated,
		// since a truncated diagnostic is not worth the channel it travels on.
		assertEquals("GitHub token OK.", message(0, "gh: warning: something unexpected happened"));
		assertEquals("GitHub token OK.", message(0, "octocat\ngh: extra line"));
		assertEquals("GitHub token OK.", message(0, "a".repeat(40)));
		assertEquals("GitHub token OK.", message(0, "has spaces and punctuation!"));
		assertEquals("GitHub token OK.", message(0, "ghp_0123456789abcdef0123456789abcdef0123"));
		assertEquals("GitHub token OK (user octo-cat9).", message(0, "octo-cat9\n"));
	}

	@Test
	void c02_failureReasonsAreEnumerated() {
		assertEquals("GitHub rejected the token (401 \u2014 not accepted).", message(1, "gh: Bad credentials"));
		assertEquals("The token was accepted but lacks the required scope (403).", message(1, "HTTP 403: Forbidden"));
		assertEquals("GitHub was unreachable \u2014 check the network.", message(1, "dial tcp: connection refused"));
		assertEquals("gh is not installed or not on the PATH.", message(127, "gh: command not found"));
	}

	@Test
	void c03_unrecognizedFailureCarriesTheExitCodeAndNothingElse() {
		assertEquals("GitHub rejected the token (gh exit code 9).", message(9, "something entirely unexpected"));
	}
}
