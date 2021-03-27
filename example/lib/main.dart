import 'dart:convert';

import 'package:flutter/material.dart';
import 'dart:async';

import 'package:logger/logger.dart';
import 'package:logger_flutter/logger_flutter.dart';
import 'package:package_manager/package_manager.dart';
import 'package:package_manager_example/util.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {

  var logger = Logger(printer: PrettyPrinter(printTime: true));

  @override
  void initState() {
    super.initState();
    LogConsole.init();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: SingleChildScrollView(
            child: Center(
              child: Column(
                children: [

                  Card(
                    child: Column(
                      children: [
                        ListTile(title: Text("包管理器"), subtitle: Text("包相关操作"),),
                        Container(
                          // width: double.infinity,
                          child: Wrap(
                            crossAxisAlignment: WrapCrossAlignment.start,
                            spacing: 10,
                            children: [

                              ElevatedButton.icon(
                                  onPressed: () async {

                                    int last = new DateTime.now().millisecondsSinceEpoch;
                                    /// 获取所有应用(含详情)
                                    List list =  await PackageManager.getInstance().getInstalledList();
                                    int diff = new DateTime.now().millisecondsSinceEpoch - last;
                                    logger.i("获取所有包名: 耗时：${diff}，数据：${list}");
                                  },
                                  icon: Icon(Icons.home_filled),
                                  label: Text("获取所有应用(含详情) 数据量过大，慎点")),

                              ElevatedButton.icon(
                                  onPressed: () async {
                                    int last = new DateTime.now().millisecondsSinceEpoch;
                                    /// 获取所有应用(仅包名)
                                    List list =  await PackageManager.getInstance().getInstalledList(isDetail: false);
                                    int diff = new DateTime.now().millisecondsSinceEpoch - last;
                                    logger.i("获取所有包名: 耗时：${diff}，数据：${list}");
                                  },
                                  icon: Icon(Icons.home_filled),
                                  label: Text("获取所有应用(仅包名)")),

                              ElevatedButton.icon(
                                  onPressed: () async {
                                    Map ret =  await PackageManager.getInstance().getPackageDetail("com.tencent.mm");
                                    logger.i("获取单个应用详情: 结果：${ret}");
                                  },
                                  icon: Icon(Icons.home_filled),
                                  label: Text("获取单个应用详情")),

                              ElevatedButton.icon(
                                  onPressed: () async {
                                    bool ret =  await PackageManager.getInstance().isInstall("uni.UNI1E28B6B");
                                    logger.i("判断应用是否存在: 结果：${ret}");
                                  },
                                  icon: Icon(Icons.home_filled),
                                  label: Text("判断应用是否存在")),

                              ElevatedButton.icon(
                                  onPressed: () async {
                                    bool ret =  await PackageManager.getInstance().openApp("uni.UNI1E28B6B");
                                    logger.i("打开应用: 结果：${ret}");
                                  },
                                  icon: Icon(Icons.home_filled),
                                  label: Text("打开应用")),

                              ElevatedButton.icon(
                                  onPressed: () async {
                                    String filePath =  await Util.downloadFile("https://ossdafuhao.oss-cn-shanghai.aliyuncs.com/ZM%2Fandroid_debug.apk");
                                    bool ret =  await PackageManager.getInstance().install(filePath);
                                    logger.i("安装应用: 结果：${ret}");
                                  },
                                  icon: Icon(Icons.home_filled),
                                  label: Text("安装应用")),


                              ElevatedButton.icon(
                                  onPressed: () async {
                                    bool ret =  await PackageManager.getInstance().unInstall("uni.UNI1E28B6B");
                                    logger.i("卸载应用: 结果：${ret}");
                                  },
                                  icon: Icon(Icons.home_filled),
                                  label: Text("卸载应用")),

                            ],
                          ),
                        ),
                      ],
                    ),
                  ),


                  Container(
                    margin: EdgeInsets.only(top: 20),
                    child: LogConsoleOnShake(
                      dark: true,
                      child: Center(
                        child: Text("摇一摇手机，查看控制台日志", style: TextStyle(color: Colors.red, fontWeight: FontWeight.w700, fontSize: 15),),
                      ),
                    ),
                  ),

                ],
              ),
            )
        ),
      ),
    );
  }
}
