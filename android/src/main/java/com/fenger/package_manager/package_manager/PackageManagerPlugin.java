package com.fenger.package_manager.package_manager;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
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



    } else if(call.method.equals("install")){
      String path = call.argument("path");
      File file = new File(path);
      if(file.exists() == false){
        result.success(false);
        return;
      }
      Intent intent = new Intent();
      intent.setAction(Intent.ACTION_VIEW);
      intent.setDataAndType(Uri.fromFile(file),
              "application/vnd.android.package-archive");
      this.activity.startActivity(intent);
      result.success(true);



    } else if(call.method.equals("unInstall")){
      String packageName = call.argument("packageName");
      Uri packageURI = Uri.parse("package:".concat(packageName));
      Intent intent = new Intent(Intent.ACTION_DELETE);
      intent.setData(packageURI);
      this.activity.startActivity(intent);


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
    try{
      Drawable appIcon = packageInfo.applicationInfo.loadIcon(this.activity.getPackageManager());
      BitmapDrawable btDrawable = (BitmapDrawable) appIcon;
      Bitmap bitmap = btDrawable.getBitmap();

      //先将bitmap转为byte[]
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      bitmap.compress(Bitmap.CompressFormat.PNG,100,baos);
      byte[] bytes = baos.toByteArray();
      //将byte[]转为base64
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


  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
    activity = binding.getActivity();
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {

  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {

  }

  @Override
  public void onDetachedFromActivity() {

  }
}
