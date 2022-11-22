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
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.xml.annotation.XmlFormat.*;

import java.io.IOException;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;
import java.util.function.*;

import javax.xml.stream.*;
import javax.xml.stream.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.swap.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Session object that lives for the duration of a single use of {@link XmlParser}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.XmlDetails">XML Details</a>
 * </ul>
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class XmlParserSession extends ReaderParserSession {

	//-------------------------------------------------------------------------------------------------------------------
	// Static
	//-------------------------------------------------------------------------------------------------------------------

	private static final int UNKNOWN=0, OBJECT=1, ARRAY=2, STRING=3, NUMBER=4, BOOLEAN=5, NULL=6;

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * @return A new builder.
	 */
	public static Builder create(XmlParser ctx) {
		return new Builder(ctx);
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Builder
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public static class Builder extends ReaderParserSession.Builder {

		XmlParser ctx;

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 */
		protected Builder(XmlParser ctx) {
			super(ctx);
			this.ctx = ctx;
		}

		@Override
		public XmlParserSession build() {
			return new XmlParserSession(this);
		}

		// <FluentSetters>

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public <T> Builder apply(Class<T> type, Consumer<T> apply) {
			super.apply(type, apply);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public Builder debug(Boolean value) {
			super.debug(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public Builder properties(Map<String,Object> value) {
			super.properties(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public Builder property(String key, Object value) {
			super.property(key, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public Builder unmodifiable() {
			super.unmodifiable();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder locale(Locale value) {
			super.locale(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder localeDefault(Locale value) {
			super.localeDefault(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder mediaType(MediaType value) {
			super.mediaType(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder mediaTypeDefault(MediaType value) {
			super.mediaTypeDefault(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder timeZone(TimeZone value) {
			super.timeZone(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder timeZoneDefault(TimeZone value) {
			super.timeZoneDefault(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.ParserSession.Builder */
		public Builder javaMethod(Method value) {
			super.javaMethod(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.ParserSession.Builder */
		public Builder outer(Object value) {
			super.outer(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.ParserSession.Builder */
		public Builder schema(HttpPartSchema value) {
			super.schema(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.ParserSession.Builder */
		public Builder schemaDefault(HttpPartSchema value) {
			super.schemaDefault(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.ReaderParserSession.Builder */
		public Builder fileCharset(Charset value) {
			super.fileCharset(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.ReaderParserSession.Builder */
		public Builder streamCharset(Charset value) {
			super.streamCharset(value);
			return this;
		}

		// </FluentSetters>
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final XmlParser ctx;
	private final StringBuilder rsb = new StringBuilder();  // Reusable string builder used in this class.

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected XmlParserSession(Builder builder) {
		super(builder);
		ctx = builder.ctx;
	}

	/**
	 * Wrap the specified reader in a STAX reader based on settings in this context.
	 *
	 * @param pipe The parser input.
	 * @return The new STAX reader.
	 * @throws IOException Thrown by underlying stream.
	 * @throws XMLStreamException Unexpected XML processing error.
	 */
	protected final XmlReader getXmlReader(ParserPipe pipe) throws IOException, XMLStreamException {
		return new XmlReader(pipe, isValidating(), getReporter(), getResolver(), getEventAllocator());
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
	 * Returns the _name attribute value.
	 * Any <js>'_x####_'</js> sequences in the string will be decoded.
	 */
	private String getNameProperty(XmlReader r) {
		return decodeString(r.getAttributeValue(null, getNamePropertyName()));
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
	 * @throws XMLStreamException Thrown by underlying reader.
	 * @throws IOException Thrown by underlying stream.
	 * @throws ParseException Malformed input encountered.
	 */
	protected String getElementText(XmlReader r) throws XMLStreamException, IOException, ParseException {
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
			throw new BasicRuntimeException("Invalid event type on stream reader for elementToString() method: ''{0}''", XmlUtils.toReadableEvent(r));
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
	 * @param r The input reader.
	 * @return The parsed text.
	 * @throws XMLStreamException Thrown by underlying reader.
	 * @throws IOException Thrown by underlying stream.
	 * @throws ParseException Malformed input encountered.
	 */
	protected String parseText(XmlReader r) throws IOException, XMLStreamException, ParseException {
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
	 * @throws XMLStreamException Thrown by underlying reader.
	 * @throws IOException Thrown by underlying stream.
	 * @throws ParseException Malformed input encountered.
	 */
	protected String parseWhitespaceElement(XmlReader r) throws IOException, XMLStreamException, ParseException {
		return null;
	}

	@Override /* ParserSession */
	protected <T> T doParse(ParserPipe pipe, ClassMeta<T> type) throws IOException, ParseException, ExecutableException {
		try {
			return parseAnything(type, null, getXmlReader(pipe), getOuter(), true, null);
		} catch (XMLStreamException e) {
			throw new ParseException(e);
		}
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
	 * @param <T> The expected type of object.
	 * @param eType The expected type of object.
	 * @param currAttr The current bean property name.
	 * @param r The reader.
	 * @param outer The outer object.
	 * @param isRoot If <jk>true</jk>, then we're serializing a root element in the document.
	 * @param pMeta The bean property metadata.
	 * @return The parsed object.
	 * @throws IOException Thrown by underlying stream.
	 * @throws ParseException Malformed input encountered.
	 * @throws ExecutableException Exception occurred on invoked constructor/method/field.
	 * @throws XMLStreamException Malformed XML encountered.
	 */
	protected <T> T parseAnything(ClassMeta<T> eType, String currAttr, XmlReader r,
			Object outer, boolean isRoot, BeanPropertyMeta pMeta) throws IOException, ParseException, ExecutableException, XMLStreamException {

		if (eType == null)
			eType = (ClassMeta<T>)object();
		ObjectSwap<T,Object> swap = (ObjectSwap<T,Object>)eType.getSwap(this);
		BuilderSwap<T,Object> builder = (BuilderSwap<T,Object>)eType.getBuilderSwap(this);
		ClassMeta<?> sType = null;
		if (builder != null)
			sType = builder.getBuilderClassMeta(this);
		else if (swap != null)
			sType = swap.getSwapClassMeta(this);
		else
			sType = eType;

		if (sType.isOptional())
			return (T)optional(parseAnything(eType.getElementType(), currAttr, r, outer, isRoot, pMeta));

		setCurrentClass(sType);

		String wrapperAttr = (isRoot && isPreserveRootElement()) ? r.getName().getLocalPart() : null;
		String typeAttr = r.getAttributeValue(null, getBeanTypePropertyName(eType));
		boolean isNil = "true".equals(r.getAttributeValue(null, "nil"));
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
				JsonMap m = new JsonMap(this);
				parseIntoMap(r, m, string(), object(), pMeta);
				if (wrapperAttr != null)
					m = new JsonMap(this).append(wrapperAttr, m);
				o = cast(m, pMeta, eType);
			} else if (jsonType == ARRAY)
				o = parseIntoCollection(r, new JsonList(this), null, pMeta);
			else if (jsonType == STRING) {
				o = getElementText(r);
				if (sType.isChar())
					o = parseCharacter(o);
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
			o = parseCharacter(getElementText(r));
		} else if (sType.isMap()) {
			Map m = (sType.canCreateNewInstance(outer) ? (Map)sType.newInstance(outer) : newGenericMap(sType));
			o = parseIntoMap(r, m, sType.getKeyType(), sType.getValueType(), pMeta);
			if (wrapperAttr != null)
				o = new JsonMap(this).append(wrapperAttr, m);
		} else if (sType.isCollection()) {
			Collection l = (sType.canCreateNewInstance(outer) ? (Collection)sType.newInstance(outer) : new JsonList(this));
			o = parseIntoCollection(r, l, sType, pMeta);
		} else if (sType.isNumber()) {
			o = parseNumber(getElementText(r), (Class<? extends Number>)sType.getInnerClass());
		} else if (builder != null || sType.canCreateNewBean(outer)) {
			if (getXmlClassMeta(sType).getFormat() == COLLAPSED) {
				String fieldName = r.getLocalName();
				BeanMap<?> m = builder != null ? toBeanMap(builder.create(this, eType)) : newBeanMap(outer, sType.getInnerClass());
				BeanPropertyMeta bpm = getXmlBeanMeta(m.getMeta()).getPropertyMeta(fieldName);
				ClassMeta<?> cm = m.getMeta().getClassMeta();
				Object value = parseAnything(cm, currAttr, r, m.getBean(false), false, null);
				setName(cm, value, currAttr);
				bpm.set(m, currAttr, value);
				o = builder != null ? builder.build(this, m.getBean(), eType) : m.getBean();
			} else {
				BeanMap m = builder != null ? toBeanMap(builder.create(this, eType)) : newBeanMap(outer, sType.getInnerClass());
				m = parseIntoBean(r, m, isNil);
				o = builder != null ? builder.build(this, m.getBean(), eType) : m.getBean();
			}
		} else if (sType.isArray() || sType.isArgs()) {
			ArrayList l = (ArrayList)parseIntoCollection(r, list(), sType, pMeta);
			o = toArray(sType, l);
		} else if (sType.canCreateNewInstanceFromString(outer)) {
			o = sType.newInstanceFromString(outer, getElementText(r));
		} else if (sType.getProxyInvocationHandler() != null) {
			JsonMap m = new JsonMap(this);
			parseIntoMap(r, m, string(), object(), pMeta);
			if (wrapperAttr != null)
				m = new JsonMap(this).append(wrapperAttr, m);
			o = newBeanMap(outer, sType.getInnerClass()).load(m).getBean();
		} else {
			throw new ParseException(this,
				"Class ''{0}'' could not be instantiated.  Reason: ''{1}'', property: ''{2}''",
				sType.getInnerClass().getName(), sType.getNotABeanReason(), pMeta == null ? null : pMeta.getName());
		}

		if (swap != null && o != null)
			o = unswap(swap, o, eType);

		if (outer != null)
			setParent(eType, o, outer);

		return (T)o;
	}

	private <K,V> Map<K,V> parseIntoMap(XmlReader r, Map<K,V> m, ClassMeta<K> keyType,
			ClassMeta<V> valueType, BeanPropertyMeta pMeta) throws IOException, ParseException, ExecutableException, XMLStreamException {
		int depth = 0;
		for (int i = 0; i < r.getAttributeCount(); i++) {
			String a = r.getAttributeLocalName(i);
			// TODO - Need better handling of namespaces here.
			if (! isSpecialAttr(a)) {
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
				currAttr = getNameProperty(r);
				if (currAttr == null)
					currAttr = getElementName(r);
				K key = convertAttrToType(m, currAttr, keyType);
				V value = parseAnything(valueType, currAttr, r, m, false, pMeta);
				setName(valueType, value, currAttr);
				if (valueType.isObject() && m.containsKey(key)) {
					Object o = m.get(key);
					if (o instanceof List)
						((List)o).add(value);
					else
						m.put(key, (V)new JsonList(o, value).setBeanSession(this));
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
			ClassMeta<?> type, BeanPropertyMeta pMeta) throws IOException, ParseException, ExecutableException, XMLStreamException {
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

	private <T> BeanMap<T> parseIntoBean(XmlReader r, BeanMap<T> m, boolean isNil) throws IOException, ParseException, ExecutableException, XMLStreamException {
		BeanMeta<?> bMeta = m.getMeta();
		XmlBeanMeta xmlMeta = getXmlBeanMeta(bMeta);

		for (int i = 0; i < r.getAttributeCount(); i++) {
			String key = getAttributeName(r, i);
			if (! ("nil".equals(key) || isSpecialAttr(key))) {
				String val = r.getAttributeValue(i);
				String ns = r.getAttributeNamespace(i);
				BeanPropertyMeta bpm = xmlMeta.getPropertyMeta(key);
				if (bpm == null) {
					if (xmlMeta.getAttrsProperty() != null) {
						xmlMeta.getAttrsProperty().add(m, key, key, val);
					} else if (ns == null) {
						onUnknownProperty(key, m, val);
					}
				} else {
					try {
						bpm.set(m, key, val);
					} catch (BeanRuntimeException e) {
						onBeanSetterException(bpm, e);
						throw e;
					}
				}
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
							l = new LinkedList<>();
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
								l = new LinkedList<>();
							l.add(parseWhitespaceElement(r));
						} else {
							cp.set(m, null, parseWhitespaceElement(r));
						}
					} else {
						if (cpcm.isCollectionOrArray()) {
							if (l == null)
								l = new LinkedList<>();
							l.add(parseAnything(cpcm.getElementType(), cp.getName(), r, m.getBean(false), false, cp));
						} else {
							cp.set(m, null, parseAnything(cpcm, cp.getName(), r, m.getBean(false), false, cp));
						}
					}
				} else if (cp != null && cpf == ELEMENTS) {
					cp.add(m, null, parseAnything(cpcm.getElementType(), cp.getName(), r, m.getBean(false), false, cp));
				} else {
					currAttr = getNameProperty(r);
					if (currAttr == null)
						currAttr = getElementName(r);
					BeanPropertyMeta pMeta = xmlMeta.getPropertyMeta(currAttr);
					if (pMeta == null) {
						Object value = parseAnything(object(), currAttr, r, m.getBean(false), false, null);
						onUnknownProperty(currAttr, m, value);
					} else {
						setCurrentProperty(pMeta);
						XmlFormat xf = getXmlBeanPropertyMeta(pMeta).getXmlFormat();
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
			} else if (event == COMMENT) {
				// Ignore comments.
			} else {
				throw new ParseException("Unexpected event type: {0}", XmlUtils.toReadableEvent(r));
			}
		} while (depth >= 0);

		if (cp != null && ! isNil) {
			if (sb != null)
				cp.set(m, null, sb.toString());
			else if (l != null)
				cp.set(m, null, XmlUtils.collapseTextNodes(l));
			else if (cpcm.isCollectionOrArray()) {
				Object o = cp.get(m, null);
				if (o == null)
					cp.set(m, cp.getName(), list());
			}
		}

		returnStringBuilder(sb);
		return m;
	}

	private boolean isSpecialAttr(String key) {
		return key.equals(getBeanTypePropertyName(null)) || key.equals(getNamePropertyName());
	}

	private Object getUnknown(XmlReader r) throws IOException, ParseException, ExecutableException, XMLStreamException {
		if (r.getEventType() != START_ELEMENT) {
			throw new ParseException(this, "Parser must be on START_ELEMENT to read next text.");
		}
		JsonMap m = null;

		// If this element has attributes, then it's always a JsonMap.
		if (r.getAttributeCount() > 0) {
			m = new JsonMap(this);
			for (int i = 0; i < r.getAttributeCount(); i++) {
				String key = getAttributeName(r, i);
				String val = r.getAttributeValue(i);
				if (! isSpecialAttr(key))
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
				throw new ParseException(this, "Unexpected end of document when reading element text content");
			} else if (eventType == START_ELEMENT) {
				// Oops...this has an element in it.
				// Parse it as a map.
				if (m == null)
					m = new JsonMap(this);
				int depth = 0;
				do {
					int event = (eventType == -1 ? r.nextTag() : eventType);
					String currAttr;
					if (event == START_ELEMENT) {
						depth++;
						currAttr = getNameProperty(r);
						if (currAttr == null)
							currAttr = getElementName(r);
						String key = convertAttrToType(null, currAttr, string());
						Object value = parseAnything(object(), currAttr, r, null, false, null);
						if (m.containsKey(key)) {
							Object o = m.get(key);
							if (o instanceof JsonList)
								((JsonList)o).add(value);
							else
								m.put(key, new JsonList(o, value).setBeanSession(this));
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
				throw new ParseException(this, "Unexpected event type ''{0}''", eventType);
			}
			eventType = r.next();
		}
		String s = sb.toString().trim();
		returnStringBuilder(sb);
		s = decodeString(s);
		if (m != null) {
			if (! s.isEmpty())
				m.put("contents", s);
			return m;
		}
		return s;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * XML event allocator.
	 *
	 * @see XmlParser.Builder#eventAllocator(Class)
	 * @return
	 * 	The {@link XMLEventAllocator} associated with this parser, or <jk>null</jk> if there isn't one.
	 */
	protected final XMLEventAllocator getEventAllocator() {
		return ctx.getEventAllocator();
	}

	/**
	 * Preserve root element during generalized parsing.
	 *
	 * @see XmlParser.Builder#preserveRootElement()
	 * @return
	 * 	<jk>true</jk> if when parsing into a generic {@link JsonMap}, the map will contain a single entry whose key
	 * 	is the root element name.
	 */
	protected final boolean isPreserveRootElement() {
		return ctx.isPreserveRootElement();
	}

	/**
	 * XML reporter.
	 *
	 * @see XmlParser.Builder#reporter(Class)
	 * @return
	 * 	The {@link XMLReporter} associated with this parser, or <jk>null</jk> if there isn't one.
	 */
	protected final XMLReporter getReporter() {
		return ctx.getReporter();
	}

	/**
	 * XML resolver.
	 *
	 * @see XmlParser.Builder#resolver(Class)
	 * @return
	 * 	The {@link XMLResolver} associated with this parser, or <jk>null</jk> if there isn't one.
	 */
	protected final XMLResolver getResolver() {
		return ctx.getResolver();
	}

	/**
	 * Enable validation.
	 *
	 * @see XmlParser.Builder#validating()
	 * @return
	 * 	<jk>true</jk> if XML document will be validated.
	 */
	protected final boolean isValidating() {
		return ctx.isValidating();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Extended metadata
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the language-specific metadata on the specified class.
	 *
	 * @param cm The class to return the metadata on.
	 * @return The metadata.
	 */
	protected XmlClassMeta getXmlClassMeta(ClassMeta<?> cm) {
		return ctx.getXmlClassMeta(cm);
	}

	/**
	 * Returns the language-specific metadata on the specified bean.
	 *
	 * @param bm The bean to return the metadata on.
	 * @return The metadata.
	 */
	protected XmlBeanMeta getXmlBeanMeta(BeanMeta<?> bm) {
		return ctx.getXmlBeanMeta(bm);
	}

	/**
	 * Returns the language-specific metadata on the specified bean property.
	 *
	 * @param bpm The bean property to return the metadata on.
	 * @return The metadata.
	 */
	protected XmlBeanPropertyMeta getXmlBeanPropertyMeta(BeanPropertyMeta bpm) {
		return ctx.getXmlBeanPropertyMeta(bpm);
	}
}
