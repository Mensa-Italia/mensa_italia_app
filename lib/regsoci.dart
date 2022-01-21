/*
Creato da Matteo Sipion nella data del 15/10/2019.

Matteo Sipione detiene i diritti autoriali e commerciali di questo software.

-------------------------------------------------------------------------------

Created by Matteo Sipion on the date of 15/10/2019.

Matteo Sipione holds the authorial and commercial rights to this software.
*/
import 'package:auto_size_text/auto_size_text.dart';
import 'package:extended_image/extended_image.dart';
import 'package:flutter/material.dart';
import 'package:html/dom.dart';
import 'package:html/dom.dart' as dom;
import 'login.dart';

class RegSoci extends StatefulWidget {
  @override
  _RegSociState createState() => _RegSociState();
}

class _RegSociState extends State<RegSoci> {


  Document document;

  TextEditingController nameController = new TextEditingController();
  TextEditingController surnameController = new TextEditingController();



  List<dom.Element> elements=[];

  int maxPage;


  prepare() async {
    document=await API().getData("https://www.cloud32.it/Associazioni/utenti/regsocio?s_cognome="+Uri.encodeFull(surnameController.text)+"&s_nome="+Uri.encodeFull(nameController.text)+"&s_citta=&s_provincia=&s_regione=&page=1");
    try{
      maxPage=int.parse(document.getElementsByTagName("ul").where((e)=>e.attributes["class"]=="pagination").first.getElementsByTagName("li").elementAt(document.getElementsByTagName("ul").where((e)=>e.attributes["class"]=="pagination").first.getElementsByTagName("li").length-2).getElementsByTagName("a").first.text);

    }catch(e){
      maxPage=0;
    }
    elements=document.getElementsByTagName("table").where((e)=>e.attributes["class"]=="table table-hover table-striped table-condensed").first.getElementsByTagName("tbody").first.getElementsByTagName("tr");
    setState(() {

    });
  }

  @override
  void initState() {
    // TODO: implement initState
    super.initState();
    prepare();
  }


  int page=1;
  bool isLoading=false;
  addMore() async {
    isLoading=true;
    if(page<maxPage) {
      page++;




      document = await API().getData(
          "https://www.cloud32.it/Associazioni/utenti/regsocio?s_cognome="+Uri.encodeFull(surnameController.text)+"&s_nome="+Uri.encodeFull(nameController.text)+"&s_citta=&s_provincia=&s_regione=&Ricerca=Ricerca&page=" +
              page.toString());

      elements.addAll( document
          .getElementsByTagName("table")
          .where((e) =>
      e.attributes["class"] ==
          "table table-hover table-striped table-condensed")
          .first
          .getElementsByTagName("tbody")
          .first
          .getElementsByTagName("tr"));


      setState(() {

      });
    }
    isLoading=false;

  }

