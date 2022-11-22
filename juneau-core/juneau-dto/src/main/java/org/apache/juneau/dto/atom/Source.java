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

import org.apache.juneau.internal.*;

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
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-dto.jd.Atom">Overview &gt; juneau-dto &gt; Atom</a>
 * 	<li class='jp'><a class="doclink" href="package-summary.html#TOC">package-summary.html</a>
 * </ul>
 */
@FluentSetters
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
	 * @return This object
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
	 * @return This object
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
	 * @return This object
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
	 * @return This object
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

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.dto.atom.Common */
	public Source setBase(Object value) {
		super.setBase(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.atom.Common */
	public Source setLang(String value) {
		super.setLang(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.atom.CommonEntry */
	public Source setAuthors(Person...value) {
		super.setAuthors(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.atom.CommonEntry */
	public Source setCategories(Category...value) {
		super.setCategories(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.atom.CommonEntry */
	public Source setContributors(Person...value) {
		super.setContributors(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.atom.CommonEntry */
	public Source setId(String value) {
		super.setId(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.atom.CommonEntry */
	public Source setId(Id value) {
		super.setId(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.atom.CommonEntry */
	public Source setLinks(Link...value) {
		super.setLinks(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.atom.CommonEntry */
	public Source setRights(String value) {
		super.setRights(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.atom.CommonEntry */
	public Source setRights(Text value) {
		super.setRights(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.atom.CommonEntry */
	public Source setTitle(String value) {
		super.setTitle(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.atom.CommonEntry */
	public Source setTitle(Text value) {
		super.setTitle(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.atom.CommonEntry */
	public Source setUpdated(String value) {
		super.setUpdated(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.atom.CommonEntry */
	public Source setUpdated(Calendar value) {
		super.setUpdated(value);
		return this;
	}

	// </FluentSetters>
}
