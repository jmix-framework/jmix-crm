# Gemini Next Report Suggestions

Dieses Dokument beschreibt drei Vorschläge für neue Jmix-Reports, die speziell darauf ausgelegt sind, den Mehrwert des `RunReportTool` für das LLM zu demonstrieren. Diese Reports nutzen komplexe Business-Logik in Java, die über einfache JPQL-Abfragen nicht oder nur sehr schwer abbildbar ist.

---

## 1. Category Vitality & Trend Matrix (Vitalitäts-Score)

### Ziel
Eine detaillierte Analyse der Produktkategorien, die nicht nur den Umsatz betrachtet, sondern einen "Vitalitäts-Score" berechnet. Dieser Score zeigt, welche Kategorien gesund wachsen und welche trotz Umsatzes "sterben".

### Warum eine gute Idee?
Es liefert der KI eine fertige Einschätzung der Marktlage pro Kategorie, anstatt dass die KI hunderte Einzeldatensätze selbst aggregieren muss.

### Warum nicht einfach mit JPQL?
* **Perioden-Vergleich:** Vergleiche von "Umsatz letzten 30 Tage" vs. "Umsatz 30-60 Tage davor" sind in JPQL extrem sperrig (erfordern komplexe Subqueries oder Joins).
* **Gewichtetes Scoring:** Der Score könnte sich aus mehreren Faktoren zusammensetzen: (Umsatz-Wachstum * 0.5) + (Neukunden-Quote * 0.3) - (Stornierungs-Rate * 0.2). Solche mathematischen Gewichtungen über Zeiträume hinweg sind in Java-DataLoadern deutlich wartbarer.
* **Fuzzy Trends:** Erkennung von Trends (z.B. "sinkt seit 3 Wochen stetig"), was sequentielle Datenverarbeitung erfordert.

### Beispiel-Fragen an die AI
* "Welche Kategorien verlieren aktuell an Bedeutung, obwohl sie historisch stark waren?"
* "Erstelle mir eine Liste von 'Rising Stars' unter unseren Produktkategorien für das Management-Meeting."
* "Welche Kategorien haben die schlechteste Neukunden-Conversion?"

---

## 2. Operational Status Velocity & Bottleneck Report

### Ziel
Analyse der Effizienz der Auftragsabwicklung pro Kategorie. Wie lange verweilen Aufträge in bestimmten Status (z.B. von `ACCEPTED` zu `DONE`) und wo liegen die Verzögerungen?

### Warum eine gute Idee?
Es zeigt operative Schwachstellen auf. Die KI kann so gezielte Prozessverbesserungen vorschlagen.

### Warum nicht einfach mit JPQL?
* **Zeit-Differenzen zwischen Status:** JPQL kann zwar zwei Datumsfelder vergleichen, aber die Aggregation von *Durchschnittswerten der Differenzen* über eine ganze Prozesskette (Status-Historie) hinweg ist komplex, besonders wenn Feiertage oder Arbeitszeiten (Java-Logik) berücksichtigt werden sollen.
* **Engpass-Identifikation:** Die Logik, welcher Status im Vergleich zum Durchschnitt aller Kategorien "zu langsam" ist, erfordert zwei Durchläufe über die Daten (Gesamt-Schnitt vs. Kategorie-Schnitt), was in Java trivial, in JPQL aber ein Albtraum ist.

### Beispiel-Fragen an die AI
* "Wo ist unser Flaschenhals bei der Bearbeitung von Software-Lizenzen im Vergleich zu Hardware-Bestellungen?"
* "Warum dauern Bestellungen in der Kategorie 'Consulting' aktuell 20% länger als im Vormonat?"
* "Gibt es einen Account Manager, dessen Aufträge in der 'Pending'-Phase hängen bleiben?"

---

## 3. Cross-Category Affinity & Gap Analysis (Warenkorb-Analyse)

### Ziel
Identifikation von Abhängigkeiten zwischen Kategorien. Welche Kategorien werden oft zusammen gekauft und bei welchen Kunden fehlt die "logische" zweite Kategorie (Cross-Selling-Potenzial)?

### Warum eine gute Idee?
Es bietet direkten vertrieblichen Nutzen. Die KI kann konkrete "Next-Best-Offer"-Vorschläge generieren.

### Warum nicht einfach mit JPQL?
* **Warenkorb-Logik (Market Basket):** "Finde alle Kunden, die A gekauft haben, aber noch nie B" ist in JPQL über `NOT EXISTS` möglich, aber "Finde die Top 3 Kategorien, die typischerweise 2 Monate nach Kategorie A gekauft werden" ist extrem schwer abzubilden.
* **Affinitäts-Index:** Die Berechnung eines Wahrscheinlichkeits-Scores für Cross-Selling basiert auf statistischen Berechnungen, die in Java (z.B. mit Apache Commons Math oder einfacher Set-Logik) effizienter sind.

### Beispiel-Fragen an die AI
* "Welche Kunden haben Hardware gekauft, aber noch keinen passenden Service-Vertrag?"
* "Welche Produktkategorien ergänzen sich am besten und sollten wir als Bundle anbieten?"
* "Identifiziere Cross-Selling-Lücken bei unseren Top-10-Kunden basierend auf ihrem bisherigen Kaufverhalten."
