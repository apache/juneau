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
package org.apache.juneau.utils;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.UriRelativity.*;
import static org.apache.juneau.UriResolution.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Verifies that the resolveUri() methods in UriContext work correctly.
 */
class UriContextResolutionCombo_Test extends TestBase {

	private static final Tester[] TESTERS = {

		// Happy cases - All URL parts known.
		tester(1,
			"Happy-1a",
			input(
				"http://host:port","/context","/resource","/path",
				"http://foo.com:123/foobar"
			),
			results(
				"http://foo.com:123/foobar",
				"http://foo.com:123/foobar",
				"http://foo.com:123/foobar",
				"http://foo.com:123/foobar",
				"http://foo.com:123/foobar",
				"http://foo.com:123/foobar"
			)
		),
		tester(2,
			"Happy-2",
			input(
				"http://host:port","/context","/resource","/path",
				"http://foo.com:123"
			),
			results(
				"http://foo.com:123",
				"http://foo.com:123",
				"http://foo.com:123",
				"http://foo.com:123",
				"http://foo.com:123",
				"http://foo.com:123"
			)
		),
		tester(3,
			"Happy-3",
			input(
				"http://host:port","/context","/resource","/path",
				"/foobar"
			),
			results(
				"http://host:port/foobar",
				"http://host:port/foobar",
				"/foobar",
				"/foobar",
				"/foobar",
				"/foobar"
			)
		),
		tester(4,
			"Happy-4",
			input(
				"http://host:port","/context","/resource","/path",
				"/"
			),
			results(
				"http://host:port",
				"http://host:port",
				"/",
				"/",
				"/",
				"/"
			)
		),
		tester(5,
			"Happy-5",
			input(
				"http://host:port","/context","/resource","/path",
				"foobar"
			),
			results(
				"http://host:port/context/resource/foobar",
				"http://host:port/context/resource/foobar",
				"/context/resource/foobar",
				"/context/resource/foobar",
				"foobar",
				"foobar"
			)
		),
		tester(6,
			"Happy-6",
			input(
				"http://host:port","/context","/resource","/path",
				""
			),
			results(
				"http://host:port/context/resource",
				"http://host:port/context/resource/path",
				"/context/resource",
				"/context/resource/path",
				"",
				""
			)
		),
		tester(7,
			"Happy-7",
			input(
				"http://host:port","/context","/resource","/path",
				"context:/foo"
			),
			results(
				"http://host:port/context/foo",
				"http://host:port/context/foo",
				"/context/foo",
				"/context/foo",
				"/context/foo",
				"/context/foo"
			)
		),
		tester(8,
			"Happy-8",
			input(
				"http://host:port","/context","/resource","/path",
				"context:/"
			),
			results(
				"http://host:port/context",
				"http://host:port/context",
				"/context",
				"/context",
				"/context",
				"/context"
			)
		),
		tester(9,
			"Happy-9",
			input(
				"http://host:port","/context","/resource","/path",
				"servlet:/foo"
			),
			results(
				"http://host:port/context/resource/foo",
				"http://host:port/context/resource/foo",
				"/context/resource/foo",
				"/context/resource/foo",
				"/context/resource/foo",
				"/context/resource/foo"
			)
		),
		tester(10,
			"Happy-10",
			input(
				"http://host:port","/context","/resource","/path",
				"servlet:/"
			),
			results(
				"http://host:port/context/resource",
				"http://host:port/context/resource",
				"/context/resource",
				"/context/resource",
				"/context/resource",
				"/context/resource"
			)
		),

		// Multiple context and resource parts
		tester(11,
			"MultiContextResource-1",
			input(
				"http://host:port","/c1/c2","/r1/r2","/p1/p2",
				"http://foo.com:123/foobar"
			),
			results(
				"http://foo.com:123/foobar",
				"http://foo.com:123/foobar",
				"http://foo.com:123/foobar",
				"http://foo.com:123/foobar",
				"http://foo.com:123/foobar",
				"http://foo.com:123/foobar"
			)
		),
		tester(12,
			"MultiContextResource-2",
			input(
				"http://host:port","/c1/c2","/r1/r2","/p1/p2",
				"http://foo.com:123"
			),
			results(
				"http://foo.com:123",
				"http://foo.com:123",
				"http://foo.com:123",
				"http://foo.com:123",
				"http://foo.com:123",
				"http://foo.com:123"
			)
		),
		tester(13,
			"MultiContextResource-3",
			input(
				"http://host:port","/c1/c2","/r1/r2","/p1/p2",
				"/foobar"
			),
			results(
				"http://host:port/foobar",
				"http://host:port/foobar",
				"/foobar",
				"/foobar",
				"/foobar",
				"/foobar"
			)
		),
		tester(14,
			"MultiContextResource-4",
			input(
				"http://host:port","/c1/c2","/r1/r2","/p1/p2",
				"/"
			),
			results(
				"http://host:port",
				"http://host:port",
				"/",
				"/",
				"/",
				"/"
			)
		),
		tester(15,
			"MultiContextResource-5",
			input(
				"http://host:port","/c1/c2","/r1/r2","/p1/p2",
				"foobar"
			),
			results(
				"http://host:port/c1/c2/r1/r2/foobar",
				"http://host:port/c1/c2/r1/r2/p1/foobar",
				"/c1/c2/r1/r2/foobar",
				"/c1/c2/r1/r2/p1/foobar",
				"foobar",
				"foobar"
			)
		),
		tester(16,
			"MultiContextResource-6",
			input(
				"http://host:port","/c1/c2","/r1/r2","/p1/p2",
				""
			),
			results(
				"http://host:port/c1/c2/r1/r2",
				"http://host:port/c1/c2/r1/r2/p1/p2",
				"/c1/c2/r1/r2",
				"/c1/c2/r1/r2/p1/p2",
				"",
				""
			)
		),
		tester(17,
			"MultiContextResource-7",
			input(
				"http://host:port","/c1/c2","/r1/r2","/p1/p2",
				"context:/foo"
			),
			results(
				"http://host:port/c1/c2/foo",
				"http://host:port/c1/c2/foo",
				"/c1/c2/foo",
				"/c1/c2/foo",
				"/c1/c2/foo",
				"/c1/c2/foo"
			)
		),
		tester(18,
			"MultiContextResource-8",
			input(
				"http://host:port","/c1/c2","/r1/r2","/p1/p2",
				"context:/"
			),
			results(
				"http://host:port/c1/c2",
				"http://host:port/c1/c2",
				"/c1/c2",
				"/c1/c2",
				"/c1/c2",
				"/c1/c2"
			)
		),
		tester(19,
			"MultiContextResource-9",
			input(
				"http://host:port","/c1/c2","/r1/r2","/p1/p2",
				"servlet:/foo"
			),
			results(
				"http://host:port/c1/c2/r1/r2/foo",
				"http://host:port/c1/c2/r1/r2/foo",
				"/c1/c2/r1/r2/foo",
				"/c1/c2/r1/r2/foo",
				"/c1/c2/r1/r2/foo",
				"/c1/c2/r1/r2/foo"
			)
		),
		tester(20,
			"MultiContextResource-10",
			input(
				"http://host:port","/c1/c2","/r1/r2","/p1/p2",
				"servlet:/"
			),
			results(
				"http://host:port/c1/c2/r1/r2",
				"http://host:port/c1/c2/r1/r2",
				"/c1/c2/r1/r2",
				"/c1/c2/r1/r2",
				"/c1/c2/r1/r2",
				"/c1/c2/r1/r2"
			)
		),

		// No authority given
		tester(21,
			"NoAuthority-1",
			input(
				"","/context","/resource","/path",
				"http://foo.com:123/foobar"
			),
			results(
				"http://foo.com:123/foobar",
				"http://foo.com:123/foobar",
				"http://foo.com:123/foobar",
				"http://foo.com:123/foobar",
				"http://foo.com:123/foobar",
				"http://foo.com:123/foobar"
			)
		),
		tester(22,
			"NoAuthority-2",
			input(
				"","/context","/resource","/path",
				"http://foo.com:123"
			),
			results(
				"http://foo.com:123",
				"http://foo.com:123",
				"http://foo.com:123",
				"http://foo.com:123",
				"http://foo.com:123",
				"http://foo.com:123"
			)
		),
		tester(23,
			"NoAuthority-3",
			input(
				"","/context","/resource","/path",
				"/foobar"
			),
			results(
				"/foobar",
				"/foobar",
				"/foobar",
				"/foobar",
				"/foobar",
				"/foobar"
			)
		),
		tester(24,
			"NoAuthority-4",
			input(
				"","/context","/resource","/path",
				"/"
			),
			results(
				"/",
				"/",
				"/",
				"/",
				"/",
				"/"
			)
		),
		tester(25,
			"NoAuthority-5",
			input(
				"","/context","/resource","/path",
				"foobar"
			),
			results(
				"/context/resource/foobar",
				"/context/resource/foobar",
				"/context/resource/foobar",
				"/context/resource/foobar",
				"foobar",
				"foobar"
			)
		),
		tester(26,
			"NoAuthority-6",
			input(
				"","/context","/resource","/path",
				""
			),
			results(
				"/context/resource",
				"/context/resource/path",
				"/context/resource",
				"/context/resource/path",
				"",
				""
			)
		),
		tester(27,
			"NoAuthority-7",
			input(
				"","/context","/resource","/path",
				"context:/foo"
			),
			results(
				"/context/foo",
				"/context/foo",
				"/context/foo",
				"/context/foo",
				"/context/foo",
				"/context/foo"
			)
		),
		tester(28,
			"NoAuthority-8",
			input(
				"","/context","/resource","/path",
				"context:/"
			),
			results(
				"/context",
				"/context",
				"/context",
				"/context",
				"/context",
				"/context"
			)
		),
		tester(29,
			"NoAuthority-9",
			input(
				"","/context","/resource","/path",
				"servlet:/foo"
			),
			results(
				"/context/resource/foo",
				"/context/resource/foo",
				"/context/resource/foo",
				"/context/resource/foo",
				"/context/resource/foo",
				"/context/resource/foo"
			)
		),
		tester(30,
			"NoAuthority-10",
			input(
				"","/context","/resource","/path",
				"servlet:/"
			),
			results(
				"/context/resource",
				"/context/resource",
				"/context/resource",
				"/context/resource",
				"/context/resource",
				"/context/resource"
			)
		),

		// No authority or context given
		tester(31,
			"NoAuthorityOrContext-1",
			input(
				"","","/resource","/path",
				"http://foo.com:123/foobar"
			),
			results(
				"http://foo.com:123/foobar",
				"http://foo.com:123/foobar",
				"http://foo.com:123/foobar",
				"http://foo.com:123/foobar",
				"http://foo.com:123/foobar",
				"http://foo.com:123/foobar"
			)
		),
		tester(32,
			"NoAuthorityOrContext-2",
			input(
				"","","/resource","/path",
				"http://foo.com:123"
			),
			results(
				"http://foo.com:123",
				"http://foo.com:123",
				"http://foo.com:123",
				"http://foo.com:123",
				"http://foo.com:123",
				"http://foo.com:123"
			)
		),
		tester(33,
			"NoAuthorityOrContext-3",
			input(
				"","","/resource","/path",
				"/foobar"
			),
			results(
				"/foobar",
				"/foobar",
				"/foobar",
				"/foobar",
				"/foobar",
				"/foobar"
			)
		),
		tester(34,
			"NoAuthorityOrContext-4",
			input(
				"","","/resource","/path",
				"/"
			),
			results(
				"/",
				"/",
				"/",
				"/",
				"/",
				"/"
			)
		),
		tester(35,
			"NoAuthorityOrContext-5",
			input(
				"","","/resource","/path",
				"foobar"
			),
			results(
				"/resource/foobar",
				"/resource/foobar",
				"/resource/foobar",
				"/resource/foobar",
				"foobar",
				"foobar"
			)
		),
		tester(36,
			"NoAuthorityOrContext-6",
			input(
				"","","/resource","/path",
				""
			),
			results(
				"/resource",
				"/resource/path",
				"/resource",
				"/resource/path",
				"",
				""
			)
		),
		tester(37,
			"NoAuthorityOrContext-7",
			input(
				"","","/resource","/path",
				"context:/foo"
			),
			results(
				"/foo",
				"/foo",
				"/foo",
				"/foo",
				"/foo",
				"/foo"
			)
		),
		tester(38,
			"NoAuthorityOrContext-8",
			input(
				"","","/resource","/path",
				"context:/"
			),
			results(
				"/",
				"/",
				"/",
				"/",
				"/",
				"/"
			)
		),
		tester(39,
			"NoAuthorityOrContext-9",
			input(
				"","","/resource","/path",
				"servlet:/foo"
			),
			results(
				"/resource/foo",
				"/resource/foo",
				"/resource/foo",
				"/resource/foo",
				"/resource/foo",
				"/resource/foo"
			)
		),
		tester(40,
			"NoAuthorityOrContext-10",
			input(
				"","","/resource","/path",
				"servlet:/"
			),
			results(
				"/resource",
				"/resource",
				"/resource",
				"/resource",
				"/resource",
				"/resource"
			)
		),

		// No authority or context or resource given
		tester(41,
			"NoAuthorityOrContextOrResource-1",
			input(
				"","","","/path",
				"http://foo.com:123/foobar"
			),
			results(
				"http://foo.com:123/foobar",
				"http://foo.com:123/foobar",
				"http://foo.com:123/foobar",
				"http://foo.com:123/foobar",
				"http://foo.com:123/foobar",
				"http://foo.com:123/foobar"
			)
		),
		tester(42,
			"NoAuthorityOrContextOrResource-2",
			input(
				"","","","/path",
				"http://foo.com:123"
			),
			results(
				"http://foo.com:123",
				"http://foo.com:123",
				"http://foo.com:123",
				"http://foo.com:123",
				"http://foo.com:123",
				"http://foo.com:123"
			)
		),
		tester(43,
			"NoAuthorityOrContextOrResource-3",
			input(
				"","","","/path",
				"/foobar"
			),
			results(
				"/foobar",
				"/foobar",
				"/foobar",
				"/foobar",
				"/foobar",
				"/foobar"
			)
		),
		tester(44,
			"NoAuthorityOrContextOrResource-4",
			input(
				"","","","/path",
				"/"
			),
			results(
				"/",
				"/",
				"/",
				"/",
				"/",
				"/"
			)
		),
		tester(45,
			"NoAuthorityOrContextOrResource-5",
			input(
				"","","","/path",
				"foobar"
			),
			results(
				"/foobar",
				"/foobar",
				"/foobar",
				"/foobar",
				"foobar",
				"foobar"
			)
		),
		tester(46,
			"NoAuthorityOrContextOrResource-6",
			input(
				"","","","/path",
				""
			),
			results(
				"/",
				"/path",
				"/",
				"/path",
				"",
				""
			)
		),
		tester(47,
			"NoAuthorityOrContextOrResource-7",
			input(
				"","","","/path",
				"context:/foo"
			),
			results(
				"/foo",
				"/foo",
				"/foo",
				"/foo",
				"/foo",
				"/foo"
			)
		),
		tester(48,
			"NoAuthorityOrContextOrResource-8",
			input(
				"","","","/path",
				"context:/"
			),
			results(
				"/",
				"/",
				"/",
				"/",
				"/",
				"/"
			)
		),
		tester(49,
			"NoAuthorityOrContextOrResource-9",
			input(
				"","","","/path",
				"servlet:/foo"
			),
			results(
				"/foo",
				"/foo",
				"/foo",
				"/foo",
				"/foo",
				"/foo"
			)
		),
		tester(50,
			"NoAuthorityOrContextOrResource-10",
			input(
				"","","","/path",
				"servlet:/"
			),
			results(
				"/",
				"/",
				"/",
				"/",
				"/",
				"/"
			)
		),

		// No context or resource given.
		tester(51,
			"NoContextOrResource-1",
			input(
				"http://host:port","","","/path",
				"http://foo.com:123/foobar"
			),
			results(
				"http://foo.com:123/foobar",
				"http://foo.com:123/foobar",
				"http://foo.com:123/foobar",
				"http://foo.com:123/foobar",
				"http://foo.com:123/foobar",
				"http://foo.com:123/foobar"
			)
		),
		tester(52,
			"NoContextOrResource-2",
			input(
				"http://host:port","","","/path",
				"http://foo.com:123"
			),
			results(
				"http://foo.com:123",
				"http://foo.com:123",
				"http://foo.com:123",
				"http://foo.com:123",
				"http://foo.com:123",
				"http://foo.com:123"
			)
		),
		tester(53,
			"NoContextOrResource-3",
			input(
				"http://host:port","","","/path",
				"/foobar"
			),
			results(
				"http://host:port/foobar",
				"http://host:port/foobar",
				"/foobar",
				"/foobar",
				"/foobar",
				"/foobar"
			)
		),
		tester(54,
			"NoContextOrResource-4",
			input(
				"http://host:port","","","/path",
				"/"
			),
			results(
				"http://host:port",
				"http://host:port",
				"/",
				"/",
				"/",
				"/"
			)
		),
		tester(55,
			"NoContextOrResource-5",
			input(
				"http://host:port","","","/path",
				"foobar"
			),
			results(
				"http://host:port/foobar",
				"http://host:port/foobar",
				"/foobar",
				"/foobar",
				"foobar",
				"foobar"
			)
		),
		tester(56,
			"NoContextOrResource-6",
			input(
				"http://host:port","","","/path",
				""
			),
			results(
				"http://host:port",
				"http://host:port/path",
				"/",
				"/path",
				"",
				""
			)
		),
		tester(57,
			"NoContextOrResource-7",
			input(
				"http://host:port","","","/path",
				"context:/foo"
			),
			results(
				"http://host:port/foo",
				"http://host:port/foo",
				"/foo",
				"/foo",
				"/foo",
				"/foo"
			)
		),
		tester(58,
			"NoContextOrResource-8",
			input(
				"http://host:port","","","/path",
				"context:/"
			),
			results(
				"http://host:port",
				"http://host:port",
				"/",
				"/",
				"/",
				"/"
			)
		),
		tester(59,
			"NoContextOrResource-9",
			input(
				"http://host:port","","","/path",
				"servlet:/foo"
			),
			results(
				"http://host:port/foo",
				"http://host:port/foo",
				"/foo",
				"/foo",
				"/foo",
				"/foo"
			)
		),
		tester(60,
			"NoContextOrResource-10",
			input(
				"http://host:port","","","/path",
				"servlet:/"
			),
			results(
				"http://host:port",
				"http://host:port",
				"/",
				"/",
				"/",
				"/"
			)
		),

		// Test query string support (JUNEAU-117)
		tester(61,
			"request:?query",
			input(
				"http://host:port","/context","/resource","/path",
				"request:?foo=bar"
			),
			results(
				"http://host:port/context/resource/path?foo=bar",
				"http://host:port/context/resource/path?foo=bar",
				"/context/resource/path?foo=bar",
				"/context/resource/path?foo=bar",
				"/context/resource/path?foo=bar",
				"/context/resource/path?foo=bar"
			)
		),
		tester(62,
			"servlet:?query",
			input(
				"http://host:port","/context","/resource","/path",
				"servlet:?foo=bar"
			),
			results(
				"http://host:port/context/resource?foo=bar",
				"http://host:port/context/resource?foo=bar",
				"/context/resource?foo=bar",
				"/context/resource?foo=bar",
				"/context/resource?foo=bar",
				"/context/resource?foo=bar"
			)
		),
		tester(63,
			"context:?query",
			input(
				"http://host:port","/context","/resource","/path",
				"context:?foo=bar"
			),
			results(
				"http://host:port/context?foo=bar",
				"http://host:port/context?foo=bar",
				"/context?foo=bar",
				"/context?foo=bar",
				"/context?foo=bar",
				"/context?foo=bar"
			)
		),
		tester(64,
			"request:#hash",
			input(
				"http://host:port","/context","/resource","/path",
				"request:#section"
			),
			results(
				"http://host:port/context/resource/path#section",
				"http://host:port/context/resource/path#section",
				"/context/resource/path#section",
				"/context/resource/path#section",
				"/context/resource/path#section",
				"/context/resource/path#section"
			)
		),
		tester(65,
			"request:pathOnly",
			input(
				"http://host:port","/context","/resource","/path",
				"request:foo"
			),
			results(
				"http://host:port/context/resource/path/foo",
				"http://host:port/context/resource/path/foo",
				"/context/resource/path/foo",
				"/context/resource/path/foo",
				"/context/resource/path/foo",
				"/context/resource/path/foo"
			)
		),
		tester(66,
			"request:empty",
			input(
				"http://host:port","/context","/resource","/path",
				"request:"
			),
			results(
				"http://host:port/context/resource/path",
				"http://host:port/context/resource/path",
				"/context/resource/path",
				"/context/resource/path",
				"/context/resource/path",
				"/context/resource/path"
			)
		)
	};

