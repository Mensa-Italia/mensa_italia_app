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
import 'package:shared_preferences/shared_preferences.dart';
import 'package:wave/config.dart';
import 'package:wave/wave.dart';
import 'home.dart';
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
        accentColor: Color(0xFF2f2e6a),
        appBarTheme: AppBarTheme(
          color: Color(0xFF2f2e6a),

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



        Navigator.pushReplacement(context, MaterialPageRoute(builder: (d)=>MensaPage(document)));


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
                    Hero(tag: "logo", child: Material(color: Colors.transparent,child: Image.asset("assets/images/logo.png", width: size.width/2,),),),
                    Container(height: 20,),
                    AutoSizeText("MENSA ITALIA", style: TextStyle(fontSize: 20,fontWeight: FontWeight.bold, color: Color(0xFF2f2e6a)),),
                    Container(height: 40,),
                    isPreparing?CircularProgressIndicator():MensaButton(
                      onPressedNew: () {
                        Navigator.push(context, MaterialPageRoute(builder: (d)=>LoginPage()));
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
          [Color(0xEEadabff), Color(0xEEadabff)],
          [Color(0xEE6c69f5), Color(0xEE6c69f5)],
          [Color(0xEE504eb5), Color(0xEE504eb5)],
          [Color(0xEE2f2e6a), Color(0xEE2f2e6a)]
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
