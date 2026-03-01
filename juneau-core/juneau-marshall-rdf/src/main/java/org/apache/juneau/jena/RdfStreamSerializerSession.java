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
package org.apache.juneau.jena;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.IoUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;
import static org.apache.juneau.jena.Constants.*;

import java.io.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.utils.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.xml.*;

import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.*;

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
	"java:S115"
})
public class RdfStreamSerializerSession extends OutputStreamSerializerSession {

	private static final String ARG_ctx = "ctx";

	/**
	 * Builder class.
	 */
	public static class Builder extends OutputStreamSerializerSession.Builder {

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
		public Builder locale(java.util.Locale value) {
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
		public Builder timeZone(java.util.TimeZone value) {
			super.timeZone(value);
			return this;
		}

		@Override
		public Builder timeZoneDefault(java.util.TimeZone value) {
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
	 * @return A new builder.
	 */
	public static Builder create(RdfStreamSerializer ctx) {
		return new Builder(assertArgNotNull(ARG_ctx, ctx));
	}

	private final Model model;
	private final Property pRoot, pValue;
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
			throw new IllegalStateException("Unknown RDF language: " + langName);
	}

	private static Lang toLang(String langName) {
		var l = org.apache.jena.riot.RDFLanguages.nameToLang(langName);
		if (l != null)
			return l;
		return switch (langName) {
			case "N-TRIPLE" -> Lang.NTRIPLES;
			case "RDF/XML-ABBREV" -> Lang.RDFXML;
			case "JSON-LD" -> Lang.JSONLD;
			case "N-QUADS" -> Lang.NQUADS;
			case "TRIG" -> Lang.TRIG;
			case "TRIX" -> Lang.TRIX;
			case "RDF/JSON" -> Lang.RDFJSON;
			case "RDF/THRIFT" -> Lang.RDFTHRIFT;
			case "RDF/PROTO" -> Lang.RDFPROTO;
			default -> null;
		};
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

	private String getUri(Object uri, Object uri2) {
		var s = (String)null;
		if (nn(uri))
			s = uri.toString();
		if ((s == null || s.isEmpty()) && nn(uri2))
			s = uri2.toString();
		if (s == null)
			return null;
		return getUriResolver().resolve(s);
	}

	@SuppressWarnings("null")
	private RDFNode serializeAnything(Object o, boolean isURI, ClassMeta<?> eType, String attrName, BeanPropertyMeta bpm, Resource parentResource) throws SerializeException {
		var m = model;
		var aType = (ClassMeta<?>)null;
		var wType = (ClassMeta<?>)null;
		ClassMeta<?> sType = object();

		aType = push2(attrName, o, eType);
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
				aType = ((Delegate)o).getClassMeta();
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
		var n = (RDFNode)null;

		if (o == null || sType.isChar() && ((Character)o).charValue() == 0) {
			if (nn(bpm)) {
				if (isKeepNullProperties())
					n = m.createResource(RDF_NIL);
			} else {
				n = m.createResource(RDF_NIL);
			}
		} else if (sType.isUri() || isURI) {
			var uri = getUri(o, null);
			if (isAbsoluteUri(uri))
				n = m.createResource(uri);
			else
				n = m.createLiteral(encodeTextInvalidChars(uri));
		} else if (sType.isCharSequence() || sType.isChar()) {
			n = m.createLiteral(encodeTextInvalidChars(o));
		} else if (sType.isNumber() || sType.isBoolean()) {
			if (!ctx.isAddLiteralTypes())
				n = m.createLiteral(o.toString());
			else
				n = m.createTypedLiteral(o);
		} else if (sType.isMap() || (nn(wType) && wType.isMap())) {
			if (o instanceof BeanMap o2) {
				var uri = (Object)null;
				var rbm = ctx.getRdfBeanMeta(o2.getMeta());
				if (rbm.hasBeanUri())
					uri = rbm.getBeanUriProperty().get(o2, null);
				var uri2 = getUri(uri, null);
				n = m.createResource(uri2);
				serializeBeanMap(o2, (Resource)n, typeName);
			} else {
				n = m.createResource();
				serializeMap((Map)o, (Resource)n, sType);
			}
		} else if (sType.isBean()) {
			var bm = toBeanMap(o);
			var uri = (Object)null;
			var rbm = ctx.getRdfBeanMeta(bm.getMeta());
			if (rbm.hasBeanUri())
				uri = rbm.getBeanUriProperty().get(bm, null);
			var uri2 = getUri(uri, null);
			n = m.createResource(uri2);
			serializeBeanMap(bm, (Resource)n, typeName);
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
				serializeToMultiProperties(c, eType, bpm, attrName, parentResource);
			} else {
				n = switch (f) {
					case BAG -> serializeToContainer(c, eType, m.createBag());
					case LIST -> serializeToList(c, eType);
					default -> serializeToContainer(c, eType, m.createSeq());
				};
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

	private void serializeBeanMap(BeanMap<?> m, Resource r, String typeName) throws SerializeException {
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
			var cMeta = bpMeta.getClassMeta();
			var bpRdf = getRdfBeanPropertyMeta(bpMeta);
			var bpXml = getXmlBeanPropertyMeta(bpMeta);
			if (bpRdf.isBeanUri())
				return;
			var key = x.getName();
			var value = x.getValue();
			var t = x.getThrown();
			if (nn(t))
				onBeanGetterException(bpMeta, t);
			if (canIgnoreValue(cMeta, key, value))
				return;
			var ns = bpRdf.getNamespace();
			if (ns == null && ctx.isUseXmlNamespaces())
				ns = bpXml.getNamespace();
			if (ns == null)
				ns = ctx.getJuneauBpNs();
			else if (ctx.isAutoDetectNamespaces())
				addModelPrefix(ns);
			var p = model.createProperty(ns.getUri(), encodeElementName(key));
			var n = serializeAnything(value, bpMeta.isUri(), cMeta, key, bpMeta, r);
			if (nn(n))
				r.addProperty(p, n);
		});
	}

	private void serializeMap(Map m, Resource r, ClassMeta<?> type) throws SerializeException {
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
			var n = serializeAnything(value, false, valueType, toString(key), null, r);
			if (nn(n))
				r.addProperty(p, n);
		});
	}

