/*
Creato da Matteo Sipion nella data del 15/10/2019.

Matteo Sipione detiene i diritti autoriali e commerciali di questo software.

-------------------------------------------------------------------------------

Created by Matteo Sipion on the date of 15/10/2019.

Matteo Sipione holds the authorial and commercial rights to this software.
*/
import 'dart:io';
import 'package:auto_size_text/auto_size_text.dart';
import 'package:barcode_flutter/barcode_flutter.dart';
import 'package:flip_card/flip_card.dart';
import 'package:flutter/material.dart';
import 'package:flutter_html/flutter_html.dart';
import 'package:flutter_plugin_pdf_viewer/flutter_plugin_pdf_viewer.dart';
import 'package:html/dom.dart';
import 'package:html/dom.dart' as prefix1;
import 'package:mensa_italia/regsoci.dart';
import 'package:mensa_italia/sig.dart';
import 'package:page_transition/page_transition.dart';
import 'package:path_provider/path_provider.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:url_launcher/url_launcher.dart';
import 'blog.dart';
import 'document.dart';
import 'drawer.dart';
import 'login.dart';
import 'package:cookie_jar/cookie_jar.dart';

import 'main.dart';


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


  @override
  void initState() {
    // TODO: implement initState
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((duration){

      precacheImage(new NetworkImage( "https://www.cloud32.it"+widget.document.getElementsByTagName("img").where((e)=>e.attributes["alt"]=="Foto").first.attributes["src"]), context);

    });

  }

  Size size;
  @override
  Widget build(BuildContext context) {
    size=MediaQuery.of(context).size;



    return Scaffold(


        drawer: MensaDrawer(widget.document),


        appBar: AppBar(
          title: AutoSizeText("MENSA ITALIA".toUpperCase()),
        ),
        body:  new GestureDetector(
            onTap: () {

              FocusScope.of(context).requestFocus(new FocusNode());
            },
            child:SingleChildScrollView(

                child: Column(
                    crossAxisAlignment: CrossAxisAlignment.center,
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: <Widget>[
                      Container(
                        width: size.width,
                        child: Column(
                          children: <Widget>[
                            Container(

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
                            ),




                            FlipCard(
                              direction: FlipDirection.HORIZONTAL, // default
                              front:Container(

                                margin: EdgeInsets.all(30),
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

                                margin: EdgeInsets.all(30),

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

                            GestureDetector(
                              onTap: (){
                               
                                Navigator.push(context, PageTransition(child: SIGMensa(), type: PageTransitionType.rightToLeft));
                              },
                              child: Container(
                                margin: EdgeInsets.all(20),
                                padding: EdgeInsets.all(20),
                                decoration: BoxDecoration(
                                    border: Border.all(color: Theme.of(context).accentColor),
                                    borderRadius: BorderRadius.circular(25)
                                ),
                                child:Row(
                                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                  children: <Widget>[
                                    Expanded(
                                      child: AutoSizeText("Vivi al meglio la vita associativa, trova il SIG perfetto per te", style: TextStyle(color: Theme.of(context).accentColor),),
                                    ),
                                    Container(
                                      width: 40,
                                    ),
                                    Icon(
                                        Icons.arrow_forward,
                                        color: Theme.of(context).accentColor
                                    )

                                  ],
                                ),
                              ),
                            ),

                            Container(
                              width: double.infinity,
                              padding: EdgeInsets.all(10),
                              decoration: BoxDecoration(
                                  color: Color(0xFF184295)
                              ),
                              child: AutoSizeText("Eventi".toUpperCase(), style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 20),textAlign: TextAlign.center,),
                            ),

                            BlogBlock(),

                            GestureDetector(
                              onTap: (){

                                Navigator.push(context, PageTransition(child: BlogMensa(title:"Eventi"), type: PageTransitionType.rightToLeft));
                              },
                              child: Container(
                                margin: EdgeInsets.all(20),
                                padding: EdgeInsets.all(20),
                                decoration: BoxDecoration(
                                    border: Border.all(color: Theme.of(context).accentColor),
                                    borderRadius: BorderRadius.circular(25)
                                ),
                                child:Row(
                                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                  children: <Widget>[
                                    Expanded(
                                      child: AutoSizeText("Vedi tutti gli eventi", style: TextStyle(color: Theme.of(context).accentColor),),
                                    ),
                                    Container(
                                      width: 40,
                                    ),
                                    Icon(
                                        Icons.arrow_forward,
                                        color: Theme.of(context).accentColor
                                    )

                                  ],
                                ),
                              ),
                            ),



                            Container(
                              width: double.infinity,
                              padding: EdgeInsets.all(10),
                              decoration: BoxDecoration(
                                  color: Color(0xFF184295)
                              ),
                              child: AutoSizeText("Comunicazioni".toUpperCase(), style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 20),textAlign: TextAlign.center,),
                            ),
                            elaborateTable("comunicazioni"),

                            Container(
                              width: double.infinity,
                              padding: EdgeInsets.all(10),
                              decoration: BoxDecoration(
                                  color: Color(0xFF184295)
                              ),
                              child: AutoSizeText("Documenti".toUpperCase(), style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 20),textAlign: TextAlign.center,),
                            ),
                            elaborateTable("documenti"),


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
                        ),
                      )

                    ]
                )
            )
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
          row.add(Flexible(
              child: Image.network(createLink(e.getElementsByTagName("td")[i].getElementsByTagName("img").first.attributes["src"]),height: 50,width: 50,)
          ));
          link=e.getElementsByTagName("td")[i].getElementsByTagName("a").first.attributes["href"];
        }else{


          row.add(Flexible(
            child: AutoSizeText(e.getElementsByTagName("td")[i].text.trim(), textAlign: TextAlign.start,),
          ));
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
        margin: EdgeInsets.symmetric(vertical: 10),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceEvenly,
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


  PDFDocument doc;
  prepare() async {
    _isLoading=true;

    doc = await PDFDocument.fromFile(await API().getFile(widget.link));
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
                : PDFViewer(document: doc)),
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