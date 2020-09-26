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
 * DTO for an HTML {@doc ExtHTML5.forms#the-datalist-element <datalist>}
 * element.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc DtoHtml5}
 * </ul>
 */
@Bean(typeName="datalist")
public class Datalist extends HtmlElementContainer {

	/**
	 * Creates an empty {@link Datalist} element.
	 */
	public Datalist() {}

	/**
	 * Creates a {@link Datalist} element with the specified {@link Datalist#id(String)} attribute and child nodes.
	 *
	 * @param id The {@link Datalist#id(String)} attribute.
	 * @param children The child nodes.
	 */
	public Datalist(String id, Option...children) {
		id(id).children((Object[])children);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Overridden methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* HtmlElement */
	public final Datalist _class(String _class) {
		super._class(_class);
		return this;
	}

	@Override /* HtmlElement */
	public final Datalist id(String id) {
		super.id(id);
		return this;
	}

	@Override /* HtmlElement */
	public final Datalist style(String style) {
		super.style(style);
		return this;
	}

	@Override /* HtmlElementContainer */
	public final Datalist children(Object...children) {
		super.children(children);
		return this;
	}

	@Override /* HtmlElementContainer */
	public final Datalist child(Object child) {
		super.child(child);
		return this;
	}
}
