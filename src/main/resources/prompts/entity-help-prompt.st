CRM Database Entities - Complete JPQL Specification:

IMPORTANT: This is JPQL (Java Persistence Query Language), NOT SQL!
- Use entity names (not table names)
- Use attribute names (not column names)
- Use entity relationships for joins (not foreign key columns)
- No SELECT * allowed - specify exact attributes

CRITICAL: Always use AS aliases for ALL SELECT fields!
- Every field in SELECT clause MUST have an explicit AS alias
- Aliases should be meaningful and descriptive (e.g., clientName, orderCount, totalRevenue)
- This ensures consistent and predictable query results
- Example: SELECT c.name AS clientName, COUNT(o) AS orderCount, SUM(o.total) AS totalRevenue FROM Client c LEFT JOIN c.orders o GROUP BY c

All entities inherit audit attributes: id, createdBy, createdDate, updatedBy, updatedDate, version, deletedBy, deletedDate

1. Client - Customer records (Entity: Client)
   Attributes:
   - name (String) - Primary client name
   - fullName (String) - Full company name
   - type (ClientType enum) - BUSINESS, INDIVIDUAL (stored as String)
   - vatNumber (String) - VAT registration number
   - regNumber (String) - Company registration number
   - website (String) - Company website URL
   - address.country, address.city, address.street, address.building, address.apartment, address.postalCode (embedded Address)

   Relationships for JPQL:
   - accountManager (User entity)
   - orders (List<Order_> entity)
   - invoices (List<Invoice> entity)
   - contacts (List<Contact> entity)
   - payments (computed from invoices.payments)

2. Order_ - Sales orders (Entity: Order_)
   IMPORTANT: Entity name is "Order_" due to SQL keyword conflict!

   Attributes:
   - number (String) - Auto-generated order number
   - date (LocalDate) - Order date
   - quote (String) - Quote reference
   - comment (String) - Order comments
   - total (BigDecimal) - Total order amount
   - discountValue (BigDecimal) - Absolute discount amount
   - discountPercent (BigDecimal) - Percentage discount
   - status (OrderStatus enum) - NEW=10, ACCEPTED=20, IN_PROGRESS=30, DONE=40 (stored as Integer)

   Relationships for JPQL:
   - client (Client entity)
   - orderItems (List<OrderItem> entity)

   Computed Properties:
   - itemsTotal (BigDecimal) - Sum of all orderItems.total

3. OrderItem - Line items in orders (Entity: OrderItem)
   Attributes:
   - quantity (BigDecimal) - Item quantity
   - discount (BigDecimal) - Line item discount
   - netPrice (BigDecimal) - Net price per unit
   - grossPrice (BigDecimal) - Gross price per unit
   - vatAmount (BigDecimal) - VAT amount
   - vatIncluded (Boolean) - Whether VAT is included

   Relationships for JPQL:
   - order (Order_ entity)
   - categoryItem (CategoryItem entity)

   Computed Properties:
   - total (BigDecimal) - netPrice * quantity - discount

4. Category - Product categories (Entity: Category)
   Attributes:
   - name (String) - Category name
   - code (String) - Category code
   - description (String) - Category description

   Relationships for JPQL:
   - parent (Category entity) - self-referencing for hierarchy
   - categoryItems (List<CategoryItem> - implicit reverse relationship)

5. CategoryItem - Products/items (Entity: CategoryItem)
   Attributes:
   - name (String) - Item name
   - code (String) - Item code
   - description (String) - Item description
   - uom (String) - Unit of measure
   - image (FileRef) - Product image file reference

   Relationships for JPQL:
   - category (Category entity)
   - comments (List<CategoryItemComment> entity)

6. Invoice - Billing documents (Entity: Invoice)
   Attributes:
   - date (LocalDate) - Invoice date
   - dueDate (LocalDate) - Payment due date
   - subtotal (BigDecimal) - Subtotal amount
   - vat (BigDecimal) - VAT amount
   - total (BigDecimal) - Total invoice amount
   - status (InvoiceStatus enum) - NEW=10, PENDING=20, OVERDUE=30, PAID=40 (stored as Integer)

   Relationships for JPQL:
   - client (Client entity)
   - order (Order_ entity) - optional
   - payments (List<Payment> entity)

7. Payment - Payment records (Entity: Payment)
   Attributes:
   - date (LocalDate) - Payment date
   - amount (BigDecimal) - Payment amount

   Relationships for JPQL:
   - invoice (Invoice entity)

   Computed Properties:
   - order (derived from invoice.order)
   - client (derived from invoice.client)

8. User - System users and account managers (Entity: User)
   Attributes:
   - username (String) - Login username
   - firstName (String) - First name
   - lastName (String) - Last name
   - email (String) - Email address
   - active (Boolean) - User active status
   - timeZoneId (String) - User timezone

   Relationships for JPQL:
   - tasks (List<UserTask> entity)

   Computed Properties:
   - fullName (String) - firstName + lastName
   - displayName (String) - fullName [username]

