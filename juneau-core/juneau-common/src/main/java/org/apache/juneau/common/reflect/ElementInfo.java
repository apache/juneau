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
package org.apache.juneau.common.reflect;

import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.ThrowableUtils.*;

import java.lang.annotation.*;
import java.lang.reflect.Modifier;

/**
 * Base class for all reflection wrapper objects.
 *
 * <p>
 * Provides common modifier checking functionality for classes, methods, fields, constructors, and other reflection elements.
 *
 */
public abstract class ElementInfo {

	private final int modifiers;

	/**
	 * Constructor.
	 *
	 * @param modifiers The Java modifiers for this element.
	 */
	protected ElementInfo(int modifiers) {
		this.modifiers = modifiers;
	}

	/**
	 * Returns the Java language modifiers for this element.
	 *
	 * @return The Java language modifiers for this element.
	 */
	public int getModifiers() { return modifiers; }

	/**
	 * Returns <jk>true</jk> if the specified flag is applicable to this element.
	 *
	 * <p>
	 * Subclasses should override this method and call {@code super.is(flag)} to handle common modifier flags,
	 * then handle their own specific flags.
	 *
	 * @param flag The flag to test for.
	 * @return <jk>true</jk> if the specified flag is applicable to this element.
	 */
	public boolean is(ElementFlag flag) {
		return switch (flag) {
			case PUBLIC -> isPublic();
			case NOT_PUBLIC -> isNotPublic();
			case PRIVATE -> isPrivate();
			case NOT_PRIVATE -> isNotPrivate();
			case PROTECTED -> isProtected();
			case NOT_PROTECTED -> isNotProtected();
			case STATIC -> isStatic();
			case NOT_STATIC -> isNotStatic();
			case FINAL -> isFinal();
			case NOT_FINAL -> isNotFinal();
			case SYNCHRONIZED -> isSynchronized();
			case NOT_SYNCHRONIZED -> isNotSynchronized();
			case VOLATILE -> isVolatile();
			case NOT_VOLATILE -> isNotVolatile();
			case TRANSIENT -> isTransient();
			case NOT_TRANSIENT -> isNotTransient();
			case NATIVE -> isNative();
			case NOT_NATIVE -> isNotNative();
			case INTERFACE -> isInterface();
			case ABSTRACT -> isAbstract();
			case NOT_ABSTRACT -> isNotAbstract();
			case STRICT -> isStrict();
			case NOT_STRICT -> isNotStrict();
			default -> throw rex("Invalid flag for element: {0}", flag);
		};
	}

	/**
	 * Returns <jk>true</jk> if all specified flags are applicable to this element.
	 *
	 * <p>
	 * Subclasses should override this method and call {@code super.isAll(flags)} to handle common modifier flags,
	 * then handle their own specific flags.
	 *
	 * @param flags The flags to test for.
	 * @return <jk>true</jk> if all specified flags are applicable to this element.
	 */
	public boolean isAll(ElementFlag...flags) {
		return stream(flags).allMatch(this::is);
	}

	/**
	 * Returns <jk>true</jk> if any of the specified flags are applicable to this element.
	 *
	 * <p>
	 * Subclasses should override this method and call {@code super.isAny(flags)} to handle common modifier flags,
	 * then handle their own specific flags.
	 *
	 * @param flags The flags to test for.
	 * @return <jk>true</jk> if any of the specified flags are applicable to this element.
	 */
	public boolean isAny(ElementFlag...flags) {
		return stream(flags).anyMatch(this::is);
	}

	/**
	 * Returns <jk>true</jk> if this element is public.
	 *
	 * @return <jk>true</jk> if this element is public.
	 */
	public boolean isPublic() { return Modifier.isPublic(modifiers); }

	/**
	 * Returns <jk>true</jk> if this element is not public.
	 *
	 * @return <jk>true</jk> if this element is not public.
	 */
	public boolean isNotPublic() { return ! Modifier.isPublic(modifiers); }

	/**
	 * Returns <jk>true</jk> if this element is private.
	 *
	 * @return <jk>true</jk> if this element is private.
	 */
	public boolean isPrivate() { return Modifier.isPrivate(modifiers); }

	/**
	 * Returns <jk>true</jk> if this element is not private.
	 *
	 * @return <jk>true</jk> if this element is not private.
	 */
	public boolean isNotPrivate() { return ! Modifier.isPrivate(modifiers); }

	/**
	 * Returns <jk>true</jk> if this element is protected.
	 *
	 * @return <jk>true</jk> if this element is protected.
	 */
	public boolean isProtected() { return Modifier.isProtected(modifiers); }

