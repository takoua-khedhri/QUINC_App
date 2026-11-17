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
 * Remplace FactureFournisseurImpression.java (impression manuelle via
 * Graphics2D).
 *
 * ★ Colonnes réelles (7, ni 9 ni 6) : Ref., Désignation, Qte, PU HT,
 * Total HT, TVA, Total TTC — pas de PU TTC séparé, pas de remise par ligne.
 * ★ Le libellé "TVA (19%)" est un texte fixe dans l'original (pas calculé
 * dynamiquement à partir des vraies lignes) — reproduit tel quel.
 * ★ Retenue à la source : pourcentage saisi librement par l'utilisateur
 * (pas une liste fixe comme sur les autres documents), affichée seulement
 * si son montant est positif.
 */
public class FactureFournisseurReportService {

    private static final String TEMPLATE_PATH = "/reports/FactureFournisseur.jrxml";

    public static List<LigneAchat> convertirModel(DefaultTableModel model) {
        List<LigneAchat> lignes = new ArrayList<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            try {
                String ref     = str(model.getValueAt(i, 0));
                String desig   = str(model.getValueAt(i, 1));
                int    qte     = parseInt(model.getValueAt(i, 2));
                double puHT    = parseDouble(model.getValueAt(i, 3));
                double totalHT = parseDouble(model.getValueAt(i, 4));
                double tva     = parseDouble(model.getValueAt(i, 5));
                double totalTTC = parseDouble(model.getValueAt(i, 6));
                lignes.add(new LigneAchat(ref, desig, qte, puHT, totalHT, tva, totalTTC));
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

    public static JasperPrint remplirRapport(String numeroFacture, String dateFacture,
                                              String numeroFactureFournisseur,
                                              String nomFournisseur, String matriculeFiscale,
                                              String adresseFournisseur, String telFournisseur,
                                              DefaultTableModel model,
                                              double retenueSourcePct, double retenueSourceMontant,
                                              double netAPayer, InputStream logoStream) throws Exception {

        List<LigneAchat> lignes = convertirModel(model);

        double totalHT = 0, totalTVA = 0;
        for (LigneAchat l : lignes) {
            totalHT  += l.getTotalHT();
            totalTVA += (l.getTotalTTC() - l.getTotalHT());
        }
        double totalTTC = totalHT + totalTVA;
        boolean hasRetenue = retenueSourcePct > 0.0;
        boolean avecNumeroFournisseur = numeroFactureFournisseur != null && !numeroFactureFournisseur.trim().isEmpty();

        Map<String, Object> params = new HashMap<>();
        params.put("numeroFacture", numeroFacture);
        params.put("dateFacture", dateFacture);
        params.put("numeroFactureFournisseur", numeroFactureFournisseur == null ? "" : numeroFactureFournisseur);
        params.put("avecNumeroFournisseur", avecNumeroFournisseur);
        params.put("nomFournisseur", nomFournisseur == null ? "" : nomFournisseur);
        params.put("matriculeFiscale", matriculeFiscale == null ? "" : matriculeFiscale);
        params.put("adresseFournisseur", adresseFournisseur == null ? "" : adresseFournisseur);
        params.put("telFournisseur", telFournisseur == null ? "" : telFournisseur);
        params.put("totalHT", totalHT);
        params.put("totalTVA", totalTVA);
        params.put("totalTTC", totalTTC);
        params.put("retenueSourcePct", retenueSourcePct);
        params.put("retenueSourceMontant", retenueSourceMontant);
        params.put("hasRetenue", hasRetenue);
        params.put("netAPayer", netAPayer);
        params.put("logoImage", logoStream);

        params.put("montantEnLettres", MontantEnLettresUtil.convertirMontantEnLettres(netAPayer));

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
        InputStream jrxml = FactureFournisseurReportService.class.getResourceAsStream(TEMPLATE_PATH);
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