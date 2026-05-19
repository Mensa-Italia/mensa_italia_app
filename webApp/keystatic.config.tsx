import { config, collection, singleton, fields } from "@keystatic/core";

function MensaMark() {
  return (
    <svg
      viewBox="0 0 86 86"
      fill="currentColor"
      aria-hidden="true"
      style={{ width: 24, height: 24, color: "var(--color-mensa-blue, #1d2f6f)" }}
    >
      <path
        fillRule="evenodd"
        d="M6.5,23.03v41.69l7.81,4.73l7.81,-4.73v-16.96l13.43,7.76v25.99l7.48,4.43l7.48,-4.2v-26.22s13.87,-7.76 13.87,-7.76v16.96l7.56,4.73l7.56,-4.73V23.03s-36.46,19.45 -36.46,19.45L6.5,23.03Z"
      />
      <path d="M43,0.05C33.35,0.05 25.5,7.84 25.5,17.42s7.85,17.36 17.5,17.36s17.5,-7.79 17.5,-17.36S52.65,0.05 43,0.05ZM42.45,4.68s0.11,-0.01 0.17,-0.01v12.25h-5.77c0.07,-3.73 1.17,-7.09 2.87,-9.52c0.81,-1.14 1.73,-2.06 2.73,-2.72ZM40.58,4.89c-0.71,0.61 -1.35,1.34 -1.92,2.15c-1.8,2.57 -2.93,6.04 -3,9.88h-5.53c0.23,-5.99 4.65,-10.94 10.45,-12.03ZM30.13,17.91h5.53c0.1,3.79 1.22,7.21 3,9.76c0.62,0.88 1.32,1.65 2.09,2.3c-5.88,-1.02 -10.4,-6.01 -10.62,-12.06ZM39.72,27.31c-1.69,-2.4 -2.77,-5.72 -2.87,-9.4h5.77v12.23c-1.07,-0.67 -2.05,-1.63 -2.9,-2.83ZM55.87,16.92h-5.31c-0.07,-3.84 -1.19,-7.31 -2.99,-9.88c-0.56,-0.79 -1.18,-1.5 -1.87,-2.09c5.66,1.19 9.95,6.08 10.17,11.97ZM43.61,4.68c0.07,0 0.13,0 0.19,0c0.99,0.66 1.91,1.58 2.71,2.71c1.7,2.43 2.8,5.79 2.87,9.52h-5.77V4.68ZM43.61,30.14v-12.23h5.77c-0.1,3.68 -1.19,7 -2.87,9.4c-0.85,1.2 -1.84,2.16 -2.9,2.83ZM45.52,29.92c0.76,-0.62 1.44,-1.39 2.05,-2.25c1.78,-2.55 2.89,-5.97 2.99,-9.76h5.31c-0.22,5.96 -4.6,10.88 -10.35,12.01Z" />
    </svg>
  );
}

