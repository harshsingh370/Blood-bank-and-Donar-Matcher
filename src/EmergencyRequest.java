package src;

public class EmergencyRequest {
    private int id;
    private String patientName;
    private String hospitalName;
    private String requiredBloodType; // A+, A-, B+, B-, AB+, AB-, O+, O-
    private String requiredComponent; // Red Blood Cells (RBC), Platelets, Plasma
    private int unitsNeeded;          // default 1
    private String urgencyLevel;      // CRITICAL, URGENT, ROUTINE
    private String requestDate;       // YYYY-MM-DD
    private String status;            // Pending, Matched, Fulfilled, Cancelled
    private String notes;

    public EmergencyRequest() {
        this.unitsNeeded = 1;
        this.urgencyLevel = "CRITICAL";
        this.status = "Pending";
    }

    public EmergencyRequest(int id, String patientName, String hospitalName,
                            String requiredBloodType, String requiredComponent,
                            int unitsNeeded, String urgencyLevel, String requestDate,
                            String status, String notes) {
        this.id = id;
        this.patientName = patientName;
        this.hospitalName = hospitalName;
        this.requiredBloodType = requiredBloodType;
        this.requiredComponent = requiredComponent;
        this.unitsNeeded = unitsNeeded;
        this.urgencyLevel = urgencyLevel;
        this.requestDate = requestDate;
        this.status = status;
        this.notes = notes;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getHospitalName() { return hospitalName; }
    public void setHospitalName(String hospitalName) { this.hospitalName = hospitalName; }

    public String getRequiredBloodType() { return requiredBloodType; }
    public void setRequiredBloodType(String requiredBloodType) { this.requiredBloodType = requiredBloodType; }

    public String getRequiredComponent() { return requiredComponent; }
    public void setRequiredComponent(String requiredComponent) { this.requiredComponent = requiredComponent; }

    public int getUnitsNeeded() { return unitsNeeded; }
    public void setUnitsNeeded(int unitsNeeded) { this.unitsNeeded = unitsNeeded; }

    public String getUrgencyLevel() { return urgencyLevel; }
    public void setUrgencyLevel(String urgencyLevel) { this.urgencyLevel = urgencyLevel; }

    public String getRequestDate() { return requestDate; }
    public void setRequestDate(String requestDate) { this.requestDate = requestDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    @Override
    public String toString() {
        return "#" + id + " " + patientName + " [" + requiredBloodType + " " + requiredComponent + " (" + urgencyLevel + ")]";
    }
}
