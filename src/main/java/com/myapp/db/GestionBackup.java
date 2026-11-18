package com.myapp.db;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class GestionBackup {

    // --- CONFIGURATION ---
    private static final String DB_NAME = "gestion.db";
    
    // 1. Nom du dossier dans le CLOUD (OneDrive)
    private static final String CLOUD_FOLDER = "Backup_Facturation_Cloud";
    
    // 2. Nom du dossier sur le PC (Disque Dur)
    private static final String LOCAL_FOLDER = "Backup_Facturation_LOCAL";
    
    // Garder seulement les 30 derniers (backup au démarrage ET à la fermeture)
    private static final int MAX_BACKUPS = 30;

    private static boolean automatiqueInstalle = false;

    /**
     * ★ CORRECTIF : le backup n'était fait QUE si l'on fermait l'écran de
     * connexion ou le tableau de bord avec ✕. Extinction de Windows, fermeture
     * d'une autre fenêtre, application restée en arrière-plan… = aucun backup
     * (ex. aucun backup du 17/11 chez un client qui avait pourtant travaillé).
     * À appeler une fois au démarrage : backup immédiat + backup à l'arrêt de
     * la JVM (System.exit, extinction/déconnexion Windows, arrêt du processus).
     */
    public static synchronized void installerSauvegardesAutomatiques() {
        if (automatiqueInstalle) return;
        automatiqueInstalle = true;
        Thread demarrage = new Thread(GestionBackup::effectuerBackup, "backup-demarrage");
        demarrage.setDaemon(true);
        demarrage.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!backupFermetureFait) effectuerBackup();
        }, "backup-arret"));
    }

    private static volatile boolean backupFermetureFait = false;

    /** Backup de fermeture (bouton ✕ de l'écran principal) ; le hook d'arrêt ne le refera pas. */
    public static boolean effectuerBackupFermeture() {
        boolean ok = effectuerBackup();
        if (ok) backupFermetureFait = true;
        return ok;
    }

    public static synchronized boolean effectuerBackup() {
        System.out.println("\n=== 🚀 BACKUP STRICT (Cloud + Local) ===");
        
        File sourceFile = trouverFichierSource();
        if (sourceFile == null) {
            System.err.println("❌ ERREUR : Base de données introuvable !");
            return false;
        }

        List<File> destinations = detecterDestinations();
        
        if (destinations.isEmpty()) {
            System.err.println("❌ ERREUR GRAVE : Aucun dossier de destination n'a pu être créé !");
            return false;
        }

        String backupName = genererNomFichier();
        int succes = 0;

        for (File dossier : destinations) {
            // Création du dossier si inexistant
            if (!dossier.exists()) {
                dossier.mkdirs();
            }
            
            if (copierFichier(sourceFile, dossier, backupName)) {
                succes++;
                nettoyerVieuxBackups(dossier);
            }
        }

        System.out.println("=== FIN BACKUP (" + succes + " copies) ===\n");
        return succes > 0;
    }
    
    public static boolean effectuerBackupSilencieux() {
        return effectuerBackup();
    }

    // ========================================================================
    // 📍 DÉTECTION DES EMPLACEMENTS
    // ========================================================================
    private static List<File> detecterDestinations() {
        List<File> liste = new ArrayList<>();
        String userHome = System.getProperty("user.home"); // Ex: C:\Users\dell

        // --- 1. CLOUD (ONEDRIVE) ---
        String envOneDrive = System.getenv("OneDrive");
        if (envOneDrive != null && new File(envOneDrive).exists()) {
            File dossierCloud = new File(envOneDrive, CLOUD_FOLDER);
            liste.add(dossierCloud);
            System.out.println("☁️  Cible Cloud : " + dossierCloud.getAbsolutePath());
        }

        // --- 2. LOCAL (RACINE UTILISATEUR) ---
        // On force le chemin : C:\Users\dell\Backup_Facturation_LOCAL
        // On ne passe pas par "Documents" pour éviter les conflits
        File dossierLocal = new File(userHome, LOCAL_FOLDER);
        liste.add(dossierLocal);
        System.out.println("🏠 Cible Locale : " + dossierLocal.getAbsolutePath());

        return liste;
    }

    // --- UTILITAIRES ---

    private static File trouverFichierSource() {
        // Cherche le fichier source gestion.db
        String[] chemins = { DB_NAME, "src/" + DB_NAME, System.getProperty("user.dir") + File.separator + DB_NAME };
        for (String chemin : chemins) {
            File f = new File(chemin);
            if (f.exists()) return f;
        }
        return null;
    }

    private static boolean copierFichier(File source, File dossierDest, String nomFichier) {
        try {
            File destFile = new File(dossierDest, nomFichier);
            Files.copy(source.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            // ★ CORRECTIF : Windows conserve la date de modification de la base
            // source ; le backup apparaissait donc avec une date ancienne.
            destFile.setLastModified(System.currentTimeMillis());
            System.out.println("✅ OK -> " + destFile.getAbsolutePath());
            return true;
        } catch (IOException e) {
            System.err.println("❌ Échec copie vers : " + dossierDest.getAbsolutePath());
            e.printStackTrace();
            return false;
        }
    }

    private static String genererNomFichier() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        return "gestion_" + sdf.format(new Date()) + ".db";
    }

    /**
     * ★ Remet la date de modification de chaque backup existant à la date
     * inscrite dans son nom (gestion_aaaa-MM-jj_HH-mm-ss.db) — corrige les
     * anciens backups qui affichaient la date de la base d'origine.
     * Usage : java -cp ... com.myapp.db.GestionBackup corriger-dates <dossier>...
     */
    public static int corrigerDatesBackups(File dossier) {
        int n = 0;
        File[] fichiers = dossier.listFiles((dir, name) -> name.matches("gestion_\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}\\.db"));
        if (fichiers == null) return 0;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        for (File f : fichiers) {
            try {
                Date d = sdf.parse(f.getName().substring(8, 27));
                if (f.setLastModified(d.getTime())) n++;
            } catch (Exception e) {
                System.err.println("Date illisible : " + f.getName());
            }
        }
        return n;
    }

    // Garde uniquement les 10 derniers fichiers
    private static void nettoyerVieuxBackups(File dossier) {
        File[] fichiers = dossier.listFiles((dir, name) -> name.endsWith(".db") && name.startsWith("gestion_"));
        
        if (fichiers != null && fichiers.length > MAX_BACKUPS) {
            // Tri par date (ancien -> récent)
            // ★ CORRECTIF : tri par NOM (gestion_aaaa-MM-jj_HH-mm-ss.db) et non par
            // date de modification, qui pouvait être celle de la base d'origine
            // et faisait supprimer un backup récent au lieu du plus ancien.
            Arrays.sort(fichiers, (f1, f2) -> f1.getName().compareTo(f2.getName()));
            
            // Suppression des plus vieux
            int aSupprimer = fichiers.length - MAX_BACKUPS;
            for (int i = 0; i < aSupprimer; i++) {
                if(fichiers[i].delete()) {
                    System.out.println("🧹 Nettoyage (Ancien supprimé) : " + fichiers[i].getName());
                }
            }
        }
    }
    
    public static void main(String[] args) {
        if (args.length > 0 && "corriger-dates".equals(args[0])) {
            for (int i = 1; i < args.length; i++) {
                System.out.println(args[i] + " : " + corrigerDatesBackups(new File(args[i])) + " fichier(s) corrigé(s)");
            }
            return;
        }
        effectuerBackup();
    }
}