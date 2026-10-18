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

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.*;
import java.io.*;
import java.nio.charset.*;

import javax.imageio.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.parser.*;
import org.junit.jupiter.api.*;

class PetPhotoParser_Test extends TestBase {

	private static final int W = 7, H = 5;

	private static byte[] encode(String format) throws IOException {
		var img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
		for (var x = 0; x < W; x++)
			for (var y = 0; y < H; y++)
				img.setRGB(x, y, (x * 30) << 16 | (y * 40) << 8 | 0x80);
		var baos = new ByteArrayOutputStream();
		assertTrue(ImageIO.write(img, format, baos));
		return baos.toByteArray();
	}

	private final PetPhotoParser parser = new PetPhotoParser();

	@Test void a01_parsesJpeg() throws Exception {
		assertTrue(parser.canHandle("image/jpeg"));
		assertBean(parser.read(encode("jpeg"), BufferedImage.class), "width,height", "7,5");
	}

	@Test void a02_parsesPng() throws Exception {
		assertTrue(parser.canHandle("image/png"));
		assertBean(parser.read(encode("png"), BufferedImage.class), "width,height", "7,5");
	}

	@Test void a03_mediaTypesIncludeJpegAndLegacyJpg() {
		assertList(parser.getMediaTypes(), "image/png", "image/jpeg", "image/jpg");
		// Legacy non-standard alias still accepted.
		assertTrue(parser.canHandle("image/jpg"));
	}

	@Test void a04_nonImageInputThrowsParseException() {
		var input = "not an image".getBytes(StandardCharsets.UTF_8);
		assertThrows(ParseException.class, () -> parser.read(input, BufferedImage.class));
	}

	@Test void a05_corruptJpegThrowsParseException() throws Exception {
		var good = encode("jpeg");
		// Valid JPEG header followed by garbage: ImageIO throws instead of returning null.
		var bad = new byte[good.length];
		System.arraycopy(good, 0, bad, 0, 40);
		for (var i = 40; i < bad.length; i++)
			bad[i] = (byte)0x5A;
		assertThrows(ParseException.class, () -> parser.read(bad, BufferedImage.class));
	}

	@Test void a06_corruptPngThrowsParseException() throws Exception {
		var good = encode("png");
		var bad = java.util.Arrays.copyOf(good, 40);
		assertThrows(ParseException.class, () -> parser.read(bad, BufferedImage.class));
	}
}
