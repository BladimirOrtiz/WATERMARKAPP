package com.example.watermarkapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.InputStream
import kotlin.math.min

class Principal : AppCompatActivity() {

    private val PICK_IMAGES_REQUEST = 1
    private val MAX_IMAGES = 10
    private lateinit var imageList: MutableList<Bitmap>
    private lateinit var imageAdapter: ImageAdapter
    private lateinit var btnCargarImagenes: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var linearLayoutImages: LinearLayout

    private val requestPermissionLauncher: ActivityResultLauncher<String> = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            openGallery()
        } else {
            // Handle the case where permission is denied
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_principal)

        // Inicializar las variables
        imageList = ArrayList()
        btnCargarImagenes = findViewById(R.id.btn_cargar_imagenes)
        recyclerView = findViewById(R.id.recycler_view)
        linearLayoutImages = findViewById(R.id.linear_layout_images)
        val btnColocar = findViewById<Button>(R.id.btn_colocar)
        val btnDescargar = findViewById<Button>(R.id.btn_descargar)

        // Cargar la imagen de la marca de agua
        val watermarkBitmap = BitmapFactory.decodeResource(resources, R.drawable.logofcd)

        // Configurar RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        imageAdapter = ImageAdapter(imageList, { position ->
            imageList.removeAt(position)
            imageAdapter.notifyItemRemoved(position)
            updateLinearLayoutImages()
        }, { position, newBitmap ->
            imageList[position] = newBitmap
            imageAdapter.notifyItemChanged(position)
            updateLinearLayoutImages()
        })
        recyclerView.adapter = imageAdapter

        // Verificar y solicitar permisos
        checkAndRequestPermissions()

        // Configurar botón para cargar imágenes
        btnCargarImagenes.setOnClickListener { openGallery() }

        // Configurar botón para colocar marca de agua
        btnColocar.setOnClickListener {
            showWatermarkPositionDialog(watermarkBitmap)
        }

        // Configurar botón para descargar imágenes con marca de agua
        btnDescargar.setOnClickListener {
            for ((index, bitmap) in imageList.withIndex()) {
                saveImageToGallery(bitmap, "imagen_marcada_$index.jpg")
            }
            showDownloadNotification()
        }
    }

    private fun checkAndRequestPermissions() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED -> {
                // Permiso ya concedido
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(Intent.createChooser(intent, "Seleccionar Imágenes"), PICK_IMAGES_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGES_REQUEST && resultCode == RESULT_OK && data != null) {
            if (data.clipData != null) {
                val count = data.clipData!!.itemCount
                val limit = Math.min(count, MAX_IMAGES - imageList.size)

                for (i in 0 until limit) {
                    val imageUri = data.clipData!!.getItemAt(i).uri
                    addImageFromUri(imageUri)
                }
            } else if (data.data != null) {
                val imageUri = data.data
                addImageFromUri(imageUri!!)
            }
            updateLinearLayoutImages()
            imageAdapter.notifyDataSetChanged()
            recyclerView.visibility = if (imageList.isNotEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun addImageFromUri(imageUri: Uri) {
        var imageStream: InputStream? = null
        try {
            imageStream = contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(imageStream)
            if (imageList.size < MAX_IMAGES) {
                imageList.add(bitmap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            imageStream?.close()
        }
    }

    private fun showWatermarkPositionDialog(watermark: Bitmap) {
        // Crear un AlertDialog para seleccionar la posición de la marca de agua
        val dialogView = layoutInflater.inflate(R.layout.dialog_watermark_position, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Seleccionar posición de la marca de agua")
            .create()

        // Configurar los botones del dialogo
        val positions = listOf(
            R.id.top_left, R.id.top_center, R.id.top_right,
            R.id.center_left, R.id.center, R.id.center_right,
            R.id.bottom_left, R.id.bottom_center, R.id.bottom_right
        )

        val positionMap = mapOf(
            R.id.top_left to Pair(0f, 0f),
            R.id.top_center to Pair(0.5f, 0f),
            R.id.top_right to Pair(1f, 0f),
            R.id.center_left to Pair(0f, 0.5f),
            R.id.center to Pair(0.5f, 0.5f),
            R.id.center_right to Pair(1f, 0.5f),
            R.id.bottom_left to Pair(0f, 1f),
            R.id.bottom_center to Pair(0.5f, 1f),
            R.id.bottom_right to Pair(1f, 1f)
        )

        for (id in positions) {
            dialogView.findViewById<View>(id).setOnClickListener {
                val (horizontalBias, verticalBias) = positionMap[id]!!
                applyWatermarkWithPosition(watermark, horizontalBias, verticalBias)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun resizeWatermark(watermark: Bitmap, scaleFactor: Float): Bitmap {
        val width = (watermark.width * scaleFactor).toInt()
        val height = (watermark.height * scaleFactor).toInt()
        return Bitmap.createScaledBitmap(watermark, width, height, true)
    }

    private fun calculateWatermarkSize(imageWidth: Int, imageHeight: Int): Float {
        // Define un tamaño máximo para la marca de agua (por ejemplo, 25% del tamaño de la imagen)
        val maxWatermarkWidth = (imageWidth * 0.25).toInt()
        val maxWatermarkHeight = (imageHeight * 0.25).toInt()

        // Ajusta el tamaño de la marca de agua para que encaje en el área segura
        val watermarkBitmap = BitmapFactory.decodeResource(resources, R.drawable.logofcd)
        val scaleFactor = minOf(
            maxWatermarkWidth.toFloat() / watermarkBitmap.width,
            maxWatermarkHeight.toFloat() / watermarkBitmap.height
        )

        return scaleFactor
    }

    private fun applyWatermarkWithPosition(watermark: Bitmap, horizontalBias: Float, verticalBias: Float) {
        val updatedImages = imageList.map { originalBitmap ->
            val result = Bitmap.createBitmap(originalBitmap.width, originalBitmap.height, originalBitmap.config)
            val canvas = Canvas(result)
            canvas.drawBitmap(originalBitmap, 0f, 0f, null)

            // Calcular tamaño y posición de la marca de agua
            val scaleFactor = calculateWatermarkSize(originalBitmap.width, originalBitmap.height)
            val resizedWatermark = resizeWatermark(watermark, scaleFactor)

            // Calcular márgenes y posición
            val margin = 16.dpToPx()
            val left = (originalBitmap.width - resizedWatermark.width - margin) * horizontalBias
            val top = (originalBitmap.height - resizedWatermark.height - margin) * verticalBias

            canvas.drawBitmap(resizedWatermark, left, top, null)
            result
        }

        imageList.clear()
        imageList.addAll(updatedImages)
        imageAdapter.notifyDataSetChanged()
        updateLinearLayoutImages()
    }

    private fun updateLinearLayoutImages() {
        linearLayoutImages.removeAllViews()
        for (i in 0 until imageList.size) {
            val imageView = ImageView(this)
            imageView.layoutParams = LinearLayout.LayoutParams(100.dpToPx(), 100.dpToPx())
            imageView.setImageBitmap(imageList[i])
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
            imageView.setOnClickListener {
                imageList.removeAt(i)
                updateLinearLayoutImages()
                imageAdapter.notifyDataSetChanged()
            }
            linearLayoutImages.addView(imageView)
        }
    }

    private fun saveImageToGallery(bitmap: Bitmap, fileName: String) {
        val savedImageURL = MediaStore.Images.Media.insertImage(
            contentResolver,
            bitmap,
            fileName,
            "Imagen con marca de agua"
        )

        if (savedImageURL != null) {
            Log.d("Principal", "Imagen guardada en la galería: $savedImageURL")
        } else {
            Log.d("Principal", "Error al guardar la imagen.")
        }
    }

    private fun showDownloadNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "watermark_download_channel"
        val channelName = "Watermark Download"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_download)  // Asegúrate de que ic_download está en res/drawable
            .setContentTitle("Descarga completada")
            .setContentText("Las imágenes con marca de agua se han guardado correctamente.")
            .setAutoCancel(true)

        notificationManager.notify(1, notificationBuilder.build())
    }

    // Extensión para convertir dp a px
    private fun Int.dpToPx(): Int {
        val scale = resources.displayMetrics.density
        return (this * scale + 0.5f).toInt()
    }
}
