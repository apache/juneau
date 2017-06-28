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
 *
 * <p>
 * Context properties are set by calling {@link PropertyStore#setProperty(String, Object)} on the property store
 * passed into the constructor.
 *
 * <p>
 * See {@link PropertyStore} for more information about context properties.
 */
public class UrlEncodingSerializerContext extends UonSerializerContext {


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
	public UrlEncodingSerializerContext(PropertyStore ps) {
		super(ps);
		this.expandedParams = ps.getProperty(UrlEncodingContext.URLENC_expandedParams, boolean.class, false);
	}

	@Override /* Context */
	public ObjectMap asMap() {
		return super.asMap()
			.append("UrlEncodingSerializerContext", new ObjectMap()
				.append("expandedParams", expandedParams)
			);
	}
}
