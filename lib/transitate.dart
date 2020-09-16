import 'package:flutter/cupertino.dart';






class NavigateTo{

  BuildContext context;
  NavigateTo(this.context);

  dynamic page(Widget page, {bool replace=false}) async {

    if(replace){
      return await Navigator.pushReplacement(context, CupertinoPageRoute(builder: (context) => page));

    }
    return await Navigator.push(context, CupertinoPageRoute(builder: (context) => page));
  }

  dynamic pageClear(Widget page){

    Navigator.pushAndRemoveUntil(context, CupertinoPageRoute(builder: (context) => page), (Route<dynamic> route) => false);
  }

}