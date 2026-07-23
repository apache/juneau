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
package org.apache.juneau.marshall.xml;

import static javax.xml.stream.XMLStreamConstants.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.marshall.xml.XmlFormat.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import javax.xml.stream.*;
import javax.xml.stream.util.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.stream.*;
import org.apache.juneau.marshall.swap.*;

/**
 * Session object that lives for the duration of a single use of {@link XmlParser}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/XmlSupport">XML Basics</a>

 * </ul>
 */
@SuppressWarnings({
	"java:S110",  // Deep inheritance inherent to the serializer/parser session hierarchy
	"java:S115",  // PROP_xxx constants use camelCase after prefix intentionally (property keys, not enum-style constants)
	"unchecked", // Type erasure requires unchecked casts
	"rawtypes", // Raw types necessary for generic type handling
	"resource" // RecordReader returned by RecordAdapter is a Closeable owned by the caller; Eclipse JDT @Owning warning is by design.
})
public class XmlParserSession extends ReaderParserSession implements RecordReadable, ArrayRecordReadable {

	// Property name constants
	private static final String PROP_preserveRootElement = "preserveRootElement";
	private static final String PROP_validating = "validating";
	private static final String PROP_XmlParserSession_preserveRootElement = "XmlParserSession.preserveRootElement";
	private static final String PROP_XmlParserSession_validating = "XmlParserSession.validating";

	/**
	 * Builder class.
	 */
	@SuppressWarnings({
		"java:S119" // 'SELF' (CRTP self-type) is intentional and clearer than a single-letter name.
	})
	public abstract static class Builder<SELF extends Builder<SELF>> extends ReaderParserSession.Builder<SELF> {

		private boolean preserveRootElement;
		private boolean validating;
		private XmlParser ctx;

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(XmlParser ctx) {
			super(ctx);
			this.ctx = ctx;
			preserveRootElement = ctx.isPreserveRootElement();
			validating = ctx.isValidating();
		}

		@Override
		public XmlParserSession build() {
			return new XmlParserSession(this);
		}

		/**
		 * Preserve root element during XML parsing.
		 *
		 * @param value The new value for this setting.
		 * @return This object.
		 */
		public SELF preserveRootElement(boolean value) {
			preserveRootElement = value;
			return self();
		}

		/**
		 * Validate XML during parsing.
		 *
		 * @param value The new value for this setting.
		 * @return This object.
		 */
		public SELF validating(boolean value) {
			validating = value;
			return self();
		}

		@Override /* Overridden from Builder */
		public SELF property(String key, Object value) {
			if (key == null) { super.property(key, value); return self(); }
			switch (key) {
				case PROP_preserveRootElement, PROP_XmlParserSession_preserveRootElement:
					return preserveRootElement(cvt(value, Boolean.class));
				case PROP_validating, PROP_XmlParserSession_validating:
					return validating(cvt(value, Boolean.class));
				default:
					super.property(key, value);
					return self();
			}
		}

	}

	/**
	 * Concrete default builder leaf for the non-subclassed {@code create()} path (CRTP terminal).
	 */
	public static final class DefaultBuilder extends Builder<DefaultBuilder> {

		DefaultBuilder(XmlParser ctx) {
			super(ctx);
		}
	}

	private static final int UNKNOWN = 0;
	private static final int OBJECT = 1;
	private static final int ARRAY = 2;
	private static final int STRING = 3;
	private static final int NUMBER = 4;
	private static final int BOOLEAN = 5;
	private static final int NULL = 6;

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * 	<br>Cannot be <jk>null</jk>.
	 * @return A new builder.
	 */
	@SuppressWarnings({
		"java:S1452" // Builder<?> wildcard return intentional; callers use it to construct session instances polymorphically
	})
	public static Builder<?> create(XmlParser ctx) {
		return new DefaultBuilder(ctx);
	}

	private static int getJsonType(String s) {
		if (s == null)
			return UNKNOWN;
		var c = s.charAt(0);
		return switch (c) {
			case 'o' -> (s.equals("object") ? OBJECT : UNKNOWN);
			case 'a' -> (s.equals("array") ? ARRAY : UNKNOWN);
			case 's' -> (s.equals("string") ? STRING : UNKNOWN);
			case 'b' -> (s.equals("boolean") ? BOOLEAN : UNKNOWN);
			case 'n' -> {
				c = s.charAt(2);
				yield switch (c) {
					case 'm' -> (s.equals("number") ? NUMBER : UNKNOWN);
					case 'l' -> (s.equals("null") ? NULL : UNKNOWN);
					default -> NUMBER;
				};
			}
			default -> UNKNOWN;
		};
	}

