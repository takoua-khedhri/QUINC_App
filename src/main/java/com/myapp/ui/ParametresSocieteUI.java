package com.myapp.ui;

import com.myapp.util.SocieteConfig;

import javax.swing.*;
import java.awt.*;

/**
 * Écran de modification des informations de la société.
 * Toute modification enregistrée ici se répercute automatiquement sur tous
 * les documents imprimés (Facture, BL, Bon de Sortie, Bon de Commande,
 * Devis, Facture Fournisseur, Ticket) — voir SocieteConfig.java.
 */
public class ParametresSocieteUI extends JFrame {

    private JTextField txtNom;
    private JTextField txtActivite;
    private JTextField txtAdresse;
    private JTextField txtTelephone;
    private JTextField txtEmail;
    private JTextField txtMatricule;

    private static final Color BG_DARK   = new Color(30, 41, 59);
    private static final Color BG_PANEL  = new Color(255, 255, 255);
    private static final Color ACCENT    = new Color(41, 128, 185);

    public ParametresSocieteUI() {
        this.setTitle("Paramètres de la société");
        // ★ CORRECTIF : affichage en plein écran ("page"), cohérent avec
        // tous les autres écrans de l'application, au lieu d'une petite
        // fenêtre flottante centrée qui donnait une impression d'écran
        // "isolé" du reste de l'application.
        this.setMinimumSize(new Dimension(600, 560));
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setLayout(new BorderLayout());
        this.getContentPane().setBackground(BG_DARK);

        this.add(creerEntete(), BorderLayout.NORTH);
        this.add(creerFormulaire(), BorderLayout.CENTER);
        this.add(creerPiedDePage(), BorderLayout.SOUTH);

        chargerValeursActuelles();
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    private JPanel creerEntete() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_DARK);
        header.setBorder(BorderFactory.createEmptyBorder(20, 25, 15, 25));

        JLabel titre = new JLabel("Paramètres de la société");
        titre.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titre.setForeground(Color.WHITE);

        JLabel sousTitre = new JLabel("Ces informations apparaissent sur tous vos documents imprimés");
        sousTitre.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sousTitre.setForeground(new Color(180, 190, 200));

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        textPanel.setOpaque(false);
        textPanel.add(titre);
        textPanel.add(sousTitre);

        header.add(textPanel, BorderLayout.WEST);
        return header;
    }

    private JPanel creerFormulaire() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG_DARK);
        wrapper.setBorder(BorderFactory.createEmptyBorder(0, 25, 0, 25));

        JPanel form = new JPanel();
        form.setBackground(BG_PANEL);
        form.setLayout(new GridBagLayout());
        form.setBorder(BorderFactory.createCompoundBorder(
                new RoundedComponents.RoundedLineBorder(new Color(220, 224, 230), 1, 10),
                BorderFactory.createEmptyBorder(25, 25, 25, 25)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 0, 8, 0);
        gbc.gridx = 0;
        gbc.weightx = 1;

        gbc.gridy = 0; form.add(creerLabel("Nom de la société"), gbc);
        gbc.gridy = 1; txtNom = creerChamp(); form.add(txtNom, gbc);

        gbc.gridy = 2; form.add(creerLabel("Activité"), gbc);
        gbc.gridy = 3; txtActivite = creerChamp(); form.add(txtActivite, gbc);

        gbc.gridy = 4; form.add(creerLabel("Adresse"), gbc);
        gbc.gridy = 5; txtAdresse = creerChamp(); form.add(txtAdresse, gbc);

        gbc.gridy = 6; form.add(creerLabel("Téléphone"), gbc);
        gbc.gridy = 7; txtTelephone = creerChamp(); form.add(txtTelephone, gbc);

        gbc.gridy = 8; form.add(creerLabel("Email"), gbc);
        gbc.gridy = 9; txtEmail = creerChamp(); form.add(txtEmail, gbc);

        gbc.gridy = 10; form.add(creerLabel("Matricule Fiscal"), gbc);
        gbc.gridy = 11; txtMatricule = creerChamp(); form.add(txtMatricule, gbc);

        wrapper.add(form, BorderLayout.CENTER);
        return wrapper;
    }

    private JLabel creerLabel(String texte) {
        JLabel label = new JLabel(texte);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(new Color(70, 80, 95));
        return label;
    }

    private JTextField creerChamp() {
        JTextField champ = new JTextField();
        champ.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        champ.setPreferredSize(new Dimension(100, 32));
        champ.setBorder(BorderFactory.createCompoundBorder(
                new RoundedComponents.RoundedLineBorder(new Color(200, 205, 215), 1, 10),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        return champ;
    }

    private JPanel creerPiedDePage() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        footer.setBackground(BG_DARK);
        footer.setBorder(BorderFactory.createEmptyBorder(10, 25, 20, 25));

        JButton btnAnnuler = new RoundedComponents.RoundedActionButton(
            "Retour au tableau de bord", new Color(90, 100, 115),
            RoundedComponents.RoundedActionButton.darken(new Color(90, 100, 115), 0.85F));
        btnAnnuler.setPreferredSize(new Dimension(200, 36));
        btnAnnuler.addActionListener(e -> {
            this.dispose();
            new AdminDashboard().setVisible(true);
        });

        JButton btnEnregistrer = new RoundedComponents.RoundedActionButton(
            "Enregistrer", ACCENT, RoundedComponents.RoundedActionButton.darken(ACCENT, 0.85F));
        btnEnregistrer.setPreferredSize(new Dimension(190, 36));
        btnEnregistrer.addActionListener(e -> enregistrer());

        footer.add(btnAnnuler);
        footer.add(btnEnregistrer);
        return footer;
    }

    // (styleBouton retirée — les boutons sont désormais créés directement
    // en RoundedActionButton, voir creerPiedDePage)


    private void chargerValeursActuelles() {
        txtNom.setText(SocieteConfig.getNom());
        txtActivite.setText(SocieteConfig.getActivite());
        txtAdresse.setText(SocieteConfig.getAdresse());
        txtTelephone.setText(SocieteConfig.getTelephone());
        txtEmail.setText(SocieteConfig.getEmail());
        txtMatricule.setText(SocieteConfig.getMatriculeFiscale());
    }

    private void enregistrer() {
        if (txtNom.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Le nom de la société ne peut pas être vide.",
                    "Champ requis", JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean ok = SocieteConfig.enregistrer(
                txtNom.getText().trim(),
                txtActivite.getText().trim(),
                txtAdresse.getText().trim(),
                txtTelephone.getText().trim(),
                txtEmail.getText().trim(),
                txtMatricule.getText().trim()
        );

        if (ok) {
            JOptionPane.showMessageDialog(this,
                    "✅ Informations enregistrées.\nElles apparaîtront désormais sur tous les nouveaux documents imprimés.",
                    "Succès", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this,
                    "❌ Erreur lors de l'enregistrement. Vérifiez la console pour le détail.",
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }
}