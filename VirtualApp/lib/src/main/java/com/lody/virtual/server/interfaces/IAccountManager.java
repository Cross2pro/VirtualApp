package com.lody.virtual.server.interfaces;

import android.accounts.Account;
import android.accounts.AuthenticatorDescription;
import android.accounts.IAccountManagerResponse;
import android.os.Bundle;
import android.os.RemoteException;

import java.util.Map;

/**
 * @author Lody
 */
public interface IAccountManager extends IPCInterface {

    AuthenticatorDescription[] getAuthenticatorTypes(int userId) throws RemoteException;

    void getAccountsByFeatures(int userId, IAccountManagerResponse response, String type, String[] features) throws RemoteException;

    String getPreviousName(int userId, Account account) throws RemoteException;

    Account[] getAccounts(int userId, String type) throws RemoteException;

    void getAuthToken(int userId, IAccountManagerResponse response, Account account, String authTokenType, boolean notifyOnAuthFailure, boolean expectActivityLaunch, Bundle loginOptions,int callingUid) throws RemoteException;

    void setPassword(int userId, Account account, String password) throws RemoteException;

    void setAuthToken(int userId, Account account, String authTokenType, String authToken) throws RemoteException;

    void setUserData(int userId, Account account, String key, String value) throws RemoteException;

    void hasFeatures(int userId, IAccountManagerResponse response, Account account, String[] features) throws RemoteException;

    void updateCredentials(int userId, IAccountManagerResponse response, Account account, String authTokenType, boolean expectActivityLaunch, Bundle loginOptions) throws RemoteException;

    void editProperties(int userId, IAccountManagerResponse response, String accountType, boolean expectActivityLaunch) throws RemoteException;

    void getAuthTokenLabel(int userId, IAccountManagerResponse response, String accountType, String authTokenType) throws RemoteException;

    String getUserData(int userId, Account account, String key) throws RemoteException;

    String getPassword(int userId, Account account) throws RemoteException;

    void confirmCredentials(int userId, IAccountManagerResponse response, Account account, Bundle options, boolean expectActivityLaunch) throws RemoteException;

    void addAccount(int userId, IAccountManagerResponse response, String accountType, String authTokenType, String[] requiredFeatures, boolean expectActivityLaunch, Bundle optionsIn) throws RemoteException;

    boolean addAccountExplicitly(int userId, Account account, String password, Bundle extras) throws RemoteException;

    boolean removeAccountExplicitly(int userId, Account account) throws RemoteException;

    void renameAccount(int userId, IAccountManagerResponse response, Account accountToRename, String newName) throws RemoteException;

    void removeAccount(int userId, IAccountManagerResponse response, Account account, boolean expectActivityLaunch) throws RemoteException;

    void clearPassword(int userId, Account account) throws RemoteException;

    boolean accountAuthenticated(int userId, Account account) throws RemoteException;

    void invalidateAuthToken(int userId, String accountType, String authToken) throws RemoteException;

    String peekAuthToken(int userId, Account account, String authTokenType) throws RemoteException;

    boolean setAccountVisibility(int userId, Account a, String packageName, int newVisibility) throws RemoteException;

    int getAccountVisibility(int userId, Account a, String packageName) throws RemoteException;

    void startAddAccountSession(IAccountManagerResponse response, String accountType,
                                String authTokenType, String[] requiredFeatures, boolean expectActivityLaunch,
                                Bundle options) throws RemoteException;

    void startUpdateCredentialsSession(IAccountManagerResponse response, Account account,
                                       String authTokenType, boolean expectActivityLaunch,
                                       Bundle options) throws RemoteException;

    void registerAccountListener(String[] accountTypes, String opPackageName) throws RemoteException;

    void unregisterAccountListener(String[] accountTypes, String opPackageName) throws RemoteException;

    Map getPackagesAndVisibilityForAccount(int userId, Account account) throws RemoteException;

    Map getAccountsAndVisibilityForPackage(int userId, String packageName, String accountType) throws RemoteException;

    void finishSessionAsUser(IAccountManagerResponse response, Bundle sessionBundle,
                             boolean expectActivityLaunch, Bundle appInfo, int userId) throws RemoteException;

    void isCredentialsUpdateSuggested(IAccountManagerResponse response, Account account,
                                      String statusToken) throws RemoteException;

    boolean addAccountExplicitlyWithVisibility(int userId, Account account, String password, Bundle extras,
                                               Map visibility) throws RemoteException;

    abstract class Stub implements IAccountManager {
        @Override
        public boolean isBinderAlive() {
            return false;
        }

        @Override
        public boolean pingBinder() {
            return false;
        }
    }
}
