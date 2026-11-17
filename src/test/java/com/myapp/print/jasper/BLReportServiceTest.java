package com.myapp.print.jasper;

import org.junit.jupiter.api.Test;
import javax.swing.table.DefaultTableModel;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour BLReportService.convertirModel() — 9 colonnes,
 * identique à Facture (0=Code, 1=Désignation, 2=Qté, 3=PU HT, 4=Total HT,
 * 5=TVA, 6=PU TTC, 7=Remise %, 8=Total TTC).
 */
public class BLReportServiceTest {

    private static final String[] COLONNES = {
        "Code", "Désignation", "Qté", "PU HT", "Total HT", "TVA", "PU TTC", "Remise %", "Total TTC"
    };

    private DefaultTableModel creerModelAvecUneLigne(String... valeurs) {
        DefaultTableModel model = new DefaultTableModel(COLONNES, 0);
        model.addRow((Object[]) valeurs);
        return model;
    }

    @Test
    void testConversionLigneSimple() {
        DefaultTableModel model = creerModelAvecUneLigne(
                "ART001", "Interrupteur", "3", "2.000", "6.000", "19", "2.380", "0", "7.140");

        List<ArticleLigne> lignes = BLReportService.convertirModel(model);

        assertEquals(1, lignes.size());
        ArticleLigne l = lignes.get(0);
        assertEquals("ART001", l.getCode());
        assertEquals(3, l.getQte());
        assertEquals(6.000, l.getTotalHT(), 0.001);
        assertEquals(7.140, l.getTotalTTC(), 0.001);
    }

    @Test
    void testConversionGereVirguleDecimale() {
        DefaultTableModel model = creerModelAvecUneLigne(
                "ART002", "Câble", "1", "1,750", "1,750", "19", "2,083", "0", "2,083");

        List<ArticleLigne> lignes = BLReportService.convertirModel(model);
        assertEquals(1.75, lignes.get(0).getPuHT(), 0.001);
    }

    @Test
    void testLigneAvecValeursVidesNePlanteJamais() {
        DefaultTableModel model = creerModelAvecUneLigne("", "article", "", "", "", "", "", "", "");
        assertDoesNotThrow(() -> BLReportService.convertirModel(model));
    }

    @Test
    void testPlusieursLignesAucuneNestPerdue() {
        DefaultTableModel model = new DefaultTableModel(COLONNES, 0);
        for (int i = 1; i <= 12; i++) {
            model.addRow(new Object[]{"A" + i, "Article " + i, "1", "1.000", "1.000", "19", "1.190", "0", "1.190"});
        }
        List<ArticleLigne> lignes = BLReportService.convertirModel(model);
        assertEquals(12, lignes.size());
    }
}
