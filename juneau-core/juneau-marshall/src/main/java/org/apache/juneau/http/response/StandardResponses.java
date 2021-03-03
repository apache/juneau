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
package org.apache.juneau.http.response;

/**
 * Standard predefined HTTP responses.
 */
public class StandardResponses {

	/**
	 * A synonym for {@link Accepted#INSTANCE}.
	 */
	public static final Accepted ACCEPTED = Accepted.INSTANCE;

	/**
	 * A synonym for {@link AlreadyReported#INSTANCE}.
	 */
	public static final AlreadyReported ALREADY_REPORTED = AlreadyReported.INSTANCE;

	/**
	 * A synonym for {@link BadRequest#INSTANCE}.
	 */
	public static final BadRequest BAD_REQUEST = BadRequest.INSTANCE;

	/**
	 * A synonym for {@link Conflict#INSTANCE}.
	 */
	public static final Conflict CONFLICT = Conflict.INSTANCE;

	/**
	 * A synonym for {@link Continue#INSTANCE}.
	 */
	public static final Continue CONTINUE = Continue.INSTANCE;

	/**
	 * A synonym for {@link Created#INSTANCE}.
	 */
	public static final Created CREATED = Created.INSTANCE;

	/**
	 * A synonym for {@link EarlyHints#INSTANCE}.
	 */
	public static final EarlyHints EARLY_HINTS = EarlyHints.INSTANCE;

	/**
	 * A synonym for {@link ExpectationFailed#INSTANCE}.
	 */
	public static final ExpectationFailed EXPECTATION_FAILED = ExpectationFailed.INSTANCE;

	/**
	 * A synonym for {@link FailedDependency#INSTANCE}.
	 */
	public static final FailedDependency FAILED_DEPENDENCY = FailedDependency.INSTANCE;

	/**
	 * A synonym for {@link Forbidden#INSTANCE}.
	 */
	public static final Forbidden FORBIDDEN = Forbidden.INSTANCE;

	/**
	 * A synonym for {@link Found#INSTANCE}.
	 */
	public static final Found FOUND = Found.INSTANCE;

	/**
	 * A synonym for {@link Gone#INSTANCE}.
	 */
	public static final Gone GONE = Gone.INSTANCE;

	/**
	 * A synonym for {@link HttpVersionNotSupported#INSTANCE}.
	 */
	public static final HttpVersionNotSupported HTTP_VERSION_NOT_SUPPORTED = HttpVersionNotSupported.INSTANCE;

	/**
	 * A synonym for {@link IMUsed#INSTANCE}.
	 */
	public static final IMUsed IM_USED = IMUsed.INSTANCE;

	/**
	 * A synonym for {@link InsufficientStorage#INSTANCE}.
	 */
	public static final InsufficientStorage INSUFFICIENT_STORAGE = InsufficientStorage.INSTANCE;

	/**
	 * A synonym for {@link InternalServerError#INSTANCE}.
	 */
	public static final InternalServerError INTERNAL_SERVER_ERROR = InternalServerError.INSTANCE;

	/**
	 * A synonym for {@link LengthRequired#INSTANCE}.
	 */
	public static final LengthRequired LENGTH_REQUIRED = LengthRequired.INSTANCE;

	/**
	 * A synonym for {@link Locked#INSTANCE}.
	 */
	public static final Locked LOCKED = Locked.INSTANCE;

	/**
	 * A synonym for {@link LoopDetected#INSTANCE}.
	 */
	public static final LoopDetected LOOP_DETECTED = LoopDetected.INSTANCE;

	/**
	 * A synonym for {@link MethodNotAllowed#INSTANCE}.
	 */
	public static final MethodNotAllowed METHOD_NOT_ALLOWED = MethodNotAllowed.INSTANCE;

	/**
	 * A synonym for {@link MisdirectedRequest#INSTANCE}.
	 */
	public static final MisdirectedRequest MISDIRECTED_REQUEST = MisdirectedRequest.INSTANCE;

	/**
	 * A synonym for {@link MovedPermanently#INSTANCE}.
	 */
	public static final MovedPermanently MOVED_PERMANENTLY = MovedPermanently.INSTANCE;

