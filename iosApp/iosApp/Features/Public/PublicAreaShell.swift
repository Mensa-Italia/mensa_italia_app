import SwiftUI

/// Root shell del flusso "esplora senza account".
///
/// Mounted da `RootView` quando `guestMode == true` e l'auth state e'
/// `.anonymous`. Ospita la `PublicAreaView` come radice di un suo
/// `NavigationStack` dedicato (niente back button — siamo a livello root).
/// Sovrappone il `MiniAudioPlayer` cosi' l'utente che fa partire un podcast
/// dall'area pubblica ne mantiene il controllo navigando.
struct PublicAreaShell: View {
    var body: some View {
        NavigationStack {
            PublicAreaView()
        }
        .miniPlayerOverlay()
    }
}