	private Container serializeToContainer(Collection c, ClassMeta<?> type, Container list) throws SerializeException {
		var elementType = type.getElementType();
		c.forEach(x -> list.add(serializeAnything(x, false, elementType, null, null, null)));
		return list;
	}

	private RDFList serializeToList(Collection c, ClassMeta<?> type) throws SerializeException {
		var elementType = type.getElementType();
		List<RDFNode> l = listOfSize(c.size());
		c.forEach(x -> l.add(serializeAnything(x, false, elementType, null, null, null)));
		return model.createList(l.iterator());
	}

	private void serializeToMultiProperties(Collection c, ClassMeta<?> sType, BeanPropertyMeta bpm, String attrName, Resource parentResource) throws SerializeException {
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
			var n2 = serializeAnything(x, false, elementType, null, null, null);
			var p = model.createProperty(ns.getUri(), encodeElementName(attrName));
			parentResource.addProperty(p, n2);
		});
	}

	@Override
	protected void doSerialize(SerializerPipe out, Object o) throws IOException, SerializeException {
		var cm = getClassMetaForObject(o);
		if (ctx.isLooseCollections() && nn(cm) && cm.isCollectionOrArray()) {
			Collection c = cm.isCollection() ? (Collection)o : toList(cm.inner(), o);
			forEachEntry(c, x -> serializeAnything(x, false, object(), "root", null, null));
		} else {
			var n = serializeAnything(o, false, getExpectedRootType(o), "root", null, null);
			Resource r;
			if (n.isLiteral()) {
				r = model.createResource();
				r.addProperty(pValue, n);
			} else {
				r = n.asResource();
			}
			if (ctx.isAddRootProp())
				r.addProperty(pRoot, "true");
		}
		RDFDataMgr.write(out.getOutputStream(), model, lang);
	}

	@Override
	protected boolean isAddBeanTypes() {
		return ctx.isAddBeanTypes();
	}
}
