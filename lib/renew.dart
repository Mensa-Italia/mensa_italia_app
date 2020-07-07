



import 'package:auto_size_text/auto_size_text.dart';
import 'package:dio_cookie_manager/dio_cookie_manager.dart' as dio;
import 'package:flip_card/flip_card.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_inappwebview/flutter_inappwebview.dart';
import 'package:flutter_masked_text/flutter_masked_text.dart';
import 'package:html/dom.dart';
import 'package:mensa_italia/login.dart';
import 'package:mensa_italia/transitate.dart';
import 'package:shared_preferences/shared_preferences.dart';


class RenewCardPage extends StatefulWidget {
  @override
  _RenewCardPageState createState() => _RenewCardPageState();
}

class _RenewCardPageState extends State<RenewCardPage> {


  Document document=null;
  init() async {
    document=await API().getData("https://www.cloud32.it/Associazioni/utenti/rinnovo");

    setState(() {

    });
  }


  @override
  void initState() {
    // TODO: implement initState
    super.initState();
    init();
  }

  Size size;
  String selected="";
  String moneyToPay="";
  String moneyToAdd=null;
  MensaTextField mensaTextField;

  @override
  Widget build(BuildContext context) {
    size=MediaQuery.of(context).size;
    return Scaffold(

        appBar: AppBar(
          title: AutoSizeText("Rinnovo Tessera".toUpperCase()),
        ),
        body: ListView(
          children: document==null?[

            Container(
              margin: EdgeInsets.only(top: 20),
              width: 100,
              height: 100,
              child: LoadingDialog(),
            )

          ]:[


            DropdownButton<String>(
              iconSize: 30,
              itemHeight: 100,
              value: selected,
              dropdownColor: Theme.of(context).accentColor,
              selectedItemBuilder: (ctx){
                return List.generate(document.getElementById("selectListinoAss").children.length, (i){
                  return Container(
                    width: size.width-40,
                    padding: EdgeInsets.all(10),
                    margin: EdgeInsets.only(left: 10),
                    child: Row(
                      children: <Widget>[
                        Expanded(
                          child: AutoSizeText(document.getElementById("selectListinoAss").children[i].text.trim()==""?"Seleziona tipo rinnovo".toUpperCase():document.getElementById("selectListinoAss").children[i].text.trim(), minFontSize: 0,),
                        )
                      ],
                    ),
                  );
                });
              },

              items: List.generate(document.getElementById("selectListinoAss").children.length, (i){
                return DropdownMenuItem(
                  value: document.getElementById("selectListinoAss").children[i].attributes["value"].toString(),
                  child: Container(
                    padding: EdgeInsets.all(10),
                    child: Row(
                      children: <Widget>[
                        Expanded(
                          child: AutoSizeText(
                            document.getElementById("selectListinoAss").children[i].text.trim()==""?"Seleziona tipo rinnovo".toUpperCase():document.getElementById("selectListinoAss").children[i].text.trim(), minFontSize: 0,
                            style: TextStyle(
                                color: Colors.white
                            ),
                          ),
                        )
                      ],
                    ),
                  ),
                );
              }),
              onChanged: (str) async {
                selected=str;
                String data=(await API().getRawData("https://www.cloud32.it/Associazioni/GetListinoIscri.php", data: {
                  "codass":"170734",
                  "codListino":str,
                })).toString().trim();
                mensaTextField=null;
                print(data);
                moneyToPay=data.split("|")[0];
                try{
                  if(data.split("|")[1]=="1"){
                    moneyToAdd="0.0";
                  }else{
                    moneyToAdd=null;
                  }
                }catch(exc){
                  moneyToAdd=null;
                  moneyToPay="";
                }

                setState(() {

                });
              },
            ),


            moneyToPay==""?Container():FlipCard(
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
                              return Container(
                                padding: EdgeInsets.all(20),
                                child: Row(
                                  mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                                  children: <Widget>[
                                    Expanded(
                                        child:Container(
                                            child:Image.asset("assets/images/lettering_white.png", height: constraints.maxHeight*2/3,)
                                        )
                                    ),
                                    Expanded(
                                      child:  Container(
                                        child: AutoSizeText.rich(TextSpan(
                                            children: [
                                              TextSpan(text: "€ ", style: TextStyle(color: Colors.transparent)),
                                              TextSpan(text: (double.parse(moneyToPay??"0.0")+double.parse(moneyToAdd??"0.0")).toStringAsFixed(2)+" €"),
                                            ]
                                        ), style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 25), minFontSize: 0,),
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

              back: Container(

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
                        child: Container(
                          padding: EdgeInsets.all(20),
                          alignment: Alignment.center,
                          child: AutoSizeText(
                            document.getElementById("selectListinoAss").children.where((element) => element.attributes["value"]==selected).first.text.trim()
                            , style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 15), minFontSize: 0,textAlign: TextAlign.center,),
                        ),

                      )
                  ),
                ),
              ),

            ),

            moneyToAdd==null?Container():Container(height:20),
            moneyToAdd==null?Container():AutoSizeText("Quanto vuoi aggiungere alla quota?".toUpperCase(), style: TextStyle(color: Theme.of(context).accentColor, fontWeight: FontWeight.bold, fontSize: 12),textAlign: TextAlign.center,),

            moneyToAdd==null?Container():Container(
                padding: EdgeInsets.only(left: 20, right:20, bottom:20, top:10),
                child:mensaTextField??=MensaTextField("Aggiungi una donazione",onChag: (text){

                  try{

                    double mn=double.parse(text.toString().replaceAll(",", ".").toString());
                    moneyToAdd=mn.abs().toStringAsFixed(2);
                    print(text);
                    print(moneyToAdd);
                  }catch(exc){
                    print(exc);
                    moneyToAdd="0.0";
                  }

                  setState(() {

                  });
                }, textEditingController:MoneyMaskedTextController(
                    thousandSeparator: "",
                    decimalSeparator: ",",
                    precision:2,
                    initialValue:0.0
                ), textInputType: TextInputType.number)
            ),

            moneyToPay==""?Container():Container(
              padding: EdgeInsets.all(20),
              child: AutoSizeText(
                List.generate(document.getElementsByClassName("informative").where((element) => element.attributes["class"]=="informative").length, (index) =>
                    document.getElementsByClassName("informative").where((element) => element.attributes["class"]=="informative").elementAt(index).children.fold("", (previousValue, element) => previousValue+"\n"+element.text)
                ).fold("\n", (previousValue, element) => previousValue+"\n"+element).trim(),
                textAlign: TextAlign.justify,
              ),

            ),

            moneyToPay==""?Container():Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: <Widget>[
                Container(
                    margin: EdgeInsets.only(left: 10),
                    child:Checkbox(
                        value: confirmPrivacy,

                        onChanged: (data){
                          confirmPrivacy=data;
                          setState(() {

                          });
                        }
                    )
                ),
                Expanded(
                    child:Container(
                        alignment: Alignment.centerLeft,
                        child: AutoSizeText("Acconsento")
                    )
                ),
              ],
            ),

            moneyToPay==""?Container():Container(
                padding: EdgeInsets.all(20),
                child:MensaButton(text:"Paga", onPressedNew: () async {


                  int i=await NavigateTo(context).page(StartPayMode(
                      await API().getRawData("https://www.cloud32.it/Associazioni/utenti/rinnovo",data: {
                        "_token":document.getElementsByTagName("input").where((element) => element.attributes["name"]=="_token").first.attributes["value"],
                        "selectListinoAss":selected,
                        "prezzo":moneyToPay,
                        "qtaVariabile":moneyToAdd??"0.0",
                        "testoprivacy":"testoprivacy",
                        "pagamento":"2",
                      }), await API().getCookieJar()
                  ));

                  if(i==0){
                    showDialog(
                      context: context,
                      builder: (ctx)=>Dialog(
                        backgroundColor: Colors.white,
                        child: Column(
                          mainAxisSize:MainAxisSize.min,
                          children: <Widget>[
                            Container(
                              width: double.infinity,
                              padding: EdgeInsets.all(10),
                              decoration: BoxDecoration(
                                  borderRadius: BorderRadius.only(topRight: Radius.circular(25),topLeft: Radius.circular(25)),
                                  color: Color(0xFF184295)
                              ),
                              child: AutoSizeText("ERRORE".toUpperCase(), style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 20),textAlign: TextAlign.center,),
                            ),
                            Container(
                                width: double.infinity,
                                padding: EdgeInsets.all(10),
                                child: AutoSizeText("Qualcosa è andato storto e il pagamento non è avvenuto correttamente.", style: TextStyle(),textAlign: TextAlign.center,)
                            ),
                            Container(
                                width: double.infinity,
                                padding: EdgeInsets.all(10),
                                child:Row(
                                  mainAxisAlignment: MainAxisAlignment.end,
                                  children: <Widget>[
                                    MensaButton(
                                        text: "Okay",
                                        onPressedNew:(){
                                          Navigator.pop(context);
                                        }
                                    )
                                  ],
                                )
                            )
                          ],
                        ),
                      ),
                    );
                  }else{
                    var a = await SharedPreferences.getInstance();
                    a.setString("NextRenew", null);
                    Navigator.pop(context);
                  }

                },
                    enableded:moneyToPay!=""&&confirmPrivacy
                )
            ),

            Container(height: 150,)

          ],
        )
    );
  }

  bool confirmPrivacy=false;

}


