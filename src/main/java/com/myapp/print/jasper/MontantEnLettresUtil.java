package com.myapp.print.jasper;

/**
 * Conversion d'un montant en toutes lettres (français, dinars/millimes).
 * Logique reprise à l'identique de l'ancien FactureImpression.java (et des
 * autres classes d'impression manuelle) pour ne rien changer au texte que
 * voient déjà vos clients.
 */
public class MontantEnLettresUtil {

    public static String convertirMontantEnLettres(double montant) {
        try {
            double montantArrondi = Math.round(montant * 1000.0) / 1000.0;
            int    partieEntiere  = (int) montantArrondi;
            int    partieDecimale = (int) Math.round((montantArrondi - partieEntiere) * 1000);

            String entiereLettres  = convertirNombreEnLettres(partieEntiere);
            String decimaleLettres = (partieDecimale == 0) ? "zéro" : convertirNombreEnLettres(partieDecimale);

            StringBuilder resultat = new StringBuilder();

            if      (partieEntiere == 1) resultat.append("un dinar");
            else if (partieEntiere  > 1) resultat.append(entiereLettres).append(" dinars");
            else                         resultat.append(entiereLettres).append(" dinar");

            if (partieDecimale > 0) {
                resultat.append(" et ");
                if      (partieDecimale == 1)   resultat.append("un millime");
                else if (partieDecimale < 1000) resultat.append(decimaleLettres).append(" millimes");
                else {
                    int centimes = partieDecimale / 10;
                    if      (centimes == 1) resultat.append("un centime");
                    else if (centimes  > 1) resultat.append(convertirNombreEnLettres(centimes)).append(" centimes");
                }
            }

            return resultat.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "Montant non convertible";
        }
    }

    public static String convertirNombreEnLettres(int nombre) {
        if (nombre < 0 || nombre > 999999999) return "montant trop élevé";
        if (nombre == 0) return "zéro";

        String[] unites  = {"", "un", "deux", "trois", "quatre", "cinq", "six", "sept", "huit", "neuf",
                            "dix", "onze", "douze", "treize", "quatorze", "quinze", "seize",
                            "dix-sept", "dix-huit", "dix-neuf"};
        String[] dizaines = {"", "", "vingt", "trente", "quarante", "cinquante",
                             "soixante", "soixante", "quatre-vingt", "quatre-vingt"};

        if (nombre < 20) return unites[nombre];

        if (nombre < 100) {
            int dizaine = nombre / 10;
            int unite   = nombre % 10;
            if (dizaine == 7 || dizaine == 9) {
                int temp = 10 + unite;
                if (temp == 10) return dizaines[dizaine] + "-dix";
                return dizaines[dizaine] + "-" + unites[temp];
            }
            if (unite == 0) return (dizaine == 8) ? dizaines[dizaine] + "s" : dizaines[dizaine];
            if (unite == 1 && dizaine != 8) return dizaines[dizaine] + " et un";
            return dizaines[dizaine] + "-" + unites[unite];
        }

        if (nombre < 1000) {
            int centaines = nombre / 100;
            int reste     = nombre % 100;
            String res    = (centaines == 1) ? "cent" : unites[centaines] + " cent";
            if (reste > 0)          res += " " + convertirNombreEnLettres(reste);
            else if (centaines > 1) res += "s";
            return res;
        }

        if (nombre < 1000000) {
            int milliers = nombre / 1000;
            int reste    = nombre % 1000;
            String res   = (milliers == 1) ? "mille" : convertirNombreEnLettres(milliers) + " mille";
            if (reste > 0) res += " " + convertirNombreEnLettres(reste);
            return res;
        }

        if (nombre < 1000000000) {
            int millions = nombre / 1000000;
            int reste    = nombre % 1000000;
            String res   = convertirNombreEnLettres(millions) + (millions > 1 ? " millions" : " million");
            if (reste > 0) res += " " + convertirNombreEnLettres(reste);
            return res;
        }

        return "montant trop élevé";
    }
}