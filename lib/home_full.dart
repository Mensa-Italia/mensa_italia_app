/*
Creato da Matteo Sipion nella data del 15/10/2019.

Matteo Sipione detiene i diritti autoriali e commerciali di questo software.

-------------------------------------------------------------------------------

Created by Matteo Sipion on the date of 15/10/2019.

Matteo Sipione holds the authorial and commercial rights to this software.
*/
import 'dart:async';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:auto_size_text/auto_size_text.dart';
import 'package:flip_card/flip_card.dart';
import 'package:flutter/animation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/material.dart' as AM;
import 'package:html/dom.dart' as prefix1;
import 'package:html/dom.dart';
import 'package:http/http.dart' as http;
import 'package:in_app_review/in_app_review.dart';
import 'package:mensa_italia/locals.dart';
import 'package:mensa_italia/phone_book.dart';
import 'package:mensa_italia/renew.dart';
import 'package:mensa_italia/sig.dart';
import 'package:mensa_italia/transitate.dart';
import 'package:mensa_italia/youtube.dart';
import 'package:pdf_viewer_plugin/pdf_viewer_plugin.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:simple_html_css/simple_html_css.dart';
import 'package:url_launcher/url_launcher.dart';
import 'blog.dart';
import 'document.dart';
import 'drawer.dart';
import 'login.dart';

class TransitionAppBar extends StatelessWidget {

  TransitionAppBar({Key key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return SliverPersistentHeader(
      pinned: true,
      floating: false,

      delegate: _TransitionAppBarDelegate(),
    );
  }
}

class _TransitionAppBarDelegate extends SliverPersistentHeaderDelegate {

  @override
  Widget build(
      BuildContext context, double shrinkOffset, bool overlapsContent) {
    double tempVal = 34 * maxExtent / 100;
    final progress =  shrinkOffset > tempVal ? 1.0 : shrinkOffset / tempVal;
    final Rprogress=1-progress;
    return SizedBox.expand(
        child: Container(
          color: Theme.of(context).accentColor.withOpacity(progress),
          alignment: Alignment.topLeft,
          child:  Card(
            elevation: 5.0*Rprogress,
            color: Theme.of(context).accentColor,
            margin: EdgeInsets.only(left: 20.0*Rprogress, right: 20.0*Rprogress, top: MediaQuery.of(context).padding.top+20*Rprogress, bottom: 5*Rprogress),
            shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(50*Rprogress)
            ),
            child: Container(
              height: 80,
              padding: EdgeInsets.only(left: 5*Rprogress, right: 5*Rprogress,),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.center,
                children: <Widget>[
                  Expanded(
                    child: Row(
                      children: <Widget>[
                        Container(
                            margin: EdgeInsets.only(left: 10*Rprogress),
                            child: IconButton(
                              icon: Icon(Icons.menu, color: Theme.of(context).primaryTextTheme.title.color,),
                              onPressed: () {
                                Scaffold.of(context).openDrawer();
                              },
                            )
                        ),
                        AutoSizeText("MENSA ITALIA", style: Theme.of(context).primaryTextTheme.title, minFontSize: 0,)
                      ],
                    ),
                  ),
                  Container(
                    margin: EdgeInsets.only(right: 10*Rprogress),
                    child:  IconButton(
                      icon: Icon(Icons.phone, color: Theme.of(context).primaryTextTheme.title.color,),
                      onPressed: () {


                        NavigateTo(context).page(PhoneBook());
                      },
                    ),
                  )

                ],
              ),
            ),
          ),
        )
    );
  }

  @override
  double get maxExtent => 120;

  @override
  double get minExtent => 90;

  @override
  bool shouldRebuild(_TransitionAppBarDelegate oldDelegate) {
    return false;
  }
}





class MensaFullPage extends StatefulWidget {


  Document document;

  MensaFullPage(this.document);

  @override
  _MensaFullPageState createState() => _MensaFullPageState();
}

class _MensaFullPageState extends State<MensaFullPage> {

  bool isNumeric(String s) {
    if(s == null) {
      return false;
    }
    return double.parse(s, (e) => null) != null;
  }


  ScrollController scrollController=ScrollController();

  reload() async {
    SharedPreferences prefs = await SharedPreferences.getInstance();
    Document document=await API().doLoginAndRetrieveMain(context, prefs.getString("email"), prefs.getString("password"));
    widget.document=document;
    setState(() {

    });
  }


