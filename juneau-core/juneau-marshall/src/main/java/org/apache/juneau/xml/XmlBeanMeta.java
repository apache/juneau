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
package org.apache.juneau.xml;

import static org.apache.juneau.common.utils.Utils.*;
import static org.apache.juneau.internal.CollectionUtils.map;
import static org.apache.juneau.xml.annotation.XmlFormat.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Metadata on beans specific to the XML serializers and parsers pulled from the {@link Xml @Xml} annotation on the
 * class.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/XmlBasics">XML Basics</a>
 * </ul>
 */
public class XmlBeanMeta extends ExtendedBeanMeta {

	private static class XmlBeanMetaBuilder {
		Map<String,BeanPropertyMeta> attrs = map(), elements = map(), collapsedProperties = map();
		BeanPropertyMeta attrsProperty, contentProperty;
		XmlFormat contentFormat = DEFAULT;

		XmlBeanMetaBuilder(BeanMeta<?> beanMeta, XmlMetaProvider mp) {
			Class<?> c = beanMeta.getClassMeta().getInnerClass();
			Value<XmlFormat> defaultFormat = Value.empty();

			mp.forEachAnnotation(Xml.class, c, x -> true, x -> {
				XmlFormat xf = x.format();
				if (xf == ATTRS)
					defaultFormat.set(XmlFormat.ATTR);
				else if (xf.isOneOf(ELEMENTS, DEFAULT))
					defaultFormat.set(ELEMENT);
				else if (xf == VOID) {
					contentFormat = VOID;
					defaultFormat.set(VOID);
				} else
					throw new BeanRuntimeException(c, "Invalid format specified in @Xml annotation on bean: {0}.  Must be one of the following: DEFAULT,ATTRS,ELEMENTS,VOID", x.format());
			});

			beanMeta.forEachProperty(null, p -> {
				XmlFormat xf = mp.getXmlBeanPropertyMeta(p).getXmlFormat();
				ClassMeta<?> pcm = p.getClassMeta();
				if (xf == ATTR) {
					attrs.put(p.getName(), p);
				} else if (xf == ELEMENT) {
					elements.put(p.getName(), p);
				} else if (xf == COLLAPSED) {
					collapsedProperties.put(p.getName(), p);
				} else if (xf == DEFAULT) {
					if (defaultFormat.get() == ATTR)
						attrs.put(p.getName(), p);
					else
						elements.put(p.getName(), p);
				} else if (xf == ATTRS) {
					if (attrsProperty != null)
						throw new BeanRuntimeException(c, "Multiple instances of ATTRS properties defined on class.  Only one property can be designated as such.");
					if (! pcm.isMapOrBean())
						throw new BeanRuntimeException(c, "Invalid type for ATTRS property.  Only properties of type Map and bean can be used.");
					attrsProperty = p;
				} else if (xf.isOneOf(ELEMENTS, MIXED, MIXED_PWS, TEXT, TEXT_PWS, XMLTEXT)) {
					if (xf.isOneOf(ELEMENTS, MIXED, MIXED_PWS) && ! pcm.isCollectionOrArray())
						throw new BeanRuntimeException(c, "Invalid type for {0} property.  Only properties of type Collection and array can be used.", xf);
					if (contentProperty != null) {
						if (xf == contentFormat)
							throw new BeanRuntimeException(c, "Multiple instances of {0} properties defined on class.  Only one property can be designated as such.", xf);
						throw new BeanRuntimeException(c, "{0} and {1} properties found on the same bean.  Only one property can be designated as such.", contentFormat, xf);
					}
					contentProperty = p;
					contentFormat = xf;
				}
				// Look for any properties that are collections with @Xml.childName specified.
				String n = mp.getXmlBeanPropertyMeta(p).getChildName();
				if (n != null) {
					if (collapsedProperties.containsKey(n) && collapsedProperties.get(n) != p)
						throw new BeanRuntimeException(c, "Multiple properties found with the child name ''{0}''.", n);
					collapsedProperties.put(n, p);
				}
			});
		}
	}

	// XML related fields
	private final Map<String,BeanPropertyMeta> attrs;                        // Map of bean properties that are represented as XML attributes.
	private final Map<String,BeanPropertyMeta> elements;                     // Map of bean properties that are represented as XML elements.
	private final BeanPropertyMeta attrsProperty;                            // Bean property that contain XML attribute key/value pairs for this bean.
	private final Map<String,BeanPropertyMeta> collapsedProperties;          // Properties defined with @Xml.childName annotation.
	private final BeanPropertyMeta contentProperty;

	private final XmlFormat contentFormat;

