import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.*;

public class Main {

    private static JLabel resultLabel;
    private static JLabel counterLabel;
    private static JLabel titleLabel;
    private static JButton randomSeatButton;

    private static List<String> seatPool = new ArrayList<>();
    private static int seatIndex = 0;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::createAndShowUi);
    }

    private static void createAndShowUi() {
        JFrame frame = new JFrame("Random Seat Picker");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setBackground(new Color(30, 30, 40));

        // Create UI components first
        titleLabel = new JLabel("Random Seat Picker", JLabel.CENTER);
        titleLabel.setForeground(Color.WHITE);

        resultLabel = new JLabel("Click to pick a seat", JLabel.CENTER);
        resultLabel.setForeground(new Color(220, 220, 220));

        counterLabel = new JLabel("", JLabel.CENTER);
        counterLabel.setForeground(new Color(180, 180, 180));

        // Create the button using the restored makeTxtButton
        randomSeatButton = makeTxtButton("Pick Seat", new Color(72, 99, 255));

        initSeats();

        JPanel card = new JPanel();
        card.setBackground(new Color(45, 45, 60));
        card.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        resultLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        counterLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        randomSeatButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(titleLabel);
        card.add(Box.createVerticalStrut(20));
        card.add(resultLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(counterLabel);
        card.add(Box.createVerticalStrut(25));
        card.add(randomSeatButton);

        frame.setLayout(new BorderLayout());
        frame.add(card, BorderLayout.CENTER);

        frame.setSize(420, 320);
        frame.setLocationRelativeTo(null);

        updateFonts(frame.getWidth());

        frame.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                updateFonts(frame.getWidth());
            }
        });

        frame.setVisible(true);
    }

    /* ---------------- Seat Logic ---------------- */

    private static void initSeats() {
        seatPool.clear();

        String[] rows = {"A", "B", "C", "D", "E", "F", "G", "H", "I"};
        String[] numbers = {"1", "2", "3", "4"};

        for (String row : rows) {
            for (String num : numbers) {
                seatPool.add(row + num);
            }
        }

        Collections.shuffle(seatPool);
        seatIndex = 0;
        updateCounter();
    }

    private static String getNextSeat() {
        if (seatIndex >= seatPool.size()) {
            initSeats();
        }
        updateCounter();
        return seatPool.get(seatIndex++);
    }

    private static void updateCounter() {
        if (counterLabel != null) {
            counterLabel.setText("Remaining seats: " + (seatPool.size() - seatIndex));
        }
    }

    /* ---------------- Button Handler ---------------- */

    private static void handler(String name) {
        if (name.equals("Pick Seat")) {
            String seat = getNextSeat();
            resultLabel.setText(seat);
        }
    }

    /* ---------------- Styled Button with Dark Outline ---------------- */

    /**
     * Creates a JButton with text and mouse hover/press effects.
     * To make it call a certain function, alter the handler method.
     * By default, it will output "name" not found.
     *
     * @param txt The text to display on the button
     * @param clr The color background on the button
     * @return The customized JButton
     */
    public static JButton makeTxtButton(String txt, Color clr) {
        JButton btn = new JButton(txt);
        btn.setName(txt);
        btn.setFont(new Font("SansSerif", Font.BOLD, 16));
        btn.setBackground(clr);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Make button larger so text fits
        Dimension size = new Dimension(160, 50); // wider and taller
        btn.setPreferredSize(size);
        btn.setMinimumSize(size);
        btn.setMaximumSize(size);

        btn.setContentAreaFilled(false);
        btn.setOpaque(true);

        // Darker outline
        Color outline = clr.darker();
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(outline, 3), // darker outline
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));

        btn.addActionListener(e -> handler(txt));

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn.setBackground(lighten(clr, 0.2));
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                btn.setBackground(clr);
            }

            public void mousePressed(java.awt.event.MouseEvent evt) {
                btn.setBackground(darken(clr, 0.2));
            }

            public void mouseReleased(java.awt.event.MouseEvent evt) {
                btn.setBackground(clr);
            }
        });
        return btn;
    }

    /* ---------------- Dynamic Font Scaling ---------------- */

    private static void updateFonts(int width) {
        int titleSize = Math.max(20, width / 14);
        int resultSize = Math.max(18, width / 18);
        int counterSize = Math.max(14, width / 26);
        int buttonSize = Math.max(14, width / 22);

        titleLabel.setFont(new Font("SansSerif", Font.BOLD, titleSize));
        resultLabel.setFont(new Font("SansSerif", Font.BOLD, resultSize));
        counterLabel.setFont(new Font("SansSerif", Font.PLAIN, counterSize));
        randomSeatButton.setFont(new Font("SansSerif", Font.BOLD, buttonSize));
    }

    /* ---------------- Color Helpers ---------------- */

    private static Color lighten(Color c, double amount) {
        return new Color(
                Math.min(255, (int) (c.getRed() + 255 * amount)),
                Math.min(255, (int) (c.getGreen() + 255 * amount)),
                Math.min(255, (int) (c.getBlue() + 255 * amount))
        );
    }

    private static Color darken(Color c, double amount) {
        return new Color(
                Math.max(0, (int) (c.getRed() - 255 * amount)),
                Math.max(0, (int) (c.getGreen() - 255 * amount)),
                Math.max(0, (int) (c.getBlue() - 255 * amount))
        );
    }
}
