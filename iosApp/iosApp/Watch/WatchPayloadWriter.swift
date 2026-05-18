import Foundation
import CoreImage
import CoreImage.CIFilterBuiltins
import UIKit
import WatchConnectivity
import WidgetKit
import Shared

/// Osserva `koin.auth.currentUser` + `koin.events.observeAll()` e propaga un
/// `WatchPayload` JSON alla Watch app via due canali:
///
///   1. **`WCSession.updateApplicationContext`** — il pattern Apple per
///      mirroring "latest value wins" iPhone → Watch. Funziona anche se la
///      Watch app non e' in primo piano, e viene consegnato non appena il
///      Watch puo' riceverlo. Questo e' il canale fra device.
///   2. **App Group UserDefaults locale** — utile come fallback / debug
///      sull'iPhone stesso (l'iOS app puo' leggere lo stesso payload se
///      domani vogliamo mostrare un'anteprima del Watch nell'iOS app).
///      NON e' un canale verso il Watch: gli App Groups sono device-locali.
///
/// Errore architetturale evitato qui: gli App Groups condividono dati solo
/// tra processi dello stesso device, NON tra iPhone e Watch paired. Il
/// canale corretto cross-device e' WatchConnectivity. Vedi
/// `WatchSessionMirror.swift` lato Watch per il delegate ricevente.
///
/// Il QR PNG viene generato qui (CoreImage su iOS e' disponibile, su watchOS
/// non risolve come modulo in questa toolchain).
@MainActor
final class WatchPayloadWriter: NSObject {
    static let shared = WatchPayloadWriter()
    private override init() { super.init() }

    private var userSub: Closeable?
    private var eventsSub: Closeable?

    private var lastUser: UserModel?
    private var lastEvents: [EventModel] = []
    private var sessionActivated = false

    func start() {
        guard userSub == nil else { return } // already running
        activateSessionIfNeeded()

        let userFlow = koin.auth.currentUser as Kotlinx_coroutines_coreFlow
        userSub = subscribeOptionalFlow(userFlow) { [weak self] (u: UserModel?) in
            Task { @MainActor [weak self] in
                self?.lastUser = u
                self?.rebuildAndWrite()
            }
        } onError: { _ in }

        let eventsFlow = koin.events.observeAll() as Kotlinx_coroutines_coreFlow
        eventsSub = subscribeFlow(eventsFlow) { [weak self] (list: NSArray) in
            Task { @MainActor [weak self] in
                self?.lastEvents = (list as? [EventModel]) ?? []
                self?.rebuildAndWrite()
            }
        } onError: { _ in }
    }

    func stop() {
        userSub?.close(); userSub = nil
        eventsSub?.close(); eventsSub = nil
    }

    private func rebuildAndWrite() {
        let payload = WatchPayload(
            card: buildCard(),
            nextEvent: buildNextEvent(),
            generatedAt: Date()
        )
        // Local mirror (debug / future iOS preview of Watch state).
        WatchAppGroup.write(payload)
        WidgetCenter.shared.reloadAllTimelines()
        // Cross-device push iPhone -> Watch.
        sendToWatch(payload)
    }

    // MARK: - WatchConnectivity

    private func activateSessionIfNeeded() {
        guard !sessionActivated, WCSession.isSupported() else { return }
        let session = WCSession.default
        session.delegate = self
        session.activate()
        sessionActivated = true
    }

    private func sendToWatch(_ payload: WatchPayload) {
        guard WCSession.isSupported() else { return }
        let session = WCSession.default
        guard session.activationState == .activated else { return }
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        guard let data = try? encoder.encode(payload) else { return }
        do {
            try session.updateApplicationContext(["payload": data])
        } catch {
            // Non-fatal — il Watch riprenderá il prossimo updateApplicationContext.
            // Cause comuni: Watch non paired (isWatchAppInstalled == false).
        }
    }

    // MARK: Card

    private func buildCard() -> WatchPayload.CardSnapshot? {
        guard let user = lastUser else { return nil }
        let fullName = user.name.isEmpty ? user.username : user.name
        let expiryFormatted = formatItalianDate(user.expireMembership)
        let qrPayload = "MENSA-IT|id:\(user.id)|user:\(user.username)|exp:\(expiryFormatted)"
        let isActive: Bool = {
            // Stessa euristica di CardView: scadenza nel futuro = attiva.
            let nowSec = Int64(Date().timeIntervalSince1970)
            return user.expireMembership.epochSeconds > nowSec
        }()
        return WatchPayload.CardSnapshot(
            memberId: user.id,
            fullName: fullName,
            expiryFormatted: expiryFormatted,
            isActive: isActive,
            qrPng: generateQrPng(qrPayload)
        )
    }

    // MARK: Next event

    private func buildNextEvent() -> WatchPayload.EventSnapshot? {
        let nowSec = Int64(Date().timeIntervalSince1970)
        let upcoming = lastEvents
            .filter { $0.whenStart.epochSeconds >= nowSec }
            .sorted { $0.whenStart.epochSeconds < $1.whenStart.epochSeconds }
        // Stessa logica di TodayViewModel: prima upcoming cronologico, con
        // fallback al primo evento se non ci sono upcoming.
        guard let ev = upcoming.first ?? lastEvents.first else { return nil }
        return WatchPayload.EventSnapshot(
            id: ev.id,
            name: ev.name,
            startDate: Date(timeIntervalSince1970: TimeInterval(ev.whenStart.epochSeconds)),
            endDate: Date(timeIntervalSince1970: TimeInterval(ev.whenEnd.epochSeconds)),
            locationName: ev.position?.name,
            isNational: ev.isNational
        )
    }

    // MARK: QR generation

    private let qrContext = CIContext()
    private let qrFilter = CIFilter.qrCodeGenerator()

    private func generateQrPng(_ payload: String) -> Data? {
        qrFilter.message = Data(payload.utf8)
        qrFilter.correctionLevel = "M"
        guard
            let output = qrFilter.outputImage?.transformed(by: CGAffineTransform(scaleX: 8, y: 8)),
            let cg = qrContext.createCGImage(output, from: output.extent)
        else {
            return nil
        }
        return UIImage(cgImage: cg).pngData()
    }
}

// MARK: - WCSessionDelegate

extension WatchPayloadWriter: WCSessionDelegate {
    nonisolated func session(
        _ session: WCSession,
        activationDidCompleteWith activationState: WCSessionActivationState,
        error: Error?
    ) {
        // Su attivazione riuscita, ripubblica subito l'ultimo payload se
        // ne abbiamo gia' uno costruito. Coperto comunque dalle subscription
        // ai Flow Kotlin, che emettono il valore corrente alla iscrizione.
    }

    nonisolated func sessionDidBecomeInactive(_ session: WCSession) {}
    nonisolated func sessionDidDeactivate(_ session: WCSession) {
        // iOS: re-attivare per supportare lo switch tra orologi multipli.
        WCSession.default.activate()
    }
}
