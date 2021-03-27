import 'dart:io' show Directory, File, Platform;
import 'dart:convert';
import 'package:dio/dio.dart';
import 'package:convert/convert.dart';
import 'package:crypto/crypto.dart';
import 'package:flutter/material.dart';
import 'package:path_provider/path_provider.dart';

/// 综合工具类
/// Created by 枫儿 on 2021/1/14.
/// @email：hsnndly@163.com
class Util{

  /// @Desc  : 创建MD5
  /// @author: 枫儿
  static String generateMD5(String data) {
    var content = new Utf8Encoder().convert(data);
    var digest = md5.convert(content);
    return hex.encode(digest.bytes);
  }

  /// @Desc  : 下载文件
  /// @author: 枫儿
  static Future<String> downloadFile(String url, {ValueChanged<double> onProgress, bool cache = true}) async{
    String fileName = getURLFileName(url);
    assert(fileName != null, "Util.downloadFile：获取路径文件名为空 ${url}");

    Directory docDir = await getApplicationSupportDirectory();
    String baseDir = docDir.path + "/downloads/";
    String urlMD5 = generateMD5(url);
    String suffix = getURLFileSuffix(url);
    String savePath = "${baseDir}${urlMD5}.${suffix}";
    String tmpPath = "${savePath}.download";

    // cache为true 并且路径存在直接返回
    if(cache && await File(savePath).exists()){
      return savePath; 
    }

    Dio dio = Dio();
    Response response = await dio.download(url, tmpPath, onReceiveProgress: (int count, int total){
      double progress = (count / total) * 100;
      if(onProgress != null){
        onProgress(progress);
      }
    });

    /// 临时路径拷贝到保存路径
    File tmpPathFile = File(tmpPath);
    tmpPathFile.copySync(savePath);

    return savePath;
  }

  /// @Desc  : 截取url里的文件名
  /// @author: 枫儿
  static getURLFileName(String url){
    if(url == null || url == ''){
      return null;
    }
    url = url.split("?")[0];

    List<String> uArr = url.split("/");
    if(uArr.isEmpty){
      return null;
    }

    String fileName = uArr[uArr.length - 1];
    return fileName;
  }

  static getURLFileSuffix(String url){
    String fileName = getURLFileName(url);
    List split = fileName.split(".");
    String suffix = split[split.length-1];
    return suffix;
  }


  /// @Desc  : Map转为query字符串
  /// @author: 枫儿
  static String mapToQuery(Map map){
    List ret = [];
    map.forEach((key, value) {
      String p = (key.toString().trim()) + "=" + (value==null? "" : value.toString().trim());
      ret.add(p);
    });
    return ret.join("&");
  }

  /// @Desc  : query字符串转为Map
  /// @author: 枫儿
  static Map queryToMap(String query){
    List<String> split = query.split("&");
    Map ret = {};

    for(String str in split){
      List line = str.split("=");
      ret[line[0]] = line[1];
    }
    return ret;
  }


}