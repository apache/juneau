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
 * DTO for an HTML {@doc ext.HTML5.tabular-data#the-th-element <th>}
 * element.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc jd.Html5}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
@Bean(typeName="th")
public class Th extends HtmlElementMixed {

	/**
	 * Creates an empty {@link Th} element.
	 */
	public Th() {}

	/**
	 * Creates a {@link Th} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 */
	public Th(Object...children) {
		children(children);
	}

	/**
	 * {@doc ext.HTML5.tabular-data#attr-th-abbr abbr} attribute.
	 *
	 * <p>
	 * Alternative label to use for the header cell when referencing the cell in other contexts.
	 *
	 * @param abbr The new value for this attribute.
	 * @return This object.
	 */
	public final Th abbr(String abbr) {
		attr("abbr", abbr);
		return this;
	}

	/**
	 * {@doc ext.HTML5.tabular-data#attr-tdth-colspan colspan} attribute.
	 *
	 * <p>
	 * Number of columns that the cell is to span.
	 *
	 * @param colspan
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object.
	 */
	public final Th colspan(Object colspan) {
		attr("colspan", colspan);
		return this;
	}

	/**
	 * {@doc ext.HTML5.tabular-data#attr-tdth-headers headers} attribute.
	 *
	 * <p>
	 * The headers for this cell.
	 *
	 * @param headers The new value for this attribute.
	 * @return This object.
	 */
	public final Th headers(String headers) {
		attr("headers", headers);
		return this;
	}

	/**
	 * {@doc ext.HTML5.tabular-data#attr-tdth-rowspan rowspan} attribute.
	 *
	 * <p>
	 * Number of rows that the cell is to span.
	 *
	 * @param rowspan
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object.
	 */
	public final Th rowspan(Object rowspan) {
		attr("rowspan", rowspan);
		return this;
	}

	/**
	 * {@doc ext.HTML5.tabular-data#attr-th-scope scope} attribute.
	 *
	 * <p>
	 * Specifies which cells the header cell applies to.
	 *
	 * @param scope The new value for this attribute.
	 * @return This object.
	 */
	public final Th scope(String scope) {
		attr("scope", scope);
		return this;
	}

	/**
	 * {@doc ext.HTML5.tabular-data#attr-th-sorted sorted}  attribute.
	 *
	 * <p>
	 * Column sort direction and ordinality.
	 *
	 * @param sorted The new value for this attribute.
	 * @return This object.
	 */
	public final Th sorted(String sorted) {
		attr("sorted", sorted);
		return this;
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Overridden methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* HtmlElement */
	public final Th _class(String _class) {
		super._class(_class);
		return this;
	}

	@Override /* HtmlElement */
	public final Th id(String id) {
		super.id(id);
		return this;
	}

	@Override /* HtmlElement */
	public final Th style(String style) {
		super.style(style);
		return this;
	}

	@Override /* HtmlElementMixed */
	public Th children(Object...children) {
		super.children(children);
		return this;
	}

	@Override /* HtmlElementMixed */
	public Th child(Object child) {
		super.child(child);
		return this;
	}

	// <FluentSetters>

	// </FluentSetters>
}
