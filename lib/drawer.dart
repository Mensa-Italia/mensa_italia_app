import 'dart:io';

import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:html/dom.dart' as dom;
import 'package:mensa_italia/regsoci.dart';
import 'package:mensa_italia/store.dart';
import 'package:page_transition/page_transition.dart';
import 'package:path_provider/path_provider.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:url_launcher/url_launcher.dart';

import 'blog.dart';
import 'home_full.dart';
import 'main.dart';
import 'package:cookie_jar/cookie_jar.dart';






class MensaDrawer extends StatefulWidget {
  dom.Document document;
  MensaDrawer(this.document);
  @override
  _MensaDrawerState createState() => _MensaDrawerState();
}

class _MensaDrawerState extends State<MensaDrawer> {

  String createLink(String link) {

    if (link.split("://").contains("https")) {
      return link;
    }else{
      return "https://www.cloud32.it"+link;

    }
  }

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.transparent,
      child: ListView(
        padding: EdgeInsets.zero,
        children: <Widget>[
          Container(height: MediaQuery.of(context).padding.top+10,),
          CardClipperElements(
              Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: <Widget>[
                  AspectRatio(
                    child: Image.network(
                      "https://www.cloud32.it"+widget.document.getElementsByTagName("img").where((e)=>e.attributes["alt"]=="Foto").first.attributes["src"],
                      fit: BoxFit.cover,
                    ),
                    aspectRatio: 10/9,
                  ),
                  Container(


                    padding: EdgeInsets.all(10),

                    color: Theme.of(context).accentColor,
                    child: AutoSizeText(widget.document.getElementsByTagName("span").where((e)=>e.attributes["class"]=="itemless nomeprofilo").first.text.toUpperCase(), style: TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold), textAlign: TextAlign.center,),
                  ),
                ],
              )
          ),



          CardClipperElements(
              ListTile(
                leading: Icon(Icons.sim_card, color: Theme.of(context).accentColor,),
                title: Text('Carta PDF'),
                onTap: () {

                  String link=(createLink(widget.document.getElementsByTagName("a").where((e)=>e.attributes["class"]=="btn btn-success btn-sm btn-block").where((e)=>e.text=="Tessera").first.attributes["href"]));

                  Navigator.pop(context);
                  Navigator.push(context, PageTransition(type: PageTransitionType.rightToLeft, child:ShowDocumentPage(link)));
                },
              )
          ),
    CardClipperElements(ListTile(
            leading: Icon(Icons.supervised_user_circle, color: Theme.of(context).accentColor,),
            title: Text('Registro soci'),
            onTap: () {

              Navigator.pop(context);
              Navigator.push(context, PageTransition(type: PageTransitionType.rightToLeft, child:RegSoci()));
            },
          )),
    CardClipperElements(ListTile(
            leading: Icon(Icons.insert_drive_file, color: Theme.of(context).accentColor,),
            title: Text('Documenti'),
            onTap: () {

              Navigator.pop(context);
              showDialog(
                  context: context,
                  builder: (d)=>DialogDocumentPick(widget.document.getElementsByTagName("li").where((e)=>e.attributes["class"]=="dropdown").where((e)=>e.getElementsByTagName("a").first.text=="Documenti").first.getElementsByTagName("ul").first.getElementsByTagName("li"))
              );
            },
          )),
    CardClipperElements(ListTile(
              leading: Icon(Icons.close, color: Colors.redAccent,),
              title: Text('Disconnetti profilo'),
              onTap: () async {

                Directory appDocDir = await getApplicationDocumentsDirectory();
                String appDocPath = appDocDir.path;
                var cookieJar=PersistCookieJar(dir:appDocPath+"/.cookies/");
                cookieJar.deleteAll();

                SharedPreferences prefs = await SharedPreferences.getInstance();
                prefs.clear();

                Navigator.pushAndRemoveUntil(context, PageTransition(type: PageTransitionType.fade, child:HomePage()), ModalRoute.withName('/'));

              }
          )),
          Container(height: 50,),
          CardClipperElements(Container(
            child: GestureDetector(
                onTap: _launchURL,
                child:  Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: <Widget>[
                    AutoSizeText("Thought by ", style: TextStyle(fontStyle: FontStyle.italic, color: Color(0xFF184295)), textAlign: TextAlign.center,),
                    AutoSizeText("Matteo Sipione", style: TextStyle(fontStyle: FontStyle.italic, color: Color(0xFF184295), fontWeight: FontWeight.bold), textAlign: TextAlign.center,),

                  ],
                )
            ),
            padding: EdgeInsets.all(5),
          ))
        ],
      ),
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
