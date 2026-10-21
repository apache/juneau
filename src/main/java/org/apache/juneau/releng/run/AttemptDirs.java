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
package org.apache.juneau.releng.run;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Creates attempt directories exactly as {@code jrm_release.runfile.new_attempt_dir} does, so CLI and UI attempts
 * share one numbering: {@code <home>/attempts/<YYYYMMDD-HHMMSS>-<NNN>}, a taken number skipped.
 */
public final class AttemptDirs {

	static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

	private AttemptDirs() {}

	/**
	 * Creates the next attempt directory for now.
	 *
	 * @param home {@code rm.home}.
	 * @return The new, empty directory.
	 * @throws IOException If it cannot be created.
	 */
	public static Path create(Path home) throws IOException {
		return create(home, stamp());
	}

	/** The current local time as a {@link #STAMP}, the zone {@code release.py} stamps with too. */
	static String stamp() {
		return LocalDateTime.now(ZoneId.systemDefault()).format(STAMP);
	}

	static Path create(Path home, String stamp) throws IOException {
		var base = Files.createDirectories(home.resolve("attempts"));
		for (var n = 1;; n++) {
			try {
				return Files.createDirectory(base.resolve(stamp + "-" + String.format("%03d", n)));
			} catch (@SuppressWarnings("unused") FileAlreadyExistsException e) {
				// taken by a concurrent run: try the next number
			}
		}
	}
}
