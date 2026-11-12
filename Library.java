import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;
import java.sql.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.io.IOException;

// ============================================================
// LOOK AND FEEL (blue theme, fonts, custom-painted components)
// ============================================================

class Theme {
    static final Color PRIMARY      = new Color(37, 99, 235);
    static final Color PRIMARY_DARK = new Color(23, 37, 120);
    static final Color PRIMARY_SOFT = new Color(219, 234, 254);
    static final Color BG           = new Color(239, 246, 255);
    static final Color CARD         = Color.WHITE;
    static final Color TEXT         = new Color(15, 23, 42);
    static final Color MUTED        = new Color(100, 116, 139);
    static final Color BORDER       = new Color(203, 213, 225);
    static final Color SUCCESS      = new Color(22, 163, 74);
    static final Color DANGER       = new Color(220, 38, 38);
    static final Color WARNING      = new Color(217, 119, 6);
    static final Color INFO         = new Color(14, 116, 144);
    static final Color GRAY         = new Color(100, 116, 139);

    static Font font(int style, int size) {
        return new Font("Segoe UI", style, size); // falls back to a default font if missing
    }

    static void smooth(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    /** A transparent panel that stacks its children top to bottom. */
    static JPanel column() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    static JLabel label(String text, int style, int size, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font(style, size));
        l.setForeground(color);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    static JLabel caption(String text) {
        JLabel l = label(text, Font.BOLD, 12, MUTED);
        l.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        return l;
    }

    static JLabel title(String text) {
        JLabel l = label(text, Font.BOLD, 28, TEXT);
        l.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        return l;
    }

    static JLabel subtitle(String text) {
        return label(text, Font.PLAIN, 14, MUTED);
    }

    private static javax.swing.border.Border fieldBorder(Color line) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(line, 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10));
    }

    /** Gives a text field a modern style with a blue focus highlight. */
    static void styleField(JTextField f) {
        f.setFont(font(Font.PLAIN, 14));
        f.setForeground(TEXT);
        f.setCaretColor(PRIMARY);
        f.setBorder(fieldBorder(BORDER));
        f.setPreferredSize(new Dimension(200, 38));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        f.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { f.setBorder(fieldBorder(PRIMARY)); }
            @Override public void focusLost(FocusEvent e) { f.setBorder(fieldBorder(BORDER)); }
        });
    }

    /** Adds a small caption + styled field + spacing to a column. */
    static void addField(JPanel column, String caption, JTextField f) {
        column.add(caption(caption));
        styleField(f);
        column.add(f);
        column.add(Box.createVerticalStrut(14));
    }

    /** A full-width button for forms. */
    static StyledButton wide(String text, Color color) {
        StyledButton b = new StyledButton(text, color);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setPreferredSize(new Dimension(100, 42));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        return b;
    }

    /** A plain text link that behaves like a button. */
    static JButton link(String text) {
        JButton b = new JButton(text);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setForeground(PRIMARY);
        b.setFont(font(Font.BOLD, 13));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        return b;
    }
}

/** A panel with rounded corners and an optional border line. */
class RoundedPanel extends JPanel {
    private final int radius;
    private final Color fill;
    private final Color line;

