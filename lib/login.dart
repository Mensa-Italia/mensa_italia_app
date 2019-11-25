/*
Creato da Matteo Sipion nella data del 15/10/2019.

Matteo Sipione detiene i diritti autoriali e commerciali di questo software.

-------------------------------------------------------------------------------

Created by Matteo Sipion on the date of 15/10/2019.

Matteo Sipione holds the authorial and commercial rights to this software.
*/
import 'dart:async';
import 'dart:io';
import 'package:auto_size_text/auto_size_text.dart';
import 'package:dio/dio.dart';
import 'package:dio_cookie_manager/dio_cookie_manager.dart';
import 'package:flutter/material.dart';
import 'package:cookie_jar/cookie_jar.dart';
import 'package:html/dom.dart';
import 'package:html/parser.dart' as html;
import 'package:page_transition/page_transition.dart';
import 'package:path_provider/path_provider.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:url_launcher/url_launcher.dart';
import 'home_full.dart';

class LoginPage extends StatefulWidget {
  @override
  _LoginPageState createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  TextEditingController emailController = new TextEditingController();
  TextEditingController passwordController = new TextEditingController();


  Size size;
  @override
  Widget build(BuildContext context) {
    size=MediaQuery.of(context).size;


    return Scaffold(



      body:  new GestureDetector(
          onTap: () {

            FocusScope.of(context).requestFocus(new FocusNode());
          },
          child:Container(
            width: size.width,

            height: size.height,
            child: Stack(
              children: [
                SingleChildScrollView(

                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.center,
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: <Widget>[
                      Container(
                        height: 50,
                      ),
                      Hero(tag: "logo", child: Material(color: Colors.transparent,child: Image.asset("assets/images/lettering_blue.png", width: size.width/3,),),),
                      Container(height: 40,),
                      Container(
                        padding: EdgeInsets.all(20),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.stretch,
                          children: <Widget>[

                            MensaTextField("Email", textEditingController: emailController,),
                            Container(height: 10,),
                            MensaTextField("Password", obscure: true, textEditingController: passwordController,),

                            Container(height: 30,),
                            MensaButton(
                              onPressedNew: () async {


                                showDialog(
                                  context: context,
                                  builder: (BuildContext context) {
                                    // return object of type Dialog
                                    return Padding(
                                        padding: EdgeInsets.only(left: 50.0, right: 50.0),
                                    child://AlertDialog or any other Dialog you can use
                                    Dialog(
                                    elevation: 0.0,
                                    backgroundColor: Colors.transparent,
                                    child: Container(

                                    decoration: BoxDecoration(
                                    color: Colors.white,
                                    borderRadius: BorderRadius.all(Radius.circular(25))
                                    ),
                                    child: Column(
                                    mainAxisSize: MainAxisSize.min,
                                    crossAxisAlignment: CrossAxisAlignment.stretch,
                                    children: <Widget>[
                                    Container(

                                    padding: EdgeInsets.symmetric(horizontal: 20, vertical: 25),
                                    child: Column(
                                    mainAxisSize: MainAxisSize.min,
                                    children: <Widget>[LoadingDialog()]))]))));
                                  },
                                );
                                Document document=await API().doLoginAndRetrieveMain(context, emailController.text, passwordController.text);

                                Navigator.pop(context);



                                if(document!=null){


                                  Navigator.pushAndRemoveUntil(context, PageTransition(type: PageTransitionType.fade, child:MensaFullPage(document)), ModalRoute.withName('/'));

                                }else{
                                  showDialog(
                                    context: context,
                                    builder: (BuildContext context) {
                                      // return object of type Dialog
                                      return ErrorDialog();
                                    },
                                  );
                                }




                              },

                              text: "ACCEDI",
                            ),
                          ],
                        ),
                      ),


                      Container(height: 50,),
                      GestureDetector(
                        onTap: _launchURL,
                        child:  Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: <Widget>[
                            AutoSizeText("Thought by ", style: TextStyle(fontStyle: FontStyle.italic, color: Color(0xFF184295)), textAlign: TextAlign.center,),
                            AutoSizeText("Matteo Sipione", style: TextStyle(fontStyle: FontStyle.italic, color: Color(0xFF184295), fontWeight: FontWeight.bold), textAlign: TextAlign.center,),

                          ],
                        )
                      )
                    ],
                  ),

                ),
              ],
            ),
          )),
    );
  }

  _launchURL() async {
    const url = 'https://sipio.it';
    if (await canLaunch(url)) {
      await launch(url);
    } else {
      throw 'Could not launch $url';
    }
  }

}