  Future<String> deleteWithBodyExample(String notyId) async {
    final request = http.Request("DELETE", Uri.parse("https://onesignal.com/api/v1/notifications/"+notyId+"?app_id=f2b93a2b-0d67-4e9e-b5c8-991c96a33ddc"));
    request.headers.addAll(<String, String>{
      "Accept": "application/json",
    });
    final response = await request.send();
    print(response);
    if (response.statusCode != 200)
      return Future.error("error: status code ${response.statusCode}");
    return await response.stream.bytesToString();
  }

  renew() async {

    var a = await SharedPreferences.getInstance();

    //a.setString("NextRenew", null);

    if(widget.document.getElementsByClassName("btn btn-success btn-sm btn-block").where((element) => element.text=="Rinnova").isEmpty){
      if(a.getStringList("renewNotify")!=null){
        for(int i=0;i<a.getStringList("renewNotify").length;i++){
          deleteWithBodyExample(a.getStringList("renewNotify").elementAt(i));
        }
        a.setStringList("renewNotify", null);
      }
    }

    if(a.getString("NextRenew")==null&&widget.document.getElementsByClassName("btn btn-success btn-sm btn-block").where((element) => element.text=="Rinnova").isNotEmpty){

      DateTime toSend=DateTime.parse(
          widget.document.getElementsByTagName("label").where((e)=>!isNumeric(e.text)).first.text.trim().split("/")[2]+"-"+
              widget.document.getElementsByTagName("label").where((e)=>!isNumeric(e.text)).first.text.trim().split("/")[1]+"-"+
              widget.document.getElementsByTagName("label").where((e)=>!isNumeric(e.text)).first.text.trim().split("/")[0]
      );


      if(a.getStringList("renewNotify")!=null){
        for(int i=0;i<a.getStringList("renewNotify").length;i++){
          deleteWithBodyExample(a.getStringList("renewNotify").elementAt(i));
        }
        a.setStringList("renewNotify", null);
      }



      if(a.getStringList("renewNotify")==null){
        a.setStringList("renewNotify",[]);
      }


      for(int i=0;i<30;i++){
        DateTime temp=(DateTime(toSend.year, toSend.month, toSend.day, 18,0).toUtc().subtract(Duration(days: i)));

        try{

          /*  Map ax=await OneSignal.shared.postNotification(OSCreateNotification(
              playerIds: [(await OneSignal.shared.getPermissionSubscriptionState()).subscriptionStatus.userId],
              content: "La tua tessera scadrà tra "+(i-1).toString()+" giorni. Rinnovala ora!",
              delayedOption: OSCreateNotificationDelayOption.timezone,
              sendAfter: temp
          ));

          print(ax["id"]+" "+DateTime.now().toUtc().add(Duration(seconds: 5*i)).toIso8601String());
          await a.setStringList("renewNotify", a.getStringList("renewNotify")..addAll([
            ax["id"]
          ]));*/
        }catch(Exc){

        }


      }





      a.setString("NextRenew", "true");
    }



  }

  @override
  void initState() {
    // TODO: implement initState
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((duration){

      precacheImage(new NetworkImage( "https://www.cloud32.it"+widget.document.getElementsByTagName("img").where((e)=>e.attributes["alt"]=="Foto").first.attributes["src"]), context);

    });



    scrollController.addListener(() { toDoWhileOnMove();});

    startReview();
  }

  startReview() async {
    SharedPreferences prefs = await SharedPreferences.getInstance();
    int point=prefs.getInt("downloadedPoint");
    if(point==null||point>1){
      PhoneDB().download();
    }
    InAppReview inAppReview = InAppReview.instance;

    if (await inAppReview.isAvailable()) {
      inAppReview.requestReview();
    }
  }


  Timer time;

  toDoWhileOnMove(){

    if(time!=null&&time.isActive){
      time.cancel();
    }
    time=Timer(Duration(milliseconds: 200),(){
      if(scrollController.offset<28){
        scrollController.animateTo(0, duration: Duration(milliseconds: 200), curve: Curves.linear);
      }else if(scrollController.offset<60){
        scrollController.animateTo(60, duration: Duration(milliseconds: 200), curve: Curves.linear);

      }
    });


  }




