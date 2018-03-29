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
package org.apache.juneau.utils;

import java.util.concurrent.atomic.*;

/**
 * Predefined ID generators.
 */
public class IdGenerators {
	
	/**
	 * Creates an ID generator using {@link AtomicInteger} initialized to value <code>1</code>.
	 * 
	 * @param initValue The initial value. 
	 * @return A new ID generator.
	 */
	public static IdGenerator<Integer> createIntGenerator(final int initValue) {
		return new IdGenerator<Integer>() {
			private final AtomicInteger i = new AtomicInteger(initValue);
			
			@Override /* IdGenerator */
			public Integer next() {
				return i.getAndIncrement();
			}
		};
	}

	/**
	 * Creates an ID generator using {@link AtomicInteger} initialized to the specified value.
	 * 
	 * @return A new ID generator.
	 */
	public static IdGenerator<Integer> createIntGenerator() {
		return createIntGenerator(1);
	}

	/**
	 * Creates an ID generator using {@link AtomicLong} initialized to value <code>1</code>.
	 * 
	 * @param initValue The initial value. 
	 * @return A new ID generator.
	 */
	public static IdGenerator<Long> createLongGenerator(final long initValue) {
		return new IdGenerator<Long>() {
			private final AtomicLong l = new AtomicLong(initValue);
			
			@Override /* IdGenerator */
			public Long next() {
				return l.getAndIncrement();
			}
		};
	}

	/**
	 * Creates an ID generator using {@link AtomicLong} initialized to the specified value.
	 * 
	 * @return A new ID generator.
	 */
	public static IdGenerator<Long> createLongGenerator() {
		return createLongGenerator(1);
	}
}
