package com.MIT.harisharma;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

public class WalkthroughAdapter extends RecyclerView.Adapter<WalkthroughAdapter.ViewHolder> {
    private Context context;
    private WalkthroughItem[] items;

    public WalkthroughAdapter(Context context) {
        this.context = context;
        this.items = new WalkthroughItem[]{
                new WalkthroughItem("Welcome to AI Translator",
                        "Break language barriers with AI-powered translations",
                        R.drawable.ic_translate),
                new WalkthroughItem("Smart Explanations",
                        "Get detailed grammar explanations with every translation",
                        R.drawable.ic_explanation),
                new WalkthroughItem("Learn & Grow",
                        "Improve your language skills with contextual learning",
                        R.drawable.ic_learn)
        };
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_walkthrough, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        WalkthroughItem item = items[position];
        holder.title.setText(item.title);
        holder.description.setText(item.description);
        holder.image.setImageResource(item.imageRes);
    }

    @Override
    public int getItemCount() {
        return items.length;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, description;
        ImageView image;

        ViewHolder(View view) {
            super(view);
            title = view.findViewById(R.id.tvTitle);
            description = view.findViewById(R.id.tvDescription);
            image = view.findViewById(R.id.ivImage);
        }
    }

    static class WalkthroughItem {
        String title, description;
        int imageRes;

        WalkthroughItem(String title, String description, int imageRes) {
            this.title = title;
            this.description = description;
            this.imageRes = imageRes;
        }
    }
}
