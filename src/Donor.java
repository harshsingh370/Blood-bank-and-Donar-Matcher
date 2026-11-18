package src;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

public class Donor {
    private int id;
    private String name;
    private String bloodType; // A+, A-, B+, B-, AB+, AB-, O+, O-
    private String gender;    // Male, Female, Other
    private int age;
    private String phone;
    private String email;
    private String city;
    private String lastDonationDate; // YYYY-MM-DD
    private String status;    // Eligible, Deferred, Ineligible

    public Donor() {}

    public Donor(int id, String name, String bloodType, String gender, int age,
                 String phone, String email, String city, String lastDonationDate, String status) {
        this.id = id;
        this.name = name;
        this.bloodType = bloodType;
        this.gender = gender;
        this.age = age;
        this.phone = phone;
        this.email = email;
        this.city = city;
        this.lastDonationDate = lastDonationDate;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBloodType() { return bloodType; }
    public void setBloodType(String bloodType) { this.bloodType = bloodType; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getLastDonationDate() { return lastDonationDate; }
    public void setLastDonationDate(String lastDonationDate) { this.lastDonationDate = lastDonationDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    /**
     * Standard blood bank interval between whole blood donations is ~56 days (8 weeks).
     */
    public boolean isEligibleNow() {
        if (!"Eligible".equalsIgnoreCase(status)) return false;
        if (lastDonationDate == null || lastDonationDate.trim().isEmpty()) return true;
        try {
            LocalDate last = LocalDate.parse(lastDonationDate.trim());
            long days = ChronoUnit.DAYS.between(last, LocalDate.now());
            return days >= 56;
        } catch (DateTimeParseException e) {
            return true;
        }
    }

    public long getDaysSinceLastDonation() {
        if (lastDonationDate == null || lastDonationDate.trim().isEmpty()) return 999;
        try {
            LocalDate last = LocalDate.parse(lastDonationDate.trim());
            return ChronoUnit.DAYS.between(last, LocalDate.now());
        } catch (DateTimeParseException e) {
            return 999;
        }
    }

    @Override
    public String toString() {
        return name + " (" + bloodType + ", " + city + ")";
    }
}
