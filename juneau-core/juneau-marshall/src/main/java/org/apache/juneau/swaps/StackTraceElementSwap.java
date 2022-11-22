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
package org.apache.juneau.swaps;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.swap.*;

/**
 * Transforms {@link StackTraceElement StackTraceElements} to {@code String} objects.
 *
 * <p>
 * The swap is identical to just calling {@link StackTraceElement#toString()}, but provides the ability to
 * parse the resulting string back into a bean.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.Swaps">Swaps</a>
 * </ul>
 */
public class StackTraceElementSwap extends ObjectSwap<StackTraceElement,String> {

	/**
	 * Converts the specified {@link Enumeration} to a {@link List}.
	 */
	@Override /* ObjectSwap */
	public String swap(BeanSession session, StackTraceElement o) {
		return o.toString();
	}

	@Override /* ObjectSwap */
	public StackTraceElement unswap(BeanSession session, String in, ClassMeta<?> hint) {
		String methodName = "", fileName = null;
		int lineNumber = -1;

		if (in == null)
			return null;

		int i = in.indexOf('(');
		if (i != -1) {
			String s = in.substring(i+1, in.lastIndexOf(')'));
			in = in.substring(0, i);
			i = s.indexOf(':');
			if (i != -1) {
				fileName = s.substring(0, i);
				lineNumber = Integer.parseInt(s.substring(i+1));
			} else if ("Native Method".equals(s)) {
				lineNumber = -2;
			} else if (! "Unknown Source".equals(s)) {
				fileName = s;
			}
 		}

		i = in.lastIndexOf('.');
		if (i != -1) {
			methodName = in.substring(i+1);
			in = in.substring(0, i);
		}

		return new StackTraceElement(in, methodName, fileName, lineNumber);
	}
}
