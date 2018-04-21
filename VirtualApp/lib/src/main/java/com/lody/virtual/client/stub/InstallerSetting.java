package com.lody.virtual.client.stub;

import java.util.HashSet;
import java.util.Set;

/**
 * @Date 18-4-19 15
 * @Author lxf@xdja.com
 * @Descrip:
 */
public class InstallerSetting {

    static public Set<String> safeApps = new HashSet<>();

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
//        safeApps.add("");
//        safeApps.add("");

    }
}
