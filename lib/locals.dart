import 'dart:convert';
import 'dart:math';

import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:mensa_italia/login.dart';
import 'package:mensa_italia/sig.dart';
import 'package:page_transition/page_transition.dart';
import 'package:shimmer/shimmer.dart';
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
        children: [

          Container(
            padding: EdgeInsets.all(20),
            child:mensaTextField??=MensaTextField("Cerca Gruppo",onChag: (text){
              setState(() {
                filtered=list.where((element) => element["name"].toString().toLowerCase().contains(text.toLowerCase())).toList();
              });
            },),
          ),
          ...getChildren()
        ],
      ),

    );
  }

  List<Widget> getChildren(){
    if(list==null){
      return List.generate(3, (index) => Shimmer.fromColors(
        baseColor: Colors.white,
        highlightColor: Theme.of(context).accentColor.withOpacity(0.5),
        child: Container(
          margin: EdgeInsets.all(10),
          width: MediaQuery.of(context).size.width,
          height: MediaQuery.of(context).size.width*758/1875+50,
          decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(15),
              color: Color(0xFF303030)
          ),
        ),
      ));
    }
    if(list.isNotEmpty){
      return [
        ...List.generate(filtered.length, (i){
          return SigItem(filtered[i]["link"],filtered[i]["name"],filtered[i]["image"]);
        })
      ];
    }
    return [];
  }
}





