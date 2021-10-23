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

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Top-level ATOM feed object.
 *
 * <p>
 * Represents an <c>atomFeed</c> construct in the RFC4287 specification.
 *
 * <h5 class='figure'>Schema</h5>
 * <p class='bcode w800'>
 * 	atomFeed =
 * 		element atom:feed {
 * 			atomCommonAttributes,
 * 			(atomAuthor*
 * 			 &amp; atomCategory*
 * 			 &amp; atomContributor*
 * 			 &amp; atomGenerator?
 * 			 &amp; atomIcon?
 * 			 &amp; atomId
 * 			 &amp; atomLink*
 * 			 &amp; atomLogo?
 * 			 &amp; atomRights?
 * 			 &amp; atomSubtitle?
 * 			 &amp; atomTitle
 * 			 &amp; atomUpdated
 * 			 &amp; extensionElement*),
 * 			atomEntry*
 * 		}
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc DtoAtom}
 * 	<li class='jp'>{@doc package-summary.html#TOC}
 * </ul>
 */
@Bean(typeName="feed")
public class Feed extends CommonEntry {

	private Generator generator;  // atomGenerator?
	private Icon icon;            // atomIcon?
	private Logo logo;            // atomLogo?
	private Text subtitle;        // atomSubtitle?
	private Entry[] entries;      // atomEntry*

	/**
	 * Normal constructor.
	 *
	 * @param id The feed identifier.
	 * @param title The feed title.
	 * @param updated The feed updated timestamp.
	 */
	public Feed(Id id, Text title, Calendar updated) {
		super(id, title, updated);
	}

	/**
	 * Normal constructor.
	 *
	 * @param id The feed identifier.
	 * @param title The feed title.
	 * @param updated The feed updated timestamp.
	 */
	public Feed(String id, String title, String updated) {
		super(id, title, updated);
	}

	/** Bean constructor. */
	public Feed() {}


	//-----------------------------------------------------------------------------------------------------------------
	// Bean properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Bean property getter:  <property>generator</property>.
	 *
	 * <p>
	 * The generator information on this feed.
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
	 * The generator information on this feed.
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
	 * The generator information on this feed.
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
	 * The generator information on this feed.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Feed generator(Generator value) {
		setGenerator(value);
		return this;
	}

	/**
	 * Bean property getter:  <property>icon</property>.
	 *
	 * <p>
	 * The feed icon.
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
	 * The feed icon.
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
	 * The feed icon.
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
	 * The feed icon.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Feed icon(Icon value) {
		setIcon(value);
		return this;
	}

	/**
	 * Bean property getter:  <property>logo</property>.
	 *
	 * <p>
	 * The feed logo.
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
	 * The feed logo.
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
	 * The feed logo.
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
	 * The feed logo.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Feed logo(Logo value) {
		setLogo(value);
		return this;
	}

	/**
	 * Bean property getter:  <property>subtitle</property>.
	 *
	 * <p>
	 * The feed subtitle.
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
	 * The feed subtitle.
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
	 * The feed subtitle.
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
	 * The feed subtitle.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Feed subtitle(Text value) {
		setSubtitle(value);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>subtitle</property>.
	 *
	 * <p>
	 * The feed subtitle.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Feed subtitle(String value) {
		setSubtitle(new Text(value));
		return this;
	}

	/**
	 * Bean property getter:  <property>entries</property>.
	 *
	 * <p>
	 * The entries in the feed.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Xml(format=COLLAPSED)
	public Entry[] getEntries() {
		return entries;
	}

	/**
	 * Bean property setter:  <property>entries</property>.
	 *
	 * <p>
	 * The entries in the feed.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 */
	public void setEntries(Entry[] value) {
		this.entries = value;
	}

	/**
	 * Bean property fluent getter:  <property>entries</property>.
	 *
	 * <p>
	 * The entries in the feed.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Entry[]> entries() {
		return Optional.ofNullable(entries);
	}

	/**
	 * Bean property fluent setter:  <property>entries</property>.
	 *
	 * <p>
	 * The entries in the feed.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Feed entries(Entry...value) {
		setEntries(value);
		return this;
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Overridden setters (to simplify method chaining)
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* CommonEntry */
	public Feed authors(Person...authors) {
		super.authors(authors);
		return this;
	}

	@Override /* CommonEntry */
	public Feed categories(Category...categories) {
		super.categories(categories);
		return this;
	}

	@Override /* CommonEntry */
	public Feed contributors(Person...contributors) {
		super.contributors(contributors);
		return this;
	}

	@Override /* CommonEntry */
	public Feed id(Id id) {
		super.id(id);
		return this;
	}

	@Override /* CommonEntry */
	public Feed links(Link...links) {
		super.links(links);
		return this;
	}

	@Override /* CommonEntry */
	public Feed rights(Text rights) {
		super.rights(rights);
		return this;
	}

	@Override /* CommonEntry */
	public Feed rights(String rights) {
		super.rights(rights);
		return this;
	}

	@Override /* CommonEntry */
	public Feed title(Text title) {
		super.title(title);
		return this;
	}

	@Override /* CommonEntry */
	public Feed title(String title) {
		super.title(title);
		return this;
	}

	@Override /* CommonEntry */
	public Feed updated(Calendar updated) {
		super.updated(updated);
		return this;
	}

	@Override /* CommonEntry */
	public Feed updated(String updated) {
		super.updated(updated);
		return this;
	}

	@Override /* Common */
	public Feed base(Object base) {
		super.base(base);
		return this;
	}

	@Override /* Common */
	public Feed lang(String lang) {
		super.lang(lang);
		return this;
	}
}
