import 'dart:async';
import 'package:auto_size_text/auto_size_text.dart';
import 'package:contacts_service/contacts_service.dart';
import 'package:extended_image/extended_image.dart';
import 'package:flutter/material.dart';
import 'package:liquid_progress_indicator/liquid_progress_indicator.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:sqflite/sqflite.dart';
import 'package:html/dom.dart' as dom;
import 'package:url_launcher/url_launcher.dart';
import 'login.dart';

class PhoneBook extends StatefulWidget {
  @override
  _PhoneBookState createState() => _PhoneBookState();
}

class _PhoneBookState extends State<PhoneBook> {

  @override
  void initState() {
    // TODO: implement initState
    super.initState();
    init();
  }

  double loading=0;



  init() async {

    PhoneDB().pointOfOperation((i,j){
      loading=i/j;
      if(mounted)
      setState(() {

      });
    });

    if(await PhoneDB().load()){

    }else{
      await PhoneDB().download(redo: (){
        if(mounted)
        setState(() {

        });
      });
      await PhoneDB().load();
      if(mounted)
      setState(() {

      });
    }

    if(mounted)
    setState(() {

    });
  }

  MensaTextField mensaTextField;

  @override
  Widget build(BuildContext context) {




    return  Scaffold(

        appBar: AppBar(
            title: AutoSizeText("Rubrica".toUpperCase()),
            actions: <Widget>[

              PhoneDB().primalContact==null?Container():GestureDetector(
                onTap: () async {

                  loading=0;
                  await PhoneDB().download(redo: (){
                    if(mounted)
                    setState(() {

                    });
                  });
                  await PhoneDB().load();

                  if(mounted)
                  setState(() {

                  });
                },
                child: Icon(Icons.refresh),
              ),

              Container(
                width: 20,
              )
            ]
        ),

        body: Stack(
          children: <Widget>[



            GestureDetector(
                onTap: () {

                  FocusScope.of(context).requestFocus(new FocusNode());
                },
                child: PhoneDB().primalContact==null?LiquidLinearProgressIndicator(
                  value: loading.toDouble(), // Defaults to 0.5.
                  valueColor: AlwaysStoppedAnimation(Theme.of(context).accentColor), // Defaults to the current Theme's accentColor.
                  backgroundColor: Colors.white, // Defaults to the current Theme's backgroundColor.
                  borderRadius: 0.0,
                  direction: Axis.vertical, // The direction the liquid moves (Axis.vertical = bottom to top, Axis.horizontal = left to right). Defaults to Axis.horizontal.
                  center: Container(
                    padding: EdgeInsets.all(40),
                    child: AutoSizeText(("Sto scaricando la rubrica mensana "+(loading*100).toStringAsFixed(2).toString()+"%").toUpperCase(), style: TextStyle(fontWeight: FontWeight.bold), textAlign: TextAlign.center,),
                  )
                ):ListView(
                  children: List.generate(PhoneDB().primalContact.length, (i){
                    return ContactPhone(PhoneDB().primalContact[i].name,PhoneDB().primalContact[i].image,(PhoneDB().primalContact[i].place1??"")+","+(PhoneDB().primalContact[i].place2??""),PhoneDB().primalContact[i].link);
                  }),
                )
            ),

            PhoneDB().primalContact==null?Container():Positioned(
              left: 0,
              right: 0,
              bottom: 0,
              child: Container(
                padding: EdgeInsets.all(20),
                color: Colors.transparent,
                child: Card(
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(200)),
                  elevation: 10.0,
                  child: Container(

                    padding: EdgeInsets.all(5),
                    decoration: BoxDecoration(

                        color: Colors.white,
                        borderRadius: BorderRadius.circular(200)
                    ),
                    child: mensaTextField??=MensaTextField("Cerca (Cognome Nome)",onChag: (text){

                      PhoneDB().load(text: text);
                      if(mounted)
                      setState(() {

                      });
                    },),
                  ),
                ),
              ),
            )

          ],
        )

    );
  }
}



class ContactPhone extends StatefulWidget {
  final String image;
  final String name;
  final String city;
  final String link;

  ContactPhone(this.name, this.image, this.city, this.link);

  @override
  _ContactPhoneState createState() => _ContactPhoneState();
}

class _ContactPhoneState extends State<ContactPhone> {


  String createLink(String link) {

    if (link.split("://").contains("https")) {
      return (""+link);
    }else{
      return "https://www.cloud32.it"+link;

    }
  }


