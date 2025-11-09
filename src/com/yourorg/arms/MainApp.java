package com.yourorg.arms;

import com.yourorg.arms.DatabaseConnector;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.List;

public class MainApp extends JFrame {

    // === PALETTE ===
    private static final Color BG_LIGHT = Color.decode("#F1FAEE");
    private static final Color BG_DARK = Color.decode("#1E1E1E");
    private static final Color ACCENT_PRIMARY = Color.decode("#1D3557");
    private static final Color ACCENT_SECONDARY = Color.decode("#457B9D");
    private static final Color TEXT_DARK = Color.decode("#1D3557");
    private static final Color TEXT_LIGHT = Color.decode("#FFFFFF");
    private static final Color TEXT_BODY = Color.decode("#333333");
    private static final Color TEXT_BODY_DARK = Color.decode("#CCCCCC");
    private static final Color BTN_BG = ACCENT_PRIMARY;
    private static final Color BTN_BG_HOVER = new Color(40, 70, 125);
    private static final Color BTN_TEXT = Color.WHITE;
    private static final Font FONT_UI = new Font("Segoe UI", Font.PLAIN, 15);
    private static final Font FONT_HEADING = new Font("Segoe UI", Font.BOLD, 22);
    private static final Font FONT_TABLE_HDR = new Font("Segoe UI", Font.BOLD, 15);
    private static final Font FONT_TABLE_CELL = new Font("Segoe UI", Font.PLAIN, 14);

    // New vibrant colors for login UI gradients
    private static final Color GRADIENT_CARD_START = Color.decode("#1D3557");
    private static final Color GRADIENT_CARD_END = Color.decode("#457B9D");
    private static final Color GRADIENT_BTN_START = Color.decode("#457B9D");
    private static final Color GRADIENT_BTN_END = Color.decode("#1D3557");
    private static final Color SHADOW_COLOR = new Color(0, 0, 0, 50);

    // Dark mode state
    private boolean isDarkMode = false;

    private CardLayout mainLayout;
    private JPanel rootPanel;
    private CardLayout contentLayout;
    private JPanel contentPanel;

    private JTextField studentNumberField;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton; // exposed so frame can set it as default button

    private int currentStudentId = -1;
    private String currentStudentName = "";

    private JLabel lblProfilePicTop;

    private JPanel topBar;
    private JPanel sidebar;

    // ========== DATA STRUCTURE INTEGRATION ==========
    // HashMap for Login Accounts (cache, not replacing SQL)
    private final HashMap<String, String> loginAccounts = new HashMap<>(); // username -> password

    // ArrayList for grades per term (we’ll collect scalar grades for quick stats
    // like GWA)
    private final ArrayList<Double> gradesPerTerm = new ArrayList<>(16);

    // LinkedList for enrolled courses (dynamic list)
    private final LinkedList<String> enrolledCourses = new LinkedList<>();

    // Queue for recent grades (FIFO, only last 5 for dashboard)
    private final Queue<String> recentGradesQueue = new LinkedList<>();

    // Full in-memory grades list for sorting (Merge Sort demo) and table population
    private List<GradeRecord> gradeRecords = new ArrayList<>();

    // References for UI updates
    private JLabel lblGwaStat;
    private JLabel lblCoursesStat;
    private DefaultTableModel gradesTableModel; // main Grades table model (for sorting/repopulation)

    public MainApp() {
        setTitle("ARMS - Student Grade Checker");
        setSize(1280, 720);
        setMinimumSize(new Dimension(1280, 720));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(true);

        mainLayout = new CardLayout();
        rootPanel = new JPanel(mainLayout);

        rootPanel.add(createLoginPanel(), "Login");
        rootPanel.add(createMainAppPanel(), "MainApp");

        setContentPane(rootPanel);

        // Make LOGIN the default button so Enter triggers it (loginButton is created in
        // createLoginPanel)
        SwingUtilities.invokeLater(() -> {
            if (loginButton != null)
                getRootPane().setDefaultButton(loginButton);
            // Focus student number
            if (studentNumberField != null)
                studentNumberField.requestFocusInWindow();
            // App icon
            ImageIcon appIc = loadIconResource("/assets/ARMS Logo.png", 32, 32);
            if (appIc != null)
                setIconImage(appIc.getImage());
        });

        mainLayout.show(rootPanel, "Login");
    }

