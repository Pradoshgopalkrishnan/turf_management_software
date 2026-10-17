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
