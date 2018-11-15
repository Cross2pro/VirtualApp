package io.virtualapp.home.device;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.os.VUserInfo;
import com.lody.virtual.os.VUserManager;
import com.lody.virtual.remote.InstalledAppInfo;

import java.util.ArrayList;
import java.util.List;

import io.virtualapp.R;
import io.virtualapp.abs.ui.VUiKit;
import io.virtualapp.home.adapters.AppDeviceAdapter;
import io.virtualapp.home.models.DeviceData;

/**
 * @author Lody
 */
public class DeviceFragment extends Fragment {
    private static final String KEY_SELECT_FROM = "key_device_app";
    private ListView mListView;
    private boolean isAppSettings;
    private AppDeviceAdapter mAppLocationAdapter;

    public static DeviceFragment newInstance(boolean app) {
        Bundle args = new Bundle();
        args.putBoolean(KEY_SELECT_FROM, app);
        DeviceFragment fragment = new DeviceFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Bundle args = getArguments();
        isAppSettings = args != null && args.getBoolean(KEY_SELECT_FROM, true);
        return inflater.inflate(R.layout.fragment_list_settings, null);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        //
        mListView = (ListView) view.findViewById(R.id.listview);
        mAppLocationAdapter = new AppDeviceAdapter(getContext());
        if (isAppSettings) {
            mListView.setAdapter(mAppLocationAdapter);
            loadAppData();
        } else {
            int count = VUserManager.get().getUserCount();
            List<DeviceData> list = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                VUserInfo userInfo = VUserManager.get().getUserInfo(i);
                if (userInfo != null) {
                    DeviceData deviceData = new DeviceData(getContext(), null, userInfo.id);
                    deviceData.name = userInfo.name;
                    list.add(deviceData);
                }
            }
            mAppLocationAdapter.set(list);
            mListView.setAdapter(mAppLocationAdapter);
        }
        mListView.setOnItemClickListener((parent, view1, position, id) -> {
            DeviceDetailAcitivty.open(this, mAppLocationAdapter.getDataItem(position), position);
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null) {
            int pos = data.getIntExtra("pos", -1);
            if (pos >= 0) {
                mAppLocationAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAppLocationAdapter != null) {
            mAppLocationAdapter.notifyDataSetChanged();
        }
    }

    private void loadAppData() {
        ProgressDialog dialog = ProgressDialog.show(getContext(), null, "loading");
        VUiKit.defer().when(() -> {
            List<InstalledAppInfo> infos = VirtualCore.get().getInstalledApps(0);
            List<DeviceData> models = new ArrayList<>();
            for (InstalledAppInfo info : infos) {
                if (!VirtualCore.get().isPackageLaunchable(info.packageName)) {
                    continue;
                }
                int[] userIds = info.getInstalledUsers();
                for (int userId : userIds) {
                    DeviceData data = new DeviceData(getContext(), info, userId);
                    if (userId > 0) {
                        data.name = data.name + "(" + (userId + 1) + ")";
                    }
                    models.add(data);
                }
            }
            return models;
        }).done((list) -> {
            dialog.dismiss();
            mAppLocationAdapter.set(list);
            mAppLocationAdapter.notifyDataSetChanged();
        }).fail((e) -> {
            dialog.dismiss();
        });
    }
}
