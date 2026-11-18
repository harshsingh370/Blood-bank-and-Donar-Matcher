package src;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

public class BloodUnit {
    private int id;
    private String unitCode;
    private Integer donorId;
    private String donorName;
    private String bloodType; // A+, A-, B+, B-, AB+, AB-, O+, O-
    private String component; // Red Blood Cells (RBC), Platelets, Plasma
    private int volumeMl;     // e.g. 450
    private String donationDate;   // YYYY-MM-DD
    private String expirationDate; // YYYY-MM-DD
    private String storageLocation;
    private String status;    // Available, Reserved, Transfused, Expired

    public BloodUnit() {}

    public BloodUnit(int id, String unitCode, Integer donorId, String bloodType,
                     String component, int volumeMl, String donationDate,
                     String expirationDate, String storageLocation, String status) {
        this.id = id;
        this.unitCode = unitCode;
        this.donorId = donorId;
        this.bloodType = bloodType;
        this.component = component;
        this.volumeMl = volumeMl;
        this.donationDate = donationDate;
        this.expirationDate = expirationDate;
        this.storageLocation = storageLocation;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUnitCode() { return unitCode; }
    public void setUnitCode(String unitCode) { this.unitCode = unitCode; }

    public Integer getDonorId() { return donorId; }
    public void setDonorId(Integer donorId) { this.donorId = donorId; }

    public String getDonorName() { return donorName; }
    public void setDonorName(String donorName) { this.donorName = donorName; }

    public String getBloodType() { return bloodType; }
    public void setBloodType(String bloodType) { this.bloodType = bloodType; }

    public String getComponent() { return component; }
    public void setComponent(String component) { this.component = component; }

    public int getVolumeMl() { return volumeMl; }
    public void setVolumeMl(int volumeMl) { this.volumeMl = volumeMl; }

    public String getDonationDate() { return donationDate; }
    public void setDonationDate(String donationDate) { this.donationDate = donationDate; }

    public String getExpirationDate() { return expirationDate; }
    public void setExpirationDate(String expirationDate) { this.expirationDate = expirationDate; }

    public String getStorageLocation() { return storageLocation; }
    public void setStorageLocation(String storageLocation) { this.storageLocation = storageLocation; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getDaysUntilExpiration() {
        if (expirationDate == null || expirationDate.trim().isEmpty()) return 999;
        try {
            LocalDate exp = LocalDate.parse(expirationDate.trim());
            return ChronoUnit.DAYS.between(LocalDate.now(), exp);
        } catch (DateTimeParseException e) {
            return 999;
        }
    }

    public boolean isExpired() {
        if ("Expired".equalsIgnoreCase(status) || "Transfused".equalsIgnoreCase(status)) return true;
        return getDaysUntilExpiration() < 0;
    }

    public boolean isExpiringSoon() {
        if (!"Available".equalsIgnoreCase(status)) return false;
        long days = getDaysUntilExpiration();
        return days >= 0 && days <= 7;
    }

    public String getDisplayStatus() {
        if ("Transfused".equalsIgnoreCase(status)) return "Transfused";
        if ("Reserved".equalsIgnoreCase(status)) return "Reserved";
        long days = getDaysUntilExpiration();
        if (days < 0) return "Expired";
        if (days <= 7) return "Expiring Soon";
        return "Available";
    }

    @Override
    public String toString() {
        return unitCode + " (" + bloodType + " " + component + ")";
    }
}
