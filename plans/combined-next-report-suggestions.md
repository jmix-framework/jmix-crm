# Combined Next Report Suggestions (Category Focus)

Dieses Dokument kombiniert die besten Report-Ideen für eine tiefgehende Kategorien-Analyse. Diese Reports sind darauf optimiert, komplexe Business-Zusammenhänge aufzubereiten, die über einfache Datenbankabfragen (JPQL) nicht effizient darstellbar sind.

---

## 1. Category Health & Vitality Score (Der Allrounder)

### Idee
Ein kombinierter Report, der jeder Kategorie einen "Health Score" (0-100) zuweist und sie in Klassen (`STRONG`, `WATCH`, `AT_RISK`) einteilt. Der Score basiert auf gewichteten Kennzahlen wie Umsatztrend (PoP), Neukunden-Quote und Storno-Raten.

### Warum das eine gute Idee ist
*   Bietet dem Management eine sofort priorisierbare Liste.
*   Ideal für LLM-Analysen: Die KI muss nicht hunderte Zeilen Rohdaten interpretieren, sondern erhält eine fundierte Bewertungsgrundlage.

### Warum das nicht mit JPQL einfach geht
*   **Gewichtung:** Mathematische Kombination verschiedener Metriken mit unterschiedlichen Prioritäten.
*   **Perioden-Vergleich:** Zeitgleicher Vergleich von aktuellen 30 Tagen vs. Vorperiode (30-60 Tage) erfordert in JPQL sehr komplexe Subqueries.
*   **Klassifizierung:** Die Logik zur Einordnung in `STRONG/WATCH/AT_RISK` ist eine klassische Business-Regel, die in Java deutlich sauberer abgebildet wird.

### Beispiel-Fragen an die AI
*   "Welche Kategorien zeigen Anzeichen für einen 'Gesundheits-Einbruch' im Vergleich zum letzten Quartal?"
*   "Erstelle eine Liste unserer 5 stärksten Wachstumskategorien mit einer Zusammenfassung der Gründe."

---

## 2. Operational Status Velocity & Bottleneck Report (Operations)

### Idee
Dieser Report analysiert die Effizienz der Auftragsabwicklung pro Kategorie. Er misst die durchschnittliche Verweildauer von Aufträgen in den verschiedenen Statusphasen (z. B. von `ACCEPTED` zu `DONE`) und identifiziert signifikante Abweichungen vom Gesamtschnitt.

### Warum das eine gute Idee ist
*   Identifiziert operative Probleme (z. B. "Warum dauert Consulting-Abwicklung 40% länger als Software-Lizenzen?").
*   Ermöglicht der KI, prozessorientierte Verbesserungsvorschläge zu machen.

### Warum das nicht mit JPQL einfach geht
*   **Zeitdifferenz-Aggregation:** Durchschnittliche Zeitdifferenzen über Status-Historien hinweg sind in SQL schwer zu aggregieren.
*   **Kalender-Logik:** Ausschluss von Wochenenden oder Feiertagen bei der Messung von "Arbeitstagen" ist in JPQL unmöglich.
*   **Benchmark-Vergleich:** Der Vergleich "Kategorie-Schnitt vs. Global-Schnitt" erfordert zwei Aggregations-Ebenen in einem Lauf.

### Beispiel-Fragen an die AI
*   "Wo ist unser operativer Flaschenhals bei der Bearbeitung von Hardware-Aufträgen?"
*   "Gibt es Kategorien, in denen die Durchlaufzeit in der letzten Woche sprunghaft angestiegen ist?"

---

## 3. Category Cashflow Risk Allocation Report (Finance & Sales)

### Idee
Rankt Kategorien nach echtem Cashflow-Risiko. Es werden offene Forderungen (Invoices) auf Kategorien zurückgerechnet (auch bei Teilzahlungen) und Kennzahlen wie "Days-to-Cash" und "Revenue at Risk" pro Kategorie ausgewiesen.

### Warum das eine gute Idee ist
*   Verbindet Vertriebserfolg mit finanzieller Realität (Umsatz vs. echter Cash-Eingang).
*   Hilft der KI, Warnungen auszusprechen, wenn eine Kategorie zwar viel verkauft, aber die Kunden dort nicht zahlen.

### Warum das nicht mit JPQL einfach geht
*   **Teilzahlungs-Allokation:** Die Verteilung einer Teilzahlung einer Rechnung auf die enthaltenen OrderItems (und damit auf die Kategorien) ist eine hochgradig imperative Rechenlogik.
*   **Daten-Kette:** Die Verbindung über `Payment -> Invoice -> Order -> OrderItem -> Category` ist relational tief und in einer flachen Query schwer performant und korrekt zu aggregieren.
*   **Risiko-Projektion:** Berechnung von Erwartungswerten basierend auf historischem Zahlungsverhalten.

### Beispiel-Fragen an die AI
*   "Welche Kategorien haben das schlechteste Zahlungsverhalten, obwohl der Umsatz hoch ist?"
*   "Wie hoch ist der 'Revenue at Risk' in der Kategorie 'Professional Services' aktuell?"
