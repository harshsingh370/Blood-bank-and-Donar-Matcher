package src;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Hospital Blood Bank Inventory & Donor Matcher Application.
 * Tracks donated blood units by ABO/Rh group, component (RBC, Platelets, Plasma),
 * expiration dates, and automatically matches emergency hospital requests with
 * compatible blood units and on-call donors.
 */
public class BloodBankManager extends JFrame {

    private final BloodBankDAO dao = new BloodBankDAO();

    // Stat Cards
    private Ui.StatCard totalUnitsCard;
    private Ui.StatCard criticalReqsCard;
    private Ui.StatCard expiringSoonCard;
    private Ui.StatCard eligibleDonorsCard;

    // Navigation Tabs / CardLayout
    private JPanel tabContentPanel;
    private CardLayout tabCardLayout;
    private Ui.Button tabInvBtn, tabReqBtn, tabDonorBtn;

    // Status & Header
    private JLabel statusLabel;
    private JLabel dbStatusBadge;
    private Ui.Button themeButton;
    private int statusKind = 0; // 0 neutral, 1 success, 2 error
    private final Timer toastTimer = new Timer(4000, e -> setStatus("System Ready", 0));

    // ==================== TAB 1: INVENTORY COMPONENTS ====================
    private DefaultTableModel unitTableModel;
    private JTable unitTable;
    private TableRowSorter<DefaultTableModel> unitSorter;
    private Ui.Field unitCodeField, volumeField, donationDateField, expirationDateField, storageField, unitSearchField;
    private Ui.ComboBox<String> unitBloodTypeCombo, unitComponentCombo, unitStatusCombo;
    private Ui.ComboBox<String> donorLinkCombo;
    private Ui.ComboBox<String> filterGroupCombo, filterCompCombo, filterStatusCombo;
    private Ui.Label unitCountLabel;
    private int unitHoverRow = -1;

    // ==================== TAB 2: EMERGENCY REQUESTS & MATCHER ============
    private DefaultTableModel reqTableModel;
    private JTable reqTable;
    private TableRowSorter<DefaultTableModel> reqSorter;
    private Ui.Field patientField, hospitalField, unitsNeededField, reqDateField, reqNotesField;
    private Ui.ComboBox<String> reqBloodTypeCombo, reqComponentCombo, reqUrgencyCombo, reqStatusCombo;
    private Ui.Label reqCountLabel;
    private int reqHoverRow = -1;

    // Smart Matcher panels
    private JPanel matcherContainer;
    private DefaultTableModel matchedUnitsModel;
    private JTable matchedUnitsTable;
    private DefaultTableModel matchedDonorsModel;
    private JTable matchedDonorsTable;
    private JLabel matchTitleLabel;
    private EmergencyRequest selectedRequest = null;

    // ==================== TAB 3: DONORS DIRECTORY ========================
    private DefaultTableModel donorTableModel;
    private JTable donorTable;
    private TableRowSorter<DefaultTableModel> donorSorter;
    private Ui.Field donorNameField, donorAgeField, donorPhoneField, donorEmailField, donorCityField, donorLastDateField, donorSearchField;
    private Ui.ComboBox<String> donorBloodTypeCombo, donorGenderCombo, donorStatusCombo;
    private Ui.Label donorCountLabel;
    private int donorHoverRow = -1;

