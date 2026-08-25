package com.example.ingresosegresoscx.utils

import android.content.Context
import android.net.Uri
import com.example.ingresosegresoscx.database.DatabaseHelper
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipBackupHelper {

    private const val JSON_ENTRY_NAME = "backup_data.json"
    private const val IMAGES_DIR_NAME = "images"

    fun exportarBackupZip(context: Context, uri: Uri, dbHelper: DatabaseHelper): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { os ->
                ZipOutputStream(BufferedOutputStream(os)).use { zos ->
                    // 1. Agregar JSON
                    val jsonString = JsonBackupUtils.exportarDatos(context, dbHelper)
                    if (jsonString != null) {
                        val entry = ZipEntry(JSON_ENTRY_NAME)
                        zos.putNextEntry(entry)
                        zos.write(jsonString.toByteArray())
                        zos.closeEntry()
                    }

                    // 2. Agregar carpeta images
                    val imagesDir = File(context.filesDir, IMAGES_DIR_NAME)
                    if (imagesDir.exists() && imagesDir.isDirectory) {
                        imagesDir.listFiles()?.forEach { file ->
                            if (file.isFile) {
                                val zipPath = "$IMAGES_DIR_NAME/${file.name}"
                                val entry = ZipEntry(zipPath)
                                zos.putNextEntry(entry)
                                file.inputStream().use { it.copyTo(zos) }
                                zos.closeEntry()
                            }
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun importarBackupZip(context: Context, uri: Uri, dbHelper: DatabaseHelper): Boolean {
        var jsonContent: String? = null
        val imagesDir = File(context.filesDir, IMAGES_DIR_NAME)
        if (!imagesDir.exists()) imagesDir.mkdirs()

        try {
            // Intento 1: Tratar como ZIP
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
                    var entry: ZipEntry? = zis.nextEntry
                    var foundZipEntries = false
                    while (entry != null) {
                        foundZipEntries = true
                        if (entry.name == JSON_ENTRY_NAME) {
                            val baos = ByteArrayOutputStream()
                            zis.copyTo(baos)
                            jsonContent = baos.toString()
                        } else if (entry.name.startsWith("$IMAGES_DIR_NAME/")) {
                            val fileName = entry.name.substringAfterLast("/")
                            if (fileName.isNotEmpty()) {
                                val destFile = File(imagesDir, fileName)
                                destFile.outputStream().use { zis.copyTo(it) }
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                    
                    if (foundZipEntries && jsonContent != null) {
                        return JsonBackupUtils.importarDatos(context, jsonContent!!, dbHelper)
                    }
                }
            }
        } catch (e: Exception) {
            // No es un ZIP o hubo error al leerlo como tal
        }

        // Intento 2: Tratar como JSON legado si no se encontró nada como ZIP
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                jsonContent = inputStream.bufferedReader().readText()
            }
            if (jsonContent != null) {
                return JsonBackupUtils.importarDatos(context, jsonContent!!, dbHelper)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return false
    }
}
