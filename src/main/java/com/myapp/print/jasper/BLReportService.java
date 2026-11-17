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
 * Remplace BLImpression.java (impression manuelle via Graphics2D).
 *
 * ★ Les totaux sont recalculés ICI à partir du tableau, exactement comme le
 * faisait l'ancienne classe BLImpression.printTotals() — PAS transmis
 * depuis BLUI — pour ne rien changer au résultat final déjà connu :
 *   - remise par ligne = (PU TTC × Qté) - Total TTC de la ligne
 *   - TVA affichée = Total TTC global - Total HT global (pas la somme des
 *     TVA ligne par ligne, qui existe mais n'était déjà pas utilisée pour
 *     l'affichage dans l'ancien code)
 *   - "TVA (X%)" affiche le taux de la première ligne comme exemple,
 *     comportement repris tel quel de l'original
 */
public class BLReportService {

    private static final String TEMPLATE_PATH = "/reports/BL.jrxml";

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

    public static JasperPrint remplirRapport(String numeroBL, String dateBL,
                                              String nomClient, String prenomClient,
                                              String adresseClient, String telClient,
                                              String matriculeClient,
                                              String nomChauffeur, String adresseChauffeur,
                                              String telChauffeur, String matriculeVoiture,
                                              DefaultTableModel model,
                                              InputStream logoStream,
                                              String modePaiement) throws Exception {

        List<ArticleLigne> lignes = convertirModel(model);

        // ★ Recalcul fidèle à BLImpression.printTotals() — voir commentaire de classe.
        double totalHT = 0, totalTTC = 0, totalRemise = 0;
        String tauxTVAExemple = lignes.isEmpty() ? "0" : String.valueOf((int) lignes.get(0).getTvaPct());

        for (ArticleLigne l : lignes) {
            double ttcBrut     = l.getPuTTC() * l.getQte();
            double remiseLigne = ttcBrut - l.getTotalTTC();
            if (remiseLigne < 0) remiseLigne = 0;

            totalHT     += l.getTotalHT();
            totalTTC    += l.getTotalTTC();
            totalRemise += remiseLigne;
        }
        double totalTVAReelle     = totalTTC - totalHT;
        // ★ CORRECTIF : totalHT (somme des Total HT de ligne) est déjà net de
        // remise et totalRemise est TTC : ne pas la soustraire une 2e fois.
        double ratioTTC = totalHT > 0 ? totalTTC / totalHT : 1.0;
        double remiseHT = totalRemise / ratioTTC;
        double totalHTApresRemise = totalHT;
        double totalHTBrut = totalHT + remiseHT;
        boolean avecRemise = totalRemise > 0;

        Map<String, Object> params = new HashMap<>();
        params.put("numeroBL", numeroBL);
        params.put("dateBL", dateBL);
        params.put("nomClient", nomClient == null ? "" : nomClient);
        params.put("prenomClient", prenomClient == null ? "" : prenomClient);
        params.put("adresseClient", adresseClient == null ? "" : adresseClient);
        params.put("telClient", telClient == null ? "" : telClient);
        params.put("matriculeClient", matriculeClient == null ? "" : matriculeClient);
        params.put("nomChauffeur", nomChauffeur == null ? "" : nomChauffeur);
        params.put("adresseChauffeur", adresseChauffeur == null ? "" : adresseChauffeur);
        params.put("telChauffeur", telChauffeur == null ? "" : telChauffeur);
        params.put("matriculeVoiture", matriculeVoiture == null ? "" : matriculeVoiture);
        params.put("modePaiement", modePaiement == null ? "" : modePaiement);

        params.put("totalHT", totalHTBrut);
        params.put("totalRemise", remiseHT);
        params.put("avecRemise", avecRemise);
        params.put("totalHTApresRemise", totalHTApresRemise);
        params.put("tauxTVAExemple", tauxTVAExemple);
        params.put("totalTVAReelle", totalTVAReelle);
        params.put("totalTTC", totalTTC);
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
        InputStream jrxml = BLReportService.class.getResourceAsStream(TEMPLATE_PATH);
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