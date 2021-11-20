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
import android.graphics.Canvas;

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

import static com.fenger.package_manager.package_manager.utils.Base64Utils.encodeToBase64;
import static com.fenger.package_manager.package_manager.utils.DrawableUtils.getBitmapFromDrawable;

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



    } else if(call.method.equals("getPackageArchiveInfo")){
      String archivePath = call.argument("path");
      result.success(getPackageArchiveInfo(archivePath));



    } else if(call.method.equals("isInstall")){
      String packageName = call.argument("packageName");
      result.success(isInstall(packageName));



    } else if(call.method.equals("openApp")){
      String packageName = call.argument("packageName");
      PackageManager packageManager = this.activity.getPackageManager();
      Intent intent = packageManager.getLaunchIntentForPackage(packageName);
      if(intent==null){ //未安装
        result.success(false);
      }else { // 去打开
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
      if(Build.VERSION.SDK_INT>=24) { //Android 7.0及以上
        // 参数2 清单文件中provider节点里面的authorities ; 参数3  共享的文件,即apk包的file类

        Uri apkUri = FileProvider.getUriForFile(this.activity, this.activity.getPackageName()+".fenger.package_manager.fileprovider", file);
        //对目标应用临时授权该Uri所代表的文件
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
   * 获取已安装列表
   * @return
   */
  public List<Map> getInstalledList(Boolean isDetail){
    List<Map> list = new ArrayList<Map>();

    // 获取 PackageManager
    final PackageManager packageManager = this.activity.getPackageManager();

    //获取所有已安装程序的包信息
    List<PackageInfo> packages = packageManager.getInstalledPackages(0);
    for(int i=0; i<packages.size(); i++) {
      PackageInfo packageInfo = packages.get(i);
      Map info = new HashMap();

      // 获取详细再调用获取详细信息
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
   * 获取包详细信息
   * @param packageName
   * @return
   */
  public Map getPackageDetail(String packageName){
    Map info = new HashMap();

    // 获取 PackageManager
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
    try {
      Drawable appIcon = packageInfo.applicationInfo.loadIcon(this.activity.getPackageManager());
      String encodedImage = encodeToBase64(getBitmapFromDrawable(appIcon), Bitmap.CompressFormat.PNG, 100);
      info.put("appIcon", encodedImage);
    } catch (Exception e){
        e.printStackTrace();
        // info.put("appIcon", "no icon :(");
    }

    // info.put("appIcon", "data:image/png;base64,"+appIconBase64);

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


   public Map getPackageArchiveInfo(String path){
    Map info = new HashMap();

    // 获取 PackageManager
    final PackageManager packageManager = this.activity.getPackageManager();
    PackageInfo packageInfo = null;
    try {
      packageInfo = packageManager.getPackageArchiveInfo(path, 0);
      packageInfo.applicationInfo.publicSourceDir = path;
      packageInfo.applicationInfo.sourceDir = path;
    // } catch (PackageManager.NameNotFoundException e) {
    } catch (Exception e) {
      e.printStackTrace();
      //return null;
    }

    if (packageInfo == null) {
      return null;
    }

    info.put("packageName", packageInfo.packageName);
    info.put("appName", packageInfo.applicationInfo.loadLabel(this.activity.getPackageManager()).toString());
    info.put("versionName", packageInfo.versionName);
    info.put("versionCode", packageInfo.versionCode);

    info.put("firstInstallTime", packageInfo.firstInstallTime);
    info.put("lastUpdateTime", packageInfo.lastUpdateTime);

    String appIconBase64 = null;
    try {
      Drawable appIcon = packageInfo.applicationInfo.loadIcon(this.activity.getPackageManager());
      String encodedImage = encodeToBase64(getBitmapFromDrawable(appIcon), Bitmap.CompressFormat.PNG, 100);
      info.put("appIcon", encodedImage);
    } catch (Exception e){
        e.printStackTrace();
        // info.put("appIcon", "no icon :(");
    }

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
   * 获取包是否已安装
   * @return
   */
  public boolean isInstall(String packageName){

    // 获取 PackageManager
    final PackageManager packageManager = this.activity.getPackageManager();

    //获取所有已安装程序的包信息
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
   * 注册前后台切换检测
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
   * 取消注册前后台切换
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
