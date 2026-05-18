import SwiftUI

/// Three animated pulsing dots used as a subtle loading indicator.
struct LoadingDots: View {
    @State private var phase: Double = 0

    var body: some View {
        HStack(spacing: 6) {
            ForEach(0..<3, id: \.self) { i in
                Circle()
                    .frame(width: 8, height: 8)
                    .foregroundStyle(.white.opacity(0.7))
                    .scaleEffect(dotScale(index: i))
                    .animation(
                        .easeInOut(duration: 0.5)
                            .repeatForever()
                            .delay(Double(i) * 0.15),
                        value: phase
                    )
            }
        }
        .onAppear { phase = 1 }
    }

    private func dotScale(index: Int) -> CGFloat {
        phase == 0 ? 1.0 : (index == Int(phase * 3) % 3 ? 1.4 : 1.0)
    }
}