	/**
	 * A synonym for {@link MultiStatus#INSTANCE}.
	 */
	public static final MultiStatus MULTI_STATUS = MultiStatus.INSTANCE;

	/**
	 * A synonym for {@link MultipleChoices#INSTANCE}.
	 */
	public static final MultipleChoices MULTIPLE_CHOICES = MultipleChoices.INSTANCE;

	/**
	 * A synonym for {@link NetworkAuthenticationRequired#INSTANCE}.
	 */
	public static final NetworkAuthenticationRequired NETWORK_AUTHENTICATION_REQUIRED = NetworkAuthenticationRequired.INSTANCE;

	/**
	 * A synonym for {@link NoContent#INSTANCE}.
	 */
	public static final NoContent NO_CONTENT = NoContent.INSTANCE;

	/**
	 * A synonym for {@link NonAuthoritiveInformation#INSTANCE}.
	 */
	public static final NonAuthoritiveInformation NON_AUTHORATIVE_INFORMATION = NonAuthoritiveInformation.INSTANCE;

	/**
	 * A synonym for {@link NotAcceptable#INSTANCE}.
	 */
	public static final NotAcceptable NOT_ACCEPTABLE = NotAcceptable.INSTANCE;

	/**
	 * A synonym for {@link NotExtended#INSTANCE}.
	 */
	public static final NotExtended NOT_EXTENDED = NotExtended.INSTANCE;

	/**
	 * A synonym for {@link NotFound#INSTANCE}.
	 */
	public static final NotFound NOT_FOUND = NotFound.INSTANCE;

	/**
	 * A synonym for {@link NotImplemented#INSTANCE}.
	 */
	public static final NotImplemented NOT_IMPLEMENTED = NotImplemented.INSTANCE;

	/**
	 * A synonym for {@link NotModified#INSTANCE}.
	 */
	public static final NotModified NOT_MODIFIED = NotModified.INSTANCE;

	/**
	 * A synonym for {@link Ok#INSTANCE}.
	 */
	public static final Ok OK = Ok.INSTANCE;

	/**
	 * A synonym for {@link PartialContent#INSTANCE}.
	 */
	public static final PartialContent PARTIAL_CONTENT = PartialContent.INSTANCE;

	/**
	 * A synonym for {@link PayloadTooLarge#INSTANCE}.
	 */
	public static final PayloadTooLarge PAYLOAD_TOO_LARGE = PayloadTooLarge.INSTANCE;

	/**
	 * A synonym for {@link PermanentRedirect#INSTANCE}.
	 */
	public static final PermanentRedirect PERMANENT_REDIRECT = PermanentRedirect.INSTANCE;

	/**
	 * A synonym for {@link PreconditionFailed#INSTANCE}.
	 */
	public static final PreconditionFailed PRECONDITION_FAILED = PreconditionFailed.INSTANCE;

	/**
	 * A synonym for {@link PreconditionRequired#INSTANCE}.
	 */
	public static final PreconditionRequired PRECONDITION_REQUIRED = PreconditionRequired.INSTANCE;

	/**
	 * A synonym for {@link Processing#INSTANCE}.
	 */
	public static final Processing PROCESSING = Processing.INSTANCE;

	/**
	 * A synonym for {@link RangeNotSatisfiable#INSTANCE}.
	 */
	public static final RangeNotSatisfiable RANGE_NOT_SATISFIABLE = RangeNotSatisfiable.INSTANCE;

	/**
	 * A synonym for {@link RequestHeaderFieldsTooLarge#INSTANCE}.
	 */
	public static final RequestHeaderFieldsTooLarge REQUEST_HEADER_FIELDS_TOO_LARGE = RequestHeaderFieldsTooLarge.INSTANCE;

	/**
	 * A synonym for {@link ResetContent#INSTANCE}.
	 */
	public static final ResetContent RESET_CONTENT = ResetContent.INSTANCE;

	/**
	 * A synonym for {@link SeeOther#INSTANCE}.
	 */
	public static final SeeOther SEE_OTHER = SeeOther.INSTANCE;

