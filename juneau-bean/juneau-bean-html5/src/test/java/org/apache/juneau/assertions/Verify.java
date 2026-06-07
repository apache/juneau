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
package org.apache.juneau.assertions;

/**
 * Minimal Verify stub for juneau-marshall test scope.
 * The real implementation lives in juneau-assertions which depends on juneau-marshall,
 * so we cannot add it as a test dependency here.
 */
public class Verify {

	public static Verify verify(Object o) {
		return new Verify(o);
	}

	private final Object o;

	protected Verify(Object o) {
		this.o = o;
	}

	public String is(Object expected) {
		if (expected == o) return null;
		if (expected == null || o == null || !expected.equals(o))
			return "Expected=" + expected + ", actual=" + o;
		return null;
	}

	public String isFalse() { return is(false); }

	public String isTrue() { return is(true); }

	public String isType(Class<?> type) {
		if ((type == null && o == null) || (type != null && type.isInstance(o)))
			return null;
		var c = o == null ? null : o.getClass();
		return "Expected type=" + (type == null ? "null" : type.getName()) + ", actual=" + (c == null ? "null" : c.getName());
	}
}