  Size size;
  @override
  Widget build(BuildContext context) {
    size=MediaQuery.of(context).size;
    //renew();


    ListView BaseBlock=ListView(
      children: <Widget>[

        Container(
          height: 25,
        ),




        FlipCard(
          direction: FlipDirection.HORIZONTAL, // default
          front:Container(

            margin: EdgeInsets.only(left: 30, right: 30,),
            child: Card(
              elevation: 5,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(25.0),
              ),
              child: AspectRatio(
                  aspectRatio: 86/54,
                  child:Container(

                    decoration: BoxDecoration(
                      color: Theme.of(context).accentColor,
                      borderRadius: BorderRadius.all(Radius.circular(size.width/50)),

                    ),
                    child:  LayoutBuilder(
                        builder: (BuildContext context, BoxConstraints constraints) {
                          return Column(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: <Widget>[
                              Image.asset("assets/images/lettering_white.png", height: constraints.maxHeight*2/3,)
                            ],
                          );
                        }
                    ),

                  )
              ),
            ),
          ),

          back: Container(

            margin: EdgeInsets.only(left: 30, right: 30),

            child: Card(
              elevation: 5,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(size.width/50),
              ),
              child: AspectRatio(
                  aspectRatio: 86/54,

                  child: Container(

                    padding: EdgeInsets.all(20),
                    decoration: BoxDecoration(
                      color: Colors.grey,
                      image: DecorationImage(image: AssetImage("assets/images/backcard.jpg"), fit: BoxFit.cover,),
                      borderRadius: BorderRadius.all(Radius.circular(size.width/50)),
                    ),
                    child:  LayoutBuilder(
                        builder: (BuildContext context, BoxConstraints constraints) {
                          return Container(
                            height: constraints.maxHeight,
                            child: Column(
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              crossAxisAlignment: CrossAxisAlignment.start,
                              mainAxisSize: MainAxisSize.max,
                              children: <Widget>[
                                Image.asset("assets/images/lettering_horizzontal_white.png", width: constraints.maxWidth*2/3,),
                                Expanded(
                                  child:Container(
                                      margin: EdgeInsets.only(left: constraints.maxWidth*2/7),
                                      child: Column(
                                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                        crossAxisAlignment: CrossAxisAlignment.start,
                                        children: <Widget>[

                                          Expanded(
                                            child: Column(

                                              mainAxisAlignment: MainAxisAlignment.start,
                                              crossAxisAlignment: CrossAxisAlignment.start,
                                              children:widget.document.getElementsByTagName("span").where((e)=>e.attributes["class"]=="itemless nomeprofilo").first.text.split(" ").isEmpty?[

                                              ]:List.generate(widget.document.getElementsByTagName("span").where((e)=>e.attributes["class"]=="itemless nomeprofilo").first.text.split(" ").length, (i){
                                                return    Expanded(
                                                  child: AutoSizeText(widget.document.getElementsByTagName("span").where((e)=>e.attributes["class"]=="itemless nomeprofilo").first.text.split(" ")[i].toUpperCase().trim(), style: TextStyle(color: Colors.black, fontWeight: FontWeight.bold, fontSize: 20), textAlign: TextAlign.left, minFontSize: 0,),
                                                );
                                              }),
                                            ),
                                          ),



                                          Expanded(
                                            child: Column(
                                              mainAxisAlignment: MainAxisAlignment.end,
                                              crossAxisAlignment: CrossAxisAlignment.start,
                                              children: <Widget>[
                                                Expanded(

                                                  child:Container(

                                                      alignment: Alignment.bottomLeft,
                                                      child:AutoSizeText("Tessera", style: TextStyle(fontWeight: FontWeight.bold,), minFontSize: 0)
                                                  ),
                                                ),
                                                Expanded(
                                                  child:Container(
                                                    width: constraints.maxWidth-(constraints.maxWidth*2/7),

                                                    alignment: Alignment.bottomLeft,
                                                    child: Row(
                                                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                                      mainAxisSize: MainAxisSize.max,
                                                      crossAxisAlignment: CrossAxisAlignment.end,
                                                      children: <Widget>[
                                                        AutoSizeText(widget.document.getElementsByTagName("label").where((e)=>isNumeric(e.text)).first.text.toUpperCase().trim(), style: TextStyle(color: Colors.black, fontWeight: FontWeight.bold, fontSize: 20), minFontSize: 0),
                                                        AutoSizeText("MENSA.IT", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14), minFontSize: 0,),
                                                      ],
                                                    ),
                                                  ),
                                                )

                                              ],
                                            ),
                                          ),
                                        ],
                                      )

                                  ),
                                )
                              ],
                            ),
                          );
                        }
                    ),

                  )
              ),
            ),
          ),
        ),


        Container(
          height: 25,
        ),

