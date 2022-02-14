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
package org.apache.juneau.dto.atom;

import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.xml.annotation.XmlFormat.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Represents an <c>atomId</c> construct in the RFC4287 specification.
 *
 * <h5 class='figure'>Schema</h5>
 * <p class='bschema'>
 * 	atomId = element atom:id {
 * 		atomCommonAttributes,
 * 		(atomUri)
 * 	}
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc jd.Atom}
 * 	<li class='jp'>{@doc package-summary.html#TOC}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
@Bean(typeName="id")
public class Id extends Common {

	private String text;

	/**
	 * Normal constructor.
	 *
	 * @param text The id element contents.
	 */
	public Id(String text) {
		text(text);
	}

	/** Bean constructor. */
	public Id() {}


	//-----------------------------------------------------------------------------------------------------------------
	// Bean properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Bean property getter:  <property>text</property>.
	 *
	 * <p>
	 * The content of this identifier.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Xml(format=TEXT)
	public String getText() {
		return text;
	}

	/**
	 * Bean property setter:  <property>text</property>.
	 *
	 * <p>
	 * The content of this identifier.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 */
	public void setText(String value) {
		this.text = value;
	}

	/**
	 * Bean property fluent getter:  <property>text</property>.
	 *
	 * <p>
	 * The content of this identifier.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<String> text() {
		return optional(text);
	}

	/**
	 * Bean property fluent setter:  <property>text</property>.
	 *
	 * <p>
	 * The content of this identifier.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Id text(String value) {
		setText(value);
		return this;
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Overridden setters (to simplify method chaining)
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Common */
	public Id base(Object base) {
		super.base(base);
		return this;
	}

	@Override /* Common */
	public Id lang(String lang) {
		super.lang(lang);
		return this;
	}
}
