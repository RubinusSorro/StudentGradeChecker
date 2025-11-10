package com.yourorg.arms;

import com.yourorg.arms.DatabaseConnector;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.List;


public class MainApp extends JFrame {

    // === PALETTE ===
    private static final Color BG_LIGHT = Color.decode("#F1FAEE");
    private static final Color ACCENT_PRIMARY = Color.decode("#1D3557");
    private static final Color ACCENT_SECONDARY = Color.decode("#457B9D");
    private static final Color TEXT_DARK = Color.decode("#1D3557");
    private static final Color TEXT_BODY = Color.decode("#333333");
    private static final Color BTN_BG = ACCENT_PRIMARY;
    private static final Color BTN_BG_HOVER = new Color(40, 70, 125);
    private static final Color BTN_TEXT = Color.WHITE;
    private static final Font FONT_UI = new Font("Segoe UI", Font.PLAIN, 15);
    private static final Font FONT_HEADING = new Font("Segoe UI", Font.BOLD, 22);
    private static final Font FONT_TABLE_HDR = new Font("Segoe UI", Font.BOLD, 15);
    private static final Font FONT_TABLE_CELL = new Font("Segoe UI", Font.PLAIN, 14);

    private CardLayout mainLayout;
    private JPanel rootPanel;
    private CardLayout contentLayout;
    private JPanel contentPanel;

    // Student Login Fields
    private JTextField studentNumberField;
    private JTextField usernameField;
    private JPasswordField passwordField;

    // Teacher Login Fields
    private JTextField teacherUsernameField;
    private JPasswordField teacherPasswordField;

    private int currentStudentId = -1;
    private String currentStudentName = "";
    private int currentTeacherId = -1;
    private String currentTeacherName = "";

    private JLabel lblProfilePicTop;

    // Teacher Portal Components
    private JComboBox<String> studentSelector;
    private JComboBox<String> courseSelector;
    private JTextField prelimField;
    private JTextField midtermField;
    private JTextField finalsField;
    private JTextField remarksField;
    private DefaultTableModel teacherGradesTableModel;

    
    private final HashMap<String, String> loginAccounts = new HashMap<>();
    private final ArrayList<Double> gradesPerTerm = new ArrayList<>(16);
    private final LinkedList<String> enrolledCourses = new LinkedList<>();
    private final Queue<String> recentGradesQueue = new LinkedList<>();
    private List<GradeRecord> gradeRecords = new ArrayList<>();

    private JLabel lblGwaStat;
    private JLabel lblCoursesStat;
    private DefaultTableModel gradesTableModel;

    public MainApp() {
        setTitle("ARMS - Academic Records Management System");
        setSize(1280, 720);
        setMinimumSize(new Dimension(1280, 720));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(true);

        mainLayout = new CardLayout();
        rootPanel = new JPanel(mainLayout);

        rootPanel.add(createRoleSelectionPanel(), "RoleSelection");
        rootPanel.add(createStudentLoginPanel(), "StudentLogin");
        rootPanel.add(createTeacherLoginPanel(), "TeacherLogin");
        rootPanel.add(createMainAppPanel(), "StudentApp");
        rootPanel.add(createTeacherPortalPanel(), "TeacherPortal");

        setContentPane(rootPanel);
        mainLayout.show(rootPanel, "RoleSelection");
    }

 // =================== ROLE SELECTION PANEL ===================
    private JPanel createRoleSelectionPanel() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(BG_LIGHT);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;

        JPanel contentPanel = new JPanel();
        contentPanel.setBackground(BG_LIGHT);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

     // Logo
        JLabel logo = new JLabel();
        ImageIcon icon = loadIconResource("/assets/ARMS.png", 150, 150);
        if (icon != null) logo.setIcon(icon);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Title
        JLabel title = new JLabel("ARMS Portal");
        title.setFont(new Font("Segoe UI", Font.BOLD, 32));
        title.setForeground(TEXT_DARK);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Select your role to continue");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        subtitle.setForeground(TEXT_BODY);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Role buttons
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 30, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setMaximumSize(new Dimension(500, 200));

        JPanel studentCard = createRoleCard("Student", "🎒");
        JPanel teacherCard = createRoleCard("Teacher", "👨‍🏫");