    RoundedPanel(int radius, Color fill, Color line) {
        this.radius = radius;
        this.fill = fill;
        this.line = line;
        setOpaque(false);
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.smooth(g2);
        g2.setColor(fill);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
        if (line != null) {
            g2.setColor(line);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}

/** A diagonal color gradient, optionally with soft decorative circles. */
class GradientPanel extends JPanel {
    private final Color from, to;
    private final boolean decorate;

    GradientPanel(Color from, Color to, boolean decorate) {
        this.from = from;
        this.to = to;
        this.decorate = decorate;
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.smooth(g2);
        int w = getWidth(), h = getHeight();
        g2.setPaint(new GradientPaint(0, 0, from, w, h, to));
        g2.fillRect(0, 0, w, h);
        if (decorate) {
            g2.setColor(new Color(255, 255, 255, 22));
            g2.fillOval(-90, h - 270, 360, 360);
            g2.setColor(new Color(255, 255, 255, 14));
            g2.fillOval(w - 120, -80, 260, 260);
        }
        g2.dispose();
    }
}

/**
 * Background picture used by the login/sign-up page and the home page.
 *
 * If a file named background.jpg (or .jpeg / .png) sits in the same folder
 * as the program, that picture is used. Otherwise a built-in blue library
 * bookshelf picture is drawn, so it works with no extra files.
 */
class BackgroundPanel extends JPanel {
    private static BufferedImage custom;
    private static boolean customTried = false;

    private final int overlayAlpha;
    private BufferedImage cache;

    BackgroundPanel(int overlayAlpha) {
        this.overlayAlpha = overlayAlpha;
        setOpaque(true);
    }

    private static BufferedImage loadCustom() {
        if (!customTried) {
            customTried = true;
            String[] names = {"background.jpg", "background.jpeg", "background.png"};
            for (String n : names) {
                File f = new File(n);
                if (f.exists()) {
                    try {
                        custom = ImageIO.read(f);
                        if (custom != null) break;
                    } catch (IOException ignored) { }
                }
            }
        }
        return custom;
    }

    @Override protected void paintComponent(Graphics g) {
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        if (cache == null || cache.getWidth() != w || cache.getHeight() != h) {
            cache = render(w, h);
        }
        g.drawImage(cache, 0, 0, null);
    }

    private BufferedImage render(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        Theme.smooth(g);
        BufferedImage pic = loadCustom();
        if (pic != null) {
            // scale to cover the whole panel, keeping the picture's shape
            double s = Math.max((double) w / pic.getWidth(), (double) h / pic.getHeight());
            int dw = (int) Math.ceil(pic.getWidth() * s);
            int dh = (int) Math.ceil(pic.getHeight() * s);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(pic, (w - dw) / 2, (h - dh) / 2, dw, dh, null);
        } else {
            drawBookshelfScene(g, w, h);
        }

        // dark blue tint so text stays easy to read
        g.setPaint(new GradientPaint(
                0, 0, new Color(8, 20, 70, overlayAlpha),
                0, h, new Color(8, 20, 70, Math.max(0, overlayAlpha - 50))));
        g.fillRect(0, 0, w, h);
        g.dispose();
        return img;
    }

    private void drawBookshelfScene(Graphics2D g, int w, int h) {
        g.setPaint(new GradientPaint(0, 0, new Color(37, 99, 235),
                w, h, new Color(10, 25, 90)));
        g.fillRect(0, 0, w, h);

        // soft light glow
        g.setColor(new Color(255, 255, 255, 28));
        g.fillOval(w / 2 - 320, -240, 640, 480);
        g.setColor(new Color(255, 255, 255, 16));
        g.fillOval(w - 380, h - 300, 560, 560);
        Color[] spines = {
            new Color(59, 130, 246), new Color(96, 165, 250),
            new Color(147, 197, 253), new Color(191, 219, 254),
            new Color(30, 58, 138),  new Color(37, 99, 235),
            new Color(125, 211, 252), new Color(239, 246, 255)
        };
        for (int row = 0; ; row++) {
            int baseY = 120 + row * 190;
            if (baseY - 150 > h) break;
            Random r = new Random(1000L + row * 77L); // fixed seed: same picture every time
            int x = -10 + r.nextInt(20);
            while (x < w) {
                int bw = 20 + r.nextInt(28);
                int bh = 85 + r.nextInt(60);
                Color c = spines[r.nextInt(spines.length)];
                g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 175));
                g.fillRoundRect(x, baseY - bh, bw, bh, 6, 6);
                g.setColor(new Color(255, 255, 255, 70));
                g.fillRect(x + 3, baseY - bh + 12, bw - 6, 4);
                g.fillRect(x + 3, baseY - bh + 22, bw - 6, 2);
                g.fillRect(x + 3, baseY - 20, bw - 6, 3);
                x += bw + 2 + (r.nextInt(5) == 0 ? 16 : 0);
            }

            // the shelf board
            g.setColor(new Color(10, 25, 80, 235));
            g.fillRect(0, baseY, w, 14);
            g.setColor(new Color(147, 197, 253, 100));
            g.fillRect(0, baseY, w, 2);
        }
    }
}

/** Small logo: an open book on a rounded square. */
class LogoBadge extends JComponent {
    private final Color bg, fg;

    LogoBadge(int size, Color bg, Color fg) {
        this.bg = bg;
        this.fg = fg;
        Dimension d = new Dimension(size, size);
        setPreferredSize(d);
        setMinimumSize(d);
        setMaximumSize(d);
        setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.smooth(g2);
        int w = getWidth(), h = getHeight();
        g2.setColor(bg);
        g2.fillRoundRect(0, 0, w, h, w / 3, w / 3);
        g2.setColor(fg);
        int[] lx = {(int) (w * .20), (int) (w * .47), (int) (w * .47), (int) (w * .20)};
        int[] ly = {(int) (h * .30), (int) (h * .38), (int) (h * .78), (int) (h * .70)};
        int[] rx = {(int) (w * .53), (int) (w * .80), (int) (w * .80), (int) (w * .53)};
        int[] ry = {(int) (h * .38), (int) (h * .30), (int) (h * .70), (int) (h * .78)};
        g2.fillPolygon(lx, ly, 4);
        g2.fillPolygon(rx, ry, 4);
        g2.dispose();
    }
}

/** Round icon for the statistic cards: 0 = books, 1 = check, 2 = clock. */
class StatIcon extends JComponent {
    private final Color color;
    private final int kind;

    StatIcon(Color color, int kind) {
        this.color = color;
        this.kind = kind;
        setPreferredSize(new Dimension(48, 48));
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.smooth(g2);
        int w = getWidth(), h = getHeight();
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 32));
        g2.fillOval(0, 0, w, h);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (kind == 0) {            // three books standing on a shelf
            g2.fillRoundRect(w / 2 - 14, h / 2 - 11, 7, 22, 3, 3);
            g2.fillRoundRect(w / 2 - 4,  h / 2 - 15, 7, 26, 3, 3);
            g2.fillRoundRect(w / 2 + 6,  h / 2 - 8,  7, 19, 3, 3);
        } else if (kind == 1) {     // check mark
            g2.drawPolyline(new int[] {w / 2 - 11, w / 2 - 3, w / 2 + 11},
                            new int[] {h / 2 + 1,  h / 2 + 9, h / 2 - 8}, 3);
        } else {                    // clock
            g2.drawOval(w / 2 - 12, h / 2 - 12, 24, 24);
            g2.drawLine(w / 2, h / 2, w / 2, h / 2 - 8);
            g2.drawLine(w / 2, h / 2, w / 2 + 6, h / 2 + 3);
        }
        g2.dispose();
    }
}

