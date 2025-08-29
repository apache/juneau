// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.internal;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

class VersionRange_Test extends SimpleTestBase {

	private static final Input[] INPUT = {
		/* 00 */ input("1.1", "1.1.3", true),
		/* 01 */ input("1.1", "1.1", true),
		/* 02 */ input("1.1", "1.1.0", true),
		/* 03 */ input("1.1", "1.0", false),
		/* 04 */ input("1.1", "1.0.9", false),
		/* 05 */ input("[1.0,2.0)", ".9", false),
		/* 06 */ input("[1.0,2.0)", "1", true),
		/* 07 */ input("[1.0,2.0)", "1.0", true),
		/* 08 */ input("[1.0,2.0)", "1.0.0", true),
		/* 09 */ input("[1.0,2.0)", "1.1", true),
		/* 10 */ input("[1.0,2.0)", "2.0", false),
		/* 11 */ input("[1.0,2.0)", "2", false),
		/* 12 */ input("(1.0,2.0]", "2", true),
		/* 13 */ input("(1.0,2.0]", "2.0", true),
		/* 14 */ input("(1.0,2.0]", "2.0.1", true),
		/* 15 */ input("(1.0,2.0]", "2.1", false),
		/* 16 */ input("(.5.0,.6]", ".5", false),
		/* 17 */ input("(.5.0,.6]", ".5.1", true),
		/* 18 */ input("(.5.0,.6]", ".6", true),
		/* 19 */ input("(.5.0,.6]", ".6.1", true),
		/* 20 */ input("(.5.0,.6]", ".7", false),
		/* 21 */ input("[1.1,2.0)", "1", false)
	};

	private static Input input(String range, String version, boolean shouldMatch) {
		return new Input(range, version, shouldMatch);
	}

	private static class Input {
		VersionRange range;
		String version;
		boolean shouldMatch;

		public Input(String range, String version, boolean shouldMatch) {
			this.version = version;
			this.range = new VersionRange(range);
			this.shouldMatch = shouldMatch;
		}
	}

	static Input[] input() {
		return INPUT;
	}

	@ParameterizedTest
	@MethodSource("input")
	void a01_basic(Input input) {
		assertEquals(input.shouldMatch, input.range.matches(input.version));
	}
}