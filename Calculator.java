import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;

public class Calculator extends JFrame implements ActionListener {

    private final JTextField display;
    private StringBuilder input;
    private final DecimalFormat format = new DecimalFormat("0.########");

    public Calculator() {
        setTitle("Scientific Calculator");
        setSize(500, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        input = new StringBuilder();

        // === Display ===
        display = new JTextField();
        display.setFont(new Font("Segoe UI", Font.BOLD, 36));
        display.setBackground(new Color(30, 30, 30));
        display.setForeground(Color.WHITE);
        display.setCaretColor(Color.WHITE);
        display.setEditable(false);
        display.setHorizontalAlignment(SwingConstants.RIGHT);
        display.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        add(display, BorderLayout.NORTH);

        // === Buttons Panel ===
        JPanel panel = new JPanel(new GridLayout(7, 5, 8, 8));
        panel.setBackground(new Color(25, 25, 25));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] buttons = {
                "AC", "DEL", "π", "e", "1/x",
                "sin", "cos", "tan", "ln", "log",
                "√", "x²", "x³", "^", "!",
                "7", "8", "9", "/", "%",
                "4", "5", "6", "*", "±",
                "1", "2", "3", "-", "",
                "0", ".", "=", "+", ""
        };

        for (String text : buttons) {
            if (text.isEmpty()) {
                panel.add(new JLabel());
                continue;
            }

            JButton button = new JButton(text);
            button.setFont(new Font("Segoe UI", Font.BOLD, 20));
            button.setForeground(Color.WHITE);
            button.setFocusPainted(false);

            // === Button Color Scheme ===
            if ("AC".equals(text) || "DEL".equals(text)) {
                button.setBackground(new Color(255, 90, 90)); // red
            } else if ("=".equals(text)) {
                button.setBackground(new Color(255, 160, 0)); // orange
            } else if ("+".equals(text) || "-".equals(text) || "*".equals(text) || "/".equals(text) || "^".equals(text)) {
                button.setBackground(new Color(90, 90, 255)); // blue
            } else {
                button.setBackground(new Color(50, 50, 50)); // default dark
            }

            button.setBorder(BorderFactory.createLineBorder(new Color(40, 40, 40)));
            button.addActionListener(this);
            panel.add(button);
        }

        add(panel, BorderLayout.CENTER);
        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String b = ((JButton) e.getSource()).getText();

        try {
            switch (b) {
                case "AC" -> input.setLength(0);
                case "DEL" -> {
                    if (input.length() > 0)
                        input.deleteCharAt(input.length() - 1);
                }
                case "=" -> calculate();
                case "π" -> input.append(Math.PI);
                case "e" -> input.append(Math.E);
                case "sin" -> applyUnary(Math::sin);
                case "cos" -> applyUnary(Math::cos);
                case "tan" -> applyUnary(Math::tan);
                case "log" -> applyUnary(Math::log10);
                case "ln" -> applyUnary(Math::log);
                case "√" -> applyUnary(Math::sqrt);
                case "x²" -> applyUnary(x -> x * x);
                case "x³" -> applyUnary(x -> x * x * x);
                case "^" -> input.append("^");
                case "%" -> input.append("%");
                case "1/x" -> applyUnary(x -> 1 / x);
                case "!" -> applyUnary(this::factorial);
                case "±" -> applyUnary(x -> -x);
                default -> input.append(b);
            }

            display.setText(input.toString());

        } catch (Exception ex) {
            display.setText("Error");
            input.setLength(0);
        }
    }

    private void calculate() {
        String expr = input.toString().replaceAll("×", "*").replaceAll("÷", "/");

        if (expr.contains("^")) {
            String[] parts = expr.split("\\^");
            double base = Double.parseDouble(parts[0]);
            double exp = Double.parseDouble(parts[1]);
            double result = Math.pow(base, exp);
            input.setLength(0);
            input.append(format.format(result));
            display.setText(input.toString());
            return;
        }

        try {
            double result = eval(expr);
            input.setLength(0);
            input.append(format.format(result));
            display.setText(input.toString());
        } catch (Exception e) {
            display.setText("Invalid Expression");
            input.setLength(0);
        }
    }

    private void applyUnary(Function function) {
        double val = eval(input.toString());
        double result = function.apply(val);
        input.setLength(0);
        input.append(format.format(result));
    }

    private double factorial(double n) {
        if (n < 0) return Double.NaN;
        if (n == 0 || n == 1) return 1;
        double res = 1;
        for (int i = 2; i <= (int) n; i++) res *= i;
        return res;
    }

    private double eval(String expr) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < expr.length()) ? expr.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < expr.length()) throw new RuntimeException("Unexpected: " + (char) ch);
                return x;
            }

            double parseExpression() {
                double x = parseTerm();
                while (true) {
                    if      (eat('+')) x += parseTerm();
                    else if (eat('-')) x -= parseTerm();
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                while (true) {
                    if      (eat('*')) x *= parseFactor();
                    else if (eat('/')) x /= parseFactor();
                    else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return parseFactor();
                if (eat('-')) return -parseFactor();

                double x;
                int startPos = this.pos;
                if (eat('(')) {
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(expr.substring(startPos, this.pos));
                } else {
                    throw new RuntimeException("Unexpected character: " + (char) ch);
                }

                return x;
            }
        }.parse();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Calculator::new);
    }

    interface Function {
        double apply(double x);
    }
}
