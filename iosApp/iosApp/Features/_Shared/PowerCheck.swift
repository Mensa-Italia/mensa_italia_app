import Foundation
import Shared

/// True if the user has `p`, `p_helper`, or `super` in `user.powers`.
/// `super` always passes.
func hasPower(_ p: String, user: UserModel?) -> Bool {
    guard let user else { return false }
    let powers = Set(user.powers)
    if powers.contains("super") { return true }
    if powers.contains(p) { return true }
    if powers.contains("\(p)_helper") { return true }
    return false
}

/// True if the user is allowed to see/open the given addon, based on
/// the addon's `requiredPower`. Power "level" is encoded as a small int
/// matching the Flutter app's contract:
///   0 → everyone
///   1+ → must have at least one matching power tag
func userCanSeeAddon(_ addon: AddonModel, user: UserModel?) -> Bool {
    if addon.requiredPower == 0 { return true }
    guard let user else { return false }
    // Liberal interpretation: super passes; otherwise require any power
    // tag whose name contains the addon id (e.g. "stamp", "stamp_helper").
    let powers = Set(user.powers)
    if powers.contains("super") { return true }
    return hasPower(addon.id, user: user)
}
