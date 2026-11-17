package com.myapp.logic;

import com.myapp.db.ConnexionSQLite;
import com.myapp.db.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Gère les retours de marchandise par un client.
 *
 * ★ Nouveau module (n'existait pas dans ce projet). Principe repris de
 * l'application Bibliothèque : on recherche un document existant (Facture,
 * BL ou Ticket — tous stockés dans la même table Factures, différenciés
 * par la colonne "type"), on charge ses lignes, l'utilisateur indique la
 * quantité réellement retournée par article, et la validation :
 *   1) enregistre le retour dans une table dédiée (jamais créée jusqu'ici,
 *      créée automatiquement au premier lancement — même principe que
 *      SocieteConfig)
 *   2) recrédite le stock via DatabaseManager.augmenterStock() (méthode
 *      déjà existante et déjà utilisée ailleurs dans le projet)
 * Le document d'origine n'est jamais modifié — seul un nouvel
 * enregistrement de retour est créé, consultable séparément.
 */
public class RetourManager {

    public static class LigneDocument {
        public final String designation;
        public final int quantiteOrigine;
        public final double prixUnitaire;

        public LigneDocument(String designation, int quantiteOrigine, double prixUnitaire) {
            this.designation = designation;
            this.quantiteOrigine = quantiteOrigine;
            this.prixUnitaire = prixUnitaire;
        }
    }

    /**
     * Représente une ligne effectivement retournée (quantité choisie par
     * l'utilisateur, distincte de la quantité d'origine du document).
     */
    public static class LigneRetour {
        public final String designation;
        public final int quantiteRetournee;
        public final double prixUnitaire;

        public LigneRetour(String designation, int quantiteRetournee, double prixUnitaire) {
            this.designation = designation;
            this.quantiteRetournee = quantiteRetournee;
            this.prixUnitaire = prixUnitaire;
        }
    }

    private static boolean tablesVerifiees = false;

    private static synchronized void assurerTables() {
        if (tablesVerifiees) return;
        try (Connection conn = ConnexionSQLite.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS Retours (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "numero TEXT NOT NULL UNIQUE, " +
                "document_origine_numero TEXT NOT NULL, " +
                "document_origine_type TEXT NOT NULL, " +
                "date TEXT NOT NULL, " +
                "motif TEXT NOT NULL, " +
                "date_creation TEXT DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS RetourDetails (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "retour_id INTEGER NOT NULL, " +
                "article_designation TEXT NOT NULL, " +
                "quantite_retournee INTEGER NOT NULL, " +
                "prix_unitaire REAL NOT NULL, " +
                "FOREIGN KEY (retour_id) REFERENCES Retours(id) ON DELETE CASCADE" +
                ")"
            );
            tablesVerifiees = true;
        } catch (SQLException e) {
            System.err.println("Erreur initialisation tables Retours: " + e.getMessage());
        }
    }

