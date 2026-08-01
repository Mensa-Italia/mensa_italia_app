import AuthenticationServices
import Foundation
import UIKit

/// Ponte fra le WebAuthn options che arrivano dal backend e `ASAuthorization`.
///
/// Su Android l'equivalente e' banale, perche' Credential Manager accetta e
/// restituisce direttamente il JSON WebAuthn. `ASAuthorization` no: vuole i
/// campi decodificati in ingresso e restituisce oggetti tipizzati in uscita,
/// quindi il JSON di risposta va ricostruito a mano. Tutto quel lavoro sta qui,
/// cosi' i ViewModel vedono solo `String` in e `String` out.
///
/// L'encoding e' base64url senza padding in entrambe le direzioni: e' il formato
/// che WebAuthn richiede ed e' anche quello che Zitadel emette e si aspetta
/// (vedi il commento in MensaCore/tools/zauth/webauthn_json.go).
@MainActor
final class PasskeyAuthenticator: NSObject {

    /// Deve combaciare con `zauth.PasskeyRPID` lato server e con il dominio che
    /// serve l'apple-app-site-association. Resta come fallback: l'`rpId` vero
    /// arriva dentro le options.
    static let defaultRelyingPartyID = "auth.mensa.it"

    enum Failure: Error {
        /// Le options non sono decodificabili: bug di protocollo, non un caso utente.
        case malformedOptions(String)
        /// L'utente ha annullato, **oppure** non c'e' nessuna passkey su questo
        /// device. iOS riporta i due casi con lo stesso `ASAuthorizationError.canceled`
        /// e non sono distinguibili: chi chiama deve trattarli allo stesso modo,
        /// cioe' riportando l'utente alla password senza mostrare un errore.
        case cancelledOrNoCredential
        case authorizationFailed(Error)
        /// Il registration non ha restituito l'attestation object.
        case missingAttestation
    }

    private var continuation: CheckedContinuation<ASAuthorizationCredential, Error>?

    // MARK: - Login

    /// Esegue l'assertion e ritorna il `PublicKeyCredential` JSON da mandare a
    /// `/api/cs/auth-with-passkey/finish`.
    func assert(requestOptionsJSON: String) async throws -> String {
        let options = try AssertionOptions(json: requestOptionsJSON)

        let provider = ASAuthorizationPlatformPublicKeyCredentialProvider(
            relyingPartyIdentifier: options.rpID ?? Self.defaultRelyingPartyID
        )
        let request = provider.createCredentialAssertionRequest(challenge: options.challenge)

        if !options.allowedCredentialIDs.isEmpty {
            request.allowedCredentials = options.allowedCredentialIDs.map {
                ASAuthorizationPlatformPublicKeyCredentialDescriptor(credentialID: $0)
            }
        }
        if let preference = options.userVerification {
            request.userVerificationPreference = .init(rawValue: preference)
        }

        // preferImmediatelyAvailableCredentials: senza una passkey locale
        // fallisce subito invece di aprire uno sheet che qui non porterebbe da
        // nessuna parte — abbiamo scelto di non supportare il flusso QR
        // cross-device, quindi l'unica alternativa e' la password.
        let credential = try await perform(
            requests: [request],
            options: .preferImmediatelyAvailableCredentials
        )

        guard let assertion = credential as? ASAuthorizationPlatformPublicKeyCredentialAssertion else {
            throw Failure.authorizationFailed(Failure.malformedOptions("unexpected credential type"))
        }

        return try encodeJSON([
            "id": assertion.credentialID.base64URLString,
            "rawId": assertion.credentialID.base64URLString,
            "type": "public-key",
            "authenticatorAttachment": "platform",
            "clientExtensionResults": [String: String](),
            "response": [
                "clientDataJSON": assertion.rawClientDataJSON.base64URLString,
                "authenticatorData": assertion.rawAuthenticatorData.base64URLString,
                "signature": assertion.signature.base64URLString,
                "userHandle": assertion.userID.base64URLString,
            ],
        ])
    }

