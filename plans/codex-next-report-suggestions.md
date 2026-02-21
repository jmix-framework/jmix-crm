# Codex Plan: Next Report Suggestions (Category Performance)

## Ziel
Wir wollen neben `client-360-report` einen weiteren Showcase-Report haben, der den Mehrwert vom Report-Tool gegenueber einfachen JPQL-Abfragen zeigt. Fokus: Kategorien als "laeuft gut / laeuft schlecht" mit nachvollziehbarer Begruendung.

## 1) Category Health Score Report

### Idee
Ein Report, der jede Kategorie mit einem zusammengesetzten Health-Score (0-100) bewertet und in Klassen einsortiert:
- `STRONG` (laeuft gut)
- `WATCH` (neutral/unsicher)
- `AT_RISK` (laeuft schlecht)

Score-Komponenten (gewichtet):
- Umsatztrend (z. B. letzte 30 Tage vs. vorherige 30 Tage)
- Marge/Deckungsbeitrag proxy (net/gross + Rabatt + VAT)
- Zahlungsrisiko (Anteil overdue invoices)
- Nachfrage-Stabilitaet (Varianz/Volatilitaet der Mengen)
- Kundenkonzentration (Abhaengigkeit von wenigen Top-Kunden)

### Warum das eine gute Idee ist
- Liefert sofort priorisierbare Kategorie-Liste fuer Sales/Management.
- Sehr gut erklaerbar im Chat: "warum rot/gelb/gruen" pro Kategorie.
- Demonstriert, dass das Report-Tool nicht nur Daten abfragt, sondern Business-Logik kapselt (aehnlich wie `client-360-report`).

### Warum das nicht mit JPQL einfach geht
- Gewichteter Composite-Score aus mehreren Kennzahlen und Schwellen.
- Rolling-Window-Vergleiche (Period-over-Period) plus Normalisierung.
- Risiko-Anteile aus mehreren Entitaeten (Order/Invoice/Payment) muessen sauber zusammengefuehrt werden.
- Regel-Engine-artige Klassifikation (`STRONG/WATCH/AT_RISK`) ist eher Service-/Java-Logik als reine JPQL-Abfrage.

### Beispiel-Fragen an die AI
- "Zeig mir die 10 Kategorien, die im letzten Quartal am schlechtesten laufen, mit Begruendung."
- "Welche Kategorien sind stabil stark und welche sind nur kurzfristig gut?"
- "Welche Kategorien sind wegen Zahlungsrisiko kritisch, obwohl der Umsatz noch okay ist?"

## 2) Category Momentum & Early Warning Report

### Idee
Ein fruehes Warnsystem auf Kategorie-Ebene:
- erkennt Trendbrueche,
- markiert ungewoehnliche Einbrueche/Spikes,
- liefert "Early Warning"-Flags mit Severity.

Beispiel-Flags:
- `DEMAND_DROP`
- `PAYMENT_RISK_SPIKE`
- `DISCOUNT_PRESSURE`
- `PIPELINE_GAP` (wenig neue Orders bei historisch hoher Nachfrage)

### Warum das eine gute Idee ist
- Zeigt echten operativen Nutzen: nicht nur Rueckblick, sondern Frueherkennung.
- Sehr gutes Demo-Narrativ fuer AI: "Was kippt gerade und wo muss ich handeln?"
- Erweitert den Mehrwert von `client-360-report` um Portfolio-/Category-Sicht.

### Warum das nicht mit JPQL einfach geht
- Anomalie-Erkennung braucht Baseline-Modelle (gleitende Durchschnitte, Z-Score/MAD, Trend-Slope).
- Mehrstufige Logik: erst Daten verdichten, dann statistische Ausreisser erkennen, dann Flags priorisieren.
- Dynamische Schwellen (z. B. Kategorie-spezifisch statt global) sind in JPQL allein unhandlich.
- Fuer robuste Ergebnisse braucht man In-Memory-Berechnung/Service-Logik nach der Datenselektion.

### Beispiel-Fragen an die AI
- "Welche Kategorien zeigen seit 6 Wochen einen negativen Trend und sollten wir sofort pruefen?"
- "Wo gibt es bei Kategorien einen ploezlichen Nachfragerueckgang trotz normaler Saison?"
- "Zeig mir Kategorien mit fruehen Warnsignalen, bevor sie in `AT_RISK` fallen."

## 3) Category Cashflow Risk Allocation Report

### Idee
Kategorien nach echtem Cashflow-Risiko ranken, nicht nur nach Umsatz:
- Offene Forderungen auf Kategorien allokieren (inkl. Teilzahlungen).
- Days-to-Cash pro Kategorie berechnen.
- "Revenue at Risk" je Kategorie ausweisen.
- Ergebnisliste: Kategorien mit hohem Umsatz, aber schlechtem Cashflow-Verhalten.

### Warum das eine gute Idee ist
- Bringt Finance + Sales zusammen: "viel verkauft" ist nicht automatisch "gesund".
- Liefert harte Priorisierung fuer Mahn-/Vertriebsaktionen.
- Sehr stark fuer Tool-Demo, weil Zusammenhaenge ueber Order -> Invoice -> Payment sichtbar werden.

### Warum das nicht mit JPQL einfach geht
- Teilzahlungen muessen auf Invoice- und indirekt auf Kategorie-Anteile verteilt werden.
- Korrekte Risikoallokation benoetigt mehrstufige Berechnung (OrderItem-Anteile, Payment-Zuordnung, Zeitachsen).
- Zeitliche Kennzahlen wie Days-to-Cash und Exposure-Entwicklung ueber Perioden sind aggregationslogisch komplex.
- Ergebnis ist nicht nur ein Query-Result, sondern eine berechnete Sicht mit Erklaertexten pro Kategorie.

### Beispiel-Fragen an die AI
- "Welche Kategorien haben das hoechste Umsatz-zu-Cashflow-Risiko in den letzten 90 Tagen?"
- "In welchen Kategorien steigt die offene Forderung schneller als der Umsatz?"
- "Welche Top-Umsatz-Kategorien sollte Finance zuerst angehen, weil Cash-In zu langsam ist?"

## Empfehlung fuer den naechsten Schritt
Wenn wir nur einen Report als naechsten Showcase bauen, sollte es **Report 1: Category Health Score Report** sein:
- am einfachsten als "gut/schlecht Kategorienliste" kommunizierbar,
- direkt anschlussfaehig an bestehende `Client360`-Logik,
- hoher Demo-Wert fuer AI + Report Tool bei moderatem Implementierungsrisiko.
