package com.example.watermarkapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.IOException
import java.io.InputStream
import kotlin.math.min

class Principal : AppCompatActivity() {

    private val PICK_IMAGES_REQUEST = 1
    private val MAX_IMAGES = 10
    private lateinit var imageList: MutableList<Bitmap>
    private lateinit var imageUriList: MutableList<Uri> // Para almacenar los URIs de las imágenes
    private lateinit var imageAdapter: ImageAdapter
    private lateinit var btnCargarImagenes: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var linearLayoutImages: LinearLayout

    private val watermarkStatus = mutableMapOf<Bitmap, Boolean>()

    private var Bitmap.isWatermarked: Boolean
        get() = watermarkStatus[this] ?: false
        set(value) {
            watermarkStatus[this] = value
        }

    private val requestPermissionLauncher: ActivityResultLauncher<String> = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            openGallery()
        } else {
            Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_principal)

        imageList = ArrayList()
        imageUriList = ArrayList() // Inicializar la lista de URIs
        btnCargarImagenes = findViewById(R.id.btn_cargar_imagenes)
        recyclerView = findViewById(R.id.recycler_view)
        linearLayoutImages = findViewById(R.id.linear_layout_images)
        val btnColocar = findViewById<Button>(R.id.btn_colocar)
        val btnDescargar = findViewById<Button>(R.id.btn_descargar)

        val watermarkBitmap = BitmapFactory.decodeResource(resources, R.drawable.logofcd)

        recyclerView.layoutManager = LinearLayoutManager(this)
        imageAdapter = ImageAdapter(imageList, { position ->
            imageList.removeAt(position)
            imageAdapter.notifyItemRemoved(position)
            updateLinearLayoutImages()
        }, { position, newBitmap ->
            imageList[position] = newBitmap
            imageAdapter.notifyItemChanged(position)
            updateLinearLayoutImages()
        }, { position ->
            showWatermarkPositionDialog(watermarkBitmap)
        })
        recyclerView.adapter = imageAdapter

        checkAndRequestPermissions()

        btnCargarImagenes.setOnClickListener { openGallery() }

        btnColocar.setOnClickListener {
            showWatermarkPositionDialog(watermarkBitmap)
        }

        btnDescargar.setOnClickListener {
            for ((index, bitmap) in imageList.withIndex()) {
                saveImageToGallery(bitmap, "imagen_marcada_$index.jpg")
            }
            showDownloadNotification()
            imageList.clear()
            imageUriList.clear() // Limpiar también la lista de URIs
            imageAdapter.notifyDataSetChanged()
            updateLinearLayoutImages()
        }
    }

    private fun checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PICK_IMAGES_REQUEST)
        } else {
            openGallery()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(Intent.createChooser(intent, "Seleccionar Imágenes"), PICK_IMAGES_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGES_REQUEST && resultCode == RESULT_OK && data != null) {
            if (data.clipData != null) {
                val count = data.clipData!!.itemCount
                val limit = min(count, MAX_IMAGES - imageList.size)

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
        if (imageUriList.contains(imageUri)) {
            Toast.makeText(this, "La imagen ya ha sido seleccionada", Toast.LENGTH_SHORT).show()
            return // Si la imagen ya existe, no la agregues
        }

        var imageStream: InputStream? = null
        try {
            imageStream = contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(imageStream)
            if (imageList.size < MAX_IMAGES) {
                imageList.add(bitmap)
                imageUriList.add(imageUri) // Añadir el URI a la lista
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            imageStream?.close()
        }
    }

    private fun showWatermarkPositionDialog(watermark: Bitmap) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_watermark_position, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Seleccionar posición de la marca de agua")
            .create()

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
        val maxWatermarkWidth = (imageWidth * 0.25).toInt()
        val maxWatermarkHeight = (imageHeight * 0.25).toInt()
        val watermarkBitmap = BitmapFactory.decodeResource(resources, R.drawable.logofcd)
        val scaleFactor = minOf(
            maxWatermarkWidth.toFloat() / watermarkBitmap.width,
            maxWatermarkHeight.toFloat() / watermarkBitmap.height
        )
        return scaleFactor
    }

    private fun applyWatermarkWithPosition(watermark: Bitmap, horizontalBias: Float, verticalBias: Float) {
        if (imageList.all { it.isWatermarked }) {
            Toast.makeText(this, "Las imágenes ya tienen la marca de agua.", Toast.LENGTH_SHORT).show()
            return
        }

        val updatedImages = imageList.map { originalBitmap ->
            val result = Bitmap.createBitmap(originalBitmap.width, originalBitmap.height, originalBitmap.config)
            val canvas = Canvas(result)
            canvas.drawBitmap(originalBitmap, 0f, 0f, null)

            val scaleFactor = calculateWatermarkSize(originalBitmap.width, originalBitmap.height)
            val resizedWatermark = resizeWatermark(watermark, scaleFactor)
            val margin = 16.dpToPx()
            val left = (originalBitmap.width - resizedWatermark.width - margin) * horizontalBias
            val top = (originalBitmap.height - resizedWatermark.height - margin) * verticalBias

            canvas.drawBitmap(resizedWatermark, left, top, null)
            result.isWatermarked = true
            result
        }

        imageList.clear()
        imageList.addAll(updatedImages)
        imageAdapter.notifyDataSetChanged()
        updateLinearLayoutImages()
    }

    private fun updateLinearLayoutImages() {
        linearLayoutImages.removeAllViews()
        for (i in imageList.indices) {
            val imageView = ImageView(this)
            imageView.layoutParams = LinearLayout.LayoutParams(100.dpToPx(), 100.dpToPx())
            imageView.setImageBitmap(imageList[i])
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
            imageView.setOnClickListener {
                watermarkStatus.remove(imageList[i])
                imageUriList.removeAt(i) // Eliminar el URI correspondiente
                imageList.removeAt(i)
                updateLinearLayoutImages()
                imageAdapter.notifyDataSetChanged()
            }
            linearLayoutImages.addView(imageView)
        }
    }

    private fun saveImageToGallery(bitmap: Bitmap, fileName: String) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/WatermarkedImages")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        uri?.let { // Asegurarse de que el URI no sea nulo
            try {
                // Aquí verificamos si el OutputStream no es nulo
                resolver.openOutputStream(uri)?.let { outputStream ->
                    // Si no es nulo, procedemos a comprimir la imagen
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    outputStream.close() // Cerramos el flujo de salida correctamente
                } ?: run {
                    Toast.makeText(this, "Error al abrir el flujo de salida", Toast.LENGTH_SHORT).show()
                }

                // Después de guardar la imagen, actualizamos el estado IS_PENDING si es necesario
                values.clear()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }

                Toast.makeText(this, "Imagen guardada", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, "Error al guardar la imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun showDownloadNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "download_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Descargas", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Descarga completa")
            .setContentText("Las imágenes con marca de agua se han guardado correctamente.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        notificationManager.notify(1, notification)
    }

    private fun Int.dpToPx(): Int {
        val density = resources.displayMetrics.density
        return (this * density).toInt()
    }
}
