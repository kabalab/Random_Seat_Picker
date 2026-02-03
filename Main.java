import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.time.LocalDate;
import java.util.*;
import javax.swing.*;

public class Main {

    // UI components
    private static JLabel resultLabel;
    private static JLabel counterLabel;
    private static JLabel titleLabel;
    private static JButton randomSeatButton;

    // Seats
    private static final java.util.List<String> seatPool = new ArrayList<>();
    private static final Set<String> disabledSeats = new HashSet<>();
    private static final Set<String> tardySeats = new HashSet<>();
    private static int seatIndex = 0;
    private static final String[] leftRows  = {"A","B","C","D","E"};
    private static final String[] rightRows = {"F","G","H","I"};

    private static final Color BASE_SEAT_COLOR = new Color(90, 90, 110);
    private static final Color TARDY_SEAT_COLOR = new Color(160, 60, 60);

    // Names
    private static final java.util.List<NameEntry> namePool = new ArrayList<>();
    private static int nameIndex = 0;
    private static final File CSV_FILE = new File("charts.csv");

    // Labels for seats (used by Seat Settings + Edit Chart)
    private static final Map<String, String> seatLabels = new HashMap<>();
    private static final Map<String, JToggleButton> seatToggleButtons = new HashMap<>();

    // Single-instance windows
    private static JFrame seatSettingsFrame;
    private static JFrame seatingEditorFrame;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::createAndShowUi);
    }

    /* ================= MAIN WINDOW ================= */
    private static void createAndShowUi() {
        JFrame frame = new JFrame("Random Seat Picker");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setBackground(new Color(30, 30, 40));

        titleLabel = new JLabel("Random Seat Picker", JLabel.CENTER);
        titleLabel.setForeground(Color.WHITE);

        resultLabel = new JLabel("Click to pick a seat", JLabel.CENTER);
        resultLabel.setForeground(new Color(220, 220, 220));

        counterLabel = new JLabel("", JLabel.CENTER);
        counterLabel.setForeground(new Color(180, 180, 180));

        randomSeatButton = makeTxtButton("Pick Seat", new Color(72, 99, 255));
        JButton menuButton = makeTxtButton("Menu", new Color(120, 120, 120));

        loadNames();
        initSeats();

        JPanel card = new JPanel();
        card.setBackground(new Color(45, 45, 60));
        card.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        resultLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        counterLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        randomSeatButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        menuButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(titleLabel);
        card.add(Box.createVerticalStrut(20));
        card.add(resultLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(counterLabel);
        card.add(Box.createVerticalStrut(25));
        card.add(randomSeatButton);
        card.add(Box.createVerticalStrut(10));
        card.add(menuButton);

        menuButton.addActionListener(e -> showMenu(menuButton));

        frame.add(card);
        frame.setSize(460, 450);
        frame.setLocationRelativeTo(null);

        updateFonts(frame.getWidth());
        frame.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                updateFonts(frame.getWidth());
            }
        });

        frame.setVisible(true);
    }

    private static void showMenu(Component anchor) {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem seatSettings = new JMenuItem("Seat Settings");
        seatSettings.addActionListener(e -> openSeatSettings());

        JMenuItem editChart = new JMenuItem("Edit Chart");
        editChart.addActionListener(e -> openSeatingEditor());

        JMenuItem exportAttendance = new JMenuItem("Export Attendance");
        exportAttendance.addActionListener(e -> exportAttendance());

        menu.add(seatSettings);
        menu.add(editChart);
        menu.add(exportAttendance);

        menu.show(anchor, 0, anchor.getHeight());
    }

    private static void exportAttendance() {
        JFileChooser chooser = new JFileChooser();
        String date = LocalDate.now().toString();
        chooser.setSelectedFile(new File("Attendence_" + date + "_P.txt"));

        if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        ensureSeatLabels();

        java.util.List<String> absentNames = new ArrayList<>();
        java.util.List<String> tardyNames = new ArrayList<>();
        java.util.List<String> presentNames = new ArrayList<>();

        for (String seat : seatLabels.keySet()) {
            String name = getSeatLabel(seat);
            if (name == null || name.trim().isEmpty() || name.equals(seat)) {
                continue;
            }

            if (disabledSeats.contains(seat)) {
                absentNames.add(name.trim());
            } else if (tardySeats.contains(seat)) {
                tardyNames.add(name.trim());
            } else {
                presentNames.add(name.trim());
            }
        }

        Collections.sort(absentNames);
        Collections.sort(tardyNames);
        Collections.sort(presentNames);

        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("Absent:");
            pw.println();
            if (!absentNames.isEmpty()) {
                pw.println(String.join(", ", absentNames));
            }
            pw.println();
            pw.println("Tardy:");
            pw.println();
            if (!tardyNames.isEmpty()) {
                pw.println(String.join(", ", tardyNames));
            }
            pw.println();
            pw.println("Present:");
            pw.println();
            if (!presentNames.isEmpty()) {
                pw.println(String.join(", ", presentNames));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* ================= SEAT SETTINGS ================= */
    private static void openSeatSettings() {
        if (seatSettingsFrame != null) {
            seatSettingsFrame.dispose();
        }

        seatSettingsFrame = new JFrame("Seat Settings");
        seatSettingsFrame.setSize(600, 420);
        seatSettingsFrame.setLocationRelativeTo(null);
        seatSettingsFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        seatSettingsFrame.getContentPane().setBackground(new Color(30, 30, 40));

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(45, 45, 60));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Toggle Active Seats", JLabel.CENTER);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));

        panel.add(title, BorderLayout.NORTH);
        panel.add(createSeatGrid(), BorderLayout.CENTER);

        seatSettingsFrame.add(panel);
        seatSettingsFrame.setVisible(true);

        seatSettingsFrame.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                seatSettingsFrame = null;
            }
        });
    }

    /* ================= SEAT GRID ================= */
    private static JPanel createSeatGrid() {
        ensureSeatLabels();
        seatToggleButtons.clear();

        JPanel grid = new JPanel(new GridLayout(0, 9, 10, 10));
        grid.setBackground(new Color(45, 45, 60));

        for (int i = 0; i < leftRows.length; i++) {
            for (int n = 4; n >= 1; n--) grid.add(createSeatToggle(leftRows[i] + n));
            grid.add(new JLabel()); // aisle
            if (i < rightRows.length) for (int n = 1; n <= 4; n++) grid.add(createSeatToggle(rightRows[i] + n));
            else for (int n = 1; n <= 4; n++) grid.add(new JLabel());
        }

        return grid;
    }

    private static JToggleButton createSeatToggle(String seat) {
        JToggleButton btn = new JToggleButton(getSeatLabel(seat));
        btn.setFocusPainted(false);
        btn.setForeground(Color.WHITE);
        btn.setBackground(BASE_SEAT_COLOR);
        btn.setSelected(disabledSeats.contains(seat));

        if (!btn.isSelected() && tardySeats.contains(seat)) {
            btn.setBackground(TARDY_SEAT_COLOR);
        } else {
            btn.setBackground(BASE_SEAT_COLOR);
        }

        btn.addItemListener(e -> {
            if (btn.isSelected()) {
                disabledSeats.add(seat);
                tardySeats.remove(seat);
                btn.setBackground(BASE_SEAT_COLOR);
            } else {
                disabledSeats.remove(seat);
                handleReenabledSeatPrompt(seat, btn);
            }
            initSeats();
        });

        seatToggleButtons.put(seat, btn);
        return btn;
    }

    private static void handleReenabledSeatPrompt(String seat, JToggleButton btn) {
        String label = getSeatLabel(seat);
        if (label == null || label.trim().isEmpty() || label.equals(seat)) {
            tardySeats.remove(seat);
            btn.setBackground(BASE_SEAT_COLOR);
            return;
        }

        String[] options = {"Present", "Tardy"};
        int choice = JOptionPane.showOptionDialog(
            btn,
            "Mark " + label + " as:",
            "Attendance",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]
        );

        if (choice == 1) {
            tardySeats.add(seat);
            btn.setBackground(TARDY_SEAT_COLOR);
        } else {
            tardySeats.remove(seat);
            btn.setBackground(BASE_SEAT_COLOR);
        }
    }

    /* ================= SEAT LABEL HELPERS ================= */
    private static void ensureSeatLabels() {
        for (int i = 0; i < leftRows.length; i++) {
            for (int n = 1; n <= 4; n++) {
                String leftSeat = leftRows[i] + n;
                seatLabels.putIfAbsent(leftSeat, leftSeat);
                if (i < rightRows.length) {
                    String rightSeat = rightRows[i] + n;
                    seatLabels.putIfAbsent(rightSeat, rightSeat);
                }
            }
        }
    }

    private static String getSeatLabel(String seat) {
        return seatLabels.getOrDefault(seat, seat);
    }

    private static void updateSeatLabel(String seat, String label) {
        String normalized = (label == null || label.trim().isEmpty()) ? seat : label.trim();
        seatLabels.put(seat, normalized);

        JToggleButton toggle = seatToggleButtons.get(seat);
        if (toggle != null) {
            toggle.setText(normalized);
        }
    }

    private static void refreshSeatSettingsToggles() {
        for (Map.Entry<String, JToggleButton> entry : seatToggleButtons.entrySet()) {
            String seat = entry.getKey();
            JToggleButton toggle = entry.getValue();
            toggle.setSelected(disabledSeats.contains(seat));
            toggle.setText(getSeatLabel(seat));

            if (disabledSeats.contains(seat)) {
                toggle.setBackground(BASE_SEAT_COLOR);
            } else if (tardySeats.contains(seat)) {
                toggle.setBackground(TARDY_SEAT_COLOR);
            } else {
                toggle.setBackground(BASE_SEAT_COLOR);
            }
        }
    }

    /* ================= SEAT LOGIC ================= */
    private static void initSeats() {
        seatPool.clear();
        for (int i = 0; i < leftRows.length; i++) {
            for (int n = 1; n <= 4; n++) {
                String leftSeat = leftRows[i] + n;
                if (!disabledSeats.contains(leftSeat)) seatPool.add(leftSeat);
                if (i < rightRows.length) {
                    String rightSeat = rightRows[i] + n;
                    if (!disabledSeats.contains(rightSeat)) seatPool.add(rightSeat);
                }
            }
        }
        Collections.shuffle(seatPool);
        seatIndex = 0;
        updateCounter();
    }

    private static String getNextSeat() {
        if (seatPool.isEmpty()) return "No seats";
        if (seatIndex >= seatPool.size()) initSeats();
        updateCounter();
        return seatPool.get(seatIndex++);
    }

    private static void updateCounter() {
        counterLabel.setText("Remaining seats: " + Math.max(0, seatPool.size() - seatIndex));
    }

    /* ================= BUTTON HANDLER ================= */
    private static void handler(String name) {
        if (name.equals("Pick Seat")) {
            String student = getNextName();
            String seat = getNextSeat();
            String seatLabel = getSeatLabel(seat);

            if ("No name".equals(student)) {
                resultLabel.setText(seatLabel);
            } else {
                resultLabel.setText(student + " → " + seatLabel);
            }

            if (!namePool.isEmpty() && nameIndex > 0) {
                namePool.get(nameIndex - 1).assignedSeat = seat;
                saveNames();
            }
        }
    }

    /* ================= BUTTON CREATION ================= */
    public static JButton makeTxtButton(String txt, Color clr) {
        JButton btn = new JButton(txt);
        btn.setFont(new Font("SansSerif", Font.BOLD, 16));
        btn.setBackground(clr);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(160, 50));
        btn.addActionListener(e -> handler(txt));
        return btn;
    }

    /* ================= FONT SCALING ================= */
    private static void updateFonts(int width) {
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, Math.max(20, width / 14)));
        resultLabel.setFont(new Font("SansSerif", Font.BOLD, Math.max(18, width / 18)));
        counterLabel.setFont(new Font("SansSerif", Font.PLAIN, Math.max(14, width / 26)));
        randomSeatButton.setFont(new Font("SansSerif", Font.BOLD, Math.max(14, width / 22)));
    }

    /* ================= NAME STORAGE ================= */
    private static void saveNames() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(CSV_FILE))) {
            pw.println("Name,Seat");
            for (NameEntry n : namePool) {
                pw.println(n.name + "," + (n.assignedSeat != null ? n.assignedSeat : ""));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadNames() {
        namePool.clear();
        if (!CSV_FILE.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(CSV_FILE))) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",", -1);
                if (!parts[0].trim().isEmpty()) {
                    namePool.add(new NameEntry(parts[0].trim(), parts.length > 1 ? parts[1].trim() : null));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Collections.shuffle(namePool);
        nameIndex = 0;
    }

    private static String getNextName() {
        if (namePool.isEmpty()) return "No name";
        if (nameIndex >= namePool.size()) {
            Collections.shuffle(namePool);
            nameIndex = 0;
        }
        return namePool.get(nameIndex++).name;
    }

    /* ================= EDITABLE SEATING CHART ================= */
    private static void openSeatingEditor() {
        if (seatingEditorFrame != null) {
            seatingEditorFrame.dispose();
        }

        seatingEditorFrame = new JFrame("Seating Chart Editor");
        seatingEditorFrame.setSize(700, 500);
        seatingEditorFrame.setLocationRelativeTo(null);

        JPanel grid = new JPanel(new GridLayout(leftRows.length, 9, 10, 10));
        grid.setBackground(new Color(45, 45, 60));
        Map<String, JButton> seatButtons = new HashMap<>();

        for (int i = 0; i < leftRows.length; i++) {
            for (int n = 4; n >= 1; n--) addEditableSeat(grid, seatButtons, leftRows[i] + n);
            grid.add(new JLabel());
            if (i < rightRows.length) {
                for (int n = 1; n <= 4; n++) addEditableSeat(grid, seatButtons, rightRows[i] + n);
            } else {
                for (int n = 1; n <= 4; n++) grid.add(new JLabel());
            }
        }

        JButton saveBtn = new JButton("Save Chart");
        saveBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            if (chooser.showSaveDialog(seatingEditorFrame) == JFileChooser.APPROVE_OPTION) {
                saveSeatingChart(chooser.getSelectedFile(), seatButtons);
            }
        });

        JButton loadBtn = new JButton("Load Chart");
        loadBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            if (chooser.showOpenDialog(seatingEditorFrame) == JFileChooser.APPROVE_OPTION) {
                loadSeatingChart(chooser.getSelectedFile(), seatButtons);
            }
        });

        JPanel bottom = new JPanel();
        bottom.add(saveBtn);
        bottom.add(loadBtn);

        seatingEditorFrame.add(grid, BorderLayout.CENTER);
        seatingEditorFrame.add(bottom, BorderLayout.SOUTH);
        seatingEditorFrame.setVisible(true);

        seatingEditorFrame.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                seatingEditorFrame = null;
            }
        });
    }

    private static void addEditableSeat(JPanel grid, Map<String, JButton> map, String seat) {
        JButton btn = new JButton(getSeatLabel(seat));
        btn.setBackground(new Color(90, 90, 110));
        btn.setForeground(Color.WHITE);
        btn.addActionListener(e -> {
            String name = JOptionPane.showInputDialog("Enter name for seat " + seat);
            if (name != null) {
                updateSeatLabel(seat, name);
                btn.setText(getSeatLabel(seat));
            }
        });
        grid.add(btn);
        map.put(seat, btn);
    }

    private static void saveSeatingChart(File file, Map<String, JButton> seats) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("Seat,Enabled,Name");
            for (String seat : seats.keySet()) {
                String name = getSeatLabel(seat);
                if (name.equals(seat)) name = "";
                boolean enabled = !disabledSeats.contains(seat);
                pw.println(seat + "," + enabled + "," + name);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadSeatingChart(File file, Map<String, JButton> seats) {
        tardySeats.clear();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",", -1);
                if (parts.length >= 3 && seats.containsKey(parts[0])) {
                    String seat = parts[0];
                    boolean enabled = Boolean.parseBoolean(parts[1].trim());
                    String name = parts[2].trim();

                    if (enabled) disabledSeats.remove(seat);
                    else disabledSeats.add(seat);

                    updateSeatLabel(seat, name);
                    seats.get(seat).setText(getSeatLabel(seat));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        initSeats();
        refreshSeatSettingsToggles();
    }

    static class NameEntry {
        String name;
        String assignedSeat;

        NameEntry(String n) {
            name = n;
        }

        NameEntry(String n, String s) {
            name = n;
            assignedSeat = s;
        }
    }
}
