package com.myapp.print.jasper;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.JasperPrintManager;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.view.JasperViewer;
import com.myapp.util.SocieteConfig;

import javax.swing.table.DefaultTableModel;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Remplace FactureImpressionTicket.java (impression manuelle via
 * PrinterJob/Paper de taille dynamique).
 *
 * ★ Même raison de migration que pour l'application Bibliothèque : une
 * taille de papier calculée à la main (rowCount*30+350) n'est pas toujours
 * honorée par tous les pilotes d'impression (notamment "Imprimer en PDF"),
 * ce qui peut couper le bas du ticket. JRParameter.IS_IGNORE_PAGINATION
 * fait s'étendre le contenu sur une longueur illimitée, sans dépendre
 * d'aucun pilote.
 *
 * ★ Corrige aussi un défaut trouvé dans l'ancien code : le montant de
 * chaque ligne était recalculé à la main (quantité × PU HT, colonne 3),
 * ignorant toute remise appliquée sur la ligne. On utilise maintenant le
 * vrai "Total HT" de la ligne (colonne 4, remise déjà incluse).
 */
public class TicketReportService {

    private static final String TEMPLATE_PATH = "/reports/Ticket.jrxml";

    /**
     * Colonnes réelles du modèle (identiques à Facture/BL/Devis dans ce
     * projet) : 0=Code, 1=Désignation, 2=Qté, 3=PU HT, 4=Total HT, 5=TVA,
     * 6=PU TTC, 7=Remise %, 8=Total TTC.
     */
    public static List<ArticleLigne> convertirModel(DefaultTableModel model) {
        List<ArticleLigne> lignes = new ArrayList<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            try {
                String code    = str(model.getValueAt(i, 0));
                String desig   = str(model.getValueAt(i, 1));
                int    qte     = parseInt(model.getValueAt(i, 2));
                double puHT    = parseDouble(model.getValueAt(i, 3));
                double totalHT = parseDouble(model.getValueAt(i, 4)); // ★ remise déjà incluse
                double tva     = parseDouble(model.getValueAt(i, 5));
                double puTTC   = parseDouble(model.getValueAt(i, 6));
                double remise  = parseDouble(model.getValueAt(i, 7));
                double totalTTC = parseDouble(model.getValueAt(i, 8));
                lignes.add(new ArticleLigne(code, desig, qte, puHT, totalHT, tva, puTTC, remise, totalTTC));
            } catch (Exception e) {
                System.err.println("Erreur conversion ligne " + i + ": " + e.getMessage());
            }
        }
        return lignes;
    }

    private static String str(Object o) { return o != null ? o.toString() : ""; }

    private static int parseInt(Object o) {
        String s = str(o).replaceAll("[^0-9]", "");
        return s.isEmpty() ? 0 : Integer.parseInt(s);
    }

    private static double parseDouble(Object o) {
        String s = str(o).replace(" DT", "").replace(",", ".").replaceAll("[^0-9.-]", "");
        return s.isEmpty() ? 0.0 : Double.parseDouble(s);
    }

    public static JasperPrint remplirRapport(String numeroTicket, String dateTicket,
                                              String nomClient, DefaultTableModel model,
                                              InputStream logoStream, String modePaiement,
                                              double totalHT, double totalTTC,
                                              double montantRemise) throws Exception {

        List<ArticleLigne> lignes = convertirModel(model);
        boolean avecRemise = montantRemise > 0;

        Map<String, Object> params = new HashMap<>();
        // Mécanisme correct pour cette version de JasperReports : paramètre
        // réservé au remplissage, PAS un attribut XML (rejeté par le schéma).
        params.put(net.sf.jasperreports.engine.JRParameter.IS_IGNORE_PAGINATION, Boolean.TRUE);

        params.put("numeroTicket", numeroTicket);
        params.put("dateTicket", dateTicket);
        params.put("nomClient", (nomClient != null && !nomClient.isEmpty()) ? nomClient : "Particulier");
        params.put("modePaiement", modePaiement == null ? "" : modePaiement);
        params.put("totalHT", totalHT);
        params.put("totalTTC", totalTTC);
        params.put("montantRemise", montantRemise);
        params.put("avecRemise", avecRemise);
        params.put("logoImage", logoStream);

        params.put("societeNom", SocieteConfig.getNom());
        params.put("societeAdresse", SocieteConfig.getAdresse());
        params.put("societeTel", SocieteConfig.getTelephone());
        params.put("societeMF", SocieteConfig.getMatriculeFiscale());

        JasperReport rapport = compilerOuChargerTemplate();

        return JasperFillManager.fillReport(rapport, params, new JRBeanCollectionDataSource(lignes));
    }

    private static JasperReport compilerOuChargerTemplate() throws Exception {
        InputStream jrxml = TicketReportService.class.getResourceAsStream(TEMPLATE_PATH);
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
}