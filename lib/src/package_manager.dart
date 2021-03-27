import 'package:flutter/services.dart';

/// 包管理器
/// Created by 枫儿 on 2021/3/25.
/// @email：hsnndly@163.com
class PackageManager {
  static PackageManager _packageManager;
  static PackageManager getInstance() {
    if (_packageManager == null) {
      _packageManager = PackageManager();
    }
    return _packageManager;
  }

  MethodChannel _channel = MethodChannel("package_manager");

  /// @Desc  : 获取已安装应用列表
  /// detail 参数：true=获取详情、false=只获取包名等不耗时信息，为false会大幅度提升速度
  /// @author: 枫儿
  Future<List> getInstalledList({bool isDetail = false}) async{
    List list = await _channel.invokeMethod("getInstalledList", {"isDetail": isDetail});
    return list;
  }

  /// @Desc  : 根据单个包名，获取详情
  /// @author: 枫儿
  Future<Map> getPackageDetail(String packageName) async{
    return await _channel.invokeMapMethod("getPackageDetail", {"packageName": packageName});
  }

  /// @Desc  : 安装应用
  /// path路径建议放在：/data/data/com.xxx.your_package/file/download目录下
  /// 相当于：getApplicationSupportDirectory().path + "/downloads/"
  /// @author: 枫儿
  Future<bool> install(String path) async{
    await _channel.invokeMethod("install", {"path": path});
  }

  /// @Desc  : 卸载应用
  /// @author: 枫儿
  Future<bool> unInstall(String packageName) async{
    await _channel.invokeMethod("unInstall", {"packageName": packageName});
  }

}
