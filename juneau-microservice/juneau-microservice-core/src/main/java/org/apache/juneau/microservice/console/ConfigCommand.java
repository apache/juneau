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
package org.apache.juneau.microservice.console;

import java.io.*;
import java.util.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.config.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.microservice.*;

/**
 * Implements the 'config' console command to get or set configuration.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-microservice-core">juneau-microservice-core</a>
 * </ul>
 */
public class ConfigCommand extends ConsoleCommand {

	private final Messages mb = Messages.of(ConfigCommand.class, "Messages");

	@Override /* ConsoleCommand */
	public String getName() {
		return "config";
	}

	@Override /* ConsoleCommand */
	public String getSynopsis() {
		return "config [get|set]";
	}

	@Override /* ConsoleCommand */
	public String getInfo() {
		return mb.getString("info");
	}

	@Override /* ConsoleCommand */
	public String getDescription() {
		return mb.getString("description");
	}

	@Override /* ConsoleCommand */
	public boolean execute(Scanner in, PrintWriter out, Args args) {
		Config conf = Microservice.getInstance().getConfig();
		if (args.size() > 2) {
			String option = args.getArg(1);
			String key = args.getArg(2);
			if (option.equals("get")) {
				// config get <key>
				if (args.size() == 3) {
					String val = conf.get(key).orElse(null);
					if (val != null)
						out.println(val);
					else
						out.println(mb.getString("KeyNotFound", key));
				} else {
					out.println(mb.getString("TooManyArguments"));
				}
			} else if (option.equals("set")) {
				// config set <key> <value>
				if (args.size() == 4) {
					conf.set(key, args.getArg(3));
					out.println(mb.getString("ConfigSet"));
				} else if (args.size() < 4) {
					out.println(mb.getString("InvalidArguments"));
				} else {
					out.println(mb.getString("TooManyArguments"));
				}
			} else if (option.equals("remove")) {
				// config remove <key>
				if (args.size() == 3) {
					if (conf.get(key).isPresent()) {
						conf.remove(key);
						out.println(mb.getString("ConfigRemove", key));
					} else {
						out.println(mb.getString("KeyNotFound", key));
					}
				} else {
					out.println(mb.getString("TooManyArguments"));
				}
			} else {
				out.println(mb.getString("InvalidArguments"));
			}
		} else {
			out.println(mb.getString("InvalidArguments"));
		}
		return false;
	}
}
