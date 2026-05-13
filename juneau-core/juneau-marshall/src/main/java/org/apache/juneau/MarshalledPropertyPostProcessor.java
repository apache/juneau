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
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.swap.*;
import org.apache.juneau.swaps.*;

/**
 * Marshalling-side post-processor that applies {@link MarshalledProp @MarshalledProp} and {@link Swap @Swap}
 * annotation effects to a {@link org.apache.juneau.commons.bean.BeanPropertyMeta.Builder} after {@link org.apache.juneau.commons.bean.BeanPropertyMeta.Builder#validate validate()}
 * runs successfully.
 *
 * <p>
 * The reads here used to live inside {@link BeanPropertyMeta.Builder#validate}.  They were lifted out during
 * the Task-5 Step 8b-ii refactor so the bean-modeling builder no longer references marshalling-side
 * {@link ObjectSwap}/{@link StringFormatSwap}/{@link Surrogate} types or the {@link MarshalledProp} annotation.
 *
 * <p>
 * This post-processor mutates the following fields on the supplied builder when it runs:
 * <ul>
 * 	<li>{@code swap} — set to a {@link StringFormatSwap} (from {@link MarshalledProp#format()}) or a custom
 * 		{@link ObjectSwap} (from {@link Swap}).
 * 	<li>{@code dictionaryClasses} — appended with the {@link MarshalledProp#dictionary()} entries.
 * 	<li>{@code typeMeta} — refreshed to the swap class meta when a swap is installed.
 * </ul>
 *
 * <p>
 * Only invoked from {@link BeanMeta#validateAndRegisterProperty} on the marshalling-side construction path
 * (i.e. when {@link MarshallingContext} is non-null).  The bean-modeling-only path
 * ({@link BeanMeta#of(Class, BeanConfigContext)}) does not run this post-processor.
 */
@SuppressWarnings({
	"java:S6548" // Intentional stateless singleton SPI implementation wired through BeanConfigContext.
})
final class MarshalledPropertyPostProcessor implements BeanPropertyPostProcessor {

	/** Singleton instance wired into {@link MarshallingContext}-built {@link BeanConfigContext}s. */
	static final MarshalledPropertyPostProcessor INSTANCE = new MarshalledPropertyPostProcessor();

	private MarshalledPropertyPostProcessor() {}

