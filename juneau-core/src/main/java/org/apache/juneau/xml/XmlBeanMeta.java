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
package org.apache.juneau.xml;

import static org.apache.juneau.xml.annotation.XmlFormat.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Metadata on beans specific to the XML serializers and parsers pulled from the {@link Xml @Xml} annotation on the class.
 */
public class XmlBeanMeta extends BeanMetaExtended {

	// XML related fields
	private final Map<String,BeanPropertyMeta> attrs;                        // Map of bean properties that are represented as XML attributes.
	private final Map<String,BeanPropertyMeta> elements;                     // Map of bean properties that are represented as XML elements.
	private final BeanPropertyMeta attrsProperty;                            // Bean property that contain XML attribute key/value pairs for this bean.
	private final Map<String,BeanPropertyMeta> collapsedProperties;          // Properties defined with @Xml.childName annotation.
	private BeanPropertyMeta contentProperty;
	private XmlFormat contentFormat;

	/**
	 * Constructor.
	 *
	 * @param beanMeta The metadata on the bean that this metadata applies to.
	 */
	public XmlBeanMeta(BeanMeta<?> beanMeta) {
		super(beanMeta);
		Class<?> c = beanMeta.getClassMeta().getInnerClass();
		Xml xml = c.getAnnotation(Xml.class);
		XmlFormat defaultFormat = null;
		XmlFormat _contentFormat = null;

		if (xml != null) {
			XmlFormat xf = xml.format();
			if (xf == ATTRS)
				defaultFormat = XmlFormat.ATTR;
			else if (xf.isOneOf(ELEMENTS, DEFAULT))
				defaultFormat = ELEMENT;
			else
				throw new BeanRuntimeException(c, "Invalid format specified in @Xml annotation on bean: {0}.  Must be one of the following: DEFAULT,ATTRS,ELEMENTS", xml.format());
		}

		Map<String,BeanPropertyMeta> _attrs = new LinkedHashMap<String,BeanPropertyMeta>();
		Map<String,BeanPropertyMeta> _elements = new LinkedHashMap<String,BeanPropertyMeta>();
		BeanPropertyMeta _attrsProperty = null, _contentProperty = null;
		Map<String,BeanPropertyMeta> _collapsedProperties = new LinkedHashMap<String,BeanPropertyMeta>();

		for (BeanPropertyMeta p : beanMeta.getPropertyMetas()) {
			XmlFormat xf = p.getExtendedMeta(XmlBeanPropertyMeta.class).getXmlFormat();
			ClassMeta<?> pcm = p.getClassMeta();
			if (xf == ATTR) {
				_attrs.put(p.getName(), p);
			} else if (xf == ELEMENT) {
				_elements.put(p.getName(), p);
			} else if (xf == COLLAPSED) {
				_collapsedProperties.put(p.getName(), p);
			} else if (xf == DEFAULT) {
				if (defaultFormat == ATTR)
					_attrs.put(p.getName(), p);
				else
					_elements.put(p.getName(), p);
			} else if (xf == ATTRS) {
				if (_attrsProperty != null)
					throw new BeanRuntimeException(c, "Multiple instances of ATTRS properties defined on class.  Only one property can be designated as such.");
				if (! pcm.isMapOrBean())
					throw new BeanRuntimeException(c, "Invalid type for ATTRS property.  Only properties of type Map and bean can be used.");
				_attrsProperty = p;
			} else if (xf.isOneOf(ELEMENTS, MIXED, TEXT, XMLTEXT)) {
				if (xf.isOneOf(ELEMENTS, MIXED) && ! pcm.isCollectionOrArray())
					throw new BeanRuntimeException(c, "Invalid type for {0} property.  Only properties of type Collection and array can be used.", xf);
				if (_contentProperty != null) {
					if (xf == _contentFormat)
						throw new BeanRuntimeException(c, "Multiple instances of {0} properties defined on class.  Only one property can be designated as such.", xf);
					throw new BeanRuntimeException(c, "{0} and {1} properties found on the same bean.  Only one property can be designated as such.", _contentFormat, xf);
				}
				_contentProperty = p;
				_contentFormat = xf;
			}
			// Look for any properties that are collections with @Xml.childName specified.
			String n = p.getExtendedMeta(XmlBeanPropertyMeta.class).getChildName();
			if (n != null) {
				if (_collapsedProperties.containsKey(n) && _collapsedProperties.get(n) != p)
					throw new BeanRuntimeException(c, "Multiple properties found with the child name ''{0}''.", n);
				_collapsedProperties.put(n, p);
			}
		}

		attrs = Collections.unmodifiableMap(_attrs);
		elements = Collections.unmodifiableMap(_elements);
		attrsProperty = _attrsProperty;
		collapsedProperties = Collections.unmodifiableMap(_collapsedProperties);
		contentProperty = _contentProperty;
		contentFormat = _contentFormat;

		// Do some validation.
		if (contentProperty != null) {
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
	protected Map<String,BeanPropertyMeta> getAttrProperties() {
		return attrs;
	}

	/**
	 * The list of names of properties that should be rendered as XML attributes.
	 *
	 * @return Set of property names.
	 */
	protected Set<String> getAttrPropertyNames() {
		return attrs.keySet();
	}

	/**
	 * The list of properties that should be rendered as child elements.
	 *
	 * @return Map of property names to property metadata.
	 */
	protected Map<String,BeanPropertyMeta> getElementProperties() {
		return elements;
	}

	/**
	 * The list of names of properties that should be rendered as child elements.
	 *
	 * @return Set of property names.
	 */
	protected Set<String> getElementPropertyNames() {
		return elements.keySet();
	}

	/**
	 * The list of properties that should be rendered as collapsed child elements.
	 * See {@link Xml#childName()}
	 *
	 * @return Map of property names to property metadata.
	 */
	protected Map<String,BeanPropertyMeta> getCollapsedProperties() {
		return collapsedProperties;
	}

	/**
	 * The list of names of properties that should be rendered as collapsed child elements.
	 *
	 * @return Set of property names.
	 */
	protected Set<String> getCollapsedPropertyNames() {
		return collapsedProperties.keySet();
	}

	/**
	 * The property that returns a map of XML attributes as key/value pairs.
	 *
	 * @return The bean property metadata, or <jk>null</jk> if there is no such method.
	 */
	protected BeanPropertyMeta getAttrsProperty() {
		return attrsProperty;
	}

	/**
	 * The name of the property that returns a map of XML attributes as key/value pairs.
	 *
	 * @return The bean property name, or <jk>null</jk> if there is no such method.
	 */
	protected String getAttrsPropertyName() {
		return attrsProperty == null ? null : attrsProperty.getName();
	}

	/**
	 * The property that represents the inner XML content of this bean.
	 *
	 * @return The bean property metadata, or <jk>null</jk> if there is no such method.
	 */
	protected BeanPropertyMeta getContentProperty() {
		return contentProperty;
	}

	/**
	 * The name of the property that represents the inner XML content of this bean.
	 *
	 * @return The bean property name, or <jk>null</jk> if there is no such method.
	 */
	protected String getContentPropertyName() {
		return contentProperty == null ? null : contentProperty.getName();
	}

	/**
	 * Returns the format of the inner XML content of this bean.
	 * <p>
	 * Can be one of the following:
	 * <ul>
	 * 	<li>{@link XmlFormat#ELEMENTS}
	 * 	<li>{@link XmlFormat#MIXED}
	 * 	<li>{@link XmlFormat#TEXT}
	 * 	<li>{@link XmlFormat#XMLTEXT}
	 * 	<li><jk>null</jk>
	 *
	 * @return The format of the inner XML content of this bean.
	 */
	protected XmlFormat getContentFormat() {
		return contentFormat;
	}

	/**
	 * Returns bean property meta with the specified name.
	 * This is identical to calling {@link BeanMeta#getPropertyMeta(String)} except it first retrieves
	 * 	the bean property meta based on the child name (e.g. a property whose name is "people", but whose child name is "person").
	 *
	 * @param fieldName The bean property name.
	 * @return The property metadata.
	 */
	protected BeanPropertyMeta getPropertyMeta(String fieldName) {
		if (collapsedProperties != null) {
			BeanPropertyMeta bpm = collapsedProperties.get(fieldName);
			if (bpm != null)
				return bpm;
		}
		return getBeanMeta().getPropertyMeta(fieldName);
	}
}
