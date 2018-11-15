package com.lody.virtual.client.core;

public enum SettingRule {
    Unknown,
    /**
     * other plugin framework
     */
    DisableDlOpen,

    /**
     * other game
     */
    DisableNotCopyApk,
    /**
     * /data/data/{va}/virtual/data/{pkg}/ -> /data/data/{pkg}/
     */
    UseRealDataDir,
    /**
     * /data/data/{va}/virtual/data/{pkg}/lib -> /data/data/{pkg}/lib
     */
    UseOutsideLibraryFiles;

}
