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

package org.apache.juneau.releng.engine;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.juneau.marshall.marshaller.Json;

/** Reads/writes {@code release-<version>.json} state files under {@code rm.state.dir}. */
public class RunStateStore {

	private final Path stateDir;

	public RunStateStore(Path stateDir) {
		this.stateDir = stateDir;
	}

	public Path stateDir() {
		return stateDir;
	}

	private Path fileFor(String version) {
		return stateDir.resolve("release-" + version + ".json");
	}

	/** Persist the run (pretty JSON), creating {@code rm.state.dir} if absent. */
	public synchronized void save(RunState rs) {
		try {
			Files.createDirectories(stateDir);
			rs.touch();
			Files.writeString(fileFor(rs.version), Json.DEFAULT.write(rs));
		} catch (IOException e) {
			throw isex(e, "Cannot save run state for %s", rs.version);
		}
	}

	public Optional<RunState> load(String version) {
		var f = fileFor(version);
		if (!Files.isRegularFile(f))
			return Optional.empty();
		try {
			return Optional.of(Json.DEFAULT.read(Files.readString(f), RunState.class));
		} catch (IOException e) {
			throw isex(e, "Unreadable run state: %s", f);
		}
	}

	/** Every persisted run, loaded from disk. */
	public List<RunState> loadAll() {
		var out = new ArrayList<RunState>();
		if (!Files.isDirectory(stateDir))
			return out;
		try (var files = Files.list(stateDir)) {
			files.filter(p -> {
				var n = p.getFileName().toString();
				return n.startsWith("release-") && n.endsWith(".json");
			}).sorted().forEach(p -> {
				try {
					out.add(Json.DEFAULT.read(Files.readString(p), RunState.class));
				} catch (IOException e) {
					throw isex(e, "Unreadable run state: %s", p);
				}
			});
		} catch (IOException e) {
			throw isex(e, "Cannot list state dir: %s", stateDir);
		}
		return out;
	}

	/** The single run whose status is RUNNING or AWAITING_VOTE, if any. */
	public Optional<RunState> activeRun() {
		return loadAll().stream().filter(r -> r.status == RunStatus.RUNNING || r.status == RunStatus.AWAITING_VOTE)
				.findFirst();
	}
}
