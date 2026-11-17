package com.myapp.ui;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Composants visuels arrondis partagés par tous les écrans de l'application
 * (Dashboard, GestionStock, écrans clients/fournisseurs/articles...).
 *
 * ★ Créé pour la modernisation visuelle de Chaa Elect : remplace les
 * bordures rectangulaires (BorderFactory.createLineBorder) et les boutons
 * à angles vifs par des équivalents à coins arrondis, de façon cohérente
 * sur tous les écrans, sans dupliquer ce code dans chaque fichier.
 */
public class RoundedComponents {

    /** Bordure à coins arrondis, remplace BorderFactory.createLineBorder(color, thickness). */
    public static class RoundedLineBorder extends AbstractBorder {
        private final Color color;
        private final int thickness;
        private final int radius;

        public RoundedLineBorder(Color color, int thickness, int radius) {
            this.color = color;
            this.thickness = thickness;
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(this.color);
            g2.setStroke(new BasicStroke(this.thickness));
            g2.drawRoundRect(x + this.thickness / 2, y + this.thickness / 2,
                    w - this.thickness, h - this.thickness, this.radius, this.radius);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            int i = this.thickness + 4;
            return new Insets(i, i, i, i);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            int i = this.thickness + 4;
            insets.set(i, i, i, i);
            return insets;
        }
    }

    /** Bouton à coins arrondis avec effet de survol, remplace un JButton classique. */
    public static class RoundedActionButton extends JButton {
        private final Color baseColor;
        private final Color hoverColor;
        private boolean hovered = false;

        public RoundedActionButton(String text, Color baseColor, Color hoverColor) {
            super(text);
            this.baseColor = baseColor;
            this.hoverColor = hoverColor;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, 12));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(9, 14, 9, 14));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
            });
        }

        public static Color darken(Color c, float factor) {
            return new Color(
                    Math.max((int) (c.getRed() * factor), 0),
                    Math.max((int) (c.getGreen() * factor), 0),
                    Math.max((int) (c.getBlue() * factor), 0));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(this.hovered ? this.hoverColor : this.baseColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Carte blanche à coins arrondis avec ombre douce, pour remplacer un JPanel simple. */
    public static class RoundedCardPanel extends JPanel {
        private final int radius;

        public RoundedCardPanel(int radius) {
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            int w = getWidth(), h = getHeight();
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            for (int i = 6; i > 0; i--) {
                g2.setColor(new Color(0, 0, 0, 6));
                g2.fillRoundRect(i, i + 2, w - i, h - i, this.radius, this.radius);
            }
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, w - 6, h - 6, this.radius, this.radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}