import 'dart:convert';
import 'dart:math';

import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:mensa_italia/login.dart';
import 'package:mensa_italia/sig.dart';
import 'package:page_transition/page_transition.dart';
import 'package:url_launcher/url_launcher.dart';




class LOCALSMensa extends StatefulWidget {
  @override
  _LOCALSMensaState createState() => _LOCALSMensaState();
}

class _LOCALSMensaState extends State<LOCALSMensa> {


  List<dynamic> list;
  List<dynamic> filtered;

  init() async {
    list=jsonDecode((await API().getRawData("https://raw.githubusercontent.com/Mensa-Italia/SIGs/master/locals.json?id="+(new Random(15)).nextInt(15000).toString())));
    list.sort((el1, el2)=>el1["name"].toString().toLowerCase().compareTo(el2["name"].toString().toLowerCase()));
    filtered=list;
    setState(() {

    });
  }

  @override
  void initState() {
    // TODO: implement initState
    super.initState();
    init();
  }

  MensaTextField mensaTextField;
  @override
  Widget build(BuildContext context) {


    return Scaffold(


      appBar: AppBar(
        title: AutoSizeText("Gruppi Locali".toUpperCase()),

      ),
      body: ListView(
        children: list==null?[

          Container(
            margin: EdgeInsets.only(top: 20),
            width: 100,
            height: 100,
            child: LoadingDialog(),
          )

        ]:list.isNotEmpty?([

          Container(
            padding: EdgeInsets.all(20),
            child:mensaTextField??=MensaTextField("Cerca Gruppo",onChag: (text){

              print(text);
              filtered=list.where((element) => element["name"].toString().toLowerCase().contains(text.toLowerCase())).toList();
              setState(() {

              });
            },),
          )

        ]..addAll(List.generate(filtered.length, (i){
          return SigItem(filtered[i]["link"],filtered[i]["image"]);
        }))):[],
      ),

    );
  }
}





