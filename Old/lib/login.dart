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
import 'package:mensa_italia/transitate.dart';
import 'package:path_provider/path_provider.dart';
import 'package:rounded_loading_button/rounded_loading_button.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'home_full.dart';

class LoginPage extends StatefulWidget {
  @override
  _LoginPageState createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  TextEditingController emailController = new TextEditingController();
  TextEditingController passwordController = new TextEditingController();
  final RoundedLoadingButtonController _btnController = new RoundedLoadingButtonController();


  Size size;
  bool enabled=true;

  @override
  Widget build(BuildContext context) {
    size=MediaQuery.of(context).size;


    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.stretch,
      mainAxisAlignment: MainAxisAlignment.start,
      children: <Widget>[

        MensaTextField("Email", textEditingController: emailController,enablede: enabled,
          textInputType: TextInputType.emailAddress,),
        Container(height: 10,),
        MensaTextField("Password", obscure: true, textEditingController: passwordController, enablede: enabled,   textInputType: TextInputType.visiblePassword,),
        Container(height: 20,),

        RoundedLoadingButton(
          height: 40,
          child: AutoSizeText('ACCEDI', style: TextStyle(color: Colors.white)),
          controller: _btnController,
          color: Theme.of(context).accentColor,
          onPressed: () async {

            enabled=false;
            setState(() {

            });
            _btnController.start();



            Document document=await API().doLoginAndRetrieveMain(context, emailController.text, passwordController.text);



            if(document!=null){
              enabled=true;
              setState(() {

              });

              NavigateTo(context).pageClear(MensaFullPage(document));

            }else{
              enabled=true;
              setState(() {

              });
              _btnController.reset();
              showDialog(
                context: context,
                builder: (BuildContext context) {
                  // return object of type Dialog
                  return ErrorDialog();
                },
              );

            }
          },
        ),

      ],
    );
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
    return  Dialog(
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
        );
  }
}



class MensaTextField extends TextField{

  final String text;
  final bool obscure;
  final TextEditingController textEditingController;
  final TextInputType textInputType;
  final Function(String) onChag;
  final bool enablede;

  MensaTextField(this.text, {this.obscure=false, TextEditingController textEditingController, this.onChag, this.enablede=true, this.textInputType}) : this.textEditingController=textEditingController??TextEditingController();

  @override
  bool get enabled => enablede;

  @override
  // TODO: implement keyboardType
  TextInputType get keyboardType => this.textInputType;

  @override
  get onChanged => (s){
    if(onChag!=null){
      onChag(controller.text);
    }
    if(super.onChanged!=null){
      super.onChanged(s);
    }
  };


  @override
  // TODO: implement controller
  TextEditingController get controller => textEditingController;

  @override
  // TODO: implement obscureText
  bool get obscureText => obscure;

  @override
  // TODO: implement decoration
  InputDecoration get decoration => InputDecoration(
    hintText: text,
    filled: true,
    fillColor: Color(0xFFd9d9d9),
    contentPadding: EdgeInsets.symmetric(vertical: 10, horizontal: 20),
    focusColor: Color(0xFFd9d9d9),
    hoverColor: Color(0xFFd9d9d9),
    hintStyle: TextStyle(color: Colors.black.withOpacity(0.3)),
    labelStyle: TextStyle(
        color: Color(0xFF3d3d3d),
        fontWeight: FontWeight.bold
    ),
    border: OutlineInputBorder(
        borderSide: BorderSide(color: Colors.transparent),
        borderRadius: BorderRadius.circular(200)
    ),
    focusedBorder: OutlineInputBorder(
        borderSide: BorderSide(color: Colors.transparent),
        borderRadius: BorderRadius.circular(200)
    ),
    enabledBorder: OutlineInputBorder(
        borderSide: BorderSide(color: Colors.transparent),
        borderRadius: BorderRadius.circular(200)
    ),
    //fillColor: Colors.green
  );
}

class MensaButton extends MaterialButton{

  final Function onPressedNew;
  final String text;
  final BorderRadius radius;
  final bool enableded;
  MensaButton({this.onPressedNew,this.text, this.radius, this.enableded=true});


  @override
  // TODO: implement enabled
  bool get enabled => this.enableded;

  @override
  // TODO: implement onPressed
  get onPressed => (){
    if(enableded){
      onPressedNew();
    }
  };

  @override
  // TODO: implement materialTapTargetSize
  MaterialTapTargetSize get materialTapTargetSize => MaterialTapTargetSize.shrinkWrap;

  @override
  // TODO: implement elevation
  double get elevation => 3.0;

  @override
  // TODO: implement shape
  ShapeBorder get shape => new RoundedRectangleBorder(
      borderRadius: radius??BorderRadius.circular(200.0),
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



