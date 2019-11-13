/*
Creato da Matteo Sipion nella data del 15/10/2019.

Matteo Sipione detiene i diritti autoriali e commerciali di questo software.

-------------------------------------------------------------------------------

Created by Matteo Sipion on the date of 15/10/2019.

Matteo Sipione holds the authorial and commercial rights to this software.
*/
import 'dart:async';
import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:html/dom.dart';
import 'package:html/dom.dart' as prefix0;
import 'home.dart';
import 'login.dart';

class DocumentPageInfo extends StatefulWidget {

  String link;

  DocumentPageInfo(this.link);


  @override
  _DocumentPageInfoState createState() => _DocumentPageInfoState();
}

class _DocumentPageInfoState extends State<DocumentPageInfo> {


  Document document;

  TextEditingController nameController = new TextEditingController();



  List<prefix0.Element> elements=[];

  int maxPage=0;


  prepare() async {
    document=await API().getData(widget.link+"?docdescr="+Uri.encodeFull(nameController.text)+"&datada=&dataa=&tags=");
    try{
      maxPage=int.parse(document.getElementsByTagName("ul").where((e)=>e.attributes["class"]=="pagination").first.getElementsByTagName("li").elementAt(document.getElementsByTagName("ul").first.getElementsByTagName("li").length+1).getElementsByTagName("a").first.text);

    }catch(e){
      maxPage=0;
    }
    try{
      elements=document.getElementsByTagName("table").where((e)=>e.attributes["class"]=="table table-hover table-striped table-condensed").first.getElementsByTagName("tbody").first.getElementsByTagName("tr");

    }catch(e){
      elements=[];
    }
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
    Timer(Duration(seconds: 1),() async {
        page++;




        document = await API().getData(
            widget.link+"?docdescr="+Uri.encodeFull(nameController.text)+"&datada=&dataa=&tags=page=" +
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
      isLoading=false;
    });

    }
  }

  @override
  Widget build(BuildContext context) {


    return Scaffold(
        appBar: AppBar(
          title: AutoSizeText("Documenti".toUpperCase()),
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
                  child: AutoSizeText("Cerca documenti".toUpperCase(), minFontSize: 0, style: TextStyle(fontSize: 25, color:Color(0xFF2f2e6a),fontWeight: FontWeight.bold),maxLines: 1,),
                ),


                Container(height: 20,),

                Container(
                  margin: EdgeInsets.symmetric(horizontal: 20),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: <Widget>[
                      MensaTextField("Testo da cercare", textEditingController: nameController,),


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
                  ]:elements.isEmpty?[

                    Container(height: 50,),

                    Container(
                      margin: EdgeInsets.symmetric(horizontal: 20),
                      alignment: Alignment.center,
                      child: AutoSizeText("Nessun risultato disponibile".toUpperCase(), minFontSize: 0, style: TextStyle(fontSize: 12, fontStyle: FontStyle.italic),maxLines: 1,),
                    ),



                  ]:List.generate(elements.length, (i){
                    String image;
                    String desc;
                    String link;

                    String date;


                    var data=elements[i].getElementsByTagName("td");

                    for(int j=0;j<data.length;j++){

                      if(j==0){
                        date=data[j].text;
                      }
                      if(j==1){
                        desc=data[j].text;
                      }


                      if(j==4){
                        try{
                          image=data[j].getElementsByTagName("img").first.attributes["src"];
                          link=data[j].getElementsByTagName("a").first.attributes["href"];

                        }catch(e){

                          image="";
                        }
                        break;
                      }


                    }

                    return DocumentBlock(image, desc, date, link);
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




class DocumentBlock extends StatefulWidget {
  String image;
  String desc;
  String data;
  String link;

  DocumentBlock(this.image, this.desc, this.data, this.link);


  @override
  _DocumentBlockState createState() => _DocumentBlockState();
}

class _DocumentBlockState extends State<DocumentBlock> {
  @override
  Widget build(BuildContext context) {
    return GestureDetector(onTap: (){

      Navigator.push(context, MaterialPageRoute(builder:(d)=>ShowDocumentPage(createLink(widget.link))));
    },child:Container(
      margin: EdgeInsets.symmetric(horizontal: 20, vertical: 10),
      padding: EdgeInsets.all(20),
      width: double.infinity,
      decoration: BoxDecoration(
          borderRadius: BorderRadius.all(Radius.circular(25)),
          color: Theme.of(context).accentColor,
          boxShadow: [
            BoxShadow(blurRadius: 3.0)
          ]
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
                mainAxisAlignment: MainAxisAlignment.start,
                mainAxisSize: MainAxisSize.max,
                children: <Widget>[

                  AutoSizeText(widget.desc.toUpperCase(), minFontSize: 0, style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold, color: Colors.white),maxLines: 1,),
                  AutoSizeText(widget.data, minFontSize: 0,style: TextStyle( color: Colors.white),maxLines: 1,),

                ],
              ),
            ),
          )

        ],
      ),
    )
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
