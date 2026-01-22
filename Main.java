import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class Main {
    private static final String[] ROW_LETTERS = {"A", "B", "C", "D", "E", "F", "G", "H", "I"};
    private static final String[] SEAT_NUMBERS = {"1", "2", "3", "4"};

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::createAndShowUi);
    }

    private static void createAndShowUi() {
        JFrame frame = new JFrame("Random Seat Picker");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JLabel resultLabel = new JLabel("Click the button to pick a seat", JLabel.CENTER);
        resultLabel.setFont(new Font("SansSerif", Font.BOLD, 18));

        /**
         * Creates a button that generates a random seat when clicked, 
         * and displays the result in a label. Sets up the GUI layout 
         * and initializes the frame.
         */
        JButton randomSeatButton = new JButton("Random Seat");
        randomSeatButton.setPreferredSize(new Dimension(160, 40));
        randomSeatButton.addActionListener(e -> {
            /**
             * Generates a random seat using the getRandomSeat method 
             * and updates the result label with the selected seat.
             */
            String seat = getRandomSeat(ROW_LETTERS, SEAT_NUMBERS);
            resultLabel.setText("You picked: " + seat);
        });

        JPanel panel = new JPanel();
        /**
         * Adds the random seat button to the panel.
         */
        panel.add(randomSeatButton);

        frame.setLayout(new BorderLayout(10, 10));
        /**
         * Adds the result label to the center of the frame and the panel 
         * containing the button to the bottom of the frame.
         */
        frame.add(resultLabel, BorderLayout.CENTER);
        frame.add(panel, BorderLayout.SOUTH);

        frame.setSize(360, 200);
        frame.setLocationRelativeTo(null);
        /**
         * Makes the frame visible and centers it on the screen.
         */
        frame.setVisible(true);
    }

    public static String getRandomSeat(String[] rows, String[] seats) {
        int randomRowIndex = (int) (Math.random() * rows.length);
        int randomSeatIndex = (int) (Math.random() * seats.length);
        return rows[randomRowIndex] + seats[randomSeatIndex];
    }
}