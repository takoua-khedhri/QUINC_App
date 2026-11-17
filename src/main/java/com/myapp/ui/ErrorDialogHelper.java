package com.myapp.ui;

import javax.swing.JOptionPane;
import java.awt.Component;

/**
 * Affichage d'erreurs compréhensibles pour l'utilisateur final, sans jargon
 * technique Java/JDBC (ex: "SQLException: no such column", des messages
 * NullPointerException, des chemins de fichiers internes...).
 *
 * ★ Avant cette classe, la plupart des écrans affichaient directement
 * e.getMessage() dans une boîte de dialogue — utile pour un développeur,
 * mais incompréhensible et anxiogène pour un client final. Le détail
 * technique complet reste toujours visible dans la console (utile pour le
 * support), seul ce qui s'affiche à l'écran change.
 */
public class ErrorDialogHelper {

    /**
     * Affiche un message d'erreur clair à l'utilisateur, tout en gardant le
     * détail technique complet dans la console pour le diagnostic.
     *
     * @param parent   composant parent pour centrer la boîte de dialogue
     * @param contexte description compréhensible de ce qui a échoué,
     *                 ex: "Impossible d'enregistrer la facture"
     * @param e        l'exception technique réelle (journalisée, jamais affichée telle quelle)
     */
    public static void afficherErreur(Component parent, String contexte, Exception e) {
        if (e != null) {
            e.printStackTrace(); // détail technique complet, réservé à la console/au support
        }
        String message = contexte + ".\nSi le problème persiste, contactez votre prestataire.";
        JOptionPane.showMessageDialog(parent, message, "Erreur", JOptionPane.ERROR_MESSAGE);
    }

    /** Variante sans exception (erreur détectée sans lever d'exception Java). */
    public static void afficherErreur(Component parent, String contexte) {
        JOptionPane.showMessageDialog(parent, contexte, "Erreur", JOptionPane.ERROR_MESSAGE);
    }

    /** Variante avertissement (moins grave qu'une erreur bloquante). */
    public static void afficherAvertissement(Component parent, String contexte, Exception e) {
        if (e != null) {
            e.printStackTrace();
        }
        JOptionPane.showMessageDialog(parent, contexte, "Attention", JOptionPane.WARNING_MESSAGE);
    }
}