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
import org.apache.juneau.swap.*;
import org.apache.juneau.swaps.*;
import org.junit.runner.*;
import org.junit.runners.*;

@RunWith(Parameterized.class)
public class FunctionalSwapTest extends RoundTripObjectSwapTest<Locale,String> {

	//------------------------------------------------------------------------------------------------------------------
	// Setup
	//------------------------------------------------------------------------------------------------------------------

	private static BeanSession BS = BeanContext.DEFAULT_SESSION;
	private static final LocaleSwap localeSwap = new LocaleSwap();
	private static FunctionalSwap<Locale,String> SWAP = new FunctionalSwap<>(Locale.class, String.class, x->localeSwap.swap(BS, x), x->localeSwap.unswap(BS, x, null));

	public FunctionalSwapTest(String label, Locale o, FunctionalSwap<Locale,String> s, String r, BeanSession bs) {
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