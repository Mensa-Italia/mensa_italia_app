import 'dart:convert';
import 'dart:math';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:auto_size_text/auto_size_text.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:memoize/memoize.dart';
import 'package:mensa_italia/login.dart';
import 'package:url_launcher/url_launcher.dart';




class SIGMensa extends StatefulWidget {
  @override
  _SIGMensaState createState() => _SIGMensaState();
}

class _SIGMensaState extends State<SIGMensa> {


  List<dynamic> list;
  List<dynamic> filtered;
  MensaTextField mensaTextField;

  init() async {
    list=jsonDecode((await API().getRawData("https://raw.githubusercontent.com/Mensa-Italia/SIGs/master/sigs.json?id="+(new Random(15)).nextInt(15000).toString())));
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


  @override
  Widget build(BuildContext context) {

    List<Widget> children=list==null?[

      Container(
        margin: EdgeInsets.only(top: 20),
        width: 100,
        height: 100,
        child: LoadingDialog(),
      )

    ]:list.isNotEmpty?([

      Container(
        padding: EdgeInsets.all(20),
        child:mensaTextField??=MensaTextField("Cerca SIG",onChag: (text){

          filtered=list.where((element) => element["name"].toString().toLowerCase().contains(text.toLowerCase())).toList();
          setState(() {

          });
        },),
      )

    ]..addAll(List.generate(filtered.length, (i){
      return SigItem(filtered[i]["link"],filtered[i]["name"],filtered[i]["image"]);
    }))):[];

    return Scaffold(


      appBar: AppBar(
        title: AutoSizeText("Special Interest Groups".toUpperCase()),

      ),
      body: ListView.builder(
        itemBuilder: (context, index){
          return children.elementAt(index);
        },
        itemCount: children.length,
      ),

    );
  }
}







class SigItem extends StatelessWidget {
  String url;
  String name;
  String image;
  int userNumber;
  SigItem(this.url,this.name,this.image);

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: (){
        tryToLunchUrl(url);
      },
      child: Card(
        margin: EdgeInsets.all(10),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(15)),
        elevation: 5.0,
        child: Container(
          width: MediaQuery.of(context).size.width,
          decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(15),
              color: Color(0xFF303030)
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              ClipRRect(
                  borderRadius: BorderRadius.only(topRight: Radius.circular(15), topLeft: Radius.circular(15)),
                  child: CachedNetworkImage(
                    imageUrl:image,
                    width: MediaQuery.of(context).size.width,
                    fit: BoxFit.cover,
                    errorWidget: (d,s,t)=>CachedNetworkImage(imageUrl: "https://www.mensaitalia.it/wp-content/uploads/2019/12/sig_miog_error.jpg", fit: BoxFit.cover,),
                  )
              ),
              Container(
                margin: EdgeInsets.only(left: 20, top: 10, bottom: 10, right: 20),
                child: FutureBuilder<String>(
                  future: getUserData(url),
                  builder: (BuildContext context, AsyncSnapshot<String> snapshot) {
                    switch (snapshot.connectionState) {
                      case ConnectionState.waiting: return AutoSizeText.rich(TextSpan(
                          children: [
                            TextSpan(text: name.toUpperCase()+"\n", style: TextStyle(fontWeight: FontWeight.bold)),
                            TextSpan(text: userNumber==null?"Caricamento soci...":userNumber.toString()),
                          ]
                      ), style: TextStyle(color: Colors.white));
                      default:
                        if (snapshot.hasError)
                          return AutoSizeText.rich(TextSpan(
                              children: [
                                TextSpan(text: name.toUpperCase(), style: TextStyle(fontWeight: FontWeight.bold)),
                              ]
                          ), style: TextStyle(color: Colors.white));
                        else
                          return AutoSizeText.rich(TextSpan(
                              children: [
                                TextSpan(text: name.toUpperCase()+"\n", style: TextStyle(fontWeight: FontWeight.bold)),
                                TextSpan(text: "Soci registrati: "+snapshot.data.toString()),
                              ]
                          ), style: TextStyle(color: Colors.white),);
                    }
                  },
                ),
              )

            ],
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


Future<String> getUserData(String url) async {
  var func = memo0<Future<String>>(() async => getUsersNumber(url));
  return await func();
}


Future<String> getUsersNumber(String url) async {
  Dio dio= Dio();
  String response = (await dio.get("https://api.allorigins.win/raw?url="+url,)).data.toString();
  String str = response.toString();
  const start = "has ";
  const end = " members";
  final startIndex = str.indexOf(start);
  final endIndex = str.indexOf(end, startIndex + start.length);
  return int.parse(str.substring(startIndex + start.length, endIndex).toString().trim()).toString();
}