  @override
  Widget build(BuildContext context) {


    return Scaffold(

        appBar: AppBar(
          title: AutoSizeText("Registro soci".toUpperCase()),
        ),
        body: new GestureDetector(
            onTap: () {

              FocusScope.of(context).requestFocus(new FocusNode());
            },
            child:NotificationListener<ScrollNotification>(
              onNotification: (ScrollNotification scrollInfo) {
                if (scrollInfo.metrics.pixels==scrollInfo.metrics.maxScrollExtent&&!isLoading) {
                  addMore();
                }
                return false;
              },
              child: SingleChildScrollView(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: document!=null?[

                    Container(height: 20,),

                    Container(
                      margin: EdgeInsets.symmetric(horizontal: 20),
                      alignment: Alignment.center,
                      child: AutoSizeText("Cerca soci".toUpperCase(), minFontSize: 0, style: TextStyle(fontSize: 20, color:Color(0xFF184295),fontWeight: FontWeight.bold),maxLines: 1,),
                    ),


                    Container(height: 20,),

                    Container(
                      margin: EdgeInsets.symmetric(horizontal: 20),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: <Widget>[
                          MensaTextField("Nome", textEditingController: nameController,),

                          Container(height: 10,),
                          MensaTextField("Cognome", textEditingController: surnameController,),

                          Container(height: 10,),
                          MensaButton(text: "Cerca",onPressedNew: (){

                            if(elements!=null){
                              elements.clear();
                            }
                            elements=null;
                            setState(() {

                            });
                            prepare();
                          },)


                        ],
                      ),
                    ),

                    Container(height: 40,),

                    Container(
                      margin: EdgeInsets.symmetric(horizontal: 20),
                      alignment: Alignment.center,
                      child: AutoSizeText(document.getElementsByTagName("div").where((e)=>e.attributes["class"]=="panel-heading titolopanel").first.text.trim().toUpperCase(), minFontSize: 0, style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold),maxLines: 1,),
                    ),


                    Container(height: 10,),
                    Column(
                      children: elements==null?[
                        Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: <Widget>[
                            Container(
                              height: 20,
                            ),
                            CircularProgressIndicator(),
                            Container(
                              height: 20,
                            )
                          ],
                        )
                      ]:elements[0].attributes["class"]=="alert-danger"?[

                        Container(height: 50,),

                        Container(
                          margin: EdgeInsets.symmetric(horizontal: 20),
                          alignment: Alignment.center,
                          child: AutoSizeText("Nessun risultato disponibile".toUpperCase(), minFontSize: 0, style: TextStyle(fontSize: 12, fontStyle: FontStyle.italic),maxLines: 1,textAlign: TextAlign.center,),
                        ),



                      ]:List.generate(elements.length, (i){
                        String image;
                        String name;
                        String card;
                        String date;
                        String place1;
                        String place2;
                        String link;

                        var data=elements[i].getElementsByTagName("td");

                        for(int j=0;j<data.length;j++){
                          if(j==0){
                            try{
                              image=data[j].getElementsByTagName("img").first.attributes["src"].replaceAll("\\", "/");

                            }catch(e){

                              image="";
                            }
                          }
                          if(j==1){
                            card=data[j].text;
                          }
                          if(j==2){
                            name=data[j].text;
                          }
                          if(j==3){
                            date=data[j].text;
                          }
                          if(j==4){
                            place1=data[j].text;
                          }
                          if(j==5){
                            place2=data[j].text;
                          }
                          if(j==6){
                            link=data[j].getElementsByTagName("a").first.attributes["href"];
                          }
                        }

                        return UserBlock(image,name, card, date, place1, place2, link);
                      }),
                    ),
                    Container(height: 10,),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: <Widget>[
                        page<maxPage?CircularProgressIndicator():Container()
                      ],
                    ),
                    Container(height: 10,)

                  ]:[

                    Container(height: 50,),

                    Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: <Widget>[
                        CircularProgressIndicator()
                      ],
                    ),

                    Container(height: 50,),

                  ],
                ),
              ),
            ))
    );
  }
}




class UserBlock extends StatefulWidget {
  final String image;
  final String name;
  final String card;
  final String date;
  final String place1;
  final String place2;
  final String link;

  UserBlock(this.image, this.name, this.card, this.date, this.place1, this.place2, this.link);


  @override
  _UserBlockState createState() => _UserBlockState();
}

class _UserBlockState extends State<UserBlock> {
  @override
  Widget build(BuildContext context) {
    return Container(

      padding: EdgeInsets.symmetric(horizontal: 20, vertical: 5),
      child: Card(
        elevation: 5.0,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(15)),
        color: Theme.of(context).accentColor,
        child: GestureDetector(
          onTap: (){
            showDialog(context: context, builder: (d)=>DialogBlock(widget.image, widget.name, widget.card, widget.date, widget.place1, widget.place2, widget.link));
          },
          child: Container(
            margin: EdgeInsets.symmetric(horizontal: 20, vertical: 20),
            width: double.infinity,
            decoration: BoxDecoration(
              borderRadius: BorderRadius.all(Radius.circular(25)),
              color: Theme.of(context).accentColor,
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: <Widget>[
                Container(
                  height: 100,
                  width: 80,
                  decoration: BoxDecoration(
                      image: DecorationImage(image: NetworkImage(createLink(widget.image)),fit: BoxFit.cover),
                      borderRadius: BorderRadius.all(Radius.circular(10))
                  ),
                  child: Opacity(opacity: 0.0, child: Image.network(createLink(widget.image), height: 100,),),
                ),
                Container(width: 20,),
                Expanded(
                  child: Container(
                    height: 100,
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                      mainAxisSize: MainAxisSize.max,
                      children: <Widget>[

                        AutoSizeText((widget.name??"").toUpperCase(), minFontSize: 0, style: TextStyle(fontSize: 25, fontWeight: FontWeight.bold, color: Colors.white),maxLines: 1,),

                        AutoSizeText(widget.card, minFontSize: 0, style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Colors.white),maxLines: 1,),
                        AutoSizeText(widget.date, minFontSize: 0,style: TextStyle( color: Colors.white),maxLines: 1,),
                        AutoSizeText(widget.place1+", "+widget.place2, minFontSize: 0,style: TextStyle( color: Colors.white),maxLines: 1,),
                      ],
                    ),
                  ),
                )

              ],
            ),
          ),

        ),
      ),
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

