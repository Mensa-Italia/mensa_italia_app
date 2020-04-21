import 'dart:convert';

import 'package:auto_size_text/auto_size_text.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:mensa_italia/login.dart';
import 'package:page_transition/page_transition.dart';
import 'package:url_launcher/url_launcher.dart';




class SIGMensa extends StatefulWidget {
  @override
  _SIGMensaState createState() => _SIGMensaState();
}

class _SIGMensaState extends State<SIGMensa> {


  List<dynamic> list;

  init() async {
    list=jsonDecode((await API().getRawData("https://raw.githubusercontent.com/Mensa-Italia/SIGs/master/sigs.json")));
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


    return Scaffold(


      appBar: AppBar(
        title: AutoSizeText("Special Interest Groups".toUpperCase()),

      ),
      body: SingleChildScrollView(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: list==null?[

            Container(
              margin: EdgeInsets.only(top: 20),
              width: 100,
              height: 100,
              child: LoadingDialog(),
            )


           /* SigItem("https://www.facebook.com/groups/1739607052753863/","https://www.mensaitalia.it/wp-content/uploads/2019/12/sig_acquariofilia.jpg"),
            SigItem("https://www.facebook.com/groups/2927299450673592","https://www.mensaitalia.it/wp-content/uploads/2019/12/sig_armi_e_tiro.jpg"),
            SigItem("https://www.facebook.com/groups/sigbusiness/","https://www.mensaitalia.it/wp-content/uploads/2019/12/sig_business.jpg"),
            SigItem("https://www.facebook.com/groups/sigchimica/","https://www.mensaitalia.it/wp-content/uploads/2019/12/sig_chimica.jpg"),
            SigItem("https://www.facebook.com/groups/1261001867247150/","https://www.mensaitalia.it/wp-content/uploads/2019/12/sig_cinema.jpg"),
            SigItem("https://www.facebook.com/groups/883452132040041/","https://www.mensaitalia.it/wp-content/uploads/2019/12/sig_cinofilia.jpg"),
            SigItem("https://www.facebook.com/groups/348857848911700/","https://www.mensaitalia.it/wp-content/uploads/2019/12/sig_cryptovalute.jpg"),
            SigItem("https://www.facebook.com/groups/sigmenssanaincorporesano","https://www.mensaitalia.it/wp-content/uploads/2019/12/sig_salute_benessere.jpg"),
            SigItem("https://www.facebook.com/groups/517555231704182/?ref=gs&fref=gs&dti=504720036958312&","https://www.mensaitalia.it/wp-content/uploads/2019/12/sig_forti_di_testa.jpg"),
            SigItem("https://www.facebook.com/groups/siggenitori","https://www.mensaitalia.it/wp-content/uploads/2019/12/sig_genitori.jpg"),
            SigItem("https://www.facebook.com/groups/456326971174954/","https://www.mensaitalia.it/wp-content/uploads/2019/12/sig_giochi.jpg"),
            SigItem("https://www.facebook.com/groups/398889487559205/","https://www.mensaitalia.it/wp-content/uploads/2019/12/sig_humanitas.jpg"),
            SigItem("https://www.facebook.com/groups/257025094501775","https://www.mensaitalia.it/wp-content/uploads/2019/12/sig_libri.jpg"),
            SigItem("https://www.facebook.com/groups/motorimensani/","https://www.mensaitalia.it/wp-content/uploads/2019/12/sig_motori.jpg"),
            SigItem("https://www.facebook.com/groups/274940252649779","https://www.mensaitalia.it/wp-content/uploads/2019/12/sig_politica.jpg"),
            SigItem("https://www.facebook.com/groups/422257988377354","https://www.mensaitalia.it/wp-content/uploads/2019/12/sig_rainbow.jpg"),
            SigItem("https://www.facebook.com/groups/1977792852250654","https://www.mensaitalia.it/wp-content/uploads/2019/12/sig_ricerca_sull_intelligenza.jpg"),
            SigItem("https://www.facebook.com/groups/420332905062409/?ref=br_rs","https://www.mensaitalia.it/wp-content/uploads/2019/12/sig_skeptical.jpg"),
            SigItem("https://www.facebook.com/groups/259962521209761","https://www.mensaitalia.it/wp-content/uploads/2019/12/sig_stile_bellezza_arte.jpg"),
            SigItem("https://www.facebook.com/groups/sigtecnologia","https://www.mensaitalia.it/wp-content/uploads/2019/12/sig_tecnologia.jpg"),
*/
          ]:list.isNotEmpty?List.generate(list.length, (i){
            return SigItem(list[i]["link"],list[i]["image"]);
          }):[],
        ),
      ),

    );
  }
}







class SigItem extends StatefulWidget {
  String url;
  String image;
  SigItem(this.url,this.image);
  @override
  _SigItemState createState() => _SigItemState();
}

class _SigItemState extends State<SigItem> {
  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: (){
        tryToLunchUrl(widget.url);
      },
      child: Card(
        margin: EdgeInsets.all(10),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(15)),
        elevation: 5.0,
        child: Container(
          width: MediaQuery.of(context).size.width,
          decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(15)
          ),
          child: ClipRRect(
            borderRadius: BorderRadius.circular(15),
            child: CachedNetworkImage(
              imageUrl:widget.image,
              width: MediaQuery.of(context).size.width,
              errorWidget: (d,s,t)=>CachedNetworkImage(imageUrl: "https://www.mensaitalia.it/wp-content/uploads/2019/12/sig_miog_error.jpg",),
            ),
          ),
        ),
      ),
    );
  }

  tryToLunchUrl(String url) async {

    if (await canLaunch(url)) {
      await launch(url);
    }
  }
}

