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
 * Remplace FactureImpression.java (impression manuelle via Graphics2D) par
 * un rapport JasperReports — pagination automatique, plus de calcul manuel
 * de hauteur de page.
 *
 * ★ Colonnes réelles du DefaultTableModel de FactureUI (Quincaillerie) :
 * 0=Code, 1=Désignation, 2=Qté, 3=PU HT, 4=Total HT, 5=TVA, 6=PU TTC,
 * 7=Remise %, 8=Total TTC — DIFFÉRENT d'une autre application qui aurait
 * 8 colonnes sans Code ni PU TTC séparé. Ne pas réutiliser ce mapping de
 * colonnes ailleurs sans vérifier la vraie structure du tableau concerné.
 *
 * Infos société codées en dur ici (comme dans l'ancien FactureImpression.java
 * d'origine) — pas de classe SocieteConfig dans ce projet.
 */
public class FactureReportService {

    private static final String TEMPLATE_PATH = "/reports/Facture.jrxml";

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

    public static JasperPrint remplirRapport(String numeroFacture, String dateFacture,
                                              String nomClient, String prenomClient,
                                              String adresseClient, String telClient,
                                              String matriculeFiscale, String matriculeVoiture,
                                              DefaultTableModel model,
                                              InputStream logoStream,
                                              String modePaiement, double timbre,
                                              double totalHT, double totalTVA, double totalTTC,
                                              double totalRemise,
                                              double retenueSource, double netAPayer) throws Exception {

        List<ArticleLigne> lignes = convertirModel(model);

        // ★ CORRECTIF : le paramètre reçu ici (nommé auparavant "totalTTCLignes")
        // est en réalité déjà le TOTAL TTC FINAL, timbre inclus — c'est
        // exactement ce que FactureManager.calculerTotaux() calcule et que
        // FactureUI/ListeFactures transmettent (totaux.totalTTC ou
        // montant_ttc lu en base, qui ont TOUJOURS le timbre déjà additionné).
        // L'ancien code faisait "totalTTCLignes + timbre", ajoutant le timbre
        // une SECONDE fois par-dessus un total qui le contenait déjà — d'où
        // l'écart exact d'1 timbre entre le total attendu et le total imprimé
        // (ex: 0.650 + 1.000 affiché comme 2.650 au lieu de 1.650), reproduit
        // à l'identique à la réimpression depuis la liste des factures
        // puisque la valeur fausse était lue depuis la même logique.
        double totalTTCFinal = totalTTC;
        boolean avecRetenue   = retenueSource > 0;
        boolean avecRemise    = totalRemise > 0;

        Map<String, Object> params = new HashMap<>();
        params.put("numeroFacture", numeroFacture);
        params.put("dateFacture", dateFacture);
        params.put("nomClient", nomClient == null ? "" : nomClient);
        params.put("prenomClient", prenomClient == null ? "" : prenomClient);
        params.put("adresseClient", adresseClient == null ? "" : adresseClient);
        params.put("telClient", telClient == null ? "" : telClient);
        params.put("matriculeFiscale", matriculeFiscale == null ? "" : matriculeFiscale);
        params.put("matriculeVoiture", matriculeVoiture == null ? "" : matriculeVoiture);
        params.put("avecVehicule", matriculeVoiture != null && !matriculeVoiture.trim().isEmpty());
        params.put("modePaiement", modePaiement == null ? "" : modePaiement);
        params.put("timbre", timbre);
        params.put("totalHT", totalHT);
        params.put("totalTVA", totalTVA);
        params.put("totalRemise", totalRemise);
        params.put("avecRemise", avecRemise);
        params.put("retenueSource", retenueSource);
        params.put("avecRetenue", avecRetenue);
        params.put("netAPayer", netAPayer);
        params.put("totalTTCFinal", totalTTCFinal);
        params.put("logoImage", logoStream);

        // ★ Infos société lues dynamiquement depuis SocieteConfig — plus
        // aucune valeur codée en dur ici ni dans le template. Modifier
        // l'écran "Paramètres Société" du tableau de bord change ces
        // valeurs sur tous les prochains documents imprimés.
        params.put("societeNom", SocieteConfig.getNom());
        params.put("societeActivite", SocieteConfig.getActivite());
        params.put("societeAdresse", SocieteConfig.getAdresse());
        params.put("societeTel", SocieteConfig.getTelephone());
        params.put("societeEmail", SocieteConfig.getEmail());
        params.put("societeMF", SocieteConfig.getMatriculeFiscale());

        double montantALettrer = avecRetenue ? netAPayer : totalTTCFinal;
        params.put("montantEnLettres", MontantEnLettresUtil.convertirMontantEnLettres(montantALettrer));
        params.put("libelleArrete", avecRetenue
                ? "Arrêté le net à payer à la somme de :"
                : "Arrêté la présente facture à la somme de :");

        JasperReport rapport = compilerOuChargerTemplate();

        return JasperFillManager.fillReport(rapport, params, new JRBeanCollectionDataSource(lignes));
    }

    private static JasperReport compilerOuChargerTemplate() throws Exception {
        InputStream jrxml = FactureReportService.class.getResourceAsStream(TEMPLATE_PATH);
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