/** A flat rounded button with a hover effect. */
class StyledButton extends JButton {
    private final Color base;
    private boolean hover;

    StyledButton(String text, Color base) {
        super(text);
        this.base = base;
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setForeground(Color.WHITE);
        setFont(Theme.font(Font.BOLD, 13));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
            @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
        });
    }

    private static Color lighter(Color c) {
        if (c.getAlpha() < 255) {
            return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.min(255, c.getAlpha() + 40));
        }
        return new Color(Math.min(255, c.getRed() + 22),
                Math.min(255, c.getGreen() + 22), Math.min(255, c.getBlue() + 22));
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.smooth(g2);
        Color c = base;
        if (!isEnabled()) c = new Color(203, 213, 225);
        else if (getModel().isPressed()) c = base.darker();
        else if (hover) c = lighter(base);
        g2.setColor(c);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
        g2.dispose();
        super.paintComponent(g); // draws the text
    }
}

/** Table header: light blue, small bold capitals. */
class HeaderRenderer extends DefaultTableCellRenderer {
    @Override public Component getTableCellRendererComponent(JTable t, Object value,
            boolean selected, boolean focus, int row, int col) {
        super.getTableCellRendererComponent(t, value, false, false, row, col);
        setText(value == null ? "" : value.toString().toUpperCase());
        setBackground(Theme.PRIMARY_SOFT);
        setForeground(Theme.PRIMARY_DARK);
        setFont(Theme.font(Font.BOLD, 12));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, Theme.PRIMARY),
                BorderFactory.createEmptyBorder(0, 12, 0, 12)));
        return this;
    }
}

/** Table rows: striped, padded, and a colored Status column. */
class BodyRenderer extends DefaultTableCellRenderer {
    @Override public Component getTableCellRendererComponent(JTable t, Object value,
            boolean selected, boolean focus, int row, int col) {
        super.getTableCellRendererComponent(t, value, selected, false, row, col);
        setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        setFont(Theme.font(Font.PLAIN, 13));
        setForeground(Theme.TEXT);
        if (!selected) {
            setBackground(row % 2 == 0 ? Color.WHITE : new Color(245, 249, 255));
        }
        if ("status".equalsIgnoreCase(t.getColumnName(col)) && value != null) {
            String s = value.toString();
            setText(s.isEmpty() ? s : s.charAt(0) + s.substring(1).toLowerCase(Locale.ROOT));
            setFont(Theme.font(Font.BOLD, 13));
            setForeground("AVAILABLE".equalsIgnoreCase(s) ? Theme.SUCCESS : Theme.WARNING);
        }
        return this;
    }
}

// ============================================================
// LIBRARY SYSTEM: home page (sidebar, book table, details panel)
// ============================================================

public class Library extends JFrame {
    static final String DEFAULT_SUBTITLE = "Search by ID, title or category";

    // BOOK CLASS
    static class LibraryBook {
        String id;
        String title;
        String category;
        String status;
        LibraryBook(String id, String title,
                    String category, String status) {
            this.id = id;
            this.title = title;
            this.category = category;
            this.status = status;
        }
    }

    ArrayList<LibraryBook> books;

    DefaultTableModel tableModel;
    JTable bookTable;

    JLabel totalValue = new JLabel("0");
    JLabel availableValue = new JLabel("0");
    JLabel borrowedValue = new JLabel("0");
    JLabel welcomeLabel;

    JTextField searchField = new JTextField();
    JTextField idField = new JTextField();
    JTextField titleField = new JTextField();
    JTextField categoryField = new JTextField();

    final String username;

