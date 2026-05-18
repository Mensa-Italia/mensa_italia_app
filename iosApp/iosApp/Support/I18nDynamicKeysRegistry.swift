import Foundation

/// Translation key registry for dynamic Tolgee keys.
///
/// Several screens look up Tolgee keys whose names come from PocketBase data
/// (orgchart group titles, document categories, …). The Swift source uses
/// `tr(raw, fallback: pretty)` where `raw` is a runtime String — so the
/// `tools/tolgee-push.sh` extractor (which greps literal `tr("key", fallback:)`
/// pairs out of `.swift` files) cannot see them.
///
/// To keep those keys flowing into Tolgee, declare them here as plain
/// `tr("literal", fallback: "Italiano")` calls. The function is `@inline(never)`
/// and never invoked at runtime — it exists purely so the extractor picks up
/// the literals. Add a new entry whenever the backend ships a new dynamic key
/// that should be translated.
@inline(never)
private func _i18nDynamicKeysRegistry() {
    // Suppress unused warnings — these calls have no runtime side-effect.
    let registered: [String] = [
        // MARK: - Org chart groups (PocketBase `org_chart_groups.title`)
        tr("consiglio", fallback: "Consiglio Direttivo"),
        tr("comitato_dei_garanti", fallback: "Comitato dei Garanti"),
        tr("mensa_ludo", fallback: "Mensa Ludo"),
        tr("mensa_fiere", fallback: "Mensa Fiere"),
        tr("mensa_news", fallback: "Mensa News"),
        tr("redazione_quid", fallback: "Redazione QuID"),
        tr("team_developer", fallback: "Team Sviluppo"),
        tr("team_comunicazione", fallback: "Team Comunicazione"),

        // MARK: - Documents categories (PocketBase `area_documents.category`)
        tr("bilanci", fallback: "Bilanci"),
        tr("elezioni", fallback: "Elezioni"),
        tr("eventi_progetti", fallback: "Eventi e Progetti"),
        tr("materiale_comunicazione", fallback: "Materiale Comunicazione"),
        tr("modulistica_contratti", fallback: "Modulistica e Contratti"),
        tr("news_pubblicazioni", fallback: "News e Pubblicazioni"),
        tr("normativa_interna", fallback: "Normativa Interna"),
        tr("tesoreria_contabilita", fallback: "Tesoreria e Contabilità"),
        tr("verbali_delibere", fallback: "Verbali e Delibere"),
    ]
    _ = registered
}
