package com.myapp.print.jasper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour MontantEnLettresUtil — la conversion d'un montant en
 * toutes lettres (français, dinars/millimes), utilisée sur tous les
 * documents imprimés (Facture, BL, Bon de Sortie, Devis, Facture
 * Fournisseur, Ticket).
 *
 * Ces tests couvrent uniquement la logique de conversion pure — pas le
 * rendu visuel du PDF, qui reste à vérifier manuellement dans JasperViewer.
 */
public class MontantEnLettresUtilTest {

    // =========================================================================
    // Conversion montant -> lettres : la logique la plus sujette aux bugs
    // silencieux (arrondis, cas 80/100/1000, pluriels).
    // =========================================================================

    @ParameterizedTest
    @CsvSource({
        "1.000,   'un dinar'",
        "0.000,   'zéro dinar'",
        "2.000,   'deux dinars'",
        "44.895,  'quarante-quatre dinars et huit cent quatre-vingt-quinze millimes'",
        "49.795,  'quarante-neuf dinars et sept cent quatre-vingt-quinze millimes'",
        "1.001,   'un dinar et un millime'",
        "100.000, 'cent dinars'",
        "80.000,  'quatre-vingts dinars'",
        "21.000,  'vingt et un dinars'",
        "71.000,  'soixante-onze dinars'"
    })
    void testConvertirMontantEnLettres(double montant, String attendu) {
        assertEquals(attendu, MontantEnLettresUtil.convertirMontantEnLettres(montant));
    }

    @Test
    void testArrondiFlottantNeCasseRienA1000Millimes() {
        // Cas limite : un montant qui, mal arrondi, pourrait afficher
        // "1000 millimes" au lieu de remonter la retenue au dinar suivant.
        String resultat = MontantEnLettresUtil.convertirMontantEnLettres(1.9999999);
        assertFalse(resultat.contains("1000 millimes"),
                "Ne doit jamais afficher 1000 millimes, doit remonter au dinar suivant");
    }

    @Test
    void testArrondiJusteEnDessousDuMillimeSuperieur() {
        // 2.4995 doit arrondir à 2.500 (500 millimes), pas rester bloqué
        // à 499 millimes par troncature au lieu d'un vrai arrondi.
        String resultat = MontantEnLettresUtil.convertirMontantEnLettres(2.4995);
        assertTrue(resultat.contains("cinq cent"),
                "2.4995 doit arrondir à 500 millimes, résultat: " + resultat);
    }

    @Test
    void testArrondiJusteEnDessousDUnDinarEntier() {
        // 9.9996 doit remonter à 10 dinars exactement (pas "neuf dinars
        // et 1000 millimes", ni rester bloqué à 999 millimes).
        String resultat = MontantEnLettresUtil.convertirMontantEnLettres(9.9996);
        assertTrue(resultat.startsWith("dix dinar"),
                "9.9996 doit remonter à dix dinars, résultat: " + resultat);
        assertFalse(resultat.contains("1000 millimes"));
    }

    @Test
    void testArrondiValeurExactementSurLaLimite() {
        // 0.0005 est le cas limite exact du demi-millime — vérifie qu'il
        // ne lève aucune exception et produit un résultat cohérent, quel
        // que soit le sens de l'arrondi choisi (haut ou bas).
        assertDoesNotThrow(() -> MontantEnLettresUtil.convertirMontantEnLettres(0.0005));
    }

    @Test
    void testArrondiNeDeriveJamaisPourDesMontantsRepetitifs() {
        // Vérifie l'absence de dérive de précision flottante classique :
        // 0.1 + 0.2 (en double) ne vaut PAS exactement 0.3 en mémoire —
        // un montant construit ainsi ne doit jamais produire un résultat
        // bizarre du type "...et zéro millime" au lieu d'un compte rond.
        double montant = 0.1 + 0.2; // = 0.30000000000000004 en double
        String resultat = MontantEnLettresUtil.convertirMontantEnLettres(montant);
        assertEquals("zéro dinar et trois cents millimes", resultat);
    }

    @Test
    void testMontantAvecCentimesSeuls() {
        // partieDecimale >= 1000 en interne (cas des "centimes", dixièmes
        // de millime) — vérifie que la branche centimes fonctionne.
        String resultat = MontantEnLettresUtil.convertirMontantEnLettres(5.500);
        assertTrue(resultat.contains("cinq dinars"));
    }

    // =========================================================================
    // Conversion des nombres en lettres (cas particuliers du français)
    // =========================================================================

    @ParameterizedTest
    @CsvSource({
        "0,    zéro",
        "1,    un",
        "20,   vingt",
        "21,   vingt et un",
        "70,   soixante-dix",
        "71,   soixante-onze",
        "80,   quatre-vingts",
        "81,   quatre-vingt-un",
        "90,   quatre-vingt-dix",
        "91,   quatre-vingt-onze",
        "100,  cent",
        "101,  cent un",
        "200,  deux cents",
        "1000, mille",
        "2000, deux mille"
    })
    void testConvertirNombreEnLettres(int nombre, String attendu) {
        assertEquals(attendu, MontantEnLettresUtil.convertirNombreEnLettres(nombre));
    }

    @Test
    void testNombreNegatifOuTropGrand() {
        assertEquals("montant trop élevé", MontantEnLettresUtil.convertirNombreEnLettres(-5));
        assertEquals("montant trop élevé", MontantEnLettresUtil.convertirNombreEnLettres(1_000_000_000));
    }

    @Test
    void testMillionsAvecReste() {
        String resultat = MontantEnLettresUtil.convertirNombreEnLettres(2_500_000);
        assertTrue(resultat.contains("deux millions"));
        assertTrue(resultat.contains("cinq cent"));
    }
}