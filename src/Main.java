import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class Main {
    static final String DB_URL  = "jdbc:mysql://localhost:3306/employee";
    static final String DB_USER = "root";
    static final String DB_PASS = "sayantan@123";

    public static void main(String[] args) {

        initializeDatabase();

        SwingUtilities.invokeLater(() -> {

            JFrame frame = new JFrame("Employee Department Manager");
            frame.setSize(600, 420);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout(5, 5));

            JPanel inputPanel = new JPanel(new GridLayout(3, 2, 5, 5));
            inputPanel.setBorder(BorderFactory.createTitledBorder("Department Transfer"));

            JTextField txtOldDept = new JTextField("IT");
            JTextField txtNewDept = new JTextField("HR");

            inputPanel.add(new JLabel("From Department:"));
            inputPanel.add(txtOldDept);
            inputPanel.add(new JLabel("To Department:"));
            inputPanel.add(txtNewDept);

            JButton btnLoad    = new JButton("Load Employees");
            JButton btnExecute = new JButton("Update Department");
            inputPanel.add(btnLoad);
            inputPanel.add(btnExecute);

            frame.add(inputPanel, BorderLayout.NORTH);

            DefaultTableModel model = new DefaultTableModel(
                    new String[]{"ID", "Name", "Department"}, 0);
            frame.add(new JScrollPane(new JTable(model)), BorderLayout.CENTER);

            btnLoad.addActionListener(e -> {
                model.setRowCount(0);
                String dept = txtOldDept.getText().trim();

                try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                     PreparedStatement ps = conn.prepareStatement(
                             "SELECT id, name, department FROM employees WHERE department = ?")) {

                    ps.setString(1, dept);
                    ResultSet rs = ps.executeQuery();

                    while (rs.next()) {
                        model.addRow(new Object[]{
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("department")
                        });
                    }

                    if (model.getRowCount() == 0)
                        JOptionPane.showMessageDialog(frame, "No employees found in '" + dept + "'.");

                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(frame, "Load error: " + ex.getMessage());
                }
            });
            btnExecute.addActionListener(e -> {
                String oldDept = txtOldDept.getText().trim();
                String newDept = txtNewDept.getText().trim();

                try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                    conn.setAutoCommit(false);   

                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE employees SET department = ? WHERE department = ?")) {

                        ps.setString(1, newDept);
                        ps.setString(2, oldDept);
                        int rows = ps.executeUpdate();

                        if (rows == 0)
                            throw new SQLException("No employees found in '" + oldDept + "'.");

                        conn.commit();  
                        JOptionPane.showMessageDialog(frame,
                                rows + " employee(s) moved from '" + oldDept + "' → '" + newDept + "'.");
                        btnLoad.doClick(); 

                    } catch (SQLException ex) {
                        conn.rollback(); 
                        JOptionPane.showMessageDialog(frame,
                                "Transaction rolled back.\nReason: " + ex.getMessage());
                    }

                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(frame, "Connection error: " + ex.getMessage());
                }
            });

            frame.setVisible(true);
        });
    }

    private static void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS employees (" +
                "  id         INT PRIMARY KEY AUTO_INCREMENT," +
                "  name       VARCHAR(50)," +
                "  department VARCHAR(50)" +
                ")"
            );
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM employees");
            rs.next();
            if (rs.getInt(1) == 0) {
                stmt.executeUpdate("INSERT INTO employees (name, department) VALUES ('Sayantan', 'IT')");
                stmt.executeUpdate("INSERT INTO employees (name, department) VALUES ('tuya',    'IT')");
                stmt.executeUpdate("INSERT INTO employees (name, department) VALUES ('kuntal',   'HR')");
            }

        } catch (SQLException ex) {
            System.err.println("DB init error: " + ex.getMessage());
        }
    }
}