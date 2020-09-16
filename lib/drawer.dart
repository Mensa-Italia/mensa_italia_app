import 'dart:io';
import 'package:auto_size_text/auto_size_text.dart';
import 'package:dart_rss/domain/media/media.dart';
import 'package:flutter/material.dart';
import 'package:html/dom.dart' as dom;
import 'package:mensa_italia/phone_book.dart';
import 'package:mensa_italia/regsoci.dart';
import 'package:mensa_italia/renew.dart';
import 'package:mensa_italia/transitate.dart';
import 'package:path_provider/path_provider.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:url_launcher/url_launcher.dart';
import 'blog.dart';
import 'home_full.dart';
import 'main.dart';
import 'package:cookie_jar/cookie_jar.dart';






class MensaDrawer extends StatefulWidget {
  dom.Document document;
  Function reload;
  MensaDrawer(this.document, this.reload);
  @override
  _MensaDrawerState createState() => _MensaDrawerState();
}

class _MensaDrawerState extends State<MensaDrawer> {

  String createLink(String link) {

    if (link.split("://").contains("https")) {
      return (""+link);
    }else{
      return "https://www.cloud32.it"+link;


    }
  }

  Size size;
  @override
  Widget build(BuildContext context) {
    size=MediaQuery.of(context).size;
    return new LayoutBuilder(
        builder: (BuildContext context, BoxConstraints constraints) {
          return Material(
            color: Colors.transparent,
            child: ListView(
              padding: EdgeInsets.zero,
              children: <Widget>[
                Container(height: MediaQuery.of(context).padding.top+25,),
                CardClipperElements(
                  Container(
                    color: Theme.of(context).accentColor,
                    height: constraints.maxWidth/2,
                    child: Row(
                      children: <Widget>[
                        Container(
                          height: constraints.maxWidth/2,
                          child: AspectRatio(
                            child: Image.network(
                              "https://www.cloud32.it"+widget.document.getElementsByTagName("img").where((e)=>e.attributes["alt"]=="Foto").first.attributes["src"],
                              fit: BoxFit.cover,
                            ),
                            aspectRatio: 1,
                          ),
                        ),
                        Expanded(
                          child: Container(


                            padding: EdgeInsets.all(10),

                            color: Theme.of(context).accentColor,
                            child: AutoSizeText(widget.document.getElementsByTagName("span").where((e)=>e.attributes["class"]=="itemless nomeprofilo").first.text.toUpperCase(), style: TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold), textAlign: TextAlign.center,),
                          ),
                        )
                      ],
                    ),
                  ),
                  color: Theme.of(context).accentColor,

                ),


                size.width>=600?CardClipperElements(
                    Column(
                      children: [

                        ListTile(
                          leading: Icon(Icons.phone, color: Theme.of(context).accentColor,),
                          title: Text('Rubrica Mensana'),
                          onTap: () {

                            String link=(createLink(widget.document.getElementsByTagName("a").where((e)=>e.attributes["class"]=="btn btn-success btn-sm btn-block").where((e)=>e.text=="Tessera").first.attributes["href"]));

                            NavigateTo(context).page(PhoneBook());
                          },
                        ),
                      ],
                    )
                ):Container(),

                CardClipperElements(
                    Column(
                      children: <Widget>[
                        ListTile(
                          leading: Icon(Icons.sim_card, color: Theme.of(context).accentColor,),
                          title: Text('Tessera PDF'),
                          onTap: () {

                            String link=(createLink(widget.document.getElementsByTagName("a").where((e)=>e.attributes["class"]=="btn btn-success btn-sm btn-block").where((e)=>e.text=="Tessera").first.attributes["href"]));

                            if(!(size.width>=600))Navigator.pop(context);
                            NavigateTo(context).page(ShowDocumentPage(link));
                          },
                        ),
                        widget.document.getElementsByClassName("btn btn-success btn-sm btn-block").where((element) => element.text=="Rinnova").isNotEmpty?ListTile(
                          leading: Icon(Icons.trending_up, color: Theme.of(context).accentColor,),
                          title: Text('Rinnova Tessera'),
                          onTap: () async {

                            if(!(size.width>=600))Navigator.pop(context);
                            await NavigateTo(context).page(RenewCardPage());
                            widget.reload();
                          },
                        ):Container(),
                        ListTile(
                          leading: Icon(Icons.supervised_user_circle, color: Theme.of(context).accentColor,),
                          title: Text('Registro soci'),
                          onTap: () {

                            if(!(size.width>=600))Navigator.pop(context);
                            NavigateTo(context).page(RegSoci());
                          },
                        ),
                        ListTile(
                          leading: Icon(Icons.insert_drive_file, color: Theme.of(context).accentColor,),
                          title: Text('Documenti'),
                          onTap: () {

                            if(!(size.width>=600))Navigator.pop(context);
                            showDialog(
                                context: context,
                                builder: (d)=>DialogDocumentPick(widget.document.getElementsByTagName("li").where((e)=>e.attributes["class"]=="dropdown").where((e)=>e.getElementsByTagName("a").first.text=="Documenti").first.getElementsByTagName("ul").first.getElementsByTagName("li"))
                            );
                          },
                        ),
                        ListTile(
                            leading: Icon(Icons.close, color: Colors.redAccent,),
                            title: Text('Disconnetti profilo'),
                            onTap: () async {

                              Directory appDocDir = await getApplicationDocumentsDirectory();
                              String appDocPath = appDocDir.path;
                              var cookieJar=PersistCookieJar(dir:appDocPath+"/.cookies/");
                              cookieJar.deleteAll();

                              SharedPreferences prefs = await SharedPreferences.getInstance();
                              prefs.clear();

                              NavigateTo(context).pageClear(HomePage());

                            }
                        ),
                        Divider(
                          indent: 20,
                          endIndent: 20,
                          color: Theme.of(context).accentColor,
                        ),
                        Container(
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
                          margin: EdgeInsets.only(bottom: 10),
                          padding: EdgeInsets.all(5),
                        )
                      ],
                    )

                ),


                Container(height: 50,),
              ],
            ),
          );
        }
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
