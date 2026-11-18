package src;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Custom modern Swing UI components: Cards, Rounded Buttons, Text Fields,
 * ComboBoxes, Stat Cards, Scrollbars, and Modal Dialogs.
 */
public final class Ui {
    private Ui() {}

    public static Graphics2D aa(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g2;
    }

    // ------------------------------------------------------------ Label
    public enum Tone { TEXT, MUTED, WHITE, SOFT, ACCENT, DANGER, SUCCESS, WARNING }

    public static class Label extends JLabel {
        private final Tone tone;
        public Label(String text, Tone tone, int style, float size) {
            super(text);
            this.tone = tone;
            setFont(Theme.font(style, size));
        }
        @Override public Color getForeground() {
            if (tone == null) return super.getForeground();
            switch (tone) {
                case MUTED:   return Theme.muted();
                case WHITE:   return Color.WHITE;
                case SOFT:    return new Color(255, 255, 255, 210);
                case ACCENT:  return Theme.accentText();
                case DANGER:  return Theme.danger();
                case SUCCESS: return Theme.success();
                case WARNING: return Theme.warning();
                default:      return Theme.text();
            }
        }
    }

    // ------------------------------------------------------------ Button
    public enum Kind { PRIMARY, SECONDARY, DANGER, SUCCESS, GHOST, HEADER }

