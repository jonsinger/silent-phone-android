/*
Copyright © 2012-2013, Silent Circle, LLC.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Any redistribution, use, or modification is done solely for personal 
      benefit and not for any commercial purpose or for monetary gain
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name Silent Circle nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL SILENT CIRCLE, LLC BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.silentcircle.silentphone.utils;

import com.silentcircle.silentphone.TiviPhoneService;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.util.Log;
import android.widget.ImageView;

/**
 * This class has static functions only, mainly used for convenience
 * 
 * @author werner
 * 
 */
public class Utilities {
    private static final String LOG_TAG = "Utilities";

    /** Speaker state, persisting between various speaker events */
    private static boolean sIsSpeakerEnabled = false;

    public static long get_time_ms() {
        return System.currentTimeMillis();
    }

    public static long get_time_sec() {
        return System.currentTimeMillis() / 1000;
    }

    static public void Sleep(long ms) {
        try {
            Thread.sleep(ms);
        }
        catch (InterruptedException e) {
        }
    }

    /**
     * Get a configuration value from Tivi C/C++, convert result to String an return it.
     * 
     * @param s the configuration key to look at
     * @return A result string.
     */
    static public String getTCValue(String s) {
        byte[] response = new byte[128];

        if(TiviPhoneService.getSetCfgVal(1, s.getBytes(), s.length(), response) < 0){
           return "";
        }
        int l = 0;
        while (response[l] != 0 && l < 64)
            l++;
        String ret=new String(response, 0, l);
        return ret;
     }

    /**
     * Check configuration if ZRTP is enabled.
     * 
     * @return <code>true</code> if enabled, <code>false</code> otherwise.
     */
    static public boolean checkZRTP() {
        boolean result = false;
        final String zkey = "iCanUseZRTP";
        byte byteResp[] = new byte[64];

        if (TZRTP.bEnabled) {
            TiviPhoneService.getSetCfgVal(1, zkey.getBytes(), zkey.length(), byteResp);
            result = (byteResp[0] - '0') == 1;
        }
        else {
            byteResp[0] = '0';
            TiviPhoneService.getSetCfgVal(0, zkey.getBytes(), zkey.length(), byteResp);
        }
        return result;
    }


    /**
     * Set the caller image if available
     * 
     * @param call
     *            the call information
     */
    static public void setCallerImage(CTCall call, ImageView iw) {
        if (call == null)
            return;

        int height = iw.getMeasuredHeight();
        call.iImgHeight = call.iImgHeight < height ? height : call.iImgHeight;

        if (call.image != null && call.iImgHeight > 0) {
            iw.setImageBitmap(call.image);
            // set maximum image size size, actual size may be smaller
            iw.setMaxHeight(call.iImgHeight);
            iw.setMaxWidth(call.iImgHeight); // try to make it quadratic
            iw.setAdjustViewBounds(true);
            iw.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        }
    }

    static public void turnOnSpeaker(Context context, boolean flag, boolean store) {
       spkrStatic(context, flag);
        // record the speaker-enable value
        if (store) {
            sIsSpeakerEnabled = flag;
        }
    }
    
    /**
     * Restore the speaker mode, called after a wired headset disconnect
     * event.
     */
    static public void restoreSpeakerMode(Context context) {
        // change the mode if needed.
        if (isSpeakerOn(context) != sIsSpeakerEnabled) {
            turnOnSpeaker(context, sIsSpeakerEnabled, false);
        }
    }

    static public boolean isSpeakerOn(Context context) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        return audioManager.isSpeakerphoneOn();
    }

    /**
     * Static function to handle speaker switching and audio manager.
     * 
     * This function takes care of some specific device handling. 
     */
    private static void spkrStatic(Context ctx, boolean bOn) {
        AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        int iApiLevel = android.os.Build.VERSION.SDK_INT;

        boolean bSams = android.os.Build.BRAND.equalsIgnoreCase("samsung");// || android.os.Build.MANUFACTURER.equalsIgnoreCase("samsung");
        boolean bGoogle = !bSams && android.os.Build.BRAND.equalsIgnoreCase("google");

        if(bGoogle && android.os.Build.MANUFACTURER.equalsIgnoreCase("samsung")){
            bGoogle=false;
            bSams=true;
        }


        if ((bGoogle && iApiLevel > 10) || (bSams && (iApiLevel == 5 || iApiLevel == 6 || iApiLevel == 7))) {
            // Samsung 2.0, 2.0.1 and 2.1 devices
            if (bOn) {
                // route audio to back speaker
                am.setMode(AudioManager.MODE_IN_CALL);
                am.setMode(AudioManager.MODE_NORMAL);
                am.setSpeakerphoneOn(bOn);
            }
            else {
                // route audio to earpiece
                am.setMode(AudioManager.MODE_IN_CALL);
                am.setSpeakerphoneOn(bOn);
            }
        }
        else {

            if (bSams) {// isGalaxyS() || nexus) 
                am.setMode(AudioManager.MODE_IN_CALL);
                am.setMode(AudioManager.MODE_NORMAL);
                am.setSpeakerphoneOn(bOn);
            }
            // Non-Samsung and Samsung 2.2 and up devices
            else
                am.setSpeakerphoneOn(bOn);
        }
    }

    /**
     * Plays the specified tone.
     * 
     * The tone is played locally, using the audio stream for phone calls. Tones are played only if the "Audible touch tones" user
     * preference is checked, and are NOT played if the device is in silent mode.
     * 
     * @param tone
     *            a tone code from {@link ToneGenerator}
     */
    public static void playTone(Context ctx, ToneGenerator mToneGenerator, int tone, int duration) {

        // Also do nothing if the phone is in silent mode.
        // We need to re-check the ringer mode for *every* playTone()
        // call, rather than keeping a local flag that's updated in
        // onResume(), since it's possible to toggle silent mode without
        // leaving the current activity (via the ENDCALL-longpress menu.)
        AudioManager audioManager = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        int ringerMode = audioManager.getRingerMode();
        if ((ringerMode == AudioManager.RINGER_MODE_SILENT) || (ringerMode == AudioManager.RINGER_MODE_VIBRATE)) {
            return;
        }

        if (mToneGenerator == null) {
            Log.w(LOG_TAG, "playTone: mToneGenerator == null, tone: " + tone);
            return;
        }

        mToneGenerator.startTone(tone, duration);
    }

    /*
     * The following lines are currently a reminder that we may implement status bar icons
     */

//    private boolean mShowingSpeakerphoneIcon;
//    private boolean mShowingMuteIcon;
//
//    mStatusBar = (StatusBarManager) context.getSystemService(Context.STATUS_BAR_SERVICE);
//
//    void notifySpeakerphone() {
//        if (!mShowingSpeakerphoneIcon) {
//            mStatusBar.setIcon("speakerphone", android.R.drawable.stat_sys_speakerphone, 0);
//            mShowingSpeakerphoneIcon = true;
//        }
//    }
//
//    void cancelSpeakerphone() {
//        if (mShowingSpeakerphoneIcon) {
//            mStatusBar.removeIcon("speakerphone");
//            mShowingSpeakerphoneIcon = false;
//        }
//    }
//
//    private void notifyMute() {
//        if (!mShowingMuteIcon) {
//            mStatusBar.setIcon("mute", android.R.drawable.stat_notify_call_mute, 0);
//            mShowingMuteIcon = true;
//        }
//    }
//
//    private void cancelMute() {
//        if (mShowingMuteIcon) {
//            mStatusBar.removeIcon("mute");
//            mShowingMuteIcon = false;
//        }
//    }

}