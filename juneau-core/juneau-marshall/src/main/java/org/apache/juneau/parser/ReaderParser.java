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
 * Subclass of {@link Parser} for characters-based parsers.
 * 
 * 
 * <h5 class='topic'>Description</h5>
 * 
 * This class is typically the parent class of all character-based parsers.
 * It has 1 abstract method to implement...
 * <ul>
 * 	<li><code>parse(ParserSession, ClassMeta)</code>
 * </ul>
 */
public abstract class ReaderParser extends Parser {

	/**
	 * Constructor.
	 * 
	 * @param ps The property store containing all the settings for this object.
	 * @param consumes The list of media types that this parser consumes (e.g. <js>"application/json"</js>, <js>"*&#8203;/json"</js>).
	 */
	protected ReaderParser(PropertyStore ps, String...consumes) {
		super(ps, consumes);
	}

	@Override /* Parser */
	public final boolean isReaderParser() {
		return true;
	}
}