    /**
     * Recherche des documents (Factures/BL/Tickets) dont le numéro
     * contient le texte tapé — utilisé pour l'autocomplétion.
     */
    public static List<String> rechercherNumerosDocuments(String texte) {
        assurerTables();
        List<String> resultats = new ArrayList<>();
        if (texte == null || texte.trim().length() < 2) return resultats;

        String sql = "SELECT numero FROM Factures WHERE numero LIKE ? AND numero NOT LIKE 'BL/FACT-%' " +
                     "ORDER BY date_creation DESC LIMIT 15";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, "%" + texte.trim() + "%");
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) resultats.add(rs.getString("numero"));
            }
        } catch (SQLException e) {
            System.err.println("Erreur recherche documents: " + e.getMessage());
        }
        return resultats;
    }

    /**
     * Charge les lignes d'un document (Facture/BL/Ticket) à partir de son
     * numéro, pour affichage dans le tableau de retour.
     */
    public static List<LigneDocument> chargerLignesDocument(String numeroDocument) {
        assurerTables();
        List<LigneDocument> lignes = new ArrayList<>();

        // ★ CORRECTIF : "BL/FACT-…" est une simple copie de la facture (elle
        // n'a jamais sorti de stock) : la retourner recréditait deux fois.
        if (numeroDocument == null || numeroDocument.startsWith("BL/FACT-")) return lignes;

        int factureId = DatabaseManager.getFactureId(numeroDocument);
        if (factureId <= 0) return lignes;

        // ★ CORRECTIF : on renvoie la quantité ENCORE retournable (origine −
        // retours déjà enregistrés sur ce document), regroupée par article ;
        // auparavant on pouvait retourner indéfiniment la même marchandise.
        String sql =
            "SELECT d.article_designation, d.qte - COALESCE(r.deja, 0) AS restant, d.pu FROM (" +
            "  SELECT article_designation, SUM(quantite) AS qte, " +
            "         SUM(quantite * prix_unitaire) / NULLIF(SUM(quantite), 0) AS pu " +
            "  FROM DetailsFacture WHERE facture_id = ? GROUP BY article_designation) d " +
            "LEFT JOIN (" +
            "  SELECT rd.article_designation, SUM(rd.quantite_retournee) AS deja " +
            "  FROM RetourDetails rd JOIN Retours rt ON rt.id = rd.retour_id " +
            "  WHERE rt.document_origine_numero = ? GROUP BY rd.article_designation) r " +
            "ON r.article_designation = d.article_designation";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, factureId);
            pst.setString(2, numeroDocument);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    int restant = rs.getInt("restant");
                    if (restant <= 0) continue;
                    lignes.add(new LigneDocument(
                        rs.getString("article_designation"),
                        restant,
                        rs.getDouble("pu")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur chargement lignes document: " + e.getMessage());
        }
        return lignes;
    }

    public static String getTypeDocument(String numeroDocument) {
        assurerTables();
        String sql = "SELECT type FROM Factures WHERE numero = ?";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, numeroDocument);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return rs.getString("type");
            }
        } catch (SQLException e) {
            System.err.println("Erreur lecture type document: " + e.getMessage());
        }
        return "";
    }

    /**
     * Génère le prochain numéro de retour, format RET-AAAA-NNNN.
     */
    public static String genererNumeroRetour() {
        assurerTables();
        int annee = java.time.LocalDate.now().getYear();
        String prefix = "RET-" + annee + "-";
        String sql = "SELECT numero FROM Retours WHERE numero LIKE ? ORDER BY id DESC LIMIT 1";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, prefix + "%");
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    String dernier = rs.getString("numero");
                    int n = Integer.parseInt(dernier.substring(prefix.length())) + 1;
                    return prefix + String.format("%04d", n);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur génération numéro retour: " + e.getMessage());
        }
        return prefix + "0001";
    }

    /**
     * Valide le retour : enregistre le retour en base ET recrédite le
     * stock de chaque article retourné. Opération transactionnelle —
     * si une étape échoue, rien n'est enregistré.
     */
    public static boolean validerRetour(String numeroRetour, String numeroDocumentOrigine,
                                         String date, String motif,
                                         List<LigneRetour> lignesRetournees) {
        assurerTables();
        if (lignesRetournees == null || lignesRetournees.isEmpty()) return false;

        String typeDocument = getTypeDocument(numeroDocumentOrigine);

        try (Connection conn = ConnexionSQLite.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int retourId;
                String sqlRetour = "INSERT INTO Retours (numero, document_origine_numero, document_origine_type, date, motif) " +
                                    "VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement pst = conn.prepareStatement(sqlRetour, Statement.RETURN_GENERATED_KEYS)) {
                    pst.setString(1, numeroRetour);
                    pst.setString(2, numeroDocumentOrigine);
                    pst.setString(3, typeDocument);
                    pst.setString(4, date);
                    pst.setString(5, motif);
                    pst.executeUpdate();
                    try (ResultSet rs = pst.getGeneratedKeys()) {
                        if (!rs.next()) throw new SQLException("Échec création du retour");
                        retourId = rs.getInt(1);
                    }
                }

                String sqlDetail = "INSERT INTO RetourDetails (retour_id, article_designation, quantite_retournee, prix_unitaire) " +
                                    "VALUES (?, ?, ?, ?)";
                try (PreparedStatement pst = conn.prepareStatement(sqlDetail)) {
                    for (LigneRetour ligne : lignesRetournees) {
                        pst.setInt(1, retourId);
                        pst.setString(2, ligne.designation);
                        pst.setInt(3, ligne.quantiteRetournee);
                        pst.setDouble(4, ligne.prixUnitaire);
                        pst.addBatch();
                    }
                    pst.executeBatch();
                }

                // ★ CORRECTIF : DatabaseManager.augmenterStock() ouvrait une 2e
                // connexion bloquée par le verrou d'écriture de cette transaction
                // (SQLITE_BUSY avalé) : le stock n'était jamais recrédité. On
                // met à jour le stock sur la MÊME connexion, dans la transaction.
                try (PreparedStatement upd = conn.prepareStatement(
                        "UPDATE Articles SET stock = stock + ? WHERE designation = ?")) {
                    for (LigneRetour ligne : lignesRetournees) {
                        if (ligne.quantiteRetournee > 0) {
                            upd.setInt(1, ligne.quantiteRetournee);
                            upd.setString(2, ligne.designation);
                            if (upd.executeUpdate() == 0) {
                                throw new SQLException("Article introuvable : " + ligne.designation);
                            }
                        }
                    }
                }

                conn.commit();
                return true;

            } catch (SQLException e) {
                conn.rollback();
                System.err.println("Erreur validation retour, annulation: " + e.getMessage());
                return false;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("Erreur connexion validation retour: " + e.getMessage());
            return false;
        }
    }

    /**
     * Liste tous les retours enregistrés, pour l'écran de consultation.
     */
    public static List<Object[]> listerRetours() {
        assurerTables();
        List<Object[]> retours = new ArrayList<>();
        String sql = "SELECT numero, document_origine_numero, document_origine_type, date, motif FROM Retours ORDER BY id DESC";
        try (Connection conn = ConnexionSQLite.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                retours.add(new Object[]{
                    rs.getString("numero"),
                    rs.getString("document_origine_numero"),
                    rs.getString("document_origine_type"),
                    rs.getString("date"),
                    rs.getString("motif")
                });
            }
        } catch (SQLException e) {
            System.err.println("Erreur listage retours: " + e.getMessage());
        }
        return retours;
    }
}