	/**
	 * SPI entry point — narrows the opaque marshalling context, then dispatches to
	 * {@link #process(MarshallingContext, BeanPropertyMeta.Builder)}.
	 *
	 * @param marshallingContext The marshalling-side context.  Must not be <jk>null</jk> on the marshalling path.
	 * @param builder The bean-property builder.  Must not be <jk>null</jk>.
	 */
	@Override
	public void process(Object marshallingContext, BeanPropertyMeta.Builder builder) {
		if (marshallingContext == null)
			return;
		process((MarshallingContext) marshallingContext, builder);
	}

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
				bdClasses.addAll(l(mp.dictionary()));
			});
			ap.find(Swap.class, b.innerField).stream().findFirst().ifPresent(x -> b.swap = swapSwap(x));
			b.isUri |= ap.has(Uri.class, b.innerField);
		}

		if (nn(b.getter)) {
			ap.find(MarshalledProp.class, b.getter).forEach(x -> {
				var mp = x.inner();
				if (b.swap == null)
					b.swap = marshalledPropSwap(x);
				bdClasses.addAll(l(mp.dictionary()));
			});
			ap.find(Swap.class, b.getter).forEach(x -> b.swap = swapSwap(x));
			b.isUri |= ap.has(Uri.class, b.getter);
		}

		if (nn(b.setter)) {
			ap.find(MarshalledProp.class, b.setter).forEach(x -> {
				var mp = x.inner();
				if (b.swap == null)
					b.swap = marshalledPropSwap(x);
				bdClasses.addAll(l(mp.dictionary()));
			});
			ap.find(Swap.class, b.setter).forEach(x -> b.swap = swapSwap(x));
			b.isUri |= ap.has(Uri.class, b.setter);
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
			b.typeMeta = bc.getClassMeta(((ObjectSwap<?,?>) b.swap).getSwapClass());

		// Install swap-aware read/write transforms on the builder.
		// Previously lived as a private static helper on {@link BeanMeta}; moved here during Task-5 Step 8b-ii
		// Phase C Task 3 so {@link BeanMeta} no longer references {@link ObjectSwap}/{@link ParseException}/{@link SerializeException}.
		installSwapAwareTransforms(b);
	}

	/**
	 * Installs swap-aware read/write transforms on a {@link org.apache.juneau.commons.bean.BeanPropertyMeta.Builder} after validation.
	 *
	 * <p>
	 * After {@link BeanPropertyMeta.Builder#validate validate()} succeeds, the builder's {@code swap} and
	 * {@code rawTypeMeta} fields describe whether the property has a configured {@link ObjectSwap} (via
	 * {@link MarshalledProp @MarshalledProp(format=...)} or
	 * {@link Swap @Swap}) and whether the property's raw type has child swaps registered
	 * on it.  This method packages those concerns into install-time closures so the marshalling-side swap behavior is
	 * established as data on the {@link BeanPropertyMeta} rather than executed by the bean-modeling
	 * {@link BeanPropertyMeta#get get}/{@link BeanPropertyMeta#set set} methods themselves.
	 *
	 * <p>
	 * If neither a property-level swap nor a child swap on the raw type meta is present, no transforms are installed
	 * and the property's {@code get}/{@code set} fall through to identity (raw access).
	 *
	 * @param p The builder to attach swap-aware transforms to.
	 */
	@SuppressWarnings({
		"java:S3776", // Centralized swap-transform wiring; branching mirrors swap/no-swap and child-swap read/write paths.
		"unchecked"   // Wildcard ObjectSwap captured from Object-typed builder fields for runtime polymorphic dispatch.
	})
	static void installSwapAwareTransforms(BeanPropertyMeta.Builder p) {
		ObjectSwap<?,?> sw = (ObjectSwap<?,?>) p.swap;
		ClassMeta<?> rtm = (ClassMeta<?>) p.rawTypeMeta;
		if (sw == null && (rtm == null || ! rtm.hasChildSwaps()))
			return;
		if (p.readTransform == null) {
			p.readTransform = (session, o) -> {
				try {
					// The transform is typed against BeanSession (commons.bean SPI) but ObjectSwap.swap requires a
					// MarshallingSession.  The marshalling-side BeanMap always wires a MarshallingSession into its
					// transform call sites, so the narrowing cast is safe.
					var ms = (MarshallingSession) session;
					if (nn(sw))
						return ((ObjectSwap<Object,Object>) sw).swap(ms, o);
					if (o == null)
						return null;
					if (rtm.hasChildSwaps()) {
						ObjectSwap<?,?> f = rtm.getChildObjectSwapForSwap(o.getClass());
						if (nn(f))
							return ((ObjectSwap<Object,Object>) f).swap(ms, o);
					}
					return o;
				} catch (RuntimeException e) {
					throw e;
				} catch (Exception e) {
					throw new SerializeException(e);
				}
			};
		}
		if (p.writeTransform == null) {
			p.writeTransform = (session, o) -> {
				try {
					// See readTransform note: BeanSession → MarshallingSession is safe on the marshalling-side path.
					var ms = (MarshallingSession) session;
					if (nn(sw))
						return ((ObjectSwap<Object,Object>) sw).unswap(ms, o, rtm);
					if (o == null)
						return null;
					if (rtm.hasChildSwaps()) {
						ObjectSwap<?,?> f = rtm.getChildObjectSwapForUnswap(o.getClass());
						if (nn(f))
							return ((ObjectSwap<Object,Object>) f).unswap(ms, o, rtm);
					}
					return o;
				} catch (RuntimeException e) {
					throw e;
				} catch (Exception e) {
					throw new ParseException(e);
				}
			};
		}
	}

	private static ObjectSwap<?,?> marshalledPropSwap(AnnotationInfo<MarshalledProp> ai) {
		var p = ai.inner();
		if (! p.format().isEmpty())
			return BeanInstantiator.of(ObjectSwap.class).type(StringFormatSwap.class).addBean(String.class, p.format()).run();
		return null;
	}

	@SuppressWarnings({
		"java:S112" // throws RuntimeException intentional - callback/lifecycle method for swap initialization
	})
	private static ObjectSwap<?,?> swapSwap(AnnotationInfo<Swap> ai) {
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