    public Library(String username) {
        this.username = username;
        setTitle("Library System");
        setSize(1300, 820);
        setMinimumSize(new Dimension(1180, 740));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        // BOOKS COME FROM THE MYSQL DATABASE (sample books are in class Db)
        books = Db.loadBooks();

        // STAT CARDS
        JPanel stats = new JPanel(new GridLayout(1, 3, 18, 0));
        stats.setOpaque(false);
        stats.add(statCard("TOTAL BOOKS", totalValue, Theme.PRIMARY, 0));
        stats.add(statCard("AVAILABLE", availableValue, Theme.SUCCESS, 1));
        stats.add(statCard("BORROWED", borrowedValue, Theme.WARNING, 2));

        // PAGE HEADING (sits on the background picture)
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JPanel headText = Theme.column();
        headText.add(Theme.label("Home", Font.BOLD, 30, Color.WHITE));
        headText.add(Theme.label("Welcome, " + username
                + "! Manage your book collection below.",
                Font.PLAIN, 14, new Color(219, 234, 254)));
        heading.add(headText, BorderLayout.WEST);
        JLabel date = Theme.label(
            LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH)),
            Font.BOLD, 14, new Color(219, 234, 254));
        heading.add(date, BorderLayout.EAST);
        JPanel north = new JPanel(new BorderLayout(0, 16));
        north.setOpaque(false);
        north.add(heading, BorderLayout.NORTH);
        north.add(stats, BorderLayout.CENTER);

        // CONTENT (with the background picture)
        BackgroundPanel content = new BackgroundPanel(140);
        content.setLayout(new BorderLayout(18, 18));
        content.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        content.add(north, BorderLayout.NORTH);
        content.add(createCatalogCard(), BorderLayout.CENTER);
        content.add(createFormCard(), BorderLayout.EAST);

        // MAIN CONTAINER
        JPanel main = new JPanel(new BorderLayout());
        main.add(createSidebar(), BorderLayout.WEST);
        main.add(content, BorderLayout.CENTER);
        setContentPane(main);
        refreshTable();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                quitProgram();
            }
        });
    }

    // SIDEBAR (brand, signed-in user, log out)
    JPanel createSidebar() {
        GradientPanel side = new GradientPanel(
            Theme.PRIMARY_DARK, Theme.PRIMARY, true
        );
        side.setLayout(new BorderLayout());
        side.setPreferredSize(new Dimension(220, 0));
        side.setBorder(BorderFactory.createEmptyBorder(30, 24, 26, 24));
        JPanel top = Theme.column();
        top.add(new LogoBadge(48, new Color(255, 255, 255, 235), Theme.PRIMARY));
        top.add(Box.createVerticalStrut(14));
        top.add(Theme.label("LIBRARY", Font.BOLD, 26, Color.WHITE));
        top.add(Theme.label("SYSTEM", Font.PLAIN, 26, new Color(191, 219, 254)));
        top.add(Box.createVerticalStrut(34));

        // "Home" pill
        RoundedPanel pill = new RoundedPanel(14, new Color(255, 255, 255, 45), null);
        pill.setLayout(new BorderLayout());
        pill.setBorder(BorderFactory.createEmptyBorder(11, 16, 11, 16));
        pill.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        pill.setAlignmentX(Component.LEFT_ALIGNMENT);
        pill.add(Theme.label("Home", Font.BOLD, 14, Color.WHITE), BorderLayout.WEST);
        top.add(pill);
        top.add(Box.createVerticalStrut(34));
        top.add(Theme.label("SIGNED IN AS", Font.BOLD, 11, new Color(147, 197, 253)));
        top.add(Box.createVerticalStrut(4));
        top.add(Theme.label(username, Font.BOLD, 17, Color.WHITE));
        StyledButton logout = Theme.wide("Log Out", new Color(255, 255, 255, 50));
        logout.addActionListener(e -> logout());
        side.add(top, BorderLayout.NORTH);
        side.add(logout, BorderLayout.SOUTH);
        return side;
    }

    // STAT CARD
    RoundedPanel statCard(String title, JLabel value, Color accent, int icon) {
        RoundedPanel card = new RoundedPanel(16, Theme.CARD, null);
        card.setLayout(new BorderLayout(10, 0));
        card.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        JLabel t = new JLabel(title);
        t.setFont(Theme.font(Font.BOLD, 12));
        t.setForeground(Theme.MUTED);
        value.setFont(Theme.font(Font.BOLD, 30));
        value.setForeground(accent);
        JPanel text = new JPanel(new BorderLayout());
        text.setOpaque(false);
        text.add(t, BorderLayout.NORTH);
        text.add(value, BorderLayout.CENTER);
        card.add(text, BorderLayout.CENTER);
        card.add(new StatIcon(accent, icon), BorderLayout.EAST);
        return card;
    }

    // RIGHT CARD: book form and action buttons
    RoundedPanel createFormCard() {
        RoundedPanel card = new RoundedPanel(18, Theme.CARD, null);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(22, 22, 22, 22));
        card.setPreferredSize(new Dimension(330, 0));
        JPanel form = Theme.column();
        JLabel heading = Theme.label("Book Details", Font.BOLD, 18, Theme.TEXT);
        form.add(heading);
        form.add(Theme.label("Fill in the fields to add a book, or",
                Font.PLAIN, 12, Theme.MUTED));
        form.add(Theme.label("click a row to edit it, then Update.",
                Font.PLAIN, 12, Theme.MUTED));
        form.add(Box.createVerticalStrut(18));
        Theme.addField(form, "BOOK ID", idField);
        Theme.addField(form, "BOOK TITLE", titleField);
        Theme.addField(form, "BOOK CATEGORY", categoryField);
        JPanel saveRow = new JPanel(new GridLayout(1, 2, 10, 0));
        saveRow.setOpaque(false);
        saveRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        saveRow.setPreferredSize(new Dimension(100, 42));
        saveRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        saveRow.add(action("Add Book", Theme.SUCCESS, this::addBook));
        saveRow.add(action("Update Book", Theme.PRIMARY, this::updateBook));
        form.add(Box.createVerticalStrut(4));
        form.add(saveRow);
        JPanel grid = new JPanel(new GridLayout(1, 2, 10, 0));
        grid.setOpaque(false);
        grid.add(action("Borrow", Theme.INFO, this::borrowBook));
        grid.add(action("Return", Theme.WARNING, this::returnBook));
        JPanel buttons = new JPanel(new GridLayout(3, 1, 0, 10));
        buttons.setOpaque(false);
        buttons.add(grid);
        buttons.add(action("Remove Book", Theme.DANGER, this::deleteBook));
        buttons.add(action("Clear Fields", Theme.GRAY, this::clearForm));
        JLabel actions = Theme.caption("BOOK ACTIONS");
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(actions, BorderLayout.NORTH);
        bottom.add(buttons, BorderLayout.CENTER);
        card.add(form, BorderLayout.NORTH);
        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }

    StyledButton action(String text, Color color, Runnable task) {
        StyledButton b = new StyledButton(text, color);
        b.addActionListener(e -> task.run());
        return b;
    }

    // CENTER CARD: search bar and the book table
    RoundedPanel createCatalogCard() {
        RoundedPanel card = new RoundedPanel(18, Theme.CARD, null);
        card.setLayout(new BorderLayout(0, 14));
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // TITLES
        welcomeLabel = Theme.label(
            DEFAULT_SUBTITLE, Font.PLAIN, 12, Theme.MUTED
        );
        JPanel titles = Theme.column();
        titles.add(Theme.label("Your Book Collection", Font.BOLD, 18, Theme.TEXT));
        titles.add(welcomeLabel);
        titles.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        // SEARCH BAR
        Theme.styleField(searchField);
        searchField.setToolTipText("Search by ID, title, or category");
        StyledButton search = new StyledButton("Search", Theme.PRIMARY);
        StyledButton showAll = new StyledButton("Show All", Theme.GRAY);
        search.addActionListener(e -> searchBook());
        searchField.addActionListener(e -> searchBook());
        showAll.addActionListener(e -> {
            searchField.setText("");
            refreshTable();
        });
        JPanel buttons = new JPanel(new GridLayout(1, 2, 8, 0));
        buttons.setOpaque(false);
        buttons.add(search);
        buttons.add(showAll);
        JPanel bar = new JPanel(new BorderLayout(10, 0));
        bar.setOpaque(false);
        bar.add(searchField, BorderLayout.CENTER);
        bar.add(buttons, BorderLayout.EAST);
        JPanel top = new JPanel(new BorderLayout(0, 4));
        top.setOpaque(false);
        top.add(titles, BorderLayout.NORTH);
        top.add(bar, BorderLayout.CENTER);

        // TABLE
        String[] columns = {
            "BOOK ID", "BOOK TITLE", "CATEGORY", "STATUS"
        };
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        bookTable = new JTable(tableModel);
        styleTable();
        JScrollPane scroll = new JScrollPane(bookTable);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        scroll.getViewport().setBackground(Color.WHITE);
        card.add(top, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);

        // clicking a row fills the form
        bookTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int row = bookTable.getSelectedRow();
            if (row < 0) return;
            int m = bookTable.convertRowIndexToModel(row);
            idField.setText(cell(m, 0));
            titleField.setText(cell(m, 1));
            categoryField.setText(cell(m, 2));
        });
        return card;
    }

    void styleTable() {
        bookTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bookTable.setRowHeight(34);
        bookTable.setShowGrid(false);
        bookTable.setIntercellSpacing(new Dimension(0, 0));
        bookTable.setFont(Theme.font(Font.PLAIN, 13));
        bookTable.setSelectionBackground(Theme.PRIMARY_SOFT);
        bookTable.setSelectionForeground(Theme.TEXT);
        bookTable.setFillsViewportHeight(true);
        JTableHeader header = bookTable.getTableHeader();
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(0, 40));
        header.setDefaultRenderer(new HeaderRenderer());
        bookTable.setDefaultRenderer(Object.class, new BodyRenderer());
        int[] widths = {110, 320, 220, 120};
        for (int i = 0; i < widths.length; i++) {
            bookTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    /** Null-safe read of a table cell (by model row). */
    String cell(int modelRow, int col) {
        Object v = tableModel.getValueAt(modelRow, col);
        return v == null ? "" : v.toString();
    }

    void clearForm() {
        idField.setText("");
        titleField.setText("");
        categoryField.setText("");
        bookTable.clearSelection();
    }

    LibraryBook find(String id) {
        for (LibraryBook b : books) {
            if (b.id.equalsIgnoreCase(id)) return b;
        }
        return null;
    }

    // ADD BOOK (uses the form on the right)
    void addBook() {
        String id = idField.getText().trim();
        String title = titleField.getText().trim();
        String category = categoryField.getText().trim();
        if (id.isEmpty() || title.isEmpty() || category.isEmpty()) {
            showMessage("Please complete all book information!");
            return;
        }
        if (find(id) != null) {
            showMessage("That Book ID already exists!");
            return;
        }
        LibraryBook book = new LibraryBook(id, title, category, "Available");
        if (!Db.addBook(book)) {
            showMessage(Db.ERROR);
            return;
        }
        books.add(book);
        refreshTable();
        showMessage("Your new book has been added! ✨");
    }

    // UPDATE BOOK: click a row, change the fields, press Update Book
    void updateBook() {
        int row = bookTable.getSelectedRow();
        if (row < 0) {
            showMessage("Click a book in the table first, then change its details.");
            return;
        }
        LibraryBook book = find(cell(bookTable.convertRowIndexToModel(row), 0));
        String id = idField.getText().trim();
        String title = titleField.getText().trim();
        String category = categoryField.getText().trim();
        if (book == null) {
            showMessage("Book not found!");
            return;
        }
        if (id.isEmpty() || title.isEmpty() || category.isEmpty()) {
            showMessage("Please complete all book information!");
            return;
        }
        LibraryBook other = find(id);
        if (other != null && other != book) {
            showMessage("That Book ID already exists!");
            return;
        }
        if (!Db.updateBook(book.id, id, title, category)) {
            showMessage(Db.ERROR);
            return;
        }
        book.id = id;
        book.title = title;
        book.category = category;
        refreshTable();
        showMessage("Book updated successfully! ✏️");
    }

    void borrowBook() {
        changeStatus("Enter the Book ID to borrow:", "Available", "Borrowed",
            "Enjoy reading %s! 📖", "This book is already borrowed!");
    }

    void returnBook() {
        changeStatus("Enter the Book ID to return:", "Borrowed", "Available",
            "Thank you for returning %s! ✨", "This book is already available!");
    }

    // shared by Borrow and Return
    void changeStatus(String ask, String from, String to, String okMsg, String badMsg) {
        String id = getSelectedOrEnteredId(ask);
        if (id == null) return;
        LibraryBook book = find(id);
        if (book == null) {
            showMessage("Book not found!");
        } else if (!book.status.equals(from)) {
            showMessage(badMsg);
        } else if (!Db.setStatus(book.id, to)) {
            showMessage(Db.ERROR);
        } else {
            book.status = to;
            refreshTable();
            showMessage(String.format(okMsg, book.title));
        }
    }

    // REMOVE BOOK
    void deleteBook() {
        String id = getSelectedOrEnteredId("Enter the Book ID to remove:");
        if (id == null) return;
        LibraryBook book = find(id);
        if (book == null) {
            showMessage("Book not found!");
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
            "Remove \"" + book.title + "\" from the library?",
            "Confirm Removal", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) return;
        if (Db.deleteBook(book.id)) {
            books.remove(book);
            refreshTable();
            showMessage("Book removed successfully!");
        } else {
            showMessage(Db.ERROR);
        }
    }

    // GET SELECTED ROW, OR THE ID IN THE FORM, OR ASK FOR ONE
    String getSelectedOrEnteredId(String message) {
        int row = bookTable.getSelectedRow();
        if (row != -1) {
            return tableModel.getValueAt(
                bookTable.convertRowIndexToModel(row), 0
            ).toString();
        }
        String typed = idField.getText().trim();
        if (!typed.isEmpty()) {
            return typed;
        }
        String id = JOptionPane.showInputDialog(
            this, message
        );
        if (id == null) return null;
        return id.trim();
    }

    // SEARCH BOOK
    void searchBook() {
        String keyword = searchField.getText()
            .trim().toLowerCase();
        if (keyword.isEmpty()) {
            keyword = JOptionPane.showInputDialog(
                this,
                "Search by Book ID, Title, or Category:"
            );
            if (keyword == null) return;
            keyword = keyword.trim().toLowerCase();
        }
        if (keyword.isEmpty()) return;
        tableModel.setRowCount(0);
        int matches = 0;
        for (LibraryBook book : books) {
            if (book.id.toLowerCase().contains(keyword)
                || book.title.toLowerCase().contains(keyword)
                || book.category.toLowerCase().contains(keyword)) {
                tableModel.addRow(new Object[] {
                    book.id,
                    book.title,
                    book.category,
                    book.status
                });
                matches++;
            }
        }
        if (matches == 0) {
            showMessage("No matching books found!");
        } else {
            welcomeLabel.setText(
                matches + " matching book(s) found."
            );
        }
    }

    // REFRESH TABLE AND STATISTICS
    void refreshTable() {
        tableModel.setRowCount(0);
        int available = 0;
        int borrowed = 0;
        for (LibraryBook book : books) {
            tableModel.addRow(new Object[] {
                book.id,
                book.title,
                book.category,
                book.status
            });
            if (book.status.equals("Available")) {
                available++;
            } else {
                borrowed++;
            }
        }
        totalValue.setText(String.valueOf(books.size()));
        availableValue.setText(String.valueOf(available));
        borrowedValue.setText(String.valueOf(borrowed));
        welcomeLabel.setText(DEFAULT_SUBTITLE);
        clearForm();
    }

    // MESSAGE
    void showMessage(String message) {
        JOptionPane.showMessageDialog(
            this,
            message,
            "LIBRARY SYSTEM",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    // LOGOUT
    void logout() {
        int choice = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to logout?",
            "Logout",
            JOptionPane.YES_NO_OPTION
        );
        if (choice == JOptionPane.YES_OPTION) {
            dispose();
            new LoginFrame().setVisible(true);
        }
    }

    // QUIT
    void quitProgram() {
        int choice = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to close the Library System?",
            "Exit Library",
            JOptionPane.YES_NO_OPTION
        );
        if (choice == JOptionPane.YES_OPTION) {
            JOptionPane.showMessageDialog(this, "Thanks for using the Library System! Happy reading! 💙");
            System.exit(0);
        }
    }

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        SwingUtilities.invokeLater(() -> {
            String error = Db.init();
            if (error != null) {
                JOptionPane.showMessageDialog(null,
                    "Could not connect to MySQL.\n\n" + error
                    + "\n\nCheck that MySQL is running, that the mysql-connector jar is in this"
                    + "\nfolder, and that the user/password at the top of class Db are correct.",
                    "Database Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
            new LoginFrame().setVisible(true);
        });
    }
}

// ============================================================
// LOGIN / SIGN UP PAGE (one card on a background picture)
// ============================================================

class LoginFrame extends JFrame {
    // login fields
    JTextField usernameField = new JTextField();
    JPasswordField passwordField = new JPasswordField();

    // sign-up fields
    JTextField newUsername = new JTextField();
    JPasswordField newPassword = new JPasswordField();
    JPasswordField confirmPassword = new JPasswordField();

    CardLayout cards = new CardLayout();
    JPanel cardHolder = new JPanel(cards);

    StyledButton loginButton;
    StyledButton signupButton;

    LoginFrame() {
        setTitle("Library System | Login");
        setSize(1000, 740);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        cardHolder.setOpaque(false);
        cardHolder.add(buildLoginForm(), "login");
        cardHolder.add(buildSignupForm(), "signup");

        // white card in the middle of the picture
        RoundedPanel card = new RoundedPanel(28, new Color(255, 255, 255, 245), null) {
            @Override public Dimension getPreferredSize() {
                return new Dimension(430, super.getPreferredSize().height); // fixed card width
            }
        };
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        JPanel brand = Theme.column();
        brand.add(new LogoBadge(52, Theme.PRIMARY, Color.WHITE));
        brand.add(Box.createVerticalStrut(12));
        brand.add(Theme.label("LIBRARY SYSTEM", Font.BOLD, 26, Theme.PRIMARY_DARK));
        brand.add(Theme.label("A little place for big imaginations.",
                Font.PLAIN, 13, Theme.MUTED));
        brand.add(Box.createVerticalStrut(20));
        card.add(brand, BorderLayout.NORTH);
        card.add(cardHolder, BorderLayout.CENTER);
        BackgroundPanel screen = new BackgroundPanel(95);
        screen.setLayout(new GridBagLayout());
        screen.add(card);
        setContentPane(screen);
        showCard("login");
    }

    JPanel buildLoginForm() {
        JCheckBox show = showBox(passwordField);
        JPanel form = Theme.column();
        form.add(Theme.title("Welcome back"));
        form.add(Theme.subtitle("Login to access your library."));
        form.add(Box.createVerticalStrut(20));
        Theme.addField(form, "USERNAME", usernameField);
        Theme.addField(form, "PASSWORD", passwordField);
        form.add(show);
        form.add(Box.createVerticalStrut(16));
        loginButton = Theme.wide("Log In", Theme.PRIMARY);
        loginButton.addActionListener(e -> login());
        form.add(loginButton);
        form.add(Box.createVerticalStrut(12));
        JButton toSignup = Theme.link("New here? Create an account");
        toSignup.addActionListener(e -> showCard("signup"));
        form.add(toSignup);
        return form;
    }

    JPanel buildSignupForm() {
        JCheckBox show = showBox(newPassword, confirmPassword);
        JPanel form = Theme.column();
        form.add(Theme.title("Create account"));
        form.add(Theme.subtitle("Sign up to start managing books."));
        form.add(Box.createVerticalStrut(20));
        Theme.addField(form, "USERNAME", newUsername);
        Theme.addField(form, "PASSWORD", newPassword);
        Theme.addField(form, "CONFIRM PASSWORD", confirmPassword);
        form.add(show);
        form.add(Box.createVerticalStrut(16));
        signupButton = Theme.wide("Sign Up", Theme.PRIMARY);
        signupButton.addActionListener(e -> signup());
        form.add(signupButton);
        form.add(Box.createVerticalStrut(12));
        JButton toLogin = Theme.link("Already have an account? Log in");
        toLogin.addActionListener(e -> showCard("login"));
        form.add(toLogin);
        return form;
    }

    /** "Show Password" check box that reveals the given password fields. */
    JCheckBox showBox(JPasswordField... fields) {
        JCheckBox box = new JCheckBox("Show Password");
        box.setOpaque(false);
        box.setFocusPainted(false);
        box.setFont(Theme.font(Font.PLAIN, 13));
        box.setForeground(Theme.MUTED);
        box.setAlignmentX(Component.LEFT_ALIGNMENT);
        box.addActionListener(e -> {
            for (JPasswordField f : fields) {
                f.setEchoChar(box.isSelected() ? (char) 0 : '•');
            }
        });
        return box;
    }

    void showCard(String name) {
        cards.show(cardHolder, name);
        getRootPane().setDefaultButton(
            name.equals("login") ? loginButton : signupButton
        );
        setTitle(name.equals("login")
            ? "Library System | Login" : "Library System | Sign Up");
    }

    // LOGIN
    void login() {
        String username = usernameField.getText().trim();
        String password = new String(
            passwordField.getPassword()
        );
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter your username and password.");
            return;
        }
        if (Db.login(username, password)) {
            JOptionPane.showMessageDialog(this, "Welcome to the Library System, " + username + "! 💙");
            dispose();
            new Library(username).setVisible(true);
        } else {
            JOptionPane.showMessageDialog(
                this,
                "Incorrect username or password!",
                "Login Failed",
                JOptionPane.ERROR_MESSAGE
            );
            passwordField.setText("");
        }
    }

    // SIGN UP
    void signup() {
        String username = newUsername.getText().trim();
        String password = new String(newPassword.getPassword());
        String confirm = new String(confirmPassword.getPassword());
        if (username.isEmpty() || password.isEmpty()
            || confirm.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please complete all fields!");
            return;
        }
        if (password.length() < 6) {
            JOptionPane.showMessageDialog(this, "Password must have at least 6 characters!");
            return;
        }
        if (!password.equals(confirm)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match!");
            return;
        }
        if (Db.usernameExists(username)) {
            JOptionPane.showMessageDialog(this, "Username already exists!");
            return;
        }
        if (Db.register(username, password)) {
            JOptionPane.showMessageDialog(this, "Your account has been created! Welcome to the Library System! 💙");
            usernameField.setText(username);
            passwordField.setText("");
            newUsername.setText("");
            newPassword.setText("");
            confirmPassword.setText("");
            showCard("login");
        } else {
            JOptionPane.showMessageDialog(this, "Unable to create account. Please try again.");
        }
    }
}

