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
package org.apache.juneau.transforms;

import static org.apache.juneau.utest.utils.Utils2.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.swap.*;
import org.apache.juneau.swaps.*;
import org.junit.runner.*;
import org.junit.runners.*;

@RunWith(Parameterized.class)
public class ReaderSwapTest extends OneWayStringSwapTest<Reader> {

	//------------------------------------------------------------------------------------------------------------------
	// Setup
	//------------------------------------------------------------------------------------------------------------------

	private static BeanSession BS = BeanContext.DEFAULT_SESSION;
	private static ReaderSwap SWAP = new ReaderSwap();

	public ReaderSwapTest(String label, Reader o, StringSwap<Reader> s, String r, BeanSession bs) throws Exception {
		super(label, o, s, r, bs);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Parameters
	//------------------------------------------------------------------------------------------------------------------

	@Parameterized.Parameters
	public static Collection<Object[]> getPairs() {
		return Arrays.asList(new Object[][] {

			//----------------------------------------------------------------------------------------------------------
			// Basic tests
			//----------------------------------------------------------------------------------------------------------
			{
				"[0] Basic string",
				reader("foo"),
				SWAP,
				"foo",
				BS
			},
			{
				"[1] Blank string",
				reader(""),
				SWAP,
				"",
				BS
			},
			{
				"[2] null",
				null,
				SWAP,
				null,
				BS
			},
		});
	}
}