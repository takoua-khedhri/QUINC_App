package com.myapp.ui;

import com.myapp.logic.RetourManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Consultation de l'historique de tous les retours enregistrés.
 * ★ Nouveau module — complète BonRetourUI.
 */
public class ListeRetours extends JFrame {

    private static final Color BG_DARK = new Color(30, 41, 59);

    public ListeRetours() {
        this.setTitle("Liste des Retours");
        // ★ CORRECTIF : affichage en plein écran ("page"), cohérent avec
        // les autres écrans de l'application, au lieu d'une petite fenêtre
        // flottante centrée.
        this.setMinimumSize(new Dimension(800, 550));
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setLayout(new BorderLayout());
        this.getContentPane().setBackground(BG_DARK);

        JLabel titre = new JLabel("  Historique des Retours");
        titre.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titre.setForeground(Color.WHITE);
        titre.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));
        this.add(titre, BorderLayout.NORTH);

        String[] colonnes = {"N° Retour", "Document d'origine", "Type", "Date", "Motif"};
        DefaultTableModel model = new DefaultTableModel(colonnes, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        List<Object[]> retours = RetourManager.listerRetours();
        for (Object[] retour : retours) {
            model.addRow(retour);
        }

        JTable table = new JTable(model);
        table.setRowHeight(26);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        this.add(new JScrollPane(table), BorderLayout.CENTER);

        JButton btnRetour = new JButton("← Tableau de bord");
        btnRetour.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRetour.setBackground(new Color(90, 100, 115));
        btnRetour.setForeground(Color.WHITE);
        btnRetour.setFocusPainted(false);
        btnRetour.addActionListener(e -> { this.dispose(); new AdminDashboard().setVisible(true); });

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        footer.setBackground(BG_DARK);
        footer.add(btnRetour);
        this.add(footer, BorderLayout.SOUTH);

        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
    }
}