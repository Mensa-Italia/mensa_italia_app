/*
Creato da Matteo Sipion nella data del 15/10/2019.

Matteo Sipione detiene i diritti autoriali e commerciali di questo software.

-------------------------------------------------------------------------------

Created by Matteo Sipion on the date of 15/10/2019.

Matteo Sipione holds the authorial and commercial rights to this software.
*/


import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:html/dom.dart';
import 'package:onesignal_flutter/onesignal_flutter.dart';
import 'package:page_transition/page_transition.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:wave/config.dart';
import 'package:wave/wave.dart';
import 'blog.dart';
import 'home_full.dart';
import 'login.dart';

void main() => runApp(MyApp());

class MyApp extends StatelessWidget {


  MyApp(){
    OneSignal.shared.init(
        "f2b93a2b-0d67-4e9e-b5c8-991c96a33ddc",
        iOSSettings: {
          OSiOSSettings.autoPrompt: false,
          OSiOSSettings.inAppLaunchUrl: false,
          OSiOSSettings.inAppAlerts: false,
        },


    );
    OneSignal.shared.setInFocusDisplayType(OSNotificationDisplayType.notification);
    sendFirst();

  }

  sendFirst() async {

    var a = await SharedPreferences.getInstance();


    if(a.getBool("FIRSTNOTIFY")==null||!a.getBool("FIRSTNOTIFY")){

      OneSignal.shared.postNotification(OSCreateNotification(
          playerIds: [(await OneSignal.shared.getPermissionSubscriptionState()).subscriptionStatus.userId],
          content: "Benvenuto nell'app del MENSA!"
      ));
      a.setBool("FIRSTNOTIFY", true);
    }

  }



  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Mensa Italia',
      theme: ThemeData(
          primarySwatch: Colors.blue,
          accentColor: Color(0xFF184295),
          bottomAppBarColor: Colors.transparent,
          bottomAppBarTheme: BottomAppBarTheme(
              color: Colors.transparent
          ),
          dialogTheme: DialogTheme(
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(25),

            ),

          ),
          appBarTheme: AppBarTheme(
            color: Color(0xFF184295),


          ),
          textTheme: TextTheme(
            title: TextStyle(fontFamily: "Gotham"),
            body1: TextStyle(fontFamily: "Gotham"),
            body2: TextStyle(fontFamily: "Gotham"),
            subtitle: TextStyle(fontFamily: "Gotham"),
            headline: TextStyle(fontFamily: "Gotham"),
            display4: TextStyle(fontFamily: "Gotham"),
            display3: TextStyle(fontFamily: "Gotham"),
            display2: TextStyle(fontFamily: "Gotham"),
            display1: TextStyle(fontFamily: "Gotham"),
            subhead: TextStyle(fontFamily: "Gotham"),
            overline: TextStyle(fontFamily: "Gotham"),
            button: TextStyle(fontFamily: "Gotham"),
            caption: TextStyle(fontFamily: "Gotham"),
          )
      ),
      debugShowMaterialGrid: false,

      debugShowCheckedModeBanner: false,
      showSemanticsDebugger: false,
      home: HomePage(),
    );
  }
}


