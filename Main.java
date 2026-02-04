import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
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
    private static final Color DISABLED_SEAT_COLOR = new Color(65, 65, 80);
    private static final Color SWAP_SELECTED_COLOR = new Color(120, 120, 160);

    // Names
    private static final java.util.List<NameEntry> namePool = new ArrayList<>();
    private static int nameIndex = 0;

    // Labels for seats (used by Seat Settings + Edit Chart)
    private static final Map<String, String> seatLabels = new HashMap<>();
    private static final Map<String, JToggleButton> seatToggleButtons = new HashMap<>();
    private static final Map<String, JButton> seatEditButtons = new HashMap<>();
    private static final Map<String, JButton> seatSwapButtons = new HashMap<>();
    private static String swapSelectedSeat;

    // Combined Seat Manager window
    private static JFrame seatManagerFrame;
    private static JLabel seatManagerTitle;
    private static JPanel seatManagerGridPanel;
    private static JButton seatManagerEditButton;
    private static JButton seatManagerSwapButton;

    private enum SeatManagerMode {
        SETTINGS, EDIT, SWAP
    }

    private static SeatManagerMode seatManagerMode = SeatManagerMode.SETTINGS;

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
        JButton menuButton = makeTxtButton("Menu", new Color(120, 120, 120), new Dimension(160, 50), 16, false);

        boolean loaded = autoLoadSeatingChart();
        if (!loaded) {
            initSeats();
        }

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

        JMenuItem chart = new JMenuItem("Chart");
        chart.addActionListener(e -> openSeatManager(SeatManagerMode.SETTINGS));

        JMenuItem swapSeats = new JMenuItem("Swap Seats");
        swapSeats.addActionListener(e -> openSeatManager(SeatManagerMode.SWAP));

        JMenuItem exportAttendance = new JMenuItem("Export Attendance");
        exportAttendance.addActionListener(e -> exportAttendance());

        menu.add(chart);
        menu.add(swapSeats);
        menu.add(exportAttendance);

        menu.show(anchor, 0, anchor.getHeight());
    }

    private static void exportAttendance() {
        JFileChooser chooser = new JFileChooser();
        String date = LocalDate.now().toString();
        String periodSuffix = getAttendancePeriodSuffix();
        String periodTag = periodSuffix.isEmpty() ? "P" : "P" + periodSuffix;
        chooser.setSelectedFile(new File("Attendence_" + date + "_" + periodTag + ".txt"));

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
            } else {
                pw.println("None");
            }
            pw.println();
            pw.println("Tardy:");
            pw.println();
            if (!tardyNames.isEmpty()) {
                pw.println(String.join(", ", tardyNames));
            } else {
                pw.println("None");
            }
            pw.println();
            pw.println("Present:");
            pw.println();
            if (!presentNames.isEmpty()) {
                pw.println(String.join(", ", presentNames));
            } else {
                pw.println("None");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static File getPreferredDocumentsDir() {
        File oneDriveDocuments = new File(System.getProperty("user.home"),
                "OneDrive - San Diego Unified School District\\Documents");
        if (oneDriveDocuments.exists() && oneDriveDocuments.isDirectory()) {
            return oneDriveDocuments;
        }
        return new File(System.getProperty("user.home"), "Documents");
    }

    private static boolean autoLoadSeatingChart() {
        String periodSuffix = getAttendancePeriodSuffix();
        if (periodSuffix.isEmpty()) {
            return false;
        }

        String periodTag = "P" + periodSuffix;
        File documentsDir = getPreferredDocumentsDir();
        File chartFile = new File(documentsDir, periodTag + "_seats.csv");

        if (!chartFile.exists()) {
            return false;
        }

        ensureSeatLabels();
        loadSeatingChart(chartFile);
        return true;
    }

    private static String getAttendancePeriodSuffix() {
        LocalDate date = LocalDate.now();
        LocalTime now = LocalTime.now();
        DayOfWeek day = date.getDayOfWeek();

        if (day == DayOfWeek.MONDAY) {
            return getMondayPeriod(now);
        }

        if (day == DayOfWeek.TUESDAY || day == DayOfWeek.WEDNESDAY
                || day == DayOfWeek.THURSDAY || day == DayOfWeek.FRIDAY) {
            return getDailyPeriod(now);
        }

        return "";
    }

    private static String getMondayPeriod(LocalTime now) {
        String period = findPeriod(now, LocalTime.of(9, 20), LocalTime.of(10, 10), "1");
        if (period != null) return period;

        period = findPeriod(now, LocalTime.of(10, 16), LocalTime.of(11, 6), "2");
        if (period != null) return period;

        period = findPeriod(now, LocalTime.of(11, 12), LocalTime.of(12, 2), "3");
        if (period != null) return period;

        period = findPeriod(now, LocalTime.of(12, 8), LocalTime.of(12, 58), "4");
        if (period != null) return period;

        period = findPeriod(now, LocalTime.of(13, 40), LocalTime.of(14, 39), "5");
        if (period != null) return period;

        period = findPeriod(now, LocalTime.of(14, 45), LocalTime.of(15, 35), "6");
        if (period != null) return period;

        return "";
    }

    private static String getDailyPeriod(LocalTime now) {
        String period = findPeriod(now, LocalTime.of(8, 45), LocalTime.of(9, 42), "1");
        if (period != null) return period;

        period = findPeriod(now, LocalTime.of(9, 48), LocalTime.of(10, 45), "2");
        if (period != null) return period;

        period = findPeriod(now, LocalTime.of(10, 51), LocalTime.of(11, 48), "3");
        if (period != null) return period;

        period = findPeriod(now, LocalTime.of(11, 54), LocalTime.of(12, 51), "4");
        if (period != null) return period;

        period = findPeriod(now, LocalTime.of(13, 33), LocalTime.of(14, 30), "5");
        if (period != null) return period;

        period = findPeriod(now, LocalTime.of(14, 36), LocalTime.of(15, 33), "6");
        if (period != null) return period;

        return "";
    }

    private static String findPeriod(LocalTime now, LocalTime start, LocalTime end, String period) {
        if (!now.isBefore(start) && now.isBefore(end)) {
            return period;
        }
        return null;
    }

    /* ================= SEAT MANAGER (COMBINED) ================= */
    private static void openSeatManager(SeatManagerMode mode) {
        if (seatManagerFrame == null) {
            seatManagerFrame = new JFrame();
            seatManagerFrame.setSize(700, 500);
            seatManagerFrame.setLocationRelativeTo(null);
            seatManagerFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            seatManagerFrame.getContentPane().setBackground(new Color(30, 30, 40));

            JPanel panel = new JPanel(new BorderLayout());
            panel.setBackground(new Color(45, 45, 60));
            panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            seatManagerTitle = new JLabel("", JLabel.CENTER);
            seatManagerTitle.setForeground(Color.WHITE);
            seatManagerTitle.setFont(new Font("SansSerif", Font.BOLD, 20));

            seatManagerGridPanel = new JPanel(new BorderLayout());
            seatManagerGridPanel.setBackground(new Color(45, 45, 60));

            JButton saveBtn = makeTxtButton("Save Chart", new Color(72, 99, 255), new Dimension(130, 38), 14, false);
            saveBtn.addActionListener(e -> {
                JFileChooser chooser = new JFileChooser();
                String periodSuffix = getAttendancePeriodSuffix();
                String periodTag = periodSuffix.isEmpty() ? "P" : "P" + periodSuffix;
                chooser.setSelectedFile(new File(periodTag + "_seats.csv"));
                if (chooser.showSaveDialog(seatManagerFrame) == JFileChooser.APPROVE_OPTION) {
                    saveSeatingChart(chooser.getSelectedFile());
                }
            });

            JButton loadBtn = makeTxtButton("Load Chart", new Color(110, 110, 120), new Dimension(130, 38), 14, false);
            loadBtn.addActionListener(e -> {
                JFileChooser chooser = new JFileChooser();
                if (chooser.showOpenDialog(seatManagerFrame) == JFileChooser.APPROVE_OPTION) {
                    loadSeatingChart(chooser.getSelectedFile());
                }
            });

            JButton clearBtn = makeTxtButton("Clear", new Color(160, 60, 60), new Dimension(120, 38), 14, false);
            clearBtn.addActionListener(e -> clearSeatingChart());

            seatManagerEditButton = makeTxtButton("Edit Chart", new Color(80, 130, 180), new Dimension(140, 38), 14, false);
            seatManagerEditButton.addActionListener(e -> {
                if (seatManagerMode == SeatManagerMode.SWAP) {
                    randomizeSeatingChart();
                    return;
                }
                if (seatManagerMode == SeatManagerMode.EDIT) {
                    setSeatManagerMode(SeatManagerMode.SETTINGS);
                } else {
                    setSeatManagerMode(SeatManagerMode.EDIT);
                }
            });

            seatManagerSwapButton = makeTxtButton("Swap Seats", new Color(120, 120, 120), new Dimension(140, 38), 14, false);
            seatManagerSwapButton.addActionListener(e -> {
                if (seatManagerMode == SeatManagerMode.SWAP) {
                    setSeatManagerMode(SeatManagerMode.SETTINGS);
                } else {
                    setSeatManagerMode(SeatManagerMode.SWAP);
                }
            });

            JPanel bottom = new JPanel();
            bottom.setBackground(new Color(45, 45, 60));
            bottom.add(saveBtn);
            bottom.add(loadBtn);
            bottom.add(clearBtn);
            bottom.add(seatManagerEditButton);
            bottom.add(seatManagerSwapButton);

            panel.add(seatManagerTitle, BorderLayout.NORTH);
            panel.add(seatManagerGridPanel, BorderLayout.CENTER);
            panel.add(bottom, BorderLayout.SOUTH);

            seatManagerFrame.add(panel);

            seatManagerFrame.addWindowListener(new WindowAdapter() {
                public void windowClosed(WindowEvent e) {
                    seatManagerFrame = null;
                }
            });
        }

        setSeatManagerMode(mode);
        seatManagerFrame.setVisible(true);
    }

    private static String getSeatManagerTitle(SeatManagerMode mode) {
        switch (mode) {
            case EDIT:
                return "Seating Chart Editor";
            case SWAP:
                return "Swap Seats";
            default:
                return "Seat Settings";
        }
    }

    private static void setSeatManagerMode(SeatManagerMode mode) {
        seatManagerMode = mode;
        swapSelectedSeat = null;

        String title = getSeatManagerTitle(mode);
        seatManagerFrame.setTitle(title);
        seatManagerTitle.setText(title);

        if (mode == SeatManagerMode.EDIT) {
            seatManagerEditButton.setText("Seat Settings");
        } else if (mode == SeatManagerMode.SWAP) {
            seatManagerEditButton.setText("Randomize");
        } else {
            seatManagerEditButton.setText("Edit Chart");
        }

        if (mode == SeatManagerMode.SWAP) {
            seatManagerSwapButton.setText("Seat Settings");
        } else {
            seatManagerSwapButton.setText("Swap Seats");
        }

        seatManagerGridPanel.removeAll();
        if (mode == SeatManagerMode.EDIT) {
            seatManagerGridPanel.add(createEditableSeatGrid(), BorderLayout.CENTER);
            refreshSeatEditorButtons();
        } else if (mode == SeatManagerMode.SWAP) {
            seatManagerGridPanel.add(createSwapSeatGrid(), BorderLayout.CENTER);
            refreshSeatSwapButtons();
        } else {
            seatManagerGridPanel.add(createSeatGrid(), BorderLayout.CENTER);
            refreshSeatSettingsToggles();
        }
        seatManagerGridPanel.revalidate();
        seatManagerGridPanel.repaint();
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

    private static JPanel createEditableSeatGrid() {
        ensureSeatLabels();
        seatEditButtons.clear();

        JPanel grid = new JPanel(new GridLayout(leftRows.length, 9, 10, 10));
        grid.setBackground(new Color(45, 45, 60));

        for (int i = 0; i < leftRows.length; i++) {
            for (int n = 4; n >= 1; n--) addEditableSeat(grid, leftRows[i] + n);
            grid.add(new JLabel());
            if (i < rightRows.length) {
                for (int n = 1; n <= 4; n++) addEditableSeat(grid, rightRows[i] + n);
            } else {
                for (int n = 1; n <= 4; n++) grid.add(new JLabel());
            }
        }

        return grid;
    }

    private static JPanel createSwapSeatGrid() {
        ensureSeatLabels();
        seatSwapButtons.clear();

        JPanel grid = new JPanel(new GridLayout(leftRows.length, 9, 10, 10));
        grid.setBackground(new Color(45, 45, 60));

        for (int i = 0; i < leftRows.length; i++) {
            for (int n = 4; n >= 1; n--) addSwapSeat(grid, leftRows[i] + n);
            grid.add(new JLabel());
            if (i < rightRows.length) {
                for (int n = 1; n <= 4; n++) addSwapSeat(grid, rightRows[i] + n);
            } else {
                for (int n = 1; n <= 4; n++) grid.add(new JLabel());
            }
        }

        return grid;
    }

    private static JToggleButton createSeatToggle(String seat) {
        JToggleButton btn = new JToggleButton(getSeatLabel(seat));
        btn.setFocusPainted(false);
        btn.setForeground(Color.WHITE);
        btn.setBackground(BASE_SEAT_COLOR);
        btn.setSelected(disabledSeats.contains(seat));

        if (disabledSeats.contains(seat)) {
            btn.setBackground(DISABLED_SEAT_COLOR);
        } else if (tardySeats.contains(seat)) {
            btn.setBackground(TARDY_SEAT_COLOR);
        } else {
            btn.setBackground(BASE_SEAT_COLOR);
        }

        btn.addItemListener(e -> {
            if (btn.isSelected()) {
                disabledSeats.add(seat);
                tardySeats.remove(seat);
                btn.setBackground(DISABLED_SEAT_COLOR);
            } else {
                disabledSeats.remove(seat);
                handleReenabledSeatPrompt(seat, btn);
            }
            refreshSeatEditorButton(seat);
            refreshSeatSwapButton(seat);
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
            refreshSeatEditorButton(seat);
            refreshSeatSwapButton(seat);
            return;
        }

        tardySeats.remove(seat);
        btn.setBackground(BASE_SEAT_COLOR);

        JPopupMenu menu = new JPopupMenu();

        JMenuItem presentItem = new JMenuItem("Present");
        presentItem.addActionListener(e -> {
            tardySeats.remove(seat);
            btn.setBackground(BASE_SEAT_COLOR);
            refreshSeatEditorButton(seat);
            refreshSeatSwapButton(seat);
        });

        JMenuItem tardyItem = new JMenuItem("Tardy");
        tardyItem.addActionListener(e -> {
            tardySeats.add(seat);
            btn.setBackground(TARDY_SEAT_COLOR);
            refreshSeatEditorButton(seat);
            refreshSeatSwapButton(seat);
        });

        menu.add(presentItem);
        menu.add(tardyItem);

        menu.show(btn, 0, btn.getHeight());
    }

    private static void addEditableSeat(JPanel grid, String seat) {
        JButton btn = new JButton(getSeatLabel(seat));
        btn.setBackground(getSeatEditorColor(seat));
        btn.setForeground(Color.WHITE);
        btn.addActionListener(e -> {
            String name = JOptionPane.showInputDialog("Enter name for seat " + seat);
            if (name != null) {
                updateSeatLabel(seat, name);
                btn.setText(getSeatLabel(seat));
            }
        });
        grid.add(btn);
        seatEditButtons.put(seat, btn);
    }

    private static void addSwapSeat(JPanel grid, String seat) {
        JButton btn = new JButton(getSeatLabel(seat));
        btn.setBackground(getSeatSwapColor(seat));
        btn.setForeground(Color.WHITE);
        btn.addActionListener(e -> handleSwapSelection(seat));
        grid.add(btn);
        seatSwapButtons.put(seat, btn);
    }

    private static void handleSwapSelection(String seat) {
        String label = getSeatLabel(seat);
        boolean seatHasName = label != null && !label.trim().isEmpty() && !label.equals(seat);

        if (swapSelectedSeat == null) {
            if (!seatHasName) {
                return;
            }
            swapSelectedSeat = seat;
            refreshSeatSwapButtons();
            return;
        }

        if (swapSelectedSeat.equals(seat)) {
            swapSelectedSeat = null;
            refreshSeatSwapButtons();
            return;
        }

        String firstLabel = getSeatLabel(swapSelectedSeat);
        String secondLabel = getSeatLabel(seat);

        boolean firstHasName = firstLabel != null && !firstLabel.trim().isEmpty() && !firstLabel.equals(swapSelectedSeat);
        boolean secondHasName = secondLabel != null && !secondLabel.trim().isEmpty() && !secondLabel.equals(seat);

        if (firstHasName && !secondHasName) {
            updateSeatLabel(seat, firstLabel);
            disabledSeats.remove(seat);

            updateSeatLabel(swapSelectedSeat, swapSelectedSeat);
            disabledSeats.add(swapSelectedSeat);
        } else if (firstHasName && secondHasName) {
            updateSeatLabel(swapSelectedSeat, secondLabel);
            updateSeatLabel(seat, firstLabel);
        }

        swapSelectedSeat = null;
        refreshSeatSettingsToggles();
        refreshSeatEditorButtons();
        refreshSeatSwapButtons();
        initSeats();
    }

    private static Color getSeatEditorColor(String seat) {
        if (disabledSeats.contains(seat)) {
            return DISABLED_SEAT_COLOR;
        }
        if (tardySeats.contains(seat)) {
            return TARDY_SEAT_COLOR;
        }
        return BASE_SEAT_COLOR;
    }

    private static Color getSeatSwapColor(String seat) {
        if (seat.equals(swapSelectedSeat)) {
            return SWAP_SELECTED_COLOR;
        }
        return getSeatEditorColor(seat);
    }

    private static void refreshSeatEditorButton(String seat) {
        JButton editBtn = seatEditButtons.get(seat);
        if (editBtn != null) {
            editBtn.setText(getSeatLabel(seat));
            editBtn.setBackground(getSeatEditorColor(seat));
        }
    }

    private static void refreshSeatEditorButtons() {
        for (String seat : seatEditButtons.keySet()) {
            refreshSeatEditorButton(seat);
        }
    }

    private static void refreshSeatSwapButton(String seat) {
        JButton swapBtn = seatSwapButtons.get(seat);
        if (swapBtn != null) {
            swapBtn.setText(getSeatLabel(seat));
            swapBtn.setBackground(getSeatSwapColor(seat));
        }
    }

    private static void refreshSeatSwapButtons() {
        for (String seat : seatSwapButtons.keySet()) {
            refreshSeatSwapButton(seat);
        }
    }

    private static void clearSeatingChart() {
        disabledSeats.clear();
        tardySeats.clear();
        swapSelectedSeat = null;
        ensureSeatLabels();

        for (String seat : seatLabels.keySet()) {
            updateSeatLabel(seat, seat);
        }

        initSeats();
        refreshSeatSettingsToggles();
        refreshSeatEditorButtons();
        refreshSeatSwapButtons();
    }

    private static java.util.List<String> getAllSeatIds() {
        java.util.List<String> seats = new ArrayList<>();
        for (int i = 0; i < leftRows.length; i++) {
            for (int n = 1; n <= 4; n++) {
                seats.add(leftRows[i] + n);
            }
            if (i < rightRows.length) {
                for (int n = 1; n <= 4; n++) {
                    seats.add(rightRows[i] + n);
                }
            }
        }
        return seats;
    }

    private static void randomizeSeatingChart() {
        ensureSeatLabels();

        java.util.List<String> names = new ArrayList<>();
        for (String seat : getAllSeatIds()) {
            String label = getSeatLabel(seat);
            if (label != null && !label.trim().isEmpty() && !label.equals(seat)) {
                names.add(label.trim());
            }
        }

        Collections.shuffle(names);

        java.util.List<String> seats = getAllSeatIds();

        disabledSeats.clear();
        tardySeats.clear();
        swapSelectedSeat = null;

        for (int i = 0; i < seats.size(); i++) {
            String seat = seats.get(i);
            if (i < names.size()) {
                updateSeatLabel(seat, names.get(i));
            } else {
                updateSeatLabel(seat, seat);
                disabledSeats.add(seat);
            }
        }

        initSeats();
        refreshSeatSettingsToggles();
        refreshSeatEditorButtons();
        refreshSeatSwapButtons();
    }

    private static void saveSeatingChart(File file) {
        ensureSeatLabels();

        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("Seat,Enabled,Name");
            for (String seat : seatLabels.keySet()) {
                String name = getSeatLabel(seat);
                if (name.equals(seat)) name = "";
                boolean enabled = !disabledSeats.contains(seat);
                pw.println(seat + "," + enabled + "," + name);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadSeatingChart(File file) {
        tardySeats.clear();
        swapSelectedSeat = null;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",", -1);
                if (parts.length >= 3) {
                    String seat = parts[0];
                    boolean enabled = Boolean.parseBoolean(parts[1].trim());
                    String name = parts[2].trim();

                    if (enabled) disabledSeats.remove(seat);
                    else disabledSeats.add(seat);

                    updateSeatLabel(seat, name);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        initSeats();
        refreshSeatSettingsToggles();
        refreshSeatEditorButtons();
        refreshSeatSwapButtons();
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

        refreshSeatEditorButton(seat);
        refreshSeatSwapButton(seat);
    }

    private static void refreshSeatSettingsToggles() {
        for (Map.Entry<String, JToggleButton> entry : seatToggleButtons.entrySet()) {
            String seat = entry.getKey();
            JToggleButton toggle = entry.getValue();
            toggle.setSelected(disabledSeats.contains(seat));
            toggle.setText(getSeatLabel(seat));

            if (disabledSeats.contains(seat)) {
                toggle.setBackground(DISABLED_SEAT_COLOR);
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
        }
    }

    /* ================= BUTTON CREATION ================= */
    public static JButton makeTxtButton(String txt, Color clr) {
        return makeTxtButton(txt, clr, new Dimension(160, 50), 16, true);
    }

    public static JButton makeTxtButton(String txt, Color clr, Dimension size, int fontSize, boolean useHandler) {
        JButton btn = new JButton(txt);
        btn.setFont(new Font("SansSerif", Font.BOLD, fontSize));
        btn.setBackground(clr);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        if (size != null) {
            btn.setPreferredSize(size);
        }
        if (useHandler) {
            btn.addActionListener(e -> handler(txt));
        }
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
    private static String getNextName() {
        if (namePool.isEmpty()) return "No name";
        if (nameIndex >= namePool.size()) {
            Collections.shuffle(namePool);
            nameIndex = 0;
        }
        return namePool.get(nameIndex++).name;
    }

    static class NameEntry {
        String name;

        NameEntry(String n) {
            name = n;
        }
    }
}