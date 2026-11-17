package com.myapp.print.jasper;

import org.junit.jupiter.api.Test;
import javax.swing.table.DefaultTableModel;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour FactureReportService.convertirModel() — le point
 * d'entrée le plus fragile de la migration Jasper, car il parse du texte
 * saisi/affiché par l'utilisateur (virgules, espaces, symboles %) vers des
 * types numériques stricts.
 *
 * ★ Colonnes réelles (9, spécifiques à Chaa Elect) : 0=Code, 1=Désignation,
 * 2=Qté, 3=PU HT, 4=Total HT, 5=TVA, 6=PU TTC, 7=Remise %, 8=Total TTC.
 *
 * Ces tests ne couvrent pas remplirRapport() (nécessite le vrai moteur
 * JasperReports, indisponible dans cet environnement de test) — seulement
 * la logique de conversion, testable indépendamment.
 */
public class FactureReportServiceTest {

    private static final String[] COLONNES = {
        "Code", "Désignation", "Qté", "PU HT", "Total HT", "TVA", "PU TTC", "Remise %", "Total TTC"
    };

    private DefaultTableModel creerModelAvecUneLigne(String code, String designation, String qte,
                                                        String puHT, String totalHT, String tva,
                                                        String puTTC, String remise, String totalTTC) {
        DefaultTableModel model = new DefaultTableModel(COLONNES, 0);
        model.addRow(new Object[]{code, designation, qte, puHT, totalHT, tva, puTTC, remise, totalTTC});
        return model;
    }

    @Test
    void testConversionLigneSimple() {
        DefaultTableModel model = creerModelAvecUneLigne(
                "ART001", "Câble électrique", "2", "3.500", "7.000", "19", "4.165", "0", "8.330");

        List<ArticleLigne> lignes = FactureReportService.convertirModel(model);

        assertEquals(1, lignes.size());
        ArticleLigne l = lignes.get(0);
        assertEquals("ART001", l.getCode());
        assertEquals("Câble électrique", l.getDesignation());
        assertEquals(2, l.getQte());
        assertEquals(3.500, l.getPuHT(), 0.001);
        assertEquals(7.000, l.getTotalHT(), 0.001);
        assertEquals(19.0, l.getTvaPct(), 0.001);
        assertEquals(4.165, l.getPuTTC(), 0.001);
        assertEquals(0.0, l.getRemisePct(), 0.001);
        assertEquals(8.330, l.getTotalTTC(), 0.001);
    }

    @Test
    void testConversionGereVirguleDecimale() {
        // Certains champs peuvent être saisis/affichés avec une virgule
        // française (1,500) au lieu d'un point (1.500).
        DefaultTableModel model = creerModelAvecUneLigne(
                "ART002", "Disjoncteur", "1", "1,500", "1,500", "19", "1,785", "10", "1,607");

        List<ArticleLigne> lignes = FactureReportService.convertirModel(model);

        assertEquals(1.5, lignes.get(0).getPuHT(), 0.001);
        assertEquals(1.785, lignes.get(0).getPuTTC(), 0.001);
    }

    @Test
    void testConversionGereSymbolePourcentage() {
        // Les colonnes TVA et Remise peuvent contenir "19 %" ou "19%",
        // pas juste "19" — le parsing doit nettoyer ça correctement.
        DefaultTableModel model = creerModelAvecUneLigne(
                "ART003", "Prise murale", "1", "2.000", "2.000", "19 %", "2.380", "5 %", "2.261");

        List<ArticleLigne> lignes = FactureReportService.convertirModel(model);

        assertEquals(19.0, lignes.get(0).getTvaPct(), 0.001);
        assertEquals(5.0, lignes.get(0).getRemisePct(), 0.001);
    }

    @Test
    void testLigneAvecValeursVidesNePlanteJamais() {
        DefaultTableModel model = creerModelAvecUneLigne(
                "", "article", "", "", "", "", "", "", "");

        // La ligne malformée doit être ignorée/donner des zéros, mais ne
        // jamais lever d'exception qui casserait toute l'impression pour
        // une seule ligne fautive.
        assertDoesNotThrow(() -> FactureReportService.convertirModel(model));

        List<ArticleLigne> lignes = FactureReportService.convertirModel(model);
        assertEquals(1, lignes.size());
        assertEquals(0, lignes.get(0).getQte());
        assertEquals(0.0, lignes.get(0).getPuHT(), 0.001);
    }

    @Test
    void testPlusieursLignesAucuneNestPerdue() {
        DefaultTableModel model = new DefaultTableModel(COLONNES, 0);
        for (int i = 1; i <= 15; i++) {
            model.addRow(new Object[]{
                "ART0" + i, "Article " + i, "1",
                "1.000", "1.000", "19", "1.190", "0", "1.190"
            });
        }

        List<ArticleLigne> lignes = FactureReportService.convertirModel(model);
        assertEquals(15, lignes.size());
    }

    @Test
    void testTotalTTCSurPlusieursLignes() {
        DefaultTableModel model = new DefaultTableModel(COLONNES, 0);
        model.addRow(new Object[]{"A1", "a", "1", "1.500", "1.500", "0", "1.500", "0", "1.500"});
        model.addRow(new Object[]{"A2", "b", "2", "1.500", "3.000", "0", "1.500", "0", "3.000"});

        List<ArticleLigne> lignes = FactureReportService.convertirModel(model);
        double totalTTC = lignes.stream().mapToDouble(ArticleLigne::getTotalTTC).sum();

        assertEquals(4.500, totalTTC, 0.001);
    }
}
