# Verification results

The Windows build used Azul Zulu JDK 17, Apache Maven 3.9.9, SQLite JDBC 3.46.1.0 and JUnit Jupiter 5.11.0. Maven ran clean verify with a local dependency cache. The test temporary directory was set to a writable project workspace. All test databases used fictional data.

| Check | Actual result |
| --- | --- |
| FinanceIntegrationTest | 20 run, 0 failures, 0 errors, 0 skipped |
| BudgetAndReportTest | 7 run, 0 failures, 0 errors, 0 skipped |
| Maven package | BUILD SUCCESS, shaded JAR with App as its main class |
| Packaged database probe | DATABASE_OK records=12 using demo mode |
| Independent process restart | WRITE_OK followed by RESTART_READ_OK |
| DesktopSmoke | Packaged GUI opened. Budget edit produced an exceeded warning. Search found the grocery expense. Weekly reports displayed data. The dashboard display preference took effect. |
| UiCapture | Rendered six actual Swing screens against a fresh synthetic database. |

The JUnit suite exercises CRUD, validation, type and date filters, reporting boundaries, exact paise totals, budget persistence, CSV escaping and legacy migration rollback. The screen check uses the real controls programmatically. Transaction editing and CSV file output are covered at the service and file layers. A person can follow the separate demo script to exercise the native file chooser and confirmation dialogs.

## Reproduce on Windows

```powershell
mvn clean verify
java -jar target/student-expense-tracker.jar --demo --check-db
$cp = "target/student-expense-tracker.jar;target/test-classes"
java -cp $cp com.studentfinance.DesktopSmoke
java -cp $cp com.studentfinance.UiCapture docs/screenshots
$db = Join-Path $env:TEMP ("tracker-probe-" + [guid]::NewGuid() + ".db")
java -cp $cp com.studentfinance.PersistenceProbe write $db
java -cp $cp com.studentfinance.PersistenceProbe read $db
```

Maven writes detailed JUnit results to target/surefire-reports. The runnable distribution includes the two plain-text reports from the final build.