        CardClipperElements(

            Container(

                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: <Widget>[

                    Container(
                      padding: EdgeInsets.all(15),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: <Widget>[

                         Expanded(
                           child:  AutoSizeText(widget.document.getElementsByTagName("span").where((e)=>e.attributes["class"]=="itemless nomeprofilo").first.text.trim(), style: TextStyle(fontWeight: FontWeight.bold, fontSize: 15),),
                         ),

                          Container(width: 5,),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.end,
                              children: <Widget>[

                                AutoSizeText("Tessera n: "+widget.document.getElementsByTagName("label").where((e)=>isNumeric(e.text)).first.text.trim(), style: TextStyle(fontWeight: FontWeight.bold), minFontSize: 0, textAlign: TextAlign.right,),

                                widget.document.getElementsByClassName("btn btn-success btn-sm btn-block").where((element) => element.text=="Rinnova").isNotEmpty?
                                AutoSizeText("Scadenza: "+widget.document.getElementsByTagName("label").where((e)=>!isNumeric(e.text)).first.text.trim(), style: TextStyle(), minFontSize: 0, textAlign: TextAlign.right,):
                                (DateTime.parse(
                                    widget.document.getElementsByTagName("label").where((e)=>!isNumeric(e.text)).first.text.trim().split("/")[2]+"-"+
                                        widget.document.getElementsByTagName("label").where((e)=>!isNumeric(e.text)).first.text.trim().split("/")[1]+"-"+
                                        widget.document.getElementsByTagName("label").where((e)=>!isNumeric(e.text)).first.text.trim().split("/")[0]
                                ).difference(DateTime.now())).inDays<30?
                                AutoSizeText("RINNOVO IN CORSO", style: TextStyle(), minFontSize: 0, textAlign: TextAlign.right,):
                                AutoSizeText("Scadenza: "+widget.document.getElementsByTagName("label").where((e)=>!isNumeric(e.text)).first.text.trim(), style: TextStyle(), minFontSize: 0, textAlign: TextAlign.right, maxLines: 1,)

                              ],
                            ),
                          )
                        ],
                      ),
                    ),
                    (DateTime.parse(
                        widget.document.getElementsByTagName("label").where((e)=>!isNumeric(e.text)).first.text.trim().split("/")[2]+"-"+
                            widget.document.getElementsByTagName("label").where((e)=>!isNumeric(e.text)).first.text.trim().split("/")[1]+"-"+
                            widget.document.getElementsByTagName("label").where((e)=>!isNumeric(e.text)).first.text.trim().split("/")[0]
                    ).difference(DateTime.now())).inDays<30&&widget.document.getElementsByClassName("btn btn-success btn-sm btn-block").where((element) => element.text=="Rinnova").isNotEmpty?GestureDetector(
                      onTap: () async {

                        await NavigateTo(context).page(RenewCardPage());
                        reload();
                      },
                      child: Container(
                        padding: EdgeInsets.only(left: 20, bottom: 10, top: 10),
                        decoration: BoxDecoration(
                            color: Colors.red.withOpacity(0.15)
                        ),
                        child: Row(
                          children: <Widget>[
                            Expanded(
                              child: AutoSizeText("La tua tesserà scadrà tra "+(DateTime.parse(
                                  widget.document.getElementsByTagName("label").where((e)=>!isNumeric(e.text)).first.text.trim().split("/")[2]+"-"+
                                      widget.document.getElementsByTagName("label").where((e)=>!isNumeric(e.text)).first.text.trim().split("/")[1]+"-"+
                                      widget.document.getElementsByTagName("label").where((e)=>!isNumeric(e.text)).first.text.trim().split("/")[0]
                              ).difference(DateTime.now())).inDays.toString()+" giorni.", style: TextStyle(fontWeight: FontWeight.bold),),
                            ),
                            Container(
                              padding: EdgeInsets.only(left: 10, right: 10),
                              child: Icon(Icons.arrow_forward),
                            )
                          ],
                        ),
                      ),
                    ):Container(),


                  ],
                )
            )
        ),

        Container(height: 10,),
        Divider(endIndent: 80, indent: 80, color: Theme.of(context).accentColor,height: 5,),
        Container(height: 20,),
        CardClipperElements(
            Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: <Widget>[
                Container(
                  width: double.infinity,
                  padding: EdgeInsets.all(10),
                  decoration: BoxDecoration(
                      color: Color(0xFF184295)
                  ),
                  child: AutoSizeText("GRUPPO UFFICIALE".toUpperCase(), style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 20),textAlign: TextAlign.center,),
                ),
                GestureDetector(
                  onTap: (){
                    tryToLunchUrl("https://www.facebook.com/groups/MensaItalia/");
                  },
                  child: Container(
                    width: MediaQuery.of(context).size.width,
                    decoration: BoxDecoration(
                    ),
                    child: ClipRRect(
                      child: CachedNetworkImage(
                        imageUrl:"https://www.mensaitalia.it/wp-content/uploads/2019/12/sig_miog.jpg",
                        width: MediaQuery.of(context).size.width,
                        errorWidget: (d,s,t)=>CachedNetworkImage(imageUrl: "https://www.mensaitalia.it/wp-content/uploads/2019/12/sig_miog_error.jpg",),
                      ),
                    ),
                  ),
                )
              ],
            )
        ),

        CardClipperElements(
            Container(

              color: Color(0xFF184295),
              child:  Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: <Widget>[
                  Container(
                    width: double.infinity,
                    padding: EdgeInsets.all(10),
                    decoration: BoxDecoration(
                        color: Color(0xFF184295)
                    ),
                    child: AutoSizeText("LA TUA VITA ASSOCIATIVA".toUpperCase(), style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 20),textAlign: TextAlign.center,),
                  ),
                  GestureDetector(
                    onTap: (){

                      NavigateTo(context).page(LOCALSMensa());
                    },

                    child: Container(
                        margin: EdgeInsets.only(bottom: 1),
                        alignment: Alignment.center,
                        color: Colors.white,
                        padding: EdgeInsets.only(left: 10, top: 20, bottom: 20),
                        child: Row(
                          children: <Widget>[
                            Expanded(
                                child: AutoSizeText.rich(TextSpan(
                                    children: [
                                      TextSpan(text: "GRUPPI LOCALI", style: TextStyle(fontWeight: FontWeight.bold)),
                                    ]
                                ), style: TextStyle(color: Theme.of(context).accentColor), textAlign: TextAlign.start)
                            ),
                            Container(
                              padding: EdgeInsets.only(left: 10, right: 10),
                              child: Icon(Icons.arrow_forward),
                            )
                          ],
                        )
                    ),
                  ),
                  GestureDetector(
                    onTap: (){

                      NavigateTo(context).page(SIGMensa());
                    },

                    child: Container(
                      alignment: Alignment.center,
                      color: Colors.white,
                      padding: EdgeInsets.only(left: 10, top: 20, bottom: 20),
                      child: Row(
                        children: <Widget>[
                          Expanded(
                              child: AutoSizeText.rich(TextSpan(
                                  children: [
                                    TextSpan(text: "SIGs ", style: TextStyle(fontWeight: FontWeight.bold)),
                                    TextSpan(text: "(Special Interest Groups)"),
                                  ]
                              ), style: TextStyle(color: Theme.of(context).accentColor), textAlign: TextAlign.start)
                          ),
                          Container(
                            padding: EdgeInsets.only(left: 10, right: 10),
                            child: Icon(Icons.arrow_forward),
                          )
                        ],
                      ),
                    ),
                  ),
                ],
              ),
            )

        ),


        CardClipperElements(
            YoutubeMensaPlayer()
        ),


        Container(height: 10,),
        Divider(endIndent: 80, indent: 80, color: Theme.of(context).accentColor,height: 5,),
        Container(height: 20,),

        BlogBlock(),


        CardClipperElements(
            Column(
              children: <Widget>[
                Container(
                  width: double.infinity,
                  padding: EdgeInsets.all(10),
                  decoration: BoxDecoration(
                      color: Color(0xFF184295)
                  ),
                  child: AutoSizeText("Comunicazioni".toUpperCase(), style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 20),textAlign: TextAlign.center,),
                ),
                elaborateTable("comunicazioni")
              ],
            )
        ),

        CardClipperElements(
            Column(
              children: <Widget>[
                Container(
                  width: double.infinity,
                  padding: EdgeInsets.all(10),
                  decoration: BoxDecoration(
                      color: Color(0xFF184295)
                  ),
                  child: AutoSizeText("Documenti".toUpperCase(), style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 20),textAlign: TextAlign.center,),
                ),
                elaborateTable("documenti"),
              ],
            )
        ),

        CardClipperElements(
            Column(
              children: <Widget>[
                Container(
                  width: double.infinity,
                  padding: EdgeInsets.all(10),
                  decoration: BoxDecoration(
                      color: Color(0xFF184295)
                  ),
                  child: AutoSizeText("Informazioni".toUpperCase(), style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 20),textAlign: TextAlign.center,),
                ),

                SingleChildScrollView(
                  scrollDirection: Axis.horizontal,
                  child: Row(
                    children: <Widget>[
                      GestureDetector(
                        onTap: () async {
                          tryToLunchUrl('https://www.mensaitalia.it/cose-il-mensa/');
                        },
                        child: Container(
                          margin: EdgeInsets.all(20),
                          child: Column(
                            mainAxisSize: MainAxisSize.min,
                            children: <Widget>[
                              Container(
                                  width: 60,
                                  height: 60,
                                  margin: EdgeInsets.only(bottom: 10),
                                  alignment: Alignment.center,
                                  decoration: BoxDecoration(
                                      color: Theme.of(context).accentColor,
                                      borderRadius: BorderRadius.circular(200)
                                  ),
                                  child: Icon(Icons.whatshot, color: Colors.white,)
                              ),

                              AutoSizeText("Cosa siamo".toUpperCase(), style: TextStyle(color: Theme.of(context).accentColor, fontWeight: FontWeight.bold, fontSize: 10),textAlign: TextAlign.center,),
                            ],
                          ),
                        ),

                      ),
                      GestureDetector(
                        onTap: () async {
                          tryToLunchUrl('https://www.mensaitalia.it/storia/');
                        },
                        child: Container(
                          margin: EdgeInsets.all(20),
                          child: Column(
                            mainAxisSize: MainAxisSize.min,
                            children: <Widget>[
                              Container(
                                  width: 60,
                                  height: 60,
                                  margin: EdgeInsets.only(bottom: 10),
                                  alignment: Alignment.center,
                                  decoration: BoxDecoration(
                                      color: Theme.of(context).accentColor,
                                      borderRadius: BorderRadius.circular(200)
                                  ),
                                  child: Icon(Icons.change_history, color: Colors.white,)
                              ),

                              AutoSizeText("storia".toUpperCase(), style: TextStyle(color: Theme.of(context).accentColor, fontWeight: FontWeight.bold, fontSize: 10),textAlign: TextAlign.center,),
                            ],
                          ),
                        ),

                      ),
                      GestureDetector(
                        onTap: () async {
                          tryToLunchUrl('https://www.mensaitalia.it/statuto/');
                        },
                        child: Container(
                          margin: EdgeInsets.all(20),
                          child: Column(
                            mainAxisSize: MainAxisSize.min,
                            children: <Widget>[
                              Container(
                                  width: 60,
                                  height: 60,
                                  margin: EdgeInsets.only(bottom: 10),
                                  alignment: Alignment.center,
                                  decoration: BoxDecoration(
                                      color: Theme.of(context).accentColor,
                                      borderRadius: BorderRadius.circular(200)
                                  ),
                                  child: Icon(Icons.wb_incandescent, color: Colors.white,)
                              ),

                              AutoSizeText("statuto".toUpperCase(), style: TextStyle(color: Theme.of(context).accentColor, fontWeight: FontWeight.bold, fontSize: 10),textAlign: TextAlign.center,),
                            ],
                          ),
                        ),

                      ),
                      GestureDetector(
                        onTap: () async {
                          tryToLunchUrl('https://www.mensaitalia.it/domande-frequenti/');
                        },
                        child: Container(
                          margin: EdgeInsets.all(20),
                          child: Column(
                            mainAxisSize: MainAxisSize.min,
                            children: <Widget>[
                              Container(
                                  width: 60,
                                  height: 60,
                                  margin: EdgeInsets.only(bottom: 10),
                                  alignment: Alignment.center,
                                  decoration: BoxDecoration(
                                      color: Theme.of(context).accentColor,
                                      borderRadius: BorderRadius.circular(200)
                                  ),
                                  child: Icon(Icons.question_answer, color: Colors.white,)
                              ),

                              AutoSizeText("domande".toUpperCase(), style: TextStyle(color: Theme.of(context).accentColor, fontWeight: FontWeight.bold, fontSize: 10),textAlign: TextAlign.center,),
                            ],
                          ),
                        ),

                      ),

                    ],
                  ),
                ),
              ],
            )
        ),


        Container(
          height: 100,
        ),
        Container(
          child: GestureDetector(
              onTap: (){
                tryToLunchUrl('https://sipio.it');
              },
              child:  Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: <Widget>[
                  AutoSizeText("Thought by ", style: TextStyle(fontStyle: FontStyle.italic, color: Colors.black.withOpacity(0.3)), textAlign: TextAlign.center,),
                  AutoSizeText("Matteo Sipione", style: TextStyle(fontStyle: FontStyle.italic, color: Colors.black.withOpacity(0.3), fontWeight: FontWeight.bold), textAlign: TextAlign.center,),

                ],
              )
          ),
          margin: EdgeInsets.only(bottom: 10),
          padding: EdgeInsets.all(5),
        ),

        Container(
          height: 100,
        ),

      ],
    );


    return GestureDetector(
        onTap: () {

          FocusScope.of(context).requestFocus(new FocusNode());
        },
        child: Scaffold(


            drawer: size.width>=600?null:MensaDrawer(widget.document, reload),


            appBar: AppBar(
              title: AutoSizeText("MENSA"),
              actions: size.width>=600?null:[
                IconButton(
                  icon: Icon(Icons.phone, color: Theme.of(context).primaryTextTheme.title.color,),
                  onPressed: () {

                    NavigateTo(context).page(PhoneBook());
                  },
                ),
              ],
            ),




            body: size.width>=600?Row(
              children: [
                Container(
                  width: size.width/2,
                  child: MensaDrawer(widget.document, reload)
                ),
                Container(
                  width: size.width/2,
                  child: BaseBlock
                )

              ],
            ):BaseBlock
        )

    );
  }


  tryToLunchUrl(String url) async {

    if (await canLaunch(url)) {
      await launch(url);
    }
  }

  Widget elaborateTable(String point){


    var el=widget.document.getElementsByTagName("div").where((e)=>e.attributes["class"]=="panel panel-primary "+point).first;
    List<prefix1.Element> element=el.getElementsByTagName("table").first.getElementsByTagName("tr");

    List<Widget> lista=[];

    element.forEach((e){

      List<Widget> row=[];



      String link;
      for(int i=0;i<e.getElementsByTagName("td").length;i++){

        if(i==0){
          row.add( Container(
            height: 50,
            width: 50,
            decoration: BoxDecoration(
                image: DecorationImage(
                    fit: BoxFit.fitHeight,
                    image: CachedNetworkImageProvider(createLink(e.getElementsByTagName("td")[i].getElementsByTagName("img").first.attributes["src"]))
                )
            ),
          )
          );
          link=e.getElementsByTagName("td")[i].getElementsByTagName("a").first.attributes["href"];
        }else{

          if(i==1){
            row.add(Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: <Widget>[
                  Container(
                    margin: EdgeInsets.only(left: 10),
                    child: AM.Text(e.getElementsByTagName("td")[i].text.trim(), textAlign: TextAlign.start,maxLines: 1, style: TextStyle(fontWeight: FontWeight.bold), overflow: TextOverflow.ellipsis,),
                  ),
                  Container(
                    margin: EdgeInsets.only(left: 10),
                    child: AM.Text(e.getElementsByTagName("td")[i+1].text.trim(), textAlign: TextAlign.start,maxLines: 1, style: TextStyle(fontSize: 10), overflow: TextOverflow.ellipsis,),
                  )
                ],
              ),
            ));
            i++;
          }else{
            row.add( Container(
              margin: EdgeInsets.only(left: 10),
              child: i==e.getElementsByTagName("td").length-1?AutoSizeText(e.getElementsByTagName("td")[i].text.trim(), textAlign: TextAlign.end,maxLines: 1, minFontSize: 0,):AM.Text(e.getElementsByTagName("td")[i].text.trim(), textAlign: TextAlign.start,maxLines: 2, overflow: TextOverflow.ellipsis,),
            ),
            );

          }


        }
      }
      lista.add(
          BuildRowBlock(row,createLink(link),point=="comunicazioni")
      );
    });

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: lista,
    );


  }


  String createLink(String link) {

    if (link.split("://").contains("https")) {
      return (""+link);
    }else{
      return "https://www.cloud32.it"+link;


    }
  }


}


