package com.myapp.ui;

import com.myapp.logic.RetourManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Écran de saisie d'un retour de marchandise client.
 * ★ Nouveau module — n'existait pas dans ce projet. Principe repris de
 * l'application Bibliothèque : recherche d'un document existant (Facture,
 * BL ou Ticket), chargement de ses lignes, indication de la quantité
 * réellement retournée par article, puis validation qui recrédite
 * automatiquement le stock. Le document d'origine n'est jamais modifié.
 */
public class BonRetourUI extends JFrame {

    private static final Color BG_DARK  = new Color(30, 41, 59);
    private static final Color ACCENT   = new Color(41, 128, 185);

    private JTextField txtNumeroDocument;
    private JPopupMenu popupSuggestions;
    private com.toedter.calendar.JDateChooser dateChooserRetour;
    private JTextField txtMotif;
    private JLabel lblNumeroRetour;
    private JLabel lblTypeDocumentTrouve;

    private DefaultTableModel model;
    private JTable table;

    private List<RetourManager.LigneDocument> lignesChargees;
    /** ★ numéro du document réellement chargé (le champ texte peut être modifié ensuite). */
    private String numeroDocumentCharge;

    public BonRetourUI() {
        this.setTitle("Bon de Retour");
        // ★ CORRECTIF : affichage en plein écran ("page"), cohérent avec
        // les autres écrans de l'application (FactureUI, BLUI...), au lieu
        // d'une petite fenêtre flottante centrée.
        this.setMinimumSize(new Dimension(900, 640));
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setLayout(new BorderLayout());
        this.getContentPane().setBackground(BG_DARK);

        this.add(creerEntete(), BorderLayout.NORTH);
        this.add(creerCentre(), BorderLayout.CENTER);
        this.add(creerPiedDePage(), BorderLayout.SOUTH);

        this.reinitialiser();
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    // ==================== EN-TÊTE ====================
    private JPanel creerEntete() {
        JPanel header = new JPanel(new GridBagLayout());
        header.setBackground(BG_DARK);
        header.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        header.add(creerLabel("Bon de Retour N° :"), gbc);
        gbc.gridx = 1;
        lblNumeroRetour = new JLabel("...");
        lblNumeroRetour.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblNumeroRetour.setForeground(Color.WHITE);
        header.add(lblNumeroRetour, gbc);

        gbc.gridx = 2;
        header.add(creerLabel("Date :"), gbc);
        gbc.gridx = 3;
        dateChooserRetour = new com.toedter.calendar.JDateChooser();
        dateChooserRetour.setDate(new java.util.Date());
        dateChooserRetour.setDateFormatString("yyyy-MM-dd");
        dateChooserRetour.setPreferredSize(new Dimension(120, 27));
        header.add(dateChooserRetour, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        header.add(creerLabel("N° Document (Facture/BL/Ticket) :"), gbc);
        gbc.gridx = 1;
        txtNumeroDocument = new JTextField(18);
        styleChamp(txtNumeroDocument);
        header.add(txtNumeroDocument, gbc);
        installerAutocompletion();

        gbc.gridx = 2;
        JButton btnRechercher = new JButton("Rechercher");
        styleBouton(btnRechercher, ACCENT);
        btnRechercher.addActionListener(e -> rechercherDocument());
        header.add(btnRechercher, gbc);

        gbc.gridx = 3;
        lblTypeDocumentTrouve = new JLabel("");
        lblTypeDocumentTrouve.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblTypeDocumentTrouve.setForeground(new Color(150, 200, 150));
        header.add(lblTypeDocumentTrouve, gbc);

        return header;
    }

    private void installerAutocompletion() {
        popupSuggestions = new JPopupMenu();
        popupSuggestions.setFocusable(false);

        txtNumeroDocument.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                String texte = txtNumeroDocument.getText();
                if (texte.length() < 2) { popupSuggestions.setVisible(false); return; }

                List<String> suggestions = RetourManager.rechercherNumerosDocuments(texte);
                if (suggestions.isEmpty()) { popupSuggestions.setVisible(false); return; }

                popupSuggestions.removeAll();
                for (String s : suggestions) {
                    JMenuItem item = new JMenuItem(s);
                    item.addActionListener(ev -> {
                        txtNumeroDocument.setText(s);
                        popupSuggestions.setVisible(false);
                        rechercherDocument();
                    });
                    popupSuggestions.add(item);
                }
                popupSuggestions.show(txtNumeroDocument, 0, txtNumeroDocument.getHeight());
            }
        });
    }

    private JLabel creerLabel(String texte) {
        JLabel label = new JLabel(texte);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(Color.WHITE);
        return label;
    }

    private void styleChamp(JTextField champ) {
        champ.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        champ.setBackground(new Color(60, 80, 100));
        champ.setForeground(Color.WHITE);
        champ.setCaretColor(Color.WHITE);
        champ.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
    }

    private void styleBouton(JButton bouton, Color couleur) {
        bouton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        bouton.setBackground(couleur);
        bouton.setForeground(Color.WHITE);
        bouton.setFocusPainted(false);
        bouton.setBorderPainted(false);
        bouton.setOpaque(true);
        bouton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        bouton.setPreferredSize(new Dimension(140, 32));
    }

    // ==================== CENTRE (tableau) ====================
    private JPanel creerCentre() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG_DARK);
        wrapper.setBorder(BorderFactory.createEmptyBorder(0, 20, 10, 20));

        String[] colonnes = {"Désignation", "Qté d'origine", "Qté à retourner", "PU"};
        model = new DefaultTableModel(colonnes, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 2; // seule "Qté à retourner" est modifiable
            }
            @Override
            public Class<?> getColumnClass(int col) {
                if (col == 1 || col == 2) return Integer.class;
                return String.class;
            }
        };
        table = new JTable(model);
        table.setRowHeight(26);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        wrapper.add(new JScrollPane(table), BorderLayout.CENTER);
        return wrapper;
    }

    // ==================== PIED DE PAGE ====================
    private JPanel creerPiedDePage() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(BG_DARK);
        footer.setBorder(BorderFactory.createEmptyBorder(5, 20, 15, 20));

        JPanel motifPanel = new JPanel(new BorderLayout(10, 0));
        motifPanel.setOpaque(false);
        motifPanel.add(creerLabel("Motif du retour (obligatoire) :"), BorderLayout.WEST);
        txtMotif = new JTextField();
        styleChamp(txtMotif);
        motifPanel.add(txtMotif, BorderLayout.CENTER);
        footer.add(motifPanel, BorderLayout.NORTH);

        JPanel boutons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        boutons.setOpaque(false);

        JButton btnRetour = new JButton("← Tableau de bord");
        styleBouton(btnRetour, new Color(90, 100, 115));
        // ★ CORRECTIF : "← Tableau de bord" est plus long que les autres
        // libellés de boutons (140px suffisait pour "Rechercher" mais
        // tronquait ce texte-ci en "← Tableau d...").
        btnRetour.setPreferredSize(new Dimension(190, 32));
        btnRetour.addActionListener(e -> { this.dispose(); new AdminDashboard().setVisible(true); });

        JButton btnReinitialiser = new JButton("Réinitialiser");
        styleBouton(btnReinitialiser, new Color(90, 100, 115));
        btnReinitialiser.addActionListener(e -> reinitialiser());

        JButton btnValider = new JButton("Valider le Retour");
        styleBouton(btnValider, ACCENT);
        btnValider.setPreferredSize(new Dimension(180, 34));
        btnValider.addActionListener(e -> validerRetour());

        boutons.add(btnRetour);
        boutons.add(btnReinitialiser);
        boutons.add(btnValider);
        footer.add(boutons, BorderLayout.SOUTH);

        return footer;
    }

    // ==================== LOGIQUE ====================
    private void rechercherDocument() {
        String numero = txtNumeroDocument.getText().trim();
        if (numero.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Entrez un numéro de document.");
            return;
        }

        String type = RetourManager.getTypeDocument(numero);
        if (type == null || type.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Aucun document trouvé avec ce numéro.",
                    "Introuvable", JOptionPane.WARNING_MESSAGE);
            model.setRowCount(0);
            lblTypeDocumentTrouve.setText("");
            return;
        }

        lignesChargees = RetourManager.chargerLignesDocument(numero);
        numeroDocumentCharge = numero;
        model.setRowCount(0);
        for (RetourManager.LigneDocument ligne : lignesChargees) {
            model.addRow(new Object[]{
                ligne.designation, ligne.quantiteOrigine, 0,
                String.format("%.3f", ligne.prixUnitaire)
            });
        }
        lblTypeDocumentTrouve.setText("Type : " + type + " — " + lignesChargees.size() + " article(s)");
    }

    private void validerRetour() {
        if (lignesChargees == null || lignesChargees.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Recherchez d'abord un document valide.");
            return;
        }
        if (txtMotif.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Le motif du retour est obligatoire.",
                    "Champ requis", JOptionPane.WARNING_MESSAGE);
            return;
        }

        java.util.List<RetourManager.LigneRetour> lignesRetour = new java.util.ArrayList<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            int qteOrigine = (Integer) model.getValueAt(i, 1);
            Object qteRetObj = model.getValueAt(i, 2);
            int qteRetour = (qteRetObj instanceof Integer) ? (Integer) qteRetObj : 0;

            if (qteRetour < 0 || qteRetour > qteOrigine) {
                JOptionPane.showMessageDialog(this,
                        "Ligne " + (i + 1) + " : la quantité à retourner ne peut pas dépasser la quantité d'origine (" + qteOrigine + ").",
                        "Quantité invalide", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (qteRetour > 0) {
                String designation = (String) model.getValueAt(i, 0);
                double pu = Double.parseDouble(((String) model.getValueAt(i, 3)).replace(",", "."));
                lignesRetour.add(new RetourManager.LigneRetour(designation, qteRetour, pu));
            }
        }

        if (lignesRetour.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Indiquez au moins une quantité à retourner.",
                    "Aucune quantité", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String numeroRetour = RetourManager.genererNumeroRetour();
        // ★ CORRECTIF : date vide -> NullPointerException ; numéro relu dans le
        // champ texte -> retour rattaché à un autre document si modifié.
        java.util.Date dateRetour = dateChooserRetour.getDate();
        if (dateRetour == null) dateRetour = new java.util.Date();
        String date = new java.text.SimpleDateFormat("yyyy-MM-dd").format(dateRetour);
        String numeroDocument = numeroDocumentCharge != null ? numeroDocumentCharge : txtNumeroDocument.getText().trim();

        boolean ok = RetourManager.validerRetour(numeroRetour, numeroDocument, date,
                txtMotif.getText().trim(), lignesRetour);

        if (ok) {
            JOptionPane.showMessageDialog(this,
                    "✅ Retour " + numeroRetour + " enregistré — stock recrédité pour "
                            + lignesRetour.size() + " article(s).");
            reinitialiser();
        } else {
            JOptionPane.showMessageDialog(this,
                    "❌ Erreur lors de l'enregistrement du retour.",
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void reinitialiser() {
        model.setRowCount(0);
        txtNumeroDocument.setText("");
        txtMotif.setText("");
        lblTypeDocumentTrouve.setText("");
        dateChooserRetour.setDate(new java.util.Date());
        lignesChargees = null;
        lblNumeroRetour.setText(RetourManager.genererNumeroRetour());
    }
}