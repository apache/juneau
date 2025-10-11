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

import static org.apache.juneau.common.utils.Utils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Verifies that the getUri() methods in UriContext work correctly.
 */
class UriContextUriCombo_Test extends TestBase {

	private static final Data[] DATA = {

		// Happy cases - All URL parts known.
		data(
			"Happy-1",
			input(
				"http://foo.com:123","/context","/resource","/path"
			),
			results(
				"http://foo.com:123",
				"http://foo.com:123/context",
				"http://foo.com:123/context/resource",
				"http://foo.com:123/context/resource/path",
				"/context",
				"/context/resource",
				"/context/resource/path"
			)
		),
		data(
			"Happy-2",
			input(
				"http://foo.com:123","/c1/c2","/r1/r2","/p1/p2"
			),
			results(
				"http://foo.com:123",
				"http://foo.com:123/c1/c2",
				"http://foo.com:123/c1/c2/r1/r2",
				"http://foo.com:123/c1/c2/r1/r2/p1/p2",
				"/c1/c2",
				"/c1/c2/r1/r2",
				"/c1/c2/r1/r2/p1/p2"
			)
		),
		data(
			"NoAuthority-1",
			input(
				"","/context","/resource","/path"
			),
			results(
				"/",
				"/context",
				"/context/resource",
				"/context/resource/path",
				"/context",
				"/context/resource",
				"/context/resource/path"
			)
		),
		data(
			"NoContext-1",
			input(
				"http://foo.com:123","","/resource","/path"
			),
			results(
				"http://foo.com:123",
				"http://foo.com:123",
				"http://foo.com:123/resource",
				"http://foo.com:123/resource/path",
				"/",
				"/resource",
				"/resource/path"
			)
		),
		data(
			"NoResource-1",
			input(
				"http://foo.com:123","/context","","/path"
			),
			results(
				"http://foo.com:123",
				"http://foo.com:123/context",
				"http://foo.com:123/context",
				"http://foo.com:123/context/path",
				"/context",
				"/context",
				"/context/path"
			)
		),
		data(
			"NoPath-1",
			input(
				"http://foo.com:123","/context","/resource",""
			),
			results(
				"http://foo.com:123",
				"http://foo.com:123/context",
				"http://foo.com:123/context/resource",
				"http://foo.com:123/context/resource",
				"/context",
				"/context/resource",
				"/context/resource"
			)
		),
		data(
			"NoAuthorityNoContext-1",
			input(
				"","","/resource","/path"
			),
			results(
				"/",
				"/",
				"/resource",
				"/resource/path",
				"/",
				"/resource",
				"/resource/path"
			)
		),
		data(
			"NoContextNoResource-1",
			input(
				"http://foo.com:123","","","/path"
			),
			results(
				"http://foo.com:123",
				"http://foo.com:123",
				"http://foo.com:123",
				"http://foo.com:123/path",
				"/",
				"/",
				"/path"
			)
		),
		data(
			"NoAuthorityNoContextNoResource-1",
			input(
				"","","","/path"
			),
			results(
				"/",
				"/",
				"/",
				"/path",
				"/",
				"/",
				"/path"
			)
		),
		data(
			"Nothing-1",
			input(
				"","","",""
			),
			results(
				"/",
				"/",
				"/",
				"/",
				"/",
				"/",
				"/"
			)
		)
	};

	private static class Data {
		final String label;
		final Input in;
		final Results r;

		public Data(String label, Input in, Results r) {
			this.label = label;
			this.in = in;
			this.r = r;
		}
	}

	private static class Input {
		final UriContext uriContext;

		public Input(String authority, String context, String resource, String path) {
			this.uriContext = UriContext.of(authority, context, resource, path);
		}
	}

	private static class Results {
		final String eAbsoluteAuthority, eAbsoluteContext, eAbsoluteResource, eAbsolutePath, eRootRelativeContext, eRootRelativeResource, eRootRelativePath;

		public Results(String eAbsoluteAuthority, String eAbsoluteContext, String eAbsoluteResource, String eAbsolutePath,
					String eRootRelativeContext, String eRootRelativeResource, String eRootRelativePath) {
			this.eAbsoluteAuthority = eAbsoluteAuthority;
			this.eAbsoluteContext = eAbsoluteContext;
			this.eAbsoluteResource = eAbsoluteResource;
			this.eAbsolutePath = eAbsolutePath;
			this.eRootRelativeContext = eRootRelativeContext;
			this.eRootRelativeResource = eRootRelativeResource;
			this.eRootRelativePath = eRootRelativePath;
		}
	}

	public static Data data(String label, Input input, Results results) {
		return new Data(label, input, results);
	}

	public static Input input(String authority, String context, String resource, String path) {
		return new Input(authority, context, resource, path);
	}

	public static Results results(String eAbsoluteAuthority, String eAbsoluteContext, String eAbsoluteResource, String eAbsolutePath,
			String eRootRelativeContext, String eRootRelativeResource, String eRootRelativePath) {
		return new Results(eAbsoluteAuthority, eAbsoluteContext, eAbsoluteResource, eAbsolutePath, eRootRelativeContext, eRootRelativeResource, eRootRelativePath);
	}

	static Data[] data() {
		return DATA;
	}

	@ParameterizedTest
	@MethodSource("data")
	void a01_testAbsoluteAuthority(Data d) {
		assertEquals(d.r.eAbsoluteAuthority, d.in.uriContext.getAbsoluteAuthority(), fms("{0}: testAbsoluteAuthority() failed", d.label));
	}

	@ParameterizedTest
	@MethodSource("data")
	void a02_testAbsoluteContext(Data d) {
		assertEquals(d.r.eAbsoluteContext, d.in.uriContext.getAbsoluteContextRoot(), fms("{0}: testAbsoluteContext() failed", d.label));
	}

	@ParameterizedTest
	@MethodSource("data")
	void a03_testAbsoluteResource(Data d) {
		assertEquals(d.r.eAbsoluteResource, d.in.uriContext.getAbsoluteServletPath(), fms("{0}: testAbsoluteResource() failed", d.label));
	}

	@ParameterizedTest
	@MethodSource("data")
	void a04_testAbsolutePath(Data d) {
		assertEquals(d.r.eAbsolutePath, d.in.uriContext.getAbsolutePathInfo(), fms("{0}: testAbsolutePath() failed", d.label));
	}

	@ParameterizedTest
	@MethodSource("data")
	void a05_testRootRelativeContext(Data d) {
		assertEquals(d.r.eRootRelativeContext, d.in.uriContext.getRootRelativeContextRoot(), fms("{0}: testRootRelativeContext() failed", d.label));
	}

	@ParameterizedTest
	@MethodSource("data")
	void a06_testRootRelativeResource(Data d) {
		assertEquals(d.r.eRootRelativeResource, d.in.uriContext.getRootRelativeServletPath(), fms("{0}: testRootRelativeResource() failed", d.label));
	}

	@ParameterizedTest
	@MethodSource("data")
	void a07_testRootRelativePath(Data d) {
		assertEquals(d.r.eRootRelativePath, d.in.uriContext.getRootRelativePathInfo(), fms("{0}: testRootRelativePath() failed", d.label));
	}
}