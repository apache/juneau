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

import java.io.*;
import java.lang.reflect.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;

import javax.xml.*;
import javax.xml.transform.stream.*;
import javax.xml.validation.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.http.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.xml.annotation.*;
import org.w3c.dom.bootstrap.*;
import org.w3c.dom.ls.*;

/**
 * Serializes POJO metadata to HTTP responses as XML.
 *
 * <h5 class='section'>Media types:</h5>
 * <p>
 * Handles <code>Accept</code> types: <code>text/xml+schema</code>
 * <p>
 * Produces <code>Content-Type</code> types: <code>text/xml</code>
 *
 * <h5 class='section'>Description:</h5>
 * <p>
 * Produces the XML-schema representation of the XML produced by the {@link XmlSerializer} class with the same properties.
 *
 * <h5 class='section'>Configurable properties:</h5>
 * <p>
 * This class has the following properties associated with it:
 * <ul>
 * 	<li>{@link XmlSerializerContext}
 * 	<li>{@link BeanContext}
 * </ul>
 */
@Produces(value="text/xml+schema",contentType="text/xml")
public class XmlSchemaSerializer extends XmlSerializer {

	private final XmlSerializerContext ctx;

	/**
	 * Constructor.
	 * @param propertyStore Initialize with the specified config property store.
	 */
	public XmlSchemaSerializer(PropertyStore propertyStore) {
		this(propertyStore, null);
	}

	/**
	 * Constructor
	 * @param propertyStore The property store containing all the settings for this object.
	 * @param overrideProperties A set of overridden settings, typically defined by the class itself.
	 */
	public XmlSchemaSerializer(PropertyStore propertyStore, Map<String,Object> overrideProperties) {
		super(propertyStore);
		this.ctx = this.propertyStore.create(overrideProperties).getContext(XmlSerializerContext.class);
	}

	@Override /* XmlSerializer */
	protected void doSerialize(SerializerSession session, Object o) throws Exception {
		XmlSerializerSession s = (XmlSerializerSession)session;

		if (s.isEnableNamespaces() && s.isAutoDetectNamespaces())
			findNsfMappings(s, o);

		Namespace xs = s.getXsNamespace();
		Namespace[] allNs = ArrayUtils.append(new Namespace[]{s.getDefaultNamespace()}, s.getNamespaces());

		Schemas schemas = new Schemas(s, xs, s.getDefaultNamespace(), allNs);
		schemas.process(s, o);
		schemas.serializeTo(session.getWriter());
	}

