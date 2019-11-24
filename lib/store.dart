import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:mensa_italia/login.dart';
import 'package:page_transition/page_transition.dart';




class StoreMensa extends StatefulWidget {
  @override
  _StoreMensaState createState() => _StoreMensaState();
}

class _StoreMensaState extends State<StoreMensa> {
  @override
  Widget build(BuildContext context) {


    return Scaffold(


      appBar: AppBar(
        title: AutoSizeText("Mensa Store".toUpperCase()),
        actions: <Widget>[
          GestureDetector(
            onTap: (){
              showDialog(
                  context: context,
                  builder: (d){
                    return Padding(
                        padding: EdgeInsets.only(left: 50.0, right: 50.0),
                        child://AlertDialog or any other Dialog you can use
                        Dialog(
                            elevation: 0.0,
                            backgroundColor: Colors.transparent,
                            child: Container(

                              padding: EdgeInsets.all(20),
                              decoration: BoxDecoration(
                                  color: Colors.white,
                                  borderRadius: BorderRadius.all(Radius.circular(25))
                              ),
                              child: Column(
                                mainAxisSize: MainAxisSize.min,
                                crossAxisAlignment: CrossAxisAlignment.stretch,
                                children: [
                                  AutoSizeText("QUESTA FUNZIONALITà è disattivata fino ad approvazione".toUpperCase(), textAlign: TextAlign.center, style: TextStyle(fontWeight: FontWeight.bold),)
                                ],
                              ),
                            )
                        ));
                  }

              );
            },
            child: Icon(Icons.shopping_cart),
          ),
          Container(width: 20,)
        ],

      ),
      body: SingleChildScrollView(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: <Widget>[

            Container(
              padding: EdgeInsets.all(25),
              child: AutoSizeText("Questo è il Mensa Store. Puoi acquistare prodotti brendizzati così da supportare le attività dell'associazione",textAlign: TextAlign.center,),

            ),
            Wrap(
              alignment: WrapAlignment.spaceEvenly,
              runAlignment: WrapAlignment.spaceEvenly,
              runSpacing: 20.0,
              children: <Widget>[
                ShopBlock(
                  link: "https://files.cdn.printful.com/files/6c8/6c8cb6995e3044954c5d65e8c9cb4054_preview.png",
                  name: "TAZZA",
                  price: "19,50",
                ),
                ShopBlock(
                  link: "https://files.cdn.printful.com/files/dc1/dc16f074fc5465a8d04e9a1a650d644b_preview.png",
                  name: "TAZZA WOW",
                  price: "19,50",
                ),
                ShopBlock(
                  link: "https://files.cdn.printful.com/files/353/3530b3c5fe7ebe1f53e4ed3a0ba832ec_preview.png",
                  name: "ASCIUGAMANO",
                  price: "45,50",
                ),
                ShopBlock(
                  link: "https://files.cdn.printful.com/files/bcd/bcd407c43680ac48d8ed33db59f59021_preview.png",
                  name: "BIKINI",
                  price: "46,50",
                ),
              ],
            ),

            Container(
            margin: EdgeInsets.only(top: 80),
              padding: EdgeInsets.all(25),
              child: AutoSizeText("QUEST'AREA NON è attiva, deve ancora essere approvata dal CDG".toUpperCase(), style: TextStyle(color: Colors.red, fontWeight: FontWeight.bold),textAlign: TextAlign.center,)
            ),

          ],
        ),
      ),

    );
  }
}


class ShopBlock extends StatefulWidget {


  String link;
  String name;
  String price;

  ShopBlock({this.link, this.price, this.name});

  @override
  _ShopBlockState createState() => _ShopBlockState();
}

class _ShopBlockState extends State<ShopBlock> {
  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: (){
        Navigator.push(context, PageTransition(type: PageTransitionType.rightToLeft, child:ProductPage(widget.link, widget.name, widget.price)));
      },
      child: Card(
        color: Theme.of(context).accentColor,
        shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(25)
        ),
        child: ClipRRect(
          borderRadius: BorderRadius.circular(25),
          child: Container(
            width: 150,
            child: Column(
              children: <Widget>[
                Hero(
                  tag: widget.link,
                  child: Material(
                    color: Colors.transparent,
                    child:Image.network(widget.link),
                  ),
                ),
                Container(
                  padding: EdgeInsets.all(25),
                  child: Column(
                    children: <Widget>[
                      AutoSizeText(widget.name, style: TextStyle(fontWeight: FontWeight.bold,color: Colors.white),),
                      Container(height: 20,),
                      Row(
                        crossAxisAlignment: CrossAxisAlignment.end,
                        mainAxisAlignment: MainAxisAlignment.end,
                        children: <Widget>[

                          AutoSizeText(widget.price+" €", textAlign: TextAlign.end, style: TextStyle(color: Colors.white),)
                        ],
                      ),
                    ],
                  ),
                )
              ],
            ),
          ),
        ),
      ),
    );
  }
}


class ProductPage extends StatefulWidget {

  String link;
  String name;
  String price;

  ProductPage(this.link,  this.name, this.price,);
  @override
  _ProductPageState createState() => _ProductPageState();
}

class _ProductPageState extends State<ProductPage> {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: AutoSizeText(widget.name.toUpperCase()),

      ),
      body: SingleChildScrollView(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: <Widget>[
              Column(
                children: <Widget>[

                  Hero(
                    tag: widget.link,
                    child: Material(
                      color: Colors.transparent,
                      child:Image.network(widget.link),
                    ),
                  ),
                  Container(height: 50,),
                  AutoSizeText(widget.price+" €", style: TextStyle(fontSize: 50, fontWeight: FontWeight.bold, color: Theme.of(context).accentColor),)
                ],
              ),

              Container(
                padding: EdgeInsets.all(20),
                margin: EdgeInsets.only(top: 50),
                child: MensaButton(
                  text: "AGGIUNGI AL CARRELLO",
                  onPressedNew: (){

                    showDialog(
                      context: context,
                      builder: (d){
                        return Padding(
                            padding: EdgeInsets.only(left: 50.0, right: 50.0),
                            child://AlertDialog or any other Dialog you can use
                            Dialog(
                                elevation: 0.0,
                                backgroundColor: Colors.transparent,
                                child: Container(

                                  padding: EdgeInsets.all(20),
                                  decoration: BoxDecoration(
                                      color: Colors.white,
                                      borderRadius: BorderRadius.all(Radius.circular(25))
                                  ),
                                  child: Column(
                                    mainAxisSize: MainAxisSize.min,
                                    crossAxisAlignment: CrossAxisAlignment.stretch,
                                    children: [
                                      AutoSizeText("QUESTA FUNZIONALITà è disattivata fino ad approvazione".toUpperCase(), textAlign: TextAlign.center, style: TextStyle(fontWeight: FontWeight.bold),)
                                    ],
                                  ),
                                )
                            ));
                      }

                    );
                  },
                ),
              )

            ],
          ),
      ),
    );
  }
}







