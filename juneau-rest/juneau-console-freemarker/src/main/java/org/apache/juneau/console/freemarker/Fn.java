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
package org.apache.juneau.console.freemarker;

import static org.apache.juneau.commons.utils.Shorts.*;

import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.console.*;

/**
 * Builds {@code $fn} markers: JSON values the chart glue replaces with a function from {@code JuneauCharts.fn}.
 * Nothing is evaluated from a string; the marker is a name plus JSON arguments.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	ChartDef.<jsm>line</jsm>(<js>"t"</js>).src(<js>"/s"</js>).x(<js>"d"</js>).series(<js>"v"</js>, <js>"V"</js>)
 * 		.patch(JsonMap.<jsm>of</jsm>(<js>"tooltip"</js>, JsonMap.<jsm>of</jsm>(<js>"valueFormatter"</js>, Fn.<jsm>ref</jsm>(<js>"compact"</js>))));
 * </p>
 *
 * @since 10.0.0
 */
public final class Fn {

	private Fn() {}

	/** @param name A built-in (bare) or app (dotted) fn name. @return The marker {@code {"$fn": name}}. */
	public static JsonMap ref(String name) {
		return ref(name, null);
	}

	/**
	 * @param name A built-in (bare) or app (dotted) fn name.
	 * @param args The factory arguments, or <jk>null</jk>.
	 * @return The marker {@code {"$fn": name, "args": args}}.
	 * @throws IllegalArgumentException E-74 if the name is not a valid extension name.
	 */
	public static JsonMap ref(String name, JsonMap args) {
		checkName(name, "$fn");
		var m = JsonMap.of("$fn", name);
		if (nn(args))
			m.put("args", args);
		return m;
	}

	// The one E-74 name predicate: Fn.checkName and ChartCardType both use it.
	static boolean validName(String name) {
		return name != null && BusWiringValidator.EXTENSION_NAME.matcher(name).matches();
	}

	static void checkName(String name, String what) {
		if (! validName(name))
			throw new IllegalArgumentException("E-74: " + f(BusWiringValidator.E74, name, what));
	}
}
