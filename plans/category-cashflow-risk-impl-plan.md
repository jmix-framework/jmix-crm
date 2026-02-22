# Implementation Plan: Category Cashflow Risk Allocation Report (Runtime-only)

Dieses Dokument beschreibt die Umsetzung eines Kategorie-Risiko-Reports ohne Modell-Erweiterung und ohne zusätzliche Persistenz. Die gesamte Allokation erfolgt zur Laufzeit in Java ("heavy calculation").

## 1. Zielbild und Constraints

- Report Name: `Category Cashflow Risk Allocation Report`
- Report Code: `category-cashflow-risk-report`
- Einstiegspunkt UI: `InvoiceListView`
- Keine neuen Entities/Felder/DB-Tabellen.
- Keine Snapshot-Speicherung bei Invoice-Erstellung.
- Alle Kennzahlen werden zur Laufzeit aus `Invoice -> Order -> OrderItem -> Category` und `Payment` berechnet.

## 2. Fachliche Definitionen

- Allokations-Einheit ist die `Invoice` (nicht die gesamte `Order` aggregiert).
- Kategorie-Shares werden je Rechnung aus den aktuellen `OrderItem`-Totals der zugehörigen `Order` berechnet.
- Falls eine Rechnung keiner Order zugeordnet ist (`invoice.order == null`) oder keine verwertbaren OrderItems existieren, wird die Rechnung vollständig auf Bucket `UNASSIGNED` gebucht.
- Payments werden pro Rechnung auf Kategorie-Anteile verteilt.
- Monetary Precision: `scale=2`, `RoundingMode.HALF_UP`.
- DTC (Days to Cash) wird als gewichteter Mittelwert über allokierte Payment-Beträge berechnet.
- RaR (Revenue at Risk) ist offener Betrag je Kategorie für überfällige Rechnungen.

## 3. Eingaben und Ausgabe-Struktur

### 3.1 Report-Parameter

- `fromDate` (optional, ISO `YYYY-MM-DD`): Untere Grenze für Invoice-Date.
- `toDate` (optional, ISO `YYYY-MM-DD`): Obere Grenze für Invoice-Date.
- `clientId` (optional): Filter auf Client.
- `includePaid` (optional, default `true`): Voll bezahlte Rechnungen einbeziehen oder nur offene.
- `asOfDate` (optional, default `LocalDate.now()`): Stichtag für Overdue-Bewertung.

### 3.2 DataSets

- `RiskByCategory`:
  - `categoryCode`
  - `categoryName`
  - `invoicedAmount`
  - `paidAmount`
  - `openAmount`
  - `overdueOpenAmount` (RaR)
  - `dtcDaysWeighted`
  - `paymentsCount`
  - `invoicesCount`
  - `overpaymentAmount` (optional aus Resten > Invoice-Open)
- `CriticalInvoices`:
  - `invoiceNumber`
  - `clientName`
  - `invoiceDate`
  - `dueDate`
  - `invoiceStatus`
  - `categoryCode`
  - `categoryName`
  - `categoryOpenAmount`
  - `daysOverdue`

## 4. Laufzeit-Algorithmus (Detail)

## 4.1 Phase A: Daten laden

1. Lade alle relevanten Invoices mit Payments, Order, OrderItems und Category-Referenzen in einem Durchlauf (Fetch-Plan so wählen, dass N+1 minimiert wird).
2. Wende Filter an (`fromDate`, `toDate`, `clientId`, optional `includePaid`).
3. Für jede Invoice initialisiere eine technische Arbeitsstruktur:
   - `invoiceAllocatedByCategory: Map<CategoryKey, BigDecimal>`
   - `invoicePaidByCategory: Map<CategoryKey, BigDecimal>`
   - `invoiceRemainingByCategory: Map<CategoryKey, BigDecimal>`
   - `invoiceOverpayment: BigDecimal`

## 4.2 Phase B: Kategorie-Shares je Invoice berechnen

1. Lies `invoice.total`; falls `null`, verwende `0`.
2. Falls `invoice.total <= 0`, Rechnung für Allokation überspringen (nur optional in Debug-Statistik zählen).
3. Bestimme Kategorie-Anteile:
   - Wenn `invoice.order == null`: `UNASSIGNED = 100%`.
   - Sonst: gruppiere `order.items` nach Kategorie:
     - Kategorie-Key: `category.code` (fallback `UNASSIGNED`, wenn Kategorie fehlt).
     - Kategorie-Basiswert: `max(orderItem.total, 0)`.
4. Berechne `sumBasis` aller Kategorien.
5. Falls `sumBasis <= 0`: `UNASSIGNED = 100%`.
6. Sonst: `share(category) = basis(category) / sumBasis`.
7. Rechne `invoiceAllocatedByCategory`:
   - Für alle Kategorien außer letzter: `alloc = round(invoice.total * share, 2)`.
   - Letzte Kategorie erhält Restkorrektur: `invoice.total - sum(previousAlloc)`.
   - Ergebnis ist zentgenau und summiert exakt auf `invoice.total`.
8. Initialisiere `invoiceRemainingByCategory = invoiceAllocatedByCategory`.

