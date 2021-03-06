package com.fenger.package_manager.package_manager;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.util.Base64;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/** PackageManagerPlugin */
public class PackageManagerPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private MethodChannel channel;
  private Activity activity;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "package_manager");
    channel.setMethodCallHandler(this);
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    if (call.method.equals("getInstalledList")) {
      Boolean isDetail = call.argument("isDetail");
      result.success(getInstalledList(isDetail));



    } else if(call.method.equals("getPackageDetail")){
      String packageName = call.argument("packageName");
      result.success(getPackageDetail(packageName));



    } else if(call.method.equals("isInstall")){
      String packageName = call.argument("packageName");
      result.success(isInstall(packageName));



    } else if(call.method.equals("openApp")){
      String packageName = call.argument("packageName");
      PackageManager packageManager = this.activity.getPackageManager();
      Intent intent = packageManager.getLaunchIntentForPackage(packageName);
      if(intent==null){ //?????????
        result.success(false);
      }else { // ?????????
        this.activity.startActivity(intent);
        result.success(true);
      }



    } else if(call.method.equals("install")){
      String path = call.argument("path");
      File file = new File(path);
      if(file.exists() == false){
        result.success(false);
        return;
      }

      Intent intent = new Intent(Intent.ACTION_VIEW);
      intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      if(Build.VERSION.SDK_INT>=24) { //Android 7.0?????????
        // ??????2 ???????????????provider???????????????authorities ; ??????3  ???????????????,???apk??????file???

        Uri apkUri = FileProvider.getUriForFile(this.activity, this.activity.getPackageName()+".fenger.package_manager.fileprovider", file);
        //??????????????????????????????Uri??????????????????
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
      }else{
        intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
      }
      this.activity.startActivity(intent);
      result.success(true);



    } else if(call.method.equals("unInstall")){
      String packageName = call.argument("packageName");
      Uri uri = Uri.fromParts("package", packageName, null);
      Intent intent = new Intent(Intent.ACTION_DELETE, uri);
      this.activity.startActivity(intent);
      result.success(true);
      
    } else {
      result.notImplemented();
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }


  /**
   * ?????????????????????
   * @return
   */
  public List<Map> getInstalledList(Boolean isDetail){
    List<Map> list = new ArrayList<Map>();

    // ?????? PackageManager
    final PackageManager packageManager = this.activity.getPackageManager();

    //???????????????????????????????????????
    List<PackageInfo> packages = packageManager.getInstalledPackages(0);
    for(int i=0; i<packages.size(); i++) {
      PackageInfo packageInfo = packages.get(i);
      Map info = new HashMap();

      // ???????????????????????????????????????
      if(isDetail){
        info = this.getPackageDetail(packageInfo.packageName);
      }else{
        info.put("packageName", packageInfo.packageName);
        info.put("appName", packageInfo.applicationInfo.loadLabel(this.activity.getPackageManager()).toString());
        info.put("versionName", packageInfo.versionName);
        info.put("versionCode", packageInfo.versionCode);
        info.put("firstInstallTime", packageInfo.firstInstallTime);
        info.put("lastUpdateTime", packageInfo.lastUpdateTime);
      }

      list.add(info);
    }
    return list;
  }

  /**
   * ?????????????????????
   * @param packageName
   * @return
   */
  public Map getPackageDetail(String packageName){
    Map info = new HashMap();

    // ?????? PackageManager
    final PackageManager packageManager = this.activity.getPackageManager();
    PackageInfo packageInfo = null;
    try {
      packageInfo = packageManager.getPackageInfo(packageName, 0);
    } catch (PackageManager.NameNotFoundException e) {
      e.printStackTrace();
      return null;
    }

    info.put("packageName", packageInfo.packageName);
    info.put("appName", packageInfo.applicationInfo.loadLabel(this.activity.getPackageManager()).toString());
    info.put("versionName", packageInfo.versionName);
    info.put("versionCode", packageInfo.versionCode);

    info.put("firstInstallTime", packageInfo.firstInstallTime);
    info.put("lastUpdateTime", packageInfo.lastUpdateTime);

    String appIconBase64 = null;
    try{
      Drawable appIcon = packageInfo.applicationInfo.loadIcon(this.activity.getPackageManager());
      BitmapDrawable btDrawable = (BitmapDrawable) appIcon;
      Bitmap bitmap = btDrawable.getBitmap();

      //??????bitmap??????byte[]
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      bitmap.compress(Bitmap.CompressFormat.PNG,100,baos);
      byte[] bytes = baos.toByteArray();
      //???byte[]??????base64
      appIconBase64 = Base64.encodeToString(bytes,Base64.DEFAULT);
    }catch (Exception e){e.printStackTrace();}


    info.put("appIcon", "data:image/png;base64,"+appIconBase64);

    String publicSourceDir = packageInfo.applicationInfo.publicSourceDir;
    int size = Integer.valueOf((int)new File(publicSourceDir).length());
    info.put("appSize", size);

    if((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM)==0) {
      info.put("isSystemApp", false);
    }else{
      info.put("isSystemApp", true);
    }
    return info;
  }


  /**
   * ????????????????????????
   * @return
   */
  public boolean isInstall(String packageName){

    // ?????? PackageManager
    final PackageManager packageManager = this.activity.getPackageManager();

    //???????????????????????????????????????
    List<PackageInfo> packages = packageManager.getInstalledPackages(0);
    for(int i=0; i<packages.size(); i++) {
      PackageInfo packageInfo = packages.get(i);
      if(packageInfo.packageName.equals(packageName)){
        return true;
      }
    }
    return false;
  }


  AppFrontBackHelper appFrontBackHelper;
  Application appFrontBackHelperApplication;

  /**
   * ???????????????????????????
   * @param activity
   */
  public void registerAppFront(Activity activity){
    if(appFrontBackHelper!=null){
      return;
    }

    appFrontBackHelper = new AppFrontBackHelper();
    appFrontBackHelperApplication = activity.getApplication();
    appFrontBackHelper.register(appFrontBackHelperApplication, new AppFrontBackHelper.OnAppStatusListener() {
      @Override
      public void onFront() {
        PackageManagerPlugin.this.channel.invokeMethod("dartApplife", new HashMap<String, Object>(){{
          put("life", "onFront");
        }});
      }

      @Override
      public void onBack() {
        PackageManagerPlugin.this.channel.invokeMethod("dartApplife", new HashMap<String, Object>(){{
          put("life", "onBack");
        }});
      }
    });
  }

  /**
   * ???????????????????????????
   */
  public void unRegisterAppFront(){
    if(appFrontBackHelper != null && appFrontBackHelperApplication != null){
      appFrontBackHelper.unRegister(appFrontBackHelperApplication);
      appFrontBackHelper = null;
      appFrontBackHelperApplication = null;
    }
  }

  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
    activity = binding.getActivity();
    this.registerAppFront(activity);
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {

  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {

  }

  @Override
  public void onDetachedFromActivity() {
    this.unRegisterAppFront();
  }
}