	/**
	 * A synonym for {@link ServiceUnavailable#INSTANCE}.
	 */
	public static final ServiceUnavailable SERVICE_UNAVAILABLE = ServiceUnavailable.INSTANCE;

	/**
	 * A synonym for {@link SwitchingProtocols#INSTANCE}.
	 */
	public static final SwitchingProtocols SWITCHING_PROTOCOLS = SwitchingProtocols.INSTANCE;

	/**
	 * A synonym for {@link TemporaryRedirect#INSTANCE}.
	 */
	public static final TemporaryRedirect TEMPORARY_REDIRECT = TemporaryRedirect.INSTANCE;

	/**
	 * A synonym for {@link TooManyRequests#INSTANCE}.
	 */
	public static final TooManyRequests TOO_MANY_REQUESTS = TooManyRequests.INSTANCE;

	/**
	 * A synonym for {@link Unauthorized#INSTANCE}.
	 */
	public static final Unauthorized UNAUTHORIZED = Unauthorized.INSTANCE;

	/**
	 * A synonym for {@link UnavailableForLegalReasons#INSTANCE}.
	 */
	public static final UnavailableForLegalReasons UNAVAILABLE_FOR_LEGAL_REASONS = UnavailableForLegalReasons.INSTANCE;

	/**
	 * A synonym for {@link UnprocessableEntity#INSTANCE}.
	 */
	public static final UnprocessableEntity UNPROCESSABLE_ENTITIY = UnprocessableEntity.INSTANCE;

	/**
	 * A synonym for {@link UnsupportedMediaType#INSTANCE}.
	 */
	public static final UnsupportedMediaType UNSUPPORTED_MEDIA_TYPE = UnsupportedMediaType.INSTANCE;

	/**
	 * A synonym for {@link UpgradeRequired#INSTANCE}.
	 */
	public static final UpgradeRequired UPGRADE_REQUIRED = UpgradeRequired.INSTANCE;

	/**
	 * A synonym for {@link UriTooLong#INSTANCE}.
	 */
	public static final UriTooLong URI_TOO_LONG = UriTooLong.INSTANCE;

	/**
	 * A synonym for {@link UseProxy#INSTANCE}.
	 */
	public static final UseProxy USE_PROXY = UseProxy.INSTANCE;

	/**
	 * A synonym for {@link VariantAlsoNegotiates#INSTANCE}.
	 */
	public static final VariantAlsoNegotiates VARIANT_ALSO_NEGOTIATES = VariantAlsoNegotiates.INSTANCE;


	/**
	 * A shortcut for calling {@link Continue#create()}.
	 *
	 * @return A new bean.
	 */
	public static HttpResponseBuilder<Continue> _continue() {
		return Continue.create();
	}

	/**
	 * A shortcut for calling {@link Accepted#create()}.
	 *
	 * @return A new bean.
	 */
	public static HttpResponseBuilder<Accepted> accepted() {
		return Accepted.create();
	}

	/**
	 * A shortcut for calling {@link AlreadyReported#create()}.
	 *
	 * @return A new bean.
	 */
	public static HttpResponseBuilder<AlreadyReported> alreadyReported() {
		return AlreadyReported.create();
	}

	/**
	 * A shortcut for calling {@link BadRequest#create()}.
	 *
	 * @return A new bean builder.
	 */
	public static HttpExceptionBuilder<BadRequest> badRequest() {
		return BadRequest.create();
	}

	/**
	 * A shortcut for calling {@link Conflict#create()}.
	 *
	 * @return A new bean builder.
	 */
	public static HttpExceptionBuilder<Conflict> conflict() {
		return Conflict.create();
	}

	/**
	 * A shortcut for calling {@link Created#create()}.
	 *
	 * @return A new bean.
	 */
	public static HttpResponseBuilder<Created> created() {
		return Created.create();
	}

	/**
	 * A shortcut for calling {@link EarlyHints#create()}.
	 *
	 * @return A new bean.
	 */
	public static HttpResponseBuilder<EarlyHints> earlyHints() {
		return EarlyHints.create();
	}

	/**
	 * A shortcut for calling {@link ExpectationFailed#create()}.
	 *
	 * @return A new bean builder.
	 */
	public static HttpExceptionBuilder<ExpectationFailed> expectationFailed() {
		return ExpectationFailed.create();
	}

