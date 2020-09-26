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
 * DTO for an HTML {@doc ExtHTML5.text-level-semantics#the-time-element <time>}
 * element.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc DtoHtml5}
 * </ul>
 */
@Bean(typeName="time")
public class Time extends HtmlElementMixed {

	/**
	 * Creates an empty {@link Time} element.
	 */
	public Time() {}

	/**
	 * Creates a {@link Time} element with the specified {@link Time#children(Object[])} nodes.
	 *
	 * @param children The {@link Time#children(Object[])} nodes.
	 */
	public Time(Object...children) {
		children(children);
	}

	/**
	 * {@doc ExtHTML5.text-level-semantics#attr-time-datetime datetime}
	 * attribute.
	 *
	 * <p>
	 * Machine-readable value.
	 *
	 * @param datetime The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Time datetime(String datetime) {
		attr("datetime", datetime);
		return this;
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Overridden methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* HtmlElement */
	public final Time _class(String _class) {
		super._class(_class);
		return this;
	}

	@Override /* HtmlElement */
	public final Time id(String id) {
		super.id(id);
		return this;
	}

	@Override /* HtmlElement */
	public final Time style(String style) {
		super.style(style);
		return this;
	}

	@Override /* HtmlElementMixed */
	public Time children(Object...children) {
		super.children(children);
		return this;
	}

	@Override /* HtmlElementMixed */
	public Time child(Object child) {
		super.child(child);
		return this;
	}
}
