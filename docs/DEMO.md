# Live demonstration and recording plan

## Opening script (about two minutes at a conversational pace)

Good morning. My project is Student Expense Tracker, a Java desktop application for college students. Imagine that I receive my allowance on the first day of the month. A week later, I can remember paying for a bus pass and a textbook, but I am less sure how much I spent on meals. My bank balance only tells part of the story, especially if I paid some expenses in cash.

I built a ledger where I can enter both income and expenses. For each entry I choose a category, enter an amount and date, and optionally write a short description. If I make a mistake, I can find the entry and correct it. The dashboard answers two separate questions. It shows how much money remains across the whole ledger and how much I spent during the month I selected.

The feature I will demonstrate in detail is the monthly budget. If I set aside 150 rupees and spend 120 rupees and 50 paise, the app shows that I have 29 rupees and 50 paise left. It also tells me that I have reached the 80 percent warning level. Adding income changes my balance, but my spending limit still measures what I spent.

The interface is built in Java Swing. SQLite saves the records in a local file, so the application works offline after installation. I will first enter two example transactions and check the figures. Then I will edit one entry, compare the reports and export the ledger.

## Walkthrough with a fresh database

Use a new filename for each recording. From the source folder, run `java "-Dexpense.tracker.db=student-demo-new.db" -jar target/student-expense-tracker.jar`. In the runnable ZIP folder, omit `target/`. For a prefilled tour, open `Run-Demo.bat`.

| Step | Action | What the audience should see |
| --- | --- | --- |
| 1 | Open Dashboard and select the current month. | An empty ledger has zero totals. |
| 2 | Add Income, Pocket Money, 5000, today's date, Allowance. | Total income and balance both rise to INR 5,000.00. |
| 3 | Add Expense, Food, 120.50, today's date, Canteen lunch. | Expenses reach INR 120.50. Balance is INR 4,879.50. |
| 4 | Open Budgets and save a 150 limit. | Remaining INR 29.50, with the 80% warning. |
| 5 | In Expenses search for canteen. Edit the amount to 151. | Budget exceeded by INR 1.00. Balance is INR 4,849.00. |
| 6 | In Reports show Daily, Weekly and By category. | One purchase of INR 151.00 appears in each relevant grouping. |
| 7 | Choose Export all to CSV, save to a new file and open it. | Two rows plus the header, with separate income and expense amounts. |
| 8 | Reopen the same database file. | Both entries and the budget are still available. |

**Closing.** “The ledger tells me what I entered, the reports explain where the money went, and the budget gives me an early warning. My next improvement would be a preview that helps students import existing CSV records safely.”

## Video and college submission

Record the application window with the Windows screen recorder or OBS. Show only fictional records. Capture the budget change before switching to reports. Play back the video to check the amounts and narration. Upload the recording to Drive or YouTube and use its viewer link if the evaluator accepts a desktop demonstration video.

For a Drive submission, upload the editable PPTX and report PDF. Open Share, select General access, Anyone with the link and Viewer, then copy each link. Open those links in a signed-out window before submitting. If an institutional policy restricts sharing, invite the evaluator's college account.
