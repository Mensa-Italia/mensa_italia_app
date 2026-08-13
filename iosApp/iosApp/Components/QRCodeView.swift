import SwiftUI
import CoreImage.CIFilterBuiltins

/// Disegna un QR a partire da una stringa.
///
/// Stava dentro `Features/Card/CardView.swift`, perche' era nato per il QR
/// della tessera. Quel QR non esiste piu' (non c'era niente da leggerci), ma i
/// biglietti hanno un QR vero, che arriva dal backend: la vista serve ancora e
/// vive qui, dove non dipende da una feature.
struct QRCodeView: View {
    let payload: String
    let size: CGFloat

    var body: some View {
        if !payload.isEmpty, let image = generate() {
            Image(uiImage: image)
                .interpolation(.none)
                .resizable()
                .scaledToFit()
                .frame(width: size, height: size)
                .background(.white)
                .clipShape(.rect(cornerRadius: 12))
        } else {
            Image(systemName: "qrcode")
                .resizable().scaledToFit()
                .frame(width: size, height: size)
                .foregroundStyle(.secondary)
        }
    }

    private func generate() -> UIImage? {
        let filter = CIFilter.qrCodeGenerator()
        filter.message = Data(payload.utf8)
        filter.correctionLevel = "M"
        guard let outputImage = filter.outputImage else { return nil }
        let scaleX = size * UIScreen.main.scale / outputImage.extent.size.width
        let scaled = outputImage.transformed(by: CGAffineTransform(scaleX: scaleX, y: scaleX))
        let context = CIContext()
        guard let cg = context.createCGImage(scaled, from: scaled.extent) else { return nil }
        return UIImage(cgImage: cg)
    }
}
