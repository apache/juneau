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
package org.apache.juneau.parser;

import org.apache.juneau.*;

/**
 * Configurable properties common to all parsers.
 *
 *
 * <h6 class='topic' id='ConfigProperties'>Configurable properties common to all parsers</h6>
 * <table class='styled' style='border-collapse: collapse;'>
 * 	<tr><th>Setting name</th><th>Description</th><th>Data type</th><th>Default value</th></tr>
 * 	<tr>
 * 		<td>{@link #PARSER_debug}</td>
 * 		<td>Debug mode.</td>
 * 		<td><code>Boolean</code></td>
 * 		<td><jk>false</jk></td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link #PARSER_trimStrings}</td>
 * 		<td>Trim parsed strings.</td>
 * 		<td><code>Boolean</code></td>
 * 		<td><jk>false</jk></td>
 * 	</tr>
 * </table>
 *
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public class ParserContext extends Context {

	/**
	 * <b>Configuration property:</b>  Debug mode.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Parser.debug"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * Enables the following additional information during parsing:
	 * <ul class='spaced-list'>
	 * 	<li>When bean setters throws exceptions, the exception includes the object stack information
	 * 		in order to determine how that method was invoked.
	 * </ul>
	 */
	public static final String PARSER_debug = "Parser.debug";

	/**
	 * <b>Configuration property:</b>  Trim parsed strings.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Parser.trimStrings"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * If <jk>true</jk>, string values will be trimmed of whitespace using {@link String#trim()} before being added to the POJO.
	 */
	public static final String PARSER_trimStrings = "Parser.trimStrings";


	final boolean debug, trimStrings;

	/**
	 * Constructor.
	 *
	 * @param cf The factory that created this context.
	 */
	public ParserContext(ContextFactory cf) {
		super(cf);
		this.debug = cf.getProperty(PARSER_debug, boolean.class, false);
		this.trimStrings = cf.getProperty(PARSER_trimStrings, boolean.class, false);
	}
}
