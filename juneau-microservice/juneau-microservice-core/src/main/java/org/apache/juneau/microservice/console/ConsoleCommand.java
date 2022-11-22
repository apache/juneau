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

/**
 * Implements a command that can be invoked from the console of the microservice.
 *
 * <p>
 * Console commands allow you to interact with your microservice through the system console.
 *
 * <p>
 * Console commands are associated with the microservice through the following:
 * <ul>
 * 	<li>The <js>"Console/commands"</js> configuration value.
 * 		<br>This is a comma-delimited list of fully-qualified names of classes implementing this interface.
 * 		<br>When associated this way, the implementation class must have a no-arg constructor.
 * 	<li>Specifying commands via the {@link org.apache.juneau.microservice.Microservice.Builder#consoleCommands(Class...)} method.
 * 		<br>This allows you to override the default implementation above and augment or replace the list
 * 			with your own implementation objects.
 * </ul>
 *
 * <p>
 * For example, the {@link HelpCommand} is used to provide help on other commands.
 *
 * <p class='bconsole'>
 * 	Running class 'JettyMicroservice' using config file 'examples.cfg'.
 * 	Server started on port 10000
 *
 * 	List of available commands:
 * 		exit -- Shut down service
 * 		restart -- Restarts service
 * 		help -- Commands help
 * 		echo -- Echo command
 *
 * 	&gt; <span style='color:green'>help help</span>
 * 	NAME
 * 		help -- Commands help
 *
 * 	SYNOPSIS
 * 		help [command]
 *
 * 	DESCRIPTION
 * 		When called without arguments, prints the descriptions of all available commands.
 * 		Can also be called with one or more arguments to get detailed information on a command.
 *
 * 	EXAMPLES
 * 		List all commands:
 * 			&gt; help
 *
 * 		List help on the help command:
 * 			&gt; help help
 *
 * 	&gt;
 * </p>
 *
 * <p>
 * The arguments are available as an {@link Args} object which allows for easy accessed to parsed command lines.
 * Some simple examples of valid command lines:
 *
 * <p class='bjava'>
 * 	<jc>// mycommand</jc>
 * 	<jv>args</jv>.get(<js>"0"</js>);  <jc>// "mycommand"</jc>
 *
 * 	<jc>// mycommand arg1 arg2</jc>
 * 	<jv>args</jv>.get(<js>"0"</js>);  <jc>// "mycommand"</jc>
 * 	<jv>args</jv>.get(<js>"1"</js>);  <jc>// "arg1"</jc>
 * 	<jv>args</jv>.get(<js>"2"</js>);  <jc>// "arg2"</jc>
 *
 * 	<jc>// mycommand -optArg1 foo bar -optArg2 baz qux</jc>
 * 	<jv>args</jv>.get(<js>"0"</js>);  <jc>// "mycommand"</jc>
 * 	<jv>args</jv>.get(<js>"optArg1"</js>, String[].<jk>class</jk>);  <jc>// ["foo","bar"]</jc>
 * 	<jv>args</jv>.get(<js>"optArg2"</js>, String[].<jk>class</jk>);  <jc>// ["baz","qux"]</jc>
 *
 * 	<jc>// mycommand -optArg1 "foo bar" -optArg2 'baz qux'</jc>
 * 	<jv>args</jv>.get(<js>"0"</js>);  <jc>// "mycommand"</jc>
 * 	<jv>args</jv>.get(<js>"optArg1"</js>, String[].<jk>class</jk>);  <jc>// ["foo bar"]</jc>
 * 	<jv>args</jv>.get(<js>"optArg2"</js>, String[].<jk>class</jk>);  <jc>// ["baz qux"]</jc>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-microservice-core">juneau-microservice-core</a>
 * </ul>
 */
public abstract class ConsoleCommand {

	/**
	 * Returns the name of the command.
	 *
	 * <p>
	 * Example:  <js>"help"</js> for the help command.
	 *
	 * @return
	 * 	The name of the command.
	 * 	<br>Must not be <jk>null</jk> or contain spaces.
	 */
	abstract public String getName();

	/**
	 * Returns the usage synopsis of the command.
	 *
	 * <p>
	 * Example:  <js>"help [command ...]"
	 *
	 * <p>
	 * The default implementation just returns the name, which implies the command takes no additional arguments.
	 *
	 * @return The synopsis of the command.
	 */
	public String getSynopsis() {
		return getName();
	}

	/**
	 * Returns a one-line localized description of the command.
	 *
	 * <p>
	 * The locale should be the system locale.
	 *
	 * @return
	 * 	The localized description of the command.
	 * 	<br>Can be <jk>null</jk> if there is no information.
	 */
	public String getInfo() {
		return null;
	}

	/**
	 * Returns localized details of the command.
	 *
	 * <p>
	 * The locale should be the system locale.
	 *
	 * @return
	 * 	The localized details of the command.
	 * 	<br>Can be <jk>null</jk> if there is no additional description.
	 */
	public String getDescription() {
		return null;
	}

	/**
	 * Returns localized examples of the command.
	 *
	 * <p>
	 * The locale should be the system locale.
	 *
	 * @return
	 * 	The localized examples of the command.
	 * 	<br>Can be <jk>null</jk> if there is no examples.
	 */
	public String getExamples() {
		return null;
	}

	/**
	 * Executes a command.
	 * @param in The console reader.
	 * @param out The console writer.
	 * @param args The command arguments.  The first argument is always the command itself.
	 *
	 * @return
	 * 	<jk>true</jk> if the console read thread should exit.
	 * 	<br>Normally you want to return <jk>true</jk> if your action is causing the microservice to exit or restart.
	 * @throws Exception
	 * 	Any thrown exception will simply be sent to STDERR.
	 */
	abstract public boolean execute(Scanner in, PrintWriter out, Args args) throws Exception;
}
