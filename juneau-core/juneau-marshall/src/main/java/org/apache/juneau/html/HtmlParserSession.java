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
package org.apache.juneau.html;

import static javax.xml.stream.XMLStreamConstants.*;
import static org.apache.juneau.html.HtmlTag.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;

import javax.xml.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.utils.*;
import org.apache.juneau.xml.*;

/**
 * Session object that lives for the duration of a single use of {@link HtmlParser}.
 *
 * <p>
 * This class is NOT thread safe.
 * It is typically discarded after one-time use although it can be reused against multiple inputs.
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public final class HtmlParserSession extends XmlParserSession {

	private static final Set<String> whitespaceElements = ASet.of("br","bs","sp","ff");

	private final HtmlParser ctx;

	/**
	 * Create a new session using properties specified in the context.
	 *
	 * @param ctx
	 * 	The context creating this session object.
	 * 	The context contains all the configuration settings for this object.
	 * @param args
	 * 	Runtime session arguments.
	 */
	protected HtmlParserSession(HtmlParser ctx, ParserSessionArgs args) {
		super(ctx, args);
		this.ctx = ctx;
	}

	@Override /* ParserSession */
	protected <T> T doParse(ParserPipe pipe, ClassMeta<T> type) throws IOException, ParseException, ExecutableException {
		try {
			return parseAnything(type, getXmlReader(pipe), getOuter(), true, null);
		} catch (XMLStreamException e) {
			throw new ParseException(e);
		}
	}

	@Override /* ReaderParserSession */
	protected <K,V> Map<K,V> doParseIntoMap(ParserPipe pipe, Map<K,V> m, Type keyType, Type valueType)
			throws Exception {
		return parseIntoMap(getXmlReader(pipe), m, (ClassMeta<K>)getClassMeta(keyType),
			(ClassMeta<V>)getClassMeta(valueType), null);
	}

	@Override /* ReaderParserSession */
	protected <E> Collection<E> doParseIntoCollection(ParserPipe pipe, Collection<E> c, Type elementType)
			throws Exception {
		return parseIntoCollection(getXmlReader(pipe), c, getClassMeta(elementType), null);
	}

	/*
	 * Reads anything starting at the current event.
	 * <p>
	 * Precondition:  Must be pointing at outer START_ELEMENT.
	 * Postcondition:  Pointing at outer END_ELEMENT.
	 */
	private <T> T parseAnything(ClassMeta<T> eType, XmlReader r, Object outer, boolean isRoot, BeanPropertyMeta pMeta) throws IOException, ParseException, ExecutableException, XMLStreamException {

		if (eType == null)
			eType = (ClassMeta<T>)object();
		PojoSwap<T,Object> swap = (PojoSwap<T,Object>)eType.getPojoSwap(this);
		BuilderSwap<T,Object> builder = (BuilderSwap<T,Object>)eType.getBuilderSwap(this);
		ClassMeta<?> sType = null;
		if (builder != null)
			sType = builder.getBuilderClassMeta(this);
		else if (swap != null)
			sType = swap.getSwapClassMeta(this);
		else
			sType = eType;

		if (sType.isOptional())
			return (T)Optional.ofNullable(parseAnything(eType.getElementType(), r, outer, isRoot, pMeta));

		setCurrentClass(sType);

		int event = r.getEventType();
		if (event != START_ELEMENT)
			throw new ParseException(this, "parseAnything must be called on outer start element.");

		if (! isRoot)
			event = r.next();
		boolean isEmpty = (event == END_ELEMENT);

		// Skip until we find a start element, end document, or non-empty text.
		if (! isEmpty)
			event = skipWs(r);

		if (event == END_DOCUMENT)
			throw new ParseException(this, "Unexpected end of stream in parseAnything for type ''{0}''", eType);

		// Handle @Html(asXml=true) beans.
		HtmlClassMeta hcm = getHtmlClassMeta(sType);
		if (hcm.getFormat() == HtmlFormat.XML)
			return super.parseAnything(eType, null, r, outer, false, pMeta);

		Object o = null;

		boolean isValid = true;
		HtmlTag tag = (event == CHARACTERS ? null : HtmlTag.forString(r.getName().getLocalPart(), false));

		// If it's not a known tag, then parse it as XML.
		// Allows us to parse stuff like "<div/>" into HTML5 beans.
		if (tag == null && event != CHARACTERS)
			return super.parseAnything(eType, null, r, outer, false, pMeta);

		if (tag == HTML)
			tag = skipToData(r);

		if (isEmpty) {
			o = "";
		} else if (tag == null || tag.isOneOf(BR,BS,FF,SP)) {
			String text = parseText(r);
			if (sType.isObject() || sType.isCharSequence())
				o = text;
			else if (sType.isChar())
				o = parseCharacter(text);
			else if (sType.isBoolean())
				o = Boolean.parseBoolean(text);
			else if (sType.isNumber())
				o = parseNumber(text, (Class<? extends Number>)eType.getInnerClass());
			else if (sType.canCreateNewInstanceFromString(outer))
				o = sType.newInstanceFromString(outer, text);
			else
				isValid = false;

		} else if (tag == STRING || (tag == A && pMeta != null && getHtmlBeanPropertyMeta(pMeta).getLink() != null)) {
			String text = getElementText(r);
			if (sType.isObject() || sType.isCharSequence())
				o = text;
			else if (sType.isChar())
				o = parseCharacter(text);
			else if (sType.canCreateNewInstanceFromString(outer))
				o = sType.newInstanceFromString(outer, text);
			else
				isValid = false;
			skipTag(r, tag == STRING ? xSTRING : xA);

		} else if (tag == NUMBER) {
			String text = getElementText(r);
			if (sType.isObject())
				o = parseNumber(text, Number.class);
			else if (sType.isNumber())
				o = parseNumber(text, (Class<? extends Number>)sType.getInnerClass());
			else
				isValid = false;
			skipTag(r, xNUMBER);

		} else if (tag == BOOLEAN) {
			String text = getElementText(r);
			if (sType.isObject() || sType.isBoolean())
				o = Boolean.parseBoolean(text);
			else
				isValid = false;
			skipTag(r, xBOOLEAN);

		} else if (tag == P) {
			String text = getElementText(r);
			if (! "No Results".equals(text))
				isValid = false;
			skipTag(r, xP);

		} else if (tag == NULL) {
			skipTag(r, NULL);
			skipTag(r, xNULL);

		} else if (tag == A) {
			o = parseAnchor(r, eType);
			skipTag(r, xA);

		} else if (tag == TABLE) {

			String typeName = getAttribute(r, getBeanTypePropertyName(eType), "object");
			ClassMeta cm = getClassMeta(typeName, pMeta, eType);

			if (cm != null) {
				sType = eType = cm;
				typeName = sType.isCollectionOrArray() ? "array" : "object";
			} else if (! "array".equals(typeName)) {
				// Type name could be a subtype name.
				typeName = sType.isCollectionOrArray() ? "array" : "object";
			}

			if (typeName.equals("object")) {
				if (sType.isObject()) {
					o = parseIntoMap(r, (Map)new ObjectMap(this), sType.getKeyType(), sType.getValueType(),
						pMeta);
				} else if (sType.isMap()) {
					o = parseIntoMap(r, (Map)(sType.canCreateNewInstance(outer) ? sType.newInstance(outer)
						: new ObjectMap(this)), sType.getKeyType(), sType.getValueType(), pMeta);
				} else if (builder != null) {
					BeanMap m = toBeanMap(builder.create(this, eType));
					o = builder.build(this, parseIntoBean(r, m).getBean(), eType);
				} else if (sType.canCreateNewBean(outer)) {
					BeanMap m = newBeanMap(outer, sType.getInnerClass());
					o = parseIntoBean(r, m).getBean();
				} else {
					isValid = false;
				}
				skipTag(r, xTABLE);

			} else if (typeName.equals("array")) {
				if (sType.isObject())
					o = parseTableIntoCollection(r, (Collection)new ObjectList(this), sType, pMeta);
				else if (sType.isCollection())
					o = parseTableIntoCollection(r, (Collection)(sType.canCreateNewInstance(outer)
						? sType.newInstance(outer) : new ObjectList(this)), sType, pMeta);
				else if (sType.isArray() || sType.isArgs()) {
					ArrayList l = (ArrayList)parseTableIntoCollection(r, new ArrayList(), sType, pMeta);
					o = toArray(sType, l);
				}
				else
					isValid = false;
				skipTag(r, xTABLE);

			} else {
				isValid = false;
			}

		} else if (tag == UL) {
			String typeName = getAttribute(r, getBeanTypePropertyName(eType), "array");
			ClassMeta cm = getClassMeta(typeName, pMeta, eType);
			if (cm != null)
				sType = eType = cm;

			if (sType.isObject())
				o = parseIntoCollection(r, new ObjectList(this), sType, pMeta);
			else if (sType.isCollection() || sType.isObject())
				o = parseIntoCollection(r, (Collection)(sType.canCreateNewInstance(outer)
					? sType.newInstance(outer) : new ObjectList(this)), sType, pMeta);
			else if (sType.isArray() || sType.isArgs())
				o = toArray(sType, parseIntoCollection(r, new ArrayList(), sType, pMeta));
			else
				isValid = false;
			skipTag(r, xUL);

		}

		if (! isValid)
			throw new ParseException(this, "Unexpected tag ''{0}'' for type ''{1}''", tag, eType);

		if (swap != null && o != null)
			o = unswap(swap, o, eType);

		if (outer != null)
			setParent(eType, o, outer);

		skipWs(r);
		return (T)o;
	}

	/*
	 * For parsing output from HtmlDocSerializer, this skips over the head, title, and links.
	 */
	private HtmlTag skipToData(XmlReader r) throws ParseException, XMLStreamException {
		while (true) {
			int event = r.next();
			if (event == START_ELEMENT && "div".equals(r.getLocalName()) && "data".equals(r.getAttributeValue(null, "id"))) {
				r.nextTag();
				event = r.getEventType();
				boolean isEmpty = (event == END_ELEMENT);
				// Skip until we find a start element, end document, or non-empty text.
				if (! isEmpty)
					event = skipWs(r);
				if (event == END_DOCUMENT)
					throw new ParseException(this, "Unexpected end of stream looking for data.");
				return (event == CHARACTERS ? null : HtmlTag.forString(r.getName().getLocalPart(), false));
			}
		}
	}

	private static String getAttribute(XmlReader r, String name, String def) {
		for (int i = 0; i < r.getAttributeCount(); i++)
			if (r.getAttributeLocalName(i).equals(name))
				return r.getAttributeValue(i);
		return def;
	}

	/*
	 * Reads an anchor tag and converts it into a bean.
	 */
	private <T> T parseAnchor(XmlReader r, ClassMeta<T> beanType)
			throws IOException, ParseException, XMLStreamException {
		String href = r.getAttributeValue(null, "href");
		String name = getElementText(r);
		if (beanType.hasAnnotation(HtmlLink.class)) {
			String uriProperty = "", nameProperty = "";
			for (HtmlLink a : beanType.getAnnotations(HtmlLink.class)) {
				if (! a.uriProperty().isEmpty())
					uriProperty = a.uriProperty();
				if (! a.nameProperty().isEmpty())
					nameProperty = a.nameProperty();
			}
			BeanMap<T> m = newBeanMap(beanType.getInnerClass());
			m.put(uriProperty, href);
			m.put(nameProperty, name);
			return m.getBean();
		}
		return convertToType(href, beanType);
	}

	private static Map<String,String> getAttributes(XmlReader r) {
		Map<String,String> m = new TreeMap<>() ;
		for (int i = 0; i < r.getAttributeCount(); i++)
			m.put(r.getAttributeLocalName(i), r.getAttributeValue(i));
		return m;
	}

	/*
	 * Reads contents of <table> element.
	 * Precondition:  Must be pointing at <table> event.
	 * Postcondition:  Pointing at next START_ELEMENT or END_DOCUMENT event.
	 */
	private <K,V> Map<K,V> parseIntoMap(XmlReader r, Map<K,V> m, ClassMeta<K> keyType,
			ClassMeta<V> valueType, BeanPropertyMeta pMeta) throws IOException, ParseException, ExecutableException, XMLStreamException {
		while (true) {
			HtmlTag tag = nextTag(r, TR, xTABLE);
			if (tag == xTABLE)
				break;
			tag = nextTag(r, TD, TH);
			// Skip over the column headers.
			if (tag == TH) {
				skipTag(r);
				r.nextTag();
				skipTag(r);
			} else {
				K key = parseAnything(keyType, r, m, false, pMeta);
				nextTag(r, TD);
				V value = parseAnything(valueType, r, m, false, pMeta);
				setName(valueType, value, key);
				m.put(key, value);
			}
			tag = nextTag(r, xTD, xTR);
			if (tag == xTD)
				nextTag(r, xTR);
		}

		return m;
	}

	/*
	 * Reads contents of <ul> element.
	 * Precondition:  Must be pointing at event following <ul> event.
	 * Postcondition:  Pointing at next START_ELEMENT or END_DOCUMENT event.
	 */
	private <E> Collection<E> parseIntoCollection(XmlReader r, Collection<E> l,
			ClassMeta<?> type, BeanPropertyMeta pMeta) throws IOException, ParseException, ExecutableException, XMLStreamException {
		int argIndex = 0;
		while (true) {
			HtmlTag tag = nextTag(r, LI, xUL, xLI);
			if (tag == xLI)
				tag = nextTag(r, LI, xUL, xLI);
			if (tag == xUL)
				break;
			ClassMeta<?> elementType = type.isArgs() ? type.getArg(argIndex++) : type.getElementType();
			l.add((E)parseAnything(elementType, r, l, false, pMeta));
		}
		return l;
	}

	/*
	 * Reads contents of <ul> element.
	 * Precondition:  Must be pointing at event following <ul> event.
	 * Postcondition:  Pointing at next START_ELEMENT or END_DOCUMENT event.
	 */
	private <E> Collection<E> parseTableIntoCollection(XmlReader r, Collection<E> l,
			ClassMeta<E> type, BeanPropertyMeta pMeta) throws IOException, ParseException, ExecutableException, XMLStreamException {

		HtmlTag tag = nextTag(r, TR);
		List<String> keys = new ArrayList<>();
		while (true) {
			tag = nextTag(r, TH, xTR);
			if (tag == xTR)
				break;
			keys.add(getElementText(r));
		}

		int argIndex = 0;

		while (true) {
			r.nextTag();
			tag = HtmlTag.forEvent(this, r);
			if (tag == xTABLE)
				break;

			ClassMeta elementType = null;
			String beanType = getAttribute(r, getBeanTypePropertyName(type), null);
			if (beanType != null)
				elementType = getClassMeta(beanType, pMeta, null);
			if (elementType == null)
				elementType = type.isArgs() ? type.getArg(argIndex++) : type.getElementType();
			if (elementType == null)
				elementType = object();

			BuilderSwap<E,Object> builder = elementType.getBuilderSwap(this);

			if (builder != null || elementType.canCreateNewBean(l)) {
				BeanMap m =
					builder != null
					? toBeanMap(builder.create(this, elementType))
					: newBeanMap(l, elementType.getInnerClass())
				;
				for (int i = 0; i < keys.size(); i++) {
					tag = nextTag(r, xTD, TD, NULL);
					if (tag == xTD)
						tag = nextTag(r, TD, NULL);
					if (tag == NULL) {
						m = null;
						nextTag(r, xNULL);
						break;
					}
					String key = keys.get(i);
					BeanMapEntry e = m.getProperty(key);
					if (e == null) {
						//onUnknownProperty(key, m, -1, -1);
						parseAnything(object(), r, l, false, null);
					} else {
						BeanPropertyMeta bpm = e.getMeta();
						ClassMeta<?> cm = bpm.getClassMeta();
						Object value = parseAnything(cm, r, m.getBean(false), false, bpm);
						setName(cm, value, key);
						bpm.set(m, key, value);
					}
				}
				l.add(
					m == null
					? null
					: builder != null
						? builder.build(this, m.getBean(), elementType)
						: (E)m.getBean()
				);
			} else {
				String c = getAttributes(r).get(getBeanTypePropertyName(type.getElementType()));
				Map m = (Map)(elementType.isMap() && elementType.canCreateNewInstance(l) ? elementType.newInstance(l)
					: new ObjectMap(this));
				for (int i = 0; i < keys.size(); i++) {
					tag = nextTag(r, TD, NULL);
					if (tag == NULL) {
						m = null;
						nextTag(r, xNULL);
						break;
					}
					String key = keys.get(i);
					if (m != null) {
						ClassMeta<?> kt = elementType.getKeyType(), vt = elementType.getValueType();
						Object value = parseAnything(vt, r, l, false, pMeta);
						setName(vt, value, key);
						m.put(convertToType(key, kt), value);
					}
				}
				if (m != null && c != null) {
					ObjectMap m2 = (m instanceof ObjectMap ? (ObjectMap)m : new ObjectMap(m).setBeanSession(this));
					m2.put(getBeanTypePropertyName(type.getElementType()), c);
					l.add((E)cast(m2, pMeta, elementType));
				} else {
					if (m instanceof ObjectMap)
						l.add((E)convertToType(m, elementType));
					else
						l.add((E)m);
				}
			}
			nextTag(r, xTR);
		}
		return l;
	}

	/*
	 * Reads contents of <table> element.
	 * Precondition:  Must be pointing at event following <table> event.
	 * Postcondition:  Pointing at next START_ELEMENT or END_DOCUMENT event.
	 */
	private <T> BeanMap<T> parseIntoBean(XmlReader r, BeanMap<T> m) throws IOException, ParseException, ExecutableException, XMLStreamException {
		while (true) {
			HtmlTag tag = nextTag(r, TR, xTABLE);
			if (tag == xTABLE)
				break;
			tag = nextTag(r, TD, TH);
			// Skip over the column headers.
			if (tag == TH) {
				skipTag(r);
				r.nextTag();
				skipTag(r);
			} else {
				String key = getElementText(r);
				nextTag(r, TD);
				BeanPropertyMeta pMeta = m.getPropertyMeta(key);
				if (pMeta == null) {
					onUnknownProperty(key, m);
					parseAnything(object(), r, null, false, null);
				} else {
					ClassMeta<?> cm = pMeta.getClassMeta();
					Object value = parseAnything(cm, r, m.getBean(false), false, pMeta);
					setName(cm, value, key);
					pMeta.set(m, key, value);
				}
			}
			HtmlTag t = nextTag(r, xTD, xTR);
			if (t == xTD)
				nextTag(r, xTR);
		}
		return m;
	}

	/*
	 * Reads the next tag.  Advances past anything that's not a start or end tag.  Throws an exception if
	 * 	it's not one of the expected tags.
	 * Precondition:  Must be pointing before the event we want to parse.
	 * Postcondition:  Pointing at the tag just parsed.
	 */
	private HtmlTag nextTag(XmlReader r, HtmlTag...expected) throws ParseException, XMLStreamException {
		int et = r.next();

		while (et != START_ELEMENT && et != END_ELEMENT && et != END_DOCUMENT)
			et = r.next();

		if (et == END_DOCUMENT)
			throw new ParseException(this, "Unexpected end of document.");

		HtmlTag tag = HtmlTag.forEvent(this, r);
		if (expected.length == 0)
			return tag;
		for (HtmlTag t : expected)
			if (t == tag)
				return tag;

		throw new ParseException(this, "Unexpected tag: ''{0}''.  Expected one of the following: {1}", tag, expected);
	}

	/*
	 * Skips over the current element and advances to the next element.
	 * <p>
	 * Precondition:  Pointing to opening tag.
	 * Postcondition:  Pointing to next opening tag.
	 *
	 * @param r The stream being read from.
	 * @throws XMLStreamException
	 */
	private void skipTag(XmlReader r) throws ParseException, XMLStreamException {
		int et = r.getEventType();

		if (et != START_ELEMENT)
			throw new ParseException(this,
				"skipToNextTag() call on invalid event ''{0}''.  Must only be called on START_ELEMENT events.",
				XmlUtils.toReadableEvent(r)
			);

		String n = r.getLocalName();

		int depth = 0;
		while (true) {
			et = r.next();
			if (et == START_ELEMENT) {
				String n2 = r.getLocalName();
					if (n.equals(n2))
				depth++;
			} else if (et == END_ELEMENT) {
				String n2 = r.getLocalName();
				if (n.equals(n2))
					depth--;
				if (depth < 0)
					return;
			}
		}
	}

	private void skipTag(XmlReader r, HtmlTag...expected) throws ParseException, XMLStreamException {
		HtmlTag tag = HtmlTag.forEvent(this, r);
		if (tag.isOneOf(expected))
			r.next();
		else
			throw new ParseException(this,
				"Unexpected tag: ''{0}''.  Expected one of the following: {1}",
				tag, expected);
	}

	private static int skipWs(XmlReader r)  throws XMLStreamException {
		int event = r.getEventType();
		while (event != START_ELEMENT && event != END_ELEMENT && event != END_DOCUMENT && r.isWhiteSpace())
			event = r.next();
		return event;
	}

	/**
	 * Parses CHARACTERS data.
	 *
	 * <p>
	 * Precondition:  Pointing to event immediately following opening tag.
	 * Postcondition:  Pointing to closing tag.
	 *
	 * @param r The stream being read from.
	 * @return The parsed string.
	 * @throws XMLStreamException Thrown by underlying XML stream.
	 */
	@Override /* XmlParserSession */
	protected final String parseText(XmlReader r) throws IOException, ParseException, XMLStreamException {

		StringBuilder sb = getStringBuilder();

		int et = r.getEventType();
		if (et == END_ELEMENT)
			return "";

		int depth = 0;

		String characters = null;

		while (true) {
			if (et == START_ELEMENT) {
				if (characters != null) {
					if (sb.length() == 0)
						characters = trimStart(characters);
					sb.append(characters);
					characters = null;
				}
				HtmlTag tag = HtmlTag.forEvent(this, r);
				if (tag == BR) {
					sb.append('\n');
					r.nextTag();
				} else if (tag == BS) {
					sb.append('\b');
					r.nextTag();
				} else if (tag == SP) {
					et = r.next();
					if (et == CHARACTERS) {
						String s = r.getText();
						if (s.length() > 0) {
							char c = r.getText().charAt(0);
							if (c == '\u2003')
								c = '\t';
							sb.append(c);
						}
						r.nextTag();
					}
				} else if (tag == FF) {
					sb.append('\f');
					r.nextTag();
				} else if (tag.isOneOf(STRING, NUMBER, BOOLEAN)) {
					et = r.next();
					if (et == CHARACTERS) {
						sb.append(r.getText());
						r.nextTag();
					}
				} else {
					sb.append('<').append(r.getLocalName());
					for (int i = 0; i < r.getAttributeCount(); i++)
						sb.append(' ').append(r.getAttributeName(i)).append('=').append('\'').append(r.getAttributeValue(i)).append('\'');
					sb.append('>');
					depth++;
				}
			} else if (et == END_ELEMENT) {
				if (characters != null) {
					if (sb.length() == 0)
						characters = trimStart(characters);
					if (depth == 0)
						characters = trimEnd(characters);
					sb.append(characters);
					characters = null;
				}
				if (depth == 0)
					break;
				sb.append('<').append(r.getLocalName()).append('>');
				depth--;
			} else if (et == CHARACTERS) {
				characters = r.getText();
			}
			et = r.next();
		}

		String s = trim(sb.toString());
		returnStringBuilder(sb);
		return s;
	}

	/**
	 * Identical to {@link #parseText(XmlReader)} except assumes the current event is the opening tag.
	 *
	 * <p>
	 * Precondition:  Pointing to opening tag.
	 * Postcondition:  Pointing to closing tag.
	 *
	 * @param r The stream being read from.
	 * @return The parsed string.
	 * @throws XMLStreamException Thrown by underlying XML stream.
	 * @throws ParseException Malformed input encountered.
	 */
	@Override /* XmlParserSession */
	protected final String getElementText(XmlReader r) throws IOException, XMLStreamException, ParseException {
		r.next();
		return parseText(r);
	}

	@Override /* XmlParserSession */
	protected final boolean isWhitespaceElement(XmlReader r) {
		String s = r.getLocalName();
		return whitespaceElements.contains(s);
	}

	@Override /* XmlParserSession */
	protected final String parseWhitespaceElement(XmlReader r) throws IOException, ParseException, XMLStreamException {

		HtmlTag tag = HtmlTag.forEvent(this, r);
		int et = r.next();
		if (tag == BR) {
			return "\n";
		} else if (tag == BS) {
			return "\b";
		} else if (tag == FF) {
			return "\f";
		} else if (tag == SP) {
			if (et == CHARACTERS) {
				String s = r.getText();
				if (s.charAt(0) == '\u2003')
					s = "\t";
				r.next();
				return decodeString(s);
			}
			return "";
		} else {
			throw new ParseException(this, "Invalid tag found in parseWhitespaceElement(): ''{0}''", tag);
		}
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
	protected HtmlClassMeta getHtmlClassMeta(ClassMeta<?> cm) {
		return ctx.getHtmlClassMeta(cm);
	}

	/**
	 * Returns the language-specific metadata on the specified bean property.
	 *
	 * @param bpm The bean property to return the metadata on.
	 * @return The metadata.
	 */
	protected HtmlBeanPropertyMeta getHtmlBeanPropertyMeta(BeanPropertyMeta bpm) {
		return ctx.getHtmlBeanPropertyMeta(bpm);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Session */
	public ObjectMap toMap() {
		return super.toMap()
			.append("HtmlParserSession", new DefaultFilteringObjectMap()
			);
	}
}
