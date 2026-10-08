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
package org.apache.juneau.rest.server.views.report;

import java.net.*;
import java.nio.file.*;
import java.util.*;

import org.apache.juneau.rest.server.views.RunEvent.*;

/** Shared helpers for the reader tests. */
final class ReportTestSupport {

	private ReportTestSupport() {}

	static Path resource(String name) {
		try {
			var u = ReportTestSupport.class.getResource(name);
			if (u == null)
				throw new IllegalStateException("missing test resource " + name);
			return Path.of(u.toURI());
		} catch (URISyntaxException e) {
			throw new IllegalStateException(e);
		}
	}

	static List<String> statuses(ReportResult r) {
		return r.tests().stream().map(t -> t.status().name().toLowerCase(Locale.ROOT)).toList();
	}

	static TestStatus none() {
		return null;
	}
}
