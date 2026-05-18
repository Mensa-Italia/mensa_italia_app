import Shared

/// Bridge: subscribe to a Kotlin Flow (exposed as `Kotlinx_coroutines_coreFlow` in Swift).
/// The returned `Closeable` must be retained; call `.close()` to cancel.
///
/// Usage:
///   sub = subscribeFlow(koin.auth.authState) { (state: AuthState) in … }
///
/// Note: `FlowBridgeKt.subscribe(flow:onEach:onError:)` is the Kotlin/Native
/// bridged name. `onError` receives a `KotlinThrowable`.
@discardableResult
func subscribeFlow<T: AnyObject>(
    _ flow: Kotlinx_coroutines_coreFlow,
    onEach: @escaping (T) -> Void,
    onError: @escaping (Error) -> Void = { _ in }
) -> Closeable {
    FlowBridgeKt.subscribe(
        flow: flow,
        onEach: { value in
            if let typed = value as? T {
                onEach(typed)
            }
        },
        onError: { throwable in
            let err = NSError(
                domain: "KotlinThrowable",
                code: 0,
                userInfo: [NSLocalizedDescriptionKey: throwable.message ?? "unknown Kotlin error"]
            )
            onError(err)
        }
    )
}

/// Overload for nullable Kotlin flows (e.g. `StateFlow<UserModel?>`).
/// `onEach` receives `T?` — called with `nil` when Kotlin emits `null`.
///
/// Uses `FlowBridgeKt.subscribeNullable` (typed `(T?) -> Unit` in Kotlin) so
/// the Swift thunk never sees a non-Optional `Any` parameter with a null
/// payload — that combination would crash in `swift_getObjectType`.
@discardableResult
func subscribeOptionalFlow<T: AnyObject>(
    _ flow: Kotlinx_coroutines_coreFlow,
    onEach: @escaping (T?) -> Void,
    onError: @escaping (Error) -> Void = { _ in }
) -> Closeable {
    FlowBridgeKt.subscribeNullable(
        flow: flow,
        onEach: { value in
            onEach(value as? T)
        },
        onError: { throwable in
            let err = NSError(
                domain: "KotlinThrowable",
                code: 0,
                userInfo: [NSLocalizedDescriptionKey: throwable.message ?? "unknown Kotlin error"]
            )
            onError(err)
        }
    )
}
