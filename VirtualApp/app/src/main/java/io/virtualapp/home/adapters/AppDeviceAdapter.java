package io.virtualapp.home.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import io.virtualapp.R;
import io.virtualapp.abs.ui.BaseAdapterPlus;
import io.virtualapp.home.models.DeviceData;

public class AppDeviceAdapter extends BaseAdapterPlus<DeviceData> {
    public AppDeviceAdapter(Context context) {
        super(context);
    }

    @Override
    protected View createView(int position, ViewGroup parent) {
        View view = inflate(R.layout.item_location_app, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);
        return view;
    }

    @Override
    protected void attach(View view, DeviceData item, int position) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();
        if (item.icon == null) {
            viewHolder.icon.setImageResource(R.drawable.ic_about);
        } else {
            viewHolder.icon.setVisibility(View.VISIBLE);
            viewHolder.icon.setImageDrawable(item.icon);
        }
        viewHolder.label.setText(item.name);
        if (item.getEditor().exists()) {
            viewHolder.location.setText(R.string.mock_device);
        } else if (item.getUserEditor().exists()) {
            viewHolder.location.setText(R.string.mock_global);
        } else {
            viewHolder.location.setText(R.string.mock_none);
        }
    }

    static class ViewHolder extends BaseViewHolder {
        public ViewHolder(View view) {
            super(view);
            icon = $(R.id.item_app_icon);
            label = $(R.id.item_app_name);
            location = $(R.id.item_location);
        }

        final ImageView icon;
        final TextView label, location;
    }
}