    public static class Button extends JButton {
        private final Kind kind;
        public Button(String text, Kind kind) {
            super(text);
            this.kind = kind;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFont(Theme.font(Font.BOLD, 13f));
        }
        @Override public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(fm.stringWidth(getText()) + 32, 38);
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            ButtonModel m = getModel();
            boolean down = m.isPressed(), over = m.isRollover();
            Color fill, fg, line = null;
            switch (kind) {
                case PRIMARY:
                    fill = down ? Theme.primaryDown() : over ? Theme.primaryHover() : Theme.primary();
                    fg = Color.WHITE; break;
                case DANGER:
                    fill = down ? Theme.dangerDown() : over ? Theme.dangerHover() : Theme.danger();
                    fg = Color.WHITE; break;
                case SUCCESS:
                    fill = down ? new Color(0x15803D) : over ? new Color(0x16A34A) : Theme.success();
                    fg = Color.WHITE; break;
                case SECONDARY:
                    fill = (down || over) ? Theme.hover() : Theme.card();
                    line = Theme.fieldBorder(); fg = Theme.text(); break;
                case HEADER:
                    fill = new Color(255, 255, 255, over ? 70 : 40);
                    fg = Color.WHITE; break;
                default: // GHOST
                    fill = (down || over) ? Theme.hover() : new Color(0, 0, 0, 0);
                    fg = over ? Theme.text() : Theme.muted();
            }
            if (!isEnabled()) { fill = Theme.alpha(fill, 110); fg = Theme.alpha(fg, 130); }
            int w = getWidth(), h = getHeight();
            g2.setColor(fill);
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 12, 12));
            if (line != null) {
                g2.setColor(line);
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 12, 12));
            }
            g2.setFont(getFont());
            g2.setColor(fg);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(getText(), (w - fm.stringWidth(getText())) / 2,
                    (h - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    // ------------------------------------------------------------ Text field
    public static class Field extends JTextField {
        private final String hint;
        public Field(String hint) {
            this.hint = hint;
            setOpaque(false);
            setFont(Theme.font(Font.PLAIN, 13.5f));
            setBorder(BorderFactory.createEmptyBorder(9, 12, 9, 12));
            addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { repaint(); }
                @Override public void focusLost(FocusEvent e) { repaint(); }
            });
        }
        @Override public Color getForeground()        { return Theme.text(); }
        @Override public Color getCaretColor()        { return Theme.text(); }
        @Override public Color getSelectionColor()    { return Theme.alpha(Theme.primary(), 90); }
        @Override public Color getSelectedTextColor() { return Theme.text(); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            int w = getWidth(), h = getHeight();
            g2.setColor(Theme.field());
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 10, 10));
            boolean focus = hasFocus();
            g2.setColor(focus ? Theme.primary() : Theme.fieldBorder());
            g2.setStroke(new BasicStroke(focus ? 2f : 1f));
            float o = focus ? 1f : 0.5f;
            g2.draw(new RoundRectangle2D.Float(o, o, w - 2 * o, h - 2 * o, 10, 10));
            g2.dispose();
            super.paintComponent(g);
            if (hint != null && getText().isEmpty()) {
                Graphics2D g3 = aa(g);
                g3.setFont(getFont());
                g3.setColor(Theme.alpha(Theme.muted(), 190));
                FontMetrics fm = g3.getFontMetrics();
                g3.drawString(hint, getInsets().left, (h - fm.getHeight()) / 2 + fm.getAscent());
                g3.dispose();
            }
        }
    }

    // ------------------------------------------------------------ ComboBox
    public static class ComboBox<T> extends JComboBox<T> {
        public ComboBox(T[] items) {
            super(items);
            setFont(Theme.font(Font.PLAIN, 13f));
            setBackground(Theme.field());
            setForeground(Theme.text());
            setOpaque(false);
            setPreferredSize(new Dimension(getPreferredSize().width, 38));
            setUI(new BasicComboBoxUI() {
                @Override protected JButton createArrowButton() {
                    JButton b = new JButton() {
                        @Override protected void paintComponent(Graphics g) {
                            Graphics2D g2 = aa(g);
                            g2.setColor(Theme.muted());
                            int cx = getWidth() / 2, cy = getHeight() / 2;
                            int[] x = {cx - 4, cx + 4, cx};
                            int[] y = {cy - 2, cy - 2, cy + 3};
                            g2.fillPolygon(x, y, 3);
                            g2.dispose();
                        }
                    };
                    b.setBorderPainted(false);
                    b.setContentAreaFilled(false);
                    b.setOpaque(false);
                    b.setFocusable(false);
                    return b;
                }
                @Override public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
                    // avoid standard harsh default highlight
                }
            });
            setRenderer(new DefaultListCellRenderer() {
                @Override public Component getListCellRendererComponent(JList<?> list, Object value,
                                                                        int index, boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
                    setBackground(isSelected ? Theme.tableSelect() : Theme.card());
                    setForeground(Theme.text());
                    return this;
                }
            });
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            int w = getWidth(), h = getHeight();
            g2.setColor(Theme.field());
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 10, 10));
            g2.setColor(hasFocus() ? Theme.primary() : Theme.fieldBorder());
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 10, 10));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ------------------------------------------------------------ Card
    public static class Card extends JPanel {
        public static final int SHADOW = 4;
        public Card(LayoutManager lm, int pad) {
            super(lm);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(pad + SHADOW, pad + SHADOW, pad + SHADOW + 2, pad + SHADOW));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            int s = SHADOW, w = getWidth(), h = getHeight();
            if (!Theme.dark) {
                for (int i = s; i >= 1; i--) {
                    g2.setColor(new Color(15, 23, 42, 3));
                    g2.fill(new RoundRectangle2D.Float(s - i, s - i + 2, w - 2 * (s - i), h - 2 * (s - i) - 2,
                            16 + 2 * i, 16 + 2 * i));
                }
            }
            g2.setColor(Theme.card());
            g2.fill(new RoundRectangle2D.Float(s, s, w - 2 * s, h - 2 * s - 2, 16, 16));
            g2.setColor(Theme.border());
            g2.draw(new RoundRectangle2D.Float(s + 0.5f, s + 0.5f, w - 2 * s - 1, h - 2 * s - 3, 16, 16));
            g2.dispose();
        }
    }

    // ------------------------------------------------------------ Stat card
    public static class StatCard extends Card {
        private final Color accent;
        private final Label value;
        public StatCard(String title, Color accent) {
            super(new BorderLayout(), 12);
            this.accent = accent;
            value = new Label("0", Tone.TEXT, Font.BOLD, 26f);
            JPanel box = new JPanel(new BorderLayout(0, 2));
            box.setOpaque(false);
            box.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 0));
            box.add(new Label(title, Tone.MUTED, Font.PLAIN, 12.5f), BorderLayout.NORTH);
            box.add(value, BorderLayout.CENTER);
            add(box, BorderLayout.CENTER);
        }
        public void setValue(int v) { value.setText(String.valueOf(v)); }
        public void setValue(String v) { value.setText(v); }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = aa(g);
            int top = SHADOW + 12;
            g2.setColor(accent);
            g2.fill(new RoundRectangle2D.Float(top, top, 5, getHeight() - 2 * top - 2, 5, 5));
            g2.dispose();
        }
    }

    // ------------------------------------------------------------ Scrollbar
    public static class ScrollUI extends BasicScrollBarUI {
        @Override protected void configureScrollBarColors() {}
        private JButton zero() {
            JButton b = new JButton();
            Dimension d = new Dimension(0, 0);
            b.setPreferredSize(d); b.setMinimumSize(d); b.setMaximumSize(d);
            return b;
        }
        @Override protected JButton createDecreaseButton(int o) { return zero(); }
        @Override protected JButton createIncreaseButton(int o) { return zero(); }
        @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) {}
        @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            if (r.isEmpty()) return;
            Graphics2D g2 = aa(g);
            g2.setColor(Theme.alpha(Theme.muted(), isThumbRollover() ? 170 : 105));
            int arc = Math.min(r.width, r.height) - 4;
            g2.fill(new RoundRectangle2D.Float(r.x + 2, r.y + 2, r.width - 4, r.height - 4, arc, arc));
            g2.dispose();
        }
        @Override public Dimension getPreferredSize(JComponent c) { return new Dimension(10, 10); }
    }

    // ------------------------------------------------------------ Confirm dialog
    public static boolean confirm(Component parent, String title, String message, String yesText) {
        Window owner = SwingUtilities.getWindowAncestor(parent);
        JDialog d = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
        boolean[] result = {false};

        JPanel root = new JPanel(new BorderLayout(0, 18)) {
            @Override public Color getBackground() { return Theme.card(); }
        };
        root.setBorder(BorderFactory.createEmptyBorder(24, 26, 20, 26));
        root.add(new Label(title, Tone.TEXT, Font.BOLD, 17f), BorderLayout.NORTH);
        root.add(new Label("<html><body style='width:280px'>" + message + "</body></html>",
                Tone.MUTED, Font.PLAIN, 13.5f), BorderLayout.CENTER);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        row.setOpaque(false);
        Button no = new Button("Cancel", Kind.SECONDARY);
        Button yes = new Button(yesText, Kind.DANGER);
        no.addActionListener(e -> d.dispose());
        yes.addActionListener(e -> { result[0] = true; d.dispose(); });
        row.add(no); row.add(yes);
        root.add(row, BorderLayout.SOUTH);

        d.setContentPane(root);
        d.getRootPane().setDefaultButton(yes);
        d.getRootPane().registerKeyboardAction(e -> d.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        d.setResizable(false);
        d.pack();
        d.setLocationRelativeTo(parent);
        d.setVisible(true);
        return result[0];
    }
}
