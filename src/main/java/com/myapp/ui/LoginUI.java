package com.myapp.ui;

import com.myapp.db.ConnexionSQLite;
import com.myapp.db.GestionBackup;
import com.myapp.util.SessionManager;
import com.myapp.util.SocieteConfig;

import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.*;

/**
 * REDESIGN : meme systeme visuel "aurora" que l'application Bibliotheque
 * (fond degrade sombre anime avec orbes lumineux, carte vitree semi-
 * transparente, champs a halo bleu au focus, bouton en degrade) - repris
 * fidelement pour une identite visuelle coherente entre les deux
 * applications. Toute la logique metier (connexion, session, sauvegarde a
 * la fermeture) reste strictement identique a avant.
 */
public class LoginUI extends JFrame {

    private static final Color BG_DARK_1     = new Color(9, 13, 26);
    private static final Color BG_DARK_2     = new Color(28, 37, 58);
    private static final Color ORB_AMBER     = new Color(251, 146, 60);
    private static final Color ORB_PURPLE    = new Color(139, 92, 246);
    private static final Color ORB_CYAN      = new Color(34, 211, 238);
    private static final Color ORB_PINK      = new Color(236, 72, 153);
    private static final Color CARD_TOP      = new Color(30, 41, 59, 235);
    private static final Color CARD_BOTTOM   = new Color(15, 23, 42, 235);
    private static final Color CARD_BORDER   = new Color(148, 163, 184, 60);
    private static final Color TEXT_WHITE    = new Color(241, 245, 249);
    private static final Color TEXT_SUBTLE   = new Color(148, 163, 184);
    private static final Color TEXT_LABEL    = new Color(203, 213, 225);
    private static final Color FIELD_BG      = new Color(15, 23, 42, 210);
    private static final Color FIELD_BORDER  = new Color(71, 85, 105);
    // ★ COULEUR D'ACCENT : rose/magenta moderne, pour différencier Chaa Elect
    // de l'application Bibliothèque (qui garde le bleu).
    private static final Color FIELD_FOCUS   = new Color(219, 39, 119);
    private static final Color BTN_BASE      = new Color(219, 39, 119);
    private static final Color BTN_HOVER     = new Color(190, 24, 93);
    private static final Color MSG_ERROR     = new Color(248, 113, 113);
    private static final Color MSG_SUCCESS   = new Color(74, 222, 128);

    private JTextField txtNom;
    private JPasswordField txtMotDePasse;
    private JButton btnConnexion;
    private JLabel lblMessage;
    private Font fontAwesomeSolid;
    private FocusFieldPanel userFieldPanel;
    private FocusFieldPanel pwdFieldPanel;
    private GlowBackgroundPanel glowBackground;

