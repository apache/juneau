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
 * Implements the 'exit' console command to gracefully shut down the microservice and JVM.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-microservice-core">juneau-microservice-core</a>
 * </ul>
 */
public class ExitCommand extends ConsoleCommand {

	private final Messages mb = Messages.of(ExitCommand.class, "Messages");

	@Override /* ConsoleCommand */
	public String getName() {
		return "exit";
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
		try {
			Microservice.getInstance().stop().exit();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}
}