class API{


  Response response;
  Dio dio = new Dio();



  Future<File> getFile(String url) async {

    Directory appDocDir = await getApplicationDocumentsDirectory();
    String appDocPath = appDocDir.path;
    var cookieJar=PersistCookieJar(dir:appDocPath+"/.cookies/");
    dio.interceptors.add(CookieManager(cookieJar));
    response = await dio.download(url, appDocPath+"/pdf.pdf");
    return File(appDocPath+"/pdf.pdf");

  }




  Future<Document> getData(String link) async{

    Directory appDocDir = await getApplicationDocumentsDirectory();
    String appDocPath = appDocDir.path;
    var cookieJar=PersistCookieJar(dir:appDocPath+"/.cookies/");
    dio.interceptors.add(CookieManager(cookieJar));
    response = await dio.get(link, options: Options(
        followRedirects: true,
        validateStatus: (status) { return status < 500; }
    ),);

    return html.parse(response.data);
  }


  Future<Document> doLoginAndRetrieveMain(BuildContext context,String email, String password) async{



    Directory appDocDir = await getApplicationDocumentsDirectory();
    String appDocPath = appDocDir.path;
    var cookieJar=PersistCookieJar(dir:appDocPath+"/.cookies/");
    dio.interceptors.add(CookieManager(cookieJar));

    response = await dio.get("https://www.cloud32.it/Associazioni/utenti/login?codass=170734", options: Options(
        followRedirects: true,

        validateStatus: (status) { return status < 500; }
    ),);



    String Token;
    Document document;





    document = html.parse(response.data);



    if(!response.isRedirect&&document.getElementsByTagName("input").where((e)=>e.attributes["name"]=="_token").isNotEmpty){

      Token=(document.getElementsByTagName("input").where((e)=>e.attributes["name"]=="_token").first.attributes["value"]);



      FormData formData=FormData.fromMap({
        "email":email,
        "password":password,
        "_token":Token
      });
      response = await dio.post("https://www.cloud32.it/Associazioni/utenti/login", data: formData,options: Options(
          followRedirects: true,

          validateStatus: (status) { return status < 500; }
      ),);




    }

    response = await dio.get("https://www.cloud32.it/Associazioni/utenti/home", options: Options(
        followRedirects: true,

        validateStatus: (status) { return status < 500; }
    ),);


    print(response);





    document = html.parse(response.data);

    if(document.getElementsByTagName("img").where((e)=>e.attributes["alt"]=="Foto").isNotEmpty){

      savePasswordEmail(email, password);
      return document;
    }else{

      return null;





    }
  }


  savePasswordEmail(String email, String password) async {
    SharedPreferences prefs = await SharedPreferences.getInstance();
    await prefs.setString("email", email);
    await prefs.setString("password", password);
  }


}



class LoadingDialog extends StatefulWidget {
  @override
  _LoadingDialogState createState() => _LoadingDialogState();
}

class _LoadingDialogState extends State<LoadingDialog> with SingleTickerProviderStateMixin {
  AnimationController _controller;

  @override
  void initState() {
    _controller = AnimationController(vsync: this, duration: Duration(seconds: 10000));
    _controller.addListener((){
      setState(() {

      });
    });
    _controller.forward();
    super.initState();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }


  Size size;
  @override
  Widget build(BuildContext context) {
    size=MediaQuery.of(context).size;
    return Transform.rotate(angle: _controller.value*50000, child: Image.asset("assets/images/loading.png", width: size.width/4,),);
  }
}





