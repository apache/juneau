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
package org.apache.juneau.petstore.marshall;

import java.awt.image.*;
import java.io.*;

import javax.imageio.*;
import javax.imageio.stream.*;

import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.parser.*;

/**
 * Parses {@code image/png}/{@code image/jpeg}/{@code image/jpg} byte streams into {@link BufferedImage} pet photos.
 *
 * <p>
 * Petstore-local port of the deleted {@code juneau-examples-rest} image demo, relocated here (rather than
 * {@code juneau-examples-core}) so {@code juneau-petstore-core} stays free of any dependency on the examples
 * modules.  Wired into {@link org.apache.juneau.petstore.rest.PetStoreResource#putPetPhoto(long, BufferedImage)}.
 *
 * <h2 class='section'>See Also:</h2><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauPetstore">juneau-petstore</a>
 * </ul>
 */
public class PetPhotoParser extends InputStreamParser {

	/** The largest accepted photo, in pixels (width times height). */
	public static final long MAX_PIXELS = 16L * 1024 * 1024;

	/** The largest accepted photo width or height, in pixels. */
	public static final int MAX_DIMENSION = 8192;

	/**
	 * Constructor with default settings (consumes image/png, image/jpeg and image/jpg).
	 */
	public PetPhotoParser() {
		super(create().consumes("image/png,image/jpeg,image/jpg"));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override /* Parser */
	@SuppressWarnings({
		"unchecked" // Type erasure requires cast for image return
	})
	public <T> T doRead(ParserSession session, ParserPipe pipe, ClassMeta<T> type) throws IOException, ParseException {
		try (var is = pipe.getInputStream()) {
			BufferedImage image;
			try (var iis = ImageIO.createImageInputStream(is)) {
				image = iis == null ? null : read(session, iis);
			} catch (IIOException e) {
				// Corrupt data that looks like a supported image.  Unchecked decoder failures are already wrapped
				// as ParseException by ParserSession.readInner, so only IIOException needs handling here.
				throw new ParseException(session, e, "Unsupported or unreadable image data");
			}
			if (image == null)
				throw new ParseException(session, "Unsupported or unreadable image data");
			return (T)image;
		}
	}

	// Reads the declared dimensions from the image header and refuses an oversized image before it is decoded.
	private static BufferedImage read(ParserSession session, ImageInputStream iis) throws IOException, ParseException {
		var readers = ImageIO.getImageReaders(iis);
		if (! readers.hasNext())
			return null;
		var reader = readers.next();
		try {
			reader.setInput(iis, true, true);
			long w = reader.getWidth(0);
			long h = reader.getHeight(0);
			if (w > MAX_DIMENSION || h > MAX_DIMENSION || w * h > MAX_PIXELS)
				throw new ParseException(session, "Image dimensions exceed the {0} pixel limit", MAX_PIXELS);
			return reader.read(0);
		} finally {
			reader.dispose();
		}
	}
}
