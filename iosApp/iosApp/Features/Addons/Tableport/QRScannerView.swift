import SwiftUI
import AVFoundation
import UIKit
import Shared

/// QR scanner backed by AVCaptureSession.
/// On a match formatted `stampId:::verificationCode`, invokes `onScanned`.
struct QRScannerView: View {
    let onScanned: (_ id: String, _ code: String) -> Void
    let onCancel: () -> Void

    var body: some View {
        ZStack {
            QRScannerRepresentable { value in
                guard let payload = StampQRParser.shared.parse(raw: value) else { return }
                onScanned(payload.stampId, payload.verificationCode)
            }
            .ignoresSafeArea()

            VStack {
                HStack {
                    Spacer()
                    Button {
                        onCancel()
                    } label: {
                        Image(systemName: "xmark")
                            .font(.system(size: 16, weight: .bold))
                            .padding(12)
                            .background(.ultraThinMaterial, in: Circle())
                    }
                    .padding(.top, 8)
                    .padding(.trailing, 16)
                }
                Spacer()
                RoundedRectangle(cornerRadius: 24)
                    .strokeBorder(Color.white.opacity(0.9), lineWidth: 3)
                    .frame(width: 260, height: 260)
                Spacer()
                Text(tr(
                    "addons.stamp.scan_hint",
                    fallback: "Inquadra il QR del francobollo"
                ))
                .font(.headline)
                .foregroundStyle(.white)
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(.ultraThinMaterial, in: Capsule())
                .padding(.bottom, 48)
            }
        }
        .preferredColorScheme(.dark)
    }

}

// MARK: - AVCaptureSession wrapper

private struct QRScannerRepresentable: UIViewControllerRepresentable {
    let onMatch: (String) -> Void

    func makeUIViewController(context: Context) -> ScannerVC {
        let vc = ScannerVC()
        vc.onMatch = { value in
            onMatch(value)
        }
        return vc
    }

    func updateUIViewController(_ uiViewController: ScannerVC, context: Context) {}
}

final class ScannerVC: UIViewController, AVCaptureMetadataOutputObjectsDelegate {
    var onMatch: ((String) -> Void)?
    private let session = AVCaptureSession()
    private var previewLayer: AVCaptureVideoPreviewLayer?
    private var didMatch = false

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .black

        guard
            let device = AVCaptureDevice.default(for: .video),
            let input = try? AVCaptureDeviceInput(device: device),
            session.canAddInput(input)
        else { return }
        session.addInput(input)

        let output = AVCaptureMetadataOutput()
        guard session.canAddOutput(output) else { return }
        session.addOutput(output)
        output.setMetadataObjectsDelegate(self, queue: .main)
        output.metadataObjectTypes = [.qr]

        let layer = AVCaptureVideoPreviewLayer(session: session)
        layer.videoGravity = .resizeAspectFill
        layer.frame = view.layer.bounds
        view.layer.addSublayer(layer)
        previewLayer = layer
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        previewLayer?.frame = view.layer.bounds
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        didMatch = false
        if !session.isRunning {
            DispatchQueue.global(qos: .userInitiated).async { [session] in
                session.startRunning()
            }
        }
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        if session.isRunning {
            session.stopRunning()
        }
    }

    func metadataOutput(
        _ output: AVCaptureMetadataOutput,
        didOutput metadataObjects: [AVMetadataObject],
        from connection: AVCaptureConnection
    ) {
        guard !didMatch else { return }
        guard
            let obj = metadataObjects.first as? AVMetadataMachineReadableCodeObject,
            obj.type == .qr,
            let value = obj.stringValue,
            !value.isEmpty
        else { return }
        didMatch = true
        let generator = UINotificationFeedbackGenerator()
        generator.notificationOccurred(.success)
        onMatch?(value)
    }
}
