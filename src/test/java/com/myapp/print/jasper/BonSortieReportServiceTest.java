package com.myapp.print.jasper;

import org.junit.jupiter.api.Test;
import javax.swing.table.DefaultTableModel;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour BonSortieReportService.convertirModel() — 9
 * colonnes, identique à Facture/BL.
 */
public class BonSortieReportServiceTest {

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
                "ART010", "Rallonge", "1", "5.000", "5.000", "19", "5.950", "0", "5.950");

        List<ArticleLigne> lignes = BonSortieReportService.convertirModel(model);

        assertEquals(1, lignes.size());
        assertEquals("Rallonge", lignes.get(0).getDesignation());
        assertEquals(5.950, lignes.get(0).getTotalTTC(), 0.001);
    }

    @Test
    void testConversionAvecRemise() {
        DefaultTableModel model = creerModelAvecUneLigne(
                "ART011", "Multiprise", "2", "4.000", "6.400", "19", "3.808", "20", "7.616");

        List<ArticleLigne> lignes = BonSortieReportService.convertirModel(model);
        assertEquals(20.0, lignes.get(0).getRemisePct(), 0.001);
    }

    @Test
    void testLigneAvecValeursVidesNePlanteJamais() {
        DefaultTableModel model = creerModelAvecUneLigne("", "article", "", "", "", "", "", "", "");
        assertDoesNotThrow(() -> BonSortieReportService.convertirModel(model));
    }

    @Test
    void testPlusieursLignesAucuneNestPerdue() {
        DefaultTableModel model = new DefaultTableModel(COLONNES, 0);
        for (int i = 1; i <= 10; i++) {
            model.addRow(new Object[]{"S" + i, "Sortie " + i, "1", "1.000", "1.000", "19", "1.190", "0", "1.190"});
        }
        List<ArticleLigne> lignes = BonSortieReportService.convertirModel(model);
        assertEquals(10, lignes.size());
    }
}
