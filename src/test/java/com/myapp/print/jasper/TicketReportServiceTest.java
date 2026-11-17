package com.myapp.print.jasper;

import org.junit.jupiter.api.Test;
import javax.swing.table.DefaultTableModel;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour TicketReportService.convertirModel() — 9 colonnes,
 * identique à Facture/BL/BonSortie/Devis.
 *
 * ★ Vérifie en particulier le correctif appliqué lors de la migration
 * Jasper : le montant de chaque ligne utilise le vrai "Total HT" (colonne
 * 4, remise déjà incluse) au lieu de recalculer qté × PU HT brut — l'ancien
 * FactureImpressionTicket.java ignorait toute remise appliquée sur la
 * ligne.
 */
public class TicketReportServiceTest {

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
                "T001", "Ampoule LED", "3", "2.000", "6.000", "19", "2.380", "0", "7.140");

        List<ArticleLigne> lignes = TicketReportService.convertirModel(model);

        assertEquals(1, lignes.size());
        assertEquals(6.000, lignes.get(0).getTotalHT(), 0.001);
    }

    @Test
    void testMontantLigneUtiliseLeTotalHTNetPasLeBrutRecalcule() {
        // Article à 10.000 DT/unité, quantité 1, remise 20% -> le vrai
        // Total HT (colonne 4) est 8.000, PAS 10.000 (qté × PU HT brut).
        DefaultTableModel model = creerModelAvecUneLigne(
                "T002", "Article remisé", "1", "10.000", "8.000", "0", "8.000", "20", "8.000");

        List<ArticleLigne> lignes = TicketReportService.convertirModel(model);

        assertEquals(8.000, lignes.get(0).getTotalHT(), 0.001,
                "Doit utiliser le Total HT net (8.000), pas qté×PU HT brut (10.000)");
    }

    @Test
    void testLigneAvecValeursVidesNePlanteJamais() {
        DefaultTableModel model = creerModelAvecUneLigne("", "article", "", "", "", "", "", "", "");
        assertDoesNotThrow(() -> TicketReportService.convertirModel(model));
    }

    @Test
    void testPlusieursLignesAucuneNestPerdue() {
        DefaultTableModel model = new DefaultTableModel(COLONNES, 0);
        for (int i = 1; i <= 22; i++) {
            model.addRow(new Object[]{"T" + i, "Article " + i, "1", "1.000", "1.000", "19", "1.190", "0", "1.190"});
        }
        List<ArticleLigne> lignes = TicketReportService.convertirModel(model);
        assertEquals(22, lignes.size());
    }
}
