package com.myapp.print.jasper;

import org.junit.jupiter.api.Test;
import javax.swing.table.DefaultTableModel;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour DevisReportService.convertirModel() — 9 colonnes,
 * identique à Facture/BL/BonSortie.
 */
public class DevisReportServiceTest {

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
                "ART020", "Tableau électrique", "1", "45.000", "45.000", "19", "53.550", "0", "53.550");

        List<ArticleLigne> lignes = DevisReportService.convertirModel(model);

        assertEquals(1, lignes.size());
        assertEquals(45.000, lignes.get(0).getPuHT(), 0.001);
        assertEquals(53.550, lignes.get(0).getTotalTTC(), 0.001);
    }

    @Test
    void testConversionGereSymbolePourcentage() {
        DefaultTableModel model = creerModelAvecUneLigne(
                "ART021", "Fil", "1", "1.000", "1.000", "19 %", "1.190", "0 %", "1.190");

        List<ArticleLigne> lignes = DevisReportService.convertirModel(model);
        assertEquals(19.0, lignes.get(0).getTvaPct(), 0.001);
    }

    @Test
    void testLigneAvecValeursVidesNePlanteJamais() {
        DefaultTableModel model = creerModelAvecUneLigne("", "article", "", "", "", "", "", "", "");
        assertDoesNotThrow(() -> DevisReportService.convertirModel(model));
    }

    @Test
    void testPlusieursLignesAucuneNestPerdue() {
        DefaultTableModel model = new DefaultTableModel(COLONNES, 0);
        for (int i = 1; i <= 8; i++) {
            model.addRow(new Object[]{"D" + i, "Devis " + i, "1", "1.000", "1.000", "19", "1.190", "0", "1.190"});
        }
        List<ArticleLigne> lignes = DevisReportService.convertirModel(model);
        assertEquals(8, lignes.size());
    }
}
