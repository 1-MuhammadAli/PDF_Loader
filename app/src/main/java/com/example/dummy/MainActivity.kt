package com.example.dummy


import android.Manifest
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.example.dummy.DataModel.FileItem
import com.orbitalsonic.pdfloader.interfaces.OnDialogPermissionClickListener
import com.orbitalsonic.pdfloader.utils.Constants.STORAGE_PERMISSION
import com.orbitalsonic.pdfloader.utils.DialogUtils
import com.orbitalsonic.pdfloader.utils.GeneralUtils
import java.io.File


class MainActivity : AppCompatActivity() {

    var fileList:ArrayList<FileItem> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()){
                println("Documents files------------------------------>")
                getAllDocumentFilesFromLocalStorage()
            }else{
                println("showPermissionDialog------------------------------>")
                showPermissionDialog()
            }
        } else {
            if (checkReadWritePermission()) {
                println("Documents files------------------------------AAAAAAAAAAAA>")
                getAllDocumentFilesFromLocalStorage()
            }else{
                requestStoragePermission()
            }
        }


    }

    ///////////////////////////////
    private fun showPermissionDialog() {

        DialogUtils.permissionDialog(
            this,
            object : OnDialogPermissionClickListener {
                override fun onDiscardClick() {
                }

                @RequiresApi(Build.VERSION_CODES.R)
                override fun onProceedClick() {
                    requestExternalStorageManager()
                }

            })

    }
    ////////////////////////////////
    @RequiresApi(Build.VERSION_CODES.R)
    private fun requestExternalStorageManager(){
        try {
            val mIntent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            mIntent.addCategory("android.intent.category.DEFAULT")
            mIntent.data = Uri.parse(String.format("package:%s", packageName))
            openActivityForResult(mIntent)
        } catch (e: Exception) {
            val mIntent = Intent()
            mIntent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
            openActivityForResult(mIntent)
        }
    }

    private fun openActivityForResult(mIntent:Intent) {
        resultLauncher.launch(mIntent)
    }

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()){
                getAllDocumentFilesFromLocalStorage()
            }else{
                showPermissionDialog()
            }
        }
    }
    ///////////////////////////
    private fun checkReadWritePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
    //////////////////////////
    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            STORAGE_PERMISSION
        )
    }
    ////////////////////////
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (STORAGE_PERMISSION==requestCode){
            println("DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD")
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                println("EEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE")
                getAllDocumentFilesFromLocalStorage()
            } else {
                Toast.makeText(this, "Access denied", Toast.LENGTH_SHORT).show()
            }
        }

    }

    fun getAllDocumentFilesFromLocalStorage(){
        val cursor = getAllMediaFilesCursor()
        println("false--------------------------->${cursor?.moveToFirst()}")
        if (true == cursor?.moveToFirst()) {

            println("hhhhhhhhhhhhhhhhhhhhhhhhhhhh")

            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
            val pathCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
            val nameCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val dateCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_MODIFIED)
            val mimeType = cursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)
            val sizeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)

            do {
                val id = cursor.getLong(idCol)
                val path = cursor.getStringOrNull(pathCol) ?: continue
                val name = cursor.getStringOrNull(nameCol) ?: continue
                val dateTime = cursor.getLongOrNull(dateCol) ?: continue
                val type = cursor.getStringOrNull(mimeType) ?: continue
                val size = cursor.getLongOrNull(sizeCol) ?: continue
                val contentUri = ContentUris.appendId(
                    MediaStore.Files.getContentUri("external").buildUpon(),
                    id
                ).build()

                val media =   "Uri:$contentUri,\nPath:$path,\nFileName:$name,\nFileSize:$size,\nDate:$dateTime,\ntype:$type"
                val file = File(path)

                Log.d("TAG","Media: $media")
                if (file.exists()){
                    //println("file Exists----------------------------->")
                    fileList.add(
                        FileItem(
                            pdfFilePath = file,

                            fileName = file.name,
                            //.replace(".pdf", ""),
                            dateCreatedName = GeneralUtils.getDate(dateTime * 1000),
                            dateModifiedName = GeneralUtils.getDate(dateTime * 1000),
                            sizeName = GeneralUtils.getFileSize(size),
                            originalDateCreated = dateTime,
                            originalDateModified = dateTime,
                            originalSize = size
                        )
                    )

                }
                println("list -----------------------> $fileList")

            } while (cursor.moveToNext())
        }
        cursor?.close()
    }
    enum class FileTypes(val mimeTypes: List<String?>) {
        PDF(
            listOf(
                MimeTypeMap.getSingleton().getMimeTypeFromExtension("pdf"),
            ),
        ),
        WORD(
            listOf(
                MimeTypeMap.getSingleton().getMimeTypeFromExtension("doc"),
                MimeTypeMap.getSingleton().getMimeTypeFromExtension("docx"),
            ),
        ),
        PPT(
            listOf(
                MimeTypeMap.getSingleton().getMimeTypeFromExtension("ppt"),
                MimeTypeMap.getSingleton().getMimeTypeFromExtension("pptx"),
            ),
        ),
        EXCEL(
            listOf(
                MimeTypeMap.getSingleton().getMimeTypeFromExtension("xls"),
                MimeTypeMap.getSingleton().getMimeTypeFromExtension("xlsx"),
            ),
        ),
    }


    private fun getAllMediaFilesCursor(): Cursor? {

        val projections =
            arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DATA, //TODO: Use URI instead of this.. see official docs for this field
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.DATE_MODIFIED,
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Files.FileColumns.SIZE
            )

        val sortBy = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

        val selectionArgs =
            FileTypes.values().map { it.mimeTypes }.flatten().filterNotNull().toTypedArray()

        val args = selectionArgs.joinToString {
            "?"
        }

        val selection =
            MediaStore.Files.FileColumns.MIME_TYPE + " IN (" + args + ")"

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            println("qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq")
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            println("ttttttttttttttttttttttttttttttttttttttt")
            MediaStore.Files.getContentUri("external")
        }



        return contentResolver.query(
            collection,
            projections,
            selection,
            selectionArgs,
            sortBy
        )
    }

    /////////////////////////////////////////