    // =================== LOGIN PANEL ===================
    private JPanel createLoginPanel() {
        // New centered single-column login UI matching the provided design
        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(BG_LIGHT);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.CENTER;

        // Outer column panel to stack logo and card
        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Logo at top
        JLabel logo = new JLabel();
        ImageIcon icon = loadIconResource("/assets/ARMS Logo.png", 120, 120);
        if (icon != null)
            logo.setIcon(icon);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        logo.setBorder(new EmptyBorder(0, 0, 20, 0)); // Increased spacing

        // Card (gradient panel) containing inputs with shadow
        GradientPanel card = new GradientPanel(GRADIENT_CARD_START, GRADIENT_CARD_END);
        card.setPreferredSize(new Dimension(520, 340)); // Slightly taller for better spacing
        card.setMaximumSize(new Dimension(520, 340));
        // Custom border with shadow effect
        card.setBorder(new CompoundBorder(new EmptyBorder(32, 32, 32, 32), new RoundedBorder(20, Color.WHITE))); // Softer
                                                                                                                 // radius
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        // Input fields centered with light backgrounds and icons
        studentNumberField = createInputFieldWithIcon("", "/assets/id.png");
        usernameField = createInputFieldWithIcon("", "/assets/user.png");
        passwordField = createPasswordFieldWithIcon("", "/assets/lock.png");

        // Style inputs to look like the design (wider and with margin)
        Dimension inpDim = new Dimension(380, 44); // Slightly taller
        studentNumberField.setMaximumSize(inpDim);
        usernameField.setMaximumSize(inpDim);
        passwordField.setMaximumSize(inpDim);

        // Centering wrapper
        JPanel inputsWrap = new JPanel();
        inputsWrap.setOpaque(false);
        inputsWrap.setLayout(new BoxLayout(inputsWrap, BoxLayout.Y_AXIS));
        inputsWrap.setAlignmentX(Component.CENTER_ALIGNMENT);
        // Add small labels to indicate what to enter in each field (student number,
        // username, password)
        JLabel lblStudentNumber = new JLabel("Student Number");
        lblStudentNumber.setFont(new Font("Segoe UI", Font.BOLD, 16)); // Larger font
        lblStudentNumber.setForeground(Color.WHITE);
        lblStudentNumber.setAlignmentX(Component.CENTER_ALIGNMENT);
        inputsWrap.add(lblStudentNumber);
        inputsWrap.add(Box.createVerticalStrut(8));
        inputsWrap.add(studentNumberField);
        inputsWrap.add(Box.createVerticalStrut(16)); // More spacing

        JLabel lblUsername = new JLabel("Username");
        lblUsername.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblUsername.setForeground(Color.WHITE);
        lblUsername.setAlignmentX(Component.CENTER_ALIGNMENT);
        inputsWrap.add(lblUsername);
        inputsWrap.add(Box.createVerticalStrut(8));
        inputsWrap.add(usernameField);
        inputsWrap.add(Box.createVerticalStrut(16));

        JLabel lblPassword = new JLabel("Password");
        lblPassword.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblPassword.setForeground(Color.WHITE);
        lblPassword.setAlignmentX(Component.CENTER_ALIGNMENT);
        inputsWrap.add(lblPassword);
        inputsWrap.add(Box.createVerticalStrut(8));
        // Place password field and a show/hide checkbox in a single row
        JPanel passRow = new JPanel();
        passRow.setOpaque(false);
        passRow.setLayout(new BoxLayout(passRow, BoxLayout.X_AXIS));
        passwordField.setMaximumSize(inpDim);
        passRow.add(passwordField);
        passRow.add(Box.createHorizontalStrut(10));
        JCheckBox showPass = new JCheckBox("Show");
        showPass.setOpaque(false);
        showPass.setForeground(Color.WHITE);
        showPass.setFont(FONT_UI);
        final char defaultEcho = passwordField.getEchoChar();
        showPass.addActionListener(ev -> {
            if (showPass.isSelected())
                passwordField.setEchoChar((char) 0);
            else
                passwordField.setEchoChar(defaultEcho);
        });
        passRow.add(showPass);
        inputsWrap.add(passRow);

        // Large login button styled with gradient
        loginButton = createGradientButton("LOGIN");
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 18)); // Larger font
        loginButton.setPreferredSize(new Dimension(180, 50)); // Larger size
        loginButton.setMaximumSize(new Dimension(180, 50));
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginButton.addActionListener(e -> attemptLogin());
        loginButton.setToolTipText("Press Enter to login");

        // Assemble card
        card.add(Box.createVerticalGlue());
        card.add(inputsWrap);
        card.add(Box.createVerticalStrut(24)); // More spacing
        card.add(loginButton);
        card.add(Box.createVerticalGlue());

        stack.add(logo);
        stack.add(card);

        root.add(stack, c);
        return root;
    }

    // =================== LOGIN LOGIC (with HashMap cache) ===================
    private void attemptLogin() {
        String studentNumber = studentNumberField.getText().trim();
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (studentNumber.isEmpty() || username.isEmpty() || password.isEmpty()) {
            showMessage("Please fill Student Number, Username, and Password.", "Login Failed",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // --- Check in-memory cache first (HashMap for quick validation) ---
        if (loginAccounts.containsKey(username) && Objects.equals(loginAccounts.get(username), password)) {
            // Cache hit, skip DB
            mainLayout.show(rootPanel, "MainApp");
            loadStudentDataStructures(); // Fill grades/courses/queue + stats
            refreshGradesForCurrentStudent();
            updateDashboardStats();
            return;
        }

        // --- SQL check as primary source ---
        String sql = "SELECT id, full_name, profile_pic, password FROM students WHERE student_number = ? AND username = ?";
        try (Connection conn = DatabaseConnector.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentNumber);
            ps.setString(2, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String dbPassword = rs.getString("password");
                    if (Objects.equals(dbPassword, password)) {
                        // Add to HashMap cache for future quick validation
                        loginAccounts.put(username, dbPassword);

                        currentStudentId = rs.getInt("id");
                        currentStudentName = rs.getString("full_name");
                        byte[] blob = rs.getBytes("profile_pic");
                        if (blob != null) {
                            ImageIcon ic = imageFromBytes(blob, 40, 40);
                            if (ic != null && lblProfilePicTop != null)
                                lblProfilePicTop.setIcon(ic);
                        }
                        mainLayout.show(rootPanel, "MainApp");
                        loadStudentDataStructures(); // Fill grades/courses/queue + stats
                        refreshGradesForCurrentStudent();
                        updateDashboardStats();
                        return;
                    }
                }
                showMessage("Login failed (no matching student or wrong password).", "Login Failed",
                        JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            showMessage("Database error during login:\n" + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    // =================== MAIN APP PANEL ===================
    private JPanel createMainAppPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());

        // --- Top bar ---
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(ACCENT_PRIMARY);
        topBar.setPreferredSize(new Dimension(1280, 60));
        topBar.setBorder(new MatteBorder(0, 0, 2, 0, ACCENT_SECONDARY));

        // Spacer on WEST to keep title perfectly centered despite EAST profile pic
        JPanel westSpacer = new JPanel();
        westSpacer.setOpaque(false);
        westSpacer.setPreferredSize(new Dimension(64, 1)); // approx width of profile pic + padding
        topBar.add(westSpacer, BorderLayout.WEST);

        JLabel title = new JLabel("Student Portal", SwingConstants.CENTER);
        title.setFont(FONT_HEADING);
        title.setForeground(BTN_TEXT);
        topBar.add(title, BorderLayout.CENTER);

        lblProfilePicTop = new JLabel();
        lblProfilePicTop.setBorder(new EmptyBorder(6, 6, 6, 18));
        ImageIcon topDefault = loadIconResource("/assets/default_profile.png", 40, 40);
        if (topDefault != null)
            lblProfilePicTop.setIcon(topDefault);
        topBar.add(lblProfilePicTop, BorderLayout.EAST);

        mainPanel.add(topBar, BorderLayout.NORTH);

        // --- Sidebar ---
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(ACCENT_PRIMARY);
        sidebar.setPreferredSize(new Dimension(220, 720));
        sidebar.setBorder(new MatteBorder(0, 0, 0, 2, ACCENT_SECONDARY));

        // Center the sidebar logo perfectly horizontally using a wrapper with glue
        JPanel logoWrapper = new JPanel();
        logoWrapper.setBackground(ACCENT_PRIMARY);
        logoWrapper.setLayout(new BoxLayout(logoWrapper, BoxLayout.X_AXIS));
        logoWrapper.setMaximumSize(new Dimension(220, 70));
        logoWrapper.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel logoLabel = new JLabel();
        ImageIcon logoSmall = loadIconResource("/assets/ARMS Logo.png", 56, 56);
        if (logoSmall != null)
            logoLabel.setIcon(logoSmall);
        logoLabel.setAlignmentY(Component.CENTER_ALIGNMENT);

        logoWrapper.add(Box.createHorizontalGlue());
        logoWrapper.add(logoLabel);
        logoWrapper.add(Box.createHorizontalGlue());

        sidebar.add(Box.createVerticalStrut(18));
        sidebar.add(logoWrapper);
        sidebar.add(Box.createVerticalStrut(28));

        JButton btnDashboard = createSidebarButton("Dashboard", "/assets/dashboard.png");
        JButton btnGrades = createSidebarButton("My Grades", "/assets/mygrades.png");
        JButton btnGWA = createSidebarButton("GWA Calculator", "/assets/gwacalculator.png");
        JButton btnSettings = createSidebarButton("Settings", "/assets/settings.png");
        JButton btnLogout = createSidebarButton("Logout", "/assets/icon_logout.png");

        sidebar.add(btnDashboard);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(btnGrades);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(btnGWA);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(btnSettings);
        sidebar.add(Box.createVerticalGlue());
        sidebar.add(btnLogout);
        sidebar.add(Box.createVerticalStrut(28));

        mainPanel.add(sidebar, BorderLayout.WEST);

        // --- Content ---
        contentLayout = new CardLayout();
        contentPanel = new JPanel(contentLayout);

        contentPanel.setBackground(BG_LIGHT);
        contentPanel.add(createDashboardPanel(), "Dashboard");
        contentPanel.add(createGradesPanel(), "Grades");
        contentPanel.add(createGWAPanel(), "GWA");
        contentPanel.add(createSettingsPanel(), "Settings");

        mainPanel.add(contentPanel, BorderLayout.CENTER);

        // Sidebar actions
        btnDashboard.addActionListener(e -> {
            contentLayout.show(contentPanel, "Dashboard");
            updateDashboardStats();
        });
        btnGrades.addActionListener(e -> {
            contentLayout.show(contentPanel, "Grades");
            refreshGradesForCurrentStudent();
        });
        btnGWA.addActionListener(e -> contentLayout.show(contentPanel, "GWA"));
        btnSettings.addActionListener(e -> contentLayout.show(contentPanel, "Settings"));
        btnLogout.addActionListener(e -> {
            currentStudentId = -1;
            currentStudentName = "";
            lblProfilePicTop.setIcon(loadIconResource("/assets/default_profile.png", 40, 40));
            gradeRecords.clear();
            gradesPerTerm.clear();
            enrolledCourses.clear();
            recentGradesQueue.clear();
            if (gradesTableModel != null)
                gradesTableModel.setRowCount(0);
            if (lblGwaStat != null)
                lblGwaStat.setText("—");
            if (lblCoursesStat != null)
                lblCoursesStat.setText("—");
            mainLayout.show(rootPanel, "Login");
        });

        return mainPanel;
    }

    // =================== SCREENS (PANELS) ===================
    private JPanel createDashboardPanel() {
        JPanel panel = createCardPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel hello = new JLabel("Welcome, " + (currentStudentName.isEmpty() ? "Student" : currentStudentName) + "!",
                SwingConstants.CENTER);
        hello.setFont(new Font("Segoe UI", Font.BOLD, 26));
        hello.setForeground(TEXT_DARK);
        hello.setAlignmentX(Component.CENTER_ALIGNMENT);
        hello.setBorder(new EmptyBorder(30, 0, 10, 0));

        JPanel stats = new JPanel(new GridLayout(1, 2, 30, 0));
        stats.setOpaque(false);
        stats.setBorder(new EmptyBorder(10, 60, 10, 60));
        stats.setBackground(BG_LIGHT);

        stats.add(createStatBox("GWA", "—"));
        stats.add(createStatBox("Courses", "—"));

        JLabel recentLabel = new JLabel("Recent Grades", SwingConstants.CENTER);
        recentLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        recentLabel.setForeground(TEXT_DARK);
        recentLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTable table = new JTable(new DefaultTableModel(new Object[] { "Course", "Grade", "Remarks" }, 0));
        styleGradesTable(table);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new EmptyBorder(8, 60, 8, 60));
        scroll.setBackground(BG_LIGHT);

        JButton viewAll = createAccentButton("View All Grades");
        viewAll.setAlignmentX(Component.CENTER_ALIGNMENT);
        viewAll.addActionListener(e -> contentLayout.show(contentPanel, "Grades"));

        panel.add(hello);
        panel.add(stats);
        panel.add(Box.createVerticalStrut(10));
        panel.add(recentLabel);
        panel.add(scroll);
        panel.add(Box.createVerticalStrut(8));
        panel.add(viewAll);
        panel.add(Box.createVerticalStrut(20));

        return panel;
    }

    private JPanel createGradesPanel() {
        JPanel panel = createCardPanel();
        panel.setLayout(new BorderLayout());
        panel.setBackground(BG_LIGHT);

        String[] columns = { "Course Code", "Course Name", "Prelim", "Midterm", "Finals", "Remarks" };
        DefaultTableModel tm = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        gradesTableModel = tm;

        JTable table = new JTable(tm);
        styleGradesTable(table);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new EmptyBorder(16, 36, 16, 36));
        scroll.setBackground(BG_LIGHT);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setBackground(BG_LIGHT);
        JButton sortCourse = createOutlinedButton("Sort by Course");
        JButton sortFinal = createOutlinedButton("Sort by Final Grade (MergeSort)");
        top.add(sortCourse);
        top.add(sortFinal);

        panel.add(top, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);

        if (currentStudentId != -1)
            loadGradesIntoTable(tm);

        // Sorting actions using in-memory gradeRecords
        sortCourse.addActionListener(e -> {
            if (currentStudentId == -1)
                return;
            if (gradeRecords.isEmpty())
                loadStudentDataStructures();
            gradeRecords.sort(Comparator.comparing(gr -> gr.courseCode, String.CASE_INSENSITIVE_ORDER));
            repopulateGradesTableFromRecords();
        });
        sortFinal.addActionListener(e -> {
            if (currentStudentId == -1)
                return;
            if (gradeRecords.isEmpty())
                loadStudentDataStructures();
            gradeRecords = mergeSort(gradeRecords, Comparator.comparing(MainApp::finalComparableNullsLast));
            repopulateGradesTableFromRecords();
        });

        return panel;
    }

    private JPanel createGWAPanel() {
        JPanel panel = createCardPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("GWA Calculator");
        title.setFont(FONT_HEADING);
        title.setForeground(TEXT_DARK);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setBorder(new EmptyBorder(20, 0, 10, 0));

        JPanel fields = new JPanel(new GridLayout(3, 2, 10, 10));
        fields.setMaximumSize(new Dimension(400, 120));
        fields.setBackground(BG_LIGHT);
        fields.setBorder(new EmptyBorder(10, 80, 10, 80));

        JLabel l1 = new JLabel("Prelim:");
        l1.setFont(FONT_UI);
        JTextField prelim = createInputField("");
        JLabel l2 = new JLabel("Midterm:");
        l2.setFont(FONT_UI);
        JTextField midterm = createInputField("");
        JLabel l3 = new JLabel("Finals:");
        l3.setFont(FONT_UI);
        JTextField finals = createInputField("");
        fields.add(l1);
        fields.add(prelim);
        fields.add(l2);
        fields.add(midterm);
        fields.add(l3);
        fields.add(finals);

        JLabel result = new JLabel("GWA: —", SwingConstants.CENTER);
        result.setFont(new Font("Segoe UI", Font.BOLD, 18));
        result.setForeground(TEXT_DARK);
        result.setBorder(new EmptyBorder(10, 0, 10, 0));

        JButton compute = createAccentButton("Compute GWA");
        compute.setAlignmentX(Component.CENTER_ALIGNMENT);
        compute.addActionListener(e -> {
            try {
                double p = Double.parseDouble(prelim.getText());
                double m = Double.parseDouble(midterm.getText());
                double f = Double.parseDouble(finals.getText());
                double gwa = (p + m + f) / 3.0;
                result.setText(String.format("GWA: %.2f", gwa));
            } catch (Exception ex) {
                result.setText("Please enter valid numbers.");
            }
        });

        panel.add(title);
        panel.add(fields);
        panel.add(Box.createVerticalStrut(10));
        panel.add(compute);
        panel.add(Box.createVerticalStrut(10));
        panel.add(result);

        return panel;
    }

    private JPanel createSettingsPanel() {
        JPanel panel = createCardPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(isDarkMode ? BG_DARK : BG_LIGHT);

        JLabel title = new JLabel("Settings");
        title.setFont(FONT_HEADING);
        title.setForeground(isDarkMode ? TEXT_LIGHT : TEXT_DARK);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setBorder(new EmptyBorder(20, 0, 20, 0));

        JLabel profilePic = new JLabel();
        ImageIcon bigDefault = loadIconResource("/assets/default_profile.png", 120, 120);
        if (bigDefault != null)
            profilePic.setIcon(bigDefault);
        profilePic.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton changePic = createOutlinedButton("Change Profile Picture");
        changePic.setAlignmentX(Component.CENTER_ALIGNMENT);
        changePic.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            int res = chooser.showOpenDialog(this);
            if (res == JFileChooser.APPROVE_OPTION) {
                File f = chooser.getSelectedFile();
                try {
                    byte[] bytes = java.nio.file.Files.readAllBytes(f.toPath());
                    if (currentStudentId != -1) {
                        try (Connection conn = DatabaseConnector.getConnection();
                                PreparedStatement ps = conn
                                        .prepareStatement("UPDATE students SET profile_pic = ? WHERE id = ?")) {
                            ps.setBytes(1, bytes);
                            ps.setInt(2, currentStudentId);
                            ps.executeUpdate();
                            ImageIcon ic = imageFromBytes(bytes, 40, 40);
                            if (ic != null)
                                lblProfilePicTop.setIcon(ic);
                            ImageIcon big = imageFromBytes(bytes, 120, 120);
                            if (big != null)
                                profilePic.setIcon(big);
                            showMessage("Profile updated.", "Success", JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showMessage("Failed to update profile: " + ex.getMessage(), "Profile Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // Dark Mode Toggle
        JToggleButton darkModeToggle = new JToggleButton(isDarkMode ? "Disable Dark Mode" : "Enable Dark Mode");
        darkModeToggle.setFont(FONT_UI);
        darkModeToggle.setAlignmentX(Component.CENTER_ALIGNMENT);
        darkModeToggle.setSelected(isDarkMode);
        darkModeToggle.addActionListener(e -> {
            isDarkMode = darkModeToggle.isSelected();
            darkModeToggle.setText(isDarkMode ? "Disable Dark Mode" : "Enable Dark Mode");
            applyTheme();
        });

        panel.add(title);
        panel.add(profilePic);
        panel.add(Box.createVerticalStrut(10));
        panel.add(changePic);
        panel.add(Box.createVerticalStrut(15));
        panel.add(darkModeToggle);

        return panel;
    }

    // =================== COMPONENT HELPERS ===================
    private JPanel createCardPanel() {
        JPanel panel = new JPanel();
        panel.setBorder(new CompoundBorder(
                new EmptyBorder(26, 26, 26, 26),
                new LineBorder(ACCENT_SECONDARY, 1, true)));
        panel.setOpaque(true);
        return panel;
    }

    private JTextField createInputField(String placeholder) {
        JTextField field = new JTextField();
        field.setFont(FONT_UI);
        field.setMaximumSize(new Dimension(280, 34));
        field.setBackground(Color.WHITE);
        field.setForeground(TEXT_BODY);
        field.setBorder(new CompoundBorder(
                new LineBorder(ACCENT_SECONDARY, 1, true),
                new EmptyBorder(8, 12, 8, 12)));
        if (!placeholder.isEmpty())
            field.putClientProperty("JTextField.placeholderText", placeholder);
        return field;
    }

    private JPasswordField createPasswordField(String placeholder) {
        JPasswordField field = new JPasswordField();
        field.setFont(FONT_UI);
        field.setMaximumSize(new Dimension(280, 34));
        field.setBackground(Color.WHITE);
        field.setForeground(TEXT_BODY);
        field.setBorder(new CompoundBorder(
                new LineBorder(ACCENT_SECONDARY, 1, true),
                new EmptyBorder(8, 12, 8, 12)));
        field.putClientProperty("JTextField.placeholderText", placeholder);
        return field;
    }

    private JTextField createInputFieldWithIcon(String placeholder, String iconPath) {
        JTextField field = new JTextField();
        field.setFont(FONT_UI);
        field.setMaximumSize(new Dimension(280, 34));
        field.setBackground(Color.WHITE);
        field.setForeground(TEXT_BODY);
        field.setBorder(new CompoundBorder(
                new LineBorder(ACCENT_SECONDARY, 1, true),
                new EmptyBorder(8, 12, 8, 12)));
        if (!placeholder.isEmpty())
            field.putClientProperty("JTextField.placeholderText", placeholder);
        // Add icon to the left
        ImageIcon icon = loadIconResource(iconPath, 20, 20);
        if (icon != null) {
            field.setLayout(new BorderLayout());
            JLabel iconLabel = new JLabel(icon);
            iconLabel.setBorder(new EmptyBorder(0, 8, 0, 8));
            field.add(iconLabel, BorderLayout.WEST);
        }
        return field;
    }

    private JPasswordField createPasswordFieldWithIcon(String placeholder, String iconPath) {
        JPasswordField field = new JPasswordField();
        field.setFont(FONT_UI);
        field.setMaximumSize(new Dimension(280, 34));
        field.setBackground(Color.WHITE);
        field.setForeground(TEXT_BODY);
        field.setBorder(new CompoundBorder(
                new LineBorder(ACCENT_SECONDARY, 1, true),
                new EmptyBorder(8, 12, 8, 12)));
        field.putClientProperty("JTextField.placeholderText", placeholder);
        // Add icon to the left
        ImageIcon icon = loadIconResource(iconPath, 20, 20);
        if (icon != null) {
            field.setLayout(new BorderLayout());
            JLabel iconLabel = new JLabel(icon);
            iconLabel.setBorder(new EmptyBorder(0, 8, 0, 8));
            field.add(iconLabel, BorderLayout.WEST);
        }
        return field;
    }

    private JButton createAccentButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btn.setBackground(BTN_BG);
        btn.setForeground(BTN_TEXT);
        btn.setFocusPainted(false);
        btn.setBorder(new RoundedBorder(18, BTN_BG));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(180, 40));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(BTN_BG_HOVER);
            }

            public void mouseExited(MouseEvent e) {
                btn.setBackground(BTN_BG);
            }
        });
        return btn;
    }

    private JButton createGradientButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, GRADIENT_BTN_START, 0, getHeight(), GRADIENT_BTN_END);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(getForeground());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btn.setForeground(BTN_TEXT);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(180, 40));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setForeground(Color.YELLOW); // Change text color on hover
            }

            public void mouseExited(MouseEvent e) {
                btn.setForeground(BTN_TEXT);
            }
        });
        return btn;
    }

    private JButton createOutlinedButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setBackground(Color.WHITE);
        btn.setForeground(ACCENT_PRIMARY);
        btn.setFocusPainted(false);
        btn.setBorder(new RoundedOutlineBorder(18, ACCENT_PRIMARY));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(160, 36));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(ACCENT_PRIMARY);
                btn.setForeground(Color.WHITE);
            }

            public void mouseExited(MouseEvent e) {
                btn.setBackground(Color.WHITE);
                btn.setForeground(ACCENT_PRIMARY);
            }
        });
        return btn;
    }

    private JButton createSidebarButton(String text, String iconPath) {
        JButton btn = new JButton(text, loadIconResource(iconPath, 32, 32));
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBackground(ACCENT_PRIMARY);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(new CompoundBorder(new EmptyBorder(8, 18, 8, 18), new RoundedBorder(18, ACCENT_PRIMARY)));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setIconTextGap(18);
        btn.setPreferredSize(new Dimension(200, 48));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(ACCENT_SECONDARY);
            }

            public void mouseExited(MouseEvent e) {
                btn.setBackground(ACCENT_PRIMARY);
            }
        });
        return btn;
    }

    private JPanel createStatBox(String title, String value) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new CompoundBorder(
                new LineBorder(ACCENT_SECONDARY, 1, true),
                new EmptyBorder(16, 12, 16, 12)));
        panel.setMaximumSize(new Dimension(220, 90));

        JLabel t = new JLabel(title, SwingConstants.CENTER);
        t.setFont(new Font("Segoe UI", Font.BOLD, 15));
        t.setForeground(ACCENT_PRIMARY);

        JLabel v = new JLabel(value, SwingConstants.CENTER);
        v.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        v.setForeground(TEXT_DARK);

        // capture references for dynamic updates
        if ("GWA".equalsIgnoreCase(title)) {
            lblGwaStat = v;
        } else if ("Courses".equalsIgnoreCase(title)) {
            lblCoursesStat = v;
        }

        panel.add(t, BorderLayout.NORTH);
        panel.add(v, BorderLayout.CENTER);
        return panel;
    }

    private void showMessage(String msg, String title, int type) {
        JOptionPane.showMessageDialog(this, msg, title, type);
    }

    // =================== TABLES ===================
    private void styleGradesTable(JTable table) {
        table.setRowHeight(32);
        table.setFont(FONT_TABLE_CELL);

        JTableHeader th = table.getTableHeader();
        th.setFont(FONT_TABLE_HDR);
        th.setBackground(ACCENT_PRIMARY);
        th.setForeground(Color.WHITE);
        ((DefaultTableCellRenderer) th.getDefaultRenderer()).setHorizontalAlignment(SwingConstants.CENTER);

        table.setSelectionBackground(ACCENT_SECONDARY);
        table.setSelectionForeground(Color.WHITE);

        // Alternating row colors
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean isSelected, boolean hasFocus,
                    int row, int col) {
                Component c = super.getTableCellRendererComponent(t, v, isSelected, hasFocus, row, col);
                c.setBackground(isSelected ? ACCENT_SECONDARY : (row % 2 == 0 ? BG_LIGHT : Color.WHITE));
                c.setForeground(TEXT_BODY);
                setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        });
    }

    // =================== DATA LOADING & REFRESH ===================
    private void refreshGradesForCurrentStudent() {
        Component[] comps = contentPanel.getComponents();
        for (Component c : comps) {
            if (c instanceof JPanel) {
                JPanel p = (JPanel) c;
                JScrollPane sc = findScrollPane(p);
                if (sc != null) {
                    JViewport vp = sc.getViewport();
                    Component view = vp.getView();
                    if (view instanceof JTable) {
                        JTable table = (JTable) view;
                        DefaultTableModel tm = (DefaultTableModel) table.getModel();
                        tm.setRowCount(0);
                        if (p.getLayout() instanceof BorderLayout) {
                            loadGradesIntoTable(tm);
                        } else {
                            loadRecentGradesIntoTable(tm, 5);
                        }
                    }
                }
            }
        }
    }

    private JScrollPane findScrollPane(Container parent) {
        for (Component comp : parent.getComponents()) {
            if (comp instanceof JScrollPane)
                return (JScrollPane) comp;
            if (comp instanceof Container) {
                JScrollPane found = findScrollPane((Container) comp);
                if (found != null)
                    return found;
            }
        }
        return null;
    }

    private void loadGradesIntoTable(DefaultTableModel tm) {
        if (currentStudentId == -1)
            return;
        // Populate data structures first
        loadStudentDataStructures();
        // Fill table from in-memory gradeRecords
        for (GradeRecord gr : gradeRecords) {
            Vector<Object> row = new Vector<>();
            row.add(gr.courseCode);
            row.add(gr.courseName);
            row.add(gr.prelim);
            row.add(gr.midterm);
            row.add(gr.finals);
            row.add(gr.remarks);
            tm.addRow(row);
        }
    }

    private void loadRecentGradesIntoTable(DefaultTableModel tm, int limit) {
        if (currentStudentId == -1)
            return;
        recentGradesQueue.clear();
        String sql = "SELECT course_code, COALESCE(finals, midterm, prelim) AS grade, remarks FROM grades WHERE student_id = ? ORDER BY created_at DESC LIMIT ?";
        try (Connection conn = DatabaseConnector.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentStudentId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String course = rs.getString("course_code");
                    Object gradeObj = rs.getObject("grade");
                    String gradeStr = gradeObj == null ? "—" : String.valueOf(((Number) gradeObj).doubleValue());
                    String remarks = rs.getString("remarks");

                    recentGradesQueue.offer(course + ": " + gradeStr);
                    if (recentGradesQueue.size() > 5)
                        recentGradesQueue.poll();

                    Vector<Object> row = new Vector<>();
                    row.add(course);
                    row.add(gradeObj);
                    row.add(remarks);
                    tm.addRow(row);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // =================== IMAGE HELPERS ===================
    private ImageIcon loadIconResource(String path, int w, int h) {
        try {
            java.net.URL res = getClass().getResource(path);
            BufferedImage img = null;
            if (res != null)
                img = ImageIO.read(res);
            else {
                File f = new File("." + path);
                if (f.exists())
                    img = ImageIO.read(f);
            }
            if (img != null)
                return new ImageIcon(img.getScaledInstance(w, h, Image.SCALE_SMOOTH));
        } catch (Exception e) {
        }
        return null;
    }

    private ImageIcon imageFromBytes(byte[] bytes, int w, int h) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
            BufferedImage img = ImageIO.read(bais);
            if (img == null)
                return null;
            return new ImageIcon(img.getScaledInstance(w, h, Image.SCALE_SMOOTH));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    // =================== BORDER CLASSES ===================
    private static class RoundedBorder extends LineBorder {
        private final int radius;

        public RoundedBorder(int r, Color c) {
            super(c, 2, true);
            this.radius = r;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            g.setColor(lineColor);
            g.drawRoundRect(x, y, w - 1, h - 1, radius, radius);
        }
    }

    private static class RoundedOutlineBorder extends LineBorder {
        private final int radius;

        public RoundedOutlineBorder(int r, Color c) {
            super(c, 2, true);
            this.radius = r;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            g.setColor(lineColor);
            g.drawRoundRect(x, y, w - 1, h - 1, radius, radius);
        }
    }

    // =================== GRADIENT PANEL CLASS ===================
    private static class GradientPanel extends JPanel {
        private final Color startColor;
        private final Color endColor;

        public GradientPanel(Color start, Color end) {
            this.startColor = start;
            this.endColor = end;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            int w = getWidth();
            int h = getHeight();
            GradientPaint gp = new GradientPaint(0, 0, startColor, 0, h, endColor);
            g2d.setPaint(gp);
            g2d.fillRect(0, 0, w, h);

            // Add shadow effect
            g2d.setColor(SHADOW_COLOR);
            g2d.fillRoundRect(5, 5, w - 10, h - 10, 20, 20);
        }
    }

    // =================== DATA STRUCTURE LOADER & HELPERS ===================

    // Grade record model for table + sorting
    private static class GradeRecord {
        String courseCode;
        String courseName;
        Double prelim;
        Double midterm;
        Double finals;
        String remarks;

        GradeRecord(String courseCode, String courseName, Double prelim, Double midterm, Double finals,
                String remarks) {
            this.courseCode = courseCode;
            this.courseName = courseName;
            this.prelim = prelim;
            this.midterm = midterm;
            this.finals = finals;
            this.remarks = remarks;
        }
    }

    // Load/refresh all DS: gradeRecords, gradesPerTerm, enrolledCourses,
    // recentGradesQueue
    private void loadStudentDataStructures() {
        if (currentStudentId == -1)
            return;

        gradeRecords = new ArrayList<>();
        gradesPerTerm.clear();
        enrolledCourses.clear();
        // recentGradesQueue filled by loadRecentGradesIntoTable (dashboard path); we
        // keep it as-is here

        String sql = "SELECT course_code, course_name, prelim, midterm, finals, remarks FROM grades WHERE student_id = ?";
        try (Connection conn = DatabaseConnector.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentStudentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String code = rs.getString("course_code");
                    String name = rs.getString("course_name");
                    Double prelim = rs.getObject("prelim") == null ? null : rs.getDouble("prelim");
                    Double midterm = rs.getObject("midterm") == null ? null : rs.getDouble("midterm");
                    Double finals = rs.getObject("finals") == null ? null : rs.getDouble("finals");
                    String remarks = rs.getString("remarks");

                    gradeRecords.add(new GradeRecord(code, name, prelim, midterm, finals, remarks));

                    // Enrolled courses
                    if (code != null && !enrolledCourses.contains(code)) {
                        enrolledCourses.add(code);
                    }

                    // for GWA, push an effective grade (finals->midterm->prelim)
                    Double effective = finals != null ? finals : (midterm != null ? midterm : prelim);
                    if (effective != null)
                        gradesPerTerm.add(effective);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void updateDashboardStats() {
        // GWA from gradesPerTerm
        if (lblGwaStat != null) {
            if (gradesPerTerm.isEmpty()) {
                lblGwaStat.setText("—");
            } else {
                double sum = 0.0;
                for (Double d : gradesPerTerm)
                    sum += d;
                double gwa = sum / gradesPerTerm.size();
                lblGwaStat.setText(String.format(Locale.ENGLISH, "%.2f", gwa));
            }
        }
        // Courses count
        if (lblCoursesStat != null) {
            lblCoursesStat.setText(enrolledCourses.isEmpty() ? "0" : String.valueOf(enrolledCourses.size()));
        }
    }

    private void repopulateGradesTableFromRecords() {
        if (gradesTableModel == null)
            return;
        gradesTableModel.setRowCount(0);
        for (GradeRecord gr : gradeRecords) {
            Vector<Object> row = new Vector<>();
            row.add(gr.courseCode);
            row.add(gr.courseName);
            row.add(gr.prelim);
            row.add(gr.midterm);
            row.add(gr.finals);
            row.add(gr.remarks);
            gradesTableModel.addRow(row);
        }
    }

    // Comparator helper: finals (then midterm, then prelim); nulls last
    private static Double finalComparableNullsLast(GradeRecord gr) {
        if (gr == null)
            return Double.NEGATIVE_INFINITY;
        if (gr.finals != null)
            return gr.finals;
        if (gr.midterm != null)
            return gr.midterm;
        if (gr.prelim != null)
            return gr.prelim;
        return Double.NEGATIVE_INFINITY; // pushes to beginning for ascending; we’ll just sort ascending so "lowest"
                                         // empty first
    }

    // Generic Merge Sort that returns a new sorted list
    private static <T> List<T> mergeSort(List<T> src, Comparator<T> cmp) {
        if (src.size() <= 1)
            return new ArrayList<>(src);
        int mid = src.size() / 2;
        List<T> left = mergeSort(src.subList(0, mid), cmp);
        List<T> right = mergeSort(src.subList(mid, src.size()), cmp);
        return merge(left, right, cmp);
    }

    private static <T> List<T> merge(List<T> left, List<T> right, Comparator<T> cmp) {
        List<T> out = new ArrayList<>(left.size() + right.size());
        int i = 0, j = 0;
        while (i < left.size() && j < right.size()) {
            if (cmp.compare(left.get(i), right.get(j)) <= 0)
                out.add(left.get(i++));
            else
                out.add(right.get(j++));
        }
        while (i < left.size())
            out.add(left.get(i++));
        while (j < right.size())
            out.add(right.get(j++));
        return out;
    }

    // =================== THEME APPLICATION ===================
    private void applyTheme() {
        // Update content panel background
        contentPanel.setBackground(isDarkMode ? BG_DARK : BG_LIGHT);

        // Update visible panel
        Component current = null;
        for (Component c : contentPanel.getComponents()) {
            if (c.isVisible()) {
                current = c;
                break;
            }
        }
        if (current instanceof JPanel) {
            JPanel p = (JPanel) current;
            p.setBackground(isDarkMode ? BG_DARK : BG_LIGHT);
            // Update labels and components
            updateComponentTheme(p);
        }

        // Update topBar
        topBar.setBackground(isDarkMode ? BG_DARK : ACCENT_PRIMARY);

        // Update sidebar
        sidebar.setBackground(isDarkMode ? BG_DARK : ACCENT_PRIMARY);

        // Repaint to apply changes
        repaint();
    }

    private void updateComponentTheme(Container container) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JLabel) {
                ((JLabel) comp).setForeground(isDarkMode ? TEXT_LIGHT : TEXT_DARK);
            } else if (comp instanceof JButton) {
                JButton btn = (JButton) comp;
                if (btn.getBackground().equals(ACCENT_PRIMARY)) {
                    // Sidebar buttons
                    btn.setBackground(isDarkMode ? BG_DARK : ACCENT_PRIMARY);
                    btn.setForeground(Color.WHITE);
                }
            } else if (comp instanceof JTable) {
                JTable table = (JTable) comp;
                table.setBackground(isDarkMode ? BG_DARK : BG_LIGHT);
                table.setForeground(isDarkMode ? TEXT_LIGHT : TEXT_DARK);
                // Update table header
                JTableHeader header = table.getTableHeader();
                header.setBackground(isDarkMode ? BG_DARK : ACCENT_PRIMARY);
                header.setForeground(Color.WHITE);
            } else if (comp instanceof Container) {
                updateComponentTheme((Container) comp);
            }
        }
    }

    // =================== MAIN ===================
    public static void main(String[] args) {
        // Set cross-platform look and feel for modern appearance
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
        }
        SwingUtilities.invokeLater(() -> new MainApp().setVisible(true));
    }
}
