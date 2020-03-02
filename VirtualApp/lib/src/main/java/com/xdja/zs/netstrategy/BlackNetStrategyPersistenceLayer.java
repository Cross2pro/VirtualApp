package com.xdja.zs.netstrategy;

import android.os.Parcel;

import com.lody.virtual.helper.PersistenceLayer;
import com.lody.virtual.os.VEnvironment;
import com.xdja.zs.controllerService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class BlackNetStrategyPersistenceLayer extends  PersistenceLayer {

    private static final int CURRENT_VERSION = 1;
    private controllerService controllerService;

    public BlackNetStrategyPersistenceLayer(controllerService controllerService) {
        super(VEnvironment.getBlackNetStrategyInfoFile());
        this.controllerService = controllerService;
    }

    @Override
    public int getCurrentVersion() {
        return CURRENT_VERSION;
    }

    @Override
    public boolean verifyMagic(Parcel p) {
        return true;
    }

    @Override
    public void writePersistenceData(Parcel p) {
        HashMap<String,Integer> network_strategy = (HashMap<String, Integer>) controllerService.Black_Network_Strategy;
        int size = network_strategy.size();
        p.writeInt(size);
        for(Map.Entry<String,Integer> entry : network_strategy.entrySet()) {
            p.writeString(entry.getKey());
            p.writeInt(entry.getValue());
        }
    }

    @Override
    public void readPersistenceData(Parcel p, int version) {
        HashMap<String,Integer> network_strategy = (HashMap<String, Integer>) controllerService.Black_Network_Strategy;
        int size = p.readInt();;
        while(size-- > 0) {
            network_strategy.put(p.readString(),p.readInt());
        }
    }

    @Override
    public void onPersistenceFileDamage() {
        getPersistenceFile().delete();
    }
}