// ============================================================
// MYSQL DATABASE (accounts + books)
// ============================================================

class Db {
    // ---- EDIT THESE 3 LINES to match your MySQL ----
    static final String URL = "jdbc:mysql://localhost:3306/library_system"
        + "?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true";
    static final String USER = "root";
    static final String PASSWORD = "shirooo1201";
    // ------------------------------------------------

    static final String ERROR = "Database error! Could not save the change to MySQL.";

    // sample books, added only the first time (when the books table is empty)
    static final String[][] SAMPLE = {
        {"BK-1001", "To Kill a Mockingbird", "Classic", "Available"},
        {"BK-1002", "The Great Gatsby", "Classic", "Available"},
        {"BK-1003", "Atomic Habits", "Self-Help", "Available"},
        {"BK-1004", "Sapiens", "History", "Available"},
        {"BK-1005", "The Hobbit", "Fantasy", "Borrowed"},
        {"BK-1006", "Clean Code", "Programming", "Available"},
        {"BK-1007", "A Brief History of Time", "Science", "Available"},
        {"BK-1008", "The Alchemist", "Adventure", "Available"},
        {"BK-1009", "Pride and Prejudice", "Romance", "Available"},
        {"BK-1010", "The Diary of a Young Girl", "Biography", "Borrowed"},
        {"BK-1011", "Introduction to Algorithms", "Education", "Available"},
        {"BK-1012", "Dune", "Science Fiction", "Available"}
    };

