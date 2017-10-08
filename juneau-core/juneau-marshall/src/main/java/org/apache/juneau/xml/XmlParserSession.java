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

import static javax.xml.stream.XMLStreamConstants.*;
import static org.apache.juneau.xml.XmlParserContext.*;
import static org.apache.juneau.xml.annotation.XmlFormat.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.lang.reflect.*;
import java.util.*;

import javax.xml.stream.*;
import javax.xml.stream.util.*;

import org.apache.juneau.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Session object that lives for the duration of a single use of {@link XmlParser}.
 *
 * <p>
 * This class is NOT thread safe.
 * It is typically discarded after one-time use although it can be reused against multiple inputs.
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class XmlParserSession extends ReaderParserSession {

	private static final int UNKNOWN=0, OBJECT=1, ARRAY=2, STRING=3, NUMBER=4, BOOLEAN=5, NULL=6;


	private final boolean
		validating,
		preserveRootElement;
	private final XMLReporter reporter;
	private final XMLResolver resolver;
	private final XMLEventAllocator eventAllocator;
	private final StringBuilder rsb = new StringBuilder();  // Reusable string builder used in this class.

	/**
	 * Create a new session using properties specified in the context.
	 *
	 * @param ctx
	 * 	The context creating this session object.
	 * 	The context contains all the configuration settings for this object.
	 * @param args
	 * 	Runtime session arguments.
	 */
	protected XmlParserSession(XmlParserContext ctx, ParserSessionArgs args) {
		super(ctx, args);
		ObjectMap p = getProperties();
		validating = p.getBoolean(XML_validating, ctx.validating);
		reporter = p.getWithDefault(XML_reporter, ctx.reporter, XMLReporter.class);
		resolver = p.getWithDefault(XML_resolver, ctx.resolver, XMLResolver.class);
		eventAllocator = p.getWithDefault(XML_eventAllocator, ctx.eventAllocator, XMLEventAllocator.class);
		preserveRootElement = p.getBoolean(XML_preserveRootElement, ctx.preserveRootElement);
	}

	@Override /* Session */
	public ObjectMap asMap() {
		return super.asMap()
			.append("XmlParser", new ObjectMap()
				.append("eventAllocator", eventAllocator)
				.append("preserveRootElement", preserveRootElement)
				.append("reporter", reporter)
				.append("resolver", resolver)
				.append("validating", validating)
			);
	}

	/**
	 * Wrap the specified reader in a STAX reader based on settings in this context.
	 *
	 * @param pipe The parser input.
	 * @return The new STAX reader.
	 * @throws Exception If problem occurred trying to create reader.
	 */
	protected final XmlReader getXmlReader(ParserPipe pipe) throws Exception {
		return new XmlReader(pipe, validating, reporter, resolver, eventAllocator);
	}

	/**
	 * Decodes and trims the specified string.
	 *
	 * <p>
	 * Any <js>'_x####_'</js> sequences in the string will be decoded.
	 *
	 * @param s The string to be decoded.
	 * @return The decoded string.
	 */
	protected final String decodeString(String s) {
		if (s == null)
			return null;
		rsb.setLength(0);
		s = XmlUtils.decode(s, rsb);
		if (isTrimStrings())
			s = s.trim();
		return s;
	}

	/*
	 * Returns the name of the current XML element.
	 * Any <js>'_x####_'</js> sequences in the string will be decoded.
	 */
	private String getElementName(XmlReader r) {
		return decodeString(r.getLocalName());
	}

	/*
	 * Returns the name of the specified attribute on the current XML element.
	 * Any <js>'_x####_'</js> sequences in the string will be decoded.
	 */
	private String getAttributeName(XmlReader r, int i) {
		return decodeString(r.getAttributeLocalName(i));
	}

	/*
	 * Returns the value of the specified attribute on the current XML element.
	 * Any <js>'_x####_'</js> sequences in the string will be decoded.
	 */
	private String getAttributeValue(XmlReader r, int i) {
		return decodeString(r.getAttributeValue(i));
	}

	/**
	 * Returns the text content of the current XML element.
	 *
	 * <p>
	 * Any <js>'_x####_'</js> sequences in the string will be decoded.
	 *
	 * <p>
	 * Leading and trailing whitespace (unencoded) will be trimmed from the result.
	 *
	 * @param r The reader to read the element text from.
	 * @return The decoded text.  <jk>null</jk> if the text consists of the sequence <js>'_x0000_'</js>.
	 * @throws Exception
	 */
	protected String getElementText(XmlReader r) throws Exception {
		return decodeString(r.getElementText().trim());
	}

	/*
	 * Returns the content of the current CHARACTERS node.
	 * Any <js>'_x####_'</js> sequences in the string will be decoded.
	 * Leading and trailing whitespace (unencoded) will be trimmed from the result.
	 */
	private String getText(XmlReader r, boolean trim) {
		String s = r.getText();
		if (trim)
			s = s.trim();
		if (s.isEmpty())
			return null;
		return decodeString(s);
	}

	/*
	 * Shortcut for calling <code>getText(r, <jk>true</jk>);</code>.
	 */
	private String getText(XmlReader r) {
		return getText(r, true);
	}

	/*
	 * Takes the element being read from the XML stream reader and reconstructs it as XML.
	 * Used when reconstructing bean properties of type {@link XmlFormat#XMLTEXT}.
	 */
	private String getElementAsString(XmlReader r) {
		int t = r.getEventType();
		if (t > 2)
			throw new FormattedRuntimeException("Invalid event type on stream reader for elementToString() method: ''{0}''", XmlUtils.toReadableEvent(r));
		rsb.setLength(0);
		rsb.append("<").append(t == 1 ? "" : "/").append(r.getLocalName());
		if (t == 1)
			for (int i = 0; i < r.getAttributeCount(); i++)
				rsb.append(' ').append(r.getAttributeName(i)).append('=').append('\'').append(r.getAttributeValue(i)).append('\'');
		rsb.append('>');
		return rsb.toString();
	}

	/**
	 * Parses the current element as text.
	 *
	 * @param r
	 * @return The parsed text.
	 * @throws Exception
	 */
	protected String parseText(XmlReader r) throws Exception {
		// Note that this is different than {@link #getText(XmlReader)} since it assumes that we're pointing to a
		// whitespace element.

		StringBuilder sb2 = getStringBuilder();

		int depth = 0;
		while (true) {
			int et = r.getEventType();
			if (et == START_ELEMENT) {
				sb2.append(getElementAsString(r));
				depth++;
			} else if (et == CHARACTERS) {
				sb2.append(getText(r));
			} else if (et == END_ELEMENT) {
				sb2.append(getElementAsString(r));
				depth--;
				if (depth <= 0)
					break;
			}
			et = r.next();
		}
		String s = sb2.toString();
		returnStringBuilder(sb2);
		return s;
	}

	/**
	 * Returns <jk>true</jk> if the current element is a whitespace element.
	 *
	 * <p>
	 * For the XML parser, this always returns <jk>false</jk>.
	 * However, the HTML parser defines various whitespace elements such as <js>"br"</js> and <js>"sp"</js>.
	 *
	 * @param r The XML stream reader to read the current event from.
	 * @return <jk>true</jk> if the current element is a whitespace element.
	 */
	protected boolean isWhitespaceElement(XmlReader r) {
		return false;
	}

	/**
	 * Parses the current whitespace element.
	 *
	 * <p>
	 * For the XML parser, this always returns <jk>null</jk> since there is no concept of a whitespace element.
	 * However, the HTML parser defines various whitespace elements such as <js>"br"</js> and <js>"sp"</js>.
	 *
	 * @param r The XML stream reader to read the current event from.
	 * @return The whitespace character or characters.
	 * @throws XMLStreamException
	 * @throws Exception
	 */
	protected String parseWhitespaceElement(XmlReader r) throws Exception {
		return null;
	}

	@Override /* ParserSession */
	protected <T> T doParse(ParserPipe pipe, ClassMeta<T> type) throws Exception {
		return parseAnything(type, null, getXmlReader(pipe), getOuter(), true, null);
	}

	@Override /* ReaderParserSession */
	protected <K,V> Map<K,V> doParseIntoMap(ParserPipe pipe, Map<K,V> m, Type keyType, Type valueType) throws Exception {
		ClassMeta cm = getClassMeta(m.getClass(), keyType, valueType);
		return parseIntoMap(pipe, m, cm.getKeyType(), cm.getValueType());
	}

	@Override /* ReaderParserSession */
	protected <E> Collection<E> doParseIntoCollection(ParserPipe pipe, Collection<E> c, Type elementType) throws Exception {
		ClassMeta cm = getClassMeta(c.getClass(), elementType);
		return parseIntoCollection(pipe, c, cm.getElementType());
	}

	/**
	 * Workhorse method.
	 *
	 * @param eType The expected type of object.
	 * @param currAttr The current bean property name.
	 * @param r The reader.
	 * @param outer The outer object.
	 * @param isRoot If <jk>true</jk>, then we're serializing a root element in the document.
	 * @param pMeta The bean property metadata.
	 * @return The parsed object.
	 * @throws Exception
	 */
	protected <T> T parseAnything(ClassMeta<T> eType, String currAttr, XmlReader r,
			Object outer, boolean isRoot, BeanPropertyMeta pMeta) throws Exception {

		if (eType == null)
			eType = (ClassMeta<T>)object();
		PojoSwap<T,Object> swap = (PojoSwap<T,Object>)eType.getPojoSwap(this);
		ClassMeta<?> sType = swap == null ? eType : swap.getSwapClassMeta(this);
		setCurrentClass(sType);

		String wrapperAttr = (isRoot && preserveRootElement) ? r.getName().getLocalPart() : null;
		String typeAttr = r.getAttributeValue(null, getBeanTypePropertyName(eType));
		int jsonType = getJsonType(typeAttr);
		String elementName = getElementName(r);
		if (jsonType == 0) {
			if (elementName == null || elementName.equals(currAttr))
				jsonType = UNKNOWN;
			else {
				typeAttr = elementName;
				jsonType = getJsonType(elementName);
			}
		}

		ClassMeta tcm = getClassMeta(typeAttr, pMeta, eType);
		if (tcm == null && elementName != null && ! elementName.equals(currAttr))
			tcm = getClassMeta(elementName, pMeta, eType);
		if (tcm != null)
			sType = eType = tcm;

		Object o = null;

		if (jsonType == NULL) {
			r.nextTag();	// Discard end tag
			return null;
		}

		if (sType.isObject()) {
			if (jsonType == OBJECT) {
				ObjectMap m = new ObjectMap(this);
				parseIntoMap(r, m, string(), object(), pMeta);
				if (wrapperAttr != null)
					m = new ObjectMap(this).append(wrapperAttr, m);
				o = cast(m, pMeta, eType);
			} else if (jsonType == ARRAY)
				o = parseIntoCollection(r, new ObjectList(this), null, pMeta);
			else if (jsonType == STRING) {
				o = getElementText(r);
				if (sType.isChar())
					o = o.toString().charAt(0);
			}
			else if (jsonType == NUMBER)
				o = parseNumber(getElementText(r), null);
			else if (jsonType == BOOLEAN)
				o = Boolean.parseBoolean(getElementText(r));
			else if (jsonType == UNKNOWN)
				o = getUnknown(r);
		} else if (sType.isBoolean()) {
			o = Boolean.parseBoolean(getElementText(r));
		} else if (sType.isCharSequence()) {
			o = getElementText(r);
		} else if (sType.isChar()) {
			String s = getElementText(r);
			o = s.length() == 0 ? 0 : s.charAt(0);
		} else if (sType.isMap()) {
			Map m = (sType.canCreateNewInstance(outer) ? (Map)sType.newInstance(outer) : new ObjectMap(this));
			o = parseIntoMap(r, m, sType.getKeyType(), sType.getValueType(), pMeta);
			if (wrapperAttr != null)
				o = new ObjectMap(this).append(wrapperAttr, m);
		} else if (sType.isCollection()) {
			Collection l = (sType.canCreateNewInstance(outer) ? (Collection)sType.newInstance(outer) : new ObjectList(this));
			o = parseIntoCollection(r, l, sType, pMeta);
		} else if (sType.isNumber()) {
			o = parseNumber(getElementText(r), (Class<? extends Number>)sType.getInnerClass());
		} else if (sType.canCreateNewBean(outer)) {
			if (sType.getExtendedMeta(XmlClassMeta.class).getFormat() == COLLAPSED) {
				String fieldName = r.getLocalName();
				BeanMap<?> m = newBeanMap(outer, sType.getInnerClass());
				BeanPropertyMeta bpm = m.getMeta().getExtendedMeta(XmlBeanMeta.class).getPropertyMeta(fieldName);
				ClassMeta<?> cm = m.getMeta().getClassMeta();
				Object value = parseAnything(cm, currAttr, r, m.getBean(false), false, null);
				setName(cm, value, currAttr);
				bpm.set(m, currAttr, value);
				o = m.getBean();
			} else {
				BeanMap m = newBeanMap(outer, sType.getInnerClass());
				o = parseIntoBean(r, m).getBean();
			}
		} else if (sType.isArray() || sType.isArgs()) {
			ArrayList l = (ArrayList)parseIntoCollection(r, new ArrayList(), sType, pMeta);
			o = toArray(sType, l);
		} else if (sType.canCreateNewInstanceFromString(outer)) {
			o = sType.newInstanceFromString(outer, getElementText(r));
		} else if (sType.canCreateNewInstanceFromNumber(outer)) {
			o = sType.newInstanceFromNumber(this, outer, parseNumber(getElementText(r), sType.getNewInstanceFromNumberClass()));
		} else {
			throw new ParseException(loc(r),
				"Class ''{0}'' could not be instantiated.  Reason: ''{1}'', property: ''{2}''",
				sType.getInnerClass().getName(), sType.getNotABeanReason(), pMeta == null ? null : pMeta.getName());
		}

		if (swap != null && o != null)
			o = swap.unswap(this, o, eType);

		if (outer != null)
			setParent(eType, o, outer);

		return (T)o;
	}

	private <K,V> Map<K,V> parseIntoMap(XmlReader r, Map<K,V> m, ClassMeta<K> keyType,
			ClassMeta<V> valueType, BeanPropertyMeta pMeta) throws Exception {
		int depth = 0;
		for (int i = 0; i < r.getAttributeCount(); i++) {
			String a = r.getAttributeLocalName(i);
			// TODO - Need better handling of namespaces here.
			if (! (a.equals(getBeanTypePropertyName(null)))) {
				K key = trim(convertAttrToType(m, a, keyType));
				V value = trim(convertAttrToType(m, r.getAttributeValue(i), valueType));
				setName(valueType, value, key);
				m.put(key, value);
			}
		}
		do {
			int event = r.nextTag();
			String currAttr;
			if (event == START_ELEMENT) {
				depth++;
				currAttr = getElementName(r);
				K key = convertAttrToType(m, currAttr, keyType);
				V value = parseAnything(valueType, currAttr, r, m, false, pMeta);
				setName(valueType, value, currAttr);
				if (valueType.isObject() && m.containsKey(key)) {
					Object o = m.get(key);
					if (o instanceof List)
						((List)o).add(value);
					else
						m.put(key, (V)new ObjectList(o, value).setBeanSession(this));
				} else {
					m.put(key, value);
				}
			} else if (event == END_ELEMENT) {
				depth--;
				return m;
			}
		} while (depth > 0);
		return m;
	}

	private <E> Collection<E> parseIntoCollection(XmlReader r, Collection<E> l,
			ClassMeta<?> type, BeanPropertyMeta pMeta) throws Exception {
		int depth = 0;
		int argIndex = 0;
		do {
			int event = r.nextTag();
			if (event == START_ELEMENT) {
				depth++;
				ClassMeta<?> elementType = type == null ? object() : type.isArgs() ? type.getArg(argIndex++) : type.getElementType();
				E value = (E)parseAnything(elementType, null, r, l, false, pMeta);
				l.add(value);
			} else if (event == END_ELEMENT) {
				depth--;
				return l;
			}
		} while (depth > 0);
		return l;
	}

	private static int getJsonType(String s) {
		if (s == null)
			return UNKNOWN;
		char c = s.charAt(0);
		switch(c) {
			case 'o': return (s.equals("object") ? OBJECT : UNKNOWN);
			case 'a': return (s.equals("array") ? ARRAY : UNKNOWN);
			case 's': return (s.equals("string") ? STRING : UNKNOWN);
			case 'b': return (s.equals("boolean") ? BOOLEAN : UNKNOWN);
			case 'n': {
				c = s.charAt(2);
				switch(c) {
					case 'm': return (s.equals("number") ? NUMBER : UNKNOWN);
					case 'l': return (s.equals("null") ? NULL : UNKNOWN);
				}
				//return NUMBER;
			}
		}
		return UNKNOWN;
	}

	private <T> BeanMap<T> parseIntoBean(XmlReader r, BeanMap<T> m) throws Exception {
		BeanMeta<?> bMeta = m.getMeta();
		XmlBeanMeta xmlMeta = bMeta.getExtendedMeta(XmlBeanMeta.class);

		for (int i = 0; i < r.getAttributeCount(); i++) {
			String key = getAttributeName(r, i);
			String val = r.getAttributeValue(i);
			BeanPropertyMeta bpm = xmlMeta.getPropertyMeta(key);
			if (bpm == null) {
				if (xmlMeta.getAttrsProperty() != null) {
					xmlMeta.getAttrsProperty().add(m, key, key, val);
				} else {
					Location l = r.getLocation();
					onUnknownProperty(r.getPipe(), key, m, l.getLineNumber(), l.getColumnNumber());
				}
			} else {
				bpm.set(m, key, val);
			}
		}

		BeanPropertyMeta cp = xmlMeta.getContentProperty();
		XmlFormat cpf = xmlMeta.getContentFormat();
		boolean trim = cp == null || ! cpf.isOneOf(MIXED_PWS, TEXT_PWS);
		ClassMeta<?> cpcm = (cp == null ? object() : cp.getClassMeta());
		StringBuilder sb = null;
		BeanRegistry breg = cp == null ? null : cp.getBeanRegistry();
		LinkedList<Object> l = null;

		int depth = 0;
		do {
			int event = r.next();
			String currAttr;
			// We only care about text in MIXED mode.
			// Ignore if in ELEMENTS mode.
			if (event == CHARACTERS) {
				if (cp != null && cpf.isOneOf(MIXED, MIXED_PWS)) {
					if (cpcm.isCollectionOrArray()) {
						if (l == null)
							l = new LinkedList<Object>();
						l.add(getText(r, false));
					} else {
						cp.set(m, null, getText(r, trim));
					}
				} else if (cpf != ELEMENTS) {
					String s = getText(r, trim);
					if (s != null) {
						if (sb == null)
							sb = getStringBuilder();
						sb.append(s);
					}
				} else {
					// Do nothing...we're in ELEMENTS mode.
				}
			} else if (event == START_ELEMENT) {
				if (cp != null && cpf.isOneOf(TEXT, TEXT_PWS)) {
					String s = parseText(r);
					if (s != null) {
						if (sb == null)
							sb = getStringBuilder();
						sb.append(s);
					}
					depth--;
				} else if (cpf == XMLTEXT) {
					if (sb == null)
						sb = getStringBuilder();
					sb.append(getElementAsString(r));
					depth++;
				} else if (cp != null && cpf.isOneOf(MIXED, MIXED_PWS)) {
					if (isWhitespaceElement(r) && (breg == null || ! breg.hasName(r.getLocalName()))) {
						if (cpcm.isCollectionOrArray()) {
							if (l == null)
								l = new LinkedList<Object>();
							l.add(parseWhitespaceElement(r));
						} else {
							cp.set(m, null, parseWhitespaceElement(r));
						}
					} else {
						if (cpcm.isCollectionOrArray()) {
							if (l == null)
								l = new LinkedList<Object>();
							l.add(parseAnything(cpcm.getElementType(), cp.getName(), r, m.getBean(false), false, cp));
						} else {
							cp.set(m, null, parseAnything(cpcm, cp.getName(), r, m.getBean(false), false, cp));
						}
					}
				} else if (cp != null && cpf == ELEMENTS) {
					cp.add(m, null, parseAnything(cpcm.getElementType(), cp.getName(), r, m.getBean(false), false, cp));
				} else {
					currAttr = getElementName(r);
					BeanPropertyMeta pMeta = xmlMeta.getPropertyMeta(currAttr);
					if (pMeta == null) {
						Location loc = r.getLocation();
						onUnknownProperty(r.getPipe(), currAttr, m, loc.getLineNumber(), loc.getColumnNumber());
						skipCurrentTag(r);
					} else {
						setCurrentProperty(pMeta);
						XmlFormat xf = pMeta.getExtendedMeta(XmlBeanPropertyMeta.class).getXmlFormat();
						if (xf == COLLAPSED) {
							ClassMeta<?> et = pMeta.getClassMeta().getElementType();
							Object value = parseAnything(et, currAttr, r, m.getBean(false), false, pMeta);
							setName(et, value, currAttr);
							pMeta.add(m, currAttr, value);
						} else if (xf == ATTR)  {
							pMeta.set(m, currAttr, getAttributeValue(r, 0));
							r.nextTag();
						} else {
							ClassMeta<?> cm = pMeta.getClassMeta();
							Object value = parseAnything(cm, currAttr, r, m.getBean(false), false, pMeta);
							setName(cm, value, currAttr);
							pMeta.set(m, currAttr, value);
						}
						setCurrentProperty(null);
					}
				}
			} else if (event == END_ELEMENT) {
				if (depth > 0) {
					if (cpf == XMLTEXT) {
						if (sb == null)
							sb = getStringBuilder();
						sb.append(getElementAsString(r));
					}
					else
						throw new ParseException("End element found where one was not expected.  {0}", XmlUtils.toReadableEvent(r));
				}
				depth--;
			} else {
				throw new ParseException("Unexpected event type: {0}", XmlUtils.toReadableEvent(r));
			}
		} while (depth >= 0);

		if (sb != null && cp != null)
			cp.set(m, null, sb.toString());
		else if (l != null && cp != null)
			cp.set(m, null, XmlUtils.collapseTextNodes(l));

		returnStringBuilder(sb);
		return m;
	}

	private static void skipCurrentTag(XmlReader r) throws XMLStreamException {
		int depth = 1;
		do {
			int event = r.next();
			if (event == START_ELEMENT)
				depth++;
			else if (event == END_ELEMENT)
				depth--;
		} while (depth > 0);
	}

	private Object getUnknown(XmlReader r) throws Exception {
		if (r.getEventType() != START_ELEMENT) {
			throw new XmlParseException(r.getLocation(), "Parser must be on START_ELEMENT to read next text.");
		}
		ObjectMap m = null;

		// If this element has attributes, then it's always an ObjectMap.
		if (r.getAttributeCount() > 0) {
			m = new ObjectMap(this);
			for (int i = 0; i < r.getAttributeCount(); i++) {
				String key = getAttributeName(r, i);
				String val = r.getAttributeValue(i);
				if (! key.equals(getBeanTypePropertyName(null)))
					m.put(key, val);
			}
		}
		int eventType = r.next();
		StringBuilder sb = getStringBuilder();
		while (eventType != END_ELEMENT) {
			if (eventType == CHARACTERS || eventType == CDATA || eventType == SPACE || eventType == ENTITY_REFERENCE) {
				sb.append(r.getText());
			} else if (eventType == PROCESSING_INSTRUCTION || eventType == COMMENT) {
				// skipping
			} else if (eventType == END_DOCUMENT) {
				throw new XmlParseException(r.getLocation(), "Unexpected end of document when reading element text content");
			} else if (eventType == START_ELEMENT) {
				// Oops...this has an element in it.
				// Parse it as a map.
				if (m == null)
					m = new ObjectMap(this);
				int depth = 0;
				do {
					int event = (eventType == -1 ? r.nextTag() : eventType);
					String currAttr;
					if (event == START_ELEMENT) {
						depth++;
						currAttr = getElementName(r);
						String key = convertAttrToType(null, currAttr, string());
						Object value = parseAnything(object(), currAttr, r, null, false, null);
						if (m.containsKey(key)) {
							Object o = m.get(key);
							if (o instanceof ObjectList)
								((ObjectList)o).add(value);
							else
								m.put(key, new ObjectList(o, value).setBeanSession(this));
						} else {
							m.put(key, value);
						}

					} else if (event == END_ELEMENT) {
						depth--;
						break;
					}
					eventType = -1;
				} while (depth > 0);
				break;
			} else {
				throw new XmlParseException(r.getLocation(), "Unexpected event type ''{0}''", eventType);
			}
			eventType = r.next();
		}
		String s = sb.toString();
		returnStringBuilder(sb);
		s = decodeString(s);
		if (m != null) {
			if (! s.isEmpty())
				m.put("contents", s);
			return m;
		}
		return s;
	}

	private ObjectMap loc(XmlReader r) {
		return getLastLocation().append("line", r.getLocation().getLineNumber()).append("column", r.getLocation().getColumnNumber());
	}
}
