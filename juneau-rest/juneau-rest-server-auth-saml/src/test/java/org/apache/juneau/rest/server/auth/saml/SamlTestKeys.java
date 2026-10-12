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
package org.apache.juneau.rest.server.auth.saml;

import java.io.*;
import java.security.*;
import java.security.cert.*;
import java.security.spec.*;
import java.util.*;

import org.opensaml.security.credential.*;
import org.opensaml.security.x509.*;

/**
 * A fixed, self-signed RSA identity (certificate valid 2026-10-11 through 2126-09-17) shared by the metadata-driven
 * tests, so they need no certificate-generation dependency.
 *
 * <p>
 * The key pair exists only for these tests and protects nothing.
 */
final class SamlTestKeys {

	/** Base64 DER of the self-signed X.509 certificate. */
	static final String CERT_BASE64 =
		"MIIDFzCCAf+gAwIBAgIUXK0vYKCqDvjiO4DHfXeuH6X8UpMwDQYJKoZIhvcNAQELBQAwGjEYMBYGA1UEAwwPaWRwLmV4YW1w"
		+ "bGUuY29tMCAXDTI2MTAxMTIzNTkxNloYDzIxMjYwOTE3MjM1OTE2WjAaMRgwFgYDVQQDDA9pZHAuZXhhbXBsZS5jb20wggEi"
		+ "MA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDfdpOIKouMuD5uxYiKN6UDsubm6a6nRw/GZspHbDLrGQvnfxHb1h9b6zEG"
		+ "yhobzvlukhATv+9HL0glhL7/a5ginzQUwPbvtk+z8z6dzz2Hja6b89rSPSqxwvs9Ks4JmKUiFxQGp7cYA/gkSkN4sLlCwWn0"
		+ "TrMAGXQi8CAxJ7kMAzfeaCfuuh/xD1f/nl8h+QTu/0SQA7G6ledXqwOe/hwIOM2ZSB0LIIkQb4zDRoBcG6CcSsegUxnMLVwF"
		+ "GKjc87n7mOiUVTLMZR82s6ziY2lvcVTGLw0JWjnZQA94tao6g6FP5sL73CPmswgX8NKWuFIRrceusOJB+mhPoRXBZkynAgMB"
		+ "AAGjUzBRMB0GA1UdDgQWBBScpsKKZeQyevv33NvLogtmm3jbMDAfBgNVHSMEGDAWgBScpsKKZeQyevv33NvLogtmm3jbMDAP"
		+ "BgNVHRMBAf8EBTADAQH/MA0GCSqGSIb3DQEBCwUAA4IBAQAgIRGqPgh5YKCRvoOaNQZmCUTkWufBXgxlxzUcJHhMH1OM0Aqt"
		+ "0+Btqgtbc0550FE8UwuH62tNJQ0wh+kS9I4KoREoP5JPeoz3PJF+bStBg3oFlLP3sGcQKPd3HSP3ODOMvAIMgipCLUxWmQf7"
		+ "8HSfYxhSi3x7BjaZpJLnIz2gSEG7udtjOJU8E/woAdC7UOZFXZV4vSiOtv53xrzXuGH+TxyoNkY0ridnXjyVHxxU4k9XzqVF"
		+ "9TH4LfChNAftKosL54l1uvXoRAM4w2OyIgIvoHJNpy76c9Ns+//RIdTiZa4SD8xRHCvx/P6AYlnRQrB/oDeaXmJQz44Gis53"
		+ "18xH";

	/** Base64 DER of a second, unrelated self-signed certificate (no matching key is kept). */
	static final String OTHER_CERT_BASE64 =
		"MIIDGzCCAgOgAwIBAgIUEtuSohAQ3OvFb0vMIYdCsI5Ax6MwDQYJKoZIhvcNAQELBQAwHDEaMBgGA1UEAwwRb3RoZXIuZXhh"
		+ "bXBsZS5jb20wIBcNMjYxMDEyMDAzMjM5WhgPMjEyNjA5MTgwMDMyMzlaMBwxGjAYBgNVBAMMEW90aGVyLmV4YW1wbGUuY29t"
		+ "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAosYQ1zluzDDHWUZcgL392mi5NaQqeH0dMxw4Hxa/61u3PC5eJ9H7"
		+ "sW++qlcQvYgvBIZhZ4orP/Nm29XPJnuoiAPCgxWKgksTTIR7hRK3NmE22OuFCqJplAq6L4nU4MsE1nPYDBF94rKgs/IflvJl"
		+ "inntdC4Kk1Lcdp5iZWx5SCMplUjadr73hYRlOi9NzwwgPdtsocUTvtV3A5dNO800YWkaPqjjDL7Mf9qznqkguecrDdglSKP3"
		+ "iDfjpbWiXHAPWKsHNUFrXTB7N7CDHl/pGAkbp+C+7Fgea1Q8+EXwN5fFaUX32KxinM3k/x8wCW2LxjwVs0LkHscxYy/Q7JI7"
		+ "GwIDAQABo1MwUTAdBgNVHQ4EFgQU9su5e5pPME8usnsPGJeW/9uXoJIwHwYDVR0jBBgwFoAU9su5e5pPME8usnsPGJeW/9uX"
		+ "oJIwDwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG9w0BAQsFAAOCAQEAHefxTO8vEBrToIhpBBELpXpt4ZK3yled6QGezZiCsCkc"
		+ "MwlIKBjvXj8FDYaCLEn1PUqaQ9Re8LvGzkwNxT74Vp8NlVjI82D8ZCZaDaiqVkpYoKX5DOZF/9YvG4YTJtPcWagJyD/LMD71"
		+ "074uUryy1IsX236avHcMayCdnYHRbvE8oDaIM0eHnEv4/hGTuAc7ZtOklCsFgMTd1qW0W/iDDNEePRhAesbg+9tPtP0J36+k"
		+ "pYA+5gGOxAZmAKwwWaRVfWhwf+uxu5uVZWqVjREoCBDD9WMXkMnEfQumGeKwlYaACbVoT4dnTt0/rWitzSj8kfoNlIfVVSyi"
		+ "3HgY4NrUFw==";

