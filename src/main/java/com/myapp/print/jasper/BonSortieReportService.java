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
 * Remplace BonSortieImpression.java (impression manuelle via Graphics2D).
 *
 * ★ Contrairement au BL (qui recalcule ses totaux en interne), le Bon de
 * Sortie reçoit ses totaux déjà calculés depuis BonSortieUI (via
 * BonSortieManager.calculerTotaux()) — même principe que la Facture.
 * Pas de ligne TVA dans les totaux (comportement d'origine, contrairement
 * à Facture/BL qui en affichent une).
 */
public class BonSortieReportService {

    private static final String TEMPLATE_PATH = "/reports/BonSortie.jrxml";

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
                                              String nom, String prenom,
                                              String adresse, String tel, String matricule,
                                              String matriculeVoiture,
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
        params.put("nomClient", nom == null ? "" : nom);
        params.put("prenomClient", prenom == null ? "" : prenom);
        params.put("adresseClient", adresse == null ? "" : adresse);
        params.put("telClient", tel == null ? "" : tel);
        params.put("matriculeFiscale", matricule == null ? "" : matricule);
        params.put("matriculeVoiture", matriculeVoiture == null ? "" : matriculeVoiture);
        params.put("avecVehicule", matriculeVoiture != null && !matriculeVoiture.trim().isEmpty());
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
        InputStream jrxml = BonSortieReportService.class.getResourceAsStream(TEMPLATE_PATH);
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