//    fun getAllDocumentFilesFromLocalStorage(): List<File> {
//        val documentList = mutableListOf<File>()
//        //val path = getFilesDir().getAbsolutePath()
//        val dir: File = Environment.getExternalStorageDirectory()
//        val path = dir.toString()
//
//        val storageFile = File(path) // Update the path according to the local storage directory on your device
//
//        if (dir.exists() && dir.isDirectory) {
//
//            println("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF")
//            val files = storageFile.listFiles()
//
//            println("files---------------------------->>>>>>$files")
//
//            if (files != null) {
//                for (file in files) {
//                    if (file.isFile && isDocumentFile(file)) {
//                        documentList.add(file)
//                    } else if (file.isDirectory) {
//                        documentList.addAll(getAllDocumentFilesFromDirectory(file))
//                    }
//                }
//                println("files----------------------->$documentList")
//            }
//        }
//
//
//
//        return documentList
//    }



//    fun getAllDocumentFilesFromDirectory(directory: File): List<File> {
//        val documentList = mutableListOf<File>()
//
//        if (directory.exists() && directory.isDirectory) {
//            val files = directory.listFiles()
//
//            if (files != null) {
//                for (file in files) {
//                    if (file.isFile && isDocumentFile(file)) {
//                        documentList.add(file)
//                    } else if (file.isDirectory) {
//                        documentList.addAll(getAllDocumentFilesFromDirectory(file))
//                    }
//                }
//            }
//        }
//
//        return documentList
//    }
//
//    fun isDocumentFile(file: File): Boolean {
//        val extension = file.extension.toLowerCase()
//        val documentExtensions = arrayOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx")
//
//        return extension in documentExtensions
//    }
}



