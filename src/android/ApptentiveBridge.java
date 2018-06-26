package com.apptentive.cordova;

import android.app.Activity;
import android.app.Application;

import com.apptentive.android.sdk.Apptentive;
import com.apptentive.android.sdk.Apptentive.BooleanCallback;
import com.apptentive.android.sdk.ApptentiveInternal;
import com.apptentive.android.sdk.ApptentiveLog;
import com.apptentive.android.sdk.module.messagecenter.UnreadMessagesListener;
import com.apptentive.android.sdk.module.rating.impl.AmazonAppstoreRatingProvider;
import com.apptentive.android.sdk.module.survey.OnSurveyFinishedListener;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import java.lang.reflect.Field;
import java.util.Map;

public class ApptentiveBridge extends CordovaPlugin {

    private static final String ACTION_DEVICE_READY = "deviceReady";
    private static final String ACTION_ADD_CUSTOM_DEVICE_DATA = "addCustomDeviceData";
    private static final String ACTION_ADD_CUSTOM_PERSON_DATA = "addCustomPersonData";
    private static final String ACTION_ENGAGE = "engage";
    private static final String ACTION_GET_UNREAD_MESSAGE_COUNT = "getUnreadMessageCount";
    private static final String ACTION_PUT_RATING_PROVIDER_ARG = "putRatingProviderArg";
    private static final String ACTION_REMOVE_CUSTOM_DEVICE_DATA = "removeCustomDeviceData";
    private static final String ACTION_REMOVE_CUSTOM_PERSON_DATA = "removeCustomPersonData";
    private static final String ACTION_SEND_ATTACHMENT_FILE_URI = "sendAttachmentFileUri";
    private static final String ACTION_SEND_ATTACHMENT_FILE = "sendAttachmentFile";
    private static final String ACTION_SEND_ATTACHMENT_TEXT = "sendAttachmentText";
    private static final String ACTION_GET_PERSON_EMAIL = "getPersonEmail";
    private static final String ACTION_SET_PERSON_EMAIL = "setPersonEmail";
    private static final String ACTION_GET_PERSON_NAME = "getPersonName";
    private static final String ACTION_SET_PERSON_NAME = "setPersonName";
    private static final String ACTION_ADD_UNREAD_MESSAGES_LISTENER = "addUnreadMessagesListener";
    private static final String ACTION_SET_ON_SURVEY_FINISHED_LISTENER = "setOnSurveyFinishedListener";
    private static final String ACTION_SET_RATING_PROVIDER = "setRatingProvider";
    private static final String ACTION_SHOW_MESSAGE_CENTER = "showMessageCenter";
    private static final String ACTION_CAN_SHOW_MESSAGE_CENTER = "canShowMessageCenter";
    private static final String ACTION_CAN_SHOW_INTERACTION = "canShowInteraction";
    private static final String ACTION_LOGIN = "login";
    private static final String ACTION_LOGOUT = "logout";

    /**
     * Constructor.
     */
    public ApptentiveBridge() {
    }

    UnreadMessagesListener listener = null;

