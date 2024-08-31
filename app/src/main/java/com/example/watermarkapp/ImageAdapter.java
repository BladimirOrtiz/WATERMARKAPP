package com.example.watermarkapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {

    private List<Bitmap> imageList;
    private final RemoveImageCallback removeImageCallback;
    private final ReplaceImageCallback replaceImageCallback;

    // Constructor
    public ImageAdapter(List<Bitmap> imageList, RemoveImageCallback removeImageCallback, ReplaceImageCallback replaceImageCallback) {
        this.imageList = imageList;
        this.removeImageCallback = removeImageCallback;
        this.replaceImageCallback = replaceImageCallback;
    }

    // Interfaz para manejar la eliminación de imágenes
    public interface RemoveImageCallback {
        void onRemoveImage(int position);
    }

    // Interfaz para manejar la sustitución de imágenes
    public interface ReplaceImageCallback {
        void onReplaceImage(int position, Bitmap newBitmap);
    }

    // ViewHolder para manejar la vista de cada elemento
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView imageView1;
        public ImageView imageView2;
        public ImageView imageView3;

        public ViewHolder(View view) {
            super(view);
            imageView1 = view.findViewById(R.id.imageView1);
            imageView2 = view.findViewById(R.id.imageView2);
            imageView3 = view.findViewById(R.id.imageView3);
        }
    }

    // Inflar el layout para cada fila del RecyclerView
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_imagen, parent, false);
        return new ViewHolder(view);
    }

    // Vincular los datos con las vistas en la fila
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        int index = position * 3;
        if (index < imageList.size()) {
            holder.imageView1.setImageBitmap(imageList.get(index));
            holder.imageView1.setVisibility(View.VISIBLE);
            holder.imageView1.setOnClickListener(v -> showFullImageDialog(v.getContext(), imageList.get(index), index));
            holder.imageView1.setOnLongClickListener(v -> {
                removeImageCallback.onRemoveImage(index);
                return true;
            });
        }

        if (index + 1 < imageList.size()) {
            holder.imageView2.setImageBitmap(imageList.get(index + 1));
            holder.imageView2.setVisibility(View.VISIBLE);
            holder.imageView2.setOnClickListener(v -> showFullImageDialog(v.getContext(), imageList.get(index + 1), index + 1));
            holder.imageView2.setOnLongClickListener(v -> {
                removeImageCallback.onRemoveImage(index + 1);
                return true;
            });
        }

        if (index + 2 < imageList.size()) {
            holder.imageView3.setImageBitmap(imageList.get(index + 2));
            holder.imageView3.setVisibility(View.VISIBLE);
            holder.imageView3.setOnClickListener(v -> showFullImageDialog(v.getContext(), imageList.get(index + 2), index + 2));
            holder.imageView3.setOnLongClickListener(v -> {
                removeImageCallback.onRemoveImage(index + 2);
                return true;
            });
        }
    }

    // Indicar cuántas filas tiene la lista
    @Override
    public int getItemCount() {
        return (int) Math.ceil(imageList.size() / 3.0);
    }

    // Mostrar la imagen en pantalla completa y permitir eliminarla o sustituirla
    private void showFullImageDialog(Context context, Bitmap bitmap, int position) {
        ImageView imageView = new ImageView(context);
        imageView.setImageBitmap(bitmap);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        new AlertDialog.Builder(context)
                .setView(imageView)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .setNeutralButton("Eliminar", (dialog, which) -> {
                    removeImageCallback.onRemoveImage(position);
                    dialog.dismiss();
                })
                .setNegativeButton("Sustituir", (dialog, which) -> {
                    // Aquí puedes agregar la lógica para abrir una selección de imágenes
                    // y reemplazar la imagen actual en la posición 'position'.
                    // Por ahora, simularemos una nueva imagen con un Bitmap vacío.
                    Bitmap newBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
                    replaceImageCallback.onReplaceImage(position, newBitmap);
                    dialog.dismiss();
                })
                .create()
                .show();
    }
}