9. Contact - Client contacts (Entity: Contact)
   Attributes:
   - person (String) - Contact person name
   - position (String) - Job position
   - startDate (LocalDate) - Start date
   - endDate (LocalDate) - End date
   - phone (String) - Phone number
   - email (String) - Email address

   Relationships for JPQL:
   - client (Client entity)

JPQL Query Examples with Mandatory AS Aliases:
- Client analysis: "SELECT c.name AS clientName, c.fullName AS fullName, c.type AS clientType, c.vatNumber AS vatNumber, COUNT(o) AS orderCount, SUM(o.total) AS totalRevenue, AVG(o.total) AS avgOrderValue FROM Client c LEFT JOIN c.orders o GROUP BY c"
- Order trends: "SELECT o.date AS orderDate, o.status AS orderStatus, o.total AS orderTotal, c.name AS clientName FROM Order_ o JOIN o.client c WHERE o.date >= :startDate ORDER BY o.date DESC"
- Invoice payment status: "SELECT i.total AS invoiceTotal, SUM(p.amount) AS totalPaid, (i.total - COALESCE(SUM(p.amount), 0)) AS remainingBalance FROM Invoice i LEFT JOIN i.payments p GROUP BY i"
- Category performance: "SELECT cat.name AS categoryName, COUNT(ci) AS itemCount, SUM(oi.quantity) AS totalQuantitySold, SUM(oi.total) AS totalRevenue FROM Category cat LEFT JOIN cat.categoryItems ci LEFT JOIN OrderItem oi ON oi.categoryItem = ci GROUP BY cat"
- Account manager performance: "SELECT u.firstName AS managerFirstName, u.lastName AS managerLastName, COUNT(c) AS clientCount, SUM(o.total) AS totalRevenue FROM User u LEFT JOIN Client c ON c.accountManager = u LEFT JOIN c.orders o GROUP BY u"
- Product analysis: "SELECT ci.name AS productName, ci.uom AS unitOfMeasure, COUNT(oi) AS orderCount, SUM(oi.quantity) AS totalQuantitySold FROM CategoryItem ci LEFT JOIN OrderItem oi ON oi.categoryItem = ci GROUP BY ci"

JPQL Rules:
- Always use entity names: Client, Order_, OrderItem, Category, CategoryItem, Invoice, Payment, User, Contact
- Always use attribute names: name, total, date, etc. (not column names)
- Use relationships for joins: c.orders, i.payments, etc.
- No SELECT * - specify exact attributes
- Use entity references in GROUP BY: "GROUP BY c" not "GROUP BY c.id"
- Enum values stored as their underlying type (Integer for status enums, String for ClientType)

JMIX JPQL EXTENSIONS (Additional Functions Available):

String Functions:
- CONCAT(string1, string2, ...) - Concatenate strings
- SUBSTRING(string, start, length) - Extract substring
- LOCATE(substring, string) - Find position of substring
- LENGTH(string) - String length
- LOWER(string) - Convert to lowercase
- UPPER(string) - Convert to uppercase
- TRIM([LEADING|TRAILING|BOTH] [character] FROM string) - Trim whitespace/characters

Date/Time Functions:
- YEAR(date) - Extract year from date
- MONTH(date) - Extract month from date (1-12)
- DAY(date) - Extract day from date
- HOUR(datetime) - Extract hour from datetime
- MINUTE(datetime) - Extract minute from datetime
- SECOND(datetime) - Extract second from datetime
- CURRENT_DATE - Current date
- CURRENT_TIME - Current time
- CURRENT_TIMESTAMP - Current date and time

Numeric Functions:
- ABS(number) - Absolute value
- SQRT(number) - Square root
- MOD(number, divisor) - Modulo operation
- SIZE(collection) - Size of collection

Aggregate Functions:
- COUNT(expression|entity) - Count records (use COUNT(entity) not COUNT(*))
- SUM(expression) - Sum numeric values
- AVG(expression) - Average of numeric values
- MAX(expression) - Maximum value
- MIN(expression) - Minimum value

Conditional Expressions:
- CASE WHEN condition THEN value [WHEN condition THEN value] [ELSE value] END
- COALESCE(expression1, expression2, ...) - Return first non-null value
- NULLIF(expression1, expression2) - Return null if expressions are equal

Collection Operations:
- IS EMPTY / IS NOT EMPTY - Check if collection is empty
- MEMBER OF / NOT MEMBER OF - Check collection membership
- SIZE(collection) - Get collection size

Soft Delete Support (Jmix Extension):
- All queries automatically exclude soft-deleted records (deletedDate IS NULL)
- To include deleted records: Use native queries or special repository methods

