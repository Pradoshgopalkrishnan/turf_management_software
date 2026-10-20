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
