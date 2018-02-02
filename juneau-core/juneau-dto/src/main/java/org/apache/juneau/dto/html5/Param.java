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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#the-param-element">&lt;param&gt;</a>
 * element.
 * 
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='link'>
 * 		<a class='doclink' href='../../../../../overview-summary.html#DTOs'>Juneau Data Transfer Objects
 * 		(org.apache.juneau.dto)</a>
 * 		<ul>
 * 			<li class='sublink'>
 * 				<a class='doclink' href='../../../../../overview-summary.html#DTOs.HTML5'>HTML5</a>
 * 		</ul>
 * 	</li>
 * </ul>
 */
@Bean(typeName="param")
public class Param extends HtmlElementVoid {

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-param-name">name</a> attribute.
	 * 
	 * <p>
	 * Name of parameter.
	 * 
	 * @param name The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Param name(String name) {
		attr("name", name);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-param-value">value</a>
	 * attribute.
	 * 
	 * <p>
	 * Value of parameter.
	 * 
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Param value(Object value) {
		attr("value", value);
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
