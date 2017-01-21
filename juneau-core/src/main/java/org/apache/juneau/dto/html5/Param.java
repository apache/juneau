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
package org.apache.juneau.dto.html5;

import org.apache.juneau.annotation.*;

/**
 * DTO for an HTML <a href='https://www.w3.org/TR/html5/embedded-content-0.html#the-param-element'>&lt;param&gt;</a> element.
 * <p>
 */
@Bean(typeName="param")
public class Param extends HtmlElementEmpty {

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/embedded-content-0.html#attr-param-name'>name</a> attribute.
	 * Name of parameter.
	 * @param name - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Param name(String name) {
		attrs.put("name", name);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/embedded-content-0.html#attr-param-value'>value</a> attribute.
	 * Value of parameter.
	 * @param value - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Param value(String value) {
		attrs.put("value", value);
		return this;
	}

	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* HtmlElement */
	public final Param _class(String _class) {
		super._class(_class);
		return this;
	}

	@Override /* HtmlElement */
	public final Param id(String id) {
		super.id(id);
		return this;
	}
}