class DialogDocumentPick extends StatefulWidget {

  List<prefix1.Element> elements;

  DialogDocumentPick(this.elements);


  @override
  _DialogDocumentPickState createState() => _DialogDocumentPickState();
}

class _DialogDocumentPickState extends State<DialogDocumentPick> {
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
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: List.generate(widget.elements.length, (i){

                  return BlockDialogDoc(widget.elements[i].getElementsByTagName("a").first,i==0,i==widget.elements.length-1);
                }),
              ),
            )
        ));
  }
}
class BlockDialogDoc extends StatefulWidget {
  prefix1.Element element;
  bool isTop;
  bool isEnd;

  BlockDialogDoc(this.element,this.isTop, this.isEnd);
  @override
  _BlockDialogDocState createState() => _BlockDialogDocState();
}

class _BlockDialogDocState extends State<BlockDialogDoc> {
  @override
  Widget build(BuildContext context) {
    return Container(
      margin: widget.isTop?EdgeInsets.only(bottom: 5):widget.isEnd?EdgeInsets.only(top: 5):EdgeInsets.symmetric(vertical: 5),
      child: MensaButton(onPressedNew: (){

        NavigateTo(context).page(DocumentPageInfo(createLink(widget.element.attributes["href"])));
      },text: widget.element.text,radius: widget.isTop?BorderRadius.only(topLeft: Radius.circular(25),topRight: Radius.circular(25)):widget.isEnd?BorderRadius.only(bottomLeft: Radius.circular(25),bottomRight: Radius.circular(25)):BorderRadius.circular(0.0),),
    );
  }



