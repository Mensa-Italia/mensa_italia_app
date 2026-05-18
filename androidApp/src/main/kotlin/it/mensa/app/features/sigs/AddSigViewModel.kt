package it.mensa.app.features.sigs

import androidx.lifecycle.ViewModel
import it.mensa.shared.model.SigModel

// Thin VM — state lives in the composable sheet (mirrors iOS AddSigSheet pattern)
class AddSigViewModel(val sigId: String?) : ViewModel() {
    // Pre-populated when editing. The composable reads this once.
    var initial: SigModel? = null
}
