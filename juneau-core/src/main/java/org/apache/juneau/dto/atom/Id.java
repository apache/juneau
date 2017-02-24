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

import static org.apache.juneau.xml.annotation.XmlFormat.*;

import java.net.URI;

import org.apache.juneau.annotation.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Represents an <code>atomId</code> construct in the RFC4287 specification.
 * <p>
 * <h6 class='figure'>Schema</h6>
 * <p class='bcode'>
 * 	atomId = element atom:id {
 * 		atomCommonAttributes,
 * 		(atomUri)
 * 	}
 * </p>
 * <p>
 * Refer to <a class='doclink' href='package-summary.html#TOC'>org.apache.juneau.dto.atom</a> for further information about ATOM support.
 */
@Bean(typeName="id")
@SuppressWarnings("hiding")
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


	//--------------------------------------------------------------------------------
	// Bean properties
	//--------------------------------------------------------------------------------

	/**
	 * Returns the content of this identifier.
	 *
	 * @return The content of this identifier.
	 */
	@Xml(format=TEXT)
	public String getText() {
		return text;
	}

	/**
	 * Sets the content of this identifier.
	 *
	 * @param text The content of this identifier.
	 * @return This object (for method chaining).
	 */
	@BeanProperty(name="text")
	public Id text(String text) {
		this.text = text;
		return this;
	}


	//--------------------------------------------------------------------------------
	// Overridden setters (to simplify method chaining)
	//--------------------------------------------------------------------------------

	@Override /* Common */
	public Id base(URI base) {
		super.base(base);
		return this;
	}

	@Override /* Common */
	public Id lang(String lang) {
		super.lang(lang);
		return this;
	}
}
