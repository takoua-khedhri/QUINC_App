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
 * Remplace DevisImpression.java (impression manuelle via Graphics2D).
 *
 * ★ Colonnes réelles (9, comme Facture/BL/BonSortie) : Code, Désignation,
 * Qté, PU HT, Total HT, TVA, PU TTC, Remise %, Total TTC.
 * ★ Contrairement à Facture/BL, pas de ligne TVA affichée dans les totaux
 * (comportement d'origine, identique à Bon de Sortie) — ni timbre, ni
 * retenue à la source, ni véhicule.
 */
public class DevisReportService {

    private static final String TEMPLATE_PATH = "/reports/Devis.jrxml";

    public static List<ArticleLigne> convertirModel(DefaultTableModel model) {
        List<ArticleLigne> lignes = new ArrayList<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            try {
                String code    = str(model.getValueAt(i, 0));
                String desig   = str(model.getValueAt(i, 1));
                int    qte     = parseInt(model.getValueAt(i, 2));
                double puHT    = parseDouble(model.getValueAt(i, 3));
                double totalHT = parseDouble(model.getValueAt(i, 4));
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
        String s = str(o).replace(",", ".").replaceAll("[^0-9.-]", "");
        return s.isEmpty() ? 0.0 : Double.parseDouble(s);
    }

    public static JasperPrint remplirRapport(String numero, String date,
                                              String nomClient, String prenomClient,
                                              String adresseClient, String telClient,
                                              String matriculeFiscale,
                                              DefaultTableModel model,
                                              InputStream logoStream,
                                              double totalHT, double totalTTC,
                                              double montantRemise) throws Exception {

        List<ArticleLigne> lignes = convertirModel(model);
        boolean avecRemise = montantRemise > 0;
        // ★ CORRECTIF : totalHT est DÉJÀ net de remise et montantRemise est
        // un montant TTC : "totalHT - montantRemise" retirait la remise une
        // 2e fois. On imprime : HT brut − remise HT = HT après remise (net).
        double ratioTTC = totalHT > 0 ? totalTTC / totalHT : 1.0;
        double remiseHT = montantRemise / ratioTTC;
        double totalHTApresRemise = totalHT;
        double totalHTBrut = totalHT + remiseHT;

        Map<String, Object> params = new HashMap<>();
        params.put("numero", numero);
        params.put("date", date);
        params.put("nomClient", nomClient == null ? "" : nomClient);
        params.put("prenomClient", prenomClient == null ? "" : prenomClient);
        params.put("adresseClient", adresseClient == null ? "" : adresseClient);
        params.put("telClient", telClient == null ? "" : telClient);
        params.put("matriculeFiscale", matriculeFiscale == null ? "" : matriculeFiscale);
        params.put("totalHT", totalHTBrut);
        params.put("totalTTC", totalTTC);
        params.put("montantRemise", remiseHT);
        params.put("avecRemise", avecRemise);
        params.put("totalHTApresRemise", totalHTApresRemise);
        params.put("logoImage", logoStream);

        params.put("montantEnLettres", MontantEnLettresUtil.convertirMontantEnLettres(totalTTC));

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
        InputStream jrxml = DevisReportService.class.getResourceAsStream(TEMPLATE_PATH);
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