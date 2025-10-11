/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.bean.atom;

import static org.apache.juneau.xml.annotation.XmlFormat.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Top-level ATOM feed object.
 *
 * <p>
 * Represents an <c>atomFeed</c> construct in the RFC4287 specification.
 *
 * <h5 class='figure'>Schema</h5>
 * <p class='bschema'>
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
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanAtom">juneau-bean-atom</a>
 * </ul>
 */
@Bean(typeName="feed")
@FluentSetters
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
	 * @return This object
	 */
	public Feed setGenerator(Generator value) {
		this.generator = value;
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
	 * @return This object
	 */
	public Feed setIcon(Icon value) {
		this.icon = value;
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
	 * @return This object
	 */
	public Feed setLogo(Logo value) {
		this.logo = value;
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
	 * @return This object
	 */
	public Feed setSubtitle(Text value) {
		this.subtitle = value;
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
	public Feed setSubtitle(String value) {
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
	 * @return This object
	 */
	public Feed setEntries(Entry...value) {
		this.entries = value;
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Overridden setters (to simplify method chaining)
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.bean.atom.Common */
	public Feed setBase(Object value) {
		super.setBase(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.atom.Common */
	public Feed setLang(String value) {
		super.setLang(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.atom.CommonEntry */
	public Feed setAuthors(Person...value) {
		super.setAuthors(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.atom.CommonEntry */
	public Feed setCategories(Category...value) {
		super.setCategories(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.atom.CommonEntry */
	public Feed setContributors(Person...value) {
		super.setContributors(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.atom.CommonEntry */
	public Feed setId(String value) {
		super.setId(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.atom.CommonEntry */
	public Feed setId(Id value) {
		super.setId(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.atom.CommonEntry */
	public Feed setLinks(Link...value) {
		super.setLinks(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.atom.CommonEntry */
	public Feed setRights(String value) {
		super.setRights(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.atom.CommonEntry */
	public Feed setRights(Text value) {
		super.setRights(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.atom.CommonEntry */
	public Feed setTitle(String value) {
		super.setTitle(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.atom.CommonEntry */
	public Feed setTitle(Text value) {
		super.setTitle(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.atom.CommonEntry */
	public Feed setUpdated(String value) {
		super.setUpdated(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.atom.CommonEntry */
	public Feed setUpdated(Calendar value) {
		super.setUpdated(value);
		return this;
	}

	// </FluentSetters>
}