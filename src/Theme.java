package src;

import java.awt.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Colour palette and typography for the Blood Bank and Donor Matcher app.
 * Provides medical-themed tones with full Light and Dark mode support.
 */
public final class Theme {
    private Theme() {}

    public static boolean dark = false;

    private static Color pick(int light, int dk) { return new Color(dark ? dk : light); }

    public static Color bg()           { return pick(0xF8FAFC, 0x0F172A); }
    public static Color card()         { return pick(0xFFFFFF, 0x1E293B); }
    public static Color border()       { return pick(0xE2E8F0, 0x334155); }
    public static Color text()         { return pick(0x0F172A, 0xF8FAFC); }
    public static Color muted()        { return pick(0x64748B, 0x94A3B8); }

    // Primary Brand - Hospital Blood Crimson
    public static Color primary()      { return pick(0xDC2626, 0xEF4444); }
    public static Color primaryHover() { return pick(0xB91C1C, 0xF87171); }
    public static Color primaryDown()  { return pick(0x991B1B, 0xDC2626); }
    public static Color accentText()   { return pick(0xDC2626, 0xFCA5A5); }

    public static Color danger()       { return pick(0xDC2626, 0xEF4444); }
    public static Color dangerHover()  { return pick(0xB91C1C, 0xF87171); }
    public static Color dangerDown()   { return pick(0x991B1B, 0xDC2626); }

    public static Color success()      { return pick(0x16A34A, 0x4ADE80); }
    public static Color warning()      { return pick(0xD97706, 0xFBBF24); }
    public static Color info()         { return pick(0x2563EB, 0x60A5FA); }

    public static Color field()        { return pick(0xF8FAFC, 0x0F172A); }
    public static Color fieldBorder()  { return pick(0xCBD5E1, 0x475569); }
    public static Color hover()        { return pick(0xF1F5F9, 0x334155); }

    public static Color tableHeader()  { return pick(0xF1F5F9, 0x1E293B); }
    public static Color tableAlt()     { return pick(0xF8FAFC, 0x172033); }
    public static Color tableHover()   { return pick(0xFEF2F2, 0x2A1B24); }
    public static Color tableSelect()  { return pick(0xFEE2E2, 0x451A1A); }

    // Gradient Header
    public static Color headerA()      { return pick(0x881337, 0x4C0519); }
    public static Color headerB()      { return pick(0xBE123C, 0x881337); }

    public static Color alpha(Color c, int a) { return new Color(c.getRed(), c.getGreen(), c.getBlue(), a); }

    // Component Badge Accents
    public static Color rbcColor()     { return pick(0xDC2626, 0xF87171); }
    public static Color plateletColor(){ return pick(0xD97706, 0xFBBF24); }
    public static Color plasmaColor()   { return pick(0x0284C7, 0x38BDF8); }

    // ---- fonts: select best installed font family or default to sans-serif
    private static String family;

    public static Font font(int style, float size) {
        if (family == null) {
            Set<String> names = new HashSet<>(Arrays.asList(
                    GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
            family = Font.SANS_SERIF;
            for (String f : new String[]{"Inter", "Segoe UI", "Noto Sans", "Cantarell", "Roboto", "Ubuntu", "DejaVu Sans"}) {
                if (names.contains(f)) { family = f; break; }
            }
        }
        return new Font(family, style, 1).deriveFont(style, size);
    }
}
