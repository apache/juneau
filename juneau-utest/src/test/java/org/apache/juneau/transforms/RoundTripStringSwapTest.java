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

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.*;
import org.apache.juneau.swap.*;
import org.junit.*;

/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
@FixMethodOrder(NAME_ASCENDING)
public abstract class RoundTripStringSwapTest<T> {

	private final String label;
	private final T o;
	private final StringSwap<T> ss;
	private final String expected;
	private final BeanSession bs;

	public RoundTripStringSwapTest(String label, T o, StringSwap<T> ss, String expected, BeanSession bs) {
		this.label = label;
		this.o = o;
		this.ss = ss;
		this.expected = expected;
		this.bs = bs;
	}

	@Test
	public void testSwap() throws Exception {
		String s = ss.swap(bs, o);
		if (ne(expected, s)) {
			if (expected.isEmpty()) {
				if (! label.startsWith("[]"))
					System.err.println(label.substring(0, label.indexOf(']')+1) + " "+s);  // NOT DEBUG
				Assert.fail();
			} else {
				fail("Test [{0} swap] failed.  Expected=[{1}], Actual=[{2}]", label, expected, s);
			}
		}
	}

	@Test
	public void testUnswap() throws Exception {
		String s = ss.swap(bs, o);
		T o2 = ss.unswap(bs, s, bs.getClassMetaForObject(o));
		String s2 = ss.swap(bs, o2);
		if (ne(s, s2)) {
			if (expected.isEmpty())
				Assert.fail();
			System.err.println("s=["+s+"], o=["+o+"], o.type=["+o.getClass().getName()+"], o2=["+o2+"], o2.type=["+o2.getClass().getName()+"]");  // NOT DEBUG
			fail("Test [{0} unswap] failed.  Expected=[{1}], Actual=[{2}]", label, s, s2);
		}
	}

	private void fail(String msg, Object...args) {
		String s = format(msg, args);
		System.err.println(s);  // NOT DEBUG
		Assert.fail(s);
	}
}