class DialogBlock extends StatefulWidget {

  final String image;
  final String name;
  final String card;
  final String date;
  final String place1;
  final String place2;
  final String link;

  DialogBlock(this.image, this.name, this.card, this.date, this.place1, this.place2, this.link);
  @override
  _DialogBlockState createState() => _DialogBlockState();
}

class _DialogBlockState extends State<DialogBlock> {

  List<dom.Element> elements;

  init() async {
    dom.Document document = await API().getData(createLink(widget.link));


    elements=document.getElementsByClassName("form-group").where((f)=>f.getElementsByClassName("col-sm-2 col-sm-offset-3").isNotEmpty).toList();

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
    return Dialog(
        backgroundColor: Theme.of(context).accentColor,
        child:  ClipRRect(
          borderRadius: BorderRadius.circular(25),
          child:SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              mainAxisAlignment: MainAxisAlignment.center,
              crossAxisAlignment: CrossAxisAlignment.center,
              children: <Widget>[
                Container(height: 20,),
                AutoSizeText(widget.name, style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold,color: Colors.white), textAlign: TextAlign.center,),

                Container(height: 20,),
                Container(
                  height: MediaQuery.of(context).size.width*2/3,
                  width: double.infinity,
                  decoration: BoxDecoration(
                      image: DecorationImage(image: ExtendedNetworkImageProvider(createLink(widget.image)??"https://helpx.adobe.com/content/dam/help/en/stock/how-to/visual-reverse-image-search/jcr_content/main-pars/image/visual-reverse-image-search-v2_intro.jpg",), fit: BoxFit.cover)
                  ),


                ),


                elements==null?Container(
                  padding: EdgeInsets.all(20),
                  child: LoadingDialog(),
                ):Container(padding: EdgeInsets.all(20),
                  child: Column(
                    children: List.generate(elements.length, (i){

                      String secondData="";

                      if(elements.elementAt(i).getElementsByClassName("col-sm-6 col-sm-offset-1").first.children.isNotEmpty){
                        secondData=elements.elementAt(i).getElementsByClassName("col-sm-6 col-sm-offset-1").first.children.first.text;
                      }else{
                        secondData=elements.elementAt(i).getElementsByClassName("col-sm-6 col-sm-offset-1").first.text;
                      }

                      if(secondData.toLowerCase().contains('protected'.toLowerCase())){
                        String protectedEmail = elements.elementAt(i).getElementsByClassName("__cf_email__").first.attributes["data-cfemail"];
                        secondData = decodeEmail(protectedEmail);
                      }


                      return Column(

                        children: <Widget>[
                          Row(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              AutoSizeText(elements.elementAt(i).getElementsByClassName("col-sm-2 col-sm-offset-3").first.text, minFontSize: 0, style: TextStyle(fontWeight: FontWeight.bold, color: Colors.white),),

                              Container(width: 10,),
                              Expanded(
                                child:AutoSizeText(secondData, minFontSize: 0, style: TextStyle(color: Colors.white),),
                              )
                            ],
                          ),
                          Divider()
                        ],
                      );
                    }),
                  ),),

              ],
            ),
          ),
        )
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

int getHexPair(str, pos) {
  str = str.substring(pos, pos + 2);
  return int.parse(str, radix: 16);
}

String decodeEmail(token) {
  String mail = "";
  int xor = getHexPair(token, 0);

  for (int i = 2; i < token.length; i += 2) {
    int l = getHexPair(token, i) ^ xor;
    mail += String.fromCharCode(l);
  }

  return mail;
}