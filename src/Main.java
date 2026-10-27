

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class Main {
    static final String DB_URL =
    System.getenv().getOrDefault(
        "ORACLE_DB_URL",
        "jdbc:oracle:thin:@//localhost:1521/XE"
    );
    static final String DB_USER = "system";
    static final String DB_PASS = "hsmCvdijpu1@";

    public static void main(String[] args) {
        try {
            Class.forName("oracle.jdbc.OracleDriver");
        } catch (ClassNotFoundException e) {
            System.out.println("Oracle JDBC Driver not found!");
        }

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Employee Department Transaction Manager - RA2511003010796");
            frame.setSize(650, 450);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            JPanel inputPanel = new JPanel(new GridLayout(3, 2, 5, 5));
            inputPanel.setBorder(BorderFactory.createTitledBorder("Transaction Parameters"));

            JTextField txtOldDept = new JTextField("IT");
            JTextField txtNewDept = new JTextField("HR");

            inputPanel.add(new JLabel("Current Department (to select):"));
            inputPanel.add(txtOldDept);
            inputPanel.add(new JLabel("New Department (to update to):"));
            inputPanel.add(txtNewDept);

            JButton btnLoad = new JButton("1. Load Employees");
            inputPanel.add(btnLoad);
            
            JButton btnExecuteTx = new JButton("2. Execute Transaction (Update & Commit/Rollback)");
            inputPanel.add(btnExecuteTx);

            frame.add(inputPanel, BorderLayout.NORTH);

            String[] columns = {"ID", "Name", "Department"};
            DefaultTableModel tableModel = new DefaultTableModel(columns, 0);
            JTable table = new JTable(tableModel);
            frame.add(new JScrollPane(table), BorderLayout.CENTER);

            initializeDatabase();

            btnLoad.addActionListener(e -> {
                tableModel.setRowCount(0);
                String oldDept = txtOldDept.getText().trim();
                String sql = "SELECT id, name, department FROM employees WHERE department = ?";

                try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    
                    pstmt.setString(1, oldDept);
                    ResultSet rs = pstmt.executeQuery();

                    while (rs.next()) {
                        tableModel.addRow(new Object[]{
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("department")
                        });
                    }
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(frame, "Load Error: " + ex.getMessage());
                }
            });

            btnExecuteTx.addActionListener(e -> {
                String oldDept = txtOldDept.getText().trim();
                String newDept = txtNewDept.getText().trim();

                Connection conn = null;
                try {
                    conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                    conn.setAutoCommit(false);

                    String updateSql = "UPDATE employees SET department = ? WHERE department = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                        pstmt.setString(1, newDept);
                        pstmt.setString(2, oldDept);
                        
                        int rowsUpdated = pstmt.executeUpdate();

                        if (rowsUpdated == 0) {
                            throw new SQLException("No employees found in department '" + oldDept + "' to update!");
                        }

                        conn.commit();
                        JOptionPane.showMessageDialog(frame, "Transaction Successful! " + rowsUpdated + " employee(s) updated and committed.");
                        btnLoad.doClick();

                    } catch (SQLException innerEx) {
                        if (conn != null) { conn.rollback(); }
                        JOptionPane.showMessageDialog(frame, "Transaction Failed! Changes rolled back.\nReason: " + innerEx.getMessage());
                    }

                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(frame, "Database Connection Error: " + ex.getMessage());
                } finally {
                    if (conn != null) {
                        try { conn.close(); } catch (SQLException ignored) {}
                    }
                }
            });

            frame.setVisible(true);
        });
    }

    private static void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement()) {
            try {
                stmt.executeUpdate("CREATE TABLE employees (id NUMBER PRIMARY KEY, name VARCHAR2(50), department VARCHAR2(50))");
                stmt.executeUpdate("INSERT INTO employees VALUES (1, 'Sayantan', 'IT')");
                stmt.executeUpdate("INSERT INTO employees VALUES (2, 'Rahul', 'IT')");
                stmt.executeUpdate("INSERT INTO employees VALUES (3, 'Ananya', 'HR')");
                conn.commit();
            } catch (SQLException ignored) {}
        } catch (SQLException ignored) {}
    }
}