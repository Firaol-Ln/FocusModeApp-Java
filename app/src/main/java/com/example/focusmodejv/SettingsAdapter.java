package com.example.focusmodejv;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.focusmodejv.model.SettingsItem;
import com.google.android.material.materialswitch.MaterialSwitch;
import java.util.List;

public class SettingsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private final List<SettingsItem> items;
    private final OnSettingsClickListener listener;
    private final SharedPreferences prefs;

    public interface OnSettingsClickListener {
        void onItemClick(SettingsItem item);
        void onToggleClick(SettingsItem item, boolean isChecked);
    }

    public SettingsAdapter(Context context, List<SettingsItem> items, OnSettingsClickListener listener) {
        this.items = items;
        this.listener = listener;
        this.prefs = context.getSharedPreferences("Settings", Context.MODE_PRIVATE);
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).getType() == SettingsItem.Type.SECTION_HEADER ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_settings_section, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_settings_row, parent, false);
            return new ItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        SettingsItem item = items.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).tvSectionTitle.setText(item.getTitle());
        } else if (holder instanceof ItemViewHolder) {
            ItemViewHolder itemHolder = (ItemViewHolder) holder;
            itemHolder.tvTitle.setText(item.getTitle());
            itemHolder.tvSubtitle.setText(item.getSubtitle());
            // Icon handling
            if (item.getIconResId() != 0) {
                itemHolder.flIconContainer.setVisibility(View.VISIBLE);
                itemHolder.ivIcon.setImageResource(item.getIconResId());
            } else {
                itemHolder.flIconContainer.setVisibility(View.GONE);
            }

            // Reset visibility
            itemHolder.switchToggle.setVisibility(View.GONE);
            itemHolder.ivArrow.setVisibility(View.GONE);
            itemHolder.tvPremiumBadge.setVisibility(View.GONE);
            itemHolder.tvInfoValue.setVisibility(View.GONE);

            // We ignore isPremiumUser for now as per request to always show premium status
            if (item.isPremium()) {
                itemHolder.tvPremiumBadge.setVisibility(View.VISIBLE);
            }

            // Robust click handling for the entire row
            View.OnClickListener rowClickListener = v -> {
                if (listener != null) {
                    listener.onItemClick(item);
                }
            };

            // Apply listener to both the root CardView and the inner LinearLayout
            itemHolder.itemView.setOnClickListener(rowClickListener);
            if (itemHolder.llItemRoot != null) {
                itemHolder.llItemRoot.setOnClickListener(rowClickListener);
            }

            switch (item.getType()) {
                case TOGGLE:
                    // If item is premium, it's always gated now
                    if (!item.isPremium()) {
                        itemHolder.switchToggle.setVisibility(View.VISIBLE);
                        boolean isChecked = prefs.getBoolean(item.getKey(), false);
                        itemHolder.switchToggle.setOnCheckedChangeListener(null);
                        itemHolder.switchToggle.setChecked(isChecked);
                        itemHolder.switchToggle.setOnCheckedChangeListener((buttonView, isChecked1) -> {
                            prefs.edit().putBoolean(item.getKey(), isChecked1).apply();
                            listener.onToggleClick(item, isChecked1);
                        });
                        
                        // For toggles, clicking the row should toggle the switch
                        View.OnClickListener toggleRowListener = v -> {
                            itemHolder.switchToggle.toggle();
                        };
                        itemHolder.itemView.setOnClickListener(toggleRowListener);
                        if (itemHolder.llItemRoot != null) {
                            itemHolder.llItemRoot.setOnClickListener(toggleRowListener);
                        }
                    } else {
                        // Gated toggle: clicking any part goes to PremiumActivity via rowClickListener
                        itemHolder.itemView.setOnClickListener(rowClickListener);
                        if (itemHolder.llItemRoot != null) {
                            itemHolder.llItemRoot.setOnClickListener(rowClickListener);
                        }
                    }
                    break;
                case NAVIGATE:
                    if (!item.isPremium()) {
                        itemHolder.ivArrow.setVisibility(View.VISIBLE);
                    }
                    // rowClickListener already handles the redirect in SettingsActivity
                    break;
                case ACTION:
                case DIALOG:
                case INFO:
                    if (item.getType() == SettingsItem.Type.INFO) {
                        itemHolder.tvInfoValue.setVisibility(View.VISIBLE);
                        itemHolder.tvInfoValue.setText(item.getInfoValue());
                    }
                    break;
            }
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvSectionTitle;
        HeaderViewHolder(View itemView) {
            super(itemView);
            tvSectionTitle = itemView.findViewById(R.id.tvSectionTitle);
        }
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSubtitle, tvPremiumBadge, tvInfoValue;
        ImageView ivIcon, ivArrow;
        View flIconContainer, llItemRoot;
        MaterialSwitch switchToggle;
        ItemViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            flIconContainer = itemView.findViewById(R.id.flIconContainer);
            llItemRoot = itemView.findViewById(R.id.llItemRoot);
            ivArrow = itemView.findViewById(R.id.ivArrow);
            switchToggle = itemView.findViewById(R.id.switchToggle);
            tvPremiumBadge = itemView.findViewById(R.id.tvPremiumBadge);
            tvInfoValue = itemView.findViewById(R.id.tvInfoValue);
        }
    }
}