  String createLink(String link) {

    if (link.split("://").contains("https")) {
      return (""+link);
    }else{
      return "https://www.cloud32.it"+link;

    }
  }
}







class BuildRowBlock extends StatefulWidget {

  List<Widget> row;
  String link;

  bool isCommunication=false;


  BuildRowBlock(this.row, this.link, this.isCommunication){
    isCommunication=isCommunication??false;
  }


  @override
  _BuildRowBlockState createState() => _BuildRowBlockState();
}

class _BuildRowBlockState extends State<BuildRowBlock> {
  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: (){
        if(widget.isCommunication){

          NavigateTo(context).page(ShowCommunicationPage(widget.link));

        }else{

          NavigateTo(context).page(ShowDocumentPage(widget.link));


        }
      },
      child: Container(
        margin: EdgeInsets.symmetric(vertical: 10, horizontal: 10),
        child: Row(
          children: widget.row,
        ),
      ),
    );
  }
}


class ShowCommunicationPage extends StatefulWidget {
  String link;

  ShowCommunicationPage(this.link);

  @override
  _ShowCommunicationPageState createState() => _ShowCommunicationPageState();
}

class _ShowCommunicationPageState extends State<ShowCommunicationPage> {


  @override
  void initState() {
    // TODO: implement initState
    super.initState();
    prepare();
  }

