package flutter.overlay.window.flutter_overlay_window;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationManagerCompat;

import java.util.List;
import java.util.Map;

import io.flutter.FlutterInjector;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.embedding.engine.FlutterEngineCache;
import io.flutter.embedding.engine.FlutterEngineGroup;
import io.flutter.embedding.engine.dart.DartExecutor;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BasicMessageChannel;
import io.flutter.plugin.common.JSONMessageCodec;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;

public class FlutterOverlayWindowPlugin implements
        FlutterPlugin, ActivityAware, BasicMessageChannel.MessageHandler, MethodCallHandler,
        PluginRegistry.ActivityResultListener {

    private MethodChannel channel;
    private Context context;
    private Activity mActivity;
    private BasicMessageChannel<Object> messenger;
    private Result pendingResult;
    final int REQUEST_CODE_FOR_OVERLAY_PERMISSION = 1248;

    private String lastEngineId;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        this.context = flutterPluginBinding.getApplicationContext();
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), OverlayConstants.CHANNEL_TAG);
        channel.setMethodCallHandler(this);

        messenger = new BasicMessageChannel(flutterPluginBinding.getBinaryMessenger(), OverlayConstants.MESSENGER_TAG,
                JSONMessageCodec.INSTANCE);
        messenger.setMessageHandler(this);

        WindowSetup.messenger = messenger;
        WindowSetup.messenger.setMessageHandler(this);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        pendingResult = result;
        switch (call.method) {
            case "checkPermission":
                result.success(checkOverlayPermission());
                break;
            case "requestPermission":
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                    intent.setData(Uri.parse("package:" + mActivity.getPackageName()));
                    mActivity.startActivityForResult(intent, REQUEST_CODE_FOR_OVERLAY_PERMISSION);
                } else {
                    result.success(true);
                }
                break;
            case "showOverlay":
                if (!checkOverlayPermission()) {
                    result.error("PERMISSION", "overlay permission is not enabled", null);
                    return;
                }

                String engineId = call.argument("engineId");
                String entryPoint = call.argument("entryPoint");
                List<String> args = call.argument("args");

                lastEngineId = engineId;
                createDartEntry(engineId, entryPoint, args);


                Integer height = call.argument("height");
                Integer width = call.argument("width");
                String alignment = call.argument("alignment");
                String flag = call.argument("flag");
                String overlayTitle = call.argument("overlayTitle");
                String overlayContent = call.argument("overlayContent");
                String notificationVisibility = call.argument("notificationVisibility");
                boolean enableDrag = call.argument("enableDrag");
                String positionGravity = call.argument("positionGravity");
                Map<String, Integer> startPosition = call.argument("startPosition");
                int startX = startPosition != null ? startPosition.getOrDefault("x", OverlayConstants.DEFAULT_XY) : OverlayConstants.DEFAULT_XY;
                int startY = startPosition != null ? startPosition.getOrDefault("y", OverlayConstants.DEFAULT_XY) : OverlayConstants.DEFAULT_XY;


                WindowSetup.width = width != null ? width : -1;
                WindowSetup.height = height != null ? height : -1;
                WindowSetup.enableDrag = enableDrag;
                WindowSetup.setGravityFromAlignment(alignment != null ? alignment : "center");
                WindowSetup.setFlag(flag != null ? flag : "flagNotFocusable");
                WindowSetup.overlayTitle = overlayTitle;
                WindowSetup.overlayContent = overlayContent == null ? "" : overlayContent;
                WindowSetup.positionGravity = positionGravity;
                WindowSetup.setNotificationVisibility(notificationVisibility);

                final Intent intent = new Intent(context, OverlayService.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra("startX", startX);
                intent.putExtra("startY", startY);
                intent.putExtra("engineId", engineId);
                context.startService(intent);
                result.success(null);
                break;

            case "closeOverlay":
                final Intent i = new Intent(context, OverlayService.class);
                context.stopService(i);

                destroyDartEntry(call.argument("engineId"));

                result.success(true);
                return;

            case "isOverlayActive":
                result.success(isOverlayActive(call.argument("engineId")));
                return;

            case "moveOverlay":
                int x = call.argument("x");
                int y = call.argument("y");
                result.success(OverlayService.moveOverlay(x, y));
                break;

            case "getOverlayPosition":
                result.success(OverlayService.getCurrentPosition());
                break;

            default:
                result.notImplemented();
                break;
        }

    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
        WindowSetup.messenger.setMessageHandler(null);
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        mActivity = binding.getActivity();
    }

    public void createDartEntry(String engineId, String entryPoint, List<String> args){
       if(!isOverlayActive(engineId)){
           FlutterEngineGroup enn = new FlutterEngineGroup(context);

           DartExecutor.DartEntrypoint dEntry = new DartExecutor.DartEntrypoint(
                   FlutterInjector.instance().flutterLoader().findAppBundlePath(),
                   entryPoint
           );

           FlutterEngineGroup.Options options = new FlutterEngineGroup.Options(context);
           options.setDartEntrypoint(dEntry);
           options.setDartEntrypointArgs(args);
           FlutterEngine engine = enn.createAndRunEngine(options);

           FlutterEngineCache.getInstance().put(engineId, engine);
       }
    }

    public void destroyDartEntry(String engineId){
        FlutterEngineCache.getInstance().remove(engineId);
    }

    public boolean isOverlayActive(String engineId){
        return FlutterEngineCache.getInstance().contains(engineId);
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        this.mActivity = binding.getActivity();
    }

    @Override
    public void onDetachedFromActivity() {
    }

    @Override
    public void onMessage(@Nullable Object message, @NonNull BasicMessageChannel.Reply reply) {
       BasicMessageChannel overlayMessageChannel = new BasicMessageChannel(
               FlutterEngineCache.getInstance().get(lastEngineId).getDartExecutor(),
               OverlayConstants.MESSENGER_TAG, JSONMessageCodec.INSTANCE
       );
       overlayMessageChannel.send(message, reply);
    }

    private boolean checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        }
        return true;
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_FOR_OVERLAY_PERMISSION) {
            pendingResult.success(checkOverlayPermission());
            return true;
        }
        return false;
    }

}