	/**
	 * A shortcut for calling {@link FailedDependency#create()}.
	 *
	 * @return A new bean builder.
	 */
	public static HttpExceptionBuilder<FailedDependency> failedDependency() {
		return FailedDependency.create();
	}

	/**
	 * A shortcut for calling {@link Forbidden#create()}.
	 *
	 * @return A new bean builder.
	 */
	public static HttpExceptionBuilder<Forbidden> forbidden() {
		return Forbidden.create();
	}

	/**
	 * A shortcut for calling {@link Found#create()}.
	 *
	 * @param location The value for the Location header.
	 * @return A new bean.
	 */
	public static HttpResponseBuilder<Found> found(String location) {
		return Found.create().location(location);
	}

	/**
	 * A shortcut for calling {@link Gone#create()}.
	 *
	 * @return A new bean builder.
	 */
	public static HttpExceptionBuilder<Gone> gone() {
		return Gone.create();
	}

	/**
	 * A shortcut for calling {@link HttpException#create()}.
	 *
	 * @return A new bean builder.
	 */
	public static HttpExceptionBuilder<HttpException> httpException() {
		return HttpException.create(HttpException.class);
	}

	/**
	 * A shortcut for calling {@link HttpVersionNotSupported#create()}.
	 *
	 * @return A new bean builder.
	 */
	public static HttpExceptionBuilder<HttpVersionNotSupported> httpVersionNotSupported() {
		return HttpVersionNotSupported.create();
	}

	/**
	 * A shortcut for calling {@link IMUsed#create()}.
	 *
	 * @return A new bean.
	 */
	public static HttpResponseBuilder<IMUsed> imUsed() {
		return IMUsed.create();
	}

	/**
	 * A shortcut for calling {@link InsufficientStorage#create()}.
	 *
	 * @return A new bean builder.
	 */
	public static HttpExceptionBuilder<InsufficientStorage> insufficientStorage() {
		return InsufficientStorage.create();
	}

	/**
	 * A shortcut for calling {@link InternalServerError#create()}.
	 *
	 * @return A new bean builder.
	 */
	public static HttpExceptionBuilder<InternalServerError> internalServerError() {
		return InternalServerError.create();
	}

	/**
	 * A shortcut for calling {@link LengthRequired#create()}.
	 *
	 * @return A new bean builder.
	 */
	public static HttpExceptionBuilder<LengthRequired> lengthRequired() {
		return LengthRequired.create();
	}

	/**
	 * A shortcut for calling {@link Locked#create()}.
	 *
	 * @return A new bean builder.
	 */
	public static HttpExceptionBuilder<Locked> locked() {
		return Locked.create();
	}

	/**
	 * A shortcut for calling {@link LoopDetected#create()}.
	 *
	 * @return A new bean builder.
	 */
	public static HttpExceptionBuilder<LoopDetected> loopDetected() {
		return LoopDetected.create();
	}

	/**
	 * A shortcut for calling {@link MethodNotAllowed#create()}.
	 *
	 * @return A new bean builder.
	 */
	public static HttpExceptionBuilder<MethodNotAllowed> methodNotAllowed() {
		return MethodNotAllowed.create();
	}

	/**
	 * A shortcut for calling {@link MisdirectedRequest#create()}.
	 *
	 * @return A new bean builder.
	 */
	public static HttpExceptionBuilder<MisdirectedRequest> misdirectedRequest() {
		return MisdirectedRequest.create();
	}
	/**
	 * A shortcut for calling {@link MovedPermanently#create()}.
	 *
	 * @param location The value for the Location header.
	 * @return A new bean.
	 */
	public static HttpResponseBuilder<MovedPermanently> movedPermanently(String location) {
		return MovedPermanently.create().location(location);
	}

	/**
	 * A shortcut for calling {@link MovedPermanently#create()}.
	 *
	 * @return A new bean.
	 */
	public static HttpResponseBuilder<MultipleChoices> multipleChoices() {
		return MultipleChoices.create();
	}

