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

import java.util.*;

/**
 * Represents an <c>atomSource</c> construct in the RFC4287 specification.
 *
 * <h5 class='figure'>Schema</h5>
 * <p class='bschema'>
 * 	atomSource =
 * 		element atom:source {
 * 			atomCommonAttributes,
 * 			(atomAuthor*
 * 			&amp; atomCategory*
 * 			&amp; atomContributor*
 * 			&amp; atomGenerator?
 * 			&amp; atomIcon?
 * 			&amp; atomId?
 * 			&amp; atomLink*
 * 			&amp; atomLogo?
 * 			&amp; atomRights?
 * 			&amp; atomSubtitle?
 * 			&amp; atomTitle?
 * 			&amp; atomUpdated?
 * 			&amp; extensionElement*)
 * 		}
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc jd.Atom}
 * 	<li class='jp'>{@doc package-summary.html#TOC}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public class Source extends CommonEntry {

	private Generator generator;
	private Icon icon;
	private Logo logo;
	private Text subtitle;


	//-----------------------------------------------------------------------------------------------------------------
	// Bean properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Bean property getter:  <property>generator</property>.
	 *
	 * <p>
	 * The generator info of this source.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Generator getGenerator() {
		return generator;
	}

	/**
	 * Bean property setter:  <property>generator</property>.
	 *
	 * <p>
	 * The generator info of this source.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Source setGenerator(Generator value) {
		this.generator = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>icon</property>.
	 *
	 * <p>
	 * The icon of this source.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Icon getIcon() {
		return icon;
	}

	/**
	 * Bean property setter:  <property>icon</property>.
	 *
	 * <p>
	 * The icon of this source.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Source setIcon(Icon value) {
		this.icon = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>logo</property>.
	 *
	 * <p>
	 * The logo of this source.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Logo getLogo() {
		return logo;
	}

	/**
	 * Bean property setter:  <property>logo</property>.
	 *
	 * <p>
	 * The logo of this source.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Source setLogo(Logo value) {
		this.logo = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>subtitle</property>.
	 *
	 * <p>
	 * The subtitle of this source.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Text getSubtitle() {
		return subtitle;
	}

	/**
	 * Bean property setter:  <property>subtitle</property>.
	 *
	 * <p>
	 * The subtitle of this source.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Source setSubtitle(Text value) {
		this.subtitle = value;
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>subtitle</property>.
	 *
	 * <p>
	 * The subtitle of this source.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Source setSubtitle(String value) {
		setSubtitle(new Text(value));
		return this;
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Overridden setters (to simplify method chaining)
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* CommonEntry */
	public Source setAuthors(Person...authors) {
		super.setAuthors(authors);
		return this;
	}

	@Override /* CommonEntry */
	public Source setCategories(Category...categories) {
		super.setCategories(categories);
		return this;
	}

	@Override /* CommonEntry */
	public Source setContributors(Person...contributors) {
		super.setContributors(contributors);
		return this;
	}

	@Override /* CommonEntry */
	public Source setId(Id id) {
		super.setId(id);
		return this;
	}

	@Override /* CommonEntry */
	public Source setLinks(Link...links) {
		super.setLinks(links);
		return this;
	}

	@Override /* CommonEntry */
	public Source setRights(Text rights) {
		super.setRights(rights);
		return this;
	}

	@Override /* CommonEntry */
	public Source setRights(String rights) {
		super.setRights(rights);
		return this;
	}

	@Override /* CommonEntry */
	public Source setTitle(Text title) {
		super.setTitle(title);
		return this;
	}

	@Override /* CommonEntry */
	public Source setTitle(String title) {
		super.setTitle(title);
		return this;
	}

	@Override /* CommonEntry */
	public Source setUpdated(Calendar updated) {
		super.setUpdated(updated);
		return this;
	}

	@Override /* CommonEntry */
	public Source setUpdated(String updated) {
		super.setUpdated(updated);
		return this;
	}

	@Override /* Common */
	public Source setBase(Object base) {
		super.setBase(base);
		return this;
	}

	@Override /* Common */
	public Source setLang(String lang) {
		super.setLang(lang);
		return this;
	}
}
