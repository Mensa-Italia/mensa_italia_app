import 'package:flutter/material.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/model/notification.dart';
import 'package:mensa_italia_app/model/receipt.dart';
import 'package:mensa_italia_app/model/ticket.dart';
import 'package:pocketbase/pocketbase.dart';

class TicketSSE extends ChangeNotifier {
  static final TicketSSE _instance = TicketSSE._internal();
  factory TicketSSE() => _instance;
  TicketSSE._internal();

  UnsubscribeFunc? _unsubscribe;

  List<TicketModel> _data = [];

  List<TicketModel> get tickets => _data;

  void start() {
    _data.clear();
    Api().pb.collection('tickets').subscribe('*', (e) {
      if (e.action == "create") {
        _data.add(TicketModel.fromJson(e.record!.toJson()));
      } else if (e.action == "update") {
        var index = _data.indexWhere((element) => element.id == e.record!.id);
        if (index != -1) {
          _data[index] = TicketModel.fromJson(e.record!.toJson());
        } else {
          _data.add(TicketModel.fromJson(e.record!.toJson()));
        }
      } else if (e.action == "delete") {
        _data.removeWhere((element) => element.id == e.record!.id);
      }
      _data.sort((a, b) => b.created.compareTo(a.created));
      notifyListeners();
    }).then((value) {
      _unsubscribe = value;
      Api().getTickets().then((value) {
        _data.addAll(value);
        _data.sort((a, b) => b.created.compareTo(a.created));
        notifyListeners();
      });
    });
  }

  void stop() {
    _unsubscribe?.call();
  }
}
