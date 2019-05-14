// IServiceKeepAlive.aidl
package com.xdja.zs;

// Declare any non-default types here with import statements

interface IServiceKeepAlive {
    void addKeepAliveServiceName(String pkgName, String serviceName);
    void removeKeepAliveServiceName(String serviceName);
    void scheduleRunKeepAliveService(String pkgName, int userId);
    void scheduleUpdateKeepAliveList(String pkgName, int action);
}
