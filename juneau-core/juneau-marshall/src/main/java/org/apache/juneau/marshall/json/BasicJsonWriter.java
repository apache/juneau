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
package org.apache.juneau.marshall.json;

import java.io.*;

import org.apache.juneau.marshall.*;

/**
 * Concrete {@link JsonWriter} leaf for standard JSON output.
 *
 * <p>
 * This is the directly-instantiable form of {@link JsonWriter}, which is itself an abstract CRTP self-type base so it
 * can also be extended (e.g. by {@link org.apache.juneau.marshall.jcs.JcsWriter}).
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>
 * 		This class is not intended for external use.
 * </ul>
 */
public final class BasicJsonWriter extends JsonWriter<BasicJsonWriter> {

	/**
	 * Constructor.
	 *
	 * @param out The writer being wrapped.
	 * @param useWhitespace If <jk>true</jk>, tabs and spaces will be used in output.
	 * @param maxIndent The maximum indentation level.
	 * @param escapeSolidus If <jk>true</jk>, forward slashes should be escaped in the output.
	 * @param quoteChar The quote character to use (i.e. <js>'\''</js> or <js>'"'</js>)
	 * @param simpleAttrs If <jk>true</jk>, JSON attributes will only be quoted when necessary.
	 * @param trimStrings If <jk>true</jk>, strings will be trimmed before being serialized.
	 * @param uriResolver The URI resolver for resolving URIs to absolute or root-relative form.
	 */
	@SuppressWarnings({
		"java:S107" // Constructor requires 8 parameters for JSON writer configuration
	})
	public BasicJsonWriter(Writer out, boolean useWhitespace, int maxIndent, boolean escapeSolidus, char quoteChar, boolean simpleAttrs, boolean trimStrings, UriResolver uriResolver) {
		super(out, useWhitespace, maxIndent, escapeSolidus, quoteChar, simpleAttrs, trimStrings, uriResolver);
	}
}