    public BloodBankManager() {
        super("Hospital Blood Bank & Emergency Donor Matcher (MySQL)");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 720));
        setSize(1240, 800);
        setLocationRelativeTo(null);
        toastTimer.setRepeats(false);

        setContentPane(buildRoot());
        refreshAllData();
    }

    // ========================================================================
    // ROOT LAYOUT
    // ========================================================================
    private JPanel buildRoot() {
        JPanel root = new JPanel(new BorderLayout()) {
            @Override public Color getBackground() { return Theme.bg(); }
        };

        root.add(buildHeader(), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(12, 20, 0, 20));

        // Stats Row
        JPanel stats = new JPanel(new GridLayout(1, 4, 10, 0));
        stats.setOpaque(false);
        totalUnitsCard = new Ui.StatCard("Total Blood Units", Theme.primary());
        criticalReqsCard = new Ui.StatCard("Critical Emergencies", Theme.danger());
        expiringSoonCard = new Ui.StatCard("Expiring Soon (≤ 7d)", Theme.warning());
        eligibleDonorsCard = new Ui.StatCard("Eligible Donors", Theme.success());
        stats.add(totalUnitsCard);
        stats.add(criticalReqsCard);
        stats.add(expiringSoonCard);
        stats.add(eligibleDonorsCard);
        body.add(stats, BorderLayout.NORTH);

        // Center: Tab Navigation + Content Panel
        JPanel centerPanel = new JPanel(new BorderLayout(0, 8));
        centerPanel.setOpaque(false);
        centerPanel.add(buildTabBar(), BorderLayout.NORTH);

        tabCardLayout = new CardLayout();
        tabContentPanel = new JPanel(tabCardLayout);
        tabContentPanel.setOpaque(false);

        tabContentPanel.add(buildInventoryTab(), "INVENTORY");
        tabContentPanel.add(buildRequestsTab(), "REQUESTS");
        tabContentPanel.add(buildDonorsTab(), "DONORS");

        centerPanel.add(tabContentPanel, BorderLayout.CENTER);
        body.add(centerPanel, BorderLayout.CENTER);

        root.add(body, BorderLayout.CENTER);

        // Status Bar at Bottom
        statusLabel = new JLabel("System Ready") {
            @Override public Color getForeground() {
                return statusKind == 1 ? Theme.success() : statusKind == 2 ? Theme.danger() : Theme.muted();
            }
        };
        statusLabel.setFont(Theme.font(Font.PLAIN, 13f));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 24, 10, 24));
        root.add(statusLabel, BorderLayout.SOUTH);

        return root;
    }

    // ========================================================================
    // HEADER BANNER
    // ========================================================================
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = Ui.aa(g);
                int w = getWidth(), h = getHeight();
                g2.setPaint(new GradientPaint(0, 0, Theme.headerA(), w, h, Theme.headerB()));
                g2.fillRect(0, 0, w, h);
                // Subtle decorative background circles
                g2.setColor(new Color(255, 255, 255, 16));
                g2.fillOval(w - 260, -90, 300, 300);
                g2.fillOval(w - 420, 20, 150, 150);
                g2.dispose();
            }
        };
        header.setBorder(BorderFactory.createEmptyBorder(18, 28, 18, 28));

        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 3));
        titles.setOpaque(false);
        titles.add(new Ui.Label("HOSPITAL BLOOD BANK & DONOR MATCHER", Ui.Tone.WHITE, Font.BOLD, 23f));
        titles.add(new Ui.Label("Inventory Tracking (ABO/Rh, RBC, Platelets, Plasma) & Emergency Transfusion Matcher",
                Ui.Tone.SOFT, Font.PLAIN, 13.5f));
        header.add(titles, BorderLayout.WEST);

        // Right side: DB config, connection status, Theme toggle
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        dbStatusBadge = new JLabel("● MySQL Checking...") {
            @Override public Color getForeground() { return Color.WHITE; }
        };
        dbStatusBadge.setFont(Theme.font(Font.BOLD, 12f));

        Ui.Button dbConfigBtn = new Ui.Button("Database Config", Ui.Kind.HEADER);
        dbConfigBtn.addActionListener(e -> showDatabaseConfigDialog());

        themeButton = new Ui.Button(Theme.dark ? "Light mode" : "Dark mode", Ui.Kind.HEADER);
        themeButton.addActionListener(e -> {
            Theme.dark = !Theme.dark;
            themeButton.setText(Theme.dark ? "Light mode" : "Dark mode");
            getContentPane().repaint();
        });

        right.add(dbStatusBadge);
        right.add(dbConfigBtn);
        right.add(themeButton);
        header.add(right, BorderLayout.EAST);

        return header;
    }

    // ========================================================================
    // TAB BAR
    // ========================================================================
    private JPanel buildTabBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);

        tabInvBtn = new Ui.Button("Blood Inventory", Ui.Kind.PRIMARY);
        tabReqBtn = new Ui.Button("Emergency Requests & Matcher", Ui.Kind.SECONDARY);
        tabDonorBtn = new Ui.Button("Donors Directory", Ui.Kind.SECONDARY);

        tabInvBtn.addActionListener(e -> selectTab("INVENTORY", tabInvBtn));
        tabReqBtn.addActionListener(e -> selectTab("REQUESTS", tabReqBtn));
        tabDonorBtn.addActionListener(e -> selectTab("DONORS", tabDonorBtn));

        bar.add(tabInvBtn);
        bar.add(tabReqBtn);
        bar.add(tabDonorBtn);

        return bar;
    }

    private void selectTab(String name, Ui.Button activeBtn) {
        tabCardLayout.show(tabContentPanel, name);
        // Reset button kinds
        tabInvBtn.setForeground(Theme.text());
        tabReqBtn.setForeground(Theme.text());
        tabDonorBtn.setForeground(Theme.text());

        // Update look
        repaint();
    }

    // ========================================================================
    // TAB 1: BLOOD INVENTORY
    // ========================================================================
    private JPanel buildInventoryTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);

        // Left: Form Card
        Ui.Card form = new Ui.Card(new GridBagLayout(), 16);
        form.setPreferredSize(new Dimension(360, 0));

        unitCodeField = new Ui.Field("e.g. BLD-2026-0101");
        donorLinkCombo = new Ui.ComboBox<>(new String[]{"None / Anonymous"});
        unitBloodTypeCombo = new Ui.ComboBox<>(CompatibilityEngine.BLOOD_TYPES);
        unitComponentCombo = new Ui.ComboBox<>(CompatibilityEngine.COMPONENTS);
        volumeField = new Ui.Field("450");
        donationDateField = new Ui.Field("YYYY-MM-DD");
        expirationDateField = new Ui.Field("YYYY-MM-DD");
        storageField = new Ui.Field("e.g. Cold Room Shelf A1");
        unitStatusCombo = new Ui.ComboBox<>(new String[]{"Available", "Reserved", "Transfused", "Expired"});

        // Auto-compute expiration date when donation date or component changes
        ItemListener dateListener = e -> calculateExpiryDate();
        unitComponentCombo.addItemListener(dateListener);
        donationDateField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { calculateExpiryDate(); }
            public void removeUpdate(DocumentEvent e) { calculateExpiryDate(); }
            public void changedUpdate(DocumentEvent e) { calculateExpiryDate(); }
        });

        Ui.Button addBtn = new Ui.Button("Add Blood Unit", Ui.Kind.PRIMARY);
        Ui.Button updateBtn = new Ui.Button("Update", Ui.Kind.SECONDARY);
        Ui.Button transfusedBtn = new Ui.Button("Mark Transfused", Ui.Kind.SUCCESS);
        Ui.Button deleteBtn = new Ui.Button("Delete", Ui.Kind.DANGER);
        Ui.Button clearBtn = new Ui.Button("Clear Form", Ui.Kind.GHOST);

        addBtn.addActionListener(e -> addBloodUnit());
        updateBtn.addActionListener(e -> updateBloodUnit());
        transfusedBtn.addActionListener(e -> markUnitTransfused());
        deleteBtn.addActionListener(e -> deleteBloodUnit());
        clearBtn.addActionListener(e -> clearUnitForm());

        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.NORTHWEST;
        int y = 0;

        g.gridy = y++; form.add(new Ui.Label("Register Blood Unit", Ui.Tone.TEXT, Font.BOLD, 17f), g);
        g.gridy = y++; g.insets = new Insets(2, 0, 8, 0);
        form.add(new Ui.Label("Track unit by ABO/Rh, component & shelf-life", Ui.Tone.MUTED, Font.PLAIN, 12f), g);

        String[] labels = {"Unit Code", "Blood Type (ABO/Rh)", "Component", "Volume (mL)",
                "Donation Date", "Expiration Date", "Storage Location", "Status"};
        JComponent[] inputs = {unitCodeField, unitBloodTypeCombo, unitComponentCombo, volumeField,
                donationDateField, expirationDateField, storageField, unitStatusCombo};

        for (int i = 0; i < labels.length; i++) {
            g.gridy = y++; g.insets = new Insets(4, 0, 2, 0);
            form.add(new Ui.Label(labels[i], Ui.Tone.MUTED, Font.BOLD, 11.5f), g);
            g.gridy = y++; g.insets = new Insets(0, 0, 2, 0);
            form.add(inputs[i], g);
        }

        g.gridy = y++; g.insets = new Insets(12, 0, 0, 0);
        form.add(addBtn, g);

        JPanel row2 = new JPanel(new GridLayout(1, 2, 6, 0));
        row2.setOpaque(false);
        row2.add(updateBtn);
        row2.add(transfusedBtn);
        g.gridy = y++; g.insets = new Insets(6, 0, 0, 0);
        form.add(row2, g);

        JPanel row3 = new JPanel(new GridLayout(1, 2, 6, 0));
        row3.setOpaque(false);
        row3.add(deleteBtn);
        row3.add(clearBtn);
        g.gridy = y++; g.insets = new Insets(6, 0, 0, 0);
        form.add(row3, g);

        g.gridy = y; g.weighty = 1; g.fill = GridBagConstraints.BOTH;
        form.add(Box.createGlue(), g);

        panel.add(form, BorderLayout.WEST);

        // Right: Table Card with filters and search
        Ui.Card card = new Ui.Card(new BorderLayout(0, 10), 16);

        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setOpaque(false);

        JPanel titleBox = new JPanel(new GridLayout(2, 1, 0, 1));
        titleBox.setOpaque(false);
        titleBox.add(new Ui.Label("Blood Inventory Units", Ui.Tone.TEXT, Font.BOLD, 17f));
        unitCountLabel = new Ui.Label("0 units", Ui.Tone.MUTED, Font.PLAIN, 12f);
        titleBox.add(unitCountLabel);
        top.add(titleBox, BorderLayout.WEST);

        // Filter Dropdowns
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        filterBar.setOpaque(false);

        filterGroupCombo = new Ui.ComboBox<>(new String[]{"All Groups", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"});
        filterCompCombo = new Ui.ComboBox<>(new String[]{"All Components", "Red Blood Cells (RBC)", "Platelets", "Plasma"});
        filterStatusCombo = new Ui.ComboBox<>(new String[]{"All Statuses", "Available", "Expiring Soon", "Expired", "Transfused"});

        ActionListener filterListener = e -> applyUnitFilter();
        filterGroupCombo.addActionListener(filterListener);
        filterCompCombo.addActionListener(filterListener);
        filterStatusCombo.addActionListener(filterListener);

        unitSearchField = new Ui.Field("Search units...");
        unitSearchField.setPreferredSize(new Dimension(150, 36));
        unitSearchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applyUnitFilter(); }
            public void removeUpdate(DocumentEvent e) { applyUnitFilter(); }
            public void changedUpdate(DocumentEvent e) { applyUnitFilter(); }
        });

        Ui.Button refreshBtn = new Ui.Button("Refresh", Ui.Kind.SECONDARY);
        refreshBtn.addActionListener(e -> { refreshAllData(); setStatus("Inventory refreshed", 1); });

        filterBar.add(new Ui.Label("Filter:", Ui.Tone.MUTED, Font.PLAIN, 12f));
        filterBar.add(filterGroupCombo);
        filterBar.add(filterCompCombo);
        filterBar.add(filterStatusCombo);
        filterBar.add(unitSearchField);
        filterBar.add(refreshBtn);

        top.add(filterBar, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);

        // Table
        String[] cols = {"ID", "Unit Code", "Group", "Component", "Vol (mL)", "Donation Date", "Expiration", "Location", "Status"};
        unitTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) {
                return (c == 0 || c == 4) ? Integer.class : String.class;
            }
        };

        unitTable = createStyledTable(unitTableModel, new int[]{45, 110, 65, 140, 70, 95, 95, 130, 110});
        unitSorter = new TableRowSorter<>(unitTableModel);
        unitTable.setRowSorter(unitSorter);

        unitTable.getColumnModel().getColumn(2).setCellRenderer(new BloodTypeBadgeRenderer());
        unitTable.getColumnModel().getColumn(8).setCellRenderer(new FreshnessStatusRenderer());

        unitTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int v = unitTable.getSelectedRow();
            if (v == -1) return;
            int r = unitTable.convertRowIndexToModel(v);

            unitCodeField.setText(str(unitTableModel.getValueAt(r, 1)));
            unitBloodTypeCombo.setSelectedItem(str(unitTableModel.getValueAt(r, 2)));
            unitComponentCombo.setSelectedItem(str(unitTableModel.getValueAt(r, 3)));
            volumeField.setText(str(unitTableModel.getValueAt(r, 4)));
            donationDateField.setText(str(unitTableModel.getValueAt(r, 5)));
            expirationDateField.setText(str(unitTableModel.getValueAt(r, 6)));
            storageField.setText(str(unitTableModel.getValueAt(r, 7)));

            String st = str(unitTableModel.getValueAt(r, 8));
            if (st.equals("Expiring Soon")) st = "Available";
            unitStatusCombo.setSelectedItem(st);
        });

        JScrollPane sp = createStyledScrollPane(unitTable);
        card.add(sp, BorderLayout.CENTER);

        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    // ========================================================================
    // TAB 2: EMERGENCY REQUESTS & SMART MATCHER
    // ========================================================================
    private JPanel buildRequestsTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);

        // Left Form: Create Emergency Request
        Ui.Card form = new Ui.Card(new GridBagLayout(), 16);
        form.setPreferredSize(new Dimension(340, 0));

        patientField = new Ui.Field("e.g. Jane Doe");
        hospitalField = new Ui.Field("e.g. City General ICU");
        reqBloodTypeCombo = new Ui.ComboBox<>(CompatibilityEngine.BLOOD_TYPES);
        reqComponentCombo = new Ui.ComboBox<>(CompatibilityEngine.COMPONENTS);
        unitsNeededField = new Ui.Field("1");
        reqUrgencyCombo = new Ui.ComboBox<>(CompatibilityEngine.URGENCIES);
        reqDateField = new Ui.Field(LocalDate.now().toString());
        reqStatusCombo = new Ui.ComboBox<>(new String[]{"Pending", "Matched", "Fulfilled", "Cancelled"});
        reqNotesField = new Ui.Field("Clinical notes & diagnosis");

        Ui.Button addReqBtn = new Ui.Button("Create Request", Ui.Kind.DANGER);
        Ui.Button updateReqBtn = new Ui.Button("Update", Ui.Kind.SECONDARY);
        Ui.Button fulfillReqBtn = new Ui.Button("Mark Fulfilled", Ui.Kind.SUCCESS);
        Ui.Button deleteReqBtn = new Ui.Button("Delete", Ui.Kind.DANGER);
        Ui.Button clearReqBtn = new Ui.Button("Clear Form", Ui.Kind.GHOST);

        addReqBtn.addActionListener(e -> addEmergencyRequest());
        updateReqBtn.addActionListener(e -> updateEmergencyRequest());
        fulfillReqBtn.addActionListener(e -> markRequestFulfilled());
        deleteReqBtn.addActionListener(e -> deleteEmergencyRequest());
        clearReqBtn.addActionListener(e -> clearReqForm());

        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.NORTHWEST;
        int y = 0;

        g.gridy = y++; form.add(new Ui.Label("Emergency Transfusion Request", Ui.Tone.TEXT, Font.BOLD, 16.5f), g);
        g.gridy = y++; g.insets = new Insets(2, 0, 8, 0);
        form.add(new Ui.Label("Select a request below to auto-match blood units & donors", Ui.Tone.MUTED, Font.PLAIN, 11.5f), g);

        String[] labels = {"Patient Name", "Hospital / Unit", "Needed Blood Group", "Needed Component",
                "Units Needed", "Urgency Level", "Request Date", "Status", "Clinical Notes"};
        JComponent[] inputs = {patientField, hospitalField, reqBloodTypeCombo, reqComponentCombo,
                unitsNeededField, reqUrgencyCombo, reqDateField, reqStatusCombo, reqNotesField};

        for (int i = 0; i < labels.length; i++) {
            g.gridy = y++; g.insets = new Insets(4, 0, 2, 0);
            form.add(new Ui.Label(labels[i], Ui.Tone.MUTED, Font.BOLD, 11.5f), g);
            g.gridy = y++; g.insets = new Insets(0, 0, 2, 0);
            form.add(inputs[i], g);
        }

        g.gridy = y++; g.insets = new Insets(12, 0, 0, 0);
        form.add(addReqBtn, g);

        JPanel row2 = new JPanel(new GridLayout(1, 2, 6, 0));
        row2.setOpaque(false);
        row2.add(updateReqBtn);
        row2.add(fulfillReqBtn);
        g.gridy = y++; g.insets = new Insets(6, 0, 0, 0);
        form.add(row2, g);

        JPanel row3 = new JPanel(new GridLayout(1, 2, 6, 0));
        row3.setOpaque(false);
        row3.add(deleteReqBtn);
        row3.add(clearReqBtn);
        g.gridy = y++; g.insets = new Insets(6, 0, 0, 0);
        form.add(row3, g);

        g.gridy = y; g.weighty = 1; g.fill = GridBagConstraints.BOTH;
        form.add(Box.createGlue(), g);

        panel.add(form, BorderLayout.WEST);

        // Center Split: Upper = Emergency Requests Table, Lower = Smart Matcher Results
        JPanel centerSplit = new JPanel(new GridLayout(2, 1, 0, 10));
        centerSplit.setOpaque(false);

        // Top Card: Requests Table
        Ui.Card reqCard = new Ui.Card(new BorderLayout(0, 8), 14);
        JPanel topReq = new JPanel(new BorderLayout());
        topReq.setOpaque(false);

        JPanel titleBox = new JPanel(new GridLayout(2, 1, 0, 1));
        titleBox.setOpaque(false);
        titleBox.add(new Ui.Label("Emergency Hospital Requests", Ui.Tone.TEXT, Font.BOLD, 16.5f));
        reqCountLabel = new Ui.Label("Pick a row to trigger smart compatibility matching", Ui.Tone.MUTED, Font.PLAIN, 12f);
        titleBox.add(reqCountLabel);
        topReq.add(titleBox, BorderLayout.WEST);

        reqCard.add(topReq, BorderLayout.NORTH);

        String[] reqCols = {"ID", "Patient", "Hospital", "Req Group", "Component", "Units", "Urgency", "Date", "Status"};
        reqTableModel = new DefaultTableModel(reqCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) {
                return (c == 0 || c == 5) ? Integer.class : String.class;
            }
        };

        reqTable = createStyledTable(reqTableModel, new int[]{45, 120, 140, 75, 140, 50, 95, 90, 85});
        reqSorter = new TableRowSorter<>(reqTableModel);
        reqTable.setRowSorter(reqSorter);

        reqTable.getColumnModel().getColumn(3).setCellRenderer(new BloodTypeBadgeRenderer());
        reqTable.getColumnModel().getColumn(6).setCellRenderer(new UrgencyBadgeRenderer());

        reqTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int v = reqTable.getSelectedRow();
            if (v == -1) {
                selectedRequest = null;
                updateSmartMatcher();
                return;
            }
            int r = reqTable.convertRowIndexToModel(v);
            int id = (int) reqTableModel.getValueAt(r, 0);

            patientField.setText(str(reqTableModel.getValueAt(r, 1)));
            hospitalField.setText(str(reqTableModel.getValueAt(r, 2)));
            reqBloodTypeCombo.setSelectedItem(str(reqTableModel.getValueAt(r, 3)));
            reqComponentCombo.setSelectedItem(str(reqTableModel.getValueAt(r, 4)));
            unitsNeededField.setText(str(reqTableModel.getValueAt(r, 5)));
            reqUrgencyCombo.setSelectedItem(str(reqTableModel.getValueAt(r, 6)));
            reqDateField.setText(str(reqTableModel.getValueAt(r, 7)));
            reqStatusCombo.setSelectedItem(str(reqTableModel.getValueAt(r, 8)));

            selectedRequest = new EmergencyRequest(id,
                    patientField.getText(), hospitalField.getText(),
                    (String) reqBloodTypeCombo.getSelectedItem(),
                    (String) reqComponentCombo.getSelectedItem(),
                    Integer.parseInt(unitsNeededField.getText().trim()),
                    (String) reqUrgencyCombo.getSelectedItem(),
                    reqDateField.getText(), (String) reqStatusCombo.getSelectedItem(), "");

            updateSmartMatcher();
        });

        JScrollPane reqSp = createStyledScrollPane(reqTable);
        reqCard.add(reqSp, BorderLayout.CENTER);
        centerSplit.add(reqCard);

        // Bottom Card: SMART MATCHER RESULTS
        Ui.Card matchCard = new Ui.Card(new BorderLayout(0, 8), 14);

        JPanel matchHeader = new JPanel(new BorderLayout());
        matchHeader.setOpaque(false);
        matchTitleLabel = new JLabel("SMART MATCHER: Select an emergency request above to find compatible units & donors") {
            @Override public Color getForeground() { return Theme.primary(); }
        };
        matchTitleLabel.setFont(Theme.font(Font.BOLD, 14.5f));
        matchHeader.add(matchTitleLabel, BorderLayout.WEST);

        Ui.Button allocateActionBtn = new Ui.Button("Allocate Selected Unit to Request", Ui.Kind.PRIMARY);
        allocateActionBtn.addActionListener(e -> allocateSelectedUnit());
        matchHeader.add(allocateActionBtn, BorderLayout.EAST);
        matchCard.add(matchHeader, BorderLayout.NORTH);

        // Split inside match card: Left = In-Stock Matched Units, Right = Matched Donors On-Call
        JPanel matchSplit = new JPanel(new GridLayout(1, 2, 10, 0));
        matchSplit.setOpaque(false);

        // Matched Units Table
        JPanel matchedUnitsBox = new JPanel(new BorderLayout(0, 4));
        matchedUnitsBox.setOpaque(false);
        matchedUnitsBox.add(new Ui.Label("In-Stock Compatible Blood Units", Ui.Tone.TEXT, Font.BOLD, 13f), BorderLayout.NORTH);

        String[] mUnitCols = {"Unit ID", "Code", "Group", "Expiry", "Days Left", "Compatibility Reason"};
        matchedUnitsModel = new DefaultTableModel(mUnitCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        matchedUnitsTable = createStyledTable(matchedUnitsModel, new int[]{50, 95, 55, 85, 65, 170});
        matchedUnitsTable.getColumnModel().getColumn(2).setCellRenderer(new BloodTypeBadgeRenderer());
        matchedUnitsBox.add(createStyledScrollPane(matchedUnitsTable), BorderLayout.CENTER);
        matchSplit.add(matchedUnitsBox);

        // Matched Donors Table
        JPanel matchedDonorsBox = new JPanel(new BorderLayout(0, 4));
        matchedDonorsBox.setOpaque(false);
        matchedDonorsBox.add(new Ui.Label("Compatible Registered Donors (On Call)", Ui.Tone.TEXT, Font.BOLD, 13f), BorderLayout.NORTH);

        String[] mDonorCols = {"Donor", "Group", "Phone", "City", "Eligibility"};
        matchedDonorsModel = new DefaultTableModel(mDonorCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        matchedDonorsTable = createStyledTable(matchedDonorsModel, new int[]{110, 55, 110, 85, 90});
        matchedDonorsTable.getColumnModel().getColumn(1).setCellRenderer(new BloodTypeBadgeRenderer());
        matchedDonorsBox.add(createStyledScrollPane(matchedDonorsTable), BorderLayout.CENTER);
        matchSplit.add(matchedDonorsBox);

        matchCard.add(matchSplit, BorderLayout.CENTER);
        centerSplit.add(matchCard);

        panel.add(centerSplit, BorderLayout.CENTER);
        return panel;
    }

    // ========================================================================
    // TAB 3: DONORS DIRECTORY
    // ========================================================================
    private JPanel buildDonorsTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);

        // Left Form: Register Donor
        Ui.Card form = new Ui.Card(new GridBagLayout(), 16);
        form.setPreferredSize(new Dimension(340, 0));

        donorNameField = new Ui.Field("e.g. John Smith");
        donorBloodTypeCombo = new Ui.ComboBox<>(CompatibilityEngine.BLOOD_TYPES);
        donorGenderCombo = new Ui.ComboBox<>(new String[]{"Male", "Female", "Other"});
        donorAgeField = new Ui.Field("28");
        donorPhoneField = new Ui.Field("+1 (555) 000-0000");
        donorEmailField = new Ui.Field("john@example.com");
        donorCityField = new Ui.Field("Metro City");
        donorLastDateField = new Ui.Field("YYYY-MM-DD");
        donorStatusCombo = new Ui.ComboBox<>(new String[]{"Eligible", "Deferred", "Ineligible"});

        Ui.Button addDonorBtn = new Ui.Button("Register Donor", Ui.Kind.PRIMARY);
        Ui.Button updateDonorBtn = new Ui.Button("Update", Ui.Kind.SECONDARY);
        Ui.Button deleteDonorBtn = new Ui.Button("Delete", Ui.Kind.DANGER);
        Ui.Button clearDonorBtn = new Ui.Button("Clear Form", Ui.Kind.GHOST);

        addDonorBtn.addActionListener(e -> addDonor());
        updateDonorBtn.addActionListener(e -> updateDonor());
        deleteDonorBtn.addActionListener(e -> deleteDonor());
        clearDonorBtn.addActionListener(e -> clearDonorForm());

        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.NORTHWEST;
        int y = 0;

        g.gridy = y++; form.add(new Ui.Label("Volunteer Donor Registration", Ui.Tone.TEXT, Font.BOLD, 17f), g);
        g.gridy = y++; g.insets = new Insets(2, 0, 8, 0);
        form.add(new Ui.Label("Register volunteer donors & track eligibility intervals", Ui.Tone.MUTED, Font.PLAIN, 12f), g);

        String[] labels = {"Full Name", "Blood Group (ABO/Rh)", "Gender", "Age", "Phone Number",
                "Email Address", "City / Area", "Last Donation Date", "Status"};
        JComponent[] inputs = {donorNameField, donorBloodTypeCombo, donorGenderCombo, donorAgeField,
                donorPhoneField, donorEmailField, donorCityField, donorLastDateField, donorStatusCombo};

        for (int i = 0; i < labels.length; i++) {
            g.gridy = y++; g.insets = new Insets(4, 0, 2, 0);
            form.add(new Ui.Label(labels[i], Ui.Tone.MUTED, Font.BOLD, 11.5f), g);
            g.gridy = y++; g.insets = new Insets(0, 0, 2, 0);
            form.add(inputs[i], g);
        }

        g.gridy = y++; g.insets = new Insets(12, 0, 0, 0);
        form.add(addDonorBtn, g);

        JPanel row2 = new JPanel(new GridLayout(1, 2, 6, 0));
        row2.setOpaque(false);
        row2.add(updateDonorBtn);
        row2.add(deleteDonorBtn);
        g.gridy = y++; g.insets = new Insets(6, 0, 0, 0);
        form.add(row2, g);

        g.gridy = y++; g.insets = new Insets(6, 0, 0, 0);
        form.add(clearDonorBtn, g);

        g.gridy = y; g.weighty = 1; g.fill = GridBagConstraints.BOTH;
        form.add(Box.createGlue(), g);

        panel.add(form, BorderLayout.WEST);

        // Right Table Card: Donors Table
        Ui.Card card = new Ui.Card(new BorderLayout(0, 10), 16);

        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setOpaque(false);

        JPanel titleBox = new JPanel(new GridLayout(2, 1, 0, 1));
        titleBox.setOpaque(false);
        titleBox.add(new Ui.Label("Registered Donors Directory", Ui.Tone.TEXT, Font.BOLD, 17f));
        donorCountLabel = new Ui.Label("0 donors", Ui.Tone.MUTED, Font.PLAIN, 12f);
        titleBox.add(donorCountLabel);
        top.add(titleBox, BorderLayout.WEST);

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        filterBar.setOpaque(false);

        donorSearchField = new Ui.Field("Search donors by name, blood group, city...");
        donorSearchField.setPreferredSize(new Dimension(280, 36));
        donorSearchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applyDonorFilter(); }
            public void removeUpdate(DocumentEvent e) { applyDonorFilter(); }
            public void changedUpdate(DocumentEvent e) { applyDonorFilter(); }
        });

        Ui.Button refreshBtn = new Ui.Button("Refresh", Ui.Kind.SECONDARY);
        refreshBtn.addActionListener(e -> { refreshAllData(); setStatus("Donors refreshed", 1); });

        filterBar.add(donorSearchField);
        filterBar.add(refreshBtn);
        top.add(filterBar, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);

        String[] donorCols = {"ID", "Full Name", "Group", "Age/Gender", "Phone", "Email", "City", "Last Donated", "Eligibility"};
        donorTableModel = new DefaultTableModel(donorCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) {
                return c == 0 ? Integer.class : String.class;
            }
        };

        donorTable = createStyledTable(donorTableModel, new int[]{45, 140, 65, 85, 115, 140, 95, 95, 110});
        donorSorter = new TableRowSorter<>(donorTableModel);
        donorTable.setRowSorter(donorSorter);

        donorTable.getColumnModel().getColumn(2).setCellRenderer(new BloodTypeBadgeRenderer());
        donorTable.getColumnModel().getColumn(8).setCellRenderer(new DonorEligibilityRenderer());

        donorTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int v = donorTable.getSelectedRow();
            if (v == -1) return;
            int r = donorTable.convertRowIndexToModel(v);

            donorNameField.setText(str(donorTableModel.getValueAt(r, 1)));
            donorBloodTypeCombo.setSelectedItem(str(donorTableModel.getValueAt(r, 2)));

            String[] ag = str(donorTableModel.getValueAt(r, 3)).split(" / ");
            if (ag.length == 2) {
                donorAgeField.setText(ag[0].replace("y", "").trim());
                donorGenderCombo.setSelectedItem(ag[1].trim());
            }

            donorPhoneField.setText(str(donorTableModel.getValueAt(r, 4)));
            donorEmailField.setText(str(donorTableModel.getValueAt(r, 5)));
            donorCityField.setText(str(donorTableModel.getValueAt(r, 6)));
            donorLastDateField.setText(str(donorTableModel.getValueAt(r, 7)));

            String el = str(donorTableModel.getValueAt(r, 8));
            donorStatusCombo.setSelectedItem(el.equals("Deferred") ? "Deferred" : "Eligible");
        });

        card.add(createStyledScrollPane(donorTable), BorderLayout.CENTER);
        panel.add(card, BorderLayout.CENTER);

        return panel;
    }

    // ========================================================================
    // DATA ACTIONS & DATABASE OPERATIONS
    // ========================================================================
    private void refreshAllData() {
        checkDbStatus();
        loadInventoryData();
        loadRequestsData();
        loadDonorsData();
        updateSmartMatcher();
    }

    private void checkDbStatus() {
        new Thread(() -> {
            boolean ok = DBConnection.testConnection(
                    DBConnection.getDbHost(), DBConnection.getDbPort(),
                    DBConnection.getDbName(), DBConnection.getDbUser(), DBConnection.getDbPassword());
            SwingUtilities.invokeLater(() -> {
                if (ok) {
                    dbStatusBadge.setText("● MySQL Connected (" + DBConnection.getDbName() + ")");
                    dbStatusBadge.setForeground(new Color(0x86EFAC)); // light green
                } else {
                    dbStatusBadge.setText("● MySQL Disconnected");
                    dbStatusBadge.setForeground(new Color(0xFCA5A5)); // light red
                }
            });
        }).start();
    }

    private void loadInventoryData() {
        unitTableModel.setRowCount(0);
        List<BloodUnit> units = dao.getAllUnits();
        int expiringSoon = 0;

        for (BloodUnit u : units) {
            String status = u.getDisplayStatus();
            if (u.isExpiringSoon()) expiringSoon++;

            unitTableModel.addRow(new Object[]{
                    u.getId(), u.getUnitCode(), u.getBloodType(), u.getComponent(),
                    u.getVolumeMl(), u.getDonationDate(), u.getExpirationDate(),
                    u.getStorageLocation(), status
            });
        }

        totalUnitsCard.setValue(units.size());
        expiringSoonCard.setValue(expiringSoon);
        unitCountLabel.setText(units.size() + (units.size() == 1 ? " unit in inventory" : " units in inventory"));
    }

    private void loadRequestsData() {
        reqTableModel.setRowCount(0);
        List<EmergencyRequest> reqs = dao.getAllRequests();
        int criticalCount = 0;

        for (EmergencyRequest r : reqs) {
            if ("CRITICAL".equalsIgnoreCase(r.getUrgencyLevel()) && !"Fulfilled".equalsIgnoreCase(r.getStatus())) {
                criticalCount++;
            }
            reqTableModel.addRow(new Object[]{
                    r.getId(), r.getPatientName(), r.getHospitalName(),
                    r.getRequiredBloodType(), r.getRequiredComponent(),
                    r.getUnitsNeeded(), r.getUrgencyLevel(), r.getRequestDate(), r.getStatus()
            });
        }

        criticalReqsCard.setValue(criticalCount);
        reqCountLabel.setText(reqs.size() + (reqs.size() == 1 ? " active request" : " active requests"));
    }

    private void loadDonorsData() {
        donorTableModel.setRowCount(0);
        donorLinkCombo.removeAllItems();
        donorLinkCombo.addItem("None / Anonymous");

        List<Donor> donors = dao.getAllDonors();
        int eligibleCount = 0;

        for (Donor d : donors) {
            boolean eligible = d.isEligibleNow();
            if (eligible) eligibleCount++;
            String elStatus = eligible ? "Eligible Now" : ("Deferred (" + d.getDaysSinceLastDonation() + "d ago)");

            donorTableModel.addRow(new Object[]{
                    d.getId(), d.getName(), d.getBloodType(),
                    d.getAge() + "y / " + d.getGender(),
                    d.getPhone(), d.getEmail(), d.getCity(),
                    d.getLastDonationDate(), elStatus
            });

            donorLinkCombo.addItem(d.getId() + ": " + d.getName() + " (" + d.getBloodType() + ")");
        }

        eligibleDonorsCard.setValue(eligibleCount);
        donorCountLabel.setText(donors.size() + (donors.size() == 1 ? " registered donor" : " registered donors"));
    }

    // ========================================================================
    // SMART MATCHER LOGIC
    // ========================================================================
    private void updateSmartMatcher() {
        matchedUnitsModel.setRowCount(0);
        matchedDonorsModel.setRowCount(0);

        if (selectedRequest == null) {
            matchTitleLabel.setText("SMART MATCHER: Select an emergency request above to find compatible units & donors");
            return;
        }

        String recGroup = selectedRequest.getRequiredBloodType();
        String recComp = selectedRequest.getRequiredComponent();

        matchTitleLabel.setText("MATCHING FOR: " + selectedRequest.getPatientName() +
                " [" + recGroup + " " + recComp + " (" + selectedRequest.getUrgencyLevel() + ")] - Compatible Options:");

        // 1. Scan available blood units
        List<BloodUnit> allUnits = dao.getAllUnits();
        int matchedUnitCount = 0;
        for (BloodUnit u : allUnits) {
            if ("Available".equalsIgnoreCase(u.getStatus()) && !u.isExpired()) {
                if (CompatibilityEngine.isCompatible(recGroup, recComp, u.getBloodType(), u.getComponent())) {
                    String reason = CompatibilityEngine.getMatchExplanation(recGroup, u.getBloodType(), recComp);
                    matchedUnitsModel.addRow(new Object[]{
                            u.getId(), u.getUnitCode(), u.getBloodType(),
                            u.getExpirationDate(), u.getDaysUntilExpiration() + " days", reason
                    });
                    matchedUnitCount++;
                }
            }
        }

        // 2. Scan compatible on-call donors
        List<Donor> allDonors = dao.getAllDonors();
        int matchedDonorCount = 0;
        for (Donor d : allDonors) {
            if (CompatibilityEngine.isBloodTypeCompatible(recGroup, d.getBloodType(), recComp)) {
                String elText = d.isEligibleNow() ? "Eligible to Donate" : "Interval: " + d.getDaysSinceLastDonation() + "d";
                matchedDonorsModel.addRow(new Object[]{
                        d.getName(), d.getBloodType(), d.getPhone(), d.getCity(), elText
                });
                matchedDonorCount++;
            }
        }

        if (matchedUnitCount == 0 && matchedDonorCount == 0) {
            setStatus("Alert: No immediate stock or donor found for " + recGroup + " " + recComp + "!", 2);
        }
    }

    private void allocateSelectedUnit() {
        if (selectedRequest == null) {
            setStatus("Please select an emergency request first!", 2);
            return;
        }
        int v = matchedUnitsTable.getSelectedRow();
        if (v == -1) {
            setStatus("Select a compatible blood unit from the table on the left first", 2);
            return;
        }
        int unitId = (int) matchedUnitsModel.getValueAt(v, 0);
        String code = str(matchedUnitsModel.getValueAt(v, 1));

        String confirmMsg = "Allocate unit <b>" + code + "</b> to emergency request #" +
                selectedRequest.getId() + " for patient <b>" + selectedRequest.getPatientName() + "</b>?";

        if (Ui.confirm(this, "Allocate Blood Unit", confirmMsg, "Allocate Unit")) {
            if (dao.allocateUnitToRequest(selectedRequest.getId(), unitId)) {
                setStatus("Success! Unit " + code + " allocated and reserved for patient " + selectedRequest.getPatientName(), 1);
                refreshAllData();
            } else {
                setStatus("Could not allocate blood unit (see database connection)", 2);
            }
        }
    }

    // ========================================================================
    // FORM CRUD ACTIONS: BLOOD UNITS
    // ========================================================================
    private void calculateExpiryDate() {
        String donDate = donationDateField.getText().trim();
        String comp = (String) unitComponentCombo.getSelectedItem();
        if (donDate.isEmpty() || comp == null) return;

        try {
            LocalDate d = LocalDate.parse(donDate);
            LocalDate exp;
            if (comp.contains("Platelet")) {
                exp = d.plusDays(6); // Platelets: 5-7 days
            } else if (comp.contains("Plasma")) {
                exp = d.plusDays(365); // Plasma: 1 year
            } else {
                exp = d.plusDays(42); // RBC: 42 days
            }
            expirationDateField.setText(exp.toString());
        } catch (DateTimeParseException ignored) {}
    }

    private void addBloodUnit() {
        BloodUnit u = readUnitForm(0);
        if (u == null) return;
        if (dao.addUnit(u)) {
            refreshAllData();
            setStatus("Blood unit " + u.getUnitCode() + " successfully registered!", 1);
            clearUnitForm();
        } else {
            setStatus("Failed to register blood unit. Check code uniqueness & database connection.", 2);
        }
    }

    private void updateBloodUnit() {
        int v = unitTable.getSelectedRow();
        if (v == -1) { setStatus("Select a unit in the table to update", 2); return; }
        int id = (int) unitTableModel.getValueAt(unitTable.convertRowIndexToModel(v), 0);
        BloodUnit u = readUnitForm(id);
        if (u == null) return;
        if (dao.updateUnit(u)) {
            refreshAllData();
            setStatus("Blood unit " + u.getUnitCode() + " updated!", 1);
        } else {
            setStatus("Failed to update blood unit.", 2);
        }
    }

    private void markUnitTransfused() {
        int v = unitTable.getSelectedRow();
        if (v == -1) { setStatus("Select a unit in the table first", 2); return; }
        int r = unitTable.convertRowIndexToModel(v);
        int id = (int) unitTableModel.getValueAt(r, 0);
        String code = str(unitTableModel.getValueAt(r, 1));

        if (Ui.confirm(this, "Mark Transfused", "Mark unit <b>" + code + "</b> as Transfused?", "Confirm")) {
            if (dao.updateUnitStatus(id, "Transfused")) {
                refreshAllData();
                setStatus("Blood unit " + code + " marked as transfused.", 1);
            }
        }
    }

    private void deleteBloodUnit() {
        int v = unitTable.getSelectedRow();
        if (v == -1) { setStatus("Select a unit in the table first", 2); return; }
        int r = unitTable.convertRowIndexToModel(v);
        int id = (int) unitTableModel.getValueAt(r, 0);
        String code = str(unitTableModel.getValueAt(r, 1));

        if (Ui.confirm(this, "Delete Unit", "Permanently delete unit <b>" + code + "</b> from inventory?", "Delete")) {
            if (dao.deleteUnit(id)) {
                refreshAllData();
                setStatus("Unit " + code + " deleted.", 1);
                clearUnitForm();
            }
        }
    }

    private BloodUnit readUnitForm(int id) {
        String code = unitCodeField.getText().trim();
        String bloodType = (String) unitBloodTypeCombo.getSelectedItem();
        String component = (String) unitComponentCombo.getSelectedItem();
        String volStr = volumeField.getText().trim();
        String donDate = donationDateField.getText().trim();
        String expDate = expirationDateField.getText().trim();
        String loc = storageField.getText().trim();
        String status = (String) unitStatusCombo.getSelectedItem();

        if (code.isEmpty()) { setStatus("Unit code is required", 2); unitCodeField.requestFocus(); return null; }
        int vol = 450;
        try { vol = Integer.parseInt(volStr); } catch (NumberFormatException e) {
            setStatus("Volume must be a number (e.g. 450)", 2); volumeField.requestFocus(); return null;
        }

        try { LocalDate.parse(donDate); } catch (DateTimeParseException e) {
            setStatus("Donation date must be format YYYY-MM-DD", 2); donationDateField.requestFocus(); return null;
        }
        try { LocalDate.parse(expDate); } catch (DateTimeParseException e) {
            setStatus("Expiration date must be format YYYY-MM-DD", 2); expirationDateField.requestFocus(); return null;
        }

        if (loc.isEmpty()) loc = "Cold Storage Unit 1";

        return new BloodUnit(id, code, null, bloodType, component, vol, donDate, expDate, loc, status);
    }

    private void clearUnitForm() {
        unitCodeField.setText("BLD-2026-0" + (unitTableModel.getRowCount() + 101));
        donationDateField.setText(LocalDate.now().toString());
        calculateExpiryDate();
        storageField.setText("Main Cold Room");
        unitTable.clearSelection();
    }

    // ========================================================================
    // FORM CRUD ACTIONS: EMERGENCY REQUESTS
    // ========================================================================
    private void addEmergencyRequest() {
        EmergencyRequest r = readReqForm(0);
        if (r == null) return;
        if (dao.addRequest(r)) {
            refreshAllData();
            setStatus("Emergency request created for patient " + r.getPatientName() + "!", 1);
            clearReqForm();
        } else {
            setStatus("Failed to create emergency request.", 2);
        }
    }

    private void updateEmergencyRequest() {
        int v = reqTable.getSelectedRow();
        if (v == -1) { setStatus("Select a request in the table to update", 2); return; }
        int id = (int) reqTableModel.getValueAt(reqTable.convertRowIndexToModel(v), 0);
        EmergencyRequest r = readReqForm(id);
        if (r == null) return;
        if (dao.updateRequest(r)) {
            refreshAllData();
            setStatus("Request #" + id + " updated!", 1);
        } else {
            setStatus("Failed to update request.", 2);
        }
    }

    private void markRequestFulfilled() {
        int v = reqTable.getSelectedRow();
        if (v == -1) { setStatus("Select a request in the table first", 2); return; }
        int id = (int) reqTableModel.getValueAt(reqTable.convertRowIndexToModel(v), 0);
        if (dao.updateRequestStatus(id, "Fulfilled")) {
            refreshAllData();
            setStatus("Request #" + id + " marked as Fulfilled.", 1);
        }
    }

    private void deleteEmergencyRequest() {
        int v = reqTable.getSelectedRow();
        if (v == -1) { setStatus("Select a request in the table first", 2); return; }
        int id = (int) reqTableModel.getValueAt(reqTable.convertRowIndexToModel(v), 0);
        if (Ui.confirm(this, "Delete Request", "Delete request #" + id + "?", "Delete")) {
            if (dao.deleteRequest(id)) {
                refreshAllData();
                setStatus("Request #" + id + " deleted.", 1);
                clearReqForm();
            }
        }
    }

    private EmergencyRequest readReqForm(int id) {
        String patient = patientField.getText().trim();
        String hosp = hospitalField.getText().trim();
        String grp = (String) reqBloodTypeCombo.getSelectedItem();
        String comp = (String) reqComponentCombo.getSelectedItem();
        String unitsStr = unitsNeededField.getText().trim();
        String urgency = (String) reqUrgencyCombo.getSelectedItem();
        String date = reqDateField.getText().trim();
        String status = (String) reqStatusCombo.getSelectedItem();
        String notes = reqNotesField.getText().trim();

        if (patient.isEmpty()) { setStatus("Patient name is required", 2); patientField.requestFocus(); return null; }
        if (hosp.isEmpty()) { setStatus("Hospital name is required", 2); hospitalField.requestFocus(); return null; }

        int units = 1;
        try { units = Integer.parseInt(unitsStr); } catch (NumberFormatException e) {
            setStatus("Units needed must be a positive number", 2); unitsNeededField.requestFocus(); return null;
        }

        try { LocalDate.parse(date); } catch (DateTimeParseException e) {
            setStatus("Request date must be format YYYY-MM-DD", 2); reqDateField.requestFocus(); return null;
        }

        return new EmergencyRequest(id, patient, hosp, grp, comp, units, urgency, date, status, notes);
    }

    private void clearReqForm() {
        patientField.setText("");
        hospitalField.setText("");
        unitsNeededField.setText("1");
        reqDateField.setText(LocalDate.now().toString());
        reqNotesField.setText("");
        reqTable.clearSelection();
        selectedRequest = null;
        updateSmartMatcher();
    }

    // ========================================================================
    // FORM CRUD ACTIONS: DONORS
    // ========================================================================
    private void addDonor() {
        Donor d = readDonorForm(0);
        if (d == null) return;
        if (dao.addDonor(d)) {
            refreshAllData();
            setStatus("Donor " + d.getName() + " registered successfully!", 1);
            clearDonorForm();
        } else {
            setStatus("Failed to register donor.", 2);
        }
    }

    private void updateDonor() {
        int v = donorTable.getSelectedRow();
        if (v == -1) { setStatus("Select a donor in the table to update", 2); return; }
        int id = (int) donorTableModel.getValueAt(donorTable.convertRowIndexToModel(v), 0);
        Donor d = readDonorForm(id);
        if (d == null) return;
        if (dao.updateDonor(d)) {
            refreshAllData();
            setStatus("Donor " + d.getName() + " updated!", 1);
        } else {
            setStatus("Failed to update donor.", 2);
        }
    }

    private void deleteDonor() {
        int v = donorTable.getSelectedRow();
        if (v == -1) { setStatus("Select a donor in the table first", 2); return; }
        int id = (int) donorTableModel.getValueAt(donorTable.convertRowIndexToModel(v), 0);
        String name = str(donorTableModel.getValueAt(donorTable.convertRowIndexToModel(v), 1));

        if (Ui.confirm(this, "Delete Donor", "Delete volunteer donor <b>" + name + "</b>?", "Delete")) {
            if (dao.deleteDonor(id)) {
                refreshAllData();
                setStatus("Donor " + name + " deleted.", 1);
                clearDonorForm();
            }
        }
    }

    private Donor readDonorForm(int id) {
        String name = donorNameField.getText().trim();
        String grp = (String) donorBloodTypeCombo.getSelectedItem();
        String gender = (String) donorGenderCombo.getSelectedItem();
        String ageStr = donorAgeField.getText().trim();
        String phone = donorPhoneField.getText().trim();
        String email = donorEmailField.getText().trim();
        String city = donorCityField.getText().trim();
        String lastDate = donorLastDateField.getText().trim();
        String status = (String) donorStatusCombo.getSelectedItem();

        if (name.isEmpty()) { setStatus("Donor name cannot be empty", 2); donorNameField.requestFocus(); return null; }
        int age = 25;
        try { age = Integer.parseInt(ageStr); } catch (NumberFormatException e) {
            setStatus("Age must be a valid number", 2); donorAgeField.requestFocus(); return null;
        }

        if (phone.isEmpty()) { setStatus("Phone number is required", 2); donorPhoneField.requestFocus(); return null; }

        if (!lastDate.isEmpty()) {
            try { LocalDate.parse(lastDate); } catch (DateTimeParseException e) {
                setStatus("Last donation date must be format YYYY-MM-DD", 2); donorLastDateField.requestFocus(); return null;
            }
        }

        return new Donor(id, name, grp, gender, age, phone, email, city, lastDate, status);
    }

    private void clearDonorForm() {
        donorNameField.setText("");
        donorAgeField.setText("30");
        donorPhoneField.setText("");
        donorEmailField.setText("");
        donorCityField.setText("");
        donorLastDateField.setText("");
        donorTable.clearSelection();
    }

    // ========================================================================
    // FILTERING & SEARCH
    // ========================================================================
    private void applyUnitFilter() {
        String query = unitSearchField.getText().trim();
        String grp = (String) filterGroupCombo.getSelectedItem();
        String comp = (String) filterCompCombo.getSelectedItem();
        String st = (String) filterStatusCombo.getSelectedItem();

        unitSorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                String eCode = entry.getStringValue(1);
                String eGrp = entry.getStringValue(2);
                String eComp = entry.getStringValue(3);
                String eLoc = entry.getStringValue(7);
                String eStatus = entry.getStringValue(8);

                if (grp != null && !grp.startsWith("All") && !eGrp.equals(grp)) return false;
                if (comp != null && !comp.startsWith("All") && !eComp.equals(comp)) return false;
                if (st != null && !st.startsWith("All")) {
                    if (st.equals("Expiring Soon") && !eStatus.equals("Expiring Soon")) return false;
                    if (!st.equals("Expiring Soon") && !eStatus.equalsIgnoreCase(st)) return false;
                }

                if (!query.isEmpty()) {
                    String full = (eCode + " " + eGrp + " " + eComp + " " + eLoc + " " + eStatus).toLowerCase();
                    return full.contains(query.toLowerCase());
                }
                return true;
            }
        });
    }

    private void applyDonorFilter() {
        String q = donorSearchField.getText().trim();
        donorSorter.setRowFilter(q.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(q)));
    }

    // ========================================================================
    // DATABASE CONFIGURATION DIALOG
    // ========================================================================
    private void showDatabaseConfigDialog() {
        JDialog dlg = new JDialog(this, "MySQL Database Settings", true);
        dlg.setLayout(new BorderLayout());

        JPanel root = new JPanel(new GridBagLayout()) {
            @Override public Color getBackground() { return Theme.card(); }
        };
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        Ui.Field hostF = new Ui.Field("localhost"); hostF.setText(DBConnection.getDbHost());
        Ui.Field portF = new Ui.Field("3306"); portF.setText(String.valueOf(DBConnection.getDbPort()));
        Ui.Field dbF = new Ui.Field("blood_bank_db"); dbF.setText(DBConnection.getDbName());
        Ui.Field userF = new Ui.Field("root"); userF.setText(DBConnection.getDbUser());
        JPasswordField passF = new JPasswordField(DBConnection.getDbPassword());
        passF.setFont(Theme.font(Font.PLAIN, 13f));
        passF.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JLabel testStatus = new JLabel("Test connection to verify settings");
        testStatus.setFont(Theme.font(Font.PLAIN, 12f));
        testStatus.setForeground(Theme.muted());

        Ui.Button testBtn = new Ui.Button("Test Connection", Ui.Kind.SECONDARY);
        testBtn.addActionListener(e -> {
            int port = 3306;
            try { port = Integer.parseInt(portF.getText().trim()); } catch (Exception ignored) {}
            boolean ok = DBConnection.testConnection(
                    hostF.getText().trim(), port, dbF.getText().trim(),
                    userF.getText().trim(), new String(passF.getPassword()));
            if (ok) {
                testStatus.setText("✓ Connected successfully to MySQL!");
                testStatus.setForeground(Theme.success());
            } else {
                testStatus.setText("✕ Connection failed. Check host/user/pass.");
                testStatus.setForeground(Theme.danger());
            }
        });

        Ui.Button saveBtn = new Ui.Button("Save & Connect", Ui.Kind.PRIMARY);
        saveBtn.addActionListener(e -> {
            int port = 3306;
            try { port = Integer.parseInt(portF.getText().trim()); } catch (Exception ignored) {}
            DBConnection.saveConfig(
                    hostF.getText().trim(), port, dbF.getText().trim(),
                    userF.getText().trim(), new String(passF.getPassword()));
            dlg.dispose();
            refreshAllData();
            setStatus("Database settings saved. Reconnected to MySQL.", 1);
        });

        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(4, 0, 2, 0);
        int y = 0;

        g.gridy = y++; root.add(new Ui.Label("MySQL Connection Settings", Ui.Tone.TEXT, Font.BOLD, 16f), g);
        g.gridy = y++; g.insets = new Insets(0, 0, 10, 0);
        root.add(new Ui.Label("Saved in db.properties in your project folder", Ui.Tone.MUTED, Font.PLAIN, 12f), g);

        String[] dLabels = {"Database Host", "Port", "Database Name", "Username", "Password"};
        JComponent[] dInputs = {hostF, portF, dbF, userF, passF};

        for (int i = 0; i < dLabels.length; i++) {
            g.gridy = y++; g.insets = new Insets(4, 0, 2, 0);
            root.add(new Ui.Label(dLabels[i], Ui.Tone.MUTED, Font.BOLD, 12f), g);
            g.gridy = y++; g.insets = new Insets(0, 0, 4, 0);
            root.add(dInputs[i], g);
        }

        g.gridy = y++; g.insets = new Insets(8, 0, 4, 0);
        root.add(testStatus, g);

        JPanel rowBtn = new JPanel(new GridLayout(1, 2, 8, 0));
        rowBtn.setOpaque(false);
        rowBtn.add(testBtn);
        rowBtn.add(saveBtn);

        g.gridy = y++; g.insets = new Insets(10, 0, 0, 0);
        root.add(rowBtn, g);

        dlg.setContentPane(root);
        dlg.pack();
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    // ========================================================================
    // TABLE CREATION & RENDERERS
    // ========================================================================
    private JTable createStyledTable(DefaultTableModel model, int[] colWidths) {
        JTable t = new JTable(model) {
            @Override public Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
                Component c = super.prepareRenderer(renderer, row, col);
                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 1 ? Theme.tableAlt() : Theme.card());
                } else {
                    c.setBackground(Theme.tableSelect());
                }
                return c;
            }
        };

        t.setOpaque(false);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setRowHeight(38);
        t.setFillsViewportHeight(true);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setDefaultRenderer(Object.class, new GeneralCellRenderer());
        t.setDefaultRenderer(Integer.class, new GeneralCellRenderer());

        JTableHeader h = new JTableHeader(t.getColumnModel()) {
            @Override public Color getBackground() { return Theme.tableHeader(); }
        };
        h.setReorderingAllowed(false);
        h.setDefaultRenderer(new TableHeaderRenderer());
        h.setPreferredSize(new Dimension(100, 36));
        t.setTableHeader(h);

        for (int i = 0; i < colWidths.length && i < t.getColumnCount(); i++) {
            t.getColumnModel().getColumn(i).setPreferredWidth(colWidths[i]);
        }

        return t;
    }

    private JScrollPane createStyledScrollPane(JTable t) {
        JScrollPane sp = new JScrollPane(t);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        sp.setColumnHeaderView(t.getTableHeader());
        sp.getColumnHeader().setOpaque(false);
        sp.getVerticalScrollBar().setUI(new Ui.ScrollUI());
        sp.getVerticalScrollBar().setOpaque(false);
        sp.getHorizontalScrollBar().setUI(new Ui.ScrollUI());
        sp.getHorizontalScrollBar().setOpaque(false);
        return sp;
    }

    private static class GeneralCellRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                                 boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, v, sel, false, row, col);
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            setFont(Theme.font(col == 1 ? Font.BOLD : Font.PLAIN, 13f));
            setForeground(col == 0 ? Theme.muted() : Theme.text());
            return this;
        }
    }

    private static class TableHeaderRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                                 boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, v, false, false, row, col);
            setText(String.valueOf(v).toUpperCase());
            setFont(Theme.font(Font.BOLD, 11f));
            setForeground(Theme.muted());
            setBackground(Theme.tableHeader());
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            setHorizontalAlignment(SwingConstants.LEFT);
            return this;
        }
    }

    // Blood Type Pill Renderer
    private static class BloodTypeBadgeRenderer extends JPanel implements TableCellRenderer {
        private String text = "";
        public BloodTypeBadgeRenderer() { setOpaque(false); }

        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                                 boolean focus, int row, int col) {
            text = String.valueOf(v);
            return this;
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = Ui.aa(g);
            g2.setFont(Theme.font(Font.BOLD, 12f));
            FontMetrics fm = g2.getFontMetrics();
            int w = fm.stringWidth(text) + 16, h = 22, x = 8, y = (getHeight() - h) / 2;

            Color base = Theme.primary();
            g2.setColor(Theme.alpha(base, 35));
            g2.fill(new RoundRectangle2D.Float(x, y, w, h, 8, 8));
            g2.setColor(base);
            g2.drawString(text, x + 8, y + (h - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    // Freshness & Expiration Badge Renderer
    private static class FreshnessStatusRenderer extends JPanel implements TableCellRenderer {
        private String text = "";
        public FreshnessStatusRenderer() { setOpaque(false); }

        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                                 boolean focus, int row, int col) {
            text = String.valueOf(v);
            return this;
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Color base;
            switch (text) {
                case "Available":     base = Theme.success(); break;
                case "Expiring Soon": base = Theme.warning(); break;
                case "Expired":       base = Theme.danger(); break;
                case "Transfused":    base = Theme.muted(); break;
                default:              base = Theme.info();
            }
            Graphics2D g2 = Ui.aa(g);
            g2.setFont(Theme.font(Font.BOLD, 11.5f));
            FontMetrics fm = g2.getFontMetrics();
            int w = fm.stringWidth(text) + 16, h = 22, x = 6, y = (getHeight() - h) / 2;

            g2.setColor(Theme.alpha(base, 35));
            g2.fill(new RoundRectangle2D.Float(x, y, w, h, 8, 8));
            g2.setColor(base);
            g2.drawString(text, x + 8, y + (h - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    // Urgency Badge Renderer
    private static class UrgencyBadgeRenderer extends JPanel implements TableCellRenderer {
        private String text = "";
        public UrgencyBadgeRenderer() { setOpaque(false); }

        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                                 boolean focus, int row, int col) {
            text = String.valueOf(v);
            return this;
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Color base;
            switch (text) {
                case "CRITICAL": base = Theme.danger(); break;
                case "URGENT":   base = Theme.warning(); break;
                default:         base = Theme.info();
            }
            Graphics2D g2 = Ui.aa(g);
            g2.setFont(Theme.font(Font.BOLD, 11.5f));
            FontMetrics fm = g2.getFontMetrics();
            int w = fm.stringWidth(text) + 16, h = 22, x = 6, y = (getHeight() - h) / 2;

            g2.setColor(Theme.alpha(base, 35));
            g2.fill(new RoundRectangle2D.Float(x, y, w, h, 8, 8));
            g2.setColor(base);
            g2.drawString(text, x + 8, y + (h - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    // Donor Eligibility Renderer
    private static class DonorEligibilityRenderer extends JPanel implements TableCellRenderer {
        private String text = "";
        public DonorEligibilityRenderer() { setOpaque(false); }

        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                                 boolean focus, int row, int col) {
            text = String.valueOf(v);
            return this;
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            boolean ok = text.startsWith("Eligible");
            Color base = ok ? Theme.success() : Theme.warning();

            Graphics2D g2 = Ui.aa(g);
            g2.setFont(Theme.font(Font.BOLD, 11.5f));
            FontMetrics fm = g2.getFontMetrics();
            int w = fm.stringWidth(text) + 16, h = 22, x = 6, y = (getHeight() - h) / 2;

            g2.setColor(Theme.alpha(base, 35));
            g2.fill(new RoundRectangle2D.Float(x, y, w, h, 8, 8));
            g2.setColor(base);
            g2.drawString(text, x + 8, y + (h - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    // ========================================================================
    // HELPERS
    // ========================================================================
    private void setStatus(String msg, int kind) {
        statusKind = kind;
        statusLabel.setText(msg);
        statusLabel.repaint();
        if (kind == 0) toastTimer.stop(); else toastTimer.restart();
    }

    private static String str(Object o) { return o == null ? "" : o.toString(); }

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new BloodBankManager().setVisible(true));
    }
}