        studentCard.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                mainLayout.show(rootPanel, "StudentLogin");
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                studentCard.setBackground(ACCENT_SECONDARY);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                studentCard.setBackground(Color.WHITE);
            }
        });

        teacherCard.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                mainLayout.show(rootPanel, "TeacherLogin");
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                teacherCard.setBackground(ACCENT_SECONDARY);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                teacherCard.setBackground(Color.WHITE);
            }
        });

        buttonPanel.add(studentCard);
        buttonPanel.add(teacherCard);

        contentPanel.add(logo);
        contentPanel.add(Box.createVerticalStrut(20));
        contentPanel.add(title);
        contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(subtitle);
        contentPanel.add(Box.createVerticalStrut(40));
        contentPanel.add(buttonPanel);

        root.add(contentPanel, gbc);
        return root;
    }

    private JPanel createRoleCard(String roleName, String emoji) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                new LineBorder(ACCENT_PRIMARY, 2, true),
                new EmptyBorder(30, 20, 30, 20)
        ));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel icon = new JLabel(emoji);
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 80));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel label = new JLabel(roleName);
        label.setFont(new Font("Segoe UI", Font.BOLD, 20));
        label.setForeground(TEXT_DARK);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(icon);
        card.add(Box.createVerticalStrut(15));
        card.add(label);

        return card;
    }

 // =================== STUDENT LOGIN PANEL ===================
    private JPanel createStudentLoginPanel() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(BG_LIGHT);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(BG_LIGHT);

        // Logo with border
        JPanel logoContainer = new JPanel();
        logoContainer.setBackground(Color.WHITE);
        logoContainer.setBorder(new CompoundBorder(
                new LineBorder(ACCENT_PRIMARY, 3, true),
                new EmptyBorder(20, 40, 20, 40)
        ));
        logoContainer.setMaximumSize(new Dimension(200, 180));

        JLabel logo = new JLabel();
        ImageIcon studentIcon = loadIconResource("/assets/ARMS Logo.png", 80, 80);
        if (studentIcon != null) {
            logo.setIcon(studentIcon);
        } else {
            // Fallback to emoji if image not found
            logo.setText("🎓");
            logo.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 80));
        }
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel armsLabel = new JLabel("ARMS");
        armsLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        armsLabel.setForeground(ACCENT_PRIMARY);
        armsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Student Portal");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(Color.GRAY);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel logoPanel = new JPanel();
        logoPanel.setLayout(new BoxLayout(logoPanel, BoxLayout.Y_AXIS));
        logoPanel.setBackground(Color.WHITE);
        logoPanel.add(logo);
        logoPanel.add(Box.createVerticalStrut(5));
        logoPanel.add(armsLabel);
        logoPanel.add(subtitle);

        logoContainer.add(logoPanel);

        // Form Panel
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(ACCENT_PRIMARY);
        formPanel.setBorder(new CompoundBorder(
                new LineBorder(ACCENT_PRIMARY, 2, true),
                new EmptyBorder(40, 50, 40, 50)
        ));
        formPanel.setMaximumSize(new Dimension(420, 400));

        // Student Number Field with Label
        JLabel lblStudentNumber = new JLabel("Student Number");
        lblStudentNumber.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblStudentNumber.setForeground(Color.WHITE);
        lblStudentNumber.setAlignmentX(Component.CENTER_ALIGNMENT);

        studentNumberField = createStyledInputField("Enter your student number");
        studentNumberField.setMaximumSize(new Dimension(320, 40));

        // Username Field with Label
        JLabel lblUsername = new JLabel("Username");
        lblUsername.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblUsername.setForeground(Color.WHITE);
        lblUsername.setAlignmentX(Component.CENTER_ALIGNMENT);

        usernameField = createStyledInputField("Enter your username");
        usernameField.setMaximumSize(new Dimension(320, 40));

        // Password Field with Label
        JLabel lblPassword = new JLabel("Password");
        lblPassword.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblPassword.setForeground(Color.WHITE);
        lblPassword.setAlignmentX(Component.CENTER_ALIGNMENT);

        passwordField = createStyledPasswordField("Enter your password");
        passwordField.setMaximumSize(new Dimension(320, 40));

        JButton loginButton = createWhiteButton("LOGIN");
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(250, 45));
        loginButton.addActionListener(e -> attemptStudentLogin());

        JButton backButton = createTransparentButton("← Back to Role Selection");
        backButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        backButton.addActionListener(e -> mainLayout.show(rootPanel, "RoleSelection"));

        formPanel.add(lblStudentNumber);
        formPanel.add(Box.createVerticalStrut(5));
        formPanel.add(studentNumberField);
        formPanel.add(Box.createVerticalStrut(20));
        formPanel.add(lblUsername);
        formPanel.add(Box.createVerticalStrut(5));
        formPanel.add(usernameField);
        formPanel.add(Box.createVerticalStrut(20));
        formPanel.add(lblPassword);
        formPanel.add(Box.createVerticalStrut(5));
        formPanel.add(passwordField);
        formPanel.add(Box.createVerticalStrut(30));
        formPanel.add(loginButton);
        formPanel.add(Box.createVerticalStrut(15));
        formPanel.add(backButton);

        mainPanel.add(logoContainer);
        mainPanel.add(Box.createVerticalStrut(30));
        mainPanel.add(formPanel);

        root.add(mainPanel, gbc);
        return root;
    }

    // =================== TEACHER LOGIN PANEL ===================
    private JPanel createTeacherLoginPanel() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(BG_LIGHT);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(BG_LIGHT);

        // Logo with border
        JPanel logoContainer = new JPanel();
        logoContainer.setBackground(Color.WHITE);
        logoContainer.setBorder(new CompoundBorder(
                new LineBorder(ACCENT_PRIMARY, 3, true),
                new EmptyBorder(20, 40, 20, 40)
        ));
        logoContainer.setMaximumSize(new Dimension(200, 180));

        JLabel logo = new JLabel();
        ImageIcon teacherIcon = loadIconResource("/assets/ARMS Teacher Logo.PNG", 80, 80);
        if (teacherIcon != null) {
            logo.setIcon(teacherIcon);
        } else {
            // Fallback to emoji if image not found
            logo.setText("👨‍🏫");
            logo.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 80));
        }
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel armsLabel = new JLabel("ARMS");
        armsLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        armsLabel.setForeground(ACCENT_PRIMARY);
        armsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Teacher Portal");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(Color.GRAY);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel logoPanel = new JPanel();
        logoPanel.setLayout(new BoxLayout(logoPanel, BoxLayout.Y_AXIS));
        logoPanel.setBackground(Color.WHITE);
        logoPanel.add(logo);
        logoPanel.add(Box.createVerticalStrut(5));
        logoPanel.add(armsLabel);
        logoPanel.add(subtitle);

        logoContainer.add(logoPanel);

        // Form Panel
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(ACCENT_PRIMARY);
        formPanel.setBorder(new CompoundBorder(
                new LineBorder(ACCENT_PRIMARY, 2, true),
                new EmptyBorder(40, 50, 40, 50)
        ));
        formPanel.setMaximumSize(new Dimension(420, 340));

        // Username Field with Label
        JLabel lblUsername = new JLabel("Username");
        lblUsername.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblUsername.setForeground(Color.WHITE);
        lblUsername.setAlignmentX(Component.CENTER_ALIGNMENT);

        teacherUsernameField = createStyledInputField("Enter your username");
        teacherUsernameField.setMaximumSize(new Dimension(320, 40));

        // Password Field with Label
        JLabel lblPassword = new JLabel("Password");
        lblPassword.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblPassword.setForeground(Color.WHITE);
        lblPassword.setAlignmentX(Component.CENTER_ALIGNMENT);

        teacherPasswordField = createStyledPasswordField("Enter your password");
        teacherPasswordField.setMaximumSize(new Dimension(320, 40));

        JButton loginButton = createWhiteButton("LOGIN");
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(250, 45));
        loginButton.addActionListener(e -> attemptTeacherLogin());

        JButton backButton = createTransparentButton("← Back to Role Selection");
        backButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        backButton.addActionListener(e -> mainLayout.show(rootPanel, "RoleSelection"));

        formPanel.add(lblUsername);
        formPanel.add(Box.createVerticalStrut(5));
        formPanel.add(teacherUsernameField);
        formPanel.add(Box.createVerticalStrut(20));
        formPanel.add(lblPassword);
        formPanel.add(Box.createVerticalStrut(5));
        formPanel.add(teacherPasswordField);
        formPanel.add(Box.createVerticalStrut(30));
        formPanel.add(loginButton);
        formPanel.add(Box.createVerticalStrut(15));
        formPanel.add(backButton);

        mainPanel.add(logoContainer);
        mainPanel.add(Box.createVerticalStrut(30));
        mainPanel.add(formPanel);

        root.add(mainPanel, gbc);
        return root;
    }

    // =================== NEW STYLED INPUT FIELD METHODS ===================
    private JTextField createStyledInputField(String placeholder) {
        JTextField field = new JTextField();
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBackground(Color.WHITE);
        field.setForeground(Color.GRAY);
        field.setText(placeholder);
        field.setBorder(new CompoundBorder(
                new LineBorder(Color.WHITE, 1, true),
                new EmptyBorder(10, 15, 10, 15)
        ));
        
        // Placeholder behavior
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(TEXT_BODY);
                }
            }
            
            @Override
            public void focusLost(FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(Color.GRAY);
                }
            }
        });
        
        return field;
    }

    private JPasswordField createStyledPasswordField(String placeholder) {
        JPasswordField field = new JPasswordField();
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBackground(Color.WHITE);
        field.setForeground(Color.GRAY);
        field.setText(placeholder);
        field.setEchoChar((char) 0); // Show placeholder text
        field.setBorder(new CompoundBorder(
                new LineBorder(Color.WHITE, 1, true),
                new EmptyBorder(10, 15, 10, 15)
        ));
        
        // Placeholder behavior
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (String.valueOf(field.getPassword()).equals(placeholder)) {
                    field.setText("");
                    field.setEchoChar('•'); // Set password masking
                    field.setForeground(TEXT_BODY);
                }
            }
            
            @Override
            public void focusLost(FocusEvent e) {
                if (field.getPassword().length == 0) {
                    field.setEchoChar((char) 0); // Remove masking for placeholder
                    field.setText(placeholder);
                    field.setForeground(Color.GRAY);
                }
            }
        });
        
        return field;
    }

    private JButton createWhiteButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btn.setBackground(Color.WHITE);
        btn.setForeground(ACCENT_PRIMARY);
        btn.setFocusPainted(false);
        btn.setBorder(new CompoundBorder(
                new LineBorder(Color.WHITE, 2, true),
                new EmptyBorder(10, 20, 10, 20)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { 
                btn.setBackground(BG_LIGHT);
            }
            public void mouseExited(MouseEvent e) { 
                btn.setBackground(Color.WHITE);
            }
        });
        
        return btn;
    }

    private JButton createTransparentButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btn.setBackground(ACCENT_PRIMARY);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(8, 15, 8, 15));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setContentAreaFilled(false);
        
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { 
                btn.setForeground(BG_LIGHT);
            }
            public void mouseExited(MouseEvent e) { 
                btn.setForeground(Color.WHITE);
            }
        });
        
        return btn;
    }
 // =================== LOGIN LOGIC ===================
    private void attemptStudentLogin() {
        String studentNumber = studentNumberField.getText().trim();
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        // Check for empty fields or placeholder text
        if (studentNumber.isEmpty() || studentNumber.equals("Enter your student number") ||
            username.isEmpty() || username.equals("Enter your username") ||
            password.isEmpty() || password.equals("Enter your password")) {
            showMessage("Please fill all fields.", "Login Failed", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Check in-memory cache first
        if (loginAccounts.containsKey(username) && Objects.equals(loginAccounts.get(username), password)) {
            mainLayout.show(rootPanel, "StudentApp");
            loadStudentDataStructures();
            refreshGradesForCurrentStudent();
            updateDashboardStats();
            return;
        }

        // SQL check as primary source
        String sql = "SELECT id, full_name, profile_pic, password FROM students WHERE student_number = ? AND username = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentNumber);
            ps.setString(2, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String dbPassword = rs.getString("password");
                    if (Objects.equals(dbPassword, password)) {
                        // Cache credentials for future quick validation
                        loginAccounts.put(username, dbPassword);
                        
                        // Set current student data
                        currentStudentId = rs.getInt("id");
                        currentStudentName = rs.getString("full_name");
                        
                        // Load profile picture if available
                        byte[] blob = rs.getBytes("profile_pic");
                        if (blob != null) {
                            ImageIcon ic = imageFromBytes(blob, 40, 40);
                            if (ic != null && lblProfilePicTop != null) {
                                lblProfilePicTop.setIcon(ic);
                            }
                        }
                        
                        // Navigate to student portal and load data
                        mainLayout.show(rootPanel, "StudentApp");
                        loadStudentDataStructures();
                        refreshGradesForCurrentStudent();
                        updateDashboardStats();
                        
                        // Clear login fields
                        clearStudentLoginFields();
                        return;
                    }
                }
                showMessage("Invalid credentials. Please check your student number, username, and password.", 
                           "Login Failed", 
                           JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            showMessage("Database connection error. Please try again later.\n\nDetails: " + ex.getMessage(), 
                       "Database Error", 
                       JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void attemptTeacherLogin() {
        String username = teacherUsernameField.getText().trim();
        String password = new String(teacherPasswordField.getPassword());

        // Check for empty fields or placeholder text
        if (username.isEmpty() || username.equals("Enter your username") ||
            password.isEmpty() || password.equals("Enter your password")) {
            showMessage("Please fill all fields.", "Login Failed", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Check teachers table
        String sql = "SELECT id, full_name, password FROM teachers WHERE username = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String dbPassword = rs.getString("password");
                    if (Objects.equals(dbPassword, password)) {
                        // Set current teacher data
                        currentTeacherId = rs.getInt("id");
                        currentTeacherName = rs.getString("full_name");
                        
                        // Navigate to teacher portal and load data
                        mainLayout.show(rootPanel, "TeacherPortal");
                        loadTeacherPortalData();
                        
                        // Clear login fields
                        clearTeacherLoginFields();
                        return;
                    }
                }
                showMessage("Invalid credentials. Please check your username and password.", 
                           "Login Failed", 
                           JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            showMessage("Database connection error. Please try again later.\n\nDetails: " + ex.getMessage(), 
                       "Database Error", 
                       JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    // =================== HELPER METHODS FOR CLEARING LOGIN FIELDS ===================
    private void clearStudentLoginFields() {
        studentNumberField.setText("Enter your student number");
        studentNumberField.setForeground(Color.GRAY);
        
        usernameField.setText("Enter your username");
        usernameField.setForeground(Color.GRAY);
        
        passwordField.setEchoChar((char) 0);
        passwordField.setText("Enter your password");
        passwordField.setForeground(Color.GRAY);
    }

    private void clearTeacherLoginFields() {
        teacherUsernameField.setText("Enter your username");
        teacherUsernameField.setForeground(Color.GRAY);
        
        teacherPasswordField.setEchoChar((char) 0);
        teacherPasswordField.setText("Enter your password");
        teacherPasswordField.setForeground(Color.GRAY);
    }
    // =================== TEACHER PORTAL ===================
    private JPanel createTeacherPortalPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Top bar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(ACCENT_PRIMARY);
        topBar.setPreferredSize(new Dimension(1280, 60));

        JLabel title = new JLabel("Teacher Portal", SwingConstants.CENTER);
        title.setFont(FONT_HEADING);
        title.setForeground(BTN_TEXT);
        topBar.add(title, BorderLayout.CENTER);

        JButton logoutBtn = createOutlinedButton("Logout");
        logoutBtn.setBackground(ACCENT_PRIMARY);
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.addActionListener(e -> {
            currentTeacherId = -1;
            currentTeacherName = "";
            mainLayout.show(rootPanel, "RoleSelection");
        });
        JPanel eastPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        eastPanel.setBackground(ACCENT_PRIMARY);
        eastPanel.add(logoutBtn);
        topBar.add(eastPanel, BorderLayout.EAST);

        mainPanel.add(topBar, BorderLayout.NORTH);

        // Main content
        JPanel content = new JPanel(new BorderLayout(20, 20));
        content.setBackground(BG_LIGHT);
        content.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Left: Grade Management Form
        JPanel formPanel = createGradeManagementForm();
        content.add(formPanel, BorderLayout.WEST);

        // Center/Right: Grades Table
        JPanel tablePanel = createTeacherGradesTable();
        content.add(tablePanel, BorderLayout.CENTER);

        mainPanel.add(content, BorderLayout.CENTER);

        return mainPanel;
    }

    private JPanel createGradeManagementForm() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new CompoundBorder(
                new LineBorder(ACCENT_PRIMARY, 2, true),
                new EmptyBorder(20, 20, 20, 20)
        ));
        panel.setPreferredSize(new Dimension(400, 600));

        JLabel formTitle = new JLabel("Grade Management");
        formTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        formTitle.setForeground(TEXT_DARK);
        formTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Student Selection
        JLabel lblStudent = new JLabel("Select Student:");
        lblStudent.setFont(FONT_UI);
        studentSelector = new JComboBox<>();
        studentSelector.setFont(FONT_UI);
        studentSelector.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));

        // Course Selection
        JLabel lblCourse = new JLabel("Select Course:");
        lblCourse.setFont(FONT_UI);
        courseSelector = new JComboBox<>();
        courseSelector.setFont(FONT_UI);
        courseSelector.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));

        // Grade Fields
        JLabel lblPrelim = new JLabel("Prelim Grade:");
        lblPrelim.setFont(FONT_UI);
        prelimField = createInputField("");
        prelimField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));

        JLabel lblMidterm = new JLabel("Midterm Grade:");
        lblMidterm.setFont(FONT_UI);
        midtermField = createInputField("");
        midtermField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));

        JLabel lblFinals = new JLabel("Finals Grade:");
        lblFinals.setFont(FONT_UI);
        finalsField = createInputField("");
        finalsField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));

        JLabel lblRemarks = new JLabel("Remarks:");
        lblRemarks.setFont(FONT_UI);
        remarksField = createInputField("");
        remarksField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));

        // Buttons
        JPanel buttonPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        buttonPanel.setOpaque(false);
        buttonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));

        JButton addBtn = createAccentButton("Add Grade");
        JButton updateBtn = createAccentButton("Update Grade");
        JButton deleteBtn = createAccentButton("Delete Grade");
        JButton clearBtn = createOutlinedButton("Clear Form");

        addBtn.addActionListener(e -> addGrade());
        updateBtn.addActionListener(e -> updateGrade());
        deleteBtn.addActionListener(e -> deleteGrade());
        clearBtn.addActionListener(e -> clearGradeForm());

        buttonPanel.add(addBtn);
        buttonPanel.add(updateBtn);
        buttonPanel.add(deleteBtn);
        buttonPanel.add(clearBtn);

        // Load existing grade when student and course selected
        ActionListener loadGradeListener = e -> loadExistingGrade();
        studentSelector.addActionListener(loadGradeListener);
        courseSelector.addActionListener(loadGradeListener);

        panel.add(formTitle);
        panel.add(Box.createVerticalStrut(20));
        panel.add(lblStudent);
        panel.add(Box.createVerticalStrut(5));
        panel.add(studentSelector);
        panel.add(Box.createVerticalStrut(15));
        panel.add(lblCourse);
        panel.add(Box.createVerticalStrut(5));
        panel.add(courseSelector);
        panel.add(Box.createVerticalStrut(15));
        panel.add(lblPrelim);
        panel.add(Box.createVerticalStrut(5));
        panel.add(prelimField);
        panel.add(Box.createVerticalStrut(10));
        panel.add(lblMidterm);
        panel.add(Box.createVerticalStrut(5));
        panel.add(midtermField);
        panel.add(Box.createVerticalStrut(10));
        panel.add(lblFinals);
        panel.add(Box.createVerticalStrut(5));
        panel.add(finalsField);
        panel.add(Box.createVerticalStrut(10));
        panel.add(lblRemarks);
        panel.add(Box.createVerticalStrut(5));
        panel.add(remarksField);
        panel.add(Box.createVerticalStrut(20));
        panel.add(buttonPanel);

        return panel;
    }

    private JPanel createTeacherGradesTable() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_LIGHT);

        JLabel tableTitle = new JLabel("All Grades", SwingConstants.CENTER);
        tableTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        tableTitle.setForeground(TEXT_DARK);
        tableTitle.setBorder(new EmptyBorder(0, 0, 10, 0));

        String[] columns = {"ID", "Student", "Course Code", "Course Name", "Prelim", "Midterm", "Finals", "Remarks"};
        teacherGradesTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        JTable table = new JTable(teacherGradesTableModel);
        styleGradesTable(table);
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(0).setMaxWidth(70);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new LineBorder(ACCENT_PRIMARY, 1));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBackground(BG_LIGHT);
        JButton refreshBtn = createOutlinedButton("Refresh");
        refreshBtn.addActionListener(e -> loadAllGradesIntoTable());
        topPanel.add(refreshBtn);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    // =================== TEACHER PORTAL DATA OPERATIONS ===================
    private void loadTeacherPortalData() {
        loadStudentList();
        loadCourseList();
        loadAllGradesIntoTable();
    }

    private void loadStudentList() {
        studentSelector.removeAllItems();
        String sql = "SELECT id, student_number, full_name FROM students ORDER BY full_name";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String display = rs.getString("student_number") + " - " + rs.getString("full_name");
                studentSelector.addItem(display);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void loadCourseList() {
        courseSelector.removeAllItems();
        String sql = "SELECT DISTINCT course_code, course_name FROM grades ORDER BY course_code";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String display = rs.getString("course_code") + " - " + rs.getString("course_name");
                courseSelector.addItem(display);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void loadAllGradesIntoTable() {
        teacherGradesTableModel.setRowCount(0);
        String sql = "SELECT g.id, s.full_name, g.course_code, g.course_name, g.prelim, g.midterm, g.finals, g.remarks " +
                     "FROM grades g JOIN students s ON g.student_id = s.id ORDER BY s.full_name, g.course_code";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Object[] row = {
                    rs.getInt("id"),
                    rs.getString("full_name"),
                    rs.getString("course_code"),
                    rs.getString("course_name"),
                    rs.getObject("prelim"),
                    rs.getObject("midterm"),
                    rs.getObject("finals"),
                    rs.getString("remarks")
                };
                teacherGradesTableModel.addRow(row);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void loadExistingGrade() {
        if (studentSelector.getSelectedItem() == null || courseSelector.getSelectedItem() == null) return;

        String studentInfo = (String) studentSelector.getSelectedItem();
        String courseInfo = (String) courseSelector.getSelectedItem();
        
        String studentNumber = studentInfo.split(" - ")[0];
        String courseCode = courseInfo.split(" - ")[0];

        String sql = "SELECT prelim, midterm, finals, remarks FROM grades g " +
                     "JOIN students s ON g.student_id = s.id " +
                     "WHERE s.student_number = ? AND g.course_code = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentNumber);
            ps.setString(2, courseCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Object prelim = rs.getObject("prelim");
                    Object midterm = rs.getObject("midterm");
                    Object finals = rs.getObject("finals");
                    String remarks = rs.getString("remarks");

                    prelimField.setText(prelim != null ? prelim.toString() : "");
                    midtermField.setText(midterm != null ? midterm.toString() : "");
                    finalsField.setText(finals != null ? finals.toString() : "");
                    remarksField.setText(remarks != null ? remarks : "");
                } else {
                    clearGradeForm();
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void addGrade() {
        if (!validateGradeForm()) return;

        String studentInfo = (String) studentSelector.getSelectedItem();
        String courseInfo = (String) courseSelector.getSelectedItem();
        String studentNumber = studentInfo.split(" - ")[0];
        String courseCode = courseInfo.split(" - ")[0];
        String courseName = courseInfo.split(" - ")[1];

        // Get student ID
        int studentId = getStudentIdByNumber(studentNumber);
        if (studentId == -1) {
            showMessage("Student not found.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Check if grade already exists
        if (gradeExists(studentId, courseCode)) {
            showMessage("Grade already exists for this student and course. Use Update instead.", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sql = "INSERT INTO grades (student_id, course_code, course_name, prelim, midterm, finals, remarks) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setString(2, courseCode);
            ps.setString(3, courseName);
            ps.setObject(4, getDoubleOrNull(prelimField.getText()));
            ps.setObject(5, getDoubleOrNull(midtermField.getText()));
            ps.setObject(6, getDoubleOrNull(finalsField.getText()));
            ps.setString(7, remarksField.getText().trim());
            ps.executeUpdate();
            showMessage("Grade added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            loadAllGradesIntoTable();
            clearGradeForm();
        } catch (SQLException ex) {
            showMessage("Error adding grade:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void updateGrade() {
        if (!validateGradeForm()) return;

        String studentInfo = (String) studentSelector.getSelectedItem();
        String courseInfo = (String) courseSelector.getSelectedItem();
        String studentNumber = studentInfo.split(" - ")[0];
        String courseCode = courseInfo.split(" - ")[0];

        int studentId = getStudentIdByNumber(studentNumber);
        if (studentId == -1) {
            showMessage("Student not found.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String sql = "UPDATE grades SET prelim = ?, midterm = ?, finals = ?, remarks = ? WHERE student_id = ? AND course_code = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, getDoubleOrNull(prelimField.getText()));
            ps.setObject(2, getDoubleOrNull(midtermField.getText()));
            ps.setObject(3, getDoubleOrNull(finalsField.getText()));
            ps.setString(4, remarksField.getText().trim());
            ps.setInt(5, studentId);
            ps.setString(6, courseCode);
            int updated = ps.executeUpdate();
            if (updated > 0) {
                showMessage("Grade updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadAllGradesIntoTable();
                clearGradeForm();
            } else {
                showMessage("No grade found to update. Use Add instead.", "Error", JOptionPane.WARNING_MESSAGE);
            }
        } catch (SQLException ex) {
            showMessage("Error updating grade:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void deleteGrade() {
        if (studentSelector.getSelectedItem() == null || courseSelector.getSelectedItem() == null) {
            showMessage("Please select a student and course.", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to delete this grade?", 
            "Confirm Delete", 
            JOptionPane.YES_NO_OPTION);
        
        if (confirm != JOptionPane.YES_OPTION) return;

        String studentInfo = (String) studentSelector.getSelectedItem();
        String courseInfo = (String) courseSelector.getSelectedItem();
        String studentNumber = studentInfo.split(" - ")[0];
        String courseCode = courseInfo.split(" - ")[0];

        int studentId = getStudentIdByNumber(studentNumber);
        if (studentId == -1) return;

        String sql = "DELETE FROM grades WHERE student_id = ? AND course_code = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setString(2, courseCode);
            int deleted = ps.executeUpdate();
            if (deleted > 0) {
                showMessage("Grade deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadAllGradesIntoTable();
                clearGradeForm();
            } else {
                showMessage("No grade found to delete.", "Error", JOptionPane.WARNING_MESSAGE);
            }
        } catch (SQLException ex) {
            showMessage("Error deleting grade:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void clearGradeForm() {
        prelimField.setText("");
        midtermField.setText("");
        finalsField.setText("");
        remarksField.setText("");
    }

    private boolean validateGradeForm() {
        if (studentSelector.getSelectedItem() == null) {
            showMessage("Please select a student.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        if (courseSelector.getSelectedItem() == null) {
            showMessage("Please select a course.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    private int getStudentIdByNumber(String studentNumber) {
        String sql = "SELECT id FROM students WHERE student_number = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return -1;
    }

    private boolean gradeExists(int studentId, String courseCode) {
        String sql = "SELECT 1 FROM grades WHERE student_id = ? AND course_code = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setString(2, courseCode);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    private Double getDoubleOrNull(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // =================== STUDENT MAIN APP PANEL (unchanged) ===================
    private JPanel createMainAppPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(ACCENT_PRIMARY);
        topBar.setPreferredSize(new Dimension(1280, 60));
        topBar.setBorder(new MatteBorder(0, 0, 2, 0, ACCENT_SECONDARY));

        JPanel westSpacer = new JPanel();
        westSpacer.setOpaque(false);
        westSpacer.setPreferredSize(new Dimension(64, 1));
        topBar.add(westSpacer, BorderLayout.WEST);

        JLabel title = new JLabel("Student Portal", SwingConstants.CENTER);
        title.setFont(FONT_HEADING);
        title.setForeground(BTN_TEXT);
        topBar.add(title, BorderLayout.CENTER);

        lblProfilePicTop = new JLabel();
        lblProfilePicTop.setBorder(new EmptyBorder(6, 6, 6, 18));
        ImageIcon topDefault = loadIconResource("/assets/default_profile.png", 40, 40);
        if (topDefault != null) lblProfilePicTop.setIcon(topDefault);
        topBar.add(lblProfilePicTop, BorderLayout.EAST);

        mainPanel.add(topBar, BorderLayout.NORTH);

        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(ACCENT_PRIMARY);
        sidebar.setPreferredSize(new Dimension(220, 720));
        sidebar.setBorder(new MatteBorder(0, 0, 0, 2, ACCENT_SECONDARY));

        JPanel logoWrapper = new JPanel();
        logoWrapper.setBackground(ACCENT_PRIMARY);
        logoWrapper.setLayout(new BoxLayout(logoWrapper, BoxLayout.X_AXIS));
        logoWrapper.setMaximumSize(new Dimension(220, 70));
        logoWrapper.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel logoLabel = new JLabel();
        ImageIcon logoSmall = loadIconResource("/assets/ARMS Logo.png", 56, 56);
        if (logoSmall != null) logoLabel.setIcon(logoSmall);
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

        sidebar.add(btnDashboard); sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(btnGrades); sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(btnGWA); sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(btnSettings); sidebar.add(Box.createVerticalGlue());
        sidebar.add(btnLogout); sidebar.add(Box.createVerticalStrut(28));

        mainPanel.add(sidebar, BorderLayout.WEST);

        contentLayout = new CardLayout();
        contentPanel = new JPanel(contentLayout);

        contentPanel.setBackground(BG_LIGHT);
        contentPanel.add(createDashboardPanel(), "Dashboard");
        contentPanel.add(createGradesPanel(), "Grades");
        contentPanel.add(createGWAPanel(), "GWA");
        contentPanel.add(createSettingsPanel(), "Settings");

        mainPanel.add(contentPanel, BorderLayout.CENTER);

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
            if (gradesTableModel != null) gradesTableModel.setRowCount(0);
            if (lblGwaStat != null) lblGwaStat.setText("—");
            if (lblCoursesStat != null) lblCoursesStat.setText("—");
            mainLayout.show(rootPanel, "RoleSelection");
        });

        return mainPanel;
    }

    // =================== STUDENT SCREENS (unchanged) ===================
    private JPanel createDashboardPanel() {
        JPanel panel = createCardPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel hello = new JLabel("Welcome, " + (currentStudentName.isEmpty() ? "Student" : currentStudentName) + "!", SwingConstants.CENTER);
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

        JTable table = new JTable(new DefaultTableModel(new Object[]{"Course", "Grade", "Remarks"}, 0));
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

        String[] columns = {"Course Code", "Course Name", "Prelim", "Midterm", "Finals", "Remarks"};
        DefaultTableModel tm = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
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
        top.add(sortCourse); top.add(sortFinal);

        panel.add(top, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);

        if (currentStudentId != -1) loadGradesIntoTable(tm);

        sortCourse.addActionListener(e -> {
            if (currentStudentId == -1) return;
            if (gradeRecords.isEmpty()) loadStudentDataStructures();
            gradeRecords.sort(Comparator.comparing(gr -> gr.courseCode, String.CASE_INSENSITIVE_ORDER));
            repopulateGradesTableFromRecords();
        });
        sortFinal.addActionListener(e -> {
            if (currentStudentId == -1) return;
            if (gradeRecords.isEmpty()) loadStudentDataStructures();
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

        JLabel l1 = new JLabel("Prelim:"); l1.setFont(FONT_UI);
        JTextField prelim = createInputField("");
        JLabel l2 = new JLabel("Midterm:"); l2.setFont(FONT_UI);
        JTextField midterm = createInputField("");
        JLabel l3 = new JLabel("Finals:"); l3.setFont(FONT_UI);
        JTextField finals = createInputField("");
        fields.add(l1); fields.add(prelim);
        fields.add(l2); fields.add(midterm);
        fields.add(l3); fields.add(finals);

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
        panel.setBackground(BG_LIGHT);

        JLabel title = new JLabel("Settings");
        title.setFont(FONT_HEADING);
        title.setForeground(TEXT_DARK);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setBorder(new EmptyBorder(20, 0, 20, 0));

        // Profile Picture Section
        JLabel profilePic = new JLabel("👤");
        profilePic.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 120));
        profilePic.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel profileButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        profileButtonPanel.setOpaque(false);
        
        JButton changePicBtn = createOutlinedButton("Change Picture");
        changePicBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                changeProfilePicture(profilePic);
            }
        });
        
        JButton removePicBtn = createOutlinedButton("Remove Picture");
        removePicBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                removeProfilePicture(profilePic);
            }
        });
        
        profileButtonPanel.add(changePicBtn);
        profileButtonPanel.add(removePicBtn);

        // Change Password Section
        JLabel passwordTitle = new JLabel("Change Password");
        passwordTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        passwordTitle.setForeground(TEXT_DARK);
        passwordTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        passwordTitle.setBorder(new EmptyBorder(30, 0, 10, 0));

        JPasswordField currentPasswordField = createPasswordField("Current Password");
        currentPasswordField.setMaximumSize(new Dimension(350, 35));
        currentPasswordField.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JPasswordField newPasswordField = createPasswordField("New Password");
        newPasswordField.setMaximumSize(new Dimension(350, 35));
        newPasswordField.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JPasswordField confirmPasswordField = createPasswordField("Confirm Password");
        confirmPasswordField.setMaximumSize(new Dimension(350, 35));
        confirmPasswordField.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JButton changePasswordBtn = createAccentButton("Update Password");
        changePasswordBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        changePasswordBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                changePassword(currentPasswordField, newPasswordField, confirmPasswordField);
            }
        });

        

        // Add all components
        panel.add(title);
        panel.add(profilePic);
        panel.add(Box.createVerticalStrut(15));
        panel.add(profileButtonPanel);
        panel.add(passwordTitle);
        panel.add(Box.createVerticalStrut(10));
        panel.add(currentPasswordField);
        panel.add(Box.createVerticalStrut(10));
        panel.add(newPasswordField);
        panel.add(Box.createVerticalStrut(10));
        panel.add(confirmPasswordField);
        panel.add(Box.createVerticalStrut(15));
        panel.add(changePasswordBtn);
        panel.add(Box.createVerticalStrut(10));
        panel.add(Box.createVerticalStrut(30));

        return panel;
    }

    // Change Profile Picture
    private void changeProfilePicture(JLabel profilePicLabel) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Profile Picture");
        int result = chooser.showOpenDialog(this);
        
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
                
                if (currentStudentId != -1) {
                    Connection conn = DatabaseConnector.getConnection();
                    PreparedStatement ps = conn.prepareStatement("UPDATE students SET profile_pic = ? WHERE id = ?");
                    ps.setBytes(1, bytes);
                    ps.setInt(2, currentStudentId);
                    ps.executeUpdate();
                    ps.close();
                    conn.close();
                    
                    // Update displays
                    ImageIcon topIcon = imageFromBytes(bytes, 40, 40);
                    if (topIcon != null) {
                        lblProfilePicTop.setIcon(topIcon);
                        lblProfilePicTop.setText("");
                    }
                    
                    ImageIcon bigIcon = imageFromBytes(bytes, 120, 120);
                    if (bigIcon != null) {
                        profilePicLabel.setIcon(bigIcon);
                        profilePicLabel.setText("");
                    }
                    
                    showMessage("Profile picture updated!", "Success", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                showMessage("Failed to update picture: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // Remove Profile Picture
    private void removeProfilePicture(JLabel profilePicLabel) {
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Remove your profile picture?", 
            "Confirm", 
            JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION && currentStudentId != -1) {
            try {
                Connection conn = DatabaseConnector.getConnection();
                PreparedStatement ps = conn.prepareStatement("UPDATE students SET profile_pic = NULL WHERE id = ?");
                ps.setInt(1, currentStudentId);
                ps.executeUpdate();
                ps.close();
                conn.close();
                
                // Reset to emoji
                lblProfilePicTop.setIcon(null);
                lblProfilePicTop.setText("👤");
                lblProfilePicTop.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));
                
                profilePicLabel.setIcon(null);
                profilePicLabel.setText("👤");
                profilePicLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 120));
                
                showMessage("Profile picture removed!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException ex) {
                ex.printStackTrace();
                showMessage("Failed to remove picture: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // Change Password
    private void changePassword(JPasswordField currentPwdField, JPasswordField newPwdField, JPasswordField confirmPwdField) {
        String currentPassword = new String(currentPwdField.getPassword());
        String newPassword = new String(newPwdField.getPassword());
        String confirmPassword = new String(confirmPwdField.getPassword());
        
        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            showMessage("Please fill all fields.", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (newPassword.length() < 6) {
            showMessage("Password must be at least 6 characters.", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (!newPassword.equals(confirmPassword)) {
            showMessage("Passwords do not match.", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (currentStudentId != -1) {
            try {
                Connection conn = DatabaseConnector.getConnection();
                
                // Check current password
                PreparedStatement checkPs = conn.prepareStatement("SELECT password FROM students WHERE id = ?");
                checkPs.setInt(1, currentStudentId);
                ResultSet rs = checkPs.executeQuery();
                
                if (rs.next()) {
                    String dbPassword = rs.getString("password");
                    if (!dbPassword.equals(currentPassword)) {
                        showMessage("Current password is incorrect.", "Error", JOptionPane.ERROR_MESSAGE);
                        rs.close();
                        checkPs.close();
                        conn.close();
                        return;
                    }
                    
                    // Update password
                    PreparedStatement updatePs = conn.prepareStatement("UPDATE students SET password = ? WHERE id = ?");
                    updatePs.setString(1, newPassword);
                    updatePs.setInt(2, currentStudentId);
                    updatePs.executeUpdate();
                    updatePs.close();
                    
                    currentPwdField.setText("");
                    newPwdField.setText("");
                    confirmPwdField.setText("");
                    
                    showMessage("Password changed successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                }
                
                rs.close();
                checkPs.close();
                conn.close();
                
            } catch (SQLException ex) {
                ex.printStackTrace();
                showMessage("Failed to change password: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    
    private JPanel createCardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(BG_LIGHT);
        panel.setBorder(new CompoundBorder(
                new EmptyBorder(26, 26, 26, 26),
                new LineBorder(ACCENT_SECONDARY, 1, true)
        ));
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
                new EmptyBorder(8, 12, 8, 12)
        ));
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
                new EmptyBorder(8, 12, 8, 12)
        ));
        field.putClientProperty("JTextField.placeholderText", placeholder);
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
            public void mouseEntered(MouseEvent e) { btn.setBackground(BTN_BG_HOVER);}
            public void mouseExited(MouseEvent e) { btn.setBackground(BTN_BG);}
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
            public void mouseEntered(MouseEvent e) { btn.setBackground(ACCENT_PRIMARY); btn.setForeground(Color.WHITE);}
            public void mouseExited(MouseEvent e) { btn.setBackground(Color.WHITE); btn.setForeground(ACCENT_PRIMARY);}
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
            public void mouseEntered(MouseEvent e) { btn.setBackground(ACCENT_SECONDARY); }
            public void mouseExited(MouseEvent e) { btn.setBackground(ACCENT_PRIMARY);}
        });
        return btn;
    }

    private JPanel createStatBox(String title, String value) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new CompoundBorder(
                new LineBorder(ACCENT_SECONDARY, 1, true),
                new EmptyBorder(16, 12, 16, 12)
        ));
        panel.setMaximumSize(new Dimension(220, 90));

        JLabel t = new JLabel(title, SwingConstants.CENTER);
        t.setFont(new Font("Segoe UI", Font.BOLD, 15));
        t.setForeground(ACCENT_PRIMARY);

        JLabel v = new JLabel(value, SwingConstants.CENTER);
        v.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        v.setForeground(TEXT_DARK);

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

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, v, isSelected, hasFocus, row, col);
                c.setBackground(isSelected ? ACCENT_SECONDARY : (row % 2 == 0 ? BG_LIGHT : Color.WHITE));
                c.setForeground(TEXT_BODY);
                setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        });
    }

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
            if (comp instanceof JScrollPane) return (JScrollPane) comp;
            if (comp instanceof Container) {
                JScrollPane found = findScrollPane((Container) comp);
                if (found != null) return found;
            }
        }
        return null;
    }

    private void loadGradesIntoTable(DefaultTableModel tm) {
        if (currentStudentId == -1) return;
        loadStudentDataStructures();
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
        if (currentStudentId == -1) return;
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
                    if (recentGradesQueue.size() > 5) recentGradesQueue.poll();

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
            if (res != null) img = ImageIO.read(res);
            else {
                File f = new File("." + path);
                if (f.exists()) img = ImageIO.read(f);
            }
            if (img != null) return new ImageIcon(img.getScaledInstance(w, h, Image.SCALE_SMOOTH));
        } catch (Exception e) {}
        return null;
    }

    private ImageIcon imageFromBytes(byte[] bytes, int w, int h) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
            BufferedImage img = ImageIO.read(bais);
            if (img == null) return null;
            return new ImageIcon(img.getScaledInstance(w, h, Image.SCALE_SMOOTH));
        } catch (Exception ex) {ex.printStackTrace();}
        return null;
    }

    // =================== BORDER CLASSES ===================
    private static class RoundedBorder extends LineBorder {
        private final int radius;
        public RoundedBorder(int r, Color c) { super(c, 2, true); this.radius = r;}
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            g.setColor(lineColor);
            g.drawRoundRect(x, y, w-1, h-1, radius, radius);
        }
    }
    
    private static class RoundedOutlineBorder extends LineBorder {
        private final int radius;
        public RoundedOutlineBorder(int r, Color c) { super(c, 2, true); this.radius = r;}
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            g.setColor(lineColor);
            g.drawRoundRect(x, y, w-1, h-1, radius, radius);
        }
    }

    // =================== DATA STRUCTURE LOADER & HELPERS ===================
    private static class GradeRecord {
        String courseCode;
        String courseName;
        Double prelim;
        Double midterm;
        Double finals;
        String remarks;

        GradeRecord(String courseCode, String courseName, Double prelim, Double midterm, Double finals, String remarks) {
            this.courseCode = courseCode;
            this.courseName = courseName;
            this.prelim = prelim;
            this.midterm = midterm;
            this.finals = finals;
            this.remarks = remarks;
        }
    }

    private void loadStudentDataStructures() {
        if (currentStudentId == -1) return;

        gradeRecords = new ArrayList<>();
        gradesPerTerm.clear();
        enrolledCourses.clear();

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

                    if (code != null && !enrolledCourses.contains(code)) {
                        enrolledCourses.add(code);
                    }

                    Double effective = finals != null ? finals : (midterm != null ? midterm : prelim);
                    if (effective != null) gradesPerTerm.add(effective);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void updateDashboardStats() {
        if (lblGwaStat != null) {
            if (gradesPerTerm.isEmpty()) {
                lblGwaStat.setText("—");
            } else {
                double sum = 0.0;
                for (Double d : gradesPerTerm) sum += d;
                double gwa = sum / gradesPerTerm.size();
                lblGwaStat.setText(String.format(Locale.ENGLISH, "%.2f", gwa));
            }
        }
        if (lblCoursesStat != null) {
            lblCoursesStat.setText(enrolledCourses.isEmpty() ? "0" : String.valueOf(enrolledCourses.size()));
        }
    }

    private void repopulateGradesTableFromRecords() {
        if (gradesTableModel == null) return;
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

    private static Double finalComparableNullsLast(GradeRecord gr) {
        if (gr == null) return Double.NEGATIVE_INFINITY;
        if (gr.finals != null) return gr.finals;
        if (gr.midterm != null) return gr.midterm;
        if (gr.prelim != null) return gr.prelim;
        return Double.NEGATIVE_INFINITY;
    }

    private static <T> List<T> mergeSort(List<T> src, Comparator<T> cmp) {
        if (src.size() <= 1) return new ArrayList<>(src);
        int mid = src.size() / 2;
        List<T> left = mergeSort(src.subList(0, mid), cmp);
        List<T> right = mergeSort(src.subList(mid, src.size()), cmp);
        return merge(left, right, cmp);
    }
    
    private static <T> List<T> merge(List<T> left, List<T> right, Comparator<T> cmp) {
        List<T> out = new ArrayList<>(left.size() + right.size());
        int i = 0, j = 0;
        while (i < left.size() && j < right.size()) {
            if (cmp.compare(left.get(i), right.get(j)) <= 0) out.add(left.get(i++));
            else out.add(right.get(j++));
        }
        while (i < left.size()) out.add(left.get(i++));
        while (j < right.size()) out.add(right.get(j++));
        return out;
    }

    // =================== MAIN ===================
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); } catch (Exception e) {}
        SwingUtilities.invokeLater(() -> new MainApp().setVisible(true));
    }
}