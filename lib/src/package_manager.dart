import 'dart:async';

import 'package:flutter/services.dart';

/// 包管理器
/// Created by 枫儿 on 2021/3/25.
/// @email：hsnndly@163.com
class PackageManager {
  static PackageManager? _packageManager;
  static PackageManager getInstance() {
    if (_packageManager == null) {
      _packageManager = PackageManager();
      _packageManager!._initDartCall();
    }
    return _packageManager!;
  }

  MethodChannel _channel = MethodChannel("package_manager");

  /// App前后台状态监听
  StreamController<AppLife> _dartAppLifeController = new StreamController.broadcast();
  Stream<AppLife> get appLifeHandler => _dartAppLifeController.stream;

  void _initDartCall() {
    _channel.setMethodCallHandler((MethodCall call) async {
      if (call.method == "dartApplife") {
        Map args = call.arguments;
        if (args['life'] == "onFront") {
          _dartAppLifeController.add(AppLife.onFront);
        } else if (args['life'] == "onBack") {
          _dartAppLifeController.add(AppLife.onBack);
        }
      }
      return null;
    });
  }

  /// @Desc  : 获取已安装应用列表
  /// detail 参数：true=获取详情、false=只获取包名等不耗时信息，为false会大幅度提升速度
  /// @author: 枫儿
  Future<List> getInstalledList({bool isDetail = false}) async {
    List list = await _channel.invokeMethod("getInstalledList", {"isDetail": isDetail});
    return list;
  }

  /// @Desc  : 根据单个包名，获取详情
  /// @author: 枫儿
  Future<Map?> getPackageDetail(String packageName) async {
    return await _channel.invokeMapMethod("getPackageDetail", {"packageName": packageName});
  }

  /// @Desc  : 根据包名判断是否安装了该App
  /// @author: 枫儿
  Future<bool> isInstall(String packageName) async {
    return await _channel.invokeMethod("isInstall", {"packageName": packageName});
  }

  /// @Desc  : 根据包名打开App
  /// @author: 枫儿
  Future<bool> openApp(String packageName) async {
    return await _channel.invokeMethod("openApp", {"packageName": packageName});
  }

  /// @Desc  : 安装应用，传入packageName才能监听到安装完成
  /// path路径建议放在：/data/data/com.xxx.your_package/file/download目录下
  /// 相当于：getApplicationSupportDirectory().path + "/downloads/"
  /// @author: 枫儿
  Future<bool> install(String path, {String? packageName}) async {
    bool ret = await _channel.invokeMethod("install", {"path": path});
    if (ret && packageName != null) {
      return await _watchAppInstallStatus(packageName, installType: true);
    } else {
      return ret;
    }
  }

  /// @Desc  : 卸载应用
  /// @author: 枫儿
  Future<bool> unInstall(String packageName) async {
    bool ret = await _channel.invokeMethod("unInstall", {"packageName": packageName});
    if (ret) {
      return await _watchAppInstallStatus(packageName, installType: false);
    } else {
      return ret;
    }
  }

  /// @Desc  : 监听APP安装状态
  /// [packageName] 监听的包名
  /// [installType] bool: true = 监听安装、false = 监听卸载
  /// [timeout] 超时时间
  /// @author: 枫儿
  Future<bool> _watchAppInstallStatus(String packageName,
      {bool installType = true, Duration timeout = const Duration(seconds: 30)}) async {
    Completer completer = Completer();
    int lastTimestamp = DateTime.now().millisecondsSinceEpoch + timeout.inMilliseconds;
    Timer timer = Timer.periodic(Duration(milliseconds: 300), (timer) async {
      bool isInstall = await this.isInstall(packageName);

      /// 监听安装类型
      if (installType == true && isInstall == true) {
        timer.cancel();
        completer.complete(true);
      }

      /// 监听卸载类型
      if (installType == false && isInstall == false) {
        timer.cancel();
        completer.complete(true);
      }

      /// 超时
      if (DateTime.now().millisecondsSinceEpoch >= lastTimestamp) {
        timer.cancel();
        completer.complete(false);
      }
    });

    return await completer.future;
  }
}

enum AppLife { onFront, onBack }
