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
import org.apache.juneau.annotation.*;

/**
 * Subclass of {@link Parser} for characters-based parsers.
 *
 * <h5 class='section'>Description:</h5>
 *
 * This class is typically the parent class of all character-based parsers.
 * It has 1 abstract method to implement...
 * <ul>
 * 	<li><code>parse(ParserSession, ClassMeta)</code>
 * </ul>
 *
 * <h6 class='topic'>@Consumes annotation</h6>
 *
 * The media types that this parser can handle is specified through the {@link Consumes @Consumes} annotation.
 *
 * <p>
 * However, the media types can also be specified programmatically by overriding the {@link #getMediaTypes()} method.
 */
public abstract class ReaderParser extends Parser {

	/**
	 * Constructor.
	 *
	 * @param propertyStore The property store containing all the settings for this object.
	 */
	protected ReaderParser(PropertyStore propertyStore) {
		super(propertyStore);
	}

	@Override /* Parser */
	public boolean isReaderParser() {
		return true;
	}
}
