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
package org.apache.juneau;

import static org.apache.juneau.collections.JsonMap.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.ClassUtils.*;

import java.text.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.internal.*;

/**
 * ContextSession that lives for the duration of a single use of {@link BeanTraverseContext}.
 *
 * <p>
 * Used by serializers and other classes that traverse POJOs for the following purposes:
 * <ul class='spaced-list'>
 * 	<li>
 * 		Keeping track of how deep it is in a model for indentation purposes.
 * 	<li>
 * 		Ensuring infinite loops don't occur by setting a limit on how deep to traverse a model.
 * 	<li>
 * 		Ensuring infinite loops don't occur from loops in the model (when detectRecursions is enabled.
 * </ul>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class BeanTraverseSession extends BeanSession {

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public static abstract class Builder extends BeanSession.Builder {

		BeanTraverseContext ctx;
		int initialDepth;

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 */
		protected Builder(BeanTraverseContext ctx) {
			super(ctx.getBeanContext());
			this.ctx = ctx;
			initialDepth = ctx.initialDepth;
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

		// </FluentSetters>
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final BeanTraverseContext ctx;
	private final Map<Object,Object> set;                                           // Contains the current objects in the current branch of the model.
	private final LinkedList<StackElement> stack = new LinkedList<>();              // Contains the current objects in the current branch of the model.

	// Writable properties
	private boolean isBottom;                                                       // If 'true', then we're at a leaf in the model (i.e. a String, Number, Boolean, or null).
	private BeanPropertyMeta currentProperty;
	private ClassMeta<?> currentClass;

	/** The current indentation depth into the model. */
	public int indent;

	private int depth;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected BeanTraverseSession(Builder builder) {
		super(builder);
		ctx = builder.ctx;
		indent =  builder.initialDepth;
		if (isDetectRecursions() || isDebug()) {
			set = new IdentityHashMap<>();
		} else {
			set = Collections.emptyMap();
		}
	}

	/**
	 * Sets the current bean property being traversed for proper error messages.
	 *
	 * @param currentProperty The current property being traversed.
	 */
	protected final void setCurrentProperty(BeanPropertyMeta currentProperty) {
		this.currentProperty = currentProperty;
	}

	/**
	 * Sets the current class being traversed for proper error messages.
	 *
	 * @param currentClass The current class being traversed.
	 */
	protected final void setCurrentClass(ClassMeta<?> currentClass) {
		this.currentClass = currentClass;
	}

	/**
	 * Push the specified object onto the stack.
	 *
	 * @param attrName The attribute name.
	 * @param o The current object being traversed.
	 * @param eType The expected class type.
	 * @return
	 * 	The {@link ClassMeta} of the object so that <c>instanceof</c> operations only need to be performed
	 * 	once (since they can be expensive).
	 * @throws BeanRecursionException If recursion occurred.
	 */
	protected final ClassMeta<?> push(String attrName, Object o, ClassMeta<?> eType) throws BeanRecursionException {
		indent++;
		depth++;
		isBottom = true;
		if (o == null)
			return null;
		Class<?> c = o.getClass();
		ClassMeta<?> cm = (eType != null && c == eType.getInnerClass()) ? eType : ((o instanceof ClassMeta) ? (ClassMeta<?>)o : getClassMeta(c));
		if (cm.isCharSequence() || cm.isNumber() || cm.isBoolean())
			return cm;
		if (depth > getMaxDepth())
			return null;
		if (isDetectRecursions() || isDebug()) {
			if (willRecurse(attrName, o, cm))
				return null;
			isBottom = false;
			stack.add(new StackElement(stack.size(), attrName, o, cm));
			set.put(o, o);
		}
		return cm;
	}

	/**
	 * Returns <jk>true</jk> if we're processing the root node.
	 *
	 * <p>
	 * Must be called after {@link #push(String, Object, ClassMeta)} and before {@link #pop()}.
	 *
	 * @return <jk>true</jk> if we're processing the root node.
	 */
	protected final boolean isRoot() {
		return depth == 1;
	}

	/**
	 * Returns <jk>true</jk> if {@link BeanTraverseContext.Builder#detectRecursions()} is enabled, and the specified
	 * object is already higher up in the traversal chain.
	 *
	 * @param attrName The bean property attribute name, or some other identifier.
	 * @param o The object to check for recursion.
	 * @param cm The metadata on the object class.
	 * @return <jk>true</jk> if recursion detected.
	 * @throws BeanRecursionException If recursion occurred.
	 */
	protected final boolean willRecurse(String attrName, Object o, ClassMeta<?> cm) throws BeanRecursionException {
		if (! (isDetectRecursions() || isDebug()) || ! set.containsKey(o))
			return false;
		if (isIgnoreRecursions() && ! isDebug())
			return true;

		stack.add(new StackElement(stack.size(), attrName, o, cm));
		throw new BeanRecursionException("Recursion occurred, stack={0}", getStack(true));
	}

	/**
	 * Returns <jk>true</jk> if we're about to exceed the max depth for the document.
	 *
	 * @return <jk>true</jk> if we're about to exceed the max depth for the document.
	 */
	protected final boolean willExceedDepth() {
		return (depth >= getMaxDepth());
	}

	/**
	 * Pop an object off the stack.
	 */
	protected final void pop() {
		indent--;
		depth--;
		if ((isDetectRecursions() || isDebug()) && ! isBottom)  {
			Object o = stack.removeLast().o;
			Object o2 = set.remove(o);
			if (o2 == null)
				onError(null, "Couldn't remove object of type ''{0}'' on attribute ''{1}'' from object stack.", className(o), stack);
		}
		isBottom = false;
	}

	/**
	 * Same as {@link ClassMeta#isOptional()} but gracefully handles a null {@link ClassMeta}.
	 *
	 * @param cm The meta to check.
	 * @return <jk>true</jk> if the specified meta is an {@link Optional}.
	 */
	protected final boolean isOptional(ClassMeta<?> cm) {
		return (cm != null && cm.isOptional());
	}

	/**
	 * Returns the inner type of an {@link Optional}.
	 *
	 * @param cm The meta to check.
	 * @return The inner type of an {@link Optional}.
	 */
	protected final ClassMeta<?> getOptionalType(ClassMeta<?> cm) {
		if (cm.isOptional())
			return getOptionalType(cm.getElementType());
		return cm;
	}

	/**
	 * If the specified object is an {@link Optional}, returns the inner object.
	 *
	 * @param o The object to check.
	 * @return The inner object if it's an {@link Optional}, <jk>null</jk> if it's <jk>null</jk>, or else the same object.
	 */
	protected final Object getOptionalValue(Object o) {
		if (o == null)
			return null;
		if (o instanceof Optional)
			return getOptionalValue(((Optional<?>)o).orElse(null));
		return o;
	}

	/**
	 * Logs a warning message.
	 *
	 * @param t The throwable that was thrown (if there was one).
	 * @param msg The warning message.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	protected void onError(Throwable t, String msg, Object... args) {
		super.addWarning(msg, args);
	}

	private final class StackElement {
		final int depth;
		final String name;
		final Object o;
		final ClassMeta<?> aType;

		StackElement(int depth, String name, Object o, ClassMeta<?> aType) {
			this.depth = depth;
			this.name = name;
			this.o = o;
			this.aType = aType;
		}

		String toString(boolean simple) {
			StringBuilder sb = new StringBuilder().append('[').append(depth).append(']').append(' ');
			sb.append(isEmpty(name) ? "<noname>" : name).append(':');
			sb.append(aType.toString(simple));
			if (aType != aType.getSerializedClassMeta(BeanTraverseSession.this))
				sb.append('/').append(aType.getSerializedClassMeta(BeanTraverseSession.this).toString(simple));
			return sb.toString();
		}
	}

	/**
	 * Returns the current stack trace.
	 *
	 * @param full
	 * 	If <jk>true</jk>, returns a full stack trace.
	 * @return The current stack trace.
	 */
	protected String getStack(boolean full) {
		StringBuilder sb = new StringBuilder();
		stack.forEach(x -> {
			if (full) {
				sb.append("\n\t");
				for (int i = 1; i < x.depth; i++)
					sb.append("  ");
				if (x.depth > 0)
					sb.append("->");
				sb.append(x.toString(false));
			} else {
				sb.append(" > ").append(x.toString(true));
			}
		});
		return sb.toString();
	}

	/**
	 * Returns information used to determine at what location in the parse a failure occurred.
	 *
	 * @return A map, typically containing something like <c>{line:123,column:456,currentProperty:"foobar"}</c>
	 */
	public final JsonMap getLastLocation() {
		Predicate<Object> nn = ObjectUtils::isNotNull;
		Predicate<Collection<?>> nec = CollectionUtils::isNotEmpty;
		return JsonMap.create()
			.appendIf(nn, "currentClass", currentClass)
			.appendIf(nn, "currentProperty", currentProperty)
			.appendIf(nec, "stack", stack);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Automatically detect POJO recursions.
	 *
	 * @see BeanTraverseContext.Builder#detectRecursions()
	 * @return
	 * 	<jk>true</jk> if recursions should be checked for during traversal.
	 */
	public final boolean isDetectRecursions() {
		return ctx.isDetectRecursions();
	}

	/**
	 * Ignore recursion errors.
	 *
	 * @see BeanTraverseContext.Builder#ignoreRecursions()
	 * @return
	 * 	<jk>true</jk> if when we encounter the same object when traversing a tree, we set the value to <jk>null</jk>.
	 * 	<br>Otherwise, a {@link BeanRecursionException} is thrown with the message <js>"Recursion occurred, stack=..."</js>.
	 */
	public final boolean isIgnoreRecursions() {
		return ctx.isIgnoreRecursions();
	}

	/**
	 * Initial depth.
	 *
	 * @see BeanTraverseContext.Builder#initialDepth(int)
	 * @return
	 * 	The initial indentation level at the root.
	 */
	public final int getInitialDepth() {
		return ctx.getInitialDepth();
	}

	/**
	 * Max traversal depth.
	 *
	 * @see BeanTraverseContext.Builder#maxDepth(int)
	 * @return
	 * 	The depth at which traversal is aborted if depth is reached in the POJO tree.
	 *	<br>If this depth is exceeded, an exception is thrown.
	 */
	public final int getMaxDepth() {
		return ctx.getMaxDepth();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* ContextSession */
	protected JsonMap properties() {
		return filteredMap("indent", indent, "depth", depth);
	}
}
