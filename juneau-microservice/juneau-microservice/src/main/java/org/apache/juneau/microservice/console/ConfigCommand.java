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
package org.apache.juneau.microservice.console;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.commons.runtime.*;
import org.apache.juneau.marshall.cp.*;
import org.apache.juneau.microservice.*;

/**
 * Implements the 'config' console command to get or set configuration.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauMicroservice">juneau-microservice Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S115" // Constants use UPPER_snakeCase convention (e.g., MKEY_invalidArguments)
})
public class ConfigCommand extends ConsoleCommand {

	private final Messages mb = Messages.of(ConfigCommand.class, "Messages");

	@Override /* Overridden from ConsoleCommand */
	@SuppressWarnings({
		"java:S3776" // Cognitive complexity acceptable for console command logic
	})
	public boolean execute(Scanner in, PrintWriter out, Args args) {
		var conf = Microservice.getInstance().getConfig();
		var size = args.argCount() + args.optionCount();
		if (size > 2) {
			var option = args.get(1).orElse("");
			var key = args.get(2).orElse("");
			if (eq(option, "get")) {
				// config get <key>
				if (size == 3) {
					var val = conf.get(key).orElse(null);
					if (nn(val))
						out.println(val);
					else
						out.println(mb.getString("KeyNotFound", key));
				} else {
					out.println(mb.getString("TooManyArguments"));
				}
			} else if (eq(option, "set")) {
				// config set <key> <value>
				if (size == 4) {
					conf.set(key, args.get(3).orElse(null));
					out.println(mb.getString("ConfigSet"));
				} else if (size < 4) {
					out.println(mb.getString("InvalidArguments"));
				} else {
					out.println(mb.getString("TooManyArguments"));
				}
			} else if (eq(option, "remove")) {
				// config remove <key>
				if (size == 3) {
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

	@Override /* Overridden from ConsoleCommand */
	public String getDescription() { return mb.getString("description"); }

	@Override /* Overridden from ConsoleCommand */
	public String getInfo() { return mb.getString("info"); }

	@Override /* Overridden from ConsoleCommand */
	public String getName() { return "config"; }

	@Override /* Overridden from ConsoleCommand */
	public String getSynopsis() { return "config [get|set]"; }
}