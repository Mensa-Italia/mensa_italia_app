/*
Creato da Matteo Sipion nella data del 15/10/2019.

Matteo Sipione detiene i diritti autoriali e commerciali di questo software.

-------------------------------------------------------------------------------

Created by Matteo Sipion on the date of 15/10/2019.

Matteo Sipione holds the authorial and commercial rights to this software.
*/
import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:html/dom.dart';
import 'package:in_app_update/in_app_update.dart';
import 'package:mensa_italia/transitate.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:wave/config.dart';
import 'package:wave/wave.dart';
import 'blog.dart';
import 'home_full.dart';
import 'login.dart';
import 'dart:io';
import 'package:flutter_localizations/flutter_localizations.dart';


Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await SharedPreferences.getInstance();
  SystemChrome.setPreferredOrientations([
    DeviceOrientation.portraitUp,
    DeviceOrientation.portraitDown,
  ]).then((value) => runApp(MyApp()));

}

class MyApp extends StatelessWidget {




  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Mensa Italia',

      localizationsDelegates: [
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      supportedLocales: [
        const Locale('en', 'US'), // English
        const Locale('it', 'IT'), // Italian
        const Locale('fr', 'FR'), // French
        const Locale('th', 'TH'), // Thai
      ],
      theme: ThemeData(
          primarySwatch: Colors.blue,
          accentColor: Color(0xFF184295),
          bottomAppBarColor: Colors.transparent,
          brightness: Brightness.dark,
          dialogBackgroundColor: Colors.white,
          backgroundColor: Colors.white,
          scaffoldBackgroundColor: Colors.white,
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
            title: TextStyle(fontFamily: "Gotham", color: Colors.black),
            body1: TextStyle(fontFamily: "Gotham", color: Colors.black),
            body2: TextStyle(fontFamily: "Gotham", color: Colors.black),
            subtitle: TextStyle(fontFamily: "Gotham", color: Colors.black),
            headline: TextStyle(fontFamily: "Gotham", color: Colors.black),
            display4: TextStyle(fontFamily: "Gotham", color: Colors.black),
            display3: TextStyle(fontFamily: "Gotham", color: Colors.black),
            display2: TextStyle(fontFamily: "Gotham", color: Colors.black),
            display1: TextStyle(fontFamily: "Gotham", color: Colors.black),
            subhead: TextStyle(fontFamily: "Gotham", color: Colors.black),
            overline: TextStyle(fontFamily: "Gotham", color: Colors.black),
            button: TextStyle(fontFamily: "Gotham", color: Colors.black),
            caption: TextStyle(fontFamily: "Gotham", color: Colors.black),
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

class _HomePageState extends State<HomePage> with TickerProviderStateMixin{



  @override
  void initState() {
    super.initState();
    prepare();
  }




  bool isPreparing=true;
  prepare() async {
    SharedPreferences prefs = await SharedPreferences.getInstance();



      try{
        if((await InAppUpdate.checkForUpdate()).updateAvailability==UpdateAvailability.updateAvailable){
          await InAppUpdate.performImmediateUpdate();
          exit(0);
        }
      }catch(E){

      }



      Document document=await API().doLoginAndRetrieveMain(context, prefs.getString("email"), prefs.getString("password"));

      if(document!=null){
        NavigateTo(context).page(MensaFullPage(document),replace:true);
      }else{
        SharedPreferences prefs = await SharedPreferences.getInstance();
        if(prefs.getBool("isJumped")!=null&&prefs.getBool("isJumped")){
          NavigateTo(context).page(BlogMensa(),replace:true);
        }else{
          setState(() {
            isPreparing=false;
            _visible=true;
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

  static Matrix4 _pmat(num pv) {
    return new Matrix4(
      1.0, 0.0, 0.0, 0.0, //
      0.0, 1.0, 0.0, 0.0, //
      0.0, 0.0, 1.0, pv * 0.001, //
      0.0, 0.0, 0.0, 1.0,
    );
  }

  Matrix4 perspective = _pmat(1.0);

  bool _visible=false;


  @override
  Widget build(BuildContext context) {
    size=MediaQuery.of(context).size;


    return GestureDetector(
        onTap: () {

          FocusScope.of(context).requestFocus(new FocusNode());
        },
        child:Scaffold(
          backgroundColor: Theme.of(context).accentColor,
          body:Container(
            width: size.width,
            height: size.height,
            child: SingleChildScrollView(
              child: ConstrainedBox(
                constraints: BoxConstraints(
                    minHeight: size.height
                ),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: <Widget>[
                    AnimatedOpacity(
                      opacity: _visible?1.0:0.0,
                      duration: Duration(milliseconds: 500),
                      child: Container(
                        margin: EdgeInsets.only(top: MediaQuery.of(context).padding.top+50, bottom: 50),
                        child: Image.asset('assets/images/mensa_under.png', width: size.width/2>300?300:size.width/2,),
                      ),

                    ),


                    isPreparing?CircularProgressIndicator(
                      valueColor: new AlwaysStoppedAnimation<Color>(Colors.white),
                    ):AnimatedOpacity(
                      opacity: _visible?1.0:0.0,
                      duration: Duration(milliseconds: 200),
                      child: Card(
                      margin: EdgeInsets.only(bottom: 0),
                      elevation: 5.0,
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(25)),
                      color: Colors.white,
                      child: Container(
                        width: size.width*3/4>300?300:size.width*3/4,
                        padding: EdgeInsets.all(20),
                        child:   LoginPage(),
                        ),
                      ),
                    ),

                    AnimatedOpacity(
                      opacity: _visible?1.0:0.0,
                      duration: Duration(milliseconds: 500),

                      child:GestureDetector(
                        onTap: () async {
                          SharedPreferences prefs = await SharedPreferences.getInstance();
                          await prefs.setBool("isJumped", true);
                          NavigateTo(context).page(BlogMensa(), replace:true);
                        },
                        child: Container(
                          margin: EdgeInsets.only(top: 100),
                          child: AutoSizeText("SALTA ACCESSO", style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold),),
                        ),
                      ),

                    ),


                    GestureDetector(
                        onTap: (){
                          tryToLunchUrl("https://www.sipio.it");
                        },
                        child:  Container(
                          margin: EdgeInsets.all(20),
                          child: Row(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: <Widget>[
                              AutoSizeText("Thought by ", style: TextStyle(fontStyle: FontStyle.italic, color: Colors.white.withOpacity(0.5)), textAlign: TextAlign.center,),
                              AutoSizeText("Matteo Sipione", style: TextStyle(fontStyle: FontStyle.italic, color: Colors.white.withOpacity(0.5), fontWeight: FontWeight.bold), textAlign: TextAlign.center,),

                            ],
                          ),
                        )
                    )
                  ],
                ),
              ),
            ),
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
