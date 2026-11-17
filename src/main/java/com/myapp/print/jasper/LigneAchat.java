package com.myapp.print.jasper;

/**
 * Ligne d'article pour la Facture Fournisseur.
 *
 * ★ Structure DIFFÉRENTE de ArticleLigne (Facture/BL/BonSortie/Devis, 9
 * colonnes) et de LigneCommande (Bon de Commande, 6 colonnes) : ici 7
 * colonnes — Ref., Désignation, Qte, PU HT, Total HT, TVA, Total TTC —
 * pas de PU TTC séparé, pas de remise par ligne.
 */
public class LigneAchat {

    private String reference;
    private String designation;
    private int qte;
    private double puHT;
    private double totalHT;
    private double tvaPct;
    private double totalTTC;

    public LigneAchat() {
    }

    public LigneAchat(String reference, String designation, int qte,
                       double puHT, double totalHT, double tvaPct, double totalTTC) {
        this.reference   = reference;
        this.designation = designation;
        this.qte         = qte;
        this.puHT        = puHT;
        this.totalHT     = totalHT;
        this.tvaPct      = tvaPct;
        this.totalTTC    = totalTTC;
    }

    public String getReference()   { return reference; }
    public String getDesignation() { return designation; }
    public int    getQte()         { return qte; }
    public double getPuHT()        { return puHT; }
    public double getTotalHT()     { return totalHT; }
    public double getTvaPct()      { return tvaPct; }
    public double getTotalTTC()    { return totalTTC; }

    public void setReference(String reference)     { this.reference = reference; }
    public void setDesignation(String designation) { this.designation = designation; }
    public void setQte(int qte)                    { this.qte = qte; }
    public void setPuHT(double puHT)               { this.puHT = puHT; }
    public void setTotalHT(double totalHT)         { this.totalHT = totalHT; }
    public void setTvaPct(double tvaPct)           { this.tvaPct = tvaPct; }
    public void setTotalTTC(double totalTTC)       { this.totalTTC = totalTTC; }
}