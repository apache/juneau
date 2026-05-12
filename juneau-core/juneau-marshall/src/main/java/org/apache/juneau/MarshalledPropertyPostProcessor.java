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
package org.apache.juneau;

import static org.apache.juneau.commons.reflect.ReflectionUtils.*;
import static org.apache.juneau.commons.utils.ClassUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.swap.*;
import org.apache.juneau.swaps.*;

/**
 * Marshalling-side post-processor that applies {@link MarshalledProp @MarshalledProp} and {@link Swap @Swap}
 * annotation effects to a {@link BeanPropertyMeta.Builder} after {@link BeanPropertyMeta.Builder#validate validate()}
 * runs successfully.
 *
 * <p>
 * The reads here used to live inside {@link BeanPropertyMeta.Builder#validate}.  They were lifted out as part of
 * TODO-5 Step 8b-ii so the bean-modeling builder no longer references the marshalling-side
 * {@link ObjectSwap}/{@link StringFormatSwap}/{@link Surrogate} types or the {@link MarshalledProp} annotation.
 *
 * <p>
 * This post-processor mutates the following fields on the supplied builder when it runs:
 * <ul>
 * 	<li>{@code swap} — set to a {@link StringFormatSwap} (from {@link MarshalledProp#format()}) or a custom
 * 		{@link ObjectSwap} (from {@link Swap}).
 * 	<li>{@code properties} — set to the property-override list from {@link MarshalledProp#properties()}.
 * 	<li>{@code dictionaryClasses} — appended with the {@link MarshalledProp#dictionary()} entries.
 * 	<li>{@code typeMeta} — refreshed to the swap class meta when a swap is installed.
 * </ul>
 *
 * <p>
 * Only invoked from {@link BeanMeta#validateAndRegisterProperty} on the marshalling-side construction path
 * (i.e. when {@link MarshallingContext} is non-null).  The bean-modeling-only path
 * ({@link BeanMeta#of(Class, BeanConfigContext)}) does not run this post-processor.
 */
final class MarshalledPropertyPostProcessor {

	private MarshalledPropertyPostProcessor() {}

	/**
	 * Applies {@code @MarshalledProp}/{@code @Swap} annotation effects to {@code b} using {@code bc} for
	 * {@link ClassMeta} resolution.
	 *
	 * @param bc The marshalling context.  Must not be <jk>null</jk>.
	 * @param b The bean-property builder.  Must not be <jk>null</jk>.
	 */
	static void process(MarshallingContext bc, BeanPropertyMeta.Builder b) {
		var ap = bc.getAnnotationProvider();
		var bdClasses = new ArrayList<Class<?>>();

		// innerField, getter, setter — same order as the original validate() loop.
		if (nn(b.innerField)) {
			ap.find(MarshalledProp.class, b.innerField).forEach(x -> {
				var mp = x.inner();
				if (b.swap == null)
					b.swap = marshalledPropSwap(x);
				if (ne(mp.properties()))
					b.properties = split(mp.properties());
				bdClasses.addAll(l(mp.dictionary()));
			});
			ap.find(Swap.class, b.innerField).stream().findFirst().ifPresent(x -> b.swap = swapSwap(x));
		}

		if (nn(b.getter)) {
			ap.find(MarshalledProp.class, b.getter).forEach(x -> {
				var mp = x.inner();
				if (b.swap == null)
					b.swap = marshalledPropSwap(x);
				if (nn(b.properties) && ne(mp.properties()))
					b.properties = split(mp.properties());
				bdClasses.addAll(l(mp.dictionary()));
			});
			ap.find(Swap.class, b.getter).stream().forEach(x -> b.swap = swapSwap(x));
		}

		if (nn(b.setter)) {
			ap.find(MarshalledProp.class, b.setter).forEach(x -> {
				var mp = x.inner();
				if (b.swap == null)
					b.swap = marshalledPropSwap(x);
				if (nn(b.properties) && ne(mp.properties()))
					b.properties = split(mp.properties());
				bdClasses.addAll(l(mp.dictionary()));
			});
			ap.find(Swap.class, b.setter).stream().forEach(x -> b.swap = swapSwap(x));
		}

		if (! bdClasses.isEmpty()) {
			var infos = new ArrayList<ClassInfo>(b.dictionaryClasses.size() + bdClasses.size());
			infos.addAll(b.dictionaryClasses);
			bdClasses.forEach(c -> infos.add(info(c)));
			b.dictionaryClasses = infos;
		}

		// If a swap was installed and we have a rawTypeMeta, refresh typeMeta to the swap's swap-class meta.
		// Matches the original behavior inside validate() which routed swap-class meta through bc.getClassMeta.
		// Cast to ObjectSwap because b.swap is Object-typed (BeanPropertyMeta.Builder lives in the bean-modeling
		// layer and cannot reference ObjectSwap directly).
		if (nn(b.swap) && nn(b.rawTypeMeta))
			b.typeMeta = bc.getClassMeta(((ObjectSwap) b.swap).getSwapClass());
	}

	private static ObjectSwap marshalledPropSwap(AnnotationInfo<MarshalledProp> ai) {
		var p = ai.inner();
		if (! p.format().isEmpty())
			return BeanInstantiator.of(ObjectSwap.class).type(StringFormatSwap.class).addBean(String.class, p.format()).run();
		return null;
	}

	@SuppressWarnings({
		"java:S112" // throws RuntimeException intentional - callback/lifecycle method for swap initialization
	})
	private static ObjectSwap swapSwap(AnnotationInfo<Swap> ai) {
		var s = ai.inner();
		var c = s.value();
		if (isVoid(c))
			c = s.impl();
		if (isVoid(c))
			return null;
		var ci = info(c);
		if (ci.isAssignableTo(ObjectSwap.class)) {
			var ps = BeanInstantiator.of(ObjectSwap.class).type(ci).run();
			if (nn(ps.forMediaTypes()))
				throw unsupportedOp("TODO - Media types on swaps not yet supported on bean properties.");
			if (nn(ps.withTemplate()))
				throw unsupportedOp("TODO - Templates on swaps not yet supported on bean properties.");
			return ps;
		}
		if (ci.isAssignableTo(Surrogate.class))
			throw unsupportedOp("TODO - Surrogate swaps not yet supported on bean properties.");
		throw rex("Invalid class used in @Swap annotation.  Must be a subclass of ObjectSwap or Surrogate. {0}", cn(c));
	}
}
