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
package org.apache.juneau.urlencoding;

import org.apache.juneau.*;
import org.apache.juneau.uon.*;

/**
 * Configurable properties on the {@link UrlEncodingParser} class.
 *
 * <p>
 * Context properties are set by calling {@link PropertyStore#setProperty(String, Object)} on the property store
 * passed into the constructor.
 *
 * <p>
 * See {@link PropertyStore} for more information about context properties.
 */
public class UrlEncodingParserContext extends UonParserContext {

	static final String PREFIX = "UrlEncodingParser.";

	/**
	 * Parser bean property collections/arrays as separate key/value pairs ({@link Boolean}, default=<jk>false</jk>).
	 *
	 * <p>
	 * This is the parser-side equivalent of the {@link UrlEncodingSerializerContext#URLENC_expandedParams} setting.
	 *
	 * <p>
	 * This option only applies to beans.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>If parsing multi-part parameters, it's highly recommended to use <code>Collections</code> or <code>Lists</code>
	 * 		as bean property types instead of arrays since arrays have to be recreated from scratch every time a value
	 * 		is added to it.
	 * </ul>
	 */
	public static final String URLENC_expandedParams = PREFIX + "expandedParams";

	final boolean
		expandedParams;

	/**
	 * Constructor.
	 *
	 * <p>
	 * Typically only called from {@link PropertyStore#getContext(Class)}.
	 *
	 * @param ps The property store that created this context.
	 */
	public UrlEncodingParserContext(PropertyStore ps) {
		super(ps);
		this.expandedParams = ps.getProperty(URLENC_expandedParams, boolean.class, false);
	}

	@Override /* Context */
	public ObjectMap asMap() {
		return super.asMap()
			.append("UrlEncodingParserContext", new ObjectMap()
				.append("expandedParams", expandedParams)
			);
	}
}
