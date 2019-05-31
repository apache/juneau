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
package org.apache.juneau.rest;

import static org.apache.juneau.BeanContext.*;
import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.rest.util.RestUtils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.httppart.bean.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.util.*;
import org.apache.juneau.rest.widget.*;
import org.apache.juneau.svl.*;

/**
 * Builder class for {@link RestMethodContext} objects.
 */
public class RestMethodContextBuilder extends BeanContextBuilder {

	RestContext context;
	java.lang.reflect.Method method;
	boolean hasConfigAnnotations;

	String httpMethod;
	EncoderGroup encoders;
	JsonSchemaGenerator jsonSchemaGenerator;
	BeanContext beanContext;
	RestMethodProperties properties;
	PropertyStore propertyStore;
	Map<String,Object> defaultRequestHeaders, defaultQuery, defaultFormData;
	Integer priority;
	Map<String,Widget> widgets;
	List<MediaType> supportedAcceptTypes, supportedContentTypes;
	ResponseBeanMeta responseMeta;

	@SuppressWarnings("deprecation")
	RestMethodContextBuilder(Object servlet, java.lang.reflect.Method method, RestContext context) throws RestServletException {
		this.context = context;
		this.method = method;

		String sig = method.getDeclaringClass().getName() + '.' + method.getName();
		MethodInfo mi = getMethodInfo(servlet.getClass(), method);

		try {

			RestMethod m = mi.getAnnotation(RestMethod.class);
			if (m == null)
				throw new RestServletException("@RestMethod annotation not found on method ''{0}''", sig);

			VarResolver vr = context.getVarResolver();
			VarResolverSession vrs = vr.createSession();

			// If this method doesn't have any config annotations (e.g. @BeanConfig), then we want to
			// reuse the serializers/parsers on the class.
			for (AnnotationInfo<?> ai : mi.getAnnotationListMethodOnlyParentFirst(ConfigAnnotationFilter.INSTANCE)) {
				hasConfigAnnotations = ! ai.isType(RestMethod.class);
				if (hasConfigAnnotations)
					break;
			}

			applyAnnotations(mi.getAnnotationListParentFirst(ConfigAnnotationFilter.INSTANCE), vrs);

			jsonSchemaGenerator = context.getJsonSchemaGenerator();
			beanContext = context.getBeanContext();
			encoders = context.getEncoders();
			properties = new RestMethodProperties(context.getProperties());
			AnnotationList configAnnotationList = hasConfigAnnotations ? mi.getAnnotationListParentFirst(ConfigAnnotationFilter.INSTANCE) : context.getConfigAnnotationList();

			HtmlDocBuilder hdb = new HtmlDocBuilder(properties);

			HtmlDoc hd = m.htmldoc();
			hdb.process(hd);

			widgets = new HashMap<>(context.getWidgets());
			for (Class<? extends Widget> wc : hd.widgets()) {
				Widget w = castOrCreate(Widget.class, wc);
				widgets.put(w.getName(), w);
				hdb.script("INHERIT", "$W{"+w.getName()+".script}");
				hdb.style("INHERIT", "$W{"+w.getName()+".style}");
			}

			BeanContextBuilder bcb = null;
			JsonSchemaGeneratorBuilder jsgb = null;
			PropertyStore cps = context.getPropertyStore();

			Object[] mPojoSwaps = merge(cps.getArrayProperty(BEAN_pojoSwaps, Object.class), m.pojoSwaps());
			Object[] mBeanFilters = merge(cps.getArrayProperty(BEAN_beanFilters, Object.class), m.beanFilters());

			if (m.serializers().length > 0 || m.parsers().length > 0 || m.properties().length > 0 || m.flags().length > 0
					|| m.beanFilters().length > 0 || m.pojoSwaps().length > 0 || m.bpi().length > 0
					|| m.bpx().length > 0 || hasConfigAnnotations) {
				bcb = beanContext.builder();
				jsgb = JsonSchemaGenerator.create();
			}

			httpMethod = emptyIfNull(firstNonEmpty(m.name(), m.method())).toUpperCase(Locale.ENGLISH);
			if (httpMethod.isEmpty())
				httpMethod = HttpUtils.detectHttpMethod(method, true, "GET");
			if ("METHOD".equals(httpMethod))
				httpMethod = "*";

			priority = m.priority();

			VarResolverSession sr = vr.createSession();

			PropertyStoreBuilder psb = PropertyStore.create().apply(context.getPropertyStore()).set(BEAN_beanFilters, mBeanFilters).set(BEAN_pojoSwaps, mPojoSwaps);

			for (Property p1 : m.properties())
				psb.set(p1.name(), p1.value());
			for (String p1 : m.flags())
				psb.set(p1, true);
			if (hasConfigAnnotations)
				psb.applyAnnotations(configAnnotationList, sr);

			this.propertyStore = psb.build();

			if (bcb != null) {
				bcb.apply(propertyStore);
				bcb.beanFilters(mBeanFilters);
				bcb.pojoSwaps(mPojoSwaps);
			}

			if (jsgb != null) {
				jsgb.apply(propertyStore);
				jsgb.beanFilters(mBeanFilters);
				jsgb.pojoSwaps(mPojoSwaps);
			}

			if (m.properties().length > 0 || m.flags().length > 0) {
				properties = new RestMethodProperties(properties);
				for (Property p1 : m.properties())
					properties.put(p1.name(), p1.value());
				for (String p1 : m.flags())
					properties.put(p1, true);
			}

			if (m.encoders().length > 0) {
				EncoderGroupBuilder g = EncoderGroup.create().append(IdentityEncoder.INSTANCE);
				for (Class<?> c : m.encoders()) {
					try {
						g.append(c);
					} catch (Exception e) {
						throw new RestServletException(
							"Exception occurred while trying to instantiate ConfigEncoder on method ''{0}'': ''{1}''", sig, c.getSimpleName()).initCause(e);
					}
				}
				encoders = g.build();
			}

			defaultRequestHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
			for (String s : m.defaultRequestHeaders()) {
				String[] h = RestUtils.parseKeyValuePair(vr.resolve(s));
				if (h == null)
					throw new RestServletException(
						"Invalid default request header specified on method ''{0}'': ''{1}''.  Must be in the format: ''name[:=]value''", sig, s);
				defaultRequestHeaders.put(h[0], h[1]);
			}

			String defaultAccept = vr.resolve(m.defaultAccept());
			if (isNotEmpty(defaultAccept))
				defaultRequestHeaders.put("Accept", defaultAccept);

			String defaultContentType = vr.resolve(m.defaultContentType());
			if (isNotEmpty(defaultContentType))
				defaultRequestHeaders.put("Content-Type", defaultAccept);

			defaultQuery = new LinkedHashMap<>();
			for (String s : m.defaultQuery()) {
				String[] h = RestUtils.parseKeyValuePair(vr.resolve(s));
				if (h == null)
					throw new RestServletException(
						"Invalid default query parameter specified on method ''{0}'': ''{1}''.  Must be in the format: ''name[:=]value''", sig, s);
				defaultQuery.put(h[0], h[1]);
			}

			defaultFormData = new LinkedHashMap<>();
			for (String s : m.defaultFormData()) {
				String[] h = RestUtils.parseKeyValuePair(vr.resolve(s));
				if (h == null)
					throw new RestServletException(
						"Invalid default form data parameter specified on method ''{0}'': ''{1}''.  Must be in the format: ''name[:=]value''", sig, s);
				defaultFormData.put(h[0], h[1]);
			}

			Type[] pt = method.getGenericParameterTypes();
			Annotation[][] pa = method.getParameterAnnotations();
			for (int i = 0; i < pt.length; i++) {
				for (Annotation a : pa[i]) {
					if (a instanceof Header) {
						Header h = (Header)a;
						if (h._default().length > 0)
							defaultRequestHeaders.put(firstNonEmpty(h.name(), h.value()), parseAnything(joinnl(h._default())));
					} else if (a instanceof Query) {
						Query q = (Query)a;
						if (q._default().length > 0)
							defaultQuery.put(firstNonEmpty(q.name(), q.value()), parseAnything(joinnl(q._default())));
					} else if (a instanceof FormData) {
						FormData f = (FormData)a;
						if (f._default().length > 0)
							defaultFormData.put(firstNonEmpty(f.name(), f.value()), parseAnything(joinnl(f._default())));
					}
				}
			}

			if (bcb != null)
				beanContext = bcb.build();
			if (jsgb != null)
				jsonSchemaGenerator = jsgb.build();

			// Need this to access methods in anonymous inner classes.
			mi.setAccessible();
		} catch (RestServletException e) {
			throw e;
		} catch (Exception e) {
			throw new RestServletException("Exception occurred while initializing method ''{0}''", sig).initCause(e);
		}
	}
}