  @override
  Widget build(BuildContext context) {
    return  GestureDetector(

      onTap: (){
        showDialog(context: context, builder: (d)=>DialogCall(widget.name, createLink(widget.image), widget.link));
      },

      child: Column(
        children: <Widget>[
          Container(
            padding: EdgeInsets.all(20),
            child:
            Row(
              children: <Widget>[
                Container(
                  height: 50,
                  width: 50,
                  margin: EdgeInsets.only(right: 20),
                  decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(5),
                      boxShadow: [
                        BoxShadow(blurRadius: 1.0, offset: Offset(0, 1.0))
                      ],
                      image: DecorationImage(image: ExtendedNetworkImageProvider(createLink(widget.image),), fit: BoxFit.cover)
                  ),
                ),
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[

                    AutoSizeText(widget.name, style: TextStyle(fontSize: 16),),

                    AutoSizeText(widget.city, style: TextStyle(color: Colors.black.withOpacity(0.5)),)
                  ],
                )
              ],
            ),

          ),
          Divider(height: 0, color: Colors.black,)
        ],
      ),
    );
  }
}




class DialogCall extends StatefulWidget {


  final String name;
  final String image;
  final String link;

  DialogCall(this.name, this.image, this.link);


  @override
  _DialogCallState createState() => _DialogCallState();
}

class _DialogCallState extends State<DialogCall> {


  String number;
  bool existsytem=false;

  init() async {

    number=await PhoneDB().getPhone(widget.link);
    if(number==null){
      number="-1";
    }


    Map<Permission, PermissionStatus> permissions = await [Permission.contacts].request();


    if(permissions[Permission.contacts].isDenied) {
      var xa = await ContactsService.getContacts(query: widget.name);
      existsytem = xa.isNotEmpty;
    }

    if(mounted)
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
  void dispose() {
    PhoneDB().pointOfOperation(null);
    super.dispose();
  }


  @override
  Widget build(BuildContext context) {
    return Dialog(
      child:  ClipRRect(
        borderRadius: BorderRadius.circular(25),
    child:SingleChildScrollView(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        mainAxisAlignment: MainAxisAlignment.center,
        crossAxisAlignment: CrossAxisAlignment.center,
        children: <Widget>[
          Container(height: 20,),
          AutoSizeText(widget.name, style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold), textAlign: TextAlign.center,),

          Container(height: 20,),
          Container(
            height: MediaQuery.of(context).size.width*2/3,
            width: double.infinity,
            decoration: BoxDecoration(
                image: DecorationImage(image: ExtendedNetworkImageProvider(widget.image??"https://helpx.adobe.com/content/dam/help/en/stock/how-to/visual-reverse-image-search/jcr_content/main-pars/image/visual-reverse-image-search-v2_intro.jpg",), fit: BoxFit.cover)
            ),


          ),

          Container(height: 20,),
          number==null?Container(
            padding: EdgeInsets.all(20),
            child: LoadingDialog(),
          ):AutoSizeText(number!=null?(number=="-1"?"Non è presente un numero di telefono":number):"OTTENGO NUMERO", style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold), textAlign: TextAlign.center,),
          Container(height: 20,),

          number!=null&&number!="-1"?Column(
            children: <Widget>[
              InkWell(
                splashColor: Colors.white,
                onTap: (){
                  launch("tel:"+number);
                },
                child: Container(

                  width: double.infinity,
                  padding: EdgeInsets.all(20),
                  color: Theme.of(context).accentColor,
                  child:AutoSizeText("Chiama".toUpperCase(), style: TextStyle(color: Colors.white),textAlign: TextAlign.center,),
                ),
              ),
              existsytem?Container():InkWell(
                splashColor: Colors.white,
                onTap: () async {

                  Map<Permission, PermissionStatus> permissions = await [Permission.contacts].request();


                  if(permissions[Permission.contacts].isGranted) {
                    Contact newContact = new Contact(
                        givenName: widget.name.substring(0, widget.name.indexOf(' ',0)),
                        familyName: widget.name.substring(widget.name.indexOf(' ',0), widget.name.length),
                        androidAccountName: widget.name,
                        displayName: widget.name,
                        company: "Mensa"
                    );

                    newContact.phones = [
                      Item(label: "Phone", value: number)
                    ];


                    await ContactsService.addContact(newContact);
                    Navigator.pop(context);
                  }else{

                  }

                },
                child: Container(

                  width: double.infinity,
                  padding: EdgeInsets.all(20),
                  color: Colors.green,
                  child:AutoSizeText("Aggiungi ai contatti".toUpperCase(), style: TextStyle(color: Colors.white),textAlign: TextAlign.center,),
                ),
              ),

            ],
          ):Container()
        ],
      ),
    ),
      )
    );
  }

  doPermissionRequest(){

  }


}



class PhoneDB{

  static final PhoneDB _singleton = PhoneDB._internal();

  List<PrimalContacts> primalContact=[];


  factory PhoneDB() {
    return _singleton;
  }

  PhoneDB._internal(){
    _init();
  }

  Database db;
  _init() async {
    db = await openDatabase('MensaContact.db', version: 1,
        onCreate: (Database db, int version) async {
          await db.execute('CREATE TABLE Contacts (id INTEGER PRIMARY KEY AUTOINCREMENT,  name TEXT, image TEXT , phone TEXT, place1 TEXT, place2 TEXT, link TEXT UNIQUE)');
        }
    );
    await load();
  }