	/**
	 * A shortcut for calling {@link MultiStatus#create()}.
	 *
	 * @return A new bean.
	 */
	public static HttpResponseBuilder<MultiStatus> multiStatus() {
		return MultiStatus.create();
	}

	/**
	 * A shortcut for calling {@link NetworkAuthenticationRequired#create()}.
	 *
	 * @return A new bean builder.
	 */
	public static HttpExceptionBuilder<NetworkAuthenticationRequired> networkAuthenticationRequired() {
		return NetworkAuthenticationRequired.create();
	}

	/**
	 * A shortcut for calling {@link NoContent#create()}.
	 *
	 * @return A new bean.
	 */
	public static HttpResponseBuilder<NoContent> noContent() {
		return NoContent.create();
	}

	/**
	 * A shortcut for calling {@link NonAuthoritiveInformation#create()}.
	 *
	 * @return A new bean.
	 */
	public static HttpResponseBuilder<NonAuthoritiveInformation> nonAuthoritiveInformation() {
		return NonAuthoritiveInformation.create();
	}

	/**
	 * A shortcut for calling {@link NotAcceptable#create()}.
	 *
	 * @return A new bean builder.
	 */
	public static HttpExceptionBuilder<NotAcceptable> notAcceptable() {
		return NotAcceptable.create();
	}

	/**
	 * A shortcut for calling {@link NotExtended#create()}.
	 *
	 * @return A new bean builder.
	 */
	public static HttpExceptionBuilder<NotExtended> notExtended() {
		return NotExtended.create();
	}

	/**
	 * A shortcut for calling {@link NotFound#create()}.
	 *
	 * @return A new bean builder.
	 */
	public static HttpExceptionBuilder<NotFound> notFound() {
		return NotFound.create();
	}

	/**
	 * A shortcut for calling {@link NotImplemented#create()}.
	 *
	 * @return A new bean builder.
	 */
	public static HttpExceptionBuilder<NotImplemented> notImplemented() {
		return NotImplemented.create();
	}

	/**
	 * A shortcut for calling {@link NotModified#create()}.
	 *
	 * @return A new bean.
	 */
	public static HttpResponseBuilder<NotModified> notModified() {
		return NotModified.create();
	}

	/**
	 * A shortcut for calling {@link Ok#create()}.
	 *
	 * @return A new bean.
	 */
	public static HttpResponseBuilder<Ok> ok() {
		return Ok.create();
	}

	/**
	 * A shortcut for calling {@link PartialContent#create()}.
	 *
	 * @return A new bean.
	 */
	public static HttpResponseBuilder<PartialContent> partialContent() {
		return PartialContent.create();
	}

	/**
	 * A shortcut for calling {@link PayloadTooLarge#create()}.
	 *
	 * @return A new bean builder.
	 */
	public static HttpExceptionBuilder<PayloadTooLarge> payloadTooLarge() {
		return PayloadTooLarge.create();
	}

	/**
	 * A shortcut for calling {@link PermanentRedirect#create()}.
	 *
	 * @param location The value for the Location header.
	 * @return A new bean.
	 */
	public static HttpResponseBuilder<PermanentRedirect> permanentRedirect(String location) {
		return PermanentRedirect.create().location(location);
	}

	/**
	 * A shortcut for calling {@link PreconditionFailed#create()}.
	 *
	 * @return A new bean builder.
	 */
	public static HttpExceptionBuilder<PreconditionFailed> preconditionFailed() {
		return PreconditionFailed.create();
	}

	/**
	 * A shortcut for calling {@link PreconditionRequired#create()}.
	 *
	 * @return A new bean builder.
	 */
	public static HttpExceptionBuilder<PreconditionRequired> preconditionRequired() {
		return PreconditionRequired.create();
	}

	/**
	 * A shortcut for calling {@link Processing#create()}.
	 *
	 * @return A new bean.
	 */
	public static HttpResponseBuilder<Processing> processing() {
		return Processing.create();
	}

	/**
	 * A shortcut for calling {@link RangeNotSatisfiable#create()}.
	 *
	 * @return A new bean builder.
	 */
	public static HttpExceptionBuilder<RangeNotSatisfiable> rangeNotSatisfiable() {
		return RangeNotSatisfiable.create();
	}