class ErrorDialog extends StatefulWidget {
  @override
  _ErrorDialogState createState() => _ErrorDialogState();
}

class _ErrorDialogState extends State<ErrorDialog> {
  @override
  Widget build(BuildContext context) {
    return Padding(
        padding: EdgeInsets.only(left: 50.0, right: 50.0),
        child://AlertDialog or any other Dialog you can use
        Dialog(
            elevation: 0.0,
            backgroundColor: Colors.transparent,
            child: Container(

              decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.all(Radius.circular(25))
              ),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: <Widget>[
                  Container(

                    padding: EdgeInsets.symmetric(horizontal: 20, vertical: 25),
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: <Widget>[

                        AutoSizeText("ERRORE", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 25,color: Color(0xFF184295)),),
                        Container(height: 20,),
                        AutoSizeText("Credenziali non corrette, riprova", textAlign: TextAlign.center,),
                      ],
                    ),
                  ),
                  MensaButton(onPressedNew: (){

                    Navigator.pop(context);
                  },text: "Va bene",),

                  Container(height: 20,),
                ],
              ),
            )
        ));
  }
}



class MensaTextField extends TextField{

  String text;
  bool obscure;
  TextEditingController textEditingController;
  MensaTextField(this.text, {this.obscure=false, this.textEditingController});


  @override
  // TODO: implement controller
  TextEditingController get controller => textEditingController??super.controller;

  @override
  // TODO: implement obscureText
  bool get obscureText => obscure;

  @override
  // TODO: implement decoration
  InputDecoration get decoration => InputDecoration(
    labelText: text,

    fillColor: Colors.white,
    contentPadding: EdgeInsets.symmetric(vertical: 10, horizontal: 20),
    labelStyle: TextStyle(
        color: Color(0xFF184295),
        fontWeight: FontWeight.bold
    ),
    border: new OutlineInputBorder(
      borderRadius: new BorderRadius.circular(25.0),
      borderSide: new BorderSide(
          width: 1.5,
          color: Color(0xFF184295)
      ),
    ),
    focusedBorder: new OutlineInputBorder(
      borderRadius: new BorderRadius.circular(25.0),
      borderSide: new BorderSide(

          width: 2.0,
          color: Color(0xFF184295)
      ),

    ),
    enabledBorder: new OutlineInputBorder(
      borderRadius: new BorderRadius.circular(25.0),
      borderSide: new BorderSide(

          width: 1.5,
          color: Color(0xFF184295)
      ),

    ),
    //fillColor: Colors.green
  );
}

class MensaButton extends FlatButton{




  Function onPressedNew;
  String text;
  BorderRadius radius;
  MensaButton({this.onPressedNew,this.text, this.radius}){
    radius=radius??BorderRadius.circular(200.0);
  }



  @override
  // TODO: implement onPressed
  get onPressed => onPressedNew;

  @override
  // TODO: implement materialTapTargetSize
  MaterialTapTargetSize get materialTapTargetSize => MaterialTapTargetSize.shrinkWrap;

  @override
  // TODO: implement elevation
  double get elevation => 3.0;

  @override
  // TODO: implement shape
  ShapeBorder get shape => new RoundedRectangleBorder(
      borderRadius: radius,
      side: BorderSide(color: Color(0xFF184295)));


  @override
  // TODO: implement child
  Widget get child => AutoSizeText(
    text.toUpperCase(),
    style: TextStyle(
      fontSize: 14.0,
    ),
    textAlign: TextAlign.center,
  );

  @override
  // TODO: implement padding
  EdgeInsetsGeometry get padding => EdgeInsets.all(8.0);

  @override
  // TODO: implement color
  Color get color => Color(0xFF184295);

  @override
  // TODO: implement textColor
  Color get textColor => Colors.white;


  @override
  Widget build(BuildContext context) {
    // TODO: implement build
    return Hero(tag: text,child: Material(
      color: Colors.transparent,
      child: super.build(context),
    ),);
  }

}



