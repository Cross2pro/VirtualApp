package com.lody.virtual.client.stub;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.lody.virtual.R;

import java.util.HashSet;
import java.util.Set;

/**
 * @Date 18-4-19 15
 * @Author lxf@xdja.com
 * @Descrip:
 */
public class InstallerSetting {

    static public Set<String> safeApps = new HashSet<>();   //认证应用
    static public Set<String> systemApps = new HashSet<>(); //系统应用
    static public Set<String> protectApps = new HashSet<>();//保护应用

    static{

        safeApps.add("com.tencent.mm");         //微信
        safeApps.add("cn.wps.moffice_eng");     //WPS

        safeApps.add("com.xdja.jxclient");      //警信
        safeApps.add("com.xdja.safeclient");    //安全客户端
        safeApps.add("com.xdja.jwt.law");       //法律法规
        safeApps.add("com.xdja.jwt.bj");        //一键报警
        safeApps.add("com.xdja.jwtlxhc");       //拍照识别 & 识别库
        safeApps.add("com.xdja.hdfjwt");        //综合查询
        safeApps.add("com.xdja.jwt.jtgl");      //交通管理
        safeApps.add("com.xdja.jwtlxhc");       //离线核查
        safeApps.add("com.xdja.eoa");           //移动办公
        safeApps.add("com.xdja.uaac");          //统一认证
        safeApps.add("com.xdja.jwt.portal");    //陕西警务
        safeApps.add("com.xdja.swbg");          //税务办公
        safeApps.add("com.xdja.jxpush");        //指令推送 警信依赖
        //预置应用
        systemApps.add("com.fihtdc.filemanager");   //文件管理器
        systemApps.add("com.android.gallery3d");    //图库
        systemApps.add("com.android.providers.media");  //媒体存储
        systemApps.add("net.sourceforge.freecamera");   //相机
        systemApps.add("com.xdja.decrypt");         //解密服务
        systemApps.add("com.xdja.fileexplorer");    //文件浏览器

    }
    public static void addProtectApps(String packageName){
        if(!protectApps.contains(packageName))
            protectApps.add(packageName);
    }
    public static Set<String> getProtectApps(){
        return protectApps;
    }
    public static void deleteProtectApps(String packageName){
        if(protectApps.contains(packageName)){
            protectApps.remove(packageName);
        }
    }

    public static void showToast(Context context, String message, int duration) {
        Toast toast = new Toast(context);
        View toastView = LayoutInflater.from(context).inflate(R.layout.toast_install_del, null);
        TextView contentView = toastView.findViewById(R.id.TextViewInfo);
        contentView.setText(message);
        toast.setView(toastView);
        toast.setDuration(duration);
        toast.setGravity(Gravity.BOTTOM, 0,
                context.getResources().getDimensionPixelOffset(R.dimen.dp_110));
        toast.show();
    }
}