	private final XmlParser ctx;
	private final boolean preserveRootElement;
	private final boolean validating;

	private final StringBuilder rsb = new StringBuilder();  // Reusable string builder used in this class.

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected XmlParserSession(Builder<?> builder) {
		super(builder);
		ctx = builder.ctx;
		preserveRootElement = builder.preserveRootElement;
		validating = builder.validating;
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

	/*
	 * Takes the element being read from the XML stream reader and reconstructs it as XML.
	 * Used when reconstructing bean properties of type {@link XmlFormat#XMLTEXT}.
	 */
	private String getElementAsString(XmlReader r) {
		int t = r.getEventType();
		if (t > 2)
			throw rex("Invalid event type on stream reader for elementToString() method: '%s'", XmlUtils.toReadableEvent(r));
		rsb.setLength(0);
		rsb.append("<").append(t == 1 ? "" : "/").append(r.getLocalName());
		if (t == 1)
			for (var i = 0; i < r.getAttributeCount(); i++)
				rsb.append(' ').append(r.getAttributeName(i)).append('=').append('\'').append(r.getAttributeValue(i)).append('\'');
		rsb.append('>');
		return rsb.toString();
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
		return decodeString(r.getAttributeValue(null, MarshallingSession.NAME_PROPERTY_NAME));
	}

	/*
	 * Shortcut for calling <code>getText(r, <jk>true</jk>);</code>.
	 */
	private String getText(XmlReader r) {
		return getText(r, true);
	}

	/*
	 * Returns the content of the current CHARACTERS node.
	 * Any <js>'_x####_'</js> sequences in the string will be decoded.
	 * Leading and trailing whitespace (unencoded) will be trimmed from the result.
	 */
	private String getText(XmlReader r, boolean trim) {
		var s = r.getText();
		if (trim)
			s = s.trim();
		if (s.isEmpty())
			return null;
		return decodeString(s);
	}

	@SuppressWarnings({
		"null",        // Null handling verified by context or framework
		"java:S3776",  // Cognitive complexity acceptable for this specific logic
		"java:S6541",  // Brain Method — complex XML parsing logic; structural refactoring would reduce readability without meaningful benefit.
	})
	private Object getUnknown(XmlReader r) throws IOException, ParseException, ExecutableException, XMLStreamException {
		if (r.getEventType() != START_ELEMENT) {
			throw new ParseException(this, "Parser must be on START_ELEMENT to read next text.");
		}
		MarshalledMap m = null;

		// If this element has attributes, then it's always a MarshalledMap.
		if (r.getAttributeCount() > 0) {
			m = newGenericMap();
			for (var i = 0; i < r.getAttributeCount(); i++) {
				var key = getAttributeName(r, i);
				var val = r.getAttributeValue(i);
				if (! isSpecialAttr(key))
					m.put(key, val);
			}
		}
		int eventType = r.next();
		var sb = getStringBuilder();
		while (eventType != END_ELEMENT) {
			if (eventType == CHARACTERS || eventType == CDATA || eventType == XMLStreamConstants.SPACE || eventType == ENTITY_REFERENCE) {
				sb.append(r.getText());
			} else if (eventType == PROCESSING_INSTRUCTION || eventType == COMMENT) {
				// skipping
			} else if (eventType == END_DOCUMENT) {
				throw new ParseException(this, "Unexpected end of document when reading element text content");
			} else if (eventType == START_ELEMENT) {
				// Oops...this has an element in it.
				// Parse it as a map.
				if (m == null)
					m = newGenericMap();
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
						var value = readAnything(object(), currAttr, r, null, false, null);
						if (m.containsKey(key)) {
							var o = m.get(key);
							if (o instanceof MarshalledList o2)
								o2.add(value);
							else
								m.put(key, new JsonList(o, value).setBeanSession(this));
						} else {
							m.put(key, value);
						}

					} else if (event == END_ELEMENT) {
						break;
					}
					eventType = -1;
				} while (depth > 0);
				break;
			} else {
				throw new ParseException(this, "Unexpected event type '%s'", eventType);
			}
			eventType = r.next();
		}
		var s = sb.toString().trim();
		returnStringBuilder(sb);
		s = decodeString(s);
		if (nn(m)) {
			if (! s.isEmpty())
				m.put("contents", s);
			return m;
		}
		return s;
	}

	private boolean isSpecialAttr(String key) {
		return key.equals(getBeanTypePropertyName(null)) || key.equals(MarshallingSession.NAME_PROPERTY_NAME);
	}

	@SuppressWarnings({
		"null", // Null handling verified by context or framework
		"java:S3776", // Cognitive complexity acceptable for this specific logic
		"java:S6541", // Single-threaded session contexts do not require synchronization
	})
	private <T> BeanMap<T> readIntoBean(XmlReader r, BeanMap<T> m, boolean isNil) throws IOException, ParseException, ExecutableException, XMLStreamException {
		var bMeta = m.getMeta();
		var xmlMeta = getXmlBeanMeta(bMeta);

		for (var i = 0; i < r.getAttributeCount(); i++) {
			String key = getAttributeName(r, i);
			if (! ("nil".equals(key) || isSpecialAttr(key))) {
				var val = r.getAttributeValue(i);
				var ns = r.getAttributeNamespace(i);
				var bpm = xmlMeta.getPropertyMeta(key);
				if (bpm == null) {
					if (nn(xmlMeta.getAttrsProperty())) {
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

		var cp = xmlMeta.getContentProperty();
		var cpf = xmlMeta.getContentFormat();
		var trim = cp == null || ! cpf.isOneOf(MIXED_PWS, TEXT_PWS);
		ClassMeta<?> cpcm = (cp == null ? object() : (ClassMeta<?>) cp.getBeanInfo());
		StringBuilder sb = null;
		var breg = cp == null ? null : cp.getBeanRegistry();
		List<Object> l = null;

		int depth = 0;
		do {
			var event = r.next();
			String currAttr;
			// We only care about text in MIXED mode.
			// Ignore if in ELEMENTS mode.
			if (event == CHARACTERS) {
				if (nn(cp) && cpf.isOneOf(MIXED, MIXED_PWS)) {
					if (cpcm.isCollectionOrArray()) {
						if (l == null)
							l = new LinkedList<>();
						l.add(getText(r, false));
					} else {
						cp.set(m, null, getText(r, trim));
					}
				} else if (cpf != ELEMENTS) {
					var s = getText(r, trim);
					if (nn(s)) {
						if (sb == null)
							sb = getStringBuilder();
						sb.append(s);
					}
				} else {
					// Do nothing...we're in ELEMENTS mode.
				}
			} else if (event == START_ELEMENT) {
				if (nn(cp) && cpf.isOneOf(TEXT, TEXT_PWS)) {
					var s = readText(r);
					if (nn(s)) {
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
				} else if (nn(cp) && cpf.isOneOf(MIXED, MIXED_PWS)) {
					if (isWhitespaceElement(r) && (breg == null || ! breg.hasName(r.getLocalName()))) {
						if (cpcm.isCollectionOrArray()) {
							if (l == null)
								l = new LinkedList<>();
							l.add(readWhitespaceElement(r));
						} else {
							cp.set(m, null, readWhitespaceElement(r));
						}
					} else {
						if (cpcm.isCollectionOrArray()) {
							if (l == null)
								l = new LinkedList<>();
							l.add(readAnything(cpcm.getElementType(), cp.getName(), r, m.getBean(false), false, cp));
						} else {
							cp.set(m, null, readAnything(cpcm, cp.getName(), r, m.getBean(false), false, cp));
						}
					}
				} else if (nn(cp) && cpf == ELEMENTS) {
					cp.add(m, null, readAnything(cpcm.getElementType(), cp.getName(), r, m.getBean(false), false, cp));
				} else {
					currAttr = getNameProperty(r);
					if (currAttr == null)
						currAttr = getElementName(r);
					var pMeta = xmlMeta.getPropertyMeta(currAttr);
					if (pMeta == null) {
						var value = readAnything(object(), currAttr, r, m.getBean(false), false, null);
						onUnknownProperty(currAttr, m, value);
					} else {
						setCurrentProperty(pMeta);
						var xf = getXmlBeanPropertyMeta(pMeta).getXmlFormat();
						if (xf == COLLAPSED) {
							var et = ((ClassMeta<?>) pMeta.getBeanInfo()).getElementType();
							var value = readAnything(et, currAttr, r, m.getBean(false), false, pMeta);
							setName(et, value, currAttr);
							pMeta.add(m, currAttr, value);
						} else if (xf == ATTR) {
							pMeta.set(m, currAttr, getAttributeValue(r, 0));
							r.nextTag();
						} else {
							var cm = (ClassMeta<?>) pMeta.getBeanInfo();
							var value = readAnything(cm, currAttr, r, m.getBean(false), false, pMeta);
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
					} else
						throw new ParseException("End element found where one was not expected.  %s", XmlUtils.toReadableEvent(r));
				}
				depth--;
			} else if (event == COMMENT) {
				// Ignore comments.
			} else {
				throw new ParseException("Unexpected event type: %s", XmlUtils.toReadableEvent(r));
			}
		} while (depth >= 0);

		if (nn(cp) && ! isNil) {
			if (nn(sb))
				cp.set(m, null, sb.toString());
			else if (nn(l))
				cp.set(m, null, XmlUtils.collapseTextNodes(l));
			else if (cpcm.isCollectionOrArray()) {
				var o = cp.get(m, null);
				if (o == null)
					cp.set(m, cp.getName(), list());
			}
		}

		returnStringBuilder(sb);
		return m;
	}

	private <E> Collection<E> readIntoCollection(XmlReader r, Collection<E> l, ClassMeta<?> type, BeanPropertyMeta pMeta) throws IOException, ParseException, ExecutableException, XMLStreamException {
		int depth = 0;
		int argIndex = 0;
		do {
			var event = r.nextTag();
			if (event == START_ELEMENT) {
				depth++;
				ClassMeta<?> elementType;
				if (type == null) {
					elementType = object();
				} else if (type.isArgs()) {
					elementType = type.getArg(argIndex++);
				} else {
					elementType = type.getElementType();
				}
				E value = (E)readAnything(elementType, null, r, l, false, pMeta);
				l.add(value);
			} else if (event == END_ELEMENT) {
				return l;
			}
		} while (depth > 0);
		return l;
	}

	@SuppressWarnings({
		"java:S3776" // Cognitive complexity acceptable for parser state machine
	})
	private <K,V> Map<K,V> readIntoMap(XmlReader r, Map<K,V> m, ClassMeta<K> keyType, ClassMeta<V> valueType, BeanPropertyMeta pMeta)
		throws IOException, ParseException, ExecutableException, XMLStreamException {
		int depth = 0;
		for (var i = 0; i < r.getAttributeCount(); i++) {
			var a = r.getAttributeLocalName(i);
			if (! isSpecialAttr(a)) {
				K key = trim(convertAttrToType(m, a, keyType));
				V value = trim(convertAttrToType(m, r.getAttributeValue(i), valueType));
				setName(valueType, value, key);
				m.put(key, value);
			}
		}
		do {
			var event = r.nextTag();
			String currAttr;
			if (event == START_ELEMENT) {
				depth++;
				currAttr = getNameProperty(r);
				if (currAttr == null)
					currAttr = getElementName(r);
				K key = convertAttrToType(m, currAttr, keyType);
				V value = readAnything(valueType, currAttr, r, m, false, pMeta);
				setName(valueType, value, currAttr);
				if (valueType.isObject() && m.containsKey(key)) {
					var o = m.get(key);
					if (o instanceof List o2)
						o2.add(value);
					else
						m.put(key, (V)new JsonList(o, value).setBeanSession(this));
				} else {
					m.put(key, value);
				}
			} else if (event == END_ELEMENT) {
				return m;
			}
		} while (depth > 0);
		return m;
	}

	/**
	 * Decodes and trims the specified string.
	 *
	 * <p>
	 * Any <js>'_x####_'</js> sequences in the string will be decoded.
	 *
	 * @param s The string to be decoded.
	 * 	<br>Can be <jk>null</jk> (returns <jk>null</jk>).
	 * @return The decoded string, or <jk>null</jk> if the input was <jk>null</jk>.
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

	@Override /* Overridden from ParserSession */
	protected <T> T doRead(ParserPipe pipe, ClassMeta<T> type) throws IOException, ParseException, ExecutableException {
		try {
			return readAnything(type, null, getXmlReader(pipe), getOuter(), true, null);
		} catch (XMLStreamException e) {
			throw new ParseException(e);
		}
	}

	@Override /* Overridden from ReaderParserSession */
	protected <E> Collection<E> doReadIntoCollection(ParserPipe pipe, Collection<E> c, Type elementType) throws Exception {
		var cm = getClassMeta(c.getClass(), elementType);
		return readIntoCollection(getXmlReader(pipe), c, cm.getElementType(), null);
	}

	@Override /* Overridden from ReaderParserSession */
	protected <K,V> Map<K,V> doReadIntoMap(ParserPipe pipe, Map<K,V> m, Type keyType, Type valueType) throws Exception {
		return readIntoMap(getXmlReader(pipe), m, (ClassMeta<K>)getClassMeta(keyType), (ClassMeta<V>)getClassMeta(valueType), null);
	}

	/**
	 * Opens a whole-value pull-parser cursor over an XML document, bound to this live session.
	 *
	 * <p>
	 * Because XML's wire format (attributes, namespaces, mixed content) is non-trivial to expose
	 * as a fine-grained token cursor, this implementation supports only the cursor-level POJO
	 * bridge &mdash; {@link RecordReader#read(Class) read(...)} delegates to the polymorphic
	 * {@link ParserSession#read(Object, Class)} entry point.
	 *
	 * @param input The input.
	 * @return A new {@link RecordReader} cursor.
	 * @throws IOException If a problem occurred opening the underlying input.
	 */
	@Override /* RecordReadable */
	public RecordReader readRecords(Object input) throws IOException {
		return RecordAdapter.reader(this, input);
	}

	/**
	 * Buffered array-element {@link RecordReader} for XML, bound to this live session.  Calls
	 * {@code parse(input, List.class, Object.class)} once and iterates the result.
	 *
	 * <p>
	 * For caller-specified-root semantics use {@link #readArrayRecords(Object, String)}.
	 *
	 * @param input The input.
	 * @return A buffered {@link RecordReader}.
	 * @throws IOException If a problem occurred reading the input.
	 */
	@Override /* ArrayRecordReadable */
	public RecordReader readArrayRecords(Object input) throws IOException {
		return RecordAdapter.arrayReader(this, input);
	}

	/**
	 * Buffered array-element {@link RecordReader} that honors the caller-specified root element
	 * name.
	 *
	 * <p>
	 * The document is parsed once into a generic structure and the records to yield are selected by
	 * the supplied {@code rootElementName}:
	 * <ul>
	 * 	<li>If the document root is a <b>wrapper element</b> (parses to a {@link Map}) that contains a
	 * 		child named {@code rootElementName}, that child's value supplies the records (a repeated
	 * 		child element parses to a {@link List}; a single occurrence yields one record).  Sibling
	 * 		elements with other names are ignored.
	 * 	<li>If the document root is an <b>array wrapper</b> (parses to a {@link List}, e.g. Juneau's
	 * 		{@code <array>} envelope), its already-unwrapped elements are the records and the hint is a
	 * 		no-op.
	 * 	<li>A scalar root yields a single record.
	 * </ul>
	 *
	 * <p>
	 * A <jk>null</jk> {@code rootElementName} is equivalent to {@link #readArrayRecords(Object)}.
	 * The cursor is buffered ({@link RecordReader#isStreaming()} == <jk>false</jk>); see
	 * {@code 175ab} Item 3 for why true element-at-a-time XML streaming is left for a
	 * demand-driven StAX cursor.
	 *
	 * @param input The input.
	 * @param rootElementName The name of the wrapping/child element whose occurrences are the
	 * 	records.  <jk>null</jk> defers to {@link #readArrayRecords(Object)}.
	 * @return A buffered {@link RecordReader}.
	 * @throws IOException If a problem occurred reading the input.
	 */
	@Override /* ArrayRecordReadable */
	public RecordReader readArrayRecords(Object input, String rootElementName) throws IOException {
		if (rootElementName == null)
			return readArrayRecords(input);
		Object parsed;
		try {
			parsed = read(input, Object.class);
		} catch (ParseException e) {
			throw new IOException(e);
		}
		var records = selectRootElements(parsed, rootElementName);
		var iter = records.iterator();
		return new RecordReader() {
			@Override public boolean canRead() { return iter.hasNext(); }
			@Override public <T> T read(Class<T> type) { return convertToType(nextElement(), type); }
			@Override public <T> T read(ClassMeta<T> type) { return convertToType(nextElement(), type); }
			@Override public <T> T read(Type type, Type... args) { return XmlParserSession.this.<T>convertToType(nextElement(), type, args); }
			@Override public boolean isStreaming() { return false; }
			@Override public void close() throws IOException {
				if (input instanceof Closeable input2)
					input2.close();
			}
			private Object nextElement() {
				if (! iter.hasNext())
					throw new IllegalStateException("Array stream is exhausted.");
				return iter.next();
			}
		};
	}

	/** Selects the records to yield from a generically-parsed XML document, honoring the root-element hint. */
	private static List<Object> selectRootElements(Object parsed, String rootElementName) {
		if (parsed == null)
			return Collections.emptyList();
		if (parsed instanceof Map<?,?> parsed2) {
			var v = parsed2.get(rootElementName);
			if (v == null)
				return Collections.emptyList();
			if (v instanceof List<?> v2)
				return new ArrayList<>(v2);
			return new ArrayList<>(Collections.singletonList(v));
		}
		if (parsed instanceof List<?> parsed2)
			return new ArrayList<>(parsed2);
		return new ArrayList<>(Collections.singletonList(parsed));
	}

	/**
	 * The XML record cursor is buffered/{@link RecordAdapter}-backed, not O(1) streaming.
	 *
	 * @return Always <jk>false</jk>.
	 */
	@Override /* RecordReadable */
	public boolean isRecordStreaming() { return false; }

	/**
	 * The XML array-record cursor is buffered/{@link RecordAdapter}-backed, not O(1) streaming.
	 *
	 * @return Always <jk>false</jk>.
	 */
	@Override /* ArrayRecordReadable */
	public boolean isArrayRecordStreaming() { return false; }

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
	 * 	<br>Must not be <jk>null</jk>.
	 * @return The decoded text.  <jk>null</jk> if the text consists of the sequence <js>'_x0000_'</js>.
	 * @throws XMLStreamException Thrown by underlying reader.
	 * @throws IOException Thrown by underlying stream.
	 * @throws ParseException Malformed input encountered.
	 */
	protected String getElementText(XmlReader r) throws XMLStreamException, IOException, ParseException {
		return decodeString(r.getElementText().trim());
	}

	/**
	 * XML event allocator.
	 *
	 * @see XmlParser.Builder#eventAllocator(Class)
	 * @return
	 * 	The {@link XMLEventAllocator} associated with this parser, or <jk>null</jk> if there isn't one.
	 */
	protected final XMLEventAllocator getEventAllocator() { return ctx.getEventAllocator(); }

	/**
	 * XML reporter.
	 *
	 * @see XmlParser.Builder#reporter(Class)
	 * @return
	 * 	The {@link XMLReporter} associated with this parser, or <jk>null</jk> if there isn't one.
	 */
	protected final XMLReporter getReporter() { return ctx.getReporter(); }

	/**
	 * XML resolver.
	 *
	 * @see XmlParser.Builder#resolver(Class)
	 * @return
	 * 	The {@link XMLResolver} associated with this parser, or <jk>null</jk> if there isn't one.
	 */
	protected final XMLResolver getResolver() { return ctx.getResolver(); }

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
	 * Wrap the specified reader in a STAX reader based on settings in this context.
	 *
	 * @param pipe The parser input.
	 * 	<br>Must not be <jk>null</jk>.
	 * @return The new STAX reader.
	 * @throws IOException Thrown by underlying stream.
	 * @throws XMLStreamException Unexpected XML processing error.
	 */
	protected final XmlReader getXmlReader(ParserPipe pipe) throws IOException, XMLStreamException {
		return new XmlReader(pipe, isValidating(), getReporter(), getResolver(), getEventAllocator());
	}

	/**
	 * Preserve root element during generalized parsing.
	 *
	 * @see XmlParser.Builder#preserveRootElement()
	 * @return
	 * 	<jk>true</jk> if when parsing into a generic {@link JsonMap}, the map will contain a single entry whose key
	 * 	is the root element name.
	 */
	protected final boolean isPreserveRootElement() { return preserveRootElement; }

	/**
	 * Enable validation.
	 *
	 * @see XmlParser.Builder#validating()
	 * @return
	 * 	<jk>true</jk> if XML document will be validated.
	 */
	protected final boolean isValidating() { return validating; }

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
	 * Workhorse method.
	 *
	 * @param <T> The expected type of object.
	 * @param eType The expected type of object.
	 * 	<br>Can be <jk>null</jk>, in which case the generic {@link Object} type is used and the actual type is auto-detected from the XML.
	 * @param currAttr The current bean property name.
	 * 	<br>Can be <jk>null</jk> if not being parsed as a named bean property (e.g. the root value).
	 * @param r The reader.
	 * 	<br>Must not be <jk>null</jk>.
	 * @param outer The outer object.
	 * 	<br>Can be <jk>null</jk> if there is no enclosing (outer) object; the parsed object is then not linked to a parent.
	 * @param isRoot If <jk>true</jk>, then we're serializing a root element in the document.
	 * @param pMeta The bean property metadata.
	 * 	<br>Can be <jk>null</jk> if no bean property metadata applies (e.g. the root value or an unknown property).
	 * @return The parsed object, or <jk>null</jk> if the element represents a <jk>null</jk> value.
	 * @throws IOException Thrown by underlying stream.
	 * @throws ParseException Malformed input encountered.
	 * @throws ExecutableException Exception occurred on invoked constructor/method/field.
	 * @throws XMLStreamException Malformed XML encountered.
	 */
	@SuppressWarnings({
		"null",        // Null handling verified by context or framework
		"java:S3776",  // Cognitive complexity acceptable for this specific logic
		"java:S6541",  // Brain Method — complex XML parsing logic; structural refactoring would reduce readability without meaningful benefit.
	})
	protected <T> T readAnything(ClassMeta<T> eType, String currAttr, XmlReader r, Object outer, boolean isRoot, BeanPropertyMeta pMeta)
		throws IOException, ParseException, ExecutableException, XMLStreamException {

		if (eType == null)
			eType = (ClassMeta<T>)object();
		var swap = (ObjectSwap<T,Object>)eType.getSwap(this);
		var builder = (BuilderSwap<T,Object>)eType.getBuilderSwap(this);
		ClassMeta<?> sType = null;
		if (nn(builder))
			sType = builder.getBuilderClassMeta(this);
		else if (nn(swap))
			sType = swap.getSwapClassMeta(this);
		else
			sType = eType;

		if (sType.isOptional())
			return (T)o(readAnything(eType.getElementType(), currAttr, r, outer, isRoot, pMeta));

		setCurrentClass(sType);

		var wrapperAttr = (isRoot && isPreserveRootElement()) ? r.getName().getLocalPart() : null;
		var typeAttr = r.getAttributeValue(null, getBeanTypePropertyName(eType));
		var isNil = "true".equals(r.getAttributeValue(null, "nil"));
		var jsonType = getJsonType(typeAttr);
		var elementName = getElementName(r);
		if (jsonType == 0) {
			if (elementName == null || elementName.equals(currAttr))
				jsonType = UNKNOWN;
			else {
				typeAttr = elementName;
				jsonType = getJsonType(elementName);
			}
		}

		ClassMeta tcm = getClassMeta(typeAttr, pMeta, eType);
		if (tcm == null && nn(elementName) && ! elementName.equals(currAttr))
			tcm = getClassMeta(elementName, pMeta, eType);
		if (nn(tcm))
			sType = eType = tcm;

		Object o = null;

		if (jsonType == NULL) {
			r.nextTag();	// Discard end tag
			return null;
		}

		if (sType.isObject()) {
			if (jsonType == OBJECT) {
				var m = newGenericMap();
				readIntoMap(r, m, string(), object(), pMeta);
				if (nn(wrapperAttr))
					m = newGenericMap().append(wrapperAttr, m);
				o = cast(m, pMeta, eType);
			} else if (jsonType == ARRAY)
				o = readIntoCollection(r, newGenericList(), null, pMeta);
			else if (jsonType == STRING) {
				o = getElementText(r);
				if (sType.isChar())
					o = parseCharacter(o);
			} else if (jsonType == NUMBER)
				o = parseNumber(getElementText(r), null);
			else if (jsonType == BOOLEAN)
				o = Boolean.parseBoolean(getElementText(r));
			else if (jsonType == UNKNOWN)
				o = getUnknown(r);
		} else if (nn(builder) || sType.canCreateNewBean(outer)) {
			if (getXmlClassMeta(sType).getFormat() == COLLAPSED) {
				var fieldName = r.getLocalName();
				var m = nn(builder) ? toBeanMap(builder.create(this, eType)) : newBeanMap(outer, sType.inner());
				var bpm = getXmlBeanMeta(m.getMeta()).getPropertyMeta(fieldName);
				var cm = (ClassMeta<?>) m.getMeta().getBeanInfo();
				Object value = readAnything(cm, currAttr, r, m.getBean(false), false, null);
				setName(cm, value, currAttr);
				bpm.set(m, currAttr, value);
				o = nn(builder) ? builder.build(this, m.getBean(), eType) : m.getBean();
			} else {
				var m = nn(builder) ? toBeanMap(builder.create(this, eType)) : newBeanMap(outer, sType.inner());
				m = readIntoBean(r, m, isNil);
				o = nn(builder) ? builder.build(this, m.getBean(), eType) : m.getBean();
			}
		} else if (sType.isMap()) {
			var m = (sType.canCreateNewInstance(outer) ? (Map)sType.newInstance(outer) : newGenericMap(sType));
			o = readIntoMap(r, m, sType.getKeyType(), sType.getValueType(), pMeta);
			if (nn(wrapperAttr)) {
				var wrapper = (sType.canCreateNewInstance(outer) ? (Map)sType.newInstance(outer) : newGenericMap());
				wrapper.put(wrapperAttr, m);
				o = wrapper;
			}
		} else if (sType.isCollection()) {
			var l = (sType.canCreateNewInstance(outer) ? (Collection)sType.newInstance(outer) : newGenericList());
			o = readIntoCollection(r, l, sType, pMeta);
		} else if (sType.isArray() || sType.isArgs()) {
			var l = (ArrayList)readIntoCollection(r, list(), sType, pMeta);
			o = toArray(sType, l);
		} else if (sType.isCharSequence()) {
			o = getElementText(r);
		} else if (sType.isChar()) {
			o = parseCharacter(getElementText(r));
		} else if (sType.isNumber()) {
			o = parseNumber(getElementText(r), (Class<? extends Number>)sType.inner());
		} else if (sType.isBoolean()) {
			o = Boolean.parseBoolean(getElementText(r));
		} else if (sType.isDate()) {
			o = readDate(getElementText(r), sType);
		} else if (sType.isCalendar()) {
			o = readCalendar(getElementText(r), sType);
		} else if (sType.isTemporal()) {
			o = readTemporal(getElementText(r), sType);
		} else if (sType.isDuration()) {
			o = readDuration(getElementText(r));
		} else if (sType.isPeriod()) {
			o = readPeriod(getElementText(r));
		} else if (sType.canCreateNewInstanceFromString(outer)) {
			o = sType.newInstanceFromString(outer, getElementText(r));
		} else if (nn(sType.getProxyInvocationHandler())) {
			var m = newGenericMap();
			readIntoMap(r, m, string(), object(), pMeta);
			if (nn(wrapperAttr))
				m = newGenericMap().append(wrapperAttr, m);
			o = newBeanMap(outer, sType.inner()).load(m).getBean();
		} else {
			throw new ParseException(this, "Class '%s' could not be instantiated.  Reason: '%s', property: '%s'", cn(sType), sType.getNotABeanReason(),
				pMeta == null ? null : pMeta.getName());
		}

		if (nn(swap) && nn(o))
			o = unswap(swap, o, eType);

		if (nn(outer))
			setParent(eType, o, outer);

		return (T)o;
	}

	/**
	 * Parses the current element as text.
	 *
	 * @param r The input reader.
	 * 	<br>Must not be <jk>null</jk>.
	 * @return The parsed text.
	 * @throws XMLStreamException Thrown by underlying reader.
	 * @throws IOException Thrown by underlying stream.
	 * @throws ParseException Malformed input encountered.
	 */
	protected String readText(XmlReader r) throws IOException, XMLStreamException, ParseException {
		// Note that this is different than {@link #getText(XmlReader)} since it assumes that we're pointing to a
		// whitespace element.

		var sb2 = getStringBuilder();

		int depth = 0;
		while (true) {
			var et = r.getEventType();
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
			r.next();
		}
		var s = sb2.toString();
		returnStringBuilder(sb2);
		return s;
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
	protected String readWhitespaceElement(XmlReader r) throws IOException, XMLStreamException, ParseException {
		return null;
	}
}
