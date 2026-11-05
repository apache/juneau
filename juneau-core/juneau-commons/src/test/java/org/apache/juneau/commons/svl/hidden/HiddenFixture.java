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
package org.apache.juneau.commons.svl.hidden;

/** Fixture whose bean type is package-private in a different package from the evaluator (public getter, inaccessible class). */
public class HiddenFixture {

	private HiddenFixture() {}

	/** @return An instance of a non-public class with a public getter. */
	public static Object create() { return new Hid(); }

	/** @return An instance of a non-public class whose getter is declared by a public interface. */
	public static Object createViaInterface() { return new Hid2(); }

	/** Public interface declaring the getter. */
	public interface HasY {
		/** @return A value. */
		int getY();
	}

	static class Hid2 implements HasY {
		@Override public int getY() { return 2; }
	}

	static class Hid {
		public int getX() { return 1; }
	}
}
