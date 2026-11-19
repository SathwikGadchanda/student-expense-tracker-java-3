# Student Expense Tracker | 30 viva questions

## A. Basic Java

1. **What is Java?** Java is a statically typed language compiled to bytecode. Here the Java source becomes classes inside the runnable JAR.
2. **What is the JVM?** The Java Virtual Machine runs bytecode for the host system. Running the JAR starts a JVM that executes App.main.
3. **What is the JDK?** The Java Development Kit includes the compiler and tools for building programs. Maven uses the JDK to compile MainFrame and the other classes.
4. **What is the JRE?** The Java Runtime Environment means the JVM and libraries needed to run a program. A Java 17 installation with desktop support can run the packaged tracker.
5. **What are classes and objects?** A class defines fields and behavior. An object is one instance. Transaction is a class, and a particular INR 120.50 lunch is one Transaction object.

## B. Object-oriented programming

6. **What is encapsulation?** A class controls access to its data. Transaction keeps its fields private and validates its amount in the constructor.
7. **What is inheritance?** A subclass reuses and specializes a parent class. CategoryChartPanel extends JPanel and adds category chart drawing.
8. **What is polymorphism?** The same method call can select behavior for the actual object. Swing calls getValueAt on the tracker's TransactionTableModel when it displays each cell.
9. **What is abstraction?** An abstraction exposes operations while hiding implementation details. FinanceService can request all transactions without knowing the SQLite query.
10. **What is an interface?** An interface defines a contract. TransactionDao declares findAll, insert, update and delete, and SqliteTransactionDao implements them.

## C. Database

11. **What is SQLite?** SQLite is an embedded relational database engine. The tracker stores its tables in expenses.db on the student's computer.
12. **What is JDBC?** Java Database Connectivity is Java's standard database API. Database.connect obtains a JDBC Connection for the SQLite file.
13. **What is SQL?** Structured Query Language describes operations on relational tables. The ledger uses SELECT to read entries and UPDATE to save corrections.
14. **What is a primary key?** A primary key uniquely identifies a row. Transactions use a numeric ID. Budgets use the month, so one month can have only one limit.
15. **What is CRUD?** Create, read, update and delete are the basic data operations. Adding a lunch, opening history, correcting its price and deleting a duplicate demonstrate all four.

## D. Project implementation

16. **How does the application work?** A Swing form constructs a valid Transaction. The service delegates storage to the DAO and the dashboard refreshes from the resulting records.
17. **How is income calculated?** The service adds the amounts of records whose type is INCOME. Monthly income includes only the selected year and month.
18. **How are expenses stored?** Each expense is one row with its ID, type, category, date, description and integer amount in paise. INR 120.50 is stored as 12050.
19. **How does the dashboard work?** It summarizes the currently loaded ledger. All-time income minus expenses gives balance. The selected month controls spending, category totals and the budget warning.
20. **How does CSV export work?** CsvExporter writes one UTF-8 header and one row per transaction. It escapes quotes and commas, then moves the finished temporary file to the chosen location.

## E. Technical questions

21. **Why use Java Swing?** Swing provides window, form and table components in the Java desktop library. JTable supports sortable history, while JPanel supports the custom chart.
22. **Why use SQLite?** The app needs a small, durable database on one device. SQLite creates a file at first launch and needs no separate database server.
23. **What is Maven?** Maven reads pom.xml to resolve dependencies and run build steps. The verify command compiles, tests and packages this app with its SQLite driver.
24. **What is exception handling?** It lets code respond when an operation fails. The transaction dialog catches an invalid calendar date and gives the student a correction message.
25. **What is a PreparedStatement?** It is a parameterized SQL statement. The DAO binds the description as data so a quote in a student's note does not change the query.
26. **What is database persistence?** Saved records survive the end of a process. The restart check writes a transaction in one JVM and reads it back in a new JVM.
27. **What is the purpose of a DAO?** A data access object concentrates the queries and connection handling. BudgetDao performs the insert-or-update for a monthly limit.
28. **What is the purpose of the service layer?** It owns application rules that can be tested without clicking the interface. FinanceService filters records and calculates the monthly summary.
29. **How did you test the application?** Twenty-seven JUnit tests cover the original ledger and the new budget, reporting and migration rules. A second check starts the packaged Swing app and exercises controls on a temporary demo database.
30. **What improvements could be added?** Recurring expenses could reduce repeated entry. A CSV import preview could map columns and let students correct invalid rows before saving.

See [the class-by-class guide](DESIGN.md) for the roles of the application entry point, models, database, DAOs, service, interface and utilities.
