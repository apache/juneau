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
package org.apache.juneau.marshall;

import static org.apache.juneau.commons.reflect.ReflectionUtils.*;
import static org.apache.juneau.commons.utils.ClassUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.math.*;
import java.time.*;
import java.time.Duration;
import java.time.temporal.*;
import java.util.*;

import javax.xml.datatype.*;

import org.apache.juneau.commons.*;
import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.httppart.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.csv.*;
import org.apache.juneau.marshall.oapi.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.swap.*;
import org.apache.juneau.marshall.swaps.*;

/**
 * Marshalling-side post-processor that applies {@link MarshalledProp @MarshalledProp} and {@link Swap @Swap}
 * annotation effects to a {@link BeanPropertyMeta.Builder} after {@link BeanPropertyMeta.Builder#validate validate()}
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
	"java:S6548", // Intentional stateless singleton SPI implementation wired through BeanConfigContext.
	"java:S6539"  // Monster Class threshold is advisory; this is a routing seam that intentionally depends on many marshalling types.
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
	@SuppressWarnings({
		"java:S3776" // Annotation-discovery dispatch over innerField/getter/setter — branching is intentional; splitting hurts JIT inlining.
	})
	static void process(MarshallingContext bc, BeanPropertyMeta.Builder b) {
		var ap = bc.getAnnotationProvider();
		var bdClasses = new ArrayList<Class<?>>();
		var propertyClass = propertyClass(b);

		// XMLGregorianCalendar always uses XML format regardless of any CalendarFormat setting.
		if (b.swap == null && XMLGregorianCalendar.class.equals(propertyClass))
			b.swap = xmlGregorianCalendarSwap();

		// innerField, getter, setter — same order as the original validate() loop.
		if (nn(b.innerField)) {
			ap.find(MarshalledProp.class, b.innerField).forEach(x -> {
				var mp = x.inner();
				if (b.swap == null)
					b.swap = marshalledPropSwap(x);
				applyPropertyFormats(b, mp, propertyClass);
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
				applyPropertyFormats(b, mp, propertyClass);
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
				applyPropertyFormats(b, mp, propertyClass);
				bdClasses.addAll(l(mp.dictionary()));
			});
			ap.find(Swap.class, b.setter).forEach(x -> b.swap = swapSwap(x));
			b.isUri |= ap.has(Uri.class, b.setter);
		}

		ClassInfo ownerClass = owningClass(b);
		if (nn(ownerClass)) {
			ap.find(Marshalled.class, ownerClass).stream().findFirst().ifPresent(x -> {
				var m = x.inner();
				applyClassFormats(b, m, propertyClass);
			});
		}

		if (b.swap == null)
			applyContextFormats(b, bc, propertyClass);

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
		// so {@link BeanMeta} no longer references {@link ObjectSwap}/{@link ParseException}/{@link SerializeException}.
		installSwapAwareTransforms(b);

		// Install schema-validation transforms (wraps any existing readTransform/writeTransform) when a
		// PropertyValidatorFactory is on the classpath and the property carries at least one @Schema annotation.
		installSchemaValidationTransforms(bc, b);
	}

	/**
	 * Installs swap-aware read/write transforms on a {@link BeanPropertyMeta.Builder} after validation.
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
		"unchecked",  // Wildcard ObjectSwap captured from Object-typed builder fields for runtime polymorphic dispatch.
		"null"        // `sw` may be null but is guarded by `nn(sw)` before access; Eclipse doesn't recognise `nn()` as a null-check function.
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

	/**
	 * Installs {@code @Schema}-driven validation transforms on a {@link BeanPropertyMeta.Builder}.
	 *
	 * <p>
	 * Discovery flow:
	 * <ol>
	 * 	<li>Resolve a {@link PropertyValidatorFactory} via {@link PropertyValidators#factory()} (ServiceLoader-backed).
	 * 		If no factory is on the classpath, this is a silent no-op.
	 * 	<li>Collect every {@link Schema @Schema} annotation reachable from {@code b.innerField} / {@code b.getter} /
	 * 		{@code b.setter} via the {@link AnnotationProvider}.
	 * 	<li>If at least one is found, merge them into a single {@link JsonMap} via {@link SchemaAnnotation#asMap(Schema)}
	 * 		(later entries override earlier).
	 * 	<li>Ask the factory for a {@link PropertyValidator} keyed off the merged map.  If the factory returns
	 * 		{@code null} (e.g. the map carried only descriptive keywords), do nothing.
	 * 	<li>Wrap {@code b.readTransform} and {@code b.writeTransform} so the validator runs whenever the
	 * 		{@link MarshallingSession} has {@code isValidateSchema()} enabled.
	 * </ol>
	 *
	 * <p>
	 * Validation failures throw {@link SchemaValidationException}, which we wrap in {@link BeanRuntimeException} so
	 * the parser's outer catch block lifts them into {@link ParseException} and the serializer surfaces them through
	 * the existing exception flow.
	 *
	 * @param bc The marshalling context.  Must not be <jk>null</jk>.
	 * @param b The bean-property builder.  Must not be <jk>null</jk>.
	 */
	@SuppressWarnings({
		"java:S3776" // Discovery branches over innerField/getter/setter mirror the swap discovery loop above.
	})
	static void installSchemaValidationTransforms(MarshallingContext bc, BeanPropertyMeta.Builder b) {
		// Skip when validation is not enabled on the context.  BeanMeta is cached per-context, so contexts with
		// validateSchema=true get their own cache slot and we don't pay the discovery / factory cost on the
		// default (validateSchema=false) path.  Also avoids a recursive BeanMeta build cycle: the factory parses
		// JSON into org.apache.juneau.bean.jsonschema.JsonSchema (FQCN: juneau-bean-jsonschema is a runtime-only
		// dep, not on the juneau-marshall compile classpath) via the default JSON parser, which would otherwise
		// re-enter this method while the original BeanMeta is still under construction.
		if (! bc.isValidateSchema())
			return;
		var factory = PropertyValidators.factory();
		if (factory == null)
			return;
		var ap = bc.getAnnotationProvider();
		JsonMap merged = null;
		if (nn(b.innerField))
			for (var ai : ap.find(Schema.class, b.innerField))
				merged = applyToMap(merged, ai.inner());
		if (nn(b.getter))
			for (var ai : ap.find(Schema.class, b.getter))
				merged = applyToMap(merged, ai.inner());
		if (nn(b.setter))
			for (var ai : ap.find(Schema.class, b.setter))
				merged = applyToMap(merged, ai.inner());
		if (merged == null || merged.isEmpty())
			return;
		var validator = factory.create(merged, propertyClass(b));
		if (validator == null)
			return;

		final var propertyName = b.name;
		var innerRead = b.readTransform;
		b.readTransform = (session, o) -> {
			Object v = nn(innerRead) ? innerRead.apply(session, o) : o;
			if (session instanceof MarshallingSession ms && ms.isValidateSchema()) {
				try {
					validator.validate(v);
				} catch (SchemaValidationException e) {
					throw new BeanRuntimeException(e, null, "Schema validation failed on property ''{0}'': {1}", propertyName, e.getMessage());
				}
			}
			return v;
		};
		var innerWrite = b.writeTransform;
		b.writeTransform = (session, o) -> {
			if (session instanceof MarshallingSession ms && ms.isValidateSchema()) {
				try {
					validator.validate(o);
				} catch (SchemaValidationException e) {
					throw new BeanRuntimeException(e, null, "Schema validation failed on property ''{0}'': {1}", propertyName, e.getMessage());
				}
			}
			return nn(innerWrite) ? innerWrite.apply(session, o) : o;
		};
	}

	@SuppressWarnings({
		"java:S112" // Rewrap as RuntimeException - ParseException at this level is a programming error in the @Schema input.
	})
	private static JsonMap applyToMap(JsonMap acc, Schema schema) {
		JsonMap m;
		try {
			m = SchemaAnnotation.asMap(schema);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
		if (m == null || m.isEmpty())
			return acc;
		if (acc == null)
			return new JsonMap(m);
		acc.putAll(m);
		return acc;
	}

	private static Class<?> propertyClass(BeanPropertyMeta.Builder b) {
		if (nn(b.rawTypeMeta))
			return b.rawTypeMeta.inner();
		if (nn(b.innerField))
			return b.innerField.getFieldType().inner();
		if (nn(b.getter))
			return b.getter.getReturnType().inner();
		if (nn(b.setter)) {
			var params = b.setter.getParameterTypes();
			if (! params.isEmpty())
				return params.get(0).inner();
		}
		return Object.class;
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
				throw unsupportedOp("Media types on swaps not yet supported on bean properties.");
			if (nn(ps.withTemplate()))
				throw unsupportedOp("Templates on swaps not yet supported on bean properties.");
			return ps;
		}
		if (ci.isAssignableTo(Surrogate.class))
			throw unsupportedOp("Surrogate swaps not yet supported on bean properties.");
		throw rex("Invalid class used in @Swap annotation.  Must be a subclass of ObjectSwap or Surrogate. {0}", cn(c));
	}

	private static ClassInfo owningClass(BeanPropertyMeta.Builder b) {
		if (nn(b.innerField))
			return b.innerField.getDeclaringClass();
		if (nn(b.getter))
			return b.getter.getDeclaringClass();
		if (nn(b.setter))
			return b.setter.getDeclaringClass();
		return null;
	}

	@SuppressWarnings({
		"java:S3776" // @MarshalledProp format-dispatch chain — type-routing if-chain is intentional; splitting hurts JIT inlining.
	})
	private static void applyPropertyFormats(BeanPropertyMeta.Builder b, MarshalledProp mp, Class<?> propertyClass) {
		if (Duration.class.equals(propertyClass) && mp.durationFormat() != DurationFormat.NOT_SET)
			b.swap = durationSwap(mp.durationFormat());
		if (Period.class.equals(propertyClass) && mp.periodFormat() != PeriodFormat.NOT_SET)
			b.swap = periodSwap(mp.periodFormat());
		if (isCalendarType(propertyClass) && mp.calendarFormat() != CalendarFormat.NOT_SET)
			b.swap = calendarSwap(mp.calendarFormat());
		if (Date.class.equals(propertyClass) && mp.dateFormat() != DateFormat.NOT_SET)
			b.swap = dateSwap(mp.dateFormat());
		if (isTemporalType(propertyClass) && mp.temporalFormat() != TemporalFormat.NOT_SET)
			b.swap = temporalSwap(mp.temporalFormat(), propertyClass);
		if (isTemporalAccessorType(propertyClass) && mp.temporalFormat() != TemporalFormat.NOT_SET)
			b.swap = temporalAccessorSwap(mp.temporalFormat(), propertyClass);
		if (isTimeZoneType(propertyClass) && mp.timeZoneFormat() != TimeZoneFormat.NOT_SET)
			b.swap = timeZoneSwap(mp.timeZoneFormat(), propertyClass);
		if (Locale.class.equals(propertyClass) && mp.localeFormat() != LocaleFormat.NOT_SET)
			b.swap = localeSwap(mp.localeFormat());
		if (byte[].class.equals(propertyClass) && mp.binaryFormat() != BinaryFormat.NOT_SET)
			b.swap = binarySwap(mp.binaryFormat());
		if (isEnumType(propertyClass) && mp.enumFormat() != EnumFormat.NOT_SET)
			b.swap = enumSwap(mp.enumFormat(), propertyClass);
		if (UUID.class.equals(propertyClass) && mp.uuidFormat() != UuidFormat.NOT_SET)
			b.swap = uuidSwap(mp.uuidFormat());
		if (isBigNumberType(propertyClass) && mp.bigNumberFormat() != BigNumberFormat.NOT_SET)
			b.swap = bigNumberSwap(mp.bigNumberFormat(), propertyClass);
		if (isBooleanType(propertyClass) && mp.booleanFormat() != BooleanFormat.NOT_SET)
			b.swap = booleanSwap(mp.booleanFormat());
		if (isFloatType(propertyClass) && mp.floatFormat() != FloatFormat.NOT_SET)
			b.swap = floatSwap(mp.floatFormat(), propertyClass);
		if (Currency.class.equals(propertyClass) && mp.currencyFormat() != CurrencyFormat.NOT_SET)
			b.swap = currencySwap(mp.currencyFormat());
		if (Class.class.equals(propertyClass) && mp.classFormat() != ClassFormat.NOT_SET)
			b.swap = classSwap(mp.classFormat());
	}

	@SuppressWarnings({
		"java:S3776" // Class-level @Marshalled format-dispatch chain — same shape as applyPropertyFormats; splitting hurts JIT inlining.
	})
	private static void applyClassFormats(BeanPropertyMeta.Builder b, Marshalled m, Class<?> propertyClass) {
		if (b.swap != null)
			return;
		if (Duration.class.equals(propertyClass) && m.durationFormat() != DurationFormat.NOT_SET)
			b.swap = durationSwap(m.durationFormat());
		else if (Period.class.equals(propertyClass) && m.periodFormat() != PeriodFormat.NOT_SET)
			b.swap = periodSwap(m.periodFormat());
		else if (isCalendarType(propertyClass) && m.calendarFormat() != CalendarFormat.NOT_SET)
			b.swap = calendarSwap(m.calendarFormat());
		else if (Date.class.equals(propertyClass) && m.dateFormat() != DateFormat.NOT_SET)
			b.swap = dateSwap(m.dateFormat());
		else if (isTemporalType(propertyClass) && m.temporalFormat() != TemporalFormat.NOT_SET)
			b.swap = temporalSwap(m.temporalFormat(), propertyClass);
		else if (isTemporalAccessorType(propertyClass) && m.temporalFormat() != TemporalFormat.NOT_SET)
			b.swap = temporalAccessorSwap(m.temporalFormat(), propertyClass);
		else if (isTimeZoneType(propertyClass) && m.timeZoneFormat() != TimeZoneFormat.NOT_SET)
			b.swap = timeZoneSwap(m.timeZoneFormat(), propertyClass);
		else if (Locale.class.equals(propertyClass) && m.localeFormat() != LocaleFormat.NOT_SET)
			b.swap = localeSwap(m.localeFormat());
		else if (byte[].class.equals(propertyClass) && m.binaryFormat() != BinaryFormat.NOT_SET)
			b.swap = binarySwap(m.binaryFormat());
		else if (isEnumType(propertyClass) && m.enumFormat() != EnumFormat.NOT_SET)
			b.swap = enumSwap(m.enumFormat(), propertyClass);
		else if (UUID.class.equals(propertyClass) && m.uuidFormat() != UuidFormat.NOT_SET)
			b.swap = uuidSwap(m.uuidFormat());
		else if (isBigNumberType(propertyClass) && m.bigNumberFormat() != BigNumberFormat.NOT_SET)
			b.swap = bigNumberSwap(m.bigNumberFormat(), propertyClass);
		else if (isBooleanType(propertyClass) && m.booleanFormat() != BooleanFormat.NOT_SET)
			b.swap = booleanSwap(m.booleanFormat());
		else if (isFloatType(propertyClass) && m.floatFormat() != FloatFormat.NOT_SET)
			b.swap = floatSwap(m.floatFormat(), propertyClass);
		else if (Currency.class.equals(propertyClass) && m.currencyFormat() != CurrencyFormat.NOT_SET)
			b.swap = currencySwap(m.currencyFormat());
		else if (Class.class.equals(propertyClass) && m.classFormat() != ClassFormat.NOT_SET)
			b.swap = classSwap(m.classFormat());
	}

	@SuppressWarnings({
		"java:S3776" // Cognitive-complexity threshold is advisory; consolidated per-type dispatch keeps JIT inlining effective per AGENTS.md policy.
	})
	private static void applyContextFormats(BeanPropertyMeta.Builder b, MarshallingContext bc, Class<?> propertyClass) {
		if (Duration.class.equals(propertyClass))
			b.swap = durationSwap(bc.getDurationFormat());
		else if (Period.class.equals(propertyClass))
			b.swap = periodSwap(bc.getPeriodFormat());
		else if (isCalendarType(propertyClass))
			b.swap = calendarSwap(bc.getCalendarFormat());
		else if (Date.class.equals(propertyClass))
			b.swap = dateSwap(bc.getDateFormat());
		else if (isTemporalType(propertyClass))
			b.swap = temporalSwap(bc.getTemporalFormat(), propertyClass);
		else if (isTemporalAccessorType(propertyClass))
			b.swap = temporalAccessorSwap(bc.getTemporalFormat(), propertyClass);
		else if (isTimeZoneType(propertyClass))
			b.swap = timeZoneSwap(bc.getTimeZoneFormat(), propertyClass);
		else if (Locale.class.equals(propertyClass))
			b.swap = localeSwap(bc.getLocaleFormat());
		// byte[] context default: install a per-property swap only when an explicit BinaryFormat is configured
		// (i.e. != NOT_SET).  The per-property swap drives both the marshalling-side serializer path AND the
		// {@link BeanMap} property-level read/write path.  When the context is at NOT_SET we leave byte[]
		// untouched so it falls back to the surrounding serializer's native array handling (e.g. JSON array).
		else if (byte[].class.equals(propertyClass) && bc.getBinaryFormat() != BinaryFormat.NOT_SET)
			b.swap = binarySwap(bc.getBinaryFormat());
		else if (isEnumType(propertyClass) && bc.getEnumFormat() == EnumFormat.ORDINAL)
			// Only install context-level swap for ORDINAL so binary serializers emit a native number.
			// Other formats are handled by ClassMeta.toString() / ClassMeta.newInstanceFromString() against
			// MarshallingContext.getEnumFormat() — installing a String-producing swap on every enum property
			// would mask reflection-driven enum handling for no behavioural benefit.
			b.swap = enumSwap(bc.getEnumFormat(), propertyClass);
		else if (UUID.class.equals(propertyClass) && bc.getUuidFormat() != UuidFormat.STANDARD && bc.getUuidFormat() != UuidFormat.NOT_SET)
			// Install context-level swap only when the configured format diverges from STANDARD (today's
			// default — UUID.toString()).  At STANDARD / NOT_SET, the serializer's natural String-conversion
			// path already produces the right wire form.
			b.swap = uuidSwap(bc.getUuidFormat());
		else if (isBigNumberType(propertyClass) && bc.getBigNumberFormat() != BigNumberFormat.NUMBER && bc.getBigNumberFormat() != BigNumberFormat.NOT_SET)
			// Install context-level swap only for STRING / AUTO.  At NUMBER / NOT_SET, BigInteger /
			// BigDecimal are already emitted as bare numeric tokens by every text serializer.
			b.swap = bigNumberSwap(bc.getBigNumberFormat(), propertyClass);
		else if (isBooleanType(propertyClass) && bc.getBooleanFormat() != BooleanFormat.TRUE_FALSE && bc.getBooleanFormat() != BooleanFormat.NOT_SET)
			// Install context-level swap only when format diverges from TRUE_FALSE (today's default —
			// native boolean wire token).  At TRUE_FALSE / NOT_SET, text serializers' natural boolean
			// emit path already produces the right wire form.
			b.swap = booleanSwap(bc.getBooleanFormat());
		else if (isFloatType(propertyClass) && requiresFloatSwap(bc.getFloatFormat(), propertyClass))
			// Install context-level swap only when the format diverges from Juneau's native bare-numeric
			// emit AND the property type can meaningfully represent the transformed value.  Primitive
			// float/double properties cannot hold null/String results, so we skip the swap and let the
			// native emit handle NaN/Infinity directly.  Explicit per-property or per-class @MarshalledProp /
			// @Marshalled overrides still install via the applyPropertyFormats / applyClassFormats branches.
			b.swap = floatSwap(bc.getFloatFormat(), propertyClass);
		else if (Currency.class.equals(propertyClass) && bc.getCurrencyFormat() != CurrencyFormat.ISO_CODE && bc.getCurrencyFormat() != CurrencyFormat.NOT_SET)
			// Install context-level swap only when format diverges from ISO_CODE (today's default —
			// Currency.toString()).  At ISO_CODE / NOT_SET, the serializer's natural String-conversion
			// path already produces the right wire form.
			b.swap = currencySwap(bc.getCurrencyFormat());
		// Class<?> bean properties fall through to the {@link ClassFormatSwap}
		// registered in {@link DefaultSwaps} — that swap reads the context's
		// classFormat dynamically.  Only @MarshalledProp / @Marshalled overrides install a per-property swap
		// (handled in applyPropertyFormats / applyClassFormats above).
	}

	private static boolean isCalendarType(Class<?> c) {
		return c != null && Calendar.class.isAssignableFrom(c) && c != XMLGregorianCalendar.class;
	}

	private static boolean isTemporalType(Class<?> c) {
		return c != null && Temporal.class.isAssignableFrom(c);
	}

	private static boolean isTemporalAccessorType(Class<?> c) {
		// Returns true for non-Temporal TemporalAccessor subclasses.  Currently this is just MonthDay
		// (the only java.time.* TemporalAccessor that is not also a Temporal); the predicate is written
		// as the negative-space complement so a future JDK addition is picked up automatically.
		return c != null && TemporalAccessor.class.isAssignableFrom(c) && ! Temporal.class.isAssignableFrom(c);
	}

	private static boolean isTimeZoneType(Class<?> c) {
		return TimeZone.class.equals(c) || (c != null && ZoneId.class.isAssignableFrom(c));
	}

	private static boolean isEnumType(Class<?> c) {
		return c != null && c.isEnum();
	}

	private static boolean isBigNumberType(Class<?> c) {
		return BigInteger.class.equals(c) || BigDecimal.class.equals(c);
	}

	private static boolean isBooleanType(Class<?> c) {
		return Boolean.class.equals(c) || boolean.class.equals(c);
	}

	private static boolean isFloatType(Class<?> c) {
		return Float.class.equals(c) || float.class.equals(c) || Double.class.equals(c) || double.class.equals(c);
	}

	private static boolean requiresFloatSwap(FloatFormat format, Class<?> propertyClass) {
		// Primitives can't hold a String or null transform result without breaking Juneau's
		// null-to-primitive-default convention.  Only install for boxed Float/Double here.
		if (float.class.equals(propertyClass) || double.class.equals(propertyClass))
			return false;
		return format == FloatFormat.NaN_AS_NULL || format == FloatFormat.NaN_AS_STRING || format == FloatFormat.NaN_AS_ERROR;
	}

	private static ObjectSwap<?,?> durationSwap(DurationFormat format) {
		return new ObjectSwap<>(Duration.class, Object.class) {
			@Override /* ObjectSwap */
			public Object swap(MarshallingSession session, Duration o) {
				if (o == null)
					return null;
				var s = format.format(o);
				if (format.isNumeric()) {
					if (format == DurationFormat.SECONDS)
						return Double.valueOf(s);
					return Long.valueOf(s);
				}
				return s;
			}

			@Override /* ObjectSwap */
			public Duration unswap(MarshallingSession session, Object o, ClassMeta<?> hint) {
				if (o == null)
					return null;
				return format.parse(o.toString());
			}
		};
	}

	private static ObjectSwap<?,?> periodSwap(PeriodFormat format) {
		return new ObjectSwap<>(Period.class, Object.class) {
			@Override /* ObjectSwap */
			public Object swap(MarshallingSession session, Period o) {
				if (o == null)
					return null;
				var s = format.format(o);
				return format == PeriodFormat.DAYS ? Integer.valueOf(s) : s;
			}

			@Override /* ObjectSwap */
			public Period unswap(MarshallingSession session, Object o, ClassMeta<?> hint) {
				if (o == null)
					return null;
				return format.parse(o.toString());
			}
		};
	}

	private static ObjectSwap<?,?> calendarSwap(CalendarFormat format) {
		return new ObjectSwap<>(Calendar.class, Object.class) {
			@Override /* ObjectSwap */
			public Object swap(MarshallingSession session, Calendar o) {
				if (o == null)
					return null;
				var s = format.format(o, session.getTimeZoneId());
				return format.isNumeric() ? Long.valueOf(s) : s;
			}

			@Override /* ObjectSwap */
			public Calendar unswap(MarshallingSession session, Object o, ClassMeta<?> hint) {
				if (o == null)
					return null;
				return format.parse(o.toString(), session.getTimeZoneId());
			}
		};
	}

	private static ObjectSwap<?,?> dateSwap(DateFormat format) {
		return new ObjectSwap<>(Date.class, Object.class) {
			@Override /* ObjectSwap */
			public Object swap(MarshallingSession session, Date o) {
				if (o == null)
					return null;
				var s = format.format(o, session.getTimeZoneId());
				return format.isNumeric() ? Long.valueOf(s) : s;
			}

			@Override /* ObjectSwap */
			public Date unswap(MarshallingSession session, Object o, ClassMeta<?> hint) {
				if (o == null)
					return null;
				return format.parse(o.toString(), session.getTimeZoneId());
			}
		};
	}

	@SuppressWarnings({
		"unchecked" // Temporal subtype is determined at runtime via the bean property's class meta
	})
	private static ObjectSwap<?,?> temporalSwap(TemporalFormat format, Class<?> propertyClass) {
		var temporalType = (Class<? extends Temporal>) propertyClass;
		return new ObjectSwap<>(Temporal.class, Object.class) {
			@Override /* ObjectSwap */
			public Object swap(MarshallingSession session, Temporal o) {
				if (o == null)
					return null;
				var s = format.format(o, session.getTimeZoneId());
				// MILLIS coercion mirrors TemporalFormat.format()'s actual output shape — see
				// TemporalFormat.isMillisNumeric for the canonical decision (the only types that emit
				// a numeric string at MILLIS, vs the carve-outs that fall back to DEFAULT's ISO string).
				if (format == TemporalFormat.MILLIS && TemporalFormat.isMillisNumeric(temporalType))
					return Long.valueOf(s);
				return s;
			}

			@Override /* ObjectSwap */
			public Temporal unswap(MarshallingSession session, Object o, ClassMeta<?> hint) {
				if (o == null)
					return null;
				return format.parse(o.toString(), temporalType, session.getTimeZoneId());
			}
		};
	}

	@SuppressWarnings({
		"unchecked" // TemporalAccessor subtype is determined at runtime via the bean property's class meta
	})
	private static ObjectSwap<?,?> temporalAccessorSwap(TemporalFormat format, Class<?> propertyClass) {
		// Sibling of temporalSwap parameterized on TemporalAccessor at the source class so it accepts
		// non-Temporal TemporalAccessor subclasses (currently MonthDay).  TemporalFormat.format / .parse
		// accept TemporalAccessor directly, so the swap body mirrors temporalSwap exactly.
		var temporalType = (Class<? extends TemporalAccessor>) propertyClass;
		return new ObjectSwap<>(TemporalAccessor.class, Object.class) {
			@Override /* ObjectSwap */
			public Object swap(MarshallingSession session, TemporalAccessor o) {
				if (o == null)
					return null;
				var s = format.format(o, session.getTimeZoneId());
				if (format == TemporalFormat.MILLIS && TemporalFormat.isMillisNumeric(temporalType))
					return Long.valueOf(s);
				return s;
			}

			@Override /* ObjectSwap */
			public TemporalAccessor unswap(MarshallingSession session, Object o, ClassMeta<?> hint) {
				if (o == null)
					return null;
				return format.parse(o.toString(), temporalType, session.getTimeZoneId());
			}
		};
	}

	private static ObjectSwap<?,?> timeZoneSwap(TimeZoneFormat format, Class<?> propertyClass) {
		if (TimeZone.class.equals(propertyClass))
			return new ObjectSwap<TimeZone,String>(TimeZone.class, String.class) {
				@Override /* ObjectSwap */
				public String swap(MarshallingSession session, TimeZone o) {
					return format.format(o);
				}

				@Override /* ObjectSwap */
				public TimeZone unswap(MarshallingSession session, String o, ClassMeta<?> hint) {
					return TimeZoneFormat.parseTimeZone(o);
				}
			};
		return new ObjectSwap<ZoneId,String>(ZoneId.class, String.class) {
			@Override /* ObjectSwap */
			public String swap(MarshallingSession session, ZoneId o) {
				return format.format(o);
			}

			@Override /* ObjectSwap */
			public ZoneId unswap(MarshallingSession session, String o, ClassMeta<?> hint) {
				return TimeZoneFormat.parseZoneId(o);
			}
		};
	}

	private static ObjectSwap<?,?> localeSwap(LocaleFormat format) {
		return new ObjectSwap<Locale,String>(Locale.class, String.class) {
			@Override /* ObjectSwap */
			public String swap(MarshallingSession session, Locale o) {
				return format.format(o);
			}

			@Override /* ObjectSwap */
			public Locale unswap(MarshallingSession session, String o, ClassMeta<?> hint) {
				return format.parse(o);
			}
		};
	}

	private static ObjectSwap<?,?> binarySwap(BinaryFormat format) {
		// Variant swap class (Object) — binary serializers with a native byte-array wire type
		// (hasNativeBytes()==true: BSON / CBOR / MsgPack) receive raw byte[]; binary serializers
		// without a native byte-array wire type (hasNativeBytes()==false: Parquet, binary RDF) and
		// textual sessions receive the formatted String.  Mirrors the variant-output pattern used by
		// {@link #calendarSwap(CalendarFormat)} / {@link #temporalSwap(TemporalFormat, Class)}.  See
		return new ObjectSwap<>(byte[].class, Object.class) {
			@Override /* ObjectSwap */
			public Object swap(MarshallingSession session, byte[] o) {
				if (o == null)
					return null;
				// Binary serializers with native byte-array wire type pass raw bytes through.  Binary
				// serializers without native byte-array wire type (Parquet, binary RDF) fall through to
				// format.format(o) and emit the configured BinaryFormat's text wire form.
				if (session instanceof OutputStreamSerializerSession oss && oss.hasNativeBytes())
					return o;
				// CSV defers to its own CsvByteArrayCellFormat cell encoding.
				// OpenAPI applies its own schema-directed BYTE / BINARY / BINARY_SPACED encoding.
				if (session instanceof CsvSerializerSession || session instanceof OpenApiSerializerSession)
					return o;
				return format.format(o);
			}

			@Override /* ObjectSwap */
			@SuppressWarnings({
				"java:S1168" // Returning null is part of the contract — callers branch on null to detect 'no swap installed'.
			})
			public byte[] unswap(MarshallingSession session, Object o, ClassMeta<?> hint) {
				if (o == null)
					return null;
				if (o instanceof byte[] b)
					return b;
				return format.parse(o.toString());
			}
		};
	}

	@SuppressWarnings({
		"unchecked", "rawtypes" // Enum subtype is determined at runtime via the bean property's class meta
	})
	private static ObjectSwap<?,?> enumSwap(EnumFormat format, Class<?> propertyClass) {
		var enumType = (Class<? extends Enum>) propertyClass;
		if (format.isNumeric()) {
			return new ObjectSwap<Enum,Number>(Enum.class, Number.class) {
				@Override /* ObjectSwap */
				public Number swap(MarshallingSession session, Enum o) {
					if (o == null)
						return null;
					return Integer.valueOf(format.format(o));
				}

				@Override /* ObjectSwap */
				public Enum unswap(MarshallingSession session, Number o, ClassMeta<?> hint) {
					if (o == null)
						return null;
					return EnumFormat.parse(o.toString(), enumType);
				}
			};
		}
		return new ObjectSwap<Enum,String>(Enum.class, String.class) {
			@Override /* ObjectSwap */
			public String swap(MarshallingSession session, Enum o) {
				return format.format(o);
			}

			@Override /* ObjectSwap */
			public Enum unswap(MarshallingSession session, String o, ClassMeta<?> hint) {
				if (o == null)
					return null;
				return EnumFormat.parse(o, enumType);
			}
		};
	}

	private static ObjectSwap<?,?> uuidSwap(UuidFormat format) {
		return new ObjectSwap<UUID,String>(UUID.class, String.class) {
			@Override /* ObjectSwap */
			public String swap(MarshallingSession session, UUID o) {
				return UuidFormat.format(o, format);
			}

			@Override /* ObjectSwap */
			public UUID unswap(MarshallingSession session, String o, ClassMeta<?> hint) {
				return UuidFormat.parse(o, format);
			}
		};
	}

	@SuppressWarnings({
		"java:S3776" // Cognitive-complexity threshold is advisory; consolidated BigInteger/BigDecimal dispatch keeps JIT inlining effective per AGENTS.md policy.
	})
	private static ObjectSwap<?,?> bigNumberSwap(BigNumberFormat format, Class<?> propertyClass) {
		// Variant swap class (Object) — text serializers may receive either a Number (for NUMBER / AUTO-safe
		// values) or a String (for STRING / AUTO-out-of-range values); binary serializers always receive the
		// native BigInteger / BigDecimal so they can emit native numeric types.
		if (BigInteger.class.equals(propertyClass)) {
			return new ObjectSwap<>(BigInteger.class, Object.class) {
				@Override /* ObjectSwap */
				public Object swap(MarshallingSession session, BigInteger o) {
					if (o == null)
						return null;
					if (session instanceof OutputStreamSerializerSession)
						return o;
					return BigNumberFormat.format(o, format);
				}

				@Override /* ObjectSwap */
				public BigInteger unswap(MarshallingSession session, Object o, ClassMeta<?> hint) {
					if (o == null)
						return null;
					if (o instanceof BigInteger bi)
						return bi;
					if (o instanceof Number n)
						return new BigInteger(n.toString());
					return BigNumberFormat.parse(o.toString(), format, BigInteger.class);
				}
			};
		}
		return new ObjectSwap<>(BigDecimal.class, Object.class) {
			@Override /* ObjectSwap */
			public Object swap(MarshallingSession session, BigDecimal o) {
				if (o == null)
					return null;
				if (session instanceof OutputStreamSerializerSession)
					return o;
				return BigNumberFormat.format(o, format);
			}

			@Override /* ObjectSwap */
			public BigDecimal unswap(MarshallingSession session, Object o, ClassMeta<?> hint) {
				if (o == null)
					return null;
				if (o instanceof BigDecimal bd)
					return bd;
				if (o instanceof Number n)
					return new BigDecimal(n.toString());
				return BigNumberFormat.parse(o.toString(), format, BigDecimal.class);
			}
		};
	}

	private static ObjectSwap<?,?> booleanSwap(BooleanFormat format) {
		// Variant swap class (Object) — binary serializers receive the native Boolean, textual sessions
		// receive a Boolean / Integer / String depending on the format constant.
		return new ObjectSwap<>(Boolean.class, Object.class) {
			@Override /* ObjectSwap */
			public Object swap(MarshallingSession session, Boolean o) {
				if (o == null)
					return null;
				// Binary serializers (BSON / CBOR / MsgPack / Proto / Parquet) emit native bool regardless.
				if (session instanceof OutputStreamSerializerSession)
					return o;
				return BooleanFormat.format(o.booleanValue(), format);
			}

			@Override /* ObjectSwap */
			@SuppressWarnings({
				"java:S2447" // Null passthrough is the ObjectSwap.unswap contract (null/blank input → null); the generic override is bound to boxed Boolean, mirrors sibling binarySwap's java:S1168 suppression.
			})
			public Boolean unswap(MarshallingSession session, Object o, ClassMeta<?> hint) {
				if (o == null)
					return null;
				if (o instanceof Boolean b)
					return b;
				if (o instanceof Number n)
					return n.intValue() != 0;
				var str = o.toString();
				if (str.trim().isEmpty())
					return null;
				return BooleanFormat.parse(str, format);
			}
		};
	}

	@SuppressWarnings({
		"java:S3776" // Cognitive-complexity threshold is advisory; consolidated dispatch keeps JIT inlining effective per AGENTS.md policy.
	})
	private static ObjectSwap<?,?> floatSwap(FloatFormat format, Class<?> propertyClass) {
		// Variant swap class (Object) — binary serializers receive native IEEE-754; textual sessions
		// receive a boxed Float / Double, a String, or null depending on the format and value.
		var isFloatType = Float.class.equals(propertyClass) || float.class.equals(propertyClass);
		var swapNormal = numberClass(isFloatType ? Float.class : Double.class);
		return new ObjectSwap<>(swapNormal, Object.class) {
			@Override /* ObjectSwap */
			public int match(MarshallingSession session) {
				// Null sessions arrive from BeanMap.set / type-conversion lookup paths — keep Juneau's
				// native float/double conversion semantics (null → primitive default, blank → 0.0) intact.
				if (session == null)
					return 0;
				return super.match(session);
			}

			@Override /* ObjectSwap */
			public Object swap(MarshallingSession session, Number o) {
				if (o == null)
					return null;
				// Binary serializers always emit native IEEE-754 (including native NaN / Infinity tags).
				if (session instanceof OutputStreamSerializerSession)
					return o;
				if (o instanceof Float f)
					return FloatFormat.format(f.floatValue(), format);
				return FloatFormat.format(o.doubleValue(), format);
			}

			@Override /* ObjectSwap */
			public Number unswap(MarshallingSession session, Object o, ClassMeta<?> hint) {
				// null passes through unchanged — the bean machinery's primitive-default handling
				// (null → 0.0 for primitive float/double) and boxed-type null semantics both rely on
				// receiving the original null, not a swap-synthesized NaN.
				if (o == null)
					return null;
				if (o instanceof Number n && Float.class.equals(propertyClass))
					return n.floatValue();
				if (o instanceof Number n)
					return n.doubleValue();
				var s = o.toString();
				// Preserve Juneau's lenient parsing convention: empty / blank string maps to zero for
				// boxed Float / Double properties (matches the no-swap path's historical behaviour).
				if (s.isBlank()) {
					if (isFloatType)
						return Float.valueOf(0.0f);
					return Double.valueOf(0.0d);
				}
				if (isFloatType)
					return FloatFormat.parse(s, format, Float.class);
				return FloatFormat.parse(s, format, Double.class);
			}
		};
	}

	@SuppressWarnings({
		"unchecked" // Runtime branch guarantees Float.class or Double.class, both Number subclasses.
	})
	private static Class<Number> numberClass(Class<? extends Number> c) {
		return (Class<Number>)c;
	}

	private static ObjectSwap<?,?> currencySwap(CurrencyFormat format) {
		return new ObjectSwap<Currency,String>(Currency.class, String.class) {
			@Override /* ObjectSwap */
			public String swap(MarshallingSession session, Currency o) {
				return CurrencyFormat.format(o, format, session.getLocale());
			}

			@Override /* ObjectSwap */
			public Currency unswap(MarshallingSession session, String o, ClassMeta<?> hint) {
				return CurrencyFormat.parse(o, format, session.getLocale());
			}
		};
	}

	private static ObjectSwap<?,?> classSwap(ClassFormat format) {
		return new ObjectSwap<Class<?>,String>(asClassWildcard(), String.class) {
			@Override /* ObjectSwap */
			public String swap(MarshallingSession session, Class<?> o) {
				return ClassFormat.format(o, format);
			}

			@Override /* ObjectSwap */
			public Class<?> unswap(MarshallingSession session, String o, ClassMeta<?> hint) {
				ClassLoader cl = session != null ? session.getClassLoader() : null;
				if (cl == null)
					cl = Thread.currentThread().getContextClassLoader();
				return ClassFormat.parse(o, format, cl);
			}
		};
	}

	@SuppressWarnings({
		"unchecked", "rawtypes" // ObjectSwap constructor requires a concrete Class<T>; the wildcard captures Class.class.
	})
	private static Class<Class<?>> asClassWildcard() {
		return (Class) Class.class;
	}

	@SuppressWarnings({
		"java:S112" // Rewrap DatatypeConfigurationException as RuntimeException; we cannot recover at the swap level
	})
	private static ObjectSwap<?,?> xmlGregorianCalendarSwap() {
		final DatatypeFactory df;
		try {
			df = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			throw new RuntimeException(e);
		}
		return new ObjectSwap<XMLGregorianCalendar,String>(XMLGregorianCalendar.class, String.class) {
			@Override /* ObjectSwap */
			public String swap(MarshallingSession session, XMLGregorianCalendar o) {
				return o == null ? null : o.toXMLFormat();
			}

			@Override /* ObjectSwap */
			public XMLGregorianCalendar unswap(MarshallingSession session, String o, ClassMeta<?> hint) {
				if (o == null)
					return null;
				var s = o.trim();
				return s.isEmpty() ? null : df.newXMLGregorianCalendar(s);
			}
		};
	}
}
