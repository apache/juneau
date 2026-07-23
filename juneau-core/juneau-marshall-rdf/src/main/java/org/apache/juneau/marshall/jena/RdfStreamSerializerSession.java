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
package org.apache.juneau.marshall.jena;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.IoUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.marshall.jena.Constants.*;

import java.io.*;
import java.util.*;
import java.util.function.*;

import org.apache.jena.datatypes.xsd.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.*;
import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.http.*;
import org.apache.juneau.commons.svl.*;
import org.apache.juneau.commons.utils.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.httppart.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.xml.*;

/**
 * Session for stream-based RDF serialization (binary formats like RDF/THRIFT, RDF/PROTO).
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'>{doc jmr.RdfDetails}
 * </ul>
 */
@SuppressWarnings({
	"rawtypes",
	"unchecked",
	"resource",
	"java:S110", // Inheritance depth acceptable for serializer session hierarchy
	"java:S115", // ARG_ prefix follows framework convention
	"java:S3776", // Cognitive complexity acceptable for RDF stream serialization
	"java:S6541"  // Brain method acceptable for RDF stream serialization dispatch logic
})
public class RdfStreamSerializerSession extends OutputStreamSerializerSession {

	private static final String ARG_ctx = "ctx";

	/**
	 * Builder class.
	 */
	public static class Builder extends OutputStreamSerializerSession.Builder<Builder> {

		private RdfStreamSerializer ctx;

		protected Builder(RdfStreamSerializer ctx) {
			super(assertArgNotNull(ARG_ctx, ctx));
			this.ctx = ctx;
		}

		@Override
		public <T> Builder apply(Class<T> type, Consumer<T> apply) {
			super.apply(type, apply);
			return this;
		}

		@Override
		public RdfStreamSerializerSession build() {
			return new RdfStreamSerializerSession(this);
		}

		@Override
		public Builder debug(Boolean value) {
			super.debug(value);
			return this;
		}

		@Override
		public Builder javaMethod(java.lang.reflect.Method value) {
			super.javaMethod(value);
			return this;
		}

		@Override
		public Builder locale(Locale value) {
			super.locale(value);
			return this;
		}

		@Override
		public Builder mediaType(MediaType value) {
			super.mediaType(value);
			return this;
		}

		@Override
		public Builder mediaTypeDefault(MediaType value) {
			super.mediaTypeDefault(value);
			return this;
		}

		@Override
		public Builder properties(Map<String,Object> value) {
			super.properties(value);
			return this;
		}

		@Override
		public Builder property(String key, Object value) {
			super.property(key, value);
			return this;
		}

		@Override
		public Builder resolver(VarResolverSession value) {
			super.resolver(value);
			return this;
		}

		@Override
		public Builder schema(HttpPartSchema value) {
			super.schema(value);
			return this;
		}

		@Override
		public Builder schemaDefault(HttpPartSchema value) {
			super.schemaDefault(value);
			return this;
		}

		@Override
		public Builder timeZone(TimeZone value) {
			super.timeZone(value);
			return this;
		}

		@Override
		public Builder timeZoneDefault(TimeZone value) {
			super.timeZoneDefault(value);
			return this;
		}

		@Override
		public Builder unmodifiable() {
			super.unmodifiable();
			return this;
		}

		@Override
		public Builder uriContext(UriContext value) {
			super.uriContext(value);
			return this;
		}
	}

	/**
	 * Creates a new builder for this session.
	 *
	 * @param ctx The serializer creating this session.
	 * 	<br>Cannot be <jk>null</jk>.
	 * @return A new builder.
	 */
	public static Builder create(RdfStreamSerializer ctx) {
		return new Builder(assertArgNotNull(ARG_ctx, ctx));
	}

	private final Model model;
	private final Property pRoot;
	private final Property pValue;
	private final Lang lang;
	private final RdfStreamSerializer ctx;
	private final Namespace[] namespaces;

	protected RdfStreamSerializerSession(Builder builder) {
		super(builder);
		ctx = builder.ctx;

		model = ModelFactory.createDefaultModel();
		namespaces = ctx.getNamespaces();
		addModelPrefix(ctx.getJuneauNs());
		addModelPrefix(ctx.getJuneauBpNs());
		if (namespaces != null)
			for (var ns : namespaces)
				addModelPrefix(ns);
		pRoot = model.createProperty(ctx.getJuneauNs().getUri(), RDF_juneauNs_ROOT);
		pValue = model.createProperty(ctx.getJuneauNs().getUri(), RDF_juneauNs_VALUE);

		var langName = ctx.getLanguage();
		lang = toLang(langName);
		if (lang == null)
			throw isex("Unknown RDF language: %s", langName);
	}

	@Override /* OutputStreamSerializerSession */
	public boolean hasNativeBytes() {
		// The RDF graph layer has no native byte-array literal type — byte[] is surfaced either as an
		// xsd:base64Binary typed literal or as a plain string literal
		// carrying the configured BinaryFormat's text wire form (at any other BinaryFormat, after the
		// BinarySwap fires).
		return false;
	}

