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
    private final ChangeWatermarkPositionCallback changeWatermarkPositionCallback;

    public ImageAdapter(List<Bitmap> imageList, RemoveImageCallback removeImageCallback, ReplaceImageCallback replaceImageCallback, ChangeWatermarkPositionCallback changeWatermarkPositionCallback) {
        this.imageList = imageList;
        this.removeImageCallback = removeImageCallback;
        this.replaceImageCallback = replaceImageCallback;
        this.changeWatermarkPositionCallback = changeWatermarkPositionCallback;
    }

    public interface RemoveImageCallback {
        void onRemoveImage(int position);
    }

    public interface ReplaceImageCallback {
        void onReplaceImage(int position, Bitmap newBitmap);
    }

    // Interfaz para cambiar la posición de la marca de agua
    public interface ChangeWatermarkPositionCallback {
        void onChangeWatermarkPosition(int position);
    }

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

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_imagen, parent, false);
        return new ViewHolder(view);
    }

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
        } else {
            holder.imageView1.setVisibility(View.GONE);
        }

        if (index + 1 < imageList.size()) {
            holder.imageView2.setImageBitmap(imageList.get(index + 1));
            holder.imageView2.setVisibility(View.VISIBLE);
            holder.imageView2.setOnClickListener(v -> showFullImageDialog(v.getContext(), imageList.get(index + 1), index + 1));
            holder.imageView2.setOnLongClickListener(v -> {
                removeImageCallback.onRemoveImage(index + 1);
                return true;
            });
        } else {
            holder.imageView2.setVisibility(View.GONE);
        }

        if (index + 2 < imageList.size()) {
            holder.imageView3.setImageBitmap(imageList.get(index + 2));
            holder.imageView3.setVisibility(View.VISIBLE);
            holder.imageView3.setOnClickListener(v -> showFullImageDialog(v.getContext(), imageList.get(index + 2), index + 2));
            holder.imageView3.setOnLongClickListener(v -> {
                removeImageCallback.onRemoveImage(index + 2);
                return true;
            });
        } else {
            holder.imageView3.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return (int) Math.ceil(imageList.size() / 3.0);
    }

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
                    replaceImageCallback.onReplaceImage(position, null);
                    dialog.dismiss();
                })
                .setOnCancelListener(dialog -> {
                    changeWatermarkPositionCallback.onChangeWatermarkPosition(position);
                })
                .create()
                .show();
    }
}
