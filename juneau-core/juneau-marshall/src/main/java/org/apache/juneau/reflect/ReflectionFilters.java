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
package org.apache.juneau.reflect;

import java.util.function.*;

import org.apache.juneau.*;

/**
 * Predefined predicates for filtering out executable methods/constructors.
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public class ReflectionFilters {

	/**
	 * Predicate for testing that a method or constructor has the exact specified args.
	 *
	 * @param args The args to test.
	 * @return A new predicate.
	 */
	public static <T extends ExecutableInfo> Predicate<T> hasArgs(Class<?>...args) {
		return new Predicate<T>() {
			@Override
			public boolean test(T t) {
				Class<?>[] pt = t._getRawParamTypes();
				if (pt.length == args.length) {
					for (int i = 0; i < pt.length; i++)
						if (! pt[i].equals(args[i]))
							return false;
					return true;
				}
				return false;
			}
		};
	}

	/**
	 * Predicate for testing that a method or constructor has arguments that will take all of the specified
	 * arguments.
	 *
	 * <p>
	 * Unlike {@link #hasArgs(Class...)}, this allows for matching based on parent classes.
	 *
	 * @param args The args to test.
	 * @return A new predicate.
	 */
	public static <T extends ExecutableInfo> Predicate<T> hasParentArgs(Object...args) {
		return new Predicate<T>() {
			@Override
			public boolean test(T t) {
				ClassInfo[] pt = t._getParamTypes();
				if (pt.length != args.length)
					return false;
				for (int i = 0; i < pt.length; i++) {
					boolean matched = false;
					for (int j = 0; j < args.length; j++)
						if (pt[i].isParentOfFuzzyPrimitives(args[j].getClass()))
							matched = true;
					if (! matched)
						return false;
				}
				return true;
			}
		};
	}

	/**
	 * Predicate for testing that a method or constructor has the minimum specified visibility.
	 * arguments.
	 *
	 * <p>
	 * Unlike {@link #hasArgs(Class...)}, this allows for matching based on parent classes.
	 *
	 * @param value The minimum visibility for the method or constructor.
	 * @return A new predicate.
	 */
	public static <T extends ExecutableInfo> Predicate<T> isVisible(Visibility value) {
		return new Predicate<T>() {
			@Override
			public boolean test(T t) {
				return t.isVisible(value);
			}
		};
	}
}