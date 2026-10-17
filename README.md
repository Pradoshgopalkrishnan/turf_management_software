# Turf Booking System

A desktop application for booking sports turf slots, built with **Java Swing**, **JDBC** and **MySQL**.
Developed as the Object Oriented Programming mini project (PBCST304) at Govt. Model Engineering College, Thrikkakara (Academic Year 2026-2027).

Sports turf owners often take bookings by phone or message, which leads to double bookings and messy schedules. This project gives customers one place to see which slots are free and book them, and gives the owner one place to manage turfs, bookings and discounts.

---

## Features

| Feature | Who | Status |
|---|---|---|
| Database design and seed data | | Done |
| JDBC connection (credentials kept out of Git) | | Done |
| Model classes (OOP backbone) | | Written, being tested |
| Login and customer registration | Everyone | Planned |
| View turfs and slot availability | Customer | Planned |
| Weekend / evening surge pricing | Customer | Planned |
| Promo code validation | Customer | Planned |
| Book a slot (with double-booking prevention) | Customer | Planned |
| View and cancel own bookings | Customer | Planned |
| Manage turfs (add, edit, delete) | Admin | Planned |
| Manage all bookings | Admin | Planned |
| Manage promo codes and surge pricing | Admin | Planned |

See [`docs/TURF_BOOKING_PLAN.md`](docs/TURF_BOOKING_PLAN.md) for the full phase-by-phase build plan.

---

## How pricing works

```
finalTotal = (basePrice x surgeMultiplier) - discount
```

- **Weekend** (Saturday or Sunday) has a surge multiplier.
- **Evening** (slot starting at 18:00 or later) has a surge multiplier.
- If both apply, the higher multiplier is used.
- A valid promo code takes a percentage off the surged price.

Example: base Rs. 1000, Saturday 7 PM (x1.3), code `WELCOME10` (10% off) gives 1300 - 130 = **Rs. 1170**.

---

## OOP concepts used

| Concept | Where |
|---|---|
| **Abstraction** | `User` is an abstract class with abstract methods `openDashboard()` and `getRole()` |
| **Inheritance** | `Customer` and `Admin` extend `User` |
| **Polymorphism** | Each subclass overrides `openDashboard()`; login code calls it without knowing the type |
| **Encapsulation** | All fields are private and accessed through getters and setters |

---

## Tech stack

- Java 17 or newer
- Java Swing (GUI)
- JDBC with MySQL Connector/J
- MySQL 8
- VS Code with the Extension Pack for Java

---

## Project structure

```
.
├── .vscode/
│   └── settings.json              VS Code Java settings (finds the jar in lib/)
├── docs/                          Build plan, UML, DFD, proposal
├── lib/
│   └── mysql-connector-j-x.x.x.jar
├── sql/
│   ├── schema.sql                 Creates the database and tables
│   └── seed.sql                   Test data
├── src/
│   ├── db/
│   │   ├── DatabaseManager.java   The only class that talks to MySQL
│   │   └── TestConnection.java    Quick connection check
│   ├── model/                     User, Customer, Admin, Turf, Booking, PromoCode
│   └── ui/                        Swing windows (coming in later phases)
├── db.properties.example          Template for your local DB settings
└── README.md
```

---

## Setup

### 1. Install

- JDK 17 or newer
- MySQL Server and MySQL Workbench (or the `mysql` command line)
- VS Code with **Extension Pack for Java**

### 2. Get the code

```bash
git clone <https://github.com/Pradoshgopalkrishnan/turf_management_software>
cd <repo-folder>
```

### 3. Create the database

Run the two scripts in order, in MySQL Workbench or the `mysql` prompt:

1. `sql/schema.sql`
2. `sql/seed.sql`

This creates a database named `turf` with 5 tables and test data.

### 4. Add your database settings

Copy `db.properties.example` to a new file named `db.properties` in the project root, then fill in your details:

```
db.url=jdbc:mysql://localhost:3306/turf
db.user=root
db.password=your_mysql_password
```

`db.properties` is listed in `.gitignore`, so your password is never pushed to GitHub. Never commit it.

### 5. Open in VS Code

Open the project folder in VS Code and wait for the Java extension to finish loading. The jar in `lib/` is picked up automatically through `.vscode/settings.json`. Check that it appears under **Java Projects, then Referenced Libraries**.

### 6. Check the connection

Open `src/db/TestConnection.java` and click **Run** above `main`. You should see:

```
Connected!
1 | Green Arena | Rs. 1000.0
2 | Kick Off Turf | Rs. 800.0
```

Or, from the terminal in the project root (Windows PowerShell):

```powershell
javac -cp "lib/*" -d bin src\db\*.java src\model\*.java
java -cp "bin;lib/*" db.TestConnection
```

### Common errors

| Error | Cause |
|---|---|
| `No suitable driver found` | The jar isn't listed under Referenced Libraries |
| `Access denied for user` | Wrong username or password in `db.properties` |
| `Unknown database 'turf'` | `schema.sql` wasn't run |
| `Communications link failure` | The MySQL service isn't running |
| `Could not read db.properties` | The file is missing or not in the project root |

---

## Test accounts (seed data only)

| Role | Username | Password |
|---|---|---|
| Admin | `admin` | `admin123` |
| Customer | `cust1` | `pass123` |

| Promo code | Meaning |
|---|---|
| `WELCOME10` | Valid, 10% off (until 31 Dec 2026) |
| `OLD20` | Expired, for testing |
| `OFF50` | Inactive, for testing |

These are fake accounts for development. Do not reuse real passwords here.

---

## Database overview

| Table | Purpose |
|---|---|
| `users` | Admins and customers |
| `turfs` | Turf name, location, capacity, base price per hour |
| `pricing_rules` | Weekend and evening surge multipliers |
| `promo_codes` | Code, discount percent, expiry date, active flag |
| `bookings` | Every booking, with price breakdown and status (`CONFIRMED` or `CANCELLED`) |

---

## Team

| Roll No | Name |
|---|---|
| 2 | Abel Mathew Appu |
| 16 | Avin Amba |
| 24 | Evan Paul Gibi |
| 28 | Hasib VS |
| 37 | Leonel Mathew Thuruthiyil |
| 49 | Pradosh Gopalkrishnan |
| 56 | Shraddha Suresh |
| 61 | Vaisali P |

**Project guide:** Ms. Sinu T S, Assistant Professor, Department of Computer Engineering

---

## Contributing (team workflow)

- One branch per feature, small commits, merge into `main` only when it runs.
- Never commit `db.properties` or any real password.
- Follow the phase order in the build plan so features don't block each other.