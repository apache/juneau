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

class FunctionalSwapTest extends RoundTripObjectSwapTest_Base {

	//------------------------------------------------------------------------------------------------------------------
	// Setup
	//------------------------------------------------------------------------------------------------------------------

	private static BeanSession BS = BeanContext.DEFAULT_SESSION;
	private static final LocaleSwap localeSwap = new LocaleSwap();
	private static FunctionalSwap<Locale,String> SWAP = new FunctionalSwap<>(Locale.class, String.class, x->localeSwap.swap(BS, x), x->localeSwap.unswap(BS, x, null));

	private static <T,S> RoundTripObjectSwapTester<T,S> tester(int index, String label, T object, ObjectSwap<T,S> swap, S expected, BeanSession bs) {
		return RoundTripObjectSwapTester.create(index, label, object, swap, expected, bs).build();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Parameters
	//------------------------------------------------------------------------------------------------------------------

	private static final RoundTripObjectSwapTester<?,?>[] TESTERS = {
		tester(1, "Language only", Locale.ENGLISH, SWAP, "en", BS),
		tester(2, "Language and country", Locale.JAPAN, SWAP, "ja-JP", BS),
		tester(3, "null", null, SWAP, null, BS)
	};

	static RoundTripObjectSwapTester<?,?>[] testers() {
		return TESTERS;
	}
}