package com.myapp.print.jasper;

import com.myapp.db.ConnexionSQLite;
import com.myapp.util.SocieteConfig;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperPrintManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.view.JasperViewer;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Remplace StockImpression.java (impression manuelle via Graphics2D).
 *
 * ★ Même logique métier que l'ancienne classe :
 *   - même requête SQL "SELECT id, designation, stock FROM Articles ORDER BY id"
 *   - même repli du logo (src/images/logo.png ou images/logo.png) si aucun logo n'est fourni
 *   - même message "Aucun article trouvé dans le stock à imprimer." si la table est vide
 *   - même date d'édition au format dd/MM/yyyy HH:mm:ss
 *
 * La mise en page (logo, entête société, titre, tableau, pagination) est désormais gérée
 * par le template Stock.jrxml au lieu d'être dessinée manuellement.
 */
public class StockReportService {

    private static final String TEMPLATE_PATH = "/reports/Stock.jrxml";

    // =========================================================================
    // CHARGEMENT DES DONNÉES — identique à StockImpression.chargerDonneesStock()
    // =========================================================================
    public static List<ArticleStockLigne> chargerArticles() {
        List<ArticleStockLigne> articles = new ArrayList<>();
        String query = "SELECT id, designation, stock FROM Articles ORDER BY id";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(query);
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String designation = rs.getString("designation");
                int stock = rs.getInt("stock");
                articles.add(new ArticleStockLigne(id, designation, stock));
            }
            System.out.println("✅ Données de stock chargées: " + articles.size() + " articles");
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors du chargement des données de stock: " + e.getMessage());
            e.printStackTrace();
        }
        return articles;
    }

    // =========================================================================
    // LOGO — copie fidèle de l'ancien StockImpression.chargerLogo(), adaptée
    // pour fournir un InputStream (attendu par le champ image du .jrxml)
    // =========================================================================
    public static InputStream chargerLogoStream() {
        try {
            String[] paths = {"src/images/logo.png", "images/logo.png"};
            for (String p : paths) {
                File f = new File(p);
                if (f.exists()) {
                    return new FileInputStream(f);
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Logo non trouvé: " + e.getMessage());
        }
        return null;
    }

    // ✅ Permet, comme avant, de passer un ImageIcon déjà chargé par l'appelant (ex: BLUI)
    private static InputStream logoIconVersStream(ImageIcon logoIcon) {
        if (logoIcon == null) {
            return chargerLogoStream();
        }
        try {
            java.awt.image.BufferedImage bImg = new java.awt.image.BufferedImage(
                logoIcon.getIconWidth(), logoIcon.getIconHeight(),
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics g = bImg.createGraphics();
            logoIcon.paintIcon(null, g, 0, 0);
            g.dispose();

            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(bImg, "png", baos);
            return new java.io.ByteArrayInputStream(baos.toByteArray());
        } catch (Exception e) {
            System.err.println("⚠️ Conversion logo échouée, repli sur fichier: " + e.getMessage());
            return chargerLogoStream();
        }
    }

    // =========================================================================
    // REMPLISSAGE DU RAPPORT
    // =========================================================================
    public static JasperPrint remplirRapport(List<ArticleStockLigne> articles, InputStream logoStream) throws Exception {
        String dateImpression = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());

        Map<String, Object> params = new HashMap<>();
        params.put("dateImpression", dateImpression);
        params.put("logoImage", logoStream);

        params.put("societeNom", SocieteConfig.getNom());
        params.put("societeActivite", SocieteConfig.getActivite());
        params.put("societeAdresse", SocieteConfig.getAdresse());
        params.put("societeTel", SocieteConfig.getTelephone());
        params.put("societeEmail", SocieteConfig.getEmail());
        params.put("societeMF", SocieteConfig.getMatriculeFiscale());

        JasperReport rapport = compilerOuChargerTemplate();

        return JasperFillManager.fillReport(rapport, params, new JRBeanCollectionDataSource(articles));
    }

    private static JasperReport compilerOuChargerTemplate() throws Exception {
        InputStream jrxml = StockReportService.class.getResourceAsStream(TEMPLATE_PATH);
        if (jrxml == null) {
            throw new IllegalStateException("Template introuvable sur le classpath: " + TEMPLATE_PATH);
        }
        return JasperCompileManager.compileReport(jrxml);
    }

    public static void afficherApercu(JasperPrint print) {
        JasperViewer.viewReport(print, false);
    }

    public static void imprimer(JasperPrint print) throws Exception {
        JasperPrintManager.printReport(print, true);
    }

    public static void exporterPDF(JasperPrint print, String cheminFichier) throws Exception {
        JasperExportManager.exportReportToPdfFile(print, cheminFichier);
    }

    // =========================================================================
    // MÉTHODE "TOUT-EN-UN" — équivalent direct de l'ancien
    // new StockImpression(logoIcon).imprimer()
    // =========================================================================
    public static void imprimerEtatStock(ImageIcon logoIcon) throws Exception {
        List<ArticleStockLigne> articles = chargerArticles();

        if (articles.isEmpty()) {
            JOptionPane.showMessageDialog(null,
                "Aucun article trouvé dans le stock à imprimer.",
                "Stock vide",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        System.out.println("🖨️ Démarrage de l'impression de l'état du stock...");
        System.out.println("📊 Nombre d'articles: " + articles.size());

        InputStream logoStream = logoIconVersStream(logoIcon);
        JasperPrint print = remplirRapport(articles, logoStream);

        // ★ CORRECTIF : JasperPrintManager.printReport(print, true) envoie directement
        // à l'imprimante par défaut SANS ouvrir la moindre fenêtre — contrairement à
        // l'ancien job.printDialog() qui affichait au moins le sélecteur d'imprimante.
        // Résultat : l'utilisateur avait l'impression que rien ne se passait.
        // On affiche donc désormais un APERÇU (fenêtre JasperViewer) ; l'utilisateur
        // peut ensuite cliquer sur l'icône imprimante DANS cette fenêtre pour imprimer
        // réellement (ou l'icône export PDF, etc.).
        afficherApercu(print);

        System.out.println("✅ Aperçu de l'état du stock affiché");
    }
}