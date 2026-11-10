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
package org.apache.juneau.console.views;

/**
 * The categorical pill palette's slot function.
 *
 * <p>
 * A {@code pill} column marked {@link Render#categorical()} colours each distinct value from a fixed palette of
 * {@link #SIZE} colours.  The browser picks the slot with the hash documented on {@link #slot(String)}; this class is
 * the same function in Java, for parity tests and for server-side code that wants to know which slot a value gets
 * (for example to build a legend).  The renderer does not call it at runtime.
 *
 * <p>
 * {@link #SIZE} is part of the colour contract: changing it recolours every categorical value.
 *
 * @since 10.0.0
 */
public final class PillPalette {

	/** The number of palette slots (theme tokens {@code --jc-pill-cat-1-*} through {@code --jc-pill-cat-10-*}). */
	public static final int SIZE = 10;

	private static final int FNV_OFFSET = 0x811c9dc5;
	private static final int FNV_PRIME = 0x01000193;

	/**
	 * Returns the palette slot for a value.
	 *
	 * <p>
	 * FNV-1a, 32-bit, over the UTF-16 code units of the exact string (no case folding, no trimming), then
	 * {@code (hash mod SIZE) + 1} with the hash treated as unsigned.  Matching is case-sensitive: {@code "Pet"} and
	 * {@code "PET"} are different values with independent slots.  A <jk>null</jk> value hashes as the empty string.
	 *
	 * <p>
	 * Reports only the hash slot, never a slot pinned with {@link Render#category(String,int)}.
	 *
	 * @param value The cell value as displayed.  Can be <jk>null</jk>.
	 * @return The slot, from 1 to {@link #SIZE} inclusive.
	 */
	public static int slot(String value) {
		var h = FNV_OFFSET;
		if (value != null)
			for (var i = 0; i < value.length(); i++)
				h = (h ^ value.charAt(i)) * FNV_PRIME;
		return Integer.remainderUnsigned(h, SIZE) + 1;
	}

	private PillPalette() {}
}
