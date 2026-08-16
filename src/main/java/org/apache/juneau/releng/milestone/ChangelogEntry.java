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

package org.apache.juneau.releng.milestone;

import java.util.ArrayList;
import java.util.List;

/** One grouped "** Changes" line (a dependency bump aggregated across PRs). */
public class ChangelogEntry {
	public final String dependency; // "spring.version" (may include " in /juneau-docs")
	public final String fromVersion;
	public final String toVersion;
	public final List<Integer> prNumbers;

	public ChangelogEntry(String dependency, String fromVersion, String toVersion, List<Integer> prNumbers) {
		this.dependency = dependency;
		this.fromVersion = fromVersion;
		this.toVersion = toVersion;
		this.prNumbers = prNumbers;
	}

	/** Renders e.g. "    * Bump spring.version from 4.0.1 to 4.0.6 #308, #316." */
	public String toLine() {
		var refs = new StringBuilder();
		for (var i = 0; i < prNumbers.size(); i++) {
			if (i > 0)
				refs.append(", ");
			refs.append('#').append(prNumbers.get(i));
		}
		return "    * Bump " + dependency + " from " + fromVersion + " to " + toVersion + " " + refs + ".";
	}

	/** Accumulates PRs for one dependency; from = oldest, to = newest (by PR order). */
	public static final class Builder {
		private final String dependency;
		private String fromVersion;
		private String toVersion;
		private final List<Integer> prNumbers = new ArrayList<>();

		public Builder(String dependency) {
			this.dependency = dependency;
		}

		public void add(String from, String to, int prNumber) {
			if (fromVersion == null)
				fromVersion = from; // first (lowest PR#) sets the floor
			toVersion = to; // last (highest PR#) sets the ceiling
			prNumbers.add(prNumber);
		}

		public ChangelogEntry build() {
			return new ChangelogEntry(dependency, fromVersion, toVersion, List.copyOf(prNumbers));
		}
	}
}
