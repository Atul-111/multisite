
package com.multisite.core.services;

public interface AssetNotificationService {

    void sendNotification(String assetPath, String action)
            throws Exception;
}
