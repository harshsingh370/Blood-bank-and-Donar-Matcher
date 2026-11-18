package src;

import java.util.*;

/**
 * Medically-grounded compatibility engine for Red Blood Cells (RBC),
 * Platelets, and Plasma matching between hospital emergency requests,
 * available blood units, and volunteer donors.
 */
public final class CompatibilityEngine {

    private CompatibilityEngine() {}

    public static final String[] BLOOD_TYPES = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
    public static final String[] COMPONENTS = {"Red Blood Cells (RBC)", "Platelets", "Plasma"};
    public static final String[] URGENCIES = {"CRITICAL", "URGENT", "ROUTINE"};

    /**
     * Determines whether a donor blood unit is medically compatible with a recipient's request.
     *
     * @param recipientType ABO/Rh of recipient (e.g. "A+", "O-")
     * @param recipientComponent Requested component ("Red Blood Cells (RBC)", "Platelets", "Plasma")
     * @param unitType ABO/Rh of available blood unit
     * @param unitComponent Component of available blood unit
     */
    public static boolean isCompatible(String recipientType, String recipientComponent,
                                       String unitType, String unitComponent) {
        if (recipientType == null || recipientComponent == null || unitType == null || unitComponent == null) {
            return false;
        }

        // Components must match
        if (!normalizeComponent(recipientComponent).equalsIgnoreCase(normalizeComponent(unitComponent))) {
            return false;
        }

        return isBloodTypeCompatible(recipientType.trim(), unitType.trim(), recipientComponent.trim());
    }

    /**
     * Checks blood type compatibility based on component type.
     */
    public static boolean isBloodTypeCompatible(String recipientType, String donorType, String component) {
        String comp = normalizeComponent(component);
        if (comp.contains("Plasma")) {
            return isPlasmaCompatible(recipientType, donorType);
        } else if (comp.contains("Platelet")) {
            return isPlateletCompatible(recipientType, donorType);
        } else {
            // Default to Red Blood Cells (RBC) compatibility
            return isRbcCompatible(recipientType, donorType);
        }
    }

    /**
     * Red Blood Cells (RBC) compatibility:
     * - Universal donor: O-
     * - Universal recipient: AB+
     */
    public static boolean isRbcCompatible(String recipient, String donor) {
        switch (recipient) {
            case "O-":  return donor.equals("O-");
            case "O+":  return donor.equals("O-") || donor.equals("O+");
            case "A-":  return donor.equals("O-") || donor.equals("A-");
            case "A+":  return donor.equals("O-") || donor.equals("O+") || donor.equals("A-") || donor.equals("A+");
            case "B-":  return donor.equals("O-") || donor.equals("B-");
            case "B+":  return donor.equals("O-") || donor.equals("O+") || donor.equals("B-") || donor.equals("B+");
            case "AB-": return donor.equals("O-") || donor.equals("A-") || donor.equals("B-") || donor.equals("AB-");
            case "AB+": return true; // Universal RBC recipient
            default:    return recipient.equalsIgnoreCase(donor);
        }
    }

    /**
     * Fresh Frozen Plasma compatibility:
     * - Universal plasma donor: AB (contains no anti-A or anti-B antibodies)
     * - Universal plasma recipient: O (O red cells have no A or B antigens)
     */
    public static boolean isPlasmaCompatible(String recipient, String donor) {
        String recAbo = extractAbo(recipient);
        String donAbo = extractAbo(donor);

        switch (recAbo) {
            case "AB": return donAbo.equals("AB");
            case "A":  return donAbo.equals("A") || donAbo.equals("AB");
            case "B":  return donAbo.equals("B") || donAbo.equals("AB");
            case "O":  return true; // Any plasma is safe for O recipient
            default:   return recAbo.equals(donAbo);
        }
    }

    /**
     * Platelet compatibility:
     * Identical ABO/Rh is prioritized; compatible alternatives follow RBC/plasma guidelines.
     */
    public static boolean isPlateletCompatible(String recipient, String donor) {
        if (recipient.equalsIgnoreCase(donor)) return true;
        // Compatible alternative: O- for general red-cell antigen safety, or same ABO Rh-
        String recAbo = extractAbo(recipient);
        String donAbo = extractAbo(donor);
        if (recAbo.equals(donAbo)) return true;
        return isRbcCompatible(recipient, donor);
    }

    /**
     * Returns list of all compatible donor blood types for a recipient and component.
     */
    public static List<String> getCompatibleDonorTypes(String recipientType, String component) {
        List<String> list = new ArrayList<>();
        for (String b : BLOOD_TYPES) {
            if (isBloodTypeCompatible(recipientType, b, component)) {
                list.add(b);
            }
        }
        return list;
    }

    /**
     * Human-readable clinical justification for the match.
     */
    public static String getMatchExplanation(String recipientType, String donorType, String component) {
        if (recipientType.equalsIgnoreCase(donorType)) {
            return "Exact ABO/Rh match (" + donorType + ")";
        }
        String comp = normalizeComponent(component);
        if (comp.contains("Plasma")) {
            if (donorType.startsWith("AB")) {
                return "AB is Universal Plasma Donor (no anti-A/B antibodies)";
            }
            return "Compatible Plasma donor for " + recipientType;
        } else {
            if (donorType.equals("O-")) {
                return "O- is Universal Red Blood Cell donor";
            }
            if (recipientType.equals("AB+")) {
                return "AB+ is Universal Recipient; accepts " + donorType;
            }
            return "Compatible Rh/ABO subgroup match for " + recipientType;
        }
    }

    private static String extractAbo(String bloodType) {
        if (bloodType == null) return "O";
        String s = bloodType.replace("+", "").replace("-", "").trim();
        return s.isEmpty() ? "O" : s;
    }

    public static String normalizeComponent(String comp) {
        if (comp == null) return "Red Blood Cells (RBC)";
        if (comp.toLowerCase().contains("platelet")) return "Platelets";
        if (comp.toLowerCase().contains("plasma")) return "Plasma";
        return "Red Blood Cells (RBC)";
    }
}
