package com.myapp.print.jasper;

/**
 * Bean simple représentant une ligne d'article de stock.
 * Sert de source de données (JRBeanCollectionDataSource) pour le template Stock.jrxml.
 */
public class ArticleStockLigne {

    private int id;
    private String designation;
    private int stock;

    public ArticleStockLigne(int id, String designation, int stock) {
        this.id = id;
        this.designation = designation;
        this.stock = stock;
    }

    public int getId() {
        return id;
    }

    public String getDesignation() {
        return designation;
    }

    public int getStock() {
        return stock;
    }
}