class HomePage extends StatefulWidget {
  @override
  _HomePageState createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {



  @override
  void initState() {
    // TODO: implement initState
    super.initState();
    prepare();
  }


  bool isPreparing=true;
  prepare() async {
    SharedPreferences prefs = await SharedPreferences.getInstance();


    Document document=await API().doLoginAndRetrieveMain(context, prefs.getString("email"), prefs.getString("password"));

    if(document!=null){



      Navigator.pushReplacement(context, PageTransition(type: PageTransitionType.rightToLeft, child:MensaFullPage(document)));


    }else{


      SharedPreferences prefs = await SharedPreferences.getInstance();
      if(prefs.getBool("isJumped")!=null&&prefs.getBool("isJumped")){
        Navigator.pushReplacement(context, PageTransition(type: PageTransitionType.rightToLeft, child:BlogMensa()));
      }else{

        setState(() {
          isPreparing=false;
        });
      }
    }

  }



  tryToLunchUrl(String url) async {

    if (await canLaunch(url)) {
      await launch(url);
    }
  }

  Size size;
  @override
  Widget build(BuildContext context) {
    size=MediaQuery.of(context).size;


    return Scaffold(



        body: Container(
          width: size.width,

          color: Colors.black.withOpacity(0.4),
          height: size.height,
          child: Stack(
              children: [



                Stack(
                  alignment: Alignment.bottomCenter,
                  children: <Widget>[


                    BackGroundHome(),
                    Container(
                      margin: EdgeInsets.only(bottom: 20),
                      child:  AutoSizeText("FLOREAT MENSA!", style: TextStyle(color: Colors.white, fontStyle: FontStyle.italic),),
                    )
                  ],
                ),

                SingleChildScrollView(

                  child: Column(
                      crossAxisAlignment: CrossAxisAlignment.center,
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: <Widget>[
                        Container(
                          height: 100,
                        ),
                        Hero(tag: "logo", child: Material(color: Colors.transparent,child: Image.asset("assets/images/lettering_blue.png", width: size.width/2,),),),
                        Container(height: 40,),
                        isPreparing?LoadingDialog():MensaButton(
                          onPressedNew: () {
                            Navigator.push(context, PageTransition(type: PageTransitionType.rightToLeft, child:LoginPage()));
                          },


                          text: "ACCEDI",
                        ),
                        Container(height: 30,),
                        isPreparing?Container():AutoSizeText("Se non sei mensano salta l'accesso oppure scopri come diventarlo!.", textAlign: TextAlign.center,),
                        Container(height: 50,),



                      ]
                  ),
                ),
                Positioned(

                    right: 20,
                    left: 20,
                    top: MediaQuery.of(context).padding.top+20,
                    child:
                    isPreparing?Container():Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: <Widget>[

                        Expanded(
                          child: GestureDetector(
                            onTap: (){
                              tryToLunchUrl("https://www.mensaitalia.it/iscriviti/");
                            },
                            child: Container(
                              child:AutoSizeText("DIVENTA MENSANO", textAlign: TextAlign.start, style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold), ),
                            ),
                          ),
                        ),
                        Expanded(
                            child: GestureDetector(
                              onTap: () async {
                                SharedPreferences prefs = await SharedPreferences.getInstance();
                                await prefs.setBool("isJumped", true);
                                Navigator.pushReplacement(context, PageTransition(child: BlogMensa(), type: PageTransitionType.rightToLeft));
                              },
                              child: AutoSizeText("SALTA", textAlign: TextAlign.end, style: TextStyle(color: Theme.of(context).accentColor, fontWeight: FontWeight.bold, fontSize: 14),),
                            )
                        )


                      ],
                    )
                ),
              ]
          ),
        )
    );
  }
}

class BackGroundHome extends StatefulWidget {
  @override
  _BackGroundHomeState createState() => _BackGroundHomeState();
}

class _BackGroundHomeState extends State<BackGroundHome> {
  @override
  Widget build(BuildContext context) {
    return WaveWidget(
      config: CustomConfig(
        gradients: [
          [Color(0xEE6d9eff), Color(0xEE6d9eff)],
          [Color(0xEE2666e5), Color(0xEE2666e5)],
          [Color(0xEE1f54bc), Color(0xEE1f54bc)],
          [Color(0xEE184295), Color(0xEE184295)]
        ],
        durations: [35000, 19440, 10800, 6000],
        heightPercentages: [0.70, 0.73, 0.75, 0.80],
        blur: MaskFilter.blur(BlurStyle.solid, 10),
        gradientBegin: Alignment.bottomLeft,
        gradientEnd: Alignment.topRight,
      ),
      waveAmplitude: 0,
      backgroundColor: Colors.white,
      size: Size(
        double.infinity,
        double.infinity,
      ),
      duration: 1000,
    );
  }
}
