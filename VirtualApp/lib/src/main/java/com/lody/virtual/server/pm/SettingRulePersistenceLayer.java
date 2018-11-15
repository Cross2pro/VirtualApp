package com.lody.virtual.server.pm;

import android.os.Parcel;

import com.lody.virtual.helper.PersistenceLayer;
import com.lody.virtual.os.VEnvironment;
import com.lody.virtual.remote.SettingRuleInfo;

import java.util.List;

/**
 * @author Lody
 */

public class SettingRulePersistenceLayer extends PersistenceLayer {

    private VAppManagerService mService;

    public SettingRulePersistenceLayer(VAppManagerService service) {
        super(VEnvironment.getDeviceInfoFile());
        this.mService = service;
    }

    @Override
    public int getCurrentVersion() {
        return 1;
    }

    @Override
    public void writeMagic(Parcel p) {

    }

    @Override
    public boolean verifyMagic(Parcel p) {
        return true;
    }

    @Override
    public void writePersistenceData(Parcel p) {
        List<SettingRuleInfo> infos = mService.getSettingRules();
        int size = infos.size();
        p.writeInt(size);
        for (SettingRuleInfo info : infos){
            info.writeToParcel(p, 0);
        }
    }

    @Override
    public void readPersistenceData(Parcel p) {
        List<SettingRuleInfo> infos = mService.getSettingRules();
        infos.clear();
        int size = p.readInt();
        while (size-- > 0) {
            SettingRuleInfo info = SettingRuleInfo.CREATOR.createFromParcel(p);
            infos.add(info);
        }
    }

    @Override
    public boolean onVersionConflict(int fileVersion, int currentVersion) {
        return false;
    }

    @Override
    public void onPersistenceFileDamage() {
        getPersistenceFile().delete();
    }
}
