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
import org.apache.juneau.cp.*;
import org.apache.juneau.microservice.*;

/**
 * Implements the 'restart' console command to gracefully shut down and restart the microservice.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-microservice-core">juneau-microservice-core</a>
 * </ul>
 */
public class HelpCommand extends ConsoleCommand {

	private final Messages mb = Messages.of(HelpCommand.class, "Messages");

	@Override /* ConsoleCommand */
	public String getName() {
		return "help";
	}

	@Override /* ConsoleCommand */
	public String getSynopsis() {
		return "help [command]";
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
	public String getExamples() {
		return mb.getString("examples");
	}

	@Override /* ConsoleCommand */
	public boolean execute(Scanner in, PrintWriter out, Args args) throws Exception {
		Map<String,ConsoleCommand> commands = Microservice.getInstance().getConsoleCommands();
		if (args.size() == 1) {
			out.println(mb.getString("ListOfAvailableCommands"));
			commands.forEach((k,v) -> out.append("\t").append(v.getName()).append(" -- ").append(indent(v.getInfo())).println());
			out.println();
		} else {
			ConsoleCommand cc = commands.get(args.getArg(1));
			if (cc == null) {
				out.println(mb.getString("CommandNotFound"));
			} else {
				String
					info = cc.getInfo(),
					synopsis = cc.getSynopsis(),
					description = cc.getDescription(),
					examples = cc.getExamples();

				out.append(mb.getString("NAME")).append("\n\t").append(cc.getName()).append(info == null ? "" : " -- " + indent(info)).println();

				if (synopsis != null)
					out.append('\n').append(mb.getString("SYNOPSIS")).append("\n\t").append(indent(synopsis)).println();

				if (description != null)
					out.append('\n').append(mb.getString("DESCRIPTION")).append("\n\t").append(indent(description)).println();

				if (examples != null)
					out.append('\n').append(mb.getString("EXAMPLES")).append("\n\t").append(indent(examples)).println();
			}
		}
		return false;
	}

	private String indent(String in) {
		if (in == null)
			return "";
		return in.replaceAll("\n", "\n\t");
	}
}
