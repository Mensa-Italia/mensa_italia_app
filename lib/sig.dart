import 'dart:convert';
import 'dart:math';

import 'package:auto_size_text/auto_size_text.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:mensa_italia/login.dart';
import 'package:page_transition/page_transition.dart';
import 'package:url_launcher/url_launcher.dart';




class SIGMensa extends StatefulWidget {
  @override
  _SIGMensaState createState() => _SIGMensaState();
}

class _SIGMensaState extends State<SIGMensa> {


  List<dynamic> list;
  List<dynamic> filtered;

  init() async {
    list=jsonDecode((await API().getRawData("https://raw.githubusercontent.com/Mensa-Italia/SIGs/master/sigs.json?id="+(new Random(15)).nextInt(15000).toString())));
    list.sort((el1, el2)=>el1["name"].toString().toLowerCase().compareTo(el2["name"].toString().toLowerCase()));
    filtered=list;
    print(list);
    setState(() {

    });
  }

  @override
  void initState() {
    // TODO: implement initState
    super.initState();
    init();
  }


  @override
  Widget build(BuildContext context) {


    return Scaffold(


      appBar: AppBar(
        title: AutoSizeText("Special Interest Groups".toUpperCase()),

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
            child:MensaTextField("Cerca SIG",onChag: (text){

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







class SigItem extends StatefulWidget {
  String url;
  String image;
  SigItem(this.url,this.image);
  @override
  _SigItemState createState() => _SigItemState();
}

class _SigItemState extends State<SigItem> {
  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: (){
        tryToLunchUrl(widget.url);
      },
      child: Card(
        margin: EdgeInsets.all(10),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(15)),
        elevation: 5.0,
        child: Container(
          width: MediaQuery.of(context).size.width,
          decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(15)
          ),
          child: ClipRRect(
              borderRadius: BorderRadius.circular(15),
              child: CachedNetworkImage(
                imageUrl:widget.image,
                width: MediaQuery.of(context).size.width,
                fit: BoxFit.cover,
                errorWidget: (d,s,t)=>CachedNetworkImage(imageUrl: "https://www.mensaitalia.it/wp-content/uploads/2019/12/sig_miog_error.jpg", fit: BoxFit.cover,),
              )
          ),
        ),
      ),
    );
  }

  tryToLunchUrl(String url) async {

    if (await canLaunch(url)) {
      await launch(url);
    }
  }
}