  Document document;

  prepare() async {
    document=await API().getData(widget.link);
    setState(() {

    });
  }


  @override
  Widget build(BuildContext context) {


    return Scaffold(
      appBar: AppBar(
        title: AutoSizeText("COMUNICAZIONE"),
      ),
      body: ListView(
        padding: EdgeInsets.all(20),
        children: [
          AutoSizeText.rich(HTML.toTextSpan(context,
              document!=null?document.getElementsByTagName("div").where((e)=>e.attributes["class"]=="panel-body").first.outerHtml:"<div></div>",
              defaultTextStyle: TextStyle(
                fontSize: 12,
                decoration: TextDecoration.none,
              ),
          ))

        ],
      ),
    );
  }
}




class ShowDocumentPage extends StatefulWidget {

  String link;
  ShowDocumentPage(this.link);

  @override
  _ShowDocumentPageState createState() => _ShowDocumentPageState();
}

class _ShowDocumentPageState extends State<ShowDocumentPage> {


  bool _isLoading=true;

  @override
  void initState() {
    // TODO: implement initState
    super.initState();
    prepare();
  }


  String pathPDF;
  prepare() async {
    _isLoading=true;

    pathPDF = (await API().getFile(widget.link)).path;
    _isLoading=false;
    setState(() {

    });
  }



