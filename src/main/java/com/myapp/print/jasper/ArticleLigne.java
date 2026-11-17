package com.myapp.print.jasper;

/**
 * Représente une ligne d'article pour l'impression via JasperReports.
 *
 * ★ Adapté à la vraie structure de l'application Quincaillerie — 9 colonnes
 * (Code, Désignation, Qté, PU HT, Total HT, TVA, PU TTC, Remise %, Total TTC),
 * différente de celle d'une autre application (8 colonnes, sans Code ni PU TTC
 * séparé). Ne pas copier cette classe telle quelle vers un autre projet sans
 * vérifier la vraie structure de son tableau.
 */
public class ArticleLigne {

    private String code;
    private String designation;
    private int qte;
    private double puHT;
    private double totalHT;
    private double tvaPct;
    private double puTTC;
    private double remisePct;
    private double totalTTC;

    public ArticleLigne() {
    }

    public ArticleLigne(String code, String designation, int qte, double puHT, double totalHT,
                         double tvaPct, double puTTC, double remisePct, double totalTTC) {
        this.code        = code;
        this.designation = designation;
        this.qte         = qte;
        this.puHT        = puHT;
        this.totalHT     = totalHT;
        this.tvaPct      = tvaPct;
        this.puTTC       = puTTC;
        this.remisePct   = remisePct;
        this.totalTTC    = totalTTC;
    }

    public String getCode()          { return code; }
    public String getDesignation()   { return designation; }
    public int    getQte()           { return qte; }
    public double getPuHT()          { return puHT; }
    public double getTotalHT()       { return totalHT; }
    public double getTvaPct()        { return tvaPct; }
    public double getPuTTC()         { return puTTC; }
    public double getRemisePct()     { return remisePct; }
    public double getTotalTTC()      { return totalTTC; }

    public void setCode(String code)                 { this.code = code; }
    public void setDesignation(String designation)    { this.designation = designation; }
    public void setQte(int qte)                       { this.qte = qte; }
    public void setPuHT(double puHT)                  { this.puHT = puHT; }
    public void setTotalHT(double totalHT)            { this.totalHT = totalHT; }
    public void setTvaPct(double tvaPct)              { this.tvaPct = tvaPct; }
    public void setPuTTC(double puTTC)                { this.puTTC = puTTC; }
    public void setRemisePct(double remisePct)        { this.remisePct = remisePct; }
    public void setTotalTTC(double totalTTC)          { this.totalTTC = totalTTC; }
}