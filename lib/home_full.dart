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
import 'package:barcode_flutter/barcode_flutter.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:flip_card/flip_card.dart';
import 'package:flutter/animation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_full_pdf_viewer/full_pdf_viewer_scaffold.dart';
import 'package:flutter_html/flutter_html.dart';
import 'package:html/dom.dart';
import 'package:html/dom.dart' as prefix1;
import 'package:mensa_italia/phone_book.dart';
import 'package:mensa_italia/regsoci.dart';
import 'package:mensa_italia/sig.dart';
import 'package:mensa_italia/youtube.dart';
import 'package:page_transition/page_transition.dart';
import 'package:path_provider/path_provider.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:shimmer/shimmer.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:youtube_player_flutter/youtube_player_flutter.dart';
import 'blog.dart';
import 'document.dart';
import 'drawer.dart';
import 'login.dart';
import 'package:cookie_jar/cookie_jar.dart';


import 'package:flutter/material.dart' as AM;

import 'main.dart';

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
                        Navigator.push(context, PageTransition(child: PhoneBook(), type: PageTransitionType.rightToLeft));
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

  @override
  void initState() {
    // TODO: implement initState
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((duration){

      precacheImage(new NetworkImage( "https://www.cloud32.it"+widget.document.getElementsByTagName("img").where((e)=>e.attributes["alt"]=="Foto").first.attributes["src"]), context);

    });

    scrollController.addListener(() { toDoWhileOnMove();});
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



    return Scaffold(


        drawer: MensaDrawer(widget.document),



        body:  Stack(
          children: <Widget>[

            Positioned.fill(child: new GestureDetector(
              onTap: () {

                FocusScope.of(context).requestFocus(new FocusNode());
              },
              child: CustomScrollView(
                controller: scrollController,
                slivers: <Widget>[
                  TransitionAppBar(),
                  SliverList(
                    delegate: SliverChildListDelegate( <Widget>[

                      Container(
                        height: 25,
                      ),


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
                                    borderRadius: BorderRadius.all(Radius.circular(25)),

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
                              borderRadius: BorderRadius.circular(25.0),
                            ),
                            child: AspectRatio(
                                aspectRatio: 86/54,

                                child: Container(

                                  padding: EdgeInsets.all(20),
                                  decoration: BoxDecoration(
                                    color: Colors.grey,
                                    image: DecorationImage(image: AssetImage("assets/images/backcard.jpg"), fit: BoxFit.cover,),
                                    borderRadius: BorderRadius.all(Radius.circular(25)),
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



                      CardClipperElements(Container(

                        padding: EdgeInsets.all(15),
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: <Widget>[

                            AutoSizeText(widget.document.getElementsByTagName("span").where((e)=>e.attributes["class"]=="itemless nomeprofilo").first.text.trim(), style: TextStyle(fontWeight: FontWeight.bold, fontSize: 15),),

                            Column(
                              crossAxisAlignment: CrossAxisAlignment.end,
                              children: <Widget>[

                                AutoSizeText("Tessera n: "+widget.document.getElementsByTagName("label").where((e)=>isNumeric(e.text)).first.text.trim(), style: TextStyle(fontWeight: FontWeight.bold),),

                                AutoSizeText("Scadenza: "+widget.document.getElementsByTagName("label").where((e)=>!isNumeric(e.text)).first.text.trim(), style: TextStyle(),),

                              ],
                            )
                          ],
                        ),
                      )
                      ),

                      Divider(endIndent: 80, indent: 80, color: Theme.of(context).accentColor,height: 40,),


                      CardClipperElements(GestureDetector(
                        onTap: (){

                          Navigator.push(context, PageTransition(child: SIGMensa(), type: PageTransitionType.rightToLeft));
                        },

                        child: Stack(
                          children: <Widget>[
                            Shimmer.fromColors(
                                child: Container(
                                  padding: EdgeInsets.all(20),
                                  decoration: BoxDecoration(
                                      borderRadius: BorderRadius.circular(15),
                                      color:Colors.black
                                  ),
                                  child:Row(
                                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                    children: <Widget>[
                                      Expanded(
                                          child: Container(
                                            padding: EdgeInsets.all(5),
                                            child: AutoSizeText.rich(TextSpan(
                                                children: [
                                                  TextSpan(text: "Vivi al meglio la vita associativa, trova il"),
                                                  TextSpan(text: " SIG ", style: TextStyle(fontWeight: FontWeight.bold)),
                                                  TextSpan(text: "perfetto per te"),
                                                ]
                                            ), style: TextStyle(color: Colors.white),),
                                          )
                                      ),
                                      Container(
                                        padding: EdgeInsets.only(left: 20,),
                                        child: Icon(
                                            Icons.arrow_forward,
                                            color: Colors.white
                                        ),
                                      ),


                                    ],
                                  ),
                                ),

                                baseColor: Theme.of(context).accentColor,
                                highlightColor: Colors.red
                            ),
                            Container(
                              padding: EdgeInsets.all(20),
                              decoration: BoxDecoration(
                                  borderRadius: BorderRadius.circular(25)
                              ),
                              child:Row(
                                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                children: <Widget>[
                                  Expanded(
                                      child: Container(
                                        padding: EdgeInsets.all(5),
                                        child: AutoSizeText.rich(TextSpan(
                                            children: [
                                              TextSpan(text: "Vivi al meglio la vita associativa, trova il"),
                                              TextSpan(text: " SIG ", style: TextStyle(fontWeight: FontWeight.bold)),
                                              TextSpan(text: "perfetto per te"),
                                            ]
                                        ), style: TextStyle(color: Colors.white),),
                                      )
                                  ),
                                  Container(
                                    padding: EdgeInsets.only(left: 20,),
                                    child: Icon(
                                        Icons.arrow_forward,
                                        color: Colors.white
                                    ),
                                  ),


                                ],
                              ),
                            )
                          ],
                        ),
                      )),

                      Divider(endIndent: 80, indent: 80, color: Theme.of(context).accentColor,height: 40,),


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
                        height: 200,
                      )

                    ],
                    ),
                  )
                ],
              ),
            )),

            Positioned(
              bottom: 0,
              left: 0,
              right: 0,
              child:       Container(
                child: YoutubeMensaPlayer(),
              ),
            )

          ],
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
      return link;
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
        Navigator.push(context, PageTransition(type: PageTransitionType.rightToLeft, child:DocumentPageInfo(createLink(widget.element.attributes["href"]))) );
      },text: widget.element.text,radius: widget.isTop?BorderRadius.only(topLeft: Radius.circular(25),topRight: Radius.circular(25)):widget.isEnd?BorderRadius.only(bottomLeft: Radius.circular(25),bottomRight: Radius.circular(25)):BorderRadius.circular(0.0),),
    );
  }



  String createLink(String link) {

    if (link.split("://").contains("https")) {
      return link;
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
          Navigator.push(context, PageTransition(type: PageTransitionType.rightToLeft, child:ShowCommunicationPage(widget.link)));

        }else{
          Navigator.push(context, PageTransition(type: PageTransitionType.rightToLeft, child:ShowDocumentPage(widget.link)));

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
      body: SingleChildScrollView(
        padding: EdgeInsets.all(10),
        child: Column(
          children: <Widget>[

            Html(
              data: document!=null?document.getElementsByTagName("div").where((e)=>e.attributes["class"]=="panel-body").first.outerHtml:"",
            )


          ],
        ),
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
        child: Center(
            child: _isLoading
                ? Center(child: CircularProgressIndicator())
                : PDFViewerScaffold(
                appBar: AppBar(
                  title: AutoSizeText("Documento".toUpperCase()),
                ),
                path: pathPDF
            )
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