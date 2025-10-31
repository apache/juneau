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
package org.apache.juneau.internal;

import static org.apache.juneau.common.utils.Utils.*;

import org.apache.juneau.common.reflect.*;

/**
 * Class-related utility methods.
 *
 * <h5 class='section'>See Also:</h5><ul>

 * </ul>
 */
public class ClassUtils2 {

	/**
	 * Matches arguments to a list of parameter types.
	 *
	 * <p>
	 * Extra parameters are ignored.
	 * <br>Missing parameters are left null.
	 *
	 * @param paramTypes The parameter types.
	 * @param args The arguments to match to the parameter types.
	 * @return
	 * 	An array of parameters.
	 */
	public static Object[] getMatchingArgs(Class<?>[] paramTypes, Object...args) {
		boolean needsShuffle = paramTypes.length != args.length;
		if (! needsShuffle) {
			for (int i = 0; i < paramTypes.length; i++) {
				if (! paramTypes[i].isInstance(args[i]))
					needsShuffle = true;
			}
		}
		if (! needsShuffle)
			return args;
		Object[] params = new Object[paramTypes.length];
		for (int i = 0; i < paramTypes.length; i++) {
			var pt = ClassInfo.of(paramTypes[i]).getWrapperInfoIfPrimitive();
			for (var arg : args) {
				if (nn(arg) && pt.isParentOf(arg.getClass())) {
					params[i] = arg;
					break;
				}
			}
		}
		return params;
	}
}