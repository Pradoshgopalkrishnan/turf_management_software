# Project Change and Issue Summary

This file records diagnoses, fixes, features, and configuration changes made
in the project. Add a dated entry after each future change or issue diagnosis,
including affected files and how the result was validated.

## 2026-10-17

### MySQL JDBC driver not found when running `Testconnection`

- **Issue:** Running `db.Testconnection` reported `No suitable driver found for
  jdbc:mysql://localhost:3306/turf` (an earlier attempt used the `turf_db`
  database name).
- **Diagnosis:** The Java command's runtime classpath contained the compiled
  classes directory but not the MySQL Connector/J JAR. The JAR was present in
  `lib`, and `.vscode/settings.json` already referenced JARs under `lib`.
- **Verification:** Running with both the compiled classes directory and
  `lib/mysql-connector-j-26.7.0.jar` on the classpath connected successfully
  and read the two seeded turf records.
- **Change:** Added `.vscode/launch.json` with a `Run Testconnection`
  configuration for `db.Testconnection`, including the MySQL connector JAR.
- **Usage:** Open `src/db/Testconnection.java` and click the Java **Run**
  action above `main`, or select **Run Testconnection** from Run and Debug.
- **Validation note:** The explicit-classpath run confirmed the driver and
  database connection work. The Java debugger also started the test class.
  The saved launch configuration itself was not separately exercised through
  the editor's Run button.

### `ModelTest.java` launch fails

- **Issue:** The `Run ModelTest` launch configuration targets `model.ModelTest`,
  and `src/model/ModelTest.java` does define that class and a `main` method.
- **Diagnosis:** The actual compile blocker is `src/model/Promocode.java`:
  it declares `public class PromoCode`, and Java requires a public class's
  filename to match exactly. The required name is `PromoCode.java`, not
  `Promocode.java`.
- **Validation:** Compiling every Java file in `src/model` with `javac`
  failed with `class PromoCode is public, should be declared in a file named
  PromoCode.java`. The launch configuration's `mainClass` is correct. No
  Java source files were changed during diagnosis.
- **Correction to earlier diagnosis:** The previous report that
  `ModelTest.java` declared `Booking` and lacked `main` was incorrect; the
  file's actual contents define a runnable `ModelTest`. The earlier debugger
  response saying it started the class did not validate a full successful
  project compilation.

### Clarifying the Java "non-project file" message

- **Meaning:** VS Code's Java language server is not associating
  `Booking.java` with an imported Java project. It therefore limits reporting
  for that file to syntax errors and may not provide project-aware type,
  dependency, or build diagnostics for it.
- **Correction:** This status does not establish that `Booking.java` is
  compiled as part of the project, so it must not be used alone to claim a
  duplicate-class build failure.

### `model.ModelTest` still reports `ClassNotFoundException`

- **Issue:** The manual Java command uses the Java Language Server output
  directory as its classpath, but reports `Could not find or load main class
  model.ModelTest`.
- **Diagnosis:** At inspection time, that output directory did not contain a
  `model` class output directory. The Java source files now have matching
  public class names, and `ModelTest.java` defines `main`; the earlier
  `Promocode.java`/`PromoCode` filename mismatch has been corrected in the
  workspace. A fresh compilation succeeds, so the remaining error in the
  pasted command is stale/missing compiled output in the Java Language Server
  workspace classpath, not a missing `main` method.
- **Validation:** Compiled all files in `src/model` to a temporary output
  directory and ran `model.ModelTest` successfully, including its polymorphism,
  pricing, and promo checks. The Java debugger also accepted a launch of
  `model.ModelTest` after the source filename was corrected.
- **Next action:** In VS Code run **Java: Clean Java Language Server
  Workspace** from the Command Palette and allow the Java project to reload,
  then use the Java **Run** action. Avoid manually launching against the
  `jdt_ws` output directory until it has regenerated the class files.

### Java project now runs successfully

### Added architecture diagram

- **Change:** Created `architecture.md` containing a Mermaid flowchart that visualises the workspace layout.
- **Files involved:** `architecture.md`.
- **Validation:** The file was created successfully and contains a valid Mermaid diagram that can be rendered by VS Code or any Mermaid viewer.