export default config({
  storage: { kind: "local" },

  ui: {
    brand: {
      name: "Mensa Blogging",
      mark: MensaMark,
    },
  },

  collections: {
    news: collection({
      label: "News",
      slugField: "title",
      path: "src/content/news/*",
      format: { contentField: "body" },
      schema: {
        title: fields.slug({ name: { label: "Titolo", validation: { isRequired: true } } }),
        publishedAt: fields.date({ label: "Data pubblicazione" }),
        excerpt: fields.text({ label: "Estratto", multiline: false, validation: { length: { max: 280 } } }),
        body: fields.markdoc({ label: "Corpo" }),
        cover: fields.image({ label: "Copertina", directory: "public/images/news", publicPath: "/images/news/" }),
      },
    }),

    staticPages: collection({
      label: "Pagine istituzionali",
      slugField: "slug",
      path: "src/content/staticPages/*",
      format: { contentField: "body" },
      schema: {
        slug: fields.slug({ name: { label: "Slug locale", validation: { isRequired: true } } }),
        title: fields.text({ label: "Titolo", validation: { length: { min: 1 } } }),
        cluster: fields.select({
          label: "Cluster",
          defaultValue: "chi-siamo",
          options: [
            { label: "Chi siamo", value: "chi-siamo" },
            { label: "Iscriviti", value: "iscriviti" },
            { label: "Concorsi", value: "concorsi" },
            { label: "Intelligenza", value: "intelligenza" },
            { label: "Pubblicazioni", value: "pubblicazioni" },
            { label: "Contatti", value: "contatti" },
            { label: "Note legali", value: "legal" },
            { label: "News", value: "news" },
          ],
        }),
        kicker: fields.text({ label: "Kicker (mini-etichetta sopra al titolo)" }),
        intro: fields.text({ label: "Introduzione (1–2 frasi sotto al titolo)", multiline: true }),
        order: fields.integer({ label: "Ordine nel cluster", defaultValue: 100 }),
        seoTitle: fields.text({ label: "SEO title (opzionale)" }),
        seoDescription: fields.text({ label: "SEO description (opzionale)", multiline: true }),
        sourceUrl: fields.url({ label: "URL originale (mensa.it)" }),
        body: fields.markdoc({ label: "Corpo" }),
      },
    }),

    faq: collection({
      label: "FAQ",
      slugField: "question",
      path: "src/content/faq/*",
      format: { data: "yaml" },
      schema: {
        question: fields.slug({ name: { label: "Domanda", validation: { isRequired: true } } }),
        answer: fields.text({ label: "Risposta", multiline: true }),
        order: fields.integer({ label: "Ordine" }),
        category: fields.select({
          label: "Categoria",
          defaultValue: "Test",
          options: [
            { label: "Iscrizione", value: "Iscrizione" },
            { label: "Test", value: "Test" },
            { label: "Pagamento", value: "Pagamento" },
            { label: "Risultati", value: "Risultati" },
          ],
        }),
      },
    }),
  },

  singletons: {
    about: singleton({
      label: "Pagina: Chi siamo",
      path: "src/content/pages/about",
      format: { contentField: "body" },
      schema: {
        heroKicker: fields.text({ label: "Hero kicker" }),
        heroTitle: fields.text({ label: "Hero titolo" }),
        heroSubtitle: fields.text({ label: "Hero sottotitolo" }),
        title: fields.text({ label: "Titolo (meta)" }),
        subtitle: fields.text({ label: "Sottotitolo (meta)" }),
        body: fields.markdoc({ label: "Corpo" }),
      },
    }),
    iscrizioneInfo: singleton({
      label: "Pagina: Iscrizione info",
      path: "src/content/pages/iscrizione-info",
      format: { contentField: "body" },
      schema: {
        title: fields.text({ label: "Titolo" }),
        subtitle: fields.text({ label: "Sottotitolo" }),
        body: fields.markdoc({ label: "Corpo" }),
      },
    }),
    chapters: singleton({
      label: "Pagina: Gruppi locali",
      path: "src/content/pages/chapters",
      format: { data: "yaml" },
      schema: {
        heroKicker: fields.text({ label: "Hero kicker" }),
        heroTitle: fields.text({ label: "Hero titolo" }),
        heroSubtitle: fields.text({ label: "Hero sottotitolo" }),
        ctaKicker: fields.text({ label: "CTA kicker" }),
        ctaTitle: fields.text({ label: "CTA titolo" }),
        ctaBody: fields.text({ label: "CTA corpo", multiline: true }),
      },
    }),
    podcasts: singleton({
      label: "Pagina: Podcast",
      path: "src/content/pages/podcasts",
      format: { data: "yaml" },
      schema: {
        heroKicker: fields.text({ label: "Hero kicker" }),
        heroTitle: fields.text({ label: "Hero titolo" }),
        heroSubtitle: fields.text({ label: "Hero sottotitolo" }),
      },
    }),
    events: singleton({
      label: "Pagina: Eventi pubblici",
      path: "src/content/pages/events",
      format: { data: "yaml" },
      schema: {
        heroKicker: fields.text({ label: "Hero kicker" }),
        heroTitle: fields.text({ label: "Hero titolo" }),
        heroSubtitle: fields.text({ label: "Hero sottotitolo" }),
        ctaKicker: fields.text({ label: "CTA kicker" }),
        ctaTitle: fields.text({ label: "CTA titolo" }),
        ctaBody: fields.text({ label: "CTA corpo", multiline: true }),
      },
    }),
    quid: singleton({
      label: "Pagina: Quid",
      path: "src/content/pages/quid",
      format: { data: "yaml" },
      schema: {
        heroKicker: fields.text({ label: "Hero kicker" }),
        heroTitle: fields.text({ label: "Hero titolo" }),
        heroSubtitle: fields.text({ label: "Hero sottotitolo" }),
        ctaKicker: fields.text({ label: "CTA kicker" }),
        ctaTitle: fields.text({ label: "CTA titolo" }),
        ctaBody: fields.text({ label: "CTA corpo", multiline: true }),
      },
    }),
    iqtest: singleton({
      label: "Pagina: Test QI",
      path: "src/content/pages/iq-test",
      format: { data: "yaml" },
      schema: {
        heroKicker: fields.text({ label: "Hero kicker" }),
        heroTitle: fields.text({ label: "Hero titolo" }),
        heroSubtitle: fields.text({ label: "Hero sottotitolo" }),
      },
    }),
    home: singleton({
      label: "Pagina: Home",
      path: "src/content/pages/home",
      format: { data: "yaml" },
      schema: {
        heroKicker: fields.text({ label: "Hero kicker" }),
        heroTitle: fields.text({
          label: "Hero titolo",
          description: "Racchiudi una parola tra _underscore_ per renderla in corsivo blu (es. \"Persone _curiose_.\").",
        }),
        heroSubtitle: fields.text({ label: "Hero sottotitolo" }),
        featuresKicker: fields.text({ label: "Features kicker" }),
        featuresTitle: fields.text({ label: "Features titolo" }),
        joinKicker: fields.text({ label: "Join kicker" }),
        joinTitle: fields.text({ label: "Join titolo" }),
        endKicker: fields.text({ label: "End kicker" }),
        endTitle: fields.text({ label: "End titolo" }),
        endSubtitle: fields.text({ label: "End sottotitolo" }),
      },
    }),
  },
});
