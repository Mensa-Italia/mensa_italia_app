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
import 'package:path_provider/path_provider.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'document.dart';
import 'login.dart';
import 'package:cookie_jar/cookie_jar.dart';

import 'main.dart';


class MensaPage extends StatefulWidget {


  Document document;

  MensaPage(this.document);

  @override
  _MensaPageState createState() => _MensaPageState();
}

class _MensaPageState extends State<MensaPage> {

  bool isNumeric(String s) {
    if(s == null) {
      return false;
    }
    return double.parse(s, (e) => null) != null;
  }



  Size size;
  @override
  Widget build(BuildContext context) {
    size=MediaQuery.of(context).size;



    return Scaffold(



        body:  new GestureDetector(
            onTap: () {

              FocusScope.of(context).requestFocus(new FocusNode());
            },
            child:Container(
                width: size.width,

                height: size.height,
                child: Stack(
                    children: [

                      Image.network("https://www.cloud32.it"+widget.document.getElementsByTagName("img").where((e)=>e.attributes["alt"]=="Foto").first.attributes["src"]),
                      Positioned(
                        top: -20,
                        bottom: 0,
                        left: 0,
                        right: 0,
                        child: SingleChildScrollView(

                            child: Column(
                                crossAxisAlignment: CrossAxisAlignment.center,
                                mainAxisAlignment: MainAxisAlignment.center,
                                children: <Widget>[
                                  Opacity(
                                    opacity: 0.0,
                                    child: Image.network("https://www.cloud32.it"+widget.document.getElementsByTagName("img").where((e)=>e.attributes["alt"]=="Foto").first.attributes["src"]),
                                  ),
                                  Container(
                                    width: size.width,
                                    decoration: BoxDecoration(
                                        borderRadius: BorderRadius.only(topLeft: Radius.circular(25), topRight: Radius.circular(25)),
                                        color: Colors.white,
                                        border: Border.all(color: Color(0xFF184295)),
                                        boxShadow: [
                                          BoxShadow(
                                              color: Colors.black,
                                              blurRadius: 3.0
                                          )
                                        ]
                                    ),
                                    child: Column(
                                      children: <Widget>[
                                        Container(

                                          padding: EdgeInsets.all(15),
                                          child: Row(
                                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                            children: <Widget>[

                                              AutoSizeText(widget.document.getElementsByTagName("span").where((e)=>e.attributes["class"]=="itemless nomeprofilo").first.text, style: TextStyle(fontWeight: FontWeight.bold, fontSize: 15),),

                                              Column(
                                                crossAxisAlignment: CrossAxisAlignment.end,
                                                children: <Widget>[

                                                  AutoSizeText("Tessera n: "+widget.document.getElementsByTagName("label").where((e)=>isNumeric(e.text)).first.text, style: TextStyle(fontWeight: FontWeight.bold),),

                                                  AutoSizeText("Scadenza: "+widget.document.getElementsByTagName("label").where((e)=>!isNumeric(e.text)).first.text, style: TextStyle(),),

                                                ],
                                              )
                                            ],
                                          ),
                                        ),




                                        FlipCard(
                                          direction: FlipDirection.HORIZONTAL, // default
                                          front: Container(
                                            margin: EdgeInsets.all(30),
                                            child: AspectRatio(
                                              aspectRatio: 16/10,
                                              child: Container(

                                                decoration: BoxDecoration(
                                                    color: Colors.black,
                                                    borderRadius: BorderRadius.all(Radius.circular(25)),
                                                    boxShadow: [
                                                      BoxShadow(
                                                          color: Colors.black,
                                                          blurRadius: 5.0
                                                      )
                                                    ]
                                                ),


                                                child: Column(

                                                  mainAxisSize: MainAxisSize.max,
                                                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                                  children: <Widget>[



                                                    Row(
                                                      crossAxisAlignment: CrossAxisAlignment.center,
                                                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                                      children: <Widget>[
                                                        Container(
                                                          padding: EdgeInsets.only(left: 20, top: 10, right: 20, bottom: 10),
                                                          child: Column(
                                                            crossAxisAlignment: CrossAxisAlignment.start,
                                                            children: <Widget>[
                                                              AutoSizeText("Scadenza", style: TextStyle(fontWeight: FontWeight.bold, color: Colors.white),),
                                                              AutoSizeText(widget.document.getElementsByTagName("label").where((e)=>!isNumeric(e.text)).first.text, style: TextStyle(fontWeight: FontWeight.bold, color: Colors.white),),
                                                            ],
                                                          ),
                                                        ),
                                                        Container(

                                                            padding: EdgeInsets.all(25),

                                                            child: Image.asset("assets/images/logo.png", height: 50,)
                                                        ),
                                                      ],
                                                    ),

                                                    Row(
                                                      crossAxisAlignment: CrossAxisAlignment.end,
                                                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                                      children: <Widget>[
                                                        Row(
                                                          children: <Widget>[
                                                            Container(
                                                              margin: EdgeInsets.only(bottom: 25),
                                                              decoration: BoxDecoration(
                                                                  color: Colors.grey
                                                              ),
                                                              padding: EdgeInsets.only(left: 20, top: 10, right: 10, bottom: 10),
                                                              child: AutoSizeText(widget.document.getElementsByTagName("label").where((e)=>isNumeric(e.text)).first.text, style: TextStyle(fontWeight: FontWeight.bold),),
                                                            ),
                                                            Container(
                                                              margin: EdgeInsets.only(bottom: 25),
                                                              child: CustomPaint(
                                                                painter: TrianglePainter(
                                                                  strokeColor: Colors.grey,
                                                                  strokeWidth: 10,
                                                                  paintingStyle: PaintingStyle.fill,
                                                                ),
                                                                child:Container(
                                                                  padding: EdgeInsets.only(left: 20, top: 10, right: 10, bottom: 10),
                                                                  child:  AutoSizeText("",),
                                                                ),
                                                              ),
                                                            )
                                                          ],
                                                        ),
                                                        Container(
                                                          margin: EdgeInsets.only(bottom: 25),
                                                          padding: EdgeInsets.only(left: 20, top: 10, right: 20, bottom: 10),
                                                          child: AutoSizeText(widget.document.getElementsByTagName("span").where((e)=>e.attributes["class"]=="itemless nomeprofilo").first.text, style: TextStyle(fontWeight: FontWeight.bold, color: Color(0xFFfaaa00)),),
                                                        ),
                                                      ],
                                                    ),

                                                  ],
                                                ),
                                              ),
                                            ),
                                          ),
                                          back: Container(
                                            margin: EdgeInsets.all(30),
                                            child: AspectRatio(
                                              aspectRatio: 16/10,
                                              child: Container(

                                                decoration: BoxDecoration(
                                                    color: Colors.black,
                                                    borderRadius: BorderRadius.all(Radius.circular(25)),
                                                    boxShadow: [
                                                      BoxShadow(
                                                          color: Colors.black,
                                                          blurRadius: 5.0
                                                      )
                                                    ]
                                                ),


                                                child: Column(

                                                  mainAxisSize: MainAxisSize.max,
                                                  mainAxisAlignment: MainAxisAlignment.center,
                                                  children: <Widget>[
                                                    Container(

                                                      padding: EdgeInsets.only(left: 0, right: 8,top: 10, bottom: 10),
                                                      decoration: BoxDecoration(

                                                        color: Colors.white,
                                                        borderRadius: BorderRadius.all(Radius.circular(15)),
                                                      ),
                                                      child: Container(
                                                        width: 200,
                                                        alignment: Alignment.center,
                                                        child: BarCodeImage(
                                                          data: widget.document.getElementsByTagName("label").where((e)=>isNumeric(e.text)).first.text.trim(),              // Code string. (required)
                                                          codeType: BarCodeType.Code39, // height for the entire widget (default: 100.0)
                                                          hasText: false,
                                                          barHeight: 90.0,// Render with text label or not (default: false)
                                                          onError: (error) {             // Error handler
                                                            print('error = $error');
                                                          },
                                                        ),
                                                      ),
                                                    )



                                                  ],
                                                ),
                                              ),
                                            ),
                                          ),
                                        ),
                                        Container(
                                          height: 20,
                                        ),

                                        Row(

                                          mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                                          children: <Widget>[
                                            GestureDetector(
                                              onTap: (){

                                                print("ok");
                                                Navigator.push(context, MaterialPageRoute(builder: (d)=>RegSoci()));
                                              },
                                              child: Column(

                                                children: <Widget>[

                                                  Container(
                                                    padding: EdgeInsets.all(10),
                                                    decoration: BoxDecoration(
                                                        color: Color(0xFF184295),
                                                        borderRadius: BorderRadius.all(Radius.circular(300))
                                                    ),
                                                    child:Icon(Icons.person, size: 40, color: Colors.white,),
                                                  ),
                                                  Container(
                                                    height: 10,
                                                  ),
                                                  AutoSizeText("Registro Soci".toUpperCase(), style: TextStyle(fontWeight: FontWeight.bold),)
                                                ],
                                              ),
                                            ),

                                            GestureDetector(
                                              onTap: (){

                                                showDialog(
                                                  context: context,
                                                  builder: (d)=>DialogDocumentPick(widget.document.getElementsByTagName("li").where((e)=>e.attributes["class"]=="dropdown").where((e)=>e.getElementsByTagName("a").first.text=="Documenti").first.getElementsByTagName("ul").first.getElementsByTagName("li"))
                                                );

                                              },
                                              child: Column(

                                                children: <Widget>[

                                                  Container(
                                                    padding: EdgeInsets.all(10),
                                                    decoration: BoxDecoration(
                                                        color: Color(0xFF184295),
                                                        borderRadius: BorderRadius.all(Radius.circular(300))
                                                    ),
                                                    child:Icon(Icons.insert_drive_file, size: 40, color: Colors.white,),
                                                  ),
                                                  Container(
                                                    height: 10,
                                                  ),
                                                  AutoSizeText("Documenti".toUpperCase(), style: TextStyle(fontWeight: FontWeight.bold),)
                                                ],
                                              ),
                                            ),


                                          ],
                                        ),

                                        Container(
                                          height: 20,
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

                                        Container(height: 100,),


                                        GestureDetector(
                                          onTap: () async {

                                            Directory appDocDir = await getApplicationDocumentsDirectory();
                                            String appDocPath = appDocDir.path;
                                            var cookieJar=PersistCookieJar(dir:appDocPath+"/.cookies/");
                                            cookieJar.deleteAll();

                                            SharedPreferences prefs = await SharedPreferences.getInstance();
                                            prefs.clear();
                                            Navigator.pushReplacement(context, MaterialPageRoute( builder: (d)=>HomePage()));
                                          },
                                          child: Container(
                                            width: double.infinity,
                                            padding: EdgeInsets.all(10),
                                            decoration: BoxDecoration(
                                                color: Colors.redAccent
                                            ),
                                            child: AutoSizeText("Disconnetti".toUpperCase(), style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 20),textAlign: TextAlign.center,),
                                          ),
                                        ),

                                      ],
                                    ),
                                  )

                                ]
                            )
                        ),
                      )

                    ]
                )
            )
        )
    );
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

        print(widget.element.attributes["href"]);
        Navigator.push(context, MaterialPageRoute(builder:(d)=>DocumentPageInfo(createLink(widget.element.attributes["href"]))) );
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
          Navigator.push(context, MaterialPageRoute(builder:(d)=>ShowCommunicationPage(widget.link)));

        }else{
          Navigator.push(context, MaterialPageRoute(builder:(d)=>ShowDocumentPage(widget.link)));

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