	private static final String KEY_BASE64 =
		"MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDfdpOIKouMuD5uxYiKN6UDsubm6a6nRw/GZspHbDLrGQvn"
		+ "fxHb1h9b6zEGyhobzvlukhATv+9HL0glhL7/a5ginzQUwPbvtk+z8z6dzz2Hja6b89rSPSqxwvs9Ks4JmKUiFxQGp7cYA/gk"
		+ "SkN4sLlCwWn0TrMAGXQi8CAxJ7kMAzfeaCfuuh/xD1f/nl8h+QTu/0SQA7G6ledXqwOe/hwIOM2ZSB0LIIkQb4zDRoBcG6Cc"
		+ "SsegUxnMLVwFGKjc87n7mOiUVTLMZR82s6ziY2lvcVTGLw0JWjnZQA94tao6g6FP5sL73CPmswgX8NKWuFIRrceusOJB+mhP"
		+ "oRXBZkynAgMBAAECggEAR74JeodyOxDx1l1ryzg3LbtTn2H51TGe0msw2QJXTJxDcqScprAVbEzTdiMPg8sM0J9rMvQFjZyT"
		+ "9JusOFOiScdEEL+s5+PnrdXTzhV5YxotK1t8bs/9c4muYIJsGK+doJa5MrPIuBiaI4iGJerS56gx7KIRnOAPJaJjK3Td14ld"
		+ "T1DLqPCbtaY1ukVuC7/EJlBVjR05RMIO+e+v37Gamdt8g4ZFSlJtnpehmKUAenY4nZEqrxR0qi6EkUTue7dICySqzb2oboy6"
		+ "YDKAT0z8IVanyRkttgK+vuly1WGy7CK8asCE41zmaYSpf1bqH/w/zZ2FDE/MKy920JwA3+RwyQKBgQD3D6DcaRUV5vWrSEDY"
		+ "uCkQ8LZcqR+E9VZ0gQRoV/kzextvH7z0YhEgHg2G33J0MNn3kcbE8iya0CNCBtRHZv8qMk9PObkR3oGdO3M8/ZnhXU2lpBNh"
		+ "+mCzs/iicemekgQA8p77UnBMgtEiMvBEMKWs2eZXMzOeuVbSA3BDIEJsuQKBgQDnjGAxKloyt3suvs9DtisuvaEmFQHMAai+"
		+ "wS4c5N/OvtcZm/9COm3SEc0Ys/AREQ74u0Ms7V5T1SIaoOXBObtCCj0Pw7DUJZTM3T5HALaImJ+8yaFIVUOZiCbgSvTNI9iE"
		+ "mXJxHPritNvq8Z7UzFs5Y6+VfxlE+1kVGGE0gI2UXwKBgFjgbmXcB5HRfB8Z5voPhfZqIbBDxur1txxCLWu7+hHi/4OCT186"
		+ "yB5dsVALogjaag79ihvfYtxuc8CU5FirXK+5WX6OrV0YRlhhwpr2QB0qi3xDAhYFFDSRUahegkRhEr+kiDyw82USylkbKgPd"
		+ "3DvKR2qnKLK4ydrTEdeUC1FBAoGBAICfTMvi3+nuXGISImZVqlUzh3H1F2NwxID6JJhuVxiskCYsrrXmaRzZQJnpUoDYff2T"
		+ "YHRzgwOEkIZEPHyCik/cCiFYrkCag7bta2vbrIniZlFjm9DndD60EAxl9uiBcgPujVDiLUeD+enfwEArKL2Pb3jW+n2ENe7u"
		+ "9QhKJNt3AoGBAKo0du/l2ClJol4UoVxDCYSBaIU4x+xDdYJAma6QuTY601F2J5l/ctZjoxQBfFD3N91G0v8Nu6CT8uZX8mWu"
		+ "tX5zRmjT3e1cJT6stbzA2K+CAw8Pk368Qd5W5sPr6Ikz8UhHdSUB9MWQ35cHQjEHvlOYKaFK4Sd9TKpuAbgY/0Hy";

	/** The first instant at which the certificate is valid (with margin); later instants up to 2126 are valid. */
	static final java.time.Instant VALID_AT = java.time.Instant.parse("2027-06-01T00:00:00Z");

	private SamlTestKeys() {}

	/** Returns the parsed certificate. */
	static X509Certificate certificate() throws Exception {
		var bytes = Base64.getDecoder().decode(CERT_BASE64);
		return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(bytes));
	}

	/** Returns the second, unrelated certificate. */
	static X509Certificate otherCertificate() throws Exception {
		var bytes = Base64.getDecoder().decode(OTHER_CERT_BASE64);
		return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(bytes));
	}

	/** Returns a signing/decryption credential holding the certificate's key pair. */
	static BasicX509Credential credential() throws Exception {
		var cert = certificate();
		var key = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(KEY_BASE64)));
		var c = new BasicX509Credential(cert, key);
		c.setUsageType(UsageType.UNSPECIFIED);
		return c;
	}
}
