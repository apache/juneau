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

import java.util.List;

/** Parsed, comparable Juneau release version derived from a git tag or version string. */
public final class ReleaseVersion implements Comparable<ReleaseVersion> {

	private final String version; // numeric core, e.g. "9.2.0" or "9.0"
	private final String rc; // "RC3" or null
	private final boolean prerelease; // RC or B (beta)
	private final int[] parts; // numeric core split for comparison

	private ReleaseVersion(String version, String rc, boolean prerelease, int[] parts) {
		this.version = version;
		this.rc = rc;
		this.prerelease = prerelease;
		this.parts = parts;
	}

	public static ReleaseVersion ofTag(String tag) {
		var s = tag.startsWith("juneau-") ? tag.substring("juneau-".length()) : tag;
		return of(s);
	}

	public static ReleaseVersion of(String s) {
		String rc = null;
		boolean pre = false;
		var idxRc = s.indexOf("-RC");
		if (idxRc >= 0) {
			rc = s.substring(idxRc + 1);
			pre = true;
			s = s.substring(0, idxRc);
		}
		var idxB = s.indexOf("-B");
		if (idxB >= 0) {
			pre = true;
			s = s.substring(0, idxB);
		}
		var core = s;
		var split = core.split("\\.");
		var parts = new int[split.length];
		for (var i = 0; i < split.length; i++)
			parts[i] = Integer.parseInt(split[i].replaceAll("\\D", ""));
		return new ReleaseVersion(core, rc, pre, parts);
	}

	public String version() {
		return version;
	}

	public String rc() {
		return rc;
	}

	public boolean isPrerelease() {
		return prerelease;
	}

	/** Numeric component {@code i} (0-based) of the version core, or 0 if absent. */
	public int part(int i) {
		return i < parts.length ? parts[i] : 0;
	}

	/** Major (x in x.y.z). */
	public int major() {
		return part(0);
	}

	/** Minor (y in x.y.z). */
	public int minor() {
		return part(1);
	}

	/** Maintenance (z in x.y.z); 0 for a two-part or major/minor version. */
	public int maintenance() {
		return part(2);
	}

	@Override
	public int compareTo(ReleaseVersion o) {
		var n = Math.max(parts.length, o.parts.length);
		for (var i = 0; i < n; i++) {
			var a = i < parts.length ? parts[i] : 0;
			var b = i < o.parts.length ? o.parts[i] : 0;
			if (a != b)
				return Integer.compare(a, b);
		}
		return 0;
	}

	@Override
	public boolean equals(Object o) {
		return this == o || (o instanceof ReleaseVersion o2 && compareTo(o2) == 0);
	}

	@Override
	public int hashCode() {
		// Consistent with compareTo: absent and explicit trailing-zero components are equivalent
		// (e.g. "9.2" == "9.2.0"), so hash only the significant leading numeric components.
		var last = parts.length;
		while (last > 0 && parts[last - 1] == 0)
			last--;
		var h = 1;
		for (var i = 0; i < last; i++)
			h = 31 * h + parts[i];
		return h;
	}

	/** Highest non-prerelease version strictly below {@code ceiling} (exclusive). */
	public static ReleaseVersion highestReleasedBelow(List<String> tags, String ceiling) {
		var cap = of(ceiling);
		ReleaseVersion best = null;
		for (var t : tags) {
			var v = ofTag(t);
			if (v.isPrerelease() || v.compareTo(cap) >= 0)
				continue;
			if (best == null || v.compareTo(best) > 0)
				best = v;
		}
		return best;
	}
}
