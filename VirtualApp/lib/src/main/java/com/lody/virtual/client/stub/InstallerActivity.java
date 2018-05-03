package com.lody.virtual.client.stub;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.lody.virtual.R;
import com.lody.virtual.client.core.InstallStrategy;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.helper.utils.FileUtils;
import com.lody.virtual.os.VUserHandle;
import com.lody.virtual.remote.InstallResult;
import com.lody.virtual.remote.InstalledAppInfo;
import com.lody.virtual.server.interfaces.IAppRequestListener;
import com.lody.virtual.server.pm.VAppManagerService;

import java.io.File;

/**
 * @Date 18-4-16 15
 * @Author wxd@xdja.com
 * @Descrip:
 */

public class InstallerActivity extends Activity {

    private String TAG = "InstallerActivity";

    LinearLayout ll_install;
    LinearLayout ll_installing;
    LinearLayout ll_installed;
    LinearLayout ll_installed_1;
    TextView tv_warn;
    Button btn_open;
    boolean tv_warn_isshow = false;
    private AppInfo apkinfo;
    private AppInfo sourceapkinfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.custom_installer);
        ll_install = (LinearLayout) findViewById(R.id.ll_install);
        ll_installing = (LinearLayout) findViewById(R.id.ll_installing);
        ll_installed = (LinearLayout) findViewById(R.id.ll_installed);
        ll_installed_1 = (LinearLayout) findViewById(R.id.ll_installed_1);
        tv_warn = (TextView) findViewById(R.id.tv_warn);
        tv_warn.setText("警告：该应用不是来自安全盒应用中心，请注意应用安全。建议在安全盒应用中心下载使用该应用");

        Button btn_install = (Button) findViewById(R.id.btn_install);
        Button btn_quit = (Button) findViewById(R.id.btn_quit);
        btn_open = (Button) findViewById(R.id.btn_open);
        Button btn_cancle = (Button) findViewById(R.id.btn_cancle);
        ImageView img_appicon = (ImageView) findViewById(R.id.img_appicon);
        TextView tv_appname = (TextView) findViewById(R.id.tv_appname);
        TextView tv_source = (TextView) findViewById(R.id.tv_source);


        String path = getIntent().getStringExtra("installer_path");
        String source_apk_packagename = getIntent().getStringExtra("source_apk");
        Log.e(TAG, " Install apk path : " + path + " source_apk : " + source_apk_packagename);

        apkinfo = parseInstallApk(path);

        InstalledAppInfo info = VirtualCore.get().getInstalledAppInfo(source_apk_packagename, 0);
        sourceapkinfo = parseInstallApk(info.apkPath);
        tv_source.setText("应用来源："+sourceapkinfo.name);

        img_appicon.setImageDrawable(apkinfo.icon);
        tv_appname.setText(apkinfo.name);

        if(InstallerSetting.safeApps.contains(apkinfo.packageName)){
            tv_warn_isshow = false;
        }else{
            tv_warn_isshow = true;
        }

        stateChanged(STATE_INSTALL);
        btn_install.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mHandler.sendEmptyMessage(STATE_INSTALL);
                stateChanged(STATE_INSTALLING);
            }
        });

        btn_quit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        btn_open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = VirtualCore.get().getLaunchIntent(apkinfo.packageName, VirtualCore.get().myUserId());
                if(intent!=null)
                    VActivityManager.get().startActivity(intent, VirtualCore.get().myUserId());
                showDelDialog();
            }
        });
        btn_cancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDelDialog();
            }
        });

    }

    private void stateChanged(int state){
        switch(state){
            case STATE_NONE:
                break;
            case STATE_INSTALL:
                ll_install.setVisibility(View.VISIBLE);
                ll_installing.setVisibility(View.INVISIBLE);
                ll_installed.setVisibility(View.INVISIBLE);
                ll_installed_1.setVisibility(View.INVISIBLE);
                tv_warn.setVisibility(tv_warn_isshow?View.VISIBLE:View.INVISIBLE);
                break;
            case STATE_INSTALLING:
                ll_install.setVisibility(View.INVISIBLE);
                ll_installing.setVisibility(View.VISIBLE);
                ll_installed.setVisibility(View.INVISIBLE);
                ll_installed_1.setVisibility(View.INVISIBLE);
                tv_warn.setVisibility(tv_warn_isshow?View.VISIBLE:View.INVISIBLE);
                break;
            case STATE_INSTALLED:
            case STATE_INSTALLFAILED:
                ll_install.setVisibility(View.INVISIBLE);
                ll_installing.setVisibility(View.INVISIBLE);
                ll_installed.setVisibility(View.VISIBLE);
                ll_installed_1.setVisibility(View.VISIBLE);
                tv_warn.setVisibility(View.INVISIBLE);
                Intent intent = VirtualCore.get().getLaunchIntent(apkinfo.packageName, VirtualCore.get().myUserId());
                if(intent==null){
                    btn_open.setText("完成");
                }else {
                    btn_open.setText("打开");
                }
                break;
        }

    }

    private AppInfo parseInstallApk(String path) {
        AppInfo appinfo = new AppInfo();
        File f = new File(path);
        PackageManager pm = VirtualCore.get().getContext().getPackageManager();
        try {
            PackageInfo pkgInfo = VirtualCore.get().getContext().getPackageManager().getPackageArchiveInfo(f.getAbsolutePath(), 0);
            ApplicationInfo ai = pkgInfo.applicationInfo;
            ai.sourceDir = f.getAbsolutePath();
            ai.publicSourceDir = f.getAbsolutePath();
            appinfo.packageName = pkgInfo.packageName;
            appinfo.icon = ai.loadIcon(pm);
            appinfo.name = ai.loadLabel(pm);
            appinfo.path = path;
            Log.e(TAG, " packageName : " + appinfo.packageName + " name : " + appinfo.name);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return appinfo;
    }

    public class AppInfo {
        public String packageName;
        public Drawable icon;
        public CharSequence name;
        public String path;
    }
    private final int STATE_NONE= -1;
    private final int STATE_INSTALL = 0;
    private final int STATE_INSTALLING = 1;
    private final int STATE_INSTALLED = 2;
    private final int STATE_INSTALLFAILED = 3;
    private InstallerHandler mHandler = new InstallerHandler();
    class InstallerHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch(msg.what){
                case STATE_INSTALL:

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            InstallResult res = VirtualCore.get().installPackage(apkinfo.path, InstallStrategy.UPDATE_IF_EXIST);
                            Message msg1 = new Message();
                            msg1.what = STATE_INSTALLING;
                            msg1.obj = res;
                            mHandler.sendMessage(msg1);
                        }
                    }).start();
                    break;
                case STATE_INSTALLING:
                    InstallResult res = (InstallResult)msg.obj;
                    if (res.isSuccess) {
                        if (res.isUpdate) {
                            VAppManagerService.get().sendUpdateBroadcast(res.packageName, VUserHandle.ALL);
                        } else {
                            VAppManagerService.get().sendInstalledBroadcast(res.packageName, VUserHandle.ALL);
                        }

                        try {
                            IAppRequestListener listener = VirtualCore.get().getAppRequestListener();
                            listener.onRequestInstall(apkinfo.packageName);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        //mHandler.sendEmptyMessage(STATE_INSTALLED);
                        stateChanged(STATE_INSTALLED);
                    }else{
                        //mHandler.sendEmptyMessage(STATE_INSTALLFAILED);
                        stateChanged(STATE_INSTALLFAILED);
                    }
                    break;
                case STATE_INSTALLED:
                    break;
                case STATE_INSTALLFAILED:
                    break;

            }
        }
    }
    private void showDelDialog(){

        final AlertDialog delDlg = new AlertDialog.Builder(InstallerActivity.this).create();
        delDlg.getWindow().setGravity(Gravity.BOTTOM);
        delDlg.show();
        delDlg.setContentView(R.layout.custom_installer_del);

        Button btn_del_cancle = delDlg.getWindow().findViewById(R.id.btn_del_cancel);
        btn_del_cancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                delDlg.dismiss();
                finish();
            }
        });
        Button btn_del_del = delDlg.getWindow().findViewById(R.id.btn_del_del);
        btn_del_del.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                boolean delsuc = FileUtils.deleteDir(apkinfo.path);
                if(delsuc){
                    Toast.makeText(InstallerActivity.this,"安装包删除成功",Toast.LENGTH_SHORT).show();
                }
                delDlg.dismiss();
                finish();
            }
        });
    }

}