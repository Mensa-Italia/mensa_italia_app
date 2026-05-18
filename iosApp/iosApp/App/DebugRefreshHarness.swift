import Foundation
import Shared

/// DEBUG-only harness: after auth, sequentially refresh every repository so we
/// can find which one crashes. Each call is wrapped in do/catch and logged via
/// NSLog so the failure is visible in Console.app even if Kotlin crashes after.
///
/// Triggered when env `MENSA_REFRESH_ALL=1` is set.
enum DebugRefreshHarness {
    static func runIfRequested() {
        guard ProcessInfo.processInfo.environment["MENSA_REFRESH_ALL"] == "1" else { return }
        let env = ProcessInfo.processInfo.environment
        Task.detached(priority: .background) {
            try? await koin.auth.doInit()
            if !(koin.auth.authState.value is AuthStateAuthenticated),
               let email = env["MENSA_AUTOLOGIN_EMAIL"],
               let pwd = env["MENSA_AUTOLOGIN_PWD"] {
                NSLog("MENSA_DEBUG: auto-login for \(email)")
                _ = try? await koin.auth.login(email: email, password: pwd)
            }
            for _ in 0..<60 {
                if koin.auth.authState.value is AuthStateAuthenticated { break }
                try? await Task.sleep(nanoseconds: 500_000_000)
            }
            guard koin.auth.authState.value is AuthStateAuthenticated else {
                NSLog("MENSA_DEBUG: not authenticated, harness skipped")
                return
            }
            NSLog("MENSA_DEBUG: starting refresh-all harness")
            await step("events") { _ = try await koin.events.firstSnapshot(); try await koin.events.refresh(filter: nil, sort: "when_end") }
            await step("deals") { try await koin.deals.refresh(filter: nil, sort: "created") }
            await step("sigs") { try await koin.sigs.refresh(filter: nil, sort: "name") }
            await step("stamps") { try await koin.stamps.refresh(filter: nil, sort: "-created") }
            await step("notifications") { try await koin.notifications.refresh() }
            await step("addons") { try await koin.addons.refresh() }
            await step("regSoci") { try await koin.regSoci.refresh() }
            await step("boutique") { try await koin.boutique.refresh() }
            await step("documents") { try await koin.documents.refresh() }
            await step("tickets") { try await koin.tickets.refresh() }
            await step("receipts") { try await koin.receipts.refresh() }
            await step("devices") { try await koin.devices.refresh() }
            await step("calendarLinks") { try await koin.calendarLinks.refresh() }
            NSLog("MENSA_DEBUG: refresh-all harness DONE")
        }
    }

    private static func step(_ name: String, _ block: () async throws -> Void) async {
        NSLog("MENSA_DEBUG: refreshing \(name)…")
        do {
            try await block()
            NSLog("MENSA_DEBUG: ✅ \(name) OK")
        } catch {
            NSLog("MENSA_DEBUG: ❌ \(name) FAILED: \((error as NSError).localizedDescription)")
        }
    }
}