class StartPayMode extends StatefulWidget {

  String startPayPal;
  dio.CookieManager cookieManager;

  StartPayMode(this.startPayPal, this.cookieManager);

  @override
  _StartPayModeState createState() => _StartPayModeState();
}

class _StartPayModeState extends State<StartPayMode> {

  @override
  void initState() {
    // TODO: implement initState
    super.initState();
    widget.cookieManager.cookieJar.loadForRequest(Uri.parse("https://www.cloud32.it")).forEach((element) {
      CookieManager.instance().setCookie(
          url: "https://www.cloud32.it",
          name: element.name,
          value: element.value,
          domain: element.domain,
          expiresDate: element.expires.millisecondsSinceEpoch,
          isSecure: element.secure,
          maxAge: element.maxAge,
          isHttpOnly: element.httpOnly,
          path: element.path
      );
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(

      appBar: AppBar(
        title: AutoSizeText("PayPal".toUpperCase()),
      ),
      body:InAppWebView(
        initialUrl: "https://www.cloud32.it/Associazioni/utenti/rinnovo",
        initialHeaders: {
          //   "cookie":widget.cookieManager..cookieJar.loadForRequest(Uri.parse("https://www.cloud32.it/Associazioni/utenti/rinnovo")).map((e) => e.name+"="+e.value+";").fold("", (previousValue, element) => previousValue+element)

        },


        initialOptions: InAppWebViewGroupOptions(
          crossPlatform: InAppWebViewOptions(
            debuggingEnabled: true,
          ),




        ),
        initialData: InAppWebViewInitialData(
          data: widget.startPayPal,
          baseUrl: "https://www.cloud32.it/Associazioni/utenti/rinnovo",

        ),


        onLoadStart: (inawc, str) async {
          print(str);
          try{
            if(str=="https://www.cloud32.it/Associazioni/utenti/home") {
              if (((await inawc.getHtml()).contains(
                  "Errore durante la procedura di pagamento"))) {
                Navigator.pop(context, 0);
              } else {
                Navigator.pop(context, 1);
              }
            }

          }catch(exc){

          }
        },
      ),
    );
  }
}