    private static Connection con;

    private static Connection get() throws SQLException {
        if (con == null || con.isClosed()) {
            con = DriverManager.getConnection(URL, USER, PASSWORD);
        }
        return con;
    }

    /** Connects and creates the tables. Returns null if OK, or the error text. */
    static String init() {
        try (Statement st = get().createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS users ("
                + "username VARCHAR(50) PRIMARY KEY, password_hash CHAR(64) NOT NULL)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS books ("
                + "book_id VARCHAR(20) PRIMARY KEY, title VARCHAR(150) NOT NULL, "
                + "category VARCHAR(60) NOT NULL, status VARCHAR(10) NOT NULL)");
            ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM books");
            rs.next();
            if (rs.getInt(1) == 0) {
                for (String[] b : SAMPLE) {
                    run("INSERT INTO books VALUES (?, ?, ?, ?)", b);
                }
            }
            return null;
        } catch (SQLException e) {
            return e.getMessage();
        }
    }

    /** Runs an INSERT / UPDATE / DELETE. Returns false if it failed. */
    private static boolean run(String sql, String... args) {
        try (PreparedStatement ps = get().prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) ps.setString(i + 1, args[i]);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return false;
        }
    }

    /** True if the query returns at least one row. */
    private static boolean exists(String sql, String... args) {
        try (PreparedStatement ps = get().prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) ps.setString(i + 1, args[i]);
            return ps.executeQuery().next();
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return false;
        }
    }

    // ---------- books ----------
    static ArrayList<Library.LibraryBook> loadBooks() {
        ArrayList<Library.LibraryBook> list = new ArrayList<>();
        try (Statement st = get().createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT book_id, title, category, status FROM books ORDER BY book_id")) {
            while (rs.next()) {
                list.add(new Library.LibraryBook(rs.getString(1), rs.getString(2),
                    rs.getString(3), rs.getString(4)));
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return list;
    }

    static boolean addBook(Library.LibraryBook b) {
        return run("INSERT INTO books VALUES (?, ?, ?, ?)", b.id, b.title, b.category, b.status);
    }

    static boolean updateBook(String oldId, String id, String title, String category) {
        return run("UPDATE books SET book_id = ?, title = ?, category = ? WHERE book_id = ?",
            id, title, category, oldId);
    }

    static boolean setStatus(String id, String status) {
        return run("UPDATE books SET status = ? WHERE book_id = ?", status, id);
    }

    static boolean deleteBook(String id) {
        return run("DELETE FROM books WHERE book_id = ?", id);
    }

    // ---------- accounts ----------
    private static String hash(String value) {
        try {
            byte[] result = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : result) hex.append(String.format("%02x", b & 0xff));
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static boolean usernameExists(String username) {
        return exists("SELECT 1 FROM users WHERE username = ?", username.toLowerCase());
    }

    static boolean register(String username, String password) {
        return !usernameExists(username)
            && run("INSERT INTO users VALUES (?, ?)", username.toLowerCase(), hash(password));
    }

    static boolean login(String username, String password) {
        return exists("SELECT 1 FROM users WHERE username = ? AND password_hash = ?",
            username.toLowerCase(), hash(password));
    }
}