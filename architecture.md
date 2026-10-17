# Project Architecture Diagram

The following diagram shows the high‑level layout of the Turf Management project. It is a **workspace‑level** view, not a detailed class diagram.

```mermaid
flowchart TD
    subgraph src
        subgraph db
            Testconnection[TestConnection.java]
            DatabaseManager[DatabaseManager.java]
        end
        subgraph model
            User[User.java]
            Turf[Turf.java]
            PromoCode[PromoCode.java]
            ModelTest[ModelTest.java]
            Customer[Customer.java]
            Booking[Booking.java]
            Admin[Admin.java]
        end
    end
    subgraph bin
        db_bin[bin/db]
        model_bin[bin/model]
    end
    subgraph lib
        mysql[mysql‑connector‑j‑26.7.0.jar]
    end
    subgraph sql
        schema[sql/schema.sql]
        seed[sql/seed.sql]
    end
    subgraph .vscode
        launch[launch.json]
    end
    src --> bin
    src --> lib
    src --> sql
    src --> .vscode
```

- **src/** – source code
  - **db/** – database connection helpers
  - **model/** – domain entities and business logic
- **bin/** – compiled `.class` files
- **lib/** – external JAR dependencies
- **sql/** – database schema and seed data
- **.vscode/** – VS Code launch configuration

Feel free to adjust the diagram if you add more modules or change the structure.
