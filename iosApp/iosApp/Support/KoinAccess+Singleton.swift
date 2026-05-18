import Shared

/// Global singleton KoinAccess. Call only after MensaSdk.shared.doInitKoinIos().
let koin: KoinAccess = KoinHelperKt.koinAccess()
