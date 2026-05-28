package com.posbah.app;

import android.content.Intent;
import com.getcapacitor.BridgeActivity;
import com.getcapacitor.PluginHandle;
import ee.forgr.capacitor.social.login.GoogleProvider;
import ee.forgr.capacitor.social.login.SocialLoginPlugin;
import ee.forgr.capacitor.social.login.ModifiedMainActivityForSocialLoginPlugin;

public class MainActivity extends BridgeActivity implements ModifiedMainActivityForSocialLoginPlugin {

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Check if the result is for Google Sign-In
        if (requestCode >= GoogleProvider.REQUEST_AUTHORIZE_GOOGLE_MIN && 
            requestCode < GoogleProvider.REQUEST_AUTHORIZE_GOOGLE_MAX) {
            
            PluginHandle pluginHandle = getBridge().getPlugin("SocialLogin");
            if (pluginHandle != null) {
                SocialLoginPlugin plugin = (SocialLoginPlugin) pluginHandle.getInstance();
                plugin.handleGoogleLoginIntent(requestCode, data);
            }
        }
    }

    // Required by the ModifiedMainActivityForSocialLoginPlugin interface
    @Override
    public void IHaveModifiedTheMainActivityForTheUseWithSocialLoginPlugin() {}
}