	/**
	 * Constructor.
	 *
	 * @param beanMeta The metadata on the bean that this metadata applies to.
	 * @param mp XML metadata provider (for finding information about other artifacts).
	 */
	public XmlBeanMeta(BeanMeta<?> beanMeta, XmlMetaProvider mp) {
		super(beanMeta);

		Class<?> c = beanMeta.getClassMeta().getInnerClass();
		XmlBeanMetaBuilder b = new XmlBeanMetaBuilder(beanMeta, mp);

		attrs = u(b.attrs);
		elements = u(b.elements);
		attrsProperty = b.attrsProperty;
		collapsedProperties = u(b.collapsedProperties);
		contentProperty = b.contentProperty;
		contentFormat = b.contentFormat;

		// Do some validation.
		if (contentProperty != null || contentFormat == XmlFormat.VOID) {
			if (! elements.isEmpty())
				throw new BeanRuntimeException(c, "{0} and ELEMENT properties found on the same bean.  These cannot be mixed.", contentFormat);
			if (! collapsedProperties.isEmpty())
				throw new BeanRuntimeException(c, "{0} and COLLAPSED properties found on the same bean.  These cannot be mixed.", contentFormat);
		}

		if (! collapsedProperties.isEmpty()) {
			if (! Collections.disjoint(elements.keySet(), collapsedProperties.keySet()))
				throw new BeanRuntimeException(c, "Child element name conflicts found with another property.");
		}
	}

	/**
	 * The list of properties that should be rendered as XML attributes.
	 *
	 * @return Map of property names to property metadata.
	 */
	public Map<String,BeanPropertyMeta> getAttrProperties() { return attrs; }

	/**
	 * Returns the format of the inner XML content of this bean.
	 *
	 * <p>
	 * Can be one of the following:
	 * <ul>
	 * 	<li>{@link XmlFormat#ELEMENTS}
	 * 	<li>{@link XmlFormat#MIXED}
	 * 	<li>{@link XmlFormat#MIXED_PWS}
	 * 	<li>{@link XmlFormat#TEXT}
	 * 	<li>{@link XmlFormat#TEXT_PWS}
	 * 	<li>{@link XmlFormat#XMLTEXT}
	 * 	<li>{@link XmlFormat#VOID}
	 * 	<li><jk>null</jk>
	 * </ul>
	 *
	 * @return The format of the inner XML content of this bean.
	 */
	public XmlFormat getContentFormat() { return contentFormat; }

	/**
	 * The property that represents the inner XML content of this bean.
	 *
	 * @return The bean property metadata, or <jk>null</jk> if there is no such method.
	 */
	public BeanPropertyMeta getContentProperty() { return contentProperty; }

	/**
	 * The list of names of properties that should be rendered as XML attributes.
	 *
	 * @return Set of property names.
	 */
	protected Set<String> getAttrPropertyNames() { return attrs.keySet(); }

	/**
	 * The property that returns a map of XML attributes as key/value pairs.
	 *
	 * @return The bean property metadata, or <jk>null</jk> if there is no such method.
	 */
	protected BeanPropertyMeta getAttrsProperty() { return attrsProperty; }

	/**
	 * The name of the property that returns a map of XML attributes as key/value pairs.
	 *
	 * @return The bean property name, or <jk>null</jk> if there is no such method.
	 */
	protected String getAttrsPropertyName() { return attrsProperty == null ? null : attrsProperty.getName(); }

	/**
	 * The list of properties that should be rendered as collapsed child elements.
	 * <br>See {@link Xml#childName() @Xml(childName)}
	 *
	 * @return Map of property names to property metadata.
	 */
	protected Map<String,BeanPropertyMeta> getCollapsedProperties() { return collapsedProperties; }

	/**
	 * The list of names of properties that should be rendered as collapsed child elements.
	 *
	 * @return Set of property names.
	 */
	protected Set<String> getCollapsedPropertyNames() { return collapsedProperties.keySet(); }

	/**
	 * The name of the property that represents the inner XML content of this bean.
	 *
	 * @return The bean property name, or <jk>null</jk> if there is no such method.
	 */
	protected String getContentPropertyName() { return contentProperty == null ? null : contentProperty.getName(); }

	/**
	 * The list of properties that should be rendered as child elements.
	 *
	 * @return Map of property names to property metadata.
	 */
	protected Map<String,BeanPropertyMeta> getElementProperties() { return elements; }

	/**
	 * The list of names of properties that should be rendered as child elements.
	 *
	 * @return Set of property names.
	 */
	protected Set<String> getElementPropertyNames() { return elements.keySet(); }

	/**
	 * Returns bean property meta with the specified name.
	 *
	 * <p>
	 * This is identical to calling {@link BeanMeta#getPropertyMeta(String)} except it first retrieves the bean property
	 * meta based on the child name (e.g. a property whose name is "people", but whose child name is "person").
	 *
	 * @param fieldName The bean property name.
	 * @return The property metadata.
	 */
	protected BeanPropertyMeta getPropertyMeta(String fieldName) {
		if (collapsedProperties != null) {
			BeanPropertyMeta bpm = collapsedProperties.get(fieldName);
			if (bpm == null)
				bpm = collapsedProperties.get("*");
			if (bpm != null)
				return bpm;
		}
		return getBeanMeta().getPropertyMeta(fieldName);
	}
}