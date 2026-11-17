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
 * Remplace BonCommandeImpression.java (impression manuelle via Graphics2D).
 *
 * ★ Colonnes réelles (6, pas 9) : Référence, Désignation, Quantité,
 * PU Achat, Remise %, Total HT. Pas de TVA — "Pour le bon de commande,
 * TTC = HT" (règle confirmée dans BonCommandeManager, non modifiée ici).
 */
public class BonCommandeReportService {

    private static final String TEMPLATE_PATH = "/reports/BonCommande.jrxml";

    public static List<LigneCommande> convertirModel(DefaultTableModel model) {
        List<LigneCommande> lignes = new ArrayList<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            try {
                String ref     = str(model.getValueAt(i, 0));
                String desig   = str(model.getValueAt(i, 1));
                int    qte     = parseInt(model.getValueAt(i, 2));
                double puAchat = parseDouble(model.getValueAt(i, 3));
                double remise  = parseDouble(model.getValueAt(i, 4));
                double totalHT = parseDouble(model.getValueAt(i, 5));
                lignes.add(new LigneCommande(ref, desig, qte, puAchat, remise, totalHT));
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
        String s = str(o).replace(",", ".").replaceAll("[^0-9.-]", "");
        return s.isEmpty() ? 0.0 : Double.parseDouble(s);
    }

    public static JasperPrint remplirRapport(String numero, String date,
                                              String nomFournisseur, String adresseFournisseur,
                                              String telFournisseur, String matriculeFournisseur,
                                              String nomAcheteur, String prenomAcheteur,
                                              String telAcheteur, String emailAcheteur,
                                              String observations,
                                              DefaultTableModel model,
                                              InputStream logoStream,
                                              double totalHT, double totalTTC,
                                              double montantRemise) throws Exception {

        List<LigneCommande> lignes = convertirModel(model);

        boolean fournisseurExistant = nomFournisseur != null && !nomFournisseur.isEmpty()
                && (nomAcheteur == null || nomAcheteur.isEmpty());
        boolean avecRemise = montantRemise > 0;

        Map<String, Object> params = new HashMap<>();
        params.put("numero", numero);
        params.put("date", date);
        params.put("nomFournisseur", nomFournisseur == null ? "" : nomFournisseur);
        params.put("adresseFournisseur", adresseFournisseur == null ? "" : adresseFournisseur);
        params.put("telFournisseur", telFournisseur == null ? "" : telFournisseur);
        params.put("matriculeFournisseur", matriculeFournisseur == null ? "" : matriculeFournisseur);
        params.put("nomAcheteur", nomAcheteur == null ? "" : nomAcheteur);
        params.put("prenomAcheteur", prenomAcheteur == null ? "" : prenomAcheteur);
        params.put("telAcheteur", telAcheteur == null ? "" : telAcheteur);
        params.put("emailAcheteur", emailAcheteur == null ? "" : emailAcheteur);
        params.put("fournisseurExistant", fournisseurExistant);
        params.put("observations", observations == null ? "" : observations);
        params.put("avecObservations", observations != null && !observations.trim().isEmpty());
        params.put("totalHT", totalHT);
        params.put("totalTTC", totalTTC);
        params.put("montantRemise", montantRemise);
        params.put("avecRemise", avecRemise);
        params.put("logoImage", logoStream);

        params.put("societeNom", SocieteConfig.getNom());
        params.put("societeActivite", SocieteConfig.getActivite());
        params.put("societeAdresse", SocieteConfig.getAdresse());
        params.put("societeTel", SocieteConfig.getTelephone());
        params.put("societeEmail", SocieteConfig.getEmail());
        params.put("societeMF", SocieteConfig.getMatriculeFiscale());

        JasperReport rapport = compilerOuChargerTemplate();

        return JasperFillManager.fillReport(rapport, params, new JRBeanCollectionDataSource(lignes));
    }

    private static JasperReport compilerOuChargerTemplate() throws Exception {
        InputStream jrxml = BonCommandeReportService.class.getResourceAsStream(TEMPLATE_PATH);
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