	private static class Tester {
		final String label;
		final Input input;
		final Results results;

		Tester(int index, String label, Input input, Results results) {
			this.label = "[" + index + "] " + label;
			this.input = input;
			this.results = results;
		}
	}

	private static class Input {
		final String uri;
		final String authority, context, resource, path;

		Input(String authority, String context, String resource, String path, String uri) {
			this.authority = authority;
			this.context = context;
			this.resource = resource;
			this.path = path;
			this.uri = uri;
		}
	}

	private static class Results {
		final String aResource, aPathInfo, rrResource, rrPathInfo, nResource, nPathInfo;

		public Results(String aResource, String aPathInfo, String rrResource, String rrPathInfo, String nResource, String nPathInfo) {
			this.aResource = aResource;
			this.aPathInfo = aPathInfo;
			this.rrResource = rrResource;
			this.rrPathInfo = rrPathInfo;
			this.nResource = nResource;
			this.nPathInfo = nPathInfo;
		}
	}

	public static Tester tester(int index, String label, Input input, Results results) {
		return new Tester(index, label, input, results);
	}

	public static Input input(String authority, String context, String resource, String path, String uri) {
		return new Input(authority, context, resource, path, uri);
	}

	public static Results results(String eAbsResource, String eAbsPathInfo, String eRrResource, String eRrPathInfo, String eNoneResource, String eNonePathInfo) {
		return new Results(eAbsResource, eAbsPathInfo, eRrResource, eRrPathInfo, eNoneResource, eNonePathInfo);
	}