  Size size;
  @override
  Widget build(BuildContext context) {
    size=MediaQuery.of(context).size;
    return Scaffold(
      appBar: AppBar(
        title: AutoSizeText("Documento".toUpperCase()),
      ),
      body: Container(
        height: size.height,
        width: size.width,
        child: _isLoading
            ?Center(
            child:  Center(child: CircularProgressIndicator())

        ):PdfViewer(
          filePath: pathPDF,
        ),
      ),
    );
  }
}




class TrianglePainter extends CustomPainter {
  final Color strokeColor;
  final PaintingStyle paintingStyle;
  final double strokeWidth;

  TrianglePainter({this.strokeColor = Colors.black, this.strokeWidth = 3, this.paintingStyle = PaintingStyle.stroke});

  @override
  void paint(Canvas canvas, Size size) {
    Paint paint = Paint()
      ..color = strokeColor
      ..strokeWidth = strokeWidth
      ..style = paintingStyle;

    canvas.drawPath(getTrianglePath(size.width, size.height), paint);
  }

  Path getTrianglePath(double x, double y) {
    return Path()
      ..moveTo(0, 0)
      ..lineTo(x,0)
      ..lineTo(0,y)
      ..lineTo(0,0);
  }

  @override
  bool shouldRepaint(TrianglePainter oldDelegate) {
    return oldDelegate.strokeColor != strokeColor ||
        oldDelegate.paintingStyle != paintingStyle ||
        oldDelegate.strokeWidth != strokeWidth;
  }
}