    public LoginUI() {
        this.loadFontAwesome();

        this.setTitle("Systeme de Facturation - Connexion");
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setLayout(new BorderLayout());

        SessionManager session = SessionManager.getInstance();
        if (session.isSessionActive()) {
            this.redirectToDashboard();
        } else {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                // Ignore
            }
            this.initUI();
        }
    }

    @Override
    protected void processWindowEvent(java.awt.event.WindowEvent e) {
        if (e.getID() == java.awt.event.WindowEvent.WINDOW_CLOSING) {
            if (this.glowBackground != null) this.glowBackground.stopAnimation();
            quitterApplicationAvecBackup();
        } else {
            super.processWindowEvent(e);
        }
    }

    private void quitterApplicationAvecBackup() {
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            System.out.println("Fermeture de l'application : Lancement du backup...");
            boolean succes = GestionBackup.effectuerBackupFermeture();
            if (succes) {
                System.out.println("Backup termine avec succes avant fermeture.");
            } else {
                System.err.println("Le backup a rencontre un probleme, mais l'app va se fermer.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            System.exit(0);
        }
    }

    private void loadFontAwesome() {
        try {
            String path = "/fonts/fa.ttf";
            InputStream fontStream = this.getClass().getResourceAsStream(path);
            if (fontStream != null) {
                Font font = Font.createFont(Font.TRUETYPE_FONT, fontStream);
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                ge.registerFont(font);
                this.fontAwesomeSolid = font;
                fontStream.close();
            } else {
                this.fontAwesomeSolid = new Font("SansSerif", Font.PLAIN, 12);
            }
        } catch (Exception e) {
            this.fontAwesomeSolid = new Font("SansSerif", Font.PLAIN, 12);
        }
    }

    private void initUI() {
        this.glowBackground = new GlowBackgroundPanel();
        this.glowBackground.setLayout(new GridBagLayout());

        JPanel contentPanel = this.createContentPanel();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0D;
        gbc.weighty = 1.0D;
        gbc.anchor = GridBagConstraints.CENTER;
        this.glowBackground.add(contentPanel, gbc);

        this.add(this.glowBackground, BorderLayout.CENTER);

        this.btnConnexion.addActionListener((e) -> this.verifierConnexion());
        this.txtMotDePasse.addActionListener((e) -> this.verifierConnexion());
        this.txtNom.addActionListener((e) -> this.verifierConnexion());

        SwingUtilities.invokeLater(() -> this.txtNom.requestFocus());
    }

    private JPanel createContentPanel() {
        GlassCard contentPanel = new GlassCard();
        contentPanel.setLayout(new GridBagLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(40, 50, 40, 50));
        contentPanel.setPreferredSize(new Dimension(450, 560));
        contentPanel.setMinimumSize(new Dimension(350, 500));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.weightx = 1.0;

        JLabel lblTitre = new JLabel("CONNEXION", JLabel.CENTER);
        lblTitre.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblTitre.setForeground(TEXT_WHITE);
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 5, 0);
        contentPanel.add(lblTitre, gbc);

        String nomSociete = SocieteConfig.getNom();
        JLabel lblSousTitre = new JLabel(
                (nomSociete != null && !nomSociete.isEmpty()) ? nomSociete : "Espace Administrateur",
                JLabel.CENTER);
        lblSousTitre.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblSousTitre.setForeground(TEXT_SUBTLE);
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 40, 0);
        contentPanel.add(lblSousTitre, gbc);

        JLabel lblNom = new JLabel("Nom d'utilisateur");
        lblNom.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblNom.setForeground(TEXT_LABEL);
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 5, 0);
        contentPanel.add(lblNom, gbc);

        this.userFieldPanel = new FocusFieldPanel();

        JLabel userIcon = new JLabel("\uf007");
        userIcon.setFont(this.fontAwesomeSolid != null ? this.fontAwesomeSolid.deriveFont(16f) : new Font("SansSerif", Font.PLAIN, 16));
        userIcon.setForeground(TEXT_SUBTLE);
        userIcon.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 5));
        userIcon.setOpaque(false);

        this.txtNom = new JTextField();
        this.txtNom.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        this.txtNom.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 12));
        this.txtNom.setOpaque(false);
        this.txtNom.setForeground(TEXT_WHITE);
        this.txtNom.setCaretColor(TEXT_WHITE);
        this.txtNom.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                userFieldPanel.setFocused(true);
                userIcon.setForeground(FIELD_FOCUS);
            }
            public void focusLost(FocusEvent e) {
                userFieldPanel.setFocused(false);
                userIcon.setForeground(TEXT_SUBTLE);
            }
        });

        this.userFieldPanel.add(userIcon, BorderLayout.WEST);
        this.userFieldPanel.add(this.txtNom, BorderLayout.CENTER);

        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 20, 0);
        contentPanel.add(this.userFieldPanel, gbc);

        JLabel lblPwd = new JLabel("Mot de passe");
        lblPwd.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblPwd.setForeground(TEXT_LABEL);
        gbc.gridy = 4;
        gbc.insets = new Insets(0, 0, 5, 0);
        contentPanel.add(lblPwd, gbc);

        this.pwdFieldPanel = new FocusFieldPanel();

        JLabel pwdIcon = new JLabel("\uf023");
        pwdIcon.setFont(this.fontAwesomeSolid != null ? this.fontAwesomeSolid.deriveFont(16f) : new Font("SansSerif", Font.PLAIN, 16));
        pwdIcon.setForeground(TEXT_SUBTLE);
        pwdIcon.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 5));
        pwdIcon.setOpaque(false);

        this.txtMotDePasse = new JPasswordField();
        this.txtMotDePasse.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        this.txtMotDePasse.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 12));
        this.txtMotDePasse.setOpaque(false);
        this.txtMotDePasse.setForeground(TEXT_WHITE);
        this.txtMotDePasse.setCaretColor(TEXT_WHITE);
        this.txtMotDePasse.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                pwdFieldPanel.setFocused(true);
                pwdIcon.setForeground(FIELD_FOCUS);
            }
            public void focusLost(FocusEvent e) {
                pwdFieldPanel.setFocused(false);
                pwdIcon.setForeground(TEXT_SUBTLE);
            }
        });

        this.pwdFieldPanel.add(pwdIcon, BorderLayout.WEST);
        this.pwdFieldPanel.add(this.txtMotDePasse, BorderLayout.CENTER);

        gbc.gridy = 5;
        gbc.insets = new Insets(0, 0, 30, 0);
        contentPanel.add(this.pwdFieldPanel, gbc);

        this.btnConnexion = new GradientButton("SE CONNECTER");
        this.btnConnexion.setFont(new Font("Segoe UI", Font.BOLD, 14));
        this.btnConnexion.setBackground(BTN_BASE);
        this.btnConnexion.setForeground(Color.WHITE);
        this.btnConnexion.setFocusPainted(false);
        this.btnConnexion.setCursor(new Cursor(Cursor.HAND_CURSOR));
        this.btnConnexion.setBorder(BorderFactory.createEmptyBorder(13, 0, 13, 0));

        this.btnConnexion.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                if (LoginUI.this.btnConnexion.isEnabled()) {
                    LoginUI.this.btnConnexion.setBackground(BTN_HOVER);
                }
            }
            public void mouseExited(MouseEvent evt) {
                if (LoginUI.this.btnConnexion.isEnabled()) {
                    LoginUI.this.btnConnexion.setBackground(BTN_BASE);
                }
            }
        });

        gbc.gridy = 6;
        gbc.insets = new Insets(0, 0, 20, 0);
        contentPanel.add(this.btnConnexion, gbc);

        this.lblMessage = new JLabel("", JLabel.CENTER);
        this.lblMessage.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        this.lblMessage.setForeground(MSG_ERROR);
        gbc.gridy = 7;
        gbc.insets = new Insets(0, 0, 10, 0);
        contentPanel.add(this.lblMessage, gbc);

        JLabel lblInfo = new JLabel("Systeme de Facturation v2.1", JLabel.CENTER);
        lblInfo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblInfo.setForeground(TEXT_SUBTLE);
        gbc.gridy = 8;
        gbc.insets = new Insets(10, 0, 0, 0);
        contentPanel.add(lblInfo, gbc);

        return contentPanel;
    }

    private void verifierConnexion() {
        String nom = this.txtNom.getText().trim();
        String motDePasse = new String(this.txtMotDePasse.getPassword());

        this.btnConnexion.setText("CONNEXION...");
        this.btnConnexion.setBackground(new Color(100, 100, 100));
        this.btnConnexion.setEnabled(false);

        Timer timer = new Timer(500, (e) -> {
            if (!nom.isEmpty() && !motDePasse.isEmpty()) {
                try {
                    Connection conn = ConnexionSQLite.getConnection();
                    // ★ CORRECTIF : le mot de passe était comparé (et stocké) en
                    // clair. Il est désormais haché (PBKDF2) ; un ancien mot de
                    // passe en clair est accepté une fois puis converti.
                    PreparedStatement pst = conn.prepareStatement("SELECT id, pswd FROM admin WHERE nom = ?");
                    pst.setString(1, nom);
                    ResultSet rs = pst.executeQuery();

                    boolean ok = false;
                    while (!ok && rs.next()) {
                        String stocke = rs.getString("pswd");
                        if (com.myapp.util.PasswordUtil.verifier(motDePasse, stocke)) {
                            ok = true;
                            if (!com.myapp.util.PasswordUtil.estHache(stocke)) {
                                try (PreparedStatement up = conn.prepareStatement("UPDATE admin SET pswd = ? WHERE id = ?")) {
                                    up.setString(1, com.myapp.util.PasswordUtil.hacher(motDePasse));
                                    up.setInt(2, rs.getInt("id"));
                                    up.executeUpdate();
                                }
                            }
                        }
                    }

                    if (ok) {
                        SessionManager.getInstance().startSession(nom, "administrateur");
                        this.lblMessage.setText("Connexion reussie !");
                        this.lblMessage.setForeground(MSG_SUCCESS);

                        Timer redirectTimer = new Timer(500, (evt) -> this.redirectToDashboard());
                        redirectTimer.setRepeats(false);
                        redirectTimer.start();
                    } else {
                        this.lblMessage.setText("Identifiants incorrects");
                        this.lblMessage.setForeground(MSG_ERROR);
                        this.resetBoutonConnexion();
                    }

                    rs.close();
                    pst.close();
                    conn.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    this.lblMessage.setText("Erreur Base de Donnees");
                    this.resetBoutonConnexion();
                }
            } else {
                this.lblMessage.setText("Veuillez remplir tous les champs");
                this.resetBoutonConnexion();
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void redirectToDashboard() {
        if (this.glowBackground != null) this.glowBackground.stopAnimation();
        (new AdminDashboard()).setVisible(true);
        this.dispose();
    }

    private void resetBoutonConnexion() {
        this.btnConnexion.setText("SE CONNECTER");
        this.btnConnexion.setBackground(BTN_BASE);
        this.btnConnexion.setEnabled(true);
    }

    public static void main(String[] args) {
        // ★ Backup au démarrage + backup à l'arrêt (extinction Windows, etc.)
        GestionBackup.installerSauvegardesAutomatiques();
        installerFermetureSansFenetre();
        SwingUtilities.invokeLater(() -> {
            (new LoginUI()).setVisible(true);
        });
    }

    /**
     * ★ CORRECTIF : fermer certaines fenêtres avec ✕ (Facture, Stock…) les
     * détruisait sans rouvrir d'écran : l'application restait alors ouverte en
     * arrière-plan, invisible, et ne faisait jamais son backup de fermeture.
     * Désormais, si plus aucune fenêtre n'est affichée pendant 2 secondes
     * (délai pour laisser s'ouvrir l'écran suivant), on quitte proprement :
     * le backup d'arrêt est fait par GestionBackup.
     */
    private static void installerFermetureSansFenetre() {
        Timer verification = new Timer(2000, ev -> {
            for (java.awt.Window w : java.awt.Window.getWindows()) {
                if (w.isShowing()) return;
            }
            System.out.println("Plus aucune fenêtre ouverte : fermeture de l'application.");
            System.exit(0);
        });
        verification.setRepeats(false);
        java.awt.Toolkit.getDefaultToolkit().addAWTEventListener(e -> {
            if (e.getID() == java.awt.event.WindowEvent.WINDOW_CLOSED) verification.restart();
        }, java.awt.AWTEvent.WINDOW_EVENT_MASK);
    }

    private static class GlowBackgroundPanel extends JPanel {
        private float t = 0f;
        private final Timer animTimer;

        GlowBackgroundPanel() {
            setOpaque(true);
            this.animTimer = new Timer(45, e -> {
                this.t += 0.015f;
                repaint();
            });
            this.animTimer.start();
        }

        void stopAnimation() {
            if (this.animTimer.isRunning()) this.animTimer.stop();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int w = getWidth(), h = getHeight();
            if (w <= 0 || h <= 0) return;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setPaint(new GradientPaint(0, 0, BG_DARK_1, w, h, BG_DARK_2));
            g2.fillRect(0, 0, w, h);

            drawOrb(g2, (float) (w * 0.16 + 45 * Math.sin(this.t)), (float) (h * 0.22 + 35 * Math.cos(this.t * 0.8)), Math.min(w, h) * 0.32f, ORB_AMBER);
            drawOrb(g2, (float) (w * 0.86 + 35 * Math.cos(this.t * 0.6)), (float) (h * 0.18 + 40 * Math.sin(this.t * 0.7)), Math.min(w, h) * 0.26f, ORB_PURPLE);
            drawOrb(g2, (float) (w * 0.78 + 40 * Math.sin(this.t * 0.5)), (float) (h * 0.88 + 30 * Math.cos(this.t)), Math.min(w, h) * 0.34f, ORB_CYAN);
            drawOrb(g2, (float) (w * 0.14 + 30 * Math.cos(this.t * 0.9)), (float) (h * 0.82 + 35 * Math.sin(this.t * 0.4)), Math.min(w, h) * 0.22f, ORB_PINK);

            g2.dispose();
        }

        private void drawOrb(Graphics2D g2, float cx, float cy, float radius, Color color) {
            if (radius <= 0) return;
            Color edge = new Color(color.getRed(), color.getGreen(), color.getBlue(), 0);
            Color center = new Color(color.getRed(), color.getGreen(), color.getBlue(), 85);
            RadialGradientPaint paint = new RadialGradientPaint(
                    new Point(Math.round(cx), Math.round(cy)), radius,
                    new float[]{0f, 1f},
                    new Color[]{center, edge});
            g2.setPaint(paint);
            g2.fillOval(Math.round(cx - radius), Math.round(cy - radius), Math.round(radius * 2), Math.round(radius * 2));
        }
    }

    private static class GlassCard extends JPanel {
        private static final int ARC = 28;

        GlassCard() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            int w = getWidth(), h = getHeight();
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            for (int i = 10; i >= 1; i--) {
                g2.setColor(new Color(0, 0, 0, 5));
                g2.fill(new RoundRectangle2D.Float(i, i + 5, w - i * 2, h - i * 2, ARC, ARC));
            }

            g2.setPaint(new GradientPaint(0, 0, CARD_TOP, 0, h, CARD_BOTTOM));
            g2.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, ARC, ARC));

            g2.setStroke(new BasicStroke(1.2f));
            g2.setColor(CARD_BORDER);
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 2, h - 2, ARC, ARC));

            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class FocusFieldPanel extends JPanel {
        private static final int ARC = 14;
        private boolean focused = false;

        FocusFieldPanel() {
            setOpaque(false);
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        }

        void setFocused(boolean focused) {
            this.focused = focused;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            int w = getWidth(), h = getHeight();
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (this.focused) {
                g2.setColor(new Color(FIELD_FOCUS.getRed(), FIELD_FOCUS.getGreen(), FIELD_FOCUS.getBlue(), 55));
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, ARC + 6, ARC + 6));
            }

            g2.setColor(FIELD_BG);
            g2.fill(new RoundRectangle2D.Float(3, 3, w - 6, h - 6, ARC, ARC));

            g2.setStroke(new BasicStroke(this.focused ? 1.8f : 1f));
            g2.setColor(this.focused ? FIELD_FOCUS : FIELD_BORDER);
            g2.draw(new RoundRectangle2D.Float(3, 3, w - 7, h - 7, ARC, ARC));

            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class GradientButton extends JButton {
        GradientButton(String text) {
            super(text);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            int w = getWidth(), h = getHeight();
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color base = getBackground();
            Color light = base.brighter();
            Color dark = base.darker();

            float alpha = isEnabled() ? 1f : 0.75f;
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2.setPaint(new GradientPaint(0, 0, light, w, h, dark));
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 14, 14));

            if (isEnabled() && getModel().isRollover()) {
                g2.setColor(new Color(255, 255, 255, 25));
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 14, 14));
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }
}