    // MARK: - Registrazione

    /// Crea la passkey e ritorna il `PublicKeyCredential` JSON da mandare a
    /// `/api/cs/passkeys/finish`.
    func create(creationOptionsJSON: String) async throws -> String {
        let options = try CreationOptions(json: creationOptionsJSON)

        let provider = ASAuthorizationPlatformPublicKeyCredentialProvider(
            relyingPartyIdentifier: options.rpID ?? Self.defaultRelyingPartyID
        )
        let request = provider.createCredentialRegistrationRequest(
            challenge: options.challenge,
            name: options.userName,
            userID: options.userID
        )
        if let preference = options.userVerification {
            request.userVerificationPreference = .init(rawValue: preference)
        }

        // excludedCredentials esiste solo da iOS 17.4. Sotto quella soglia
        // l'utente puo' creare una seconda passkey per lo stesso account sullo
        // stesso device: Zitadel le accetta entrambe, e la schermata di gestione
        // permette di rimuovere il duplicato. Nessun dato a rischio, solo una
        // lista piu' disordinata.
        if #available(iOS 17.4, *), !options.excludedCredentialIDs.isEmpty {
            request.excludedCredentials = options.excludedCredentialIDs.map {
                ASAuthorizationPlatformPublicKeyCredentialDescriptor(credentialID: $0)
            }
        }

        let credential = try await perform(requests: [request], options: [])

        guard let registration = credential as? ASAuthorizationPlatformPublicKeyCredentialRegistration else {
            throw Failure.authorizationFailed(Failure.malformedOptions("unexpected credential type"))
        }
        guard let attestation = registration.rawAttestationObject else {
            throw Failure.missingAttestation
        }

        return try encodeJSON([
            "id": registration.credentialID.base64URLString,
            "rawId": registration.credentialID.base64URLString,
            "type": "public-key",
            "authenticatorAttachment": "platform",
            "clientExtensionResults": [String: String](),
            "response": [
                "clientDataJSON": registration.rawClientDataJSON.base64URLString,
                "attestationObject": attestation.base64URLString,
            ],
        ])
    }

    // MARK: - Esecuzione

    private func perform(
        requests: [ASAuthorizationRequest],
        options: ASAuthorizationController.RequestOptions
    ) async throws -> ASAuthorizationCredential {
        try await withCheckedThrowingContinuation { continuation in
            self.continuation = continuation
            let controller = ASAuthorizationController(authorizationRequests: requests)
            controller.delegate = self
            controller.presentationContextProvider = self
            controller.performRequests(options: options)
        }
    }

    private func resume(with result: Result<ASAuthorizationCredential, Error>) {
        guard let continuation else { return }
        self.continuation = nil
        continuation.resume(with: result)
    }

    private func encodeJSON(_ payload: [String: Any]) throws -> String {
        let data = try JSONSerialization.data(withJSONObject: payload, options: [])
        guard let json = String(data: data, encoding: .utf8) else {
            throw Failure.malformedOptions("credential payload is not valid UTF-8")
        }
        return json
    }
}

// MARK: - Delegate

extension PasskeyAuthenticator: ASAuthorizationControllerDelegate {
    func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization authorization: ASAuthorization
    ) {
        resume(with: .success(authorization.credential))
    }

    func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithError error: Error
    ) {
        if let authError = error as? ASAuthorizationError, authError.code == .canceled {
            resume(with: .failure(Failure.cancelledOrNoCredential))
            return
        }
        resume(with: .failure(Failure.authorizationFailed(error)))
    }
}

extension PasskeyAuthenticator: ASAuthorizationControllerPresentationContextProviding {
    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        // Stessa risoluzione della scena usata da StripeService.
        let scene = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first { $0.activationState == .foregroundActive }
            ?? UIApplication.shared.connectedScenes
                .compactMap { $0 as? UIWindowScene }
                .first
        return scene?.keyWindow ?? scene?.windows.first ?? ASPresentationAnchor()
    }
}