    public boolean execute(final String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        ApptentiveLog.v("Executing action: %s", action);

        if (action.equals(ACTION_DEVICE_READY)) {
            final Activity currentActivity = cordova.getActivity();
            if (currentActivity != null && !ApptentiveInternal.isApptentiveRegistered()) {
                currentActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Apptentive.register(currentActivity.getApplication());
                        Application.ActivityLifecycleCallbacks callbacks = getActivityLifecycleCallbacks();
                        if (callbacks != null) {
                            callbacks.onActivityCreated(currentActivity, null);
                            callbacks.onActivityStarted(currentActivity);
                            callbacks.onActivityResumed(currentActivity);
                        } else {
                            ApptentiveLog.e("Unable to properly initialize Apptentive");
                        }
                    }
                });
            }
            callbackContext.success();
            return true;

        } else if (action.equals(ACTION_CAN_SHOW_MESSAGE_CENTER)) {
            Apptentive.canShowMessageCenter(new BooleanCallback() {
                @Override
                public void onFinish(boolean canShowMessageCenter) {
                    PluginResult result = new PluginResult(PluginResult.Status.OK, canShowMessageCenter);
                    callbackContext.sendPluginResult(result);
                }
            });
            return true;

        } else if (action.equals(ACTION_SHOW_MESSAGE_CENTER)) {
            // TODO: pass boolean callback and use returned value
            if (args.length() > 0) {
                Map<String, Object> config = JsonHelper.toMap(args.getJSONObject(0));
                Apptentive.showMessageCenter(cordova.getActivity(), config);
            } else {
                Apptentive.showMessageCenter(cordova.getActivity());
            }
            callbackContext.success();
            return true;

        } else if (action.equals(ACTION_ADD_CUSTOM_DEVICE_DATA)) {
            String key = args.getString(0);
            Object value = args.get(1);
            if (value instanceof String) {
                Apptentive.addCustomDeviceData(key, (String) value);
            } else if (value instanceof Number) {
                Apptentive.addCustomDeviceData(key, (Number) value);
            } else if (value instanceof Boolean) {
                Apptentive.addCustomDeviceData(key, (Boolean) value);
            } else {
                callbackContext.error("Custom Device Data Type not supported: " + ((value != null) ? value.getClass() : "NULL"));
                return false;
            }

            callbackContext.success();
            return true;

        } else if (action.equals(ACTION_ADD_CUSTOM_PERSON_DATA)) {
            String key = args.getString(0);
            Object value = args.get(1);
            if (value instanceof String) {
                Apptentive.addCustomPersonData(key, (String) value);
            } else if (value instanceof Number) {
                Apptentive.addCustomPersonData(key, (Number) value);
            } else if (value instanceof Boolean) {
                Apptentive.addCustomPersonData(key, (Boolean) value);
            } else {
                callbackContext.error("Custom Person Data Type not supported: " + ((value != null) ? value.getClass() : "NULL"));
                return false;
            }

            callbackContext.success();
            return true;

        } else if (action.equals(ACTION_CAN_SHOW_INTERACTION)) {
            final String eventId = args.getString(0);
            Apptentive.queryCanShowInteraction(eventId, new BooleanCallback() {
                @Override
                public void onFinish(boolean canShowInteraction) {
                    PluginResult result = new PluginResult(PluginResult.Status.OK, canShowInteraction);
                    callbackContext.sendPluginResult(result);
                }
            });
            return true;

        } else if (action.equals(ACTION_ENGAGE)) {
            final String eventId = args.getString(0);
            BooleanCallback callback = new BooleanCallback() {
                @Override
                public void onFinish(boolean shown) {
                    PluginResult result = new PluginResult(PluginResult.Status.OK, shown);
                    callbackContext.sendPluginResult(result);
                }
            };

            if (args.length() > 1) {
                Map<String, Object> customData = JsonHelper.toMap(args.getJSONObject(1));
                Apptentive.engage(cordova.getActivity(), eventId, callback, customData);
            } else {
                Apptentive.engage(cordova.getActivity(), eventId, callback);
            }
            return true;

        } else if (action.equals(ACTION_GET_UNREAD_MESSAGE_COUNT)) {
            int unreadMessageCount = Apptentive.getUnreadMessageCount();
            PluginResult result = new PluginResult(PluginResult.Status.OK, unreadMessageCount);
            callbackContext.sendPluginResult(result);
            return true;

        } else if (action.equals(ACTION_PUT_RATING_PROVIDER_ARG)) {
            String key = args.getString(0);
            String value = args.getString(1);
            Apptentive.putRatingProviderArg(key, value);
            callbackContext.success();
            return true;

        } else if (action.equals(ACTION_REMOVE_CUSTOM_DEVICE_DATA)) {
            String key = args.getString(0);
            Apptentive.removeCustomDeviceData(key);
            callbackContext.success();
            return true;

        } else if (action.equals(ACTION_REMOVE_CUSTOM_PERSON_DATA)) {
            String key = args.getString(0);
            Apptentive.removeCustomPersonData(key);
            callbackContext.success();
            return true;

        } else if (action.equals(ACTION_SEND_ATTACHMENT_FILE_URI)) {
            String uri = args.getString(0);
            Apptentive.sendAttachmentFile(uri);
            return true;

        } else if (action.equals(ACTION_SEND_ATTACHMENT_FILE)) {
            byte[] content = args.getString(0).getBytes();
            String mimeType = args.getString(1);
            Apptentive.sendAttachmentFile(content, mimeType);
            return true;

        } else if (action.equals(ACTION_SEND_ATTACHMENT_TEXT)) {
            String text = args.getString(0);
            Apptentive.sendAttachmentText(text);
            callbackContext.success();
            return true;

        } else if (action.equals(ACTION_GET_PERSON_EMAIL)) {
            String email = Apptentive.getPersonEmail();
            PluginResult result = new PluginResult(PluginResult.Status.OK, email);
            callbackContext.sendPluginResult(result);
            return true;

        } else if (action.equals(ACTION_SET_PERSON_EMAIL)) {
            String email = args.getString(0);
            Apptentive.setPersonEmail(email);
            callbackContext.success();
            return true;

        } else if (action.equals(ACTION_GET_PERSON_NAME)) {
            String name = Apptentive.getPersonName();
            PluginResult result = new PluginResult(PluginResult.Status.OK, name);
            callbackContext.sendPluginResult(result);
            return true;

        } else if (action.equals(ACTION_SET_PERSON_NAME)) {
            String name = args.getString(0);
            Apptentive.setPersonName(name);
            callbackContext.success();
            return true;

        } else if (action.equals(ACTION_SET_RATING_PROVIDER)) {
            String providerName = args.getString(0);
            if (providerName.equals("amazon")) {
                Apptentive.setRatingProvider(new AmazonAppstoreRatingProvider());
            }
            callbackContext.success();
            return true;

        } else if (action.equals(ACTION_ADD_UNREAD_MESSAGES_LISTENER)) {
            ApptentiveLog.i("Adding message listener.");
            listener = new UnreadMessagesListener() {
                public void onUnreadMessageCountChanged(int unreadMessages) {
                    ApptentiveLog.i("Unread messages: %d", unreadMessages);
                    PluginResult result = new PluginResult(PluginResult.Status.OK, unreadMessages);
                    result.setKeepCallback(true);
                    callbackContext.sendPluginResult(result);
                }
            };
            Apptentive.addUnreadMessagesListener(listener);
            return true;

        } else if (action.equals(ACTION_SET_ON_SURVEY_FINISHED_LISTENER)) {
            OnSurveyFinishedListener listener = new OnSurveyFinishedListener() {
                public void onSurveyFinished(boolean finished) {
                    int completed = finished ? 1 : 0;
                    PluginResult result = new PluginResult(PluginResult.Status.OK, completed);
                    result.setKeepCallback(true);
                    callbackContext.sendPluginResult(result);
                }
            };
            Apptentive.setOnSurveyFinishedListener(listener);
            return true;
        } else if (action.equals(ACTION_LOGIN)) {
            String token = args.getString(0);
            Apptentive.login(token, new Apptentive.LoginCallback() {
                @Override
                public void onLoginFinish() {
                    callbackContext.success();
                }

                @Override
                public void onLoginFail(String errorMessage) {
                    callbackContext.error(errorMessage);
                }
            });
            return true;
        } else if (action.equals(ACTION_LOGOUT)) {
            Apptentive.logout();
            callbackContext.success();
            return true;
        }

        ApptentiveLog.w("Unhandled action in ApptentiveBridge: %s", action);

        callbackContext.error("Unhandled action in ApptentiveBridge: " + action);
        return false;
    }

    private static Application.ActivityLifecycleCallbacks getActivityLifecycleCallbacks()  {
        try {
            Class<?> holderClass = Class.forName("com.apptentive.android.sdk.lifecycle.ApptentiveActivityLifecycleCallbacks$Holder");
            Field instance = holderClass.getDeclaredField("INSTANCE");
            instance.setAccessible(true);
            return (Application.ActivityLifecycleCallbacks) instance.get(null);
        } catch (Exception e) {
            ApptentiveLog.e(e, "Exception while resolving ApptentiveLifecycleCallbacks");
        }
        return null;
    }
}