	private static Lang toLang(String langName) {
		var l = org.apache.jena.riot.RDFLanguages.nameToLang(langName);
		if (l != null)
			return l;
		if ("RDF/PROTO".equals(langName)) // HTT - not registered in Jena's RDFLanguages
			return Lang.RDFPROTO;
		return null;
	}

	private void addModelPrefix(Namespace ns) {
		model.setNsPrefix(ns.getName(), ns.getUri());
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

	private String encodeElementName(Object o) {
		return XmlUtils.encodeElementName(toString(o));
	}

	private String encodeTextInvalidChars(Object o) {
		if (o == null)
			return null;
		return XmlUtils.escapeText(toString(o));
	}

	private String getUri(Object uri) {
		if (! nn(uri))
			return null;
		return getUriResolver().resolve(uri.toString());
	}

	@SuppressWarnings({
		"null" // aType/wType intentionally null before assignment in control flow
	})
	private RDFNode writeAnything(Object o, boolean isURI, ClassMeta<?> eType, String attrName, BeanPropertyMeta bpm, Resource parentResource) throws SerializeException {
		var m = model;
		ClassMeta<?> aType = push2(attrName, o, eType);
		ClassMeta<?> wType = null;
		ClassMeta<?> sType;

		if (eType == null)
			eType = object();
		if (aType == null) {
			o = null;
			aType = object();
		}
		if (isOptional(aType)) {
			o = getOptionalValue(o);
			eType = getOptionalType(eType);
			aType = getClassMetaForObject(o, object());
		}
		if (nn(o)) {
			if (aType.isDelegate()) {
				wType = aType;
				aType = (ClassMeta)((Delegate)o).getBeanInfo();
			}
			sType = aType;
			var swap = aType.getSwap(this);
			if (nn(swap)) {
				o = swap(swap, o);
				sType = swap.getSwapClassMeta(this);
				if (sType.isObject())
					sType = getClassMetaForObject(o);
			}
		} else {
			sType = eType.getSerializedClassMeta(this);
		}

		var typeName = getBeanTypeName(this, eType, aType, bpm);
		RDFNode n = null;

		if (o == null || sType.isChar() && ((Character)o).charValue() == 0) {
			if (nn(bpm)) {
				if (isKeepNullProperties())
					n = m.createResource(RDF_NIL);
			} else {
				n = m.createResource(RDF_NIL);
			}
		} else if (sType.isUri() || isURI) {
			// RDF URI gate must come before isBean/isMap/isCharSequence: @Uri-annotated values need Resource emission, not literal text.
			var uri = getUri(o);
			if (isAbsoluteUri(uri))
				n = m.createResource(uri);
			else
				n = m.createLiteral(encodeTextInvalidChars(uri));
		} else if (sType.isBean()) {
			var bm = toBeanMap(o);
			Object uri = null;
			var rbm = ctx.getRdfBeanMeta(bm.getMeta());
			if (rbm.hasBeanUri())
				uri = rbm.getBeanUriProperty().get(bm, null);
			var uri2 = getUri(uri);
			n = m.createResource(uri2);
			writeBeanMap(bm, (Resource)n, typeName);
		} else if (sType.isMap() || (nn(wType) && wType.isMap())) {
			if (o instanceof BeanMap o2) {
				Object uri = null;
				var rbm = ctx.getRdfBeanMeta(o2.getMeta());
				if (rbm.hasBeanUri())
					uri = rbm.getBeanUriProperty().get(o2, null);
				var uri2 = getUri(uri);
				n = m.createResource(uri2);
				writeBeanMap(o2, (Resource)n, typeName);
			} else {
				n = m.createResource();
				writeMap((Map)o, (Resource)n, sType);
			}
		} else if (sType.isByteArray()) {
			// byte[] gate must come before isCollectionOrArray: byte[] satisfies isArray() but RDF emits it as a typed base64 literal, not as a Seq of bytes.
			var b64 = Base64.getEncoder().encodeToString((byte[]) o);
			n = m.createTypedLiteral(b64, XSDDatatype.XSDbase64Binary);
		} else if (sType.isCollectionOrArray() || (nn(wType) && wType.isCollection())) {
			var c = sort(sType.isCollection() ? (Collection)o : toList(sType.inner(), o));
			var f = ctx.getCollectionFormat();
			var cRdf = ctx.getRdfClassMeta(sType);
			var bpRdf = getRdfBeanPropertyMeta(bpm);
			if (cRdf.getCollectionFormat() != RdfCollectionFormat.DEFAULT)
				f = cRdf.getCollectionFormat();
			if (bpRdf.getCollectionFormat() != RdfCollectionFormat.DEFAULT)
				f = bpRdf.getCollectionFormat();
			if (f == RdfCollectionFormat.MULTI_VALUED) {
				writeToMultiProperties(c, eType, bpm, attrName, parentResource);
			} else {
				n = switch (f) {
					case BAG -> writeToContainer(c, eType, m.createBag());
					case LIST -> writeToList(c, eType);
					default -> writeToContainer(c, eType, m.createSeq());
				};
			}
		} else if (sType.isCharSequence() || sType.isChar()) {
			n = m.createLiteral(encodeTextInvalidChars(o));
		} else if (sType.isNumber() || sType.isBoolean()) {
			if (!ctx.isAddLiteralTypes())
				n = m.createLiteral(o.toString());
			else
				n = m.createTypedLiteral(o);
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

	private void writeBeanMap(BeanMap<?> m, Resource r, String typeName) throws SerializeException {
		var l = new ArrayList<BeanPropertyValue>();
		if (nn(typeName)) {
			var pm = m.getMeta().getTypeProperty();
			l.add(new BeanPropertyValue(pm, pm.getName(), typeName, null));
		}
		var checkNull = (Predicate<Object>)(x -> isKeepNullProperties() || nn(x));
		m.forEachValue(checkNull, (pMeta, key, value, thrown) -> l.add(new BeanPropertyValue(pMeta, key, value, thrown)));
		Collections.reverse(l);
		l.forEach(x -> {
			var bpMeta = x.getMeta();
			var cMeta = (ClassMeta<?>) bpMeta.getBeanInfo();
			var bpRdf = getRdfBeanPropertyMeta(bpMeta);
			var bpXml = getXmlBeanPropertyMeta(bpMeta);
			if (bpRdf.isBeanUri())
				return;
			var key = x.getName();
			var value = x.getValue();
			var t = x.getThrown();
			if (nn(t))
				onBeanGetterException(bpMeta, t);
			if (canIgnoreValue(bpMeta, key, value))
				return;
			var ns = bpRdf.getNamespace();
			if (ns == null && ctx.isUseXmlNamespaces())
				ns = bpXml.getNamespace();
			if (ns == null)
				ns = ctx.getJuneauBpNs();
			else if (ctx.isAutoDetectNamespaces())
				addModelPrefix(ns);
			var p = model.createProperty(ns.getUri(), encodeElementName(key));
			var n = writeAnything(value, bpMeta.isUri(), cMeta, key, bpMeta, r);
			if (nn(n))
				r.addProperty(p, n);
		});
	}

	private void writeMap(Map m, Resource r, ClassMeta<?> type) throws SerializeException {
		m = sort(m);
		var keyType = type.getKeyType();
		var valueType = type.getValueType();
		var l = CollectionUtils.toList(m.entrySet());
		Collections.reverse(l);
		l.forEach(x -> {
			var entry = (Map.Entry<Object,Object>)x;
			Object value = entry.getValue();
			Object key = generalize(entry.getKey(), keyType);
			var ns = ctx.getJuneauBpNs();
			var p = model.createProperty(ns.getUri(), encodeElementName(toString(key)));
			var n = writeAnything(value, false, valueType, toString(key), null, r);
			if (nn(n))
				r.addProperty(p, n);
		});
	}

	private Container writeToContainer(Collection c, ClassMeta<?> type, Container list) throws SerializeException {
		var elementType = type.getElementType();
		c.forEach(x -> list.add(writeAnything(x, false, elementType, null, null, null)));
		return list;
	}

	private RDFList writeToList(Collection c, ClassMeta<?> type) throws SerializeException {
		var elementType = type.getElementType();
		List<RDFNode> l = listOfSize(c.size());
		c.forEach(x -> l.add(writeAnything(x, false, elementType, null, null, null)));
		return model.createList(l.iterator());
	}

	private void writeToMultiProperties(Collection c, ClassMeta<?> sType, BeanPropertyMeta bpm, String attrName, Resource parentResource) throws SerializeException {
		var elementType = sType.getElementType();
		var bpRdf = getRdfBeanPropertyMeta(bpm);
		var bpXml = getXmlBeanPropertyMeta(bpm);
		c.forEach(x -> {
			var ns = bpRdf.getNamespace();
			if (ns == null && ctx.isUseXmlNamespaces())
				ns = bpXml.getNamespace();
			if (ns == null)
				ns = ctx.getJuneauBpNs();
			else if (ctx.isAutoDetectNamespaces())
				addModelPrefix(ns);
			var n2 = writeAnything(x, false, elementType, null, null, null);
			var p = model.createProperty(ns.getUri(), encodeElementName(attrName));
			parentResource.addProperty(p, n2);
		});
	}

	@Override
	protected void doWrite(SerializerPipe out, Object o) throws IOException, SerializeException {
		var cm = getClassMetaForObject(o);
		// HTT: RdfStreamSerializer.Builder only exposes language(); looseCollections is always false for stream serializers.
		if (ctx.isLooseCollections() && nn(cm) && cm.isCollectionOrArray()) {
			Collection c = cm.isCollection() ? (Collection)o : toList(cm.inner(), o);
			forEachEntry(c, x -> writeAnything(x, false, object(), "root", null, null));
		} else {
			var n = writeAnything(o, false, getExpectedRootType(o), "root", null, null);
			Resource r;
			if (n.isLiteral()) {
				r = model.createResource();
				r.addProperty(pValue, n);
			} else {
				r = n.asResource();
			}
			// HTT: RdfStreamSerializer.Builder only exposes language(); addRootProp is always false.
			if (ctx.isAddRootProp())
				r.addProperty(pRoot, "true");
		}
		RDFDataMgr.write(out.getOutputStream(), model, lang);
	}

}
