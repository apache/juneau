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

import static org.apache.juneau.common.internal.Utils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.swap.*;

/**
 * Tester class for round-trip string swap testing designed for JUnit 5 parameterized tests.
 */
public class StringSwapTester<T> {

	public static <T> Builder<T> create(int index, String label, T object, StringSwap<T> swap, String expected, BeanSession beanSession) {
		return new Builder<>("["+index+"] " + label, ()->object, swap, expected, beanSession);
	}

	public static class Builder<T> {
		private String label;
		private Supplier<T> objectSupplier;
		private StringSwap<T> swap;
		private String expected;
		private BeanSession beanSession;
		private String exceptionMsg;

		public Builder(String label, Supplier<T> objectSupplier, StringSwap<T> swap, String expected, BeanSession beanSession) {
			this.label = label;
			this.objectSupplier = objectSupplier;
			this.swap = swap;
			this.expected = expected;
			this.beanSession = beanSession;
		}

		public Builder<T> exceptionMsg(String v) {
			exceptionMsg = v;
			return this;
		}

		public StringSwapTester<T> build() {
			return new StringSwapTester<>(this);
		}
	}

	private final String label;
	private final Supplier<T> objectSupplier;
	private final StringSwap<T> swap;
	private final String expected;
	private final BeanSession beanSession;
	private final String exceptionMsg;

	private StringSwapTester(Builder<T> b) {
		label = b.label;
		objectSupplier = b.objectSupplier;
		swap = b.swap;
		expected = b.expected;
		beanSession = b.beanSession;
		exceptionMsg = b.exceptionMsg;
	}

	public void testSwap() throws Exception {
		try {
			var o = objectSupplier.get();
			var s = swap.swap(beanSession, o);
			if (ne(expected, s)) {
				if (expected.isEmpty()) {
					if (! label.startsWith("[]"))
						System.err.println(label.substring(0, label.indexOf(']')+1) + " "+s);  // NOT DEBUG
					fail("Test [" + label + " swap] failed - expected was empty but got: " + s);
				} else {
					fail("Test [" + label + " swap] failed. Expected=[" + expected + "], Actual=[" + s + "]");
				}
			}
		} catch (AssertionError e) {
			if (exceptionMsg == null)
				throw e;
			assertTrue(e.getMessage().contains(exceptionMsg), fs("Expected exception message to contain: {0}, but was {1}.", exceptionMsg, e.getMessage()));
		} catch (Exception e) {
			if (exceptionMsg == null)
				throw new AssertionError("Test [" + label + " swap] failed with exception: " + e.getLocalizedMessage(), e);
			assertTrue(e.getMessage().contains(exceptionMsg), fs("Expected exception message to contain: {0}, but was {1}.", exceptionMsg, e.getMessage()));
		}
	}

	public void testUnswap() throws Exception {
		try {
			var o = objectSupplier.get();
			var s = swap.swap(beanSession, o);
			var o2 = swap.unswap(beanSession, s, beanSession.getClassMetaForObject(o));
			var s2 = swap.swap(beanSession, o2);
			if (ne(s, s2)) {
				if (expected.isEmpty())
					fail("Test [" + label + " unswap] failed - expected was empty");
				fail("Test [" + label + " unswap] failed. Expected=[" + s + "], Actual=[" + s2 + "]");
			}
		} catch (AssertionError e) {
			if (exceptionMsg == null)
				throw e;
			assertTrue(e.getMessage().contains(exceptionMsg), fs("Expected exception message to contain: {0}, but was {1}.", exceptionMsg, e.getMessage()));
		} catch (Exception e) {
			if (exceptionMsg == null)
				throw new AssertionError("Test [" + label + " unswap] failed with exception: " + e.getLocalizedMessage(), e);
			assertTrue(e.getMessage().contains(exceptionMsg), fs("Expected exception message to contain: {0}, but was {1}.", exceptionMsg, e.getMessage()));
		}
	}

	@Override
	public String toString() {
		return "StringSwapTester: " + label;
	}
}