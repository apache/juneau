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
package org.apache.juneau.jsonschema;

import java.net.*;
import java.text.*;

import org.apache.juneau.*;

/**
 * Simple implementation of the {@link BeanDefMapper} interface.
 *
 * <p>
 * IDs are created by calling {@link Class#getSimpleName()}.
 * <p>
 * URIs are constructed using the pattern <js>"#/definitions/{id}"</js>.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.JsonSchemaDetails">JSON-Schema Support</a>
 * </ul>
 */
public class BasicBeanDefMapper implements BeanDefMapper {

	private final MessageFormat format;

	/**
	 * Default constructor.
	 */
	public BasicBeanDefMapper() {
		this("#/definitions/{0}");
	}

	/**
	 * Constructor that allows you to override the URI pattern.
	 *
	 * @param uriPattern The URI pattern using {@link MessageFormat}-style arguments.
	 */
	protected BasicBeanDefMapper(String uriPattern) {
		format = new MessageFormat(uriPattern);
	}

	@Override /* BeanDefMapper */
	public String getId(ClassMeta<?> cm) {
		return cm.getSimpleName();
	}

	@Override /* BeanDefMapper */
	public URI getURI(ClassMeta<?> cm) {
		return getURI(getId(cm));
	}

	@Override /* BeanDefMapper */
	public URI getURI(String id) {
		return URI.create(format.format(new Object[]{id}));
	}
}