	/**
	 * Returns an XML-Schema validator based on the output returned by {@link #doSerialize(SerializerSession, Object)};
	 *
	 * @param session The serializer session object return by {@link #createSession(Object, ObjectMap, Method, Locale, TimeZone, MediaType, UriContext)}.<br>
	 * Can be <jk>null</jk>.
	 * @param o The object to serialize.
	 * @return The new validator.
	 * @throws Exception If a problem was detected in the XML-Schema output produced by this serializer.
	 */
	public Validator getValidator(SerializerSession session, Object o) throws Exception {
		doSerialize(session, o);
		String xmlSchema = session.getWriter().toString();

		// create a SchemaFactory capable of understanding WXS schemas
		SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

		if (xmlSchema.indexOf('\u0000') != -1) {

			// Break it up into a map of namespaceURI->schema document
			final Map<String,String> schemas = new HashMap<String,String>();
			String[] ss = xmlSchema.split("\u0000");
			xmlSchema = ss[0];
			for (String s : ss) {
				Matcher m = pTargetNs.matcher(s);
				if (m.find())
					schemas.put(m.group(1), s);
			}

			// Create a custom resolver
			factory.setResourceResolver(
				new LSResourceResolver() {

					@Override /* LSResourceResolver */
					public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {

						String schema = schemas.get(namespaceURI);
						if (schema == null)
							throw new RuntimeException(MessageFormat.format("No schema found for namespaceURI ''{0}''", namespaceURI));

						try {
							DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
							DOMImplementationLS domImplementationLS = (DOMImplementationLS)registry.getDOMImplementation("LS 3.0");
							LSInput in = domImplementationLS.createLSInput();
							in.setCharacterStream(new StringReader(schema));
							in.setSystemId(systemId);
							return in;

						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
				}
			);
		}
		return factory.newSchema(new StreamSource(new StringReader(xmlSchema))).newValidator();
	}

	private static Pattern pTargetNs = Pattern.compile("targetNamespace=['\"]([^'\"]+)['\"]");


	/* An instance of a global element, global attribute, or XML type to be serialized. */
	private static class QueueEntry {
		Namespace ns;
		String name;
		ClassMeta<?> cm;
		QueueEntry(Namespace ns, String name, ClassMeta<?> cm) {
			this.ns = ns;
			this.name = name;
			this.cm = cm;
		}
	}

	/* An encapsulation of all schemas present in the metamodel of the serialized object. */
	private class Schemas extends LinkedHashMap<Namespace,Schema> {

		private static final long serialVersionUID = 1L;

		private Namespace defaultNs;
		private LinkedList<QueueEntry>
			elementQueue = new LinkedList<QueueEntry>(),
			attributeQueue = new LinkedList<QueueEntry>(),
			typeQueue = new LinkedList<QueueEntry>();

		private Schemas(XmlSerializerSession session, Namespace xs, Namespace defaultNs, Namespace[] allNs) throws IOException {
			this.defaultNs = defaultNs;
			for (Namespace ns : allNs)
				put(ns, new Schema(session, this, xs, ns, defaultNs, allNs));
		}

		private Schema getSchema(Namespace ns) {
			if (ns == null)
				ns = defaultNs;
			Schema s = get(ns);
			if (s == null)
				throw new RuntimeException("No schema defined for namespace '"+ns+"'");
			return s;
		}

		private void process(SerializerSession session, Object o) throws IOException {
			ClassMeta<?> cm = session.getClassMetaForObject(o);
			Namespace ns = defaultNs;
			if (cm == null)
				queueElement(ns, "null", object());
			else {
				XmlClassMeta xmlMeta = cm.getExtendedMeta(XmlClassMeta.class);
				if (cm.getDictionaryName() != null && xmlMeta.getNamespace() != null)
					ns = xmlMeta.getNamespace();
				queueElement(ns, cm.getDictionaryName(), cm);
			}
			processQueue();
		}


		private void processQueue() throws IOException {
			boolean b;
			do {
				b = false;
				while (! elementQueue.isEmpty()) {
					QueueEntry q = elementQueue.removeFirst();
					b |= getSchema(q.ns).processElement(q.name, q.cm);
				}
				while (! typeQueue.isEmpty()) {
					QueueEntry q = typeQueue.removeFirst();
					b |= getSchema(q.ns).processType(q.name, q.cm);
				}
				while (! attributeQueue.isEmpty()) {
					QueueEntry q = attributeQueue.removeFirst();
					b |= getSchema(q.ns).processAttribute(q.name, q.cm);
				}
			} while (b);
		}

		private void queueElement(Namespace ns, String name, ClassMeta<?> cm) {
			elementQueue.add(new QueueEntry(ns, name, cm));
		}

		private void queueType(Namespace ns, String name, ClassMeta<?> cm) {
			if (name == null)
				name = XmlUtils.encodeElementName(cm);
			typeQueue.add(new QueueEntry(ns, name, cm));
		}

		private void queueAttribute(Namespace ns, String name, ClassMeta<?> cm) {
			attributeQueue.add(new QueueEntry(ns, name, cm));
		}

		private void serializeTo(Writer w) throws IOException {
			boolean b = false;
			for (Schema s : values()) {
				if (b)
					w.append('\u0000');
				w.append(s.toString());
				b = true;
			}
		}
	}

	/* An encapsulation of a single schema. */
	private class Schema {
		private StringWriter sw = new StringWriter();
		private XmlWriter w;
		private XmlSerializerSession session;
		private Namespace defaultNs, targetNs;
		private Schemas schemas;
		private Set<String>
			processedTypes = new HashSet<String>(),
			processedAttributes = new HashSet<String>(),
			processedElements = new HashSet<String>();

		public Schema(XmlSerializerSession session, Schemas schemas, Namespace xs, Namespace targetNs, Namespace defaultNs, Namespace[] allNs) throws IOException {
			this.schemas = schemas;
			this.defaultNs = defaultNs;
			this.targetNs = targetNs;
			this.session = session;
			w = new XmlWriter(sw, session.isUseWhitespace(), session.isTrimStrings(), session.getQuoteChar(), null, null, null, true, null);
			int i = session.getIndent();
			w.oTag(i, "schema");
			w.attr("xmlns", xs.getUri());
			w.attr("targetNamespace", targetNs.getUri());
			w.attr("elementFormDefault", "qualified");
			if (targetNs != defaultNs)
				w.attr("attributeFormDefault", "qualified");
			for (Namespace ns2 : allNs)
				w.attr("xmlns", ns2.name, ns2.uri);
			w.append('>').nl();
			for (Namespace ns : allNs) {
				if (ns != targetNs) {
					w.oTag(i+1, "import")
						.attr("namespace", ns.getUri())
						.attr("schemaLocation", ns.getName()+".xsd")
						.append("/>").nl();
				}
			}
		}

		private boolean processElement(String name, ClassMeta<?> cm) throws IOException {
			if (processedElements.contains(name))
				return false;
			processedElements.add(name);

			ClassMeta<?> ft = cm.getSerializedClassMeta();
			int i = session.getIndent() + 1;
			if (name == null)
				name = getElementName(ft);
			Namespace ns = first(ft.getExtendedMeta(XmlClassMeta.class).getNamespace(), defaultNs);
			String type = getXmlType(ns, ft);

			w.oTag(i, "element")
				.attr("name", XmlUtils.encodeElementName(name))
				.attr("type", type)
				.append('/').append('>').nl();

			schemas.queueType(ns, null, ft);
			schemas.processQueue();
			return true;
		}

		private boolean processAttribute(String name, ClassMeta<?> cm) throws IOException {
			if (processedAttributes.contains(name))
				return false;
			processedAttributes.add(name);

			int i = session.getIndent() + 1;
			String type = getXmlAttrType(cm);

			w.oTag(i, "attribute")
				.attr("name", name)
				.attr("type", type)
				.append('/').append('>').nl();

			return true;
		}

		private boolean processType(String name, ClassMeta<?> cm) throws IOException {
			if (processedTypes.contains(name))
				return false;
			processedTypes.add(name);

			int i = session.getIndent() + 1;

			cm = cm.getSerializedClassMeta();
			XmlBeanMeta xbm = cm.isBean() ? cm.getBeanMeta().getExtendedMeta(XmlBeanMeta.class) : null;

			w.oTag(i, "complexType")
				.attr("name", name);

			// This element can have mixed content if:
			// 	1) It's a generic Object (so it can theoretically be anything)
			//		2) The bean has a property defined with @XmlFormat.CONTENT.
			if ((xbm != null && (xbm.getContentFormat() != null && xbm.getContentFormat().isOneOf(TEXT,TEXT_PWS,MIXED,MIXED_PWS,XMLTEXT))) || ! cm.isMapOrBean())
				w.attr("mixed", "true");

			w.cTag().nl();

			if (! (cm.isMapOrBean() || cm.isCollectionOrArray() || (cm.isAbstract() && ! cm.isNumber()) || cm.isObject())) {
				w.oTag(i+1, "attribute").attr("name", session.getBeanTypePropertyName(cm)).attr("type", "string").ceTag().nl();

			} else {

				//----- Bean -----
				if (cm.isBean()) {
					BeanMeta<?> bm = cm.getBeanMeta();

					boolean hasChildElements = false;

					for (BeanPropertyMeta pMeta : bm.getPropertyMetas()) {
						XmlFormat pMetaFormat = pMeta.getExtendedMeta(XmlBeanPropertyMeta.class).getXmlFormat();
						if (pMetaFormat != XmlFormat.ATTR)
							hasChildElements = true;
					}

					XmlBeanMeta xbm2 = bm.getExtendedMeta(XmlBeanMeta.class);
					if (xbm2.getContentProperty() != null && xbm2.getContentFormat() == ELEMENTS) {
						w.sTag(i+1, "sequence").nl();
						w.oTag(i+2, "any")
							.attr("processContents", "skip")
							.attr("minOccurs", 0)
							.ceTag().nl();
						w.eTag(i+1, "sequence").nl();

					} else if (hasChildElements) {

						boolean hasOtherNsElement = false;
						boolean hasCollapsed = false;

						for (BeanPropertyMeta pMeta : bm.getPropertyMetas()) {
							XmlBeanPropertyMeta xmlMeta = pMeta.getExtendedMeta(XmlBeanPropertyMeta.class);
							if (xmlMeta.getXmlFormat() != ATTR) {
								if (xmlMeta.getNamespace() != null) {
									ClassMeta<?> ct2 = pMeta.getClassMeta();
									Namespace cNs = first(xmlMeta.getNamespace(), ct2.getExtendedMeta(XmlClassMeta.class).getNamespace(), cm.getExtendedMeta(XmlClassMeta.class).getNamespace(), defaultNs);
									// Child element is in another namespace.
									schemas.queueElement(cNs, pMeta.getName(), ct2);
									hasOtherNsElement = true;
								}
								if (xmlMeta.getXmlFormat() == COLLAPSED)
									hasCollapsed = true;
							}
						}

						if (hasOtherNsElement || hasCollapsed) {
							// If this bean has any child elements in another namespace,
							// we need to add an <any> element.
							w.oTag(i+1, "choice").attr("maxOccurs", "unbounded").cTag().nl();
							w.oTag(i+2, "any")
								.attr("processContents", "skip")
								.attr("minOccurs", 0)
								.ceTag().nl();
							w.eTag(i+1, "choice").nl();

						} else {
							w.sTag(i+1, "all").nl();
							for (BeanPropertyMeta pMeta : bm.getPropertyMetas()) {
								XmlBeanPropertyMeta xmlMeta = pMeta.getExtendedMeta(XmlBeanPropertyMeta.class);
								if (xmlMeta.getXmlFormat() != ATTR) {
									boolean isCollapsed = xmlMeta.getXmlFormat() == COLLAPSED;
									ClassMeta<?> ct2 = pMeta.getClassMeta();
									String childName = pMeta.getName();
									if (isCollapsed) {
										if (xmlMeta.getChildName() != null)
											childName = xmlMeta.getChildName();
										ct2 = pMeta.getClassMeta().getElementType();
									}
									Namespace cNs = first(xmlMeta.getNamespace(), ct2.getExtendedMeta(XmlClassMeta.class).getNamespace(), cm.getExtendedMeta(XmlClassMeta.class).getNamespace(), defaultNs);
									if (xmlMeta.getNamespace() == null) {
										w.oTag(i+2, "element")
											.attr("name", XmlUtils.encodeElementName(childName), true)
											.attr("type", getXmlType(cNs, ct2))
											.attr("minOccurs", 0);

										w.ceTag().nl();
									} else {
										// Child element is in another namespace.
										schemas.queueElement(cNs, pMeta.getName(), ct2);
										hasOtherNsElement = true;
									}

								}
							}
							w.eTag(i+1, "all").nl();
						}

					}

					for (BeanPropertyMeta pMeta : bm.getExtendedMeta(XmlBeanMeta.class).getAttrProperties().values()) {
						Namespace pNs = pMeta.getExtendedMeta(XmlBeanPropertyMeta.class).getNamespace();
						if (pNs == null)
							pNs = defaultNs;

						// If the bean attribute has a different namespace than the bean, then it needs to
						// be added as a top-level entry in the appropriate schema file.
						if (pNs != targetNs) {
							schemas.queueAttribute(pNs, pMeta.getName(), pMeta.getClassMeta());
							w.oTag(i+1, "attribute")
							//.attr("name", pMeta.getName(), true)
							.attr("ref", pNs.getName() + ':' + pMeta.getName())
							.ceTag().nl();
						}

						// Otherwise, it's just a plain attribute of this bean.
						else {
							w.oTag(i+1, "attribute")
								.attr("name", pMeta.getName(), true)
								.attr("type", getXmlAttrType(pMeta.getClassMeta()))
								.ceTag().nl();
						}
					}

				//----- Collection -----
				} else if (cm.isCollectionOrArray()) {
					ClassMeta<?> elementType = cm.getElementType();
					if (elementType.isObject()) {
						w.sTag(i+1, "sequence").nl();
						w.oTag(i+2, "any")
							.attr("processContents", "skip")
							.attr("maxOccurs", "unbounded")
							.attr("minOccurs", "0")
							.ceTag().nl();
						w.eTag(i+1, "sequence").nl();
					} else {
						Namespace cNs = first(elementType.getExtendedMeta(XmlClassMeta.class).getNamespace(), cm.getExtendedMeta(XmlClassMeta.class).getNamespace(), defaultNs);
						schemas.queueType(cNs, null, elementType);
						w.sTag(i+1, "sequence").nl();
						w.oTag(i+2, "any")
							.attr("processContents", "skip")
							.attr("maxOccurs", "unbounded")
							.attr("minOccurs", "0")
							.ceTag().nl();
						w.eTag(i+1, "sequence").nl();
					}

				//----- Map -----
				} else if (cm.isMap() || cm.isAbstract() || cm.isObject()) {
					w.sTag(i+1, "sequence").nl();
					w.oTag(i+2, "any")
						.attr("processContents", "skip")
						.attr("maxOccurs", "unbounded")
						.attr("minOccurs", "0")
						.ceTag().nl();
					w.eTag(i+1, "sequence").nl();
				}

				w.oTag(i+1, "attribute")
					.attr("name", session.getBeanTypePropertyName(null))
					.attr("type", "string")
					.ceTag().nl();
			}

			w.eTag(i, "complexType").nl();
			schemas.processQueue();

			return true;
		}

		private String getElementName(ClassMeta<?> cm) {
			cm = cm.getSerializedClassMeta();
			String name = cm.getDictionaryName();

			if (name == null) {
				if (cm.isBoolean())
					name = "boolean";
				else if (cm.isNumber())
					name = "number";
				else if (cm.isCollectionOrArray())
					name = "array";
				else if (! (cm.isMapOrBean() || cm.isCollectionOrArray() || cm.isObject() || cm.isAbstract()))
					name = "string";
				else
					name = "object";
			}
			return name;
		}

		@Override /* Object */
		public String toString() {
			try {
				w.eTag(session.getIndent(), "schema").nl();
			} catch (IOException e) {
				throw new RuntimeException(e); // Shouldn't happen.
			}
			return sw.toString();
		}

		private String getXmlType(Namespace currentNs, ClassMeta<?> cm) {
			String name = null;
			cm = cm.getSerializedClassMeta();
			if (currentNs == targetNs) {
				if (cm.isPrimitive()) {
					if (cm.isBoolean())
						name = "boolean";
					else if (cm.isNumber()) {
						if (cm.isDecimal())
							name = "decimal";
						else
							name = "integer";
					}
				}
			}
			if (name == null) {
				name = XmlUtils.encodeElementName(cm);
				schemas.queueType(currentNs, name, cm);
				return currentNs.getName() + ":" + name;
			}

			return name;
		}
	}

	private static <T> T first(T...tt) {
		for (T t : tt)
			if (t != null)
				return t;
		return null;
	}


	private static String getXmlAttrType(ClassMeta<?> cm) {
		if (cm.isBoolean())
			return "boolean";
		if (cm.isNumber()) {
			if (cm.isDecimal())
				return "decimal";
			return "integer";
		}
		return "string";
	}

	@Override /* Serializer */
	public XmlSerializerSession createSession(Object output, ObjectMap op, Method javaMethod, Locale locale, TimeZone timeZone, MediaType mediaType, UriContext uriContext) {
		// This serializer must always have namespaces enabled.
		if (op == null)
			op = new ObjectMap();
		op.put(XmlSerializerContext.XML_enableNamespaces, true);
		return new XmlSerializerSession(ctx, op, output, javaMethod, locale, timeZone, mediaType, uriContext);
	}
}
