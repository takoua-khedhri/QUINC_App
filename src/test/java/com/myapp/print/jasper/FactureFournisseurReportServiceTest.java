package com.myapp.print.jasper;

import org.junit.jupiter.api.Test;
import javax.swing.table.DefaultTableModel;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour FactureFournisseurReportService.convertirModel() —
 * structure DIFFÉRENTE des autres documents : 7 colonnes (0=Ref.,
 * 1=Désignation, 2=Qte, 3=PU HT, 4=Total HT, 5=TVA, 6=Total TTC) — pas de
 * PU TTC séparé, pas de remise par ligne.
 */
public class FactureFournisseurReportServiceTest {

    private static final String[] COLONNES = {
        "Ref.", "Désignation", "Qte", "P.U. HT", "Total HT", "TVA", "Total TTC"
    };

    private DefaultTableModel creerModelAvecUneLigne(String... valeurs) {
        DefaultTableModel model = new DefaultTableModel(COLONNES, 0);
        model.addRow((Object[]) valeurs);
        return model;
    }

    @Test
    void testConversionLigneSimple() {
        DefaultTableModel model = creerModelAvecUneLigne(
                "REF010", "Disjoncteur 20A", "20", "4.000", "80.000", "19", "95.200");

        List<LigneAchat> lignes = FactureFournisseurReportService.convertirModel(model);

        assertEquals(1, lignes.size());
        LigneAchat l = lignes.get(0);
        assertEquals("REF010", l.getReference());
        assertEquals(20, l.getQte());
        assertEquals(80.000, l.getTotalHT(), 0.001);
        assertEquals(95.200, l.getTotalTTC(), 0.001);
    }

    @Test
    void testConversionGereVirguleDecimale() {
        DefaultTableModel model = creerModelAvecUneLigne(
                "REF011", "Fil 2.5mm", "50", "0,850", "42,500", "19", "50,575");

        List<LigneAchat> lignes = FactureFournisseurReportService.convertirModel(model);
        assertEquals(0.85, lignes.get(0).getPuHT(), 0.001);
    }

    @Test
    void testLigneAvecValeursVidesNePlanteJamais() {
        DefaultTableModel model = creerModelAvecUneLigne("", "article", "", "", "", "", "");
        assertDoesNotThrow(() -> FactureFournisseurReportService.convertirModel(model));
    }

    @Test
    void testPlusieursLignesAucuneNestPerdue() {
        DefaultTableModel model = new DefaultTableModel(COLONNES, 0);
        for (int i = 1; i <= 10; i++) {
            model.addRow(new Object[]{"REF" + i, "Article " + i, "1", "1.000", "1.000", "19", "1.190"});
        }
        List<LigneAchat> lignes = FactureFournisseurReportService.convertirModel(model);
        assertEquals(10, lignes.size());
    }

    @Test
    void testTotalTVACalculableDepuisLesLignes() {
        // Vérifie que la TVA par ligne (totalTTC - totalHT) reste cohérente
        // avec le calcul fait dans remplirRapport() (totalTVA += totalTTC - totalHT).
        DefaultTableModel model = new DefaultTableModel(COLONNES, 0);
        model.addRow(new Object[]{"A", "a", "1", "10.000", "10.000", "19", "11.900"});
        model.addRow(new Object[]{"B", "b", "1", "5.000", "5.000", "19", "5.950"});

        List<LigneAchat> lignes = FactureFournisseurReportService.convertirModel(model);
        double totalTVA = lignes.stream().mapToDouble(l -> l.getTotalTTC() - l.getTotalHT()).sum();

        assertEquals(2.850, totalTVA, 0.001);
    }

    @Test
    void testSommeTVASurBeaucoupDeLignesNeDerivePasParPrecisionFlottante() {
        // ★ Point d'arrondi précis : remplirRapport() cumule la TVA ligne
        // par ligne via "totalTVA += (totalTTC - totalHT)" — une somme de
        // doubles en Java, sujette à une dérive de précision flottante
        // classique quand il y a beaucoup de lignes (chaque addition
        // accumule une infime erreur d'arrondi binaire). Ce test reproduit
        // exactement ce calcul avec 50 lignes à 0.190 DT de TVA chacune et
        // vérifie que le total reste exact à la milliseconde de dinar près.
        DefaultTableModel model = new DefaultTableModel(COLONNES, 0);
        for (int i = 1; i <= 50; i++) {
            model.addRow(new Object[]{"A" + i, "article " + i, "1", "1.000", "1.000", "19", "1.190"});
        }

        List<LigneAchat> lignes = FactureFournisseurReportService.convertirModel(model);
        double totalTVA = 0;
        for (LigneAchat l : lignes) {
            totalTVA += (l.getTotalTTC() - l.getTotalHT());
        }

        // 50 lignes × 0.190 DT de TVA = 9.500 DT exactement.
        assertEquals(9.500, totalTVA, 0.0001,
                "La somme de 50 lignes à 0.190 DT de TVA ne doit pas dériver au-delà de 0.0001");
    }
}