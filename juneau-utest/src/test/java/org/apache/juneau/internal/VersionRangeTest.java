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

import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;

@RunWith(Parameterized.class)
@FixMethodOrder(NAME_ASCENDING)
public class VersionRangeTest {

	@Parameterized.Parameters
	public static Collection<Object[]> getPairs() {
		return Arrays.asList(new Object[][] {
			// {range, version, shouldMatch } - NOSONAR
			/* 00 */ {"1.1", "1.1.3", true},
			/* 01 */ {"1.1", "1.1", true},
			/* 02 */ {"1.1", "1.1.0", true},
			/* 03 */ {"1.1", "1.0", false},
			/* 04 */ {"1.1", "1.0.9", false},
			/* 05 */ {"[1.0,2.0)", ".9", false},
			/* 06 */ {"[1.0,2.0)", "1", true},
			/* 07 */ {"[1.0,2.0)", "1.0", true},
			/* 08 */ {"[1.0,2.0)", "1.0.0", true},
			/* 09 */ {"[1.0,2.0)", "1.1", true},
			/* 10 */ {"[1.0,2.0)", "2.0", false},
			/* 11 */ {"[1.0,2.0)", "2", false},
			/* 12 */ {"(1.0,2.0]", "2", true},
			/* 13 */ {"(1.0,2.0]", "2.0", true},
			/* 14 */ {"(1.0,2.0]", "2.0.1", true},
			/* 15 */ {"(1.0,2.0]", "2.1", false},
			/* 16 */ {"(.5.0,.6]", ".5", false},
			/* 17 */ {"(.5.0,.6]", ".5.1", true},
			/* 18 */ {"(.5.0,.6]", ".6", true},
			/* 19 */ {"(.5.0,.6]", ".6.1", true},
			/* 20 */ {"(.5.0,.6]", ".7", false},
			/* 21 */ {"[1.1,2.0)", "1", false},
		});
	}

	private VersionRange range;
	private String version;
	private boolean shouldMatch;

	public VersionRangeTest(String range, String version, boolean shouldMatch) {
		this.version = version;
		this.range = new VersionRange(range);
		this.shouldMatch = shouldMatch;
	}

	@Test
	public void test() throws Exception {
		assertEquals(shouldMatch, range.matches(version));
	}
}