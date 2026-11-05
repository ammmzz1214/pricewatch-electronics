package com.pricewatch.electronics.data.local

class Converters {
    // All enum fields are stored as plain String names; no converter is
    // strictly required for String columns. This class is retained for
    // future non-primitive fields and to satisfy the Room compiler hook.
    // NOTE: no identity converters here — Room rejects two methods that
    // define the same Long? -> Long? conversion, and it handles Long
    // natively, so nothing needs converting.
}
