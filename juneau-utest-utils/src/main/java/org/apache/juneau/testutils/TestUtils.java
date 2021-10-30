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
package org.apache.juneau.testutils;

import java.util.*;

@SuppressWarnings({})
public class TestUtils {

	private static ThreadLocal<TimeZone> systemTimeZone = new ThreadLocal<>();
	private static ThreadLocal<Locale> systemLocale = new ThreadLocal<>();

	/**
	 * Temporarily sets the default system timezone to the specified timezone ID.
	 * Use {@link #unsetTimeZone()} to unset it.
	 *
	 * @param name
	 */
	public synchronized static final void setTimeZone(String name) {
		systemTimeZone.set(TimeZone.getDefault());
		TimeZone.setDefault(TimeZone.getTimeZone(name));
	}

	public synchronized static final void unsetTimeZone() {
		TimeZone.setDefault(systemTimeZone.get());
	}

	/**
	 * Temporarily sets the default system locale to the specified locale.
	 * Use {@link #unsetLocale()} to unset it.
	 *
	 * @param name
	 */
	public static final void setLocale(Locale locale) {
		systemLocale.set(Locale.getDefault());
		Locale.setDefault(locale);
	}

	public static final void unsetLocale() {
		Locale.setDefault(systemLocale.get());
	}
}