- **Status:** The Java debugging and database connectivity issues are resolved.
- **What fixed it:** The real blockers were (1) the public class/file naming
  mismatch (`Promocode.java` versus `PromoCode`), and (2) missing MySQL JDBC
  access on the classpath for the database test program. Once the class name
  matched the file name and the JDBC JAR was included in the runtime classpath,
  the project compiled and ran correctly.
- **Files involved:** `src/model/PromoCode.java`, `src/model/ModelTest.java`,
  `src/db/Testconnection.java`, and the Java launch setup under `.vscode/`.
- **Validation:** Confirmed with a full compile and runtime check:
  - `javac -d bin -cp "lib/mysql-connector-j-26.7.0.jar" $(find src -name '*.java')`
    completed successfully.
  - `java -cp "bin;lib/mysql-connector-j-26.7.0.jar" model.ModelTest`
    printed the expected polymorphism, pricing, and promo output.
  - `java -cp "bin;lib/mysql-connector-j-26.7.0.jar" db.Testconnection`
    connected successfully and printed the seeded turf records.
- **Result:** Both the Java model test and the MySQL connection test now work
  end-to-end in the project environment.

 

## 2026-10-19

### Added MySQL connector JAR to launch.json classpath

- **Issue:** The `Run Testconnection` launch configuration in `.vscode/launch.json` had no `classpath` entry, so the MySQL Connector/J JAR was not on the runtime classpath when debugging.
- **Change:** Added `"classpath": ["${workspaceFolder}/lib/mysql-connector-j-26.7.0.jar", "${workspaceFolder}/bin"]` to the `Run Testconnection` configuration.
- **Files involved:** `.vscode/launch.json`.
- **Validation:** The launch configuration now includes the JDBC driver JAR and the compiled classes directory, so `db.Testconnection` will find the driver at runtime.

### Fixed README case inconsistency for Testconnection

- **Issue:** README.md referenced `TestConnection.java` (capital C) in three places, but the actual file is `Testconnection.java` (lowercase c).
- **Change:** Updated all three references in README.md:
  1. Project structure tree: `TestConnection.java` → `Testconnection.java`
  2. Step 6 instruction: `TestConnection.java` → `Testconnection.java`
  3. Terminal command: `db.TestConnection` → `db.Testconnection`
- **Files involved:** `README.md`.
- **Validation:** README now matches the actual filename on disk.
 **PHASE 4** NEXT IMPLEMENTS LOGIN FEATURES
## 2026-10-20

### Build fails: `validateUser`, `usernameExists`, `registerUser` not found

- **Issue:** `LoginFrame` and `RegisterFrame` (Phase 4) call three methods that
  did not exist in `DatabaseManager`, which still only had `connect()`.
- **Change:** Added `validateUser(String, String)` (returns `Customer`, `Admin`
  or `null`), `usernameExists(String)` and `registerUser(String, String, String,
  String)` to `src/db/DatabaseManager.java`, all with `PreparedStatement` and
  try-with-resources.
- **Change:** Moved `Main.java` from the project root into `src/`. The VS Code
  setting `java.project.sourcePaths` is `["src"]`, so a root-level `Main.java`
  was outside the project (the "non-project file" state), was never compiled
  into `bin/`, and made `Run Main` fail with class-not-found.
- **Validation:** `javac -Xlint:all -cp "lib/*"` over every file in `src/`
  finishes with 0 errors in about 1.3 s. The SQL itself was not run against a
  live MySQL; test with `admin/admin123`, `cust1/pass123`, a wrong password and
  a new registration.

### `launch.json`: "Property classpath is not allowed"

- **Issue:** VS Code flagged `classpath` in `Run Main` and `Run Testconnection`.
- **Diagnosis:** The Java launch schema has no `classpath` property; the correct
  name is `classPaths`. The old lines were ignored.
