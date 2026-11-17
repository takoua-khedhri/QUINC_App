package com.myapp.util;

import com.myapp.db.ConnexionSQLite;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Source unique des informations de la société, utilisées partout où elles
 * apparaissent : en-tête de chaque document imprimé (Facture, BL, Bon de
 * Sortie, Bon de Commande, Devis, Facture Fournisseur, Ticket) ET les écrans
 * de l'application.
 *
 * ★ Avant cette classe, ces informations étaient codées en dur, en double,
 * dans chacune des ~14 classes d'impression et d'interface. Modifier
 * l'adresse ou le téléphone de la société obligeait à changer manuellement
 * chaque fichier. Désormais, une seule modification ici (via l'écran
 * "Paramètres Société" du tableau de bord) se répercute automatiquement
 * partout.
 *
 * Une seule ligne existe dans la table ParametresSociete (id=1). Si elle
 * n'existe pas encore (première utilisation après mise à jour), elle est
 * créée automatiquement avec les valeurs qui étaient jusqu'ici codées en
 * dur — aucune donnée n'est donc perdue ni modifiée au premier lancement.
 */
public class SocieteConfig {

    private static boolean tableVerifiee = false;

    private static synchronized void assurerTableEtValeursParDefaut() {
        if (tableVerifiee) return;
        try (Connection conn = ConnexionSQLite.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS ParametresSociete (" +
                "id INTEGER PRIMARY KEY CHECK (id = 1), " +
                "nom TEXT NOT NULL DEFAULT '', " +
                "activite TEXT NOT NULL DEFAULT '', " +
                "adresse TEXT NOT NULL DEFAULT '', " +
                "telephone TEXT NOT NULL DEFAULT '', " +
                "email TEXT NOT NULL DEFAULT '', " +
                "matricule_fiscal TEXT NOT NULL DEFAULT ''" +
                ")"
            );

            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM ParametresSociete WHERE id = 1")) {
                rs.next();
                if (rs.getInt(1) == 0) {
                    // Valeurs par défaut = celles qui étaient codées en dur
                    // partout auparavant, pour ne rien changer visuellement
                    // au premier lancement après mise à jour.
                    try (PreparedStatement pst = conn.prepareStatement(
                            "INSERT INTO ParametresSociete (id, nom, activite, adresse, telephone, email, matricule_fiscal) " +
                            "VALUES (1, ?, ?, ?, ?, ?, ?)")) {
                        pst.setString(1, "CHAA ELECT");
                        pst.setString(2, "VTE EN GROS MATERIEL ELECTRIQUE");
                        pst.setString(3, "08, RUE 42500, Ezzahrouni, El Hrairia, Tunis, 2051");
                        pst.setString(4, "94 226 752");
                        pst.setString(5, "chaa.elec@outlook.fr");
                        pst.setString(6, "000/M/A/1981916C");
                        pst.executeUpdate();
                    }
                }
            }
            tableVerifiee = true;
        } catch (SQLException e) {
            System.err.println("Erreur initialisation ParametresSociete: " + e.getMessage());
        }
    }

    private static String lireChamp(String colonne) {
        assurerTableEtValeursParDefaut();
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(
                 "SELECT " + colonne + " FROM ParametresSociete WHERE id = 1")) {
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lecture ParametresSociete." + colonne + ": " + e.getMessage());
        }
        return "";
    }

    public static String getNom()             { return lireChamp("nom"); }
    public static String getActivite()        { return lireChamp("activite"); }
    public static String getAdresse()         { return lireChamp("adresse"); }
    public static String getTelephone()       { return lireChamp("telephone"); }
    public static String getEmail()           { return lireChamp("email"); }
    public static String getMatriculeFiscale(){ return lireChamp("matricule_fiscal"); }

    /**
     * Enregistre les nouvelles informations société. Appelé uniquement
     * depuis ParametresSocieteUI, après validation du formulaire.
     */
    public static boolean enregistrer(String nom, String activite, String adresse,
                                       String telephone, String email, String matriculeFiscal) {
        assurerTableEtValeursParDefaut();
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(
                 "UPDATE ParametresSociete SET nom=?, activite=?, adresse=?, telephone=?, email=?, matricule_fiscal=? WHERE id=1")) {
            pst.setString(1, nom);
            pst.setString(2, activite);
            pst.setString(3, adresse);
            pst.setString(4, telephone);
            pst.setString(5, email);
            pst.setString(6, matriculeFiscal);
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur enregistrement ParametresSociete: " + e.getMessage());
            return false;
        }
    }
}