package com.myapp.print.jasper;

/**
 * Ligne d'article pour le Bon de Commande.
 *
 * ★ Structure DIFFÉRENTE de ArticleLigne (utilisée par Facture/BL/BonSortie) :
 * le Bon de Commande n'a que 6 colonnes (Référence, Désignation, Quantité,
 * PU Achat, Remise %, Total HT) — pas de TVA, pas de PU TTC, pas de Total
 * TTC. Règle métier confirmée dans BonCommandeManager : "Pour le bon de
 * commande, TTC = HT (pas de TVA)". Ne pas réutiliser ArticleLigne ici,
 * la structure ne correspond pas.
 */
public class LigneCommande {

    private String reference;
    private String designation;
    private int qte;
    private double puAchat;
    private double remisePct;
    private double totalHT;

    public LigneCommande() {
    }

    public LigneCommande(String reference, String designation, int qte,
                          double puAchat, double remisePct, double totalHT) {
        this.reference   = reference;
        this.designation = designation;
        this.qte         = qte;
        this.puAchat     = puAchat;
        this.remisePct   = remisePct;
        this.totalHT     = totalHT;
    }

    public String getReference()     { return reference; }
    public String getDesignation()   { return designation; }
    public int    getQte()           { return qte; }
    public double getPuAchat()       { return puAchat; }
    public double getRemisePct()     { return remisePct; }
    public double getTotalHT()       { return totalHT; }

    public void setReference(String reference)     { this.reference = reference; }
    public void setDesignation(String designation) { this.designation = designation; }
    public void setQte(int qte)                    { this.qte = qte; }
    public void setPuAchat(double puAchat)         { this.puAchat = puAchat; }
    public void setRemisePct(double remisePct)     { this.remisePct = remisePct; }
    public void setTotalHT(double totalHT)         { this.totalHT = totalHT; }
}