## 4.3 Phase C: Payment-Allokation je Invoice

1. Sortiere Payments deterministisch: `payment.date asc (null last), payment.id asc`.
2. Für jedes Payment:
   - `paymentAmount = max(payment.amount, 0)`; bei `null` oder `0`: skip.
   - `invoiceRemainingTotal = sum(invoiceRemainingByCategory)`.
   - Wenn `invoiceRemainingTotal <= 0`: gesamter Betrag als `invoiceOverpayment += paymentAmount` markieren; nächstes Payment.
3. Verteile Payment primär auf aktuelle Remaining-Anteile (nicht auf ursprüngliche Shares), damit Over-Allocation verhindert wird:
   - Für alle Kategorien außer letzter:
     - `raw = paymentAmount * (remainingCategory / invoiceRemainingTotal)`
     - `allocPayment = min(round(raw, 2), remainingCategory)`
   - Letzte Kategorie bekommt den Rest `paymentAmount - sum(previousAllocPayment)`, ebenfalls mit Cap auf Remaining.
4. Falls durch Caps ein nicht verteilbarer Rest bleibt: iterativ auf Kategorien mit Rest-Remaining verteilen.
5. Wenn nach Iteration weiterhin Rest vorhanden: als `invoiceOverpayment` buchen.
6. Update pro Kategorie:
   - `invoicePaidByCategory += allocPayment`
   - `invoiceRemainingByCategory -= allocPayment`

## 4.4 Phase D: DTC-Berechnung (Days To Cash)

1. Für jede allokierte Payment-Komponente `(invoice, category, allocPaymentAmount)`:
   - Wenn `allocPaymentAmount <= 0`: ignore.
   - Wenn `invoice.date == null`: nicht in DTC einfließen lassen.
   - Wenn `payment.date == null`: Betrag zählt als bezahlt, aber nicht in DTC-Zähler/Nenner.
   - Sonst: `days = ChronoUnit.DAYS.between(invoice.date, payment.date)`.
   - Negative Werte auf `0` clampen.
2. Kategorie-Akkumulatoren:
   - `dtcNumerator += allocPaymentAmount * days`
   - `dtcDenominator += allocPaymentAmount`
3. Finale Kennzahl:
   - `dtcDaysWeighted = dtcNumerator / dtcDenominator` (wenn `dtcDenominator > 0`, sonst `null`).

## 4.5 Phase E: Open Amount und Revenue at Risk (RaR)

1. `openByCategory = max(invoiceAllocatedByCategory - invoicePaidByCategory, 0)`.
2. Overdue-Regel je Invoice:
   - Overdue wenn `invoice.status == OVERDUE`, oder
   - `invoice.dueDate != null && invoice.dueDate.isBefore(asOfDate) && invoiceOpenTotal > 0`.
3. Wenn overdue:
   - `rarByCategory += openByCategory`.

## 4.6 Phase F: Aggregation pro Kategorie

1. Für jede Kategorie über alle Invoices aggregieren:
   - `invoicedAmount += allocated`
   - `paidAmount += paid`
   - `openAmount += open`
   - `overdueOpenAmount += rar`
   - `overpaymentAmount += invoiceOverpayment` (optional auf `UNASSIGNED`/`OVERPAYMENT` Bucket).
2. Konsistenzprüfungen:
   - `invoicedAmount = paidAmount + openAmount` (bis auf Rundungscent-Differenz).
3. Sortierung für Ausgabe:
   - Primär `overdueOpenAmount desc`
   - Sekundär `dtcDaysWeighted desc`

## 5. Explizite Edge-Case-Regeln

- `invoice.order == null`: komplette Rechnung nach `UNASSIGNED`.
- `order.items leer` oder Basis-Summe `<= 0`: `UNASSIGNED`.
- `payment.date == null`: zählt zu `paidAmount`, nicht zu DTC.
- `invoice.date == null`: Payment zählt zu `paidAmount`, nicht zu DTC.
- `payment.amount > invoice.open`: Überschuss in `overpaymentAmount`.
- `invoice.total == 0`: nicht allokieren, aber optional als technische Anomalie zählen.
- Fehlende Kategorie (`categoryItem/category`) wird als `UNASSIGNED` gemappt.

## 6. Implementierungsschritte

1. `CashflowAnalyticsService` mit oben definierten Phasen A-F implementieren.
2. Interne DTOs für Runtime-Berechnung erstellen (nur Service-intern, keine Entity-Änderung).
3. `CategoryCashflowDataLoader` bauen, der `RiskByCategory` und `CriticalInvoices` aus Service-Ergebnis liefert.
4. HTML-Template auf tabellarische, LLM-lesbare Ausgabe fokussieren:
   - Primär: strukturierte Tabellen mit Zahlen.
   - Sekundär: Visuals (Heatmap/Waterfall) für UI.
5. `InvoiceListView` Button "Cashflow-Analyse" anbinden und Report-Parameter setzen.
6. Report-Whitelisting für AI-Flow ergänzen (`CrmAnalyticsService`), damit E2E-LLM-Tests den Report verwenden können.