// MARK: - Decodifica delle options

/// Sottoinsieme di `PublicKeyCredentialRequestOptions` che serve ad ASAuthorization.
private struct AssertionOptions {
    let challenge: Data
    let rpID: String?
    let allowedCredentialIDs: [Data]
    let userVerification: String?

    init(json: String) throws {
        let root = try PasskeyOptionsDecoder.object(from: json)

        guard let challenge = PasskeyOptionsDecoder.data(root["challenge"]) else {
            throw PasskeyAuthenticator.Failure.malformedOptions("challenge")
        }
        self.challenge = challenge
        self.rpID = root["rpId"] as? String
        self.userVerification = root["userVerification"] as? String
        self.allowedCredentialIDs = PasskeyOptionsDecoder.credentialIDs(root["allowCredentials"])
    }
}

/// Sottoinsieme di `PublicKeyCredentialCreationOptions`.
private struct CreationOptions {
    let challenge: Data
    let rpID: String?
    let userName: String
    let userID: Data
    let excludedCredentialIDs: [Data]
    let userVerification: String?

    init(json: String) throws {
        let root = try PasskeyOptionsDecoder.object(from: json)

        guard let challenge = PasskeyOptionsDecoder.data(root["challenge"]) else {
            throw PasskeyAuthenticator.Failure.malformedOptions("challenge")
        }
        self.challenge = challenge
        self.rpID = (root["rp"] as? [String: Any])?["id"] as? String

        guard let user = root["user"] as? [String: Any] else {
            throw PasskeyAuthenticator.Failure.malformedOptions("user")
        }
        guard let userID = PasskeyOptionsDecoder.data(user["id"]) else {
            throw PasskeyAuthenticator.Failure.malformedOptions("user.id")
        }
        self.userID = userID
        // `name` e' il login name mostrato nel prompt di sistema e salvato nel
        // portachiavi; se manca, meglio una stringa vuota che far fallire tutto.
        self.userName = (user["name"] as? String) ?? ""

        self.userVerification =
            (root["authenticatorSelection"] as? [String: Any])?["userVerification"] as? String
        self.excludedCredentialIDs = PasskeyOptionsDecoder.credentialIDs(root["excludeCredentials"])
    }
}

private enum PasskeyOptionsDecoder {
    static func object(from json: String) throws -> [String: Any] {
        guard let data = json.data(using: .utf8),
              let root = try JSONSerialization.jsonObject(with: data) as? [String: Any]
        else {
            throw PasskeyAuthenticator.Failure.malformedOptions("root object")
        }
        return root
    }

    static func data(_ value: Any?) -> Data? {
        guard let encoded = value as? String else { return nil }
        return Data(base64URLEncoded: encoded)
    }

    static func credentialIDs(_ value: Any?) -> [Data] {
        guard let list = value as? [[String: Any]] else { return [] }
        return list.compactMap { data($0["id"]) }
    }
}

// MARK: - base64url

private extension Data {
    /// base64url senza padding, come richiesto da WebAuthn JSON.
    var base64URLString: String {
        base64EncodedString()
            .replacingOccurrences(of: "+", with: "-")
            .replacingOccurrences(of: "/", with: "_")
            .replacingOccurrences(of: "=", with: "")
    }

    /// Tollera anche il padding e l'alfabeto standard, cosi' un cambio di
    /// encoding lato server non ci rompe silenziosamente.
    init?(base64URLEncoded value: String) {
        var normalized = value
            .replacingOccurrences(of: "-", with: "+")
            .replacingOccurrences(of: "_", with: "/")
        while normalized.count % 4 != 0 { normalized += "=" }
        guard let decoded = Data(base64Encoded: normalized) else { return nil }
        self = decoded
    }
}