	/**
	 * Returns <jk>true</jk> if this element is not protected.
	 *
	 * @return <jk>true</jk> if this element is not protected.
	 */
	public boolean isNotProtected() { return ! Modifier.isProtected(modifiers); }

	/**
	 * Returns <jk>true</jk> if this element is static.
	 *
	 * @return <jk>true</jk> if this element is static.
	 */
	public boolean isStatic() { return Modifier.isStatic(modifiers); }

	/**
	 * Returns <jk>true</jk> if this element is not static.
	 *
	 * @return <jk>true</jk> if this element is not static.
	 */
	public boolean isNotStatic() { return ! Modifier.isStatic(modifiers); }

	/**
	 * Returns <jk>true</jk> if this element is final.
	 *
	 * @return <jk>true</jk> if this element is final.
	 */
	public boolean isFinal() { return Modifier.isFinal(modifiers); }

	/**
	 * Returns <jk>true</jk> if this element is not final.
	 *
	 * @return <jk>true</jk> if this element is not final.
	 */
	public boolean isNotFinal() { return ! Modifier.isFinal(modifiers); }

	/**
	 * Returns <jk>true</jk> if this element is synchronized.
	 *
	 * @return <jk>true</jk> if this element is synchronized.
	 */
	public boolean isSynchronized() { return Modifier.isSynchronized(modifiers); }

	/**
	 * Returns <jk>true</jk> if this element is not synchronized.
	 *
	 * @return <jk>true</jk> if this element is not synchronized.
	 */
	public boolean isNotSynchronized() { return ! Modifier.isSynchronized(modifiers); }

	/**
	 * Returns <jk>true</jk> if this element is volatile.
	 *
	 * @return <jk>true</jk> if this element is volatile.
	 */
	public boolean isVolatile() { return Modifier.isVolatile(modifiers); }

	/**
	 * Returns <jk>true</jk> if this element is not volatile.
	 *
	 * @return <jk>true</jk> if this element is not volatile.
	 */
	public boolean isNotVolatile() { return ! Modifier.isVolatile(modifiers); }

	/**
	 * Returns <jk>true</jk> if this element is transient.
	 *
	 * @return <jk>true</jk> if this element is transient.
	 */
	public boolean isTransient() { return Modifier.isTransient(modifiers); }

	/**
	 * Returns <jk>true</jk> if this element is not transient.
	 *
	 * @return <jk>true</jk> if this element is not transient.
	 */
	public boolean isNotTransient() { return ! Modifier.isTransient(modifiers); }

	/**
	 * Returns <jk>true</jk> if this element is native.
	 *
	 * @return <jk>true</jk> if this element is native.
	 */
	public boolean isNative() { return Modifier.isNative(modifiers); }

	/**
	 * Returns <jk>true</jk> if this element is not native.
	 *
	 * @return <jk>true</jk> if this element is not native.
	 */
	public boolean isNotNative() { return ! Modifier.isNative(modifiers); }

	/**
	 * Returns <jk>true</jk> if this element is an interface.
	 *
	 * @return <jk>true</jk> if this element is an interface.
	 */
	public boolean isInterface() { return Modifier.isInterface(modifiers); }

	/**
	 * Returns <jk>true</jk> if this element is not an interface.
	 *
	 * @return <jk>true</jk> if this element is not an interface.
	 */
	public boolean isNotInterface() { return ! Modifier.isInterface(modifiers); }

	/**
	 * Returns <jk>true</jk> if this element is abstract.
	 *
	 * @return <jk>true</jk> if this element is abstract.
	 */
	public boolean isAbstract() { return Modifier.isAbstract(modifiers); }

	/**
	 * Returns <jk>true</jk> if this element is not abstract.
	 *
	 * @return <jk>true</jk> if this element is not abstract.
	 */
	public boolean isNotAbstract() { return ! Modifier.isAbstract(modifiers); }

	/**
	 * Returns <jk>true</jk> if this element is strict.
	 *
	 * @return <jk>true</jk> if this element is strict.
	 */
	public boolean isStrict() { return Modifier.isStrict(modifiers); }

	/**
	 * Returns <jk>true</jk> if this element is not strict.
	 *
	 * @return <jk>true</jk> if this element is not strict.
	 */
	public boolean isNotStrict() { return ! Modifier.isStrict(modifiers); }

	//-----------------------------------------------------------------------------------------------------------------
	// Helper methods
	//-----------------------------------------------------------------------------------------------------------------

	protected <A extends Annotation> AnnotationInfo<A> ai(Annotatable on, A value) {
		return AnnotationInfo.of(on, value);
	}
}
