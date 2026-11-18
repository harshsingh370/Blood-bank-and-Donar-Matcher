package src;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for MySQL database interactions:
 * Blood Inventory, Donors, Emergency Requests, and Match Allocations.
 */
public class BloodBankDAO {

    public BloodBankDAO() {
        // Triggers bootstrap if needed
        DBConnection.autoBootstrap();
    }

    // ========================================================================
    // BLOOD INVENTORY (UNITS)
    // ========================================================================
    public List<BloodUnit> getAllUnits() {
        List<BloodUnit> list = new ArrayList<>();
        String sql = "SELECT b.*, d.full_name AS donor_name FROM blood_inventory b " +
                     "LEFT JOIN donors d ON b.donor_id = d.donor_id " +
                     "ORDER BY b.expiration_date ASC";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                BloodUnit u = new BloodUnit();
                u.setId(rs.getInt("unit_id"));
                u.setUnitCode(rs.getString("unit_code"));
                int dId = rs.getInt("donor_id");
                u.setDonorId(rs.wasNull() ? null : dId);
                u.setDonorName(rs.getString("donor_name"));
                u.setBloodType(rs.getString("blood_type"));
                u.setComponent(rs.getString("component"));
                u.setVolumeMl(rs.getInt("volume_ml"));
                u.setDonationDate(rs.getString("donation_date"));
                u.setExpirationDate(rs.getString("expiration_date"));
                u.setStorageLocation(rs.getString("storage_location"));
                u.setStatus(rs.getString("status"));
                list.add(u);
            }
        } catch (SQLException e) {
            System.err.println("getAllUnits error: " + e.getMessage());
        }
        return list;
    }

    public boolean addUnit(BloodUnit u) {
        String sql = "INSERT INTO blood_inventory (unit_code, donor_id, blood_type, component, " +
                     "volume_ml, donation_date, expiration_date, storage_location, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, u.getUnitCode());
            if (u.getDonorId() == null) ps.setNull(2, Types.INTEGER);
            else ps.setInt(2, u.getDonorId());
            ps.setString(3, u.getBloodType());
            ps.setString(4, u.getComponent());
            ps.setInt(5, u.getVolumeMl());
            ps.setString(6, u.getDonationDate());
            ps.setString(7, u.getExpirationDate());
            ps.setString(8, u.getStorageLocation());
            ps.setString(9, u.getStatus() == null ? "Available" : u.getStatus());

            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("addUnit error: " + e.getMessage());
            return false;
        }
    }

    public boolean updateUnit(BloodUnit u) {
        String sql = "UPDATE blood_inventory SET unit_code = ?, donor_id = ?, blood_type = ?, " +
                     "component = ?, volume_ml = ?, donation_date = ?, expiration_date = ?, " +
                     "storage_location = ?, status = ? WHERE unit_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, u.getUnitCode());
            if (u.getDonorId() == null) ps.setNull(2, Types.INTEGER);
            else ps.setInt(2, u.getDonorId());
            ps.setString(3, u.getBloodType());
            ps.setString(4, u.getComponent());
            ps.setInt(5, u.getVolumeMl());
            ps.setString(6, u.getDonationDate());
            ps.setString(7, u.getExpirationDate());
            ps.setString(8, u.getStorageLocation());
            ps.setString(9, u.getStatus());
            ps.setInt(10, u.getId());

            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("updateUnit error: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteUnit(int id) {
        String sql = "DELETE FROM blood_inventory WHERE unit_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("deleteUnit error: " + e.getMessage());
            return false;
        }
    }

    public boolean updateUnitStatus(int unitId, String status) {
        String sql = "UPDATE blood_inventory SET status = ? WHERE unit_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, unitId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("updateUnitStatus error: " + e.getMessage());
            return false;
        }
    }

    // ========================================================================
    // DONORS DIRECTORY
    // ========================================================================
    public List<Donor> getAllDonors() {
        List<Donor> list = new ArrayList<>();
        String sql = "SELECT * FROM donors ORDER BY full_name ASC";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Donor d = new Donor();
                d.setId(rs.getInt("donor_id"));
                d.setName(rs.getString("full_name"));
                d.setBloodType(rs.getString("blood_type"));
                d.setGender(rs.getString("gender"));
                d.setAge(rs.getInt("age"));
                d.setPhone(rs.getString("phone"));
                d.setEmail(rs.getString("email"));
                d.setCity(rs.getString("city"));
                d.setLastDonationDate(rs.getString("last_donation_date"));
                d.setStatus(rs.getString("status"));
                list.add(d);
            }
        } catch (SQLException e) {
            System.err.println("getAllDonors error: " + e.getMessage());
        }
        return list;
    }

    public boolean addDonor(Donor d) {
        String sql = "INSERT INTO donors (full_name, blood_type, gender, age, phone, " +
                     "email, city, last_donation_date, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, d.getName());
            ps.setString(2, d.getBloodType());
            ps.setString(3, d.getGender());
            ps.setInt(4, d.getAge());
            ps.setString(5, d.getPhone());
            ps.setString(6, d.getEmail());
            ps.setString(7, d.getCity());
            ps.setString(8, d.getLastDonationDate());
            ps.setString(9, d.getStatus() == null ? "Eligible" : d.getStatus());

            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("addDonor error: " + e.getMessage());
            return false;
        }
    }

    public boolean updateDonor(Donor d) {
        String sql = "UPDATE donors SET full_name = ?, blood_type = ?, gender = ?, age = ?, " +
                     "phone = ?, email = ?, city = ?, last_donation_date = ?, status = ? WHERE donor_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, d.getName());
            ps.setString(2, d.getBloodType());
            ps.setString(3, d.getGender());
            ps.setInt(4, d.getAge());
            ps.setString(5, d.getPhone());
            ps.setString(6, d.getEmail());
            ps.setString(7, d.getCity());
            ps.setString(8, d.getLastDonationDate());
            ps.setString(9, d.getStatus());
            ps.setInt(10, d.getId());

            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("updateDonor error: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteDonor(int id) {
        String sql = "DELETE FROM donors WHERE donor_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("deleteDonor error: " + e.getMessage());
            return false;
        }
    }

    // ========================================================================
    // EMERGENCY REQUESTS
    // ========================================================================
    public List<EmergencyRequest> getAllRequests() {
        List<EmergencyRequest> list = new ArrayList<>();
        String sql = "SELECT * FROM emergency_requests ORDER BY " +
                     "CASE urgency_level WHEN 'CRITICAL' THEN 1 WHEN 'URGENT' THEN 2 ELSE 3 END, " +
                     "request_id DESC";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                EmergencyRequest r = new EmergencyRequest();
                r.setId(rs.getInt("request_id"));
                r.setPatientName(rs.getString("patient_name"));
                r.setHospitalName(rs.getString("hospital_name"));
                r.setRequiredBloodType(rs.getString("required_blood_type"));
                r.setRequiredComponent(rs.getString("required_component"));
                r.setUnitsNeeded(rs.getInt("units_needed"));
                r.setUrgencyLevel(rs.getString("urgency_level"));
                r.setRequestDate(rs.getString("request_date"));
                r.setStatus(rs.getString("status"));
                r.setNotes(rs.getString("notes"));
                list.add(r);
            }
        } catch (SQLException e) {
            System.err.println("getAllRequests error: " + e.getMessage());
        }
        return list;
    }

    public boolean addRequest(EmergencyRequest r) {
        String sql = "INSERT INTO emergency_requests (patient_name, hospital_name, " +
                     "required_blood_type, required_component, units_needed, urgency_level, " +
                     "request_date, status, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, r.getPatientName());
            ps.setString(2, r.getHospitalName());
            ps.setString(3, r.getRequiredBloodType());
            ps.setString(4, r.getRequiredComponent());
            ps.setInt(5, r.getUnitsNeeded());
            ps.setString(6, r.getUrgencyLevel());
            ps.setString(7, r.getRequestDate());
            ps.setString(8, r.getStatus() == null ? "Pending" : r.getStatus());
            ps.setString(9, r.getNotes());

            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("addRequest error: " + e.getMessage());
            return false;
        }
    }

    public boolean updateRequest(EmergencyRequest r) {
        String sql = "UPDATE emergency_requests SET patient_name = ?, hospital_name = ?, " +
                     "required_blood_type = ?, required_component = ?, units_needed = ?, " +
                     "urgency_level = ?, request_date = ?, status = ?, notes = ? WHERE request_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, r.getPatientName());
            ps.setString(2, r.getHospitalName());
            ps.setString(3, r.getRequiredBloodType());
            ps.setString(4, r.getRequiredComponent());
            ps.setInt(5, r.getUnitsNeeded());
            ps.setString(6, r.getUrgencyLevel());
            ps.setString(7, r.getRequestDate());
            ps.setString(8, r.getStatus());
            ps.setString(9, r.getNotes());
            ps.setInt(10, r.getId());

            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("updateRequest error: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteRequest(int id) {
        String sql = "DELETE FROM emergency_requests WHERE request_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("deleteRequest error: " + e.getMessage());
            return false;
        }
    }

    public boolean updateRequestStatus(int requestId, String status) {
        String sql = "UPDATE emergency_requests SET status = ? WHERE request_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, requestId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("updateRequestStatus error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Allocates an available inventory unit to an emergency request.
     */
    public boolean allocateUnitToRequest(int requestId, int unitId) {
        String sqlAlloc = "INSERT INTO match_allocations (request_id, unit_id, status) VALUES (?, ?, 'Allocated')";
        String sqlUnit = "UPDATE blood_inventory SET status = 'Reserved' WHERE unit_id = ?";
        String sqlReq = "UPDATE emergency_requests SET status = 'Matched' WHERE request_id = ?";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement psA = conn.prepareStatement(sqlAlloc);
                 PreparedStatement psU = conn.prepareStatement(sqlUnit);
                 PreparedStatement psR = conn.prepareStatement(sqlReq)) {

                psA.setInt(1, requestId);
                psA.setInt(2, unitId);
                psA.executeUpdate();

                psU.setInt(1, unitId);
                psU.executeUpdate();

                psR.setInt(1, requestId);
                psR.executeUpdate();

                conn.commit();
                return true;
            }
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            System.err.println("allocateUnitToRequest error: " + e.getMessage());
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }
}
