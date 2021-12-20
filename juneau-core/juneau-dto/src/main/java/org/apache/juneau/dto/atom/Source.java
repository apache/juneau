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
 * <p class='bcode w800'>
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
	 */
	public void setGenerator(Generator value) {
		this.generator = value;
	}

	/**
	 * Bean property fluent getter:  <property>generator</property>.
	 *
	 * <p>
	 * The generator info of this source.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Generator> generator() {
		return Optional.ofNullable(generator);
	}

	/**
	 * Bean property fluent setter:  <property>generator</property>.
	 *
	 * <p>
	 * The generator info of this source.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Source generator(Generator value) {
		setGenerator(value);
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
	 */
	public void setIcon(Icon value) {
		this.icon = value;
	}

	/**
	 * Bean property fluent getter:  <property>icon</property>.
	 *
	 * <p>
	 * The icon of this source.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Icon> icon() {
		return Optional.ofNullable(icon);
	}

	/**
	 * Bean property fluent setter:  <property>icon</property>.
	 *
	 * <p>
	 * The icon of this source.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Source icon(Icon value) {
		setIcon(value);
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
	 */
	public void setLogo(Logo value) {
		this.logo = value;
	}

	/**
	 * Bean property fluent getter:  <property>logo</property>.
	 *
	 * <p>
	 * The logo of this source.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Logo> logo() {
		return Optional.ofNullable(logo);
	}

	/**
	 * Bean property fluent setter:  <property>logo</property>.
	 *
	 * <p>
	 * The logo of this source.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Source logo(Logo value) {
		setLogo(value);
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
	 */
	public void setSubtitle(Text value) {
		this.subtitle = value;
	}

	/**
	 * Bean property fluent getter:  <property>subtitle</property>.
	 *
	 * <p>
	 * The subtitle of this source.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Text> subtitle() {
		return Optional.ofNullable(subtitle);
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
	public Source subtitle(Text value) {
		setSubtitle(value);
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
	public Source subtitle(String value) {
		setSubtitle(new Text(value));
		return this;
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Overridden setters (to simplify method chaining)
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* CommonEntry */
	public Source authors(Person...authors) {
		super.authors(authors);
		return this;
	}

	@Override /* CommonEntry */
	public Source categories(Category...categories) {
		super.categories(categories);
		return this;
	}

	@Override /* CommonEntry */
	public Source contributors(Person...contributors) {
		super.contributors(contributors);
		return this;
	}

	@Override /* CommonEntry */
	public Source id(Id id) {
		super.id(id);
		return this;
	}

	@Override /* CommonEntry */
	public Source links(Link...links) {
		super.links(links);
		return this;
	}

	@Override /* CommonEntry */
	public Source rights(Text rights) {
		super.rights(rights);
		return this;
	}

	@Override /* CommonEntry */
	public Source rights(String rights) {
		super.rights(rights);
		return this;
	}

	@Override /* CommonEntry */
	public Source title(Text title) {
		super.title(title);
		return this;
	}

	@Override /* CommonEntry */
	public Source title(String title) {
		super.title(title);
		return this;
	}

	@Override /* CommonEntry */
	public Source updated(Calendar updated) {
		super.updated(updated);
		return this;
	}

	@Override /* CommonEntry */
	public Source updated(String updated) {
		super.updated(updated);
		return this;
	}

	@Override /* Common */
	public Source base(Object base) {
		super.base(base);
		return this;
	}

	@Override /* Common */
	public Source lang(String lang) {
		super.lang(lang);
		return this;
	}
}
