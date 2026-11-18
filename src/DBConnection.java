package src;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.Properties;

/**
 * Handles MySQL database connectivity, configuration properties loading/saving,
 * and automatic schema/starter data initialization.
 */
public class DBConnection {

    private static final String CONFIG_FILE = "db.properties";
    private static String host = "localhost";
    private static int port = 3306;
    private static String dbName = "blood_bank_db";
    private static String user = "root";
    private static String password = "";

    private static boolean driverLoaded = false;
    private static boolean initialized = false;

    static {
        loadConfig();
        ensureDriver();
    }

    private static void ensureDriver() {
        if (!driverLoaded) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                driverLoaded = true;
            } catch (ClassNotFoundException e) {
                try {
                    Class.forName("com.mysql.jdbc.Driver");
                    driverLoaded = true;
                } catch (ClassNotFoundException ex) {
                    System.err.println("MySQL JDBC Driver not found! Ensure mysql-connector-j.jar is in the classpath.");
                }
            }
        }
    }

    public static synchronized void loadConfig() {
        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            Properties props = new Properties();
            try (FileInputStream in = new FileInputStream(file)) {
                props.load(in);
                host = props.getProperty("db.host", host).trim();
                try {
                    port = Integer.parseInt(props.getProperty("db.port", String.valueOf(port)).trim());
                } catch (NumberFormatException ignored) {}
                dbName = props.getProperty("db.name", dbName).trim();
                user = props.getProperty("db.user", user).trim();
                password = props.getProperty("db.password", password);
            } catch (IOException e) {
                System.err.println("Notice: Could not read " + CONFIG_FILE + ", using defaults: " + e.getMessage());
            }
        }
    }

    public static synchronized void saveConfig(String newHost, int newPort, String newDb, String newUser, String newPass) {
        host = newHost;
        port = newPort;
        dbName = newDb;
        user = newUser;
        password = newPass;

        Properties props = new Properties();
        props.setProperty("db.host", host);
        props.setProperty("db.port", String.valueOf(port));
        props.setProperty("db.name", dbName);
        props.setProperty("db.user", user);
        props.setProperty("db.password", password == null ? "" : password);

        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
            props.store(out, "MySQL Database Configuration for Hospital Blood Bank & Donor Matcher");
        } catch (IOException e) {
            System.err.println("Notice: Could not write " + CONFIG_FILE + ": " + e.getMessage());
        }
        initialized = false; // re-init check for new target database
    }

    public static String getDbHost() { return host; }
    public static int getDbPort() { return port; }
    public static String getDbName() { return dbName; }
    public static String getDbUser() { return user; }
    public static String getDbPassword() { return password; }

    public static String getJdbcUrl() {
        return "jdbc:mysql://" + host + ":" + port + "/" + dbName
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8";
    }

    public static String getServerJdbcUrl() {
        return "jdbc:mysql://" + host + ":" + port + "/"
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8";
    }

    public static Connection getConnection() throws SQLException {
        ensureDriver();
        if (!initialized) {
            autoBootstrap();
        }
        return DriverManager.getConnection(getJdbcUrl(), user, password);
    }

    public static boolean testConnection(String tHost, int tPort, String tDb, String tUser, String tPass) {
        ensureDriver();
        String url = "jdbc:mysql://" + tHost + ":" + tPort + "/" + tDb
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8";
        try (Connection conn = DriverManager.getConnection(url, tUser, tPass)) {
            return conn.isValid(3);
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Automatically creates the database and required tables if they don't already exist,
     * and seeds initial realistic blood bank data.
     */
    public static synchronized void autoBootstrap() {
        if (initialized) return;
        ensureDriver();

        // 1. Create database if missing
        try (Connection serverConn = DriverManager.getConnection(getServerJdbcUrl(), user, password);
             Statement stmt = serverConn.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + dbName + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        } catch (SQLException e) {
            System.err.println("Note: DB auto-creation skipped or direct access required: " + e.getMessage());
        }

        // 2. Create tables and seed data
        try (Connection dbConn = DriverManager.getConnection(getJdbcUrl(), user, password);
             Statement stmt = dbConn.createStatement()) {

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS `donors` (" +
                    "  `donor_id` INT AUTO_INCREMENT PRIMARY KEY," +
                    "  `full_name` VARCHAR(100) NOT NULL," +
                    "  `blood_type` VARCHAR(5) NOT NULL," +
                    "  `gender` VARCHAR(10) NOT NULL DEFAULT 'Male'," +
                    "  `age` INT NOT NULL," +
                    "  `phone` VARCHAR(25) NOT NULL," +
                    "  `email` VARCHAR(100)," +
                    "  `city` VARCHAR(100) NOT NULL," +
                    "  `last_donation_date` DATE," +
                    "  `status` VARCHAR(20) NOT NULL DEFAULT 'Eligible'" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS `blood_inventory` (" +
                    "  `unit_id` INT AUTO_INCREMENT PRIMARY KEY," +
                    "  `unit_code` VARCHAR(30) NOT NULL UNIQUE," +
                    "  `donor_id` INT NULL," +
                    "  `blood_type` VARCHAR(5) NOT NULL," +
                    "  `component` VARCHAR(30) NOT NULL," +
                    "  `volume_ml` INT NOT NULL DEFAULT 450," +
                    "  `donation_date` DATE NOT NULL," +
                    "  `expiration_date` DATE NOT NULL," +
                    "  `storage_location` VARCHAR(100) NOT NULL," +
                    "  `status` VARCHAR(20) NOT NULL DEFAULT 'Available'" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS `emergency_requests` (" +
                    "  `request_id` INT AUTO_INCREMENT PRIMARY KEY," +
                    "  `patient_name` VARCHAR(100) NOT NULL," +
                    "  `hospital_name` VARCHAR(150) NOT NULL," +
                    "  `required_blood_type` VARCHAR(5) NOT NULL," +
                    "  `required_component` VARCHAR(30) NOT NULL," +
                    "  `units_needed` INT NOT NULL DEFAULT 1," +
                    "  `urgency_level` VARCHAR(20) NOT NULL DEFAULT 'CRITICAL'," +
                    "  `request_date` DATE NOT NULL," +
                    "  `status` VARCHAR(20) NOT NULL DEFAULT 'Pending'," +
                    "  `notes` TEXT" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS `match_allocations` (" +
                    "  `allocation_id` INT AUTO_INCREMENT PRIMARY KEY," +
                    "  `request_id` INT NOT NULL," +
                    "  `unit_id` INT NOT NULL," +
                    "  `allocated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "  `status` VARCHAR(20) NOT NULL DEFAULT 'Allocated'" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            // Seed if tables are empty
            ResultSet rsDonors = stmt.executeQuery("SELECT COUNT(*) FROM `donors`");
            int donorCount = rsDonors.next() ? rsDonors.getInt(1) : 0;
            rsDonors.close();

            if (donorCount == 0) {
                stmt.executeUpdate("INSERT INTO `donors` (`donor_id`, `full_name`, `blood_type`, `gender`, `age`, `phone`, `email`, `city`, `last_donation_date`, `status`) VALUES " +
                        "(1, 'Sarah Jenkins',      'O-',  'Female', 29, '+1 (555) 234-8901', 'sarah.j@gmail.com',     'Metro City',   '2026-08-15', 'Eligible')," +
                        "(2, 'Marcus Vance',       'O+',  'Male',   34, '+1 (555) 345-6712', 'mvance@outlook.com',    'Springfield',  '2026-09-01', 'Eligible')," +
                        "(3, 'Dr. Priya Sharma',   'A+',  'Female', 38, '+1 (555) 456-7823', 'priya.s@hospital.org',  'Metro City',   '2026-10-10', 'Eligible')," +
                        "(4, 'Liam O''Connor',     'A-',  'Male',   26, '+1 (555) 567-8934', 'liam.oc@gmail.com',     'Riverdale',    '2026-11-01', 'Deferred')," +
                        "(5, 'Carlos Ramirez',     'B+',  'Male',   41, '+1 (555) 678-9045', 'c.ramirez@company.net', 'Metro City',   '2026-07-20', 'Eligible')," +
                        "(6, 'Amina Al-Mansoor',   'B-',  'Female', 31, '+1 (555) 789-0156', 'amina.m@gmail.com',     'Oakwood',      '2026-08-28', 'Eligible')," +
                        "(7, 'David Kim',          'AB+', 'Male',   24, '+1 (555) 890-1267', 'dkim99@gmail.com',      'Springfield',  '2026-09-18', 'Eligible')," +
                        "(8, 'Elena Rostova',      'AB-', 'Female', 35, '+1 (555) 901-2378', 'elena.rost@tech.io',    'Metro City',   '2026-10-05', 'Eligible')," +
                        "(9, 'James Wilson',       'O+',  'Male',   45, '+1 (555) 112-3344', 'jwilson@cityline.org',  'Riverdale',    '2026-06-12', 'Eligible')," +
                        "(10, 'Sophia Martinez',   'O-',  'Female', 27, '+1 (555) 223-4455', 'smartinez@gmail.com',   'Metro City',   '2026-09-22', 'Eligible')");
            }

            ResultSet rsInventory = stmt.executeQuery("SELECT COUNT(*) FROM `blood_inventory`");
            int invCount = rsInventory.next() ? rsInventory.getInt(1) : 0;
            rsInventory.close();

            if (invCount == 0) {
                stmt.executeUpdate("INSERT INTO `blood_inventory` (`unit_id`, `unit_code`, `donor_id`, `blood_type`, `component`, `volume_ml`, `donation_date`, `expiration_date`, `storage_location`, `status`) VALUES " +
                        "(1,  'BLD-2026-0101', 1,  'O-',  'Red Blood Cells (RBC)', 450, '2026-11-01', '2026-12-13', 'Main Cold Room - Shelf A1', 'Available')," +
                        "(2,  'BLD-2026-0102', 10, 'O-',  'Red Blood Cells (RBC)', 450, '2026-10-18', '2026-11-29', 'Main Cold Room - Shelf A1', 'Available')," +
                        "(3,  'BLD-2026-0103', 1,  'O-',  'Plasma',                250, '2026-06-10', '2027-06-10', 'Deep Freezer - Box F1',    'Available')," +
                        "(4,  'BLD-2026-0104', 2,  'O+',  'Red Blood Cells (RBC)', 450, '2026-11-05', '2026-12-17', 'Main Cold Room - Shelf A2', 'Available')," +
                        "(5,  'BLD-2026-0105', 9,  'O+',  'Platelets',             300, '2026-11-15', '2026-11-21', 'Agitator Chamber - Bay 1', 'Available')," +
                        "(6,  'BLD-2026-0106', 2,  'O+',  'Plasma',                250, '2026-08-01', '2027-08-01', 'Deep Freezer - Box F2',    'Available')," +
                        "(7,  'BLD-2026-0107', 3,  'A+',  'Red Blood Cells (RBC)', 450, '2026-11-10', '2026-12-22', 'Main Cold Room - Shelf B1', 'Available')," +
                        "(8,  'BLD-2026-0108', 3,  'A+',  'Platelets',             300, '2026-11-14', '2026-11-20', 'Agitator Chamber - Bay 2', 'Available')," +
                        "(9,  'BLD-2026-0109', 3,  'A+',  'Plasma',                250, '2026-09-15', '2027-09-15', 'Deep Freezer - Box F3',    'Available')," +
                        "(10, 'BLD-2026-0110', 4,  'A-',  'Red Blood Cells (RBC)', 450, '2026-10-12', '2026-11-23', 'Main Cold Room - Shelf B2', 'Available')," +
                        "(11, 'BLD-2026-0111', 5,  'B+',  'Red Blood Cells (RBC)', 450, '2026-10-25', '2026-12-06', 'Main Cold Room - Shelf C1', 'Available')," +
                        "(12, 'BLD-2026-0112', 5,  'B+',  'Platelets',             300, '2026-11-16', '2026-11-22', 'Agitator Chamber - Bay 3', 'Available')," +
                        "(13, 'BLD-2026-0113', 6,  'B-',  'Red Blood Cells (RBC)', 450, '2026-10-20', '2026-12-01', 'Main Cold Room - Shelf C2', 'Available')," +
                        "(14, 'BLD-2026-0114', 7,  'AB+', 'Red Blood Cells (RBC)', 450, '2026-11-08', '2026-12-20', 'Main Cold Room - Shelf D1', 'Available')," +
                        "(15, 'BLD-2026-0115', 7,  'AB+', 'Platelets',             300, '2026-11-13', '2026-11-19', 'Agitator Chamber - Bay 4', 'Available')," +
                        "(16, 'BLD-2026-0116', 8,  'AB-', 'Plasma',                250, '2026-05-20', '2027-05-20', 'Deep Freezer - Box F4',    'Available')," +
                        "(17, 'BLD-2026-0117', 8,  'AB-', 'Red Blood Cells (RBC)', 450, '2026-10-08', '2026-11-19', 'Main Cold Room - Shelf D2', 'Available')");
            }

            ResultSet rsReq = stmt.executeQuery("SELECT COUNT(*) FROM `emergency_requests`");
            int reqCount = rsReq.next() ? rsReq.getInt(1) : 0;
            rsReq.close();

            if (reqCount == 0) {
                stmt.executeUpdate("INSERT INTO `emergency_requests` (`request_id`, `patient_name`, `hospital_name`, `required_blood_type`, `required_component`, `units_needed`, `urgency_level`, `request_date`, `status`, `notes`) VALUES " +
                        "(1, 'Michael Chang',   'City Trauma Center - ICU',  'A+', 'Red Blood Cells (RBC)', 2, 'CRITICAL', '2026-11-17', 'Pending',   'Severe motor vehicle trauma with hemorrhagic shock.')," +
                        "(2, 'Emma Watson',      'St. Jude Children Hospital','O-', 'Red Blood Cells (RBC)', 1, 'CRITICAL', '2026-11-17', 'Pending',   'Emergency pediatric cardiac surgery. Strict O- negative required.')," +
                        "(3, 'Robert Gallagher', 'Memorial Oncology Wing',    'B+', 'Platelets',             2, 'URGENT',   '2026-11-16', 'Pending',   'Severe post-chemotherapy thrombocytopenia, active mucosal bleed.')," +
                        "(4, 'Anita Desai',      'Women & Children Hospital', 'AB+','Plasma',                1, 'ROUTINE',  '2026-11-16', 'Fulfilled', 'Disseminated intravascular coagulation management - unit dispatched.')," +
                        "(5, 'Lucas Bennett',    'General Hospital - ER',     'O+', 'Red Blood Cells (RBC)', 2, 'CRITICAL', '2026-11-17', 'Pending',   'Ruptured abdominal aortic aneurysm, massive transfusion protocol initiated.')");
            }

            initialized = true;
        } catch (SQLException e) {
            System.err.println("Database autoBootstrap error: " + e.getMessage());
        }
    }
}
