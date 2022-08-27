@file:Suppress("unused")

package dev.entao.app.http

private val codeMap = hashMapOf(
    100 to "Continue",
    101 to "Switching Protocols",
    102 to "Processing",
    200 to "OK",
    201 to "Created",
    202 to "Accepted",
    203 to "Non-Authoritative Information",
    204 to "No Content",
    205 to "Reset Content",
    206 to "Partial Content",
    207 to "Multi-Status",
    300 to "Multiple Choices",
    301 to "Moved Permanently",
    302 to "Move temporarily",
    303 to "See Other",
    304 to "Not Modified",
    305 to "Use Proxy",
    306 to "Switch Proxy",
    307 to "Temporary Redirect",
    400 to "Bad Request",
    401 to "Unauthorized",
    402 to "Payment Required",
    403 to "Forbidden",
    404 to "Not Found",
    405 to "Method Not Allowed",
    406 to "Not Acceptable",
    407 to "Proxy Authentication Required",
    408 to "Request Timeout",
    409 to "Conflict",
    410 to "Gone",
    411 to "Length Required",
    412 to "Precondition Failed",
    413 to "Request Entity Too Large",
    414 to "Request-URI Too Long",
    415 to "Unsupported Media Type",
    416 to "Requested Range Not Satisfiable",
    417 to "Expectation Failed",
    421 to "too many connections",
    422 to "Unprocessable Entity",
    423 to "Locked",
    424 to "Failed Dependency",
    425 to "Unordered Collection",
    426 to "Upgrade Required",
    449 to "Retry With",
    451 to "Unavailable For Legal Reasons",
    500 to "Internal Server Error",
    501 to "Not Implemented",
    502 to "Bad Gateway",
    503 to "Service Unavailable",
    504 to "Gateway Timeout",
    505 to "HTTP Version Not Supported",
    506 to "Variant Also Negotiates",
    507 to "Insufficient Storage",
    509 to "Bandwidth Limit Exceeded",
    510 to "Not Extended",
    600 to "Unparseable Response Headers"
)

internal fun httpMsgByCode(code: Int): String? {
    return codeMap[code]
}

internal object HttpCode {
    const val Continue = 100
    const val SwitchingProtocols = 101
    const val Processing = 102
    const val OK = 200
    const val Created = 201
    const val Accepted = 202
    const val NonAuthoritativeInformation = 203
    const val NoContent = 204
    const val ResetContent = 205
    const val PartialContent = 206
    const val MultiStatus = 207
    const val MultipleChoices = 300
    const val MovedPermanently = 301
    const val MoveTemporarily = 302
    const val SeeOther = 303
    const val NotModified = 304
    const val UseProxy = 305
    const val SwitchProxy = 306
    const val TemporaryRedirect = 307
    const val BadRequest = 400
    const val Unauthorized = 401
    const val PaymentRequired = 402
    const val Forbidden = 403
    const val NotFound = 404
    const val MethodNotAllowed = 405
    const val NotAcceptable = 406
    const val ProxyAuthenticationRequired = 407
    const val RequestTimeout = 408
    const val Conflict = 409
    const val Gone = 410
    const val LengthRequired = 411
    const val PreconditionFailed = 412
    const val RequestEntityTooLarge = 413
    const val RequestURITooLong = 414
    const val UnsupportedMediaType = 415
    const val RequestedRangeNotSatisfiable = 416
    const val ExpectationFailed = 417
    const val TooManyConnections = 421
    const val UnprocessableEntity = 422
    const val Locked = 423
    const val FailedDependency = 424
    const val UnorderedCollection = 425
    const val UpgradeRequired = 426
    const val RetryWith = 449
    const val UnavailableForLegalReasons = 451
    const val InternalServerError = 500
    const val NotImplemented = 501
    const val BadGateway = 502
    const val ServiceUnavailable = 503
    const val GatewayTimeout = 504
    const val HTTPVersionNotSupported = 505
    const val VariantAlsoNegotiates = 506
    const val InsufficientStorage = 507
    const val BandwidthLimitExceeded = 509
    const val NotExtended = 510
    const val UnparseableResponseHeader = 600
}