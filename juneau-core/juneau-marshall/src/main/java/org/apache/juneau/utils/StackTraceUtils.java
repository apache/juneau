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

/**
 * Stack trace utility methods.
 */
public class StackTraceUtils {

	/**
	 * Calculates a 16-bit hash for the specified throwable based on it's stack trace.
	 *
	 * @param t The throwable to calculate the stack trace on.
	 * @param stopClass Optional stop class on which to stop calculation of a stack trace beyond when found.
	 * @return A calculated hash.
	 */
	public static int hash(Throwable t, String stopClass) {
		int i = 0;
		while (t != null) {
			for (StackTraceElement e : t.getStackTrace()) {
				if (e.getClassName().equals(stopClass))
					break;
				if (e.getClassName().indexOf('$') == -1)
					i ^= e.hashCode();
			}
			t = t.getCause();
		}
		return i;
	}
}