- **Change:** Replaced with `"classPaths": ["$Auto", "${workspaceFolder}/lib/mysql-connector-j-26.7.0.jar"]`
  (`$Auto` keeps the project's own output and libraries on the path).

## 2026-10-20 (Phase 5)

### Customer Dashboard and View Turfs

- **Change:** `DatabaseManager.getAllTurfs()` returns every row of `turfs` as a
  `List<Turf>` (ordered by name), using `PreparedStatement` and try-with-resources.
- **Change:** New `ui/CustomerDashboard` (`JFrame` with a `JTabbedPane`: Book a Slot |
  My Bookings, a welcome label and a Logout button). Logout calls `customer.logout()`,
  closes the window and shows `LoginFrame` again.
- **Change:** New `ui/BookSlotPanel`: turf `JComboBox` loaded from `getAllTurfs()`, with
  location, capacity and base price shown in labels. Selecting a turf updates them.
  `getSelectedTurf()` is there for Phase 6 onwards. If the DB call fails a dialog is
  shown; if there are no turfs the combo is disabled and says "No turfs available".
- **Change:** New `ui/MyBookingsPanel`: placeholder until Phase 10.
- **Change:** `Customer.openDashboard()` now opens `CustomerDashboard`.
  `Admin.openDashboard()` opens `PlaceholderDashboard` until Phase 11.
- **Change:** `LoginFrame` no longer opens `PlaceholderDashboard` itself; it only calls
  `user.openDashboard()` (polymorphism). Before, a customer would have got two windows.
- **Validation:** all files in `src/` compile with 0 errors. The windows were NOT run
  (no display and no MySQL in the build environment).
- **Note:** `model/ModelTest.java` calls `openDashboard()`, so running it now opens real
  windows and needs the database. Remove that call from the test if you want it DB-free.

## 2026-10-21 (Phase 6)

### Date, Slot List and Availability Check

- **Change:** `DatabaseManager.checkSlot(turfId, java.sql.Date, timeSlot)` counts
  CONFIRMED bookings for that turf + date + slot and returns `true` when the count is 0
  (slot is free). Cancelled bookings do not block a slot.
- **Change:** `BookSlotPanel` now has a date `JComboBox<LocalDate>` (today + next 13 days,
  shown as "Tue, 20 Oct 2026") and a read-only `JTable` (`DefaultTableModel`, columns
  Slot | Status) for the 17 hourly slots 06:00-07:00 ... 22:00-23:00.
- **Change:** `refreshSlots()` clears the table, then loops through every slot calling
  `checkSlot()` once per slot and writes "Available" or "Booked". It runs whenever the
  turf or the date changes. On a DB error it shows one dialog and stops.
- **Change:** `StatusRenderer` (small custom cell renderer) draws "Booked" in red and
  "Available" in dark green (`new Color(0, 128, 0)`).
- **Bug fixed:** the first version only set red for "Booked". `DefaultTableCellRenderer`
  remembers the last colour set, so red stuck to every unselected cell drawn afterwards
  (even "Available"). Now the colour is set explicitly in both cases (red or green).
- **Change:** `getSelectedDate()` added for later phases. Listeners are registered after
  all components are built because changing a selection calls `refreshSlots()`.
- **Change:** `CustomerDashboard` window height 520 -> 680 so the 17 rows fit.
- **Validation:** all files in `src/` compile with 0 errors. NOT run against MySQL or a
  display in the build environment, so "Done when" is still to be checked by hand.

## 2026-10-21 (Phase 7)

### Price Calculation with Surge

- **Change:** `DatabaseManager.getMultiplier(String ruleName)` reads `multiplier` from
  `pricing_rules` for "WEEKEND" or "EVENING" (seed: 1.20 and 1.30). Returns 1.0 if the
  rule does not exist.
- **Change:** `BookSlotPanel` now has a "Price" box (SOUTH of the panel). Clicking a row in
  the slot table calls `showPrice()`, which reads both multipliers from MySQL, calls the
  existing `Booking.determineSurge(...)` (no new surge logic was written) and shows
  `Base: ₹1000.00 | Surge: ×1.30 | Total: ₹1300.00`.
- **Behaviour:** multipliers are read from the database on every click (not cached), so a
  change made by the admin in Phase 11 shows up immediately. Clicking a "Booked" slot shows
  "This slot is already booked". Changing the turf or date resets the box to its prompt.
- **Change:** a selection listener on the table (`ListSelectionListener`) triggers
  `showPrice()`; it ignores events where `getValueIsAdjusting()` is true so each click
  costs one pair of queries, not two. It is registered with the other listeners, last.
- **Change:** `CustomerDashboard` window height 680 -> 720 to make room for the price box.
- **Validation:** all of `src/` compiles with 0 errors. The four weekday/weekend x
  day/evening combinations were checked by a small test of `determineSurge` with the seed
  multipliers (base 1000: 1000, 1300, 1200, 1300; base 800: 800, 1040, 960, 1040), and the
  18:00 / 17:00 boundary behaves correctly. The window itself was NOT run (no display or
  MySQL in the build environment).

## 2026-10-21 (Phase 8)

### Promo Code Validation

- **Change:** `DatabaseManager.validatePromoCode(String code)` runs
  `SELECT ... FROM promo_codes WHERE code = ? AND is_active = TRUE` and returns a
  `PromoCode` object, or `null` if no active code with that name exists. The expiry date
  is checked separately with `promo.isValid()`. MySQL compares text case-insensitively,
  so "welcome10" finds "WELCOME10".
- **Change:** the "Price" box in `BookSlotPanel` is now three rows: promo row (text field,
  Apply button, message label), the Base / Surge / Total line, and a Discount / Final
  total line. Pressing Enter in the field also applies the code.
- **Change:** `applyPromo()`: blank field = no promo (removes any applied code, no error);
  `null` or expired = red "Invalid or expired promo code" (and any earlier promo is
  removed); valid = green "<CODE> applied: N% off", remembered in `appliedPromo`, price
  recalculated. A promo stays applied when the slot, turf or date changes.
- **Change:** `showPrice()` now builds a `Booking`, calls `setSurgeMultiplier(surge)` and
  `calculateFinalTotal(discountPercent)`, so the discount maths lives in `Booking`
  (one place, reused by Phase 9). With a promo the first line says "Subtotal" and the
  second line shows the discount and final total.
- **Change:** `percent()` helper prints 10.0 as "10" and 12.5 as "12.5". The dark green
  colour is now one shared constant `GREEN` used by the promo message and `StatusRenderer`.
- **Validation:** all of `src/` compiles with 0 errors. Tested in code with today's date
  (2026-10-21): WELCOME10 valid; OLD20 invalid (expired); OFF50 invalid (inactive, and
  the SQL never returns it); WELCOME10 on base 1000 gives 1300 -> 1170, 1000 -> 900,
  1200 -> 1080; base 800 x1.30 gives 1040 -> 936. The window and the real MySQL lookups
  were NOT run in the build environment.
- **Note:** `WELCOME10` expires on 2026-12-31. If the demo is later than that, run
  `UPDATE promo_codes SET expiry_date = '2027-12-31' WHERE code = 'WELCOME10';`.
- **Next (Phase 9):** re-check the promo when Confirm is pressed (it could expire between
  Apply and Confirm) and save `promo_code`, `discount_amount` and `final_total`.

## 2026-10-21 (Phase 9)

### Confirm Booking & Transactional Save

- **Change (DatabaseManager):** Added `saveBooking(Booking b)` with full transaction
  support to prevent double-booking race conditions:
  1. `SELECT ... FOR UPDATE` locks the turf row (waits if another booking for same turf
     is in progress)
  2. Re-runs `isSlotFree()` while holding the lock (same check as `checkSlot()`)
  3. `INSERT INTO bookings` with all fields: `user_id`, `turf_id`, `booking_date`,
     `time_slot`, `base_price`, `surge_multiplier`, `promo_code` (nullable),
     `discount_amount`, `final_total`, `status='CONFIRMED'`
  4. `COMMIT` (releases lock) or `ROLLBACK` on any error
  Returns the generated `booking_id`, or `-1` if the slot was taken by someone else
  in the meantime.
- **Change (BookSlotPanel):** Added "Confirm Booking" button (enabled only when a free
  slot is selected). `confirmBooking()` flow:
  1. Re-applies promo from text field (so confirmed price matches what customer sees)
  2. Rejects slots whose start time has already passed
  3. Shows confirmation dialog with turf name, date, slot, and final total
  4. Calls `db.saveBooking(booking)` — database re-checks slot availability
  5. On success: shows booking ID, clears promo field, calls `refreshSlots()` so the
     slot now shows "Booked"
  6. On `-1` (race condition): shows "Sorry, that slot was just booked by someone else"
- **Change (BookSlotPanel):** Added `currentBooking` field to remember the booking
  being shown in the price box; `setCurrentBooking()` enables/disables Confirm button.
- **Change (BookSlotPanel):** `applyPromo()` now called at start of `confirmBooking()`
  to ensure the promo in the text field matches what's applied.
- **Validation:** all of `src/` compiles with 0 errors. Transaction logic and race
  condition handling reviewed. The window and real MySQL were NOT run in the build
  environment.SQL compares text case-insensitively,
  so `welcome10` finds `WELCOME10`.
- **Change:** the "Price" box in `BookSlotPanel` is now three rows: promo row (text field,
  Apply button, message label), the Base / Surge / Total line, and a Discount / Final
  total line. Pressing Enter in the field also applies the code.
- **Change:** `applyPromo()`: blank field = no promo (removes any applied code, no error);
  `null` or expired = red "Invalid or expired promo code" (and any earlier promo is
  removed); valid = green "<CODE> applied: N% off", remembered in `appliedPromo`, price
  recalculated. A promo stays applied when the slot, turf or date changes.
- **Change:** `showPrice()` now builds a `Booking`, calls `setSurgeMultiplier(surge)` and
  `calculateFinalTotal(discountPercent)`, so the discount maths lives in `Booking`
  (one place, reused by Phase 9). With a promo the first line says "Subtotal" and the
  second line shows the discount and final total.
- **Change:** `percent()` helper prints 10.0 as "10" and 12.5 as "12.5". The dark green
  colour is now one shared constant `GREEN` used by the promo message and `StatusRenderer`.
- **Validation:** all of `src/` compiles with 0 errors. Tested in code with today's date
  (2026-10-21): WELCOME10 valid; OLD20 invalid (expired); OFF50 invalid (inactive, and
  the SQL never returns it); WELCOME10 on base 1000 gives 1300 -> 1170, 1000 -> 900,
  1200 -> 1080; base 800 x1.30 gives 1040 -> 936. The window and the real MySQL lookups
  were NOT run in the build environment.
- **Note:** `WELCOME10` expires on 2026-12-31. If the demo is later than that, run
  `UPDATE promo_codes SET expiry_date = '2027-12-31' WHERE code = 'WELCOME10';`.
- **Next (Phase 9):** re-check the promo when Confirm is pressed (it could expire between
  Apply and Confirm) and save `promo_code`, `discount_amount` and `final_total`.

  ## 2026-10-22 (Phase 10)

### My Bookings and Cancel

- **Change:** `DatabaseManager.getBookingsByUser(userId)` joins `bookings` with `turfs`
  (so each booking carries its turf name) for one user, newest first
  (`ORDER BY booking_id DESC`, the highest ID is the most recent booking). Confirmed and
  cancelled bookings are both returned so the customer keeps their history.
- **Change:** `DatabaseManager.cancelBooking(bookingId, userId)` runs
  `UPDATE bookings SET status='CANCELLED' WHERE booking_id=? AND user_id=? AND status='CONFIRMED'`
  and returns `true` when exactly 1 row changed. `user_id` means a customer can only cancel
  their own booking; `status='CONFIRMED'` stops a double cancel. The row is kept, and
  `checkSlot()` ignores cancelled rows, so the slot becomes free again.
- **Change:** `Booking` gets a full constructor (to rebuild a stored booking from a row
  without recalculating anything), a display-only `turfName` field with getter/setter, and
  `isCancellable()`: true only when the status is CONFIRMED and the slot has not started yet
  (this is the plan's optional "no cancelling after the slot start time" rule, and it matches
  Phase 9, which refuses to book a slot that has already started).
- **Change:** `MyBookingsPanel` replaces the placeholder: table `ID | Turf | Date | Slot |
  Total | Status` (read-only), a count label, and a Cancel Booking button that is enabled
  only when the selected row `isCancellable()`. Clicking it re-checks the rule, asks "are
  you sure?", cancels, and reloads the table. Status text is green (CONFIRMED) or grey
  (CANCELLED).
- **Change:** `CustomerDashboard` adds a `ChangeListener` on the tabs: opening My Bookings
  reloads the bookings; opening Book a Slot calls `refreshSlots()`. This is what makes a
  cancelled booking's slot turn back to "Available" and a new booking appear in the list
  without restarting. `BookSlotPanel.refreshSlots()` was made `public` for this.
- **Validation:** all of `src/` compiles with 0 errors. `Booking.isCancellable()` was tested
  for: future date, 13 days ahead, already cancelled, yesterday, last week, today's slot that
  started an hour ago, today's current-hour slot and today's later slot. The window and the
  SQL were NOT run in the build environment (no display or MySQL).
- **Note:** switching back to the Book tab reloads the slot table, so any selected slot and
  price are cleared (the promo code stays applied).