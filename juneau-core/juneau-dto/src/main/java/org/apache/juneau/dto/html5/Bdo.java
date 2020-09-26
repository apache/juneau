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
 * DTO for an HTML {@doc ExtHTML5.text-level-semantics#the-bdo-element <bdo>}
 * element.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc DtoHtml5}
 * </ul>
 */
@Bean(typeName="bdo")
public class Bdo extends HtmlElementMixed {

	/**
	 * Creates an empty {@link Bdo} element.
	 */
	public Bdo() {}

	/**
	 * Creates a {@link Bdo} element with the specified {@link Bdo#dir(String)} attribute and child nodes.
	 *
	 * @param dir The {@link Bdo#dir(String)} attribute.
	 * @param children The child nodes.
	 */
	public Bdo(String dir, Object...children) {
		dir(dir).children(children);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Overridden methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* HtmlElement */
	public final Bdo _class(String _class) {
		super._class(_class);
		return this;
	}

	@Override /* HtmlElement */
	public Bdo dir(String dir) {
		super.dir(dir);
		return this;
	}

	@Override /* HtmlElement */
	public final Bdo id(String id) {
		super.id(id);
		return this;
	}

	@Override /* HtmlElement */
	public final Bdo style(String style) {
		super.style(style);
		return this;
	}

	@Override /* HtmlElementMixed */
	public Bdo children(Object...children) {
		super.children(children);
		return this;
	}

	@Override /* HtmlElementMixed */
	public Bdo child(Object child) {
		super.child(child);
		return this;
	}
}
