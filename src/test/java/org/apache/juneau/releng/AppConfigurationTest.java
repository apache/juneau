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

package org.apache.juneau.releng;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.apache.juneau.releng.config.TargetProfile;
import org.apache.juneau.releng.engine.BranchResolver;
import org.apache.juneau.releng.engine.ReleaseEngine;
import org.apache.juneau.releng.engine.RunStateStore;
import org.apache.juneau.releng.engine.StepRegistry;
import org.apache.juneau.releng.nexus.NexusStagingClient;
import org.apache.juneau.releng.util.ProcessRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AppConfigurationTest {

	private ProcessRunner okRunner() {
		return new ProcessRunner() {
			@Override
			public List<String> runLines(List<String> c) {
				return List.of();
			}

			@Override
			public String runText(List<String> c) {
				return "";
			}

			@Override
			public ProcResult run(List<String> c, String s, Map<String, String> e) {
				if (c.contains("ls-remote"))
					return new ProcResult(0, "sha\trefs/heads/juneau-9.2.1-branch\n");
				return new ProcResult(0, "ok");
			}

			@Override
			public ProcResult run(List<String> c, String s, Map<String, String> e,
					java.util.function.Consumer<String> k) {
				return run(c, s, e);
			}
		};
	}

	private ReleaseEngine engine(Path dir) {
		var runner = okRunner();
		var branches = new BranchResolver(runner, "/repo");
		return ReleaseEngine.forTests(new RunStateStore(dir), StepRegistry.standard(branches), runner, branches, dir);
	}

	@Test
	void a01_broadcasterResolverIsEmptyForUnknownVersionAndStep(@TempDir Path dir) {
		var store = new RunStateStore(dir);
		var eng = engine(dir);
		var resolver = AppConfiguration.broadcasterForStep(eng, store);
		assertTrue(resolver.apply("nope", "nope").isEmpty());
	}

	@Test
	void a02_broadcasterResolverIsEmptyForKnownVersionButUnknownStep(@TempDir Path dir) {
		var store = new RunStateStore(dir);
		var eng = engine(dir);
		eng.start("9.2.1", null);
		var resolver = AppConfiguration.broadcasterForStep(eng, store);
		assertTrue(resolver.apply("9.2.1", "not-a-real-step").isEmpty());
	}

	@Test
	void a03_broadcasterResolverIsPresentForRealActiveRunAndStep(@TempDir Path dir) {
		var store = new RunStateStore(dir);
		var eng = engine(dir);
		eng.start("9.2.1", null);
		var resolver = AppConfiguration.broadcasterForStep(eng, store);
		var bc = resolver.apply("9.2.1", "preflight");
		assertTrue(bc.isPresent());
		assertSame(eng.broadcaster("9.2.1", "preflight"), bc.get());
	}

	@Test
	void b01_stateBroadcasterResolverIsEmptyForUnknownVersion(@TempDir Path dir) {
		var store = new RunStateStore(dir);
		var eng = engine(dir);
		var resolver = AppConfiguration.stateBroadcasterForVersion(eng, store);
		assertTrue(resolver.apply("nope").isEmpty());
	}

	@Test
	void b02_stateBroadcasterResolverIsPresentAndSharedForAKnownVersion(@TempDir Path dir) {
		var store = new RunStateStore(dir);
		var eng = engine(dir);
		eng.start("9.2.1", null);
		var resolver = AppConfiguration.stateBroadcasterForVersion(eng, store);
		var bc = resolver.apply("9.2.1");
		assertTrue(bc.isPresent());
		assertSame(eng.stateBroadcaster("9.2.1"), bc.get());
	}

	@Test
	void c01_logPathResolverIsEmptyForUnknownVersionAndStep(@TempDir Path dir) {
		var store = new RunStateStore(dir);
		var resolver = AppConfiguration.logPathForStep(store);
		assertTrue(resolver.apply("nope", "nope").isEmpty());
	}

	@Test
	void c02_logPathResolverIsEmptyForKnownVersionButUnknownStep(@TempDir Path dir) {
		var store = new RunStateStore(dir);
		var eng = engine(dir);
		eng.start("9.2.1", null);
		var resolver = AppConfiguration.logPathForStep(store);
		assertTrue(resolver.apply("9.2.1", "not-a-real-step").isEmpty());
	}

	@Test
	void d01_nexusClientUsesKeychainCredsWhenBothPresent() {
		var fallbackCalled = new boolean[] { false };
		var client = AppConfiguration.nexusClient(TargetProfile.prodDefault(), "jbognar", "s3cr3t", () -> {
			fallbackCalled[0] = true;
			return NexusStagingClient.forTests((m, p, b) -> "");
		});
		assertNotNull(client);
		assertFalse(fallbackCalled[0]);
	}

	@Test
	void d02_nexusClientFallsBackToSettingsXmlWhenKeychainEntryAbsent() {
		var target = TargetProfile.prodDefault();
		var sentinel = NexusStagingClient.forTests((m, p, b) -> "");
		var client = AppConfiguration.nexusClient(target, "", "", () -> sentinel);
		assertSame(sentinel, client);

		var client2 = AppConfiguration.nexusClient(target, "jbognar", "", () -> sentinel);
		assertSame(sentinel, client2);
	}
}
