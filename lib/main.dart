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
import 'package:page_transition/page_transition.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:wave/config.dart';
import 'package:wave/wave.dart';
import 'home_full.dart';
import 'login.dart';

void main() => runApp(MyApp());

class MyApp extends StatelessWidget {
  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Mensa Italia',
      theme: ThemeData(
        // This is the theme of your application.
        //
        // Try running your application with "flutter run". You'll see the
        // application has a blue toolbar. Then, without quitting the app, try
        // changing the primarySwatch below to Colors.green and then invoke
        // "hot reload" (press "r" in the console where you ran "flutter run",
        // or simply save your changes to "hot reload" in a Flutter IDE).
        // Notice that the counter didn't reset back to zero; the application
        // is not restarted.
        primarySwatch: Colors.blue,
        accentColor: Color(0xFF184295),
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



        Navigator.pushReplacement(context, PageTransition(type: PageTransitionType.fade, child:MensaFullPage(document)));


      }else{

        setState(() {
          isPreparing=false;
        });
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
            BackGroundHome(),

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
                        Navigator.push(context, PageTransition(type: PageTransitionType.fade, child:LoginPage()));
                      },


                      text: "ACCEDI",
                    ),
                    Container(height: 40,)

                  ],
                ),

            ),
          ],
        ),
      ),
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
    );
  }
}
