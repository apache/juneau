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

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.transform.*;
import org.junit.runner.*;
import org.junit.runners.*;

@RunWith(Parameterized.class)
public class LocaleSwapTest extends RoundTripStringSwapTest<Locale> {

	//------------------------------------------------------------------------------------------------------------------
	// Setup
	//------------------------------------------------------------------------------------------------------------------

	private static BeanSession BS = BeanContext.DEFAULT.createBeanSession();
	private static LocaleSwap SWAP = new LocaleSwap();

	public LocaleSwapTest(String label, Locale o, StringSwap<Locale> s, String r, BeanSession bs) throws Exception {
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
				"[0] Language only ",
				Locale.ENGLISH,
				SWAP,
				"en",
				BS
			},
			{
				"[1] Language and country",
				Locale.JAPAN,
				SWAP,
				"ja-JP",
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