	/**
	 * A shortcut for calling {@link RequestHeaderFieldsTooLarge#create()}.
	 *
	 * @return A new bean builder.
	 */
	public static HttpExceptionBuilder<RequestHeaderFieldsTooLarge> requestHeaderFieldsTooLarge() {
		return RequestHeaderFieldsTooLarge.create();
	}

	/**
	 * A shortcut for calling {@link ResetContent#create()}.
	 *
	 * @return A new bean.
	 */
	public static HttpResponseBuilder<ResetContent> resetContent() {
		return ResetContent.create();
	}

	/**
	 * A shortcut for calling {@link SeeOther#create()}.
	 *
	 * @param location The value for the Location header.
	 * @return A new bean.
	 */
	public static HttpResponseBuilder<SeeOther> seeOther(String location) {
		return SeeOther.create().location(location);
	}

	/**
	 * A shortcut for calling {@link ServiceUnavailable#create()}.
	 *
	 * @return A new bean builder.
	 */
	public static HttpExceptionBuilder<ServiceUnavailable> serviceUnavailable() {
		return ServiceUnavailable.create();
	}

	/**
	 * A shortcut for calling {@link SwitchingProtocols#create()}.
	 *
	 * @return A new bean.
	 */
	public static HttpResponseBuilder<SwitchingProtocols> switchingProtocols() {
		return SwitchingProtocols.create();
	}

	/**
	 * A shortcut for calling {@link TemporaryRedirect#create()}.
	 *
	 * @param location The value for the Location header.
	 * @return A new bean.
	 */
	public static HttpResponseBuilder<TemporaryRedirect> temporaryRedirect(String location) {
		return TemporaryRedirect.create().location(location);
	}

	/**
	 * A shortcut for calling {@link TooManyRequests#create()}.
	 *
	 * @return A new bean builder.
	 */
	public static HttpExceptionBuilder<TooManyRequests> tooManyRequests() {
		return TooManyRequests.create();
	}

	/**
	 * A shortcut for calling {@link Unauthorized#create()}.
	 *
	 * @return A new bean builder.
	 */
	public static HttpExceptionBuilder<Unauthorized> unauthorized() {
		return Unauthorized.create();
	}

	/**
	 * A shortcut for calling {@link UnavailableForLegalReasons#create()}.
	 *
	 * @return A new bean builder.
	 */
	public static HttpExceptionBuilder<UnavailableForLegalReasons> unavailableForLegalReasons() {
		return UnavailableForLegalReasons.create();
	}

	/**
	 * A shortcut for calling {@link UnprocessableEntity#create()}.
	 *
	 * @return A new bean builder.
	 */
	public static HttpExceptionBuilder<UnprocessableEntity> unprocessableEntity() {
		return UnprocessableEntity.create();
	}

	/**
	 * A shortcut for calling {@link UnsupportedMediaType#create()}.
	 *
	 * @return A new bean builder.
	 */
	public static HttpExceptionBuilder<UnsupportedMediaType> unsupportedMediaType() {
		return UnsupportedMediaType.create();
	}

	/**
	 * A shortcut for calling {@link UpgradeRequired#create()}.
	 *
	 * @return A new bean builder.
	 */
	public static HttpExceptionBuilder<UpgradeRequired> upgradeRequired() {
		return UpgradeRequired.create();
	}

	/**
	 * A shortcut for calling {@link UriTooLong#create()}.
	 *
	 * @return A new bean builder.
	 */
	public static HttpExceptionBuilder<UriTooLong> uriTooLong() {
		return UriTooLong.create();
	}

	/**
	 * A shortcut for calling {@link UseProxy#create()}.
	 *
	 * @return A new bean.
	 */
	public static HttpResponseBuilder<UseProxy> useProxy() {
		return UseProxy.create();
	}

	/**
	 * A shortcut for calling {@link VariantAlsoNegotiates#create()}.
	 *
	 * @return A new bean builder.
	 */
	public static HttpExceptionBuilder<VariantAlsoNegotiates> variantAlsoNegotiates() {
		return VariantAlsoNegotiates.create();
	}
}
