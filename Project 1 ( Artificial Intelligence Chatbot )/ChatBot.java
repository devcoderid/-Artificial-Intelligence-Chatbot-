import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class Chatbot {

    public static final String BOT_NAME = "JavaBot";

    static class NLP {

        private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
                "a", "an", "the", "is", "are", "was", "were", "am", "be", "been", "do", "does",
                "did", "i", "me", "my", "you", "your", "we", "our", "it", "its", "to", "of",
                "in", "on", "at", "for", "and", "or", "but", "with", "can", "could", "would",
                "should", "will", "shall", "please", "tell", "about", "how", "what", "when",
                "where", "who", "which", "there", "this", "that", "so", "if", "as", "by",
                "want", "need", "know", "get", "have", "has", "any", "some"));

        private static final Map<String, String> SYNONYMS = new HashMap<>();
        static {
            SYNONYMS.put("cost", "price");
            SYNONYMS.put("costs", "price");
            SYNONYMS.put("fee", "price");
            SYNONYMS.put("fees", "price");
            SYNONYMS.put("charge", "price");
            SYNONYMS.put("charges", "price");
            SYNONYMS.put("phone", "contact");
            SYNONYMS.put("call", "contact");
            SYNONYMS.put("number", "contact");
            SYNONYMS.put("mail", "email");
            SYNONYMS.put("timing", "hour");
            SYNONYMS.put("timings", "hour");
            SYNONYMS.put("open", "hour");
            SYNONYMS.put("close", "hour");
            SYNONYMS.put("passcode", "password");
            SYNONYMS.put("signup", "register");
            SYNONYMS.put("enroll", "register");
            SYNONYMS.put("join", "register");
            SYNONYMS.put("address", "location");
            SYNONYMS.put("place", "location");
            SYNONYMS.put("located", "location");
            SYNONYMS.put("developer", "creator");
            SYNONYMS.put("made", "creator");
            SYNONYMS.put("built", "creator");
            SYNONYMS.put("created", "creator");
            SYNONYMS.put("return", "refund");
            SYNONYMS.put("cancel", "refund");
        }

        static List<String> tokenize(String text) {
            List<String> tokens = new ArrayList<>();
            for (String t : text.toLowerCase().split("[^a-z0-9]+")) {
                if (!t.isEmpty()) tokens.add(t);
            }
            return tokens;
        }

        static String stem(String w) {
            int n = w.length();
            if (n > 4 && w.endsWith("ies")) {
                w = w.substring(0, n - 3) + "y";
            } else if (n > 5 && w.endsWith("ing")) {
                w = w.substring(0, n - 3);
            } else if (n > 4 && w.endsWith("ed")) {
                w = w.substring(0, n - 2);
            } else if (n > 4 && w.endsWith("es")) {
                w = w.substring(0, n - 2);
            } else if (n > 3 && w.endsWith("s") && !w.endsWith("ss")) {
                w = w.substring(0, n - 1);
            }
            if (w.length() > 3 && w.endsWith("e")) {
                w = w.substring(0, w.length() - 1);
            }
            return w;
        }

        static List<String> preprocess(String text) {
            List<String> out = new ArrayList<>();
            for (String t : tokenize(text)) {
                t = SYNONYMS.getOrDefault(t, t);
                if (STOP_WORDS.contains(t)) continue;
                out.add(stem(t));
            }
            return out;
        }
    }

    static class FaqEntry {
        final String question;
        final String answer;
        Map<String, Double> vector = new HashMap<>();
        double norm = 0;

        FaqEntry(String question, String answer) {
            this.question = question;
            this.answer = answer;
        }
    }

    static class Engine {

        private static final double THRESHOLD = 0.30;

        private final List<FaqEntry> faqs = new ArrayList<>();
        private final Map<String, Double> idf = new HashMap<>();
        private double defaultIdf = 1.0;
        private final Random random = new Random();

        Engine() {
            loadDefaultFaqs();
            train();
        }

        private void loadDefaultFaqs() {
            add("What are your working hours and opening timings?",
                    "We are open Monday to Saturday, 9:00 AM to 6:00 PM. We are closed on Sundays.");
            add("How can I contact support? What is your phone number or email?",
                    "You can reach us at support@javabot.com or call +91-141-0000000 during working hours.");
            add("Where is your office located? What is the address?",
                    "Our head office is in Jaipur, Rajasthan, India. Visitors are welcome during working hours.");
            add("How do I reset my password? I forgot my password.",
                    "Click 'Forgot Password' on the login page, enter your registered email and follow the link we send you.");
            add("How do I register or create a new account?",
                    "Click 'Sign Up', fill in your name, email and password, then verify your email address to activate the account.");
            add("What is the price or fee of your courses and plans?",
                    "The Basic plan is free, the Standard plan is Rs. 499 per month and the Premium plan is Rs. 999 per month.");
            add("What is your refund policy? Can I cancel and get my money back?",
                    "You may cancel within 7 days of purchase for a full refund. Refunds reach your account in 5-7 working days.");
            add("What courses or services do you offer?",
                    "We offer Java Programming, Python, Web Development, Data Structures and Artificial Intelligence.");
            add("How long does delivery or shipping take?",
                    "Orders are delivered within 3-5 working days across India.");
            add("What payment methods do you accept?",
                    "We accept UPI, credit/debit cards, net banking and popular wallets.");
            add("Do you provide a certificate after course completion?",
                    "Yes, a verified digital certificate is issued after you complete all modules and pass the final test.");
            add("Who created you? Who is your developer?",
                    "I was created in Java as an AI chatbot project using NLP and a TF-IDF similarity model.");
            add("What is artificial intelligence?",
                    "Artificial Intelligence (AI) is the ability of machines to perform tasks that normally need human intelligence, such as learning, reasoning and understanding language.");
            add("What is Java programming language?",
                    "Java is a popular, object-oriented, platform-independent programming language known for 'write once, run anywhere'.");
            add("What is machine learning?",
                    "Machine Learning is a branch of AI where systems learn patterns from data instead of being explicitly programmed.");
            add("What is NLP natural language processing?",
                    "Natural Language Processing (NLP) helps computers understand, interpret and generate human language.");
        }

        private void add(String q, String a) {
            faqs.add(new FaqEntry(q, a));
        }

        void train() {
            int n = faqs.size();
            Map<String, Integer> df = new HashMap<>();
            List<List<String>> docs = new ArrayList<>();

            for (FaqEntry f : faqs) {
                List<String> toks = NLP.preprocess(f.question);
                docs.add(toks);
                for (String t : new HashSet<>(toks)) df.merge(t, 1, Integer::sum);
            }

            idf.clear();
            for (Map.Entry<String, Integer> e : df.entrySet()) {
                idf.put(e.getKey(), Math.log((n + 1.0) / (e.getValue() + 1.0)) + 1.0);
            }
            defaultIdf = Math.log(n + 1.0) + 1.0;

            for (int i = 0; i < n; i++) {
                FaqEntry f = faqs.get(i);
                f.vector = vectorize(docs.get(i));
                f.norm = norm(f.vector);
            }
        }

        private Map<String, Double> vectorize(List<String> tokens) {
            Map<String, Double> tf = new HashMap<>();
            for (String t : tokens) tf.merge(t, 1.0, Double::sum);
            Map<String, Double> vec = new HashMap<>();
            for (Map.Entry<String, Double> e : tf.entrySet()) {
                vec.put(e.getKey(), e.getValue() * idf.getOrDefault(e.getKey(), defaultIdf));
            }
            return vec;
        }

        private double norm(Map<String, Double> v) {
            double s = 0;
            for (double x : v.values()) s += x * x;
            return Math.sqrt(s);
        }

        private double cosine(Map<String, Double> a, double na, Map<String, Double> b, double nb) {
            if (na == 0 || nb == 0) return 0;
            double dot = 0;
            for (Map.Entry<String, Double> e : a.entrySet()) {
                Double other = b.get(e.getKey());
                if (other != null) dot += e.getValue() * other;
            }
            return dot / (na * nb);
        }

        String reply(String input) {
            String text = input.trim();
            if (text.isEmpty()) return "Please type something so that I can help you.";

            if (text.toLowerCase().startsWith("/teach")) {
                return teach(text.substring(6).trim());
            }

            List<String> raw = NLP.tokenize(text);
            Set<String> words = new HashSet<>(raw);
            String lower = text.toLowerCase();

            if (raw.size() <= 4 && containsAny(words, "hi", "hello", "hey", "hola", "namaste", "greetings")) {
                return pick("Hello! I am " + BOT_NAME + ". How can I help you today?",
                        "Hi there! Ask me anything about our services.",
                        "Namaste! How may I assist you?");
            }
            if (containsAny(words, "bye", "goodbye", "exit", "quit") || lower.contains("see you")) {
                return pick("Goodbye! Have a wonderful day.", "Bye! Come back any time.");
            }
            if (containsAny(words, "thanks", "thank", "thx")) {
                return pick("You are welcome!", "Happy to help!", "Anytime!");
            }
            if (lower.contains("your name") || lower.contains("who are you")) {
                return "I am " + BOT_NAME + ", a Java chatbot built with NLP and machine-learning based FAQ matching.";
            }
            if (lower.contains("how are you")) {
                return "I am running perfectly, thank you for asking! How can I help you?";
            }
            if (words.contains("time") && !lower.contains("working") && !lower.contains("hours")) {
                return "The current time is " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss a")) + ".";
            }
            if (words.contains("date") || words.contains("today") || words.contains("day")) {
                return "Today is " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy")) + ".";
            }
            if (words.contains("help") || lower.contains("what can you do")) {
                return "I can answer questions about working hours, contact details, courses, prices, refunds, "
                        + "passwords, payments and more. You can also teach me: /teach question => answer";
            }

            Map<String, Double> qv = vectorize(NLP.preprocess(text));
            double qn = norm(qv);

            FaqEntry best = null;
            double bestScore = 0;
            for (FaqEntry f : faqs) {
                double s = cosine(qv, qn, f.vector, f.norm);
                if (s > bestScore) {
                    bestScore = s;
                    best = f;
                }
            }

            if (best != null && bestScore >= THRESHOLD) {
                return best.answer;
            }
            return "Sorry, I did not understand that. You can rephrase your question, or teach me using:\n"
                    + "/teach your question => the answer";
        }

        private String teach(String args) {
            int idx = args.indexOf("=>");
            if (idx < 1 || idx + 2 >= args.length()) {
                return "Usage: /teach question => answer";
            }
            String q = args.substring(0, idx).trim();
            String a = args.substring(idx + 2).trim();
            if (q.isEmpty() || a.isEmpty()) return "Usage: /teach question => answer";
            add(q, a);
            train();
            return "Thank you! I have learned a new answer. Total knowledge: " + faqs.size() + " FAQs.";
        }

        private boolean containsAny(Set<String> words, String... options) {
            for (String o : options) if (words.contains(o)) return true;
            return false;
        }

        private String pick(String... options) {
            return options[random.nextInt(options.length)];
        }
    }

    static final Color BG = new Color(0xEEF2F7);
    static final Color BLUE = new Color(0x2A5298);
    static final Color DARK_BLUE = new Color(0x1E3C72);
    static final Color LIGHT_BLUE = new Color(0xDCE8FA);
    static final Color TEXT = new Color(0x1F2933);
    static final Color MUTED = new Color(0x8A94A6);
    static final Color LINE = new Color(0xD5DDE8);
    static final String FONT = "Segoe UI";

    static void smooth(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    static class GradientPanel extends JPanel {
        GradientPanel() {
            super(new BorderLayout());
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setPaint(new GradientPaint(0, 0, DARK_BLUE, getWidth(), 0, new Color(0x3B6FD0)));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }

    static class Avatar extends JComponent {
        private final int size;
        private final String letter;
        private final Color c1, c2, fg;

        Avatar(int size, String letter, Color c1, Color c2, Color fg) {
            this.size = size;
            this.letter = letter;
            this.c1 = c1;
            this.c2 = c2;
            this.fg = fg;
            Dimension d = new Dimension(size, size);
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            g2.setPaint(new GradientPaint(0, 0, c1, size, size, c2));
            g2.fillOval(0, 0, size - 1, size - 1);
            g2.setColor(fg);
            g2.setFont(new Font(FONT, Font.BOLD, size / 2));
            FontMetrics fm = g2.getFontMetrics();
            int x = (size - fm.stringWidth(letter)) / 2;
            int y = (size - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(letter, x, y);
            g2.dispose();
        }
    }

    static class Dot extends JComponent {
        private final Color color;

        Dot(Color color) {
            this.color = color;
            setPreferredSize(new Dimension(10, 10));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            g2.setColor(color);
            g2.fillOval(0, 0, 9, 9);
            g2.dispose();
        }
    }

    static class RoundButton extends JButton {
        private final Color base, hover, outline;
        private final int arc;
        private boolean over;

        RoundButton(String text, Color base, Color hover, Color fg, int arc, Color outline) {
            super(text);
            this.base = base;
            this.hover = hover;
            this.arc = arc;
            this.outline = outline;
            setForeground(fg);
            setFont(new Font(FONT, Font.BOLD, 13));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setBorder(new EmptyBorder(10, 22, 10, 22));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    over = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    over = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            g2.setColor(over ? hover : base);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            if (outline != null) {
                g2.setColor(outline);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class RoundField extends JTextField {
        private final String hint;

        RoundField(String hint) {
            this.hint = hint;
            setOpaque(false);
            setFont(new Font(FONT, Font.PLAIN, 15));
            setForeground(TEXT);
            setCaretColor(BLUE);
            setBorder(new EmptyBorder(11, 18, 11, 18));
            addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    repaint();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            g2.setColor(new Color(0xF7F9FC));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 28, 28);
            g2.setColor(hasFocus() ? BLUE : LINE);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 28, 28);
            g2.dispose();
            super.paintComponent(g);
            if (getText().isEmpty()) {
                Graphics2D h = (Graphics2D) g.create();
                smooth(h);
                h.setColor(MUTED);
                h.setFont(getFont());
                FontMetrics fm = h.getFontMetrics();
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                h.drawString(hint, getInsets().left, y);
                h.dispose();
            }
        }
    }

    static class Bubble extends JPanel {
        private final Color fill;
        private final boolean outlined;

        Bubble(String text, String time, Color fill, Color fg, Color timeColor, int maxTextWidth, boolean outlined) {
            super(new BorderLayout(0, 4));
            this.fill = fill;
            this.outlined = outlined;
            setOpaque(false);
            setBorder(new EmptyBorder(10, 14, 8, 14));

            Font f = new Font(FONT, Font.PLAIN, 15);
            JTextArea area = new JTextArea(text);
            area.setFont(f);
            area.setForeground(fg);
            area.setOpaque(false);
            area.setEditable(false);
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            area.setBorder(null);

            FontMetrics fm = area.getFontMetrics(f);
            int natural = 0;
            for (String line : text.split("\n")) {
                natural = Math.max(natural, fm.stringWidth(line));
            }
            int w = Math.max(60, Math.min(maxTextWidth, natural + 14));
            area.setSize(w, Short.MAX_VALUE);
            int h = area.getPreferredSize().height;
            area.setPreferredSize(new Dimension(w, h));

            JLabel stamp = new JLabel(time);
            stamp.setFont(new Font(FONT, Font.PLAIN, 10));
            stamp.setForeground(timeColor);
            stamp.setHorizontalAlignment(SwingConstants.RIGHT);

            add(area, BorderLayout.CENTER);
            add(stamp, BorderLayout.SOUTH);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            RoundRectangle2D shape = new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 22, 22);
            g2.setColor(fill);
            g2.fill(shape);
            if (outlined) {
                g2.setColor(LINE);
                g2.draw(shape);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class ChatWindow extends JFrame {

        private static final String[][] QUICK = {
                {"Working hours", "What are your working hours?"},
                {"Courses", "What courses do you offer?"},
                {"Prices", "What is the price of your plans?"},
                {"Refund", "What is your refund policy?"},
                {"Contact", "How can I contact support?"}
        };

        private final Engine engine = new Engine();
        private final JPanel messages = new JPanel();
        private final JScrollPane scroll;
        private final RoundField inputField = new RoundField("Type your message here...");
        private final RoundButton sendButton = new RoundButton("Send", BLUE, DARK_BLUE, Color.WHITE, 28, null);
        private final JLabel statusText = new JLabel("Online");
        private boolean busy = false;

        ChatWindow() {
            super(BOT_NAME + " - AI Chat Assistant");
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            setSize(720, 700);
            setMinimumSize(new Dimension(560, 480));
            setLocationRelativeTo(null);
            getContentPane().setLayout(new BorderLayout());

            messages.setLayout(new BoxLayout(messages, BoxLayout.Y_AXIS));
            messages.setBackground(BG);
            messages.setBorder(new EmptyBorder(12, 0, 12, 0));

            JPanel holder = new JPanel(new BorderLayout());
            holder.setBackground(BG);
            holder.add(messages, BorderLayout.NORTH);

            scroll = new JScrollPane(holder, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.setBorder(null);
            scroll.getViewport().setBackground(BG);
            scroll.getVerticalScrollBar().setUnitIncrement(18);

            add(buildHeader(), BorderLayout.NORTH);
            add(scroll, BorderLayout.CENTER);
            add(buildBottom(), BorderLayout.SOUTH);

            showWelcome();

            addWindowListener(new WindowAdapter() {
                @Override
                public void windowOpened(WindowEvent e) {
                    inputField.requestFocusInWindow();
                }
            });
        }

        private JComponent buildHeader() {
            GradientPanel header = new GradientPanel();
            header.setBorder(new EmptyBorder(14, 18, 14, 18));

            JPanel left = new JPanel(new BorderLayout(12, 0));
            left.setOpaque(false);
            left.add(new Avatar(46, "J", Color.WHITE, LIGHT_BLUE, BLUE), BorderLayout.WEST);

            JLabel title = new JLabel(BOT_NAME + " Assistant");
            title.setFont(new Font(FONT, Font.BOLD, 20));
            title.setForeground(Color.WHITE);

            statusText.setFont(new Font(FONT, Font.PLAIN, 12));
            statusText.setForeground(new Color(0xD6E4FF));
            JPanel status = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            status.setOpaque(false);
            status.add(new Dot(new Color(0x5BE584)));
            status.add(statusText);

            JPanel names = new JPanel(new GridLayout(2, 1, 0, 2));
            names.setOpaque(false);
            names.add(title);
            names.add(status);
            left.add(names, BorderLayout.CENTER);

            RoundButton clear = new RoundButton("Clear chat", new Color(255, 255, 255, 45),
                    new Color(255, 255, 255, 95), Color.WHITE, 22, null);
            clear.setBorder(new EmptyBorder(8, 16, 8, 16));
            clear.setFont(new Font(FONT, Font.PLAIN, 12));
            clear.addActionListener(e -> {
                messages.removeAll();
                showWelcome();
                messages.revalidate();
                messages.repaint();
            });

            JPanel right = new JPanel(new GridBagLayout());
            right.setOpaque(false);
            right.add(clear);

            header.add(left, BorderLayout.WEST);
            header.add(right, BorderLayout.EAST);
            return header;
        }

        private JComponent buildBottom() {
            JPanel bottom = new JPanel(new BorderLayout(0, 8));
            bottom.setBackground(Color.WHITE);
            bottom.setBorder(new CompoundBorder(new MatteBorder(1, 0, 0, 0, LINE), new EmptyBorder(10, 14, 8, 14)));

            JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            chips.setOpaque(false);
            for (String[] q : QUICK) {
                RoundButton chip = new RoundButton(q[0], Color.WHITE, LIGHT_BLUE, BLUE, 26, BLUE);
                chip.setBorder(new EmptyBorder(6, 14, 6, 14));
                chip.setFont(new Font(FONT, Font.PLAIN, 12));
                chip.addActionListener(e -> sendMessage(q[1]));
                chips.add(chip);
            }

            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setOpaque(false);
            row.add(inputField, BorderLayout.CENTER);
            row.add(sendButton, BorderLayout.EAST);

            JLabel tip = new JLabel("Tip: teach me new answers with   /teach question => answer");
            tip.setFont(new Font(FONT, Font.PLAIN, 11));
            tip.setForeground(MUTED);

            bottom.add(chips, BorderLayout.NORTH);
            bottom.add(row, BorderLayout.CENTER);
            bottom.add(tip, BorderLayout.SOUTH);

            ActionListener send = e -> sendMessage(inputField.getText());
            sendButton.addActionListener(send);
            inputField.addActionListener(send);
            return bottom;
        }

        private void showWelcome() {
            JLabel day = new JLabel(LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy")),
                    SwingConstants.CENTER);
            day.setFont(new Font(FONT, Font.PLAIN, 11));
            day.setForeground(MUTED);
            day.setAlignmentX(0.5f);
            day.setMaximumSize(new Dimension(Integer.MAX_VALUE, day.getPreferredSize().height + 6));
            messages.add(day);
            addRow(makeRow(false, "Hello! I am " + BOT_NAME + ", your virtual assistant.\n"
                    + "Tap a quick question below or type your own message.", now()));
        }

        private String now() {
            return LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"));
        }

        private JPanel makeRow(boolean fromUser, String text, String time) {
            Bubble bubble = fromUser
                    ? new Bubble(text, time, BLUE, Color.WHITE, new Color(0xC9D8F5), 400, false)
                    : new Bubble(text, time, Color.WHITE, TEXT, MUTED, 400, true);

            JPanel inner = new JPanel(new BorderLayout(8, 0));
            inner.setOpaque(false);
            if (!fromUser) {
                JPanel holder = new JPanel(new BorderLayout());
                holder.setOpaque(false);
                holder.add(new Avatar(36, "J", new Color(0x4C8DF6), DARK_BLUE, Color.WHITE), BorderLayout.NORTH);
                inner.add(holder, BorderLayout.WEST);
            }
            inner.add(bubble, BorderLayout.CENTER);

            JPanel row = new JPanel(new BorderLayout()) {
                @Override
                public Dimension getMaximumSize() {
                    return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
                }
            };
            row.setOpaque(false);
            row.setAlignmentX(0.5f);
            row.setBorder(new EmptyBorder(5, 14, 5, 14));
            row.add(inner, fromUser ? BorderLayout.EAST : BorderLayout.WEST);
            return row;
        }

        private void addRow(JComponent row) {
            messages.add(row);
            messages.revalidate();
            messages.repaint();
            javax.swing.Timer t = new javax.swing.Timer(60, e -> {
                JScrollBar bar = scroll.getVerticalScrollBar();
                bar.setValue(bar.getMaximum());
            });
            t.setRepeats(false);
            t.start();
        }

        private void sendMessage(String raw) {
            String text = raw.trim();
            if (busy || text.isEmpty()) return;
            busy = true;
            inputField.setText("");
            addRow(makeRow(true, text, now()));

            statusText.setText("typing...");
            JPanel typing = makeRow(false, "typing...", "");
            addRow(typing);

            String answer = engine.reply(text);
            int delay = 450 + Math.min(answer.length() * 6, 900);
            javax.swing.Timer t = new javax.swing.Timer(delay, e -> {
                messages.remove(typing);
                addRow(makeRow(false, answer, now()));
                statusText.setText("Online");
                busy = false;
                inputField.requestFocusInWindow();
            });
            t.setRepeats(false);
            t.start();
        }
    }

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("console")) {
            runConsole();
            return;
        }
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            new ChatWindow().setVisible(true);
        });
    }

    private static void runConsole() {
        Engine engine = new Engine();
        Scanner sc = new Scanner(System.in);
        System.out.println("=== " + BOT_NAME + " (console mode) - type 'bye' to exit ===");
        while (true) {
            System.out.print("You    : ");
            if (!sc.hasNextLine()) break;
            String line = sc.nextLine();
            String answer = engine.reply(line);
            System.out.println(BOT_NAME + ": " + answer);
            if (NLP.tokenize(line).contains("bye")) break;
        }
    }
}