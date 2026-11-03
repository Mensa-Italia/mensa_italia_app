import Shared

/// Global singleton KoinAccess. Call only after MensaSdk.shared.doInitKoinIos()
/// AND after the database is ready: the first access resolves every
/// repository at once, `AuthRepository` included, and that one needs
/// `MensaDatabase`. From anything that can run before the window exists
/// (BGTask, silent push, FCM token) await `AppBootstrap.shared.waitUntilReady()`
/// first, or the Kotlin error becomes an `abort()`.
let koin: KoinAccess = KoinHelperKt.koinAccess()