	static Tester[] testers() {
		return TESTERS;
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a01_testAbsoluteResource(Tester t) {
		var x = UriResolver.of(ABSOLUTE, RESOURCE, UriContext.of(t.input.authority, t.input.context, t.input.resource, t.input.path)).resolve(t.input.uri);
		assertEquals(t.results.aResource, x, fs("{0}: testAbsolute() failed", t.label));
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a02_testAppendAbsoluteResource(Tester t) {
		var x = UriResolver.of(ABSOLUTE, RESOURCE, UriContext.of(t.input.authority, t.input.context, t.input.resource, t.input.path)).append(new StringBuilder(), t.input.uri);
		assertString(t.results.aResource, x, fs("{0}: testAbsolute() failed", t.label));
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a03_testAbsolutePathInfo(Tester t) {
		var x = UriResolver.of(ABSOLUTE, PATH_INFO, UriContext.of(t.input.authority, t.input.context, t.input.resource, t.input.path)).resolve(t.input.uri);
		assertEquals(t.results.aPathInfo, x, fs("{0}: testAbsolute() failed", t.label));
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a04_testAppendAbsolutePathInfo(Tester t) {
		var x = UriResolver.of(ABSOLUTE, PATH_INFO, UriContext.of(t.input.authority, t.input.context, t.input.resource, t.input.path)).append(new StringBuilder(), t.input.uri);
		assertString(t.results.aPathInfo, x, fs("{0}: testAbsolute() failed", t.label));
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a05_testRootRelativeResource(Tester t) {
		var x = UriResolver.of(ROOT_RELATIVE, RESOURCE, UriContext.of(t.input.authority, t.input.context, t.input.resource, t.input.path)).resolve(t.input.uri);
		assertEquals(t.results.rrResource, x, fs("{0}: testAbsolute() failed", t.label));
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a06_testAppendRootRelativeResource(Tester t) {
		var x = UriResolver.of(ROOT_RELATIVE, RESOURCE, UriContext.of(t.input.authority, t.input.context, t.input.resource, t.input.path)).append(new StringBuilder(), t.input.uri);
		assertString(t.results.rrResource, x, fs("{0}: testAbsolute() failed", t.label));
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a07_testRootRelativePathInfo(Tester t) {
		var x = UriResolver.of(ROOT_RELATIVE, PATH_INFO, UriContext.of(t.input.authority, t.input.context, t.input.resource, t.input.path)).resolve(t.input.uri);
		assertEquals(t.results.rrPathInfo, x, fs("{0}: testAbsolute() failed", t.label));
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a08_testAppendRootRelativePathInfo(Tester t) {
		var x = UriResolver.of(ROOT_RELATIVE, PATH_INFO, UriContext.of(t.input.authority, t.input.context, t.input.resource, t.input.path)).append(new StringBuilder(), t.input.uri);
		assertString(t.results.rrPathInfo, x, fs("{0}: testAbsolute() failed", t.label));
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a09_testNoneResource(Tester t) {
		var x = UriResolver.of(NONE, RESOURCE, UriContext.of(t.input.authority, t.input.context, t.input.resource, t.input.path)).resolve(t.input.uri);
		assertEquals(t.results.nResource, x, fs("{0}: testAbsolute() failed", t.label));
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a10_testAppendNoneResource(Tester t) {
		var x = UriResolver.of(NONE, RESOURCE, UriContext.of(t.input.authority, t.input.context, t.input.resource, t.input.path)).append(new StringBuilder(), t.input.uri);
		assertString(t.results.nResource, x, fs("{0}: testAbsolute() failed", t.label));
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a11_testNonePathInfo(Tester t) {
		var x = UriResolver.of(NONE, PATH_INFO, UriContext.of(t.input.authority, t.input.context, t.input.resource, t.input.path)).resolve(t.input.uri);
		assertEquals(t.results.nPathInfo, x, fs("{0}: testAbsolute() failed", t.label));
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a12_testAppendNonePathInfo(Tester t) {
		var x = UriResolver.of(NONE, PATH_INFO, UriContext.of(t.input.authority, t.input.context, t.input.resource, t.input.path)).append(new StringBuilder(), t.input.uri);
		assertString(t.results.nPathInfo, x, fs("{0}: testAbsolute() failed", t.label));
	}
}