## 7. Testmatrix (Verifikation)

Alle fachlichen Kernfälle als Integrationstests auf Service-Ebene (`CashflowAnalyticsService`) plus ausgewählte E2E-LLM-Tests.

### 7.1 Service-Integrationstests (Pflicht)

- `testSingleInvoice_twoCategories_partialPayment`
  - Testdaten: 1 Invoice total `1000.00`, Order mit 2 Kategorien je `500.00`, 1 Payment `500.00` nach 10 Tagen.
  - Assertions: `paidAmount` je Kategorie `250.00`, `openAmount` je Kategorie `250.00`, DTC je Kategorie `10.0`.

- `testSingleInvoice_twoPayments_weightedDtc`
  - Testdaten: gleiche Invoice, Payment1 `300.00` nach 5 Tagen, Payment2 `700.00` nach 20 Tagen.
  - Assertions: Summe paid `1000.00`, open `0`, gewichteter DTC korrekt nach Betrag.

- `testMultipleInvoices_sameOrder_allocationPerInvoice`
  - Testdaten: 2 Invoices derselben Order (`400.00` und `600.00`), mehrere Payments.
  - Assertions: Allokation erfolgt je Invoice separat; Summen pro Kategorie stimmen über beide Invoices.

- `testInvoiceWithoutOrder_goesToUnassigned`
  - Testdaten: Invoice mit `order=null`, total `300.00`, Payment `100.00`.
  - Assertions: Nur Kategorie `UNASSIGNED` enthält `invoiced=300`, `paid=100`, `open=200`.

- `testMissingCategoryMappedToUnassigned`
  - Testdaten: OrderItems ohne Category/CategoryItem.
  - Assertions: komplette Allokation in `UNASSIGNED`.

- `testPaymentDateNull_excludedFromDtc`
  - Testdaten: Payment mit `date=null`, gültige Invoice-Date.
  - Assertions: `paidAmount` erhöht sich, DTC-Numerator/Denominator bleiben unverändert.

- `testInvoiceDateNull_excludedFromDtc`
  - Testdaten: Invoice mit `date=null`, Payment mit Datum.
  - Assertions: `paidAmount` erhöht sich, DTC bleibt `null`/unverändert.

- `testOverpayment_trackedSeparately`
  - Testdaten: Invoice total `1000.00`, Payments gesamt `1200.00`.
  - Assertions: `paidAmount` max `1000.00`, `openAmount=0`, `overpaymentAmount=200.00`.

- `testOverdueRar_byStatus`
  - Testdaten: offene Invoice mit Status `OVERDUE`.
  - Assertions: offener Kategorie-Anteil wird vollständig in `overdueOpenAmount` gezählt.

- `testOverdueRar_byDueDateFallback`
  - Testdaten: Status nicht `OVERDUE`, aber `dueDate < asOfDate`, offene Beträge vorhanden.
  - Assertions: Betrag fließt in `overdueOpenAmount`.

- `testZeroTotalInvoice_ignoredForAllocation`
  - Testdaten: Invoice total `0.00`, Payments optional `0.00`.
  - Assertions: keine Kategorie-Summen verändert.

- `testRoundingRestCorrection_centExact`
  - Testdaten: Invoice `100.00`, 3 Kategorien mit Anteilen, die Rundungsreste erzeugen.
  - Assertions: Summe allokierter Kategorie-Beträge exakt `100.00`.

- `testNegativeDayDiff_clampedToZero`
  - Testdaten: Payment-Date vor Invoice-Date.
  - Assertions: DTC-Tagesanteil wird als `0` behandelt, keine negativen DTC-Werte.

### 7.2 DataLoader-/Report-Integrationstests

- `testRiskByCategory_datasetContainsExpectedColumns`
  - Testdaten: kleiner Seed mit 2 Kategorien.
  - Assertions: alle definierten Felder in `RiskByCategory` befüllt.

- `testCriticalInvoices_top10ByOverdueOpenAmount`
  - Testdaten: >10 overdue Invoices.
  - Assertions: genau 10 Einträge, korrekt nach Risiko sortiert.

### 7.3 AI / E2E-Tests

- `testRunReportTool_categoryCashflowRiskReport`
  - Testdaten: Seed mit klarer Risikokategorie.
  - Assertions: Tool-Result `success=true`, HTML enthält tabellarische Risiko-Kennzahlen.

- `testCrmAnalyticsService_cashflowRiskQuestion`
  - Frage: "Welche Kategorie hat aktuell das höchste Cashflow-Risiko und warum?"
  - Assertions: Antwort nennt Top-Kategorie, referenziert DTC/RaR-Werte aus Reportinhalt.

## 8. Bekannte Limitation (bewusst akzeptiert für Demo)

Da keine Snapshot-Persistenz genutzt wird, basiert die Kategorie-Split-Logik immer auf dem aktuellen Zustand der `OrderItems`. Historische Änderungen an Orders können damit rückwirkend alte Rechnungs-Allokationen beeinflussen. Für die Demo ist das akzeptiert; bei produktivem Einsatz wäre ein Invoice-Snapshot-Modell vorzusehen.
