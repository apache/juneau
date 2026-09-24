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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Consumer;
import org.apache.juneau.releng.engine.StepState;

/**
 * Appends output lines to the current RC's log file and fans them out to the {@link LogBroadcaster}.
 */
public class RunLog {

	private final Path file;
	private final LogBroadcaster broadcaster;

	/**
	 * Creates a log backed by {@code file}, fanning out appended lines to {@code broadcaster}.
	 */
	public RunLog(Path file, LogBroadcaster broadcaster) {
		this.file = file;
		this.broadcaster = broadcaster;
		try {
			Files.createDirectories(file.getParent());
		} catch (IOException e) {
			throw isex(e, "Cannot create log dir for %s", file);
		}
	}

	/**
	 * Append one line (newline-terminated) to disk, flushed, then broadcast it live.
	 */
	public synchronized void append(String line) {
		try {
			Files.writeString(file, line + "\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE,
					StandardOpenOption.APPEND);
		} catch (IOException e) {
			throw isex(e, "Cannot append to log %s", file);
		}
		broadcaster.publish(line);
	}

	/**
	 * Truncates this step's log file to empty, so it always reflects only the most recent run. A no-op if
	 * the file doesn't exist yet. Already-connected SSE clients keep seeing old lines until they reconnect.
	 */
	public synchronized void reset() {
		try {
			Files.write(file, new byte[0], StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			throw isex(e, "Cannot reset log %s", file);
		}
	}

	/**
	 * A {@link Consumer} suitable for {@code ProcessRunner.run(..., lineSink)}.
	 */
	public Consumer<String> lineSink() {
		return this::append;
	}

	/**
	 * Current byte size of the log file (used for {@link StepState#logOffset}).
	 */
	public long size() {
		try {
			return Files.isRegularFile(file) ? Files.size(file) : 0L;
		} catch (IOException e) {
			return 0L;
		}
	}

	/**
	 * The on-disk log file this instance appends to.
	 */
	public Path file() {
		return file;
	}

	/**
	 * The {@link LogBroadcaster} this instance publishes appended lines to.
	 */
	public LogBroadcaster broadcaster() {
		return broadcaster;
	}
}
