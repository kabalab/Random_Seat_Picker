import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import javax.swing.*;

public class Main {

    private static JLabel resultLabel;
    private static JLabel counterLabel;
    private static JLabel titleLabel;
    private static JButton randomSeatButton;

    private static final List<String> seatPool = new ArrayList<>();
    private static final Set<String> disabledSeats = new HashSet<>();
    private static int seatIndex = 0;

    private static final String[] leftRows  = {"A","B","C","D"};
    private static final String[] rightRows = {"E","F","G","H"};

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

        card.add(titleLabel);
        card.add(Box.createVerticalStrut(20));
        card.add(resultLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(counterLabel);
        card.add(Box.createVerticalStrut(25));
        card.add(randomSeatButton);
        card.add(Box.createVerticalStrut(10));
        card.add(settingsButton);

        frame.add(card);
        frame.setSize(420, 360);
        frame.setLocationRelativeTo(null);

        updateFonts(frame.getWidth());

        frame.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                updateFonts(frame.getWidth());
            }
        });

        frame.setVisible(true);
    }

    /* ================= SEAT SETTINGS WINDOW ================= */

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

            for (int n = 4; n >= 1; n--) {
                grid.add(createSeatToggle(leftRows[i] + n));
            }

            grid.add(new JLabel());

            for (int n = 1; n <= 4; n++) {
                grid.add(createSeatToggle(rightRows[i] + n));
            }
        }

        return grid;
    }

    private static JToggleButton createSeatToggle(String seat) {
        JToggleButton btn = new JToggleButton(seat);

        btn.setFocusPainted(false);
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(90, 90, 110));
        btn.setSelected(disabledSeats.contains(seat));

        if (btn.isSelected()) {
            btn.setBackground(new Color(120, 60, 60));
        }

        btn.addItemListener(e -> {
            if (btn.isSelected()) {
                disabledSeats.add(seat);
                btn.setBackground(new Color(120, 60, 60));
            } else {
                disabledSeats.remove(seat);
                btn.setBackground(new Color(90, 90, 110));
            }
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
                if (!disabledSeats.contains(leftSeat)) {
                    seatPool.add(leftSeat);
                }

                String rightSeat = rightRows[i] + n;
                if (!disabledSeats.contains(rightSeat)) {
                    seatPool.add(rightSeat);
                }
            }
        }

        Collections.shuffle(seatPool);
        seatIndex = 0;
        updateCounter();
    }

    private static String getNextSeat() {
        if (seatPool.isEmpty()) {
            return "No seats";
        }
        if (seatIndex >= seatPool.size()) {
            initSeats();
        }
        updateCounter();
        return seatPool.get(seatIndex++);
    }

    private static void updateCounter() {
        counterLabel.setText("Remaining seats: " + Math.max(0, seatPool.size() - seatIndex));
    }

    /* ================= BUTTON HANDLER ================= */

    private static void handler(String name) {
        if (name.equals("Pick Seat")) {
            resultLabel.setText(getNextSeat());
        }
        if (name.equals("Seat Settings")) {
            openSeatSettings();
        }
    }

    /* ================= STYLED BUTTON ================= */

    public static JButton makeTxtButton(String txt, Color clr) {
        JButton btn = new JButton(txt);
        btn.setName(txt);
        btn.setFont(new Font("SansSerif", Font.BOLD, 16));
        btn.setBackground(clr);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);

        Dimension size = new Dimension(160, 50);
        btn.setPreferredSize(size);

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
}
