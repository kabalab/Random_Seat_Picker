import java.awt.*;
import java.awt.event.*;
import java.io.*;
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
    private static int seatIndex = 0;
    private static final String[] leftRows  = {"A","B","C","D","E"};
    private static final String[] rightRows = {"F","G","H","I"};

    // Names
    private static final java.util.List<NameEntry> namePool = new ArrayList<>();
    private static int nameIndex = 0;
    private static final File CSV_FILE = new File("charts.csv");

    // Mode
    private static boolean editMode = false;

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
        JButton settingsButton = makeTxtButton("Seat Settings", new Color(120, 120, 120));
        JButton namesButton = makeTxtButton("Names", new Color(90, 150, 120));
        JButton editButton = makeTxtButton("Edit Chart", new Color(255, 140, 60));

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
        settingsButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        namesButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        editButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(titleLabel);
        card.add(Box.createVerticalStrut(20));
        card.add(resultLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(counterLabel);
        card.add(Box.createVerticalStrut(25));
        card.add(randomSeatButton);
        card.add(Box.createVerticalStrut(10));
        card.add(settingsButton);
        card.add(Box.createVerticalStrut(10));
        card.add(namesButton);
        card.add(Box.createVerticalStrut(10));
        card.add(editButton);

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

    /* ================= SEAT SETTINGS ================= */
    private static void openSeatSettings() {
        JFrame settingsFrame = new JFrame("Seat Settings");
        settingsFrame.setSize(600, 420);
        settingsFrame.setLocationRelativeTo(null);
        settingsFrame.getContentPane().setBackground(new Color(30, 30, 40));

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(45, 45, 60));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Toggle Active Seats", JLabel.CENTER);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));

        panel.add(title, BorderLayout.NORTH);
        panel.add(createSeatGrid(), BorderLayout.CENTER);

        settingsFrame.add(panel);
        settingsFrame.setVisible(true);
    }

    /* ================= SEAT GRID ================= */
    private static JPanel createSeatGrid() {
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
        JToggleButton btn = new JToggleButton(seat);
        btn.setFocusPainted(false);
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(90, 90, 110));
        btn.setSelected(disabledSeats.contains(seat));
        if (btn.isSelected()) btn.setBackground(new Color(120, 60, 60));

        btn.addItemListener(e -> {
            if (btn.isSelected()) disabledSeats.add(seat);
            else disabledSeats.remove(seat);
            initSeats();
        });

        return btn;
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
            resultLabel.setText(student + " → " + seat);
            namePool.get(nameIndex - 1).assignedSeat = seat;
            saveNames();
        }
        if (name.equals("Seat Settings")) openSeatSettings();
        if (name.equals("Names")) openNameEditor();
        if (name.equals("Edit Chart")) openSeatingEditor();
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

    /* ================= NAME MANAGEMENT ================= */
    private static void openNameEditor() {
        JFrame frame = new JFrame("Student Names");
        frame.setSize(400, 450);
        frame.setLocationRelativeTo(null);

        DefaultListModel<String> model = new DefaultListModel<>();
        namePool.forEach(n -> model.addElement(n.name));

        JList<String> list = new JList<>(model);
        JScrollPane scroll = new JScrollPane(list);

        JTextField input = new JTextField();
        JButton add = new JButton("Add");
        JButton remove = new JButton("Remove");

        add.addActionListener(e -> {
            String text = input.getText().trim();
            if (!text.isEmpty()) {
                model.addElement(text);
                input.setText("");
                saveNames(model);
            }
        });

        remove.addActionListener(e -> {
            int idx = list.getSelectedIndex();
            if (idx >= 0) {
                model.remove(idx);
                saveNames(model);
            }
        });

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(input, BorderLayout.CENTER);
        JPanel btns = new JPanel();
        btns.add(add);
        btns.add(remove);
        bottom.add(btns, BorderLayout.EAST);

        frame.add(scroll, BorderLayout.CENTER);
        frame.add(bottom, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    private static void saveNames(DefaultListModel<String> model) {
        namePool.clear();
        try (PrintWriter pw = new PrintWriter(new FileWriter(CSV_FILE))) {
            pw.println("Name,Seat");
            for (int i = 0; i < model.size(); i++) {
                pw.println(model.get(i) + ",");
                namePool.add(new NameEntry(model.get(i)));
            }
        } catch (IOException e) { e.printStackTrace(); }
        Collections.shuffle(namePool);
        nameIndex = 0;
    }

    private static void saveNames() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(CSV_FILE))) {
            pw.println("Name,Seat");
            for (NameEntry n : namePool) {
                pw.println(n.name + "," + (n.assignedSeat != null ? n.assignedSeat : ""));
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    private static void loadNames() {
        namePool.clear();
        if (!CSV_FILE.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(CSV_FILE))) {
            br.readLine(); // skip header
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",", -1);
                if (parts.length >= 1 && !parts[0].trim().isEmpty())
                    namePool.add(new NameEntry(parts[0].trim(), parts.length > 1 ? parts[1].trim() : null));
            }
        } catch (IOException e) { e.printStackTrace(); }
        Collections.shuffle(namePool);
        nameIndex = 0;
    }

    private static String getNextName() {
        if (namePool.isEmpty()) return "No name";
        if (nameIndex >= namePool.size()) { Collections.shuffle(namePool); nameIndex = 0; }
        return namePool.get(nameIndex++).name;
    }

    /* ================= EDITABLE SEATING CHART ================= */
    private static void openSeatingEditor() {
        JFrame frame = new JFrame("Seating Chart Editor");
        frame.setSize(700, 500);
        frame.setLocationRelativeTo(null);

        JPanel grid = new JPanel(new GridLayout(leftRows.length, 9, 10, 10));
        grid.setBackground(new Color(45, 45, 60));
        Map<String, JButton> seatButtons = new HashMap<>();

        for (int i = 0; i < leftRows.length; i++) {
            for (int n = 4; n >= 1; n--) addEditableSeat(grid, seatButtons, leftRows[i]+n);
            grid.add(new JLabel());
            if (i < rightRows.length) for (int n = 1; n <= 4; n++) addEditableSeat(grid, seatButtons, rightRows[i]+n);
            else for (int n = 1; n <= 4; n++) grid.add(new JLabel());
        }

        JButton saveBtn = new JButton("Save Chart");
        saveBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            if (chooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION)
                saveSeatingChart(chooser.getSelectedFile(), seatButtons);
        });

        JButton loadBtn = new JButton("Load Chart");
        loadBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION)
                loadSeatingChart(chooser.getSelectedFile(), seatButtons);
        });

        JPanel bottom = new JPanel();
        bottom.add(saveBtn);
        bottom.add(loadBtn);

        frame.add(grid, BorderLayout.CENTER);
        frame.add(bottom, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    private static void addEditableSeat(JPanel grid, Map<String,JButton> map, String seat) {
        JButton btn = new JButton(seat);
        btn.setBackground(new Color(90,90,110));
        btn.setForeground(Color.WHITE);
        btn.addActionListener(e -> {
            String name = JOptionPane.showInputDialog("Enter name for seat " + seat);
            if (name != null) btn.setText(name.isEmpty() ? seat : name);
        });
        grid.add(btn);
        map.put(seat, btn);
    }

    private static void saveSeatingChart(File file, Map<String,JButton> seats) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("Seat,Name");
            for (String seat : seats.keySet()) {
                String name = seats.get(seat).getText();
                if (name.equals(seat)) name = "";
                pw.println(seat + "," + name);
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    private static void loadSeatingChart(File file, Map<String,JButton> seats) {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",", -1);
                if (parts.length >= 2 && seats.containsKey(parts[0])) {
                    seats.get(parts[0]).setText(parts[1].isEmpty() ? parts[0] : parts[1]);
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    /* ================= HELPER ================= */
    static class NameEntry {
        String name;
        String assignedSeat;
        NameEntry(String n) { name = n; }
        NameEntry(String n, String s) { name = n; assignedSeat = s; }
    }
}
