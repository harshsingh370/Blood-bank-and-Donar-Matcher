# Hospital Blood Bank Inventory & Donor Matcher (MySQL Version)

A modern, high-performance Java Swing hospital inventory and emergency transfusion matching application connected to **MySQL**.

This system tracks donated blood units across all 8 ABO/Rh blood groups, handles blood components (**Red Blood Cells**, **Platelets**, **Plasma**) with clinical expiration lifespans, and automatically matches emergency hospital transfusion requests with compatible blood units and on-call volunteer donors.

---

## 🌟 Key Features

### 1. Blood Inventory Management (ABO/Rh & Components)
* **All 8 Blood Groups**: A+, A-, B+, B-, AB+, AB-, O+, O-.
* **Component-Specific Lifespans**:
  * **Red Blood Cells (RBC)**: 42 days refrigerated (1–6 °C).
  * **Platelets**: 5–7 days with agitation at room temperature (20–24 °C).
  * **Fresh Frozen Plasma (FFP)**: 365 days (1 year) deep frozen (-18 °C or colder).
* **Automated Expiry & Freshness Badges**:
  * `Available` (Green)
  * `Expiring Soon (≤ 7 days)` (Amber warning)
  * `Expired` (Red)
  * `Transfused` (Muted/Muted Gray)
* **Real-time Inventory Filtering**: Filter by Blood Group, Component type, Freshness status, or use live keyword search.
* One-click **"Mark Transfused"** and **"Delete Unit"** actions.

### 2. Emergency Hospital Requests & Smart Matcher Engine
* **Emergency Transfusion Tracking**: Patient name, hospital/ICU ward, required ABO/Rh group, required component, units needed, and urgency level (`CRITICAL`, `URGENT`, `ROUTINE`).
* **Automated Smart Compatibility Engine**:
  * Medically accurate compatibility logic for **Red Blood Cells (RBC)** (Universal donor O-, Universal recipient AB+).
  * Inverted antibody compatibility for **Plasma** (Universal plasma donor AB).
  * Platelet compatibility matching.
* **Instant Dual Matching Display**:
  1. **Compatible In-Stock Units**: Shows all available stock matching patient criteria with clinical compatibility justifications (e.g. *"Universal Red Blood Cell donor (O-)"* or *"Exact ABO/Rh match (A+)"*) and a **One-Click Allocate & Dispatch** button.
  2. **Compatible On-Call Donors**: Scans the volunteer registry for compatible donors who are eligible to donate today, displaying phone number, city, and eligibility status.

### 3. Volunteer Donor Directory
* Manages volunteer donors: Full name, ABO/Rh type, gender, age, phone number, email address, city, and last donation date.
* **56-Day Donation Interval Tracker**: Automatically computes days since last donation to determine if the donor is eligible to donate today or deferred.

### 4. Modern UI & Theme
* Hospital Blood Crimson palette with custom rounded cards, soft drop shadows, and anti-aliased typography.
* One-click **Light / Dark Mode** toggle in the header.
* Real-time **Stat Cards**: Total Blood Units, Critical Emergencies, Units Expiring Soon, Eligible Donors.
* Live search filtering on all tables.
* Styled confirmation modals and non-intrusive status toast messages.

---

## 🗄️ MySQL Database Setup

The project includes `blood_bank.sql`, containing the full relational schema and realistic sample records.

### 1. Database Schema Overview
* **`donors`**: Registered volunteer donors with contact details and donation history.
* **`blood_inventory`**: Donated units with unit codes, ABO/Rh groups, components, volumes, donation dates, and expiration dates.
* **`emergency_requests`**: Hospital emergency requests with urgency ratings.
* **`match_allocations`**: Transfusion unit allocation records linking requests to inventory units.

### 2. Importing Schema & Sample Data
In your terminal or MySQL command line:
```bash
mysql -u root -p < blood_bank.sql
```
*(If prompted for a password, enter your MySQL root password).*

### 3. Database Configuration (`db.properties`)
The app reads database credentials from `db.properties` in the project root:
```properties
db.host=localhost
db.port=3306
db.name=blood_bank_db
db.user=root
db.password=
```
> **Tip:** You can also change and test database connection settings directly inside the application GUI by clicking the **"Database Config"** button in the top right header!

### 4. Auto-Bootstrap Fallback
If you launch the app before running the SQL script, the application **automatically bootstraps** the `blood_bank_db` database, generates all necessary tables, and seeds starter data upon connection.

---

## 🚀 How to Run

### Option A: Using the Provided Scripts
* **Linux / macOS**:
  ```bash
  ./run.sh
  ```
* **Windows**:
  Double-click `run.bat` or run:
  ```cmd
  run.bat
  ```

The script compiles all `.java` files against `lib/mysql-connector-j.jar` and launches the application.

---

### Option B: Manual Terminal Execution
```bash
# Compile
javac -d bin -cp "lib/*" src/*.java

# Run
java -cp "bin:lib/*" src.BloodBankManager
```

---

### Option C: Eclipse IDE Setup
1. Open Eclipse and select **File > Open Projects from File System...** (or **New > Java Project** and import the `src/` folder).
2. Right-click the project > **Build Path > Configure Build Path...**
3. Under the **Libraries** tab, select **Classpath** > click **Add JARs...**
4. Select `lib/mysql-connector-j.jar` (and `lib/slf4j-api.jar`).
5. Right-click `src/BloodBankManager.java` > **Run As > Java Application**.
