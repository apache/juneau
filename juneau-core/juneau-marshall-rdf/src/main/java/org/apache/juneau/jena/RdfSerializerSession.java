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
package org.apache.juneau.jena;

import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.IOUtils.*;
import static org.apache.juneau.jena.Constants.*;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;
import java.util.function.*;

import org.apache.jena.rdf.model.*;
import org.apache.juneau.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.jena.annotation.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.swap.*;
import org.apache.juneau.xml.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Session object that lives for the duration of a single use of {@link RdfSerializer}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'>{doc jmr.RdfDetails}
 * </ul>
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class RdfSerializerSession extends WriterSerializerSession {

	/**
	 * Maps RDF writer names to property prefixes that apply to them.
	 */
	static final Map<String,String> LANG_PROP_MAP = mapBuilder(String.class,String.class)
		.add("RDF/XML","rdfXml.").add("RDF/XML-ABBREV","rdfXml.").add("N3","n3.").add("N3-PP","n3.").add("N3-PLAIN","n3.").add("N3-TRIPLES","n3.").add("TURTLE","n3.").add("N-TRIPLE","ntriple.")
		.build();

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * @return A new builder.
	 */
	public static Builder create(RdfSerializer ctx) {
		return new Builder(ctx);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public static class Builder extends WriterSerializerSession.Builder {

		RdfSerializer ctx;

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 */
		protected Builder(RdfSerializer ctx) {
			super(ctx);
			this.ctx = ctx;
		}

		@Override
		public RdfSerializerSession build() {
			return new RdfSerializerSession(this);
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

		@Override /* GENERATED - org.apache.juneau.serializer.SerializerSession.Builder */
		public Builder javaMethod(Method value) {
			super.javaMethod(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.SerializerSession.Builder */
		public Builder resolver(VarResolverSession value) {
			super.resolver(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.SerializerSession.Builder */
		public Builder schema(HttpPartSchema value) {
			super.schema(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.SerializerSession.Builder */
		public Builder schemaDefault(HttpPartSchema value) {
			super.schemaDefault(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.SerializerSession.Builder */
		public Builder uriContext(UriContext value) {
			super.uriContext(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializerSession.Builder */
		public Builder fileCharset(Charset value) {
			super.fileCharset(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializerSession.Builder */
		public Builder streamCharset(Charset value) {
			super.streamCharset(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializerSession.Builder */
		public Builder useWhitespace(Boolean value) {
			super.useWhitespace(value);
			return this;
		}

		// </FluentSetters>
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final RdfSerializer ctx;
	private final Property pRoot, pValue;
	private final Model model;
	private final RDFWriter writer;
	private final Namespace[] namespaces;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected RdfSerializerSession(Builder builder) {
		super(builder);
		ctx = builder.ctx;

		namespaces = ctx.namespaces;
		model = ModelFactory.createDefaultModel();
		addModelPrefix(ctx.getJuneauNs());
		addModelPrefix(ctx.getJuneauBpNs());
		for (Namespace ns : this.namespaces)
			addModelPrefix(ns);
		pRoot = model.createProperty(ctx.getJuneauNs().getUri(), RDF_juneauNs_ROOT);
		pValue = model.createProperty(ctx.getJuneauNs().getUri(), RDF_juneauNs_VALUE);
		writer = model.getWriter(ctx.getLanguage());

		// Only apply properties with this prefix!
		String propPrefix = LANG_PROP_MAP.get(ctx.getLanguage());
		if (propPrefix == null)
			throw new BasicRuntimeException("Unknown RDF language encountered: ''{0}''", ctx.getLanguage());

		// RDF/XML specific properties.
		if (propPrefix.equals("rdfXml.")) {
			writer.setProperty("tab", isUseWhitespace() ? 2 : 0);
			writer.setProperty("attributeQuoteChar", Character.toString(getQuoteChar()));
		}

		ctx.getJenaSettings().forEach((k,v) -> {
			if (k.startsWith(propPrefix, 5))
				writer.setProperty(k.substring(5 + propPrefix.length()), v);
		});
	}

	/*
	 * Adds the specified namespace as a model prefix.
	 */
	private void addModelPrefix(Namespace ns) {
		model.setNsPrefix(ns.getName(), ns.getUri());
	}

	/*
	 * XML-encodes the specified string using the {@link XmlUtils#escapeText(Object)} method.
	 */
	private String encodeTextInvalidChars(Object o) {
		if (o == null)
			return null;
		String s = toString(o);
		return XmlUtils.escapeText(s);
	}

	/*
	 * XML-encoded the specified element name using the {@link XmlUtils#encodeElementName(Object)} method.
	 */
	private String encodeElementName(Object o) {
		return XmlUtils.encodeElementName(toString(o));
	}

	@Override /* Serializer */
	protected void doSerialize(SerializerPipe out, Object o) throws SerializeException {

		Resource r = null;

		ClassMeta<?> cm = getClassMetaForObject(o);
		if (isLooseCollections() && cm != null && cm.isCollectionOrArray()) {
			Collection c = cm.isCollection() ? (Collection)o : toList(cm.getInnerClass(), o);
			forEachEntry(c, x -> serializeAnything(x, false, object(), "root", null, null));
		} else {
			RDFNode n = serializeAnything(o, false, getExpectedRootType(o), "root", null, null);
			if (n.isLiteral()) {
				r = model.createResource();
				r.addProperty(pValue, n);
			} else {
				r = n.asResource();
			}

			if (isAddRootProp())
				r.addProperty(pRoot, "true");
		}

		writer.write(model, out.getWriter(), "http://unknown/");
	}

	private RDFNode serializeAnything(Object o, boolean isURI, ClassMeta<?> eType, String attrName, BeanPropertyMeta bpm, Resource parentResource) throws SerializeException {
		Model m = model;

		ClassMeta<?> aType = null;       // The actual type
		ClassMeta<?> wType = null;       // The wrapped type
		ClassMeta<?> sType = object();   // The serialized type

		aType = push2(attrName, o, eType);

		if (eType == null)
			eType = object();

		// Handle recursion
		if (aType == null) {
			o = null;
			aType = object();
		}

		// Handle Optional<X>
		if (isOptional(aType)) {
			o = getOptionalValue(o);
			eType = getOptionalType(eType);
			aType = getClassMetaForObject(o, object());
		}

		if (o != null) {

			if (aType.isDelegate()) {
				wType = aType;
				aType = ((Delegate)o).getClassMeta();
			}

			sType = aType;

			// Swap if necessary
			ObjectSwap swap = aType.getSwap(this);
			if (swap != null) {
				o = swap(swap, o);
				sType = swap.getSwapClassMeta(this);

				// If the getSwapClass() method returns Object, we need to figure out
				// the actual type now.
				if (sType.isObject())
					sType = getClassMetaForObject(o);
			}
		} else {
			sType = eType.getSerializedClassMeta(this);
		}

		String typeName = getBeanTypeName(this, eType, aType, bpm);

		RDFNode n = null;

		if (o == null || sType.isChar() && ((Character)o).charValue() == 0) {
			if (bpm != null) {
				if (isKeepNullProperties()) {
					n = m.createResource(RDF_NIL);
				}
			} else {
				n = m.createResource(RDF_NIL);
			}

		} else if (sType.isUri() || isURI) {
			// Note that RDF URIs must be absolute to be valid!
			String uri = getUri(o, null);
			if (StringUtils.isAbsoluteUri(uri))
				n = m.createResource(uri);
			else
				n = m.createLiteral(encodeTextInvalidChars(uri));

		} else if (sType.isCharSequence() || sType.isChar()) {
			n = m.createLiteral(encodeTextInvalidChars(o));

		} else if (sType.isNumber() || sType.isBoolean()) {
			if (! isAddLiteralTypes())
				n = m.createLiteral(o.toString());
			else
				n = m.createTypedLiteral(o);

		} else if (sType.isMap() || (wType != null && wType.isMap())) {
			if (o instanceof BeanMap) {
				BeanMap bm = (BeanMap)o;
				Object uri = null;
				RdfBeanMeta rbm = getRdfBeanMeta(bm.getMeta());
				if (rbm.hasBeanUri())
					uri = rbm.getBeanUriProperty().get(bm, null);
				String uri2 = getUri(uri, null);
				n = m.createResource(uri2);
				serializeBeanMap(bm, (Resource)n, typeName);
			} else {
				Map m2 = (Map)o;
				n = m.createResource();
				serializeMap(m2, (Resource)n, sType);
			}

		} else if (sType.isBean()) {
			BeanMap bm = toBeanMap(o);
			Object uri = null;
			RdfBeanMeta rbm = getRdfBeanMeta(bm.getMeta());
			if (rbm.hasBeanUri())
				uri = rbm.getBeanUriProperty().get(bm, null);
			String uri2 = getUri(uri, null);
			n = m.createResource(uri2);
			serializeBeanMap(bm, (Resource)n, typeName);

		} else if (sType.isCollectionOrArray() || (wType != null && wType.isCollection())) {

			Collection c = sort(sType.isCollection() ? (Collection)o : toList(sType.getInnerClass(), o));
			RdfCollectionFormat f = getCollectionFormat();
			RdfClassMeta cRdf = getRdfClassMeta(sType);
			RdfBeanPropertyMeta bpRdf = getRdfBeanPropertyMeta(bpm);

			if (cRdf.getCollectionFormat() != RdfCollectionFormat.DEFAULT)
				f = cRdf.getCollectionFormat();
			if (bpRdf.getCollectionFormat() != RdfCollectionFormat.DEFAULT)
				f = bpRdf.getCollectionFormat();

			switch (f) {
				case BAG: n = serializeToContainer(c, eType, m.createBag()); break;
				case LIST: n = serializeToList(c, eType); break;
				case MULTI_VALUED: serializeToMultiProperties(c, eType, bpm, attrName, parentResource); break;
				default: n = serializeToContainer(c, eType, m.createSeq());
			}

		} else if (sType.isReader()) {
			n = m.createLiteral(encodeTextInvalidChars(read((Reader)o, SerializerSession::handleThrown)));
		} else if (sType.isInputStream()) {
			n = m.createLiteral(encodeTextInvalidChars(read((InputStream)o, SerializerSession::handleThrown)));

		} else {
			n = m.createLiteral(encodeTextInvalidChars(toString(o)));
		}

		pop();

		return n;
	}

	private String getUri(Object uri, Object uri2) {
		String s = null;
		if (uri != null)
			s = uri.toString();
		if ((s == null || s.isEmpty()) && uri2 != null)
			s = uri2.toString();
		if (s == null)
			return null;
		return getUriResolver().resolve(s);
	}

	private void serializeMap(Map m, Resource r, ClassMeta<?> type) throws SerializeException {

		m = sort(m);

		ClassMeta<?> keyType = type.getKeyType(), valueType = type.getValueType();

		ArrayList<Map.Entry<Object,Object>> l = listFrom(m.entrySet());
		Collections.reverse(l);
		l.forEach(x -> {
			Object value = x.getValue();
			Object key = generalize(x.getKey(), keyType);
			Namespace ns = getJuneauBpNs();
			Property p = model.createProperty(ns.getUri(), encodeElementName(toString(key)));
			RDFNode n = serializeAnything(value, false, valueType, toString(key), null, r);
			if (n != null)
				r.addProperty(p, n);
		});
	}

	private void serializeBeanMap(BeanMap<?> m, Resource r, String typeName) throws SerializeException {
		List<BeanPropertyValue> l = new ArrayList<>();

		if (typeName != null) {
			BeanPropertyMeta pm = m.getMeta().getTypeProperty();
			l.add(new BeanPropertyValue(pm, pm.getName(), typeName, null));
		}

		Predicate<Object> checkNull = x -> isKeepNullProperties() || x != null;
		m.forEachValue(checkNull, (pMeta,key,value,thrown) -> {
			l.add(new BeanPropertyValue(pMeta, key, value, thrown));
		});

		Collections.reverse(l);
		l.forEach(x -> {
			BeanPropertyMeta bpMeta = x.getMeta();
			ClassMeta<?> cMeta = bpMeta.getClassMeta();
			RdfBeanPropertyMeta bpRdf = getRdfBeanPropertyMeta(bpMeta);
			XmlBeanPropertyMeta bpXml = getXmlBeanPropertyMeta(bpMeta);

			if (bpRdf.isBeanUri())
				return;

			String key = x.getName();
			Object value = x.getValue();
			Throwable t = x.getThrown();
			if (t != null)
				onBeanGetterException(bpMeta, t);

			if (canIgnoreValue(cMeta, key, value))
				return;

			Namespace ns = bpRdf.getNamespace();
			if (ns == null && isUseXmlNamespaces())
				ns = bpXml.getNamespace();
			if (ns == null)
				ns = getJuneauBpNs();
			else if (isAutoDetectNamespaces())
				addModelPrefix(ns);

			Property p = model.createProperty(ns.getUri(), encodeElementName(key));
			RDFNode n = serializeAnything(value, bpMeta.isUri(), cMeta, key, bpMeta, r);
			if (n != null)
				r.addProperty(p, n);
		});
	}


	private Container serializeToContainer(Collection c, ClassMeta<?> type, Container list) throws SerializeException {
		ClassMeta<?> elementType = type.getElementType();
		c.forEach(x -> list.add(serializeAnything(x, false, elementType, null, null, null)));
		return list;
	}

	private RDFList serializeToList(Collection c, ClassMeta<?> type) throws SerializeException {
		ClassMeta<?> elementType = type.getElementType();
		List<RDFNode> l = list(c.size());
		c.forEach(x -> l.add(serializeAnything(x, false, elementType, null, null, null)));
		return model.createList(l.iterator());
	}

	private void serializeToMultiProperties(Collection c, ClassMeta<?> sType,
			BeanPropertyMeta bpm, String attrName, Resource parentResource) throws SerializeException {

		ClassMeta<?> elementType = sType.getElementType();
		RdfBeanPropertyMeta bpRdf = getRdfBeanPropertyMeta(bpm);
		XmlBeanPropertyMeta bpXml = getXmlBeanPropertyMeta(bpm);

		c.forEach(x -> {
			Namespace ns = bpRdf.getNamespace();
			if (ns == null && isUseXmlNamespaces())
				ns = bpXml.getNamespace();
			if (ns == null)
				ns = getJuneauBpNs();
			else if (isAutoDetectNamespaces())
				addModelPrefix(ns);
			RDFNode n2 = serializeAnything(x, false, elementType, null, null, null);
			Property p = model.createProperty(ns.getUri(), encodeElementName(attrName));
			parentResource.addProperty(p, n2);
		});
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Common properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * RDF format for representing collections and arrays.
	 *
	 * @see RdfSerializer.Builder#collectionFormat(RdfCollectionFormat)
	 * @return
	 * 	RDF format for representing collections and arrays.
	 */
	protected final RdfCollectionFormat getCollectionFormat() {
		return ctx.getCollectionFormat();
	}

	/**
	 * Default XML namespace for bean properties.
	 *
	 * @see RdfSerializer.Builder#juneauBpNs(Namespace)
	 * @return
	 * 	The XML namespace to use for bean properties.
	 */
	protected final Namespace getJuneauBpNs() {
		return ctx.getJuneauBpNs();
	}

	/**
	 * XML namespace for Juneau properties.
	 *
	 * @see RdfSerializer.Builder#juneauNs(Namespace)
	 * @return
	 * 	The XML namespace to use for Juneau properties.
	 */
	protected final Namespace getJuneauNs() {
		return ctx.getJuneauNs();
	}

	/**
	 * RDF language.
	 *
	 * @see RdfSerializer.Builder#language(String)
	 * @return
	 * 	The RDF language to use.
	 */
	protected final String getLanguage() {
		return ctx.getLanguage();
	}

	/**
	 * Collections should be serialized and parsed as loose collections.
	 *
	 * @see RdfSerializer.Builder#looseCollections()
	 * @return
	 * 	<jk>true</jk> if collections of resources are handled as loose collections of resources in RDF instead of
	 * 	resources that are children of an RDF collection (e.g. Sequence, Bag).
	 */
	protected final boolean isLooseCollections() {
		return ctx.isLooseCollections();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Jena properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * All Jena-related configuration properties.
	 *
	 * @return
	 * 	A map of all Jena-related configuration properties.
	 */
	protected final Map<String,Object> getJenaSettings() {
		return ctx.getJenaSettings();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Add <js>"_type"</js> properties when needed.
	 *
	 * @see RdfSerializer.Builder#addBeanTypes()
	 * @return
	 * 	<jk>true</jk> if <js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * 	through reflection.
	 */
	@Override
	protected final boolean isAddBeanTypes() {
		return ctx.isAddBeanTypes();
	}

	/**
	 * Add XSI data types to non-<c>String</c> literals.
	 *
	 * @see RdfSerializer.Builder#addLiteralTypes()
	 * @return
	 * 	<jk>true</jk> if XSI data types should be added to string literals.
	 */
	protected final boolean isAddLiteralTypes() {
		return ctx.isAddLiteralTypes();
	}

	/**
	 * Add RDF root identifier property to root node.
	 *
	 * @see RdfSerializer.Builder#addRootProperty()
	 * @return
	 * 	<jk>true</jk> if RDF property <c>http://www.apache.org/juneau/root</c> is added with a value of <js>"true"</js>
	 * 	to identify the root node in the graph.
	 */
	protected final boolean isAddRootProp() {
		return ctx.isAddRootProp();
	}

	/**
	 * Auto-detect namespace usage.
	 *
	 * @see RdfSerializer.Builder#disableAutoDetectNamespaces()
	 * @return
	 * 	<jk>true</jk> if namespaces usage should be detected before serialization.
	 */
	protected final boolean isAutoDetectNamespaces() {
		return ctx.isAutoDetectNamespaces();
	}

	/**
	 * Default namespaces.
	 *
	 * @see RdfSerializer.Builder#namespaces(Namespace...)
	 * @return
	 * 	The default list of namespaces associated with this serializer.
	 */
	protected final Namespace[] getNamespaces() {
		return ctx.getNamespaces();
	}

	/**
	 * Reuse XML namespaces when RDF namespaces not specified.
	 *
	 * @see RdfSerializer.Builder#disableUseXmlNamespaces()
	 * @return
	 * 	<jk>true</jk> if namespaces defined using {@link XmlNs @XmlNs} and {@link org.apache.juneau.xml.annotation.Xml @Xml} will be inherited by the RDF serializers.
	 * 	<br>Otherwise, namespaces will be defined using {@link RdfNs @RdfNs} and {@link Rdf @Rdf}.
	 */
	protected final boolean isUseXmlNamespaces() {
		return ctx.isUseXmlNamespaces();
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
	protected RdfClassMeta getRdfClassMeta(ClassMeta<?> cm) {
		return ctx.getRdfClassMeta(cm);
	}

	/**
	 * Returns the language-specific metadata on the specified bean.
	 *
	 * @param bm The bean to return the metadata on.
	 * @return The metadata.
	 */
	protected RdfBeanMeta getRdfBeanMeta(BeanMeta<?> bm) {
		return ctx.getRdfBeanMeta(bm);
	}

	/**
	 * Returns the language-specific metadata on the specified bean property.
	 *
	 * @param bpm The bean property to return the metadata on.
	 * @return The metadata.
	 */
	protected RdfBeanPropertyMeta getRdfBeanPropertyMeta(BeanPropertyMeta bpm) {
		return bpm == null ? RdfBeanPropertyMeta.DEFAULT : ctx.getRdfBeanPropertyMeta(bpm);
	}

	/**
	 * Returns the language-specific metadata on the specified bean property.
	 *
	 * @param bpm The bean property to return the metadata on.
	 * @return The metadata.
	 */
	protected XmlBeanPropertyMeta getXmlBeanPropertyMeta(BeanPropertyMeta bpm) {
		return bpm == null ? XmlBeanPropertyMeta.DEFAULT : ctx.getXmlBeanPropertyMeta(bpm);
	}
}