JPQL LIMITATIONS AND RESTRICTIONS (What is NOT allowed):

Forbidden Functions:
- COUNT(*) - Use COUNT(entity) instead: "SELECT COUNT(c) FROM Client c" not "SELECT COUNT(*) FROM Client c"
- SELECT * - Must specify exact attributes: "SELECT c.name, c.total" not "SELECT *"
- Database-specific functions (MySQL LIMIT, PostgreSQL OFFSET, etc.)
- Subqueries in SELECT clause (not supported by EclipseLink)
- Window functions (ROW_NUMBER(), RANK(), etc.)
- Common Table Expressions (WITH clause)

Alias Restrictions:
- No aliases in SELECT for security reasons in Jmix queries via AI tools
- Use direct attribute references: "SELECT c.name, c.total FROM Client c"
- NOT: "SELECT c.name as clientName, c.total as totalAmount FROM Client c"

Join Limitations:
- No explicit table joins with ON clauses using foreign key columns
- Use entity relationships instead: "JOIN c.orders o" not "JOIN ORDER_ o ON o.CLIENT_ID = c.ID"
- No CROSS JOIN or complex join syntax

Data Modification Restrictions:
- No INSERT, UPDATE, DELETE statements in queries
- Jmix queries are read-only for security
- Use repository methods or EntityManager for modifications

Pagination Limitations:
- No LIMIT/OFFSET clauses
- Use Jmix pagination mechanisms instead
- No TOP, FIRST, or database-specific limiting functions

Complex Expression Limitations:
- No user-defined functions
- No stored procedure calls
- No recursive queries or hierarchical queries (CONNECT BY, etc.)
- Limited regular expression support

Type Conversion Limitations:
- No explicit CAST functions in many cases
- Rely on JPA type conversion
- No database-specific type functions

Security Restrictions:
- No direct database metadata access
- No system functions or administrative queries
- All queries subject to Jmix entity-level security

Example Queries with Jmix Extensions (All with AS Aliases):
- Date filtering: "SELECT o.number AS orderNumber, o.total AS orderTotal FROM Order_ o WHERE YEAR(o.date) = 2024"
- Month grouping: "SELECT YEAR(o.date) AS orderYear, MONTH(o.date) AS orderMonth, COUNT(o) AS orderCount, SUM(o.total) AS monthlyRevenue FROM Order_ o GROUP BY YEAR(o.date), MONTH(o.date)"
- String operations: "SELECT c.name AS clientName, c.fullName AS fullName FROM Client c WHERE UPPER(c.name) LIKE UPPER(:searchTerm)"
- Case expressions: "SELECT o.number AS orderNumber, CASE WHEN o.status = 40 THEN 'COMPLETED' WHEN o.status = 10 THEN 'NEW' ELSE 'IN_PROGRESS' END AS statusLabel FROM Order_ o"
- Collection size: "SELECT c.name AS clientName, SIZE(c.orders) AS orderCount FROM Client c"
- Null handling: "SELECT c.name AS clientName, COALESCE(c.fullName, c.name) AS displayName FROM Client c"
- Date range: "SELECT COUNT(o) AS orderCount FROM Order_ o WHERE o.date BETWEEN :startDate AND :endDate"
- Complex aggregation: "SELECT c.name AS clientName, COUNT(o) AS orderCount, AVG(o.total) AS avgOrderValue, MIN(o.date) AS firstOrderDate, MAX(o.date) AS lastOrderDate FROM Client c LEFT JOIN c.orders o GROUP BY c HAVING COUNT(o) > 5"

CORRECT vs INCORRECT Examples:
✓ CORRECT: "SELECT COUNT(c) AS clientCount FROM Client c"
✗ INCORRECT: "SELECT COUNT(*) FROM Client c"

✓ CORRECT: "SELECT c.name AS clientName, c.total AS clientTotal FROM Client c"
✗ INCORRECT: "SELECT * FROM Client c"

✓ CORRECT: "SELECT c.name AS clientName FROM Client c JOIN c.orders o WHERE o.total > 1000"
✗ INCORRECT: "SELECT c FROM Client c JOIN ORDER_ o ON o.CLIENT_ID = c.ID WHERE o.total > 1000"

✓ CORRECT: "SELECT c.name AS clientName, SIZE(c.orders) AS orderCount FROM Client c"
✗ INCORRECT: "SELECT c.name, (SELECT COUNT(*) FROM Order_ o WHERE o.client = c) FROM Client c"

✓ CORRECT WITH ALIASES: "SELECT c.name AS clientName, COUNT(o) AS orderCount FROM Client c LEFT JOIN c.orders o GROUP BY c"
✗ INCORRECT WITHOUT ALIASES: "SELECT c.name, COUNT(o) FROM Client c LEFT JOIN c.orders o GROUP BY c"