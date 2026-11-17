package com.myapp.print.jasper;

import org.junit.jupiter.api.Test;
import javax.swing.table.DefaultTableModel;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour BonCommandeReportService.convertirModel() —
 * structure DIFFÉRENTE des autres documents : 6 colonnes seulement
 * (0=Référence, 1=Désignation, 2=Qté, 3=PU Achat, 4=Remise %, 5=Total HT),
 * pas de TVA — règle métier confirmée dans BonCommandeManager
 * ("TTC = HT pour le bon de commande").
 */
public class BonCommandeReportServiceTest {

    private static final String[] COLONNES = {
        "Référence", "Désignation", "Qté", "PU Achat", "Remise %", "Total HT"
    };

    private DefaultTableModel creerModelAvecUneLigne(String... valeurs) {
        DefaultTableModel model = new DefaultTableModel(COLONNES, 0);
        model.addRow((Object[]) valeurs);
        return model;
    }

    @Test
    void testConversionLigneSimple() {
        DefaultTableModel model = creerModelAvecUneLigne(
                "REF001", "Tube néon", "10", "3.500", "0", "35.000");

        List<LigneCommande> lignes = BonCommandeReportService.convertirModel(model);

        assertEquals(1, lignes.size());
        LigneCommande l = lignes.get(0);
        assertEquals("REF001", l.getReference());
        assertEquals("Tube néon", l.getDesignation());
        assertEquals(10, l.getQte());
        assertEquals(3.500, l.getPuAchat(), 0.001);
        assertEquals(0.0, l.getRemisePct(), 0.001);
        assertEquals(35.000, l.getTotalHT(), 0.001);
    }

    @Test
    void testConversionAvecRemise() {
        DefaultTableModel model = creerModelAvecUneLigne(
                "REF002", "Câble 10m", "5", "8.000", "15", "34.000");

        List<LigneCommande> lignes = BonCommandeReportService.convertirModel(model);
        assertEquals(15.0, lignes.get(0).getRemisePct(), 0.001);
    }

    @Test
    void testLigneAvecValeursVidesNePlanteJamais() {
        DefaultTableModel model = creerModelAvecUneLigne("", "article", "", "", "", "");
        assertDoesNotThrow(() -> BonCommandeReportService.convertirModel(model));
    }

    @Test
    void testPlusieursLignesAucuneNestPerdue() {
        DefaultTableModel model = new DefaultTableModel(COLONNES, 0);
        for (int i = 1; i <= 10; i++) {
            model.addRow(new Object[]{"REF" + i, "Article " + i, "1", "1.000", "0", "1.000"});
        }
        List<LigneCommande> lignes = BonCommandeReportService.convertirModel(model);
        assertEquals(10, lignes.size());
    }
}
