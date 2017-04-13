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
 * Configurable properties on the {@link UrlEncodingSerializer} class.
 * <p>
 * Context properties are set by calling {@link PropertyStore#setProperty(String, Object)} on the property store
 * passed into the constructor.
 * <p>
 * See {@link PropertyStore} for more information about context properties.
 */
public class UrlEncodingSerializerContext extends UonSerializerContext {

	/**
	 * <b>Configuration property:</b>  Format to use for top-level query names and simple parameters.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"UrlEncodingSerializer.paramFormat"</js>
	 * 	<li><b>Data type:</b> <code>String</code>
	 * 	<li><b>Default:</b> <js>"UON"</js>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * Specifies the format to use for URL GET parameter keys and values.
	 * <p>
	 * The possible values are:
	 * <ul>
	 * 	<li><js>"UON"</js> (default) - Use UON notation for values.
	 * 		<br>String values such as <js>"(foo='bar')"</js> will end up being quoted and escaped to <js>"'(foo=bar~'baz~')'"</js>.
	 * 		<br>Similarly, boolean and numeric values will also end up quoted.
	 * 	<li><js>"PLAINTEXT"</js> (default) - Serialize as plain text.
	 * 		<br>Strings will never be quoted or escaped.
	 * 		<br>Note that this can cause errors during parsing if you're using the URL-encoding parser to parse
	 * 		the results since UON constructs won't be differentiatable.
	 * 		<br>However, this is not an issue if you're simply creating queries or form posts against 3rd-party interfaces.
	 * </ul>
	 */
	public static final String URLENC_paramFormat = "UrlEncodingSerializer.paramFormat";


	final boolean
		expandedParams,
		plainTextParams;

	/**
	 * Constructor.
	 * <p>
	 * Typically only called from {@link PropertyStore#getContext(Class)}.
	 *
	 * @param ps The property store that created this context.
	 */
	public UrlEncodingSerializerContext(PropertyStore ps) {
		super(ps);
		this.expandedParams = ps.getProperty(UrlEncodingContext.URLENC_expandedParams, boolean.class, false);
		this.plainTextParams = ps.getProperty(UrlEncodingSerializerContext.URLENC_paramFormat, String.class, "UON").equals("PLAINTEXT");
	}

	@Override /* Context */
	public ObjectMap asMap() {
		return super.asMap()
			.append("UrlEncodingSerializerContext", new ObjectMap()
				.append("expandedParams", expandedParams)
				.append("plainTextParams", plainTextParams)
			);
	}
}