  Future<bool> load({String text}) async {
    if(db==null){
      await _init();
    }

    SharedPreferences sp=await SharedPreferences.getInstance();
    if(sp.getInt("downloadedPoint")!=-1){
      return false;
    }

    var tal;
    if(text==null){

      tal=await db.rawQuery("SELECT * FROM Contacts");
    }else{



        tal=await db.rawQuery("SELECT * FROM Contacts WHERE name LIKE ? OR name LIKE ?",["%"+text+"%"]);

    }
    primalContact.clear();
    await tal.forEach((din){
      primalContact.add(PrimalContacts(id: din["id"],image: din["image"],name: din["name"],phone: din["phone"],link: din["link"],place1: din["place1"],place2: din["place2"],));
    });
    return primalContact.isNotEmpty;
  }

  int maxPage;
  int page=1;
  Function(int,int) everyPage;

  pointOfOperation(Function(int,int) everyPage){
    this.everyPage=everyPage;
  }


  download({Function redo}) async{

    SharedPreferences sp=await SharedPreferences.getInstance();

    page=sp.getInt("downloadedPoint");
    page=page==null?1:page;
    if(page>1){


    }else{
      if(db==null){
        await _init();
      }
      primalContact.clear();
      primalContact=null;
      db.delete("Contacts");
      if(redo!=null){
        redo();
      }
      page=1;
    }
    dom.Document document = await API().getData("https://www.cloud32.it/Associazioni/utenti/regsocio?s_cognome=&s_nome=&s_citta=&s_provincia=&s_regione=&Ricerca=Ricerca&page="+page.toString());

    maxPage=int.parse(document.getElementsByTagName("ul").where((e)=>e.attributes["class"]=="pagination").first.getElementsByTagName("li").elementAt(document.getElementsByTagName("ul").where((e)=>e.attributes["class"]=="pagination").first.getElementsByTagName("li").length-2).getElementsByTagName("a").first.text);

    for(;page<=maxPage;page++){
      dom.Document document = await API().getData("https://www.cloud32.it/Associazioni/utenti/regsocio?s_cognome=&s_nome=&s_citta=&s_provincia=&s_regione=&Ricerca=Ricerca&page="+page.toString());

      try{
        List<dom.Element> elements=document.getElementsByTagName("table").where((e)=>e.attributes["class"]=="table table-hover table-striped table-condensed").first.getElementsByTagName("tbody").first.getElementsByTagName("tr");


        elements.forEach((i) async {

          String image;
          String name;
          String link;
          String place1;
          String place2;

          var data=i.getElementsByTagName("td");

          for(int j=0;j<data.length;j++){
            if(j==0){
              try{
                image=data[j].getElementsByTagName("img").first.attributes["src"].replaceAll("\\", "/");

              }catch(e){

                image="";
              }
            }
            if(j==2){
              name=data[j].text;
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

          try{
            db.rawInsert("INSERT INTO Contacts(image,name,link,place1,place2) VALUES (?,?,?,?,?)",
                [image,name,link,place1,place2]
            );
          }catch(e){
          }

          if(everyPage!=null){
            everyPage(page, maxPage);
          }

        });
      }catch(e){

      }

      sp.setInt("downloadedPoint", page);

    }

    sp.setInt("downloadedPoint", -1);

    primalContact=[];



  }


  Future<String> getPhone(String link) async {

    var tal=await db.rawQuery("SELECT phone FROM Contacts WHERE link=?",[
      link
    ]);

    try{


      if(tal.first["phone"]==null){

        dom.Document document = await API().getData(createLink(link));
        String phones=(
            document.getElementsByClassName("form-group").where((f)=>f.getElementsByClassName("col-sm-2 col-sm-offset-3").isNotEmpty).where((f){

              return f.getElementsByClassName("col-sm-2 col-sm-offset-3").where((d)=>d.text=="Cellulare:").isNotEmpty;
            }).first.getElementsByClassName("col-sm-6 col-sm-offset-1").first.text
        );

        db.rawQuery("UPDATE Contacts SET phone=? WHERE link=?", [phones, link]);
        return phones;
      }else{
        return tal.first["phone"];
      }



    }catch(e){
      return null;
    }

  }



  String createLink(String link) {

    if (link.split("://").contains("https")) {
      return (""+link);
    }else{
      return "https://www.cloud32.it"+link;


    }
  }

}

class PrimalContacts{
  int id;
  String image;
  String name;
  String phone;
  String link;
  String place1;
  String place2;

  PrimalContacts({this.id, this.image, this.name, this.phone, this.link, this.place1, this.place2});

  @override
  String toString() {
    return "id: "+(id??"null").toString()+" name: "+(name??"null")+" link: "+(link??"null")+ "phone: "+(phone??"null")+" image: "+(image??"null\n");
  }
}












