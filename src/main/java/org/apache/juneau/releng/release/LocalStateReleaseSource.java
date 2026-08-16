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

package org.apache.juneau.releng.release;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.juneau.marshall.marshaller.Json;

/** Produces in-progress rows from JSON state files under the state dir. */
public class LocalStateReleaseSource {

	private final Path stateDir;

	public LocalStateReleaseSource(Path stateDir) {
		this.stateDir = stateDir;
	}

	public List<Release> list() {
		var out = new ArrayList<Release>();
		if (!Files.isDirectory(stateDir))
			return out;
		try (var files = Files.list(stateDir)) {
			files.filter(p -> p.toString().endsWith(".json")).sorted().forEach(p -> {
				try {
					var r = Json.DEFAULT.read(Files.readString(p), Release.class);
					if (r.source == null)
						r.source = "state";
					out.add(r);
				} catch (IOException e) {
					throw isex(e, "Unreadable state file: %s", p);
				}
			});
		} catch (IOException e) {
			throw isex(e, "Cannot list state dir: %s", stateDir);
		}
		return out;
	}
}
